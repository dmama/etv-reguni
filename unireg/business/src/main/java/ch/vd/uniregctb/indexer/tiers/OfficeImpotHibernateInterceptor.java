package ch.vd.uniregctb.indexer.tiers;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.hibernate.interceptor.AbstractLinkedInterceptor;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.*;
import org.apache.log4j.Logger;
import org.hibernate.CallbackException;
import org.hibernate.Transaction;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Cet intercepteur se charge de tenir à-jour l'id de l'office d'impôt caché au niveau de chaque tiers.
 */
public class OfficeImpotHibernateInterceptor extends AbstractLinkedInterceptor {

	private static final Logger LOGGER = Logger.getLogger(OfficeImpotHibernateInterceptor.class);

	private TiersService tiersService;
	private TacheDAO tacheDAO;
	private ServiceInfrastructureService infraService;

	private static class Behavior {
		public boolean enabled = true;
	}

	private final ThreadLocal<Behavior> byThreadBehavior = new ThreadLocal<Behavior>();

	private final ThreadLocal<HashMap<Long, Tiers>> dirtyEntities = new ThreadLocal<HashMap<Long, Tiers>>();

	/**
	 * Une map numéro de tiers -> nouveau numéro d'oid des tiers qui ont été changés.
	 */
	private final ThreadLocal<HashMap<Long, Integer>> modifiedOids = new ThreadLocal<HashMap<Long, Integer>>();

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
		final HashMap<Long, Integer> modifs = getModifiedOids();
		try {
			// On change la valeur de l'oid sur le tiers. On traitera les tâches dans le post-flush.
			for (Tiers tiers : dirty.values()) {
				final Integer oid = detectNewOfficeID(tiers);
				if (oid != null) {
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

		Tiers tiers = checkEntity(entity);
		if (tiers != null) {
			Integer oid = detectNewOfficeID(tiers);
			if (oid != null) {
				// Met-à-jour l'OID
				for (int i = 0; i < propertyNames.length; ++i) {
					if ("officeImpotId".equals(propertyNames[i])) {
						currentState[i] = oid;
						break;
					}
				}
				getModifiedOids().put(tiers.getNumero(), oid);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {

		if (!isEnabled()) {
			return false;
		}

		Tiers tiers = checkEntity(entity);
		if (tiers != null) {
			// on attend l'événement de pre-flush, car l'entité spécifiée est potentiellement encore incomplète.
			addDirtyEntity(tiers);
		}
		return false;
	}

	@Override
	public void postFlush(Iterator<?> entities) throws CallbackException {
		super.postFlush(entities);

		// [UNIREG-2306] maintenant que l'on connait les tiers qui ont été changés, on met-à-jour toutes les tâches impactées en vrac
		final HashMap<Long, Integer> modifs = getModifiedOids();
		for (Map.Entry<Long, Integer> e : modifs.entrySet()) {
			tacheDAO.updateCollAdmAssignee(e.getKey(), e.getValue());
		}
		modifs.clear();
	}

	@Override
	public void afterTransactionCompletion(Transaction tx) {
		// comme ça on est sûr de ne pas garder des entités en mémoire
		getModifiedOids().clear();
		getDirtyEntities().clear();
	}

	private Tiers checkEntity(Object entity) {
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

	private HashMap<Long, Integer> getModifiedOids() {
		HashMap<Long, Integer> ent = modifiedOids.get();
		if (ent == null) {
			ent = new HashMap<Long, Integer>();
			modifiedOids.set(ent);
		}
		return ent;
	}

	/**
	 * Détecte et met-à-jour le cas échéant l'office d'impôt du tiers spécifié, et retourne la nouvelle valeur.
	 *
	 * @param tiers le tiers à mettre à jour.
	 * @return la nouvelle valeur de l'OID, ou <b>null</b> si l'office d'impôt n'a pas été changé.
	 */
	public Integer updateOfficeID(Tiers tiers) {
		Integer oid = detectNewOfficeID(tiers);
		if (oid != null) {
			// Met-à-jour l'OID sur le tiers
			tiers.setOfficeImpotId(oid);

			// [UNIREG-2306] Met-à-jour l'OID sur les éventuelles tâches en instances associé au contribuable
			if (tiers instanceof Contribuable) {
				tacheDAO.updateCollAdmAssignee(tiers.getNumero(), oid);
			}
		}
		return oid;
	}

	/**
	 * Détecte si l'id de l'office d'impôt du tiers spécifié doit être changé, et retourne la nouvelle valeur. Le tiers n'est pas modifié.
	 *
	 * @param tiers le tiers à mettre à jour.
	 * @return le numéro du nouveau OID; ou <b>null</b> si le tiers est à jour.
	 */
	private Integer detectNewOfficeID(Tiers tiers) {
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
		}
		final Integer old = tiers.getOfficeImpotId();
		if (oid != null && !oid.equals(old)) { // on évite de rendre le tiers dirty pour rien
			return oid;
		}
		else {
			return null;
		}
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
