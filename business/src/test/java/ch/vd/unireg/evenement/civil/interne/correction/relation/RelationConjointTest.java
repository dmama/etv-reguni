package ch.vd.unireg.evenement.civil.interne.correction.relation;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.common.WithoutSpringTest;

public class RelationConjointTest extends WithoutSpringTest {

	@Test
	public void testHasDifferenceToutPareil() throws Exception {
		final List<RelationConjoint> fiscal = Arrays.asList(new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), 1234L, false),
		                                                                       new RelationConjoint(date(2006, 5, 1), date(2008, 9, 5), 4567L, false),
		                                                                       new RelationConjoint(date(2010, 7, 30), null, 4321L, false));

		final List<RelationConjoint> civil = Arrays.asList(new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), 1234L, false),
		                                                                      new RelationConjoint(date(2006, 5, 1), date(2008, 9, 5), 4567L, false),
		                                                                      new RelationConjoint(date(2010, 7, 30), null, 4321L, false));

		Assert.assertFalse(RelationConjoint.hasDifference(fiscal, civil));
	}

	@Test
	public void testHasDifferenceChangementNoIndividu() throws Exception {
		final List<RelationConjoint> fiscal = Arrays.asList(new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), 1234L, false),
		                                                                       new RelationConjoint(date(2006, 5, 1), date(2008, 9, 5), 45678L, false),
		                                                                       new RelationConjoint(date(2010, 7, 30), null, 4321L, false));

		final List<RelationConjoint> civil = Arrays.asList(new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), 1234L, false),
		                                                                      new RelationConjoint(date(2006, 5, 1), date(2008, 9, 5), 4567L, false),
		                                                                      new RelationConjoint(date(2010, 7, 30), null, 4321L, false));

		Assert.assertTrue(RelationConjoint.hasDifference(fiscal, civil));
	}

	@Test
	public void testHasDifferenceDisparitionNoIndividu() throws Exception {
		final List<RelationConjoint> fiscal = Arrays.asList(new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), 1234L, false),
		                                                                       new RelationConjoint(date(2006, 5, 1), date(2008, 9, 5), 4567L, false),
		                                                                       new RelationConjoint(date(2010, 7, 30), null, 4321L, false));

		final List<RelationConjoint> civil = Arrays.asList(new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), 1234L, false),
		                                                                      new RelationConjoint(date(2006, 5, 1), date(2008, 9, 5), null, false),
		                                                                      new RelationConjoint(date(2010, 7, 30), null, 4321L, false));

		Assert.assertTrue(RelationConjoint.hasDifference(fiscal, civil));
	}

	@Test
	public void testHasDifferenceApparitionNoIndividu() throws Exception {
		final List<RelationConjoint> fiscal = Arrays.asList(new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), 1234L, false),
		                                                                       new RelationConjoint(date(2006, 5, 1), date(2008, 9, 5), null, false),
		                                                                       new RelationConjoint(date(2010, 7, 30), null, 4321L, false));

		final List<RelationConjoint> civil = Arrays.asList(new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), 1234L, false),
		                                                                      new RelationConjoint(date(2006, 5, 1), date(2008, 9, 5), 4567L, false),
		                                                                      new RelationConjoint(date(2010, 7, 30), null, 4321L, false));

		Assert.assertTrue(RelationConjoint.hasDifference(fiscal, civil));
	}


	@Test
	public void testHasDifferenceIdentiquesYComprisNumerosIndividuInconnus() throws Exception {
		final List<RelationConjoint> fiscal = Arrays.asList(new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), 1234L, false),
		                                                                       new RelationConjoint(date(2006, 5, 1), date(2008, 9, 5), null, false),
		                                                                       new RelationConjoint(date(2010, 7, 30), null, 4321L, false));

		final List<RelationConjoint> civil = Arrays.asList(new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), 1234L, false),
		                                                                      new RelationConjoint(date(2006, 5, 1), date(2008, 9, 5), null, false),
		                                                                      new RelationConjoint(date(2010, 7, 30), null, 4321L, false));

		Assert.assertFalse(RelationConjoint.hasDifference(fiscal, civil));
	}

	@Test
	public void testHasDifferenceIdentiquesNumerosIndividuInconnusAvecConjointFiscalConnu() throws Exception {
		final List<RelationConjoint> fiscal = Arrays.asList(new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), 1234L, false),
		                                                                       new RelationConjoint(date(2006, 5, 1), date(2008, 9, 5), null, true),
		                                                                       new RelationConjoint(date(2010, 7, 30), null, 4321L, false));

		final List<RelationConjoint> civil = Arrays.asList(new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), 1234L, false),
		                                                                      new RelationConjoint(date(2006, 5, 1), date(2008, 9, 5), null, false),
		                                                                      new RelationConjoint(date(2010, 7, 30), null, 4321L, false));

		Assert.assertTrue(RelationConjoint.hasDifference(fiscal, civil));
	}

	@Test
	public void testHasDifferenceChangementDateDebut() throws Exception {
		final List<RelationConjoint> fiscal = Arrays.asList(new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), 1234L, false),
		                                                                       new RelationConjoint(date(2006, 5, 2), date(2008, 9, 5), 4567L, false),
		                                                                       new RelationConjoint(date(2010, 7, 30), null, 4321L, false));

		final List<RelationConjoint> civil = Arrays.asList(new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), 1234L, false),
		                                                                      new RelationConjoint(date(2006, 5, 1), date(2008, 9, 5), 4567L, false),
		                                                                      new RelationConjoint(date(2010, 7, 30), null, 4321L, false));

		Assert.assertTrue(RelationConjoint.hasDifference(fiscal, civil));
	}

	@Test
	public void testHasDifferenceApparitionDateDebut() throws Exception {
		final List<RelationConjoint> fiscal = Arrays.asList(new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), 1234L, false),
		                                                    new RelationConjoint(null, date(2008, 9, 5), 4567L, false),
		                                                    new RelationConjoint(date(2010, 7, 30), null, 4321L, false));

		final List<RelationConjoint> civil = Arrays.asList(new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), 1234L, false),
		                                                   new RelationConjoint(date(2006, 5, 1), date(2008, 9, 5), 4567L, false),
		                                                   new RelationConjoint(date(2010, 7, 30), null, 4321L, false));

		Assert.assertTrue(RelationConjoint.hasDifference(fiscal, civil));
	}

	@Test
	public void testHasDifferenceDisparitionDateDebut() throws Exception {
		final List<RelationConjoint> fiscal = Arrays.asList(new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), 1234L, false),
		                                                    new RelationConjoint(date(2006, 5, 1), date(2008, 9, 5), 4567L, false),
		                                                    new RelationConjoint(date(2010, 7, 30), null, 4321L, false));

		final List<RelationConjoint> civil = Arrays.asList(new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), 1234L, false),
		                                                   new RelationConjoint(null, date(2008, 9, 5), 4567L, false),
		                                                   new RelationConjoint(date(2010, 7, 30), null, 4321L, false));

		Assert.assertTrue(RelationConjoint.hasDifference(fiscal, civil));
	}

	@Test
	public void testHasDifferenceChangementDateFin() throws Exception {
		final List<RelationConjoint> fiscal = Arrays.asList(new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), 1234L, false),
		                                                                       new RelationConjoint(date(2006, 5, 1), date(2008, 9, 6), 4567L, false),
		                                                                       new RelationConjoint(date(2010, 7, 30), null, 4321L, false));

		final List<RelationConjoint> civil = Arrays.asList(new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), 1234L, false),
		                                                                      new RelationConjoint(date(2006, 5, 1), date(2008, 9, 5), 4567L, false),
		                                                                      new RelationConjoint(date(2010, 7, 30), null, 4321L, false));

		Assert.assertTrue(RelationConjoint.hasDifference(fiscal, civil));
	}

	@Test
	public void testHasDifferenceApparitionDateFin() throws Exception {
		final List<RelationConjoint> fiscal = Arrays.asList(new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), 1234L, false),
		                                                                       new RelationConjoint(date(2006, 5, 1), date(2008, 9, 5), 4567L, false),
		                                                                       new RelationConjoint(date(2010, 7, 30), null, 4321L, false));

		final List<RelationConjoint> civil = Arrays.asList(new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), 1234L, false),
		                                                                      new RelationConjoint(date(2006, 5, 1), date(2008, 9, 5), 4567L, false),
		                                                                      new RelationConjoint(date(2010, 7, 30), date(2014, 3, 1), 4321L, false));

		Assert.assertTrue(RelationConjoint.hasDifference(fiscal, civil));
	}

	@Test
	public void testHasDifferenceDisparitionDateFin() throws Exception {
		final List<RelationConjoint> fiscal = Arrays.asList(new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), 1234L, false),
		                                                                       new RelationConjoint(date(2006, 5, 1), date(2008, 9, 5), 4567L, false),
		                                                                       new RelationConjoint(date(2010, 7, 30), date(2014, 3, 1), 4321L, false));

		final List<RelationConjoint> civil = Arrays.asList(new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), 1234L, false),
		                                                                      new RelationConjoint(date(2006, 5, 1), date(2008, 9, 5), 4567L, false),
		                                                                      new RelationConjoint(date(2010, 7, 30), null, 4321L, false));

		Assert.assertTrue(RelationConjoint.hasDifference(fiscal, civil));
	}

	@Test
	public void testHasDifferenceFiscalEnPlus() throws Exception {
		final List<RelationConjoint> fiscal = Arrays.asList(new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), 1234L, false),
		                                                                       new RelationConjoint(date(2006, 5, 1), date(2008, 9, 5), 4567L, false),
		                                                                       new RelationConjoint(date(2010, 7, 30), date(2014, 3, 1), 4321L, false));

		final List<RelationConjoint> civil = Arrays.asList(new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), 1234L, false),
		                                                                      new RelationConjoint(date(2006, 5, 1), date(2008, 9, 5), 4567L, false));

		Assert.assertTrue(RelationConjoint.hasDifference(fiscal, civil));
	}

	@Test
	public void testHasDifferenceCivilEnPlus() throws Exception {
		final List<RelationConjoint> fiscal = Arrays.asList(new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), 1234L, false),
		                                                    new RelationConjoint(date(2006, 5, 1), date(2008, 9, 5), 4567L, false));

		final List<RelationConjoint> civil = Arrays.asList(new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), 1234L, false),
		                                                   new RelationConjoint(date(2006, 5, 1), date(2008, 9, 5), 4567L, false),
		                                                   new RelationConjoint(date(2010, 7, 30), null, 4321L, false));

		Assert.assertTrue(RelationConjoint.hasDifference(fiscal, civil));
	}

	@Test
	public void testMariesSeuls() throws Exception {
		{
			final RelationConjoint rel = new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), null, false);
			Assert.assertTrue(rel.isMarieSeul());
		}
		{
			final RelationConjoint rel = new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), null, true);
			Assert.assertFalse(rel.isMarieSeul());
		}
		{
			final RelationConjoint rel = new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), 32356L, false);
			Assert.assertFalse(rel.isMarieSeul());
		}
		{
			final RelationConjoint rel = new RelationConjoint(date(2001, 12, 3), date(2005, 3, 2), 342561L, true);
			Assert.assertFalse(rel.isMarieSeul());
		}
	}

	@Test
	public void testEqualsAvecChampsNulls() throws Exception {
		{
			final RelationConjoint one = new RelationConjoint(date(2000, 1, 1), null, 1234L, false);
			final RelationConjoint two = new RelationConjoint(date(2000, 1, 1), null, 1234L, false);
			Assert.assertTrue(one.equals(one));
			Assert.assertTrue(two.equals(two));
			Assert.assertTrue(one.equals(two));
			Assert.assertTrue(two.equals(one));
		}
		{
			final RelationConjoint one = new RelationConjoint(null, date(2009, 3, 1), 1234L, false);
			final RelationConjoint two = new RelationConjoint(null, date(2009, 3, 1), 1234L, false);
			Assert.assertTrue(one.equals(one));
			Assert.assertTrue(two.equals(two));
			Assert.assertTrue(one.equals(two));
			Assert.assertTrue(two.equals(one));
		}
		{
			final RelationConjoint one = new RelationConjoint(date(2000, 5, 4), date(2009, 3, 1), null, false);
			final RelationConjoint two = new RelationConjoint(date(2000, 5, 4), date(2009, 3, 1), null, false);
			Assert.assertTrue(one.equals(one));
			Assert.assertTrue(two.equals(two));
			Assert.assertTrue(one.equals(two));
			Assert.assertTrue(two.equals(one));
		}
		{
			final RelationConjoint one = new RelationConjoint(date(2000, 5, 4), date(2009, 3, 1), null, true);
			final RelationConjoint two = new RelationConjoint(date(2000, 5, 4), date(2009, 3, 1), null, false);
			Assert.assertTrue(one.equals(one));
			Assert.assertTrue(two.equals(two));
			Assert.assertFalse(one.equals(two));
			Assert.assertFalse(two.equals(one));
		}
	}
}
