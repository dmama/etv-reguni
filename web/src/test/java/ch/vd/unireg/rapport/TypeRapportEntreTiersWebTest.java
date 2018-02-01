package ch.vd.unireg.rapport;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.type.TypeRapportEntreTiers;

public class TypeRapportEntreTiersWebTest extends WithoutSpringTest {

	@Test
	public void testConsistanceAvecTypeDeCore() {

		// toutes les valeurs de CORE doivent être reprises dans le WEB
		for (TypeRapportEntreTiers modalite : TypeRapportEntreTiers.values()) {
			final TypeRapportEntreTiersWeb modaliteWeb = TypeRapportEntreTiersWeb.fromCore(modalite);
			Assert.assertNotNull(modaliteWeb);
			Assert.assertEquals(modalite, modaliteWeb.toCore());
		}

		// toutes les valeurs de WEB doivent être reprises dans le CORE
		for (TypeRapportEntreTiersWeb modalite : TypeRapportEntreTiersWeb.values()) {
			final TypeRapportEntreTiers modaliteCore = modalite.toCore();
			Assert.assertNotNull(modaliteCore);
			Assert.assertEquals(modalite, TypeRapportEntreTiersWeb.fromCore(modaliteCore));
		}
	}
}
