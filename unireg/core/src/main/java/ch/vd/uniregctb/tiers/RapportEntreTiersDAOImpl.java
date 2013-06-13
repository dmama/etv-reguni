package ch.vd.uniregctb.tiers;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.dao.support.DataAccessUtils;

import ch.vd.registre.base.dao.GenericDAOImpl;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.dbutils.QueryFragment;
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

		final QueryFragment fragment = new QueryFragment("SELECT rapport FROM RapportPrestationImposable rapport WHERE rapport.objetId = ?", Arrays.<Object>asList(numeroDebiteur));
		if (activesOnly) {
			fragment.add(" and rapport.dateFin is null and rapport.annulationDate is null");
		}
		fragment.add(paramPagination.buildOrderClause("rapport", "logCreationDate", true, null));

		final int firstResult = paramPagination.getSqlFirstResult();
		final int maxResult = paramPagination.getSqlMaxResults();

		final Session session = getCurrentSession();
		final Query queryObject = fragment.createQuery(session);

		queryObject.setFirstResult(firstResult);
		queryObject.setMaxResults(maxResult);

		return queryObject.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<RapportPrestationImposable> getRapportsPrestationImposable(final Long numeroDebiteur, final Long numeroSourcier, boolean activesOnly, boolean doNotAutoFlush) {
		final StringBuilder b = new StringBuilder();
		final FlushMode mode = (doNotAutoFlush ? FlushMode.MANUAL : null);
		b.append("SELECT rapport FROM RapportPrestationImposable rapport WHERE rapport.objetId = :debiteur and rapport.sujetId = :sourcier");
		if (activesOnly) {
			b.append(" and rapport.dateFin is null and rapport.annulationDate is null");
		}
		final String query = b.toString();

		final Session session = getCurrentSession();
		final Query queryObject = session.createQuery(query);
		queryObject.setParameter("debiteur", numeroDebiteur);
		queryObject.setParameter("sourcier", numeroSourcier);
		queryObject.setFlushMode(mode);
		return queryObject.list();
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
		return DataAccessUtils.intResult(find(query, null, null));
	}

	@Override
	public List<RapportEntreTiers> findBySujetAndObjet(final long tiersId, final boolean appartenanceMenageOnly, final boolean showHisto, final TypeRapportEntreTiers type,
	                                                   final ParamPagination pagination, final boolean excludeRapportPrestationImposable, final boolean excludeContactImpotSource) {

		final QueryFragment fragment = new QueryFragment("from RapportEntreTiers r where ((r.sujetId = " + tiersId + ") or (r.objetId = " + tiersId + "))");

		if (excludeRapportPrestationImposable && !excludeContactImpotSource) {
			fragment.add(" and r.class != RapportPrestationImposable ");
		}
		else if (excludeContactImpotSource && !excludeRapportPrestationImposable) {
			fragment.add(" and r.class != ContactImpotSource ");
		}
		else if (excludeContactImpotSource && excludeRapportPrestationImposable) {
			fragment.add(" and r.class != ContactImpotSource  and r.class != RapportPrestationImposable ");
		}
		if (appartenanceMenageOnly) {
			fragment.add(" and r.class = AppartenanceMenage");
		}
		if (!showHisto) {
			fragment.add(" and r.dateFin is null and r.annulationDate is null");
		}
		if (type != null) {
			fragment.add(" and r.class = " + type.getRapportClass().getSimpleName());
		}
		fragment.add(buildOrderClause(pagination));

		final Session session = getCurrentSession();
		final Query queryObject = fragment.createQuery(session);
		final int firstResult = pagination.getSqlFirstResult();
		final int maxResult = pagination.getSqlMaxResults();
		queryObject.setFirstResult(firstResult);
		queryObject.setMaxResults(maxResult);

		//noinspection unchecked
		return queryObject.list();
	}

	private static QueryFragment buildOrderClause(ParamPagination pagination) {
		return pagination.buildOrderClause("r", null, true, new ParamPagination.CustomOrderByGenerator() {
			@Override
			public boolean supports(String fieldName) {
				return "tiersId".equals(fieldName);
			}

			@Override
			public QueryFragment generate(String fieldName, ParamPagination pagination) {
				// pour le champs tiersId, on s'arrange pour trier selon l'ordre sujet-objet
				if (pagination.isSensAscending()) {
					return new QueryFragment("r.sujetId asc, r.objetId");
				}
				else {
					return new QueryFragment("r.sujetId desc, r.objetId");
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
		return DataAccessUtils.intResult(find(query, null, null));
	}
}