package ch.vd.unireg.evenement.registrefoncier;

import java.util.List;

import org.hibernate.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BaseDAOImpl;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.dbutils.QueryFragment;

public class EvenementRFImportDAOImpl extends BaseDAOImpl<EvenementRFImport, Long> implements EvenementRFImportDAO {

	protected EvenementRFImportDAOImpl() {
		super(EvenementRFImport.class);
	}

	@Nullable
	@Override
	public EvenementRFImport findNextImportToProcess(TypeImportRF type) {
		final Query query = getCurrentSession().createQuery("from EvenementRFImport where type = :type and etat in ('A_TRAITER', 'EN_ERREUR') order by dateEvenement asc");
		query.setParameter("type", type);
		query.setMaxResults(1);
		return (EvenementRFImport) query.uniqueResult();
	}

	@Nullable
	@Override
	public EvenementRFImport findOldestImportWithUnprocessedMutations(long importId, TypeImportRF type) {
		final Query query = getCurrentSession().createQuery("from EvenementRFImport where type = :type and id in (select parentImport.id from EvenementRFMutation where parentImport.id != :importId and etat in ('A_TRAITER', 'EN_ERREUR')) order by dateEvenement asc");
		query.setParameter("importId", importId);
		query.setParameter("type", type);
		query.setMaxResults(1);
		return (EvenementRFImport) query.uniqueResult();
	}

	@Nullable
	@Override
	public RegDate findValueDateOfOldestProcessedImport(long importId, TypeImportRF type) {
		final Query query = getCurrentSession().createQuery("select max(dateEvenement) from EvenementRFImport where id != :importId and type = :type and etat in ('EN_TRAITEMENT', 'TRAITE', 'EN_ERREUR', 'FORCE')");
		query.setParameter("importId", importId);
		query.setParameter("type", type);
		return (RegDate) query.uniqueResult();
	}

	@Override
	public @NotNull List<EvenementRFImport> find(@NotNull TypeImportRF type) {
		final Query query = getCurrentSession().createQuery("from EvenementRFImport where type = :type");
		query.setParameter("type", type);
		//noinspection unchecked
		return query.list();
	}

	@Override
	@Nullable
	public EvenementRFImport find(@NotNull TypeImportRF type, @NotNull RegDate dateEvenement) {
		final Query query = getCurrentSession().createQuery("from EvenementRFImport where type = :type and dateEvenement = :dateEvenement");
		query.setParameter("type", type);
		query.setParameter("dateEvenement", dateEvenement);
		//noinspection unchecked
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
		fragment.add(pagination.buildOrderClause("evenement", "dateEvenement", false, null));

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

	@Override
	public int fixAbnormalJVMTermination() {
		final Query query = getCurrentSession().createQuery("update EvenementRFImport set etat = 'EN_ERREUR', errorMessage = 'Abnormal JVM termination.' where etat = 'EN_TRAITEMENT'");
		return query.executeUpdate();
	}
}
