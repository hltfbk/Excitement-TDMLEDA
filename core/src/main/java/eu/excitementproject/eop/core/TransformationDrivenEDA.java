package eu.excitementproject.eop.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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

import org.uimafit.util.JCasUtil;
import org.w3c.dom.*;

import de.tuebingen.uni.sfs.germanet.api.Example;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.trees.J48;
import weka.classifiers.functions.SMO;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.core.converters.CSVLoader;

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
	
	/** The actual classifier. */
	//private Classifier classifier = new J48(); 
	private Classifier classifier = new SMO();
	private ArrayList<String> featureList;;
	//private ArrayList<String> features;
    private FastVector attributeList;
    //the data set for training or test
    private Instances inputDataset;
    //the entailment class: ENTAILMENT, NONENTAILMENT
    private FastVector entailmentClassList;
	
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
	static Logger logger = Logger.getLogger(TransformationDrivenEDA.class.getName());

	/**
	 * the training data directory
	 */
	private String trainDIR;
	
	/**
	 * the tmp directory
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
	 * get the accuracy obtained on the training data set
	 * 
	 * @return the accuracy
	 */
	protected double getTrainingAccuracy() {
		
		return this.trainingAccuracy;
		
	}
	
	/**
	 * Construct an TransformationDriven EDA.
	 */
	public TransformationDrivenEDA() {
    	
		logger.info("Creating an instance of TransformationDrivenEDA ");
		
		this.component = null;
        this.trainDIR = "/tmp/training/";
        this.testDIR = "/tmp/test/";
        this.tmpDIR = "/tmp/temporaryfiles/";
        
        logger.info("done.");
        
    }

	@Override
	public void initialize(CommonConfig config) throws ConfigurationException, EDAException, ComponentException {
		
		logger.info("Initialization ...");
		
		try {
        	
        	//checking the configuration file
			checkConfiguration(config);
			
			//getting the name value table of the EDA
			NameValueTable nameValueTable  = null;
			//= config.getSection(this.getType());
			
			//setting the training directory
			if (this.trainDIR == null)
				this.trainDIR = nameValueTable.getString("trainDir");
			
			//setting the test directory
			if (this.testDIR == null)
				this.testDIR = nameValueTable.getString("testDir");
			
			//setting the test directory
			if (this.tmpDIR == null)
				this.tmpDIR = nameValueTable.getString("tmpDir");

			//component initialization
			String componentName;//  = nameValueTable.getString("components");
			
			componentName  = "eu.excitementproject.eop.core.component.distance.FixedWeightTreeEditDistance";

			
			/*
			if (component == null) {
				
				try {
					
					Class<?> componentClass = Class.forName(componentName);
					logger.info("Using:" + componentClass.getCanonicalName());
					Constructor<?> componentClassConstructor = componentClass.getConstructor(CommonConfig.class);
					this.component = (FixedWeightTreeEditDistance) componentClassConstructor.newInstance(config);
					
				} catch (Exception e) {
					throw new ComponentException(e.getMessage());
				}
				
			}
			*/

			
	    
			
		} catch (ConfigurationException e) {
			throw e;
		} catch (Exception e) {
			throw new EDAException(e.getMessage());
		}
		
		logger.info("done.");
	
	}
	
	@Override
	public EditDistanceTEDecision process(JCas jcas) throws EDAException, ComponentException {
		
		logger.info("Processing ...");
		
		Pair pair = JCasUtil.selectSingle(jcas, Pair.class);
		double score;
		
		try {
				
				//reads the feature list
				//una feature per riga
				File fileDir = new File(tmpDIR + "/" + "feature_list.txt");
			 
				BufferedReader in = new BufferedReader(
					  new InputStreamReader(
			                     new FileInputStream(fileDir), "UTF8"));
			 
				String str;
			 
				while ((str = in.readLine()) != null) {
				
					featureList.add(str);
					//System.out.println(str);
					
				}
			 
			    in.close();
			    	
			} catch (UnsupportedEncodingException e) {
				System.out.println(e.getMessage());
			} catch (IOException e) {
				System.out.println(e.getMessage());
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
			
		
		try {
			
			
			
			String goldAnswer = pair.getGoldAnswer(); //get gold annotation
			//logger.fine("gold answer: " + goldAnswer);
			//get the distance between T and H
			
			//double distance = component.calculation(jcas).getDistance();
			
			//get the transformations to transform T into H
			
			//List<Transformation> transformations = component.getTransformations();
			
			//vi e` gia init data set che inizializza il data set come
			//Transformations,Entailment
			//updateDataSet salva i valori nel file come:
			//"trasformazione1 trasformazione2 ...........",ENTAILMENT
			//populating data set
			
			
			
            //saveDataSet(fileName, true, 
            	//	"\"" + transformations.toString().replaceAll("\"", "'") + 
            	//	"\"", goldAnswer);
            
			//init data set
			saveDataSet(tmpDIR + "/" + "test_data_set.csv", false, 
            		"Transformation", "Entailment");
			if (goldAnswer.equals("ENTAILMENT"))
				saveDataSet(tmpDIR + "/" + "test_data_set.csv", true, 
	            		"\"" + "tr5 tr6 tr7 tr8".replaceAll("\"", "'") + 
	            		"\"", goldAnswer);
			else
				saveDataSet(tmpDIR + "/" + "test_data_set.csv", true, 
            		"\"" + "tr1 tr2 tr3 tr4".replaceAll("\"", "'") + 
            		"\"", goldAnswer);
			
			score = testClassifier(tmpDIR + "/" + "test_data_set.csv");
			System.out.println("===score:" + score);
		
		} catch (Exception e) {
			
			throw new EDAException(e.getMessage());
			
		} 
			
		// During the test phase the method applies the threshold, so that
		// pairs resulting in a distance below the threshold are classiﬁed as ENTAILMENT, while pairs 
		// above the threshold are classiﬁed as NONENTAILMENT.
		if (score <= 0)
			return new EditDistanceTEDecision(DecisionLabel.Entailment, pair.getPairID(), 0);
		
		return new EditDistanceTEDecision(DecisionLabel.NonEntailment, pair.getPairID(), 0);
		
	}
	
	@Override
	public void shutdown() {
		
		logger.info("Shutting down ...");
	        	
		if (component != null)
			((FixedWeightTreeEditDistance)component).shutdown();
		
		this.component = null;
        this.trainDIR = null;
        this.trainDIR = null;
        
        logger.info("done.");
		
	}
	
	@Override
	public void startTraining(CommonConfig config) throws ConfigurationException, EDAException, ComponentException {
		
		logger.info("Training ...");
		
		try {
			
			initialize(config);

			createDataSet(tmpDIR + "/" + "train_data_set.csv", trainDIR);
			
			createFetureList();
			
		    featureList = new ArrayList<String>();
		    attributeList = new FastVector();
			
			
			try {
				
				//reads the feature list
				//una feature per riga
				File fileDir = new File(tmpDIR + "/" + "feature_list.txt");
			 
				BufferedReader in = new BufferedReader(
					  new InputStreamReader(
			                     new FileInputStream(fileDir), "UTF8"));
			 
				String str;
			 
				while ((str = in.readLine()) != null) {
				
					featureList.add(str);
					//System.out.println(str);
					
				}
			 
			    in.close();
			    	
			} catch (UnsupportedEncodingException e) {
				System.out.println(e.getMessage());
			} catch (IOException e) {
				System.out.println(e.getMessage());
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
			
	        //creating an attribute list from the list of feature words
	        entailmentClassList = new FastVector();
	        entailmentClassList.addElement("ENTAILMENT");
	        entailmentClassList.addElement("NONENTAILMENT");
	        
	        for(String feature_i : featureList) {
	            attributeList.addElement(new Attribute(feature_i));
	        }
	        
	        
	        //the last attribute reprsents ths CLASS (Sentiment) of the tweet
	        attributeList.addElement(new Attribute("Entailment", entailmentClassList));
			
			trainClassifier(tmpDIR + "/" + "train_data_set.csv");
			
		} catch (Exception e) {
			
			throw new EDAException(e.getMessage());
			
		} 
		
	}
		
	
	public void createDataSet(String fileName, String dirInput) {
		
		logger.info("Writing data set ...");
		
		try {
			
			//stores gold answers
			ArrayList<String> goldAnswers = new ArrayList<String>();

			File f = new File(dirInput);
			if (f.exists() == false) {
				throw new ConfigurationException(dirInput + ":" + f.getAbsolutePath() + " not found!");
			}
			
			//init data set
			saveDataSet(fileName, false, 
            		"Transformation", "Entailment");
			
			//build up the dataset from training data
			for (File xmi : f.listFiles()) {
				
				if (!xmi.getName().endsWith(".xmi")) {
					continue;
				}
			
				// The annotated pair is added into the CAS.
				JCas jcas = PlatformCASProber.probeXmi(xmi, null);
				Pair pair = JCasUtil.selectSingle(jcas, Pair.class);
				//logger.fine("processing pair: " + pair.getPairID());
				String goldAnswer = pair.getGoldAnswer(); //get gold annotation
				//logger.fine("gold answer: " + goldAnswer);
				//get the distance between T and H
				
				//double distance = component.calculation(jcas).getDistance();
				
				//get the transformations to transform T into H
				
				//List<Transformation> transformations = component.getTransformations();
				
				//vi e` gia init data set che inizializza il data set come
				//Transformations,Entailment
				//updateDataSet salva i valori nel file come:
				//"trasformazione1 trasformazione2 ...........",ENTAILMENT
				//populating data set
				
				
				
                //saveDataSet(fileName, true, 
                	//	"\"" + transformations.toString().replaceAll("\"", "'") + 
                	//	"\"", goldAnswer);
                
                
				if (goldAnswer.equals("ENTAILMENT"))
					saveDataSet(fileName, true, 
	                		"\"" + "tr5 tr6 tr7 tr8".replaceAll("\"", "'") + 
	                		"\"", goldAnswer);
				else
					saveDataSet(fileName, true, 
                		"\"" + "tr1 tr2 tr3 tr4".replaceAll("\"", "'") + 
                		"\"", goldAnswer);
                
			}
			
		} catch (Exception e) {
			System.err.println("errorre:" + e.getMessage());
			//throw new EDAException(e.getMessage());
		} 
		
	}
	
	
	
	private void createFetureList() {
		
		Set<String> features = new HashSet<String>();
		
		try {
			
			//reads the feature list
			//una feature per riga
			File fileDir = new File(tmpDIR + "/" + "train_data_set.csv");
		 
			BufferedReader in = new BufferedReader(
				  new InputStreamReader(
		                     new FileInputStream(fileDir), "UTF8"));
		 
			String str;
		 
			int counter = 0;
			while ((str = in.readLine()) != null) {
			
				if (counter == 0) {
					counter++;
					continue;
				}
				
				String featuresList = str.split("\",")[0];
				featuresList = featuresList.substring(1);
				
				String[] splitFeatures = featuresList.split("\\s");
				
				for (String feature_i : splitFeatures) {
					
					features.add(feature_i);
					
				}
				
			}
		 
		    in.close();
		    	
		} catch (UnsupportedEncodingException e) {
			System.out.println(e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		
		BufferedWriter writer = null;
	    StringBuffer stringBuffer = new StringBuffer();
		
	    try {
	    		
	    	for (String feature_i : features) {
	    		
	    		stringBuffer.append(feature_i);
	    		stringBuffer.append("\n");
	    		
	    	}
	    	
	    	writer = new BufferedWriter(new OutputStreamWriter(
	                  new FileOutputStream(tmpDIR + "/" + "feature_list.txt", false), "UTF-8"));

	    	PrintWriter printout = new PrintWriter(writer);
	    	//"tr1 tr2 tr3", ENTAILMENT
	    	printout.print(stringBuffer);
	    	printout.close();
	    	
	    	writer.close();
		    	
	    } catch (Exception e) {
	    	
	    	System.out.println(e.getMessage());
	    	
	    } 
		
	}
	
	
	
		
	/**
	 * Save the optimized parameters (e.g. threshold) into the configuration file itself
	*/
	private void saveDataSet(String fileName, boolean append, String transformations, String goldAnswer) throws IOException {
	    	
	    BufferedWriter writer = null;
	    
	    try {
	    		
	    	writer = new BufferedWriter(new OutputStreamWriter(
	                  new FileOutputStream(fileName, append), "UTF-8"));

	    	PrintWriter printout = new PrintWriter(writer);
	    	//"tr1 tr2 tr3", ENTAILMENT
	    	printout.println(transformations + "," + goldAnswer);
	    	printout.close();
		    	
	    } catch (Exception e) {
	    	throw new IOException(e.getMessage());
	    } finally {
	    	if (writer != null)
	    		writer.close();
	    }

	}
	
	
	public void trainClassifier(final String INPUT_FILENAME) {
		
		getDataset(INPUT_FILENAME);
            
		//trainingInstances consists of feature vector of every input
		Instances trainingInstances = createInstances("TRAINING_INSTANCES");
            
		for (int i = 0; i < inputDataset.numInstances(); i++) {
		
			Instance instance_i = inputDataset.instance(i);
			//extractFeature method returns the feature vector for the current input
			Instance featureVector_i = getFeatures(instance_i);
			//Make the currentFeatureVector to be added to the trainingInstances
			featureVector_i.setDataset(trainingInstances);
			trainingInstances.add(featureVector_i);
		}
            
        //You can create the classifier that you want. In this tutorial we use NaiveBayes Classifier
        //For instance classifier = new SMO;
        classifier = new NaiveBayes();
            
        try {
            //classifier training code
            classifier.buildClassifier(trainingInstances);
            
            //storing the trained classifier to a file for future use
            weka.core.SerializationHelper.write(tmpDIR + "/" + "NaiveBayes.model",classifier);
            
        } catch (Exception ex) {
            System.out.println("Exception in training the classifier.");
        }
        
    }
    
    
    public double testClassifier(final String INPUT_FILENAME) {
    	
    	double score = 0;
    	
        getDataset(INPUT_FILENAME);
            
        //trainingInstances consists of feature vector of every input
        Instances testingInstances = createInstances("TESTING_INSTANCES");

        for (int i = 0; i < inputDataset.numInstances(); i++) {
    		
			Instance instance_i = inputDataset.instance(i);
			//extractFeature method returns the feature vector for the current input
			Instance featureVector_i = getFeatures(instance_i);
			//Make the currentFeatureVector to be added to the trainingInstances
			featureVector_i.setDataset(testingInstances);
			testingInstances.add(featureVector_i);
		}
        
        try {
        	
            //Classifier deserialization
            classifier = (Classifier) weka.core.SerializationHelper.read(tmpDIR + "/" + "NaiveBayes.model");
            
            for (int i = 0; i < testingInstances.numInstances(); i++) {
                
            	Instance instance_i = testingInstances.instance(i);
                score = classifier.classifyInstance(instance_i);
                System.out.println(testingInstances.attribute("Entailment").value((int)score));
                
            }
            
        } catch (Exception ex) {
        	
            System.out.println("Exception in testing the classifier.");
            
        }
        
        return score;
        
    }
    
    
    private void getDataset(final String INPUT_FILENAME) {
    	
        try{
        	
            //reading the training dataset from CSV file
            CSVLoader trainingLoader = new CSVLoader();
            trainingLoader.setSource(new File(INPUT_FILENAME));
            inputDataset = trainingLoader.getDataSet();
            
        } catch(IOException ex) {
            System.out.println("Exception in getDataset Method");
        }
    }
    
    
    private Instances createInstances(final String INSTANCES_NAME) {
        
        //create an Instances object with initial capacity as zero 
        Instances instances = new Instances(INSTANCES_NAME, attributeList, 0);
        
        //sets the class index as the last attribute (positive or negative)
        instances.setClassIndex(instances.numAttributes()-1);
            
        return instances;
        
    }
    
    
    private Instance getFeatures(Instance inputInstance) {
    	
        Map<Integer,Double> featureMap = new TreeMap<>();
        
        //transformations produced by the Tree Edit Distance components and
        //that are used as features are separated by a space character
        String[] features = inputInstance.stringValue(0).split("\\s");

        for(String feature_i : features) {
        	
        	if(featureList.contains(feature_i)) {
        		//adding 1.0 to the featureMap represents that the feature_i is present in the input data
        		featureMap.put(featureList.indexOf(feature_i), 1.0);
        	}
        	
        }
        
        int indices[] = new int[featureMap.size() + 1];
        double values[] = new double[featureMap.size() + 1];
        int i=0;
        for(Map.Entry<Integer,Double> entry : featureMap.entrySet()) {
        	
            indices[i] = entry.getKey();
            values[i] = entry.getValue();
            i++;
        }
        indices[i] = featureList.size();
        values[i] = (double)entailmentClassList.indexOf(inputInstance.stringValue(1));
        
        return new SparseInstance(1.0, values, indices, featureList.size());
        
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
	
	
	public static void main(String args[]) {
		
		TransformationDrivenEDA tdEDA = new TransformationDrivenEDA();
		
		try {
		
			tdEDA.startTraining(null);
			File f = new File(tdEDA.testDIR);
			
			//build up the dataset from training data
			for (File xmi : f.listFiles()) {
				
				if (!xmi.getName().endsWith(".xmi")) {
					continue;
				}
			
				// The annotated pair is added into the CAS.
				JCas jcas = PlatformCASProber.probeXmi(xmi, null);
				tdEDA.process(jcas);
				
			}
		
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		
	}
	
	
}
