package ch.vd.uniregctb.mouvement;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.uniregctb.type.Localisation;

@Entity
@DiscriminatorValue(value = "ReceptionPersonnel")
public class ReceptionDossierPersonnel extends ReceptionDossier {

	private long noIndividuRecepteur;


	public ReceptionDossierPersonnel() {
	}

	public ReceptionDossierPersonnel(long noIndividuRecepteur) {
		this.noIndividuRecepteur = noIndividuRecepteur;
	}

	@Column(name = "NUMERO_INDIVIDU")
	public long getNoIndividuRecepteur() {
		return noIndividuRecepteur;
	}

	public void setNoIndividuRecepteur(long noIndividuRecepteur) {
		this.noIndividuRecepteur = noIndividuRecepteur;
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