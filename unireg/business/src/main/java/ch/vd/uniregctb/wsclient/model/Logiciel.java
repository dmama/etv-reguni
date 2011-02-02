package ch.vd.uniregctb.wsclient.model;

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


	public boolean isCertifie();
}
