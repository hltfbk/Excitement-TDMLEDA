package eu.excitementproject.eop.core;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.io.*;
import java.lang.reflect.Constructor;

import org.apache.uima.jcas.JCas;
import org.uimafit.util.JCasUtil;

import weka.attributeSelection.AttributeEvaluator;
import weka.attributeSelection.InfoGainAttributeEval;
import weka.attributeSelection.Ranker;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.core.SerializationHelper;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;

import eu.excitementproject.eop.common.DecisionLabel;
import eu.excitementproject.eop.common.EDABasic;
import eu.excitementproject.eop.common.EDAException;
import eu.excitementproject.eop.common.TEDecision;
import eu.excitementproject.eop.common.configuration.CommonConfig;
import eu.excitementproject.eop.common.configuration.NameValueTable;
import eu.excitementproject.eop.common.exception.ComponentException;
import eu.excitementproject.eop.common.exception.ConfigurationException;
import eu.excitementproject.eop.common.utilities.configuration.ImplCommonConfig;
import eu.excitementproject.eop.core.component.distance.*;
import eu.excitement.type.entailment.Pair;
import eu.excitementproject.eop.lap.LAPAccess;
import eu.excitementproject.eop.lap.PlatformCASProber;
import eu.excitementproject.eop.lap.dkpro.MaltParserEN;


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
	 * 
	 * EDA's variables section
	 * 
	 */
	
	/**
	 * the edit distance component to be used
	 */
	private FixedWeightTreeEditDistance component;
	
	/**
	 * the logger
	 */
	static Logger logger = 
			Logger.getLogger(TransformationDrivenEDA.class.getName());

	/**
	 * the training data directory
	 */
	private String trainDIR;
	
	/**
	 * the tmp directory where to put temporary data
	 */
	private String tmpDIR;

	/**
	 * the test data directory
	 */
	private String testDIR;
    
	/**
	 * get the component used by the EDA
	 * 
	 * @return the component
	 */
	public FixedWeightTreeEditDistance getComponent() {
		
		return this.component;
		
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
	 * get the training data directory
	 * 
	 * @return the training directory
	 */
	public String getTrainDIR() {
		
		return this.trainDIR;
		
	}
	
	/**
	 * get the tmp directory
	 * 
	 * @return the tmp directory
	 */
	public String getTmpDIR() {
		
		return this.tmpDIR;
		
	}
	
	/**
	 * set the training data directory
	 * 
	 */
	public void setTrainDIR(String trainDIR) {
		
		this.trainDIR = trainDIR;
		
	}
	
	/**
	 * set the tmp directory
	 * 
	 */
	public void setTmpDIR(String tmpDIR) {
		
		this.tmpDIR = tmpDIR;
		
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
	 * 
	 * Classifier's variables section; classifier is the
	 * classifier used by the EDA to classifier T/H pairs
	 * 
	 */

	/** 
	 * The actual classifier
	 */
	private Classifier classifier;
	
	/** 
	 * The max number of features to be used by the classifier
	 * for training and test. It is used for feature selection.
	 */
	private int maxNumberOfFeatures;
	
	/** 
	 * The feature set used for training and test
	 */
	private Map<String,Integer> featuresList;
	
	/** 
	 * The data set (it is build by using the weka data structures), 
	 * for training and test.
	 */
    private Instances inputDataset;
    
    /** 
     * The annotation classes, e.g. ENTAILMENT, NONENTAILMENT 
     */
    private FastVector classesList;
    
    /** 
     * The classifier evaluation; it contains methods for getting
     * a number of measures like precision, recall and F1measure.
     */
	private Evaluation evaluation;
	
	/** 
     * number of folds for cross-validation
     */
	private final int numFolds = 10;
	
	/** 
	 * If an evaluation has to be done on the 
	 * training data set, e.g. cross validation 
	 * */
	private boolean crossValidation;

	/** 
	 * Enable feature selection
	 * */
	private boolean featureSelection;
	
	
	/**
	 * Construct an TransformationDriven EDA
	 */
	public TransformationDrivenEDA() {
    	
		logger.info("creating an instance of TransformationDrivenEDA ...");
		
		this.component = null;
		this.classifier = null;
		this.crossValidation = false;
		this.featureSelection = false;
		this.evaluation = null;
        this.trainDIR = null; //"/tmp/training/";
        this.testDIR = null; //"/tmp/test/";
        this.tmpDIR = null; //"/tmp/temporaryfiles/";
        
        logger.info("done.");
        
    }

	
	@Override
	public void initialize(CommonConfig config) 
			throws ConfigurationException, EDAException, ComponentException {
		
		logger.info("initialize ...");
		
		try {
        	
        	//checking the configuration file
			checkConfiguration(config);
			
			//getting the name value table of the EDA; it contains the methods
			//for getting the EDA configuration form the configuration file.
			NameValueTable nameValueTable = config.getSection(this.getType());
			
			//setting the training directory
			if (this.trainDIR == null)
				this.trainDIR = nameValueTable.getString("trainDir");
			logger.info("training directory:" + this.trainDIR);
			
			//setting the test directory
			if (this.testDIR == null)
				this.testDIR = nameValueTable.getString("testDir");
			logger.info("testing directory:" + this.testDIR);
			
			//setting the test directory
			if (this.tmpDIR == null)
				this.tmpDIR = nameValueTable.getString("tmpDir");
			logger.info("temporary directory:" + this.tmpDIR);
			
			//evaluation on the training data set
			if (this.crossValidation == false)
				this.crossValidation = Boolean.parseBoolean(nameValueTable.getString("cross-validation"));
			logger.info("cross-validation:" + this.crossValidation);
			
			//enable feature selection
			if (this.featureSelection == false)
				this.featureSelection = Boolean.parseBoolean(nameValueTable.getString("feature-selection"));
			logger.info("feature-selection:" + this.featureSelection);
			if (this.featureSelection == true) {
				this.maxNumberOfFeatures = Integer.parseInt(nameValueTable.getString("max-number-of-features"));
				logger.info("max number of features to be selected:" + this.maxNumberOfFeatures);
			}
			
			//classifier initialization
			String classifierName = nameValueTable.getString("classifier");
			//classifierName = "weka.classifiers.bayes.NaiveBayes";
			if (this.classifier == null) {
				
				try {
					Class<?> classifierClass = Class.forName(classifierName);
					logger.info("classifier:" + classifierClass.getCanonicalName());
					Constructor<?> classifierClassConstructor = classifierClass.getConstructor();
					this.classifier = (Classifier) classifierClassConstructor.newInstance();
				} catch (Exception e) {
					e.printStackTrace();
					throw new EDAException(e.getMessage());
				}
			
			}
			
			//component initialization
			String componentName = nameValueTable.getString("components");
			//componentName  = "eu.excitementproject.eop.core.component.distance.FixedWeightTreeEditDistance";
			if (this.component == null) {
				
				try {
					
					Class<?> componentClass = Class.forName(componentName);
					logger.info("Using:" + componentClass.getCanonicalName());
					Constructor<?> componentClassConstructor = componentClass.getConstructor(CommonConfig.class);
					this.component = (FixedWeightTreeEditDistance) componentClassConstructor.newInstance(config);
					
				} catch (Exception e) {
					e.printStackTrace();
					throw new ComponentException(e.getMessage());
				}
				
			}

		} catch (ConfigurationException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new EDAException(e.getMessage());
		}
		
		logger.info("done.");
	
	}
	
	
	@Override
	public EditDistanceTEDecision process(JCas jcas) 
			throws EDAException, ComponentException {
		
		logger.info("process ...");
		
		//the predicted class
		String annotationClass = null;
		//the confidence assigned by the classifier to the class
		double confidence = 0.0;
		//the classified T/H pair
		Pair pair = null;
		
		try {
			
			//initialize the feature index (i.e. featureList)
		    getFeaturesList();
		    //initialize the classes index (i.e classesLits)
		    getClasses();
		    
			//get the T/H pair
			pair = JCasUtil.selectSingle(jcas, Pair.class);
			logger.info("processing pair: " + pair.getPairID());
			
			/**
			 * this records the gold standard answer for this pair. If the pair 
			 * represents a training data, this value is the gold standard answer. If 
			 * it is a null value, the pair represents a problem that is yet to be answered.
			*/
			String goldAnswer = pair.getGoldAnswer(); //get gold annotation
			logger.info("annotation class label: " + goldAnswer);
			
			//get the distance between T and H
			double distance = component.calculation(jcas).getDistance();
			
			System.err.println("===================== distance:" + distance);
			
			//get the transformations needed to transform T into H
			List<Transformation> transformations = component.getTransformations();
			
			/*
			String tr = null;
			double random = Math.random();
			if (random <= 0.5)
				tr = "tr5 tr6 tr7 tr8";
			else 
				tr = "tr1 tr2 tr3 tr4";
			
			//creating example_i
			HashSet<String> example_i = new HashSet<String>();
			String[] features = tr.split("\\s");
			//only consider the features that have been seen in the training set 
			for (int i = 0; i < features.length; i++) {
				if (featuresList.containsKey(features[i]))
					example_i.add(features[i]);
			}
			*/
			
			HashSet<String> example_i = new HashSet<String>();
			Iterator<Transformation> iteratorTransformation = transformations.iterator();
			while(iteratorTransformation.hasNext()) {
				Transformation transformation_i = iteratorTransformation.next();
				System.out.println(transformation_i);
				String transformation_i_name = transformation_i.print(true, false, true,true);
				if (transformation_i_name == null)
					continue;
				if (featuresList.containsKey(transformation_i_name))
					example_i.add(transformation_i_name);
			}
			
			//data structure for storing gold annotations (e.g. ENTAILMENT)
			ArrayList<String> annotation = new ArrayList<String>();
			
			//data structure for storing the examples to be used for training
            List<HashSet<String>> examples = new ArrayList<HashSet<String>>();
			
			//adding example_i into the list of the examples
			examples.add(example_i);
			//adding the annotation of the example_i
			if (goldAnswer != null)
				annotation.add(goldAnswer); //the annotation is in the test set
			else
				annotation.add("?"); //the annotation is not in the test set
			
			//initialize the data set (i.e. declare attributes and classes)
			initDataSet();
			//fill the data set
			fillDataSet(examples, annotation);
					
			//logger.info("number of features:" + featureList.size());
			//logger.info("number of examples:" + examples.size() );
			logger.info("input data set:\n" + inputDataset);
			
			//the classifier returns with a confidence level for each of the possible
			//classes; following we look for the most probable classes and report
			//the confidence assigned to this class by the classifier
			double[] score = testClassifier();
			int index = 0;
			for (int i = 0; i < score.length; i++) {
				if (score[i] >= confidence) {
					confidence = score[i];
					index = i;
				}
			}
			//get the class label (e.g. Entailment)
			annotationClass = inputDataset.attribute("class").value(index);
			
			logger.info("annotation class:" + annotationClass);
		
		} catch (Exception e) {
			
			throw new EDAException(e.getMessage());
			
		} 
		
		DecisionLabel decisionLabel = DecisionLabel.getLabelFor(annotationClass);
		
		return new EditDistanceTEDecision(decisionLabel, pair.getPairID(), confidence);
		
	}
	
	
	@Override
	public void shutdown() {
		
		logger.info("shutdown ...");
	        	
		if (component != null)
			((FixedWeightTreeEditDistance)component).shutdown();
		
		this.component = null;
		this.classifier = null;
		this.crossValidation = false;
		this.evaluation = null;
        this.trainDIR = null;
        this.testDIR = null;
        this.tmpDIR = null;
        
        logger.info("done.");
		
	}
	
	
	@Override
	public void startTraining(CommonConfig config) throws ConfigurationException, EDAException, ComponentException {
		
		logger.info("training ...");
		
		try {

			//initialize the EDA
			initialize(config);

			//if there are no files for training
			File f = new File(trainDIR);
			if (f.exists() == false) {
				throw new ConfigurationException(trainDIR + ":" + f.getAbsolutePath() + " not found!");
			}
				
			//initialize the feature index
		    initFeaturesList();
		    
		    //initialize the class index
		    initClassesList();
		    
			//data structure for storing gold annotations (e.g. ENTAILMENT)
		    //for each of the example in the data set
			ArrayList<String> annotation = new ArrayList<String>();
			
			//data structure for storing the examples to be used for training
            List<HashSet<String>> examples = new ArrayList<HashSet<String>>();
            		
			//reading the training data set
			for (File xmi : f.listFiles()) {
					
				if (!xmi.getName().endsWith(".xmi")) {
					continue;
				}
				
				// The annotated pair is added into the CAS.
				JCas jcas = PlatformCASProber.probeXmi(xmi, null);
				
				//the T/H pair
				Pair pair = JCasUtil.selectSingle(jcas, Pair.class);
				//logger.fine("processing pair: " + pair.getPairID());
				
				//the pair annotation
				String goldAnswer = pair.getGoldAnswer(); //get gold annotation
				//logger.fine("gold annotation: " + goldAnswer);
				
				//get the distance between T and H
				double distance = component.calculation(jcas).getDistance();
				
				//get the transformations to transform T into H
				List<Transformation> transformations = component.getTransformations();
				
				/*
				String tr = null;
					
				if (goldAnswer.equals("ENTAILMENT")) {
					tr = "tr5 tr6 tr7 tr8";
				}
				else {
					tr = "tr1 tr2 tr3 tr4";
				}
					
				//creating example_i
				HashSet<String> example_i = new HashSet<String>();
				String[] features = tr.split("\\s");
				//features indexing
				for (int i = 0; i < features.length; i++) {
					//creating the feature index starting from 0
					if (!featuresList.containsKey(features[i])) {
						featuresList.put(features[i], featuresList.size());
					}
					example_i.add(features[i]);
				}
				//creating the classes index starting from 0
				if (!this.classesList.contains(goldAnswer))
					classesList.addElement(goldAnswer);
				*/
				
				HashSet<String> example_i = new HashSet<String>();
				Iterator<Transformation> iteratorTransformation = transformations.iterator();
				while(iteratorTransformation.hasNext()) {
					Transformation transformation_i = iteratorTransformation.next();
					String transformation_i_name = transformation_i.print(true, false, true,true);
					if (transformation_i_name == null)
						continue;
					if (!featuresList.containsKey(transformation_i_name))
						featuresList.put(transformation_i_name, featuresList.size());
					example_i.add(transformation_i_name);
				}
				//creating the classes index starting from 0
				if (!this.classesList.contains(goldAnswer))
					classesList.addElement(goldAnswer);
				
				//add the example_i into the list of the examples
				examples.add(example_i);
				//add the annotation of the example_i
				annotation.add(goldAnswer);
				
			}
			
			//init the data set (i.e. attribute and classes declaration) for
			//training the classifier
			initDataSet();
			
			//fill the data set for training the classifier
			fillDataSet(examples, annotation);
			
			logger.info("number of examples:" + examples.size() );
			logger.info("number of features:" + featuresList.size());
			logger.info("input data set:\n" + inputDataset);
			
			//enable feature selection
			//if (this.featureSelection == true) {
			//	selectFeatures();
			//}
			
			//save the list of the features with their index into a file to be used
			//during the test phase (see the process method)
			saveFeaturesList();
			
			//save the list of the classes and their index into a file to be used
			//during the test phase (see the process method)
			saveClasses();
				
			//train the classifier
			trainClassifier();
			
            //cross-validation
            if (this.crossValidation == true) {
            	evaluation = new Evaluation(inputDataset);
            	evaluation.crossValidateModel(classifier, inputDataset, numFolds, new Random(1));
            	logger.info("evaluation summary:\n" + evaluation.toSummaryString());
            }
            
		} catch (Exception e) {
			
			e.printStackTrace();
			throw new EDAException(e.getMessage());
			
		} 
		
		logger.info("done.");
		
	}
	

	/*
	 * Define the data set format
	 */
	private void initDataSet() throws Exception {
	
		logger.info("data set initialization ...");
		
		try {
		
			//1) defining the attributes; basically each of the 
			//extracted features is a new attribute, e.g.
			//@attribute attr6 numeric
			//@attribute attr7 numeric
			//@attribute attr8 numeric
			FastVector attributes = new FastVector();
			
			for (Entry<String, Integer> entry  : entriesSortedByValues(featuresList)) {
		        String featureName = entry.getKey();
				//each of the extracted features is a new attribute
				Attribute attribute_i = new Attribute(featureName);
				//adding the attribute_i into the list of the attributes
				System.err.println(attribute_i +  "\t" + featuresList.get(featureName));
				attributes.addElement(attribute_i);
				logger.info("adding attribute_i:" + attributes.size());
			}
			
			// 2) defining the class attribute, e.g.
			//@attribute class {null,ENTAILMENT,NONENTAILMENT}
			
			Attribute attribute_class = new Attribute("class", classesList);
			//logger.info("adding class attribute:" + attribute_class);
			attributes.addElement(attribute_class);
			
			//create the data set named 'dataset', e.g.
			//@relation dataset
			inputDataset = new Instances("dataset", attributes, 0);
			
			//the last attribute is the class
			inputDataset.setClassIndex(featuresList.size());
			
			//logger.info("data set:\n" + inputDataset);
		
		} catch (Exception e) {
			
			throw new Exception("Data Set initialization error:" + e.getMessage());
			
		} 
		
		logger.info("done.");
		
	}
	
	/*
	 * Adding data into the data set
	 */
	private void fillDataSet(List<HashSet<String>> examples, List<String> annotation) 
			throws Exception {
		
		logger.info("creating data set ...");
		
		try {
		
			//creating an instance for each of the examples 
			for (int i = 0; i < examples.size(); i++) {
				//getting the example_i
				HashSet<String> example_i = examples.get(i);
				//logger.info("example_i:" + example_i);
				//an array of size(featuresList)+1 values 
				double[] initValues = new double[featuresList.size() + 1];
				//creating a SPARSE instance i and initialize it so that 
				//its values are set to 0 
				Instance instance_i = new SparseInstance(1.0, initValues);//1.0 is the instance weight
				Iterator<String> iterator_j = example_i.iterator();
				while(iterator_j.hasNext()) {
					String feature_j = iterator_j.next();
					int featureIndex;
					if (featuresList.containsKey(feature_j)) {
						//System.err.println(feature_j + "\t" +  featuresList.get(feature_j));
						featureIndex = featuresList.get(feature_j);
						//only the features with weight different from 0 are set
						instance_i.setValue(featureIndex, 1.0);//1.0 is the feature weight
					}
				}
				//the last value is that of the annotation class
				instance_i.setValue(featuresList.size(), classesList.indexOf(annotation.get(i)));
				//adding the instance into the data set
				inputDataset.add(instance_i);
				
			}
		
		} catch (Exception e) {
			
			throw new Exception("Creating data set error:" + e.getMessage());
			
		} 
		
		logger.info("done.");
		
	}
	
	
	private void initClassesList() {
		
		logger.info("Initialize classes list ...");
		
		classesList = new FastVector();
		//the 'null' value has been added to solve the issue saving SparseInstance objects from 
		//datasets that have string attribute; see WekaManual-3-6-10-1.pdf
		classesList.addElement("null");
		
		logger.info("done.");
		
	}
	
	private void initFeaturesList() {
		
		logger.info("Initialize features list ...");
		
		this.featuresList = new HashMap<String,Integer>();
		
		logger.info("done.");
		
	}
	
	
	/*
	 * Save the feature index to be used for processing
	 */
	private void saveFeaturesList() throws Exception {
		
		logger.info("saving features list ...");
		
	    int attributeNumber = inputDataset.numAttributes();
	    initFeaturesList();
	    
		BufferedWriter writer = null;
	    StringBuffer stringBuffer = new StringBuffer();
		
	    String attributeClassName = inputDataset.classAttribute().name();
	    
	    try {
	    		
	    	for (int i = 0; i < attributeNumber; i++) {
		    	String feature_i = inputDataset.attribute(i).name();
		    	if (feature_i.equals(attributeClassName))
		    			continue;
		    	//featuresList.put(feature_i, i);
	    		stringBuffer.append(feature_i);
	    		stringBuffer.append("\t");
	    		stringBuffer.append(i);
	    		stringBuffer.append("\n");
	    	}
	    	
	    	writer = new BufferedWriter(new OutputStreamWriter(
	                  new FileOutputStream(tmpDIR + "/" + "feature_list.txt", false), "UTF-8"));

	    	PrintWriter printout = new PrintWriter(writer);
	    	printout.print(stringBuffer);
	    	printout.close();
	    	
	    	writer.close();
		    	
	    } catch (Exception e) {
	    	
	    	throw new Exception("Saving features list error:" + e.getMessage());
	    	
	    }
		
	    logger.info("done.");
	    
	}
	
	/*
	 * Get the feature index to be used for processing
	 */
	private void getFeaturesList() throws Exception {
		
		logger.info("getting features list ...");
		
		try {
			
			File fileDir = new File(tmpDIR + "/" + "feature_list.txt");
	 
			BufferedReader in = new BufferedReader(
			   new InputStreamReader(
	                      new FileInputStream(fileDir), "UTF8"));
	 
			String str;
	 
			while ((str = in.readLine()) != null) {
			    String[] splitLine = str.split("\\s");
			    String feature_i = splitLine[0];
			    int index = Integer.parseInt(splitLine[1]);
			    featuresList.put(feature_i, index);
			}
	                in.close();
		} catch (UnsupportedEncodingException e) {
				throw new Exception("Getting features list Unsupported Encoding Exception:" + e.getMessage());
		} catch (IOException e) {
		    	throw new Exception("Getting features list IOError:" + e.getMessage());
		} catch (Exception e) {
				throw new Exception("Getting features list error:" + e.getMessage());
		}
		
		logger.info("done.");
		
	}
	
	
	/**
	 * Save the feature index to be used for processing
	 */
	private void saveClasses() throws Exception {
		
		logger.info("saving the classes list ...");
		
		BufferedWriter writer = null;
	    StringBuffer stringBuffer = new StringBuffer();
		
	    try {
	    		
	    	for (int i = 0; i < classesList.size(); i++) {
	    		
	    		String classLabel_i = (String)classesList.elementAt(i);
	    		stringBuffer.append(classLabel_i);
	    		stringBuffer.append("\t");
	    		stringBuffer.append(i);
	    		stringBuffer.append("\n");
	    		
	    	}
	    	
	    	writer = new BufferedWriter(new OutputStreamWriter(
	                  new FileOutputStream(tmpDIR + "/" + "classes.txt", false), "UTF-8"));

	    	PrintWriter printout = new PrintWriter(writer);
	    	printout.print(stringBuffer);
	    	printout.close();
	    	
	    	writer.close();
		    	
	    } catch (Exception e) {
	    	
	    	throw new Exception("Saving the classes list error:" + e.getMessage());
	    	
	    }
	    
	    logger.info("done.");
		
	}
	
	
	/**
	 * Get the feature index to be used for processing
	 */
	private void getClasses() throws Exception {
		
		logger.info("getting the classes list ...");
		
		try {
			
			File fileDir = new File(tmpDIR + "/" + "classes.txt");
	 
			BufferedReader in = new BufferedReader(
			   new InputStreamReader(
	                      new FileInputStream(fileDir), "UTF8"));
	 
			String str;
	 
			while ((str = in.readLine()) != null) {
			    String[] splitLine = str.split("\\s");
			    String classLabel_i = splitLine[0];
			    int index = Integer.parseInt(splitLine[1]);
			    classesList.setElementAt(classLabel_i, index);
			}
	                in.close();
		    
		} catch (UnsupportedEncodingException e) {
			throw new Exception("Unsupported Encoding Exception:" + e.getMessage());
		} catch (IOException e) {
			throw new Exception("IOError:" + e.getMessage());
		} catch (Exception e) {
			throw new Exception("Error:" + e.getMessage());
		}
		 
		logger.info("done.");
		
	}
	
	
	/**
	 * Train the classifier
	 */
	private void trainClassifier() throws Exception {
		
		logger.info("training the classifier ...");
		
        try {
            
        	//building the classifier
            classifier.buildClassifier(inputDataset);
            //storing the trained classifier to a file for future use
            weka.core.SerializationHelper.write(tmpDIR + "/" + "NaiveBayes.model", classifier);
            
        } catch (Exception ex) {
        	
        	throw new Exception(ex.getMessage());
        	
        }
        
        logger.info("done.");
        
    }
	
    
	/**
	 * Test the classifier
	 */
    private double[] testClassifier() throws Exception {
    	
    	//it contains a confidence value of each of the predicted classes
    	double[] score = null;
    	
        try {
        	
            //Classifier deserialization
            classifier = (Classifier) weka.core.SerializationHelper.read(tmpDIR + "/" + "NaiveBayes.model");
            
            for (int i = 0; i < inputDataset.numInstances(); i++) {
                
            	Instance instance_i = inputDataset.instance(i);
            	
            	//the predicted class, e.g. 1
                //double entailment_class = classifier.classifyInstance(instance_i);
            	//the class label, e.g. ENTAILMENT
            	//logger.info("predicted class:" + inputDataset.attribute("class").value((int)entailment_class));
            	
            	//the confidence values
                score = classifier.distributionForInstance(instance_i); 
       
            }
            
        } catch (Exception ex) {
        	
        	throw new Exception(ex.getMessage());
            
        }
        
        return score;
        
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
		
		//if (config == null)
			//throw new ConfigurationException("Configuration file not found.");
		
	}
	
	
	/**
	 * Get a summary description of the classifier evaluation
	 * 
	 * @return the summary
	 */
	public String toSummaryString() {
		
		return evaluation.toSummaryString();
		
	}
	
	
	/**
	 * calculate the estimated error rate
	 * 
	 * @return the estimated error rate
	 */
	public double errorRate() {
		
		return evaluation.errorRate();
		
	}
	
	
	/**
	 * 
	 * selects a given number of features by info gain eval
	 * @param number the number of features to select
	 * @throws Exception
	 */
	public void selectFeatures() throws Exception {
		
		logger.info("feature selection ...");
		
		try {
		
		    AttributeSelection filter = new AttributeSelection();
		    InfoGainAttributeEval eval = new InfoGainAttributeEval();
		    Ranker search = new Ranker();
		    search.setNumToSelect(this.maxNumberOfFeatures);
		    filter.setEvaluator(eval);
		    filter.setSearch(search);
		    filter.setInputFormat(inputDataset);
		    Instances newData = Filter.useFilter(inputDataset, filter);
		    
		    int numberOfremovedFeatures = inputDataset.numAttributes() - newData.numAttributes();
		    
		    logger.info("removed:" + numberOfremovedFeatures);
		    
		    inputDataset= newData;
	    
		} catch (Exception ex) {
    	
			throw new Exception(ex.getMessage());
        
		}
	    
		logger.info("done.");
		
	}
	
	
	static <K,V extends Comparable<? super V>> SortedSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map) {
        SortedSet<Map.Entry<K,V>> sortedEntries = new TreeSet<Map.Entry<K,V>>(
            new Comparator<Map.Entry<K,V>>() {
                @Override public int compare(Map.Entry<K,V> e1, Map.Entry<K,V> e2) {
                    int res = e1.getValue().compareTo(e2.getValue());
                    return res != 0 ? res : 1; // Special fix to preserve items with equal values
                }
            }
        );
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }
	
	
	
	public static void main(String args[]) {
		
		TransformationDrivenEDA tdEDA = new TransformationDrivenEDA();
		
		try {
		
			File configFile = new File("./src/main/resources/configuration-file/TransformationDrivenEDA_EN.xml");
			
			CommonConfig config = new ImplCommonConfig(configFile);
			
			//tdEDA.test();
			LAPAccess lap = new MaltParserEN();
			// process TE data format, and produce XMI files.
			// Let's process English RTE3 data (formatted as RTE5+) as an example. 
			File input = new File("/tmp/examples.xml"); // this only holds the first 3 of them.. generate 3 XMIs (first 3 of t.xml) 
			//File input = new File("./src/test/resources/t.xml");  // this is full, and will generate 800 XMIs (serialized CASes)
			File outputDir = new File("/tmp/training/"); 
			try {
				lap.processRawInputFormat(input, outputDir); // outputDir will have those XMIs
			} catch (Exception e)
			{
				System.err.println(e.getMessage()); 
				System.exit(0);
			}
			
			
			
			tdEDA.startTraining(config);
			
			System.exit(0);
			
			File f = new File("/tmp/training/");
			
			//build up the dataset from training data
			for (File xmi : f.listFiles()) {
				
				if (!xmi.getName().endsWith(".xmi")) {
					continue;
				}
			
				// The annotated pair is added into the CAS.
				JCas jcas = PlatformCASProber.probeXmi(xmi, null);
				EditDistanceTEDecision edtedecision = tdEDA.process(jcas);
				System.out.println(edtedecision.getPairID() + "\t" + 
						edtedecision.getDecision() + " " + 
						edtedecision.getConfidence());
				
			}
		
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		
	}
	
	
}
