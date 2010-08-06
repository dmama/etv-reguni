package ch.vd.uniregctb.identification.contribuable.view;

import java.util.Date;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.Etat;

public class IdentificationMessagesResultView implements Annulable{

	private Long id;
	private String typeMessage;
	private Integer periodeFiscale;
	private String emetteurId;
	private String utilisateurTraitant;
	private Date dateMessage;
	private Etat etatMessage;
	private String nom;
	private String prenoms;
	private RegDate dateNaissance;
	private String navs13;
	private boolean annule;
	private long numeroContribuable;

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getTypeMessage() {
		return typeMessage;
	}
	public void setTypeMessage(String typeMessage) {
		this.typeMessage = typeMessage;
	}
	public Integer getPeriodeFiscale() {
		return periodeFiscale;
	}
	public void setPeriodeFiscale(Integer periodeFiscale) {
		this.periodeFiscale = periodeFiscale;
	}
	public String getEmetteurId() {
		return emetteurId;
	}
	public void setEmetteurId(String emetteurId) {
		this.emetteurId = emetteurId;
	}
	public Date getDateMessage() {
		return dateMessage;
	}
	public void setDateMessage(Date dateMessage) {
		this.dateMessage = dateMessage;
	}
	public Etat getEtatMessage() {
		return etatMessage;
	}
	public void setEtatMessage(Etat etatMessage) {
		this.etatMessage = etatMessage;
	}
	public String getNom() {
		return nom;
	}
	public void setNom(String nom) {
		this.nom = nom;
	}
	public String getPrenoms() {
		return prenoms;
	}
	public void setPrenoms(String prenoms) {
		this.prenoms = prenoms;
	}
	public RegDate getDateNaissance() {
		return dateNaissance;
	}
	public void setDateNaissance(RegDate dateNaissance) {
		this.dateNaissance = dateNaissance;
	}
	public String getNavs13() {
		return navs13;
	}
	public void setNavs13(String navs13) {
		this.navs13 = navs13;
	}
	public boolean isAnnule() {
		return annule;
	}
	public void setAnnule(boolean annule) {
		this.annule = annule;
	}
	public void setUtilisateurTraitant(String utilisateurTraitant) {
		this.utilisateurTraitant = utilisateurTraitant;
	}
	public String getUtilisateurTraitant() {
		return utilisateurTraitant;
	}

	public void setNumeroContribuable(long numeroContribuable) {
		this.numeroContribuable = numeroContribuable;
	}

	public long getNumeroContribuable() {
		return numeroContribuable;
	}


}
