package ch.vd.unireg.validation.fors;

import ch.vd.unireg.tiers.ForFiscal;

/**
 * Le validateur utilisé dans ce test et exposé dans spring sous le nom concreteTestForFiscalValidator
 */
public final class ConcreteForFiscalValidator extends ForFiscalValidator<ForFiscal> {

	@Override
	protected Class<ForFiscal> getValidatedClass() {
		return ForFiscal.class;
	}
}
