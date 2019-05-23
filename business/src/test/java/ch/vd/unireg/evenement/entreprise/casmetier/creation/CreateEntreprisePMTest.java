package ch.vd.unireg.evenement.entreprise.casmetier.creation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.engine.AbstractEvenementEntrepriseCivileProcessorTest;
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalFor;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalRegimeFiscal;
import ch.vd.unireg.interfaces.entreprise.data.DateRanged;
import ch.vd.unireg.interfaces.entreprise.data.EntreeJournalRC;
import ch.vd.unireg.interfaces.entreprise.data.EtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.InscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.PublicationFOSC;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.MockEntrepriseConnector;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockDonneesRC;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEtablissementCivilFactory;
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

import static ch.vd.unireg.type.EtatEvenementEntreprise.A_TRAITER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Raphaël Marmier, 2015-09-03
 */
public class CreateEntreprisePMTest extends AbstractEvenementEntrepriseCivileProcessorTest {

	private EvenementFiscalDAO evtFiscalDAO;

	public CreateEntreprisePMTest() {
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
	public void testCreationPM() throws Exception {

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
			assertEquals(TypeEtatEntreprise.INSCRITE_RC, entreprise.getEtatActuel().getType());

			final Set<RegimeFiscal> regimesFiscaux = entreprise.getRegimesFiscaux();
			assertEquals(2, regimesFiscaux.size());
			final RegimeFiscal regimeFiscal1 = regimesFiscaux.iterator().next();
			assertNotNull(regimeFiscal1);
			assertEquals(RegDate.get(2015, 6, 25), regimeFiscal1.getDateDebut());
			assertNull(regimeFiscal1.getDateFin());
			assertEquals("01", regimeFiscal1.getCode());

			final RaisonSocialeFiscaleEntreprise surchargeRaisonSocialeFiscale = entreprise.getRaisonsSocialesNonAnnuleesTriees().get(0);
			assertEquals(date(2015, 6, 24), surchargeRaisonSocialeFiscale.getDateDebut());
			assertEquals(date(2015, 6, 26), surchargeRaisonSocialeFiscale.getDateFin());
			assertEquals("Synergy SA", surchargeRaisonSocialeFiscale.getRaisonSociale());

			final FormeJuridiqueFiscaleEntreprise surchargeFormeJuridiqueFiscale = entreprise.getFormesJuridiquesNonAnnuleesTriees().get(0);
			assertEquals(date(2015, 6, 24), surchargeFormeJuridiqueFiscale.getDateDebut());
			assertEquals(date(2015, 6, 26), surchargeFormeJuridiqueFiscale.getDateFin());
			assertEquals(FormeJuridiqueEntreprise.SA, surchargeFormeJuridiqueFiscale.getFormeJuridique());

			assertTrue(entreprise.getCapitauxNonAnnulesTries().isEmpty());

			ForFiscalPrincipal forFiscalPrincipal = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(RegDate.get(2015, 6, 25)).get(0);
			assertEquals(RegDate.get(2015, 6, 25), forFiscalPrincipal.getDateDebut());
			assertNull(forFiscalPrincipal.getDateFin());
			assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
			assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
			assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
			assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipal.getMotifOuverture());

			// [SIFISC-30696] la date du premier exercice commercial doit être renseignée automatiquement
			assertEquals(date(2015, 1, 1), entreprise.getDateDebutPremierExerciceCommercial());

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
				assertEquals(date(2015, 6, 26), surchargeDomicileEtablissement.getDateFin());
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

	@Test(timeout = 10000L)
	public void testCreationPMAvecSiteSecondaire() throws Exception {

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
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 26), A_TRAITER);
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

