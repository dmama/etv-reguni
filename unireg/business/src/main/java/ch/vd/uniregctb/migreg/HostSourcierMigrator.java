package ch.vd.uniregctb.migreg;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.date.DateRangeHelper.Range;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.interfaces.model.Canton;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.IdentificationPersonne;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RapportPrestationImposable;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.tiers.SituationFamilleMenageCommun;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.CategorieEtranger;
import ch.vd.uniregctb.type.CategorieIdentifiant;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TarifImpotSource;
import ch.vd.uniregctb.type.TypeActivite;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

// FIXME (GDY) supprimer les annotations "unused" lorsque le migrator sera stable.
public class HostSourcierMigrator extends HostMigrator {

	private static final Logger LOGGER = Logger.getLogger(HostSourcierMigrator.class);
	private static final Logger REJET = Logger.getLogger(HostSourcierMigrator.class.getName() + ".Rejet");
	private static final Logger RAPPORT = Logger.getLogger(HostSourcierMigrator.class.getName() + ".Rapport");
	private static final Logger DOUBLON = Logger.getLogger(HostSourcierMigrator.class.getName() + ".Doublon");

	private static final String NUMERO_INDIVIDU = "NO_IND_REG_FISCAL";
	private static final String NUMERO_SOURCIER = "NO_SOURCIER";
	private static final String NUMERO_REGISTRE_ETRANGER = "NO_REGISTRE_ETRANG";
	private static final String NUMERO_REF_CANTONAL = "NO_REF_CANTONAL";
	private static final String NUMERO_AVS = "NO_AVS";
	private static final String CODE_SEXE = "SEXE";
	private static final String NOM_SOURCIER = "NOM";
	private static final String PRENOM_SOURCIER = "PRENOM";
	private static final String REMARQUE_SOURCIER = "REMARQUE";
	private static final String DATE_NAISSANCE_SOURCIER = "DATE_NAISSANCE";
	private static final String DATE_NAISSANCE_INDIVIDU = "DATE_NAISSANCE";
	private static final String SEXE_SOURCIER = "SEXE";
	private static final String SEXE_INDIVIDU = "SEXE";
	// private static final String NUMERO_INDIVIDU_ENFANT = "FK_INDNO_ENFANT";

	private static final String NO_SEQUENCE_ADRESSE_FISCALE = "NOSEQ_ADRFISC";
	private static final String ADR_FISCALE_DATE_DEBUT = "DAD_ADR_FISCALE";
	private static final String ADR_FISCALE_DATE_FIN = "DAF_ADR_FISCALE";
	private static final String ADR_FISCALE_DATE_DEPART = "DA_DEPART_ETRANGER";
	private static final String ADR_FISCALE_DATE_RETOUR = "DA_RETOUR_ETRANGER";
	private static final String ADR_FISCALE_NO_TECH_COMMUNE = "FK_COMNO";
	private static final String ADR_FISCALE_NOM_COMMUNE = "NOM_OFS_MIN";

	private static final String NO_SEQUENCE_TYPE_IMPOSITION = "NOSEQ_TYIMP";
	private static final String TYPE_IMPOSIT_DATE_DEBUT = "DAD_TY_IMPOSIT";
	private static final String TYPE_IMPOSIT_DATE_FIN = "DAF_TY_IMPOSIT";
	private static final String TYPE_IMPOSITION = "TY_IMPOSIT";
	private static final String TYPE_PERMIS_SOURCIER = "TYPE_PERMIS";

	private static final String NO_SEQUENCE_SITUATION_FAMILLE = "NOSEQ_SITFAM";
	private static final String SIT_FAMILLE_DATE_DEBUT = "DAD_SITU_FAMILLE";
	private static final String SIT_FAMILLE_DATE_FIN = "DAF_SITU_FAMILLE";
	private static final String SIT_FAMILLE_ETAT_CIVIL = "ET_CIVIL";
	private static final String SIT_FAMILLE_CHARGE_FAMILLE = "CHARGE_FAMILLE";
	private static final String SIT_FAMILLE_NB_ENFANTS = "NB_ENFANTS";
	private static final String SIT_FAMILLE_NO_SRC_CONJOINT = "CONJOINT";

	private static final String NO_SEQUENCE_PERMIS_TRAVAIL = "NOSEQ_PERMIS";
	private static final String PERMIS_TRAVAIL_DATE_DEBUT = "DAD_PERMIS";
	private static final String PERMIS_TRAVAIL_DATE_FIN = "DAF_PERMIS";
	private static final String PERMIS_TRAVAIL_TYPE = "TY_PERMIS";

	private static final String NO_SEQUENCE_PERMIS_TRAVAIL_IND = "NOSEQ_PERMIS_IND";
	private static final String PERMIS_TRAVAIL_IND_DATE_DEBUT = "DAD_PERMIS_IND";
	private static final String PERMIS_TRAVAIL_IND_DATE_FIN = "DAF_PERMIS_IND";
	private static final String PERMIS_TRAVAIL_IND_TYPE = "TY_PERMIS_IND";

	private static final String INDIVIDU_IDENTIFIANT_CH = "IDENTIFIANT_CH";
	private static final String NOM_INDIVIDU = "INDIVIDU_NOM";
	private static final String PRENOM_INDIVIDU = "INDIVIDU_PRENOM";

	private static final String INDIVIDU_NO_IND_ENFANT = "FK_INDNO_ENFANT";

	private static final String NO_SEQUENCE_ETAT_CIVIL_IND = "NOSEQ_ETCIV_IND";
	private static final String ETAT_CIVIL_IND_DATE_DEBUT = "DAD_ETCIV_IND";
	private static final String ETAT_CIVIL_IND_DATE_INV_MAR = "DA_INVALID_MARIAGE";
	private static final String ETAT_CIVIL_IND_MARIE_SEUL = "MARIE_SEUL";
	private static final String ETAT_CIVIL_IND_CODE = "ETCIV_IND";
	private static final String ETAT_CIVIL_IND_CONJOINT = "CONJOINT_IND";

	private static final String DATE_DEBUT_RAPPORT = "DAD_RAPPORT_TRAV";

	private static final String DATE_FIN_RAPPORT = "DAF_RAPPORT_TRAV";
	private static final String TYPE_ACTIVITE = "TY_ACTIVITE";
	private static final String NUMERO_EMPLOYEUR = "NO_EMPLOYEUR";
	private static final String NUMERO_EMPLOYEUR_RAPPORT = "FK_EMPNO";

	private static final String BAREME_DATE_DEBUT = "DAD_ASS_BAR";
	private static final String BAREME_DATE_FIN = "DAF_ASS_BAR";
	private static final String BAREME_CO_BAREME = "CO_BAREME";


	// private static final String NO_SEQUENCE_ADRESSE_IND = "NOSEQ_ADRIND";
	// private static final String ADRESSE_IND_DAD_VALIDITE = "DAD_ADRIND";
	// private static final String ADRESSE_IND_DAF_VALIDITE = "DAF_ADRIND";
	// private static final String ADRESSE_IND_CHEZ = "CHEZ_ADRIND";
	// private static final String ADRESSE_IND_NOM_RUE = "RUE_ADRIND";
	// private static final String ADRESSE_IND_NO_POLICE = "NOPOL_ADRIND";
	// private static final String ADRESSE_IND_LIEU = "LIEU_ADRIND";
	// private static final String ADRESSE_IND_ID_LOC_POSTALE = "LOCPOS_ADRIND";
	// private static final String ADRESSE_IND_ID_RUE = "RUEOFS_ADRIND";
	// private static final String ADRESSE_IND_ID_PAYS = "PAYS_ADRIND";
	// private static final String ADRESSE_IND_DESIGN_RUE_OFS = "NOM_RUEOFS_ADRIND";
	// private static final String ADRESSE_IND_ID_LOCPOS_RUE_OFS = "LOCPOS_RUEOFS_ADRIND";

	private static final String NO_SEQUENCE_ADRESSE_SRC = "NOSEQ_ADRSRC";
	private static final String ADRESSE_SRC_DAD_VALIDITE = "DAD_ADRSRC";
	private static final String ADRESSE_SRC_DAF_VALIDITE = "DAF_ADRSRC";
	private static final String ADRESSE_SRC_CHEZ = "CHEZ_ADRSRC";
	private static final String ADRESSE_SRC_NOM_RUE = "RUE_ADRSRC";
	private static final String ADRESSE_SRC_NO_POLICE = "NOPOL_ADRSRC";
	private static final String ADRESSE_SRC_LIEU = "LIEU_ADRSRC";
	private static final String ADRESSE_SRC_ID_LOC_POSTALE = "LOCPOS_ADRSRC";
	private static final String ADRESSE_SRC_ID_RUE = "RUEOFS_ADRSRC";
	private static final String ADRESSE_SRC_ID_PAYS = "PAYS_ADRSRC";
	private static final String ADRESSE_SRC_DESIGN_RUE_OFS = "NOM_RUE_ADRSRC";
	private static final String ADRESSE_SRC_ID_LOCPOS_RUE_OFS = "LOCPOS_RUE_ADRSRC";

	// private final List<RowRsSourcier> lstSourciersGris = new ArrayList<RowRsSourcier>();

	private final HostSourcierMigratorHelper sourcierHelper = new HostSourcierMigratorHelper();
	private final List<RowRsSourcier> lstSourciers = new ArrayList<RowRsSourcier>();

	private final List<SourcierAdresseFiscale> lstAdrFiscale = new ArrayList<SourcierAdresseFiscale>();
	private final List<SourcierTypeImposition> lstTyImposit = new ArrayList<SourcierTypeImposition>();
	private final List<SourcierSituationFamille> lstSitFam = new ArrayList<SourcierSituationFamille>();
	private final List<SourcierPermisTravail> lstPermis = new ArrayList<SourcierPermisTravail>();
	private final List<SourcierAdressePostale> lstSrcAdressePostale = new ArrayList<SourcierAdressePostale>();
	// private final List<SourcierAdressePostale> lstIndAdressePostale = new ArrayList<SourcierAdressePostale>();
	// private final List<SourcierPermisTravail> lstPermisInd = new ArrayList<SourcierPermisTravail>();

	private final StringBuilder sourciersAMigrer = null;
	private StringBuilder employeurValide = null;

	// private int nbEnfantsInd;
	private int sourcierStart = -1;
	private int sourcierEnd = -1;

	public HostSourcierMigrator(HostMigratorHelper deps, MigRegLimits limits, MigregStatusManager mgr, StringBuilder employeurValide) {
		super(deps, limits, mgr);
		if (limits.srcFirst != null) {
			sourcierStart = limits.srcFirst;

			if (limits.srcEnd != null) {
				sourcierEnd = limits.srcEnd;
			}
		}
		sourcierHelper.setHelper(this.helper);
		this.employeurValide = employeurValide;
	}

	@Override
	public int migrate() throws Exception {

		int ret = 0;

		try {
			if (limits.getNoPassSrc() == 1) {
				ret = migrateSourcierSansConjoint();
				ret = ret + migrateSourcierEnCouple();
			}
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}

		return ret;
	}

	private abstract class SrcGeneric {
		public int noSequence;
		public RegDate dateDebut;
		public RegDate dateFin;

		public int compareTo(Object other) {
			RegDate date1 = ((SrcGeneric) other).dateDebut;
			RegDate date2 = this.dateDebut;
			if (date1.isAfter(date2))
				return -1;
			else if (date1 == date2)
				return 0;
			else
				return 1;
		}
	}

	private class SourcierAdresseFiscale extends SrcGeneric implements Comparable<Object> {
		public MotifFor motifOuverture;
		public MotifFor motifFermeture;
		public RegDate dateDepartEtranger;
		public RegDate dateRetourEtranger;
		private int communeFiscale;


	};

	private class SourcierTypeImposition extends SrcGeneric implements Comparable<Object> {
		public String typeImposition;
		public MotifFor motifOuverture;
		public MotifFor motifFermeture;


	};

	private class SourcierBareme  extends SrcGeneric implements Comparable<Object> {
	 public String codeBareme;
	}

	private class SourcierSituationFamille extends SrcGeneric implements Comparable<Object> {
		public EtatCivil etatCivil;
		public int nbEnfant;
		public String chargeFamille;
		public Long conjointNoSourcier;
		public MotifFor motifOuverture;
		public MotifFor motifFermeture;


	};

	private class SourcierPermisTravail extends SrcGeneric implements Comparable<Object> {
		public String typePermis;

		@Override
		public int compareTo(Object other) {
			RegDate date1 = ((SourcierPermisTravail) other).dateDebut;
			RegDate date2 = this.dateDebut;
			if (date1.isAfter(date2))
				return -1;
			else if (date1 == date2)
				return 0;
			else
				return 1;
		}
	};

	private class SourcierAdressePostale extends SrcGeneric implements Comparable<Object> {
		public String chez;
		public String nomRue;
		public String noPolice;
		public String lieu;
		public int idLocPos;
		public int idPays;
		public int idRue;
		public String designationRueOfs;
		public int idLocPosRueOfs;

		@Override
		public int compareTo(Object other) {
			RegDate date1 = ((SourcierAdressePostale) other).dateDebut;
			RegDate date2 = this.dateDebut;
			if (date1.isAfter(date2))
				return -1;
			else if (date1 == date2)
				return 0;
			else
				return 1;
		}

	}

