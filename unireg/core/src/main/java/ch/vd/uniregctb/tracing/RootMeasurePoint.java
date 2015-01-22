package ch.vd.uniregctb.tracing;


public class RootMeasurePoint extends MeasurePoint {

	public RootMeasurePoint(String description) {

		super(description);
	}

	public String toString() {
		Thread[] threads = new Thread[Thread.activeCount()];
		int nbThreads = Thread.enumerate(threads);
		boolean found = false;
		for (int i=0;i<nbThreads && !found;i++) {
			Thread t = threads[i];
			if (t.getName().equals(getDescription())) {
				found = true;
			}
		}
		String str = getDescription()+" Total: "+getNanoAsString(getChildrenTime());
		if (!found) {
			str += " (Finished)";
		}
		return str;
	}

}
