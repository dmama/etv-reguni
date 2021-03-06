package ch.vd.unireg.evenement.entreprise.casmetier.creation;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.engine.AbstractEvenementEntrepriseCivileProcessorTest;
import ch.vd.unireg.interfaces.entreprise.data.DateRanged;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.MockEntrepriseConnector;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.RegimeFiscal;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementEntreprise;

import static ch.vd.unireg.type.EtatEvenementEntreprise.A_TRAITER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Raphaël Marmier, 2015-09-03
 */
public class CreateEntrepriseAPMTest extends AbstractEvenementEntrepriseCivileProcessorTest {

	public CreateEntrepriseAPMTest() {
		setWantIndexationTiers(true);
	}

	protected boolean buildProcessorOnSetup() {
		return true;
	}

	@Test(timeout = 10000L)
	public void testCreationAPM() throws Exception {

		// Mise en place service mock
		final long noEntrepriseCivile = 101202100L;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(
						MockEntrepriseFactory.createSimpleEntrepriseRC(noEntrepriseCivile, noEntrepriseCivile + 1000000, "Association bidule", RegDate.get(2015, 6, 27), null, FormeLegale.N_0110_FONDATION,
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
			final List<RegimeFiscal> regimesFiscaux = new ArrayList<>(entreprise.getRegimesFiscaux());
			assertEquals(4, regimesFiscaux.size());
			regimesFiscaux.sort(new DateRangeComparator<RegimeFiscal>().thenComparing(RegimeFiscal::getPortee));
			assertRegimeFiscal(RegDate.get(2015, 6, 25), RegDate.get(2017, 12, 31), RegimeFiscal.Portee.VD, "70", regimesFiscaux.get(0));
			assertRegimeFiscal(RegDate.get(2015, 6, 25), RegDate.get(2017, 12, 31), RegimeFiscal.Portee.CH, "70", regimesFiscaux.get(1));
			assertRegimeFiscal(RegDate.get(2018, 1, 1), null, RegimeFiscal.Portee.VD, "703", regimesFiscaux.get(2));
			assertRegimeFiscal(RegDate.get(2018, 1, 1), null, RegimeFiscal.Portee.CH, "703", regimesFiscaux.get(3));

			ForFiscalPrincipal forFiscalPrincipal = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(RegDate.get(2015, 6, 25)).get(0);
			assertEquals(RegDate.get(2015, 6, 25), forFiscalPrincipal.getDateDebut());
			assertNull(forFiscalPrincipal.getDateFin());
			assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
			assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());

			// [SIFISC-30696] la date du premier exercice commercial doit être renseignée automatiquement
			assertEquals(date(2015, 1, 1), entreprise.getDateDebutPremierExerciceCommercial());

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
	public void testCreationAPMNonRC() throws Exception {

		// Mise en place service mock
		final long noEntrepriseCivile = 101202100L;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEntrepriseCivile + 1000000, "Association bidule", RegDate.get(2015, 6, 24), null, FormeLegale.N_0110_FONDATION,
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
			assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt.getEtat());

			assertNull(tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile()));

			assertEquals(
					String.format("Pas de création automatique de l'association/fondation n°%d [Association bidule] non inscrite au RC (risque de création de doublon). Veuillez vérifier et le cas échéant créer le tiers associé à la main.",
					              noEntrepriseCivile)
					, evt.getErreurs().get(1).getMessage());

			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testArriveeAPMIDERC() throws Exception {

		// Mise en place service mock
		final long noEntrepriseCivile = 101202100L;

		final MockEntrepriseCivile ent = MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEntrepriseCivile + 1000000, "Synergy Assoc", RegDate.get(2015, 6, 28), null, FormeLegale.N_0109_ASSOCIATION,
		                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, RegDate.get(2015, 6, 26),
		                                                                        StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.ASSOCIATION, "CHE999999996");
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
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, RegDate.get(2015, 6, 28), A_TRAITER);
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
			final List<RegimeFiscal> regimesFiscaux = new ArrayList<>(entreprise.getRegimesFiscaux());
			assertEquals(4, regimesFiscaux.size());
			regimesFiscaux.sort(new DateRangeComparator<RegimeFiscal>().thenComparing(RegimeFiscal::getPortee));
			assertRegimeFiscal(RegDate.get(2015, 6, 26), RegDate.get(2017, 12, 31), RegimeFiscal.Portee.VD, "70", regimesFiscaux.get(0));
			assertRegimeFiscal(RegDate.get(2015, 6, 26), RegDate.get(2017, 12, 31), RegimeFiscal.Portee.CH, "70", regimesFiscaux.get(1));
			assertRegimeFiscal(RegDate.get(2018, 1, 1), null, RegimeFiscal.Portee.VD, "703", regimesFiscaux.get(2));
			assertRegimeFiscal(RegDate.get(2018, 1, 1), null, RegimeFiscal.Portee.CH, "703", regimesFiscaux.get(3));

			ForFiscalPrincipal forFiscalPrincipal = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(RegDate.get(2015, 6, 26)).get(0);
			assertEquals(RegDate.get(2015, 6, 26), forFiscalPrincipal.getDateDebut());
			assertNull(forFiscalPrincipal.getDateFin());
			assertEquals(GenreImpot.BENEFICE_CAPITAL, forFiscalPrincipal.getGenreImpot());
			assertEquals(MockCommune.Lausanne.getNoOFS(), forFiscalPrincipal.getNumeroOfsAutoriteFiscale().intValue());

			// [SIFISC-30696] la date du premier exercice commercial doit être renseignée automatiquement
			assertEquals(date(2015, 1, 1), entreprise.getDateDebutPremierExerciceCommercial());

			{
				final List<DateRanged<Etablissement>> etbsPrns = tiersService.getEtablissementsPrincipauxEntreprise(entreprise);
				assertEquals(1, etbsPrns.size());
				assertEquals(RegDate.get(2015, 6, 26), etbsPrns.get(0).getDateDebut());
			}
			{
				final List<DateRanged<Etablissement>> etbsSecs = tiersService.getEtablissementsSecondairesEntreprise(entreprise);
				assertEquals(0, etbsSecs.size());
			}

			return null;
		});
	}
}
