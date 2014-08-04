package eu.excitementproject.eop.core.component.distance;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringList;
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
 * The <code>FixedWeightedEditDistance</code> class implements the DistanceCalculation interface.
 * Given a pair of T-H, each of them represented as a sequences of tokens, the edit distance between 
 * T and H is the minimum number of operations required to convert T to H. 
 * FixedWeightedEditDistance implements the simplest form of weighted edit distance that simply uses a 
 * constant cost for each of the edit operations: match, substitute, insert, delete.
 * 
 * <h4>Relation to Simple Edit Distance</h4>
 * Weighted edit distance agrees with edit distance as a distance assuming the following weights:
 * match weight is 0, substitute, insert and delete weights are <code>1</code>.
 *
 * <h4>Symmetry</h4>
 * If the insert and delete costs of a character are equal, then weighted edit distance will be 
 * symmetric.  
 * 
 * <h4>Metricity</h4>
 * If the match weight of all tokens is zero, then the distance between a token sequence
 * and itself will be zero.  
 * 
 * @author Roberto Zanoli
 * 
 */
public class FixedWeightTreeEditDistance implements DistanceCalculation {

	/**
	 * The aligner component that finds the alignments between the tokens in T and those in H
	 */
	private LexicalAligner aligner;
	/**
	 * The transformations converting T into H
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
     *  the logger
     */
    static Logger logger = Logger.getLogger(FixedWeightTreeEditDistance.class.getName());
    
