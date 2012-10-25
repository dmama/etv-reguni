package ch.vd.uniregctb.common;

import org.apache.commons.lang.StringUtils;

/**
 * Container pour les informations dissociées de rue et de numéro sur une adresse
 */
public class NpaEtLocalite {

	public static final NpaEtLocalite VIDE = new NpaEtLocalite(null, null);

	private final String npa;
	private final String localite;

	public NpaEtLocalite(String npa, String localite) {
		this.npa = StringUtils.trimToNull(npa);
		this.localite = StringUtils.trimToNull(localite);
	}

	public String getNpa() {
		return npa;
	}

	public String getLocalite() {
		return localite;
	}

	/**
	 * @return la concaténation du NPA et de la localité (dans cet ordre) avec un espace de séparation; ou <b>null</b> si les deux informations sont vides.
	 */
	@Override
	public String toString() {
		final String resultat;
		if (npa != null && localite != null) {
			resultat = String.format("%s %s", npa, localite);
		}
		else if (npa != null) {
			resultat = npa;
		}
		else if (localite != null) {
			resultat = localite;
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

		final NpaEtLocalite that = (NpaEtLocalite) o;

		if (localite != null ? !localite.equals(that.localite) : that.localite != null) return false;
		if (npa != null ? !npa.equals(that.npa) : that.npa != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = npa != null ? npa.hashCode() : 0;
		result = 31 * result + (localite != null ? localite.hashCode() : 0);
		return result;
	}
}
