package ch.vd.uniregctb.evenement.civil.externe;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.orm.hibernate3.HibernateCallback;

import ch.vd.registre.base.dao.GenericDAOImpl;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * DAO des événements civils.
 *
 * @author Ludovic BERTIN <mailto:ludovic.bertin@vd.ch>
 *
 */
public class EvenementCivilExterneDAOImpl extends GenericDAOImpl<EvenementCivilExterne, Long> implements EvenementCivilExterneDAO {

	private static final Logger LOGGER = Logger.getLogger(EvenementCivilExterneDAOImpl.class);

	private static final List<String> ETATS_NON_TRAITES;

	static {
		final List<String> etats = new ArrayList<String>(EtatEvenementCivil.values().length);
		for (EtatEvenementCivil etat : EtatEvenementCivil.values()) {
			if (!etat.isTraite()) {
				etats.add(etat.name());
			}
		}
		ETATS_NON_TRAITES = Collections.unmodifiableList(etats);
	}

	/**
	 * Constructeur par défaut.
	 */
	public EvenementCivilExterneDAOImpl() {
		super(EvenementCivilExterne.class);
	}

	/**
	 * Retourne les evenements d'un individu
	 * @see EvenementCivilExterneDAO#rechercheEvenementExistant(java.util.Date, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<EvenementCivilExterne> rechercheEvenementExistantEtTraitable(final RegDate dateEvenement, final TypeEvenementCivil typeEvenement, final Long noIndividu) {
		final StringBuffer b = new StringBuffer();
		b.append("from EvenementCivilExterne as ec where ec.dateEvenement = :date");
		b.append(" and ec.type = :type");
		b.append(" and ec.numeroIndividuPrincipal = :noIndividu");
		b.append(" and ec.etat in (:etats) ");
		final String sql = b.toString();

		return (List<EvenementCivilExterne>) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
			public List<EvenementCivilExterne> doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createQuery(sql);
				query.setParameter("date", dateEvenement.index());
				query.setParameter("type", typeEvenement.name());
				query.setParameter("noIndividu", noIndividu);
				query.setParameterList("etats", ETATS_NON_TRAITES);
				return query.list();
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<EvenementCivilExterne> find(final EvenementCivilExterneCriteria criterion, final ParamPagination paramPagination) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start of EvenementCivilExterneDAO:find");
		}
		Assert.notNull(criterion, "Les critères de recherche peuvent pas être nuls");

		final List<Object> criteria = new ArrayList<Object>();
		final String queryWhere = buildCriterion(criteria, criterion);
		String queryOrder = "";
		if (paramPagination != null && paramPagination.getChamp() != null) {
			queryOrder = String.format(" order by evenement.%s", paramPagination.getChamp());
		} else {
			queryOrder = " order by evenement.dateEvenement";
		}
		if (paramPagination != null && paramPagination.isSensAscending()) {
			queryOrder = String.format("%s asc", queryOrder);
		} else {
			queryOrder = String.format("%s desc", queryOrder);
		}

		final String query = String.format("select evenement from EvenementCivilExterne evenement where 1=1 %s%s", queryWhere, queryOrder);

		return (List<EvenementCivilExterne>) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {

				final Query queryObject = session.createQuery(query);
				final Object[] values = criteria.toArray();
				if (values != null) {
					for (int i = 0; i < values.length; i++) {
						queryObject.setParameter(i, values[i]);
					}
				}
				if (paramPagination != null) {
					final int firstResult = (paramPagination.getNumeroPage() - 1) * paramPagination.getTaillePage();
					final int maxResult = paramPagination.getTaillePage();

                    queryObject.setFirstResult(firstResult);
                    queryObject.setMaxResults(maxResult);
				}

				return queryObject.list();
			}
		});
	}

	/**
	 * @see EvenementCivilExterneDAO#count(EvenementCivilExterneCriteria)
	 */
	public int count(EvenementCivilExterneCriteria criterion){
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start of EvenementCivilExterneDAO:count");
		}
		Assert.notNull(criterion, "Les critères de recherche peuvent pas être nuls");


