package topicmodels;


/**
 * @author Md. Mustafizur Rahman (mr4xb@virginia.edu)
 * Probabilistic Latent Semantic Analysis Topic Modeling 
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;

import optimization.gradientBasedMethods.ProjectedGradientDescent;
import optimization.gradientBasedMethods.stats.OptimizerStats;
import optimization.linesearch.ArmijoLineSearchMinimizationAlongProjectionArc;
import optimization.linesearch.InterpolationPickFirstStep;
import optimization.linesearch.LineSearchMethod;
import optimization.stopCriteria.CompositeStopingCriteria;
import optimization.stopCriteria.ProjectedGradientL2Norm;
import optimization.stopCriteria.StopingCriteria;
import prapplication.problem_coin;
import prapplication.problem_prattrb;
import structures.MyPriorityQueue;
import structures._Corpus;
import structures._Doc;
import structures._RankItem;
import structures._SparseFeature;
import utils.Utils;


public class pLSA_attr_bipartite extends twoTopic {
	// Dirichlet prior for p(\theta|d)
	double d_alpha; // smoothing of p(z|d)
	int optimizer_success_count = 0;
	int new_egg_doc_counter = 0;
	
	double[][] topic_term_probabilty ; /* p(w|z) */
	double[][] topic_attribute_probabilty ; /* p(a|z) */
	
	double[][] word_topic_sstat; /* fractional count for p(z|d,w) */
	double[][] attribute_topic_sstat; /* fractional count for attribute and topic */
	
	public pLSA_attr_bipartite(int number_of_iteration, double converge, double beta, _Corpus c, //arguments for general topic model
			double lambda, double back_ground [], //arguments for 2topic topic model
			int number_of_topics, double alpha) { //arguments for pLSA			
		super(number_of_iteration, converge, beta, c, lambda, back_ground);
		
		this.d_alpha = alpha;
		this.number_of_topics = number_of_topics;
		topic_term_probabilty = new double[this.number_of_topics][this.vocabulary_size];
		topic_attribute_probabilty = new double[this.number_of_topics][2];
		
		word_topic_sstat = new double[this.number_of_topics][this.vocabulary_size];
		attribute_topic_sstat = new double[this.number_of_topics][2]; // since 2 attributes
	}
	
	@Override
	public String toString() {
		return String.format("pLSA[k:%d, lambda:%.2f]", number_of_topics, m_lambda);
	}

	@Override
	protected void initialize_probability(Collection<_Doc> collection) {	
		// initialize topic document proportion, p(z|d)
		for(_Doc d:collection)
			d.setTopics(number_of_topics, d_alpha-1.0);//allocate memory and randomize it
		
		// initialize term topic matrix p(w|z,\phi)
		for(int i=0;i<number_of_topics;i++)
			Utils.randomize(this.topic_term_probabilty[i], d_beta-1.0);
	}
	
	@Override
	protected void init() { // clear up for next iteration
		for(int k=0;k<this.number_of_topics;k++)
			Arrays.fill(word_topic_sstat[k], d_beta-1.0);//pseudo counts for p(w|z)
		
		//initiate sufficient statistics
		for(_Doc d:m_trainSet)
			Arrays.fill(d.m_sstat, 0);//pseudo counts for p(\theta|d)
		
		for(int k=0;k<this.number_of_topics;k++)
		{
			Arrays.fill(attribute_topic_sstat[k], 0.0);//pseudo counts for p(a|z)
		}
		
		for(_Doc d:m_trainSet)
		{
			for(int k=0;k<this.number_of_topics;k++)
			{
				Arrays.fill(d.m_topic_attribute_sstat[k], 0.0);
			}
		}
		
		this.optimizer_success_count = 0;
		this.new_egg_doc_counter = 0;
		
	}
	
	@Override
	protected void initTestDoc(_Doc d) {
		//allocate memory and randomize it
		d.setTopics(number_of_topics, d_alpha-1.0);//in real space
	}
	
	public double[][] getTopics()
	{
		return this.topic_term_probabilty;
	}
	
	
	public void document_summary(int topic)
	{
		
		int index = -1;
		for(_Doc d:m_trainSet){
		
			if(d.getID()%100 == 0){
				
				if(d.from == 1)
				{
					
					if(d.getAllSentences()!=null)
					{
						System.out.println("Doc Id:"+d.getID()+" from:"+"newegg_pros");
						_SparseFeature[][] pos_sentence = d.getAllSentences();
					
					
						double max = 0.0;
						for(int s=0; s<pos_sentence.length; s++)
						{
							double prod = 1.0;
							for(int w=0; w<pos_sentence[s].length;w++)
							{
								int j = pos_sentence[s][w].getIndex();
								prod = prod * topic_term_probabilty[topic][j]*d.m_topics[topic];
							}

							if(prod>max)
							{
								max = prod;
								index = s;
							}
						}
						
						
						for(int w=0; w<pos_sentence[index].length;w++)
						{
							int j = pos_sentence[index][w].getIndex(); 
							System.out.print(m_corpus.getFeature(j)+" ");
						}
						
						System.out.println();
					}
					
					if(d.getAllSentencesneg()!=null)
					{
						System.out.println("Doc Id:"+d.getID()+" from:"+"newegg_cons");
						_SparseFeature[][] neg_sentence = d.getAllSentencesneg();
						
						
						double max = 0.0;
						for(int s=0; s<neg_sentence.length; s++)
						{
							double prod = 1.0;
							for(int w=0; w<neg_sentence[s].length;w++)
							{
								int j = neg_sentence[s][w].getIndex();
								prod = prod * topic_term_probabilty[topic][j]*d.m_topics[topic];
							}

							if(prod>max)
							{
								max = prod;
								index = s;
							}
						}
						
						System.out.println("Index ="+ index + ", Lenght = "+neg_sentence[index].length);
						
						for(int w=0; w<neg_sentence[index].length;w++)
						{
							int j = neg_sentence[index][w].getIndex(); 
							System.out.print(m_corpus.getFeature(j)+" ");
						}
						System.out.println();
					}
					
				}
				
				if(d.from==2)
				{
					
					
					if(d.getAllSentences()!=null)
					{
						System.out.println("Doc Id:"+d.getID()+" from:"+"Amazon");
						_SparseFeature[][] pos_sentence = d.getAllSentences();
					
					
						double max = 0.0;
						for(int s=0; s<pos_sentence.length; s++)
						{
							double prod = 1.0;
							for(int w=0; w<pos_sentence[s].length;w++)
							{
								int j = pos_sentence[s][w].getIndex();
								prod = prod * topic_term_probabilty[topic][j]*d.m_topics[topic];
							}

							if(prod>max)
							{
								max = prod;
								index = s;
							}
						}
						
						
						for(int w=0; w<pos_sentence[index].length;w++)
						{
							int j = pos_sentence[index][w].getIndex(); 
							System.out.print(m_corpus.getFeature(j)+" ");
						}
						System.out.println();
						
						
					}
					
				}
			}
		}
	}
	
	
	
	@Override
	public double calculate_E_step(_Doc d) {	
		double propB; // background proportion
		double exp = 0; // expectation of each term under topic assignment
		double poscount = 0.0;
		double negcount = 0.0;

		
		double exp_pos = 0; // expectation of positive term under topic assignment
		double exp_neg = 0;  // expectation of negative term under topic assignment
		
		_SparseFeature[] fvarray = null;
		
		int pos_neg_flag = -1; // 2 means neg and 1 means pos
		if(d.getSparse()!=null)
		{
			fvarray = d.getSparse();
			pos_neg_flag = 1;
			
		}
		else if(d.getSparseneg()!=null)
		{
			fvarray = d.getSparseneg();
			pos_neg_flag = 2; 
		}
		
		for(_SparseFeature fv:fvarray) {
			int j = fv.getIndex(); // jth word in doc
			double v = fv.getValue();
			

			if(d.from == 1) // that means doc from new Egg so collect pos and neg count
			{
				poscount = fv.poscount;
				negcount = fv.negcount;
			}
			
			
			//-----------------compute posterior----------- 
			double sum = 0;
			for(int k=0;k<this.number_of_topics;k++)
				sum += d.m_topics[k]*topic_term_probabilty[k][j];//shall we compute it in log space?
			
			propB = m_lambda * background_probability[j];
			propB /= propB + (1-m_lambda) * sum;//posterior of background probability
			
			//-----------------compute and accumulate expectations----------- 
			for(int k=0;k<this.number_of_topics;k++) {
				if(pos_neg_flag == 1)
					exp = poscount * (1-propB)*d.m_topics[k]*topic_term_probabilty[k][j]/sum;
				else if(pos_neg_flag == 2)
					exp = negcount * (1-propB)*d.m_topics[k]*topic_term_probabilty[k][j]/sum;
				
				d.m_sstat[k] += exp;
				
				if(d.from == 1) // that means doc from new Egg so collect pos and neg count
				{
				
					exp_pos = poscount * (1-propB)*d.m_topics[k]*topic_term_probabilty[k][j]/sum;
					exp_neg = negcount * (1-propB)*d.m_topics[k]*topic_term_probabilty[k][j]/sum;
				
					
					d.m_topic_attribute_sstat[k][0] += exp_pos;
					d.m_topic_attribute_sstat[k][1] += exp_neg;
			    }
				
				
				if (m_collectCorpusStats)
				{
					
					word_topic_sstat[k][j] += exp;
					
					if(d.from == 1) // that means doc from new Egg so collect pos and neg count
					{
						attribute_topic_sstat[k][0] += exp_pos;
						attribute_topic_sstat[k][1] += exp_neg;
					}
				}
			}
		}
		
		// p(z | d) 
		double sum_topics = Utils.sumOfArray(d.m_sstat);
		for(int k=0;k<this.number_of_topics;k++)
			d.m_topics[k] = d.m_sstat[k] / sum_topics;	
		
		return calculate_log_likelihood(d);
	}
	
	@Override
	public void calculate_M_step() {
		
		
		// update topic-term matrix -------------
		double sum = 0;
		for(int k=0;k<this.number_of_topics;k++) {
			sum = Utils.sumOfArray(word_topic_sstat[k]);
			for(int i=0;i<this.vocabulary_size;i++)
				topic_term_probabilty[k][i] = word_topic_sstat[k][i] / sum;
		}
		
		
		// update topic-attribute matrix -------------
	    sum = 0;
		for(int k=0;k<this.number_of_topics;k++) {
			sum = Utils.sumOfArray(attribute_topic_sstat[k]);
			for(int i=0;i<2;i++) // 2 number of attributes
				topic_attribute_probabilty[k][i] = attribute_topic_sstat[k][i] / sum;
		}
	
		
		// update per-document topic distribution vectors
		/*for(_Doc d:m_trainSet)
			estThetaInDoc(d);*/
	}
	
	@Override
	protected void estThetaInDoc(_Doc d) {
		double sum = Utils.sumOfArray(d.m_sstat);
		for(int k=0;k<this.number_of_topics;k++)
			d.m_topics[k] = d.m_sstat[k] / sum;
	}
	
	/*likelihod calculation */
	/* M is number of doc
	 * N is number of word in corpus
	 */
	/* p(w,d) = sum_1_M sum_1_N count(d_i, w_j) * log[ lambda*p(w|theta_B) + [lambda * sum_1_k (p(w|z) * p(z|d)) */ 
	//NOTE: cannot be used for unseen documents!
	@Override
	public double calculate_log_likelihood(_Doc d) {
		double logLikelihood = 0.0, prob;
		_SparseFeature[] fvarray = null;
		if(d.getSparse()!=null)
		{
			fvarray = d.getSparse();
		}
		else if(d.getSparseneg()!=null)
		{
			fvarray = d.getSparseneg();
		}
		
		
		for(_SparseFeature fv:fvarray) {
			int j = fv.getIndex();	
			prob = 0.0;
			for(int k=0;k<this.number_of_topics;k++)//\sum_z p(w|z,\theta)p(z|d)
				prob += d.m_topics[k]*topic_term_probabilty[k][j];
			prob = prob*(1-m_lambda) + this.background_probability[j]*m_lambda;//(1-\lambda)p(w|d) * \lambda p(w|theta_b)
			logLikelihood += fv.getValue() * Math.log(prob);
		}
		return logLikelihood;
	}
	
	@Override
	protected double calculate_log_likelihood() {
		//prior from Dirichlet distributions
		double logLikelihood = 0;
		for(int i=0; i<this.number_of_topics; i++) {
			for(int v=0; v<this.vocabulary_size; v++) {
				logLikelihood += (d_beta-1)*topic_term_probabilty[i][v];
			}
		}
		
		return logLikelihood;
	}
	
	
	public void printTopicAttribute() {
		System.out.println("Global Attribute Set");
		for(int i=0; i<topic_attribute_probabilty.length; i++) {
			System.out.println(topic_attribute_probabilty[i][0]+","+topic_attribute_probabilty[i][1]);
			
		}
	}
	
	@Override
	public void printTopWords(int k) {
			
		for(int i=0; i<topic_term_probabilty.length; i++) {
			MyPriorityQueue<_RankItem> fVector = new MyPriorityQueue<_RankItem>(k);
			for(int j = 0; j < vocabulary_size; j++)
				fVector.add(new _RankItem(m_corpus.getFeature(j), topic_term_probabilty[i][j]));
			for(_RankItem it:fVector)
				System.out.format("%s(%.3f)\t", it.m_name, it.m_value);
			System.out.println();
		}
	}
	
	public int[] maxbipartitematching(double[][] topic_term)
	{
		double[][] costMatrix = new double [topic_term_probabilty.length][topic_term.length];
		
		for(int i=0; i<topic_term_probabilty.length; i++) {
			for(int j=0; j<topic_term.length; j++) {
				costMatrix[i][j] = klDivergence(topic_term_probabilty[i],topic_term[j]);
			}
		}
		
		HungarianAlgorithm hma = new HungarianAlgorithm(costMatrix);
		int result[] = hma.execute();
		
		for(int i=0; i<result.length; i++)
		{
			System.out.println("Topic "+i+"mapped to topic "+result[i]);
		}
		
		return result;
	}
	
	public double klDivergence(double[] p1, double[] p2) {
		  double log2 = Math.log(2);

	      double klDiv = 0.0;

	      for (int i = 0; i < p1.length; ++i) {
	        if (p1[i] == 0) { continue; }
	        if (p2[i] == 0.0) { continue; } 

	      klDiv += p1[i] * Math.log( p1[i] / p2[i] );
	      }

	      return klDiv / log2; 
	 }
	
	public void printInDoc(String topic)
	{
		int k = 0;

		String location = "./data/"+topic+"local_attribute"+this.number_of_topics+".txt";
		
		PrintWriter writer=null;
		try {
			writer = new PrintWriter(new File(location));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		for(_Doc d:m_trainSet){
			
			
			
			writer.println("Doc:"+ k);
			writer.println("Doc Content:\n"+d.getSource());
			for(int i=0; i<d.m_topic_attribute_sstat.length; i++) {
				double sum = Utils.sumOfArray(d.m_topic_attribute_sstat[i]);
				writer.println("Topic["+i+"]: "+ d.m_topic_attribute_sstat[i][0]/sum+", "+d.m_topic_attribute_sstat[i][1]/sum);
			}
			
			for(int z=0; z<d.m_topics.length; z++)
			{
				writer.println("Topic["+z+"]: "+ d.m_topics[z]);
			}
			
			k++;
			if( k == 2690) // it is the number of doc in newEgg
				break;
		}
		
		writer.close();
		
		
		
	}
	
}
