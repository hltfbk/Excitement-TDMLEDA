package eu.excitementproject.eop.core.component.distance;

/**
 * This class represents a token with all the information that is available in the
 * CoNLL-X file produced by the parser, e.g. token id, lemma, form, pos, head, dprel
 * 
 * @author roberto zanoli
 * @author silvia colombo
 * 
 * @since August 2014
 */
public class FToken {

    	//ID Token counter, starting at 1 for each new sentence.
    	private int id;
    	//FORM 	Word form or punctuation symbol.
    	private String form;
    	//LEMMA Lemma or stem (depending on particular data set) of word form, or an underscore if not available. 
    	private String lemma;
    	//STEM; not in CoNLL
    	//private String stem;
    	//PoS
    	private String pos;
    	//HEAD 	Head of the current token, which is either a value of ID or zero ('0'). 
     	//Note that depending on the original treebank annotation, there may be multiple tokens with an ID of zero.
    	private int head;
    	//DEPREL Dependency relation to the HEAD. The set of dependency relations depends on the particular language. 
    	//Note that depending on the original treebank annotation, the dependency relation may be meaningful or simply 'ROOT'.
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
    		this.deprelRelations = null;
    		if (this.lemma.equals("no"))
    			this.deprel = "neg";
    		
    	}
    	
    	
    	/**
    	 * 
    	 * Get the token id
    	 * 
    	 * @return the token id
    	 */
    	public int getId() {
    		
    		return this.id;
    		
    	}
    	
    	
    	/**
    	 * 
    	 * Get the form of the token
    	 * 
    	 * @return the form
    	 */
    	public String getForm() {
    		
    		return this.form;
    		
    	}
    	
    	
    	/**
    	 * 
    	 * Get the lemma of the token
    	 * 
    	 * @return the lemma
    	 */
    	public String getLemma() {
    		
    		return this.lemma;
    		
    	}
    	
    	
    	/**
    	 * Get the stem
    	 */
    	/*
    	public String getStem() {
    		
    		return this.stem;
    		
    	}
    	*/
    	
    	
    	/**
    	 * 
    	 * Get the POS of the token
    	 * 
    	 * @return the pos
    	 */
    	public String getPOS() {
    		
    		return this.pos;
    		
    	}
    	
    	
    	/**
    	 * 
    	 * Get the head of the token
    	 * 
    	 * @return the head
    	 */
    	public int getHead() {
    		
    		return this.head;
    		
    	}
    	
    	
    	/**
    	 * 
    	 * Get the dprel relation
    	 * 
    	 * @return the dprel relation
    	 */
    	public String getDeprel() {
    		
    		return this.deprel;
    		
    	}
    	
    	
    	/**
    	 * 
    	 * set the drpel relations
    	 * 
    	 * @param the dprel relations
    	 */
    	public void setDeprelRelations(String deprelRelations) {
	    		
    		this.deprelRelations = deprelRelations;
	    		
    	}
       
       
    	/**
    	 * 
    	 * Get the dprel relations
    	 * 
    	 * @return the dprel relations
    	 */
    	public String getDeprelRelations() {
    		
    		return this.deprelRelations;
    		
    	}
       
       
    	/**
    	 *  Get a description of the token
    	 *  
    	 *  return the description of the token
    	 */
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
    