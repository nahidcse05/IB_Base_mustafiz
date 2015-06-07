/**
 * 
 */
package topicmodels;

import java.awt.List;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.Collections;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

public class combine_unigram {
	
	Map<String, Double> dictionary = new HashMap<String, Double>();
	Map<String, Integer> sortedMap;
	
	public void readcsv(String csvFile)
	{
		
		BufferedReader br = null;
		String line1 = "";
		String cvsSplitBy = "\t";
	
		System.out.println("Here");
		try {
	 
			br = new BufferedReader(new FileReader(csvFile));
			while ((line1 = br.readLine()) != null) {
	 
				
			        // use comma as separator
				String line[] = line1.split(",");
				System.out.println(line[0]+"&"+line[1]+"&"+line[2]+"\\\\");
				
			
				
	 
			}
	 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	 
		//System.out.println("Done"+smart_system_stop_wordlist.size());
	  
	}
	
	private void generateCsvFile(String sFileName)
    {
 	try
 	{
 		
 		
 		FileWriter writer = new FileWriter(sFileName); // for Matlab
 		//FileWriter writer1 = new FileWriter(sFileName+"_stopword.csv");// for StopWord Removal
 	
	 	   int x= 1;
	 	   for (Map.Entry<String, Double> entry : dictionary.entrySet()) {
			
	 		  writer.append(entry.getKey()+"\n");
	 
	 		   x++;
	 		   }	    
	 	    
 	  
 	    //generate whatever data you want
  
 	   writer.flush();
 	   //writer1.flush();
 	   // writer_log.flush();
 	   writer.close();
 	   //writer1.close();
 	   // writer_log.close();
 	    
 	}
 	catch(IOException e)
 	{
 	     e.printStackTrace();
 	} 
     }
	

	
	public static void main(String[] args) {
		
		
		combine_unigram com = new combine_unigram();
		
        String filename1 = "./data/Features/plsa.csv";
        String filename2 = "./data/Features/prplsa.csv";
        com.readcsv(filename1);
        com.readcsv(filename2);
        
		//com.generateCsvFile("./data/Features/selected_combine_fv.txt");
		}

	

}
