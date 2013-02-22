package ch.vd.uniregctb.hibernate;

import java.util.List;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.uniregctb.common.CoreDAOTest;
import ch.vd.uniregctb.common.DataHolder;
import ch.vd.uniregctb.tiers.PersonnePhysique;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

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

	@Test(timeout = 10000L)
	public void testLocking() throws Exception {

		// Créée un Habitant dans le base
		template.execute(new TransactionCallback<Object>() {
			@Override
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
			@Override
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
			@Override
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
			@Override
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
			@Override
			public Object doInTransaction(TransactionStatus status) {

				Session session = sessionFactory.getCurrentSession();
				Query q = session.createQuery("from PersonnePhysique");
				List<?> list = q.list();
				PersonnePhysique hab = (PersonnePhysique) list.get(0);
				assertEquals(new Long(12346L), hab.getNumeroIndividu());
				return null;
			}
		});

		final MutableBoolean waitingForFirstPick = new MutableBoolean(true);
		final MutableBoolean waitingForModification = new MutableBoolean(true);

		// modificateur heureux
		final DataHolder<Exception> heureuxInterrompu = new DataHolder<>();
		final Thread heureux = new Thread(new Runnable() {
			@Override
			public void run() {

				// on attend que le thread chanceux ait fini de prendre les données dans la base
				synchronized (waitingForFirstPick) {
					while (waitingForFirstPick.booleanValue()) {
						try {
							waitingForFirstPick.wait();
						}
						catch (InterruptedException e) {
							heureuxInterrompu.set(e);
							return;
						}
					}
				}

				setAuthentication("Heureux");
				try {
					final TransactionTemplate template = new TransactionTemplate(transactionManager);
					template.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
					template.execute(new TransactionCallback<Object>() {
						@Override
						public Object doInTransaction(TransactionStatus status) {
							final Session session = sessionFactory.getCurrentSession();
							final Query q1 = session.createQuery("from PersonnePhysique");
							final List<?> list1 = q1.list();
							final PersonnePhysique hab2 = (PersonnePhysique) list1.get(0);
							assertEquals(new Long(12346L), hab2.getNumeroIndividu());
							hab2.setNumeroIndividu(12347L);
							return null;
						}
					});

					synchronized (waitingForModification) {
						waitingForModification.setValue(false);
						waitingForModification.notify();
					}
				}
				catch (Exception e) {
					heureuxInterrompu.set(e);
				}
				finally {
					resetAuthentication();
				}
			}
		}, "Heureux");

		// modificateur malheureux
		final DataHolder<Exception> malheureuxInterrompu = new DataHolder<>();
		final Thread malheureux = new Thread(new Runnable() {
			@Override
			public void run() {
				setAuthentication("Malheureux");
				try {
					final TransactionTemplate template = new TransactionTemplate(transactionManager);
					template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
					template.execute(new TransactionCallback<Object>() {
						@Override
						public Object doInTransaction(TransactionStatus status) {
							final Session session = sessionFactory.getCurrentSession();
							final Query q1 = session.createQuery("from PersonnePhysique");
							final List<?> list1 = q1.list();
							final PersonnePhysique hab1 = (PersonnePhysique) list1.get(0);
							assertEquals(new Long(12346L), hab1.getNumeroIndividu());

							synchronized (waitingForFirstPick) {
								waitingForFirstPick.setValue(false);
								waitingForFirstPick.notify();
							}

							synchronized (waitingForModification) {
								while (waitingForModification.booleanValue()) {
									try {
										waitingForModification.wait();
									}
									catch (InterruptedException e) {
										malheureuxInterrompu.set(e);
										return null;
									}
								}
							}

							hab1.setNumeroIndividu(12341L);
							return null;
						}
					});
				}
				catch (Exception e) {
					malheureuxInterrompu.set(e);
				}
				finally {
					resetAuthentication();
				}
			}
		}, "Malheureux");

		heureux.start();
		malheureux.start();
		heureux.join();
		malheureux.join();

		assertNull(heureuxInterrompu.get());
		assertNotNull(malheureuxInterrompu.get());

		Throwable rootCause = malheureuxInterrompu.get();
		while (rootCause.getCause() != null) {
			rootCause = rootCause.getCause();
		}
		assertTrue(rootCause.getMessage().contains("Rollback"));

		// Teste que l'Habitant a le bon numéro
		template.execute(new TransactionCallback<Object>() {
			@Override
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
