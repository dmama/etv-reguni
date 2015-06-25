package ch.vd.uniregctb.migration.pm.engine.log;

import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.migration.pm.log.LogCategory;

public class LogStructureTest {

	@Test
	public void testCompletudeConfiguration() {
		// vérification que toutes les catégories ont été prises en compte dans la map
		Stream.of(LogCategory.values())
				.filter(cat -> LogStructure.STRUCTURES_CONTEXTES.get(cat) == null)
				.findAny()
				.ifPresent(cat -> Assert.fail("La valeur de la catégorie " + cat + " n'est pas prise en compte dans la map des structures de contexte disponibles"));
	}
}
