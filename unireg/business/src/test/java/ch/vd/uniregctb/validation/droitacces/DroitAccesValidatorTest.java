package ch.vd.uniregctb.validation.droitacces;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.DroitAcces;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

import static junit.framework.Assert.assertFalse;

public class DroitAccesValidatorTest extends AbstractValidatorTest<DroitAcces> {

	@Override
	protected String getValidatorBeanName() {
		return "droitAccesValidator";
	}

	@Test
	public void testValidateDroitAccesAnnule() {

		final DroitAcces acces = new DroitAcces();

		// Acces invalide (date début nul) mais annulée => pas d'erreur
		{
			acces.setDateDebut(null);
			acces.setAnnule(true);
			assertFalse(validate(acces).hasErrors());
		}

		// Acces valide et annulée => pas d'erreur
		{
			acces.setDateDebut(RegDate.get(2000, 1, 1));
			acces.setAnnule(true);
			assertFalse(validate(acces).hasErrors());
		}
	}
}