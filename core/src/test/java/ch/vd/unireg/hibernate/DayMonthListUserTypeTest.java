package ch.vd.unireg.hibernate;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ch.vd.unireg.type.DayMonth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DayMonthListUserTypeTest {

	private DayMonthListUserType userType;

	@Before
	public void setUp() throws Exception {
		userType = new DayMonthListUserType();
	}

	@Test
	public void testNullSafeGet() throws Exception {
		final ResultSet resultSet = Mockito.mock(ResultSet.class);
		Mockito.when(resultSet.getString("UN")).thenReturn("0203");
		Mockito.when(resultSet.getString("DEUX")).thenReturn("0203,0911");
		Mockito.when(resultSet.getString("DEUXBIS")).thenReturn("0203, 0911");

		{
			//noinspection unchecked
			final List<DayMonth> list = (List<DayMonth>) userType.nullSafeGet(resultSet, new String[]{"UN"}, null, null);
			assertNotNull(list);
			assertEquals(1, list.size());

			final DayMonth dm0 = list.get(0);
			assertNotNull(dm0);
			assertEquals(2, dm0.month());
			assertEquals(3, dm0.day());
		}

		{
			//noinspection unchecked
			final List<DayMonth> list = new ArrayList<>((List<DayMonth>) userType.nullSafeGet(resultSet, new String[]{"DEUX"}, null, null));
			assertNotNull(list);
			assertEquals(2, list.size());
			list.sort(Comparator.naturalOrder());

			final DayMonth dm0 = list.get(0);
			assertNotNull(dm0);
			assertEquals(2, dm0.month());
			assertEquals(3, dm0.day());

			final DayMonth dm1 = list.get(1);
			assertNotNull(dm1);
			assertEquals(9, dm1.month());
			assertEquals(11, dm1.day());
		}

		{
			//noinspection unchecked
			final List<DayMonth> list = new ArrayList<>((List<DayMonth>) userType.nullSafeGet(resultSet, new String[]{"DEUXBIS"}, null, null));
			assertNotNull(list);
			assertEquals(2, list.size());
			list.sort(Comparator.naturalOrder());

			final DayMonth dm0 = list.get(0);
			assertNotNull(dm0);
			assertEquals(2, dm0.month());
			assertEquals(3, dm0.day());

			final DayMonth dm1 = list.get(1);
			assertNotNull(dm1);
			assertEquals(9, dm1.month());
			assertEquals(11, dm1.day());
		}
	}

	@Test
	public void testNullSafeGetNull() throws Exception {
		final ResultSet resultSet = Mockito.mock(ResultSet.class);
		Mockito.when(resultSet.getString("NULL")).thenReturn(null);
		Mockito.when(resultSet.wasNull()).thenReturn(true);

		//noinspection unchecked
		final List<DayMonth> list = (List<DayMonth>) userType.nullSafeGet(resultSet, new String[]{"NULL"}, null, null);
		assertNotNull(list);
		assertEquals(0, list.size());
	}

	@Test
	public void testNullSafeGetEmpty() throws Exception {
		final ResultSet resultSet = Mockito.mock(ResultSet.class);
		Mockito.when(resultSet.getString("EMPTY")).thenReturn("");
		Mockito.when(resultSet.wasNull()).thenReturn(false);

		//noinspection unchecked
		final List<DayMonth> list = (List<DayMonth>) userType.nullSafeGet(resultSet, new String[]{"EMPTY"}, null, null);
		assertNotNull(list);
		assertEquals(0, list.size());
	}

	@Test
	public void testNullSafeSet() throws Exception {

		final List<DayMonth> un = Collections.singletonList(DayMonth.get(12, 5));
		final List<DayMonth> deux = Arrays.asList(DayMonth.get(2, 15), DayMonth.get(12, 5));
		final List<DayMonth> trois = Arrays.asList(DayMonth.get(12, 5), DayMonth.get(2, 15), DayMonth.get(10, 22));

		{
			final PreparedStatement statement = Mockito.mock(PreparedStatement.class);
			userType.nullSafeSet(statement, un, 0, null);
			Mockito.verify(statement).setString(0, "1205");
		}

		{
			final PreparedStatement statement = Mockito.mock(PreparedStatement.class);
			userType.nullSafeSet(statement, deux, 0, null);
			Mockito.verify(statement).setString(0, "0215, 1205");
		}

		{
			final PreparedStatement statement = Mockito.mock(PreparedStatement.class);
			userType.nullSafeSet(statement, trois, 0, null);
			Mockito.verify(statement).setString(0, "1205, 0215, 1022");
		}
	}

	@Test
	public void testNullSafeSetNull() throws Exception {
		final PreparedStatement statement = Mockito.mock(PreparedStatement.class);
		userType.nullSafeSet(statement, null, 0, null);
		Mockito.verify(statement).setNull(0, Types.VARCHAR);
	}

	@Test
	public void testNullSafeSetEmpty() throws Exception {
		final PreparedStatement statement = Mockito.mock(PreparedStatement.class);
		userType.nullSafeSet(statement, Collections.emptyList(), 0, null);
		Mockito.verify(statement).setNull(0, Types.VARCHAR);
	}
}