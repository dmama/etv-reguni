package ch.vd.uniregctb.migration.pm.utils;

import java.io.Serializable;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;

/**
 * Clé qui peut représenter une entreprise, un établissement ou un individu (l'identifiant présent est celui de RegPM)
 */
public final class EntityKey implements Serializable {

	private static final long serialVersionUID = -7195113401516584427L;

	private final long id;
	private final Type type;
	private EntityKey(long id, Type type) {
		this.id = id;
		this.type = type;
	}

	@NotNull
	public static EntityKey of(RegpmEntreprise e) {
		return new EntityKey(e.getId(), Type.ENTREPRISE);
	}

	@NotNull
	public static EntityKey of(RegpmEtablissement e) {
		return new EntityKey(e.getId(), Type.ETABLISSEMENT);
	}

	@NotNull
	public static EntityKey of(RegpmIndividu i) {
		return new EntityKey(i.getId(), Type.INDIVIDU);
	}

	public long getId() {
		return id;
	}

	public Type getType() {
		return type;
	}

	public enum Type {
		ENTREPRISE("Entreprise"),
		ETABLISSEMENT("Etablissement"),
		INDIVIDU("Individu");

		private final String displayName;

		Type(String displayName) {
			this.displayName = displayName;
		}

		public String getDisplayName() {
			return displayName;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final EntityKey entityKey = (EntityKey) o;
		return id == entityKey.id && type == entityKey.type;
	}

	@Override
	public int hashCode() {
		int result = (int) (id ^ (id >>> 32));
		result = 31 * result + type.hashCode();
		return result;
	}
}
