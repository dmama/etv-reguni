package ch.vd.uniregctb.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.test.context.ContextConfiguration;

import ch.vd.uniregctb.common.CoreDAOTest;
import ch.vd.uniregctb.transaction.dao.SimpleDao;

@ContextConfiguration(locations = {
	"/ch/vd/uniregctb/transaction/SimpleServiceTest-spring.xml"
})
public class TransactionSimpleServiceTest extends CoreDAOTest {

	//private static Logger LOGGER = Logger.getLogger(TransactionSimpleServiceTest.class);

	private SimpleService1 service1 = null;
	private SimpleService2 service2 = null;
	private SimpleDao dao = null;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		service1 = getBean(SimpleService1.class, "service1");
		service2 = getBean(SimpleService2.class, "service2");
		dao = getBean(SimpleDao.class, "simpleDAO");

		dao.createTable();
	}

	@Test
	@NotTransactional
	public void testInsert() {

		String str = "blabla";
		service1.insertLine(3, str);
		assertEquals(str, service1.readLine(3));
	}

	@Test
	@NotTransactional
	public void testUpdate() {

		int id=44;
		String msg = "test2";
		service1.insertLine(id, msg);
		assertEquals(msg, service1.readLine(id));

		String str = "blabla3";
		service1.updateLine(id, str);
		assertEquals(str, service1.readLine(id));
	}

	@Test
	@NotTransactional
	public void testUpdateWithException() {

		int id=89;
		service1.insertLine(id, "test2");

		try {
			service1.updateLineException(id, "BliBli");
			fail();
		}
		catch (Exception e) {
			// ok
		}
		// On doit pas avoit BliBli
		assertEquals("test2", service1.readLine(id));
	}

	@Test
	@NotTransactional
	public void testInsertWithException() {

		int id=18;
		try {
			service1.insertLineException(id, "BliBli");
			fail();
		}
		catch (Exception e) {
			// ok
		}
		assertEquals(null, service1.readLine(id));
	}

	@Test
	@NotTransactional
	public void testRequiresNew() {

		int id = 78;
		service1.insertLineRequiresNew(id, "Bloi");
	}

	@Test
	@NotTransactional
	public void testInsertRequiresNew() {

		int id1 = 7;
		int id2 = 8;
		try {
			service2.insert2LinesException(id1, "msg1", id2, "msg2");
			fail();
		}
		catch (Exception e) {
			// ok
		}
		assertEquals("msg1", service1.readLine(id1));
		assertEquals(null, service1.readLine(id2));
	}

	@Test
	@NotTransactional
	public void testInsertLineMandatory() {

		int id = 23;
		String msg = "Blklkh";
		try {
			// Exception parce que on doit avoir une transaction avant
			service1.insertLineMandatory(id, msg);
			fail();
		}
		catch (Exception e) {
			// ok
		}
		assertEquals(null, service1.readLine(id));

		service2.insertLineCallMandatory(id, msg);
		assertEquals(msg, service1.readLine(id));
	}

}
