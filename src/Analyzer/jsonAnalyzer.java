/**
 * 
 */
package Analyzer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import opennlp.tools.util.InvalidFormatException;
import structures.Post;
import structures.Product;
import structures._Doc;
import structures.Review;
import utils.Utils;

/**
 * @author hongning
 * Sample codes for demonstrating OpenNLP package usage 
 */
public class jsonAnalyzer extends DocAnalyzer{
	
	private SimpleDateFormat m_dateFormatter;
	private ArrayList<String> m_doc_list; //ArrayList for features
	
	//Constructor with ngram and fValue.
	public jsonAnalyzer(String tokenModel, int classNo, String providedCV, int Ngram, int threshold) throws InvalidFormatException, FileNotFoundException, IOException {
		super(tokenModel, classNo, providedCV, Ngram, threshold);
		m_dateFormatter = new SimpleDateFormat("MMMMM dd,yyyy");// standard date format for this project
		m_doc_list = new ArrayList<String>();
	}
	
	//Constructor with ngram and fValue.
	public jsonAnalyzer(String tokenModel, int classNo, String providedCV, int Ngram, int threshold, String stnModel) throws InvalidFormatException, FileNotFoundException, IOException {
		super(tokenModel, stnModel, classNo, providedCV, Ngram, threshold);
		m_dateFormatter = new SimpleDateFormat("MMMMM dd,yyyy");// standard date format for this project
		m_doc_list = new ArrayList<String>();
	}
	
	
	@Override
	public void LoadDocAmazon(String filename) {
		Product prod = null;
		JSONArray jarray = null;
		
		try {
			JSONObject json = LoadJson(filename);
			prod = new Product(json.getJSONObject("ProductInfo"));
			jarray = json.getJSONArray("Reviews");
		} catch (Exception e) {
			System.out.print('X');
			return;
		}	
		
		for(int i=0; i<jarray.length(); i++) {
			try {
				Post post = new Post(jarray.getJSONObject(i));
				if (checkPostFormat(post)){
					long timeStamp = m_dateFormatter.parse(post.getDate()).getTime();
					String content;
					if (Utils.endWithPunct(post.getTitle()))
						content = post.getTitle() + " " + post.getContent();
					else
						content = post.getTitle() + ". " + post.getContent();
				
					if(!m_doc_list.contains(post.getContent()))
					{	
						m_doc_list.add(post.getContent());
						_Doc review = new _Doc(m_corpus.getSize(), post.getID(), content, prod.getID(), post.getLabel()-1, timeStamp);
						review.from = 2; // 2 means doc from Amazon
						/*if(this.m_stnDetector!=null)
							AnalyzeDocWithStnSplit(review);
						else*/
							AnalyzeDoc(review);
					}
					/*else
					{
						System.out.println("Same");
					}*/
					
				}
			} catch (ParseException e) {
				System.out.print('T');
			} catch (JSONException e) {
				System.out.print('P');
			}
		}
	}
	
	
	
	//Load a document and analyze it.
	@Override
	public void LoadDocNewEgg(String filename, int section) { // section 1 = pros, 2 = cons, 3 = both
		Product prod = null;
		JSONArray jarray = null;
		
		
		try {
			JSONObject json = LoadJson(filename);
			System.out.println(filename);
			//String[] products = {"camera","tablet", "laptop", "phone", "surveillance", "tv"};
			String[] products = {"camera"};
			for(int k = 0; k<products.length; k++){
				
				JSONObject product = json.getJSONObject(products[k]);
				String[] name = json.getNames(product);
				for(int i=0; i<name.length ; i++) {
					jarray = product.getJSONArray(name[i]);
					for(int j=0; j<jarray.length() ; j++) {
					Review review = new Review(jarray.getJSONObject(j));
					
					String pros, cons;
					pros = review.getPros();
					cons = review.getCons();
					_Doc doc;
					
					
					if(section == 3)
					{
						doc = new _Doc(m_corpus.getSize(), pros +" "+ cons, 0);
						doc.from = 1; // 1 means doc from newegg
						AnalyzeDoc(doc, pros , cons); // 1 for pos
					}
					else if(section == 1)
					{
						doc = new _Doc(m_corpus.getSize(), pros, 0);
						doc.from = 1; // 1 means doc from newegg
						AnalyzeDoc(doc, pros , null); // 1 for pos
					}
					else if(section == 2)
					{
						doc = new _Doc(m_corpus.getSize(), cons, 0);
						doc.from = 1; // 1 means doc from newegg
						AnalyzeDoc(doc, null , cons); // 1 for pos
					}
					
					
					
					}
				}
			}
			

			
			
		} catch (Exception e) {
			e.printStackTrace();
			System.out.print("PROB");
			return;
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
			System.out.print("File read error");
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
