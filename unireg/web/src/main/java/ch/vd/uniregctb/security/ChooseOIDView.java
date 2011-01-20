package ch.vd.uniregctb.security;

import java.util.List;

import ch.vd.infrastructure.model.CollectiviteAdministrative;

public class ChooseOIDView {

	private List<CollectiviteAdministrative> officesImpot;

	public List<CollectiviteAdministrative> getOfficesImpot() {
		return officesImpot;
	}

	public void setOfficesImpot(List<CollectiviteAdministrative> officesImpot) {
		this.officesImpot = officesImpot;
	}
}