    /**
     * Construct a fixed weight edit distance
     */
	public FixedWeightTreeEditDistance() {
    	
        this.transformations = new ArrayList<Transformation>();
        this.instances = null;
        this.aligner = null;
        
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
	    	logger.info("using:" + instances);
	    	
	    	//get the instance configuration
	    	NameValueTable instanceNameValueTable = 
	    			config.getSubSection(this.getClass().getCanonicalName(), instances);
	    	
	    	//get the aligner component configuration
			String componentName = 
					instanceNameValueTable.getString("alignment-component");
			logger.info("using:" + componentName);
			
			//get the configuration file of the aligner component
			String cofigurationFile = 
					instanceNameValueTable.getString("configuration-file");
			logger.info("component configuration file:" + cofigurationFile);
			
			//create an instance of the aligner component
	    	if (this.aligner == null) {
				
				try {
					
					Class<?> componentClass = Class.forName(componentName);
					Constructor<?> componentClassConstructor = componentClass.getConstructor(CommonConfig.class);
					File configFile = new File(cofigurationFile);
					ImplCommonConfig commonConfig = new ImplCommonConfig(configFile);
					this.aligner = (LexicalAligner) componentClassConstructor.newInstance(commonConfig);
					
				} catch (Exception e) {
					throw new ComponentException(e.getMessage());
				}
				
			}
	    	
    	} catch (ConfigurationException e) {
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
     * Get the transformations needed to transform T into H
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
		
		this.aligner.cleanUp();
		this.transformations =  null;		 
		this.instances = null;
		
		logger.info("done.");
		
	}
    
    
    @Override
    public DistanceValue calculation(JCas jcas) throws DistanceComponentException {

    	DistanceValue distanceValue = null;
    	
    	try {
    		
    		//get the alignments between T and H produced by the aligner component
    		Map<String,Link> alignments = getAlignments(jcas);
    	    //get the Text
	    	JCas tView = jcas.getView(LAP_ImplBase.TEXTVIEW);
	    	//get the dependency tree of Text
	    	String t_tree = cas2CoNLLX(tView);
	    	//TODO
	    	logger.info("\nThe Tree of Text:\n" + t_tree);
	    	
	       	t_tree = mergeTrees(t_tree);
	    	logger.info("\nThe Merged Tree of Text:\n" + t_tree);
	    	
	    	t_tree = removePunctuation(t_tree);
	    	logger.info("\nThe Cleaned Tree of Text:\n" + t_tree);
	    	//create the Text fragment
	    	Fragment t_fragment = getFragment(t_tree);
	    	//get the Hypothesis
	    	JCas hView = jcas.getView(LAP_ImplBase.HYPOTHESISVIEW); 
	    	//the dependency tree of Hypothesis
	    	String h_tree = cas2CoNLLX(hView);
	    	logger.info("\nThe Tree of Hypothesis:\n" + h_tree);
	    	
	       	h_tree = mergeTrees(h_tree);
	       	logger.info("\nThe Merged Tree of Hypothesis:\n" + h_tree);
	       	
	       	h_tree = removePunctuation(h_tree);
	    	logger.info("\nThe Cleaned Tree of Hypothesis:\n" + h_tree);
	    	//create the Hypothesis fragment
	    	Fragment h_fragment = getFragment(h_tree);
            //calculate the distance between T and H by using the matches
	    	//provided by the aligner component.
	    	distanceValue = distance(t_fragment, h_fragment, alignments);
	    	
    	} catch (Exception e) {
    		
    		e.printStackTrace();
    		System.exit(0);
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
    		Map<String,Link> alignments = getAlignments(jcas);
	 	   	// get Text
		    JCas tView = jcas.getView(LAP_ImplBase.TEXTVIEW);
		    //get the dependency tree of Text
		    String t_tree = removePunctuation(cas2CoNLLX(tView));
		    logger.info("Text:\n" + t_tree);
		    //TODO
		    
		    t_tree = mergeTrees(t_tree);
		    logger.info("Merged text:\n" + t_tree);
		    
		    t_tree = removePunctuation(t_tree);
		    logger.info("Cleaned text:\n" + t_tree);
		    //create the Text fragment
		    Fragment t_fragment = getFragment(t_tree);
		    //get Hypothesis
		    JCas hView = jcas.getView(LAP_ImplBase.HYPOTHESISVIEW); 
		    //the dependency tree of Hypothesis
		    String h_tree = cas2CoNLLX(hView);
		    logger.info("Hypothesis:\n" + h_tree);
		    
		    h_tree = mergeTrees(h_tree);
		    logger.info("Merged hypothesis:\n" + h_tree);
		    
		    h_tree = removePunctuation(h_tree);
		    logger.info("Cleaned hypothesis:\n" + h_tree);
		    
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
     * values are instead the resource, the resource version, the relation that has been used to do the 
     * alignment. Other information is about the strength of the alignment and the direction of the alignment
     * itself, e.g. WORDNET__3.0__HYPERNYM__0.5__TtoH
     * 
     */
    private Map<String,Link> getAlignments(JCas jcas) throws Exception {
		
    	Map<String,Link> result = new HashMap<String,Link>();
    	
		try {
			
			// Call the aligner component to align T and H
			logger.info("\ngetting the alignments ...");
			aligner.annotate(jcas);
			logger.info("done.");
						
			logger.info("\n\nalignments list:\n");
			
			//get the HYPOTHESIS view
			JCas hypoView = jcas.getView(LAP_ImplBase.HYPOTHESISVIEW);
			
			//cycle through the alignments
			for (Link link : JCasUtil.select(hypoView, Link.class)) {
				
				logger.info(String.format("\nText phrase: %s, " +
						"Hypothesis phrase: %s, " + 
						"id: %s, confidence: %f, direction: %s", 
						link.getTSideTarget().getCoveredText(),
						link.getHSideTarget().getCoveredText(),
						link.getID(), link.getStrength(),
						link.getDirection().toString())); 
				
				String key = link.getTSideTarget().getCoveredText() + 
				        		"__" + 
				        		link.getHSideTarget().getCoveredText();
				     
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
    public DistanceValue distance(Fragment t, Fragment h, Map<String,Link> alignments) throws ArithmeticException {

    	//here we need to call the library for calculating tree edit distance
    	double distance = 0.0;
    	double norm = 1.0;
    	
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
		
		try {
		
		
	    
        this.transformations = new ArrayList<Transformation>();
        
	    //cycle through the list of the edit distance operations (i.e. replace -rep, 
        //insertion -ins, deleletion -del)
	    //operations are in the format: rep:2,3 rep:1,1 rep:0,0 ins:2 rep:3,4 rep:4,5
        //e.g. rep:2,3 means replacing node id_2 with node id_3
	    List<String> operationSequence = map.getSequence();
	    
	    logger.info("\ntree edit distance:" + distance);
		logger.info("\ntransformations:" + operationSequence.size());
	    logger.info("\nlist of transformations:");
	    
	    for (int i = 0; i < operationSequence.size(); i++) {
	    	String operation_i = (String)operationSequence.get(i);
	    	//System.err.print(operation_i + " ");
	    	String transformationType = operation_i.split(":")[0];
	    	String nodes = operation_i.split(":")[1];
	    	Transformation trans = null;
	    	//case of replace operations; the library we use for tree edit distance doesn't distinguish 
	    	//between replace and match operations. Distinguish between replace and match is done in this way: 
	    	//
	    	//match: 
	    	//  -- lexical match between tokens
	    	//  -- positive alignments
	    	//
	    	//replace:
	    	//  -- no lexical match between tokens
	    	//  -- negative alignments or no alignments
	    	//
	    	if (transformationType.contains(Transformation.REPLACE)) {
		    	int node1 = Integer.parseInt(nodes.split(",")[0]);
		    	int node2= Integer.parseInt(nodes.split(",")[1]);
		    	FToken t_token = t_tree.getToken(node1);
		    	FToken h_token = h_tree.getToken(node2);
		    	Link alignment = getAlignment(t_token, h_token, alignments);
		    	//LOCAL-ENTAILMENT, LOCAL-CONTRADICTION
		    	String alignmentMatchType = getAlignmentType(t_token, h_token, alignment);
		    	
		    	if (alignmentMatchType == null) {
		    		trans = new Transformation(Transformation.REPLACE, alignmentMatchType, t_token, h_token);
		    	}
		    	else if (alignmentMatchType.equals("LOCAL-CONTRADICTION")) {
		    		trans = new Transformation(Transformation.REPLACE, alignmentMatchType, t_token, h_token);
		    	}
		    	else if (alignmentMatchType.equals("LOCAL-ENTAILMENT")) {
		    		trans = new Transformation(Transformation.MATCH, alignmentMatchType, t_token, h_token);
		    	}
		    	
			    transformations.add(trans);
			    
			    logger.info("\n" + trans.toString());
	    	}
	    	//case of insertion
	    	else if (transformationType.contains(Transformation.INSERTION)){
	    		int node = Integer.parseInt(nodes);
		    	FToken token = h_tree.getToken(node);
		    	trans = new Transformation(transformationType, token);
		    	transformations.add(trans);
		    	logger.info(trans.toString());
	    	}
	    	//case of deletion
	    	else {
	    		int node = Integer.parseInt(nodes);
		    	FToken token = t_tree.getToken(node);
		    	trans = new Transformation(transformationType, token);
		    	transformations.add(trans);
		    	logger.info(trans.toString());
	    	}
	    	
	    }
	    
    	// norm is the distance equivalent to the cost of inserting all the nodes in H and deleting
    	// all the nodes in T. This value is used to normalize distance values.
    	norm = (double)(t_tree.size() * this.mDeleteWeight + h_tree.size() * this.mInsertWeight);
    	
    	double normalizedDistanceValue = distance/norm;
    	
    	} catch (Exception e) 
    	{ e.printStackTrace(); System.exit(0);}
    	
    	//return new EditDistanceValue(normalizedDistanceValue, false, distance);
    	return new EditDistanceValue(distance, false, distance);
	    
    }
    
    /**
     * Create a tree
     */
    private LabeledTree createTree(Fragment f) {
    	
    	LabeledTree lTree;
    	
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
    	
    	lTree = new LabeledTree( //
    	    	//the parents of the nodes
    	    		parents,
    	    		//the ids of the tokens
    	    		ids,
    	    		//the tokens with all their information
    	    		tokens);
    	
    	return lTree;
    	
    }
    
    
    /**
     * this method accepts in input a tree (it has been produced by cas2CoNLLX
     * and it is in CoNLL-X format and returns a fragment containing all the tokens in the tree
     * 
     * @param dependencyTree
     * 
     * @return the fragment
     *
     * @throws Exception
     */
    private Fragment getFragment(String dependencyTree) throws Exception {
    	
    	//System.out.println(dependencyTree);
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
 //TODO   
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
   
   //TODO
   private String mergeTrees(String multiTree){
	   String[] trees = multiTree.split("\n\n");
	   String newTree = "";
	   //add new node
	   newTree+="1\t_\t_\t_\t_\t_\t_\t_\t_\t_\n";
	   int prevtreelenght = 1;
	   for (int i = 0; i < trees.length; i++){
		   	String tree = trees[i];
		   	String[] lines = tree.split("\n");
		   	for(int j = 0; j<lines.length; j++){
			   	String[] fields = lines[j].split("\\s");
	   			int tokenId = Integer.parseInt(fields[0]);
	   			fields[0] = (tokenId + prevtreelenght) + "";
	   			if(fields[6].equals("_")){
		   	    	fields[6] = "1";
	   			}
	   			else {
	   				fields[6] = (Integer.parseInt(fields[6]) + prevtreelenght) + "";
	   			}
	   			String line = "";
    			for (String field:fields){
    				line+= field + "\t"; 
    			}
    			lines[j]=line;
    			newTree+=line+"\n";
    			
		   	}
		   	prevtreelenght+=lines.length;
	   }
	   return newTree;
   }
    
    /**
     * Given a cas (it contains the T view or the H view) in input it produces a
     * string containing the tree in the CoNLL-X format
     * 
     *  @param aJCas the cas
     *  
     *  @return the tree in the CoNLL-X format
     */
   
    private String cas2CoNLLX(JCas aJCas) {
    	
    	StringBuffer result = new StringBuffer();
    	
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
        
        return result.toString();
        
    }

    
	/**
     * Given 2 tokens, token1 and token2, it says if the 2 token match.
     * The match is calculated on the bases of the tokens lemma.
     * 
     * @param token1
     * @param token2
     * 
     * @return if the 2 tokens match
     */
	private String getAlignmentType(FToken token1, FToken token2, Link alignment) {
    	
		String alignmentLabel = null;;
		
		if (token1.getDeprel().equals(token2.getDeprel())) {
		
	    	if (token1.getLemma().equalsIgnoreCase(token2.getLemma())) {
	    		alignmentLabel = "LOCAL-ENTAILMENT";
	    		return alignmentLabel;
	    	}
	    	else if (alignment != null) {
	    		//alignmentLabel = alignment.getGroupLabel();
	    		//to be completed when the getGroupLabel will be implemented
	    		//alignmentLabel = "LOCAL-CONTRADICTION";
	    		 if (alignment.getDirectionString().equals("TtoH"))
	    			 alignmentLabel = "LOCAL-ENTAILMENT";
	    		 else if (alignment.getDirectionString().equals("HtoT"))
	    			 alignmentLabel = "LOCAL-CONTRADICTION";
	    		return alignmentLabel;
	    	}
	    	
		}
    		
    	return alignmentLabel;
    	
	}
    	
	
	/**
	 * Given 2 tokens, token1 and token2, it says if between the tokens
	 * there is a positive or negative alignement.
     * 
	 * @param token1 token1
	 * @param token2 token2
	 * @param alignments the alignments
	 * 
	 * @return if the 2 tokens match
	 */
	private Link getAlignment(FToken token1, 
			FToken token2, Map<String,Link> alignments) {
    	
		Link alignment = alignments.get(token1.getLemma() + "__" + token2.getLemma());
		
		return alignment;
	    
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
   
    
    class ScoreImpl implements EditScore {
		
		private final LabeledTree tree1, tree2;
		private final Map<String,Link> alignments;
		
		public ScoreImpl(LabeledTree tree1, LabeledTree tree2, Map<String,Link> alignments) {
			
			this.tree1 = tree1;
			this.tree2 = tree2;
			this.alignments = alignments;
			
		}

		@Override
		public double replace(int node1, int node2) {
			
			Link alignment = getAlignment(tree1.getToken(node1), tree2.getToken(node2), alignments);
			
			String alignmentMatchType = getAlignmentType(tree1.getToken(tree1.getLabel(node1)), tree2.getToken(tree2.getLabel(node2)), alignment);
			
			if (alignmentMatchType != null && alignmentMatchType.equals("LOCAL-ENTAILMENT")) 
			
			//if ( //tree1.getLabel(node1) == tree2.getLabel(node2) )// && 
				//tree1.getToken(tree1.getLabel(node1)).equals(tree2.getLabel(node2)))
				
					//(tree1.getToken(tree1.getLabel(node1)).getLemma().equals(tree2.getToken(tree2.getLabel(node2)).getLemma())))
					
				//getAlignmentMatchType(tree1.getToken(tree1.getLabel(node1)), tree2.getToken(tree2.getLabel(node2)), alignment).equals("LOCAL-ENTAILMENT"))
			//tree1.getToken(node1).getLemma().equalsIgnoreCase(tree2.getToken(node2).getLemma())) 
					{
				
				//System.err.println(tree1.getLabel(node1) + "\t" +  tree2.getLabel(node2));
				//System.err.println(tree1.getToken(tree1.getLabel(node1)) + "\t" + tree2.getToken(tree2.getLabel(node2)));
				//System.err.println("==============================");
				
				return 0;
				//return mMatchWeight;
			} else {
				//return 4;
				return 1;
				//return mSubstituteWeight;
			}
		}
		
		@Override
		public double insert(int node2) {
			//return 3;
			//return 1;
			return mInsertWeight;
		}

		@Override
		public double delete(int node1) {
			//return 2;
			//return 1;
			return mDeleteWeight;
		}
		
	}
    
}