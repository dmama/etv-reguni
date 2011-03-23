package ch.vd.uniregctb.identification.contribuable.view;

import java.util.Date;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.Etat;
import ch.vd.uniregctb.type.Sexe;

public class DemandeIdentificationView implements Annulable {

	private Long id;
	private String typeMessage;
	private Integer periodeFiscale;
	private String emetteurId;
	private String businessId;
	private Date dateMessage;
	private Etat etatMessage;
	private String navs13;
	private String navs11;
	private String nom;
	private String prenoms;
	private RegDate dateNaissance;
	private Sexe sexe;
	private String rue;
	private Integer npa;
	private String lieu;
	private String pays;
	private String npaEtranger;
	private String noPolice;
	private boolean annule;
	private boolean viewable;

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
	public void setRue(String rue) {
		this.rue = rue;
	}
	public String getRue() {
		return rue;
	}
	public void setNpa(Integer npa) {
		this.npa = npa;
	}
	public Integer getNpa() {
		return npa;
	}
	public void setLieu(String lieu) {
		this.lieu = lieu;
	}
	public String getLieu() {
		return lieu;
	}
	public void setPays(String pays) {
		this.pays = pays;
	}
	public String getPays() {
		return pays;
	}
	public void setNpaEtranger(String npaEtranger) {
		this.npaEtranger = npaEtranger;
	}
	public String getNpaEtranger() {
		return npaEtranger;
	}
	public void setNoPolice(String noPolice) {
		this.noPolice = noPolice;
	}
	public String getNoPolice() {
		return noPolice;
	}

	public String getBusinessId() {
		return businessId;
	}

	public void setBusinessId(String businessId) {
		this.businessId = businessId;
	}

	public boolean isAnnule() {
		return annule;
	}

	public void setAnnule(boolean annule) {
		this.annule = annule;
	}

	public String getNavs11() {
		return navs11;
	}

	public void setNavs11(String navs11) {
		this.navs11 = navs11;
	}

	public boolean isViewable() {
		return viewable;
	}

	public void setViewable(boolean viewable) {
		this.viewable = viewable;
	}
}
