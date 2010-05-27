package ch.vd.uniregctb.evenement.cedi;

import java.util.Date;

import org.apache.commons.lang.StringUtils;

/**
 * Données utiles à Unireg extraites de l'événement JMS envoyé par le CEDI suite au retour (au scannage pour être précis) d'une DI. 
 */
public class RetourDI extends EvenementCedi {

	public enum TypeDocument {
		VAUDTAX,
		ORDINAIRE;

		public static TypeDocument fromJms(String code) {
			if (StringUtils.isBlank(code)) {
				return null;
			}

			if ("M".equals(code)) {
				return ORDINAIRE;
			}
			else if ("E".equals(code)) {
				return VAUDTAX;
			}

			throw new IllegalArgumentException("Type de document inconnu");
		}
	}

	private String businessId;
	private Date dateTraitement;

	private long noContribuable;
	private int periodeFiscale;
	private int noSequenceDI;
	private TypeDocument typeDocument;
	private String email;
	private String iban;
	private String noMobile;
	private String noTelephone;
	private String titulaireCompte;

	public String getBusinessId() {
		return businessId;
	}

	public void setBusinessId(String businessId) {
		this.businessId = businessId;
	}

	public Date getDateTraitement() {
		return dateTraitement;
	}

	public void setDateTraitement(Date dateTraitement) {
		this.dateTraitement = dateTraitement;
	}

	public long getNoContribuable() {
		return noContribuable;
	}

	public void setNoContribuable(long noContribuable) {
		this.noContribuable = noContribuable;
	}

	public int getPeriodeFiscale() {
		return periodeFiscale;
	}

	public void setPeriodeFiscale(int periodeFiscale) {
		this.periodeFiscale = periodeFiscale;
	}

	public int getNoSequenceDI() {
		return noSequenceDI;
	}

	public void setNoSequenceDI(int noSequenceDI) {
		this.noSequenceDI = noSequenceDI;
	}

	public TypeDocument getTypeDocument() {
		return typeDocument;
	}

	public void setTypeDocument(TypeDocument typeDocument) {
		this.typeDocument = typeDocument;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getIban() {
		return iban;
	}

	public void setIban(String iban) {
		this.iban = iban;
	}

	public String getNoMobile() {
		return noMobile;
	}

	public void setNoMobile(String noMobile) {
		this.noMobile = noMobile;
	}

	public String getNoTelephone() {
		return noTelephone;
	}

	public void setNoTelephone(String noTelephone) {
		this.noTelephone = noTelephone;
	}

	public String getTitulaireCompte() {
		return titulaireCompte;
	}

	public void setTitulaireCompte(String titulaireCompte) {
		this.titulaireCompte = titulaireCompte;
	}

	@Override
	public String toString() {
		return "RetourDI{" +
				"businessId='" + businessId + '\'' +
				", dateTraitement=" + dateTraitement +
				", noContribuable=" + noContribuable +
				", periodeFiscale=" + periodeFiscale +
				", noSequenceDI=" + noSequenceDI +
				", typeDocument=" + typeDocument +
				", email='" + email + '\'' +
				", iban='" + iban + '\'' +
				", noMobile='" + noMobile + '\'' +
				", noTelephone='" + noTelephone + '\'' +
				", titulaireCompte='" + titulaireCompte + '\'' +
				'}';
	}
}
