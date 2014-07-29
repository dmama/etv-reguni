package ch.vd.uniregctb.tiers;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.common.WebTestSpring3;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.PeriodiciteDecompte;

public class DebiteurEditControllerTest extends WebTestSpring3 {

	private TiersMapHelper tiersMapHelper;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		this.tiersMapHelper = getBean(TiersMapHelper.class, "tiersMapHelper");
	}

	private static void assertPeriodicitesAutorisees(String message, EnumSet<PeriodiciteDecompte> expected, Map<PeriodiciteDecompte, String> foundMap) {
		Assert.assertNotNull(foundMap);

		final Set<PeriodiciteDecompte> found = EnumSet.noneOf(PeriodiciteDecompte.class);
		found.addAll(foundMap.keySet());

		Assert.assertEquals(message, expected, found);
	}

	private void assertPeriodicitesAutorisees(String message,
	                                          EnumSet<PeriodiciteDecompte> expected,
	                                          @Nullable PeriodiciteDecompte periodiciteCourante,
	                                          CategorieImpotSource cis,
	                                          boolean fullRights) {
		final Map<PeriodiciteDecompte, String> map = DebiteurEditController.getMapPeriodicites(periodiciteCourante, cis, fullRights, tiersMapHelper);
		assertPeriodicitesAutorisees(message, expected, map);
	}

	@Test
	public void testGetMapPeriodicitesModeCreation() throws Exception {

		// omnipotent
		for (CategorieImpotSource cis : CategorieImpotSource.values()) {
			assertPeriodicitesAutorisees(cis.name(), EnumSet.allOf(PeriodiciteDecompte.class), null, cis, true);
		}

		// non-omnipotent
		for (CategorieImpotSource cis : CategorieImpotSource.values()) {
			final EnumSet<PeriodiciteDecompte> expected;
			if (cis == CategorieImpotSource.REGULIERS) {
				expected = EnumSet.of(PeriodiciteDecompte.MENSUEL, PeriodiciteDecompte.ANNUEL);
			}
			else {
				expected = EnumSet.allOf(PeriodiciteDecompte.class);
			}
			assertPeriodicitesAutorisees(cis.name(), expected, null, cis, false);
		}
	}

	@Test
	public void testGetMapPeriodicitesModeEditionOmnipotent() throws Exception {
		// omnipotent -> on peut toujours tout choisir
		for (PeriodiciteDecompte oldPeriodicite : PeriodiciteDecompte.values()) {
			for (CategorieImpotSource cis : CategorieImpotSource.values()) {
				assertPeriodicitesAutorisees(String.format("%s/%s", oldPeriodicite, cis), EnumSet.allOf(PeriodiciteDecompte.class), oldPeriodicite, cis, true);
			}
		}
	}

	@Test
	public void testGetMapPeriodicitesModeEditionNonOmnipotentReguliers() throws Exception {
		assertPeriodicitesAutorisees(null, EnumSet.of(PeriodiciteDecompte.MENSUEL, PeriodiciteDecompte.ANNUEL), PeriodiciteDecompte.ANNUEL, CategorieImpotSource.REGULIERS, false);
		assertPeriodicitesAutorisees(null, EnumSet.of(PeriodiciteDecompte.MENSUEL), PeriodiciteDecompte.MENSUEL, CategorieImpotSource.REGULIERS, false);
		assertPeriodicitesAutorisees(null, EnumSet.of(PeriodiciteDecompte.MENSUEL, PeriodiciteDecompte.SEMESTRIEL), PeriodiciteDecompte.SEMESTRIEL, CategorieImpotSource.REGULIERS, false);
		assertPeriodicitesAutorisees(null, EnumSet.of(PeriodiciteDecompte.MENSUEL, PeriodiciteDecompte.TRIMESTRIEL), PeriodiciteDecompte.TRIMESTRIEL, CategorieImpotSource.REGULIERS, false);
		assertPeriodicitesAutorisees(null, EnumSet.of(PeriodiciteDecompte.MENSUEL, PeriodiciteDecompte.UNIQUE), PeriodiciteDecompte.UNIQUE, CategorieImpotSource.REGULIERS, false);
	}

	@Test
	public void testGetMapPeriodicitesModeEditionNonOmnipotentNonReguliers() throws Exception {
		for (CategorieImpotSource cis : EnumSet.complementOf(EnumSet.of(CategorieImpotSource.REGULIERS))) {
			assertPeriodicitesAutorisees(cis.name(), EnumSet.of(PeriodiciteDecompte.MENSUEL, PeriodiciteDecompte.TRIMESTRIEL, PeriodiciteDecompte.SEMESTRIEL, PeriodiciteDecompte.ANNUEL, PeriodiciteDecompte.UNIQUE), PeriodiciteDecompte.ANNUEL, cis, false);
			assertPeriodicitesAutorisees(cis.name(), EnumSet.of(PeriodiciteDecompte.MENSUEL, PeriodiciteDecompte.UNIQUE), PeriodiciteDecompte.MENSUEL, cis, false);
			assertPeriodicitesAutorisees(cis.name(), EnumSet.of(PeriodiciteDecompte.MENSUEL, PeriodiciteDecompte.TRIMESTRIEL, PeriodiciteDecompte.SEMESTRIEL, PeriodiciteDecompte.UNIQUE), PeriodiciteDecompte.SEMESTRIEL, cis, false);
			assertPeriodicitesAutorisees(cis.name(), EnumSet.of(PeriodiciteDecompte.MENSUEL, PeriodiciteDecompte.TRIMESTRIEL, PeriodiciteDecompte.UNIQUE), PeriodiciteDecompte.TRIMESTRIEL, cis, false);
			assertPeriodicitesAutorisees(cis.name(), EnumSet.of(PeriodiciteDecompte.MENSUEL, PeriodiciteDecompte.UNIQUE), PeriodiciteDecompte.UNIQUE, cis, false);
		}
	}
}