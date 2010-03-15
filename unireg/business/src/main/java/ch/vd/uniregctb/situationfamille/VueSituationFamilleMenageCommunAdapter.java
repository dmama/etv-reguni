package ch.vd.uniregctb.situationfamille;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TarifImpotSource;

/**
 * Permet d'adapter une situation de famille de ménage commun en fonction de nouvelles dates de début/fin.
 */
public class VueSituationFamilleMenageCommunAdapter extends VueSituationFamilleAdapter implements VueSituationFamilleMenageCommun {

	private final VueSituationFamilleMenageCommun target;

	public VueSituationFamilleMenageCommunAdapter(VueSituationFamilleMenageCommun target, RegDate dateDebut, RegDate dateFin) {
		super(target, dateDebut, dateFin);
		this.target = target;
	}

	public Long getNumeroContribuablePrincipal() {
		return target.getNumeroContribuablePrincipal();
	}

	public TarifImpotSource getTarifApplicable() {
		return target.getTarifApplicable();
	}


}
