package ch.vd.uniregctb.civil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.ServiceCivilRaw;
import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.RelationVersIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.rcpers.ServiceCivilRCPers;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.fidor.ServiceInfrastructureFidor;
import ch.vd.unireg.wsclient.rcpers.RcPersClientImpl;
import ch.vd.uniregctb.common.StandardBatchIterator;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.webservice.fidor.v5.FidorClientImpl;

public class TableFillerWithCivilValues {

	private static final String dbDriverClassName = "oracle.jdbc.OracleDriver";

	private static final String dbUrl = "jdbc:oracle:thin:@sso0209v.etat-de-vaud.ch:1526:FUNIREGI";
	private static final String dbUser = "UNIREG";
	private static final String dbPassword = "uniregi_082";

	private static final String RCPERS_URL = "http://rp-ws-va.etat-de-vaud.ch/registres/int-rcpers/west/ws/v4";
	private static final String RCPERS_USER = "gvd0unireg";
	private static final String RCPERS_PWD = "Welc0me_";

	private static final String FIDOR_URL = "http://rp-ws-va.etat-de-vaud.ch/fiscalite/int-fidor/ws/v5";
	private static final String FIDOR_USER = "gvd0unireg";
	private static final String FIDOR_PWD = "Welc0me_";

	private static final String TABLE_NAME = "DUMP_CIVIL";
	private static final int NB_TREADS = 8;
	private static final int BATCH_SIZE = 20;
	private static final String SQL_INSERT = buildInsertSql();

	private final ServiceCivilRaw serviceCivil;

	public static void main(String[] args) throws Exception {
		Class.forName(dbDriverClassName);
		new TableFillerWithCivilValues().run();
	}

	public TableFillerWithCivilValues() throws Exception {

		final RcPersClientImpl rcpersClient = new RcPersClientImpl();
		rcpersClient.setBaseUrl(RCPERS_URL);
		rcpersClient.setUsername(RCPERS_USER);
		rcpersClient.setPassword(RCPERS_PWD);
		rcpersClient.setPeoplePath("persons/ct.vd.rcpers");
		rcpersClient.setRelationsPath("relations/ct.vd.rcpers");
		rcpersClient.afterPropertiesSet();

		final FidorClientImpl fidorClient = new FidorClientImpl();
		fidorClient.setServiceUrl(FIDOR_URL);
		fidorClient.setUsername(FIDOR_USER);
		fidorClient.setPassword(FIDOR_PWD);

		final ServiceInfrastructureFidor infraService = new ServiceInfrastructureFidor();
		infraService.setFidorClientv5(fidorClient);

		final ServiceCivilRCPers serviceCivilRCPers = new ServiceCivilRCPers();
		serviceCivilRCPers.setClient(rcpersClient);
		serviceCivilRCPers.setInfraService(infraService);
		serviceCivil = serviceCivilRCPers;
	}

	private static interface ConnectionCallback<T> {
		T doInConnection(Connection con) throws SQLException;
	}

	private static <T> T doInNewConnection(ConnectionCallback<T> callback) throws SQLException {
		try (Connection con = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
			con.setAutoCommit(false);
			try {
				final T result = callback.doInConnection(con);
				con.commit();
				return result;
			}
			catch (RuntimeException | SQLException e) {
				con.rollback();
				throw e;
			}
		}
	}

	public void run() throws Exception {

		// récupérons tous les numéros d'individus
		final List<Long> nosIndividus = getNumerosIndividus();

		// un peu de log...
		System.err.println("Trouvé " + nosIndividus.size() + " individus distincts");

		// creation de la table
		doInNewConnection(new ConnectionCallback<Object>() {
			@Override
			public Object doInConnection(Connection con) throws SQLException {
				final String sql = buildCreateTableSql();
				try (final PreparedStatement ps = con.prepareCall(sql)) {
					ps.executeUpdate();
				}
				return null;
			}
		});

		final ExecutorService executorService = Executors.newFixedThreadPool(NB_TREADS);

		// faisons des groupes, allons chercher les données et remplissons les infos...
		final List<Future<?>> futures = new LinkedList<>();
		final StandardBatchIterator<Long> iter = new StandardBatchIterator<>(nosIndividus, BATCH_SIZE);
		while (iter.hasNext()) {
			futures.add(executorService.submit(new DataCollector(iter.next())));
		}

		// c'est la fin...
		executorService.shutdown();

		// récupération des éventuelles erreurs et attente de la fin
		for (Future<?> future : futures) {
			try {
				future.get();
			}
			catch (ExecutionException e) {
				final Throwable cause = e.getCause();
				System.err.println("Exception recue: " + cause.getMessage());
				cause.printStackTrace(System.err);
			}
		}
	}

