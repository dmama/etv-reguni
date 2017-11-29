package ch.vd.uniregctb.foncier;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

public class DegrevementICITest extends WithoutSpringTest {

	// big décimals avec précision de 2 décimales
	private static final BigDecimal ZERO = BigDecimal.valueOf(0, 2);
	private static final BigDecimal CENT = BigDecimal.valueOf(10000, 2);
	private static final BigDecimal SOIXANTE = BigDecimal.valueOf(6000, 2);
	private static final BigDecimal HUITANTE = BigDecimal.valueOf(8000, 2);

	/**
	 * [SIFISC-26123] si aucune valeur n'est arrêtée, le dégrèvement final doit être 0% (et non pas une valeur nulle)
	 */
	@Test
	public void testGetPourcentageDegrevementAucuneValeurArretee() throws Exception {
		final DegrevementICI deg = new DegrevementICI();
		Assert.assertEquals(ZERO, deg.getPourcentageDegrevement());

		deg.setLoiLogement(new DonneesLoiLogement());
		deg.setPropreUsage(new DonneesUtilisation());
		deg.setLocation(new DonneesUtilisation());
		Assert.assertEquals(ZERO, deg.getPourcentageDegrevement());

		deg.setLoiLogement(new DonneesLoiLogement(Boolean.TRUE, null, null, BigDecimal.TEN));
		deg.setPropreUsage(new DonneesUtilisation(100L, 100L, 100L, BigDecimal.valueOf(50L), null));
		deg.setLocation(new DonneesUtilisation(100L, 100L, 100L, BigDecimal.valueOf(50L), null));
		Assert.assertEquals(ZERO, deg.getPourcentageDegrevement());
	}

	@Test
	public void testGetPourcentageDegrevementSansControleLoiLogement() throws Exception {
		final DegrevementICI deg = new DegrevementICI();
		deg.setPropreUsage(new DonneesUtilisation(100L, 100L, 100L, BigDecimal.valueOf(80L), BigDecimal.valueOf(60L)));
		deg.setLocation(new DonneesUtilisation(167L, 167L, 167L, BigDecimal.valueOf(20L), BigDecimal.valueOf(40L)));

		Assert.assertEquals(SOIXANTE, deg.getPourcentageDegrevement());
	}

	@Test
	public void testGetPourcentageDegrevementAvecControleLoiLogement() throws Exception {
		final DegrevementICI deg = new DegrevementICI();
		deg.setPropreUsage(new DonneesUtilisation(100L, 100L, 100L, BigDecimal.valueOf(80L), BigDecimal.valueOf(60L)));
		deg.setLocation(new DonneesUtilisation(167L, 167L, 167L, BigDecimal.valueOf(20L), BigDecimal.valueOf(40L)));
		deg.setLoiLogement(new DonneesLoiLogement(Boolean.TRUE, date(2009, 1, 1), null, BigDecimal.valueOf(50L)));

		// 60% + (50% * 40%) = 80%
		Assert.assertEquals(HUITANTE, deg.getPourcentageDegrevement());
	}

	@Test
	public void testGetPourcentageDegrevementAvecControleLoiLogementNonActive() throws Exception {
		final DegrevementICI deg = new DegrevementICI();
		deg.setPropreUsage(new DonneesUtilisation(100L, 100L, 100L, BigDecimal.valueOf(80L), BigDecimal.valueOf(60L)));
		deg.setLocation(new DonneesUtilisation(167L, 167L, 167L, BigDecimal.valueOf(20L), BigDecimal.valueOf(40L)));
		deg.setLoiLogement(new DonneesLoiLogement(Boolean.FALSE, date(2009, 1, 1), null, BigDecimal.valueOf(50L)));

		Assert.assertEquals(SOIXANTE, deg.getPourcentageDegrevement());
	}

	@Test
	public void testGetPourcentageDegrevementSansLocationArreteeSeulementPropreUsage() throws Exception {
		final DegrevementICI deg = new DegrevementICI();
		deg.setPropreUsage(new DonneesUtilisation(100L, 100L, 100L, BigDecimal.valueOf(80L), BigDecimal.valueOf(60L)));
		deg.setLocation(new DonneesUtilisation(167L, 167L, 167L, BigDecimal.valueOf(20L), null));
		deg.setLoiLogement(new DonneesLoiLogement(Boolean.FALSE, null, null, null));

		Assert.assertEquals(SOIXANTE, deg.getPourcentageDegrevement());
	}

	@Test
	public void testGetPourcentageDegrevementSansPropreUsageArreteeSeulementLocation() throws Exception {
		final DegrevementICI deg = new DegrevementICI();
		deg.setPropreUsage(new DonneesUtilisation(100L, 100L, 100L, BigDecimal.valueOf(80L), null));
		deg.setLocation(new DonneesUtilisation(167L, 167L, 167L, BigDecimal.valueOf(20L), BigDecimal.valueOf(40L)));
		deg.setLoiLogement(new DonneesLoiLogement(Boolean.FALSE, null, null, null));

		Assert.assertEquals(SOIXANTE, deg.getPourcentageDegrevement());
	}

	@Test
	public void testGetPourcentageDegrevementAvecCapping() throws Exception {
		final DegrevementICI deg = new DegrevementICI();
		deg.setPropreUsage(new DonneesUtilisation(100L, 100L, 100L, BigDecimal.valueOf(80L), BigDecimal.valueOf(60L)));
		deg.setLocation(new DonneesUtilisation(167L, 167L, 167L, BigDecimal.valueOf(20L), BigDecimal.valueOf(80L)));
		deg.setLoiLogement(new DonneesLoiLogement(Boolean.TRUE, date(2009, 1, 1), null, BigDecimal.valueOf(70L)));

		// 60% + (70% * 80%) = 116% cappé à 100%
		Assert.assertEquals(CENT, deg.getPourcentageDegrevement());
	}

}
