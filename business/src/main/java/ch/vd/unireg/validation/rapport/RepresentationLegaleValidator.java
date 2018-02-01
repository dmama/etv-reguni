package ch.vd.uniregctb.validation.rapport;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RepresentationLegale;
import ch.vd.uniregctb.tiers.Tiers;

public abstract class RepresentationLegaleValidator<T extends RepresentationLegale> extends RapportEntreTiersValidator<T> {

	@Override
	protected void verificationClasses(ValidationResults vr, T ret) {
		super.verificationClasses(vr, ret);
		final Tiers autorite = ret.getAutoriteTutelaireId() != null ? tiersDAO.get(ret.getAutoriteTutelaireId()) : null;
		verificationClasseAutoriteTutelaire(vr, autorite);
	}

	protected void verificationClasseAutoriteTutelaire(ValidationResults vr, @Nullable Tiers autoriteTutelaire) {
		if (autoriteTutelaire != null && !(autoriteTutelaire instanceof CollectiviteAdministrative)) {
			vr.addError("L'autorité tutélaire d'une représentation légale ne peut être qu'une collectivité administrative");
		}
	}

	@Override
	protected void verificationClasseSujet(ValidationResults vr, Tiers sujet) {
		super.verificationClasseSujet(vr, sujet);
		if (sujet == null) {
			vr.addError("Le tiers sous représentation légale n'existe pas");
		}
		else if (!(sujet instanceof PersonnePhysique)) { // [SIFISC-719]
			vr.addError("Une représentation légale ne peut s'appliquer que sur une personne physique");
		}
	}
}
