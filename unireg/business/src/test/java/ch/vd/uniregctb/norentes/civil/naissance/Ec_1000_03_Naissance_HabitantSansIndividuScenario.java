package ch.vd.uniregctb.norentes.civil.naissance;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.norentes.annotation.Check;
import ch.vd.uniregctb.norentes.annotation.Etape;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Teste la naissance d'un individu qui existe deja
 *
 * @author jec
 *
 */
public class Ec_1000_03_Naissance_HabitantSansIndividuScenario extends EvenementCivilScenario {

	public static final String NAME = "1000_03_Naissance";

	private final long noIndividu = 983254L;

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getDescription() {
		return "Test l'événement de naissance d'un Habitant sans Individu dans le registre civil";
	}

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.NAISSANCE;
	}

	@Etape(id=1, descr="Chargement de l'habitant")
	public void etape1() throws Exception {

		addHabitant(noIndividu);
	}

	@Check(id=1, descr="Vérifie qu'il y a 1 tiers dans la base de données")
	public void check1() throws Exception {

		List<Tiers> list = tiersDAO.getAll();
		assertEquals(1, list.size(), "");
	}

	@Etape(id=2, descr="Envoi de l'événement civil de naissance")
	public void etape2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.NAISSANCE, noIndividu, RegDate.get(2007, 4, 23), 5757);
		commitAndStartTransaction();

		// On traite les evenements
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie qu'il y a toujours un seul habitant dans la base de données et que l'événement est en erreur")
	public void check2() throws Exception {
		List<Tiers> list = tiersDAO.getAll();
		assertEquals(1, list.size(), "");

		checkEtatEvtCivils(1, EtatEvenementCivil.EN_ERREUR);
	}

}
