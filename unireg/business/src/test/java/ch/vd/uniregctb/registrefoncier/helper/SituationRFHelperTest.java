package ch.vd.uniregctb.registrefoncier.helper;

import org.junit.Test;

import ch.vd.capitastra.grundstueck.GrundstueckNummer;
import ch.vd.uniregctb.registrefoncier.SituationRF;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SituationRFHelperTest {

	@Test
	public void testSituationEquals() throws Exception {

		// deux situations égales
		final SituationRF situation = new SituationRF();
		situation.setNoRfCommune(2233);
		situation.setNoParcelle(109);
		situation.setIndex1(17);

		final GrundstueckNummer grundstueckNummer = new GrundstueckNummer();
		grundstueckNummer.setBfsNr(2233);
		grundstueckNummer.setStammNr(109);
		grundstueckNummer.setIndexNr1(17);

		assertTrue(SituationRFHelper.situationEquals(situation, grundstueckNummer));
	}

	/**
	 * Ce cas test le cas où deux situations ne sont pas égales à cause de communes différentes.
	 */
	@Test
	public void testSituationEqualsDifferentesCommunes() throws Exception {

		// deux situations égales
		final SituationRF situation = new SituationRF();
		situation.setNoRfCommune(23);
		situation.setNoParcelle(109);
		situation.setIndex1(17);

		final GrundstueckNummer grundstueckNummer = new GrundstueckNummer();
		grundstueckNummer.setBfsNr(2233);
		grundstueckNummer.setStammNr(109);
		grundstueckNummer.setIndexNr1(17);

		assertFalse(SituationRFHelper.situationEquals(situation, grundstueckNummer));
	}

	/**
	 * Ce cas test le cas où deux situations ne sont pas égales à cause de parcelles différentes.
	 */
	@Test
	public void testSituationEqualsDifferentesParcelles() throws Exception {

		// deux situations égales
		final SituationRF situation = new SituationRF();
		situation.setNoRfCommune(2233);
		situation.setNoParcelle(109);
		situation.setIndex1(17);

		final GrundstueckNummer grundstueckNummer = new GrundstueckNummer();
		grundstueckNummer.setBfsNr(2233);
		grundstueckNummer.setStammNr(2);
		grundstueckNummer.setIndexNr1(17);

		assertFalse(SituationRFHelper.situationEquals(situation, grundstueckNummer));
	}

	/**
	 * Ce cas test le cas où deux situations ne sont pas égales à cause d'indexes différents.
	 */
	@Test
	public void testSituationEqualsDifferentsIndexes() throws Exception {

		// deux situations égales
		final SituationRF situation = new SituationRF();
		situation.setNoRfCommune(2233);
		situation.setNoParcelle(109);
		situation.setIndex1(17);
		situation.setIndex1(9);

		final GrundstueckNummer grundstueckNummer = new GrundstueckNummer();
		grundstueckNummer.setBfsNr(2233);
		grundstueckNummer.setStammNr(109);
		grundstueckNummer.setIndexNr1(17);

		assertFalse(SituationRFHelper.situationEquals(situation, grundstueckNummer));
	}
}