package ch.vd.uniregctb.registrefoncier.dao;

import java.util.Collection;
import java.util.List;

import org.hibernate.Query;
import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.common.BaseDAOImpl;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.common.ParamSorting;
import ch.vd.uniregctb.dbutils.QueryFragment;
import ch.vd.uniregctb.registrefoncier.SituationRF;

public class SituationRFDAOImpl extends BaseDAOImpl<SituationRF, Long> implements SituationRFDAO {
	protected SituationRFDAOImpl() {
		super(SituationRF.class);
	}

	@Override
	public List<SituationRF> findSituationNonSurchargeesSurCommunes(@NotNull Collection<Integer> noOfsCommunes, @NotNull ParamPagination pagination) {

		final QueryFragment fragment = new QueryFragment("from SituationRF situation where annulationDate is null and noOfsCommuneSurchargee is null and commune.noOfs in (:noOfs) ", "noOfs", noOfsCommunes);
		fragment.add(pagination.buildOrderClause("situation", null,
		                                         new ParamSorting("commune.noOfs", true),
		                                         new ParamSorting("noParcelle", true),
		                                         new ParamSorting("index1", true),
		                                         new ParamSorting("index2", true),
		                                         new ParamSorting("index3", true)
		));

		final Query query = fragment.createQuery(getCurrentSession());
		query.setFirstResult(pagination.getSqlFirstResult());
		query.setMaxResults(pagination.getSqlMaxResults());

		//noinspection unchecked
		return query.list();
	}

	@Override
	public int countSituationsNonSurchargeesSurCommunes(Collection<Integer> noOfsCommunes) {
		final Query query = getCurrentSession().createQuery("select count(*) from SituationRF where annulationDate is null and noOfsCommuneSurchargee is null and commune.noOfs in (:noOfs)");
		query.setParameterList("noOfs", noOfsCommunes);
		final Number o = (Number) query.uniqueResult();
		return o.intValue();
	}
}
