package ch.vd.unireg.param.online;

import java.util.HashSet;

import org.junit.Test;

import ch.vd.unireg.parametrage.ParametreDemandeDelaisOnline;

import static ch.vd.unireg.common.WithoutSpringTest.assertEmpty;

public class DelaisOnlinePPViewTest {

	/**
	 * [FISCPROJ-1077] Vérifie que la méthode ne crash pas si la collection des périodes est nulle (= pas de périodes dans le formulaire).
	 */
	@Test
	public void testCalculeDatesFinSansPeriode() {
		final DelaisOnlinePPView view = new DelaisOnlinePPView();
		view.calculeDatesFin();
		assertEmpty(view.getPeriodes());
	}

	/**
	 * [FISCPROJ-1077] Vérifie que la méthode ne crash pas si la collection des périodes est nulle (= pas de périodes dans le formulaire).
	 */
	@Test
	public void testCopyToSansPeriode() {
		final DelaisOnlinePPView view = new DelaisOnlinePPView();
		final ParametreDemandeDelaisOnline delais = new ParametreDemandeDelaisOnline();
		delais.setPeriodesDelais(new HashSet<>());
		view.copyTo(delais);
		assertEmpty(delais.getPeriodesDelais());
	}
}