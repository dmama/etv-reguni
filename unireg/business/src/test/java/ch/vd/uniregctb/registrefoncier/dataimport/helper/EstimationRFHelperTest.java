package ch.vd.uniregctb.registrefoncier.dataimport.helper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;

import ch.vd.capitastra.grundstueck.AmtlicheBewertung;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.UniregJUnit4Runner;
import ch.vd.uniregctb.registrefoncier.EstimationRF;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("Duplicates")
@RunWith(UniregJUnit4Runner.class)
public class EstimationRFHelperTest {

	/**
	 * Ce test vérifie que deux estimations identiques sont bien considérées égales.
	 */
	@Test
	public void testDataEquals() throws Exception {

		final EstimationRF estimation = new EstimationRF();
		estimation.setMontant(120000L);
		estimation.setReference("2015");
		estimation.setDateInscription(RegDate.get(2015, 7, 1));
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
		estimation.setDateInscription(RegDate.get(2015, 7, 1));
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
		estimation.setDateInscription(RegDate.get(2015, 7, 1));
		estimation.setEnRevision(false);

		final AmtlicheBewertung amtlicheBewertung = new AmtlicheBewertung();
		amtlicheBewertung.setAmtlicherWert(120000L);
		amtlicheBewertung.setProtokollNr("RG94");
		amtlicheBewertung.setProtokollDatum(RegDate.get(2015, 7, 1));
		amtlicheBewertung.setProtokollGueltig(true);

		assertFalse(EstimationRFHelper.dataEquals(estimation, amtlicheBewertung));
	}

	/**
	 * Ce test vérifie que deux estimations qui diffèrent sur la date d'inscription sont bien considérées inégales.
	 */
	@Test
	public void testDataEqualsDatesInscriptionDifferentes() throws Exception {

		final EstimationRF estimation = new EstimationRF();
		estimation.setMontant(120000L);
		estimation.setReference("2015");
		estimation.setDateInscription(RegDate.get(2015, 7, 1));
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
		estimation.setDateInscription(RegDate.get(2015, 7, 1));
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
		assertEquals(RegDate.get(2015, 7, 1), estimation.getDateInscription());
		assertFalse(estimation.isEnRevision());
	}

	/**
	 * [SIFISC-22995] Ce test vérifie que la date de début métier est bien le 1er janvier de l'année de référence fiscale.
	 */
	@Test
	public void testDetermineDateDebutMetier() throws Exception {
		assertNull(EstimationRFHelper.determineDateDebutMetier(null, null));
		assertEquals(RegDate.get(2017, 1, 1), EstimationRFHelper.determineDateDebutMetier("2017", null));
		assertEquals(RegDate.get(2017, 1, 1), EstimationRFHelper.determineDateDebutMetier("23.03.2017", null));
		assertEquals(RegDate.get(2017, 1, 1), EstimationRFHelper.determineDateDebutMetier(null, RegDate.get(2017, 12, 14)));
	}

