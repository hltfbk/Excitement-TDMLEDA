package ac.biu.nlp.nlp.engineml.rteflow.systems.rtepairs.interactive;

import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.log4j.Logger;

import ac.biu.nlp.nlp.datasets.TextHypothesisPair;
import ac.biu.nlp.nlp.engineml.classifiers.Classifier;
import ac.biu.nlp.nlp.engineml.classifiers.ClassifierException;
import ac.biu.nlp.nlp.engineml.classifiers.ClassifierUtils;
import ac.biu.nlp.nlp.engineml.classifiers.LinearClassifier;
import ac.biu.nlp.nlp.engineml.generic.truthteller.AnnotatorException;
import ac.biu.nlp.nlp.engineml.operations.OperationException;
import ac.biu.nlp.nlp.engineml.plugin.PluginAdministrationException;
import ac.biu.nlp.nlp.engineml.rteflow.macro.TreeAndFeatureVector;
import ac.biu.nlp.nlp.engineml.rteflow.preprocess.Instruments;
import ac.biu.nlp.nlp.engineml.rteflow.preprocess.InstrumentsFactory;
import ac.biu.nlp.nlp.engineml.rteflow.systems.ConfigurationParametersNames;
import ac.biu.nlp.nlp.engineml.rteflow.systems.RTESystemsUtils;
import ac.biu.nlp.nlp.engineml.rteflow.systems.rtepairs.ExtendedPairData;
import ac.biu.nlp.nlp.engineml.rteflow.systems.rtepairs.PairData;
import ac.biu.nlp.nlp.engineml.rteflow.systems.rtepairs.PairDataToExtendedPairDataConverter;
import ac.biu.nlp.nlp.engineml.rteflow.systems.rtepairs.PairProcessor;
import ac.biu.nlp.nlp.engineml.rteflow.systems.rtepairs.RTEPairsBaseSystem;
import ac.biu.nlp.nlp.engineml.rteflow.systems.rtepairs.RTEPairsMultiThreadTester;
import ac.biu.nlp.nlp.engineml.rteflow.systems.rtepairs.SinglePairPreProcessor;
import ac.biu.nlp.nlp.engineml.script.OperationsScript;
import ac.biu.nlp.nlp.engineml.script.ScriptFactory;
import ac.biu.nlp.nlp.engineml.utilities.DatasetParameterValueParser;
import ac.biu.nlp.nlp.engineml.utilities.LogInitializer;
import ac.biu.nlp.nlp.engineml.utilities.TeEngineMlException;
import ac.biu.nlp.nlp.general.configuration.ConfigurationException;
import ac.biu.nlp.nlp.general.configuration.ConfigurationFileDuplicateKeyException;
import ac.biu.nlp.nlp.general.configuration.ConfigurationParams;
import ac.biu.nlp.nlp.general.text.TextPreprocessorException;
import ac.biu.nlp.nlp.instruments.coreference.CoreferenceResolutionException;
import ac.biu.nlp.nlp.instruments.coreference.TreeCoreferenceInformationException;
import ac.biu.nlp.nlp.instruments.lemmatizer.LemmatizerException;
import ac.biu.nlp.nlp.instruments.ner.NamedEntityRecognizerException;
import ac.biu.nlp.nlp.instruments.parse.ParserRunException;
import ac.biu.nlp.nlp.instruments.parse.representation.basic.Info;
import ac.biu.nlp.nlp.instruments.parse.tree.dependency.basic.BasicNode;
import ac.biu.nlp.nlp.instruments.parse.tree.dependency.view.TreeStringGenerator.TreeStringGeneratorException;
import ac.biu.nlp.nlp.instruments.sentencesplit.SentenceSplitterException;

/**
 * A system flow similar to RTE-pairs-test flow, but with test samples given
 * interactively.
 * In contrast to {@link RTEPairsMultiThreadTester}, this flow does not get the
 * test samples from a (preprocessed) file, but it provides the method
 * {@link #processPair(TextHypothesisPair)}, in which the user provides a
 * test sample.<P>
 * Usage:
 * <OL>
 * <LI>Call the constructor with configuration-file-name, rte-pairs-preprocessing module-name
 * and rte-pairs train-and-test module-name.</LI>
 * <LI>Call {@link #initLogger()}.</LI>
 * <LI>Call {@link #init()}</LI>
 * <LI>Call {@link #processPair(TextHypothesisPair)} which returns a {@link PairProcessor}</LI>
 * <LI>Call {@link PairProcessor#process()}</LI>
 * <LI>Get the best-tree by calling {@link PairProcessor#getBestTree()}</LI>
 * <LI>Use that best-tree as parameter to {@link #getResult(TreeAndFeatureVector)}</LI>
 * <LI>Process any number of pairs you need. Then call {@link #cleanUp()}</LI>
 * </OL>
 * 
 * @author Asher Stern
 * @since Jun 8, 2012
 *
 */
