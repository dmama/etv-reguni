package ch.vd.unireg.evenement.entreprise.casmetier.creation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.engine.AbstractEvenementEntrepriseCivileProcessorTest;
import ch.vd.unireg.interfaces.entreprise.data.DateRanged;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.MockEntrepriseConnector;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.tiers.Bouclement;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.FormeJuridiqueFiscaleEntreprise;
import ch.vd.unireg.tiers.RaisonSocialeFiscaleEntreprise;
import ch.vd.unireg.tiers.RegimeFiscal;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEtatEntreprise;
import ch.vd.unireg.type.TypeEvenementEntreprise;

import static ch.vd.unireg.type.EtatEvenementEntreprise.A_TRAITER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Raphaël Marmier, 2017-03-27
 */
public class CreateEntrepriseINDETTest extends AbstractEvenementEntrepriseCivileProcessorTest {

	public CreateEntrepriseINDETTest() {
		setWantIndexationTiers(true);
	}

	protected boolean buildProcessorOnSetup() {
		return true;
	}

	@Test(timeout = 10000L)
	public void testCreationDroitPublic() throws Exception {

		// Mise en place service mock
		final long noEntrepriseCivile = 101202100L;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(MockEntrepriseFactory
						              .createEntreprise(noEntrepriseCivile, noEntrepriseCivile + 1000000, "CorpoPub", RegDate.get(2015, 6, 26), null,
						                                FormeLegale.N_0234_CORPORATION_DE_DROIT_PUBLIC_ENTREPRISE,
						                                TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2015, 6, 24),
						                                StatusRegistreIDE.DEFINITIF,
						                                TypeEntrepriseRegistreIDE.ASSOCIATION, "CHE999999996", BigDecimal.valueOf(100000), "CHF"));
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

			assertEquals(TypeEtatEntreprise.INSCRITE_RC, entreprise.getEtatActuel().getType());

			final Set<RegimeFiscal> regimesFiscaux = entreprise.getRegimesFiscaux();
			assertEquals(2, regimesFiscaux.size());
			final RegimeFiscal regimeFiscal1 = regimesFiscaux.iterator().next();
			assertNotNull(regimeFiscal1);
			assertEquals(RegDate.get(2015, 6, 25), regimeFiscal1.getDateDebut());
			assertNull(regimeFiscal1.getDateFin());
			assertEquals("00", regimeFiscal1.getCode());

			final RaisonSocialeFiscaleEntreprise surchargeRaisonSocialeFiscale = entreprise.getRaisonsSocialesNonAnnuleesTriees().get(0);
			assertEquals(date(2015, 6, 24), surchargeRaisonSocialeFiscale.getDateDebut());
			assertEquals(date(2015, 6, 25), surchargeRaisonSocialeFiscale.getDateFin());
			assertEquals("CorpoPub", surchargeRaisonSocialeFiscale.getRaisonSociale());

			final FormeJuridiqueFiscaleEntreprise surchargeFormeJuridiqueFiscale = entreprise.getFormesJuridiquesNonAnnuleesTriees().get(0);
			assertEquals(date(2015, 6, 24), surchargeFormeJuridiqueFiscale.getDateDebut());
			assertEquals(date(2015, 6, 25), surchargeFormeJuridiqueFiscale.getDateFin());
			assertEquals(FormeJuridiqueEntreprise.CORP_DP_ENT, surchargeFormeJuridiqueFiscale.getFormeJuridique());

			assertEquals(100000L, entreprise.getCapitauxNonAnnulesTries().get(0).getMontant().getMontant().longValue());

			final Bouclement bouclement = entreprise.getBouclements().iterator().next();
			assertEquals(RegDate.get(2015, 6, 25), bouclement.getDateDebut());
			assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
			assertEquals(12, bouclement.getPeriodeMois());

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
	public void testCreationDroitPublicNonRC() throws Exception {

		// Mise en place service mock
		final long noEntrepriseCivile = 101202100L;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(MockEntrepriseFactory
						              .createEntreprise(noEntrepriseCivile, noEntrepriseCivile + 1000000, "CorpoPub", RegDate.get(2015, 6, 24), null,
						                                FormeLegale.N_0234_CORPORATION_DE_DROIT_PUBLIC_ENTREPRISE,
						                                TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), null, null,
						                                StatusRegistreIDE.DEFINITIF,
						                                TypeEntrepriseRegistreIDE.ASSOCIATION, "CHE999999996", BigDecimal.valueOf(100000), "CHF"));
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.REE_NOUVELLE_INSCRIPTION, RegDate.get(2015, 6, 24), A_TRAITER);
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

			assertEquals(TypeEtatEntreprise.FONDEE, entreprise.getEtatActuel().getType());

			final Set<RegimeFiscal> regimesFiscaux = entreprise.getRegimesFiscaux();
			assertEquals(2, regimesFiscaux.size());
			final RegimeFiscal regimeFiscal1 = regimesFiscaux.iterator().next();
			assertNotNull(regimeFiscal1);
			assertEquals(RegDate.get(2015, 6, 24), regimeFiscal1.getDateDebut());
			assertNull(regimeFiscal1.getDateFin());
			assertEquals("00", regimeFiscal1.getCode());

			final Bouclement bouclement = entreprise.getBouclements().iterator().next();
			assertEquals(RegDate.get(2015, 6, 24), bouclement.getDateDebut());
			assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
			assertEquals(12, bouclement.getPeriodeMois());

			ForFiscalPrincipal forFiscalPrincipal = (ForFiscalPrincipal) entreprise.getForsFiscauxValidAt(RegDate.get(2015, 6, 24)).get(0);
			assertEquals(RegDate.get(2015, 6, 24), forFiscalPrincipal.getDateDebut());
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
	public void testCreationFondPlacement() throws Exception {

		// Mise en place service mock
		final long noEntrepriseCivile = 101202100L;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(
						MockEntrepriseFactory.createSimpleEntrepriseRC(noEntrepriseCivile, noEntrepriseCivile + 1000000, "Kramer Placements", RegDate.get(2015, 6, 27), null,
						                                               FormeLegale.N_0114_SOCIETE_EN_COMMANDITE_POUR_PLACEMENTS_CAPITAUX, MockCommune.Lausanne));
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
			assertEquals("00", regimeFiscal1.getCode());

			final RaisonSocialeFiscaleEntreprise surchargeRaisonSocialeFiscale = entreprise.getRaisonsSocialesNonAnnuleesTriees().get(0);
			assertEquals(date(2015, 6, 24), surchargeRaisonSocialeFiscale.getDateDebut());
			assertEquals(date(2015, 6, 26), surchargeRaisonSocialeFiscale.getDateFin());
			assertEquals("Kramer Placements", surchargeRaisonSocialeFiscale.getRaisonSociale());

			final FormeJuridiqueFiscaleEntreprise surchargeFormeJuridiqueFiscale = entreprise.getFormesJuridiquesNonAnnuleesTriees().get(0);
			assertEquals(date(2015, 6, 24), surchargeFormeJuridiqueFiscale.getDateDebut());
			assertEquals(date(2015, 6, 26), surchargeFormeJuridiqueFiscale.getDateFin());
			assertEquals(FormeJuridiqueEntreprise.SCPC, surchargeFormeJuridiqueFiscale.getFormeJuridique());

			assertTrue(entreprise.getCapitauxNonAnnulesTries().isEmpty());

			final Bouclement bouclement = entreprise.getBouclements().iterator().next();
			assertEquals(RegDate.get(2015, 6, 25), bouclement.getDateDebut());
			assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
			assertEquals(12, bouclement.getPeriodeMois());

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
}
