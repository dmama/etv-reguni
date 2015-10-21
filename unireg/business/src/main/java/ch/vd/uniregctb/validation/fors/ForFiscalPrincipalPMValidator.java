package ch.vd.uniregctb.validation.fors;

import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifRattachement;

public class ForFiscalPrincipalPMValidator extends ForFiscalPrincipalValidator<ForFiscalPrincipalPM> {

	private TiersService tiersService;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@Override
	protected Class<ForFiscalPrincipalPM> getValidatedClass() {
		return ForFiscalPrincipalPM.class;
	}

	@Override
	protected boolean isGenreImpotCoherent(ForFiscalPrincipalPM forFiscal) {
		final ContribuableImpositionPersonnesMorales cipm = forFiscal.getTiers();
		if (cipm instanceof Entreprise && tiersService.isSocieteDePersonnes((Entreprise) cipm, forFiscal.getDateDebut())) {
			return forFiscal.getGenreImpot() == GenreImpot.REVENU_FORTUNE;
		}
		else {
			return forFiscal.getGenreImpot() == GenreImpot.BENEFICE_CAPITAL;
		}
	}

	@Override
	protected boolean isRattachementCoherent(MotifRattachement motif) {
		return MotifRattachement.DOMICILE == motif;
	}
}
