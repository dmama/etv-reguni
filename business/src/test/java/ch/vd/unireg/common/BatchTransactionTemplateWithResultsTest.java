package ch.vd.unireg.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchResults;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.declaration.EtatDeclarationEmise;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BatchTransactionTemplateWithResultsTest extends BusinessTest {

	private static class TestJobResults implements BatchResults<Long, TestJobResults> {
		@Override
		public void addErrorException(Long element, Exception e) {
		}

		@Override
		public void addAll(TestJobResults right) {
		}
	}

	@Test
	public void testEmptyList() {
		List<Long> list = Collections.emptyList();
		final TestJobResults rapportFinal = new TestJobResults();
		BatchTransactionTemplateWithResults<Long, TestJobResults> template = new BatchTransactionTemplateWithResults<>(list, 100, Behavior.SANS_REPRISE, transactionManager, null);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, TestJobResults>() {

			@Override
			public void beforeTransaction() {
				fail();
			}

			@Override
			public boolean doInTransaction(List<Long> batch, TestJobResults rapport) throws Exception {
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

			@Override
			public TestJobResults createSubRapport() {
				return new TestJobResults();
			}
		}, null);
	}

	private static String buildNameForPP(List<Long> batchPP) {
		final StringBuilder b = new StringBuilder("Traitement du batch");
		for (Long idx : batchPP) {
			final String str = Long.toString(idx);
			b.append(' ');
			for (char c : str.toCharArray()) {
				b.append((char) (c - '0' + 'A'));
			}
		}
		return b.toString();
	}

	@Test
	public void testSansRepriseSansException() throws Exception {

		List<Long> list = new ArrayList<>();
		list.add(0L);
		list.add(1L);
		list.add(2L);
		list.add(3L);
		list.add(4L);

		assertTiersCountHorsTransaction(0);

		final TestJobResults rapportFinal = new TestJobResults();
		BatchTransactionTemplateWithResults<Long, TestJobResults> template = new BatchTransactionTemplateWithResults<>(list, 2, Behavior.SANS_REPRISE, transactionManager, null);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, TestJobResults>() {

			@Override
			public boolean doInTransaction(List<Long> batch, TestJobResults rapport) throws Exception {
				assertTrue(batch.size() == 1 || batch.size() == 2);
				addNonHabitant("Test", buildNameForPP(batch), RegDate.get(), Sexe.MASCULIN);
				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				fail("La transaction ne doit pas sauter");
			}

			@Override
			public TestJobResults createSubRapport() {
				return new TestJobResults();
			}
		}, null);

		doInNewTransaction(status -> {
			// On vérifie que les batchs ont bien été processés et committés
			final List<PersonnePhysique> lines = allTiersOfType(PersonnePhysique.class);
			Collections.sort(lines, (Comparator<Tiers>) (o1, o2) -> {
				return (int) (o1.getNumero() - o2.getNumero()); // -> ordre naturel d'insertion
			});
			assertEquals(3, lines.size());

			final PersonnePhysique tiers0 = (PersonnePhysique) lines.get(0);
			assertEquals("Traitement du batch A B", tiers0.getNom());
			final PersonnePhysique tiers1 = (PersonnePhysique) lines.get(1);
			assertEquals("Traitement du batch C D", tiers1.getNom());
			final PersonnePhysique tiers2 = (PersonnePhysique) lines.get(2);
			assertEquals("Traitement du batch E", tiers2.getNom());
			return null;
		});
	}

	@Test
	public void testSansRepriseAvecException() throws Exception {

		List<Long> list = new ArrayList<>();
		list.add(0L);
		list.add(1L);
		list.add(2L);
		list.add(3L);
		list.add(4L);

		assertTiersCountHorsTransaction(0);

		final TestJobResults rapportFinal = new TestJobResults();
		BatchTransactionTemplateWithResults<Long, TestJobResults> template = new BatchTransactionTemplateWithResults<>(list, 2, Behavior.SANS_REPRISE, transactionManager, null);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, TestJobResults>() {

			@Override
			public boolean doInTransaction(List<Long> batch, TestJobResults rapport) throws Exception {
				assertTrue(batch.size() == 1 || batch.size() == 2);

				if (batch.get(0).equals(2L)) {
					throw new RuntimeException("Rollback du deuxième batch");
				}

				addNonHabitant("Test", buildNameForPP(batch), RegDate.get(), Sexe.MASCULIN);
				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				assertFalse(willRetry);
			}

			@Override
			public TestJobResults createSubRapport() {
				return new TestJobResults();
			}
		}, null);

		doInNewTransaction(status -> {
			// On vérifie que les batchs ont bien été processés et committés à l'exception du deuxième batch qui a été rollé-back
			final List<PersonnePhysique> lines = allTiersOfType(PersonnePhysique.class);
			Collections.sort(lines, (Comparator<Tiers>) (o1, o2) -> {
				return (int) (o1.getNumero() - o2.getNumero()); // -> ordre naturel d'insertion
			});
			assertEquals(2, lines.size());

			final PersonnePhysique tiers0 = (PersonnePhysique) lines.get(0);
			assertEquals("Traitement du batch A B", tiers0.getNom());
			final PersonnePhysique tiers1 = (PersonnePhysique) lines.get(1);
			assertEquals("Traitement du batch E", tiers1.getNom());
			return null;
		});
	}

	@Test
	public void testAvecRepriseSansException() throws Exception {

		List<Long> list = new ArrayList<>();
		list.add(0L);
		list.add(1L);
		list.add(2L);
		list.add(3L);
		list.add(4L);

		assertTiersCountHorsTransaction(0);

		final TestJobResults rapportFinal = new TestJobResults();
		final BatchTransactionTemplateWithResults<Long, TestJobResults> template = new BatchTransactionTemplateWithResults<>(list, 2, Behavior.REPRISE_AUTOMATIQUE, transactionManager, null);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, TestJobResults>() {

			@Override
			public boolean doInTransaction(List<Long> batch, TestJobResults rapport) throws Exception {
				assertTrue(batch.size() == 1 || batch.size() == 2);
				addNonHabitant("Test", buildNameForPP(batch), RegDate.get(), Sexe.MASCULIN);
				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				fail("La transaction ne doit pas sauter");
			}

			@Override
			public TestJobResults createSubRapport() {
				return new TestJobResults();
			}
		}, null);

		doInNewTransaction(status -> {
			// On vérifie que les batchs ont bien été processés et committés
			final List<PersonnePhysique> lines = allTiersOfType(PersonnePhysique.class);
			Collections.sort(lines, (Comparator<Tiers>) (o1, o2) -> {
				return (int) (o1.getNumero() - o2.getNumero()); // -> ordre naturel d'insertion
			});
			assertEquals(3, lines.size());

			final PersonnePhysique tiers0 = (PersonnePhysique) lines.get(0);
			assertEquals("Traitement du batch A B", tiers0.getNom());
			final PersonnePhysique tiers1 = (PersonnePhysique) lines.get(1);
			assertEquals("Traitement du batch C D", tiers1.getNom());
			final PersonnePhysique tiers2 = (PersonnePhysique) lines.get(2);
			assertEquals("Traitement du batch E", tiers2.getNom());
			return null;
		});
	}

	@Test
	public void testAvecRepriseAvecException() throws Exception {

		List<Long> list = new ArrayList<>();
		list.add(0L);
		list.add(1L);
		list.add(2L);
		list.add(3L);
		list.add(4L);

		assertTiersCountHorsTransaction(0);

		final TestJobResults rapportFinal = new TestJobResults();
		BatchTransactionTemplateWithResults<Long, TestJobResults> template = new BatchTransactionTemplateWithResults<>(list, 2, Behavior.REPRISE_AUTOMATIQUE, transactionManager, null);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, TestJobResults>() {

			boolean dejaRepris = false;

			@Override
			public boolean doInTransaction(List<Long> batch, TestJobResults rapport) throws Exception {
				assertTrue(batch.size() == 1 || batch.size() == 2);

				if (batch.get(0).equals(2L)) {
					throw new RuntimeException("Rollback du deuxième batch");
				}

				addNonHabitant("Test", buildNameForPP(batch), RegDate.get(), Sexe.MASCULIN);
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

			@Override
			public TestJobResults createSubRapport() {
				return new TestJobResults();
			}
		}, null);

		doInNewTransaction(status -> {
			/**
			 * On vérifie que :
			 * <ul>
			 * <li>le premier batch est committé complétement</li>
			 * <li>le deuxième batch est committé partiellement (reprise automatique)</li>
			 * <li>le troisième batch est committé complétement</li>
			 * </ul>
			 */
			final List<PersonnePhysique> lines = allTiersOfType(PersonnePhysique.class);
			Collections.sort(lines, (Comparator<Tiers>) (o1, o2) -> {
				return (int) (o1.getNumero() - o2.getNumero()); // -> ordre naturel d'insertion
			});
			assertEquals(3, lines.size());

			final PersonnePhysique tiers0 = (PersonnePhysique) lines.get(0);
			assertEquals("Traitement du batch A B", tiers0.getNom());
			final PersonnePhysique tiers1 = (PersonnePhysique) lines.get(1);
			assertEquals("Traitement du batch D", tiers1.getNom());
			final PersonnePhysique tiers2 = (PersonnePhysique) lines.get(2);
			assertEquals("Traitement du batch E", tiers2.getNom());
			return null;
		});
	}

	@Test
	public void testRollbackOnException() throws Exception {

		List<Long> list = new ArrayList<>();
		list.add(1234L);

		assertTiersCountHorsTransaction(0);

		final TestJobResults rapportFinal = new TestJobResults();
		BatchTransactionTemplateWithResults<Long, TestJobResults> template = new BatchTransactionTemplateWithResults<>(list, 2, Behavior.SANS_REPRISE, transactionManager, null);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, TestJobResults>() {

			@Override
			public boolean doInTransaction(List<Long> batch, TestJobResults rapport) throws Exception {

				for (Long id : batch) {
					DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
					dpi.setNumero(id);
					dpi.setNom1("nom1");
					dpi.setNom2("nom2");
					dpi = hibernateTemplate.merge(dpi);

					DeclarationImpotSource lr = new DeclarationImpotSource();
					lr.setDateDebut(date(2000, 1, 1));
					lr.setDateFin(date(2000, 12, 31));
					lr.setPeriodicite(PeriodiciteDecompte.ANNUEL);

					EtatDeclaration etatDeclaration = new EtatDeclarationEmise();
					etatDeclaration.setDateObtention(date(2001, 1, 1));
					lr.addEtat(etatDeclaration);

					DelaiDeclaration delaiDeclaration = new DelaiDeclaration();
					delaiDeclaration.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
					delaiDeclaration.setDateTraitement(date(2001, 1, 1));
					delaiDeclaration.setDelaiAccordeAu(date(2001, 6, 1));
					delaiDeclaration.setCleArchivageCourrier(null);
					lr.addDelai(delaiDeclaration);

					lr.setTiers(dpi);
					lr = hibernateTemplate.merge(lr);
				}

				hibernateTemplate.flush();

				if (true) {
					throw new IllegalArgumentException();
				}

				return true;
			}

			@Override
			public TestJobResults createSubRapport() {
				return new TestJobResults();
			}
		}, null);

		doInNewTransaction(status -> {
			/*
			 * On vérifie que la base est toujours vide
			 */
			assertEquals(0, tiersDAO.getCount(DebiteurPrestationImposable.class));
			assertEquals(0, tiersDAO.getCount(PersonnePhysique.class));
			assertEquals(0, tiersDAO.getCount(MenageCommun.class));
			assertEquals(0, tiersDAO.getCount(DeclarationImpotSource.class));
			assertEquals(0, tiersDAO.getCount(EtatDeclaration.class));
			assertEquals(0, tiersDAO.getCount(DelaiDeclaration.class));
			return null;
		});

	}

	private void assertTiersCountHorsTransaction(final int count) throws Exception {
		doInNewTransaction(status -> {
			final List<Tiers> all = allTiersOfType(PersonnePhysique.class, MenageCommun.class, DebiteurPrestationImposable.class);
			assertEquals(count, all.size());
			return null;
		});
	}

	/**
	 * Vérifie que la génération du rapport final fonctionne correctement dans le cas simple (sans rollback)
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGenerationRapportSansRollback() {

		final int count = 50;

		final List<Long> list = new ArrayList<>();
		for (int i = 0; i < count; ++i) {
			list.add((long) i);
		}

		final Rapport rapportFinal = new Rapport();
		final BatchTransactionTemplateWithResults<Long, Rapport> template = new BatchTransactionTemplateWithResults<>(list, 10, Behavior.REPRISE_AUTOMATIQUE, transactionManager, null);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, Rapport>() {

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
		}, null);

		assertEquals(count, rapportFinal.traites.size());
		assertEmpty(rapportFinal.erreurs);
	}

	/**
	 * Vérifie que la génération du rapport final fonctionne correctement avec des rollbacks
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGenerationRapportAvecRollback() {

		final int count = 50;

		final List<Long> list = new ArrayList<>();
		for (int i = 0; i < count; ++i) {
			list.add((long) i);
		}

		final Rapport rapportFinal = new Rapport();
		final BatchTransactionTemplateWithResults<Long, Rapport> template = new BatchTransactionTemplateWithResults<>(list, 10, Behavior.REPRISE_AUTOMATIQUE, transactionManager, null);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, Rapport>() {

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
		}, null);

		assertEquals(count -2 , rapportFinal.traites.size());
		assertEquals(2 , rapportFinal.erreurs.size());
		assertTrue(rapportFinal.erreurs.contains((long)13));
		assertTrue(rapportFinal.erreurs.contains((long)23));
	}

	private static class Rapport implements BatchResults<Long, Rapport> {

		public Set<Long> traites = new HashSet<>();
		public Set<Long> erreurs = new HashSet<>();

		public void addTraite(Long element) {
			traites.add(element);
		}

		@Override
		public void addErrorException(Long element, Exception e) {
			erreurs.add(element);
		}

		@Override
		public void addAll(Rapport right) {
			this.traites.addAll(right.traites);
			this.erreurs.addAll(right.erreurs);
		}
	}
}
