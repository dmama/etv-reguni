package ch.vd.uniregctb.database;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Set;

import org.hibernate.CallbackException;
import org.hibernate.type.Type;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.hibernate.interceptor.ModificationInterceptor;
import ch.vd.uniregctb.hibernate.interceptor.ModificationSubInterceptor;
import ch.vd.uniregctb.registrefoncier.BatimentRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
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
public class DatabaseChangeInterceptor implements ModificationSubInterceptor, InitializingBean, DisposableBean {

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
		// notification sur l'entité et ses nouvelles valeurs
		notifyChange(entity, isAnnulation);

		// [SIFISC-25533] En cas de changement de lien vers une entité, il faut notifier à la fois l'ancienne et la nouvelle entité
		// (la nouvelle a normalement déjà été notifiée dans l'appel à {@link #notifyChange()} plus haut, reste l'ancienne)
		if (previousState != null && entity instanceof LinkedEntity) {
			for (int i = 0; i < types.length; ++i) {
				final Object currentValue = currentState[i];
				final Object previousValue = previousState[i];
				final Type type = types[i];
				if (currentValue != previousValue && type.isEntityType() && previousValue instanceof HibernateEntity) {
					notifyChange((HibernateEntity) previousValue, isAnnulation);
				}
			}
		}
		return false;
	}

	private void notifyChange(HibernateEntity entity, boolean isAnnulation) {
		if (entity instanceof Tiers) {
			// Un tiers a été modifié en base => on envoie un événement correspondant
			final Tiers tiers = (Tiers) entity;
			final Long numero = tiers.getNumero();
			if (numero != null) {
				dataEventService.onTiersChange(numero);
			}
		}
		else if (entity instanceof ImmeubleRF) {
			// Un immeuble a été modifié en base => on envoie un événement correspondant
			final ImmeubleRF immeuble = (ImmeubleRF) entity;
			final Long i = immeuble.getId();
			if (i != null) {
				dataEventService.onImmeubleChange(i);
			}
		}
		else if (entity instanceof BatimentRF) {
			// Un bâtiment a été modifié en base => on envoie un événement correspondant
			final BatimentRF batiment = (BatimentRF) entity;
			final Long i = batiment.getId();
			if (i != null) {
				dataEventService.onBatimentChange(i);
			}
		}
		else if (entity instanceof CommunauteRF) {
			// une communauté de propriétaires a été modifiée en base => on envoie un événement correspondant
			final CommunauteRF communaute = (CommunauteRF) entity;
			final Long i = communaute.getId();
			if (i != null) {
				dataEventService.onCommunauteChange(i);
			}
		}
		else if (entity instanceof RapportEntreTiers) {
			final RapportEntreTiers ret = (RapportEntreTiers) entity;
			dataEventService.onRelationshipChange(ret.getType(), ret.getSujetId(), ret.getObjetId());
			dataEventService.onTiersChange(ret.getSujetId());
			dataEventService.onTiersChange(ret.getObjetId());
		}
		else if (entity instanceof LinkedEntity) { // [UNIREG-2581] on doit remonter sur le tiers en cas de changement sur les classes satellites
			final LinkedEntity child = (LinkedEntity) entity;
			// [SIFISC-915] En cas d'annulation, on DOIT inclure les liens nouvellement annulés pour invalider correctement les caches
			final Set<Tiers> tiers = tiersService.getLinkedEntities(child, Tiers.class, LinkedEntity.Context.DATA_EVENT, isAnnulation);
			for (Tiers t : tiers) {
				dataEventService.onTiersChange(t.getNumero());
			}
			final Set<ImmeubleRF> immeubles = tiersService.getLinkedEntities(child, ImmeubleRF.class, LinkedEntity.Context.DATA_EVENT, isAnnulation);
			for (ImmeubleRF i : immeubles) {
				dataEventService.onImmeubleChange(i.getId());
			}
			final Set<BatimentRF> batiments = tiersService.getLinkedEntities(child, BatimentRF.class, LinkedEntity.Context.DATA_EVENT, isAnnulation);
			for (BatimentRF b : batiments) {
				dataEventService.onBatimentChange(b.getId());
			}
			final Set<CommunauteRF> communautes = tiersService.getLinkedEntities(child, CommunauteRF.class, LinkedEntity.Context.DATA_EVENT, isAnnulation);
			for (CommunauteRF c : communautes) {
				dataEventService.onCommunauteChange(c.getId());
			}
		}
		else if (entity instanceof DroitAcces) {
			// [UNIREG-1191] Un droit d'accès a été modifié en base => on purge tous les tiers impactés par le changement
			final DroitAcces da = (DroitAcces) entity;
			final Contribuable ctb = da.getTiers();

			// le tiers lui-même
			final Long numero = ctb.getNumero();
			if (numero != null) {
				dataEventService.onDroitAccessChange(numero);
			}

			// tous les ménages communs auxquel il a pu appartenir, ou les établissements liés
			final Set<RapportEntreTiers> rapports = ctb.getRapportsSujet();
			if (rapports != null) {
				final Set<TypeRapportEntreTiers> typesPropages = EnumSet.of(TypeRapportEntreTiers.APPARTENANCE_MENAGE,
				                                                            TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE);
				for (RapportEntreTiers r : rapports) {
					if (!r.isAnnule() && typesPropages.contains(r.getType())) {
						dataEventService.onDroitAccessChange(r.getObjetId());
					}
				}
			}
		}
	}

	@Override
	public void postFlush() throws CallbackException {
		// rien à faire ici
	}

	@Override
	public void suspendTransaction() {
		// rien à faire ici
	}

	@Override
	public void resumeTransaction() {
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

	@Override
	public void destroy() throws Exception {
		parent.unregister(this);
	}
}
