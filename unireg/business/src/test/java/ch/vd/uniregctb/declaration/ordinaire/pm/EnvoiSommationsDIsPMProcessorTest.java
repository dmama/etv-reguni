package ch.vd.uniregctb.declaration.ordinaire.pm;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.EtatDeclarationSommee;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MontantMonetaire;
import ch.vd.uniregctb.type.DayMonth;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;

public class EnvoiSommationsDIsPMProcessorTest extends BusinessTest {

	private EnvoiSommationsDIsPMProcessor processor;
	private DeclarationImpotOrdinaireDAO diDao;
	private DeclarationImpotService diService;
	private DelaisService delaisService;
	private PeriodeImpositionService periodeImpositionService;
	private AdresseService adresseService;

	private ParametreAppService parametreAppService;
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
		parametreAppService.setPremierePeriodeFiscaleDeclarationsPersonnesMorales(2014);
	}

	@Override
	public void onTearDown() throws Exception {
		if (premiereAnneeDI != null) {
			parametreAppService.setPremierePeriodeFiscaleDeclarationsPersonnesMorales(premiereAnneeDI);
			premiereAnneeDI = null;
		}
		super.onTearDown();
	}

	@Test
	public void testDiRetournee() throws Exception {

		final RegDate dateDebut = RegDate.get(2000, 1, 1);

		final long diId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final Entreprise e = addEntrepriseInconnueAuCivil();
				addDonneesRegistreCommerce(e, dateDebut, null, "Truc machin SA", FormeJuridiqueEntreprise.SA, new MontantMonetaire(1000000L, MontantMonetaire.CHF));
				addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
				addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

				final RegDate dateEmission = RegDate.get(2015, 1, 5);
				final RegDate dateDelaiInitial = RegDate.get(2015, 6, 30);
				final PeriodeFiscale periode = addPeriodeFiscale(2014);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM, periode);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(e, periode, date(2014, 1, 1), date(2014, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				declaration.addEtat(new EtatDeclarationEmise(dateEmission));
				declaration.addEtat(new EtatDeclarationRetournee(dateDelaiInitial.addDays(5), "TEST"));   // oui, le retour est après le délai initial, mais cela ne doit pas avoir d'influence

				final DelaiDeclaration delai = new DelaiDeclaration();
				delai.setDateDemande(dateEmission);
				delai.setDelaiAccordeAu(dateDelaiInitial);
				declaration.addDelai(delai);

				return declaration.getId();
			}
		});

		final EnvoiSommationsDIsPMResults results = processor.run(RegDate.get(), null, null);
		Assert.assertEquals("La DI n'aurait même pas dû être vue", 0, results.getTotalDisTraitees());

		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(0, results.getTotalSommations(2014));
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisOptionnelles());
	}

	/**
	 * [SIFISC-5208] On s'assure qu'une déclaration retournée plusieurs fois, n'est pas sommée
	 */
	@Test
	public void testDiRetourneePlusieursFois() throws Exception {

		final RegDate dateDebut = RegDate.get(2000, 1, 1);

		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final Entreprise e = addEntrepriseInconnueAuCivil();
				addDonneesRegistreCommerce(e, dateDebut, null, "Truc machin SA", FormeJuridiqueEntreprise.SA, new MontantMonetaire(1000000L, MontantMonetaire.CHF));
				addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
				addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

				final RegDate dateEmission = RegDate.get(2015, 1, 15);
				final RegDate dateDelaiInitial = RegDate.get(2015, 6, 30);
				final PeriodeFiscale periode = addPeriodeFiscale(2014);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM, periode);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(e, periode, date(2014, 1, 1), date(2014, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				declaration.addEtat(new EtatDeclarationEmise(dateEmission));
				declaration.addEtat(new EtatDeclarationRetournee(dateDelaiInitial.addDays(-5), "ADDI"));
				declaration.addEtat(new EtatDeclarationRetournee(dateDelaiInitial.addDays(5), "TEST"));   // oui, le retour est après le délai initial, mais cela ne doit pas avoir d'influence

				final DelaiDeclaration delai = new DelaiDeclaration();
				delai.setDateDemande(dateEmission);
				delai.setDelaiAccordeAu(dateDelaiInitial);
				declaration.addDelai(delai);

				return declaration.getId();
			}
		});

		final EnvoiSommationsDIsPMResults results = processor.run(RegDate.get(), null, null);
		Assert.assertEquals("La DI n'aurait même pas dû être vue", 0, results.getTotalDisTraitees());

		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(0, results.getTotalSommations(2014));
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisOptionnelles());
	}

	@Test
	public void testDiPasEncoreSommable() throws Exception {

		final int anneePf = 2014;
		final RegDate dateDebut = RegDate.get(2000, 1, 1);
		final RegDate dateEmission = RegDate.get(anneePf + 1, 1, 15);
		final RegDate delaiInitial = RegDate.get(anneePf + 1, 3, 15);

		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final Entreprise e = addEntrepriseInconnueAuCivil();
				addDonneesRegistreCommerce(e, dateDebut, null, "Truc machin SA", FormeJuridiqueEntreprise.SA, new MontantMonetaire(1000000L, MontantMonetaire.CHF));
				addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
				addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

				final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM, periode);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(e, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				declaration.addEtat(new EtatDeclarationEmise(dateEmission));

				final DelaiDeclaration delai = new DelaiDeclaration();
				delai.setDateDemande(dateEmission);
				delai.setDelaiAccordeAu(delaiInitial);
				declaration.addDelai(delai);

				return e.getNumero();
			}
		});

		final RegDate dateTraitement = delaiInitial.addDays(5);         // avant l'expiration du délai administratif
		final EnvoiSommationsDIsPMResults results = processor.run(dateTraitement, null, null);
		Assert.assertEquals(1, results.getTotalDisTraitees());
		Assert.assertEquals(1, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisOptionnelles());

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

		final long diId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final Entreprise e = addEntrepriseInconnueAuCivil();
				addDonneesRegistreCommerce(e, dateDebut, null, "Truc machin SA", FormeJuridiqueEntreprise.SA, new MontantMonetaire(1000000L, MontantMonetaire.CHF));
				addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
				addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

				final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM, periode);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(e, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				declaration.addEtat(new EtatDeclarationEmise(dateEmission));

				final DelaiDeclaration delai = new DelaiDeclaration();
				delai.setDateDemande(dateEmission);
				delai.setDelaiAccordeAu(delaiInitial);
				declaration.addDelai(delai);

				return declaration.getId();
			}
		});

		final RegDate dateTraitement = delaiInitial.addMonths(1);       // on a passé le délai administratif de 15 jours...
		final EnvoiSommationsDIsPMResults results = processor.run(dateTraitement, null, null);
		Assert.assertEquals(1, results.getTotalDisTraitees());
		Assert.assertEquals(1, results.getTotalDisSommees());
		Assert.assertEquals(1, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisOptionnelles());
	}

	@Test
	public void testDateEnvoiCourrierDiSommee() throws Exception {

		final int anneePf = 2014;
		final RegDate dateDebut = RegDate.get(2000, 1, 1);
		final RegDate dateEmission = RegDate.get(anneePf + 1, 1, 15);
		final RegDate delaiInitial = RegDate.get(anneePf + 1, 3, 18);

		final long diId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final Entreprise e = addEntrepriseInconnueAuCivil();
				addDonneesRegistreCommerce(e, dateDebut, null, "Truc machin SA", FormeJuridiqueEntreprise.SA, new MontantMonetaire(1000000L, MontantMonetaire.CHF));
				addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
				addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

				final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM, periode);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(e, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				declaration.addEtat(new EtatDeclarationEmise(dateEmission));

				final DelaiDeclaration delai = new DelaiDeclaration();
				delai.setDateDemande(dateEmission);
				delai.setDelaiAccordeAu(delaiInitial);
				declaration.addDelai(delai);

				return declaration.getId();
			}
		});

		final RegDate dateTraitement = delaiInitial.addMonths(1);
		final EnvoiSommationsDIsPMResults results = processor.run(dateTraitement, 0, null);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DeclarationImpotOrdinaire declarationImpotOrdinaire = diDao.get(diId);
				final EtatDeclarationSommee etatSomme = (EtatDeclarationSommee) declarationImpotOrdinaire.getDernierEtat();

				Assert.assertEquals(dateTraitement, etatSomme.getDateObtention());

				final RegDate dateEnvoiCourrier = dateTraitement.addDays(3);
				Assert.assertEquals(dateEnvoiCourrier, etatSomme.getDateEnvoiCourrier());
				return null;
			}
		});
	}

	@Test
	public void testDiSommableMaisNonAssujetti() throws Exception {

		final int anneePf = 2014;
		final RegDate dateDebut = RegDate.get(2000, 1, 1);
		final RegDate dateEmission = RegDate.get(anneePf + 1, 1, 15);
		final RegDate delaiInitial = RegDate.get(anneePf + 1, 3, 15);

		final long diId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final Entreprise e = addEntrepriseInconnueAuCivil();
				addDonneesRegistreCommerce(e, dateDebut, null, "Truc machin SA", FormeJuridiqueEntreprise.SA, new MontantMonetaire(1000000L, MontantMonetaire.CHF));
				addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
				final ForFiscalPrincipal ffp = addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);
				ffp.setAnnule(true);

				final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM, periode);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(e, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				declaration.addEtat(new EtatDeclarationEmise(dateEmission));

				final DelaiDeclaration delai = new DelaiDeclaration();
				delai.setDateDemande(dateEmission);
				delai.setDelaiAccordeAu(delaiInitial);
				declaration.addDelai(delai);

				return declaration.getId();
			}
		});

		final RegDate dateTraitement = delaiInitial.addMonths(1);
		final EnvoiSommationsDIsPMResults results = processor.run(dateTraitement, null, null);
		Assert.assertEquals(1, results.getTotalDisTraitees());
		Assert.assertEquals(0, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalSommations(anneePf));
		Assert.assertEquals(1, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisOptionnelles());
	}

	//UNIREG-2466 test sur le log correcte des erreurs notamment les NullPointerException
	@Test
	public void testErreurSommation() throws Exception {

		final int anneePf = 2009;
		final RegDate dateDebut = RegDate.get(2000, 1, 1);
		final RegDate dateEmission = RegDate.get(2009, 1, 15);
		final RegDate delaiInitial = RegDate.get(2010, 6, 30);

		final long diId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final Entreprise e = addEntrepriseInconnueAuCivil();
				addDonneesRegistreCommerce(e, dateDebut, null, "Truc machin SA", FormeJuridiqueEntreprise.SA, new MontantMonetaire(1000000L, MontantMonetaire.CHF));
				addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
				addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

				final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM, periode);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(e, periode, date(anneePf, 4, 1), date(anneePf, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				declaration.addEtat(new EtatDeclarationEmise(dateEmission));

				final DelaiDeclaration delai = new DelaiDeclaration();
				delai.setDateDemande(dateEmission);
				delai.setDelaiAccordeAu(delaiInitial);
				declaration.addDelai(delai);			

				return declaration.getId();
			}
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

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DeclarationImpotOrdinaire declaration = diDao.get(diId);

				final EnvoiSommationsDIsPMResults.ErrorInfo error = infoListErreur.get(0);
				Assert.assertEquals(declaration.getTiers().getNumero(), error.getNumeroTiers());
				Assert.assertEquals("java.lang.RuntimeException - Exception de test", error.getCause());
				return null;
			}
		});

		Assert.assertEquals(1, results.getTotalDisTraitees());
		Assert.assertEquals(0, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalDisOptionnelles());
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

		final long diId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final Entreprise e = addEntrepriseInconnueAuCivil();
				addDonneesRegistreCommerce(e, dateDebut, null, "Truc machin SA", FormeJuridiqueEntreprise.SA, new MontantMonetaire(1000000L, MontantMonetaire.CHF));
				addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
				addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

				final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM, periode);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(e, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				declaration.addEtat(new EtatDeclarationEmise(dateEmission));
				declaration.addEtat(new EtatDeclarationRetournee(dateEmission.addDays(-5), "TEST"));

				final DelaiDeclaration delai = new DelaiDeclaration();
				delai.setDateDemande(dateEmission);
				delai.setDelaiAccordeAu(delaiInitial);
				declaration.addDelai(delai);

				return declaration.getId();
			}
		});

		final RegDate dateTraitement = delaiInitial.addYears(1);
		final EnvoiSommationsDIsPMResults results = processor.run(dateTraitement, null, null);
		Assert.assertEquals(0, results.getTotalDisTraitees());
		Assert.assertEquals(0, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisOptionnelles());
	}

	@Test
	public void testNonSommationDiDejaSommee() throws Exception {

		final int anneePf = 2014;
		final RegDate dateDebut = RegDate.get(2000, 1, 1);
		final RegDate dateEmission = RegDate.get(anneePf + 1, 1, 15);
		final RegDate delaiInitial = RegDate.get(anneePf + 1, 3, 15);

		final long diId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final Entreprise e = addEntrepriseInconnueAuCivil();
				addDonneesRegistreCommerce(e, dateDebut, null, "Truc machin SA", FormeJuridiqueEntreprise.SA, new MontantMonetaire(1000000L, MontantMonetaire.CHF));
				addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
				addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

				final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM, periode);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(e, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				declaration.addEtat(new EtatDeclarationEmise(dateEmission));
				declaration.addEtat(new EtatDeclarationSommee(delaiInitial.addMonths(1), delaiInitial.addMonths(1).addDays(3)));

				final DelaiDeclaration delai = new DelaiDeclaration();
				delai.setDateDemande(dateEmission);
				delai.setDelaiAccordeAu(delaiInitial);
				declaration.addDelai(delai);

				return declaration.getId();
			}
		});

		final RegDate dateTraitement = delaiInitial.addYears(1);
		final EnvoiSommationsDIsPMResults results = processor.run(dateTraitement, null, null);
		Assert.assertEquals(0, results.getTotalDisTraitees());
		Assert.assertEquals(0, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisOptionnelles());
	}

	@Test
	public void testNonSommationDiSuspendue() throws Exception {

		final int anneePf = 2014;
		final RegDate dateDebut = RegDate.get(2000, 1, 1);
		final RegDate dateEmission = RegDate.get(anneePf + 1, 1, 15);
		final RegDate delaiInitial = RegDate.get(anneePf + 1, 3, 15);

		final long diId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final Entreprise e = addEntrepriseInconnueAuCivil();
				addDonneesRegistreCommerce(e, dateDebut, null, "Truc machin SA", FormeJuridiqueEntreprise.SA, new MontantMonetaire(1000000L, MontantMonetaire.CHF));
				addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
				addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

				final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM, periode);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(e, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				addEtatDeclarationEmise(declaration, dateEmission);
				addEtatDeclarationSuspendue(declaration, dateEmission.addMonths(2));

				final DelaiDeclaration delai = new DelaiDeclaration();
				delai.setDateDemande(dateEmission);
				delai.setDelaiAccordeAu(delaiInitial);
				declaration.addDelai(delai);

				return declaration.getId();
			}
		});

		final RegDate dateTraitement = delaiInitial.addYears(1);
		final EnvoiSommationsDIsPMResults results = processor.run(dateTraitement, null, null);
		Assert.assertEquals(0, results.getTotalDisTraitees());
		Assert.assertEquals(0, results.getTotalDelaisEffectifsNonEchus());
		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisOptionnelles());
	}
}