	@SuppressWarnings("unused")
	private class RowRsSourcier {
		private Long noSourcier;
		private Long noIndividu;
		private Long noAvs;
		private String sexe;
		private String nomSrc;
		private String prenomSrc;
		private RegDate dateNaissance;
		private int noSeqAdrFisc;
		private RegDate dateDebutAdrFisc;
		private RegDate dateFinAdrFisc;
		private int communeNoTech;
		private int noSeqTyImp;
		private RegDate dateDebutTyImp;
		private RegDate dateFinTyImp;
		private String typeImposition;
		private int noSeqSitFam;
		private RegDate dateDebutSitFam;
		private RegDate dateFinSitFam;
		private String etatCivilSrc;
		private int nbEnfantsSrc;
		private Long noSourcierConjoint;
		private int noSeqPermisTr;
		private RegDate dateDebutPermisTr;
		private RegDate dateFinPermisTr;
		private String typePermisTr;
		private int noSeqAdrSrc;
		private RegDate dateDebutAdrSrc;
		private RegDate dateFinAdrSrc;
		private String chezSrc;
		private String nomRueSrc;
		private String noPoliceSrc;
		private String lieuSrc;
		private int idLocPostSrc;
		private int idPaysSrc;
		private int idRueSrc;
		private String designRueOfsSrc;
		private int idLocPosRueOfsSrc;
	};

	public class SourcierLu {
		public Long noSourcier;
		private Long noIndividu;
		private String noRegistreEtranger;
		private String numeroReferenceCantonal;
		private String noAvs;
		private String sexe;
		private String nom;
		private String prenom;
		private String remarque;
		private String typePermis;
		private RegDate dateNaissance;
		private Long noEmployeur;

	};

	private class RowRsInd {
		public int noSeqPermisTrInd;
		public RegDate dateDebutPermisTrInd;
		public RegDate dateFinPermisTrInd;
		public String typePermisTrInd;
		public String nomInd;
		public String prenomInd;
		public String identificationCh;
		public Long noIndividuEnfant;
		public int noSeqAdrInd;
		public RegDate dateDebutAdrInd;
		public RegDate dateFinAdrInd;
		public String chezInd;
		public String nomRueInd;
		public String noPoliceInd;
		public String lieuInd;
		public int idLocPostInd;
		public int idPaysInd;
		public int idRueInd;
		public String designRueOfsInd;
		public int idLocPosRueOfsInd;
		public int noSeqEtCivil;
		public RegDate dateValiditeEtCivil;
		public String codeEtCivil;
		public RegDate dateInvalidMariage;
		public String marieSeul;
		public Long noIndConjoint;
	};

	private RowRsSourcier buildRowRsSrc(ResultSet rs) throws Exception {
		RowRsSourcier row = new RowRsSourcier();

		row.noSourcier = rs.getLong(NUMERO_SOURCIER);
		row.noIndividu = rs.getLong(NUMERO_INDIVIDU);
		row.noAvs = rs.getLong(NUMERO_AVS);
		row.sexe = rs.getString(CODE_SEXE);
		row.nomSrc = rs.getString(NOM_SOURCIER);
		row.prenomSrc = rs.getString(PRENOM_SOURCIER);
		row.dateNaissance = RegDate.get(rs.getDate(DATE_NAISSANCE_SOURCIER));
		row.noSeqAdrFisc = rs.getInt(NO_SEQUENCE_ADRESSE_FISCALE);
		row.dateDebutAdrFisc = RegDate.get(rs.getDate(ADR_FISCALE_DATE_DEBUT));
		row.dateFinAdrFisc = RegDate.get(rs.getDate(ADR_FISCALE_DATE_FIN));
		row.communeNoTech = rs.getInt(ADR_FISCALE_NO_TECH_COMMUNE);
		row.noSeqTyImp = rs.getInt(NO_SEQUENCE_TYPE_IMPOSITION);
		row.dateDebutTyImp = RegDate.get(rs.getDate(TYPE_IMPOSIT_DATE_DEBUT));
		row.dateFinTyImp = RegDate.get(rs.getDate(TYPE_IMPOSIT_DATE_FIN));
		row.typeImposition = rs.getString(TYPE_IMPOSITION);
		row.noSeqSitFam = rs.getInt(NO_SEQUENCE_SITUATION_FAMILLE);
		row.dateDebutSitFam = RegDate.get(rs.getDate(SIT_FAMILLE_DATE_DEBUT));
		row.dateFinSitFam = RegDate.get(rs.getDate(SIT_FAMILLE_DATE_FIN));
		row.etatCivilSrc = rs.getString(SIT_FAMILLE_ETAT_CIVIL);
		row.nbEnfantsSrc = rs.getInt(SIT_FAMILLE_NB_ENFANTS);
		row.noSourcierConjoint = rs.getLong(SIT_FAMILLE_NO_SRC_CONJOINT);
		row.noSeqPermisTr = rs.getInt(NO_SEQUENCE_PERMIS_TRAVAIL);
		row.dateDebutPermisTr = RegDate.get(rs.getDate(PERMIS_TRAVAIL_DATE_DEBUT));
		row.dateFinPermisTr = RegDate.get(rs.getDate(PERMIS_TRAVAIL_DATE_FIN));
		row.typePermisTr = rs.getString(PERMIS_TRAVAIL_TYPE);
		row.noSeqAdrSrc = rs.getInt(NO_SEQUENCE_ADRESSE_SRC);
		row.dateDebutAdrSrc = RegDate.get(rs.getDate(ADRESSE_SRC_DAD_VALIDITE));
		row.dateFinAdrSrc = RegDate.get(rs.getDate(ADRESSE_SRC_DAF_VALIDITE));
		row.chezSrc = rs.getString(ADRESSE_SRC_CHEZ);
		row.nomRueSrc = rs.getString(ADRESSE_SRC_NOM_RUE);
		row.noPoliceSrc = rs.getString(ADRESSE_SRC_NO_POLICE);
		row.lieuSrc = rs.getString(ADRESSE_SRC_LIEU);
		row.idLocPostSrc = rs.getInt(ADRESSE_SRC_ID_LOC_POSTALE);
		row.idPaysSrc = rs.getInt(ADRESSE_SRC_ID_PAYS);
		row.idRueSrc = rs.getInt(ADRESSE_SRC_ID_RUE);
		row.designRueOfsSrc = rs.getString(ADRESSE_SRC_DESIGN_RUE_OFS);
		row.idLocPosRueOfsSrc = rs.getInt(ADRESSE_SRC_ID_LOCPOS_RUE_OFS);
		return row;
	}

	@SuppressWarnings("unused")
	private RowRsInd buildRowRsInd(ResultSet rs) throws Exception {
		RowRsInd row = new RowRsInd();

		row.noSeqPermisTrInd = rs.getInt(NO_SEQUENCE_PERMIS_TRAVAIL_IND);
		row.dateDebutPermisTrInd = RegDate.get(rs.getDate(PERMIS_TRAVAIL_IND_DATE_DEBUT));
		row.dateFinPermisTrInd = RegDate.get(rs.getDate(PERMIS_TRAVAIL_IND_DATE_FIN));
		row.typePermisTrInd = rs.getString(PERMIS_TRAVAIL_IND_TYPE);
		row.identificationCh = rs.getString(INDIVIDU_IDENTIFIANT_CH);
		row.nomInd = rs.getString(NOM_INDIVIDU);
		row.prenomInd = rs.getString(PRENOM_INDIVIDU);
		row.noIndividuEnfant = rs.getLong(INDIVIDU_NO_IND_ENFANT);
		row.noSeqEtCivil = rs.getInt(NO_SEQUENCE_ETAT_CIVIL_IND);
		row.dateValiditeEtCivil = RegDate.get(rs.getDate(ETAT_CIVIL_IND_DATE_DEBUT));
		row.dateInvalidMariage = RegDate.get(rs.getDate(ETAT_CIVIL_IND_DATE_INV_MAR));
		row.codeEtCivil = rs.getString(ETAT_CIVIL_IND_CODE);
		row.marieSeul = rs.getString(ETAT_CIVIL_IND_MARIE_SEUL);
		row.noIndConjoint = rs.getLong(ETAT_CIVIL_IND_CONJOINT);
		// row.noSeqAdrInd = rs.getInt(NO_SEQUENCE_ADRESSE_IND);
		// row.dateDebutAdrInd = RegDate.get(rs.getDate(ADRESSE_IND_DAD_VALIDITE));
		// row.dateFinAdrInd = RegDate.get(rs.getDate(ADRESSE_IND_DAF_VALIDITE));
		// row.chezInd = rs.getString(ADRESSE_IND_CHEZ);
		// row.nomRueInd = rs.getString(ADRESSE_IND_NOM_RUE);
		// row.noPoliceInd = rs.getString(ADRESSE_IND_NO_POLICE);
		// row.lieuInd = rs.getString(ADRESSE_IND_LIEU);
		// row.idLocPostInd = rs.getInt(ADRESSE_IND_ID_LOC_POSTALE);
		// row.idPaysInd = rs.getInt(ADRESSE_IND_ID_PAYS);
		// row.idRueInd = rs.getInt(ADRESSE_IND_ID_RUE);
		// row.designRueOfsInd = rs.getString(ADRESSE_IND_DESIGN_RUE_OFS);
		// row.idLocPosRueOfsInd = rs.getInt(ADRESSE_IND_ID_LOCPOS_RUE_OFS);

		return row;
	}



	@SuppressWarnings("unchecked")
	private int migrateSourcierSansConjoint() throws Exception {

		int ret = 0;

		// Récupère la liste des sourciers à traiter

		TransactionTemplate template = new TransactionTemplate(helper.transactionManager);
		final List<SourcierLu> sourciers = (List<SourcierLu>) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				try {
					return buildUniciteSourcier(true);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});

		// Exécute la migration par batchs, avec reprise automatique en cas d'erreur

		final BatchTransactionTemplate batchTemplate = new BatchTransactionTemplate<SourcierLu>(sourciers, 100,
				BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE, helper.transactionManager, null, helper.hibernateTemplate);
		batchTemplate.execute(new BatchTransactionTemplate.BatchCallback<SourcierLu>() {

			private Long noSourcierUnique;
			private List<SourcierLu> b;

			@Override
			public void beforeTransaction() {
				noSourcierUnique = null;
				b = null;
			}

			@Override
			public boolean doInTransaction(List<SourcierLu> batch) throws Exception {
				b = batch;
				if (batch.size() == 1) {
					// on est en reprise
					noSourcierUnique = batch.get(0).noSourcier;
				}

				for (SourcierLu sourcier : batch) {
					if (findSourcier(sourcier) == null) {
						// recherche de doublon
						if (sourcierHelper.isDoublon(sourcier.noIndividu)) {
							DOUBLON.error(sourcier.noSourcier + ";" + sourcier.noIndividu);
						}
						else {
							final PersonnePhysique personneSourcier = buildAndSaveOneSourcier(sourcier);
							completerInformationsSourciers(personneSourcier, true);
							RAPPORT.info(getInfosSourcier(personneSourcier));
							nbCtbMigrated.valeur++;
						}

					}
				}

				for (SourcierLu s : b) {
					helper.migrationErrorDAO.removeForContribuable(s.noSourcier);
				}

				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				String message = "===> Rollback du batch [" + b.get(0).noSourcier + "-" + b.get(b.size() - 1).noSourcier + "] willRetry="
						+ willRetry;
				LOGGER.warn(message);
				if (!willRetry) {
					Assert.notNull(noSourcierUnique);
					LOGGER.error("Impossible de migrer le sourcier n°" + noSourcierUnique, e);

					MigrationError er = helper.migrationErrorDAO.getErrorForContribuable(noSourcierUnique);
					if (er == null) {
						er = new MigrationError();
						er.setNoContribuable(noSourcierUnique);
					}
					er.setMessage(e.getMessage());
					helper.migrationErrorDAO.saveObject(er);
				}
			}
		});

