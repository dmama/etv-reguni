package ch.vd.unireg.evenement.regpp.view;

import java.io.Serializable;
import java.util.Date;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.TypeEvenementCivil;

public class EvenementCivilRegPPElementListeView implements Serializable {

	private static final long serialVersionUID = -3314742942975357599L;

	private final Long id;
	private final TypeEvenementCivil type;
	private final EtatEvenementCivil etat;
	private final Date dateTraitement;
	private final RegDate dateEvenement;
	private final Long numeroIndividuPrincipal;
	private final Long numeroIndividuConjoint;
	private final Integer numeroOfsCommuneAnnonce;
	private Long numeroCTB;
	private String nom1;
	private String nom2;
	private final String commentaireTraitement;

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
