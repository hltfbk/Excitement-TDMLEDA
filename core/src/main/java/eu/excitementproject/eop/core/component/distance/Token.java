package eu.excitementproject.eop.core.component.distance;

public class Token {

	//ID Token counter, starting at 1 for each new sentence.
	private int id;
	//FORM 	Word form or punctuation symbol.
	private String form;
	//LEMMA Lemma or stem (depending on particular data set) of word form, or an underscore if not available. 
	private String lemma;
	//STEM; not in CoNLL
	private String stem;
	//HEAD 	Head of the current token, which is either a value of ID or zero ('0'). 
 	//Note that depending on the original treebank annotation, there may be multiple tokens with an ID of zero.
	private int head;
	//DEPREL Dependency relation to the HEAD. The set of dependency relations depends on the particular language. 
	//Note that depending on the original treebank annotation, the dependency relation may be meaningfull or simply 'ROOT'.
	private String deprel;
	
	//dprel relations from the current token to the root
	private String deprelRelations;
	
	public Token(int id, String form, String lemma, String stem, int head, String deprel) {
		
		this.id = id;
		this.form = form;
		this.lemma= lemma;
		this.stem = stem;
		this.head = head;
		this.deprel = deprel;
		this.deprelRelations = "";
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
	
	public String getStem() {
		
		return this.stem;
		
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
		
		return this.form + ":" + 
		       this.lemma + ":" + 
			   this.stem + ":" +
		       this.head + ":" + 
			   this.deprel + ":" + 
		       this.deprelRelations;
		
	}
	
}

