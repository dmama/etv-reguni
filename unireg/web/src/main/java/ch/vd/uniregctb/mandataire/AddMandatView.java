package ch.vd.uniregctb.mandataire;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.iban.IbanHelper;
import ch.vd.uniregctb.tiers.view.AdresseView;
import ch.vd.uniregctb.type.TypeMandat;

public class AddMandatView implements DateRange {

	private long idMandant;
	private RegDate dateDebut;
	private RegDate dateFin;
	private TypeMandat typeMandat;
	private boolean withCopy = true;
	private String prenomPersonneContact;
	private String nomPersonneContact;
	private String noTelContact;
	private String codeGenreImpot;
	private String iban;

	private String raisonSociale;
	private final AdresseView adresse = new AdresseView();

	private Long idTiersMandataire;

	public AddMandatView() {
	}

	public AddMandatView(long idMandant) {
		this.idMandant = idMandant;
	}

	public AddMandatView(long idMandant, long idTiersMandataire) {
		this.idMandant = idMandant;
		this.idTiersMandataire = idTiersMandataire;
	}

	public long getIdMandant() {
		return idMandant;
	}

	public void setIdMandant(long idMandant) {
		this.idMandant = idMandant;
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
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	public TypeMandat getTypeMandat() {
		return typeMandat;
	}

	public void setTypeMandat(TypeMandat typeMandat) {
		this.typeMandat = typeMandat;
	}

	public boolean isWithCopy() {
		return withCopy;
	}

	public void setWithCopy(boolean withCopy) {
		this.withCopy = withCopy;
	}

	public String getPrenomPersonneContact() {
		return prenomPersonneContact;
	}

	public void setPrenomPersonneContact(String prenomPersonneContact) {
		this.prenomPersonneContact = prenomPersonneContact;
	}

	public String getNomPersonneContact() {
		return nomPersonneContact;
	}

	public void setNomPersonneContact(String nomPersonneContact) {
		this.nomPersonneContact = nomPersonneContact;
	}

	public String getNoTelContact() {
		return noTelContact;
	}

	public void setNoTelContact(String noTelContact) {
		this.noTelContact = noTelContact;
	}

	public String getCodeGenreImpot() {
		return codeGenreImpot;
	}

	public void setCodeGenreImpot(String codeGenreImpot) {
		this.codeGenreImpot = codeGenreImpot;
	}

	public String getIban() {
		return IbanHelper.toDisplayString(iban);
	}

	public void setIban(String iban) {
		this.iban = IbanHelper.normalize(iban);
	}

	public String getRaisonSociale() {
		return raisonSociale;
	}

	public void setRaisonSociale(String raisonSociale) {
		this.raisonSociale = raisonSociale;
	}

	public AdresseView getAdresse() {
		return adresse;
	}

	public Long getIdTiersMandataire() {
		return idTiersMandataire;
	}

	public void setIdTiersMandataire(Long idTiersMandataire) {
		this.idTiersMandataire = idTiersMandataire;
	}
}
