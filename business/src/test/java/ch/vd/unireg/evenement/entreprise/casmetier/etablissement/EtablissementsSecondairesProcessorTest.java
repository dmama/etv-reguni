package ch.vd.unireg.evenement.entreprise.casmetier.etablissement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseErreur;
import ch.vd.unireg.evenement.entreprise.engine.AbstractEvenementEntrepriseCivileProcessorTest;
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalFor;
import ch.vd.unireg.interfaces.entreprise.data.DateRanged;
import ch.vd.unireg.interfaces.entreprise.data.EntreeJournalRC;
import ch.vd.unireg.interfaces.entreprise.data.EtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.InscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.InscriptionREE;
import ch.vd.unireg.interfaces.entreprise.data.PublicationFOSC;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusREE;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.MockEntrepriseConnector;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockDonneesRC;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockDonneesREE;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockDonneesRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEtablissementCivilFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.tiers.CapitalFiscalEntreprise;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.tiers.FormeJuridiqueFiscaleEntreprise;
import ch.vd.unireg.tiers.MontantMonetaire;
import ch.vd.unireg.tiers.RaisonSocialeFiscaleEntreprise;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementEntreprise;
import ch.vd.unireg.type.TypeRapportEntreTiers;

import static ch.vd.unireg.type.EtatEvenementEntreprise.A_TRAITER;

/**
 * @author Raphaël Marmier, 2016-02-29
 */
public class EtablissementsSecondairesProcessorTest extends AbstractEvenementEntrepriseCivileProcessorTest {

