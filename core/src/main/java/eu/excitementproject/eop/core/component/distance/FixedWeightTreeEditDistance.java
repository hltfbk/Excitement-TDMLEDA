package eu.excitementproject.eop.core.component.distance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.uima.jcas.JCas;
import org.uimafit.util.JCasUtil;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import treedist.EditScore;
import treedist.Mapping;
import treedist.TreeEditDistance;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

import eu.excitement.type.alignment.Link;
import eu.excitementproject.eop.common.component.distance.DistanceCalculation;
import eu.excitementproject.eop.common.component.distance.DistanceComponentException;
import eu.excitementproject.eop.common.component.distance.DistanceValue;
import eu.excitementproject.eop.common.component.scoring.ScoringComponentException;
import eu.excitementproject.eop.common.configuration.CommonConfig;
import eu.excitementproject.eop.common.configuration.NameValueTable;
import eu.excitementproject.eop.common.exception.ComponentException;
import eu.excitementproject.eop.common.exception.ConfigurationException;
import eu.excitementproject.eop.common.utilities.configuration.ImplCommonConfig;
import eu.excitementproject.eop.core.component.alignment.lexicallink.LexicalAligner;
import eu.excitementproject.eop.lap.implbase.LAP_ImplBase;


/**
 * The <code>FixedWeightedTreeEditDistance</code> class implements the DistanceCalculation interface.
 * Given a pair of T-H, each of them represented as a sequences of tokens, the edit distance between 
 * T and H is the minimum number of operations required to convert T to H. 
 * FixedWeightedTreeEditDistance implements the simplest form of weighted edit distance that simply uses a 
 * constant cost for each of the edit operations: match, substitute, insert, delete.
 * 
 * The component uses an implementation of Zhang and Shasha's algorithm [Zhang89] for calculating tree edit distance
 * that is kindle make available by Yuya Unno from this site: https://github.com/unnonouno/tree-edit-distance
 * 
 * @author roberto zanoli
 * @author silvia colombo
 * 
 * @since August 2014
 * 
 */
public class FixedWeightTreeEditDistance implements DistanceCalculation {

	/**
	 * The aligner component that finds the alignments between the tokens in T and those in H
	 */
	private LexicalAligner aligner;
	/**
	 * The transformations obtained converting T into H
	 */
    private List<Transformation> transformations;
	/**
	 * weight for match
	 */
    private final double mMatchWeight = 0;
    /**
	 * weight for delete
	 */
    private final double mDeleteWeight = 1;
    /**
	 * weight for insert
	 */
    private final double mInsertWeight = 1;
    /**
	 * weight for substitute
	 */
    private final double mSubstituteWeight = 1;
    /**
	 * the activated instances
	 */
    private String instances;
    /**
     * the alignments produced by the aligner components
     */
    private Map<String,Link> alignments;
    /**
     * it allows users to specify their own alignments
     */
    private Set<String> userAlignments;
    /**
     * if the punctuation has to be removed from the trees
     */
    private boolean punctuationRemoval;
	/**
	 * verbose level
	 */
	private String verbosityLevel;
	
