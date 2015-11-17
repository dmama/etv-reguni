package ch.vd.uniregctb.evenement.organisation.view;

import java.io.Serializable;
import java.util.Date;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

public class EvenementOrganisationElementListeRechercheView implements Serializable {

	private static final long serialVersionUID = -5244992269330676463L;

	private Long id;
	private TypeEvenementOrganisation type;
	private EtatEvenementOrganisation etat;
	private Date dateTraitement;
	private RegDate dateEvenement;
	private Long numeroOrganisation;
	private Long numeroCTB;
	private String nom;
	private String commentaireTraitement;

	public EvenementOrganisationElementListeRechercheView(EvenementOrganisation evt) {
		this.id = evt.getId();
		this.etat = evt.getEtat();
		this.numeroOrganisation = evt.getNoOrganisation();
		this.type = evt.getType();
		this.dateEvenement = evt.getDateEvenement();
		this.dateTraitement = evt.getDateTraitement();
		this.commentaireTraitement = evt.getCommentaireTraitement();
	}

	public Long getId() {
		return id;
	}

	public TypeEvenementOrganisation getType() {
		return type;
	}

	public EtatEvenementOrganisation getEtat() {
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
	public Long getNumeroOrganisation() {
		return numeroOrganisation;
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

}
