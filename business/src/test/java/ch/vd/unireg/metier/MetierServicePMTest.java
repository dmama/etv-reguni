package ch.vd.unireg.metier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.adresse.AdresseMandataire;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.adresse.AdresseSuisse;
import ch.vd.unireg.adresse.AdresseTiers;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalFor;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalInformationComplementaire;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockSiteOrganisationFactory;
import ch.vd.unireg.tache.TacheService;
import ch.vd.unireg.tiers.ActiviteEconomique;
import ch.vd.unireg.tiers.DomicileEtablissement;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.EtatEntreprise;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalAvecMotifs;
import ch.vd.unireg.tiers.ForFiscalPrincipalPM;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.tiers.ForsParType;
import ch.vd.unireg.tiers.FusionEntreprises;
import ch.vd.unireg.tiers.MontantMonetaire;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.Remarque;
import ch.vd.unireg.tiers.ScissionEntreprise;
import ch.vd.unireg.tiers.TransfertPatrimoine;
import ch.vd.unireg.tiers.dao.RemarqueDAO;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEtatEntreprise;
import ch.vd.unireg.type.TypeGenerationEtatEntreprise;
import ch.vd.unireg.type.TypeMandat;
import ch.vd.unireg.type.TypeRapportEntreTiers;

@SuppressWarnings("Duplicates")
public class MetierServicePMTest extends BusinessTest {

	private MetierServicePMImpl metierServicePM;
	private EvenementFiscalDAO evenementFiscalDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		evenementFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");

