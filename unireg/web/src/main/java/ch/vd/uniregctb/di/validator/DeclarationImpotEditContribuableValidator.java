package ch.vd.uniregctb.di.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.di.manager.DeclarationImpotEditManager;
import ch.vd.uniregctb.di.view.DeclarationImpotListView;

public class DeclarationImpotEditContribuableValidator implements Validator {

	private DeclarationImpotEditManager diEditManager;

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return DeclarationImpotListView.class.equals(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		DeclarationImpotListView diView = (DeclarationImpotListView) target;

		RegDate date = diEditManager.getDateNewDi(diView.getContribuable().getNumero());
		if(date == null){
			//la période fiscale n'existe pas
			errors.reject("error.di.creation.interdit");
		}

	}

	public DeclarationImpotEditManager getDiEditManager() {
		return diEditManager;
	}

	public void setDiEditManager(DeclarationImpotEditManager diEditManager) {
		this.diEditManager = diEditManager;
	}

}
