package ch.vd.uniregctb.evenement.externe;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;

import junit.framework.Assert;

import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.junit.Test;

import ch.vd.fiscalite.registre.evenementImpotSourceV1.EvenementImpotSourceQuittanceDocument;
import ch.vd.fiscalite.registre.evenementImpotSourceV1.EvenementImpotSourceQuittanceType;
import ch.vd.fiscalite.registre.evenementImpotSourceV1.EvenementImpotSourceQuittanceType.TypeQuittance;
import ch.vd.uniregctb.common.WithoutSpringTest;

public class EvenementImpotSourceTest extends WithoutSpringTest {

	public static String CURRENT_SCHEMA_VERSION ="1.0";
	public static String CURRENT_SCHEMA_LOCATION="http://www.vd.ch/uniregctb/EvenementImpotSource-1.0.xsd";

	private final static Long NUMERO_CONTRIBUABLE = 12300002L;

	@Test
	public void serialize() throws IOException {
		final EvenementImpotSourceQuittanceDocument document = EvenementImpotSourceQuittanceDocument.Factory.newInstance();
		final EvenementImpotSourceQuittanceType evenementImpotSource = EvenementImpotSourceQuittanceDocument.Factory.newInstance().addNewEvenementImpotSourceQuittance();
		document.setEvenementImpotSourceQuittance(evenementImpotSource);
		evenementImpotSource.setNumeroTiers(String.valueOf(NUMERO_CONTRIBUABLE));
		createQuittance(evenementImpotSource);
		Assert.assertTrue(validate(evenementImpotSource));
		final OutputStream writer = new ByteArrayOutputStream();
		evenementImpotSource.save(writer, new XmlOptions().setSaveOuter().setCharacterEncoding("UTF-8"));
		System.out.println(writer.toString());
	}

	@Test
	public void serializeError() throws IOException {
		final EvenementImpotSourceQuittanceDocument document = EvenementImpotSourceQuittanceDocument.Factory.newInstance();
		final EvenementImpotSourceQuittanceType evenementImpotSource = document.addNewEvenementImpotSourceQuittance();
		evenementImpotSource.setNumeroTiers(String.valueOf(NUMERO_CONTRIBUABLE));
		Assert.assertFalse( validate(evenementImpotSource));
	}


	@Test
	public void deserialize() throws XmlException {
		final String xml = "<v1:evenementImpotSourceQuittance xmlns:v1=\"http://www.vd.ch/fiscalite/registre/evenementImpotSource-v1\"><v1:numeroTiers>1080071</v1:numeroTiers><v1:dateQuittance>2009-07-06</v1:dateQuittance><v1:dateDebutPeriode>2009-01-01</v1:dateDebutPeriode><v1:dateFinPeriode>2009-03-31</v1:dateFinPeriode><v1:typeQuittance>QUITTANCEMENT</v1:typeQuittance></v1:evenementImpotSourceQuittance>";
		final EvenementImpotSourceQuittanceDocument evenementImpotSource = EvenementImpotSourceQuittanceDocument.Factory.parse(xml);
		Assert.assertTrue(validate(evenementImpotSource.getEvenementImpotSourceQuittance()));
	}

	private void createQuittance(EvenementImpotSourceQuittanceType quittance) {
		quittance.setDateDebutPeriode(Calendar.getInstance());
		quittance.setDateFinPeriode(Calendar.getInstance());
		quittance.setDateQuittance(Calendar.getInstance());
		quittance.setTypeQuittance(TypeQuittance.QUITTANCEMENT);
	}

	private boolean  validate( XmlObject object) {
		XmlOptions validateOptions = new XmlOptions();
		ArrayList<XmlError> errorList = new ArrayList<XmlError>();
		validateOptions.setErrorListener(errorList);
		if (!object.validate(validateOptions)) {
			StringBuilder builder = new StringBuilder();
			for (XmlError error : errorList) {
				builder.append("\n");
				builder.append("Message: " + error.getErrorCode() + " " + error.getMessage() + "\n");
				builder.append("Location of invalid XML: " + error.getCursorLocation().xmlText() + "\n");
				System.out.println(builder.toString());
			}
			return false;
		}
		return true;
	}
}
