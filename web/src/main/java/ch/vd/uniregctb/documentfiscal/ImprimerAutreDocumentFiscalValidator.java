package ch.vd.uniregctb.documentfiscal;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;

public class ImprimerAutreDocumentFiscalValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return ImprimerAutreDocumentFiscalView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final ImprimerAutreDocumentFiscalView view = (ImprimerAutreDocumentFiscalView) target;
		final TypeAutreDocumentFiscalEmettableManuellement typeDocument = view.getTypeDocument();

		if (typeDocument != null) {       // s'il est vide, c'est que quelqu'un joue au con... ça va pêter plus loin
			switch (typeDocument) {
			case AUTORISATION_RADIATION:
				validateDateReference(view, errors);
				break;
			case DEMANDE_BILAN_FINAL:
				validateDateReference(view, errors);
				validatePeriodeFiscale(view, errors);
				break;
			case LETTRE_TYPE_INFORMATION_LIQUIDATION:
				// rien de spécial à faire
				break;
			case LETTRE_BIENVENUE:
				validateTypeLettreBienvenue(view, errors);
				validateDelaiRetour(view, errors);
				break;
			default:
				throw new IllegalArgumentException("Type de document inconnu = [" + typeDocument + "]");
			}
		}
	}

	private static void validateDateReference(ImprimerAutreDocumentFiscalView view, Errors errors) {
		final RegDate dateReference = view.getDateReference();
		if (dateReference == null) {
			errors.rejectValue("dateReference", "error.date.vide");
		}
		else if (dateReference.isAfter(RegDate.get())) {
			errors.rejectValue("dateReference", "error.date.future");
		}
	}

	private static void validatePeriodeFiscale(ImprimerAutreDocumentFiscalView view, Errors errors) {
		final Integer periodeFiscale = view.getPeriodeFiscale();
		if (periodeFiscale == null) {
			errors.rejectValue("periodeFiscale", "error.periode.fiscale.vide");
		}
		else if (periodeFiscale > RegDate.get().year()) {
			errors.rejectValue("periodeFiscale", "error.periode.fiscale.future");
		}
	}

	private static void validateTypeLettreBienvenue(ImprimerAutreDocumentFiscalView view, Errors errors) {
		if (view.getTypeLettreBienvenue() == null) {
			errors.rejectValue("typeLettreBienvenue", "error.type.lettre.bienvenue.vide");
		}
	}

	private static void validateDelaiRetour(ImprimerAutreDocumentFiscalView view, Errors errors) {
		final RegDate delaiRetour = view.getDelaiRetour();
		if (delaiRetour == null) {
			errors.rejectValue("delaiRetour", "error.delai.accorde.vide");
		}
		else if (delaiRetour.isBeforeOrEqual(RegDate.get())) {
			errors.rejectValue("delaiRetour", "error.delai.accorde.invalide");
		}
	}
}
