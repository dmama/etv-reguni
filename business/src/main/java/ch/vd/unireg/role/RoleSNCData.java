package ch.vd.unireg.role;

import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.TiersService;

public class RoleSNCData extends AbstractRolePMData {

	public RoleSNCData(Entreprise entreprise, int ofsCommune, int annee, AdresseService adresseService, ServiceInfrastructureService infrastructureService, TiersService tiersService, AssujettissementService assujettissementService) throws
			CalculRoleException {
		super(entreprise, ofsCommune, annee, adresseService, infrastructureService, tiersService, assujettissementService);
	}

	@Override
	protected boolean estSoumisImpot() {
		return Boolean.FALSE;
	}
}
