package ch.vd.uniregctb.indexer.tiers;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.hibernate.CallbackException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.type.Type;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.hibernate.interceptor.HibernateFakeInterceptor;
import ch.vd.uniregctb.hibernate.interceptor.ModificationInterceptor;
import ch.vd.uniregctb.hibernate.interceptor.ModificationSubInterceptor;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;

public class TiersIndexerHibernateInterceptor implements ModificationSubInterceptor, InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(TiersIndexerHibernateInterceptor.class);

	private ModificationInterceptor parent;
	private SessionFactory sessionFactory;
	private GlobalTiersIndexer indexer;

	private final ThreadLocal<HashSet<Long>> dirtyEntities = new ThreadLocal<HashSet<Long>>();

	/**
	 * Cette méthode est appelé lorsque une entité hibernate est modifié/sauvé.
	 */
	public boolean onChange(HibernateEntity entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames,
			Type[] types) throws CallbackException {

		boolean modified = false;

		if (indexer.isOnTheFlyIndexation()) {
			if (entity instanceof Tiers) {
				Tiers tiers = (Tiers) entity;
				addDirtyEntity(tiers);
			}
			else if (entity instanceof ForFiscal) {
				ForFiscal ff = (ForFiscal) entity;
				Tiers tiers = ff.getTiers();
				addDirtyEntity(tiers);
			}
			else if (entity instanceof AdresseTiers) {
				AdresseTiers adr = (AdresseTiers) entity;
				Tiers tiers = adr.getTiers();
				addDirtyEntity(tiers);
			}
			else if (entity instanceof RapportEntreTiers) {
				RapportEntreTiers ret = (RapportEntreTiers) entity;
				Tiers tiers1 = ret.getObjet();
				addDirtyEntity(tiers1);
				Tiers tiers2 = ret.getSujet();
				addDirtyEntity(tiers2);
			}
		}
		else {
			if (entity instanceof Tiers) {
				Tiers tiers = (Tiers) entity;
				setTiersDirty(tiers, currentState, propertyNames);
				modified = true;
			}
			/*
			 * msi: ici on devrait aussi flagger les tiers comme dirty, mais ce n'est pas possible car on reçoit le currentState sur les
			 * fors fiscaux, adresses, etc. On peut bien essayer de mettre le flag 'dirty' sur le tiers lui-même, mais il ne sera pas
			 * répercuté en base ! Pour l'instant, je ne vois pas de solution...
			 */
			// else if (entity instanceof ForFiscal) {
			// ...
			// }
			// else if (entity instanceof AdresseTiers) {
			// ...
			// }
			// else if (entity instanceof RapportEntreTiers) {
			// ...
			// }
		}

		return modified;
	}

	/**
	 * Défini le tiers spécifié (ainsi que son état 'hibernate') comme dirty.
	 */
	private void setTiersDirty(Tiers tiers, Object[] currentState, String[] propertyNames) {
		tiers.setIndexDirty(Boolean.TRUE);
		final int length = propertyNames.length;
		for (int i = 0; i < length; ++i) {
			String n = propertyNames[i];
			if ("indexDirty".equals(n)) {
				currentState[i] = Boolean.TRUE;
				break;
			}
		}
	}

	/**
	 * Ajoute le tiers spécifié dans les liste des tiers qui seront indéxés après le flush.
	 */
	private void addDirtyEntity(Tiers tiers) {
		Assert.isTrue((indexer.isOnTheFlyIndexation()));
		if (tiers != null) {
			getDirtyEntities().add(tiers.getNumero());
		}
	}

	private HashSet<Long> getDirtyEntities() {
		HashSet<Long> ent = dirtyEntities.get();
		if (ent == null) {
			ent = new HashSet<Long>();
			dirtyEntities.set(ent);
		}
		return ent;
	}

	/**
	 * Lorsque les tiers ont été mis-à-jour dans la base, on les recharge pour les indexer.
	 * <p>
	 * On est obligé de les recharger dans une nouvelle session parce que le 'postFlush' de l'intercepteur Hibernate travaille hors-session
	 * (ou elle n'est plus valable) et que l'on risque de tomber sur des collections lazy non-initialisées si on ne le fait pas.
	 */
	public void postFlush() throws CallbackException {

		final HashSet<Long> entities = getDirtyEntities();
		if (entities.isEmpty()) {
			return;
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Réindexation on-the-fly des tiers = " + Arrays.toString(entities.toArray()));
		}

		Session session = sessionFactory.openSession(new HibernateFakeInterceptor());
		try {
			for (Long id : entities) {
				final Tiers tiers = (Tiers) session.get(Tiers.class, id);
				if (tiers == null) {
					// Le tiers peut être null si il y a eu un rollback, et donc pas de flush
					// Mais on n'est pas notifié en JTA...
					continue;
				}
				try {
					indexer.indexTiers(tiers, true/* Remove before reindexation */);
					if (tiers.isDirty()) {
						tiers.setIndexDirty(Boolean.FALSE); // il est plus dirty maintenant
					}
				}
				catch (Exception ee) {
					if (indexer.isThrowOnTheFlyException()) {
						throw new IndexerException(tiers, ee);
					}
					else {
						// Pour pas qu'un autre thread le reindex aussi
						tiers.setIndexDirty(true);

						final String message = "Reindexation du contribuable " + tiers.getId() + " impossible : " + ee.getMessage();
						Audit.error(message);
						LOGGER.error(message, ee);
					}
				}
			}
			session.flush();
		}
		finally {
			entities.clear();
			session.close();
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Réindexation on-the-fly des tiers terminée.");
		}
	}

	public void setIndexer(GlobalTiersIndexer indexer) {
		this.indexer = indexer;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void setParent(ModificationInterceptor parent) {
		this.parent = parent;
	}

	public void afterPropertiesSet() throws Exception {
		parent.register(this);
	}

}
