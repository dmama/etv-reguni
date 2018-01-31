package ch.vd.uniregctb.security;

import java.util.List;

import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrativeUtilisateur;


@SuppressWarnings({"UnusedDeclaration"})
public class ChooseOIDView {

	private List<CollectiviteAdministrativeUtilisateur> officesImpot;
	private String initialUrl;
	private Integer selectedOID;

	public List<CollectiviteAdministrativeUtilisateur> getOfficesImpot() {
		return officesImpot;
	}

	public void setOfficesImpot(List<CollectiviteAdministrativeUtilisateur> officesImpot) {
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
