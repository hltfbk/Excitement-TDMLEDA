
package eu.excitementproject.eop.core.component.distance;

import org.apache.uima.jcas.JCas;
import org.junit.Test;
import static org.junit.Assert.*;
import eu.excitementproject.eop.lap.LAPAccess;
import eu.excitementproject.eop.lap.LAPException;
import eu.excitementproject.eop.lap.PlatformCASProber;
//import eu.excitementproject.eop.lap.implbase.ExampleLAP;
import eu.excitementproject.eop.lap.dkpro.MaltParserEN;
import eu.excitementproject.eop.lap.dkpro.OpenNLPTaggerEN;

import eu.excitementproject.eop.lap.dkpro.TreeTaggerEN;

public class FixedWeightTreeEditDistanceTest {

	@Test
	public void test() {
    	
        FixedWeightTreeEditDistance fixedEd
            = new FixedWeightLemmaTreeEditDistance();
        

        JCas mycas = null; 
        LAPAccess lap = null; 
        try 
        {
        	lap = new MaltParserEN();
        	
        	String t1 = "The assassin was convicted and sentenced to death penalty";
        	String h1 = "The killer has been accused of murder and doomed to capital punishment";
        	
            //mycas = lap.generateSingleTHPairCAS("I live in an appartment.", "I live in an appartment.");
            mycas = lap.generateSingleTHPairCAS(t1, h1);
            
            //PlatformCASProber.probeCasAndPrintContent(mycas, System.out);
            
            //generateSingleTHPairCAS("The person is hired as a postdoc.", "The person must have a PhD.", "ENTAILMENT"); 
        }
        catch(LAPException e)
        {
        	System.err.println(e.getMessage()); 
        }
        try {
        	
        	System.out.println(fixedEd.calculation(mycas).getDistance());
        	
            //Silvia here we would need to have the produced transformations
        	System.out.println(fixedEd.getTransformations());
        	
        } catch(Exception e) {
        	System.err.println(e.getMessage());
        }
        
    }
	
}

