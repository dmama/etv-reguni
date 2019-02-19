package ch.vd.unireg.validation.mouvementdossier;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.mouvement.EnvoiDossierVersCollaborateur;
import ch.vd.unireg.validation.EntityValidatorImpl;

public class EnvoiDossierVersCollaborateurValidator extends EntityValidatorImpl<EnvoiDossierVersCollaborateur> {

	@Override
	protected Class<EnvoiDossierVersCollaborateur> getValidatedClass() {
		return EnvoiDossierVersCollaborateur.class;
	}

	@Override
	@NotNull
	public ValidationResults validate(EnvoiDossierVersCollaborateur entity) {
		final ValidationResults vr = new ValidationResults();
		if (!entity.isAnnule()) {
			// [SIFISC-30400] on n'autorise pas les movement dossiers sans le visa...
			if (StringUtils.isBlank(entity.getVisaDestinataire())) {
				vr.addError("visaDestinataire", "Le visaDestinataire de l'envoi dossier vers collaborateur ne doit pas être vide.");
			}
		}
		return vr;
	}
}