    /**
     *  the logger
     */
    static Logger logger = Logger.getLogger(FixedWeightTreeEditDistance.class.getName());
    
    
    /**
     * Construct a fixed weight edit distance
     */
	public FixedWeightTreeEditDistance() {
    	
        this.transformations = null;
        this.instances = null;
        this.aligner = null;
        this.alignments = null;
        this.punctuationRemoval = false;
        
    }

	
    /** 
	 * Constructor used to create this object. 
	 * 
	 * @param config the configuration
	 * 
	 */
    public FixedWeightTreeEditDistance(CommonConfig config) throws ConfigurationException, ComponentException {
    
    	this();
        
    	logger.info("creating an instance of " + this.getComponentName() + " ...");
    
        try {
        	
	        //get the component configuration
	    	NameValueTable componentNameValueTable = 
	    			config.getSection(this.getClass().getCanonicalName());
	    	
	    	//get the selected instance
	    	instances = componentNameValueTable.getString("instances");
	    	
	    	//get the instance configuration
	    	NameValueTable instanceNameValueTable = 
	    			config.getSubSection(this.getClass().getCanonicalName(), instances);
	    	
			//setting the logger verbosity level
			if (this.verbosityLevel == null) {
				this.verbosityLevel = instanceNameValueTable.getString("verbosity-level");
				logger.setUseParentHandlers(false);
				ConsoleHandler consoleHandler = new ConsoleHandler();
				consoleHandler.setLevel(Level.parse(this.verbosityLevel));
				logger.addHandler(consoleHandler);
				logger.setLevel(Level.parse(this.verbosityLevel));
			}
			
			logger.fine("using:" + instances);
			
			//get the configuration file of the aligner component
			String configurationFile = 
					instanceNameValueTable.getString("configuration-file");
			logger.fine("component configuration file:" + configurationFile);
			
	    	//get the aligner component configuration
			String componentName = 
					instanceNameValueTable.getString("alignment-component");
			logger.fine("using:" + componentName);
			
			//if the punctuation has to be removed from dependencies trees
			punctuationRemoval = Boolean.parseBoolean(instanceNameValueTable.getString("punctuation-removal"));
			logger.fine("punctuation removal:" + punctuationRemoval);
			
			//the alignments provided by users
			this.userAlignments = loadUserAlignements(instanceNameValueTable.getString("alignments-file"));
			logger.fine("user alignments:" + this.userAlignments.size());
			
			//create an instance of the aligner component
	    	if (this.aligner == null && componentName != null && !componentName.equals("")) {
				
				try {
					
					Class<?> componentClass = Class.forName(componentName);
					Constructor<?> componentClassConstructor = componentClass.getConstructor(CommonConfig.class);
					File configFile = new File(configurationFile);
					ImplCommonConfig commonConfig = new ImplCommonConfig(configFile);
					this.aligner = (LexicalAligner) componentClassConstructor.newInstance(commonConfig);
					
				} catch (Exception e) {
					
					throw new ComponentException(e.getMessage());
					
				}
				
			}
	    	
    	} catch (ConfigurationException e) {
    		
    		throw new ComponentException(e.getMessage());
    		
    	} catch (Exception e) {
		
    		throw new ComponentException(e.getMessage());
		
    	}
        
        logger.info("done.");
    	
    }
    
    
    /** 
	 * Constructor
	 * 
	 * @param alignerComponentName The lexical aligner component to do the matches between the tokens, e.g.
	 * eu.excitementproject.eop.core.component.alignment.lexicallink.LexicalAligner
    
	 * @param alignerComponentConfigurationFileName The configuration file of the aligner component
	 * 
	 * @param usersAlignmentFile The file containing the users alignments.
	 * 
	 * @param punctuationRemoval True if the punctuation has to be removed.
	 * 
	 */
    public FixedWeightTreeEditDistance(String alignerComponentName, String alignerComponentConfigurationFileName, String usersAlignmentFile, boolean punctuationRemoval) throws ConfigurationException, ComponentException {
    
    	this();
        
    	logger.info("creating an instance of " + this.getComponentName() + " ...");
    
        try {
        	
	    	//get the aligner component configuration
			String componentName = alignerComponentName;
			logger.fine("using:" + componentName);
			
			//if the punctuation has to be removed from dependencies trees
			this.punctuationRemoval = punctuationRemoval;
			logger.fine("punctuation removal:" + punctuationRemoval);
			
			//the alignments provided by users
			this.userAlignments = loadUserAlignements(usersAlignmentFile);
			logger.fine("user alignments:" + this.userAlignments.size());
			
			//create an instance of the aligner component
	    	if (this.aligner == null && componentName != null && !componentName.equals("")) {
				
				try {
					
					Class<?> componentClass = Class.forName(componentName);
					Constructor<?> componentClassConstructor = componentClass.getConstructor(CommonConfig.class);
					File configFile = new File(alignerComponentConfigurationFileName);
					ImplCommonConfig commonConfig = new ImplCommonConfig(configFile);
					this.aligner = (LexicalAligner) componentClassConstructor.newInstance(commonConfig);
					
				} catch (Exception e) {
					
					throw new ComponentException(e.getMessage());
					
				}
				
			}
	    	
    	} catch (ConfigurationException e) {
    		
    		throw new ComponentException(e.getMessage());
    		
    	} catch (Exception e) {
		
    		throw new ComponentException(e.getMessage());
		
    	}
        
        logger.info("done.");
    	
    }
    
    
    @Override
    public String getComponentName() {
    
    	return "FixedWeightTreeEditDistance";
	
	}
    
    
    @Override
    public String getInstanceName() {
    	
    	return instances;
    	
    }
    
    
    /**
     * Get the transformations used to transform T into H
     * 
     * @return the transformations
     * 
     */
    public List<Transformation> getTransformations() {

    	return this.transformations;
    	
    }
    
    
    /** 
	 * shutdown the component and the used resources
	 */
	public void shutdown() {
		
		logger.info("shut down ...");
		
		if (this.aligner != null)
			this.aligner.cleanUp();
		
		this.transformations =  null;		 
		this.instances = null;
		this.alignments = null;
		this.punctuationRemoval = false;
		
		logger.info("done.");
		
	}
    
    
    @Override
    public DistanceValue calculation(JCas jcas) throws DistanceComponentException {

    	DistanceValue distanceValue = null;
    	
    	try {
    		
    		//get the alignments between T and H produced by the aligner component
    		alignments = getAlignments(jcas);
    	    //get the Text
	    	JCas tView = jcas.getView(LAP_ImplBase.TEXTVIEW);
	    	//get the dependency tree of Text
	    	String t_tree = cas2CoNLLX(tView);
	    	logger.fine("\nThe Tree of Text:\n" + t_tree);
	    	
	    	//remove punctuation
	    	if (this.punctuationRemoval) {
	    		t_tree = removePunctuation(t_tree);
	    		logger.fine("\nThe Tree of Text after removing punctuation:\n" + t_tree);
	    	}
	    	
	    	//create the Text fragment
	    	Fragment t_fragment = getFragment(t_tree);
	    	//get the Hypothesis
	    	JCas hView = jcas.getView(LAP_ImplBase.HYPOTHESISVIEW); 
	    	//the dependency tree of Hypothesis
	    	String h_tree = cas2CoNLLX(hView);

	    	logger.fine("\nThe Tree of Hypothesis:\n" + h_tree);
	    	
	    	//remove punctuation
	    	if (this.punctuationRemoval) {
		    	h_tree = removePunctuation(h_tree);
		    	logger.fine("\nThe Tree of Hypothesis after removing punctuation:\n" + h_tree);
	    	}

	    	//create the Hypothesis fragment
	    	Fragment h_fragment = getFragment(h_tree);
            //calculate the distance between T and H by using the matches
	    	//provided by the aligner component.
	    	distanceValue = distance(t_fragment, h_fragment, alignments);
	    	
    	} catch (Exception e) {
    		
    		throw new DistanceComponentException(e.getMessage());
    		
    	}
    	
    	return distanceValue;
    	
    }

    
    @Override
    public Vector<Double> calculateScores(JCas jcas) throws ScoringComponentException {
    
    	DistanceValue distanceValue = null;
    	Vector<Double> v = new Vector<Double>();
	    
    	try {
    		
    		//get the alignments between T and H produced by the aligner component
    		alignments = getAlignments(jcas);
	 	   	// get Text
		    JCas tView = jcas.getView(LAP_ImplBase.TEXTVIEW);
		    //get the dependency tree of Text

		    String t_tree = cas2CoNLLX(tView);
		    logger.fine("Text:\n" + t_tree);
		    
	    	//remove punctuation
	    	if (this.punctuationRemoval) {
				t_tree = removePunctuation(t_tree);
				logger.fine("\nThe Cleaned Tree of Text:\n" + t_tree);
	    	}

		    //create the Text fragment
		    Fragment t_fragment = getFragment(t_tree);
		    //get Hypothesis
		    JCas hView = jcas.getView(LAP_ImplBase.HYPOTHESISVIEW); 
		    //the dependency tree of Hypothesis
		    String h_tree = cas2CoNLLX(hView);

		    logger.fine("Hypothesis:\n" + h_tree);
		    
		    //remove punctuation
	    	if (this.punctuationRemoval) {
				h_tree = removePunctuation(h_tree);
				logger.fine("\nThe Cleaned Tree of Hypothesis:\n" + h_tree);
	    	}

		    //create the Hypothesis fragment
		    Fragment h_fragment = getFragment(h_tree);
		    
	        //calculate the distance between T and H by using the matches
		    //provided by the aligner component.
		    distanceValue = distance(t_fragment, h_fragment, alignments);
		    	
    	} catch (Exception e) {
    		
    		throw new ScoringComponentException(e.getMessage());
    		
    	}
     
    	v.add(distanceValue.getDistance());
	    v.add(distanceValue.getUnnormalizedValue());
    
	    return v;
	     
    }
    
    
    /*
     * Get the alignments between T and H produced by the aligner component
     * 
     * @param jcas the CAS containing T and H
     * 
     * @return key value pairs 
     * key represents the alignment tokens, e.g. assassin__killer (`___` separates the 2 tokens)
     * values are instead the produced links containing the resource version, the relation that has been used to do the 
     * alignment. Other information is about the strength of the alignment and the direction of the alignment
     * itself, e.g. WORDNET__3.0__HYPERNYM__0.5__TtoH
     * 
     */
    private Map<String,Link> getAlignments(JCas jcas) throws Exception {
		
    	Map<String,Link> result = new HashMap<String,Link>();
    	
		try {
			
			// Call the aligner component to get the alignments between T and H
			if (this.aligner != null) {
				logger.finer("\ngetting the alignments ...");
				aligner.annotate(jcas);
				logger.finer("done.");
			}
						
			logger.finer("\n\nalignments list:\n");
			
			//get the HYPOTHESIS view
			JCas hypoView = jcas.getView(LAP_ImplBase.HYPOTHESISVIEW);
			
			//cycle through the alignments
			for (Link link : JCasUtil.select(hypoView, Link.class)) {
				
				logger.finer(String.format("\nText phrase: %s, " +
						"Hypothesis phrase: %s, " + 
						"id: %s, confidence: %f, direction: %s", 
						link.getTSideTarget().getCoveredText(),
						link.getHSideTarget().getCoveredText(),
						link.getID(), link.getStrength(),
						link.getDirection().toString())); 
				
				String key = link.getTSideTarget().getCoveredText() +
				        		"__" + 
				        		link.getHSideTarget().getCoveredText();
				     
				//for a couple of tokens it can save a type of alignment only in the
				//order as provided by the aligner component.
				if (!result.containsKey(key))
					result.put(key, link);
				
			}
			
		} catch (Exception e) {
			
			throw new Exception(e.getMessage());
			
		}
		
		return result;
	}
    
    
    /**
     * Returns the weighted edit distance between T and H. During this
     * phase the transformations producing H from T are calculated too.
     * 
     * @param t the text fragment 
     * @param h the hypothesis fragment
     * 
     * @return The edit distance between the sequences of tokens
     * 
     * @throws ArithmeticException
     * 
     */
    public DistanceValue distance(Fragment t, Fragment h, Map<String,Link> alignments) throws Exception {

    	//here we need to call the library for calculating tree edit distance
    	double distance = 0.0;
    	double normalizedDistanceValue = 0.0;
    	double norm = 1.0;
    	
    	try {

	    	//Creating the Tree of Text
	    	LabeledTree t_tree = createTree(t);
	        //logger.info("T:" + t_tree);
	    	
	    	//Creating the Tree of Hypothesis
	    	LabeledTree h_tree = createTree(h);
	    	//logger.info("H:" + h_tree);
			
	    	//creating an instance of scoreImpl containing the definition of the 
	    	//the edit distance operations.
	    	ScoreImpl scoreImpl = new ScoreImpl(t_tree, h_tree, alignments);
	    	
		    //Create an instance of TreeEditDistance
			TreeEditDistance dist = new TreeEditDistance(scoreImpl);
			
			//This is used for storing the sequence of edit distance operations
			Mapping map = new Mapping(t_tree, h_tree);
			
			//Distance calculation
			distance = dist.calc(t_tree, h_tree, map);
			
		    //cycle through the list of the edit distance operations (i.e. replace -rep, 
	        //insertion -ins, deleletion -del)
		    //operations are in the format: rep:2,3 rep:1,1 rep:0,0 ins:2 rep:3,4 rep:4,5
	        //e.g. rep:2,3 means replacing node id_2 with node id_3
		    //List<String> operationSequence = map.getSequence();
		    
		    // calculate the transformations required to transform T into H
		    this.transformations = calculateTransformations(t_tree, h_tree, map);
		    
	    	// norm is the distance equivalent to the cost of inserting all the nodes in H and deleting
	    	// all the nodes in T. This value is used to normalize distance values.
	    	norm = (double)(t_tree.size() * this.mDeleteWeight + h_tree.size() * this.mInsertWeight);
	    	
	    	normalizedDistanceValue = distance/norm;
    	
    	} catch (Exception e) { 
    		
    		throw new Exception(e.getMessage());
    		
    	}
    	
    	//return new EditDistanceValue(normalizedDistanceValue, false, distance);
    	return new EditDistanceValue(normalizedDistanceValue, false, distance);
	    
    }
    
     
    /**
     * Create a labeled tree
     */
    private LabeledTree createTree(Fragment f) throws Exception {
    	
    	LabeledTree lTree;
    	
    	try {
    	
	    	//the parents of the nodes
	    	int[] parents = new int[f.size()];
	    	//the ids of the nodes (they are the ids of the tokens as assigned by the dependency parser).
	    	int[] ids = new int[f.size()];
	    	//the tokens themselves
	    	FToken[] tokens = new FToken[f.size()];
	    	
	    	//Filling the data structure
	    	Iterator<FToken> iterator = f.getIterator();
	    	int i = 0;
	    	while (iterator.hasNext()) {
	    		FToken token_i = iterator.next();
	    		//we need to subtract -1 given that the tree edit distance library requires that
	        	//the id of the nodes starts from 0 instead 1.
	    		//System.out.println("======" + token_i);
	    		parents[i] = token_i.getHead();
	    		ids[i] = token_i.getId();
	    		tokens[i] = token_i;
	    		i++;
	    	}
	    	
	    	lTree = new LabeledTree ( //
	    			//the parents of the nodes
	    	    	parents,
	    	    	//the ids of the tokens
	    	    	ids,
	    	    	//the tokens with all their information
	    	    	tokens);
    	
    	} catch (Exception e) { 
    		
    		throw new Exception(e.getMessage());
    		
    	}
    	
    	return lTree;
    	
    }
    
    
    /**
     * This method accepts in input 2 labeled treed (LabeledTree) and the edit operations on the trees needed to transform
     * the tree t_tree into h_tree (map) and return the list of transformations, e.g.
     * 
     * Type: match Info: LOCAL-ENTAILMENT token_T: 7__ocean__ocean__NN__5__pobj__pobj#prep#_ token_H: 7__ocean__ocean__NN__5__pobj__pobj#prep#_
     * Type: rep Info: null token_T: 0__Tigers__tiger__NNP__1__nsubj__nsubj#_ token_H: 0__Men__man__NNS__1__nsubj__nsubj#_
     * 
     * @param t_tree
     * @param h_tree
     * @param alignments
     * @param map
     * 
     * @return the list of transformations
     *
     * @throws Exception
     */
    private List<Transformation> calculateTransformations(LabeledTree t_tree, 
    		LabeledTree h_tree, Mapping map) throws Exception {
    	
    	List<Transformation> transformations = new ArrayList<Transformation>();
        
    	try {
    	
		    //cycle through the list of the edit distance operations (i.e. replace -rep, 
	        //insertion -ins, deleletion -del)
		    //operations are in the format: rep:2,3 rep:1,1 rep:0,0 ins:2 rep:3,4 rep:4,5
	        //e.g. rep:2,3 means replacing node id_2 with node id_3
		    List<String> operationSequence = map.getSequence();
		    
			logger.finer("\nnumber of transformations:" + operationSequence.size());
		    logger.finer("\nlist of transformations:");
		    
		    for (int i = 0; i < operationSequence.size(); i++) {
		    	
		    	String operation_i = (String)operationSequence.get(i);
		    	//System.err.print(operation_i + " ");
		    	String transformationType = operation_i.split(":")[0];
		    	String nodes = operation_i.split(":")[1];
		    	Transformation trans = null;
		    	//case of replace operations; the library we use for tree edit distance doesn't tell us 
		    	//if it was a replace or match operation. Distinguish between replace and match is done in this way: 
		    	//
		    	//match: 
		    	//  -- match between tokens
		    	//  -- positive alignments
		    	//
		    	//replace:
		    	//  -- no matches between tokens
		    	//  -- negative alignments or no alignments
		    	//
		    	if (transformationType.contains(Transformation.REPLACE)) {
			    	int node1 = Integer.parseInt(nodes.split(",")[0]);
			    	int node2= Integer.parseInt(nodes.split(",")[1]);
			    	FToken t_token = t_tree.getToken(node1);
			    	FToken h_token = h_tree.getToken(node2);
			    	
			    	//ENTAILMENT, CONTRADICTION, UNKNOWN
			    	String alignment = this.getAlignmentType(t_token, h_token)[0];
			    	//e.g. resource:WordNET, direction:TtoH
			    	String alignmentInfo = this.getAlignmentType(t_token, h_token)[1];
			    	
			    	//NO ALIGNMENTS --> REPLACE TRANSFORMATION
			    	if (alignment == null) {
			    		trans = new Transformation(Transformation.REPLACE, alignmentInfo, t_token, h_token);
			    	}
			    	//CONTRADICTION --> REPLACE TRANSFORMATION
			    	else if (alignment.indexOf("LOCAL-CONTRADICTION") != -1) {
			    		trans = new Transformation(Transformation.REPLACE, alignmentInfo, t_token, h_token);
			    	}
			    	//UNKNOWN --> REPLACE TRANSFORMATION
			    	else if (alignment.indexOf("UNKNOWN") != -1) {
			    		trans = new Transformation(Transformation.REPLACE, alignmentInfo, t_token, h_token);
			    	}
			    	//ENTAILMENT --> MATCH TRANSFORMATION
			    	else if (alignment.indexOf("LOCAL-ENTAILMENT") != -1) {
			    		trans = new Transformation(Transformation.MATCH, alignmentInfo, t_token, h_token);
			    	}
				    transformations.add(trans);
				    logger.finer("transformation:" + trans.toString());
		    	}
		    	//case of insertion transformation
		    	else if (transformationType.contains(Transformation.INSERTION)){
		    		int node = Integer.parseInt(nodes);
			    	FToken token = h_tree.getToken(node);
			    	trans = new Transformation(transformationType, token);
			    	transformations.add(trans);
			    	logger.finer("transformation:" + trans.toString());
		    	}
		    	//case of deletion transformation
		    	else {
		    		int node = Integer.parseInt(nodes);
			    	FToken token = t_tree.getToken(node);
			    	trans = new Transformation(transformationType, token);
			    	transformations.add(trans);
			    	logger.finer("transformation:" + trans.toString());
		    	}
		    	
		    }
    	
    	} catch (Exception e) {
    		
    		e.printStackTrace();
    		throw new Exception(e.getMessage());
    		
    	}
		    
	    return transformations;
	    
    }
    
    
    /**
     * This method accepts in input a tree (it has been produced by cas2CoNLLX
     * and it is in CoNLL-X format and returns a fragment containing all the tokens in the tree
     * 
     * @param dependencyTree
     * 
     * @return the fragment
     *
     * @throws Exception
     */
    private Fragment getFragment(String dependencyTree) throws Exception {
    	
    	Fragment fragment = null;
    	
    	/* here we need to parse the tree CoNLLX format (i.e. dependencyTree)
    	/ and for each line of it we would need to create an object of the class Token
    	/ and the put it into the Fragment
    	*/
    	try {
    		
    		fragment = new Fragment();

	    	String[] lines = dependencyTree.split("\n");
	    	
	    	for (int i = 0; i < lines.length; i++) {
	    		String[] fields = lines[i].split("\\s");
	    		int tokenId = Integer.parseInt(fields[0]) - 1;
	    		String form = fields[1];	
	    		String lemma = fields[2];	
	    		String pos = fields[3];	
	    		
	    		int head;
	    		if (fields[6].equals("_")) {
	    			head = -1;
	    		}
	    		else
	    			head = Integer.parseInt(fields[6]) - 1;
	    			
	    		String deprel = fields[7];
	    	    //and for each line of it we would need to create an object of the class FToken
	    	    //and then put it into the Fragment
	    		FToken token_i = new FToken(tokenId, form, lemma, pos, head, deprel);
	    		fragment.addToken(token_i);
	    	}
	    	
	    } catch (Exception e) {
    		
    		throw new Exception(e.getMessage());
    		
    	}
    	
    	return fragment;
    	
    }

    
    /**
     * Given a cas (it contains the T view or the H view) in input it produces a
     * string containing the tree in the CoNLL-X format, e.g. 
     * 
     * 1	A	a	DT	_	_	3	det	_	_
     * 2	soccer	soccer	NN	_	_	3	nn	_	_
	 * 3	ball	ball	NN	_	_	6	nsubj	_	_
	 * 4	is	be	VBZ	_	_	6	aux	_	_
     * 5	not	not	RB	_	_	6	neg	_	_
	 * 6	rolling	roll	VBG	_	_	_	_	_	_
	 * 7	into	into	IN	_	_	6	prep	_	_
	 * 8	a	a	DT	_	_	10	det	_	_
	 * 9	goal	goal	NN	_	_	10	nn	_	_
	 * 10	net	net	NN	_	_	7	pobj	_	_
	 * 11	.	.	.	_	_	6	punct	_	_
     * 
     *  @param aJCas the cas
     *  
     *  @return the tree in the CoNLL-X format
     */
    private String cas2CoNLLX(JCas aJCas) throws Exception {
    	
    	StringBuffer result = new StringBuffer();
    	
    	try { 
    	
	        // StringBuilder conllSb = new StringBuilder();
	        for (Sentence sentence : select(aJCas, Sentence.class)) {
	            // Map of token and the dependent (token address used as a Key)
	            Map<Integer, Integer> dependentMap = new HashMap<Integer, Integer>();
	            // Map of governor token address and its token position
	            Map<Integer, Integer> dependencyMap = new HashMap<Integer, Integer>();
	            // Map of governor token address and its dependency function value
	            Map<Integer, String> dependencyTypeMap = new HashMap<Integer, String>();
	
	            for (Dependency dependecny : selectCovered(Dependency.class, sentence)) {
	                dependentMap.put(dependecny.getDependent()
	                        .getAddress(), dependecny.getGovernor().getAddress());
	            }
	
	            int i = 1;
	            for (Token token : selectCovered(Token.class, sentence)) {
	                dependencyMap.put(token.getAddress(), i);
	                i++;
	            }
	
	            for (Dependency dependecny : selectCovered(Dependency.class, sentence)) {
	                dependencyTypeMap.put(dependecny.getDependent().getAddress(),
	                        dependecny.getDependencyType());
	            }
	
	            int j = 1;
	            for (Token token : selectCovered(Token.class, sentence)) {
	                String lemma = token.getLemma() == null ? "_" : token.getLemma().getValue();
	                String pos = token.getPos() == null ? "_" : token.getPos().getPosValue();
	                String dependent = "_";
	
	                if (dependentMap.get(token.getAddress()) != null) {
	                    if (dependencyMap.get(dependentMap.get(token.getAddress())) != null) {
	                        dependent = "" + dependencyMap.get(dependentMap.get(token.getAddress()));
	                    }
	                }
	                String type = dependencyTypeMap.get(token.getAddress()) == null ? "_"
	                        : dependencyTypeMap.get(token.getAddress());
	
	                if (dependentMap.get(token.getAddress()) != null
	                        && dependencyMap.get(dependentMap.get(token.getAddress())) != null
	                        && j == dependencyMap.get(dependentMap.get(token.getAddress()))) {
	                   // IOUtils.write(j + "\t" + token.getCoveredText() + "\t" + lemma + "\t" + pos
	                           // + "\t_\t_\t" + 0 + "\t" + type + "\t_\t_\n", aOs, aEncoding);
	                    result.append(j + "\t" + token.getCoveredText() + "\t" + lemma + "\t" + pos
	                            + "\t_\t_\t" + 0 + "\t" + type + "\t_\t_\n");
	                }
	                else {
	                    //IOUtils.write(j + "\t" + token.getCoveredText() + "\t" + lemma + "\t" + pos
	                          //  + "\t_\t_\t" + dependent + "\t" + type + "\t_\t_\n", aOs, aEncoding);
	                    result.append(j + "\t" + token.getCoveredText() + "\t" + lemma + "\t" + pos
	                            + "\t_\t_\t" + dependent + "\t" + type + "\t_\t_\n");
	                }
	                j++;
	            }
	            
	            //IOUtils.write("\n", aOs, aEncoding);
	            //System.out.print("\n");
	            result.append("\n");
	            
	        }
        
    	} catch (Exception e) {
		
    		throw new Exception(e.getMessage());
		
    	}
        
        return result.toString();
        
    }

    
    /**
     * Given a dependency tree it removes the punctuation (the punct marker in the
     * CoNLL-X file is used to recognize the punctuation). 
     * 
     * @param dependencyTree the tree
     * 
     * @return the tree in input without the punctuation
     * 
     */
    private String removePunctuation(String dependencyTree){

    	String cleaned_tree = "";
    	
    	Boolean hasChild = false;
    	String[] lines = dependencyTree.split("\n");
     
    	for (int i = 0; i < lines.length; i++) {
    		if(!lines[i].isEmpty()){
    			String[] fields = lines[i].split("\\s");
    			int tokenId = Integer.parseInt(fields[0]);
    				if(fields[7].equals("punct")){
    					//checking for children
    					for (int j = 0; j < lines.length; j++){
    						if(!lines[j].isEmpty()){
    							String[] fieldsj = lines[j].split("\\s");
    							if(fieldsj[6].equals(tokenId+"")){
    								hasChild = true;
    							}
    						}
    					}
    					//update stage
    					if (!hasChild) {
    						lines[i]="";
    						for (int j = 0; j < lines.length; j++){
    							if(!lines[j].isEmpty()){
    								String[] fieldsj = lines[j].split("\\s");
    								//updating the IDs for the deletion
    								if(Integer.parseInt(fieldsj[0]) >= tokenId){
    									fieldsj[0] = (Integer.parseInt(fieldsj[0])-1)+"";
    								}
    								//updating the heads. I assume that the root cannot be a punctuation mark
    								if(!fieldsj[6].equals("_") && Integer.parseInt(fieldsj[6]) > tokenId){
    									fieldsj[6] = (Integer.parseInt(fieldsj[6])-1)+"";
    								}
    								String line = "";
    								for (String field:fieldsj){
    									line+= field + "\t";
    								}
    								lines[j]=line;
    							}
    						}
    					}
    				}
    		}
    	}
    	for (int i = 0; i < lines.length; i++){
    		if(!lines[i].isEmpty())
    			cleaned_tree+=lines[i]+"\n";
    	}
    	
    	return cleaned_tree+"\n";
    	
    }
    
    
	/**
     * Given 2 tokens, token1 and token2 it says if there is a local ENTAILMENT or CONTRADICTION;
     * and in case the resource and the relation that the aligner used to say that. UNKNOWN is another
     * possible value;
     * 
     * @param token1
     * @param token2
     * 
     * @return an array of 2 values: 
     * 
     * 1) The type of alignment: LOCAL-ENTAILMENT, UNKNOWN, LOCAL-CONTRADICTION
     * 2) The resource and the relations used to create the alignment
     * 
     */
	private String[] getAlignmentType(FToken token1, FToken token2) {
    	
		String[] result = new String[2];
		
		Link alignment = alignments.get(token1.getForm() + "__" + token2.getForm());
		
		String alignmentType = null;;
		
		//ALIGNMENTS only when the dprel of the 2 tokens is the same.
		if (token1.getDeprel().equals(token2.getDeprel())) {
		
			//ENTAILMENT
		    if (token1.getLemma().equalsIgnoreCase(token2.getLemma()) ||
		    		userAlignments.contains(token1.getForm() + "_" +  token1.getPOS() + "\t" +
		    				token2.getForm() + "_" +  token2.getPOS())) {
		    			alignmentType = "LOCAL-ENTAILMENT";
		    			result[0] = alignmentType;
		    			result[1] = null;
		    } 
		    
		   //ENTAILMENT | UNKNOWN | CONTRADICTION
		    else if (alignment != null) {
		    	
		    	if (alignment.getDirectionString().equals("HtoT")) {
		    		result[0] = "UNKNOWN";
		    		result[1] = alignment.getLinkInfo() + ":" + alignment.getDirection();
		    	}
		    	else if (alignment.getLinkInfo().indexOf("ANTONYM") != -1) {
		    		result[0] = "LOCAL-CONTRADICTION";
		    		result[1] = alignment.getLinkInfo() + ":" + alignment.getDirection();
		    	}
		    	else  {
		    		result[0] = "LOCAL-ENTAILMENT";
		    		result[1] = alignment.getLinkInfo() + ":" + alignment.getDirection();
		    	}
		    	
		    }
		    	
		}
    		
    	return result;
    	
	}
    
	
	/**
	 * 
	 * Load the positive alignments between tokens from file. This allow users to load their own alignments.
	 * 
	 * @param fileName the file containing the positive alignments. The file has to be in the following
	 * format: token1_pos \t token2_pos, e.g.
	 *    
	 * animal_NN       dog_NN
     * beside_IN       near_IN
     *      
	 * @return the loaded alignments
	 * 
	 * @throws Exception
	 */
	private Set<String> loadUserAlignements(String fileName) throws Exception {
		
		Set<String> result = new HashSet<String>();
		
		if (fileName == null || fileName.equals(""))
			return result;
		
		try {
			
			File fileDir = new File(fileName);
	 
			BufferedReader in = new BufferedReader(
			   new InputStreamReader(
	                      new FileInputStream(fileDir), "UTF8"));
	 
			String str;
	 
			while ((str = in.readLine()) != null) {
				result.add(str);
			}
	 
	        in.close();
		} 
		catch (UnsupportedEncodingException e) 
		{
		    throw new Exception("Reading provided linked tokens error:" + e.getMessage());
		} 
		catch (IOException e) 
		{
			throw new Exception("Reading provided linked tokens error:" + e.getMessage());
		}
		catch (Exception e)
		{
			throw new Exception("Reading provided linked tokens error:" + e.getMessage());
		}
		
		return result;
		
	}
	
    
    /**
     * The <code>EditDistanceValue</code> class extends the DistanceValue
     * to hold the distance calculation result. 
     */
    private class EditDistanceValue extends DistanceValue {

