package eu.excitementproject.eop.core.component.distance;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * 
 * This class represents a fragment of Text, i.e. the tokens contained in the hypothesis H or text T,
 * 
 * @author roberto zanoli
 * @author silvia colombo
 * 
 * @since August 2014
*/
public class Fragment {
	
	private ArrayList<FToken> tokens;
	
	/**
	 * The constructor
	 */
	protected Fragment(){
		this.tokens= new ArrayList<FToken>();
	}
	
	/**
	 * The constructor
	 */
	protected Fragment(ArrayList<FToken> tokens){
		this.tokens=tokens;
	}
	
	/**
	 * Get the token with id tokenId
	 * 
	 * @param tokenId the token id
	 * 
	 * @return the token
	 */
	protected FToken getToken(int tokenId){
		return tokens.get(tokenId-1);
	}
	
	/**
	 * Get the number of tokens
	 * 
	 * @return the number of tokens
	 */
	protected int size(){
		return tokens.size();
	}
	
	/**
	 * 
	 * Add a new token
	 * 
	 * @param the token to be added
	 */
	public void addToken (FToken token){
		this.tokens.add(token);
	}
	
	/**
	 * 
	 * Add a list of token
	 * 
	 * @param addedTokens the list of tokens to be added
	 */
	public void addTokens(ArrayList<FToken> addedTokens){
		
	}
	
	/**
	 * Get an iterator over the list of tokens
	 * 
	 * @return the iterator
	 */
	public Iterator<FToken> getIterator() {
		
		return tokens.iterator();
		
	}
	
	/**
	 * Print the list of the tokens
	 * 
	 * @return the list of the tokens
	 */
	public String toString(){
		
		String frg = "";
		for(FToken token:tokens){
			frg = frg + "\n" + token.toString();
		}
		
		return frg;
		
	}
		

}

