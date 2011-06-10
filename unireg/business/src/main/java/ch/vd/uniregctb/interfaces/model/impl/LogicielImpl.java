package ch.vd.uniregctb.interfaces.model.impl;

import ch.vd.uniregctb.interfaces.model.Logiciel;
import ch.vd.uniregctb.interfaces.model.LogicielMetier;

public class LogicielImpl implements Logiciel {

	private final String contactMetierMail;
	private final String contactMetierNom;
	private final String contactTechniqueMail;
	private final String contactTechniqueNom;
	private final String fournisseur;
	private final String fournisseurAdresse;
	private final long id;
	private final String libelle;
	private final LogicielMetier metier;
	private final String version;
	private final boolean certifie;

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

	@Override
	public String getContactMetierMail() {
		return contactMetierMail;
	}

	@Override
	public String getContactMetierNom() {
		return contactMetierNom;
	}

	@Override
	public String getContactTechniqueMail() {
		return contactTechniqueMail;
	}

	@Override
	public String getContactTechniqueNom() {
		return contactTechniqueNom;
	}

	@Override
	public String getFournisseur() {
		return fournisseur;
	}

	@Override
	public String getFournisseurAdresse() {
		return fournisseurAdresse;
	}

	@Override
	public long getId() {
		return id;
	}

	@Override
	public String getLibelle() {
		return libelle;
	}

	@Override
	public LogicielMetier getMetier() {
		return metier;
	}

	@Override
	public String getVersion() {
		return version;
	}


	@Override
	public String getLibelleComplet() {
		return fournisseur+" - "+libelle+" - "+version;
	}

	@Override
	public boolean isCertifie() {
		return certifie;
	}
}
