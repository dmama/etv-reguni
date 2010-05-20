package ch.vd.uniregctb.activation;

import java.util.List;
import java.util.Set;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

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
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
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
}
