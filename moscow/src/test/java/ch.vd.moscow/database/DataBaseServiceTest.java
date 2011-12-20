package ch.vd.moscow.database;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.moscow.MoscowTest;
import ch.vd.moscow.data.Call;
import ch.vd.moscow.data.Environment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DataBaseServiceTest extends MoscowTest {

	private DatabaseService service;
	private DAO dao;

	@Before
	public void setup() {
		service = context.getBean("databaseService", DatabaseService.class);
		dao = context.getBean("dao", DAO.class);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testImportOneFile() throws Exception {

		service.clearDb();

		final Environment env = getDevEnv();
		assertEquals(0, dao.getCalls(env).size());

		final String filename = getFilepath("ws-access-1.log");
		assertNotNull(filename);

		service.importLog(env, filename, null);

		final List<Call> calls = dao.getCalls(env);
		assertNotNull(calls);
		assertEquals(10, calls.size());
		assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 508L, calls.get(0));
		assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 0L, calls.get(1));
		assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 255L, calls.get(2));
		assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 0L, calls.get(3));
		assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 5739L, calls.get(4));
		assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 0L, calls.get(5));
		assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 271L, calls.get(6));
		assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 0L, calls.get(7));
		assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 136L, calls.get(8));
		assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 0L, calls.get(9));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testImportOneFileTwice() throws Exception {

		service.clearDb();

		final Environment env = getDevEnv();
		assertEquals(0, dao.getCalls(env).size());

		final String filename = getFilepath("ws-access-1.log");
		assertNotNull(filename);

		service.importLog(env, filename, null);
		{
			final List<Call> calls = dao.getCalls(env);
			assertNotNull(calls);
			assertEquals(10, calls.size());
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 508L, calls.get(0));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 0L, calls.get(1));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 255L, calls.get(2));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 0L, calls.get(3));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 5739L, calls.get(4));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 0L, calls.get(5));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 271L, calls.get(6));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 0L, calls.get(7));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 136L, calls.get(8));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 0L, calls.get(9));
		}

		// import again the same file
		service.importLog(env, filename, null);
		{
			// calls should not be imported twice
			final List<Call> calls = dao.getCalls(env);
			assertNotNull(calls);
			assertEquals(10, calls.size());
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 508L, calls.get(0));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 0L, calls.get(1));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 255L, calls.get(2));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 0L, calls.get(3));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 5739L, calls.get(4));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 0L, calls.get(5));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 271L, calls.get(6));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 0L, calls.get(7));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 136L, calls.get(8));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 0L, calls.get(9));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testImportTwoFiles() throws Exception {

		service.clearDb();
		
		final Environment env = getDevEnv();
		assertEquals(0, dao.getCalls(env).size());

		{
			final String filename = getFilepath("ws-access-1.log");
			assertNotNull(filename);

			service.importLog(env, filename, null);

			final List<Call> calls = dao.getCalls(env);
			assertNotNull(calls);
			assertEquals(10, calls.size());
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 508L, calls.get(0));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 0L, calls.get(1));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 255L, calls.get(2));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 0L, calls.get(3));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 5739L, calls.get(4));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 0L, calls.get(5));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 271L, calls.get(6));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 0L, calls.get(7));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 136L, calls.get(8));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 0L, calls.get(9));
		}

		// import a different file with 10 first calls identifical to previous file + 10 new calls
		{
			final String filename = getFilepath("ws-access-1-extended.log");
			assertNotNull(filename);

			service.importLog(env, filename, null);

			final List<Call> calls = dao.getCalls(env);
			assertNotNull(calls);
			assertEquals(20, calls.size());

			// identical calls shouldn't be duplicated
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 508L, calls.get(0));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 0L, calls.get(1));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 255L, calls.get(2));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 0L, calls.get(3));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 5739L, calls.get(4));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 0L, calls.get(5));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 271L, calls.get(6));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 0L, calls.get(7));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 136L, calls.get(8));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 0L, calls.get(9));

			// extended calls should be imported
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 262L, calls.get(10));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 0L, calls.get(11));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 286L, calls.get(12));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 0L, calls.get(13));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 379L, calls.get(14));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 0L, calls.get(15));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 138L, calls.get(16));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 0L, calls.get(17));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 396L, calls.get(18));
			assertCall("dev", "tiers2.read", "tao-ba", "GetTiersHisto", 0L, calls.get(19));
		}
	}

	private static void assertCall(String environment, String service, String user, String method, long latency, Call call) {
		assertNotNull(call);
		assertEquals(environment, call.getEnvironment().getName());
		assertEquals(service, call.getService());
		assertEquals(user, call.getCaller());
		assertEquals(method, call.getMethod());
		assertEquals(latency, call.getLatency());
	}

	private Environment getDevEnv() {
		Environment env = dao.getEnvironment("dev");
		if (env == null) {
			env = new Environment("dev");
			env = dao.saveEnvironment(env);
		}
		dao.flush();
		return env;
	}
}
