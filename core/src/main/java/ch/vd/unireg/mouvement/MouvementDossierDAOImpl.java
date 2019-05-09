package ch.vd.unireg.mouvement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.DateRange;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.BaseDAOImpl;
import ch.vd.unireg.common.HibernateQueryHelper;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.common.pagination.ParamSorting;
import ch.vd.unireg.type.TypeMouvement;

public class MouvementDossierDAOImpl extends BaseDAOImpl<MouvementDossier, Long> implements MouvementDossierDAO {

	private static final Logger LOGGER = LoggerFactory.getLogger(MouvementDossierDAOImpl.class);

	public MouvementDossierDAOImpl() {
		super(MouvementDossier.class);
	}

	/**
	 * Recherche tous les mouvements en fonction du numero de contribuable
	 *
	 * @param numero
	 * @param seulementTraites
	 * @param inclureMouvementsAnnules
	 * @return
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<MouvementDossier> findByNumeroDossier(long numero, boolean seulementTraites, boolean inclureMouvementsAnnules) {
		final MouvementDossierCriteria criteria = new MouvementDossierCriteria();
		criteria.setNoCtb(numero);
		criteria.setInclureMouvementsAnnules(inclureMouvementsAnnules);
		if (seulementTraites) {
			criteria.setEtatsMouvement(EtatMouvementDossier.getEtatsTraites());
		}
		return find(criteria, null);
	}

	/**
	 * Construit une requête HQL qui récupère les mouvements de dossiers correspondant aux critères donnés
	 *
	 * @param criteria critères de sélection des mouvements à récupérer
	 * @param idsOnly si <code>true</code>, la requête n'ira chercher que des ID, si <code>false</code>, des objets métier seront renvoyés
	 * @param sorting indications sur le tri souhaité des résultats de la recherche
	 * @param params Paramètres à remplir
	 * @return Requête HQL
	 */
	private static String buildFindHql(MouvementDossierCriteria criteria, boolean idsOnly, @Nullable ParamSorting sorting, Map<String, Object> params) {
		final StringBuilder b = new StringBuilder("SELECT mvt");
		if (idsOnly) {
			b.append(".id");
		}
		buildFromClause(criteria, b);
		buildWhereClause(criteria, b, params);
		buildOrderByClause(sorting, b);
		return b.toString();
	}

	private static String buildCountHql(MouvementDossierCriteria criteria, Map<String, Object> params) {
		final StringBuilder b = new StringBuilder("SELECT COUNT(mvt)");
		buildFromClause(criteria, b);
		buildWhereClause(criteria, b, params);
		return b.toString();
	}

	private static void buildOrderByClause(@Nullable ParamSorting sorting, StringBuilder b) {
		if (sorting != null && !StringUtils.isBlank(sorting.getField())) {
			b.append(" ORDER BY mvt.").append(sorting.getField());
			if (!sorting.isAscending()) {
				b.append(" DESC");
			}
		}
		else {
			b.append(" ORDER BY mvt.contribuable.id ASC");
		}
	}

	private static void buildWhereClause(MouvementDossierCriteria criteria, StringBuilder b, Map<String, Object> params) {

		b.append(" WHERE 1=1");

		// contribuable associé
		buildWhereClausePartieCtb(criteria, b, params);

		// date de mouvement
		buildWhereClausePartieDateMouvement(criteria, b, params);

		// annulés ?
		buildWhereClausePartieAnnule(criteria, b);

		// état du mouvement
		buildWhereClausePartieEtatMouvement(criteria, b, params);

		// localisation des mouvements de réception
		buildWhereClausePartieLocalisationReception(criteria, b, params);

		// individu destinataire d'un envoi
		buildWhereClausePartieIndividuDestinataire(criteria, b, params);

		// collectivité administrative destinataire d'un envoi
		buildWhereClausePartieCollectiviteAdministrativeDestinataire(criteria, b, params);

		// collectivité administrative initiatrice (filtrage OID)
		buildWhereClausePartieCollectiviteAdministrativeInitiatrice(criteria, b, params);

		// filtre sur l'historique des mouvements pour chaque dossier
		buildWhereClausePartieEtatActuelSeulement(criteria, b, params);
	}

