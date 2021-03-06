package ch.vd.unireg.evenement.entreprise.casmetier.creation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.engine.AbstractEvenementEntrepriseCivileProcessorTest;
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalFor;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalRegimeFiscal;
import ch.vd.unireg.interfaces.entreprise.data.DateRanged;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.MockEntrepriseConnector;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEtablissementCivilFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.tiers.Bouclement;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.tiers.RegimeFiscal;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementEntreprise;

import static ch.vd.unireg.type.EtatEvenementEntreprise.A_TRAITER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Raphaël Marmier, 2015-09-03
 */
public class CreateEntrepriseHorsVDTest extends AbstractEvenementEntrepriseCivileProcessorTest {

	private EvenementFiscalDAO evtFiscalDAO;

	public CreateEntrepriseHorsVDTest() {
		setWantIndexationTiers(true);
	}

	protected boolean buildProcessorOnSetup() {
		return true;
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		evtFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
	}

	@Test(timeout = 10000L)
	public void testCreationHorsVDPM() throws Exception {

		// Mise en place service mock
		final long noEntrepriseCivile = 101202100L;
		final MockEntrepriseCivile ent = MockEntrepriseFactory.createSimpleEntrepriseRC(noEntrepriseCivile, noEntrepriseCivile + 1000000, "Robert Alkan et autres", RegDate.get(2015, 6, 27), null,
		                                                                                FormeLegale.N_0106_SOCIETE_ANONYME, MockCommune.Zurich);
		MockEtablissementCivilFactory.addEtablissement(1012021001234L,
		                                               ent,
		                                               RegDate.get(2015, 6, 27),
		                                               null,
		                                               "Robert Alkan et autres",
		                                               FormeLegale.N_0106_SOCIETE_ANONYME,
		                                               false,
		                                               TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                               MockCommune.Lausanne.getNoOFS(),
		                                               StatusInscriptionRC.ACTIF,
		                                               date(2015, 6, 24),
		                                               StatusRegistreIDE.DEFINITIF,
		                                               TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(ent);
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 27), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());
			assertEquals(2, entreprise.getRegimesFiscaux().size());

			ForFiscalPrincipal forFiscalPrincipal = entreprise.getDernierForFiscalPrincipal();
			assertEquals(RegDate.get(2015, 6, 24), forFiscalPrincipal.getDateDebut());
			assertNull(forFiscalPrincipal.getDateFin());
			assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
			assertEquals(MockCommune.Zurich.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
			assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
			assertNull(forFiscalPrincipal.getMotifOuverture());

			// [SIFISC-30696] la date du premier exercice commercial doit être renseignée automatiquement
			assertEquals(date(2015, 1, 1), entreprise.getDateDebutPremierExerciceCommercial());

			final Bouclement bouclement = entreprise.getBouclements().iterator().next();
			assertEquals(RegDate.get(2015, 6, 24), bouclement.getDateDebut());
			assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
			assertEquals(12, bouclement.getPeriodeMois());

			{
				final List<DateRanged<Etablissement>> etbsPrns = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
				assertEquals(1, etbsPrns.size());
				assertEquals(RegDate.get(2015, 6, 24), etbsPrns.get(0).getDateDebut());
			}
			{
				final List<DateRanged<Etablissement>> etbsSecs = tiersService.getEtablissementsSecondairesEntreprise(entreprise);
				assertEquals(1, etbsSecs.size());
			}

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			assertNotNull(evtsFiscaux);
			assertEquals(4, evtsFiscaux.size());    // 2 pour les régimes fiscaux, un pour le for principal

			// on sait (parce qu'on a regardé...) que l'ordre de création est : d'abord les régimes fiscaux (CH puis VD)
			// puis les fors... donc les IDs sont dans cet ordre
			final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
			evtsFiscauxTries.sort(Comparator.comparingLong(EvenementFiscal::getId));

			{
				final EvenementFiscal ef = evtsFiscauxTries.get(0);
				assertNotNull(ef);
				assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
				assertEquals(date(2015, 6, 24), ef.getDateValeur());

				final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
				assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
				assertEquals(RegimeFiscal.Portee.CH, efrf.getRegimeFiscal().getPortee());
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), efrf.getRegimeFiscal().getCode());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(1);
				assertNotNull(ef);
				assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
				assertEquals(date(2015, 6, 24), ef.getDateValeur());

