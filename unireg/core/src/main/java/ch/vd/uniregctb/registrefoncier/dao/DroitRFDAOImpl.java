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
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.key.DroitRFKey;

public class DroitRFDAOImpl extends BaseDAOImpl<DroitRF, Long> implements DroitRFDAO {

	protected DroitRFDAOImpl() {
		super(DroitRF.class);
	}

	@Override
	public @Nullable DroitRF find(@NotNull DroitRFKey key) {
		final Query query = getCurrentSession().createQuery("from DroitRF where masterIdRF = :masterIdRF and versionIdRF = :versionIdRF");
		query.setParameter("masterIdRF", key.getMasterIdRF());
		query.setParameter("versionIdRF", key.getVersionIdRF());
		return (DroitRF) query.uniqueResult();
	}

	@Nullable
	@Override
	public DroitRF findActive(@NotNull DroitRFKey key) {
		final Query query = getCurrentSession().createQuery("from DroitRF where masterIdRF = :masterIdRF and versionIdRF = :versionIdRF and dateFin is null");
		query.setParameter("masterIdRF", key.getMasterIdRF());
		query.setParameter("versionIdRF", key.getVersionIdRF());
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
	public Set<DroitRFKey> findIdsServitudesActives() {
		final Set<DroitRFKey> set = new HashSet<>();

		// toutes les servitudes sans date de fin
		final Query query1 = getCurrentSession().createQuery("select masterIdRF, versionIdRF from ServitudeRF where annulationDate is null and dateFin is null");

		//noinspection unchecked
		final List<Object[]> rows1 = query1.list();
		for (Object[] row : rows1) {
			set.add(new DroitRFKey((String) row[0], (String) row[1]));
		}

		// toutes les servitudes avec des dates de fin dans le futur
		final Query query2 = getCurrentSession().createQuery("select masterIdRF, versionIdRF from ServitudeRF where annulationDate is null and :today <= dateFin");
		query2.setParameter("today", RegDate.get());

		//noinspection unchecked
		final List<Object[]> rows2 = query2.list();
		for (Object[] row : rows2) {
			set.add(new DroitRFKey((String) row[0], (String) row[1]));
		}

		return set;
	}

	@Override
	public @Nullable DroitProprieteRF findDroitPrecedent(@NotNull DroitRFKey key) {
		// on veut retourner le droit immédiatement précédant le droit spécifié par la clé. Il faut donc :
		//  - filtrer sur le même masterIdRF
		//  - exclure le droit courant (= celui qui correspond au versionIdRF de la clé)
		//  - retourner le droit le plus récemment créé (= tri sur les ids, car on ne peut pas se baser sur les versionIdRF qui sont des codes de hashage sans valeur d'ordonnancement)
		final Query query = getCurrentSession().createQuery("from DroitProprieteRF where annulationDate is null and masterIdRF = :masterIdRF and versionIdRF != :versionIdRF order by id desc");
		query.setParameter("masterIdRF", key.getMasterIdRF());
		query.setParameter("versionIdRF", key.getVersionIdRF());
		query.setMaxResults(1);
		return (DroitProprieteRF) query.uniqueResult();
	}
}
