package ch.vd.uniregctb.norentes.civil.annulation.mariage;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.norentes.annotation.Check;
import ch.vd.uniregctb.norentes.annotation.Etape;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Scénario d'un événement annulation de mariage du cas JIRA UNIREG-1086.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_4001_06_AnnulationMariage_JIRA1157_Scenario extends EvenementCivilScenario {

	public static final String NAME = "4001_06_AnnulationMariage";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ANNUL_MARIAGE;
	}

	@Override
	public String getDescription() {
		return "Scénario d'un événement annulation de mariage d'un couple (UNIREG-1157).";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final class DefaultMockServiceCivil extends MockServiceCivil {

		@Override
		protected void init() {
			MockIndividu sylvie = addIndividu(noIndSylvie, dateNaissanceSylvie, "Grandchamp", "Sylvie", false);
			addOrigine(sylvie, MockPays.Suisse.getNomMinuscule());
			addNationalite(sylvie, MockPays.Suisse, dateNaissanceSylvie, null);

			addAdresse(sylvie, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArriveeSylvie, null);
			addAdresse(sylvie, TypeAdresseCivil.COURRIER, MockRue.Lausanne.RouteMaisonNeuve, null, dateArriveeSylvie, null);
		}

		public void arriveeMonsieur() {
			MockIndividu alexandre = addIndividu(noIndAlexandre, dateNaissanceAlexandre, "Getaz", "Alexandre", true);
			addOrigine(alexandre, MockPays.Suisse.getNomMinuscule());
			addNationalite(alexandre, MockPays.Suisse, dateNaissanceAlexandre, null);

			addAdresse(alexandre, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArriveeAlexandre, null);
			addAdresse(alexandre, TypeAdresseCivil.COURRIER, MockRue.Lausanne.RouteMaisonNeuve, null, dateArriveeAlexandre, null);
		}

		public void marionsLes() {
			MockIndividu sylvie = getIndividu(noIndSylvie);
			MockIndividu alexandre = getIndividu(noIndAlexandre);
			marieIndividus(alexandre, sylvie, dateMariage);
		}

		public void annuleMariage() {
			MockIndividu alexandre = getIndividu(noIndAlexandre);
			MockIndividu sylvie = getIndividu(noIndSylvie);
			annuleMariage(alexandre, sylvie);
		}

	}

	private DefaultMockServiceCivil serviceCivil;

	@Override
	protected void initServiceCivil() {
		serviceCivil = new DefaultMockServiceCivil();
		serviceCivilService.setUp(serviceCivil);
	}

	private static final long noIndAlexandre = 2000465; // Alexandre
	private static final long noIndSylvie = 2000457; // Sylvie

	private long noHabAlexandre;
	private long noHabSylvie;
	private long noMenage;

	private final RegDate dateNaissanceSylvie = RegDate.get(1971, 11, 6);
	private final RegDate dateArriveeSylvie = RegDate.get(2001, 3, 1);
	private final RegDate dateNaissanceAlexandre = RegDate.get(1970, 3, 3);
	private final RegDate dateArriveeAlexandre = RegDate.get(2009, 2, 1); 	// 01.02.2009
	private final RegDate dateMariage = RegDate.get(2009, 1, 1);			// 01.01.2009
	private final MockCommune commune = MockCommune.Lausanne;

	@Etape(id=1, descr="Chargement de madame et ses fors")
	public void step1() {
		// Sylvie
		final PersonnePhysique sylvie = addHabitant(noIndSylvie);
		noHabSylvie = sylvie.getNumero();

		final ForFiscalPrincipal ffpSylvie = addForFiscalPrincipal(sylvie, MockCommune.Vevey, dateArriveeSylvie, null, MotifFor.ARRIVEE_HC, null);
		ffpSylvie.setModeImposition(ModeImposition.ORDINAIRE);
	}

	@Check(id=1, descr="Vérifie que madame existe et a un for ouvert")
	public void check1() {
		{
			final PersonnePhysique sylvie = (PersonnePhysique) tiersDAO.get(noHabSylvie);
			final ForFiscalPrincipal ffp = sylvie.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + sylvie.getNumero() + " null");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition(), "Le mode d'imposition n'est pas ORDINAIRE");
		}
	}

	@Etape(id=2, descr="Envoi de l'événement d'arrivée de monsieur")
	public void step2() throws Exception {
		// simulation de l'arrivée de monsieur dans le civil
		serviceCivil.arriveeMonsieur();
		// envoi de l'événement
		final long id = addEvenementCivil(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC, noIndAlexandre, dateArriveeAlexandre, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que l'habitant correspondant à monsieur a bien été créé et possède un for ouvert")
	public void check2() {
		final PersonnePhysique alexandre = tiersDAO.getHabitantByNumeroIndividu(noIndAlexandre);
		assertNotNull(alexandre, "L'individu correspondant à monsieur n'a pas été créé");
		noHabAlexandre = alexandre.getNumero();
		{
			final ForFiscalPrincipal ffp = alexandre.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + alexandre.getNumero() + " null");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition(), "Le mode d'imposition n'est pas ORDINAIRE");
		}
	}

	@Etape(id=3, descr="Envoi de l'événement de mariage")
	public void step3() throws Exception {
		// mariage civil
		serviceCivil.marionsLes();
		// envoi de l'événement
		final long id = addEvenementCivil(TypeEvenementCivil.MARIAGE, noIndAlexandre, dateMariage, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=3, descr="Vérifie que le ménage est créé et qu'il possède un for ouvert")
	public void check3() {
		final PersonnePhysique alexandre = (PersonnePhysique) tiersDAO.get(noHabAlexandre);
		{
			// Vérifications des fors d'Alexandre
			final ForFiscalPrincipal ffp = alexandre.getForFiscalPrincipalAt(null);
			assertNull(ffp, "Alexandre ne doit pas avoir un for ouvert");
		}
		{
			// Le dernier for d'Alexandre doit être annulé car arrivé déjà marié
			assertEquals(1, alexandre.getForsFiscaux().size(), "Alexandre devrait avoir un seul for fiscal");
			final ForFiscalPrincipal ffp = (ForFiscalPrincipal) alexandre.getForsFiscaux().toArray()[0];
			assertTrue(ffp.isAnnule(), "Le dernier for d'Alexandre devrait être annulé car arrivé déjà marié");
		}

		final PersonnePhysique sylvie = (PersonnePhysique) tiersDAO.get(noHabSylvie);
		{
			// Vérifications des fors de Sylvie
			final ForFiscalPrincipal ffp = sylvie.getForFiscalPrincipalAt(null);
			assertNull(ffp, "Sylvie ne doit pas avoir un for ouvert");
		}
		{
			// Le dernier for de Sylvie doit être fermé la veille du mariage
			final ForFiscalPrincipal ffp = sylvie.getDernierForFiscalPrincipal();
			assertEquals(dateMariage.getOneDayBefore(), ffp.getDateFin(), "Le dernier for de Sylvie devrait être fermé la veille du mariage");
		}

		final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(alexandre, dateMariage);
		assertNotNull(ensemble, "Le ménage commun n'a pas été trouvé");
		assertNotNull(ensemble.getMenage(), "Le ménage commun n'a pas été trouvé");
		MenageCommun mc = ensemble.getMenage();
		noMenage = mc.getNumero();
		{
			final ForFiscalPrincipal ffp = mc.getForFiscalPrincipalAt(null);
			assertNotNull(ffp, "Le ménage devrait avoir un for ouvert");
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début de for fausse");
			assertNull(ffp.getDateFin(), "Date de fin de for fausse");
		}
	}

	@Etape(id=4, descr="Envoi de l'événement d'annulation de mariage")
	public void step5() throws Exception {
		// annulation de mariage dans le civil
		serviceCivil.annuleMariage();
		// envoi de l'événement
		final long id = addEvenementCivil(TypeEvenementCivil.ANNUL_MARIAGE, noIndSylvie, dateMariage, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=4, descr="Vérifie que le mariage a été bien annulé")
	public void check5() {
		final EvenementCivilRegPP evt = getEvenementCivilRegoupeForHabitant(noHabSylvie);
		assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "L'événement d'annulation de mariage devrait être traité");

		checkHabitantApresAnnulation((PersonnePhysique) tiersDAO.get(noHabSylvie), dateArriveeSylvie, MotifFor.ARRIVEE_HC, null);
		checkHabitantApresAnnulation((PersonnePhysique) tiersDAO.get(noHabAlexandre), dateArriveeAlexandre, MotifFor.ARRIVEE_HC, null);

		MenageCommun mc = (MenageCommun) tiersDAO.get(noMenage);
		assertEquals(0, mc.getForsFiscauxNonAnnules(false).size(), "Le ménage ne devrait avoir aucun for principal");
	}

	private void checkHabitantApresAnnulation(PersonnePhysique habitant, RegDate dateFor, MotifFor motifFor, EtatCivil situationFamille) {
		ForFiscalPrincipal ffp = habitant.getForFiscalPrincipalAt(null);

		final String numeroHabitant = FormatNumeroHelper.numeroCTBToDisplay(habitant.getNumero());
		assertNotNull(ffp, "L'habitant " + numeroHabitant + " doit avoir un for principal actif après l'annulation de mariage");
		assertEquals(dateFor, ffp.getDateDebut(), "Le for de l'habitant " + numeroHabitant + " devrait commencer le " + dateFor);
		assertEquals(motifFor, ffp.getMotifOuverture(), "Le motif de fermeture n'est pas " + motifFor.name());
		assertNull(ffp.getDateFin(), "Le for de l'habitant " + numeroHabitant + " est fermé");
		assertNull(ffp.getMotifFermeture(), "Le motif de fermeture devrait être null");
		// Vérification des fors fiscaux
		for (ForFiscal forFiscal : habitant.getForsFiscaux()) {
			if (forFiscal.getDateFin() != null && dateMariage.getOneDayBefore().equals(forFiscal.getDateFin()) &&
					(forFiscal instanceof ForFiscalRevenuFortune && MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION == ((ForFiscalRevenuFortune) forFiscal).getMotifFermeture())) {
				assertEquals(true, forFiscal.isAnnule(), "Les fors fiscaux fermés lors du mariage doivent être annulés");
			}
		}
		SituationFamille sf = habitant.getSituationFamilleActive();
		if (situationFamille == null) {
			assertNull(sf, "La situation de famille devrait être null");
		}
		else {
			assertNotNull(sf, "La situation de famille null");
			assertEquals(situationFamille, sf.getEtatCivil(), "La situation de famille devrait être " + situationFamille.name());
		}
	}
}
