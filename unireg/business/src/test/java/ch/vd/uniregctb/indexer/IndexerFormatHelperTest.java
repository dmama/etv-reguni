package ch.vd.uniregctb.indexer;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

public class IndexerFormatHelperTest extends WithoutSpringTest {

	@Test
	public void testFormatNumeroAVS() {

		assertEquals("27474184116", IndexerFormatHelper.formatNumeroAVS("274.74.184.116"));
		assertEquals("27474184116", IndexerFormatHelper.formatNumeroAVS("274.74-184 116"));
		assertEquals("27474184116", IndexerFormatHelper.formatNumeroAVS("274.74-184116"));
	}

	@Test
	public void testObjectToString() {

		assertEquals("", IndexerFormatHelper.objectToString(null));
		assertEquals("str", IndexerFormatHelper.objectToString("str"));
		assertEquals("1234", IndexerFormatHelper.objectToString(new Long(1234L)));
		assertEquals("9876", IndexerFormatHelper.objectToString(9876));
	}

}
