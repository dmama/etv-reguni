package ch.vd.uniregctb.interfaces.model;

public interface Logiciel {

	public String getContactMetierMail();

	public String getContactMetierNom();

	public String getContactTechniqueMail();

	public String getContactTechniqueNom();

	public String getFournisseur();

	public String getFournisseurAdresse();

	public long getId();

	public String getLibelle();

	public LogicielMetier getMetier();

	public String getVersion();

	public String getLibelleComplet();


	public boolean isCertifie();
}
