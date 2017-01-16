package org.processmining.martinbauer.plugins;

public class EventTimeObject {
	String eventName;
	double appearances;
	double avgEventTime;
	double epsilon;
	
	EventTimeObject(double epsilon){
		appearances=0;
		avgEventTime=0;
		this.epsilon=epsilon;
		
	}
	
	public boolean addNewEventTime(double time){
		double oldEventTime=this.avgEventTime;
		this.avgEventTime=this.avgEventTime*appearances;
		appearances++;
		this.avgEventTime=(this.avgEventTime+time)/appearances;
		if(Math.abs(oldEventTime-this.avgEventTime)>this.epsilon){
			return false;
		}
		return true;
	}
}
