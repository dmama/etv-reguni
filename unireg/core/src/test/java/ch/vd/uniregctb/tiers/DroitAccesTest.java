package ch.vd.uniregctb.tiers;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;

import static junit.framework.Assert.assertFalse;

public class DroitAccesTest extends WithoutSpringTest {

	@Test
	public void testValidateDroitAccesAnnule() {

		final DroitAcces acces = new DroitAcces();

		// Acces invalide (date début nul) mais annulée => pas d'erreur
		{
			acces.setDateDebut(null);
			acces.setAnnule(true);
			assertFalse(acces.validate().hasErrors());
		}

		// Acces valide et annulée => pas d'erreur
		{
			acces.setDateDebut(RegDate.get(2000, 1, 1));
			acces.setAnnule(true);
			assertFalse(acces.validate().hasErrors());
		}
	}
}