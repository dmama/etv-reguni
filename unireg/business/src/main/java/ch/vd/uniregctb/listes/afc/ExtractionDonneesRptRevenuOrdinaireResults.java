package ch.vd.uniregctb.listes.afc;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersService;

public class ExtractionDonneesRptRevenuOrdinaireResults extends ExtractionDonneesRptPeriodeImpositionResults {

	public ExtractionDonneesRptRevenuOrdinaireResults(RegDate dateTraitement, int periodeFiscale, int nbThreads, TiersService tiersService, ServiceInfrastructureService infraService,
	                                                  AssujettissementService assujettissementService, PeriodeImpositionService periodeImpositionService) {
		super(dateTraitement, periodeFiscale, nbThreads, tiersService, infraService, assujettissementService, periodeImpositionService);
	}

	@Override
	public TypeExtractionDonneesRpt getMode() {
		return TypeExtractionDonneesRpt.REVENU_ORDINAIRE;
	}

	@Override
	protected String filterPeriodes(Contribuable ctb, List<PeriodeImposition> listeAFiltrer) {
		return null;
	}
}
