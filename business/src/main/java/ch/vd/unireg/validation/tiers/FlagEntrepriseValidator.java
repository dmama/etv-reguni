package ch.vd.unireg.validation.tiers;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.tiers.FlagEntreprise;

public class FlagEntrepriseValidator extends DateRangeEntityValidator<FlagEntreprise> {

	@Override
	protected Class<FlagEntreprise> getValidatedClass() {
		return FlagEntreprise.class;
	}

	@Override
	protected String getEntityCategoryName() {
		return "Le flag entreprise";
	}

	@Override
	protected boolean isDateDebutFutureAllowed() {
		return true;
	}

	@Override
	protected boolean isDateFinFutureAllowed() {
		return true;
	}

	@Override
	public ValidationResults validate(FlagEntreprise flag) {
		final ValidationResults vr = super.validate(flag);
		if (!flag.isAnnule()) {
			// le type est obligatoire
			if (flag.getType() == null) {
				vr.addError("Le type de flag entreprise est obligatoire.");
			}
		}
		return vr;
	}
}