    	public EditDistanceValue(double distance, boolean simBased, double rawValue)
    	{
    		super(distance, simBased, rawValue); 
    	}
    	
    }
   
    
    /**
	 * The class ScoreImpl define the method for the tree edit distance operations
	 * with their weights and basic logic.
	 */
    class ScoreImpl implements EditScore {
		
		private final LabeledTree tree1, tree2;
		
		public ScoreImpl(LabeledTree tree1, LabeledTree tree2, Map<String,Link> alignments) {
			
			this.tree1 = tree1;
			this.tree2 = tree2;
			
		}

		@Override
		public double replace(int node1, int node2) {
			
			//match
			FToken token_t = tree1.getToken(tree1.getLabel(node1));
			FToken token_h = tree2.getToken(tree2.getLabel(node2));
			//LOCAL-ENTAILMENT, LOCAL-CONTRADICTION, UNKNOWN
			String alignment = getAlignmentType(token_t, token_h)[0];
			if (alignment != null && alignment.equals("LOCAL-ENTAILMENT")) {
				return mMatchWeight; //return 0;
				
			} else { //replace
				return mSubstituteWeight; //return 1;
			}
		}
		
		@Override
		public double insert(int node2) {
			//return 3;
			return mInsertWeight; //return 1;
		}

		@Override
		public double delete(int node1) {
			//return 2;
			return mDeleteWeight; //return 1;
		}
		
	}
    
}