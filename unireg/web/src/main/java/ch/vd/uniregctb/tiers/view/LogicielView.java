package ch.vd.uniregctb.tiers.view;

import ch.vd.uniregctb.interfaces.model.Logiciel;
import ch.vd.uniregctb.interfaces.model.LogicielMetier;

public class LogicielView {
	private String contactMetierMail;
	private String contactMetierNom;
	private String contactTechniqueMail;
	private String contactTechniqueNom;
	private String fournisseur;
	private String fournisseurAdresse;
	private long id;
	private String libelle;
	private String libelleComplet;
	private LogicielMetier metier;
	private String version;
	private boolean certifie;

	public LogicielView() {
	}

	public LogicielView(Logiciel logiciel) {
		this.certifie = logiciel.isCertifie();
		this.contactMetierMail = logiciel.getContactMetierMail();
		this.contactMetierNom = logiciel.getContactMetierNom();
		this.contactTechniqueMail = logiciel.getContactTechniqueMail();
		this.contactTechniqueNom = logiciel.getContactTechniqueNom();
		this.fournisseur = logiciel.getFournisseur();
		this.fournisseurAdresse = logiciel.getFournisseurAdresse();
		this.id = logiciel.getId();
		this.libelle = logiciel.getLibelle();
		this.metier = logiciel.getMetier();
		this.version = logiciel.getVersion();
		this.libelleComplet = logiciel.getLibelleComplet();
	}

	public String getContactMetierMail() {
		return contactMetierMail;
	}

	public void setContactMetierMail(String contactMetierMail) {
		this.contactMetierMail = contactMetierMail;
	}

	public String getContactMetierNom() {
		return contactMetierNom;
	}

	public void setContactMetierNom(String contactMetierNom) {
		this.contactMetierNom = contactMetierNom;
	}

	public String getContactTechniqueMail() {
		return contactTechniqueMail;
	}

	public void setContactTechniqueMail(String contactTechniqueMail) {
		this.contactTechniqueMail = contactTechniqueMail;
	}

	public String getContactTechniqueNom() {
		return contactTechniqueNom;
	}

	public void setContactTechniqueNom(String contactTechniqueNom) {
		this.contactTechniqueNom = contactTechniqueNom;
	}

	public String getFournisseur() {
		return fournisseur;
	}

	public void setFournisseur(String fournisseur) {
		this.fournisseur = fournisseur;
	}

	public String getFournisseurAdresse() {
		return fournisseurAdresse;
	}

	public void setFournisseurAdresse(String fournisseurAdresse) {
		this.fournisseurAdresse = fournisseurAdresse;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getLibelle() {
		return libelle;
	}

	public void setLibelle(String libelle) {
		this.libelle = libelle;
	}

	public LogicielMetier getMetier() {
		return metier;
	}

	public void setMetier(LogicielMetier metier) {
		this.metier = metier;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public boolean isCertifie() {
		return certifie;
	}

	public void setCertifie(boolean certifie) {
		this.certifie = certifie;
	}

	public String getLibelleComplet() {
		return libelleComplet;
	}

	public void setLibelleComplet(String libelleComplet) {
		this.libelleComplet = libelleComplet;
	}
}
