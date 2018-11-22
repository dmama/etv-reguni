package ch.vd.unireg.hibernate;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ch.vd.unireg.type.delai.Delai;
import ch.vd.unireg.type.delai.DelaiComposite;
import ch.vd.unireg.type.delai.DelaiEnJours;
import ch.vd.unireg.type.delai.DelaiEnMois;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DelaiListUserTypeTest {

	private DelaiListUserType userType;

	@Before
	public void setUp() throws Exception {
		userType = new DelaiListUserType();
	}

	@Test
	public void testNullSafeGet() throws Exception {
		final ResultSet resultSet = Mockito.mock(ResultSet.class);
		Mockito.when(resultSet.getString("UN")).thenReturn("6M");
		Mockito.when(resultSet.getString("DEUX")).thenReturn("6M + 75D, 13M");
		Mockito.when(resultSet.getString("DEUXBIS")).thenReturn("6M~+12D, 18M");

		{
			//noinspection unchecked
			final List<Delai> list = (List<Delai>) userType.nullSafeGet(resultSet, new String[]{"UN"}, null, null);
			assertNotNull(list);
			assertEquals(1, list.size());

			final DelaiEnMois d0 = (DelaiEnMois) list.get(0);
			assertNotNull(d0);
			assertEquals(6, d0.getMois());
			assertFalse(d0.isReportFinMois());
		}

		{
			//noinspection unchecked
			final List<Delai> list = new ArrayList<>((List<Delai>) userType.nullSafeGet(resultSet, new String[]{"DEUX"}, null, null));
			assertNotNull(list);
			assertEquals(2, list.size());

			final DelaiComposite d0 = (DelaiComposite) list.get(0);
			assertNotNull(d0);
			{
				final List<Delai> composants = d0.getComposants();
				final DelaiEnMois c0 = (DelaiEnMois) composants.get(0);
				assertNotNull(c0);
				assertEquals(6, c0.getMois());
				assertFalse(c0.isReportFinMois());

				final DelaiEnJours c1 = (DelaiEnJours) composants.get(1);
				assertNotNull(c1);
				assertEquals(75, c1.getJours());
				assertFalse(c1.isReportFinMois());
			}

			final DelaiEnMois d1 = (DelaiEnMois) list.get(1);
			assertNotNull(d1);
			assertEquals(13, d1.getMois());
			assertFalse(d1.isReportFinMois());
		}

		{
			//noinspection unchecked
			final List<Delai> list = new ArrayList<>((List<Delai>) userType.nullSafeGet(resultSet, new String[]{"DEUXBIS"}, null, null));
			assertNotNull(list);
			assertEquals(2, list.size());

			final DelaiComposite d0 = (DelaiComposite) list.get(0);
			assertNotNull(d0);
			{
				final List<Delai> composants = d0.getComposants();
				final DelaiEnMois c0 = (DelaiEnMois) composants.get(0);
				assertNotNull(c0);
				assertEquals(6, c0.getMois());
				assertTrue(c0.isReportFinMois());

				final DelaiEnJours c1 = (DelaiEnJours) composants.get(1);
				assertNotNull(c1);
				assertEquals(12, c1.getJours());
				assertFalse(c1.isReportFinMois());
			}

			final DelaiEnMois d1 = (DelaiEnMois) list.get(1);
			assertNotNull(d1);
			assertEquals(18, d1.getMois());
			assertFalse(d1.isReportFinMois());
		}
	}

	@Test
	public void testNullSafeGetNull() throws Exception {
		final ResultSet resultSet = Mockito.mock(ResultSet.class);
		Mockito.when(resultSet.getString("NULL")).thenReturn(null);
		Mockito.when(resultSet.wasNull()).thenReturn(true);

		//noinspection unchecked
		final List<Delai> list = (List<Delai>) userType.nullSafeGet(resultSet, new String[]{"NULL"}, null, null);
		assertNotNull(list);
		assertEquals(0, list.size());
	}

	@Test
	public void testNullSafeGetEmpty() throws Exception {
		final ResultSet resultSet = Mockito.mock(ResultSet.class);
		Mockito.when(resultSet.getString("EMPTY")).thenReturn("");
		Mockito.when(resultSet.wasNull()).thenReturn(false);

		//noinspection unchecked
		final List<Delai> list = (List<Delai>) userType.nullSafeGet(resultSet, new String[]{"EMPTY"}, null, null);
		assertNotNull(list);
		assertEquals(0, list.size());
	}

	@Test
	public void testNullSafeSet() throws Exception {

		final List<Delai> un = Collections.singletonList(new DelaiEnMois(6, false));
		final List<Delai> deux = Arrays.asList(new DelaiEnMois(6, false), new DelaiEnJours(75, false));
		final List<Delai> trois = Arrays.asList(new DelaiComposite(Arrays.asList(new DelaiEnMois(6, true), new DelaiEnJours(75, false))), new DelaiEnMois(18, false));

		{
			final PreparedStatement statement = Mockito.mock(PreparedStatement.class);
			userType.nullSafeSet(statement, un, 0, null);
			Mockito.verify(statement).setString(0, "6M");
		}

		{
			final PreparedStatement statement = Mockito.mock(PreparedStatement.class);
			userType.nullSafeSet(statement, deux, 0, null);
			Mockito.verify(statement).setString(0, "6M, 75D");
		}

		{
			final PreparedStatement statement = Mockito.mock(PreparedStatement.class);
			userType.nullSafeSet(statement, trois, 0, null);
			Mockito.verify(statement).setString(0, "6M~ + 75D, 18M");
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