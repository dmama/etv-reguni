package ch.vd.uniregctb.migreg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.apache.log4j.Logger;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.migreg.HostMigratorHelper.IndexationMode;

public class LimitsConfigurator {

	private static Logger LOGGER = Logger.getLogger(LimitsConfigurator.class);

	public static final String TUTELLES = "TUTELLES";
	public static final String MARIE_SEUL = "MARIE_SEUL";

	public static final String FULL = "FULL";
	public static final String ACI_TESTING = "ACI_TESTING";
	public static final String PERFS = "PERFS";
	public static final String DATA_TEST = "DATA_TEST";
	public static final String DEVEL = "DEVEL";
	public static final String DPI = "DPI";
	public static final String DPI_FULL = "DPI_FULL";
	public static final String DPI_VAUD_HACK = "DPI_VAUD_HACK";
	public static final String SMALL = "SMALL";
	public static final String ANNIE = "ANNIE";
	public static final String GILLES = "GILLES";
	public static final String GILLES_SOURCIER = "GILLES_SOURCIER";
	public static final String SOURCIER = "SOURCIER";
	public static final String SOURCIERDEBUG = "SOURCIERDEBUG";
	public static final String SOURCIER_FULL = "SOURCIER_FULL";
	public static final String SOURCIER_BADAVS = "SOURCIER_BADAVS";
	public static final String JEC = "JEC";
	public static final String TRUNCATE = "TRUNCATE";
	public static final String CHARGE1_CTB = "CHARGE1_CTB";
	public static final String CHARGE1_COUPLE = "CHARGE1_COUPLE";
	public static final String CHARGE2_CTB = "CHARGE2_CTB";
	public static final String CHARGE2_COUPLE = "CHARGE2_COUPLE";
	public static final String CHARGE3_CTB = "CHARGE3_CTB";
	public static final String CHARGE3_COUPLE = "CHARGE3_COUPLE";
	public static final String CHARGE_ALL_CTB = "CHARGE_ALL_CTB";
	public static final String CHARGE_ALL_COUPLE = "CHARGE_ALL_COUPLE";
	public static final String CHARGE_DROITS_ACCES = "CHARGE_DROITS_ACCES";

	private static final HashMap<String, MigRegLimitsList> configs = new HashMap<String, MigRegLimitsList>();

	static {
		init();
	}

	private static void init() {

		addTruncateConfig(TRUNCATE);
		createConfig(TUTELLES).setWantTutelles(true);
		createConfig(MARIE_SEUL).setWantMariesSeuls(true);

		addConfigFull();
		addConfigAciTesting();
		addConfigPerfs();
		addConfigAnnie(); // <-- nécessaire pour le test MigregPlusEnvoiDIsTest !!
		addConfigDevel();
		addConfigDPI();
		addConfigDPIFull();
		addConfigSourcierFull();
		addConfigSmall();
		addConfigGilles();
		addConfigTestDeCharge();
		addConfigTestDeChargeAll();
		addConfigSourcier();
		addConfigSourcierDebug();
		addConfigDroitAcces();
		addConfigDPIVAUDHACK();
		addConfigSourcierBadAvs();
	}

	private static void addConfigAnnie() {

		addConfigAnnieCJVZ(ANNIE+"_CIIV1");
		addConfigAnnieCFTZ(ANNIE+"_CIIT1");
		addConfigAnnieCJVZ(ANNIE); // Job par défaut
	}

	private static void addConfigAnnieCJVZ(String jobName) {

		addTruncateConfig(jobName).setIndexationMode(IndexationMode.AT_END); // le mode ASYNC provoque des dead-locks dans la base !

		addConfigToAnnie(jobName, 10006616);
		addConfigToAnnie(jobName, 10007650);
		addConfigToAnnie(jobName, 10018609);
		addConfigToAnnie(jobName, 10021307);
		addConfigToAnnie(jobName, 10027705);
		addConfigToAnnie(jobName, 10028255);
		addConfigToAnnie(jobName, 10097845);
		addConfigToAnnie(jobName, 10119001);
		addConfigToAnnie(jobName, 10127669);
		addConfigToAnnie(jobName, 10194002, 10194003);
		addConfigToAnnie(jobName, 10204906, 10204907);
		addConfigToAnnie(jobName, 10261342, 10261343);
		addConfigToAnnie(jobName, 10291623, 10291624);

		// Broulis
		addConfigToAnnie(jobName, 10149508, false);
		addConfigToAnnie(jobName, 10149509, false);
		addConfigToAnnie(jobName, 22317802, true);

		// Tutellisée
		addConfigToAnnie(jobName, 87418907, false);

		// Schumacher
		addConfigToAnnie(jobName, 10246700, false);
		addConfigToAnnie(jobName, 10246701, false);
		addConfigToAnnie(jobName, 87007406, true);

		// Quelques couples
		addConfigToAnnie(jobName, 10216551, false);
		addConfigToAnnie(jobName, 10216552, false);
		addConfigToAnnie(jobName, 10019348, true);

		addConfigToAnnie(jobName, 10532123, false);
		addConfigToAnnie(jobName, 10532124, false);
		addConfigToAnnie(jobName, 10011360, true);


		// Enfant mineur
		addConfigToAnnie(jobName, 10291626);

		// Maillard
		addConfigToAnnie(jobName, 10159702, false);
		addConfigToAnnie(jobName, 10159703, false);
		addConfigToAnnie(jobName, 61608102, true);

		// PACS
		addConfigToAnnie(jobName, 52904501, false);
		addConfigToAnnie(jobName, 65300904, false);
		addConfigToAnnie(jobName, 10095087, true);

		/*
		 * Spécifique pour les DIs
		 */

		// Mariage en 2008
		addConfigToAnnie(jobName, 57109210, false); // homme
		addConfigToAnnie(jobName, 75107103, false); // femme
		addConfigToAnnie(jobName, 10099496, true); // couple

		// Veuvage en 2008
		addConfigToAnnie(jobName, 10105284, false); // homme
		// addConfigToAnnie(jobName, ?, false); // femme
		addConfigToAnnie(jobName, 13108406, true); // couple

		// Veuvage en 2007
		// addConfigToAnnie(jobName, ?, false); // homme
		addConfigToAnnie(jobName, 10094996, false); // femme
		addConfigToAnnie(jobName, 79302304, true); // couple

		// Arrivée de hors-Suisse d'un Suisse
		addConfigToAnnie(jobName, 2110107); // ancien numéro
		addConfigToAnnie(jobName, 10105123); // nouveau numéro

		// Départ hors-Suisse durant la période fiscale
		addConfigToAnnie(jobName, 10091859);
	}

