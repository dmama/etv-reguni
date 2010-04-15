package ch.vd.uniregctb.interfaces;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.sql.DataSource;

import ch.vd.uniregctb.common.TestingConstants;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.ContextConfiguration;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.common.AbstractSpringTest;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.HistoriqueIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilApireg;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;

/**
 * Vérifie que les informations retournées par les services civils 'host' et 'apireg' sont équivalentes
 */
@ContextConfiguration(locations = {
	TestingConstants.UNIREG_CORE_UT_PROPERTIES,
	"classpath:ch/vd/uniregctb/interfaces/ServiceCivilApiregTest.xml"
})
public class ServiceCivilApiregTest extends AbstractSpringTest {

	private static class AdresseComparator implements Comparator<Adresse> {
		public int compare(Adresse o1, Adresse o2) {
			if (o1.getTypeAdresse().equals(o2.getTypeAdresse())) {
				RegDate d1 = o1.getDateDebutValidite();
				RegDate d2 = o2.getDateDebutValidite();

				if (d1 == null && d2 == null) {
					return 0;
				}
				if (d1 == null) {
					return -1;
				}
				if (d2 == null) {
					return 1;
				}
				return d1.compareTo(d2);
			}
			else {
				return o1.getTypeAdresse().compareTo(o2.getTypeAdresse());
			}
		}
	}

	private static final int NO_IND_LAURENT_SCHMIDT = 505858;
	private static final int NO_IND_DANIEL_BRELAZ = 583678;
	private static final int NO_IND_CAMILLE_JAQUIER = 750300;
	private static final int NO_IND_HORS_SUISSE = 814190;

	private ServiceCivilService serviceHost;
	private ServiceCivilApireg serviceApireg;
	private DataSource db2DataSource;

