package ch.vd.unireg.norentes.civil.naissance;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.TypeEvenementCivil;

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

	@Check(id=1, descr="Contrôle qu'il n'y a pas de personne physique dans la base de données")
	public void check1() throws Exception {

		final List<Tiers> list = tiersDAO.getAll();
		CollectionUtils.filter(list, object -> object instanceof PersonnePhysique);
		assertEquals(0, list.size(), "");
	}

	@Etape(id=2, descr="Envoi de l'événement de Naissance")
	public void etape2() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.NAISSANCE, 89123L, RegDate.get(2007, 4, 23), 5757);
		commitAndStartTransaction();

		// On traite les evenements
		traiteEvenements(id);
	}

	@Check(id=2, descr="Contrôle qu'il n'y a pas de personne physique dans la base de données et que l'événement est en erreur")
	public void check2() throws Exception {
		final List<Tiers> list = tiersDAO.getAll();
		CollectionUtils.filter(list, object -> object instanceof PersonnePhysique);
		assertEquals(0, list.size(), "");

		checkEtatEvtCivils(1, EtatEvenementCivil.EN_ERREUR);
	}

}
