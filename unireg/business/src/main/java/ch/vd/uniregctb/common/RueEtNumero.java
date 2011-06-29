package ch.vd.uniregctb.common;

import org.apache.commons.lang.StringUtils;

/**
 * Container pour les informations dissociées de rue et de numéro sur une adresse
 */
public class RueEtNumero {

	public static final RueEtNumero VIDE = new RueEtNumero(null, null);

	private final String rue;
	private final String numero;

	public RueEtNumero(String rue, String numero) {
		this.rue = StringUtils.trimToNull(rue);
		this.numero = StringUtils.trimToNull(numero);
	}

	public String getRue() {
		return rue;
	}

	public String getNumero() {
		return numero;
	}

	/**
	 * @return la concaténation de la rue et du numéro (dans cet ordre) avec un espace de séparation
	 */
	public String getRueEtNumero() {
		final String resultat;
		if (rue != null && numero != null) {
			resultat = String.format("%s %s", rue, numero);
		}
		else if (rue != null) {
			resultat = rue;
		}
		else {
			resultat = "";
		}
		return resultat;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final RueEtNumero that = (RueEtNumero) o;

		if (numero != null ? !numero.equals(that.numero) : that.numero != null) return false;
		if (rue != null ? !rue.equals(that.rue) : that.rue != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = rue != null ? rue.hashCode() : 0;
		result = 31 * result + (numero != null ? numero.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "RueEtNumero{" +
				"rue='" + rue + '\'' +
				", numero='" + numero + '\'' +
				'}';
	}
}
