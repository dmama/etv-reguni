package ch.vd.uniregctb.security;

import java.util.List;

import ch.vd.infrastructure.model.CollectiviteAdministrative;

@SuppressWarnings({"UnusedDeclaration"})
public class ChooseOIDView {

	private List<CollectiviteAdministrative> officesImpot;
	private String initialUrl;
	private Integer selectedOID;

	public List<CollectiviteAdministrative> getOfficesImpot() {
		return officesImpot;
	}

	public void setOfficesImpot(List<CollectiviteAdministrative> officesImpot) {
		this.officesImpot = officesImpot;
	}

	public String getInitialUrl() {
		return initialUrl;
	}

	public void setInitialUrl(String initialUrl) {
		this.initialUrl = initialUrl;
	}

	public Integer getSelectedOID() {
		return selectedOID;
	}

	public void setSelectedOID(Integer selectedOID) {
		this.selectedOID = selectedOID;
	}
}