	private List<Long> getNumerosIndividus() throws SQLException {
		return doInNewConnection(new ConnectionCallback<List<Long>>() {
			@Override
			public List<Long> doInConnection(Connection con) throws SQLException {
				try (final PreparedStatement ps = con.prepareStatement("SELECT DISTINCT NUMERO_INDIVIDU FROM TIERS WHERE NUMERO_INDIVIDU IS NOT NULL")) {
					final List<Long> noInds = new LinkedList<>();
					final ResultSet rs = ps.executeQuery();
					while (rs.next()) {
						final long noIndividu = rs.getBigDecimal(1).longValue();
						noInds.add(noIndividu);
					}
					return noInds;
				}
			}
		});
	}

	private class DataCollector implements Runnable {

		private final List<Long> nosIndividus;

		private DataCollector(List<Long> nosIndividus) {
			this.nosIndividus = new ArrayList<>(nosIndividus);
		}

		@Override
		public void run() {
			try {
				final List<Individu> individus = getIndividus(nosIndividus, AttributeIndividu.PARENTS, AttributeIndividu.ADRESSES);
				dumpIndividus(individus, null);
			}
			catch (Exception e) {
				throw new RuntimeException("Erreur avec l'un des individus " + Arrays.toString(nosIndividus.toArray()), e);
			}
		}
	}

	private void dumpIndividus(final List<Individu> individus, String erreur) throws Exception {
		try {
			doInNewConnection(new ConnectionCallback<Object>() {
				@Override
				public Object doInConnection(Connection con) throws SQLException {
					try (final PreparedStatement ps = preparedStatementInsert(con)) {
						for (Individu ind : individus) {
							final List<Long> idsParents = new ArrayList<>(2);
							if (ind.getParents() != null) {
								for (RelationVersIndividu rel : ind.getParents()) {
									idsParents.add(rel.getNumeroAutreIndividu());
								}
							}

							final List<Individu> parents = getIndividus(idsParents);
							insertData(ind, parents, ps);
						}
					}
					return null;
				}
			});
		}
		catch (Exception e) {
			if (individus.size() == 1) {
				// rien à faire de plus, ça pête, ça pête !
				if (erreur != null) {
					// on part en boucle -> il vaut mieux s'arrêter là... ?
					throw e;
				}
				else {
					dumpIndividus(Arrays.asList(buildMockIndividu(individus.get(0).getNoTechnique(), e.getMessage())), e.getMessage());
				}
			}
			else {
				// un par un
				for (Individu ind : individus) {
					dumpIndividus(Arrays.asList(ind), null);
				}
			}
		}
	}

	private List<Individu> getIndividus(List<Long> nosIndividus, AttributeIndividu... parts) {
		try {
			return serviceCivil.getIndividus(nosIndividus, parts);
		}
		catch (Exception e) {
			if (nosIndividus.size() == 1) {
				// rien à faire de plus, ça pête déjà pour lui,,,
				return Arrays.asList(buildMockIndividu(nosIndividus.get(0), e.getMessage()));
			}
			else {
				// un par un
				final List<Individu> res = new ArrayList<>(nosIndividus.size());
				for (Long noIndividu : nosIndividus) {
					res.addAll(getIndividus(Arrays.asList(noIndividu), parts));
				}
				return res;
			}
		}
	}

	private static Individu buildMockIndividu(long noIndividu, String erreur) {
		final MockIndividu ind = new MockIndividu();
		ind.setNoTechnique(noIndividu);
		ind.setNom(StringUtils.abbreviate(erreur, 100));
		return ind;
	}

	private static String buildCreateTableSql() {
		final StringBuilder b = new StringBuilder();
		b.append("CREATE TABLE ").append(TABLE_NAME).append(" (");
		b.append("NO_INDIVIDU NUMBER(19) NOT NULL, ");
		b.append("NOM NVARCHAR2(100), ");
		b.append("PRENOM NVARCHAR2(100), ");
		b.append("DATE_NAISSANCE NUMBER(10), ");
		b.append("SEXE NVARCHAR2(8), ");
		b.append("AVS13 NVARCHAR2(13), ");
		b.append("AVS11 NVARCHAR2(11), ");
		b.append("NPA_SUISSE_CONTACT NUMBER(10), ");
		b.append("NPA_SUISSE_CONTACT_CPLT NUMBER(10), ");
		b.append("NPA_ETR_CONTACT NVARCHAR2(50), ");
		b.append("OFS_PAYS_CONTACT NUMBER(10), ");
		b.append("NPA_SUISSE_RESIDENCE NUMBER(10), ");
		b.append("NPA_SUISSE_RESIDENCE_CPLT NUMBER(10), ");
		b.append("NPA_ETR_RESIDENCE NVARCHAR2(50), ");
		b.append("OFS_PAYS_RESIDENCE NUMBER(10), ");
		b.append("NO_INDIVIDU_PERE NUMBER(19), ");
		b.append("NOM_PERE NVARCHAR2(100), ");
		b.append("PRENOM_PERE NVARCHAR2(100), ");
		b.append("DATE_NAISSANCE_PERE NUMBER(10), ");
		b.append("SEXE_PERE NVARCHAR2(8), ");
		b.append("AVS13_PERE NVARCHAR2(13), ");
		b.append("AVS11_PERE NVARCHAR2(11), ");
		b.append("NO_INDIVIDU_MERE NUMBER(19), ");
		b.append("NOM_MERE NVARCHAR2(100), ");
		b.append("PRENOM_MERE NVARCHAR2(100), ");
		b.append("DATE_NAISSANCE_MERE NUMBER(10), ");
		b.append("SEXE_MERE NVARCHAR2(8), ");
		b.append("AVS13_MERE NVARCHAR2(13), ");
		b.append("AVS11_MERE NVARCHAR2(11), ");
		b.append("PRIMARY KEY (NO_INDIVIDU))");
		return b.toString();
	}

