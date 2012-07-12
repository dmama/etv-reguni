package ch.vd.uniregctb.evenement.regpp.view;

import java.io.Serializable;
import java.util.Date;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class EvenementCivilRegPPElementListeView implements Serializable {

	private static final long serialVersionUID = -3314742942975357599L;

	private Long id;
	private TypeEvenementCivil type;
	private EtatEvenementCivil etat = EtatEvenementCivil.A_TRAITER;
	private Date dateTraitement;
	private RegDate dateEvenement;
	private Long numeroIndividuPrincipal;
	private Long numeroIndividuConjoint;
	private Integer numeroOfsCommuneAnnonce;
	private Long numeroCTB;
	private String nom1;
	private String nom2;
	private String commentaireTraitement;

	public EvenementCivilRegPPElementListeView(EvenementCivilRegPP evt) {
		this.id = evt.getId();
		this.etat = evt.getEtat();
		this.numeroIndividuPrincipal = evt.getNumeroIndividuPrincipal();
		this.numeroIndividuConjoint = evt.getNumeroIndividuConjoint();
		this.type = evt.getType();
		this.numeroOfsCommuneAnnonce = evt.getNumeroOfsCommuneAnnonce();
		this.dateEvenement = evt.getDateEvenement();
		this.dateTraitement = evt.getDateTraitement();
		this.commentaireTraitement = evt.getCommentaireTraitement();
	}

	public Long getId() {
		return id;
	}

	public TypeEvenementCivil getType() {
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
	public Long getNumeroIndividuPrincipal() {
		return numeroIndividuPrincipal;
	}

	@SuppressWarnings("unused")
	public Long getNumeroIndividuConjoint() {
		return numeroIndividuConjoint;
	}

	@SuppressWarnings("unused")
	public Integer getNumeroOfsCommuneAnnonce() {
		return numeroOfsCommuneAnnonce;
	}

	@SuppressWarnings("unused")
	public Long getNumeroCTB() {
		return numeroCTB;
	}

	public void setNumeroCTB(Long numeroCTB) {
		this.numeroCTB = numeroCTB;
	}

	@SuppressWarnings("unused")
	public String getNom1() {
		return nom1;
	}

	public void setNom1(String nom1) {
		this.nom1 = nom1;
	}

	@SuppressWarnings("unused")
	public String getNom2() {
		return nom2;
	}

	public void setNom2(String nom2) {
		this.nom2 = nom2;
	}

	@SuppressWarnings("unused")
	public String getCommentaireTraitement() {
		return commentaireTraitement;
	}
}
