package ch.vd.uniregctb.mouvement;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(value = "EnvoiVersCollaborateur")
public class EnvoiDossierVersCollaborateur extends EnvoiDossier {

	private long noIndividuDestinataire;

	public EnvoiDossierVersCollaborateur() {
	}

	public EnvoiDossierVersCollaborateur(long noIndividuDestinataire) {
		this.noIndividuDestinataire = noIndividuDestinataire;
	}

	@Column(name = "NUMERO_INDIVIDU")
	public long getNoIndividuDestinataire() {
		return noIndividuDestinataire;
	}

	public void setNoIndividuDestinataire(long noIndividuDestinataire) {
		this.noIndividuDestinataire = noIndividuDestinataire;
	}
}
