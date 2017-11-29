package ch.vd.uniregctb.foncier;

import java.math.BigDecimal;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

public class DegrevementICITest extends WithoutSpringTest {

	// big décimals avec précision de 2 décimales
	private static final BigDecimal ZERO = BigDecimal.valueOf(0, 2);
	private static final BigDecimal CENT = BigDecimal.valueOf(10000, 2);
	private static final BigDecimal VINGT = BigDecimal.valueOf(2000, 2);
	private static final BigDecimal TRENTE = BigDecimal.valueOf(3000, 2);
	private static final BigDecimal CINQUANTE = BigDecimal.valueOf(5000, 2);
	private static final BigDecimal SOIXANTE = BigDecimal.valueOf(6000, 2);
	private static final BigDecimal SEPTANTE = BigDecimal.valueOf(7000, 2);
	private static final BigDecimal SEPTANTE_CINQ = BigDecimal.valueOf(7500, 2);
	private static final BigDecimal HUITANTE = BigDecimal.valueOf(8000, 2);
	private static final BigDecimal CENT_TRENTE = BigDecimal.valueOf(13000, 2);

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

	/**
	 * [SIFISC-27250] Vérifie les calculs du pourcentage de dégrèvement dans les cas de base où les valeurs arrêtées sont renseignées et valides.
	 */
	@Test
	public void testGetPourcentageDegrevementCasDeBase() throws Exception {

		// usage propre=0 + location=0 => degrevement=0
		Assert.assertEquals(ZERO, createDegrevement(ZERO, ZERO, null, null).getPourcentageDegrevement());

		// usage propre=0 + location=100 => degrevement=0
		Assert.assertEquals(ZERO, createDegrevement(ZERO, CENT, null, null).getPourcentageDegrevement());

		// usage propre=100 + location=0 => degrevement=100
		Assert.assertEquals(CENT, createDegrevement(CENT, ZERO, null, null).getPourcentageDegrevement());

		// usage propre=20 + location=80 => degrevement=20
		Assert.assertEquals(VINGT, createDegrevement(VINGT, HUITANTE, null, null).getPourcentageDegrevement());

		// usage propre=80 + location=20 => degrevement=80
		Assert.assertEquals(HUITANTE, createDegrevement(HUITANTE, VINGT, null, null).getPourcentageDegrevement());
	}

	/**
	 * [SIFISC-27250] Vérifie les calculs du pourcentage de dégrèvement dans les cas de base où les valeurs arrêtées sont renseignées et valides <b>et</b> avec un pourcentage à caractère sociale sur la location.
	 */
	@Test
	public void testGetPourcentageDegrevementCasDeBaseAvecPourcentageSocial() throws Exception {

	    // usage propre=20 + location=80 + social=50 *sans* contrôle de l'office du logement => degrevement=60
		Assert.assertEquals(VINGT, createDegrevement(VINGT, HUITANTE, false, CINQUANTE).getPourcentageDegrevement());

	    // usage propre=20 + location=80 + social=50 *avec* contrôle de l'office du logement => degrevement=60
		Assert.assertEquals(SOIXANTE, createDegrevement(VINGT, HUITANTE, true, CINQUANTE).getPourcentageDegrevement());
	}

	/**
	 * [SIFISC-27250] Vérifie les calculs du pourcentage de dégrèvement dans les cas de base où les valeurs arrêtées dépassent les 100% cumulés.
	 */
	@Test
	public void testGetPourcentageDegrevementCasAvecPlusDeCentPourcentArretes() throws Exception {

		// usage propre=70 + location=60 => degrevement=70
		Assert.assertEquals(SEPTANTE, createDegrevement(SEPTANTE, SOIXANTE, null, null).getPourcentageDegrevement());

		// usage propre=60 + location=70 => degrevement=60
		Assert.assertEquals(SOIXANTE, createDegrevement(SOIXANTE, SEPTANTE, null, null).getPourcentageDegrevement());

		// usage propre=130 + location=20 => degrevement=100
		Assert.assertEquals(CENT, createDegrevement(CENT_TRENTE, VINGT, null, null).getPourcentageDegrevement());

		// usage propre=20 + location=130 => degrevement=20
		Assert.assertEquals(VINGT, createDegrevement(VINGT, CENT_TRENTE, null, null).getPourcentageDegrevement());
	}