public class RTEPairsSingleThreadInteractiveSystem extends RTEPairsBaseSystem
{
	public RTEPairsSingleThreadInteractiveSystem(String configurationFileName,
			String preprocessConfigurationModuleName,
			String trainAndTestConfigurationModuleName
			)
	{
		super(configurationFileName, trainAndTestConfigurationModuleName);
		this.preprocessConfigurationModuleName = preprocessConfigurationModuleName;
	}
	
	/**
	 * Initializes log4j
	 * 
	 * @throws IOException
	 * @throws TeEngineMlException
	 */
	public void initLogger() throws IOException, TeEngineMlException
	{
		new LogInitializer(this.configurationFileName).init();

		loggerIsInitialized = true;
	}
	
	/**
	 * In case log4j is already initialized - call this method before calling {@link #init()}.
	 */
	public void acknowledgeLoggerIsAlreadyInitialized()
	{
		loggerIsInitialized = true;
	}

	/**
	 * Initializes the system
	 */
	@Override
	public void init() throws ConfigurationFileDuplicateKeyException, MalformedURLException, TeEngineMlException, PluginAdministrationException, ConfigurationException, LemmatizerException, IOException
	{
		if (!loggerIsInitialized) throw new TeEngineMlException("Logger is not initialized. Please call either initLogger() or acknowledgeLoggerIsAlreadyInitialized() before calling this method.");
		
		super.init();
		
		this.preprocessConfigurationModule = this.configurationFile.getModuleConfiguration(this.preprocessConfigurationModuleName);
		
		DatasetParameterValueParser parameterParser = constructDatasetParameterParser();
		updateFeatureVectorStructure(parameterParser);
		
		logger.info("creating OperationsScript");
		script = new ScriptFactory(this.configurationFile,this.teSystemEnvironment.getPluginRegistry()).getDefaultScript();
		try
		{
			script.init();
		}
		catch (OperationException e)
		{
			throw new TeEngineMlException("Init script failed.",e);
		}
		this.completeInitializationWithScript(script);
		
		try
		{
			this.classifierForSearch = RTESystemsUtils.createOrLoadSearchClassifierForTest(configurationParams, teSystemEnvironment.getFeatureVectorStructureOrganizer());
			logger.info("Tester: classifierForSearch:");
			logger.info(this.classifierForSearch.descriptionOfTraining());

			this.classifierForPredictions = RTESystemsUtils.createOrLoadClassiferForPredictionsForTest(configurationParams, teSystemEnvironment.getFeatureVectorStructureOrganizer());
			logger.info("classifierForPredictions:\n"+this.classifierForPredictions.descriptionOfTraining());
			
		} catch (OperationException e)
		{

		} catch (ClassifierException e)
		{
			throw new TeEngineMlException("Init classifiers failed. See nested exception",e);
		} catch (ClassNotFoundException e)
		{
			throw new TeEngineMlException("Init classifiers failed. See nested exception",e);
		}
		finally
		{}
		
		try
		{
			logger.info("creating instruments");
			InstrumentsFactory instrumentsFactory = new InstrumentsFactory();
			this.instruments = instrumentsFactory.getDefaultInstruments(this.preprocessConfigurationModule);
			
			logger.info("Initializing instruments");
			
			instruments.getParser().init();
			instruments.getNamedEntityRecognizer().init();
			instruments.getCoreferenceResolver().init();
			
			
			
			if (this.preprocessConfigurationModule.containsKey(ConfigurationParametersNames.PREPROCESS_DO_NER))
				doNer = this.preprocessConfigurationModule.getBoolean(ConfigurationParametersNames.PREPROCESS_DO_NER);
			else
				doNer = true;
		
			if (this.preprocessConfigurationModule.containsKey(ConfigurationParametersNames.PREPROCESS_DO_TEXT_NORMALIZATION))
				doTextNormalization = this.preprocessConfigurationModule.getBoolean(ConfigurationParametersNames.PREPROCESS_DO_TEXT_NORMALIZATION);
			else 
				doTextNormalization = true;

		} catch (NumberFormatException e)
		{
			throw new TeEngineMlException("Init instruments failed. See nested exception",e);
		} catch (ParserRunException e)
		{
			throw new TeEngineMlException("Init instruments failed. See nested exception",e);
		} catch (NamedEntityRecognizerException e)
		{
			throw new TeEngineMlException("Init instruments failed. See nested exception",e);
		} catch (TextPreprocessorException e)
		{
			throw new TeEngineMlException("Init instruments failed. See nested exception",e);
		} catch (CoreferenceResolutionException e)
		{
			throw new TeEngineMlException("Init instruments (coreference) failed. See nested exception",e);
		}
		finally
		{}
	}
	
	/**
	 * Makes any clean-ups necessary.
	 */
	@Override
	public void cleanUp()
	{
		super.cleanUp();
		if (script!=null)
			script.cleanUp();
		
		if (instruments!=null)
		{
			if (instruments.getParser()!=null)
				instruments.getParser().cleanUp();
			if (instruments.getNamedEntityRecognizer()!=null)
				instruments.getNamedEntityRecognizer().cleanUp();
			if (instruments.getCoreferenceResolver()!=null)
				instruments.getCoreferenceResolver().cleanUp();
		}
		
		
	}
	