	private static void buildWhereClausePartieEtatActuelSeulement(MouvementDossierCriteria criteria, StringBuilder b, Map<String, Object> params) {
		if (criteria.isSeulementDerniersMouvements()) {
			b.append(" AND NOT EXISTS (");
			{
				// tous les états dits "traités"
				b.append("SELECT other.id FROM MouvementDossier other WHERE other.annulationDate IS NULL AND other.contribuable.numero = mvt.contribuable.numero AND other.etat IN (:etats) AND (other.dateMouvement > mvt.dateMouvement OR (other.dateMouvement = mvt.dateMouvement AND other.logModifDate > mvt.logModifDate))");
				params.put("etats", EtatMouvementDossier.getEtatsTraites());
			}
			b.append(')');
		}
	}

	private static void buildWhereClausePartieCollectiviteAdministrativeInitiatrice(MouvementDossierCriteria criteria, StringBuilder b, Map<String, Object> params) {
		if (criteria.getIdCollAdministrativeInitiatrice() != null) {
			if (criteria.getTypeMouvement() == null) {
				final String part = String.format(" AND (EXISTS (SELECT reception.id FROM %s reception WHERE reception.id=mvt.id AND reception.collectiviteAdministrativeReceptrice.id = :colAdmInit) OR EXISTS (SELECT envoi.id FROM %s envoi WHERE envoi.id=mvt.id AND envoi.collectiviteAdministrativeEmettrice.id = :colAdmInit))",
												ReceptionDossier.class.getSimpleName(), EnvoiDossier.class.getSimpleName());
				b.append(part);
				params.put("colAdmInit", criteria.getIdCollAdministrativeInitiatrice());
			}
			else if (criteria.getTypeMouvement() == TypeMouvement.EnvoiDossier) {
				b.append(" AND mvt.collectiviteAdministrativeEmettrice.id = :colAdmInit");
				params.put("colAdmInit", criteria.getIdCollAdministrativeInitiatrice());
			}
			else if (criteria.getTypeMouvement() == TypeMouvement.ReceptionDossier) {
				b.append(" AND mvt.collectiviteAdministrativeReceptrice.id = :colAdmInit");
				params.put("colAdmInit", criteria.getIdCollAdministrativeInitiatrice());
			}
		}
	}

	private static void buildWhereClausePartieCollectiviteAdministrativeDestinataire(MouvementDossierCriteria criteria, StringBuilder b, Map<String, Object> params) {
		if (criteria.getIdCollAdministrativeDestinataire() != null) {
			b.append(" AND type(mvt) = ").append(EnvoiDossierVersCollectiviteAdministrative.class.getSimpleName());
			b.append(" AND mvt.collectiviteAdministrativeDestinataire.id = :colAdmDest");
			params.put("colAdmDest", criteria.getIdCollAdministrativeDestinataire());
		}
	}

	private static void buildWhereClausePartieIndividuDestinataire(MouvementDossierCriteria criteria, StringBuilder b, Map<String, Object> params) {
		if (StringUtils.isNotBlank(criteria.getVisaDestinataire())) {
			b.append(" AND type(mvt) = ").append(EnvoiDossierVersCollaborateur.class.getSimpleName());
			b.append(" AND mvt.visaDestinataire = :visaDestinataire");
			params.put("visaDestinataire", criteria.getVisaDestinataire().toLowerCase());
		}
	}

	private static void buildWhereClausePartieLocalisationReception(MouvementDossierCriteria criteria, StringBuilder b, Map<String, Object> params) {
		if (criteria.getLocalisation() != null) {
			final Class<? extends ReceptionDossier> clazz;
			switch (criteria.getLocalisation()) {
				case PERSONNE:
					clazz = ReceptionDossierPersonnel.class;
					break;
				case ARCHIVES:
					clazz = ReceptionDossierArchives.class;
					break;
				case CLASSEMENT_GENERAL:
					clazz = ReceptionDossierClassementGeneral.class;
					break;
				case CLASSEMENT_INDEPENDANTS:
					clazz = ReceptionDossierClassementIndependants.class;
					break;
				default:
					throw new RuntimeException("Type de localisation non-supportée : " + criteria.getLocalisation());
			}

			b.append(" AND type(mvt) = ").append(clazz.getSimpleName());
			if (StringUtils.isNotBlank(criteria.getVisaRecepteur())) {
				b.append(" AND mvt.visaRecepteur = :visaRecepteur");
				params.put("visaRecepteur", criteria.getVisaRecepteur().toLowerCase());
			}
		}
	}

