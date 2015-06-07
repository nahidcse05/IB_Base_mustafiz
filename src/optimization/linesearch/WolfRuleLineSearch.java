package optimization.linesearch;

import java.io.PrintStream;
import java.util.ArrayList;

import optimization.util.Interpolation;

/**
 * Nocedal gives example values 
  of c1 = 10-4 and c2 = 0.9 
 * for Newton or quasi-Newton methods 
 * and c2 = 0.1 for the
 * nonlinear conjugate gradient method. 
 */



/**
 * 
 * @author javg
 *
 */
public class WolfRuleLineSearch implements LineSearchMethod{

	GenericPickFirstStep pickFirstStep;
	
	double c1 = 1.0E-4;
	double c2 = 0.9;
	
	//Application dependent
	double maxStep;
	
	int extrapolationIteration;
	int maxExtrapolationIteration = 10;
	
	
	double minZoomDiffTresh = 10E-8;

	
	ArrayList<Double> steps;
	ArrayList<Double> gradientDots;
	ArrayList<Double> functionVals;
	ArrayList<Boolean> inZoom;
	
	int debugLevel = 0;
	boolean foudStep = false;
	
	public WolfRuleLineSearch(GenericPickFirstStep pickFirstStep){
		this.pickFirstStep = pickFirstStep;
		
	}
	
	

	
	public WolfRuleLineSearch(GenericPickFirstStep pickFirstStep,  double c1, double c2, double maxStepSize){
		this.maxStep = maxStepSize; 
		this.pickFirstStep = pickFirstStep;
		initialStep = pickFirstStep.getFirstStep(this);
		this.c1 = c1;
		this.c2 = c2;
	}
	
	public void setDebugLevel(int level){
		debugLevel = level;
	}
	
	//Experiment
	double previousStepPicked = -1;;
	double previousInitGradientDot = -1;
	double currentInitGradientDot = -1;
	
	double initialStep;

	
	public void reset(){
		previousStepPicked = -1;;
		previousInitGradientDot = -1;
		currentInitGradientDot = -1;
		if(steps != null)
			steps.clear();
		if(gradientDots != null)
			gradientDots.clear();
		if(functionVals != null)
			functionVals.clear();
		if(inZoom != null)
			inZoom.clear();
	}
	
	public void setInitialStep(double initial){
		initialStep = pickFirstStep.getFirstStep(this);
	}
	
	
	
	/**
	 * Implements Wolf Line search as described in nocetal.
	 * This process consists in two stages. The first stage we try to satisfy the
	 * biggest step size that still satisfies the curvature condition. We keep increasing
	 * the initial step size until we find a step satisfying the curvature condition, we return 
	 * success, we failed the sufficient increase so we cannot increase more and we can call zoom with 
	 * that maximum step, or we pass the minimum in which case we can call zoom the same way. 
	 * 
	 */
	public double getStepSize(DifferentiableLineSearchObjective objective){
		//System.out.println("entering line search");
		
		foudStep = false;
		if(debugLevel >= 1){
			steps = new ArrayList<Double>();
			gradientDots = new ArrayList<Double>();
			functionVals  =new ArrayList<Double>();
			inZoom = new ArrayList<Boolean>();
		}
		
		//test
		currentInitGradientDot = objective.getInitialGradient();
		
		
		double previousValue = objective.getCurrentValue();
		double previousStep = 0;
		double currentStep =pickFirstStep.getFirstStep(this);
		for(extrapolationIteration = 0; 
		extrapolationIteration < maxExtrapolationIteration; extrapolationIteration++){	
			
			objective.updateAlpha(currentStep);
			double currentValue = objective.getCurrentValue();
			if(debugLevel >= 1){
				steps.add(currentStep);
				functionVals.add(currentValue);
				gradientDots.add(objective.getCurrentGradient());
				inZoom.add(false);
			}
			
			
			//The current step does not satisfy the sufficient decrease condition anymore
			// so we cannot get bigger than that calling zoom.
			if(!WolfeConditions.suficientDecrease(objective,c1)||					
					(extrapolationIteration > 0 && currentValue >= previousValue)){
				currentStep = zoom(objective,previousStep,currentStep,objective.nrIterations-1,objective.nrIterations);
				break;
			}
			
			//Satisfying both conditions ready to leave
			if(WolfeConditions.sufficientCurvature(objective,c1,c2)){
				//Found step
				foudStep = true;
				break;
			}
			
			/**
			 * This means that we passed the minimum already since the dot product that should be 
			 * negative (descent direction) is now positive. So we cannot increase more. On the other hand
			 * since we know the direction is a descent direction the value the objective at the current step
			 * is for sure smaller than the preivous step so we change the order.
			 */
			if(objective.getCurrentGradient() >= 0){
				currentStep =  zoom(objective,currentStep,previousStep,objective.nrIterations,objective.nrIterations-1);
				break;
			}
			
			
			//Ok, so we can still get a bigger step, 
			double aux = currentStep;
			//currentStep = currentStep*2;
			if(Math.abs(currentStep-maxStep)>1.1e-2){
				currentStep = (currentStep+maxStep)/2;
			}else{
				currentStep = currentStep*2;
			}
			previousStep = aux;
			previousValue = currentValue;
			//Could be done better
			if(currentStep >= maxStep){
				System.out.println("Excedded max step...calling zoom with maxStepSize " + maxStep);
				currentStep = zoom(objective,previousStep,maxStep,objective.nrIterations-1,objective.nrIterations);
				break;
			}
		}
		if(!foudStep){
			System.out.println("Wolfe Rule exceed number of iterations");
			if(debugLevel >= 1){
				printSmallWolfeStats(System.out);		
			}
			return -1;
		}
		if(debugLevel >= 2){
			printSmallWolfeStats(System.out);
		}

		previousStepPicked = currentStep;
		previousInitGradientDot = currentInitGradientDot;
//		objective.printLineSearchSteps();
		return currentStep;
	}
	
	
	
	
	
