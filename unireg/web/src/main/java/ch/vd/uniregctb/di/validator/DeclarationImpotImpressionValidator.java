package ch.vd.uniregctb.di.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.di.view.DeclarationImpotImpressionView;

public class DeclarationImpotImpressionValidator implements Validator {

	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return DeclarationImpotImpressionView.class.equals(clazz);
	}

	public void validate(Object target, Errors errors) {
/*		DeclarationImpotImpressionView diImpressionView = (DeclarationImpotImpressionView) target;
		List<ModeleDocumentView> modelesDocumentView = diImpressionView.getModelesDocumentView();
		Iterator<ModeleDocumentView> itModelesDocumentView = modelesDocumentView.iterator();
		while (itModelesDocumentView.hasNext()) {
			ModeleDocumentView modeleDocumentView = itModelesDocumentView.next();
			if (modeleDocumentView.getTypeDocument().equals(diImpressionView.getSelectedTypeDocument())) {
				List<ModeleFeuilleDocumentEditique> modelesFeuille = modeleDocumentView.getModelesFeuilles();
				Iterator<ModeleFeuilleDocumentEditique> itModelesFeuille = modelesFeuille.iterator();
				while (itModelesFeuille.hasNext()) {
					ModeleFeuilleDocumentEditique modeleFeuille = itModelesFeuille.next();
				}
			}
		}
*/
	}

}
