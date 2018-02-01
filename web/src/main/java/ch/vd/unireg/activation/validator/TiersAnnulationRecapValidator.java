package ch.vd.uniregctb.activation.validator;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
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
		final TiersAnnulationRecapView tiersAnnulationRecapView = (TiersAnnulationRecapView) obj;

		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("dateAnnulation")) {
			if (tiersAnnulationRecapView.getDateAnnulation() == null) {
				errors.rejectValue("dateAnnulation", "error.date.annulation.vide");
			}
			else if (tiersAnnulationRecapView.getDateAnnulation().isAfter(RegDate.get())) {
				errors.rejectValue("dateAnnulation", "error.date.annulation.future");
			}
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
