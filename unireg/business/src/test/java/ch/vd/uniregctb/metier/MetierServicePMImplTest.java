package ch.vd.uniregctb.metier;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockSiteOrganisationFactory;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.MontantMonetaire;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * @author Raphaël Marmier, 2016-03-21, <raphael.marmier@vd.ch>
 */
public class MetierServicePMImplTest extends BusinessTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(MetierServicePMImplTest.class);

	private TiersService tiersService;
	private MetierServicePM metierServicePM;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		tiersService = getBean(TiersService.class, "tiersService");
		metierServicePM = getBean(MetierServicePM.class, "metierServicePM");
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
		                                                                                 StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);
		MockSiteOrganisationFactory.addSite(noSite2, organisation, date(2003, 10, 5), null, nomSite2, FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                    MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2003, 10, 1), StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);

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

		RattachementOrganisationResult resultTmp;
		resultTmp = doInNewTransactionAndSession(new TransactionCallback<RattachementOrganisationResult>() {
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
		final RattachementOrganisationResult result = resultTmp;
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

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
				return null;
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
		                                                                                 StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);
		MockSiteOrganisationFactory.addSite(noSite2, organisation, date(2003, 10, 5), null, nomSite2, FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                    MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2003, 10, 1), StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(organisation);
			}
		});


		// mise en place des données fiscales
		final Long etablissement3Id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final Etablissement etablissement3 = addEtablissement();
				etablissement3.setRaisonSociale(nomSite3);
				addDomicileEtablissement(etablissement3, dateCreation, null, MockCommune.Cossonay);

				return etablissement3.getNumero();
			}
		});

		// mise en place des données fiscales
		final long entrepriseId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Etablissement etablissement = addEtablissement();
				etablissement.setRaisonSociale(nomSite);
				addDomicileEtablissement(etablissement, dateCreation, null, MockCommune.Lausanne);

				Etablissement etablissement3 = (Etablissement) tiersDAO.get(etablissement3Id);

				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSocialeFiscaleEntreprise(entreprise, dateCreation, null, nom);
				addFormeJuridique(entreprise, dateCreation, null, FormeJuridiqueEntreprise.SARL);
				addCapitalEntreprise(entreprise, dateCreation, null, new MontantMonetaire(25000L, "CHF"));
				tiersService.addActiviteEconomique(etablissement, entreprise, dateCreation, true);
				tiersService.addActiviteEconomique(etablissement3, entreprise, dateCreation, false);

				return entreprise.getNumero();
			}
		});

		RattachementOrganisationResult resultTmp;
		resultTmp = doInNewTransactionAndSession(new TransactionCallback<RattachementOrganisationResult>() {
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
		final RattachementOrganisationResult result = resultTmp;
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

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
				Assert.assertEquals(etablissement3Id.longValue(), result.getEtablissementsNonRattaches().get(0).getNumero().longValue());
				Assert.assertEquals(1, result.getSitesNonRattaches().size());
				Assert.assertEquals(noSite2.longValue(), result.getSitesNonRattaches().get(0).getNumeroSite());

				return null;
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
		                                                                                 StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);
		MockSiteOrganisationFactory.addSite(noSite2, organisation, date(2003, 10, 5), null, nomSite2, FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                    MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2003, 10, 1), StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(organisation);
			}
		});


		// mise en place des données fiscales
		final Long etablissement2Id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final Etablissement etablissement2 = addEtablissement();
				etablissement2.setRaisonSociale(nomSite2);
				etablissement2.setNumeroEtablissement(noSite2);
				addDomicileEtablissement(etablissement2, dateCreation, null, MockCommune.Aubonne);

				return etablissement2.getNumero();
			}
		});

		// mise en place des données fiscales
		final long entrepriseId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Etablissement etablissement = addEtablissement();
				etablissement.setRaisonSociale(nomSite);
				addDomicileEtablissement(etablissement, dateCreation, null, MockCommune.Lausanne);

				Etablissement etablissement2 = (Etablissement) tiersDAO.get(etablissement2Id);

				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSocialeFiscaleEntreprise(entreprise, dateCreation, null, nom);
				addFormeJuridique(entreprise, dateCreation, null, FormeJuridiqueEntreprise.SARL);
				addCapitalEntreprise(entreprise, dateCreation, null, new MontantMonetaire(25000L, "CHF"));
				tiersService.addActiviteEconomique(etablissement, entreprise, dateCreation, true);
				tiersService.addActiviteEconomique(etablissement2, entreprise, dateCreation, false);

				return entreprise.getNumero();
			}
		});

		RattachementOrganisationResult resultTmp;
		resultTmp = doInNewTransactionAndSession(new TransactionCallback<RattachementOrganisationResult>() {
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
		final RattachementOrganisationResult result = resultTmp;
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

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

				return null;
			}
		});
	}

	@Test
	public void testRattacheMauvaisEtablissement() throws Exception {

		final RegDate dateCreation = date(2001, 5, 1);
		final RegDate dateRattachement = date(2010, 6, 26);
		final String nom = "Synergy SA";
		final String nomSite = "Synergy Etablissement SA";
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		// mise en place du service mock Organisation
		final MockOrganisation organisation = MockOrganisationFactory.createOrganisation(noOrganisation, noSite, nom, dateRattachement, null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
		                                                                                 TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF,
		                                                                                 date(2010, 6, 24),
		                                                                                 StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE);
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(organisation);
			}
		});


		// mise en place des données fiscales
		final long etablissementId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Etablissement etablissement = addEtablissement();
				etablissement.setRaisonSociale(nomSite);
				addDomicileEtablissement(etablissement, dateCreation, null, MockCommune.Renens);

				return etablissement.getNumero();
			}
		});
		final long entrepriseId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final Etablissement etablissement = (Etablissement) tiersDAO.get(etablissementId);

				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSocialeFiscaleEntreprise(entreprise, dateCreation, null, nom);
				addFormeJuridique(entreprise, dateCreation, null, FormeJuridiqueEntreprise.SARL);
				addCapitalEntreprise(entreprise, dateCreation, null, new MontantMonetaire(25000L, "CHF"));
				tiersService.addActiviteEconomique(etablissement, entreprise, dateCreation, true);

				return entreprise.getNumero();
			}
		});

		RattachementOrganisationResult resultTmp = null;
		try {
			resultTmp = doInNewTransactionAndSession(new TransactionCallback<RattachementOrganisationResult>() {
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
			                             }
			);
		} catch (RuntimeException e) {
			Assert.assertEquals(String.format("ch.vd.uniregctb.metier.MetierServiceException: L'établissement principal %s n'a pas de domicile ou celui-ci ne correspond pas avec celui que rapporte le régistre civil.",
			                                  FormatNumeroHelper.numeroCTBToDisplay(etablissementId)), e.getMessage());
		}

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(noOrganisation);
				Assert.assertNull(entreprise);

				return null;
			}
		});
	}
}