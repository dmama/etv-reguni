package ch.vd.unireg.identification.contribuable.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.unireg.identification.contribuable.view.IdentificationContribuableListCriteria;
import ch.vd.unireg.identification.contribuable.view.IdentificationMessagesStatsCriteriaView;

public class IdentificationMessageValidator implements Validator {

	private IdentificationMessagesListValidator validatorList;

	private IdentificationMessagesStatsValidator validatorStats;

	public void setValidatorList(IdentificationMessagesListValidator validatorList) {
		this.validatorList = validatorList;
	}

	public void setValidatorStats(IdentificationMessagesStatsValidator validatorStats) {
		this.validatorStats = validatorStats;
	}

	@Override
	public boolean supports(Class clazz) {
		return IdentificationContribuableListCriteria.class.equals(clazz) || IdentificationMessagesStatsCriteriaView.class.equals(clazz) ;
	}

	@Override
	public void validate(Object target, Errors errors) {

		if(target instanceof IdentificationMessagesStatsCriteriaView){
			validatorStats.validate(target,errors);
		}
		else if( target instanceof IdentificationContribuableListCriteria){
			validatorList.validate(target,errors);
		}
		else{
			throw new IllegalArgumentException();
		}


	}
}
