package ch.vd.uniregctb.migreg;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.FlushMode;
import org.springframework.jdbc.support.rowset.ResultSetWrappingSqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseEtrangere;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.adresse.AdresseSupplementaire;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.Constants;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeMigRegError;

public class HostContribuableMigrator extends HostMigrator {


	private static final Logger LOGGER = Logger.getLogger(HostContribuableMigrator.class);

	/**
	 * Champs pour la recup des infos du SQL results set
	 */
	private static final String NUMERO_CTB = "NO_CONTRIBUABLE";
	private static final String NO_INDIVIDU = "FK_INDNO";
	private static final String REMB_AUTOMATIC = "BLOCAGE_REMB_AUTO";
	private static final String CO_IROLE = "CO_IROLE";
	private static final String ADR_CHEZ = "CHEZ";
	private static final String ADR_RUE = "RUE";
	private static final String ADR_NO_POLICE = "NO_POLICE";
	private static final String ADR_LIEU = "LIEU";
	private static final String ADR_ID_LOC_POSTAL = "FK_LOC_POSNO";
	private static final String ADR_ID_RUE = "FK_RUENO";
	private static final String ADR_ID_PAYS = "FK_PAYSNO_OFS";
	private static final String BIC = "BIC";
	private static final String NUMERO_COMPTE_BANCAIRE = "NO_COMPTE_BANCAIRE";
	private static final String TITULAIRE_COMPTE_BANCAIRE = "TITULAIRE_DU_COMPT";
	private static final String IBAN = "IBAN";
	private static final String NUMERO_CCP = "NO_CCP";
	private static final String NUMERO_TEL_FIXE = "NO_TEL_FIXE";
	private static final String NUMERO_MOBILE = "NO_TEL_PORTABLE";
	private static final String COURRIEL = "EMAIL";
	private static final String NOM_CTB = "NOM_COURRIER_1";
	private static final String PRENOM_CTB = "NOM_COURRIER_2";
	private static final String I107 = "ORIGINE_I107";
	private static final String DATE_MUTATION = "DA_MUT";
	private static final String VISA_MUTATION = "VS_MUT";

	private final int ctbStart;
	private final int ctbEnd;
	private final boolean doCouple;
	private final List<Long> listCtbInError;

	private final DeclarationsLoader diLoader;
	private final ForsFiscauxLoader forsFiscauxLoader;
	private final AdressesLoader adressesLoader;
	private final MouvementDossierLoader mouvementDossierLoader;

	private class RowRs {
		private Long noContribuable;
		private String nomCourrier1;
		private String nomCourrier2;
		private String chez;
		private String rue;
		private String noPolice;
		private String lieu;
		private String bic;
		private String noTelFixe;
		private String noTelPortable;
		private String email;
		private String iban;
		private String titulaireDuCompt;
		private String noCompteBancaire;
		private boolean blocageRembAuto;
		private boolean origineI107;
		private String noCcp;
		private boolean coIrole;
		private int fkLocPosno;
		private int fkRueno;
		private int fkPaysnoOfs;
		private Long fkIndno;
		private Date dateMutation;
		private String visaMutation;
	};

	public HostContribuableMigrator(HostMigratorHelper helper, MigRegLimits limits, MigregStatusManager mgr, List<Long> listCtbInError) throws Exception {
		super(helper, limits, mgr);
		this.ctbStart = limits.ctbFirst;
		this.ctbEnd = limits.ctbEnd;
		this.doCouple = limits.isWantCouple();
		this.listCtbInError = listCtbInError;

		errorsManager = new MigregCtbErrorsManager(helper, ctbStart, ctbEnd);
		diLoader = new DeclarationsLoader(helper, mgr);
		forsFiscauxLoader = new ForsFiscauxLoader(helper, mgr, errorsManager);
		adressesLoader = new AdressesLoader(helper, mgr);
		mouvementDossierLoader = new MouvementDossierLoader(helper, mgr, errorsManager);
	}

	@Override
	public int migrate() throws Exception {

		int nbCtbMigrated = 0;

		try {
			beginTransaction();
			try {
				if (existsCtbInRange(ctbStart, ctbEnd)) {
					if (!isInterrupted()) {
						nbCtbMigrated += loadRoleOrdinaire(ctbStart, ctbEnd, doCouple, listCtbInError);
					}
					if (!isInterrupted()) {
						nbCtbMigrated += loadCtbCloseWithIndividu(ctbStart, ctbEnd, doCouple, listCtbInError);
					}
					if (!isInterrupted()) {
						nbCtbMigrated += loadCtbPourTous(ctbStart, ctbEnd, listCtbInError);
					}
					if (!doCouple) {
						if (!isInterrupted()) {
							nbCtbMigrated += loadCtbOpenWhithoutIndividu(ctbStart, ctbEnd, listCtbInError);
						}
						if (!isInterrupted()) {
							nbCtbMigrated += loadCtbCloseWithoutIndividu(ctbStart, ctbEnd, listCtbInError);
						}
					}
				}
			}
			finally {
				endTransaction();
			}
		}
		catch (Exception ignored) {
			LOGGER.error(ignored, ignored);
			errorsManager.onRollback();
		}

		errorsManager.terminate();

		return nbCtbMigrated;
	}

	private int loadCtbOpenWhithoutIndividu(int ctbStart, int ctbEnd, List<Long> listCtbInErrors) throws Exception {

		int nbCtbMigrated = 0;

		String query = readCtbOpenWithoutIndividu(ctbStart, ctbEnd);
		Statement stat = helper.db2Connection.createStatement();
		try {
			ResultSet rs = stat.executeQuery(query);
			try {
				while (rs.next() && !isInterrupted()) {

					RowRs row = buildRsRow(new ResultSetWrappingSqlRowSet(rs));
					if (!listCtbInErrors.isEmpty() && !listCtbInErrors.contains(row.noContribuable)) {
						continue;
					}

					try {

						// Audit.info("Numéro CTB lu : "+numeroCtb);
						if (helper.tiersDAO.exists(row.noContribuable, FlushMode.MANUAL)) {
							continue;
						}

						if (isForPrincipalActifOneCtbVd(row.noContribuable)) {
							String message = "Contribuable vaudois " + row.noContribuable + " n'a pas de regroupement avec un individu";
							Audit.info(message);
//							errorsManager.setInError(row.noContribuable, message, row.nomCourrier1, TypeMigRegError.ERROR_APPLICATIVE);
						}
//						else {
						PersonnePhysique nonHab = new PersonnePhysique(false);
						nonHab.setNumero(row.noContribuable);
						rowRs2NonHabitant(row, nonHab);
						saveTiers(nonHab);
						doCommit();
						nbCtbMigrated++;
//						}
					}
					catch (Exception e) {
						String message = "Le tiers " + row.noContribuable + " ne peut être inséré. Cause=" + e.getMessage();
						errorsManager.setInError(row.noContribuable, message, row.nomCourrier1, TypeMigRegError.ERROR_APPLICATIVE);
					}
				}
			}
			finally {
				rs.close();
			}
		}
		finally {
			stat.close();
		}
		// LOGGER.info("Fin de Load CTB open without individu (nb="+nbCtbMigrated+")");
		return nbCtbMigrated;
	}

