package ch.vd.uniregctb.registrefoncier;

import java.math.BigDecimal;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class AbstractEditDegrevementViewValidator implements Validator {

	private static final BigDecimal CENT = BigDecimal.valueOf(100L);

	@Override
	public boolean supports(Class<?> clazz) {
		return AbstractEditDegrevementView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final AbstractEditDegrevementView view = (AbstractEditDegrevementView) target;

		// la période de début est obligatoire
		if (view.getPfDebut() == null && !errors.hasFieldErrors("pfDebut")) {
			errors.rejectValue("pfDebut", "error.champ.obligatoire");
		}

		// les montants, surfaces et volumes, si présents doivent être positifs ou nuls
		if (view.getLocation().getRevenu() != null && view.getLocation().getRevenu() < 0) {
			errors.rejectValue("location.revenu", "error.degexo.montant.negatif");
		}
		if (view.getLocation().getVolume() != null && view.getLocation().getVolume() < 0) {
			errors.rejectValue("location.volume", "error.degexo.volume.negatif");
		}
		if (view.getLocation().getSurface() != null && view.getLocation().getSurface() < 0) {
			errors.rejectValue("location.surface", "error.degexo.surface.negative");
		}
		if (view.getPropreUsage().getRevenu() != null && view.getPropreUsage().getRevenu() < 0) {
			errors.rejectValue("propreUsage.revenu", "error.degexo.montant.negatif");
		}
		if (view.getPropreUsage().getVolume() != null && view.getPropreUsage().getVolume() < 0) {
			errors.rejectValue("propreUsage.volume", "error.degexo.volume.negatif");
		}
		if (view.getPropreUsage().getSurface() != null && view.getPropreUsage().getSurface() < 0) {
			errors.rejectValue("propreUsage.surface", "error.degexo.surface.negative");
		}

		// les pourcentages doivent être compris entre 0 et 100, avec deux décimales
		if (view.getLocation().getPourcentage() != null) {
			final BigDecimal value = view.getLocation().getPourcentage();
			if (value.compareTo(CENT) > 0 || value.compareTo(BigDecimal.ZERO) < 0) {
				errors.rejectValue("location.pourcentage", "error.degexo.pourcentage.hors.limites");
			}
		}
		if (view.getLocation().getPourcentageArrete() != null) {
			final BigDecimal value = view.getLocation().getPourcentageArrete();
			if (value.compareTo(CENT) > 0 || value.compareTo(BigDecimal.ZERO) < 0) {
				errors.rejectValue("location.pourcentageArrete", "error.degexo.pourcentage.hors.limites");
			}
			else if (view.getPropreUsage().getPourcentageArrete() == null) {
				errors.rejectValue("propreUsage.pourcentageArrete", "error.degexo.champ.recalcule");
			}
		}
		if (view.getPropreUsage().getPourcentage() != null) {
			final BigDecimal value = view.getPropreUsage().getPourcentage();
			if (value.compareTo(CENT) > 0 || value.compareTo(BigDecimal.ZERO) < 0) {
				errors.rejectValue("propreUsage.pourcentage", "error.degexo.pourcentage.hors.limites");
			}
		}
		if (view.getPropreUsage().getPourcentageArrete() != null) {
			final BigDecimal value = view.getPropreUsage().getPourcentageArrete();
			if (value.compareTo(CENT) > 0 || value.compareTo(BigDecimal.ZERO) < 0) {
				errors.rejectValue("propreUsage.pourcentageArrete", "error.degexo.pourcentage.hors.limites");
			}
			else if (view.getLocation().getPourcentageArrete() == null) {
				errors.rejectValue("location.pourcentageArrete", "error.degexo.champ.recalcule");
			}
		}
		if (view.getLoiLogement().getPourcentageCaractereSocial() != null) {
			final BigDecimal value = view.getLoiLogement().getPourcentageCaractereSocial();
			if (value.compareTo(CENT) > 0 || value.compareTo(BigDecimal.ZERO) < 0) {
				errors.rejectValue("loiLogement.pourcentageCaractereSocial", "error.degexo.pourcentage.hors.limites");
			}
		}
	}
}
