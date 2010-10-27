package ch.vd.uniregctb.evenement.identification.contribuable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
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
import ch.vd.uniregctb.evenement.identification.contribuable.Demande.PrioriteEmetteur;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.Etat;

public class IdentCtbDAOImpl extends GenericDAOImpl<IdentificationContribuable, Long> implements IdentCtbDAO {

	private static final Logger LOGGER = Logger.getLogger(IdentCtbDAOImpl.class);

	private static final String TOUS = "TOUS";

	public IdentCtbDAOImpl() {
		super(IdentificationContribuable.class);
	}


	public List<IdentificationContribuable> find(IdentificationContribuableCriteria identificationContribuableCriteria, ParamPagination paramPagination, boolean nonTraiteOnly, boolean archiveOnly,
	                                             boolean nonTraiteAndSuspendu, TypeDemande typeDemande) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start of IdentificationContribuableDAO:find");
		}

		final List<Object> criteria = new ArrayList<Object>();
		String queryWhere = buildCriterion(criteria, identificationContribuableCriteria, nonTraiteOnly, archiveOnly, nonTraiteAndSuspendu);

		String queryOrder = "";
		return executeSearch(paramPagination, criteria, queryWhere, typeDemande);

	}

	@SuppressWarnings("unchecked")
	private List<IdentificationContribuable> executeSearch(ParamPagination paramPagination, final List<Object> criteria, String queryWhere, TypeDemande typeDemande) {
		String queryOrder = new String("");
		if (paramPagination.getChamp() != null) {
			queryOrder = " order by identificationContribuable." + paramPagination.getChamp();
		}
		else {
			queryOrder = " order by identificationContribuable.demande.date";
		}
		if (paramPagination.isSensAscending()) {
			queryOrder = queryOrder + " asc";
		}
		else {
			queryOrder = queryOrder + " desc";
		}

		final String query = " select identificationContribuable from IdentificationContribuable identificationContribuable where DEMANDE_TYPE ='"+typeDemande.name()+"'" + queryWhere + queryOrder;

		final int firstResult = (paramPagination.getNumeroPage() - 1) * paramPagination.getTaillePage();
		final int maxResult = paramPagination.getTaillePage();

		return (List<IdentificationContribuable>) getHibernateTemplate().executeWithNativeSession(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {

				Query queryObject = session.createQuery(query);
				Object[] values = criteria.toArray();
				if (values != null) {
					for (int i = 0; i < values.length; i++) {
						queryObject.setParameter(i, values[i]);
					}
				}
				queryObject.setFirstResult(firstResult);
				queryObject.setMaxResults(maxResult);

				return queryObject.list();
			}
		});
	}


	/**
	 * @param nonTraiteOnly
	 * @param nonTraiteAndSuspendu
	 * @param typeDemande
	 * @see ch.vd.uniregctb.evenement.identification.contribuable.IdentCtbDAO#count(ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableCriteria, boolean, boolean)
	 */

	public int count(IdentificationContribuableCriteria identificationContribuableCriteria, boolean nonTraiteOnly, boolean archiveOnly, boolean nonTraiteAndSuspendu, TypeDemande typeDemande) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start of IdentificationContribuableDAO:count");
		}
		Assert.notNull(identificationContribuableCriteria, "Les critères de recherche peuvent pas être nuls");
		List<Object> criteria = new ArrayList<Object>();
		String queryWhere = buildCriterion(criteria, identificationContribuableCriteria, nonTraiteOnly, archiveOnly, nonTraiteAndSuspendu);


		String query = " select count(*) from IdentificationContribuable identificationContribuable where DEMANDE_TYPE ='"+typeDemande.name()+"'" + queryWhere;
		int count = DataAccessUtils.intResult(getHibernateTemplate().find(query, criteria.toArray()));
		return count;
	}

	/**
	 * Récupère la liste des types de message
	 *
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<String> getTypesMessage() {
		String query = " select distinct identificationContribuable.demande.typeMessage from IdentificationContribuable identificationContribuable";
		return getHibernateTemplate().find(query);
	}

	/**
	 * Récupère la liste des émetteurs
	 *
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<String> getEmetteursId() {
		String query = " select distinct identificationContribuable.demande.emetteurId from IdentificationContribuable identificationContribuable";
		return getHibernateTemplate().find(query);
	}


	/**
	 * Construit la clause Where
	 *
	 * @param criteria
	 * @param identificationContribuableCriteria
	 *
	 * @param nonTraiteOnly       TODO
	 * @param archiveOnly         TODO
	 * @param nonTraiteOrSuspendu TODO
	 * @return
	 */
	private String buildCriterion(List<Object> criteria, IdentificationContribuableCriteria identificationContribuableCriteria, boolean nonTraiteOnly, boolean archiveOnly,
	                              boolean nonTraiteOrSuspendu) {
		String queryWhere = "";

		String typeMessage = identificationContribuableCriteria.getTypeMessage();
		if ((typeMessage != null) && (!TOUS.equals(typeMessage))) {
			queryWhere += " and identificationContribuable.demande.typeMessage = ? ";
			criteria.add(typeMessage);
		}

		Integer periodeFiscale = identificationContribuableCriteria.getPeriodeFiscale();
		if ((periodeFiscale != null) && (periodeFiscale.intValue() != -1)) {
			queryWhere += " and identificationContribuable.demande.periodeFiscale = ? ";
			criteria.add(periodeFiscale);
		}

		String emetteurId = identificationContribuableCriteria.getEmetteurId();
		if ((emetteurId != null) && (!TOUS.equals(emetteurId))) {
			queryWhere += " and identificationContribuable.demande.emetteurId = ? ";
			criteria.add(emetteurId);
		}

		// Si la valeur n'existe pas (TOUS par exemple), etat = null
		PrioriteEmetteur prioriteEmetteur;
		try {
			prioriteEmetteur = PrioriteEmetteur.valueOf(identificationContribuableCriteria.getPrioriteEmetteur());
		}
		catch (Exception e) {
			prioriteEmetteur = null; // pas de priorité => TOUS
		}

		if (prioriteEmetteur != null) {
			queryWhere += " and identificationContribuable.demande.prioriteEmetteur = ? ";
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Priorité émetteur : " + prioriteEmetteur);
			}
			criteria.add(prioriteEmetteur.name());
		}

		Date dateMessageDebut = identificationContribuableCriteria.getDateMessageDebut();
		if (dateMessageDebut != null) {
			queryWhere += " and identificationContribuable.demande.date >= ? ";
			// On prends la date a Zero Hour
			criteria.add(dateMessageDebut);
		}
		Date dateMessageFin = identificationContribuableCriteria.getDateMessageFin();
		if (dateMessageFin != null) {
			queryWhere += " and identificationContribuable.demande.date <= ? ";
			// On prends la date a 24 Hour
			criteria.add(dateMessageFin);
		}

		if (nonTraiteOnly) {
			queryWhere = buildCriterionEtatNonTraite(criteria, queryWhere, identificationContribuableCriteria);
		}
		else if (archiveOnly) {
			queryWhere = buildCriterionEtatArchive(criteria, queryWhere, identificationContribuableCriteria);
		}
		else if (nonTraiteOrSuspendu) {
			queryWhere = buildCriterionEtatNonTraiteOrSuspendu(criteria, queryWhere, identificationContribuableCriteria);
		}
		else {
			queryWhere = buildCriterionEtat(criteria, queryWhere, identificationContribuableCriteria);
		}


		String nom = identificationContribuableCriteria.getNom();
		if (StringUtils.isNotEmpty(nom)) {
			queryWhere += " and upper(identificationContribuable.demande.personne.nom) = upper(?) ";
			criteria.add(nom);
		}

		String prenoms = identificationContribuableCriteria.getPrenoms();
		if (StringUtils.isNotEmpty(prenoms)) {
			queryWhere += " and upper(identificationContribuable.demande.personne.prenoms) = upper(?) ";
			criteria.add(prenoms);
		}

		String navs13 = identificationContribuableCriteria.getNAVS13();
		String navs13WithoutDot = StringUtils.replace(navs13, ".", "");
		if (StringUtils.isNotEmpty(navs13)) {
			queryWhere += " and (identificationContribuable.demande.personne.NAVS13 = ? or identificationContribuable.demande.personne.NAVS13 = ?) ";
			criteria.add(navs13);
			criteria.add(navs13WithoutDot);
		}
		String navs11 = identificationContribuableCriteria.getNAVS11();
		String navs11WithoutDot = StringUtils.replace(navs11, ".", "");
		if (StringUtils.isNotEmpty(navs11)) {
			queryWhere += " and (identificationContribuable.demande.personne.NAVS11 = ? or identificationContribuable.demande.personne.NAVS11 = ?) ";
			criteria.add(navs11);
			criteria.add(navs11WithoutDot);
		}

		RegDate dateNaissance = identificationContribuableCriteria.getDateNaissance();
		if (dateNaissance != null) {
			if (dateNaissance.isPartial()) {
				RegDate[] partialDateRange = dateNaissance.getPartialDateRange();
				queryWhere += " and ((identificationContribuable.demande.personne.dateNaissance = ?) " +
						"or ((identificationContribuable.demande.personne.dateNaissance >= ?) and (identificationContribuable.demande.personne.dateNaissance <= ?)))";
				criteria.add(dateNaissance.index());
				criteria.add(partialDateRange[0].index());
				criteria.add(partialDateRange[1].index());
			}
			else {
				queryWhere += " and (identificationContribuable.demande.personne.dateNaissance = ?) ";
				criteria.add(dateNaissance.index());
			}

		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("IdentificationContribuableCriteria Query: " + queryWhere);
			LOGGER.trace("IdentificationContribuableCriteria Table size: " + criteria.toArray().length);
		}
		return queryWhere;
	}


	/**
	 * Construit le critere avec les état initialisés à non Traité
	 *
	 * @param criteria
	 * @param identificationContribuableCriteria
	 *
	 * @param queryWhere
	 * @return
	 */
	private String buildCriterionEtatNonTraite(List<Object> criteria, String queryWhere, IdentificationContribuableCriteria identificationContribuableCriteria) {


		// Si la valeur n'existe pas (TOUS par exemple), etat = null
		Etat etat;
		try {
			etat = Etat.valueOf(identificationContribuableCriteria.getEtatMessage());
		}
		catch (Exception e) {
			etat = null; // Etat inconnu => TOUS
		}
		if (etat == null) {
			// la valeur de l'etat est a expertiser ou en cours
			Etat aTraiterManuellement = Etat.A_TRAITER_MANUELLEMENT;
			Etat aExpertiser = Etat.A_EXPERTISER;
			queryWhere += " and identificationContribuable.etat in(?,?) ";
			if (LOGGER.isTraceEnabled()) {

				LOGGER.trace("Etat identification CTB: " + aTraiterManuellement + " - " + aExpertiser);
			}
			criteria.add(aTraiterManuellement.name());
			criteria.add(aExpertiser.name());
		}
		else {
			queryWhere += " and identificationContribuable.etat = ? ";
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Etat identification CTB: " + etat);
			}
			criteria.add(etat.name());
		}

		return queryWhere;
	}

	/**
	 * Construit le critere avec les états archivés
	 *
	 * @param criteria
	 * @param identificationContribuableCriteria
	 *
	 * @param queryWhere
	 * @return
	 */
	private String buildCriterionEtatArchive(List<Object> criteria, String queryWhere, IdentificationContribuableCriteria identificationContribuableCriteria) {


		// Si la valeur n'existe pas (TOUS par exemple), etat = null
		Etat etat;
		try {
			etat = Etat.valueOf(identificationContribuableCriteria.getEtatMessage());
		}
		catch (Exception e) {
			etat = null; // Etat inconnu => TOUS
		}
		if (etat == null) {
			// la valeur de l'etat est a expertiser ou en cours
			Etat traiterAutomatique = Etat.TRAITE_AUTOMATIQUEMENT;
			Etat nonIdentifie = Etat.NON_IDENTIFIE;
			Etat traiteManuellementCBO = Etat.TRAITE_MANUELLEMENT;
			Etat traiteManuellementGBO = Etat.TRAITE_MAN_EXPERT;
			queryWhere += " and identificationContribuable.etat in(?,?,?,?) ";
			if (LOGGER.isTraceEnabled()) {

				LOGGER.trace("Etat identification CTB: " + traiterAutomatique + " - " + nonIdentifie + " - " + traiteManuellementCBO + " - " + traiteManuellementGBO);
			}
			criteria.add(traiterAutomatique.name());
			criteria.add(nonIdentifie.name());
			criteria.add(traiteManuellementCBO.name());
			criteria.add(traiteManuellementGBO.name());
		}
		else {
			queryWhere += " and identificationContribuable.etat = ? ";
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Etat identification CTB: " + etat);
			}
			criteria.add(etat.name());
		}

		return queryWhere;
	}


	/**
	 * Construit le critere avec les états en cours, a expertiser
	 *
	 * @param criteria
	 * @param identificationContribuableCriteria
	 *
	 * @param queryWhere
	 * @return
	 */
	private String buildCriterionEtatNonTraiteOrSuspendu(List<Object> criteria, String queryWhere, IdentificationContribuableCriteria identificationContribuableCriteria) {


		// Si la valeur n'existe pas (TOUS par exemple), etat = null
		Etat etat;
		try {
			etat = Etat.valueOf(identificationContribuableCriteria.getEtatMessage());
		}
		catch (Exception e) {
			etat = null; // Etat inconnu => TOUS
		}
		if (etat == null) {
			// la valeur de l'etat est a expertiser ou en cours
			Etat aExpertiser = Etat.A_EXPERTISER;
			Etat aExpertiserSuspendu = Etat.A_EXPERTISER_SUSPENDU;
			Etat aTraiterManuellement = Etat.A_TRAITER_MANUELLEMENT;
			Etat aTraiterManSuspendu = Etat.A_TRAITER_MAN_SUSPENDU;

			queryWhere += " and identificationContribuable.etat in(?,?,?,?) ";
			if (LOGGER.isTraceEnabled()) {

				LOGGER.trace("Etat identification CTB: " + aExpertiser + " - " + aExpertiserSuspendu + " - " + aTraiterManuellement + " - " + aTraiterManSuspendu);
			}
			criteria.add(aExpertiser.name());
			criteria.add(aExpertiserSuspendu.name());
			criteria.add(aTraiterManuellement.name());
			criteria.add(aTraiterManSuspendu.name());
		}
		else {
			queryWhere += " and identificationContribuable.etat = ? ";
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Etat identification CTB: " + etat);
			}
			criteria.add(etat.name());
		}

		return queryWhere;
	}

	/**
	 * Construit le critere avec l'état passé en paramètre
	 *
	 * @param criteria
	 * @param identificationContribuableCriteria
	 *
	 * @param queryWhere
	 * @return
	 */
	private String buildCriterionEtat(List<Object> criteria, String queryWhere, IdentificationContribuableCriteria identificationContribuableCriteria) {


		// Si la valeur n'existe pas (TOUS par exemple), etat = null
		Etat etat;
		try {
			etat = Etat.valueOf(identificationContribuableCriteria.getEtatMessage());
		}
		catch (Exception e) {
			etat = null; // Etat inconnu => TOUS
		}

		if (etat != null) {
			if (Etat.SUSPENDU.equals(etat)) {
				Etat aTraiterManuellementSuspendu = Etat.A_TRAITER_MAN_SUSPENDU;
				Etat aExpertiserSuspendu = Etat.A_EXPERTISER_SUSPENDU;
				queryWhere += " and identificationContribuable.etat in(?,?,?) ";
				if (LOGGER.isTraceEnabled()) {

					LOGGER.trace("Etat identification CTB: " + aTraiterManuellementSuspendu + " - " + aExpertiserSuspendu + " - " + etat);
				}
				criteria.add(aTraiterManuellementSuspendu.name());
				criteria.add(aExpertiserSuspendu.name());
				criteria.add(etat.name());
			}
			else {
				queryWhere += " and identificationContribuable.etat = ? ";
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Etat identification CTB: " + etat);
				}
				criteria.add(etat.name());

			}

		}

		return queryWhere;
	}


}
