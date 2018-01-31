package ch.vd.uniregctb.validation.declaration;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

import static org.junit.Assert.assertFalse;

public abstract class DeclarationImpotOrdinaireValidatorTest<T extends DeclarationImpotOrdinaire> extends AbstractValidatorTest<T> {

	protected abstract T newDeclarationInstance();

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateDeclarationAnnulee() {

		final T di = newDeclarationInstance();

		// Déclaration invalide (numéro de séquence nul) mais annulée => pas d'erreur
		{
			di.setNumero(null);
			di.setAnnule(true);
			assertFalse(validate(di).hasErrors());
		}

		// Déclaration valide et annulée => pas d'erreur
		{
			di.setPeriode(new PeriodeFiscale());
			di.setModeleDocument(new ModeleDocument());
			di.setNumero(1);
			di.setAnnule(true);
			assertFalse(validate(di).hasErrors());
		}
	}
}