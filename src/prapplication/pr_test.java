package prapplication;

import optimization.gradientBasedMethods.ProjectedGradientDescent;
import optimization.gradientBasedMethods.stats.OptimizerStats;
import optimization.linesearch.ArmijoLineSearchMinimizationAlongProjectionArc;
import optimization.linesearch.InterpolationPickFirstStep;
import optimization.linesearch.LineSearchMethod;
import optimization.stopCriteria.CompositeStopingCriteria;
import optimization.stopCriteria.ProjectedGradientL2Norm;
import optimization.stopCriteria.StopingCriteria;

public class pr_test {

	public static void main(String[] args) {
		
		
		double tmp_pij [] = {0.1531478099455653, 0.12905204209451868, 0.1350820938150242, 0.15593451022541338, 0.42678354391947854};
		//double tmp_pij [] = {0.2,0.2,0.2,0.2,0.2};
		int  Yi = 2;
		double pr_pij[] = new double [5];
		
		System.out.println("True Label:"+ Yi);
		for(int i=0; i<tmp_pij.length; i++)
		{
			System.out.println("Real Pij:"+ tmp_pij[i]);
		}
		System.out.println();
		double gdelta = 1e-5, istp = 1.0;
		int maxStep = 5000;
		
		PR_LR3 testcase = new PR_LR3(tmp_pij, Yi); // Yi is the true label			
		testcase.setDebugLevel(-1);
		
		LineSearchMethod ls = new ArmijoLineSearchMinimizationAlongProjectionArc(new InterpolationPickFirstStep(istp));
		ProjectedGradientDescent optimizer = new ProjectedGradientDescent(ls);
		StopingCriteria stopGrad = new ProjectedGradientL2Norm(gdelta);
		CompositeStopingCriteria compositeStop = new CompositeStopingCriteria();
		compositeStop.add(stopGrad);
		optimizer.setMaxIterations(maxStep);
		
		if (optimizer.optimize(testcase, new OptimizerStats(), compositeStop)) {
			pr_pij = testcase.getPA(); // get the regularized PR here
		}
		
		for(int i=0; i<pr_pij.length; i++)
		{
			System.out.println("Regularized Pij:"+ pr_pij[i]);
		}

	}

}
