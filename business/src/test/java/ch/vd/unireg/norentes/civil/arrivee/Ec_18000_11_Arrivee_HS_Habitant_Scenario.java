package ch.vd.unireg.norentes.civil.arrivee;

import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeEvenementCivil;

public class Ec_18000_11_Arrivee_HS_Habitant_Scenario extends EvenementCivilScenario {

	public static final String NAME = "Ec_18000_11_Arrivee_HS_Habitant";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getDescription() {
		return "Arrivée hors Suisse d'un individu possedant un for principal vaudois";
	}

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS;
	}

	private static final long noIndAntoine = 1020206L;
	private long evenementId;


	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockIndividuConnector() {
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
		final EvenementCivilRegPP evenement = evtExterneDAO.get(evenementId);
		assertEquals(EtatEvenementCivil.EN_ERREUR, evenement.getEtat(), "L'événement civil devrait être en erreur.");
	}
}
