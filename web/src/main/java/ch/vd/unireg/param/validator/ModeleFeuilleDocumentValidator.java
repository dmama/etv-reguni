package ch.vd.unireg.param.validator;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.ModeleDocumentDAO;
import ch.vd.unireg.param.view.ModeleFeuilleDocumentView;

public class ModeleFeuilleDocumentValidator implements Validator {

	private ModeleDocumentDAO modeleDocumentDAO;

	public void setModeleDocumentDAO(ModeleDocumentDAO modeleDocumentDAO) {
		this.modeleDocumentDAO = modeleDocumentDAO;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return clazz.isAssignableFrom(ModeleFeuilleDocumentView.class);
	}

	@Override
	@Transactional(readOnly = true)
	public void validate(Object target, Errors errors) {
		ModeleFeuilleDocumentView view = (ModeleFeuilleDocumentView) target;
		ModeleDocument modeleDocument = modeleDocumentDAO.get(view.getIdModele());
		if (modeleDocument.possedeModeleFeuilleDocument(view.getModeleFeuille().getNoCADEV())) {
			errors.rejectValue("modeleFeuille", "error.modele.feuille.existante");
		}
	}

}
