package ch.vd.uniregctb.norentes.civil.arrivee;

import java.util.List;

import annotation.Check;
import annotation.Etape;
import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class Ec_18000_05_Arrivee_RollBack_1_Scenario extends EvenementCivilScenario {

	public static final String NAME = "18000_05_Arrivee_RollBack";
			
	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC;
	}

	@Override
	public String getDescription() {
		return "arrivée dans le canton";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final long noIndAlain = 122456L;

	private MockIndividu indAlain;

	private final RegDate dateArriveeZurich = RegDate.get(2004, 3, 3);
	private final RegDate dateArriveeBex = RegDate.get(2006, 7, 5);
	
	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				indAlain = addIndividu(noIndAlain, RegDate.get(1952, 2, 21), "Gregoire", "Alain", true);
				addAdresse(indAlain, EnumTypeAdresse.PRINCIPALE, MockRue.Zurich.GloriaStrasse, null,
						dateArriveeZurich, dateArriveeBex.getOneDayBefore());
				addAdresse(indAlain, EnumTypeAdresse.PRINCIPALE, MockRue.Bex.RouteDuBoet, null,
						dateArriveeBex, null);

			}
		});
	}


	@Etape(id=1, descr="Chargement d'un habitant à Lausanne")
	public void etape1() throws Exception {

	}

	@Check(id=1, descr="Vérifie que l'habitant Alain est inconnu")
	public void check1() throws Exception {
		PersonnePhysique alain = tiersDAO.getHabitantByNumeroIndividu(noIndAlain);
		assertNull(alain, "Alain ne devrait pas être dans le registre");
	}
	
	@Etape(id=2, descr="Envoi de l'événement d'arrivée de l'individu Alain")
	public void etape2() throws Exception {

		long id = addEvenementCivil(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC, noIndAlain, dateArriveeBex, MockCommune.Bex.getNoOFS());

		commitAndStartTransaction();

		// On traite les evenements
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que l'habitant Alain est inconnu et que l'événements est en erreur")
	public void check4() throws Exception {
		PersonnePhysique alain = tiersDAO.getHabitantByNumeroIndividu(noIndAlain);
		assertNull(alain, "Alain ne devrait pas être dans le registre");
		List<EvenementCivilData> list = evtDAO.getAll();
		for (EvenementCivilData evt : list) {
			assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat(), "");
		}

	}
	
}
