package ch.vd.unireg.transaction;

public interface SimpleService1 extends SimpleService {

	public void insertLineRequiresNew(int id, String msg);

	public void updateLineException(int id, String msg);

}
