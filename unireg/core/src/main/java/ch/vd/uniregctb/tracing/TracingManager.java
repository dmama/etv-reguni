package ch.vd.uniregctb.tracing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.springframework.util.Assert;

public class TracingManager {

	public static final Logger LOGGER = Logger.getLogger(TracingManager.class);

	private static boolean isActive = false;

	// private static boolean isActive = true;
	private static HashMap<Long, Stack<TracePoint>> threadStacks = new HashMap<Long, Stack<TracePoint>>();

	private static HashMap<String, RootMeasurePoint> measures = new HashMap<String, RootMeasurePoint>();

	public static TracePoint begin() {
		return beginWithPrefix(""); // Pas de prefix par défaut
	}

	public static TracePoint begin(String prefix) {
		return beginWithPrefix(prefix + ": ");
	}

	private static synchronized TracePoint beginWithPrefix(String prefix) {

		TracePoint tp = null;
		if (isActive) {
			// Recupere le CallStack
			StackTraceElement[] elems = Thread.currentThread().getStackTrace();
			StackTraceElement lastCall = elems[4];

			String cn = lastCall.getClassName();
			int lastPoint = cn.lastIndexOf('.');
			cn = cn.substring(lastPoint + 1, cn.length());
			String description = prefix + cn + "." + lastCall.getMethodName();

			// Create and add it to stack
			TracePoint parent = TracingManager.getLastTracePoint();

			MeasurePoint parentMeasure = parent.getMeasure();
			MeasurePoint measure = parentMeasure.getOrCreateChild(description);

			tp = new TracePoint(parent, measure, description);
			pushPoint(TracingManager.getStackForThread(), tp);
		}
		return tp;
	}

	public static synchronized void reset() {

		threadStacks = new HashMap<Long, Stack<TracePoint>>();
		measures = new HashMap<String, RootMeasurePoint>();
	}

	private static void pushPoint(Stack<TracePoint> points, TracePoint tp) {
		points.push(tp);
		//long threadId = Thread.currentThread().getId();
		//LOGGER.debug("Pushing point ("+threadId+"): "+tp.getMeasure().getDescription());
		//threadId = -1;
	}

	private static TracePoint popPoint(Stack<TracePoint> points) {
		TracePoint tp = points.pop();
		//long threadId = Thread.currentThread().getId();
		//LOGGER.debug("Poping point ("+threadId+"): "+tp.getMeasure().getDescription());
		return tp;
	}

	public static synchronized void cleanUntil(TracePoint tp) {

		if (isActive) {

			Stack<TracePoint> points = getStackForThread();

			// Remove from Stack while it's not the given point
			while (points.peek() != tp) {
				TracePoint last = popPoint(points);
				endPoint(last);
			}
		}
	}

	public static synchronized void end(TracePoint tp) {

		if (isActive) {

			endPoint(tp);

			Stack<TracePoint> points = getStackForThread();

			// Remove from Stack
			TracePoint last = popPoint(points);
			if (last != tp) {
				long threadId = Thread.currentThread().getId();
				LOGGER.error("Thread: "+threadId+" Last: "+last.getMeasure().getDescription()+" Current: "+tp.getMeasure().getDescription());
			}
			Assert.isTrue(last == tp);

			Assert.isTrue(points.peek() == tp.getParent());
		}
	}

	public static synchronized List<String> getMeasuresAsStringList() {

		return getMeasuresAsStringList("\t");
	}

	@SuppressWarnings("unchecked")
	public static synchronized List<String> getMeasuresAsStringList(String tab) {

		List<String> list = new ArrayList<String>();
		if (isActive) {

			fillListWithMeasures(list, (HashMap) measures, "", tab);
		}
		return list;
	}

	public static synchronized void outputMeasures(Logger LOGGER) {

		if (isActive) {
			long begin = System.nanoTime();

			List<String> list = getMeasuresAsStringList();
			for (String s : list) {
				LOGGER.info(s);
			}

			long diff = (System.nanoTime() - begin) / 1000000;
			LOGGER.info("It took " + diff + "[ms] to log all measures");
		} else {
			//LOGGER.warn("The Tracing infrastructure is disabled");
		}
	}

	private static void fillListWithMeasures(List<String> list, HashMap<String, MeasurePoint> points, String tabs, String tab) {

		Collection<MeasurePoint> values = points.values();

		// Sort the entries
		List<MeasurePoint> sortedValues = new ArrayList<MeasurePoint>(values);
		Collections.sort(sortedValues);

		/*
		 * { LOGGER.debug("Nb elems: "+values.size());
		 *
		 * Iterator<MeasurePoint> iter = sortedValues.iterator(); while
		 * (iter.hasNext()) { MeasurePoint p = iter.next(); LOGGER.debug("Elem:
		 * "+p.getDescription()+" / "+(p.getCallsTime()/1000000)); } iter =
		 * null; // For BP }
		 */

		// Create the strings
		for (MeasurePoint p : sortedValues) {
			list.add(tabs + p.toString());

			fillListWithMeasures(list, p.getChildren(), tabs + tab, tab);
		}
	}

	private static void endPoint(TracePoint tp) {
		tp.end();
		addMeasure(tp);
	}

	private static void addMeasure(TracePoint tp) {

		MeasurePoint point = tp.getMeasure();
		point.addCall(tp.getDiffTime());
	}

	private static Stack<TracePoint> getStackForThread() {

		long threadId = Thread.currentThread().getId();
		Stack<TracePoint> points = threadStacks.get(threadId);
		if (points == null) {
			points = new Stack<TracePoint>();
			threadStacks.put(threadId, points);
		}
		return points;
	}

	private static TracePoint getLastTracePoint() {

		TracePoint tp = null;

		Stack<TracePoint> points = getStackForThread();
		if (points.size() > 0) {
			tp = points.peek();
		} else {
			String description = Thread.currentThread().getName();

			RootMeasurePoint measure = new RootMeasurePoint(description);
			measure.addCall(0);
			measures.put(description, measure);

			tp = new RootTracePoint(measure, description);
			pushPoint(points, tp);
		}
		return tp;
	}

	public static synchronized boolean isActive() {
		return isActive;
	}

	public static synchronized void setActive(boolean isActive) {
		TracingManager.isActive = isActive;

		// On reset seulement si on remet le falg à active
		if (isActive) {
			reset();
		}
	}

	private static double lastCall = 0;

	public static void logWithNano(String message) {

		long nano = System.nanoTime();
		double millis = nano / 1000000.0;
		double diff = millis - lastCall;
		String milliss = String.format("%.3f (%.3f): ", millis, diff);
		String str = milliss + message;
		LOGGER.debug(str);

		lastCall = millis;
	}

}
