package ch.vd.unireg.norentes.civil.ignore;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPPDAO;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivil;

public class Ec_0_01_Ech99_Scenario extends EvenementCivilScenario {

	public static final String NAME = "Ec_0_01_Ech99";

	private long idEvenementCivil;

	private EvenementCivilRegPPDAO evtCivilExterneDAO;

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ETAT_COMPLET;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getDescription() {
		return "Test de l'arrivée d'un événement de déclaration de l'état complet des données d'un individu (doit être traité sans autre)";
	}

	public void setEvtCivilExterneDAO(EvenementCivilRegPPDAO evtCivilExterneDAO) {
		this.evtCivilExterneDAO = evtCivilExterneDAO;
	}

	private static final long noIndAlain = 123456L;
	private MockIndividu indAlain;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				indAlain = addIndividu(noIndAlain, RegDate.get(1952, 2, 21), "Baschung", "Alain", true);
				addAdresse(indAlain, TypeAdresseCivil.PRINCIPALE, MockRue.VillarsSousYens.CheminDuCollege, null, RegDate.get(1990,1,1), null);
			}
		});
	}

	@Etape(id=1, descr = "Envoi de l'événement")
	public void etape1() throws Exception {
		idEvenementCivil = addEvenementCivil(TypeEvenementCivil.ETAT_COMPLET, noIndAlain, RegDate.get(), MockCommune.Vevey.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(idEvenementCivil);
	}

	@Check(id=1, descr="L'événement doit être traité")
	public void check1() throws Exception {
		final EvenementCivilRegPP evtCivil = evtCivilExterneDAO.get(idEvenementCivil);
		assertNotNull(evtCivil, "L'événement est introuvable.");
		assertEquals(EtatEvenementCivil.TRAITE, evtCivil.getEtat(), "Evénement non traité");
	}
}
