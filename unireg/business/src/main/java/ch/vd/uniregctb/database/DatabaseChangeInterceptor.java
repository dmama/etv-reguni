package ch.vd.uniregctb.database;

import java.io.Serializable;
import java.util.Set;

import org.hibernate.CallbackException;
import org.hibernate.type.Type;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.hibernate.interceptor.ModificationInterceptor;
import ch.vd.uniregctb.hibernate.interceptor.ModificationSubInterceptor;
import ch.vd.uniregctb.tiers.DroitAcces;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * Cette classe intercepte les changements appliqués au tiers dans la base de données et propage l'information au database service.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class DatabaseChangeInterceptor implements ModificationSubInterceptor, InitializingBean {

	private ModificationInterceptor parent;
	private DataEventService dataEventService;

	public void setParent(ModificationInterceptor parent) {
		this.parent = parent;
	}

	public void setDataEventService(DataEventService dataEventService) {
		this.dataEventService = dataEventService;
	}

	public boolean onChange(HibernateEntity entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames,
			Type[] types) throws CallbackException {

		if (entity instanceof Tiers) {
			// Un tiers a été modifié en base => on purge tous les éléments cachés concernant ce tiers
			final Tiers tiers = (Tiers) entity;
			final Long numero = tiers.getNumero();
			if (numero != null) {
				dataEventService.onTiersChange(numero);
			}
		}
		if (entity instanceof Declaration) {
			// Cas spécial pour les déclarations car il n'y a pas de cascade merge entre Declaration et Tiers.
			final Declaration declaration = (Declaration) entity;
			final Long numero = declaration.getTiers().getNumero();
			if (numero != null) {
				dataEventService.onTiersChange(numero);
			}
		}
		else if (entity instanceof DroitAcces) {
			// [UNIREG-1191] Un droit d'accès a été modifié en base => on purge tous les tiers impactés par le changement
			final DroitAcces da = (DroitAcces) entity;
			final PersonnePhysique pp = da.getTiers();

			// le tiers lui-même
			final Long numero = pp.getNumero();
			if (numero != null) {
				dataEventService.onDroitAccessChange(numero);
			}

			// tous les ménages communs auxquel il a pu appartenir
			final Set<RapportEntreTiers> rapports = pp.getRapportsSujet();
			if (rapports != null) {
				for (RapportEntreTiers r : rapports) {
					if (r.isAnnule() || r.getType() != TypeRapportEntreTiers.APPARTENANCE_MENAGE) {
						continue;
					}
					final Long mcId = r.getObjetId();
					dataEventService.onDroitAccessChange(mcId);
				}
			}
		}
		return false;
	}

	public void postFlush() throws CallbackException {
		// rien à faire ici
	}

	public void preTransactionCommit() {
		// rien à faire ici
	}

	public void postTransactionCommit() {
		// rien à faire ici
	}

	public void postTransactionRollback() {
		// rien à faire ici
	}

	public void afterPropertiesSet() throws Exception {
		parent.register(this);
	}
}
