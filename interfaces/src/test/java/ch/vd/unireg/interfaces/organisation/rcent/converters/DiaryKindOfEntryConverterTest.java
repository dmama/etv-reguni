package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.junit.Test;

import ch.vd.evd0022.v3.DiaryKindOfEntry;

public class DiaryKindOfEntryConverterTest {

	private final DiaryKindOfEntryConverter converter = new DiaryKindOfEntryConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(DiaryKindOfEntry.class, converter);
	}
}