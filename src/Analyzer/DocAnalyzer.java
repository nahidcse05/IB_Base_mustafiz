package Analyzer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import structures._Doc;
import structures._SparseFeature;
import structures.wordLabel;
import utils.Utils;

public class DocAnalyzer extends Analyzer {
	
	protected int m_lengthThreshold;
	
	PrintWriter corpus_writer = new PrintWriter(new File("./data/amazon/MR.dat"));
	PrintWriter test_corpus_writer = new PrintWriter(new File("./data/amazon/MR_test.dat"));
	PrintWriter test_corpus_label_writer = new PrintWriter(new File("./data/amazon/MR_test_label.dat"));
	
	// for sentence level of JST
	PrintWriter test_corpus_writer_sentence = new PrintWriter(new File("./data/amazon/MR_test_sentence.dat"));
	PrintWriter test_corpus_label_writer_sentence = new PrintWriter(new File("./data/amazon/MR_test_label_sentence.dat"));
	
	
	PrintWriter asum_bag_writer = new PrintWriter(new File("./data/amazon/BagOfSentences.txt"));
	PrintWriter asum_bag_writer_pros_cons = new PrintWriter(new File("./data/amazon/BagOfSentences_pros_cons.txt"));
	
	protected Tokenizer m_tokenizer;
	protected SnowballStemmer m_stemmer;
	protected SentenceDetectorME m_stnDetector;
	Set<String> m_stopwords;
	
	/* Indicate if we can allow new features.After loading the CV file, the flag is set to true, 
	 * which means no new features will be allowed.*/
	protected boolean m_isCVLoaded; 
	protected boolean m_releaseContent;
	public int sentence_counter = 0;
	
	//Constructor with ngram and fValue.
	public DocAnalyzer(String tokenModel, int classNo, String providedCV, int Ngram, int threshold) throws InvalidFormatException, FileNotFoundException, IOException{
		super(tokenModel, classNo);
		m_tokenizer = new TokenizerME(new TokenizerModel(new FileInputStream(tokenModel)));
		m_stemmer = new englishStemmer();
		//m_stnDetector = null; // indicating we don't need sentence splitting
		
		m_stnDetector = new SentenceDetectorME(new SentenceModel(new FileInputStream("./data/Model/en-sent.bin")));
		
		
		m_Ngram = Ngram;
		m_lengthThreshold = threshold;
		m_isCVLoaded = LoadCV(providedCV);
		m_stopwords = new HashSet<String>();
		m_releaseContent = true;
	}
	
	//Constructor with ngram and fValue and sentence check.
	public DocAnalyzer(String tokenModel, String stnModel, int classNo, String providedCV, int Ngram, int threshold) throws InvalidFormatException, FileNotFoundException, IOException{
		super(tokenModel, classNo);
		m_tokenizer = new TokenizerME(new TokenizerModel(new FileInputStream(tokenModel)));
		m_stemmer = new englishStemmer();
		
		//if (stnModel!=null)
			m_stnDetector = new SentenceDetectorME(new SentenceModel(new FileInputStream("./data/Model/en-sent.bin")));
		//else
		//	m_stnDetector = null;
		
		m_Ngram = Ngram;
		m_lengthThreshold = threshold;
		m_isCVLoaded = LoadCV(providedCV);
		m_stopwords = new HashSet<String>();
		m_releaseContent = true;
	}
	
	public void setReleaseContent(boolean release) {
		m_releaseContent = release;
	}
	