	public EtablissementsSecondairesProcessorTest() {
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
	public void testNouvelEtablissementSecondaire() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;
		final Long noEtablissement2 = noEntrepriseCivile + 2000000;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999995");

				MockEtablissementCivil nouveauSiteSecondaire = MockEtablissementCivilFactory.addEtablissement(noEtablissement2, ent, date(2015, 7, 8), null, "Synergy Aubonne SA",
				                                                                                              FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                              MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2015, 7, 4),
				                                                                                              StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
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
			final Etablissement etablissementSecondaire = tiersService.getEtablissementsSecondairesEntreprise(entreprise).get(0).getPayload();
			Assert.assertNotNull(etablissementSecondaire);

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
			{
				final List<ForFiscalSecondaire> forFiscalSecondaires = entreprise.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.Aubonne.getNoOFS());
				Assert.assertEquals(1, forFiscalSecondaires.size());
				ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
				Assert.assertEquals(date(2015, 7, 4), forFiscalSecondaire.getDateDebut());
				Assert.assertEquals(null, forFiscalSecondaire.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
				Assert.assertEquals(MockCommune.Aubonne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
			}

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			Assert.assertEquals(1, evtsFiscaux.size()); // 1 for secondaire créé

			final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
			Collections.sort(evtsFiscauxTries, (o1, o2) -> Long.compare(o1.getId(), o2.getId()));

			{
				final EvenementFiscal ef = evtsFiscauxTries.get(0);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 7, 4), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				Assert.assertEquals(date(2015, 7, 4), eff.getForFiscal().getDateDebut());
			}
			return null;
		});
	}

	// SIFISC-18739 - Ajouter le REE dans le calcul de la notion d'activité de l'établissement
	@Test(timeout = 10000L)
	public void testNouvelEtablissementSecondaireREE() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;
		final Long noEtablissement2 = noEntrepriseCivile + 2000000;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999995");

				MockEtablissementCivil nouveauSiteSecondaire = MockEtablissementCivilFactory.addEtablissement(noEtablissement2, ent, date(2015, 7, 4), null, "Synergy Aubonne SA",
				                                                                                              FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                              MockCommune.Aubonne.getNoOFS(), null, null,
				                                                                                              null, null, null);
				final MockDonneesREE donneesREE = nouveauSiteSecondaire.getDonneesREE();
				donneesREE.changeInscriptionREE(date(2015, 7, 4), new InscriptionREE(StatusREE.ACTIF, date(2015, 7, 1)));
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
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.REE_NOUVELLE_INSCRIPTION, date(2015, 7, 4), A_TRAITER);
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
/* En attendant qu'on déplombe la création des établissements pur REE				                             {

				                             final Etablissement etablissementPrincipal = tiersService.getEtablissementsPrincipauxEntreprise(organisation).get(0).getPayload();
				                             Assert.assertNotNull(etablissementPrincipal);
				                             Assert.assertEquals(date(2010, 6, 24), etablissementPrincipal.getRapportObjetValidAt(date(2010, 6, 24), TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE).getDateDebut());
				                             final Etablissement etablissementSecondaire = tiersService.getEtablissementsSecondairesEntreprise(organisation).get(0).getPayload();
				                             Assert.assertNotNull(etablissementSecondaire);
*/
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
/* En attendant qu'on déplombe la création des établissements pur REE				                             {
					                             final List<ForFiscalSecondaire> forFiscalSecondaires = organisation.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.Aubonne.getNoOFS());
					                             Assert.assertEquals(1, forFiscalSecondaires.size());
					                             ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
					                             Assert.assertEquals(date(2015, 7, 4), forFiscalSecondaire.getDateDebut());
					                             Assert.assertEquals(null, forFiscalSecondaire.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Aubonne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
				                             }

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(1, evtsFiscaux.size()); // 1 for secondaire créé

				                             final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
				                             Collections.sort(evtsFiscauxTries, new Comparator<EvenementFiscal>() {
					                             @Override
					                             public int compare(EvenementFiscal o1, EvenementFiscal o2) {
						                             return Long.compare(o1.getId(), o2.getId());
					                             }
				                             });

				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(0);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 7, 4), ef.getDateValeur());

					                             final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
					                             Assert.assertEquals(date(2015, 7, 4), eff.getForFiscal().getDateDebut());
				                             }
*/
			Assert.assertEquals("L'établissement secondaire civil n°103202100 (Synergy Aubonne SA, à Aubonne (VD)) n'est pas une succursale ou est une succursale radiée du RC et ne sera donc pas créé dans Unireg.",
			                    evt.getErreurs().get(2).getMessage());
			return null;
		});
	}

	// SIFISC-19909 - Ne pas créer les fors secondaires s'il n'y a pas de for principal.
	@Test(timeout = 10000L)
	public void testNouvelEtablissementSecondairePasDeForPrincipal() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;
		final Long noEtablissement2 = noEntrepriseCivile + 2000000;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999995");

				MockEtablissementCivil nouveauSiteSecondaire = MockEtablissementCivilFactory.addEtablissement(noEtablissement2, ent, date(2015, 7, 8), null, "Synergy Aubonne SA",
				                                                                                              FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                              MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2015, 7, 4),
				                                                                                              StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
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
			final Etablissement etablissementSecondaire = tiersService.getEtablissementsSecondairesEntreprise(entreprise).get(0).getPayload();
			Assert.assertNotNull(etablissementSecondaire);

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			Assert.assertEquals(0, evtsFiscaux.size()); // Pas de for secondaire créé

			final String message = evt.getErreurs().get(4).getMessage();
			Assert.assertEquals(String.format("Calcul des fors secondaires entreprise n°%s: Impossible de calculer les fors secondaires en l'absence d'un for principal.", FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero())), message);
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testNouvelEtablissementSecondaireForPrincipalCouvrePartiellement() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;
		final Long noEtablissement2 = noEntrepriseCivile + 2000000;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999995");

				MockEtablissementCivil nouveauSiteSecondaire = MockEtablissementCivilFactory.addEtablissement(noEtablissement2, ent, date(2015, 7, 8), null, "Synergy Aubonne SA",
				                                                                                              FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                              MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2015, 7, 4),
				                                                                                              StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
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

			addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, date(2015, 9, 30),
			                MotifFor.DEMENAGEMENT_SIEGE, MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
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
			final Etablissement etablissementSecondaire = tiersService.getEtablissementsSecondairesEntreprise(entreprise).get(0).getPayload();
			Assert.assertNotNull(etablissementSecondaire);

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			Assert.assertEquals(2, evtsFiscaux.size()); // Pas de for secondaire créé

			final String message = evt.getErreurs().get(5).getMessage();
			Assert.assertEquals("Impossible de créer le for: la période de for fiscal secondaire à Aubonne (VD) (ofs: 5422) débutant le 01.10.2015 n'est pas couverte " +
					                    "par un for principal valide. Veuillez créer ou ajuster le for à la main après avoir corrigé le for principal.", message);
			return null;
		});
	}

	/*
		SIFISC-19086 Ne pas créer les succursales HC
	 */
	@Test(timeout = 10000L)
	public void testNouvelEtablissementSecondaireHC() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;
		final Long noEtablissement2 = noEntrepriseCivile + 2000000;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999995");

				MockEtablissementCivil nouveauSiteSecondaire = MockEtablissementCivilFactory.addEtablissement(noEtablissement2, ent, date(2015, 7, 8), null, "Synergy Züri SA",
				                                                                                              FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_HC,
				                                                                                              MockCommune.Zurich.getNoOFS(), StatusInscriptionRC.ACTIF, date(2015, 7, 4),
				                                                                                              StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
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
			Assert.assertEquals(0, tiersService.getEtablissementsSecondairesEntreprise(entreprise).size());

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
			{
				Assert.assertNull(entreprise.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.Aubonne.getNoOFS()));
			}

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			Assert.assertEquals(0, evtsFiscaux.size());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testNouvelESDejaConnu() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;
		final Long noEtablissement2 = noEntrepriseCivile + 2000000;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(),
						                                       StatusInscriptionRC.ACTIF, date(2010, 6, 23),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999995");

				MockEtablissementCivil nouveauSiteSecondaire = MockEtablissementCivilFactory.addEtablissement(noEtablissement2, ent, date(2015, 7, 8), null, "Synergy Aubonne SA",
				                                                                                              FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                              MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2012, 1, 3),
				                                                                                              StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
				addEntreprise(ent);

			}
		});

		// Création de l'entreprise

		final Long etablissement2Id = doInNewTransactionAndSession(status -> {
			Etablissement etablissementSecondaire = addEtablissement();
			etablissementSecondaire.setNumeroEtablissement(noEtablissement2);
			addDomicileEtablissement(etablissementSecondaire, date(2012, 1, 4), null, MockCommune.Aubonne);
			return etablissementSecondaire.getNumero();
		});

		doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);

			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);
			addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

			final Etablissement etablissementSecondaire = (Etablissement) tiersDAO.get(etablissement2Id);
			addActiviteEconomique(entreprise, etablissementSecondaire, date(2012, 1, 4), null, false);

			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			addForSecondaire(entreprise, date(2012, 1, 4), MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
			return entreprise.getNumero();
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
			final Etablissement etablissementSecondaire = tiersService.getEtablissementsSecondairesEntreprise(entreprise).get(0).getPayload();
			Assert.assertNotNull(etablissementSecondaire);

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
			{
				final List<ForFiscalSecondaire> forFiscalSecondaires = entreprise.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.Aubonne.getNoOFS());
				Assert.assertEquals(1, forFiscalSecondaires.size());
				ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
				Assert.assertEquals(date(2012, 1, 4), forFiscalSecondaire.getDateDebut());
				Assert.assertEquals(null, forFiscalSecondaire.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
				Assert.assertEquals(MockCommune.Aubonne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
			}

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			Assert.assertEquals(0, evtsFiscaux.size());

			final List<EvenementEntrepriseErreur> evtErreurs = evt.getErreurs();
			Assert.assertEquals(3, evtErreurs.size());
			Assert.assertEquals(String.format("Nouvel établissement secondaire civil n°%d (Synergy Aubonne SA, à Aubonne (VD), IDE: CHE-999.999.996) déjà connu de Unireg en tant que tiers n°%s. Ne sera pas créé.",
			                                  noEtablissement2, FormatNumeroHelper.numeroCTBToDisplay(etablissement2Id)),
			                    evtErreurs.get(2).getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testDeuxiemeESMemeCommune() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 1012021L;
		final Long noEtablissement = noEntrepriseCivile + 100;
		final Long noEtablissement2 = noEntrepriseCivile + 200;
		final Long noEtablissement3 = noEntrepriseCivile + 300;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999994");

				MockEtablissementCivil nouveauSiteSecondaire = MockEtablissementCivilFactory.addEtablissement(noEtablissement2, ent, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
				                                                                                              FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                              MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
				                                                                                              StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999994");
				MockEtablissementCivil nouveauSiteSecondaire2 = MockEtablissementCivilFactory.addEtablissement(noEtablissement3, ent, date(2015, 7, 8), null, "Synergy Distribution Aubonne SA",
				                                                                                               FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                               MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2015, 7, 5),
				                                                                                               StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
				addEntreprise(ent);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);

			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

			Etablissement etablissementSecondaire = addEtablissement();
			etablissementSecondaire.setNumeroEtablissement(noEtablissement2);

			addActiviteEconomique(entreprise, etablissementSecondaire, date(2010, 6, 24), null, false);

			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			addForSecondaire(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
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
			final Etablissement etablissementSecondaire = tiersService.getEtablissementsSecondairesEntreprise(entreprise).get(0).getPayload();
			Assert.assertNotNull(etablissementSecondaire);

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
			{
				final List<ForFiscalSecondaire> forFiscalSecondaires = entreprise.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.Aubonne.getNoOFS());
				Assert.assertEquals(1, forFiscalSecondaires.size());
				ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
				Assert.assertEquals(date(2010, 6, 24), forFiscalSecondaire.getDateDebut());
				Assert.assertEquals(null, forFiscalSecondaire.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
				Assert.assertEquals(MockCommune.Aubonne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
			}

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			Assert.assertEquals(0, evtsFiscaux.size());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testFinES1DebutES2CommuneForPrincipal() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 1012021L;
		final Long noEtablissement = noEntrepriseCivile + 100;
		final Long noEtablissement2 = noEntrepriseCivile + 200;
		final Long noEtablissement3 = noEntrepriseCivile + 300;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999994");

				MockEtablissementCivil nouveauSiteSecondaire = MockEtablissementCivilFactory.addEtablissement(noEtablissement2, ent, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
				                                                                                              FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                              MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
				                                                                                              StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999995");
				final MockDonneesRC donneesRC = nouveauSiteSecondaire.getDonneesRC();
				donneesRC.changeInscription(date(2015, 7, 8), new InscriptionRC(StatusInscriptionRC.RADIE, null,
				                                                                date(2010, 6, 23), null,
				                                                                date(2010, 6, 23), date(2015, 7, 5)));
				final MockDonneesRegistreIDE donneesRegistreIDE = nouveauSiteSecondaire.getDonneesRegistreIDE();
				donneesRegistreIDE.changeStatus(date(2015, 7, 8), StatusRegistreIDE.RADIE);

				MockEtablissementCivil nouveauSiteSecondaire2 = MockEtablissementCivilFactory.addEtablissement(noEtablissement3, ent, date(2015, 7, 8), null, "Synergy Distribution Lausanne SA",
				                                                                                               FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                               MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2015, 7, 5),
				                                                                                               StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
				addEntreprise(ent);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);

			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

			Etablissement etablissementSecondaire = addEtablissement();
			etablissementSecondaire.setNumeroEtablissement(noEtablissement2);

			addActiviteEconomique(entreprise, etablissementSecondaire, date(2010, 6, 24), null, false);

			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			addForSecondaire(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne,
			                 MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
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
			final Etablissement etablissementSecondaire = tiersService.getEtablissementsSecondairesEntreprise(entreprise).get(0).getPayload();
			Assert.assertNotNull(etablissementSecondaire);
			final Etablissement etablissementSecondaire2 = tiersService.getEtablissementsSecondairesEntreprise(entreprise).get(1).getPayload();
			Assert.assertNotNull(etablissementSecondaire2);

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
			{
				final List<ForFiscalSecondaire> forFiscalSecondaires = entreprise.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.Aubonne.getNoOFS());
				Assert.assertEquals(1, forFiscalSecondaires.size());
				ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
				Assert.assertEquals(date(2010, 6, 24), forFiscalSecondaire.getDateDebut());
				Assert.assertEquals(date(2015, 7, 5), forFiscalSecondaire.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
				Assert.assertEquals(MockCommune.Aubonne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
			}
			{
				final List<ForFiscalSecondaire> forFiscalSecondaires = entreprise.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.Lausanne.getNoOFS());
				Assert.assertEquals(1, forFiscalSecondaires.size());
				ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
				Assert.assertEquals(date(2015, 7, 5), forFiscalSecondaire.getDateDebut());
				Assert.assertEquals(null, forFiscalSecondaire.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
				Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
			}

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			Assert.assertEquals(2, evtsFiscaux.size()); // 1 for secondaire créé, 1 fermé

			final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
			Collections.sort(evtsFiscauxTries, (o1, o2) -> Long.compare(o1.getId(), o2.getId()));

			{
				final EvenementFiscal ef = evtsFiscauxTries.get(0);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 7, 5), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.FERMETURE, eff.getType());
				Assert.assertEquals(date(2010, 6, 24), eff.getForFiscal().getDateDebut());
				Assert.assertEquals(date(2015, 7, 5), eff.getForFiscal().getDateFin());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(1);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 7, 5), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				Assert.assertEquals(date(2015, 7, 5), eff.getForFiscal().getDateDebut());
				Assert.assertEquals(null, eff.getForFiscal().getDateFin());
			}
			return null;
		});
	}

	/**
	 * Simulation d'un hypothétique changement de genre d'impôt de REVENU/FORTUNE à BENEFICE/CAPITAL puis retour à REVENU/FORTUNE
	 */
	@Test(timeout = 10000L)
	public void testDebutES2CommuneForPrincipalChgmtGenreImpot() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 1012021L;
		final Long noEtablissement = noEntrepriseCivile + 100;
		final Long noEtablissement2 = noEntrepriseCivile + 200;
		final Long noEtablissement3 = noEntrepriseCivile + 300;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0103_SOCIETE_NOM_COLLECTIF,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.AUTRE, "CHE999999994");

				MockEtablissementCivil nouveauSiteSecondaire = MockEtablissementCivilFactory.addEtablissement(noEtablissement2, ent, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
				                                                                                              FormeLegale.N_0103_SOCIETE_NOM_COLLECTIF, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                              MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
				                                                                                              StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.AUTRE, "CHE999999995");
				nouveauSiteSecondaire.changeFormeLegale(date(2015, 1, 1), FormeLegale.N_0106_SOCIETE_ANONYME);
				final MockDonneesRegistreIDE donneesRegistreIDE = nouveauSiteSecondaire.getDonneesRegistreIDE();
				donneesRegistreIDE.changeTypeEntreprise(date(2015, 1, 1), TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE);

				MockEtablissementCivil nouveauSiteSecondaire2 = MockEtablissementCivilFactory.addEtablissement(noEtablissement3, ent, date(2015, 7, 8), null, "Synergy Distribution Lausanne SA",
				                                                                                               FormeLegale.N_0103_SOCIETE_NOM_COLLECTIF, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                               MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2015, 7, 5),
				                                                                                               StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.AUTRE, "CHE999999996");
				addEntreprise(ent);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);

			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

			Etablissement etablissementSecondaire = addEtablissement();
			etablissementSecondaire.setNumeroEtablissement(noEtablissement2);

			addActiviteEconomique(entreprise, etablissementSecondaire, date(2010, 6, 24), null, false);

			addRegimeFiscalVD(entreprise, date(2010, 6, 24), date(2014, 12, 31), MockTypeRegimeFiscal.SOCIETE_PERS);
			addRegimeFiscalVD(entreprise, date(2015, 1, 1), date(2015, 7, 7), MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalVD(entreprise, date(2015, 7, 8), null, MockTypeRegimeFiscal.SOCIETE_PERS);
			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			// Ici CHGT_MODE_IMPOSITION utilisé faute de mieux.
			addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, date(2014, 12, 31), MotifFor.CHGT_MODE_IMPOSITION,
			                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.REVENU_FORTUNE);
			addForPrincipal(entreprise, date(2015, 1, 1), MotifFor.DEBUT_EXPLOITATION, date(2015, 7, 7), MotifFor.CHGT_MODE_IMPOSITION,
			                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			addForPrincipal(entreprise, date(2015, 7, 8), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.REVENU_FORTUNE);
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
			final Etablissement etablissementSecondaire = tiersService.getEtablissementsSecondairesEntreprise(entreprise).get(0).getPayload();
			Assert.assertNotNull(etablissementSecondaire);
			final Etablissement etablissementSecondaire2 = tiersService.getEtablissementsSecondairesEntreprise(entreprise).get(1).getPayload();
			Assert.assertNotNull(etablissementSecondaire2);

			{
				final List<ForFiscalSecondaire> forFiscalSecondaires = entreprise.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.Aubonne.getNoOFS());
				Assert.assertEquals(3, forFiscalSecondaires.size());
				{
					ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
					Assert.assertEquals(date(2010, 6, 24), forFiscalSecondaire.getDateDebut());
					Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
					Assert.assertEquals(date(2014, 12, 31), forFiscalSecondaire.getDateFin());
					Assert.assertEquals(MotifFor.CHGT_MODE_IMPOSITION, forFiscalSecondaire.getMotifFermeture());
					Assert.assertEquals(GenreImpot.REVENU_FORTUNE, forFiscalSecondaire.getGenreImpot());
					Assert.assertEquals(MockCommune.Aubonne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
					Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
				}
				{
					ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(1);
					Assert.assertEquals(date(2015, 1, 1), forFiscalSecondaire.getDateDebut());
					Assert.assertEquals(MotifFor.CHGT_MODE_IMPOSITION, forFiscalSecondaire.getMotifOuverture());
					Assert.assertEquals(date(2015, 7, 7), forFiscalSecondaire.getDateFin());
					Assert.assertEquals(MotifFor.CHGT_MODE_IMPOSITION, forFiscalSecondaire.getMotifFermeture());
					Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
					Assert.assertEquals(MockCommune.Aubonne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
					Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
				}
				{
					ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(2);
					Assert.assertEquals(date(2015, 7, 8), forFiscalSecondaire.getDateDebut());
					Assert.assertEquals(MotifFor.CHGT_MODE_IMPOSITION, forFiscalSecondaire.getMotifOuverture());
					Assert.assertEquals(null, forFiscalSecondaire.getDateFin());
					Assert.assertEquals(null, forFiscalSecondaire.getMotifFermeture());
					Assert.assertEquals(GenreImpot.REVENU_FORTUNE, forFiscalSecondaire.getGenreImpot());
					Assert.assertEquals(MockCommune.Aubonne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
					Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
				}
			}
			{
				final List<ForFiscalSecondaire> forFiscalSecondaires = entreprise.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.Lausanne.getNoOFS());
				Assert.assertEquals(2, forFiscalSecondaires.size());
				{
					ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
					Assert.assertEquals(date(2015, 7, 5), forFiscalSecondaire.getDateDebut());
					Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
					Assert.assertEquals(date(2015, 7, 7), forFiscalSecondaire.getDateFin());
					Assert.assertEquals(MotifFor.CHGT_MODE_IMPOSITION, forFiscalSecondaire.getMotifFermeture());
					Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
					Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
					Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
					Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
				}
				{
					ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(1);
					Assert.assertEquals(date(2015, 7, 8), forFiscalSecondaire.getDateDebut());
					Assert.assertEquals(MotifFor.CHGT_MODE_IMPOSITION, forFiscalSecondaire.getMotifOuverture());
					Assert.assertEquals(null, forFiscalSecondaire.getDateFin());
					Assert.assertEquals(null, forFiscalSecondaire.getMotifFermeture());
					Assert.assertEquals(GenreImpot.REVENU_FORTUNE, forFiscalSecondaire.getGenreImpot());
					Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
					Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
				}
			}

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			Assert.assertEquals(8, evtsFiscaux.size()); // 8 for secondaire créé, 6 fermé

			final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
			Collections.sort(evtsFiscauxTries, (o1, o2) -> Long.compare(o1.getId(), o2.getId()));

			{
				final EvenementFiscal ef = evtsFiscauxTries.get(0);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 7, 5), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(1);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 7, 7), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.FERMETURE, eff.getType());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(2);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 7, 8), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(3);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2010, 6, 24), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(4);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2014, 12, 31), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.FERMETURE, eff.getType());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(5);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 1, 1), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(6);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 7, 7), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.FERMETURE, eff.getType());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(7);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 7, 8), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
			}
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testNouveauESAnterieurESCommuneForPrincipal() throws Exception {

		/*
		On pare de la situation résultant du test précédent (testFinES1DebutES2CommuneForPrincipal) et on simule un événement de
		création d'un nouvel établissement secondaire su Lausanne à une date légèrement antérieure. Ceci doit entraîner
		l'annulation du for secondaire Lausanne, qui démarre le 05.07.2015, et son remplacement par un for secondaire commençant
		le 10.06.2015.
		 */

		// Mise en place service mock
		final Long noEntrepriseCivile = 1012021L;
		final Long noEtablissement = noEntrepriseCivile + 100;
		final Long noEtablissement2 = noEntrepriseCivile + 200;
		final Long noEtablissement3 = noEntrepriseCivile + 300;
		final Long noEtablissement4 = noEntrepriseCivile + 400;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999994");

				MockEtablissementCivil nouveauSiteSecondaire2 = MockEtablissementCivilFactory.addEtablissement(noEtablissement2, ent, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
				                                                                                               FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                               MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
				                                                                                               StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999995");
				nouveauSiteSecondaire2.getDonneesRC().changeInscription(date(2015, 7, 10), new InscriptionRC(StatusInscriptionRC.RADIE, null,
				                                                                                             date(2010, 6, 23), null,
				                                                                                             date(2010, 6, 23), date(2015, 7, 8)));
				nouveauSiteSecondaire2.getDonneesRegistreIDE().changeStatus(date(2015, 7, 10), StatusRegistreIDE.RADIE);
				MockEtablissementCivil nouveauSiteSecondaire3 = MockEtablissementCivilFactory.addEtablissement(noEtablissement3, ent, date(2015, 7, 8), null, "Synergy Distribution Lausanne SA",
				                                                                                               FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                               MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2015, 7, 4),
				                                                                                               StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
				MockEtablissementCivil nouveauSiteSecondaire4 = MockEtablissementCivilFactory.addEtablissement(noEtablissement4, ent, date(2015, 6, 13), null, "Synergy Capital Lausanne SA",
				                                                                                               FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                               MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2015, 6, 10),
				                                                                                               StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999997");
				addEntreprise(ent);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);

			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

			Etablissement etablissementSecondaire2 = addEtablissement();
			etablissementSecondaire2.setNumeroEtablissement(noEtablissement2);

			addActiviteEconomique(entreprise, etablissementSecondaire2, date(2010, 6, 24), date(2015, 7, 8), false);

			Etablissement etablissementSecondaire3 = addEtablissement();
			etablissementSecondaire3.setNumeroEtablissement(noEtablissement3);

			addActiviteEconomique(entreprise, etablissementSecondaire3, date(2015, 7, 5), null, false);

			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			addForSecondaire(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, date(2015, 7, 8), MotifFor.FIN_EXPLOITATION, MockCommune.Aubonne,
			                 MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
			addForSecondaire(entreprise, date(2015, 7, 5), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne,
			                 MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
			return entreprise;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2015, 6, 13), A_TRAITER);
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
			final List<DateRanged<Etablissement>> etablissementsSecondairesEntreprise = tiersService.getEtablissementsSecondairesEntreprise(entreprise);
			Collections.sort(etablissementsSecondairesEntreprise, (o1, o2) -> o1.getPayload().getNumero().compareTo(o2.getPayload().getNumero()));

			final Etablissement etablissementSecondaire = etablissementsSecondairesEntreprise.get(0).getPayload();
			Assert.assertNotNull(etablissementSecondaire);
			final Etablissement etablissementSecondaire2 = etablissementsSecondairesEntreprise.get(1).getPayload();
			Assert.assertNotNull(etablissementSecondaire2);
			final Etablissement etablissementSecondaire3 = etablissementsSecondairesEntreprise.get(2).getPayload();
			Assert.assertNotNull(etablissementSecondaire3);

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
			{
				final List<ForFiscalSecondaire> forFiscalSecondaires = entreprise.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.Aubonne.getNoOFS());
				Assert.assertEquals(1, forFiscalSecondaires.size());
				ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
				Assert.assertEquals(date(2010, 6, 24), forFiscalSecondaire.getDateDebut());
				Assert.assertEquals(date(2015, 7, 8), forFiscalSecondaire.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
				Assert.assertEquals(MockCommune.Aubonne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
			}
			{
				final List<ForFiscalSecondaire> forFiscalSecondaires = entreprise.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.Lausanne.getNoOFS());
				Assert.assertEquals(1, forFiscalSecondaires.size());
				ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
				Assert.assertEquals(date(2015, 6, 10), forFiscalSecondaire.getDateDebut());
				Assert.assertEquals(null, forFiscalSecondaire.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
				Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
			}

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			Assert.assertEquals(2, evtsFiscaux.size()); // 1 for secondaire créé, 1 fermé

			final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
			Collections.sort(evtsFiscauxTries, (o1, o2) -> Long.compare(o1.getId(), o2.getId()));

			{
				final EvenementFiscal ef = evtsFiscauxTries.get(0);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 7, 5), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.ANNULATION, eff.getType());
				Assert.assertEquals(date(2015, 7, 5), eff.getForFiscal().getDateDebut());
				Assert.assertEquals(null, eff.getForFiscal().getDateFin());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(1);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 6, 10), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				Assert.assertEquals(date(2015, 6, 10), eff.getForFiscal().getDateDebut());
				Assert.assertEquals(null, eff.getForFiscal().getDateFin());
			}
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testESDemenageNonInscritRC() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 1012021L;
		final Long noEtablissement = noEntrepriseCivile + 100;
		final Long noEtablissement2 = noEntrepriseCivile + 200;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0109_ASSOCIATION,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.ASSOCIATION, "CHE999999996");

				MockEtablissementCivil nouveauSiteSecondaire = MockEtablissementCivilFactory.addEtablissement(noEtablissement2, ent, date(2010, 6, 24), null, "Synergy Conception Aubonne SA",
				                                                                                              FormeLegale.N_0109_ASSOCIATION, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                              MockCommune.Aubonne.getNoOFS(), null, null,
				                                                                                              StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.ASSOCIATION, "CHE999999997");
				nouveauSiteSecondaire.changeDomicile(date(2015, 7, 5), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.CheseauxSurLausanne.getNoOFS());

				addEntreprise(ent);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);

			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

			Etablissement etablissementSecondaire = addEtablissement();
			etablissementSecondaire.setNumeroEtablissement(noEtablissement2);

			addActiviteEconomique(entreprise, etablissementSecondaire, date(2010, 6, 24), null, false);

			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			addForSecondaire(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
			return entreprise;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2015, 7, 5), A_TRAITER);
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
			final Etablissement etablissementSecondaire = tiersService.getEtablissementsSecondairesEntreprise(entreprise).get(0).getPayload();
			Assert.assertNotNull(etablissementSecondaire);

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
			{
				final List<ForFiscalSecondaire> forFiscalSecondaires = entreprise.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.Aubonne.getNoOFS());
				Assert.assertEquals(1, forFiscalSecondaires.size());
				ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
				Assert.assertEquals(date(2010, 6, 24), forFiscalSecondaire.getDateDebut());
				Assert.assertEquals(date(2015, 7, 4), forFiscalSecondaire.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
				Assert.assertEquals(MockCommune.Aubonne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
				Assert.assertEquals(MotifFor.FIN_EXPLOITATION, forFiscalSecondaire.getMotifFermeture());
			}
			{
				final List<ForFiscalSecondaire> forFiscalSecondaires = entreprise.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.CheseauxSurLausanne.getNoOFS());
				Assert.assertEquals(1, forFiscalSecondaires.size());
				ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
				Assert.assertEquals(date(2015, 7, 5), forFiscalSecondaire.getDateDebut());
				Assert.assertEquals(null, forFiscalSecondaire.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
				Assert.assertEquals(MockCommune.CheseauxSurLausanne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
			}

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			Assert.assertEquals(2, evtsFiscaux.size());

			final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
			Collections.sort(evtsFiscauxTries, (o1, o2) -> Long.compare(o1.getId(), o2.getId()));

			{
				final EvenementFiscal ef = evtsFiscauxTries.get(0);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 7, 4), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.FERMETURE, eff.getType());
				Assert.assertEquals(date(2010, 6, 24), eff.getForFiscal().getDateDebut());
				Assert.assertEquals(date(2015, 7, 4), eff.getForFiscal().getDateFin());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(1);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 7, 5), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				Assert.assertEquals(date(2015, 7, 5), eff.getForFiscal().getDateDebut());
				Assert.assertEquals(null, eff.getForFiscal().getDateFin());
			}
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testESDemenageInscritAuRC() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 1012021L;
		final Long noEtablissement = noEntrepriseCivile + 100;
		final Long noEtablissement2 = noEntrepriseCivile + 200;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

				MockEtablissementCivil nouveauSiteSecondaire = MockEtablissementCivilFactory.addEtablissement(noEtablissement2, ent, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
				                                                                                              FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                              MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
				                                                                                              StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999997");
				nouveauSiteSecondaire.changeDomicile(date(2015, 7, 5), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.CheseauxSurLausanne.getNoOFS());
				{
					final PublicationFOSC publicationFOSC = new PublicationFOSC(date(2015, 7, 5), "77777", "Mutation au journal RC.");
					nouveauSiteSecondaire.getDonneesRC().addEntreeJournal(new EntreeJournalRC(EntreeJournalRC.TypeEntree.NORMAL, date(2015, 7, 2), 111111L, publicationFOSC));
				}

				addEntreprise(ent);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);

			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

			Etablissement etablissementSecondaire = addEtablissement();
			etablissementSecondaire.setNumeroEtablissement(noEtablissement2);

			addActiviteEconomique(entreprise, etablissementSecondaire, date(2010, 6, 24), null, false);

			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			addForSecondaire(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
			return entreprise;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2015, 7, 5), A_TRAITER);
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
			final Etablissement etablissementSecondaire = tiersService.getEtablissementsSecondairesEntreprise(entreprise).get(0).getPayload();
			Assert.assertNotNull(etablissementSecondaire);

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
			{
				final List<ForFiscalSecondaire> forFiscalSecondaires = entreprise.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.Aubonne.getNoOFS());
				Assert.assertEquals(1, forFiscalSecondaires.size());
				ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
				Assert.assertEquals(date(2010, 6, 24), forFiscalSecondaire.getDateDebut());
				Assert.assertEquals(date(2015, 7, 1), forFiscalSecondaire.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
				Assert.assertEquals(MockCommune.Aubonne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
				Assert.assertEquals(MotifFor.FIN_EXPLOITATION, forFiscalSecondaire.getMotifFermeture());
			}
			{
				final List<ForFiscalSecondaire> forFiscalSecondaires = entreprise.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.CheseauxSurLausanne.getNoOFS());
				Assert.assertEquals(1, forFiscalSecondaires.size());
				ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
				Assert.assertEquals(date(2015, 7, 2), forFiscalSecondaire.getDateDebut());
				Assert.assertEquals(null, forFiscalSecondaire.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
				Assert.assertEquals(MockCommune.CheseauxSurLausanne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
			}

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			Assert.assertEquals(2, evtsFiscaux.size());

			final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
			Collections.sort(evtsFiscauxTries, (o1, o2) -> Long.compare(o1.getId(), o2.getId()));

			{
				final EvenementFiscal ef = evtsFiscauxTries.get(0);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 7, 1), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.FERMETURE, eff.getType());
				Assert.assertEquals(date(2010, 6, 24), eff.getForFiscal().getDateDebut());
				Assert.assertEquals(date(2015, 7, 1), eff.getForFiscal().getDateFin());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(1);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 7, 2), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				Assert.assertEquals(date(2015, 7, 2), eff.getForFiscal().getDateDebut());
				Assert.assertEquals(null, eff.getForFiscal().getDateFin());
			}
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testESArriveInscritRC() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 1012021L;
		final Long noEtablissement = noEntrepriseCivile + 100;
		final Long noEtablissement2 = noEntrepriseCivile + 200;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                       TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Zurich.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999995");

				MockEtablissementCivil nouveauSiteSecondaire = MockEtablissementCivilFactory.addEtablissement(noEtablissement2, ent, date(2010, 6, 26), null, "Synergy Conception Succursale SA",
				                                                                                              FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_HC,
				                                                                                              MockCommune.Zurich.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
				                                                                                              StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
				nouveauSiteSecondaire.getDonneesRC().changeInscription(date(2015, 7, 5), new InscriptionRC(StatusInscriptionRC.ACTIF, null,
				                                                                                           date(2015, 7, 2), null,
				                                                                                           date(2010, 6, 23), null));
				nouveauSiteSecondaire.changeDomicile(date(2015, 7, 5), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.CheseauxSurLausanne.getNoOFS());
				addEntreprise(ent);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);

			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

			Etablissement etablissementSecondaire = addEtablissement();
			etablissementSecondaire.setNumeroEtablissement(noEtablissement2);

			addActiviteEconomique(entreprise, etablissementSecondaire, date(2010, 6, 24), null, false);

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
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt.getEtat());
			Assert.assertEquals("L'arrivée HC/HS d'une succursale [n°1012221, Synergy Conception Succursale SA, à Cheseaux-sur-Lausanne (VD), IDE: CHE-999.999.996] n'est pas censé se produire.", evt.getErreurs().get(0).getMessage());

