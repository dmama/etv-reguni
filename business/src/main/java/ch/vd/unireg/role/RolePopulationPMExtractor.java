package ch.vd.unireg.role;

import java.util.EnumSet;

import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ForFiscalRevenuFortune;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;

public class RolePopulationPMExtractor extends RolePopulationExtractorImpl<Entreprise> {

	public RolePopulationPMExtractor() {
		super(EnumSet.of(MotifFor.FIN_EXPLOITATION, MotifFor.DEPART_HS, MotifFor.FUSION_ENTREPRISES, MotifFor.FAILLITE));
	}

	@Override
	protected boolean isForAPrendreEnCompte(ForFiscalRevenuFortune ff) {
		return ff.getGenreImpot() == GenreImpot.BENEFICE_CAPITAL;
	}
}
