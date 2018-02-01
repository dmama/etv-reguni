package ch.vd.unireg.decision.aci;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.type.TypeAutoriteFiscale;

public class AddDecisionAciValidator  implements Validator {

	private final ServiceInfrastructureService infraService;

	public AddDecisionAciValidator(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return AddDecisionAciView.class.equals(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final AddDecisionAciView view =(AddDecisionAciView) target;

		// validation de la date de début
		final RegDate dateDebut = view.getDateDebut();
		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("dateDebut")) {
			if (dateDebut == null) {
				errors.rejectValue("dateDebut", "error.date.debut.vide");
			}
			else if (RegDate.get().isBefore(dateDebut)) {
				errors.rejectValue("dateDebut", "error.date.debut.future");
			}
		}

		// validation de la date de fin
		final RegDate dateFin = view.getDateFin();
		if (dateFin != null) {
			if (RegDate.get().isBefore(dateFin)) {
				errors.rejectValue("dateFin", "error.date.fin.dans.futur");
			}
			else if (dateDebut != null && dateFin.isBefore(dateDebut)) {
				errors.rejectValue("dateFin", "error.date.fin.avant.debut");
			}
		}

		if (view.getNumeroAutoriteFiscale() == null) {
			errors.rejectValue("numeroAutoriteFiscale", "error.autorite.fiscale.vide");
		}

		if (view.getTypeAutoriteFiscale() == null) {
			errors.rejectValue("typeAutoriteFiscale", "error.type.autorite.fiscale.vide");
		}

		if (view.getId() == null && view.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS && view.getNumeroAutoriteFiscale() != null) {
			final Integer noOfsPays = view.getNumeroAutoriteFiscale();
			final Pays pays = infraService.getPays(noOfsPays, dateDebut);
			if (pays == null) {
				errors.rejectValue("numeroAutoriteFiscale", "error.pays.inconnu");
			}
			else if (!pays.isValide()) {
				errors.rejectValue("numeroAutoriteFiscale", "error.pays.non.valide");
			}
		}
	}
}