		List<Object> criteria = new ArrayList<Object>();
		String queryWhere = buildCriterion(criteria, criterion);
		String query = " select count(*) from EvenementCivilExterne evenement where 1=1 " + queryWhere;
		int count = DataAccessUtils.intResult(getHibernateTemplate().find(query, criteria.toArray()));
		return count;
	}


	/**
	 * @param criteria
	 * @param criterion
	 * @return
	 */
	private String buildCriterion(List<Object> criteria, EvenementCivilExterneCriteria criterion) {
		String queryWhere = "";

		// Si la valeur n'existe pas (TOUS par exemple), type = null
		final TypeEvenementCivil type = criterion.getType();
		if (type != null) {
			queryWhere += " and evenement.type = ? ";
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Type evt: "+type);
			}
			criteria.add(type.name());
		}

		// Si la valeur n'existe pas (TOUS par exemple), etat = null
		final EtatEvenementCivil etat = criterion.getEtat();
		if (etat != null) {
			queryWhere += " and evenement.etat = ? ";
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Etat evt: "+etat);
			}
			criteria.add(etat.name());
		}


		Date dateTraitementDebut = criterion.getDateTraitementDebut();
		if (dateTraitementDebut != null) {
			queryWhere += " and evenement.dateTraitement >= ? ";
			// On prends la date a Zero Hour
			criteria.add(dateTraitementDebut);
		}
		Date dateTraitementFin = criterion.getDateTraitementFin();
		if (dateTraitementFin != null) {
			queryWhere += " and evenement.dateTraitement <= ? ";
			// On prends la date a 24 Hour
			criteria.add(dateTraitementFin);
		}

		RegDate dateEvenementDebut = criterion.getRegDateEvenementDebut();
		if (dateEvenementDebut != null) {
			queryWhere += " and evenement.dateEvenement >= ? ";
			criteria.add(dateEvenementDebut.index());
		}
		RegDate dateEvenementFin = criterion.getRegDateEvenementFin();
		if (dateEvenementFin != null) {
			queryWhere += " and evenement.dateEvenement <= ? ";
			criteria.add(dateEvenementFin.index());
		}

		Long numero = criterion.getNumeroIndividu();
		if (numero != null) {
			queryWhere += " and (evenement.numeroIndividuPrincipal = ? or evenement.numeroIndividuConjoint = ?) ";
			criteria.add(numero);
			criteria.add(numero);
		}

		Long numeroCTB = criterion.getNumeroCTB();
		if (numeroCTB != null) {
			queryWhere += " and (evenement.habitantPrincipalId = ? or evenement.habitantConjointId = ?) ";
			criteria.add(numeroCTB);
			criteria.add(numeroCTB);
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("EvenementCivilExterneCriteria Query: " + queryWhere);
			LOGGER.trace("EvenementCivilExterneCriteria Table size: " + criteria.toArray().length);
		}
		return queryWhere;
	}

	@SuppressWarnings("unchecked")
	public List<Long> getEvenementCivilsNonTraites() {
		return getHibernateTemplate().findByNamedParam("select evt.id from EvenementCivilExterne evt where evt.etat in (:etats) order by evt.dateTraitement desc", "etats", ETATS_NON_TRAITES);
	}

	@SuppressWarnings("unchecked")
	public List<EvenementCivilExterne> getEvenementsCivilsNonTraites(final Collection<Long> nosIndividus) {
		final String s = "SELECT e FROM EvenementCivilExterne e WHERE e.etat IN (:etats) AND (e.numeroIndividuPrincipal IN (:col) OR e.numeroIndividuConjoint IN (:col))";
		return (List<EvenementCivilExterne>) getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createQuery(s);
				query.setParameterList("etats", ETATS_NON_TRAITES);
				query.setParameterList("col", nosIndividus);
				return query.list();
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<Long> getIdsEvenementCivilsErreurIndividu(final Long numIndividu){
		final String s ="select evt.id from EvenementCivilExterne evt where evt.etat in (:etats) and evt.numeroIndividuPrincipal = :ind order by evt.id asc";
		return (List<Long>) getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createQuery(s);
				query.setParameterList("etats", ETATS_NON_TRAITES);
				query.setParameter("ind", numIndividu);
				return query.list();
			}
		});
	}
}
