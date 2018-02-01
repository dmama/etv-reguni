package ch.vd.uniregctb.validation.etiquette;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.etiquette.Etiquette;
import ch.vd.uniregctb.validation.EntityValidatorImpl;

/**
 * Validateur d'une étiquette liable à un tiers
 */
public class EtiquetteValidator extends EntityValidatorImpl<Etiquette> {

	@Override
	protected Class<Etiquette> getValidatedClass() {
		return Etiquette.class;
	}

	@Override
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
