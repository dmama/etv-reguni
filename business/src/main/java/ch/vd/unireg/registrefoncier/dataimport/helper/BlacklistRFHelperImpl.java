package ch.vd.unireg.registrefoncier.dataimport.helper;

import java.util.concurrent.TimeUnit;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.unireg.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.unireg.registrefoncier.dao.BlacklistRFDAO;

public class BlacklistRFHelperImpl implements BlacklistRFHelper {

	private static final long CACHE_TTL = TimeUnit.MINUTES.toMillis(5); // cinq minutes en millisecondes

	private BlacklistRFDAO blacklistRFDAO;
	private PlatformTransactionManager transactionManager;

	/**
	 * Un cache qui contient tous les éléments blacklistés en mémoire (parce qu'il y en a une poignée et qu'on ne veut pas faire des centaines de milliers d'appels en DB pour ça).
	 */
	private BlacklistRF cache = null;
	private long cacheBuildTime = 0;

	@Override
	public synchronized boolean isBlacklisted(String idRF) {
		if (System.currentTimeMillis() > cacheBuildTime + CACHE_TTL) {
			refreshCache();
		}
		return cache.isBlacklisted(TypeEntiteRF.IMMEUBLE, idRF);
	}

	/**
	 * Recharge le cache avec les valeurs de la DB.
	 */
	private void refreshCache() {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		template.execute(status -> {
			cache = new BlacklistRF(blacklistRFDAO.getAll());
			cacheBuildTime = System.currentTimeMillis();
			return null;
		});
	}

	public void setBlacklistRFDAO(BlacklistRFDAO blacklistRFDAO) {
		this.blacklistRFDAO = blacklistRFDAO;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}
}
