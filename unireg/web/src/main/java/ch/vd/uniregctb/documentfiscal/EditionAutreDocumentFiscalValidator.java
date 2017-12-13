package ch.vd.uniregctb.documentfiscal;

import org.hibernate.SessionFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.utils.ValidatorUtils;

/**
 * @author Raphaël Marmier, 2017-12-05, <raphael.marmier@vd.ch>
 */
public class EditionAutreDocumentFiscalValidator implements Validator {

	private SessionFactory sessionFactory;

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return EditionDelaiAutreDocumentFiscalView.class.equals(clazz) ||
				AjouterEtatAutreDocumentFiscalView.class.equals(clazz);
	}

	@Override
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public void validate(Object target, Errors errors) {
		if (target instanceof EditionDelaiAutreDocumentFiscalView) {
			validateEditionDelai((EditionDelaiAutreDocumentFiscalView) target, errors);
		}
		else if (target instanceof AjouterEtatAutreDocumentFiscalView) {
			validateAjouterQuittance((AjouterEtatAutreDocumentFiscalView) target, errors);
		}
	}

	private void validateEditionDelai(EditionDelaiAutreDocumentFiscalView view, Errors errors) {

		if (view.getIdDocumentFiscal() == null) {
			errors.reject("error.docfisc.inexistant");
			return;
		}

		final AutreDocumentFiscal doc = (AutreDocumentFiscal) sessionFactory.getCurrentSession().get(AutreDocumentFiscal.class, view.getIdDocumentFiscal());
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

	private void validateAjouterQuittance(AjouterEtatAutreDocumentFiscalView view, Errors errors) {

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
