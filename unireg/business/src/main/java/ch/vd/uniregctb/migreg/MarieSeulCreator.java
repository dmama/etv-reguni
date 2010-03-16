package ch.vd.uniregctb.migreg;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public class MarieSeulCreator extends HostMigrator {

	private static final Logger LOGGER = Logger.getLogger(MarieSeulCreator.class);


	private static final String NUMERO_INDIVIDU = "FK_INDNO";
	private static final String NUMERO_CONTRIBUABLE = "FK_CONTRIBUABLENO";
//	private static final String DATE_VALIDITE = "DA_VALIDITE";
	//private static final String NO_SEQUENCE = "NO_SEQUENCE";

	public MarieSeulCreator(HostMigratorHelper helper, MigregStatusManager mgr) {
		super(helper, mgr);

	}

	@Override
	public int migrate() throws Exception {

		setRunningMessage("Création des tiers 'mariés seuls'...");

		// Chargement des "mariés seuls".

//		tempReadMenageCommunSansRapport();
		int nb = loadMariesSeuls();

//		endTransaction();

//		return 0;
		return nb;
	}

	private int loadMariesSeuls() {

		int nbMigrated = 0;

		try {
//			beginTransaction();
			SqlRowSet ms = readMariesSeuls();
//			StringBuilder listTiers = new StringBuilder();
			HashMap<Long, Long> allMariesSeuls = new HashMap<Long, Long>();
//			Map<Long,RegDate> mapInd = new HashMap<Long, RegDate>();
			List<Long> lotTiersAModifier = new ArrayList<Long>();
			while (ms != null && ms.next() && !isInterrupted()) {
				Contribuable marieSeul = (Contribuable) helper.tiersService.getTiers(ms.getLong(NUMERO_CONTRIBUABLE));
				if ( marieSeul == null) {
					continue;
				}
				else {
					if (marieSeul instanceof MenageCommun) {
						continue;
					}
				}
				lotTiersAModifier.add(ms.getLong(NUMERO_CONTRIBUABLE));
				allMariesSeuls.put(ms.getLong(NUMERO_CONTRIBUABLE), ms.getLong(NUMERO_INDIVIDU));
//				mapInd.put(ms.getLong(NUMERO_CONTRIBUABLE), RegDate.get(ms.getDate(DATE_VALIDITE)));
				if (lotTiersAModifier.size() > 500) {
					updateTiers(lotTiersAModifier);
					lotTiersAModifier = new ArrayList<Long>();
				}
			}
			if (lotTiersAModifier.size() > 0) {
				updateTiers(lotTiersAModifier);
			}

//			compteur = 0;
//			StringBuilder listIndividus = new StringBuilder();
//			for (Long noCtb : allMariesSeuls.keySet()) {
//				listIndividus.append(noCtb);
//				listIndividus.append(",");
//				if (++compteur > 500 && listIndividus.length() > 0) {
//					listIndividus.deleteCharAt(listIndividus.lastIndexOf(","));
//					updateTypeTiers(listIndividus);
//					listIndividus = new StringBuilder();
//					compteur = 0;
//					commitAndOpenTransaction();
//				}
//			}
//			if (compteur > 0 && listIndividus.length() > 0) {
//				listIndividus.deleteCharAt(listIndividus.lastIndexOf(","));
//				updateTypeTiers(listIndividus);
//				commitAndOpenTransaction();
//			}

//			endTransaction();
			beginTransaction();


//			for (Long noCtb : allMariesSeuls.keySet()) {
			for (Map.Entry<Long, Long> habitant  : allMariesSeuls.entrySet()) {
				MenageCommun coupleMarieSeul = (MenageCommun) helper.tiersService.getTiers(habitant.getKey());
				if (coupleMarieSeul == null) {
					continue;
				}
				PersonnePhysique tiersMarieSeul = new PersonnePhysique(true);
//				tiersMarieSeul.setAdressesTiers(coupleMarieSeul.getAdressesTiers());
//				tiersMarieSeul.setDeclarations(coupleMarieSeul.getDeclarations());
//				tiersMarieSeul.setForsFiscaux(coupleMarieSeul.getForsFiscaux());
				tiersMarieSeul.setComplementNom(coupleMarieSeul.getComplementNom());
				tiersMarieSeul.setBlocageRemboursementAutomatique(coupleMarieSeul.getBlocageRemboursementAutomatique());
				tiersMarieSeul.setDebiteurInactif(coupleMarieSeul.isDebiteurInactif());
				tiersMarieSeul.setLogCreationDate(coupleMarieSeul.getLogCreationDate());
				tiersMarieSeul.setLogCreationUser(coupleMarieSeul.getLogCreationUser());
				tiersMarieSeul.setLogModifDate(coupleMarieSeul.getLogModifDate());
				tiersMarieSeul.setLogModifUser(coupleMarieSeul.getLogModifUser());
				tiersMarieSeul.setNumeroCompteBancaire(coupleMarieSeul.getNumeroCompteBancaire());
				tiersMarieSeul.setNumeroTelecopie(coupleMarieSeul.getNumeroTelecopie());
				tiersMarieSeul.setNumeroTelephonePortable(coupleMarieSeul.getNumeroTelephonePortable());
				tiersMarieSeul.setNumeroTelephonePrive(coupleMarieSeul.getNumeroTelephonePrive());
				tiersMarieSeul.setNumeroTelephoneProfessionnel(coupleMarieSeul.getNumeroTelephoneProfessionnel());
				tiersMarieSeul.setPersonneContact(coupleMarieSeul.getPersonneContact());
				tiersMarieSeul.setRemarque(coupleMarieSeul.getRemarque());
				tiersMarieSeul.setTitulaireCompteBancaire(coupleMarieSeul.getTitulaireCompteBancaire());
				tiersMarieSeul.setNumeroIndividu(habitant.getValue());
				tiersMarieSeul.setNumero(new Long(helper.listContribuableFree.get(0)));
				helper.listContribuableFree.remove(0);
				LOGGER.info("No tiers marié seul : "+coupleMarieSeul.getNumero());
				try {
					helper.tiersService.addTiersToCouple(coupleMarieSeul, tiersMarieSeul, coupleMarieSeul.getForsFiscauxPrincipauxActifsSorted().iterator().next().getDateDebut(), null);
					commitAndOpenTransaction();
				}
				catch (Exception e) {
					LOGGER.error(e.getMessage());

				}
				nbMigrated++;
			}

			endTransaction();



		}
		catch (Exception e) {
			nbMigrated = 0;
			LOGGER.error(e, e);
		}

		return nbMigrated;
	}

//	@SuppressWarnings("unused")
//	private RegDate getDateFinMarieSeul(Individu ind, int noSeqMarieSeul) {
//		RegDate dateFin = null;
//		for (EtatCivil etatCivil : ind.getEtatsCivils()) {
//			if (noSeqMarieSeul < etatCivil.getNoSequence()) {
//				dateFin = etatCivil.getDateDebutValidite().getOneDayBefore();
//			}
//		}
//
//		return dateFin;
//	}

	private void updateTiers(List<Long> listeTiers) throws Exception {
		StringBuilder tiersAModifier = new StringBuilder();
		for (Long noTiers : listeTiers) {
			tiersAModifier.append(noTiers);
			tiersAModifier.append(",");
		}
		if (tiersAModifier.length() == 0) {
			return;
		}
		tiersAModifier.deleteCharAt(tiersAModifier.lastIndexOf(","));

		updateTypeTiers(tiersAModifier);

	}

	private SqlRowSet readMariesSeuls() {
		String query =
			"SELECT " +
			" A.NO_SEQUENCE" +
			" , A.DA_VALIDITE" +
			" , A.CODE_ETAT_CIVIL" +
			" , A.FK_INDNO" +
			" , B.FK_CONTRIBUABLENO" +
			" FROM " + helper.getTableDb2("ETAT_CIVIL_IND") + " A" +
			" , "+helper.getTableDb2("REGROUPEMENT_IND") + " B" +
			" WHERE A.MARIE_SEUL = 'O'" +
			" AND A.DATE_ANNULATION " + helper.getSqlDateIsNull(false) +
			" AND B.DAD_APPARTENANCE <> B.DAF_APPARTENANCE " +
			" AND B.FK_INDNO = A.FK_INDNO " +
			" AND NOT EXISTS " +
			" ( " +
				"SELECT 1 " +
				" FROM " + helper.getTableDb2("ETAT_CIVIL_IND") + " C" +
				" WHERE C.FK_INDNO = A.FK_INDNO " +
				" AND C.DATE_ANNULATION " + helper.getSqlDateIsNull(false) +
				" AND C.NO_SEQUENCE > A.NO_SEQUENCE" +
			" ) ";

		HostMigratorHelper.SQL_LOG.debug("Query: " + query);

		return helper.db2Template.queryForRowSet(query);
	}


//	@SuppressWarnings("unused")
//	private void tempReadMenageCommunSansRapport() throws Exception {
//		String query =
//			"SELECT NUMERO FROM TIERS WHERE TIERS_TYPE='MenageCommun' AND NOT EXISTS (SELECT 1 FROM RAPPORT_ENTRE_TIERS WHERE TIERS_OBJET_ID = TIERS.NUMERO)";
//		Statement stmt = helper.orclConnection.createStatement();
//		ResultSet rs = stmt.executeQuery(query);
//
//		while (rs.next()) {
//			Long noCtb = rs.getLong("NUMERO");
//			String query1 =
//				"SELECT DISTINCT(FK_INDNO) FROM "+helper.getTableDb2("REGROUPEMENT_IND")+" WHERE FK_CONTRIBUABLENO = "+noCtb;
//			Statement stmtDb2 = helper.db2Connection.createStatement();
//			ResultSet rsDb2 = stmtDb2.executeQuery(query1);
//			while (rsDb2.next()) {
//				String query2 =
//					"Update "+helper.getTableOrcl("TIERS")+" set TIERS_TYPE='Habitant', NUMERO_INDIVIDU="+rsDb2.getInt("FK_INDNO")+" where NUMERO = "+noCtb;
//				Statement stmtupd = helper.orclConnection.createStatement();
//				stmtupd.executeQuery(query2);
//				stmtupd.close();
//			}
//			stmtDb2.close();
//		}
//	}

	private void updateTypeTiers(StringBuilder listeTiers) throws Exception {
		String query =
			"UPDATE " + helper.getTableOrcl("TIERS") +
			" SET TIERS_TYPE = 'MenageCommun' ," +
			" NUMERO_INDIVIDU = NULL " +
			"WHERE NUMERO IN (" + listeTiers.toString() +")";

		HostMigratorHelper.SQL_LOG.debug("Query: " + query);

		Statement stmt = helper.orclConnection.createStatement();
		//ResultSet rs = stmt.executeQuery(query);
		stmt.executeQuery(query);

	}

}
