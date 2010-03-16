package ch.vd.uniregctb.migreg;

import java.util.ArrayList;

import org.springframework.jdbc.support.rowset.SqlRowSet;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.mouvement.MouvementDossier;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Tiers;

public class MouvementDossierLoader extends SubElementsFetcher {

	private static final String NO_CONTRIBUABLE = "FK_CONTRIBUABLENO";
	private static final String TYPE_MVT = "TY_MOUVEMENT";
	private static final String LOCALISATION = "LOC_INTERNE";
	private static final String COLLECTIVITE_ADM = "FK_COLADMNO";
	private static final String NO_INDIVIDU = "FK_INDNO";
	private static final String DA_MUT = "DA_MUT";
	private static final String VS_MUT = "VS_MUT";

	public MouvementDossierLoader(HostMigratorHelper helper, StatusManager mgr, MigregErrorsManager errorsManager) {
		super(helper, mgr);
	}

	public ArrayList<MigrationError> loadMvtDossier(ArrayList<Tiers> lstTiers, ArrayList<MigrationError> errors) throws Exception {

		Assert.fail("Méthode non maintenue");

//		SqlRowSet mvtDossier = readMvtDossier(lstTiers);
//		long savedNoCtb = 0L;
//		MouvementDossier mouvDossier = null;
//		while (mvtDossier != null && mvtDossier.next() && !mgr.interrupted()) {
//			if (savedNoCtb != mvtDossier.getLong(NO_CONTRIBUABLE)) {
//				if (mvtDossier.getString(TYPE_MVT).equals("E") ||
//					mvtDossier.getString(TYPE_MVT).equals("A")) {
//					ReceptionDossier rcpDos = new ReceptionDossier();
//					mouvDossier = rcpDos;
//					rcpDos.setLocalisation(Localisation.PERSONNE);
//					if (mvtDossier.getString(LOCALISATION).equals("A")) {
//						rcpDos.setLocalisation(Localisation.ARCHIVES);
//					}
//					if (mvtDossier.getString(LOCALISATION).equals("C")) {
//						rcpDos.setLocalisation(Localisation.CLASSEMENT_GENERAL);
//					}
//					if (mvtDossier.getString(LOCALISATION).equals("I")) {
//						rcpDos.setLocalisation(Localisation.CLASSEMENT_INDEPENDANTS);
//					}
//				} else {
//					EnvoiDossier envDos = new EnvoiDossier();
//					mouvDossier = envDos;
//					try {
//					envDos.setCollectiviteAdministrativeEmettrice(helper.tiersService.getCollectiviteAdministrative(mvtDossier.getInt(COLLECTIVITE_ADM),true));
//					} catch (Exception e) {
//						Audit.error("Problème à la lecture de la collectivité administrative destinataire de l'envoi du dossier");
//						continue;
//					}
//				}
//				if (mvtDossier.getLong(NO_INDIVIDU) > 0) {
//					mouvDossier.setNumeroIndividu(mvtDossier.getLong(NO_INDIVIDU));
//				}
//				mouvDossier.setLogCreationDate(mvtDossier.getDate(DA_MUT));
//				mouvDossier.setLogCreationUser(mvtDossier.getString(VS_MUT));
//
//				addMouvDossier(lstTiers, mvtDossier.getLong(NO_CONTRIBUABLE), mouvDossier);
//				savedNoCtb = mvtDossier.getLong(NO_CONTRIBUABLE);
//			}
//		}
//
		return errors;
	}

	private void addMouvDossier(ArrayList<Tiers> lstTiers, Long noCtb, MouvementDossier md) {
		for (Tiers t : lstTiers) {
			Contribuable ctb = (Contribuable) t;
			ctb.addMouvementDossier(md);
			break;
		}
	}

	private SqlRowSet readMvtDossier(ArrayList<Tiers> listCtb) throws Exception {
		StringBuilder sbCtb = new StringBuilder();
		for (Tiers tiers : listCtb) {
			sbCtb.append(tiers.getNumero());
			sbCtb.append(",");
		}
		if (sbCtb.length() == 0) {
			return null;
		}
		sbCtb.deleteCharAt(sbCtb.lastIndexOf(","));

		String query =
			"SELECT" +
			" A.NUMERO" +
			", A.DATE_MOUVEMENT" +
			", A.TY_MOUVEMENT" +
			", A.TY_DOSSIER" +
			", A.VISA_MOUVEMENT" +
			", A.COMMENTAIRE" +
			", A.LOC_INTERNE" +
			", A.DAA_MOUVEMENT" +
			", A.DA_MUT" +
			", A.HR_MUT" +
			", A.VS_MUT" +
			", A.NO_MAJ" +
			", A.FK_CONTRIBUABLENO" +
			", A.FK_INDNO" +
			", A.FK_COLADMNO" +
			", A.FK_FK_CONTNO" +
			", A.FK_DI_AN_FISC" +
			", A.FK_DI_NO_PAR_AN" +
			" FROM "+helper.getTableDb2("MOUVEMENT_DOSSIER")+" A" +
			" WHERE A.TY_DOSSIER = 'DC'" +
			" AND A.FK_CONTRIBUABLENO IN (" +sbCtb.toString()+")"+
			" AND A.DAA_MOUVEMENT = '0001-01-01'" +
			" ORDER BY A.DATE_MOUVEMENT DESC";

		HostMigratorHelper.SQL_LOG.debug("Query: " + query);

		return helper.db2Template.queryForRowSet(query);

	}

}
