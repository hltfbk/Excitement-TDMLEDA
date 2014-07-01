package eu.excitementproject.eop.core.component.distance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;



import org.uimafit.util.JCasUtil;

import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import treedist.EditScore;
import treedist.Mapping;
import treedist.TreeEditDistance;
import treedist.TreeImpl;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.ART;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.CARD;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.CONJ;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.O;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.PP;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.PUNC;


import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;


import eu.excitement.type.alignment.Link;
import eu.excitementproject.eop.common.component.distance.DistanceCalculation;
import eu.excitementproject.eop.common.component.distance.DistanceComponentException;
import eu.excitementproject.eop.common.component.distance.DistanceValue;
import eu.excitementproject.eop.common.component.lexicalknowledge.LexicalResource;
import eu.excitementproject.eop.common.component.lexicalknowledge.LexicalResourceException;
import eu.excitementproject.eop.common.component.lexicalknowledge.LexicalRule;
import eu.excitementproject.eop.common.component.scoring.ScoringComponentException;
import eu.excitementproject.eop.common.configuration.CommonConfig;
import eu.excitementproject.eop.common.configuration.NameValueTable;
import eu.excitementproject.eop.common.exception.ComponentException;
import eu.excitementproject.eop.common.exception.ConfigurationException;
import eu.excitementproject.eop.common.representation.partofspeech.ByCanonicalPartOfSpeech;
import eu.excitementproject.eop.common.representation.partofspeech.PartOfSpeech;
import eu.excitementproject.eop.common.utilities.Utils;
import eu.excitementproject.eop.common.utilities.configuration.ImplCommonConfig;
import eu.excitementproject.eop.core.component.alignment.lexicallink.LexicalAligner;
import eu.excitementproject.eop.core.component.lexicalknowledge.germanet.GermaNetWrapper;
import eu.excitementproject.eop.core.component.lexicalknowledge.wikipedia.WikiExtractionType;
import eu.excitementproject.eop.core.component.lexicalknowledge.wikipedia.WikiLexicalResource;
import eu.excitementproject.eop.core.component.lexicalknowledge.wikipedia.it.WikiLexicalResourceIT;
import eu.excitementproject.eop.core.component.lexicalknowledge.wordnet.WordnetLexicalResource;
import eu.excitementproject.eop.core.utilities.dictionary.wordnet.WordNetRelation;
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
public abstract class FixedWeightTreeEditDistance implements DistanceCalculation {

	private static final String STOP_WORD_POS = "POS";
	private static final String STOP_WORD_LIST = "LIST";
	private static final String STOP_WORD_POS_LIST = "POS,LIST";
	private static final String IGNORE_CASE = "ignoreCase";
	private static final String NORMALIZATION_TYPE = "normalizationType";
	private static final String DEFAULT = "default";
	private static final String LONG = "long";
	private static final String PATH_STOP_WORD = "pathStopWordFile";
	private static final String STOP_WORD_TYPE = "stopWordRemoval";
	// Private Members
	private LexicalAligner aligner;
	private static final String UNUSED = "_";
    private List<Transformation> transformations;
	
	/**
	 * weight for match
	 */
    private double mMatchWeight;
    /**
	 * weight for delete
	 */
    private double mDeleteWeight;
    /**
	 * weight for insert
	 */
    private double mInsertWeight;
    /**
	 * weight for substitute
	 */
    private double mSubstituteWeight;
    /**
	 * the activated instance
	 */
    private String instances;
    /**
	 * the resource
	 */
    @SuppressWarnings("rawtypes")
	private List<LexicalResource> lexR;
    /**
	 * stop word removal
	 */
    private boolean stopWordRemovalPOS;
	private boolean ignoreCase;
    Set<WordNetRelation> relations = new HashSet<WordNetRelation>();
	private HashSet<String> ignoreSet = null;
	private String normalizationType;