				final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
				assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
				assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), efrf.getRegimeFiscal().getCode());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(2);
				assertNotNull(ef);
				assertEquals(EvenementFiscalFor.class, ef.getClass());
				assertEquals(date(2015, 6, 24), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				assertTrue(eff.getForFiscal() instanceof ForFiscalPrincipal);
				assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				assertEquals(date(2015, 6, 24), eff.getForFiscal().getDateDebut());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(3);
				assertNotNull(ef);
				assertEquals(EvenementFiscalFor.class, ef.getClass());
				assertEquals(date(2015, 6, 24), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				assertTrue(eff.getForFiscal() instanceof ForFiscalSecondaire);
				assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				assertEquals(date(2015, 6, 24), eff.getForFiscal().getDateDebut());
			}

			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testCreationHorsVDSP() throws Exception {

		// Mise en place service mock
		final long noEntrepriseCivile = 101202100L;
		final MockEntrepriseCivile ent = MockEntrepriseFactory.createSimpleEntrepriseRC(noEntrepriseCivile, noEntrepriseCivile + 1000000, "Robert Alkan et autres fous", RegDate.get(2015, 6, 27), null, FormeLegale.N_0103_SOCIETE_NOM_COLLECTIF,
		                                                                                MockCommune.Zurich);
		MockEtablissementCivilFactory.addEtablissement(1012021001234L,
		                                               ent,
		                                               RegDate.get(2015, 6, 27),
		                                               null,
		                                               "Robert Alkan et autres fous",
		                                               FormeLegale.N_0103_SOCIETE_NOM_COLLECTIF,
		                                               false,
		                                               TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                               MockCommune.Lausanne.getNoOFS(),
		                                               StatusInscriptionRC.ACTIF,
		                                               date(2015, 6, 24),
		                                               StatusRegistreIDE.DEFINITIF,
		                                               TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(ent);
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 27), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.A_VERIFIER, evt.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());
			assertEquals(2, entreprise.getRegimesFiscaux().size());

			ForFiscalPrincipal forFiscalPrincipal = entreprise.getDernierForFiscalPrincipal();
			assertEquals(RegDate.get(2015, 6, 24), forFiscalPrincipal.getDateDebut());
			assertNull(forFiscalPrincipal.getDateFin());
			assertEquals(GenreImpot.REVENU_FORTUNE, forFiscalPrincipal.getGenreImpot());
			assertEquals(MockCommune.Zurich.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
			assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
			assertNull(forFiscalPrincipal.getMotifOuverture());

			{
				final List<DateRanged<Etablissement>> etbsPrns = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
				assertEquals(1, etbsPrns.size());
				assertEquals(RegDate.get(2015, 6, 24), etbsPrns.get(0).getDateDebut());
			}
			{
				final List<DateRanged<Etablissement>> etbsSecs = tiersService.getEtablissementsSecondairesEntreprise(entreprise);
				assertEquals(1, etbsSecs.size());
			}

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			assertNotNull(evtsFiscaux);
			assertEquals(4, evtsFiscaux.size());    // 2 pour les régimes fiscaux, 1 pour le for principal, 1 pour le for secondaire

			// on sait (parce qu'on a regardé...) que l'ordre de création est : d'abord les régimes fiscaux (CH puis VD)
			// puis les fors... donc les IDs sont dans cet ordre
			final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
			evtsFiscauxTries.sort(Comparator.comparingLong(EvenementFiscal::getId));

			{
				final EvenementFiscal ef = evtsFiscauxTries.get(0);
				assertNotNull(ef);
				assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
				assertEquals(date(2015, 6, 24), ef.getDateValeur());

				final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
				assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
				assertEquals(RegimeFiscal.Portee.CH, efrf.getRegimeFiscal().getPortee());
				assertEquals(MockTypeRegimeFiscal.SOCIETE_PERS.getCode(), efrf.getRegimeFiscal().getCode());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(1);
				assertNotNull(ef);
				assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
				assertEquals(date(2015, 6, 24), ef.getDateValeur());

				final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
				assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
				assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
				assertEquals(MockTypeRegimeFiscal.SOCIETE_PERS.getCode(), efrf.getRegimeFiscal().getCode());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(2);
				assertNotNull(ef);
				assertEquals(EvenementFiscalFor.class, ef.getClass());
				assertEquals(date(2015, 6, 24), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				assertTrue(eff.getForFiscal() instanceof ForFiscalPrincipal);
				assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				assertEquals(date(2015, 6, 24), eff.getForFiscal().getDateDebut());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(3);
				assertNotNull(ef);
				assertEquals(EvenementFiscalFor.class, ef.getClass());
				assertEquals(date(2015, 6, 24), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				assertTrue(eff.getForFiscal() instanceof ForFiscalSecondaire);
				assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				assertEquals(date(2015, 6, 24), eff.getForFiscal().getDateDebut());
			}

			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testCreationPasSurVDDuTout() throws Exception {

		// Mise en place service mock
		final long noEntrepriseCivile = 101202100L;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(
						MockEntrepriseFactory.createSimpleEntrepriseRC(noEntrepriseCivile, noEntrepriseCivile + 1000000, "Robert Alkan et autres", RegDate.get(2015, 6, 24), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                               MockCommune.Zurich));
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 24), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

			assertNull(tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile()));

			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testCreationHorsVDPMNouvelEtablissementVD() throws Exception {

		// Mise en place service mock
		final long noEntrepriseCivile = 101202100L;
		final MockEntrepriseCivile ent = MockEntrepriseFactory.createSimpleEntrepriseRC(noEntrepriseCivile, noEntrepriseCivile + 1000000, "Robert Alkan et autres", RegDate.get(1920, 1, 5), null, FormeLegale.N_0106_SOCIETE_ANONYME,
		                                                                                MockCommune.Zurich);
		MockEtablissementCivilFactory.addEtablissement(1012021001234L,
		                                               ent,
		                                               RegDate.get(2015, 6, 27),
		                                               null,
		                                               "Robert Alkan et autres",
		                                               FormeLegale.N_0106_SOCIETE_ANONYME,
		                                               false,
		                                               TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                               MockCommune.Lausanne.getNoOFS(),
		                                               StatusInscriptionRC.ACTIF,
		                                               date(2015, 6, 24),
		                                               StatusRegistreIDE.DEFINITIF,
		                                               TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(ent);
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, RegDate.get(2015, 6, 27), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());
			assertEquals(2, entreprise.getRegimesFiscaux().size());

			ForFiscalPrincipal forFiscalPrincipal = entreprise.getDernierForFiscalPrincipal();
			assertEquals(RegDate.get(2015, 6, 24), forFiscalPrincipal.getDateDebut());
			assertNull(forFiscalPrincipal.getDateFin());
			assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
			assertEquals(MockCommune.Zurich.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
			assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
			assertNull(forFiscalPrincipal.getMotifOuverture());

			final Bouclement bouclement = entreprise.getBouclements().iterator().next();
			assertEquals(RegDate.get(2015, 12, 1), bouclement.getDateDebut());
			assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
			assertEquals(12, bouclement.getPeriodeMois());

			assertEquals(date(2015, 1, 1), entreprise.getDateDebutPremierExerciceCommercial());

			{
				final List<DateRanged<Etablissement>> etbsPrns = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
				assertEquals(1, etbsPrns.size());
				assertEquals(RegDate.get(2015, 6, 24), etbsPrns.get(0).getDateDebut());
			}
			{
				final List<DateRanged<Etablissement>> etbsSecs = tiersService.getEtablissementsSecondairesEntreprise(entreprise);
				assertEquals(1, etbsSecs.size());
			}

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			assertNotNull(evtsFiscaux);
			assertEquals(4, evtsFiscaux.size());    // 2 pour les régimes fiscaux, un pour le for principal

			// on sait (parce qu'on a regardé...) que l'ordre de création est : d'abord les régimes fiscaux (CH puis VD)
			// puis les fors... donc les IDs sont dans cet ordre
			final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
			evtsFiscauxTries.sort(Comparator.comparingLong(EvenementFiscal::getId));

			{
				final EvenementFiscal ef = evtsFiscauxTries.get(0);
				assertNotNull(ef);
				assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
				assertEquals(date(2015, 6, 24), ef.getDateValeur());

				final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
				assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
				assertEquals(RegimeFiscal.Portee.CH, efrf.getRegimeFiscal().getPortee());
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), efrf.getRegimeFiscal().getCode());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(1);
				assertNotNull(ef);
				assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
				assertEquals(date(2015, 6, 24), ef.getDateValeur());

				final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
				assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
				assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), efrf.getRegimeFiscal().getCode());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(2);
				assertNotNull(ef);
				assertEquals(EvenementFiscalFor.class, ef.getClass());
				assertEquals(date(2015, 6, 24), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				assertTrue(eff.getForFiscal() instanceof ForFiscalPrincipal);
				assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				assertEquals(date(2015, 6, 24), eff.getForFiscal().getDateDebut());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(3);
				assertNotNull(ef);
				assertEquals(EvenementFiscalFor.class, ef.getClass());
				assertEquals(date(2015, 6, 24), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				assertTrue(eff.getForFiscal() instanceof ForFiscalSecondaire);
				assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				assertEquals(date(2015, 6, 24), eff.getForFiscal().getDateDebut());
			}

			return null;
		});
	}
}
