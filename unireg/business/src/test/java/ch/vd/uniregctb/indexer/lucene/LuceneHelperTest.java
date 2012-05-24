package ch.vd.uniregctb.indexer.lucene;

import org.apache.lucene.search.Query;
import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.indexer.lucene.LuceneHelper;

import static org.junit.Assert.assertNull;

public class LuceneHelperTest extends WithoutSpringTest {

	/**
	 * [UNIREG-2715] Vérifie que la méthode getTermsContient n'asserte pas lorsqu'on lui demande de créé une query sur une valeur inférieur à la limite spécifiée.
	 */
	@Test
	public void testGetTermsContient() {
		final Query query = LuceneHelper.getTermsContient("some field", "bla", 4);
		assertNull(query); // "bla" ne contient que 3 caractères et le nombre minimal est 4 => null
	}
}
