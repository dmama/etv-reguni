package ch.vd.unireg.validation.etiquette;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.etiquette.Etiquette;
import ch.vd.unireg.validation.EntityValidatorImpl;

/**
 * Validateur d'une étiquette liable à un tiers
 */
public class EtiquetteValidator extends EntityValidatorImpl<Etiquette> {

	@Override
	protected Class<Etiquette> getValidatedClass() {
		return Etiquette.class;
	}

	@Override
	@NotNull
	public ValidationResults validate(Etiquette entity) {
		final ValidationResults vr = new ValidationResults();
		if (!entity.isAnnule()) {
			if (StringUtils.isBlank(entity.getCode())) {
				vr.addError("code", "Le code ne doit pas être vide.");
			}
			if (StringUtils.isBlank(entity.getLibelle())) {
				vr.addError("libelle", "Le libellé ne doit pas être vide.");
			}
		}
		return vr;
	}
}