	@Test
	public void testDetermineDatesFinMetier() throws Exception {

		// cas liste vide
		{
			final List<EstimationRF> estimations = Collections.emptyList();
			EstimationRFHelper.determineDatesFinMetier(estimations);
		}

		// cas liste avec une seule valeur
		{
			final EstimationRF e1 = newEstimationRF(RegDate.get(1970, 1, 1));
			final List<EstimationRF> estimations = Collections.singletonList(e1);
			EstimationRFHelper.determineDatesFinMetier(estimations);
			assertNull(e1.getDateFinMetier());
		}

		// cas liste avec deux valeurs
		{
			final EstimationRF e1 = newEstimationRF(RegDate.get(1970, 1, 1));
			final EstimationRF e2 = newEstimationRF(RegDate.get(1980, 1, 1));
			final List<EstimationRF> estimations = Arrays.asList(e1, e2);
			EstimationRFHelper.determineDatesFinMetier(estimations);
			assertEquals(RegDate.get(1979, 12, 31), e1.getDateFinMetier());
			assertNull(e2.getDateFinMetier());
		}

		// cas liste avec trois valeurs
		{
			final EstimationRF e1 = newEstimationRF(RegDate.get(1970, 1, 1));
			final EstimationRF e2 = newEstimationRF(RegDate.get(1980, 1, 1));
			final EstimationRF e3 = newEstimationRF(RegDate.get(1980, 1, 2));
			final List<EstimationRF> estimations = Arrays.asList(e1, e2, e3);
			EstimationRFHelper.determineDatesFinMetier(estimations);
			assertEquals(RegDate.get(1979, 12, 31), e1.getDateFinMetier());
			assertEquals(RegDate.get(1980, 1, 1), e2.getDateFinMetier());
			assertNull(e3.getDateFinMetier());
		}

		// cas liste avec trois valeurs dans le désordre
		{
			final EstimationRF e1 = newEstimationRF(RegDate.get(1970, 1, 1));
			final EstimationRF e2 = newEstimationRF(RegDate.get(1980, 1, 1));
			final EstimationRF e3 = newEstimationRF(RegDate.get(1980, 1, 2));
			final List<EstimationRF> estimations = Arrays.asList(e3, e2, e1);
			EstimationRFHelper.determineDatesFinMetier(estimations);
			assertEquals(RegDate.get(1979, 12, 31), e1.getDateFinMetier());
			assertEquals(RegDate.get(1980, 1, 1), e2.getDateFinMetier());
			assertNull(e3.getDateFinMetier());
		}
	}

	@NotNull
	private static EstimationRF newEstimationRF(RegDate dateDebutMetier) {
		final EstimationRF e1 = new EstimationRF();
		e1.setDateDebut(dateDebutMetier);
		e1.setDateDebutMetier(dateDebutMetier);
		return e1;
	}

