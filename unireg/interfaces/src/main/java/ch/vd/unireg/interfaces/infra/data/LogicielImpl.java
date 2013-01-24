package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;

public class LogicielImpl implements Logiciel, Serializable {

	private static final long serialVersionUID = 5078303547684479416L;

	private final String contactMetierMail;
	private final String contactTechniqueMail;
	private final String fournisseur;
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

	public static Logiciel get(ch.vd.evd0012.v1.Logiciel target) {
		if (target == null) {
			return null;
		}
		return new LogicielImpl(target);
	}

	private LogicielImpl(ch.vd.fidor.ws.v2.Logiciel target) {
		this.contactMetierMail = target.getContactMetierMail();
		this.contactTechniqueMail = target.getContactTechniqueMail();
		this.fournisseur = target.getFournisseur();
		this.id = target.getId();
		this.libelle = target.getLibelle();
		this.metier = LogicielMetier.get(target.getMetier());
		this.version = target.getVersion();
		this.certifie = target.isCertifie();
	}

	public LogicielImpl(ch.vd.evd0012.v1.Logiciel target) {
		this.contactMetierMail = target.getContactMetierEMail();
		this.contactTechniqueMail = target.getContactTechniqueEMail();
		this.fournisseur = target.getFournisseur();
		this.id = target.getIdLogiciel();
		this.libelle = target.getLibelle();
		this.metier = LogicielMetier.get(target.getMetier());
		this.version = target.getVersion();
		this.certifie = target.isIsCertifie();
	}

	@Override
	public String getContactMetierMail() {
		return contactMetierMail;
	}

	@Override
	public String getContactTechniqueMail() {
		return contactTechniqueMail;
	}

	@Override
	public String getFournisseur() {
		return fournisseur;
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
