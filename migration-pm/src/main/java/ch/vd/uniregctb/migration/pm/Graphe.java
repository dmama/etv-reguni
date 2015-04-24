package ch.vd.uniregctb.migration.pm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmImmeuble;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;
import ch.vd.uniregctb.migration.pm.regpm.WithLongId;

public class Graphe implements Serializable {

	private final Map<Long, RegpmEntreprise> entreprises = new TreeMap<>();
	private final Map<Long, RegpmEtablissement> etablissements = new TreeMap<>();
	private final Map<Long, RegpmIndividu> individus = new TreeMap<>();
	private final Map<Long, RegpmImmeuble> immeubles = new TreeMap<>();

	@Override
	public String toString() {
		final List<String> array = new ArrayList<>(4);
		if (!entreprises.isEmpty()) {
			array.add(String.format("%d entreprise(s) (%s)", entreprises.size(), Arrays.toString(entreprises.keySet().toArray(new Long[entreprises.size()]))));
		}
		if (!etablissements.isEmpty()) {
			array.add(String.format("%d établissement(s) (%s)", etablissements.size(), Arrays.toString(etablissements.keySet().toArray(new Long[etablissements.size()]))));
		}
		if (!individus.isEmpty()) {
			array.add(String.format("%d individu(s) (%s)", individus.size(), Arrays.toString(individus.keySet().toArray(new Long[individus.size()]))));
		}
		if (!immeubles.isEmpty()) {
			array.add(String.format("%d immeuble(s) (%s)", immeubles.size(), Arrays.toString(immeubles.keySet().toArray(new Long[immeubles.size()]))));
		}

		if (array.isEmpty()) {
			return "rien (???)";
		}
		else {
			return array.stream().collect(Collectors.joining(", "));
		}
	}

	public Map<Long, RegpmEntreprise> getEntreprises() {
		return Collections.unmodifiableMap(entreprises);
	}

	public Map<Long, RegpmEtablissement> getEtablissements() {
		return Collections.unmodifiableMap(etablissements);
	}

	public Map<Long, RegpmIndividu> getIndividus() {
		return Collections.unmodifiableMap(individus);
	}

	public Map<Long, RegpmImmeuble> getImmeubles() {
		return Collections.unmodifiableMap(immeubles);
	}

	/**
	 * @param entreprise entreprise à enregistrer
	 * @return <code>true</code> si l'entreprise a été enregistrée, <code>false</code> si elle l'était déjà
	 */
	public boolean register(RegpmEntreprise entreprise) {
		return register(entreprise, entreprises);
	}

	/**
	 * @param etablissement établissement à enregistrer
	 * @return <code>true</code> si l'établissement a été enregistré, <code>false</code> s'il l'était déjà
	 */
	public boolean register(RegpmEtablissement etablissement) {
		return register(etablissement, etablissements);
	}

	/**
	 * @param individu individu à enregistrer
	 * @return <code>true</code> si l'individu a été enregistré, <code>false</code> s'il l'était déjà
	 */
	public boolean register(RegpmIndividu individu) {
		return register(individu, individus);
	}

	/**
	 * @param immeuble immeuble à enregistrer
	 * @return <code>true</code> si l'immeuble a été enregistré, <code>false</code> s'il l'était déjà
	 */
	public boolean register(RegpmImmeuble immeuble) {
		return register(immeuble, immeubles);
	}

	/**
	 * @param entity une entité dont l'identifiant est un long
	 * @param entities la map des entités du même type déjà enregistrées
	 * @return <code>true</code> le l'entité à été enregistrée, <code>false</code> si elle l'était déjà
	 */
	private static <T extends WithLongId> boolean register(T entity, Map<Long, T> entities) {
		if (entity == null) {
			return false;
		}

		final Long id = entity.getId();
		if (entities.containsKey(id)) {
			return false;
		}

		// la prochaine fois, on saura que c'est fait...
		entities.put(id, entity);
		return true;
	}
}
