package eu.excitementproject.eop.core.component.distance;

public class FToken {

    	//ID Token counter, starting at 1 for each new sentence.
    	private int id;
    	//FORM 	Word form or punctuation symbol.
    	private String form;
    	//LEMMA Lemma or stem (depending on particular data set) of word form, or an underscore if not available. 
    	private String lemma;
    	//STEM; not in CoNLL
    	private String stem;
    	//PoS
    	private String pos;
    	//HEAD 	Head of the current token, which is either a value of ID or zero ('0'). 
     	//Note that depending on the original treebank annotation, there may be multiple tokens with an ID of zero.
    	private int head;
    	//DEPREL Dependency relation to the HEAD. The set of dependency relations depends on the particular language. 
    	//Note that depending on the original treebank annotation, the dependency relation may be meaningfull or simply 'ROOT'.
    	private String deprel;
    	
    	//dprel relations from the current token to the root
    	private String deprelRelations;
    	
    	public FToken(int id, String form, String lemma, String pos, int head, String deprel) {
    		
    		this.id = id;
    		this.form = form;
    		this.lemma= lemma;
    		this.pos = pos;
    		//this.stem = stem;
    		this.head = head;
    		this.deprel = deprel;
    		//the relation (deprel) moving from the node containing the token and
    		//the root of the tree.
    		this.deprelRelations = null;
    	}
    	
    	public int getId() {
    		
    		return this.id;
    		
    	}
    	
    	public String getForm() {
    		
    		return this.form;
    		
    	}
    	
    	public String getLemma() {
    		
    		return this.lemma;
    		
    	}
    	
    	/*
    	public String getStem() {
    		
    		return this.stem;
    		
    	}
    	*/
    	
    	public String getPOS() {
    		
    		return this.pos;
    		
    	}
    	
    	public int getHead() {
    		
    		return this.head;
    		
    	}
    	
    	public String getDeprel() {
    		
    		return this.deprel;
    		
    	}
    	
       public void setDeprelRelations(String deprelRelations) {
    		
    		this.deprelRelations = deprelRelations;
    		
    	}
       
       public String getDeprelRelations() {
    		
    		return this.deprelRelations;
    		
    	}
       
       public String toString() {
    		
    		return this.id + "__" + 
    			   this.form + "__" + 
    		       this.lemma + "__" + 
    		       this.pos + "__" + 
    			   //this.stem + ":" +
    		       this.head + "__" + 
    			   this.deprel + "__" + 
    		       this.deprelRelations;
    		
    	}
    	
    }
    