package ch.vd.unireg.database;

import java.io.Serializable;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.CallbackException;
import org.hibernate.type.Type;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;
import ch.vd.unireg.common.linkedentity.LinkedEntityPhase;
import ch.vd.unireg.data.DataEventService;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.hibernate.interceptor.ModificationInterceptor;
import ch.vd.unireg.hibernate.interceptor.ModificationSubInterceptor;
import ch.vd.unireg.registrefoncier.BatimentRF;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.DroitAcces;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.TypeRapportEntreTiers;

/**
 * Cette classe intercepte les changements appliqués au tiers dans la base de données et propage l'information au database service.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class DatabaseChangeInterceptor implements ModificationSubInterceptor, InitializingBean, DisposableBean {

	private ModificationInterceptor parent;
	private DataEventService dataEventService;
	private TiersService tiersService;
	private HibernateTemplate hibernateTemplate;

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

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
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
			handleLinkedEntity(ret, isAnnulation);
		}
		else if (entity instanceof LinkedEntity) { // [UNIREG-2581] on doit remonter sur le tiers en cas de changement sur les classes satellites
			handleLinkedEntity((LinkedEntity) entity, isAnnulation);
		}
		else if (entity instanceof DroitAcces) {
			// [UNIREG-1191] Un droit d'accès a été modifié en base => on purge tous les tiers impactés par le changement
			handleDroitAcces((DroitAcces) entity);
		}
	}

	private void handleLinkedEntity(LinkedEntity child, boolean isAnnulation) {
		// [SIFISC-915] En cas d'annulation, on DOIT inclure les liens nouvellement annulés pour invalider correctement les caches
		final Set<HibernateEntity> linked = tiersService.getLinkedEntities(child,
		                                                                   new HashSet<>(Arrays.asList(Tiers.class,
		                                                                                               ImmeubleRF.class,
		                                                                                               BatimentRF.class,
		                                                                                               CommunauteRF.class)),
		                                                                   new LinkedEntityContext(LinkedEntityPhase.DATA_EVENT, hibernateTemplate),
		                                                                   isAnnulation);
		for (HibernateEntity e : linked) {
			if (e instanceof Tiers) {
				dataEventService.onTiersChange(((Tiers) e).getNumero());
			}
			else if (e instanceof ImmeubleRF) {
				dataEventService.onImmeubleChange(((ImmeubleRF) e).getId());
			}
			else if (e instanceof BatimentRF) {
				dataEventService.onBatimentChange(((BatimentRF) e).getId());
			}
			else if (e instanceof CommunauteRF) {
				dataEventService.onCommunauteChange(((CommunauteRF) e).getId());
			}
			else {
				throw new IllegalArgumentException("Type d'entité inconnu = [" + e.getClass() + "]");
			}
		}
	}

	private void handleDroitAcces(DroitAcces da) {
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