	private int loadCtbCloseWithIndividu(int ctbStart, int ctbEnd, boolean doCouple, List<Long> listCtbInErrors) throws Exception {

		int nbCtbMigrated = 0;

		String query = readCtbAssujCloseRegroupementClose(ctbStart, ctbEnd);
		nbCtbMigrated = createContribuable(listCtbInErrors, nbCtbMigrated, query);
		// LOGGER.info("Fin de Load CTB close with individu (nb="+nbCtbMigrated+")");
		return nbCtbMigrated;
	}

	private int createContribuable(List<Long> listCtbInErrors, int nbCtbMigrated, String query) throws SQLException, Exception {

		Statement stat = helper.db2Connection.createStatement();
		try {
			ResultSet rs = stat.executeQuery(query);
			try {
				int nbCtbLus = 0;
				Long saveNoCtb = 0L;
				Long saveNoInd = 0L;
				List<RowRs> membreFam = new ArrayList<RowRs>();
				while (rs.next() && !isInterrupted()) {
					RowRs row = buildRsRow(new ResultSetWrappingSqlRowSet(rs));
					if (!listCtbInErrors.isEmpty() && !listCtbInErrors.contains(row.noContribuable)) {
						continue;
					}
					if (row.fkIndno.equals(saveNoInd) && row.noContribuable.equals(saveNoCtb)) {
						continue;
					}
					saveNoInd = row.fkIndno;
					nbCtbLus++;
					try {
						if (!helper.tiersDAO.exists(row.noContribuable, FlushMode.MANUAL)) {
							if (!saveNoCtb.equals(row.noContribuable)) {
								if (!saveNoCtb.equals(0L)) {
									prepareCtb(membreFam);
									nbCtbMigrated++;
									membreFam = new ArrayList<RowRs>();
								}
								saveNoCtb = row.noContribuable;
							}
							membreFam.add(row);
						}
					}
					catch (Exception e) {
						String message = "Le tiers " + row.noContribuable + " ne peut être inséré. Cause=" + e.getMessage();
						errorsManager.setInError(row.noContribuable, message, row.nomCourrier1, TypeMigRegError.ERROR_APPLICATIVE);
					}
				} // while RS

				if (membreFam.size() > 0) {
					prepareCtb(membreFam);
					nbCtbMigrated++;
				}
			}
			finally {
				rs.close();
			}
		}
		finally {
			stat.close();
		}
		return nbCtbMigrated;
	}

	private int loadCtbPourTous(int ctbStart, int ctbEnd, List<Long> listCtbInErrors) throws Exception {

		int nbCtbMigrated = 0;

		String query = readCtbPourTous(ctbStart, ctbEnd);
		nbCtbMigrated = createContribuable(listCtbInErrors, nbCtbMigrated, query);
//		Statement stat = helper.db2Connection.createStatement();
//		try {
//			ResultSet rs = stat.executeQuery(query);
//			try {
//				int nbCtbLus = 0;
//				Long saveNoCtb = 0L;
//				Long saveNoInd = 0L;
//				List<RowRs> membreFam = new ArrayList<RowRs>();
//				while (rs.next() && !isInterrupted()) {
//					RowRs row = buildRsRow(new ResultSetWrappingSqlRowSet(rs));
//					if (!listCtbInErrors.isEmpty() && !listCtbInErrors.contains(row.noContribuable)) {
//						continue;
//					}
//					try {
//						if (helper.tiersDAO.exists(row.noContribuable, FlushMode.MANUAL)) {
//							continue;
//						}
//						if (row.fkIndno.equals(saveNoInd) && row.noContribuable.equals(saveNoCtb)) {
//							continue;
//						}
//						saveNoInd = row.fkIndno;
//						nbCtbLus++;
//						try {
//							if (!helper.tiersDAO.exists(row.noContribuable, FlushMode.MANUAL)) {
//								if (!saveNoCtb.equals(row.noContribuable)) {
//									if (!saveNoCtb.equals(0L)) {
//										prepareCtb(membreFam);
//										nbCtbMigrated++;
//										membreFam = new ArrayList<RowRs>();
//									}
//									saveNoCtb = row.noContribuable;
//								}
//								membreFam.add(row);
//							}
//						}
//						catch (Exception e) {
//							String message = "Le tiers " + row.noContribuable + " ne peut être inséré. Cause=" + e.getMessage();
//							errorsManager.setInError(row.noContribuable, message, row.nomCourrier1, TypeMigRegError.ERROR_APPLICATIVE);
//						}
//					} // while RS
//
//					if (membreFam.size() > 0) {
//						prepareCtb(membreFam);
//						nbCtbMigrated++;
//					}
//						PersonnePhysique h = new PersonnePhysique(true);
//						h.setNumeroIndividu(row.fkIndno);
//						rowRs2Tiers(row, h);
//						saveTiers(h);
//						doCommit();
//						nbCtbMigrated++;
//					}
//					catch (Exception e) {
//						String message = "Le tiers " + row.noContribuable + " ne peut être inséré. Cause=" + e.getMessage();
//						errorsManager.setInError(row.noContribuable, message, row.nomCourrier1, TypeMigRegError.ERROR_APPLICATIVE);
//					}
//				} // while RS
//			}
//			finally {
//				rs.close();
//			}
//		}
//		finally {
//			stat.close();
//		}
//
		//LOGGER.info("Fin de Load CTB pour tous (nb=" + nbCtbMigrated + ")");
		return nbCtbMigrated;
	}

	private int loadCtbCloseWithoutIndividu(int ctbStart, int ctbEnd, List<Long> listCtbInErrors) throws Exception {

		int nbCtbMigrated = 0;

		String query = readCtbAssujCloseWithoutRegroupement(ctbStart, ctbEnd);
		Statement stat = helper.db2Connection.createStatement();
		try {
			ResultSet rs = stat.executeQuery(query);
			try {
				while (rs.next() && !isInterrupted()) {
					RowRs row = buildRsRow(new ResultSetWrappingSqlRowSet(rs));
					if (!listCtbInErrors.isEmpty() && !listCtbInErrors.contains(row.noContribuable)) {
						continue;
					}
					try {
						if (helper.tiersDAO.exists(row.noContribuable, FlushMode.MANUAL)) {
							continue;
						}
						PersonnePhysique nonHab = new PersonnePhysique(false);
						nonHab.setNumero(row.noContribuable);
						rowRs2NonHabitant(row, nonHab);
						saveTiers(nonHab);
						doCommit();
						nbCtbMigrated++;
					}
					catch (Exception e) {
						String message = "Le tiers " + row.noContribuable + " ne peut être inséré. Cause=" + e.getMessage();
						errorsManager.setInError(row.noContribuable, message, row.nomCourrier1, TypeMigRegError.ERROR_APPLICATIVE);
					}
				}
			}
			finally {
				rs.close();
			}
		}
		finally {
			stat.close();
		}

		// LOGGER.info("Fin de Load CTB close without Individu (nb="+nbCtbMigrated+")");
		return nbCtbMigrated;
	}

