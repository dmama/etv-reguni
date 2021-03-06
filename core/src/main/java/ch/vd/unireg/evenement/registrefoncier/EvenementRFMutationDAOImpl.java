package ch.vd.unireg.evenement.registrefoncier;

import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.dialect.Dialect;
import org.hibernate.query.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.BaseDAOImpl;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.dbutils.QueryFragment;

public class EvenementRFMutationDAOImpl extends BaseDAOImpl<EvenementRFMutation, Long> implements EvenementRFMutationDAO {

	private Dialect dialect;

	protected EvenementRFMutationDAOImpl() {
		super(EvenementRFMutation.class);
	}

	public void setDialect(Dialect dialect) {
		this.dialect = dialect;
	}

	@NotNull
	@Override
	public List<Long> findIds(long importId, @NotNull TypeEntiteRF typeEntite, @NotNull Collection<EtatEvenementRF> etats, @NotNull Collection<TypeMutationRF> typesMutation) {
		final Query query = getCurrentSession().createQuery("select id from EvenementRFMutation where typeEntite = :typeEntite and parentImport.id = :importId and etat in (:etats) and typeMutation in (:typesMutation)");
		query.setParameter("importId", importId);
		query.setParameter("typeEntite", typeEntite);
		query.setParameterList("etats", etats);
		query.setParameterList("typesMutation", typesMutation);
		//noinspection unchecked
		return query.list();
	}

	@Override
	public long count(long importId, @NotNull TypeEntiteRF typeEntite, @NotNull TypeMutationRF typeMutation) {
		final Query query = getCurrentSession().createQuery("select count(*) from EvenementRFMutation where parentImport.id = :importId and typeEntite = :typeEntite and typeMutation = :typeMutation");
		query.setParameter("importId", importId);
		query.setParameter("typeEntite", typeEntite);
		query.setParameter("typeMutation", typeMutation);
		return ((Number) query.uniqueResult()).longValue();
	}

	@NotNull
	@Override
	public Iterator<String> findRfIds(long importId, @NotNull TypeEntiteRF typeEntite, @NotNull TypeMutationRF typeMutation) {
		final Query query = getCurrentSession().createQuery("select idRF from EvenementRFMutation where parentImport.id = :importId and typeEntite = :typeEntite and typeMutation = :typeMutation");
		query.setParameter("importId", importId);
		query.setParameter("typeEntite", typeEntite);
		query.setParameter("typeMutation", typeMutation);
		//noinspection unchecked
		return query.iterate();
	}

	@Nullable
	@Override
	public EvenementRFMutation find(long importId, @NotNull TypeEntiteRF typeEntite, @NotNull TypeMutationRF typeMutation, @NotNull String idRF) {
		final Query query = getCurrentSession().createQuery("from EvenementRFMutation where typeEntite = :typeEntite and typeMutation = :typeMutation and parentImport.id = :importId and idRF = :idRF");
		query.setParameter("importId", importId);
		query.setParameter("typeEntite", typeEntite);
		query.setParameter("typeMutation", typeMutation);
		query.setParameter("idRF", idRF);
		//noinspection unchecked
		return (EvenementRFMutation) query.uniqueResult();
	}

	@Override
	public int forceMutation(long mutId) {
		final Query query = getCurrentSession().createQuery("update EvenementRFMutation set etat = 'FORCE', logModifDate = :date, logModifUser = :user where id = :mutId and etat in ('A_TRAITER', 'EN_ERREUR')");
		query.setParameter("mutId", mutId);
		query.setParameter("date", new Date());
		query.setParameter("user", AuthenticationHelper.getCurrentPrincipal());
		return query.executeUpdate();
	}

	@Override
	public int forceMutations(long importId) {
		final Query query = getCurrentSession().createQuery("update EvenementRFMutation set etat = 'FORCE' , logModifDate = :date, logModifUser = :user where parentImport.id = :importId and etat in ('A_TRAITER', 'EN_ERREUR')");
		query.setParameter("importId", importId);
		query.setParameter("date", new Date());
		query.setParameter("user", AuthenticationHelper.getCurrentPrincipal());
		return query.executeUpdate();
	}

	@Override
	public Map<EtatEvenementRF, Integer> countByState(long importId) {
		final Query query = getCurrentSession().createQuery("select etat, count(*) from EvenementRFMutation where parentImport.id = :importId group by etat");
		query.setParameter("importId", importId);
		final List<?> list = query.list();

		final Map<EtatEvenementRF, Integer> res = new EnumMap<>(EtatEvenementRF.class);
		list.forEach(o -> {
			Object[] row = (Object[]) o;
			final EtatEvenementRF key = (EtatEvenementRF) row[0];
			final Number count = (Number) row[1];
			res.merge(key, count.intValue(), (a, b) -> a + b);
		});
		return res;
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
		final Query query = getCurrentSession().createNativeQuery(queryString);
		query.setParameter("id", importId);
		// query.setMaxResults(maxResults); le maxResults ne fonctionne *pas* avec les deletes !
		return query.executeUpdate();
	}

	@Nullable
	@Override
	public Long findNextMutationsToProcess() {
		final Query query = getCurrentSession().createQuery("select parentImport.id from EvenementRFMutation where etat in ('A_TRAITER', 'EN_ERREUR') order by parentImport.dateEvenement asc");
		query.setMaxResults(1);
		final Object result = query.uniqueResult();
		return result == null ? null : ((Number) result).longValue();
	}

	@NotNull
	@Override
	public List<EvenementRFMutation> find(long importId, @Nullable List<EtatEvenementRF> etats, @NotNull ParamPagination pagination) {

		final QueryFragment fragment;
		if (etats == null || etats.isEmpty()) {
			fragment= new QueryFragment("from EvenementRFMutation evenement where parentImport.id = :importId", "importId", importId);
		}
		else {
			final Map<String, Object> params = new HashMap<>();
			params.put("importId", importId);
			params.put("etats", etats);
			fragment = new QueryFragment("from EvenementRFMutation evenement where parentImport.id = :importId and etat in (:etats)", params);
		}
		fragment.add(pagination.buildOrderClause("evenement", "id", true, null));

		final Query queryObject = fragment.createQuery(getCurrentSession());
		queryObject.setFirstResult(pagination.getSqlFirstResult());
		queryObject.setMaxResults(pagination.getSqlMaxResults());

		//noinspection unchecked
		return queryObject.list();
	}

	@Override
	public int count(long importId, @Nullable List<EtatEvenementRF> etats) {

		final String queryString;
		if (etats == null || etats.isEmpty()) {
			queryString = "select count(*) from EvenementRFMutation where parentImport.id = :importId";
		}
		else {
			queryString = "select count(*) from EvenementRFMutation where parentImport.id = :importId and etat in (:etats)";
		}

		final Query query = getCurrentSession().createQuery(queryString);
		query.setParameter("importId", importId);
		if (etats != null && !etats.isEmpty()) {
			query.setParameterList("etats", etats);
		}

		final Number o = (Number) query.uniqueResult();
		return o.intValue();
	}
}
