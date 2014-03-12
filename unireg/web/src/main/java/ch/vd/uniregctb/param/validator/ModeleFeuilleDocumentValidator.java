package ch.vd.uniregctb.param.validator;

import java.lang.annotation.Target;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.param.view.ModeleFeuilleDocumentView;

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
		if (modeleDocument.possedeModeleFeuilleDocument(view.getModeleFeuille().getCode())) {
			errors.rejectValue("modeleFeuille", "error.modele.feuille.existante");
		}
	}

}
