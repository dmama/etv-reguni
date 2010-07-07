package ch.vd.uniregctb.transaction.impl;

import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.transaction.SimpleService1;

@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
public class SimpleService1Impl extends SimpleServiceImpl implements SimpleService1 {

	private static Logger LOGGER = Logger.getLogger(SimpleService1Impl.class);

	@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Throwable.class)
	public void insertLineRequiresNew(int id, String msg) {

		LOGGER.info("insertLine("+id+", "+msg+")");
		dao.insertData(id, msg);
		//readLine(id);
	}

	public void updateLineException(int id, String msg) {
		LOGGER.info("updateLineException("+id+", "+msg+")");
		dao.updateData(id, msg);
		throw new RuntimeException();
	}

}
