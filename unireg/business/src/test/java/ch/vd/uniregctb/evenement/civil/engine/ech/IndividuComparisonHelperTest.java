package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

public class IndividuComparisonHelperTest  extends WithoutSpringTest {

	@Test(timeout = 10000L)
	public void testAreContentsEqual() throws Exception {

		Assert.assertTrue(IndividuComparisonHelper.areContentsEqual(null, null, IndividuComparisonHelper.INTEGER_COMPARATOR, IndividuComparisonHelper.INTEGER_EQUALATOR, null, null));
		Assert.assertTrue(IndividuComparisonHelper.areContentsEqual(null, Collections.<Integer>emptyList(), IndividuComparisonHelper.INTEGER_COMPARATOR, IndividuComparisonHelper.INTEGER_EQUALATOR, null, null));
		Assert.assertTrue(IndividuComparisonHelper.areContentsEqual(Collections.<Integer>emptyList(), null, IndividuComparisonHelper.INTEGER_COMPARATOR, IndividuComparisonHelper.INTEGER_EQUALATOR, null, null));
		Assert.assertTrue(IndividuComparisonHelper.areContentsEqual(Arrays.asList(1, 2, 3), Arrays.asList(3, 1, 2), IndividuComparisonHelper.INTEGER_COMPARATOR, IndividuComparisonHelper.INTEGER_EQUALATOR, null, null));
		Assert.assertTrue(IndividuComparisonHelper.areContentsEqual(Collections.singletonList(1), Collections.singletonList(1), IndividuComparisonHelper.INTEGER_COMPARATOR, IndividuComparisonHelper.INTEGER_EQUALATOR, null, null));
		Assert.assertTrue(IndividuComparisonHelper.areContentsEqual(Arrays.asList(1, 2), Arrays.asList(1, 2), IndividuComparisonHelper.INTEGER_COMPARATOR, IndividuComparisonHelper.INTEGER_EQUALATOR, null, null));

		Assert.assertFalse(IndividuComparisonHelper.areContentsEqual(Arrays.asList(1, 2, 3), null, IndividuComparisonHelper.INTEGER_COMPARATOR, IndividuComparisonHelper.INTEGER_EQUALATOR, null, null));
		Assert.assertFalse(IndividuComparisonHelper.areContentsEqual(null, Arrays.asList(1, 2, 3), IndividuComparisonHelper.INTEGER_COMPARATOR, IndividuComparisonHelper.INTEGER_EQUALATOR, null, null));
		Assert.assertFalse(IndividuComparisonHelper.areContentsEqual(Arrays.asList(1, 2, 3), Collections.<Integer>emptyList(), IndividuComparisonHelper.INTEGER_COMPARATOR, IndividuComparisonHelper.INTEGER_EQUALATOR, null, null));
		Assert.assertFalse(IndividuComparisonHelper.areContentsEqual(Arrays.asList(1, 2), Arrays.asList(1, 2, 3), IndividuComparisonHelper.INTEGER_COMPARATOR, IndividuComparisonHelper.INTEGER_EQUALATOR, null, null));
		Assert.assertFalse(IndividuComparisonHelper.areContentsEqual(Arrays.asList(1, 3), Arrays.asList(1, 2, 3), IndividuComparisonHelper.INTEGER_COMPARATOR, IndividuComparisonHelper.INTEGER_EQUALATOR, null, null));
		Assert.assertFalse(IndividuComparisonHelper.areContentsEqual(Arrays.asList(1, 2, 3, 4), Arrays.asList(1, 2, 3), IndividuComparisonHelper.INTEGER_COMPARATOR, IndividuComparisonHelper.INTEGER_EQUALATOR, null, null));
	}

	@Test(timeout = 10000L)
	public void testFieldMonitorOnAreContentsEqual() throws Exception {
		{
			final IndividuComparisonHelper.FieldMonitor monitor = new IndividuComparisonHelper.FieldMonitor();
			final boolean equal = IndividuComparisonHelper.areContentsEqual(Arrays.asList(1, 2, 3), null, IndividuComparisonHelper.INTEGER_COMPARATOR, IndividuComparisonHelper.INTEGER_EQUALATOR, monitor, "toto");
			Assert.assertFalse(equal);
			Assert.assertEquals(2, monitor.getCollectedFields().size());
			Assert.assertEquals("toto", monitor.getCollectedFields().get(0));
			Assert.assertEquals("disparition", monitor.getCollectedFields().get(1));
		}
		{
			final IndividuComparisonHelper.FieldMonitor monitor = new IndividuComparisonHelper.FieldMonitor();
			final boolean equal = IndividuComparisonHelper.areContentsEqual(Arrays.asList(1, 2), Arrays.asList(1, 2, 3), IndividuComparisonHelper.INTEGER_COMPARATOR, IndividuComparisonHelper.INTEGER_EQUALATOR, monitor, "toto");
			Assert.assertFalse(equal);
			Assert.assertEquals(2, monitor.getCollectedFields().size());
			Assert.assertEquals("toto", monitor.getCollectedFields().get(0));
			Assert.assertEquals("apparition", monitor.getCollectedFields().get(1));
		}
		{
			final IndividuComparisonHelper.FieldMonitor monitor = new IndividuComparisonHelper.FieldMonitor();
			final boolean equal = IndividuComparisonHelper.areContentsEqual(Arrays.asList(1, 2, 4), Arrays.asList(1, 2, 3), IndividuComparisonHelper.INTEGER_COMPARATOR, IndividuComparisonHelper.INTEGER_EQUALATOR, monitor, "toto");
			Assert.assertFalse(equal);
			Assert.assertEquals(1, monitor.getCollectedFields().size());
			Assert.assertEquals("toto", monitor.getCollectedFields().get(0));
		}
		{
			final IndividuComparisonHelper.FieldMonitor monitor = new IndividuComparisonHelper.FieldMonitor();
			final boolean equal = IndividuComparisonHelper.areContentsEqual(Arrays.asList(1, 2, 4), Arrays.asList(1, 2, 4), IndividuComparisonHelper.INTEGER_COMPARATOR, IndividuComparisonHelper.INTEGER_EQUALATOR, monitor, "toto");
			Assert.assertTrue(equal);
			Assert.assertEquals(0, monitor.getCollectedFields().size());
		}
	}
}
