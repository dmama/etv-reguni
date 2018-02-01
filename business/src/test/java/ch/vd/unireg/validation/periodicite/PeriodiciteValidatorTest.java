package ch.vd.unireg.validation.periodicite;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.declaration.Periodicite;
import ch.vd.unireg.type.PeriodeDecompte;
import ch.vd.unireg.validation.AbstractValidatorTest;

import static org.junit.Assert.assertFalse;

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
