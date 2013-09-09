package ch.vd.uniregctb.role;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.tiers.TiersService;

public class ProduireRolesCommunesResults extends ProduireRolesResults<ProduireRolesCommunesResults> {

	/** renseigné en cas de sélection d'une seule commune */
	public final Integer noOfsCommune;

	public ProduireRolesCommunesResults(int anneePeriode, int nbThreads, RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		this(anneePeriode, null, nbThreads, dateTraitement, tiersService, adresseService);
	}

	public ProduireRolesCommunesResults(int anneePeriode, Integer noOfsCommune, int nbThreads, RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		super(anneePeriode, nbThreads, dateTraitement, tiersService, adresseService);
		this.noOfsCommune = noOfsCommune;
	}
}
