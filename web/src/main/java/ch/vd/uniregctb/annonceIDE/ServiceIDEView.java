package ch.vd.uniregctb.annonceIDE;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.NumeroIDE;

/**
 * Vue web des informations concernant l'application utilisée par l'annonceur d'une annonce à l'IDE.
 */
public class ServiceIDEView {

	private NumeroIDE noIdeServiceIDE;
	private String applicationId;
	private String applicationName;

	public ServiceIDEView() {
	}

	public ServiceIDEView(NumeroIDE noIdeServiceIDE, String applicationId, String applicationName) {
		this.noIdeServiceIDE = noIdeServiceIDE;
		this.applicationId = applicationId;
		this.applicationName = applicationName;
	}

	public ServiceIDEView(@NotNull BaseAnnonceIDE.InfoServiceIDEObligEtendues serviceIDE) {
		this.noIdeServiceIDE = serviceIDE.getNoIdeServiceIDEObligEtendues();
		this.applicationId = serviceIDE.getApplicationId();
		this.applicationName = serviceIDE.getApplicationName();
	}

	public NumeroIDE getNoIdeServiceIDE() {
		return noIdeServiceIDE;
	}

	public void setNoIdeServiceIDE(NumeroIDE noIdeServiceIDE) {
		this.noIdeServiceIDE = noIdeServiceIDE;
	}

	public String getApplicationId() {
		return applicationId;
	}

	public void setApplicationId(String applicationId) {
		this.applicationId = applicationId;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}
}
