package ch.vd.uniregctb.evenement.reqdes;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.infra.data.Pays;

/**
 * Nationalité courante donnée par les messages eReqDes
 */
public final class ReqDesNationalite implements Nationalite {

	private final Pays pays;

	public ReqDesNationalite(Pays pays) {
		this.pays = pays;
	}

	@Override
	public RegDate getDateDebut() {
		return null;
	}

	@Override
	public RegDate getDateFin() {
		return null;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return true;
	}

	@Override
	public Pays getPays() {
		return pays;
	}
}
