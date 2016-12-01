package ch.vd.uniregctb.registrefoncier.helper;

import org.junit.Test;
import org.junit.runner.RunWith;

import ch.vd.capitastra.grundstueck.GeburtsDatum;
import ch.vd.capitastra.grundstueck.Gemeinschaft;
import ch.vd.capitastra.grundstueck.GemeinschaftsArt;
import ch.vd.capitastra.grundstueck.JuristischePersonUnterart;
import ch.vd.capitastra.grundstueck.JuristischePersonstamm;
import ch.vd.capitastra.grundstueck.NatuerlichePersonstamm;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.UniregJUnit4Runner;
import ch.vd.uniregctb.registrefoncier.CollectivitePubliqueRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.PersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.TypeCommunaute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(UniregJUnit4Runner.class)
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

	/**
	 * Ce test vérifie que deux communautés identiques sont bien considérées comme égales.
	 */
	@Test
	public void testEqualsCommunaute() throws Exception {

		final CommunauteRF comm = new CommunauteRF();
		comm.setIdRF("3893728273382823");
		comm.setType(TypeCommunaute.COMMUNAUTE_HEREDITAIRE);

		final Gemeinschaft gemeinschaft = new Gemeinschaft();
		gemeinschaft.setGemeinschatID("3893728273382823");
		gemeinschaft.setArt(GemeinschaftsArt.ERBENGEMEINSCHAFT);

		assertTrue(AyantDroitRFHelper.dataEquals(comm, gemeinschaft));
	}

	/**
	 * Ce test vérifie que deux communautés avec des types différents sont bien considérées comme inégales.
	 */
	@Test
	public void testNotEqualsCommunaute() throws Exception {

		final CommunauteRF comm = new CommunauteRF();
		comm.setIdRF("3893728273382823");
		comm.setType(TypeCommunaute.COMMUNAUTE_HEREDITAIRE);

		final Gemeinschaft gemeinschaft = new Gemeinschaft();
		gemeinschaft.setGemeinschatID("3893728273382823");
		gemeinschaft.setArt(GemeinschaftsArt.GUETERGEMEINSCHAFT);

		assertFalse(AyantDroitRFHelper.dataEquals(comm, gemeinschaft));
	}

	@Test
	public void testNewAyantDroitPP() throws Exception {

		final NatuerlichePersonstamm natuerliche = new NatuerlichePersonstamm();
		natuerliche.setPersonstammID("3893728273382823");
		natuerliche.setNoRF(3727L);
		natuerliche.setNrIROLE(827288022L);
		natuerliche.setName("Nom");
		natuerliche.setVorname("Prénom");
		natuerliche.setGeburtsdatum(new GeburtsDatum(23, 1, 1956));

		final PersonnePhysiqueRF pp = (PersonnePhysiqueRF) AyantDroitRFHelper.newAyantDroitRF(natuerliche);
		assertEquals("3893728273382823", pp.getIdRF());
		assertEquals(3727L, pp.getNoRF());
		assertEquals(Long.valueOf(827288022L), pp.getNoContribuable());
		assertEquals("Nom", pp.getNom());
		assertEquals("Prénom", pp.getPrenom());
		assertEquals(RegDate.get(1956, 1, 23), pp.getDateNaissance());
	}

	@Test
	public void testNewAyantDroitPM() throws Exception {

		final JuristischePersonstamm juristische = new JuristischePersonstamm();
		juristische.setPersonstammID("48349384890202");
		juristische.setNoRF(3727L);
		juristische.setNrACI(827288022L);
		juristische.setName("Raison sociale");
		juristische.setUnterart(JuristischePersonUnterart.SCHWEIZERISCHE_JURISTISCHE_PERSON);

		final PersonneMoraleRF pp = (PersonneMoraleRF) AyantDroitRFHelper.newAyantDroitRF(juristische);
		assertEquals("48349384890202", pp.getIdRF());
		assertEquals(3727L, pp.getNoRF());
		assertEquals(Long.valueOf(827288022L), pp.getNoContribuable());
		assertEquals("Raison sociale", pp.getRaisonSociale());
	}

	@Test
	public void testNewAyantDroitColl() throws Exception {

		final JuristischePersonstamm juristische = new JuristischePersonstamm();
		juristische.setPersonstammID("574739202303482");
		juristische.setNoRF(3727L);
		juristische.setNrACI(827288022L);
		juristische.setName("Raison sociale");
		juristische.setUnterart(JuristischePersonUnterart.OEFFENTLICHE_KOERPERSCHAFT);

		final CollectivitePubliqueRF coll = (CollectivitePubliqueRF) AyantDroitRFHelper.newAyantDroitRF(juristische);
		assertEquals("574739202303482", coll.getIdRF());
		assertEquals(3727L, coll.getNoRF());
		assertEquals(Long.valueOf(827288022L), coll.getNoContribuable());
		assertEquals("Raison sociale", coll.getRaisonSociale());
	}
}
