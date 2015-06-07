package topicmodels;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import structures._Corpus;
import structures._Doc;
import structures._SparseFeature;
import utils.Utils;
import Analyzer.jsonAnalyzer;

public abstract class TopicModel {
	protected int number_of_topics;
	protected int vocabulary_size;
	protected int number_of_iteration;
	protected double m_converge;
	protected _Corpus m_corpus;	
	int precision_recall [][] = new int[2][2]; // row for true label and col for predicted label 
	int precision_recall_sentences [][] = new int[2][2]; // row for true label and col for predicted label 
	int crossV;
	String model = null;
	
	//for training/testing split
	public ArrayList<_Doc> m_trainSet, m_testSet;
	
	//smoothing parameter for p(w|z, \beta)
	protected double d_beta; 	
	
	boolean m_display; // output EM iterations
	boolean m_collectCorpusStats; // if we will collect corpus-level statistics (for efficiency purpose)
	
	public TopicModel(int number_of_iteration, double converge, double beta, _Corpus c) {
		this.vocabulary_size = c.getFeatureSize();
		this.number_of_iteration = number_of_iteration;
		this.m_converge = converge;
		this.d_beta = beta;
		this.m_corpus = c;
		
		m_display = false; // by default we won't track EM iterations
	}
	
	@Override
	public String toString() {
		return "Topic Model";
	}
	
	public void setDisplay(boolean disp) {
		m_display = disp;
	}
	
	//initialize necessary model parameters
	protected abstract void initialize_probability(Collection<_Doc> collection);	
	
	// to be called per EM-iteration
	protected abstract void init();
	
	// to be call per test document
	protected abstract void initTestDoc(_Doc d);
	
	//estimate posterior distribution of p(\theta|d)
	protected abstract void estThetaInDoc(_Doc d);
	
	// perform inference of topic distribution in the document
	public double inference(_Doc d) {
		initTestDoc(d);//this is not a corpus level estimation
		
		double delta, last = 1, current;
		int  i = 0;
		
		do {
			
			
			current = calculate_E_step(d);
			estThetaInDoc(d);			
			delta = (last - current)/last;
			last = current;
		} while (Math.abs(delta)>m_converge && ++i<this.number_of_iteration);
		return current;
	}
		
	//E-step should be per-document computation
	public abstract double calculate_E_step(_Doc d); // return log-likelihood
	
	//M-step should be per-corpus computation
	public abstract void calculate_M_step();
	
	//compute per-document log-likelihood
	protected abstract double calculate_log_likelihood(_Doc d);
	
	//print top k words under each topic
	public abstract void printTopWords(int k);
	
	// compute corpus level log-likelihood
	protected double calculate_log_likelihood() {
		return 0;
	}
	
	public void EMonCorpus() {
		m_trainSet = m_corpus.getCollection();
		EM();
	}

