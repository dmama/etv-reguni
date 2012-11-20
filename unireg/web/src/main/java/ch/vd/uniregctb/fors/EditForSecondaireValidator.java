package ch.vd.uniregctb.fors;

public class EditForSecondaireValidator extends EditForRevenuFortuneValidator {

	@Override
	public boolean supports(Class<?> clazz) {
		return EditForSecondaireView.class.equals(clazz);
	}
}
