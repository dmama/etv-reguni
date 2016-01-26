package ch.vd.uniregctb.tiers.validator;

import java.math.BigDecimal;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.view.AddAllegementFiscalView;

public class AddAllegementFiscalViewValidator implements Validator {

	private static final BigDecimal HUNDRED = new BigDecimal(100L);

	@Override
	public boolean supports(Class<?> clazz) {
		return AddAllegementFiscalView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final AddAllegementFiscalView view = (AddAllegementFiscalView) target;

		// présence des dates et cohérence entre elles
		if (view.getDateDebut() == null) {
			errors.rejectValue("dateDebut", "error.date.debut.vide");
		}
		else if (view.getDateFin() != null && view.getDateDebut().isAfter(view.getDateFin())) {
			errors.rejectValue("dateFin", "error.date.fin.avant.debut");
		}

		// date de début dans le futur -> interdit
		final RegDate today = RegDate.get();
		if (view.getDateDebut() != null && today.isBefore(view.getDateDebut())) {
			errors.rejectValue("dateDebut", "error.date.debut.future");
		}

		// le type de collectivité et le type d'impôt sont obligatoires
		if (view.getTypeCollectivite() == null) {
			errors.rejectValue("typeCollectivite", "error.type.collectivite.vide");
		}
		else {
			switch (view.getTypeCollectivite()) {
			case COMMUNE:
			case CANTON:
				if (view.getTypeICC() == null) {
					errors.rejectValue("typeICC", "error.type.allegement.vide");
				}
				break;
			case CONFEDERATION:
				if (view.getTypeIFD() == null) {
					errors.rejectValue("typeIFD", "error.type.allegement.vide");
				}
				break;
			default:
				throw new IllegalArgumentException("Type de collectivité non-supporté : " + view.getTypeCollectivite());
			}
		}
		if (view.getTypeImpot() == null) {
			errors.rejectValue("typeImpot", "error.type.impot.vide");
		}

		// montant / pourcentage d'allègement ?
		if (view.getFlagPourcentageMontant() == null) {
			errors.rejectValue("flagPourcentageMontant", "error.choix.pourcentage.montant.allegement.vide");
		}
		else if (view.getFlagPourcentageMontant() == AddAllegementFiscalView.PourcentageMontant.POURCENTAGE) {
			if (view.getPourcentageAllegement() == null) {
				errors.rejectValue("pourcentageAllegement", "error.pourcentage.allegement.vide");
			}
			else if (BigDecimal.ZERO.compareTo(view.getPourcentageAllegement()) > 0) {
				errors.rejectValue("pourcentageAllegement", "error.pourcentage.allegement.invalide");
			}
			else if (HUNDRED.compareTo(view.getPourcentageAllegement()) < 0) {
				errors.rejectValue("pourcentageAllegement", "error.pourcentage.allegement.invalide");
			}
		}
	}
}
