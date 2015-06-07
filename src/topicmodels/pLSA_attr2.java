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


public class pLSA_attr2 extends twoTopic {
	// Dirichlet prior for p(\theta|d)
	double d_alpha; // smoothing of p(z|d)
	int optimizer_success_count = 0;
	int new_egg_doc_counter = 0;
	
	double[][] topic_term_probabilty ; /* p(w|z) */
	double[][] topic_attribute_probabilty ; /* p(a|z) */
	
	double[][] word_topic_sstat; /* fractional count for p(z|d,w) */
	double[][] attribute_topic_sstat; /* fractional count for attribute and topic */
	int [] mapping;
	
	
	public pLSA_attr2(int number_of_iteration, double converge, double beta, _Corpus c, //arguments for general topic model
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
		this.mapping = new int [this.number_of_topics/2];
		
		// initial mapping  (0 ->5 , 1->6 etc)
		for(int i=0; i<this.number_of_topics/2; i++)
		{
			this.mapping[i] = i + this.number_of_topics/2;
		}
		
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
	
	@Override
	public double calculate_E_step(_Doc d) {	
		double propB; // background proportion
		double exp; // expectation of each term under topic assignment
		double poscount = 0.0;
		double negcount = 0.0;

		
		if(d.from == 1)
		{
			for(_SparseFeature fv:d.getSparse()) {
				int j = fv.getIndex(); // jth word in doc
				poscount = fv.poscount;
				negcount = fv.negcount;
				
				
				//-----------------compute posterior----------- 
				double sum = 0;
				for(int k=0;k<this.number_of_topics;k++)
					sum += d.m_topics[k]*topic_term_probabilty[k][j];//shall we compute it in log space?

				propB = m_lambda * background_probability[j];
				propB /= propB + (1-m_lambda) * sum;//posterior of background probability

				
				
				
				double pros_sum = 0;
				for(int k=0;k<this.number_of_topics/2;k++)
					pros_sum += d.m_topics[k]*topic_term_probabilty[k][j];//shall we compute it in log space?

				//-----------------compute and accumulate expectations----------- 
				for(int k=0;k<this.number_of_topics/2;k++) {
					exp = poscount * (1-propB)*d.m_topics[k]*topic_term_probabilty[k][j]/pros_sum;
					d.m_sstat[k] += exp;

					if (m_collectCorpusStats)
					{
						word_topic_sstat[k][j] += exp;
					}
				}
				
				
				
				double cons_sum = 0;
				for(int k=this.number_of_topics/2;k<this.number_of_topics;k++)
					cons_sum += d.m_topics[k]*topic_term_probabilty[k][j];//shall we compute it in log space?

				//-----------------compute and accumulate expectations----------- 
				for(int k=this.number_of_topics/2;k<this.number_of_topics;k++) {
					exp = negcount * (1-propB)*d.m_topics[k]*topic_term_probabilty[k][j]/cons_sum;
					d.m_sstat[k] += exp;

					if (m_collectCorpusStats)
					{
						word_topic_sstat[k][j] += exp;
					}
				}
				
				
			}
		}
		
		
		
		if(d.from  == 2)
		{
			for(_SparseFeature fv:d.getSparse()) {
				int j = fv.getIndex(); // jth word in doc
				double v = fv.getValue();
				//-----------------compute posterior----------- 
				double sum = 0;
				for(int k=0;k<this.number_of_topics;k++)
					sum += d.m_topics[k]*topic_term_probabilty[k][j];//shall we compute it in log space?

				propB = m_lambda * background_probability[j];
				propB /= propB + (1-m_lambda) * sum;//posterior of background probability

				//-----------------compute and accumulate expectations----------- 
				for(int k=0;k<this.number_of_topics;k++) {
					exp = v * (1-propB)*d.m_topics[k]*topic_term_probabilty[k][j]/sum;
					d.m_sstat[k] += exp;

					if (m_collectCorpusStats)
					{
						word_topic_sstat[k][j] += exp;
					}
				}
			}


		}
		
		
		
		
		// p(z | d) 
		double sum_topics = Utils.sumOfArray(d.m_sstat);
		for(int k=0;k<this.number_of_topics;k++)
			d.m_topics[k] = d.m_sstat[k] / sum_topics;	
		
		
		double constraint_set [] = new double[this.number_of_topics];
		
		int range = this.number_of_topics/2; 
		
		
		
	/*	for(int k=0; k<range;k++)
		{
			constraint_set[k] = d.m_sstat[k] * d.m_sstat[k+range];
			constraint_set[k+range] = d.m_sstat[k] * d.m_sstat[k+range];
		}
		*/
		

		for(int k=0; k<range;k++)
		{
			constraint_set[k] = d.m_sstat[k] * d.m_sstat[this.mapping[k]];
			constraint_set[this.mapping[k]] = d.m_sstat[k] * d.m_sstat[this.mapping[k]];
		}
		
		
		this.new_egg_doc_counter++;
		double gdelta = 1e-5, istp = 1.0;
		int maxStep = 50;
		problem_prattrb testcase = new problem_prattrb(d.m_topics,constraint_set);			
		testcase.setDebugLevel(-1);

		LineSearchMethod ls = new ArmijoLineSearchMinimizationAlongProjectionArc(new InterpolationPickFirstStep(istp));
		ProjectedGradientDescent optimizer = new ProjectedGradientDescent(ls);
		StopingCriteria stopGrad = new ProjectedGradientL2Norm(gdelta);
		CompositeStopingCriteria compositeStop = new CompositeStopingCriteria();
		compositeStop.add(stopGrad);
		optimizer.setMaxIterations(maxStep);

		if (optimizer.optimize(testcase, new OptimizerStats(), compositeStop)) {
			this.optimizer_success_count++;

			d.m_topics = testcase.getPA();
			System.out.println("Here:"+this.optimizer_success_count);
		}

		
		return calculate_log_likelihood(d);
	}
	
	@Override
	public void calculate_M_step() {
		
		System.out.println("Succes #:"+this.optimizer_success_count+", Doc #:"+this.new_egg_doc_counter);
		if(this.new_egg_doc_counter!=0)
			System.out.println("Success Rate:" + (double)(this.optimizer_success_count/this.new_egg_doc_counter) +"\n");
		

		optimizer_success_count = 0;
		new_egg_doc_counter = 0; 
		// update topic-term matrix -------------
		double sum = 0;
		for(int k=0;k<this.number_of_topics;k++) {
			sum = Utils.sumOfArray(word_topic_sstat[k]);
			for(int i=0;i<this.vocabulary_size;i++)
				topic_term_probabilty[k][i] = word_topic_sstat[k][i] / sum;
		}
		
		//maxbipartitematching_new(); 
		
		
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
		for(_SparseFeature fv:d.getSparse()) {
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
	
	public void maxbipartitematching(double[][] topic_term)
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
	}
	
	
	public void maxbipartitematching_new()
	{
		
		int range = this.number_of_topics/2;
		double[][] costMatrix = new double [range][range];
		
		
		
		for(int i=0; i<range; i++) {
			for(int j=range,  k =0; j<this.number_of_topics; j++, k++) {
				costMatrix[i][k] = klDivergence(topic_term_probabilty[i],topic_term_probabilty[j]);
			}
		}
		
		HungarianAlgorithm hma = new HungarianAlgorithm(costMatrix);
		this.mapping = hma.execute();
		
		for(int i=0; i<this.mapping.length; i++)
		{
			this.mapping[i] = this.mapping[i] + range; // rescaling
			System.out.println("Topic "+i+"mapped to topic "+this.mapping[i]);
		}
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
