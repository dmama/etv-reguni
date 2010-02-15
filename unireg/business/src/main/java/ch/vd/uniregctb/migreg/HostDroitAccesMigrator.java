package ch.vd.uniregctb.migreg;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.DroitAcces;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.type.TypeDroitAcces;

public class HostDroitAccesMigrator extends HostMigrator {

	private static final Logger LOGGER = Logger.getLogger(HostDroitAccesMigrator.class);
	private static final Logger RAPPORT = Logger.getLogger(HostDroitAccesMigrator.class.getName() + ".Rapport");
	private static final Logger REJET = Logger.getLogger(HostDroitAccesMigrator.class.getName() + ".Rejet");

	private int indiviDroitAccesStart = -1;
	private int indiviDroitAccesEnd = -1;

	public HostDroitAccesMigrator(HostMigratorHelper h, MigRegLimits limits, MigregStatusManager mgr) {
		super(h, limits, mgr);
		if (limits.indiDroitAccesFirst != null) {
			indiviDroitAccesStart = limits.indiDroitAccesFirst;

			if (limits.indiDroitAccesEnd != null) {
				indiviDroitAccesEnd = limits.indiDroitAccesEnd;
			}
		}
	}

	@Override
	public int migrate() throws Exception {
		// errorsManager = new MigregDroitAccesErrorsManager(helper, indiviDroitAccesStart, indiviDroitAccesEnd);
		int ret = 0;
		try {
			try {
				beginTransaction();

				ret = iterateOnDroitsAcces();
			}
			finally {
				endTransaction();
			}
		}
		catch (Exception ignored) {
			LOGGER.debug(ignored, ignored);
			errorsManager.onRollback();
		}

		// errorsManager.terminate();

		return ret;
	}

	private int iterateOnDroitsAcces() throws SQLException {
		int nbDroitAccesMigrated = 0;
		int nbDroitAutorisation = 0;
		int nbDroitInterdiction = 0;

		nbDroitAutorisation = migrateDroitAcces("AUT_ACCES_CONT", TypeDroitAcces.AUTORISATION);
		nbDroitInterdiction = migrateDroitAcces("INT_ACCES_CONT", TypeDroitAcces.INTERDICTION);
		LOGGER.info("Droit accès de type autorisation migrés par ce thread: " + nbDroitAutorisation);
		LOGGER.info("Droit accès de type interdiction migrés par ce thread: " + nbDroitInterdiction);
		nbDroitAccesMigrated = nbDroitAutorisation + nbDroitInterdiction;
		return nbDroitAccesMigrated;
	}

	@SuppressWarnings("unchecked")
	private int migrateDroitAcces(String table, TypeDroitAcces typeDroit) throws SQLException {

		int nbDroitAccesMigrated = 0;
		Statement stat = helper.db2Connection.createStatement();
		String sql = getDroitAccesHost(table);
		ResultSet rs = null;
		try {
			rs = stat.executeQuery(sql);
			while (rs.next()) {
				Tiers tiers = helper.tiersService.getTiers(rs.getLong("FK_CONTRIBUABLENO"));
				if (tiers instanceof PersonnePhysique) {

					createNewDroitAcces((PersonnePhysique)tiers, typeDroit, rs);
					nbDroitAccesMigrated++;

				}
				//Dans le cas d'un menage, les droits sont propagés à chaque personne du couple
				else if (tiers instanceof MenageCommun) {
					MenageCommun menage=(MenageCommun)tiers;
					HashSet<PersonnePhysique> personnes = (HashSet<PersonnePhysique>) menage.getPersonnesPhysiques();
					if (personnes!=null) {
						List listePersonne= Arrays.asList(personnes.toArray());
						for (Iterator iterator = listePersonne.iterator(); iterator.hasNext();) {
							PersonnePhysique personne = (PersonnePhysique) iterator.next();
							createNewDroitAcces(personne, typeDroit, rs);
							nbDroitAccesMigrated++;

						}

					}
					else  {
						REJET.info("le tiers couple " + rs.getLong("FK_CONTRIBUABLENO") + " n'a aucun membre");
					}
				}
				else if (tiers == null) {
					REJET.info("le tiers " + rs.getLong("FK_CONTRIBUABLENO") + " n'est pas dans unireg");
				}
			}
		}
		catch (Exception e) {
			LOGGER.error("Erreur lors de la migration des droits d'accès de type " + typeDroit.name() + " " + e.getMessage());
		}
		finally {
			rs.close();
			stat.close();
		}

		return nbDroitAccesMigrated;
	}

	private String getDroitAccesHost(String nomTable) {
		String sql = "select A.CO_ACTION," +
				" A.FK_INDNO," +
				" A.FK_CONTRIBUABLENO" +
				" from " + helper.db2Schema + "."+nomTable+" A," +
				" " + helper.db2Schema + ".VISA_OPERATEUR V" +
				" where A.FK_INDNO = V.FK_INDNO" +
				" AND  V.DAF_VALIDITE= '0001-01-01'";
		if (indiviDroitAccesStart > 0) {
			sql += " AND " + " A.FK_INDNO >= " + indiviDroitAccesStart;
		}
		if (indiviDroitAccesEnd > 0) {
			sql += " AND A.FK_INDNO <= " + indiviDroitAccesEnd;

		}
		LOGGER.info(sql);
		return sql;

	}

	private Niveau getNiveau(String code) {

		if (code.equals("C")) {
			return Niveau.LECTURE;
		}
		if (code.equals("M")) {
			return Niveau.ECRITURE;
		}
		return null;
	}

	private void createNewDroitAcces(PersonnePhysique personne,TypeDroitAcces typeDroit,ResultSet rs) throws Exception{
		if (helper.droitAccesDAO.getDroitAcces(rs.getLong("FK_INDNO"), personne.getNumero(), RegDate.get())==null) {
			DroitAcces droitAcces = new DroitAcces();
			droitAcces.setType(typeDroit);
			droitAcces.setNiveau(getNiveau(rs.getString("CO_ACTION")));
			droitAcces.setNoIndividuOperateur(rs.getLong("FK_INDNO"));
			droitAcces.setDateDebut(RegDate.get());

			droitAcces.setTiers(personne);
			helper.droitAccesDAO.save(droitAcces);
			int nb = status.addGlobalObjectsMigrated(1);
			if (nb % 100 == 0) {
				LOGGER.info("Nombre de droits sauvés: "+nb);
			}
			RAPPORT.info(rs.getLong("FK_INDNO")+";"+typeDroit.name()+";"+rs.getString("CO_ACTION")+";"+personne.getNumero());
		}
	}
}
