package ch.vd.uniregctb.evenement;

import java.sql.SQLException;
import java.util.ArrayList;
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
 * DAO des évènements civils.
 *
 * @author Ludovic BERTIN <mailto:ludovic.bertin@vd.ch>
 *
 */
public class EvenementCivilDAOImpl extends GenericDAOImpl<EvenementCivilData, Long> implements EvenementCivilDAO {

	private static final Logger LOGGER = Logger.getLogger(EvenementCivilDAOImpl.class);

	/**
	 * Constructeur par défaut.
	 */
	public EvenementCivilDAOImpl() {
		super(EvenementCivilData.class);
	}

	/**
	 * Retourne les evenements d'un individu
	 * @see EvenementCivilDAO#rechercheEvenementExistant(java.util.Date, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<EvenementCivilData> rechercheEvenementExistantEtTraitable(RegDate dateEvenement, TypeEvenementCivil typeEvenement, Long noIndividu ) {
		StringBuffer sql = new StringBuffer();
		sql.append("from EvenementCivilData as ec where ec.dateEvenement = ? ");
		sql.append("and ec.type = ? ");
		sql.append("and ec.numeroIndividuPrincipal = ? ");
		sql.append("and not ec.etat in (?,?) ");
		Object[] values = {dateEvenement.index(), typeEvenement.name(), noIndividu, EtatEvenementCivil.TRAITE.name(), EtatEvenementCivil.A_VERIFIER.name()};
		return getHibernateTemplate().find(sql.toString(), values);
	}

	@SuppressWarnings("unchecked")
	public List<EvenementCivilData> find(final EvenementCriteria criterion, final ParamPagination paramPagination) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start of EvenementCivilDAO:find");
		}
		Assert.notNull(criterion, "Les critères de recherche peuvent pas être nuls");

		final List<Object> criteria = new ArrayList<Object>();
		String queryWhere = buildCriterion(criteria, criterion);
		String queryOrder = "";
		if (paramPagination != null && paramPagination.getChamp() != null) {
			queryOrder = " order by evenement." + paramPagination.getChamp();
		} else {
			queryOrder = " order by evenement.dateEvenement";
		}
		if (paramPagination != null && paramPagination.isSensAscending()) {
			queryOrder = queryOrder + " asc" ;
		} else {
			queryOrder = queryOrder + " desc" ;
		}

		final String query = " select evenement from EvenementCivilData evenement where 1=1 " + queryWhere + queryOrder;

		return (List<EvenementCivilData>) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {

				Query queryObject = session.createQuery(query);
				Object[] values = criteria.toArray();
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
	 * @see EvenementCivilDAO#count(ch.vd.uniregctb.evenement.EvenementCriteria)
	 */
	public int count(EvenementCriteria criterion){
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start of EvenementCivilDAO:count");
		}
		Assert.notNull(criterion, "Les critères de recherche peuvent pas être nuls");


		List<Object> criteria = new ArrayList<Object>();
		String queryWhere = buildCriterion(criteria, criterion);
		String query = " select count(*) from EvenementCivilData evenement where 1=1 " + queryWhere;
		int count = DataAccessUtils.intResult(getHibernateTemplate().find(query, criteria.toArray()));
		return count;
	}


	/**
	 * @param criteria
	 * @param criterion
	 * @return
	 */
	private String buildCriterion(List<Object> criteria, EvenementCriteria criterion) {
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
			LOGGER.trace("EvenementCriteria Query: " + queryWhere);
			LOGGER.trace("EvenementCriteria Table size: " + criteria.toArray().length);
		}
		return queryWhere;
	}

	@SuppressWarnings("unchecked")
	public List<Long> getEvenementCivilsNonTraites() {
		return getHibernateTemplate().find("select evt.id from EvenementCivilData evt " +
			"where evt.etat = ? OR evt.etat = ? order by evt.dateTraitement desc",
			new Object[] {EtatEvenementCivil.A_TRAITER.name(), EtatEvenementCivil.EN_ERREUR.name()});
	}

	@SuppressWarnings("unchecked")
	public List<EvenementCivilData> getEvenementsCivilsNonTraites(final List<Long> nosIndividus) {
		final String s = "SELECT e FROM EvenementCivilData e WHERE e.etat IN (:etats) AND (e.numeroIndividuPrincipal IN (:col) OR e.numeroIndividuConjoint IN (:col))";
		final String[] etatsNonTraites = { EtatEvenementCivil.A_TRAITER.name(), EtatEvenementCivil.EN_ERREUR.name() };

		return (List<EvenementCivilData>) getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createQuery(s);
				query.setParameterList("etats", etatsNonTraites);
				query.setParameterList("col", nosIndividus);
				return query.list();
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<Long> getIdsEvenementCivilsATraites() {
		return getHibernateTemplate().find("select evt.id from EvenementCivilData evt " +
			"where evt.etat = ? order by evt.id asc",
			new Object[] {EtatEvenementCivil.A_TRAITER.name()});
	}

	@SuppressWarnings("unchecked")
	public List<Long> getIdsEvenementCivilsErreurIndividu(Long numIndividu){
		return getHibernateTemplate().find("select evt.id from EvenementCivilData evt " +
				"where (evt.etat = ? or evt.etat = ? ) and evt.numeroIndividuPrincipal = ? order by evt.id asc",
				new Object[] {EtatEvenementCivil.EN_ERREUR.name(), EtatEvenementCivil.A_TRAITER, numIndividu});
	}
}
