package ch.vd.unireg.validation.rapport;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.tiers.Heritage;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;

public class HeritageValidator extends RapportEntreTiersValidator<Heritage> {

	private TiersService tiersService;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@Override
	protected Class<Heritage> getValidatedClass() {
		return Heritage.class;
	}

	@Override
	protected void verificationClasseObjet(ValidationResults vr, Tiers objet) {
		super.verificationClasseObjet(vr, objet);
		if (objet == null) {
			vr.addError("Le tiers défunt n'existe pas");
		}
		else if (!(objet instanceof PersonnePhysique)) {
			vr.addError(String.format("Le tiers défunt %s n'est pas une personne physique", FormatNumeroHelper.numeroCTBToDisplay(objet.getNumero())));
		}
	}

	@Override
	protected void verificationClasseSujet(ValidationResults vr, Tiers sujet) {
		super.verificationClasseSujet(vr, sujet);
		if (sujet == null) {
			vr.addError("Le tiers héritier n'existe pas");
		}
		else if (!(sujet instanceof PersonnePhysique)) {
			vr.addError(String.format("Le tiers héritier %s n'est pas une personne physique", FormatNumeroHelper.numeroCTBToDisplay(sujet.getNumero())));
		}
	}

	@Override
	@NotNull
	public ValidationResults validate(@NotNull Heritage ret) {
		final ValidationResults vr = super.validate(ret);
		if (!ret.isAnnule()) {
			final Tiers defunt = tiersDAO.get(ret.getObjetId());

			// si ce n'est pas le cas, ça a dû déjà être remonté...
			if (defunt instanceof PersonnePhysique) {
				final RegDate dateDeces = tiersService.getDateDeces((PersonnePhysique) defunt);
				if (dateDeces == null) {
					vr.addWarning(String.format("%s alors que le 'défunt' n'est pas décédé", getEntityDisplayString(ret)));
				}
			}
		}
		return vr;
	}
}
