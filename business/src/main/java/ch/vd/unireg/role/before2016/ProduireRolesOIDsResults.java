package ch.vd.uniregctb.role.before2016;

import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.TiersService;

public class ProduireRolesOIDsResults extends ProduireRolesResults<ProduireRolesOIDsResults> {

	/** renseigné en cas de sélection d'un office d'impôt */
	public final Integer noColOID;

	private final RolesPP roles = new RolesPP();

	public ProduireRolesOIDsResults(int anneePeriode, int nbThreads, RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		this(anneePeriode, null, nbThreads, dateTraitement, tiersService, adresseService);
	}

	public ProduireRolesOIDsResults(int anneePeriode, Integer noColOID, int nbThreads, RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		super(anneePeriode, nbThreads, dateTraitement, tiersService, adresseService);
		this.noColOID = noColOID;
	}

	@Override
	public TypeRoles getTypeRoles() {
		return TypeRoles.PP;
	}

	@Override
	public void digestInfoFor(InfoFor infoFor, Contribuable ctb, Assujettissement assujettissement, RegDate dateFinAssujettissementPrecedent, int annee, int noOfsCommune, AdresseService adresseService, TiersService tiersService) {
		roles.digestInfoFor(infoFor, (ContribuableImpositionPersonnesPhysiques) ctb, assujettissement, dateFinAssujettissementPrecedent, annee, noOfsCommune, adresseService, tiersService);
	}

	@Override
	public List<DateRange> getPeriodesFiscales(Contribuable ctb, TiersService tiersService) {
		return roles.getPeriodesFiscales((ContribuableImpositionPersonnesPhysiques) ctb, tiersService);
	}

	@Override
	public void addAll(ProduireRolesOIDsResults rapport) {
		super.addAll(rapport);
		roles.addAll(rapport.roles);
	}

	public List<InfoContribuablePP> buildInfoPourRegroupementCommunes(List<Integer> noOfsCommunes) {
		return roles.buildInfosPourRegroupementCommunes(noOfsCommunes);
	}

	@Override
	public Set<Integer> getNoOfsCommunesTraitees() {
		return roles.getNoOfsCommunesTraitees();
	}
}