package ch.vd.uniregctb.validation.periodicite;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

import static junit.framework.Assert.assertFalse;

public class PeriodiciteValidatorTest extends AbstractValidatorTest<Periodicite> {

	@Override
	protected String getValidatorBeanName() {
		return "periodiciteValidator";
	}


	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidatePeriodiciteAnnulee() {

		final Periodicite periodicite = new Periodicite();

		// Adresse invalide (périodicité décompte nul) mais annulée => pas d'erreur
		{
			periodicite.setPeriodeDecompte(null);
			periodicite.setAnnule(true);
			assertFalse(validate(periodicite).hasErrors());
		}

		// Adresse valide et annulée => pas d'erreur
		{
			periodicite.setPeriodeDecompte(PeriodeDecompte.M01);
			periodicite.setAnnule(true);
			assertFalse(validate(periodicite).hasErrors());
		}
	}
}
