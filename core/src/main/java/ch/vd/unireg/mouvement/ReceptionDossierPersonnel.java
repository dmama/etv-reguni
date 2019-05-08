package ch.vd.unireg.mouvement;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.unireg.type.Localisation;

@Entity
@DiscriminatorValue(value = "ReceptionPersonnel")
public class ReceptionDossierPersonnel extends ReceptionDossier {

	/**
	 * Visa du collaborateur récepteur.
	 */
	private String visaRecepteur;


	public ReceptionDossierPersonnel() {
	}

	public ReceptionDossierPersonnel(String visaRecepteur) {
		this.visaRecepteur = visaRecepteur;
	}

	@Column(name = "VISA_COLLABORATEUR", length = 25)
	public String getVisaRecepteur() {
		return visaRecepteur;
	}

	public void setVisaRecepteur(String visaRecepteur) {
		this.visaRecepteur = (visaRecepteur == null ? null : visaRecepteur.toLowerCase());  // le visa est toujours stocké en minuscules
	}

	@Transient
	@Override
	public Localisation getLocalisation() {
		return Localisation.PERSONNE;
	}

}