    static Logger logger = Logger.getLogger(FixedWeightTreeEditDistance.class.getName());
    
    
    /**
     * Construct a fixed weight edit distance with the following constant
     * weights for edits:
     * match weight is 0, substitute, insert and delete weights are
     */
    @SuppressWarnings("rawtypes")
	public FixedWeightTreeEditDistance() {
    	
    	this.mMatchWeight = 0.0;
    	this.mDeleteWeight = 0.0;
    	this.mInsertWeight = 1.0;
    	this.mSubstituteWeight = 1.0;
    	this.lexR = new ArrayList<LexicalResource>();
        this.transformations = new ArrayList<Transformation>();
    	InitLexicalAligner();
    	
    }

    
    /** 
	 * Constructor used to create this object. 
	 * 
	 * @param config the configuration
	 * 
	 */
    public FixedWeightTreeEditDistance(CommonConfig config) throws ConfigurationException, ComponentException {
    
    	this();
        
    	logger.info("Creating an instance of " + this.getComponentName() + " ...");
    
        try {
        	
	        //get the platform name value table
	        NameValueTable platformNameValueTable = config.getSection("PlatformConfiguration");
	        
	       //get the platform language value
	        String language = platformNameValueTable.getString("language");
	        
	        //get the selected component
	    	NameValueTable componentNameValueTable = config.getSection(this.getClass().getCanonicalName());
    		
    	} catch (ConfigurationException e) {
    		throw new ComponentException(e.getMessage());
    	}
        
        logger.info("done.");
    	
    }
    

	/** 
	 * Constructor used to create this object. All the main parameters of the component are
	 * exposed in the constructor. Here is an example on how it can be used. 
	 * 
	 * <pre>
	 * {@code
	 * 
	 * //setting the weights of the edit distance operations
	 * double mMatchWeight = 0.0;
	 * double mDeleteWeight = 0.0;
	 * double mInsertWeight = 1.0;
	 * double mSubstituteWeight = 1.0;
	 * //enable stop words so that stop words will be removed.
	 * boolean stopWordRemoval = true;
	 * //the component has to work on a data set for Italian language
	 * String language = "IT";
	 * //setting the resources: wikipedia and wordnet will be used
	 * Map<String,String> resources = new HasMap<String,String>();
	 * resources.put("wordnet", "/tmp/wordnet/");
	 * resources.put("wikipedia", "jdbc:mysql://nathrezim:3306/wikilexresita#johnsmith#mypasswd");
	 * //creating an instance of the FixedWeightTokenEditDistance component
	 * FixedWeightEditDistance fwed = 
	 * new FixedWeightTokenEditDistance(mMatchWeight, mDeleteWeight, mInsertWeight, mSubstituteWeight, stopWordRemoval, language, resources)
	 * 
	 * }
	 * </pre>
	 * 
	 * @param mMatchWeight weight for match
     * @param mDeleteWeight weight for delete
     * @param mInsertWeight weight for insert
     * @param mSubstituteWeight weight for substitute
     * @param stopWordRemoval if stop words has to be removed or not; Possible values are: true, false
     * @param language the language of the data the component has to deal with; Possible values are: DE, EN, IT
     * @param resources the external resources the component has to use; it is a key/value pairs table.
     * The supported resources with their parameters are (reported as key/value pairs):
     * wordnet, path of the resource residing in the file system, e.g. /tmp/wordnet/
     * wikipedia, dbConnection#dbUser#dbPasswd, e.g. jdbc:mysql://nathrezim:3306/wikilexresita#johnsmith#mypasswd
     * 
	 */
    public FixedWeightTreeEditDistance(double mMatchWeight, double mDeleteWeight, double mInsertWeight, double mSubstituteWeight, boolean stopWordRemoval, String language, Map<String,String> resources) throws ConfigurationException, ComponentException {
    
    	this();
        
    	logger.info("Creating an instance of " + this.getComponentName() + " ...");
    
    	this.mMatchWeight = mMatchWeight;
    	this.mDeleteWeight = mDeleteWeight;
    	this.mInsertWeight = mInsertWeight;
    	this.mSubstituteWeight = mSubstituteWeight;
    	
		logger.info("done.");
    	
    }
    
    
    /**
	 * set the weight of the match edit distant operation
	 * 
	 * @param mMatchWeight the value of the edit distant operation
	 *
	 * @return
	 */
    public void setmMatchWeight(double mMatchWeight) {
    	
    	this.mMatchWeight = mMatchWeight;
    	
    }
    
    
    /**
	 * set the weight of the delete edit distant operation
	 * 
	 * @param mDeleteWeight the value of the edit distant operation
	 *
	 * @return
	 */
    public void setmDeleteWeight(double mDeleteWeight) {
    	
    	this.mDeleteWeight = mDeleteWeight;
    	
    }
    
    
    /**
	 * set the weight of the insert edit distant operation
	 * 
	 * @param mInsertWeight the value of the edit distant operation
	 *
	 * @return
	 */
    public void setmInsertWeight(double mInsertWeight ) {
    	
    	this.mInsertWeight = mInsertWeight;
    	
    }
    
    
    /**
	 * set the weight of the substitute edit distant operation
	 * 
	 * @param mSubstituteWeight the value of the edit distant operation
	 *
	 * @return
	 */
    public void setmSubstituteWeight(double mSubstituteWeight) {
    	
    	this.mSubstituteWeight = mSubstituteWeight;
    	
    }
   
    
    @Override
    public abstract String getComponentName();
    
    
    @Override
    public String getInstanceName() {
    	
    	return instances;
    	
    }
    
