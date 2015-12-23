package ch.vd.uniregctb.tiers;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

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
		for1.duplicate();

		ForFiscal for2 = for1.duplicate();
		for2.setDateDebut(RegDate.get(2012, 7, 5));

		Assert.isTrue(is(for1, for2));
	}

	@Test
	public void testDatesFinDifferent() throws Exception {

		ForFiscal for1 = new ForFiscalSecondaire();
		for1.setDateDebut(RegDate.get(2010, 6, 25));
		for1.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		for1.setNumeroOfsAutoriteFiscale(100);
		for1.setDateFin(null);
		for1.duplicate();

		ForFiscal for2 = for1.duplicate();
		for2.setDateFin(RegDate.get(2012, 7, 5));

		Assert.isTrue(is(for2, for1));

		for1.setDateFin(RegDate.get(2010, 1, 1));

		Assert.isTrue(is(for1, for2));
	}

	@Test
	public void testTypesDifferent() throws Exception {

		ForFiscal for1 = new ForFiscalSecondaire();
		for1.setDateDebut(RegDate.get(2010, 6, 25));
		for1.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		for1.setNumeroOfsAutoriteFiscale(100);
		for1.setDateFin(null);
		for1.duplicate();

		ForFiscal for2 = new ForFiscalPrincipalPM();
		for2.setDateDebut(for1.getDateDebut());
		for2.setDateFin(for1.getDateFin());
		for2.setTypeAutoriteFiscale(for1.getTypeAutoriteFiscale());
		for2.setNumeroOfsAutoriteFiscale(200);

		Assert.isTrue(is(for2, for1));
	}

	@Test
	public void testNoOfsDifferent() throws Exception {

		ForFiscal for1 = new ForFiscalSecondaire();
		for1.setDateDebut(RegDate.get(2010, 6, 25));
		for1.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		for1.setNumeroOfsAutoriteFiscale(100);
		for1.setDateFin(null);
		for1.duplicate();

		ForFiscal for2 = for1.duplicate();
		for2.setNumeroOfsAutoriteFiscale(200);

		Assert.isTrue(is(for1, for2));
	}

	@Test
	public void testDatesDebutPrimeSurDateFin() throws Exception {

		ForFiscal for1 = new ForFiscalSecondaire();
		for1.setDateDebut(RegDate.get(2010, 6, 25));
		for1.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		for1.setNumeroOfsAutoriteFiscale(100);
		for1.setDateFin(null);
		for1.duplicate();

		ForFiscal for2 = for1.duplicate();
		for2.setDateFin(RegDate.get(2012, 12, 12));

		// A ce stade, la date de fin décide, les dates de début étant identiques
		Assert.isTrue(is(for2, for1));

		for2.setDateDebut(RegDate.get(2012, 7, 5));
		Assert.isTrue(is(for1, for2));
	}

	protected boolean is(ForFiscal for1, ForFiscal for2) {
		return 0 > comparator.compare(for1, for2);
	}
}