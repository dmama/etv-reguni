package ch.vd.unireg.tiers.validator;

import java.math.BigDecimal;

import org.springframework.validation.Errors;

import ch.vd.unireg.tiers.AllegementFiscal;
import ch.vd.unireg.tiers.view.AddAllegementFiscalView;

public class AddAllegementFiscalViewValidator extends AbstractAllegementFiscalViewValidator {

	private static final BigDecimal HUNDRED = new BigDecimal(100L);

	@Override
	public boolean supports(Class<?> clazz) {
		return AddAllegementFiscalView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final AddAllegementFiscalView view = (AddAllegementFiscalView) target;

		// les dates
		validateRange(view, errors);

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

		// pour le type de collectivité commune, si on n'a pas choisi "toutes les communes", il faut une commune
		if (view.getTypeCollectivite() == AllegementFiscal.TypeCollectivite.COMMUNE && !view.isToutesCommunes() && view.getNoOfsCommune() == null) {
			errors.rejectValue("noOfsCommune", "error.commune.non.vd");
		}

		// montant / pourcentage d'allègement ?
		if (view.getFlagPourcentageMontant() == null) {
			errors.rejectValue("flagPourcentageMontant", "error.choix.pourcentage.montant.allegement.vide");
		}
		else if (view.getFlagPourcentageMontant() == AddAllegementFiscalView.PourcentageMontant.POURCENTAGE) {

			// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
			if (!errors.hasFieldErrors("pourcentageAllegement")) {
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
}
