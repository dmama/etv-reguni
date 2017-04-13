package ch.vd.uniregctb.registrefoncier.dao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.ListUtils;
import org.hibernate.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BaseDAOImpl;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.key.DroitRFKey;

public class DroitRFDAOImpl extends BaseDAOImpl<DroitRF, Long> implements DroitRFDAO {

	protected DroitRFDAOImpl() {
		super(DroitRF.class);
	}

	@Override
	public @Nullable DroitRF find(@NotNull DroitRFKey key) {
		final Query query = getCurrentSession().createQuery("from DroitRF where masterIdRF = :masterIdRF");
		query.setParameter("masterIdRF", key.getMasterIdRF());
		return (DroitRF) query.uniqueResult();
	}

	@Nullable
	@Override
	public DroitRF findActive(@NotNull DroitRFKey key) {
		final Query query = getCurrentSession().createQuery("from DroitRF where masterIdRF = :masterIdRF and dateFin is null");
		query.setParameter("masterIdRF", key.getMasterIdRF());
		return (DroitRF) query.uniqueResult();
	}

	@SuppressWarnings("unchecked")
	@NotNull
	@Override
	public List<DroitRF> findForAyantDroit(long ayantDroitId, boolean fetchSituationsImmeuble) {

		// les droits de propriété
		final StringBuilder sqlProp = new StringBuilder();
		sqlProp.append("SELECT DISTINCT dp FROM DroitProprieteRF dp");
		if (fetchSituationsImmeuble) {
			sqlProp.append(" INNER JOIN FETCH dp.immeuble AS imm");
			sqlProp.append(" LEFT OUTER JOIN FETCH imm.situations");
		}
		sqlProp.append(" WHERE dp.ayantDroit.id = :ayantDroitId");

		final Query queryProp = getCurrentSession().createQuery(sqlProp.toString());
		queryProp.setParameter("ayantDroitId", ayantDroitId);

		// les servitudes
		final StringBuilder sqlServ = new StringBuilder();
		sqlServ.append("SELECT DISTINCT serv FROM ServitudeRF serv");
		sqlServ.append(" INNER JOIN serv.ayantDroits ayantDroit");
		if (fetchSituationsImmeuble) {
			sqlServ.append(" INNER JOIN FETCH serv.immeubles AS imm");
			sqlServ.append(" LEFT OUTER JOIN FETCH imm.situations");
		}
		sqlServ.append(" WHERE ayantDroit.id = :ayantDroitId");

		final Query queryServ = getCurrentSession().createQuery(sqlServ.toString());
		queryServ.setParameter("ayantDroitId", ayantDroitId);

		return ListUtils.union(queryProp.list(), queryServ.list());
	}

	@NotNull
	@Override
	public Set<String> findIdsServitudesActives() {
		final Set<String> set = new HashSet<>();

		// toutes les servitudes sans date de fin
		final Query query1 = getCurrentSession().createQuery("select masterIdRF from ServitudeRF where annulationDate is null and dateFin is null");
		set.addAll(query1.list());

		// toutes les servitudes avec des dates de fin dans le futur
		final Query query2 = getCurrentSession().createQuery("select masterIdRF from ServitudeRF where annulationDate is null and :today <= dateFin");
		query2.setParameter("today", RegDate.get());
		set.addAll(query2.list());

		return set;
	}
}
