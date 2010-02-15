package ch.vd.uniregctb.mouvement;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;

import ch.vd.registre.base.dao.GenericDAOImpl;
import ch.vd.registre.base.date.DateRange;
import ch.vd.uniregctb.common.ParamPagination;
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
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<MouvementDossier> findByNumeroDossier(long numero, boolean seulementTraites) {
		final MouvementDossierCriteria criteria = new MouvementDossierCriteria();
		criteria.setNoCtb(numero);
		if (seulementTraites) {
			criteria.setEtatsMouvement(EtatMouvementDossier.getEtatsTraites());
		}
		return find(criteria, null);
	}

	/**
	 * Construit une requête HQL qui récupère les mouvements de dossier correspondant aux critères donnés
	 * @param criteria
	 * @param paramPagination
	 * @param params Paramètres à remplir
	 * @return Requête HQL
	 */
	private static String buildFindHql(MouvementDossierCriteria criteria, ParamPagination paramPagination, List<Object> params) {
		final StringBuilder b = new StringBuilder("SELECT mvt");
		buildFromClause(criteria, b);
		buildWhereClause(criteria, b, params);
		buildOrderByClause(paramPagination, b);
		return b.toString();
	}

	private static String buildCountHql(MouvementDossierCriteria criteria, List<Object> params) {
		final StringBuilder b = new StringBuilder("SELECT COUNT(mvt)");
		buildFromClause(criteria, b);
		buildWhereClause(criteria, b, params);
		return b.toString();
	}

	private static void buildOrderByClause(ParamPagination paramPagination, StringBuilder b) {
		if (paramPagination != null && !StringUtils.isBlank(paramPagination.getChamp())) {
			b.append(" ORDER BY mvt.").append(paramPagination.getChamp());
			if (!paramPagination.isSensAscending()) {
				b.append(" DESC");
			}
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
		final Collection<EtatMouvementDossier> etatsMouvement = criteria.getEtatsMouvement();
		if (etatsMouvement != null) {
			if (etatsMouvement.size() == 0) {
				// aucun etat!
				b.append(" AND 1=0");
			}
			else if (etatsMouvement.size() > 1) {
				b.append(" AND mvt.etat in (");
				boolean first = true;
				for (EtatMouvementDossier etat : etatsMouvement) {
					if (first) {
						b.append("?");
						first = false;
					}
					else {
						b.append(",?");
					}
					params.add(etat.name());
				}
				b.append(")");
			}
			else {
				b.append(" AND mvt.etat = ?");
				params.add(etatsMouvement.iterator().next().name());
			}
		}
	}

	private static void buildWhereClausePartieAnnule(MouvementDossierCriteria criteria, StringBuilder b) {
		if (!criteria.isInclureMouvementsAnnules()) {
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

	@SuppressWarnings({"unchecked"})
	public List<MouvementDossier> find(MouvementDossierCriteria criteria, final ParamPagination paramPagination) {

		final List<Object> params = new ArrayList<Object>();
		final String hql = buildFindHql(criteria, paramPagination, params);
		return (List<MouvementDossier>) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
			public List<MouvementDossier> doInHibernate(Session session) throws HibernateException, SQLException {

				final Query query = session.createQuery(hql);
				for (int i = 0 ; i < params.size() ; ++ i) {
					query.setParameter(i, params.get(i));
				}
				if (paramPagination != null) {
					final int firstResult = (paramPagination.getNumeroPage() - 1) * paramPagination.getTaillePage();
					final int maxResult = paramPagination.getTaillePage();
					query.setFirstResult(firstResult);
					query.setMaxResults(maxResult);
				}
				return query.list();
			}
		});
	}

	public long count(MouvementDossierCriteria criteria) {
		final List<Object> params = new ArrayList<Object>();
		final String hql = buildCountHql(criteria, params);
		return (Long) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
			public Long doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createQuery(hql);
				for (int i = 0 ; i < params.size() ; ++ i) {
					query.setParameter(i, params.get(i));
				}
				return (Long) query.uniqueResult();
			}
		});
	}

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
			builder.append(")");
		}

		final String hql = builder.toString();
		return (List<MouvementDossier>) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
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

	@SuppressWarnings({"unchecked"})
	public List<ProtoBordereauMouvementDossier> getAllProtoBordereaux(final Integer noCollAdmInitiatricePourFiltrage) {

		return (List<ProtoBordereauMouvementDossier>) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
			public List<ProtoBordereauMouvementDossier> doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createSQLQuery(PROTO_BORDEREAUX_SQL);
				final List<Object[]> rows = (List<Object[]>) query.list();
				if (rows != null && rows.size() > 0) {
					final List<ProtoBordereauMouvementDossier> liste = new ArrayList<ProtoBordereauMouvementDossier>(rows.size());
					for (Object[] row : rows) {
						final String typeStr = (String) row[0];
						final long idCollAdminInitiatrice = ((BigDecimal) row[1]).longValue();
						final int noCollAdminInitiatrice = ((BigDecimal) row[2]).intValue();
						final int countMvt = ((BigDecimal) row[5]).intValue();

						// filtrage pour n'afficher que les proto-bordereaux émis par la collectivité administrative logguée
						if (noCollAdmInitiatricePourFiltrage == null || noCollAdminInitiatrice == noCollAdmInitiatricePourFiltrage) {
							final ProtoBordereauMouvementDossier proto;
							if ("EnvoiVersCollAdm".equals(typeStr)) {
								final long idCollAdminDest = ((BigDecimal) row[3]).longValue();
								final int noCollAdminDest = ((BigDecimal) row[4]).intValue();
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
					return liste.size() > 0 ? liste : null;
				}
				else {
					return null;
				}
			}
		});
	}
}
