package ch.vd.uniregctb.common;

import java.util.Date;

/**
 * Vue pour la consultation des logs
 *
 * @author xcifde
 *
 */
public class ConsultLogView {

	private String utilisateurCreation;

	private Date dateHeureCreation;

	private String utilisateurDerniereModif;

	private Date dateHeureDerniereModif;

	private String utilisateurAnnulation;

	private Date dateHeureAnnulation;

	public String getUtilisateurCreation() {
		return utilisateurCreation;
	}

	public void setUtilisateurCreation(String utilisateurCreation) {
		this.utilisateurCreation = utilisateurCreation;
	}

	public Date getDateHeureCreation() {
		return dateHeureCreation;
	}

	public void setDateHeureCreation(Date dateHeureCreation) {
		this.dateHeureCreation = dateHeureCreation;
	}

	public String getUtilisateurDerniereModif() {
		return utilisateurDerniereModif;
	}

	public void setUtilisateurDerniereModif(String utilisateurDerniereModif) {
		this.utilisateurDerniereModif = utilisateurDerniereModif;
	}

	public Date getDateHeureDerniereModif() {
		return dateHeureDerniereModif;
	}

	public void setDateHeureDerniereModif(Date dateHeureDerniereModif) {
		this.dateHeureDerniereModif = dateHeureDerniereModif;
	}

	public String getUtilisateurAnnulation() {
		return utilisateurAnnulation;
	}

	public void setUtilisateurAnnulation(String utilisateurAnnulation) {
		this.utilisateurAnnulation = utilisateurAnnulation;
	}

	public Date getDateHeureAnnulation() {
		return dateHeureAnnulation;
	}

	public void setDateHeureAnnulation(Date dateHeureAnnulation) {
		this.dateHeureAnnulation = dateHeureAnnulation;
	}

}
