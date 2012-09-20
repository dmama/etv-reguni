package ch.vd.uniregctb.common;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;

import junit.framework.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.DateConstants;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;

public class XmlUtilsTest extends WithoutSpringTest {

	@Test
	public void testXmlNormalDateToRegDate() throws Exception {
		final Date date = DateHelper.getDate(2012, 3, 12);
		final XMLGregorianCalendar xml = XmlUtils.date2xmlcal(date);
		final RegDate converted = XmlUtils.xmlcal2regdate(xml);
		Assert.assertEquals(RegDate.get(date), converted);
	}

	@Test
	public void testXmlDate01010001ToRegDate() throws Exception {
		final Date date = DateHelper.getDate(1, 1, 1);
		final XMLGregorianCalendar xml = XmlUtils.date2xmlcal(date);
		final RegDate regDate = XmlUtils.xmlcal2regdate(xml);
		Assert.assertNull(regDate);
	}

	@Test
	public void testXmlDateJustBeforeBigBangToRegDate() throws Exception {
		final Date date = DateHelper.getDate(DateConstants.EARLY_YEAR - 1, 12, 31);
		final XMLGregorianCalendar xml = XmlUtils.date2xmlcal(date);
		final RegDate regDate = XmlUtils.xmlcal2regdate(xml);
		Assert.assertNull(regDate);
	}

	@Test
	public void testXmlDateOnBigBangToRegDate() throws Exception {
		final Date date = DateHelper.getDate(DateConstants.EARLY_YEAR, DateConstants.EARLY_MONTH, DateConstants.EARLY_DAY);
		final XMLGregorianCalendar xml = XmlUtils.date2xmlcal(date);
		final RegDate regDate = XmlUtils.xmlcal2regdate(xml);
		Assert.assertNotNull(regDate);
		Assert.assertEquals(RegDate.get(date), regDate);
	}
}
