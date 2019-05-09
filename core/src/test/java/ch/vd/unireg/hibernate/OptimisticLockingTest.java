package ch.vd.unireg.hibernate;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.hibernate5.HibernateOptimisticLockingFailureException;

import ch.vd.unireg.common.CoreDAOTest;
import ch.vd.unireg.tiers.PersonnePhysique;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class OptimisticLockingTest extends CoreDAOTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(OptimisticLockingTest.class);

	private SessionFactory sessionFactory;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		sessionFactory = getBean(SessionFactory.class, "sessionFactory");
	}

	@Test
	public void testLocking() throws Exception {

		// Créée un Habitant dans le base
		doInNewTransaction(status -> {
			Session session = sessionFactory.getCurrentSession();
			PersonnePhysique hab = new PersonnePhysique(true);
			hab.setNumero(12345678L);
			hab.setNumeroIndividu(12345L);
			session.save(hab);
			return null;
		});

		// Teste que l'Habitant est bien dans la base
		doInNewReadOnlyTransaction(status -> {
			Session session = sessionFactory.getCurrentSession();
			Query q = session.createQuery("from PersonnePhysique");
			List<?> list = q.list();
			PersonnePhysique hab = (PersonnePhysique) list.get(0);
			assertEquals(new Long(12345L), hab.getNumeroIndividu());
			return null;
		});

		// On recupère l'Habitant dans la session1
		// Puis on le modifie dans la session2
		doInNewTransaction(status -> {
			Session session = sessionFactory.getCurrentSession();
			Query q1 = session.createQuery("from PersonnePhysique");
			List<?> list1 = q1.list();
			PersonnePhysique hab1 = (PersonnePhysique) list1.get(0);
			assertEquals(new Long(12345L), hab1.getNumeroIndividu());
			return null;
		});

		doInNewTransaction(status -> {
			Session session = sessionFactory.getCurrentSession();
			Query q2 = session.createQuery("from PersonnePhysique");
			List<?> list = q2.list();
			PersonnePhysique hab = (PersonnePhysique) list.get(0);
			assertEquals(new Long(12345L), hab.getNumeroIndividu());

			// On modifie le numero IND
			hab.setNumeroIndividu(12346L);
			return null;
		});

		// Teste que l'Habitant a bien été modifié
		doInNewReadOnlyTransaction(status -> {
			Session session = sessionFactory.getCurrentSession();
			Query q = session.createQuery("from PersonnePhysique");
			List<?> list = q.list();
			PersonnePhysique hab = (PersonnePhysique) list.get(0);
			assertEquals(new Long(12346L), hab.getNumeroIndividu());
			return null;
		});

		// Modification après coup => Exception
		try {
			doInNewTransaction(status -> {
				Session session = sessionFactory.getCurrentSession();
				Query q1 = session.createQuery("from PersonnePhysique");
				List<?> list1 = q1.list();
				PersonnePhysique hab1 = (PersonnePhysique) list1.get(0);
				assertEquals(new Long(12346L), hab1.getNumeroIndividu());

				try {
					doInNewTransaction(status2 -> {
						Session session2 = sessionFactory.getCurrentSession();
						Query q2 = session2.createQuery("from PersonnePhysique");
						List<?> list2 = q2.list();
						PersonnePhysique hab2 = (PersonnePhysique) list2.get(0);
						assertEquals(new Long(12346L), hab2.getNumeroIndividu());
						hab2.setNumeroIndividu(12347L);
						return null;
					});
				}
				catch (Exception e) {
					fail();
				}
				hab1.setNumeroIndividu(12341L);
				return null;
			});
			fail();
		}
		catch (HibernateOptimisticLockingFailureException e) {
			LOGGER.error("L'exception générée ci-dessus est normale. Le test passe malgré cette exception");
			// Tout va bien
			e = null;
		}

		// Teste que l'Habitant a le bon numéro
		doInNewReadOnlyTransaction(status -> {
			Session session = sessionFactory.getCurrentSession();
			Query q = session.createQuery("from PersonnePhysique");
			List<?> list = q.list();
			PersonnePhysique hab = (PersonnePhysique) list.get(0);
			assertEquals(new Long(12347L), hab.getNumeroIndividu());
			return null;
		});
	}
}
