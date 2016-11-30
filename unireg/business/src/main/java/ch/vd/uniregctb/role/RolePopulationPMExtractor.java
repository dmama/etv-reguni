package ch.vd.uniregctb.role;

import java.util.EnumSet;

import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;

public class RolePopulationPMExtractor extends RolePopulationExtractorImpl<Entreprise> {

	public RolePopulationPMExtractor() {
		super(EnumSet.of(MotifFor.FIN_EXPLOITATION, MotifFor.DEPART_HS, MotifFor.FUSION_ENTREPRISES));
	}

	@Override
	protected boolean isForAPrendreEnCompte(ForFiscalRevenuFortune ff) {
		return ff.getGenreImpot() == GenreImpot.BENEFICE_CAPITAL;
	}
}
