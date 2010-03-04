package ch.vd.uniregctb.evenement.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.Sexe;

public class EnsembleTiersCoupleTest extends BusinessTest{

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		tiersService = getBean(TiersService.class, "tiersService");
		serviceCivil.setUp(new DefaultMockServiceCivil());
	}
	@Test
	public void testEstComposeDe2Personnes(){
		PersonnePhysique alain = addNonHabitant("alain", "proviste", RegDate.get(1956, 3, 15), Sexe.MASCULIN);
		PersonnePhysique mylaine = addNonHabitant("mylaine", "micoton", RegDate.get(1960, 3, 12), Sexe.FEMININ);
		EnsembleTiersCouple couple = createEnsembleTiersCouple(alain,mylaine,RegDate.get(1985,5, 8) );

		assertEquals(couple.estComposeDe(alain, mylaine),true);
		assertEquals(couple.estComposeDe(null, mylaine),false);
		assertEquals(couple.estComposeDe(alain, null),false);



	}

	@Test
	public void testEstComposeDeAlain(){

		PersonnePhysique alain = addNonHabitant("alain", "proviste", RegDate.get(1956, 3, 15), Sexe.MASCULIN);
		PersonnePhysique mylaine = addNonHabitant("mylaine", "micoton", RegDate.get(1960, 3, 12), Sexe.FEMININ);
		EnsembleTiersCouple couple = createEnsembleTiersCouple(alain,null,RegDate.get(1985,5, 8) );

		assertEquals(couple.estComposeDe(alain, mylaine),false);
		assertEquals(couple.estComposeDe(null, mylaine),false);
		assertEquals(couple.estComposeDe(alain, null),true);



	}


	@Test
	public void testEstComposeDeMylaine(){

		PersonnePhysique alain = addNonHabitant("alain", "proviste", RegDate.get(1956, 3, 15), Sexe.MASCULIN);
		PersonnePhysique mylaine = addNonHabitant("mylaine", "micoton", RegDate.get(1960, 3, 12), Sexe.FEMININ);
		EnsembleTiersCouple couple = createEnsembleTiersCouple(mylaine,null,RegDate.get(1985,5, 8) );

		assertEquals(couple.estComposeDe(alain, mylaine),false);
		assertEquals(couple.estComposeDe(null, mylaine),true);
		assertEquals(couple.estComposeDe(alain, null),false);



	}
	@Test
	public void testContientMylaine(){

		PersonnePhysique alain = addNonHabitant("alain", "proviste", RegDate.get(1956, 3, 15), Sexe.MASCULIN);
		PersonnePhysique mylaine = addNonHabitant("mylaine", "micoton", RegDate.get(1960, 3, 12), Sexe.FEMININ);
		PersonnePhysique Atanase = addNonHabitant("Atanase", "illard", RegDate.get(1960, 3, 12), Sexe.FEMININ);
		EnsembleTiersCouple couple = createEnsembleTiersCouple(mylaine,null,RegDate.get(1985,5, 8) );

		assertEquals(couple.contient(Atanase),false);
		assertEquals(couple.contient(mylaine),true);
		assertEquals(couple.contient(alain),false);



	}

	@Test
	public void testContientAlain(){

		PersonnePhysique alain = addNonHabitant("alain", "proviste", RegDate.get(1956, 3, 15), Sexe.MASCULIN);
		PersonnePhysique mylaine = addNonHabitant("mylaine", "micoton", RegDate.get(1960, 3, 12), Sexe.FEMININ);
		PersonnePhysique Atanase = addNonHabitant("Atanase", "illard", RegDate.get(1960, 3, 12), Sexe.FEMININ);
		EnsembleTiersCouple couple = createEnsembleTiersCouple(alain,null,RegDate.get(1985,5, 8) );

		assertEquals(couple.contient(Atanase),false);
		assertEquals(couple.contient(mylaine),false);
		assertEquals(couple.contient(alain),true);



	}
	@Test
	public void testContientMylaineEtAlain(){

		PersonnePhysique alain = addNonHabitant("alain", "proviste", RegDate.get(1956, 3, 15), Sexe.MASCULIN);
		PersonnePhysique mylaine = addNonHabitant("mylaine", "micoton", RegDate.get(1960, 3, 12), Sexe.FEMININ);
		PersonnePhysique Atanase = addNonHabitant("Atanase", "illard", RegDate.get(1960, 3, 12), Sexe.FEMININ);
		EnsembleTiersCouple couple = createEnsembleTiersCouple(mylaine,alain,RegDate.get(1985,5, 8) );

		assertEquals(couple.contient(Atanase),false);
		assertEquals(couple.contient(mylaine),true);
		assertEquals(couple.contient(alain),true);



	}

}
