package ch.vd.moscow.database;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.moscow.MoscowTest;
import ch.vd.moscow.data.Call;
import ch.vd.moscow.data.CompletionStatus;
import ch.vd.moscow.data.Environment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DAOTest extends MoscowTest {

	private DAO dao;

	@Before
	public void setup() {
		dao = context.getBean("dao", DAO.class);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSetGetUpToDate() throws Exception {

		dao.clearUpToStatus();
		final Environment env = getDevEnv();
		assertNull(dao.getCompletionStatus(env));

		final Date ts = Call.parseTimestamp("2011-12-06 00:00:13.993");
		dao.setCompletionStatus(env, ts);

		final CompletionStatus status = dao.getCompletionStatus(env);
		assertNotNull(status);
		assertEquals(ts, status.getUpTo());
	}

	private Environment getDevEnv() {
		Environment env = dao.getEnvironment("dev");
		if (env == null) {
			env = new Environment("dev");
			env = dao.saveEnvironment(env);
		}
		return env;
	}
}
