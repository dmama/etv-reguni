package ch.vd.uniregctb.evenement.ech.view;

import java.io.Serializable;
import java.util.Date;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class EvenementCivilEchElementListeRechercheView implements Serializable {

	private static final long serialVersionUID = 3596946976206609859L;

	private Long id;
	private TypeEvenementCivilEch type;
	private EtatEvenementCivil etat;
	private ActionEvenementCivilEch action;
	private Date dateTraitement;
	private RegDate dateEvenement;
	private Long numeroIndividu;
	private Long numeroCTB;
	private String nom;
	private String commentaireTraitement;

	public EvenementCivilEchElementListeRechercheView(EvenementCivilEch evt) {
		this.id = evt.getId();
		this.etat = evt.getEtat();
		this.action = evt.getAction();
		this.numeroIndividu = evt.getNumeroIndividu();
		this.type = evt.getType();
		this.dateEvenement = evt.getDateEvenement();
		this.dateTraitement = evt.getDateTraitement();
		this.commentaireTraitement = evt.getCommentaireTraitement();
	}

	public Long getId() {
		return id;
	}

	public TypeEvenementCivilEch getType() {
		return type;
	}

	public EtatEvenementCivil getEtat() {
		return etat;
	}

	@SuppressWarnings("unused")
	public Date getDateTraitement() {
		return dateTraitement;
	}

	@SuppressWarnings("unused")
	public RegDate getDateEvenement() {
		return dateEvenement;
	}

	@SuppressWarnings("unused")
	public Long getNumeroIndividu() {
		return numeroIndividu;
	}

	@SuppressWarnings("unused")
	public Long getNumeroCTB() {
		return numeroCTB;
	}

	public void setNumeroCTB(Long numeroCTB) {
		this.numeroCTB = numeroCTB;
	}

	@SuppressWarnings("unused")
	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	@SuppressWarnings("unused")
	public String getCommentaireTraitement() {
		return commentaireTraitement;
	}

	@SuppressWarnings("unused")
	public ActionEvenementCivilEch getAction() {
		return action;
	}

}
