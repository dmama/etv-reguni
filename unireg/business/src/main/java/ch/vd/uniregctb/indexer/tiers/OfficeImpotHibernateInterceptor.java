package ch.vd.uniregctb.indexer.tiers;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.CallbackException;
import org.hibernate.Transaction;
import org.hibernate.type.Type;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.Switchable;
import ch.vd.uniregctb.common.ThreadSwitch;
import ch.vd.uniregctb.hibernate.interceptor.AbstractLinkedInterceptor;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Cet intercepteur se charge de tenir à-jour l'id de l'office d'impôt caché au niveau de chaque tiers.
 */
public class OfficeImpotHibernateInterceptor extends AbstractLinkedInterceptor implements Switchable {

	//private static final Logger LOGGER = Logger.getLogger(OfficeImpotHibernateInterceptor.class);

	private TiersService tiersService;
	private TacheDAO tacheDAO;
	private TiersDAO tiersDAO;

	private final ThreadSwitch enabled = new ThreadSwitch(true);

	private final ThreadLocal<HashMap<Long, Tiers>> dirtyEntities = new ThreadLocal<HashMap<Long, Tiers>>() {
		@Override
		protected HashMap<Long, Tiers> initialValue() {
			return new HashMap<>();
		}
	};

	/**
	 * Les tiers (avec leurs nouveaux OIDs) qui doivent être mis-à-jour
	 */
	private final ThreadLocal<HashMap<Long, Integer>> toUpdateOids = new ThreadLocal<HashMap<Long, Integer>>() {
		@Override
		protected HashMap<Long, Integer> initialValue() {
			return new HashMap<>();
		}
	};

	/**
	 * Une map numéro de tiers -> nouveau numéro d'oid des tiers qui ont été changés.
	 */
	private final ThreadLocal<HashMap<Long, Integer>> updatedOids = new ThreadLocal<HashMap<Long, Integer>>() {
		@Override
		protected HashMap<Long, Integer> initialValue() {
			return new HashMap<>();
		}
	};

	@Override
	public void preFlush(Iterator<?> entities) throws CallbackException {

		if (!isEnabled()) {
			return;
		}

		/*
		 * Note: on ne peut pas utiliser l'iterateur 'entities' parce qu'il est possible que le preFlush soit appelé alors qu'on est déjà
		 * entrain d'itérer sur la même collection d'entités. Dans ce cas-là, l'itérateur lève une ConcurrentModificationException car la
		 * collection sur laquelle il est basé à été modifiée dans son dos. Donc, on stocke directement toutes les entités dirty et c'est
		 * celle-là qu'on processe.
		 */

		final HashMap<Long, Tiers> dirty = getDirtyEntities();
		final HashMap<Long, Integer> modifs = getUpdatedOids();
		try {
			// On change la valeur de l'oid sur le tiers. On traitera les tâches dans le post-flush.
			for (Tiers tiers : dirty.values()) {
				final Integer oid = tiersService.calculateCurrentOfficeID(tiers);
				final Integer old = tiers.getOfficeImpotId();
				if (!ObjectUtils.equals(oid,old)) {
					tiers.setOfficeImpotId(oid);
					modifs.put(tiers.getNumero(), oid);
				}
			}
		}
		finally {
			dirty.clear();
		}
	}

