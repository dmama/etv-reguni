package ch.vd.uniregctb.registrefoncier;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.util.ResourceUtils;

import ch.vd.capitastra.rechteregister.Dienstbarkeit;
import ch.vd.uniregctb.common.UniregJUnit4Runner;
import ch.vd.uniregctb.registrefoncier.elements.XmlHelperRFImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(UniregJUnit4Runner.class)
public class FichierUsufruitiersRFParserTest {

	private FichierUsufruitiersRFParser parser;

	@Before
	public void setUp() throws Exception {
		parser = new FichierUsufruitiersRFParser();
		parser.setXmlHelperRF(new XmlHelperRFImpl());
	}

	@Test
	public void testParseDroits() throws Exception {

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/export_usufruitiers_rf_hebdo.xml");
		assertNotNull(file);

		// on parse le fichier
		final TestCallback callback = new TestCallback();
		try (InputStream is = new FileInputStream(file)) {
			parser.processFile(is, callback);
		}

		// on s'assure que les droits ont bien été parsés
		final List<Dienstbarkeit> droits = callback.getDroits();
		assertEquals(2, droits.size());

		final Dienstbarkeit droit0 = droits.get(0);
		{
			assertNotNull(droit0);
			assertEquals("_1f109152380ffd8901380ffec2506c02", droit0.getStandardRechtID());

			final List<String> immeubles = droit0.getBeteiligtesGrundstueckIDREF();
			assertNotNull(immeubles);
			assertEquals(1, immeubles.size());
			assertEquals("_1f109152380ffd8901380ffe0d893e41", immeubles.get(0));

			assertEquals("Droit d'habitation", droit0.getStichwort().getTextFr());
		}

		final Dienstbarkeit droit1 = droits.get(1);
		{
			assertNotNull(droit1);
			assertEquals("_1f109152380ffd8901380ffefad54360", droit1.getStandardRechtID());

			final List<String> immeubles = droit1.getBeteiligtesGrundstueckIDREF();
			assertNotNull(immeubles);
			assertEquals(1, immeubles.size());
			assertEquals("_1f109152380ffd8901380ffe090827e1", immeubles.get(0));

			assertEquals("Usufruit", droit1.getStichwort().getTextFr());
		}
	}

	private static class TestCallback implements FichierUsufruitiersRFParser.Callback {

		private final List<Dienstbarkeit> droits = new ArrayList<>();

		@Override
		public void onDroit(@NotNull Dienstbarkeit droit) {
			droits.add(droit);
		}

		public List<Dienstbarkeit> getDroits() {
			return droits;
		}
	}
}