	@SuppressWarnings({"unchecked"})
	private static void buildWhereClausePartieEtatMouvement(MouvementDossierCriteria criteria, StringBuilder b, Map<String, Object> params) {

		final Collection<EtatMouvementDossier> etatsDemandes;
		if (criteria.isSeulementDerniersMouvements()) {
			// si on doit filtrer sur les derniers mouvements seulement, seuls les mouvements traités doivent revenir
			if (criteria.getEtatsMouvement() != null && !criteria.getEtatsMouvement().isEmpty()) {
				final Set<EtatMouvementDossier> temp = EnumSet.noneOf(EtatMouvementDossier.class);
				for (EtatMouvementDossier etat : criteria.getEtatsMouvement()) {
					if (etat.isTraite()) {
						temp.add(etat);
					}
				}
				etatsDemandes = temp;
			}
			else if (criteria.getEtatsMouvement() == null) {
			    etatsDemandes = EtatMouvementDossier.getEtatsTraites();
			}
			else {
				etatsDemandes = criteria.getEtatsMouvement();
			}
		}
		else {
			etatsDemandes = criteria.getEtatsMouvement();
		}

		if (etatsDemandes != null) {
			if (etatsDemandes.isEmpty()) {
				// aucun etat!
				b.append(" AND 1=0");
			}
			else {
				b.append(" AND mvt.etat in (:etats)");
				params.put("etats", etatsDemandes);
			}
		}
	}

	private static void buildWhereClausePartieAnnule(MouvementDossierCriteria criteria, StringBuilder b) {
		if (!criteria.isInclureMouvementsAnnules() || criteria.isSeulementDerniersMouvements()) {
			b.append(" AND mvt.annulationDate IS NULL");
		}
	}

	@SuppressWarnings({"unchecked"})
	private static void buildWhereClausePartieDateMouvement(MouvementDossierCriteria criteria, StringBuilder b, Map<String, Object> params) {
		final DateRange rangeDateMvt = criteria.getRangeDateMouvement();
		if (rangeDateMvt != null && (rangeDateMvt.getDateDebut() != null || rangeDateMvt.getDateFin() != null)) {
			b.append(" AND mvt.dateMouvement");
			if (rangeDateMvt.getDateDebut() != null && rangeDateMvt.getDateFin() != null) {
				b.append(" BETWEEN :dateMin AND :dateMax");
				params.put("dateMin", rangeDateMvt.getDateDebut());
				params.put("dateMax", rangeDateMvt.getDateFin());
			}
			else if (rangeDateMvt.getDateDebut() != null) {
				b.append(" >= :dateMin");
				params.put("dateMin", rangeDateMvt.getDateDebut());
			}
			else {
				b.append(" <= :dateMax");
				params.put("dateMax", rangeDateMvt.getDateFin());
			}
		}
	}

	@SuppressWarnings({"unchecked"})
	private static void buildWhereClausePartieCtb(MouvementDossierCriteria criteria, StringBuilder b, Map<String, Object> params) {
		if (criteria.getNoCtb() != null) {
			b.append(" AND mvt.contribuable.numero=:noCtb");
			params.put("noCtb", criteria.getNoCtb());
		}
	}