		return  nbCtbMigrated.valeur;
	}

	@SuppressWarnings("unchecked")
	private int migrateSourcierEnCouple() throws Exception {

		int ret = 0;

		// Récupère la liste des sourciers à traiter

		TransactionTemplate template = new TransactionTemplate(helper.transactionManager);
		final List<SourcierLu> sourciers = (List<SourcierLu>) template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				try {
					return buildUniciteSourcier(false);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});

		// Exécute la migration par batchs, avec reprise automatique en cas d'erreur

		final BatchTransactionTemplate batchTemplate = new BatchTransactionTemplate<SourcierLu>(sourciers, 100,
				BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE, helper.transactionManager, null, helper.hibernateTemplate);
		batchTemplate.execute(new BatchTransactionTemplate.BatchCallback<SourcierLu>() {

			private Long noSourcierUnique;
			private int nbSourciers;
			private List<SourcierLu> b;

			@Override
			public void beforeTransaction() {
				noSourcierUnique = null;
				nbSourciers = 0;
			}

			@Override
			public boolean doInTransaction(List<SourcierLu> batch) throws Exception {
				b = batch;
				nbSourciers = batch.size();
				if (nbSourciers == 1) {
					// on est en reprise
					noSourcierUnique = batch.get(0).noSourcier;
				}

				 nbCtbMigrated.valeur =  processBatchSourcierEnCouple(batch, status);

				for (SourcierLu s : b) {
					helper.migrationErrorDAO.removeForContribuable(s.noSourcier);
				}
				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				String message = "===> Rollback du batch [" + b.get(0).noSourcier + "-" + b.get(b.size() - 1).noSourcier + "] willRetry="
						+ willRetry;
				LOGGER.warn(message);
				if (!willRetry) {
					Assert.notNull(noSourcierUnique);
					LOGGER.error("Impossible de migrer le sourcier n°" + noSourcierUnique, e);
					REJET.info(noSourcierUnique + ";" + e.getMessage() + ";BLOQUANTE");
					MigrationError er = helper.migrationErrorDAO.getErrorForContribuable(noSourcierUnique);
					if (er == null) {
						er = new MigrationError();
						er.setNoContribuable(noSourcierUnique);
					}
					er.setMessage(e.getMessage());
					helper.migrationErrorDAO.saveObject(er);
				}
			}
		});

		return  nbCtbMigrated.valeur;
	}

	private int processBatchSourcierEnCouple(List<SourcierLu> sourciers, StatusManager status) throws Exception {
		int ret = 0;
		for (SourcierLu sourcier : sourciers) {
			if (status.interrupted()) {
				break;
			}
			if (findSourcier(sourcier) == null) {
				ret = ret + processSourcierEnCouple(sourcier);
			}
		}
		return ret;
	}

	private int processSourcierEnCouple(SourcierLu sourcierCourant) throws Exception {
		int ret = 0;

		if (sourcierHelper.isDoublon(sourcierCourant.noIndividu)) {
			DOUBLON.error(sourcierCourant.noSourcier + ";" + sourcierCourant.noIndividu);
		}
		else {
			final PersonnePhysique sourcierPrincipal = buildAndSaveOneSourcier(sourcierCourant);
			//
			completerInformationsSourciers(sourcierPrincipal, true);
			RAPPORT.info(getInfosSourcier(sourcierPrincipal));
			ret++;
			// On recupere les differents conjoints du sourcier
			// Pour chaque conjoint, on migre ses informations sourciers et ses fors eventuels.

			final PersonnePhysique sourcierConjoint = sourcierHelper.getConjointSourcier(sourcierPrincipal);
			if (sourcierConjoint != null && sourcierConjoint.getAncienNumeroSourcier() != null) {

				if (sourcierHelper.isDoublon(sourcierConjoint.getNumeroIndividu())) {
					DOUBLON.error(sourcierConjoint.getAncienNumeroSourcier() + ";" + sourcierConjoint.getNumeroIndividu());
				}
				else {
					completerInformationsSourciers(sourcierConjoint, false);
					RAPPORT.info(getInfosSourcier(sourcierConjoint));
					ret++;
				}
			}

		}


		//completerInformationsSourciers(sourcierConjoint, false);
		//RAPPORT.info(getInfosSourcier(sourcierConjoint));


		// A la recherche du conjoint dans le Host
		//SourcierLu sourcierCourantConjoint = findConjoint(sourcierCourant.noSourcier);
		/*if (sourcierCourantConjoint != null) {

			PersonnePhysique sourcierConjoint = null;
			try {
				sourcierConjoint = buildAndSaveOneSourcier(sourcierCourantConjoint);
			}
			catch (Exception e) {
				String message = "Erreur lors de la création du conjoint du sourcier numero " + sourcierCourant.noSourcier + ":"
						+ e.getMessage();
				throw new ValidationException(sourcierPrincipal, message);
			}



			if (sourcierConjoint != null) {
				completerInformationsSourciers(sourcierConjoint, false);
				RAPPORT.info(getInfosSourcier(sourcierConjoint));
			}


		}*/



		return ret;
	}

	private Tiers buildSourcierPersonnePhysique(SourcierLu sl) throws Exception {
		PersonnePhysique sourcier = null;
		if (sl.noIndividu == null || sl.noIndividu == 0) {
			sourcier = new PersonnePhysique(false);
		}
		else {
			sourcier = new PersonnePhysique(true);
			sourcier.setNumeroIndividu(sl.noIndividu);
		}


		String numeroAvs = sl.noAvs;
		sourcier.setAncienNumeroSourcier(sl.noSourcier);

		// sourcier.setNumeroAssureSocial(numeroAvs);
		sourcier.setNom(sl.nom);
		sourcier.setPrenom(sl.prenom);
		sourcier.setDateNaissance(sl.dateNaissance);
		sourcier.setSexe(sl.sexe.equals("M") ? Sexe.MASCULIN : Sexe.FEMININ);
		sourcier.setCategorieEtranger(getCategorieEtranger(sl.typePermis));
		String messageNumRefCantonal = "Numéro de référence cantonal: " + sl.numeroReferenceCantonal;
		// TODO(BNM)verifier le type de separateur a mettre
		if (sl.remarque == null) {
			sl.remarque = "";
		}
		String remarque = sl.remarque + "ANCIEN NUMERO SOURCIER: " + sl.noSourcier + " " + messageNumRefCantonal;
		sourcier.setRemarque(remarque);

		IdentificationPersonne identifiantAvs = new IdentificationPersonne();
		identifiantAvs.setCategorieIdentifiant(CategorieIdentifiant.CH_AHV_AVS);
		identifiantAvs.setIdentifiant(numeroAvs);

		IdentificationPersonne identifiantEtranger = new IdentificationPersonne();
		identifiantEtranger.setCategorieIdentifiant(CategorieIdentifiant.CH_ZAR_RCE);
		identifiantEtranger.setIdentifiant(sl.noRegistreEtranger);

		HashSet<IdentificationPersonne> identifiantsPersonnes = new HashSet<IdentificationPersonne>();
		identifiantsPersonnes.add(identifiantAvs);
		identifiantsPersonnes.add(identifiantEtranger);
		sourcier.setIdentificationsPersonnes(identifiantsPersonnes);
		sourcier = (PersonnePhysique) helper.tiersDAO.save(sourcier);

		return sourcier;
	}

	private void buildSituationFamille(PersonnePhysique sourcier, List<SourcierSituationFamille> situationAcreer) throws Exception {

		// si la personne est au role ordinaire, la situation de famille du civil est prioritaire
		if (sourcierHelper.findRoleOrdinaire(sourcier) == null) {
			for (SourcierSituationFamille sourcierSituationFamille : situationAcreer) {
				SituationFamilleMenageCommun situationFamille = new SituationFamilleMenageCommun();

				RegDate dateDebutSituation = sourcierSituationFamille.dateDebut;
				if (dateDebutSituation.equals(RegDate.getEarlyDate())) {
					RegDate dateNaissance = sourcier.getDateNaissance();
					if (dateNaissance!=null) {
						dateDebutSituation = dateNaissance;
					}

				}
				situationFamille.setDateDebut(dateDebutSituation);
				situationFamille.setDateFin(sourcierSituationFamille.dateFin);
				situationFamille.setEtatCivil(sourcierSituationFamille.etatCivil);
				situationFamille.setNombreEnfants(sourcierSituationFamille.nbEnfant);
				situationFamille.setContribuablePrincipal(sourcier);

				List<SituationFamilleMenageCommun> listeSituationARajouter = readAndAddTarifApplicable(situationFamille, sourcier);
				Contribuable contribuableCible = sourcier;

				if (EtatCivil.MARIE.equals(sourcierSituationFamille.etatCivil)) {

					if ("O".equals(sourcierSituationFamille.chargeFamille)) {
						MenageCommun menage = helper.tiersService.findMenageCommun(sourcier, dateDebutSituation);
						if (menage != null) {
							for (SituationFamilleMenageCommun situationFamilleMenageCommun : listeSituationARajouter) {
								if (!sourcierHelper.situtionIsPresent(situationFamilleMenageCommun, menage)) {
									menage.addSituationFamille(situationFamilleMenageCommun);
								}


							}
						}

					}
				}
				else {
					for (SituationFamilleMenageCommun situationFamilleMenageCommun : listeSituationARajouter) {
						if (!sourcierHelper.situtionIsPresent(situationFamilleMenageCommun, sourcier)) {
							sourcier.addSituationFamille(situationFamilleMenageCommun);
						}


					}
				}

			}

		}
	}

	private List<SituationFamilleMenageCommun> readAndAddTarifApplicable(SituationFamilleMenageCommun situationFamille, PersonnePhysique sourcier) throws Exception {
		List<SourcierBareme> listeBareme = readBaremeSource(situationFamille, sourcier);

		List<SituationFamilleMenageCommun> listeResultat = new ArrayList<SituationFamilleMenageCommun>();

		if (listeBareme.size() == 1) {
			SourcierBareme baremeUnique = listeBareme.get(0);
			SituationFamilleMenageCommun situationBareme = new SituationFamilleMenageCommun(situationFamille);
			situationBareme.setTarifApplicable(translateBareme(baremeUnique.codeBareme));
			listeResultat.add(situationBareme);
			//verifyNbEnfants(sourcier, situationFamille, baremeUnique.codeBareme);
		}
		else {

			for (SourcierBareme sourcierBareme : listeBareme) {
				SituationFamilleMenageCommun situationBareme = new SituationFamilleMenageCommun(situationFamille);
				situationBareme.setDateDebut(sourcierBareme.dateDebut);
				situationBareme.setDateFin(sourcierBareme.dateFin);
				situationBareme.setTarifApplicable(translateBareme(sourcierBareme.codeBareme));
				listeResultat.add(situationBareme);
				//verifyNbEnfants(sourcier, situationFamille, sourcierBareme.codeBareme);

			}
		}



		return listeResultat;
	}

