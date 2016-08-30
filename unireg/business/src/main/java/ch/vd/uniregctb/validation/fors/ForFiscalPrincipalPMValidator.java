package ch.vd.uniregctb.validation.fors;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
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

	/**
	 * Sur le principe, un for fiscal sur une société de personnes (= qui n'est pas un sujet fiscal) devrait avoir le genre d'impôt {@link GenreImpot#REVENU_FORTUNE}
	 * alors qu'il devrait être {@link GenreImpot#BENEFICE_CAPITAL} pour une société de capitaux. Le problème, c'est que l'historique des formes juridiques d'une
	 * entreprise n'est que partiellement connu (dans RegPM, on le connaissait plutôt bien, mais dans RCEnt, il n'y a pas d'historique avant leur chargement initial de mi-2015),
	 * donc tout ce que l'on peut faire, c'est interdire le genre d'impôt {@link GenreImpot#REVENU_FORTUNE} sur les périodes où on est certain que la forme juridique
	 * de l'entreprise ne correspond pas à une société de personnes...
	 * @param forFiscal le for fiscal à tester
	 * @return <code>true</code> si le genre d'impôt du for fiscal est autorisé
	 */
	@Override
	protected boolean isGenreImpotCoherent(ForFiscalPrincipalPM forFiscal) {
		final Set<GenreImpot> allowed = EnumSet.of(GenreImpot.BENEFICE_CAPITAL);
		final ContribuableImpositionPersonnesMorales cipm = forFiscal.getTiers();
		if (cipm instanceof Entreprise) {
			final List<DateRange> nonSP = tiersService.getPeriodesNonSocieteDePersonnesNiIndividuelle((Entreprise) cipm);
			if (!DateRangeHelper.intersect(forFiscal, nonSP)) {
				// ok, le for est complètement dans une zone dans laquelle on ne peut pas dire avec
				// certitude que l'entreprise n'est pas une société de personnes : on autorise les deux genres d'impôt
				allowed.add(GenreImpot.REVENU_FORTUNE);
			}
		}
		return allowed.contains(forFiscal.getGenreImpot());
	}

	@Override
	protected boolean isRattachementCoherent(MotifRattachement motif) {
		return MotifRattachement.DOMICILE == motif;
	}
}
