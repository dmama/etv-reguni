package ch.vd.uniregctb.stats.evenements;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.evenement.externe.EtatEvenementExterne;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;

public class StatistiquesEvenementsServiceImpl implements StatistiquesEvenementsService {

	private HibernateTemplate hibernateTemplate;

	private static final String VISA_HUMAIN_TEMPLATE = "zai%";

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	/**
	 * Renvoie les statistiques sur les événements civils
	 */
	public StatsEvenementsCivilsResults getStatistiquesEvenementsCivils(RegDate debutActivite) {
		final Map<EtatEvenementCivil, BigDecimal> etats = getEtatsEvenementsCivils(null);
		final Map<EtatEvenementCivil, BigDecimal> etatsNouveaux = getEtatsEvenementsCivils(debutActivite);
		final Map<TypeEvenementCivil, BigDecimal> erreursParType = getNombreEvenementsCivilsEnErreurParType();
		final List<StatsEvenementsCivilsResults.EvenementCivilEnErreurInfo> toutesErreurs = getToutesErreursEvenementsCivils();
		final List<StatsEvenementsCivilsResults.EvenementCivilTraiteManuellementInfo> manipulationsManuelles = getManipulationsManuelles(debutActivite);
		final Map<Integer, BigDecimal> ignores = getNombreEvenementsCivilsIgnores(debutActivite);
		return new StatsEvenementsCivilsResults(etats, etatsNouveaux, erreursParType, toutesErreurs, manipulationsManuelles, ignores);
	}

	private Map<EtatEvenementCivil, BigDecimal> getEtatsEvenementsCivils(RegDate debutActivite) {
		final String sql;
		final Map<String, Object> sqlParameters;
		if (debutActivite != null) {
			sql = "SELECT ETAT, COUNT(*) FROM EVENEMENT_CIVIL_REGROUPE WHERE LOG_CDATE > TO_DATE(:debutActivite, 'YYYYMMDD') GROUP BY ETAT";
			sqlParameters = new HashMap<String, Object>(1);
			sqlParameters.put("debutActivite", debutActivite.index());
		}
		else {
			sql = "SELECT ETAT, COUNT(*) FROM EVENEMENT_CIVIL_REGROUPE GROUP BY ETAT";
			sqlParameters = null;
		}
		return getNombreParModalite(EtatEvenementCivil.class, sql, sqlParameters);
	}

	private Map<TypeEvenementCivil, BigDecimal> getNombreEvenementsCivilsEnErreurParType() {
		final String sql = "SELECT TYPE, COUNT(*) FROM EVENEMENT_CIVIL_REGROUPE WHERE ETAT='EN_ERREUR' GROUP BY TYPE";
		return getNombreParModalite(TypeEvenementCivil.class, sql, null);
	}

	@SuppressWarnings({"unchecked"})
	private Map<Integer, BigDecimal> getNombreEvenementsCivilsIgnores(final RegDate debutActivite) {
		final String sql = "SELECT REPLACE(SUBSTR(MESSAGE, INSTR(MESSAGE, 'code ') + 5), ')') AS CODE, COUNT(*) FROM AUDIT_LOG" +
				" WHERE MESSAGE LIKE 'Arrivée d''un message JMS ignoré (%' AND LOG_DATE > TO_DATE(:debutActivite, 'YYYYMMDD')" +
				" GROUP BY REPLACE(SUBSTR(MESSAGE, INSTR(MESSAGE, 'code ') + 5), ')')";

		return (Map<Integer, BigDecimal>) hibernateTemplate.executeWithNewSession(new HibernateCallback() {
			public Map<Integer, BigDecimal> doInHibernate(Session session) throws HibernateException, SQLException {

				final Query query = session.createSQLQuery(sql);
				query.setParameter("debutActivite", debutActivite.index());

				final List<Object[]> result = query.list();
				if (result != null && result.size() > 0) {
					final Map<Integer, BigDecimal> map = new HashMap<Integer, BigDecimal>(result.size());
					for (Object[] row : result) {
						final Integer code = Integer.valueOf((String) row[0]);
						final BigDecimal nombre = (BigDecimal) row[1];
						map.put(code, nombre);
					}
					return map;
				}
				else {
					return null;
				}
			}
		});
	}

