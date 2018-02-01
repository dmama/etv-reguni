package ch.vd.unireg.role.before2016;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.tiers.TiersService;

public abstract class ProduireRolesCommunesResults<T extends ProduireRolesCommunesResults<T>> extends ProduireRolesResults<T> {

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
