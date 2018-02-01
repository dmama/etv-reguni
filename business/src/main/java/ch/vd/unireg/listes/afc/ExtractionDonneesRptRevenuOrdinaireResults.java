package ch.vd.unireg.listes.afc;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.metier.assujettissement.PeriodeImposition;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionService;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.TiersService;

public class ExtractionDonneesRptRevenuOrdinaireResults extends ExtractionDonneesRptPeriodeImpositionResults {

	public ExtractionDonneesRptRevenuOrdinaireResults(RegDate dateTraitement, int periodeFiscale, int nbThreads, TiersService tiersService, ServiceInfrastructureService infraService,
	                                                  AssujettissementService assujettissementService, PeriodeImpositionService periodeImpositionService, AdresseService adresseService) {
		super(dateTraitement, periodeFiscale, nbThreads, tiersService, infraService, assujettissementService, periodeImpositionService, adresseService);
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
