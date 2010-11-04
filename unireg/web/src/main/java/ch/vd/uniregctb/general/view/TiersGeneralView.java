package ch.vd.uniregctb.general.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;

/**
 * TiersGeneralView
 *
 * @author xcifde
 *
 */
public class TiersGeneralView {

	public TiersGeneralView() {
	}

	public TiersGeneralView(Long numero) {
		this.numero = numero;
	}

	private RoleView role;

	private Long numero;

	private AdresseEnvoiDetaillee adresseEnvoi;

	private Exception adresseEnvoiException;

	private CategorieImpotSource categorie;

	private ModeCommunication modeCommunication;

	private PeriodiciteDecompte periodicite;

	private PeriodeDecompte periode;

	private String personneContact;

	private String numeroTelephone;

	private RegDate dateNaissance;

	private String numeroAssureSocial;

	private String ancienNumeroAVS;

	private String natureTiers;

	private String nomCommuneGestion;

	private boolean annule;

	public RoleView getRole() {
		return role;
	}

	public void setRole(RoleView role) {
		this.role = role;
	}

	public Long getNumero() {
		return numero;
	}

	public void setNumero(Long numero) {
		this.numero = numero;
	}

	public AdresseEnvoiDetaillee getAdresseEnvoi() {
		return adresseEnvoi;
	}

	public void setAdresseEnvoi(AdresseEnvoiDetaillee adresseEnvoi) {
		this.adresseEnvoi = adresseEnvoi;
	}

	public CategorieImpotSource getCategorie() {
		return categorie;
	}

	public void setCategorie(CategorieImpotSource categorie) {
		this.categorie = categorie;
	}

	public ModeCommunication getModeCommunication() {
		return modeCommunication;
	}

	public void setModeCommunication(ModeCommunication modeCommunication) {
		this.modeCommunication = modeCommunication;
	}

	public PeriodiciteDecompte getPeriodicite() {
		return periodicite;
	}

	public void setPeriodicite(PeriodiciteDecompte periodicite) {
		this.periodicite = periodicite;
	}

	public PeriodeDecompte getPeriode() {
		return periode;
	}

	public void setPeriode(PeriodeDecompte periode) {
		this.periode = periode;
	}

	public String getPersonneContact() {
		return personneContact;
	}

	public void setPersonneContact(String personneContact) {
		this.personneContact = personneContact;
	}

	public String getNumeroTelephone() {
		return numeroTelephone;
	}

	public void setNumeroTelephone(String numeroTelephone) {
		this.numeroTelephone = numeroTelephone;
	}

	public RegDate getDateNaissance() {
		return dateNaissance;
	}

	public void setDateNaissance(RegDate dateNaissance) {
		this.dateNaissance = dateNaissance;
	}

	public String getNumeroAssureSocial() {
		return numeroAssureSocial;
	}

	public void setNumeroAssureSocial(String numeroAssureSocial) {
		this.numeroAssureSocial = numeroAssureSocial;
	}

	public String getAncienNumeroAVS() {
		return ancienNumeroAVS;
	}

	public void setAncienNumeroAVS(String ancienNumeroAVS) {
		this.ancienNumeroAVS = ancienNumeroAVS;
	}

	public String getNatureTiers() {
		return natureTiers;
	}

	public void setNatureTiers(String natureTiers) {
		this.natureTiers = natureTiers;
	}

	public boolean isAnnule() {
		return annule;
	}

	public void setAnnule(boolean annule) {
		this.annule = annule;
	}

	public Exception getAdresseEnvoiException() {
		return adresseEnvoiException;
	}

	public void setAdresseEnvoiException(Exception adresseEnvoiException) {
		this.adresseEnvoiException = adresseEnvoiException;
	}

	public String getNomCommuneGestion() {
		return nomCommuneGestion;
	}

	public void setNomCommuneGestion(String nomCommuneGestion) {
		this.nomCommuneGestion = nomCommuneGestion;
	}
}
