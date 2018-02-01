package ch.vd.unireg.mandataire;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseMandataire;
import ch.vd.unireg.iban.IbanHelper;
import ch.vd.unireg.tiers.CoordonneesFinancieres;
import ch.vd.unireg.tiers.Mandat;
import ch.vd.unireg.type.TypeMandat;

public class EditMandatView implements DateRange {

	private Long idMandat;              // non-éditable
	private Long idAdresse;             // non-éditable
	private RegDate dateDebut;          // non-éditable mais utile pour la validation
	private RegDate dateFin;
	private TypeMandat typeMandat;      // non-éditable mais utile pour la validation
	private String personneContact;
	private String noTelContact;
	private boolean withCopy;
	private String iban;

	public EditMandatView() {
	}

	public EditMandatView(Mandat mandat) {
		this.idMandat = mandat.getId();
		this.idAdresse = null;
		this.typeMandat = mandat.getTypeMandat();
		this.dateDebut = mandat.getDateDebut();
		this.dateFin = mandat.getDateFin();
		this.withCopy = mandat.getWithCopy() != null && mandat.getWithCopy();

		final CoordonneesFinancieres cf = mandat.getCoordonneesFinancieres();
		if (cf != null) {
			this.iban = IbanHelper.normalize(cf.getIban());
		}
		else {
			this.iban = null;
		}

		this.personneContact = mandat.getPersonneContact();
		this.noTelContact = mandat.getNoTelephoneContact();
	}

	public EditMandatView(AdresseMandataire mandat) {
		this.idMandat = null;
		this.idAdresse = mandat.getId();
		this.typeMandat = mandat.getTypeMandat();
		this.dateDebut = mandat.getDateDebut();
		this.dateFin = mandat.getDateFin();
		this.withCopy = mandat.isWithCopy();
		this.iban = null;
		this.personneContact = mandat.getPersonneContact();
		this.noTelContact = mandat.getNoTelephoneContact();
	}

	public Long getIdMandat() {
		return idMandat;
	}

	public void setIdMandat(Long idMandat) {
		this.idMandat = idMandat;
	}

	public Long getIdAdresse() {
		return idAdresse;
	}

	public void setIdAdresse(Long idAdresse) {
		this.idAdresse = idAdresse;
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

	public TypeMandat getTypeMandat() {
		return typeMandat;
	}

	public void setTypeMandat(TypeMandat typeMandat) {
		this.typeMandat = typeMandat;
	}

	public String getPersonneContact() {
		return personneContact;
	}

	public void setPersonneContact(String personneContact) {
		this.personneContact = StringUtils.trimToNull(personneContact);
	}

	public String getNoTelContact() {
		return noTelContact;
	}

	public void setNoTelContact(String noTelContact) {
		this.noTelContact = StringUtils.trimToNull(noTelContact);
	}

	public boolean isWithCopy() {
		return withCopy;
	}

	public void setWithCopy(boolean withCopy) {
		this.withCopy = withCopy;
	}

	public String getIban() {
		return StringUtils.trimToNull(IbanHelper.toDisplayString(iban));
	}

	public void setIban(String iban) {
		this.iban = StringUtils.trimToNull(IbanHelper.normalize(iban));
	}
}
