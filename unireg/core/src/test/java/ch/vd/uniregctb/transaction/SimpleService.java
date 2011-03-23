package ch.vd.uniregctb.transaction;

public interface SimpleService {

	public void insertLine(int id, String msg);
	public void insertLineException(int id, String msg);
	public void insertLineMandatory(int id, String msg);

	public void updateLine(int id, String msg);

	public String readLine(int id);

}
