package ch.vd.uniregctb.evenement.registrefoncier;

import org.hibernate.Query;
import org.hibernate.dialect.Dialect;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.BaseDAOImpl;

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
}
