package ch.vd.uniregctb.tache.sync;

import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;

/**
 * Classe de base des actions d'envoi de DI
 * @param <T> type concret de la periode d'imposition
 */
public abstract class AddDI<T extends PeriodeImposition> extends SynchronizeAction {

	public final T periodeImposition;

	public AddDI(T periodeImposition) {
		this.periodeImposition = periodeImposition;
	}

	@Override
	public boolean willChangeEntity() {
		return false;
	}
}
