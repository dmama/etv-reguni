package ch.vd.unireg.hibernate;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ch.vd.unireg.type.delai.Delai;
import ch.vd.unireg.type.delai.DelaiEnJours;
import ch.vd.unireg.type.delai.DelaiEnMois;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DelaiUserTypeTest {

	private DelaiUserType userType;

	@Before
	public void setUp() throws Exception {
		userType = new DelaiUserType();
	}

	@Test
	public void testNullSafeGet() throws Exception {
		final ResultSet resultSet = Mockito.mock(ResultSet.class);
		Mockito.when(resultSet.getString("UN")).thenReturn("6M");
		Mockito.when(resultSet.getString("DEUX")).thenReturn("75D");
		Mockito.when(resultSet.getString("TROIS")).thenReturn("6M~");

		{
			final DelaiEnMois delai = (DelaiEnMois) userType.nullSafeGet(resultSet, new String[]{"UN"}, null, null);
			assertNotNull(delai);
			assertEquals(6, delai.getMois());
			assertFalse(delai.isReportFinMois());
		}

		{
			final DelaiEnJours delai = (DelaiEnJours) userType.nullSafeGet(resultSet, new String[]{"DEUX"}, null, null);
			assertNotNull(delai);
			assertEquals(75, delai.getJours());
			assertFalse(delai.isReportFinMois());
		}

		{
			final DelaiEnMois delai = (DelaiEnMois) userType.nullSafeGet(resultSet, new String[]{"TROIS"}, null, null);
			assertNotNull(delai);
			assertEquals(6, delai.getMois());
			assertTrue(delai.isReportFinMois());
		}
	}

	@Test
	public void testNullSafeGetNull() throws Exception {
		final ResultSet resultSet = Mockito.mock(ResultSet.class);
		Mockito.when(resultSet.getString("NULL")).thenReturn(null);
		Mockito.when(resultSet.wasNull()).thenReturn(true);

		final DelaiEnMois delai = (DelaiEnMois) userType.nullSafeGet(resultSet, new String[]{"NULL"}, null, null);
		assertNull(delai);
	}

	@Test
	public void testNullSafeGetEmpty() throws Exception {
		final ResultSet resultSet = Mockito.mock(ResultSet.class);
		Mockito.when(resultSet.getString("EMPTY")).thenReturn("");
		Mockito.when(resultSet.wasNull()).thenReturn(false);

		final DelaiEnMois delai = (DelaiEnMois) userType.nullSafeGet(resultSet, new String[]{"EMPTY"}, null, null);
		assertNull(delai);
	}

	@Test
	public void testNullSafeSet() throws Exception {

		final Delai un = new DelaiEnMois(6, false);
		final Delai deux = new DelaiEnJours(75, false);
		final Delai trois = new DelaiEnMois(-18, true);

		{
			final PreparedStatement statement = Mockito.mock(PreparedStatement.class);
			userType.nullSafeSet(statement, un, 0, null);
			Mockito.verify(statement).setString(0, "6M");
		}

		{
			final PreparedStatement statement = Mockito.mock(PreparedStatement.class);
			userType.nullSafeSet(statement, deux, 0, null);
			Mockito.verify(statement).setString(0, "75D");
		}

		{
			final PreparedStatement statement = Mockito.mock(PreparedStatement.class);
			userType.nullSafeSet(statement, trois, 0, null);
			Mockito.verify(statement).setString(0, "-18M~");
		}
	}

	@Test
	public void testNullSafeSetNull() throws Exception {
		final PreparedStatement statement = Mockito.mock(PreparedStatement.class);
		userType.nullSafeSet(statement, null, 0, null);
		Mockito.verify(statement).setNull(0, Types.VARCHAR);
	}
}