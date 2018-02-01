package ch.vd.unireg.hibernate;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.hibernate4.HibernateOptimisticLockingFailureException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

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
		doInNewTransaction(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				Session session = sessionFactory.getCurrentSession();
				PersonnePhysique hab = new PersonnePhysique(true);
				hab.setNumero(12345678L);
				hab.setNumeroIndividu(12345L);
				session.save(hab);
			}
		});

		// Teste que l'Habitant est bien dans la base
		doInNewReadOnlyTransaction(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {

				Session session = sessionFactory.getCurrentSession();
				Query q = session.createQuery("from PersonnePhysique");
				List<?> list = q.list();
				PersonnePhysique hab = (PersonnePhysique) list.get(0);
				assertEquals(new Long(12345L), hab.getNumeroIndividu());
			}
		});

		// On recupère l'Habitant dans la session1
		// Puis on le modifie dans la session2
		doInNewTransaction(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				Session session = sessionFactory.getCurrentSession();
				Query q1 = session.createQuery("from PersonnePhysique");
				List<?> list1 = q1.list();
				PersonnePhysique hab1 = (PersonnePhysique) list1.get(0);
				assertEquals(new Long(12345L), hab1.getNumeroIndividu());
			}
		});

		doInNewTransaction(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				Session session = sessionFactory.getCurrentSession();
				Query q2 = session.createQuery("from PersonnePhysique");
				List<?> list = q2.list();
				PersonnePhysique hab = (PersonnePhysique) list.get(0);
				assertEquals(new Long(12345L), hab.getNumeroIndividu());

				// On modifie le numero IND
				hab.setNumeroIndividu(12346L);
			}
		});

		// Teste que l'Habitant a bien été modifié
		doInNewReadOnlyTransaction(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				Session session = sessionFactory.getCurrentSession();
				Query q = session.createQuery("from PersonnePhysique");
				List<?> list = q.list();
				PersonnePhysique hab = (PersonnePhysique) list.get(0);
				assertEquals(new Long(12346L), hab.getNumeroIndividu());
			}
		});

		// Modification après coup => Exception
		try {
			doInNewTransaction(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
					Session session = sessionFactory.getCurrentSession();
					Query q1 = session.createQuery("from PersonnePhysique");
					List<?> list1 = q1.list();
					PersonnePhysique hab1 = (PersonnePhysique) list1.get(0);
					assertEquals(new Long(12346L), hab1.getNumeroIndividu());

					try {
						doInNewTransaction(new TransactionCallbackWithoutResult() {
							@Override
							protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
								Session session = sessionFactory.getCurrentSession();
								Query q1 = session.createQuery("from PersonnePhysique");
								List<?> list1 = q1.list();
								PersonnePhysique hab2 = (PersonnePhysique) list1.get(0);
								assertEquals(new Long(12346L), hab2.getNumeroIndividu());
								hab2.setNumeroIndividu(12347L);
							}
						});
					}
					catch (Exception e) {
						fail();
					}
					hab1.setNumeroIndividu(12341L);
				}
			});
			fail();
		}
		catch (HibernateOptimisticLockingFailureException e) {
			LOGGER.error("L'exception générée ci-dessus est normale. Le test passe malgré cette exception");
			// Tout va bien
			e = null;
		}

		// Teste que l'Habitant a le bon numéro
		doInNewReadOnlyTransaction(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				Session session = sessionFactory.getCurrentSession();
				Query q = session.createQuery("from PersonnePhysique");
				List<?> list = q.list();
				PersonnePhysique hab = (PersonnePhysique) list.get(0);
				assertEquals(new Long(12347L), hab.getNumeroIndividu());
			}
		});
	}
}
