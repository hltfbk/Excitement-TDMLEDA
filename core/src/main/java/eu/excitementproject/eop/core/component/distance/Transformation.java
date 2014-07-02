package eu.excitementproject.eop.core.component.distance;


public class Transformation {
	
	//type of operation: e.g. rep
	private String operation;	
	
	//type of alignment: e.g. WORDNET__3.0__HYPERNYM__0.5__TtoH  
	private String alignment;	
	
	//token in the text
	private FToken token_T;		 
	
	//token in the hypothesis 
	private FToken token_H;		
	
	//constructor for match and rep (replace)
	public Transformation(String operation, String alignment, FToken token_T, FToken token_H){
		
		this.operation = operation;
		this.alignment = alignment;
		this.token_T = token_T;
		this.token_H = token_H;
		
	}
	
	//constructor for insertion and deletion
	public Transformation(String operation, FToken token){
		
		this.operation = operation;
		if (operation.equals("ins")){
			this.token_H = token;
		}
		else
			this.token_T = token;
	}

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public String getAlignment() {
		return alignment;
	}

	public void setAlignment(String alignment) {
		this.alignment = alignment;
	}

	public FToken getToken_T() {
		return token_T;
	}

	public void setToken_T(FToken token_T) {
		this.token_T = token_T;
	}

	public FToken getToken_H() {
		return token_H;
	}

	public void setToken_H(FToken token_H) {
		this.token_H = token_H;
	}
	
	public String toString(){
		switch (this.operation){
		
		case "rep":
			return ("\noperation: " + this.operation + " token_T: " + this.token_T + " token_H: " + this.token_H);
			
		case "match":
			return ("\noperation: " + this.operation + " alignment: " + this.alignment + " token_T: " + this.token_T + " token_H: " + this.token_H );
		
		case "ins":
			return ("\noperation: " + this.operation + " token_H:: " + this.token_H );
		
		default:
			return ("\noperation: " + this.operation + " token_T: " + this.token_T );
		
		}
		
	}
	
	

}
