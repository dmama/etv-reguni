package ch.vd.uniregctb.registrefoncier.helper;

import org.junit.Test;

import ch.vd.capitastra.grundstueck.GrundstueckNummer;
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.SituationRF;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SituationRFHelperTest {

	@Test
	public void testDataEquals() throws Exception {

		// deux situations égales
		final CommuneRF commune = new CommuneRF();
		commune.setNoRf(2233);

		final SituationRF situation = new SituationRF();
		situation.setCommune(commune);
		situation.setNoParcelle(109);
		situation.setIndex1(17);

		final GrundstueckNummer grundstueckNummer = new GrundstueckNummer();
		grundstueckNummer.setBfsNr(2233);
		grundstueckNummer.setStammNr(109);
		grundstueckNummer.setIndexNr1(17);

		assertTrue(SituationRFHelper.dataEquals(situation, grundstueckNummer));
	}

	/**
	 * Ce cas test le cas où deux situations ne sont pas égales à cause de communes différentes.
	 */
	@Test
	public void testDataEqualsDifferentesCommunes() throws Exception {

		final CommuneRF commune = new CommuneRF();
		commune.setNoRf(23);

		// deux situations avec des communes différentes
		final SituationRF situation = new SituationRF();
		situation.setCommune(commune);
		situation.setNoParcelle(109);
		situation.setIndex1(17);

		final GrundstueckNummer grundstueckNummer = new GrundstueckNummer();
		grundstueckNummer.setBfsNr(2233);
		grundstueckNummer.setStammNr(109);
		grundstueckNummer.setIndexNr1(17);

		assertFalse(SituationRFHelper.dataEquals(situation, grundstueckNummer));
	}

	/**
	 * Ce cas test le cas où deux situations ne sont pas égales à cause de parcelles différentes.
	 */
	@Test
	public void testDataEqualsDifferentesParcelles() throws Exception {

		final CommuneRF commune = new CommuneRF();
		commune.setNoRf(2233);

		// deux situations avec des parcelles différentes
		final SituationRF situation = new SituationRF();
		situation.setCommune(commune);
		situation.setNoParcelle(109);
		situation.setIndex1(17);

		final GrundstueckNummer grundstueckNummer = new GrundstueckNummer();
		grundstueckNummer.setBfsNr(2233);
		grundstueckNummer.setStammNr(2);
		grundstueckNummer.setIndexNr1(17);

		assertFalse(SituationRFHelper.dataEquals(situation, grundstueckNummer));
	}

	/**
	 * Ce cas test le cas où deux situations ne sont pas égales à cause d'indexes différents.
	 */
	@Test
	public void testDataEqualsDifferentsIndexes() throws Exception {

		final CommuneRF commune = new CommuneRF();
		commune.setNoRf(2233);

		// deux situations avec des indexes différents
		final SituationRF situation = new SituationRF();
		situation.setCommune(commune);
		situation.setNoParcelle(109);
		situation.setIndex1(17);
		situation.setIndex1(9);

		final GrundstueckNummer grundstueckNummer = new GrundstueckNummer();
		grundstueckNummer.setBfsNr(2233);
		grundstueckNummer.setStammNr(109);
		grundstueckNummer.setIndexNr1(17);

		assertFalse(SituationRFHelper.dataEquals(situation, grundstueckNummer));
	}

	@Test
	public void testNewSituationRF() throws Exception {

		final GrundstueckNummer grundstueckNummer = new GrundstueckNummer();
		grundstueckNummer.setBfsNr(2233);
		grundstueckNummer.setStammNr(109);
		grundstueckNummer.setIndexNr1(17);
		grundstueckNummer.setIndexNr2(3);
		grundstueckNummer.setIndexNr3(122233);

		final CommuneRF commune = new CommuneRF();
		commune.setNoRf(2233);

		final SituationRF situation = SituationRFHelper.newSituationRF(grundstueckNummer, (no) -> commune);
		assertEquals(2233, situation.getCommune().getNoRf());
		assertEquals(109, situation.getNoParcelle());
		assertEquals(Integer.valueOf(17), situation.getIndex1());
		assertEquals(Integer.valueOf(3), situation.getIndex2());
		assertEquals(Integer.valueOf(122233), situation.getIndex3());
	}
}