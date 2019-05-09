package ch.vd.unireg.registrefoncier.dao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.ListUtils;
import org.hibernate.query.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BaseDAOImpl;
import ch.vd.unireg.registrefoncier.DroitProprieteRF;
import ch.vd.unireg.registrefoncier.DroitRF;
import ch.vd.unireg.registrefoncier.key.DroitRFKey;

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
		final String sqlServ = "SELECT DISTINCT bene.servitude " +
				"FROM BeneficeServitudeRF bene " +
				"WHERE bene.annulationDate is null AND bene.ayantDroit.id = :ayantDroitId";

		final Query queryServ = getCurrentSession().createQuery(sqlServ);
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
	public @Nullable DroitProprieteRF findDroitPrecedentByMasterId(@NotNull DroitRFKey key) {
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

	@Override
	public @Nullable DroitProprieteRF findDroitPrecedentByAyantDroit(@NotNull DroitProprieteRF droit) {
		// on veut retourner le droit immédiatement précédant le droit spécifié. Il faut donc :
		//  - filtrer sur le même ayantDroitId
		//  - exclure les droits existants fermés
		//  - exclure le droit courant (même masterId/versionId)
		//  - retourner le droit le plus récemment créé (= tri sur les ids, car on ne peut pas se baser sur les versionIdRF qui sont des codes de hashage sans valeur d'ordonnancement)
		//
		// Note : le droit reçu en paramètre vient certainement d'être construit à partir des données d'import RF : il ne contient pas
		//        encore de dates techniques de début et de fin (à ce stade, on ne sait pas encore s'il s'agit d'un droit inchangé, d'un nouveau
		//        droit ou d'un droit modifié, de toutes façons). D'autre part, comme l'import est en cours de traitement, les droits existants dans
		//        la DB n'ont pas encore été mis-à-jour : pour un droit qui n'existe plus dans l'import et devrait être fermé, sa date de fin
		//        technique est encore nulle.
		//        Logiquement, on devrait donc rechercher le droit immédiatement précédent au droit spécifié et s'assurer que la date de fin technique
		//        du droit précédent = veille de la date de début technique du droit spécifié. Mais comme il n'y a pas de date de début/fin technique,
		//        on prend un raccourci et on spécifie simplement que la date de fin technique du droit précédent ne doit pas être renseignée
		//        (= le droit était valide avant l'import, pour ignorer des droits fermés plus anciens).
		final String queryString = "from DroitProprieteRF where annulationDate is null " +
				"and ayantDroit.id = :ayantDroitId " +
				"and immeuble.id = :immeubleId " +
				"and dateFin is null " +
				"and (masterIdRF != :masterIdRF or versionIdRF != :versionIdRF) order by id desc";

		final Query query = getCurrentSession().createQuery(queryString);
		query.setParameter("ayantDroitId", droit.getAyantDroit().getId());
		query.setParameter("immeubleId", droit.getImmeuble().getId());
		query.setParameter("masterIdRF", droit.getMasterIdRF());
		query.setParameter("versionIdRF", droit.getVersionIdRF());
		query.setMaxResults(1);

		return (DroitProprieteRF) query.uniqueResult();
	}
}
