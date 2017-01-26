package org.processmining.martinbauer.plugins;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.plugins.InductiveMiner.mining.MiningParameters;
import org.processmining.plugins.InductiveMiner.plugins.IMProcessTree;
import org.processmining.plugins.InductiveMiner.plugins.dialogs.IMMiningDialog;
import org.processmining.processtree.ProcessTree;

public class StatisticalInductiveMiner{
	@Plugin(name = "Mine Process Tree with statistical Inductive Miner, with Analysis", returnLabels = { "Process Tree" }, returnTypes = { ProcessTree.class }, parameterLabels = { "Log" }, userAccessible = true)
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
	
	
	
	@Plugin(name = "Analyse statistical Inductive Miner", returnLabels = { "Process Tree" }, returnTypes = { String.class }, parameterLabels = { "Log" }, userAccessible = true)
	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Martin Bauer", email = "bauermax@informatik.hu-berlin.de")
	@PluginVariant(variantLabel = "Mine a Process Tree, dialog", requiredParameterLabels = { 0 })
	public String ExecuteAnalysis(final UIPluginContext context, XLog log) throws Exception {
		String sIMpreProcessingTimes="sIM - preProcessingTime:  ";
		String sIMminingTimes="sIM - MiningTime:         ";
		String sIMtotalTimes="sIM - TotalTime:          ";
		//String sIMpreProcessingMemoryConsumption="sIM - preProcessingMemory:";
		//String sIMminingMemoryConsumption="sIM - MiningMemory:       ";
		String sIMtotalMemoryConsumption="sIM - TotalMemory:        ";
		String sIMTraces="sIM - Traces:             ";
		String IMtotalTimes=" IM - Total:              ";
		String IMTotalMemoryConsumption=" IM - Memory             ";
		//get Mining Parameters
		MiningParameters parameters= getMiningParameters(context, log);
		//i times
		long filename = System.currentTimeMillis()*1000*60*60*24;
		System.out.println("Starting sIM Analysis - Results will be written to"+Long.toString(filename)+".txt");
		for(int i=0;i<100;i++){
			System.out.println("Experiment "+i+" of "+100);
			System.out.println("\t Scrambling Log");
			log=scrambleLog(log);
			//sIM
			System.out.println("\t sIM - Begin PreProcessing");
			long preProcessingStart = System.currentTimeMillis();
			double sIMStartMemory = (Runtime.getRuntime().totalMemory() -  Runtime.getRuntime().freeMemory())/ 1024d / 1024d;
			XLog newLog=statisticalPreprocess(log);
			long preProcessingEnd = System.currentTimeMillis();
			
			System.out.println("\t sIM - Begin Mining");
			long sIMStart = System.currentTimeMillis();
			ProcessTree statisticalProcessTree = mineStatisticalProcessTreeWithParameters(context, log, parameters);
			double sIMEndMemory = (Runtime.getRuntime().totalMemory() -  Runtime.getRuntime().freeMemory())/ 1024d / 1024d;
			long sIMEnd = System.currentTimeMillis();
			
			//IM
			
			System.out.println("\t IM - Begin Mining");
			long IMStart = System.currentTimeMillis();
			double IMStartMemory = (Runtime.getRuntime().totalMemory() -  Runtime.getRuntime().freeMemory())/ 1024d / 1024d;
			ProcessTree processTree = mineProcessTree(context, log, parameters);
			double IMEndMemory = (Runtime.getRuntime().totalMemory() -  Runtime.getRuntime().freeMemory())/ 1024d / 1024d;
			long IMEnd = System.currentTimeMillis();
			
			long preProcessingTime=preProcessingEnd-preProcessingStart;
			long sIMMiningTime=sIMEnd-sIMStart;
			long sIMTime=preProcessingTime+sIMMiningTime;
			long IMTime=IMEnd-IMStart;
			double  sIMMemory = sIMEndMemory-sIMStartMemory;
			double  IMMemory = IMEndMemory-IMStartMemory;

			
			sIMpreProcessingTimes=sIMpreProcessingTimes+preProcessingTime+",";
			sIMminingTimes=sIMminingTimes+sIMMiningTime+",";
			sIMtotalTimes=sIMtotalTimes+sIMTime+",";
			IMtotalTimes=IMtotalTimes+IMTime+",";
			sIMtotalMemoryConsumption=sIMtotalMemoryConsumption+sIMMemory+",";
			IMTotalMemoryConsumption=IMTotalMemoryConsumption+IMMemory+",";

			
			
			sIMTraces=sIMTraces+newLog.size()+",";
		}
		try{
		    PrintWriter writer = new PrintWriter(Long.toString(filename), "UTF-8");
		    writer.println(sIMpreProcessingTimes);
		    writer.println(sIMminingTimes);
		    writer.println(sIMtotalTimes+"\n");
		    writer.println(IMtotalTimes+"\n");
		    writer.println(sIMTraces+"\n");
		    writer.println(sIMtotalMemoryConsumption);
		    writer.println(IMTotalMemoryConsumption);
		    writer.close();
		} catch (IOException e) {
		   // do something
		}
		return "Experiment finished without Errors";
	}
	
	
	protected XLog statisticalPreprocess(XLog log) throws Exception{
		//log=scrambleLog(log);
		boolean restrictiveTimeAnalysis=true;
		double deltaModelAverageTimeTresholdInMinutes=5;
		double deltaEventAverageTimeTresholdInMinutes=0.5;

		double probabilityThreshold=0.05;
		double confidenceLevel=0.99;
		
		
		int threshold=calculateTraceThreshold(probabilityThreshold, confidenceLevel);
		System.out.println(threshold);
		//Log and Monitoring variables
		XLog newLog=new XLogImpl(log.getAttributes());
		
		//Model and Time Analysis variables
		List<String> knownEdges=new ArrayList<>();
		List<String> knownEvents=new ArrayList<>();
		List<String> knownStartingEvents=new ArrayList<>();
		List<String> knownEndingEvents=new ArrayList<>();
		EventTimeObject modelTime=new EventTimeObject(deltaModelAverageTimeTresholdInMinutes);
		Map<String, EventTimeObject> eventTimeList=new HashMap<>();
		SimpleDateFormat dateFormat=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		
		//Variables for the experimental Chain
		boolean newInformationGained=true;
		int tracesWithoutNewInformation=0;
	
		for(int i=log.size();i>0;i--){
			newInformationGained=false;
			int randomTrace=(int)Math.random()*i;
			XTrace currentXTrace=log.get(randomTrace);
			String currentEvent="";
			String priorEvent="";
			Date priorDate=null;
			Date currentDate=null;
			
			//update model cycle time
			Date modelStartingTime=dateFormat.parse(currentXTrace.get(0).getAttributes().get("time:timestamp").toString());
			Date modelEndingTime=dateFormat.parse(currentXTrace.get(currentXTrace.size()-1).getAttributes().get("time:timestamp").toString());
			double modelDifference=(modelEndingTime.getTime()-modelStartingTime.getTime())/1000.0/60.0/60.0/24.0;
			boolean didModelTimechange=modelTime.addNewEventTime(modelDifference);
			if(didModelTimechange){
				newInformationGained=true;
			}
	
			for(int j=0;j<currentXTrace.size();j++){
				priorEvent=currentEvent;
				priorDate=currentDate;
				XEvent currentXEvent=currentXTrace.get(j);
				currentEvent=currentXEvent.getAttributes().get("concept:name").toString();
				currentDate=dateFormat.parse(currentXEvent.getAttributes().get("time:timestamp").toString());
				
				//test if the trace contains new information
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
				//has the event changed the average event time of the event? Only on restrictive analysis 
				if(restrictiveTimeAnalysis){
					//are both if's really needed?
					if(j!=0){
						if (eventTimeList.containsKey(currentEvent)){
							double difference=(currentDate.getTime()-priorDate.getTime());
							boolean didTimechange=eventTimeList.get(currentEvent).addNewEventTime(difference);
							if(didTimechange){
								newInformationGained=true;
							}
						}
						else{
							EventTimeObject newEventTime=new EventTimeObject(deltaEventAverageTimeTresholdInMinutes);
							eventTimeList.put(currentEvent, newEventTime);
							newInformationGained=true;
						}	
					}
					else{
						if (!eventTimeList.containsKey(currentEvent)){
							EventTimeObject newEventTime=new EventTimeObject(deltaEventAverageTimeTresholdInMinutes);
							eventTimeList.put(currentEvent, newEventTime);
							newInformationGained=true;				
						}
					}
				}
			}
			newLog.add(currentXTrace);
			if(!newInformationGained){
				tracesWithoutNewInformation++;
			}
			else{
				tracesWithoutNewInformation=0;
			}
			if(tracesWithoutNewInformation==threshold){
				System.out.println(+" / "+log.size());
				return newLog;
			}
		}
		return newLog;
	}
	
	
	public ProcessTree mineStatisticalProcessTreeWithParameters(final UIPluginContext context, XLog log, MiningParameters parameters) throws Exception {
			//copied from IM to get user input prior to time measurement	
			XLog newLog = statisticalPreprocess(log);
			ProcessTree processTree = mineProcessTree(context, newLog, parameters);
			return processTree;
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
	
	
	//calculate the trace threshold for the experiment
	protected int calculateTraceThreshold(double threshold, double confidenceLevel) {
		ThresholdCalculator thresholdCalculator=new ThresholdCalculator(threshold, confidenceLevel);
		int thresholdHistory=thresholdCalculator.getThresholdCalculation();
		return thresholdHistory;
	}
	
	
	//scrambles the Log
	protected XLog scrambleLog(XLog log){
		XLog scrambledLog=new XLogImpl(log.getAttributes());
		for(int i=0;i<log.size();){
			int index=(int) (Math.random()*log.size());
			scrambledLog.add(log.remove(index));
		}
		return scrambledLog;
	}
	
	
	//taken from IM - collects the mining parameters through a wizard
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
	
	
	//taken from IM - user has to confirm that he wants to mine the possibly large log
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
