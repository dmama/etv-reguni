package ch.vd.uniregctb.listes.afc;

import java.util.Iterator;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.SourcierPur;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersService;

public class ExtractionDonneesRptRevenuSourcePureResults extends ExtractionDonneesRptResults {

	private static final String ASSUJETTI_ORDINAIRE = "Assujetti au rôle ordinaire";

	public ExtractionDonneesRptRevenuSourcePureResults(RegDate dateTraitement, int periodeFiscale, int nbThreads, TiersService tiersService, ServiceInfrastructureService infraService) {
		super(dateTraitement, periodeFiscale, nbThreads, tiersService, infraService);
	}

	@Override
	public TypeExtractionDonneesRpt getMode() {
		return TypeExtractionDonneesRpt.REVENU_SOURCE_PURE;
	}

	@Override
	protected String filterAssujettissements(Contribuable ctb, List<Assujettissement> listeAFiltrer) {
		// ici, on ne prend en compte que les assujettissements "source pur"
		final Iterator<Assujettissement> iterator = listeAFiltrer.iterator();
		while (iterator.hasNext()) {
			final Assujettissement a = iterator.next();
			if (!(a instanceof SourcierPur)) {
				iterator.remove();
			}
		}

		return listeAFiltrer.size() == 0 ? ASSUJETTI_ORDINAIRE : null;
	}
}
