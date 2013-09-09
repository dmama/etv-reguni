package ch.vd.uniregctb.role;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.tiers.TiersService;

public class ProduireRolesOIDsResults extends ProduireRolesResults<ProduireRolesOIDsResults> {

	/** renseigné en cas de sélection d'un office d'impôt */
	public final Integer noColOID;

	public ProduireRolesOIDsResults(int anneePeriode, int nbThreads, RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		this(anneePeriode, null, nbThreads, dateTraitement, tiersService, adresseService);
	}

	public ProduireRolesOIDsResults(int anneePeriode, Integer noColOID, int nbThreads, RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		super(anneePeriode, nbThreads, dateTraitement, tiersService, adresseService);
		this.noColOID = noColOID;
	}
}