	public void printWolfeStats(PrintStream out){
		for(int i = 0; i < steps.size(); i++){		
			out.println("Step " + steps.get(i) + " value " + functionVals.get(i) + " dot " + gradientDots.get(i));
		}
	}
	
	public void printSmallWolfeStats(PrintStream out){
		for(int i = 0; i < steps.size(); i++){		
			out.print(inZoom.get(i) + ":"+ steps.get(i) + ":"+functionVals.get(i)+":"+gradientDots.get(i)+" ");
		}
		System.out.println();
	}
	
	
	
	/**
	 * Pick a step satisfying the strong wolfe condition from an given from lowerStep and higherStep
	 * picked on the routine above.
	 * 
	 * Both lowerStep and higherStep have been evaluated, so we only need to pass the iteration where they have
	 * been evaluated and save extra evaluations.
	 * 
	 * We know that lowerStepValue as to be smaller than higherStepValue, and that a point 
	 * satisfying both conditions exists in such interval.
	 * 
	 * LowerStep always satisfies at least the sufficient decrease
	 * 
	 * Step can never leave the interval lowerStep higherStep 
	 * @return
	 */
	public double zoom(DifferentiableLineSearchObjective o, double lowerStep, double higherStep,
			int lowerStepIter, int higherStepIter){
		
		if(debugLevel >=1){
			System.out.println("Entering zoom with " + lowerStep+"-"+higherStep);
		}
		//Case the limits are the same
		if(Math.abs(lowerStep - higherStep) < 1E-10){
			foudStep = true;
			return lowerStep; 
		}
		double currentStep=-1;	
		int zoomIter = 0;
		while(zoomIter < 30){	
			//If difference between steps is too small leave and return the smaller step which satisfies 
			// the decrease condition
			if(Math.abs(lowerStep-higherStep) < minZoomDiffTresh){
				o.updateAlpha(lowerStep);
				if(debugLevel >= 1){
					steps.add(lowerStep);
					functionVals.add(o.getCurrentValue());
					gradientDots.add(o.getCurrentGradient());
					inZoom.add(true);
				}
				foudStep = true;
				return lowerStep;
			}	
	
			
			
			
			//FIXME ITERPOLATION IS NOT WORKING. SHOULD LOOK AT WHAT VALUES ARE IN O AND IF CODE IS WELL MADE
			// FOR NOW I AM REMOVING IT
			//Try to find a step using cubic interpolation
//			currentStep = 
//				Interpolation.cubicInterpolation(lowerStep, o.getValue(lowerStepIter), o.getGradient(lowerStepIter), 
//						higherStep, o.getValue(higherStepIter), o.getGradient(higherStepIter));
//			
//			
//			if(currentStep > higherStep || currentStep < lowerStep){
//				System.err.println("Cubic Interpolation return a step outside boundaries");
//				System.err.println("ls " + lowerStep + " cs " + currentStep + " hs " + higherStep);
//			}
//			
//			//Safeguard..Test if step is very close to boundary or small 
//			if(Math.abs(lowerStep - currentStep) < minZoomDiffTresh || Math.abs(higherStep - currentStep) < minZoomDiffTresh || currentStep < 0){
//				currentStep = (lowerStep+higherStep)/2;
//			}
//			
//			
//			if(Double.isNaN(currentStep) || Double.isInfinite(currentStep)){
//				currentStep = (lowerStep+higherStep)/2;
//			}
			currentStep = (lowerStep+higherStep)/2;
//			System.out.println("Trying "+currentStep);
			o.updateAlpha(currentStep);
			if(debugLevel >=1){
				steps.add(currentStep);
				functionVals.add(o.getCurrentValue());
				gradientDots.add(o.getCurrentGradient());
				inZoom.add(true);
			}
			if(!WolfeConditions.suficientDecrease(o,c1)
					|| o.getCurrentValue() >= o.getValue(lowerStepIter)){
				higherStepIter = o.nrIterations;
				higherStep = currentStep;
			}
			//Note when entering here the new step satisfies the sufficent decrease and
			// or as a function value that is better than the previous best (lowerStepFunctionValues)
			// so we either leave or change the value of the alpha low.
			else{
				if(WolfeConditions.sufficientCurvature(o,c1,c2)){
					//Satisfies the both wolf conditions
					foudStep = true;
					break;
				}
				//If does not satisfy curvature 
				if(o.getCurrentGradient()*(higherStep-lowerStep) >= 0){
					higherStep = lowerStep;
					higherStepIter = lowerStepIter;
				}
				lowerStep = currentStep;
				lowerStepIter = o.nrIterations;
			}
			
			if(debugLevel >= 1){
				System.out.println("New Lower: " + lowerStep + " new Higher: " + higherStep);
			}
			
			zoomIter++;
		}
		return currentStep;
	}

	public double getInitialGradient() {
		return currentInitGradientDot;
		
	}

	public double getPreviousInitialGradient() {
		return previousInitGradientDot;
	}

	public double getPreviousStepUsed() {
		return previousStepPicked;
	}
	
	
}