			ForFiscalPrincipal forFiscalPrincipal = entreprise.getForsFiscauxPrincipauxActifsSorted().get(0);
			assertEquals(RegDate.get(2015, 6, 25), forFiscalPrincipal.getDateDebut());
			assertNull(forFiscalPrincipal.getDateFin());
			assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
			assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
			assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
			assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipal.getMotifOuverture());

			final List<ForFiscalSecondaire> forsFiscauxSecondairesSorted = entreprise.getForsParType(true).secondaires;
			{
				ForFiscalSecondaire forFiscalSecondaire = forsFiscauxSecondairesSorted.get(0);
				assertEquals(RegDate.get(2015, 6, 25), forFiscalSecondaire.getDateDebut());
				assertNull(forFiscalSecondaire.getDateFin());
				assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
				assertEquals(MockCommune.Aubonne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
				assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
				assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
			}

			final Bouclement bouclement = entreprise.getBouclements().iterator().next();
			assertEquals(RegDate.get(2015, 6, 25), bouclement.getDateDebut());
			assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
			assertEquals(12, bouclement.getPeriodeMois());

			{
				final List<DateRanged<Etablissement>> etsbPrns = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
				assertEquals(1, etsbPrns.size());
				assertEquals(RegDate.get(2015, 6, 24), etsbPrns.get(0).getDateDebut());
			}
			{
				final List<DateRanged<Etablissement>> etbsSecs = tiersService.getEtablissementsSecondairesEntreprise(entreprise);
				assertEquals(1, etbsSecs.size());
				assertEquals(RegDate.get(2015, 6, 24), etbsSecs.get(0).getDateDebut());
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
				assertTrue(((EvenementFiscalFor) ef).getForFiscal() instanceof ForFiscalPrincipalPM);
				assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				assertEquals(date(2015, 6, 25), eff.getForFiscal().getDateDebut());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(3);
				assertNotNull(ef);
				assertEquals(EvenementFiscalFor.class, ef.getClass());
				assertEquals(date(2015, 6, 25), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				assertTrue(((EvenementFiscalFor) ef).getForFiscal() instanceof ForFiscalSecondaire);
				assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				assertEquals(date(2015, 6, 25), eff.getForFiscal().getDateDebut());
			}

			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testCreationPMHCAvecSiteSecondaireVD() throws Exception {

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
						                                                                  TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Zurich.getNoOFS(),
						                                                                  TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Aubonne.getNoOFS(),
						                                                                  StatusInscriptionRC.ACTIF, date(2015, 1, 24), StatusInscriptionRC.ACTIF, date(2015, 6, 24),
						                                                                  StatusRegistreIDE.DEFINITIF, StatusRegistreIDE.DEFINITIF,
						                                                                  TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999995", "CHE999999996");
				addEntreprise(entrepriseAvecSiteSecondaire);
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_SUCCURSALE, RegDate.get(2015, 6, 26), A_TRAITER);
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

			ForFiscalPrincipal forFiscalPrincipal = entreprise.getForsFiscauxPrincipauxActifsSorted().get(0);
			assertEquals(RegDate.get(2015, 6, 24), forFiscalPrincipal.getDateDebut());
			assertNull(forFiscalPrincipal.getDateFin());
			assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
			assertEquals(MockCommune.Zurich.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
			assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
			assertNull(forFiscalPrincipal.getMotifOuverture());

			final List<ForFiscalSecondaire> forsFiscauxSecondairesSorted = entreprise.getForsParType(true).secondaires;
			{
				ForFiscalSecondaire forFiscalSecondaire = forsFiscauxSecondairesSorted.get(0);
				assertEquals(RegDate.get(2015, 6, 24), forFiscalSecondaire.getDateDebut());
				assertNull(forFiscalSecondaire.getDateFin());
				assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
				assertEquals(MockCommune.Aubonne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
				assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
				assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
			}

			final Bouclement bouclement = entreprise.getBouclements().iterator().next();
			assertEquals(RegDate.get(2015, 12, 1), bouclement.getDateDebut());
			assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
			assertEquals(12, bouclement.getPeriodeMois());

			{
				final List<DateRanged<Etablissement>> etsbPrns = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
				assertEquals(1, etsbPrns.size());
				assertEquals(RegDate.get(2015, 6, 24), etsbPrns.get(0).getDateDebut());
			}
			{
				final List<DateRanged<Etablissement>> etbsSecs = tiersService.getEtablissementsSecondairesEntreprise(entreprise);
				assertEquals(1, etbsSecs.size());
				assertEquals(RegDate.get(2015, 6, 24), etbsSecs.get(0).getDateDebut());
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
				assertTrue(((EvenementFiscalFor) ef).getForFiscal() instanceof ForFiscalPrincipalPM);
				assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				assertEquals(date(2015, 6, 24), eff.getForFiscal().getDateDebut());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(3);
				assertNotNull(ef);
				assertEquals(EvenementFiscalFor.class, ef.getClass());
				assertEquals(date(2015, 6, 24), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				assertTrue(((EvenementFiscalFor) ef).getForFiscal() instanceof ForFiscalSecondaire);
				assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				assertEquals(date(2015, 6, 24), eff.getForFiscal().getDateDebut());
			}

			return null;
		});
	}

	@Ignore // Cas non permis actuellement car on ne tolère que les établissements inscrits au RC afin d'ignorer ceux du REE.
	@Test(timeout = 10000L)
	public void testCreationPMHCNonRCAvecSiteSecondaireVD() throws Exception {

		// Mise en place service mock
		final long noEntrepriseCivile = 101202100L;
		final long noEtablissementPrincipal = noEntrepriseCivile + 1000000;
		final long noEtablissementSecondaire = 9999999L;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final MockEntrepriseCivile entrepriseAvecSiteSecondaire =
						MockEntrepriseFactory.createEntrepriseAvecEtablissementSecondaire(noEntrepriseCivile, noEtablissementPrincipal, noEtablissementSecondaire, "SiAsso", RegDate.get(2015, 6, 26), null,
						                                                                  FormeLegale.N_0109_ASSOCIATION,
						                                                                  TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Zurich.getNoOFS(),
						                                                                  TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Aubonne.getNoOFS(),
						                                                                  null, null, null, null,
						                                                                  StatusRegistreIDE.DEFINITIF, StatusRegistreIDE.DEFINITIF,
						                                                                  TypeEntrepriseRegistreIDE.ASSOCIATION, TypeEntrepriseRegistreIDE.SITE, "CHE999999996", "CHE999999997");
				addEntreprise(entrepriseAvecSiteSecondaire);
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.IDE_NOUVELLE_INSCRIPTION, RegDate.get(2015, 6, 26), A_TRAITER);
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
			assertEquals(TypeEtatEntreprise.FONDEE, entreprise.getEtatActuel().getType());
			assertEquals(2, entreprise.getRegimesFiscaux().size());

			ForFiscalPrincipal forFiscalPrincipal = entreprise.getForsFiscauxPrincipauxActifsSorted().get(0);
			assertEquals(RegDate.get(2015, 6, 26), forFiscalPrincipal.getDateDebut());
			assertNull(forFiscalPrincipal.getDateFin());
			assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
			assertEquals(MockCommune.Zurich.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
			assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
			assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipal.getMotifOuverture());

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
			assertEquals(RegDate.get(2015, 6, 26), bouclement.getDateDebut());
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
			assertEquals(4, evtsFiscaux.size());    // 2 pour les régimes fiscaux, un pour le for principal

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
				assertEquals(date(2015, 6, 26), ef.getDateValeur());

				final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
				assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
				assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_APM.getCode(), efrf.getRegimeFiscal().getCode());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(2);
				assertNotNull(ef);
				assertEquals(EvenementFiscalFor.class, ef.getClass());
				assertEquals(date(2015, 6, 26), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				assertTrue(((EvenementFiscalFor) ef).getForFiscal() instanceof ForFiscalPrincipalPM);
				assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				assertEquals(date(2015, 6, 26), eff.getForFiscal().getDateDebut());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(3);
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
	public void testCreationPMAvecSitesSecondairesSurMemeCommune() throws Exception {

		// Mise en place service mock
		final long noEntrepriseCivile = 101202100L;
		final long noEtablissementPrincipal = noEntrepriseCivile + 1000000;
		final long noEtablissementSecondaire1 = 9999998L;
		final long noEtablissementSecondaire2 = 9999999L;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntrepriseAvecEtablissementSecondaire(noEntrepriseCivile, noEtablissementPrincipal, noEtablissementSecondaire1, "Synergy SA", RegDate.get(2015, 6, 26), null,
						                                                                  FormeLegale.N_0106_SOCIETE_ANONYME,
						                                                                  TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(),
						                                                                  TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(),
						                                                                  StatusInscriptionRC.ACTIF, date(2015, 6, 24), StatusInscriptionRC.ACTIF, date(2015, 6, 24),
						                                                                  StatusRegistreIDE.DEFINITIF, StatusRegistreIDE.DEFINITIF,
						                                                                  TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", "CHE999999996");
				MockEtablissementCivilFactory.addEtablissement(noEtablissementSecondaire2, entreprise, RegDate.get(2015, 6, 26), null, "Synergy Plus Plus SA", FormeLegale.N_0106_SOCIETE_ANONYME,
				                                               false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(),
				                                               StatusInscriptionRC.ACTIF, date(2015, 6, 24), StatusRegistreIDE.DEFINITIF,
				                                               TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999997", null, null);
				addEntreprise(entreprise);
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 26), A_TRAITER);
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

			final List<ForFiscal> forsFiscauxSorted = entreprise.getForsFiscauxSorted();
			for (ForFiscal forFiscal : forsFiscauxSorted) {
				if (forFiscal instanceof ForFiscalSecondaire) {
					ForFiscalSecondaire forFiscalSecondaire = (ForFiscalSecondaire) forFiscal;
					assertEquals(RegDate.get(2015, 6, 25), forFiscalSecondaire.getDateDebut());
					assertNull(forFiscalSecondaire.getDateFin());
					assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalSecondaire.getGenreImpot());
					assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalSecondaire.getNumeroOfsAutoriteFiscale().intValue());
					assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, forFiscalSecondaire.getMotifRattachement());
					assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalSecondaire.getMotifOuverture());
					break;
				}
			}

			final Bouclement bouclement = entreprise.getBouclements().iterator().next();
			assertEquals(RegDate.get(2015, 6, 25), bouclement.getDateDebut());
			assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
			assertEquals(12, bouclement.getPeriodeMois());

			{
				final List<DateRanged<Etablissement>> etsbPrns = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
				assertEquals(1, etsbPrns.size());
				assertEquals(RegDate.get(2015, 6, 24), etsbPrns.get(0).getDateDebut());
			}
			{
				final List<DateRanged<Etablissement>> etbsSecs = tiersService.getEtablissementsSecondairesEntreprise(entreprise);
				assertEquals(2, etbsSecs.size());
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
				assertTrue(((EvenementFiscalFor) ef).getForFiscal() instanceof ForFiscalPrincipalPM);
				assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				assertEquals(date(2015, 6, 25), eff.getForFiscal().getDateDebut());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(3);
				assertNotNull(ef);
				assertEquals(EvenementFiscalFor.class, ef.getClass());
				assertEquals(date(2015, 6, 25), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				assertTrue(((EvenementFiscalFor) ef).getForFiscal() instanceof ForFiscalSecondaire);
				assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				assertEquals(date(2015, 6, 25), eff.getForFiscal().getDateDebut());
			}

			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testArriveePM() throws Exception {

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
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 26), A_TRAITER);
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

			assertEquals(date(2015, 1, 1), entreprise.getDateDebutPremierExerciceCommercial());

			ForFiscalPrincipal forFiscalPrincipal = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(date(2015, 6, 20)).get(0);
			assertEquals(date(2015, 6, 20), forFiscalPrincipal.getDateDebut());

			assertNull(forFiscalPrincipal.getDateFin());
			assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
			assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
			assertEquals(MotifFor.ARRIVEE_HC, forFiscalPrincipal.getMotifOuverture());

			final Bouclement bouclement = entreprise.getBouclements().iterator().next();

			assertEquals(date(2015, 12, 1), bouclement.getDateDebut());
			assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
			assertEquals(12, bouclement.getPeriodeMois());

			assertEquals(date(2015, 1, 1), entreprise.getDateDebutPremierExerciceCommercial());

			{
				final List<DateRanged<Etablissement>> etbsPrns = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
				assertEquals(1, etbsPrns.size());
				assertEquals(date(2015, 6, 20), etbsPrns.get(0).getDateDebut());
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
				assertEquals(date(2015, 6, 20), ef.getDateValeur());

				final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
				assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
				assertEquals(RegimeFiscal.Portee.CH, efrf.getRegimeFiscal().getPortee());
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), efrf.getRegimeFiscal().getCode());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(1);
				assertNotNull(ef);
				assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
				assertEquals(date(2015, 6, 20), ef.getDateValeur());

				final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
				assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
				assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
				assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), efrf.getRegimeFiscal().getCode());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(2);
				assertNotNull(ef);
				assertEquals(EvenementFiscalFor.class, ef.getClass());
				assertEquals(date(2015, 6, 20), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				assertEquals(date(2015, 6, 20), eff.getForFiscal().getDateDebut());
			}

			return null;
		});
	}

	@Test(timeout = 1000000L)
	public void testArriveePMArriveeLongtempsAvant() throws Exception {

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
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 26), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt.getEtat());

			assertEquals(
					"Refus de créer dans Unireg une entreprise dont la fondation remonte à 24.06.2010, 1828 jours avant la date de l'événement. La tolérance étant de 15 jours. Il y a probablement une erreur d'identification ou un problème de date.",
					evt.getErreurs().get(0).getMessage());

			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testCreationPMCommuneFaitiere() throws Exception {

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

			ForFiscalPrincipal forFiscalPrincipal = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(RegDate.get(2015, 6, 25)).get(0);
			assertEquals(RegDate.get(2015, 6, 25), forFiscalPrincipal.getDateDebut());
			assertNull(forFiscalPrincipal.getDateFin());
			assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
			assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());

			{
				final List<DateRanged<Etablissement>> etbsPrns = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
				assertEquals(1, etbsPrns.size());
				assertEquals(RegDate.get(2015, 6, 24), etbsPrns.get(0).getDateDebut());
			}
			{
				final List<DateRanged<Etablissement>> etbsSecs = tiersService.getEtablissementsSecondairesEntreprise(entreprise);
				assertEquals(0, etbsSecs.size());
			}

			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testCreationPMNonRC() throws Exception {

		// Mise en place service mock
		final long noEntrepriseCivile = 101202100L;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEntrepriseCivile + 1000000, "Synergy SA", RegDate.get(2015, 6, 24), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), null, null, StatusRegistreIDE.DEFINITIF,
						                                       TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996"));
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
			assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt.getEtat());

			assertNull(tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile()));
			return null;
		});
	}

	// SIFISC-19332 - accepter la création d'une entreprise que si l'entreprise VD n'est pas connue de RCEnt depuis plus de 15 jours.
	@Test(timeout = 10000L)
	public void testEntrepriseVDDejaConnueRCEntNonIdentifiee() throws Exception {

		// Mise en place service mock
		final long noEntrepriseCivile = 101202100L;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(
						MockEntrepriseFactory.createSimpleEntrepriseRC(noEntrepriseCivile, noEntrepriseCivile + 1000000, "Synergy SA", RegDate.get(2015, 12, 5), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                               MockCommune.Lausanne));
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2016, 5, 23), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt.getEtat());

			assertNull(tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile()));

			assertEquals(2, evt.getErreurs().size());
			assertEquals(
					String.format("L'entreprise civile n°%s est présente sur Vaud (Lausanne (VD)) depuis plus de 15 jours et devrait être déjà connue d'Unireg. L'identification n'a probablement pas fonctionné. Veuillez traiter le cas à la main.",
					              noEntrepriseCivile),
					evt.getErreurs().get(1).getMessage());

			return null;
		});
	}

	// SIFISC-19471 - Ne pas vérifier la présence antérieur d'une entreprise PP.
	@Test(timeout = 10000L)
	public void testEntrepriseVDDejaConnueRCEntNonIdentifieePP() throws Exception {

		// Mise en place service mock
		final long noEntrepriseCivile = 101202100L;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(
						MockEntrepriseFactory.createSimpleEntrepriseRC(noEntrepriseCivile, noEntrepriseCivile + 1000000, "Synergy Jean Pierre", RegDate.get(2015, 12, 5), null, FormeLegale.N_0101_ENTREPRISE_INDIVIDUELLE,
						                                               MockCommune.Lausanne));
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2016, 5, 23), A_TRAITER);
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

			assertEquals(2, evt.getErreurs().size());
			assertEquals(String.format("L'entreprise civile n°%s est une entreprise individuelle vaudoise. Pas de création.",
			                           noEntrepriseCivile),
			             evt.getErreurs().get(1).getMessage());

			return null;
		});
	}

	// SIFISC-19332 - accepter la création d'une entreprise que si l'entreprise VD n'est pas connue de RCEnt depuis plus de 15 jours.
	@Test(timeout = 10000L)
	public void testEntrepriseVDDejaConnueRCEntMaisRecent() throws Exception {

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
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 7, 5), A_TRAITER);
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

	@Test(timeout = 10000L)
	public void testCreationDonneesRCInvalides() throws Exception {

		// Mise en place service mock
		final long noEntrepriseCivile = 101202100L;
		final RegDate datePublication = date(2015, 6, 27);
		final RegDate dateMauvaiseInscriptionRC = date(2015, 6, 24);
		final RegDate dateEntreeJournal = date(2015, 6, 20);

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {

				final MockEntrepriseCivile simpleEntrepriseRC =
						MockEntrepriseFactory.createSimpleEntrepriseRC(noEntrepriseCivile, noEntrepriseCivile + 1000000, "Synergy SA", datePublication, null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                               MockCommune.Lausanne);
				addEntreprise(
						simpleEntrepriseRC);
				final EtablissementCivil etablissementCivil = simpleEntrepriseRC.getEtablissements().get(0);
				final MockDonneesRC donneesRC = (MockDonneesRC) etablissementCivil.getDonneesRC();
				donneesRC.addEntreeJournal(new EntreeJournalRC(EntreeJournalRC.TypeEntree.NORMAL, dateEntreeJournal, 123456L, new PublicationFOSC(datePublication, "998877", "Nouvelle entreprise blah blah")));
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
			assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt.getEtat());

			assertEquals(String.format("La date d'inscription au RC (%s) de l'entreprise Synergy SA (civil: %d) diffère de la date de l'entrée de journal au RC (%s)! (Possible problème de transcription au RC) Impossible de continuer.",
			                           RegDateHelper.dateToDisplayString(dateMauvaiseInscriptionRC),
			                           noEntrepriseCivile,
			                           RegDateHelper.dateToDisplayString(dateEntreeJournal)),
			             evt.getErreurs().get(2).getMessage());

			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testCreationPMFailCommuneExistePasEncore() throws Exception {

		// La commune de Jorat-Mézières débute civilement le 1er juillet 2016 et fiscalement le 1er janvier 2017.

		// Mise en place service mock
		final long noEntrepriseCivile = 101202100L;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(
						MockEntrepriseFactory.createSimpleEntrepriseRC(noEntrepriseCivile, noEntrepriseCivile + 1000000, "Synergy SA", RegDate.get(2016, 7, 10), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                               MockCommune.JoratMezieres));
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2016, 7, 12), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt.getEtat());

			assertEquals("La commune au numéro ofs 5806 n'existe pas en date du 07.07.2016! On en trouve cependant une appelée Jorat-Mézières (VD) commençant fiscalement en date du 01.01.2017.",
			             evt.getErreurs().get(0).getMessage());

			return null;
		});
	}
}
