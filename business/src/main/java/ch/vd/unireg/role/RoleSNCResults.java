package ch.vd.unireg.role;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.TiersService;

public class RoleSNCResults extends RolePMResults<RoleSNCResults> {
	private final String SNC = "SNC";

	public final List<RoleSNCData> extraction = new LinkedList<>();

	public RoleSNCResults(int annee, int nbThreads, AdresseService adresseService, ServiceInfrastructureService infraService, TiersService tiersService, AssujettissementService assujettissementService) {
		super(annee, nbThreads, adresseService, infraService, tiersService, assujettissementService);

	}

	@Override
	public void addAll(RoleSNCResults right) {
		this.extraction.addAll(right.extraction);
		super.addAll(right);
	}

	@Override
	public void end() {
		this.extraction.sort(Comparator.comparingLong(data -> data.noContribuable));
		super.end();
	}

	@Override
	public String getTypePopulationRoleName() {
		return SNC;
	}

	public void addToRole(Entreprise entreprise, Integer ofsCommune) throws CalculRoleException {
		this.extraction.add(new RoleSNCData(entreprise, ofsCommune, annee, adresseService, infraService, tiersService, assujettissementService));
		addContribuableAuDecompte();
	}
}
