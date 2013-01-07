package ac.biu.nlp.nlp.engineml.rteflow.systems.rtepairs;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import ac.biu.nlp.nlp.datasets.TextHypothesisPair;
import ac.biu.nlp.nlp.instruments.coreference.TreeCoreferenceInformation;
import ac.biu.nlp.nlp.instruments.parse.representation.basic.Info;
import ac.biu.nlp.nlp.instruments.parse.tree.dependency.basic.BasicNode;

/**
 * Contains information about a "pair" in the RTE 1-5 "main task" data-sets. The information
 * is the objects returned by pre-processing utilities, such as parse-trees and co-reference
 * information.
 * 
 * @author Asher Stern
 * 
 * @see PairsPreProcessor
 *
 */
public class PairData extends GenericPairData<Info, BasicNode> implements Serializable
{
	private static final long serialVersionUID = 8113653440646836737L;

	public PairData(TextHypothesisPair pair, List<BasicNode> textTrees,
			BasicNode hypothesisTree,
			Map<BasicNode, String> mapTreesToSentences,
			TreeCoreferenceInformation<BasicNode> coreferenceInformation,
			String datasetName)
	{
		super(pair, textTrees, hypothesisTree, mapTreesToSentences,coreferenceInformation, datasetName);
	}

	public PairData(TextHypothesisPair pair, List<BasicNode> textTrees,
			BasicNode hypothesisTree,
			Map<BasicNode, String> mapTreesToSentences,
			TreeCoreferenceInformation<BasicNode> coreferenceInformation)
	{
		super(pair, textTrees, hypothesisTree, mapTreesToSentences,coreferenceInformation);
	}

	
	
	

}