	//Load the features from a file and store them in the m_featurNames.@added by Lin.
	protected boolean LoadCV(String filename) {
		if (filename==null || filename.isEmpty())
			return false;
		
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#")){
					if (line.startsWith("#NGram")) {//has to be decoded
						int pos = line.indexOf(':');
						m_Ngram = Integer.valueOf(line.substring(pos+1));
					}
						
				} else 
					expandVocabulary(line);
			}
			reader.close();
			
			System.out.format("%d feature words loaded from %s...\n", m_featureNames.size(), filename);
		} catch (IOException e) {
			System.err.format("[Error]Failed to open file %s!!", filename);
			return false;
		}
		
		return true; // if loading is successful
	}
	
	public void LoadStopwords(String filename) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			String line;

			while ((line = reader.readLine()) != null) {
				line = SnowballStemming(Normalize(line));
				if (!line.isEmpty())
					m_stopwords.add(line);
			}
			reader.close();
			System.out.format("Loading %d stopwords from %s\n", m_stopwords.size(), filename);
		} catch(IOException e){
			System.err.format("[Error]Failed to open file %s!!", filename);
		}
	}
	
	//Tokenizer.
	protected String[] Tokenizer(String source){
		String[] tokens = m_tokenizer.tokenize(source);
		return tokens;
	}
	
	//Normalize.
	protected String Normalize(String token){
		token = Normalizer.normalize(token, Normalizer.Form.NFKC);
		token = token.replaceAll("\\W+", "");
		token = token.toLowerCase();
		
		if (Utils.isNumber(token))
			return "NUM";
		else
			return token;
	}
	
	//Snowball Stemmer.
	protected String SnowballStemming(String token){
		m_stemmer.setCurrent(token);
		if(m_stemmer.stem())
			return m_stemmer.getCurrent();
		else
			return token;
	}
	
	protected boolean isLegit(String token) {
		return !token.isEmpty() 
			&& !m_stopwords.contains(token)
			&& token.length()>1
			&& token.length()<20;
	}
	
	protected boolean isBoundary(String token) {
		return token.isEmpty();//is this a good checking condition?
	}
	
	//Given a long string, tokenize it, normalie it and stem it, return back the string array.
	protected String[] TokenizerNormalizeStemmer(String source){
		String[] tokens = Tokenizer(source); //Original tokens.
		//Normalize them and stem them.		
		for(int i = 0; i < tokens.length; i++)
			tokens[i] = SnowballStemming(Normalize(tokens[i]));
		
		LinkedList<String> Ngrams = new LinkedList<String>();
		int tokenLength = tokens.length, N = m_Ngram;		
		for(int i=0; i<tokenLength; i++) {
			String token = tokens[i];
			boolean legit = isLegit(token);
			if (legit)
				Ngrams.add(token);//unigram
			
			//N to 2 grams
			if (!isBoundary(token)) {
				for(int j=i-1; j>=Math.max(0, i-N+1); j--) {	
					if (isBoundary(tokens[j]))
						break;//touch the boundary
					
					token = tokens[j] + "-" + token;
					legit |= isLegit(tokens[j]);
					if (legit)//at least one of them is legitimate
						Ngrams.add(token);
				}
			}
		}
		
		return Ngrams.toArray(new String[Ngrams.size()]);
	}

	//Load a document and analyze it.
	public void LoadDoc(String filename) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			StringBuffer buffer = new StringBuffer(1024);
			String line;

			while ((line = reader.readLine()) != null) {
				buffer.append(line);
			}
			reader.close();
			
			//How to generalize it to several classes???? 
