package ch.vd.uniregctb.role;

import java.util.EnumSet;

import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.type.MotifFor;

public class RolePopulationPPExtractor extends RolePopulationExtractorImpl<ContribuableImpositionPersonnesPhysiques> {

	public RolePopulationPPExtractor() {
		super(EnumSet.of(MotifFor.VEUVAGE_DECES, MotifFor.DEPART_HS));
	}

	@Override
	protected boolean isForAPrendreEnCompte(ForFiscal ff) {
		return true;
	}
}
