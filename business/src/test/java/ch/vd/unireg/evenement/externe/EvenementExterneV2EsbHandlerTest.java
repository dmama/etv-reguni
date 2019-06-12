package ch.vd.unireg.evenement.externe;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.math.BigInteger;
import java.util.Date;

import org.junit.Test;
import org.w3c.dom.Document;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.xml.event.lr.event.v2.CaracteristiquesDebiteur;
import ch.vd.unireg.xml.event.lr.event.v2.CaracteristiquesListe;
import ch.vd.unireg.xml.event.lr.event.v2.DebiteurQualification;
import ch.vd.unireg.xml.event.lr.event.v2.Evenement;
import ch.vd.unireg.xml.event.lr.event.v2.EvtListe;
import ch.vd.unireg.xml.event.lr.event.v2.Liste;
import ch.vd.unireg.xml.event.lr.event.v2.ObjectFactory;
import ch.vd.unireg.xml.event.lr.event.v2.PeriodeDeclaration;
import ch.vd.unireg.xml.event.lr.event.v2.Utilisateur;

import static org.junit.Assert.assertNotNull;

public class EvenementExterneV2EsbHandlerTest extends AbstractEvenementExterneEsbHandlerTest {

	@Override
	protected EvenementExterneConnector<?> getTestedConnector() {
		return new EvtListeV2Connector();
	}

	@Test
	public void testEventQuittancement() throws Exception {
		doTestNewEventQuittancement((dpiId, dateDebut, dateFin, dateQuittancement) -> createMessageQuittancement(dpiId, dateDebut, dateFin, dateQuittancement));
	}

	@Test
	public void testEventLc() throws Exception {
		doTestNewEventLC((dpiId, dateDebut, dateFin, dateQuittancement) -> createMessageLc(dpiId, dateDebut, dateFin, dateQuittancement));
	}

	@Test
	public void testEventAnnulationEtatRetourInexistant() throws Exception {
		doTestNewEventAnnulationEtatRetourInexistant((dpiId, dateDebut, dateFin, dateQuittancement) -> createMessageAnnulationQuittancement(dpiId, dateDebut, dateFin, dateQuittancement));
	}

	@Test
	public void testEventDoubleAnnulation() throws Exception {
		doTestNewEventDoubleAnnulation((dpiId, dateDebut, dateFin, dateQuittancement) -> createMessageAnnulationQuittancement(dpiId, dateDebut, dateFin, dateQuittancement));
	}

	@Test
	public void testEventAnnulation() throws Exception {
		doTestNewEventAnnulation((dpiId, dateDebut, dateFin, dateQuittancement) -> createMessageAnnulationQuittancement(dpiId, dateDebut, dateFin, dateQuittancement));
	}

	@Test
	public void testEvenementDoubleQuittancement() throws Exception {
		doTestNewEvenementDoubleQuittancement((dpiId, dateDebut, dateFin, dateQuittancement) -> createMessageQuittancement(dpiId, dateDebut, dateFin, dateQuittancement));
	}

	@Test
	public void testEvenementAnnulationDoubleQuittancement() throws Exception {
		doTestNewEvenementAnnulationDoubleQuittancement((dpiId, dateDebut, dateFin, dateQuittancement) -> createMessageAnnulationQuittancement(dpiId, dateDebut, dateFin, dateQuittancement));
	}

	private EsbMessage createMessageQuittancement(long noCtb, RegDate debutPeriode, RegDate finPeriode, RegDate dateEvenement) throws Exception {
		return createEsbMessage(createEvenementQuittancement(Evenement.QUITTANCE, noCtb, Liste.LR, debutPeriode, finPeriode, dateEvenement));
	}

	private EsbMessage createMessageAnnulationQuittancement(long noCtb, RegDate debutPeriode, RegDate finPeriode, RegDate dateEvenement) throws Exception {
		return createEsbMessage(createEvenementQuittancement(Evenement.ANNULATION, noCtb, Liste.LR, debutPeriode, finPeriode, dateEvenement));
	}

	private EsbMessage createMessageLc(long noCtb, RegDate debutPeriode, RegDate finPeriode, RegDate dateEvenement) throws Exception {
		return createEsbMessage(createEvenementQuittancement(Evenement.QUITTANCE, noCtb, Liste.LC, debutPeriode, finPeriode, dateEvenement));
	}

	private static EvtListe createEvenementQuittancement(Evenement quitancement, Long numeroCtb, Liste listeType, RegDate dateDebut,
	                                                     RegDate dateFin, RegDate dateEvenement) {

		assertNotNull("le type de quittancement est obligation", quitancement);
		assertNotNull("Le numero du débiteur est obligatoire", numeroCtb);
		assertNotNull("la date du début du récapitulatif est obligatoire", dateDebut);
		assertNotNull("la date de quittancement du récapitulatif est obligatoire", dateEvenement);

		final String businessId = "123456789";

		final PeriodeDeclaration periodeDeclaration = new PeriodeDeclaration();
		periodeDeclaration.setDateDebut(XmlUtils.regdate2xmlcal(dateDebut));
		if (dateFin != null) {
			periodeDeclaration.setDateFin(XmlUtils.regdate2xmlcal(dateFin));
		}

		final CaracteristiquesListe identification = new CaracteristiquesListe();
		identification.setTypeListe(listeType);
		identification.setNumeroSequence(BigInteger.valueOf(1));
		identification.setPeriodeDeclaration(periodeDeclaration);
		identification.setPeriodeFiscale(BigInteger.valueOf(dateDebut.year()));

		final CaracteristiquesDebiteur debiteur = new CaracteristiquesDebiteur();
		debiteur.setNumeroDebiteur(numeroCtb.intValue());
		debiteur.setTypeDebiteur(DebiteurQualification.REG);

		final EvtListe evenement = new EvtListe();
		evenement.setUtilisateur(new Utilisateur("testuser", 22));
		evenement.setCaracteristiquesListe(identification);
		evenement.setTypeEvenement(quitancement);
		evenement.setCaracteristiquesDebiteur(debiteur);
		evenement.setCodeApplication("test");
		evenement.setVersionApplication("0.0");
		evenement.setTimestamp(XmlUtils.date2xmlcal(new Date()));
		evenement.setBusinessId(businessId);
		evenement.setDateEvenement(XmlUtils.regdate2xmlcal(dateEvenement));

		return evenement;
	}

	private EsbMessage createEsbMessage(EvtListe event) throws Exception {
		JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		Marshaller marshaller = context.createMarshaller();

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.newDocument();
		marshaller.marshal(event, doc);

		final EsbMessage m = EsbMessageFactory.createMessage();
		m.setBody(doc);

		return m;
	}
}
