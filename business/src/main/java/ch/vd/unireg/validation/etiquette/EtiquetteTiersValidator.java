package ch.vd.unireg.validation.etiquette;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.etiquette.Etiquette;
import ch.vd.unireg.etiquette.EtiquetteTiers;
import ch.vd.unireg.validation.tiers.DateRangeEntityValidator;

/**
 * Validateur d'une étiquette liée temporellement à un tiers
 */
public class EtiquetteTiersValidator extends DateRangeEntityValidator<EtiquetteTiers> {

	@Override
	protected Class<EtiquetteTiers> getValidatedClass() {
		return EtiquetteTiers.class;
	}

	@Override
	@NotNull
	public ValidationResults validate(EtiquetteTiers entity) {
		final ValidationResults vr = super.validate(entity);
		if (!entity.isAnnule()) {
			if (entity.getEtiquette() == null) {
				vr.addError("etiquette", "Le lien d'étiquetage doit être associé à une étiquette.");
			}
			else {
				vr.merge(getValidationService().validate(entity.getEtiquette()));
			}
		}
		return vr;
	}

	@Override
	protected String getEntityCategoryName() {
		return "L'étiquette";
	}

	@Override
	protected String getEntityDisplayString(@NotNull EtiquetteTiers entity) {
		return String.format("'%s' (%s)",
		                     Optional.ofNullable(entity.getEtiquette()).map(Etiquette::getLibelle).orElse(StringUtils.EMPTY),
		                     rangeToString(entity));
	}

	@Override
	protected boolean isDateDebutFutureAllowed() {
		// [SIFISC-22506] en fait, si, il faut autoriser les dates de début dans le futur...
		return true;
	}

	@Override
	protected boolean isDateFinFutureAllowed() {
		return true;
	}
}
