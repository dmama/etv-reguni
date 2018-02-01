package ch.vd.unireg.listes.afc;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.assujettissement.Assujettissement;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.metier.assujettissement.SourcierPur;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.TiersService;

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
