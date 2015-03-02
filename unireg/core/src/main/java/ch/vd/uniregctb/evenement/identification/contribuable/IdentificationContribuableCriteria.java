package ch.vd.uniregctb.evenement.identification.contribuable;

import java.util.Date;

import ch.vd.registre.base.date.RegDate;

public class IdentificationContribuableCriteria {

	private String typeMessage;

	private Integer periodeFiscale;

	private String emetteurId;

	private Demande.PrioriteEmetteur prioriteEmetteur;

	private Date dateMessageDebut;

	private Date dateMessageFin;

	private Date dateTraitementDebut;

	private Date dateTraitementFin;

	private IdentificationContribuable.Etat etatMessage;

	private String nom;

	private String prenoms;

	private String NAVS13;

	private String NAVS11;

	private RegDate dateNaissance;

	private String traitementUser;

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

	public Demande.PrioriteEmetteur getPrioriteEmetteur() {
		return prioriteEmetteur;
	}

	public void setPrioriteEmetteur(Demande.PrioriteEmetteur prioriteEmetteur) {
		this.prioriteEmetteur = prioriteEmetteur;
	}

	public Date getDateMessageDebut() {
		return dateMessageDebut;
	}

	public void setDateMessageDebut(Date dateMessageDebut) {
		this.dateMessageDebut = dateMessageDebut;
	}

	public Date getDateMessageFin() {
		return dateMessageFin;
	}

	public void setDateMessageFin(Date dateMessageFin) {
		this.dateMessageFin = dateMessageFin;
	}

	public IdentificationContribuable.Etat getEtatMessage() {
		return etatMessage;
	}

	public void setEtatMessage(IdentificationContribuable.Etat etatMessage) {
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

	public String getNAVS13() {
		return NAVS13;
	}

	public void setNAVS13(String navs13) {
		NAVS13 = navs13;
	}

	public RegDate getDateNaissance() {
		return dateNaissance;
	}

	public void setDateNaissance(RegDate dateNaissance) {
		this.dateNaissance = dateNaissance;
	}

	public String getNAVS11() {
		return NAVS11;
	}

	public void setNAVS11(String NAVS11) {
		this.NAVS11 = NAVS11;
	}

	public String getTraitementUser() {
		return traitementUser;
	}

	public void setTraitementUser(String traitementUser) {
		this.traitementUser = traitementUser;
	}

	public Date getDateTraitementDebut() {
		return dateTraitementDebut;
	}

	public void setDateTraitementDebut(Date dateTraitementDebut) {
		this.dateTraitementDebut = dateTraitementDebut;
	}

	public Date getDateTraitementFin() {
		return dateTraitementFin;
	}

	public void setDateTraitementFin(Date dateTraitementFin) {
		this.dateTraitementFin = dateTraitementFin;
	}
	public boolean isEmpty(){
		return dateMessageDebut==null &&
				dateMessageFin==null &&
				dateNaissance == null &&
				dateTraitementDebut == null &&
				dateTraitementFin == null &&
				emetteurId == null &&
				NAVS11 == null &&
				etatMessage ==null &&
				NAVS13 == null &&
				nom == null &&
				periodeFiscale == null &&
				prenoms == null &&
				etatMessage == null &&
				prioriteEmetteur == null&&
				typeMessage == null &&
				traitementUser == null;

	}
}
