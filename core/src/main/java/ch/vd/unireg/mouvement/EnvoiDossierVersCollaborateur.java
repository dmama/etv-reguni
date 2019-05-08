package ch.vd.unireg.mouvement;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(value = "EnvoiVersCollaborateur")
public class EnvoiDossierVersCollaborateur extends EnvoiDossier {

	/**
	 * Visa du collaborateur destinataire.
	 */
	private String visaDestinataire;

	public EnvoiDossierVersCollaborateur() {
	}

	public EnvoiDossierVersCollaborateur(String visaDestinataire) {
		this.visaDestinataire = visaDestinataire;
	}

	@Column(name = "VISA_COLLABORATEUR", length = 25)
	public String getVisaDestinataire() {
		return visaDestinataire;
	}

	public void setVisaDestinataire(String visaDestinataire) {
		this.visaDestinataire = (visaDestinataire == null ? null : visaDestinataire.toLowerCase());  // le visa est toujours stocké en minuscules
	}
}
