package ch.vd.unireg.evenement.entreprise.casmetier.creation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.engine.AbstractEvenementEntrepriseCivileProcessorTest;
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalFor;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalRegimeFiscal;
import ch.vd.unireg.interfaces.entreprise.data.DateRanged;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.InscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.MockEntrepriseConnector;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockDonneesRC;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockDonneesRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.tiers.Bouclement;
import ch.vd.unireg.tiers.DomicileEtablissement;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPM;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.tiers.FormeJuridiqueFiscaleEntreprise;
import ch.vd.unireg.tiers.RaisonSocialeFiscaleEntreprise;
import ch.vd.unireg.tiers.RegimeFiscal;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEtatEntreprise;
import ch.vd.unireg.type.TypeEvenementEntreprise;
import ch.vd.unireg.type.TypeGenerationEtatEntreprise;

import static ch.vd.unireg.type.EtatEvenementEntreprise.A_TRAITER;
import static ch.vd.unireg.type.EtatEvenementEntreprise.FORCE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Raphaël Marmier, 2015-11-xx
 */
public class CreateEntrepriseIgnoreIDETest extends AbstractEvenementEntrepriseCivileProcessorTest {

	private EvenementFiscalDAO evtFiscalDAO;

	public CreateEntrepriseIgnoreIDETest() {
		setWantIndexationTiers(true);
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		evtFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
	}

	protected boolean buildProcessorOnSetup() {
		return true;
	}

