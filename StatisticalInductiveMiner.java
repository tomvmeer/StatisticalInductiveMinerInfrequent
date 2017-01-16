package org.processmining.martinbauer.plugins;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XLogImpl;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.plugins.InductiveMiner.mining.MiningParameters;
import org.processmining.plugins.InductiveMiner.plugins.IM;
import org.processmining.plugins.InductiveMiner.plugins.IMProcessTree;
import org.processmining.plugins.InductiveMiner.plugins.dialogs.IMMiningDialog;
import org.processmining.processtree.ProcessTree;



public class StatisticalInductiveMiner{

@Plugin(name = "Statistical Inductive Miner - calculate Trace Threshold", returnLabels = { "Trace Threshold" }, returnTypes = { int.class }, parameterLabels = {"Log"}, userAccessible = true)
@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Martin Bauer", email = "bauermax@informatik.hu-berlin.de")
@PluginVariant(variantLabel = "Calculate the trace threshold, dialog", requiredParameterLabels = { 0 })
//prints the trace threshold - use for debugging purposes
public int /*ProcessTree*/ calculateTraceThreshold(final UIPluginContext context, XLog log) {
	double threshold=0.05;
	double confidenceLevel=0.99;
	ThresholdCalculator thresholdCalculator=new ThresholdCalculator(threshold, confidenceLevel);
	int thresholdHistory=thresholdCalculator.getThresholdCalculation();
	return thresholdHistory;
}



//This is the Plugin that will be used for the algorithm
//It uses the IM-class as an outline.
//First it starts the PreProcessing of the Log and then call IM with the new Log
@Plugin(name = "Statistical Inductive Miner Test version", returnLabels = { "Process Tree" }, returnTypes = { ProcessTree.class }, parameterLabels = { "Log" }, userAccessible = true)
@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "Martin Bauer", email = "bauermax@informatik.hu-berlin.de")
@PluginVariant(variantLabel = "Mine a Process Tree, dialog", requiredParameterLabels = { 0 })
//extract the confidence out of the input parameter, make preprocessing, and call IMProcessTree with new
//Log and parameters without confidence
public ProcessTree mineGuiProcessTree(final UIPluginContext context, XLog log) throws ParseException {
	try{
		//Track time for own plugin, IM and whole Plugin
		long startTime = System.currentTimeMillis();
	    PrintWriter writer = new PrintWriter("traceView.txt", "UTF-8");

		int threshold=calculateTraceThreshold(context, log);
		
		//initialize the data for the preprocessing
		boolean restrictiveTimeAnalysis=true;
		
		double deltaAverageTimeThresholdInSeconds=1; //the threshold for average cycle time analysis
		
		//Log and Monitoring variables
		XLog newLog=new XLogImpl(log.getAttributes());
		String traces="";
		String experimentInformation="";
		
		//Model Analysis
		List<String> knownEdges=new ArrayList<>();
		List<String> knownEvents=new ArrayList<>();
		List<String> knownStartingEvents=new ArrayList<>();
		List<String> knownEndingEvents=new ArrayList<>();
		
		
		//Time Analysis
		EventTimeObject modelTime=new EventTimeObject(deltaAverageTimeThresholdInSeconds);
		Map<String, EventTimeObject> eventTimeList=new HashMap<>();
		SimpleDateFormat dateFormat=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		
		//experiment variables
		boolean newInformationGained=false;
		int tracesWithoutNewInformation=0;
	
		//for each Trace in Log
		for(int i=0;i<log.size();i++){
			newInformationGained=false;

			XTrace currentXTrace=log.get(i);
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
				if (!newInformationGained)experimentInformation+="New ModelTime at "+tracesWithoutNewInformation+"\n";
				newInformationGained=true;
			}
			
			
			//for each event in Log
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
					if (!newInformationGained)experimentInformation+="New Event at "+tracesWithoutNewInformation+"\n";
					newInformationGained=true;
					
				}
				//is the event a new starting node in the df-graph?
				if(priorEvent.equals("") && !knownStartingEvents.contains(currentEvent)){
					knownStartingEvents.add(currentEvent);
					if (!newInformationGained)experimentInformation+="New Start at "+tracesWithoutNewInformation+"\n";
					newInformationGained=true;
				}
				//is the event a new ending node in the df-graph?
				if(j==currentXTrace.size()-1 && !knownEndingEvents.contains(currentEvent)){
					knownEndingEvents.add(currentEvent);
					if (!newInformationGained)experimentInformation+="New End at "+tracesWithoutNewInformation+"\n";
					newInformationGained=true;
					
				}
				//is the seen edge new?
				if(!priorEvent.equals("") && !knownEdges.contains("("+priorEvent+","+currentEvent+")")){
					knownEdges.add("("+priorEvent+","+currentEvent+")");
					if (!newInformationGained)experimentInformation+="New Edge at "+tracesWithoutNewInformation+"\n";
					newInformationGained=true;
					
				}
				//has the event changed the average event time of the event? Only on restrictive analysis 
				if(restrictiveTimeAnalysis){
					if(j!=0){
						if (eventTimeList.containsKey(currentEvent)){
							double difference=(currentDate.getTime()-priorDate.getTime())/1000.0/60.0/60.0/24.0;
							boolean didTimechange=eventTimeList.get(currentEvent).addNewEventTime(difference);
							if(didTimechange){
								if (!newInformationGained)experimentInformation+="New EventTime at "+tracesWithoutNewInformation+"\n";
								newInformationGained=true;
							}
						}
						else{
							EventTimeObject newEventTime=new EventTimeObject(deltaAverageTimeThresholdInSeconds);
							eventTimeList.put(currentEvent, newEventTime);
							if (!newInformationGained)experimentInformation+="New EventTime at "+tracesWithoutNewInformation+"\n";
							newInformationGained=true;
						}	
					}
					else if(j==0){
						if (!eventTimeList.containsKey(currentEvent)){
							EventTimeObject newEventTime=new EventTimeObject(deltaAverageTimeThresholdInSeconds);
							eventTimeList.put(currentEvent, newEventTime);
							if (!newInformationGained)experimentInformation+="New EventTime at "+tracesWithoutNewInformation+"\n";
							newInformationGained=true;
							
						}
					}
				}
				
				traces=traces+currentXEvent.getAttributes().get("concept:name").toString()+", ";
			}
			
			traces=i+") "+traces.substring(0,(traces.length()-2))+"\n";
			newLog.add(currentXTrace);
			
			//if no new Information has been seen increment counter
			if(!newInformationGained){
				tracesWithoutNewInformation++;
			}
			//else restart experiment
			else{
				tracesWithoutNewInformation=0;
			}
			
			//if threshold has been reached call IM with new log
			if(tracesWithoutNewInformation==threshold){
				IM inductiveMiner= new IM();
				long preProcessingEndTime = System.currentTimeMillis();
				ProcessTree processTree= inductiveMiner.mineGuiProcessTree(context, newLog);
				long IMEndTime = System.currentTimeMillis();
				experimentInformation+="Threshold reached. Calling IM with new Log\n";
				
				writer.println("Calculated trace threshold: "+threshold);
				writer.println((newLog.size()) +"/"+log.size()+" Traces collected");
				writer.println("Time: "+(preProcessingEndTime-startTime)+"+"+(IMEndTime-preProcessingEndTime)+"="+(IMEndTime-startTime));
				writer.println("Memory: ");
				writer.println("\nExperiment History:");
				writer.println(experimentInformation);
				writer.println("\nTraces observed in the Log:");
				writer.println(traces);
				writer.close();
				
				return processTree;
			}
		}
		//if whole log has been traversed call IM with original Log, no gain through sIM possible
		IM inductiveMiner= new IM();
		long preProcessingEndTime = System.currentTimeMillis();
		ProcessTree processTree= inductiveMiner.mineGuiProcessTree(context, newLog);
		long IMEndTime = System.currentTimeMillis();
		experimentInformation+="Threshold not reached. Calling IM with original Log\n";
		
		writer.println("Calculated trace threshold: "+threshold);
		writer.println((newLog.size()) +"/"+log.size()+" Traces collected");
		writer.println("Time: "+((preProcessingEndTime-startTime)/1000.0)+"+"+((IMEndTime-preProcessingEndTime)/1000.0)+"="+((IMEndTime-startTime))/1000.0);
		writer.println("Memory: ");
		writer.println("\nExperiment History:");
		writer.println(experimentInformation);
		writer.println("\nTraces observed in the Log:");
		writer.println(traces);
		writer.close();
		
		return processTree;

	}		
	catch (IOException e) {
	   // do something
		return null;
	}
}
/*	IMMiningDialog dialog = new IMMiningDialog(log);
	InteractionResult result = context.showWizard("Mine using Statistical Inductive Miner", true, true, dialog);
	if (result != InteractionResult.FINISHED || !confirmLargeLogs(context, log, dialog)) {
		context.getFutureResult(0).cancel(false);
		return null;
	}

	context.log("Mining...");

	return IMProcessTree.mineProcessTree(log, dialog.getMiningParameters(), new Canceller() {
		public boolean isCancelled() {
			return context.getProgress().isCancelled();
		}
	});
}*/



@Plugin(name = "Statistical Inductive Miner, with parameters", returnLabels = { "Process tree" }, returnTypes = { ProcessTree.class }, parameterLabels = {
		"Log", "IM Parameters" }, userAccessible = false)
@PluginVariant(variantLabel = "Mine a Process Tree, parameters", requiredParameterLabels = { 0, 1 })
public static ProcessTree mineProcessTree(PluginContext context, XLog log, MiningParameters parameters) {
	context.log("Mining...");
	//extract the confidence out of the input parameter, make preprocessing, and call IMProcessTree with new
	//Log and parameters without confidence
	return IMProcessTree.mineProcessTree(log, parameters);
}

public boolean confirmLargeLogs(final UIPluginContext context, XLog log, IMMiningDialog dialog) {
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
