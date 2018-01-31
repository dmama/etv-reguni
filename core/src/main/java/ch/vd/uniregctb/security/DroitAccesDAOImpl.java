package ch.vd.uniregctb.security;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.dao.support.DataAccessUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BaseDAOImpl;
import ch.vd.uniregctb.common.pagination.ParamPagination;
import ch.vd.uniregctb.tiers.DroitAcces;

public class DroitAccesDAOImpl extends BaseDAOImpl<DroitAcces, Long> implements DroitAccesDAO {

	public DroitAccesDAOImpl() {
		super(DroitAcces.class);
	}

	@Override
	@SuppressWarnings("unchecked")
	public DroitAcces getDroitAcces(long operateurId, long tiersId, RegDate date) {
		final String query = "from DroitAcces da where da.tiers.id = :tiersId and da.noIndividuOperateur = :operId and da.annulationDate is null and da.dateDebut <= :dateRef and (da.dateFin is null or da.dateFin >= :dateRef) order by da.dateDebut desc";
		final List<DroitAcces> list = find(query,
		                                   buildNamedParameters(Pair.of("tiersId", tiersId),
		                                                        Pair.of("operId", operateurId),
		                                                        Pair.of("dateRef", date)),
		                                   null);
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

	@SuppressWarnings("unchecked")
	@Override
	public List<DroitAcces> getDroitsAcces(final long noIndividuOperateur, final ParamPagination paramPagination) {
		final String query = "from DroitAcces da where da.noIndividuOperateur = :operId order by da.annulationDate desc, da.dateDebut desc, da.id";
		final List<DroitAcces> list;
		if (paramPagination == null) {
			list = find(query, buildNamedParameters(Pair.of("operId", noIndividuOperateur)), null);
		}
		else {
			final Session session = getCurrentSession();
			final Query q = session.createQuery(query);
			q.setLong("operId", noIndividuOperateur);
			q.setFirstResult(paramPagination.getSqlFirstResult());
			q.setMaxResults(paramPagination.getSqlMaxResults());
			list = q.list();
		}
		return list;
	}

	@Override
	public Integer getDroitAccesCount(long noIndividuOperateur) {
		return DataAccessUtils.intResult(find("select count(*) from DroitAcces da where da.noIndividuOperateur = :operId",
		                                      buildNamedParameters(Pair.of("operId", noIndividuOperateur)),
		                                      null));
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Long> getIdsDroitsAcces(long noIndividuOperateur) {
		final String query = " select da.id from DroitAcces da where da.noIndividuOperateur = :operId";
		return find(query, buildNamedParameters(Pair.of("operId", noIndividuOperateur)), null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<DroitAcces> getDroitsAccessTiers(long tiersId) {
		final String query = "from DroitAcces da where da.tiers.id = :tiersId";
		return find(query, buildNamedParameters(Pair.of("tiersId", tiersId)), null);
	}


	/**
	 * @param date
	 *            date de validité des droits d'accès. Cette date est obligatoire.
	 * @return la liste de tous les droits d'accès définis sur le tiers spécifié.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<DroitAcces> getDroitsAccessTiers(long tiersId, RegDate date) {
		final String query = "from DroitAcces da where da.tiers.id = :tiersId and da.dateDebut <= :dateRef and (da.dateFin is null or da.dateFin >= :dateRef)";
		return find(query,
		            buildNamedParameters(Pair.of("tiersId", tiersId),
		                                 Pair.of("dateRef", date)),
		            null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<DroitAcces> getDroitsAccessTiers(final Set<Long> ids, final RegDate date) {
		if (ids == null || ids.isEmpty()) {
			return Collections.emptyList();
		}
		final Session session = getCurrentSession();
		final Query query = session.createQuery("from DroitAcces da where da.tiers.id in (:ids) and da.dateDebut <= :date and (da.dateFin is null or da.dateFin >= :date)");
		query.setParameterList("ids", ids);
		query.setParameter("date", date);
		return query.list();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Set<Long> getContribuablesControles() {

		final HashSet<Long> results = new HashSet<>();
		final Session session = getCurrentSession();
		final RegDate today = RegDate.get();

		// récupère les ids des contribuables avec droits d'accès directs
		{
			final String query = "select da.tiers.id from DroitAcces da where da.annulationDate is null and da.dateDebut <= :today and (da.dateFin is null or da.dateFin >= :today)";
			final Query queryObject = session.createQuery(query);
			queryObject.setParameter("today", today);
			results.addAll(queryObject.list());
		}

		// ajoute les ids des ménages communs ayant (ou ayant eu) pour membre une personne physique avec droits d'accès
		{
			final String query = "select am.objetId from AppartenanceMenage am, DroitAcces da where am.sujetId = da.tiers.id and am.annulationDate is null and da.annulationDate is null and da.dateDebut <= :today and (da.dateFin is null or da.dateFin >= :today)";
			final Query queryObject = session.createQuery(query);
			queryObject.setParameter("today", today);
			results.addAll(queryObject.list());
		}

		// ajoute les ids des établissements ayant (ou ayant eu) pour entité parente un contribuable avec droit d'accès
		{
			final String query = "select ae.objetId from ActiviteEconomique ae, DroitAcces da where ae.sujetId = da.tiers.id and ae.annulationDate is null and da.annulationDate is null and da.dateDebut <= :today and (da.dateFin is null or da.dateFin >= :today)";
			final Query queryObject = session.createQuery(query);
			queryObject.setParameter("today", today);
			results.addAll(queryObject.list());
		}

		return results;
	}
}
