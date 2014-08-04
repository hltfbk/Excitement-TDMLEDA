package eu.excitementproject.eop.core.component.distance;

import treedist.TreeImpl;

/**
 * 
 * This class extends TreeImpl to implement Tree Edit Distance
 * 
 * @author roberto zanoli
 * @author silvia colombo
 * 
 * @since August 2014
 *
 */
public class LabeledTree extends TreeImpl {
	
	private int[] labels;
	private FToken[] tokens;
	
	
	/**
	 * The constructor
	 */
	public LabeledTree(int[] parents, int[] labels) {
		super(parents);

		if (parents == null || labels == null)
			throw new NullPointerException();
		if (parents.length != labels.length)
			throw new IllegalArgumentException();
		
		this.labels = labels;
	}
	
	
	/**
	 * The constructor
	 */
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

	
	/**
	 * Get the label of the node in the tree (i.e. the token id)
	 * 
	 * @return the label of the token
	 */
	protected int getLabel(int nodeId) {
		
		return labels[nodeId];
		
	}
	
	
	/**
	 * Get the token of the specified node in the tree
	 * 
	 * @return the token of the specified node
	 */
	protected FToken getToken(int nodeId) {
		
		return tokens[nodeId];
		
	}
	
	
	/**
	 * Get the list of the tokens in the tree
	 * 
	 * @return the list of the tokens
	 */
    protected FToken[] getTokens() {
		
		return this.tokens;
		
	}
    
    
    /**
     * It calculates the path from the node to the root of the tree
     * 
     */
    private void getDeprelRelationsFromNodeToRoot() {
		
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
    		}
    		token_z.setDeprelRelations(relations);
    	}
		
	}
    
}