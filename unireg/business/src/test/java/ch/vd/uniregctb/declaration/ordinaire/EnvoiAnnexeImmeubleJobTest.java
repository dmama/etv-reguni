package ch.vd.uniregctb.declaration.ordinaire;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.LoggingStatusManager;

public class EnvoiAnnexeImmeubleJobTest extends BusinessTest {

	private static final String ENCODING = "ISO-8859-1";
	private static final Logger LOGGER = Logger.getLogger(EnvoiAnnexeImmeubleJobTest.class);

	@Test
	public void testNullInputFileParsing() throws Exception {
		final List<ContribuableAvecImmeuble> liste = EnvoiAnnexeImmeubleJob.extractCtbFromCSV(null, new LoggingStatusManager(LOGGER));
		Assert.assertNotNull(liste);
		Assert.assertEquals(0, liste.size());
	}

	@Test
	public void testEmptyInputFileParsing() throws Exception {
		final List<ContribuableAvecImmeuble> liste = EnvoiAnnexeImmeubleJob.extractCtbFromCSV(new byte[0], new LoggingStatusManager(LOGGER));
		Assert.assertNotNull(liste);
		Assert.assertEquals(0, liste.size());
	}

	@Test
	public void testBasicInputFileParsing() throws Exception {
		final String content = "NO_CTB;NB_IMMEUBLES\n10010010;2\n10010011;5";
		final List<ContribuableAvecImmeuble> liste = EnvoiAnnexeImmeubleJob.extractCtbFromCSV(content.getBytes(ENCODING), new LoggingStatusManager(LOGGER));
		Assert.assertNotNull(liste);
		Assert.assertEquals(2, liste.size());

		Assert.assertEquals(10010010L, liste.get(0).getNumeroContribuable());
		Assert.assertEquals(2, liste.get(0).getNombreImmeuble());
		Assert.assertEquals(10010011L, liste.get(1).getNumeroContribuable());
		Assert.assertEquals(5, liste.get(1).getNombreImmeuble());
	}

	@Test
	public void testInputFileParsingWithMoreThanTwoColumns() throws Exception {
		final String content = "NO_CTB;NB_IMMEUBLES;TRALALA\n10010010;2;dghjuze\n10010011;5;261\n10010001;65;";
		final List<ContribuableAvecImmeuble> liste = EnvoiAnnexeImmeubleJob.extractCtbFromCSV(content.getBytes(ENCODING), new LoggingStatusManager(LOGGER));
		Assert.assertNotNull(liste);
		Assert.assertEquals(3, liste.size());

		Assert.assertEquals(10010001L, liste.get(0).getNumeroContribuable());
		Assert.assertEquals(65, liste.get(0).getNombreImmeuble());
		Assert.assertEquals(10010010L, liste.get(1).getNumeroContribuable());
		Assert.assertEquals(2, liste.get(1).getNombreImmeuble());
		Assert.assertEquals(10010011L, liste.get(2).getNumeroContribuable());
		Assert.assertEquals(5, liste.get(2).getNombreImmeuble());
	}

	@Test
	public void testInputFileParsingWithInvalidLines() throws Exception {
		final String content = "NO_CTB;NB_IMMEUBLES\n10010010;2d\n10010011d;5\nTOTO;12353;3";
		final List<ContribuableAvecImmeuble> liste = EnvoiAnnexeImmeubleJob.extractCtbFromCSV(content.getBytes(ENCODING), new LoggingStatusManager(LOGGER));
		Assert.assertNotNull(liste);
		Assert.assertEquals(0, liste.size());
	}
}
