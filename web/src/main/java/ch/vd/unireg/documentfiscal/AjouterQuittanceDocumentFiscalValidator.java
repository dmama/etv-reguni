package ch.vd.unireg.documentfiscal;

import org.hibernate.SessionFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.utils.ValidatorUtils;

/**
 * Validateur des données nécessaires à l'ajout d'un délai sur un document fiscal.
 */
public class AjouterQuittanceDocumentFiscalValidator implements Validator {

	private SessionFactory sessionFactory;

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return AjouterQuittanceDocumentFiscalView.class.equals(clazz);
	}

	@Override
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public void validate(Object target, Errors errors) {
		validateAjouterQuittance((AjouterQuittanceDocumentFiscalView) target, errors);
	}

	private void validateAjouterQuittance(AjouterQuittanceDocumentFiscalView view, Errors errors) {

		if (view.getId() == null) {
			errors.reject("error.docfisc.inexistant");
			return;
		}

		final AutreDocumentFiscal doc = (AutreDocumentFiscal) sessionFactory.getCurrentSession().get(AutreDocumentFiscal.class, view.getId());
		if (doc == null) {
			errors.reject("error.docfisc.inexistant");
			return;
		}

		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("dateRetour")) {
			if (view.getDateRetour() == null) {
				ValidationUtils.rejectIfEmpty(errors, "dateRetour", "error.date.retour.vide");
			}
			else if (view.getDateRetour().isAfter(RegDate.get())) {
				if (!ValidatorUtils.alreadyHasErrorOnField(errors, "dateRetour")) {
					errors.rejectValue("dateRetour", "error.date.retour.future");
				}
			}
		}
	}

}
