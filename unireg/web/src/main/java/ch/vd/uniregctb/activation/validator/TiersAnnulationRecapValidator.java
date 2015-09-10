package ch.vd.uniregctb.activation.validator;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.activation.view.TiersAnnulationRecapView;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

public class TiersAnnulationRecapValidator implements Validator {

	private TiersService tiersService;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return TiersAnnulationRecapView.class.equals(clazz);
	}

	@Override
	@Transactional(readOnly = true)
	public void validate(Object obj, Errors errors) {
		Assert.isTrue(obj instanceof TiersAnnulationRecapView);

		final TiersAnnulationRecapView tiersAnnulationRecapView = (TiersAnnulationRecapView) obj;
		if (tiersAnnulationRecapView.getDateAnnulation() == null) {
			ValidationUtils.rejectIfEmpty(errors, "dateAnnulation", "error.date.annulation.vide");
			return;
		}

		final Long numeroTiers = tiersAnnulationRecapView.getNumeroTiers();
		final Tiers tiers = tiersService.getTiers(numeroTiers);
		if (tiers == null) {
			errors.reject("global.error.msg", "Le tiers n°" + numeroTiers + " n'existe pas !");
		}
		else {
			final Long numeroTiersRemplacant = tiersAnnulationRecapView.getNumeroTiersRemplacant();
			if (numeroTiersRemplacant != null) {
				final Tiers tiersRemplacant = tiersService.getTiers(numeroTiersRemplacant);
				if (tiersRemplacant == null) {
					errors.rejectValue("numeroTiersRemplacant", "error.tiers.inexistant");
				}
				else if (numeroTiers.longValue() == numeroTiersRemplacant.longValue()) {
					errors.rejectValue("numeroTiersRemplacant", "error.tiers.remplacant.identique");
				}
				else if (tiers.getType() != tiersRemplacant.getType()) {
					errors.rejectValue("numeroTiersRemplacant", "error.tiers.remplacant.type.different", new Object[]{tiersRemplacant.getType(), tiers.getType()}, null);
				}
			}
		}
	}
}
