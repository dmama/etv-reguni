package ch.vd.uniregctb.identification.contribuable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.evenement.identification.contribuable.TypeDemande;

public class IdentificationContribuableCache {

	private Map<IdentificationContribuableEtatFilter, Collection<String>> emetteursIds;
	private Map<IdentificationContribuableEtatFilter, Collection<Integer>> periodesFiscales;
	private Map<IdentificationContribuableEtatFilter, Collection<IdentificationContribuable.Etat>> etats;

	private Map<TypeDemande, Map<IdentificationContribuableEtatFilter, Collection<String>>> typesMessagesParTypeDemande = new EnumMap<TypeDemande, Map<IdentificationContribuableEtatFilter, Collection<String>>>(TypeDemande.class);

	private List<String> listTraitementUsers = new ArrayList<String>();

	public IdentificationContribuableCache() {
		emetteursIds = Collections.emptyMap();
		periodesFiscales = Collections.emptyMap();
		etats = Collections.emptyMap();
	}

	private static <T> Collection<T> findValues(Map<IdentificationContribuableEtatFilter, Collection<T>> all, IdentificationContribuableEtatFilter filter) {
		final Collection<T> col = all.get(filter);
		return col != null ? col : Collections.<T>emptyList();
	}

	private static <T> Map<IdentificationContribuableEtatFilter, Collection<T>> buildDataStructureFromMap(Map<IdentificationContribuable.Etat, List<T>> map) {
		final Map<IdentificationContribuableEtatFilter, Collection<T>> ds = new EnumMap<IdentificationContribuableEtatFilter, Collection<T>>(IdentificationContribuableEtatFilter.class);
		for (IdentificationContribuableEtatFilter filter : IdentificationContribuableEtatFilter.values()) {

			// récupération des valeurs associées à tous les états filtrés
			final Collection<T> collectionToFill = new LinkedList<T>();
			for (Map.Entry<IdentificationContribuable.Etat, List<T>> entry : map.entrySet()) {
				final IdentificationContribuable.Etat etat = entry.getKey();
				if (filter.isIncluded(etat)) {
					collectionToFill.addAll(entry.getValue());
				}
			}

			// suppression des doublons et assignation à la collection
			ds.put(filter, new HashSet<T>(collectionToFill));
		}

		return ds;
	}

	public void setEmetteursIds(Map<IdentificationContribuable.Etat, List<String>> emetteursIds) {
		this.emetteursIds = buildDataStructureFromMap(emetteursIds);
	}

	public Collection<String> getEmetteurIds(IdentificationContribuableEtatFilter filter) {
		return findValues(emetteursIds, filter);
	}

	public void setPeriodesFiscales(Map<IdentificationContribuable.Etat, List<Integer>> periodesFiscales) {
		this.periodesFiscales = buildDataStructureFromMap(periodesFiscales);
	}

	public Collection<Integer> getPeriodesFiscales(IdentificationContribuableEtatFilter filter) {
		return findValues(periodesFiscales, filter);
	}

	public void setTypesMessages(Map<TypeDemande, Map<IdentificationContribuable.Etat, List<String>>> typesMessages) {
		for (Map.Entry<TypeDemande, Map<IdentificationContribuable.Etat, List<String>>> entry : typesMessages.entrySet()) {
			this.typesMessagesParTypeDemande.put(entry.getKey(), buildDataStructureFromMap(entry.getValue()));
		}
	}

	public Collection<String> getTypesMessages(IdentificationContribuableEtatFilter filter) {
		return getTypesMessagesParTypeDemande(filter, TypeDemande.values());
	}

	public Collection<String> getTypesMessagesParTypeDemande(IdentificationContribuableEtatFilter filter, TypeDemande... typesDemande) {
		final Collection<String> res;
		if (typesDemande == null || typesDemande.length == 0) {
			res = Collections.emptyList();
		}
		else if (typesDemande.length == 1) {
			final Map<IdentificationContribuableEtatFilter, Collection<String>> typesMessages = typesMessagesParTypeDemande.get(typesDemande[0]);
			res = findValues(typesMessages, filter);
		}
		else {
			final List<String> accumulator = new LinkedList<String>();
			for (TypeDemande type : typesDemande) {
				final Map<IdentificationContribuableEtatFilter, Collection<String>> typesMessages = typesMessagesParTypeDemande.get(type);
				accumulator.addAll(findValues(typesMessages, filter));
			}
			res = new HashSet<String>(accumulator);
		}
		return res;
	}

	public List<String> getListTraitementUsers() {
		return listTraitementUsers;
	}

	public void setListTraitementUsers(List<String> listTraitementUsers) {
		this.listTraitementUsers = listTraitementUsers;
	}

	public void setEtats(Map<IdentificationContribuable.Etat, List<IdentificationContribuable.Etat>> map) {
		this.etats = buildDataStructureFromMap(map);
	}

	public Collection<IdentificationContribuable.Etat> getEtats(IdentificationContribuableEtatFilter filter) {
		return findValues(etats, filter);
	}
}