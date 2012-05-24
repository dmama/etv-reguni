package ch.vd.uniregctb.tiers.validator;

import org.apache.commons.lang.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import ch.vd.registre.base.avs.AvsHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.utils.ValidatorUtils;

/**
 * Validateur de l'objet du meme nom.
 *
 * @author Akram BEN AISSI <mailto:akram.ben-aissi@vd.ch>
 */
public class CivilEditValidator implements Validator {

	/**
	 * @see org.springframework.validation.Validator#supports(java.lang.Class)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return Tiers.class.equals(clazz) || DebiteurPrestationImposable.class.equals(clazz) || TiersEditView.class.equals(clazz);
	}

	/**
	 * @see org.springframework.validation.Validator#validate(java.lang.Object, org.springframework.validation.Errors)
	 */
	@Override
	public void validate(Object obj, Errors errors) {
		TiersEditView tiersView = (TiersEditView) obj;
		if (tiersView != null) {
			Tiers tiers = tiersView.getTiers();
			if (tiers != null) {

				if (tiers instanceof AutreCommunaute) {
					ValidationUtils.rejectIfEmptyOrWhitespace(errors, "tiers.nom", "error.tiers.nom.vide");
				}

				if (tiers instanceof PersonnePhysique) {
					PersonnePhysique nonHabitant = (PersonnePhysique) tiers;

					if (!nonHabitant.isHabitantVD()) {
						// --------- --------------Onglets Civil-----------------------------

						ValidationUtils.rejectIfEmptyOrWhitespace(errors, "tiers.nom", "error.tiers.nom.vide");

						if (StringUtils.isNotBlank(nonHabitant.getNumeroAssureSocial())) {
							if (!AvsHelper.isValidNouveauNumAVS(nonHabitant.getNumeroAssureSocial())) {
								errors.rejectValue("tiers.numeroAssureSocial", "error.numeroAssureSocial");
							}
						}

						RegDate dateNais = null;
						try {
							dateNais = RegDateHelper.displayStringToRegDate(tiersView.getSdateNaissance(), true);
						}
						catch (Exception e) {
							// exception traitée plus bas
						}

						String ancienNumAVS = tiersView.getIdentificationPersonne().getAncienNumAVS();
						if (StringUtils.isNotBlank(ancienNumAVS) && nonHabitant.getSexe() == null) {
							errors.rejectValue("identificationPersonne.ancienNumAVS", "error.ancienNumeroAssureSocial.sexeNonRenseigne");
							errors.reject("onglet.error.civil");
						}
						else if (StringUtils.isNotBlank(ancienNumAVS)) {
							ancienNumAVS = FormatNumeroHelper.completeAncienNumAvs(ancienNumAVS);

							if (!AvsHelper.isValidAncienNumAVS(ancienNumAVS, dateNais, nonHabitant.getSexe() == Sexe.MASCULIN)) {
								errors.rejectValue("identificationPersonne.ancienNumAVS", "error.ancienNumeroAssureSocial");
							}
							else {
								tiersView.getIdentificationPersonne().setAncienNumAVS(ancienNumAVS);
							}
						}

						if (tiersView.getSdateNaissance() != null && !tiersView.getSdateNaissance().isEmpty()) {
							if (dateNais == null || dateNais.isAfter(RegDate.get())) {
								errors.rejectValue("sdateNaissance", "error.dateNaissance.invalide");
							}
						}

						if (tiersView.getSdateDeces() != null && !tiersView.getSdateDeces().isEmpty()) {
							RegDate dateDeces = null;
							try {
								dateDeces = RegDateHelper.displayStringToRegDate(tiersView.getSdateDeces(), true);
							}
							catch (Exception e) {
								// exception traitée plus bas
							}
							if (dateDeces == null || dateDeces.isAfter(RegDate.get())) {
								errors.rejectValue("sdateDeces", "error.dateDeces.invalide");
							}
						}

						/*
											 * if (StringUtils.isNotBlank(tiersView.getIdentificationPersonne().getNumRegistreEtranger())) { if
											 * ((!ValidateHelper.isNumber(tiersView.getIdentificationPersonne().getNumRegistreEtranger())) ) {
											 * errors.rejectValue("identificationPersonne.numRegistreEtranger", "error.numRegistreEtranger");
											 * errors.reject("onglet.error.civil"); } }
											 */

						if (StringUtils.isNotBlank(tiersView.getIdentificationPersonne().getNumRegistreEtranger())) {
							if ((!ValidatorUtils.isNumber(FormatNumeroHelper.removeSpaceAndDash(tiersView.getIdentificationPersonne().getNumRegistreEtranger())))
									|| (FormatNumeroHelper.removeSpaceAndDash(tiersView.getIdentificationPersonne().getNumRegistreEtranger())
									.length() > 10)) {
								errors.rejectValue("identificationPersonne.numRegistreEtranger", "error.numRegistreEtranger");
							}
						}
					}
				}
			}
		}
	}


}