private void verifyNbEnfants(PersonnePhysique sourcier,SituationFamilleMenageCommun situation, String bareme){
	int nombreEnfantsSituation = situation.getNombreEnfants();
	int nbEnfantOnBareme = getNbEnfantOnBareme(bareme);
	if (nombreEnfantsSituation != nbEnfantOnBareme ) {
		REJET.info(sourcier.getAncienNumeroSourcier()+";"+"le nombre d'enfant de la situation de famille ("+nombreEnfantsSituation+") est different du nombre d'enfant  sur le bareme("+nbEnfantOnBareme+")");
	}
}

	private List<PersonnePhysique> readAllSourciersMigrated() {
		return helper.tiersService.getAllMigratedSourciers();
	}

	// private void loadSourciers() throws Exception {
	//
	// StringBuilder listSourciersIndividu = new StringBuilder();
	// String query = readSourcierWithoutIndividu(readSourcierToMigrate());
	// Statement stmt = helper.isConnection.createStatement();
	// try {
	// ResultSet rs = stmt.executeQuery(query);
	// try {
	// while (rs.next()) {
	// if (rs.getLong(NUMERO_INDIVIDU) > 0) {
	// lstSourciers.add(buildRowRsSrc(rs));
	// } else {
	// lstSourciersGris.add(buildRowRsSrc(rs));
	// }
	// }
	// loadSourcierWithIndividu();
	// }
	// finally {
	// rs.close();
	// }
	// }
	// finally {
	// stmt.close();
	// }
	// }





	private SourcierTypeImposition findTypeImposition(Long noSourcierHost, RegDate date) throws Exception {

		Statement stmtTypeImposition = helper.isConnection.createStatement();
		ResultSet rsTypeImposition = stmtTypeImposition.executeQuery(readTypeImposition(noSourcierHost, date));
		SourcierTypeImposition typeImposition = null;
		try {
			while (rsTypeImposition.next()) {
				typeImposition = new SourcierTypeImposition();
				typeImposition.typeImposition = rsTypeImposition.getString(TYPE_IMPOSITION);
				// on veut le premier mode
				break;
			}
		}
		finally {
			rsTypeImposition.close();
			stmtTypeImposition.close();
		}
		return typeImposition;
	}

	private PersonnePhysique findSourcier(SourcierLu sourcierLu) throws Exception{
		//(PersonnePhysique) helper.tiersService.getTiers(getNumeroContribuableSourcier(sourcierLu.noSourcier));
		PersonnePhysique sourcier = null;
		List<PersonnePhysique> listeSourciers = helper.tiersService.getSourciers(sourcierLu.noSourcier.intValue());
		if (listeSourciers!=null && !listeSourciers.isEmpty()) {
			sourcier = listeSourciers.get(0);
		}
		return sourcier;
	}








	private PersonnePhysique buildAndSaveOneSourcier(SourcierLu sourcierCourant) throws Exception {

		Long noSourcierHost = sourcierCourant.noSourcier;
		Long noIndividu = sourcierCourant.noIndividu;
		PersonnePhysique sourcier = null;
		String remarqueExistante = "";
		if (sourcierCourant.remarque != null) {
			remarqueExistante = sourcierCourant.remarque;
		}
		String remarque = remarqueExistante + " " + "ANCIEN NUMERO SOURCIER: " + noSourcierHost;

		sourcier = helper.tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
		// On traite la remarque d'un sourcier connu du civil
		if (sourcier != null) {
			if (sourcier.getRemarque() != null) {
				remarque = sourcier.getRemarque() + " " + remarque;
			}
			sourcier.setRemarque(remarque);
		}
		else {

			sourcier = (PersonnePhysique) buildSourcierPersonnePhysique(sourcierCourant);
			// sourcier.setNumeroIndividu(noIndividu);

		}

		sourcier.setAncienNumeroSourcier(noSourcierHost);
		sourcier = (PersonnePhysique) saveTiers(sourcier, noSourcierHost);
		buildRapportDePrestationImposable(sourcier, noSourcierHost);


		return sourcier;

	}




	private boolean isDateFinOrDateDepartValide(SourcierAdresseFiscale adresse) {
		boolean isValid = true;
		if (adresse.dateFin == null) {
			if (adresse.dateDepartEtranger != null) {
				if (adresse.dateDepartEtranger.isBefore(RegDate.get(2008, 1, 1))) {
					isValid = false;
				}

			}
		}
		else if (adresse.dateFin.isBefore(RegDate.get(2008, 1, 1))) {

			isValid = false;

		}

		return isValid;
	}

	private ArrayList<SourcierAdresseFiscale> buildAdresseValides(ResultSet rs, Long noSourcierHost) throws Exception {

		ArrayList<SourcierAdresseFiscale> adresses = new ArrayList<SourcierAdresseFiscale>();
		while (rs.next()) {
			SourcierAdresseFiscale adresse = new SourcierAdresseFiscale();
			adresse.dateDebut = RegDate.get(rs.getDate(ADR_FISCALE_DATE_DEBUT));
			adresse.dateFin = RegDate.get(rs.getDate(ADR_FISCALE_DATE_FIN));
			adresse.communeFiscale = rs.getInt(ADR_FISCALE_NO_TECH_COMMUNE);
			adresse.dateDepartEtranger = RegDate.get(rs.getDate(ADR_FISCALE_DATE_DEPART));
			adresse.dateRetourEtranger = RegDate.get(rs.getDate(ADR_FISCALE_DATE_RETOUR));
			adresses.add(adresse);

		}
		ArrayList<SourcierAdresseFiscale> adressesValides = new ArrayList<SourcierAdresseFiscale>();
		// important pour la suite de l'algorythme
		Collections.sort(adresses, Collections.reverseOrder());
		if (!adresses.isEmpty()) {

			// on recupere la derniere adresse fiscale valide
			SourcierAdresseFiscale derniereAdresse = adresses.get(0);
			if (derniereAdresse != null) {

				if (!isDateFinOrDateDepartValide(derniereAdresse)) {
					LOGGER.debug("Le sourcier " + noSourcierHost + " n'a aucune adresse fiscale active après le 01.01.2008");
				}
				else {
					// Si c'est une adresse qui a eu au moins un jour de validité après le 01.01.2008
					if (derniereAdresse.dateFin != null || derniereAdresse.dateDepartEtranger!=null) {
						derniereAdresse.motifFermeture = MotifFor.DEPART_HS;
					}

					// On recupere l'adresse precedente si elle existe afin de calculer
					// le motif d'ouverture de la dernière adresse
					if (adresses.size() > 1) {
						SourcierAdresseFiscale adressePrecedente = adresses.get(1);
						if (derniereAdresse.dateDebut.getOneDayBefore().equals(adressePrecedente.dateFin)) {
							derniereAdresse.motifOuverture = MotifFor.DEMENAGEMENT_VD;
							adressesValides.add(derniereAdresse);
							// On recupere les adresses precedentes accollées
							for (int i = 1; i < adresses.size(); i++) {
								SourcierAdresseFiscale adresseCourante = adresses.get(i);
								adresseCourante.motifFermeture = MotifFor.DEMENAGEMENT_VD;
								int precedent = i + 1;
								if (precedent < adresses.size()) {
									SourcierAdresseFiscale aPrec = adresses.get(precedent);
									if (adresseCourante.dateDebut.getOneDayBefore().equals(aPrec.dateFin)
											&& aPrec.dateDepartEtranger == null && aPrec.dateRetourEtranger == null) {
										adresseCourante.motifOuverture = MotifFor.DEMENAGEMENT_VD;
										adressesValides.add(adresseCourante);

									}
									else {
										// plus d'adresses accollées, on s'arrète
										adresseCourante.motifOuverture = MotifFor.ARRIVEE_HS;
										adressesValides.add(adresseCourante);
										break;
									}
								}
								else {
									// Premiére adresse dans le canton
									adresseCourante.motifOuverture = MotifFor.ARRIVEE_HS;
									adressesValides.add(adresseCourante);
									break;
								}

							}
						}
						else {
							derniereAdresse.motifOuverture = MotifFor.ARRIVEE_HS;
							adressesValides.add(derniereAdresse);
						}

					}
					else {
						derniereAdresse.motifOuverture = MotifFor.ARRIVEE_HS;
						adressesValides.add(derniereAdresse);
					}

				}
			}
		}
		Collections.sort(adressesValides);
		if (adressesValides.isEmpty()) {
			// TODO Peut faul il lever une erreur ? à demander à TD
			REJET.info("Le sourcier " + noSourcierHost + " n'a aucune adresse fiscale active aprés le 01.01.2008");
			return null;
		}

		return adressesValides;

	}





	private ModeImposition getAssujettissement(Contribuable sourcier, RegDate date) {
		String typeImposition = null;
		for (SourcierTypeImposition srcTypeImposition : lstTyImposit) {
			typeImposition = srcTypeImposition.typeImposition;
			if (srcTypeImposition.dateDebut.isBeforeOrEqual(date)) {
				break;
			}
		}

		ModeImposition mi = ModeImposition.SOURCE;
		if (sourcier.getSituationFamilleAt(date).getEtatCivil().equals(EtatCivil.MARIE)
				|| sourcier.getSituationFamilleAt(date).getEtatCivil().equals(EtatCivil.LIE_PARTENARIAT_ENREGISTRE)) {
			if (typeImposition.equals("M")) {
				mi = ModeImposition.MIXTE_137_1;
			}
		}
		else {
			if (typeImposition.equals("M")) {
				mi = ModeImposition.MIXTE_137_1;
			}
		}

		return mi;
	}







	/**
	 *
	 * @param sourcier
	 * @param isPrincipal
	 *            TODO
	 * @throws Exception
	 */
	private void completerInformationsSourciers(PersonnePhysique sourcier, boolean isPrincipal) throws Exception {


		Canton vaud = helper.serviceInfrastructureService.getVaud();

		ArrayList<SourcierAdresseFiscale> listeAdresseValides = null;
		Long noSourcierHost = sourcier.getAncienNumeroSourcier();

		Statement stmtAdresse = helper.isConnection.createStatement();
		ResultSet rsAdresse = stmtAdresse.executeQuery(readAdresseFiscalesSourcier(noSourcierHost));

		try {
			listeAdresseValides = buildAdresseValides(rsAdresse, noSourcierHost);

		}
		finally {
			rsAdresse.close();
			stmtAdresse.close();
		}
		if (listeAdresseValides != null && !listeAdresseValides.isEmpty()) {
			 final List<ForFiscalPrincipal> forByAdresse = buildAssujetissementByAdresse(noSourcierHost, listeAdresseValides,vaud);

			// Si il y a une adresse fiscal, on peut construire les autres fors
			if (forByAdresse != null && !forByAdresse.isEmpty()) {

				RegDate dateEntreeCanton = forByAdresse.get(0).getDateDebut();
				List<SourcierSituationFamille> listeSituation = readSituationsFamilleOneSourcier(noSourcierHost.intValue(),
						dateEntreeCanton);
				final List<ForFiscalPrincipal> forByEtatCivil = buildInformationFromEtatCivil(sourcier, noSourcierHost, dateEntreeCanton, listeAdresseValides,
						listeSituation, isPrincipal, vaud);
				final List<ForFiscalPrincipal> forByTypeImposition = buildAssujetissementByTypeImposition(sourcier, noSourcierHost, dateEntreeCanton, listeAdresseValides, vaud);
				List<ForFiscalPrincipal> forCalcules = sourcierHelper.removeForSameDate(forByAdresse, forByEtatCivil, forByTypeImposition);
				forCalcules = sourcierHelper.adaptListFor(forCalcules);

				// LOGGER.info("Création des fors pour le souricier "+sourcier.getAncienNumeroSourcier()+" isPrincipal " +isPrincipal);
				// Création des periodes de mariages
				// Elles seront utilisées dans mergeSaveFor afin de s'assurer que les fors soient associés
				// au bon contribuable: menage ou personne physique
				List<DateRange> listePeriodeCouple = buildPeriodeCouple(listeSituation);

				sourcierHelper.mergeAndSaveFor(sourcier, forCalcules, isPrincipal, listePeriodeCouple);
			}
		}

	}

	private List<DateRange> buildPeriodeCouple(List<SourcierSituationFamille> listeSituation) {
		List<DateRange> listePeriodeCouple = new ArrayList<DateRange>();
		if (listeSituation != null && !listeSituation.isEmpty()) {
			for (SourcierSituationFamille sourcierSituationFamille : listeSituation) {
				if (EtatCivil.MARIE.equals(sourcierSituationFamille.etatCivil)) {
					Range periodeMariage = new Range(sourcierSituationFamille.dateDebut, sourcierSituationFamille.dateFin);
					listePeriodeCouple.add(periodeMariage);
				}
			}
			listePeriodeCouple =  DateRangeHelper.collateRange(listePeriodeCouple);
		}

		return listePeriodeCouple;
	}
	private List<ForFiscalPrincipal> buildAssujetissementByAdresse(Long noSourcierHost,
			ArrayList<SourcierAdresseFiscale> listeAdresseValides,Canton vaud) throws Exception {
		List<ForFiscalPrincipal> listFor = new ArrayList<ForFiscalPrincipal>();

		for (SourcierAdresseFiscale sa : listeAdresseValides) {
			String typeImposition = null;
			SourcierTypeImposition typeImpot = findTypeImposition(noSourcierHost, sa.dateDebut);
			if (typeImpot == null) {
				typeImposition = "S";
			}
			else {
				typeImposition = findTypeImposition(noSourcierHost, sa.dateDebut).typeImposition;
			}

			ModeImposition modeImposition = getModeImposition(typeImposition);
			ForFiscalPrincipal ffp = new ForFiscalPrincipal();
			ffp.setMotifRattachement(MotifRattachement.DOMICILE);
			ffp.setDateDebut(sa.dateDebut);
			ffp.setDateFin(sa.dateFin);
			if (ffp.getDateFin()==null && sa.dateDepartEtranger!=null) {
				ffp.setDateFin(sa.dateDepartEtranger);
			}
			ffp.setMotifOuverture(sa.motifOuverture);
			ffp.setMotifFermeture(sa.motifFermeture);
			ffp.setModeImposition(modeImposition);
			ffp.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			ffp.setTypeAutoriteFiscale(determineAutoriteFiscal(vaud, sa.communeFiscale));
			ffp.setNumeroOfsAutoriteFiscale(sa.communeFiscale);
			listFor.add(ffp);

		}

		return listFor;
	}

	private TypeAutoriteFiscale determineAutoriteFiscal(Canton vaud, int numeroCommuneResidence) throws InfrastructureException {
		Canton cantonByCommune = helper.serviceInfrastructureService.getCantonByCommune(numeroCommuneResidence);

		if (cantonByCommune!=null && vaud.getSigleOFS().equals(cantonByCommune.getSigleOFS())) {
			return TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
		}
		else if(cantonByCommune!=null && !vaud.getSigleOFS().equals(cantonByCommune.getSigleOFS()))  {
			return TypeAutoriteFiscale.COMMUNE_HC;
		}

		return null;
	}



	private List<ForFiscalPrincipal> buildInformationFromEtatCivil(PersonnePhysique sourcier, Long noSourcierHost,
			RegDate dateArrivee, ArrayList<SourcierAdresseFiscale> listeAdresseValides, List<SourcierSituationFamille> listeSituation, boolean isPrincipal, Canton vaud)
			throws Exception {

		//List<SourcierSituationFamille> listeSituation = readSituationsFamilleOneSourcier(noSourcierHost.intValue(), dateArrivee);

		List<ForFiscalPrincipal> listeFor = new ArrayList<ForFiscalPrincipal>();
		if (listeSituation != null && !listeSituation.isEmpty()) {

			// on crée le ménage commun à partir des situations de famille trouvées si le c'est le sourcier principal
			// qui est traité
			if (isPrincipal) {
				// ON verifie que le sourcier n'est pas au role ordinaire, si c'est le cas on evite de créer un ménage commun
				// qui risque de rentrer en conflit avec les informations du role odinaire, notamment sur la validation des fors
				if (sourcierHelper.findRoleOrdinaire(sourcier) == null) {

					sourcier = checkOrCreateCoupleSourcier(sourcier, listeSituation);

				}
				buildSituationFamille(sourcier, listeSituation);
			}



			for (int i = 0; i < listeSituation.size(); i++) {

				SourcierSituationFamille situation = listeSituation.get(i);

				// On ne créée les fors qu'après la date d'arrivée dans le canton
				if (dateArrivee.isBeforeOrEqual(situation.dateDebut)) {
					String typeImposition = null;
					SourcierTypeImposition typeImpot = findTypeImposition(noSourcierHost, situation.dateDebut);
					if (typeImpot == null) {
						typeImposition = "S";
					}
					else {
						typeImposition = findTypeImposition(noSourcierHost, situation.dateDebut).typeImposition;
					}
					final int numeroCommuneResidence = findCommuneResidence(situation.dateDebut, listeAdresseValides);
					ModeImposition modeImposition = getModeImposition(typeImposition);
					ForFiscalPrincipal forSituation = new ForFiscalPrincipal();
					forSituation.setMotifRattachement(MotifRattachement.DOMICILE);
					forSituation.setMotifOuverture(situation.motifOuverture);
					forSituation.setDateDebut(situation.dateDebut);
					forSituation.setModeImposition(modeImposition);
					forSituation.setTypeAutoriteFiscale(determineAutoriteFiscal(vaud, numeroCommuneResidence));

					forSituation.setNumeroOfsAutoriteFiscale(numeroCommuneResidence);
					// Dans le cas ou il y a plusieurs situation de famille qui se suivent
					// on ferme la courante avec la date de début de la suivante. le motif de fermeture est le
					// motif d'ouverture de la situation suivante.
					if (i + 1 < listeSituation.size()) {
						SourcierSituationFamille situationSuivante = listeSituation.get(i + 1);
						forSituation.setDateFin(situationSuivante.dateDebut.getOneDayBefore());
						forSituation.setMotifFermeture(situationSuivante.motifOuverture);

					}

					// afin de guarantir que le premier for est celui qui est arrivéé hors canton les for a enregistrer
					// doivent avoir une date de debut > à la date d'arrivée
					if (forSituation.getDateDebut().isAfter(dateArrivee)) {
						listeFor.add(forSituation);
					}
				}

			}

		}
		return listeFor;
	}

	private List<ForFiscalPrincipal> buildAssujetissementByTypeImposition(PersonnePhysique sourcier, Long noSourcierHost,
			RegDate dateOuverture, ArrayList<SourcierAdresseFiscale> listeAdresseValides, Canton vaud) throws Exception {
		List<SourcierTypeImposition> listeTypeImposition = readAllTypeImposition(noSourcierHost.intValue(), dateOuverture);
		List<ForFiscalPrincipal> listeFor = new ArrayList<ForFiscalPrincipal>();
		if (listeTypeImposition != null && !listeTypeImposition.isEmpty()) {
			for (int i = 0; i < listeTypeImposition.size(); i++) {
				SourcierTypeImposition typeImposition = listeTypeImposition.get(i);

				final int numeroCommuneResidence = findCommuneResidence(typeImposition.dateDebut, listeAdresseValides);

				ModeImposition modeImposition = getModeImposition(typeImposition.typeImposition);
				ForFiscalPrincipal forTypeImposition = new ForFiscalPrincipal();
				forTypeImposition.setMotifRattachement(MotifRattachement.DOMICILE);
				forTypeImposition.setMotifOuverture(typeImposition.motifOuverture);
				forTypeImposition.setDateDebut(typeImposition.dateDebut);
				forTypeImposition.setModeImposition(modeImposition);
				forTypeImposition.setTypeAutoriteFiscale(determineAutoriteFiscal(vaud, numeroCommuneResidence));
				forTypeImposition.setNumeroOfsAutoriteFiscale(numeroCommuneResidence);
				// Dans le cas ou il y a plusieurs type d'imposition qui se suivent
				// on ferme le courant avec la date de début du suivant. le motif de fermeture est le
				// motif d'ouverture du type suivant.
				if (i + 1 < listeTypeImposition.size()) {
					SourcierTypeImposition typeImpositionSuivante = listeTypeImposition.get(i + 1);
					forTypeImposition.setDateFin(typeImpositionSuivante.dateDebut.getOneDayBefore());
					forTypeImposition.setMotifFermeture(typeImpositionSuivante.motifOuverture);

				}

				listeFor.add(forTypeImposition);

			}

		}
		return listeFor;
	}

	private PersonnePhysique checkOrCreateCoupleSourcier(PersonnePhysique sourcierCourant, List<SourcierSituationFamille> listeSituation)
			throws Exception {

		// Recherche d'un menage commun si non trouvé, on le crée

		for (SourcierSituationFamille sourcierSituationFamille : listeSituation) {
			// on ne créé que les menageCommun si le sourcier en est le charge de famille
			if ((EtatCivil.MARIE.equals(sourcierSituationFamille.etatCivil))) {
				if ("O".equals(sourcierSituationFamille.chargeFamille)) {
					// Dans le cas de couple toujours marié on crée à la fois le menage commun et le sourcier associé
					// s'il n'existe pas

					if (sourcierSituationFamille.dateFin == null) {
						PersonnePhysique sourcierConjoint = null;
						SourcierLu conjoint = findConjoint(sourcierCourant.getAncienNumeroSourcier());
						if (conjoint != null) {
							sourcierConjoint = buildAndSaveOneSourcier(conjoint);
						}else{
							//Recherche du conjoint dans Unireg
							sourcierConjoint = sourcierHelper.getConjointSourcier(sourcierCourant);
							if (sourcierConjoint!=null && sourcierConjoint.getNumeroIndividu() != null ) {
								SourcierLu infoConjoint = findSourcierByNumeroIndividu(sourcierConjoint.getNumeroIndividu());
								//Mis à jour des informations Sourcier du conjoint.
								if (infoConjoint!=null) {
									sourcierConjoint = buildAndSaveOneSourcier(infoConjoint);
								}

							}
						}

						MenageCommun menage = sourcierHelper.findDernierMenageCommunActif(sourcierCourant);
						if (menage == null) {
							// Création du ménage et de la relation avec le sourcierPrincipal
							menage = new MenageCommun();
							menage.setRemarque(sourcierCourant.getRemarque());
							RapportEntreTiers rapportPrincipal = helper.tiersService.addTiersToCouple(menage, sourcierCourant,
									sourcierSituationFamille.dateDebut, sourcierSituationFamille.dateFin);
							menage = (MenageCommun) rapportPrincipal.getObjet();
							sourcierCourant = (PersonnePhysique) rapportPrincipal.getSujet();

							// Création de la relation avec le conjoint s'il n'est pas null
							if (sourcierConjoint != null) {
								RapportEntreTiers rapportConjoint = helper.tiersService.addTiersToCouple(menage, sourcierConjoint,
										sourcierSituationFamille.dateDebut, sourcierSituationFamille.dateFin);
								menage = (MenageCommun) rapportConjoint.getObjet();
								sourcierConjoint = (PersonnePhysique) rapportConjoint.getSujet();
							}

						}else{
							String remarque = menage.getRemarque();
							if (remarque==null) {
								menage.setRemarque(sourcierCourant.getRemarque());
							}else{
								menage.setRemarque(remarque+" "+sourcierCourant.getRemarque());
							}
						}
					}
					else {


							MenageCommun menage = helper.tiersService.findMenageCommun(sourcierCourant, sourcierSituationFamille.dateFin
									.getOneDayBefore());
							if (menage == null) {
								// Création du ménage et de la relation avec le sourcierPrincipal
								menage = new MenageCommun();
								menage.setRemarque(sourcierCourant.getRemarque());
								RapportEntreTiers rapportPrincipal = helper.tiersService.addTiersToCouple(menage, sourcierCourant,
										sourcierSituationFamille.dateDebut, sourcierSituationFamille.dateFin);
								menage = (MenageCommun) rapportPrincipal.getObjet();
								sourcierCourant = (PersonnePhysique) rapportPrincipal.getSujet();
								// recherche d'un conjoint
								SourcierLu ancienConjoint = findAncienConjoint(sourcierCourant.getAncienNumeroSourcier(),
										sourcierSituationFamille.dateDebut);
								PersonnePhysique sourcierAncienConjoint = null;
								if (ancienConjoint != null) {
									// On recherche le sourcier conjoint dans unireg
									sourcierAncienConjoint = findSourcier(ancienConjoint);
								}
								// Création de la relation avec le conjoint s'il n'est pas null
								if (sourcierAncienConjoint != null) {

									RapportEntreTiers rapportConjoint = helper.tiersService.addTiersToCouple(menage,
											sourcierAncienConjoint, sourcierSituationFamille.dateDebut, sourcierSituationFamille.dateFin);
									menage = (MenageCommun) rapportConjoint.getObjet();
									sourcierAncienConjoint = (PersonnePhysique) rapportConjoint.getSujet();

								}
							}
							else {
								String remarque = menage.getRemarque();
								if (remarque == null) {
									menage.setRemarque(sourcierCourant.getRemarque());
								}
								else {
									menage.setRemarque(remarque + " " + sourcierCourant.getRemarque());
								}

							}
						}
					}



			}
		}

		return sourcierCourant;

	}









	private ModeImposition getModeImposition(String typeImposition) {
		if (typeImposition.equals("M")) {
			return ModeImposition.MIXTE_137_2;
		}
		if (typeImposition.equals("S")) {
			return ModeImposition.SOURCE;
		}
		if (typeImposition.equals("O")) {
			return ModeImposition.ORDINAIRE;
		}
		return ModeImposition.SOURCE;
	}

	private CategorieEtranger getCategorieEtranger(String permis) {
		CategorieEtranger categorie = null;
		if (permis.equals("B")) {
			categorie = CategorieEtranger._02_PERMIS_SEJOUR_B;
		}
		else if (permis.equals("C")) {
			categorie = CategorieEtranger._03_ETABLI_C;
		}
		else if (permis.equals("CI")) {
			categorie = CategorieEtranger._04_CONJOINT_DIPLOMATE_CI;
		}
		else if (permis.equals("CH")) {
			categorie = null;
		}
		else if (permis.equals("F")) {
			categorie = CategorieEtranger._05_ETRANGER_ADMIS_PROVISOIREMENT_F;
		}
		else if (permis.equals("G")) {
			categorie = CategorieEtranger._06_FRONTALIER_G;
		}
		else if (permis.equals("L")) {
			categorie = CategorieEtranger._07_PERMIS_SEJOUR_COURTE_DUREE_L;
		}
		else if (permis.equals("N")) {
			categorie = CategorieEtranger._08_REQUERANT_ASILE_N;
		}

		return categorie;
	}

	private String getInfosSourcier(PersonnePhysique personne) {
		String res = "";
		res = personne.getNumero().toString() + ";";
		res += personne.getNatureTiers() + ";";
		MenageCommun menage = helper.tiersService.findMenageCommun(personne, RegDate.get());
		if (menage != null) {
			res += "En couple";
		}
		else {
			res += "Celibataire";
		}

		return res;
	}



	/**
	 * Traduction en type Unireg de l'état civil d'un sourcier. Etat civil du sourcier dans SIMPA-IS : 1 = célibataire 2 = marié 3 = veuf 4
	 * = divorcé 5 = séparé '' = non renseigné
	 *
	 * @param etatCivilSourcier
	 * @return
	 */
	private EtatCivil getEtatCivil(String etatCivilSourcier) {
		if (etatCivilSourcier.equals("1")) {
			return EtatCivil.CELIBATAIRE;
		}
		if (etatCivilSourcier.equals("2")) {
			return EtatCivil.MARIE;
		}
		if (etatCivilSourcier.equals("3")) {
			return EtatCivil.VEUF;
		}
		if (etatCivilSourcier.equals("4")) {
			return EtatCivil.DIVORCE;
		}
		if (etatCivilSourcier.equals("5")) {
			return EtatCivil.SEPARE;
		}
		return null;
	}

	/**
	 *
	 * @param etatCivilSourcier
	 * @return
	 */
	@SuppressWarnings("deprecation")
	private MotifFor getMotifForByEtatCivil(EtatCivil etatCivilSourcier) {

		if (etatCivilSourcier.equals(EtatCivil.MARIE)) {
			return MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION;
		}
		if (etatCivilSourcier.equals(EtatCivil.VEUF)) {
			return MotifFor.VEUVAGE_DECES;
		}
		if (etatCivilSourcier.equals(EtatCivil.DIVORCE)) {
			return MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT;
		}
		if (etatCivilSourcier.equals(EtatCivil.SEPARE)) {
			return MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT;
		}
		if (etatCivilSourcier.equals(EtatCivil.CELIBATAIRE)) {
			return MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT;
		}

		return MotifFor.INDETERMINE;
	}








	private String readRapportDeTravail(Long noSourcier) throws Exception {
		String query = "select" + " R.DAD_RAPPORT_TRAV" + ", R.DAF_RAPPORT_TRAV" + ", R.TY_ACTIVITE" + ", R.FK_EMPNO" + " From "
				+ helper.getTableIs("RAPPORT_TRAVAIL") + " R" + " where R.FK_SOUNO =" + noSourcier;
		LOGGER.debug("Query: " + query);
		return query;
	}

	private String readTypeImposition(Long noSourcier, RegDate date) throws Exception {
		String query = "select" + " T.DAD_TY_IMPOSIT" + ", T.TY_IMPOSIT" + " from " + helper.getTableIs("TY_IMPOSITION") + " T"
				+ " where (T.DAF_TY_IMPOSIT >= '" + RegDateHelper.dateToDisplayString(date) + "' OR T.DAF_TY_IMPOSIT = '0001-01-01')"
				+ " AND T.DAA_TY_IMPOSIT = '0001-01-01'" + " AND T.FK_SOUNO =" + noSourcier + " AND T.TY_IMPOSIT <> 'O' "
				+ " ORDER BY T.DAD_TY_IMPOSIT ASC";
		LOGGER.debug("Query: " + query);
		return query;
	}

	private List<SourcierTypeImposition> readAllTypeImposition(int numeroSourcier, RegDate dateDebutValidite) throws Exception {
		String query = "select" + " T.DAD_TY_IMPOSIT" + ", T.TY_IMPOSIT" + " from " + helper.getTableIs("TY_IMPOSITION") + " T"
				+ " where T.DAD_TY_IMPOSIT > '" + RegDateHelper.dateToDisplayString(dateDebutValidite) + "'" + " AND T.TY_IMPOSIT <> 'O' "
				+ " AND T.DAA_TY_IMPOSIT = '0001-01-01'" + " AND T.FK_SOUNO =" + numeroSourcier;
		LOGGER.debug("Query: " + query);

		List<SourcierTypeImposition> listeTypeImposition = new ArrayList<SourcierTypeImposition>();
		Statement stmt = helper.isConnection.createStatement();
		ResultSet rs = stmt.executeQuery(query);
		SourcierTypeImposition typeImposition = null;
		try {
			while (rs.next()) {

				typeImposition = new SourcierTypeImposition();
				typeImposition.dateDebut = RegDate.get(rs.getDate(TYPE_IMPOSIT_DATE_DEBUT));
				typeImposition.typeImposition = rs.getString(TYPE_IMPOSITION);
				typeImposition.motifOuverture = MotifFor.CHGT_MODE_IMPOSITION;
				listeTypeImposition.add(typeImposition);

			}
		}
		finally {
			rs.close();
			stmt.close();
		}
		Collections.sort(listeTypeImposition);
		return listeTypeImposition;

	}





	@SuppressWarnings("unused")
	private StringBuilder readEmployeurValide() throws Exception {
		String query = "   SELECT e.NO_EMPLOYEUR" + "   FROM " + helper.getTableIs("employeur") + " e" + "   WHERE e.ACTIF='O'      "
				+ "  OR e.DESIGN_ABREGEE LIKE '%@%'" + "  UNION" + "   SELECT emp.NO_EMPLOYEUR" + "   FROM  "
				+ helper.getTableIs("employeur") + " emp, " + helper.getTableIs("facture_employeur") + " fact" + "   WHERE emp.ACTIF='N'"
				+ "   AND emp.NO_EMPLOYEUR = fact.FK_CAE_EMPNO " + "   AND fact.FK_TFINOTECH = 1" + "   AND fact.CO_ETAT = 'B'"
				+ "   AND YEAR(fact.DAD_PER_EFFECTIVE)= 2009";

		LOGGER.debug("Query: " + query);
		Statement stmt = helper.isConnection.createStatement();
		StringBuilder listEmployeur = new StringBuilder();
		try {
			ResultSet rs = stmt.executeQuery(query);
			try {
				while (rs.next()) {
					listEmployeur.append(rs.getLong("NO_EMPLOYEUR"));
					listEmployeur.append(",");
				}
			}
			finally {
				rs.close();
			}
		}
		catch (Exception e) {
			LOGGER.info(e.getMessage());
		}
		finally {
			stmt.close();
		}
		if (listEmployeur.length() == 0) {
			return null;
		}
		return listEmployeur.deleteCharAt(listEmployeur.lastIndexOf(","));
	}


