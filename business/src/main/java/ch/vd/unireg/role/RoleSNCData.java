package ch.vd.unireg.role;

import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.TiersService;

import static ch.vd.unireg.role.RolePMData.buildFormeJuridique;
import static ch.vd.unireg.role.RolePMData.buildRaisonSociale;

public class RoleSNCData extends RoleData {

	public final String noIDE;
	public final String raisonSociale;
	public final FormeLegale formeJuridique;

	public RoleSNCData(Entreprise entreprise, int ofsCommune, int annee, AdresseService adresseService, ServiceInfrastructureService infrastructureService, TiersService tiersService, AssujettissementService assujettissementService) throws
			CalculRoleException {
		super(entreprise, ofsCommune, annee, adresseService, infrastructureService, assujettissementService);
		this.noIDE = tiersService.getNumeroIDE(entreprise);
		this.raisonSociale = buildRaisonSociale(entreprise, annee, tiersService);
		this.formeJuridique = buildFormeJuridique(entreprise, annee, tiersService);
	}

	@Override
	protected boolean estSoumisImpot() {
		return Boolean.FALSE;
	}
}
