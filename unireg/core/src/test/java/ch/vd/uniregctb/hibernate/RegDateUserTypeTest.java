package ch.vd.uniregctb.hibernate;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.Hibernate;
import org.hibernate.SQLQuery;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.classic.Session;
import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.test.context.ContextConfiguration;

import ch.vd.registre.base.date.PartialDateException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.CoreDAOTest;

@ContextConfiguration(locations = {
	"classpath:ut/RegDateUserTypeTest-spring.xml"
})
public class RegDateUserTypeTest extends CoreDAOTest {

	@Entity(name = "TEST_DATA")
	public static class TestData {

		private Long id;

		private RegDate partialDate;

		private RegDate fullDate;

		public TestData() {
			this.partialDate = null;
			this.fullDate = null;
		}

		public TestData(RegDate partialDate, RegDate fullDate) {
			this.partialDate = partialDate;
			this.fullDate = fullDate;
		}

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType", parameters = {
			@Parameter(name = "allowPartial", value = "true")
		})
		public RegDate getPartialDate() {
			return partialDate;
		}

		public void setPartialDate(RegDate date) {
			this.partialDate = date;
		}

		@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
		public RegDate getFullDate() {
			return fullDate;
		}

		public void setFullDate(RegDate fullDate) {
			this.fullDate = fullDate;
		}
	}

	private SessionFactory sessionFactory;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		sessionFactory = getBean(SessionFactory.class, "sessionFactoryRegDate");
	}

	@Test
	@NotTransactional
	public void testBasicSaveReload() {

		Session session = sessionFactory.openSession();
		Transaction trans = session.beginTransaction();

		final Long id;
		{
			TestData data = new TestData(RegDate.get(2003, 3), RegDate.get(2008, 4, 8));
			id = (Long) session.save(data);
		}

		trans.commit();

		{
			TestData data = (TestData) session.get(TestData.class, id);
			assertEquals(RegDate.get(2003, 3), data.getPartialDate());
			assertEquals(RegDate.get(2008, 4, 8), data.getFullDate());
		}

		session.close();
	}

	@Test
	@NotTransactional
	public void testLoadFromDatabase() throws Exception {

		loadDatabase("RegDateUserTypeTest.xml");

		Session session = sessionFactory.openSession();

		/*
		 * différent types de dates valides
		 */

		TestData data1 = (TestData) session.get(TestData.class, 1L);
		assertEquals(RegDate.get(1953, 3, 12), data1.getFullDate());
		assertEquals(RegDate.get(1953), data1.getPartialDate());

		TestData data2 = (TestData) session.get(TestData.class, 2L);
		assertEquals(RegDate.get(1943, 10, 9), data2.getFullDate());
		assertEquals(RegDate.get(1943, 10), data2.getPartialDate());

		TestData data3 = (TestData) session.get(TestData.class, 3L);
		assertEquals(RegDate.get(2003, 10, 22), data3.getFullDate());
		assertEquals(RegDate.get(2008, 12, 24), data3.getPartialDate());

		/*
		 * différent types de dates invalides
		 */

		try {
			session.get(TestData.class, 4L);
			fail("Ce test ne doit pas passer car '19431000' n'est pas autorisé pour une date complète");
		}
		catch (PartialDateException e) {
			// ok
		}

		try {
			session.get(TestData.class, 5L);
			fail("Ce test ne doit pas passer car '19430000' n'est pas autorisé pour une date complète");
		}
		catch (PartialDateException e) {
			// ok
		}

		session.close();
	}

	@Test
	@NotTransactional
	public void testSaveToDatabase() {

		Session session = sessionFactory.openSession();
		session.createSQLQuery("delete from TEST_DATA").executeUpdate();
		assertNull(session.createQuery("select count(1) from TEST_DATA").uniqueResult());

		Transaction trans = session.beginTransaction();

		/*
		 * Sauve des données dans la base de données en passant pas Hibernate
		 */
		final Long id1;
		final Long id2;
		final Long id3;
		{
			TestData data1 = new TestData(RegDate.get(2000, 11, 30), RegDate.get(2012, 1, 18));
			id1 = (Long) session.save(data1);

			TestData data2 = new TestData(RegDate.get(2000, 11), RegDate.get(1991, 8, 1));
			id2 = (Long) session.save(data2);

			TestData data3 = new TestData(RegDate.get(2000), RegDate.get(2345, 11, 30));
			id3 = (Long) session.save(data3);
		}

		trans.commit();

		/*
		 * Relis les données sauvées en pure SQL et compare les valeurs
		 */
		{
			final SQLQuery query = session.createSQLQuery("select ID, FULLDATE, PARTIALDATE from TEST_DATA order by ID asc");
			query.addScalar("ID", Hibernate.LONG);
			query.addScalar("FULLDATE", Hibernate.INTEGER);
			query.addScalar("PARTIALDATE", Hibernate.INTEGER);

			final List<?> list = query.list();
			assertEquals(3, list.size());

			final Object[] line1 = (Object[]) list.get(0);
			assertEquals(id1, line1[0]); // id
			assertEquals(new Integer(20120118), line1[1]); // full date
			assertEquals(new Integer(20001130), line1[2]); // partial date

			final Object[] line2 = (Object[]) list.get(1);
			assertEquals(id2, line2[0]); // id
			assertEquals(new Integer(19910801), line2[1]); // full date
			assertEquals(new Integer(20001100), line2[2]); // partial date

			final Object[] line3 = (Object[]) list.get(2);
			assertEquals(id3, line3[0]); // id
			assertEquals(new Integer(23451130), line3[1]); // full date
			assertEquals(new Integer(20000000), line3[2]); // partial date
		}
		session.close();
	}

	@Override
	public String[] getTableNames(boolean reverse) {
		String[] origNames = super.getTableNames(reverse);
		String[] names = new String[origNames.length+1];
		names[0] = "TEST_DATA";
		System.arraycopy(origNames, 0, names, 1, origNames.length);
		return names;
	}

}
