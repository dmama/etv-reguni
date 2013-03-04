package ch.vd.uniregctb.complements;

import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.iban.IbanValidationException;
import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.tiers.Tiers;

public class ComplementsEditCoordonneesFinancieresValidator implements Validator {

	private IbanValidator ibanValidator;
	private HibernateTemplate hibernateTemplate;

	public ComplementsEditCoordonneesFinancieresValidator(IbanValidator ibanValidator, HibernateTemplate hibernateTemplate) {
		this.ibanValidator = ibanValidator;
		this.hibernateTemplate = hibernateTemplate;
	}

	@Override
	public boolean supports(Class clazz) {
		return ComplementsEditCoordonneesFinancieresView.class.equals(clazz);
	}

	@Override
	@Transactional(readOnly = true)
	public void validate(Object obj, Errors errors) {
		final ComplementsEditCoordonneesFinancieresView view = (ComplementsEditCoordonneesFinancieresView) obj;

		final long id = view.getId();
		final Tiers tiers = hibernateTemplate.get(Tiers.class, id);
		if (tiers == null) {
			throw new TiersNotFoundException(id);
		}
		view.initReadOnlyData(tiers);

		final String iban = view.getIban();
		if (StringUtils.isNotBlank(iban)) {
			//[UNIREG-1449] il ne faudrait pas bloquer la sauvegarde de la page des "compléments" si l'IBAN, inchangé, est invalide.
			if (!iban.equals(tiers.getNumeroCompteBancaire())) {
				try {
					ibanValidator.validate(iban);
				}
				catch (IbanValidationException e) {
					if (StringUtils.isBlank(e.getMessage())) {
						errors.rejectValue("iban", "error.iban");
					}
					else {
						errors.rejectValue("iban", "error.iban.detail", new Object[]{e.getMessage()}, "IBAN invalide");
					}
				}
			}
		}
	}
}
