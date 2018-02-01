package ch.vd.unireg.rapport.validator;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.HibernateDateRangeEntity;
import ch.vd.unireg.rapport.view.SetPrincipalView;
import ch.vd.unireg.tiers.Heritage;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;

public class SetPrincipalValidator implements Validator {

	private TiersDAO tiersDAO;

	@Override
	public boolean supports(Class<?> clazz) {
		return SetPrincipalView.class.equals(clazz);
	}

	@Override
	@Transactional(readOnly = true)
	public void validate(Object target, Errors errors) {
		final SetPrincipalView view = (SetPrincipalView) target;

		final RegDate dateDebut = view.getDateDebut();
		if (dateDebut == null) {
			errors.rejectValue("dateDebut", "error.date.debut.vide");
			return; // inutile d'aller plus loin
		}

		final Tiers defunt = tiersDAO.get(view.getDefuntId());
		if (defunt == null) {
			errors.reject("error.tiers.inexistant");
			return; // inutile d'aller plus loin
		}

		// on cherche la date de début la plus récente
		final RegDate maxDateDebut = defunt.getRapportsObjet().stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(Heritage.class::isInstance)
				.map(Heritage.class::cast)
				.filter(h -> h.getPrincipalCommunaute() != null && h.getPrincipalCommunaute())
				.map(HibernateDateRangeEntity::getDateDebut)
				.max(RegDate::compareTo)
				.orElse(null);

		if (maxDateDebut != null && dateDebut.isBefore(maxDateDebut)) {
			// on ne peut pas intercaler un principal entre deux périodes existantes
			errors.rejectValue("dateDebut", "error.date.debut.anterieure.a.autre", new String[] {RegDateHelper.dateToDisplayString(maxDateDebut)}, null);
		}
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}
}