    public List<Transformation> getTransformations() {
    	
    	return this.transformations;
    	
    }
    
    
    /** 
	 * shutdown the resources
	 */
	public void shutdown() {
		
		logger.info(this.getComponentName() + " shutting down ...");
		
		try {
			for (int i = 0; i < lexR.size(); i++)
				lexR.get(i).close();
		} catch (Exception e) {
			logger.warning(e.getMessage());
    	}
		
		logger.info("done.");
		
	}
    
    
    @Override
    public DistanceValue calculation(JCas jcas) throws DistanceComponentException {

    	DistanceValue distanceValue = null;
    	
    	try {
    		//get the alignments between T and H
    		Map<String,String> alignments = getAlignments(jcas);
    		
    	    // get Text
	    	JCas tView = jcas.getView("TextView");
	    	//the dependency tree of t
	    	String t_tree = cas2CoNLLX(tView);
	    	System.out.println(t_tree);
	    	
	    	//the text fragment
	    	Fragment t_fragment = getFragment(t_tree);
	    	//List<Token> tTokensSequence = getTokenSequences(tView);
	    	
	    	// get Hypothesis
	    	JCas hView = jcas.getView("HypothesisView"); 
	    	//the dependency tree of t
	    	String h_tree = cas2CoNLLX(hView);
	    	//System.out.println(h_tree);
	    	

	    	//the text fragment
	    	Fragment h_fragment = getFragment(h_tree);
	    	//List<Token> hTokensSequence = getTokenSequences(hView);

	    	//distanceValue = distance(tTokensSequence, hTokensSequence);
	    	//distanceValue = distance(tTokensSequence, hTokensSequence);
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
    	// get Text
	    	JCas tView = jcas.getView("TextView");
	    	//the dependency tree of t
	    	String t_tree = cas2CoNLLX(tView);
	    	//the text fragment
	    	Fragment t_fragment = getFragment(t_tree);
	    	//List<Token> tTokensSequence = getTokenSequences(tView);
	    	
	    	// get Hypothesis
	    	JCas hView = jcas.getView("HypothesisView"); 
	    	//the dependency tree of t
	    	String h_tree = cas2CoNLLX(hView);
	    	//the text fragment
	    	Fragment h_fragment = getFragment(h_tree);
	    	//List<Token> hTokensSequence = getTokenSequences(hView);

	    	//distanceValue = distance(tTokensSequence, hTokensSequence);
	    	//distanceValue = distance(tTokensSequence, hTokensSequence);
	    	//distanceValue = distance(t_fragment, h_fragment);
    	 

     } catch (Exception e) {
     throw new ScoringComponentException(e.getMessage());
     }
    
     v.add(distanceValue.getDistance());
     v.add(distanceValue.getUnnormalizedValue());
    
     return v;
    }
    
    
    /*
     * Get the alignments between T and H by using the set aligner component
     * 
     * @param jcas the CAS containing T and H
     * 
     * @return key value pairs (e.g. assassin__killer --> WORDNET__3.0__HYPERNYM__0.5__TtoH) where key 
     * represents the tokens one in T and the other in H that have been alignment (e.g. token1_T__token5_H; 
     * a `__` character is used to separate the 2 tokens).
     * The value is instead the resource, the resource version, the relation that has been used to do the
     * alignment. Other information is about the strength of the alignment and the direction of the alignment
     * itself, e.g. WORDNET__3.0__HYPERNYM__0.5__TtoH
     * 
     */
    private Map<String,String> getAlignments(JCas jcas) {
		
    	Map<String,String> result = new HashMap<String,String>();
    	
		try {
			
			// Call the aligner to align T and H of pair 1
			logger.info("aligning ...");
			aligner.annotate(jcas);
			logger.info("done.");
						
			// Print the alignment of pair 1
			JCas hypoView = jcas.getView(LAP_ImplBase.HYPOTHESISVIEW);
			
			for (Link link : JCasUtil.select(hypoView, Link.class)) {
				
				logger.info(String.format("Text phrase: %s, " +
						"Hypothesis phrase: %s, " + 
						"id: %s, confidence: %f, direction: %s", 
						link.getTSideTarget().getCoveredText(),
						link.getHSideTarget().getCoveredText(),
						link.getID(), link.getStrength(),
						link.getDirection().toString()));
				
				String key = link.getTSideTarget().getCoveredText() + 
				        		"__" + 
				        		link.getHSideTarget().getCoveredText();
				        
				String value = link.getID() + 
				        		"__" + 
				        		link.getStrength() + 
				        		"__" +
				        		link.getDirection().toString();
				        
				//logger.info(key + " --> " + value);
				        
				result.put(key, value);
				
			}
			
		} catch (Exception e) {
			logger.info("Could not process the pair. " + e.getMessage());
		}
		
		return result;
	}
    
    
    /**
     * Returns a string based representation of the fixed weight
     * edit distance's parameters.
     *
     * @return string-based representation of this distance.
     */
    public String toString() {
    	
        StringBuilder sb = new StringBuilder();
        
        sb.append("costs:");
        sb.append("  match weight=" + mMatchWeight);
        sb.append("  insert weight=" + mInsertWeight);
        sb.append("  delete weight=" + mDeleteWeight);
        sb.append("  substitute weight=" + mSubstituteWeight);
        
        return sb.toString();
        
    }


