package ch.vd.uniregctb.role;

import java.util.EnumSet;

import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;

public class RolePopulationPMExtractor extends RolePopulationExtractorImpl<Entreprise> {

	public RolePopulationPMExtractor() {
		super(EnumSet.of(MotifFor.FIN_EXPLOITATION, MotifFor.DEPART_HS, MotifFor.FUSION_ENTREPRISES));
	}

	@Override
	protected boolean isForAPrendreEnCompte(ForFiscal ff) {
		return ff.getGenreImpot() == GenreImpot.BENEFICE_CAPITAL;
	}
}
