package ch.vd.uniregctb.evenement.externe;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.math.BigInteger;
import java.util.Date;

import org.junit.Test;
import org.w3c.dom.Document;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.unireg.xml.event.lr.event.v3.CaracteristiquesDebiteur;
import ch.vd.unireg.xml.event.lr.event.v3.CaracteristiquesListe;
import ch.vd.unireg.xml.event.lr.event.v3.Evenement;
import ch.vd.unireg.xml.event.lr.event.v3.EvtListe;
import ch.vd.unireg.xml.event.lr.event.v3.Liste;
import ch.vd.unireg.xml.event.lr.event.v3.ObjectFactory;
import ch.vd.unireg.xml.event.lr.event.v3.PeriodeDeclaration;
import ch.vd.unireg.xml.event.lr.event.v3.Utilisateur;
import ch.vd.uniregctb.common.XmlUtils;

import static org.junit.Assert.assertNotNull;

public class EvenementExterneV3EsbHandlerTest extends AbstractEvenementExterneEsbHandlerTest {

	@Override
	protected EvenementExterneConnector<?> getTestedConnector() {
		return new EvtListeV3Connector();
	}

	@Test
	public void testEventQuittancement() throws Exception {
		doTestNewEventQuittancement(new MessageCreator() {
			@Override
			public EsbMessage createNewMessage(long dpiId, RegDate dateDebut, RegDate dateFin, RegDate dateQuittancement) throws Exception {
				return createMessageQuittancement(dpiId, dateDebut, dateFin, dateQuittancement);
			}
		});
	}

	@Test
	public void testEventLc() throws Exception {
		doTestNewEventLC(new MessageCreator() {
			@Override
			public EsbMessage createNewMessage(long dpiId, RegDate dateDebut, RegDate dateFin, RegDate dateQuittancement) throws Exception {
				return createMessageLc(dpiId, dateDebut, dateFin, dateQuittancement);
			}
		});
	}

	@Test
	public void testEventAnnulationEtatRetourInexistant() throws Exception {
		doTestNewEventAnnulationEtatRetourInexistant(new MessageCreator() {
			@Override
			public EsbMessage createNewMessage(long dpiId, RegDate dateDebut, RegDate dateFin, RegDate dateQuittancement) throws Exception {
				return createMessageAnnulationQuittancement(dpiId, dateDebut, dateFin, dateQuittancement);
			}
		});
	}

	@Test
	public void testEventDoubleAnnulation() throws Exception {
		doTestNewEventDoubleAnnulation(new MessageCreator() {
			@Override
			public EsbMessage createNewMessage(long dpiId, RegDate dateDebut, RegDate dateFin, RegDate dateQuittancement) throws Exception {
				return createMessageAnnulationQuittancement(dpiId, dateDebut, dateFin, dateQuittancement);
			}
		});
	}

	@Test
	public void testEventAnnulation() throws Exception {
		doTestNewEventAnnulation(new MessageCreator() {
			@Override
			public EsbMessage createNewMessage(long dpiId, RegDate dateDebut, RegDate dateFin, RegDate dateQuittancement) throws Exception {
				return createMessageAnnulationQuittancement(dpiId, dateDebut, dateFin, dateQuittancement);
			}
		});
	}

	@Test
	public void testEvenementDoubleQuittancement() throws Exception {
		doTestNewEvenementDoubleQuittancement(new MessageCreator() {
			@Override
			public EsbMessage createNewMessage(long dpiId, RegDate dateDebut, RegDate dateFin, RegDate dateQuittancement) throws Exception {
				return createMessageQuittancement(dpiId, dateDebut, dateFin, dateQuittancement);
			}
		});
	}

	@Test
	public void testEvenementQuittancementDesynchroHeure() throws Exception {
		doTestNewEvenementQuittancementDesynchroHeure(new MessageCreator() {
			@Override
			public EsbMessage createNewMessage(long dpiId, RegDate dateDebut, RegDate dateFin, RegDate dateQuittancement) throws Exception {
				return createMessageQuittancementDansFutur(dpiId, dateDebut, dateFin);
			}
		});
	}

	@Test
	public void testEvenementAnnulationDoubleQuittancement() throws Exception {
		doTestNewEvenementAnnulationDoubleQuittancement(new MessageCreator() {
			@Override
			public EsbMessage createNewMessage(long dpiId, RegDate dateDebut, RegDate dateFin, RegDate dateQuittancement) throws Exception {
				return createMessageAnnulationQuittancement(dpiId, dateDebut, dateFin, dateQuittancement);
			}
		});
	}

	private EsbMessage createMessageQuittancement(long noCtb, RegDate debutPeriode, RegDate finPeriode, RegDate dateEvenement) throws Exception {
		return createEsbMessage(createEvenementQuittancement(Evenement.QUITTANCE, noCtb, Liste.LR, debutPeriode, finPeriode, dateEvenement));
	}
	private EsbMessage createMessageQuittancementDansFutur(long noCtb, RegDate debutPeriode, RegDate finPeriode) throws Exception {
		return createEsbMessage(createEvenementQuittancementDansFutur(Evenement.QUITTANCE, noCtb, Liste.LR, debutPeriode, finPeriode));
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

	private static EvtListe createEvenementQuittancementDansFutur(Evenement quitancement, Long numeroCtb, Liste listeType, RegDate dateDebut,
	                                                              RegDate dateFin) {

		assertNotNull("le type de quittancement est obligation", quitancement);
		assertNotNull("Le numero du débiteur est obligatoire", numeroCtb);
		assertNotNull("la date du début du récapitulatif est obligatoire", dateDebut);

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

		final EvtListe evenement = new EvtListe();
		evenement.setUtilisateur(new Utilisateur("testuser", 22));
		evenement.setCaracteristiquesListe(identification);
		evenement.setTypeEvenement(quitancement);
		evenement.setCaracteristiquesDebiteur(debiteur);
		evenement.setCodeApplication("test");
		evenement.setVersionApplication("0.0");
		evenement.setTimestamp(XmlUtils.date2xmlcal(new Date()));
		evenement.setBusinessId(businessId);
		final Date currentDate = DateHelper.getCurrentDate();
		final long longCurrentDate = currentDate.getTime();
		//2 mn dans le futur
		final long longFuturDate= longCurrentDate + 120000;
		evenement.setDateEvenement(XmlUtils.date2xmlcal(new Date(longFuturDate)));

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