    /**
     * Returns the constant weight of matching the specified token.
     *
     * @param tMatched token matched.
     * @return weight of matching token.
     */
    public double getmMatchWeight(Token tMatched) {
    	
        return mMatchWeight;
        
    }
    
    
    /**
     * Returns the constant weight of deleting the specified token.
     *
     * @param tDeleted token deleted.
     * @return weight of deleting token.
     */
    public double getmDeleteWeight(Token tDeleted) {
    	
        return mDeleteWeight;
        
    }

    
    /**
     * Returns the constant weight of inserting the specified token.
     *
     * @param tInserted token inserted.
     * @return weight of inserting token.
     */
    public double getmInsertWeight(Token tInserted) {

        return mInsertWeight;
        
    }

    
    /**
     * Returns the constant weight of substituting the inserted token for
     * the deleted token.
     *
     * @param tDeleted deleted token.
     * @param tInserted inserted token.
     * @return the weight of substituting the inserted token for
     * the deleted token.
     */
    public double getmSubstituteWeight(Token tDeleted, Token tInserted) {

        return mSubstituteWeight;
        
    }
    
    
    /**
     * Returns the weighted edit distance between T and H.
     * 
     * @param t
     * @param h
     * 
     * @return The edit distance between the sequences of tokens
     * 
     * @throws ArithmeticException
     * 
     */

