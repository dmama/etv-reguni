package ch.vd.uniregctb.wsclient.model.impl;

import ch.vd.uniregctb.wsclient.model.Logiciel;
import ch.vd.uniregctb.wsclient.model.LogicielMetier;

public class LogicielImpl implements Logiciel {

	private String contactMetierMail;
	private String contactMetierNom;
	private String contactTechniqueMail;
	private String contactTechniqueNom;
	private String fournisseur;
	private String fournisseurAdresse;
	private long id;
	private String libelle;
	private LogicielMetier metier;
	private String version;
	private boolean certifie;

	public static LogicielImpl get(ch.vd.fidor.ws.v2.Logiciel target) {
		if (target == null) {
			return null;
		}
		return new LogicielImpl(target);
	}

	private LogicielImpl(ch.vd.fidor.ws.v2.Logiciel target) {
		this.contactMetierMail = target.getContactMetierMail();
		this.contactMetierNom = target.getContactMetierNom();
		this.contactTechniqueMail = target.getContactTechniqueMail();
		this.contactTechniqueNom = target.getContactTechniqueNom();
		this.fournisseur = target.getFournisseur();
		this.fournisseurAdresse = target.getFournisseurAdresse();
		this.id = target.getId();
		this.libelle = target.getLibelle();
		this.metier = LogicielMetier.get(target.getMetier());
		this.version = target.getVersion();
		this.certifie = target.isCertifie();

	}

	public String getContactMetierMail() {
		return contactMetierMail;
	}

	public String getContactMetierNom() {
		return contactMetierNom;
	}

	public String getContactTechniqueMail() {
		return contactTechniqueMail;
	}

	public String getContactTechniqueNom() {
		return contactTechniqueNom;
	}

	public String getFournisseur() {
		return fournisseur;
	}

	public String getFournisseurAdresse() {
		return fournisseurAdresse;
	}

	public long getId() {
		return id;
	}

	public String getLibelle() {
		return libelle;
	}

	public LogicielMetier getMetier() {
		return metier;
	}

	public String getVersion() {
		return version;
	}

	public boolean isCertifie() {
		return certifie;
	}
}
