package ch.vd.unireg.tiers.validator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.avs.AvsHelper;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.tiers.view.NonHabitantCivilView;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.utils.ValidatorUtils;

public class NonHabitantCivilViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return NonHabitantCivilView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		if (!errors.hasErrors()) {
			final NonHabitantCivilView view = (NonHabitantCivilView) target;

			if (StringUtils.isBlank(view.getNom())) {
				errors.rejectValue("nom", "error.tiers.nom.vide");
			}

			if (StringUtils.isNotBlank(view.getNumeroAssureSocial())) {
				if (!AvsHelper.isValidNouveauNumAVS(view.getNumeroAssureSocial())) {
					errors.rejectValue("numeroAssureSocial", "error.numeroAssureSocial");
				}
			}

			RegDate dateNais = null;
			try {
				dateNais = RegDateHelper.displayStringToRegDate(view.getsDateNaissance(), true);
			}
			catch (Exception e) {
				// exception traitée plus bas
			}

			String ancienNumAVS = view.getIdentificationPersonne().getAncienNumAVS();
			if (StringUtils.isNotBlank(ancienNumAVS) && view.getSexe() == null) {
				errors.rejectValue("identificationPersonne.ancienNumAVS", "error.ancienNumeroAssureSocial.sexeNonRenseigne");
			}
			else if (StringUtils.isNotBlank(ancienNumAVS)) {
				ancienNumAVS = FormatNumeroHelper.completeAncienNumAvs(ancienNumAVS);

				if (!AvsHelper.isValidAncienNumAVS(ancienNumAVS, dateNais, view.getSexe() == Sexe.MASCULIN)) {
					errors.rejectValue("identificationPersonne.ancienNumAVS", "error.ancienNumeroAssureSocial");
				}
				else {
					ancienNumAVS = FormatNumeroHelper.removeSpaceAndDash(ancienNumAVS);
					view.getIdentificationPersonne().setAncienNumAVS(ancienNumAVS);
				}
			}

			if (StringUtils.isNotBlank(view.getsDateNaissance())) {
				if (dateNais == null || dateNais.isAfter(RegDate.get())) {
					errors.rejectValue("sDateNaissance", "error.dateNaissance.invalide");
				}
			}

			if (StringUtils.isNotBlank(view.getIdentificationPersonne().getNumRegistreEtranger())) {
				if ((!ValidatorUtils.isNumber(FormatNumeroHelper.removeSpaceAndDash(view.getIdentificationPersonne().getNumRegistreEtranger())))
						|| (FormatNumeroHelper.removeSpaceAndDash(view.getIdentificationPersonne().getNumRegistreEtranger()).length() > 10)) {
					errors.rejectValue("identificationPersonne.numRegistreEtranger", "error.numRegistreEtranger");
				}
			}
		}
	}
}
