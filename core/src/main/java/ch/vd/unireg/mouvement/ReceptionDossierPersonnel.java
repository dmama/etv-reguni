package ch.vd.unireg.mouvement;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.annotations.Index;

import ch.vd.unireg.type.Localisation;

@Entity
@DiscriminatorValue(value = "ReceptionPersonnel")
public class ReceptionDossierPersonnel extends ReceptionDossier {

	private long noIndividuRecepteur;

	/**
	 * Visa du collaborateur récepteur.
	 */
	private String visaRecepteur;


	public ReceptionDossierPersonnel() {
	}

	public ReceptionDossierPersonnel(String visaRecepteur) {
		this.visaRecepteur = visaRecepteur;
	}

	@Column(name = "NUMERO_INDIVIDU")
	public long getNoIndividuRecepteur() {
		return noIndividuRecepteur;
	}

	public void setNoIndividuRecepteur(long noIndividuRecepteur) {
		this.noIndividuRecepteur = noIndividuRecepteur;
	}

	@Column(name = "VISA_COLLABORATEUR", length = 25)
	@Index(name = "IDX_VISA_COLLABORATEUR")
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

	@Transient
	public long getNoIndividuDestinataire() {
		return getNoIndividuRecepteur();
	}
}