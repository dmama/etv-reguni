package ch.vd.uniregctb.type;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;

/**
 * Définit les différents types de tiers auxquels une étiquette est associable
 */
public enum TypeTiersEtiquette {

	/**
	 * Individus personnes physiques seulement
	 */
	PP(PersonnePhysique.class),

	/**
	 * Contribuable PP (personnes physiques ou ménages communs)
	 */
	PP_MC(PersonnePhysique.class, MenageCommun.class),

	/**
	 * Entreprises seulement
	 */
	PM(Entreprise.class),

	/**
	 * Personnes physiques ou entreprises
	 */
	PP_PM(PersonnePhysique.class, Entreprise.class),

	/**
	 * Contribuables PP ou PM (personnes physiques, ménages communs ou entreprises)
	 */
	PP_MC_PM(PersonnePhysique.class, MenageCommun.class, Entreprise.class);

	private final Set<Class<? extends Tiers>> typesTiersAutorises;

	@SafeVarargs
	TypeTiersEtiquette(Class<? extends Tiers>... classes) {
		if (classes != null) {
			this.typesTiersAutorises = Arrays.stream(classes)
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());
		}
		else {
			this.typesTiersAutorises = Collections.emptySet();
		}
		if (this.typesTiersAutorises.isEmpty()) {
			throw new IllegalArgumentException("Au moins un type de tiers doit être autorisé...");
		}
	}

	/**
	 * @param clazz classe de tiers à tester
	 * @return <code>true</code> si la classe fait partie des classes autorisées dans ce type
	 */
	public boolean isForClass(Class<? extends Tiers> clazz) {
		return this.typesTiersAutorises.contains(clazz);
	}
}
