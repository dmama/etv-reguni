package ch.vd.uniregctb.registrefoncier;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.LoggingStatusManager;

public class RapprocherCtbRegistreFoncierJobTest extends BusinessTest {

	private static final String ENCODING = "ISO-8859-1";
	private static final Logger LOGGER = Logger.getLogger(RapprocherCtbRegistreFoncierJobTest.class);

	@Test
	public void testExtractFromNullCsv() throws Exception {
		final List<ProprietaireFoncier> proprios = RapprocherCtbRegistreFoncierJob.extractProprioFromCSV(null, new LoggingStatusManager(LOGGER));
		Assert.assertNotNull(proprios);
		Assert.assertEquals(0, proprios.size());
	}

	@Test
	public void testExtractFromEmptyCsv() throws Exception {
		final List<ProprietaireFoncier> proprios = RapprocherCtbRegistreFoncierJob.extractProprioFromCSV(new byte[0], new LoggingStatusManager(LOGGER));
		Assert.assertNotNull(proprios);
		Assert.assertEquals(0, proprios.size());
	}

	@Test
	public void testSimpleExtract() throws Exception {
		final byte[] csv = "1234;Tartempion;Amédée;12.4.1976;10000421".getBytes(ENCODING);
		final List<ProprietaireFoncier> proprios = RapprocherCtbRegistreFoncierJob.extractProprioFromCSV(csv, new LoggingStatusManager(LOGGER));
		Assert.assertNotNull(proprios);
		Assert.assertEquals(1, proprios.size());

		final ProprietaireFoncier p = proprios.get(0);
		Assert.assertNotNull(p);
		Assert.assertEquals(1234L, p.getNumeroRegistreFoncier());
		Assert.assertEquals("Tartempion", p.getNom());
		Assert.assertEquals("Amédée", p.getPrenom());
		Assert.assertEquals(date(1976, 4, 12), p.getDateNaissance());
		Assert.assertEquals(10000421L, (long) p.getNumeroContribuable());
	}

	@Test
	public void testExtractWithoutNoCtb() throws Exception {
		final byte[] csv = "1234;Tartempion;Amédée;12.4.1976;".getBytes(ENCODING);
		final List<ProprietaireFoncier> proprios = RapprocherCtbRegistreFoncierJob.extractProprioFromCSV(csv, new LoggingStatusManager(LOGGER));
		Assert.assertNotNull(proprios);
		Assert.assertEquals(1, proprios.size());

		final ProprietaireFoncier p = proprios.get(0);
		Assert.assertNotNull(p);
		Assert.assertEquals(1234L, p.getNumeroRegistreFoncier());
		Assert.assertEquals("Tartempion", p.getNom());
		Assert.assertEquals("Amédée", p.getPrenom());
		Assert.assertEquals(date(1976, 4, 12), p.getDateNaissance());
		Assert.assertNull(p.getNumeroContribuable());
	}

	@Test
	public void testExtractWithInvalidDateOfBirth() throws Exception {
		final byte[] csv = "1234;Tartempion;Amédée;12.4.1076;12345678".getBytes(ENCODING);
		final List<ProprietaireFoncier> proprios = RapprocherCtbRegistreFoncierJob.extractProprioFromCSV(csv, new LoggingStatusManager(LOGGER));
		Assert.assertNotNull(proprios);
		Assert.assertEquals(1, proprios.size());

		final ProprietaireFoncier p = proprios.get(0);
		Assert.assertNotNull(p);
		Assert.assertEquals(1234L, p.getNumeroRegistreFoncier());
		Assert.assertEquals("Tartempion", p.getNom());
		Assert.assertEquals("Amédée", p.getPrenom());
		Assert.assertNull(p.getDateNaissance());
		Assert.assertEquals(12345678L, (long) p.getNumeroContribuable());
	}

