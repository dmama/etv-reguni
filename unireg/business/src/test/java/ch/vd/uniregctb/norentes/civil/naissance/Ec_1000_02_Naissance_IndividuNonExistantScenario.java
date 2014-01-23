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
 * Traitement d'une naissance d'un individu majeur
 *
 * @author jec
 *
 */
public class Ec_1000_02_Naissance_IndividuNonExistantScenario extends EvenementCivilScenario {

	public static final String NAME = "1000_02_Naissance";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getDescription() {
		return "Test de la naissance d'un Individu no existant dans le registre civil";
	}

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.NAISSANCE;
	}

	@Etape(id=1, descr="Chargement des données : aucune")
	public void etape1() {
	}

	@Check(id=1, descr="Contrôle qu'il n'y a pas de tiers dans la base de données")
	public void check1() throws Exception {

		List<Tiers> list = tiersDAO.getAll();
		assertEquals(0, list.size(), "");
	}

	@Etape(id=2, descr="Envoi de l'événement de Naissance")
	public void etape2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.NAISSANCE, 89123L, RegDate.get(2007, 4, 23), 5757);
		commitAndStartTransaction();

		// On traite les evenements
		traiteEvenements(id);
	}

	@Check(id=2, descr="Contrôle qu'il n'y a pas de Tiers dans la base de données et que l'événement est en erreur")
	public void check2() throws Exception {
		List<Tiers> list = tiersDAO.getAll();
		assertEquals(0, list.size(), "");

		checkEtatEvtCivils(1, EtatEvenementCivil.EN_ERREUR);
	}

}
