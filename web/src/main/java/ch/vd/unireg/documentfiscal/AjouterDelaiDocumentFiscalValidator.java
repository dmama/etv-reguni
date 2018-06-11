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
public class AjouterDelaiDocumentFiscalValidator implements Validator {

	private SessionFactory sessionFactory;

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return AjouterDelaiDocumentFiscalView.class.isAssignableFrom(clazz);
	}

	@Override
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public void validate(Object target, Errors errors) {
		validateEditionDelai((AjouterDelaiDocumentFiscalView) target, errors);
	}

	private void validateEditionDelai(AjouterDelaiDocumentFiscalView view, Errors errors) {

		if (view.getIdDocumentFiscal() == null) {
			errors.reject("error.docfisc.inexistant");
			return;
		}

		final DocumentFiscal doc = (DocumentFiscal) sessionFactory.getCurrentSession().get(DocumentFiscal.class, view.getIdDocumentFiscal());
		if (doc == null) {
			errors.reject("error.docfisc.inexistant");
			return;
		}

		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("delaiAccordeAu")) {
			if (view.getDelaiAccordeAu() == null) {
				ValidationUtils.rejectIfEmpty(errors, "delaiAccordeAu", "error.delai.accorde.vide");
			}
			else {
				final RegDate ancienDelaiAccorde = doc.getDelaiAccordeAu();
				if (view.getDelaiAccordeAu().isBefore(RegDate.get()) || (ancienDelaiAccorde != null && view.getDelaiAccordeAu().isBeforeOrEqual(ancienDelaiAccorde))) {
					errors.rejectValue("delaiAccordeAu", "error.delai.accorde.invalide");
				}
			}
		}

		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("dateDemande")) {
			if (view.getDateDemande() == null) {
				ValidationUtils.rejectIfEmpty(errors, "dateDemande", "error.date.demande.vide");
			}
			else if (view.getDateDemande().isAfter(RegDate.get())) {
				if (!ValidatorUtils.alreadyHasErrorOnField(errors, "dateDemande")) {
					errors.rejectValue("dateDemande", "error.date.demande.future");
				}
			}
		}
	}
}
