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

public class JSTPR_sentence {
	
	Map<String, Double> dictionary = new HashMap<String, Double>();
	Map<String, Integer> sortedMap;
	
	public void readcsv(String real, String pred)
	{
		
		int precision_recall [][] = new int[2][2]; // row for true label and col for predicted label 
		precision_recall[0][0] = 0; // 0 is for neg
		precision_recall[0][1] = 0; // 1 is pos 
		precision_recall[1][0] = 0;
		precision_recall[1][1] = 0;
		
		BufferedReader br_real = null;
		BufferedReader br_pred = null;
		String real_line = "";
		String pred_line = "";
		String cvsSplitBy = " ";
	
		System.out.println("Here");
		try {
	 
			br_real = new BufferedReader(new FileReader(real));
			br_pred = new BufferedReader(new FileReader(pred));
			
			while ((real_line = br_real.readLine()) != null && (pred_line = br_pred.readLine()) != null) {
	 
				String label[] = real_line.split(" ");
				String predict[] = pred_line.split(" ");
				
				for(int i=1; i<predict.length; i++) // i = 0 is not used since it contains docid
				{ 
					int pred_val = Integer.parseInt(predict[i]);
					if( pred_val == 0) continue; // skipping the neutral 
					
					int real_val = Integer.parseInt(label[i]);
					
					if(real_val==2) // 2 for real
					{
						real_val = 0; // 0 for neg
					}
					
					if(pred_val==2)
					{
						pred_val = 0; // 0 for neg
					}
					
					// NO need to change pos as both in both fomrat 1 is pos
					
					precision_recall[real_val][pred_val]++;
					
				}
				
				
			}
			
			
			System.out.println("Confusion Matrix");
			for(int i=0; i<2; i++)
			{
				for(int j=0; j<2; j++)
				{
					System.out.print(precision_recall[i][j]+",");
				}
				System.out.println();
			}
			
			double cons_precision = (double)precision_recall[0][0]/(precision_recall[0][0] + precision_recall[1][0]);
			double pros_precision = (double)precision_recall[1][1]/(precision_recall[0][1] + precision_recall[1][1]);
			
			
			double cons_recall = (double)precision_recall[0][0]/(precision_recall[0][0] + precision_recall[0][1]);
			double pros_recall = (double)precision_recall[1][1]/(precision_recall[1][0] + precision_recall[1][1]);
			
			System.out.println("pros_precision:"+pros_precision+" pros_recall:"+pros_recall);
			System.out.println("cons_precision:"+cons_precision+" cons_recall:"+cons_recall);
			
			
			double pros_f1 = 2/(1/pros_precision + 1/pros_recall);
			double cons_f1 = 2/(1/cons_precision + 1/cons_recall);
			
			System.out.println("F1 measure:pros:"+pros_f1+", cons:"+cons_f1);
	 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
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
		
		
		JSTPR_sentence com = new JSTPR_sentence();
		
        String filename1 = "./data/amazon/MR_test_label.dat";
        String filename2 = "./data/amazon/predicted_label.dat";
        com.readcsv(filename1,filename2);
        //com.readcsv(filename2);
        
		//com.generateCsvFile("./data/Features/selected_combine_fv.txt");
		}

	

}
