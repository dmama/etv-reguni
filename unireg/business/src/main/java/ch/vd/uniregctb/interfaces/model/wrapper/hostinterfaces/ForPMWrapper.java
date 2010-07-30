package ch.vd.uniregctb.interfaces.model.wrapper.hostinterfaces;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.ForPM;
import ch.vd.uniregctb.interfaces.model.TypeNoOfs;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ForPMWrapper implements ForPM {

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final int noOfsAutoriteFiscale;
	private final TypeNoOfs typeAutoriteFiscale;

	public static ForPMWrapper get(ch.vd.registre.pm.model.ForPM target) {
		if (target == null) {
			return null;
		}
		return new ForPMWrapper(target);
	}

	public ForPMWrapper(ch.vd.registre.pm.model.ForPM target) {
		this.dateDebut = RegDate.get(target.getDateDebut());
		this.dateFin = RegDate.get(target.getDateFin());
		this.noOfsAutoriteFiscale = target.getNoOfsAutoriteFiscale();
		this.typeAutoriteFiscale = TypeNoOfs.valueOf(target.getTypeAutoriteFiscale().name());
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	public int getNoOfsAutoriteFiscale() {
		return noOfsAutoriteFiscale;
	}

	public TypeNoOfs getTypeAutoriteFiscale() {
		return typeAutoriteFiscale;
	}
}