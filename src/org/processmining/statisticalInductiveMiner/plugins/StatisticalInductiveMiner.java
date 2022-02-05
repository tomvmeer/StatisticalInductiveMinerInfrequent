package org.processmining.statisticalInductiveMiner.plugins;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

import javax.swing.JOptionPane;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XLogImpl;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.packages.PackageManager.Canceller;
import org.processmining.framework.plugin.ProMCanceller;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.plugins.InductiveMiner.efficienttree.ProcessTree2EfficientTree;
import org.processmining.plugins.InductiveMiner.efficienttree.UnknownTreeNodeException;
import org.processmining.plugins.InductiveMiner.mining.MiningParameters;
import org.processmining.plugins.InductiveMiner.mining.MiningParametersIM;
import org.processmining.plugins.InductiveMiner.plugins.IMProcessTree;
import org.processmining.plugins.InductiveMiner.plugins.dialogs.IMMiningDialog;
import org.processmining.processtree.ProcessTree;
import org.processmining.projectedrecallandprecision.framework.CompareParameters.RecallName;
import org.processmining.projectedrecallandprecision.helperclasses.AutomatonFailedException;
import org.processmining.projectedrecallandprecision.plugins.CompareLog2ProcessTreePlugin;
import org.processmining.projectedrecallandprecision.plugins.CompareParametersDialog;
import org.processmining.projectedrecallandprecision.result.ProjectedRecallPrecisionResult;
import org.processmining.projectedrecallandprecision.result.ProjectedRecallPrecisionResult.ProjectedMeasuresFailedException;



public class StatisticalInductiveMiner{
	
	//for use in the algorithm
	double probabilityThreshold=0.01;
	double confidenceLevel=0.99;
	boolean eventTimeAnalysis=true;
	double epsilonInMinutes=5;


	//for use in performance analysis
	double deltaModelIncrement=1;
	double deltaEventIncrement=1;
	int NoOfExperiments=101;
	
	
	@Plugin(name = "Mine Process Tree with statistical Inductive Miner", returnLabels = { "Process Tree" }, returnTypes = { ProcessTree.class }, parameterLabels = { "Log" }, userAccessible = true)
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Martin Bauer", email = "bauermax@informatik.hu-berlin.de")
	@PluginVariant(variantLabel = "Mine a Process Tree, dialog", requiredParameterLabels = { 0 })
	//get mining parameters
	//preprocess log
	//mine with new log
	public ProcessTree mineStatisticalGuiProcessTree(final UIPluginContext context, XLog log) throws Exception {
			MiningParameters parameters= getMiningParameters(context, log);
			XLog newLog = statisticalPreprocess(log);
			ProcessTree processTree = mineProcessTree(context, newLog, parameters);
			return processTree;
	}
	
	
	@Plugin(name = "Mine log with statistics", returnLabels = { "Log" }, returnTypes = { XLog.class }, parameterLabels = { "Log" }, userAccessible = true)
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Martin Bauer", email = "bauermax@informatik.hu-berlin.de")
	@PluginVariant(variantLabel = "Mine a Process Tree, dialog", requiredParameterLabels = { 0 })
	//get mining parameters
	//preprocess log
	//mine with new log
	public XLog mineStatisticalGuiLog(final UIPluginContext context, XLog log) throws Exception {
			XLog newLog = statisticalPreprocess(log);
			return newLog;
	}
	
	
	