	private List<StatsEvenementsCivilsResults.EvenementCivilEnErreurInfo> getToutesErreursEvenementsCivils() {

		final String sql = "SELECT R.ID, R.DATE_EVENEMENT, R.DATE_TRAITEMENT, R.ETAT, R.NO_INDIVIDU_PRINCIPAL, R.NO_INDIVIDU_CONJOINT, R.TYPE, R.NUMERO_OFS_ANNONCE, E.MESSAGE"
				+ " FROM EVENEMENT_CIVIL_REGROUPE R JOIN EVENEMENT_CIVIL_ERREUR E ON E.EVT_CIVIL_ID = R.ID WHERE R.ETAT != 'TRAITE' ORDER BY R.ID, R.DATE_TRAITEMENT";

		return executeSelect(sql, new SelectCallback<StatsEvenementsCivilsResults.EvenementCivilEnErreurInfo>() {
			public StatsEvenementsCivilsResults.EvenementCivilEnErreurInfo onRow(Object[] row) {

				Assert.isEqual(9, row.length);

				final long id = ((BigDecimal) row[0]).longValueExact();
				final RegDate dateEvenement = RegDate.fromIndex(((BigDecimal) row[1]).intValueExact(), false);
				final Date dateTraitement = (Date) row[2];
				final EtatEvenementCivil etat = EtatEvenementCivil.valueOf((String) row[3]);
				final Long individuPrincipal = ((BigDecimal) row[4]).longValueExact();
				final Long individuConjoint = row[5] != null ? ((BigDecimal) row[5]).longValueExact() : null;
				final TypeEvenementCivil type = TypeEvenementCivil.valueOf((String) row[6]);
				final Integer ofsAnnonce = row[7] != null ? ((BigDecimal) row[7]).intValueExact() : null;
				final String message = (String) row[8];
				return new StatsEvenementsCivilsResults.EvenementCivilEnErreurInfo(id, type, dateEvenement, dateTraitement, etat, individuPrincipal, individuConjoint, ofsAnnonce, message);
			}
		});
	}

	private List<StatsEvenementsCivilsResults.EvenementCivilTraiteManuellementInfo> getManipulationsManuelles(RegDate debutActivite) {

		final String sql = "SELECT ID, LOG_CDATE, LOG_MDATE, LOG_MUSER, DATE_EVENEMENT, ETAT, NO_INDIVIDU_PRINCIPAL, NO_INDIVIDU_CONJOINT, NUMERO_OFS_ANNONCE, TYPE FROM EVENEMENT_CIVIL_REGROUPE"
				+ " WHERE LOG_MUSER LIKE '" + VISA_HUMAIN_TEMPLATE + "' AND LOG_MDATE > TO_DATE('" + debutActivite.index() + "', 'YYYYMMDD') ORDER BY ID";

		return executeSelect(sql, new SelectCallback<StatsEvenementsCivilsResults.EvenementCivilTraiteManuellementInfo>() {
			public StatsEvenementsCivilsResults.EvenementCivilTraiteManuellementInfo onRow(Object[] row) {

				Assert.isEqual(10, row.length);

				final long id = ((BigDecimal) row[0]).longValueExact();
				final Date dateReception = (Date) row[1];
				final Date dateModification = (Date) row[2];
				final String visaOperateur = (String) row[3];
				final RegDate dateEvenement = RegDate.fromIndex(((BigDecimal) row[4]).intValueExact(), false);
				final EtatEvenementCivil etat = EtatEvenementCivil.valueOf((String) row[5]);
				final Long individuPrincipal = ((BigDecimal) row[6]).longValueExact();
				final Long individuConjoint = row[7] != null ? ((BigDecimal) row[7]).longValueExact() : null;
				final Integer ofsAnnonce = row[8] != null ? ((BigDecimal) row[8]).intValueExact() : null;
				final TypeEvenementCivil type = TypeEvenementCivil.valueOf((String) row[9]);
				return new StatsEvenementsCivilsResults.EvenementCivilTraiteManuellementInfo(id, type, dateEvenement, etat, individuPrincipal, individuConjoint, ofsAnnonce, visaOperateur, dateReception, dateModification);
			}
		});
	}