	@Test
	public void testMultilineExtract() throws Exception {
		final byte[] csv = "1234;Tartempion;Amédée;12.4.1976;10000001\n1235;Tartempion;Gribouille;12.5.1977;10000002\n1233;Tartempion;Anatole;13.1.1932;10000003".getBytes(ENCODING);
		final List<ProprietaireFoncier> proprios = RapprocherCtbRegistreFoncierJob.extractProprioFromCSV(csv, new LoggingStatusManager(LOGGER));
		Assert.assertNotNull(proprios);
		Assert.assertEquals(3, proprios.size());

		{
			final ProprietaireFoncier p = proprios.get(0);
			Assert.assertNotNull(p);
			Assert.assertEquals(1233L, p.getNumeroRegistreFoncier());
			Assert.assertEquals("Tartempion", p.getNom());
			Assert.assertEquals("Anatole", p.getPrenom());
			Assert.assertEquals(date(1932, 1, 13), p.getDateNaissance());
			Assert.assertEquals(10000003L, (long) p.getNumeroContribuable());
		}
		{
			final ProprietaireFoncier p = proprios.get(1);
			Assert.assertNotNull(p);
			Assert.assertEquals(1234L, p.getNumeroRegistreFoncier());
			Assert.assertEquals("Tartempion", p.getNom());
			Assert.assertEquals("Amédée", p.getPrenom());
			Assert.assertEquals(date(1976, 4, 12), p.getDateNaissance());
			Assert.assertEquals(10000001L, (long) p.getNumeroContribuable());
		}
		{
			final ProprietaireFoncier p = proprios.get(2);
			Assert.assertNotNull(p);
			Assert.assertEquals(1235L, p.getNumeroRegistreFoncier());
			Assert.assertEquals("Tartempion", p.getNom());
			Assert.assertEquals("Gribouille", p.getPrenom());
			Assert.assertEquals(date(1977, 5, 12), p.getDateNaissance());
			Assert.assertEquals(10000002L, (long) p.getNumeroContribuable());
		}
	}

	@Test
	public void testInvalidLineExtract() throws Exception {
		final byte[] csv = "2781367;6e67367236268gdvs;toto".getBytes(ENCODING);
		final List<ProprietaireFoncier> proprios = RapprocherCtbRegistreFoncierJob.extractProprioFromCSV(csv, new LoggingStatusManager(LOGGER));
		Assert.assertNotNull(proprios);
		Assert.assertEquals(0, proprios.size());
	}

	@Test
	public void testInvalidLineInTheMiddleOfFineLinesExtract() throws Exception {
		final byte[] csv = "1234;Tartempion;Amédée;12.4.1976;10000001\n2781367;6e67367236268gdvs;toto\n1235;Tartempion;Gribouille;12.5.1977;10000002\n1233;Tartempion;Anatole;13.1.1932;10000003".getBytes(ENCODING);
		final List<ProprietaireFoncier> proprios = RapprocherCtbRegistreFoncierJob.extractProprioFromCSV(csv, new LoggingStatusManager(LOGGER));
		Assert.assertNotNull(proprios);
		Assert.assertEquals(3, proprios.size());

		{
			final ProprietaireFoncier p = proprios.get(0);
			Assert.assertNotNull(p);
			Assert.assertEquals(1233L, p.getNumeroRegistreFoncier());
			Assert.assertEquals("Tartempion", p.getNom());
			Assert.assertEquals("Anatole", p.getPrenom());
			Assert.assertEquals(date(1932, 1, 13), p.getDateNaissance());
			Assert.assertEquals(10000003L, (long) p.getNumeroContribuable());
		}
		{
			final ProprietaireFoncier p = proprios.get(1);
			Assert.assertNotNull(p);
			Assert.assertEquals(1234L, p.getNumeroRegistreFoncier());
			Assert.assertEquals("Tartempion", p.getNom());
			Assert.assertEquals("Amédée", p.getPrenom());
			Assert.assertEquals(date(1976, 4, 12), p.getDateNaissance());
			Assert.assertEquals(10000001L, (long) p.getNumeroContribuable());
		}
		{
			final ProprietaireFoncier p = proprios.get(2);
			Assert.assertNotNull(p);
			Assert.assertEquals(1235L, p.getNumeroRegistreFoncier());
			Assert.assertEquals("Tartempion", p.getNom());
			Assert.assertEquals("Gribouille", p.getPrenom());
			Assert.assertEquals(date(1977, 5, 12), p.getDateNaissance());
			Assert.assertEquals(10000002L, (long) p.getNumeroContribuable());
		}
	}
}
