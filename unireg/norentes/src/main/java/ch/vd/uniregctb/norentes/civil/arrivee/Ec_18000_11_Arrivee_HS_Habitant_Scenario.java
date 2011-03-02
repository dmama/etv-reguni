package ch.vd.uniregctb.norentes.civil.arrivee;

import annotation.Check;
import annotation.Etape;

import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class Ec_18000_11_Arrivee_HS_Habitant_Scenario extends EvenementCivilScenario {

	public static final String NAME = "Ec_18000_11_Arrivee_HS_Habitant";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getDescription() {
		return "Arrivée hors suisse d'un individu possedant un for principal vaudois";
	}

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS;
	}

	private final long noIndAntoine = 1020206L;
	private long evenementId;


	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndAntoine, date(1952, 2, 21) , "Lenormand", "Antoine", true);
			}
		});
	}

	@Etape(id = 1, descr = "Antoine vit à Lausanne depuis sa majorité")
	public void etape1() throws Exception {
		PersonnePhysique antoine = addHabitant(noIndAntoine);
		addForFiscalPrincipal(antoine, MockCommune.Lausanne, date(1990, 1, 1), null, MotifFor.MAJORITE, null);
	}

	@Etape(id = 2, descr = "Pour on ne sait quelle raison on reçoit un evenement comme quoi Antoine arrive à Renens depuis l'etranger")
	public void etape2() throws Exception {
		evenementId = addEvenementCivil(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS, noIndAntoine, date(2008, 11, 13), MockCommune.Renens.getNoOFS());
		commitAndStartTransaction();
		// On traite les evenements
		traiteEvenements(evenementId);
	}


	@Check(id = 2, descr = "Vérifie que l'évenement est bien en erreur")
	public void check1() throws Exception {
		final EvenementCivilExterne evenement = evtExterneDAO.get(evenementId);
		assertEquals(EtatEvenementCivil.EN_ERREUR, evenement.getEtat(), "L'événement civil devrait être en erreur.");
	}
}
