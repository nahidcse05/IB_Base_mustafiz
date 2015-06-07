package optimization.stopCriteria;

import optimization.gradientBasedMethods.Objective;
import util.ArrayMath;

public class GradientL2Norm implements StopingCriteria{
	
	/**
	 * Stop if gradientNorm/(originalGradientNorm) smaller
	 * than gradientConvergenceValue
	 */
	protected double gradientConvergenceValue;
	
	
	public GradientL2Norm(double gradientConvergenceValue){
		this.gradientConvergenceValue = gradientConvergenceValue;
	}
	
	public void reset(){}
	
	public boolean stopOptimization(Objective obj){
		double norm = ArrayMath.L2Norm(obj.gradient);
		if(norm < gradientConvergenceValue){
			System.out.println("Gradient norm below treshold");
			return true;
		}
		return false;
		
	}
}
