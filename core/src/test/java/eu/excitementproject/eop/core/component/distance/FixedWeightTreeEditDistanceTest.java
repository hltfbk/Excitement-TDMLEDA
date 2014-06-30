
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
            mycas = lap.generateSingleTHPairCAS("I live in an appartment.", "I live in an appartment.");
            //PlatformCASProber.probeCasAndPrintContent(mycas, System.out);
            
            //generateSingleTHPairCAS("The person is hired as a postdoc.", "The person must have a PhD.", "ENTAILMENT"); 
        }
        catch(LAPException e)
        {
        	System.err.println(e.getMessage()); 
        }
        try {
        	
        	// get Text
        	JCas tView = mycas.getView("TextView");
        	String tTree = fixedEd.cas2CoNLLX(tView);
        	System.out.println(tTree);
        	
        	// get Hypothesis
	    	JCas hView = mycas.getView("HypothesisView"); 
        	String hTree = fixedEd.cas2CoNLLX(hView);
        	System.out.println(hTree);
        	
        	
        	//System.out.println(fixedEd.calculation(mycas).getDistance());
        	//String[] trees = fixedEd.cas2CoNLLX(mycas);
        	//fixedEd.convert(mycas, null);
        	
        	//System.out.println("T-Tree in CoNLL-X format:" + trees[0]);
        	//System.out.println("H-Tree in CoNLL-X format:" + trees[1]);
        	
        	//assertTrue(fixedEd.calculation(mycas).getDistance() > -1.0);
        	
        } catch(Exception e) {
        	System.err.println(e.getMessage());
        }
        
    }
	
}

