package ch.vd.uniregctb.webservice.tiers3;

import javax.xml.ws.BindingProvider;
import java.net.URL;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.message.Message;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.ResourceUtils;

import ch.vd.unireg.webservices.tiers3.AccountNumberFormat;
import ch.vd.unireg.webservices.tiers3.BankAccount;
import ch.vd.unireg.webservices.tiers3.Date;
import ch.vd.unireg.webservices.tiers3.PartyWebService;
import ch.vd.unireg.webservices.tiers3.PartyWebServiceFactory;
import ch.vd.uniregctb.common.WebitTest;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

public abstract class AbstractTiersServiceWebTest extends WebitTest {

	private static final Logger LOGGER = Logger.getLogger(AbstractTiersServiceWebTest.class);

	protected static PartyWebService service;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		if (service == null) {
			LOGGER.info("Connecting to: " + tiers3Url + " with user = " + username);

			URL wsdlUrl = ResourceUtils.getURL("classpath:PartyService3.wsdl");
			PartyWebServiceFactory s = new PartyWebServiceFactory(wsdlUrl);
			service = s.getService();

			Map<String, Object> context = ((BindingProvider) service).getRequestContext();
			if (StringUtils.isNotBlank(username)) {
				context.put(BindingProvider.USERNAME_PROPERTY, username);
				context.put(BindingProvider.PASSWORD_PROPERTY, password);
			}
			context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, tiers3Url);

			// Désactive la validation du schéma (= ignore silencieusement les éléments inconnus), de manière à permettre l'évolution ascendante-compatible du WSDL.
			context.put(Message.SCHEMA_VALIDATION_ENABLED, false);
			context.put("set-jaxb-validation-event-handler", false);
		}
	}

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

	private static String format(@Nullable String message, Date expected, Date actual) {
		String formatted = "";
		if (message != null) {
			formatted = message + " ";
		}
		final String stringExpected = (expected == null ? "null" : expected.getYear() + "." + expected.getMonth() + "." + expected.getDay());
		final String stringActual = (actual == null ? "null" : actual.getYear() + "." + actual.getMonth() + "." + actual.getDay());
		return formatted + "expected:<" + stringExpected + "> but was:<" + stringActual + ">";
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

	protected static void assertCompte(String titulaire, String numero, AccountNumberFormat format, BankAccount compte) {
		assertNotNull(compte);
		assertEquals(titulaire, compte.getOwnerName());
		assertEquals(numero, compte.getAccountNumber());
		assertEquals(format, compte.getFormat());
	}

	protected static boolean within(Date d, Date rangeStart, Date rangeEnd) {
		return beforeOrEqual(rangeStart, d) && (rangeEnd == null || beforeOrEqual(d, rangeEnd));
	}

	protected static boolean beforeOrEqual(Date left, Date right) {
		if (left.getYear() == right.getYear()) {
			if (left.getMonth() == right.getMonth()) {
				return left.getDay() <= right.getDay();
			}
			else {
				return left.getMonth() < right.getMonth();
			}
		}
		else {
			return left.getYear() < right.getYear();
		}
	}
}