	/**
	 * Effectue le chargement des tiers "rôle ordinaire"
	 *
	 * @param ctbStart
	 * @param ctbEnd
	 * @return int le nombre de contribuables migrés
	 * @throws Exception
	 */
	private int loadRoleOrdinaire(int ctbStart, int ctbEnd, boolean doCouple, List<Long> listCtbInErrors) throws Exception {

		int nbCtbMigrated = 0;

		// Lire les contribuables au role ordinaire
		String query = readCtbRoleOrdinaire(ctbStart, ctbEnd);
		Statement stmt = helper.db2Connection.createStatement();
		try {
			ResultSet rs = stmt.executeQuery(query);
			try {
				Long saveNoCtb = 0L;
				List<RowRs> membreFam = new ArrayList<RowRs>();
				Long saveNoInd = 0L;
				while (rs.next() && !isInterrupted()) {
					RowRs row = buildRsRow(new ResultSetWrappingSqlRowSet(rs));
					if (!listCtbInErrors.isEmpty() && !listCtbInErrors.contains(row.noContribuable)) {
						continue;
					}
					if (row.fkIndno.equals(saveNoInd) && row.noContribuable.equals(saveNoCtb)) {
						continue;
					}
					saveNoInd = row.fkIndno;
					try {
						if (!saveNoCtb.equals(row.noContribuable)) {
							if (!saveNoCtb.equals(0L)) {
								prepareCtb(membreFam);
								nbCtbMigrated++;
								membreFam = new ArrayList<RowRs>();
							}
							saveNoCtb = row.noContribuable;
						}
						membreFam.add(row);
					}
					catch (Exception e) {
						String message = "Le tiers " + row.noContribuable + " ne peut être inséré. Cause=" + e.getMessage();
						errorsManager.setInError(row.noContribuable, message, row.nomCourrier1, TypeMigRegError.ERROR_APPLICATIVE);
					}
				} // while RS

				if (membreFam.size() > 0) {
					prepareCtb(membreFam);
					nbCtbMigrated++;
				}
			}
			finally {
				rs.close();
			}
		}
		finally {
			stmt.close();
		}

		//LOGGER.info("Fin de Load CTB role ordinaire (nb=" + nbCtbMigrated + ")");
		return nbCtbMigrated;
	}

	private RowRs buildRsRow(SqlRowSet rs) throws Exception {
		RowRs rrs = new RowRs();
		rrs.noContribuable = rs.getLong(NUMERO_CTB);
		rrs.nomCourrier1 = rs.getString(NOM_CTB);
		rrs.nomCourrier2 = rs.getString(PRENOM_CTB);
		rrs.chez = rs.getString(ADR_CHEZ);
		rrs.rue = rs.getString(ADR_RUE);
		rrs.noPolice = rs.getString(ADR_NO_POLICE);
		rrs.lieu = rs.getString(ADR_LIEU);
		rrs.bic = rs.getString(BIC);
		rrs.noTelFixe = rs.getString(NUMERO_TEL_FIXE);
		rrs.noTelPortable = rs.getString(NUMERO_MOBILE);
		rrs.email = rs.getString(COURRIEL);
		rrs.iban = rs.getString(IBAN);
		rrs.titulaireDuCompt = rs.getString(TITULAIRE_COMPTE_BANCAIRE);
		rrs.noCompteBancaire = rs.getString(NUMERO_COMPTE_BANCAIRE);
		rrs.blocageRembAuto = (rs.getString(REMB_AUTOMATIC).equals(Constants.OUI));
		rrs.origineI107 = (rs.getString(I107).equals(Constants.OUI));
		rrs.noCcp = rs.getString(NUMERO_CCP);
		rrs.coIrole = (rs.getString(CO_IROLE).equals(Constants.OUI));
		rrs.fkLocPosno = rs.getInt(ADR_ID_LOC_POSTAL);
		rrs.fkRueno = rs.getInt(ADR_ID_RUE);
		rrs.fkPaysnoOfs = rs.getInt(ADR_ID_PAYS);
		rrs.dateMutation = rs.getDate(DATE_MUTATION);
		rrs.visaMutation = rs.getString(VISA_MUTATION);
		try {
			rrs.fkIndno = rs.getLong(NO_INDIVIDU);
//			rrs.dadAppartenance = RegDate.get(rs.getDate(DATE_DEBUT_APPARTENANCE));
//			rrs.dafAppartenance = RegDate.get(rs.getDate(DATE_FIN_APPARTENANCE));
		}
		catch (Exception ignored) {
//			LOGGER.error(ignored.getMessage());
			// Les colonnes de la table REGROUEPEMENT_IND peuvent être absentes.
		}
		return rrs;
	}

	private void prepareCtb(List<RowRs> membreFam) {

		long beginPreCtb = System.nanoTime();
		if (membreFam.size() > 0) {

			RowRs membre1 = membreFam.get(0);

			try {


				if (membreFam.size() == 1) {
					if (!doCouple) {
						if (!errorsManager.tiersExistedAtStart(membre1.noContribuable)) {
							Tiers tiers = buildCtbSingle(membre1);
							if (tiers.getNumero() != null) {
								saveTiers(tiers);
								doCommit();
							}
						}
					}
				}
				else {
					if (doCouple) {
						long begin = System.nanoTime();
						if (!errorsManager.tiersExistedAtStart(membre1.noContribuable)) {
							long end = System.nanoTime();
							LOGGER.debug("Tiers couple "+membre1.noContribuable+" non trouvé. Temps de contrôle : "+formatDiffTime(end,begin)+"[millisec]");
							Tiers couple = buildCtbCouple(membreFam);
							if (couple.getNumero() != null) {
								saveTiers(couple);
								doCommit();
							}
						} else {
							long end = System.nanoTime();
							LOGGER.debug("Tiers couple trouvé "+membre1.noContribuable+". Temps de contrôle : "+formatDiffTime(end,begin)+"[millisec]");
						}
					}
				}
			}
			catch (Exception e) {
				String message = "Le tiers " + membre1.noContribuable + " ne peut être inséré. Cause=" + getRootMessage(e);
				LOGGER.warn(message);
				errorsManager.setInError(membre1.noContribuable, message, TypeMigRegError.ERROR_APPLICATIVE);
			}
			long endPrepCtb = System.nanoTime();
			LOGGER.debug("Préparation Ctb "+membre1.noContribuable+". Temps exécution : "+formatDiffTime(endPrepCtb, beginPreCtb)+"[millisec]");
		}
	}