private List<SourcierBareme> readBaremeSource(SituationFamilleMenageCommun situation, PersonnePhysique sourcier) throws Exception {

		String dateDebutSituation=null;


		if ("".equals(RegDateHelper.dateToDisplayString(situation.getDateDebut()))) {
			dateDebutSituation = "0001-01-01";
		}else{
			dateDebutSituation = RegDateHelper.dateToDisplayString(situation.getDateDebut());
		}



		String query = "SELECT CO_BAREME, DAD_ASS_BAR, DAF_ASS_BAR FROM  " + helper.getTableIs("ASSUJETTIS_BAREME")
				+ " where DAA_ASS_BAR = '0001-01-01'" + " AND DAD_ASS_BAR>='" + dateDebutSituation
				+ "'" + " AND (DAF_ASS_BAR > '" + dateDebutSituation
				+ "' OR DAF_ASS_BAR ='0001-01-01')" + " AND FK_SIF_SOUNO=" + sourcier.getAncienNumeroSourcier();

		LOGGER.debug("Query: " + query);

		List<SourcierBareme> listeBareme = new ArrayList<SourcierBareme>();
		Statement stmt = helper.isConnection.createStatement();
		ResultSet rs = stmt.executeQuery(query);
		SourcierBareme bareme = null;
		SourcierBareme baremePrecedent = null;
		try {
			while (rs.next()) {

				bareme = new SourcierBareme();
				bareme.dateDebut = RegDate.get(rs.getDate(BAREME_DATE_DEBUT));
				bareme.dateFin = RegDate.get(rs.getDate(BAREME_DATE_FIN));
				bareme.codeBareme = rs.getString(BAREME_CO_BAREME);

				// Deux bareme normaux se suivent on met juste à jour la date de fin du precedent avec celle du courant
				if (baremePrecedent != null && TarifImpotSource.NORMAL.equals(translateBareme(baremePrecedent.codeBareme))
						&& TarifImpotSource.NORMAL.equals(translateBareme(bareme.codeBareme))) {
					baremePrecedent.dateFin = bareme.dateFin;
					baremePrecedent.codeBareme = bareme.codeBareme;

				}
				else {

					listeBareme.add(bareme);
					baremePrecedent = bareme;
				}


			}
		}
		finally {
			rs.close();
			stmt.close();
		}
		Collections.sort(listeBareme);
		return listeBareme;

	}




	private List<SourcierSituationFamille> readSituationsFamilleOneSourcier(int noSourcier, RegDate dateOuverture) throws Exception {
		String query = "SELECT " + "NO_SEQUENCE AS NOSEQ_SITFAM" + ", DAD_SITU_FAMILLE" + ", DAF_SITU_FAMILLE" + ", ET_CIVIL"
				+ ", NB_ENFANTS" + ", FK_R_SOUNO AS CONJOINT" + ",CHARGE_FAMILLE" + " FROM " + helper.getTableIs("SITUATION_FAMILLE") + " WHERE FK_A_SOUNO = "
				+ noSourcier + " AND DAA_SITU_FAMILLE " + helper.getSqlDateIsNull(false) + " AND (DAF_SITU_FAMILLE > '"
				+ RegDateHelper.dateToDisplayString(dateOuverture) + "'" + "OR DAF_SITU_FAMILLE='0001-01-01')"
				+ " ORDER BY DAD_SITU_FAMILLE ASC";

		LOGGER.debug("Query: " + query);

		List<SourcierSituationFamille> listeSituations = new ArrayList<SourcierSituationFamille>();
		Statement stmt = helper.isConnection.createStatement();
		ResultSet rs = stmt.executeQuery(query);
		SourcierSituationFamille situation = null;
		SourcierSituationFamille situationPrecedente = null;
		try {
			while (rs.next()) {

				situation = new SourcierSituationFamille();
				situation.conjointNoSourcier = rs.getLong(SIT_FAMILLE_NO_SRC_CONJOINT);
				situation.dateDebut = RegDate.get(rs.getDate(SIT_FAMILLE_DATE_DEBUT));

				if (situation.dateDebut!=null && situation.dateDebut.equals(RegDate.get(1900, 1, 1))) {
					situation.dateDebut = RegDate.getEarlyDate();
				}

				situation.dateFin = RegDate.get(rs.getDate(SIT_FAMILLE_DATE_FIN));
				situation.etatCivil = getEtatCivil(rs.getString(SIT_FAMILLE_ETAT_CIVIL));
				situation.nbEnfant = rs.getInt(SIT_FAMILLE_NB_ENFANTS);
				situation.chargeFamille = rs.getString(SIT_FAMILLE_CHARGE_FAMILLE);
				situation.motifOuverture = getMotifForByEtatCivil(situation.etatCivil);

				// Dans le host une nouvelle situation de famille est créé pour chaque nouvelle enfants
				// ou lorsqu'il y a une séparation suivie d'un divorce
				// il n'est pas necessaire de recuperer les même type d'etat civil qui se suivent
				if ((situationPrecedente != null && situation.etatCivil.equals(situationPrecedente.etatCivil))
						|| (situationPrecedente != null && EtatCivil.SEPARE.equals(situationPrecedente.etatCivil) && EtatCivil.DIVORCE
								.equals(situation.etatCivil))) {
					// la date de fin de la situation courante est reportée à la date de la situation précédente
					// Ceci afin de guarantir une situation de fin réelle a chaque situation de famille
					situationPrecedente.dateFin = situation.dateFin;
					situationPrecedente.nbEnfant = situation.nbEnfant;

				}
				else {
					listeSituations.add(situation);
					situationPrecedente = situation;
				}

			}
		}
		finally {
			rs.close();
			stmt.close();
		}
		Collections.sort(listeSituations);
		return listeSituations;

	}

	@SuppressWarnings( {
		"unused"
	})
	private SqlRowSet readOneSourcier(long noSourcier) throws Exception {
		String query = "SELECT " + "A.NO_SOURCIER" + ", A.NO_IND_REG_FISCAL" + ", A.NO_REGISTRE_ETRANG" + ", A.NO_AVS" + ", A.SEXE"
				+ ", A.NOM" + ", A.PRENOM" + ", A.DATE_NAISSANCE" + ", B.NO_SEQUENCE AS NOSEQ_ADRFISC " + ", B.DAD_ADR_FISCALE"
				+ ", B.DAF_ADR_FISCALE" + ", B.FK_COMNO" + ", C.NO_SEQUENCE AS NOSEQ_TYIMP" + ", C.DAD_TY_IMPOSIT" + ", C.DAF_TY_IMPOSIT"
				+ ", C.TY_IMPOSIT" + ", D.NO_SEQUENCE AS NOSEQ_SITFAM" + ", D.DAD_SITU_FAMILLE" + ", D.DAF_SITU_FAMILLE" + ", D.ET_CIVIL"
				+ ", D.NB_ENFANTS" + ", D.FK_R_SOUNO AS CONJOINT" + ", E.NO_SEQUENCE AS NOSEQ_PERMIS" + ", E.DAD_PERMIS" + ", E.DAF_PERMIS"
				+ ", E.TY_PERMIS" + " FROM "
				+ helper.getTableIs("SOURCIER")
				+ " A"
				+ ", "
				+ helper.getTableIs("ADR_FISCALE")
				+ "  B "
				+ ", "
				+ helper.getTableIs("TY_IMPOSITION")
				+ "  C "
				+ ", "
				+ helper.getTableIs("SITUATION_FAMILLE")
				+ "  D "
				+ ", "
				+ helper.getTableIs("PERMIS_TRAVAIL")
				+ "  E "
				+ "WHERE 1=1 "
				+ "AND A.NO_SOURCIER = B.FK_SOUNO "
				+ "AND A.NO_SOURCIER = C.FK_SOUNO "
				+ "AND A.NO_SOURCIER = D.FK_A_SOUNO "
				+ "AND A.NO_SOURCIER = E.FK_SOUNO "
				+ "AND B.DAA_ADR_FISCALE "
				+ helper.getSqlDateIsNull(false)
				+ " AND C.DAA_TY_IMPOSIT "
				+ helper.getSqlDateIsNull(false)
				+ " AND D.DAA_SITU_FAMILLE "
				+ helper.getSqlDateIsNull(false)
				+ " AND E.DAA_PERMIS "
				+ helper.getSqlDateIsNull(false)
				+ " AND A.NO_SOURCIER = " + noSourcier;
		query += " ORDER BY B.DAD_ADR_FISCALE DESC, C.DAD_TY_IMPOSIT DESC, D.DAD_SITU_FAMILLE DESC, E.DAD_PERMIS DESC";

		LOGGER.debug("Query: " + query);
		return helper.isTemplate.queryForRowSet(query);
	}



	private String readSourciersOlds() throws Exception {
		String query = "SELECT C.NO_SOURCIER" + ", C.NO_IND_REG_FISCAL" + ", C.NO_REGISTRE_ETRANG" + ", C.NO_AVS" + ", C.SEXE" + ", C.NOM"
				+ ", C.PRENOM" + ", C.DATE_NAISSANCE" + ", C.REMARQUE" + ", C.TYPE_PERMIS" + ", C.NO_REGISTRE_ETRANG"
				+ ", C.NO_REF_CANTONAL" + ", A.NO_EMPLOYEUR" + " FROM "
				+ helper.getTableIs("employeur")
				+ " A, "
				+ helper.getTableIs("rapport_travail")
				+ " B, "
				+ helper.getTableIs("sourcier")
				+ " C, "
				+ helper.getTableIs("cpte_annuel_sour")
				+ " D, "
				+ helper.getTableIs("detail_facture")
				+ " E, "
				+ helper.getTableIs("histo_rapport_trav")
				+ " F "
				+ " WHERE "
				+ "(oracle iooo";
		if (sourcierStart > 0) {
			query += " AND C.NO_SOURCIER >=" + sourcierStart;
		}
		if (sourcierEnd > 0) {
			query += " AND C.NO_SOURCIER <=" + sourcierEnd;
		}

		LOGGER.debug("Query: " + query);
		return query;

	}

	private String readSourciersSansConjointIncomplet() throws Exception {

		String query = "SELECT DISTINCT C.NO_SOURCIER" + ", C.NO_IND_REG_FISCAL" + ", C.NO_REGISTRE_ETRANG" + ", C.NO_AVS" + ", C.SEXE"
				+ ", C.NOM" + ", C.PRENOM" + ", C.DATE_NAISSANCE" + ", C.REMARQUE" + ", C.TYPE_PERMIS" + ", C.NO_REGISTRE_ETRANG"
				+ ", C.NO_REF_CANTONAL" + ", A.NO_EMPLOYEUR" + ",'0001-01-01' as DAD_SITU_FAMILLE FROM "
				+ helper.getTableIs("employeur")
				+ " A, "
				+ helper.getTableIs("rapport_travail")
				+ " B, "
				+ helper.getTableIs("sourcier")
				+ " C, "
				+ helper.getTableIs("cpte_annuel_sour")
				+ " D, "
				+ helper.getTableIs("detail_facture")
				+ " E, "
				+ helper.getTableIs("histo_rapport_trav")
				+ " F, "
				+ helper.getTableIs("situation_famille")
				+ " G "
				+ " WHERE "
				+ " C.CATEGORIE_SOURCIER = 'A + B'"
				+ " AND ((F.DAA_HRT = '01.01.0001'"
				+ " AND (F.DAF_HRT = '01.01.0001'"
				+ " OR F.DAF_HRT >= '01.01.2009')"
				+ " AND A.NO_EMPLOYEUR = B.FK_EMPNO"
				+ " AND B.FK_SOUNO = C.NO_SOURCIER"
				+ " AND B.FK_EMPNO = F.FK_RAT_EMPNO"
				+ " AND B.FK_SOUNO = F.FK_RAT_SOUNO)"
				+ " OR "
				+ " (A.NO_EMPLOYEUR = D.FK_RAT_FKEMPNO"
				+ " AND C.NO_SOURCIER = D.FK_RAT_FKSOUNO"
				+ " AND D.AN_IMPOSITION = E.FK_CASANIMP"
				+ " AND D.FK_RAT_FKEMPNO = E.FK_CAS_RAT_EMPNO"
				+ " AND D.FK_RAT_FKSOUNO = E.FK_CAS_RAT_SOUNO"
				+ " AND E.DA_ENTREE_DECLAREE >= '01.01.2008'"
				+ " AND NOT (E.DA_CTBLN_IMPOT = '01.01.0001')))"
				+ " AND ((G.FK_A_SOUNO = C.NO_SOURCIER)"
				+ " AND (G.ET_CIVIL in ('1','3','4','5'))"
				+ " AND (G.CHARGE_FAMILLE ='O')" + " AND (G.DAF_SITU_FAMILLE = '0001-01-01')" + " AND (G.DAA_SITU_FAMILLE = '0001-01-01'))";
		;
		if (sourcierStart > 0) {
			query += " AND C.NO_SOURCIER >=" + sourcierStart;
		}
		if (sourcierEnd > 0) {
			query += " AND C.NO_SOURCIER <=" + sourcierEnd;
		}
		LOGGER.debug("Query: " + query);

		return query;

	}

	private String readSourciers(String critereEtatCivil) throws Exception {

		String query = "SELECT DISTINCT C.NO_SOURCIER" + ", C.NO_IND_REG_FISCAL" + ", C.NO_REGISTRE_ETRANG" + ", C.NO_AVS" + ", C.SEXE"
				+ ", C.NOM" + ", C.PRENOM" + ", C.DATE_NAISSANCE" + ", C.REMARQUE" + ", C.TYPE_PERMIS" + ", C.NO_REGISTRE_ETRANG"
				+ ", C.NO_REF_CANTONAL FROM "
				+ helper.getTableIs("employeur")
				+ " A, "
				+ helper.getTableIs("rapport_travail")
				+ " B, "
				+ helper.getTableIs("sourcier")
				+ " C, "
				+ helper.getTableIs("histo_rapport_trav")
				+ " F, "
				+ helper.getTableIs("situation_famille")
				+ " G "
				+ " WHERE "
				+ " C.CATEGORIE_SOURCIER = 'A + B'"
				+ " AND F.DAA_HRT = '01.01.0001'"
				+ " AND (F.DAF_HRT = '01.01.0001'"
				+ " OR F.DAF_HRT >= '01.01.2009')"
				+ " AND A.NO_EMPLOYEUR = B.FK_EMPNO"
				+ " AND B.FK_SOUNO = C.NO_SOURCIER"
				+ " AND B.FK_EMPNO = F.FK_RAT_EMPNO"
				+ " AND B.FK_SOUNO = F.FK_RAT_SOUNO"
				+ " AND ((G.FK_A_SOUNO = C.NO_SOURCIER)"
				+ " AND G.ET_CIVIL "+critereEtatCivil
				+ " AND (G.CHARGE_FAMILLE ='O')"
				+ " AND (G.DAF_SITU_FAMILLE = '0001-01-01')"
				+ " AND (G.DAA_SITU_FAMILLE = '0001-01-01'))";

		if (sourcierStart > 0) {
			query += " AND C.NO_SOURCIER >=" + sourcierStart;
		}
		if (sourcierEnd > 0) {
			query += " AND C.NO_SOURCIER <=" + sourcierEnd;
		}

		query+=" UNION ";

		query+="SELECT DISTINCT C.NO_SOURCIER" + ", C.NO_IND_REG_FISCAL" + ", C.NO_REGISTRE_ETRANG" + ", C.NO_AVS" + ", C.SEXE"
		+ ", C.NOM" + ", C.PRENOM" + ", C.DATE_NAISSANCE" + ", C.REMARQUE" + ", C.TYPE_PERMIS" + ", C.NO_REGISTRE_ETRANG"
		+ ", C.NO_REF_CANTONAL FROM "
		+ helper.getTableIs("employeur")
		+ " A, "
		+ helper.getTableIs("sourcier")
		+ " C, "
		+ helper.getTableIs("cpte_annuel_sour")
		+ " D, "
		+ helper.getTableIs("detail_facture")
		+ " E, "
		+ helper.getTableIs("situation_famille")
		+ " G "
		+ " WHERE "
		+ " A.NO_EMPLOYEUR = D.FK_RAT_FKEMPNO"
		+ " AND C.NO_SOURCIER = D.FK_RAT_FKSOUNO"
		+ " AND D.AN_IMPOSITION = E.FK_CASANIMP"
		+ " AND D.FK_RAT_FKEMPNO = E.FK_CAS_RAT_EMPNO"
		+ " AND D.FK_RAT_FKSOUNO = E.FK_CAS_RAT_SOUNO"
		+ " AND E.DA_ENTREE_DECLAREE >= '01.01.2008'"
		+ " AND NOT (E.DA_CTBLN_IMPOT = '01.01.0001')"
		+ " AND ((G.FK_A_SOUNO = C.NO_SOURCIER)"
		+ " AND G.ET_CIVIL "+critereEtatCivil
		+ " AND (G.CHARGE_FAMILLE ='O')"
		+ " AND (G.DAF_SITU_FAMILLE = '0001-01-01')"
		+ " AND (G.DAA_SITU_FAMILLE = '0001-01-01'))";


		if (sourcierStart > 0) {
			query += " AND C.NO_SOURCIER >=" + sourcierStart;
		}
		if (sourcierEnd > 0) {
			query += " AND C.NO_SOURCIER <=" + sourcierEnd;
		}

		LOGGER.debug("Query: " + query);

		return query;

	}

	private String readSourciersEnCouple() throws Exception {

		String query = "SELECT DISTINCT C.NO_SOURCIER" + ", C.NO_IND_REG_FISCAL" + ", C.NO_REGISTRE_ETRANG" + ", C.NO_AVS" + ", C.SEXE"
				+ ", C.NOM" + ", C.PRENOM" + ", C.DATE_NAISSANCE" + ", C.REMARQUE" + ", C.TYPE_PERMIS" + ", C.NO_REGISTRE_ETRANG"
				+ ", C.NO_REF_CANTONAL" + ", A.NO_EMPLOYEUR FROM "
				+ helper.getTableIs("employeur")
				+ " A, "
				+ helper.getTableIs("rapport_travail")
				+ " B, "
				+ helper.getTableIs("sourcier")
				+ " C, "
				+ helper.getTableIs("cpte_annuel_sour")
				+ " D, "
				+ helper.getTableIs("detail_facture")
				+ " E, "
				+ helper.getTableIs("histo_rapport_trav")
				+ " F, "
				+ helper.getTableIs("situation_famille")
				+ " G "
				+ " WHERE "
				+ " C.CATEGORIE_SOURCIER = 'A + B'"
				+ " AND ((F.DAA_HRT = '01.01.0001'"
				+ " AND (F.DAF_HRT = '01.01.0001'"
				+ " OR F.DAF_HRT >= '01.01.2009')"
				+ " AND A.NO_EMPLOYEUR = B.FK_EMPNO"
				+ " AND B.FK_SOUNO = C.NO_SOURCIER"
				+ " AND B.FK_EMPNO = F.FK_RAT_EMPNO"
				+ " AND B.FK_SOUNO = F.FK_RAT_SOUNO)"
				+ " OR "
				+ " (A.NO_EMPLOYEUR = D.FK_RAT_FKEMPNO"
				+ " AND C.NO_SOURCIER = D.FK_RAT_FKSOUNO"
				+ " AND D.AN_IMPOSITION = E.FK_CASANIMP"
				+ " AND D.FK_RAT_FKEMPNO = E.FK_CAS_RAT_EMPNO"
				+ " AND D.FK_RAT_FKSOUNO = E.FK_CAS_RAT_SOUNO"
				+ " AND E.DA_ENTREE_DECLAREE >= '01.01.2008'"
				+ " AND NOT (E.DA_CTBLN_IMPOT = '01.01.0001')))"
				+ " AND ((G.FK_A_SOUNO = C.NO_SOURCIER)"
				+ " AND (G.ET_CIVIL ='2')"
				+ " AND (G.CHARGE_FAMILLE ='O')" + " AND (G.DAF_SITU_FAMILLE = '0001-01-01')" + " AND (G.DAA_SITU_FAMILLE = '0001-01-01'))";
		if (sourcierStart > 0) {
			query += " AND C.NO_SOURCIER >=" + sourcierStart;
		}
		if (sourcierEnd > 0) {
			query += " AND C.NO_SOURCIER <=" + sourcierEnd;
		}

		LOGGER.debug("Query: " + query);

		return query;

	}

	private SourcierLu findConjoint(Long noSourcier) throws SQLException {
		String query = "SELECT DISTINCT C.NO_SOURCIER" + ", C.NO_IND_REG_FISCAL" + ", C.NO_REGISTRE_ETRANG" + ", C.NO_AVS" + ", C.SEXE"
				+ ", C.NOM" + ", C.PRENOM" + ", C.DATE_NAISSANCE" + ", C.REMARQUE" + ", C.TYPE_PERMIS" + ", C.NO_REGISTRE_ETRANG"
				+ ", C.NO_REF_CANTONAL" + ", S.DAD_SITU_FAMILLE FROM " + helper.getTableIs("situation_famille") + " S ,"
				+ helper.getTableIs("sourcier") + " C " + " WHERE S.FK_A_SOUNO =  " + noSourcier + " AND  S.ET_CIVIL ='2'"
				+ " AND (S.CHARGE_FAMILLE ='O')"  + " AND (S.DAA_SITU_FAMILLE = '0001-01-01')"
				 + " AND (S.DAF_SITU_FAMILLE = '0001-01-01')"
				+ " AND  C.NO_SOURCIER = S.FK_R_SOUNO ";
		LOGGER.debug("Query: " + query);
		Statement stmt = helper.isConnection.createStatement();
		ResultSet rs = stmt.executeQuery(query);
		SourcierLu sourcierConjoint = null;
		try {
			while (rs.next()) {

				 sourcierConjoint = buildSourcierLu(rs);


			}
		}
		finally {
			rs.close();
			stmt.close();
		}
		return sourcierConjoint;
	}

	private SourcierLu findSourcierByNumeroIndividu(Long noIndividu) throws SQLException {
		String query = "SELECT DISTINCT C.NO_SOURCIER" + ", C.NO_IND_REG_FISCAL" + ", C.NO_REGISTRE_ETRANG" + ", C.NO_AVS" + ", C.SEXE"
				+ ", C.NOM" + ", C.PRENOM" + ", C.DATE_NAISSANCE" + ", C.REMARQUE" + ", C.TYPE_PERMIS" + ", C.NO_REGISTRE_ETRANG"
				+ ", C.NO_REF_CANTONAL" + ", S.DAD_SITU_FAMILLE FROM " + helper.getTableIs("situation_famille") + " S ,"
				+ helper.getTableIs("sourcier") + " C " + " WHERE S.FK_A_SOUNO =  C.NO_SOURCIER "
				+ " AND  C.NO_IND_REG_FISCAL = "+noIndividu;
		LOGGER.debug("Query: " + query);
		Statement stmt = helper.isConnection.createStatement();
		ResultSet rs = stmt.executeQuery(query);
		SourcierLu sourcierConjoint = null;
		try {
			while (rs.next()) {

				 sourcierConjoint = buildSourcierLu(rs);


			}
		}
		finally {
			rs.close();
			stmt.close();
		}
		return sourcierConjoint;
	}
	private SourcierLu findAncienConjoint(Long noSourcier,RegDate debutCouple) throws SQLException {
		String query = "SELECT DISTINCT C.NO_SOURCIER" + ", C.NO_IND_REG_FISCAL" + ", C.NO_REGISTRE_ETRANG" + ", C.NO_AVS" + ", C.SEXE"
				+ ", C.NOM" + ", C.PRENOM" + ", C.DATE_NAISSANCE" + ", C.REMARQUE" + ", C.TYPE_PERMIS" + ", C.NO_REGISTRE_ETRANG"
				+ ", C.NO_REF_CANTONAL" + ", S.DAD_SITU_FAMILLE FROM " + helper.getTableIs("situation_famille") + " S ,"
				+ helper.getTableIs("sourcier") + " C " + " WHERE S.FK_A_SOUNO =  " + noSourcier + " AND  S.ET_CIVIL ='2'"
				+ " AND (S.CHARGE_FAMILLE ='O')"  + " AND (S.DAA_SITU_FAMILLE = '0001-01-01')"
				 + " AND (S.DAD_SITU_FAMILLE =  '" + RegDateHelper.dateToDisplayString(debutCouple) + "')"
				+ " AND  C.NO_SOURCIER = S.FK_R_SOUNO ";
		LOGGER.debug("Query: " + query);
		Statement stmt = helper.isConnection.createStatement();
		ResultSet rs = stmt.executeQuery(query);
		SourcierLu sourcierConjoint = null;
		try {
			while (rs.next()) {

				 sourcierConjoint = buildSourcierLu(rs);


			}
		}
		finally {
			rs.close();
			stmt.close();
		}
		return sourcierConjoint;
	}

	private SourcierLu findAncienConjointPrincipal(Long noSourcierConjoint,RegDate debutCouple) throws SQLException {
		String query = "SELECT DISTINCT C.NO_SOURCIER" + ", C.NO_IND_REG_FISCAL" + ", C.NO_REGISTRE_ETRANG" + ", C.NO_AVS" + ", C.SEXE"
				+ ", C.NOM" + ", C.PRENOM" + ", C.DATE_NAISSANCE" + ", C.REMARQUE" + ", C.TYPE_PERMIS" + ", C.NO_REGISTRE_ETRANG"
				+ ", C.NO_REF_CANTONAL" + ", S.DAD_SITU_FAMILLE FROM " + helper.getTableIs("situation_famille") + " S ,"
				+ helper.getTableIs("sourcier") + " C " + " WHERE S.FK_A_SOUNO =  " + noSourcierConjoint + " AND  S.ET_CIVIL ='2'"
				+ " AND (S.CHARGE_FAMILLE ='N')"  + " AND (S.DAA_SITU_FAMILLE = '0001-01-01')"
				 + " AND (S.DAD_SITU_FAMILLE =  '" + RegDateHelper.dateToDisplayString(debutCouple) + "')"
				+ " AND  C.NO_SOURCIER = S.FK_R_SOUNO ";
		LOGGER.debug("Query: " + query);
		Statement stmt = helper.isConnection.createStatement();
		ResultSet rs = stmt.executeQuery(query);
		SourcierLu sourcierConjoint = null;
		try {
			while (rs.next()) {

				 sourcierConjoint = buildSourcierLu(rs);


			}
		}
		finally {
			rs.close();
			stmt.close();
		}
		return sourcierConjoint;
	}

	private int findCommuneResidence(RegDate date, ArrayList<SourcierAdresseFiscale> adressesValides) {
		for (SourcierAdresseFiscale adresse : adressesValides) {
			if (RegDateHelper.isBetween(date, adresse.dateDebut, adresse.dateFin, NullDateBehavior.EARLIEST)) {
				return adresse.communeFiscale;
			}
		}
		return 0;
	}

	private Long getNumeroContribuableSourcier(Long noSourcier) throws SQLException {

		Long numero = 0L;

		String query = "select MAX(NUMERO) from TIERS where ANCIEN_NUMERO_SOURCIER=" + noSourcier;
		numero = helper.orclTemplate.queryForLong(query);
		return numero;
	}

	private String readAdresseFiscalesSourcier(Long numeroSourcier) {
		String query = "SELECT A.DAD_ADR_FISCALE," + " A.DAF_ADR_FISCALE," + " A.DA_DEPART_ETRANGER," + " A.DA_RETOUR_ETRANGER,"
				+ " A.FK_COMNO," + " C.NOM_OFS_MIN FROM " + helper.getTableIs("ADR_FISCALE") + " A, " + helper.getTableIs("COMMUNE") + " C"
				+ " WHERE A.DAA_ADR_FISCALE = '0001-01-01'" + " AND A.FK_COMNO = C.NO_TECHNIQUE"
				+ " AND A.FK_SOUNO =" + numeroSourcier;

		LOGGER.debug("Query: " + query);

		return query;
	}



	@SuppressWarnings( {
			"unused", "unchecked"
	})
	private List<ListOrderedMap> getNoIndividuOneSourcier(int noSourcier) {
		String query = "SELECT " + "A.NO_IND_REG_FISCAL " + ", A.SEXE " + "FROM " + helper.getTableIs("SOURCIER") + " A "
				+ "WHERE A.NO_SOURCIER = " + noSourcier;

		LOGGER.debug("Query: " + query);
		return helper.isTemplate.queryForList(query);
	}

	@SuppressWarnings("unused")
	// private PersonnePhysique getTiersSourcier(Integer noHabitant) {
	// if (noHabitant == null) {
	// return new NonHabitant();
	// }
	// Habitant habitant = helper.tiersService.getHabitantByNumeroIndividu(noHabitant.longValue());
	// if (habitant == null) {
	// try {
	// habitant = buildHabitant(helper.serviceCivil.getIndividu(noHabitant, 2400, EnumAttributeIndividu.TOUS));
	// } catch (Exception e) {
	// return new NonHabitant();
	// }
	// }
	// return habitant;
	// }
	private PersonnePhysique buildHabitant(Individu ind) {
		PersonnePhysique sourcier = new PersonnePhysique(true);
		sourcier.setNumeroIndividu(ind.getNoTechnique());
		return sourcier;
	}

	private void buildRapportDePrestationImposable(PersonnePhysique sourcier, Long noSourcier) throws Exception {

		Statement stmt = helper.isConnection.createStatement();
		ResultSet rs = stmt.executeQuery(readRapportDeTravail(noSourcier));

		boolean rapportAbsent = true;
		try {
			while (rs.next()) {
				Long numeroDebiteur = rs.getLong(NUMERO_EMPLOYEUR_RAPPORT) + DebiteurPrestationImposable.FIRST_MIGRATION_ID;
				DebiteurPrestationImposable dpi = helper.tiersService.getDebiteurPrestationImposable(numeroDebiteur);
				RegDate dateDebut = RegDate.get(rs.getDate(DATE_DEBUT_RAPPORT));
				if (dpi != null) {
					rapportAbsent = false;
					if (!isRapportExist(sourcier, dpi, dateDebut)) {
						RegDate dateFin = RegDate.get(rs.getDate(DATE_FIN_RAPPORT));
						TypeActivite typeActivite = translateTypeActivite(rs.getString(TYPE_ACTIVITE));
						Integer tauxActivite = null;
						if (TypeActivite.PRINCIPALE.equals(typeActivite)) {
							tauxActivite = 100;
						}

						addRapportPrestationImposableBatch(sourcier, dpi, dateDebut, dateFin, typeActivite, tauxActivite);
					}
				}

			}
		}
		finally {

			rs.close();
			stmt.close();
		}

		/*
		 * if (rapportAbsent) { REJET.info(sourcier.getNumero() + ";" + noSourcier + ";Aucun rapport de travail trouvé"); }
		 */

	}

	/**
	 * Ajoute un rapport-entre-tiers entre un débiteur et un sourcier <b>sans</b> modifier l'état du débiteur.
	 * <p>
	 * Le but est de ne pas provoquer le flush du débiteur, ce qui aurait pour conséquence d'incrémenter le numéro de version de
	 * l'optimistic locking et finalement de provoquer des erreurs dans les autres threads.
	 * <p>
	 * <b>Attention !</b> Les collections <i>rapportsObjet</i> et <i>rapportsSujet</i> ne sont pas modifiées, ni dans le débiteur ni dans le
	 * sourcier !
	 */
	private void addRapportPrestationImposableBatch(PersonnePhysique sourcier, DebiteurPrestationImposable dpi, RegDate dateDebut,
			RegDate dateFin, TypeActivite typeActivite, Integer tauxActivite) {

		RapportPrestationImposable rapport = new RapportPrestationImposable();
		rapport.setDateDebut(dateDebut);
		rapport.setDateFin(dateFin);
		rapport.setTypeActivite(typeActivite);
		rapport.setTauxActivite(tauxActivite);
		rapport.setObjet(dpi);
		rapport.setSujet(sourcier);
		rapport = (RapportPrestationImposable) helper.tiersDAO.save(rapport);
	}

	private TypeActivite translateTypeActivite(String code) {
		if (code.equals("P")) {
			return TypeActivite.PRINCIPALE;
		}
		if (code.equals("C")) {
			return TypeActivite.COMPLEMENTAIRE;
		}
		if (code.equals("A")) {
			return TypeActivite.ACCESSOIRE;
		}

		return null;
	}

	private TarifImpotSource translateBareme(String codeBareme){
		String codeAVerifier = codeBareme.substring(0, 3);
		if ("VDB".equals(codeAVerifier) || "VDD".equals(codeAVerifier) || "VDA".equals(codeAVerifier)) {
			return TarifImpotSource.NORMAL;
		}

		if ("VDC".equals(codeAVerifier)) {
			return TarifImpotSource.DOUBLE_GAIN;
		}
		return null;
	}

	private int getNbEnfantOnBareme(String codeBareme){
		codeBareme = StringUtils.trim(codeBareme);
		String nbEnfants = codeBareme.substring(3);
		if ("".equals(nbEnfants)) {
			return 0;
		}
		return Integer.parseInt(nbEnfants);
	}

	@SuppressWarnings( {
			"unused", "unchecked"
	})
	private List<SourcierSituationFamille> getConjoints(Long noSourcier) {
		List<SourcierSituationFamille> lstConjoints = new ArrayList<SourcierSituationFamille>();
		String query = "SELECT " + "DAD_SITU_FAMILLE" + ", DAF_SITU_FAMILLE" + ", FK_R_SOUNO AS CONJOINT " + "FROM "
				+ helper.getTableIs("SITUATION_FAMILLE") + " A " + "WHERE FK_A_SOUNO = " + noSourcier + " AND FK_R_SOUNO IS NOT NULL"
				+ " ORDER BY DAD_SITU_FAMILLE DESC";
		LOGGER.debug("Query: " + query);
		List<ListOrderedMap> lst = helper.isTemplate.queryForList(query);
		Long saveNoConjoint = 0L;
		RegDate lastDebDate = RegDate.get(new Date(0));
		for (ListOrderedMap oneOcc : lst) {
			SourcierSituationFamille srcSitFam = new SourcierSituationFamille();
			if (saveNoConjoint != (Long) oneOcc.getValue(2)) {
				srcSitFam.dateDebut = RegDate.get((Date) oneOcc.getValue(0));
				srcSitFam.dateFin = RegDate.get((Date) oneOcc.getValue(1));
				srcSitFam.conjointNoSourcier = (Long) oneOcc.getValue(2);
				saveNoConjoint = srcSitFam.conjointNoSourcier;
				lastDebDate = RegDate.get((Date) oneOcc.getValue(0));
			}
			else {
				if (lastDebDate.getOneDayBefore().equals(RegDate.get((Date) oneOcc.getValue(1)))) {
					lastDebDate = RegDate.get((Date) oneOcc.getValue(0));
					continue;
				}
				srcSitFam.dateDebut = RegDate.get((Date) oneOcc.getValue(0));
				srcSitFam.dateFin = RegDate.get((Date) oneOcc.getValue(1));
				srcSitFam.conjointNoSourcier = (Long) oneOcc.getValue(2);
				saveNoConjoint = (Long) oneOcc.getValue(2);
				lastDebDate = RegDate.get((Date) oneOcc.getValue(0));
			}
			lstConjoints.add(srcSitFam);
		}
		return lstConjoints;
	}

	@SuppressWarnings( {
		"unused"
	})
	private void mergeSituationFamille(Set<SituationFamille> setSitFamCelib, Set<SituationFamille> setSitFamCouple,
			Set<SituationFamille> setSitFamEnf) {
		for (SituationFamille sitFamEnf : setSitFamEnf) {
			for (SituationFamille sitFamCelib : setSitFamCelib) {
				if (RegDateHelper.isBetween(sitFamEnf.getDateDebut(), sitFamCelib.getDateDebut(), sitFamCelib.getDateFin(),
						NullDateBehavior.LATEST)) {
					if (!sitFamEnf.getDateDebut().equals(sitFamCelib.getDateDebut())) {
						RegDate dateFinToKeep = sitFamCelib.getDateFin();
						sitFamCelib.setDateFin(sitFamEnf.getDateDebut().getOneDayBefore());
						SituationFamille sitFam = new SituationFamille();
						sitFam.setDateDebut(sitFamEnf.getDateDebut());
						sitFam.setDateFin(dateFinToKeep);
						sitFam.setNombreEnfants(sitFamEnf.getNombreEnfants());
						setSitFamCelib.add(sitFam);
					}
					else {
						sitFamCelib.setNombreEnfants(sitFamEnf.getNombreEnfants());
					}
					setSitFamEnf.remove(sitFamEnf);
				}
			}
			for (SituationFamille sitFamCouple : setSitFamCouple) {
				if (RegDateHelper.isBetween(sitFamEnf.getDateDebut(), sitFamCouple.getDateDebut(), sitFamCouple.getDateFin(),
						NullDateBehavior.LATEST)) {
					if (!sitFamEnf.getDateDebut().equals(sitFamCouple.getDateDebut())) {
						RegDate dateFinToKeep = sitFamCouple.getDateFin();
						sitFamCouple.setDateFin(sitFamEnf.getDateDebut().getOneDayBefore());
						SituationFamille sitFam = new SituationFamille();
						sitFam.setDateDebut(sitFamEnf.getDateDebut());
						sitFam.setDateFin(dateFinToKeep);
						sitFam.setNombreEnfants(sitFamEnf.getNombreEnfants());
						setSitFamCouple.add(sitFam);
					}
					else {
						sitFamCouple.setNombreEnfants(sitFamEnf.getNombreEnfants());
					}
					setSitFamEnf.remove(sitFamEnf);
				}
			}
		}
	}

	private boolean isRapportExist(PersonnePhysique sourcier, DebiteurPrestationImposable dpi, RegDate dateDebut) {

		Set<RapportEntreTiers> listeRapport = sourcier.getRapportsSujet();
		if (listeRapport != null) {
			for (Iterator<?> iterator = listeRapport.iterator(); iterator.hasNext();) {
				RapportEntreTiers rapportEntreTiers = (RapportEntreTiers) iterator.next();
				if (rapportEntreTiers instanceof RapportPrestationImposable && rapportEntreTiers.getObjet() == dpi
						&& rapportEntreTiers.getDateDebut() == dateDebut) {
					return true;

				}
			}
		}
		return false;

	}

	protected Tiers saveTiers(Tiers tiers, long numeroSourcier) {
		Assert.notNull(tiers);
		ValidationResults validation = tiers.validate();
		if (!validation.getErrors().isEmpty()) {
			throw new ValidationException(tiers, validation.getErrors(), validation.getWarnings());
		}

		// DEBUG
		long numero = -1L;
		if (tiers.getNumero() != null) {
			numero = tiers.getNumero();
		}
		tiers = helper.tiersDAO.save(tiers);

		int nb = status.addGlobalObjectsMigrated(1);
		if (nb % 100 == 0) {
			LOGGER.info("Nombre de tiers sauvés: " + nb);
		}

		if (LOGGER.isDebugEnabled()) {
			// LOGGER.debug("Tiers saved: "+numero);
			setRunningMessage("Migration de " + tiers.getNatureTiers() + "(" + numero + ") total: " + status.getGlobalNbObjectsMigrated());
		}
		return tiers;
	}

	private SourcierLu buildSourcierLu(ResultSet rs) throws SQLException {
		SourcierLu sourcierCourant = new SourcierLu();
		sourcierCourant.noSourcier = rs.getLong(NUMERO_SOURCIER);
		sourcierCourant.noIndividu = rs.getLong(NUMERO_INDIVIDU);
		sourcierCourant.noAvs = rs.getString(NUMERO_AVS);
		sourcierCourant.dateNaissance = RegDate.get(rs.getDate(DATE_NAISSANCE_SOURCIER));
		sourcierCourant.prenom = rs.getString(PRENOM_SOURCIER);
		sourcierCourant.nom = rs.getString(NOM_SOURCIER);
		sourcierCourant.sexe = rs.getString(SEXE_SOURCIER);
		sourcierCourant.remarque = rs.getString(REMARQUE_SOURCIER);
		sourcierCourant.typePermis = rs.getString(TYPE_PERMIS_SOURCIER);
		sourcierCourant.noRegistreEtranger = rs.getString(NUMERO_REGISTRE_ETRANGER);
		sourcierCourant.numeroReferenceCantonal = rs.getString(NUMERO_REF_CANTONAL);

		return sourcierCourant;
	}

	private List<SourcierLu> buildUniciteSourcier(boolean isSansConjoint) throws Exception {
		HashMap<Long, SourcierLu> result = new HashMap<Long, SourcierLu>();
		Statement stmt = helper.isConnection.createStatement();
		ResultSet rs = null;
		if (isSansConjoint) {
			rs = stmt.executeQuery(readSourciers("in ('1','3','4','5')"));
		}
		else {
			rs = stmt.executeQuery(readSourciers("= '2'"));
		}

		try {
			while (rs.next()) {
				Long numeroSourcier = rs.getLong(NUMERO_SOURCIER);
				if (!result.containsKey(numeroSourcier)) {
					SourcierLu sourcierCourant = buildSourcierLu(rs);
					result.put(numeroSourcier, sourcierCourant);
				}
			}
		}
		finally {
			rs.close();
			stmt.close();
		}

		List<SourcierLu> sourciers = new ArrayList<SourcierLu>(result.values());
		Collections.sort(sourciers, new Comparator<SourcierLu>() {

			public int compare(SourcierLu o1, SourcierLu o2) {
				return o1.noSourcier.compareTo(o2.noSourcier);
			}
		});

		return sourciers;
	}
}
