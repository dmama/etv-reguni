package ch.vd.uniregctb.role;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TiersService;

public class RolePMCommunesResults extends RolePMResults<RolePMCommunesResults> {

	@Nullable
	public final Integer ofsCommune;

	/**
	 * La clé est le numéro OFS de la commune, la valeur est la liste des données de contribuable
	 * à présenter sur le rôle de la commune en question
	 */
	public final Map<Integer, List<RolePMData>> extraction = new HashMap<>();

	public RolePMCommunesResults(int annee, int nbThreads, @Nullable Integer ofsCommune, AdresseService adresseService, ServiceInfrastructureService infraService, TiersService tiersService, AssujettissementService assujettissementService) {
		super(annee, nbThreads, adresseService, infraService, tiersService, assujettissementService);
		this.ofsCommune = ofsCommune;
	}

	@Override
	public void addAll(RolePMCommunesResults right) {
		right.extraction.forEach((ofs, list) -> this.extraction.merge(ofs, list, (v1, v2) -> Stream.concat(v1.stream(), v2.stream()).collect(Collectors.toCollection(LinkedList::new))));
		super.addAll(right);
	}

	@Override
	public void end() {
		this.extraction.values().forEach(list -> list.sort(Comparator.comparingLong(data -> data.noContribuable)));
		super.end();
	}

	public void addToRole(Entreprise entreprise, int ofsCommune) throws CalculRoleException {
		final List<RolePMData> list = extraction.computeIfAbsent(ofsCommune, key -> new LinkedList<>());
		list.add(new RolePMData(entreprise, ofsCommune, annee, adresseService, infraService, tiersService, assujettissementService));
		addContribuableAuDecompte();
	}
}
