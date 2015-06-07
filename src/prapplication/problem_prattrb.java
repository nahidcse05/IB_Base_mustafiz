package prapplication;

import optimization.gradientBasedMethods.ProjectedObjective;
import optimization.projections.BoundsProjection;
import optimization.projections.Projection;
import util.Utils;

public class problem_prattrb extends ProjectedObjective {

	double[] m_pA;
	double[] m_phi_Z_x;
	double m_epsilon = 0.1; // slack variable
	Projection m_projection;
	
	public problem_prattrb(double[] pA, double[] cz) {//caz expected count of attribute in topic 
		m_pA = pA;
		m_phi_Z_x = new double[m_pA.length];// number of topics
		parameters = new double[m_pA.length];
		gradient = new double[m_pA.length];
		
		
		for(int z = 0; z <m_pA.length; z++ ){
			
			
				m_phi_Z_x[z] = cz[z];
			
				
				//constraint 1
				//m_phi_Z_x[z] = cz[z][0] * cz[z][1];
				
				//constraint 2
				//m_phi_Z_x[z] = (cz[z][0] - cz[z][1])*(cz[z][0] - cz[z][1]);
				
				//constraint 3 Entropy
			/*	double p_z_a_0 = (double)cz[z][0] / (cz[z][0] + cz[z][1]);
				double p_z_a_1 = (double)cz[z][1] / (cz[z][0] + cz[z][1]);
			    m_phi_Z_x[z] =  (-1)*p_z_a_0*Math.log(p_z_a_0) + (-1)*p_z_a_1*Math.log(p_z_a_1);
			*/	
				parameters[z] = 1.0;
				gradient[z] = 0.0;
		}
		m_projection = new BoundsProjection(0.0,Double.MAX_VALUE);
	}

	@Override
	public double[] getGradient() {
		gradientCalls ++;

		double z_lambda = 0.0;
		for(int z = 0; z <m_pA.length; z++ ){
			z_lambda = z_lambda + m_pA[z]*Math.exp(-parameters[z]*m_phi_Z_x[z]);
		
		}
		
		for(int z = 0; z <m_pA.length; z++ ){
			gradient[z] = 2*m_epsilon*parameters[z];
			gradient[z]= gradient[z] - m_pA[z]*Math.exp(-parameters[z]*m_phi_Z_x[z])*m_phi_Z_x[z]/z_lambda; 
		}
		
		return gradient;
	}

	@Override
	public double getValue() {
		functionCalls++;
		double z_lambda = 0.0;
		for(int z = 0; z <m_pA.length; z++ ){
			z_lambda = z_lambda + m_pA[z]*Math.exp(-parameters[z]*m_phi_Z_x[z]);
		}
		return Math.log(z_lambda) + m_epsilon*Utils.dotProduct(parameters, parameters);
	}
	
	@Override
	public double[] projectPoint(double[] point) {
		double[] newPoint = point.clone();
		m_projection.project(newPoint);
		return newPoint;
	}
	
	public double[] getPA() {
		double z_lambda = 0.0;
		for(int z = 0; z <m_pA.length; z++ ){
		
			//System.out.print(parameters[z]+",");
			z_lambda = z_lambda + m_pA[z]*Math.exp(-parameters[z]*m_phi_Z_x[z]);
		
		}
		System.out.println();
		double[] m_pA_final = new double[m_pA.length];
		for(int z = 0; z <m_pA.length; z++ ){
			for(int i=0; i<m_pA.length; i++)
				m_pA_final[z] =  m_pA[z]*Math.exp(-parameters[z]*m_phi_Z_x[z]) / z_lambda;
		
		}
		
	
		
		return m_pA_final;
	}

	@Override
	public String toString() {
		return "Problem PLSA Attrb";
	}
}
