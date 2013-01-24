package ch.vd.uniregctb.validation.declaration;

import java.util.List;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

public class EtatDeclarationEmiseValidatorTest extends AbstractValidatorTest<EtatDeclaration> {


	@Override
	protected String getValidatorBeanName() {
		return "concreteEtatDeclarationValidator";
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateDateObtention() {

		final EtatDeclarationEmise etat = new EtatDeclarationEmise();


		// Date d'obtention nulle
		{
			final ValidationResults results = validate(etat);
			Assert.assertTrue(results.hasErrors());
			final List<String> errors = results.getErrors();
			assertEquals(1, errors.size());
			assertEquals("L'etat émise de la déclaration possède une date d'obtention nulle", errors.get(0));

		}

		// Date d'obtention renseignée
		{
			etat.setDateObtention(RegDate.get(2000, 7, 1));
			assertFalse(validate(etat).hasErrors());
		}

	}


}
