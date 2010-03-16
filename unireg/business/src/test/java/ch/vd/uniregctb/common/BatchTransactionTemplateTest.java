package ch.vd.uniregctb.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.PlatformTransactionManager;

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
		BatchTransactionTemplate<Long> template = new BatchTransactionTemplate<Long>(list, 100, Behavior.SANS_REPRISE, transactionManager, null, hibernateTemplate);
		template.execute(new BatchCallback<Long>() {

			@Override
			public void beforeTransaction() {
				fail();
			}

			@Override
			public boolean doInTransaction(List<Long> batch) throws Exception {
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
	public void testSansRepriseSansException() {

		List<Long> list = new ArrayList<Long>();
		list.add(0L);
		list.add(1L);
		list.add(2L);
		list.add(3L);
		list.add(4L);

		assertEquals(0, tiersDAO.getCount(Tiers.class));

		BatchTransactionTemplate<Long> template = new BatchTransactionTemplate<Long>(list, 2, Behavior.SANS_REPRISE, transactionManager, null, hibernateTemplate);
		template.execute(new BatchCallback<Long>() {

			@Override
			public boolean doInTransaction(List<Long> batch) throws Exception {
				assertTrue(batch.size() == 1 || batch.size() == 2);
				addNonHabitant("Test", "Traitement du batch = " + batch, RegDate.get(), Sexe.MASCULIN);
				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				fail("La transaction ne doit pas sauter");
			}
		});

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
	}

	@NotTransactional
	@Test
	public void testSansRepriseAvecException() {

		List<Long> list = new ArrayList<Long>();
		list.add(0L);
		list.add(1L);
		list.add(2L);
		list.add(3L);
		list.add(4L);

		assertEquals(0, tiersDAO.getCount(Tiers.class));

		BatchTransactionTemplate<Long> template = new BatchTransactionTemplate<Long>(list, 2, Behavior.SANS_REPRISE, transactionManager, null, hibernateTemplate);
		template.execute(new BatchCallback<Long>() {

			@Override
			public boolean doInTransaction(List<Long> batch) throws Exception {
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
	}

	@NotTransactional
	@Test
	public void testAvecRepriseSansException() {

		List<Long> list = new ArrayList<Long>();
		list.add(0L);
		list.add(1L);
		list.add(2L);
		list.add(3L);
		list.add(4L);

		assertEquals(0, tiersDAO.getCount(Tiers.class));

		BatchTransactionTemplate<Long> template = new BatchTransactionTemplate<Long>(list, 2, Behavior.REPRISE_AUTOMATIQUE,
				transactionManager, null, hibernateTemplate);
		template.execute(new BatchCallback<Long>() {

			@Override
			public boolean doInTransaction(List<Long> batch) throws Exception {
				assertTrue(batch.size() == 1 || batch.size() == 2);
				addNonHabitant("Test", "Traitement du batch = " + batch, RegDate.get(), Sexe.MASCULIN);
				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				fail("La transaction ne doit pas sauter");
			}
		});

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
	}

	@NotTransactional
	@Test
	public void testAvecRepriseAvecException() {

		List<Long> list = new ArrayList<Long>();
		list.add(0L);
		list.add(1L);
		list.add(2L);
		list.add(3L);
		list.add(4L);

		assertEquals(0, tiersDAO.getCount(Tiers.class));

		BatchTransactionTemplate<Long> template = new BatchTransactionTemplate<Long>(list, 2, Behavior.REPRISE_AUTOMATIQUE,
				transactionManager, null, hibernateTemplate);
		template.execute(new BatchCallback<Long>() {

			boolean dejaRepris = false;

			@Override
			public boolean doInTransaction(List<Long> batch) throws Exception {
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
	}

	@NotTransactional
	@Test
	public void testRollbackOnException() {

		List<Long> list = new ArrayList<Long>();
		list.add(1234L);

		assertEquals(0, tiersDAO.getCount(Tiers.class));

		BatchTransactionTemplate<Long> template = new BatchTransactionTemplate<Long>(list, 2, Behavior.SANS_REPRISE,
				transactionManager, null, hibernateTemplate);
		template.execute(new BatchCallback<Long>() {

			@Override
			public boolean doInTransaction(List<Long> batch) throws Exception {

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

		/*
		 * On vérifie que la base est toujours vide
		 */
		assertEquals(0, tiersDAO.getCount(Tiers.class));
		assertEquals(0, tiersDAO.getCount(DeclarationImpotSource.class));
		assertEquals(0, tiersDAO.getCount(EtatDeclaration.class));
		assertEquals(0, tiersDAO.getCount(DelaiDeclaration.class));
	}

}
