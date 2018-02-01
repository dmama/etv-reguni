package ch.vd.unireg.situationfamille;

import ch.vd.unireg.tiers.SituationFamilleMenageCommun;
import ch.vd.unireg.type.TarifImpotSource;

/**
 * Permet d'adapter une situation de famille de ménage commun en provenance du fiscal.
 */
public class VueSituationFamilleMenageCommunFiscalAdapter extends VueSituationFamilleFiscalAdapter implements
		VueSituationFamilleMenageCommun {

	private final SituationFamilleMenageCommun situation;

	public VueSituationFamilleMenageCommunFiscalAdapter(SituationFamilleMenageCommun situation, Source source) {
		super(situation, source);
		this.situation = situation;
	}

	@Override
	public Long getNumeroContribuablePrincipal() {
		return situation.getContribuablePrincipalId();
	}

	@Override
	public TarifImpotSource getTarifApplicable() {
		return situation.getTarifApplicable();
	}
}
