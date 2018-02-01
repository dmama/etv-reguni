package ch.vd.unireg.validation.tiers;

import java.math.BigDecimal;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.tiers.AllegementFiscal;

public abstract class AllegementFiscalValidator<T extends AllegementFiscal> extends DateRangeEntityValidator<T> {

	/**
	 * Le pourcentage d'allègement maximal autorisé
	 */
	private static final BigDecimal MAX_PERCENTAGE = BigDecimal.valueOf(100L);

	/**
	 * Le pourcentage d'allègement minimal autorisé
	 */
	private static final BigDecimal MIN_PERCENTAGE = BigDecimal.ZERO;

	@Override
	protected String getEntityCategoryName() {
		return "L'allègement fiscal";
	}

	@Override
	public ValidationResults validate(T af) {
		final ValidationResults vr = super.validate(af);

		if (!af.isAnnule()) {
			// quelques données obligatoires
			if (af.getTypeImpot() == null) {
				vr.addError(String.format("%s %s n'a pas de type d'impôt assigné.", getEntityCategoryName(), getEntityDisplayString(af)));
			}

			// le pourcentage d'allègement, s'il est fixé, doit avoir une valeur raisonnable
			final BigDecimal pourcentageAllegement = af.getPourcentageAllegement();
			if (pourcentageAllegement != null && (pourcentageAllegement.compareTo(MIN_PERCENTAGE) < 0 || pourcentageAllegement.compareTo(MAX_PERCENTAGE) > 0)) {
				vr.addError(String.format("%s %s a un pourcentage d'allègement hors des limites admises (%s-%s) : %s%%.",
				                          getEntityCategoryName(), getEntityDisplayString(af), MIN_PERCENTAGE, MAX_PERCENTAGE, pourcentageAllegement));
			}
		}

		return vr;
	}

	@Override
	protected boolean isDateDebutFutureAllowed() {
		return true;
	}

	@Override
	protected boolean isDateFinFutureAllowed() {
		return true;
	}
}
