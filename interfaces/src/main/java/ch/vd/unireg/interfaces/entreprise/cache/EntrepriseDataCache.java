package ch.vd.unireg.interfaces.entreprise.cache;

import java.io.Serializable;

import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;

public final class EntrepriseDataCache implements Serializable {

	private static final long serialVersionUID = -1423842735882864058L;

	private final EntrepriseCivile entrepriseCivile;

	public EntrepriseDataCache(EntrepriseCivile entrepriseCivile) {
		this.entrepriseCivile = entrepriseCivile;
	}

	public EntrepriseCivile getEntrepriseCivile() {
		return entrepriseCivile;
	}
}