	/**
	 * [SIFISC-27250] Vérifie les calculs du pourcentage de dégrèvement dans les cas de base où les valeurs arrêtées dépassent les 100% cumulés <b>et</b> avec un pourcentage à caractère sociale sur la location..
	 */
	@Test
	public void testGetPourcentageDegrevementCasAvecPlusDeCentPourcentArretesEtAvecPourcentageSocial() throws Exception {

		// usage propre=70 + location=60 + social=100  *sans* contrôle de l'office du logement => degrevement=70
		Assert.assertEquals(SEPTANTE, createDegrevement(SEPTANTE, SOIXANTE, false, CENT).getPourcentageDegrevement());

		// usage propre=70 + location=60 + social=100 *avec* contrôle de l'office du logement => degrevement=60
		Assert.assertEquals(CENT, createDegrevement(SEPTANTE, SOIXANTE, true, CENT).getPourcentageDegrevement());

		// usage propre=130 + location=20 + social=100 *avec* contrôle de l'office du logement => degrevement=100
		Assert.assertEquals(CENT, createDegrevement(CENT_TRENTE, VINGT, true, CENT).getPourcentageDegrevement());
	}

	/**
	 * [SIFISC-27250] Vérifie les calculs du pourcentage de dégrèvement dans les cas de base où une ou pluieurs valeurs ne sont pas arrêtées.
	 */
	@Test
	public void testGetPourcentageDegrevementCasUnOuPlusieursValeursPasArretees() throws Exception {

		// usage propre=null + location=null => degrevement=0
		Assert.assertEquals(ZERO, createDegrevement(null, null, null, null).getPourcentageDegrevement());

		// usage propre=0 + location=null => degrevement=0
		Assert.assertEquals(ZERO, createDegrevement(ZERO, null, null, null).getPourcentageDegrevement());

		// usage propre=null + location=0 => degrevement=0
//		Assert.assertEquals(ZERO, createDegrevement(null, ZERO, null, null).getPourcentageDegrevement());

		// usage propre=75 + location=null => degrevement=75
		Assert.assertEquals(SEPTANTE_CINQ, createDegrevement(SEPTANTE_CINQ, null, null, null).getPourcentageDegrevement());

		// usage propre=null + location=75 => degrevement=0
//		Assert.assertEquals(ZERO, createDegrevement(null, SEPTANTE_CINQ, null, null).getPourcentageDegrevement());

		// usage propre=30 + location=null *sans* contrôle de l'office du logement => degrevement=30
		Assert.assertEquals(TRENTE, createDegrevement(TRENTE, null, false, CINQUANTE).getPourcentageDegrevement());

		// usage propre=30 + location=null *avec* contrôle de l'office du logement => degrevement=30
//		Assert.assertEquals(TRENTE, createDegrevement(TRENTE, null, true, CINQUANTE).getPourcentageDegrevement());

		// usage propre=20 + location=80 *avec* contrôle de l'office du logement => degrevement=30
		Assert.assertEquals(TRENTE, createDegrevement(TRENTE, null, true, null).getPourcentageDegrevement());
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

	@NotNull
	private static DegrevementICI createDegrevement(BigDecimal usagePropre, BigDecimal location, Boolean controleLogement, BigDecimal pourcentageCaractereSocial) {
		final DegrevementICI deg = new DegrevementICI();
		deg.setLoiLogement(new DonneesLoiLogement(controleLogement, null, null, pourcentageCaractereSocial));
		deg.setPropreUsage(new DonneesUtilisation(null, null, null, null, usagePropre));
		deg.setLocation(new DonneesUtilisation(null, null, null, null, location));
		return deg;
	}
}
