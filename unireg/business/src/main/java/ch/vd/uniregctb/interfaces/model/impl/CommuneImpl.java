package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.vd.uniregctb.interfaces.model.Commune;

public class CommuneImpl extends CommuneSimpleImpl implements Commune, Serializable {

	private static final long serialVersionUID = 8537916537832562224L;

	public static CommuneImpl get(ch.vd.infrastructure.model.Commune target) {
		if (target == null) {
			return null;
		}
		return new CommuneImpl(target);
	}

	private CommuneImpl(ch.vd.infrastructure.model.Commune target) {
		super(target);
	}
}
