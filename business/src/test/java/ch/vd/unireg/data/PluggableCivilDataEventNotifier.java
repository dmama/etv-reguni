package ch.vd.unireg.data;

import org.jetbrains.annotations.Nullable;

/**
 * Implémentation du notifier qui délègue tous les appels à une autre implémentation, qui est elle-même modifiable en tout temps.
 */
public class PluggableCivilDataEventNotifier implements CivilDataEventNotifier {

	@Nullable
	private CivilDataEventNotifier target;

	public PluggableCivilDataEventNotifier() {
	}

	public PluggableCivilDataEventNotifier(@Nullable CivilDataEventNotifier target) {
		this.target = target;
	}

	/**
	 * Spécifie l'implémentation cible du service
	 *
	 * @param target une implémentation
	 */
	public void setTarget(@Nullable CivilDataEventNotifier target) {
		this.target = target;
	}

	@Nullable
	public CivilDataEventNotifier getTarget() {
		return target;
	}

	@Override
	public void notifyIndividuChange(long id) {
		if (target != null) {
			target.notifyIndividuChange(id);
		}
	}

	@Override
	public void notifyEntrepriseChange(long id) {
		if (target != null) {
			target.notifyEntrepriseChange(id);
		}
	}
}
