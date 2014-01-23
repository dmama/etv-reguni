package ch.vd.uniregctb.common;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.civil.mock.MockNationalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;

public class NationaliteHelperTest extends WithoutSpringTest {

	@Test
	public void testValidAt() throws Exception {

		final MockNationalite espagne = new MockNationalite(date(2000, 1, 1), date(2005, 12, 31), MockPays.Espagne);
		final MockNationalite france = new MockNationalite(date(2003, 1, 1), null, MockPays.France);
		final MockNationalite suisse = new MockNationalite(date(2004, 1, 1), date(2007, 12, 31), MockPays.Suisse);
		final List<Nationalite> all = Arrays.<Nationalite>asList(espagne, france, suisse);

		{
			final List<Nationalite> validAt = NationaliteHelper.validAt(null, date(1999, 12, 31));
			Assert.assertNotNull(validAt);
			Assert.assertEquals(0, validAt.size());
		}
		{
			final List<Nationalite> validAt = NationaliteHelper.validAt(Collections.<Nationalite>emptyList(), date(1999, 12, 31));
			Assert.assertNotNull(validAt);
			Assert.assertEquals(0, validAt.size());
		}
		{
			final List<Nationalite> validAt = NationaliteHelper.validAt(all, date(1999, 12, 31));
			Assert.assertNotNull(validAt);
			Assert.assertEquals(0, validAt.size());
		}
		{
			final List<Nationalite> validAt = NationaliteHelper.validAt(all, date(2000, 12, 31));
			Assert.assertNotNull(validAt);
			Assert.assertEquals(1, validAt.size());
			Assert.assertSame(espagne, validAt.get(0));
		}
		{
			final List<Nationalite> validAt = NationaliteHelper.validAt(all, date(2003, 12, 31));
			Assert.assertNotNull(validAt);
			Assert.assertEquals(2, validAt.size());
			Assert.assertSame(espagne, validAt.get(0));
			Assert.assertSame(france, validAt.get(1));
		}
		{
			final List<Nationalite> validAt = NationaliteHelper.validAt(all, date(2004, 12, 31));
			Assert.assertNotNull(validAt);
			Assert.assertEquals(3, validAt.size());
			Assert.assertSame(espagne, validAt.get(0));
			Assert.assertSame(france, validAt.get(1));
			Assert.assertSame(suisse, validAt.get(2));
		}
		{
			final List<Nationalite> validAt = NationaliteHelper.validAt(all, date(2005, 12, 31));
			Assert.assertNotNull(validAt);
			Assert.assertEquals(3, validAt.size());
			Assert.assertSame(espagne, validAt.get(0));
			Assert.assertSame(france, validAt.get(1));
			Assert.assertSame(suisse, validAt.get(2));
		}
		{
			final List<Nationalite> validAt = NationaliteHelper.validAt(all, date(2006, 12, 31));
			Assert.assertNotNull(validAt);
			Assert.assertEquals(2, validAt.size());
			Assert.assertSame(france, validAt.get(0));
			Assert.assertSame(suisse, validAt.get(1));
		}
		{
			final List<Nationalite> validAt = NationaliteHelper.validAt(all, date(2007, 12, 31));
			Assert.assertNotNull(validAt);
			Assert.assertEquals(2, validAt.size());
			Assert.assertSame(france, validAt.get(0));
			Assert.assertSame(suisse, validAt.get(1));
		}
		{
			final List<Nationalite> validAt = NationaliteHelper.validAt(all, date(2008, 1, 1));
			Assert.assertNotNull(validAt);
			Assert.assertEquals(1, validAt.size());
			Assert.assertSame(france, validAt.get(0));
		}
		{
			final List<Nationalite> validAt = NationaliteHelper.validAt(all, null);
			Assert.assertNotNull(validAt);
			Assert.assertEquals(1, validAt.size());
			Assert.assertSame(france, validAt.get(0));
		}
	}