//			if(filename.contains("pos")){
//				//Collect the number of documents in one class.
//				AnalyzeDoc(new _Doc(m_corpus.getSize(), buffer.toString(), 0));				
//			}else if(filename.contains("neg")){
//				AnalyzeDoc(new _Doc(m_corpus.getSize(), buffer.toString(), 1));
//			}
		} catch(IOException e){
			System.err.format("[Error]Failed to open file %s!!", filename);
			e.printStackTrace();
		}
	}
	
	//Given a long string, return a set of sentences using .?! as delimiter
	// added by Md. Mustafizur Rahman for HTMM Topic Modelling 
	protected String[] findSentence(String source){
		String regexp = "[.?!]+"; 
	    String [] sentences;
	    sentences = source.split(regexp);
	    return sentences;
	}
	

	/*Analyze a document and add the analyzed document back to corpus.	
	 *In the case CV is not loaded, we need two if loops to check. 
	 * The first is if the term is in the vocabulary.***I forgot to check this one!
	 * The second is if the term is in the sparseVector.
	 * In the case CV is loaded, we still need two if loops to check.*/
	// adding sentence splitting function, modified for HTMM
	protected boolean AnalyzeDocWithStnSplit(_Doc doc) {
		String[] sentences = m_stnDetector.sentDetect(doc.getSource());
		
		
		HashMap<Integer, Double> spVct = new HashMap<Integer, Double>(); // Collect the index and counts of features.
		ArrayList<_SparseFeature[]> stnList = new ArrayList<_SparseFeature[]>(); // to avoid empty sentences
		
		for(String sentence : sentences) {
			String[] tokens = TokenizerNormalizeStemmer(sentence);// Three-step analysis.			
			int index = 0;
			double value = 0;
			HashMap<Integer, Double> sentence_vector = new HashMap<Integer, Double>(); 
			
			// Construct the sparse vector.
			for (String token : tokens) {
				// CV is not loaded, take all the tokens as features.
				if (!m_isCVLoaded) {
					if (m_featureNameIndex.containsKey(token)) {
						index = m_featureNameIndex.get(token);
						if (spVct.containsKey(index)) {
							value = spVct.get(index) + 1;
							spVct.put(index, value);
							if(sentence_vector.containsKey(index)){
								value = sentence_vector.get(index) + 1;
								sentence_vector.put(index, value);
							} else {
								sentence_vector.put(index, 1.0);
							}
												
						} else {
							spVct.put(index, 1.0);
							sentence_vector.put(index, 1.0);
							m_featureStat.get(token).addOneDF(doc.getYLabel());
						}
					} else {// indicate we allow the analyzer to dynamically expand the feature vocabulary
						expandVocabulary(token);// update the m_featureNames.
						index = m_featureNameIndex.get(token);
						spVct.put(index, 1.0);
						sentence_vector.put(index, 1.0);
				    	m_featureStat.get(token).addOneDF(doc.getYLabel());
					}
	
					m_featureStat.get(token).addOneTTF(doc.getYLabel());
				} else if (m_featureNameIndex.containsKey(token)) {// CV is loaded.
					index = m_featureNameIndex.get(token);
					if (spVct.containsKey(index)) {
						value = spVct.get(index) + 1;
						spVct.put(index, value);
						if(sentence_vector.containsKey(index)){
							value = sentence_vector.get(index) + 1;
							sentence_vector.put(index, value);
						} else {
							sentence_vector.put(index, 1.0);
						}
					} else {
						spVct.put(index, 1.0);
						sentence_vector.put(index, 1.0);
					
						m_featureStat.get(token).addOneDF(doc.getYLabel());
					}
					m_featureStat.get(token).addOneTTF(doc.getYLabel());
				}
			// if the token is not in the vocabulary, nothing to do.
			}// End for loop for token
			
			if (sentence_vector.size()>0)//avoid empty sentence
				stnList.add(Utils.createSpVct1(sentence_vector));
		} // End For loop for sentence	
	
		//the document should be long enough
		if (spVct.size()>=m_lengthThreshold && stnList.size()>1) { 
			doc.createSpVct1(spVct); // when sentence 1 is dummy parameter
			doc.setSentences(stnList);
			m_corpus.addDoc(doc);
			m_classMemberNo[doc.getYLabel()]++;
			
			if (m_releaseContent)
				doc.clearSource();
			return true;
		} else
			return false;
	}
	
	
	
	
	protected boolean AnalyzeDocWithStnSplitpros(_Doc doc, String pos) {
		String[] sentences = m_stnDetector.sentDetect(pos);
		
		
		HashMap<Integer, Double> spVct = new HashMap<Integer, Double>(); // Collect the index and counts of features.
		ArrayList<_SparseFeature[]> stnList = new ArrayList<_SparseFeature[]>(); // to avoid empty sentences
		
		for(String sentence : sentences) {
			String[] tokens = TokenizerNormalizeStemmer(sentence);// Three-step analysis.			
			int index = 0;
			double value = 0;
			HashMap<Integer, Double> sentence_vector = new HashMap<Integer, Double>(); 
			
			// Construct the sparse vector.
			for (String token : tokens) {
				// CV is not loaded, take all the tokens as features.
				if (!m_isCVLoaded) {
					if (m_featureNameIndex.containsKey(token)) {
						index = m_featureNameIndex.get(token);
						if (spVct.containsKey(index)) {
							value = spVct.get(index) + 1;
							spVct.put(index, value);
							if(sentence_vector.containsKey(index)){
								value = sentence_vector.get(index) + 1;
								sentence_vector.put(index, value);
							} else {
								sentence_vector.put(index, 1.0);
							}
												
						} else {
							spVct.put(index, 1.0);
							sentence_vector.put(index, 1.0);
							m_featureStat.get(token).addOneDF(doc.getYLabel());
						}
					} else {// indicate we allow the analyzer to dynamically expand the feature vocabulary
						expandVocabulary(token);// update the m_featureNames.
						index = m_featureNameIndex.get(token);
						spVct.put(index, 1.0);
						sentence_vector.put(index, 1.0);
				    	m_featureStat.get(token).addOneDF(doc.getYLabel());
					}
	
					m_featureStat.get(token).addOneTTF(doc.getYLabel());
				} else if (m_featureNameIndex.containsKey(token)) {// CV is loaded.
					index = m_featureNameIndex.get(token);
					if (spVct.containsKey(index)) {
						value = spVct.get(index) + 1;
						spVct.put(index, value);
						if(sentence_vector.containsKey(index)){
							value = sentence_vector.get(index) + 1;
							sentence_vector.put(index, value);
						} else {
							sentence_vector.put(index, 1.0);
						}
					} else {
						spVct.put(index, 1.0);
						sentence_vector.put(index, 1.0);
					
						m_featureStat.get(token).addOneDF(doc.getYLabel());
					}
					m_featureStat.get(token).addOneTTF(doc.getYLabel());
				}
			// if the token is not in the vocabulary, nothing to do.
			}// End for loop for token
			
			if (sentence_vector.size()>0)//avoid empty sentence
				stnList.add(Utils.createSpVct1(sentence_vector));
		} // End For loop for sentence	
	
		//the document should be long enough
		if (spVct.size()>=m_lengthThreshold && stnList.size()>1) { 
			doc.setSentences(stnList);
		}
			return true;
	}
	
	
	
	protected boolean AnalyzeDocWithStnSplitcons(_Doc doc, String neg) {
		String[] sentences = m_stnDetector.sentDetect(neg);
		
		
		HashMap<Integer, Double> spVct = new HashMap<Integer, Double>(); // Collect the index and counts of features.
		ArrayList<_SparseFeature[]> stnList = new ArrayList<_SparseFeature[]>(); // to avoid empty sentences
		
		for(String sentence : sentences) {
			String[] tokens = TokenizerNormalizeStemmer(sentence);// Three-step analysis.			
			int index = 0;
			double value = 0;
			HashMap<Integer, Double> sentence_vector = new HashMap<Integer, Double>(); 
			
			// Construct the sparse vector.
			for (String token : tokens) {
				// CV is not loaded, take all the tokens as features.
				if (!m_isCVLoaded) {
					if (m_featureNameIndex.containsKey(token)) {
						index = m_featureNameIndex.get(token);
						if (spVct.containsKey(index)) {
							value = spVct.get(index) + 1;
							spVct.put(index, value);
							if(sentence_vector.containsKey(index)){
								value = sentence_vector.get(index) + 1;
								sentence_vector.put(index, value);
							} else {
								sentence_vector.put(index, 1.0);
							}
												
						} else {
							spVct.put(index, 1.0);
							sentence_vector.put(index, 1.0);
							m_featureStat.get(token).addOneDF(doc.getYLabel());
						}
					} else {// indicate we allow the analyzer to dynamically expand the feature vocabulary
						expandVocabulary(token);// update the m_featureNames.
						index = m_featureNameIndex.get(token);
						spVct.put(index, 1.0);
						sentence_vector.put(index, 1.0);
				    	m_featureStat.get(token).addOneDF(doc.getYLabel());
					}
	
					m_featureStat.get(token).addOneTTF(doc.getYLabel());
				} else if (m_featureNameIndex.containsKey(token)) {// CV is loaded.
					index = m_featureNameIndex.get(token);
					if (spVct.containsKey(index)) {
						value = spVct.get(index) + 1;
						spVct.put(index, value);
						if(sentence_vector.containsKey(index)){
							value = sentence_vector.get(index) + 1;
							sentence_vector.put(index, value);
						} else {
							sentence_vector.put(index, 1.0);
						}
					} else {
						spVct.put(index, 1.0);
						sentence_vector.put(index, 1.0);
					
						m_featureStat.get(token).addOneDF(doc.getYLabel());
					}
					m_featureStat.get(token).addOneTTF(doc.getYLabel());
				}
			// if the token is not in the vocabulary, nothing to do.
			}// End for loop for token
			
			if (sentence_vector.size()>0)//avoid empty sentence
				stnList.add(Utils.createSpVct1(sentence_vector));
		} // End For loop for sentence	
	
		//the document should be long enough
		if (spVct.size()>=m_lengthThreshold && stnList.size()>1) { 
			
			doc.setSentencesneg(stnList);
			
		} 
		return true;
	}
	
	
	/*Analyze a document and add the analyzed document back to corpus.
	 *In the case CV is not loaded, we need two if loops to check.
	 * The first is if the term is in the vocabulary.***I forgot to check this one!
	 * The second is if the term is in the sparseVector.
	 * In the case CV is loaded, we still need two if loops to check.*/
	protected boolean AnalyzeDoc(_Doc doc) {
		String[] tokens = TokenizerNormalizeStemmer(doc.getSource());// Three-step analysis.
		HashMap<Integer, Double> spVct = new HashMap<Integer, Double>(); // Collect the index and counts of features.
		
		
		
		
		
		int index = 0;
		double value = 0;
		// Construct the sparse vector.
		for (String token : tokens) {
			
			
			// CV is not loaded, take all the tokens as features.
			if (!m_isCVLoaded) {
				if (m_featureNameIndex.containsKey(token)) {
					index = m_featureNameIndex.get(token);
					if (spVct.containsKey(index)) {
						value = spVct.get(index) + 1;
						spVct.put(index, value);
					} else {
						spVct.put(index, 1.0);
						m_featureStat.get(token).addOneDF(doc.getYLabel());
					}
				} else {// indicate we allow the analyzer to dynamically expand the feature vocabulary
					expandVocabulary(token);// update the m_featureNames.
					index = m_featureNameIndex.get(token);
					spVct.put(index, 1.0);
					m_featureStat.get(token).addOneDF(doc.getYLabel());
				}
				m_featureStat.get(token).addOneTTF(doc.getYLabel());
			} else if (m_featureNameIndex.containsKey(token)) {// CV is loaded.
				index = m_featureNameIndex.get(token);
				if (spVct.containsKey(index)) {
					value = spVct.get(index) + 1;
					spVct.put(index, value);
				} else {
					spVct.put(index, 1.0);
					m_featureStat.get(token).addOneDF(doc.getYLabel());
				}
				m_featureStat.get(token).addOneTTF(doc.getYLabel());
			}
			// if the token is not in the vocabulary, nothing to do.
		}
		
		
		if (spVct.size()>=m_lengthThreshold) {//temporary code for debugging purpose
			doc.createSpVctAmazon(spVct);
			
			corpus_writer.write("d"+m_corpus.getSize()+" ");
			
			m_corpus.addDoc(doc);
			
			for (String token : tokens) {
				corpus_writer.write(token+" ");
			}
			
			corpus_writer.write("\n\n");
			
			
			String[] sentences = m_stnDetector.sentDetect(doc.getSource());
			asum_bag_writer.write(sentences.length+"\n");
			for(String sentence : sentences) {
				tokens = TokenizerNormalizeStemmer(sentence);			
				for (String token : tokens) {
					if (m_featureNameIndex.containsKey(token)) {
					index = m_featureNameIndex.get(token);
					asum_bag_writer.write(index+" ");
					}
				}
				asum_bag_writer.write("\n");
			}
			
			m_classMemberNo[doc.getYLabel()]++;
			if (m_releaseContent)
				doc.clearSource();
			return true;
		} else
			return false;
	}
	
	
	/*Analyze a document and add the analyzed document back to corpus.
	 *In the case CV is not loaded, we need two if loops to check.
	 * The first is if the term is in the vocabulary.***I forgot to check this one!
	 * The second is if the term is in the sparseVector.
	 * In the case CV is loaded, we still need two if loops to check.*/
	protected boolean AnalyzeDoc(_Doc doc, String pos, String neg) {
		
		

		int index = 0;
		double value = 0;
		
		HashMap<Integer, wordLabel> spVct = new HashMap<Integer, wordLabel>(); // Collect the index and counts of features.
		HashMap<Integer, wordLabel> spVctneg = new HashMap<Integer, wordLabel>(); // Collect the index and counts of features.
		
		String[] pos_tokens = null;
		String[] neg_tokens = null;
		
		if(pos!=null){
			pos_tokens = TokenizerNormalizeStemmer(pos);// Three-step analysis.
			


			// Construct the sparse vector for pos
			for (String token : pos_tokens) {
				// CV is not loaded, take all the tokens as features.

				if (!m_isCVLoaded) {
					if (m_featureNameIndex.containsKey(token)) {
						index = m_featureNameIndex.get(token);
						if (spVct.containsKey(index)) {
							value = spVct.get(index).getValue() + 1;

							wordLabel w = new wordLabel();
							w.poscount = spVct.get(index).poscount + 1;
							w.setValue(value);
							w.setposLabel(1);
							w.setnegLabel(spVct.get(index).getnegLabel());
							spVct.put(index, w);
						} else {
							value = 1.0;
							wordLabel w = new wordLabel();
							w.setValue(value);
							w.setposLabel(1);
							w.setnegLabel(0);
							w.poscount = 1;
							spVct.put(index, w);
							m_featureStat.get(token).addOneDF(doc.getYLabel());
						}
					} else {// indicate we allow the analyzer to dynamically expand the feature vocabulary
						expandVocabulary(token);// update the m_featureNames.
						index = m_featureNameIndex.get(token);
						value = 1.0;
						wordLabel w = new wordLabel();
						w.setValue(value);
						w.setposLabel(1);
						w.setnegLabel(0);
						w.poscount = 1;
						spVct.put(index, w);
						m_featureStat.get(token).addOneDF(doc.getYLabel());
					}
					m_featureStat.get(token).addOneTTF(doc.getYLabel());
				} else if (m_featureNameIndex.containsKey(token)) {// CV is loaded.
					index = m_featureNameIndex.get(token);
					if (spVct.containsKey(index)) {
						value = spVct.get(index).getValue() + 1;
						wordLabel w = new wordLabel();
						w.poscount = spVct.get(index).poscount + 1;
						w.setValue(value);
						w.setposLabel(1);
						w.setnegLabel(spVct.get(index).getnegLabel());
						spVct.put(index, w);
					} else {
						value = 1.0;
						wordLabel w = new wordLabel();
						w.setValue(value);
						w.setposLabel(1);
						w.setnegLabel(0);
						w.poscount = 1;
						spVct.put(index, w);
						m_featureStat.get(token).addOneDF(doc.getYLabel());
					}
					m_featureStat.get(token).addOneTTF(doc.getYLabel());
				}
				// if the token is not in the vocabulary, nothing to do.
			}
		}
		
//		index = 0;
//		value = 0.0;
		
		if(neg!=null)
		{
			neg_tokens = TokenizerNormalizeStemmer(neg);// Three-step analysis.
		
		
		// Construct the sparse vector for pos
				for (String token : neg_tokens) {
					
					
					// CV is not loaded, take all the tokens as features.
					if (!m_isCVLoaded) {
						if (m_featureNameIndex.containsKey(token)) {
							index = m_featureNameIndex.get(token);
							if (spVctneg.containsKey(index)) {
								value = spVctneg.get(index).getValue() + 1;
								wordLabel w = new wordLabel();
								w.negcount = spVctneg.get(index).negcount + 1;
								w.setValue(value);
								w.setnegLabel(1);
							    w.setposLabel(spVctneg.get(index).getposLabel());
							    spVctneg.put(index, w);
							} else {
								value = 1.0;
								wordLabel w = new wordLabel();
								w.setValue(value);
								w.setnegLabel(1);
							    w.setposLabel(0);
							    w.negcount = 1;
							    spVctneg.put(index, w);
								m_featureStat.get(token).addOneDF(doc.getYLabel());
							}
						} else {// indicate we allow the analyzer to dynamically expand the feature vocabulary
							expandVocabulary(token);// update the m_featureNames.
							index = m_featureNameIndex.get(token);
							value = 1.0;
							wordLabel w = new wordLabel();
							w.setValue(value);
							w.setposLabel(0);
						    w.setnegLabel(1);
						    w.negcount = 1;
						    spVctneg.put(index, w);
							m_featureStat.get(token).addOneDF(doc.getYLabel());
						}
						m_featureStat.get(token).addOneTTF(doc.getYLabel());
					} else if (m_featureNameIndex.containsKey(token)) {// CV is loaded.
						index = m_featureNameIndex.get(token);
						if (spVctneg.containsKey(index)) {
							value = spVctneg.get(index).getValue() + 1;
							wordLabel w = new wordLabel();
							w.negcount = spVctneg.get(index).negcount + 1;
							w.setValue(value);
							w.setnegLabel(1);
						    w.setposLabel(spVctneg.get(index).getposLabel());
						    spVctneg.put(index, w);
						} else {
							value = 1.0;
							wordLabel w = new wordLabel();
							w.setValue(value);
							w.setnegLabel(1);
						    w.setposLabel(0);
						    w.negcount = 1;
						    spVctneg.put(index, w);
							m_featureStat.get(token).addOneDF(doc.getYLabel());
						}
						m_featureStat.get(token).addOneTTF(doc.getYLabel());
					}
					// if the token is not in the vocabulary, nothing to do.
				}
		
		}
				
		
		
		if (spVct.size()>=m_lengthThreshold || spVctneg.size()>=m_lengthThreshold) {//temporary code for debugging purpose
			
				if(pos!=null)
				{
					doc.createSpVct(spVct);
					AnalyzeDocWithStnSplitpros(doc, pos);
				}
				
				if(neg!=null)
				{
					doc.createSpVctneg(spVctneg);
					AnalyzeDocWithStnSplitcons(doc, neg);
				}
				

				if(m_corpus.getSize()%11!=0)
				{
					corpus_writer.write("d"+m_corpus.getSize()+" ");
					if(pos!=null)
					{
						for (String token : pos_tokens) {
							corpus_writer.write(token+" ");
						}
					}
					
					if(neg!=null){
						for (String token2 : neg_tokens) {
							corpus_writer.write(token2+" ");
						}
					}
					corpus_writer.write("\n\n");
				}
				
				
				if(m_corpus.getSize()%11==0)
				{
					test_corpus_writer.write("d"+m_corpus.getSize()+" ");
					test_corpus_label_writer.write("d"+m_corpus.getSize()+" ");
					if(pos!=null)
					{
						for (String token : pos_tokens) {
							test_corpus_writer.write(token+" ");
							test_corpus_label_writer.write("1"+" ");
						}
					}
					
					if(neg!=null){
						for (String token2 : neg_tokens) {
							test_corpus_writer.write(token2+" ");
							test_corpus_label_writer.write("2"+" ");
						}
					}
					test_corpus_writer.write("\n");
					test_corpus_label_writer.write("\n");
				}
			
			
				
				
				
				String[] sentences = m_stnDetector.sentDetect(doc.getSource());
				int sentence_number = 0;
				for(String sentence : sentences) {
					boolean issentence = false;
					String[] tokens = TokenizerNormalizeStemmer(sentence);			
					for (String token : tokens) {
						if (m_featureNameIndex.containsKey(token)) {
							issentence = true;
						}
					}
					if(issentence==true)
					{
						sentence_number++;
					}
				}
					
				asum_bag_writer.write(sentence_number+"\n");
				for(String sentence : sentences) {
					boolean issentence = false;
					String[] tokens = TokenizerNormalizeStemmer(sentence);			
					for (String token : tokens) {
						if (m_featureNameIndex.containsKey(token)) {
						index = m_featureNameIndex.get(token);
						asum_bag_writer.write(index+" ");
						issentence = true;
						}
					}
					if(issentence==true)
					{
						asum_bag_writer.write("\n");
					}
				}
				
	
				int number_of_sentence = 0;
				String[] pos_sentences = null;
				String[] neg_sentences = null;
				
				if(pos!=null){
					pos_sentences = m_stnDetector.sentDetect(pos);


				
					for(String sentence : pos_sentences) {
						boolean issentence = false;
						String[] tokens = TokenizerNormalizeStemmer(sentence);			
						for (String token : tokens) {
							if (m_featureNameIndex.containsKey(token)) {
								issentence = true;
							}
						}
						if(issentence==true)
						{
							number_of_sentence++;
						}
					}
				}
				
				if(neg!=null)
				{
					neg_sentences = m_stnDetector.sentDetect(neg);
					for(String sentence : neg_sentences) {
						boolean issentence = false;
						String[] tokens = TokenizerNormalizeStemmer(sentence);			
						for (String token : tokens) {
							if (m_featureNameIndex.containsKey(token)) {
								issentence = true;
							}
						}
						if(issentence==true)
						{
							number_of_sentence++;
						}
					}
				}
				
				asum_bag_writer_pros_cons.write(number_of_sentence+"\n");
				
				
				if(pos!=null){
					for(String sentence : pos_sentences) {
						boolean issentence = false;
						String[] tokens = TokenizerNormalizeStemmer(sentence);

						boolean flag = false;
						for (String token : tokens) 
						{	
							if(!token.equalsIgnoreCase(""))
							{
								flag = true;
								break;
							}
						}

						if(flag==true && tokens.length>=1)
						{
							asum_bag_writer_pros_cons.write(-1+" ");
							if(m_corpus.getSize()%11==0){
								test_corpus_writer_sentence.write("d"+this.sentence_counter+" "); // for JST sentence
								test_corpus_label_writer_sentence.write("d"+this.sentence_counter+" ");
							}
							
						}
						for (String token : tokens) {
							if (m_featureNameIndex.containsKey(token)) {
								index = m_featureNameIndex.get(token);
								asum_bag_writer_pros_cons.write(index+" ");
								if(m_corpus.getSize()%11==0)
									test_corpus_writer_sentence.write(token+" "); // for JST sentence
								issentence = true;
							}
						}
						if(issentence==true)
						{
							this.sentence_counter++;
							asum_bag_writer_pros_cons.write("\n");
							if(m_corpus.getSize()%11==0){
								test_corpus_writer_sentence.write("\n"); // for JST sentence
								test_corpus_label_writer_sentence.write(1+"\n"); //  // for JST sentence actual pos label
							}
						}
					}
				}
				
				if(neg!=null){
					for(String sentence : neg_sentences) {
						boolean issentence = false;
						String[] tokens = TokenizerNormalizeStemmer(sentence);
						boolean flag = false;
						for (String token : tokens) 
						{	
							if(!token.equalsIgnoreCase(""))
							{
								flag = true;
								break;
							}
						}

						if(flag==true && tokens.length>=1)
						{
							asum_bag_writer_pros_cons.write(-2+" ");
							if(m_corpus.getSize()%11==0){
								test_corpus_writer_sentence.write("d"+this.sentence_counter+" "); // for JST sentence
								test_corpus_label_writer_sentence.write("d"+this.sentence_counter+" ");
							}
						}
						for (String token : tokens) {
							if (m_featureNameIndex.containsKey(token)) {
								index = m_featureNameIndex.get(token);
								asum_bag_writer_pros_cons.write(index+" ");
								if(m_corpus.getSize()%11==0)
									test_corpus_writer_sentence.write(token+" "); // for JST sentence
								issentence = true;
							}
						}
						if(issentence==true)
						{
							this.sentence_counter++;
							asum_bag_writer_pros_cons.write("\n");
							if(m_corpus.getSize()%11==0){
								test_corpus_writer_sentence.write("\n"); // for JST sentence
								test_corpus_label_writer_sentence.write(2+"\n"); //  // for JST sentence actual neg label
							}
						}
					}
				}
				
				
				m_corpus.addDoc(doc);
				
				m_classMemberNo[doc.getYLabel()]++;
				
			/*if (m_releaseContent)
				doc.clearSource();*/
			return true;
		} else
			return false;
	}

	
	public void close_printer()
	{
		corpus_writer.flush();
		corpus_writer.close();
		
		test_corpus_writer.flush();
		test_corpus_writer.close();
		
		test_corpus_label_writer.flush();
		test_corpus_label_writer.close();
		
		asum_bag_writer.flush();
		asum_bag_writer.close();
		
		asum_bag_writer_pros_cons.flush();
		asum_bag_writer_pros_cons.close();
		
		test_corpus_label_writer_sentence.flush();
		test_corpus_label_writer_sentence.close();
		
		test_corpus_writer_sentence.flush();
		test_corpus_writer_sentence.close();
	}
	
	@Override
	public void LoadDocNewEgg(String filename, int section) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void LoadDocAmazon(String filename) {
		// TODO Auto-generated method stub
		
	}
}

