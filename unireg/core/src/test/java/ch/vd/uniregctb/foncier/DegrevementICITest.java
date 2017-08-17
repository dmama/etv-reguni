package ch.vd.uniregctb.foncier;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

public class DegrevementICITest extends WithoutSpringTest {

	@Test
	public void testGetPourcentageDegrevementAucuneValeurArretee() throws Exception {
		final DegrevementICI deg = new DegrevementICI();
		Assert.assertNull(deg.getPourcentageDegrevement());

		deg.setLoiLogement(new DonneesLoiLogement());
		deg.setPropreUsage(new DonneesUtilisation());
		deg.setLocation(new DonneesUtilisation());
		Assert.assertNull(deg.getPourcentageDegrevement());

		deg.setLoiLogement(new DonneesLoiLogement(Boolean.TRUE, null, null, BigDecimal.TEN));
		deg.setPropreUsage(new DonneesUtilisation(100L, 100L, 100L, BigDecimal.valueOf(50L), null));
		deg.setLocation(new DonneesUtilisation(100L, 100L, 100L, BigDecimal.valueOf(50L), null));
		Assert.assertNull(deg.getPourcentageDegrevement());
	}

	@Test
	public void testGetPourcentageDegrevementSansControleLoiLogement() throws Exception {
		final DegrevementICI deg = new DegrevementICI();
		deg.setPropreUsage(new DonneesUtilisation(100L, 100L, 100L, BigDecimal.valueOf(80L), BigDecimal.valueOf(60L)));
		deg.setLocation(new DonneesUtilisation(167L, 167L, 167L, BigDecimal.valueOf(20L), BigDecimal.valueOf(40L)));

		Assert.assertEquals(BigDecimal.valueOf(60L).stripTrailingZeros(), deg.getPourcentageDegrevement());
	}

	@Test
	public void testGetPourcentageDegrevementAvecControleLoiLogement() throws Exception {
		final DegrevementICI deg = new DegrevementICI();
		deg.setPropreUsage(new DonneesUtilisation(100L, 100L, 100L, BigDecimal.valueOf(80L), BigDecimal.valueOf(60L)));
		deg.setLocation(new DonneesUtilisation(167L, 167L, 167L, BigDecimal.valueOf(20L), BigDecimal.valueOf(40L)));
		deg.setLoiLogement(new DonneesLoiLogement(Boolean.TRUE, date(2009, 1, 1), null, BigDecimal.valueOf(50L)));

		// 60% + (50% * 40%) = 80%
		Assert.assertEquals(BigDecimal.valueOf(80L).stripTrailingZeros(), deg.getPourcentageDegrevement());
	}

	@Test
	public void testGetPourcentageDegrevementAvecControleLoiLogementNonActive() throws Exception {
		final DegrevementICI deg = new DegrevementICI();
		deg.setPropreUsage(new DonneesUtilisation(100L, 100L, 100L, BigDecimal.valueOf(80L), BigDecimal.valueOf(60L)));
		deg.setLocation(new DonneesUtilisation(167L, 167L, 167L, BigDecimal.valueOf(20L), BigDecimal.valueOf(40L)));
		deg.setLoiLogement(new DonneesLoiLogement(Boolean.FALSE, date(2009, 1, 1), null, BigDecimal.valueOf(50L)));

		Assert.assertEquals(BigDecimal.valueOf(60L).stripTrailingZeros(), deg.getPourcentageDegrevement());
	}

	@Test
	public void testGetPourcentageDegrevementSansLocationArreteeSeulementPropreUsage() throws Exception {
		final DegrevementICI deg = new DegrevementICI();
		deg.setPropreUsage(new DonneesUtilisation(100L, 100L, 100L, BigDecimal.valueOf(80L), BigDecimal.valueOf(60L)));
		deg.setLocation(new DonneesUtilisation(167L, 167L, 167L, BigDecimal.valueOf(20L), null));
		deg.setLoiLogement(new DonneesLoiLogement(Boolean.FALSE, null, null, null));

		Assert.assertEquals(BigDecimal.valueOf(60L).stripTrailingZeros(), deg.getPourcentageDegrevement());
	}

	@Test
	public void testGetPourcentageDegrevementSansPropreUsageArreteeSeulementLocation() throws Exception {
		final DegrevementICI deg = new DegrevementICI();
		deg.setPropreUsage(new DonneesUtilisation(100L, 100L, 100L, BigDecimal.valueOf(80L), null));
		deg.setLocation(new DonneesUtilisation(167L, 167L, 167L, BigDecimal.valueOf(20L), BigDecimal.valueOf(40L)));
		deg.setLoiLogement(new DonneesLoiLogement(Boolean.FALSE, null, null, null));

		Assert.assertEquals(BigDecimal.valueOf(60L).stripTrailingZeros(), deg.getPourcentageDegrevement());
	}

	@Test
	public void testGetPourcentageDegrevementAvecCapping() throws Exception {
		final DegrevementICI deg = new DegrevementICI();
		deg.setPropreUsage(new DonneesUtilisation(100L, 100L, 100L, BigDecimal.valueOf(80L), BigDecimal.valueOf(60L)));
		deg.setLocation(new DonneesUtilisation(167L, 167L, 167L, BigDecimal.valueOf(20L), BigDecimal.valueOf(80L)));
		deg.setLoiLogement(new DonneesLoiLogement(Boolean.TRUE, date(2009, 1, 1), null, BigDecimal.valueOf(70L)));

		// 60% + (70% * 80%) = 116% cappé à 100%
		Assert.assertEquals(BigDecimal.valueOf(100L).stripTrailingZeros(), deg.getPourcentageDegrevement());
	}

}
