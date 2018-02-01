package ch.vd.unireg.evenement.civil.engine.ech;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.TypeEvenementCivilEch;

public class EvenementCivilEchTranslatorImplOverride extends EvenementCivilEchTranslatorImpl {

	private Map<EventTypeKey, EvenementCivilEchTranslationStrategy> testOverride = null;

	@Override
	@Nullable
	protected EvenementCivilEchTranslationStrategy getStrategy(EventTypeKey key) {
		return testOverride != null && testOverride.containsKey(key) ? testOverride.get(key) : super.getStrategy(key);
	}

	/**
	 * Pour les tests uniquement...
	 * @param type type d'événement civil
	 * @param action action sur l'événement civil
	 * @param strategy stratégie associée
	 */
	public void overrideStrategy(TypeEvenementCivilEch type, ActionEvenementCivilEch action, EvenementCivilEchTranslationStrategy strategy) {
		if (testOverride == null) {
			testOverride = new HashMap<>();
		}
		testOverride.put(new EventTypeKey(type, action), strategy);
	}
}

