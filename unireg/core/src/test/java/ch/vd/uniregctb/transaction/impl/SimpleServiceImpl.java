package ch.vd.uniregctb.transaction.impl;

import org.apache.log4j.Logger;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.transaction.dao.SimpleDao;

@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
public class SimpleServiceImpl {

	private static Logger LOGGER = Logger.getLogger(SimpleService1Impl.class);

	protected SimpleDao dao = null;

	public void setDao(SimpleDao dao) {
		this.dao = dao;
	}

	public void insertLine(int id, String msg) {
		LOGGER.info("insertLine("+id+", "+msg+")");

		dao.insertData(id, msg);
	}
	@Transactional(propagation = Propagation.MANDATORY, rollbackFor = Throwable.class)
	public void insertLineMandatory(int id, String msg) {
		LOGGER.info("insertLineMandatory("+id+", "+msg+")");

		dao.insertData(id, msg);
	}
	public void insertLineException(int id, String msg) {

		LOGGER.info("insertLineException("+id+", "+msg+")");

		dao.insertData(id, msg);
		throw new RuntimeException();
	}

	public void updateLine(int id, String msg) {
		LOGGER.info("updateLine("+id+", "+msg+")");

		dao.updateData(id, msg);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public String readLine(int id) {

		String str = null;
		try {
			str = dao.getData(id);
		}
		catch (EmptyResultDataAccessException e) {
		}
		LOGGER.info("readLine("+id+") => "+str);
		return str;
	}

}
