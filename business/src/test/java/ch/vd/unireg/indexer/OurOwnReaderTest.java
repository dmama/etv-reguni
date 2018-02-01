package ch.vd.uniregctb.indexer;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

public class OurOwnReaderTest extends WithoutSpringTest {

	private void assertReadValue(String expected, String source) throws Exception {
		try (Reader reader = new StringReader(source);
		     Reader ownReader = new OurOwnReader(reader);
		     Writer writer = new StringWriter(source.length())) {

			IOUtils.copy(ownReader, writer);
			Assert.assertEquals(expected, writer.toString());
		}
	}

	@Test
	public void testSansPoint() throws Exception {
		assertReadValue("572 4z89czip  4z5 v7 qcrzq", "572 4z89czip  4z5 v7 qcrzq");
	}

	@Test
	public void testAvecPointSuiviEspace() throws Exception {
		assertReadValue("572 4z89czip.  4z5 v7. qcrzq", "572 4z89czip.  4z5 v7. qcrzq");
	}

	@Test
	public void testAvecPointNonSuiviEspaceMaisChiffres() throws Exception {
		assertReadValue("453789.1451.1548510.484512", "453789.1451.1548510.484512");
		assertReadValue("1.1", "1.1");
	}

	@Test
	public void testAvecPointNonSuiviEspaceMaisLettres() throws Exception {
		assertReadValue("bflwgfzag agfzwgfazdwaw gzur23sahgf", "bflwgfzag.agfzwgfazdwaw.gzur23sahgf");
	}

	@Test
	public void testAvecPointNonSuiviEspaceLettresSeules() throws Exception {
		assertReadValue("bflwgfzag sos agfzwgfazdwaw gzur23sahgf", "bflwgfzag.s.o.s.agfzwgfazdwaw.gzur23sahgf");
		assertReadValue("1 centre sos", "1.centre.s.o.s");
	}

	@Test
	public void testPointFinal() throws Exception {
		assertReadValue("bflwgfzag.", "bflwgfzag.");
	}

	@Test
	public void testPointInitial() throws Exception {
		assertReadValue(" bflwgfzag", ".bflwgfzag");
		assertReadValue(".314159", ".314159");
	}
}
