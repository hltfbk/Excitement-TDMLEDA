package eu.excitementproject.eop.core.component.distance;

/**
 * 
 * This class represents the transformations that the FixedWeightEditDistance component
 * does to obtain the Hypothesis H from the Text T. The transformation can then be used
 * as features for classifying Entailment relations.
 * 
 * @author roberto zanoli
 * @author silvia colombo
 * 
 * @since August 2014
 */
public class Transformation {
	
	//replace transformation
	public final static String REPLACE = "rep";
	//match transformation
	public final static String MATCH = "match";
	//insertion transformation
	public final static String INSERTION = "ins";
	//deletion transformation
	public final static String DELETION = "del";

	//transformation type, i.e. replace, match, deletion, insertion
	private String type;
	
	//the resource used to do the transformation, 
	//e.g. WORDNET__3.0__HYPERNYM__0.5__TtoH  
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
	 * @param type the transformation type: insertion, deletion
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
	 * Get the type
	 * 
	 * @return the type
	 * 
	 */
	public void setType(String type) {
		
		this.type = type;
		
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
			return ("Type: " + this.type + " Info: " + this.info + " token_T: " + this.token_T + " token_H: " + this.token_H);
		
		case INSERTION:
			return ("Type: " + this.type + " token_H:: " + this.token_H );
		
		default:
			return ("Type: " + this.type + " token_T: " + this.token_T );
		
		}
		
	}
	
	
	/**
	 * Print the transformation
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
			return ("Type:" + this.type + "#" + "Info:" + this.info + "#" + "T_DPrel:" + this.token_T.getDeprel() + "#" + "H_DPrel:" + this.token_H.getDeprel());
		
		else if (type.equals(MATCH) && match == true) 
			return ("Type:" + this.type + "#" + "Info:" + this.info + "#" + "T_DPrel:" + this.token_T.getDeprel() + "#" + "H_DPrel:" + this.token_H.getDeprel());
		
		else if (type.equals(INSERTION) && insertion == true)
		    return ("Type:" + this.type + "#" + "H_DPrel:" + this.token_H.getDeprel() );
		
		else if (type.equals(DELETION) && deletion == true) 
		    return ("Type:" + this.type + "#" + "T_DPrel:" + this.token_T.getDeprel() );
		
		return null;
		
	}
	
}