		metierServicePM = new MetierServicePMImpl();
		metierServicePM.setAdresseService(getBean(AdresseService.class, "adresseService"));
		metierServicePM.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		metierServicePM.setRemarqueDAO(getBean(RemarqueDAO.class, "remarqueDAO"));
		metierServicePM.setTacheService(getBean(TacheService.class, "tacheService"));
		metierServicePM.setTiersService(tiersService);
	}

	@Test
	public void testRattacheOrganisationEntreprise() throws Exception {

		final RegDate dateCreation = date(2001, 5, 4);
		final RegDate dateRattachement = date(2010, 6, 26);
		final String nom = "Synergy SA";
		final String nomSite = "Synergy Etablissement SA";
		final String nomSite2 = "Synergy Etablissement Aubonne SA";
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;
		final Long noSite2 = noSite + 1;

		// mise en place du service mock Organisation
		final MockOrganisation organisation = MockOrganisationFactory.createOrganisation(noOrganisation, noSite, nom, dateRattachement, null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
		                                                                                 TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF,
		                                                                                 date(2001, 5, 1),
		                                                                                 StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
		MockSiteOrganisationFactory.addSite(noSite2, organisation, date(2003, 10, 5), null, nomSite2, FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                    MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2003, 10, 1), StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(organisation);
			}
		});


		// mise en place des données fiscales
		final long entrepriseId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Etablissement etablissement = addEtablissement();
				etablissement.setRaisonSociale(nomSite);
				addDomicileEtablissement(etablissement, dateCreation, null, MockCommune.Lausanne);

				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSocialeFiscaleEntreprise(entreprise, dateCreation, null, nom);
				addFormeJuridique(entreprise, dateCreation, null, FormeJuridiqueEntreprise.SARL);
				addCapitalEntreprise(entreprise, dateCreation, null, new MontantMonetaire(25000L, "CHF"));
				tiersService.addActiviteEconomique(etablissement, entreprise, dateCreation, true);

				return entreprise.getNumero();
			}
		});

		final RattachementOrganisationResult result = doInNewTransactionAndSession(new TransactionCallback<RattachementOrganisationResult>() {
			@Override
			public RattachementOrganisationResult doInTransaction(TransactionStatus transactionStatus) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(entrepriseId);
				try {
					return metierServicePM.rattacheOrganisationEntreprise(organisation, entreprise, dateRattachement);
				}
				catch (MetierServiceException e) {
					throw new RuntimeException(e);
				}
			}
		});
		Assert.assertNotNull(result);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(noOrganisation);
				Assert.assertNotNull(entreprise);
				Assert.assertEquals(noOrganisation, entreprise.getNumeroEntreprise());

				Assert.assertEquals(dateRattachement.getOneDayBefore(), CollectionsUtils.getLastElement(entreprise.getRaisonsSocialesNonAnnuleesTriees()).getDateFin());
				Assert.assertEquals(dateRattachement.getOneDayBefore(), CollectionsUtils.getLastElement(entreprise.getFormesJuridiquesNonAnnuleesTriees()).getDateFin());
				Assert.assertEquals(dateRattachement.getOneDayBefore(), CollectionsUtils.getLastElement(entreprise.getCapitauxNonAnnulesTries()).getDateFin());

				final List<DateRanged<Etablissement>> etablissementsPrincipaux = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
				final Etablissement etablissement = CollectionsUtils.getLastElement(etablissementsPrincipaux).getPayload();
				Assert.assertEquals(noSite, etablissement.getNumeroEtablissement());
				final DomicileEtablissement domicile = CollectionsUtils.getLastElement(etablissement.getSortedDomiciles(false));
				Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(dateRattachement.getOneDayBefore(), domicile.getDateFin());

				// Vérification du résultat
				Assert.assertEquals(noOrganisation.longValue(), result.getEntrepriseRattachee().getNumeroEntreprise().longValue());
				Assert.assertEquals(1, result.getEtablissementsRattaches().size());
				Assert.assertEquals(noSite.longValue(), result.getEtablissementsRattaches().get(0).getNumeroEtablissement().longValue());
				Assert.assertTrue(result.getEtablissementsNonRattaches().isEmpty());
				Assert.assertEquals(1, result.getSitesNonRattaches().size());
				Assert.assertEquals(noSite2.longValue(), result.getSitesNonRattaches().get(0).getNumeroSite());
			}
		});
	}

	@Test
	public void testRattacheOrganisationEntrepriseAvecSecondaires() throws Exception {

		final RegDate dateCreation = date(2001, 5, 4);
		final RegDate dateRattachement = date(2010, 6, 26);
		final String nom = "Synergy SA";
		final String nomSite = "Synergy Etablissement SA";
		final String nomSite2 = "Synergy Etablissement Aubonne SA";
		final String nomSite3 = "Synergy Etablissement Cossonay SA";
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;
		final Long noSite2 = noSite + 1;

		// mise en place du service mock Organisation
		final MockOrganisation organisation = MockOrganisationFactory.createOrganisation(noOrganisation, noSite, nom, dateRattachement, null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
		                                                                                 TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF,
		                                                                                 date(2001, 5, 1),
		                                                                                 StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
		MockSiteOrganisationFactory.addSite(noSite2, organisation, date(2003, 10, 5), null, nomSite2, FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                    MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2003, 10, 1), StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(organisation);
			}
		});

		final class Ids {
			long entrepriseId;
			long etablissement3Id;
		}

		// mise en place des données fiscales
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final Etablissement etablissement = addEtablissement();
				etablissement.setRaisonSociale(nomSite);
				addDomicileEtablissement(etablissement, dateCreation, null, MockCommune.Lausanne);

				final Etablissement etablissement3 = addEtablissement();
				etablissement3.setRaisonSociale(nomSite3);
				addDomicileEtablissement(etablissement3, dateCreation, null, MockCommune.Cossonay);

				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSocialeFiscaleEntreprise(entreprise, dateCreation, null, nom);
				addFormeJuridique(entreprise, dateCreation, null, FormeJuridiqueEntreprise.SARL);
				addCapitalEntreprise(entreprise, dateCreation, null, new MontantMonetaire(25000L, "CHF"));

				addActiviteEconomique(entreprise, etablissement, dateCreation, null, true);
				addActiviteEconomique(entreprise, etablissement3, dateCreation, null, false);

				final Ids ids = new Ids();
				ids.entrepriseId = entreprise.getNumero();
				ids.etablissement3Id = etablissement3.getNumero();
				return ids;
			}
		});

		final RattachementOrganisationResult result = doInNewTransactionAndSession(new TransactionCallback<RattachementOrganisationResult>() {
			@Override
			public RattachementOrganisationResult doInTransaction(TransactionStatus transactionStatus) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.entrepriseId);
				try {
					return metierServicePM.rattacheOrganisationEntreprise(organisation, entreprise, dateRattachement);
				}
				catch (MetierServiceException e) {
					throw new RuntimeException(e);
				}
			}
		});

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {

				final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(noOrganisation);
				Assert.assertNotNull(entreprise);
				Assert.assertEquals(noOrganisation, entreprise.getNumeroEntreprise());

				Assert.assertEquals(dateRattachement.getOneDayBefore(), CollectionsUtils.getLastElement(entreprise.getRaisonsSocialesNonAnnuleesTriees()).getDateFin());
				Assert.assertEquals(dateRattachement.getOneDayBefore(), CollectionsUtils.getLastElement(entreprise.getFormesJuridiquesNonAnnuleesTriees()).getDateFin());
				Assert.assertEquals(dateRattachement.getOneDayBefore(), CollectionsUtils.getLastElement(entreprise.getCapitauxNonAnnulesTries()).getDateFin());

				final List<DateRanged<Etablissement>> etablissementsPrincipaux = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
				final Etablissement etablissement = CollectionsUtils.getLastElement(etablissementsPrincipaux).getPayload();
				Assert.assertEquals(noSite, etablissement.getNumeroEtablissement());
				final DomicileEtablissement domicile = CollectionsUtils.getLastElement(etablissement.getSortedDomiciles(false));
				Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(dateRattachement.getOneDayBefore(), domicile.getDateFin());

				// Vérification du résultat
				Assert.assertEquals(noOrganisation.longValue(), result.getEntrepriseRattachee().getNumeroEntreprise().longValue());
				Assert.assertEquals(1, result.getEtablissementsRattaches().size());
				Assert.assertEquals(noSite.longValue(), result.getEtablissementsRattaches().get(0).getNumeroEtablissement().longValue());
				Assert.assertEquals(ids.etablissement3Id, result.getEtablissementsNonRattaches().get(0).getNumero().longValue());
				Assert.assertEquals(1, result.getSitesNonRattaches().size());
				Assert.assertEquals(noSite2.longValue(), result.getSitesNonRattaches().get(0).getNumeroSite());
			}
		});
	}

	@Test
	public void testRattacheOrganisationEntrepriseAvecSecondaireDejaRapproche() throws Exception {

		final RegDate dateCreation = date(2001, 5, 4);
		final RegDate dateRattachement = date(2010, 6, 26);
		final String nom = "Synergy SA";
		final String nomSite = "Synergy Etablissement SA";
		final String nomSite2 = "Synergy Etablissement Aubonne SA";
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;
		final Long noSite2 = noSite + 1;

		// mise en place du service mock Organisation
		final MockOrganisation organisation = MockOrganisationFactory.createOrganisation(noOrganisation, noSite, nom, dateRattachement, null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
		                                                                                 TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF,
		                                                                                 date(2001, 5, 1),
		                                                                                 StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
		MockSiteOrganisationFactory.addSite(noSite2, organisation, date(2003, 10, 5), null, nomSite2, FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                    MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2003, 10, 1), StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(organisation);
			}
		});

		// mise en place des données fiscales
		final long entrepriseId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Etablissement etablissement = addEtablissement();
				etablissement.setRaisonSociale(nomSite);
				addDomicileEtablissement(etablissement, dateCreation, null, MockCommune.Lausanne);

				final Etablissement etablissement2 = addEtablissement();
				etablissement2.setRaisonSociale(nomSite2);
				etablissement2.setNumeroEtablissement(noSite2);
				addDomicileEtablissement(etablissement2, dateCreation, null, MockCommune.Aubonne);

				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSocialeFiscaleEntreprise(entreprise, dateCreation, null, nom);
				addFormeJuridique(entreprise, dateCreation, null, FormeJuridiqueEntreprise.SARL);
				addCapitalEntreprise(entreprise, dateCreation, null, new MontantMonetaire(25000L, "CHF"));

				addActiviteEconomique(entreprise, etablissement, dateCreation, null, true);
				addActiviteEconomique(entreprise, etablissement2, dateCreation, null, false);

				return entreprise.getNumero();
			}
		});

		final RattachementOrganisationResult result = doInNewTransactionAndSession(new TransactionCallback<RattachementOrganisationResult>() {
			@Override
			public RattachementOrganisationResult doInTransaction(TransactionStatus transactionStatus) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(entrepriseId);
				try {
					return metierServicePM.rattacheOrganisationEntreprise(organisation, entreprise, dateRattachement);
				}
				catch (MetierServiceException e) {
					throw new RuntimeException(e);
				}
			}
		});

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {

				final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(noOrganisation);
				Assert.assertNotNull(entreprise);
				Assert.assertEquals(noOrganisation, entreprise.getNumeroEntreprise());

				Assert.assertEquals(dateRattachement.getOneDayBefore(), CollectionsUtils.getLastElement(entreprise.getRaisonsSocialesNonAnnuleesTriees()).getDateFin());
				Assert.assertEquals(dateRattachement.getOneDayBefore(), CollectionsUtils.getLastElement(entreprise.getFormesJuridiquesNonAnnuleesTriees()).getDateFin());
				Assert.assertEquals(dateRattachement.getOneDayBefore(), CollectionsUtils.getLastElement(entreprise.getCapitauxNonAnnulesTries()).getDateFin());

				final List<DateRanged<Etablissement>> etablissementsPrincipaux = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
				final Etablissement etablissement = CollectionsUtils.getLastElement(etablissementsPrincipaux).getPayload();
				Assert.assertEquals(noSite, etablissement.getNumeroEtablissement());
				final DomicileEtablissement domicile = CollectionsUtils.getLastElement(etablissement.getSortedDomiciles(false));
				Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(dateRattachement.getOneDayBefore(), domicile.getDateFin());

				// Vérification du résultat
				Assert.assertEquals(noOrganisation.longValue(), result.getEntrepriseRattachee().getNumeroEntreprise().longValue());
				Assert.assertEquals(2, result.getEtablissementsRattaches().size());
				Assert.assertEquals(noSite.longValue(), result.getEtablissementsRattaches().get(0).getNumeroEtablissement().longValue());
				Assert.assertEquals(noSite2.longValue(), result.getEtablissementsRattaches().get(1).getNumeroEtablissement().longValue());
				Assert.assertTrue(result.getEtablissementsNonRattaches().isEmpty());
				Assert.assertTrue(result.getSitesNonRattaches().isEmpty());
			}
		});
	}

	@Test
	public void testFaillite() throws Exception {

		final RegDate dateCreationEntreprise = date(2000, 4, 1);
		final RegDate datePrononceFaillite = date(2010, 4, 13);

		final class Ids {
			long idEntreprise;
			long idEtablissementPrincipal;
			long idEtablissementSecondaire;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateCreationEntreprise, null, "Ma petite entreprise");
				addFormeJuridique(entreprise, dateCreationEntreprise, null, FormeJuridiqueEntreprise.ASSOCIATION);
				addRegimeFiscalCH(entreprise, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
				addRegimeFiscalVD(entreprise, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
				addBouclement(entreprise, dateCreationEntreprise, DayMonth.get(12, 31), 12);        // tous les 31.12 depuis 2000
				addForPrincipal(entreprise, dateCreationEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Grandson);
				addForSecondaire(entreprise, dateCreationEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.ChateauDoex, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
				entreprise.setBlocageRemboursementAutomatique(Boolean.FALSE);

				addAdresseMandataireSuisse(entreprise, dateCreationEntreprise, null, TypeMandat.GENERAL, "Mon mandataire chéri", MockRue.Renens.QuatorzeAvril);

				final Etablissement etablissementPrincipal = addEtablissement();
				addDomicileEtablissement(etablissementPrincipal, dateCreationEntreprise, null, MockCommune.Grandson);
				addActiviteEconomique(entreprise, etablissementPrincipal, dateCreationEntreprise, null, true);

				final Etablissement etablissementSecondaire = addEtablissement();
				addDomicileEtablissement(etablissementSecondaire, dateCreationEntreprise, null, MockCommune.ChateauDoex);
				addActiviteEconomique(entreprise, etablissementSecondaire, dateCreationEntreprise, null, false);

				addEtatEntreprise(entreprise, dateCreationEntreprise, TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.AUTOMATIQUE);

				final Ids ids = new Ids();
				ids.idEntreprise = entreprise.getNumero();
				ids.idEtablissementPrincipal = etablissementPrincipal.getNumero();
				ids.idEtablissementSecondaire = etablissementSecondaire.getNumero();
				return ids;
			}
		});

		// traitement de la faillite
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
				Assert.assertNotNull(entreprise);
				Assert.assertFalse(entreprise.getBlocageRemboursementAutomatique());
				metierServicePM.faillite(entreprise, datePrononceFaillite, "Une jolie remarque toute belle...");
			}
		});

		// vérification des résultats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
				Assert.assertNotNull(entreprise);
				Assert.assertTrue(entreprise.getBlocageRemboursementAutomatique());

				// 1. les fors doivent être fermés pour motif FAILLITE
				final Set<ForFiscal> forsFiscaux = entreprise.getForsFiscaux();
				Assert.assertNotNull(forsFiscaux);
				Assert.assertEquals(2, forsFiscaux.size());
				for (ForFiscal ff : forsFiscaux) {
					Assert.assertFalse(ff.isAnnule());
					Assert.assertEquals(dateCreationEntreprise, ff.getDateDebut());
					Assert.assertEquals(datePrononceFaillite, ff.getDateFin());
					Assert.assertTrue(ff.getClass().getName(), ff instanceof ForFiscalAvecMotifs);
					Assert.assertEquals(MotifFor.FAILLITE, ((ForFiscalAvecMotifs) ff).getMotifFermeture());
				}

				// 2. le rapport entre tiers vers l'établissement secondaire doit être fermé, pas l'autre
				boolean principalTrouve = false;
				boolean secondaireTrouve = false;
				final Set<RapportEntreTiers> rapportsSujet = entreprise.getRapportsSujet();
				Assert.assertNotNull(rapportsSujet);
				Assert.assertEquals(2, rapportsSujet.size());
				for (RapportEntreTiers ret : rapportsSujet) {
					Assert.assertFalse(ret.isAnnule());
					Assert.assertEquals(dateCreationEntreprise, ret.getDateDebut());
					Assert.assertEquals(TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE, ret.getType());

					final ActiviteEconomique ae = (ActiviteEconomique) ret;
					if (ae.isPrincipal()) {
						Assert.assertFalse(principalTrouve);
						Assert.assertNull(ae.getDateFin());
						Assert.assertEquals((Long) ids.idEtablissementPrincipal, ae.getObjetId());
						principalTrouve = true;
					}
					else {
						Assert.assertFalse(secondaireTrouve);
						Assert.assertEquals(datePrononceFaillite, ae.getDateFin());
						Assert.assertEquals((Long) ids.idEtablissementSecondaire, ae.getObjetId());
						secondaireTrouve = true;
					}
				}
				Assert.assertTrue(principalTrouve);
				Assert.assertTrue(secondaireTrouve);

				// 3. les adresses mandataires doivent être fermées
				final Set<AdresseMandataire> adressesMandataires = entreprise.getAdressesMandataires();
				Assert.assertNotNull(adressesMandataires);
				Assert.assertEquals(1, adressesMandataires.size());
				for (AdresseMandataire adresse : adressesMandataires) {
					Assert.assertFalse(adresse.isAnnule());
					Assert.assertEquals(dateCreationEntreprise, adresse.getDateDebut());
					Assert.assertEquals(datePrononceFaillite, adresse.getDateFin());
				}

				// 4. état EN_FAILLITE sur l'entreprise
				final EtatEntreprise etatActuel = entreprise.getEtatActuel();
				Assert.assertNotNull(etatActuel);
				Assert.assertEquals(TypeEtatEntreprise.EN_FAILLITE, etatActuel.getType());
				Assert.assertEquals(datePrononceFaillite, etatActuel.getDateObtention());
				Assert.assertEquals(TypeGenerationEtatEntreprise.MANUELLE, etatActuel.getGeneration());

				// 5. et la remarque ?
				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				final Remarque remarque = remarques.iterator().next();
				Assert.assertNotNull(remarque);
				Assert.assertFalse(remarque.isAnnule());
				Assert.assertEquals("Une jolie remarque toute belle...", remarque.getTexte());

				// 6. événements fiscaux
				final Collection<EvenementFiscal> evts = evenementFiscalDAO.getEvenementsFiscaux(entreprise);
				Assert.assertEquals(3, evts.size());        // 2 fermetures de for + 1 info complémentaire
				final Map<Class<?>, List<EvenementFiscal>> evtsParClass = segmenter(evts, new Extractor<EvenementFiscal, Class<?>>() {
					@Override
					public Class<?> extract(EvenementFiscal source) {
						return source.getClass();
					}
				});
				Assert.assertEquals(new HashSet<>(Arrays.asList(EvenementFiscalFor.class, EvenementFiscalInformationComplementaire.class)), evtsParClass.keySet());
				Assert.assertEquals(2, evtsParClass.get(EvenementFiscalFor.class).size());
				Assert.assertEquals(1, evtsParClass.get(EvenementFiscalInformationComplementaire.class).size());
				for (EvenementFiscal ef : evtsParClass.get(EvenementFiscalFor.class)) {
					final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					Assert.assertNotNull(eff);
					Assert.assertFalse(eff.isAnnule());
					Assert.assertEquals(datePrononceFaillite, eff.getDateValeur());
					Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.FERMETURE, eff.getType());
				}
				for (EvenementFiscal ef : evtsParClass.get(EvenementFiscalInformationComplementaire.class)) {
					final EvenementFiscalInformationComplementaire efic = (EvenementFiscalInformationComplementaire) ef;
					Assert.assertNotNull(efic);
					Assert.assertFalse(efic.isAnnule());
					Assert.assertEquals(datePrononceFaillite, efic.getDateValeur());
					Assert.assertEquals(EvenementFiscalInformationComplementaire.TypeInformationComplementaire.PUBLICATION_FAILLITE_APPEL_CREANCIERS, efic.getType());
				}
			}
		});
	}

	private static <T extends Annulable> Map<Boolean, List<T>> segmenterAnnulables(Collection<T> elements) {
		return segmenter(elements, Annulable::isAnnule, Boolean.TRUE, Boolean.FALSE);
	}

	private static <T> Map<Class<? extends T>, List<T>> segmenterParClasse(Collection<T> elements) {
		return segmenter(elements, source -> (Class<? extends T>) source.getClass());
	}

	private interface Extractor<T, U> {
		U extract(T source);
	}

	@SafeVarargs
	private static <T, U> Map<U, List<T>> segmenter(Collection<T> elements, Extractor<? super T, ? extends U> extractor, U... knownKeys) {
		final Map<U, List<T>> map = new HashMap<>(elements.size());
		for (U knownKey : knownKeys) {
			map.put(knownKey, new ArrayList<>(elements.size()));
		}
		for (T element : elements) {
			final U key = extractor.extract(element);
			final List<T> list = map.computeIfAbsent(key, k -> new ArrayList<>(elements.size()));
			list.add(element);
		}
		return map;
	}

	private static <T extends RapportEntreTiers> List<T> extractRapports(Collection<RapportEntreTiers> tous, Class<T> clazz, @Nullable Comparator<? super T> comparator) {
		final List<T> liste = new ArrayList<>(tous.size());
		for (RapportEntreTiers ret : tous) {
			if (clazz.isAssignableFrom(ret.getClass())) {
				//noinspection unchecked
				liste.add((T) ret);
			}
		}
		if (comparator != null) {
			liste.sort(comparator);
		}
		return liste;
	}

	@Test
	public void testAnnulationFaillite() throws Exception {

		final RegDate dateCreationEntreprise = date(2000, 4, 1);
		final RegDate datePrononceFaillite = date(2010, 4, 13);

		final class Ids {
			long idEntreprise;
			long idEtablissementPrincipal;
			long idEtablissementSecondaire;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateCreationEntreprise, null, "Ma petite entreprise");
				addFormeJuridique(entreprise, dateCreationEntreprise, null, FormeJuridiqueEntreprise.ASSOCIATION);
				addRegimeFiscalCH(entreprise, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
				addRegimeFiscalVD(entreprise, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
				addBouclement(entreprise, dateCreationEntreprise, DayMonth.get(12, 31), 12);        // tous les 31.12 depuis 2000
				addForPrincipal(entreprise, dateCreationEntreprise, MotifFor.DEBUT_EXPLOITATION, datePrononceFaillite, MotifFor.FAILLITE, MockCommune.Grandson);
				addForSecondaire(entreprise, dateCreationEntreprise, MotifFor.DEBUT_EXPLOITATION, datePrononceFaillite, MotifFor.FAILLITE, MockCommune.ChateauDoex, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
				entreprise.setBlocageRemboursementAutomatique(Boolean.TRUE);

				addAdresseMandataireSuisse(entreprise, dateCreationEntreprise, datePrononceFaillite, TypeMandat.GENERAL, "Mon mandataire chéri", MockRue.Renens.QuatorzeAvril);

				final Etablissement etablissementPrincipal = addEtablissement();
				addDomicileEtablissement(etablissementPrincipal, dateCreationEntreprise, null, MockCommune.Grandson);
				addActiviteEconomique(entreprise, etablissementPrincipal, dateCreationEntreprise, null, true);

				final Etablissement etablissementSecondaire = addEtablissement();
				addDomicileEtablissement(etablissementSecondaire, dateCreationEntreprise, null, MockCommune.ChateauDoex);
				addActiviteEconomique(entreprise, etablissementSecondaire, dateCreationEntreprise, datePrononceFaillite, false);

				addEtatEntreprise(entreprise, dateCreationEntreprise, TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.AUTOMATIQUE);
				addEtatEntreprise(entreprise, datePrononceFaillite, TypeEtatEntreprise.EN_FAILLITE, TypeGenerationEtatEntreprise.MANUELLE);

				final Ids ids = new Ids();
				ids.idEntreprise = entreprise.getNumero();
				ids.idEtablissementPrincipal = etablissementPrincipal.getNumero();
				ids.idEtablissementSecondaire = etablissementSecondaire.getNumero();
				return ids;
			}
		});

		// traitement de l'annulation de la faillite
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
				Assert.assertNotNull(entreprise);
				Assert.assertTrue(entreprise.getBlocageRemboursementAutomatique());
				metierServicePM.annuleFaillite(entreprise, datePrononceFaillite, "Une jolie remarque toute belle pour l'annulation...");
			}
		});

		// vérification des résultats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
				Assert.assertNotNull(entreprise);
				Assert.assertFalse(entreprise.getBlocageRemboursementAutomatique());

				// 1. les fors doivent être ré-ouverts
				final Set<ForFiscal> forsFiscaux = entreprise.getForsFiscaux();
				Assert.assertNotNull(forsFiscaux);
				Assert.assertEquals(4, forsFiscaux.size());     // deux annulés, et deux ré-ouverts
				final Map<Boolean, List<ForFiscal>> fors = segmenterAnnulables(forsFiscaux);
				Assert.assertEquals(2, fors.get(Boolean.TRUE).size());
				Assert.assertEquals(2, fors.get(Boolean.FALSE).size());
				for (ForFiscal ff : fors.get(Boolean.TRUE)) {
					Assert.assertEquals(dateCreationEntreprise, ff.getDateDebut());
					Assert.assertEquals(datePrononceFaillite, ff.getDateFin());
					Assert.assertTrue(ff.getClass().getName(), ff instanceof ForFiscalAvecMotifs);
					Assert.assertEquals(MotifFor.FAILLITE, ((ForFiscalAvecMotifs) ff).getMotifFermeture());
				}
				for (ForFiscal ff : fors.get(Boolean.FALSE)) {
					Assert.assertEquals(dateCreationEntreprise, ff.getDateDebut());
					Assert.assertNull(ff.getDateFin());
					Assert.assertTrue(ff.getClass().getName(), ff instanceof ForFiscalAvecMotifs);
					Assert.assertNull(((ForFiscalAvecMotifs) ff).getMotifFermeture());
				}

				// 2. le rapport entre tiers vers l'établissement secondaire doit être ré-ouvert
				final Set<RapportEntreTiers> rapportsSujet = entreprise.getRapportsSujet();
				Assert.assertNotNull(rapportsSujet);
				Assert.assertEquals(3, rapportsSujet.size());           // 1 annulé, 2 ouverts
				final Map<Boolean, List<RapportEntreTiers>> rets = segmenterAnnulables(rapportsSujet);
				Assert.assertEquals(1, rets.get(Boolean.TRUE).size());
				Assert.assertEquals(2, rets.get(Boolean.FALSE).size());
				for (RapportEntreTiers ret : rets.get(Boolean.FALSE)) {
					Assert.assertEquals(dateCreationEntreprise, ret.getDateDebut());
					Assert.assertNull(ret.getDateFin());
					Assert.assertEquals(TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE, ret.getType());
				}
				for (RapportEntreTiers ret : rets.get(Boolean.TRUE)) {
					Assert.assertEquals(dateCreationEntreprise, ret.getDateDebut());
					Assert.assertEquals(datePrononceFaillite, ret.getDateFin());
					Assert.assertEquals(TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE, ret.getType());
					Assert.assertFalse(((ActiviteEconomique) ret).isPrincipal());
				}

				// 3. les adresses mandataires doivent être ré-ouvertes
				final Set<AdresseMandataire> adressesMandataires = entreprise.getAdressesMandataires();
				Assert.assertNotNull(adressesMandataires);
				Assert.assertEquals(2, adressesMandataires.size());     // une annulée et une ouverte
				final Map<Boolean, List<AdresseMandataire>> ams = segmenterAnnulables(adressesMandataires);
				Assert.assertEquals(1, ams.get(Boolean.TRUE).size());
				Assert.assertEquals(1, ams.get(Boolean.FALSE).size());
				for (AdresseMandataire adresse : ams.get(Boolean.TRUE)) {
					Assert.assertEquals(dateCreationEntreprise, adresse.getDateDebut());
					Assert.assertEquals(datePrononceFaillite, adresse.getDateFin());
				}
				for (AdresseMandataire adresse : ams.get(Boolean.FALSE)) {
					Assert.assertEquals(dateCreationEntreprise, adresse.getDateDebut());
					Assert.assertNull(adresse.getDateFin());
				}

				// 4. état EN_FAILLITE sur l'entreprise (annulé)
				final EtatEntreprise etatActuel = entreprise.getEtatActuel();
				Assert.assertNotNull(etatActuel);
				Assert.assertEquals(TypeEtatEntreprise.FONDEE, etatActuel.getType());
				Assert.assertEquals(dateCreationEntreprise, etatActuel.getDateObtention());
				Assert.assertEquals(TypeGenerationEtatEntreprise.AUTOMATIQUE, etatActuel.getGeneration());

				// 5. et la remarque ?
				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				final Remarque remarque = remarques.iterator().next();
				Assert.assertNotNull(remarque);
				Assert.assertFalse(remarque.isAnnule());
				Assert.assertEquals("Une jolie remarque toute belle pour l'annulation...", remarque.getTexte());

				// 6. événements fiscaux
				final Collection<EvenementFiscal> evts = evenementFiscalDAO.getEvenementsFiscaux(entreprise);
				Assert.assertEquals(5, evts.size());        // 2 annulations de for + 2 ouvertures de for + une info complémentaire (annulation faillite)
				final Map<Class<? extends EvenementFiscal>, List<EvenementFiscal>> evtsParClasse = segmenterParClasse(evts);
				Assert.assertEquals(2, evtsParClasse.size());
				Assert.assertTrue(evtsParClasse.containsKey(EvenementFiscalInformationComplementaire.class));
				Assert.assertTrue(evtsParClasse.containsKey(EvenementFiscalFor.class));

				final Map<EvenementFiscalFor.TypeEvenementFiscalFor, List<EvenementFiscal>> evtsParType = segmenter(evtsParClasse.get(EvenementFiscalFor.class), new Extractor<EvenementFiscal, EvenementFiscalFor.TypeEvenementFiscalFor>() {
					@Override
					public EvenementFiscalFor.TypeEvenementFiscalFor extract(EvenementFiscal source) {
						return ((EvenementFiscalFor) source).getType();
					}
				});
				Assert.assertEquals(EnumSet.of(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, EvenementFiscalFor.TypeEvenementFiscalFor.ANNULATION), evtsParType.keySet());
				Assert.assertEquals(2, evtsParType.get(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE).size());
				Assert.assertEquals(2, evtsParType.get(EvenementFiscalFor.TypeEvenementFiscalFor.ANNULATION).size());
				for (EvenementFiscal ef : evtsParType.get(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE)) {
					final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					Assert.assertNotNull(eff);
					Assert.assertFalse(eff.isAnnule());
					Assert.assertEquals(dateCreationEntreprise, eff.getDateValeur());
				}
				for (EvenementFiscal ef : evtsParType.get(EvenementFiscalFor.TypeEvenementFiscalFor.ANNULATION)) {
					final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					Assert.assertNotNull(eff);
					Assert.assertFalse(eff.isAnnule());
					Assert.assertEquals(dateCreationEntreprise, eff.getDateValeur());
				}

				final List<EvenementFiscal> evtsInfoComplementaire = evtsParClasse.get(EvenementFiscalInformationComplementaire.class);
				Assert.assertNotNull(evtsInfoComplementaire);
				Assert.assertEquals(1, evtsInfoComplementaire.size());
				final EvenementFiscalInformationComplementaire infoComplementaire = (EvenementFiscalInformationComplementaire) evtsInfoComplementaire.get(0);
				Assert.assertNotNull(infoComplementaire);
				Assert.assertEquals(EvenementFiscalInformationComplementaire.TypeInformationComplementaire.ANNULATION_FAILLITE, infoComplementaire.getType());
				Assert.assertEquals(datePrononceFaillite, infoComplementaire.getDateValeur());
			}
		});
	}

	@Test
	public void testRevocationFaillite() throws Exception {

		final RegDate dateCreationEntreprise = date(2000, 4, 1);
		final RegDate datePrononceFaillite = date(2010, 4, 13);

		final class Ids {
			long idEntreprise;
			long idEtablissementPrincipal;
			long idEtablissementSecondaire;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateCreationEntreprise, null, "Ma petite entreprise");
				addFormeJuridique(entreprise, dateCreationEntreprise, null, FormeJuridiqueEntreprise.ASSOCIATION);
				addRegimeFiscalCH(entreprise, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
				addRegimeFiscalVD(entreprise, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
				addBouclement(entreprise, dateCreationEntreprise, DayMonth.get(12, 31), 12);        // tous les 31.12 depuis 2000
				addForPrincipal(entreprise, dateCreationEntreprise, MotifFor.DEBUT_EXPLOITATION, datePrononceFaillite, MotifFor.FAILLITE, MockCommune.Grandson);
				addForSecondaire(entreprise, dateCreationEntreprise, MotifFor.DEBUT_EXPLOITATION, datePrononceFaillite, MotifFor.FAILLITE, MockCommune.ChateauDoex, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
				entreprise.setBlocageRemboursementAutomatique(Boolean.TRUE);

				addAdresseMandataireSuisse(entreprise, dateCreationEntreprise, datePrononceFaillite, TypeMandat.GENERAL, "Mon mandataire chéri", MockRue.Renens.QuatorzeAvril);

				final Etablissement etablissementPrincipal = addEtablissement();
				addDomicileEtablissement(etablissementPrincipal, dateCreationEntreprise, null, MockCommune.Grandson);
				addActiviteEconomique(entreprise, etablissementPrincipal, dateCreationEntreprise, null, true);

				final Etablissement etablissementSecondaire = addEtablissement();
				addDomicileEtablissement(etablissementSecondaire, dateCreationEntreprise, null, MockCommune.ChateauDoex);
				addActiviteEconomique(entreprise, etablissementSecondaire, dateCreationEntreprise, datePrononceFaillite, false);

				addEtatEntreprise(entreprise, dateCreationEntreprise, TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.AUTOMATIQUE);
				addEtatEntreprise(entreprise, datePrononceFaillite, TypeEtatEntreprise.EN_FAILLITE, TypeGenerationEtatEntreprise.MANUELLE);

				final Ids ids = new Ids();
				ids.idEntreprise = entreprise.getNumero();
				ids.idEtablissementPrincipal = etablissementPrincipal.getNumero();
				ids.idEtablissementSecondaire = etablissementSecondaire.getNumero();
				return ids;
			}
		});

		// traitement de la révocation de la faillite
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
				Assert.assertNotNull(entreprise);
				Assert.assertTrue(entreprise.getBlocageRemboursementAutomatique());
				metierServicePM.revoqueFaillite(entreprise, datePrononceFaillite, "Une jolie remarque toute belle pour la révocation...");
			}
		});

		// vérification des résultats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
				Assert.assertNotNull(entreprise);
				Assert.assertFalse(entreprise.getBlocageRemboursementAutomatique());

				// 1. le for principal doit être ré-ouvert
				final Set<ForFiscal> forsFiscaux = entreprise.getForsFiscaux();
				Assert.assertNotNull(forsFiscaux);
				Assert.assertEquals(3, forsFiscaux.size());     // 1 annulé, et 1 ré-ouvert (et un laissé fermé, le secondaire)
				final Map<Boolean, List<ForFiscal>> fors = segmenterAnnulables(forsFiscaux);
				Assert.assertEquals(1, fors.get(Boolean.TRUE).size());
				Assert.assertEquals(2, fors.get(Boolean.FALSE).size());
				for (ForFiscal ff : fors.get(Boolean.TRUE)) {
					Assert.assertEquals(dateCreationEntreprise, ff.getDateDebut());
					Assert.assertEquals(datePrononceFaillite, ff.getDateFin());
					Assert.assertTrue(ff.getClass().getName(), ff instanceof ForFiscalAvecMotifs);
					Assert.assertEquals(MotifFor.FAILLITE, ((ForFiscalAvecMotifs) ff).getMotifFermeture());
					Assert.assertEquals(ForFiscalPrincipalPM.class, ff.getClass());
				}
				final Map<Class<? extends ForFiscal>, List<ForFiscal>> forsNonAnnulesParClasse = segmenterParClasse(fors.get(Boolean.FALSE));
				Assert.assertEquals(new HashSet<>(Arrays.asList(ForFiscalPrincipalPM.class, ForFiscalSecondaire.class)), forsNonAnnulesParClasse.keySet());
				Assert.assertEquals(1, forsNonAnnulesParClasse.get(ForFiscalPrincipalPM.class).size());
				Assert.assertEquals(1, forsNonAnnulesParClasse.get(ForFiscalSecondaire.class).size());
				for (ForFiscal ff : forsNonAnnulesParClasse.get(ForFiscalPrincipalPM.class)) {
					Assert.assertEquals(dateCreationEntreprise, ff.getDateDebut());
					Assert.assertNull(ff.getDateFin());
					Assert.assertTrue(ff.getClass().getName(), ff instanceof ForFiscalAvecMotifs);
					Assert.assertNull(((ForFiscalAvecMotifs) ff).getMotifFermeture());
				}
				for (ForFiscal ff : forsNonAnnulesParClasse.get(ForFiscalSecondaire.class)) {
					Assert.assertEquals(dateCreationEntreprise, ff.getDateDebut());
					Assert.assertEquals(datePrononceFaillite, ff.getDateFin());
					Assert.assertTrue(ff.getClass().getName(), ff instanceof ForFiscalAvecMotifs);
					Assert.assertEquals(MotifFor.FAILLITE, ((ForFiscalAvecMotifs) ff).getMotifFermeture());
				}

				// 2. le rapport entre tiers vers l'établissement secondaire ne doit pas être ré-ouvert
				final Set<RapportEntreTiers> rapportsSujet = entreprise.getRapportsSujet();
				Assert.assertNotNull(rapportsSujet);
				Assert.assertEquals(2, rapportsSujet.size());           // 1 ouvert, 1 fermé, comme avant la révocation
				final Map<Boolean, List<RapportEntreTiers>> rets = segmenterAnnulables(rapportsSujet);
				Assert.assertEquals(0, rets.get(Boolean.TRUE).size());
				Assert.assertEquals(2, rets.get(Boolean.FALSE).size());
				final Map<Boolean, List<RapportEntreTiers>> activitesEconomiques = segmenter(rets.get(Boolean.FALSE), new Extractor<RapportEntreTiers, Boolean>() {
					@Override
					public Boolean extract(RapportEntreTiers source) {
						Assert.assertTrue(source.getClass().getName(), source instanceof ActiviteEconomique);
						return ((ActiviteEconomique) source).isPrincipal();
					}
				});
				Assert.assertEquals(1, activitesEconomiques.get(Boolean.TRUE).size());
				Assert.assertEquals(1, activitesEconomiques.get(Boolean.FALSE).size());
				for (RapportEntreTiers ret : activitesEconomiques.get(Boolean.FALSE)) {
					Assert.assertEquals(dateCreationEntreprise, ret.getDateDebut());
					Assert.assertEquals(datePrononceFaillite, ret.getDateFin());
				}
				for (RapportEntreTiers ret : activitesEconomiques.get(Boolean.TRUE)) {
					Assert.assertEquals(dateCreationEntreprise, ret.getDateDebut());
					Assert.assertNull(ret.getDateFin());
				}

				// 3. les adresses mandataires ne doivent pas être ré-ouvertes
				final Set<AdresseMandataire> adressesMandataires = entreprise.getAdressesMandataires();
				Assert.assertNotNull(adressesMandataires);
				Assert.assertEquals(1, adressesMandataires.size());     // l'adresse fermée
				final AdresseMandataire adresseMandataire = adressesMandataires.iterator().next();
				Assert.assertNotNull(adresseMandataire);
				Assert.assertFalse(adresseMandataire.isAnnule());
				Assert.assertEquals(dateCreationEntreprise, adresseMandataire.getDateDebut());
				Assert.assertEquals(datePrononceFaillite, adresseMandataire.getDateFin());

				// 4. état EN_FAILLITE sur l'entreprise (annulé)
				final EtatEntreprise etatActuel = entreprise.getEtatActuel();
				Assert.assertNotNull(etatActuel);
				Assert.assertEquals(TypeEtatEntreprise.FONDEE, etatActuel.getType());
				Assert.assertEquals(dateCreationEntreprise, etatActuel.getDateObtention());
				Assert.assertEquals(TypeGenerationEtatEntreprise.AUTOMATIQUE, etatActuel.getGeneration());

				// 5. et la remarque ?
				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				final Remarque remarque = remarques.iterator().next();
				Assert.assertNotNull(remarque);
				Assert.assertFalse(remarque.isAnnule());
				Assert.assertEquals("Une jolie remarque toute belle pour la révocation...", remarque.getTexte());

				// 6. événements fiscaux
				final Collection<EvenementFiscal> evts = evenementFiscalDAO.getEvenementsFiscaux(entreprise);
				Assert.assertEquals(3, evts.size());        // 1 annulation de for + 1 ouverture de for + 1 information complémentaire
				final Map<Class<? extends EvenementFiscal>, List<EvenementFiscal>> evtsParClass = segmenterParClasse(evts);
				Assert.assertEquals(new HashSet<>(Arrays.asList(EvenementFiscalFor.class, EvenementFiscalInformationComplementaire.class)), evtsParClass.keySet());
				Assert.assertEquals(2, evtsParClass.get(EvenementFiscalFor.class).size());
				Assert.assertEquals(1, evtsParClass.get(EvenementFiscalInformationComplementaire.class).size());
				boolean annulationTrouvee = false;
				boolean ouvertureTrouvee = false;
				for (EvenementFiscal ef : evtsParClass.get(EvenementFiscalFor.class)) {
					final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					Assert.assertNotNull(eff);
					Assert.assertFalse(eff.isAnnule());
					Assert.assertEquals(dateCreationEntreprise, eff.getDateValeur());
					Assert.assertTrue(eff.getForFiscal().isPrincipal());
					if (eff.getType() == EvenementFiscalFor.TypeEvenementFiscalFor.ANNULATION) {
						Assert.assertFalse(annulationTrouvee);
						annulationTrouvee = true;
					}
					else if (eff.getType() == EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE) {
						Assert.assertFalse(ouvertureTrouvee);
						ouvertureTrouvee = true;
					}
					else {
						Assert.fail("Type inattendu : " + eff.getType());
					}
				}
				Assert.assertTrue(annulationTrouvee);
				Assert.assertTrue(ouvertureTrouvee);
				for (EvenementFiscal ef : evtsParClass.get(EvenementFiscalInformationComplementaire.class)) {
					final EvenementFiscalInformationComplementaire efic = (EvenementFiscalInformationComplementaire) ef;
					Assert.assertNotNull(efic);
					Assert.assertFalse(efic.isAnnule());
					Assert.assertEquals(RegDate.get(), efic.getDateValeur());
					Assert.assertEquals(EvenementFiscalInformationComplementaire.TypeInformationComplementaire.REVOCATION_FAILLITE, efic.getType());
				}
			}
		});
	}

	@Test
	public void testCalculForsSecondaires() throws Exception {

		final RegDate dateCreationEntreprise = date(2000, 4, 1);
		final RegDate dateCreationEtablissementSecondaire = date(2010, 4, 13);

		final class Ids {
			long idEntreprise;
			long idEtablissementPrincipal;
			long idEtablissementSecondaire;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateCreationEntreprise, null, "Ma petite association");
				addFormeJuridique(entreprise, dateCreationEntreprise, null, FormeJuridiqueEntreprise.ASSOCIATION);
				addRegimeFiscalCH(entreprise, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
				addRegimeFiscalVD(entreprise, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
				addBouclement(entreprise, dateCreationEntreprise, DayMonth.get(12, 31), 12);        // tous les 31.12 depuis 2000

				addForPrincipal(entreprise, dateCreationEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, GenreImpot.BENEFICE_CAPITAL);

				final Etablissement etablissementPrincipal = addEtablissement();
				addDomicileEtablissement(etablissementPrincipal, dateCreationEntreprise, null, MockCommune.Grandson);
				addActiviteEconomique(entreprise, etablissementPrincipal, dateCreationEntreprise, null, true);

				final Etablissement etablissementSecondaire = addEtablissement();
				addDomicileEtablissement(etablissementSecondaire, dateCreationEtablissementSecondaire, null, MockCommune.ChateauDoex);
				addActiviteEconomique(entreprise, etablissementSecondaire, dateCreationEtablissementSecondaire, null, false);

				addEtatEntreprise(entreprise, dateCreationEntreprise, TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.AUTOMATIQUE);

				final Ids ids = new Ids();
				ids.idEntreprise = entreprise.getNumero();
				ids.idEtablissementPrincipal = etablissementPrincipal.getNumero();
				ids.idEtablissementSecondaire = etablissementSecondaire.getNumero();
				return ids;
			}
		});

		final AjustementForsSecondairesResult ajustementForsSecondairesResult = doInNewTransactionAndSession(new TxCallback<AjustementForsSecondairesResult>() {
			@Override
			public AjustementForsSecondairesResult execute(TransactionStatus status) throws Exception {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
				Assert.assertNotNull(entreprise);
				return metierServicePM.calculAjustementForsSecondairesPourEtablissementsVD(entreprise);
			}
		});

		Assert.assertNotNull(ajustementForsSecondairesResult);

		Assert.assertEquals(0, ajustementForsSecondairesResult.getAFermer().size());
		Assert.assertEquals(0, ajustementForsSecondairesResult.getAAnnuler().size());
		final List<ForFiscalSecondaire> aCreer = ajustementForsSecondairesResult.getACreer();
		Assert.assertEquals(1, aCreer.size());
		final ForFiscalSecondaire forFiscalSecondaire = aCreer.get(0);
		Assert.assertNotNull(forFiscalSecondaire);
		Assert.assertEquals(date(2010, 4, 13), forFiscalSecondaire.getDateDebut());
		Assert.assertEquals(MockCommune.ChateauDoex.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().longValue());
		Assert.assertNull(forFiscalSecondaire.getDateFin());
		Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
		Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
	}

	@Test
	public void testDemenagementSiegeEntrepriseInconnueAuCivil() throws Exception {

		final RegDate dateCreation = date(2000, 5, 1);
		final RegDate dateDemenagement = date(2010, 6, 23);

		final class Ids {
			long idEntreprise;
			long idEtablissementPrincipal;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateCreation, null, "Ma petite entreprise");
				addFormeJuridique(entreprise, dateCreation, null, FormeJuridiqueEntreprise.ASSOCIATION);
				addRegimeFiscalCH(entreprise, dateCreation, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
				addRegimeFiscalVD(entreprise, dateCreation, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
				addBouclement(entreprise, dateCreation, DayMonth.get(12, 31), 12);        // tous les 31.12 depuis 2000
				addForPrincipal(entreprise, dateCreation, MotifFor.DEBUT_EXPLOITATION, MockCommune.Grandson);
				addForSecondaire(entreprise, dateCreation, MotifFor.DEBUT_EXPLOITATION, MockCommune.ChateauDoex, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
				entreprise.setBlocageRemboursementAutomatique(Boolean.FALSE);

				addAdresseMandataireSuisse(entreprise, dateCreation, null, TypeMandat.GENERAL, "Mon mandataire chéri", MockRue.Renens.QuatorzeAvril);

				final Etablissement etablissementPrincipal = addEtablissement();
				addDomicileEtablissement(etablissementPrincipal, dateCreation, null, MockCommune.Grandson);
				addActiviteEconomique(entreprise, etablissementPrincipal, dateCreation, null, true);

				addEtatEntreprise(entreprise, dateCreation, TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.AUTOMATIQUE);

				final Ids ids = new Ids();
				ids.idEntreprise = entreprise.getNumero();
				ids.idEtablissementPrincipal = etablissementPrincipal.getNumero();
				return ids;
			}
		});

		// déménagement de siège vers Lausanne
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
				Assert.assertNotNull(entreprise);
				Assert.assertFalse(entreprise.getBlocageRemboursementAutomatique());
				metierServicePM.demenageSiege(entreprise, dateDemenagement, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS());
			}
		});

		// vérification des données après déménagement
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
				Assert.assertNotNull(entreprise);
				Assert.assertFalse(entreprise.getBlocageRemboursementAutomatique());

				// 1. le for principal doit avoir été déplacé
				final ForsParType fpt = entreprise.getForsParType(true);
				final List<ForFiscalPrincipalPM> forsPrincipaux = fpt.principauxPM;
				Assert.assertNotNull(forsPrincipaux);
				Assert.assertEquals(2, forsPrincipaux.size());
				{
					final ForFiscalPrincipalPM ffp = forsPrincipaux.get(0);
					Assert.assertNotNull(ffp);
					Assert.assertFalse(ffp.isAnnule());
					Assert.assertEquals(dateCreation, ffp.getDateDebut());
					Assert.assertEquals(dateDemenagement.getOneDayBefore(), ffp.getDateFin());
					Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, ffp.getMotifOuverture());
					Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, ffp.getMotifFermeture());
					Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, ffp.getGenreImpot());
					Assert.assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Grandson.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
				}
				{
					final ForFiscalPrincipalPM ffp = forsPrincipaux.get(1);
					Assert.assertNotNull(ffp);
					Assert.assertFalse(ffp.isAnnule());
					Assert.assertEquals(dateDemenagement, ffp.getDateDebut());
					Assert.assertNull(ffp.getDateFin());
					Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, ffp.getMotifOuverture());
					Assert.assertNull(ffp.getMotifFermeture());
					Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, ffp.getGenreImpot());
					Assert.assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
				}

				// 2. le for secondaire ne doit pas avoir bougé
				final List<ForFiscalSecondaire> forsSecondaires = fpt.secondaires;
				Assert.assertNotNull(forsSecondaires);
				Assert.assertEquals(1, forsSecondaires.size());
				final ForFiscalSecondaire ffs = forsSecondaires.get(0);
				Assert.assertNotNull(ffs);
				Assert.assertFalse(ffs.isAnnule());
				Assert.assertEquals(dateCreation, ffs.getDateDebut());
				Assert.assertNull(ffs.getDateFin());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, ffs.getMotifOuverture());
				Assert.assertNull(ffs.getMotifFermeture());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, ffs.getGenreImpot());
				Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, ffs.getMotifRattachement());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffs.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.ChateauDoex.getNoOFS(), ffs.getNumeroOfsAutoriteFiscale());

				// 3. le domicile de l'établissement principal doit avoir bougé
				final Etablissement etb = (Etablissement) tiersDAO.get(ids.idEtablissementPrincipal);
				Assert.assertNotNull(etb);
				Assert.assertFalse(etb.isAnnule());

				final List<DomicileEtablissement> domiciles = etb.getSortedDomiciles(true);
				Assert.assertNotNull(domiciles);
				Assert.assertEquals(2, domiciles.size());
				{
					final DomicileEtablissement domicile = domiciles.get(0);
					Assert.assertNotNull(domicile);
					Assert.assertFalse(domicile.isAnnule());
					Assert.assertEquals(dateCreation, domicile.getDateDebut());
					Assert.assertEquals(dateDemenagement.getOneDayBefore(), domicile.getDateFin());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Grandson.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale());
				}
				{
					final DomicileEtablissement domicile = domiciles.get(1);
					Assert.assertNotNull(domicile);
					Assert.assertFalse(domicile.isAnnule());
					Assert.assertEquals(dateDemenagement, domicile.getDateDebut());
					Assert.assertNull(domicile.getDateFin());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale());
				}
			}
		});
	}

	@Test
	public void testDemenagementSiegeEntrepriseConnueAuCivilVersHorsCantonAvecForSecondaire() throws Exception {

		final long noCantonalEntreprise = 378326478L;
		final long noCantonalEtablissementPrincipal = 4378257L;
		final RegDate dateCreation = date(2000, 5, 1);
		final RegDate dateDemenagement = date(2010, 6, 23);

		// mise en place civile
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = addOrganisation(noCantonalEntreprise);
				MockSiteOrganisationFactory.addSite(noCantonalEtablissementPrincipal, org, dateCreation, null, "Titi et ses amis", FormeLegale.N_0109_ASSOCIATION,
				                                    true, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                    MockCommune.Grandson.getNoOFS(), StatusInscriptionRC.NON_INSCRIT, null,
				                                    StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.SITE, "CHE999999996", null, null);
			}
		});

		final class Ids {
			long idEntreprise;
			long idEtablissementPrincipal;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final Entreprise entreprise = addEntrepriseConnueAuCivil(noCantonalEntreprise);
				addRegimeFiscalCH(entreprise, dateCreation, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
				addRegimeFiscalVD(entreprise, dateCreation, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
				addBouclement(entreprise, dateCreation, DayMonth.get(12, 31), 12);        // tous les 31.12 depuis 2000
				addForPrincipal(entreprise, dateCreation, MotifFor.DEBUT_EXPLOITATION, MockCommune.Grandson);
				addForSecondaire(entreprise, dateCreation, MotifFor.DEBUT_EXPLOITATION, MockCommune.ChateauDoex, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
				entreprise.setBlocageRemboursementAutomatique(Boolean.FALSE);

				addAdresseMandataireSuisse(entreprise, dateCreation, null, TypeMandat.GENERAL, "Mon mandataire chéri", MockRue.Renens.QuatorzeAvril);

				final Etablissement etablissementPrincipal = addEtablissement();
				etablissementPrincipal.setNumeroEtablissement(noCantonalEtablissementPrincipal);
				addActiviteEconomique(entreprise, etablissementPrincipal, dateCreation, null, true);

				addEtatEntreprise(entreprise, dateCreation, TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.AUTOMATIQUE);

				final Ids ids = new Ids();
				ids.idEntreprise = entreprise.getNumero();
				ids.idEtablissementPrincipal = etablissementPrincipal.getNumero();
				return ids;
			}
		});

		// déménagement de siège vers Sierre (VS)
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
				Assert.assertNotNull(entreprise);
				Assert.assertFalse(entreprise.getBlocageRemboursementAutomatique());
				metierServicePM.demenageSiege(entreprise, dateDemenagement, TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Sierre.getNoOFS());
			}
		});

		// vérification des données après déménagement
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
				Assert.assertNotNull(entreprise);
				Assert.assertFalse(entreprise.getBlocageRemboursementAutomatique());     // reste débloqué car, même si le for principal est hors-Canton, il y a un for secondaire ouvert

				// 1. le for principal doit avoir été déplacé
				final ForsParType fpt = entreprise.getForsParType(true);
				final List<ForFiscalPrincipalPM> forsPrincipaux = fpt.principauxPM;
				Assert.assertNotNull(forsPrincipaux);
				Assert.assertEquals(2, forsPrincipaux.size());
				{
					final ForFiscalPrincipalPM ffp = forsPrincipaux.get(0);
					Assert.assertNotNull(ffp);
					Assert.assertFalse(ffp.isAnnule());
					Assert.assertEquals(dateCreation, ffp.getDateDebut());
					Assert.assertEquals(dateDemenagement.getOneDayBefore(), ffp.getDateFin());
					Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, ffp.getMotifOuverture());
					Assert.assertEquals(MotifFor.DEPART_HC, ffp.getMotifFermeture());
					Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, ffp.getGenreImpot());
					Assert.assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Grandson.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
				}
				{
					final ForFiscalPrincipalPM ffp = forsPrincipaux.get(1);
					Assert.assertNotNull(ffp);
					Assert.assertFalse(ffp.isAnnule());
					Assert.assertEquals(dateDemenagement, ffp.getDateDebut());
					Assert.assertNull(ffp.getDateFin());
					Assert.assertEquals(MotifFor.DEPART_HC, ffp.getMotifOuverture());
					Assert.assertNull(ffp.getMotifFermeture());
					Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, ffp.getGenreImpot());
					Assert.assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Sierre.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
				}

				// 2. le for secondaire ne doit pas avoir bougé
				final List<ForFiscalSecondaire> forsSecondaires = fpt.secondaires;
				Assert.assertNotNull(forsSecondaires);
				Assert.assertEquals(1, forsSecondaires.size());
				final ForFiscalSecondaire ffs = forsSecondaires.get(0);
				Assert.assertNotNull(ffs);
				Assert.assertFalse(ffs.isAnnule());
				Assert.assertEquals(dateCreation, ffs.getDateDebut());
				Assert.assertNull(ffs.getDateFin());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, ffs.getMotifOuverture());
				Assert.assertNull(ffs.getMotifFermeture());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, ffs.getGenreImpot());
				Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, ffs.getMotifRattachement());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffs.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.ChateauDoex.getNoOFS(), ffs.getNumeroOfsAutoriteFiscale());

				// 3. le domicile de l'établissement principal ne doit pas avoir bougé
				final Etablissement etb = (Etablissement) tiersDAO.get(ids.idEtablissementPrincipal);
				Assert.assertNotNull(etb);
				Assert.assertFalse(etb.isAnnule());

				final List<DomicileEtablissement> domiciles = etb.getSortedDomiciles(true);
				Assert.assertNotNull(domiciles);
				Assert.assertEquals(0, domiciles.size());       // tout est géré au civil
			}
		});
	}

	@Test
	public void testDemenagementSiegeEntrepriseConnueAuCivilVersHorsCantonSansForSecondaire() throws Exception {

		final long noCantonalEntreprise = 378326478L;
		final long noCantonalEtablissementPrincipal = 4378257L;
		final RegDate dateCreation = date(2000, 5, 1);
		final RegDate dateDemenagement = date(2010, 6, 23);

		// mise en place civile
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = addOrganisation(noCantonalEntreprise);
				MockSiteOrganisationFactory.addSite(noCantonalEtablissementPrincipal, org, dateCreation, null, "Titi et ses amis", FormeLegale.N_0109_ASSOCIATION,
				                                    true, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                    MockCommune.Grandson.getNoOFS(), StatusInscriptionRC.NON_INSCRIT, null,
				                                    StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.SITE, "CHE999999996", null, null);
			}
		});

		final class Ids {
			long idEntreprise;
			long idEtablissementPrincipal;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final Entreprise entreprise = addEntrepriseConnueAuCivil(noCantonalEntreprise);
				addRegimeFiscalCH(entreprise, dateCreation, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
				addRegimeFiscalVD(entreprise, dateCreation, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
				addBouclement(entreprise, dateCreation, DayMonth.get(12, 31), 12);        // tous les 31.12 depuis 2000
				addForPrincipal(entreprise, dateCreation, MotifFor.DEBUT_EXPLOITATION, MockCommune.Grandson);
				entreprise.setBlocageRemboursementAutomatique(Boolean.FALSE);

				addAdresseMandataireSuisse(entreprise, dateCreation, null, TypeMandat.GENERAL, "Mon mandataire chéri", MockRue.Renens.QuatorzeAvril);

				final Etablissement etablissementPrincipal = addEtablissement();
				etablissementPrincipal.setNumeroEtablissement(noCantonalEtablissementPrincipal);
				addActiviteEconomique(entreprise, etablissementPrincipal, dateCreation, null, true);

				addEtatEntreprise(entreprise, dateCreation, TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.AUTOMATIQUE);

				final Ids ids = new Ids();
				ids.idEntreprise = entreprise.getNumero();
				ids.idEtablissementPrincipal = etablissementPrincipal.getNumero();
				return ids;
			}
		});

		// déménagement de siège vers Sierre (VS)
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
				Assert.assertNotNull(entreprise);
				Assert.assertFalse(entreprise.getBlocageRemboursementAutomatique());
				metierServicePM.demenageSiege(entreprise, dateDemenagement, TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Sierre.getNoOFS());
			}
		});

		// vérification des données après déménagement
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
				Assert.assertNotNull(entreprise);
				Assert.assertTrue(entreprise.getBlocageRemboursementAutomatique());     // bloqué car le for principal est hors-Canton en absence de for secondaire

				// 1. le for principal doit avoir été déplacé
				final ForsParType fpt = entreprise.getForsParType(true);
				final List<ForFiscalPrincipalPM> forsPrincipaux = fpt.principauxPM;
				Assert.assertNotNull(forsPrincipaux);
				Assert.assertEquals(2, forsPrincipaux.size());
				{
					final ForFiscalPrincipalPM ffp = forsPrincipaux.get(0);
					Assert.assertNotNull(ffp);
					Assert.assertFalse(ffp.isAnnule());
					Assert.assertEquals(dateCreation, ffp.getDateDebut());
					Assert.assertEquals(dateDemenagement.getOneDayBefore(), ffp.getDateFin());
					Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, ffp.getMotifOuverture());
					Assert.assertEquals(MotifFor.DEPART_HC, ffp.getMotifFermeture());
					Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, ffp.getGenreImpot());
					Assert.assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Grandson.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
				}
				{
					final ForFiscalPrincipalPM ffp = forsPrincipaux.get(1);
					Assert.assertNotNull(ffp);
					Assert.assertFalse(ffp.isAnnule());
					Assert.assertEquals(dateDemenagement, ffp.getDateDebut());
					Assert.assertNull(ffp.getDateFin());
					Assert.assertEquals(MotifFor.DEPART_HC, ffp.getMotifOuverture());
					Assert.assertNull(ffp.getMotifFermeture());
					Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, ffp.getGenreImpot());
					Assert.assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Sierre.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
				}

				// 2. toujours pas de for secondaire...
				final List<ForFiscalSecondaire> forsSecondaires = fpt.secondaires;
				Assert.assertNotNull(forsSecondaires);
				Assert.assertEquals(0, forsSecondaires.size());

				// 3. le domicile de l'établissement principal ne doit pas avoir bougé
				final Etablissement etb = (Etablissement) tiersDAO.get(ids.idEtablissementPrincipal);
				Assert.assertNotNull(etb);
				Assert.assertFalse(etb.isAnnule());

				final List<DomicileEtablissement> domiciles = etb.getSortedDomiciles(true);
				Assert.assertNotNull(domiciles);
				Assert.assertEquals(0, domiciles.size());       // tout est géré au civil
			}
		});
	}

	@Test
	public void testFusionEntreprises() throws Exception {

		final RegDate dateDebutAbsorbante = date(2000, 5, 12);
		final RegDate dateDebutAbsorbee1 = date(2005, 6, 13);
		final RegDate dateDebutAbsorbee2 = date(2007, 9, 30);

		final RegDate dateContratFusion = date(2016, 4, 2);
		final RegDate dateBilanFusion = date(2015, 12, 31);

		final class Ids {
			long idAbsorbante;
			long idEtablissementPrincipalAbsorbante;
			long idAbsorbee1;
			long idEtablissementPrincipalAbsorbee1;
			long idAbsorbee2;
			long idEtablissementPrincipalAbsorbee2;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final Entreprise absorbante = addEntrepriseInconnueAuCivil();
				addRaisonSociale(absorbante, dateDebutAbsorbante, null, "Ma grande entreprise");
				addFormeJuridique(absorbante, dateDebutAbsorbante, null, FormeJuridiqueEntreprise.SA);
				addRegimeFiscalCH(absorbante, dateDebutAbsorbante, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalVD(absorbante, dateDebutAbsorbante, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(absorbante, dateDebutAbsorbante, DayMonth.get(12, 31), 12);        // tous les 31.12 depuis 2000
				addForPrincipal(absorbante, dateDebutAbsorbante, MotifFor.DEBUT_EXPLOITATION, MockCommune.Grandson);
				addForSecondaire(absorbante, dateDebutAbsorbante, MotifFor.DEBUT_EXPLOITATION, MockCommune.ChateauDoex, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
				absorbante.setBlocageRemboursementAutomatique(Boolean.FALSE);

				addAdresseMandataireSuisse(absorbante, dateDebutAbsorbante, null, TypeMandat.GENERAL, "Mon mandataire chéri", MockRue.Renens.QuatorzeAvril);
				addAdresseSuisse(absorbante, TypeAdresseTiers.COURRIER, dateDebutAbsorbante, null, MockRue.Prilly.RueDesMetiers);

				final Etablissement etablissementPrincipalAbsorbante = addEtablissement();
				addDomicileEtablissement(etablissementPrincipalAbsorbante, dateDebutAbsorbante, null, MockCommune.Grandson);
				addActiviteEconomique(absorbante, etablissementPrincipalAbsorbante, dateDebutAbsorbante, null, true);

				addEtatEntreprise(absorbante, dateDebutAbsorbante, TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.AUTOMATIQUE);


				final Entreprise absorbee1 = addEntrepriseInconnueAuCivil();
				addRaisonSociale(absorbee1, dateDebutAbsorbee1, null, "Ma toute petite entreprise");
				addFormeJuridique(absorbee1, dateDebutAbsorbee1, null, FormeJuridiqueEntreprise.SA);
				addRegimeFiscalCH(absorbee1, dateDebutAbsorbee1, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalVD(absorbee1, dateDebutAbsorbee1, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(absorbee1, dateDebutAbsorbee1, DayMonth.get(12, 31), 12);        // tous les 31.12 depuis 2005
				addForPrincipal(absorbee1, dateDebutAbsorbee1, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
				absorbee1.setBlocageRemboursementAutomatique(Boolean.FALSE);

				final Etablissement etablissementPrincipalAbsorbee1 = addEtablissement();
				addDomicileEtablissement(etablissementPrincipalAbsorbee1, dateDebutAbsorbee1, null, MockCommune.Lausanne);
				addActiviteEconomique(absorbee1, etablissementPrincipalAbsorbee1, dateDebutAbsorbee1, null, true);

				addEtatEntreprise(absorbee1, dateDebutAbsorbee1, TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.AUTOMATIQUE);


				final Entreprise absorbee2 = addEntrepriseInconnueAuCivil();
				addRaisonSociale(absorbee2, dateDebutAbsorbee2, null, "Ma minuscule entreprise");
				addFormeJuridique(absorbee2, dateDebutAbsorbee2, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalCH(absorbee2, dateDebutAbsorbee2, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalVD(absorbee2, dateDebutAbsorbee2, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(absorbee2, dateDebutAbsorbee2, DayMonth.get(12, 31), 12);        // tous les 31.12 depuis 2007
				addForPrincipal(absorbee2, dateDebutAbsorbee2, MotifFor.DEBUT_EXPLOITATION, MockCommune.Prilly);
				absorbee2.setBlocageRemboursementAutomatique(Boolean.FALSE);

				final Etablissement etablissementPrincipalAbsorbee2 = addEtablissement();
				addDomicileEtablissement(etablissementPrincipalAbsorbee2, dateDebutAbsorbee2, null, MockCommune.Prilly);
				addActiviteEconomique(absorbee2, etablissementPrincipalAbsorbee2, dateDebutAbsorbee2, null, true);

				addEtatEntreprise(absorbee2, dateDebutAbsorbee2, TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.AUTOMATIQUE);


				final Ids ids = new Ids();
				ids.idAbsorbante = absorbante.getNumero();
				ids.idEtablissementPrincipalAbsorbante = etablissementPrincipalAbsorbante.getNumero();
				ids.idAbsorbee1 = absorbee1.getNumero();
				ids.idEtablissementPrincipalAbsorbee1 = etablissementPrincipalAbsorbee1.getNumero();
				ids.idAbsorbee2 = absorbee2.getNumero();
				ids.idEtablissementPrincipalAbsorbee2 = etablissementPrincipalAbsorbee2.getNumero();
				return ids;
			}
		});

		// lancement de la fusion d'entreprises
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Entreprise absorbante = (Entreprise) tiersDAO.get(ids.idAbsorbante);
				final Entreprise absorbee1 = (Entreprise) tiersDAO.get(ids.idAbsorbee1);
				final Entreprise absorbee2 = (Entreprise) tiersDAO.get(ids.idAbsorbee2);
				Assert.assertFalse(absorbante.getBlocageRemboursementAutomatique());
				Assert.assertFalse(absorbee1.getBlocageRemboursementAutomatique());
				Assert.assertFalse(absorbee2.getBlocageRemboursementAutomatique());
				metierServicePM.fusionne(absorbante, Arrays.asList(absorbee1, absorbee2), dateContratFusion, dateBilanFusion);
			}
		});

		// vérification des résultats en base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {

				// 1. sur l'entreprise absorbante
				// 1.1. rien n'a changé sauf l'apparition de nouveaux rapports entre tiers
				final Entreprise absorbante = (Entreprise) tiersDAO.get(ids.idAbsorbante);
				Assert.assertNotNull(absorbante);
				Assert.assertFalse(absorbante.getBlocageRemboursementAutomatique());
			    Assert.assertEquals(2, absorbante.getForsFiscaux().size());
				final ForFiscalPrincipalPM ffp = absorbante.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertNull(ffp.getDateFin());
				Assert.assertEquals(dateDebutAbsorbante, ffp.getDateDebut());
				Assert.assertFalse(ffp.isAnnule());

				final List<FusionEntreprises> fusions = extractRapports(absorbante.getRapportsObjet(), FusionEntreprises.class, new Comparator<FusionEntreprises>() {
					@Override
					public int compare(FusionEntreprises o1, FusionEntreprises o2) {
						// ordre croissant des identifiants des entreprises absorbées
						return Long.compare(o1.getSujetId(), o2.getSujetId());
					}
				});
				Assert.assertNotNull(fusions);
				Assert.assertEquals(2, fusions.size());
				{
					final FusionEntreprises fusion = fusions.get(0);
					Assert.assertNotNull(fusion);
					Assert.assertFalse(fusion.isAnnule());
					Assert.assertEquals(dateBilanFusion, fusion.getDateDebut());
					Assert.assertNull(fusion.getDateFin());
					Assert.assertEquals((Long) ids.idAbsorbee1, fusion.getSujetId());
				}
				{
					final FusionEntreprises fusion = fusions.get(1);
					Assert.assertNotNull(fusion);
					Assert.assertFalse(fusion.isAnnule());
					Assert.assertEquals(dateBilanFusion, fusion.getDateDebut());
					Assert.assertNull(fusion.getDateFin());
					Assert.assertEquals((Long) ids.idAbsorbee2, fusion.getSujetId());
				}

				// 1.2  événement fiscal de fusion
				final Collection<EvenementFiscal> evtsFiscauxAbsorbante = evenementFiscalDAO.getEvenementsFiscaux(absorbante);
				Assert.assertNotNull(evtsFiscauxAbsorbante);
				Assert.assertEquals(1, evtsFiscauxAbsorbante.size());
				final EvenementFiscal evtFiscalAbsorbante = evtsFiscauxAbsorbante.iterator().next();
				Assert.assertNotNull(evtFiscalAbsorbante);
				Assert.assertFalse(evtFiscalAbsorbante.isAnnule());
				Assert.assertEquals(dateBilanFusion, evtFiscalAbsorbante.getDateValeur());
				Assert.assertEquals(EvenementFiscalInformationComplementaire.class, evtFiscalAbsorbante.getClass());
				Assert.assertEquals(EvenementFiscalInformationComplementaire.TypeInformationComplementaire.FUSION, ((EvenementFiscalInformationComplementaire) evtFiscalAbsorbante).getType());

				// 1.3 état inchangé
				final EtatEntreprise etatAbsorbante = absorbante.getEtatActuel();
				Assert.assertNotNull(etatAbsorbante);
				Assert.assertFalse(etatAbsorbante.isAnnule());
				Assert.assertEquals(TypeEtatEntreprise.FONDEE, etatAbsorbante.getType());
				Assert.assertEquals(dateDebutAbsorbante, etatAbsorbante.getDateObtention());

				// 1.4 adresse inchangée
				final List<AdresseTiers> adressesAbsorbante = absorbante.getAdressesTiersSorted(TypeAdresseTiers.COURRIER);
				Assert.assertNotNull(adressesAbsorbante);
				Assert.assertEquals(1, adressesAbsorbante.size());
				final AdresseTiers adresseAbsorbante = adressesAbsorbante.get(0);
				Assert.assertNotNull(adresseAbsorbante);
				Assert.assertFalse(adresseAbsorbante.isAnnule());
				Assert.assertEquals(AdresseSuisse.class, adresseAbsorbante.getClass());
				final AdresseSuisse adresseSuisseAbsorbante = (AdresseSuisse) adresseAbsorbante;
				Assert.assertEquals(MockRue.Prilly.RueDesMetiers.getNoRue(), adresseSuisseAbsorbante.getNumeroRue());
				Assert.assertEquals(dateDebutAbsorbante, adresseAbsorbante.getDateDebut());
				Assert.assertNull(adresseAbsorbante.getDateFin());

				// 2. sur les entreprises absorbées
				final Entreprise absorbee1 = (Entreprise) tiersDAO.get(ids.idAbsorbee1);
				final Entreprise absorbee2 = (Entreprise) tiersDAO.get(ids.idAbsorbee2);
				for (Entreprise absorbee : Arrays.asList(absorbee1, absorbee2)) {

					Assert.assertTrue(absorbee.getBlocageRemboursementAutomatique());

					// 2.1 les rapports entre tiers "FusionEntreprises" ont été testés plus haut

					// 2.2 fors fiscaux terminés à la date de bilan de fusion avec un motif "FUSION_ENTREPRISES"
					final Set<ForFiscal> forsFiscaux = absorbee.getForsFiscaux();
					Assert.assertNotNull(forsFiscaux);
					Assert.assertEquals(1, forsFiscaux.size());
					final ForFiscal ff = forsFiscaux.iterator().next();
					Assert.assertNotNull(ff);
					Assert.assertFalse(ff.isAnnule());
					Assert.assertEquals(dateBilanFusion, ff.getDateFin());
					Assert.assertEquals(ForFiscalPrincipalPM.class, ff.getClass());
					Assert.assertEquals(MotifFor.FUSION_ENTREPRISES, ((ForFiscalPrincipalPM) ff).getMotifFermeture());

					// 2.3 état fiscal "ABSORBEE" à la date de contrat de fusion
					final EtatEntreprise etat = absorbee.getEtatActuel();
					Assert.assertNotNull(etat);
					Assert.assertFalse(etat.isAnnule());
					Assert.assertEquals(TypeEtatEntreprise.ABSORBEE, etat.getType());
					Assert.assertEquals(dateContratFusion, etat.getDateObtention());

					// 2.4 surcharge d'adresse courrier
					final List<AdresseTiers> adresses = absorbee.getAdressesTiersSorted();
					Assert.assertNotNull(adresses);
					Assert.assertEquals(1, adresses.size());
					final AdresseTiers adresse = adresses.get(0);
					Assert.assertNotNull(adresse);
					Assert.assertFalse(adresse.isAnnule());
					Assert.assertEquals(dateContratFusion, adresse.getDateDebut());
					Assert.assertNull(adresse.getDateFin());
					Assert.assertEquals(TypeAdresseTiers.COURRIER, adresse.getUsage());
					Assert.assertEquals(AdresseSuisse.class, adresse.getClass());
					final AdresseSuisse adresseSuisse = (AdresseSuisse) adresse;
					Assert.assertEquals(MockRue.Prilly.RueDesMetiers.getNoRue(), adresseSuisse.getNumeroRue());
					Assert.assertEquals("p.a. Ma grande entreprise", adresseSuisse.getComplement());

					// 2.5 événements fiscaux
					final Collection<EvenementFiscal> evtsFiscaux = evenementFiscalDAO.getEvenementsFiscaux(absorbee);
					Assert.assertNotNull(evtsFiscaux);
					Assert.assertEquals(2, evtsFiscaux.size());     // 1 fermeture de for + 1 information complémentaire
					final Map<Class<? extends EvenementFiscal>, List<EvenementFiscal>> evtsParClass = segmenterParClasse(evtsFiscaux);
					Assert.assertEquals(new HashSet<>(Arrays.asList(EvenementFiscalFor.class, EvenementFiscalInformationComplementaire.class)), evtsParClass.keySet());
					Assert.assertEquals(1, evtsParClass.get(EvenementFiscalFor.class).size());
					Assert.assertEquals(1, evtsParClass.get(EvenementFiscalInformationComplementaire.class).size());
					{
						final EvenementFiscal evtFiscal = evtsParClass.get(EvenementFiscalInformationComplementaire.class).get(0);
						Assert.assertNotNull(evtFiscal);
						Assert.assertFalse(evtFiscal.isAnnule());
						Assert.assertEquals(dateBilanFusion, evtFiscal.getDateValeur());
						Assert.assertEquals(EvenementFiscalInformationComplementaire.TypeInformationComplementaire.FUSION, ((EvenementFiscalInformationComplementaire) evtFiscal).getType());
					}
					{
						final EvenementFiscal evtFiscal = evtsParClass.get(EvenementFiscalFor.class).get(0);
						Assert.assertNotNull(evtFiscal);
						Assert.assertFalse(evtFiscal.isAnnule());
						Assert.assertEquals(dateBilanFusion, evtFiscal.getDateValeur());
						Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.FERMETURE, ((EvenementFiscalFor) evtFiscal).getType());
					}
				}
			}
		});
	}

	@Test
	public void testAnnulationFusionEntreprises() throws Exception {

		// nous allons créer un cas un peu plus compliqué où une date de bilan de fusion a été identique dans deux contrats de fusion
		// distincts... il n'est pas question alors d'annuler les deux fusions d'un coup !

		final RegDate dateDebutAbsorbante = date(2000, 5, 12);
		final RegDate dateDebutAbsorbee1 = date(2005, 6, 13);
		final RegDate dateDebutAbsorbee2 = date(2007, 9, 30);

		final RegDate dateContratFusion1 = date(2016, 2, 2);
		final RegDate dateContratFusion2 = date(2016, 4, 2);
		final RegDate dateBilanFusion = date(2015, 12, 31);

		final class Ids {
			long idAbsorbante;
			long idEtablissementPrincipalAbsorbante;
			long idAbsorbee1;
			long idEtablissementPrincipalAbsorbee1;
			long idAbsorbee2;
			long idEtablissementPrincipalAbsorbee2;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final Entreprise absorbante = addEntrepriseInconnueAuCivil();
				addRaisonSociale(absorbante, dateDebutAbsorbante, null, "Ma grande entreprise");
				addFormeJuridique(absorbante, dateDebutAbsorbante, null, FormeJuridiqueEntreprise.SA);
				addRegimeFiscalCH(absorbante, dateDebutAbsorbante, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalVD(absorbante, dateDebutAbsorbante, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(absorbante, dateDebutAbsorbante, DayMonth.get(12, 31), 12);        // tous les 31.12 depuis 2000
				addForPrincipal(absorbante, dateDebutAbsorbante, MotifFor.DEBUT_EXPLOITATION, MockCommune.Grandson);
				addForSecondaire(absorbante, dateDebutAbsorbante, MotifFor.DEBUT_EXPLOITATION, MockCommune.ChateauDoex, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
				absorbante.setBlocageRemboursementAutomatique(Boolean.FALSE);

				addAdresseMandataireSuisse(absorbante, dateDebutAbsorbante, null, TypeMandat.GENERAL, "Mon mandataire chéri", MockRue.Renens.QuatorzeAvril);
				addAdresseSuisse(absorbante, TypeAdresseTiers.COURRIER, dateDebutAbsorbante, null, MockRue.Prilly.RueDesMetiers);

				final Etablissement etablissementPrincipalAbsorbante = addEtablissement();
				addDomicileEtablissement(etablissementPrincipalAbsorbante, dateDebutAbsorbante, null, MockCommune.Grandson);
				addActiviteEconomique(absorbante, etablissementPrincipalAbsorbante, dateDebutAbsorbante, null, true);

				addEtatEntreprise(absorbante, dateDebutAbsorbante, TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.AUTOMATIQUE);


				final Entreprise absorbee1 = addEntrepriseInconnueAuCivil();
				addRaisonSociale(absorbee1, dateDebutAbsorbee1, null, "Ma toute petite entreprise");
				addFormeJuridique(absorbee1, dateDebutAbsorbee1, null, FormeJuridiqueEntreprise.SA);
				addRegimeFiscalCH(absorbee1, dateDebutAbsorbee1, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalVD(absorbee1, dateDebutAbsorbee1, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(absorbee1, dateDebutAbsorbee1, DayMonth.get(12, 31), 12);        // tous les 31.12 depuis 2005
				addForPrincipal(absorbee1, dateDebutAbsorbee1, MotifFor.DEBUT_EXPLOITATION, dateBilanFusion, MotifFor.FUSION_ENTREPRISES, MockCommune.Lausanne);
				absorbee1.setBlocageRemboursementAutomatique(Boolean.TRUE);

				final AdresseSuisse adresseSuisseAbsorbee1 = addAdresseSuisse(absorbee1, TypeAdresseTiers.COURRIER, dateContratFusion1, null, MockRue.Prilly.RueDesMetiers);
				adresseSuisseAbsorbee1.setComplement("p.a. Ma grande entreprise");

				final Etablissement etablissementPrincipalAbsorbee1 = addEtablissement();
				addDomicileEtablissement(etablissementPrincipalAbsorbee1, dateDebutAbsorbee1, null, MockCommune.Lausanne);
				addActiviteEconomique(absorbee1, etablissementPrincipalAbsorbee1, dateDebutAbsorbee1, null, true);

				addEtatEntreprise(absorbee1, dateDebutAbsorbee1, TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.AUTOMATIQUE);
				addEtatEntreprise(absorbee1, dateContratFusion1, TypeEtatEntreprise.ABSORBEE, TypeGenerationEtatEntreprise.MANUELLE);


				final Entreprise absorbee2 = addEntrepriseInconnueAuCivil();
				addRaisonSociale(absorbee2, dateDebutAbsorbee2, null, "Ma minuscule entreprise");
				addFormeJuridique(absorbee2, dateDebutAbsorbee2, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalCH(absorbee2, dateDebutAbsorbee2, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalVD(absorbee2, dateDebutAbsorbee2, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(absorbee2, dateDebutAbsorbee2, DayMonth.get(12, 31), 12);        // tous les 31.12 depuis 2007
				addForPrincipal(absorbee2, dateDebutAbsorbee2, MotifFor.DEBUT_EXPLOITATION, dateBilanFusion, MotifFor.FUSION_ENTREPRISES, MockCommune.Prilly);
				absorbee2.setBlocageRemboursementAutomatique(Boolean.TRUE);

				final AdresseSuisse adresseSuisseAbsorbee2 = addAdresseSuisse(absorbee2, TypeAdresseTiers.COURRIER, dateContratFusion2, null, MockRue.Prilly.RueDesMetiers);
				adresseSuisseAbsorbee2.setComplement("p.a. Ma grande entreprise");

				final Etablissement etablissementPrincipalAbsorbee2 = addEtablissement();
				addDomicileEtablissement(etablissementPrincipalAbsorbee2, dateDebutAbsorbee2, null, MockCommune.Prilly);
				addActiviteEconomique(absorbee2, etablissementPrincipalAbsorbee2, dateDebutAbsorbee2, null, true);

				addEtatEntreprise(absorbee2, dateDebutAbsorbee2, TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.AUTOMATIQUE);
				addEtatEntreprise(absorbee2, dateContratFusion2, TypeEtatEntreprise.ABSORBEE, TypeGenerationEtatEntreprise.MANUELLE);

				addFusionEntreprises(absorbante, absorbee1, dateBilanFusion);
				addFusionEntreprises(absorbante, absorbee2, dateBilanFusion);


				final Ids ids = new Ids();
				ids.idAbsorbante = absorbante.getNumero();
				ids.idEtablissementPrincipalAbsorbante = etablissementPrincipalAbsorbante.getNumero();
				ids.idAbsorbee1 = absorbee1.getNumero();
				ids.idEtablissementPrincipalAbsorbee1 = etablissementPrincipalAbsorbee1.getNumero();
				ids.idAbsorbee2 = absorbee2.getNumero();
				ids.idEtablissementPrincipalAbsorbee2 = etablissementPrincipalAbsorbee2.getNumero();
				return ids;
			}
		});

		// annulation de la fusion (avec erreur)
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise absorbante = (Entreprise) tiersDAO.get(ids.idAbsorbante);
				final Entreprise absorbee1 = (Entreprise) tiersDAO.get(ids.idAbsorbee1);
				final Entreprise absorbee2 = (Entreprise) tiersDAO.get(ids.idAbsorbee2);
				Assert.assertFalse(absorbante.getBlocageRemboursementAutomatique());
				Assert.assertTrue(absorbee1.getBlocageRemboursementAutomatique());
				Assert.assertTrue(absorbee2.getBlocageRemboursementAutomatique());

				// attention, l'entreprise 2 a été mise là par erreur ou malveillance... on doit sauter!
				try {
					metierServicePM.annuleFusionEntreprises(absorbante, Arrays.asList(absorbee1, absorbee2), dateContratFusion1, dateBilanFusion);
					Assert.fail("Aurait-dû sauter car l'entreprise absorbee2 n'a pas la bonne date de contrat de fusion");
				}
				catch (MetierServiceException e) {
					Assert.assertEquals(String.format("L'entreprise %s n'est pas associée à une absorption par l'entreprise %s avec une date de contrat de fusion au %s.",
					                                  FormatNumeroHelper.numeroCTBToDisplay(ids.idAbsorbee2),
					                                  FormatNumeroHelper.numeroCTBToDisplay(ids.idAbsorbante),
					                                  RegDateHelper.dateToDisplayString(dateContratFusion1)),
					                    e.getMessage());

					// ne rien mettre en base quoi qu'il arrive...
					status.setRollbackOnly();
				}
			}
		});

		// annulation de la fusion (sans erreur cette fois)
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Entreprise absorbante = (Entreprise) tiersDAO.get(ids.idAbsorbante);
				final Entreprise absorbee1 = (Entreprise) tiersDAO.get(ids.idAbsorbee1);
				Assert.assertFalse(absorbante.getBlocageRemboursementAutomatique());
				Assert.assertTrue(absorbee1.getBlocageRemboursementAutomatique());
				metierServicePM.annuleFusionEntreprises(absorbante, Collections.singletonList(absorbee1), dateContratFusion1, dateBilanFusion);
			}
		});

		// vérification des données en base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {

				// 1. sur l'entreprise absorbante
				// 1.1. rien n'a changé sauf l'annulation du rapport entre tiers "fusion" existant avec l'entreprise absorbee1
				final Entreprise absorbante = (Entreprise) tiersDAO.get(ids.idAbsorbante);
				Assert.assertNotNull(absorbante);
				Assert.assertFalse(absorbante.getBlocageRemboursementAutomatique());
				Assert.assertFalse(absorbante.isAnnule());
				Assert.assertEquals(2, absorbante.getForsFiscaux().size());
				final ForFiscalPrincipalPM ffp = absorbante.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertNull(ffp.getDateFin());
				Assert.assertEquals(dateDebutAbsorbante, ffp.getDateDebut());
				Assert.assertFalse(ffp.isAnnule());

				final List<FusionEntreprises> fusions = extractRapports(absorbante.getRapportsObjet(), FusionEntreprises.class, new Comparator<FusionEntreprises>() {
					@Override
					public int compare(FusionEntreprises o1, FusionEntreprises o2) {
						// ordre croissant des identifiants des entreprises absorbées
						return Long.compare(o1.getSujetId(), o2.getSujetId());
					}
				});
				Assert.assertNotNull(fusions);
				Assert.assertEquals(2, fusions.size());
				{
					final FusionEntreprises fusion = fusions.get(0);
					Assert.assertNotNull(fusion);
					Assert.assertTrue(fusion.isAnnule());
					Assert.assertEquals(dateBilanFusion, fusion.getDateDebut());
					Assert.assertNull(fusion.getDateFin());
					Assert.assertEquals((Long) ids.idAbsorbee1, fusion.getSujetId());
				}
				{
					final FusionEntreprises fusion = fusions.get(1);
					Assert.assertNotNull(fusion);
					Assert.assertFalse(fusion.isAnnule());
					Assert.assertEquals(dateBilanFusion, fusion.getDateDebut());
					Assert.assertNull(fusion.getDateFin());
					Assert.assertEquals((Long) ids.idAbsorbee2, fusion.getSujetId());
				}

				// 1.3 état inchangé
				final EtatEntreprise etatAbsorbante = absorbante.getEtatActuel();
				Assert.assertNotNull(etatAbsorbante);
				Assert.assertFalse(etatAbsorbante.isAnnule());
				Assert.assertEquals(TypeEtatEntreprise.FONDEE, etatAbsorbante.getType());
				Assert.assertEquals(dateDebutAbsorbante, etatAbsorbante.getDateObtention());

				// 1.4 adresse inchangée
				final List<AdresseTiers> adressesAbsorbante = absorbante.getAdressesTiersSorted(TypeAdresseTiers.COURRIER);
				Assert.assertNotNull(adressesAbsorbante);
				Assert.assertEquals(1, adressesAbsorbante.size());
				final AdresseTiers adresseAbsorbante = adressesAbsorbante.get(0);
				Assert.assertNotNull(adresseAbsorbante);
				Assert.assertFalse(adresseAbsorbante.isAnnule());
				Assert.assertEquals(AdresseSuisse.class, adresseAbsorbante.getClass());
				final AdresseSuisse adresseSuisseAbsorbante = (AdresseSuisse) adresseAbsorbante;
				Assert.assertEquals(MockRue.Prilly.RueDesMetiers.getNoRue(), adresseSuisseAbsorbante.getNumeroRue());
				Assert.assertEquals(dateDebutAbsorbante, adresseAbsorbante.getDateDebut());
				Assert.assertNull(adresseAbsorbante.getDateFin());

				// 2.4 événement fiscal
				final Collection<EvenementFiscal> evtsFiscauxAbsorbante = evenementFiscalDAO.getEvenementsFiscaux(absorbante);
				Assert.assertNotNull(evtsFiscauxAbsorbante);
				Assert.assertEquals(1, evtsFiscauxAbsorbante.size());
				final EvenementFiscal evtFiscalAbsorbante = evtsFiscauxAbsorbante.iterator().next();
				Assert.assertNotNull(evtFiscalAbsorbante);
				Assert.assertEquals(EvenementFiscalInformationComplementaire.class, evtFiscalAbsorbante.getClass());
				final EvenementFiscalInformationComplementaire evtFiscalInfoAbsorbante = (EvenementFiscalInformationComplementaire) evtFiscalAbsorbante;
				Assert.assertEquals(EvenementFiscalInformationComplementaire.TypeInformationComplementaire.ANNULATION_FUSION, evtFiscalInfoAbsorbante.getType());
				Assert.assertEquals(dateBilanFusion, evtFiscalInfoAbsorbante.getDateValeur());

				// 2. sur l'entreprise absorbée1 dont la fusion est effectivement annulée

				final Entreprise absorbee1 = (Entreprise) tiersDAO.get(ids.idAbsorbee1);
				Assert.assertNotNull(absorbee1);
				Assert.assertFalse(absorbee1.isAnnule());
				Assert.assertFalse(absorbee1.getBlocageRemboursementAutomatique());

				// 2.1 for ré-ouvert
				final ForFiscalPrincipalPM ffp1 = absorbee1.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp1);
				Assert.assertFalse(ffp1.isAnnule());
				Assert.assertEquals(dateDebutAbsorbee1, ffp1.getDateDebut());
				Assert.assertNull(ffp1.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp1.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), ffp1.getNumeroOfsAutoriteFiscale());

				// 2.2 état absorbé annulé
				final Set<EtatEntreprise> etats1 = absorbee1.getEtats();
				Assert.assertNotNull(etats1);
				boolean etatAnnuleTrouve = false;
				for (EtatEntreprise etat : etats1) {
					if (etat.getType() == TypeEtatEntreprise.ABSORBEE && etat.getDateObtention() == dateContratFusion1) {
						Assert.assertFalse(etatAnnuleTrouve);
						Assert.assertTrue(etat.isAnnule());
						etatAnnuleTrouve = true;
					}
					else {
						// aucun autre état ne doit avoir été annulé
						Assert.assertFalse(etat.isAnnule());
					}
				}
				Assert.assertTrue(etatAnnuleTrouve);

				// 2.3 surcharge d'adresse annulée
				final List<AdresseTiers> adresses1 = absorbee1.getAdressesTiersSorted();
				Assert.assertNotNull(adresses1);
				Assert.assertEquals(1, adresses1.size());
				final AdresseTiers adresse = adresses1.get(0);
				Assert.assertNotNull(adresse);
				Assert.assertTrue(adresse.isAnnule());
				Assert.assertEquals(dateContratFusion1, adresse.getDateDebut());
				Assert.assertNull(adresse.getDateFin());
				Assert.assertEquals(TypeAdresseTiers.COURRIER, adresse.getUsage());
				Assert.assertEquals(AdresseSuisse.class, adresse.getClass());
				final AdresseSuisse adresseSuisse = (AdresseSuisse) adresse;
				Assert.assertEquals(MockRue.Prilly.RueDesMetiers.getNoRue(), adresseSuisse.getNumeroRue());
				Assert.assertEquals("p.a. Ma grande entreprise", adresseSuisse.getComplement());

				// 2.4 événement fiscal
				final Collection<EvenementFiscal> evtsFiscauxAbsorbee1 = evenementFiscalDAO.getEvenementsFiscaux(absorbee1);
				Assert.assertNotNull(evtsFiscauxAbsorbee1);
				Assert.assertEquals(3, evtsFiscauxAbsorbee1.size());    // une annulation de for, une ouverture de for, une info complémentaire

				final Map<Class<? extends EvenementFiscal>, List<EvenementFiscal>> evtsFiscauxAbsorbee1ParClasse = segmenterParClasse(evtsFiscauxAbsorbee1);
				Assert.assertEquals(2, evtsFiscauxAbsorbee1ParClasse.size());
				Assert.assertTrue(evtsFiscauxAbsorbee1ParClasse.containsKey(EvenementFiscalFor.class));
				Assert.assertTrue(evtsFiscauxAbsorbee1ParClasse.containsKey(EvenementFiscalInformationComplementaire.class));

				final List<EvenementFiscal> evtsFiscauxInfoAbsorbee1 = evtsFiscauxAbsorbee1ParClasse.get(EvenementFiscalInformationComplementaire.class);
				Assert.assertEquals(1, evtsFiscauxInfoAbsorbee1.size());
				final EvenementFiscalInformationComplementaire evtFiscalInfoAbsorbee1 = (EvenementFiscalInformationComplementaire) evtsFiscauxInfoAbsorbee1.get(0);
				Assert.assertNotNull(evtFiscalInfoAbsorbee1);
				Assert.assertEquals(EvenementFiscalInformationComplementaire.TypeInformationComplementaire.ANNULATION_FUSION, evtFiscalInfoAbsorbee1.getType());
				Assert.assertEquals(dateBilanFusion, evtFiscalInfoAbsorbee1.getDateValeur());

				// 3. sur l'entreprise absorbee2 dont la fusion n'est finalement pas annulée

				final Entreprise absorbee2 = (Entreprise) tiersDAO.get(ids.idAbsorbee2);
				Assert.assertNotNull(absorbee2);
				Assert.assertFalse(absorbee2.isAnnule());
				Assert.assertTrue(absorbee2.getBlocageRemboursementAutomatique());

				// 3.1. for toujours fermé
				final ForFiscalPrincipalPM ffp2 = absorbee2.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp2);
				Assert.assertFalse(ffp2.isAnnule());
				Assert.assertEquals(dateDebutAbsorbee2, ffp2.getDateDebut());
				Assert.assertEquals(dateBilanFusion, ffp2.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp2.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Prilly.getNoOFS(), ffp2.getNumeroOfsAutoriteFiscale());

				// 3.2. état fiscal toujours absorbé
				final EtatEntreprise etat2 = absorbee2.getEtatActuel();
				Assert.assertNotNull(etat2);
				Assert.assertFalse(etat2.isAnnule());
				Assert.assertEquals(TypeEtatEntreprise.ABSORBEE, etat2.getType());
				Assert.assertEquals(dateContratFusion2, etat2.getDateObtention());

				// 3.3 pas d'événement fiscal
				final Collection<EvenementFiscal> evtsFiscauxAbsorbee2 = evenementFiscalDAO.getEvenementsFiscaux(absorbee2);
				Assert.assertNotNull(evtsFiscauxAbsorbee2);
				Assert.assertEquals(0, evtsFiscauxAbsorbee2.size());
			}
		});
	}

	@Test
	public void testFinActivite() throws Exception {

		final RegDate dateCreationEntreprise = date(2000, 4, 1);
		final RegDate dateCessationActivite = date(2010, 4, 13);

		final class Ids {
			long idEntreprise;
			long idEtablissementPrincipal;
			long idEtablissementSecondaire;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateCreationEntreprise, null, "Ma petite entreprise");
				addFormeJuridique(entreprise, dateCreationEntreprise, null, FormeJuridiqueEntreprise.ASSOCIATION);
				addRegimeFiscalCH(entreprise, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
				addRegimeFiscalVD(entreprise, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
				addBouclement(entreprise, dateCreationEntreprise, DayMonth.get(12, 31), 12);        // tous les 31.12 depuis 2000
				addForPrincipal(entreprise, dateCreationEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.Grandson);
				addForSecondaire(entreprise, dateCreationEntreprise, MotifFor.DEBUT_EXPLOITATION, MockCommune.ChateauDoex, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
				entreprise.setBlocageRemboursementAutomatique(Boolean.FALSE);

				addAdresseMandataireSuisse(entreprise, dateCreationEntreprise, null, TypeMandat.GENERAL, "Mon mandataire chéri", MockRue.Renens.QuatorzeAvril);

				final Etablissement etablissementPrincipal = addEtablissement();
				addDomicileEtablissement(etablissementPrincipal, dateCreationEntreprise, null, MockCommune.Grandson);
				addActiviteEconomique(entreprise, etablissementPrincipal, dateCreationEntreprise, null, true);

				final Etablissement etablissementSecondaire = addEtablissement();
				addDomicileEtablissement(etablissementSecondaire, dateCreationEntreprise, null, MockCommune.ChateauDoex);
				addActiviteEconomique(entreprise, etablissementSecondaire, dateCreationEntreprise, null, false);

				addEtatEntreprise(entreprise, dateCreationEntreprise, TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.AUTOMATIQUE);

				final Ids ids = new Ids();
				ids.idEntreprise = entreprise.getNumero();
				ids.idEtablissementPrincipal = etablissementPrincipal.getNumero();
				ids.idEtablissementSecondaire = etablissementSecondaire.getNumero();
				return ids;
			}
		});

		// traitement de la faillite
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
				Assert.assertNotNull(entreprise);
				Assert.assertFalse(entreprise.getBlocageRemboursementAutomatique());
				metierServicePM.finActivite(entreprise, dateCessationActivite, "Une jolie remarque toute belle...");
			}
		});

		// vérification des résultats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
				Assert.assertNotNull(entreprise);
				Assert.assertTrue(entreprise.getBlocageRemboursementAutomatique());

				// 1. les fors doivent être fermés pour motif FAILLITE
				final Set<ForFiscal> forsFiscaux = entreprise.getForsFiscaux();
				Assert.assertNotNull(forsFiscaux);
				Assert.assertEquals(2, forsFiscaux.size());
				for (ForFiscal ff : forsFiscaux) {
					Assert.assertFalse(ff.isAnnule());
					Assert.assertEquals(dateCreationEntreprise, ff.getDateDebut());
					Assert.assertEquals(dateCessationActivite, ff.getDateFin());
					Assert.assertTrue(ff.getClass().getName(), ff instanceof ForFiscalAvecMotifs);
					Assert.assertEquals(MotifFor.FIN_EXPLOITATION, ((ForFiscalAvecMotifs) ff).getMotifFermeture());
				}

				// 2. le rapport entre tiers vers l'établissement secondaire doit être fermé, pas l'autre
				boolean principalTrouve = false;
				boolean secondaireTrouve = false;
				final Set<RapportEntreTiers> rapportsSujet = entreprise.getRapportsSujet();
				Assert.assertNotNull(rapportsSujet);
				Assert.assertEquals(2, rapportsSujet.size());
				for (RapportEntreTiers ret : rapportsSujet) {
					Assert.assertFalse(ret.isAnnule());
					Assert.assertEquals(dateCreationEntreprise, ret.getDateDebut());
					Assert.assertEquals(TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE, ret.getType());

					final ActiviteEconomique ae = (ActiviteEconomique) ret;
					if (ae.isPrincipal()) {
						Assert.assertFalse(principalTrouve);
						Assert.assertNull(ae.getDateFin());
						Assert.assertEquals((Long) ids.idEtablissementPrincipal, ae.getObjetId());
						principalTrouve = true;
					}
					else {
						Assert.assertFalse(secondaireTrouve);
						Assert.assertEquals(dateCessationActivite, ae.getDateFin());
						Assert.assertEquals((Long) ids.idEtablissementSecondaire, ae.getObjetId());
						secondaireTrouve = true;
					}
				}
				Assert.assertTrue(principalTrouve);
				Assert.assertTrue(secondaireTrouve);

				// 3. les adresses mandataires doivent être fermées
				final Set<AdresseMandataire> adressesMandataires = entreprise.getAdressesMandataires();
				Assert.assertNotNull(adressesMandataires);
				Assert.assertEquals(1, adressesMandataires.size());
				for (AdresseMandataire adresse : adressesMandataires) {
					Assert.assertFalse(adresse.isAnnule());
					Assert.assertEquals(dateCreationEntreprise, adresse.getDateDebut());
					Assert.assertEquals(dateCessationActivite, adresse.getDateFin());
				}

				// 4. état DISSOUTE sur l'entreprise
				final EtatEntreprise etatActuel = entreprise.getEtatActuel();
				Assert.assertNotNull(etatActuel);
				Assert.assertEquals(TypeEtatEntreprise.DISSOUTE, etatActuel.getType());
				Assert.assertEquals(dateCessationActivite, etatActuel.getDateObtention());
				Assert.assertEquals(TypeGenerationEtatEntreprise.MANUELLE, etatActuel.getGeneration());

				// 5. et la remarque ?
				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				final Remarque remarque = remarques.iterator().next();
				Assert.assertNotNull(remarque);
				Assert.assertFalse(remarque.isAnnule());
				Assert.assertEquals("Une jolie remarque toute belle...", remarque.getTexte());

				// 6. événements fiscaux
				final Collection<EvenementFiscal> evts = evenementFiscalDAO.getEvenementsFiscaux(entreprise);
				Assert.assertEquals(2, evts.size());        // 2 fermetures de for
				for (EvenementFiscal ef : evts) {
					Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
					final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					Assert.assertNotNull(eff);
					Assert.assertFalse(eff.isAnnule());
					Assert.assertEquals(dateCessationActivite, eff.getDateValeur());
					Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.FERMETURE, eff.getType());
				}
			}
		});
	}

	@Test
	public void testAnnulationFinActivite() throws Exception {

		final RegDate dateCreationEntreprise = date(2000, 4, 1);
		final RegDate dateFinActivite = date(2010, 4, 13);

		final class Ids {
			long idEntreprise;
			long idEtablissementPrincipal;
			long idEtablissementSecondaire;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateCreationEntreprise, null, "Ma petite entreprise");
				addFormeJuridique(entreprise, dateCreationEntreprise, null, FormeJuridiqueEntreprise.ASSOCIATION);
				addRegimeFiscalCH(entreprise, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
				addRegimeFiscalVD(entreprise, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
				addBouclement(entreprise, dateCreationEntreprise, DayMonth.get(12, 31), 12);        // tous les 31.12 depuis 2000
				addForPrincipal(entreprise, dateCreationEntreprise, MotifFor.DEBUT_EXPLOITATION, dateFinActivite, MotifFor.FIN_EXPLOITATION, MockCommune.Grandson);
				addForSecondaire(entreprise, dateCreationEntreprise, MotifFor.DEBUT_EXPLOITATION, dateFinActivite, MotifFor.FIN_EXPLOITATION, MockCommune.ChateauDoex, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
				entreprise.setBlocageRemboursementAutomatique(Boolean.TRUE);

				addAdresseMandataireSuisse(entreprise, dateCreationEntreprise, dateFinActivite, TypeMandat.GENERAL, "Mon mandataire chéri", MockRue.Renens.QuatorzeAvril);

				final Etablissement etablissementPrincipal = addEtablissement();
				addDomicileEtablissement(etablissementPrincipal, dateCreationEntreprise, null, MockCommune.Grandson);
				addActiviteEconomique(entreprise, etablissementPrincipal, dateCreationEntreprise, null, true);

				final Etablissement etablissementSecondaire = addEtablissement();
				addDomicileEtablissement(etablissementSecondaire, dateCreationEntreprise, null, MockCommune.ChateauDoex);
				addActiviteEconomique(entreprise, etablissementSecondaire, dateCreationEntreprise, dateFinActivite, false);

				addEtatEntreprise(entreprise, dateCreationEntreprise, TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.AUTOMATIQUE);
				addEtatEntreprise(entreprise, dateFinActivite, TypeEtatEntreprise.DISSOUTE, TypeGenerationEtatEntreprise.MANUELLE);

				final Ids ids = new Ids();
				ids.idEntreprise = entreprise.getNumero();
				ids.idEtablissementPrincipal = etablissementPrincipal.getNumero();
				ids.idEtablissementSecondaire = etablissementSecondaire.getNumero();
				return ids;
			}
		});

		// traitement de l'annulation de la fin d'activité
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
				Assert.assertNotNull(entreprise);
				Assert.assertTrue(entreprise.getBlocageRemboursementAutomatique());
				metierServicePM.annuleFinActivite(entreprise, dateFinActivite, "Une jolie remarque toute belle pour l'annulation...", true);
			}
		});

		// vérification des résultats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
				Assert.assertNotNull(entreprise);
				Assert.assertFalse(entreprise.getBlocageRemboursementAutomatique());

				// 1. les fors doivent être ré-ouverts
				final Set<ForFiscal> forsFiscaux = entreprise.getForsFiscaux();
				Assert.assertNotNull(forsFiscaux);
				Assert.assertEquals(4, forsFiscaux.size());     // deux annulés, et deux ré-ouverts
				final Map<Boolean, List<ForFiscal>> fors = segmenterAnnulables(forsFiscaux);
				Assert.assertEquals(2, fors.get(Boolean.TRUE).size());
				Assert.assertEquals(2, fors.get(Boolean.FALSE).size());
				for (ForFiscal ff : fors.get(Boolean.TRUE)) {
					Assert.assertEquals(dateCreationEntreprise, ff.getDateDebut());
					Assert.assertEquals(dateFinActivite, ff.getDateFin());
					Assert.assertTrue(ff.getClass().getName(), ff instanceof ForFiscalAvecMotifs);
					Assert.assertEquals(MotifFor.FIN_EXPLOITATION, ((ForFiscalAvecMotifs) ff).getMotifFermeture());
				}
				for (ForFiscal ff : fors.get(Boolean.FALSE)) {
					Assert.assertEquals(dateCreationEntreprise, ff.getDateDebut());
					Assert.assertNull(ff.getDateFin());
					Assert.assertTrue(ff.getClass().getName(), ff instanceof ForFiscalAvecMotifs);
					Assert.assertNull(((ForFiscalAvecMotifs) ff).getMotifFermeture());
				}

				// 2. le rapport entre tiers vers l'établissement secondaire doit être ré-ouvert
				final Set<RapportEntreTiers> rapportsSujet = entreprise.getRapportsSujet();
				Assert.assertNotNull(rapportsSujet);
				Assert.assertEquals(3, rapportsSujet.size());           // 1 annulé, 2 ouverts
				final Map<Boolean, List<RapportEntreTiers>> rets = segmenterAnnulables(rapportsSujet);
				Assert.assertEquals(1, rets.get(Boolean.TRUE).size());
				Assert.assertEquals(2, rets.get(Boolean.FALSE).size());
				for (RapportEntreTiers ret : rets.get(Boolean.FALSE)) {
					Assert.assertEquals(dateCreationEntreprise, ret.getDateDebut());
					Assert.assertNull(ret.getDateFin());
					Assert.assertEquals(TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE, ret.getType());
				}
				for (RapportEntreTiers ret : rets.get(Boolean.TRUE)) {
					Assert.assertEquals(dateCreationEntreprise, ret.getDateDebut());
					Assert.assertEquals(dateFinActivite, ret.getDateFin());
					Assert.assertEquals(TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE, ret.getType());
					Assert.assertFalse(((ActiviteEconomique) ret).isPrincipal());
				}

				// 3. les adresses mandataires doivent être ré-ouvertes
				final Set<AdresseMandataire> adressesMandataires = entreprise.getAdressesMandataires();
				Assert.assertNotNull(adressesMandataires);
				Assert.assertEquals(2, adressesMandataires.size());     // une annulée et une ouverte
				final Map<Boolean, List<AdresseMandataire>> ams = segmenterAnnulables(adressesMandataires);
				Assert.assertEquals(1, ams.get(Boolean.TRUE).size());
				Assert.assertEquals(1, ams.get(Boolean.FALSE).size());
				for (AdresseMandataire adresse : ams.get(Boolean.TRUE)) {
					Assert.assertEquals(dateCreationEntreprise, adresse.getDateDebut());
					Assert.assertEquals(dateFinActivite, adresse.getDateFin());
				}
				for (AdresseMandataire adresse : ams.get(Boolean.FALSE)) {
					Assert.assertEquals(dateCreationEntreprise, adresse.getDateDebut());
					Assert.assertNull(adresse.getDateFin());
				}

				// 4. état DISSOUTE sur l'entreprise (annulé)
				final EtatEntreprise etatActuel = entreprise.getEtatActuel();
				Assert.assertNotNull(etatActuel);
				Assert.assertEquals(TypeEtatEntreprise.FONDEE, etatActuel.getType());
				Assert.assertEquals(dateCreationEntreprise, etatActuel.getDateObtention());
				Assert.assertEquals(TypeGenerationEtatEntreprise.AUTOMATIQUE, etatActuel.getGeneration());

				// 5. et la remarque ?
				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				final Remarque remarque = remarques.iterator().next();
				Assert.assertNotNull(remarque);
				Assert.assertFalse(remarque.isAnnule());
				Assert.assertEquals("Une jolie remarque toute belle pour l'annulation...", remarque.getTexte());

				// 6. événements fiscaux
				final Collection<EvenementFiscal> evts = evenementFiscalDAO.getEvenementsFiscaux(entreprise);
				Assert.assertEquals(4, evts.size());        // 2 annulations de for + 2 ouvertures de for
				final Map<EvenementFiscalFor.TypeEvenementFiscalFor, List<EvenementFiscal>> evtsParType = segmenter(evts, new Extractor<EvenementFiscal, EvenementFiscalFor.TypeEvenementFiscalFor>() {
					@Override
					public EvenementFiscalFor.TypeEvenementFiscalFor extract(EvenementFiscal source) {
						Assert.assertTrue(source.getClass().getName(), source instanceof EvenementFiscalFor);
						return ((EvenementFiscalFor) source).getType();
					}
				});
				Assert.assertEquals(EnumSet.of(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, EvenementFiscalFor.TypeEvenementFiscalFor.ANNULATION), evtsParType.keySet());
				Assert.assertEquals(2, evtsParType.get(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE).size());
				Assert.assertEquals(2, evtsParType.get(EvenementFiscalFor.TypeEvenementFiscalFor.ANNULATION).size());
				for (EvenementFiscal ef : evtsParType.get(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE)) {
					final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					Assert.assertNotNull(eff);
					Assert.assertFalse(eff.isAnnule());
					Assert.assertEquals(dateCreationEntreprise, eff.getDateValeur());
				}
				for (EvenementFiscal ef : evtsParType.get(EvenementFiscalFor.TypeEvenementFiscalFor.ANNULATION)) {
					final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					Assert.assertNotNull(eff);
					Assert.assertFalse(eff.isAnnule());
					Assert.assertEquals(dateCreationEntreprise, eff.getDateValeur());
				}
			}
		});
	}

	@Test
	public void testReprisePartielleActivite() throws Exception {

		final RegDate dateCreationEntreprise = date(2000, 4, 1);
		final RegDate dateFinActivite = date(2010, 4, 13);

		final class Ids {
			long idEntreprise;
			long idEtablissementPrincipal;
			long idEtablissementSecondaire;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateCreationEntreprise, null, "Ma petite entreprise");
				addFormeJuridique(entreprise, dateCreationEntreprise, null, FormeJuridiqueEntreprise.ASSOCIATION);
				addRegimeFiscalCH(entreprise, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
				addRegimeFiscalVD(entreprise, dateCreationEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
				addBouclement(entreprise, dateCreationEntreprise, DayMonth.get(12, 31), 12);        // tous les 31.12 depuis 2000
				addForPrincipal(entreprise, dateCreationEntreprise, MotifFor.DEBUT_EXPLOITATION, dateFinActivite, MotifFor.FIN_EXPLOITATION, MockCommune.Grandson);
				addForSecondaire(entreprise, dateCreationEntreprise, MotifFor.DEBUT_EXPLOITATION, dateFinActivite, MotifFor.FIN_EXPLOITATION, MockCommune.ChateauDoex, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
				entreprise.setBlocageRemboursementAutomatique(Boolean.TRUE);

				addAdresseMandataireSuisse(entreprise, dateCreationEntreprise, dateFinActivite, TypeMandat.GENERAL, "Mon mandataire chéri", MockRue.Renens.QuatorzeAvril);

				final Etablissement etablissementPrincipal = addEtablissement();
				addDomicileEtablissement(etablissementPrincipal, dateCreationEntreprise, null, MockCommune.Grandson);
				addActiviteEconomique(entreprise, etablissementPrincipal, dateCreationEntreprise, null, true);

				final Etablissement etablissementSecondaire = addEtablissement();
				addDomicileEtablissement(etablissementSecondaire, dateCreationEntreprise, null, MockCommune.ChateauDoex);
				addActiviteEconomique(entreprise, etablissementSecondaire, dateCreationEntreprise, dateFinActivite, false);

				addEtatEntreprise(entreprise, dateCreationEntreprise, TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.AUTOMATIQUE);
				addEtatEntreprise(entreprise, dateFinActivite, TypeEtatEntreprise.DISSOUTE, TypeGenerationEtatEntreprise.MANUELLE);

				final Ids ids = new Ids();
				ids.idEntreprise = entreprise.getNumero();
				ids.idEtablissementPrincipal = etablissementPrincipal.getNumero();
				ids.idEtablissementSecondaire = etablissementSecondaire.getNumero();
				return ids;
			}
		});

		// traitement de la reprise partielle d'activité
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
				Assert.assertNotNull(entreprise);
				Assert.assertTrue(entreprise.getBlocageRemboursementAutomatique());
				metierServicePM.annuleFinActivite(entreprise, dateFinActivite, "Une jolie remarque toute belle pour la reprise partielle...", false);
			}
		});

		// vérification des résultats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
				Assert.assertNotNull(entreprise);
				Assert.assertFalse(entreprise.getBlocageRemboursementAutomatique());

				// 1. le for principal doit être ré-ouvert
				final Set<ForFiscal> forsFiscaux = entreprise.getForsFiscaux();
				Assert.assertNotNull(forsFiscaux);
				Assert.assertEquals(3, forsFiscaux.size());     // 1 annulé, et 1 ré-ouvert (et un laissé fermé, le secondaire)
				final Map<Boolean, List<ForFiscal>> fors = segmenterAnnulables(forsFiscaux);
				Assert.assertEquals(1, fors.get(Boolean.TRUE).size());
				Assert.assertEquals(2, fors.get(Boolean.FALSE).size());
				for (ForFiscal ff : fors.get(Boolean.TRUE)) {
					Assert.assertEquals(dateCreationEntreprise, ff.getDateDebut());
					Assert.assertEquals(dateFinActivite, ff.getDateFin());
					Assert.assertTrue(ff.getClass().getName(), ff instanceof ForFiscalAvecMotifs);
					Assert.assertEquals(MotifFor.FIN_EXPLOITATION, ((ForFiscalAvecMotifs) ff).getMotifFermeture());
					Assert.assertEquals(ForFiscalPrincipalPM.class, ff.getClass());
				}
				final Map<Class<? extends ForFiscal>, List<ForFiscal>> forsNonAnnulesParClasse = segmenterParClasse(fors.get(Boolean.FALSE));
				Assert.assertEquals(new HashSet<>(Arrays.asList(ForFiscalPrincipalPM.class, ForFiscalSecondaire.class)), forsNonAnnulesParClasse.keySet());
				Assert.assertEquals(1, forsNonAnnulesParClasse.get(ForFiscalPrincipalPM.class).size());
				Assert.assertEquals(1, forsNonAnnulesParClasse.get(ForFiscalSecondaire.class).size());
				for (ForFiscal ff : forsNonAnnulesParClasse.get(ForFiscalPrincipalPM.class)) {
					Assert.assertEquals(dateCreationEntreprise, ff.getDateDebut());
					Assert.assertNull(ff.getDateFin());
					Assert.assertTrue(ff.getClass().getName(), ff instanceof ForFiscalAvecMotifs);
					Assert.assertNull(((ForFiscalAvecMotifs) ff).getMotifFermeture());
				}
				for (ForFiscal ff : forsNonAnnulesParClasse.get(ForFiscalSecondaire.class)) {
					Assert.assertEquals(dateCreationEntreprise, ff.getDateDebut());
					Assert.assertEquals(dateFinActivite, ff.getDateFin());
					Assert.assertTrue(ff.getClass().getName(), ff instanceof ForFiscalAvecMotifs);
					Assert.assertEquals(MotifFor.FIN_EXPLOITATION, ((ForFiscalAvecMotifs) ff).getMotifFermeture());
				}

				// 2. le rapport entre tiers vers l'établissement secondaire ne doit pas être ré-ouvert
				final Set<RapportEntreTiers> rapportsSujet = entreprise.getRapportsSujet();
				Assert.assertNotNull(rapportsSujet);
				Assert.assertEquals(2, rapportsSujet.size());           // 1 ouvert, 1 fermé, comme avant la révocation
				final Map<Boolean, List<RapportEntreTiers>> rets = segmenterAnnulables(rapportsSujet);
				Assert.assertEquals(0, rets.get(Boolean.TRUE).size());
				Assert.assertEquals(2, rets.get(Boolean.FALSE).size());
				final Map<Boolean, List<RapportEntreTiers>> activitesEconomiques = segmenter(rets.get(Boolean.FALSE), new Extractor<RapportEntreTiers, Boolean>() {
					@Override
					public Boolean extract(RapportEntreTiers source) {
						Assert.assertTrue(source.getClass().getName(), source instanceof ActiviteEconomique);
						return ((ActiviteEconomique) source).isPrincipal();
					}
				});
				Assert.assertEquals(1, activitesEconomiques.get(Boolean.TRUE).size());
				Assert.assertEquals(1, activitesEconomiques.get(Boolean.FALSE).size());
				for (RapportEntreTiers ret : activitesEconomiques.get(Boolean.FALSE)) {
					Assert.assertEquals(dateCreationEntreprise, ret.getDateDebut());
					Assert.assertEquals(dateFinActivite, ret.getDateFin());
				}
				for (RapportEntreTiers ret : activitesEconomiques.get(Boolean.TRUE)) {
					Assert.assertEquals(dateCreationEntreprise, ret.getDateDebut());
					Assert.assertNull(ret.getDateFin());
				}

				// 3. les adresses mandataires ne doivent pas être ré-ouvertes
				final Set<AdresseMandataire> adressesMandataires = entreprise.getAdressesMandataires();
				Assert.assertNotNull(adressesMandataires);
				Assert.assertEquals(1, adressesMandataires.size());     // l'adresse fermée
				final AdresseMandataire adresseMandataire = adressesMandataires.iterator().next();
				Assert.assertNotNull(adresseMandataire);
				Assert.assertFalse(adresseMandataire.isAnnule());
				Assert.assertEquals(dateCreationEntreprise, adresseMandataire.getDateDebut());
				Assert.assertEquals(dateFinActivite, adresseMandataire.getDateFin());

				// 4. état DISSOUTE sur l'entreprise (annulé)
				final EtatEntreprise etatActuel = entreprise.getEtatActuel();
				Assert.assertNotNull(etatActuel);
				Assert.assertEquals(TypeEtatEntreprise.FONDEE, etatActuel.getType());
				Assert.assertEquals(dateCreationEntreprise, etatActuel.getDateObtention());
				Assert.assertEquals(TypeGenerationEtatEntreprise.AUTOMATIQUE, etatActuel.getGeneration());

				// 5. et la remarque ?
				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				final Remarque remarque = remarques.iterator().next();
				Assert.assertNotNull(remarque);
				Assert.assertFalse(remarque.isAnnule());
				Assert.assertEquals("Une jolie remarque toute belle pour la reprise partielle...", remarque.getTexte());

				// 6. événements fiscaux
				final Collection<EvenementFiscal> evts = evenementFiscalDAO.getEvenementsFiscaux(entreprise);
				Assert.assertEquals(2, evts.size());        // 1 annulation de for + 1 ouverture de for
				boolean annulationTrouvee = false;
				boolean ouvertureTrouvee = false;
				for (EvenementFiscal ef : evts) {
					Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
					final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					Assert.assertNotNull(eff);
					Assert.assertFalse(eff.isAnnule());
					Assert.assertEquals(dateCreationEntreprise, eff.getDateValeur());
					Assert.assertTrue(eff.getForFiscal().isPrincipal());
					if (eff.getType() == EvenementFiscalFor.TypeEvenementFiscalFor.ANNULATION) {
						Assert.assertFalse(annulationTrouvee);
						annulationTrouvee = true;
					}
					else if (eff.getType() == EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE) {
						Assert.assertFalse(ouvertureTrouvee);
						ouvertureTrouvee = true;
					}
					else {
						Assert.fail("Type inattendu : " + eff.getType());
					}
				}
				Assert.assertTrue(annulationTrouvee);
				Assert.assertTrue(ouvertureTrouvee);
			}
		});
	}

	@Test
	public void testScissionEntreprise() throws Exception {

		final RegDate dateDebutScindee = date(2000, 5, 12);
		final RegDate dateDebutResultante1 = date(2005, 6, 13);
		final RegDate dateDebutResultante2 = date(2007, 9, 30);

		final RegDate dateContratScission = date(2016, 4, 2);

		final class Ids {
			long idScindee;
			long idEtablissementPrincipalScindee;
			long idResultante1;
			long idEtablissementPrincipalResultante1;
			long idResultante2;
			long idEtablissementPrincipalResultante2;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final Entreprise scindee = addEntrepriseInconnueAuCivil();
				addRaisonSociale(scindee, dateDebutScindee, null, "Ma grande entreprise");
				addFormeJuridique(scindee, dateDebutScindee, null, FormeJuridiqueEntreprise.SA);
				addRegimeFiscalCH(scindee, dateDebutScindee, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalVD(scindee, dateDebutScindee, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(scindee, dateDebutScindee, DayMonth.get(12, 31), 12);        // tous les 31.12 depuis 2000
				addForPrincipal(scindee, dateDebutScindee, MotifFor.DEBUT_EXPLOITATION, MockCommune.Grandson);
				addForSecondaire(scindee, dateDebutScindee, MotifFor.DEBUT_EXPLOITATION, MockCommune.ChateauDoex, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
				scindee.setBlocageRemboursementAutomatique(Boolean.FALSE);

				addAdresseMandataireSuisse(scindee, dateDebutScindee, null, TypeMandat.GENERAL, "Mon mandataire chéri", MockRue.Renens.QuatorzeAvril);
				addAdresseSuisse(scindee, TypeAdresseTiers.COURRIER, dateDebutScindee, null, MockRue.Prilly.RueDesMetiers);

				final Etablissement etablissementPrincipalScindee = addEtablissement();
				addDomicileEtablissement(etablissementPrincipalScindee, dateDebutScindee, null, MockCommune.Grandson);
				addActiviteEconomique(scindee, etablissementPrincipalScindee, dateDebutScindee, null, true);

				addEtatEntreprise(scindee, dateDebutScindee, TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.AUTOMATIQUE);


				final Entreprise resultante1 = addEntrepriseInconnueAuCivil();
				addRaisonSociale(resultante1, dateDebutResultante1, null, "Ma toute petite entreprise");
				addFormeJuridique(resultante1, dateDebutResultante1, null, FormeJuridiqueEntreprise.SA);
				addRegimeFiscalCH(resultante1, dateDebutResultante1, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalVD(resultante1, dateDebutResultante1, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(resultante1, dateDebutResultante1, DayMonth.get(12, 31), 12);        // tous les 31.12 depuis 2005
				addForPrincipal(resultante1, dateDebutResultante1, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
				resultante1.setBlocageRemboursementAutomatique(Boolean.FALSE);

				final Etablissement etablissementPrincipalResultante1 = addEtablissement();
				addDomicileEtablissement(etablissementPrincipalResultante1, dateDebutResultante1, null, MockCommune.Lausanne);
				addActiviteEconomique(resultante1, etablissementPrincipalResultante1, dateDebutResultante1, null, true);

				addEtatEntreprise(resultante1, dateDebutResultante1, TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.AUTOMATIQUE);


				final Entreprise resultante2 = addEntrepriseInconnueAuCivil();
				addRaisonSociale(resultante2, dateDebutResultante2, null, "Ma minuscule entreprise");
				addFormeJuridique(resultante2, dateDebutResultante2, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalCH(resultante2, dateDebutResultante2, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalVD(resultante2, dateDebutResultante2, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(resultante2, dateDebutResultante2, DayMonth.get(12, 31), 12);        // tous les 31.12 depuis 2007
				addForPrincipal(resultante2, dateDebutResultante2, MotifFor.DEBUT_EXPLOITATION, MockCommune.Prilly);
				resultante2.setBlocageRemboursementAutomatique(Boolean.FALSE);

				final Etablissement etablissementPrincipalResultante2 = addEtablissement();
				addDomicileEtablissement(etablissementPrincipalResultante2, dateDebutResultante2, null, MockCommune.Prilly);
				addActiviteEconomique(resultante2, etablissementPrincipalResultante2, dateDebutResultante2, null, true);

				addEtatEntreprise(resultante2, dateDebutResultante2, TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.AUTOMATIQUE);


				final Ids ids = new Ids();
				ids.idScindee = scindee.getNumero();
				ids.idEtablissementPrincipalScindee = etablissementPrincipalScindee.getNumero();
				ids.idResultante1 = resultante1.getNumero();
				ids.idEtablissementPrincipalResultante1 = etablissementPrincipalResultante1.getNumero();
				ids.idResultante2 = resultante2.getNumero();
				ids.idEtablissementPrincipalResultante2 = etablissementPrincipalResultante2.getNumero();
				return ids;
			}
		});

		// lancement de la scission d'entreprise
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Entreprise scindee = (Entreprise) tiersDAO.get(ids.idScindee);
				final Entreprise resultante1 = (Entreprise) tiersDAO.get(ids.idResultante1);
				final Entreprise resultante2 = (Entreprise) tiersDAO.get(ids.idResultante2);
				Assert.assertFalse(scindee.getBlocageRemboursementAutomatique());
				Assert.assertFalse(resultante1.getBlocageRemboursementAutomatique());
				Assert.assertFalse(resultante2.getBlocageRemboursementAutomatique());
				metierServicePM.scinde(scindee, Arrays.asList(resultante1, resultante2), dateContratScission);
			}
		});

		// vérification des résultats en base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {

				// 1. sur l'entreprise scindée
				// 1.1. rien n'a changé sauf l'apparition de nouveaux rapports entre tiers
				final Entreprise scindee = (Entreprise) tiersDAO.get(ids.idScindee);
				Assert.assertNotNull(scindee);
				Assert.assertFalse(scindee.getBlocageRemboursementAutomatique());
				Assert.assertEquals(2, scindee.getForsFiscaux().size());
				final ForFiscalPrincipalPM ffp = scindee.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertNull(ffp.getDateFin());
				Assert.assertEquals(dateDebutScindee, ffp.getDateDebut());
				Assert.assertFalse(ffp.isAnnule());

				final List<ScissionEntreprise> scissions = extractRapports(scindee.getRapportsSujet(), ScissionEntreprise.class, new Comparator<ScissionEntreprise>() {
					@Override
					public int compare(ScissionEntreprise o1, ScissionEntreprise o2) {
						// ordre croissant des identifiants des entreprises résultantes
						return Long.compare(o1.getObjetId(), o2.getObjetId());
					}
				});
				Assert.assertNotNull(scissions);
				Assert.assertEquals(2, scissions.size());
				{
					final ScissionEntreprise scission = scissions.get(0);
					Assert.assertNotNull(scission);
					Assert.assertFalse(scission.isAnnule());
					Assert.assertEquals(dateContratScission, scission.getDateDebut());
					Assert.assertNull(scission.getDateFin());
					Assert.assertEquals((Long) ids.idResultante1, scission.getObjetId());
				}
				{
					final ScissionEntreprise scission = scissions.get(1);
					Assert.assertNotNull(scission);
					Assert.assertFalse(scission.isAnnule());
					Assert.assertEquals(dateContratScission, scission.getDateDebut());
					Assert.assertNull(scission.getDateFin());
					Assert.assertEquals((Long) ids.idResultante2, scission.getObjetId());
				}

				// 1.2  événement fiscal de scission
				final Collection<EvenementFiscal> evtsFiscauxScindee = evenementFiscalDAO.getEvenementsFiscaux(scindee);
				Assert.assertNotNull(evtsFiscauxScindee);
				Assert.assertEquals(1, evtsFiscauxScindee.size());
				final EvenementFiscal evtFiscalScindee = evtsFiscauxScindee.iterator().next();
				Assert.assertNotNull(evtFiscalScindee);
				Assert.assertFalse(evtFiscalScindee.isAnnule());
				Assert.assertEquals(dateContratScission, evtFiscalScindee.getDateValeur());
				Assert.assertEquals(EvenementFiscalInformationComplementaire.class, evtFiscalScindee.getClass());
				Assert.assertEquals(EvenementFiscalInformationComplementaire.TypeInformationComplementaire.SCISSION, ((EvenementFiscalInformationComplementaire) evtFiscalScindee).getType());

				// 1.3 état inchangé
				final EtatEntreprise etatScindee = scindee.getEtatActuel();
				Assert.assertNotNull(etatScindee);
				Assert.assertFalse(etatScindee.isAnnule());
				Assert.assertEquals(TypeEtatEntreprise.FONDEE, etatScindee.getType());
				Assert.assertEquals(dateDebutScindee, etatScindee.getDateObtention());

				// 1.4 adresse inchangée
				final List<AdresseTiers> adressesScindee = scindee.getAdressesTiersSorted(TypeAdresseTiers.COURRIER);
				Assert.assertNotNull(adressesScindee);
				Assert.assertEquals(1, adressesScindee.size());
				final AdresseTiers adresseScindee = adressesScindee.get(0);
				Assert.assertNotNull(adresseScindee);
				Assert.assertFalse(adresseScindee.isAnnule());
				Assert.assertEquals(AdresseSuisse.class, adresseScindee.getClass());
				final AdresseSuisse adresseSuisseScindee = (AdresseSuisse) adresseScindee;
				Assert.assertEquals(MockRue.Prilly.RueDesMetiers.getNoRue(), adresseSuisseScindee.getNumeroRue());
				Assert.assertEquals(dateDebutScindee, adresseScindee.getDateDebut());
				Assert.assertNull(adresseScindee.getDateFin());

				// 2. sur les entreprises résultantes
				final Entreprise resultante1 = (Entreprise) tiersDAO.get(ids.idResultante1);
				final Entreprise resultante2 = (Entreprise) tiersDAO.get(ids.idResultante2);
				for (Entreprise resultante : Arrays.asList(resultante1, resultante2)) {

					Assert.assertFalse(resultante.getBlocageRemboursementAutomatique());

					// 2.1 les rapports entre tiers "ScissionEntreprise" ont été testés plus haut

					// 2.2 rien sur les fors fiscaux
					final Set<ForFiscal> forsFiscaux = resultante.getForsFiscaux();
					Assert.assertNotNull(forsFiscaux);
					Assert.assertEquals(1, forsFiscaux.size());
					final ForFiscal ff = forsFiscaux.iterator().next();
					Assert.assertNotNull(ff);
					Assert.assertEquals(ForFiscalPrincipalPM.class, ff.getClass());
					Assert.assertFalse(ff.isAnnule());

					// 2.3 pas de nouvel état fiscal
					final EtatEntreprise etat = resultante.getEtatActuel();
					Assert.assertNotNull(etat);
					Assert.assertFalse(etat.isAnnule());
					Assert.assertEquals(TypeEtatEntreprise.FONDEE, etat.getType());

					// 2.4 pas de changement d'adresse
					final List<AdresseTiers> adresses = resultante.getAdressesTiersSorted();
					Assert.assertNotNull(adresses);
					Assert.assertEquals(0, adresses.size());

					// 2.5 événements fiscaux
					final Collection<EvenementFiscal> evtsFiscaux = evenementFiscalDAO.getEvenementsFiscaux(resultante);
					Assert.assertNotNull(evtsFiscaux);
					Assert.assertEquals(1, evtsFiscaux.size());     // 1 information complémentaire

					final EvenementFiscal evtFiscal = evtsFiscaux.iterator().next();
					Assert.assertNotNull(evtFiscal);
					Assert.assertFalse(evtFiscal.isAnnule());
					Assert.assertEquals(EvenementFiscalInformationComplementaire.class, evtFiscal.getClass());
					Assert.assertEquals(dateContratScission, evtFiscal.getDateValeur());
					Assert.assertEquals(EvenementFiscalInformationComplementaire.TypeInformationComplementaire.SCISSION, ((EvenementFiscalInformationComplementaire) evtFiscal).getType());
				}
			}
		});
	}

	@Test
	public void testAnnulationScissionEntreprise() throws Exception {

		// nous allons créer un cas un peu plus compliqué où il y a plusieurs dates de scission distinctes... il n'est pas question alors d'annuler les deux scissions d'un coup !

		final RegDate dateDebutScindee = date(2000, 5, 12);
		final RegDate dateDebutResultante1 = date(2005, 6, 13);
		final RegDate dateDebutResultante2 = date(2007, 9, 30);

		final RegDate dateContratScission1 = date(2016, 2, 2);
		final RegDate dateContratScission2 = date(2016, 4, 2);

		final class Ids {
			long idScindee;
			long idEtablissementPrincipalScindee;
			long idResultante1;
			long idEtablissementPrincipalResultante1;
			long idResultante2;
			long idEtablissementPrincipalResultante2;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final Entreprise scindee = addEntrepriseInconnueAuCivil();
				addRaisonSociale(scindee, dateDebutScindee, null, "Ma grande entreprise");
				addFormeJuridique(scindee, dateDebutScindee, null, FormeJuridiqueEntreprise.SA);
				addRegimeFiscalCH(scindee, dateDebutScindee, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalVD(scindee, dateDebutScindee, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(scindee, dateDebutScindee, DayMonth.get(12, 31), 12);        // tous les 31.12 depuis 2000
				addForPrincipal(scindee, dateDebutScindee, MotifFor.DEBUT_EXPLOITATION, MockCommune.Grandson);
				addForSecondaire(scindee, dateDebutScindee, MotifFor.DEBUT_EXPLOITATION, MockCommune.ChateauDoex, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
				scindee.setBlocageRemboursementAutomatique(Boolean.FALSE);

				addAdresseMandataireSuisse(scindee, dateDebutScindee, null, TypeMandat.GENERAL, "Mon mandataire chéri", MockRue.Renens.QuatorzeAvril);
				addAdresseSuisse(scindee, TypeAdresseTiers.COURRIER, dateDebutScindee, null, MockRue.Prilly.RueDesMetiers);

				final Etablissement etablissementPrincipalScindee = addEtablissement();
				addDomicileEtablissement(etablissementPrincipalScindee, dateDebutScindee, null, MockCommune.Grandson);
				addActiviteEconomique(scindee, etablissementPrincipalScindee, dateDebutScindee, null, true);

				addEtatEntreprise(scindee, dateDebutScindee, TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.AUTOMATIQUE);


				final Entreprise resultante1 = addEntrepriseInconnueAuCivil();
				addRaisonSociale(resultante1, dateDebutResultante1, null, "Ma toute petite entreprise");
				addFormeJuridique(resultante1, dateDebutResultante1, null, FormeJuridiqueEntreprise.SA);
				addRegimeFiscalCH(resultante1, dateDebutResultante1, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalVD(resultante1, dateDebutResultante1, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(resultante1, dateDebutResultante1, DayMonth.get(12, 31), 12);        // tous les 31.12 depuis 2005
				addForPrincipal(resultante1, dateDebutResultante1, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
				resultante1.setBlocageRemboursementAutomatique(Boolean.FALSE);

				final Etablissement etablissementPrincipalResultante1 = addEtablissement();
				addDomicileEtablissement(etablissementPrincipalResultante1, dateDebutResultante1, null, MockCommune.Lausanne);
				addActiviteEconomique(resultante1, etablissementPrincipalResultante1, dateDebutResultante1, null, true);

				addEtatEntreprise(resultante1, dateDebutResultante1, TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.AUTOMATIQUE);


				final Entreprise resultante2 = addEntrepriseInconnueAuCivil();
				addRaisonSociale(resultante2, dateDebutResultante2, null, "Ma minuscule entreprise");
				addFormeJuridique(resultante2, dateDebutResultante2, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalCH(resultante2, dateDebutResultante2, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalVD(resultante2, dateDebutResultante2, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(resultante2, dateDebutResultante2, DayMonth.get(12, 31), 12);        // tous les 31.12 depuis 2007
				addForPrincipal(resultante2, dateDebutResultante2, MotifFor.DEBUT_EXPLOITATION, MockCommune.Prilly);
				resultante2.setBlocageRemboursementAutomatique(Boolean.FALSE);

				final Etablissement etablissementPrincipalResultante2 = addEtablissement();
				addDomicileEtablissement(etablissementPrincipalResultante2, dateDebutResultante2, null, MockCommune.Prilly);
				addActiviteEconomique(resultante2, etablissementPrincipalResultante2, dateDebutResultante2, null, true);

				addEtatEntreprise(resultante2, dateDebutResultante2, TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.AUTOMATIQUE);

				addScissionEntreprise(scindee, resultante1, dateContratScission1);
				addScissionEntreprise(scindee, resultante2, dateContratScission2);


				final Ids ids = new Ids();
				ids.idScindee = scindee.getNumero();
				ids.idEtablissementPrincipalScindee = etablissementPrincipalScindee.getNumero();
				ids.idResultante1 = resultante1.getNumero();
				ids.idEtablissementPrincipalResultante1 = etablissementPrincipalResultante1.getNumero();
				ids.idResultante2 = resultante2.getNumero();
				ids.idEtablissementPrincipalResultante2 = etablissementPrincipalResultante2.getNumero();
				return ids;
			}
		});

		// annulation de la scission (avec erreur)
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise scindee = (Entreprise) tiersDAO.get(ids.idScindee);
				final Entreprise resultante1 = (Entreprise) tiersDAO.get(ids.idResultante1);
				final Entreprise resultante2 = (Entreprise) tiersDAO.get(ids.idResultante2);
				Assert.assertFalse(scindee.getBlocageRemboursementAutomatique());
				Assert.assertFalse(resultante1.getBlocageRemboursementAutomatique());
				Assert.assertFalse(resultante2.getBlocageRemboursementAutomatique());

				// attention, l'entreprise 2 a été mise là par erreur ou malveillance... on doit sauter!
				try {
					metierServicePM.annuleScission(scindee, Arrays.asList(resultante1, resultante2), dateContratScission1);
					Assert.fail("Aurait-dû sauter car l'entreprise resultante2 n'a pas la bonne date du contrat de scission");
				}
				catch (MetierServiceException e) {
					Assert.assertEquals(String.format("L'entreprise %s n'est pas associée à une scission depuis l'entreprise %s avec une date de contrat de scission au %s.",
					                                  FormatNumeroHelper.numeroCTBToDisplay(ids.idResultante2),
					                                  FormatNumeroHelper.numeroCTBToDisplay(ids.idScindee),
					                                  RegDateHelper.dateToDisplayString(dateContratScission1)),
					                    e.getMessage());

					// ne rien mettre en base quoi qu'il arrive...
					status.setRollbackOnly();
				}
			}
		});

		// annulation de la fusion (sans erreur cette fois)
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Entreprise scindee = (Entreprise) tiersDAO.get(ids.idScindee);
				final Entreprise resultante1 = (Entreprise) tiersDAO.get(ids.idResultante1);
				Assert.assertFalse(scindee.getBlocageRemboursementAutomatique());
				Assert.assertFalse(resultante1.getBlocageRemboursementAutomatique());
				metierServicePM.annuleScission(scindee, Collections.singletonList(resultante1), dateContratScission1);
			}
		});

		// vérification des données en base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {

				// 1. sur l'entreprise scindée
				// 1.1. rien n'a changé sauf l'annulation du rapport entre tiers "scission" existant avec l'entreprise resultante1
				final Entreprise scindee = (Entreprise) tiersDAO.get(ids.idScindee);
				Assert.assertNotNull(scindee);
				Assert.assertFalse(scindee.getBlocageRemboursementAutomatique());
				Assert.assertFalse(scindee.isAnnule());
				Assert.assertEquals(2, scindee.getForsFiscaux().size());
				final ForFiscalPrincipalPM ffp = scindee.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertNull(ffp.getDateFin());
				Assert.assertEquals(dateDebutScindee, ffp.getDateDebut());
				Assert.assertFalse(ffp.isAnnule());

				final List<ScissionEntreprise> scissions = extractRapports(scindee.getRapportsSujet(), ScissionEntreprise.class, new Comparator<ScissionEntreprise>() {
					@Override
					public int compare(ScissionEntreprise o1, ScissionEntreprise o2) {
						// ordre croissant des identifiants des entreprises résultantes
						return Long.compare(o1.getObjetId(), o2.getObjetId());
					}
				});
				Assert.assertNotNull(scissions);
				Assert.assertEquals(2, scissions.size());
				{
					final ScissionEntreprise scission = scissions.get(0);
					Assert.assertNotNull(scission);
					Assert.assertTrue(scission.isAnnule());
					Assert.assertEquals(dateContratScission1, scission.getDateDebut());
					Assert.assertNull(scission.getDateFin());
					Assert.assertEquals((Long) ids.idResultante1, scission.getObjetId());
				}
				{
					final ScissionEntreprise scission = scissions.get(1);
					Assert.assertNotNull(scission);
					Assert.assertFalse(scission.isAnnule());
					Assert.assertEquals(dateContratScission2, scission.getDateDebut());
					Assert.assertNull(scission.getDateFin());
					Assert.assertEquals((Long) ids.idResultante2, scission.getObjetId());
				}

				// 1.3 état inchangé
				final EtatEntreprise etatScindee = scindee.getEtatActuel();
				Assert.assertNotNull(etatScindee);
				Assert.assertFalse(etatScindee.isAnnule());
				Assert.assertEquals(TypeEtatEntreprise.FONDEE, etatScindee.getType());
				Assert.assertEquals(dateDebutScindee, etatScindee.getDateObtention());

				// 1.4 adresse inchangée
				final List<AdresseTiers> adressesScindee = scindee.getAdressesTiersSorted(TypeAdresseTiers.COURRIER);
				Assert.assertNotNull(adressesScindee);
				Assert.assertEquals(1, adressesScindee.size());
				final AdresseTiers adresseScindee = adressesScindee.get(0);
				Assert.assertNotNull(adresseScindee);
				Assert.assertFalse(adresseScindee.isAnnule());
				Assert.assertEquals(AdresseSuisse.class, adresseScindee.getClass());
				final AdresseSuisse adresseSuisseScindee = (AdresseSuisse) adresseScindee;
				Assert.assertEquals(MockRue.Prilly.RueDesMetiers.getNoRue(), adresseSuisseScindee.getNumeroRue());
				Assert.assertEquals(dateDebutScindee, adresseScindee.getDateDebut());
				Assert.assertNull(adresseScindee.getDateFin());

				// 1.5 événement fiscal
				final Collection<EvenementFiscal> evtsFiscauxScindee = evenementFiscalDAO.getEvenementsFiscaux(scindee);
				Assert.assertNotNull(evtsFiscauxScindee);
				Assert.assertEquals(1, evtsFiscauxScindee.size());
				final EvenementFiscal evtFiscalScindee = evtsFiscauxScindee.iterator().next();
				Assert.assertNotNull(evtFiscalScindee);
				Assert.assertEquals(EvenementFiscalInformationComplementaire.class, evtFiscalScindee.getClass());
				final EvenementFiscalInformationComplementaire evtFiscalInfoScindee = (EvenementFiscalInformationComplementaire) evtFiscalScindee;
				Assert.assertEquals(EvenementFiscalInformationComplementaire.TypeInformationComplementaire.ANNULATION_SCISSION, evtFiscalInfoScindee.getType());
				Assert.assertEquals(dateContratScission1, evtFiscalInfoScindee.getDateValeur());

				// 2. sur l'entreprise resultante1 dont la scission est effectivement annulée

				final Entreprise resultante1 = (Entreprise) tiersDAO.get(ids.idResultante1);
				Assert.assertNotNull(resultante1);
				Assert.assertFalse(resultante1.isAnnule());
				Assert.assertFalse(resultante1.getBlocageRemboursementAutomatique());

				// 2.1 for inchangé
				final ForFiscalPrincipalPM ffp1 = resultante1.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp1);
				Assert.assertFalse(ffp1.isAnnule());
				Assert.assertEquals(dateDebutResultante1, ffp1.getDateDebut());
				Assert.assertNull(ffp1.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp1.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), ffp1.getNumeroOfsAutoriteFiscale());

				// 2.2 état actuel inchangé
				final EtatEntreprise etatActuel1 = resultante1.getEtatActuel();
				Assert.assertNotNull(etatActuel1);
				Assert.assertEquals(TypeEtatEntreprise.FONDEE, etatActuel1.getType());
				Assert.assertEquals(dateDebutResultante1, etatActuel1.getDateObtention());

				// 2.3 rien sur les adresses
				final List<AdresseTiers> adresses1 = resultante1.getAdressesTiersSorted();
				Assert.assertNotNull(adresses1);
				Assert.assertEquals(0, adresses1.size());

				// 2.4 événement fiscal
				final Collection<EvenementFiscal> evtsFiscauxResultante1 = evenementFiscalDAO.getEvenementsFiscaux(resultante1);
				Assert.assertNotNull(evtsFiscauxResultante1);
				Assert.assertEquals(1, evtsFiscauxResultante1.size());
				final EvenementFiscal evtFiscalResultante1 = evtsFiscauxResultante1.iterator().next();
				Assert.assertNotNull(evtFiscalResultante1);
				Assert.assertEquals(EvenementFiscalInformationComplementaire.class, evtFiscalResultante1.getClass());
				final EvenementFiscalInformationComplementaire evtFiscalInfoResultante1 = (EvenementFiscalInformationComplementaire) evtFiscalResultante1;
				Assert.assertEquals(EvenementFiscalInformationComplementaire.TypeInformationComplementaire.ANNULATION_SCISSION, evtFiscalInfoResultante1.getType());
				Assert.assertEquals(dateContratScission1, evtFiscalInfoResultante1.getDateValeur());

				// 3. sur l'entreprise resultante2 dont la scission n'est finalement pas annulée

				final Entreprise resultante2 = (Entreprise) tiersDAO.get(ids.idResultante2);
				Assert.assertNotNull(resultante2);
				Assert.assertFalse(resultante2.isAnnule());
				Assert.assertFalse(resultante2.getBlocageRemboursementAutomatique());

				// 3.1. pas d'impact sur les fors
				final ForFiscalPrincipalPM ffp2 = resultante2.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp2);
				Assert.assertFalse(ffp2.isAnnule());
				Assert.assertEquals(dateDebutResultante2, ffp2.getDateDebut());
				Assert.assertNull(ffp2.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp2.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Prilly.getNoOFS(), ffp2.getNumeroOfsAutoriteFiscale());

				// 3.2. état fiscal inchangé
				final EtatEntreprise etat2 = resultante2.getEtatActuel();
				Assert.assertNotNull(etat2);
				Assert.assertFalse(etat2.isAnnule());
				Assert.assertEquals(TypeEtatEntreprise.FONDEE, etat2.getType());
				Assert.assertEquals(dateDebutResultante2, etat2.getDateObtention());

				// 3.3. pas d'événement fiscal
				final Collection<EvenementFiscal> evtsFiscauxResultante2 = evenementFiscalDAO.getEvenementsFiscaux(resultante2);
				Assert.assertNotNull(evtsFiscauxResultante2);
				Assert.assertEquals(0, evtsFiscauxResultante2.size());
			}
		});
	}

	@Test
	public void testTransfertPatrimoine() throws Exception {

		final RegDate dateDebutEmettrice = date(2000, 5, 12);
		final RegDate dateDebutReceptrice1 = date(2005, 6, 13);
		final RegDate dateDebutReceptrice2 = date(2007, 9, 30);

		final RegDate dateTransfert = date(2016, 4, 2);

		final class Ids {
			long idEmettrice;
			long idEtablissementPrincipalEmettrice;
			long idReceptrice1;
			long idEtablissementPrincipalReceptrice1;
			long idReceptrice2;
			long idEtablissementPrincipalReceptrice2;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final Entreprise emettrice = addEntrepriseInconnueAuCivil();
				addRaisonSociale(emettrice, dateDebutEmettrice, null, "Ma grande entreprise");
				addFormeJuridique(emettrice, dateDebutEmettrice, null, FormeJuridiqueEntreprise.SA);
				addRegimeFiscalCH(emettrice, dateDebutEmettrice, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalVD(emettrice, dateDebutEmettrice, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(emettrice, dateDebutEmettrice, DayMonth.get(12, 31), 12);        // tous les 31.12 depuis 2000
				addForPrincipal(emettrice, dateDebutEmettrice, MotifFor.DEBUT_EXPLOITATION, MockCommune.Grandson);
				addForSecondaire(emettrice, dateDebutEmettrice, MotifFor.DEBUT_EXPLOITATION, MockCommune.ChateauDoex, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
				emettrice.setBlocageRemboursementAutomatique(Boolean.FALSE);

				addAdresseMandataireSuisse(emettrice, dateDebutEmettrice, null, TypeMandat.GENERAL, "Mon mandataire chéri", MockRue.Renens.QuatorzeAvril);
				addAdresseSuisse(emettrice, TypeAdresseTiers.COURRIER, dateDebutEmettrice, null, MockRue.Prilly.RueDesMetiers);

				final Etablissement etablissementPrincipalEmettrice = addEtablissement();
				addDomicileEtablissement(etablissementPrincipalEmettrice, dateDebutEmettrice, null, MockCommune.Grandson);
				addActiviteEconomique(emettrice, etablissementPrincipalEmettrice, dateDebutEmettrice, null, true);

				addEtatEntreprise(emettrice, dateDebutEmettrice, TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.AUTOMATIQUE);


				final Entreprise receptrice1 = addEntrepriseInconnueAuCivil();
				addRaisonSociale(receptrice1, dateDebutReceptrice1, null, "Ma toute petite entreprise");
				addFormeJuridique(receptrice1, dateDebutReceptrice1, null, FormeJuridiqueEntreprise.SA);
				addRegimeFiscalCH(receptrice1, dateDebutReceptrice1, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalVD(receptrice1, dateDebutReceptrice1, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(receptrice1, dateDebutReceptrice1, DayMonth.get(12, 31), 12);        // tous les 31.12 depuis 2005
				addForPrincipal(receptrice1, dateDebutReceptrice1, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
				receptrice1.setBlocageRemboursementAutomatique(Boolean.FALSE);

				final Etablissement etablissementPrincipalReceptrice1 = addEtablissement();
				addDomicileEtablissement(etablissementPrincipalReceptrice1, dateDebutReceptrice1, null, MockCommune.Lausanne);
				addActiviteEconomique(receptrice1, etablissementPrincipalReceptrice1, dateDebutReceptrice1, null, true);

				addEtatEntreprise(receptrice1, dateDebutReceptrice1, TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.AUTOMATIQUE);


				final Entreprise receptrice2 = addEntrepriseInconnueAuCivil();
				addRaisonSociale(receptrice2, dateDebutReceptrice2, null, "Ma minuscule entreprise");
				addFormeJuridique(receptrice2, dateDebutReceptrice2, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalCH(receptrice2, dateDebutReceptrice2, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalVD(receptrice2, dateDebutReceptrice2, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(receptrice2, dateDebutReceptrice2, DayMonth.get(12, 31), 12);        // tous les 31.12 depuis 2007
				addForPrincipal(receptrice2, dateDebutReceptrice2, MotifFor.DEBUT_EXPLOITATION, MockCommune.Prilly);
				receptrice2.setBlocageRemboursementAutomatique(Boolean.FALSE);

				final Etablissement etablissementPrincipalReceptrice2 = addEtablissement();
				addDomicileEtablissement(etablissementPrincipalReceptrice2, dateDebutReceptrice2, null, MockCommune.Prilly);
				addActiviteEconomique(receptrice2, etablissementPrincipalReceptrice2, dateDebutReceptrice2, null, true);

				addEtatEntreprise(receptrice2, dateDebutReceptrice2, TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.AUTOMATIQUE);


				final Ids ids = new Ids();
				ids.idEmettrice = emettrice.getNumero();
				ids.idEtablissementPrincipalEmettrice = etablissementPrincipalEmettrice.getNumero();
				ids.idReceptrice1 = receptrice1.getNumero();
				ids.idEtablissementPrincipalReceptrice1 = etablissementPrincipalReceptrice1.getNumero();
				ids.idReceptrice2 = receptrice2.getNumero();
				ids.idEtablissementPrincipalReceptrice2 = etablissementPrincipalReceptrice2.getNumero();
				return ids;
			}
		});

		// lancement du transfert de patrimoine
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Entreprise emettrice = (Entreprise) tiersDAO.get(ids.idEmettrice);
				final Entreprise receptrice1 = (Entreprise) tiersDAO.get(ids.idReceptrice1);
				final Entreprise receptrice2 = (Entreprise) tiersDAO.get(ids.idReceptrice2);
				Assert.assertFalse(emettrice.getBlocageRemboursementAutomatique());
				Assert.assertFalse(receptrice1.getBlocageRemboursementAutomatique());
				Assert.assertFalse(receptrice2.getBlocageRemboursementAutomatique());
				metierServicePM.transferePatrimoine(emettrice, Arrays.asList(receptrice1, receptrice2), dateTransfert);
			}
		});

		// vérification des résultats en base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {

				// 1. sur l'entreprise émettrice
				// 1.1. rien n'a changé sauf l'apparition de nouveaux rapports entre tiers
				final Entreprise emettrice = (Entreprise) tiersDAO.get(ids.idEmettrice);
				Assert.assertNotNull(emettrice);
				Assert.assertFalse(emettrice.getBlocageRemboursementAutomatique());
				Assert.assertEquals(2, emettrice.getForsFiscaux().size());
				final ForFiscalPrincipalPM ffp = emettrice.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertNull(ffp.getDateFin());
				Assert.assertEquals(dateDebutEmettrice, ffp.getDateDebut());
				Assert.assertFalse(ffp.isAnnule());

				final List<TransfertPatrimoine> transferts = extractRapports(emettrice.getRapportsSujet(), TransfertPatrimoine.class, new Comparator<TransfertPatrimoine>() {
					@Override
					public int compare(TransfertPatrimoine o1, TransfertPatrimoine o2) {
						// ordre croissant des identifiants des entreprises réceptrices
						return Long.compare(o1.getObjetId(), o2.getObjetId());
					}
				});
				Assert.assertNotNull(transferts);
				Assert.assertEquals(2, transferts.size());
				{
					final TransfertPatrimoine transfert = transferts.get(0);
					Assert.assertNotNull(transfert);
					Assert.assertFalse(transfert.isAnnule());
					Assert.assertEquals(dateTransfert, transfert.getDateDebut());
					Assert.assertNull(transfert.getDateFin());
					Assert.assertEquals((Long) ids.idReceptrice1, transfert.getObjetId());
				}
				{
					final TransfertPatrimoine transfert = transferts.get(1);
					Assert.assertNotNull(transfert);
					Assert.assertFalse(transfert.isAnnule());
					Assert.assertEquals(dateTransfert, transfert.getDateDebut());
					Assert.assertNull(transfert.getDateFin());
					Assert.assertEquals((Long) ids.idReceptrice2, transfert.getObjetId());
				}

				// 1.2  événement fiscal de transfert de patrimoine
				final Collection<EvenementFiscal> evtsFiscauxEmettrice = evenementFiscalDAO.getEvenementsFiscaux(emettrice);
				Assert.assertNotNull(evtsFiscauxEmettrice);
				Assert.assertEquals(1, evtsFiscauxEmettrice.size());
				final EvenementFiscal evtFiscalEmettrice = evtsFiscauxEmettrice.iterator().next();
				Assert.assertNotNull(evtFiscalEmettrice);
				Assert.assertFalse(evtFiscalEmettrice.isAnnule());
				Assert.assertEquals(dateTransfert, evtFiscalEmettrice.getDateValeur());
				Assert.assertEquals(EvenementFiscalInformationComplementaire.class, evtFiscalEmettrice.getClass());
				Assert.assertEquals(EvenementFiscalInformationComplementaire.TypeInformationComplementaire.TRANSFERT_PATRIMOINE, ((EvenementFiscalInformationComplementaire) evtFiscalEmettrice).getType());

				// 1.3 état inchangé
				final EtatEntreprise etatEmettrice = emettrice.getEtatActuel();
				Assert.assertNotNull(etatEmettrice);
				Assert.assertFalse(etatEmettrice.isAnnule());
				Assert.assertEquals(TypeEtatEntreprise.FONDEE, etatEmettrice.getType());
				Assert.assertEquals(dateDebutEmettrice, etatEmettrice.getDateObtention());

				// 1.4 adresse inchangée
				final List<AdresseTiers> adressesEmettrice = emettrice.getAdressesTiersSorted(TypeAdresseTiers.COURRIER);
				Assert.assertNotNull(adressesEmettrice);
				Assert.assertEquals(1, adressesEmettrice.size());
				final AdresseTiers adresseEmettrice = adressesEmettrice.get(0);
				Assert.assertNotNull(adresseEmettrice);
				Assert.assertFalse(adresseEmettrice.isAnnule());
				Assert.assertEquals(AdresseSuisse.class, adresseEmettrice.getClass());
				final AdresseSuisse adresseSuisseEmettrice = (AdresseSuisse) adresseEmettrice;
				Assert.assertEquals(MockRue.Prilly.RueDesMetiers.getNoRue(), adresseSuisseEmettrice.getNumeroRue());
				Assert.assertEquals(dateDebutEmettrice, adresseEmettrice.getDateDebut());
				Assert.assertNull(adresseEmettrice.getDateFin());

				// 2. sur les entreprises réceptrices
				final Entreprise receptrice1 = (Entreprise) tiersDAO.get(ids.idReceptrice1);
				final Entreprise receptrice2 = (Entreprise) tiersDAO.get(ids.idReceptrice2);
				for (Entreprise receptrice : Arrays.asList(receptrice1, receptrice2)) {

					Assert.assertFalse(receptrice.getBlocageRemboursementAutomatique());

					// 2.1 les rapports entre tiers "TransfertPatrimoine" ont été testés plus haut

					// 2.2 rien sur les fors fiscaux
					final Set<ForFiscal> forsFiscaux = receptrice.getForsFiscaux();
					Assert.assertNotNull(forsFiscaux);
					Assert.assertEquals(1, forsFiscaux.size());
					final ForFiscal ff = forsFiscaux.iterator().next();
					Assert.assertNotNull(ff);
					Assert.assertEquals(ForFiscalPrincipalPM.class, ff.getClass());
					Assert.assertFalse(ff.isAnnule());

					// 2.3 pas de nouvel état fiscal
					final EtatEntreprise etat = receptrice.getEtatActuel();
					Assert.assertNotNull(etat);
					Assert.assertFalse(etat.isAnnule());
					Assert.assertEquals(TypeEtatEntreprise.FONDEE, etat.getType());

					// 2.4 pas de changement d'adresse
					final List<AdresseTiers> adresses = receptrice.getAdressesTiersSorted();
					Assert.assertNotNull(adresses);
					Assert.assertEquals(0, adresses.size());

					// 2.5 événements fiscaux
					final Collection<EvenementFiscal> evtsFiscaux = evenementFiscalDAO.getEvenementsFiscaux(receptrice);
					Assert.assertNotNull(evtsFiscaux);
					Assert.assertEquals(1, evtsFiscaux.size());     // 1 information complémentaire
					final EvenementFiscal evtFiscal = evtsFiscaux.iterator().next();
					Assert.assertNotNull(evtFiscal);
					Assert.assertFalse(evtFiscal.isAnnule());
					Assert.assertEquals(EvenementFiscalInformationComplementaire.class, evtFiscal.getClass());
					Assert.assertEquals(dateTransfert, evtFiscal.getDateValeur());
					Assert.assertEquals(EvenementFiscalInformationComplementaire.TypeInformationComplementaire.TRANSFERT_PATRIMOINE, ((EvenementFiscalInformationComplementaire) evtFiscal).getType());
				}
			}
		});
	}

	@Test
	public void testAnnulationTransfertPatrimoine() throws Exception {

		// nous allons créer un cas un peu plus compliqué où il y a plusieurs dates de transfert distinctes... il n'est pas question alors d'annuler les deux transferts d'un coup !

		final RegDate dateDebutEmettrice = date(2000, 5, 12);
		final RegDate dateDebutReceptrice1 = date(2005, 6, 13);
		final RegDate dateDebutReceptrice2 = date(2007, 9, 30);

		final RegDate dateTransfert1 = date(2016, 2, 2);
		final RegDate dateTransfert2 = date(2016, 4, 2);

		final class Ids {
			long idEmettrice;
			long idEtablissementPrincipalEmettrice;
			long idReceptrice1;
			long idEtablissementPrincipalReceptrice1;
			long idReceptrice2;
			long idEtablissementPrincipalReceptrice2;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final Entreprise emettrice = addEntrepriseInconnueAuCivil();
				addRaisonSociale(emettrice, dateDebutEmettrice, null, "Ma grande entreprise");
				addFormeJuridique(emettrice, dateDebutEmettrice, null, FormeJuridiqueEntreprise.SA);
				addRegimeFiscalCH(emettrice, dateDebutEmettrice, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalVD(emettrice, dateDebutEmettrice, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(emettrice, dateDebutEmettrice, DayMonth.get(12, 31), 12);        // tous les 31.12 depuis 2000
				addForPrincipal(emettrice, dateDebutEmettrice, MotifFor.DEBUT_EXPLOITATION, MockCommune.Grandson);
				addForSecondaire(emettrice, dateDebutEmettrice, MotifFor.DEBUT_EXPLOITATION, MockCommune.ChateauDoex, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
				emettrice.setBlocageRemboursementAutomatique(Boolean.FALSE);

				addAdresseMandataireSuisse(emettrice, dateDebutEmettrice, null, TypeMandat.GENERAL, "Mon mandataire chéri", MockRue.Renens.QuatorzeAvril);
				addAdresseSuisse(emettrice, TypeAdresseTiers.COURRIER, dateDebutEmettrice, null, MockRue.Prilly.RueDesMetiers);

				final Etablissement etablissementPrincipalEmettrice = addEtablissement();
				addDomicileEtablissement(etablissementPrincipalEmettrice, dateDebutEmettrice, null, MockCommune.Grandson);
				addActiviteEconomique(emettrice, etablissementPrincipalEmettrice, dateDebutEmettrice, null, true);

				addEtatEntreprise(emettrice, dateDebutEmettrice, TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.AUTOMATIQUE);


				final Entreprise receptrice1 = addEntrepriseInconnueAuCivil();
				addRaisonSociale(receptrice1, dateDebutReceptrice1, null, "Ma toute petite entreprise");
				addFormeJuridique(receptrice1, dateDebutReceptrice1, null, FormeJuridiqueEntreprise.SA);
				addRegimeFiscalCH(receptrice1, dateDebutReceptrice1, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalVD(receptrice1, dateDebutReceptrice1, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(receptrice1, dateDebutReceptrice1, DayMonth.get(12, 31), 12);        // tous les 31.12 depuis 2005
				addForPrincipal(receptrice1, dateDebutReceptrice1, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
				receptrice1.setBlocageRemboursementAutomatique(Boolean.FALSE);

				final Etablissement etablissementPrincipalReceptrice1 = addEtablissement();
				addDomicileEtablissement(etablissementPrincipalReceptrice1, dateDebutReceptrice1, null, MockCommune.Lausanne);
				addActiviteEconomique(receptrice1, etablissementPrincipalReceptrice1, dateDebutReceptrice1, null, true);

				addEtatEntreprise(receptrice1, dateDebutReceptrice1, TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.AUTOMATIQUE);


				final Entreprise receptrice2 = addEntrepriseInconnueAuCivil();
				addRaisonSociale(receptrice2, dateDebutReceptrice2, null, "Ma minuscule entreprise");
				addFormeJuridique(receptrice2, dateDebutReceptrice2, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalCH(receptrice2, dateDebutReceptrice2, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalVD(receptrice2, dateDebutReceptrice2, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(receptrice2, dateDebutReceptrice2, DayMonth.get(12, 31), 12);        // tous les 31.12 depuis 2007
				addForPrincipal(receptrice2, dateDebutReceptrice2, MotifFor.DEBUT_EXPLOITATION, MockCommune.Prilly);
				receptrice2.setBlocageRemboursementAutomatique(Boolean.FALSE);

				final Etablissement etablissementPrincipalReceptrice2 = addEtablissement();
				addDomicileEtablissement(etablissementPrincipalReceptrice2, dateDebutReceptrice2, null, MockCommune.Prilly);
				addActiviteEconomique(receptrice2, etablissementPrincipalReceptrice2, dateDebutReceptrice2, null, true);

				addEtatEntreprise(receptrice2, dateDebutReceptrice2, TypeEtatEntreprise.FONDEE, TypeGenerationEtatEntreprise.AUTOMATIQUE);

				addTransfertPatrimoine(emettrice, receptrice1, dateTransfert1);
				addTransfertPatrimoine(emettrice, receptrice2, dateTransfert2);


				final Ids ids = new Ids();
				ids.idEmettrice = emettrice.getNumero();
				ids.idEtablissementPrincipalEmettrice = etablissementPrincipalEmettrice.getNumero();
				ids.idReceptrice1 = receptrice1.getNumero();
				ids.idEtablissementPrincipalReceptrice1 = etablissementPrincipalReceptrice1.getNumero();
				ids.idReceptrice2 = receptrice2.getNumero();
				ids.idEtablissementPrincipalReceptrice2 = etablissementPrincipalReceptrice2.getNumero();
				return ids;
			}
		});

		// annulation du transfert (avec erreur)
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise emettrice = (Entreprise) tiersDAO.get(ids.idEmettrice);
				final Entreprise receptrice1 = (Entreprise) tiersDAO.get(ids.idReceptrice1);
				final Entreprise receptrice2 = (Entreprise) tiersDAO.get(ids.idReceptrice2);
				Assert.assertFalse(emettrice.getBlocageRemboursementAutomatique());
				Assert.assertFalse(receptrice1.getBlocageRemboursementAutomatique());
				Assert.assertFalse(receptrice2.getBlocageRemboursementAutomatique());

				// attention, l'entreprise 2 a été mise là par erreur ou malveillance... on doit sauter!
				try {
					metierServicePM.annuleTransfertPatrimoine(emettrice, Arrays.asList(receptrice1, receptrice2), dateTransfert1);
					Assert.fail("Aurait-dû sauter car l'entreprise receptrice2 n'a pas la bonne date du contrat de scission");
				}
				catch (MetierServiceException e) {
					Assert.assertEquals(String.format("L'entreprise %s n'est pas associée à un transfert de patrimoine depuis l'entreprise %s au %s.",
					                                  FormatNumeroHelper.numeroCTBToDisplay(ids.idReceptrice2),
					                                  FormatNumeroHelper.numeroCTBToDisplay(ids.idEmettrice),
					                                  RegDateHelper.dateToDisplayString(dateTransfert1)),
					                    e.getMessage());

					// ne rien mettre en base quoi qu'il arrive...
					status.setRollbackOnly();
				}
			}
		});

		// annulation du transfert (sans erreur cette fois)
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Entreprise emettrice = (Entreprise) tiersDAO.get(ids.idEmettrice);
				final Entreprise receptrice1 = (Entreprise) tiersDAO.get(ids.idReceptrice1);
				Assert.assertFalse(emettrice.getBlocageRemboursementAutomatique());
				Assert.assertFalse(receptrice1.getBlocageRemboursementAutomatique());
				metierServicePM.annuleTransfertPatrimoine(emettrice, Collections.singletonList(receptrice1), dateTransfert1);
			}
		});

		// vérification des données en base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {

				// 1. sur l'entreprise émettrice
				// 1.1. rien n'a changé sauf l'annulation du rapport entre tiers "transfert de patrimoine" existant avec l'entreprise receptrice1
				final Entreprise emettrice = (Entreprise) tiersDAO.get(ids.idEmettrice);
				Assert.assertNotNull(emettrice);
				Assert.assertFalse(emettrice.getBlocageRemboursementAutomatique());
				Assert.assertFalse(emettrice.isAnnule());
				Assert.assertEquals(2, emettrice.getForsFiscaux().size());
				final ForFiscalPrincipalPM ffp = emettrice.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertNull(ffp.getDateFin());
				Assert.assertEquals(dateDebutEmettrice, ffp.getDateDebut());
				Assert.assertFalse(ffp.isAnnule());

				final List<TransfertPatrimoine> transferts = extractRapports(emettrice.getRapportsSujet(), TransfertPatrimoine.class, new Comparator<TransfertPatrimoine>() {
					@Override
					public int compare(TransfertPatrimoine o1, TransfertPatrimoine o2) {
						// ordre croissant des identifiants des entreprises réceptrices
						return Long.compare(o1.getObjetId(), o2.getObjetId());
					}
				});
				Assert.assertNotNull(transferts);
				Assert.assertEquals(2, transferts.size());
				{
					final TransfertPatrimoine transfert = transferts.get(0);
					Assert.assertNotNull(transfert);
					Assert.assertTrue(transfert.isAnnule());
					Assert.assertEquals(dateTransfert1, transfert.getDateDebut());
					Assert.assertNull(transfert.getDateFin());
					Assert.assertEquals((Long) ids.idReceptrice1, transfert.getObjetId());
				}
				{
					final TransfertPatrimoine transfert = transferts.get(1);
					Assert.assertNotNull(transfert);
					Assert.assertFalse(transfert.isAnnule());
					Assert.assertEquals(dateTransfert2, transfert.getDateDebut());
					Assert.assertNull(transfert.getDateFin());
					Assert.assertEquals((Long) ids.idReceptrice2, transfert.getObjetId());
				}

				// 1.3 état inchangé
				final EtatEntreprise etatEmettrice = emettrice.getEtatActuel();
				Assert.assertNotNull(etatEmettrice);
				Assert.assertFalse(etatEmettrice.isAnnule());
				Assert.assertEquals(TypeEtatEntreprise.FONDEE, etatEmettrice.getType());
				Assert.assertEquals(dateDebutEmettrice, etatEmettrice.getDateObtention());

				// 1.4 adresse inchangée
				final List<AdresseTiers> adressesEmettrice = emettrice.getAdressesTiersSorted(TypeAdresseTiers.COURRIER);
				Assert.assertNotNull(adressesEmettrice);
				Assert.assertEquals(1, adressesEmettrice.size());
				final AdresseTiers adresseEmettrice = adressesEmettrice.get(0);
				Assert.assertNotNull(adresseEmettrice);
				Assert.assertFalse(adresseEmettrice.isAnnule());
				Assert.assertEquals(AdresseSuisse.class, adresseEmettrice.getClass());
				final AdresseSuisse adresseSuisseEmettrice = (AdresseSuisse) adresseEmettrice;
				Assert.assertEquals(MockRue.Prilly.RueDesMetiers.getNoRue(), adresseSuisseEmettrice.getNumeroRue());
				Assert.assertEquals(dateDebutEmettrice, adresseEmettrice.getDateDebut());
				Assert.assertNull(adresseEmettrice.getDateFin());

				// 1.5 événement fiscal
				final Collection<EvenementFiscal> evtsFiscauxEmettrice = evenementFiscalDAO.getEvenementsFiscaux(emettrice);
				Assert.assertNotNull(evtsFiscauxEmettrice);
				Assert.assertEquals(1, evtsFiscauxEmettrice.size());
				final EvenementFiscal evtFiscalEmettrice = evtsFiscauxEmettrice.iterator().next();
				Assert.assertNotNull(evtFiscalEmettrice);
				Assert.assertEquals(EvenementFiscalInformationComplementaire.class, evtFiscalEmettrice.getClass());
				final EvenementFiscalInformationComplementaire evtFiscalInfoEmettrice = (EvenementFiscalInformationComplementaire) evtFiscalEmettrice;
				Assert.assertEquals(EvenementFiscalInformationComplementaire.TypeInformationComplementaire.ANNULATION_TRANFERT_PATRIMOINE, evtFiscalInfoEmettrice.getType());
				Assert.assertEquals(dateTransfert1, evtFiscalInfoEmettrice.getDateValeur());

				// 2. sur l'entreprise receptrice1 pour laquelle le transfert est effectivement annulé

				final Entreprise receptrice1 = (Entreprise) tiersDAO.get(ids.idReceptrice1);
				Assert.assertNotNull(receptrice1);
				Assert.assertFalse(receptrice1.isAnnule());
				Assert.assertFalse(receptrice1.getBlocageRemboursementAutomatique());

				// 2.1 for inchangé
				final ForFiscalPrincipalPM ffp1 = receptrice1.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp1);
				Assert.assertFalse(ffp1.isAnnule());
				Assert.assertEquals(dateDebutReceptrice1, ffp1.getDateDebut());
				Assert.assertNull(ffp1.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp1.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), ffp1.getNumeroOfsAutoriteFiscale());

				// 2.2 état actuel inchangé
				final EtatEntreprise etatActuel1 = receptrice1.getEtatActuel();
				Assert.assertNotNull(etatActuel1);
				Assert.assertEquals(TypeEtatEntreprise.FONDEE, etatActuel1.getType());
				Assert.assertEquals(dateDebutReceptrice1, etatActuel1.getDateObtention());

				// 2.3 rien sur les adresses
				final List<AdresseTiers> adresses1 = receptrice1.getAdressesTiersSorted();
				Assert.assertNotNull(adresses1);
				Assert.assertEquals(0, adresses1.size());

				// 2.4 événement fiscal
				final Collection<EvenementFiscal> evtsFiscauxReceptrice1 = evenementFiscalDAO.getEvenementsFiscaux(receptrice1);
				Assert.assertNotNull(evtsFiscauxReceptrice1);
				Assert.assertEquals(1, evtsFiscauxReceptrice1.size());
				final EvenementFiscal evtFiscalReceptrice1 = evtsFiscauxReceptrice1.iterator().next();
				Assert.assertNotNull(evtFiscalReceptrice1);
				Assert.assertEquals(EvenementFiscalInformationComplementaire.class, evtFiscalReceptrice1.getClass());
				final EvenementFiscalInformationComplementaire evtFiscalInfoReceptrice1 = (EvenementFiscalInformationComplementaire) evtFiscalReceptrice1;
				Assert.assertEquals(EvenementFiscalInformationComplementaire.TypeInformationComplementaire.ANNULATION_TRANFERT_PATRIMOINE, evtFiscalInfoReceptrice1.getType());
				Assert.assertEquals(dateTransfert1, evtFiscalInfoReceptrice1.getDateValeur());

				// 3. sur l'entreprise receptrice2 pour laquelle le transfert n'est finalement pas annulé

				final Entreprise receptrice2 = (Entreprise) tiersDAO.get(ids.idReceptrice2);
				Assert.assertNotNull(receptrice2);
				Assert.assertFalse(receptrice2.isAnnule());
				Assert.assertFalse(receptrice2.getBlocageRemboursementAutomatique());

				// 3.1. pas d'impact sur les fors
				final ForFiscalPrincipalPM ffp2 = receptrice2.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp2);
				Assert.assertFalse(ffp2.isAnnule());
				Assert.assertEquals(dateDebutReceptrice2, ffp2.getDateDebut());
				Assert.assertNull(ffp2.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp2.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Prilly.getNoOFS(), ffp2.getNumeroOfsAutoriteFiscale());

				// 3.2. état fiscal inchangé
				final EtatEntreprise etat2 = receptrice2.getEtatActuel();
				Assert.assertNotNull(etat2);
				Assert.assertFalse(etat2.isAnnule());
				Assert.assertEquals(TypeEtatEntreprise.FONDEE, etat2.getType());
				Assert.assertEquals(dateDebutReceptrice2, etat2.getDateObtention());

				// 3.3 pas d'événement fiscal
				final Collection<EvenementFiscal> evtsFiscauxReceptrice2 = evenementFiscalDAO.getEvenementsFiscaux(receptrice2);
				Assert.assertNotNull(evtsFiscauxReceptrice2);
				Assert.assertEquals(0, evtsFiscauxReceptrice2.size());
			}
		});
	}

	@Test
	public void testReinscriptionRC() throws Exception {

		final RegDate dateDebutEntreprise = date(2003, 5, 12);
		final RegDate dateRadiationRC = date(2010, 4, 1);
		final RegDate dateFaillite = date(2010, 3, 1);

		// mise en place fiscale
		final long idpm = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebutEntreprise, null, "Ma grande entreprise");
				addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SA);
				addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);        // tous les 31.12 depuis 2000
				addForPrincipal(entreprise, dateDebutEntreprise, MotifFor.DEBUT_EXPLOITATION, dateFaillite, MotifFor.FAILLITE, MockCommune.Grandson);
				entreprise.setBlocageRemboursementAutomatique(Boolean.TRUE);

				addAdresseMandataireSuisse(entreprise, dateDebutEntreprise, null, TypeMandat.GENERAL, "Mon mandataire chéri", MockRue.Renens.QuatorzeAvril);
				addAdresseSuisse(entreprise, TypeAdresseTiers.COURRIER, dateDebutEntreprise, null, MockRue.Prilly.RueDesMetiers);

				final Etablissement etablissementPrincipalEmettrice = addEtablissement();
				addDomicileEtablissement(etablissementPrincipalEmettrice, dateDebutEntreprise, null, MockCommune.Grandson);
				addActiviteEconomique(entreprise, etablissementPrincipalEmettrice, dateDebutEntreprise, null, true);

				addEtatEntreprise(entreprise, dateDebutEntreprise, TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.AUTOMATIQUE);
				addEtatEntreprise(entreprise, dateFaillite, TypeEtatEntreprise.EN_FAILLITE, TypeGenerationEtatEntreprise.AUTOMATIQUE);
				addEtatEntreprise(entreprise, dateRadiationRC, TypeEtatEntreprise.RADIEE_RC, TypeGenerationEtatEntreprise.AUTOMATIQUE);
				return entreprise.getNumero();
			}
		});

		// lancement de la ré-inscription au RC
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idpm);
				Assert.assertNotNull(entreprise);
				Assert.assertTrue(entreprise.getBlocageRemboursementAutomatique());
				metierServicePM.reinscritRC(entreprise, dateRadiationRC, "Une remarque...");
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idpm);
				Assert.assertNotNull(entreprise);
				Assert.assertFalse(entreprise.getBlocageRemboursementAutomatique());

				final List<EtatEntreprise> tousEtats = new ArrayList<>(entreprise.getEtats());
				Assert.assertEquals(3, tousEtats.size());
				tousEtats.sort(Comparator.comparing(EtatEntreprise::getDateObtention, NullDateBehavior.LATEST::compare));
				{
					final EtatEntreprise etat = tousEtats.get(0);
					Assert.assertNotNull(etat);
					Assert.assertFalse(etat.isAnnule());
					Assert.assertEquals(TypeEtatEntreprise.INSCRITE_RC, etat.getType());
					Assert.assertEquals(dateDebutEntreprise, etat.getDateObtention());
				}
				{
					final EtatEntreprise etat = tousEtats.get(1);
					Assert.assertNotNull(etat);
					Assert.assertTrue(etat.isAnnule());
					Assert.assertEquals(TypeEtatEntreprise.EN_FAILLITE, etat.getType());
					Assert.assertEquals(dateFaillite, etat.getDateObtention());
				}
				{
					final EtatEntreprise etat = tousEtats.get(2);
					Assert.assertNotNull(etat);
					Assert.assertTrue(etat.isAnnule());
					Assert.assertEquals(TypeEtatEntreprise.RADIEE_RC, etat.getType());
					Assert.assertEquals(dateRadiationRC, etat.getDateObtention());
				}

				final ForFiscalPrincipalPM ffp = entreprise.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertFalse(ffp.isAnnule());
				Assert.assertNull(ffp.getDateFin());
				Assert.assertNull(ffp.getMotifFermeture());
				Assert.assertEquals(dateDebutEntreprise, ffp.getDateDebut());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, ffp.getMotifOuverture());

				// et la remarque ?
				final Set<Remarque> remarques = entreprise.getRemarques();
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());
				final Remarque remarque = remarques.iterator().next();
				Assert.assertNotNull(remarque);
				Assert.assertFalse(remarque.isAnnule());
				Assert.assertEquals("Une remarque...", remarque.getTexte());
			}
		});
	}
}