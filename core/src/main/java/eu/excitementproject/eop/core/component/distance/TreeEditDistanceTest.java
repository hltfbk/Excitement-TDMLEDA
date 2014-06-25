package eu.excitementproject.eop.core.component.distance;

import treedist.*;

//import java.util.ArrayList;
import java.util.List;

//import junit.framework.Assert;

import org.junit.Test;

public class TreeEditDistanceTest {
	
	class ScoreImpl implements EditScore {
		
		private final LabeledTree tree1, tree2;

		public ScoreImpl(LabeledTree tree1, LabeledTree tree2) {
			this.tree1 = tree1;
			this.tree2 = tree2;
		}

		@Override
		public double replace(int node1, int node2) {
			
			//if (tree1.getLabel(node1) == tree2.getLabel(node2)) {
			if (tree1.getToken(node1).getForm() == tree2.getToken(node2).getForm()) {
				return 0;
			} else {
				//return 4;
				return 1;
			}
		}
		
		@Override
		public double insert(int node2) {
			//return 3;
			return 1;
		}

		@Override
		public double delete(int node1) {
			//return 2;
			return 1;
		}
	}

	@Test
	public void testSmall() {
		
		//CoNLL format
		//Field number: 	Field name: 	Description:
		//	1 	ID 	Token counter, starting at 1 for each new sentence.
		//	2 	FORM 	Word form or punctuation symbol.
		//	3 	LEMMA 	Lemma or stem (depending on particular data set) of word form, or an underscore if not available.
		//	4 	CPOSTAG 	Coarse-grained part-of-speech tag, where tagset depends on the language.
		//	5 	POSTAG 	Fine-grained part-of-speech tag, where the tagset depends on the language, or identical to the coarse-grained part-of-speech tag if not available.
		//	6 	FEATS 	Unordered set of syntactic and/or morphological features (depending on the particular language), separated by a vertical bar (|), or an underscore if not available.
		//	7 	HEAD 	Head of the current token, which is either a value of ID or zero ('0'). Note that depending on the original treebank annotation, there may be multiple tokens with an ID of zero.
		//	8 	DEPREL 	Dependency relation to the HEAD. The set of dependency relations depends on the particular language. Note that depending on the original treebank annotation, the dependency relation may be meaningfull or simply 'ROOT'.
		//	9 	PHEAD 	Projective head of current token, which is either a value of ID or zero ('0'), or an underscore if not available. Note that depending on the original treebank annotation, there may be multiple tokens an with ID of zero. The dependency structure resulting from the PHEAD column is guaranteed to be projective (but is not available for all languages), whereas the structures resulting from the HEAD column will be non-projective for some sentences of some languages (but is always available).
		//	10 	PDEPREL 	Dependency relation to the PHEAD, or an underscore if not available. The set of dependency relations depends on the particular language. Note that depending on the original treebank annotation, the dependency relation may be meaningfull or simply 'ROOT'.
		
		
		//Text in quite CoNLL format
		//La	1	RS	det+art+f+sing	det	1	__NULL__	2	DET
		//macchina	2	SS	macchina+n+f+sing	macchina	2	__NULL__	3	SUBJ
		//corre	3	VI	correre+v+indic+pres+nil+3+sing	correre	3	__NULL__	0	ROOT
		//veloce	4	AS	veloce+adj+_+sing+pst	veloce	4	__NULL__	3	PRED
		//.	5	XPS	.+punc full_stop+punc	.	5	__NULL__	3	END
		FToken t_token1 = new FToken(1, "La", "det", null, 2, "DET");
		FToken t_token2 = new FToken(2, "macchina", "macchina", null, 3, "SUBJ");
		FToken t_token3 = new FToken(3, "corre", "correre", null, 0, "ROOT");
		FToken t_token4 = new FToken(4, "veloce", "veloce", null, 3, "PRED");
		FToken t_token5 = new FToken(5, ".", ".", null, 3, "END");
		
		
		//Hypothesis in quite CoNLL format
		//La	1	RS	det+art+f+sing	det	1	__NULL__	2	DET
		//macchina	2	SS	macchina+n+f+sing	macchina	2	__NULL__	4	SUBJ
		//non	3	B	non+adv	non	3	__NULL__	4	RMOD
		//corre	4	VI	correre+v+indic+pres+nil+3+sing	correre	4	__NULL__	0	ROOT
		//veloce	5	AS	veloce+adj+_+sing+pst	veloce	5	__NULL__	4	PRED
		//.	6	XPS	.+punc full_stop+punc	.	6	__NULL__	4	END
		FToken h_token1 = new FToken(1, "La", "det", null, 2, "DET");
		FToken h_token2 = new FToken(2, "macchina", "macchina", null, 4, "SUBJ");
		FToken h_token3 = new FToken(3, "non", "non", null, 4, "RMOD");
		FToken h_token4 = new FToken(4, "corre", "correre", null, 0, "ROOT");
		FToken h_token5 = new FToken(5, "veloce", "veloce", null, 4, "PRED");
		FToken h_token6 = new FToken(6, ".", ".", null, 4, "END");
		
		
	    //Tree of Text
		LabeledTree t6 = new LabeledTree( //
		//parents
		//we need to subtract -1 given that the data structure of the code requires that
		//the root is -1 instead of 0;
		new int[] { t_token1.getHead()-1,
				    t_token2.getHead()-1,
				    t_token3.getHead()-1,
				    t_token4.getHead()-1,
				    t_token5.getHead()-1},
		//node Ids
		new int[] { t_token1.getId()-1,
				t_token2.getId()-1,
				t_token3.getId()-1,
				t_token4.getId()-1,
				t_token5.getId()-1},
			    
		//node values
		new FToken[] {t_token1,
				t_token2,
				t_token3,
				t_token4,
				t_token5});
		
		//Tree of Hypothesis
		LabeledTree t7 = new LabeledTree( //
				new int[] { h_token1.getHead()-1,
					    h_token2.getHead()-1,
					    h_token3.getHead()-1,
					    h_token4.getHead()-1,
					    h_token5.getHead()-1,
					    h_token6.getHead()-1},
			//node Ids
			new int[] { h_token1.getId()-1,
					h_token2.getId()-1,
					h_token3.getId()-1,
					h_token4.getId()-1,
					h_token5.getId()-1,
					h_token6.getId()-1},
				    
			//node values
			new FToken[] {h_token1,
					h_token2,
					h_token3,
					h_token4,
					h_token5,
					h_token6});
				
		
		FToken[] tokensInT = t6.getTokens();
		for (int i = 0; i < tokensInT.length; i++) {
			FToken token_i = tokensInT[i];
			String deprelRelations = getDeprelRelationsFromNodeToRoot(t6, token_i.getId() - 1);
			token_i.setDeprelRelations(deprelRelations);
		}
		
		FToken[] tokensInH = t7.getTokens();
		for (int i = 0; i < tokensInH.length; i++) {
			FToken token_i = tokensInH[i];
			String deprelRelations = getDeprelRelationsFromNodeToRoot(t7, token_i.getId() - 1);
			token_i.setDeprelRelations(deprelRelations);
		}
		
		
		
		/*
		// (0 (1) (3 (2)))
		LabeledTree t6 = new LabeledTree( //
						new int[] { 1, 2, -1, 4, 2, 2 }, //
						new int[] { 0, 1, 2, 3, 4, 5},
						new String[] {"il", "bambino", "mangia", "la", "mela", "."});
		
		// (0 (1) (3 (2)))
		LabeledTree t7 = new LabeledTree( //
						new int[] { 1, -1, 3, 1, 5, 3, 1}, //
						new int[] { 0, 1, 2, 3, 4, 5, 6},
						new String[] {"il", "bambino", "non", "mangia", "la", "mela", "."});
		*/

		System.out.println("t6:" + t6);
		System.out.println("t7:" + t7);
		
		// delete
		TreeEditDistance dist = new TreeEditDistance(new ScoreImpl(t6, t7));
		
		//Assert.assertEquals(2.0, dist.calc(t1, t3));
		

		Mapping map = new Mapping(t6, t7);
		double distance = dist.calc(t6, t7, map);
		System.out.println("dist:" + distance);
	    System.err.println("del:" + map.getAllDeletion());
	    System.err.println("ins:" + map.getAllInsertion());
	    System.err.print("rep:"); 
	    List<int[]> lista = map.getAllReplacement();
	    for (int i = 0 ; i < lista.size(); i++) {
	    	int[] pippo = lista.get(i);
	    	for (int j = 0; j < pippo.length; j++)
	    		System.err.print(pippo[j] + ",");
	    	System.err.print(" ");
	    }
	    
	    
	    System.err.println();
	    System.err.println("operations sequence:");
	    
	    List<String> operationSequence = map.getSequence();
	    for (int i = 0; i < operationSequence.size(); i++) {
	    	String operation = (String)operationSequence.get(i);
	    	//e.g. rep:2,3 rep:1,1 rep:0,0 ins:2 rep:3,4 rep:4,5
	    	//System.err.print(operation + " ");
	    	String operationName = operation.split(":")[0];
	    	String nodes = operation.split(":")[1];
	    	if (nodes.contains(",")) {
		    	int node1 = Integer.parseInt(nodes.split(",")[0]);
		    	int node2= Integer.parseInt(nodes.split(",")[1]);
		    	FToken token1 = t6.getToken(node1);
		    	FToken token2 = t7.getToken(node2);
		    	System.err.println(operationName + ":" + token1 + "-->" + token2);
	    	}
	    	else if (operationName.contains("ins")){
	    		int node = Integer.parseInt(nodes);
		    	FToken token = t7.getToken(node);
		    	System.err.println(operationName + ":" + token);
	    	}
	    	else { //deletion
	    		int node = Integer.parseInt(nodes);
		    	FToken token = t6.getToken(node);
		    	System.err.println(operationName + ":" + token);
	    	}
	    		
	    }
	    
	    //if we want to know which words have been deleted, substituted or inserted
	    //we would need to add +1 to the node printed below
	    //if we have: rep:2,3 it means rep:3,4
	    
	    //System.err.println("sequence:" + map.getSequence());
		//Assert.assertEquals(-1, map.getTree1Operation(2));

	    
	    System.exit(0);
	    
	 // (0 (1) (2))
	 		/*
	 		LabeledTree t1 = new LabeledTree( //
	 				new int[] { -1, 0, 0 }, //
	 				new int[] { 0, 1, 2 });
	 		// (0 (1))
	 		LabeledTree t2 = new LabeledTree( //
	 				new int[] { -1, 0 }, //
	 				new int[] { 0, 1 });
	 		// (0 (2 (1)) (3))
	 		LabeledTree t3 = new LabeledTree( //
	 				new int[] { -1, 2, 0, 0 }, //
	 				new int[] { 0, 1, 2, 3 });
	 		// (0 (1) (2))
	 		LabeledTree t4 = new LabeledTree( //
	 				new int[] { -1, 0, 0 }, //
	 				new int[] { 0, 1, 2 });
	 		// (0 (1) (3 (2)))
	 		LabeledTree t5 = new LabeledTree( //
	 				new int[] { -1, 0, 0, 2 }, //
	 				new int[] { 0, 1, 3, 2 });
	 		*/
	    
	    
	    /*
		// insert
		dist = new TreeEditDistance(new ScoreImpl(t1, t3));
		Assert.assertEquals(3.0, dist.calc(t1, t3));

		map = new Mapping(t1, t3);
		dist.calc(t1, t3, map);
		Assert.assertEquals(-1, map.getTree2Operation(2));

		// insert
		dist = new TreeEditDistance(new ScoreImpl(t1, t5));
		Assert.assertEquals(3.0, dist.calc(t1, t5));

		map = new Mapping(t1, t5);
		dist.calc(t1, t5, map);
		Assert.assertEquals(-1, map.getTree2Operation(2));

		// replace
		dist = new TreeEditDistance(new ScoreImpl(t1, t4));
		Assert.assertEquals(4.0, dist.calc(t1, t4));

		map = new Mapping(t1, t4);
		dist.calc(t1, t4, map);
		Assert.assertEquals(2, map.getTree1Operation(2));
		Assert.assertEquals(2, map.getTree2Operation(2));
		
		*/
	}
   /*
	@Test
	public void mid() {
		LabeledTree t1 = new LabeledTree( //
				new int[] { 2, 2, -1 }, //
				new int[] { 0, 1, 2 });
		LabeledTree t2 = new LabeledTree( //
				new int[] { 1, -1, }, //
				new int[] { 1, 2, });
		TreeEditDistance dist = new TreeEditDistance(new ScoreImpl(t1, t2));
		Mapping map = new Mapping(t1, t2);
		double s = dist.calc(t1, t2, map);
		Assert.assertEquals(2.0, s);
	}

	@Test
	public void insertRoot() {
		LabeledTree t1 = new LabeledTree( //
				new int[] { 2, 2, -1 }, //
				new int[] { 0, 1, 2 });
		LabeledTree t2 = new LabeledTree( //
				new int[] { 2, 2, 3, -1 }, //
				new int[] { 0, 1, 2, 3 });
		TreeEditDistance dist = new TreeEditDistance(new ScoreImpl(t1, t2));
		Mapping map = new Mapping(t1, t2);
		double s = dist.calc(t1, t2, map);
		Assert.assertEquals(3.0, s);
	}
	
	*/
	
	private String getDeprelRelationsFromNodeToRoot(LabeledTree tree, int nodeId) {
		
		String relations = "";
		
		while (nodeId != -1) {
			String deprel = tree.getToken(nodeId).getDeprel();
			if (relations.length() == 0)
				relations = deprel;
			else
				relations = relations + "#" + deprel;
			nodeId = tree.getParent(nodeId);
		}
		
		return relations;
		
	}
	
}

