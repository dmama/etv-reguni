package ch.vd.moscow.database;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.moscow.MoscowTest;
import ch.vd.moscow.controller.graph.CallDimension;
import ch.vd.moscow.controller.graph.Filter;
import ch.vd.moscow.controller.graph.TimeResolution;
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

		final Date ts = DatabaseServiceImpl.parseTimestamp("2011-12-06 00:00:13.993");
		dao.setCompletionStatus(env, ts);

		final CompletionStatus status = dao.getCompletionStatus(env);
		assertNotNull(status);
		assertEquals(ts, status.getUpTo());
	}

	@Test
	public void testBuildLoadStatsForQueryString() {
		// no criterion
		assertEquals("select sum(1) from calls",
				DAOImpl.buildLoadStatsForQueryString(null, null, null, null, null));

		// filters
		assertEquals("select sum(1) from calls",
				DAOImpl.buildLoadStatsForQueryString(new Filter[]{}, null, null, null, null));
		assertEquals("select sum(1) from calls where env_id = :filterValue0",
				DAOImpl.buildLoadStatsForQueryString(new Filter[]{new Filter(CallDimension.ENVIRONMENT, "1")}, null, null, null, null));
		assertEquals("select sum(1) from calls where caller_id = :filterValue0",
				DAOImpl.buildLoadStatsForQueryString(new Filter[]{new Filter(CallDimension.CALLER, "sipf")}, null, null, null, null));
		assertEquals("select sum(1) from calls where env_id = :filterValue0 and caller_id = :filterValue1",
				DAOImpl.buildLoadStatsForQueryString(new Filter[]{new Filter(CallDimension.ENVIRONMENT, "1"), new Filter(CallDimension.CALLER, "sipf")}, null, null, null, null));

		// date criterion
		assertEquals("select sum(1) from calls where date >= :from",
				DAOImpl.buildLoadStatsForQueryString(null, null, null, date(2011, 1, 1), null));
		assertEquals("select sum(1) from calls where date <= :to",
				DAOImpl.buildLoadStatsForQueryString(null, null, null, null, date(2011, 1, 1)));

		// time resolution
		assertEquals("select sum(1), date_trunc('day', date) from calls group by date_trunc('day', date)",
				DAOImpl.buildLoadStatsForQueryString(null, null, TimeResolution.DAY, null, null));
		assertEquals("select sum(1), date_trunc('hour', date) from calls group by date_trunc('hour', date)",
				DAOImpl.buildLoadStatsForQueryString(null, null, TimeResolution.HOUR, null, null));
		assertEquals("select sum(1), date_trunc('minute', date) from calls group by date_trunc('minute', date)",
				DAOImpl.buildLoadStatsForQueryString(null, null, TimeResolution.FIFTEEN_MINUTES, null, null));

		// breakdown
		assertEquals("select sum(1) from calls",
				DAOImpl.buildLoadStatsForQueryString(null, new CallDimension[]{}, null, null, null));
		assertEquals("select sum(1), caller_id from calls group by caller_id",
				DAOImpl.buildLoadStatsForQueryString(null, new CallDimension[]{CallDimension.CALLER}, null, null, null));
		assertEquals("select sum(1), caller_id, env_id from calls group by caller_id, env_id",
				DAOImpl.buildLoadStatsForQueryString(null, new CallDimension[]{CallDimension.CALLER, CallDimension.ENVIRONMENT}, null, null, null));

		// all criteria
		assertEquals("select sum(1), caller_id, date_trunc('hour', date) from calls where env_id = :filterValue0 and date >= :from and date <= :to group by caller_id, date_trunc('hour', date)",
				DAOImpl.buildLoadStatsForQueryString(new Filter[]{new Filter(CallDimension.ENVIRONMENT, "1")}, new CallDimension[]{CallDimension.CALLER}, TimeResolution.HOUR, date(2011, 1, 1),
						date(2011, 1, 2)));
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
