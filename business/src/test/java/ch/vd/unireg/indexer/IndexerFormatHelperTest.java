package ch.vd.unireg.indexer;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.WithoutSpringTest;

import static org.junit.Assert.assertEquals;

public class IndexerFormatHelperTest extends WithoutSpringTest {

	@Test
	public void testNoAvsToString() {
		assertEquals("NULL", IndexerFormatHelper.noAvsToString(null));
		assertEquals("27474184116", IndexerFormatHelper.noAvsToString("274.74.184.116"));
		assertEquals("27474184116", IndexerFormatHelper.noAvsToString("274.74-184 116"));
		assertEquals("27474184116", IndexerFormatHelper.noAvsToString("274.74-184116"));
		assertEquals("27474184116", IndexerFormatHelper.noAvsToString(" 274.74-184116 "));
	}

	@Test
	public void testNumberToString() {
		assertEquals("NULL", IndexerFormatHelper.numberToString(null));
		assertEquals("1234", IndexerFormatHelper.numberToString(1234L));
		assertEquals("9876", IndexerFormatHelper.numberToString(9876));
	}

	@Test
	public void testBooleanToString() {
		assertEquals("NULL", IndexerFormatHelper.booleanToString(null));
		assertEquals("O", IndexerFormatHelper.booleanToString(Boolean.TRUE));
		assertEquals("N", IndexerFormatHelper.booleanToString(Boolean.FALSE));
		assertEquals("O", IndexerFormatHelper.booleanToString(true));
		assertEquals("N", IndexerFormatHelper.booleanToString(false));
	}

	private static enum MyEnum {
		ONE, TWO, THREE
	}

	@Test
	public void testEnumToString() {
		assertEquals("NULL", IndexerFormatHelper.enumToString(null));
		assertEquals("ONE", IndexerFormatHelper.enumToString(MyEnum.ONE));
		assertEquals("TWO", IndexerFormatHelper.enumToString(MyEnum.TWO));
		assertEquals("THREE", IndexerFormatHelper.enumToString(MyEnum.THREE));
	}

	@Test
	public void testNullableStringToString() {
		assertEquals("NULL", IndexerFormatHelper.nullableStringToString(null));
		assertEquals("Youplala!!", IndexerFormatHelper.nullableStringToString("Youplala!!"));
		assertEquals("Youplili!!", IndexerFormatHelper.nullableStringToString("Youplili!!"));
	}

	@Test
	public void testDateToStorageString() {
		assertEquals("NULL", IndexerFormatHelper.dateToString(null, IndexerFormatHelper.DateStringMode.STORAGE));
		assertEquals("1965", IndexerFormatHelper.dateToString(RegDate.get(1965), IndexerFormatHelper.DateStringMode.STORAGE));
		assertEquals("197401", IndexerFormatHelper.dateToString(RegDate.get(1974, 1), IndexerFormatHelper.DateStringMode.STORAGE));
		assertEquals("20041222", IndexerFormatHelper.dateToString(RegDate.get(2004, 12, 22), IndexerFormatHelper.DateStringMode.STORAGE));
	}

	@Test
	public void testDateToIndexationString() {
		// cas simple d'une date complète : l'année, le mois et la date exacte
		{
			assertEquals("1945 194502 19450223", IndexerFormatHelper.dateToString(date(1945, 2, 23), IndexerFormatHelper.DateStringMode.INDEXATION));
		}

		// cas d'une date partielle YYYYMM : l'année, puis le mois, puis tous les jours du mois
		{
			final StringBuilder b = new StringBuilder("1954 195406");
			for (int i = 0 ; i < 30 ; ++ i) {
				b.append(" ").append(RegDateHelper.toIndexString(RegDate.get(1954, 6, 1).addDays(i)));
			}
			assertEquals(b.toString(), IndexerFormatHelper.dateToString(date(1954, 6), IndexerFormatHelper.DateStringMode.INDEXATION));
		}

		// cas d'une date partielle YYYY : l'année, puis chaque mois, puis tous les jours de l'année
		{
			final StringBuilder b = new StringBuilder("1962");
			for (int i = 0 ; i < 12 ; ++ i) {
				b.append(" ").append(RegDateHelper.toIndexString(RegDate.get(1962, i + 1)));
			}
			for (int i = 0 ; i < 365 ; ++ i) {
				b.append(" ").append(RegDateHelper.toIndexString(RegDate.get(1962, 1, 1).addDays(i)));
			}
			assertEquals(b.toString(), IndexerFormatHelper.dateToString(date(1962), IndexerFormatHelper.DateStringMode.INDEXATION));
		}
	}

	@Test
	public void testDateCollectionToIndexationString() {
		// date complète, dates partielles
		final List<RegDate> data = Arrays.asList(RegDate.get(2001, 9, 11), RegDate.get(2010, 3), null, RegDate.get(2005));
		final StringBuilder b = new StringBuilder();
		b.append("2001 200109 20010911");
		b.append(" 2010 201003");
		for (int i = 0 ; i < 31 ; ++ i) {
			b.append(" ").append(RegDateHelper.toIndexString(RegDate.get(2010, 3, 1).addDays(i)));
		}
		b.append(" NULL");
		b.append(" 2005 200501 200502 200503 200504 200505 200506 200507 200508 200509 200510 200511 200512");
		for (int i = 0 ; i < 365 ; ++ i) {
			b.append(" ").append(RegDateHelper.toIndexString(RegDate.get(2005, 1, 1).addDays(i)));
		}
		assertEquals(b.toString(), IndexerFormatHelper.dateCollectionToString(data, IndexerFormatHelper.DateStringMode.INDEXATION));
	}

	@Test
	public void testDateCollectionToStorageString() {
		// date complète, dates partielles
		final List<RegDate> data = Arrays.asList(RegDate.get(2001, 9, 11), RegDate.get(2010, 3), null, RegDate.get(2005));
		assertEquals("20010911 201003 NULL 2005", IndexerFormatHelper.dateCollectionToString(data, IndexerFormatHelper.DateStringMode.STORAGE));
	}
}
