package ch.vd.uniregctb.interfaces.model.wrapper;

import java.io.Serializable;

import ch.vd.uniregctb.interfaces.model.Commune;

public class CommuneWrapper extends CommuneSimpleWrapper implements Commune, Serializable {

	private static final long serialVersionUID = 8537916537832562224L;

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
