package ch.vd.unireg.audit;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.common.AuthenticationHelper;

public class AuditManager implements InitializingBean, DisposableBean {

	private AuditLineDAO dao;
	private String appName;

	public void setAuditLineDAO(AuditLineDAO dao) {
		this.dao = dao;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Audit.setAuditLineDao(dao);

		AuthenticationHelper.pushPrincipal(AuthenticationHelper.SYSTEM_USER);
		try {
			Audit.info(String.format("Démarrage de l'application %s.", appName));
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	@Override
	public void destroy() throws Exception {

		AuthenticationHelper.pushPrincipal(AuthenticationHelper.SYSTEM_USER);
		try {
			Audit.info(String.format("Arrêt de l'application %s.", appName));
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}

		Audit.setAuditLineDao(null);
	}

}