	private Contribuable buildCtbCouple(List<RowRs> memberCouple) throws Exception {

		PersonnePhysique tiersCtb = null;
		PersonnePhysique tiersCtbConjoint = null;
		Long noCtbCouple = 0L;
		RowRs rowRsCouple = null;
		Long ind0 = null;
		Long ind1 = null;
		long beginCouple = System.nanoTime();
		for (RowRs row : memberCouple) {
			noCtbCouple = row.noContribuable;
			LOGGER.debug("Début de création du couple : "+row.noContribuable);

			long begin = System.nanoTime();
			PersonnePhysique tiers = helper.tiersDAO.getHabitantByNumeroIndividu(row.fkIndno, true);
			long end = System.nanoTime();
			LOGGER.debug("Lecture du tiers membre. Temps de contrôle : "+formatDiffTime(end,begin)+"[millisec]");
			if (tiersCtb == null) {
				tiersCtb = tiers;
			}
			else {
				tiersCtbConjoint = tiers;
				rowRsCouple = row;
			}

			if (ind0 == null) {
				ind0 = row.fkIndno;
			}
			else {
				ind1 = row.fkIndno;
			}
		}

		MigrationError me = helper.migrationErrorDAO.getErrorForContribuable(noCtbCouple);
		if (me != null && TypeMigRegError.A_TRANSFORMER_EN_PP.equals(me.getTypeErreur())) {
			PersonnePhysique pp = new PersonnePhysique();
			pp.setHabitant(false);
			pp.setRemarque("Le ménage commun n'a pas pu être migré. Erreur insoluble dans les fors.");
			rowRs2NonHabitant(rowRsCouple, pp);
			String message = "Le ménage commun n'a pas pu être migré. Erreur insoluble dans les fors. Personnes Physiques : "+noCtbCouple;
			Audit.info(message);
			return pp;
		}

		MenageCommun couple = new MenageCommun();
		if (tiersCtb == null || tiersCtbConjoint == null) {
			String message;
			if (tiersCtb == null) {
				message = "Le tiers couple " + noCtbCouple
						+ " ne peut pas être créé. Raison : les individus faisant partie du ménage commun n'existent pas :" + ind0 + " "
						+ ind1;
			}
			else {
				long indTrouve = tiersCtb.getNumeroIndividu();
				long indManquant = (ind0.longValue() == indTrouve ? ind1 : ind0);
				message = "Le tiers couple " + noCtbCouple
						+ " ne peut pas être créé. Raison : un seul individu faisant partie du ménage commun a été trouvé :" + indTrouve
						+ " (trouvé) " + indManquant + " (manquant)";
			}
			errorsManager.setInError(noCtbCouple, message, TypeMigRegError.ERROR_APPLICATIVE);
		}
		else {
			try {
				rowRs2Tiers(rowRsCouple, couple);
				final List<Pair<RegDate, RegDate>> periodes = readPeriodesCouple(noCtbCouple);
				for (Pair<RegDate,RegDate> periodeCouple : periodes) {
					if (periodeCouple != null && periodeCouple.getFirst() == null) {
						String message = "Le tiers couple " + noCtbCouple + " ne peut pas être créé. Raison : la date du regroupement est nulle.";
						errorsManager.setInError(noCtbCouple, message, TypeMigRegError.ERROR_APPLICATIVE);
					}
					else {
						LOGGER.debug("Couple complet : " + noCtbCouple);
						try {
							// Création du ménage et de la relation avec le premier tiers
							long begin = System.nanoTime();
							RapportEntreTiers rapport = helper.tiersService.addTiersToCouple(couple, tiersCtb, periodeCouple.getFirst(),
									periodeCouple.getSecond());
							long end = System.nanoTime();
							LOGGER.debug("Ajout du premier membre du couple. Temps de contrôle : "+formatDiffTime(end,begin)+"[millisec]");
							couple = (MenageCommun) rapport.getObjet();
							// Création de la relation avec le second tiers
							begin = System.nanoTime();
							rapport = helper.tiersService.addTiersToCouple(couple, tiersCtbConjoint, periodeCouple.getFirst(), periodeCouple.getSecond());
							end = System.nanoTime();
							LOGGER.debug("Ajout du second membre du couple. Temps de contrôle : "+formatDiffTime(end, begin)+"[millisec]");
						}
						catch (Exception e) {
							long endCouple = System.nanoTime();
							LOGGER.debug("Fin de création du couple erreur. Temps : "+formatDiffTime(endCouple, beginCouple)+"[millisec]");
							String message = "Le tiers couple " + noCtbCouple + " ne peut pas être créé. Raison : " + e.getMessage();
							errorsManager.setInError(noCtbCouple, message, TypeMigRegError.ERROR_APPLICATIVE);
						}
					}
				}
			}
			catch (Exception e){
				String message = "Le tiers couple " + noCtbCouple + " ne peut pas être créé. Raison : la date du regroupement est nulle.";
				errorsManager.setInError(noCtbCouple, message, TypeMigRegError.ERROR_APPLICATIVE);
			}
		}
		long endCouple = System.nanoTime();
		LOGGER.debug("Fin de création du couple. Temps : "+formatDiffTime(endCouple, beginCouple)+"[millisec]");
		return couple;
	}

	private Contribuable buildCtbSingle(RowRs row) {

		long numeroCtb = row.noContribuable;
		PersonnePhysique h = new PersonnePhysique(true);
		try {
			rowRs2Tiers(row, h);
			h.setNumeroIndividu(row.fkIndno);
		}
		catch (Exception e) {
			LOGGER.error(e, e);
			errorsManager.setInError(numeroCtb, "Migration du contribuable " + row.noContribuable + " failed", TypeMigRegError.ERROR_APPLICATIVE);
		}

		return h;
	}

