package ch.vd.uniregctb.rapport;

import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import org.junit.Assert;
import org.junit.Test;

public class TypeRapportEntreTiersWebTest extends WithoutSpringTest {

	@Test
	public void testConsistanceAvecTypeDeCore() {

		// toutes les valeurs de CORE doivent être reprises dans le WEB
		for (TypeRapportEntreTiers modalite : TypeRapportEntreTiers.values()) {
			final TypeRapportEntreTiersWeb modaliteWeb = TypeRapportEntreTiersWeb.fromCore(modalite);
			Assert.assertNotNull(modaliteWeb);
			Assert.assertEquals(modalite, modaliteWeb.toCore());
		}

		// seule la filiation n'existe que du côté WEB...
		for (TypeRapportEntreTiersWeb modalite : TypeRapportEntreTiersWeb.values()) {
			if (TypeRapportEntreTiersWeb.FILIATION != modalite) {
				final TypeRapportEntreTiers modaliteCore = modalite.toCore();
				Assert.assertNotNull(modaliteCore);
			}
		}
	}
}
