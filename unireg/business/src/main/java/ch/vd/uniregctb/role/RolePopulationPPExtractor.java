package ch.vd.uniregctb.role;

import java.util.EnumSet;
import java.util.function.BiPredicate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;

public class RolePopulationPPExtractor extends RolePopulationExtractorImpl<ContribuableImpositionPersonnesPhysiques> {

	private final BiPredicate<ContribuableImpositionPersonnesPhysiques, RegDate> detecteurSourcierGris;

	public RolePopulationPPExtractor(BiPredicate<ContribuableImpositionPersonnesPhysiques, RegDate> detecteurSourcierGris) {
		super(EnumSet.of(MotifFor.VEUVAGE_DECES, MotifFor.DEPART_HS));
		this.detecteurSourcierGris = detecteurSourcierGris;
	}

	@Override
	protected boolean isForAPrendreEnCompte(ForFiscalRevenuFortune ff) {
		return ff.getMotifRattachement() != MotifRattachement.DIPLOMATE_SUISSE;
	}

	@Override
	protected boolean isExcludedByNature(ContribuableImpositionPersonnesPhysiques contribuable, RegDate dateReference) {
		return super.isExcludedByNature(contribuable, dateReference) || detecteurSourcierGris.test(contribuable, dateReference);
	}
}
