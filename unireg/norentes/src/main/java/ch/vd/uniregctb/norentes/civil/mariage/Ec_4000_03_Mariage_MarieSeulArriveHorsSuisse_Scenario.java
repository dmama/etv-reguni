package ch.vd.uniregctb.norentes.civil.mariage;

import annotation.Check;
import annotation.Etape;
import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypePermis;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Scénario de mariage d'un individu marié seul arrivés de hors Suisse.
 *
 * @author Pavel BLANCO
 *
 */
public class Ec_4000_03_Mariage_MarieSeulArriveHorsSuisse_Scenario extends MariageApresArriveeScenarios {

	public static final String NAME = "4000_03_Mariage";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.MARIAGE;
	}

	@Override
	public String getDescription() {
		return "Mariage d'un marié seul arrivé de hors Suisse.";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final long noIndMaria = 3913649; // maria

	private MockIndividu indMaria;

	private long noHabMaria;

	private final RegDate dateArrivee = RegDate.get(2008, 11, 1);
	private final RegDate dateMariage = RegDate.get(2000, 6, 21);
	private final MockCommune communeArrivee = MockCommune.Lausanne;

	private class InternalServiceCivil extends MockServiceCivil {
		@Override
		protected void init() {
			RegDate dateNaissanceMaria = RegDate.get(1975, 7, 31);
			indMaria = addIndividu(noIndMaria, dateNaissanceMaria, "Nadalino", "Maria", false);

			addOrigine(indMaria, MockPays.Espagne, null, dateNaissanceMaria);
			addNationalite(indMaria, MockPays.Espagne, dateNaissanceMaria, null, 0);
			addPermis(indMaria, EnumTypePermis.ETABLLISSEMENT, RegDate.get(2008, 10, 1), null, 0, false);
			addAdresse(indMaria, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.RouteMaisonNeuve, null, dateArrivee, null);
		}

		public void prepareMariage() {
			marieIndividu(indMaria, dateMariage);
		}
	}

	private InternalServiceCivil internalServiceCivil;

	@Override
	protected void initServiceCivil() {
		internalServiceCivil = new InternalServiceCivil();
		serviceCivilService.setUp(internalServiceCivil);
	}

	@Etape(id=1, descr="Envoi des événements d'arrivée")
	public void step1() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS, noIndMaria, dateArrivee, communeArrivee.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=1, descr="Vérifie que l'habitant a bien été créé et qu'il a un for actif")
	public void check1() throws Exception {
		// maria
		noHabMaria = checkArriveeHabitant(noIndMaria, dateArrivee);
	}

	@Etape(id=2, descr="Envoi de l'événement de Mariage")
	public void step2() throws Exception {
		// marie les individus dans le civil
		internalServiceCivil.prepareMariage();
		// envoi de l'événement de mariage
		long id = addEvenementCivil(TypeEvenementCivil.MARIAGE, noIndMaria, dateMariage, communeArrivee.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que le for de l'habitant précédemment créé a été annulé, le ménage commun créé et qu'il a un for actif")
	public void check2() throws Exception {
		EvenementCivilData evt = getEvenementCivilRegoupeForHabitant(noHabMaria);

		assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "L'événement de mariage devrait être traité");

		// maria
		PersonnePhysique maria = (PersonnePhysique) tiersDAO.get(noHabMaria);
		{
			assertNotNull(maria, "L'habitant Maria n'a pas été trouvé");
			ForFiscalPrincipal ffp = maria.getForFiscalPrincipalAt(null);
			assertNull(ffp, "Maria devrait pas avoir de for fiscal principal ouvert");
		}

		// ménage
		{
			EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(maria, dateArrivee);
			assertNotNull(couple, "Le ménage n'a pas été créé");
			checkMenageApresMariage(couple.getMenage(), dateMariage, dateArrivee, MotifFor.ARRIVEE_HS);
		}
	}

}