	@Plugin(name = "Analyze performance of method paper", returnLabels = { "Process Tree" }, returnTypes = { String.class }, parameterLabels = { "Log" }, userAccessible = true)
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Martin Bauer", email = "bauermax@informatik.hu-berlin.de")
	@PluginVariant(variantLabel = "Mine a Process Tree, dialog", requiredParameterLabels = { 0 })
	public String ExecuteAnalysis(final UIPluginContext context, XLog log) throws Exception {
		System.gc();
		
		//Analysis criteria that will be measured 
		String IMpreProcessingTimes=		" preProcessingTime: ";
		String IMBaseTime=		            " Baseline time:     ";
		String totalTraces=					" Total Traces:      ";
		String tracesLeft=					" Traces Left:       ";
		String IMtotalTimes=				" TotalTime:         ";
		String IMTotalMemoryConsumption=	" Memory:            ";
		String IMiPrecisionBefore=		    " Baseline Precison: ";
		String IMiRecallBefore=		        " Baseline Recall:   ";
		String IMiPrecision=				" Precision:         ";
		String IMiRecall=					" Recall:            ";	
		String IMiPrecisionPaper=			" Precision Paper:   ";
		String IMiRecallPaper=				" Recall Paper:      ";	
	
		
		String filename = "C:\\Users\\mr\\Desktop\\data\\output";
		System.out.println("Starting sIM Analysis - Results will be written to "+filename+".txt");
		totalTraces=totalTraces + Integer.toString(log.size());
		
		//get Mining Parameters per GUI
		MiningParameters parameters= getMiningParameters(context, log);
		
		//k times
		System.gc();
		for(int k=1;k<=NoOfExperiments;k++){
			System.gc();
			System.out.println("\tStarting Measurement "+(k)+" of "+NoOfExperiments);
			
			System.out.println("\tCalculating Fitness and Precision of unfiltered IM");
			System.out.println("\t\t IM - Mining");
			long baseStart = System.currentTimeMillis();
			ProcessTree iMProcessTree=mineProcessTree(context, log, parameters);
			long baseEnd = System.currentTimeMillis();
			System.out.println("\t\t IM - Calculating Fitness");
			CompareLog2ProcessTreePlugin fitnessCalculator= new CompareLog2ProcessTreePlugin();
			ProjectedRecallPrecisionResult iMFitnessResult=fitnessCalculator.measure(context, log, iMProcessTree);
			IMiPrecisionBefore=IMiPrecisionBefore + Double.toString(iMFitnessResult.getPrecision())+", ";
			IMiRecallBefore=IMiRecallBefore+ Double.toString(iMFitnessResult.getRecall())+", ";
			
			//IMi
			System.out.println("\t\t PreProcessing");
			
			long preProcessingStart = System.currentTimeMillis();
			XLog newLog=statisticalPreprocess(log);
			long preProcessingEnd = System.currentTimeMillis();
			
			
			System.out.println("\t\t IMi - Mining");
			long IMStart = System.currentTimeMillis();
			double IMStartMemory = (Runtime.getRuntime().totalMemory() -  Runtime.getRuntime().freeMemory())/ 1024d / 1024d;
			ProcessTree processTree = mineProcessTree(context, newLog, parameters);
			double IMEndMemory = (Runtime.getRuntime().totalMemory() -  Runtime.getRuntime().freeMemory())/ 1024d / 1024d;
			long IMEnd = System.currentTimeMillis();
			System.out.println("\t\t IMi -Calculating Fitness");
			fitnessCalculator= new CompareLog2ProcessTreePlugin();
			ProjectedRecallPrecisionResult IMiFitnessResult=fitnessCalculator.measure(context, log, processTree);
			
			fitnessCalculator= new CompareLog2ProcessTreePlugin();
			ProjectedRecallPrecisionResult IMiFitnessResultPaper=fitnessCalculator.measure(context, newLog, processTree);


			long preProcessingTime=preProcessingEnd-preProcessingStart;
			long IMTime=IMEnd-IMStart;
			long baseTime=baseEnd-baseStart;
			double  IMMemory = IMEndMemory-IMStartMemory;

			IMiPrecision=IMiPrecision+Double.toString(IMiFitnessResult.getPrecision())+", ";
			IMiRecall=IMiRecall+Double.toString(IMiFitnessResult.getRecall())+", ";
			
			IMiPrecisionPaper=IMiPrecisionPaper+Double.toString(IMiFitnessResultPaper.getPrecision())+", ";
			IMiRecallPaper=IMiRecallPaper+Double.toString(IMiFitnessResultPaper.getRecall())+", ";
			
			
			IMpreProcessingTimes=IMpreProcessingTimes+preProcessingTime+", ";
			IMtotalTimes=IMtotalTimes+IMTime+", ";
			IMTotalMemoryConsumption=IMTotalMemoryConsumption+IMMemory+", ";
			tracesLeft=tracesLeft+newLog.size()+", ";
			
			IMBaseTime = IMBaseTime + baseTime +", ";
			
			PrintWriter writer = new PrintWriter(filename + epsilonInMinutes + k  + ".txt", "UTF-8");
		    writer.println("===Runtime analysis===");
		    writer.println(IMtotalTimes);
		    writer.println(IMpreProcessingTimes);
		    writer.println(IMBaseTime);
		    writer.println("===Trace analysis===");
		    writer.println(totalTraces);
		    writer.println(tracesLeft);
		    writer.println("===Memory analysis===");
		    writer.println(IMTotalMemoryConsumption);
		    writer.println("===Conformance analysis===");
		    writer.println(IMiRecallBefore);
		    writer.println(IMiPrecisionBefore);
		    writer.println(IMiPrecision);
		    writer.println(IMiRecall);
		    writer.println(IMiPrecisionPaper);
		    writer.println(IMiRecallPaper);
		    writer.close();
		}
	return "Experiment finished without Errors";
	}
	
	
	protected XLog statisticalPreprocess(XLog log) throws Exception{
		Random generator = new Random(System.currentTimeMillis());
		PriorityQueue<Integer> pickedTraces=new PriorityQueue<>();
		
		//calculate the statistical threshold
		int threshold=calculateTraceThreshold(probabilityThreshold, confidenceLevel);
		
		//new empt log with same format as original one
		XLog newLog=new XLogImpl(log.getAttributes());
		
		//Information bases of our analysis criteria
		List<String> knownEdges=new ArrayList<>();
		List<String> knownEvents=new ArrayList<>();
		List<String> knownStartingEvents=new ArrayList<>();
		List<String> knownEndingEvents=new ArrayList<>();
		
		//for cycle time one information base for model based and one for event based analyzis is created
		EventTimeObject modelTime=new EventTimeObject(epsilonInMinutes);
		Map<String, EventTimeObject> eventTimeList=new HashMap<>();
		SimpleDateFormat dateFormat=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

		//Variables for the binomial experiment
		boolean newInformationGained=true;
		int tracesWithoutNewInformation=0;
	
		//pick a trace
		for(int i=log.size();i>0;i--){
			newInformationGained=false;
			int randomTrace=generator.nextInt(i);
			pickedTraces.add(randomTrace);
			XTrace currentXTrace=log.get(randomTrace);
			String currentEvent="";
			String priorEvent="";
			Date priorDate=null;
			Date currentDate=null;
			
			//update model cycle time
			if(!eventTimeAnalysis){
				Date modelStartingTime=dateFormat.parse(currentXTrace.get(0).getAttributes().get("time:timestamp").toString());
				Date modelEndingTime=dateFormat.parse(currentXTrace.get(currentXTrace.size()-1).getAttributes().get("time:timestamp").toString());
				double modelDifference=(modelEndingTime.getTime()-modelStartingTime.getTime());
				boolean didModelTimechange=modelTime.addNewEventTime(modelDifference);
				
				if(didModelTimechange){
					newInformationGained=true;
				}
			}
			
			//iterate over the events
			for(int j=0;j<currentXTrace.size();j++){
				priorEvent=currentEvent;
				priorDate=currentDate;
				XEvent currentXEvent=currentXTrace.get(j);
				currentEvent=currentXEvent.getAttributes().get("concept:name").toString();
				currentDate=dateFormat.parse(currentXEvent.getAttributes().get("time:timestamp").toString());
				
				//is the seen event new?
				if(!knownEvents.contains(currentEvent)){
					knownEvents.add(currentEvent);
					newInformationGained=true;
	
				}
				//is the event a new starting node in the df-graph?
				if(priorEvent.equals("") && !knownStartingEvents.contains(currentEvent)){
					knownStartingEvents.add(currentEvent);
					newInformationGained=true;
				}
				//is the event a new ending node in the df-graph?
				if(j==currentXTrace.size()-1 && !knownEndingEvents.contains(currentEvent)){
					knownEndingEvents.add(currentEvent);
					newInformationGained=true;
					
				}
				//is the seen edge new?
				if(!priorEvent.equals("") && !knownEdges.contains("("+priorEvent+","+currentEvent+")")){
					knownEdges.add("("+priorEvent+","+currentEvent+")");
					newInformationGained=true;
					
				}
				
				//has the event changed the average event time of the event?
				if(eventTimeAnalysis){
					if(j!=0){
						if (eventTimeList.containsKey(currentEvent)){
							double difference=(currentDate.getTime()-priorDate.getTime());
							boolean didTimechange=eventTimeList.get(currentEvent).addNewEventTime(difference);
							
							if(didTimechange){
								newInformationGained=true;
							}
						}
						else{
							EventTimeObject newEventTime=new EventTimeObject(epsilonInMinutes);
							eventTimeList.put(currentEvent, newEventTime);
							newInformationGained=true;
						}	
					}
					else{
						if (!eventTimeList.containsKey(currentEvent)){
							EventTimeObject newEventTime=new EventTimeObject(epsilonInMinutes);
							eventTimeList.put(currentEvent, newEventTime);
							newInformationGained=true;				
						}
					}
				}
			}
			
			//trace analysis finished - update counter accordingly
			if(!newInformationGained){
				tracesWithoutNewInformation++;
			}
			else{
				tracesWithoutNewInformation=0;
			}
			if(tracesWithoutNewInformation==threshold){
				while(!pickedTraces.isEmpty()){
					//build new log with same order of traces as in original log
					newLog.add(log.get(pickedTraces.poll()));
				}
				return newLog;
			}
		}
		//if hole log is traveresed just return original one
		return log;
	}

	
	
