package ch.vd.unireg.validation.remarque;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.tiers.Remarque;
import ch.vd.unireg.validation.EntityValidatorImpl;

public class RemarqueValidator extends EntityValidatorImpl<Remarque> {

	@Override
	protected Class<Remarque> getValidatedClass() {
		return Remarque.class;
	}

	@Override
	@NotNull
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