	/**
	 * Effectue le set des propriétés du tiers en paramètre avec les données du ResultSet envoyé
	 *
	 * @param rs
	 * @param tiers
	 * @throws Exception
	 */
	private void rowRs2Tiers(RowRs row, Tiers tiers) throws Exception {

		long numeroCtb = row.noContribuable;

		tiers.setNumero(numeroCtb);

		//Dans le cas d'un nomHabitant, le prénom a été chargé avec la colonne nom_courrier2 de la table contribuable.
		//Transférer ce nom_courrier2 dans le complément du nom et mettre le prénom à blanc.
		if (tiers instanceof PersonnePhysique) {
			PersonnePhysique pp = (PersonnePhysique) tiers;
			if (!pp.isHabitant()) {
				tiers.setComplementNom(pp.getPrenom());
				pp.setPrenom("");
			}
		}
		tiers.setBlocageRemboursementAutomatique(row.blocageRembAuto);

		if (row.iban != null && !row.iban.trim().equals("")) {
			tiers.setNumeroCompteBancaire(row.iban);
		}
		else if (row.noCompteBancaire != null && !row.noCompteBancaire.trim().equals("")) {
			tiers.setNumeroCompteBancaire(row.noCompteBancaire);
		}
		else if (row.noCcp != null && !row.noCcp.trim().equals("")) {
			tiers.setNumeroCompteBancaire(row.noCcp);
		}
		if (row.titulaireDuCompt != null && !row.titulaireDuCompt.trim().equals("")) {
			tiers.setTitulaireCompteBancaire(row.titulaireDuCompt);
		}
		if (row.bic != null && !row.bic.trim().equals("")) {
			tiers.setAdresseBicSwift(row.bic);
		}

		if (row.noTelFixe != null && !row.noTelFixe.trim().equals("")) {
			tiers.setNumeroTelephonePrive(row.noTelFixe);
		}
		if (row.noTelPortable != null && !row.noTelPortable.trim().equals("")) {
			tiers.setNumeroTelephonePortable(row.noTelPortable);
		}
		if (row.email != null && !row.email.trim().equals("")) {
			tiers.setAdresseCourrierElectronique(row.email);
		}

		//On importe les attributs de logs de contribuable sauf pour les ctb pou tous (visa=BIDON)
		if (!row.visaMutation.equalsIgnoreCase("bidon")) {
			tiers.setLogCreationDate(row.dateMutation);
			tiers.setLogCreationUser(row.visaMutation);
			tiers.setLogModifMillis(row.dateMutation.getTime());
			tiers.setLogModifUser(row.visaMutation);
		}

		AdresseSupplementaire adr = null;
		if (row.fkLocPosno != 0 || row.fkPaysnoOfs == 0 || row.fkPaysnoOfs == ServiceInfrastructureService.noOfsSuisse) {
			AdresseSuisse adrS = new AdresseSuisse();
			adr = adrS;
			if (row.fkRueno != 0) {
				adrS.setNumeroRue(row.fkRueno);
			}
			else {
				adrS.setNumeroOrdrePoste(row.fkLocPosno);
			}
		}
		else {
			AdresseEtrangere adrE = new AdresseEtrangere();
			adr = adrE;
			adrE.setNumeroOfsPays(row.fkPaysnoOfs);
			adrE.setNumeroPostalLocalite(row.lieu.trim());
		}
		adr.setDateDebut(RegDate.get());
		adr.setUsage(TypeAdresseTiers.COURRIER);
		adr.setComplement(row.chez.trim());
		adr.setRue(row.rue.trim());
		adr.setNumeroMaison(row.noPolice.trim());
		adr.setPermanente(false);
		if (!row.coIrole) {
			adr.setPermanente(true);
			adr.setComplement(row.nomCourrier2);
		}
		//Ne pas tenir compte de l'adresse si celle-ci ne peut être validée. Unireg utilisera l'adresse de l'individu.
		ValidationResults results = adr.validate();
		if (adr.isPermanente() || results == null || results.getErrors().size() == 0) {
			//[Migreg-13]
			if(!isVaudois(row.fkLocPosno)||(row.fkPaysnoOfs != 0 && row.fkPaysnoOfs != ServiceInfrastructureService.noOfsSuisse)){
				adr.setComplement(row.nomCourrier2);
			}

			tiers.addAdresseTiers(adr);
		}
	}

	private void rowRs2NonHabitant(RowRs row, PersonnePhysique nonHab) throws Exception {

		String nom = row.nomCourrier1;
		Assert.notNull(nom);
		nonHab.setNom(nom);
		nonHab.setPrenom(row.nomCourrier2);
		//Origine I107. Ajout d'une qualification. A faire lorsque la qualification I107 sera implémentée dans les modèle.
		nonHab.setDebiteurInactif(row.origineI107);
		rowRs2Tiers(row, nonHab);
	}

	private String readCtbPourTous(int ctbStart, int ctbEnd) throws Exception {
		String query =
		"SELECT " +
		"A.NO_CONTRIBUABLE, " +
		"A.NOM_COURRIER_1, " +
		"A.NOM_COURRIER_2, " +
		"A.CHEZ, " +
		"A.RUE, " +
		"A.NO_POLICE, " +
		"A.LIEU, " +
		"A.BIC, " +
		"A.NO_TEL_FIXE, " +
		"A.NO_TEL_PORTABLE, " +
		"A.EMAIL, " +
		"A.IMPOT_DEPENSE_VD, " +
		"A.INDIGENT, " +
		"A.IBAN, " +
		"A.TITULAIRE_DU_COMPT, " +
		"A.NO_COMPTE_BANCAIRE, " +
		"A.BLOCAGE_REMB_AUTO, " +
		"A.ORIGINE_I107, " +
		"A.NO_CCP, " +
		"A.CO_IROLE, " +
		"A.FK_LOC_POSNO, " +
		"A.FK_RUENO, " +
		"A.FK_PAYSNO_OFS, " +
		"A.DA_MUT, " +
		"A.VS_MUT, " +
		"B.FK_INDNO " +
		"FROM " + helper.getTableDb2("CONTRIBUABLE") + " A," +
		helper.getTableDb2("REGROUPEMENT_IND") + " B," +
		helper.getTableDb2("ASSUJETTIS_PP") + " C " +
		"WHERE " + "C.FK_TYASSUJNO = 3 " +
		"AND C.FK_CONTRIBUABLENO = A.NO_CONTRIBUABLE " +
		"AND B.FK_CONTRIBUABLENO = A.NO_CONTRIBUABLE";
		if (ctbStart > 0) {
			query += " AND A.NO_CONTRIBUABLE >= " + ctbStart + " AND A.NO_CONTRIBUABLE <=" + ctbEnd;
		}
		query += " ORDER BY A.NO_CONTRIBUABLE";

		HostMigratorHelper.SQL_LOG.debug("Query: " + query);
		return query;
	}

	private boolean existsCtbInRange(int ctbStart, int ctbEnd) throws Exception {

		Assert.isTrue(ctbStart > 0 && ctbEnd > 0);

		String query = "SELECT COUNT(*) AS NBRCTB FROM " + helper.db2Schema + ".CONTRIBUABLE WHERE ";
		query += " NO_CONTRIBUABLE >= " + ctbStart + " AND NO_CONTRIBUABLE <=" + ctbEnd;

		HostMigratorHelper.SQL_LOG.debug("Query: " + query);
		int nb = helper.db2Template.queryForInt(query);
		return (nb > 0);
	}


