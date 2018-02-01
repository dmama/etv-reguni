package ch.vd.uniregctb.fourreNeutre;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.fourreNeutre.view.FourreNeutreView;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;

public class FourreNeutreControllerValidator implements Validator {

	private TiersDAO tiersDAO;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return FourreNeutreView.class.equals(clazz);
	}

	@Override
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public void validate(Object target, Errors errors) {
		if (target instanceof FourreNeutreView) {
			validateFourreNeutre((FourreNeutreView) target, errors);
		}
	}

	private void validateFourreNeutre(FourreNeutreView view, Errors errors) {
		// Vérifie que les paramètres reçus sont valides

		final Tiers tiers = tiersDAO.get(view.getTiersId());
		if (tiers == null) {
			errors.reject("error.tiers.inexistant");
			return;
		}
		final Integer periode = view.getPeriodeFiscale();
		if (periode == null) {
			errors.reject("error.fourre.neutre.periode.inconnu");
		}
	}

}
