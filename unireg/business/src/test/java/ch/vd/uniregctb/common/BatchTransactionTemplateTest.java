package ch.vd.uniregctb.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.*;

import org.junit.Test;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BatchTransactionTemplate.BatchCallback;
import ch.vd.uniregctb.common.BatchTransactionTemplate.Behavior;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class BatchTransactionTemplateTest extends BusinessTest {

	private PlatformTransactionManager transactionManager;
	private TiersDAO tiersDAO;
	private HibernateTemplate hibernateTemplate;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		transactionManager = getBean(PlatformTransactionManager.class, "transactionManager");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		hibernateTemplate = getBean(HibernateTemplate.class, "hibernateTemplate");
	}

	@NotTransactional
	@Test
	public void testEmptyList() {
		List<Long> list = Collections.emptyList();
		BatchTransactionTemplate<Long, JobResults> template = new BatchTransactionTemplate<Long, JobResults>(list, 100, Behavior.SANS_REPRISE, transactionManager, null, hibernateTemplate);
		template.execute(new BatchCallback<Long, JobResults>() {

			@Override
			public void beforeTransaction() {
				fail();
			}

			@Override
			public boolean doInTransaction(List<Long> batch, JobResults rapport) throws Exception {
				fail();
				return true;
			}

			@Override
			public void afterTransactionCommit() {
				fail();
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				fail();
			}
		});
	}

	@NotTransactional
	@Test
	public void testSansRepriseSansException() throws Exception {

		List<Long> list = new ArrayList<Long>();
		list.add(0L);
		list.add(1L);
		list.add(2L);
		list.add(3L);
		list.add(4L);

		assertTiersCountHorsTransaction(0);

		BatchTransactionTemplate<Long, JobResults> template = new BatchTransactionTemplate<Long, JobResults>(list, 2, Behavior.SANS_REPRISE, transactionManager, null, hibernateTemplate);
		template.execute(new BatchCallback<Long, JobResults>() {

			@Override
			public boolean doInTransaction(List<Long> batch, JobResults rapport) throws Exception {
				assertTrue(batch.size() == 1 || batch.size() == 2);
				addNonHabitant("Test", "Traitement du batch = " + batch, RegDate.get(), Sexe.MASCULIN);
				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				fail("La transaction ne doit pas sauter");
			}
		});

		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				// On vérifie que les batchs ont bien été processés et committés
				final List<Tiers> lines = tiersDAO.getAll();
				Collections.sort(lines, new Comparator<Tiers>() {
					public int compare(Tiers o1, Tiers o2) {
						return (int) (o1.getNumero() - o2.getNumero()); // -> ordre naturel d'insertion
					}
				});
				assertEquals(3, lines.size());

				final PersonnePhysique tiers0 = (PersonnePhysique) lines.get(0);
				assertEquals("Traitement du batch = [0, 1]", tiers0.getNom());
				final PersonnePhysique tiers1 = (PersonnePhysique) lines.get(1);
				assertEquals("Traitement du batch = [2, 3]", tiers1.getNom());
				final PersonnePhysique tiers2 = (PersonnePhysique) lines.get(2);
				assertEquals("Traitement du batch = [4]", tiers2.getNom());
				return null;
			}
		});
	}

	@NotTransactional
	@Test
	public void testSansRepriseAvecException() throws Exception {

		List<Long> list = new ArrayList<Long>();
		list.add(0L);
		list.add(1L);
		list.add(2L);
		list.add(3L);
		list.add(4L);

		assertTiersCountHorsTransaction(0);

		BatchTransactionTemplate<Long, JobResults> template = new BatchTransactionTemplate<Long, JobResults>(list, 2, Behavior.SANS_REPRISE, transactionManager, null, hibernateTemplate);
		template.execute(new BatchCallback<Long, JobResults>() {

			@Override
			public boolean doInTransaction(List<Long> batch, JobResults rapport) throws Exception {
				assertTrue(batch.size() == 1 || batch.size() == 2);

				if (batch.get(0).equals(2L)) {
					throw new RuntimeException("Rollback du deuxième batch");
				}

				addNonHabitant("Test", "Traitement du batch = " + batch, RegDate.get(), Sexe.MASCULIN);
				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				assertFalse(willRetry);
			}
		});

		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				
				// On vérifie que les batchs ont bien été processés et committés à l'exception du deuxième batch qui a été rollé-back
				final List<Tiers> lines = tiersDAO.getAll();
				Collections.sort(lines, new Comparator<Tiers>() {
					public int compare(Tiers o1, Tiers o2) {
						return (int) (o1.getNumero() - o2.getNumero()); // -> ordre naturel d'insertion
					}
				});
				assertEquals(2, lines.size());

				final PersonnePhysique tiers0 = (PersonnePhysique) lines.get(0);
				assertEquals("Traitement du batch = [0, 1]", tiers0.getNom());
				final PersonnePhysique tiers1 = (PersonnePhysique) lines.get(1);
				assertEquals("Traitement du batch = [4]", tiers1.getNom());
				return null;
			}
		});
	}

	@NotTransactional
	@Test
	public void testAvecRepriseSansException() throws Exception {

		List<Long> list = new ArrayList<Long>();
		list.add(0L);
		list.add(1L);
		list.add(2L);
		list.add(3L);
		list.add(4L);

		assertTiersCountHorsTransaction(0);

		BatchTransactionTemplate<Long, JobResults> template = new BatchTransactionTemplate<Long, JobResults>(list, 2, Behavior.REPRISE_AUTOMATIQUE,
				transactionManager, null, hibernateTemplate);
		template.execute(new BatchCallback<Long, JobResults>() {

			@Override
			public boolean doInTransaction(List<Long> batch, JobResults rapport) throws Exception {
				assertTrue(batch.size() == 1 || batch.size() == 2);
				addNonHabitant("Test", "Traitement du batch = " + batch, RegDate.get(), Sexe.MASCULIN);
				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				fail("La transaction ne doit pas sauter");
			}
		});

		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				// On vérifie que les batchs ont bien été processés et committés
				final List<Tiers> lines = tiersDAO.getAll();
				Collections.sort(lines, new Comparator<Tiers>() {
					public int compare(Tiers o1, Tiers o2) {
						return (int) (o1.getNumero() - o2.getNumero()); // -> ordre naturel d'insertion
					}
				});
				assertEquals(3, lines.size());

				final PersonnePhysique tiers0 = (PersonnePhysique) lines.get(0);
				assertEquals("Traitement du batch = [0, 1]", tiers0.getNom());
				final PersonnePhysique tiers1 = (PersonnePhysique) lines.get(1);
				assertEquals("Traitement du batch = [2, 3]", tiers1.getNom());
				final PersonnePhysique tiers2 = (PersonnePhysique) lines.get(2);
				assertEquals("Traitement du batch = [4]", tiers2.getNom());
				return null;
			}
		});
	}

	@NotTransactional
	@Test
	public void testAvecRepriseAvecException() throws Exception {

		List<Long> list = new ArrayList<Long>();
		list.add(0L);
		list.add(1L);
		list.add(2L);
		list.add(3L);
		list.add(4L);

		assertTiersCountHorsTransaction(0);

		BatchTransactionTemplate<Long, JobResults> template = new BatchTransactionTemplate<Long, JobResults>(list, 2, Behavior.REPRISE_AUTOMATIQUE,
				transactionManager, null, hibernateTemplate);
		template.execute(new BatchCallback<Long, JobResults>() {

			boolean dejaRepris = false;

			@Override
			public boolean doInTransaction(List<Long> batch, JobResults rapport) throws Exception {
				assertTrue(batch.size() == 1 || batch.size() == 2);

				if (batch.get(0).equals(2L)) {
					throw new RuntimeException("Rollback du deuxième batch");
				}

				addNonHabitant("Test", "Traitement du batch = " + batch, RegDate.get(), Sexe.MASCULIN);
				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (dejaRepris) {
					assertFalse(willRetry);
				}
				else {
					assertTrue(willRetry);
					dejaRepris = true;
				}
			}
		});

		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				/**
				 * On vérifie que :
				 * <ul>
				 * <li>le premier batch est committé complétement</li>
				 * <li>le deuxième batch est committé partiellement (reprise automatique)</li>
				 * <li>le troisième batch est committé complétement</li>
				 * </ul>
				 */
				final List<Tiers> lines = tiersDAO.getAll();
				Collections.sort(lines, new Comparator<Tiers>() {
					public int compare(Tiers o1, Tiers o2) {
						return (int) (o1.getNumero() - o2.getNumero()); // -> ordre naturel d'insertion
					}
				});
				assertEquals(3, lines.size());

				final PersonnePhysique tiers0 = (PersonnePhysique) lines.get(0);
				assertEquals("Traitement du batch = [0, 1]", tiers0.getNom());
				final PersonnePhysique tiers1 = (PersonnePhysique) lines.get(1);
				assertEquals("Traitement du batch = [3]", tiers1.getNom());
				final PersonnePhysique tiers2 = (PersonnePhysique) lines.get(2);
				assertEquals("Traitement du batch = [4]", tiers2.getNom());

				return null;
			}
		});
	}

	@NotTransactional
	@Test
	public void testRollbackOnException() throws Exception {

		List<Long> list = new ArrayList<Long>();
		list.add(1234L);

		assertTiersCountHorsTransaction(0);

		BatchTransactionTemplate<Long, JobResults> template = new BatchTransactionTemplate<Long, JobResults>(list, 2, Behavior.SANS_REPRISE,
				transactionManager, null, hibernateTemplate);
		template.execute(new BatchCallback<Long, JobResults>() {

			@Override
			public boolean doInTransaction(List<Long> batch, JobResults rapport) throws Exception {

				for (Long id : batch) {
					DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
					dpi.setNumero(id);
					dpi.setNom1("nom1");
					dpi.setNom2("nom2");
					dpi = (DebiteurPrestationImposable) hibernateTemplate.merge(dpi);

					DeclarationImpotSource lr = new DeclarationImpotSource();
					lr.setDateDebut(date(2000, 1, 1));
					lr.setDateFin(date(2000, 12, 31));
					lr.setPeriodicite(PeriodiciteDecompte.ANNUEL);

					EtatDeclaration etatDeclaration = new EtatDeclaration();
					etatDeclaration.setEtat(TypeEtatDeclaration.EMISE);
					etatDeclaration.setDateObtention(date(2001, 1, 1));
					lr.addEtat(etatDeclaration);

					DelaiDeclaration delaiDeclaration = new DelaiDeclaration();
					delaiDeclaration.setDateTraitement(date(2001, 1, 1));
					delaiDeclaration.setDelaiAccordeAu(date(2001, 6, 1));
					delaiDeclaration.setConfirmationEcrite(false);
					lr.addDelai(delaiDeclaration);

					lr.setTiers(dpi);
					lr = (DeclarationImpotSource) hibernateTemplate.merge(lr);
				}

				hibernateTemplate.flush();

				if (true) {
					throw new IllegalArgumentException();
				}

				return true;
			}
		});

		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				/*
				 * On vérifie que la base est toujours vide
				 */
				assertEquals(0, tiersDAO.getCount(Tiers.class));
				assertEquals(0, tiersDAO.getCount(DeclarationImpotSource.class));
				assertEquals(0, tiersDAO.getCount(EtatDeclaration.class));
				assertEquals(0, tiersDAO.getCount(DelaiDeclaration.class));
				return null;
			}
		});

	}

	private void assertTiersCountHorsTransaction(final int count) throws Exception {
		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				assertEquals(count, tiersDAO.getCount(Tiers.class));
				return null;
			}
		});
	}

	/**
	 * Vérifie que la génération du rapport final fonctionne correctement dans le cas simple (sans rollback)
	 */
	@Test
	public void testGenerationRapportSansRollback() {

		final int count = 50;

		final List<Long> list = new ArrayList<Long>();
		for (int i = 0; i < count; ++i) {
			list.add((long) i);
		}

		final Rapport rapportFinal = new Rapport();

		BatchTransactionTemplate<Long, Rapport> template = new BatchTransactionTemplate<Long, Rapport>(list, 10, Behavior.REPRISE_AUTOMATIQUE, transactionManager, null, hibernateTemplate);
		template.execute(rapportFinal, new BatchCallback<Long, Rapport>() {

			@Override
			public Rapport createSubRapport() {
				return new Rapport();
			}

			@Override
			public boolean doInTransaction(List<Long> batch, Rapport rapport) throws Exception {
				for (Long element : batch) {
					rapport.addTraite(element);
				}
				return true;
			}
		});

		assertEquals(count, rapportFinal.traites.size());
		assertEmpty(rapportFinal.erreurs);
	}

	/**
	 * Vérifie que la génération du rapport final fonctionne correctement avec des rollbacks
	 */
	@Test
	public void testGenerationRapportAvecRollback() {

		final int count = 50;

		final List<Long> list = new ArrayList<Long>();
		for (int i = 0; i < count; ++i) {
			list.add((long) i);
		}

		final Rapport rapportFinal = new Rapport();

		BatchTransactionTemplate<Long, Rapport> template = new BatchTransactionTemplate<Long, Rapport>(list, 10, Behavior.REPRISE_AUTOMATIQUE, transactionManager, null, hibernateTemplate);
		template.execute(rapportFinal, new BatchCallback<Long, Rapport>() {

			@Override
			public Rapport createSubRapport() {
				return new Rapport();
			}

			@Override
			public boolean doInTransaction(List<Long> batch, Rapport rapport) throws Exception {
				for (Long element : batch) {
					rapport.addTraite(element);
					if (element == 13 || element == 23) { // on fait sauter les éléments 13 et 23
						throw new RuntimeException();
					}
					// note : le rapport final ne doit pas contenir les éléments en erreur, même s'ils ont été ajoutés au rapport intermédiaire.
				}

				return true;
			}
		});

		assertEquals(count -2 , rapportFinal.traites.size());
		assertEquals(2 , rapportFinal.erreurs.size());
		assertTrue(rapportFinal.erreurs.contains((long)13));
		assertTrue(rapportFinal.erreurs.contains((long)23));
	}

	private static class Rapport implements BatchResults<Long, Rapport> {

		public Set<Long> traites = new HashSet<Long>();
		public Set<Long> erreurs = new HashSet<Long>();

		public void addTraite(Long element) {
			traites.add(element);
		}

		public void addErrorException(Long element, Exception e) {
			erreurs.add(element);
		}

		public void addAll(Rapport right) {
			this.traites.addAll(right.traites);
			this.erreurs.addAll(right.erreurs);
		}
	}
}