	private static String buildInsertSql() {
		final StringBuilder b = new StringBuilder();
		b.append("INSERT INTO ").append(TABLE_NAME).append(" (");
		b.append("NO_INDIVIDU, ");
		b.append("NOM, ");
		b.append("PRENOM, ");
		b.append("DATE_NAISSANCE, ");
		b.append("SEXE, ");
		b.append("AVS13, ");
		b.append("AVS11, ");
		b.append("NPA_SUISSE_CONTACT, ");
		b.append("NPA_SUISSE_CONTACT_CPLT, ");
		b.append("NPA_ETR_CONTACT, ");
		b.append("OFS_PAYS_CONTACT, ");
		b.append("NPA_SUISSE_RESIDENCE, ");
		b.append("NPA_SUISSE_RESIDENCE_CPLT, ");
		b.append("NPA_ETR_RESIDENCE, ");
		b.append("OFS_PAYS_RESIDENCE, ");
		b.append("NO_INDIVIDU_PERE, ");
		b.append("NOM_PERE, ");
		b.append("PRENOM_PERE, ");
		b.append("DATE_NAISSANCE_PERE, ");
		b.append("SEXE_PERE, ");
		b.append("AVS13_PERE, ");
		b.append("AVS11_PERE, ");
		b.append("NO_INDIVIDU_MERE, ");
		b.append("NOM_MERE, ");
		b.append("PRENOM_MERE, ");
		b.append("DATE_NAISSANCE_MERE, ");
		b.append("SEXE_MERE, ");
		b.append("AVS13_MERE, ");
		b.append("AVS11_MERE");
		b.append(") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		return b.toString();
	}

	private static PreparedStatement preparedStatementInsert(Connection con) throws SQLException {
		return con.prepareStatement(SQL_INSERT);
	}

	private static int insertData(Individu ind, List<Individu> parents, PreparedStatement ps) throws SQLException {
		// l'individu lui-même
		ps.setLong(1, ind.getNoTechnique());
		ps.setString(2, ind.getNom());
		ps.setString(3, ind.getPrenom());
		setRegDateParam(4, ps, ind.getDateNaissance());
		setEnumParam(5, ps, ind.getSexe());
		ps.setString(6, ind.getNouveauNoAVS());
		ps.setString(7, ind.getNoAVS11());

		// son adresse actuelle de contact
		final Adresse contact = getCurrentAdresse(ind.getAdresses(), TypeAdresse.CONTACT);
		if (contact != null) {
			final Integer noOfsPays = contact.getNoOfsPays();
			if (noOfsPays == null || ServiceInfrastructureRaw.noOfsSuisse == noOfsPays) {
				if (StringUtils.isNotBlank(contact.getNumeroPostal())) {
					ps.setInt(8, Integer.parseInt(contact.getNumeroPostal()));
				}
				else {
					ps.setNull(8, Types.INTEGER);
				}
				if (StringUtils.isNotBlank(contact.getNumeroPostalComplementaire())) {
					ps.setInt(9, Integer.parseInt(contact.getNumeroPostalComplementaire()));
				}
				else {
					ps.setNull(9, Types.INTEGER);
				}
				ps.setNull(10, Types.NVARCHAR);
				ps.setInt(11, ServiceInfrastructureRaw.noOfsSuisse);
			}
			else {
				ps.setNull(8, Types.INTEGER);
				ps.setNull(9, Types.INTEGER);
				ps.setString(10, contact.getNumeroPostal());
				ps.setInt(11, noOfsPays);
			}
		}
		else {
			ps.setNull(8, Types.INTEGER);
			ps.setNull(9, Types.INTEGER);
			ps.setNull(10, Types.NVARCHAR);
			ps.setNull(11, Types.INTEGER);
		}

		// son adresse actuelle de résidence
		final Adresse residence = getCurrentAdresse(ind.getAdresses(), TypeAdresse.RESIDENCE);
		if (residence != null) {
			final Integer noOfsPays = residence.getNoOfsPays();
			if (noOfsPays == null || ServiceInfrastructureRaw.noOfsSuisse == noOfsPays) {
				if (StringUtils.isNotBlank(residence.getNumeroPostal())) {
					ps.setInt(12, Integer.parseInt(residence.getNumeroPostal()));
				}
				else {
					ps.setNull(12, Types.INTEGER);
				}
				if (StringUtils.isNotBlank(residence.getNumeroPostalComplementaire())) {
					ps.setInt(13, Integer.parseInt(residence.getNumeroPostalComplementaire()));
				}
				else {
					ps.setNull(13, Types.INTEGER);
				}
				ps.setNull(14, Types.NVARCHAR);
				ps.setInt(15, ServiceInfrastructureRaw.noOfsSuisse);
			}
			else {
				ps.setNull(12, Types.INTEGER);
				ps.setNull(13, Types.INTEGER);
				ps.setString(14, residence.getNumeroPostal());
				ps.setInt(15, noOfsPays);
			}
		}
		else {
			ps.setNull(12, Types.INTEGER);
			ps.setNull(13, Types.INTEGER);
			ps.setNull(14, Types.NVARCHAR);
			ps.setNull(15, Types.INTEGER);
		}

		// le père de l'individu
		final Individu pere = getIndividuSexue(parents, Sexe.MASCULIN);
		if (pere != null) {
			ps.setLong(16, pere.getNoTechnique());
			ps.setString(17, pere.getNom());
			ps.setString(18, pere.getPrenom());
			setRegDateParam(19, ps, pere.getDateNaissance());
			setEnumParam(20, ps, pere.getSexe());
			ps.setString(21, pere.getNouveauNoAVS());
			ps.setString(22, pere.getNoAVS11());
		}
		else {
			ps.setNull(16, Types.INTEGER);
			ps.setNull(17, Types.NVARCHAR);
			ps.setNull(18, Types.NVARCHAR);
			ps.setNull(19, Types.INTEGER);
			ps.setNull(20, Types.NVARCHAR);
			ps.setNull(21, Types.NVARCHAR);
			ps.setNull(22, Types.NVARCHAR);
		}

		// là mère de l'individu
		final Individu mere = getIndividuSexue(parents, Sexe.FEMININ);
		if (mere != null) {
			ps.setLong(23, mere.getNoTechnique());
			ps.setString(24, mere.getNom());
			ps.setString(25, mere.getPrenom());
			setRegDateParam(26, ps, mere.getDateNaissance());
			setEnumParam(27, ps, mere.getSexe());
			ps.setString(28, mere.getNouveauNoAVS());
			ps.setString(29, mere.getNoAVS11());
		}
		else {
			ps.setNull(23, Types.INTEGER);
			ps.setNull(24, Types.NVARCHAR);
			ps.setNull(25, Types.NVARCHAR);
			ps.setNull(26, Types.INTEGER);
			ps.setNull(27, Types.NVARCHAR);
			ps.setNull(28, Types.NVARCHAR);
			ps.setNull(29, Types.NVARCHAR);
		}
		return ps.executeUpdate();
	}

	private static Individu getIndividuSexue(Collection<Individu> candidats, Sexe sexe) {
		if (candidats != null && !candidats.isEmpty()) {
		    for (Individu candidat : candidats) {
			    if (candidat.getSexe() == sexe) {
				    return candidat;
			    }
		    }
		}
		return null;
	}

	private static enum TypeAdresse { CONTACT, RESIDENCE }

	private static Adresse getCurrentAdresse(Collection<Adresse> all, TypeAdresse type) {
		final TypeAdresseCivil typeCivil;
		switch (type) {
			case CONTACT:
				typeCivil = TypeAdresseCivil.COURRIER;
				break;
			case RESIDENCE:
				typeCivil = TypeAdresseCivil.PRINCIPALE;
				break;
			default:
				throw new IllegalArgumentException("Valeur non autorisée : " + type);
		}

		if (all != null) {
			for (Adresse adr : all) {
				if (adr.getTypeAdresse() == typeCivil && adr.isValidAt(null)) {
					return adr;
				}
			}
		}
		return null;
	}

	private static void setRegDateParam(int index, PreparedStatement ps, @Nullable RegDate date) throws SQLException {
		if (date == null) {
			ps.setNull(index, Types.INTEGER);
		}
		else {
			ps.setInt(index, date.index());
		}
	}

	private static <T extends Enum<T>> void setEnumParam(int index, PreparedStatement ps, @Nullable T value) throws SQLException {
		if (value == null) {
			ps.setNull(index, Types.NVARCHAR);
		}
		else {
			ps.setString(index, value.name());
		}
	}
}
