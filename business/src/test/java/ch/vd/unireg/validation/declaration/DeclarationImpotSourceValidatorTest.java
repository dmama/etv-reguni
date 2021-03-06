package ch.vd.unireg.validation.declaration;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.validation.AbstractValidatorTest;

import static org.junit.Assert.assertFalse;

public class DeclarationImpotSourceValidatorTest extends AbstractValidatorTest<DeclarationImpotSource> {

	@Override
	protected String getValidatorBeanName() {
		return "declarationImpotSourceValidator";
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateDeclarationAnnulee() {

		final DeclarationImpotSource lr = new DeclarationImpotSource();

		// Adresse invalide (date début nul) mais annulée => pas d'erreur
		{
			lr.setDateDebut(null);
			lr.setAnnule(true);
			assertFalse(validate(lr).hasErrors());
		}

		// Adresse valide et annulée => pas d'erreur
		{
			lr.setDateDebut(date(2000, 1, 1));
			lr.setAnnule(true);
			assertFalse(validate(lr).hasErrors());
		}
	}
}
