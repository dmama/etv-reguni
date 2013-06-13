package ch.vd.uniregctb.common;

public interface StatusManager {

	public boolean interrupted();
	public void setMessage(String msg);
	public void setMessage(String msg, int percentProgression);

}
