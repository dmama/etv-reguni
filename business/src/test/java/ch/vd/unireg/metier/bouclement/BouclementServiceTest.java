package ch.vd.unireg.metier.bouclement;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.tiers.Bouclement;
import ch.vd.unireg.type.DayMonth;

/**
 * TODO pour le moment, on n'a pas besoin de spring et de tout ça, mais cela pourrait changer...
 */
public class BouclementServiceTest extends WithoutSpringTest {

	private BouclementService service;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		service = new BouclementServiceImpl();
	}

	private static Bouclement buildTransientBouclement(RegDate dateDebut, DayMonth ancrage, int periodeMois) {
		final Bouclement b = new Bouclement();
		b.setDateDebut(dateDebut);
		b.setAncrage(ancrage);
		b.setPeriodeMois(periodeMois);
		return b;
	}

	@Test
	public void testDateProchainBouclementSansBouclement() throws Exception {
		Assert.assertNull(service.getDateProchainBouclement(null, RegDate.get(), true));
		Assert.assertNull(service.getDateProchainBouclement(null, RegDate.get(), false));
		Assert.assertNull(service.getDateProchainBouclement(Collections.<Bouclement>emptyList(), RegDate.get(), true));
		Assert.assertNull(service.getDateProchainBouclement(Collections.<Bouclement>emptyList(), RegDate.get(), false));
	}

	@Test
	public void testDateDernierBouclementSansBouclement() throws Exception {
		Assert.assertNull(service.getDateDernierBouclement(null, RegDate.get(), true));
		Assert.assertNull(service.getDateDernierBouclement(null, RegDate.get(), false));
		Assert.assertNull(service.getDateDernierBouclement(Collections.<Bouclement>emptyList(), RegDate.get(), true));
		Assert.assertNull(service.getDateDernierBouclement(Collections.<Bouclement>emptyList(), RegDate.get(), false));
	}

	@Test
	public void testDateProchainBouclementReferenceAvant() throws Exception {
		// dates des bouclements attendues : 31.03.2015, 31.03.2016, ...
		final List<Bouclement> bouclements = Collections.singletonList(buildTransientBouclement(date(2015, 1, 1), DayMonth.get(3, 31), 12));

		Assert.assertEquals(date(2015, 3, 31), service.getDateProchainBouclement(bouclements, date(2014, 1, 1), true));
		Assert.assertEquals(date(2015, 3, 31), service.getDateProchainBouclement(bouclements, date(2014, 1, 1), false));
		Assert.assertEquals(date(2015, 3, 31), service.getDateProchainBouclement(bouclements, date(2014, 12, 31), true));
		Assert.assertEquals(date(2015, 3, 31), service.getDateProchainBouclement(bouclements, date(2014, 12, 31), false));
	}

	@Test
	public void testDateDernierBouclementReferenceAvant() throws Exception {
		// dates des bouclements attendues : 31.03.2015, 31.03.2016, ...
		final List<Bouclement> bouclements = Collections.singletonList(buildTransientBouclement(date(2015, 1, 1), DayMonth.get(3, 31), 12));

		Assert.assertNull(service.getDateDernierBouclement(bouclements, date(2014, 1, 1), true));
		Assert.assertNull(service.getDateDernierBouclement(bouclements, date(2014, 1, 1), false));
		Assert.assertNull(service.getDateDernierBouclement(bouclements, date(2014, 12, 31), true));
		Assert.assertNull(service.getDateDernierBouclement(bouclements, date(2014, 12, 31), false));
	}

	@Test
	public void testDateProchainBouclementDernierePeriodicite() throws Exception {
		// dates des bouclements attendues : 31.03.2015, 31.03.2016, ...
		final List<Bouclement> bouclements = Collections.singletonList(buildTransientBouclement(date(2015, 1, 1), DayMonth.get(3, 31), 12));

		Assert.assertEquals(date(2015, 3, 31), service.getDateProchainBouclement(bouclements, date(2015, 1, 1), true));
		Assert.assertEquals(date(2015, 3, 31), service.getDateProchainBouclement(bouclements, date(2015, 1, 1), false));
		Assert.assertEquals(date(2015, 3, 31), service.getDateProchainBouclement(bouclements, date(2015, 3, 31), true));
		Assert.assertEquals(date(2016, 3, 31), service.getDateProchainBouclement(bouclements, date(2015, 3, 31), false));
		Assert.assertEquals(date(2016, 3, 31), service.getDateProchainBouclement(bouclements, date(2015, 12, 31), true));
		Assert.assertEquals(date(2016, 3, 31), service.getDateProchainBouclement(bouclements, date(2015, 12, 31), false));
		Assert.assertEquals(date(2017, 3, 31), service.getDateProchainBouclement(bouclements, date(2016, 6, 30), true));
		Assert.assertEquals(date(2017, 3, 31), service.getDateProchainBouclement(bouclements, date(2016, 6, 30), false));
	}

	@Test
	public void testDateDernierBouclementPremierePeriodicite() throws Exception {
		// dates des bouclements attendues : 31.03.2015, 31.03.2016, ...
		final List<Bouclement> bouclements = Collections.singletonList(buildTransientBouclement(date(2015, 1, 1), DayMonth.get(3, 31), 12));

		Assert.assertNull(service.getDateDernierBouclement(bouclements, date(2015, 1, 1), true));
		Assert.assertNull(service.getDateDernierBouclement(bouclements, date(2015, 1, 1), false));
		Assert.assertEquals(date(2015, 3, 31), service.getDateDernierBouclement(bouclements, date(2015, 3, 31), true));
		Assert.assertNull(service.getDateDernierBouclement(bouclements, date(2015, 3, 31), false));
		Assert.assertEquals(date(2015, 3, 31), service.getDateDernierBouclement(bouclements, date(2015, 12, 31), true));
		Assert.assertEquals(date(2015, 3, 31), service.getDateDernierBouclement(bouclements, date(2015, 12, 31), false));
		Assert.assertEquals(date(2016, 3, 31), service.getDateDernierBouclement(bouclements, date(2016, 6, 30), true));
		Assert.assertEquals(date(2016, 3, 31), service.getDateDernierBouclement(bouclements, date(2016, 6, 30), false));
	}

	/**
	 * Simple = la période suivante a une date de début au lendemain d'une date de bouclement de la période précédente
	 */
	@Test
	public void testDateProchainBouclementAvecPeriodiciteSuivanteSimple() throws Exception {
		// dates des bouclements attendues : 31.03.2015, 31.12.2015, 31.12.2016, ...
		final List<Bouclement> bouclements = Arrays.asList(buildTransientBouclement(date(2015, 1, 1), DayMonth.get(3, 31), 12),
		                                                   buildTransientBouclement(date(2015, 4, 1), DayMonth.get(12, 31), 12));

		Assert.assertEquals(date(2015, 3, 31), service.getDateProchainBouclement(bouclements, date(2015, 1, 1), true));
		Assert.assertEquals(date(2015, 3, 31), service.getDateProchainBouclement(bouclements, date(2015, 1, 1), false));
		Assert.assertEquals(date(2015, 3, 31), service.getDateProchainBouclement(bouclements, date(2015, 3, 31), true));
		Assert.assertEquals(date(2015, 12, 31), service.getDateProchainBouclement(bouclements, date(2015, 3, 31), false));
		Assert.assertEquals(date(2015, 12, 31), service.getDateProchainBouclement(bouclements, date(2015, 5, 31), true));
		Assert.assertEquals(date(2015, 12, 31), service.getDateProchainBouclement(bouclements, date(2015, 5, 31), false));
		Assert.assertEquals(date(2015, 12, 31), service.getDateProchainBouclement(bouclements, date(2015, 12, 31), true));
		Assert.assertEquals(date(2016, 12, 31), service.getDateProchainBouclement(bouclements, date(2015, 12, 31), false));
		Assert.assertEquals(date(2016, 12, 31), service.getDateProchainBouclement(bouclements, date(2016, 6, 30), true));
		Assert.assertEquals(date(2016, 12, 31), service.getDateProchainBouclement(bouclements, date(2016, 6, 30), false));
	}

	/**
	 * Simple = les deux périodicités se suivent avec la date de début de la seconde au lendemain d'une date fournie par la première
	 */
	@Test
	public void testDateDernierBouclementAvecPeriodiciteSuivanteSimple() throws Exception {
		// dates des bouclements attendues : 31.03.2015, 31.12.2015, 31.12.2016, ...
		final List<Bouclement> bouclements = Arrays.asList(buildTransientBouclement(date(2015, 1, 1), DayMonth.get(3, 31), 12),
		                                                   buildTransientBouclement(date(2015, 4, 1), DayMonth.get(12, 31), 12));

		Assert.assertNull(service.getDateDernierBouclement(bouclements, date(2015, 1, 1), true));
		Assert.assertNull(service.getDateDernierBouclement(bouclements, date(2015, 1, 1), false));
		Assert.assertEquals(date(2015, 3, 31), service.getDateDernierBouclement(bouclements, date(2015, 3, 31), true));
		Assert.assertNull(service.getDateDernierBouclement(bouclements, date(2015, 3, 31), false));
		Assert.assertEquals(date(2015, 3, 31), service.getDateDernierBouclement(bouclements, date(2015, 5, 31), true));
		Assert.assertEquals(date(2015, 3, 31), service.getDateDernierBouclement(bouclements, date(2015, 5, 31), false));
		Assert.assertEquals(date(2015, 12, 31), service.getDateDernierBouclement(bouclements, date(2015, 12, 31), true));
		Assert.assertEquals(date(2015, 3, 31), service.getDateDernierBouclement(bouclements, date(2015, 12, 31), false));
		Assert.assertEquals(date(2015, 12, 31), service.getDateDernierBouclement(bouclements, date(2016, 6, 30), true));
		Assert.assertEquals(date(2015, 12, 31), service.getDateDernierBouclement(bouclements, date(2016, 6, 30), false));
		Assert.assertEquals(date(2016, 12, 31), service.getDateDernierBouclement(bouclements, date(2017, 6, 30), true));
		Assert.assertEquals(date(2016, 12, 31), service.getDateDernierBouclement(bouclements, date(2017, 6, 30), false));
	}

	@Test
	public void testDateDernierBouclementAncrageOnDateDebut() throws Exception {
		// dates des bouclements attendues 31.03.2013, 31.03.2014...
		final List<Bouclement> bouclements = Collections.singletonList(buildTransientBouclement(date(2013, 3, 31), DayMonth.get(3, 31), 12));

		Assert.assertNull(service.getDateDernierBouclement(bouclements, date(2013, 3, 12), true));
		Assert.assertNull(service.getDateDernierBouclement(bouclements, date(2013, 3, 12), false));
		Assert.assertNull(service.getDateDernierBouclement(bouclements, date(2013, 3, 30), true));
		Assert.assertNull(service.getDateDernierBouclement(bouclements, date(2013, 3, 30), false));
		Assert.assertEquals(date(2013, 3, 31), service.getDateDernierBouclement(bouclements, date(2013, 3, 31), true));
		Assert.assertNull(service.getDateDernierBouclement(bouclements, date(2013, 3, 31), false));
		Assert.assertEquals(date(2013, 3, 31), service.getDateDernierBouclement(bouclements, date(2013, 5, 12), true));
		Assert.assertEquals(date(2013, 3, 31), service.getDateDernierBouclement(bouclements, date(2013, 5, 12), false));
		Assert.assertEquals(date(2014, 3, 31), service.getDateDernierBouclement(bouclements, date(2014, 5, 12), true));
		Assert.assertEquals(date(2014, 3, 31), service.getDateDernierBouclement(bouclements, date(2014, 5, 12), false));
	}

	/**
	 * Un peu tordu = la période suivante a une date de début au beau milieu d'un cycle de la période précédente
	 */
	@Test
	public void testDateProchainBouclementAvecPeriodiciteSuivanteUnPeuTordu() throws Exception {
		// dates des bouclements attendues : 31.03.2015, 31.12.2015, 31.12.2016
		final List<Bouclement> bouclements = Arrays.asList(buildTransientBouclement(date(2015, 1, 1), DayMonth.get(3, 31), 12),
		                                                   buildTransientBouclement(date(2015, 12, 1), DayMonth.get(12, 31), 12));

		Assert.assertEquals(date(2015, 3, 31), service.getDateProchainBouclement(bouclements, date(2015, 1, 1), true));
		Assert.assertEquals(date(2015, 3, 31), service.getDateProchainBouclement(bouclements, date(2015, 1, 1), false));
		Assert.assertEquals(date(2015, 3, 31), service.getDateProchainBouclement(bouclements, date(2015, 3, 31), true));
		Assert.assertEquals(date(2015, 12, 31), service.getDateProchainBouclement(bouclements, date(2015, 3, 31), false));
		Assert.assertEquals(date(2015, 12, 31), service.getDateProchainBouclement(bouclements, date(2015, 5, 31), true));
		Assert.assertEquals(date(2015, 12, 31), service.getDateProchainBouclement(bouclements, date(2015, 5, 31), false));
		Assert.assertEquals(date(2015, 12, 31), service.getDateProchainBouclement(bouclements, date(2015, 12, 31), true));
		Assert.assertEquals(date(2016, 12, 31), service.getDateProchainBouclement(bouclements, date(2015, 12, 31), false));
		Assert.assertEquals(date(2016, 12, 31), service.getDateProchainBouclement(bouclements, date(2016, 6, 30), true));
		Assert.assertEquals(date(2016, 12, 31), service.getDateProchainBouclement(bouclements, date(2016, 6, 30), false));
	}

	/**
	 * Un peu tordu = la période suivante a une date de début au beau milieu d'un cycle de la période précédente
	 */
	@Test
	public void testDateDernierBouclementAvecPeriodiciteSuivanteUnPeuTordu() throws Exception {
		// dates des bouclements attendues : 31.03.2015, 31.12.2015, 31.12.2016, ...
		final List<Bouclement> bouclements = Arrays.asList(buildTransientBouclement(date(2015, 1, 1), DayMonth.get(3, 31), 12),
		                                                   buildTransientBouclement(date(2015, 12, 1), DayMonth.get(12, 31), 12));

		Assert.assertNull(service.getDateDernierBouclement(bouclements, date(2015, 1, 1), true));
		Assert.assertNull(service.getDateDernierBouclement(bouclements, date(2015, 1, 1), false));
		Assert.assertEquals(date(2015, 3, 31), service.getDateDernierBouclement(bouclements, date(2015, 3, 31), true));
		Assert.assertNull(service.getDateDernierBouclement(bouclements, date(2015, 3, 31), false));
		Assert.assertEquals(date(2015, 3, 31), service.getDateDernierBouclement(bouclements, date(2015, 5, 31), true));
		Assert.assertEquals(date(2015, 3, 31), service.getDateDernierBouclement(bouclements, date(2015, 5, 31), false));
		Assert.assertEquals(date(2015, 12, 31), service.getDateDernierBouclement(bouclements, date(2015, 12, 31), true));
		Assert.assertEquals(date(2015, 3, 31), service.getDateDernierBouclement(bouclements, date(2015, 12, 31), false));
		Assert.assertEquals(date(2015, 12, 31), service.getDateDernierBouclement(bouclements, date(2016, 6, 30), true));
		Assert.assertEquals(date(2015, 12, 31), service.getDateDernierBouclement(bouclements, date(2016, 6, 30), false));
		Assert.assertEquals(date(2016, 12, 31), service.getDateDernierBouclement(bouclements, date(2017, 6, 30), true));
		Assert.assertEquals(date(2016, 12, 31), service.getDateDernierBouclement(bouclements, date(2017, 6, 30), false));
	}

	/**
	 * Franchement tordu = la période suivante a une date de début au beau milieu d'un cycle de la période précédente, et est tout de suite remplacée par une autre
	 */
	@Test
	public void testDateProchainBouclementAvecPeriodiciteSuivanteFranchementTordu() throws Exception {
		// dates des bouclements attendues : 31.03.2015, 30.09.2016, 30.09.2017 ...
		final List<Bouclement> bouclements = Arrays.asList(buildTransientBouclement(date(2015, 1, 1), DayMonth.get(3, 31), 12),
		                                                   buildTransientBouclement(date(2015, 12, 1), DayMonth.get(6, 30), 12),
		                                                   buildTransientBouclement(date(2016, 5, 1), DayMonth.get(9, 30), 12));

		Assert.assertEquals(date(2015, 3, 31), service.getDateProchainBouclement(bouclements, date(2015, 1, 1), true));
		Assert.assertEquals(date(2015, 3, 31), service.getDateProchainBouclement(bouclements, date(2015, 1, 1), false));
		Assert.assertEquals(date(2015, 3, 31), service.getDateProchainBouclement(bouclements, date(2015, 3, 31), true));
		Assert.assertEquals(date(2016, 9, 30), service.getDateProchainBouclement(bouclements, date(2015, 3, 31), false));
		Assert.assertEquals(date(2016, 9, 30), service.getDateProchainBouclement(bouclements, date(2015, 5, 31), true));
		Assert.assertEquals(date(2016, 9, 30), service.getDateProchainBouclement(bouclements, date(2015, 5, 31), false));
		Assert.assertEquals(date(2016, 9, 30), service.getDateProchainBouclement(bouclements, date(2015, 12, 31), true));
		Assert.assertEquals(date(2016, 9, 30), service.getDateProchainBouclement(bouclements, date(2015, 12, 31), false));
		Assert.assertEquals(date(2016, 9, 30), service.getDateProchainBouclement(bouclements, date(2016, 6, 30), true));
		Assert.assertEquals(date(2016, 9, 30), service.getDateProchainBouclement(bouclements, date(2016, 6, 30), false));
		Assert.assertEquals(date(2016, 9, 30), service.getDateProchainBouclement(bouclements, date(2016, 9, 30), true));
		Assert.assertEquals(date(2017, 9, 30), service.getDateProchainBouclement(bouclements, date(2016, 9, 30), false));
	}

	/**
	 * Franchement tordu = la période suivante a une date de début au beau milieu d'un cycle de la période précédente, et est tout de suite remplacée par une autre
	 */
	@Test
	public void testDateDernierBouclementAvecPeriodiciteSuivanteFranchementTordu() throws Exception {
		// dates des bouclements attendues : 31.03.2015, 30.09.2016, 30.09.2017 ...
		final List<Bouclement> bouclements = Arrays.asList(buildTransientBouclement(date(2015, 1, 1), DayMonth.get(3, 31), 12),
		                                                   buildTransientBouclement(date(2015, 12, 1), DayMonth.get(6, 30), 12),
		                                                   buildTransientBouclement(date(2016, 5, 1), DayMonth.get(9, 30), 12));

		Assert.assertNull(service.getDateDernierBouclement(bouclements, date(2015, 1, 1), true));
		Assert.assertNull(service.getDateDernierBouclement(bouclements, date(2015, 1, 1), false));
		Assert.assertEquals(date(2015, 3, 31), service.getDateDernierBouclement(bouclements, date(2015, 3, 31), true));
		Assert.assertNull(service.getDateDernierBouclement(bouclements, date(2015, 3, 31), false));
		Assert.assertEquals(date(2015, 3, 31), service.getDateDernierBouclement(bouclements, date(2015, 5, 31), true));
		Assert.assertEquals(date(2015, 3, 31), service.getDateDernierBouclement(bouclements, date(2015, 5, 31), false));
		Assert.assertEquals(date(2015, 3, 31), service.getDateDernierBouclement(bouclements, date(2015, 12, 31), true));
		Assert.assertEquals(date(2015, 3, 31), service.getDateDernierBouclement(bouclements, date(2015, 12, 31), false));
		Assert.assertEquals(date(2015, 3, 31), service.getDateDernierBouclement(bouclements, date(2016, 6, 30), true));
		Assert.assertEquals(date(2015, 3, 31), service.getDateDernierBouclement(bouclements, date(2016, 6, 30), false));
		Assert.assertEquals(date(2016, 9, 30), service.getDateDernierBouclement(bouclements, date(2016, 9, 30), true));
		Assert.assertEquals(date(2015, 3, 31), service.getDateDernierBouclement(bouclements, date(2016, 9, 30), false));
		Assert.assertEquals(date(2016, 9, 30), service.getDateDernierBouclement(bouclements, date(2017, 6, 30), true));
		Assert.assertEquals(date(2016, 9, 30), service.getDateDernierBouclement(bouclements, date(2017, 6, 30), false));
		Assert.assertEquals(date(2017, 9, 30), service.getDateDernierBouclement(bouclements, date(2017, 9, 30), true));
		Assert.assertEquals(date(2016, 9, 30), service.getDateDernierBouclement(bouclements, date(2017, 9, 30), false));
	}

	@Test
	public void testDateProchainBouclementFinFevrier() throws Exception {
		// dates de bouclements attendues : 28.02.2014, 28.02.2015, 29.02.2016, 28.02.2017...
		final List<Bouclement> bouclements = Collections.singletonList(buildTransientBouclement(date(2014, 2, 28), DayMonth.get(2, 28), 12));
		Assert.assertEquals(date(2014, 2, 28), service.getDateProchainBouclement(bouclements, date(2014, 1, 1), false));
		Assert.assertEquals(date(2015, 2, 28), service.getDateProchainBouclement(bouclements, date(2015, 1, 1), false));
		Assert.assertEquals(date(2016, 2, 29), service.getDateProchainBouclement(bouclements, date(2016, 1, 1), false));
		Assert.assertEquals(date(2017, 2, 28), service.getDateProchainBouclement(bouclements, date(2017, 1, 1), false));
	}

	@Test
	public void testExtractionExercicesCommerciauxSansRognageEnAbsenceDeBouclement() throws Exception {
		final DateRange range = new DateRangeHelper.Range(date(2003, 6, 12), date(2006, 2, 14));
		final List<ExerciceCommercial> exs = service.getExercicesCommerciaux(null, range, true);
		Assert.assertNotNull(exs);
		Assert.assertEquals(1, exs.size());

		final ExerciceCommercial ex = exs.get(0);
		Assert.assertNotNull(ex);
		Assert.assertEquals(range.getDateDebut(), ex.getDateDebut());
		Assert.assertEquals(range.getDateFin(), ex.getDateFin());
	}

	@Test
	public void testExtractionExercicesCommerciauxAvecRognageEnAbsenceDeBouclement() throws Exception {
		final DateRange range = new DateRangeHelper.Range(date(2003, 6, 12), date(2006, 2, 14));
		final List<ExerciceCommercial> exs = service.getExercicesCommerciaux(null, range, false);
		Assert.assertNotNull(exs);
		Assert.assertEquals(1, exs.size());

		final ExerciceCommercial ex = exs.get(0);
		Assert.assertNotNull(ex);
		Assert.assertEquals(range.getDateDebut(), ex.getDateDebut());
		Assert.assertEquals(range.getDateFin(), ex.getDateFin());
	}

	@Test
	public void testExtractionExercicesCommerciauxSansRognage() throws Exception {
		// exercices commerciaux attendus : ... -> 31.03.2015, 01.04.2015 -> 30.09.2016, 01.10.2016 -> 30.09.2017, ...
		final List<Bouclement> bouclements = Arrays.asList(buildTransientBouclement(date(2015, 1, 1), DayMonth.get(3, 31), 12),
		                                                   buildTransientBouclement(date(2015, 12, 1), DayMonth.get(6, 30), 12),
		                                                   buildTransientBouclement(date(2016, 5, 1), DayMonth.get(9, 30), 12));

		// période 2014-2017
		{
			final DateRange range = new DateRangeHelper.Range(date(2014, 1, 1), date(2017, 12, 31));
			final List<ExerciceCommercial> exs = service.getExercicesCommerciaux(bouclements, range, true);
			Assert.assertNotNull(exs);
			Assert.assertEquals(4, exs.size());
			{
				final ExerciceCommercial ex = exs.get(0);
				Assert.assertNotNull(ex);
				Assert.assertEquals(date(2014, 1, 1), ex.getDateDebut());
				Assert.assertEquals(date(2015, 3, 31), ex.getDateFin());
			}
			{
				final ExerciceCommercial ex = exs.get(1);
				Assert.assertNotNull(ex);
				Assert.assertEquals(date(2015, 4, 1), ex.getDateDebut());
				Assert.assertEquals(date(2016, 9, 30), ex.getDateFin());
			}
			{
				final ExerciceCommercial ex = exs.get(2);
				Assert.assertNotNull(ex);
				Assert.assertEquals(date(2016, 10, 1), ex.getDateDebut());
				Assert.assertEquals(date(2017, 9, 30), ex.getDateFin());
			}
			{
				final ExerciceCommercial ex = exs.get(3);
				Assert.assertNotNull(ex);
				Assert.assertEquals(date(2017, 10, 1), ex.getDateDebut());
				Assert.assertEquals(date(2018, 9, 30), ex.getDateFin());
			}
		}

		// période 2014
		{
			final DateRange range = new DateRangeHelper.Range(date(2014, 1, 1), date(2014, 12, 31));
			final List<ExerciceCommercial> exs = service.getExercicesCommerciaux(bouclements, range, true);
			Assert.assertNotNull(exs);
			Assert.assertEquals(1, exs.size());
			{
				final ExerciceCommercial ex = exs.get(0);
				Assert.assertNotNull(ex);
				Assert.assertEquals(date(2014, 1, 1), ex.getDateDebut());
				Assert.assertEquals(date(2015, 3, 31), ex.getDateFin());
			}
		}

		// période 2015
		{
			final DateRange range = new DateRangeHelper.Range(date(2015, 1, 1), date(2015, 12, 31));
			final List<ExerciceCommercial> exs = service.getExercicesCommerciaux(bouclements, range, true);
			Assert.assertNotNull(exs);
			Assert.assertEquals(2, exs.size());
			{
				final ExerciceCommercial ex = exs.get(0);
				Assert.assertNotNull(ex);
				Assert.assertEquals(date(2015, 1, 1), ex.getDateDebut());
				Assert.assertEquals(date(2015, 3, 31), ex.getDateFin());
			}
			{
				final ExerciceCommercial ex = exs.get(1);
				Assert.assertNotNull(ex);
				Assert.assertEquals(date(2015, 4, 1), ex.getDateDebut());
				Assert.assertEquals(date(2016, 9, 30), ex.getDateFin());
			}
		}
	}

	@Test
	public void testExtractionExercicesCommerciauxAvecRognage() throws Exception {
		// exercices commerciaux attendus : ... -> 31.03.2015, 01.04.2015 -> 30.09.2016, 01.10.2016 -> 30.09.2017, ...
		final List<Bouclement> bouclements = Arrays.asList(buildTransientBouclement(date(2015, 1, 1), DayMonth.get(3, 31), 12),
		                                                   buildTransientBouclement(date(2015, 12, 1), DayMonth.get(6, 30), 12),
		                                                   buildTransientBouclement(date(2016, 5, 1), DayMonth.get(9, 30), 12));

		// période 2014-2017
		{
			final DateRange range = new DateRangeHelper.Range(date(2014, 1, 1), date(2017, 12, 31));
			final List<ExerciceCommercial> exs = service.getExercicesCommerciaux(bouclements, range, false);
			Assert.assertNotNull(exs);
			Assert.assertEquals(4, exs.size());
			{
				final ExerciceCommercial ex = exs.get(0);
				Assert.assertNotNull(ex);
				Assert.assertEquals(date(2014, 1, 1), ex.getDateDebut());
				Assert.assertEquals(date(2015, 3, 31), ex.getDateFin());
			}
			{
				final ExerciceCommercial ex = exs.get(1);
				Assert.assertNotNull(ex);
				Assert.assertEquals(date(2015, 4, 1), ex.getDateDebut());
				Assert.assertEquals(date(2016, 9, 30), ex.getDateFin());
			}
			{
				final ExerciceCommercial ex = exs.get(2);
				Assert.assertNotNull(ex);
				Assert.assertEquals(date(2016, 10, 1), ex.getDateDebut());
				Assert.assertEquals(date(2017, 9, 30), ex.getDateFin());
			}
			{
				final ExerciceCommercial ex = exs.get(3);
				Assert.assertNotNull(ex);
				Assert.assertEquals(date(2017, 10, 1), ex.getDateDebut());
				Assert.assertEquals(date(2017, 12, 31), ex.getDateFin());
			}
		}

		// période 2014
		{
			final DateRange range = new DateRangeHelper.Range(date(2014, 1, 1), date(2014, 12, 31));
			final List<ExerciceCommercial> exs = service.getExercicesCommerciaux(bouclements, range, false);
			Assert.assertNotNull(exs);
			Assert.assertEquals(1, exs.size());
			{
				final ExerciceCommercial ex = exs.get(0);
				Assert.assertNotNull(ex);
				Assert.assertEquals(date(2014, 1, 1), ex.getDateDebut());
				Assert.assertEquals(date(2014, 12, 31), ex.getDateFin());
			}
		}

		// période 2015
		{
			final DateRange range = new DateRangeHelper.Range(date(2015, 1, 1), date(2015, 12, 31));
			final List<ExerciceCommercial> exs = service.getExercicesCommerciaux(bouclements, range, false);
			Assert.assertNotNull(exs);
			Assert.assertEquals(2, exs.size());
			{
				final ExerciceCommercial ex = exs.get(0);
				Assert.assertNotNull(ex);
				Assert.assertEquals(date(2015, 1, 1), ex.getDateDebut());
				Assert.assertEquals(date(2015, 3, 31), ex.getDateFin());
			}
			{
				final ExerciceCommercial ex = exs.get(1);
				Assert.assertNotNull(ex);
				Assert.assertEquals(date(2015, 4, 1), ex.getDateDebut());
				Assert.assertEquals(date(2015, 12, 31), ex.getDateFin());
			}
		}
	}

	@Test
	public void testExtractionBouclementsDepuisDatesVides() throws Exception {
		Assert.assertEquals(0, service.extractBouclementsDepuisDates(null, 12).size());
		Assert.assertEquals(0, service.extractBouclementsDepuisDates(Collections.<RegDate>emptyList(), 12).size());
		Assert.assertEquals(0, service.extractBouclementsDepuisDates(Collections.<RegDate>singletonList(null), 12).size());
	}

	/**
	 * Vérification que toutes les dates générées par le bouclement entre la date minimale et la date maximale (comprises) de la liste fournie
	 * sont justement celles de la liste fournie...
	 * @param origin dates d'origine
	 * @param bouclementsMigres bouclements générés à partir de ces dates
	 */
	private void checkDatesFinBouclementRegenerees(Collection<RegDate> origin, List<Bouclement> bouclementsMigres, RegDate dateProchainBouclement) {
		final NavigableSet<RegDate> originSet = new TreeSet<>(origin);
		final DateRange range = new DateRangeHelper.Range(originSet.first(), originSet.last());
		final NavigableSet<RegDate> recomputedSet = new TreeSet<>();
		RegDate cursor = range.getDateDebut().getOneDayBefore();
		while (true) {
			cursor = service.getDateProchainBouclement(bouclementsMigres, cursor, false);
			if (range.isValidAt(cursor)) {
				recomputedSet.add(cursor);
			}
			else {
				break;
			}
		}
		Assert.assertEquals(originSet, recomputedSet);

		final RegDate prochainBouclementCalcule = service.getDateProchainBouclement(bouclementsMigres, originSet.last(), false);
		Assert.assertEquals(dateProchainBouclement, prochainBouclementCalcule);
	}

	@Test
	public void testExtractionBouclementsDepuisUneDate() throws Exception {
		// annuel
		{
			final List<RegDate> dates = Collections.singletonList(date(2015, 12, 31));
			final List<Bouclement> bouclements = service.extractBouclementsDepuisDates(dates, 12);
			Assert.assertNotNull(bouclements);
			checkDatesFinBouclementRegenerees(dates, bouclements, date(2016, 12, 31));

			Assert.assertEquals(1, bouclements.size());
			{
				final Bouclement b = bouclements.get(0);
				Assert.assertNotNull(b);
				Assert.assertFalse(b.isAnnule());
				Assert.assertEquals(DayMonth.get(12, 31), b.getAncrage());
				Assert.assertEquals(date(2015, 12, 1), b.getDateDebut());
				Assert.assertEquals(12, b.getPeriodeMois());
				Assert.assertNull(b.getId());
				Assert.assertNull(b.getEntreprise());
			}
		}
		// trimestriel
		{
			final List<RegDate> dates = Collections.singletonList(date(2015, 12, 31));
			final List<Bouclement> bouclements = service.extractBouclementsDepuisDates(dates, 3);
			Assert.assertNotNull(bouclements);
			checkDatesFinBouclementRegenerees(dates, bouclements, date(2016, 3, 31));

			Assert.assertEquals(1, bouclements.size());
			{
				final Bouclement b = bouclements.get(0);
				Assert.assertNotNull(b);
				Assert.assertFalse(b.isAnnule());
				Assert.assertEquals(DayMonth.get(12, 31), b.getAncrage());
				Assert.assertEquals(date(2015, 12, 1), b.getDateDebut());
				Assert.assertEquals(3, b.getPeriodeMois());
				Assert.assertNull(b.getId());
				Assert.assertNull(b.getEntreprise());
			}
		}
	}

	@Test
	public void testExtractionBouclementsDepuisDatesAnnuellesMilieuDeMois() throws Exception {
		final List<RegDate> dates = Arrays.asList(date(2014, 1, 12), date(2015, 1, 12), date(2016, 1, 12));
		final List<Bouclement> bouclements = service.extractBouclementsDepuisDates(dates, 12);
		Assert.assertNotNull(bouclements);
		checkDatesFinBouclementRegenerees(dates, bouclements, date(2017, 1, 12));

		Assert.assertEquals(1, bouclements.size());
		{
			final Bouclement b = bouclements.get(0);
			Assert.assertNotNull(b);
			Assert.assertFalse(b.isAnnule());
			Assert.assertEquals(DayMonth.get(1, 12), b.getAncrage());
			Assert.assertEquals(date(2014, 1, 1), b.getDateDebut());
			Assert.assertEquals(12, b.getPeriodeMois());
			Assert.assertNull(b.getId());
			Assert.assertNull(b.getEntreprise());
		}
	}

	@Test
	public void testExtractionBouclementsDepuisDatesAnnuellesMilieuDeMoisAvecPeriodiciteFinaleDifferenteDemandee() throws Exception {
		final List<RegDate> dates = Arrays.asList(date(2014, 1, 12), date(2015, 1, 12), date(2016, 1, 12));
		final List<Bouclement> bouclements = service.extractBouclementsDepuisDates(dates, 5);
		Assert.assertNotNull(bouclements);
		checkDatesFinBouclementRegenerees(dates, bouclements, date(2016, 6, 12));

		Assert.assertEquals(2, bouclements.size());
		{
			final Bouclement b = bouclements.get(0);
			Assert.assertNotNull(b);
			Assert.assertFalse(b.isAnnule());
			Assert.assertEquals(DayMonth.get(1, 12), b.getAncrage());
			Assert.assertEquals(date(2014, 1, 1), b.getDateDebut());
			Assert.assertEquals(12, b.getPeriodeMois());
			Assert.assertNull(b.getId());
			Assert.assertNull(b.getEntreprise());
		}
		{
			final Bouclement b = bouclements.get(1);
			Assert.assertNotNull(b);
			Assert.assertFalse(b.isAnnule());
			Assert.assertEquals(DayMonth.get(1, 12), b.getAncrage());
			Assert.assertEquals(date(2016, 1, 1), b.getDateDebut());
			Assert.assertEquals(5, b.getPeriodeMois());
			Assert.assertNull(b.getId());
			Assert.assertNull(b.getEntreprise());
		}
	}

	@Test
	public void testExtractionBouclementsDepuisDatesAnnuellesFinDeMois() throws Exception {
		final List<RegDate> dates = Arrays.asList(date(2014, 1, 31), date(2015, 1, 31), date(2016, 1, 31));
		final List<Bouclement> bouclements = service.extractBouclementsDepuisDates(dates, 12);
		Assert.assertNotNull(bouclements);
		checkDatesFinBouclementRegenerees(dates, bouclements, date(2017, 1, 31));

		Assert.assertEquals(1, bouclements.size());
		{
			final Bouclement b = bouclements.get(0);
			Assert.assertNotNull(b);
			Assert.assertFalse(b.isAnnule());
			Assert.assertEquals(DayMonth.get(1, 31), b.getAncrage());
			Assert.assertEquals(date(2014, 1, 1), b.getDateDebut());
			Assert.assertEquals(12, b.getPeriodeMois());
			Assert.assertNull(b.getId());
			Assert.assertNull(b.getEntreprise());
		}
	}

	@Test
	public void testExtractionBouclementsDepuisDatesMensuellesFinDeMois() throws Exception {
		final List<RegDate> dates = Arrays.asList(date(2016, 1, 31), date(2016, 2, 29), date(2016, 3, 31), date(2016, 4, 30));
		final List<Bouclement> bouclements = service.extractBouclementsDepuisDates(dates, 1);
		Assert.assertNotNull(bouclements);
		checkDatesFinBouclementRegenerees(dates, bouclements, date(2016, 5, 31));

		Assert.assertEquals(1, bouclements.size());
		{
			final Bouclement b = bouclements.get(0);
			Assert.assertNotNull(b);
			Assert.assertFalse(b.isAnnule());
			Assert.assertEquals(DayMonth.get(1, 31), b.getAncrage());
			Assert.assertEquals(date(2016, 1, 1), b.getDateDebut());
			Assert.assertEquals(1, b.getPeriodeMois());
			Assert.assertNull(b.getId());
			Assert.assertNull(b.getEntreprise());
		}
	}

	@Test
	public void testExtractionBouclementsDepuisDatesAnnuellesFinDeMoisFevrier() throws Exception {
		final List<RegDate> dates = Arrays.asList(date(2014, 2, 28), date(2015, 2, 28), date(2016, 2, 29));
		final List<Bouclement> bouclements = service.extractBouclementsDepuisDates(dates, 12);
		Assert.assertNotNull(bouclements);
		checkDatesFinBouclementRegenerees(dates, bouclements, date(2017, 2, 28));

		Assert.assertEquals(1, bouclements.size());
		{
			final Bouclement b = bouclements.get(0);
			Assert.assertNotNull(b);
			Assert.assertFalse(b.isAnnule());
			Assert.assertEquals(DayMonth.get(2, 28), b.getAncrage());
			Assert.assertEquals(date(2014, 2, 1), b.getDateDebut());
			Assert.assertEquals(12, b.getPeriodeMois());
			Assert.assertNull(b.getId());
			Assert.assertNull(b.getEntreprise());
		}
	}

	@Test
	public void testExtractionBouclementsChangementAncrageSansChangerPeriodeTransitionPlusPetite() throws Exception {
		final List<RegDate> dates = Arrays.asList(date(2012, 3, 31), date(2013, 3, 31), date(2013, 12, 31), date(2014, 12, 31));
		final List<Bouclement> bouclements = service.extractBouclementsDepuisDates(dates, 12);
		Assert.assertNotNull(bouclements);
		checkDatesFinBouclementRegenerees(dates, bouclements, date(2015, 12, 31));

		Assert.assertEquals(2, bouclements.size());
		{
			final Bouclement b = bouclements.get(0);
			Assert.assertNotNull(b);
			Assert.assertFalse(b.isAnnule());
			Assert.assertEquals(DayMonth.get(3, 31), b.getAncrage());
			Assert.assertEquals(date(2012, 3, 1), b.getDateDebut());
			Assert.assertEquals(12, b.getPeriodeMois());
			Assert.assertNull(b.getId());
			Assert.assertNull(b.getEntreprise());
		}
		{
			final Bouclement b = bouclements.get(1);
			Assert.assertNotNull(b);
			Assert.assertFalse(b.isAnnule());
			Assert.assertEquals(DayMonth.get(12, 31), b.getAncrage());
			Assert.assertEquals(date(2013, 12, 1), b.getDateDebut());
			Assert.assertEquals(12, b.getPeriodeMois());
			Assert.assertNull(b.getId());
			Assert.assertNull(b.getEntreprise());
		}
	}

	@Test
	public void testExtractionBouclementsChangementAncrageSansChangerPeriodeTransitionPlusGrande() throws Exception {
		final List<RegDate> dates = Arrays.asList(date(2012, 3, 31), date(2013, 3, 31), date(2014, 6, 30), date(2015, 6, 30));
		final List<Bouclement> bouclements = service.extractBouclementsDepuisDates(dates, 12);
		Assert.assertNotNull(bouclements);
		checkDatesFinBouclementRegenerees(dates, bouclements, date(2016, 6, 30));

		Assert.assertEquals(2, bouclements.size());
		{
			final Bouclement b = bouclements.get(0);
			Assert.assertNotNull(b);
			Assert.assertFalse(b.isAnnule());
			Assert.assertEquals(DayMonth.get(3, 31), b.getAncrage());
			Assert.assertEquals(date(2012, 3, 1), b.getDateDebut());
			Assert.assertEquals(12, b.getPeriodeMois());
			Assert.assertNull(b.getId());
			Assert.assertNull(b.getEntreprise());
		}
		{
			final Bouclement b = bouclements.get(1);
			Assert.assertNotNull(b);
			Assert.assertFalse(b.isAnnule());
			Assert.assertEquals(DayMonth.get(6, 30), b.getAncrage());
			Assert.assertEquals(date(2014, 3, 1), b.getDateDebut());
			Assert.assertEquals(12, b.getPeriodeMois());
			Assert.assertNull(b.getId());
			Assert.assertNull(b.getEntreprise());
		}
	}

	@Test
	public void testExtractionBouclementsChangementsArbitrairesGlobalementAnnuels() throws Exception {
		final List<RegDate> dates = Arrays.asList(date(2012, 3, 31), date(2013, 3, 15), date(2014, 6, 30), date(2015, 6, 15));
		final List<Bouclement> bouclements = service.extractBouclementsDepuisDates(dates, 12);
		Assert.assertNotNull(bouclements);
		checkDatesFinBouclementRegenerees(dates, bouclements, date(2016, 6, 15));

		Assert.assertEquals(4, bouclements.size());
		{
			final Bouclement b = bouclements.get(0);
			Assert.assertNotNull(b);
			Assert.assertFalse(b.isAnnule());
			Assert.assertEquals(DayMonth.get(3, 31), b.getAncrage());
			Assert.assertEquals(date(2012, 3, 1), b.getDateDebut());
			Assert.assertEquals(12, b.getPeriodeMois());
			Assert.assertNull(b.getId());
			Assert.assertNull(b.getEntreprise());
		}
		{
			final Bouclement b = bouclements.get(1);
			Assert.assertNotNull(b);
			Assert.assertFalse(b.isAnnule());
			Assert.assertEquals(DayMonth.get(3, 15), b.getAncrage());
			Assert.assertEquals(date(2013, 3, 1), b.getDateDebut());
			Assert.assertEquals(12, b.getPeriodeMois());
			Assert.assertNull(b.getId());
			Assert.assertNull(b.getEntreprise());
		}
		{
			final Bouclement b = bouclements.get(2);
			Assert.assertNotNull(b);
			Assert.assertFalse(b.isAnnule());
			Assert.assertEquals(DayMonth.get(6, 30), b.getAncrage());
			Assert.assertEquals(date(2014, 3, 1), b.getDateDebut());
			Assert.assertEquals(12, b.getPeriodeMois());
			Assert.assertNull(b.getId());
			Assert.assertNull(b.getEntreprise());
		}
		{
			final Bouclement b = bouclements.get(3);
			Assert.assertNotNull(b);
			Assert.assertFalse(b.isAnnule());
			Assert.assertEquals(DayMonth.get(6, 15), b.getAncrage());
			Assert.assertEquals(date(2015, 6, 1), b.getDateDebut());
			Assert.assertEquals(12, b.getPeriodeMois());
			Assert.assertNull(b.getId());
			Assert.assertNull(b.getEntreprise());
		}
	}

	@Test
	public void testExtractionBouclementsChangementsArbitraires() throws Exception {
		final List<RegDate> dates = Arrays.asList(date(2012, 3, 31), date(2013, 4, 15), date(2014, 2, 28), date(2015, 10, 15));
		final List<Bouclement> bouclements = service.extractBouclementsDepuisDates(dates, 12);
		Assert.assertNotNull(bouclements);
		checkDatesFinBouclementRegenerees(dates, bouclements, date(2016, 10, 15));

		Assert.assertEquals(4, bouclements.size());
		{
			final Bouclement b = bouclements.get(0);
			Assert.assertNotNull(b);
			Assert.assertFalse(b.isAnnule());
			Assert.assertEquals(DayMonth.get(3, 31), b.getAncrage());
			Assert.assertEquals(date(2012, 3, 1), b.getDateDebut());
			Assert.assertEquals(13, b.getPeriodeMois());
			Assert.assertNull(b.getId());
			Assert.assertNull(b.getEntreprise());
		}
		{
			final Bouclement b = bouclements.get(1);
			Assert.assertNotNull(b);
			Assert.assertFalse(b.isAnnule());
			Assert.assertEquals(DayMonth.get(4, 15), b.getAncrage());
			Assert.assertEquals(date(2013, 4, 1), b.getDateDebut());
			Assert.assertEquals(11, b.getPeriodeMois());
			Assert.assertNull(b.getId());
			Assert.assertNull(b.getEntreprise());
		}
		{
			final Bouclement b = bouclements.get(2);
			Assert.assertNotNull(b);
			Assert.assertFalse(b.isAnnule());
			Assert.assertEquals(DayMonth.get(2, 28), b.getAncrage());
			Assert.assertEquals(date(2014, 2, 1), b.getDateDebut());
			Assert.assertEquals(20, b.getPeriodeMois());
			Assert.assertNull(b.getId());
			Assert.assertNull(b.getEntreprise());
		}
		{
			final Bouclement b = bouclements.get(3);
			Assert.assertNotNull(b);
			Assert.assertFalse(b.isAnnule());
			Assert.assertEquals(DayMonth.get(10, 15), b.getAncrage());
			Assert.assertEquals(date(2015, 10, 1), b.getDateDebut());
			Assert.assertEquals(12, b.getPeriodeMois());
			Assert.assertNull(b.getId());
			Assert.assertNull(b.getEntreprise());
		}
	}

	/**
	 * Cas de saut avec deux dates, un trou, une date
	 */
	@Test
	public void testExtractionBouclementsSautPeriodeDeuxTrouUn() throws Exception {
		// il manque 31.12.2013 dans la liste
		final List<RegDate> dates = Arrays.asList(date(2011, 12, 31), date(2012, 12, 31), date(2014, 12, 31));
		final List<Bouclement> bouclements = service.extractBouclementsDepuisDates(dates, 12);
		Assert.assertNotNull(bouclements);
		checkDatesFinBouclementRegenerees(dates, bouclements, date(2015, 12, 31));

		Assert.assertEquals(3, bouclements.size());
		{
			final Bouclement b = bouclements.get(0);
			Assert.assertNotNull(b);
			Assert.assertFalse(b.isAnnule());
			Assert.assertEquals(DayMonth.get(12, 31), b.getAncrage());
			Assert.assertEquals(date(2011, 12, 1), b.getDateDebut());
			Assert.assertEquals(12, b.getPeriodeMois());
			Assert.assertNull(b.getId());
			Assert.assertNull(b.getEntreprise());
		}
		{
			final Bouclement b = bouclements.get(1);
			Assert.assertNotNull(b);
			Assert.assertFalse(b.isAnnule());
			Assert.assertEquals(DayMonth.get(12, 31), b.getAncrage());
			Assert.assertEquals(date(2012, 12, 1), b.getDateDebut());
			Assert.assertEquals(24, b.getPeriodeMois());
			Assert.assertNull(b.getId());
			Assert.assertNull(b.getEntreprise());
		}
		{
			final Bouclement b = bouclements.get(2);
			Assert.assertNotNull(b);
			Assert.assertFalse(b.isAnnule());
			Assert.assertEquals(DayMonth.get(12, 31), b.getAncrage());
			Assert.assertEquals(date(2014, 12, 1), b.getDateDebut());
			Assert.assertEquals(12, b.getPeriodeMois());
			Assert.assertNull(b.getId());
			Assert.assertNull(b.getEntreprise());
		}
	}

	/**
	 * Cas de saut avec deux dates, un trou, deux dates
	 */
	@Test
	public void testExtractionBouclementsSautPeriodeDeuxTrouDeux() throws Exception {
		// il manque 31.12.2013 dans la liste
		final List<RegDate> dates = Arrays.asList(date(2011, 12, 31), date(2012, 12, 31), date(2014, 12, 31), date(2015, 12, 31));
		final List<Bouclement> bouclements = service.extractBouclementsDepuisDates(dates, 12);
		Assert.assertNotNull(bouclements);
		checkDatesFinBouclementRegenerees(dates, bouclements, date(2016, 12, 31));

		Assert.assertEquals(3, bouclements.size());
		{
			final Bouclement b = bouclements.get(0);
			Assert.assertNotNull(b);
			Assert.assertFalse(b.isAnnule());
			Assert.assertEquals(DayMonth.get(12, 31), b.getAncrage());
			Assert.assertEquals(date(2011, 12, 1), b.getDateDebut());
			Assert.assertEquals(12, b.getPeriodeMois());
			Assert.assertNull(b.getId());
			Assert.assertNull(b.getEntreprise());
		}
		{
			final Bouclement b = bouclements.get(1);
			Assert.assertNotNull(b);
			Assert.assertFalse(b.isAnnule());
			Assert.assertEquals(DayMonth.get(12, 31), b.getAncrage());
			Assert.assertEquals(date(2012, 12, 1), b.getDateDebut());
			Assert.assertEquals(24, b.getPeriodeMois());
			Assert.assertNull(b.getId());
			Assert.assertNull(b.getEntreprise());
		}
		{
			final Bouclement b = bouclements.get(2);
			Assert.assertNotNull(b);
			Assert.assertFalse(b.isAnnule());
			Assert.assertEquals(DayMonth.get(12, 31), b.getAncrage());
			Assert.assertEquals(date(2015, 12, 1), b.getDateDebut());
			Assert.assertEquals(12, b.getPeriodeMois());
			Assert.assertNull(b.getId());
			Assert.assertNull(b.getEntreprise());
		}
	}
}
