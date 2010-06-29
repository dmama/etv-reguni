package ch.vd.uniregctb.tiers.validator;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.iban.IbanValidationException;
import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.utils.ValidateHelper;

/**
 * Validateur de l'objet du meme nom.
 *
 * @author Akram BEN AISSI <mailto:akram.ben-aissi@vd.ch>
 *
 */
public class ComplementEditValidator implements Validator {

	private IbanValidator ibanValidator;

		private TiersService tiersService;

	public IbanValidator getIbanValidator() {
		return ibanValidator;
	}

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

				// --------- --------------Onglets Complements------------------------
				if (StringUtils.isNotBlank(tiers.getNumeroTelecopie())) {
					if (!ValidateHelper.isNumberTel(tiers.getNumeroTelecopie())) {
						errors.rejectValue("tiers.numeroTelecopie", "error.telephone");
					}
				}

				if (StringUtils.isNotBlank(tiers.getNumeroTelephonePrive())) {
					if (!ValidateHelper.isNumberTel(tiers.getNumeroTelephonePrive())) {
						errors.rejectValue("tiers.numeroTelephonePrive", "error.telephone");
					}
				}

				if (StringUtils.isNotBlank(tiers.getNumeroTelephonePortable())) {
					if (!ValidateHelper.isNumberTel(tiers.getNumeroTelephonePortable())) {
						errors.rejectValue("tiers.numeroTelephonePortable", "error.telephone");
					}
				}

				if (StringUtils.isNotBlank(tiers.getNumeroTelephoneProfessionnel())) {
					if (!ValidateHelper.isNumberTel(tiers.getNumeroTelephoneProfessionnel())) {
						errors.rejectValue("tiers.numeroTelephoneProfessionnel", "error.telephone");
					}
				}

				if (StringUtils.isNotBlank(tiers.getAdresseCourrierElectronique())) {
					tiers.setAdresseCourrierElectronique(tiers.getAdresseCourrierElectronique().trim());
					if (!ValidateHelper.isValidEmail(tiers.getAdresseCourrierElectronique())) {
						errors.rejectValue("tiers.adresseCourrierElectronique", "error.email");
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
