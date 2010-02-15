package ch.vd.uniregctb.identification.contribuable.view;

import java.util.Date;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.Etat;
import ch.vd.uniregctb.type.Sexe;

public class DemandeIdentificationView {

	private Long id;
	private String typeMessage;
	private Integer periodeFiscale;
	private String emetteurId;
	private Date dateMessage;
	private Etat etatMessage;
	private String navs13;
	private String nom;
	private String prenoms;
	private RegDate dateNaissance;
	private Sexe sexe;

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
	public String getNavs13() {
		return navs13;
	}
	public void setNavs13(String navs13) {
		this.navs13 = navs13;
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
	public Sexe getSexe() {
		return sexe;
	}
	public void setSexe(Sexe sexe) {
		this.sexe = sexe;
	}

}
