package ch.vd.uniregctb.tracing;

import java.util.HashMap;
import java.util.Set;

public class MeasurePoint implements Comparable<MeasurePoint> {
	
	private final HashMap<String, MeasurePoint> children = new HashMap<String, MeasurePoint>();
	
	private final String description;
	private int nbCalls = 0;
	private long callsTime = 0;

	public MeasurePoint(String descr) {

		description = descr;
	}
	
	public void addCall(long time) {
		
		callsTime += time;
		nbCalls++;
	}
	
	public HashMap<String, MeasurePoint> getChildren() {

		return children;
	}
	
	public MeasurePoint getOrCreateChild(String description) {
		
		MeasurePoint p = null;
		if (children.containsKey(description)) {
			p = children.get(description);
		}
		else {
			p = new MeasurePoint(description);
			children.put(description, p);
		}
		return p;
	}
	
	protected long getChildrenTime() {
		
		long total = 0;
		Set<String> keys = children.keySet();
		for (String descr : keys) {
			MeasurePoint child = children.get(descr);
			total += child.getCallsTime();
		}
		return total;
	}	
	
	public String toString() {
		
		String str = ""; 
		
		long avg = 0;
		if (nbCalls > 0) {
			avg = (callsTime / nbCalls);
			long childrenTime = getChildrenTime();
			long self = callsTime - childrenTime;
			str = description+" : Calls:"+nbCalls+" Total: "+getNanoAsString(callsTime)+" Self: "+getNanoAsString(self)+" Children: "+getNanoAsString(childrenTime)+" Avg: "+getNanoAsString(avg);
		}
		else {
			str = description+" : In progress... Children: "+getNanoAsString(getChildrenTime());
		}
		return str;
	}
	
	protected static String getNanoAsString(long nano) {
		
		String str = "0";
		if (nano > 0) {
			long micro = nano / 1000;
			
			if (micro > 1000000) { // > 1s
				
				double m = micro / 1000000.0;
				str = String.format("%.2fs", m);
			}
			else if (micro > 1000) { // > 1ms
				
				double m = micro / 1000.0;
				str = String.format("%.2fms", m);
			}
			else {
	
				str = Long.toString(micro)+"us";
			}
		}
		return str;
	}

	public String getDescription() {
		return description;
	}

	public long getCallsTime() {
		if (nbCalls > 0) {
			return callsTime;
		}
		else {
			return getChildrenTime();			
		}
	}

	public int compareTo(MeasurePoint other) {
		// Compares as milliseconds, else we loose too much when truncating to int!
		return (int)((other.getCallsTime() - getCallsTime()) / 1000000);
	}
	
}
