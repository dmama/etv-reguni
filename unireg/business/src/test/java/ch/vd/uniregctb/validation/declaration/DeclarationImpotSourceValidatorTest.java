package ch.vd.uniregctb.validation.declaration;

import org.junit.Test;

import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

import static junit.framework.Assert.assertFalse;

public class DeclarationImpotSourceValidatorTest extends AbstractValidatorTest<DeclarationImpotSource> {

	@Override
	protected String getValidatorBeanName() {
		return "declarationImpotSourceValidator";
	}

	@Test
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
