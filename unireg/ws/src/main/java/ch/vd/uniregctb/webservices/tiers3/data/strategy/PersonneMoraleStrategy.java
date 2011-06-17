package ch.vd.uniregctb.webservices.tiers3.data.strategy;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.webservices.tiers3.PersonneMorale;
import ch.vd.unireg.webservices.tiers3.TiersPart;
import ch.vd.unireg.webservices.tiers3.WebServiceException;
import ch.vd.uniregctb.webservices.tiers3.impl.Context;

public class PersonneMoraleStrategy extends ContribuableStrategy<PersonneMorale> {

	@Override
	public PersonneMorale newFrom(ch.vd.uniregctb.tiers.Tiers right, @Nullable Set<TiersPart> parts, Context context) throws WebServiceException {
		throw new NotImplementedException(); // TODO (msi)
//		final PersonneMorale pm = new PersonneMorale();
//		initBase(pm, right, context);
//		initParts(pm, right, parts, context);
//		return pm;
	}

	@Override
	public PersonneMorale clone(PersonneMorale right, @Nullable Set<TiersPart> parts) {
		final PersonneMorale pm = new PersonneMorale();
		copyBase(pm, right);
		copyParts(pm, right, parts, CopyMode.EXCLUSIF);
		return pm;
	}

	@Override
	protected void copyBase(PersonneMorale to, PersonneMorale from) {
		super.copyBase(to, from);
		to.setDesignationAbregee(from.getDesignationAbregee());
		to.setRaisonSociale1(from.getRaisonSociale1());
		to.setRaisonSociale2(from.getRaisonSociale2());
		to.setRaisonSociale3(from.getRaisonSociale3());
		to.setDateFinDernierExerciceCommercial(from.getDateFinDernierExerciceCommercial());
		to.setDateBouclementFutur(from.getDateBouclementFutur());
		to.setNumeroIPMRO(from.getNumeroIPMRO());
	}

	@Override
	protected void copyParts(PersonneMorale to, PersonneMorale from, @Nullable Set<TiersPart> parts, CopyMode mode) {
		super.copyParts(to, from, parts, mode);

		if (parts != null && parts.contains(TiersPart.SIEGES)) {
			copyColl(to.getSieges(), from.getSieges());
		}

		if (parts != null && parts.contains(TiersPart.FORMES_JURIDIQUES)) {
			copyColl(to.getFormesJuridiques(), from.getFormesJuridiques());
		}

		if (parts != null && parts.contains(TiersPart.CAPITAUX)) {
			copyColl(to.getCapitaux(), from.getCapitaux());
		}

		if (parts != null && parts.contains(TiersPart.REGIMES_FISCAUX)) {
			copyColl(to.getRegimesFiscauxICC(), from.getRegimesFiscauxICC());
			copyColl(to.getRegimesFiscauxIFD(), from.getRegimesFiscauxIFD());
		}

		if (parts != null && parts.contains(TiersPart.ETATS_PM)) {
			copyColl(to.getEtats(), from.getEtats());
		}
	}
}
