package ch.vd.uniregctb.norentes.civil.ignore;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneDAO;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.annotation.Check;
import ch.vd.uniregctb.norentes.annotation.Etape;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class Ec_0_01_Ech99_Scenario extends EvenementCivilScenario {

	public static final String NAME = "Ec_0_01_Ech99";

	private long idEvenementCivil;

	private EvenementCivilExterneDAO evtCivilExterneDAO;

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

	public void setEvtCivilExterneDAO(EvenementCivilExterneDAO evtCivilExterneDAO) {
		this.evtCivilExterneDAO = evtCivilExterneDAO;
	}

	private final long noIndAlain = 123456L;
	private MockIndividu indAlain;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {
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
		final EvenementCivilExterne evtCivil = evtCivilExterneDAO.get(idEvenementCivil);
		assertNotNull(evtCivil, "L'événement est introuvable.");
		assertEquals(EtatEvenementCivil.TRAITE, evtCivil.getEtat(), "Evénement non traité");
	}
}
