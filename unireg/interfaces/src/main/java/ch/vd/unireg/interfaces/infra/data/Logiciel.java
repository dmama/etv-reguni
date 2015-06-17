package ch.vd.unireg.interfaces.infra.data;

public interface Logiciel {

	String getContactMetierMail();

	String getContactTechniqueMail();

	String getFournisseur();

	long getId();

	String getLibelle();

	LogicielMetier getMetier();

	String getVersion();

	String getLibelleComplet();


	boolean isCertifie();
}
