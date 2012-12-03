package ch.vd.uniregctb.complements;

import org.apache.commons.lang.StringUtils;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.iban.IbanValidationException;
import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.utils.ValidatorUtils;

public class ComplementsValidator implements Validator {

	private IbanValidator ibanValidator;
	private HibernateTemplate hibernateTemplate;

	public void setIbanValidator(IbanValidator ibanValidator) {
		this.ibanValidator = ibanValidator;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@Override
	public boolean supports(Class clazz) {
		return ComplementsEditView.class.equals(clazz);
	}

	@Override
	@Transactional(readOnly = true)
	public void validate(Object obj, Errors errors) {
		final ComplementsEditView view = (ComplementsEditView) obj;

		final long id = view.getId();
		final Tiers tiers = hibernateTemplate.get(Tiers.class, id);
		if (tiers == null) {
			throw new TiersNotFoundException(id);
		}
		view.initReadOnlyData(tiers);

		// --------- --------------Onglets Complements------------------------

		if (StringUtils.isNotBlank(view.getNumeroTelecopie())) {
			if (!ValidatorUtils.isNumberTel(view.getNumeroTelecopie())) {
				errors.rejectValue("numeroTelecopie", "error.telephone");
			}
		}

		if (StringUtils.isNotBlank(view.getNumeroTelephonePrive())) {
			if (!ValidatorUtils.isNumberTel(view.getNumeroTelephonePrive())) {
				errors.rejectValue("numeroTelephonePrive", "error.telephone");
			}
		}

		if (StringUtils.isNotBlank(view.getNumeroTelephonePortable())) {
			if (!ValidatorUtils.isNumberTel(view.getNumeroTelephonePortable())) {
				errors.rejectValue("numeroTelephonePortable", "error.telephone");
			}
		}

		if (StringUtils.isNotBlank(view.getNumeroTelephoneProfessionnel())) {
			if (!ValidatorUtils.isNumberTel(view.getNumeroTelephoneProfessionnel())) {
				errors.rejectValue("numeroTelephoneProfessionnel", "error.telephone");
			}
		}

		if (StringUtils.isNotBlank(view.getAdresseCourrierElectronique())) {
			if (!ValidatorUtils.isValidEmail(view.getAdresseCourrierElectronique())) {
				errors.rejectValue("adresseCourrierElectronique", "error.email");
			}
		}

		final String iban = view.getIban();
		if (StringUtils.isNotBlank(iban)) {
			//[UNIREG-1449] il ne faudrait pas bloquer la sauvegarde de la page des "compléments" si l'IBAN, inchangé, est invalide.
			if (!iban.equals(tiers.getNumeroCompteBancaire())) {
				try {
					ibanValidator.validate(iban);
				}
				catch (IbanValidationException e) {
					if (StringUtils.isBlank(e.getMessage())) {
						errors.rejectValue("numeroCompteBancaire", "error.iban");
					}
					else {
						errors.rejectValue("numeroCompteBancaire", "error.iban.detail", new Object[]{e.getMessage()}, "IBAN invalide");
					}
				}
			}
		}
	}
}