	private String readCtbOpenWithoutIndividu(int ctbStart, int ctbEnd) throws Exception {
		String query =
			"SELECT " +
			"A.NO_CONTRIBUABLE, " +
			"A.NOM_COURRIER_1, " +
			"A.NOM_COURRIER_2, " +
			"A.CHEZ, " +
			"A.RUE, " +
			"A.NO_POLICE, " +
			"A.LIEU, " +
			"A.BIC, " +
			"A.NO_TEL_FIXE, " +
			"A.NO_TEL_PORTABLE, " +
			"A.EMAIL, " +
			"A.IMPOT_DEPENSE_VD, " +
			"A.INDIGENT, " +
			"A.IBAN, " +
			"A.TITULAIRE_DU_COMPT, " +
			"A.NO_COMPTE_BANCAIRE, " +
			"A.BLOCAGE_REMB_AUTO, " +
			"A.ORIGINE_I107, " +
			"A.NO_CCP, " +
			"A.CO_IROLE, " +
			"A.FK_LOC_POSNO, " +
			"A.FK_RUENO, " +
			"A.FK_PAYSNO_OFS, " +
			"A.DA_MUT, " +
			"A.VS_MUT " +
			"FROM "+helper.getTableDb2("CONTRIBUABLE")+" A " +
			"WHERE EXISTS " +
				"(" +
				"SELECT NO_SEQUENCE FROM "+helper.getTableDb2("ASSUJETTIS_PP") +" "+
				"WHERE FK_TYASSUJNO <> 3 " +
				"AND DAD_ASSUJETTIS <= CURRENT DATE " +
				"AND (DAF_ASSUJETTIS = '0001-01-01' OR DAF_ASSUJETTIS >= CURRENT DATE) " +
				"AND DAA_ASSUJETTIS = '0001-01-01' " +
				"AND FK_CONTRIBUABLENO = A.NO_CONTRIBUABLE " +
				") " +
			"AND NOT EXISTS " +
				"(" +
				"SELECT B.NUMERO FROM "+helper.getTableDb2("REGROUPEMENT_IND")+" B " +
				"WHERE  B.FK_CONTRIBUABLENO = A.NO_CONTRIBUABLE" +
				")";
		if (ctbStart > 0) {
			query += " AND A.NO_CONTRIBUABLE >= " + ctbStart + " AND A.NO_CONTRIBUABLE <=" + ctbEnd;
			query += " ORDER BY A.NO_CONTRIBUABLE";
		}
		HostMigratorHelper.SQL_LOG.debug("Query: " + query);

		return query;
	}

	private boolean isForPrincipalActifOneCtbVd(long noCtb) throws Exception {
		String query = "SELECT " + "A.NO_SEQUENCE " + "FROM " + helper.db2Schema + ".FOR_PRINCIPAL_CONT A, " + helper.db2Schema
				+ ".COMMUNE B " + "WHERE A.FK_COMMUNENO = B.NO_TECHNIQUE " + "AND B.FK_CANTONSIGLE = 'VD' " + "AND A.FK_CONTRIBUABLENO = "
				+ noCtb + " " + "AND A.DAD_VALIDITE <= CURRENT DATE "
				+ "AND (A.DAF_VALIDITE = '0001-01-01' OR A.DAF_VALIDITE >= CURRENT DATE) " + "AND A.DAA_FOR_PRINC_CTB = '0001-01-01'";
		HostMigratorHelper.SQL_LOG.debug("Query: " + query);

		SqlRowSet rs = helper.db2Template.queryForRowSet(query);
		return rs.first();
	}


	private boolean isVaudois(long noLocalite) throws Exception {
		String query =	"select localite.FK_COMMUNENO from " + helper.db2Schema + ".LOCALITE_POSTALE localite, " +
				 helper.db2Schema + ".COMMUNE commune where localite.FK_COMMUNENO = commune.NO_TECHNIQUE " +
				"AND commune.FK_CANTONSIGLE = 'VD' " +
		        "AND localite.NO_ORDRE_P ="+ noLocalite;
		SqlRowSet rs = helper.db2Template.queryForRowSet(query);
		return rs.first();
	}

	/**
	 * Effectue la lecture des contribuables regroupés avec un individu et ayant un assujettissement valide.
	 *
	 * @param noCtbStart
	 * @param noCtbEnd
	 * @return ResultSet
	 * @throws Exception
	 */
	private String readCtbRoleOrdinaire(long noCtbStart, long noCtbEnd) throws Exception {
		String query =
			"SELECT DISTINCT " +
			"A.NO_CONTRIBUABLE, " +
			"A.NOM_COURRIER_1, " +
			"A.NOM_COURRIER_2, " +
			"A.CHEZ, " +
			"A.RUE, " +
			"A.NO_POLICE, " +
			"A.LIEU, " +
			"A.BIC, " +
			"A.NO_TEL_FIXE, " +
			"A.NO_TEL_PORTABLE, " +
			"A.EMAIL, " +
			"A.IMPOT_DEPENSE_VD, " +
			"A.INDIGENT, " +
			"A.IBAN, " +
			"A.TITULAIRE_DU_COMPT, " +
			"A.NO_COMPTE_BANCAIRE, " +
			"A.BLOCAGE_REMB_AUTO, " +
			"A.ORIGINE_I107, " +
			"A.NO_CCP, " +
			"A.CO_IROLE, " +
			"A.FK_LOC_POSNO, " +
			"A.FK_RUENO, " +
			"A.FK_PAYSNO_OFS, " +
			"A.DA_MUT, " +
			"A.VS_MUT, " +
			"B.FK_INDNO, " +
			"B.DAD_APPARTENANCE, " +
			"B.DAF_APPARTENANCE, " +
			"B.NUMERO " +
			"FROM "+helper.getTableDb2("CONTRIBUABLE")+ " A, " +
			helper.getTableDb2("REGROUPEMENT_IND") + " B, " +
			helper.getTableDb2("ASSUJETTIS_PP") + " C " +
			"WHERE " +
			"A.NO_CONTRIBUABLE = B.FK_CONTRIBUABLENO " +
//			"AND B.DAD_APPARTENANCE <= CURRENT DATE " +
//			"AND (B.DAF_APPARTENANCE >= CURRENT DATE OR B.DAF_APPARTENANCE = '0001-01-01') " +
			"AND C.FK_CONTRIBUABLENO = A.NO_CONTRIBUABLE " + "AND C.FK_TYASSUJNO <> 3 " + "AND C.DAD_ASSUJETTIS <= CURRENT DATE " +
			"AND (C.DAF_ASSUJETTIS = '0001-01-01' OR C.DAF_ASSUJETTIS >= CURRENT DATE) " +
			"AND C.DAA_ASSUJETTIS='0001-01-01'";

		if (noCtbStart > 0) {
			query += " AND A.NO_CONTRIBUABLE >= " + noCtbStart + " AND A.NO_CONTRIBUABLE <=" + noCtbEnd;
			query += " ORDER BY A.NO_CONTRIBUABLE, B.FK_INDNO, B.NUMERO DESC";
		}

		HostMigratorHelper.SQL_LOG.debug("Query: " + query);
		return query;
	}

