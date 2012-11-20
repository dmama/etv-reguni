package ch.vd.uniregctb.fors;

import org.springframework.validation.Errors;

import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

public abstract class AddForRevenuFortuneValidator extends AddForValidator {

	protected AddForRevenuFortuneValidator(ServiceInfrastructureService infraService) {
		super(infraService);
	}

	@Override
	public void validate(Object target, Errors errors) {
		super.validate(target, errors);

		final AddForRevenuFortuneView view =(AddForRevenuFortuneView) target;

		// validation du motif de début
		if (view.getDateDebut() != null && view.getMotifDebut() == null) {
			errors.rejectValue("motifDebut", "error.motif.ouverture.vide");
		}

		// validation du motif de fin
		if (view.getDateFin() != null && view.getMotifFin() == null) {
			errors.rejectValue("motifFin", "error.motif.fermeture.vide");
		}

		// le mode de rattachement
		if (view.getMotifRattachement() == null) {
			errors.rejectValue("motifRattachement", "error.motif.rattachement.vide");
		}
	}
}
