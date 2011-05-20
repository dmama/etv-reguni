package ch.vd.uniregctb.hibernate;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.orm.hibernate3.HibernateOptimisticLockingFailureException;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.uniregctb.common.CoreDAOTest;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public class OptimisticLockingTest extends CoreDAOTest {

	private static final Logger LOGGER = Logger.getLogger(OptimisticLockingTest.class);

	private SessionFactory sessionFactory;
	private PlatformTransactionManager transactionManager;
	private TransactionTemplate template;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		sessionFactory = getBean(SessionFactory.class, "sessionFactory");

		transactionManager = getBean(PlatformTransactionManager.class, "transactionManager");
		template = new TransactionTemplate(transactionManager);
	}

	@Test
	@NotTransactional
	public void testLocking() {

		// Créée un Habitant dans le base
		template.execute(new TransactionCallback<Object>() {
			public Object doInTransaction(TransactionStatus status) {

				Session session = sessionFactory.getCurrentSession();
				PersonnePhysique hab = new PersonnePhysique(true);
				hab.setNumero(12345678L);
				hab.setNumeroIndividu(12345L);
				session.save(hab);
				return null;
			}
		});


		// Teste que l'Habitant est bien dans la base
		template.execute(new TransactionCallback<Object>() {
			public Object doInTransaction(TransactionStatus status) {

				Session session = sessionFactory.getCurrentSession();
				Query q = session.createQuery("from PersonnePhysique");
				List<?> list = q.list();
				PersonnePhysique hab = (PersonnePhysique) list.get(0);
				assertEquals(new Long(12345L), hab.getNumeroIndividu());
				return hab;
			}
		});

		// On recupère l'Habitant dans la session1
		// Puis on le modifie dans la session2
		template.execute(new TransactionCallback<Object>() {
			public Object doInTransaction(TransactionStatus status) {

				Session session = sessionFactory.getCurrentSession();
				Query q1 = session.createQuery("from PersonnePhysique");
				List<?> list1 = q1.list();
				PersonnePhysique hab1 = (PersonnePhysique) list1.get(0);
				assertEquals(new Long(12345L), hab1.getNumeroIndividu());
				return hab1;
			}
		});

		template.execute(new TransactionCallback<Object>() {
			public Object doInTransaction(TransactionStatus status) {

				Session session = sessionFactory.getCurrentSession();
				Query q2 = session.createQuery("from PersonnePhysique");
				List<?> list = q2.list();
				PersonnePhysique hab = (PersonnePhysique) list.get(0);
				assertEquals(new Long(12345L), hab.getNumeroIndividu());

				// On modifie le numero IND
				hab.setNumeroIndividu(12346L);
				return null;
			}
		});

		// Teste que l'Habitant a bien été modifié
		template.execute(new TransactionCallback<Object>() {
			public Object doInTransaction(TransactionStatus status) {

				Session session = sessionFactory.getCurrentSession();
				Query q = session.createQuery("from PersonnePhysique");
				List<?> list = q.list();
				PersonnePhysique hab = (PersonnePhysique) list.get(0);
				assertEquals(new Long(12346L), hab.getNumeroIndividu());
				return null;
			}
		});

		// Modification après coup => Exception
		try {
			template.execute(new TransactionCallback<Object>() {
				public Object doInTransaction(TransactionStatus status) {
					Session session = sessionFactory.getCurrentSession();
					Query q1 = session.createQuery("from PersonnePhysique");
					List<?> list1 = q1.list();
					PersonnePhysique hab1 = (PersonnePhysique) list1.get(0);
					assertEquals(new Long(12346L), hab1.getNumeroIndividu());

					try {
					TransactionTemplate templateNew = new TransactionTemplate(transactionManager);
					templateNew.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
					templateNew.execute(new TransactionCallback<Object>() {
						public Object doInTransaction(TransactionStatus status) {

							Session session = sessionFactory.getCurrentSession();
							Query q1 = session.createQuery("from PersonnePhysique");
							List<?> list1 = q1.list();
							PersonnePhysique hab2 = (PersonnePhysique) list1.get(0);
							assertEquals(new Long(12346L), hab2.getNumeroIndividu());
							hab2.setNumeroIndividu(12347L);
							return null;
						}
					});
					}
					catch (Exception e) {
						fail();
					}
					hab1.setNumeroIndividu(12341L);
					return null;
				}
			});
			assertTrue(false);
		}
		catch (HibernateOptimisticLockingFailureException e) {
			LOGGER.error("L'exception générée ci-dessus est normale. Le test passe malgré cette exception");
			// Tout va bien
			e = null;
		}

		// Teste que l'Habitant a le bon numéro
		template.execute(new TransactionCallback<Object>() {
			public Object doInTransaction(TransactionStatus status) {

				Session session = sessionFactory.getCurrentSession();
				Query q = session.createQuery("from PersonnePhysique");
				List<?> list = q.list();
				PersonnePhysique hab = (PersonnePhysique) list.get(0);
				assertEquals(new Long(12347L), hab.getNumeroIndividu());
				return null;
			}
		});
	}

}
