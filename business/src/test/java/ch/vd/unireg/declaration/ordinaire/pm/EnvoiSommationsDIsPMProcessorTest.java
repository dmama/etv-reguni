package ch.vd.unireg.declaration.ordinaire.pm;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.declaration.EtatDeclarationSommee;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionService;
import ch.vd.unireg.parametrage.DelaisService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

public class EnvoiSommationsDIsPMProcessorTest extends BusinessTest {

	private EnvoiSommationsDIsPMProcessor processor;
	private DeclarationImpotOrdinaireDAO diDao;
	private DeclarationImpotService diService;
	private DelaisService delaisService;
	private PeriodeImpositionService periodeImpositionService;
	private AdresseService adresseService;

	private ParametreAppService parametreAppService;
	private Integer delaiAdministratifPM = null;
	private Integer premiereAnneeDI = null;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		delaisService = getBean(DelaisService.class, "delaisService");
		diService = getBean(DeclarationImpotService.class, "diService");
		diDao = getBean(DeclarationImpotOrdinaireDAO.class, "diDAO");
		periodeImpositionService = getBean(PeriodeImpositionService.class, "periodeImpositionService");
		adresseService = getBean(AdresseService.class, "adresseService");
		parametreAppService = getBean(ParametreAppService.class, "parametreAppService");
		processor = new EnvoiSommationsDIsPMProcessor(hibernateTemplate, diDao, delaisService, diService, tiersService, transactionManager, periodeImpositionService, adresseService);

