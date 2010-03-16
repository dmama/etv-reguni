package ch.vd.uniregctb.migreg;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseEtrangere;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.Rue;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class HostEmployeurMigrator extends HostMigrator {

	private static final Logger LOGGER = Logger.getLogger(HostEmployeurMigrator.class);
	private static final Logger RAPPORT = Logger.getLogger(HostEmployeurMigrator.class.getName()+".Rapport");
	private static final Logger RAPPORTLR = Logger.getLogger(HostEmployeurMigrator.class.getName()+".RapportLR");
	private static final Logger REJET = Logger.getLogger(HostEmployeurMigrator.class.getName()+".Rejet");
	private static final Logger ESSAI = Logger.getLogger(HostEmployeurMigrator.class.getName()+".Essai");

	private int empStart = -1;
	private int empEnd = -1;
	private RegDate dateAdresseSiege = null;


	private static final int INDEX_FAC_DAD_PER_EFFECTIVE = 0;
	private static final int INDEX_FAC_DA_EXPEDITION = 3;
	private static final int INDEX_FAC_DA_EXIGIBILITE = 4;
	private static final int INDEX_FAC_DA_RECEP_LIST_NOMI = 6;
	private static final int INDEX_FAC_DA_RAPPEL_LST_NOM = 8;
	private static final int INDEX_FAC_DA_2EME_RAPPEL_LN = 9;
	private static final int NO_OFS_LAUSANNE = 5586;
//	private static final String NEW_LINE = System.getProperty("line.separator");


	public enum CodeEtat {
		INCONNU, ANNULE
	}

	public HostEmployeurMigrator(HostMigratorHelper deps, MigRegLimits limits, MigregStatusManager mgr) {
		super(deps, limits, mgr);

		if (limits.empFirst != null) {
			empStart = limits.empFirst;

			if (limits.empEnd != null) {
				empEnd = limits.empEnd;
			}
		}
	}

	public class DebiteurLu{
		private Integer numero;
		private String design1;
		private String design2;
		private String design3;
		private String design4;
		private String correspondant;
		private String noTelephone;
		private String coPeriodicite ;
		private String coRappel;
		private Integer noEntreprise;
		private Integer catEmp;
		private String designAbregee;

		@Override
		public String toString() {
			return "Débiteur host n°" + numero;
		}
	}



	@Override
	public int migrate() throws Exception {

		int ret = 0;
		try {
			ret = iterateOnEmployeurs();
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}

		return ret;
	}

	private int iterateOnEmployeurs() throws Exception {


		int nbDpiMigrated = 0;
		//int nbAcMigrated = 0;

		String sqlEmployeurHackVaud = "SELECT NO_EMPLOYEUR,                                               "
			+ "       DESIGNATION_1,                                              "
			+ "       DESIGNATION_2,                                              "
			+ "       DESIGNATION_3,                                              "
			+ "       DESIGNATION_4,                                              "
			+ "       CORRESPONDANT,                                              "
			+ "       NO_TELEPHONE,                                               "
			+ "       CO_PERIODICITE,                                             "
			+ "       CO_RAPPEL,                                                  "
			+ "       FK_ENTPRNO,                                                 "
			+ "       FK_CAT_NOEMPL,                                              "
			+ "       DESIGN_ABREGEE                                              "
			+ " FROM                                                              "
			+ helper.getTableIs("employeur")
			+ " WHERE                                                             "
			+ "                         ";



		String sqlEmployeurHostActif = "SELECT NO_EMPLOYEUR,                                               "
			+ "       DESIGNATION_1,                                              "
			+ "       DESIGNATION_2,                                              "
			+ "       DESIGNATION_3,                                              "
			+ "       DESIGNATION_4,                                              "
			+ "       CORRESPONDANT,                                              "
			+ "       NO_TELEPHONE,                                               "
			+ "       CO_PERIODICITE,                                             "
			+ "       CO_RAPPEL,                                                  "
			+ "       FK_ENTPRNO,                                                 "
			+ "       FK_CAT_NOEMPL,                                              "
			+ "       DESIGN_ABREGEE                                              "
			+ " FROM                                                              "
			+ helper.getTableIs("employeur")
			+ " WHERE                                                             "
			+ "       (                                                           "
			+ "         ACTIF='O'                                                 "
			+ "      OR                                                           "
			+ "         DESIGN_ABREGEE LIKE '%@%'                                 "
			+ "    )                                                              ";

		String sqlEmployeurHostNonActif =  "SELECT DISTINCT emp.NO_EMPLOYEUR,                                               "
			+ "       emp.DESIGNATION_1,                                                                "
			+ "       emp.DESIGNATION_2,                                                                "
			+ "       emp.DESIGNATION_3,                                                                "
			+ "       emp.DESIGNATION_4,                                                                "
			+ "       emp.CORRESPONDANT,                                                                "
			+ "       emp.NO_TELEPHONE,                                                                 "
			+ "       emp.CO_PERIODICITE,                                                               "
			+ "       emp.CO_RAPPEL,                                                                    "
			+ "       emp.FK_ENTPRNO,                                                                   "
			+ "       emp.FK_CAT_NOEMPL,                                                                "
			+ "       emp.DESIGN_ABREGEE                                                                "
			+ " FROM                                                                                    "
			+ helper.getTableIs("employeur")+" emp,"+ helper.getTableIs("facture_employeur")+" fact     "
			+ " WHERE                                                                                   "
			+ "        (                                                                                "
			+ "           emp.ACTIF='N'                                                                 "
			+ "        AND                                                                              "
			+ "            emp. NO_EMPLOYEUR = fact.FK_CAE_EMPNO                                        "
			+ "        AND                                                                              "
			+ "           fact.FK_TFINOTECH = 1                                                         "
			+ "        AND                                                                              "
			+ "           fact.CO_ETAT = 'B'                                                            "
			+ "        AND                                                                              "
			+ "           YEAR(fact.DAD_PER_EFFECTIVE)= 2009                                            "
			+ "       )                                                                                 ";
		if (empStart > 0) {
			sqlEmployeurHostActif += " AND " + " NO_EMPLOYEUR >= " + empStart;
			sqlEmployeurHostNonActif += " AND " + " NO_EMPLOYEUR >= " + empStart;
			sqlEmployeurHackVaud +=  " NO_EMPLOYEUR >= " + empStart;
		}
		if (empEnd > 0) {
			sqlEmployeurHostActif += " AND NO_EMPLOYEUR <= " + empEnd;
			sqlEmployeurHostNonActif += " AND NO_EMPLOYEUR <= " + empEnd;
			sqlEmployeurHackVaud += " AND NO_EMPLOYEUR <= " + empEnd;
		}
		sqlEmployeurHostActif += " ORDER BY NO_EMPLOYEUR ASC";
		sqlEmployeurHostNonActif += " ORDER BY NO_EMPLOYEUR ASC";
		LOGGER.debug(sqlEmployeurHostActif);
		LOGGER.debug(sqlEmployeurHostNonActif);
		LOGGER.debug(sqlEmployeurHackVaud);

		//nbDpiMigrated = migrateDPI(sqlEmployeurHackVaud, true);
		nbDpiMigrated = migrateDPI(sqlEmployeurHostActif, true);
		nbDpiMigrated = nbDpiMigrated + migrateDPI(sqlEmployeurHostNonActif, false);

		return nbDpiMigrated;
	}

	@SuppressWarnings("unchecked")
	private int migrateDPI(final String sqlSelection, final boolean actif) throws Exception {
		//String infosDpi = null;

		//int nbrHorsCanton = 0;
		//ResultSet rs = null;
		// Récupère la liste des sourciers à traiter

		TransactionTemplate template = new TransactionTemplate(helper.transactionManager);

		final List<DebiteurLu> debiteurs = (List<DebiteurLu>) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				try {
					return buildListeDebiteurLu(sqlSelection);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});

		// Exécute la migration par batchs, avec reprise automatique en cas d'erreur

		final BatchTransactionTemplate batchTemplate = new BatchTransactionTemplate<DebiteurLu>(debiteurs, 100,
				BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE, helper.transactionManager, null, helper.hibernateTemplate);
		batchTemplate.execute(new BatchTransactionTemplate.BatchCallback<DebiteurLu>() {

			private Integer noDebiteurUnique;
			private List<DebiteurLu> b;

			@Override
			public void beforeTransaction() {
				noDebiteurUnique = null;
				b = null;
			}

			@Override
			public boolean doInTransaction(List<DebiteurLu> batch) throws Exception {
				b = batch;
				if (batch.size() == 1) {
					// on est en reprise
					noDebiteurUnique = batch.get(0).numero;
				}
				//Permet de differencier les debiteurs qui sont déjà en base avec ceux que l'on tente de migrer
				//Utile pour connaitre le nombre exacte de débiteur que l'on migre a chaque nouveau run
				List<DebiteurLu> debiteurAtraiter = new ArrayList<DebiteurLu>();

				for (DebiteurLu debiteur : batch) {
					Long numeroDebiteur = (long) (debiteur.numero + DebiteurPrestationImposable.FIRST_MIGRATION_ID);
					if (helper.tiersDAO.get(numeroDebiteur) == null) {
						debiteurAtraiter.add(debiteur);
					}
				}

				int nbDpi = 0;

				for (DebiteurLu debiteur : debiteurAtraiter) {
					DebiteurPrestationImposable dpi = buildAndSaveDPI(debiteur, actif);
					if (dpi != null) {
						helper.migrationErrorDAO.removeForContribuable(debiteur.numero.longValue());
						String infosDpi = getInfosDpi(dpi, debiteur.designAbregee);
						RAPPORT.info(infosDpi);
						nbDpi++;
					}
				}

				 //assertNoEmptyForeignKeys( batch) ;

				int nb = status.addGlobalObjectsMigrated(nbDpi);
				nbCtbMigrated.valeur = nbCtbMigrated.valeur+ nbDpi;

				if (nb % 100 == 0) {
					LOGGER.info("Nombre de DPI sauvés: " + nb);
				}

				setRunningMessage("Total des DPI migrés: " + status.getGlobalNbObjectsMigrated());
				return !status.interrupted();
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				String message = "===> Rollback du batch [" + b.get(0).numero + "-" + b.get(b.size() - 1).numero + "] willRetry="
						+ willRetry;
				LOGGER.warn(message);
				if (!willRetry) {
					Assert.notNull(noDebiteurUnique);
					LOGGER.error("Impossible de migrer le debiteur n°" + noDebiteurUnique, e);
					REJET.info( noDebiteurUnique +";"+ e.getMessage()+";BLOQUANTE");
					MigrationError er = helper.migrationErrorDAO.getErrorForContribuable(noDebiteurUnique);
					if (er == null) {
						er = new MigrationError();
						er.setNoContribuable(noDebiteurUnique.longValue());
					}
					er.setMessage(e.getMessage());
					helper.migrationErrorDAO.saveObject(er);
				}
			}

			@Override
			public void afterTransactionCommit() {
			}
		});






		return nbCtbMigrated.valeur;
	}

    // Code de debug
	private void assertNoEmptyForeignKeys(List<DebiteurLu> batch) {
		final List<?> list = helper.hibernateTemplate.find("from ForFiscal ff where ff.tiers is null");
		if (!list.isEmpty()) {
			final String message = "Les fors principaux suivants ont été créés avec des foreign keys nulles : "
					+ ArrayUtils.toString(list.toArray());
			LOGGER.error(message);
			if (batch != null) {
				LOGGER.error("Les débiteurs concernés sont : " + ArrayUtils.toString(batch.toArray()));
			}
			throw new IllegalArgumentException();
		}
	}

	@SuppressWarnings("unchecked")
	private String getInfosDpi(DebiteurPrestationImposable dpi,String designAbregee) {
		String res = "";
		Long ancienNumero = dpi.getNumero() - DebiteurPrestationImposable.FIRST_MIGRATION_ID;
		res = ancienNumero.toString()+";";
		res = res + dpi.getNumero().toString()+";";
		res = res + dpi.getNom1()+";"+dpi.getNom2()+";"+dpi.getComplementNom()+";";
		res = res + designAbregee + ";";
		res = res + dpi.getCategorieImpotSource().name() + ";";
		res = res + dpi.getPeriodiciteDecompte().name() + ";";
		res = res + dpi.getModeCommunication().name() + ";";

		if (dpi.getSansListeRecapitulative()) {
			res = res + "O" + ";";
		}
		else{
			res = res + "N" + ";";
		}

		if (dpi.getSansRappel()) {
			res = res + "O" + ";";
		}
		else{
			res = res + "N" + ";";
		}

		//res = res + dpi.get
		if (dpi.getDeclarations() != null) {
			res = res + dpi.getDeclarationsSorted().size() + ";";
			List lr = Arrays.asList(dpi.getDeclarations().toArray());

			for (Iterator iterator = lr.iterator(); iterator.hasNext();) {
				String resLR=null;
				DeclarationImpotSource declaration = (DeclarationImpotSource) iterator.next();
				resLR =  dpi.getNumero() + ";";
				resLR = resLR + dpi.getNom1() + ";";
				resLR = resLR + declaration.getDateDebut().toString() + ";";
				resLR = resLR + declaration.getDateFin().toString() + ";";
				resLR = resLR + declaration.getPeriodicite() + ";";
				resLR = resLR + declaration.getDateExpedition().toString() + ";";
				resLR = resLR + declaration.getDelaiAccordeAu().toString();
				RAPPORTLR.info(resLR);
			}
		}
		else {
			res = res +"0";
		}

		return res;
	}

	private RegDate determinateDateFinForDebiteur(int numeroEmployeur)throws Exception{
		String  sql = "SELECT                               "+
		" fact.DAF_PER_EFFECTIVE                          "+
		" FROM                                            "+
		helper.getTableIs("facture_employeur")+" fact     "+
		"WHERE                                            "+
		"   fact.FK_TFINOTECH = 1                         "+
		"AND                                              "+
		"   fact.CO_ETAT = 'B'                            "+
		"AND                                              "+
		"   YEAR(fact.DAD_PER_EFFECTIVE)= 2009            "+
        "AND                                              "+
        "   fact.FK_CAE_EMPNO =                           "+
        numeroEmployeur+
        "  ORDER BY fact.DAF_PER_EFFECTIVE DESC           "+
        "  FETCH FIRST ROW ONLY                           ";

		RegDate dateFin= null;
		 Statement stat = helper.db2Connection.createStatement();
		ResultSet rsEmp = stat.executeQuery(sql);
		try {
			while (rsEmp.next()) {
				 dateFin = normalizeDb2Date(rsEmp.getDate(1));
			}

		}
		finally {
			rsEmp.close();
			stat.close();
		}

		return dateFin ;
	}
	private Commune getCommuneAutoriteFiscale(DebiteurPrestationImposable dpi) throws Exception {
		Commune commune = null;
		Localite localite = null;
		AdresseTiers derniereAdresse = dpi.getAdresseActive(TypeAdresseTiers.DOMICILE, null);

		if(derniereAdresse!=null && derniereAdresse instanceof AdresseSuisse){
			AdresseSuisse adresseSuisse = (AdresseSuisse)derniereAdresse;
			//recherche par la rue
			if(adresseSuisse.getNumeroRue()!=null && adresseSuisse.getNumeroRue()!=0 ){
				Rue rue = helper.serviceInfrastructureService.getRueByNumero(adresseSuisse.getNumeroRue());
				localite =helper.serviceInfrastructureService.getLocaliteByONRP( rue.getNoLocalite());

			}
			else if(adresseSuisse.getNumeroOrdrePoste()!=null && adresseSuisse.getNumeroOrdrePoste()!=0){
				localite = helper.serviceInfrastructureService.getLocaliteByONRP( adresseSuisse.getNumeroOrdrePoste());
			}

			if (localite != null) {
				 commune = helper.serviceInfrastructureService.getCommuneByLocalite(localite);

			}
		}
		else if(derniereAdresse instanceof AdresseEtrangere){
			//Dans le cas d'une adresse à l'etranger la commune du for est lausanne
			commune =helper.serviceInfrastructureService.getCommuneByNumeroOfsEtendu(NO_OFS_LAUSANNE);
		}

		return commune;
	}

	private SqlRowSet loadAdresses(int numero,String type){
		String sql = "SELECT "+
		" NO_ADR_POSTALE,"+
		" DAD_VALIDITE,"+
		" DAF_VALIDITE,"+
		" TY_ADRESSE,"+
		" CHEZ,"+
		" NO_POLICE,"+
		" FK_RUENO,"+
		" RUE,"+
		" LIEU,"+
		" FK_LOCNO,"+
		" VILLE_ETRANGER,"+
		" FK_PAYSNO_OFS,"+
		" FK_EMPNO"+
	" from "+helper.isSchema+".ADR_POSTALE where FK_EMPNO = "+numero+
	" AND TY_ADRESSE = '"+type+"'"+
    " AND (DAF_VALIDITE = DATE('01.01.0001') OR DAF_VALIDITE IS NULL)"+
    " AND (DAA_ADR_POSTALE = DATE('01.01.0001') OR DAA_ADR_POSTALE IS NULL)";

		LOGGER.debug(sql);

		SqlRowSet rows = helper.isTemplate.queryForRowSet(sql);
		return rows;

	}

	private void migrateAdresses(int numero, DebiteurPrestationImposable dpi) {

		LOGGER.debug("Migration des adresses du DPI "+numero);



		// FIXME(JEC) : A changer quand l'adresse service supportera le DOMICILE
		AdresseSuisse lastAdresseDomicile = null;
		//AdresseSuisse lastAdresseCOurrier = null;
		boolean siegeExist = false;
		boolean courrierExist = false;
		SqlRowSet rows = loadAdresses( numero,"SE");


			while (rows.next()) {
			siegeExist = true;

			// Integer noTechnique = rows.getInt(1);
			dateAdresseSiege = normalizeDb2Date(rows.getDate(2));
			// if (dateDebut != null) {
			// String dd = dateDebut.toString();
			// dd = null;
			// }
			//RegDate dateFin = normalizeDb2Date(rows.getDate(3));
			// if (dateFin != null) {
			// String df = dateFin.toString();
			// df = null;
			// }
			String type = rows.getString(4);
			type = trimAndNullify(type);
			String chez = rows.getString(5);
			chez = trimAndNullify(chez);
			String noPolice = rows.getString(6);
			noPolice = trimAndNullify(noPolice);
			Integer rueNo = rows.getInt(7);
			String rue = rows.getString(8);
			rue = trimAndNullify(rue);
			String lieu = rows.getString(9);
			lieu = trimAndNullify(lieu);
			Integer locNo = rows.getInt(10);
			String villeEtr = rows.getString(11);
			villeEtr = trimAndNullify(villeEtr);
			Integer paysOfs = rows.getInt(12);
			// Integer noEmp = rows.getInt(13);
			if ((rueNo == null || rueNo == 0) && (locNo == null ||locNo == 0)) {
				String message = "le numero de rue et le numero de localité de l'adresse siege ne sont pas renseignés";
				throw new ValidationException(dpi,message);


			}
			else {
				// Pays hors Suisse
				if (paysOfs != null && !paysOfs.equals(ServiceInfrastructureService.noOfsSuisse) && !paysOfs.equals(0)) {
					String message = "l'adresse siege n'est pas en Suisse";
					throw new ValidationException(dpi,message);
				}
				else {
					// Suisse
					AdresseSuisse adr = new AdresseSuisse();
					// Bornes

					// TYPE
					if (dateAdresseSiege !=null) {
						adr.setDateDebut(dateAdresseSiege);
					}
					else{
						adr.setDateDebut(RegDate.get(2009, RegDate.JANVIER, 1));
					}

					adr.setDateFin(null);
					adr.setUsage(TypeAdresseTiers.DOMICILE);
					adr.setComplement(chez);
					adr.setRue(rue);
					Localite localite= null;
					try {
						localite = helper.serviceInfrastructureService.getLocaliteByONRP(locNo);

					}
					catch (InfrastructureException e) {
						Audit.error("Erreur lors de la recherche de la localité "+ locNo +" "+ e.getMessage());
						REJET.info("Erreur lors de la recherche de la localité "+ locNo +" "+ e.getMessage());
						ESSAI.info("Erreur lors de la recherche de la localité "+ locNo +"BLOQUANTE");
						String message = "Erreur lors de la recherche de la localité";
						throw new ValidationException(dpi,message);


					}

					if(locNo!=null && localite!=null){
						adr.setNumeroOrdrePoste(locNo);
					}
					adr.setNumeroMaison(noPolice);
					lastAdresseDomicile = adr;
					adr.setPermanente(false);

					dpi.addAdresseTiers(adr);
				}

			}
		}


		if (!siegeExist) {
			String message = "Adress siege inexistante";
			throw new ValidationException(dpi,message);

		}



		 rows = loadAdresses( numero,"CE");


		while (rows.next()) {
			courrierExist = true;

		// Integer noTechnique = rows.getInt(1);
		//RegDate dateDebut = normalizeDb2Date(rows.getDate(2));
		// if (dateDebut != null) {
		// String dd = dateDebut.toString();
		// dd = null;
		// }
		//RegDate dateFin = normalizeDb2Date(rows.getDate(3));
		// if (dateFin != null) {
		// String df = dateFin.toString();
		// df = null;
		// }
		String type = rows.getString(4);
		type = trimAndNullify(type);
		String chez = rows.getString(5);
		chez = trimAndNullify(chez);
		String noPolice = rows.getString(6);
		noPolice = trimAndNullify(noPolice);
		Integer rueNo = rows.getInt(7);
		String rue = rows.getString(8);
		rue = trimAndNullify(rue);
		String lieu = rows.getString(9);
		lieu = trimAndNullify(lieu);
		Integer locNo = rows.getInt(10);
		String villeEtr = rows.getString(11);
		villeEtr = trimAndNullify(villeEtr);
		Integer paysOfs = rows.getInt(12);
		// Integer noEmp = rows.getInt(13);
		if ((rueNo == null || rueNo == 0) && (locNo == null ||locNo == 0)) {

			String message = "le numero de rue et le numero de localité de l'adresse courrier ne sont pas renseignés";
			throw new ValidationException(dpi,message);

		}
		else {
			// Pays hors Suisse
			if (paysOfs != null && !paysOfs.equals(ServiceInfrastructureService.noOfsSuisse) && !paysOfs.equals(0)) {
				String message = "l'adresse courrier n'est pas en Suisse";
				throw new ValidationException(dpi,message);

			}
			else {
				// Suisse
				AdresseSuisse adr = new AdresseSuisse();
				// Bornes

				// TYPE

				adr.setDateDebut(RegDate.get(2009, RegDate.JANVIER, 1));
				adr.setDateFin(null);
				adr.setUsage(TypeAdresseTiers.COURRIER);
				adr.setComplement(chez);
				if ((rueNo == null || rueNo == 0) && locNo != null) {
					adr.setRue(rue);
				}
				adr.setNumeroMaison(noPolice);
				lastAdresseDomicile = adr;
				adr.setPermanente(false);

				// Rue
				if((rueNo != null && rueNo!=0)  && (locNo != null && locNo!=0)){
					int localiteRue;

					try {
						localiteRue = helper.serviceInfrastructureService.getRueByNumero(rueNo).getNoLocalite();
						if(localiteRue== locNo){

							adr.setNumeroRue(rueNo);
						}else{
							String message = "La rue de l'adresse Courrier n'est pas dans la localité renseignée";
							throw new ValidationException(dpi,message);
						}
					}
					catch (InfrastructureException e) {
						String message = "La rue de l'adresse courrier n'a pas de localité";
						throw new ValidationException(dpi,message);
					}
				}
				else if ((rueNo != null && rueNo!=0) && (locNo == null ||locNo == 0)) {
					adr.setNumeroRue(rueNo);

				}
				else if ((locNo != null && locNo!=0) && (rueNo == null || rueNo == 0)) {
					adr.setNumeroOrdrePoste(locNo);

				}

				dpi.addAdresseTiers(adr);
			}

		}
	}



}


	private String trimAndNullify(String s) {
		if (s != null) {
			s = s.trim();
			if (s.equals("")) {
				s = null;
			}
		}
		return s;
	}

	/**
	 * Renvoie si le DPI peut recevoir des Rappel code=O => Envoie des rappel code=N => N'envoie pas de rappel
	 */
	private boolean getSansRappel(boolean actif, String code, String designAbregee) {
		boolean present = contientArobase(designAbregee);
		boolean result=false;
		if (actif) {
			if (code.equals("N")) {
				result= true;
			}
			else if (code.equals("O")) {
				result =  false;
			}

		}
		else if (present) {
			result = true;
		}
		else {

			result = false;
		}
		return result;
	}


	private boolean contientArobase(String designAbregee) {
		boolean present=false;

		if(designAbregee.indexOf("@")!=-1 || designAbregee.indexOf("@#")!=-1){
			present=true;
		}

		return present;
	}

	private CategorieImpotSource getCatIS(Integer code) {

		/*
		 * 1 => REGULIER 2 => CAS 3 => HYPOTHEQUE 4 => ADMIN 5 => PREST PREV
		 */

		CategorieImpotSource catIS = null;
		if (code != null) {
			if (code.equals(1)) {
				catIS = CategorieImpotSource.REGULIERS;
			}
			else if (code.equals(2)) {
				catIS = CategorieImpotSource.CONFERENCIERS_ARTISTES_SPORTIFS;
			}

		}
		return catIS;
	}

	private PeriodiciteDecompte getPeriodiciteDecompte(String codeStr) {

		final PeriodiciteDecompte pd;
		int code = Integer.parseInt(codeStr);
		if (code == 1) {
			pd = PeriodiciteDecompte.MENSUEL;
		}
		else if (code == 2) {
			pd = PeriodiciteDecompte.TRIMESTRIEL;
		}
		else if (code == 3) {
			pd = PeriodiciteDecompte.SEMESTRIEL;
		}
		else if (code == 4) {
			pd = PeriodiciteDecompte.ANNUEL;
		}
		else {
			// Ne doit jamais passer ici
			pd = null;
			Assert.fail();
		}
		return pd;
	}