	private static void addConfigAnnieCFTZ(String jobName) {

		addTruncateConfig(jobName).setIndexationMode(IndexationMode.AT_END); // le mode ASYNC provoque des dead-locks dans la base !

		addConfigToAnnie(jobName, 10006616);
		addConfigToAnnie(jobName, 10007650);
		addConfigToAnnie(jobName, 10011360);
		addConfigToAnnie(jobName, 10018609);
		addConfigToAnnie(jobName, 10021307);
		addConfigToAnnie(jobName, 10027705);
		addConfigToAnnie(jobName, 10028255);
		addConfigToAnnie(jobName, 10097845);
		addConfigToAnnie(jobName, 10119001);
		addConfigToAnnie(jobName, 10127669);
		addConfigToAnnie(jobName, 10194002, 10194003);
		addConfigToAnnie(jobName, 10204906, 10204907);
		addConfigToAnnie(jobName, 10261342, 10261343);
		addConfigToAnnie(jobName, 10291623, 10291624);
		addConfigToAnnie(jobName, 52904501);
		addConfigToAnnie(jobName, 65300904);

		// HC act indep
		addConfigToAnnie(jobName, 10028255);

		// Indigent
		addConfigToAnnie(jobName, 87418907);

		// Pryde James
		addConfigToAnnie(jobName, 10523249, false);
		addConfigToAnnie(jobName, 10523250, false);
		addConfigToAnnie(jobName, 10060759, true);

		// Broulis
		addConfigToAnnie(jobName, 10144943, false);
		addConfigToAnnie(jobName, 10144944, false);
		addConfigToAnnie(jobName, 94201606, false);
		addConfigToAnnie(jobName, 22317802, true);

		// Tutellisée
		addConfigToAnnie(jobName, 87418907, false);

		// Schumacher
		addConfigToAnnie(jobName, 10241289, false);
		addConfigToAnnie(jobName, 10241290, false);
		addConfigToAnnie(jobName, 10241291, false);
		addConfigToAnnie(jobName, 87007406, true);

		// QQun en couple
		addConfigToAnnie(jobName, 10261342, false);
		addConfigToAnnie(jobName, 10261343, false);
		addConfigToAnnie(jobName, 10019348, true);

		// Enfant mineur
		addConfigToAnnie(jobName, 10291626);

		// Maillard
		addConfigToAnnie(jobName, 10153262, false);
		addConfigToAnnie(jobName, 10153263, false);
		addConfigToAnnie(jobName, 58104102, false);
		addConfigToAnnie(jobName, 61608102, true);

		// Rollo
		addConfigToAnnie(jobName, 10450326, false);
		addConfigToAnnie(jobName, 10450325, false);
		addConfigToAnnie(jobName, 41317602, false);
		addConfigToAnnie(jobName, 10450681, false);
		addConfigToAnnie(jobName, 81301001, true);

		// PACS
		addConfigToAnnie(jobName, 52904501, 52904504);
		addConfigToAnnie(jobName, 10095087, true);

		/*
		 * Spécifique pour les DIs
		 */

		// Mariage en 2008
		addConfigToAnnie(jobName, 57109210, false); // homme
		addConfigToAnnie(jobName, 75107103, false); // femme
		addConfigToAnnie(jobName, 10099496, true); // couple

		// Veuvage en 2008
		addConfigToAnnie(jobName, 10105284, false); // homme
		// addConfigToAnnie(jobName, ?, false); // femme
		addConfigToAnnie(jobName, 13108406, true); // couple

		// Veuvage en 2007
		// addConfigToAnnie(jobName, ?, false); // homme
		addConfigToAnnie(jobName, 10094996, false); // femme
		addConfigToAnnie(jobName, 79302304, true); // couple

		// Arrivée de hors-Suisse d'un Suisse
		addConfigToAnnie(jobName, 2110107); // ancien numéro
		addConfigToAnnie(jobName, 10105123); // nouveau numéro

		// Départ hors-Suisse durant la période fiscale
		addConfigToAnnie(jobName, 10091859);
	}
	private static void addConfigToAnnie(String jobName, int numero) {
		addConfigToAnnie(jobName, numero, false);
		addConfigToAnnie(jobName, numero, true);
	}
	private static void addConfigToAnnie(String jobName, int numero, boolean doCouple) {
		{
			MigRegLimits l = new MigRegLimits();
			l.setWantCouple(doCouple);
			l.setOrdinaire(numero, numero);
			addConfig(jobName, l);
		}
	}
	private static void addConfigToAnnie(String jobName, int numero1, int numero2) {
		{
			MigRegLimits l = new MigRegLimits();
			l.setWantCouple(false);
			l.setOrdinaire(numero1, numero2);
			addConfig(jobName, l);
		}
	}

