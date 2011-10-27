package ch.vd.uniregctb.situationfamille;

import ch.vd.uniregctb.tiers.SituationFamilleMenageCommun;
import ch.vd.uniregctb.type.TarifImpotSource;

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
