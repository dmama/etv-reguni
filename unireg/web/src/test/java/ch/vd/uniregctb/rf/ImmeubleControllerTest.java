package ch.vd.uniregctb.rf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;
import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

public class ImmeubleControllerTest extends WithoutSpringTest {
	
	@Test
	public void testNoImmeubleComparator() throws Exception {
		final List<String> str = new ArrayList<String>(Arrays.asList("123-3", "123-2-1", "123--1", "5312", "", "123-2", "123", "12"));
		Collections.sort(str, ImmeubleController.NO_IMMEUBLE_COMPARATOR);
		final Iterator<String> iter = str.iterator();
		Assert.assertEquals("", iter.next());
		Assert.assertEquals("12", iter.next());
		Assert.assertEquals("123", iter.next());
		Assert.assertEquals("123--1", iter.next());
		Assert.assertEquals("123-2", iter.next());
		Assert.assertEquals("123-2-1", iter.next());
		Assert.assertEquals("123-3", iter.next());
		Assert.assertEquals("5312", iter.next());
		Assert.assertFalse(iter.hasNext());
	}
}