	public void EM() {	
		m_collectCorpusStats = true;
		initialize_probability(m_trainSet);
		
		double delta, last = calculate_log_likelihood(), current;
		int  i = 0;
		
		String topic = "pLSAattr1";
		//String topic = "pLSA";
		
		String likelihood_location = "./data/"+topic+"likelihood"+this.number_of_topics+".txt";
		String perplexity_location = "./data/"+topic+"perplexity"+this.number_of_topics+".txt";
		
		PrintWriter likelihood_writer=null,perplexity_writer=null;
		try {
			likelihood_writer = new PrintWriter(new File(likelihood_location));
			perplexity_writer = new PrintWriter(new File(perplexity_location));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		do
		{
			init();
			
			current = 0;
			for(_Doc d:m_trainSet)
				current += calculate_E_step(d);
			
			calculate_M_step();
			
			current += calculate_log_likelihood();//together with corpus-level log-likelihood
			delta = (last-current)/last;
			last = current;
			
			
			
			if(this.crossV>1 && topic.equalsIgnoreCase("pLSA_bipartite"))
			{
				
			}
			else{
			
			// following part should be open if croosV >1 and else close 
			

				double perplex =  Evaluation();
				perplexity_writer.println(perplex);
				System.out.format("Perplexity at iteration %d is %.3f\n", i, perplex);
				likelihood_writer.println(current);
				System.out.format("Likelihood %.3f at step %s converge to %f...\n", current, i, delta);
				i++;
				m_collectCorpusStats = true; // setting true since the Evaluation in previous step is setting it as false

			}
			
			
			
			
		} while (Math.abs(delta)>this.m_converge && i<this.number_of_iteration);
		
		if (!m_display) // output the summary
			System.out.format("Likelihood %.3f after step %s converge to %f...\n", current, i, delta);
		likelihood_writer.close();
		perplexity_writer.close();
	
	}

	public void calculate_precision_recall_for_bipartite(pLSA_attr_bipartite model_cons)
	{
		
		double pros_loglikelihood, cons_loglikelihood;
		double perplexity = 0, loglikelihood, log2 = Math.log(2.0), sumLikelihood = 0;
		precision_recall[0][0] = 0; // 0 is for neg
		precision_recall[0][1] = 0; // 1 is pos 
		precision_recall[1][0] = 0;
		precision_recall[1][1] = 0;
		
		for(_Doc d:m_testSet) {
			
			if(d.getAllSentences()!=null){
				_SparseFeature[][] sentences =  d.getAllSentences();

				for(_SparseFeature[] sentence:sentences)
				{

					d.m_x_sparse = sentence; // making a pseduo doc for each sentence in pros section
					pros_loglikelihood = inference(d);
					cons_loglikelihood = model_cons.inference(d);

					if(pros_loglikelihood>cons_loglikelihood)
					{
						precision_recall[1][1]++; // really doc is pros and prediction is  pros so 11
						sumLikelihood += pros_loglikelihood;
						perplexity += Math.pow(2.0, -pros_loglikelihood/d.getTotalDocLength() / log2);
					}
					else
					{
						precision_recall[1][0]++; // really doc is pros and prediction is  cons so 10
						sumLikelihood += cons_loglikelihood;
						perplexity += Math.pow(2.0, -cons_loglikelihood/d.getTotalDocLength() / log2);
					}
				}
			}
		}
		
		
		for(_Doc d:model_cons.m_testSet) {
			
			if(d.getAllSentencesneg()!=null){
				_SparseFeature[][] sentences =  d.getAllSentencesneg();

				for(_SparseFeature[] sentence:sentences)
				{

					d.m_x_sparse_neg = sentence; // making a pseduo doc for each sentence in cons section

					pros_loglikelihood = inference(d);
					cons_loglikelihood = model_cons.inference(d);

					if(cons_loglikelihood>pros_loglikelihood)
					{
						precision_recall[0][0]++; // really doc is cons and prediction is  cons so 00
						sumLikelihood += cons_loglikelihood;
						perplexity += Math.pow(2.0, -cons_loglikelihood/d.getTotalDocLength() / log2);
					}
					else
					{
						precision_recall[0][1]++; // really doc is pros and prediction is  pros so 01
						sumLikelihood += pros_loglikelihood;
						perplexity += Math.pow(2.0, -pros_loglikelihood/d.getTotalDocLength() / log2);
					}
				}
			}
		}
		
		
		perplexity /= (m_testSet.size() + model_cons.m_testSet.size());
		sumLikelihood /= (m_testSet.size() + model_cons.m_testSet.size());
		
		System.out.println("Perplexity:"+ perplexity);
		System.out.println("LogLikelihood:"+ sumLikelihood);
		
		
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
		
	}
	
	public double Evaluation() {
		m_collectCorpusStats = false;
		
		// row for real label, col is for predicted label
		precision_recall[0][0] = 0; // 0 is for neg
		precision_recall[0][1] = 0; // 1 is pos 
		precision_recall[1][0] = 0;
		precision_recall[1][1] = 0;
		
		precision_recall_sentences[0][0] = 0; // 0 is for neg
		precision_recall_sentences[0][1] = 0; // 1 is pos 
		precision_recall_sentences[1][0] = 0;
		precision_recall_sentences[1][1] = 0;
		
		double perplexity = 0, loglikelihood, log2 = Math.log(2.0), sumLikelihood = 0;
		for(_Doc d:m_testSet) {
			
			if(d.from == 1){
			_SparseFeature[] pos = d.getSparse();
			_SparseFeature[] neg = d.getSparseneg();
			
			_SparseFeature[][] pos_sentences = d.getAllSentences();
			_SparseFeature[][] neg_sentences = d.getAllSentencesneg();
			
			d.m_x_sparse = neg;
			d.m_sentences = neg_sentences;
			
			d.test = 1;
			loglikelihood = inference(d);
			
			// calculate the predicted label 
			
			for(_SparseFeature fv:d.getSparse()){
				
			int label=-1;
			
			if(fv.topic>=0 && fv.topic<d.m_topics.length/2){
				label = 1; // pros
			}
			else if(fv.topic>=d.m_topics.length/2 && fv.topic<d.m_topics.length)
				label = 0; // cons
			
			precision_recall[0][label]++;  // 0 for neg
			}
			
		
			sumLikelihood += loglikelihood;
			perplexity += Math.pow(2.0, -loglikelihood/d.getTotalDocLength() / log2);
			
			
			if(d.getAllSentences()!=null){
				for(int i=0; i<d.m_sentence_topic.length;i++)
				{
					int label = -1;
					if(d.m_sentence_topic[i]>=0 && d.m_sentence_topic[i]<d.m_topics.length/2){
						label = 1; // pros
					}
					else if(d.m_sentence_topic[i]>=d.m_topics.length/2 && d.m_sentence_topic[i]<d.m_topics.length)
						label = 0; // cons

					precision_recall_sentences[0][label]++;  // 0 for neg
				}
			}
			
			
			
			d.m_x_sparse = pos;
			d.m_sentences = pos_sentences;
			d.test = 1;
			
			loglikelihood = inference(d);
			
			for(_SparseFeature fv:d.getSparse()){
				
				int label=-1;
				
				if(fv.topic>=0 && fv.topic<d.m_topics.length/2){
					label = 1; // pros
				}
				else if(fv.topic>=d.m_topics.length/2 && fv.topic<d.m_topics.length)
					label = 0; // cons
				
				precision_recall[1][label]++; // for pos
			}
			
			sumLikelihood += loglikelihood;
			perplexity += Math.pow(2.0, -loglikelihood/d.getTotalDocLength() / log2);
			
			if(d.getAllSentences()!=null){
				for(int i=0; i<d.m_sentence_topic.length;i++)
				{
					int label = -1;
					if(d.m_sentence_topic[i]>=0 && d.m_sentence_topic[i]<d.m_topics.length/2){
						label = 1; // pros
					}
					else if(d.m_sentence_topic[i]>=d.m_topics.length/2 && d.m_sentence_topic[i]<d.m_topics.length)
						label = 0; // cons

					precision_recall_sentences[1][label]++;  // 1 for pos
				}
			}
			
			///////////////////////////////////////////////Sentences
			
			
			
			} // if 
			else
			{
				loglikelihood = inference(d);
				sumLikelihood += loglikelihood;
				perplexity += Math.pow(2.0, -loglikelihood/d.getTotalDocLength() / log2);
			}
		}
		perplexity /= m_testSet.size();
		sumLikelihood /= m_testSet.size();
		
		
		double cons_precision = (double)precision_recall[0][0]/(precision_recall[0][0] + precision_recall[1][0]);
		double pros_precision = (double)precision_recall[1][1]/(precision_recall[0][1] + precision_recall[1][1]);
		
		
		double cons_recall = (double)precision_recall[0][0]/(precision_recall[0][0] + precision_recall[0][1]);
		double pros_recall = (double)precision_recall[1][1]/(precision_recall[1][0] + precision_recall[1][1]);
		
		System.out.println("pros_precision:"+pros_precision+" pros_recall:"+pros_recall);
		System.out.println("cons_precision:"+cons_precision+" cons_recall:"+cons_recall);
		
		
		
		double cons_precision_sentence = (double)precision_recall_sentences[0][0]/(precision_recall_sentences[0][0] + precision_recall_sentences[1][0]);
		double pros_precision_sentence = (double)precision_recall_sentences[1][1]/(precision_recall_sentences[0][1] + precision_recall_sentences[1][1]);
		
		
		double cons_recall_sentence = (double)precision_recall_sentences[0][0]/(precision_recall_sentences[0][0] + precision_recall_sentences[0][1]);
		double pros_recall_sentence = (double)precision_recall_sentences[1][1]/(precision_recall_sentences[1][0] + precision_recall_sentences[1][1]);
		
		System.out.println("pros_precision_sentence:"+pros_precision_sentence+" pros_recall_sentence:"+pros_recall_sentence);
		System.out.println("cons_precision_sentence:"+cons_precision_sentence+" cons_recall_sentence:"+cons_recall_sentence);
		
		
		double pros_f1 = 2/(1/pros_precision + 1/pros_recall);
		double cons_f1 = 2/(1/cons_precision + 1/cons_recall);
		
		System.out.println("Word level F1 measure:pros:"+pros_f1+", cons:"+cons_f1);
		
		
		pros_f1 = 2/(1/pros_precision_sentence + 1/pros_recall_sentence);
		cons_f1 = 2/(1/cons_precision_sentence + 1/cons_recall_sentence);
		
		System.out.println("Sentence level F1 measure:pros:"+pros_f1+", cons:"+cons_f1);
		
		System.out.println("Word Level");
		for(int i=0; i<2; i++)
		{
			for(int j=0; j<2; j++)
			{
				System.out.print(precision_recall[i][j]+",");
			}
			System.out.println();
		}
		
		System.out.println("Sentence Level");
		for(int i=0; i<2; i++)
		{
			for(int j=0; j<2; j++)
			{
				System.out.print(precision_recall_sentences[i][j]+",");
			}
			System.out.println();
		}
		
		return perplexity;
	}
	
	
	
	
/*	public double Evaluation() {
		m_collectCorpusStats = false;
		
		// row for real label, col is for predicted label
		precision_recall[0][0] = 0; // 0 is for neg
		precision_recall[0][1] = 0; // 1 is pos 
		precision_recall[1][0] = 0;
		precision_recall[1][1] = 0;
		
		double perplexity = 0, loglikelihood, log2 = Math.log(2.0), sumLikelihood = 0;
		for(_Doc d:m_testSet) {
			
			if(d.from == 1){
			_SparseFeature[] pos = d.getSparse();
			_SparseFeature[] neg = d.getSparseneg();
			
			d.m_x_sparse = neg;
			d.test = 1;
			
			loglikelihood = inference(d);
			
			// calculate the predicted label 
			
			double pros_topic_sum = 1.0, cons_topic_sum=1.0;
			for(int z=0, k=d.m_topics.length/2; z<d.m_topics.length/2; z++,k++)
			{
				pros_topic_sum = pros_topic_sum * d.m_topics[z];
				cons_topic_sum = cons_topic_sum * d.m_topics[k];
			}
			
			if(pros_topic_sum<cons_topic_sum)
			{
				d.m_predict_label = 0;
			}else
			{
				d.m_predict_label = 1;
			}
			
			precision_recall[0][d.m_predict_label]++;  // 0 for neg
			
			sumLikelihood += loglikelihood;
			perplexity += Math.pow(2.0, -loglikelihood/d.getTotalDocLength() / log2);
			
			
			
			
			d.m_x_sparse = pos;
			d.test = 1;
			
			loglikelihood = inference(d);
			
			// calculate the predicted label 
			
			pros_topic_sum = 1.0;
			cons_topic_sum=1.0;
			for(int z=0, k=d.m_topics.length/2; z<d.m_topics.length/2; z++,k++)
			{
				pros_topic_sum = pros_topic_sum * d.m_topics[z];
				cons_topic_sum = cons_topic_sum * d.m_topics[k];
			}
			
			if(pros_topic_sum<cons_topic_sum)
			{
				d.m_predict_label = 0;
			}else
			{
				d.m_predict_label = 1;
			}
			
			precision_recall[1][d.m_predict_label]++; // for pos
			
			
			sumLikelihood += loglikelihood;
			perplexity += Math.pow(2.0, -loglikelihood/d.getTotalDocLength() / log2);
			}
			
			
			else
			{
				loglikelihood = inference(d);
				sumLikelihood += loglikelihood;
				perplexity += Math.pow(2.0, -loglikelihood/d.getTotalDocLength() / log2);
			}
		}
		perplexity /= m_testSet.size();
		sumLikelihood /= m_testSet.size();
		
		
		double cons_precision = (double)precision_recall[0][0]/(precision_recall[0][0] + precision_recall[1][0]);
		double pros_precision = (double)precision_recall[1][1]/(precision_recall[0][1] + precision_recall[1][1]);
		
		
		double cons_recall = (double)precision_recall[0][0]/(precision_recall[0][0] + precision_recall[0][1]);
		double pros_recall = (double)precision_recall[1][1]/(precision_recall[1][0] + precision_recall[1][1]);
		
		System.out.println("pros_precision:"+pros_precision+" pros_recall:"+pros_recall);
		System.out.println("cons_precision:"+cons_precision+" cons_recall:"+cons_recall);
		
		
		for(int i=0; i<2; i++)
		{
			for(int j=0; j<2; j++)
			{
				System.out.print(precision_recall[i][j]+",");
			}
			System.out.println();
		}
		
		return perplexity;
	}
	*/
	//k-fold Cross Validation.
	public void crossValidation(int k) {
		//m_corpus.shuffle(k);
		this.crossV = k;
		//int[] masks = m_corpus.getMasks();
		ArrayList<_Doc> docs = m_corpus.getCollection();
		
		m_trainSet = new ArrayList<_Doc>();
		m_testSet = new ArrayList<_Doc>();
		double[] perf = new double[k];
		double corpous_size = docs.size();
		
	/*	int train_finish_index = (int)Math.ceil(0.90*corpous_size);
		int test_finish_index = (int)corpous_size;
		
		for(int i=0; i<train_finish_index; i++)
		{
			m_trainSet.add(docs.get(i));
		}
		for(int i=train_finish_index; i<test_finish_index; i++)
		{
			m_testSet.add(docs.get(i));
		}
		*/
		
		int mod = 11;
		for(int i=0; i<m_corpus.getSize(); i++)
		{
			if(i%mod!=0)
				m_trainSet.add(docs.get(i));
			else
				m_testSet.add(docs.get(i));
		}
		
		long start = System.currentTimeMillis();
		System.out.println("Train Set size:"+m_trainSet.size()+", Test Size:"+m_testSet.size());
		EM();
	
		System.out.format("%s Train/Test finished in %.2f seconds...\n", this.toString(), (System.currentTimeMillis()-start)/1000.0);
	
	}
	
	public static void main(String[] args) throws IOException, ParseException {	
		int classNumber = 5; //Define the number of classes in this Naive Bayes.
		int Ngram = 1; //The default value is unigram. 
		String featureValue = "TF"; //The way of calculating the feature value, which can also be "TFIDF", "BM25"
		int norm = 0;//The way of normalization.(only 1 and 2)
		int lengthThreshold = 5; //Document length threshold
		
		/*****parameters for the two-topic topic model*****/
		//String topicmodel = "pLSA_bipartite"; // 2topic, pLSA, pLSAattr, HTMM, LRHTMM, Tensor
		String topicmodel = "pLSAattr1"; // 2topic, pLSA, pLSAattr, HTMM, LRHTMM, Tensor
		
		
		int number_of_topics = 10;
		double alpha = 1.0 + 1e-2, beta = 1.0 + 1e-3;//these two parameters must be larger than 1!!!
		double converge = 1e-4, lambda = 0.7;
		int topK = 10, number_of_iteration = 100, crossV = 2;
		
		/*****The parameters used in loading files.*****/
		//String folder = "./data/amazon/test";
		String folder = "./data/amazon/test/";
		String neweggfolder = "./data/amazon/test/";
		String amazonfolder = "./data/amazon/camera/";
		String suffix = ".json";
		String tokenModel = "./data/Model/en-token.bin"; //Token model.
		String stnModel = null;
		if (topicmodel.equals("HTMM") || topicmodel.equals("LRHTMM"))
			stnModel = "./data/Model/en-sent.bin"; //Sentence model.
		
		String featureLocation = "./data/Features/selected_fv_topicmodel.txt";
		String finalLocation = "./data/Features/selected_fv_stat_topicmodel.txt";
		
		String finalLocation2 = "./data/Features/selected_fv_stat_topicmodel2.txt";
		
		String combinefeatureLocation = "./data/Features/selected_combine_fv.txt";

		//String combinefeatureLocation = "./data/Features/newegg_camera_selected_fv.txt";

		/*****Parameters in feature selection.*****/
		String stopwords = "./data/Model/stopwords.dat";
/*		String featureSelection = "CHI"; //Feature selection method.
		double startProb = 0.2; // Used in feature selection, the starting point of the features.
		double endProb = 0.999; // Used in feature selection, the ending point of the features.
		int DFthreshold = 10; // Filter the features with DFs smaller than this threshold.
		
		System.out.println("Performing feature selection, wait...");
		jsonAnalyzer analyzer = new jsonAnalyzer(tokenModel, classNumber, "", Ngram, lengthThreshold);
		analyzer.LoadStopwords(stopwords);
		analyzer.LoadDirectory(folder, suffix); //Load all the documents as the data set.
		analyzer.featureSelection(featureLocation, featureSelection, startProb, endProb, DFthreshold); //Select the features.
*/
		
		
		System.out.println("Creating feature vectors, wait...");
		
		jsonAnalyzer analyzer=null,analyzer2=null;
		_Corpus c=null, c2=null;
		
		if(topicmodel.equalsIgnoreCase("pLSA_bipartite")){
			analyzer = new jsonAnalyzer(tokenModel, classNumber, combinefeatureLocation, Ngram, lengthThreshold, stnModel);
			analyzer.LoadStopwords(stopwords);
			// following LoadDirectory(String folder, String suffix, int from, int section)
			// here from = 1 means newegg and from = 2 amazon , and section=1 means only pros, section = 2  means only cons, section = 3 means both pros and cons
			analyzer.LoadDirectory(neweggfolder, suffix, 1, 1); //Load all the documents as the data set.
			analyzer.close_printer();
			c = analyzer.returnCorpus(finalLocation); // Get the collection of all the documents.

			// for bipartite matching
			// all these following for loading all the cons section of newegg for bipartite matching
			analyzer2 = new jsonAnalyzer(tokenModel, classNumber, combinefeatureLocation, Ngram, lengthThreshold, stnModel);
			analyzer2.LoadStopwords(stopwords);
			analyzer2.LoadDirectory(neweggfolder, suffix, 1, 2); //Load all the documents as the data set.
			c2 = analyzer2.returnCorpus(finalLocation2); // Get the collection of all the documents.
			System.out.println("Corpus Size"+c.getSize());
			System.out.println("Corpus2 Size"+c2.getSize());
		
		}
		
		else
		{
			analyzer = new jsonAnalyzer(tokenModel, classNumber, combinefeatureLocation, Ngram, lengthThreshold, stnModel);
			analyzer.LoadStopwords(stopwords);
			// following LoadDirectory(String folder, String suffix, int from, int section)
			// here from = 1 means newegg and from = 2 amazon , and section=1 means only pros, section = 2  means only cons, section = 3 means both pros and cons
			analyzer.LoadDirectory(neweggfolder, suffix, 1, 3); //Load all the documents as the data set.
			analyzer.close_printer();
			c = analyzer.returnCorpus(finalLocation); // Get the collection of all the documents.
			System.out.println("Corpus Size"+c.getSize());
		}
		
		
		
		if (topicmodel.equals("2topic")) {
			twoTopic model = new twoTopic(number_of_iteration, converge, beta, c, lambda, analyzer.getBackgroundProb());
			
			if (crossV<=1) {
				for(_Doc d:c.getCollection()) {
					model.inference(d);
					model.printTopWords(topK);
				}
			} else 
				model.crossValidation(crossV);
		} else if (topicmodel.equals("pLSA")) {			
//			pLSA model = new pLSA(number_of_iteration, converge, beta, c, 
//					lambda, analyzer.getBackgroundProb(), 
//					number_of_topics, alpha);
			pLSA model = new pLSAGroup(number_of_iteration, converge, beta, c, 
					lambda, analyzer.getBackgroundProb(), 
					number_of_topics, alpha);
			
			if (crossV<=1) {
				model.EMonCorpus();
				model.printTopWords(topK);
			} else
			{
				System.out.println("Here PLSANOrmal");
				model.crossValidation(crossV);
				model.printTopWords(topK);
				model.printInDoc(topicmodel);
			}
				
		} else if (topicmodel.equals("pLSAattr")) {			
			//			pLSA model = new pLSA(number_of_iteration, converge, beta, c, 
			//			lambda, analyzer.getBackgroundProb(), 
			//			number_of_topics, alpha);
			pLSA_attr model = new pLSA_attr(number_of_iteration, converge, beta, c, 
					lambda, analyzer.getBackgroundProb(), 
					number_of_topics*2, alpha); // k*2 because for pros and cons 

			if (crossV<=1) {
				model.EMonCorpus();
				model.printTopWords(topK);
				model.printInDoc(topicmodel);
		
			} else{
				model.crossValidation(crossV);
				model.printTopWords(topK);
				model.printInDoc(topicmodel);
			}
		}
		else if (topicmodel.equals("pLSAattr1")) {			
			pLSA_attr1 model = new pLSA_attr1(number_of_iteration, converge, beta, c, 
					lambda, analyzer.getBackgroundProb(), 
					number_of_topics*2, alpha); // k*2 because for pros and cons 
			if (crossV<=1) {
				model.EMonCorpus();
				model.printTopWords(topK);
				model.printInDoc(topicmodel);
			
			} else{
				model.crossValidation(crossV);
				model.printTopWords(topK);
				model.document_summary(5);
			}
		}
		
		else if (topicmodel.equals("pLSA_bipartite")) {			
			pLSA_attr_bipartite model = new pLSA_attr_bipartite(number_of_iteration, converge, beta, c, 
					lambda, analyzer.getBackgroundProb(), 
					number_of_topics, alpha); 

			
			pLSA_attr_bipartite model2 = new pLSA_attr_bipartite(number_of_iteration, converge, beta, c2, 
					lambda, analyzer2.getBackgroundProb(), 
					number_of_topics, alpha);
			
			
			if (crossV<=1) {
				model.EMonCorpus();
				model.printTopWords(topK);
				//model.printInDoc(topicmodel);
				
				// for birpartite matching
				model2.EMonCorpus();
				model2.printTopWords(topK);
				//model2.printInDoc(topicmodel);
				
				
				
				double topics[][] = model2.getTopics();
				model.maxbipartitematching(topics);
		
			} else{
				model.crossValidation(crossV);
				model.printTopWords(topK);
				
				model2.crossValidation(crossV);
				model2.printTopWords(topK);
				
				// for birpartite matching
				double topics[][] = model2.getTopics();
				int mapping[] = model.maxbipartitematching(topics);
				
				
				int topic_number = 5;
				System.out.println("Document Summary for Pros:");
				model.document_summary(topic_number); // pros topics 
				
				System.out.println("Document Summary for Cons:");
				model2.document_summary(mapping[topic_number]); // corresponding cons topic using the mapping
				//model.printInDoc(topicmodel);
				
				// must be called after document summary
				model.calculate_precision_recall_for_bipartite(model2);
			}
		}
		else if (topicmodel.equals("HTMM")) {
			HTMM model = new HTMM(number_of_iteration, converge, beta, c, 
					number_of_topics, alpha);
			
			if (crossV<=1) {
				model.EMonCorpus();
				model.printTopWords(topK);
			} else 
				model.crossValidation(crossV);
		} else if (topicmodel.equals("LRHTMM")) {
			c.setStnFeatures();
			
			LRHTMM model = new LRHTMM(number_of_iteration, converge, beta, c, 
					number_of_topics, alpha,
					lambda);
			
			if (crossV<=1) {
				model.EMonCorpus();
				model.printTopWords(topK);
			} else 
				model.crossValidation(crossV);
		} else if (topicmodel.equals("Tensor")) {
			c.saveAs3WayTensor("./data/vectors/3way_tensor.txt");
		}
		
	}
}
