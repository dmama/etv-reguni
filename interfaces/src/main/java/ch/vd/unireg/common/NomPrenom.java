package ch.vd.unireg.common;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

/**
 * Container pour les informations dissociées de nom/prénom sur une personne physique
 */
public class NomPrenom implements Serializable {

	private static final long serialVersionUID = 7618933901404175521L;

	public static final NomPrenom VIDE = new NomPrenom(null, null);

	private final String nom;
	private final String prenom;

	public NomPrenom(String nom, String prenom) {
		this.nom = StringUtils.trimToNull(nom);
		this.prenom = StringUtils.trimToNull(prenom);
	}

	public String getNom() {
		return nom;
	}

	public String getPrenom() {
		return prenom;
	}

	/**
	 * @return la concaténation du prénom et du nom (dans cet ordre) avec un espace de séparation
	 */
	public String getNomPrenom() {
		final String resultat;
		if (nom != null && prenom != null) {
			resultat = String.format("%s %s", prenom, nom);
		}
		else if (nom != null) {
			resultat = nom;
		}
		else if (prenom != null) {
			resultat = prenom;
		}
		else {
			resultat = StringUtils.EMPTY;
		}
		return resultat;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final NomPrenom nomPrenom = (NomPrenom) o;

		if (nom != null ? !nom.equals(nomPrenom.nom) : nomPrenom.nom != null) return false;
		if (prenom != null ? !prenom.equals(nomPrenom.prenom) : nomPrenom.prenom != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = nom != null ? nom.hashCode() : 0;
		result = 31 * result + (prenom != null ? prenom.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "NomPrenom{" +
				"nom='" + nom + '\'' +
				", prenom='" + prenom + '\'' +
				'}';
	}

	/**
	 * @param tousPrenoms une chaîne de caractères contenant tous les prénoms d'une personne physique, séparés par des 'blancs'
	 * @return le premier de ces prénoms, considéré par défaut comme étant le prénom usuel
	 */
	@Nullable
	public static String extractPrenomUsuel(String tousPrenoms) {
		if (StringUtils.isBlank(tousPrenoms)) {
			return null;
		}
		final String[] parts = tousPrenoms.trim().split("\\s");
		return parts[0];
	}
}