	//calculate the trace threshold for the experiment
	protected int calculateTraceThreshold(double threshold, double confidenceLevel) {
		ThresholdCalculator thresholdCalculator=new ThresholdCalculator(threshold, confidenceLevel);
		int thresholdHistory=thresholdCalculator.getThresholdCalculation();
		return thresholdHistory;
	}
	
	
	
	//taken from IM.java - mine a ProcessTree
	protected ProcessTree mineProcessTree(final UIPluginContext context, XLog log, MiningParameters parameters) {
		context.log("Mining...");
		return IMProcessTree.mineProcessTree(log, parameters, new Canceller() {
			public boolean isCancelled() {
				return context.getProgress().isCancelled();
			}
		});
	}

	
	
	//taken from IM.java - collects the mining parameters through a gui wizard
	protected MiningParameters getMiningParameters(final UIPluginContext context, XLog log){
		//copied from IM to get user input prior to time measurement
		IMMiningDialog dialog = new IMMiningDialog(log);
		MiningParameters parameters = dialog.getMiningParameters();
		InteractionResult result = context.showWizard("Mine using Inductive Miner", true, true, dialog);
		if (result != InteractionResult.FINISHED || !confirmLargeLogs(context, log, dialog)) {
			context.getFutureResult(0).cancel(false);
			return null;
		}
		return parameters;
	}
	
	
	//taken from IM.java - user has to confirm that he wants to mine the possibly large log
	protected boolean confirmLargeLogs(final UIPluginContext context, XLog log, IMMiningDialog dialog) {
		if (dialog.getVariant().getWarningThreshold() > 0) {
			XEventClassifier classifier = dialog.getMiningParameters().getClassifier();
			XLogInfo xLogInfo = XLogInfoFactory.createLogInfo(log, classifier);
			int numberOfActivities = xLogInfo.getEventClasses().size();
			if (numberOfActivities > dialog.getVariant().getWarningThreshold()) {
				int cResult = JOptionPane
						.showConfirmDialog(
								null,
								dialog.getVariant().toString()
										+ " might take a long time, as the event log contains "
										+ numberOfActivities
										+ " activities.\nThe chosen variant of Inductive Miner is exponential in the number of activities.\nAre you sure you want to continue?",
								"Inductive Miner might take a while", JOptionPane.YES_NO_OPTION);
	
				return cResult == JOptionPane.YES_OPTION;
			}
		}
		return true;
	}

}
