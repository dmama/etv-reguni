package ch.vd.unireg.evenement.reqdes.reception;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.type.TypePermis;

/**
 * Permis de séjour courant donné par les messages ReqDes
 */
public final class ReqDesPermis implements Permis {

	private final TypePermis type;

	public ReqDesPermis(TypePermis type) {
		this.type = type;
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
	public RegDate getDateAnnulation() {
		return null;
	}

	@Override
	public RegDate getDateValeur() {
		return null;
	}

	@Override
	public TypePermis getTypePermis() {
		return type;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return true;
	}
}
