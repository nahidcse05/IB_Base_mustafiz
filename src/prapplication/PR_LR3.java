package prapplication;

import optimization.gradientBasedMethods.ProjectedObjective;
import optimization.projections.BoundsProjection;
import optimization.projections.Projection;
import util.Utils;

public class PR_LR3 extends ProjectedObjective {

	double m_p[], m_q[]; // NOTE m_q is unnormalized
	double[][] m_phi_Z_x;
	double m_epsilon = 0.05; // slack variable
	Projection m_projection;
	
	final int C = 5, CONT_SIZE = 4;// class size and constraint size
	
	public PR_LR3(double p[], int true_label) {//HdT means the difference between head and tail 
		m_p = p;
		initiate_constraint_feature(true_label);
		parameters = new double[]{1.0, 1.0, 1.0, 1.0};//start from a legal point
		gradient = new double[]{0.0, 0.0, 0.0, 0.0};
		m_projection = new BoundsProjection(0.0, Double.MAX_VALUE);
	}

	public void initiate_constraint_feature(int label) {
		m_phi_Z_x = new double[C][CONT_SIZE];
		m_q = new double[C];
		
		if(label == 0)
		{
			m_phi_Z_x[0][0] = -1;
			m_phi_Z_x[0][1] = 0;
			m_phi_Z_x[0][2] = 0;
			m_phi_Z_x[0][3] = 0;
			
			m_phi_Z_x[1][0] = 0;
			m_phi_Z_x[1][1] = -1;
			m_phi_Z_x[1][2] = 0;
			m_phi_Z_x[1][3] = 0;
			
			m_phi_Z_x[2][0] = 0;
			m_phi_Z_x[2][1] = 1;
			m_phi_Z_x[2][2] = -1;
			m_phi_Z_x[2][3] = 0;
			
			m_phi_Z_x[3][0] = 0;
			m_phi_Z_x[3][1] = 0;
			m_phi_Z_x[3][2] = 1;
			m_phi_Z_x[3][3] = -1;
			
			m_phi_Z_x[4][0] = 0;
			m_phi_Z_x[4][1] = 0;
			m_phi_Z_x[4][2] = 0;
			m_phi_Z_x[4][3] = 1;
			
		}
		
		if(label == 1)
		{
			m_phi_Z_x[0][0] = -1;
			m_phi_Z_x[0][1] = 0;
			m_phi_Z_x[0][2] = 0;
			m_phi_Z_x[0][3] = 0;
			
			m_phi_Z_x[1][0] = -1;
			m_phi_Z_x[1][1] = 0;
			m_phi_Z_x[1][2] = -1;
			m_phi_Z_x[1][3] = 0;
			
			m_phi_Z_x[2][0] = -1;
			m_phi_Z_x[2][1] = 0;
			m_phi_Z_x[2][2] = -1;
			m_phi_Z_x[2][3] = 0;
			
			m_phi_Z_x[3][0] = 0;
			m_phi_Z_x[3][1] = 0;
			m_phi_Z_x[3][2] = 1;
			m_phi_Z_x[3][3] = -1;
			
			m_phi_Z_x[4][0] = 0;
			m_phi_Z_x[4][1] = 0;
			m_phi_Z_x[4][2] = 0;
			m_phi_Z_x[4][3] = 1;
		}
		
		if(label == 2)
		{
			
			m_phi_Z_x[0][0] = 1;
			m_phi_Z_x[0][1] = 0;
			m_phi_Z_x[0][2] = 0;
			m_phi_Z_x[0][3] = 0;
			
			m_phi_Z_x[1][0] = -1;
			m_phi_Z_x[1][1] = -1;
			m_phi_Z_x[1][2] = 0;
			m_phi_Z_x[1][3] = 0;
			
			m_phi_Z_x[2][0] = 0;
			m_phi_Z_x[2][1] = -1;
			m_phi_Z_x[2][2] = 0;
			m_phi_Z_x[2][3] = -1;
			
			m_phi_Z_x[3][0] = 0;
			m_phi_Z_x[3][1] = -1;
			m_phi_Z_x[3][2] = 0;
			m_phi_Z_x[3][3] = -1;
			
			m_phi_Z_x[4][0] = 0;
			m_phi_Z_x[4][1] = 0;
			m_phi_Z_x[4][2] = 0;
			m_phi_Z_x[4][3] = 1;
		}
		if(label == 3)
		{
			m_phi_Z_x[0][0] = 1;
			m_phi_Z_x[0][1] = 0;
			m_phi_Z_x[0][2] = 0;
			m_phi_Z_x[0][3] = 0;
			
			m_phi_Z_x[1][0] = -1;
			m_phi_Z_x[1][1] = 1;
			m_phi_Z_x[1][2] = 0;
			m_phi_Z_x[1][3] = 0;
			
			m_phi_Z_x[2][0] = 0;
			m_phi_Z_x[2][1] = -1;
			m_phi_Z_x[2][2] = -1;
			m_phi_Z_x[2][3] = 0;
			
			m_phi_Z_x[3][0] = 0;
			m_phi_Z_x[3][1] = 0;
			m_phi_Z_x[3][2] = -1;
			m_phi_Z_x[3][3] = -1;
			
			m_phi_Z_x[4][0] = 0;
			m_phi_Z_x[4][1] = 0;
			m_phi_Z_x[4][2] = 1;
			m_phi_Z_x[4][3] = 0;
			
		
		}
		if(label == 4)
		{
			m_phi_Z_x[0][0] = 1;
			m_phi_Z_x[0][1] = 0;
			m_phi_Z_x[0][2] = 0;
			m_phi_Z_x[0][3] = 0;
			
			m_phi_Z_x[1][0] = -1;
			m_phi_Z_x[1][1] = 1;
			m_phi_Z_x[1][2] = 0;
			m_phi_Z_x[1][3] = 0;
			
			m_phi_Z_x[2][0] = 0;
			m_phi_Z_x[2][1] = -1;
			m_phi_Z_x[2][2] = 1;
			m_phi_Z_x[2][3] = 0;
			
			m_phi_Z_x[3][0] = 0;
			m_phi_Z_x[3][1] = 0;
			m_phi_Z_x[3][2] = -1;
			m_phi_Z_x[3][3] = 0;
			
			m_phi_Z_x[4][0] = 0;
			m_phi_Z_x[4][1] = 0;
			m_phi_Z_x[4][2] = -1;
			m_phi_Z_x[4][3] = 0;
		}
	}
	
