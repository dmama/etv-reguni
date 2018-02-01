package ch.vd.unireg.role;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;

public enum TypePopulationRole {

	PP(PersonnePhysique.class, MenageCommun.class),
	PM(Entreprise.class);

	private final Set<Class<? extends Contribuable>> classes;

	@SafeVarargs
	TypePopulationRole(Class<? extends Contribuable>... classes) {
		this.classes = Collections.unmodifiableSet(Stream.of(classes).collect(Collectors.toSet()));
	}

	public Set<Class<? extends Contribuable>> getClasses() {
		return classes;
	}
}
