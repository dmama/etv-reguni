package ch.vd.unireg.data;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.type.TypeRapportEntreTiers;

/**
 * Implémentation du notifier qui délègue tous les appels à une autre implémentation, qui est elle-même modifiable en tout temps.
 */
public class PluggableFiscalDataEventNotifier implements FiscalDataEventNotifier {

	@Nullable
	private FiscalDataEventNotifier target;

	public PluggableFiscalDataEventNotifier() {
	}

	public PluggableFiscalDataEventNotifier(@Nullable FiscalDataEventNotifier target) {
		this.target = target;
	}

	/**
	 * Spécifie l'implémentation cible du service
	 *
	 * @param target une implémentation
	 */
	public void setTarget(@Nullable FiscalDataEventNotifier target) {
		this.target = target;
	}

	@Nullable
	public FiscalDataEventNotifier getTarget() {
		return target;
	}

	@Override
	public void notifyTiersChange(long id) {
		if (target != null) {
			target.notifyTiersChange(id);
		}
	}

	@Override
	public void notifyDroitAccessChange(long id) {
		if (target != null) {
			target.notifyDroitAccessChange(id);
		}
	}

	@Override
	public void notifyImmeubleChange(long immeubleId) {
		if (target != null) {
			target.notifyImmeubleChange(immeubleId);
		}
	}

	@Override
	public void notifyBatimentChange(long batimentId) {
		if (target != null) {
			target.notifyBatimentChange(batimentId);
		}
	}

	@Override
	public void notifyCommunauteChange(long communauteId) {
		if (target != null) {
			target.notifyCommunauteChange(communauteId);
		}
	}

	@Override
	public void notifyRelationshipChange(TypeRapportEntreTiers type, long sujetId, long objetId) {
		if (target != null) {
			target.notifyRelationshipChange(type, sujetId, objetId);
		}
	}

	@Override
	public void notifyLoadDatabase() {
		if (target != null) {
			target.notifyLoadDatabase();
		}
	}

	@Override
	public void notifyTruncateDatabase() {
		if (target != null) {
			target.notifyTruncateDatabase();
		}
	}
}
