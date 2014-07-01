package eu.excitementproject.eop.core;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.logging.Logger;
import java.io.*;
import java.lang.reflect.Constructor;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.JCas;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import org.w3c.dom.*;

import weka.core.Attribute;
import weka.core.FastVector;

import eu.excitementproject.eop.common.DecisionLabel;
import eu.excitementproject.eop.common.EDABasic;
import eu.excitementproject.eop.common.EDAException;
import eu.excitementproject.eop.common.TEDecision;
import eu.excitementproject.eop.common.component.distance.DistanceComponentException;
import eu.excitementproject.eop.common.component.distance.DistanceValue;
import eu.excitementproject.eop.common.configuration.CommonConfig;
import eu.excitementproject.eop.common.configuration.NameValueTable;
import eu.excitementproject.eop.common.exception.ComponentException;
import eu.excitementproject.eop.common.exception.ConfigurationException;
import eu.excitementproject.eop.core.component.distance.*;
import eu.excitement.type.entailment.Pair;
import eu.excitementproject.eop.lap.PlatformCASProber;


/**
 * The <code>EditDistanceEDA</code> class implements the <code>EDABasic</code> interface.
 * Given a certain configuration, it can be trained over a specific data set in order to optimize its
 * performance. In the training phase this class produces a distance model for the data set, which
 * includes a distance threshold that best separates the positive and negative examples in the training data.
 * During the test phase it applies the calculated threshold, so that pairs resulting in a distance below the
 * threshold are classified as ENTAILMENT, while pairs above the threshold are classified as NONENTAILMENT.
 * <code>EditDistanceEDA</code> uses <code>FixedWeightEditDistance</code> for calculating edit distance
 * between each pair of T and H.
 * 
 * @author Roberto Zanoli
 * 
 */
