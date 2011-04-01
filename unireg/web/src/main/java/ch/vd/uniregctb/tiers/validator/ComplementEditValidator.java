package ch.vd.uniregctb.tiers.validator;

import org.apache.commons.lang.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.iban.IbanValidationException;
import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.view.ComplementView;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.utils.ValidatorUtils;

/**
 * Validateur de l'objet du meme nom.
 *
 * @author Akram BEN AISSI <mailto:akram.ben-aissi@vd.ch>
 *
 */
public class ComplementEditValidator implements Validator {

	private IbanValidator ibanValidator;

	private TiersService tiersService;

	@SuppressWarnings({"UnusedDeclaration"})
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
			final ComplementView complement = tiersView.getComplement();
			if (complement != null) {

				// --------- --------------Onglets Complements------------------------
				if (StringUtils.isNotBlank(complement.getNumeroTelecopie())) {
					if (!ValidatorUtils.isNumberTel(complement.getNumeroTelecopie())) {
						errors.rejectValue("tiers.numeroTelecopie", "error.telephone");
					}
				}

				if (StringUtils.isNotBlank(complement.getNumeroTelephonePrive())) {
					if (!ValidatorUtils.isNumberTel(complement.getNumeroTelephonePrive())) {
						errors.rejectValue("tiers.numeroTelephonePrive", "error.telephone");
					}
				}

				if (StringUtils.isNotBlank(complement.getNumeroTelephonePortable())) {
					if (!ValidatorUtils.isNumberTel(complement.getNumeroTelephonePortable())) {
						errors.rejectValue("tiers.numeroTelephonePortable", "error.telephone");
					}
				}

				if (StringUtils.isNotBlank(complement.getNumeroTelephoneProfessionnel())) {
					if (!ValidatorUtils.isNumberTel(complement.getNumeroTelephoneProfessionnel())) {
						errors.rejectValue("tiers.numeroTelephoneProfessionnel", "error.telephone");
					}
				}

				if (StringUtils.isNotBlank(complement.getAdresseCourrierElectronique())) {
					if (!ValidatorUtils.isValidEmail(complement.getAdresseCourrierElectronique())) {
						errors.rejectValue("tiers.adresseCourrierElectronique", "error.email");
					}
				}

				if (complement.getCompteBancaire() != null) {
					final String iban = complement.getCompteBancaire().getIban();
					if (StringUtils.isNotBlank(iban)) {
						//[UNIREG-1449] il ne faudrait pas bloquer la sauvegarde de la page des "compléments" si l'IBAN, inchangé, est invalide.
						final Tiers tiersInBase = tiersService.getTiers(tiersView.getTiers().getNumero());
						if (!iban.equals(tiersInBase.getNumeroCompteBancaire())) {
							try {
								ibanValidator.validate(iban);
							}
							catch (IbanValidationException e) {
								if (StringUtils.isBlank(e.getMessage())) {
									errors.rejectValue("tiers.numeroCompteBancaire", "error.iban");
								}
								else {
									errors.rejectValue("tiers.numeroCompteBancaire", "error.iban.detail", new Object[] { e.getMessage() }, "IBAN invalide");
								}
							}
						}
					}
				}
			}
		}
	}




}
