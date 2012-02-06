package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;
import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class EvenementCivilEchTranslatorTest extends WithoutSpringTest {
	
	@Test
	public void testCouvertureStrategies() throws Exception {
		
		final Set<TypeEvenementCivilEch> typeIgnores = new HashSet<TypeEvenementCivilEch>(Arrays.asList(TypeEvenementCivilEch.ETAT_COMPLET, TypeEvenementCivilEch.TESTING));
		for (TypeEvenementCivilEch type : TypeEvenementCivilEch.values()) {
			if (!typeIgnores.contains(type)) {
				for (ActionEvenementCivilEch action : ActionEvenementCivilEch.values()) {
					if (action != ActionEvenementCivilEch.ECHANGE_DE_CLE) {
						final EvenementCivilEchTranslatorImpl.EventTypeKey key = new EvenementCivilEchTranslatorImpl.EventTypeKey(type, action);
						final EvenementCivilEchTranslationStrategy strategy = EvenementCivilEchTranslatorImpl.getStrategy(key);
						Assert.assertNotNull(String.format("Pas de stratégie pour la combinaison %s/%s", type, action), strategy);
					}
				}
			}
		}
	}
}
