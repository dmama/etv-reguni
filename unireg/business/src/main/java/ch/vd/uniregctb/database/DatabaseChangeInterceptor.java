package ch.vd.uniregctb.database;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.CallbackException;
import org.hibernate.type.Type;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.hibernate.interceptor.ModificationInterceptor;
import ch.vd.uniregctb.hibernate.interceptor.ModificationSubInterceptor;
import ch.vd.uniregctb.tiers.DroitAcces;
import ch.vd.uniregctb.tiers.LinkedEntity;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * Cette classe intercepte les changements appliqués au tiers dans la base de données et propage l'information au database service.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class DatabaseChangeInterceptor implements ModificationSubInterceptor, InitializingBean {

	private ModificationInterceptor parent;
	private DataEventService dataEventService;
	private TiersService tiersService;
	private final ThreadLocal<Data> data = new ThreadLocal<Data>() {
		@Override
		protected Data initialValue() {
			return new Data();
		}
	};

	@SuppressWarnings({"UnusedDeclaration"})
	public void setParent(ModificationInterceptor parent) {
		this.parent = parent;
	}

	public void setDataEventService(DataEventService dataEventService) {
		this.dataEventService = dataEventService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	private static final class RelationshipKey implements Serializable {

		private static final long serialVersionUID = -2980295059934269128L;

		public final TypeRapportEntreTiers type;
		public final long sujetId;
		public final long objetId;

		private RelationshipKey(TypeRapportEntreTiers type, long sujetId, long objetId) {
			this.type = type;
			this.sujetId = sujetId;
			this.objetId = objetId;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final RelationshipKey that = (RelationshipKey) o;

			if (objetId != that.objetId) return false;
			if (sujetId != that.sujetId) return false;
			if (type != that.type) return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = type.hashCode();
			result = 31 * result + (int) (sujetId ^ (sujetId >>> 32));
			result = 31 * result + (int) (objetId ^ (objetId >>> 32));
			return result;
		}

		@Override
		public String toString() {
			return "RelationshipKey{" +
					"type=" + type +
					", sujetId=" + sujetId +
					", objetId=" + objetId +
					'}';
		}
	}

	/**
	 * Ids des tiers changés qui ont déjà été notifiés.
	 */
	private static class Data {
		private final Set<Long> tiersChange = new HashSet<>();
		private final Set<Long> droitsAccesChange = new HashSet<>();
		private final Set<RelationshipKey> relationshipChange = new HashSet<>();

		public void addTiersChange(Long id) {
			tiersChange.add(id);
		}

		public void addDroitAccesChange(Long id) {
			droitsAccesChange.add(id);
		}

		public void addRelationshipChange(RelationshipKey key) {
			relationshipChange.add(key);
		}

		public boolean containsTiersChange(Long id) {
			return tiersChange.contains(id);
		}

		public boolean containsDroitAcessChange(Long id) {
			return droitsAccesChange.contains(id);
		}

		public boolean containsRelationshipChange(RelationshipKey key) {
			return relationshipChange.contains(key);
		}

		public void clear() {
			tiersChange.clear();
			droitsAccesChange.clear();
			relationshipChange.clear();
		}
	}

	@Override
	public boolean onChange(HibernateEntity entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types, boolean isAnnulation) throws CallbackException {

		if (entity instanceof Tiers) {
			// Un tiers a été modifié en base => on envoie un événement correspondant
			final Tiers tiers = (Tiers) entity;
			final Long numero = tiers.getNumero();
			if (numero != null) {
				onTiersChange(numero);
			}
		}
		else if (entity instanceof RapportEntreTiers) {
			final RapportEntreTiers ret = (RapportEntreTiers) entity;
			onRelationshipChange(ret.getType(), ret.getSujetId(), ret.getObjetId());
			onTiersChange(ret.getSujetId());
			onTiersChange(ret.getObjetId());
		}
		else if (entity instanceof LinkedEntity) { // [UNIREG-2581] on doit remonter sur le tiers en cas de changement sur les classes satellites
			final LinkedEntity child = (LinkedEntity) entity;
			// [SIFISC-915] En cas d'annulation, on DOIT inclure les liens nouvellement annulés pour invalider correctement les caches
			final Set<Tiers> tiers = tiersService.getLinkedTiers(child, isAnnulation);
			for (Tiers t : tiers) {
				onTiersChange(t.getNumero());
			}
		}
		else if (entity instanceof DroitAcces) {
			// [UNIREG-1191] Un droit d'accès a été modifié en base => on purge tous les tiers impactés par le changement
			final DroitAcces da = (DroitAcces) entity;
			final PersonnePhysique pp = da.getTiers();

			// le tiers lui-même
			final Long numero = pp.getNumero();
			if (numero != null) {
				onDroitAccesChange(numero);
			}

			// tous les ménages communs auxquel il a pu appartenir
			final Set<RapportEntreTiers> rapports = pp.getRapportsSujet();
			if (rapports != null) {
				for (RapportEntreTiers r : rapports) {
					if (r.isAnnule() || r.getType() != TypeRapportEntreTiers.APPARTENANCE_MENAGE) {
						continue;
					}
					final Long mcId = r.getObjetId();
					onDroitAccesChange(mcId);
				}
			}
		}
		return false;
	}

	private void onTiersChange(Long id) {
		final Data d = getThreadData();
		if (!d.containsTiersChange(id)) { // on n'envoie un événement qu'une seule fois par transaction
			dataEventService.onTiersChange(id);
			d.addTiersChange(id);
		}
	}

	private void onDroitAccesChange(Long id) {
		final Data d = getThreadData();
		if (!d.containsDroitAcessChange(id)) { // on n'envoie un événement qu'une seule fois par transaction
			dataEventService.onDroitAccessChange(id);
			d.addDroitAccesChange(id);
		}
	}

	private void onRelationshipChange(TypeRapportEntreTiers type, Long sujetId, Long objetId) {
		final Data d = getThreadData();
		final RelationshipKey key = new RelationshipKey(type, sujetId, objetId);
		if (!d.containsRelationshipChange(key)) { // on n'envoie un événement qu'une seule fois par transaction
			dataEventService.onRelationshipChange(type, sujetId, objetId);
			d.addRelationshipChange(key);
		}
	}

	/**
	 * @return les données locales au thread courant. Ces données sont créées à la demande si nécessaire.
	 */
	private Data getThreadData() {
		return data.get();
	}

	private void clearThreadData() {
		final Data d = data.get();
		if (d != null) {
			d.clear();
		}
	}

	@Override
	public void postFlush() throws CallbackException {
		// rien à faire ici
	}

	@Override
	public void preTransactionCommit() {
		// rien à faire ici
	}

	@Override
	public void postTransactionCommit() {
		clearThreadData();
	}

	@Override
	public void postTransactionRollback() {
		clearThreadData();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		parent.register(this);
	}
}