	@Test
	public void testRefAt() throws Exception {

		final MockNationalite espagne = new MockNationalite(date(2000, 1, 1), date(2005, 12, 31), MockPays.Espagne);
		final MockNationalite france = new MockNationalite(date(2003, 1, 1), null, MockPays.France);
		final MockNationalite suisse = new MockNationalite(date(2004, 1, 1), date(2007, 12, 31), MockPays.Suisse);
		final List<Nationalite> all = Arrays.<Nationalite>asList(espagne, france, suisse);

		Assert.assertNull(NationaliteHelper.refAt(null, date(1999, 12, 31)));
		Assert.assertNull(NationaliteHelper.refAt(Collections.<Nationalite>emptyList(), date(1999, 12, 31)));
		Assert.assertNull(NationaliteHelper.refAt(all, date(1999, 12, 31)));
		Assert.assertSame(espagne, NationaliteHelper.refAt(all, date(2000, 12, 31)));
		Assert.assertSame(espagne, NationaliteHelper.refAt(all, date(2003, 12, 31)));
		Assert.assertSame(suisse, NationaliteHelper.refAt(all, date(2004, 12, 31)));
		Assert.assertSame(suisse, NationaliteHelper.refAt(all, date(2005, 12, 31)));
		Assert.assertSame(suisse, NationaliteHelper.refAt(all, date(2006, 12, 31)));
		Assert.assertSame(suisse, NationaliteHelper.refAt(all, date(2007, 12, 31)));
		Assert.assertSame(france, NationaliteHelper.refAt(all, date(2008, 1, 1)));
		Assert.assertSame(france, NationaliteHelper.refAt(all, null));
	}

	@Test
	public void testIsSuisseAt() throws Exception {

		final MockNationalite espagne = new MockNationalite(date(2000, 1, 1), date(2005, 12, 31), MockPays.Espagne);
		final MockNationalite france = new MockNationalite(date(2003, 1, 1), null, MockPays.France);
		final MockNationalite suisse = new MockNationalite(date(2004, 1, 1), date(2007, 12, 31), MockPays.Suisse);
		final List<Nationalite> all = Arrays.<Nationalite>asList(espagne, france, suisse);

		Assert.assertFalse(NationaliteHelper.isSuisseAt(null, date(1999, 12, 31)));
		Assert.assertFalse(NationaliteHelper.isSuisseAt(Collections.<Nationalite>emptyList(), date(1999, 12, 31)));
		Assert.assertFalse(NationaliteHelper.isSuisseAt(all, date(1999, 12, 31)));
		Assert.assertFalse(NationaliteHelper.isSuisseAt(all, date(2000, 12, 31)));
		Assert.assertFalse(NationaliteHelper.isSuisseAt(all, date(2003, 12, 31)));
		Assert.assertTrue(NationaliteHelper.isSuisseAt(all, date(2004, 12, 31)));
		Assert.assertTrue(NationaliteHelper.isSuisseAt(all, date(2005, 12, 31)));
		Assert.assertTrue(NationaliteHelper.isSuisseAt(all, date(2006, 12, 31)));
		Assert.assertTrue(NationaliteHelper.isSuisseAt(all, date(2007, 12, 31)));
		Assert.assertFalse(NationaliteHelper.isSuisseAt(all, date(2008, 1, 1)));
		Assert.assertFalse(NationaliteHelper.isSuisseAt(all, null));
	}

