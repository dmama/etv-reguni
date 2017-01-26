package ch.vd.uniregctb.evenement.organisation.casmetier.adresse;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.interfaces.organisation.data.AdresseEffectiveRCEnt;
import ch.vd.unireg.interfaces.organisation.data.AdresseLegaleRCEnt;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockDonneesRC;
import ch.vd.unireg.interfaces.organisation.mock.data.MockDonneesRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockSiteOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscal;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationErreur;
import ch.vd.uniregctb.evenement.organisation.engine.AbstractEvenementOrganisationProcessorTest;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

import static ch.vd.uniregctb.type.EtatEvenementOrganisation.A_TRAITER;

/**
 * @author Raphaël Marmier, 2016-04-11
 */
public class AdresseTest extends AbstractEvenementOrganisationProcessorTest {

	public AdresseTest() {
		setWantIndexationTiers(true);
	}

	private EvenementFiscalDAO evtFiscalDAO;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		evtFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
	}

	protected boolean buildProcessorOnSetup() {
		return true;
	}

	@Test(timeout = 10000L)
	public void testChangementAdressesRienAFaire() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                           StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
				MockSiteOrganisation sitePrincipal = (MockSiteOrganisation) org.getSitePrincipaux().get(0).getPayload();
				final MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) sitePrincipal.getDonneesRegistreIDE();
				donneesRegistreIDE.addAdresseEffective(date(2015, 7, 8), null,
						new AdresseEffectiveRCEnt(date(2015, 7, 8), null, MockLocalite.Lausanne.getNom(), "1", null, null, MockLocalite.Lausanne.getNPA().toString(), null,
						                          MockPays.Suisse.getNoOfsEtatSouverain(), "Rue du Père Noël", null, null, null)
				);
				final MockDonneesRC donneesRC = (MockDonneesRC) sitePrincipal.getDonneesRC();
				donneesRC.addAdresseLegale(date(2015, 7, 8), null,
				                           new AdresseLegaleRCEnt(date(2015, 7, 8), null, MockLocalite.Lausanne.getNom(), "427", null, null, MockLocalite.Lausanne.getNPA().toString(), null,
				                                                  MockPays.Suisse.getNoOfsEtatSouverain(), "Place des Aligators", null, null, null)
				);
				addOrganisation(org);
			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);
				Etablissement etablissement = addEtablissement();
				etablissement.setNumeroEtablissement(noSite);

				addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

				addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
				                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
				return entreprise;
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2015, 7, 8), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());

				                             final Etablissement etablissementPrincipal = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
				                             Assert.assertNotNull(etablissementPrincipal);
				                             Assert.assertEquals(date(2010, 6, 24), etablissementPrincipal.getRapportObjetValidAt(date(2010, 6, 24), TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE).getDateDebut());

				                             Assert.assertEquals(0, entreprise.getAdressesTiersSorted(TypeAdresseTiers.COURRIER).size());
				                             Assert.assertEquals(0, entreprise.getAdressesTiersSorted(TypeAdresseTiers.REPRESENTATION).size());
				                             Assert.assertEquals(0, entreprise.getAdressesTiersSorted(TypeAdresseTiers.POURSUITE).size());

				                             {
					                             ForFiscalPrincipal forFiscalPrincipal = entreprise.getDernierForFiscalPrincipal();
					                             Assert.assertEquals(date(2010, 6, 24), forFiscalPrincipal.getDateDebut());
					                             Assert.assertEquals(null, forFiscalPrincipal.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipal.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipal.getMotifOuverture());
				                             }

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(0, evtsFiscaux.size());

				                             final List<EvenementOrganisationErreur> erreurs = evt.getErreurs();
				                             Assert.assertEquals(5, erreurs.size());
				                             Assert.assertEquals("Mutation : Changement d'adresse",
				                                                 erreurs.get(1).getMessage());
				                             Assert.assertEquals("L'adresse de courrier a changé suite au changement de l'adresse effective civile (issue de l'IDE).",
				                                                 erreurs.get(2).getMessage());
				                             Assert.assertEquals("L'adresse de représentation a changé suite au changement de l'adresse effective civile (issue de l'IDE).",
				                                                 erreurs.get(3).getMessage());
				                             Assert.assertEquals("L'adresse de poursuite a changé suite au changement de l'adresse légale civile (issue du RC).",
				                                                 erreurs.get(4).getMessage());

				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testChangementAdressesLegalePoursuitePermanente() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                           StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
				MockSiteOrganisation sitePrincipal = (MockSiteOrganisation) org.getSitePrincipaux().get(0).getPayload();
				final MockDonneesRC donneesRC = (MockDonneesRC) sitePrincipal.getDonneesRC();
				donneesRC.addAdresseLegale(date(2015, 7, 8), null,
				                           new AdresseLegaleRCEnt(date(2015, 7, 8), null, MockLocalite.Lausanne.getNom(), "427", null, null, MockLocalite.Lausanne.getNPA().toString(), null,
				                                                  MockPays.Suisse.getNoOfsEtatSouverain(), "Place des Aligators", null, null, null)
				);
				addOrganisation(org);
			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);
				Etablissement etablissement = addEtablissement();
				etablissement.setNumeroEtablissement(noSite);

				AdresseSuisse poursuite = addAdresseSuisse(entreprise, TypeAdresseTiers.POURSUITE, date(2015, 1, 1), null, MockRue.Lausanne.AvenueDeLaGare);
				poursuite.setPermanente(true);

				addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

				addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
				                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
				return entreprise;
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2015, 7, 8), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.A_VERIFIER, evt.getEtat());

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());

				                             final Etablissement etablissementPrincipal = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
				                             Assert.assertNotNull(etablissementPrincipal);
				                             Assert.assertEquals(date(2010, 6, 24), etablissementPrincipal.getRapportObjetValidAt(date(2010, 6, 24), TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE).getDateDebut());

				                             Assert.assertEquals(0, entreprise.getAdressesTiersSorted(TypeAdresseTiers.COURRIER).size());
				                             Assert.assertEquals(0, entreprise.getAdressesTiersSorted(TypeAdresseTiers.REPRESENTATION).size());
				                             Assert.assertEquals(1, entreprise.getAdressesTiersSorted(TypeAdresseTiers.POURSUITE).size());
				                             final List<AdresseTiers> adressePoursuite = entreprise.getAdressesTiersSorted(TypeAdresseTiers.POURSUITE);
				                             Assert.assertEquals(1, adressePoursuite.size());
				                             Assert.assertNull(adressePoursuite.get(0).getDateFin());

				                             {
					                             ForFiscalPrincipal forFiscalPrincipal = entreprise.getDernierForFiscalPrincipal();
					                             Assert.assertEquals(date(2010, 6, 24), forFiscalPrincipal.getDateDebut());
					                             Assert.assertEquals(null, forFiscalPrincipal.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipal.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipal.getMotifOuverture());
				                             }

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(0, evtsFiscaux.size());

				                             final List<EvenementOrganisationErreur> erreurs = evt.getErreurs();
				                             Assert.assertEquals(3, erreurs.size());
				                             Assert.assertEquals("Mutation : Changement d'adresse",
				                                                 erreurs.get(1).getMessage());
				                             Assert.assertEquals("L'adresse fiscale de poursuite, permanente, est maintenue malgré le changement de l'adresse légale civile (issue du RC).",
				                                                 erreurs.get(2).getMessage());

				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testChangementAdressesLegalePoursuiteNonPermanente() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                           StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
				MockSiteOrganisation sitePrincipal = (MockSiteOrganisation) org.getSitePrincipaux().get(0).getPayload();
				final MockDonneesRC donneesRC = (MockDonneesRC) sitePrincipal.getDonneesRC();
				donneesRC.addAdresseLegale(date(2015, 7, 8), null,
						new AdresseLegaleRCEnt(date(2015, 7, 8), null, MockLocalite.Lausanne.getNom(), "427", null, null, MockLocalite.Lausanne.getNPA().toString(), null,
						                       MockPays.Suisse.getNoOfsEtatSouverain(), "Place des Aligators", null, null, null)
				);
				addOrganisation(org);
			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);
				Etablissement etablissement = addEtablissement();
				etablissement.setNumeroEtablissement(noSite);

				AdresseSuisse poursuite = addAdresseSuisse(entreprise, TypeAdresseTiers.POURSUITE, date(2015, 1, 1), null, MockRue.Lausanne.AvenueDeLaGare);
				poursuite.setPermanente(false);

				addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

				addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
				                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
				return entreprise;
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2015, 7, 8), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());

				                             final Etablissement etablissementPrincipal = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
				                             Assert.assertNotNull(etablissementPrincipal);
				                             Assert.assertEquals(date(2010, 6, 24), etablissementPrincipal.getRapportObjetValidAt(date(2010, 6, 24), TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE).getDateDebut());

				                             Assert.assertEquals(0, entreprise.getAdressesTiersSorted(TypeAdresseTiers.COURRIER).size());
				                             Assert.assertEquals(0, entreprise.getAdressesTiersSorted(TypeAdresseTiers.REPRESENTATION).size());
				                             final List<AdresseTiers> adressePoursuite = entreprise.getAdressesTiersSorted(TypeAdresseTiers.POURSUITE);
				                             Assert.assertEquals(1, adressePoursuite.size());
				                             Assert.assertEquals(date(2015, 7, 7), adressePoursuite.get(0).getDateFin());

				                             {
					                             ForFiscalPrincipal forFiscalPrincipal = entreprise.getDernierForFiscalPrincipal();
					                             Assert.assertEquals(date(2010, 6, 24), forFiscalPrincipal.getDateDebut());
					                             Assert.assertEquals(null, forFiscalPrincipal.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipal.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipal.getMotifOuverture());
				                             }

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(0, evtsFiscaux.size());

				                             final List<EvenementOrganisationErreur> erreurs = evt.getErreurs();
				                             Assert.assertEquals(3, erreurs.size());
				                             Assert.assertEquals("Mutation : Changement d'adresse",
				                                                 erreurs.get(1).getMessage());
				                             Assert.assertEquals("L'adresse fiscale de poursuite, non-permanente, a été fermée. L'adresse de poursuite est maintenant donnée par l'adresse légale civile (issue du RC).",
				                                                 erreurs.get(2).getMessage());

				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testChangementAdressesEffectivePasDeLegalePoursuiteNonPermanente() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                           StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
				MockSiteOrganisation sitePrincipal = (MockSiteOrganisation) org.getSitePrincipaux().get(0).getPayload();
				final MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) sitePrincipal.getDonneesRegistreIDE();
				donneesRegistreIDE.addAdresseEffective(date(2015, 7, 8), null,
						new AdresseEffectiveRCEnt(date(2015, 7, 8), null, MockLocalite.Lausanne.getNom(), "427", null, null, MockLocalite.Lausanne.getNPA().toString(), null,
						                       MockPays.Suisse.getNoOfsEtatSouverain(), "Place des Aligators", null, null, null)
				);
				addOrganisation(org);
			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);
				Etablissement etablissement = addEtablissement();
				etablissement.setNumeroEtablissement(noSite);

				AdresseSuisse poursuite = addAdresseSuisse(entreprise, TypeAdresseTiers.POURSUITE, date(2015, 1, 1), null, MockRue.Lausanne.AvenueDeLaGare);
				poursuite.setPermanente(false);

				addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

				addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
				                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
				return entreprise;
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2015, 7, 8), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());

				                             final Etablissement etablissementPrincipal = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
				                             Assert.assertNotNull(etablissementPrincipal);
				                             Assert.assertEquals(date(2010, 6, 24), etablissementPrincipal.getRapportObjetValidAt(date(2010, 6, 24), TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE).getDateDebut());

				                             Assert.assertEquals(0, entreprise.getAdressesTiersSorted(TypeAdresseTiers.COURRIER).size());
				                             Assert.assertEquals(0, entreprise.getAdressesTiersSorted(TypeAdresseTiers.REPRESENTATION).size());
				                             final List<AdresseTiers> adressePoursuite = entreprise.getAdressesTiersSorted(TypeAdresseTiers.POURSUITE);
				                             Assert.assertEquals(1, adressePoursuite.size());
				                             Assert.assertEquals(date(2015, 7, 7), adressePoursuite.get(0).getDateFin());

				                             {
					                             ForFiscalPrincipal forFiscalPrincipal = entreprise.getDernierForFiscalPrincipal();
					                             Assert.assertEquals(date(2010, 6, 24), forFiscalPrincipal.getDateDebut());
					                             Assert.assertEquals(null, forFiscalPrincipal.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipal.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipal.getMotifOuverture());
				                             }

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(0, evtsFiscaux.size());

				                             final List<EvenementOrganisationErreur> erreurs = evt.getErreurs();
				                             Assert.assertEquals(5, erreurs.size());
				                             Assert.assertEquals("Mutation : Changement d'adresse",
				                                                 erreurs.get(1).getMessage());
				                             Assert.assertEquals("L'adresse de courrier a changé suite au changement de l'adresse effective civile (issue de l'IDE).",
				                                                 erreurs.get(2).getMessage());
				                             Assert.assertEquals("L'adresse de représentation a changé suite au changement de l'adresse effective civile (issue de l'IDE).",
				                                                 erreurs.get(3).getMessage());
				                             Assert.assertEquals("L'adresse fiscale de poursuite, non-permanente, a été fermée. L'adresse de poursuite est maintenant donnée par l'adresse effective civile (issue de l'IDE), en l'absence d'adresse légale civile (issue du RC).",
				                                                 erreurs.get(4).getMessage());

				                             return null;
			                             }
		                             }
		);
	}

	// SIFISC-19628 - PROD : Doublons dans les mutations IDE (deux strictements identiques)
	@Test(timeout = 1000000L)
	public void testChangementAdressesEffectivePasDeLegalePoursuiteNonPermanente2foisMemeJour() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                           StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
				MockSiteOrganisation sitePrincipal = (MockSiteOrganisation) org.getSitePrincipaux().get(0).getPayload();
				final MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) sitePrincipal.getDonneesRegistreIDE();
				donneesRegistreIDE.addAdresseEffective(date(2015, 7, 8), null,
						new AdresseEffectiveRCEnt(date(2015, 7, 8), null, MockLocalite.Lausanne.getNom(), "427", null, null, MockLocalite.Lausanne.getNPA().toString(), null,
						                       MockPays.Suisse.getNoOfsEtatSouverain(), "Place des Aligators", null, null, null)
				);
				addOrganisation(org);
			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);
				Etablissement etablissement = addEtablissement();
				etablissement.setNumeroEtablissement(noSite);

				AdresseSuisse poursuite = addAdresseSuisse(entreprise, TypeAdresseTiers.POURSUITE, date(2015, 1, 1), null, MockRue.Lausanne.AvenueDeLaGare);
				poursuite.setPermanente(false);

				addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

				addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
				                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
				return entreprise;
			}
		});

		// Création de l'événement
		final Long noEvenement = 11111111L;
		final Long noEvenement2 = 222222221L;

		// Persistence événement
		final long evtid_1 = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2015, 7, 8), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		final long evtid_2 = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement2, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2015, 7, 8), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {
				                             {
					                             final EvenementOrganisation evt1 = evtOrganisationService.get(evtid_1);
					                             Assert.assertNotNull(evt1);
					                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt1.getEtat());

					                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt1.getNoOrganisation());

					                             final Etablissement etablissementPrincipal = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
					                             Assert.assertNotNull(etablissementPrincipal);
					                             Assert.assertEquals(date(2010, 6, 24),
					                                                 etablissementPrincipal.getRapportObjetValidAt(date(2010, 6, 24), TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE).getDateDebut());

					                             Assert.assertEquals(0, entreprise.getAdressesTiersSorted(TypeAdresseTiers.COURRIER).size());
					                             Assert.assertEquals(0, entreprise.getAdressesTiersSorted(TypeAdresseTiers.REPRESENTATION).size());
					                             final List<AdresseTiers> adressePoursuite = entreprise.getAdressesTiersSorted(TypeAdresseTiers.POURSUITE);
					                             Assert.assertEquals(1, adressePoursuite.size());
					                             Assert.assertEquals(date(2015, 7, 7), adressePoursuite.get(0).getDateFin());

					                             {
						                             ForFiscalPrincipal forFiscalPrincipal = entreprise.getDernierForFiscalPrincipal();
						                             Assert.assertEquals(date(2010, 6, 24), forFiscalPrincipal.getDateDebut());
						                             Assert.assertEquals(null, forFiscalPrincipal.getDateFin());
						                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
						                             Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
						                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipal.getTypeAutoriteFiscale());
						                             Assert.assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
						                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipal.getMotifOuverture());
					                             }

					                             // vérification des événements fiscaux
					                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
					                             Assert.assertNotNull(evtsFiscaux);
					                             Assert.assertEquals(0, evtsFiscaux.size());

					                             final List<EvenementOrganisationErreur> erreurs = evt1.getErreurs();
					                             Assert.assertEquals(5, erreurs.size());
					                             Assert.assertEquals("Mutation : Changement d'adresse",
					                                                 erreurs.get(1).getMessage());
					                             Assert.assertEquals("L'adresse de courrier a changé suite au changement de l'adresse effective civile (issue de l'IDE).",
					                                                 erreurs.get(2).getMessage());
					                             Assert.assertEquals("L'adresse de représentation a changé suite au changement de l'adresse effective civile (issue de l'IDE).",
					                                                 erreurs.get(3).getMessage());
					                             Assert.assertEquals(
							                             "L'adresse fiscale de poursuite, non-permanente, a été fermée. L'adresse de poursuite est maintenant donnée par l'adresse effective civile (issue de l'IDE), en l'absence d'adresse légale civile (issue du RC).",
							                             erreurs.get(4).getMessage());
				                             }
				                             {
					                             final EvenementOrganisation evt2 = evtOrganisationService.get(evtid_2);
					                             Assert.assertNotNull(evt2);
					                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt2.getEtat());

					                             final List<EvenementOrganisationErreur> erreurs2 = evt2.getErreurs();
					                             Assert.assertEquals(5, erreurs2.size());
					                             Assert.assertEquals("L'adresse de poursuite a changé suite au changement de l'adresse effective civile (issue de l'IDE), en l'absence d'adresse légale civile (issue du RC).",
					                                                 evt2.getErreurs().get(4).getMessage());
				                             }
				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testChangementAdressesEffectivePasDeLegalePoursuitePermanente() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                           StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
				MockSiteOrganisation sitePrincipal = (MockSiteOrganisation) org.getSitePrincipaux().get(0).getPayload();
				final MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) sitePrincipal.getDonneesRegistreIDE();
				donneesRegistreIDE.addAdresseEffective(date(2015, 7, 8), null,
						new AdresseEffectiveRCEnt(date(2015, 7, 8), null, MockLocalite.Lausanne.getNom(), "427", null, null, MockLocalite.Lausanne.getNPA().toString(), null,
						                       MockPays.Suisse.getNoOfsEtatSouverain(), "Place des Aligators", null, null, null)
				);
				addOrganisation(org);
			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);
				Etablissement etablissement = addEtablissement();
				etablissement.setNumeroEtablissement(noSite);

				AdresseSuisse poursuite = addAdresseSuisse(entreprise, TypeAdresseTiers.POURSUITE, date(2015, 1, 1), null, MockRue.Lausanne.AvenueDeLaGare);
				poursuite.setPermanente(true);

				addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

				addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
				                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
				return entreprise;
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2015, 7, 8), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.A_VERIFIER, evt.getEtat());

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());

				                             final Etablissement etablissementPrincipal = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
				                             Assert.assertNotNull(etablissementPrincipal);
				                             Assert.assertEquals(date(2010, 6, 24), etablissementPrincipal.getRapportObjetValidAt(date(2010, 6, 24), TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE).getDateDebut());

				                             Assert.assertEquals(0, entreprise.getAdressesTiersSorted(TypeAdresseTiers.COURRIER).size());
				                             Assert.assertEquals(0, entreprise.getAdressesTiersSorted(TypeAdresseTiers.REPRESENTATION).size());
				                             final List<AdresseTiers> adressePoursuite = entreprise.getAdressesTiersSorted(TypeAdresseTiers.POURSUITE);
				                             Assert.assertEquals(1, adressePoursuite.size());
				                             Assert.assertEquals(null, adressePoursuite.get(0).getDateFin());

				                             {
					                             ForFiscalPrincipal forFiscalPrincipal = entreprise.getDernierForFiscalPrincipal();
					                             Assert.assertEquals(date(2010, 6, 24), forFiscalPrincipal.getDateDebut());
					                             Assert.assertEquals(null, forFiscalPrincipal.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipal.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipal.getMotifOuverture());
				                             }

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(0, evtsFiscaux.size());

				                             final List<EvenementOrganisationErreur> erreurs = evt.getErreurs();
				                             Assert.assertEquals(5, erreurs.size());
				                             Assert.assertEquals("Mutation : Changement d'adresse",
				                                                 erreurs.get(1).getMessage());
				                             Assert.assertEquals("L'adresse de courrier a changé suite au changement de l'adresse effective civile (issue de l'IDE).",
				                                                 erreurs.get(2).getMessage());
				                             Assert.assertEquals("L'adresse de représentation a changé suite au changement de l'adresse effective civile (issue de l'IDE).",
				                                                 erreurs.get(3).getMessage());
				                             Assert.assertEquals("L'adresse fiscale poursuite, permanente, est maintenue malgré le changement de l'adresse effective civile (issue de l'IDE), en l'absence d'adresse légale civile (issue du RC).",
				                                                 erreurs.get(4).getMessage());

				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testChangementAdressesEffectivePasDeLegale() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                           StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
				MockSiteOrganisation sitePrincipal = (MockSiteOrganisation) org.getSitePrincipaux().get(0).getPayload();
				final MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) sitePrincipal.getDonneesRegistreIDE();
				donneesRegistreIDE.addAdresseEffective(date(2015, 7, 8), null,
				                                       new AdresseEffectiveRCEnt(date(2015, 7, 8), null, MockLocalite.Lausanne.getNom(), "427", null, null, MockLocalite.Lausanne.getNPA().toString(), null,
				                                                                 MockPays.Suisse.getNoOfsEtatSouverain(), "Place des Aligators", null, null, null)
				);
				addOrganisation(org);
			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);
				Etablissement etablissement = addEtablissement();
				etablissement.setNumeroEtablissement(noSite);

				addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

				addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
				                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
				return entreprise;
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2015, 7, 8), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());

				                             final Etablissement etablissementPrincipal = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
				                             Assert.assertNotNull(etablissementPrincipal);
				                             Assert.assertEquals(date(2010, 6, 24), etablissementPrincipal.getRapportObjetValidAt(date(2010, 6, 24), TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE).getDateDebut());

				                             Assert.assertEquals(0, entreprise.getAdressesTiersSorted(TypeAdresseTiers.COURRIER).size());
				                             Assert.assertEquals(0, entreprise.getAdressesTiersSorted(TypeAdresseTiers.REPRESENTATION).size());
				                             Assert.assertEquals(0, entreprise.getAdressesTiersSorted(TypeAdresseTiers.POURSUITE).size());

				                             {
					                             ForFiscalPrincipal forFiscalPrincipal = entreprise.getDernierForFiscalPrincipal();
					                             Assert.assertEquals(date(2010, 6, 24), forFiscalPrincipal.getDateDebut());
					                             Assert.assertEquals(null, forFiscalPrincipal.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipal.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipal.getMotifOuverture());
				                             }

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(0, evtsFiscaux.size());

				                             final List<EvenementOrganisationErreur> erreurs = evt.getErreurs();
				                             Assert.assertEquals(5, erreurs.size());
				                             Assert.assertEquals("Mutation : Changement d'adresse",
				                                                 erreurs.get(1).getMessage());
				                             Assert.assertEquals("L'adresse de courrier a changé suite au changement de l'adresse effective civile (issue de l'IDE).",
				                                                 erreurs.get(2).getMessage());
				                             Assert.assertEquals("L'adresse de représentation a changé suite au changement de l'adresse effective civile (issue de l'IDE).",
				                                                 erreurs.get(3).getMessage());
				                             Assert.assertEquals("L'adresse de poursuite a changé suite au changement de l'adresse effective civile (issue de l'IDE), en l'absence d'adresse légale civile (issue du RC).",
				                                                 erreurs.get(4).getMessage());

				                             return null;
			                             }
		                             }
		);
	}
}