	private String readCtbAssujCloseWithoutRegroupement(int ctbStart, int ctbEnd) throws Exception {
		String query =
			"SELECT " +
			"A.NO_CONTRIBUABLE, " +
			"A.NOM_COURRIER_1, " +
			"A.NOM_COURRIER_2, " +
			"A.CHEZ, " +
			"A.RUE, " +
			"A.NO_POLICE, " +
			"A.LIEU, " +
			"A.BIC, " +
			"A.NO_TEL_FIXE, " +
			"A.NO_TEL_PORTABLE, " +
			"A.EMAIL, " +
			"A.IMPOT_DEPENSE_VD, " +
			"A.INDIGENT, " +
			"A.IBAN, " +
			"A.TITULAIRE_DU_COMPT, " +
			"A.NO_COMPTE_BANCAIRE, " +
			"A.BLOCAGE_REMB_AUTO, " +
			"A.ORIGINE_I107, " +
			"A.NO_CCP, " +
			"A.CO_IROLE, " +
			"A.FK_LOC_POSNO, " +
			"A.FK_RUENO, " +
			"A.FK_PAYSNO_OFS, " +
			"A.DA_MUT, " +
			"A.VS_MUT " +
			"FROM "+helper.getTableDb2("CONTRIBUABLE")+" A " +
			"WHERE NOT EXISTS " +
				"("	+
				"SELECT Z.NUMERO FROM "+helper.getTableDb2("REGROUPEMENT_IND")+" Z " +
				"WHERE A.NO_CONTRIBUABLE = Z.FK_CONTRIBUABLENO" +
				") " +
			"AND EXISTS " +
				"(" +
				"SELECT D.NO_SEQUENCE FROM "+helper.getTableDb2("ASSUJETTIS_PP")+" D " +
				"WHERE A.NO_CONTRIBUABLE = D.FK_CONTRIBUABLENO " +
				"AND (D.DAD_ASSUJETTIS > CURRENT DATE OR (D.DAF_ASSUJETTIS <> '0001-01-01' AND D.DAF_ASSUJETTIS < CURRENT DATE)) " +
				"AND D.DAA_ASSUJETTIS = '0001-01-01'"
				+ ") " +
			"AND NOT EXISTS " +
				"(" +
				"SELECT E.NO_SEQUENCE FROM "+helper.getTableDb2("ASSUJETTIS_PP") + " E " +
				"WHERE A.NO_CONTRIBUABLE = E.FK_CONTRIBUABLENO " +
				"AND (E.DAD_ASSUJETTIS <= CURRENT DATE AND (E.DAF_ASSUJETTIS = '0001-01-01' OR E.DAF_ASSUJETTIS > CURRENT DATE)) " +
				"AND E.DAA_ASSUJETTIS = '0001-01-01'" +
				")";

		if (ctbStart > 0) {
			query += " AND A.NO_CONTRIBUABLE >= " + ctbStart + " AND A.NO_CONTRIBUABLE <=" + ctbEnd;
			query += " ORDER BY A.NO_CONTRIBUABLE";
		}

		HostMigratorHelper.SQL_LOG.debug("Query: " + query);
		return query;
	}

//	private String _readCtbAssujCloseRegroupementClose(int ctbStart, int ctbEnd) throws Exception {
//		String query =
//			"SELECT DISTINCT " +
//			"A.NO_CONTRIBUABLE, " +
//			"A.NOM_COURRIER_1, " +
//			"A.NOM_COURRIER_2, " +
//			"A.CHEZ, " +
//			"A.RUE, " +
//			"A.NO_POLICE, " +
//			"A.LIEU, " +
//			"A.BIC, " +
//			"A.NO_TEL_FIXE, " +
//			"A.NO_TEL_PORTABLE, " +
//			"A.EMAIL, " +
//			"A.IMPOT_DEPENSE_VD, " +
//			"A.INDIGENT, " +
//			"A.IBAN, " +
//			"A.TITULAIRE_DU_COMPT, " +
//			"A.NO_COMPTE_BANCAIRE, " +
//			"A.BLOCAGE_REMB_AUTO, " +
//			"A.ORIGINE_I107, " +
//			"A.NO_CCP, " +
//			"A.CO_IROLE, " +
//			"A.FK_LOC_POSNO, " +
//			"A.FK_RUENO, " +
//			"A.FK_PAYSNO_OFS, " +
//			"A.DA_MUT, " +
//			"A.VS_MUT, " +
//			"B.FK_INDNO, " +
//			"B.DAD_APPARTENANCE, " +
//			"B.DAF_APPARTENANCE, " +
//			"B.NUMERO " +
//			"FROM "+helper.getTableDb2("CONTRIBUABLE")+ " A, " +
//			helper.getTableDb2("REGROUPEMENT_IND")+ " B, " +
//			helper.getTableDb2("ASSUJETTIS_PP")+ " D " +
//			"WHERE A.NO_CONTRIBUABLE = B.FK_CONTRIBUABLENO " +
//			"AND A.NO_CONTRIBUABLE = D.FK_CONTRIBUABLENO " +
////			"AND NOT EXISTS " +
////		    "( " +
////		    "SELECT C.NUMERO " +
////		    "FROM "+helper.getTableDb2("REGROUPEMENT_IND")+ " C " +
////		    "WHERE A.NO_CONTRIBUABLE = C.FK_CONTRIBUABLENO " +
////		    "AND C.DAD_APPARTENANCE <= CURRENT DATE " +
////		    "AND (C.DAF_APPARTENANCE = '0001-01-01' OR C.DAF_APPARTENANCE > CURRENT DATE) " +
////		    ") " +
//		    "AND NOT EXISTS " +
//		    "( " +
//		    "SELECT 1 " +
//		    "FROM "+helper.getTableDb2("ASSUJETTIS_PP")+ " E " +
//		    "WHERE A.NO_CONTRIBUABLE = E.FK_CONTRIBUABLENO " +
//		    "AND E.DAD_ASSUJETTIS <= CURRENT DATE " +
//		    "AND (E.DAF_ASSUJETTIS = '0001-01-01' OR E.DAF_ASSUJETTIS > CURRENT DATE) " +
//		    "AND E.DAA_ASSUJETTIS = '0001-01-01'" +
//		    ") ";
//
//		if (ctbStart > 0) {
//			query += " AND A.NO_CONTRIBUABLE >= " + ctbStart + " AND A.NO_CONTRIBUABLE <=" + ctbEnd;
//			query += " ORDER BY A.NO_CONTRIBUABLE, B.FK_INDNO, B.NUMERO DESC";
//		}
//		HostMigratorHelper.SQL_LOG.debug("Query: " + query);
//
//		return query;
//	}