public class TransformationDrivenEDA<T extends TEDecision>
		implements EDABasic<EditDistanceTEDecision> {
	
	/**
	 * the accuracy obtained on the training data set
	 */
	private double trainingAccuracy;
	
	/**
	 * the edit distance component to be used
	 */
	private FixedWeightTreeEditDistance component;
	
	/**
	 * the logger
	 */
	static Logger logger = Logger.getLogger(EditDistanceEDA.class.getName());

	/**
	 * the language
	 */
	private String language;

	/**
	 * the training data directory
	 */
	private String trainDIR;

	/**
	 * the test data directory
	 */
	private String testDIR;

	/**
	 * if the model produced during the training phase
	 * has to be saved into the configuration file itself or
	 * if it should stay in the memory for further calculation 
	 * (e.g. EditDistancePSOEDA uses this modality)
	 */
	private boolean writeModel;
	
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
	 * measure to optimize: accuracy or f1 measure
	 */
    private String measureToOptimize;
    
	/**
	 * if the EDA has to write the learnt model at the end of the training phase
	 * 
	 * @param write true for saving the model
	 *
	 * @return
	 */
	public void setWriteModel(boolean write) {
		
		this.writeModel = write;
		
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
	 * get the weight of the match edit distant operation
	 * 
	 * @return the weight
	 */
	public double getmMatchWeight() {
    	
    	return this.mMatchWeight;
    	
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
	 * get the weight of the delete edit distant operation
	 * 
	 * @return the weight
	 */
	public double getmDeleteWeight() {
    	
    	return this.mDeleteWeight;
    	
    }
    
    /**
	 * set the weight of the insert edit distant operation
	 * 
	 * @param mInsertWeight the value of the edit distant operation
	 *
	 * @return
	 */
    public void setmInsertWeight(double mInsertWeight) {
    	
    	this.mInsertWeight = mInsertWeight;
    	
    }
    
    /**
	 * get the weight of the insert edit distant operation
	 * 
	 * @return
	 */
    public double getmInsertWeight() {
    	
    	return this.mInsertWeight;
    	
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
    
    /**
	 * get the weight of the insert edit distant operation
	 * 
	 * @return
	 */
    public double getmSubstituteWeight() {
    	
    	return this.mSubstituteWeight;
    	
    }
    
    /**
	 * set the measure to be optimize during the training phase
	 * 
	 * @param measureToOptimize the measure to be optimized
	 * 
	 * @return
	 */
    public void setMeasureToOptimize(String measureToOptimize) {
    	
    	this.measureToOptimize = measureToOptimize;
    	
    }
    
    /**
	 * get the measure to be optimize during the training phase
	 * 
	 * @return the name of the measure
	 */
    public String getMeasureToOptimize() {
    	
    	return this.measureToOptimize;
    	
    }
    
    /**
	 * get the type of component (i.e. EditDistanceEDA)
	 * 
	 * @return the type of component
	 */
    protected String getType() {
    	
    	return this.getClass().getCanonicalName();
    	
    }
    
	/**
	 * get the language the EDA has to work with
	 * 
	 * @return the language
	 */
	public String getLanguage() {
		
		return this.language;
		
	}

	/**
	 * get the component used by the EDA
	 * 
	 * @return the component
	 */
	public FixedWeightTreeEditDistance getComponent() {
		
		return this.component;
		
	}

	/**
	 * set the language the EDA has to work with
	 * 
	 * @param language the language
	 * 
	 * @return
	 */
	public void setLanguage(String language) {
		
		this.language = language;
		
	}

	/**
	 * get the training data directory
	 * 
	 * @return the training directory
	 */
	public String getTrainDIR() {
		
		return this.trainDIR;
		
	}
	
	/**
	 * set the training data directory
	 * 
	 */
	public void setTrainDIR(String trainDIR) {
		
		this.trainDIR = trainDIR;
		
	}
	
	/**
	 * get the test data directory
	 * 
	 * @return the test directory
	 */
	public String getTestDIR() {
		
		return this.testDIR;
		
	}
	
	/**
	 * set the test data directory
	 * 
	 */
	public void setTestDIR(String testDIR) {
		
		this.testDIR = testDIR;
		
	}
	
	/**
	 * get the accuracy obtained on the training data set
	 * 
	 * @return the accuracy
	 */
	protected double getTrainingAccuracy() {
		
		return this.trainingAccuracy;
		
	}
	
	/**
	 * Construct an edit distance EDA.
	 */
	public TransformationDrivenEDA() {
    	
		logger.info("Creating an instance of Edit Distance EDA");
		
		this.component = null;
        this.writeModel = true;
        this.mMatchWeight = Double.NaN;
        this.mDeleteWeight = Double.NaN;
        this.mInsertWeight = Double.NaN;
        this.mSubstituteWeight = Double.NaN;
        this.trainDIR = null;
        this.trainDIR = null;
        this.language = null;
        this.measureToOptimize = null;
        
        logger.info("done.");
        
    }
	
	/**
	 * Construct an edit distance EDA with the weights of the edit distance operations set
	 * 
	 * @param mMatchWeight weight for match
	 * @param mDeleteWeight weight for delete
	 * @param mInsertWeight weight for insert
	 * @param mSubstituteWeight weight for substitute
	 * 
	 */
	public TransformationDrivenEDA(double mMatchWeight, double mDeleteWeight, double mInsertWeight, double mSubstituteWeight) {
		
		this();
		
		this.mMatchWeight = mMatchWeight;
	    this.mDeleteWeight = mDeleteWeight;
	    this.mInsertWeight = mInsertWeight;
	    this.mSubstituteWeight = mSubstituteWeight;
		
	}

	@Override
	public void initialize(CommonConfig config) throws ConfigurationException, EDAException, ComponentException {
		
		logger.info("Initialization ...");
		
		try {
        	
        	//checking the configuration file
			checkConfiguration(config);
			
			//getting the name value table of the EDA
			NameValueTable nameValueTable = config.getSection(this.getType());
			
			//setting the training directory
			if (this.trainDIR == null)
				this.trainDIR = nameValueTable.getString("trainDir");
			
			//setting the test directory
			if (this.testDIR == null)
				this.testDIR = nameValueTable.getString("testDir");
			
			//initializing the weight of the edit distant operations
			initializeWeights(config);
			
			//component initialization
			String componentName  = nameValueTable.getString("components");
			if (component == null) {
				
				try {
					
					Class<?> componentClass = Class.forName(componentName);
					logger.info("Using:" + componentClass.getCanonicalName());
					Constructor<?> componentClassConstructor = componentClass.getConstructor(CommonConfig.class);
					this.component = (FixedWeightTreeEditDistance) componentClassConstructor.newInstance(config);
					this.component.setmMatchWeight(mMatchWeight);
					this.component.setmDeleteWeight(mDeleteWeight);
					this.component.setmInsertWeight(mInsertWeight);
					this.component.setmSubstituteWeight(mSubstituteWeight);
					
					/*
					 * Initializing FixedWeightEditDistance without a configuration file
					FixedWeightEditDistance fwed = new FixedWeightTokenEditDistance(mMatchWeight, mDeleteWeight, 
							mInsertWeight, mSubstituteWeight, true, "IT", null);
					
					this.component = fwed;
					*/
					
					/*
					 * Initializing FixedWeightEditDistance without a configuration file and using
					 * wikipedia as an external resource.
					Map<String,String> resources = new HashMap<String,String>();
					resources.put("wikipedia", "jdbc:mysql://nathrezim:3306/wikilexresita#root#nat_2k12");
					FixedWeightEditDistance fwed = new FixedWeightTokenEditDistance(mMatchWeight, mDeleteWeight, 
							mInsertWeight, mSubstituteWeight, true, "IT", resources);
					
					this.component = fwed;
					*/
					
				} catch (Exception e) {
					throw new ComponentException(e.getMessage());
				}
				
			}
			
			//setting the measure to be optimized
			if (this.measureToOptimize == null)
				this.measureToOptimize = nameValueTable.getString("measure");
			
		} catch (ConfigurationException e) {
			throw e;
		} catch (Exception e) {
			throw new EDAException(e.getMessage());
		}
		
		logger.info("done.");
	
	}
	
	@Override
	public EditDistanceTEDecision process(JCas jcas) throws EDAException, ComponentException {
		
		String pairId = getPairId(jcas);
		
		//the distance between the T-H pair
		DistanceValue distanceValue = component.calculation(jcas);
		double distance = distanceValue.getDistance();
		
		// During the test phase the method applies the threshold, so that
		// pairs resulting in a distance below the threshold are classiﬁed as ENTAILMENT, while pairs 
		// above the threshold are classiﬁed as NONENTAILMENT.
		if (distance <= this.threshold)
			return new EditDistanceTEDecision(DecisionLabel.Entailment, pairId, threshold - distance);
		
		return new EditDistanceTEDecision(DecisionLabel.NonEntailment, pairId, distance - threshold);
		
	}
	
	@Override
	public void shutdown() {
		
		logger.info("Shutting down ...");
	        	
		if (component != null)
			((FixedWeightTreeEditDistance)component).shutdown();
		
		this.component = null;
        this.writeModel = true;
        this.mMatchWeight = Double.NaN;
        this.mDeleteWeight = Double.NaN;
        this.mInsertWeight = Double.NaN;
        this.mSubstituteWeight = Double.NaN;
        this.trainDIR = null;
        this.trainDIR = null;
        this.language = null;
        this.measureToOptimize = null;
        
        logger.info("done.");
		
	}
	
	@Override
	public void startTraining(CommonConfig config) throws ConfigurationException, EDAException, ComponentException {
		
		logger.info("Training ...");
		
		try {
			
			initialize(config);
			
			//contains the distance between each pair of T-H
			List<List<String>> editDistanceOperationsList = new ArrayList<List<String>>();
			//contains the entailment annotation between each pair of T-H
			List<String> entailmentValueList = new ArrayList<String>();
			//contains the entailment annotation and the distance between each pair of T-H
			
			File f = new File(trainDIR);
			if (f.exists() == false) {
				throw new ConfigurationException("trainDIR:" + f.getAbsolutePath() + " not found!");
			}
			
			int filesCounter = 0;
			for (File xmi : f.listFiles()) {
				if (!xmi.getName().endsWith(".xmi")) {
					continue;
				}
				
				JCas cas = PlatformCASProber.probeXmi(xmi, null);
					
				List<String> operations = getEditDistanceOperations(cas);
				String entailmentAnnotation = getEntailmentAnnotation(cas);
				

				 // Declare a nominal attribute along with its values
				 FastVector fvNominalVal = new FastVector(operations.size());
				 for (int i = 0; i < operations.size(); i++) {
					 fvNominalVal.addElement(operations.get(i));
				 }
				 Attribute Attribute = new Attribute("operations", fvNominalVal);
				 
				 // Declare the class attribute along with its values
				 FastVector fvClassVal = new FastVector(1);
				 fvClassVal.addElement(entailmentAnnotation);
				 //fvClassVal.addElement(“negative”);
				 Attribute ClassAttribute = new Attribute("theClass", fvClassVal);
				 
				 // Declare the feature vector
				 FastVector fvWekaAttributes = new FastVector(2);
				 fvWekaAttributes.addElement(Attribute);  
				 fvWekaAttributes.addElement(ClassAttribute);
				
				 filesCounter++;
			}
			
			if (filesCounter == 0)
				throw new ConfigurationException("trainDIR:" + f.getAbsolutePath() + " empty!");
			
			
			
			this.threshold = thresholdAndAccuracy[0];
			this.trainingAccuracy = thresholdAndAccuracy[1];
			
			//it saves the calculated model into the configuration file itself
			if (this.writeModel == true)
				saveModel(config);
			
		} catch (ConfigurationException e) {
			throw e;
		} catch (EDAException e) {
			throw e;
		} catch (ComponentException e) {
			throw e;
		} catch (Exception e) {
			throw new EDAException(e.getMessage());
		}
		
		logger.info("done.");
		
	}
	
	/**
     * Checks the configuration and raise exceptions if the provided
     * configuration is not compatible with this class
     * 
     * param config the configuration
     *
     * @throws ConfigurationException
     */
	private void checkConfiguration(CommonConfig config) 
			throws ConfigurationException {
		
		if (config == null)
			throw new ConfigurationException("Configuration file not found.");
		
	}
	
	
	/**
     * Returns the pair identifier of the pair contained in the specified CAS
     *
     * @param jcas the CAS
     * 
     * @return the pair identifier
     */
	private String getPairId(JCas jcas) {
		
		FSIterator<TOP> pairIter = jcas.getJFSIndexRepository().getAllIndexedFS(Pair.type);
		
		Pair p = null;
		if (pairIter.hasNext())
			p = (Pair)pairIter.next();
		
		if (p != null)
			return p.getPairID();
	
		return null;
		
	}
	
	/**
     * Puts distance values calculating for each of the pair T and H
     * of the specified list of Cas into the distanceValues list. 
     * Each of the Cas of the list contains a single pair T-H
     *
     * @param jcas the list of CAS
     * @param distanceValues the list of the distance values
     * 
     * @throws DistanceComponentException
     */
	private List<String> getEditDistanceOperations(JCas jcas)
			throws DistanceComponentException {
		
		List<String> result = null;
	
		try {

				DistanceValue distanceValue = component.calculation(jcas);
				result = component.getEditDistanceOperations();

		} catch(DistanceComponentException e) {
			throw e;
		}
			
		return result;
		
	}
		
	/**
     * Puts the entailment annotations calculating of each of the pair T and H
     * of the specified list of Cas into the entailmentValueList list. 
     * Each of the Cas of the list contains a single pair T-H.
     *
     * @param jcas the list of CAS
     * @aram entailmentValueList the list of the entailment annotations
     * 
     * @throws Exception
     */
	private String getEntailmentAnnotation(JCas jcas) 
			throws Exception {
		
		String result = null;
			
		try {
			
				Pair p = null;
				FSIterator<TOP> pairIter = jcas.getJFSIndexRepository().getAllIndexedFS(Pair.type);
				p = (Pair) pairIter.next();
				String goldAnswer = p.getGoldAnswer();
				result = goldAnswer;
			
		} catch(Exception e) {
			throw e;
		}
			
		return result;		
		
	}	
	
	/**
     * Save the optimized parameters (e.g. threshold) into the configuration file itself
     */
	private void saveModel(CommonConfig config) throws IOException {
    	
		logger.info("Writing model ...");
		
    	BufferedWriter writer = null;
    	
    	try {
    		
    		String newModel = updateConfigurationFile(config);
    		
	    	writer = new BufferedWriter(new OutputStreamWriter(
	                  new FileOutputStream(config.getConfigurationFileName()), "UTF-8"));

	    	PrintWriter printout = new PrintWriter(writer);
	    	printout.println(newModel);
	    	printout.close();
	    	
    	} catch (Exception e) {
    		throw new IOException(e.getMessage());
    	} finally {
    		if (writer != null)
    			writer.close();
    	}
    	
    	logger.info("done.");

    }
	
	/**
     * Initialize the weights of the edit distance operations getting them form the configuration file
     * 
     * If all the values of the 4 edit distance operation are defined they are used
     * to calculate edit distance. Otherwise the weights are read from the configuration file.  
     * 
     * @param config the configuration
     * 
     */
    protected void initializeWeights(CommonConfig config) {

    	try{ 
    		
    		NameValueTable weightsTable = config.getSection(this.getType());
    		
    		//if they weights have already been set use those weights 
        	if (Double.isNaN(this.mMatchWeight) == false && Double.isNaN(this.mDeleteWeight) == false &&
        			Double.isNaN(this.mInsertWeight) == false && Double.isNaN(this.mSubstituteWeight) == false)
        		return;
    		
    		this.mMatchWeight = weightsTable.getDouble("match");
    		this.mDeleteWeight = weightsTable.getDouble("delete");
    		this.mInsertWeight = weightsTable.getDouble("insert");
    		this.mSubstituteWeight = weightsTable.getDouble("substitute");
    		
    	} catch (Exception e) {
    		
    		logger.info("Could not find weights section in configuration file, using defaults");
    		this.mMatchWeight = 0.0;
    		this.mDeleteWeight = 0.0;
    		this.mInsertWeight = 1.0;
    		this.mSubstituteWeight = 1.0;
    		
    	}
    	
    }
    
    /**
     * Reads the configuration file and adds the calculated optimized parameters (i.e. the threshold
     * calculated on the training data set.)
     * 
     * @return the configuration file with the optimized parameters added
     * 
     */
    protected String updateConfigurationFile(CommonConfig config) throws IOException {
    
    	StreamResult result = new StreamResult(new StringWriter());
    	
    	try {
    			
    		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
    		Document document = docBuilder.parse(new File(config.getConfigurationFileName()));

    		XPathFactory xpathFactory = XPathFactory.newInstance();
    		XPath xpath = xpathFactory.newXPath();
    		
    		Node trainingAccuracyNode = (Node) xpath.evaluate("//*[@name='model']/*[@name='trainingAccuracy']", document, XPathConstants.NODE);
    		trainingAccuracyNode.setTextContent(String.valueOf(this.getTrainingAccuracy()));
    		
    		TransformerFactory tf = TransformerFactory.newInstance();
    		Transformer t = tf.newTransformer();
    		t.transform(new DOMSource(document), result);
    			       			
    	}catch (Exception e) {
    		throw new IOException(e.getMessage());
    	}
    	
    	return result.getWriter().toString();
	    
    }
    
	
}
