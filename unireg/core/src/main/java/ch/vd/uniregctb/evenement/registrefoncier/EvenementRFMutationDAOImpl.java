package ch.vd.uniregctb.evenement.registrefoncier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.dialect.Dialect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.BaseDAOImpl;

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
	public List<Long> findIds(long importId, @NotNull EvenementRFMutation.TypeEntite typeEntite, @NotNull EtatEvenementRF... etats) {
		final Query query = getCurrentSession().createQuery("select id from EvenementRFMutation where typeEntite = :typeEntite and parentImport.id = :importId and etat in (:etats)");
		query.setParameter("importId", importId);
		query.setParameter("typeEntite", typeEntite);
		query.setParameterList("etats", etats);
		//noinspection unchecked
		return query.list();
	}

	@Nullable
	@Override
	public EvenementRFMutation find(long importId, @NotNull EvenementRFMutation.TypeEntite typeEntite, @NotNull String idImmeubleRF) {
		final Query query = getCurrentSession().createQuery("from EvenementRFMutation where typeEntite = :typeEntite and parentImport.id = :importId and idImmeubleRF = :idImmeubleRF");
		query.setParameter("importId", importId);
		query.setParameter("typeEntite", typeEntite);
		query.setParameter("idImmeubleRF", idImmeubleRF);
		//noinspection unchecked
		return (EvenementRFMutation) query.uniqueResult();
	}

	@Override
	public int forceMutations(long importId) {
		final Query query = getCurrentSession().createQuery("update EvenementRFMutation set etat = 'FORCE' where parentImport.id = :importId and etat in ('A_TRAITER', 'EN_ERREUR')");
		query.setParameter("importId", importId);
		return query.executeUpdate();
	}

	@Override
	public Map<EtatEvenementRF, Integer> countByState(long importId) {
		final Query query = getCurrentSession().createQuery("select etat, count(*) from EvenementRFMutation where parentImport.id = :importId group by etat");
		query.setParameter("importId", importId);
		final List<?> list = query.list();

		final Map<EtatEvenementRF, Integer> res = new HashMap<>();
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
		final Query query = getCurrentSession().createSQLQuery(queryString);
		query.setParameter("id", importId);
		// query.setMaxResults(maxResults); le maxResults ne fonctionne *pas* avec les deletes !
		return query.executeUpdate();
	}
}
