package ch.vd.unireg.data;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.type.TypeRapportEntreTiers;

/**
 * Implémentation du service qui délègue tous les appels à une autre implémentation, qui est elle-même modifiable en tout temps.
 */
public class PluggableFiscalDataEventService implements FiscalDataEventService {

	@Nullable
	private FiscalDataEventService target;

	public PluggableFiscalDataEventService() {
	}

	public PluggableFiscalDataEventService(@Nullable FiscalDataEventService target) {
		this.target = target;
	}

	/**
	 * Spécifie l'implémentation cible du service
	 *
	 * @param target une implémentation
	 */
	public void setTarget(@Nullable FiscalDataEventService target) {
		this.target = target;
	}

	@Nullable
	public FiscalDataEventService getTarget() {
		return target;
	}

	@Override
	public void onTiersChange(long id) {
		if (target != null) {
			target.onTiersChange(id);
		}
	}

	@Override
	public void onDroitAccessChange(long id) {
		if (target != null) {
			target.onDroitAccessChange(id);
		}
	}

	@Override
	public void onImmeubleChange(long immeubleId) {
		if (target != null) {
			target.onImmeubleChange(immeubleId);
		}
	}

	@Override
	public void onBatimentChange(long batimentId) {
		if (target != null) {
			target.onBatimentChange(batimentId);
		}
	}

	@Override
	public void onCommunauteChange(long communauteId) {
		if (target != null) {
			target.onCommunauteChange(communauteId);
		}
	}

	@Override
	public void onRelationshipChange(TypeRapportEntreTiers type, long sujetId, long objetId) {
		if (target != null) {
			target.onRelationshipChange(type, sujetId, objetId);
		}
	}

	@Override
	public void onLoadDatabase() {
		if (target != null) {
			target.onLoadDatabase();
		}
	}

	@Override
	public void onTruncateDatabase() {
		if (target != null) {
			target.onTruncateDatabase();
		}
	}
}
