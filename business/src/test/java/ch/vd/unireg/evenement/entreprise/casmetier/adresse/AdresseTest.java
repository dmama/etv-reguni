package ch.vd.unireg.evenement.entreprise.casmetier.adresse;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.adresse.AdresseSuisse;
import ch.vd.unireg.adresse.AdresseTiers;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseErreur;
import ch.vd.unireg.evenement.entreprise.engine.AbstractEvenementEntrepriseCivileProcessorTest;
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.unireg.interfaces.entreprise.data.AdresseEffectiveRCEnt;
import ch.vd.unireg.interfaces.entreprise.data.AdresseLegaleRCEnt;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.MockServiceEntreprise;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockDonneesRC;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockDonneesRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementEntreprise;
import ch.vd.unireg.type.TypeRapportEntreTiers;

import static ch.vd.unireg.type.EtatEvenementEntreprise.A_TRAITER;

/**
 * @author Raphaël Marmier, 2016-04-11
 */
public class AdresseTest extends AbstractEvenementEntrepriseCivileProcessorTest {

	public AdresseTest() {
		setWantIndexationTiers(true);
	}

	private EvenementFiscalDAO evtFiscalDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		evtFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
	}

	protected boolean buildProcessorOnSetup() {
		return true;
	}

	@Test(timeout = 10000L)
	public void testChangementAdressesRienAFaire() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
				MockEtablissementCivil etablissementPrincipal = (MockEtablissementCivil) ent.getEtablissementsPrincipaux().get(0).getPayload();
				final MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) etablissementPrincipal.getDonneesRegistreIDE();
				donneesRegistreIDE.addAdresseEffective(new AdresseEffectiveRCEnt(date(2015, 7, 8), null,
				                                                                 MockLocalite.Lausanne.getNom(), "1", null, null, MockLocalite.Lausanne.getNPA().toString(), null,
				                                                                 MockPays.Suisse.getNoOfsEtatSouverain(), "Rue du Père Noël", null, null, null));
				final MockDonneesRC donneesRC = (MockDonneesRC) etablissementPrincipal.getDonneesRC();
				donneesRC.addAdresseLegale(new AdresseLegaleRCEnt(date(2015, 7, 8), null,
				                                                  MockLocalite.Lausanne.getNom(), "427", null, null, MockLocalite.Lausanne.getNPA().toString(), null,
				                                                  MockPays.Suisse.getNoOfsEtatSouverain(), "Place des Aligators", null, null, null));
				addEntreprise(ent);
			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			return entreprise;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2015, 7, 8), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());

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

			final List<EvenementEntrepriseErreur> erreurs = evt.getErreurs();
			Assert.assertEquals(5, erreurs.size());
			Assert.assertEquals("Mutation : Changement d'adresse",
			                    erreurs.get(1).getMessage());
			Assert.assertEquals("L'adresse de courrier a changé suite au changement de l'adresse effective civile.",
			                    erreurs.get(2).getMessage());
			Assert.assertEquals("L'adresse de représentation a changé suite au changement de l'adresse effective civile.",
			                    erreurs.get(3).getMessage());
			Assert.assertEquals("L'adresse de poursuite a changé suite au changement de l'adresse légale civile (issue du RC).",
			                    erreurs.get(4).getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testChangementAdressesLegalePoursuitePermanente() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
				MockEtablissementCivil etablissementPrincipal = (MockEtablissementCivil) ent.getEtablissementsPrincipaux().get(0).getPayload();
				final MockDonneesRC donneesRC = (MockDonneesRC) etablissementPrincipal.getDonneesRC();
				donneesRC.addAdresseLegale(new AdresseLegaleRCEnt(date(2015, 7, 8), null,
				                                                  MockLocalite.Lausanne.getNom(), "427", null, null, MockLocalite.Lausanne.getNPA().toString(), null,
				                                                  MockPays.Suisse.getNoOfsEtatSouverain(), "Place des Aligators", null, null, null));
				addEntreprise(ent);
			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			AdresseSuisse poursuite = addAdresseSuisse(entreprise, TypeAdresseTiers.POURSUITE, date(2015, 1, 1), null, MockRue.Lausanne.AvenueDeLaGare);
			poursuite.setPermanente(true);

			addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			return entreprise;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2015, 7, 8), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.A_VERIFIER, evt.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());

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

			final List<EvenementEntrepriseErreur> erreurs = evt.getErreurs();
			Assert.assertEquals(3, erreurs.size());
			Assert.assertEquals("Mutation : Changement d'adresse",
			                    erreurs.get(1).getMessage());
			Assert.assertEquals("L'adresse fiscale de poursuite, permanente, est maintenue malgré le changement de l'adresse légale civile (issue du RC).",
			                    erreurs.get(2).getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testChangementAdressesLegalePoursuiteNonPermanente() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
				MockEtablissementCivil etablissementPrincipal = (MockEtablissementCivil) ent.getEtablissementsPrincipaux().get(0).getPayload();
				final MockDonneesRC donneesRC = (MockDonneesRC) etablissementPrincipal.getDonneesRC();
				donneesRC.addAdresseLegale(new AdresseLegaleRCEnt(date(2015, 7, 8), null,
				                                                  MockLocalite.Lausanne.getNom(), "427", null, null, MockLocalite.Lausanne.getNPA().toString(), null,
				                                                  MockPays.Suisse.getNoOfsEtatSouverain(), "Place des Aligators", null, null, null));
				addEntreprise(ent);
			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			AdresseSuisse poursuite = addAdresseSuisse(entreprise, TypeAdresseTiers.POURSUITE, date(2015, 1, 1), null, MockRue.Lausanne.AvenueDeLaGare);
			poursuite.setPermanente(false);

			addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			return entreprise;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2015, 7, 8), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());

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

			final List<EvenementEntrepriseErreur> erreurs = evt.getErreurs();
			Assert.assertEquals(3, erreurs.size());
			Assert.assertEquals("Mutation : Changement d'adresse",
			                    erreurs.get(1).getMessage());
			Assert.assertEquals("L'adresse fiscale de poursuite, non-permanente, a été fermée. L'adresse de poursuite est maintenant donnée par l'adresse légale civile (issue du RC).",
			                    erreurs.get(2).getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testChangementAdressesEffectivePasDeLegalePoursuiteNonPermanente() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
				MockEtablissementCivil etablissementPrincipal = (MockEtablissementCivil) ent.getEtablissementsPrincipaux().get(0).getPayload();
				final MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) etablissementPrincipal.getDonneesRegistreIDE();
				donneesRegistreIDE.addAdresseEffective(new AdresseEffectiveRCEnt(date(2015, 7, 8), null,
				                                                                 MockLocalite.Lausanne.getNom(), "427", null, null, MockLocalite.Lausanne.getNPA().toString(), null,
				                                                                 MockPays.Suisse.getNoOfsEtatSouverain(), "Place des Aligators", null, null, null));
				addEntreprise(ent);
			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			AdresseSuisse poursuite = addAdresseSuisse(entreprise, TypeAdresseTiers.POURSUITE, date(2015, 1, 1), null, MockRue.Lausanne.AvenueDeLaGare);
			poursuite.setPermanente(false);

			addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			return entreprise;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2015, 7, 8), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());

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

			final List<EvenementEntrepriseErreur> erreurs = evt.getErreurs();
			Assert.assertEquals(5, erreurs.size());
			Assert.assertEquals("Mutation : Changement d'adresse",
			                    erreurs.get(1).getMessage());
			Assert.assertEquals("L'adresse de courrier a changé suite au changement de l'adresse effective civile.",
			                    erreurs.get(2).getMessage());
			Assert.assertEquals("L'adresse de représentation a changé suite au changement de l'adresse effective civile.",
			                    erreurs.get(3).getMessage());
			Assert.assertEquals("L'adresse fiscale de poursuite, non-permanente, a été fermée. L'adresse de poursuite est maintenant donnée par l'adresse effective civile, en l'absence d'adresse légale civile (issue du RC).",
			                    erreurs.get(4).getMessage());
			return null;
		});
	}

	// SIFISC-19628 - PROD : Doublons dans les mutations IDE (deux strictements identiques)
	@Test(timeout = 1000000L)
	public void testChangementAdressesEffectivePasDeLegalePoursuiteNonPermanente2foisMemeJour() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
				MockEtablissementCivil etablissementPrincipal = (MockEtablissementCivil) ent.getEtablissementsPrincipaux().get(0).getPayload();
				final MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) etablissementPrincipal.getDonneesRegistreIDE();
				donneesRegistreIDE.addAdresseEffective(new AdresseEffectiveRCEnt(date(2015, 7, 8), null,
				                                                                 MockLocalite.Lausanne.getNom(), "427", null, null, MockLocalite.Lausanne.getNPA().toString(), null,
				                                                                 MockPays.Suisse.getNoOfsEtatSouverain(), "Place des Aligators", null, null, null));
				addEntreprise(ent);
			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			AdresseSuisse poursuite = addAdresseSuisse(entreprise, TypeAdresseTiers.POURSUITE, date(2015, 1, 1), null, MockRue.Lausanne.AvenueDeLaGare);
			poursuite.setPermanente(false);

			addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			return entreprise;
		});

		// Création de l'événement
		final Long noEvenement = 11111111L;
		final Long noEvenement2 = 222222221L;

		// Persistence événement
		final long evtid_1 = doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2015, 7, 8), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		final long evtid_2 = doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement2, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2015, 7, 8), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			{
				final EvenementEntreprise evt1 = evtEntrepriseService.get(evtid_1);
				Assert.assertNotNull(evt1);
				Assert.assertEquals(EtatEvenementEntreprise.TRAITE, evt1.getEtat());

				final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt1.getNoEntrepriseCivile());

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

				final List<EvenementEntrepriseErreur> erreurs = evt1.getErreurs();
				Assert.assertEquals(5, erreurs.size());
				Assert.assertEquals("Mutation : Changement d'adresse",
				                    erreurs.get(1).getMessage());
				Assert.assertEquals("L'adresse de courrier a changé suite au changement de l'adresse effective civile.",
				                    erreurs.get(2).getMessage());
				Assert.assertEquals("L'adresse de représentation a changé suite au changement de l'adresse effective civile.",
				                    erreurs.get(3).getMessage());
				Assert.assertEquals(
						"L'adresse fiscale de poursuite, non-permanente, a été fermée. L'adresse de poursuite est maintenant donnée par l'adresse effective civile, en l'absence d'adresse légale civile (issue du RC).",
						erreurs.get(4).getMessage());
			}
			{
				final EvenementEntreprise evt2 = evtEntrepriseService.get(evtid_2);
				Assert.assertNotNull(evt2);
				Assert.assertEquals(EtatEvenementEntreprise.TRAITE, evt2.getEtat());

				final List<EvenementEntrepriseErreur> erreurs2 = evt2.getErreurs();
				Assert.assertEquals(5, erreurs2.size());
				Assert.assertEquals("L'adresse de poursuite a changé suite au changement de l'adresse effective civile, en l'absence d'adresse légale civile (issue du RC).",
				                    evt2.getErreurs().get(4).getMessage());
			}
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testChangementAdressesEffectivePasDeLegalePoursuitePermanente() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
				MockEtablissementCivil etablissementPrincipal = (MockEtablissementCivil) ent.getEtablissementsPrincipaux().get(0).getPayload();
				final MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) etablissementPrincipal.getDonneesRegistreIDE();
				donneesRegistreIDE.addAdresseEffective(new AdresseEffectiveRCEnt(date(2015, 7, 8), null,
				                                                                 MockLocalite.Lausanne.getNom(), "427", null, null, MockLocalite.Lausanne.getNPA().toString(), null,
				                                                                 MockPays.Suisse.getNoOfsEtatSouverain(), "Place des Aligators", null, null, null));
				addEntreprise(ent);
			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			AdresseSuisse poursuite = addAdresseSuisse(entreprise, TypeAdresseTiers.POURSUITE, date(2015, 1, 1), null, MockRue.Lausanne.AvenueDeLaGare);
			poursuite.setPermanente(true);

			addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			return entreprise;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2015, 7, 8), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.A_VERIFIER, evt.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());

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

			final List<EvenementEntrepriseErreur> erreurs = evt.getErreurs();
			Assert.assertEquals(5, erreurs.size());
			Assert.assertEquals("Mutation : Changement d'adresse",
			                    erreurs.get(1).getMessage());
			Assert.assertEquals("L'adresse de courrier a changé suite au changement de l'adresse effective civile.",
			                    erreurs.get(2).getMessage());
			Assert.assertEquals("L'adresse de représentation a changé suite au changement de l'adresse effective civile.",
			                    erreurs.get(3).getMessage());
			Assert.assertEquals("L'adresse fiscale poursuite, permanente, est maintenue malgré le changement de l'adresse effective civile, en l'absence d'adresse légale civile (issue du RC).",
			                    erreurs.get(4).getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testChangementAdressesEffectivePasDeLegale() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
				MockEtablissementCivil etablissementPrincipal = (MockEtablissementCivil) ent.getEtablissementsPrincipaux().get(0).getPayload();
				final MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) etablissementPrincipal.getDonneesRegistreIDE();
				donneesRegistreIDE.addAdresseEffective(new AdresseEffectiveRCEnt(date(2015, 7, 8), null,
				                                                                 MockLocalite.Lausanne.getNom(), "427", null, null, MockLocalite.Lausanne.getNPA().toString(), null,
				                                                                 MockPays.Suisse.getNoOfsEtatSouverain(), "Place des Aligators", null, null, null));
				addEntreprise(ent);
			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			return entreprise;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2015, 7, 8), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());

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

			final List<EvenementEntrepriseErreur> erreurs = evt.getErreurs();
			Assert.assertEquals(5, erreurs.size());
			Assert.assertEquals("Mutation : Changement d'adresse",
			                    erreurs.get(1).getMessage());
			Assert.assertEquals("L'adresse de courrier a changé suite au changement de l'adresse effective civile.",
			                    erreurs.get(2).getMessage());
			Assert.assertEquals("L'adresse de représentation a changé suite au changement de l'adresse effective civile.",
			                    erreurs.get(3).getMessage());
			Assert.assertEquals("L'adresse de poursuite a changé suite au changement de l'adresse effective civile, en l'absence d'adresse légale civile (issue du RC).",
			                    erreurs.get(4).getMessage());
			return null;
		});
	}
}