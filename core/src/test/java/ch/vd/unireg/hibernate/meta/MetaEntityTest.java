package ch.vd.unireg.hibernate.meta;

import java.util.List;

import org.junit.Test;

import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.etiquette.Etiquette;
import ch.vd.unireg.foncier.DegrevementICI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MetaEntityTest extends WithoutSpringTest {

	@Test
	public void testDetermineEmbeddedElements() throws Exception {

		final MetaEntity entity = MetaEntity.determine(DegrevementICI.class);
		assertNotNull(entity);

		final List<Property> properties = entity.getProperties();
		assertNotNull(properties);

		final Property locationRevenu = entity.getProperty("location.revenu");
		assertNotNull(locationRevenu);
		assertEquals("location.revenu", locationRevenu.getName());
		assertTrue(locationRevenu.getType() instanceof LongPropertyType);
		assertEquals("DEG_LOC_REVENU", locationRevenu.getColumnName());

		final Property locationPourcentage = entity.getProperty("location.pourcentage");
		assertNotNull(locationPourcentage);
		assertEquals("location.pourcentage", locationPourcentage.getName());
		assertTrue(locationPourcentage.getType() instanceof BigDecimalPropertyType);
		assertEquals("DEG_LOC_POURCENT", locationPourcentage.getColumnName());
	}

	/**
	 * [SIFISC-28363] Vérifie que les @TypeDef sont bien compris au niveau des entités Hibernate.
	 */
	@Test
	public void testDetermineTypesHibernateAliases() throws Exception {

		final MetaEntity entity = MetaEntity.determine(Etiquette.class);
		assertNotNull(entity);

		final Property actionSurDeces = entity.getProperty("actionSurDeces");
		assertNotNull(actionSurDeces);
		assertEquals("actionSurDeces", actionSurDeces.getName());
		assertTrue(actionSurDeces.getType() instanceof ActionAutoEtiquetteUserTypePropertyType);
		assertEquals("AUTO_DECES", actionSurDeces.getColumnName());
	}
}
