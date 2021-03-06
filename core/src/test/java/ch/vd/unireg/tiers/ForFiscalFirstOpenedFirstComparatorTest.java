package ch.vd.unireg.tiers;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.type.TypeAutoriteFiscale;

import static org.junit.Assert.assertTrue;

/**
 * @author Raphaël Marmier, 2015-12-21
 */
public class ForFiscalFirstOpenedFirstComparatorTest extends WithoutSpringTest {

	ForFiscalFirstOpenedFirstComparator comparator = new ForFiscalFirstOpenedFirstComparator();

	@Test
	public void testDatesDebutDifferent() throws Exception {

		ForFiscal for1 = new ForFiscalSecondaire();
		for1.setDateDebut(RegDate.get(2010, 6, 25));
		for1.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		for1.setNumeroOfsAutoriteFiscale(100);
		for1.setDateFin(null);

		ForFiscal for2 = for1.duplicate();
		for2.setDateDebut(RegDate.get(2012, 7, 5));

		assertTrue(orderIsConserved(for1, for2));
	}

	@Test
	public void testDatesFinDifferent() throws Exception {

		ForFiscal for1 = new ForFiscalSecondaire();
		for1.setDateDebut(RegDate.get(2010, 6, 25));
		for1.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		for1.setNumeroOfsAutoriteFiscale(200);
		for1.setDateFin(null);

		ForFiscal for2 = for1.duplicate();
		for2.setDateFin(RegDate.get(2012, 7, 5));
		for2.setNumeroOfsAutoriteFiscale(100); // Garantir l'échec du test en cas de ratage de la comparaison.

		assertTrue(orderIsReversed(for1, for2));

		for1.setDateFin(RegDate.get(2011, 1, 1));

		assertTrue(orderIsConserved(for1, for2));
	}

	@Test
	public void testTypesDifferent() throws Exception {

		ForFiscal for1 = new ForFiscalSecondaire();
		for1.setDateDebut(RegDate.get(2010, 6, 25));
		for1.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		for1.setDateFin(null);
		for1.setNumeroOfsAutoriteFiscale(100);

		ForFiscal for2 = new ForFiscalPrincipalPM();
		for2.setDateDebut(RegDate.get(2010, 6, 25));
		for2.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		for2.setDateFin(null);
		for2.setNumeroOfsAutoriteFiscale(200); // Garantir l'échec du test en cas de ratage de la comparaison.

		assertTrue(orderIsReversed(for1, for2));
		assertTrue(orderIsConserved(for2, for1));
	}

	@Test
	public void testNoOfsDifferent() throws Exception {

		ForFiscal for1 = new ForFiscalSecondaire();
		for1.setDateDebut(RegDate.get(2010, 6, 25));
		for1.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		for1.setNumeroOfsAutoriteFiscale(100);
		for1.setDateFin(null);

		ForFiscal for2 = for1.duplicate();
		for2.setNumeroOfsAutoriteFiscale(200);

		assertTrue(orderIsConserved(for1, for2));

		for1.setNumeroOfsAutoriteFiscale(200);
		for2.setNumeroOfsAutoriteFiscale(100);

		assertTrue(orderIsReversed(for1, for2));
	}

	@Test
	public void testNoOfsDifferentForsPrincipaux() throws Exception {

		ForFiscal for1 = new ForFiscalPrincipalPM();
		for1.setDateDebut(RegDate.get(2010, 6, 25));
		for1.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		for1.setNumeroOfsAutoriteFiscale(100);
		for1.setDateFin(null);

		ForFiscal for2 = for1.duplicate();
		for2.setNumeroOfsAutoriteFiscale(200);

		assertTrue(orderIsConserved(for1, for2));

		for1.setNumeroOfsAutoriteFiscale(200);
		for2.setNumeroOfsAutoriteFiscale(100);

		assertTrue(orderIsReversed(for1, for2));
	}

	@Test
	public void testDatesDebutPrimeSurDateFin() throws Exception {

		ForFiscal for1 = new ForFiscalSecondaire();
		for1.setDateDebut(RegDate.get(2010, 6, 25));
		for1.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		for1.setNumeroOfsAutoriteFiscale(100);
		for1.setDateFin(null);

		ForFiscal for2 = for1.duplicate();
		for2.setDateFin(RegDate.get(2012, 12, 12));

		// A ce stade, la date de fin décide, les dates de début étant identiques
		assertTrue(orderIsReversed(for1, for2));

		for2.setDateDebut(RegDate.get(2012, 7, 5));
		assertTrue(orderIsConserved(for1, for2));
	}

	protected boolean orderIsConserved(ForFiscal for1, ForFiscal for2) {
		return comparator.compare(for1, for2) == -1;
	}

	protected boolean orderIsReversed(ForFiscal for1, ForFiscal for2) {
		return comparator.compare(for1, for2) == 1;
	}
}