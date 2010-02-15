package ch.vd.uniregctb.norentes.civil.mariage;

import annotation.Check;
import annotation.Etape;
import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypePermis;
import ch.vd.uniregctb.evenement.EvenementCivilRegroupe;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Scénario de mariage de deux individus arrivés de hors Suisse. D'abord le mari et ensuite la femme.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_4000_02_Mariage_CoupleArriveHorsSuisse_Scenario extends MariageApresArriveeScenarios {

	public static final String NAME = "4000_02_Mariage";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.MARIAGE;
	}

	@Override
	public String getDescription() {
		return "Mariage de deux individus arrivés individuellement de hors Suisse.";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final long noIndRafa = 3913648; // rafa
	private final long noIndMaria = 3913649; // maria

	private MockIndividu indRafa;
	private MockIndividu indMaria;

	private long noHabRafa;
	private long noHabMaria;

	private final RegDate dateArrivee = RegDate.get(2008, 11, 1);
	private final RegDate dateMariage = RegDate.get(2000, 6, 21);
	private final MockCommune communeArrivee = MockCommune.Lausanne;

	private class InternalServiceCivil extends MockServiceCivil {
		@Override
		protected void init() {
			RegDate dateNaissanceRafa = RegDate.get(1974, 6, 25);
			indRafa = addIndividu(noIndRafa, dateNaissanceRafa, "Nadalino", "Rafa", true);

			RegDate dateNaissanceMaria = RegDate.get(1975, 7, 31);
			indMaria = addIndividu(noIndMaria, dateNaissanceMaria, "Nadalino", "Maria", false);

			addOrigine(indRafa, MockPays.Espagne, null, dateNaissanceRafa);
			addNationalite(indRafa, MockPays.Espagne, dateNaissanceRafa, null, 0);
			addPermis(indRafa, EnumTypePermis.ETABLLISSEMENT, RegDate.get(2008, 10, 1), null, 0, false);
			addAdresse(indRafa, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.RouteMaisonNeuve, null, MockLocalite.Lausanne, dateArrivee, null);

			addOrigine(indMaria, MockPays.Espagne, null, dateNaissanceMaria);
			addNationalite(indMaria, MockPays.Espagne, dateNaissanceMaria, null, 0);
			addPermis(indMaria, EnumTypePermis.ETABLLISSEMENT, RegDate.get(2008, 10, 1), null, 0, false);
			addAdresse(indMaria, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.RouteMaisonNeuve, null, MockLocalite.Lausanne, dateArrivee, null);
		}

		public void prepareMariage() {
			marieIndividus(indRafa, indMaria, dateMariage);
		}
	}

	private InternalServiceCivil internalServiceCivil;

	@Override
	protected void initServiceCivil() {
		internalServiceCivil = new InternalServiceCivil();
		serviceCivilService.setUp(internalServiceCivil);
	}

	@Etape(id=1, descr="Envoi de l'événement d'arrivée du mari")
	public void step1() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS, noIndRafa, dateArrivee, communeArrivee.getNoOFS());
		commitAndStartTransaction();
		regroupeEtTraiteEvenements(id);
	}

	@Check(id=1, descr="Vérifie qu'un habitant correspondant au mari a bien été créé et qu'il possède un for ouvert")
	public void check1() throws Exception {
		// rafa
		noHabRafa = checkArriveeHabitant(noIndRafa, dateArrivee);
	}

	@Etape(id=2, descr="Envoi de l'événement d'arrivée de la femme")
	public void step2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS, noIndMaria, dateArrivee, communeArrivee.getNoOFS());
		commitAndStartTransaction();
		regroupeEtTraiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie qu'un habitant correspondant à l'épouse a bien été créé et qu'il possède un for ouvert")
	public void check2() throws Exception {
		// maria
		noHabMaria = checkArriveeHabitant(noIndMaria, dateArrivee);
	}

	@Etape(id=3, descr="Mariage des individus dans le civil et envoi de l'événement de Mariage")
	public void step3() throws Exception {
		// marie les individus dans le civil
		internalServiceCivil.prepareMariage();
		// envoi de l'événement de mariage
		long id = addEvenementCivil(TypeEvenementCivil.MARIAGE, noIndMaria, dateMariage, communeArrivee.getNoOFS());
		commitAndStartTransaction();
		regroupeEtTraiteEvenements(id);
	}

	@Check(id=3, descr="Vérifie que les fors des habitants précédemment créés ont été annulés, le ménage commun créé et qu'il a un for ouvert")
	public void check3() throws Exception {
		EvenementCivilRegroupe evt = getEvenementCivilRegoupeForHabitant(noHabMaria);

		assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "L'événement de mariage devrait être traité");

		// rafa
		PersonnePhysique rafa = (PersonnePhysique) tiersDAO.get(noHabRafa);
		checkHabitantApresMariage(rafa, dateArrivee);

		// maria
		checkHabitantApresMariage((PersonnePhysique) tiersDAO.get(noHabMaria), dateArrivee);

		// ménage
		{
			EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(rafa, dateArrivee);
			assertNotNull(couple, "Le ménage n'a pas été créé");
			checkMenageApresMariage(couple.getMenage(), dateMariage, dateArrivee, MotifFor.ARRIVEE_HS);
		}
	}
}