/* Code conservé dans l'éventualité que finallement, cette situation se produit bel et bien.

				                             final Entreprise organisation = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());

				                             final Etablissement etablissementPrincipal = tiersService.getEtablissementsPrincipauxEntreprise(organisation).get(0).getPayload();
				                             Assert.assertNotNull(etablissementPrincipal);
				                             final Etablissement etablissementSecondaire = tiersService.getEtablissementsSecondairesEntreprise(organisation).get(0).getPayload();
				                             Assert.assertNotNull(etablissementSecondaire);

				                             {
					                             ForFiscalPrincipal forFiscalPrincipal = organisation.getDernierForFiscalPrincipal();
					                             Assert.assertEquals(date(2010, 6, 24), forFiscalPrincipal.getDateDebut());
					                             Assert.assertEquals(null, forFiscalPrincipal.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipal.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipal.getMotifOuverture());
				                             }
				                             {
					                             final List<ForFiscalSecondaire> forFiscalSecondaires = organisation.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.CheseauxSurLausanne.getNoOFS());
					                             Assert.assertEquals(1, forFiscalSecondaires.size());
					                             ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
					                             Assert.assertEquals(date(2015, 7, 3), forFiscalSecondaire.getDateDebut());
					                             Assert.assertEquals(null, forFiscalSecondaire.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
					                             Assert.assertEquals(MockCommune.CheseauxSurLausanne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());

											     FIXME: Devrait être ARRIVEE_HC
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
				                             }

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(1, evtsFiscaux.size());

				                             final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
				                             Collections.sort(evtsFiscauxTries, new Comparator<EvenementFiscal>() {
					                             @Override
					                             public int compare(EvenementFiscal o1, EvenementFiscal o2) {
						                             return Long.compare(o1.getId(), o2.getId());
					                             }
				                             });

				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(0);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 7, 3), ef.getDateValeur());

					                             final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
					                             Assert.assertEquals(date(2015, 7, 3), eff.getForFiscal().getDateDebut());
					                             Assert.assertEquals(null, eff.getForFiscal().getDateFin());
				                             }
*/

			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testESDepartInscritRC() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 1012021L;
		final Long noEtablissement = noEntrepriseCivile + 100;
		final Long noEtablissement2 = noEntrepriseCivile + 200;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                       TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Zurich.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

				MockEtablissementCivil nouveauSiteSecondaire = MockEtablissementCivilFactory.addEtablissement(noEtablissement2, ent, date(2010, 6, 26), null, "Synergy Conception Succursale SA",
				                                                                                              FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                              MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
				                                                                                              StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999997");
				nouveauSiteSecondaire.getDonneesRC().changeInscription(date(2015, 7, 5), new InscriptionRC(StatusInscriptionRC.ACTIF, null,
				                                                                                           date(2010, 6, 23), date(2015, 7, 2),
				                                                                                           date(2010, 6, 23), null));
				nouveauSiteSecondaire.changeDomicile(date(2015, 7, 5), TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Zurich.getNoOFS());
				addEntreprise(ent);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);

			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

			Etablissement etablissementSecondaire = addEtablissement();
			etablissementSecondaire.setNumeroEtablissement(noEtablissement2);

			addActiviteEconomique(entreprise, etablissementSecondaire, date(2010, 6, 24), null, false);

			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			addForSecondaire(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
			return entreprise;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt.getEtat());
			Assert.assertEquals("Le déménagement HC/HS d'une succursale [n°1012221, Synergy Conception Succursale SA, à Zurich (ZH), IDE: CHE-999.999.997] n'est pas censé se produire.", evt.getErreurs().get(0).getMessage());

