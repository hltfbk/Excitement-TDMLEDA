package eu.excitementproject.eop.core.component.distance;

import java.util.ArrayList;
import java.util.Iterator;


public class Fragment {
	
	private ArrayList<Token> tokens;
	
	public Fragment(ArrayList<Token> tokens){
		this.tokens=tokens;
	}
	
	public Token getToken(int tokenId){
		return tokens.get(tokenId-1);
	}
	
	public void addToken (Token token){
		tokens.add(token);
	}
	
	public void addTokens(ArrayList<Token> addedTokens){
		
	}
	
	public Iterator<Token> getIterator(){
		return tokens.iterator();
	}
		

}
