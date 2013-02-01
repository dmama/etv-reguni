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

		Object[] criteria = {noTiersTuteur, noTiersPupille};
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
		b.append(paramPagination.buildOrderClause("rapport", "logCreationDate", true, null));
		final String query = b.toString();

		final int firstResult = paramPagination.getSqlFirstResult();
		final int maxResult = paramPagination.getSqlMaxResults();

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

	@Override
	public List<RapportPrestationImposable> getRapportsPrestationImposable(final Long numeroDebiteur, final Long numeroSourcier, boolean activesOnly, boolean doNotAutoFlush) {
		final StringBuilder b = new StringBuilder();
		final FlushMode mode = (doNotAutoFlush ? FlushMode.MANUAL : null);
		b.append("SELECT rapport FROM RapportPrestationImposable rapport WHERE rapport.objetId = :debiteur and rapport.sujetId = :sourcier");
		if (activesOnly) {
			b.append(" and rapport.dateFin is null and rapport.annulationDate is null");
		}
		final String query = b.toString();

		return getHibernateTemplate().executeWithNativeSession(new HibernateCallback<List<RapportPrestationImposable>>() {
			@Override
			public List<RapportPrestationImposable> doInHibernate(Session session) throws HibernateException, SQLException {

				final Query queryObject = session.createQuery(query);
				queryObject.setParameter("debiteur", numeroDebiteur);
				queryObject.setParameter("sourcier", numeroSourcier);
				queryObject.setFlushMode(mode);

				return queryObject.list();
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int countRapportsPrestationImposable(Long numeroDebiteur, boolean activesOnly) {

		String query = "select count(*) from RapportPrestationImposable rapport where rapport.objetId = " + numeroDebiteur;
		if (activesOnly) {
			query += " and rapport.dateFin is null and rapport.annulationDate is null";
		}
		return DataAccessUtils.intResult(getHibernateTemplate().find(query));
	}

	@Override
	public List<RapportEntreTiers> findBySujetAndObjet(final long tiersId, final boolean appartenanceMenageOnly, final boolean showHisto, final TypeRapportEntreTiers type,
	                                                   final ParamPagination pagination, final boolean excludeRapportPrestationImposable, final boolean excludeContactImpotSource) {
		return getHibernateTemplate().executeWithNativeSession(new HibernateCallback<List<RapportEntreTiers>>() {
			@Override
			public List<RapportEntreTiers> doInHibernate(Session session) throws HibernateException, SQLException {

				String query = "from RapportEntreTiers r where ((r.sujetId = " + tiersId + ") or (r.objetId = " + tiersId + "))";

				if (excludeRapportPrestationImposable && !excludeContactImpotSource) {
					query += " and r.class != RapportPrestationImposable ";
				}
				else if (excludeContactImpotSource && !excludeRapportPrestationImposable) {
					query += " and r.class != ContactImpotSource ";
				}
				else if (excludeContactImpotSource && excludeRapportPrestationImposable) {
					query += " and r.class != ContactImpotSource  and r.class != RapportPrestationImposable ";
				}
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

				final int firstResult = pagination.getSqlFirstResult();
				final int maxResult = pagination.getSqlMaxResults();
				queryObject.setFirstResult(firstResult);
				queryObject.setMaxResults(maxResult);

				//noinspection unchecked
				return queryObject.list();
			}
		});
	}

	private static String buildOrderClause(ParamPagination pagination) {
		return pagination.buildOrderClause("r", null, true, new ParamPagination.CustomOrderByGenerator() {
			@Override
			public boolean supports(String fieldName) {
				return "tiersId".equals(fieldName);
			}

			@Override
			public String generate(String fieldName, ParamPagination pagination) {
				// pour le champs tiersId, on s'arrange pour trier selon l'ordre sujet-objet
				if (pagination.isSensAscending()) {
					return "r.sujetId asc, r.objetId";
				}
				else {
					return "r.sujetId desc, r.objetId";
				}
			}
		});
	}

	@Override
	public int countBySujetAndObjet(long tiersId, boolean appartenanceMenageOnly, boolean showHisto, TypeRapportEntreTiers type, final boolean excludePrestationImposable,
	                                final boolean excludeContactImpotSource) {
		String query = "select count(*) from RapportEntreTiers r where ((r.sujetId = " + tiersId + ") or (r.objetId = " + tiersId + "))";

		if (excludePrestationImposable && !excludeContactImpotSource) {
			query += " and r.class != RapportPrestationImposable ";
		}
		else if (excludeContactImpotSource && !excludePrestationImposable) {
			query += " and r.class != ContactImpotSource ";
		}
		else if (excludeContactImpotSource && excludePrestationImposable) {
			query += " and r.class != RapportPrestationImposable and r.class != ContactImpotSource ";
		}

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