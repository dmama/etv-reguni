package ch.vd.uniregctb.checker;

public class MockServiceChecker implements ServiceChecker {
	public Status getStatus() {
		return Status.OK;
	}

	public String getStatusDetails() {
		return null;
	}
}
