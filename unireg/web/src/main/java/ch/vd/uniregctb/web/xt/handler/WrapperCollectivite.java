package ch.vd.uniregctb.web.xt.handler;

import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import org.apache.commons.lang.StringEscapeUtils;

public class WrapperCollectivite {

	private final String noColAdm;
	private final String nomCourt;

	public WrapperCollectivite(CollectiviteAdministrative collectiviteAdministrative) {
		this.noColAdm = String.valueOf(collectiviteAdministrative.getNoColAdm());
		this.nomCourt = StringEscapeUtils.escapeXml(collectiviteAdministrative.getNomCourt());
	}

	public String getNoColAdm() {
		return noColAdm;
	}

	public String getNomCourt() {
		return nomCourt;
	}
}
