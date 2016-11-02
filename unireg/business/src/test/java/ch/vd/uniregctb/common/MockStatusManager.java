package ch.vd.uniregctb.common;

import ch.vd.shared.batchtemplate.StatusManager;

public class MockStatusManager implements StatusManager {

	private String msg;
	private int percent;

	@Override
	public boolean interrupted() {
		return false;
	}

	@Override
	public void setMessage(String msg) {
		this.msg = msg;
	}

	@Override
	public void setMessage(String msg, int percentProgression) {
		this.msg = msg;
		this.percent = percentProgression;
	}

	public String getMsg() {
		return msg;
	}

	public int getPercent() {
		return percent;
	}
}
