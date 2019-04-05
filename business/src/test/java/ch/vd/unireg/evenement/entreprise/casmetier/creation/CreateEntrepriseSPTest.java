package ch.vd.unireg.evenement.entreprise.casmetier.creation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

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
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.MockServiceEntreprise;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.RegimeFiscal;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
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
public class CreateEntrepriseSPTest extends AbstractEvenementEntrepriseCivileProcessorTest {

	private EvenementFiscalDAO evtFiscalDAO;

	public CreateEntrepriseSPTest() {
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
	public void testCreationSP() throws Exception {

		// Mise en place service mock
		final long noEntrepriseCivile = 101202100L;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				addEntreprise(
						MockEntrepriseFactory.createSimpleEntrepriseRC(noEntrepriseCivile, noEntrepriseCivile + 1000000, "Robert Alkan et autres", RegDate.get(2015, 6, 27), null, FormeLegale.N_0103_SOCIETE_NOM_COLLECTIF,
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
			assertEquals(EtatEvenementEntreprise.A_VERIFIER, evt.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());
			final Set<RegimeFiscal> regimesFiscaux = entreprise.getRegimesFiscaux();
			assertEquals(2, regimesFiscaux.size());
			final RegimeFiscal regimeFiscal1 = regimesFiscaux.iterator().next();
			assertNotNull(regimeFiscal1);
			assertEquals(RegDate.get(2015, 6, 25), regimeFiscal1.getDateDebut());
			assertNull(regimeFiscal1.getDateFin());
			assertEquals("80", regimeFiscal1.getCode());

			ForFiscalPrincipal forFiscalPrincipal = entreprise.getDernierForFiscalPrincipal();
			assertEquals(RegDate.get(2015, 6, 25), forFiscalPrincipal.getDateDebut());
			assertNull(forFiscalPrincipal.getDateFin());
			assertEquals(GenreImpot.REVENU_FORTUNE, forFiscalPrincipal.getGenreImpot());
			assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
			assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
			assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipal.getMotifOuverture());

			{
				final List<DateRanged<Etablissement>> etbsPrns = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
				assertEquals(1, etbsPrns.size());
				assertEquals(RegDate.get(2015, 6, 24), etbsPrns.get(0).getDateDebut());
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
				assertEquals(MockTypeRegimeFiscal.SOCIETE_PERS.getCode(), efrf.getRegimeFiscal().getCode());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(1);
				assertNotNull(ef);
				assertEquals(EvenementFiscalRegimeFiscal.class, ef.getClass());
				assertEquals(date(2015, 6, 25), ef.getDateValeur());

				final EvenementFiscalRegimeFiscal efrf = (EvenementFiscalRegimeFiscal) ef;
				assertEquals(EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE, efrf.getType());
				assertEquals(RegimeFiscal.Portee.VD, efrf.getRegimeFiscal().getPortee());
				assertEquals(MockTypeRegimeFiscal.SOCIETE_PERS.getCode(), efrf.getRegimeFiscal().getCode());
			}
			{
				final EvenementFiscal ef = evtsFiscauxTries.get(2);
				assertNotNull(ef);
				assertEquals(EvenementFiscalFor.class, ef.getClass());
				assertEquals(date(2015, 6, 25), ef.getDateValeur());

				final EvenementFiscalFor eff = (EvenementFiscalFor) ef;
				assertTrue(eff.getForFiscal() instanceof ForFiscalPrincipal);
				assertEquals(EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE, eff.getType());
				assertEquals(date(2015, 6, 25), eff.getForFiscal().getDateDebut());
			}

			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testCreationSPNonRC() throws Exception {

		// Mise en place service mock
		final long noEntrepriseCivile = 101202100L;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				addEntreprise(
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEntrepriseCivile + 1000000, "Robert Alkan et autres", RegDate.get(2015, 6, 24), null, FormeLegale.N_0103_SOCIETE_NOM_COLLECTIF,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), null, null, StatusRegistreIDE.DEFINITIF,
						                                       TypeEntrepriseRegistreIDE.ASSOCIATION, "CHE999999996"));
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.IDE_NOUVELLE_INSCRIPTION, RegDate.get(2015, 6, 24), A_TRAITER);
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
			assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());
			assertEquals(MotifRattachement.DOMICILE, forFiscalPrincipal.getMotifRattachement());
			assertEquals(MotifFor.DEBUT_EXPLOITATION, forFiscalPrincipal.getMotifOuverture());

			{
				final List<DateRanged<Etablissement>> etablissements = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
				assertEquals(1, etablissements.size());
				assertEquals(RegDate.get(2015, 6, 24), etablissements.get(0).getDateDebut());
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

			return null;
		});
	}
}
