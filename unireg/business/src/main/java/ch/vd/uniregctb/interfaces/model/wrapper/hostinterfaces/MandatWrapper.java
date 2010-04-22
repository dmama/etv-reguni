package ch.vd.uniregctb.interfaces.model.wrapper.hostinterfaces;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Mandat;
import ch.vd.uniregctb.interfaces.model.TypeMandataire;

public class MandatWrapper implements Mandat {

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final TypeMandataire typeMandataire;

	private final ch.vd.registre.pm.model.Mandat target;

	public static MandatWrapper get(ch.vd.registre.pm.model.Mandat target) {
		if (target == null) {
			return null;
		}
		return new MandatWrapper(target);
	}

	private MandatWrapper(ch.vd.registre.pm.model.Mandat target) {
		this.target = target;
		this.dateDebut = RegDate.get(target.getDateDebut());
		this.dateFin = RegDate.get(target.getDateFin());
		this.typeMandataire = TypeMandataire.valueOf(target.getTypeMandataire().name());
	}

	public String getCode() {
		return target.getCode();
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	public String getPrenomContact() {
		return target.getPrenomContact();
	}

	public String getNomContact() {
		return target.getNomContact();
	}

	public String getNoTelephoneContact() {
		return target.getNoTelephoneContact();
	}

	public String getNoFaxContact() {
		return target.getNoFaxContact();
	}

	public String getCCP() {
		return target.getCCP();
	}

	public String getCompteBancaire() {
		return target.getCompteBancaire();
	}

	public String getIBAN() {
		return target.getIBAN();
	}

	public String getBicSwift() {
		return target.getBicSwift();
	}

	public Long getNumeroInstitutionFinanciere() {
		return target.getNumeroInstitutionFinanciere();
	}

	public long getNumeroMandataire() {
		return target.getNumeroMandataire();
	}

	public TypeMandataire getTypeMandataire() {
		return typeMandataire;
	}
}
