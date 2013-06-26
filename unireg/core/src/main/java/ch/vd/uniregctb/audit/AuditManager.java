package ch.vd.uniregctb.audit;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.common.AuthenticationHelper;

public class AuditManager implements InitializingBean, DisposableBean {

	private AuditLineDAO dao;
	private String appName;

	/**
	 * Délai (en jours) au delà duquel les vieilles lignes d'audit doivent
	 * être effacées au démarrage de l'application (0 = pas de purge)
	 */
	private int delaiPurge = 0;

	public void setAuditLineDAO(AuditLineDAO dao) {
		this.dao = dao;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public void setDelaiPurge(int delaiPurge) {
		this.delaiPurge = delaiPurge;
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
