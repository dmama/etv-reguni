package ch.vd.uniregctb.tiers.validator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.common.NumeroIDEHelper;
import ch.vd.uniregctb.tiers.view.EtablissementCivilView;

public class EtablissementCivilViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return EtablissementCivilView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {

		if (!errors.hasErrors()) {
			final EtablissementCivilView view = (EtablissementCivilView) target;

			// Commune
			if (view.getNoOfsCommune() == null) {
				errors.rejectValue("noOfsCommune", "error.commune.vide");
			}

			// Raison sociale
			if (StringUtils.isBlank(view.getRaisonSociale())) {
				errors.rejectValue("raisonSociale", "error.raison.sociale.vide");
			} else {
				if (view.getRaisonSociale().length() > LengthConstants.ETB_RAISON_SOCIALE) {
					errors.rejectValue("raisonSociale", "error.raison.sociale.trop.long");
				}
			}

			// Numéro IDE
			if (StringUtils.isNotBlank(view.getNumeroIDE())) {
				if (!NumeroIDEHelper.isValid(view.getNumeroIDE())) {
					errors.rejectValue("numeroIDE", "error.numero.ide.invalide");
				}
			}

			RegDate dateDebut = null;
			RegDate dateFin = null;
			try {
				dateDebut = RegDateHelper.displayStringToRegDate(view.getsDateDebut(), true);
				dateFin = RegDateHelper.displayStringToRegDate(view.getsDateFin(), true);
			} catch (Exception e) {
				// exception traitée plus bas
			}

			// Date de début
			if (StringUtils.isNotBlank(view.getsDateDebut())) {
				if (dateDebut == null || dateDebut.isAfter(RegDate.get())) {
					errors.rejectValue("sDateDebut", "error.date.debut.invalide");
				}
			} else {
				errors.rejectValue("sDateDebut", "error.date.debut.vide");
			}

			// Date de fin
			if (StringUtils.isNotBlank(view.getsDateFin())) {
				if (dateFin == null) {
					errors.rejectValue("sDateFin", "error.date.fin.invalide");
				} else if (dateFin.isBefore(dateDebut)) {
					errors.rejectValue("sDateFin", "error.date.fin.avant.debut");
				} else if (dateFin.isAfter(RegDate.get())) {
					errors.rejectValue("sDateFin", "error.date.fin.dans.futur");
				}
			}


		}
	}
}