	private static void addConfigFull() {

		MigRegLimitsList list = addTruncateConfig(FULL);
		list.setIndexationMode(IndexationMode.NONE);
//		list.setNbThreads(16);

		// CTBs pour tous
		{
			MigRegLimits l = new MigRegLimits();
			l.setWantCouple(false);
			l.setOrdinaire(10000000, 99999999);
			addConfig(FULL, l);
		}
		// CTBs couple
		{
			MigRegLimits l = new MigRegLimits();
			l.setWantCouple(true);
			l.setOrdinaire(10000000, 99999999);
			addConfig(FULL, l);
		}
		// Debiteurs
		{
			MigRegLimits l = new MigRegLimits();
			l.setDebiteurs(1, 500000);
			addConfig(FULL, l);
		}
	}

	private static void addConfigSmall() {

		MigRegLimitsList list = addTruncateConfig(SMALL);
		list.setIndexationMode(IndexationMode.ASYNC);
//		list.setNbThreads(16);

		addConfigToSmall(10288530, 10288630);
		addConfigToSmall(54700101, 54707501);

		// Debiteurs : 152 DPIs
		{
			MigRegLimits l = new MigRegLimits();
			l.setDebiteurs(10000, 13000);
			addConfig(SMALL, l);
		}

	}
	private static void addConfigToSmall(int start, int end) {

		{
			MigRegLimits l = new MigRegLimits();
			l.setWantCouple(false);
			l.setOrdinaire(start, end);
			addConfig(SMALL, l);
		}
		{
			MigRegLimits l = new MigRegLimits();
			l.setWantCouple(true);
			l.setOrdinaire(start, end);
			addConfig(SMALL, l);
		}
	}

	private static void addConfigDevel() {

		MigRegLimitsList list = addTruncateConfig(DEVEL);
		list.setIndexationMode(IndexationMode.ASYNC);
//		list.setNbThreads(4);

		// Broulis
		addConfigTodevel(10236982, 10236982);
		addConfigTodevel(22317802, 22317802);

		// Tutellisée
		addConfigTodevel(87418907, 87418907);

		// Schumacher
		addConfigTodevel(10236982, 10236983);
		addConfigTodevel(87007406, 87007406);

		// ~500 CTBs
		addConfigTodevel(10113991, 10114673);

		// ~500 CTBs
		addConfigTodevel(54700101, 54707501);

		// Debiteurs : 36 DPIs
		{
			MigRegLimits l = new MigRegLimits();
			l.setDebiteurs(4464, 4464);
			addConfig(DEVEL, l);
		}

	}

	private static void addConfigDPI() {

		//MigRegLimitsList list = createConfig(DPI);
		MigRegLimitsList list = addTruncateConfig(DPI);
		list.setWantTruncate(true);
		list.setIndexationMode(IndexationMode.NONE);

		// Debiteurs : 36 DPIs
		{
			MigRegLimits l = new MigRegLimits();
			l.setDebiteurs(195499,195499);
			addConfig(DPI,l);
		}

	}

	private static void addConfigDPIFull() {

		MigRegLimitsList list = createConfig(DPI_FULL);
		list.setWantTruncate(false);
		list.setIndexationMode(IndexationMode.NONE);

		{
			MigRegLimits l = new MigRegLimits();
			l.setDebiteurs(0,999999);
			addConfig(DPI_FULL,l);
		}

	}
	private static void addConfigDPIVAUDHACK() {

		MigRegLimitsList list = createConfig(DPI_VAUD_HACK);
		list.setWantTruncate(false);
		list.setIndexationMode(IndexationMode.NONE);

		{
			MigRegLimits l1 = new MigRegLimits();
			l1.setDebiteurs(203998,203998);
			list.append(l1);

			MigRegLimits l2 = new MigRegLimits();
			l2.setDebiteurs(204056,204056);
			list.append(l2);

			MigRegLimits l3 = new MigRegLimits();
			l3.setDebiteurs(277441, 277441);
			list.append(l3);

			MigRegLimits l4 = new MigRegLimits();
			l4.setDebiteurs(278200, 278200);
			list.append(l4);

			MigRegLimits l5 = new MigRegLimits();
			l5.setDebiteurs(281365, 281365);
			list.append(l5);
			//CHUV
			MigRegLimits l6 = new MigRegLimits();
			l6.setDebiteurs(277374, 277374);
			list.append(l6);

			MigRegLimits l7 = new MigRegLimits();
			l7.setDebiteurs(278201, 278201);
			list.append(l7);
		}

	}

