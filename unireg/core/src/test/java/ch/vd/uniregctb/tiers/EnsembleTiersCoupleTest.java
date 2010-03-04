package ch.vd.uniregctb.tiers;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.CoreDAOTest;
import ch.vd.uniregctb.type.Sexe;

public class EnsembleTiersCoupleTest extends CoreDAOTest{

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
	}
	@Test
	public void testEstComposeDe2Personnes(){
		PersonnePhysique alain = addNonHabitant("alain", "proviste", RegDate.get(1956, 3, 15), Sexe.MASCULIN);
		PersonnePhysique mylaine = addNonHabitant("mylaine", "micoton", RegDate.get(1960, 3, 12), Sexe.FEMININ);
		EnsembleTiersCouple couple = addEnsembleTiersCouple(alain,mylaine,RegDate.get(1985,5, 8) );

		assertEquals(couple.estComposeDe(alain, mylaine),true);
		assertEquals(couple.estComposeDe(null, mylaine),false);
		assertEquals(couple.estComposeDe(alain, null),false);



	}

	@Test
	public void testEstComposeDeAlain(){

		PersonnePhysique alain = addNonHabitant("alain", "proviste", RegDate.get(1956, 3, 15), Sexe.MASCULIN);
		PersonnePhysique mylaine = addNonHabitant("mylaine", "micoton", RegDate.get(1960, 3, 12), Sexe.FEMININ);
		EnsembleTiersCouple couple = addEnsembleTiersCouple(alain,null,RegDate.get(1985,5, 8) );

		assertEquals(couple.estComposeDe(alain, mylaine),false);
		assertEquals(couple.estComposeDe(null, mylaine),false);
		assertEquals(couple.estComposeDe(alain, null),true);



	}


	@Test
	public void testEstComposeDeMylaine(){

		PersonnePhysique alain = addNonHabitant("alain", "proviste", RegDate.get(1956, 3, 15), Sexe.MASCULIN);
		PersonnePhysique mylaine = addNonHabitant("mylaine", "micoton", RegDate.get(1960, 3, 12), Sexe.FEMININ);
		EnsembleTiersCouple couple = addEnsembleTiersCouple(mylaine,null,RegDate.get(1985,5, 8) );

		assertEquals(couple.estComposeDe(alain, mylaine),false);
		assertEquals(couple.estComposeDe(null, mylaine),true);
		assertEquals(couple.estComposeDe(alain, null),false);



	}
	@Test
	public void testContientMylaine(){

		PersonnePhysique alain = addNonHabitant("alain", "proviste", RegDate.get(1956, 3, 15), Sexe.MASCULIN);
		PersonnePhysique mylaine = addNonHabitant("mylaine", "micoton", RegDate.get(1960, 3, 12), Sexe.FEMININ);
		PersonnePhysique Atanase = addNonHabitant("Atanase", "illard", RegDate.get(1960, 3, 12), Sexe.FEMININ);
		EnsembleTiersCouple couple = addEnsembleTiersCouple(mylaine,null,RegDate.get(1985,5, 8) );

		assertEquals(couple.contient(Atanase),false);
		assertEquals(couple.contient(mylaine),true);
		assertEquals(couple.contient(alain),false);



	}

	@Test
	public void testContientAlain(){

		PersonnePhysique alain = addNonHabitant("alain", "proviste", RegDate.get(1956, 3, 15), Sexe.MASCULIN);
		PersonnePhysique mylaine = addNonHabitant("mylaine", "micoton", RegDate.get(1960, 3, 12), Sexe.FEMININ);
		PersonnePhysique Atanase = addNonHabitant("Atanase", "illard", RegDate.get(1960, 3, 12), Sexe.FEMININ);
		EnsembleTiersCouple couple = addEnsembleTiersCouple(alain,null,RegDate.get(1985,5, 8) );

		assertEquals(couple.contient(Atanase),false);
		assertEquals(couple.contient(mylaine),false);
		assertEquals(couple.contient(alain),true);



	}
	@Test
	public void testContientMylaineEtAlain(){

		PersonnePhysique alain = addNonHabitant("alain", "proviste", RegDate.get(1956, 3, 15), Sexe.MASCULIN);
		PersonnePhysique mylaine = addNonHabitant("mylaine", "micoton", RegDate.get(1960, 3, 12), Sexe.FEMININ);
		PersonnePhysique Atanase = addNonHabitant("Atanase", "illard", RegDate.get(1960, 3, 12), Sexe.FEMININ);
		EnsembleTiersCouple couple = addEnsembleTiersCouple(mylaine,alain,RegDate.get(1985,5, 8) );

		assertEquals(couple.contient(Atanase),false);
		assertEquals(couple.contient(mylaine),true);
		assertEquals(couple.contient(alain),true);



	}

}
