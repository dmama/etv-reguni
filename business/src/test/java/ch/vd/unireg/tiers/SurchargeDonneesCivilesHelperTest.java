package ch.vd.unireg.tiers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.emory.mathcs.backport.java.util.Collections;
import org.junit.Test;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.type.TypeAutoriteFiscale;

/**
 * @author Raphaël Marmier, 2016-04-22, <raphael.marmier@vd.ch>
 */
public class SurchargeDonneesCivilesHelperTest extends WithoutSpringTest {

	/**
		Cas nominal. On est en présence d'une surcharge qui s'arrête à la veille de notre date de valeur, typique d'une entreprise migrée.
	 */
	@Test
	public void testTronqueSimplePeriode() throws Exception {
		final DateRangeHelper.Range range = new DateRangeHelper.Range(date(2016, 4, 8), date(2016, 4, 10));
		final RegDate dateValeur = date(2016, 4, 11);
		final List<DomicileEtablissement> entites =  Collections.singletonList(new DomicileEtablissement(date(2016, 4, 1), date(2016, 4, 10), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, 119, null));

		Set<DomicileEtablissement> aSauver = SurchargeDonneesCivilesHelper.tronqueSurchargeFiscale(range, dateValeur, entites, "domicile");

		Assert.isTrue(entites.size() == 1);
		Assert.isTrue(entites.get(0).isAnnule());
		Assert.isTrue(aSauver.size() == 1);
		Assert.isEqual(date(2016, 4, 1), aSauver.iterator().next().getDateDebut());
		Assert.isEqual(date(2016, 4, 7), aSauver.iterator().next().getDateFin());
	}

	/**
	 Cas simple. On est en présence d'une surcharge entièrement inclue dans notre prériode.
	 */
	@Test
	public void testTronqueSimplePeriodeIncluse() throws Exception {
		final DateRangeHelper.Range range = new DateRangeHelper.Range(date(2016, 4, 8), date(2016, 4, 10));
		final RegDate dateValeur = date(2016, 4, 11);
		final List<DomicileEtablissement> entites =  Collections.singletonList(new DomicileEtablissement(date(2016, 4, 9), date(2016, 4, 10), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, 119, null));

		Set<DomicileEtablissement> aSauver = SurchargeDonneesCivilesHelper.tronqueSurchargeFiscale(range, dateValeur, entites, "domicile");

		Assert.isTrue(entites.size() == 1);
		Assert.isTrue(entites.get(0).isAnnule());
		Assert.isEqual(date(2016, 4, 9), entites.get(0).getDateDebut());
		Assert.isEqual(date(2016, 4, 10), entites.get(0).getDateFin());
		Assert.isTrue(aSauver.isEmpty());
	}

	/**
		Cas simple. Pas de surcharge présente.
	 */
	@Test
	public void testTronqueVide() throws Exception {
		final DateRangeHelper.Range range = new DateRangeHelper.Range(date(2016, 4, 8), date(2016, 4, 10));
		final RegDate dateValeur = date(2016, 4, 11);
		final List<DomicileEtablissement> entites =  Collections.emptyList();

		Set<DomicileEtablissement> aSauver = SurchargeDonneesCivilesHelper.tronqueSurchargeFiscale(range, dateValeur, entites, "domicile");

		Assert.isTrue(entites.isEmpty());
		Assert.isTrue(aSauver.isEmpty());
	}

	/**
	 Cas complexe. On est en présence d'une surcharge composée de plusieurs périodes.
	 */
	@Test
	public void testTronqueComplexePeriode() throws Exception {
		final DateRangeHelper.Range range = new DateRangeHelper.Range(date(2016, 4, 8), date(2016, 4, 10));
		final RegDate dateValeur = date(2016, 4, 11);
		final List<DomicileEtablissement> entites = new ArrayList<>();
		final DomicileEtablissement domicile1 = new DomicileEtablissement(date(2016, 4, 1), date(2016, 4, 8), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, 119, null);
		entites.add(domicile1);
		final DomicileEtablissement domicile2 = new DomicileEtablissement(date(2016, 4, 9), date(2016, 4, 10), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, 119, null);
		entites.add(domicile2);

		Set<DomicileEtablissement> aSauver = SurchargeDonneesCivilesHelper.tronqueSurchargeFiscale(range, dateValeur, entites, "domicile");

		Assert.isTrue(entites.size() == 2);
		Assert.isEqual(date(2016, 4, 1), domicile1.getDateDebut());
		Assert.isEqual(date(2016, 4, 8), domicile1.getDateFin());
		Assert.isEqual(date(2016, 4, 9), domicile2.getDateDebut());
		Assert.isEqual(date(2016, 4, 10), domicile2.getDateFin());

		Assert.isTrue(aSauver.size() == 1);
		Assert.isEqual(date(2016, 4, 1), aSauver.iterator().next().getDateDebut());
		Assert.isEqual(date(2016, 4, 7), aSauver.iterator().next().getDateFin());
		Assert.isTrue(domicile1.isAnnule());
		Assert.isTrue(domicile2.isAnnule());
	}

	/**
	 Cas en erreur. On est en présence d'une surcharge complexe dont une des périodes s'étend pendant et/ou delà de notre date de valeur.
	 */
	@Test
	public void testTronqueComplexePeriodeEnErreur() throws Exception {
		final DateRangeHelper.Range range = new DateRangeHelper.Range(date(2016, 4, 8), date(2016, 4, 10));
		final RegDate dateValeur = date(2016, 4, 11);
		final List<DomicileEtablissement> entites = new ArrayList<>();
		final DomicileEtablissement domicile1 = new DomicileEtablissement(date(2016, 4, 1), date(2016, 4, 8), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, 119, null);
		entites.add(domicile1);
		final DomicileEtablissement domicile2 = new DomicileEtablissement(date(2016, 4, 9), null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, 119, null);
		entites.add(domicile2);

		try {
			 SurchargeDonneesCivilesHelper.tronqueSurchargeFiscale(range, dateValeur, entites, "domicile");
		} catch (TiersException e) {
			Assert.isEqual(
					String.format("Impossible d'appliquer les données civiles car une surcharge fiscale de domicile est présente en date du %s.",
					              RegDateHelper.dateToDisplayString(dateValeur)), e.getMessage());
			Assert.isTrue(entites.size() == 2);
			Assert.isFalse(domicile1.isAnnule());
			Assert.isFalse(domicile2.isAnnule());
			return;
		}
		Assert.fail("La période dépasse la date de valeur, mais pas d'exception remontée comme ca devrait être le cas.");
	}
}