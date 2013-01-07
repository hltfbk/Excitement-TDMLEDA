package ac.biu.nlp.nlp.engineml.small_unit_tests.old_small_tests;

import java.util.Set;

import ac.biu.nlp.nlp.engineml.operations.OperationException;
import ac.biu.nlp.nlp.engineml.operations.operations.InsertNodeOperation;
import ac.biu.nlp.nlp.engineml.operations.operations.MoveNodeOperation;
import ac.biu.nlp.nlp.engineml.representation.ExtendedInfo;
import ac.biu.nlp.nlp.engineml.representation.ExtendedNode;
import ac.biu.nlp.nlp.engineml.utilities.TeEngineMlException;
import ac.biu.nlp.nlp.engineml.utilities.parsetreeutils.TreeUtilities;
import ac.biu.nlp.nlp.engineml.utilities.preprocess.ParserFactory;
import ac.biu.nlp.nlp.general.ExceptionUtil;
import ac.biu.nlp.nlp.general.configuration.ConfigurationException;
import ac.biu.nlp.nlp.general.configuration.ConfigurationFile;
import ac.biu.nlp.nlp.general.configuration.ConfigurationParams;
import ac.biu.nlp.nlp.instruments.parse.EnglishSingleTreeParser;
import ac.biu.nlp.nlp.instruments.parse.ParserRunException;
import ac.biu.nlp.nlp.instruments.parse.representation.basic.InfoGetFields;
import ac.biu.nlp.nlp.instruments.parse.tree.AbstractNodeUtils;
import ac.biu.nlp.nlp.instruments.parse.tree.TreeAndParentMap;
import ac.biu.nlp.nlp.instruments.parse.tree.TreeAndParentMap.TreeAndParentMapException;
import ac.biu.nlp.nlp.instruments.parse.tree.dependency.basic.BasicNode;
import ac.biu.nlp.nlp.instruments.parse.tree.dependency.view.TreeStringGenerator.TreeStringGeneratorException;

public class DemoMoveInsert
{
	public static void f(String[] args) throws ConfigurationException, TeEngineMlException, ParserRunException, TreeStringGeneratorException, OperationException, TreeAndParentMapException
	{
		ConfigurationFile confFile = new ConfigurationFile(args[0]);
		ConfigurationParams params = confFile.getModuleConfiguration("prototype1");
		
		String miniparArg = params.get("minipar");
		@SuppressWarnings("deprecation")
		EnglishSingleTreeParser parser = ParserFactory.getParser(miniparArg);
		parser.setSentence("I love you very much.");
		parser.parse();
		BasicNode originalTree1 = parser.getParseTree();
		ExtendedNode tree1 = TreeUtilities.copyFromBasicNode(originalTree1);
		TreeAndParentMap<ExtendedInfo,ExtendedNode> tree1AndParentMap = new TreeAndParentMap<ExtendedInfo,ExtendedNode>(tree1);
		
		String tree1Str = TreeUtilities.treeToString(tree1);
		System.out.println(tree1Str);
		
		Set<ExtendedNode> tree1Set = AbstractNodeUtils.treeToSet(tree1);
		ExtendedNode muchNode = null;
		for (ExtendedNode node : tree1Set)
		{
			if (InfoGetFields.getLemma(node.getInfo()).equalsIgnoreCase("much"))
				muchNode = node;
		}
		if (muchNode!=null)
			System.out.println("found");
		
		ExtendedNode loveNode = null;
		for (ExtendedNode node : tree1Set)
		{
			if (InfoGetFields.getLemma(node.getInfo()).equalsIgnoreCase("love"))
				loveNode = node;
		}
		if (loveNode!=null)
			System.out.println("found");
		
		MoveNodeOperation operation = new MoveNodeOperation(new TreeAndParentMap<ExtendedInfo,ExtendedNode>(tree1), new TreeAndParentMap<ExtendedInfo,ExtendedNode>(tree1), muchNode, loveNode, loveNode.getInfo().getEdgeInfo());
		operation.generate();
		ExtendedNode generatedTree = operation.getGeneratedTree();
		System.out.println(TreeUtilities.treeToString(generatedTree));
		
		System.out.println("-------------------------------------------------------------");
		
		parser.setSentence("John loves you very much.");
		parser.parse();
		BasicNode originalTree2 = parser.getParseTree();
		ExtendedNode tree2 = TreeUtilities.copyFromBasicNode(originalTree2);
		
		ExtendedNode johnNode = null;
		Set<ExtendedNode> tree2Set = AbstractNodeUtils.treeToSet(tree2);
		for (ExtendedNode node : tree2Set)
		{
			if (InfoGetFields.getLemma(node.getInfo()).equalsIgnoreCase("John"))
				johnNode = node;
		}
		if (johnNode!=null)
			System.out.println("found.");
		
		InsertNodeOperation operation2 = new InsertNodeOperation(tree1AndParentMap, new TreeAndParentMap<ExtendedInfo,ExtendedNode>(tree2), johnNode.getInfo(), loveNode);
		operation2.generate();
		ExtendedNode generatedTree2 = operation2.getGeneratedTree();
		System.out.println(TreeUtilities.treeToString(generatedTree2));
		
		
		
		
		
		
	}
	
	public static void main(String[] args)
	{
		try
		{
			f(args);
			
		}
		catch(Exception e)
		{
			ExceptionUtil.outputException(e, System.out);
		}

	}


}
