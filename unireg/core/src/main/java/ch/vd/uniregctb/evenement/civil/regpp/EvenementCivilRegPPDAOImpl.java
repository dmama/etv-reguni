package ch.vd.uniregctb.evenement.civil.regpp;

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
import ch.vd.uniregctb.evenement.civil.EvenementCivilCriteria;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * DAO des événements civils.
 *
 * @author Ludovic BERTIN <mailto:ludovic.bertin@vd.ch>
 *
 */
public class EvenementCivilRegPPDAOImpl extends GenericDAOImpl<EvenementCivilRegPP, Long> implements EvenementCivilRegPPDAO {

	private static final Logger LOGGER = Logger.getLogger(EvenementCivilRegPPDAOImpl.class);

	private static final List<String> ETATS_NON_TRAITES;

	private TiersDAO tiersDao;

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
	public EvenementCivilRegPPDAOImpl() {
		super(EvenementCivilRegPP.class);
	}

	@SuppressWarnings("unused")
	public void setTiersDao(TiersDAO tiersDao) {
		this.tiersDao = tiersDao;
	}

	/**
	 * Retourne les evenements d'un individu
	 * @see EvenementCivilRegPPDAO#rechercheEvenementExistant(java.util.Date, java.lang.Long)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<EvenementCivilRegPP> rechercheEvenementExistantEtTraitable(final RegDate dateEvenement, final TypeEvenementCivil typeEvenement, final Long noIndividu) {
		final StringBuffer b = new StringBuffer();
		b.append("from EvenementCivilRegPP as ec where ec.dateEvenement = :date");
		b.append(" and ec.type = :type");
		b.append(" and ec.numeroIndividuPrincipal = :noIndividu");
		b.append(" and ec.etat in (:etats) ");
		final String sql = b.toString();

		return getHibernateTemplate().executeWithNativeSession(new HibernateCallback<List<EvenementCivilRegPP>>() {
			@Override
			public List<EvenementCivilRegPP> doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createQuery(sql);
				query.setParameter("date", dateEvenement.index());
				query.setParameter("type", typeEvenement.name());
				query.setParameter("noIndividu", noIndividu);
				query.setParameterList("etats", ETATS_NON_TRAITES);
				return query.list();
			}
		});
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<EvenementCivilRegPP> find(final EvenementCivilCriteria criterion, final ParamPagination paramPagination) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start of EvenementCivilRegPPDAO:find");
		}
		Assert.notNull(criterion, "Les critères de recherche peuvent pas être nuls");

		final List<Object> criteria = new ArrayList<Object>();
		final String queryWhere = buildCriterion(criteria, criterion);
		if (queryWhere == null) {
			return Collections.<EvenementCivilRegPP>emptyList();
		}
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
		
		final String query = String.format("select evenement from EvenementCivilRegPP evenement where 1=1 %s%s", queryWhere, queryOrder);

		return getHibernateTemplate().executeWithNativeSession(new HibernateCallback<List<EvenementCivilRegPP>>() {
			@Override
			public List<EvenementCivilRegPP> doInHibernate(Session session) throws HibernateException, SQLException {

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
	 * @see EvenementCivilRegPPDAO#count(ch.vd.uniregctb.evenement.civil.EvenementCivilCriteria)
	 */
	@Override
	public int count(EvenementCivilCriteria criterion){
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start of EvenementCivilRegPPDAO:count");
		}
		Assert.notNull(criterion, "Les critères de recherche peuvent pas être nuls");


		List<Object> criteria = new ArrayList<Object>();
		String queryWhere = buildCriterion(criteria, criterion);
		if (queryWhere == null) {
			return 0;
		}
		String query = " select count(*) from EvenementCivilRegPP evenement where 1=1 " + queryWhere;
		return DataAccessUtils.intResult(getHibernateTemplate().find(query, criteria.toArray()));
	}


	/**
	 * @param criteria ...
	 * @param criterion ...
	 * @return la clause where correspondante à l'objet criterion, null si la requête est vouée à être vide
	 */
	private String buildCriterion(List<Object> criteria, EvenementCivilCriteria<TypeEvenementCivil> criterion) {
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
			Tiers tiers = tiersDao.get(criterion.getNumeroCTB());
			if (tiers != null && tiers instanceof PersonnePhysique ) {
				PersonnePhysique pp = (PersonnePhysique) tiers;
				queryWhere += "and (evenement.numeroIndividuPrincipal = ? or evenement.numeroIndividuConjoint = ?)";
				criteria.add(pp.getNumeroIndividu());
				criteria.add(pp.getNumeroIndividu());
			} else {
				// Si le numero de ctb n'existe pas ou si le tiers n'est pas une personne physique alors la recherche doit être vide
				return null;
			}
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("EvenementCivilCriteria Query: " + queryWhere);
			LOGGER.trace("EvenementCivilCriteria Table size: " + criteria.toArray().length);
		}
		return queryWhere;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Long> getEvenementCivilsNonTraites() {
		return getHibernateTemplate().findByNamedParam("select evt.id from EvenementCivilRegPP evt where evt.etat in (:etats) order by evt.dateTraitement desc", "etats", ETATS_NON_TRAITES);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<EvenementCivilRegPP> getEvenementsCivilsNonTraites(final Collection<Long> nosIndividus) {
		final String s = "SELECT e FROM EvenementCivilRegPP e WHERE e.etat IN (:etats) AND (e.numeroIndividuPrincipal IN (:col) OR e.numeroIndividuConjoint IN (:col))";
		return getHibernateTemplate().execute(new HibernateCallback<List<EvenementCivilRegPP>>() {
			@Override
			public List<EvenementCivilRegPP> doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createQuery(s);
				query.setParameterList("etats", ETATS_NON_TRAITES);
				query.setParameterList("col", nosIndividus);
				return query.list();
			}
		});
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Long> getIdsEvenementCivilsErreurIndividu(final Long numIndividu){
		final String s ="select evt.id from EvenementCivilRegPP evt where evt.etat in (:etats) and evt.numeroIndividuPrincipal = :ind order by evt.id asc";
		return getHibernateTemplate().execute(new HibernateCallback<List<Long>>() {
			@Override
			public List<Long> doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createQuery(s);
				query.setParameterList("etats", ETATS_NON_TRAITES);
				query.setParameter("ind", numIndividu);
				return query.list();
			}
		});
	}

	@Override
	public List<EvenementCivilRegPP> findEvenementByIndividu(final Long numIndividu) {
		final String s = "select distinct e from EvenementCivilRegPP e where e.numeroIndividuPrincipal = :numIndividu or e.numeroIndividuConjoint = :numIndividu";
		return getHibernateTemplate().execute(new HibernateCallback<List<EvenementCivilRegPP>>() {
			@SuppressWarnings("unchecked")
			@Override
			public List<EvenementCivilRegPP> doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createQuery(s);
				query.setParameter("numIndividu", numIndividu);
				return query.list();
			}
		});
	}
}
