package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.junit.Test;

import ch.vd.evd0022.v1.PersonDataSource;

public class PersonDataSourceConverterTest {
	private final PersonDataSourceConverter converter = new PersonDataSourceConverter();

	@Test
	public void tryAllValues() throws Exception {
		EnumTestHelper.testAllValues(PersonDataSource.class, converter);
	}
}