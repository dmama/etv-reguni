package ch.vd.uniregctb.listes.afc;

import java.util.Iterator;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.HorsCanton;
import ch.vd.uniregctb.metier.assujettissement.SourcierPur;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersService;

public class ExtractionDonneesRptRevenuOrdinaireResults extends ExtractionDonneesRptResults {

	private static final String SOURCIER_PUR = "Sourcier pur";
	private static final String HORS_CANTON = "Hors canton";

	public ExtractionDonneesRptRevenuOrdinaireResults(RegDate dateTraitement, int periodeFiscale, int nbThreads, TiersService tiersService, ServiceInfrastructureService infraService) {
		super(dateTraitement, periodeFiscale, nbThreads, tiersService, infraService);
	}

	@Override
	public TypeExtractionDonneesRpt getMode() {
		return TypeExtractionDonneesRpt.REVENU_ORDINAIRE;
	}

	@Override
	protected String filterAssujettissements(Contribuable ctb, List<Assujettissement> listeAFiltrer) {
		// ici, on ne tient pas compte des sourciers purs
		final Iterator<Assujettissement> iterSource = listeAFiltrer.iterator();
		while (iterSource.hasNext()) {
			final Assujettissement a = iterSource.next();
			if (a instanceof SourcierPur) {
				iterSource.remove();
			}
		}

		if (listeAFiltrer.size() == 0) {
			return SOURCIER_PUR;
		}

		// on doit également enlever les contribuables HC
		final Iterator<Assujettissement> iterHorsCanton = listeAFiltrer.iterator();
		while (iterHorsCanton.hasNext()) {
			final Assujettissement a = iterHorsCanton.next();
			if (a instanceof HorsCanton) {
				iterHorsCanton.remove();
			}
		}

		return (listeAFiltrer.size() == 0 ? HORS_CANTON : null);
	}
}
