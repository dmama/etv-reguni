package ch.vd.uniregctb.validation.fors;

import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifRattachement;

public class ForFiscalPrincipalPMValidator extends ForFiscalPrincipalValidator<ForFiscalPrincipalPM> {

	@Override
	protected Class<ForFiscalPrincipalPM> getValidatedClass() {
		return ForFiscalPrincipalPM.class;
	}

	@Override
	protected boolean isGenreImpotCoherent(GenreImpot genreImpot) {
		return genreImpot == GenreImpot.BENEFICE_CAPITAL;
	}

	@Override
	protected boolean isRattachementCoherent(MotifRattachement motif) {
		return MotifRattachement.DOMICILE == motif;
	}
}