		// dans la vraie application, la première année d'envoi des DI des personnes morales est en 2016
		// mais pour le moment, on a besoin d'un peu plus de marge...
		premiereAnneeDI = parametreAppService.getPremierePeriodeFiscaleDeclarationsPersonnesMorales();
		delaiAdministratifPM = parametreAppService.getDelaiEnvoiSommationDeclarationImpotPM();
		parametreAppService.setPremierePeriodeFiscaleDeclarationsPersonnesMorales(2014);
		parametreAppService.setDelaiEnvoiSommationDeclarationImpotPM(15);
	}

	@Override
	public void onTearDown() throws Exception {
		if (premiereAnneeDI != null) {
			parametreAppService.setPremierePeriodeFiscaleDeclarationsPersonnesMorales(premiereAnneeDI);
			premiereAnneeDI = null;
		}
		if (delaiAdministratifPM != null) {
			parametreAppService.setDelaiEnvoiSommationDeclarationImpotPM(delaiAdministratifPM);
			delaiAdministratifPM = null;
		}
		super.onTearDown();
	}

	@Test
	public void testDiRetournee() throws Exception {

		final RegDate dateDebut = RegDate.get(2000, 1, 1);

		final long diId = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e, dateDebut, null, "Truc machin SA");
			addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
			addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

			final RegDate dateEmission = RegDate.get(2015, 1, 5);
			final RegDate dateDelaiInitial = RegDate.get(2015, 6, 30);
			final PeriodeFiscale periode = addPeriodeFiscale(2014);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, periode);
			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
			final DeclarationImpotOrdinaire declaration = addDeclarationImpot(e, periode, date(2014, 1, 1), date(2014, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			addEtatDeclarationEmise(declaration, dateEmission);
			addEtatDeclarationRetournee(declaration, dateDelaiInitial.addDays(5), "TEST");         // oui, le retour est après le délai initial, mais cela ne doit pas avoir d'influence
			addDelaiDeclaration(declaration, dateEmission, dateDelaiInitial, EtatDelaiDocumentFiscal.ACCORDE);
			return declaration.getId();
		});

		final EnvoiSommationsDIsPMResults results = processor.run(RegDate.get(), null, null);
		Assert.assertEquals("La DI n'aurait même pas dû être vue", 0, results.getTotalDisTraitees());

		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(0, results.getTotalSommations(2014));
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisSuspendues());
	}

	/**
	 * [SIFISC-5208] On s'assure qu'une déclaration retournée plusieurs fois, n'est pas sommée
	 */
	@Test
	public void testDiRetourneePlusieursFois() throws Exception {

		final RegDate dateDebut = RegDate.get(2000, 1, 1);

		doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e, dateDebut, null, "Truc machin SA");
			addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
			addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

			final RegDate dateEmission = RegDate.get(2015, 1, 15);
			final RegDate dateDelaiInitial = RegDate.get(2015, 6, 30);
			final PeriodeFiscale periode = addPeriodeFiscale(2014);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, periode);
			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
			final DeclarationImpotOrdinaire declaration = addDeclarationImpot(e, periode, date(2014, 1, 1), date(2014, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			addEtatDeclarationEmise(declaration, dateEmission);
			addEtatDeclarationRetournee(declaration, dateDelaiInitial.addDays(-5), "ADDI");
			addEtatDeclarationRetournee(declaration, dateDelaiInitial.addDays(5), "TEST");             // oui, le retour est après le délai initial, mais cela ne doit pas avoir d'influence
			addDelaiDeclaration(declaration, dateEmission, dateDelaiInitial, EtatDelaiDocumentFiscal.ACCORDE);
			return declaration.getId();
		});

		final EnvoiSommationsDIsPMResults results = processor.run(RegDate.get(), null, null);
		Assert.assertEquals("La DI n'aurait même pas dû être vue", 0, results.getTotalDisTraitees());

		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(0, results.getTotalSommations(2014));
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisSuspendues());
	}

	@Test
	public void testDiPasEncoreSommable() throws Exception {

		final int anneePf = 2014;
		final RegDate dateDebut = RegDate.get(2000, 1, 1);
		final RegDate dateEmission = RegDate.get(anneePf + 1, 1, 15);
		final RegDate delaiInitial = RegDate.get(anneePf + 1, 3, 15);

		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e, dateDebut, null, "Truc machin SA");
			addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
			addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

			final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, periode);
			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
			final DeclarationImpotOrdinaire declaration = addDeclarationImpot(e, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			addEtatDeclarationEmise(declaration, dateEmission);
			addDelaiDeclaration(declaration, dateEmission, delaiInitial, EtatDelaiDocumentFiscal.ACCORDE);
			return e.getNumero();
		});

		final RegDate dateTraitement = delaiInitial.addDays(5);         // avant l'expiration du délai administratif
		final EnvoiSommationsDIsPMResults results = processor.run(dateTraitement, null, null);
		Assert.assertEquals(1, results.getTotalDisTraitees());
		Assert.assertEquals(1, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisSuspendues());

		final List<EnvoiSommationsDIsPMResults.DelaiEffectifNonEchuInfo> nonEchus = results.getListeDisDelaiEffectifNonEchu();
		Assert.assertNotNull(nonEchus);
		Assert.assertEquals(1, nonEchus.size());
		Assert.assertEquals((Long) pmId, nonEchus.get(0).getNumeroTiers());
	}

	@Test
	public void testDiSommableSimple() throws Exception {

		final int anneePf = 2014;
		final RegDate dateDebut = RegDate.get(2000, 1, 1);
		final RegDate dateEmission = RegDate.get(anneePf + 1, 1, 15);
		final RegDate delaiInitial = RegDate.get(anneePf + 1, 3, 15);

		final long diId = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e, dateDebut, null, "Truc machin SA");
			addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
			addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

			final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, periode);
			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
			final DeclarationImpotOrdinaire declaration = addDeclarationImpot(e, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			addEtatDeclarationEmise(declaration, dateEmission);
			addDelaiDeclaration(declaration, dateEmission, delaiInitial, EtatDelaiDocumentFiscal.ACCORDE);
			return declaration.getId();
		});

		final RegDate dateTraitement = delaiInitial.addMonths(1);       // on a passé le délai administratif de 15 jours...
		final EnvoiSommationsDIsPMResults results = processor.run(dateTraitement, null, null);
		Assert.assertEquals(1, results.getTotalDisTraitees());
		Assert.assertEquals(1, results.getTotalDisSommees());
		Assert.assertEquals(1, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisSuspendues());
	}

	@Test
	public void testDateEnvoiCourrierDiSommee() throws Exception {

		final int anneePf = 2014;
		final RegDate dateDebut = RegDate.get(2000, 1, 1);
		final RegDate dateEmission = RegDate.get(anneePf + 1, 1, 15);
		final RegDate delaiInitial = RegDate.get(anneePf + 1, 3, 18);

		final long diId = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e, dateDebut, null, "Truc machin SA");
			addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
			addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

			final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, periode);
			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
			final DeclarationImpotOrdinaire declaration = addDeclarationImpot(e, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			addEtatDeclarationEmise(declaration, dateEmission);
			addDelaiDeclaration(declaration, dateEmission, delaiInitial, EtatDelaiDocumentFiscal.ACCORDE);
			return declaration.getId();
		});

		final RegDate dateTraitement = delaiInitial.addMonths(1);
		final EnvoiSommationsDIsPMResults results = processor.run(dateTraitement, 0, null);
		Assert.assertEquals(1, results.getTotalDisTraitees());
		Assert.assertEquals(1, results.getTotalDisSommees());
		Assert.assertEquals(1, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisSuspendues());

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
	public void testDiSommableMaisNonAssujetti() throws Exception {

		final int anneePf = 2014;
		final RegDate dateDebut = RegDate.get(2000, 1, 1);
		final RegDate dateEmission = RegDate.get(anneePf + 1, 1, 15);
		final RegDate delaiInitial = RegDate.get(anneePf + 1, 3, 15);

		final long diId = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e, dateDebut, null, "Truc machin SA");
			addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
			addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			final ForFiscalPrincipal ffp = addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);
			ffp.setAnnule(true);

			final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, periode);
			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
			final DeclarationImpotOrdinaire declaration = addDeclarationImpot(e, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			addEtatDeclarationEmise(declaration, dateEmission);
			addDelaiDeclaration(declaration, dateEmission, delaiInitial, EtatDelaiDocumentFiscal.ACCORDE);
			return declaration.getId();
		});

		final RegDate dateTraitement = delaiInitial.addMonths(1);
		final EnvoiSommationsDIsPMResults results = processor.run(dateTraitement, null, null);
		Assert.assertEquals(1, results.getTotalDisTraitees());
		Assert.assertEquals(0, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalSommations(anneePf));
		Assert.assertEquals(1, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisSuspendues());
	}

	//UNIREG-2466 test sur le log correcte des erreurs notamment les NullPointerException
	@Test
	public void testErreurSommation() throws Exception {

		final int anneePf = 2009;
		final RegDate dateDebut = RegDate.get(2000, 1, 1);
		final RegDate dateEmission = RegDate.get(2009, 1, 15);
		final RegDate delaiInitial = RegDate.get(2010, 6, 30);

		final long diId = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e, dateDebut, null, "Truc machin SA");
			addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
			addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

			final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, periode);
			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
			final DeclarationImpotOrdinaire declaration = addDeclarationImpot(e, periode, date(anneePf, 4, 1), date(anneePf, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			addEtatDeclarationEmise(declaration, dateEmission);
			addDelaiDeclaration(declaration, dateEmission, delaiInitial, EtatDelaiDocumentFiscal.ACCORDE);
			return declaration.getId();
		});

		processor = new EnvoiSommationsDIsPMProcessor(hibernateTemplate, diDao, delaisService, diService, tiersService, transactionManager, periodeImpositionService, adresseService) {
			@Override
			protected void traiterDI(DeclarationImpotOrdinairePM di, EnvoiSommationsDIsPMResults r, RegDate dateTraitement) {
				throw new RuntimeException("Exception de test");
			}
		};

		final RegDate dateTraitement = delaiInitial.addMonths(1);
		final EnvoiSommationsDIsPMResults results = processor.run(dateTraitement, null, null);
		final List<EnvoiSommationsDIsPMResults.ErrorInfo> infoListErreur = results.getListeSommationsEnErreur();
		Assert.assertEquals(1, infoListErreur.size());

		doInNewTransactionAndSession(status -> {
			final DeclarationImpotOrdinaire declaration = diDao.get(diId);

			final EnvoiSommationsDIsPMResults.ErrorInfo error = infoListErreur.get(0);
			Assert.assertEquals(declaration.getTiers().getNumero(), error.getNumeroTiers());
			Assert.assertEquals("java.lang.RuntimeException - Exception de test", error.getCause());
			return null;
		});

		Assert.assertEquals(1, results.getTotalDisTraitees());
		Assert.assertEquals(0, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalDisSuspendues());
	}

	/**
	 * C'est un cas qui se produit beaucoup avec les LR des débiteurs "web" : ils envoient la LR avant même qu'on leur demande...
	 * Par acquis de conscience, on fait aussi le test pour les DIs
	 */
	@Test
	public void testNonSommationDiRetourneeAvantEmission() throws Exception {

		final int anneePf = 2014;
		final RegDate dateDebut = RegDate.get(2000, 1, 1);
		final RegDate dateEmission = RegDate.get(anneePf + 1, 1, 15);
		final RegDate delaiInitial = RegDate.get(anneePf + 1, 3, 15);

		final long diId = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e, dateDebut, null, "Truc machin SA");
			addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
			addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

			final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, periode);
			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
			final DeclarationImpotOrdinaire declaration = addDeclarationImpot(e, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			addEtatDeclarationEmise(declaration, dateEmission);
			addEtatDeclarationRetournee(declaration, dateEmission.addDays(-5), "TEST");
			addDelaiDeclaration(declaration, dateEmission, delaiInitial, EtatDelaiDocumentFiscal.ACCORDE);
			return declaration.getId();
		});

		final RegDate dateTraitement = delaiInitial.addYears(1);
		final EnvoiSommationsDIsPMResults results = processor.run(dateTraitement, null, null);
		Assert.assertEquals(0, results.getTotalDisTraitees());
		Assert.assertEquals(0, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisSuspendues());
	}

	@Test
	public void testNonSommationDiDejaSommee() throws Exception {

		final int anneePf = 2014;
		final RegDate dateDebut = RegDate.get(2000, 1, 1);
		final RegDate dateEmission = RegDate.get(anneePf + 1, 1, 15);
		final RegDate delaiInitial = RegDate.get(anneePf + 1, 3, 15);

		final long diId = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e, dateDebut, null, "Truc machin SA");
			addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
			addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

			final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, periode);
			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
			final DeclarationImpotOrdinaire declaration = addDeclarationImpot(e, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			addEtatDeclarationEmise(declaration, dateEmission);
			addEtatDeclarationSommee(declaration, delaiInitial.addMonths(1), delaiInitial.addMonths(1).addDays(3), null);
			addDelaiDeclaration(declaration, dateEmission, delaiInitial, EtatDelaiDocumentFiscal.ACCORDE);
			return declaration.getId();
		});

		final RegDate dateTraitement = delaiInitial.addYears(1);
		final EnvoiSommationsDIsPMResults results = processor.run(dateTraitement, null, null);
		Assert.assertEquals(0, results.getTotalDisTraitees());
		Assert.assertEquals(0, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisSuspendues());
	}

	@Test
	public void testNonSommationDiSuspendue() throws Exception {

		final int anneePf = 2014;
		final RegDate dateDebut = RegDate.get(2000, 1, 1);
		final RegDate dateEmission = RegDate.get(anneePf + 1, 1, 15);
		final RegDate delaiInitial = RegDate.get(anneePf + 1, 3, 15);

		final long diId = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e, dateDebut, null, "Truc machin SA");
			addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
			addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

			final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, periode);
			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
			final DeclarationImpotOrdinaire declaration = addDeclarationImpot(e, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			addEtatDeclarationEmise(declaration, dateEmission);
			addEtatDeclarationSuspendue(declaration, dateEmission.addMonths(2));
			addDelaiDeclaration(declaration, dateEmission, delaiInitial, EtatDelaiDocumentFiscal.ACCORDE);
			return declaration.getId();
		});

		final RegDate dateTraitement = delaiInitial.addYears(1);
		final EnvoiSommationsDIsPMResults results = processor.run(dateTraitement, null, null);
		Assert.assertEquals(1, results.getTotalDisTraitees());
		Assert.assertEquals(0, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(1, results.getTotalDisSuspendues());

		doInNewTransactionAndSession(status -> {
			final DeclarationImpotOrdinairePM di = (DeclarationImpotOrdinairePM) diDao.get(diId);
			Assert.assertNotNull(di);
			Assert.assertNull(di.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.SOMME));
			return null;
		});
	}

	@Test
	public void testSommationMemeSiOptionelle() throws Exception {

		final int anneePf = 2014;
		final RegDate dateDebut = RegDate.get(2000, 1, 1);
		final RegDate dateEmission = RegDate.get(anneePf + 1, 1, 15);
		final RegDate delaiInitial = RegDate.get(anneePf + 1, 3, 15);

		final long diId = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e, dateDebut, null, "Truc machin");
			addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.CORP_DP_ADM);
			addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
			addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
			addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
			addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

			final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_APM_BATCH, periode);
			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
			final DeclarationImpotOrdinaire declaration = addDeclarationImpot(e, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			addEtatDeclarationEmise(declaration, dateEmission);
			addDelaiDeclaration(declaration, dateEmission, delaiInitial, EtatDelaiDocumentFiscal.ACCORDE);
			return declaration.getId();
		});

		final RegDate dateTraitement = delaiInitial.addYears(1);
		final EnvoiSommationsDIsPMResults results = processor.run(dateTraitement, null, null);
		Assert.assertEquals(1, results.getTotalDisTraitees());
		Assert.assertEquals(0, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(1, results.getTotalDisSommees());
		Assert.assertEquals(1, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisSuspendues());

		doInNewTransactionAndSession(status -> {
			final DeclarationImpotOrdinaire declarationImpotOrdinaire = diDao.get(diId);
			final EtatDeclarationSommee etatSomme = (EtatDeclarationSommee) declarationImpotOrdinaire.getDernierEtatDeclaration();
			Assert.assertNotNull(etatSomme);
			Assert.assertEquals(dateTraitement, etatSomme.getDateObtention());

			final RegDate dateEnvoiCourrier = dateTraitement.addDays(3);
			Assert.assertEquals(dateEnvoiCourrier, etatSomme.getDateEnvoiCourrier());
			return null;
		});
	}
}
