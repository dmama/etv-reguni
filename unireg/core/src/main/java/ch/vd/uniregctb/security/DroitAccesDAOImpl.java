package ch.vd.uniregctb.security;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.orm.hibernate3.HibernateCallback;

import ch.vd.registre.base.dao.GenericDAOImpl;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.tiers.DroitAcces;

public class DroitAccesDAOImpl extends GenericDAOImpl<DroitAcces, Long> implements DroitAccesDAO {

	public DroitAccesDAOImpl() {
		super(DroitAcces.class);
	}

	@Override
	@SuppressWarnings("unchecked")
	public DroitAcces getDroitAcces(long operateurId, long tiersId, RegDate date) {
		Object[] criteria = {
				tiersId, operateurId, date.index(), date.index()
		};
		String query = "from DroitAcces da where da.tiers.id = ? and da.noIndividuOperateur = ? and da.annulationDate is null and da.dateDebut <= ? and (da.dateFin is null or da.dateFin >= ?) order by da.dateDebut desc";
		List<DroitAcces> list = getHibernateTemplate().find(query, criteria);
		if (list.isEmpty()) {
			return null;
		}
		else {
			return list.get(0);
		}
	}

	/**
	 * Renvoie la liste des droits d'acces d'un utilisateur
	 * @param noIndividuOperateur
	 * @return
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<DroitAcces> getDroitsAcces(long noIndividuOperateur) {
		return getDroitsAcces(noIndividuOperateur, null);
	}

	@Override
	public List<DroitAcces> getDroitsAcces(final long noIndividuOperateur, final ParamPagination paramPagination) {
		Object[] criteria = {noIndividuOperateur};
		final String query = "from DroitAcces da where da.noIndividuOperateur = ? order by da.annulationDate desc, da.dateDebut desc, da.id";
		List<DroitAcces> list;
		if (paramPagination == null) {
			list = getHibernateTemplate().find(query, criteria);
		} else {
			list = getHibernateTemplate().executeWithNativeSession(new HibernateCallback<List<DroitAcces>>() {
				@Override
				public List<DroitAcces> doInHibernate(Session session) throws HibernateException, SQLException {
					Query q = session.createQuery(query);
					q.setLong(0, noIndividuOperateur);
					q.setFirstResult(paramPagination.getSqlFirstResult());
					q.setMaxResults(paramPagination.getSqlMaxResults());
					return q.list();
				}
			});

		}
		return list;
	}

	@Override
	public Integer getDroitAccesCount(long noIndividuOperateur) {
		Object[] criteria = {noIndividuOperateur};
		return DataAccessUtils.intResult(getHibernateTemplate().find("select count (*) from DroitAcces da where da.noIndividuOperateur = ?",criteria));
	}

	@Override
	public List<Long> getIdsDroitsAcces(long noIndividuOperateur) {
		Object[] criteria = {noIndividuOperateur};
		String query = " select da.id from DroitAcces da where da.noIndividuOperateur = ? ";
		List<Long> list = getHibernateTemplate().find(query, criteria);
		return list;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<DroitAcces> getDroitsAccessTiers(long tiersId) {
		Object[] criteria = {tiersId};
		String query = "from DroitAcces da where da.tiers.id = ?";
		List<DroitAcces> list = getHibernateTemplate().find(query, criteria);
		return list;
	}


	/**
	 * @param date
	 *            date de validité des droits d'accès. Cette date est obligatoire.
	 * @return la liste de tous les droits d'accès définis sur le tiers spécifié.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<DroitAcces> getDroitsAccessTiers(long tiersId, RegDate date) {
		Object[] criteria = {tiersId, date.index(), date.index()};
		String query = "from DroitAcces da where da.tiers.id = ? and da.dateDebut <= ? and (da.dateFin is null or da.dateFin >= ?)";
		List<DroitAcces> list = getHibernateTemplate().find(query, criteria);
		return list;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<DroitAcces> getDroitsAccessTiers(final List<Long> ids, final RegDate date) {
		if (ids == null || ids.isEmpty()) {
			return Collections.emptyList();
		}
		final List<DroitAcces> list = getHibernateTemplate().executeWithNativeSession(new HibernateCallback<List<DroitAcces>>() {
			@Override
			public List<DroitAcces> doInHibernate(Session session) throws HibernateException {
				Query query = session
						.createQuery("from DroitAcces da where da.tiers.id in (:ids) and da.dateDebut <= :date and (da.dateFin is null or da.dateFin >= :date)");
				query.setParameterList("ids", ids);
				query.setParameter("date", date.index());
				return query.list();
			}
		});
		return list;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Set<Long> getContribuablesControles() {

		final HashSet<Long> results = new HashSet<Long>();

		// récupère les ids des personnes physiques avec droits d'accès
		{
			final String query = "select da.tiers.id from DroitAcces da where da.annulationDate is null and da.dateDebut <= :today and (da.dateFin is null or da.dateFin >= :today)";
			final RegDate today = RegDate.get();

			final List<Long> ids = getHibernateTemplate().executeWithNativeSession(new HibernateCallback<List<Long>>() {
				@Override
				public List<Long> doInHibernate(Session session) throws HibernateException {
					Query queryObject = session.createQuery(query);
					queryObject.setParameter("today", today.index());
					return queryObject.list();
				}
			});
			results.addAll(ids);
		}

		// ajoute les ids des ménages communs ayant (ou ayant eu) pour membre une personne physique avec droits d'accès
		{
			final String query = "select am.objetId from AppartenanceMenage am, DroitAcces da where am.sujetId = da.tiers.id and am.annulationDate is null and da.annulationDate is null and da.dateDebut <= :today and (da.dateFin is null or da.dateFin >= :today)";
			final RegDate today = RegDate.get();

			final List<Long> ids = (List<Long>) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
				@Override
				public Object doInHibernate(Session session) throws HibernateException {
					Query queryObject = session.createQuery(query);
					queryObject.setParameter("today", today.index());
					return queryObject.list();
				}
			});
			results.addAll(ids);
		}

		return results;
	}
}
