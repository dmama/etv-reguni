package ch.vd.uniregctb.validation.registrefoncier;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.registrefoncier.RapprochementRF;
import ch.vd.uniregctb.validation.tiers.DateRangeEntityValidator;

/**
 * Validateur pour les données de rapprochement RF
 */
public class RapprochementRFValidator extends DateRangeEntityValidator<RapprochementRF> {

	@Override
	protected Class<RapprochementRF> getValidatedClass() {
		return RapprochementRF.class;
	}

	@Override
	protected String getEntityCategoryName() {
		return "le rapprochement RF";
	}

	@Override
	protected boolean isDateDebutFutureAllowed() {
		return true;
	}

	@Override
	protected boolean isDateDebutNullAllowed() {
		return true;
	}

	@Override
	protected boolean isDateFinFutureAllowed() {
		return true;
	}

	@Override
	public ValidationResults validate(RapprochementRF entity) {
		final ValidationResults vr = super.validate(entity);
		if (!entity.isAnnule()) {
			// le type de rapprochement est obligatoire
			if (entity.getTypeRapprochement() == null) {
				vr.addError(String.format("%s %s ne possède pas de type renseigné", getEntityCategoryName(), getEntityDisplayString(entity)));
			}

			// le lien vers le tiers RF est également obligatoire
			if (entity.getTiersRF() == null) {
				vr.addError(String.format("%s %s n'est attaché à aucun tiers RF", getEntityCategoryName(), getEntityDisplayString(entity)));
			}
		}
		return vr;
	}
}
