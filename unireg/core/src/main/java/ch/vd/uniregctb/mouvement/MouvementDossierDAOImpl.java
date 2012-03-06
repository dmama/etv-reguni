package ch.vd.uniregctb.mouvement;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jetbrains.annotations.Nullable;
import org.springframework.orm.hibernate3.HibernateCallback;

import ch.vd.registre.base.dao.GenericDAOImpl;
import ch.vd.registre.base.date.DateRange;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.common.ParamSorting;
import ch.vd.uniregctb.type.TypeMouvement;

public class MouvementDossierDAOImpl extends GenericDAOImpl<MouvementDossier, Long> implements MouvementDossierDAO {

	private static final Logger LOGGER = Logger.getLogger(MouvementDossierDAOImpl.class);

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
	private static String buildFindHql(MouvementDossierCriteria criteria, boolean idsOnly, @Nullable ParamSorting sorting, List<Object> params) {
		final StringBuilder b = new StringBuilder("SELECT mvt");
		if (idsOnly) {
			b.append(".id");
		}
		buildFromClause(criteria, b);
		buildWhereClause(criteria, b, params);
		buildOrderByClause(sorting, b);
		return b.toString();
	}

	private static String buildCountHql(MouvementDossierCriteria criteria, List<Object> params) {
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

	private static void buildWhereClause(MouvementDossierCriteria criteria, StringBuilder b, List<Object> params) {

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

	private static void buildWhereClausePartieEtatActuelSeulement(MouvementDossierCriteria criteria, StringBuilder b, List<Object> params) {
		if (criteria.isSeulementDerniersMouvements()) {
			b.append(" AND NOT EXISTS (");
			{
				b.append("SELECT other.id FROM MouvementDossier other WHERE other.annulationDate IS NULL AND other.contribuable.numero = mvt.contribuable.numero AND other.etat IN (");

				// tous les états dits "traités"
				final List<EtatMouvementDossier> etatsTraites = EtatMouvementDossier.getEtatsTraites();
				final Iterator<EtatMouvementDossier> iterator = etatsTraites.iterator();
				while (iterator.hasNext()) {
					final EtatMouvementDossier etat = iterator.next();
					b.append('?');
					params.add(etat.name());
					if (iterator.hasNext()) {
						b.append(',');
					}
				}

				b.append(") AND (other.dateMouvement > mvt.dateMouvement OR (other.dateMouvement = mvt.dateMouvement AND other.logModifDate > mvt.logModifDate))");
			}
			b.append(')');
		}
	}

	private static void buildWhereClausePartieCollectiviteAdministrativeInitiatrice(MouvementDossierCriteria criteria, StringBuilder b, List<Object> params) {
		if (criteria.getIdCollAdministrativeInitiatrice() != null) {
			if (criteria.getTypeMouvement() == null) {
				final String part = String.format(" AND (EXISTS (SELECT reception.id FROM %s reception WHERE reception.id=mvt.id AND reception.collectiviteAdministrativeReceptrice.id = ?) OR EXISTS (SELECT envoi.id FROM %s envoi WHERE envoi.id=mvt.id AND envoi.collectiviteAdministrativeEmettrice.id = ?))",
												ReceptionDossier.class.getSimpleName(), EnvoiDossier.class.getSimpleName());
				b.append(part);
				params.add(criteria.getIdCollAdministrativeInitiatrice());
				params.add(criteria.getIdCollAdministrativeInitiatrice());
			}
			else if (criteria.getTypeMouvement() == TypeMouvement.EnvoiDossier) {
				b.append(" AND mvt.collectiviteAdministrativeEmettrice.id = ?");
				params.add(criteria.getIdCollAdministrativeInitiatrice());
			}
			else if (criteria.getTypeMouvement() == TypeMouvement.ReceptionDossier) {
				b.append(" AND mvt.collectiviteAdministrativeReceptrice.id = ?");
				params.add(criteria.getIdCollAdministrativeInitiatrice());
			}
		}
	}

	private static void buildWhereClausePartieCollectiviteAdministrativeDestinataire(MouvementDossierCriteria criteria, StringBuilder b, List<Object> params) {
		if (criteria.getIdCollAdministrativeDestinataire() != null) {
			b.append(" AND mvt.class = ").append(EnvoiDossierVersCollectiviteAdministrative.class.getSimpleName());
			b.append(" AND mvt.collectiviteAdministrativeDestinataire.id = ?");
			params.add(criteria.getIdCollAdministrativeDestinataire());
		}
	}

	private static void buildWhereClausePartieIndividuDestinataire(MouvementDossierCriteria criteria, StringBuilder b, List<Object> params) {
		if (criteria.getNoIndividuDestinataire() != null) {
			b.append(" AND mvt.class = ").append(EnvoiDossierVersCollaborateur.class.getSimpleName());
			b.append(" AND mvt.noIndividuDestinataire = ?");
			params.add(criteria.getNoIndividuDestinataire());
		}
	}

	private static void buildWhereClausePartieLocalisationReception(MouvementDossierCriteria criteria, StringBuilder b, List<Object> params) {
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

			b.append(" AND mvt.class = ").append(clazz.getSimpleName());
			if (criteria.getNoIndividuRecepteur() != null) {
				b.append(" AND mvt.noIndividuRecepteur = ?");
				params.add(criteria.getNoIndividuRecepteur());
			}
		}
	}

	@SuppressWarnings({"unchecked"})
	private static void buildWhereClausePartieEtatMouvement(MouvementDossierCriteria criteria, StringBuilder b, List<Object> params) {

		final Collection<EtatMouvementDossier> etatsDemandes;
		if (criteria.isSeulementDerniersMouvements()) {
			// si on doit filtrer sur les derniers mouvements seulement, seuls les mouvements traités doivent revenir
			if (criteria.getEtatsMouvement() != null && !criteria.getEtatsMouvement().isEmpty()) {
				final Set<EtatMouvementDossier> temp = new HashSet<EtatMouvementDossier>(criteria.getEtatsMouvement().size());
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
			else if (etatsDemandes.size() > 1) {
				b.append(" AND mvt.etat in (");
				boolean first = true;
				for (EtatMouvementDossier etat : etatsDemandes) {
					if (first) {
						b.append('?');
						first = false;
					}
					else {
						b.append(",?");
					}
					params.add(etat.name());
				}
				b.append(')');
			}
			else {
				b.append(" AND mvt.etat = ?");
				params.add(etatsDemandes.iterator().next().name());
			}
		}
	}

	private static void buildWhereClausePartieAnnule(MouvementDossierCriteria criteria, StringBuilder b) {
		if (!criteria.isInclureMouvementsAnnules() || criteria.isSeulementDerniersMouvements()) {
			b.append(" AND mvt.annulationDate IS NULL");
		}
	}

	@SuppressWarnings({"unchecked"})
	private static void buildWhereClausePartieDateMouvement(MouvementDossierCriteria criteria, StringBuilder b, List<Object> params) {
		final DateRange rangeDateMvt = criteria.getRangeDateMouvement();
		if (rangeDateMvt != null && (rangeDateMvt.getDateDebut() != null || rangeDateMvt.getDateFin() != null)) {
			b.append(" AND mvt.dateMouvement");
			if (rangeDateMvt.getDateDebut() != null && rangeDateMvt.getDateFin() != null) {
				b.append(" BETWEEN ? AND ?");
				params.add(rangeDateMvt.getDateDebut().index());
				params.add(rangeDateMvt.getDateFin().index());
			}
			else if (rangeDateMvt.getDateDebut() != null) {
				b.append(" >= ?");
				params.add(rangeDateMvt.getDateDebut().index());
			}
			else {
				b.append(" <= ?");
				params.add(rangeDateMvt.getDateFin().index());
			}
		}
	}

	@SuppressWarnings({"unchecked"})
	private static void buildWhereClausePartieCtb(MouvementDossierCriteria criteria, StringBuilder b, List<Object> params) {
		if (criteria.getNoCtb() != null) {
			b.append(" AND mvt.contribuable.numero=?");
			params.add(criteria.getNoCtb());
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

		final List<Object> params = new ArrayList<Object>();
		final String hql = buildFindHql(criteria, false, paramPagination != null ? paramPagination.getSorting() : null, params);
		return getHibernateTemplate().executeWithNativeSession(new HibernateCallback<List<MouvementDossier>>() {
			@Override
			public List<MouvementDossier> doInHibernate(Session session) throws HibernateException, SQLException {

				final Query query = session.createQuery(hql);
				for (int i = 0 ; i < params.size() ; ++ i) {
					query.setParameter(i, params.get(i));
				}
				if (paramPagination != null) {
					final int firstResult = paramPagination.getSqlFirstResult();
					final int maxResult = paramPagination.getSqlMaxResults();
					query.setFirstResult(firstResult);
					query.setMaxResults(maxResult);
				}
				return query.list();
			}
		});
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public List<Long> findIds(MouvementDossierCriteria criteria, @Nullable ParamSorting sorting) {
		final List<Object> params = new ArrayList<Object>();
		final String hql = buildFindHql(criteria, true, sorting, params);
		return getHibernateTemplate().executeWithNativeSession(new HibernateCallback<List<Long>>() {
			@Override
			public List<Long> doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createQuery(hql);
				for (int i = 0 ; i < params.size() ; ++ i) {
					query.setParameter(i, params.get(i));
				}
				return query.list();
			}
		});
	}

	@Override
	public int count(MouvementDossierCriteria criteria) {
		final List<Object> params = new ArrayList<Object>();
		final String hql = buildCountHql(criteria, params);
		return getHibernateTemplate().executeWithNativeSession(new HibernateCallback<Integer>() {
			@Override
			public Integer doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createQuery(hql);
				for (int i = 0 ; i < params.size() ; ++ i) {
					query.setParameter(i, params.get(i));
				}
				return ((Number) query.uniqueResult()).intValue();
			}
		});
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public List<MouvementDossier> get(final long[] ids) {
		final StringBuilder builder = new StringBuilder();
		builder.append("FROM MouvementDossier WHERE id");
		if (ids == null || ids.length == 0) {
			builder.append(" IS NULL");     // il n'y en a pas!
		}
		else if (ids.length == 1) {
			builder.append("=?");
		}
		else {
			builder.append(" IN (?");
			for (int i = 1 ; i < ids.length ; ++ i) {
				builder.append(",?");
			}
			builder.append(')');
		}

		final String hql = builder.toString();
		final List<MouvementDossier> found = getHibernateTemplate().executeWithNativeSession(new HibernateCallback<List<MouvementDossier>>() {
			@Override
			public List<MouvementDossier> doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createQuery(hql);
				if (ids != null) {
					for (int i = 0 ; i < ids.length ; ++ i) {
						query.setParameter(i, ids[i]);
					}
				}
				return query.list();
			}
		});

		// [UNIREG-2872] tri dans l'ordre des ids donnés en entrée
		final List<MouvementDossier> listeFinale;
		if (found != null && !found.isEmpty()) {
			final Map<Long, MouvementDossier> map = new HashMap<Long, MouvementDossier>(found.size());
			for (MouvementDossier mvt : found) {
				map.put(mvt.getId(), mvt);
			}

			listeFinale = new ArrayList<MouvementDossier>(found.size());

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

		return getHibernateTemplate().executeWithNativeSession(new HibernateCallback<List<ProtoBordereauMouvementDossier>>() {
			@Override
			public List<ProtoBordereauMouvementDossier> doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createSQLQuery(PROTO_BORDEREAUX_SQL);
				final List<Object[]> rows = (List<Object[]>) query.list();
				if (rows != null && !rows.isEmpty()) {
					final List<ProtoBordereauMouvementDossier> liste = new ArrayList<ProtoBordereauMouvementDossier>(rows.size());
					for (Object[] row : rows) {
						final String typeStr = (String) row[0];
						final long idCollAdminInitiatrice = ((Number) row[1]).longValue();
						final int noCollAdminInitiatrice = ((Number) row[2]).intValue();
						final int countMvt = ((Number) row[5]).intValue();

						// filtrage pour n'afficher que les proto-bordereaux émis par la collectivité administrative logguée
						if (noCollAdmInitiatricePourFiltrage == null || noCollAdminInitiatrice == noCollAdmInitiatricePourFiltrage) {
							final ProtoBordereauMouvementDossier proto;
							if ("EnvoiVersCollAdm".equals(typeStr)) {
								final long idCollAdminDest = ((Number) row[3]).longValue();
								final int noCollAdminDest = ((Number) row[4]).intValue();
								proto = ProtoBordereauMouvementDossier.createEnvoi(idCollAdminInitiatrice, noCollAdminInitiatrice, idCollAdminDest, noCollAdminDest, countMvt);
							}
							else if ("ReceptionArchives".equals(typeStr)) {
								proto = ProtoBordereauMouvementDossier.createArchivage(idCollAdminInitiatrice, noCollAdminInitiatrice, countMvt);
							}
							else {
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
		});
	}
}
