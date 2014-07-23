package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Mandat;
import ch.vd.uniregctb.interfaces.model.TypeMandataire;

@SuppressWarnings("UnusedDeclaration")
public class MockMandat implements Mandat {

	private String code;
	private RegDate dateDebut;
	private RegDate dateFin;
	private String prenomContact;
	private String nomContact;
	private String noTelephoneContact;
	private String noFaxContact;
	private String CCP;
	private String compteBancaire;
	private String IBAN;
	private String bicSwift;
	private Long numeroInstitutionFinanciere;
	private long numeroMandataire;
	private TypeMandataire typeMandataire;

	@Override
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	@Override
	public String getPrenomContact() {
		return prenomContact;
	}

	public void setPrenomContact(String prenomContact) {
		this.prenomContact = prenomContact;
	}

	@Override
	public String getNomContact() {
		return nomContact;
	}

	public void setNomContact(String nomContact) {
		this.nomContact = nomContact;
	}

	@Override
	public String getNoTelephoneContact() {
		return noTelephoneContact;
	}

	public void setNoTelephoneContact(String noTelephoneContact) {
		this.noTelephoneContact = noTelephoneContact;
	}

	@Override
	public String getNoFaxContact() {
		return noFaxContact;
	}

	public void setNoFaxContact(String noFaxContact) {
		this.noFaxContact = noFaxContact;
	}

	@Override
	public String getCCP() {
		return CCP;
	}

	public void setCCP(String CCP) {
		this.CCP = CCP;
	}

	@Override
	public String getCompteBancaire() {
		return compteBancaire;
	}

	public void setCompteBancaire(String compteBancaire) {
		this.compteBancaire = compteBancaire;
	}

	@Override
	public String getIBAN() {
		return IBAN;
	}

	public void setIBAN(String IBAN) {
		this.IBAN = IBAN;
	}

	@Override
	public String getBicSwift() {
		return bicSwift;
	}

	public void setBicSwift(String bicSwift) {
		this.bicSwift = bicSwift;
	}

	@Override
	public Long getNumeroInstitutionFinanciere() {
		return numeroInstitutionFinanciere;
	}

	public void setNumeroInstitutionFinanciere(Long numeroInstitutionFinanciere) {
		this.numeroInstitutionFinanciere = numeroInstitutionFinanciere;
	}

	@Override
	public long getNumeroMandataire() {
		return numeroMandataire;
	}

	public void setNumeroMandataire(long numeroMandataire) {
		this.numeroMandataire = numeroMandataire;
	}

	@Override
	public TypeMandataire getTypeMandataire() {
		return typeMandataire;
	}

	public void setTypeMandataire(TypeMandataire typeMandataire) {
		this.typeMandataire = typeMandataire;
	}
}
