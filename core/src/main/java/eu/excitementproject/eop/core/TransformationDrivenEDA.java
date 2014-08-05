package eu.excitementproject.eop.core;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.*;
import java.lang.reflect.Constructor;

import org.apache.uima.jcas.JCas;
import org.uimafit.util.JCasUtil;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

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
 * The <code>TransformationDrivenEDA</code> class implements the <code>EDABasic</code> interface.
 * Given a certain configuration, it can be trained over a specific data set in order to optimize its
 * performance. 
 * 
 * This EDA is based on modelling the Entailment Relations (i.e., Entailment, Not-Entailment) as a 
 * classification problem. First texts (T) are mapped into hypothesis (H) by sequences of editing operations
 * (i.e., insertion, deletion, substitution of text portions) needed to transform T into H, where each edit 
 * operation has a cost associated with it. Then, and this is different from the algorithms which use these 
 * operations to calculate a threshold value that best separates the Entailment Relations from the Not-Entailment
 * ones, the proposed algorithm uses the calculated operations as a feature set to feed a Supervised Learning
 * Classifier System being able to classify the relations between T and H. 
 * 
 * @author roberto zanoli
 * @author silvia colombo
 * 
 * @since August 2014
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
	private final static Logger logger = 
			Logger.getLogger(TransformationDrivenEDA.class.getName());

	/**
	 * the training data directory
	 */
	private String trainDIR;
	
	/**
	 * the test data directory
	 */
	private String testDIR;
	
	/**
	 * save the training data set into arff format so that one
	 * can do experiments by using the WEKA Explorer.
	 */
	private String saveTrainingDatasetInotArffFormat;
	
	/**
	 * if true the transformations about matches are considered as features
	 */
	private boolean match;
	
	/**
	 * if true the transformations about insertions are considered as features
	 */
	private boolean insertion;
	
	/**
	 * if true the transformations about deletions are considered as features
	 */
	private boolean deletion;
	
	/**
	 * if true the transformations about replacement are considered as features
	 */
	private boolean replacement;
	
	/**
	 * verbosity level
	 */
	private String verbosityLevel;
	
	
	/**
	 * get the component used by the EDA
	 * 
	 * @return the component
	 */
	public FixedWeightTreeEditDistance getComponent() {
		
		return this.component;
		
	}
	
	/**
	 * get the type of component
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
	 * The classifier model to be trained during the training phase
	 * and tested during the testing phase.
	 */
	private String classifierModel;
	
	/** 
	 * The feature set used for training and testing
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
     * a number of measures like precision, recall and F1 measure.
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
	 * Construct an TransformationDriven EDA
	 */
	public TransformationDrivenEDA() {
    	
		this.component = null;
		this.classifier = null;
		this.crossValidation = false;
		this.evaluation = null;
        this.trainDIR = null; //"/tmp/training/";
        this.testDIR = null; //"/tmp/test/";
        this.saveTrainingDatasetInotArffFormat = null;
        
    }

	
	@Override
	public void initialize(CommonConfig config) 
			throws ConfigurationException, EDAException, ComponentException {
		
		try {
        	
        	//checking the configuration file
			checkConfiguration(config);
			
			//getting the name value table of the EDA; it contains the methods
			//for getting the EDA configuration form the configuration file.
			NameValueTable nameValueTable = config.getSection(this.getType());
			
			//setting the logger verbosity level
			if (this.verbosityLevel == null) {
				this.verbosityLevel = nameValueTable.getString("verbosity-level");
				logger.setUseParentHandlers(false);
				ConsoleHandler consoleHandler = new ConsoleHandler();
				consoleHandler.setLevel(Level.parse(this.verbosityLevel));
				logger.addHandler(consoleHandler);
				logger.setLevel(Level.parse(this.verbosityLevel));
			}
			
			logger.info("initialize ...");
		
			//setting the training directory
			if (this.trainDIR == null)
				this.trainDIR = nameValueTable.getString("trainDir");
			logger.fine("training directory:" + this.trainDIR);
			
			//setting the test directory
			if (this.testDIR == null)
				this.testDIR = nameValueTable.getString("testDir");
			logger.fine("testing directory:" + this.testDIR);
			
			//evaluation on the training data set
			if (this.crossValidation == false)
				this.crossValidation = Boolean.parseBoolean(nameValueTable.getString("cross-validation"));
			logger.fine("cross-validation:" + this.crossValidation);
			
			//evaluation on the training data set
			if (this.saveTrainingDatasetInotArffFormat == null)
				this.saveTrainingDatasetInotArffFormat = nameValueTable.getString("save-arff-format");
			if (this.saveTrainingDatasetInotArffFormat != null)
				logger.fine("save the data set into arff format in:" + this.saveTrainingDatasetInotArffFormat);
			
			//decide which transformations (i.e. match, insertion, deletion, substitution) have to be
			//considered as features.
			String enambledTransforations = nameValueTable.getString("transformations");
			if (enambledTransforations.indexOf(Transformation.MATCH) != -1)
				this.match = true;
			if (enambledTransforations.indexOf(Transformation.DELETION) != -1)
				this.deletion = true;
			if (enambledTransforations.indexOf(Transformation.INSERTION) != -1)
				this.insertion = true;
			if (enambledTransforations.indexOf(Transformation.REPLACE) != -1)
				this.replacement = true;
			
			//classifier initialization
			String classifierName = nameValueTable.getString("classifier");
			//classifier parameters
			String[] classifierParameters = nameValueTable.getString("classifier-parameters").split(" ");
			
			if (this.classifier == null) {
				
				try {
					
					Class<?> classifierClass = Class.forName(classifierName);
					logger.fine("classifier:" + classifierClass.getCanonicalName());
					Constructor<?> classifierClassConstructor = classifierClass.getConstructor();
					this.classifier = (Classifier) classifierClassConstructor.newInstance();
					if (classifierParameters != null && !classifierParameters.equals(""))
						this.classifier.setOptions(classifierParameters);
					String[] options = this.classifier.getOptions();
					StringBuffer optionsString = new StringBuffer(); 
					for (int i = 0; i < options.length; i++) {
						optionsString.append(options[i]);
						optionsString.append("");
					}
					
					logger.fine("classifier options:" + optionsString.toString());
					
				} catch (Exception e) {
					e.printStackTrace();
					throw new EDAException(e.getMessage());
				}
			
			}
			
			//the classifier model trained during the training phase and to be used during the test phase. 
			classifierModel = nameValueTable.getString("classifier-model");
			
			//component initialization
			String componentName = nameValueTable.getString("components");
			//componentName  = "eu.excitementproject.eop.core.component.distance.FixedWeightTreeEditDistance";
			if (this.component == null) {
				
				try {
					
					Class<?> componentClass = Class.forName(componentName);
					logger.fine("using:" + componentClass.getCanonicalName());
					Constructor<?> componentClassConstructor = componentClass.getConstructor(CommonConfig.class);
					this.component = (FixedWeightTreeEditDistance) componentClassConstructor.newInstance(config);
					
				} catch (Exception e) {
					e.printStackTrace();
					throw new ComponentException(e.getMessage());
					
				}
				
			}
			
			logger.info("done.");

		} catch (ConfigurationException e) {
			
			throw e;
			
		} catch (Exception e) {
            
			throw new EDAException(e.getMessage());
		}
		
	}
	
	
	@Override
	public EditDistanceTEDecision process(JCas jcas) 
			throws EDAException, ComponentException {
		
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
			logger.fine("processing pair: " + pair.getPairID());
			
			/**
			 * this records the gold standard answer for this pair. If the pair 
			 * represents a training data, this value is the gold standard answer. If 
			 * it is a null value, the pair represents a problem that is yet to be answered.
			*/
			String goldAnswer = pair.getGoldAnswer(); //get gold annotation
			logger.fine("annotation class label: " + goldAnswer);
			
			//get the distance between T and H
			double distance = component.calculation(jcas).getDistance();
			logger.fine("distance:" + distance);
			
			//get the transformations needed to transform T into H
			List<Transformation> transformations = component.getTransformations();
			
			HashSet<String> example_i = new HashSet<String>();
			Iterator<Transformation> iteratorTransformation = transformations.iterator();
			while(iteratorTransformation.hasNext()) {
				Transformation transformation_i = iteratorTransformation.next();
				logger.finer("transformation:" + transformation_i);
				String transformation_i_name = transformation_i.print(this.replacement, this.match, this.deletion, this.insertion);
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
			
			logger.fine("annotation class:" + annotationClass);
		
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
            		
            File[] files = f.listFiles();
            //sort files on the bases of their id
            Arrays.sort(files);
            
            //the number of read files
            //int fileCounter = 0;
            
			//reading the training data set
			for (File xmi : files) {
				if (!xmi.getName().endsWith(".xmi")) {
					continue;
				}
				
				//fileCounter++;
				
				// The annotated pair is added into the CAS.
				JCas jcas = PlatformCASProber.probeXmi(xmi, null);
				
				//the T/H pair
				Pair pair = JCasUtil.selectSingle(jcas, Pair.class);
				logger.finer("processing pair: " + pair.getPairID());
				
				//the pair annotation
				String goldAnswer = pair.getGoldAnswer(); //get gold annotation
				logger.finer("gold annotation: " + goldAnswer);
				
				//get the distance between T and H
                double distance = component.calculation(jcas).getDistance();
                logger.fine("distance:" + distance);

				//get the transformations to transform T into H
				List<Transformation> transformations = component.getTransformations();
				
				HashSet<String> example_i = new HashSet<String>();
				Iterator<Transformation> iteratorTransformation = transformations.iterator();
				while(iteratorTransformation.hasNext()) {
					Transformation transformation_i = iteratorTransformation.next();
					
					String transformation_i_name = transformation_i.print(this.replacement, this.match, this.deletion, this.insertion);
					
					logger.finer("transformation_i:" + transformation_i_name);
					
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
			logger.info("number of features:" + (featuresList.size()-1));//-1 due to the fake_attribute
			logger.info("number of classes:" + (classesList.size()-1));//-1 due to the fake class
			//logger.info("input data set:\n" + inputDataset);//the data set on arff format
			
			//save the data set into arff format
			if (this.saveTrainingDatasetInotArffFormat != null)
				this.saveDataset();
			
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
            	evaluatModel();
            }
           
		} catch (Exception e) {
			
			e.printStackTrace();
			throw new EDAException(e.getMessage());
			
		} 
		
		logger.info("done.");
		
	}
	
	
	/**
	 * Evaluates the created model
	 * 
	 * @throws Exception
	 */
	private void evaluatModel() throws Exception {
		
		try {
		
			evaluation = new Evaluation(inputDataset);
	    	evaluation.crossValidateModel(classifier, inputDataset, numFolds, new Random(1));
	    	logger.info("evaluation summary:\n" + evaluation.toSummaryString());
	    	logger.info("detailed accuracy:\n" + evaluation.toClassDetailsString());
		
		} catch (Exception e) {
		
			throw new EDAException(e.getMessage());
		
		} 
    	
	}
	

	/*
	 * Define the data set structure
	 */
	private void initDataSet() throws Exception {
	
		logger.fine("data set initialization ...");
		
		try {
		
			//1) defining the attributes; basically each of the 
			//extracted features is a new attribute, e.g.
			//@attribute attr6 numeric
			//@attribute attr7 numeric
			//@attribute attr8 numeric
			FastVector attributes = new FastVector();
			
			for (Entry<String, Integer> entry : entriesSortedByValues(featuresList)) {
		        String featureName = entry.getKey();
		        
		        if (featureName.indexOf("distance:") != -1)
		        	featureName = featureName.split(":")[0];
		        
				//each of the extracted features is a new attribute
				Attribute attribute_i = new Attribute(featureName);
				//adding the attribute_i into the list of the attributes
				//System.err.println(attribute_i +  "\t" + featuresList.get(featureName));
				attributes.addElement(attribute_i);
				//logger.info("adding attribute_i:" + attributes.size());
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
			//inputDataset.setClassIndex(featuresList.size());
			inputDataset.setClassIndex(inputDataset.numAttributes() - 1);
			//logger.info("data set:\n" + inputDataset);
		
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Data Set initialization error:" + e.getMessage());
			
		} 
		
		logger.fine("done.");
		
	}
	
	
	/**
	 * Adding data into the defined data set
	 */
	private void fillDataSet(List<HashSet<String>> examples, List<String> annotation) 
			throws Exception {
		
		logger.fine("creating data set ...");
		
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
					//logger.finer("feature j:" + feature_j);
					
					if (feature_j.indexOf("distance:") != -1) {
						String new_feature_j = feature_j.split(":")[0];
						//System.err.println(feature_j);
						int featureIndex = featuresList.get(new_feature_j);
						//System.err.println(feature_j + "---");
						double weight = Double.parseDouble(feature_j.split(":")[1]);
						//System.err.println(weight);
						instance_i.setValue(featureIndex, weight);//1.0 is the feature weight
					}
					
					else if (featuresList.containsKey(feature_j)) {
						//System.err.println(feature_j + "\t" +  featuresList.get(feature_j));
						int featureIndex = featuresList.get(feature_j);
						//only the features with weight different from 0 are set
						instance_i.setValue(featureIndex, 1.0);//1.0 is the feature weight
					}
					
				}
				
				if (instance_i.numValues() == 0) {
					int featureIndex;
					featureIndex = featuresList.get("fake_attribute");
					instance_i.setValue(featureIndex, 1.0);//1.0 is the feature weight
				}
				//the last value is that of the annotation class
				instance_i.setValue(featuresList.size(), classesList.indexOf(annotation.get(i)));
				//adding the instance into the data set
				inputDataset.add(instance_i);
				
			}
		
		} catch (Exception e) {
			
			throw new Exception("Creating data set error:" + e.getMessage());
			
		} 
		
		logger.fine("done.");
		
	}
	
	
	/**
	 * Initialize the classes structure
	 */
	private void initClassesList() {
		
		logger.fine("initialize classes list ...");
		
		this.classesList = new FastVector();
		classesList.addElement("fake_class");
		
		logger.fine("done.");
		
	}
	
	
	/**
	 * Initialize the features structure
	 */
	private void initFeaturesList() {
		
		logger.fine("initialize features list ...");
		
		this.featuresList = new HashMap<String,Integer>();
		this.featuresList.put("fake_attribute", 0);
		//this.featuresList.put("distance", 1);
		
		logger.fine("done.");
		
	}
	
	
	/**
	 * Save the data set in arff format to be used with the WEKA Explorer
	 */
	private void saveDataset() throws Exception {
		
		logger.fine("saving data set into arff format ...");
		
		try{
		
			BufferedWriter writer = null;
	    		writer = new BufferedWriter(new OutputStreamWriter(
	                  new FileOutputStream(this.saveTrainingDatasetInotArffFormat, false), "UTF-8"));

	    	PrintWriter printout = new PrintWriter(writer);
	    	printout.print(this.inputDataset);
	    	printout.close();
	    	writer.close();
		    	
	    } catch (Exception e) {
	    	
	    	throw new Exception("Saving data set error:" + e.getMessage());
	    	
	    }
		
	    logger.fine("done.");
	    
	}
	
	
	/**
	 * Save the feature index to be used during the test phase
	 */
	private void saveFeaturesList() throws Exception {
		
		logger.fine("saving features list ...");
		
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
	                  new FileOutputStream(classifierModel + ".feature_list.txt", false), "UTF-8"));

	    	PrintWriter printout = new PrintWriter(writer);
	    	printout.print(stringBuffer);
	    	printout.close();
	    	
	    	writer.close();
		    	
	    } catch (Exception e) {
	    	
	    	throw new Exception("Saving features list error:" + e.getMessage());
	    	
	    }
		
	    logger.fine("done.");
	    
	}
	
	/**
	 * Get the feature index to be used during the test phase
	 */
	private void getFeaturesList() throws Exception {
		
		logger.fine("getting features list ...");
		
		try {
			
			File fileDir = new File(classifierModel + ".feature_list.txt");
	 
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
		
		logger.fine("done.");
		
	}
	
	
	/**
	 * Save the classes index to be used during the test phase
	 */
	private void saveClasses() throws Exception {
		
		logger.fine("saving the classes list ...");
		
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
	                  new FileOutputStream(classifierModel + ".classes.txt", false), "UTF-8"));

	    	PrintWriter printout = new PrintWriter(writer);
	    	printout.print(stringBuffer);
	    	printout.close();
	    	
	    	writer.close();
		    	
	    } catch (Exception e) {
	    	
	    	throw new Exception("Saving the classes error:" + e.getMessage());
	    	
	    }
	    
	    logger.fine("done.");
		
	}
	
	
	/**
	 * Get the classes index to be used during the test phase
	 */
	private void getClasses() throws Exception {
		
		logger.fine("getting the classes list ...");
		
		try {
			
			File fileDir = new File(classifierModel + ".classes.txt");
	 
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
		 
		logger.fine("done.");
		
	}
	
	
	/**
	 * Train the classifier
	 */
	private void trainClassifier() throws Exception {
		
		logger.fine("training the classifier ...");
		
        try {
            
        	//building the classifier
            classifier.buildClassifier(inputDataset);
            //storing the trained classifier to a file for future use
            weka.core.SerializationHelper.write(classifierModel, classifier);
            
        } catch (Exception ex) {
        	ex.printStackTrace();
        	throw new Exception(ex.getMessage());
        	
        }
        
        logger.fine("done.");
        
    }
	
    
	/**
	 * Test the classifier
	 */
    private double[] testClassifier() throws Exception {
    	
    	//it contains a confidence value of each of the predicted classes
    	double[] score = null;
    	
        try {
        	
            //Classifier deserialization
            classifier = (Classifier) weka.core.SerializationHelper.read(classifierModel);
            
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
		
		TransformationDrivenEDA<EditDistanceTEDecision> tdEDA;
		
		try {
		
			tdEDA = new TransformationDrivenEDA<EditDistanceTEDecision>();
			
			File configFile = new File("./src/main/resources/configuration-file/TransformationDrivenEDA_EN.xml");
			
			CommonConfig config = new ImplCommonConfig(configFile);
			
			//tdEDA.test();
			LAPAccess lap = new MaltParserEN();
			// process TE data format, and produce XMI files.
			// Let's process English RTE3 data (formatted as RTE5+) as an example. 

			//File input = new File("/home/zanoli/TBMLEDA/dataset/casi-particolari.xml");

			File input = new File("/hardmnt/norris0/zanoli/TBMLEDA/dataset/SICK_all.xml");
			
			File outputDir  = new File("/hardmnt/norris0/zanoli/TBMLEDA/tmpfiles/");
			try {
				System.out.println(input);
				//lap.processRawInputFormat(input, outputDir); // outputDir will have those XMIs
			} catch (Exception e)
			{
				System.err.println(e.getMessage()); 
			}
			
			tdEDA.startTraining(config);
			
			tdEDA.shutdown();
			
			System.exit(0);
			
			tdEDA.initialize(config);
			
			File f = new File("/hardmnt/norris0/zanoli/TBMLEDA/tmpfiles/");
			
			//build up the dataset from training data
			for (File xmi : f.listFiles()) {
				
				if (!xmi.getName().endsWith(".xmi")) {
					continue;
				}
			
				// The annotated pair is added into the CAS.
				JCas jcas = PlatformCASProber.probeXmi(xmi, null);
				EditDistanceTEDecision edtedecision = tdEDA.process(jcas);
				System.err.println(edtedecision.getPairID() + "\t" + 
						edtedecision.getDecision() + " " + 
						edtedecision.getConfidence());
				
			}
		
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		
	}
	
	/*
	 * awk 'BEGIN{counter = 0; FS="\t";} {pair_ID = $1; sentence_A=$2; sentence_B=$3; relatedness_score=$4; entailment_judgment=$5; if (entailment_judgment == "NEUTRAL") entailment_judgment = "UNKNOWN"; if (counter == 0) {printf("%s\n%s\n", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "<entailment-corpus lang=\"EN\">"); counter = counter + 1;} else{printf("<pair id=\"%s\" entailment=\"%s\" task=\"IR\">\n<t>%s</t>\n<h>%s</h>\n</pair>\n", pair_ID, entailment_judgment, sentence_A, sentence_B);}} END{printf("%s\n", "</entailment-corpus>");}' SICK_train.txt > SICK_train.xml
     *
	 */
	
	
}
