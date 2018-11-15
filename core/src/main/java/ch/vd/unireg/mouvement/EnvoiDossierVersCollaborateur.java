package ch.vd.unireg.mouvement;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.annotations.Index;

@Entity
@DiscriminatorValue(value = "EnvoiVersCollaborateur")
public class EnvoiDossierVersCollaborateur extends EnvoiDossier {

	private long noIndividuDestinataire;

	/**
	 * Visa du collaborateur destinataire.
	 */
	private String visaDestinataire;

	public EnvoiDossierVersCollaborateur() {
	}

	public EnvoiDossierVersCollaborateur(String visaDestinataire) {
		this.visaDestinataire = visaDestinataire;
	}

	@Column(name = "NUMERO_INDIVIDU")
	public long getNoIndividuDestinataire() {
		return noIndividuDestinataire;
	}

	public void setNoIndividuDestinataire(long noIndividuDestinataire) {
		this.noIndividuDestinataire = noIndividuDestinataire;
	}

	@Column(name = "VISA_COLLABORATEUR", length = 25)
	@Index(name = "IDX_VISA_COLLABORATEUR")
	public String getVisaDestinataire() {
		return visaDestinataire;
	}

	public void setVisaDestinataire(String visaDestinataire) {
		this.visaDestinataire = (visaDestinataire == null ? null : visaDestinataire.toLowerCase());  // le visa est toujours stocké en minuscules
	}
}