	private static void buildFromClause(MouvementDossierCriteria criteria, StringBuilder b) {
		b.append(" FROM ");
		if (criteria.getTypeMouvement() == TypeMouvement.EnvoiDossier) {
			b.append("EnvoiDossier");
		}
		else if (criteria.getTypeMouvement() == TypeMouvement.ReceptionDossier) {
			b.append("ReceptionDossier");
		}
		else {
			b.append("MouvementDossier");
		}
		b.append(" mvt");
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public List<MouvementDossier> find(MouvementDossierCriteria criteria, @Nullable final ParamPagination paramPagination) {

		final Map<String, Object> params = new HashMap<>();
		final String hql = buildFindHql(criteria, false, paramPagination != null ? paramPagination.getSorting() : null, params);
		final Session session = getCurrentSession();
		final Query query = session.createQuery(hql);
		HibernateQueryHelper.assignNamedParameterValues(query, params);
		if (paramPagination != null) {
			final int firstResult = paramPagination.getSqlFirstResult();
			final int maxResult = paramPagination.getSqlMaxResults();
			query.setFirstResult(firstResult);
			query.setMaxResults(maxResult);
		}
		return query.list();
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public List<Long> findIds(MouvementDossierCriteria criteria, @Nullable ParamSorting sorting) {
		final Map<String, Object> params = new HashMap<>();
		final String hql = buildFindHql(criteria, true, sorting, params);
		final Session session = getCurrentSession();
		final Query query = session.createQuery(hql);
		HibernateQueryHelper.assignNamedParameterValues(query, params);
		return query.list();
	}

	@Override
	public int count(MouvementDossierCriteria criteria) {
		final Map<String, Object> params = new HashMap<>();
		final String hql = buildCountHql(criteria, params);
		final Session session = getCurrentSession();
		final Query query = session.createQuery(hql);
		HibernateQueryHelper.assignNamedParameterValues(query, params);
		return ((Number) query.uniqueResult()).intValue();
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public List<MouvementDossier> get(final long[] ids) {
		final StringBuilder builder = new StringBuilder();
		builder.append("FROM MouvementDossier WHERE id");

		final Map<String, Object> params;
		if (ids == null || ids.length == 0) {
			builder.append(" IS NULL");     // il n'y en a pas!
			params = null;
		}
		else {
			builder.append(" IN (:ids)");

			final Set<Long> idSet = new HashSet<>(ids.length);
			for (int i = 0 ; i < ids.length ; ++ i) {
				idSet.add(ids[i]);
			}
			params = new HashMap<>(1);
			params.put("ids", idSet);
		}

		final String hql = builder.toString();
		final Session session = getCurrentSession();
		final Query query = session.createQuery(hql);
		HibernateQueryHelper.assignNamedParameterValues(query, params);
		final List<MouvementDossier> found = query.list();

		// [UNIREG-2872] tri dans l'ordre des ids donnés en entrée
		final List<MouvementDossier> listeFinale;
		if (found != null && !found.isEmpty()) {
			final Map<Long, MouvementDossier> map = new HashMap<>(found.size());
			for (MouvementDossier mvt : found) {
				map.put(mvt.getId(), mvt);
			}

			listeFinale = new ArrayList<>(found.size());

			//noinspection ConstantConditions
			for (long id : ids) {
				final MouvementDossier mvt = map.get(id);
				if (mvt != null) {
					// il a très bien pu être détruit entre le moment où la page
					// web a été affichée et le moment où le bouton "imprimer" est
					// cliqué -> ce serait dommage de faire exploser l'application
					// pour ça
					listeFinale.add(mvt);
				}
			}
		}
		else {
			listeFinale = Collections.emptyList();
		}
		return listeFinale;
	}

	private static final String PROTO_BORDEREAUX_SQL =
					"SELECT MD.MVT_TYPE, MD.COLL_ADMIN_EMETTRICE_ID AS OID_SRC, CAS.NUMERO_CA AS CA_SRC, MD.COLL_ADMIN_DEST_ID AS OID_DEST, CAD.NUMERO_CA AS CA_DEST, COUNT(*)" +
					" FROM MOUVEMENT_DOSSIER MD" +
					" JOIN TIERS CAS ON MD.COLL_ADMIN_EMETTRICE_ID = CAS.NUMERO" +
					" JOIN TIERS CAD ON MD.COLL_ADMIN_DEST_ID = CAD.NUMERO" +
					" WHERE MD.MVT_TYPE = 'EnvoiVersCollAdm'" +
					" AND MD.ETAT = 'A_ENVOYER' AND MD.ANNULATION_DATE IS NULL" +
					" GROUP BY MD.MVT_TYPE, MD.COLL_ADMIN_EMETTRICE_ID, CAS.NUMERO_CA, MD.COLL_ADMIN_DEST_ID, CAD.NUMERO_CA" +
					" UNION" +
					" SELECT MD.MVT_TYPE, MD.COLL_ADMIN_RECEPTRICE_ID, CA.NUMERO_CA, NULL, NULL, COUNT(*)" +
					" FROM MOUVEMENT_DOSSIER MD" +
					" JOIN TIERS CA ON MD.COLL_ADMIN_RECEPTRICE_ID = CA.NUMERO" +
					" WHERE MD.MVT_TYPE = 'ReceptionArchives'" +
					" AND MD.ETAT = 'A_ENVOYER' AND MD.ANNULATION_DATE IS NULL" +
					" GROUP BY MD.MVT_TYPE, MD.COLL_ADMIN_RECEPTRICE_ID, CA.NUMERO_CA";

	@Override
	@SuppressWarnings({"unchecked"})
	public List<ProtoBordereauMouvementDossier> getAllProtoBordereaux(final Integer noCollAdmInitiatricePourFiltrage) {

		final Session session = getCurrentSession();
		final Query query = session.createNativeQuery(PROTO_BORDEREAUX_SQL);
		final List<Object[]> rows = (List<Object[]>) query.list();
		if (rows != null && !rows.isEmpty()) {
			final List<ProtoBordereauMouvementDossier> liste = new ArrayList<>(rows.size());
			for (Object[] row : rows) {
				final String typeStr = (String) row[0];
				final long idCollAdminInitiatrice = ((Number) row[1]).longValue();
				final int noCollAdminInitiatrice = ((Number) row[2]).intValue();
				final int countMvt = ((Number) row[5]).intValue();

				// filtrage pour n'afficher que les proto-bordereaux émis par la collectivité administrative logguée
				if (noCollAdmInitiatricePourFiltrage == null || noCollAdminInitiatrice == noCollAdmInitiatricePourFiltrage) {
					final ProtoBordereauMouvementDossier proto;
					switch (typeStr) {
						case "EnvoiVersCollAdm":
							final long idCollAdminDest = ((Number) row[3]).longValue();
							final int noCollAdminDest = ((Number) row[4]).intValue();
							proto = ProtoBordereauMouvementDossier.createEnvoi(idCollAdminInitiatrice, noCollAdminInitiatrice, idCollAdminDest, noCollAdminDest, countMvt);
							break;
						case "ReceptionArchives":
							proto = ProtoBordereauMouvementDossier.createArchivage(idCollAdminInitiatrice, noCollAdminInitiatrice, countMvt);
							break;
						default:
							throw new RuntimeException("Type de mouvement non supporté : " + typeStr);
					}

					liste.add(proto);
				}
			}
			return !liste.isEmpty() ? liste : null;
		}
		else {
			return null;
		}
	}

	@Override
	public List<Long> getOperatorsIdsToMigrate() {

		final Query query1 = getCurrentSession().createQuery("select distinct noIndividuDestinataire from EnvoiDossierVersCollaborateur where visaDestinataire is null order by noIndividuDestinataire asc");
		//noinspection unchecked
		final List<Number> list1 = query1.list();
		final Query query2 = getCurrentSession().createQuery("select distinct noIndividuRecepteur from ReceptionDossierPersonnel where visaRecepteur is null order by noIndividuRecepteur asc");
		//noinspection unchecked
		final List<Number> list2 = query2.list();

		return Stream.concat(list1.stream(), list2.stream())
				.filter(Objects::nonNull)
				.map(Number::longValue)
				.distinct()
				.sorted()
				.collect(Collectors.toList());
	}

	@Override
	public void updateVisa(long noOperateur, @NotNull String visaOperateur) {
		final Query query1 = getCurrentSession().createQuery("update EnvoiDossierVersCollaborateur set visaDestinataire = :visa where noIndividuDestinataire = :no");
		query1.setParameter("visa", visaOperateur.toLowerCase());    // le visa est toujours stocké en minuscules
		query1.setParameter("no", noOperateur);
		query1.executeUpdate();

		final Query query2 = getCurrentSession().createQuery("update ReceptionDossierPersonnel set visaRecepteur = :visa where noIndividuRecepteur = :no");
		query2.setParameter("visa", visaOperateur.toLowerCase());    // le visa est toujours stocké en minuscules
		query2.setParameter("no", noOperateur);
		query2.executeUpdate();
	}

	@Override
	public void cancelOperateur(Long noOperateur) {
		final Query query1 = getCurrentSession().createQuery("update EnvoiDossierVersCollaborateur set annulationDate = :now, annulationUser = :user where annulationDate is null and noIndividuDestinataire = :no");
		query1.setParameter("now", DateHelper.getCurrentDate());
		query1.setParameter("user", AuthenticationHelper.getCurrentPrincipal());
		query1.setParameter("no", noOperateur);
		query1.executeUpdate();

		final Query query2 = getCurrentSession().createQuery("update ReceptionDossierPersonnel set annulationDate = :now, annulationUser = :user where annulationDate is null and noIndividuRecepteur = :no");
		query2.setParameter("now", DateHelper.getCurrentDate());
		query2.setParameter("user", AuthenticationHelper.getCurrentPrincipal());
		query2.setParameter("no", noOperateur);
		query2.executeUpdate();
	}
}
