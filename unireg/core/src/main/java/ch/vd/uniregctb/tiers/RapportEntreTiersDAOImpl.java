package ch.vd.uniregctb.tiers;

import java.sql.SQLException;
import java.util.List;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.orm.hibernate3.HibernateCallback;

import ch.vd.registre.base.dao.GenericDAOImpl;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class RapportEntreTiersDAOImpl extends GenericDAOImpl<RapportEntreTiers, Long> implements RapportEntreTiersDAO {

	public RapportEntreTiersDAOImpl() {
		super(RapportEntreTiers.class);
	}

	@Override
	public List<RapportEntreTiers> getRepresentationLegaleAvecTuteurEtPupille(Long noTiersTuteur, Long noTiersPupille) {
		return getRepresentationLegaleAvecTuteurEtPupille(noTiersTuteur, noTiersPupille, false);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<RapportEntreTiers> getRepresentationLegaleAvecTuteurEtPupille(Long noTiersTuteur, Long noTiersPupille, boolean doNotAutoFlush) {

		Object[] criteria = { noTiersTuteur,noTiersPupille };
		String query = "from RapportEntreTiers ret where ret.objetId = ? and ret.sujetId = ?";
		final FlushMode mode = (doNotAutoFlush ? FlushMode.MANUAL : null);

		return (List<RapportEntreTiers>) find(query, criteria, mode);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<RapportPrestationImposable> getRapportsPrestationImposable(final Long numeroDebiteur, ParamPagination paramPagination, boolean activesOnly) {

		final StringBuilder b = new StringBuilder();
		b.append("SELECT rapport FROM RapportPrestationImposable rapport WHERE rapport.objetId = :debiteur");
		if (activesOnly) {
			b.append(" and rapport.dateFin is null and rapport.annulationDate is null");
		}
		if (paramPagination.getChamp() == null) {
			b.append(" ORDER BY rapport.logCreationDate");
		}
		else {
			b.append(" ORDER BY rapport.").append(paramPagination.getChamp());
		}
		if (!paramPagination.isSensAscending()) {
			b.append(" DESC");
		}
		final String query = b.toString();

		final int firstResult = (paramPagination.getNumeroPage() - 1) * paramPagination.getTaillePage();
		final int maxResult = paramPagination.getTaillePage();

		return getHibernateTemplate().executeWithNativeSession(new HibernateCallback<List<RapportPrestationImposable>>() {
			@Override
			public List<RapportPrestationImposable> doInHibernate(Session session) throws HibernateException, SQLException {

				final Query queryObject = session.createQuery(query);
				queryObject.setParameter("debiteur", numeroDebiteur);

				queryObject.setFirstResult(firstResult);
				queryObject.setMaxResults(maxResult);

				return queryObject.list();
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int countRapportsPrestationImposable(Long numeroDebiteur, boolean activesOnly){

		String query = "select count(*) from RapportPrestationImposable rapport where rapport.objetId = " + numeroDebiteur ;
		if (activesOnly) {
			query += " and rapport.dateFin is null and rapport.annulationDate is null";
		}
		return DataAccessUtils.intResult(getHibernateTemplate().find(query));
	}

	@Override
	public List<RapportEntreTiers> findBySujetAndObjet(final long tiersId, final boolean appartenanceMenageOnly, final boolean showHisto, final TypeRapportEntreTiers type, final ParamPagination pagination) {
		return getHibernateTemplate().executeWithNativeSession(new HibernateCallback<List<RapportEntreTiers>>() {
			@Override
			public List<RapportEntreTiers> doInHibernate(Session session) throws HibernateException, SQLException {

				String query = "from RapportEntreTiers r where ((r.sujetId = " + tiersId + ") or (r.objetId = " + tiersId + "))" +
						" and r.class != RapportPrestationImposable and r.class != ContactImpotSource";
				if (appartenanceMenageOnly) {
					query += " and r.class = AppartenanceMenage";
				}
				if (!showHisto) {
					query += " and r.dateFin is null and r.annulationDate is null";
				}
				if (type != null) {
					query += " and r.class = " + type.getRapportClass().getSimpleName();
				}
				query += buildOrderClause(pagination);
				final Query queryObject = session.createQuery(query);

				final int firstResult = (pagination.getNumeroPage() - 1) * pagination.getTaillePage();
				final int maxResult = pagination.getTaillePage();
				queryObject.setFirstResult(firstResult);
				queryObject.setMaxResults(maxResult);

				//noinspection unchecked
				return queryObject.list();
			}
		});
	}

	private static String buildOrderClause(ParamPagination pagination) {
		String clauseOrder;
		final String champ = pagination.getChamp();
		if (champ != null) {
			if (champ.equals("type")) {
				clauseOrder = " order by r.class";
			}
			else if (champ.equals("tiersId")) {
				// pour le champs tiersId, on s'arrange pour trier selon l'ordre sujet-objet
				if (pagination.isSensAscending()) {
					clauseOrder = " order by r.sujetId asc, r.objetId";
				}
				else {
					clauseOrder = " order by r.sujetId desc, r.objetId";
				}
			}
			else {
				clauseOrder = " order by r." + champ;
			}

			if (pagination.isSensAscending()) {
				clauseOrder = clauseOrder + " asc";
			}
			else {
				clauseOrder = clauseOrder + " desc";
			}
		}
		else {
			clauseOrder = " order by r.id asc";

		}
		return clauseOrder;
	}

	@Override
	public int countBySujetAndObjet(long tiersId, boolean appartenanceMenageOnly, boolean showHisto, TypeRapportEntreTiers type) {
		String query = "select count(*) from RapportEntreTiers r where ((r.sujetId = " + tiersId + ") or (r.objetId = " + tiersId + "))" +
				" and r.class != RapportPrestationImposable and r.class != ContactImpotSource";
		if (appartenanceMenageOnly) {
			query += " and r.class = AppartenanceMenage";
		}
		if (!showHisto) {
			query += " and r.dateFin is null and r.annulationDate is null";
		}
		if (type != null) {
			query += " and r.class = " + type.getRapportClass().getSimpleName();
		}
		return DataAccessUtils.intResult(getHibernateTemplate().find(query));
	}
}