	/**
	 * [SIFISC-22995] Ce test vérifie que l'année de référence est bien déterminée à partir du code de référence et de la date d'inscription.
	 */
	@Test
	public void testGetAnneeReference() throws Exception {

		// cas des valeurs nulles
		assertNull(EstimationRFHelper.getAnneeReference(null, null).orElse(null));

		// test avec des valeurs trouvées dans les données du RF
		assertNull(EstimationRFHelper.getAnneeReference("-", null).orElse(null));
		assertNull(EstimationRFHelper.getAnneeReference("", null).orElse(null));
		assertNull(EstimationRFHelper.getAnneeReference("EF", null).orElse(null));
		assertEquals(Integer.valueOf(2001), EstimationRFHelper.getAnneeReference("EF 01", null).orElse(null));
		assertEquals(Integer.valueOf(2001), EstimationRFHelper.getAnneeReference("EF01", null).orElse(null));
		assertEquals(Integer.valueOf(1994), EstimationRFHelper.getAnneeReference("RF1994", null).orElse(null));
		assertNull(EstimationRFHelper.getAnneeReference("RG", null).orElse(null));
		assertEquals(Integer.valueOf(1994), EstimationRFHelper.getAnneeReference("RG 1994", null).orElse(null));
		assertEquals(Integer.valueOf(1992), EstimationRFHelper.getAnneeReference("RG 92", null).orElse(null));
		assertEquals(Integer.valueOf(1991), EstimationRFHelper.getAnneeReference("RG1991", null).orElse(null));
		assertEquals(Integer.valueOf(1972), EstimationRFHelper.getAnneeReference("RG72", null).orElse(null));
		assertEquals(Integer.valueOf(1991), EstimationRFHelper.getAnneeReference("Rg91", null).orElse(null));
		assertEquals(Integer.valueOf(1996), EstimationRFHelper.getAnneeReference("rg96", null).orElse(null));
		assertNull(EstimationRFHelper.getAnneeReference("0", null).orElse(null));
		assertNull(EstimationRFHelper.getAnneeReference("014", null).orElse(null));
		assertEquals(Integer.valueOf(212), EstimationRFHelper.getAnneeReference("0212", null).orElse(null));
		assertEquals(Integer.valueOf(2013), EstimationRFHelper.getAnneeReference("06.11.2013", null).orElse(null));
		assertNull(EstimationRFHelper.getAnneeReference("15", null).orElse(null));
		assertNull(EstimationRFHelper.getAnneeReference("150000", null).orElse(null));
		assertEquals(Integer.valueOf(1507), EstimationRFHelper.getAnneeReference("1507", null).orElse(null));
		assertEquals(Integer.valueOf(1700), EstimationRFHelper.getAnneeReference("1700", null).orElse(null));
		assertEquals(Integer.valueOf(1918), EstimationRFHelper.getAnneeReference("1918", null).orElse(null));
		assertEquals(Integer.valueOf(1972), EstimationRFHelper.getAnneeReference("1972RG", null).orElse(null));
		assertEquals(Integer.valueOf(1977), EstimationRFHelper.getAnneeReference("1977RP", null).orElse(null));
		assertEquals(Integer.valueOf(1978), EstimationRFHelper.getAnneeReference("1978rév.", null).orElse(null));
		assertEquals(Integer.valueOf(2016), EstimationRFHelper.getAnneeReference("19.8.2016", null).orElse(null));
		assertEquals(Integer.valueOf(1994), EstimationRFHelper.getAnneeReference("1994", null).orElse(null));
		assertEquals(Integer.valueOf(1994), EstimationRFHelper.getAnneeReference("1994RG", null).orElse(null));
		assertEquals(Integer.valueOf(1996), EstimationRFHelper.getAnneeReference("1996RF", null).orElse(null));
		assertEquals(Integer.valueOf(1996), EstimationRFHelper.getAnneeReference("1996T", null).orElse(null));
		assertEquals(Integer.valueOf(1996), EstimationRFHelper.getAnneeReference("1996T.", null).orElse(null));
		assertEquals(Integer.valueOf(1997), EstimationRFHelper.getAnneeReference("1997", null).orElse(null));
		assertEquals(Integer.valueOf(1997), EstimationRFHelper.getAnneeReference("1997T.", null).orElse(null));
		assertEquals(Integer.valueOf(1998), EstimationRFHelper.getAnneeReference("1998enrévision", null).orElse(null));
		assertEquals(Integer.valueOf(1998), EstimationRFHelper.getAnneeReference("1998T.", null).orElse(null));
		assertNull(EstimationRFHelper.getAnneeReference("19993", null).orElse(null));
		assertNull(EstimationRFHelper.getAnneeReference("200", null).orElse(null));
		assertEquals(Integer.valueOf(2000), EstimationRFHelper.getAnneeReference("2000enrévision", null).orElse(null));
		assertEquals(Integer.valueOf(2000), EstimationRFHelper.getAnneeReference("2000T.", null).orElse(null));
		assertNull(EstimationRFHelper.getAnneeReference("20002", null).orElse(null));
		assertEquals(Integer.valueOf(2001), EstimationRFHelper.getAnneeReference("2001", null).orElse(null));
		assertEquals(Integer.valueOf(2001), EstimationRFHelper.getAnneeReference("2001§", null).orElse(null));
		assertNull(EstimationRFHelper.getAnneeReference("20012", null).orElse(null));
		assertEquals(Integer.valueOf(2004), EstimationRFHelper.getAnneeReference("2004.", null).orElse(null));
		assertNull(EstimationRFHelper.getAnneeReference("20152016", null).orElse(null));
		assertNull(EstimationRFHelper.getAnneeReference("2'13", null).orElse(null));
		assertEquals(Integer.valueOf(2213), EstimationRFHelper.getAnneeReference("2213", null).orElse(null));
		assertEquals(Integer.valueOf(2440), EstimationRFHelper.getAnneeReference("2440", null).orElse(null));
		assertEquals(Integer.valueOf(2914), EstimationRFHelper.getAnneeReference("2914", null).orElse(null));
		assertEquals(Integer.valueOf(2915), EstimationRFHelper.getAnneeReference("2915", null).orElse(null));
		assertNull(EstimationRFHelper.getAnneeReference("297000", null).orElse(null));
		assertNull(EstimationRFHelper.getAnneeReference("973000", null).orElse(null));

		// cas de la valeur de référence inutilisable
		assertEquals(Integer.valueOf(2017), EstimationRFHelper.getAnneeReference(null, RegDate.get(2017, 2, 3)).orElse(null));
		assertEquals(Integer.valueOf(2017), EstimationRFHelper.getAnneeReference("blablabla", RegDate.get(2017, 2, 3)).orElse(null));
	}
}