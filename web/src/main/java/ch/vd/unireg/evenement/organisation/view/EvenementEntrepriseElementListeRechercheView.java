package ch.vd.unireg.evenement.organisation.view;

import java.io.Serializable;
import java.util.Date;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.organisation.EntrepriseView;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.TypeEvenementEntreprise;

public class EvenementEntrepriseElementListeRechercheView implements Serializable {

	private static final long serialVersionUID = -5244992269330676463L;

	private Long id;
	private Long noEvenement;
	private TypeEvenementEntreprise type;
	private EtatEvenementEntreprise etat;
	private Date dateTraitement;
	private RegDate dateEvenement;
	private Long numeroOrganisation;
	private Long numeroCTB;
	private String nom;
	private String commentaireTraitement;
	private long annonceIDEId;
	private boolean correctionDansLePasse;

	/* Ajouté de la vue de détail */
	private boolean recyclable;
	private boolean forcable;
	private EntrepriseView organisation;

	public EvenementEntrepriseElementListeRechercheView(EvenementEntreprise evt) {
		this.id = evt.getId();
		this.noEvenement = evt.getNoEvenement();
		this.etat = evt.getEtat();
		this.numeroOrganisation = evt.getNoEntrepriseCivile();
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

	public TypeEvenementEntreprise getType() {
		return type;
	}

	public void setType(TypeEvenementEntreprise type) {
		this.type = type;
	}

	public EtatEvenementEntreprise getEtat() {
		return etat;
	}

	public void setEtat(EtatEvenementEntreprise etat) {
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

	public boolean isRecyclable() {
		return recyclable;
	}

	public void setRecyclable(boolean recyclable) {
		this.recyclable = recyclable;
	}

	public boolean isForcable() {
		return forcable;
	}

	public void setForcable(boolean forcable) {
		this.forcable = forcable;
	}

	public EntrepriseView getOrganisation() {
		return organisation;
	}

	public void setOrganisation(EntrepriseView organisation) {
		this.organisation = organisation;
	}

	public long getAnnonceIDEId() {
		return annonceIDEId;
	}

	public void setAnnonceIDEId(long annonceIDEId) {
		this.annonceIDEId = annonceIDEId;
	}

	public boolean isCorrectionDansLePasse() {
		return correctionDansLePasse;
	}

	public void setCorrectionDansLePasse(boolean correctionDansLePasse) {
		this.correctionDansLePasse = correctionDansLePasse;
	}
}
