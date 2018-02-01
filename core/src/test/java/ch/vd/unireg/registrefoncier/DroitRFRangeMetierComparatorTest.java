package ch.vd.uniregctb.registrefoncier;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class DroitRFRangeMetierComparatorTest {

	/**
	 * Vérifie que l'ordre des types est bien : droits réels -> servitudes -> droits virtuels
	 */
	@Test
	public void testCompareDroitTypesDifferents() {

		final DroitProprietePersonnePhysiqueRF droitPP1 = new DroitProprietePersonnePhysiqueRF();
		droitPP1.setDateDebutMetier(RegDate.get(2000, 1, 1));

		final DroitProprietePersonnePhysiqueRF droitPP2 = new DroitProprietePersonnePhysiqueRF();
		droitPP2.setDateDebutMetier(RegDate.get(2010, 1, 1));

		final UsufruitRF usufruit1 = new UsufruitRF();
		usufruit1.setDateDebutMetier(RegDate.get(2000, 1, 1));

		final UsufruitRF usufruit2 = new UsufruitRF();
		usufruit2.setDateDebutMetier(RegDate.get(2010, 1, 1));

		final DroitProprieteVirtuelRF droitVirtuel1 = new DroitProprieteVirtuelRF();
		droitVirtuel1.setDateDebutMetier(RegDate.get(2000, 1, 1));

		final DroitProprieteVirtuelRF droitVirtuel2 = new DroitProprieteVirtuelRF();
		droitVirtuel2.setDateDebutMetier(RegDate.get(2010, 1, 1));

		final List<DroitRF> list = Arrays.asList(droitVirtuel2, droitVirtuel1, usufruit2, usufruit1, droitPP2, droitPP1);
		list.sort(new DroitRFRangeMetierComparator());

		assertEquals(6, list.size());
		assertSame(droitPP1, list.get(0));
		assertSame(usufruit1, list.get(1));
		assertSame(droitVirtuel1, list.get(2));
		assertSame(droitPP2, list.get(3));
		assertSame(usufruit2, list.get(4));
		assertSame(droitVirtuel2, list.get(5));
	}
}