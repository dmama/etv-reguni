package ch.vd.moscow.database;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.moscow.MoscowTest;
import ch.vd.moscow.controller.graph.BreakdownCriterion;
import ch.vd.moscow.controller.graph.TimeResolution;
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

	@Test
	public void testBuildLoadStatsForQueryString() {
		// no criterion
		assertEquals("select sum(1) from calls where env_id = :env_id", DAOImpl.buildLoadStatsForQueryString(null, null, null, null));

		// date criterion
		assertEquals("select sum(1) from calls where env_id = :env_id and date >= :from", DAOImpl.buildLoadStatsForQueryString(null, null, date(2011, 1, 1), null));
		assertEquals("select sum(1) from calls where env_id = :env_id and date <= :to", DAOImpl.buildLoadStatsForQueryString(null, null, null, date(2011, 1, 1)));

		// time resolution
		assertEquals("select sum(1), date_trunc('day', date) from calls where env_id = :env_id group by date_trunc('day', date)",
				DAOImpl.buildLoadStatsForQueryString(null, TimeResolution.DAY, null, null));
		assertEquals("select sum(1), date_trunc('hour', date) from calls where env_id = :env_id group by date_trunc('hour', date)",
				DAOImpl.buildLoadStatsForQueryString(null, TimeResolution.HOUR, null, null));
		assertEquals("select sum(1), date_trunc('minute', date) from calls where env_id = :env_id group by date_trunc('minute', date)",
				DAOImpl.buildLoadStatsForQueryString(null, TimeResolution.FIFTEEN_MINUTES, null, null));

		// breakdown
		assertEquals("select sum(1) from calls where env_id = :env_id", DAOImpl.buildLoadStatsForQueryString(new BreakdownCriterion[]{}, null, null, null));
		assertEquals("select sum(1), caller from calls where env_id = :env_id group by caller",
				DAOImpl.buildLoadStatsForQueryString(new BreakdownCriterion[]{BreakdownCriterion.CALLER}, null, null, null));
		assertEquals("select sum(1), caller, env from calls where env_id = :env_id group by caller, env",
				DAOImpl.buildLoadStatsForQueryString(new BreakdownCriterion[]{BreakdownCriterion.CALLER, BreakdownCriterion.ENVIRONMENT}, null, null, null));

		// all criteria
		assertEquals("select sum(1), caller, date_trunc('hour', date) from calls where env_id = :env_id and date >= :from and date <= :to group by caller, date_trunc('hour', date)",
				DAOImpl.buildLoadStatsForQueryString(new BreakdownCriterion[]{BreakdownCriterion.CALLER}, TimeResolution.HOUR, date(2011, 1, 1), date(2011, 1, 2)));
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
