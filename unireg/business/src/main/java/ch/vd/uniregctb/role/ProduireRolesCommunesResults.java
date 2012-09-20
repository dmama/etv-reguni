package ch.vd.uniregctb.role;

import ch.vd.registre.base.date.RegDate;

public class ProduireRolesCommunesResults extends ProduireRolesResults {

	/** renseigné en cas de sélection d'une seule commune */
	public final Integer noOfsCommune;

	public ProduireRolesCommunesResults(int anneePeriode, int nbThreads, RegDate dateTraitement) {
		this(anneePeriode, null, nbThreads, dateTraitement);
	}

	public ProduireRolesCommunesResults(int anneePeriode, Integer noOfsCommune, int nbThreads, RegDate dateTraitement) {
		super(anneePeriode, nbThreads, dateTraitement);
		this.noOfsCommune = noOfsCommune;
	}
}
