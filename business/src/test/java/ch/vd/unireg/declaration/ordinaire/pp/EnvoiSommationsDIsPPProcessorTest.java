package ch.vd.unireg.declaration.ordinaire.pp;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.declaration.EtatDeclarationSommee;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionService;
import ch.vd.unireg.parametrage.DelaisService;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeDocumentEmolument;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

public class EnvoiSommationsDIsPPProcessorTest extends BusinessTest {

	private EnvoiSommationsDIsPPProcessor processor;
	private DeclarationImpotOrdinaireDAO diDao;
	private DeclarationImpotService diService;
	private DelaisService delaisService;
	private AssujettissementService assujettissementService;
	private PeriodeImpositionService periodeImpositionService;
	private AdresseService adresseService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		delaisService = getBean(DelaisService.class, "delaisService");
		diService = getBean(DeclarationImpotService.class, "diService");
		diDao = getBean(DeclarationImpotOrdinaireDAO.class, "diDAO");
		assujettissementService = getBean(AssujettissementService.class, "assujettissementService");
		periodeImpositionService = getBean(PeriodeImpositionService.class, "periodeImpositionService");
		adresseService = getBean(AdresseService.class, "adresseService");
		processor = new EnvoiSommationsDIsPPProcessor(hibernateTemplate, diDao, delaisService, diService, tiersService, transactionManager, assujettissementService, periodeImpositionService, adresseService);
	}

	@Test
	public void testDiRetournee() throws Exception {

		final long diId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Jacques", "Cartier", RegDate.get(1980, 1, 5), Sexe.MASCULIN);
			addForPrincipal(pp, RegDate.get(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Aubonne);

			final RegDate dateEmission = RegDate.get(2009, 1, 15);
			final RegDate dateDelaiInitial = RegDate.get(2009, 3, 15);
			final PeriodeFiscale periode = addPeriodeFiscale(2008);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
			final DeclarationImpotOrdinaire declaration = addDeclarationImpot(pp, periode, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			addEtatDeclarationEmise(declaration, dateEmission);
			addEtatDeclarationRetournee(declaration, dateDelaiInitial.addDays(5), "TEST");   // oui, le retour est après le délai initial, mais cela ne doit pas avoir d'influence
			addDelaiDeclaration(declaration, dateEmission, dateDelaiInitial, EtatDelaiDocumentFiscal.ACCORDE);
			return declaration.getId();
		});

		final EnvoiSommationsDIsPPResults results = processor.run(RegDate.get(), false, 0, null);
		Assert.assertEquals("La DI n'aurait même pas dû être vue", 0, results.getTotalDisTraitees());

		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(0, results.getTotalSommations(2008));
		Assert.assertEquals(0, results.getTotalIndigent());
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisOptionnelles());
	}

	/**
	 * [SIFISC-5208] On s'assure qu'une déclaration retournée plusieurs fois, n'est pas sommée
	 */
	@Test
	public void testDiRetourneePlusieursFois() throws Exception {

		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Jacques", "Cartier", RegDate.get(1980, 1, 5), Sexe.MASCULIN);
			addForPrincipal(pp, RegDate.get(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Aubonne);

			final RegDate dateEmission = RegDate.get(2009, 1, 15);
			final RegDate dateDelaiInitial = RegDate.get(2009, 3, 15);
			final PeriodeFiscale periode = addPeriodeFiscale(2008);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
			final DeclarationImpotOrdinaire declaration = addDeclarationImpot(pp, periode, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			addEtatDeclarationEmise(declaration, dateEmission);
			addEtatDeclarationRetournee(declaration, dateDelaiInitial.addDays(-5), "ADDI");
			addEtatDeclarationRetournee(declaration, dateDelaiInitial.addDays(5), "TEST");    // oui, le retour est après le délai initial, mais cela ne doit pas avoir d'influence
			addDelaiDeclaration(declaration, dateEmission, dateDelaiInitial, EtatDelaiDocumentFiscal.ACCORDE);
			return declaration.getId();
		});

		final EnvoiSommationsDIsPPResults results = processor.run(RegDate.get(), false, 0, null);
		Assert.assertEquals("La DI n'aurait même pas dû être vue", 0, results.getTotalDisTraitees());

		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(0, results.getTotalSommations(2008));
		Assert.assertEquals(0, results.getTotalIndigent());
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisOptionnelles());
	}

	@Test
	public void testDiPasEncoreSommable() throws Exception {

		final int anneePf = 2008;
		final RegDate dateEmission = RegDate.get(2009, 1, 15);
		final RegDate delaiInitial = RegDate.get(2009, 3, 15);

		final long diId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Jacques", "Cartier", RegDate.get(1980, 1, 5), Sexe.MASCULIN);
			addForPrincipal(pp, RegDate.get(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Aubonne);

			final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
			final DeclarationImpotOrdinaire declaration = addDeclarationImpot(pp, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			addEtatDeclarationEmise(declaration, dateEmission);
			addDelaiDeclaration(declaration, dateEmission, delaiInitial, EtatDelaiDocumentFiscal.ACCORDE);
			return declaration.getId();
		});

		final RegDate dateTraitement = delaiInitial.addDays(5);
		final EnvoiSommationsDIsPPResults results = processor.run(dateTraitement, false, 0, null);
		Assert.assertEquals(1, results.getTotalDisTraitees());
		Assert.assertEquals(1, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalIndigent());
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisOptionnelles());
	}

	@Test
	public void testDiSommableSimple() throws Exception {

		final int anneePf = 2008;
		final RegDate dateEmission = RegDate.get(2009, 1, 15);
		final RegDate delaiInitial = RegDate.get(2009, 3, 15);

		final long diId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Jacques", "Cartier", RegDate.get(1980, 1, 5), Sexe.MASCULIN);
			addForPrincipal(pp, RegDate.get(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Aubonne);

			final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
			final DeclarationImpotOrdinaire declaration = addDeclarationImpot(pp, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			addEtatDeclarationEmise(declaration, dateEmission);
			addDelaiDeclaration(declaration, dateEmission, delaiInitial, EtatDelaiDocumentFiscal.ACCORDE);
			return declaration.getId();
		});

		final RegDate dateTraitement = delaiInitial.addMonths(1);
		final EnvoiSommationsDIsPPResults results = processor.run(dateTraitement, false, 0, null);
		Assert.assertEquals(1, results.getTotalDisTraitees());
		Assert.assertEquals(1, results.getTotalDisSommees());
		Assert.assertEquals(1, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(0, results.getTotalIndigent());
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisOptionnelles());
	}

	@Test
	public void testDateEnvoiCourrierDiSommee() throws Exception {

		final int anneePf = 2008;
		final RegDate dateEmission = RegDate.get(2009, 1, 15);
		final RegDate delaiInitial = RegDate.get(2009, 3, 18);

		final long diId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Jacques", "Cartier", RegDate.get(1980, 1, 5), Sexe.MASCULIN);
			addForPrincipal(pp, RegDate.get(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Aubonne);

			final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
			final DeclarationImpotOrdinaire declaration = addDeclarationImpot(pp, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			addEtatDeclarationEmise(declaration, dateEmission);
			addDelaiDeclaration(declaration, dateEmission, delaiInitial, EtatDelaiDocumentFiscal.ACCORDE);
			return declaration.getId();
		});

		final RegDate dateTraitement = delaiInitial.addMonths(1);
		final EnvoiSommationsDIsPPResults results = processor.run(dateTraitement, false, 0, null);

		doInNewTransactionAndSession(status -> {
			final DeclarationImpotOrdinaire declarationImpotOrdinaire = diDao.get(diId);
			final EtatDeclarationSommee etatSomme = (EtatDeclarationSommee) declarationImpotOrdinaire.getDernierEtatDeclaration();

			Assert.assertEquals(dateTraitement, etatSomme.getDateObtention());

			final RegDate dateEnvoiCourrier = dateTraitement.addDays(3);
			Assert.assertEquals(dateEnvoiCourrier, etatSomme.getDateEnvoiCourrier());
			return null;
		});
	}

	@Test
	public void testDiSommableMaisIndigent() throws Exception {
		final int anneePf = 2008;
		final RegDate dateEmission = RegDate.get(2009, 1, 15);
		final RegDate delaiInitial = RegDate.get(2009, 3, 15);

		final long diId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Jacques", "Cartier", RegDate.get(1980, 1, 5), Sexe.MASCULIN);
			addForPrincipal(pp, RegDate.get(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Aubonne, ModeImposition.INDIGENT);

			final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
			final DeclarationImpotOrdinaire declaration = addDeclarationImpot(pp, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			addEtatDeclarationEmise(declaration, dateEmission);
			addDelaiDeclaration(declaration, dateEmission, delaiInitial, EtatDelaiDocumentFiscal.ACCORDE);
			return declaration.getId();
		});

		final RegDate dateTraitement = delaiInitial.addMonths(1);
		final EnvoiSommationsDIsPPResults results = processor.run(dateTraitement, false, 0, null);
		Assert.assertEquals(1, results.getTotalDisTraitees());
		Assert.assertEquals(0, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalSommations(anneePf));
		Assert.assertEquals(1, results.getTotalIndigent());
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisOptionnelles());
	}

	@Test
	public void testDiSommableMaisNonAssujetti() throws Exception {
		final int anneePf = 2008;
		final RegDate dateEmission = RegDate.get(2009, 1, 15);
		final RegDate delaiInitial = RegDate.get(2009, 3, 15);

		final long diId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Jacques", "Cartier", RegDate.get(1980, 1, 5), Sexe.MASCULIN);
			final ForFiscalPrincipal ffp = addForPrincipal(pp, RegDate.get(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Aubonne);
			ffp.setAnnule(true);

			final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
			final DeclarationImpotOrdinaire declaration = addDeclarationImpot(pp, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			addEtatDeclarationEmise(declaration, dateEmission);
			addDelaiDeclaration(declaration, dateEmission, delaiInitial, EtatDelaiDocumentFiscal.ACCORDE);
			return declaration.getId();
		});

		final RegDate dateTraitement = delaiInitial.addMonths(1);
		final EnvoiSommationsDIsPPResults results = processor.run(dateTraitement, false, 0, null);
		Assert.assertEquals(1, results.getTotalDisTraitees());
		Assert.assertEquals(0, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalIndigent());
		Assert.assertEquals(0, results.getTotalSourcierPur());
		Assert.assertEquals(1, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisOptionnelles());
	}

	@Test
	public void testDiSommableMaisOptionnelle() throws Exception {
		final int anneePf = 2008;
		final RegDate dateEmission = RegDate.get(2009, 1, 15);
		final RegDate delaiInitial = RegDate.get(2009, 3, 15);

		final long diId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Jacques", "Cartier", RegDate.get(1980, 1, 5), Sexe.MASCULIN);
			addForPrincipal(pp, RegDate.get(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockPays.France);
			addForSecondaire(pp, RegDate.get(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne, MotifRattachement.IMMEUBLE_PRIVE);

			final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
			final DeclarationImpotOrdinaire declaration = addDeclarationImpot(pp, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			addEtatDeclarationEmise(declaration, dateEmission);
			addDelaiDeclaration(declaration, dateEmission, delaiInitial, EtatDelaiDocumentFiscal.ACCORDE);
			return declaration.getId();
		});

		final RegDate dateTraitement = delaiInitial.addMonths(1);
		final EnvoiSommationsDIsPPResults results = processor.run(dateTraitement, false, 0, null);
		Assert.assertEquals(1, results.getTotalDisTraitees());
		Assert.assertEquals(0, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalIndigent());
		Assert.assertEquals(0, results.getTotalSourcierPur());
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(1, results.getTotalDisOptionnelles());
	}

	@Test
	public void testDiSommableEtPartiellementOptionnelle() throws Exception {
		final int anneePf = 2008;
		final RegDate dateEmission = RegDate.get(2009, 1, 15);
		final RegDate delaiInitial = RegDate.get(2009, 3, 15);

		final long diId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Jacques", "Cartier", RegDate.get(1980, 1, 5), Sexe.MASCULIN);
			addForPrincipal(pp, RegDate.get(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, RegDate.get(anneePf, 5, 31), MotifFor.ARRIVEE_HS, MockPays.France);
			addForPrincipal(pp, RegDate.get(anneePf, 6, 1), MotifFor.ARRIVEE_HS, MockCommune.Aubonne);
			addForSecondaire(pp, RegDate.get(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne, MotifRattachement.IMMEUBLE_PRIVE);

			final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
			final DeclarationImpotOrdinaire declaration = addDeclarationImpot(pp, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			addEtatDeclarationEmise(declaration, dateEmission);
			addDelaiDeclaration(declaration, dateEmission, delaiInitial, EtatDelaiDocumentFiscal.ACCORDE);
			return declaration.getId();
		});

		final RegDate dateTraitement = delaiInitial.addMonths(1);
		final EnvoiSommationsDIsPPResults results = processor.run(dateTraitement, false, 0, null);
		Assert.assertEquals(1, results.getTotalDisTraitees());
		Assert.assertEquals(0, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(1, results.getTotalDisSommees());
		Assert.assertEquals(1, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalIndigent());
		Assert.assertEquals(0, results.getTotalSourcierPur());
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisOptionnelles());
	}



	//UNIREG-2466 test sur le log correcte des erreurs notamment les NullPointerException
	@Test
	public void testErreurSommation() throws Exception {

		final int anneePf = 2009;
		final RegDate dateEmission = RegDate.get(2009, 1, 15);
		final RegDate delaiInitial = RegDate.get(2010, 6, 30);

		final long diId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("arben Jakupi", "Cartier", RegDate.get(1982, 10, 15), Sexe.MASCULIN);

			addForPrincipalSource(pp, RegDate.get(2002, 1, 1), MotifFor.ARRIVEE_HS, RegDate.get(2009, 3, 31), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Aubonne.getNoOFS());
			addForPrincipal(pp, RegDate.get(anneePf, 4, 1), MotifFor.INDETERMINE, MockCommune.Aubonne);

			final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode);
			final DeclarationImpotOrdinaire declaration = addDeclarationImpot(pp, periode, date(anneePf, 4, 1), date(anneePf, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			addEtatDeclarationEmise(declaration, dateEmission);
			addDelaiDeclaration(declaration, dateEmission, delaiInitial, EtatDelaiDocumentFiscal.ACCORDE);
			return declaration.getId();
		});

		processor = new EnvoiSommationsDIsPPProcessor(hibernateTemplate, diDao, delaisService, diService, tiersService, transactionManager, assujettissementService, periodeImpositionService, adresseService) {
			@Override
			protected void traiterDI(DeclarationImpotOrdinairePP di, EnvoiSommationsDIsPPResults r, RegDate dateTraitement, boolean miseSousPliImpossible) {
				throw new RuntimeException("Exception de test");
			}
		};

		final RegDate dateTraitement = delaiInitial.addMonths(1);
		final EnvoiSommationsDIsPPResults results = processor.run(dateTraitement, false, 0, null);
		final List<EnvoiSommationsDIsPPResults.ErrorInfo> infoListErreur = results.getListeSommationsEnErreur();
		Assert.assertEquals(1, infoListErreur.size());

		doInNewTransactionAndSession(status -> {
			final DeclarationImpotOrdinaire declaration = diDao.get(diId);

			final EnvoiSommationsDIsPPResults.ErrorInfo error = infoListErreur.get(0);
			Assert.assertEquals(declaration.getTiers().getNumero(), error.getNumeroTiers());
			Assert.assertEquals("java.lang.RuntimeException - Exception de test", error.getCause());
			return null;
		});

		Assert.assertEquals(1, results.getTotalDisTraitees());
		Assert.assertEquals(0, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalIndigent());
		Assert.assertEquals(0, results.getTotalSourcierPur());
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalDisOptionnelles());
	}

	@Test
	public void testDiContribuableSourcierPur() throws Exception {
		final int anneePf = 2008;
		final RegDate dateEmission = RegDate.get(2009, 1, 15);
		final RegDate delaiInitial = RegDate.get(2009, 3, 15);

		final long diId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Jacques", "Cartier", RegDate.get(1980, 1, 5), Sexe.MASCULIN);

			addForPrincipalSource(pp, RegDate.get(anneePf, 6, 1), MotifFor.ARRIVEE_HS, null, null, MockCommune.Aubonne.getNoOFS());

			final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
			final DeclarationImpotOrdinaire declaration = addDeclarationImpot(pp, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			addEtatDeclarationEmise(declaration, dateEmission);
			addDelaiDeclaration(declaration, dateEmission, delaiInitial, EtatDelaiDocumentFiscal.ACCORDE);
			return declaration.getId();
		});

		final RegDate dateTraitement = delaiInitial.addMonths(1);
		final EnvoiSommationsDIsPPResults results = processor.run(dateTraitement, false, 0, null);
		Assert.assertEquals(1, results.getTotalDisTraitees());
		Assert.assertEquals(0, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalIndigent());
		Assert.assertEquals(1, results.getTotalSourcierPur());
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalDisOptionnelles());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
	}

	/**
	 * C'est un cas qui se produit beaucoup avec les LR des débiteurs "web" : ils envoient la LR avant même qu'on leur demande...
	 * Par acquis de conscience, on fait aussi le test pour les DIs
	 */
	@Test
	public void testNonSommationDiRetourneeAvantEmission() throws Exception {
		final int anneePf = 2008;
		final RegDate dateEmission = RegDate.get(2009, 1, 15);
		final RegDate delaiInitial = RegDate.get(2009, 3, 15);

		final long diId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Jacques", "Cartier", RegDate.get(1980, 1, 5), Sexe.MASCULIN);
			addForPrincipal(pp, RegDate.get(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, RegDate.get(anneePf, 5, 31), MotifFor.ARRIVEE_HS, MockPays.France);
			addForPrincipal(pp, RegDate.get(anneePf, 6, 1), MotifFor.ARRIVEE_HS, MockCommune.Aubonne);
			addForSecondaire(pp, RegDate.get(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne, MotifRattachement.IMMEUBLE_PRIVE);

			final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
			final DeclarationImpotOrdinaire declaration = addDeclarationImpot(pp, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			addEtatDeclarationEmise(declaration, dateEmission);
			addEtatDeclarationRetournee(declaration, dateEmission.addDays(-5), "TEST");
			addDelaiDeclaration(declaration, dateEmission, delaiInitial, EtatDelaiDocumentFiscal.ACCORDE);
			return declaration.getId();
		});

		final RegDate dateTraitement = delaiInitial.addYears(1);
		final EnvoiSommationsDIsPPResults results = processor.run(dateTraitement, false, 0, null);
		Assert.assertEquals(0, results.getTotalDisTraitees());
		Assert.assertEquals(0, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalIndigent());
		Assert.assertEquals(0, results.getTotalSourcierPur());
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisOptionnelles());
	}

	@Test
	public void testNonSommationDiDejaSommee() throws Exception {
		final int anneePf = 2008;
		final RegDate dateEmission = RegDate.get(2009, 1, 15);
		final RegDate delaiInitial = RegDate.get(2009, 3, 15);

		final long diId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Jacques", "Cartier", RegDate.get(1980, 1, 5), Sexe.MASCULIN);
			addForPrincipal(pp, RegDate.get(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, RegDate.get(anneePf, 5, 31), MotifFor.ARRIVEE_HS, MockPays.France);
			addForPrincipal(pp, RegDate.get(anneePf, 6, 1), MotifFor.ARRIVEE_HS, MockCommune.Aubonne);
			addForSecondaire(pp, RegDate.get(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne, MotifRattachement.IMMEUBLE_PRIVE);

			final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
			final DeclarationImpotOrdinaire declaration = addDeclarationImpot(pp, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			addEtatDeclarationEmise(declaration, dateEmission);
			addEtatDeclarationSommee(declaration, delaiInitial.addMonths(1), delaiInitial.addMonths(1).addDays(3), null);
			addDelaiDeclaration(declaration, dateEmission, delaiInitial, EtatDelaiDocumentFiscal.ACCORDE);
			return declaration.getId();
		});

		final RegDate dateTraitement = delaiInitial.addYears(1);
		final EnvoiSommationsDIsPPResults results = processor.run(dateTraitement, false, 0, null);
		Assert.assertEquals(0, results.getTotalDisTraitees());
		Assert.assertEquals(0, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalIndigent());
		Assert.assertEquals(0, results.getTotalSourcierPur());
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisOptionnelles());
	}

	@Test
	public void testSommationDiSurMenageAvecMembresInconnus() throws Exception {

		class Ids {
			final long mcId;
			final long diId;
			Ids(long mcId, long diId) {
				this.mcId = mcId;
				this.diId = diId;
			}
		}

		// pas de validation : nécessaire pour créer le for sur un ménage commun sans appartenance ménage existante
		final Ids ids = doInNewTransactionAndSessionWithoutValidation(status -> {
			final MenageCommun mc = hibernateTemplate.merge(new MenageCommun());
			addForPrincipal(mc, RegDate.get(2008, 1, 1), MotifFor.ARRIVEE_HS, RegDate.get(2008, 12, 31), MotifFor.ANNULATION, MockCommune.Aubonne);

			final RegDate dateEmission = RegDate.get(2009, 1, 15);
			final RegDate dateDelaiInitial = RegDate.get(2009, 3, 15);
			final PeriodeFiscale periode = addPeriodeFiscale(2008);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
			final DeclarationImpotOrdinaire declaration = addDeclarationImpot(mc, periode, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			addEtatDeclarationEmise(declaration, dateEmission);
			addDelaiDeclaration(declaration, dateEmission, dateDelaiInitial, EtatDelaiDocumentFiscal.ACCORDE);
			return new Ids(mc.getId(), declaration.getId());
		});

		final EnvoiSommationsDIsPPResults results = processor.run(RegDate.get(), false, 0, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getSommations().size());

		final List<EnvoiSommationsDIsPPResults.ErrorInfo> erreurs = results.getListeSommationsEnErreur();
		Assert.assertNotNull(erreurs);
		Assert.assertEquals(1, erreurs.size());

		final String expectedError = String.format("La di [id: %d] n'a pas été sommée car le contribuable [%s] est un ménage commun dont les membres sont inconnus", ids.diId, ids.mcId);
		final EnvoiSommationsDIsPPResults.ErrorInfo erreur = erreurs.get(0);
		Assert.assertNotNull(erreur);
		Assert.assertEquals((Long) ids.mcId, erreur.getNumeroTiers());
		Assert.assertEquals(expectedError, erreur.getCause());
	}

	@Test
	public void testSommationAvecEmolument() throws Exception {

		// 3 sommations : une sans émolument, une avec un émolument à 30 frs, une autre encore avec un émolument à 50 frs
		final int pfSansEmolument = 2014;
		final int pfAvecEmolument1 = 2013;
		final int pfAvecEmolument2 = 2012;
		final int emolument1 = 30;
		final int emolument2 = 50;

		// mise en place
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Aristolius", "Flamingo", date(1954, 7, 2), Sexe.MASCULIN);
			addForPrincipal(pp, date(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

			final PeriodeFiscale pfSans = addPeriodeFiscale(pfSansEmolument);
			pfSans.getParametrePeriodeFiscaleEmolument(TypeDocumentEmolument.SOMMATION_DI_PP).setMontant(null);
			final PeriodeFiscale pfAvec1 = addPeriodeFiscale(pfAvecEmolument1);
			pfAvec1.getParametrePeriodeFiscaleEmolument(TypeDocumentEmolument.SOMMATION_DI_PP).setMontant(emolument1);
			final PeriodeFiscale pfAvec2 = addPeriodeFiscale(pfAvecEmolument2);
			pfAvec2.getParametrePeriodeFiscaleEmolument(TypeDocumentEmolument.SOMMATION_DI_PP).setMontant(emolument2);

			final ModeleDocument mdSans = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pfSans);
			final ModeleDocument mdAvec1 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pfAvec1);
			final ModeleDocument mdAvec2 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pfAvec2);

			final DeclarationImpotOrdinairePP diSans = addDeclarationImpot(pp, pfSans, date(pfSansEmolument, 1, 1), date(pfSansEmolument, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, mdSans);
			addEtatDeclarationEmise(diSans, date(pfSansEmolument + 1, 1, 7));
			addDelaiDeclaration(diSans, date(pfSansEmolument + 1, 1, 7), date(pfSansEmolument + 1, 6, 30), EtatDelaiDocumentFiscal.ACCORDE);

			final DeclarationImpotOrdinairePP diAvec1 = addDeclarationImpot(pp, pfAvec1, date(pfAvecEmolument1, 1, 1), date(pfAvecEmolument1, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, mdAvec1);
			addEtatDeclarationEmise(diAvec1, date(pfAvecEmolument1 + 1, 1, 7));
			addDelaiDeclaration(diAvec1, date(pfAvecEmolument1 + 1, 1, 7), date(pfAvecEmolument1 + 1, 6, 30), EtatDelaiDocumentFiscal.ACCORDE);

			final DeclarationImpotOrdinairePP diAvec2 = addDeclarationImpot(pp, pfAvec2, date(pfAvecEmolument2, 1, 1), date(pfAvecEmolument2, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, mdAvec2);
			addEtatDeclarationEmise(diAvec2, date(pfAvecEmolument2 + 1, 1, 7));
			addDelaiDeclaration(diAvec2, date(pfAvecEmolument2 + 1, 1, 7), date(pfAvecEmolument2 + 1, 6, 30), EtatDelaiDocumentFiscal.ACCORDE);
			return pp.getNumero();
		});

		// lancement des sommations
		final EnvoiSommationsDIsPPResults results = processor.run(RegDate.get(), false, 0, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(3, results.getSommations().size());     // les trois DI sont sommées

		// vérification des émoluments associés aux états "sommée"
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			Assert.assertNotNull(pp);

			{
				final DeclarationImpotOrdinairePP di = pp.getDeclarationActiveAt(date(pfSansEmolument, 1, 1));
				Assert.assertNotNull(di);
				final EtatDeclaration etat = di.getDernierEtatDeclaration();
				Assert.assertNotNull(etat);
				Assert.assertFalse(etat.isAnnule());
				Assert.assertEquals(TypeEtatDocumentFiscal.SOMME, etat.getEtat());
				final EtatDeclarationSommee sommation = (EtatDeclarationSommee) etat;
				Assert.assertNull(sommation.getEmolument());
			}
			{
				final DeclarationImpotOrdinairePP di = pp.getDeclarationActiveAt(date(pfAvecEmolument1, 1, 1));
				Assert.assertNotNull(di);
				final EtatDeclaration etat = di.getDernierEtatDeclaration();
				Assert.assertNotNull(etat);
				Assert.assertFalse(etat.isAnnule());
				Assert.assertEquals(TypeEtatDocumentFiscal.SOMME, etat.getEtat());
				final EtatDeclarationSommee sommation = (EtatDeclarationSommee) etat;
				Assert.assertEquals((Integer) emolument1, sommation.getEmolument());
			}
			{
				final DeclarationImpotOrdinairePP di = pp.getDeclarationActiveAt(date(pfAvecEmolument2, 1, 1));
				Assert.assertNotNull(di);
				final EtatDeclaration etat = di.getDernierEtatDeclaration();
				Assert.assertNotNull(etat);
				Assert.assertFalse(etat.isAnnule());
				Assert.assertEquals(TypeEtatDocumentFiscal.SOMME, etat.getEtat());
				final EtatDeclarationSommee sommation = (EtatDeclarationSommee) etat;
				Assert.assertEquals((Integer) emolument2, sommation.getEmolument());
			}
			return null;
		});
	}
}
