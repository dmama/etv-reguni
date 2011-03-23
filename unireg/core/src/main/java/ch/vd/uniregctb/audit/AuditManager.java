package ch.vd.uniregctb.audit;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
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

	public void afterPropertiesSet() throws Exception {
		Audit.setAuditLineDao(dao);

		AuthenticationHelper.pushPrincipal(AuthenticationHelper.SYSTEM_USER);
		try {
			Audit.info(String.format("Démarrage de l'application %s.", appName));

			// purge des anciennes lignes de l'audit
			if (delaiPurge > 0) {
				final RegDate seuilPurge = RegDate.get().addDays(- delaiPurge);
				Audit.info(String.format("Purge des lignes d'audit plus vieilles que %d jour(s) - %s.", delaiPurge, RegDateHelper.dateToDisplayString(seuilPurge)));
				final int nbLignesPurgees = dao.purge(seuilPurge);
				Audit.info(String.format("Purge de l'audit terminée : %d ligne(s) effacée(s)", nbLignesPurgees));
			}
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

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