	@Override
	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames,
	                            Type[] types) throws CallbackException {

		if (!isEnabled()) {
			return false;
		}

		final Tiers tiers = getRelatedTiers(entity);
		if (tiers == null) {
			return false;
		}

		final Integer oid = tiersService.calculateCurrentOfficeID(tiers);
		final Integer old = tiers.getOfficeImpotId();
		if(ObjectUtils.equals(oid,old)){ // on évite de rendre le tiers dirty pour rien
		return false;
		}


		// l'oid doit être mis-à-jour
		getUpdatedOids().put(tiers.getNumero(), oid);

		if (tiers == entity) {
			// Si l'entité flushée est le tiers lui-même, on met-à-jour l'oid à la volée
			boolean found = false;
			for (int i = 0; i < propertyNames.length; ++i) {
				if ("officeImpotId".equals(propertyNames[i])) {
					currentState[i] = oid;
					found = true;
					break;
				}
			}
			Assert.isTrue(found);
			return true; // l'entité a été modifiée
		}
		else {
			// [UNIREG-2386] si l'entité flushée est une autre entité que le tiers (un for fiscal, par exemple), on
			// ne peut pas la mettre-à-jour maintenant (elle ne serait pas prise en compte par Hibernate) => on mémorise
			// le nouvel oid pour une mise-à-jour manuelle dans le postFlush.
			getToUpdateOids().put(tiers.getNumero(), oid);
			return false; // l'entité n'a pas été modifiée
		}
	}

	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {

		if (!isEnabled()) {
			return false;
		}

		Tiers tiers = getRelatedTiers(entity);
		if (tiers != null) {
			// on attend l'événement de pre-flush, car l'entité spécifiée est potentiellement encore incomplète.
			addDirtyEntity(tiers);
		}
		return false;
	}

	@Override
	public void postFlush(Iterator<?> entities) throws CallbackException {
		super.postFlush(entities);

		// [UNIREG-2386] c'est le moment de mettre-à-jour les oids sur les tiers qui n'ont pas pu être traité au fil-de-l'eau
		final HashMap<Long, Integer> toUpdate = getToUpdateOids();
		if (toUpdate != null && !toUpdate.isEmpty()) {
			tiersDAO.updateOids(toUpdate);
			toUpdate.clear();
		}

		// [UNIREG-2306] maintenant que l'on connait les tiers qui ont été changés, on met-à-jour toutes les tâches impactées en vrac
		final HashMap<Long, Integer> updated = getUpdatedOids();
		if (updated != null && !updated.isEmpty()) {
			tacheDAO.updateCollAdmAssignee(updated);
			updated.clear();
		}
	}

	@Override
	public void afterTransactionCompletion(Transaction tx) {
		// comme ça on est sûr de ne pas garder des entités en mémoire
		getToUpdateOids().clear();
		getUpdatedOids().clear();
		getDirtyEntities().clear();
	}

	private Tiers getRelatedTiers(Object entity) {
		if (entity instanceof Tiers) {
			return (Tiers) entity;
		}
		else if (entity instanceof ForFiscal) {
			ForFiscal ff = (ForFiscal) entity;
			return ff.getTiers();
		}
		else {
			return null;
		}
	}

	private void addDirtyEntity(Tiers tiers) {
		if (tiers != null) {
			getDirtyEntities().put(tiers.getNumero(), tiers);
		}
	}

	private HashMap<Long, Tiers> getDirtyEntities() {
		return dirtyEntities.get();
	}

	private HashMap<Long, Integer> getToUpdateOids() {
		return toUpdateOids.get();
	}

	private HashMap<Long, Integer> getUpdatedOids() {
		return updatedOids.get();
	}

	/**
	 * Détecte et met-à-jour le cas échéant l'office d'impôt des tiers spécifiés
	 *
	 * @param list la liste des numéros de tiers
	 */
	public void updateOfficeID(List<Long> list) {

		final HashMap<Long, Integer> todo = new HashMap<>();

		for (Long id : list) {
			final Tiers tiers = tiersDAO.get(id);
			if (tiers == null) {
				continue;
			}

			Integer oid = tiersService.calculateCurrentOfficeID(tiers);
			final Integer old = tiers.getOfficeImpotId();
			if (!ObjectUtils.equals(oid,old)) {
				// Met-à-jour l'OID sur le tiers
				tiers.setOfficeImpotId(oid);

				todo.put(id, oid);
			}
		}

		// [UNIREG-2306] Met-à-jour l'OID sur les éventuelles tâches en instances associé au contribuable
		tacheDAO.updateCollAdmAssignee(todo);
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTacheDAO(TacheDAO tacheDAO) {
		this.tacheDAO = tacheDAO;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@Override
	public void setEnabled(boolean value) {
		this.enabled.setEnabled(value);
	}

	@Override
	public boolean isEnabled() {
		return this.enabled.isEnabled();
	}
}
