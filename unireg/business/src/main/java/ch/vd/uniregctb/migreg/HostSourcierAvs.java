package ch.vd.uniregctb.migreg;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import ch.vd.uniregctb.tiers.PersonnePhysique;

public class HostSourcierAvs extends HostMigrator {
	private int sourcierBadAvsStart = -1;
	private int sourcierBadAvsEnd = -1;
	private static final Logger HBADAVS = Logger.getLogger(HostSourcierAvs.class.getName() + ".HBadAvs");
	private static final Logger NHBADAVS = Logger.getLogger(HostSourcierAvs.class.getName() + ".NHBadAvs");

	public HostSourcierAvs(HostMigratorHelper h, MigRegLimits limits, MigregStatusManager mgr) {
		super(h, limits, mgr);
		if (limits.srcFirstBadAvs != null) {
			sourcierBadAvsStart = limits.srcFirstBadAvs;

			if (limits.srcEndBadAvs != null) {
				sourcierBadAvsEnd = limits.srcEndBadAvs;
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public int migrate() throws Exception {

		Object[] criteria = {
				sourcierBadAvsStart, sourcierBadAvsEnd
		};
		String query = "from PersonnePhysique pp where pp.ancienNumeroSourcier >= ? and pp.ancienNumeroSourcier <= ?";
		List<PersonnePhysique> listContribuableSourcier = helper.hibernateTemplate.find(query, criteria);

		for (PersonnePhysique personnePhysique : listContribuableSourcier) {
			findBadNumeroAvs(personnePhysique);
		}

		return 0;
	}

	private void findBadNumeroAvs(PersonnePhysique personnePhysique) {
		Long numeroIndividu = personnePhysique.getNumeroIndividu();
		Long ancienNumeroSourcier = personnePhysique.getAncienNumeroSourcier();
		// dans le cas d'un habitant:
		if (numeroIndividu != null && numeroIndividu > 0) {
			String sql11 = "select  NO_AVS from " + helper.getTableIs("CARACT_INDIVIDU") + " where FK_INDNO = " + numeroIndividu
					+ " and DAF_VALIDITE = '0001-01-01'" + " and NO_SEQUENCE = (select MAX (NO_SEQUENCE) from "
					+ helper.getTableIs("CARACT_INDIVIDU") + " where FK_INDNO =" + numeroIndividu + " and DAF_VALIDITE = '0001-01-01')";
			SqlRowSet rs11 = helper.isTemplate.queryForRowSet(sql11);
			String badNumaVS11 = null;
			if (rs11.first()) {
				String avsATraiter = rs11.getString("NO_AVS");
				if (avsATraiter.endsWith("000") || "".equals(avsATraiter)) {
					badNumaVS11 = avsATraiter;
				}

			}
			String sql13 = "select NAVS13 from " + helper.getTableIs("INDIVIDU") + " where NO_INDIVIDU = " + numeroIndividu;
			SqlRowSet rs13 = helper.isTemplate.queryForRowSet(sql13);
			String badNumaVS13 = null;
			if (rs13.first()) {
				badNumaVS13 = rs13.getString("NAVS13");
			}

			if (badNumaVS11 != null) {

				HBADAVS.info(ancienNumeroSourcier + ";" + numeroIndividu + ";" + badNumaVS11 + ";" + badNumaVS13);
			}

		}
		else {

			String sql = "SELECT NO_AVS FROM  " + helper.getTableIs("SOURCIER") + " WHERE NO_SOURCIER = " + ancienNumeroSourcier;

			SqlRowSet rs = helper.isTemplate.queryForRowSet(sql);
			String badNumaVS = null;
			if (rs.first()) {
				String avsATraiter = rs.getString("NO_AVS");
				if (avsATraiter.endsWith("000") || "".equals(avsATraiter)) {
					badNumaVS = avsATraiter;
				}

			}
			if (badNumaVS != null) {

				NHBADAVS.info(ancienNumeroSourcier + ";" +  badNumaVS);
			}


		}

	}

}