    public DistanceValue distance(Fragment t, Fragment h, Map<String,String> alignments) throws ArithmeticException {

        	
    	//here we need to call the library for calculating tree edit distance
    	
    	double distance = 0.0;
    	double norm = 1.0;
    	
    	//Creating the Tree of Text
    	LabeledTree t_tree = createTree(t);
    	//System.out.println("t:" + t_tree);

    	
    	//Creating the Tree of Hypothesis
    	LabeledTree h_tree = createTree(h);
		//System.out.println("h:" + h_tree);
		
		
		TreeEditDistance dist = new TreeEditDistance(new ScoreImpl(t_tree, h_tree, alignments));
		Mapping map = new Mapping(t_tree, h_tree);
		distance = dist.calc(t_tree, h_tree, map);
		System.out.println("tree edit distance:" + distance);
	    System.out.println("sequence of operations to transform t into h:");
	    
	    List<String> operationSequence = map.getSequence();
	    for (int g = 0; g < operationSequence.size(); g++) {
	    	String operation = (String)operationSequence.get(g);
	    	//e.g. rep:2,3 rep:1,1 rep:0,0 ins:2 rep:3,4 rep:4,5
	    	//System.err.print(operation + " ");
	    	String operationName = operation.split(":")[0];
	    	String nodes = operation.split(":")[1];
	    	if (operationName.contains("rep")) {
		    	int node1 = Integer.parseInt(nodes.split(",")[0]);
		    	int node2= Integer.parseInt(nodes.split(",")[1]);
		    	FToken token1 = t_tree.getToken(node1);
		    	FToken token2 = h_tree.getToken(node2);
		    	String alignmentType = null;
		    	if (token1.getLemma().equalsIgnoreCase(token2.getLemma())) {
		    		alignmentType = "lexical";
		    		operationName = "match";
		    	}
		    	else if (alignments.get(token1.getLemma() + "__" + token2.getLemma()) != null) {
		    		alignmentType = alignments.get(token1.getLemma() + "__" + token2.getLemma());
		    		operationName = "match";
		    	}
		    	//Silvia here we need to create an object Transformation containing the following
		    	//information
		    	System.err.println("operation:" + operationName + " " + "alignment:" + alignmentType + " token_T:" + token1 + " token_H:" + token2);
	    	}
	    	else if (operationName.contains("ins")){
	    		int node = Integer.parseInt(nodes);
		    	FToken token = h_tree.getToken(node);
		    	//Silvia here we need to create an object Transformation containing the following
		    	//information
		    	System.err.println("operation:" + operationName + " token_H:" + token);
	    	}
	    	else { //deletion
	    		int node = Integer.parseInt(nodes);
		    	FToken token = t_tree.getToken(node);
		    	//Silvia here we need to create an object Transformation containing the following
		    	//information
		    	System.err.println("operation:" + operationName + " token_T:" + token);
	    	}
	    	
	    	//Silvia here we need to add the created tranformation
	    	//transformations.add(the created transformation);
	    		
	    }
		
    	double normalizedDistanceValue = distance/norm;
    	
    	return new EditDistanceValue(normalizedDistanceValue, false, distance);
                
     }
    
    
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
    
    
	private boolean compare(String tokenBaseForm, String tokenBaseForm2) {
		if(ignoreCase){
			return tokenBaseForm.equalsIgnoreCase(tokenBaseForm2);
		}	
		return tokenBaseForm.equals(tokenBaseForm2);
	}

	/**
     * Returns the token base form. It is the form of the token
     * or its lemma.
     *
     * @param token the token
     * 
     * @return the base form of the token
     * 
     */
    public abstract String getTokenBaseForm(Token token);
    
    
    