	private static void addConfigDroitAcces() {

		MigRegLimitsList list = createConfig(CHARGE_DROITS_ACCES);
		list.setWantTruncate(false);
		list.setIndexationMode(IndexationMode.NONE);

		// Droits acces
		{
			MigRegLimits l = new MigRegLimits();
			l.setDroitAcces(0, 999999);
			list.append(l);
		}
	}

	private static void addConfigSourcier() {

		MigRegLimitsList list = createConfig(SOURCIER);
		list.setWantTruncate(false);
		list.setIndexationMode(IndexationMode.NONE);

		// Sourcier
		{
			MigRegLimits l = new MigRegLimits();
			l.setSourciers(118433,118433);
			list.append(l);
		}

	}

	private static void addConfigSourcierDebug() {

		MigRegLimitsList list = createConfig(SOURCIERDEBUG);
		list.setWantTruncate(false);
		list.setIndexationMode(IndexationMode.NONE);

		// Sourcier

		{
			MigRegLimits l = new MigRegLimits();
			l.setSourciers(70309,70309);
			list.append(l);
		}

		{
			MigRegLimits l = new MigRegLimits();
			l.setSourciers(63133,63133);
			list.append(l);
		}










	}


	private static void addConfigSourcierFull() {

		MigRegLimitsList list = createConfig(SOURCIER_FULL);
		list.setWantTruncate(false);
		list.setIndexationMode(IndexationMode.NONE);

		// Sourcier
		{
			MigRegLimits l = new MigRegLimits();
			l.setSourciers(0,999999);
			list.append(l);
		}


	}

	private static void addConfigSourcierBadAvs() {

		MigRegLimitsList list = createConfig(SOURCIER_BADAVS);
		list.setWantTruncate(false);
		list.setIndexationMode(IndexationMode.NONE);

		// Sourcier
		{
			MigRegLimits l = new MigRegLimits();
			l.setSourciersBadAvs(0,999999);
			list.append(l);
		}


	}
	private static void addConfigTodevel(int start, int end) {
		{
			MigRegLimits l = new MigRegLimits();
			l.setWantCouple(false);
			l.setOrdinaire(start, end);
			addConfig(DEVEL, l);
		}
		{
			MigRegLimits l = new MigRegLimits();
			l.setWantCouple(true);
			l.setOrdinaire(start, end);
			addConfig(DEVEL, l);
		}
	}


