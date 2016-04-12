package ch.vd.uniregctb.migration.pm.engine;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;

public class AppariementsMultiplesManagerTest {

	@Test
	public void testChargementFichier() throws Exception {
		final String contenuFichier = "NO_CANTONAL;NB_ENTREPRISES;NOS_ENTREPRISE\n" +
				"1;1;42\n" +
				"2;1;26,32\n" +         // cette ligne ne sera pas prise en compte car la deuxième colonne est "1"
				"3;2;21,56\n" +
				"7;4;12,45,67,90\n" +
				"10;1;3\n";

		final AppariementsMultiplesManager manager;
		try (ByteArrayInputStream bais = new ByteArrayInputStream(contenuFichier.getBytes())) {
			manager = new AppariementsMultiplesManagerImpl(bais);
		}

		Assert.assertEquals(Collections.emptySet(), manager.getIdentifiantsEntreprisesAvecMemeAppariement(1));
		Assert.assertEquals(Collections.emptySet(), manager.getIdentifiantsEntreprisesAvecMemeAppariement(2));
		Assert.assertEquals(new HashSet<>(Arrays.asList(21L, 56L)), manager.getIdentifiantsEntreprisesAvecMemeAppariement(3));
		Assert.assertEquals(new HashSet<>(Arrays.asList(12L, 45L, 67L, 90L)), manager.getIdentifiantsEntreprisesAvecMemeAppariement(7));
		Assert.assertEquals(Collections.emptySet(), manager.getIdentifiantsEntreprisesAvecMemeAppariement(8));
		Assert.assertEquals(Collections.emptySet(), manager.getIdentifiantsEntreprisesAvecMemeAppariement(10));
	}
}
