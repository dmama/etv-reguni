package ch.vd.uniregctb.activation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.interfaces.model.mock.MockCollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.tiers.AnnuleEtRemplace;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class ActivationServiceTest extends BusinessTest {

	private final static String DB_UNIT_DATA_FILE = "classpath:ch/vd/uniregctb/activation/ActivationServiceTest.xml";

	private ActivationService activationService;
	private TiersService tiersService;
	private TacheDAO tacheDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		activationService = getBean(ActivationService.class, "activationService");
		tiersService = getBean(TiersService.class, "tiersService");
		tacheDAO = getBean(TacheDAO.class, "tacheDAO");
	}

	@Test
	public void testAnnuleTiers() throws Exception {
		loadDatabase(DB_UNIT_DATA_FILE);

		final Tiers tiers = tiersService.getTiers(12600003L);
		final RegDate dateAnnulation = RegDate.get(2010, 1, 1);
		activationService.annuleTiers(tiers, dateAnnulation);
		Assert.assertEquals(dateAnnulation, RegDate.get(tiers.getAnnulationDate()));

		final ForFiscalPrincipal forFiscalPrincipal = tiers.getForFiscalPrincipalAt(dateAnnulation);
		Assert.assertEquals(MotifFor.ANNULATION, forFiscalPrincipal.getMotifFermeture());

		final Tache tache = tacheDAO.get(1L);
		Assert.assertEquals(dateAnnulation, RegDate.get(tache.getAnnulationDate()));
	}

	@Test
	public void testRemplaceTiers() throws Exception {
		loadDatabase(DB_UNIT_DATA_FILE);

		final Tiers tiersRemplace = tiersService.getTiers(12600003L);
		final Tiers tiersRemplacant = tiersService.getTiers(12600009L);
		final RegDate dateRemplacement = RegDate.get(2010, 1, 1);
		activationService.remplaceTiers(tiersRemplace, tiersRemplacant, dateRemplacement);
		Assert.assertEquals(dateRemplacement, RegDate.get(tiersRemplace.getAnnulationDate()));

		final ForFiscalPrincipal forFiscalPrincipal = tiersRemplace.getForFiscalPrincipalAt(dateRemplacement);
		Assert.assertEquals(MotifFor.ANNULATION, forFiscalPrincipal.getMotifFermeture());

		AnnuleEtRemplace annuleEtRemplace = (AnnuleEtRemplace) tiersRemplacant.getRapportObjetValidAt(dateRemplacement, TypeRapportEntreTiers.ANNULE_ET_REMPLACE);
		Assert.assertNotNull(annuleEtRemplace);
		annuleEtRemplace = (AnnuleEtRemplace) tiersRemplace.getRapportSujetValidAt(dateRemplacement, TypeRapportEntreTiers.ANNULE_ET_REMPLACE);
		Assert.assertNotNull(annuleEtRemplace);

		final Tache tache = tacheDAO.get(1L);
		Assert.assertEquals(dateRemplacement, RegDate.get(tache.getAnnulationDate()));
	}

	@Test
	public void testReactiveTiers() throws Exception {
		loadDatabase(DB_UNIT_DATA_FILE);

		final Tiers tiersAnnule = tiersService.getTiers(12600004L);
		final RegDate dateReactivation = RegDate.get(2010, 1, 1);
		activationService.reactiveTiers(tiersAnnule, dateReactivation);
		Assert.assertNull(tiersAnnule.getAnnulationDate());
		Assert.assertNull(tiersAnnule.getAnnulationUser());

		final ForFiscalPrincipal forFiscalPrincipal = tiersAnnule.getForFiscalPrincipalAt(dateReactivation);
		Assert.assertNotNull(forFiscalPrincipal);
		Assert.assertEquals(MotifFor.REACTIVATION, forFiscalPrincipal.getMotifOuverture());
	}

	@Test
	public void testAnnulationInterditePourCauseDeDeclarationUlterieure() throws Exception {

		// mise en place
		final long ppId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Achille", "Talon", date(1948, 1, 26), Sexe.MASCULIN);
				final PeriodeFiscale pf2007 = addPeriodeFiscale(2007);
				final PeriodeFiscale pf2008 = addPeriodeFiscale(2008);
				final ModeleDocument modele2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf2007);
				final ModeleDocument modele2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf2008);
				addCollAdm(MockCollectiviteAdministrative.CEDI);
				addDeclarationImpot(pp, pf2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2007);
				addDeclarationImpot(pp, pf2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2008);
				addForPrincipal(pp, date(2007, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				return pp.getNumero();
			}
		});

		// on ne doit pas pouvoir l'annuler avant le 31.12.2008
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				try {
					activationService.annuleTiers(pp, date(2008, 12, 30));
					Assert.fail("Il devrait être interdit d'annuler le tiers alors qu'il a encore des DI ouvertes");
				}
				catch (ActivationServiceException e) {
					Assert.assertEquals("Il est interdit d'annuler un tiers pour lequel il existe encore des déclarations couvrant une période postérieure à la date d'annulation souhaitée.", e.getMessage());
				}
				return null;
			}
		});

		// mais on doit pouvoir l'annuler après le 31.12.2008 (y compris le jour même)
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				try {
					activationService.annuleTiers(pp, date(2008, 12, 31));
				}
				catch (ActivationServiceException e) {
					throw new RuntimeException(e);
				}

				final ForFiscalPrincipal ff = pp.getForFiscalPrincipalAt(date(2008, 12, 31));
				Assert.assertNotNull(ff);
				Assert.assertEquals(date(2008, 12, 31), ff.getDateFin());
				Assert.assertEquals(MotifFor.ANNULATION, ff.getMotifFermeture());

				Assert.assertTrue(pp.isAnnule());
				return null;
			}
		});
	}

	@Test
	public void testAnnulationAutoriseeAvecDeclarationUlterieureAnnulee() throws Exception {

		// mise en place
		final long ppId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Achille", "Talon", date(1948, 1, 26), Sexe.MASCULIN);
				final PeriodeFiscale pf2007 = addPeriodeFiscale(2007);
				final PeriodeFiscale pf2008 = addPeriodeFiscale(2008);
				final ModeleDocument modele2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf2007);
				final ModeleDocument modele2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf2008);
				addCollAdm(MockCollectiviteAdministrative.CEDI);
				addDeclarationImpot(pp, pf2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2007);
				addDeclarationImpot(pp, pf2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2008);
				addForPrincipal(pp, date(2007, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				return pp.getNumero();
			}
		});

		// on ne doit pas pouvoir l'annuler avant le 31.12.2008
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				try {
					activationService.annuleTiers(pp, date(2008, 12, 30));
					Assert.fail("Il devrait être interdit d'annuler le tiers alors qu'il a encore des DI ouvertes");
				}
				catch (ActivationServiceException e) {
					Assert.assertEquals("Il est interdit d'annuler un tiers pour lequel il existe encore des déclarations couvrant une période postérieure à la date d'annulation souhaitée.", e.getMessage());
				}
				return null;
			}
		});

		// mais on doit pouvoir l'annuler si la DI 2008 est annulée
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				final List<Declaration> declarations2008 = pp.getDeclarationForPeriode(2008);
				Assert.assertNotNull(declarations2008);
				Assert.assertEquals(1, declarations2008.size());
				declarations2008.get(0).setAnnule(true);

				try {
					activationService.annuleTiers(pp, date(2008, 12, 30));
				}
				catch (ActivationServiceException e) {
					throw new RuntimeException(e);
				}

				final ForFiscalPrincipal ff = pp.getForFiscalPrincipalAt(date(2008, 12, 30));
				Assert.assertNotNull(ff);
				Assert.assertEquals(date(2008, 12, 30), ff.getDateFin());
				Assert.assertEquals(MotifFor.ANNULATION, ff.getMotifFermeture());

				Assert.assertTrue(pp.isAnnule());
				return null;
			}
		});
	}

	@Test
	public void testAnnulationInterditePourCauseDeForOuvertApresDateAnnulation() throws Exception {

		final class Ids {
			long achille;
			long yoko;
		}
		final Ids ids = new Ids();

		// mise en place (on essaie avec un for encore ouvert, et un autre déjà refermé)
		doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {

				final PersonnePhysique achile = addNonHabitant("Achille", "Talon", date(1948, 1, 26), Sexe.MASCULIN);
				addForPrincipal(achile, date(2009, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				ids.achille = achile.getNumero();

				final PersonnePhysique yoko = addNonHabitant("Yoko", "Tsuno", date(1970, 4, 12), Sexe.FEMININ);
				addForPrincipal(yoko, date(2009, 1, 1), MotifFor.ARRIVEE_HS, date(2009, 10, 31), MotifFor.DEPART_HS, MockCommune.Bussigny);
				ids.yoko = yoko.getNumero();

				return null;
			}
		});

		// on ne doit pouvoir annuler aucun des deux tiers avant le 31.12.2008
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				final PersonnePhysique achille = (PersonnePhysique) tiersService.getTiers(ids.achille);
				try {
					activationService.annuleTiers(achille, date(2008, 12, 30));
					Assert.fail("Il devrait être interdit d'annuler le tiers alors qu'un for est ouvert après la date d'annulation souhaitée");
				}
				catch (ActivationServiceException e) {
					Assert.assertEquals("Il est interdit d'annuler un tiers pour lequel il existe des fors dont la date d'ouverture ou de fermeture est postérieure à la date d'annulation souhaitée.", e.getMessage());
				}

				final PersonnePhysique yoko = (PersonnePhysique) tiersService.getTiers(ids.yoko);
				try {
					activationService.annuleTiers(yoko, date(2008, 12, 30));
					Assert.fail("Il devrait être interdit d'annuler le tiers alors qu'un for est ouvert après la date d'annulation souhaitée");
				}
				catch (ActivationServiceException e) {
					Assert.assertEquals("Il est interdit d'annuler un tiers pour lequel il existe des fors dont la date d'ouverture ou de fermeture est postérieure à la date d'annulation souhaitée.", e.getMessage());
				}

				return null;
			}
		});

		// mais on doit pouvoir l'annuler si la DI 2008 est annulée
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				// d'abord Achille
				{
					final PersonnePhysique achille = (PersonnePhysique) tiersService.getTiers(ids.achille);
					final Set<ForFiscal> fors = achille.getForsFiscaux();
					Assert.assertNotNull(fors);
					Assert.assertEquals(1, fors.size());

					final ForFiscal ff = fors.iterator().next();
					Assert.assertNotNull(ff);
					ff.setAnnule(true);

					try {
						activationService.annuleTiers(achille, date(2008, 12, 30));
					}
					catch (ActivationServiceException e) {
						throw new RuntimeException(e);
					}

					final ForFiscalPrincipal ffApresAnnulation = achille.getForFiscalPrincipalAt(date(2008, 12, 30));
					Assert.assertNull(ffApresAnnulation);

					Assert.assertTrue(achille.isAnnule());
				}

				// puis Yoko
				{
					final PersonnePhysique yoko = (PersonnePhysique) tiersService.getTiers(ids.yoko);
					final Set<ForFiscal> fors = yoko.getForsFiscaux();
					Assert.assertNotNull(fors);
					Assert.assertEquals(1, fors.size());

					final ForFiscal ff = fors.iterator().next();
					Assert.assertNotNull(ff);
					ff.setAnnule(true);

					try {
						activationService.annuleTiers(yoko, date(2008, 12, 30));
					}
					catch (ActivationServiceException e) {
						throw new RuntimeException(e);
					}

					final ForFiscalPrincipal ffApresAnnulation = yoko.getForFiscalPrincipalAt(date(2008, 12, 30));
					Assert.assertNull(ffApresAnnulation);

					Assert.assertTrue(yoko.isAnnule());
				}

				return null;
			}
		});

	}

	@Test
	public void testAnnulationInterditePourCauseDeForOuvertAvantEtFermeApresDateAnnulation() throws Exception {

		// mise en place
		final long ppId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Achille", "Talon", date(1948, 1, 26), Sexe.MASCULIN);
				addForPrincipal(pp, date(2007, 1, 1), MotifFor.ARRIVEE_HS, date(2008, 12, 31), MotifFor.DEPART_HS, MockCommune.Lausanne);
				return pp.getNumero();
			}
		});

		// on ne doit pas pouvoir l'annuler avant le 31.12.2008
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				try {
					activationService.annuleTiers(pp, date(2008, 12, 30));
					Assert.fail("Il devrait être interdit d'annuler le tiers alors qu'un for est ouvert après la date d'annulation souhaitée");
				}
				catch (ActivationServiceException e) {
					Assert.assertEquals("Il est interdit d'annuler un tiers pour lequel il existe des fors dont la date d'ouverture ou de fermeture est postérieure à la date d'annulation souhaitée.", e.getMessage());
				}
				return null;
			}
		});

		// mais on doit pouvoir l'annuler après le 31.12.2008 (y compris le jour même)
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				try {
					activationService.annuleTiers(pp, date(2008, 12, 31));
				}
				catch (ActivationServiceException e) {
					throw new RuntimeException(e);
				}

				final ForFiscalPrincipal ff = pp.getForFiscalPrincipalAt(date(2008, 12, 31));
				Assert.assertNotNull(ff);
				Assert.assertEquals(date(2008, 12, 31), ff.getDateFin());
				Assert.assertEquals(MotifFor.DEPART_HS, ff.getMotifFermeture());

				Assert.assertTrue(pp.isAnnule());
				return null;
			}
		});
	}

	@Test
	public void testAnnulationAutoriseeSiForOuvertAvantDateAnnulationEtPasEncoreFerme() throws Exception {

		// mise en place
		final long ppId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Achille", "Talon", date(1948, 1, 26), Sexe.MASCULIN);
				addForPrincipal(pp, date(2007, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				return pp.getNumero();
			}
		});

		// on doit pouvoir l'annuler après le 2007.01.01
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				try {
					activationService.annuleTiers(pp, date(2008, 12, 31));
				}
				catch (ActivationServiceException e) {
					throw new RuntimeException(e);
				}

				final ForFiscalPrincipal ff = pp.getForFiscalPrincipalAt(date(2008, 12, 31));
				Assert.assertNotNull(ff);
				Assert.assertEquals(date(2008, 12, 31), ff.getDateFin());
				Assert.assertEquals(MotifFor.ANNULATION, ff.getMotifFermeture());

				Assert.assertTrue(pp.isAnnule());
				return null;
			}
		});
	}

	@Test
	public void testAnnulationAutoriseeSiForOuvertExactementDateAnnulationEtPasEncoreFerme() throws Exception {

		// mise en place
		final long ppId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Achille", "Talon", date(1948, 1, 26), Sexe.MASCULIN);
				addForPrincipal(pp, date(2007, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				return pp.getNumero();
			}
		});

		// on doit pouvoir l'annuler dès le 01.01.2007
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				try {
					activationService.annuleTiers(pp, date(2007, 1, 1));
				}
				catch (ActivationServiceException e) {
					throw new RuntimeException(e);
				}

				final ForFiscalPrincipal ff = pp.getForFiscalPrincipalAt(date(2007, 1, 1));
				Assert.assertNotNull(ff);
				Assert.assertEquals(date(2007, 1, 1), ff.getDateDebut());
				Assert.assertEquals(MotifFor.ARRIVEE_HS, ff.getMotifOuverture());
				Assert.assertEquals(date(2007, 1, 1), ff.getDateFin());
				Assert.assertEquals(MotifFor.ANNULATION, ff.getMotifFermeture());

				Assert.assertTrue(pp.isAnnule());
				return null;
			}
		});
	}

	private static class ForFiscalComparator extends DateRangeComparator<ForFiscal> {
		@Override
		public int compare(ForFiscal o1, ForFiscal o2) {
			final boolean isPrincipal1 = o1 instanceof ForFiscalPrincipal;
			final boolean isPrincipal2 = o2 instanceof ForFiscalPrincipal;
			if (isPrincipal1 == isPrincipal2) {
			    return super.compare(o1, o2);
			}
			else if (isPrincipal1) {
				return -1;
			}
			else {
				return 1;
			}
		}
	}

	@Test
	public void testReactivationTiersAvecPlusieursFors() throws Exception {

		final RegDate dateAnnulation = date(2009, 2, 22);

		// mise en place
		final long ppId = (Long) doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {

				final PersonnePhysique pp = addNonHabitant("Achille", "Talon", date(1948, 1, 26), Sexe.MASCULIN);
				pp.setAnnulationDate(dateAnnulation.asJavaDate());
				pp.setAnnulationUser("momo");
				addForPrincipal(pp, date(1980, 1, 1), MotifFor.ARRIVEE_HS, date(1990, 12, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				addForPrincipal(pp, date(1991, 1, 1), MotifFor.DEMENAGEMENT_VD, dateAnnulation, MotifFor.ANNULATION, MockCommune.Lausanne);
				addForSecondaire(pp, date(1991, 1, 1), MotifFor.ACHAT_IMMOBILIER, dateAnnulation, MotifFor.ANNULATION, MockCommune.Lausanne.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				addForSecondaire(pp, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, dateAnnulation, MotifFor.VENTE_IMMOBILIER, MockCommune.Bussigny.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				addForSecondaire(pp, date(2001, 1, 1), MotifFor.ACHAT_IMMOBILIER, dateAnnulation.addMonths(-1), MotifFor.ANNULATION, MockCommune.Bex.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		// vérification de l'état du tiers
		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				Assert.assertTrue(pp.isAnnule());
				Assert.assertEquals(dateAnnulation.asJavaDate(), pp.getAnnulationDate());

				final Set<ForFiscal> fors = pp.getForsFiscaux();
				Assert.assertNotNull(fors);
				Assert.assertEquals(5, fors.size());

				final List<ForFiscal> forsTries = new ArrayList<ForFiscal>(fors);
				Collections.sort(forsTries, new ForFiscalComparator());

				// ne doit pas être ré-ouvert
				final ForFiscalPrincipal ffDemenagement = (ForFiscalPrincipal) forsTries.get(0);
				Assert.assertNotNull(ffDemenagement);
				Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, ffDemenagement.getMotifFermeture());

				// doit être ré-ouvert
				final ForFiscalPrincipal ffpAnnulation = (ForFiscalPrincipal) forsTries.get(1);
				Assert.assertNotNull(ffpAnnulation);
				Assert.assertEquals(MotifFor.ANNULATION, ffpAnnulation.getMotifFermeture());
				Assert.assertEquals(dateAnnulation, ffpAnnulation.getDateFin());

				// doit être ré-ouvert
				final ForFiscalSecondaire ffsAnnulation = (ForFiscalSecondaire) forsTries.get(2);
				Assert.assertNotNull(ffsAnnulation);
				Assert.assertEquals(MotifFor.ANNULATION, ffsAnnulation.getMotifFermeture());
				Assert.assertEquals(dateAnnulation, ffsAnnulation.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffsAnnulation.getTypeAutoriteFiscale());
				Assert.assertEquals(MockCommune.Lausanne.getNoOFSEtendu(), (int) ffsAnnulation.getNumeroOfsAutoriteFiscale());

				// ne doit pas être ré-ouvert (pas bon motif de fermeture)
				final ForFiscalSecondaire ffsVente = (ForFiscalSecondaire) forsTries.get(3);
				Assert.assertNotNull(ffsVente);
				Assert.assertEquals(MotifFor.VENTE_IMMOBILIER, ffsVente.getMotifFermeture());
				Assert.assertEquals(dateAnnulation, ffsVente.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffsVente.getTypeAutoriteFiscale());
				Assert.assertEquals(MockCommune.Bussigny.getNoOFSEtendu(), (int) ffsVente.getNumeroOfsAutoriteFiscale());

				// ne doit pas être ré-ouvert (pas bonne date de fermeture)
				final ForFiscalSecondaire ffsAnnulationPrecedente = (ForFiscalSecondaire) forsTries.get(4);
				Assert.assertNotNull(ffsAnnulationPrecedente);
				Assert.assertEquals(MotifFor.ANNULATION, ffsAnnulationPrecedente.getMotifFermeture());
				Assert.assertEquals(dateAnnulation.addMonths(-1), ffsAnnulationPrecedente.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffsAnnulationPrecedente.getTypeAutoriteFiscale());
				Assert.assertEquals(MockCommune.Bex.getNoOFSEtendu(), (int) ffsAnnulationPrecedente.getNumeroOfsAutoriteFiscale());

				return null;
			}
		});

		// réactivation et tests
		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {

				final RegDate dateReactivation = dateAnnulation.addMonths(6);
				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
				try {
					activationService.reactiveTiers(pp, dateReactivation);
				}
				catch (ActivationServiceException e) {
					throw new RuntimeException(e);
				}

				Assert.assertFalse(pp.isAnnule());

				final Set<ForFiscal> fors = pp.getForsFiscaux();
				Assert.assertNotNull(fors);
				Assert.assertEquals(7, fors.size());

				final List<ForFiscal> forsTries = new ArrayList<ForFiscal>(fors);
				Collections.sort(forsTries, new ForFiscalComparator());

				final ForFiscalPrincipal ffDemenagement = (ForFiscalPrincipal) forsTries.get(0);
				Assert.assertNotNull(ffDemenagement);
				Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, ffDemenagement.getMotifFermeture());

				final ForFiscalPrincipal ffpAnnulation = (ForFiscalPrincipal) forsTries.get(1);
				Assert.assertNotNull(ffpAnnulation);
				Assert.assertEquals(MotifFor.ANNULATION, ffpAnnulation.getMotifFermeture());
				Assert.assertEquals(dateAnnulation, ffpAnnulation.getDateFin());

				final ForFiscalPrincipal ffpReactivation = (ForFiscalPrincipal) forsTries.get(2);
				Assert.assertNotNull(ffpReactivation);
				Assert.assertEquals(MotifFor.REACTIVATION, ffpReactivation.getMotifOuverture());
				Assert.assertEquals(dateReactivation, ffpReactivation.getDateDebut());
				Assert.assertNull(ffpReactivation.getMotifFermeture());
				Assert.assertNull(ffpReactivation.getDateFin());

				final ForFiscalSecondaire ffsAnnulation = (ForFiscalSecondaire) forsTries.get(3);
				Assert.assertNotNull(ffsAnnulation);
				Assert.assertEquals(MotifFor.ANNULATION, ffsAnnulation.getMotifFermeture());
				Assert.assertEquals(dateAnnulation, ffsAnnulation.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffsAnnulation.getTypeAutoriteFiscale());
				Assert.assertEquals(MockCommune.Lausanne.getNoOFSEtendu(), (int) ffsAnnulation.getNumeroOfsAutoriteFiscale());

				// ne doit pas être ré-ouvert (pas bon motif de fermeture)
				final ForFiscalSecondaire ffsVente = (ForFiscalSecondaire) forsTries.get(4);
				Assert.assertNotNull(ffsVente);
				Assert.assertEquals(MotifFor.VENTE_IMMOBILIER, ffsVente.getMotifFermeture());
				Assert.assertEquals(dateAnnulation, ffsVente.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffsVente.getTypeAutoriteFiscale());
				Assert.assertEquals(MockCommune.Bussigny.getNoOFSEtendu(), (int) ffsVente.getNumeroOfsAutoriteFiscale());

				// ne doit pas être ré-ouvert (pas bonne date de fermeture)
				final ForFiscalSecondaire ffsAnnulationPrecedente = (ForFiscalSecondaire) forsTries.get(5);
				Assert.assertNotNull(ffsAnnulationPrecedente);
				Assert.assertEquals(MotifFor.ANNULATION, ffsAnnulationPrecedente.getMotifFermeture());
				Assert.assertEquals(dateAnnulation.addMonths(-1), ffsAnnulationPrecedente.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffsAnnulationPrecedente.getTypeAutoriteFiscale());
				Assert.assertEquals(MockCommune.Bex.getNoOFSEtendu(), (int) ffsAnnulationPrecedente.getNumeroOfsAutoriteFiscale());

				final ForFiscalSecondaire ffsReactivation = (ForFiscalSecondaire) forsTries.get(6);
				Assert.assertNotNull(ffsReactivation);
				Assert.assertEquals(MotifFor.REACTIVATION, ffsReactivation.getMotifOuverture());
				Assert.assertEquals(dateReactivation, ffsReactivation.getDateDebut());
				Assert.assertNull(ffsReactivation.getMotifFermeture());
				Assert.assertNull(ffsReactivation.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffsReactivation.getTypeAutoriteFiscale());
				Assert.assertEquals(MockCommune.Lausanne.getNoOFSEtendu(), (int) ffsReactivation.getNumeroOfsAutoriteFiscale());

				return null;
			}
		});
	}
}
