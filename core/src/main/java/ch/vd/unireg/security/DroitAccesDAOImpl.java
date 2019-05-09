package ch.vd.unireg.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.support.DataAccessUtils;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.BaseDAOImpl;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.tiers.DroitAcces;

public class DroitAccesDAOImpl extends BaseDAOImpl<DroitAcces, Long> implements DroitAccesDAO {

	public DroitAccesDAOImpl() {
		super(DroitAcces.class);
	}

	@Override
	public DroitAcces getDroitAcces(@NotNull String visaOperateur, long tiersId, RegDate date) {
		final String query = "from DroitAcces da where da.tiers.id = :tiersId and da.visaOperateur = :visaOper and da.annulationDate is null and da.dateDebut <= :dateRef and (da.dateFin is null or da.dateFin >= :dateRef) order by da.dateDebut desc";
		final List<DroitAcces> list = find(query,
		                                   buildNamedParameters(Pair.of("tiersId", tiersId),
		                                                        Pair.of("visaOper", visaOperateur.toLowerCase()),
		                                                        Pair.of("dateRef", date)),
		                                   null);
		if (list.isEmpty()) {
			return null;
		}
		else {
			return list.get(0);
		}
	}

	@Override
	public List<DroitAcces> getDroitsAcces(@NotNull String visaOperateur) {
		return getDroitsAcces(visaOperateur, null);
	}

	@Override
	public List<DroitAcces> getDroitsAcces(@NotNull String visaOperateur, ParamPagination paramPagination) {
		final String query = "from DroitAcces da where da.visaOperateur = :visaOper order by da.annulationDate desc, da.dateDebut desc, da.id";
		final List<DroitAcces> list;
		if (paramPagination == null) {
			list = find(query, buildNamedParameters(Pair.of("visaOper", visaOperateur.toLowerCase())), null);
		}
		else {
			final Session session = getCurrentSession();
			final Query q = session.createQuery(query);
			q.setString("visaOper", visaOperateur.toLowerCase());
			q.setFirstResult(paramPagination.getSqlFirstResult());
			q.setMaxResults(paramPagination.getSqlMaxResults());
			list = q.list();
		}
		return list;
	}

	@Override
	public Integer getDroitAccesCount(@NotNull String visaOperateur) {
		return DataAccessUtils.intResult(find("select count(*) from DroitAcces da where da.visaOperateur = :visaOperateur",
		                                      buildNamedParameters(Pair.of("visaOperateur", visaOperateur.toLowerCase())),
		                                      null));
	}

	@Override
	@Deprecated
	public List<Long> getOperatorsIdsToMigrate() {

		final Query query = getCurrentSession().createQuery("select distinct noIndividuOperateur from DroitAcces where visaOperateur is null order by noIndividuOperateur asc");
		final List list = query.list();

		final List<Long> ids = new ArrayList<>(list.size());
		for (Object o : list) {
			if (o != null) {    // les nouveaux droits d'accès n'ont pas de numéro d'individu
				ids.add(((Number) o).longValue());
			}
		}
		return ids;
	}

	@Override
	public void updateVisa(long noOperateur, @NotNull String visaOperateur) {
		final Query query = getCurrentSession().createQuery("update DroitAcces set visaOperateur = :visa where noIndividuOperateur = :no");
		query.setParameter("visa", visaOperateur.toLowerCase());    // le visa est toujours stocké en minuscules
		query.setParameter("no", noOperateur);
		query.executeUpdate();
	}

	@Override
	public void cancelOperateur(Long noOperateur) {
		final Query query = getCurrentSession().createQuery("update DroitAcces set annulationDate = :now, annulationUser = :user where annulationDate is null and noIndividuOperateur = :no");
		query.setParameter("now", DateHelper.getCurrentDate());
		query.setParameter("user", AuthenticationHelper.getCurrentPrincipal());
		query.setParameter("no", noOperateur);
		query.executeUpdate();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Long> getIdsDroitsAcces(@NotNull String visaOperateur) {
		final String query = " select da.id from DroitAcces da where da.visaOperateur = :visaOperateur";
		return find(query, buildNamedParameters(Pair.of("visaOperateur", visaOperateur.toLowerCase())), null);
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

		//
		// ATTENTION !! En cas d'ajout de liens, il faut aussi mettre-à-jour la méthode SecurityProviderCache.onRelationshipChange() !
		//

		return results;
	}
}
