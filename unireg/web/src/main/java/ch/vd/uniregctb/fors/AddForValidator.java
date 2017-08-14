package ch.vd.uniregctb.fors;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public abstract class AddForValidator implements Validator {

	private final ServiceInfrastructureService infraService;

	protected AddForValidator(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	@Override
	public void validate(Object target, Errors errors) {
		final AddForView view =(AddForView) target;

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
		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("dateFin")) {
			if (dateFin != null) {
				if (RegDate.get().isBefore(dateFin) && !view.isDateFinFutureAutorisee()) {
					errors.rejectValue("dateFin", "error.date.fin.dans.futur");
				}
				else if (dateDebut != null && dateFin.isBefore(dateDebut)) {
					errors.rejectValue("dateFin", "error.date.fin.avant.debut");
				}
			}
		}

		if (view.getNoAutoriteFiscale() == null) {
			errors.rejectValue("noAutoriteFiscale", "error.autorite.fiscale.vide");
		}

		if (view.getTypeAutoriteFiscale() == null) {
			errors.rejectValue("typeAutoriteFiscale", "error.type.autorite.fiscale.vide");
		}

		// [UNIREG-3338] en cas de création d'un nouveau for fiscal, le pays doit être valide
		if (view.getId() == null && view.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS && view.getNoAutoriteFiscale() != null) {
			final Integer noOfsPays = view.getNoAutoriteFiscale();
			final Pays pays = infraService.getPays(noOfsPays, dateDebut);
			if (pays == null) {
				errors.rejectValue("noAutoriteFiscale", "error.pays.inconnu");
			}
			else if (!pays.isValide()) {
				errors.rejectValue("noAutoriteFiscale", "error.pays.non.valide");
			}
		}
	}
}
