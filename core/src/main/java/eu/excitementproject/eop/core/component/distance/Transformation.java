package eu.excitementproject.eop.core.component.distance;


public class Transformation {
	
	//replace transformation
	protected final static String REPLACE = "rep";
	//match transformation
	protected final static String MATCH = "match";
	//insertion transformation
	protected final static String INSERTION = "ins";
	//deletion transformation
	protected final static String DELELETION = "del";

	//i.e. replace, match, deletion, insertion
	private String type;
	
	//the resource used to do the transformation, e.g. WORDNET__3.0__HYPERNYM__0.5__TtoH  
	private String info;	
	
	//token in the text
	private FToken token_T;		 
	
	//token in the hypothesis 
	private FToken token_H;		
	
	
	/**
	 * constructor for match and rep (replace)
	 * 
	 * @param type the transformation type: deletion, substitution, insertion, deletion
	 * @param info the resource that is used to do the transformation: WORDNET__3.0__HYPERNYM__0.5__TtoH 
	 * 
	 * @param token_T the token T involved in the transformation
	 * @param token_H the token H involved in the transformation
	 * 
	 */
	public Transformation(String type, String info, FToken token_T, FToken token_H){
		
		this.type = type;
		this.info = info;
		this.token_T = token_T;
		this.token_H = token_H;
		
	}
	
	
	/**
	 * @param type the transformation type: deletion, substitution, insertion, deletion
	 * @param token the token involved in the transformation
	 * 
	 */
	public Transformation(String type, FToken token) {
		
		this.type = type;
		if (type.equals(INSERTION)){
			this.token_H = token;
		}
		else
			this.token_T = token;
	}

	
	/**
	 * Get the type
	 * 
	 * @return the type
	 * 
	 */
	public String getType() {
		
		return this.type;
		
	}

	
	/**
	 * Get the info 
	 * 
	 * @return the info
	 * 
	 */
	public String getInfo() {
		
		return this.info;
		
	}

	
	/**
	 * Get the token T
	 * 
	 * @return the token T
	 * 
	 */
	public FToken getToken_T() {
		
		return token_T;
		
	}


	/**
	 * Get the token H
	 * 
	 * @return the token H
	 * 
	 */
	public FToken getToken_H() {
		
		return token_H;
		
	}

	
	/**
	 * Print the transformation
	 * 
	 * @return the transformation
	 * 
	 */
	public String toString() {
		
		switch (this.type) {
		
		case REPLACE:
			
			return ("Type: " + this.type + " Info: " + this.info + " token_T: " + this.token_T + " token_H: " + this.token_H);
			
		case MATCH:
			return ("Type: " + this.type + " Info: " + this.info + " token_T: " + this.token_T + " token_H: " + this.token_H );
		
		case INSERTION:
			return ("Type: " + this.type + " token_H:: " + this.token_H );
		
		default:
			return ("Type: " + this.type + " token_T: " + this.token_T );
		
		}
		
	}
	
	
	/**
	 * Print the transformation when it refers to replace, match, deletion or insertion operations
	 * 
	 * @param replace print the transformation when it is a replace transformation
	 * @param match print the transformation when it is a match transformation
	 * @param deletion print the transformation when it is a deletion transformation
	 * @param insertion print the transformation when it is an insertion transformation
	 * 
	 * @return the transformation
	 */
	public String print(boolean replace, boolean match, boolean deletion, boolean insertion) {
		
		if (type.equals(REPLACE) && replace == true) 
			return ("Type:" + this.type + "#" + "Info:" + this.info + "#" + "T_DPrelR:" + this.token_T.getDeprelRelations() + "#" + "H_DPrelR:" + this.token_H.getDeprelRelations());
			
		else if (type.equals(MATCH) && match == true) 
			return ("Type:" + this.type + "#" + "Info:" + this.info + "#" + "T_DPrelR:" + this.token_T.getDeprelRelations() + "#" + "H_DPrelR:" + this.token_H.getDeprelRelations());
		
		else if (type.equals(INSERTION) && insertion == true) 
			return ("Type:" + this.type + "#" + "H_DPrelR:" + this.token_H.getDeprelRelations() );
		
		else if (type.equals(DELELETION) && deletion == true) 
			return ("Type:" + this.type + "#" + "T_DPrelR:" + this.token_T.getDeprelRelations() );
		
		return null;
		
	}
	
}