	@Test(timeout = 10000L)
	public void testCreationPMInscriptionIDE() throws Exception {

		// Mise en place service mock
		final long noEntrepriseCivile = 101202100L;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(
						MockEntrepriseFactory.createSimpleEntrepriseRC(noEntrepriseCivile, noEntrepriseCivile + 1000000, "Synergy SA", RegDate.get(2015, 6, 27), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                               MockCommune.Lausanne));
			}
		});

		// Création de l'événement

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event1 = createEvent(1111L, noEntrepriseCivile, TypeEvenementEntreprise.IDE_NOUVELLE_INSCRIPTION, RegDate.get(2015, 6, 27), A_TRAITER);
			final EvenementEntreprise event2 = createEvent(2222L, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 27), A_TRAITER);
			hibernateTemplate.merge(event1);
			hibernateTemplate.merge(event2);
			return null;
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(1111L);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

			assertEquals("L'événement pour l'entreprise civile n°" + noEntrepriseCivile + " précède un événement FOSC d'inscription RC VD sans historique avant ce jour au civil. Ignoré.",
			             evt.getErreurs().get(1).getMessage());

			final EvenementEntreprise evt2 = getUniqueEvent(2222L);
			assertNotNull(evt2);
			assertEquals(EtatEvenementEntreprise.TRAITE, evt2.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());
			assertNotNull(entreprise);

			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testCreationPMInscriptionIDEApresFOSC() throws Exception {

		/*
			Ne pas ignorer l'événement IDE reçu après la FOSC pour le même jour.
		 */
		// Mise en place service mock
		final long noEntrepriseCivile = 101202100L;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(
						MockEntrepriseFactory.createSimpleEntrepriseRC(noEntrepriseCivile, noEntrepriseCivile + 1000000, "Synergy SA", RegDate.get(2015, 6, 27), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                               MockCommune.Lausanne));
			}
		});

		// Création de l'événement

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event1 = createEvent(1111L, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 27), A_TRAITER);
			final EvenementEntreprise event2 = createEvent(2222L, noEntrepriseCivile, TypeEvenementEntreprise.IDE_NOUVELLE_INSCRIPTION, RegDate.get(2015, 6, 27), A_TRAITER);
			final EvenementEntreprise event3 = createEvent(3333L, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, RegDate.get(2015, 6, 27), A_TRAITER);
			hibernateTemplate.merge(event1);
			hibernateTemplate.merge(event2);
			hibernateTemplate.merge(event3);
			return null;
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(1111L);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

			assertEquals("Mutation : Création d'une entreprise vaudoise",
			             evt.getErreurs().get(1).getMessage());

			final EvenementEntreprise evt2 = getUniqueEvent(2222L);
			assertNotNull(evt2);
			assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt2.getEtat());

			assertEquals("L'entreprise civile n°101202100 est déjà connue d'Unireg, mais nouvelle au civil. Veuillez vérifier la transition entre les données du registre fiscal et du registre civil, notamment les établissements secondaires.",
			             evt2.getErreurs().get(1).getMessage());
			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());
			assertNotNull(entreprise);

			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testCreationPMFOSCPourDateUlterieure() throws Exception {

		/*
			Ici l'événement FOSC est valable pour une date ultérieur et ne doit pas compter. On ne peut ignorer l'événement IDE.
		 */

		// Mise en place service mock
		final long noEntrepriseCivile = 101202100L;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(
						MockEntrepriseFactory.createSimpleEntrepriseRC(noEntrepriseCivile, noEntrepriseCivile + 1000000, "Synergy SA", RegDate.get(2015, 6, 27), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                               MockCommune.Lausanne));
			}
		});

		// Création de l'événement

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event1 = createEvent(1111L, noEntrepriseCivile, TypeEvenementEntreprise.IDE_NOUVELLE_INSCRIPTION, RegDate.get(2015, 6, 27), A_TRAITER);
			final EvenementEntreprise event2 = createEvent(2222L, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 28), A_TRAITER);
			hibernateTemplate.merge(event1);
			hibernateTemplate.merge(event2);
			return null;
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(1111L);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

			assertEquals("Mutation : Création d'une entreprise vaudoise",
			             evt.getErreurs().get(1).getMessage());

			final EvenementEntreprise evt2 = getUniqueEvent(2222L);
			assertNotNull(evt2);
			assertEquals(EtatEvenementEntreprise.TRAITE, evt2.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());
			assertNotNull(entreprise);

			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testCreationPMFOSCPasInscription() throws Exception {

		/*
			SIFISC-23174: Ne pas être trop laxiste lorsqu'il s'agit d'ignorer les événements en vertu du SIFISC-22016 / SIFISC-21128.
			Ici l'événement FOSC n'est pas une nouvelle inscription et ne doit pas compter. On ne peut ignorer l'événement IDE.
		 */

		// Mise en place service mock
		final long noEntrepriseCivile = 101202100L;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(
						MockEntrepriseFactory.createSimpleEntrepriseRC(noEntrepriseCivile, noEntrepriseCivile + 1000000, "Synergy SA", RegDate.get(2015, 6, 27), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                               MockCommune.Lausanne));
			}
		});

		// Création de l'événement

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event1 = createEvent(1111L, noEntrepriseCivile, TypeEvenementEntreprise.IDE_NOUVELLE_INSCRIPTION, RegDate.get(2015, 6, 27), A_TRAITER);
			final EvenementEntreprise event2 = createEvent(2222L, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, RegDate.get(2015, 6, 27), A_TRAITER);
			hibernateTemplate.merge(event1);
			hibernateTemplate.merge(event2);
			return null;
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(1111L);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

			assertEquals("Mutation : Création d'une entreprise vaudoise",
			             evt.getErreurs().get(1).getMessage());

			final EvenementEntreprise evt2 = getUniqueEvent(2222L);
			assertNotNull(evt2);
			assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt2.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());
			assertNotNull(entreprise);

			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testCreationPMEvtPourDateAnterieurAnomalieRCEnt() throws Exception {

		/*
			On a un événement pour une date antérieure. Situation totalement anormale du côté de RCEnt.
		 */

		// Mise en place service mock
		final long noEntrepriseCivile = 101202100L;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(
						MockEntrepriseFactory.createSimpleEntrepriseRC(noEntrepriseCivile, noEntrepriseCivile + 1000000, "Synergy SA", RegDate.get(2015, 6, 27), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                               MockCommune.Lausanne));
			}
		});

		// Création de l'événement

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event0 = createEvent(1000L, noEntrepriseCivile, TypeEvenementEntreprise.IMPORTATION_ENTREPRISE, RegDate.get(2015, 6, 26), FORCE);
			final EvenementEntreprise event1 = createEvent(1111L, noEntrepriseCivile, TypeEvenementEntreprise.IDE_NOUVELLE_INSCRIPTION, RegDate.get(2015, 6, 27), A_TRAITER);
			final EvenementEntreprise event2 = createEvent(2222L, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 27), A_TRAITER);
			hibernateTemplate.merge(event0);
			hibernateTemplate.merge(event1);
			hibernateTemplate.merge(event2);
			return null;
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(1111L);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

			assertEquals("Mutation : Création d'une entreprise vaudoise",
			             evt.getErreurs().get(1).getMessage());

			final EvenementEntreprise evt2 = getUniqueEvent(2222L);
			assertNotNull(evt2);
			assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt2.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());
			assertNotNull(entreprise);

			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testCreationPMHCAvecEtablissementSecondaireVD() throws Exception {

		/*
			Vérifier qu'on n'ignore pas pour un etab. principal HC
		 */

		// Mise en place service mock
		final long noEntrepriseCivile = 101202100L;
		final long noEtablissementPrincipal = noEntrepriseCivile + 1000000;
		final long noEtablissementSecondaire = 9999999L;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final MockEntrepriseCivile entrepriseAvecEtablissementSecondaire =
						MockEntrepriseFactory.createEntrepriseAvecEtablissementSecondaire(noEntrepriseCivile, noEtablissementPrincipal, noEtablissementSecondaire, "trc SA", RegDate.get(2015, 6, 26), null,
						                                                                  FormeLegale.N_0109_ASSOCIATION,
						                                                                  TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Zurich.getNoOFS(),
						                                                                  TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Aubonne.getNoOFS(),
						                                                                  StatusInscriptionRC.ACTIF, date(2015, 1, 26), StatusInscriptionRC.ACTIF, date(2015, 6, 26),
						                                                                  StatusRegistreIDE.DEFINITIF, StatusRegistreIDE.DEFINITIF,
						                                                                  TypeEntrepriseRegistreIDE.ASSOCIATION, TypeEntrepriseRegistreIDE.SITE, "CHE999999996", "CHE999999997");
				addEntreprise(
						entrepriseAvecEtablissementSecondaire);
			}
		});

		// Création de l'événement

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event1 = createEvent(1111L, noEntrepriseCivile, TypeEvenementEntreprise.IDE_NOUVELLE_INSCRIPTION, RegDate.get(2015, 6, 26), A_TRAITER);
			final EvenementEntreprise event2 = createEvent(2222L, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_SUCCURSALE, RegDate.get(2015, 6, 26), A_TRAITER);
			hibernateTemplate.merge(event1);
			hibernateTemplate.merge(event2);
			return null;
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(1111L);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());
			assertEquals(TypeEtatEntreprise.INSCRITE_RC, entreprise.getEtatActuel().getType());
			final List<RegimeFiscal> regimesFiscaux = new ArrayList<>(entreprise.getRegimesFiscaux());
			assertEquals(4, regimesFiscaux.size());
			regimesFiscaux.sort(new DateRangeComparator<RegimeFiscal>().thenComparing(RegimeFiscal::getPortee));
			assertRegimeFiscal(RegDate.get(2015, 6, 26), RegDate.get(2017, 12, 31), RegimeFiscal.Portee.VD, "70", regimesFiscaux.get(0));
			assertRegimeFiscal(RegDate.get(2015, 6, 26), RegDate.get(2017, 12, 31), RegimeFiscal.Portee.CH, "70", regimesFiscaux.get(1));
			assertRegimeFiscal(RegDate.get(2018, 1, 1), null, RegimeFiscal.Portee.VD, "703", regimesFiscaux.get(2));
			assertRegimeFiscal(RegDate.get(2018, 1, 1), null, RegimeFiscal.Portee.CH, "703", regimesFiscaux.get(3));

			ForFiscalPrincipal forFiscalPrincipal = entreprise.getForsFiscauxPrincipauxActifsSorted().get(0);
			assertEquals(RegDate.get(2015, 6, 26), forFiscalPrincipal.getDateDebut());
			assertNull(forFiscalPrincipal.getDateFin());
			assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
			assertEquals(MockCommune.Zurich.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
			assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
			assertNull(forFiscalPrincipal.getMotifOuverture());

			final List<ForFiscal> forsFiscauxSorted = entreprise.getForsFiscauxSorted();
			for (ForFiscal forFiscal : forsFiscauxSorted) {
				if (forFiscal instanceof ForFiscalSecondaire) {
					ForFiscalSecondaire forFiscalSecondaire = (ForFiscalSecondaire) forFiscal;
					assertEquals(RegDate.get(2015, 6, 26), forFiscalSecondaire.getDateDebut());
					assertNull(forFiscalSecondaire.getDateFin());
					assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
					assertEquals(MockCommune.Aubonne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
					assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
					assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
					break;
				}
			}

			final Bouclement bouclement = entreprise.getBouclements().iterator().next();
			assertEquals(RegDate.get(2015, 12, 1), bouclement.getDateDebut());
			assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
			assertEquals(12, bouclement.getPeriodeMois());

			{
				final List<DateRanged<Etablissement>> etsbPrns = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
				assertEquals(1, etsbPrns.size());
				assertEquals(RegDate.get(2015, 6, 26), etsbPrns.get(0).getDateDebut());
			}
			{
				final List<DateRanged<Etablissement>> etbsSecs = tiersService.getEtablissementsSecondairesEntreprise(entreprise);
				assertEquals(1, etbsSecs.size());
				assertEquals(RegDate.get(2015, 6, 26), etbsSecs.get(0).getDateDebut());
			}

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			assertNotNull(evtsFiscaux);
			assertEquals(8, evtsFiscaux.size());    // 2 pour les régimes fiscaux, un pour le for principal

			// on sait (parce qu'on a regardé...) que l'ordre de création est : d'abord les régimes fiscaux (CH puis VD)
			// puis les fors... donc les IDs sont dans cet ordre
			final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
			evtsFiscauxTries.sort(Comparator.comparingLong(EvenementFiscal::getId));

			{
				final EvenementFiscal ef = evtsFiscauxTries.get(0);
				assertNotNull(ef);
				assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
				assertEquals(date(2015, 6, 26), ef.getDateValeur());

				final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
				assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
				assertEquals(RegimeFiscal.Portee.CH, efrf.getRegimeFiscal().getPortee());
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_APM.getCode(), efrf.getRegimeFiscal().getCode());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(1);
				assertNotNull(ef);
				assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
				assertEquals(date(2017, 12, 31), ef.getDateValeur());

				final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
				assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.FERMETURE, efrf.getType());
				assertEquals(RegimeFiscal.Portee.CH, efrf.getRegimeFiscal().getPortee());
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_APM.getCode(), efrf.getRegimeFiscal().getCode());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(2);
				assertNotNull(ef);
				assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
				assertEquals(date(2015, 6, 26), ef.getDateValeur());

				final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
				assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
				assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_APM.getCode(), efrf.getRegimeFiscal().getCode());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(3);
				assertNotNull(ef);
				assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
				assertEquals(date(2017, 12, 31), ef.getDateValeur());

				final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
				assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.FERMETURE, efrf.getType());
				assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_APM.getCode(), efrf.getRegimeFiscal().getCode());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(4);
				assertNotNull(ef);
				assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
				assertEquals(date(2018, 1, 1), ef.getDateValeur());

				final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
				assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
				assertEquals(RegimeFiscal.Portee.CH, efrf.getRegimeFiscal().getPortee());
				assertEquals(MockTypeRegimeFiscal.SBI.getCode(), efrf.getRegimeFiscal().getCode());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(5);
				assertNotNull(ef);
				assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
				assertEquals(date(2018, 1, 1), ef.getDateValeur());

				final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
				assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
				assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
				assertEquals(MockTypeRegimeFiscal.SBI.getCode(), efrf.getRegimeFiscal().getCode());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(6);
				assertNotNull(ef);
				assertEquals(EvenementFiscalFor.class, ef.getClass());
				assertEquals(date(2015, 6, 26), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				assertTrue(((EvenementFiscalFor) ef).getForFiscal() instanceof ForFiscalPrincipalPM);
				assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				assertEquals(date(2015, 6, 26), eff.getForFiscal().getDateDebut());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(7);
				assertNotNull(ef);
				assertEquals(EvenementFiscalFor.class, ef.getClass());
				assertEquals(date(2015, 6, 26), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				assertTrue(((EvenementFiscalFor) ef).getForFiscal() instanceof ForFiscalSecondaire);
				assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				assertEquals(date(2015, 6, 26), eff.getForFiscal().getDateDebut());
			}

			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testRadiationEvtIDENonIgnore() throws Exception {

		/*
			On n'ignore que les evt IDE inscription et mutation. Attention, le mock ne colle pas tout à fait à la réalité. (il a une date d'inscription RC VD à se fondation...)
			Mais ici ce qui compte, c'est qu'on ignore l'événement IDE
		 */

		// Mise en place service mock
		final long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final RegDate dateInscription = date(2010, 6, 24);
				final RegDate dateRadiation = date(2015, 7, 2);
				final MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, dateInscription,
						                                       StatusRegistreIDE.DEFINITIF,
						                                       TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				final MockDonneesRC rc = (MockDonneesRC) entreprise.getEtablissements().get(0).getDonneesRC();
				rc.changeInscription(date(2015, 7, 5), new InscriptionRC(StatusInscriptionRC.RADIE, null,
				                                                         dateInscription, dateRadiation,
				                                                         dateInscription, dateRadiation));
				final MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) entreprise.getEtablissements().get(0).getDonneesRegistreIDE();
				donneesRegistreIDE.changeStatus(date(2015, 7, 5), StatusRegistreIDE.RADIE);
				addEntreprise(entreprise);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(transactionStatus -> {

			final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			addEtatEntreprise(entreprise, date(2010, 6, 24), TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.AUTOMATIQUE);
			addEtatEntreprise(entreprise, date(2013, 1, 1), TypeEtatEntreprise.EN_LIQUIDATION, TypeGenerationEtatEntreprise.AUTOMATIQUE);
			return entreprise;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.IDE_RADIATION, date(2015, 7, 5), A_TRAITER);
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
			assertEquals(TypeEtatEntreprise.RADIEE_RC, entreprise.getEtatActuel().getType());

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			assertNotNull(evtsFiscaux);
			assertEquals(0, evtsFiscaux.size());

			assertEquals("L'entreprise a été radiée du registre du commerce.",
			             evt.getErreurs().get(3).getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testCreationPMAvecSiteSecondaire() throws Exception {

		/*
			Doit être ignoré la même chose qu'une création simple
		 */

		// Mise en place service mock
		final long noEntrepriseCivile = 101202100L;
		final long noEtablissementPrincipal = noEntrepriseCivile + 1000000;
		final long noEtablissementSecondaire = 9999999L;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final MockEntrepriseCivile entrepriseAvecSiteSecondaire =
						MockEntrepriseFactory.createEntrepriseAvecEtablissementSecondaire(noEntrepriseCivile, noEtablissementPrincipal, noEtablissementSecondaire, "Synergy SA", RegDate.get(2015, 6, 26), null,
						                                                                  FormeLegale.N_0106_SOCIETE_ANONYME,
						                                                                  TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(),
						                                                                  TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Aubonne.getNoOFS(),
						                                                                  StatusInscriptionRC.ACTIF, date(2015, 6, 24), StatusInscriptionRC.ACTIF, date(2015, 6, 24),
						                                                                  StatusRegistreIDE.DEFINITIF, StatusRegistreIDE.DEFINITIF,
						                                                                  TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999995", "CHE999999996");
				addEntreprise(entrepriseAvecSiteSecondaire);
			}
		});

		// Création de l'événement

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event1 = createEvent(1111L, noEntrepriseCivile, TypeEvenementEntreprise.IDE_NOUVELLE_INSCRIPTION, RegDate.get(2015, 6, 26), A_TRAITER);
			final EvenementEntreprise event2 = createEvent(2222L, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 26), A_TRAITER);
			hibernateTemplate.merge(event1);
			hibernateTemplate.merge(event2);
			return null;
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(1111L);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

			assertEquals("L'événement pour l'entreprise civile n°" + noEntrepriseCivile + " précède un événement FOSC d'inscription RC VD sans historique avant ce jour au civil. Ignoré.",
			             evt.getErreurs().get(1).getMessage());

			final EvenementEntreprise evt2 = getUniqueEvent(2222L);
			assertNotNull(evt2);
			assertEquals(EtatEvenementEntreprise.TRAITE, evt2.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());
			assertNotNull(entreprise);

			return null;
		});
	}


	@Test(timeout = 10000L)
	public void testArriveePM() throws Exception {

		/*
			Une arrivée classique. Attention, le mock ne colle pas tout à fait à la réalité. (il a une date d'inscription RC VD à se fondation...)
			Mais ici ce qui compte, c'est qu'on ignore l'événement IDE
		 */

		// Mise en place service mock
		final long noEntrepriseCivile = 101202100L;

		final MockEntrepriseCivile ent = MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEntrepriseCivile + 1000000, "Synergy SA", RegDate.get(2015, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
		                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
		                                                                        StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
		final MockDonneesRC rc = (MockDonneesRC) ent.getEtablissements().get(0).getDonneesRC();
		rc.changeInscription(date(2015, 6, 26), new InscriptionRC(StatusInscriptionRC.ACTIF, null,
		                                                          date(2015, 6, 20), null,
		                                                          date(2015, 6, 24), null));

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(ent);

			}
		});

		// Création de l'événement

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event1 = createEvent(1111L, noEntrepriseCivile, TypeEvenementEntreprise.IDE_MUTATION, RegDate.get(2015, 6, 26), A_TRAITER);
			final EvenementEntreprise event2 = createEvent(2222L, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 26), A_TRAITER);
			hibernateTemplate.merge(event1);
			hibernateTemplate.merge(event2);
			return null;
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(1111L);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

			assertEquals("L'événement pour l'entreprise civile n°" + noEntrepriseCivile + " précède un événement FOSC d'inscription RC VD sans historique avant ce jour au civil. Ignoré.",
			             evt.getErreurs().get(1).getMessage());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());
			assertNotNull(entreprise);

			final EvenementEntreprise evt2 = getUniqueEvent(2222L);
			assertNotNull(evt2);
			assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

			return null;
		});
	}

	@Test(timeout = 1000000L)
	public void testArriveePMArriveeLongtempsAvant() throws Exception {

		/*
			Ca doit sauter mais pas ici. Lors du traitement de l'événement FOSC.
		 */

		// Mise en place service mock
		final long noEntrepriseCivile = 101202100L;

		final MockEntrepriseCivile ent = MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEntrepriseCivile + 1000000, "Synergy SA", RegDate.get(2015, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
		                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
		                                                                        StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");
		final MockDonneesRC rc = (MockDonneesRC) ent.getEtablissements().get(0).getDonneesRC();
		rc.changeInscription(date(2012, 4, 16), new InscriptionRC(StatusInscriptionRC.ACTIF, null,
		                                                          date(2012, 4, 12), null,
		                                                          date(2010, 6, 24), null));

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(ent);

			}
		});

		// Création de l'événement

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event1 = createEvent(1111L, noEntrepriseCivile, TypeEvenementEntreprise.IDE_NOUVELLE_INSCRIPTION, RegDate.get(2015, 6, 26), A_TRAITER);
			final EvenementEntreprise event2 = createEvent(2222L, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 26), A_TRAITER);
			hibernateTemplate.merge(event1);
			hibernateTemplate.merge(event2);
			return null;
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(1111L);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());
			assertNull(entreprise);

			assertEquals("L'événement pour l'entreprise civile n°" + noEntrepriseCivile + " précède un événement FOSC d'inscription RC VD sans historique avant ce jour au civil. Ignoré.",
			             evt.getErreurs().get(1).getMessage());

			return null;
		});
	}

	// Accepter la création d'une entreprise que si l'entreprise VD n'est pas connue de RCEnt depuis plus de 15 jours. Ici avec événement IDE.
	@Test(timeout = 10000L)
	public void testEntrepriseVDDejaConnueRCEntMaisRecent() throws Exception {

		/*
			On a déjà de l'historique la veille. On n'ignore pas.
		 */

		// Mise en place service mock
		final long noEntrepriseCivile = 101202100L;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(
						MockEntrepriseFactory.createSimpleEntrepriseRC(noEntrepriseCivile, noEntrepriseCivile + 1000000, "Synergy SA", RegDate.get(2015, 6, 27), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                               MockCommune.Lausanne));
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.IDE_NOUVELLE_INSCRIPTION, RegDate.get(2015, 7, 5), A_TRAITER);
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
			assertEquals(TypeEtatEntreprise.INSCRITE_RC, entreprise.getEtatActuel().getType());
			assertEquals(2, entreprise.getRegimesFiscaux().size());

			final RaisonSocialeFiscaleEntreprise surchargeRaisonSocialeFiscale = entreprise.getRaisonsSocialesNonAnnuleesTriees().get(0);
			assertEquals(date(2015, 6, 24), surchargeRaisonSocialeFiscale.getDateDebut());
			assertEquals(date(2015, 7, 4), surchargeRaisonSocialeFiscale.getDateFin());
			assertEquals("Synergy SA", surchargeRaisonSocialeFiscale.getRaisonSociale());

			final FormeJuridiqueFiscaleEntreprise surchargeFormeJuridiqueFiscale = entreprise.getFormesJuridiquesNonAnnuleesTriees().get(0);
			assertEquals(date(2015, 6, 24), surchargeFormeJuridiqueFiscale.getDateDebut());
			assertEquals(date(2015, 7, 4), surchargeFormeJuridiqueFiscale.getDateFin());
			assertEquals(FormeJuridiqueEntreprise.SA, surchargeFormeJuridiqueFiscale.getFormeJuridique());

			assertTrue(entreprise.getCapitauxNonAnnulesTries().isEmpty());

			ForFiscalPrincipal forFiscalPrincipal = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(RegDate.get(2015, 6, 25)).get(0);
			assertEquals(RegDate.get(2015, 6, 25), forFiscalPrincipal.getDateDebut());
			assertNull(forFiscalPrincipal.getDateFin());
			assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
			assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
			assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
			assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipal.getMotifOuverture());

			final Bouclement bouclement = entreprise.getBouclements().iterator().next();
			assertEquals(RegDate.get(2015, 6, 25), bouclement.getDateDebut());
			assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
			assertEquals(12, bouclement.getPeriodeMois());

			{
				final List<DateRanged<Etablissement>> etsbPrns = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
				assertEquals(1, etsbPrns.size());
				final DateRanged<Etablissement> etablissementPrincipalRange = etsbPrns.get(0);
				assertEquals(RegDate.get(2015, 6, 24), etablissementPrincipalRange.getDateDebut());
				final DomicileEtablissement surchargeDomicileEtablissement = etablissementPrincipalRange.getPayload().getSortedDomiciles(false).get(0);
				assertEquals(date(2015, 6, 24), surchargeDomicileEtablissement.getDateDebut());
				assertEquals(date(2015, 7, 4), surchargeDomicileEtablissement.getDateFin());
				assertEquals(MockCommune.Lausanne.getNoOFS(), surchargeDomicileEtablissement.getNumeroOfsAutoriteFiscale().intValue());
			}
			{
				final List<DateRanged<Etablissement>> etbsSecs = tiersService.getEtablissementsSecondairesEntreprise(entreprise);
				assertEquals(0, etbsSecs.size());
			}

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			assertNotNull(evtsFiscaux);
			assertEquals(3, evtsFiscaux.size());    // 2 pour les régimes fiscaux, un pour le for principal

			// on sait (parce qu'on a regardé...) que l'ordre de création est : d'abord les régimes fiscaux (CH puis VD)
			// puis les fors... donc les IDs sont dans cet ordre
			final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
			evtsFiscauxTries.sort(Comparator.comparingLong(EvenementFiscal::getId));

			{
				final EvenementFiscal ef = evtsFiscauxTries.get(0);
				assertNotNull(ef);
				assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
				assertEquals(date(2015, 6, 25), ef.getDateValeur());

				final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
				assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
				assertEquals(RegimeFiscal.Portee.CH, efrf.getRegimeFiscal().getPortee());
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), efrf.getRegimeFiscal().getCode());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(1);
				assertNotNull(ef);
				assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
				assertEquals(date(2015, 6, 25), ef.getDateValeur());

				final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
				assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
				assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), efrf.getRegimeFiscal().getCode());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(2);
				assertNotNull(ef);
				assertEquals(EvenementFiscalFor.class, ef.getClass());
				assertEquals(date(2015, 6, 25), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				assertEquals(date(2015, 6, 25), eff.getForFiscal().getDateDebut());
			}

			return null;
		});
	}
}
