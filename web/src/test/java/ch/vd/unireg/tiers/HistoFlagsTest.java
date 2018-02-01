package ch.vd.unireg.tiers;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.common.WithoutSpringTest;

public class HistoFlagsTest extends WithoutSpringTest {

	private static void checkFlags(Set<HistoFlag> expected, HistoFlags found) {
		for (HistoFlag flag : HistoFlag.values()) {
			Assert.assertEquals(flag.name(), expected.contains(flag), found.hasHistoFlag(flag));
		}
	}

	@Test
	public void testTousParametresVides() throws Exception {
		final HistoFlags flags = new HistoFlags(name -> null);
		checkFlags(Collections.emptySet(), flags);
	}

	@Test
	public void testTousParametresFaux() throws Exception {
		final HistoFlags flags = new HistoFlags(name -> "false");
		checkFlags(Collections.emptySet(), flags);
	}

	@Test
	public void testTousParametresVrais() throws Exception {
		final HistoFlags flags = new HistoFlags(name -> "true");
		checkFlags(EnumSet.allOf(HistoFlag.class), flags);
	}

	@Test
	public void testParametresAleatoires() throws Exception {

		// remplissage
		final Random rnd = new Random();
		final Set<HistoFlag> expected = EnumSet.noneOf(HistoFlag.class);
		for (HistoFlag candidate : HistoFlag.values()) {
			if (rnd.nextBoolean()) {
				expected.add(candidate);
			}
		}

		// fonction de mapping
		final Function<String, String> extractor = paramName -> {
			final HistoFlag flag = HistoFlag.fromName(paramName);
			if (flag != null && expected.contains(flag)) {
				return "true";
			}
			return "false";
		};

		// test
		checkFlags(expected, new HistoFlags(extractor));
	}
}
