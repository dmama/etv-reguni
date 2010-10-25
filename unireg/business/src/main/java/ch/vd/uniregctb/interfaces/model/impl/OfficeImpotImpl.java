package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.vd.uniregctb.interfaces.model.OfficeImpot;

public class OfficeImpotImpl extends CollectiviteAdministrativeImpl implements OfficeImpot, Serializable {
	
	private static final long serialVersionUID = 264840177280219913L;

	protected OfficeImpotImpl(ch.vd.infrastructure.model.CollectiviteAdministrative target) {
		super(target);
	}
}
