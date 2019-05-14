package ch.vd.unireg.hibernate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.PartialDateException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.CoreDAOTest;
import ch.vd.unireg.dbutils.SqlFileExecutor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@ContextConfiguration(locations = {
	"classpath:ut/RegDateUserTypeTest-spring.xml"
})
public class RegDateUserTypeTest extends CoreDAOTest {

	@Entity(name = "TEST_DATA")
	@SequenceGenerator(name = "defaultGenerator", sequenceName = "hibernate_sequence", allocationSize = 1)
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
		@GeneratedValue(generator = "defaultGenerator")
	@SequenceGenerator(name = "defaultGenerator", sequenceName = "hibernate_sequence", allocationSize = 1)
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@Type(type = "ch.vd.unireg.hibernate.RegDateUserType", parameters = {
			@Parameter(name = "allowPartial", value = "true")
		})
		public RegDate getPartialDate() {
			return partialDate;
		}

		public void setPartialDate(RegDate date) {
			this.partialDate = date;
		}

		@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
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

	@Override
	protected void truncateDatabase() throws Exception {
		super.truncateDatabase();
		SqlFileExecutor.execute(transactionManager, dataSource, Collections.singletonList("delete from TEST_DATA"));
	}

	@Test
	public void testBasicSaveReload() throws Exception {

		final long id = doInNewTransaction(status -> {
			final Session session = sessionFactory.getCurrentSession();
			final TestData data = new TestData(RegDate.get(2003, 3), RegDate.get(2008, 4, 8));
			return (Long) session.save(data);
		});

		doInNewTransaction(status -> {
			final Session session = sessionFactory.getCurrentSession();
			final TestData data = (TestData) session.get(TestData.class, id);
			assertEquals(RegDate.get(2003, 3), data.getPartialDate());
			assertEquals(RegDate.get(2008, 4, 8), data.getFullDate());
			return null;
		});
	}

	@Test
	@Transactional
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
	public void testSaveToDatabase() throws Exception {

		doInNewTransaction(status -> {
			final Session session = sessionFactory.getCurrentSession();
			session.createNativeQuery("delete from TEST_DATA").executeUpdate();
			assertEquals(0L, ((Number) session.createQuery("select count(*) from TEST_DATA").uniqueResult()).longValue());
			return null;
		});

		class Ids {
			long id1;
			long id2;
			long id3;
		}
		/*
		 * Sauve des données dans la base de données en passant pas Hibernate
		 */
		final Ids ids = doInNewTransaction(status -> {
			final Session session = sessionFactory.getCurrentSession();
			final Ids ids1 = new Ids();
			final TestData data1 = new TestData(RegDate.get(2000, 11, 30), RegDate.get(2012, 1, 18));
			ids1.id1 = (Long) session.save(data1);

			final TestData data2 = new TestData(RegDate.get(2000, 11), RegDate.get(1991, 8, 1));
			ids1.id2 = (Long) session.save(data2);

			final TestData data3 = new TestData(RegDate.get(2000), RegDate.get(2345, 11, 30));
			ids1.id3 = (Long) session.save(data3);
			return ids1;
		});

		/*
		 * Relis les données sauvées en pure SQL et compare les valeurs
		 */

		doInNewTransaction(status -> {
			final Session session = sessionFactory.getCurrentSession();
			final NativeQuery query = session.createNativeQuery("select ID, FULLDATE, PARTIALDATE from TEST_DATA order by ID asc");
			query.addScalar("ID", StandardBasicTypes.LONG);
			query.addScalar("FULLDATE", StandardBasicTypes.INTEGER);
			query.addScalar("PARTIALDATE", StandardBasicTypes.INTEGER);

			final List<?> list = query.list();
			assertEquals(3, list.size());

			final Object[] line1 = (Object[]) list.get(0);
			assertEquals(ids.id1, line1[0]); // id
			assertEquals(20120118, line1[1]); // full date
			assertEquals(20001130, line1[2]); // partial date

			final Object[] line2 = (Object[]) list.get(1);
			assertEquals(ids.id2, line2[0]); // id
			assertEquals(19910801, line2[1]); // full date
			assertEquals(20001100, line2[2]); // partial date

			final Object[] line3 = (Object[]) list.get(2);
			assertEquals(ids.id3, line3[0]); // id
			assertEquals(23451130, line3[1]); // full date
			assertEquals(20000000, line3[2]); // partial date;
			return null;
		});
	}
}
