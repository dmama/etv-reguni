package ch.vd.uniregctb.hibernate.meta;

import java.util.List;

import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.rf.Immeuble;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MetaEntityTest extends WithoutSpringTest {

	@Test
	public void testDetermineEmbeddedElements() throws Exception {

		final MetaEntity entity = MetaEntity.determine(Immeuble.class);
		assertNotNull(entity);

		final List<Property> properties = entity.getProperties();
		assertNotNull(properties);

		final Property partProprieteNumerator = entity.getProperty("partPropriete.numerateur");
		assertNotNull(partProprieteNumerator);
		assertEquals("partPropriete.numerateur", partProprieteNumerator.getName());
		assertTrue(partProprieteNumerator.getType() instanceof IntegerPropertyType);
		assertEquals("PART_PROPRIETE_NUMERATEUR", partProprieteNumerator.getColumnName());

		final Property partProprieteDenominateur = entity.getProperty("partPropriete.denominateur");
		assertNotNull(partProprieteDenominateur);
		assertEquals("partPropriete.denominateur", partProprieteDenominateur.getName());
		assertTrue(partProprieteDenominateur.getType() instanceof IntegerPropertyType);
		assertEquals("PART_PROPRIETE_DENOMINATEUR", partProprieteDenominateur.getColumnName());

		final Property proprietaireId = entity.getProperty("proprietaire.id");
		assertNotNull(proprietaireId);
		assertEquals("proprietaire.id", proprietaireId.getName());
		assertTrue(proprietaireId.getType() instanceof StringPropertyType);
		assertEquals("ID_PROPRIETAIRE_RF", proprietaireId.getColumnName());

		final Property proprietaireNumeroIndividu = entity.getProperty("proprietaire.numeroIndividu");
		assertNotNull(proprietaireNumeroIndividu);
		assertEquals("proprietaire.numeroIndividu", proprietaireNumeroIndividu.getName());
		assertTrue(proprietaireNumeroIndividu.getType() instanceof LongPropertyType);
		assertEquals("NUMERO_INDIVIDU_RF", proprietaireNumeroIndividu.getColumnName());
	}
}
