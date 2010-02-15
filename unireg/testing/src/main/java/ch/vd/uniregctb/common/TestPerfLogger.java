package ch.vd.uniregctb.common;

import org.apache.log4j.Logger;

public class TestPerfLogger {

	private static final Logger LOGGER = Logger.getLogger(TestPerfLogger.class);

	private static long firstCall = System.currentTimeMillis();
	private static long lastCall = firstCall;

	public static void logBegin(String clas, String method) {
		long now = System.currentTimeMillis();
		long diff1 = now-lastCall;
		long diff2 = now-firstCall;
		LOGGER.warn("TestPerf: begin ("+diff1+"/"+diff2+") "+clas+"."+method);
		lastCall = now;
	}
	public static void log(String clas, String method) {
		long now = System.currentTimeMillis();
		long diff1 = now-lastCall;
		long diff2 = now-firstCall;
		LOGGER.warn("TestPerf: ("+diff1+"/"+diff2+") "+clas+"."+method);
		lastCall = now;
	}
	public static void logEnd(String clas, String method) {
		long now = System.currentTimeMillis();
		long diff1 = now-lastCall;
		long diff2 = now-firstCall;
		LOGGER.warn("TestPerf: end ("+diff1+"/"+diff2+") "+clas+"."+method);
		lastCall = now;
	}
}
