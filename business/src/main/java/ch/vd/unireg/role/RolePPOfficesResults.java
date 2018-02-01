package ch.vd.unireg.role;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.TiersService;

public class RolePPOfficesResults extends RolePPResults<RolePPOfficesResults> {

	@Nullable
	public final Integer oid;

	/**
	 * La clé est le numéro de collectivité administrative de l'OID, la valeur est la liste des données de contribuable
	 * à présenter sur le rôle de l'office en question
	 */
	public final SortedMap<Integer, List<RolePPData>> extraction = new TreeMap<>();

	public RolePPOfficesResults(int annee, int nbThreads, @Nullable Integer oid, AdresseService adresseService, ServiceInfrastructureService infraService, TiersService tiersService, AssujettissementService assujettissementService) {
		super(annee, nbThreads, adresseService, infraService, tiersService, assujettissementService);
		this.oid = oid;
	}

	@Override
	public void addAll(RolePPOfficesResults right) {
		super.addAll(right);
		right.extraction.forEach((oid, list) -> this.extraction.merge(oid, list, (v1, v2) -> Stream.concat(v1.stream(), v2.stream()).collect(Collectors.toCollection(LinkedList::new))));
	}

	@Override
	public void end() {
		this.extraction.values().forEach(list -> list.sort(Comparator.comparingLong(data -> data.noContribuable)));
		super.end();
	}

	public void addToRole(ContribuableImpositionPersonnesPhysiques contribuable, int ofsCommune, int oid) throws CalculRoleException {
		final List<RolePPData> list = extraction.computeIfAbsent(oid, key -> new LinkedList<>());
		list.add(new RolePPData(contribuable, ofsCommune, annee, adresseService, infraService, tiersService, assujettissementService));
		addContribuableAuDecompte();
	}
}
