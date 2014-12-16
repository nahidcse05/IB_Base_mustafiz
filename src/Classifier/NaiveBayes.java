package Classifier;

import java.util.Arrays;
import java.util.Collection;

import structures._Corpus;
import structures._Doc;
import structures._SparseFeature;
import utils.Utils;

public class NaiveBayes extends BaseClassifier {
	private double[][] m_Pxy; // p(X|Y)
	private double[] m_pY;//p(Y)
	private boolean m_presence;
	private double m_deltaY; // for smoothing p(Y) purpose;
	private double m_deltaXY; // for smoothing p(X|Y) purpose;
	
	//Constructor.
	public NaiveBayes(_Corpus c, int classNumber, int featureSize){
		super(c, classNumber, featureSize);
		m_Pxy = new double [m_classNo][featureSize];
		m_pY = new double [m_classNo];
		
		m_presence = false;
		m_deltaY = 0.1;
		m_deltaXY = 0.1;
	}
	
	//Constructor.
	public NaiveBayes(_Corpus c, int classNumber, int featureSize, boolean presence, double deltaY, double deltaXY){
		super(c, classNumber, featureSize);
		m_Pxy = new double [m_classNo][featureSize];
		m_pY = new double [m_classNo];
		
		m_presence = presence;
		m_deltaY = deltaY;
		m_deltaXY = deltaXY;
	}
	
	protected void init() {
		for(int i=0; i<m_classNo; i++) {
			Arrays.fill(m_Pxy[i], 0);
			m_pY[i] = 0;
		}
	}
	
	//Train the data set.
	public void train(Collection<_Doc> trainSet){
		for(_Doc doc: trainSet){
			int label = doc.getYLabel();
			m_pY[label] ++;
			for(_SparseFeature sf: doc.getSparse())
				m_Pxy[label][sf.getIndex()] += m_presence?1:sf.getValue();
		}
		normalization();
	}
	
	//Calculate the probabilities for different features in m_model;
	public void normalization(){
		for(int i = 0; i < m_classNo; i++){
			m_pY[i] = Math.log(m_pY[i] + m_deltaY);//up to a constant since normalization of this is not important
			double sum = Math.log(Utils.sumOfArray(m_Pxy[i]) + m_featureSize*m_deltaXY);
			for(int j = 0; j < m_featureSize; j++)
				m_Pxy[i][j] = Math.log(m_deltaXY+m_Pxy[i][j]) - sum;
		}
	}
	
	//Test the data set.
	public void test(){
		for(_Doc doc: m_testSet){
			doc.setPredictLabel(predict(doc)); //Set the predict label according to the probability of different classes.
			m_TPTable[doc.getPredictLabel()][doc.getYLabel()] +=1; //Compare the predicted label and original label, construct the TPTable.
		}
		m_PreRecOfOneFold = calculatePreRec(m_TPTable);
		m_precisionsRecalls.add(m_PreRecOfOneFold);
	}
	
	//Predict the label for one document.
	@Override
	public int predict(_Doc d){
		for(int i = 0; i < m_classNo; i++){
			m_cProbs[i] = m_pY[i];
			for(_SparseFeature f:d.getSparse())
				m_cProbs[i] += m_Pxy[i][f.getIndex()] * (m_presence?1.0:f.getValue());
		}
		return Utils.maxOfArrayIndex(m_cProbs);
	}
	
	//Save the parameters for classification.
	public void saveModel(String modelLocation){
		
	}
}
