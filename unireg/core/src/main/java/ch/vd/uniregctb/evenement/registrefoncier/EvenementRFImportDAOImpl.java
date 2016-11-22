package ch.vd.uniregctb.evenement.registrefoncier;

import java.util.List;

import org.hibernate.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.BaseDAOImpl;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.dbutils.QueryFragment;

public class EvenementRFImportDAOImpl extends BaseDAOImpl<EvenementRFImport, Long> implements EvenementRFImportDAO {

	protected EvenementRFImportDAOImpl() {
		super(EvenementRFImport.class);
	}

	@Nullable
	@Override
	public EvenementRFImport findNextImportToProcess() {
		final Query query = getCurrentSession().createQuery("from EvenementRFImport where etat in ('A_TRAITER', 'EN_ERREUR') order by dateEvenement asc");
		query.setMaxResults(1);
		return (EvenementRFImport) query.uniqueResult();
	}

	@Nullable
	@Override
	public EvenementRFImport findOldestImportWithUnprocessedMutations(long importId) {
		final Query query = getCurrentSession().createQuery("from EvenementRFImport where id in (select parentImport.id from EvenementRFMutation where parentImport.id != :importId and etat in ('A_TRAITER', 'EN_ERREUR')) order by dateEvenement asc");
		query.setParameter("importId", importId);
		query.setMaxResults(1);
		return (EvenementRFImport) query.uniqueResult();
	}

	@Override
	public List<EvenementRFImport> find(@Nullable List<EtatEvenementRF> etats, @NotNull ParamPagination pagination) {

		final QueryFragment fragment;
		if (etats == null || etats.isEmpty()) {
			fragment= new QueryFragment("from EvenementRFImport evenement ");
		}
		else {
			fragment = new QueryFragment("from EvenementRFImport evenement where etat in (:etats)", "etats", etats);
		}
		fragment.add(pagination.buildOrderClause("evenement", "dateEvenement", true, null));

		final Query queryObject = fragment.createQuery(getCurrentSession());
		queryObject.setFirstResult(pagination.getSqlFirstResult());
		queryObject.setMaxResults(pagination.getSqlMaxResults());

		//noinspection unchecked
		return queryObject.list();
	}

	@Override
	public int count(@Nullable List<EtatEvenementRF> etats) {

		final String queryString;
		if (etats == null || etats.isEmpty()) {
			queryString = "select count(*) from EvenementRFImport";
		}
		else {
			queryString = "select count(*) from EvenementRFImport where etat in (:etats)";
		}

		final Query query = getCurrentSession().createQuery(queryString);
		if (etats != null && !etats.isEmpty()) {
			query.setParameterList("etats", etats);
		}

		final Number o = (Number) query.uniqueResult();
		return o.intValue();
	}
}
