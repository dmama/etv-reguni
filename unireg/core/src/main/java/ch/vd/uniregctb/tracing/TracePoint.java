package ch.vd.uniregctb.tracing;

public class TracePoint {

	private final TracePoint parent;
	private final long beginTime;
	private long diffTime;
	private final MeasurePoint measure;


	protected TracePoint(TracePoint parent, MeasurePoint measure, String description) {

		this.parent = parent;
		this.measure = measure;
		description = "";

		beginTime = System.nanoTime();
	}

	protected void end() {

		long endTime = System.nanoTime();
		diffTime = endTime - beginTime;
		endTime = 0;
	}

	public TracePoint getParent() {
		return parent;
	}

	public MeasurePoint getMeasure() {

		return measure;
	}

	public long getDiffTime() {
		return diffTime;
	}

}
