package ch.vd.uniregctb.transaction;

public interface SimpleService2 extends SimpleService {

	public void insert2LinesException(int id1, String msg1, int id2, String msg2);

	public void insertLineThatException(int id1, String msg1, int id2, String msg2);
	public void insertLineCallMandatory(int id, String msg);
	
	public String readLineThatInsert(int id, String msg);

}
