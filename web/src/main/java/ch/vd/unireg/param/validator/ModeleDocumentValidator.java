package ch.vd.unireg.param.validator;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.param.view.ModeleDocumentView;

public class ModeleDocumentValidator implements Validator{

	PeriodeFiscaleDAO periodeFiscaleDAO;

	public void setPeriodeFiscaleDAO(PeriodeFiscaleDAO periodeFiscaleDAO) {
		this.periodeFiscaleDAO = periodeFiscaleDAO;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return clazz.isAssignableFrom(ModeleDocumentView.class);
	}

	@Override
	@Transactional(readOnly = true)
	public void validate(Object target, Errors errors) {
		ModeleDocumentView view = (ModeleDocumentView) target;
		PeriodeFiscale pf = periodeFiscaleDAO.get(view.getIdPeriode());
		if (view.getTypeDocument() == null) {
			errors.rejectValue("typeDocument","error.champ.obligatoire");
		}
		if (pf.possedeTypeDocument(view.getTypeDocument())) {
			errors.rejectValue(
					"typeDocument", 
					"error.param.modele.type.existant",
					new Object[]{pf.getAnnee().toString()}, 
					"error.param.modele.type.existant");
		}
	}		
}
