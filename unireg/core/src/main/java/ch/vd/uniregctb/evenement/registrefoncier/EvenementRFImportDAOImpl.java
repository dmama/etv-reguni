package ch.vd.uniregctb.evenement.registrefoncier;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.dialect.Dialect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.BaseDAOImpl;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.dbutils.QueryFragment;

public class EvenementRFImportDAOImpl extends BaseDAOImpl<EvenementRFImport, Long> implements EvenementRFImportDAO {

	private Dialect dialect;

	protected EvenementRFImportDAOImpl() {
		super(EvenementRFImport.class);
	}

	public void setDialect(Dialect dialect) {
		this.dialect = dialect;
	}

	@Nullable
	@Override
	public EvenementRFImport findNextImportToProcess() {
		final Query query = getCurrentSession().createQuery("from EvenementRFImport where etat in ('A_TRAITER', 'EN_ERREUR') order by dateEvenement asc");
		query.setMaxResults(1);
		return (EvenementRFImport) query.uniqueResult();
	}

	@Override
	public int deleteMutationsFor(long importId, int maxResults) {
		final String queryString;
		if (dialect.getClass().getSimpleName().startsWith("Oracle")) {    // ah, c'est horrible, oui.
			queryString = "delete from EVENEMENT_RF_MUTATION where IMPORT_ID = :id and rownum <= " + maxResults;
		}
		else if (dialect.getClass().getSimpleName().startsWith("PostgreSQL")) {
			// http://stackoverflow.com/questions/5170546/how-do-i-delete-a-fixed-number-of-rows-with-sorting-in-postgresql
			queryString = "delete from EVENEMENT_RF_MUTATION where ctid in (select ctid from EVENEMENT_RF_MUTATION where IMPORT_ID = :id limit " + maxResults + ")";
		}
		else {
			throw new IllegalArgumentException("Type de dialect inconnu = [" + dialect.getClass() + "]");
		}
		final Query query = getCurrentSession().createSQLQuery(queryString);
		query.setParameter("id", importId);
		// query.setMaxResults(maxResults); le maxResults ne fonctionne *pas* avec les deletes !
		return query.executeUpdate();
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