	public PairProcessor processPair(TextHypothesisPair pair) throws TextPreprocessorException, SentenceSplitterException, NamedEntityRecognizerException, ParserRunException, CoreferenceResolutionException, TreeCoreferenceInformationException, TreeStringGeneratorException, TeEngineMlException, MalformedURLException, LemmatizerException, AnnotatorException
	{
		return processPair(pair,null);
		
	}
	
	/**
	 * Returns a {@link PairProcessor} for the given {@link TextHypothesisPair}.
	 * <P>
	 * Usage:
	 * <OL>
	 * <LI>Call {@link #processPair(TextHypothesisPair)} which returns a {@link PairProcessor}</LI>
	 * <LI>Call {@link PairProcessor#process()}</LI>
	 * <LI>Get the best-tree by calling {@link PairProcessor#getBestTree()}</LI>
	 * <LI>Use that best-tree as parameter to {@link #getResult(TreeAndFeatureVector)}</LI>
	 * </OL>
	 * 
	 * @param pair the pair to process
	 * @return A {@link PairProcessor}, which can process this pair by calling its
	 * method {@link PairProcessor#process()}.
	 * 
	 * @throws TextPreprocessorException
	 * @throws SentenceSplitterException
	 * @throws NamedEntityRecognizerException
	 * @throws ParserRunException
	 * @throws CoreferenceResolutionException
	 * @throws TreeCoreferenceInformationException
	 * @throws TreeStringGeneratorException
	 * @throws TeEngineMlException
	 * @throws MalformedURLException
	 * @throws LemmatizerException
	 * @throws AnnotatorException 
	 */
	public PairProcessor processPair(TextHypothesisPair pair, String datasetName) throws TextPreprocessorException, SentenceSplitterException, NamedEntityRecognizerException, ParserRunException, CoreferenceResolutionException, TreeCoreferenceInformationException, TreeStringGeneratorException, TeEngineMlException, MalformedURLException, LemmatizerException, AnnotatorException
	{
		checkCorrectnessOfDatasetName(datasetName!=null);
		logger.info("pre-processing");
		SinglePairPreProcessor preProcessor = new SinglePairPreProcessor(pair,doTextNormalization,doNer,instruments);
		preProcessor.preprocess();
		PairData pairData = null;
		if (null==datasetName)
		{
			pairData = new PairData(pair, preProcessor.getTextTrees(), preProcessor.getHypothesisTree(), preProcessor.getMapTreesToSentences(), preProcessor.getCoreferenceInformation());
		}
		else
		{
			pairData = new PairData(pair, preProcessor.getTextTrees(), preProcessor.getHypothesisTree(), preProcessor.getMapTreesToSentences(), preProcessor.getCoreferenceInformation(),datasetName);
		}
		
		logger.info("creating an ExtendedPairData");
		PairDataToExtendedPairDataConverter converter = new PairDataToExtendedPairDataConverter(pairData,this.teSystemEnvironment);
		converter.convert();
		ExtendedPairData extendedPairData = converter.getExtendedPairData();
		
		logger.info("creating PairProcessor");
		PairProcessor pairProcessor = new PairProcessor(extendedPairData, classifierForSearch, this.getLemmatizer(), script, teSystemEnvironment);
		
		return pairProcessor;
	}
	
	
	 

	/**
	 * Returns the classifier for search used by the system
	 * @return the classifier for search used by the system
	 */
	public LinearClassifier getClassifierForSearch()
	{
		return classifierForSearch;
	}

	/**
	 * Returns the classifier for predictions used by the system
	 * @return the classifier for predictions used by the system
	 */
	public Classifier getClassifierForPredictions()
	{
		return classifierForPredictions;
	}
	
	/**
	 * Returns a boolean answer for the input, which can be provided by
	 * calling {@link PairProcessor#getBestTree()}.
	 * 
	 * @param treeAndFeatureVector
	 * @return
	 * @throws ClassifierException
	 */
	public boolean getResult(TreeAndFeatureVector treeAndFeatureVector) throws ClassifierException
	{
		return ClassifierUtils.classifierResultToBoolean(classifierForPredictions.classify(treeAndFeatureVector.getFeatureVector()));
	}
	
	



	protected String preprocessConfigurationModuleName;
	
	protected ConfigurationParams preprocessConfigurationModule;

	private boolean loggerIsInitialized = false;
	
	protected OperationsScript<Info, BasicNode> script;
	protected LinearClassifier classifierForSearch;
	protected Classifier classifierForPredictions;
	protected Instruments<Info, BasicNode> instruments;
	protected boolean doNer = true;
	protected boolean doTextNormalization = true;

	
	private static final Logger logger = Logger.getLogger(RTEPairsSingleThreadInteractiveSystem.class);
}
