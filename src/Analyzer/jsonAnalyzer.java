/**
 * 
 */
package Analyzer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import json.JSONArray;
import json.JSONObject;
import opennlp.tools.util.InvalidFormatException;
import structures.Post;
import structures._Doc;

/**
 * @author hongning
 * Sample codes for demonstrating OpenNLP package usage 
 */
public class jsonAnalyzer extends DocAnalyzer{
	
	private SimpleDateFormat m_dateFormatter;
	
	public jsonAnalyzer(String tokenModel, int classNo, String providedCV) throws InvalidFormatException, FileNotFoundException, IOException{
		super(tokenModel, classNo, providedCV);		
		m_dateFormatter = new SimpleDateFormat("MMMMM dd,yyyy");//standard date format for this project
	}
	
	//Constructor with ngram and fValue.
	public jsonAnalyzer(String tokenModel, int classNo, String providedCV, int Ngram, int threshold) throws InvalidFormatException, FileNotFoundException, IOException {
		super(tokenModel, classNo, providedCV, Ngram, threshold);
		m_dateFormatter = new SimpleDateFormat("MMMMM dd,yyyy");// standard date format for this project
	}
	
	//Load a document and analyze it.
	@Override
	public void LoadDoc(String filename) {
		try {
			JSONObject json = LoadJson(filename);
			
			/*Post post1 = new Post(json.getJSONObject("ProductInfo"));
			JSONArray jarray = json.getJSONArray("Reviews");
		    if(post1.getProductName()!=null)
			{
	        	System.out.println(post1.getProductID()+":"+jarray.length()+":"+ post1.getProductName());
				
			}*/
			
			JSONArray jarray = json.getJSONArray("Reviews");
			for(int i=0; i<jarray.length(); i++) {
				Post post = new Post(jarray.getJSONObject(i));
				if (checkPostFormat(post)){
					long timeStamp = m_dateFormatter.parse(post.getDate()).getTime();
					AnalyzeDoc(new _Doc(m_corpus.getSize(), post.getID(), post.getTitle() + " " + post.getContent(), (post.getLabel()-1), timeStamp));
				}
			}
		} catch (Exception e) {
			System.out.print("X");
		}
	}
	
	//sample code for loading the json file
	public JSONObject LoadJson(String filename) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			StringBuffer buffer = new StringBuffer(1024);
			String line;
			
			while((line=reader.readLine())!=null) {
				//System.out.println(line);
				buffer.append(line);
			}
			reader.close();
			return new JSONObject(buffer.toString());
		} catch (Exception e) {
			System.out.print("X");
			return null;
		}
	}
	
	//check format for each post
	private boolean checkPostFormat(Post p) {
		if (p.getLabel() <= 0 || p.getLabel() > 5){
			//System.err.format("[Error]Missing Lable or wrong label!!");
			System.out.print('L');
			return false;
		}
		else if (p.getContent() == null){
			//System.err.format("[Error]Missing content!!\n");
			System.out.print('C');
			return false;
		}	
		else if (p.getDate() == null){
			//System.err.format("[Error]Missing date!!\n");
			System.out.print('d');
			return false;
		}
		else {
			// to check if the date format is correct
			try {
				m_dateFormatter.parse(p.getDate());
				//System.out.println(p.getDate());
				return true;
			} catch (ParseException e) {
				System.out.print('D');
			}
			return true;
		} 
	}
}
