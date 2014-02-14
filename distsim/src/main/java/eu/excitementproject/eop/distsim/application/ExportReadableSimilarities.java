package eu.excitementproject.eop.distsim.application;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import eu.excitementproject.eop.distsim.items.Element;
import eu.excitementproject.eop.distsim.storage.IdLinkedIntDoubleMapFile;
import eu.excitementproject.eop.distsim.util.Pair;
import eu.excitementproject.eop.distsim.util.Serialization;
import eu.excitementproject.eop.distsim.util.SerializationException;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;


public class ExportReadableSimilarities {
	
	public static void main(String[] args) throws Exception {
		
		/*
		 * Usage: java eu.excitementproject.eop.distsim.application.ExportReadableSimilarities <in id-based similarity file> <out string-based similarity file>
		 * 
		 * Assumption: The parent directory of the given similarity file. contains the 'elements' file, generated by the tool 
		 */
		
		if (args.length != 2) {
			System.out.println("Usage: java eu.excitementproject.eop.distsim.application.ExportReadableSimilarities <in id-based similarity file> <out string-based similarity file>");
			System.exit(0);
		}
		File similarityFile = new File(args[0]);
		TIntObjectMap<String> elementId2Str = new TIntObjectHashMap<String>(); 
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(similarityFile.getParent() + "/elements"),"UTF-8"));
		String line = null;
		while ((line=reader.readLine())!=null) {
			String[] toks = line.split("\t");
			Element element = (Element)Serialization.deserialize(toks[1]);
			elementId2Str.put(element.getID(), element.getData().toString());
		}
		reader.close();
		
		PrintStream out = new PrintStream(args[1],"UTF-8");
		//eu.excitementproject.eop.distsim.storage.File similarities= new IdTroveBasicIntDoubleMapFile(similarityFile, true);
		eu.excitementproject.eop.distsim.storage.File similarities= new IdLinkedIntDoubleMapFile(similarityFile, true);
		similarities.open();
		Pair<Integer, Serializable> pair = null;
		boolean bCont = true;
		while (bCont) {
			try {
				pair = similarities.read();
			} catch (SerializationException e) {
				System.out.println(e.toString());
				continue;
			}
			if (pair == null) {
				bCont = false;
			} else {
				int elementId = pair.getFirst();			
				@SuppressWarnings("unchecked")
				//TroveBasedIDKeyBasicMap<Double> similarity = (TroveBasedIDKeyBasicMap<Double>)pair.getSecond();
				//ImmutableIterator<Pair<Integer, Double>> it = similarity.iterator();
				LinkedHashMap<Integer, Double> similarity = (LinkedHashMap<Integer, Double>)pair.getSecond();
				Iterator<Entry<Integer, Double>> it = similarity.entrySet().iterator();
				while (it.hasNext()) {
					//Pair<Integer, Double> entry = it.next();
					//out.println(elementId2Str.get(elementId) + "\t" + elementId2Str.get(entry.getFirst()) + "\t" + entry.getSecond());
					Entry<Integer, Double> entry = it.next();				
					out.println(elementId2Str.get(elementId) + "\t" + elementId2Str.get(entry.getKey()) + "\t" + entry.getValue());
				}
			}
		}
		out.close();
		similarities.close();
	}


}
