package ch.vd.unireg.common.pagination;

import org.junit.Test;

import ch.vd.unireg.dbutils.QueryFragment;

import static org.junit.Assert.assertEquals;

public class ParamPaginationTest {

	@Test
	public void testBuildOrderClausePasDeSortingParDefaut() throws Exception {

		final ParamPagination pagination = new ParamPagination(1, 20, null, true);
		final QueryFragment orderClause = pagination.buildOrderClause("test", null);

		assertEquals("order by test.id asc , test.id asc", orderClause.getQuery());
	}

	@Test
	public void testBuildOrderClauseunSortingParDefaut() throws Exception {

		final ParamPagination pagination = new ParamPagination(1, 20, null, true);
		final QueryFragment orderClause = pagination.buildOrderClause("test", null,
		                                                              new ParamSorting("canton", true));

		assertEquals("order by test.canton asc , test.id asc", orderClause.getQuery());
	}

	@Test
	public void testBuildOrderClausePlusieursSortingsParDefaut() throws Exception {

		final ParamPagination pagination = new ParamPagination(1, 20, null, true);
		final QueryFragment orderClause = pagination.buildOrderClause("test", null,
		                                                              new ParamSorting("rue", true),
		                                                              new ParamSorting("commune", false),
		                                                              new ParamSorting("canton", true));

		assertEquals("order by test.rue asc, test.commune desc, test.canton asc , test.id asc", orderClause.getQuery());
	}
}