	private static void addConfigGilles() {

//		addTruncateConfig(GILLES).setIndexationMode(IndexationMode.NONE);

		MigRegLimitsList celibat = new MigRegLimitsList("celibat");
		MigRegLimitsList couples = new MigRegLimitsList("couples");

//		MigRegLimits l = new MigRegLimits();
//		l.setOrdinaire(10000000, 10001000);
//		addLimitsFor(celibat, couples, 10251175, 10251175);
//		addLimitsFor(celibat, couples, 10066525, 10066526);

		addLimitsFor(celibat, couples, 10344774, 10344774);
//		addLimitsFor(celibat, couples, 10093954, 10093954);
//		addLimitsFor(celibat, couples, 10093957, 10093957);
//		addLimitsFor(celibat, couples, 10093962, 10093962);
//		addLimitsFor(celibat, couples, 10093964, 10093964);
//		addLimitsFor(celibat, couples, 10093964, 10093964);
//		addLimitsFor(celibat, couples, 10093965, 10093965);
//		addLimitsFor(celibat, couples, 10093968, 10093968);
//		addLimitsFor(celibat, couples, 10093968, 10093968);
//		addLimitsFor(celibat, couples, 10093969, 10093969);
//		addLimitsFor(celibat, couples, 10093977, 10093977);
//		addLimitsFor(celibat, couples, 10093979, 10093979);
//		addLimitsFor(celibat, couples, 10093979, 10093979);
//		addLimitsFor(celibat, couples, 10093985, 10093985);
//		addLimitsFor(celibat, couples, 10093986, 10093986);
//		addLimitsFor(celibat, couples, 10093986, 10093986);
//		addLimitsFor(celibat, couples, 10093987, 10093987);
//		addLimitsFor(celibat, couples, 10093988, 10093988);
//		addLimitsFor(celibat, couples, 10093988, 10093988);
//		addLimitsFor(celibat, couples, 10093989, 10093989);
//		addLimitsFor(celibat, couples, 10093992, 10093992);
//		addLimitsFor(celibat, couples, 10093996, 10093996);
//		addLimitsFor(celibat, couples, 10093999, 10093999);
//		addLimitsFor(celibat, couples, 10093999, 10093999);
//		addLimitsFor(celibat, couples, 10093999, 10093999);
//		addLimitsFor(celibat, couples, 10094000, 10094000);
//		addLimitsFor(celibat, couples, 10094000, 10094000);
//		addLimitsFor(celibat, couples, 10094002, 10094002);
//		addLimitsFor(celibat, couples, 10094003, 10094003);
//		addLimitsFor(celibat, couples, 10094005, 10094005);
//		addLimitsFor(celibat, couples, 10094007, 10094007);
//		addLimitsFor(celibat, couples, 10094008, 10094008);
//		addLimitsFor(celibat, couples, 10094011, 10094011);
//		addLimitsFor(celibat, couples, 10094034, 10094034);
//		addLimitsFor(celibat, couples, 10094035, 10094035);
//		addLimitsFor(celibat, couples, 10094038, 10094038);
//		addLimitsFor(celibat, couples, 10094039, 10094039);
//		addLimitsFor(celibat, couples, 10094040, 10094040);
//		addLimitsFor(celibat, couples, 10094053, 10094053);
//		addLimitsFor(celibat, couples, 10094054, 10094054);
//		addLimitsFor(celibat, couples, 10094055, 10094055);
//		addLimitsFor(celibat, couples, 10094056, 10094056);
//		addLimitsFor(celibat, couples, 10094057, 10094057);
//		addLimitsFor(celibat, couples, 10094060, 10094060);
//		addLimitsFor(celibat, couples, 10094061, 10094061);
//		addLimitsFor(celibat, couples, 10094062, 10094062);
//		addLimitsFor(celibat, couples, 10094063, 10094063);
//		addLimitsFor(celibat, couples, 10094064, 10094064);
//		addLimitsFor(celibat, couples, 10094065, 10094065);
//		addLimitsFor(celibat, couples, 10094068, 10094068);
//		addLimitsFor(celibat, couples, 10094069, 10094069);
//		addLimitsFor(celibat, couples, 10094070, 10094070);
//		addLimitsFor(celibat, couples, 10094072, 10094072);
//		addLimitsFor(celibat, couples, 10094073, 10094073);
//		addLimitsFor(celibat, couples, 10094074, 10094074);
//		addLimitsFor(celibat, couples, 10094076, 10094076);
//		addLimitsFor(celibat, couples, 10094077, 10094077);
//		addLimitsFor(celibat, couples, 10094078, 10094078);
//		addLimitsFor(celibat, couples, 10094079, 10094079);
//		addLimitsFor(celibat, couples, 10094079, 10094079);
//		addLimitsFor(celibat, couples, 10094081, 10094081);
//		addLimitsFor(celibat, couples, 10094082, 10094082);
//		addLimitsFor(celibat, couples, 10094084, 10094084);
//		addLimitsFor(celibat, couples, 10094085, 10094085);
//		addLimitsFor(celibat, couples, 10094086, 10094086);
//		addLimitsFor(celibat, couples, 10094087, 10094087);
//		addLimitsFor(celibat, couples, 10094088, 10094088);
//		addLimitsFor(celibat, couples, 10094089, 10094089);
//		addLimitsFor(celibat, couples, 10094090, 10094090);
//		addLimitsFor(celibat, couples, 10094090, 10094090);
//		addLimitsFor(celibat, couples, 10094091, 10094091);
//		addLimitsFor(celibat, couples, 10094092, 10094092);
//		addLimitsFor(celibat, couples, 10094097, 10094097);
//		addLimitsFor(celibat, couples, 10094099, 10094099);
//		addLimitsFor(celibat, couples, 10094102, 10094102);
//		addLimitsFor(celibat, couples, 10094106, 10094106);
//		addLimitsFor(celibat, couples, 10094107, 10094107);
//		addLimitsFor(celibat, couples, 10094108, 10094108);
//		addLimitsFor(celibat, couples, 10094110, 10094110);
//		addLimitsFor(celibat, couples, 10094112, 10094112);
//		addLimitsFor(celibat, couples, 10094133, 10094133);
//		addLimitsFor(celibat, couples, 10094146, 10094146);
//		addLimitsFor(celibat, couples, 10094153, 10094153);
//		addLimitsFor(celibat, couples, 10094156, 10094156);
//		addLimitsFor(celibat, couples, 10094157, 10094157);
//		addLimitsFor(celibat, couples, 10094159, 10094159);
//		addLimitsFor(celibat, couples, 10094163, 10094163);
//		addLimitsFor(celibat, couples, 10094163, 10094163);
//		addLimitsFor(celibat, couples, 10094167, 10094167);
//		addLimitsFor(celibat, couples, 10094169, 10094169);
//		addLimitsFor(celibat, couples, 10094175, 10094175);
//		addLimitsFor(celibat, couples, 10094176, 10094176);
//		addLimitsFor(celibat, couples, 10094177, 10094177);
//		addLimitsFor(celibat, couples, 10094177, 10094177);
//		addLimitsFor(celibat, couples, 10094177, 10094177);
//		addLimitsFor(celibat, couples, 10094178, 10094178);
//		addLimitsFor(celibat, couples, 10094194, 10094194);
//		addLimitsFor(celibat, couples, 10094204, 10094204);
//		addLimitsFor(celibat, couples, 10094206, 10094206);
//		addLimitsFor(celibat, couples, 10094207, 10094207);
//		addLimitsFor(celibat, couples, 10094208, 10094208);
//		addLimitsFor(celibat, couples, 10094208, 10094208);
//		addLimitsFor(celibat, couples, 10094213, 10094213);
//		addLimitsFor(celibat, couples, 10094216, 10094216);
//		addLimitsFor(celibat, couples, 10094219, 10094219);
//		addLimitsFor(celibat, couples, 10094222, 10094222);
//		addLimitsFor(celibat, couples, 10094224, 10094224);
//		addLimitsFor(celibat, couples, 10094225, 10094225);
//		addLimitsFor(celibat, couples, 10094226, 10094226);
//		addLimitsFor(celibat, couples, 10094227, 10094227);
//		addLimitsFor(celibat, couples, 10094228, 10094228);
//		addLimitsFor(celibat, couples, 10094229, 10094229);
//		addLimitsFor(celibat, couples, 10094230, 10094230);
//		addLimitsFor(celibat, couples, 10094231, 10094231);
//		addLimitsFor(celibat, couples, 10073796, 10073796);
//		addLimitsFor(celibat, couples, 10073849, 10073849);
//		addLimitsFor(celibat, couples, 10073875, 10073875);
//		addLimitsFor(celibat, couples, 10073875, 10073875);
//		addLimitsFor(celibat, couples, 10073887, 10073887);
//		addLimitsFor(celibat, couples, 10073910, 10073910);
//		addLimitsFor(celibat, couples, 10073910, 10073910);
//		addLimitsFor(celibat, couples, 10073910, 10073910);
//		addLimitsFor(celibat, couples, 10073918, 10073918);
//		addLimitsFor(celibat, couples, 10073918, 10073918);
//		addLimitsFor(celibat, couples, 10073920, 10073920);
//		addLimitsFor(celibat, couples, 10073920, 10073920);
//		addLimitsFor(celibat, couples, 10073982, 10073982);
//		addLimitsFor(celibat, couples, 10074010, 10074010);
//		addLimitsFor(celibat, couples, 10074139, 10074139);
//		addLimitsFor(celibat, couples, 10074140, 10074140);
//		addLimitsFor(celibat, couples, 10074140, 10074140);
//		addLimitsFor(celibat, couples, 10074140, 10074140);
//		addLimitsFor(celibat, couples, 10074227, 10074227);
//		addLimitsFor(celibat, couples, 10074236, 10074236);
//		addLimitsFor(celibat, couples, 10074236, 10074236);
//		addLimitsFor(celibat, couples, 10074236, 10074236);
//		addLimitsFor(celibat, couples, 10074238, 10074238);
//		addLimitsFor(celibat, couples, 10074238, 10074238);
//		addLimitsFor(celibat, couples, 10074270, 10074270);
//		addLimitsFor(celibat, couples, 10074277, 10074277);
//		addLimitsFor(celibat, couples, 10074410, 10074410);
//		addLimitsFor(celibat, couples, 10074410, 10074410);
//		addLimitsFor(celibat, couples, 10074452, 10074452);
//		addLimitsFor(celibat, couples, 10074452, 10074452);
//		addLimitsFor(celibat, couples, 10074452, 10074452);
//		addLimitsFor(celibat, couples, 10079506, 10079506);
//		addLimitsFor(celibat, couples, 10079510, 10079510);
//		addLimitsFor(celibat, couples, 10079516, 10079516);
//		addLimitsFor(celibat, couples, 10079519, 10079519);
//		addLimitsFor(celibat, couples, 10079523, 10079523);
//		addLimitsFor(celibat, couples, 10079525, 10079525);
//		addLimitsFor(celibat, couples, 10079526, 10079526);
//		addLimitsFor(celibat, couples, 10079984, 10079984);
//		addLimitsFor(celibat, couples, 10079996, 10079996);
//		addLimitsFor(celibat, couples, 55904609, 55904609);
//		addLimitsFor(celibat, couples, 10110856, 10110858);
//		addLimitsFor(celibat, couples, 10006618, 10006618);
//		addLimitsFor(celibat, couples, 10007243, 10007860);
//		addLimitsFor(celibat, couples, 10124264, 10124265);
//		addLimitsFor(celibat, couples, 87007406, 87007406);


//		addLimitsFor(celibat, couples, 10152114, 10152114);
//		addLimitsFor(celibat, couples, 10096274, 10096299);
//		addLimitsFor(celibat, couples, 10117723, 10117723);

//		addLimitsFor(celibat, couples, 1, 0);

//		addLimitsFor(celibat, couples, 10000109, 10000109);
//		addLimitsFor(celibat, couples, 10076093, 10076093);
//		addLimitsFor(celibat, couples, 10000001, 10000001);
//		addLimitsFor(celibat, couples, 62803508, 62803508);
//		addLimitsForGilles(celibat, couples, 10100135, 10100135);
//		addLimitsFor(celibat, couples, 10100100, 10100200);


//		l = new MigRegLimits();
//		l.setNbThreads(16);
//		l.setWantCouple(true);
//		l.wantTruncate = false;
//		l.wantIndexation = false;
//		l.setOrdinaire(10000000, 99999999);
//		addConfig(GILLES, l);
		// D'abord les celibat...
		for (MigRegLimits l : celibat) {
			addConfig(GILLES, l);
		}
		// Ensuite les couples
		for (MigRegLimits l : couples) {
			addConfig(GILLES, l);
		}
		getConfig(GILLES).setWantMariesSeuls(false);
		getConfig(GILLES).setWantTruncate(false);
		getConfig(GILLES).setWantTutelles(false);
//		getConfig(GILLES).setIndexationMode(IndexationMode.AT_END);
	}


