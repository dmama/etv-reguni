package ch.vd.uniregctb.evenement.organisation.view;

import java.io.Serializable;
import java.util.Date;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

public class EvenementOrganisationElementListeRechercheView implements Serializable {

	private static final long serialVersionUID = -5244992269330676463L;

	private Long id;
	private Long noEvenement;
	private TypeEvenementOrganisation type;
	private EtatEvenementOrganisation etat;
	private Date dateTraitement;
	private RegDate dateEvenement;
	private Long numeroOrganisation;
	private Long numeroCTB;
	private String nom;
	private String commentaireTraitement;
	private Integer noOFSSiege;
	private TypeAutoriteFiscale typeSiege;

	public EvenementOrganisationElementListeRechercheView(EvenementOrganisation evt) {
		this.id = evt.getId();
		this.noEvenement = evt.getNoEvenement();
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

	public void setId(Long id) {
		this.id = id;
	}

	public Long getNoEvenement() {
		return noEvenement;
	}

	public void setNoEvenement(Long noEvenement) {
		this.noEvenement = noEvenement;
	}

	public TypeEvenementOrganisation getType() {
		return type;
	}

	public void setType(TypeEvenementOrganisation type) {
		this.type = type;
	}

	public EtatEvenementOrganisation getEtat() {
		return etat;
	}

	public void setEtat(EtatEvenementOrganisation etat) {
		this.etat = etat;
	}

	public Date getDateTraitement() {
		return dateTraitement;
	}

	public void setDateTraitement(Date dateTraitement) {
		this.dateTraitement = dateTraitement;
	}

	public RegDate getDateEvenement() {
		return dateEvenement;
	}

	public void setDateEvenement(RegDate dateEvenement) {
		this.dateEvenement = dateEvenement;
	}

	public Long getNumeroOrganisation() {
		return numeroOrganisation;
	}

	public void setNumeroOrganisation(Long numeroOrganisation) {
		this.numeroOrganisation = numeroOrganisation;
	}

	public Long getNumeroCTB() {
		return numeroCTB;
	}

	public void setNumeroCTB(Long numeroCTB) {
		this.numeroCTB = numeroCTB;
	}

	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	public String getCommentaireTraitement() {
		return commentaireTraitement;
	}

	public void setCommentaireTraitement(String commentaireTraitement) {
		this.commentaireTraitement = commentaireTraitement;
	}

	public Integer getNoOFSSiege() {
		return noOFSSiege;
	}

	public void setNoOFSSiege(Integer noOFSSiege) {
		this.noOFSSiege = noOFSSiege;
	}

	public TypeAutoriteFiscale getTypeSiege() {
		return typeSiege;
	}

	public void setTypeSiege(TypeAutoriteFiscale typeSiege) {
		this.typeSiege = typeSiege;
	}
}
