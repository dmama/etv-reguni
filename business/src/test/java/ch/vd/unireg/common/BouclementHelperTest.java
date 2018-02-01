package ch.vd.uniregctb.common;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Bouclement;
import ch.vd.uniregctb.type.DayMonth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Raphaël Marmier, 2015-09-23
 */
public class BouclementHelperTest extends WithoutSpringTest {

	@Test
	public void testCreateBouclementSelonSemestre() throws Exception {

		final Bouclement bouclement1erSemestre = BouclementHelper.createBouclement3112SelonSemestre(RegDate.get(2015, 1, 1));
		assertNotNull(bouclement1erSemestre);
		assertEquals(DayMonth.get(12, 31), bouclement1erSemestre.getAncrage());
		assertEquals(12, bouclement1erSemestre.getPeriodeMois());
		assertEquals(RegDate.get(2015, 1, 1), bouclement1erSemestre.getDateDebut());

		final Bouclement bouclement1erSemestreLimite = BouclementHelper.createBouclement3112SelonSemestre(RegDate.get(2015, 6, 30));
		assertNotNull(bouclement1erSemestreLimite);
		assertEquals(DayMonth.get(12, 31), bouclement1erSemestreLimite.getAncrage());
		assertEquals(12, bouclement1erSemestreLimite.getPeriodeMois());
		assertEquals(RegDate.get(2015, 6, 30), bouclement1erSemestreLimite.getDateDebut());

		final Bouclement bouclement2erSemestreLimite = BouclementHelper.createBouclement3112SelonSemestre(RegDate.get(2015, 7, 1));
		assertNotNull(bouclement2erSemestreLimite);
		assertEquals(DayMonth.get(12, 31), bouclement2erSemestreLimite.getAncrage());
		assertEquals(12, bouclement2erSemestreLimite.getPeriodeMois());
		assertEquals(RegDate.get(2016, 1, 1), bouclement2erSemestreLimite.getDateDebut());

		final Bouclement bouclement2erSemestre = BouclementHelper.createBouclement3112SelonSemestre(RegDate.get(2015, 12, 31));
		assertNotNull(bouclement2erSemestre);
		assertEquals(DayMonth.get(12, 31), bouclement2erSemestre.getAncrage());
		assertEquals(12, bouclement2erSemestre.getPeriodeMois());
		assertEquals(RegDate.get(2016, 1, 1), bouclement2erSemestre.getDateDebut());
	}

	@Test
	public void testCreateBouclement() throws Exception {

		final Bouclement bouclement1erSemestre = BouclementHelper.createBouclement3112(RegDate.get(2015, 1, 1));
		assertNotNull(bouclement1erSemestre);
		assertEquals(DayMonth.get(12, 31), bouclement1erSemestre.getAncrage());
		assertEquals(12, bouclement1erSemestre.getPeriodeMois());
		assertEquals(RegDate.get(2015, 1, 1), bouclement1erSemestre.getDateDebut());

		final Bouclement bouclement1erSemestreLimite = BouclementHelper.createBouclement3112(RegDate.get(2015, 6, 30));
		assertNotNull(bouclement1erSemestreLimite);
		assertEquals(DayMonth.get(12, 31), bouclement1erSemestreLimite.getAncrage());
		assertEquals(12, bouclement1erSemestreLimite.getPeriodeMois());
		assertEquals(RegDate.get(2015, 6, 30), bouclement1erSemestreLimite.getDateDebut());

		final Bouclement bouclement2erSemestreLimite = BouclementHelper.createBouclement3112(RegDate.get(2015, 7, 1));
		assertNotNull(bouclement2erSemestreLimite);
		assertEquals(DayMonth.get(12, 31), bouclement2erSemestreLimite.getAncrage());
		assertEquals(12, bouclement2erSemestreLimite.getPeriodeMois());
		assertEquals(RegDate.get(2015, 7, 1), bouclement2erSemestreLimite.getDateDebut());

		final Bouclement bouclement2erSemestre = BouclementHelper.createBouclement3112(RegDate.get(2015, 12, 31));
		assertNotNull(bouclement2erSemestre);
		assertEquals(DayMonth.get(12, 31), bouclement2erSemestre.getAncrage());
		assertEquals(12, bouclement2erSemestre.getPeriodeMois());
		assertEquals(RegDate.get(2015, 12, 31), bouclement2erSemestre.getDateDebut());
	}
}