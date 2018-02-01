package ch.vd.unireg.norentes.common;

import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.TypeEvenementCivil;

public class NorentesFrameworkTestScenario extends EvenementCivilScenario {

	public static final String NAME = "NorentesFrameworkTestScenario";

	private long noHab = 0;
	private static final long noInd1 = 54321L;
	private static final long noInd2 = 12345L;
	private static final long noInd3 = 23456L;

	public NorentesFrameworkTestScenario() {
	}

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.EVENEMENT_TESTING;
	}

	@Override
	public String getName() {
		return NAME;
	}


	@Override
	public String getDescription() {
		return "Scenario qui permet de tester le framework des Norentes";
	}

	// Etape 1
	@Etape(id=1, descr="Mise en place des données")
	public void etape1() throws Exception {
		PersonnePhysique hab = addHabitant(noInd1);
		noHab = hab.getNumero();
	}
	@Check(id=1, descr="Vérification des données initiales")
	public void check1() throws Exception {
		PersonnePhysique hab = (PersonnePhysique)tiersDAO.get(noHab);
		assertEquals(noInd1, hab.getNumeroIndividu(), "");
	}

	// Etape 2
	@Etape(id=2, descr="1ere Modification des données")
	public void etape2() throws Exception {
		PersonnePhysique hab = (PersonnePhysique)tiersDAO.get(noHab);
		hab.setNumeroIndividu(noInd2);
	}
	@Check(id=2, descr="Vérification des 1ere données")
	public void check2() throws Exception {
		PersonnePhysique hab = (PersonnePhysique)tiersDAO.get(noHab);
		assertEquals(noInd2, hab.getNumeroIndividu(), "");
	}

	// Etape 3
	@Etape(id=3, descr="2eme Modification des données")
	public void etape3() throws Exception {
		PersonnePhysique hab = (PersonnePhysique)tiersDAO.get(noHab);
		hab.setNumeroIndividu(noInd3);
	}
	@Check(id=3, descr="Vérification des 2eme données")
	public void check3() throws Exception {
		PersonnePhysique hab = (PersonnePhysique)tiersDAO.get(noHab);
		assertEquals(noInd3, hab.getNumeroIndividu(), "");
	}





}