	private String readCtbAssujCloseRegroupementClose(int ctbStart, int ctbEnd) throws Exception {
		String query =
			"SELECT DISTINCT " +
			"A.NO_CONTRIBUABLE, " +
			"A.NOM_COURRIER_1, " +
			"A.NOM_COURRIER_2, " +
			"A.CHEZ, " +
			"A.RUE, " +
			"A.NO_POLICE, " +
			"A.LIEU, " +
			"A.BIC, " +
			"A.NO_TEL_FIXE, " +
			"A.NO_TEL_PORTABLE, " +
			"A.EMAIL, " +
			"A.IMPOT_DEPENSE_VD, " +
			"A.INDIGENT, " +
			"A.IBAN, " +
			"A.TITULAIRE_DU_COMPT, " +
			"A.NO_COMPTE_BANCAIRE, " +
			"A.BLOCAGE_REMB_AUTO, " +
			"A.ORIGINE_I107, " +
			"A.NO_CCP, " +
			"A.CO_IROLE, " +
			"A.FK_LOC_POSNO, " +
			"A.FK_RUENO, " +
			"A.FK_PAYSNO_OFS, " +
			"A.DA_MUT, " +
			"A.VS_MUT, " +
			"B.FK_INDNO, " +
			"B.DAD_APPARTENANCE, " +
			"B.DAF_APPARTENANCE, " +
			"B.NUMERO " +
			"FROM "+helper.getTableDb2("CONTRIBUABLE")+ " A, " +
			helper.getTableDb2("REGROUPEMENT_IND")+ " B, " +
			helper.getTableDb2("ASSUJETTIS_PP")+ " D " +
			"WHERE A.NO_CONTRIBUABLE = B.FK_CONTRIBUABLENO " +
			"AND A.NO_CONTRIBUABLE = D.FK_CONTRIBUABLENO " +
//			"AND NOT EXISTS " +
//		    "( " +
//		    "SELECT 1 " +
//		    "FROM "+helper.getTableDb2("REGROUPEMENT_IND")+ " C " +
//		    "WHERE A.NO_CONTRIBUABLE = C.FK_CONTRIBUABLENO " +
//		    "AND C.DAD_APPARTENANCE <= CURRENT DATE " +
//		    "AND (C.DAF_APPARTENANCE = '0001-01-01' OR C.DAF_APPARTENANCE > CURRENT DATE) " +
//		    ") " +
			"AND NOT EXISTS " +
		    "( " +
		    "SELECT 1 " +
		    "FROM "+helper.getTableDb2("ASSUJETTIS_PP")+ " F " +
		    "WHERE A.NO_CONTRIBUABLE = F.FK_CONTRIBUABLENO " +
			"and f.dad_assujettis = '01.01.1800' " +
			"and f.daf_assujettis = '01.01.1800' " +
			"and f.FK_TYASSUJNO = 3" +
			") " +
			"AND NOT EXISTS " +
		    "( " +
		    "SELECT 1 " +
		    "FROM "+helper.getTableDb2("ASSUJETTIS_PP")+ " E " +
		    "WHERE A.NO_CONTRIBUABLE = E.FK_CONTRIBUABLENO " +
		    "AND E.DAD_ASSUJETTIS <= CURRENT DATE " +
		    "AND (E.DAF_ASSUJETTIS = '0001-01-01' OR E.DAF_ASSUJETTIS > CURRENT DATE) " +
		    "AND E.DAA_ASSUJETTIS = '0001-01-01'" +
		    ") ";

		if (ctbStart > 0) {
			query += " AND A.NO_CONTRIBUABLE >= " + ctbStart + " AND A.NO_CONTRIBUABLE <=" + ctbEnd;
			query += " ORDER BY A.NO_CONTRIBUABLE, B.FK_INDNO, B.NUMERO DESC";
		}
		HostMigratorHelper.SQL_LOG.debug("Query: " + query);

		return query;
	}

	private List<Pair<RegDate,RegDate>> readPeriodesCouple(Long noContribuable) throws Exception {
		String query =
			"SELECT A.DAD_VALIDITE " +
			", A.MOTIF_SORTIE" +
			", A.DAF_VALIDITE " +
			"FROM "+helper.getTableDb2("FOR_PRINCIPAL_CONT")+ " A " +
			"WHERE A.FK_CONTRIBUABLENO = "+noContribuable+" "+
			"AND A.DAA_FOR_PRINC_CTB = '0001-01-01' " +
			"AND A.MOTIF_SORTIE <> 1 " +
			"ORDER BY DAD_VALIDITE";

		HostMigratorHelper.SQL_LOG.debug("Query: " + query);


		SqlRowSet rs = helper.db2Template.queryForRowSet(query);
		List<Pair<RegDate,RegDate>> periodesCouple = new ArrayList<Pair<RegDate,RegDate>>();
		boolean isSepare = false;
		Pair<RegDate,RegDate> periodeCouple = new Pair<RegDate,RegDate>();
		while (rs.next()) {
			if (rs.isFirst() && rs.isLast()) {
				periodeCouple.setFirst(RegDate.get(rs.getDate("DAD_VALIDITE")));
				periodeCouple.setSecond(RegDate.get(rs.getDate("DAF_VALIDITE")));
				periodesCouple.add(periodeCouple);
				return periodesCouple;
			}

			if (rs.isFirst() || isSepare) {
				periodeCouple.setFirst(RegDate.get(rs.getDate("DAD_VALIDITE")));
				isSepare = false;
			}
			periodeCouple.setSecond(RegDate.get(rs.getDate("DAF_VALIDITE")));
			if (rs.getInt("MOTIF_SORTIE") == 14) {
				periodesCouple.add(periodeCouple);
				periodeCouple = new Pair<RegDate,RegDate>();
				isSepare = true;
			}
		}
		if (periodeCouple.getFirst() != null) {
			periodesCouple.add(periodeCouple);
		}
		return periodesCouple;
	}

	@Override
	protected void doBeforeCommit() throws Exception {

		super.doBeforeCommit();

		errors = new ArrayList<MigrationError>();

		setRunningMessage("Chargement des Fors fiscaux...");
		errors = forsFiscauxLoader.loadForsFiscaux(getTiersList(), errors);

		if (errors.size() == 0) {
			setRunningMessage("Chargement des Declarations d'impot ordinaire...");
			errors = diLoader.loadDIListCtb(getTiersList(), errors);
		}

		if (errors.size() == 0) {
			setRunningMessage("Chargement des adresses...");
			errors = adressesLoader.loadAdressesListCtb(getTiersList(), errors);
		}

		if (errors.size() == 0) {
			setRunningMessage("Chargement des mouvements de dossier...");
			errors = mouvementDossierLoader.loadMvtDossier(getTiersList(), errors);
		}

		for (Tiers tiers : getTiersList()) {
			ValidationResults results = tiers.validate();
			for (String error : results.getErrors()) {
				MigrationError migRegError = new MigrationError();
				migRegError.setNoContribuable(tiers.getNumero());
				migRegError.setMessage(error);
				errors.add(migRegError);
			}
		}
	}

	protected String formatDiffTime(long end, long begin) {
		long diff = end-begin;
		float value = diff / 1000000.0f;

		String str = String.format("%.2f", value);
		return str;
	}
}
