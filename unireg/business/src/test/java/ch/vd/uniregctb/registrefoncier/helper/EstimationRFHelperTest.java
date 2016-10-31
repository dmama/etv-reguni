package ch.vd.uniregctb.registrefoncier.helper;

import org.junit.Test;

import ch.vd.capitastra.grundstueck.AmtlicheBewertung;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.registrefoncier.EstimationRF;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EstimationRFHelperTest {

	/**
	 * Ce test vérifie que deux estimations identiques sont bien considérées égales.
	 */
	@Test
	public void testDataEquals() throws Exception {

		final EstimationRF estimation = new EstimationRF();
		estimation.setMontant(120000L);
		estimation.setReference("2015");
		estimation.setDateEstimation(RegDate.get(2015, 7, 1));
		estimation.setEnRevision(false);

		final AmtlicheBewertung amtlicheBewertung = new AmtlicheBewertung();
		amtlicheBewertung.setAmtlicherWert(120000L);
		amtlicheBewertung.setProtokollNr("2015");
		amtlicheBewertung.setProtokollDatum(RegDate.get(2015, 7, 1));
		amtlicheBewertung.setProtokollGueltig(true);

		assertTrue(EstimationRFHelper.dataEquals(estimation, amtlicheBewertung));
	}

	/**
	 * Ce test vérifie les cas de nullités entre estimations.
	 */
	@Test
	public void testDataEqualsNullNotNull() throws Exception {
		assertTrue(EstimationRFHelper.dataEquals(null, (EstimationRF) null));
		assertFalse(EstimationRFHelper.dataEquals(null, new EstimationRF()));
		assertFalse(EstimationRFHelper.dataEquals(new EstimationRF(), (EstimationRF) null));
	}

	/**
	 * Ce test vérifie que deux estimations qui diffèrent sur le montant sont bien considérées inégales.
	 */
	@Test
	public void testDataEqualsMontantsDifferents() throws Exception {

		final EstimationRF estimation = new EstimationRF();
		estimation.setMontant(120000L);
		estimation.setReference("2015");
		estimation.setDateEstimation(RegDate.get(2015, 7, 1));
		estimation.setEnRevision(false);

		final AmtlicheBewertung amtlicheBewertung = new AmtlicheBewertung();
		amtlicheBewertung.setAmtlicherWert(500000L);
		amtlicheBewertung.setProtokollNr("2015");
		amtlicheBewertung.setProtokollDatum(RegDate.get(2015, 7, 1));
		amtlicheBewertung.setProtokollGueltig(true);

		assertFalse(EstimationRFHelper.dataEquals(estimation, amtlicheBewertung));
	}

	/**
	 * Ce test vérifie que deux estimations qui diffèrent sur la référence sont bien considérées inégales.
	 */
	@Test
	public void testDataEqualsReferencesDifferentes() throws Exception {

		final EstimationRF estimation = new EstimationRF();
		estimation.setMontant(120000L);
		estimation.setReference("2015");
		estimation.setDateEstimation(RegDate.get(2015, 7, 1));
		estimation.setEnRevision(false);

		final AmtlicheBewertung amtlicheBewertung = new AmtlicheBewertung();
		amtlicheBewertung.setAmtlicherWert(120000L);
		amtlicheBewertung.setProtokollNr("RG94");
		amtlicheBewertung.setProtokollDatum(RegDate.get(2015, 7, 1));
		amtlicheBewertung.setProtokollGueltig(true);

		assertFalse(EstimationRFHelper.dataEquals(estimation, amtlicheBewertung));
	}

	/**
	 * Ce test vérifie que deux estimations qui diffèrent sur la date d'estimation sont bien considérées inégales.
	 */
	@Test
	public void testDataEqualsDatesEstimationDifferentes() throws Exception {

		final EstimationRF estimation = new EstimationRF();
		estimation.setMontant(120000L);
		estimation.setReference("2015");
		estimation.setDateEstimation(RegDate.get(2015, 7, 1));
		estimation.setEnRevision(false);

		final AmtlicheBewertung amtlicheBewertung = new AmtlicheBewertung();
		amtlicheBewertung.setAmtlicherWert(120000L);
		amtlicheBewertung.setProtokollNr("2015");
		amtlicheBewertung.setProtokollDatum(RegDate.get(2015, 3, 17));
		amtlicheBewertung.setProtokollGueltig(true);

		assertFalse(EstimationRFHelper.dataEquals(estimation, amtlicheBewertung));
	}

	/**
	 * Ce test vérifie que deux estimations qui diffèrent sur l'état en révision sont bien considérées inégales.
	 */
	@Test
	public void testDataEqualsEtatEnRevisionDifferentes() throws Exception {

		final EstimationRF estimation = new EstimationRF();
		estimation.setMontant(120000L);
		estimation.setReference("2015");
		estimation.setDateEstimation(RegDate.get(2015, 7, 1));
		estimation.setEnRevision(true);

		final AmtlicheBewertung amtlicheBewertung = new AmtlicheBewertung();
		amtlicheBewertung.setAmtlicherWert(120000L);
		amtlicheBewertung.setProtokollNr("2015");
		amtlicheBewertung.setProtokollDatum(RegDate.get(2015, 7, 1));
		amtlicheBewertung.setProtokollGueltig(true);

		assertFalse(EstimationRFHelper.dataEquals(estimation, amtlicheBewertung));
	}

	@Test
	public void testNewEstimationRF() throws Exception {

		final AmtlicheBewertung amtlicheBewertung = new AmtlicheBewertung();
		amtlicheBewertung.setAmtlicherWert(120000L);
		amtlicheBewertung.setProtokollNr("2015");
		amtlicheBewertung.setProtokollDatum(RegDate.get(2015, 7, 1));
		amtlicheBewertung.setProtokollGueltig(true);

		final EstimationRF estimation = EstimationRFHelper.newEstimationRF(amtlicheBewertung);
		assertEquals(Long.valueOf(120000L), estimation.getMontant());
		assertEquals("2015", estimation.getReference());
		assertEquals(RegDate.get(2015, 7, 1), estimation.getDateEstimation());
		assertFalse(estimation.isEnRevision());
	}
}