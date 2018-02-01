package ch.vd.uniregctb.evenement.reqdes.reception;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.infra.data.Pays;

/**
 * Nationalité courante donnée par les messages ReqDes
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
	public Pays getPays() {
		return pays;
	}
}
