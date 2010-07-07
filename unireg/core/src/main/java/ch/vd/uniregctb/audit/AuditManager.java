package ch.vd.uniregctb.audit;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.common.AuthenticationHelper;

public class AuditManager implements InitializingBean, DisposableBean {

	private AuditLineDAO dao;
	private String appName;

	public void setAuditLineDAO(AuditLineDAO dao) {
		this.dao = dao;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public void afterPropertiesSet() throws Exception {
		Audit.setAuditLineDao(dao);

		try {
			AuthenticationHelper.pushPrincipal(AuthenticationHelper.SYSTEM_USER);
			Audit.info("Démarrage de l'application " + appName + ".");
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}
	public void destroy() throws Exception {
		try {
			AuthenticationHelper.pushPrincipal(AuthenticationHelper.SYSTEM_USER);
			Audit.info("Arrêt de l'application " + appName + ".");
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}

		Audit.setAuditLineDao(null);
	}

}