	private static final AdresseComparator adresseComparator = new AdresseComparator();

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		serviceHost = getBean(ServiceCivilService.class, "serviceCivilService");
		serviceApireg = getBean(ServiceCivilApireg.class, "serviceCivilApireg");
		db2DataSource = getBean(DataSource.class, "hostDataSourceDB2");
	}

	@Test
	public void testGetIndividu() {

		final Individu laurentHost = serviceHost.getIndividu(NO_IND_LAURENT_SCHMIDT, 2010);
		final Individu laurentApireg = serviceApireg.getIndividu(NO_IND_LAURENT_SCHMIDT, 2010);
		assertIndEquals(laurentHost, laurentApireg, null);

		final Individu danielHost = serviceHost.getIndividu(NO_IND_DANIEL_BRELAZ, 2010);
		final Individu danielApireg = serviceApireg.getIndividu(NO_IND_DANIEL_BRELAZ, 2010);
		assertIndEquals(danielHost, danielApireg, null);

		final Individu camilleHost = serviceHost.getIndividu(NO_IND_CAMILLE_JAQUIER, 2010);
		final Individu camilleApireg = serviceApireg.getIndividu(NO_IND_CAMILLE_JAQUIER, 2010);
		assertIndEquals(camilleHost, camilleApireg, null);

		final Individu hsHost = serviceHost.getIndividu(NO_IND_HORS_SUISSE, 2010);
		final Individu hsApireg = serviceApireg.getIndividu(NO_IND_HORS_SUISSE, 2010);
		assertIndEquals(hsHost, hsApireg, null);
	}

	@Test
	public void testGetAdresses() {

		final AdressesCiviles laurentHost = serviceHost.getAdresses(NO_IND_LAURENT_SCHMIDT, RegDate.get(2009, 7, 2));
		final AdressesCiviles laurentApireg = serviceApireg.getAdresses(NO_IND_LAURENT_SCHMIDT, RegDate.get(2009, 7, 2));
		assertAdrEquals(laurentHost, laurentApireg);
		assertEqualsAllAdressesHostApireg(NO_IND_LAURENT_SCHMIDT, null);

		final AdressesCiviles danielHost = serviceHost.getAdresses(NO_IND_DANIEL_BRELAZ, RegDate.get(2009, 7, 2));
		final AdressesCiviles danielApireg = serviceApireg.getAdresses(NO_IND_DANIEL_BRELAZ, RegDate.get(2009, 7, 2));
		assertAdrEquals(danielHost, danielApireg);
		assertEqualsAllAdressesHostApireg(NO_IND_DANIEL_BRELAZ, null);

		// [UNIREG-474] cas particulier au niveau des dates
		final AdressesCiviles camilleHost = serviceHost.getAdresses(NO_IND_CAMILLE_JAQUIER, RegDate.get(2009, 7, 2));
		final AdressesCiviles camilleApireg = serviceApireg.getAdresses(NO_IND_CAMILLE_JAQUIER, RegDate.get(2009, 7, 2));
		assertAdrEquals(camilleHost, camilleApireg);
		assertEqualsAllAdressesHostApireg(NO_IND_CAMILLE_JAQUIER, null);

		final AdressesCiviles hsHost = serviceHost.getAdresses(NO_IND_HORS_SUISSE, RegDate.get(2009, 11, 1));
		final AdressesCiviles hsApireg = serviceApireg.getAdresses(NO_IND_HORS_SUISSE, RegDate.get(2009, 11, 1));
		assertAdrEquals(hsHost, hsApireg);
		assertEqualsAllAdressesHostApireg(NO_IND_HORS_SUISSE, null);
	}

	@Test
	public void testGetAdressesCiviles() {

		// Individu avec des adresse annulées
		final AdressesCiviles hsHost = serviceHost.getAdresses(618818, null);
		final AdressesCiviles hsApireg = serviceApireg.getAdresses(618818, null);
		assertAdrEquals(hsHost, hsApireg);
	}

	@SuppressWarnings("unchecked")
	//@Test - trop long à tourner à chaque coup
	public void testAllDatabase() {

		final String sql = "select distinct FK_INDNO from CIIV1.ADR_INDIVIDU order by FK_INDNO";

		//Connection connection = db2DataSource.getConnection();
		JdbcTemplate template = new JdbcTemplate(db2DataSource);
		RowMapper rowMapper = new RowMapper() {
			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getLong(1);
			}
		};

		final List<Long> individus = template.query(sql, rowMapper);
		assertNotNull(individus);

		for (Long ind : individus) {
			final String message = "individu n°" + ind;

			final Individu indHost = serviceHost.getIndividu(ind, 2009);
			final Individu indApireg = serviceApireg.getIndividu(ind, 2009);
			assertIndEquals(indHost, indApireg, message);

			assertEqualsAllAdressesHostApireg(ind, message);
		}
	}

	private void assertEqualsAllAdressesHostApireg(long ind, final String message) {
		final List<Adresse> adressesHost = new ArrayList<Adresse>(serviceHost.getAdresses(ind, 2400));
		final List<Adresse> adressesApireg = new ArrayList<Adresse>(serviceApireg.getAdresses(ind, 2400));
		assertEquals(message, adressesHost.size(), adressesApireg.size());

		Collections.sort(adressesHost, adresseComparator);
		Collections.sort(adressesApireg, adresseComparator);

		for (int i = 0; i < adressesHost.size(); ++i) {
			final Adresse ah = adressesHost.get(i);
			final Adresse aa = adressesApireg.get(i);
			assertAdrEquals(ah, aa, message);
		}
	}

	private static void assertAdrEquals(AdressesCiviles expected, AdressesCiviles actual) {

		assertNotNull(expected);
		assertNotNull(actual);

		assertAdrEquals(expected.courrier, actual.courrier, null);
		assertAdrEquals(expected.principale, actual.principale, null);
		assertAdrEquals(expected.secondaire, actual.secondaire, null);
		assertAdrEquals(expected.tutelle, actual.tutelle, null);
	}

	private static void assertAdrEquals(Adresse expected, Adresse actual, String message) {
		if (expected == null || actual == null) {
			assertEquals(expected, actual);
			return;
		}
		assertEquals(message, expected.getCasePostale(), actual.getCasePostale());
		assertEquals(message, expected.getDateDebutValidite(), actual.getDateDebutValidite());
		assertEquals(message, expected.getDateFinValidite(), actual.getDateFinValidite());
		assertStringEquals(expected.getLocalite(), actual.getLocalite(), message);
		assertEquals(message, expected.getNoOfsPays(), actual.getNoOfsPays());
		assertStringEquals(expected.getNumero(), actual.getNumero(), message);
		// host-interfaces ne renseigne pas le numéro d'appartement : assertEquals(message, expected.getNumeroAppartement(), actual.getNumeroAppartement());
		assertEquals(message, expected.getNumeroOrdrePostal(), actual.getNumeroOrdrePostal());
		assertEquals(message, expected.getNumeroPostal(), actual.getNumeroPostal());
		assertNumeroEquals(expected.getNumeroPostalComplementaire(), actual.getNumeroPostalComplementaire(), message);
		assertNumeroEquals(expected.getNumeroRue(), actual.getNumeroRue(), message);
		assertStringEquals(expected.getRue(), actual.getRue(), message);
		assertStringEquals(expected.getTitre(), actual.getTitre(), message);
		assertEquals(message, expected.getTypeAdresse(), actual.getTypeAdresse());
	}

	private static void assertIndEquals(final Individu expected, final Individu actual, String message) {

		assertNotNull(message, expected);
		assertNotNull(message, actual);

		assertEquals(message, expected.getDateDeces(), actual.getDateDeces());
		assertEquals(message, expected.getDateNaissance(), actual.getDateNaissance());

		final EtatCivil expectedEtatCivil = expected.getEtatCivilCourant();
		final EtatCivil actualEtatCivil = actual.getEtatCivilCourant();
		if (expectedEtatCivil == null || actualEtatCivil == null) {
			assertEquals(message, expectedEtatCivil, actualEtatCivil);
		}
		else {
			assertEquals(message, expectedEtatCivil.getDateDebutValidite(), actualEtatCivil.getDateDebutValidite());
			assertEquals(message, expectedEtatCivil.getNoSequence(), actualEtatCivil.getNoSequence());
			assertEquals(message, expectedEtatCivil.getTypeEtatCivil(), actualEtatCivil.getTypeEtatCivil());
		}

		assertEquals(message, expected.getNoTechnique(), actual.getNoTechnique());
		assertEquals(message, expected.getNouveauNoAVS(), actual.getNouveauNoAVS());
		// not implemented in apireg : assertEquals(actual.getNumeroRCE(), expected.getNumeroRCE());
		// not implemented in apireg : assertEquals(actual.getOrigine(), expected.getOrigine());

		final HistoriqueIndividu actualHisto = actual.getDernierHistoriqueIndividu();
		final HistoriqueIndividu expectedHisto = expected.getDernierHistoriqueIndividu();
		assertNotNull(message, actualHisto);
		assertNotNull(message, expectedHisto);
		assertStringEquals(expectedHisto.getAutresPrenoms(), actualHisto.getAutresPrenoms(), message);
		assertStringEquals(expectedHisto.getComplementIdentification(), actualHisto.getComplementIdentification(), message);
		assertEquals(message, expectedHisto.getDateDebutValidite(), actualHisto.getDateDebutValidite());
		assertNumeroEquals(expectedHisto.getNoAVS(), actualHisto.getNoAVS(), message);
		assertEquals(message, expectedHisto.getNom(), actualHisto.getNom());
		// not implemented in apireg : assertEquals(expectedHisto.getNomCourrier1(), expectedActual.getNomCourrier1());
		// not implemented in apireg : assertEquals(expectedHisto.getNomCourrier2(), expectedActual.getNomCourrier2());
		assertStringEquals(expectedHisto.getNomNaissance(), actualHisto.getNomNaissance(), message);
		assertEquals(message, expectedHisto.getNoSequence(), actualHisto.getNoSequence());
		assertEquals(message, expectedHisto.getPrenom(), actualHisto.getPrenom());
		assertStringEquals(expectedHisto.getProfession(), actualHisto.getProfession(), message);
	}

	private static void assertStringEquals(final String expected, final String actual, String message) {
		if ((expected == null || "".equals(expected)) && (actual == null || "".equals(actual))) {
			return;
		}
		assertEquals(message, expected, actual);
	}

	/**
	 * Vérifie l'égalité entre deux numéros, tout en interprétant les numéros 0 comme des valeurs nulles.
	 */
	private static void assertNumeroEquals(final String expected, final String actual, String message) {
		if ((expected == null || "".equals(expected) || "0".equals(expected))
				&& (actual == null || "".equals(actual) || "0".equals(actual))) {
			return;
		}
		assertEquals(message, expected, actual);
	}

	/**
	 * Vérifie l'égalité entre deux numéros, tout en interprétant les numéros 0 comme des valeurs nulles.
	 */
	private static void assertNumeroEquals(final Integer expected, final Integer actual, String message) {
		if ((expected == null || expected.intValue() == 0) && (actual == null || actual.intValue() == 0)) {
			return;
		}
		assertEquals(message, expected, actual);
	}
}
