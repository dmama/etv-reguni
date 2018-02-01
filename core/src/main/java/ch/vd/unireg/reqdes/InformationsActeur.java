package ch.vd.unireg.reqdes;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import ch.vd.unireg.common.LengthConstants;

@Embeddable
public class InformationsActeur {

	private String visa;
	private String nom;
	private String prenom;

	public InformationsActeur() {
	}

	public InformationsActeur(String visa, String nom, String prenom) {
		this.visa = visa;
		this.nom = nom;
		this.prenom = prenom;
	}

	@Column(name = "VISA", length = LengthConstants.HIBERNATE_LOGUSER, nullable = false)
	public String getVisa() {
		return visa;
	}

	public void setVisa(String visa) {
		this.visa = visa;
	}

	@Column(name = "NOM", length = LengthConstants.ADRESSE_NOM, nullable = false)
	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	@Column(name = "PRENOM", length = LengthConstants.ADRESSE_NOM, nullable = false)
	public String getPrenom() {
		return prenom;
	}

	public void setPrenom(String prenom) {
		this.prenom = prenom;
	}
}
