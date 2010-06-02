package ch.vd.uniregctb.evenement.identification.contribuable;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;

/**
 * Contient le résultat du traitement d'une requête d'identification d'un contribuable.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@Embeddable
public class Reponse {

	private Date date;
	private Long noContribuable;
	private Long noMenageCommun;
	private Erreur erreur;
	private boolean enAttenteIdentifManuel=false;

	@Column(name = "DATE_REPONSE")
	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	@Column(name = "NO_CONTRIBUABLE")
	public Long getNoContribuable() {
		return noContribuable;
	}

	public void setNoContribuable(Long noContribuable) {
		this.noContribuable = noContribuable;
	}

	@Column(name = "NO_MENAGE_COMMUN")
	public Long getNoMenageCommun() {
		return noMenageCommun;
	}

	public void setNoMenageCommun(Long noMenageCommun) {
		this.noMenageCommun = noMenageCommun;
	}

	@Embedded
	public Erreur getErreur() {
		return erreur;
	}

	public void setErreur(Erreur erreur) {
		this.erreur = erreur;
	}

	@Column(name = "ATTENTE_IDENTIF_MANUEL")
	public boolean isEnAttenteIdentifManuel() {
		return enAttenteIdentifManuel;
	}


	public void setEnAttenteIdentifManuel(boolean enAttenteIdentifManuel) {
		this.enAttenteIdentifManuel = enAttenteIdentifManuel;
	}
}
