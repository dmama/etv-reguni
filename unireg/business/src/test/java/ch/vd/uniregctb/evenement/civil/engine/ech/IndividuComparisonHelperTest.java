package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.Arrays;
import java.util.Collections;

import junit.framework.Assert;
import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

public class IndividuComparisonHelperTest  extends WithoutSpringTest {

	@Test
	public void testAreContentsEqual() throws Exception {

		Assert.assertTrue(IndividuComparisonHelper.areContentsEqual(null, null, IndividuComparisonHelper.INTEGER_COMPARATOR, IndividuComparisonHelper.INTEGER_EQUALATOR));
		Assert.assertTrue(IndividuComparisonHelper.areContentsEqual(null, Collections.<Integer>emptyList(), IndividuComparisonHelper.INTEGER_COMPARATOR, IndividuComparisonHelper.INTEGER_EQUALATOR));
		Assert.assertTrue(IndividuComparisonHelper.areContentsEqual(Collections.<Integer>emptyList(), null, IndividuComparisonHelper.INTEGER_COMPARATOR, IndividuComparisonHelper.INTEGER_EQUALATOR));
		Assert.assertTrue(IndividuComparisonHelper.areContentsEqual(Arrays.asList(1, 2, 3), Arrays.asList(3, 1, 2), IndividuComparisonHelper.INTEGER_COMPARATOR, IndividuComparisonHelper.INTEGER_EQUALATOR));
		Assert.assertTrue(IndividuComparisonHelper.areContentsEqual(Arrays.asList(1), Arrays.asList(1), IndividuComparisonHelper.INTEGER_COMPARATOR, IndividuComparisonHelper.INTEGER_EQUALATOR));
		Assert.assertTrue(IndividuComparisonHelper.areContentsEqual(Arrays.asList(1, 2), Arrays.asList(1, 2), IndividuComparisonHelper.INTEGER_COMPARATOR, IndividuComparisonHelper.INTEGER_EQUALATOR));

		Assert.assertFalse(IndividuComparisonHelper.areContentsEqual(Arrays.asList(1, 2, 3), null, IndividuComparisonHelper.INTEGER_COMPARATOR, IndividuComparisonHelper.INTEGER_EQUALATOR));
		Assert.assertFalse(IndividuComparisonHelper.areContentsEqual(null, Arrays.asList(1, 2, 3), IndividuComparisonHelper.INTEGER_COMPARATOR, IndividuComparisonHelper.INTEGER_EQUALATOR));
		Assert.assertFalse(IndividuComparisonHelper.areContentsEqual(Arrays.asList(1, 2, 3), Collections.<Integer>emptyList(), IndividuComparisonHelper.INTEGER_COMPARATOR, IndividuComparisonHelper.INTEGER_EQUALATOR));
		Assert.assertFalse(IndividuComparisonHelper.areContentsEqual(Arrays.asList(1, 2), Arrays.asList(1, 2, 3), IndividuComparisonHelper.INTEGER_COMPARATOR, IndividuComparisonHelper.INTEGER_EQUALATOR));
		Assert.assertFalse(IndividuComparisonHelper.areContentsEqual(Arrays.asList(1, 3), Arrays.asList(1, 2, 3), IndividuComparisonHelper.INTEGER_COMPARATOR, IndividuComparisonHelper.INTEGER_EQUALATOR));
		Assert.assertFalse(IndividuComparisonHelper.areContentsEqual(Arrays.asList(1, 2, 3, 4), Arrays.asList(1, 2, 3), IndividuComparisonHelper.INTEGER_COMPARATOR, IndividuComparisonHelper.INTEGER_EQUALATOR));
	}
}
