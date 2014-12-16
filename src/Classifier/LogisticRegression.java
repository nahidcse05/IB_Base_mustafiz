package Classifier;

import java.util.Arrays;
import java.util.Collection;

import structures._Corpus;
import structures._Doc;
import structures._SparseFeature;
import utils.Utils;
import LBFGS.LBFGS;
import LBFGS.LBFGS.ExceptionWithIflag;

public class LogisticRegression extends BaseClassifier{

	double[] m_beta;
	double[] m_g;
	double m_lambda;
	
	public LogisticRegression(_Corpus c, int classNo, int featureSize){
		super(c, classNo, featureSize);
		m_beta = new double[classNo * (featureSize + 1)]; //Initialization.
		m_g = new double[m_beta.length];
		m_lambda = 0.5;//Initialize it to be 0.5.
	}
	
	public LogisticRegression(_Corpus c, int classNo, int featureSize, double lambda){
		super(c, classNo, featureSize);
		m_beta = new double[classNo * (featureSize + 1)]; //Initialization.
		m_g = new double[m_beta.length];
		m_lambda = lambda;//Initialize it to be 0.5.
	}
	
	@Override
	protected void init() {
		Arrays.fill(m_beta, 0);
	}

	/*
	 * Calculate the beta by using bfgs. In this method, we give a starting
	 * point and iterating the algorithm to find the minimum value for the beta.
	 * The input is the vector of feature[14], we need to pass the function
	 * value for the point, together with the gradient vector. When the iflag
	 * turns to 0, it finds the final point and we get the best beta.
	 */	
	@Override
	public void train(Collection<_Doc> trainSet) {
		int[] iflag = {0};
		int[] iprint = { -1, 3 };
		double[] diag = new double[m_beta.length];
		double fValue;
		int fSize = m_classNo * (m_featureSize + 1);
		
		init();
		try{
			do {
				fValue = calcFuncGradient(trainSet);
				LBFGS.lbfgs(fSize, 5, m_beta, fValue, m_g, false, diag, iprint, 1e-3, 1e-7, iflag);
			} while (iflag[0] != 0);
		} catch (ExceptionWithIflag e){
			e.printStackTrace();
		}
	}
	
	//This function is used to calculate Pij = P(Y=yi|X=xi) in multinominal LR.
	private double calculatelogPij(int Yi, _SparseFeature[] spXi){
		int offset = Yi * (m_featureSize + 1);
		double[] xs = new double [m_classNo];
		xs[Yi] = Utils.dotProduct(m_beta, spXi, offset);
		for(int i = 0; i < m_classNo; i++){
			if (i!=Yi) {
				offset = i * (m_featureSize + 1);
				xs[i] = Utils.dotProduct(m_beta, spXi, offset);
			}
		}
		return xs[Yi] - Utils.logSumOfExponentials(xs);
	}
	
	//This function is used to calculate the value and gradient with the new beta.
	private double calcFuncGradient(Collection<_Doc> trainSet) {
		
		double gValue = 0, fValue = 0;
		double Pij = 0;
		double logPij = 0;

		// Add the L2 regularization.
		double L2 = 0, b;
		for(int i = 0; i < m_beta.length; i++) {
			b = m_beta[i];
			m_g[i] = 2 * m_lambda * b;
			L2 += b * b;
		}
		
		//The computation complexity is n*classNo.
		for (_Doc doc: trainSet) {
			int Yi = doc.getYLabel();
			for(int j = 0; j < m_classNo; j++){
				logPij = calculatelogPij(j, doc.getSparse());//logP(Y=yi|X=xi)
				Pij = Math.exp(logPij);
				if (Yi == j){
					gValue = Pij - 1.0;
					fValue += logPij;
				} else
					gValue = Pij;
				
				int offset = j * (m_featureSize + 1);
				m_g[offset] += gValue;
				//(Yij - Pij) * Xi
				for(_SparseFeature sf: doc.getSparse()){
					int index = sf.getIndex();
					m_g[offset + index + 1] += gValue * sf.getValue();
				}
			}
		}
			
		// LBFGS is used to calculate the minimum value while we are trying to calculate the maximum likelihood.
		return m_lambda*L2 - fValue;
	}

	// Test the test set.
	@Override
	public void test() {
		for(_Doc doc: m_testSet){
			doc.setPredictLabel(predict(doc)); //Set the predict label according to the probability of different classes.
			m_TPTable[doc.getPredictLabel()][doc.getYLabel()] +=1; //Compare the predicted label and original label, construct the TPTable.
		}
		m_PreRecOfOneFold = calculatePreRec(m_TPTable);
		m_precisionsRecalls.add(m_PreRecOfOneFold);
	}
	
	@Override
	public int predict(_Doc doc) {
		for(int i = 0; i < m_classNo; i++)
			m_cProbs[i] = calculatelogPij(i, doc.getSparse());
		return Utils.maxOfArrayIndex(m_cProbs);
	}
}
