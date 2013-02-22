package ch.vd.uniregctb.database;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.hibernate.interceptor.HibernateFakeInterceptor;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.scheduler.JobDefinition;

public class MigrationNoOfsVersLibelleJob extends JobDefinition {

	private static final Logger LOGGER = Logger.getLogger(MigrationNoOfsVersLibelleJob.class);

	public static final String NAME = "MigrationNoOfsVersLibelleJob";
	private static final String CATEGORIE = "Database";

	private ServiceInfrastructureService serviceInfra;
	private SessionFactory sessionFactory;
	private Dialect dialect;

	public MigrationNoOfsVersLibelleJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);
	}

	public void setServiceInfra(ServiceInfrastructureService serviceInfra) {
		this.serviceInfra = serviceInfra;
	}

	public void setDialect(Dialect dialect) {
		this.dialect = dialect;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final Session session = sessionFactory.withOptions().interceptor(new HibernateFakeInterceptor()).openSession();
		try {
			final Collection<Object[]> tuples = selectTiers(session);
			if (!getStatusManager().interrupted()) {
				updateTiers(session, buildMapConversion(tuples));
			}
		}
		finally {
			if (session != null) {
				session.flush();
				session.close();
			}
		}
		getStatusManager().setMessage("Traitement terminé");
	}

	/**
	 * Retrouve l'ensemble des non-habitants dont on doit convertir le no ofs en libelle
	 *
	 */
	@SuppressWarnings("unchecked")
	private Collection<Object[]> selectTiers(final Session session) {
		return session.createSQLQuery(
			"select NUMERO, NH_NO_OFS_COMMUNE_ORIGINE from TIERS " +
				" where PP_HABITANT = " + dialect.toBooleanValueString(false) +
				" and NH_NO_OFS_COMMUNE_ORIGINE is not null" +
				" and NH_LIBELLE_COMMUNE_ORIGINE is null" +
				" and ANNULATION_DATE is null").list();
	}

	/**
	 * Construit une structure de donnée intermédiaire utile
	 * pour générer les requetes d'update :
	 * <pre>
	 * Clé                      Valeur
	 * ------------------       -----------------------------------------------
	 * | No OFS commune |  ===> | libellé commune | Liste des ctb concernés   |
	 * ------------------       -----------------------------------------------
	 * </pre>
	 */
	private Map<Integer, MapValue> buildMapConversion(final Collection<Object[]> tuples) {
		// construction d'un set avec les differents numeroOfs
		final Set<Integer> numerosOfs = new HashSet<Integer>(2000);
		for (Object[] tuple : tuples) {
			numerosOfs.add(((BigDecimal) tuple[1]).intValue());
		}

		//initialisation de la map de conversion no ofs commune -> libelle commune
		final RegDate today = RegDate.get();
		final Map<Integer, MapValue> mapConversion = new HashMap<Integer, MapValue>(numerosOfs.size());
		for (Integer ofs : numerosOfs) {
			final Commune commune = serviceInfra.getCommuneByNumeroOfs(ofs, today);
			if (commune == null) {
				LOGGER.warn("Le service infrastructure ne connait pas pas la commune no OFS " + ofs + " au " + RegDateHelper.dateToDisplayString(today));
				continue;
			}
			mapConversion.put(ofs, new MapValue(commune.getNomOfficiel()));
		}

		// remplissage de la map avec les contribuables "à convertir"
		for (Object[] tuple : tuples) {
			final Long noCtb = ((BigDecimal) tuple[0]).longValue();
			final Integer noOfs = ((BigDecimal) tuple[1]).intValue();
			final MapValue value = mapConversion.get(noOfs);
			if (value != null) {
				value.contribuables.add(noCtb);
			}
		}
		return mapConversion;
	}

	private void updateTiers(final Session session, final Map<Integer, MapValue> mapConversion) {
		final String statusMessage = "Reste  %d communes d'origine sur %s à mettre à jour";
		getStatusManager().setMessage(String.format(statusMessage, mapConversion.size() ,mapConversion.size()), 2);

		final SQLQuery sqlQuery = session.createSQLQuery(
				"update TIERS " +
					"set NH_LIBELLE_COMMUNE_ORIGINE = :libelle, " +
						"LOG_MDATE = :ts, " +
						"LOG_MUSER = '" + NAME + "'" +
				    "where NUMERO IN (:ids)");

		final int total = mapConversion.size();
		int reste = total;
		for1: for ( MapValue value : mapConversion.values()) {
			final Long[] contribuables = value.contribuables.toArray(new Long[value.contribuables.size()]);
			final List<Long> max1000ids = new ArrayList<Long>(1000);
			for (int i = 0; i < contribuables.length; i++) {
				max1000ids.add(contribuables[i]); // pas plus de mille ids dans une clause sql 'IN'
				if (max1000ids.size() == 1000 || i == contribuables.length - 1) {
					sqlQuery.setParameter("libelle", value.libelleCommune);
					sqlQuery.setParameterList("ids", max1000ids);
					sqlQuery.setParameter("ts", DateHelper.getCurrentDate());
					sqlQuery.executeUpdate();
					max1000ids.clear();
					if (getStatusManager().interrupted()) {
						break for1;
					}
				}
			}
			getStatusManager().setMessage(String.format(statusMessage, --reste ,total), (total - reste) * 100 / total );
		}
	}

	private static class MapValue {

		final String libelleCommune;
		final Set<Long> contribuables = new HashSet<Long>();

		MapValue(String libelleCommune) {
			this.libelleCommune = libelleCommune;
		}
	}

}