	private void calcPosterior() {
		for(int i=0; i<C; i++) {
			double sum = 0.0;
			for(int j=0; j<CONT_SIZE;j++)
				sum -= parameters[j] * m_phi_Z_x[i][j];
			m_q[i] = m_p[i] * Math.exp(sum);
		}
	}
	
	@Override
	public double[] getGradient() {
		gradientCalls ++;
		calcPosterior();
		
		double z_lambda = Utils.sumOfArray(m_q);
		for(int i=0; i<CONT_SIZE; i++) {
			gradient[i] = 2*m_epsilon*parameters[i];
			for(int k=0; k<C; k++)
				gradient[i] -= m_phi_Z_x[k][i]*m_q[k]/z_lambda;
		}
		return gradient;
	}

	@Override
	public double getValue() {
		functionCalls++;
		calcPosterior();
		double fvalue = Math.log(Utils.sumOfArray(m_q)) + m_epsilon*Utils.dotProduct(parameters, parameters);
		//System.out.println("objective: " + fvalue);
		return fvalue;
	}
	
	@Override
	public double[] projectPoint(double[] point) {
		double[] newPoint = point.clone();
		m_projection.project(newPoint);
		return newPoint;
	}
	
	public double[] getPA() {
		calcPosterior();
		Utils.scaleArray(m_q, Utils.sumOfArray(m_q));
		
		/*System.out.println("The lambda are here");
		for(int i=0; i<CONT_SIZE; i++)
			System.out.println("param: "+i + ":" + parameters[i]);		*/
		return m_q;
	}
	
	public double[] getConstraint(int label) {
		return m_phi_Z_x[label];
	}
	
	public double[] getLambdas() {
		return parameters;
	}
	
	
	@Override
	public String toString() {
		return "PRLogisticRegression";
	}
}

