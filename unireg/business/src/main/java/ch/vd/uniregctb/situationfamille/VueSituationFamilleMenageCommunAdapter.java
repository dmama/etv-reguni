package ch.vd.uniregctb.situationfamille;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TarifImpotSource;

/**
 * Permet d'adapter une situation de famille de ménage commun en fonction de nouvelles dates de début/fin.
 */
public class VueSituationFamilleMenageCommunAdapter extends VueSituationFamilleAdapter<VueSituationFamilleMenageCommun> implements VueSituationFamilleMenageCommun {

	public VueSituationFamilleMenageCommunAdapter(VueSituationFamilleMenageCommun target, RegDate dateDebut, RegDate dateFin) {
		super(target, dateDebut, dateFin);
	}

	public Long getNumeroContribuablePrincipal() {
		return getTarget().getNumeroContribuablePrincipal();
	}

	public TarifImpotSource getTarifApplicable() {
		return getTarget().getTarifApplicable();
	}
}
