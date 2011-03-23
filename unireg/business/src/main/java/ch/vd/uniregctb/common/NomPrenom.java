package ch.vd.uniregctb.common;

import org.apache.commons.lang.StringUtils;

/**
 * Container pour les informations dissociées de nom/prénom sur une personne physique
 */
public class NomPrenom {

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
			resultat = "";
		}
		return resultat;
	}
}