	@Test
	public void testStartingAt() throws Exception {
		final MockNationalite allemagne = new MockNationalite(null, date(1999, 12, 31), MockPays.Allemagne);
		final MockNationalite espagne = new MockNationalite(date(2000, 1, 1), date(2005, 12, 31), MockPays.Espagne);
		final MockNationalite france = new MockNationalite(date(2003, 1, 1), null, MockPays.France);
		final MockNationalite suisse = new MockNationalite(date(2003, 1, 1), date(2007, 12, 31), MockPays.Suisse);
		final List<Nationalite> all = Arrays.<Nationalite>asList(espagne, france, suisse, allemagne);

		{
			final List<Nationalite> startingAt = NationaliteHelper.startingAt(null, date(1999, 12, 31));
			Assert.assertNotNull(startingAt);
			Assert.assertEquals(0, startingAt.size());
		}
		{
			final List<Nationalite> startingAt = NationaliteHelper.startingAt(Collections.<Nationalite>emptyList(), date(1999, 12, 31));
			Assert.assertNotNull(startingAt);
			Assert.assertEquals(0, startingAt.size());
		}
		{
			final List<Nationalite> startingAt = NationaliteHelper.startingAt(all, date(1999, 12, 31));
			Assert.assertNotNull(startingAt);
			Assert.assertEquals(0, startingAt.size());
		}
		{
			final List<Nationalite> startingAt = NationaliteHelper.startingAt(all, date(2000, 1, 1));
			Assert.assertNotNull(startingAt);
			Assert.assertEquals(1, startingAt.size());
			Assert.assertSame(espagne, startingAt.get(0));
		}
		{
			final List<Nationalite> startingAt = NationaliteHelper.startingAt(all, date(2003, 1, 1));
			Assert.assertNotNull(startingAt);
			Assert.assertEquals(2, startingAt.size());
			Assert.assertSame(france, startingAt.get(0));
			Assert.assertSame(suisse, startingAt.get(1));
		}
		{
			final List<Nationalite> startingAt = NationaliteHelper.startingAt(all, null);
			Assert.assertNotNull(startingAt);
			Assert.assertEquals(1, startingAt.size());
			Assert.assertSame(allemagne, startingAt.get(0));
		}
	}

	@Test
	public void testEndingAt() throws Exception {
		final MockNationalite allemagne = new MockNationalite(null, date(1999, 12, 31), MockPays.Allemagne);
		final MockNationalite espagne = new MockNationalite(date(2000, 1, 1), date(2005, 12, 31), MockPays.Espagne);
		final MockNationalite france = new MockNationalite(date(2003, 1, 1), null, MockPays.France);
		final MockNationalite suisse = new MockNationalite(date(2003, 1, 1), date(2007, 12, 31), MockPays.Suisse);
		final MockNationalite japon = new MockNationalite(date(2002, 1, 1), date(2007, 12, 31), MockPays.Japon);
		final List<Nationalite> all = Arrays.<Nationalite>asList(espagne, france, suisse, allemagne, japon);

		{
			final List<Nationalite> endingAt = NationaliteHelper.endingAt(null, date(1999, 12, 31));
			Assert.assertNotNull(endingAt);
			Assert.assertEquals(0, endingAt.size());
		}
		{
			final List<Nationalite> endingAt = NationaliteHelper.endingAt(Collections.<Nationalite>emptyList(), date(1999, 12, 31));
			Assert.assertNotNull(endingAt);
			Assert.assertEquals(0, endingAt.size());
		}
		{
			final List<Nationalite> endingAt = NationaliteHelper.endingAt(all, date(1998, 12, 31));
			Assert.assertNotNull(endingAt);
			Assert.assertEquals(0, endingAt.size());
		}
		{
			final List<Nationalite> endingAt = NationaliteHelper.endingAt(all, date(1999, 12, 31));
			Assert.assertNotNull(endingAt);
			Assert.assertEquals(1, endingAt.size());
			Assert.assertSame(allemagne, endingAt.get(0));
		}
		{
			final List<Nationalite> endingAt = NationaliteHelper.endingAt(all, date(2003, 1, 1));
			Assert.assertNotNull(endingAt);
			Assert.assertEquals(0, endingAt.size());
		}
		{
			final List<Nationalite> endingAt = NationaliteHelper.endingAt(all, date(2007, 12, 31));
			Assert.assertNotNull(endingAt);
			Assert.assertEquals(2, endingAt.size());
			Assert.assertSame(suisse, endingAt.get(0));
			Assert.assertSame(japon, endingAt.get(1));
		}
		{
			final List<Nationalite> endingAt = NationaliteHelper.endingAt(all, null);
			Assert.assertNotNull(endingAt);
			Assert.assertEquals(1, endingAt.size());
			Assert.assertSame(france, endingAt.get(0));
		}
	}
}
