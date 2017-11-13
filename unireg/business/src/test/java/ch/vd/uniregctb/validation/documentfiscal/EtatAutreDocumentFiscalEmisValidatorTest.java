package ch.vd.uniregctb.validation.documentfiscal;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.documentfiscal.EtatAutreDocumentFiscal;
import ch.vd.uniregctb.documentfiscal.EtatAutreDocumentFiscalEmis;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class EtatAutreDocumentFiscalEmisValidatorTest extends AbstractValidatorTest<EtatAutreDocumentFiscal> {


	@Override
	protected String getValidatorBeanName() {
		return "etatAutreDocumentFiscalValidator";
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateDateObtention() {

		final EtatAutreDocumentFiscalEmis etat = new EtatAutreDocumentFiscalEmis();


		// Date d'obtention nulle
		{
			final ValidationResults results = validate(etat);
			Assert.assertTrue(results.hasErrors());
			final List<String> errors = results.getErrors();
			assertEquals(1, errors.size());
			assertEquals("L'etat émise du document fiscal possède une date d'obtention nulle", errors.get(0));

		}

		// Date d'obtention renseignée
		{
			etat.setDateObtention(RegDate.get(2000, 7, 1));
			assertFalse(validate(etat).hasErrors());
		}

	}


}
