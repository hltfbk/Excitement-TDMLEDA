package eu.excitementproject.eop.core.component.distance;

import treedist.TreeImpl;


public class LabeledTree extends TreeImpl {
	
	private int[] labels;
	private Token[] tokens;
	
	public LabeledTree(int[] parents, int[] labels) {
		super(parents);

		if (parents == null || labels == null)
			throw new NullPointerException();
		if (parents.length != labels.length)
			throw new IllegalArgumentException();
		
		this.labels = labels;
	}
	
	public LabeledTree(int[] parents, int[] labels, Token[] tokens) {
		super(parents);

		if (parents == null || labels == null || tokens == null)
			throw new NullPointerException();
		if (parents.length != labels.length || parents.length != tokens.length)
			throw new IllegalArgumentException();
		
		this.labels = labels;
		this.tokens = tokens;
	}

	public int getLabel(int nodeId) {
		
		return labels[nodeId];
		
	}
	
	public Token getToken(int nodeId) {
		
		return tokens[nodeId];
		
	}
	
    public Token[] getTokens() {
		
		return this.tokens;
		
	}
	
}

