package ch.vd.unireg.transaction.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.transaction.SimpleService1;

@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
public class SimpleService1Impl extends SimpleServiceImpl implements SimpleService1 {

	private static Logger LOGGER = LoggerFactory.getLogger(SimpleService1Impl.class);

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Throwable.class)
	public void insertLineRequiresNew(int id, String msg) {

		LOGGER.info("insertLine("+id+", "+msg+ ')');
		dao.insertData(id, msg);
		//readLine(id);
	}

	@Override
	public void updateLineException(int id, String msg) {
		LOGGER.info("updateLineException("+id+", "+msg+ ')');
		dao.updateData(id, msg);
		throw new RuntimeException();
	}

}
