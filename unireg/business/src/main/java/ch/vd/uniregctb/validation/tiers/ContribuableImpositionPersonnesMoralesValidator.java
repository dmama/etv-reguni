package ch.vd.uniregctb.validation.tiers;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.tiers.ForsParType;

public abstract class ContribuableImpositionPersonnesMoralesValidator<T extends ContribuableImpositionPersonnesMorales> extends ContribuableValidator<T> {

	private ParametreAppService parametreAppService;

	public void setParametreAppService(ParametreAppService parametreAppService) {
		this.parametreAppService = parametreAppService;
	}

	@Override
	protected ValidationResults validateFors(T ctb) {
		final ValidationResults results = super.validateFors(ctb);

		final ForsParType fors = ctb.getForsParType(true);

		// Les fors principaux PP ne sont pas autorisés
		for (ForFiscalPrincipalPP forPP : fors.principauxPP) {
			results.addError("Le for " + forPP + " n'est pas un type de for autorisé sur un contribuable de type PM.");
		}
		return results;
	}

	@Override
	protected boolean isPeriodeImpositionExpected(DeclarationImpotOrdinaire di) {
		return super.isPeriodeImpositionExpected(di)
				&& di.getPeriode() != null
				&& di.getPeriode().getAnnee() >= parametreAppService.getPremierePeriodeFiscalePersonnesMorales();
	}
}
