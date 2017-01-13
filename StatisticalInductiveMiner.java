package org.processmining.martinbauer.plugins;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
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
public ProcessTree mineGuiProcessTree(final UIPluginContext context, XLog log) {
	try{
	    PrintWriter writer = new PrintWriter("traceView.txt", "UTF-8");

		int threshold=calculateTraceThreshold(context, log);
		
		//initialize the data for the preprocessing
		XLog newLog=new XLogImpl(log.getAttributes());
		//newLog.removeAll(newLog);
		String traces="";
		List<String> knownEdges=new ArrayList<>();
		List<String> knownEvents=new ArrayList<>();
		List<String> knownStartingEvents=new ArrayList<>();
		List<String> knownEndingEvents=new ArrayList<>();
		Map<String, String> averageEventTimes=new HashMap<>();
		double averageModelTime=0;
		int edgeCounter=0;
		int eventCounter=0;
		double deltaAverageTimeThresholdInSeconds=1;
		
		boolean newInformationGained=false;
		int tracesWithoutNewInformation=0;
	
		//for each Trace in Log
		for(int i=0;i<log.size();i++){
			newInformationGained=false;

			XTrace currentXTrace=log.get(i);
			String currentEvent="";
			String priorEvent="";
			
			//for each event in Log
			for(int j=0;j<currentXTrace.size();j++){
				eventCounter++;
				priorEvent=currentEvent;
				XEvent currentXEvent=currentXTrace.get(j);
				currentEvent=currentXEvent.getAttributes().get("concept:name").toString();
				
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
				
				if(!priorEvent.equals("")){
					edgeCounter++;
				}
				
				traces=traces+currentXEvent.getAttributes().get("concept:name").toString()+", ";
			}
			
			traces=traces.substring(0,(traces.length()-2))+"\n";
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
				writer.println("Calculated trace threshold: "+threshold);
				writer.println((newLog.size()) +" Traces collected (from "+log.size()+" Traces in total)");
				writer.println("\nTraces observed in the Log:");
				writer.println(traces);
				writer.close();
				IM inductiveMiner= new IM();
				return inductiveMiner.mineGuiProcessTree(context, newLog);
			}
		}
		//if whole log has been traversed call IM with original Log, no gain through sIM possible
		writer.println("Calculated trace threshold: "+threshold);
		writer.println((newLog.size()) +" Traces collected (from "+log.size()+" Traces in total)");
		writer.println("\nTraces observed in the Log:");
		writer.println(traces);
		writer.close();
		IM inductiveMiner= new IM();
		return inductiveMiner.mineGuiProcessTree(context, log);

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
