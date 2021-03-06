package ch.vd.unireg.validation.adresse;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.adresse.AdresseMandataire;
import ch.vd.unireg.validation.tiers.DateRangeEntityValidator;

public abstract class AdresseMandataireValidator<T extends AdresseMandataire> extends DateRangeEntityValidator<T> {

	@Override
	@NotNull
	public ValidationResults validate(@NotNull T entity) {
		final ValidationResults vr = super.validate(entity);

		// on valide les données obligatoires de toute façon
		if (entity.getTypeMandat() == null) {
			vr.addError(String.format("%s %s n'a pas de type de mandat assigné", getEntityCategoryName(), getEntityDisplayString(entity)));
		}

		if (!entity.isAnnule()) {
			// le nom du destinataire est important...
			if (StringUtils.isBlank(entity.getNomDestinataire())) {
				vr.addError(String.format("%s %s possède un nom vide pour le destinataire", getEntityCategoryName(), getEntityDisplayString(entity)));
			}
		}

		return vr;
	}

	@Override
	protected String getEntityCategoryName() {
		return "L'adresse mandataire";
	}

	@Override
	protected boolean isDateDebutFutureAllowed() {
		return true;
	}

	@Override
	protected boolean isDateFinFutureAllowed() {
		return true;
	}
}
