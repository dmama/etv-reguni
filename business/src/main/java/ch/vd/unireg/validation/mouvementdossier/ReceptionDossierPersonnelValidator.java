package ch.vd.unireg.validation.mouvementdossier;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.mouvement.ReceptionDossierPersonnel;
import ch.vd.unireg.validation.EntityValidatorImpl;

public class ReceptionDossierPersonnelValidator extends EntityValidatorImpl<ReceptionDossierPersonnel> {

	@Override
	protected Class<ReceptionDossierPersonnel> getValidatedClass() {
		return ReceptionDossierPersonnel.class;
	}

	@Override
	@NotNull
	public ValidationResults validate(ReceptionDossierPersonnel entity) {
		final ValidationResults vr = new ValidationResults();
		if (!entity.isAnnule()) {
			// [SIFISC-30400] on n'autorise pas les movement dossiers sans le visa...
			if (StringUtils.isBlank(entity.getVisaRecepteur())) {
				vr.addError("VisaRecepteur", "Le VisaRecepteur de l'envoi dossier vers collaborateur ne doit pas être vide.");
			}
		}
		return vr;
	}
}
