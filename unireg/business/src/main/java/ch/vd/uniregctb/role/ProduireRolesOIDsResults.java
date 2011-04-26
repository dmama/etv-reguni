package ch.vd.uniregctb.role;

import ch.vd.registre.base.date.RegDate;

public class ProduireRolesOIDsResults extends ProduireRolesResults {

	/** renseigné en cas de sélection d'un office d'impôt */
	public final Integer noColOID;

	public ProduireRolesOIDsResults(int anneePeriode, int nbThreads, RegDate dateTraitement) {
		this(anneePeriode, null, nbThreads, dateTraitement);
	}

	public ProduireRolesOIDsResults(int anneePeriode, Integer noColOID, int nbThreads, RegDate dateTraitement) {
		super(anneePeriode, nbThreads, dateTraitement);
		this.noColOID = noColOID;
	}
}