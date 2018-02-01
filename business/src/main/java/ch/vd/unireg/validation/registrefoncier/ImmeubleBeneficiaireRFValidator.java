package ch.vd.uniregctb.validation.registrefoncier;

import java.util.Objects;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.registrefoncier.ImmeubleBeneficiaireRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.validation.EntityValidatorImpl;

public class ImmeubleBeneficiaireRFValidator extends EntityValidatorImpl<ImmeubleBeneficiaireRF> {
	@Override
	protected Class<ImmeubleBeneficiaireRF> getValidatedClass() {
		return ImmeubleBeneficiaireRF.class;
	}

	@Override
	public ValidationResults validate(ImmeubleBeneficiaireRF beneficiaire) {

		final ValidationResults results = new ValidationResults();

		// un immeuble bénéficiaire annulé est toujours valide...
		if (beneficiaire.isAnnule()) {
			return results;
		}

		final ImmeubleRF immeuble = beneficiaire.getImmeuble();
		if (immeuble == null) {
			results.addError(String.format("l'immeuble bénéficiaire IdRF=%s ne possède pas d'immeuble renseigné", beneficiaire.getIdRF()));
		}
		else if (!Objects.equals(beneficiaire.getIdRF(), immeuble.getIdRF())) {
			results.addError(String.format("l'IdRF de l'immeuble bénéficiaire (%s) et l'immeuble associé (%s) ne sont pas les mêmes", beneficiaire.getIdRF(), immeuble.getIdRF()));
		}

		return results;
	}
}
