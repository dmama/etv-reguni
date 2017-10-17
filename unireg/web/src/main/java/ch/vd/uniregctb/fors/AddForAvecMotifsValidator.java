package ch.vd.uniregctb.fors;

import org.springframework.validation.Errors;

import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

public abstract class AddForAvecMotifsValidator extends AddForValidator {

	protected AddForAvecMotifsValidator(ServiceInfrastructureService infraService) {
		super(infraService);
	}

	protected abstract boolean isEmptyMotifDebutAllowed(AddForAvecMotifsView view);

	@Override
	public void validate(Object target, Errors errors) {
		super.validate(target, errors);

		final AddForAvecMotifsView view = (AddForAvecMotifsView) target;

		// [SIFISC-7381] les dates et motifs doivent être renseignés tous les deux, ou nuls tous les deux
		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("dateFin")) {
			if (view.getMotifFin() != null && view.getDateFin() == null) {
				errors.rejectValue("dateFin", "error.date.fermeture.vide");
			}
		}

		// validation du motif de début SIFISC-25746
		if (view.getMotifDebut() == null && !isEmptyMotifDebutAllowed(view)) {
			errors.rejectValue("motifDebut", "error.motif.ouverture.vide");
		}

		// validation du motif de fin
		if (view.getDateFin() != null && view.getMotifFin() == null) {
			errors.rejectValue("motifFin", "error.motif.fermeture.vide");
		}
	}
}
