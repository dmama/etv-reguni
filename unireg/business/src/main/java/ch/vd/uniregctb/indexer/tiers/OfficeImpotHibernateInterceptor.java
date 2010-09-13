package ch.vd.uniregctb.indexer.tiers;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.ObjectUtils;
import org.apache.log4j.Logger;
import org.hibernate.CallbackException;
import org.hibernate.Transaction;
import org.hibernate.type.Type;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.hibernate.interceptor.AbstractLinkedInterceptor;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForGestion;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Cet intercepteur se charge de tenir à-jour l'id de l'office d'impôt caché au niveau de chaque tiers.
 */
public class OfficeImpotHibernateInterceptor extends AbstractLinkedInterceptor {

	private static final Logger LOGGER = Logger.getLogger(OfficeImpotHibernateInterceptor.class);

	private TiersService tiersService;
	private TacheDAO tacheDAO;
	private TiersDAO tiersDAO;
	private ServiceInfrastructureService infraService;

	private static class Behavior {
		public boolean enabled = true;
	}

	private final ThreadLocal<Behavior> byThreadBehavior = new ThreadLocal<Behavior>();

	private final ThreadLocal<HashMap<Long, Tiers>> dirtyEntities = new ThreadLocal<HashMap<Long, Tiers>>();

	/**
	 * Les tiers (avec leurs nouveaux OIDs) qui doivent être mis-à-jour
	 */
	private final ThreadLocal<HashMap<Long, Integer>> toUpdateOids = new ThreadLocal<HashMap<Long, Integer>>();

	/**
	 * Une map numéro de tiers -> nouveau numéro d'oid des tiers qui ont été changés.
	 */
	private final ThreadLocal<HashMap<Long, Integer>> updatedOids = new ThreadLocal<HashMap<Long, Integer>>();

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
				final Integer oid = calculateCurrentOfficeID(tiers);
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

		final Integer oid = calculateCurrentOfficeID(tiers);
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
		HashMap<Long, Tiers> ent = dirtyEntities.get();
		if (ent == null) {
			ent = new HashMap<Long, Tiers>();
			dirtyEntities.set(ent);
		}
		return ent;
	}

	private HashMap<Long, Integer> getToUpdateOids() {
		HashMap<Long, Integer> ent = toUpdateOids.get();
		if (ent == null) {
			ent = new HashMap<Long, Integer>();
			toUpdateOids.set(ent);
		}
		return ent;
	}

	private HashMap<Long, Integer> getUpdatedOids() {
		HashMap<Long, Integer> ent = updatedOids.get();
		if (ent == null) {
			ent = new HashMap<Long, Integer>();
			updatedOids.set(ent);
		}
		return ent;
	}

	/**
	 * Détecte et met-à-jour le cas échéant l'office d'impôt des tiers spécifiés
	 *
	 * @param list la liste des numéros de tiers
	 */
	public void updateOfficeID(List<Long> list) {

		final HashMap<Long, Integer> todo = new HashMap<Long, Integer>();

		for (Long id : list) {
			final Tiers tiers = tiersDAO.get(id);
			if (tiers == null) {
				continue;
			}

			Integer oid = calculateCurrentOfficeID(tiers);
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

	/**
	 * Détecte si l'id de l'office d'impôt du tiers spécifié doit être changé, et retourne la nouvelle valeur. Le tiers n'est pas modifié.
	 *
	 * @param tiers le tiers à mettre à jour.
	 * @return le numéro du nouveau OID; ou <b>null</b> si le tiers n'a pas de for de gestion donc pas d'OID.
	 */
	private Integer calculateCurrentOfficeID(Tiers tiers) {
		Integer oid = null;
		final ForGestion forGestion = tiersService.getDernierForGestionConnu(tiers, null);
		if (forGestion != null) {
			int noOfs = forGestion.getNoOfsCommune();
			oid = getOID(noOfs);
		}
		else {
			/*
			 * si le contribuable n'a pas de for de gestion (e.g. contribuable pour tous, mineur, ...), ne pas avoir d'oid est sans
			 * importance: le contribuable n'est pas assujetti.
			 */
			return null;
		}
		  	return oid;

	}

	/**
	 * @param noOfs le numéro Ofs d'une commune
	 * @return l'id de l'office d'impôt responsable de la commune spécifiée en paramètre.
	 */
	private Integer getOID(int noOfs) {
		Integer oid = null;
		try {
			CollectiviteAdministrative office = infraService.getOfficeImpotDeCommune(noOfs);
			if (office == null) {
				LOGGER.warn("Le service infrastructure a retourné un office d'impôt nul pour la commune avec le numéro OFS " + noOfs);
			}
			else {
				oid = office.getNoColAdm();
			}
		}
		catch (InfrastructureException e) {
			LOGGER.error("Impossible de récupérer l'office d'impôt de la commune avec le numéro OFS " + noOfs, e);
			throw new CallbackException(e);
		}
		return oid;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setTacheDAO(TacheDAO tacheDAO) {
		this.tacheDAO = tacheDAO;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	private Behavior getByThreadBehavior() {
		Behavior behavior = this.byThreadBehavior.get();
		if (behavior == null) {
			behavior = new Behavior();
			this.byThreadBehavior.set(behavior);
		}
		return behavior;
	}

	public void setEnabled(boolean value) {
		getByThreadBehavior().enabled = value;
	}

	public boolean isEnabled() {
		return getByThreadBehavior().enabled;
	}
}
