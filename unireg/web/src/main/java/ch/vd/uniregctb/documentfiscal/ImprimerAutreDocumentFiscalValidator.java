package ch.vd.uniregctb.documentfiscal;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;

public class ImprimerAutreDocumentFiscalValidator implements Validator {

	private static final Set<TypeAutreDocumentFiscalEmettableManuellement> DATE_REFERENCE_OBLIGATOIRE = EnumSet.of(TypeAutreDocumentFiscalEmettableManuellement.AUTORISATION_RADIATION,
	                                                                                                               TypeAutreDocumentFiscalEmettableManuellement.DEMANDE_BILAN_FINAL);

	private static final Set<TypeAutreDocumentFiscalEmettableManuellement> PF_OBLIGATOIRE = EnumSet.of(TypeAutreDocumentFiscalEmettableManuellement.DEMANDE_BILAN_FINAL);

	@Override
	public boolean supports(Class<?> clazz) {
		return ImprimerAutreDocumentFiscalView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final ImprimerAutreDocumentFiscalView view = (ImprimerAutreDocumentFiscalView) target;
		if (view.getTypeDocument() != null) {       // s'il est vide, c'est que quelqu'un joue au con... ça va pêter plus loin
			if (!errors.hasFieldErrors("dateReference")) {
				if (DATE_REFERENCE_OBLIGATOIRE.contains(view.getTypeDocument()) && view.getDateReference() == null) {
					errors.rejectValue("dateReference", "error.date.vide");
				}
				else if (view.getDateReference() != null && view.getDateReference().isAfter(RegDate.get())) {
					errors.rejectValue("dateReference", "error.date.future");
				}
			}
			if (!errors.hasFieldErrors("periodeFiscale")) {
				if (PF_OBLIGATOIRE.contains(view.getTypeDocument()) && view.getPeriodeFiscale() == null) {
					errors.rejectValue("periodeFiscale", "error.periode.fiscale.vide");
				}
				else if (view.getPeriodeFiscale() != null && view.getPeriodeFiscale() > RegDate.get().year()) {
					errors.rejectValue("periodeFiscale", "error.periode.fiscale.future");
				}
			}
		}
	}
}
