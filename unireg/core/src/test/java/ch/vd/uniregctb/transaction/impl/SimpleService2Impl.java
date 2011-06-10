package ch.vd.uniregctb.transaction.impl;

import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.transaction.SimpleService1;
import ch.vd.uniregctb.transaction.SimpleService2;

public class SimpleService2Impl extends SimpleServiceImpl implements SimpleService2 {

	private static Logger LOGGER = Logger.getLogger(SimpleService2Impl.class);

	private SimpleService1 service1 = null;

	public void setService(SimpleService1 service) {
		this.service1 = service;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void insert2LinesException(int id1, String msg1, int id2, String msg2) {

		LOGGER.info("insert2LinesException("+id1+", "+msg1+", "+id2+", "+msg2+")");
		service1.insertLineRequiresNew(id1, msg1);
		insertLineException(id2, msg2);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void insertLineThatException(int id1, String msg1, int id2, String msg2) {
		LOGGER.info("insertLineThatException("+id1+", "+msg1+", "+id2+", "+msg2+")");

		insertLine(id1, msg1);
		service1.insertLineException(id2, msg2);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public String readLineThatInsert(int id, String msg) {

		insertLine(id, msg);
		String str = readLine(id);
		return str;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void insertLineCallMandatory(int id, String msg) {

		service1.insertLineMandatory(id, msg);
	}

}
