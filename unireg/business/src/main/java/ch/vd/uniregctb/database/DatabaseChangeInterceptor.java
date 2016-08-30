package ch.vd.uniregctb.database;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Set;

import org.hibernate.CallbackException;
import org.hibernate.type.Type;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.hibernate.interceptor.ModificationInterceptor;
import ch.vd.uniregctb.hibernate.interceptor.ModificationSubInterceptor;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DroitAcces;
import ch.vd.uniregctb.tiers.LinkedEntity;
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
			final Contribuable ctb = da.getTiers();

			// le tiers lui-même
			final Long numero = ctb.getNumero();
			if (numero != null) {
				onDroitAccesChange(numero);
			}

			// tous les ménages communs auxquel il a pu appartenir, ou les établissements liés
			final Set<RapportEntreTiers> rapports = ctb.getRapportsSujet();
			if (rapports != null) {
				final Set<TypeRapportEntreTiers> typesPropages = EnumSet.of(TypeRapportEntreTiers.APPARTENANCE_MENAGE,
				                                                            TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE);
				for (RapportEntreTiers r : rapports) {
					if (!r.isAnnule() && typesPropages.contains(r.getType())) {
						onDroitAccesChange(r.getObjetId());
					}
				}
			}
		}
		return false;
	}

	private void onTiersChange(Long id) {
		dataEventService.onTiersChange(id);
	}

	private void onDroitAccesChange(Long id) {
		dataEventService.onDroitAccessChange(id);
	}

	private void onRelationshipChange(TypeRapportEntreTiers type, Long sujetId, Long objetId) {
		dataEventService.onRelationshipChange(type, sujetId, objetId);
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
		// rien à faire ici
	}

	@Override
	public void postTransactionRollback() {
		// rien à faire ici
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		parent.register(this);
	}
}
