package ch.vd.unireg.fors;

import java.util.List;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.validation.tiers.LocalisationDateeValidator;

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

		final Integer noAutoriteFiscale = view.getNoAutoriteFiscale();
		final TypeAutoriteFiscale typeAutoriteFiscale = view.getTypeAutoriteFiscale();

		if (noAutoriteFiscale == null) {
			errors.rejectValue("noAutoriteFiscale", "error.autorite.fiscale.vide");
		}

		if (typeAutoriteFiscale == null) {
			errors.rejectValue("typeAutoriteFiscale", "error.type.autorite.fiscale.vide");
		}

		if (noAutoriteFiscale != null && typeAutoriteFiscale != null) {
			// [SIFISC-27647] on valide l'autorité fiscale comme le fait le validator hibernate
			final List<LocalisationDateeValidator.Error> l = LocalisationDateeValidator.validateAutoriteFiscale(view, noAutoriteFiscale, typeAutoriteFiscale, "Le for fiscal", "", infraService);
			l.forEach(e -> errors.reject("global.error.validation", e.getMessage()));
		}

		// [UNIREG-3338] en cas de création d'un nouveau for fiscal, le pays doit être valide
		if (view.getId() == null && typeAutoriteFiscale == TypeAutoriteFiscale.PAYS_HS && noAutoriteFiscale != null) {
			final Integer noOfsPays = noAutoriteFiscale;
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
