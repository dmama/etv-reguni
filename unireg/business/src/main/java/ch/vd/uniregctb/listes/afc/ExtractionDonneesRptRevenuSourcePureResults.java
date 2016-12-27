package ch.vd.uniregctb.listes.afc;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.SourcierPur;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersService;

public class ExtractionDonneesRptRevenuSourcePureResults extends ExtractionDonneesRptAssujettissementResults {

	private static final String ASSUJETTI_ORDINAIRE = "Assujetti au rôle ordinaire";

	public ExtractionDonneesRptRevenuSourcePureResults(RegDate dateTraitement, int periodeFiscale, int nbThreads, TiersService tiersService, ServiceInfrastructureService infraService,
	                                                   AssujettissementService assujettissementService, AdresseService adresseService) {
		super(dateTraitement, periodeFiscale, nbThreads, tiersService, infraService, assujettissementService, adresseService);
	}

	@Override
	public TypeExtractionDonneesRpt getMode() {
		return TypeExtractionDonneesRpt.REVENU_SOURCE_PURE;
	}

	@Override
	protected String filterAssujettissements(Contribuable ctb, List<Assujettissement> listeAFiltrer) {
		// ici, on ne prend en compte que les assujettissements "source pur"
		listeAFiltrer.removeIf(a -> !(a instanceof SourcierPur));

		return listeAFiltrer.isEmpty() ? ASSUJETTI_ORDINAIRE : null;
	}
}
