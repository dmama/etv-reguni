package ch.vd.unireg.interfaces.infra.data;

public interface Logiciel {

	public String getContactMetierMail();

	public String getContactTechniqueMail();

	public String getFournisseur();

	public long getId();

	public String getLibelle();

	public LogicielMetier getMetier();

	public String getVersion();

	public String getLibelleComplet();


	public boolean isCertifie();
}
