
package eu.excitementproject.eop.core;

import org.apache.uima.jcas.JCas;

import java.io.*;

import eu.excitementproject.eop.common.configuration.CommonConfig;
import eu.excitementproject.eop.common.utilities.configuration.ImplCommonConfig;
import eu.excitementproject.eop.lap.LAPAccess;
import eu.excitementproject.eop.lap.PlatformCASProber;
import eu.excitementproject.eop.lap.dkpro.MaltParserEN;

import org.junit.*;
import java.util.logging.Logger;


/** This class tests Edit Distance EDA training and testing it 
 * on a small portion of the RTE-3 data set for English, German and Italian language.
 */
public class TransformationDrivenEDATest {

	static Logger logger = Logger.getLogger(EditDistanceEDATest.class
			.getName());
	

	@Test
	public void test() {
		
		logger.info("testing TransformationDrivenEDA ...");
		testEnglish();
		
	}
	
	/**
	 * test on the Italian data set
	 * 
	 * @return
	 */
	public void testEnglish() {
		
		TransformationDrivenEDA<EditDistanceTEDecision> tdEDA;
		
		try {
		
			tdEDA = new TransformationDrivenEDA<EditDistanceTEDecision>();
			
			File configFile = new File("./src/main/resources/configuration-file/TransformationDrivenEDA_EN.xml");
			
			CommonConfig config = new ImplCommonConfig(configFile);
			
			//tdEDA.test();
			LAPAccess lap = new MaltParserEN();
			// process TE data format, and produce XMI files.
			// Let's process English RTE3 data (formatted as RTE5+) as an example. 

			File input = new File("/tmp/subset.xml");

			//File input = new File("/hardmnt/norris0/zanoli/TBMLEDA/dataset/SICK_all.xml");
			
			File outputDir  = new File("/tmp/training/");
			try {
				System.out.println(input);
				lap.processRawInputFormat(input, outputDir); // outputDir will have those XMIs
			} catch (Exception e)
			{
				System.err.println(e.getMessage()); 
			}
			
			tdEDA.startTraining(config);
			
			tdEDA.shutdown();
			
			System.exit(0);
			
			tdEDA.initialize(config);
			
			File f = new File("/tmp/training/");
			
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
	
}

	