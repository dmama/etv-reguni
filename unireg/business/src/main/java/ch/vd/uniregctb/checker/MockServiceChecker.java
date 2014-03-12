package ch.vd.uniregctb.checker;

public class MockServiceChecker implements ServiceChecker {
	@Override
	public Status getStatus() {
		return Status.OK;
	}

	@Override
	public String getStatusDetails() {
		return null;
	}
}
