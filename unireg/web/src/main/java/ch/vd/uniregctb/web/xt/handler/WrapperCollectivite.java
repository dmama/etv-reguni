package ch.vd.uniregctb.web.xt.handler;

import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;

public class WrapperCollectivite {

	private String noColAdm;
	private String nomCourt;

	public WrapperCollectivite(CollectiviteAdministrative collectiviteAdministrative) {
		this.noColAdm = String.valueOf(collectiviteAdministrative.getNoColAdm());
		this.nomCourt = collectiviteAdministrative.getNomCourt();
	}

	public String getNoColAdm() {
		return noColAdm;
	}

	public void setNoColAdm(String noColAdm) {
		this.noColAdm = noColAdm;
	}

	public String getNomCourt() {
		return nomCourt;
	}

	public void setNomCourt(String nomCourt) {
		this.nomCourt = nomCourt;
	}

}
