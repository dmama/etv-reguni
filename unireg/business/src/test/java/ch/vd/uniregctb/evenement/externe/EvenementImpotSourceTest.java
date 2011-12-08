package ch.vd.uniregctb.evenement.externe;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;

import junit.framework.Assert;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.junit.Test;

import ch.vd.fiscalite.taxation.evtQuittanceListeV1.EvtQuittanceListeDocument;
import ch.vd.fiscalite.taxation.evtQuittanceListeV1.ListeType;
import ch.vd.fiscalite.taxation.evtQuittanceListeV1.OrigineType;
import ch.vd.fiscalite.taxation.evtQuittanceListeV1.QuittanceType;
import ch.vd.infrastructure.model.impl.DateUtils;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;

public class EvenementImpotSourceTest extends WithoutSpringTest {

	private final static Long NUMERO_CONTRIBUABLE = 1300002L;

	@Test
	public void serialize() throws IOException {
		final EvtQuittanceListeDocument document = EvtQuittanceListeDocument.Factory.newInstance();
		final EvtQuittanceListeDocument.EvtQuittanceListe evenement = document.addNewEvtQuittanceListe();
		createQuittance(evenement,NUMERO_CONTRIBUABLE);
		document.setEvtQuittanceListe(evenement);

		Assert.assertTrue(validate(evenement));
		final OutputStream writer = new ByteArrayOutputStream();
		evenement.save(writer, new XmlOptions().setSaveOuter().setCharacterEncoding("UTF-8"));
		//System.out.println(writer.toString());
	}

	@Test
	public void serializeError() throws IOException {
		final EvtQuittanceListeDocument document = EvtQuittanceListeDocument.Factory.newInstance();
		final EvtQuittanceListeDocument.EvtQuittanceListe evenement = document.addNewEvtQuittanceListe();
		Assert.assertFalse(validate(evenement));
	}


	@Test
	public void deserialize() throws XmlException {
		final String xml = "<evtQuittanceListe xmlns=\"http://www.vd.ch/fiscalite/taxation/evtQuittanceListe-v1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.vd.ch/fiscalite/taxation/evtQuittanceListe-v1:\\repositories\\SVN_ACI\\taotra\\050_Conception\\technique\\xsd\\quittance\\evtQuittanceListe-v1.xsd\">\n" +
				"\t<timestampEvtQuittance>2001-12-17T09:30:47.0Z</timestampEvtQuittance>\n" +
				"\t<typeEvtQuittance>QUITTANCEMENT</typeEvtQuittance>\n" +
				"\t<origineListe>ELECTRONIQUE</origineListe>\n" +
				"\t<identificationListe>\n" +
				"\t\t<typeListe>LR</typeListe>\n" +
				"\t\t<numeroSequence>1</numeroSequence>\n" +
				"\t\t<numeroDebiteur>1300002</numeroDebiteur>\n" +
				"\t\t<periodeDeclaration>\n" +
				"\t\t\t<dateDebut>1967-08-13</dateDebut>\n" +
				"\t\t\t<dateFin>1967-08-13</dateFin>\n" +
				"\t\t</periodeDeclaration>\n" +
				"\t</identificationListe>\n" +
				"</evtQuittanceListe>";
		final EvtQuittanceListeDocument document = EvtQuittanceListeDocument.Factory.parse(xml);
		Assert.assertTrue(validate(document.getEvtQuittanceListe()));
	}

	private void createQuittance(EvtQuittanceListeDocument.EvtQuittanceListe evenement, Long numeroDebiteur) {
		final EvtQuittanceListeDocument.EvtQuittanceListe.IdentificationListe identification = evenement.addNewIdentificationListe();
		identification.setNumeroDebiteur(numeroDebiteur.intValue());
		final EvtQuittanceListeDocument.EvtQuittanceListe.IdentificationListe.PeriodeDeclaration periodeDeclaration = identification.addNewPeriodeDeclaration();
		final Calendar datedebutC = DateUtils.calendar(RegDate.get().asJavaDate());
		periodeDeclaration.setDateDebut(datedebutC);

		final Calendar dateFinC = DateUtils.calendar(RegDate.get().asJavaDate());
		periodeDeclaration.setDateFin(dateFinC);
		identification.setPeriodeDeclaration(periodeDeclaration);
		identification.setTypeListe(ListeType.LR);
		identification.setNumeroSequence(new BigInteger("1"));
		evenement.setIdentificationListe(identification);
		evenement.setTypeEvtQuittance(QuittanceType.QUITTANCEMENT);
		final Calendar dateEvenement = DateUtils.calendar(RegDate.get().asJavaDate());
		evenement.setTimestampEvtQuittance(dateEvenement);
		evenement.setOrigineListe(OrigineType.ELECTRONIQUE);

	}

	private boolean  validate( XmlObject object) {
		XmlOptions validateOptions = new XmlOptions();
		ArrayList<XmlError> errorList = new ArrayList<XmlError>();
		validateOptions.setErrorListener(errorList);
		if (!object.validate(validateOptions)) {
//			StringBuilder builder = new StringBuilder();
//			for (XmlError error : errorList) {
//				builder.append("\n");
//				builder.append("Message: ").append(error.getErrorCode()).append(" ").append(error.getMessage()).append("\n");
//				builder.append("Location of invalid XML: ").append(error.getCursorLocation().xmlText()).append("\n");
//				System.out.println(builder.toString());
//			}
			return false;
		}
		return true;
	}
}
