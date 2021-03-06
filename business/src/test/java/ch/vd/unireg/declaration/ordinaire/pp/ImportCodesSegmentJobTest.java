package ch.vd.unireg.declaration.ordinaire.pp;

import java.util.List;

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.common.LoggingStatusManager;

public class ImportCodesSegmentJobTest extends BusinessTest {

	private static final String ENCODING = "ISO-8859-1";
	private static final Logger LOGGER = LoggerFactory.getLogger(ImportCodesSegmentJobTest.class);

	@Test
	public void testNullInputFileParsing() throws Exception {
		final List<ContribuableAvecCodeSegment> liste = ImportCodesSegmentJob.buildDataFromInputFile(null, new LoggingStatusManager(LOGGER), null, audit);
		Assert.assertNotNull(liste);
		Assert.assertEquals(0, liste.size());
	}

	@Test
	public void testEmptyInputFileParsing() throws Exception {
		final List<ContribuableAvecCodeSegment> liste = ImportCodesSegmentJob.buildDataFromInputFile(new byte[0], new LoggingStatusManager(LOGGER), null, audit);
		Assert.assertNotNull(liste);
		Assert.assertEquals(0, liste.size());
	}

	@Test
	public void testBasicInputFileParsing() throws Exception {
		final String content = "NO_CTB;CODE_SEGMENT\n10010010;2\n10010011;5";
		final List<ContribuableAvecCodeSegment> liste = ImportCodesSegmentJob.buildDataFromInputFile(content.getBytes(ENCODING), new LoggingStatusManager(LOGGER), null, audit);
		Assert.assertNotNull(liste);
		Assert.assertEquals(2, liste.size());

		Assert.assertEquals(10010010L, liste.get(0).getNoContribuable());
		Assert.assertEquals(2, liste.get(0).getCodeSegment());
		Assert.assertEquals(10010011L, liste.get(1).getNoContribuable());
		Assert.assertEquals(5, liste.get(1).getCodeSegment());
	}

	@Test
	public void testInputFileParsingWithMoreThanTwoColumns() throws Exception {
		final String content = "NO_CTB;CODE_SEGMENT;TRALALA\n10010010;2;dghjuze\n10010011;5;261\n10010001;65;";
		final List<ContribuableAvecCodeSegment> liste = ImportCodesSegmentJob.buildDataFromInputFile(content.getBytes(ENCODING), new LoggingStatusManager(LOGGER), null, audit);
		Assert.assertNotNull(liste);
		Assert.assertEquals(3, liste.size());

		Assert.assertEquals(10010001L, liste.get(0).getNoContribuable());
		Assert.assertEquals(65, liste.get(0).getCodeSegment());
		Assert.assertEquals(10010010L, liste.get(1).getNoContribuable());
		Assert.assertEquals(2, liste.get(1).getCodeSegment());
		Assert.assertEquals(10010011L, liste.get(2).getNoContribuable());
		Assert.assertEquals(5, liste.get(2).getCodeSegment());
	}

	@Test
	public void testInputFileParsingWithInvalidLines() throws Exception {
		final String content = "NO_CTB;CODE_SEGMENT\n10010010;2d\n10010011d;5\nTOTO;12353;3";
		final List<ContribuableAvecCodeSegment> liste = ImportCodesSegmentJob.buildDataFromInputFile(content.getBytes(ENCODING), new LoggingStatusManager(LOGGER), null, audit);
		Assert.assertNotNull(liste);
		Assert.assertEquals(0, liste.size());
	}

	@Test
	public void testInputFileAvecDoublons() throws Exception {
		final String content = "NO_CTB;CODE_SEGMENT\n10010010;2\n10010011;5\n10010011;5";
		final MutableInt nbLignesLues = new MutableInt(0);
		final List<ContribuableAvecCodeSegment> liste = ImportCodesSegmentJob.buildDataFromInputFile(content.getBytes(ENCODING), new LoggingStatusManager(LOGGER), nbLignesLues, audit);
		Assert.assertNotNull(liste);
		Assert.assertEquals(3, nbLignesLues.intValue());
		Assert.assertEquals(2, liste.size());
	}
}
