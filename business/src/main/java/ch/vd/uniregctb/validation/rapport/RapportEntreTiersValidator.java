package ch.vd.uniregctb.validation.rapport;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.validation.tiers.DateRangeEntityValidator;

public abstract class RapportEntreTiersValidator<T extends RapportEntreTiers> extends DateRangeEntityValidator<T> {

	protected TiersDAO tiersDAO;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@Override
	public ValidationResults validate(T ret) {
		final ValidationResults vr = super.validate(ret);

		if (ret.isAnnule()) {
			return vr;
		}

		// les deux membres du rapport doivent être renseignés
		if (ret.getSujetId() == null || ret.getObjetId() == null) {
			vr.addError(String.format("Les participants du rapport entre tiers %s ne sont pas assignés", getEntityDisplayString(ret)));
		}
		else {

			// le tiers ne doit pas être le même côté sujet et côté objet
			if (ret.getSujetId().equals(ret.getObjetId())) {
				vr.addError(String.format("Le même tiers %d est présent aux deux extrêmités du rapport entre tiers %s", ret.getSujetId(), getEntityDisplayString(ret)));
			}

			verificationClasses(vr, ret);
		}
		return vr;
	}

	protected void verificationClasses(ValidationResults vr, T ret) {
		verificationClasseObjet(vr, tiersDAO.get(ret.getObjetId()));
		verificationClasseSujet(vr, tiersDAO.get(ret.getSujetId()));
	}

	protected void verificationClasseObjet(ValidationResults vr, Tiers objet) {
	}

	protected void verificationClasseSujet(ValidationResults vr, Tiers sujet) {
	}

	private static String noCtbToDisplayString(Long id) {
		if (id != null) {
			return FormatNumeroHelper.numeroCTBToDisplay(id);
		}
		else {
			return "?";
		}
	}

	@Override
	protected String getEntityCategoryName() {
		return "Le rapport-entre-tiers";
	}

	@Override
	protected String getEntityDisplayString(@NotNull T ret) {
		return String.format("%s entre le tiers %s %s et le tiers %s %s",
		                     super.getEntityDisplayString(ret),
		                     ret.getDescriptionTypeSujet(), noCtbToDisplayString(ret.getSujetId()),
		                     ret.getDescriptionTypeObjet(), noCtbToDisplayString(ret.getObjetId()));
	}
}
