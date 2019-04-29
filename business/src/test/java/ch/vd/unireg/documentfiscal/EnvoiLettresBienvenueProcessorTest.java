package ch.vd.unireg.documentfiscal;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.MockServiceEntreprise;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEtablissementCivilFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.parametrage.DelaisService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.MontantMonetaire;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;
import ch.vd.unireg.type.TypeEtatEntreprise;
import ch.vd.unireg.type.TypeGenerationEtatEntreprise;
import ch.vd.unireg.type.TypeLettreBienvenue;

public class EnvoiLettresBienvenueProcessorTest extends BusinessTest {

	private EnvoiLettresBienvenueProcessor processor;
	private ParametreAppService paramAppService;
	private DelaisService delaisService;
	private Integer[] seuilLettresBienvenue;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		paramAppService = getBean(ParametreAppService.class, "parametreAppService");
		final AssujettissementService assujettissementService = getBean(AssujettissementService.class, "assujettissementService");
		final AutreDocumentFiscalService autreDocumentFiscalService = getBean(AutreDocumentFiscalService.class, "autreDocumentFiscalService");
		processor = new EnvoiLettresBienvenueProcessor(paramAppService, hibernateTemplate, transactionManager,
		                                               tiersService, assujettissementService, autreDocumentFiscalService);

		delaisService = getBean(DelaisService.class, "delaisService");