	private static void addConfigAciTesting() {
		addConfigAciTesting_CFTZ(ACI_TESTING); // CFTZ par défaut
		addConfigAciTesting_CFTZ(ACI_TESTING+"_CFTZ");
		addConfigAciTesting_CJVZ(ACI_TESTING+"_CJVZ");
	}
	private static void addConfigAciTesting_CJVZ(String name) {
		MigRegLimitsList list = addTruncateConfig(name);
		list.setIndexationMode(IndexationMode.NONE);
//		list.setNbThreads(16);

		MigRegLimitsList celibat = new MigRegLimitsList("celibat");
		MigRegLimitsList couples = new MigRegLimitsList("couples");

		addLimitsFor(celibat, couples, 10006600, 10019350);
		addLimitsFor(celibat, couples, 10027705, 10035924);
		addLimitsFor(celibat, couples, 10095087, 10127669);
		addLimitsFor(celibat, couples, 10114900, 10114920);
		addLimitsFor(celibat, couples, 10143855, 10143860);
		addLimitsFor(celibat, couples, 10152240, 10152250);
		addLimitsFor(celibat, couples, 10194002, 10194003);
		addLimitsFor(celibat, couples, 10236982, 10236983);
		addLimitsFor(celibat, couples, 10240380, 10240390);
		addLimitsFor(celibat, couples, 10449700, 10449710);
		addLimitsFor(celibat, couples, 22317700, 22317850);
		addLimitsFor(celibat, couples, 61608100, 61706000);
		addLimitsFor(celibat, couples, 87000000, 88801000);

		// D'abord les celibat...
		for (MigRegLimits l : celibat) {
			addConfig(name, l);
		}
		// Ensuite les couples
		for (MigRegLimits l : couples) {
			addConfig(name, l);
		}
		getConfig(name).setWantTruncate(false);
	}
	private static void addConfigAciTesting_CFTZ(String name) {
//		MigRegLimitsList list = addTruncateConfig(name);
//		list.setIndexationMode(IndexationMode.NONE);
//		list.setNbThreads(16);
//		list.setWantTruncate(true);
//		list.setWantMariesSeuls(true);
//		list.setWantTutelles(true);

		MigRegLimitsList celibat = new MigRegLimitsList("celibat");
		MigRegLimitsList couples = new MigRegLimitsList("couples");

		addLimitsFor(celibat, couples, 10006600, 10019350);
		addLimitsFor(celibat, couples, 10027705, 10035924);
		addLimitsFor(celibat, couples, 10095087, 10127669);
		addLimitsFor(celibat, couples, 10194002, 10194003);
		addLimitsFor(celibat, couples, 10236982, 10236983);
		addLimitsFor(celibat, couples, 22317700, 22317850);
		addLimitsFor(celibat, couples, 61608100, 61706000);
		addLimitsFor(celibat, couples, 87000000, 88801000);

		// D'abord les celibat...
		for (MigRegLimits l : celibat) {
			addConfig(name, l);
		}
		// Ensuite les couples
		for (MigRegLimits l : couples) {
			addConfig(name, l);
		}

//		getConfig(name).setNbThreads(16);
//		getConfig(name).setErrorCasesProcessing(true);
	}

