package ch.vd.unireg.validation.rapport;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Mandat;
import ch.vd.unireg.tiers.Tiers;

public class MandatValidator extends RapportEntreTiersValidator<Mandat> {

	@Override
	@NotNull
	public ValidationResults validate(@NotNull Mandat mandat) {
		final ValidationResults vr = super.validate(mandat);
		if (!mandat.isAnnule()) {

			// le type de mandat est obligatoire
			if (mandat.getTypeMandat() == null) {
				vr.addError(String.format("%s %s n'a pas de type de mandat assigné", getEntityCategoryName(), getEntityDisplayString(mandat)));
			}

		}
		return vr;
	}

	@Override
	protected void verificationClasseObjet(ValidationResults vr, Tiers objet) {
		super.verificationClasseObjet(vr, objet);
		if (objet == null) {
			vr.addError("Le mandataire n'existe pas");
		}
	}

	@Override
	protected void verificationClasseSujet(ValidationResults vr, Tiers sujet) {
		super.verificationClasseSujet(vr, sujet);
		if (sujet == null) {
			vr.addError("Le mandant n'existe pas");
		}
		else if (!(sujet instanceof ContribuableImpositionPersonnesPhysiques) && !(sujet instanceof Entreprise)) {
			vr.addError("Le mandant n'est ni une personne physique, ni un ménage commun, ni une entreprise");
		}
	}

	@Override
	protected Class<Mandat> getValidatedClass() {
		return Mandat.class;
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
