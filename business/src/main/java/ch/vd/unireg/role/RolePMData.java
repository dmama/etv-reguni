package ch.vd.unireg.role;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.TiersService;

public class RolePMData extends AbstractRolePMData {

	public RolePMData(Entreprise entreprise, int ofsCommune, int annee, AdresseService adresseService, ServiceInfrastructureService infrastructureService, TiersService tiersService, AssujettissementService assujettissementService) throws
			CalculRoleException {
		super(entreprise, ofsCommune, annee, adresseService, infrastructureService, tiersService, assujettissementService);
	}

	@Override
	protected boolean estSoumisImpot() {
		return Boolean.TRUE;
	}
}
