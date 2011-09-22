package ch.vd.uniregctb.validation.declaration;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

import static junit.framework.Assert.assertFalse;

public class DeclarationImpotOrdinaireValidatorTest extends AbstractValidatorTest<DeclarationImpotOrdinaire> {

	@Override
	protected String getValidatorBeanName() {
		return "declarationImpotOrdinaireValidator";
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateDeclarationAnnulee() {

		final DeclarationImpotOrdinaire di = new DeclarationImpotOrdinaire();

		// Adresse invalide (numéro de séquence nul) mais annulée => pas d'erreur
		{
			di.setNumero(null);
			di.setAnnule(true);
			assertFalse(validate(di).hasErrors());
		}

		// Adresse valide et annulée => pas d'erreur
		{
			di.setPeriode(new PeriodeFiscale());
			di.setModeleDocument(new ModeleDocument());
			di.setNumero(1);
			di.setAnnule(true);
			assertFalse(validate(di).hasErrors());
		}
	}

}