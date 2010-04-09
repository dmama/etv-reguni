package ch.vd.uniregctb.interfaces.model.wrapper;

import ch.vd.uniregctb.interfaces.model.Commune;

public class CommuneWrapper extends CommuneSimpleWrapper implements Commune {

	public static CommuneWrapper get(ch.vd.infrastructure.model.Commune target) {
		if (target == null) {
			return null;
		}
		return new CommuneWrapper(target);
	}

	private CommuneWrapper(ch.vd.infrastructure.model.Commune target) {
		super(target);
	}
}
