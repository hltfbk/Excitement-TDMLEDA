package eu.excitementproject.eop.core.component.distance;

import treedist.TreeImpl;

public class LabeledTree extends TreeImpl {
	
	private int[] labels;
	private FToken[] tokens;
	
	public LabeledTree(int[] parents, int[] labels) {
		super(parents);

		if (parents == null || labels == null)
			throw new NullPointerException();
		if (parents.length != labels.length)
			throw new IllegalArgumentException();
		
		this.labels = labels;
	}
	
	public LabeledTree(int[] parents, int[] labels, FToken[] tokens) {
		super(parents);

		if (parents == null || labels == null || tokens == null)
			throw new NullPointerException();
		if (parents.length != labels.length || parents.length != tokens.length)
			throw new IllegalArgumentException();
		
		this.labels = labels;
		this.tokens = tokens;
		getDeprelRelationsFromNodeToRoot();
		
	}

	public int getLabel(int nodeId) {
		
		return labels[nodeId];
		
	}
	
	public FToken getToken(int nodeId) {
		
		return tokens[nodeId];
		
	}
	
    public FToken[] getTokens() {
		
		return this.tokens;
		
	}
    

    //given a tree and a node it return the path from the node to the root of the tree
    private void getDeprelRelationsFromNodeToRoot() {
		
    	//store the path of each node containing the token from the node to the root of the tree
    	for (int z = 0; z < this.tokens.length; z++) {
    		FToken token_z = this.tokens[z];
    		String relations = "";
    		int nodeId = token_z.getId();
    		//System.out.println("node:" + nodeId);
    		while (nodeId != -1) {
    			String deprel = this.tokens[nodeId].getDeprel();
    			if (relations.length() == 0)
    				relations = deprel;
    			else
    				relations = relations + "#" + deprel;
    			nodeId = this.getParent(nodeId);
    			//System.out.print("====" + nodeId);
    		}
    		//System.out.println();
    		token_z.setDeprelRelations(relations);
    	}
		
	}
    
}