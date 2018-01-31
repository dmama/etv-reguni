package ch.vd.uniregctb.validation.remarque;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.Remarque;
import ch.vd.uniregctb.validation.EntityValidatorImpl;

public class RemarqueValidator extends EntityValidatorImpl<Remarque> {

	@Override
	protected Class<Remarque> getValidatedClass() {
		return Remarque.class;
	}

	@Override
	public ValidationResults validate(Remarque entity) {
		final ValidationResults vr = new ValidationResults();
		if (!entity.isAnnule()) {
			// [SIFISC-12519] on n'autorise pas les remarques vides...
			if (StringUtils.isBlank(entity.getTexte())) {
				vr.addError("texte", "Le texte de la remarque ne doit pas être vide.");
			}
		}
		return vr;
	}
}
