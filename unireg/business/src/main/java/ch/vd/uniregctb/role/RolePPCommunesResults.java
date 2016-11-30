package ch.vd.uniregctb.role;

import java.util.Collections;
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
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.TiersService;

public class RolePPCommunesResults extends RolePPResults<RolePPCommunesResults> {

	@Nullable
	public final Integer ofsCommune;

	/**
	 * La clé est le numéro OFS de la commune, la valeur est la liste des données de contribuable
	 * à présenter sur le rôle de la commune en question
	 */
	public final Map<Integer, List<RolePPData>> extraction = new HashMap<>();

	public RolePPCommunesResults(int annee, int nbThreads, @Nullable Integer ofsCommune, AdresseService adresseService, ServiceInfrastructureService infraService, TiersService tiersService) {
		super(annee, nbThreads, adresseService, infraService, tiersService);
		this.ofsCommune = ofsCommune;
	}

	@Override
	public void addAll(RolePPCommunesResults right) {
		super.addAll(right);
		right.extraction.forEach((ofs, list) -> this.extraction.merge(ofs, list, (v1, v2) -> Stream.concat(v1.stream(), v2.stream()).collect(Collectors.toCollection(LinkedList::new))));
	}

	@Override
	public void end() {
		this.extraction.values().forEach(list -> Collections.sort(list,
		                                                          Comparator.comparingLong(data -> data.noContribuable)));
		super.end();
	}

	public void addToRole(ContribuableImpositionPersonnesPhysiques contribuable, int ofsCommune) {
		final List<RolePPData> list = extraction.computeIfAbsent(ofsCommune, key -> new LinkedList());
		list.add(new RolePPData(contribuable, ofsCommune, annee, adresseService, infraService, tiersService));
		addContribuableAuDecompte();
	}
}