/* Code conservé dans l'éventualité que finallement, cette situation se produit bel et bien.
				                             final Entreprise organisation = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());

				                             final Etablissement etablissementPrincipal = tiersService.getEtablissementsPrincipauxEntreprise(organisation).get(0).getPayload();
				                             Assert.assertNotNull(etablissementPrincipal);
				                             final Etablissement etablissementSecondaire = tiersService.getEtablissementsSecondairesEntreprise(organisation).get(0).getPayload();
				                             Assert.assertNotNull(etablissementSecondaire);

				                             {
					                             ForFiscalPrincipal forFiscalPrincipal = organisation.getDernierForFiscalPrincipal();
					                             Assert.assertEquals(date(2010, 6, 24), forFiscalPrincipal.getDateDebut());
					                             Assert.assertEquals(null, forFiscalPrincipal.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipal.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipal.getMotifOuverture());
				                             }
				                             {
					                             final List<ForFiscalSecondaire> forFiscalSecondaires = organisation.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.Aubonne.getNoOFS());
					                             Assert.assertEquals(1, forFiscalSecondaires.size());
					                             ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
					                             Assert.assertEquals(date(2010, 6, 24), forFiscalSecondaire.getDateDebut());
					                             Assert.assertEquals(date(2015, 7, 2), forFiscalSecondaire.getDateFin());
					                             Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
					                             Assert.assertEquals(MockCommune.Aubonne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
					                             Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
					                             Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
					                             Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());

												 FIXME: Devrait être DEPART_HC
					                             Assert.assertEquals(MotifFor.FIN_EXPLOITATION, forFiscalSecondaire.getMotifFermeture());
				                             }


				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(1, evtsFiscaux.size());

				                             final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
				                             Collections.sort(evtsFiscauxTries, new Comparator<EvenementFiscal>() {
					                             @Override
					                             public int compare(EvenementFiscal o1, EvenementFiscal o2) {
						                             return Long.compare(o1.getId(), o2.getId());
					                             }
				                             });

				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(0);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 7, 2), ef.getDateValeur());

					                             final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
					                             Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.FERMETURE, eff.getType());
					                             Assert.assertEquals(date(2010, 6, 24), eff.getForFiscal().getDateDebut());
					                             Assert.assertEquals(date(2015, 7, 4), eff.getForFiscal().getDateFin());
				                             }
*/

			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testDemenageESAnterieurESCommuneForPrincipal() throws Exception {

		/*
		Cette situation ne devrait pas se produire en temps normal dans RCEnt. Elle sert avant tout à éprouver le moteur de calcul
		des fors secondaires.
		 */

		// Mise en place service mock
		final Long noEntrepriseCivile = 1000L;
		final Long noEtablissement = 1001L;
		final Long noEtablissement2 = 1002L;
		final Long noEtablissement3 = 1003L;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

				MockEtablissementCivil nouveauSiteSecondaire2 = MockEtablissementCivilFactory.addEtablissement(noEtablissement2, ent, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
				                                                                                               FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                               MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
				                                                                                               StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999997");
				nouveauSiteSecondaire2.getDonneesRC().changeInscription(date(2015, 7, 10), new InscriptionRC(StatusInscriptionRC.RADIE, null,
				                                                                                             date(2010, 6, 23), null,
				                                                                                             date(2010, 6, 23), date(2015, 7, 8)));
				nouveauSiteSecondaire2.getDonneesRegistreIDE().changeStatus(date(2015, 7, 10), StatusRegistreIDE.RADIE);
				MockEtablissementCivil nouveauSiteSecondaire3 = MockEtablissementCivilFactory.addEtablissement(noEtablissement3, ent, date(2015, 7, 8), null, "Synergy Distribution Lausanne SA",
				                                                                                               FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                               MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2015, 7, 5),
				                                                                                               StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999998");
				nouveauSiteSecondaire2.changeDomicile(date(2015, 6, 10), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS());

				{
					final PublicationFOSC publicationFOSC = new PublicationFOSC(date(2015, 6, 10), "77777", "Mutation au journal RC.");
					nouveauSiteSecondaire2.getDonneesRC().addEntreeJournal(new EntreeJournalRC(EntreeJournalRC.TypeEntree.NORMAL, date(2015, 6, 5), 111111L, publicationFOSC));
				}

				addEntreprise(ent);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);

			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

			Etablissement etablissementSecondaire2 = addEtablissement();
			etablissementSecondaire2.setNumeroEtablissement(noEtablissement2);

			addActiviteEconomique(entreprise, etablissementSecondaire2, date(2010, 6, 24), null, false);

			Etablissement etablissementSecondaire3 = addEtablissement();
			etablissementSecondaire3.setNumeroEtablissement(noEtablissement3);

			addActiviteEconomique(entreprise, etablissementSecondaire3, date(2015, 7, 8), null, false);

			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			addForSecondaire(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne,
			                 MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
			addForSecondaire(entreprise, date(2015, 7, 6), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne,
			                 MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
			return entreprise;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2015, 6, 10), A_TRAITER);
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
			final Etablissement etablissementSecondaire = tiersService.getEtablissementsSecondairesEntreprise(entreprise).get(0).getPayload();
			Assert.assertNotNull(etablissementSecondaire);

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
			{
				final List<ForFiscalSecondaire> forFiscalSecondaires = entreprise.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.Aubonne.getNoOFS());
				Assert.assertEquals(1, forFiscalSecondaires.size());
				ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
				Assert.assertEquals(date(2010, 6, 24), forFiscalSecondaire.getDateDebut());
				Assert.assertEquals(date(2015, 6, 4), forFiscalSecondaire.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
				Assert.assertEquals(MockCommune.Aubonne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
				Assert.assertEquals(MotifFor.FIN_EXPLOITATION, forFiscalSecondaire.getMotifFermeture());
			}
			{
				final List<ForFiscalSecondaire> forFiscalSecondaires = entreprise.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.Lausanne.getNoOFS());
				Assert.assertEquals(1, forFiscalSecondaires.size());
				ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
				Assert.assertEquals(date(2015, 6, 5), forFiscalSecondaire.getDateDebut());
				Assert.assertEquals(null, forFiscalSecondaire.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
				Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
			}

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			Assert.assertEquals(3, evtsFiscaux.size()); // 1 for secondaire créé, 1 fermé

			final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
			Collections.sort(evtsFiscauxTries, (o1, o2) -> Long.compare(o1.getId(), o2.getId()));

			{
				final EvenementFiscal ef = evtsFiscauxTries.get(0);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 7, 6), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.ANNULATION, eff.getType());
				Assert.assertEquals(date(2015, 7, 6), eff.getForFiscal().getDateDebut());
				Assert.assertEquals(null, eff.getForFiscal().getDateFin());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(1);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 6, 4), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.FERMETURE, eff.getType());
				Assert.assertEquals(date(2010, 6, 24), eff.getForFiscal().getDateDebut());
				Assert.assertEquals(date(2015, 6, 4), eff.getForFiscal().getDateFin());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(2);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 6, 5), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				Assert.assertEquals(date(2015, 6, 5), eff.getForFiscal().getDateDebut());
				Assert.assertEquals(null, eff.getForFiscal().getDateFin());
			}
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testDemenageESEnDoubleForAFermerPlusTard() throws Exception {

		/*
		On a deux établissement secondaires sur Aubonne. L'un a fermé en date du 5.7.2015. On recoit un événement pour l'autre en date du 10.6.2015
		pour un déménagement dans un autre commune. Le for secondaire d'Aubonne doit bien etre fermé mais en date du 5.7.2015.

		Cette situation ne devrait pas se produire en temps normal dans RCEnt. Elle sert avant tout à éprouver le moteur de calcul
		des fors secondaires.
		 */

		// Mise en place service mock
		final Long noEntrepriseCivile = 1012021L;
		final Long noEtablissement = noEntrepriseCivile + 100;
		final Long noEtablissement2 = noEntrepriseCivile + 200;
		final Long noEtablissement3 = noEntrepriseCivile + 300;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

				List<EtablissementCivil> etablissementsSecondaires = ent.getEtablissementsSecondaires(date(2010, 6, 26));
				MockEtablissementCivil nouveauSiteSecondaire2 = MockEtablissementCivilFactory.addEtablissement(noEtablissement2, ent, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
				                                                                                               FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                               MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
				                                                                                               StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999997");
				nouveauSiteSecondaire2.getDonneesRC().changeInscription(date(2015, 7, 7), new InscriptionRC(StatusInscriptionRC.RADIE, null,
				                                                                                            date(2010, 6, 23), null,
				                                                                                            date(2010, 6, 23), date(2015, 7, 4)));
				nouveauSiteSecondaire2.getDonneesRegistreIDE().changeStatus(date(2015, 7, 7), StatusRegistreIDE.RADIE);
				etablissementsSecondaires.add(nouveauSiteSecondaire2);
				MockEtablissementCivil nouveauSiteSecondaire3 = MockEtablissementCivilFactory.addEtablissement(noEtablissement3, ent, date(2010, 6, 26), null, "Synergy Distribution Aubonne SA",
				                                                                                               FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                               MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
				                                                                                               StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999998");
				etablissementsSecondaires.add(nouveauSiteSecondaire3);
				nouveauSiteSecondaire3.changeDomicile(date(2015, 6, 10), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Echallens.getNoOFS());
				{
					final PublicationFOSC publicationFOSC = new PublicationFOSC(date(2015, 6, 10), "77777", "Mutation au journal RC.");
					nouveauSiteSecondaire3.getDonneesRC().addEntreeJournal(new EntreeJournalRC(EntreeJournalRC.TypeEntree.NORMAL, date(2015, 6, 5), 111111L, publicationFOSC));
				}


				addEntreprise(ent);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);

			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

			Etablissement etablissementSecondaire2 = addEtablissement();
			etablissementSecondaire2.setNumeroEtablissement(noEtablissement2);

			addActiviteEconomique(entreprise, etablissementSecondaire2, date(2010, 6, 24), date(2015, 7, 4), false);

			Etablissement etablissementSecondaire3 = addEtablissement();
			etablissementSecondaire3.setNumeroEtablissement(noEtablissement3);

			addActiviteEconomique(entreprise, etablissementSecondaire3, date(2010, 6, 24), null, false);

			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			addForSecondaire(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne,
			                 MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
			return entreprise;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2015, 6, 10), A_TRAITER);
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
			final Etablissement etablissementSecondaire = tiersService.getEtablissementsSecondairesEntreprise(entreprise).get(0).getPayload();
			Assert.assertNotNull(etablissementSecondaire);
			final Etablissement etablissementSecondaire2 = tiersService.getEtablissementsSecondairesEntreprise(entreprise).get(1).getPayload();
			Assert.assertNotNull(etablissementSecondaire2);
			final Etablissement etablissementSecondaire3 = tiersService.getEtablissementsSecondairesEntreprise(entreprise).get(1).getPayload();
			Assert.assertNotNull(etablissementSecondaire3);

			{
				ForFiscalPrincipal forFiscalPrincipal = entreprise.getDernierForFiscalPrincipal();
				Assert.assertEquals(date(2010, 6, 24), forFiscalPrincipal.getDateDebut());
				Assert.assertNull(forFiscalPrincipal.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
				Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalPrincipal.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipal.getMotifOuverture());
			}
			{
				final List<ForFiscalSecondaire> forFiscalSecondaires = entreprise.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.Aubonne.getNoOFS());
				Assert.assertEquals(1, forFiscalSecondaires.size());
				ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
				Assert.assertEquals(date(2010, 6, 24), forFiscalSecondaire.getDateDebut());
				Assert.assertEquals(date(2015, 7, 4), forFiscalSecondaire.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
				Assert.assertEquals(MockCommune.Aubonne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
				Assert.assertEquals(MotifFor.FIN_EXPLOITATION, forFiscalSecondaire.getMotifFermeture());
			}
			{
				final List<ForFiscalSecondaire> forFiscalSecondaires = entreprise.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.Echallens.getNoOFS());
				Assert.assertEquals(1, forFiscalSecondaires.size());
				ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
				Assert.assertEquals(date(2015, 6, 5), forFiscalSecondaire.getDateDebut());
				Assert.assertNull(forFiscalSecondaire.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
				Assert.assertEquals(MockCommune.Echallens.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
			}

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			Assert.assertEquals(2, evtsFiscaux.size()); // 1 for secondaire créé, 1 fermé

			final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
			Collections.sort(evtsFiscauxTries, (o1, o2) -> Long.compare(o1.getId(), o2.getId()));

			{
				final EvenementFiscal ef = evtsFiscauxTries.get(0);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 7, 4), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.FERMETURE, eff.getType());
				Assert.assertEquals(date(2010, 6, 24), eff.getForFiscal().getDateDebut());
				Assert.assertEquals(date(2015, 7, 4), eff.getForFiscal().getDateFin());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(1);
				Assert.assertNotNull(ef);
				Assert.assertEquals(EvenementFiscalFor.class, ef.getClass());
				Assert.assertEquals(date(2015, 6, 5), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				Assert.assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				Assert.assertEquals(date(2015, 6, 5), eff.getForFiscal().getDateDebut());
				Assert.assertEquals(null, eff.getForFiscal().getDateFin());
			}
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testNouvelEtablissementSecondaireNonSuccursale() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;
		final Long noEtablissement2 = noEntrepriseCivile + 2000000;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

				/* Ajout d'un établissement non RC sans forme juridique, ce genre d'établissement serait plutôt au REE, mais on ne le supporte pas encore. */
				MockEtablissementCivilFactory.addEtablissement(noEtablissement2, ent, date(2015, 7, 8), null, "Synergy Aubonne SA",
				                                               null, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                               MockCommune.Aubonne.getNoOFS(), null, null,
				                                               StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999997");
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
			Assert.assertTrue(tiersService.getEtablissementsSecondairesEntreprise(entreprise).isEmpty());

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
			Assert.assertTrue(entreprise.getForsFiscauxSecondairesActifsSortedMapped().isEmpty());

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			Assert.assertEquals(0, evtsFiscaux.size()); // aucun for secondaire créé

			Assert.assertEquals(3, evt.getErreurs().size());
			Assert.assertEquals("L'établissement secondaire civil n°103202100 (Synergy Aubonne SA, à Aubonne (VD), IDE: CHE-999.999.997) n'est pas une succursale ou est une succursale radiée du RC et ne sera donc pas créé dans Unireg.",
			                    evt.getErreurs().get(2).getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testNouvelEtablissementSecondaireSuccursaleRadie() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;
		final Long noEtablissement2 = noEntrepriseCivile + 2000000;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

				MockEtablissementCivilFactory.addEtablissement(noEtablissement2, ent, date(2015, 7, 8), null, "Synergy Aubonne SA",
				                                               FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                               MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.RADIE, date(2010, 6, 24),
				                                               StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999997");
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
			Assert.assertTrue(tiersService.getEtablissementsSecondairesEntreprise(entreprise).isEmpty());

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
			Assert.assertTrue(entreprise.getForsFiscauxSecondairesActifsSortedMapped().isEmpty());

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			Assert.assertEquals(0, evtsFiscaux.size()); // aucun for secondaire créé;
			return null;
		});
	}

	/*
		JIRA SIFISC-19165 : cas réel de problème de gestion des fors secondaires.
	 */
	@Test(timeout = 10000L)
	public void testPATPhotogrammetrieChgmtEtab() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101671614L;
		final Long noEtablissement = 101392713L;
		final Long noEtablissement2 = 101043478L;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "PAT Photogrammétrie SA", date(2015, 12, 5), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                       TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Sion.getNoOFS(),
						                                       StatusInscriptionRC.ACTIF, date(1990, 11, 8),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE106065932");

				MockEtablissementCivil etablissementSecondaire = MockEtablissementCivilFactory.addEtablissement(noEtablissement2, ent, date(2015, 12, 5), null, "PAT Photogrammétrie S.A. succursale de Prilly",
				                                                                                                FormeLegale.N_0151_SUCCURSALE_SUISSE_AU_RC, false,
				                                                                                                TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Prilly.getNoOFS(),
				                                                                                                StatusInscriptionRC.ACTIF, date(1992, 6, 15),
				                                                                                                StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE426884544");
				etablissementSecondaire.changeNom(date(2016, 4, 5), "PAT Photogrammétrie SA succursale vaudoise");
				etablissementSecondaire.changeDomicile(date(2016, 4, 5), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.JouxtensMezery.getNoOFS());
				{
					final PublicationFOSC publicationFOSC = new PublicationFOSC(date(2016, 4, 5), "77777", "Mutation au journal RC.");
					etablissementSecondaire.getDonneesRC().addEntreeJournal(new EntreeJournalRC(EntreeJournalRC.TypeEntree.NORMAL, date(2016, 4, 2), 111111L, publicationFOSC));
				}


				addEntreprise(ent);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			// Entreprise et établissement principal VS
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			entreprise.addDonneeCivile(new RaisonSocialeFiscaleEntreprise(date(1990, 11, 9), date(2015, 12, 4), "PAT Photogrammétrie SA"));
			entreprise.addDonneeCivile(new FormeJuridiqueFiscaleEntreprise(date(1990, 11, 9), date(2015, 12, 4), FormeJuridiqueEntreprise.SA));
			entreprise.addDonneeCivile(new CapitalFiscalEntreprise(date(1990, 11, 9), date(2015, 12, 4), new MontantMonetaire(150000L, "CHF")));

			Etablissement ePrincipal = addEtablissement();
			ePrincipal.setNumeroEtablissement(noEtablissement);

			addDomicileEtablissement(ePrincipal, date(1990, 11, 9), date(2008, 12, 17), MockCommune.Conthey);
			addDomicileEtablissement(ePrincipal, date(2008, 12, 18), date(2015, 12, 4), MockCommune.Sion);

			addActiviteEconomique(entreprise, ePrincipal, date(1990, 11, 9), null, true);

			addRegimeFiscalVD(entreprise, date(1990, 11, 9), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(1990, 11, 9), null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			addForPrincipal(entreprise, date(1990, 11, 9), MotifFor.DEBUT_EXPLOITATION, date(2008, 12, 17), MotifFor.DEMENAGEMENT_SIEGE,
			                MockCommune.Conthey.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			addForPrincipal(entreprise, date(2008, 12, 18), MotifFor.DEMENAGEMENT_SIEGE, null, null,
			                MockCommune.Sion.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);

			// Etablissements secondaires VD

			// Dans les données issues de la migration, on a l'établissement qui est doublonné.

			// D'abord l'ancien
			Etablissement eSecondaire = addEtablissement();
			eSecondaire.setRaisonSociale("PAT Photogrammétrie S.A. succursale de Pully");

			addDomicileEtablissement(eSecondaire, date(1992, 6, 15), date(2000, 9, 6), MockCommune.Pully);

			addActiviteEconomique(entreprise, eSecondaire, date(1992, 6, 15), null, false);

			addForSecondaire(entreprise, date(1992, 6, 15), MotifFor.DEBUT_EXPLOITATION, date(2000, 9, 6), MotifFor.FIN_EXPLOITATION,
			                 MockCommune.Pully, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);

			// Ensuite le nouveau. Les gens de l'ACI on créé un doublon lors
			Etablissement eSecondaire2 = addEtablissement();
			eSecondaire2.setNumeroEtablissement(noEtablissement2);
			eSecondaire2.setRaisonSociale("PAT Photogrammétrie S.A. succursale de Prilly");

			addDomicileEtablissement(eSecondaire2, date(2000, 9, 6), date(2015, 12, 4), MockCommune.Prilly);

			addActiviteEconomique(entreprise, eSecondaire2, date(2000, 9, 6), null, false);

			addForSecondaire(entreprise, date(2000, 9, 6), MotifFor.DEBUT_EXPLOITATION,
			                 MockCommune.Prilly, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
			return entreprise.getNumero();
		});

		// Création de l'événement
		final Long noEvenement = 508002L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.IDE_RADIATION, date(2016, 4, 5), A_TRAITER);
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
			Assert.assertEquals(date(1990, 11, 9), etablissementPrincipal.getRapportObjetValidAt(date(1990, 11, 9), TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE).getDateDebut());
			final Etablissement etablissementSecondaire = tiersService.getEtablissementsSecondairesEntreprise(entreprise).get(0).getPayload();
			Assert.assertNotNull(etablissementSecondaire);

			{
				ForFiscalPrincipal forFiscalPrincipal = entreprise.getDernierForFiscalPrincipal();
				Assert.assertEquals(date(2008, 12, 18), forFiscalPrincipal.getDateDebut());
				Assert.assertEquals(null, forFiscalPrincipal.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
				Assert.assertEquals(MockCommune.Sion.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, forFiscalPrincipal.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
				Assert.assertEquals(MotifFor.DEMENAGEMENT_SIEGE, forFiscalPrincipal.getMotifOuverture());
			}
			{
				final Map<Integer, List<ForFiscalSecondaire>> forsFiscauxSecondairesActifsSortedMapped = entreprise.getForsFiscauxSecondairesActifsSortedMapped();

				final List<ForFiscalSecondaire> forFiscalSecondairesPully = forsFiscauxSecondairesActifsSortedMapped.get(MockCommune.Pully.getNoOFS());
				{
					ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondairesPully.get(0);
					Assert.assertEquals(date(1992, 6, 15), forFiscalSecondaire.getDateDebut());
					Assert.assertEquals(date(2000, 9, 6), forFiscalSecondaire.getDateFin());
					Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
					Assert.assertEquals(MockCommune.Pully.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
					Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
					Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
					Assert.assertEquals(MotifFor.FIN_EXPLOITATION, forFiscalSecondaire.getMotifFermeture());
				}
				final List<ForFiscalSecondaire> forFiscalSecondairesPrilly = forsFiscauxSecondairesActifsSortedMapped.get(MockCommune.Prilly.getNoOFS());
				{
					ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondairesPrilly.get(0);
					Assert.assertEquals(date(2000, 9, 6), forFiscalSecondaire.getDateDebut());
					Assert.assertEquals(date(2016, 4, 1), forFiscalSecondaire.getDateFin());
					Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
					Assert.assertEquals(MockCommune.Prilly.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
					Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
					Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
					Assert.assertEquals(MotifFor.FIN_EXPLOITATION, forFiscalSecondaire.getMotifFermeture());
				}
				final List<ForFiscalSecondaire> forFiscalSecondairesJouxtensMezery = forsFiscauxSecondairesActifsSortedMapped.get(MockCommune.JouxtensMezery.getNoOFS());
				{
					ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondairesJouxtensMezery.get(0);
					Assert.assertEquals(date(2016, 4, 2), forFiscalSecondaire.getDateDebut());
					Assert.assertEquals(null, forFiscalSecondaire.getDateFin());
					Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
					Assert.assertEquals(MockCommune.JouxtensMezery.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
					Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
					Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
				}
			}

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			Assert.assertEquals(2, evtsFiscaux.size());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testESRadiationInscritAuRCActiviteDejaFermee() throws Exception {

		/*
			SIFISC-19230: Le rapport peut avoir été fermé dans le cadre du processus complexe "Fin d'activité"
			Ici l'activité à été fermée en date du 2015-2-2
		 */

		// Mise en place service mock
		final Long noEntrepriseCivile = 1012021L;
		final Long noEtablissement = noEntrepriseCivile + 100;
		final Long noEtablissement2 = noEntrepriseCivile + 200;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

				MockEtablissementCivil nouveauSiteSecondaire = MockEtablissementCivilFactory.addEtablissement(noEtablissement2, ent, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
				                                                                                              FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                              MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
				                                                                                              StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999997");

				final InscriptionRC premInscrSec = nouveauSiteSecondaire.getDonneesRC().getInscription(date(2010, 6, 26));
				nouveauSiteSecondaire.getDonneesRC().addInscription(date(2015, 7, 5), null, new InscriptionRC(StatusInscriptionRC.RADIE, null, premInscrSec.getDateInscriptionVD(),
				                                                                                              date(2015, 7, 5), premInscrSec.getDateInscriptionCH(), date(2015, 7, 5)));
				{
					final PublicationFOSC publicationFOSC = new PublicationFOSC(date(2015, 7, 5), "77777", "Radition au journal RC.");
					nouveauSiteSecondaire.getDonneesRC().addEntreeJournal(new EntreeJournalRC(EntreeJournalRC.TypeEntree.NORMAL, date(2015, 7, 2), 111111L, publicationFOSC));
				}
				final MockDonneesRegistreIDE donneesRegistreIDE = nouveauSiteSecondaire.getDonneesRegistreIDE();
				donneesRegistreIDE.addStatus(date(2015, 7, 5), null, StatusRegistreIDE.RADIE);

				addEntreprise(ent);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);

			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

			Etablissement etablissementSecondaire = addEtablissement();
			etablissementSecondaire.setNumeroEtablissement(noEtablissement2);

			addActiviteEconomique(entreprise, etablissementSecondaire, date(2010, 6, 24), date(2015, 2, 1), false);

			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			addForSecondaire(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, date(2015, 2, 1), MotifFor.FIN_EXPLOITATION,
			                 MockCommune.Aubonne, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
			return entreprise;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2015, 7, 5), A_TRAITER);
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
			final Etablissement etablissementSecondaire = tiersService.getEtablissementsSecondairesEntreprise(entreprise).get(0).getPayload();
			Assert.assertNotNull(etablissementSecondaire);

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
			{
				final List<ForFiscalSecondaire> forFiscalSecondaires = entreprise.getForsFiscauxSecondairesActifsSortedMapped().get(MockCommune.Aubonne.getNoOFS());
				Assert.assertEquals(1, forFiscalSecondaires.size());
				ForFiscalSecondaire forFiscalSecondaire = forFiscalSecondaires.get(0);
				Assert.assertEquals(date(2010, 6, 24), forFiscalSecondaire.getDateDebut());
				Assert.assertEquals(date(2015, 2, 1), forFiscalSecondaire.getDateFin());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
				Assert.assertEquals(MockCommune.Aubonne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscalSecondaire.getTypeAutoriteFiscale());
				Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
				Assert.assertEquals(MotifFor.FIN_EXPLOITATION, forFiscalSecondaire.getMotifFermeture());
			}

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			Assert.assertEquals(0, evtsFiscaux.size());

			final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
			Collections.sort(evtsFiscauxTries, (o1, o2) -> Long.compare(o1.getId(), o2.getId()));
			return null;
		});
	}
}
