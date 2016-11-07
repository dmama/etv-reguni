package ch.vd.uniregctb.registrefoncier.helper;

import org.junit.Test;

import ch.vd.capitastra.grundstueck.GeburtsDatum;
import ch.vd.capitastra.grundstueck.JuristischePersonUnterart;
import ch.vd.capitastra.grundstueck.JuristischePersonstamm;
import ch.vd.capitastra.grundstueck.NatuerlichePersonstamm;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.registrefoncier.CollectivitePubliqueRF;
import ch.vd.uniregctb.registrefoncier.PersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AyantDroitRFHelperTest {

	/**
	 * Ce test vérifie que deux personnes physiques identiques sont bien considérées comme égales.
	 */
	@Test
	public void testEqualsPP() throws Exception {

		final PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
		pp.setIdRF("3893728273382823");
		pp.setNoRF(3727);
		pp.setNoContribuable(827288022L);
		pp.setNom("Nom");
		pp.setPrenom("Prénom");
		pp.setDateNaissance(RegDate.get(1956, 1, 23));

		final NatuerlichePersonstamm personStamm = new NatuerlichePersonstamm();
		personStamm.setPersonstammID("3893728273382823");
		personStamm.setNoRF(3727L);
		personStamm.setNrIROLE(827288022L);
		personStamm.setName("Nom");
		personStamm.setVorname("Prénom");
		personStamm.setGeburtsdatum(new GeburtsDatum(23, 1, 1956));

		assertTrue(AyantDroitRFHelper.dataEquals(pp, personStamm));
	}

	/**
	 * Ce test vérifie que deux personnes physiques avec des numéros de contribuable différents sont bien considérées comme inégales.
	 */
	@Test
	public void testNotEqualsPP() throws Exception {

		final PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
		pp.setIdRF("3893728273382823");
		pp.setNoRF(3727);
		pp.setNoContribuable(827288022L);
		pp.setNom("Nom");
		pp.setPrenom("Prénom");
		pp.setDateNaissance(RegDate.get(1956, 1, 23));

		final NatuerlichePersonstamm personStamm = new NatuerlichePersonstamm();
		personStamm.setPersonstammID("3893728273382823");
		personStamm.setNoRF(3727L);
		personStamm.setNrIROLE(123003045L);     // <-- changement de n° de contribuable
		personStamm.setName("Nom");
		personStamm.setVorname("Prénom");
		personStamm.setGeburtsdatum(new GeburtsDatum(23, 1, 1956));

		assertFalse(AyantDroitRFHelper.dataEquals(pp, personStamm));
	}

	/**
	 * Ce test vérifie que deux personnes morales identiques sont bien considérées comme égales.
	 */
	@Test
	public void testEqualsPM() throws Exception {

		final PersonneMoraleRF pm = new PersonneMoraleRF();
		pm.setIdRF("48349384890202");
		pm.setNoRF(3727);
		pm.setNoContribuable(827288022L);
		pm.setRaisonSociale("Raison sociale");

		final JuristischePersonstamm personStamm = new JuristischePersonstamm();
		personStamm.setPersonstammID("48349384890202");
		personStamm.setNoRF(3727L);
		personStamm.setNrACI(827288022L);
		personStamm.setName("Raison sociale");
		personStamm.setUnterart(JuristischePersonUnterart.SCHWEIZERISCHE_JURISTISCHE_PERSON);

		assertTrue(AyantDroitRFHelper.dataEquals(pm, personStamm));
	}

	/**
	 * Ce test vérifie que deux personnes morales avec des raisons sociales différentes sont bien considérées comme inégales.
	 */
	@Test
	public void testNotEqualsPM() throws Exception {

		final PersonneMoraleRF pm = new PersonneMoraleRF();
		pm.setIdRF("48349384890202");
		pm.setNoRF(3727);
		pm.setNoContribuable(827288022L);
		pm.setRaisonSociale("Raison sociale");

		final JuristischePersonstamm personStamm = new JuristischePersonstamm();
		personStamm.setPersonstammID("48349384890202");
		personStamm.setNoRF(3727L);
		personStamm.setNrACI(827288022L);
		personStamm.setName("Nouvelle raison sociale"); // <-- changement de raison sociale
		personStamm.setUnterart(JuristischePersonUnterart.SCHWEIZERISCHE_JURISTISCHE_PERSON);

		assertFalse(AyantDroitRFHelper.dataEquals(pm, personStamm));
	}

	/**
	 * Ce test vérifie que deux collectivités identiques sont bien considérées comme égales.
	 */
	@Test
	public void testEqualsCollectivitePublique() throws Exception {

		final CollectivitePubliqueRF coll = new CollectivitePubliqueRF();
		coll.setIdRF("574739202303482");
		coll.setNoRF(3727);
		coll.setNoContribuable(827288022L);
		coll.setRaisonSociale("Raison sociale");

		final JuristischePersonstamm personStamm = new JuristischePersonstamm();
		personStamm.setPersonstammID("574739202303482");
		personStamm.setNoRF(3727L);
		personStamm.setNrACI(827288022L);
		personStamm.setName("Raison sociale");
		personStamm.setUnterart(JuristischePersonUnterart.OEFFENTLICHE_KOERPERSCHAFT);

		assertTrue(AyantDroitRFHelper.dataEquals(coll, personStamm));
	}

	/**
	 * Ce test vérifie que deux collectivités publiques avec des raisons sociales différentes sont bien considérées comme inégales.
	 */
	@Test
	public void testNotEqualsCollectivitePublique() throws Exception {

		final CollectivitePubliqueRF coll = new CollectivitePubliqueRF();
		coll.setIdRF("574739202303482");
		coll.setNoRF(3727);
		coll.setNoContribuable(827288022L);
		coll.setRaisonSociale("Raison sociale");

		final JuristischePersonstamm personStamm = new JuristischePersonstamm();
		personStamm.setPersonstammID("574739202303482");
		personStamm.setNoRF(3727L);
		personStamm.setNrACI(827288022L);
		personStamm.setName("Nouvelle raison sociale"); // <-- changement de raison sociale
		personStamm.setUnterart(JuristischePersonUnterart.OEFFENTLICHE_KOERPERSCHAFT);

		assertFalse(AyantDroitRFHelper.dataEquals(coll, personStamm));
	}
}