	/**
	 * Configuration pour les tests de performance
	 */
	private static void addConfigPerfs() {
		MigRegLimitsList list = addTruncateConfig(PERFS);
		list.setIndexationMode(IndexationMode.NONE);
//		list.setNbThreads(16);
		list.setWantMariesSeuls(true);
		list.setWantTutelles(true);

		MigRegLimitsList celibat = new MigRegLimitsList("celibat");
		MigRegLimitsList couples = new MigRegLimitsList("couples");

		addLimitsFor(celibat, couples, 10006600, 10019350); // 12500
//		addLimitsFor(celibat, couples, 10027705, 10035924); // 8129
//		addLimitsFor(celibat, couples, 10095087, 10127669); // 32582
//		addLimitsFor(celibat, couples, 10194002, 10194003); // 1
//		addLimitsFor(celibat, couples, 10236982, 10236983); // 1
//		addLimitsFor(celibat, couples, 22317700, 22317850); // 150
//		addLimitsFor(celibat, couples, 61608100, 61706000); // 97900
//		addLimitsFor(celibat, couples, 87000000, 88801000); // 1801000

		// D'abord les celibat...
		for (MigRegLimits l : celibat) {
			addConfig(PERFS, l);
		}
		// Ensuite les couples
		for (MigRegLimits l : couples) {
			addConfig(PERFS, l);
		}
	}

	private static void addLimitsFor(MigRegLimitsList celibat, MigRegLimitsList couples, int first, int last) {
		{
			MigRegLimits l = new MigRegLimits();
			l.setWantCouple(false);
			l.setOrdinaire(first, last);
			celibat.append(l);
		}
		{
			MigRegLimits l = new MigRegLimits();
			l.setWantCouple(true);
			l.setOrdinaire(first, last);
			couples.append(l);
		}
	}

//	private static void addLimitsSourcierForGillesSourcier(MigRegLimitsList sourcier, int first, int last, boolean processing) {
//
//		if (!processing) {
//			MigRegLimits l = new MigRegLimits();
//			l.setSourciers(first, last);
//			sourcier.append(l);
//		}
//		else {
//			MigRegLimits l = new MigRegLimits();
//			l.setSourciers(1, 999999);
//			l.setNoPassSrc(2);
//			sourcier.append(l);
//		}
//	}

