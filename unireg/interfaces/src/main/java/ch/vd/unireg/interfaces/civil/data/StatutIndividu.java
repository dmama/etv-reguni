package ch.vd.unireg.interfaces.civil.data;

import java.io.ObjectStreamException;
import java.io.Serializable;

public class StatutIndividu implements Serializable {

	private static final long serialVersionUID = -3659870087322989733L;

	private static final StatutIndividu ACTIVE = new StatutIndividu(true, null);
	private static final StatutIndividu INACTIVE = new StatutIndividu(false, null);

	private final boolean active;
	private final Long noIndividuRemplacant;

	private StatutIndividu(boolean active, Long noIndividuRemplacant) {
		this.active = active;
		this.noIndividuRemplacant = noIndividuRemplacant;
	}

	public static StatutIndividu active() {
		return ACTIVE;
	}

	public static StatutIndividu inactiveWithoutReplacement() {
		return INACTIVE;
	}

	public static StatutIndividu replaced(long noIndividuRemplacant) {
		return new StatutIndividu(false, noIndividuRemplacant);
	}

	public boolean isActive() {
		return active;
	}

	public Long getNoIndividuRemplacant() {
		return noIndividuRemplacant;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final StatutIndividu that = (StatutIndividu) o;

		if (active != that.active) return false;
		if (noIndividuRemplacant != null ? !noIndividuRemplacant.equals(that.noIndividuRemplacant) : that.noIndividuRemplacant != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = (active ? 1 : 0);
		result = 31 * result + (noIndividuRemplacant != null ? noIndividuRemplacant.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "StatutIndividu{" +
				"active=" + active +
				", noIndividuRemplacant=" + noIndividuRemplacant +
				'}';
	}

	/**
	 * Cette méthode permet de ne pas surcharger la mémoire avec des zillions d'instances de StatutIndividu tous identiques
	 * @return le résultat de la dé-sérialisation à propager plus loin
	 * @throws ObjectStreamException en cas de souci
	 * @see java.io.Serializable
	 */
	private Object readResolve() throws ObjectStreamException {
		if (noIndividuRemplacant == null) {
			return active ? ACTIVE : INACTIVE;
		}
		else {
			return this;
		}
	}
}
