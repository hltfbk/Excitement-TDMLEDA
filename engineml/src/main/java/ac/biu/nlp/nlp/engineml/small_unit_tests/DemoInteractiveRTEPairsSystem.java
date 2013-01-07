package ac.biu.nlp.nlp.engineml.small_unit_tests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import ac.biu.nlp.nlp.datasets.TextHypothesisPair;
import ac.biu.nlp.nlp.engineml.classifiers.ClassifierException;
import ac.biu.nlp.nlp.engineml.generic.truthteller.AnnotatorException;
import ac.biu.nlp.nlp.engineml.operations.OperationException;
import ac.biu.nlp.nlp.engineml.operations.rules.RuleBaseException;
import ac.biu.nlp.nlp.engineml.plugin.PluginAdministrationException;
import ac.biu.nlp.nlp.engineml.rteflow.systems.rtepairs.PairProcessor;
import ac.biu.nlp.nlp.engineml.rteflow.systems.rtepairs.interactive.RTEPairsSingleThreadInteractiveSystem;
import ac.biu.nlp.nlp.engineml.script.ScriptException;
import ac.biu.nlp.nlp.engineml.utilities.TeEngineMlException;
import ac.biu.nlp.nlp.engineml.utilities.TreeHistoryUtilities;
import ac.biu.nlp.nlp.general.StringUtil;
import ac.biu.nlp.nlp.general.configuration.ConfigurationException;
import ac.biu.nlp.nlp.general.configuration.ConfigurationFileDuplicateKeyException;
import ac.biu.nlp.nlp.general.text.TextPreprocessorException;
import ac.biu.nlp.nlp.instruments.coreference.CoreferenceResolutionException;
import ac.biu.nlp.nlp.instruments.coreference.TreeCoreferenceInformationException;
import ac.biu.nlp.nlp.instruments.lemmatizer.LemmatizerException;
import ac.biu.nlp.nlp.instruments.ner.NamedEntityRecognizerException;
import ac.biu.nlp.nlp.instruments.parse.ParserRunException;
import ac.biu.nlp.nlp.instruments.parse.tree.dependency.view.TreeStringGenerator.TreeStringGeneratorException;
import ac.biu.nlp.nlp.instruments.sentencesplit.SentenceSplitterException;

public class DemoInteractiveRTEPairsSystem
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		try
		{
			DemoInteractiveRTEPairsSystem app = new DemoInteractiveRTEPairsSystem(args[0],args[1],args[2]);
			app.go();
		}
		catch(Exception e)
		{
			e.printStackTrace(System.out);
		}

	}


	
	public DemoInteractiveRTEPairsSystem(String configurationFileName,
			String preprocessConfigurationModuleName,
			String trainAndTestConfigurationModuleName)
	{
		super();
		this.configurationFileName = configurationFileName;
		this.preprocessConfigurationModuleName = preprocessConfigurationModuleName;
		this.trainAndTestConfigurationModuleName = trainAndTestConfigurationModuleName;
	}



	public void go() throws TeEngineMlException, IOException, ConfigurationFileDuplicateKeyException, PluginAdministrationException, ConfigurationException, LemmatizerException, TextPreprocessorException, SentenceSplitterException, NamedEntityRecognizerException, ParserRunException, CoreferenceResolutionException, TreeCoreferenceInformationException, TreeStringGeneratorException, OperationException, ClassifierException, AnnotatorException, ScriptException, RuleBaseException
	{
		RTEPairsSingleThreadInteractiveSystem system =
				new RTEPairsSingleThreadInteractiveSystem(configurationFileName,
						preprocessConfigurationModuleName,
						trainAndTestConfigurationModuleName);
		
		system.initLogger();
		system.init();
		try
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			
			System.out.println("At any time, type \"exit\" for exit.");
			int pairId=1;
			while (true)
			{
				System.out.println("Enter text:");
				String text = reader.readLine();
				if ("exit".equals(text)) break;
				System.out.println("Enter hypothesis:");
				String hypothesis = reader.readLine();
				if ("exit".equals(hypothesis)) break;

						
				PairProcessor processor = system.processPair(new TextHypothesisPair(text, hypothesis, pairId++, "IR"));
				processor.process();
				boolean result = system.getResult(processor.getBestTree());
				System.out.println("Result = "+result);

				System.out.println("Proof is:\n"+TreeHistoryUtilities.historyToString(processor.getBestTreeHistory()));
				System.out.println(StringUtil.generateStringOfCharacter('=', 60));
				System.out.println();
			}
		}
		finally
		{
			system.cleanUp();
		}

	}

	private String configurationFileName;
	private String preprocessConfigurationModuleName;
	private String trainAndTestConfigurationModuleName;
}