	public static void addConfigTestDeCharge() {

		// Permier 10mios
		addChargeConfig(CHARGE1_CTB, CHARGE1_COUPLE, 10000000, 10400000);
		addChargeConfig(CHARGE2_CTB, CHARGE2_COUPLE, 10400001, 10800000);
		addChargeConfig(CHARGE3_CTB, CHARGE3_COUPLE, 10800001, 11200000);

		// Suite de 11mios a 99mios
		// Permier 10mios
		addChargeConfig(CHARGE1_CTB, CHARGE1_COUPLE, 11200001, 38000000);
		addChargeConfig(CHARGE2_CTB, CHARGE2_COUPLE, 38000001, 66000000);
		addChargeConfig(CHARGE3_CTB, CHARGE3_COUPLE, 66000001, 99999999);

//		getConfig(CHARGE1_CTB).setNbThreads(16);
//		getConfig(CHARGE2_CTB).setNbThreads(16);
//		getConfig(CHARGE3_CTB).setNbThreads(16);
//		getConfig(CHARGE1_COUPLE).setErrorCasesProcessing(false);
//		getConfig(CHARGE2_COUPLE).setErrorCasesProcessing(false);
//		getConfig(CHARGE3_COUPLE).setErrorCasesProcessing(false);
//		getConfig(CHARGE1_COUPLE).setNbThreads(16);
//		getConfig(CHARGE2_COUPLE).setNbThreads(16);
//		getConfig(CHARGE3_COUPLE).setNbThreads(16);
		getConfig(CHARGE3_COUPLE).setWantMariesSeuls(true);
		getConfig(CHARGE3_COUPLE).setWantTutelles(true);
	}
	private static void addChargeConfig(String nameCtb, String nameCouple, int firstCtb, int lastCtb) {

		{
			MigRegLimits limits = new MigRegLimits();
			limits.setWantCouple(false);
			limits.setOrdinaire(firstCtb, lastCtb);
			addConfig(nameCtb, limits);
		}
		{
			MigRegLimits limits = new MigRegLimits();
			limits.setWantCouple(true);
			limits.setOrdinaire(firstCtb, lastCtb);
			addConfig(nameCouple, limits);
		}
	}

	public static void addConfigTestDeChargeAll() {

		// Permier 10mios
		addChargeConfigAll(CHARGE_ALL_CTB, CHARGE_ALL_COUPLE, 10000000, 99999998);
//		getConfig(CHARGE_ALL_CTB).setNbThreads(16);
//		getConfig(CHARGE_ALL_COUPLE).setNbThreads(16);
//		getConfig(CHARGE_ALL_CTB).setErrorCasesProcessing(false);
//		getConfig(CHARGE_ALL_COUPLE).setErrorCasesProcessing(false);
		getConfig(CHARGE_ALL_CTB).setWantMariesSeuls(false);
		getConfig(CHARGE_ALL_COUPLE).setWantMariesSeuls(true);
		getConfig(CHARGE_ALL_CTB).setWantTruncate(false);
		getConfig(CHARGE_ALL_COUPLE).setWantTruncate(false);
		getConfig(CHARGE_ALL_CTB).setWantTutelles(false);
		getConfig(CHARGE_ALL_COUPLE).setWantTutelles(true);
		addConfigDroitAcces();
	}
	private static void addChargeConfigAll(String nameCtb, String nameCouple, int firstCtb, int lastCtb) {

		{
			MigRegLimits limits = new MigRegLimits();
			limits.setWantCouple(false);
			limits.setOrdinaire(firstCtb, lastCtb);
			addConfig(nameCtb, limits);
		}
		{
			MigRegLimits limits = new MigRegLimits();
			limits.setWantCouple(true);
			limits.setOrdinaire(firstCtb, lastCtb);
			addConfig(nameCouple, limits);
		}
	}



	private static void addConfig(String name, MigRegLimits l) {

		MigRegLimitsList list = createConfig(name);
		list.append(l);
	}
	private static MigRegLimitsList createConfig(String name) {

		MigRegLimitsList list = configs.get(name);
		if (list == null) {
			list = new MigRegLimitsList(name);
			configs.put(name, list);
		}
		return list;
	}

	private static MigRegLimitsList getConfig(String name) {
		MigRegLimitsList l = configs.get(name);
		return l;
	}

	public static MigRegLimitsList cfg(String name, String env) {
		return cfg(name, env, false);
	}

	public static MigRegLimitsList cfg(String name, String env, boolean errorProcessing) {
		String nameEnv = name+"_"+env;
		MigRegLimitsList l = getConfig(nameEnv);
		if (l == null) {
			// Essaie de trouver une config sans Environnement
			l = getConfig(name);
		}
		logUsageIfNotExists(l, "La définition des limites pour le job "+name+" (env="+env+") n'existe pas");
		LOGGER.info("Récupération des limites : "+l.getName());
//		if (errorProcessing) {
//			l.setErrorCasesProcessing(true);
//		}
		return l;
	}

	private static void logUsageIfNotExists(MigRegLimitsList l, String message) {
		if (l == null) {
			// Dump toutes les limites définies
			// Trie la liste
			ArrayList<String> list = new ArrayList<String>();
			for (String name : configs.keySet()) {
				list.add(name);
			}
			Collections.sort(list);

			LOGGER.error(message);
			LOGGER.error("Configurations disponibles:");
			for (String name : list) {
				LOGGER.error(" * '"+name+"'");
			}
			Assert.fail(message);
		}
	}

	private static MigRegLimitsList addTruncateConfig(String name) {
		// Truncate DB
		MigRegLimitsList list = createConfig(name);
		list.setWantTruncate(true);
		return list;
	}

}