private boolean determineSansListeRecapitulative(boolean codeActif,boolean designPresent){
boolean retour = false;

	if (codeActif) {
		retour = false;
	}
	else if (!codeActif && designPresent) {
		retour = true;
	}
	return retour;




}

@SuppressWarnings("unused")
private String formaterDesignation(String designation){
	String[]  tableauTravail = StringUtils.split(designation);
	for (int i = 0; i < tableauTravail.length; i++) {
		tableauTravail[i] = StringUtils.lowerCase(tableauTravail[i]);

	}
	String resultat=null;
	return resultat;
}
	private PeriodiciteDecompte getPeriodiciteDeclaration(String codeStr) {

		final PeriodiciteDecompte pd;
		int code = Integer.parseInt(codeStr);
		if (code >= 1 && code <=12) {
			pd = PeriodiciteDecompte.MENSUEL;
		}
		else if (code >= 13 && code <=16) {
			pd = PeriodiciteDecompte.TRIMESTRIEL;
		}
		else if (code >= 17 && code <=18) {
			pd = PeriodiciteDecompte.SEMESTRIEL;
		}
		else if (code == 19) {
			pd = PeriodiciteDecompte.ANNUEL;
		}
		else {
			// Ne doit jamais passer ici
			pd = null;
			Assert.fail();
		}
		return pd;
	}

	private ModeCommunication getModeCommunication(int isPaperOnLine, int isOnLine) {

		ModeCommunication mc = ModeCommunication.PAPIER;
		if (isPaperOnLine == 0) {
			if (isOnLine == 1) {
				mc = ModeCommunication.SITE_WEB;
			}
			else if (isOnLine == 0) {
				mc = ModeCommunication.ELECTRONIQUE;

			}

		}

		return mc;
	}

	@SuppressWarnings("unchecked")
	private List<ListOrderedMap> getFacturesEmployeur(int numero) {

		String sql = "SELECT " +
		" DAD_PER_EFFECTIVE, " +
		" DAF_PER_EFFECTIVE, " +
		" CO_PERIODE, " +
		" DA_EXPEDITION, " +
		" DA_EXIGIBILITE " +
		" FROM " +
		helper.getTableIs("FACTURE_EMPLOYEUR") +
		" WHERE " +
		" FK_CAE_EMPNO = " + numero	+
		" AND  YEAR(DAD_PER_EFFECTIVE) = 2009" +
		" AND FK_TFINOTECH = 1 " +
		" AND CO_ETAT = 'B' ORDER BY DAD_PER_EFFECTIVE ASC "; // Seulement 'AE' => Avis d'echeance

		List<ListOrderedMap> lrs = helper.isTemplate.queryForList(sql);
		return lrs;
	}

	@SuppressWarnings("unchecked")
	private List<ListOrderedMap> verifierFacturesEmployeur(int numero) {

		String sql = "SELECT " +
		" DAD_PER_EFFECTIVE, " +
		" DAF_PER_EFFECTIVE, " +
		" CO_PERIODE, " +
		" DA_EXPEDITION, " +
		" DA_EXIGIBILITE " +
		" FROM " +
		helper.getTableIs("FACTURE_EMPLOYEUR") +
		" WHERE " +
		" FK_CAE_EMPNO = " + numero	+
		" AND  YEAR(DAD_PER_EFFECTIVE) = 2009" +
		" AND FK_TFINOTECH = 1 " +
		" AND CO_ETAT <> 'B'"; // Seulement 'AE' => Avis d'echeance

		List<ListOrderedMap> lrs = helper.isTemplate.queryForList(sql);
		return lrs;
	}
	@SuppressWarnings({"unchecked", "unused"})
	private List<ListOrderedMap> getDecomptesEmployeur(int numero) {

		String sql = "SELECT " + " DAD_PERIODE, " + " DAF_PERIODE, " + " MODE_DECOMPTE " + " FROM "
				+ helper.getTableIs("DECOMPTE_EMPLOYEUR") + " WHERE " + " NO_EMPLOYEUR = " + numero + " AND DAD_PERIODE >= '2007-01-01'";

		List<ListOrderedMap> list = helper.isTemplate.queryForList(sql);
		return list;
	}

	private void migrateDeclarationImpotSource(int numeroEmployeur, DebiteurPrestationImposable dpi) {

		LOGGER.debug("Migration des LR du DPI " + numeroEmployeur);

	/*	List<ListOrderedMap> facsErreur = verifierFacturesEmployeur(numeroEmployeur);
		if (facsErreur!=null && !facsErreur.isEmpty()) {
			ValidationResults  validation = new ValidationResults();
			for (ListOrderedMap fac : facsErreur) {
				RegDate dateDebut = RegDate.get((Date) fac.getValue(0));
				RegDate dateFin = RegDate.get((Date) fac.getValue(1));
				String message= "La LR ["+dateDebut.toString()+" - "+dateFin.toString()+"] a un code état incorrect:";
				validation.addError(message);
			}
			throw new ValidationException(dpi,validation.getErrors(),validation.getWarnings());

		}*/

		// FAC => FACTURE_EMPLOYEUR
		List<ListOrderedMap> facs = getFacturesEmployeur(numeroEmployeur);


		DeclarationImpotSource lrPrecedente=null;
		for (ListOrderedMap fac : facs) {

			RegDate dateDebut = RegDate.get((Date) fac.getValue(0));
			RegDate dateFin = RegDate.get((Date) fac.getValue(1));
			String periodicite = fac.getValue(2).toString();
			RegDate dateExpedition = RegDate.get((Date) fac.getValue(3));
			RegDate dateDelaiAccordee = RegDate.get((Date) fac.getValue(4));
			final PeriodeFiscale periode = helper.periodeFiscaleDAO.getPeriodeFiscaleByYear(dateDebut.year());

			DeclarationImpotSource lr = new DeclarationImpotSource();
			lr.setDateDebut(dateDebut);
			lr.setDateFin(dateFin);
			lr.setPeriodicite(getPeriodiciteDeclaration(periodicite));
			lr.setPeriode(periode);
			lr.setModeCommunication(dpi.getModeCommunication());
			final ModeleDocument listeRecapitulative = helper.modeleDocumentDAO.getModelePourDeclarationImpotSource(periode);
			Assert.notNull(listeRecapitulative);
			EtatDeclaration etatDeclaration = new EtatDeclaration();
			etatDeclaration.setEtat(TypeEtatDeclaration.EMISE);
			etatDeclaration.setDateObtention(dateExpedition);
			DelaiDeclaration delaiDeclaration = new DelaiDeclaration();
			delaiDeclaration.setDateTraitement(dateExpedition);
			delaiDeclaration.setDelaiAccordeAu(dateDelaiAccordee);
			delaiDeclaration.setConfirmationEcrite(false);
			lr.setModeleDocument(listeRecapitulative);
			lr.addEtat(etatDeclaration);
			lr.addDelai(delaiDeclaration);

			if(!isLRaDouble(lrPrecedente, lr.getDateDebut(),lr.getDateFin())){
				lr.setTiers(dpi);
				dpi.addDeclaration(lr);
				//lr = (DeclarationImpotSource)helper.hibernateTemplate.merge(lr);
				lrPrecedente = lr;
			}

		}

	}

	private boolean isLRaDouble(DeclarationImpotSource lr, RegDate dateDebut, RegDate dateFin){
		boolean reponse= false;
		if (lr!=null) {
			if (lr.getDateDebut().equals(dateDebut) && lr.getDateFin().equals(dateFin)) {
				reponse = true;
			}
		}

		return reponse;
	}
	private RegDate normalizeDb2Date(Object o) {
		Date date = null;
		if (o instanceof java.util.Date) {
			date = (Date) o;
		}
		return RegDate.get(date);
	}

	@SuppressWarnings("unused")
	private ListOrderedMap getFactureFor(List<ListOrderedMap> facs, RegDate decDebut, RegDate decFin) {
		ListOrderedMap fac = null;
		for (ListOrderedMap f : facs) {
			RegDate dateDebut = normalizeDb2Date(f.get(INDEX_FAC_DAD_PER_EFFECTIVE));
			RegDate dateFin = normalizeDb2Date(f.get(INDEX_FAC_DAD_PER_EFFECTIVE));
			if (dateDebut.equals(decDebut) && dateFin.equals(decFin)) {
				fac = f;
				break;
			}
		}
		return fac;
	}

	@SuppressWarnings("unused")
	private void lrAddEtats(DeclarationImpotSource lr, ListOrderedMap fac) {

		// String codeEtat = trimAndNullify((String)fac.getValue(INDEX_FAC_CO_ETAT));
		RegDate dateRecept = normalizeDb2Date(fac.getValue(INDEX_FAC_DA_RECEP_LIST_NOMI));
		RegDate dateRappel = normalizeDb2Date(fac.getValue(INDEX_FAC_DA_RAPPEL_LST_NOM));
		RegDate date2emeRappel = normalizeDb2Date(fac.getValue(INDEX_FAC_DA_2EME_RAPPEL_LN));
		// Date dateTblnImpot = normalizeDb2Date((Date)fac.get(INDEX_FAC_
		RegDate dateExpedition = normalizeDb2Date(fac.getValue(INDEX_FAC_DA_EXPEDITION));
		RegDate dateExigibilite = normalizeDb2Date(fac.getValue(INDEX_FAC_DA_EXIGIBILITE));

		// Envoyé
		if (dateExigibilite != null) {
			addEtatToLr(lr, TypeEtatDeclaration.EMISE, dateExpedition);
		}

		// Retourné
		if (dateRecept != null) {
			addEtatToLr(lr, TypeEtatDeclaration.RETOURNEE, dateRecept);
		}

		// Rappel
		if (dateRappel != null) {
			addEtatToLr(lr, TypeEtatDeclaration.SOMMEE, dateRappel);
		}

		// 2eme rappel
		if (date2emeRappel != null) {
			addEtatToLr(lr, TypeEtatDeclaration.SOMMEE, date2emeRappel);
		}
	}

	private EtatDeclaration addEtatToLr(DeclarationImpotSource lr, TypeEtatDeclaration type, RegDate date) {
		EtatDeclaration etat = new EtatDeclaration();
		etat.setEtat(type);
		etat.setDateObtention(date);
		lr.addEtat(etat);
		return etat;
	}

	@SuppressWarnings("unused")
	private void lrAddDelais(DeclarationImpotSource lr, List<ListOrderedMap> facs, List<ListOrderedMap> decs) {

		DelaiDeclaration delai = new DelaiDeclaration();

		lr.addDelai(delai);
	}

	@SuppressWarnings("unused")
	private ModeCommunication getModeCommunication(RegDate debutLr, List<ListOrderedMap> decs) {

		ModeCommunication mode = ModeCommunication.PAPIER;
		for (ListOrderedMap dec : decs) {
			RegDate debut = RegDate.get((Date) dec.getValue(0));
			// Date fin = (Date)dec.getValue(1);
			String theMode = (String) dec.getValue(2);

			if (debut.equals(debutLr)) {
				// Trouvé
				if (theMode.equals("S")) {
					mode = ModeCommunication.SITE_WEB;
				}
				else {
					mode = ModeCommunication.ELECTRONIQUE;
				}
			}
		}
		return mode;
	}

	/**
	 * Les codes possibles sont:
	 * <li>C => ANNULE?
	 * <li>H =>
	 * <li>L =>
	 * <li>E =>
	 *
	 * @param code
	 * @return
	 */
	@SuppressWarnings("unused")
	private CodeEtat getCodeEtat(String code) {

		CodeEtat etat = CodeEtat.INCONNU;
		if (code.equals("C")) {
			etat = CodeEtat.ANNULE;
		}
		else if (code.equals("H")) {
			etat = CodeEtat.ANNULE;
		}
		else if (code.equals("L")) {
			etat = CodeEtat.ANNULE;
		}
		else if (code.equals("E")) {
			etat = CodeEtat.ANNULE;
		}
		else {
			etat = CodeEtat.INCONNU;
		}
		return etat;
	}

	private DebiteurLu buildDebiteurLu(ResultSet rs) throws SQLException {
		DebiteurLu debiteurCourant = new DebiteurLu();
		 debiteurCourant.numero = rs.getInt(1);
		 debiteurCourant.design1 = rs.getString(2);
		 debiteurCourant.design2 = rs.getString(3);
		 debiteurCourant.design3 = rs.getString(4);
		 debiteurCourant.design4 = rs.getString(5);
		 debiteurCourant.correspondant = rs.getString(6);
		 debiteurCourant.noTelephone = rs.getString(7);
		 debiteurCourant.coPeriodicite = rs.getString(8);
		 debiteurCourant.coRappel = rs.getString(9);
		 debiteurCourant.noEntreprise = rs.getInt(10);
		 debiteurCourant.catEmp = rs.getInt(11);
		 debiteurCourant.designAbregee = rs.getString(12);

		return debiteurCourant;
	}
	private List<DebiteurLu> buildListeDebiteurLu(String sqlSelection) throws Exception{
		ResultSet rs = null;
		List<DebiteurLu> debiteurs = new ArrayList<DebiteurLu>();
		Statement stat = helper.db2Connection.createStatement();
		rs = stat.executeQuery(sqlSelection);
		try {
			while (rs.next()) {

				DebiteurLu debiteurCourant = buildDebiteurLu(rs);
				debiteurs.add(debiteurCourant);

			}
		}
		finally {
			rs.close();
			stat.close();
		}


		Collections.sort(debiteurs, new Comparator<DebiteurLu>() {

			public int compare(DebiteurLu o1, DebiteurLu o2) {
				return o1.numero.compareTo(o2.numero);
			}


		});

		return debiteurs;

	}
	private DebiteurPrestationImposable buildAndSaveDPI(DebiteurLu debiteur,boolean actif) throws Exception{

		//int nbrHorsCanton = 0;
		Statement statEmpAci = helper.empConnection.createStatement();


		// Creation de l'employeur en tant que tiers référent
		//Tiers tiers = null;
		// Création de l'employeur en tant que qu'autre communaute
		debiteur.design1 = debiteur.design1.trim();
		debiteur.design2 = debiteur.design2.trim();
		debiteur.design3 = debiteur.design3.trim();
		debiteur.design4 = debiteur.design4.trim();


		// TODO(BNM) mettre en place les nouveau numéro de contribuable
		// de 002.500.000 à 002.999.999 voir dans le core
		//entreprise.setNumero(theNumero)

		int isPaperOnLine = -1;
		String email = null;
		int isOnLine = -1;
		boolean presentdansEmpaci=false;

		// DPI
		final long numeroDebiteur = debiteur.numero + DebiteurPrestationImposable.FIRST_MIGRATION_ID;
		if (helper.tiersDAO.exists(numeroDebiteur)) {
			final String message = "Le débiteur n°" + numeroDebiteur + " a déjà été migré.";
			LOGGER.error(message);
			throw new IllegalArgumentException(message);
		}

		DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
		dpi.setNumero(numeroDebiteur);
		dpi.setNom1(debiteur.design1);
		dpi.setNom2(debiteur.design2);
		dpi.setComplementNom(debiteur.design3);
		dpi.setPersonneContact(debiteur.correspondant);
		dpi.setNumeroTelephoneProfessionnel(debiteur.noTelephone);
		dpi = (DebiteurPrestationImposable)helper.tiersDAO.save(dpi);

		//la création du contacte impot source fait appel a un merge
		//Peut provoquer un bug par exemple création à double des LR (Voir avec Manu)
		Long numeroPM = helper.mapDebiteurPm.get(debiteur.numero) ;
		//création de la personne morale
		if (numeroPM!=null) {

			Entreprise entreprise = (Entreprise) helper.tiersService.getTiers(numeroPM);
			if (entreprise == null) {
				entreprise = new Entreprise();
				entreprise.setNumero(numeroPM);
				entreprise.setNumeroEntreprise(numeroPM);
				entreprise = (Entreprise) helper.tiersDAO.save(entreprise);
			}

			helper.tiersService.addContactImpotSource(dpi, entreprise, RegDate.get(2009, RegDate.JANVIER, 1));
		}

		String sqlEmployeurEmpAci = "SELECT " + " ISPAPERONLINE,ISONLINE,EMAIL" + " FROM EMPLOYEUR WHERE NUMEMPLOYEUR=" + debiteur.numero;

		ResultSet rsEmp = statEmpAci.executeQuery(sqlEmployeurEmpAci);
		try {
			while (rsEmp.next()) {
				isPaperOnLine = rsEmp.getInt(1);
				isOnLine = rsEmp.getInt(2);
				email = rsEmp.getString(3);
				presentdansEmpaci=true;

				if (isPaperOnLine == 0) {
					dpi.setAdresseCourrierElectronique(email);
				}
			}

		}
		finally {
			rsEmp.close();
			statEmpAci.close();
		}

		dpi.setBlocageRemboursementAutomatique(false);

		dpi.setDebiteurInactif(false);

		if (debiteur.catEmp == 1 || debiteur.catEmp == 2) {
			dpi.setCategorieImpotSource(getCatIS(debiteur.catEmp));
		}
		else{
			String message = debiteur.numero+";"+"Categorie employeur incorrect: "+debiteur.catEmp+";BLOQUANTE";
			ESSAI.info(message);
			throw new ValidationException(dpi,message);
		}

		//Periodicité
		int code = Integer.parseInt(debiteur.coPeriodicite);

		if (code== 1 || code == 2 || code == 3 || code == 4) {
			dpi.setPeriodiciteDecompte(getPeriodiciteDecompte(debiteur.coPeriodicite));
		}
		else {
			String message = debiteur.numero+";"+"code périodicité incorrect: "+debiteur.coPeriodicite+";BLOQUANTE";
			ESSAI.info(message);
			throw new ValidationException(dpi,message);
		}
		//Mode de communication
		boolean codePaperCorrect = true;
		boolean codeOnLineCorrect =true;

		if (isPaperOnLine!= 0 && isPaperOnLine!=1 && presentdansEmpaci) {
			String message = debiteur.numero+";"+"code isPaperOnLine incorrect: "+isPaperOnLine+";BLOQUANTE";
			ESSAI.info(message);
			throw new ValidationException(dpi,message);
			//codePaperCorrect = false;
		}

		if (isOnLine!= 0 && isOnLine!=1 && presentdansEmpaci) {
			String message = debiteur.numero+";"+"code isPaperOnLine incorrect: "+isPaperOnLine+";BLOQUANTE";
			ESSAI.info(message);
			throw new ValidationException(dpi,message);
			//codeOnLineCorrect = false;
		}

		if (codeOnLineCorrect && codePaperCorrect) {
			dpi.setModeCommunication(getModeCommunication(isPaperOnLine, isOnLine));
		}

		if (debiteur.coRappel==null || "".equals(debiteur.coRappel) || " ".equals(debiteur.coRappel)) {
			debiteur.coRappel= "O";
		}

		if(debiteur.coRappel.equals("O") || debiteur.coRappel.equals("N") ) {
			dpi.setSansRappel(getSansRappel(actif,debiteur.coRappel, debiteur.designAbregee));
		}
		else {

			String message = debiteur.numero+";"+"code rappel incorrect: "+debiteur.coRappel+";BLOQUANTE";
			ESSAI.info(message);
			throw new ValidationException(dpi,message);
		}

		boolean designPresent = contientArobase(debiteur.designAbregee);
		dpi.setSansListeRecapitulative(determineSansListeRecapitulative(actif, designPresent));

		migrateDeclarationImpotSource(debiteur.numero, dpi);
		migrateAdresses(debiteur.numero, dpi);

		// Création du for DPI

		ForDebiteurPrestationImposable forDebiteur = new ForDebiteurPrestationImposable();

		forDebiteur.setDateDebut(RegDate.get(2009, RegDate.JANVIER, 1));
		if (!actif && !designPresent) {
			forDebiteur.setDateFin(determinateDateFinForDebiteur(debiteur.numero));
		}
		else {
			forDebiteur.setDateFin(null);
		}

		forDebiteur.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
		Commune commune = getCommuneAutoriteFiscale(dpi);

		if (commune != null) {
			forDebiteur.setNumeroOfsAutoriteFiscale(commune.getNoOFSEtendu());
			if (commune.getSigleCanton().equals("VD")) {
				forDebiteur.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			}
			else {
				forDebiteur.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_HC);
			}
		}
		else {
			Audit.error("la commune du DPI " + dpi.getNumero() + " est null");

			String message = "La commune de l'autorité fiscale n'est pas renseignée";
			throw new ValidationException(dpi,message);
		}

		// si un for a été determiné sans erreur
		forDebiteur.setTiers(dpi);
		dpi.addForFiscal(forDebiteur);
		helper.hibernateTemplate.save(forDebiteur);
		//dpi = (DebiteurPrestationImposable)helper.tiersDAO.save(dpi);



		return dpi;
	}

	@Override
	protected Tiers saveTiers(Tiers tiers) {
		Assert.notNull(tiers);

		// DEBUG
		/*long numero = -1L;
		if (tiers.getNumero() != null) {
			numero = tiers.getNumero();
		}*/
		ValidationResults  validation = tiers.validate();
		if (!validation.getErrors().isEmpty()) {
			throw new ValidationException(tiers,validation.getErrors(),validation.getWarnings());
		}

		tiers = helper.tiersDAO.save(tiers);

		return tiers;
	}
}