    // this method accept in input a tree (it has been produced by cas2CoNLLX
    // and return a fragment containing all the tokens in the tree
    private Fragment getFragment(String dependencyTree) {
    	System.out.println(dependencyTree);
    	Fragment fragment = null;
    	
    	//here we need to parse the tree CoNLLX format (i.e. dependencyTree)
    	//and for each line of it we would need to create an object of the class Token
    	//and the put it into the Fragment
    	try{
    		fragment = new Fragment();

    	//and for each line of it we would need to create an object of the class FToken
    	//and then put it into the Fragment
    	

    	String[] lines = dependencyTree.split("\n");
    	for (int i = 0; i < lines.length; i++) {
    		String[] fields = lines[i].split("\\s");
    		//for (int j = 0; j < fields.length; j++) {
    			int tokenId = Integer.parseInt(fields[0]) - 1;
    			String form = fields[1];

    			String lemma = fields[2];	
    			String pos = fields[3];	
    			int head;
    			if (fields[6].equals("_")) {
    				head = -1;
    				//System.out.println("====================================");
    			}
    			else
    				head = Integer.parseInt(fields[6]) - 1;
    			
    			String deprel = fields[7];
    			FToken token_i = new FToken(tokenId, form, lemma, pos, head, deprel);

    			fragment.addToken(token_i);
    			
    		//}
    	}
    	}
    	catch (Exception e) {
    		System.err.println(e.getMessage());
    		System.exit(0);
    	}
    	return fragment;
    	
    }
    
    

