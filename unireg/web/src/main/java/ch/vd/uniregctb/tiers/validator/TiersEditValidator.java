package ch.vd.uniregctb.tiers.validator;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.iban.IbanValidationException;
import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.utils.AVSValidator;
import ch.vd.uniregctb.utils.EAN13CheckDigitOperation;
import ch.vd.uniregctb.utils.ValidateHelper;

/**
 * Validateur de l'objet du meme nom.
 *
 * @author Akram BEN AISSI <mailto:akram.ben-aissi@vd.ch>
 */
public class TiersEditValidator implements Validator {


	private IbanValidator ibanValidator;

	private TiersService tiersService;

	public void setIbanValidator(IbanValidator ibanValidator) {
		this.ibanValidator = ibanValidator;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}


	/**
	 * @see org.springframework.validation.Validator#supports(java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return Tiers.class.equals(clazz) || DebiteurPrestationImposable.class.equals(clazz) || TiersEditView.class.equals(clazz);
	}

	/**
	 * @see org.springframework.validation.Validator#validate(java.lang.Object, org.springframework.validation.Errors)
	 */
	@Transactional(readOnly=true)
	public void validate(Object obj, Errors errors) {
		TiersEditView tiersView = (TiersEditView) obj;
		if (tiersView != null) {
			Tiers tiers = tiersView.getTiers();
			if (tiers != null) {

				if (tiers instanceof AutreCommunaute) {
					AutreCommunaute autreCommunaute = (AutreCommunaute) tiers;
					if (StringUtils.isBlank(autreCommunaute.getNom())) {
						errors.rejectValue("tiers.nom", "error.tiers.nom.vide");
						errors.reject("onglet.error.civil");
					}
				}
				// --------- --------------Onglets Civil-----------------------------
				if (tiers instanceof PersonnePhysique) {
					PersonnePhysique nonHabitant = (PersonnePhysique) tiers;
					if (!nonHabitant.isHabitant()) {

						if (StringUtils.isBlank(nonHabitant.getNom())) {
							errors.rejectValue("tiers.nom", "error.tiers.nom.vide");
							errors.reject("onglet.error.civil");
						}

						if (StringUtils.isNotBlank(nonHabitant.getNumeroAssureSocial())) {
							AVSValidator newAvsValidator = new AVSValidator();
							newAvsValidator.setCheckDigit(EAN13CheckDigitOperation.INSTANCE);
							newAvsValidator.setLength(13);

							if (!newAvsValidator.isValidNouveauNumAVS(nonHabitant.getNumeroAssureSocial())) {
								errors.rejectValue("tiers.numeroAssureSocial", "error.numeroAssureSocial");
								errors.reject("onglet.error.civil");
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

							AVSValidator oldAvsValidator = new AVSValidator();
							oldAvsValidator.setCheckDigit(EAN13CheckDigitOperation.INSTANCE);

							ancienNumAVS = oldAvsValidator.validateAncienNumAVS(ancienNumAVS, dateNais, nonHabitant.getSexe());
							if (ancienNumAVS == null) {
								errors.rejectValue("identificationPersonne.ancienNumAVS", "error.ancienNumeroAssureSocial");
								errors.reject("onglet.error.civil");
							}
							else {
								tiersView.getIdentificationPersonne().setAncienNumAVS(ancienNumAVS);
							}
						}

						if (tiersView.getSdateNaissance() != null && !tiersView.getSdateNaissance().equals("")) {
							if (dateNais == null || dateNais.isAfter(RegDate.get())) {
								errors.rejectValue("sdateNaissance", "error.dateNaissance.invalide");
								errors.reject("onglet.error.civil");
							}
						}

						if (tiersView.getSdateDeces() != null && !tiersView.getSdateDeces().equals("")) {
							RegDate dateDeces = null;
							try {
								dateDeces = RegDateHelper.displayStringToRegDate(tiersView.getSdateDeces(), true);
							}
							catch (Exception e) {
								// exception traitée plus bas
							}
							if (dateDeces == null || dateDeces.isAfter(RegDate.get())) {
								errors.rejectValue("sdateDeces", "error.dateDeces.invalide");
								errors.reject("onglet.error.civil");
							}
						}

						/*
											 * if (StringUtils.isNotBlank(tiersView.getIdentificationPersonne().getNumRegistreEtranger())) { if
											 * ((!ValidateHelper.isNumber(tiersView.getIdentificationPersonne().getNumRegistreEtranger())) ) {
											 * errors.rejectValue("identificationPersonne.numRegistreEtranger", "error.numRegistreEtranger");
											 * errors.reject("onglet.error.civil"); } }
											 */

						if (StringUtils.isNotBlank(tiersView.getIdentificationPersonne().getNumRegistreEtranger())) {
							if ((!ValidateHelper.isNumber(FormatNumeroHelper.removeSpaceAndDash(tiersView.getIdentificationPersonne().getNumRegistreEtranger())))
									|| (FormatNumeroHelper.removeSpaceAndDash(tiersView.getIdentificationPersonne().getNumRegistreEtranger())
									.length() > 10)) {
								errors.rejectValue("identificationPersonne.numRegistreEtranger", "error.numRegistreEtranger");
								errors.reject("onglet.error.civil");
							}
						}
					}
				}

				// --------- --------------Onglets Complements------------------------
				if (StringUtils.isNotBlank(tiers.getNumeroTelecopie())) {
					if (!ValidateHelper.isNumberTel(tiers.getNumeroTelecopie())) {
						errors.rejectValue("tiers.numeroTelecopie", "error.telephone");
						errors.reject("onglet.error.complements");
					}
				}

				if (StringUtils.isNotBlank(tiers.getNumeroTelephonePrive())) {
					if (!ValidateHelper.isNumberTel(tiers.getNumeroTelephonePrive())) {
						errors.rejectValue("tiers.numeroTelephonePrive", "error.telephone");
						errors.reject("onglet.error.complements");
					}
				}

				if (StringUtils.isNotBlank(tiers.getNumeroTelephonePortable())) {
					if (!ValidateHelper.isNumberTel(tiers.getNumeroTelephonePortable())) {
						errors.rejectValue("tiers.numeroTelephonePortable", "error.telephone");
						errors.reject("onglet.error.complements");
					}
				}

				if (StringUtils.isNotBlank(tiers.getNumeroTelephoneProfessionnel())) {
					if (!ValidateHelper.isNumberTel(tiers.getNumeroTelephoneProfessionnel())) {
						errors.rejectValue("tiers.numeroTelephoneProfessionnel", "error.telephone");
						errors.reject("onglet.error.complements");
					}
				}

				if (StringUtils.isNotBlank(tiers.getAdresseCourrierElectronique())) {
					tiers.setAdresseCourrierElectronique(tiers.getAdresseCourrierElectronique().trim());
					if (!ValidateHelper.isValidEmail(tiers.getAdresseCourrierElectronique())) {
						errors.rejectValue("tiers.adresseCourrierElectronique", "error.email");
						errors.reject("onglet.error.complements");
					}
				}

				if (StringUtils.isNotBlank(tiers.getNumeroCompteBancaire())) {
					//[UNIREG-1449] il ne faudrait pas bloquer la sauvegarde de la page des "compléments" si l'IBAN, inchangé, est invalide. 
					Tiers tiersInBase = tiersService.getTiers(tiers.getNumero());
					if (!tiers.getNumeroCompteBancaire().equals(tiersInBase.getNumeroCompteBancaire())) {


						try {
							ibanValidator.validate(tiers.getNumeroCompteBancaire());
						}
						catch (IbanValidationException e) {
							errors.rejectValue("tiers.numeroCompteBancaire", "error.iban");
							errors.reject("onglet.error.complements");
						}
					}
				}

				//Validation du tiers
				ValidationResults results = tiers.validate();
				List<String> erreurs = results.getErrors();
				ValidateHelper.rejectErrors(erreurs, errors);
			}
		}
	}
}