	/**
	 * Renvoie les statistiques sur les événements externes
	 */
	public StatsEvenementsExternesResults getStatistiquesEvenementsExternes() {
		final Map<EtatEvenementExterne, BigDecimal> etats = getEtatsEvenementsExternes();
		final List<StatsEvenementsExternesResults.EvenementExterneErreur> erreurs = getErreursEvenementsExternes();
		return new StatsEvenementsExternesResults(etats, erreurs);
	}

	private List<StatsEvenementsExternesResults.EvenementExterneErreur> getErreursEvenementsExternes() {

		final String sql = "SELECT ID, MESSAGE FROM EVENEMENT_EXTERNE WHERE ETAT='EN_ERREUR'";

		return executeSelect(sql, new SelectCallback<StatsEvenementsExternesResults.EvenementExterneErreur>() {
			public StatsEvenementsExternesResults.EvenementExterneErreur onRow(Object[] row) {

				Assert.isEqual(2, row.length);

				final long id = ((BigDecimal) row[0]).longValueExact();
				final String message = (String) row[1];
				return new StatsEvenementsExternesResults.EvenementExterneErreur(id, message);
			}
		});
	}

	private Map<EtatEvenementExterne, BigDecimal> getEtatsEvenementsExternes() {
		final String sql = "SELECT ETAT, COUNT(*) FROM EVENEMENT_EXTERNE GROUP BY ETAT";
		return getNombreParModalite(EtatEvenementExterne.class, sql, null);
	}

	private static interface SelectCallback<T> {
		T onRow(Object[] row);
	}

	@SuppressWarnings({"unchecked"})
	private <T> List<T> executeSelect(final String sql, final SelectCallback<T> callback) {
		return (List<T>) hibernateTemplate.executeWithNewSession(new HibernateCallback() {

			public List<T> doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createSQLQuery(sql);
				final List<Object[]> results = query.list();
				if (results != null && results.size() > 0) {
					final List<T> liste = new ArrayList<T>(results.size());
					for (Object[] row : results) {
						final T element = callback.onRow(row);
						if (element != null) {
							liste.add(element);
						}
					}
					return liste;
				}
				else {
					return null;
				}
			}
		});
	}

	@SuppressWarnings({"unchecked"})
	private <T extends Enum<T>> Map<T, BigDecimal> getNombreParModalite(final Class<T> enumClass, final String sql, final Map<String, Object> sqlParameters) {
		return (Map<T, BigDecimal>) hibernateTemplate.executeWithNewSession(new HibernateCallback() {
			public Map<T, BigDecimal> doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createSQLQuery(sql);
				if (sqlParameters != null && sqlParameters.size() > 0) {
					for (Map.Entry<String, Object> entry : sqlParameters.entrySet()) {
						query.setParameter(entry.getKey(), entry.getValue());
					}
				}
				final List<Object[]> result = query.list();
				if (result != null && result.size() > 0) {
					final Map<T, BigDecimal> map = new HashMap<T, BigDecimal>(result.size());
					for (Object[] row : result) {
						final T modalite = Enum.valueOf(enumClass, (String) row[0]);
						final BigDecimal nombre = (BigDecimal) row[1];
						map.put(modalite, nombre);
					}
					return map;
				}
				else {
					return null;
				}
			}
		});
	}

	private Map<IdentificationContribuable.Etat, BigDecimal> getEtatsEvenementsIdentificationContribuable(RegDate debutActivite) {
		final String sql;
		final Map<String, Object> sqlParameters;
		if (debutActivite != null) {
			sql = "SELECT ETAT, COUNT(*) FROM EVENEMENT_IDENTIFICATION_CTB WHERE LOG_CDATE > TO_DATE(:debutActivite, 'YYYYMMDD') GROUP BY ETAT";
			sqlParameters = new HashMap<String, Object>(1);
			sqlParameters.put("debutActivite", debutActivite.index());
		}
		else {
			sql = "SELECT ETAT, COUNT(*) FROM EVENEMENT_IDENTIFICATION_CTB GROUP BY ETAT";
			sqlParameters = null;
		}
		return getNombreParModalite(IdentificationContribuable.Etat.class, sql, sqlParameters);
	}
}
