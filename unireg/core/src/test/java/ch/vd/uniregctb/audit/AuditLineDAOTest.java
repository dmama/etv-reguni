package ch.vd.uniregctb.audit;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.uniregctb.common.CoreDAOTest;

@ContextConfiguration(locations = {
		"classpath:ut/AuditLineDAOTest-spring.xml"
})
public class AuditLineDAOTest extends CoreDAOTest {

	private static Logger LOGGER = Logger.getLogger(AuditLineDAOTest.class);

	private AuditLineDAO auditLineDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		auditLineDAO = getBean(AuditLineDAO.class, "auditLineDAO");
	}

	@Test
	@NotTransactional
	public void testLogAuditLine() throws Exception {

		setAuthentication("UnitTest");
		Audit.info("bla");
		Audit.info("bli");
		resetAuthentication();

		List<AuditLine> list = auditLineDAO.findLastCountFromID(0, 2);
		assertEquals(2, list.size());
	}

	@Test
	@NotTransactional
	public void testLogAuditLineInNewTx() throws Exception {

		doInNewTransaction(new TxCallback() {

			@Override
			public Object execute(TransactionStatus status) throws Exception {
				setAuthentication("UnitTest");
				Audit.info("bla");

				Thread.sleep(100);

				setAuthentication("UnitTest2");
				Audit.info("bli");

				Thread.sleep(100);

				setAuthentication("UnitTest");
				Audit.info("blo");

				resetAuthentication();
				return null;
			}

		});


		List<AuditLine> list = auditLineDAO.findLastCountFromID(0, 2);
		assertEquals(2, list.size());

		assertEquals("UnitTest", list.get(0).getUser());
		assertEquals("bli", list.get(1).getMessage());
		Date date0 = list.get(0).getDate();
		Date date1 = list.get(1).getDate();
		String str0 = date0.toString();
		String str1 = date1.toString();
		LOGGER.debug("Date1: " + str0 + " / Date2: " + str1);
		long m0 = date0.getTime();
		long m1 = date1.getTime();
		assertTrue(m0 > m1);
	}

	@Test
	@NotTransactional
	public void testLogAuditLineIdCriterion() throws Exception {

		final int ALL_LINES = 12345; // big enough

		doInNewTransaction(new TransactionCallback() {

			public Object doInTransaction(TransactionStatus status) {

				setAuthentication("UnitTest");
				Audit.info("foo");
				Audit.info("bar");
				Audit.info("ggg");
				Audit.info("kuc");
				return null;
			}
		});

		final List<AuditLine> all = auditLineDAO.findLastCountFromID(0, ALL_LINES);
		assertEquals(4, all.size());

		long biggestId = all.get(0).getId();
		long secondBiggestId = all.get(1).getId();
		long thirdBiggestId = all.get(2).getId();
		long smallestId = all.get(3).getId();
		assertTrue(biggestId > secondBiggestId);
		assertTrue(secondBiggestId > thirdBiggestId);
		assertTrue(thirdBiggestId > smallestId);

		{
			List<AuditLine> list = auditLineDAO.findLastCountFromID(biggestId, ALL_LINES);
			assertEquals(1, list.size());
		}
		{
			List<AuditLine> list = auditLineDAO.findLastCountFromID(secondBiggestId, ALL_LINES);
			assertEquals(2, list.size());
		}
		{
			List<AuditLine> list = auditLineDAO.findLastCountFromID(thirdBiggestId, ALL_LINES);
			assertEquals(3, list.size());
		}
		{
			List<AuditLine> list = auditLineDAO.findLastCountFromID(smallestId, ALL_LINES);
			assertEquals(4, list.size());
		}
		{
			List<AuditLine> list = auditLineDAO.findLastCountFromID(biggestId + 1, ALL_LINES);
			assertEmpty(list);
		}
	}

	@Test
	@NotTransactional
	public void testLogAuditLineCountLimit() throws Exception {

		final int ALL_IDS = 0;
		final int ALL_LINES = 12345; // big enough

		doInNewTransaction(new TransactionCallback() {

			public Object doInTransaction(TransactionStatus status) {

				setAuthentication("UnitTest");
				Audit.info("foo");
				Audit.info("bar");
				Audit.info("ggg");
				Audit.info("kuc");
				Audit.info("truc");

				return null;
			}
		});

		final List<AuditLine> all = auditLineDAO.findLastCountFromID(ALL_IDS, ALL_LINES);
		assertEquals(5, all.size());

		final AuditLine first = all.get(0);
		final AuditLine second = all.get(1);
		final AuditLine third = all.get(2);
		final AuditLine fourth = all.get(3);

		{
			List<AuditLine> list = auditLineDAO.findLastCountFromID(ALL_IDS, 4);
			assertEquals(4, list.size());
			assertEquals(first.getMessage(), list.get(0).getMessage());
			assertEquals(second.getMessage(), list.get(1).getMessage());
			assertEquals(third.getMessage(), list.get(2).getMessage());
			assertEquals(fourth.getMessage(), list.get(3).getMessage());
		}
		{
			List<AuditLine> list = auditLineDAO.findLastCountFromID(ALL_IDS, 3);
			assertEquals(3, list.size());
			assertEquals(first.getMessage(), list.get(0).getMessage());
			assertEquals(second.getMessage(), list.get(1).getMessage());
			assertEquals(third.getMessage(), list.get(2).getMessage());
		}
		{
			List<AuditLine> list = auditLineDAO.findLastCountFromID(ALL_IDS, 2);
			assertEquals(2, list.size());
			assertEquals(first.getMessage(), list.get(0).getMessage());
			assertEquals(second.getMessage(), list.get(1).getMessage());
		}
		{
			List<AuditLine> list = auditLineDAO.findLastCountFromID(ALL_IDS, 1);
			assertEquals(1, list.size());
			assertEquals(first.getMessage(), list.get(0).getMessage());
		}
		{
			List<AuditLine> list = auditLineDAO.findLastCountFromID(ALL_IDS, 0);
			assertEmpty(list);
		}
	}

	@Test
	@NotTransactional
	public void testLogEventWhenThrowException() throws Exception {

		AuditLineDAOTestService service = getBean(AuditLineDAOTestService.class, "auditLineDAOTestService");

		{
			List<AuditLine> list = auditLineDAO.getAll();
			assertEquals(0, list.size());
		}

		try {
			service.logAuditAndThrowException();
			fail();
		}
		catch (Exception e) {
			// ok
		}

		{
			List<AuditLine> list = auditLineDAO.getAll();
			assertEquals(1, list.size());
		}
	}

}
