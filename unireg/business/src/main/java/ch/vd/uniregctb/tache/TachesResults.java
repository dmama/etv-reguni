package ch.vd.uniregctb.tache;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.JobResults;

public abstract class TachesResults extends JobResults {

	// Données de processing
	public int nbOIDTotal;

	public final RegDate dateTraitement;

	public boolean interrompu;

	public TachesResults(RegDate dateTraitement) {

		this.dateTraitement = dateTraitement;
	}

}
