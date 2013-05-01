package ch.vd.uniregctb.interfaces.model.impl;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Siege;
import ch.vd.uniregctb.interfaces.model.TypeNoOfs;
import ch.vd.uniregctb.interfaces.model.helper.EntrepriseHelper;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class SiegeImpl implements Siege {

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final int noOfsSiege;
	private final TypeNoOfs type;

	public static SiegeImpl get(ch.vd.registre.pm.model.Siege target) {
		if (target == null) {
			return null;
		}
		return new SiegeImpl(target);
	}

	public SiegeImpl(ch.vd.registre.pm.model.Siege target) {
		this.dateDebut = EntrepriseHelper.get(target.getDateDebut());
		this.dateFin = EntrepriseHelper.get(target.getDateFin());
		this.noOfsSiege = target.getNoOfsSiege();
		this.type = TypeNoOfs.valueOf(target.getType().name());
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
	public int getNoOfsSiege() {
		return noOfsSiege;
	}

	@Override
	public TypeNoOfs getType() {
		return type;
	}
}
