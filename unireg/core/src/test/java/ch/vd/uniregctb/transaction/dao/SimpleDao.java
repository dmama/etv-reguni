package ch.vd.uniregctb.transaction.dao;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

public class SimpleDao {

	private static Logger LOGGER = Logger.getLogger(SimpleDao.class);

	private JdbcTemplate jdbcTemplate = null;

	public void insertData(long index, String data) {
		jdbcTemplate.update("insert into test (id, data) values (?,?)", new Object[] { index, data });
	}

	public String getData(long index) {
		return (String) jdbcTemplate.queryForObject("select data from test where id = ?", new Object[] { index }, String.class);
	}

	public void updateData(long index, String data) {
		jdbcTemplate.update("update test set data = ? where id = ?", new Object[] { data, index });
	}

	public void deleteData(long index) {
		jdbcTemplate.update("delete from test where id = ?", new Object[] { index });
	}

	public void setDataSource(DataSource dataSource) {
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	public void createTable() {
		try {
			jdbcTemplate.execute("drop table test");
		}
		catch(Exception e) {
			// on ignore joyeusement
		}
		try {
			jdbcTemplate.execute("create table test (id int primary key, data varchar(100))");
			//jdbcTemplate.update("insert into test (id, data) values (1,'test1')");
			//jdbcTemplate.update("insert into test (id, data) values (2,'test2')");
		}
		catch(DataAccessException e) {
			LOGGER.error(e,e);
			throw e;
		}
	}

}
