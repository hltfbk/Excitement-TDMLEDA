package eu.excitementproject.eop.core.component.distance;

import java.util.ArrayList;
import java.util.Iterator;


public class Fragment {
	
	private ArrayList<FToken> tokens;
	
	public Fragment(){
		this.tokens=null;
	}
	
	public Fragment(ArrayList<FToken> tokens){
		this.tokens=tokens;
	}
	
	public FToken getToken(int tokenId){
		return tokens.get(tokenId-1);
	}
	
	public int size(){
		return tokens.size();
	}
	
	public void addToken (FToken token){
		tokens.add(token);
	}
	
	public void addTokens(ArrayList<FToken> addedTokens){
		
	}
	
	public Iterator<FToken> getIterator(){
		return tokens.iterator();
	}
		

}
