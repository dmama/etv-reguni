package ch.vd.uniregctb.interfaces.model.impl;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Mandat;
import ch.vd.uniregctb.interfaces.model.TypeMandataire;

public class MandatImpl implements Mandat {

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final TypeMandataire typeMandataire;

	private final ch.vd.registre.pm.model.Mandat target;

	public static MandatImpl get(ch.vd.registre.pm.model.Mandat target) {
		if (target == null) {
			return null;
		}
		return new MandatImpl(target);
	}

	private MandatImpl(ch.vd.registre.pm.model.Mandat target) {
		this.target = target;
		this.dateDebut = RegDate.get(target.getDateDebut());
		this.dateFin = RegDate.get(target.getDateFin());
		this.typeMandataire = TypeMandataire.valueOf(target.getTypeMandataire().name());
	}

	@Override
	public String getCode() {
		return target.getCode();
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	@Override
	public String getPrenomContact() {
		return target.getPrenomContact();
	}

	@Override
	public String getNomContact() {
		return target.getNomContact();
	}

	@Override
	public String getNoTelephoneContact() {
		return target.getNoTelephoneContact();
	}

	@Override
	public String getNoFaxContact() {
		return target.getNoFaxContact();
	}

	@Override
	public String getCCP() {
		return target.getCCP();
	}

	@Override
	public String getCompteBancaire() {
		return target.getCompteBancaire();
	}

	@Override
	public String getIBAN() {
		return target.getIBAN();
	}

	@Override
	public String getBicSwift() {
		return target.getBicSwift();
	}

	@Override
	public Long getNumeroInstitutionFinanciere() {
		return target.getNumeroInstitutionFinanciere();
	}

	@Override
	public long getNumeroMandataire() {
		return target.getNumeroMandataire();
	}

	@Override
	public TypeMandataire getTypeMandataire() {
		return typeMandataire;
	}
}