    /*
     * Given a cas (it contains the T view or the H view) in input it produces a
     * String containing the tree in the CoNLL-X format
     */
    public String cas2CoNLLX(JCas aJCas) throws IOException {
    	
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
	 * Initialize the lexical aligner and prepare the tests
	 */
	public void InitLexicalAligner() {
		
		try {
			
			// Create and initialize the aligner
			logger.info("Initialize the Lexical Aligner");
			File configFile = new File(
					"src/test/resources/configuration-file/LexicalAligner_TreeEditDistance_EN.xml");
			ImplCommonConfig commonConfig = new ImplCommonConfig(configFile);
			aligner = new LexicalAligner(commonConfig);
			
		} catch (Exception e) {
			 
			logger.info("Failed initializing the LexicalAligner" + 
					e.getMessage());
		}
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
		private final Map<String,String> alignments;

		public ScoreImpl(LabeledTree tree1, LabeledTree tree2, Map<String,String> alignments) {
			
			this.tree1 = tree1;
			this.tree2 = tree2;
			this.alignments = alignments;
			
		}

		@Override
		public double replace(int node1, int node2) {
			
			
			if ( (tree1.getLabel(node1) == tree2.getLabel(node2)) && 
			tree1.getToken(node1).getLemma().equalsIgnoreCase(tree2.getToken(node2).getLemma())) {
				return 0;
			} else {
				//return 4;
				return 1;
			}
		}
		
		@Override
		public double insert(int node2) {
			//return 3;
			return 1;
		}

		@Override
		public double delete(int node1) {
			//return 2;
			return 1;
		}
		
	}
    
    class Fragment {
    	
    	private ArrayList<FToken> tokens;
    	
    	public Fragment(){
    		this.tokens = new ArrayList<FToken>();
    	}
    	
    	public Fragment(ArrayList<FToken> tokens){
    		this.tokens=tokens;
    	}
    	
    	public FToken getToken(int tokenId){
    		return tokens.get(tokenId-1);
    	}
    	
    	public int size(){
    		return tokens.size();
    	}
    	
    	public void addToken (FToken token){
    		tokens.add(token);
    	}
    	
    	public void addTokens(ArrayList<FToken> addedTokens){
    		
    	}
    	
    	public Iterator<FToken> getIterator(){
    		return tokens.iterator();
    	}
    		
    }
    
    class FToken {

    	//ID Token counter, starting at 1 for each new sentence.
    	private int id;
    	//FORM 	Word form or punctuation symbol.
    	private String form;
    	//LEMMA Lemma or stem (depending on particular data set) of word form, or an underscore if not available. 
    	private String lemma;
    	//STEM; not in CoNLL
    	private String stem;
    	//PoS
    	private String pos;
    	//HEAD 	Head of the current token, which is either a value of ID or zero ('0'). 
     	//Note that depending on the original treebank annotation, there may be multiple tokens with an ID of zero.
    	private int head;
    	//DEPREL Dependency relation to the HEAD. The set of dependency relations depends on the particular language. 
    	//Note that depending on the original treebank annotation, the dependency relation may be meaningfull or simply 'ROOT'.
    	private String deprel;
    	
    	//dprel relations from the current token to the root
    	private String deprelRelations;
    	
    	public FToken(int id, String form, String lemma, String pos, int head, String deprel) {
    		
    		this.id = id;
    		this.form = form;
    		this.lemma= lemma;
    		this.pos = pos;
    		//this.stem = stem;
    		this.head = head;
    		this.deprel = deprel;
    		//the relation (deprel) moving from the node containing the token and
    		//the root of the tree.
    		this.deprelRelations = null;
    	}
    	
    	public int getId() {
    		
    		return this.id;
    		
    	}
    	
    	public String getForm() {
    		
    		return this.form;
    		
    	}
    	
    	public String getLemma() {
    		
    		return this.lemma;
    		
    	}
    	
    	/*
    	public String getStem() {
    		
    		return this.stem;
    		
    	}
    	*/
    	
    	public String getPOS() {
    		
    		return this.pos;
    		
    	}
    	
    	public int getHead() {
    		
    		return this.head;
    		
    	}
    	
    	public String getDeprel() {
    		
    		return this.deprel;
    		
    	}
    	
       public void setDeprelRelations(String deprelRelations) {
    		
    		this.deprelRelations = deprelRelations;
    		
    	}
       
       public String getDeprelRelations() {
    		
    		return this.deprelRelations;
    		
    	}
       
       public String toString() {
    		
    		return this.id + "__" + 
    			   this.form + "__" + 
    		       this.lemma + "__" + 
    		       this.pos + "__" + 
    			   //this.stem + ":" +
    		       this.head + "__" + 
    			   this.deprel + "__" + 
    		       this.deprelRelations;
    		
    	}
    	
    }
    
    class LabeledTree extends TreeImpl {
    	
    	private int[] labels;
    	private FToken[] tokens;
    	
    	public LabeledTree(int[] parents, int[] labels) {
    		super(parents);

    		if (parents == null || labels == null)
    			throw new NullPointerException();
    		if (parents.length != labels.length)
    			throw new IllegalArgumentException();
    		
    		this.labels = labels;
    	}
    	
    	public LabeledTree(int[] parents, int[] labels, FToken[] tokens) {
    		super(parents);

    		if (parents == null || labels == null || tokens == null)
    			throw new NullPointerException();
    		if (parents.length != labels.length || parents.length != tokens.length)
    			throw new IllegalArgumentException();
    		
    		this.labels = labels;
    		this.tokens = tokens;
    		getDeprelRelationsFromNodeToRoot();
    		
    	}

    	public int getLabel(int nodeId) {
    		
    		return labels[nodeId];
    		
    	}
    	
    	public FToken getToken(int nodeId) {
    		
    		return tokens[nodeId];
    		
    	}
    	
        public FToken[] getTokens() {
    		
    		return this.tokens;
    		
    	}
        

        //given a tree and a node it return the path from the node to the root of the tree
        private void getDeprelRelationsFromNodeToRoot() {
    		
        	//store the path of each node containing the token from the node to the root of the tree
        	for (int z = 0; z < this.tokens.length; z++) {
        		FToken token_z = this.tokens[z];
        		String relations = "";
        		int nodeId = token_z.getId();
        		//System.out.println("node:" + nodeId);
        		while (nodeId != -1) {
        			String deprel = this.tokens[nodeId].getDeprel();
        			if (relations.length() == 0)
        				relations = deprel;
        			else
        				relations = relations + "#" + deprel;
        			nodeId = this.getParent(nodeId);
        			//System.out.print("====" + nodeId);
        		}
        		System.out.println();
        		token_z.setDeprelRelations(relations);
        	}
    		
    	}
        
    }

    
}