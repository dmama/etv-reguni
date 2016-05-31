package ch.vd.uniregctb.migration.pm.engine.data;

import java.util.Collection;
import java.util.Collections;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.migration.pm.regpm.RegpmMandat;

/**
 * Les données des mandats repris pour une entité
 */
public class DonneesMandats {

	/**
	 * Mandats de l'entité dans lesquels elle a un rôle de mandant
	 */
	@NotNull
	private final Collection<RegpmMandat> rolesMandant;

	/**
	 * Mandats de l'entité dans lesquels elle a un rôle de mandataire
	 */
	@NotNull
	private final Collection<RegpmMandat> rolesMandataire;

	public DonneesMandats(Collection<RegpmMandat> rolesMandant, Collection<RegpmMandat> rolesMandataire) {
		this.rolesMandant = rolesMandant == null ? Collections.emptyList() : rolesMandant;
		this.rolesMandataire = rolesMandataire == null ? Collections.emptyList() : rolesMandataire;
	}

	@NotNull
	public Collection<RegpmMandat> getRolesMandant() {
		return rolesMandant;
	}

	@NotNull
	public Collection<RegpmMandat> getRolesMandataire() {
		return rolesMandataire;
	}

	public boolean isMandataire() {
		return !rolesMandataire.isEmpty();
	}

	public boolean isMandant() {
		return !rolesMandant.isEmpty();
	}
}
