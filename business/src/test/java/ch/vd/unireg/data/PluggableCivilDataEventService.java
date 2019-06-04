package ch.vd.unireg.data;

import org.jetbrains.annotations.Nullable;

/**
 * Implémentation du service qui délègue tous les appels à une autre implémentation, qui est elle-même modifiable en tout temps.
 */
public class PluggableCivilDataEventService implements CivilDataEventService {

	@Nullable
	private CivilDataEventService target;

	public PluggableCivilDataEventService() {
	}

	public PluggableCivilDataEventService(@Nullable CivilDataEventService target) {
		this.target = target;
	}

	/**
	 * Spécifie l'implémentation cible du service
	 *
	 * @param target une implémentation
	 */
	public void setTarget(@Nullable CivilDataEventService target) {
		this.target = target;
	}

	@Nullable
	public CivilDataEventService getTarget() {
		return target;
	}

	@Override
	public void onIndividuChange(long id) {
		if (target != null) {
			target.onIndividuChange(id);
		}
	}

	@Override
	public void onEntrepriseChange(long id) {
		if (target != null) {
			target.onEntrepriseChange(id);
		}
	}
}
