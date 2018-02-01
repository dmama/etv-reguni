package ch.vd.unireg.validation.droitacces;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.DroitAcces;
import ch.vd.unireg.validation.AbstractValidatorTest;

import static org.junit.Assert.assertFalse;

public class DroitAccesValidatorTest extends AbstractValidatorTest<DroitAcces> {

	@Override
	protected String getValidatorBeanName() {
		return "droitAccesValidator";
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
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