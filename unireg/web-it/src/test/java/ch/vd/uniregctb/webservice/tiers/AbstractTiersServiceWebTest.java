package ch.vd.uniregctb.webservice.tiers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.ws.BindingProvider;

import org.apache.log4j.Logger;

import ch.vd.uniregctb.common.WebitTest;
import ch.vd.uniregctb.webservices.tiers.CompteBancaire;
import ch.vd.uniregctb.webservices.tiers.Date;
import ch.vd.uniregctb.webservices.tiers.FormatNumeroCompte;
import ch.vd.uniregctb.webservices.tiers.TiersPort;
import ch.vd.uniregctb.webservices.tiers.TiersService;

public abstract class AbstractTiersServiceWebTest extends WebitTest {

	private static final Logger LOGGER = Logger.getLogger(AbstractTiersServiceWebTest.class);

	protected static TiersPort service;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		if (service == null) {
			LOGGER.info("Connecting to: " + tiers1Url + " with user = " + username);

			TiersService s = new TiersService();
			service = s.getTiersPortPort();

			Map<String, Object> context = ((BindingProvider) service).getRequestContext();
			if (username != null) {
				context.put(BindingProvider.USERNAME_PROPERTY, username);
				context.put(BindingProvider.PASSWORD_PROPERTY, password);
			}
			context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, tiers1Url);
		}
	}

	private static final Pattern valiPattern = Pattern.compile("( *---.{4}-)");

	public static Date newDate(int year, int month, int day) {
		final Date date = new Date();
		date.setYear(year);
		date.setMonth(month);
		date.setDay(day);
		return date;
	}

	public static void assertSameDay(Date left, Date right) {
		final boolean sameDay = sameDay(left, right);
		if (!sameDay) {
			fail(format(null, left, right));
		}
	}

	public static void assertSameDay(String message, Date left, Date right) {
		final boolean sameDay = sameDay(left, right);
		if (!sameDay) {
			fail(format(message, left, right));
		}
	}

	private static String format(String message, Date expected, Date actual) {
		String formatted = "";
		if (message != null) {
			formatted = message + " ";
		}
		final String stringExpected = (expected == null ? "null" : expected.getYear() + "." + expected.getMonth() + "." + expected.getDay());
		final String stringActual = (actual == null ? "null" : actual.getYear() + "." + actual.getMonth() + "." + actual.getDay());
		return formatted + "expected:<" + stringExpected + "> but was:<" + stringActual + ">";
	}

	/**
	 * Supprime l'éventuel pattern "---VALI-" ou "---TEST-" ajouté aux DB de validation/test.
	 */
	public static String trimValiPattern(String string) {
		if (string == null) {
			return null;
		}
		else {
			return valiPattern.matcher(string).replaceAll("");
		}
	}

	public static boolean sameDay(Date left, Date right) {
		final boolean sameDay;
		if (left != null && right != null) {
			sameDay = (left.getDay() == right.getDay() && left.getMonth() == right.getMonth() && left.getYear() == right.getYear());
		}
		else {
			// assert both dates are null (or not null, but the case is treated above)
			sameDay = (left == null && right == null);
		}
		return sameDay;
	}

	protected static void assertCompte(String titulaire, String numero, FormatNumeroCompte format, CompteBancaire compte) {
		assertNotNull(compte);
		assertEquals(titulaire, compte.getTitulaire());
		assertEquals(numero, compte.getNumero());
		assertEquals(format, compte.getFormat());
	}
}