		// nécessaire tant que nous sommes avant le 11.06.2016
		seuilLettresBienvenue = paramAppService.getDateDebutEnvoiLettresBienvenue();
		paramAppService.setDateDebutEnvoiLettresBienvenue(new Integer[]{1, 1, 2016});
	}

	@Override
	public void onTearDown() throws Exception {
		if (seuilLettresBienvenue != null) {
			paramAppService.setDateDebutEnvoiLettresBienvenue(seuilLettresBienvenue);
		}
		super.onTearDown();
	}

	@Test
	public void testAssujettiDepuisLongtempsSansLettre() throws Exception {

		final RegDate dateDebut = date(2000, 4, 1);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addRaisonSociale(entreprise, dateDebut, null, "Titi SA");
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
			return entreprise.getNumero();
		});

		// lancement du processeur
		// l'entreprise n'est même pas prise en compte dans la requête initiale car elle n'a pas de nouveau for vaudois
		final EnvoiLettresBienvenueResults results = processor.run(RegDate.get(), 0, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getIgnores().size());
		Assert.assertEquals(0, results.getTraites().size());

		// vérification de la non-émission de lettre
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
			Assert.assertNotNull(entreprise);
			Assert.assertEquals(0, entreprise.getAutresDocumentsFiscaux().size());
			return null;
		});
	}

	@Test
	public void testAssujettiDepuisLongtempsAvecNouveauForSansLettre() throws Exception {

		final RegDate dateDebut = date(2000, 4, 1);
		final RegDate dateAchatImmeuble = date(2016, 2, 1);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addRaisonSociale(entreprise, dateDebut, null, "Titi SA");
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
			addForSecondaire(entreprise, dateAchatImmeuble, MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne, MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.BENEFICE_CAPITAL);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
			return entreprise.getNumero();
		});

		// lancement du processeur
		final EnvoiLettresBienvenueResults results = processor.run(RegDate.get(), 0, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(1, results.getIgnores().size());
		Assert.assertEquals(0, results.getTraites().size());

		{
			final EnvoiLettresBienvenueResults.Ignore ignore = results.getIgnores().get(0);
			Assert.assertNotNull(ignore);
			Assert.assertEquals(pmId, ignore.noCtb);
			Assert.assertEquals(EnvoiLettresBienvenueResults.RaisonIgnorement.LETTRE_NON_NECESSAIRE, ignore.raison);
		}

		// vérification de la non-émission de lettre
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
			Assert.assertNotNull(entreprise);
			Assert.assertEquals(0, entreprise.getAutresDocumentsFiscaux().size());
			return null;
		});
	}

	@Test
	public void testPetitTrouSansLettre() throws Exception {

		final RegDate dateDebut = date(2000, 4, 1);
		final RegDate dateDepartHS = date(2015, 4, 23);
		final RegDate dateRetourHS = date(2016, 1, 6);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addRaisonSociale(entreprise, dateDebut, null, "Titi SA");
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, dateDepartHS, MotifFor.DEPART_HS, MockCommune.Lausanne);
			addForPrincipal(entreprise, dateDepartHS.getOneDayAfter(), MotifFor.DEPART_HS, dateRetourHS.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.RoyaumeUni);
			addForPrincipal(entreprise, dateRetourHS, MotifFor.ARRIVEE_HS, MockCommune.Leysin);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
			return entreprise.getNumero();
		});

		// lancement du processeur
		final EnvoiLettresBienvenueResults results = processor.run(RegDate.get(), 0, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(1, results.getIgnores().size());
		Assert.assertEquals(0, results.getTraites().size());

		{
			final EnvoiLettresBienvenueResults.Ignore ignore = results.getIgnores().get(0);
			Assert.assertNotNull(ignore);
			Assert.assertEquals(pmId, ignore.noCtb);
			Assert.assertEquals(EnvoiLettresBienvenueResults.RaisonIgnorement.LETTRE_NON_NECESSAIRE, ignore.raison);
		}

		// vérification de la non-émission de lettre
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
			Assert.assertNotNull(entreprise);
			Assert.assertEquals(0, entreprise.getAutresDocumentsFiscaux().size());
			return null;
		});
	}

	@Test
	public void testNouvelAssujettissementPMVDNonInscriteRC() throws Exception {

		final RegDate dateDebut = date(2016, 1, 4);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addRaisonSociale(entreprise, dateDebut, null, "Titi SA");
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
			return entreprise.getNumero();
		});

		// lancement du processeur
		final EnvoiLettresBienvenueResults results = processor.run(RegDate.get(), 0, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getErreurs().size());
		Assert.assertEquals(0, results.getIgnores().size());
		Assert.assertEquals(0, results.getTraites().size());

		{
			final EnvoiLettresBienvenueResults.EnErreur erreur = results.getErreurs().get(0);
			Assert.assertNotNull(erreur);
			Assert.assertEquals(pmId, erreur.noCtb);
			Assert.assertTrue(erreur.msg, erreur.msg.endsWith("Entreprise non-APM avec siège vaudois mais non-inscrite au RC."));
		}

		// vérification de la non-émission de lettre
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
			Assert.assertNotNull(entreprise);
			Assert.assertEquals(0, entreprise.getAutresDocumentsFiscaux().size());
			return null;
		});
	}

	@Test
	public void testNouvelAssujettissementLettreDejaPresente() throws Exception {

		final RegDate dateDebut = date(2016, 1, 4);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addRaisonSociale(entreprise, dateDebut, null, "Titi SA");
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);

			final RegDate dateEnvoi = dateDebut.addDays(5);
			final LettreBienvenue lb = addLettreBienvenue(entreprise, TypeLettreBienvenue.HS_HC_ETABLISSEMENT);// peu importe le type de la lettre déjà envoyée
			addDelaiAutreDocumentFiscal(lb, dateEnvoi, dateEnvoi.addDays(30), EtatDelaiDocumentFiscal.ACCORDE);
			addEtatAutreDocumentFiscalEmis(lb, dateEnvoi);
			return entreprise.getNumero();
		});

		// lancement du processeur
		final EnvoiLettresBienvenueResults results = processor.run(RegDate.get(), 0, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(1, results.getIgnores().size());
		Assert.assertEquals(0, results.getTraites().size());

		{
			final EnvoiLettresBienvenueResults.Ignore ignore = results.getIgnores().get(0);
			Assert.assertNotNull(ignore);
			Assert.assertEquals(pmId, ignore.noCtb);
			Assert.assertEquals(EnvoiLettresBienvenueResults.RaisonIgnorement.LETTRE_NON_NECESSAIRE, ignore.raison);
		}

		// vérification de la non-émission de lettre
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
			Assert.assertNotNull(entreprise);
			Assert.assertEquals(1, entreprise.getAutresDocumentsFiscaux().size());      // = celle qui existait déjà;
			return null;
		});
	}

	private RegDate addJours(RegDate src, int nbJours) {
		return delaisService.getFinDelai(src, nbJours, false, true);
	}

	@Test
	public void testNouvelAssujettissementPMVDInscriteRC() throws Exception {

		final long noEntrepriseCivile = 4327324L;
		final long noEtablissementPrincipal = 4372L;
		final RegDate dateDebut = date(2016, 1, 7);

		// mise en place civile
		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				MockEntrepriseCivile ent = addEntreprise(noEntrepriseCivile);
				MockEtablissementCivilFactory.addEtablissement(noEtablissementPrincipal, ent, dateDebut, null, "Titi & Co SA", FormeLegale.N_0106_SOCIETE_ANONYME,
				                                               true, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                               MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, dateDebut.addDays(-3),
				                                               StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.SITE, "CHE999999996",
				                                               BigDecimal.valueOf(50000), MontantMonetaire.CHF);
			}
		});

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
			return entreprise.getNumero();
		});

		// lancement du processeur
		final RegDate dateTraitement = RegDate.get();
		final EnvoiLettresBienvenueResults results = processor.run(dateTraitement, 0, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getIgnores().size());
		Assert.assertEquals(1, results.getTraites().size());

		{
			final EnvoiLettresBienvenueResults.Traite traite = results.getTraites().get(0);
			Assert.assertNotNull(traite);
			Assert.assertEquals(pmId, traite.noCtb);
			Assert.assertEquals(TypeLettreBienvenue.VD_RC, traite.typeLettreEnvoyee);
		}

		// vérification de l'émission de lettre
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
			Assert.assertNotNull(entreprise);

			final Set<AutreDocumentFiscal> adfs = entreprise.getAutresDocumentsFiscaux();
			Assert.assertNotNull(adfs);
			Assert.assertEquals(1, adfs.size());

			final AutreDocumentFiscal adf = adfs.iterator().next();
			Assert.assertNotNull(adf);
			Assert.assertEquals(LettreBienvenue.class, adf.getClass());

			final LettreBienvenue lb = (LettreBienvenue) adf;
			Assert.assertEquals(TypeLettreBienvenue.VD_RC, lb.getType());
			Assert.assertEquals(addJours(dateTraitement, 3), lb.getDateEnvoi());
			Assert.assertEquals(addJours(dateTraitement, 3).addDays(30), lb.getDelaiRetour());
			Assert.assertNull(lb.getDateRappel());
			Assert.assertNull(lb.getDateRetour());
			Assert.assertEquals(TypeEtatDocumentFiscal.EMIS, lb.getEtat());
			return null;
		});
	}

	@Test
	public void testNouvelAssujettissementAPMVDInscriteRC() throws Exception {

		final long noEntrepriseCivile = 4327324L;
		final long noEtablissementPrincipal = 4372L;
		final RegDate dateDebut = date(2016, 1, 7);

		// mise en place civile
		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				MockEntrepriseCivile ent = addEntreprise(noEntrepriseCivile);
				MockEtablissementCivilFactory.addEtablissement(noEtablissementPrincipal, ent, dateDebut, null, "Titi et ses amis", FormeLegale.N_0109_ASSOCIATION,
				                                               true, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                               MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, dateDebut.addDays(-3),
				                                               StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.SITE, "CHE999999996", null, null);
			}
		});

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
			return entreprise.getNumero();
		});

		// lancement du processeur
		final RegDate dateTraitement = RegDate.get();
		final EnvoiLettresBienvenueResults results = processor.run(dateTraitement, 0, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getIgnores().size());
		Assert.assertEquals(1, results.getTraites().size());

		{
			final EnvoiLettresBienvenueResults.Traite traite = results.getTraites().get(0);
			Assert.assertNotNull(traite);
			Assert.assertEquals(pmId, traite.noCtb);
			Assert.assertEquals(TypeLettreBienvenue.VD_RC, traite.typeLettreEnvoyee);
		}

		// vérification de l'émission de lettre
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
			Assert.assertNotNull(entreprise);

			final Set<AutreDocumentFiscal> adfs = entreprise.getAutresDocumentsFiscaux();
			Assert.assertNotNull(adfs);
			Assert.assertEquals(1, adfs.size());

			final AutreDocumentFiscal adf = adfs.iterator().next();
			Assert.assertNotNull(adf);
			Assert.assertEquals(LettreBienvenue.class, adf.getClass());

			final LettreBienvenue lb = (LettreBienvenue) adf;
			Assert.assertEquals(TypeLettreBienvenue.VD_RC, lb.getType());
			Assert.assertEquals(addJours(dateTraitement, 3), lb.getDateEnvoi());
			Assert.assertEquals(addJours(dateTraitement, 3).addDays(30), lb.getDelaiRetour());
			Assert.assertNull(lb.getDateRappel());
			Assert.assertNull(lb.getDateRetour());
			Assert.assertEquals(TypeEtatDocumentFiscal.EMIS, lb.getEtat());
			return null;
		});
	}

	@Test
	public void testNouvelAssujettissementAPMManuelleInscriteRC() throws Exception {

		final RegDate dateDebut = date(2016, 1, 4);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.ASSOCIATION);
			addRaisonSociale(entreprise, dateDebut, null, "Titi");
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
			addEtatEntreprise(entreprise, dateDebut, TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.MANUELLE);
			return entreprise.getNumero();
		});

		// lancement du processeur
		final RegDate dateTraitement = RegDate.get();
		final EnvoiLettresBienvenueResults results = processor.run(dateTraitement, 0, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getIgnores().size());
		Assert.assertEquals(1, results.getTraites().size());

		{
			final EnvoiLettresBienvenueResults.Traite traite = results.getTraites().get(0);
			Assert.assertNotNull(traite);
			Assert.assertEquals(pmId, traite.noCtb);
			Assert.assertEquals(TypeLettreBienvenue.VD_RC, traite.typeLettreEnvoyee);
		}

		// vérification de l'émission de lettre
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
			Assert.assertNotNull(entreprise);

			final Set<AutreDocumentFiscal> adfs = entreprise.getAutresDocumentsFiscaux();
			Assert.assertNotNull(adfs);
			Assert.assertEquals(1, adfs.size());

			final AutreDocumentFiscal adf = adfs.iterator().next();
			Assert.assertNotNull(adf);
			Assert.assertEquals(LettreBienvenue.class, adf.getClass());

			final LettreBienvenue lb = (LettreBienvenue) adf;
			Assert.assertEquals(TypeLettreBienvenue.VD_RC, lb.getType());
			Assert.assertEquals(addJours(dateTraitement, 3), lb.getDateEnvoi());
			Assert.assertEquals(addJours(dateTraitement, 3).addDays(30), lb.getDelaiRetour());
			Assert.assertNull(lb.getDateRappel());
			Assert.assertNull(lb.getDateRetour());
			Assert.assertEquals(TypeEtatDocumentFiscal.EMIS, lb.getEtat());
			return null;
		});
	}

	@Test
	public void testNouvelAssujettissementAPMVDNonInscriteRC() throws Exception {

		final long noEntrepriseCivile = 4327324L;
		final long noEtablissementPrincipal = 4372L;
		final RegDate dateDebut = date(2016, 1, 4);

		// mise en place civile
		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				MockEntrepriseCivile ent = addEntreprise(noEntrepriseCivile);
				MockEtablissementCivilFactory.addEtablissement(noEtablissementPrincipal, ent, dateDebut, null, "Titi et ses amis", FormeLegale.N_0109_ASSOCIATION,
				                                               true, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                               MockCommune.Lausanne.getNoOFS(), null, null, StatusRegistreIDE.DEFINITIF,
				                                               TypeEntrepriseRegistreIDE.SITE, "CHE999999996", null, null);
			}
		});

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
			return entreprise.getNumero();
		});

		// lancement du processeur
		final RegDate dateTraitement = RegDate.get();
		final EnvoiLettresBienvenueResults results = processor.run(dateTraitement, 0, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getIgnores().size());
		Assert.assertEquals(1, results.getTraites().size());

		{
			final EnvoiLettresBienvenueResults.Traite traite = results.getTraites().get(0);
			Assert.assertNotNull(traite);
			Assert.assertEquals(pmId, traite.noCtb);
			Assert.assertEquals(TypeLettreBienvenue.APM_VD_NON_RC, traite.typeLettreEnvoyee);
		}

		// vérification de l'émission de lettre
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
			Assert.assertNotNull(entreprise);

			final Set<AutreDocumentFiscal> adfs = entreprise.getAutresDocumentsFiscaux();
			Assert.assertNotNull(adfs);
			Assert.assertEquals(1, adfs.size());

			final AutreDocumentFiscal adf = adfs.iterator().next();
			Assert.assertNotNull(adf);
			Assert.assertEquals(LettreBienvenue.class, adf.getClass());

			final LettreBienvenue lb = (LettreBienvenue) adf;
			Assert.assertEquals(TypeLettreBienvenue.APM_VD_NON_RC, lb.getType());
			Assert.assertEquals(addJours(dateTraitement, 3), lb.getDateEnvoi());
			Assert.assertEquals(addJours(dateTraitement, 3).addDays(30), lb.getDelaiRetour());
			Assert.assertNull(lb.getDateRappel());
			Assert.assertNull(lb.getDateRetour());
			Assert.assertEquals(TypeEtatDocumentFiscal.EMIS, lb.getEtat());
			return null;
		});
	}

	@Test
	public void testNouvelAssujettissementAPMManuelleNonInscriteRC() throws Exception {

		final RegDate dateDebut = date(2016, 1, 4);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.ASSOCIATION);
			addRaisonSociale(entreprise, dateDebut, null, "Titi");
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
			return entreprise.getNumero();
		});

		// lancement du processeur
		final RegDate dateTraitement = RegDate.get();
		final EnvoiLettresBienvenueResults results = processor.run(dateTraitement, 0, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getIgnores().size());
		Assert.assertEquals(1, results.getTraites().size());

		{
			final EnvoiLettresBienvenueResults.Traite traite = results.getTraites().get(0);
			Assert.assertNotNull(traite);
			Assert.assertEquals(pmId, traite.noCtb);
			Assert.assertEquals(TypeLettreBienvenue.APM_VD_NON_RC, traite.typeLettreEnvoyee);
		}

		// vérification de l'émission de lettre
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
			Assert.assertNotNull(entreprise);

			final Set<AutreDocumentFiscal> adfs = entreprise.getAutresDocumentsFiscaux();
			Assert.assertNotNull(adfs);
			Assert.assertEquals(1, adfs.size());

			final AutreDocumentFiscal adf = adfs.iterator().next();
			Assert.assertNotNull(adf);
			Assert.assertEquals(LettreBienvenue.class, adf.getClass());

			final LettreBienvenue lb = (LettreBienvenue) adf;
			Assert.assertEquals(TypeLettreBienvenue.APM_VD_NON_RC, lb.getType());
			Assert.assertEquals(addJours(dateTraitement, 3), lb.getDateEnvoi());
			Assert.assertEquals(addJours(dateTraitement, 3).addDays(30), lb.getDelaiRetour());
			Assert.assertNull(lb.getDateRappel());
			Assert.assertNull(lb.getDateRetour());
			Assert.assertEquals(TypeEtatDocumentFiscal.EMIS, lb.getEtat());
			return null;
		});
	}

	@Test
	public void testNouvelAssujettissementSiegeHCImmeubleSeul() throws Exception {

		final RegDate dateDebut = date(2016, 1, 4);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, dateDebut, null, MockCommune.Neuchatel);
			addForSecondaire(entreprise, dateDebut, MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne, MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.BENEFICE_CAPITAL);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
			return entreprise.getNumero();
		});

		// lancement du processeur
		final RegDate dateTraitement = RegDate.get();
		final EnvoiLettresBienvenueResults results = processor.run(dateTraitement, 0, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getIgnores().size());
		Assert.assertEquals(1, results.getTraites().size());

		{
			final EnvoiLettresBienvenueResults.Traite traite = results.getTraites().get(0);
			Assert.assertNotNull(traite);
			Assert.assertEquals(pmId, traite.noCtb);
			Assert.assertEquals(TypeLettreBienvenue.HS_HC_IMMEUBLE, traite.typeLettreEnvoyee);
		}

		// vérification de l'émission de lettre
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
			Assert.assertNotNull(entreprise);

			final Set<AutreDocumentFiscal> adfs = entreprise.getAutresDocumentsFiscaux();
			Assert.assertNotNull(adfs);
			Assert.assertEquals(1, adfs.size());

			final AutreDocumentFiscal adf = adfs.iterator().next();
			Assert.assertNotNull(adf);
			Assert.assertEquals(LettreBienvenue.class, adf.getClass());

			final LettreBienvenue lb = (LettreBienvenue) adf;
			Assert.assertEquals(TypeLettreBienvenue.HS_HC_IMMEUBLE, lb.getType());
			Assert.assertEquals(addJours(dateTraitement, 3), lb.getDateEnvoi());
			Assert.assertEquals(addJours(dateTraitement, 3).addDays(30), lb.getDelaiRetour());
			Assert.assertNull(lb.getDateRappel());
			Assert.assertNull(lb.getDateRetour());
			Assert.assertEquals(TypeEtatDocumentFiscal.EMIS, lb.getEtat());
			return null;
		});
	}

	@Test
	public void testNouvelAssujettissementSiegeHCEtablissementSeul() throws Exception {

		final RegDate dateDebut = date(2016, 1, 4);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, dateDebut, null, MockCommune.Neuchatel);
			addForSecondaire(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
			return entreprise.getNumero();
		});

		// lancement du processeur
		final RegDate dateTraitement = RegDate.get();
		final EnvoiLettresBienvenueResults results = processor.run(dateTraitement, 0, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getIgnores().size());
		Assert.assertEquals(1, results.getTraites().size());

		{
			final EnvoiLettresBienvenueResults.Traite traite = results.getTraites().get(0);
			Assert.assertNotNull(traite);
			Assert.assertEquals(pmId, traite.noCtb);
			Assert.assertEquals(TypeLettreBienvenue.HS_HC_ETABLISSEMENT, traite.typeLettreEnvoyee);
		}

		// vérification de l'émission de lettre
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
			Assert.assertNotNull(entreprise);

			final Set<AutreDocumentFiscal> adfs = entreprise.getAutresDocumentsFiscaux();
			Assert.assertNotNull(adfs);
			Assert.assertEquals(1, adfs.size());

			final AutreDocumentFiscal adf = adfs.iterator().next();
			Assert.assertNotNull(adf);
			Assert.assertEquals(LettreBienvenue.class, adf.getClass());

			final LettreBienvenue lb = (LettreBienvenue) adf;
			Assert.assertEquals(TypeLettreBienvenue.HS_HC_ETABLISSEMENT, lb.getType());
			Assert.assertEquals(addJours(dateTraitement, 3), lb.getDateEnvoi());
			Assert.assertEquals(addJours(dateTraitement, 3).addDays(30), lb.getDelaiRetour());
			Assert.assertNull(lb.getDateRappel());
			Assert.assertNull(lb.getDateRetour());
			Assert.assertEquals(TypeEtatDocumentFiscal.EMIS, lb.getEtat());
			return null;
		});
	}

	@Test
	public void testNouvelAssujettissementSiegeHCImmeubleEtEtablissement() throws Exception {

		final RegDate dateDebut = date(2016, 1, 4);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, dateDebut, null, MockCommune.Neuchatel);
			addForSecondaire(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
			addForSecondaire(entreprise, dateDebut, MotifFor.ACHAT_IMMOBILIER, MockCommune.Renens, MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.BENEFICE_CAPITAL);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
			return entreprise.getNumero();
		});

		// lancement du processeur
		final RegDate dateTraitement = RegDate.get();
		final EnvoiLettresBienvenueResults results = processor.run(dateTraitement, 0, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getIgnores().size());
		Assert.assertEquals(1, results.getTraites().size());

		{
			final EnvoiLettresBienvenueResults.Traite traite = results.getTraites().get(0);
			Assert.assertNotNull(traite);
			Assert.assertEquals(pmId, traite.noCtb);
			Assert.assertEquals(TypeLettreBienvenue.HS_HC_IMMEUBLE, traite.typeLettreEnvoyee);
		}

		// vérification de l'émission de lettre
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
			Assert.assertNotNull(entreprise);

			final Set<AutreDocumentFiscal> adfs = entreprise.getAutresDocumentsFiscaux();
			Assert.assertNotNull(adfs);
			Assert.assertEquals(1, adfs.size());

			final AutreDocumentFiscal adf = adfs.iterator().next();
			Assert.assertNotNull(adf);
			Assert.assertEquals(LettreBienvenue.class, adf.getClass());

			final LettreBienvenue lb = (LettreBienvenue) adf;
			Assert.assertEquals(TypeLettreBienvenue.HS_HC_IMMEUBLE, lb.getType());
			Assert.assertEquals(addJours(dateTraitement, 3), lb.getDateEnvoi());
			Assert.assertEquals(addJours(dateTraitement, 3).addDays(30), lb.getDelaiRetour());
			Assert.assertNull(lb.getDateRappel());
			Assert.assertNull(lb.getDateRetour());
			Assert.assertEquals(TypeEtatDocumentFiscal.EMIS, lb.getEtat());
			return null;
		});
	}

	@Test
	public void testNouvelAssujettissementSiegeHSImmeubleSeul() throws Exception {

		final RegDate dateDebut = date(2016, 1, 4);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, dateDebut, null, MockPays.RoyaumeUni);
			addForSecondaire(entreprise, dateDebut, MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne, MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.BENEFICE_CAPITAL);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
			return entreprise.getNumero();
		});

		// lancement du processeur
		final RegDate dateTraitement = RegDate.get();
		final EnvoiLettresBienvenueResults results = processor.run(dateTraitement, 0, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getIgnores().size());
		Assert.assertEquals(1, results.getTraites().size());

		{
			final EnvoiLettresBienvenueResults.Traite traite = results.getTraites().get(0);
			Assert.assertNotNull(traite);
			Assert.assertEquals(pmId, traite.noCtb);
			Assert.assertEquals(TypeLettreBienvenue.HS_HC_IMMEUBLE, traite.typeLettreEnvoyee);
		}

		// vérification de l'émission de lettre
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
			Assert.assertNotNull(entreprise);

			final Set<AutreDocumentFiscal> adfs = entreprise.getAutresDocumentsFiscaux();
			Assert.assertNotNull(adfs);
			Assert.assertEquals(1, adfs.size());

			final AutreDocumentFiscal adf = adfs.iterator().next();
			Assert.assertNotNull(adf);
			Assert.assertEquals(LettreBienvenue.class, adf.getClass());

			final LettreBienvenue lb = (LettreBienvenue) adf;
			Assert.assertEquals(TypeLettreBienvenue.HS_HC_IMMEUBLE, lb.getType());
			Assert.assertEquals(addJours(dateTraitement, 3), lb.getDateEnvoi());
			Assert.assertEquals(addJours(dateTraitement, 3).addDays(30), lb.getDelaiRetour());
			Assert.assertNull(lb.getDateRappel());
			Assert.assertNull(lb.getDateRetour());
			Assert.assertEquals(TypeEtatDocumentFiscal.EMIS, lb.getEtat());
			return null;
		});
	}

	@Test
	public void testNouvelAssujettissementSiegeHSEtablissementSeul() throws Exception {

		final RegDate dateDebut = date(2016, 1, 4);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, dateDebut, null, MockPays.RoyaumeUni);
			addForSecondaire(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
			return entreprise.getNumero();
		});

		// lancement du processeur
		final RegDate dateTraitement = RegDate.get();
		final EnvoiLettresBienvenueResults results = processor.run(dateTraitement, 0, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getIgnores().size());
		Assert.assertEquals(1, results.getTraites().size());

		{
			final EnvoiLettresBienvenueResults.Traite traite = results.getTraites().get(0);
			Assert.assertNotNull(traite);
			Assert.assertEquals(pmId, traite.noCtb);
			Assert.assertEquals(TypeLettreBienvenue.HS_HC_ETABLISSEMENT, traite.typeLettreEnvoyee);
		}

		// vérification de l'émission de lettre
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
			Assert.assertNotNull(entreprise);

			final Set<AutreDocumentFiscal> adfs = entreprise.getAutresDocumentsFiscaux();
			Assert.assertNotNull(adfs);
			Assert.assertEquals(1, adfs.size());

			final AutreDocumentFiscal adf = adfs.iterator().next();
			Assert.assertNotNull(adf);
			Assert.assertEquals(LettreBienvenue.class, adf.getClass());

			final LettreBienvenue lb = (LettreBienvenue) adf;
			Assert.assertEquals(TypeLettreBienvenue.HS_HC_ETABLISSEMENT, lb.getType());
			Assert.assertEquals(addJours(dateTraitement, 3), lb.getDateEnvoi());
			Assert.assertEquals(addJours(dateTraitement, 3).addDays(30), lb.getDelaiRetour());
			Assert.assertNull(lb.getDateRappel());
			Assert.assertNull(lb.getDateRetour());
			Assert.assertEquals(TypeEtatDocumentFiscal.EMIS, lb.getEtat());
			return null;
		});
	}

	@Test
	public void testNouvelAssujettissementSiegeHSImmeubleEtEtablissement() throws Exception {

		final RegDate dateDebut = date(2016, 1, 4);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, dateDebut, null, MockPays.RoyaumeUni);
			addForSecondaire(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
			addForSecondaire(entreprise, dateDebut, MotifFor.ACHAT_IMMOBILIER, MockCommune.Renens, MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.BENEFICE_CAPITAL);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
			return entreprise.getNumero();
		});

		// lancement du processeur
		final RegDate dateTraitement = RegDate.get();
		final EnvoiLettresBienvenueResults results = processor.run(dateTraitement, 0, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getIgnores().size());
		Assert.assertEquals(1, results.getTraites().size());

		{
			final EnvoiLettresBienvenueResults.Traite traite = results.getTraites().get(0);
			Assert.assertNotNull(traite);
			Assert.assertEquals(pmId, traite.noCtb);
			Assert.assertEquals(TypeLettreBienvenue.HS_HC_IMMEUBLE, traite.typeLettreEnvoyee);
		}

		// vérification de l'émission de lettre
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
			Assert.assertNotNull(entreprise);

			final Set<AutreDocumentFiscal> adfs = entreprise.getAutresDocumentsFiscaux();
			Assert.assertNotNull(adfs);
			Assert.assertEquals(1, adfs.size());

			final AutreDocumentFiscal adf = adfs.iterator().next();
			Assert.assertNotNull(adf);
			Assert.assertEquals(LettreBienvenue.class, adf.getClass());

			final LettreBienvenue lb = (LettreBienvenue) adf;
			Assert.assertEquals(TypeLettreBienvenue.HS_HC_IMMEUBLE, lb.getType());
			Assert.assertEquals(addJours(dateTraitement, 3), lb.getDateEnvoi());
			Assert.assertEquals(addJours(dateTraitement, 3).addDays(30), lb.getDelaiRetour());
			Assert.assertNull(lb.getDateRappel());
			Assert.assertNull(lb.getDateRetour());
			Assert.assertEquals(TypeEtatDocumentFiscal.EMIS, lb.getEtat());
			return null;
		});
	}

	@Test
	public void testDelaiCarence() throws Exception {

		final RegDate dateDebut = date(2016, 1, 4);
		final RegDate dateTraitement = dateDebut.addDays(2);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, dateDebut, null, MockPays.RoyaumeUni);
			addForSecondaire(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
			addForSecondaire(entreprise, dateDebut, MotifFor.ACHAT_IMMOBILIER, MockCommune.Renens, MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.BENEFICE_CAPITAL);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
			return entreprise.getNumero();
		});

		// lancement du processeur (rien ne doit être traité car le début d'assujettissement est occulté par le délai de carence)
		final EnvoiLettresBienvenueResults resultsAvecCarenceOccultante = processor.run(dateTraitement, 4, null);
		Assert.assertNotNull(resultsAvecCarenceOccultante);
		Assert.assertEquals(0, resultsAvecCarenceOccultante.getErreurs().size());
		Assert.assertEquals(1, resultsAvecCarenceOccultante.getIgnores().size());
		Assert.assertEquals(0, resultsAvecCarenceOccultante.getTraites().size());

		{
			final EnvoiLettresBienvenueResults.Ignore ignore = resultsAvecCarenceOccultante.getIgnores().get(0);
			Assert.assertNotNull(ignore);
			Assert.assertEquals(pmId, ignore.noCtb);
			Assert.assertEquals(EnvoiLettresBienvenueResults.RaisonIgnorement.NON_ASSUJETTI, ignore.raison);
		}

		// vérification de la non-émission de lettre
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
			Assert.assertNotNull(entreprise);
			Assert.assertEquals(0, entreprise.getAutresDocumentsFiscaux().size());
			return null;
		});

		// nouveau lancement du processeur (cette fois-ci, la lettre doit partir car le début d'assujettissement n'est plus occulté par le délai de carence)
		final EnvoiLettresBienvenueResults resultsEfficace = processor.run(dateTraitement, 1, null);
		Assert.assertNotNull(resultsEfficace);
		Assert.assertEquals(0, resultsEfficace.getErreurs().size());
		Assert.assertEquals(0, resultsEfficace.getIgnores().size());
		Assert.assertEquals(1, resultsEfficace.getTraites().size());

		{
			final EnvoiLettresBienvenueResults.Traite traite = resultsEfficace.getTraites().get(0);
			Assert.assertNotNull(traite);
			Assert.assertEquals(pmId, traite.noCtb);
			Assert.assertEquals(TypeLettreBienvenue.HS_HC_IMMEUBLE, traite.typeLettreEnvoyee);
		}

		// vérification de l'émission de lettre
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
			Assert.assertNotNull(entreprise);

			final Set<AutreDocumentFiscal> adfs = entreprise.getAutresDocumentsFiscaux();
			Assert.assertNotNull(adfs);
			Assert.assertEquals(1, adfs.size());

			final AutreDocumentFiscal adf = adfs.iterator().next();
			Assert.assertNotNull(adf);
			Assert.assertEquals(LettreBienvenue.class, adf.getClass());

			final LettreBienvenue lb = (LettreBienvenue) adf;
			Assert.assertEquals(TypeLettreBienvenue.HS_HC_IMMEUBLE, lb.getType());
			Assert.assertEquals(addJours(dateTraitement, 3), lb.getDateEnvoi());
			Assert.assertEquals(addJours(dateTraitement, 3).addDays(30), lb.getDelaiRetour());
			Assert.assertNull(lb.getDateRappel());
			Assert.assertNull(lb.getDateRetour());
			Assert.assertEquals(TypeEtatDocumentFiscal.EMIS, lb.getEtat());
			return null;
		});
	}

	/**
	 * [SIFISC-21646] cas où une entreprise HC achète un immeuble, par exemple, et le revends rapidement avant
	 * même que le job d'envoi des lettres de bienvenue n'ait tourné : il faut envoyer une lettre de bienvenue
	 */
	@Test
	public void testAchatVenteImmeubleRapideDansPremierePF() throws Exception {

		final RegDate dateAchat = date(2016, 6, 1);
		final RegDate dateVente = date(2016, 7, 12);
		final RegDate dateTraitement = date(2016, 8, 1);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addFormeJuridique(e, dateAchat, null, FormeJuridiqueEntreprise.SA);
			addRaisonSociale(e, dateAchat, null, "Le petit butineur");
			addRegimeFiscalCH(e, dateAchat, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalVD(e, dateAchat, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(e, dateAchat, null, MockCommune.Conthey, GenreImpot.BENEFICE_CAPITAL);
			addForSecondaire(e, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Cossonay, MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.BENEFICE_CAPITAL);
			return e.getNumero();
		});

		final EnvoiLettresBienvenueResults results = processor.run(dateTraitement, 4, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getIgnores().size());
		Assert.assertEquals(1, results.getTraites().size());

		{
			final EnvoiLettresBienvenueResults.Traite traite = results.getTraites().get(0);
			Assert.assertNotNull(traite);
			Assert.assertEquals(pmId, traite.noCtb);
			Assert.assertEquals(TypeLettreBienvenue.HS_HC_IMMEUBLE, traite.typeLettreEnvoyee);
		}

		// vérification de l'émission de lettre
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(pmId);
			Assert.assertNotNull(entreprise);

			final Set<AutreDocumentFiscal> adfs = entreprise.getAutresDocumentsFiscaux();
			Assert.assertNotNull(adfs);
			Assert.assertEquals(1, adfs.size());

			final AutreDocumentFiscal adf = adfs.iterator().next();
			Assert.assertNotNull(adf);
			Assert.assertEquals(LettreBienvenue.class, adf.getClass());

			final LettreBienvenue lb = (LettreBienvenue) adf;
			Assert.assertEquals(TypeLettreBienvenue.HS_HC_IMMEUBLE, lb.getType());
			Assert.assertEquals(addJours(dateTraitement, 3), lb.getDateEnvoi());
			Assert.assertEquals(addJours(dateTraitement, 3).addDays(30), lb.getDelaiRetour());
			Assert.assertNull(lb.getDateRappel());
			Assert.assertNull(lb.getDateRetour());
			Assert.assertEquals(TypeEtatDocumentFiscal.EMIS, lb.getEtat());
			return null;
		});
	}
}
