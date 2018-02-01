package ch.vd.unireg.evenement.rt;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.xml.common.v1.Date;
import ch.vd.unireg.xml.event.rt.common.v1.IdentifiantRapportTravail;
import ch.vd.unireg.xml.event.rt.request.v1.CreationProlongationRapportTravail;
import ch.vd.unireg.xml.event.rt.request.v1.FermetureRapportTravail;
import ch.vd.unireg.xml.event.rt.request.v1.MiseAJourRapportTravailRequest;
import ch.vd.unireg.xml.event.rt.response.v1.MiseAJourRapportTravailResponse;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.xml.DataHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Classe de test du listener de requêtes de modification de rapport de travail. Cette classe nécessite une connexion à l'ESB de développement pour fonctionner.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>, Baba NGOM <baba-issa.ngom@vd.ch>
 */
public class MiseAJourRapportTravailRequestListenerItTest extends RapportTravailRequestListenerItTest {

	@Override
	String getRequestXSD() {
		return "event/rt/rapport-travail-request-1.xsd";
	}

	@Override
	String getResponseXSD() {
		return "event/rt/rapport-travail-response-1.xsd";
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testMiseAJourRTRequestOK() throws Exception {

		class Ids {
			Long idDebiteur;
			Long idSourcier;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable debiteur = addDebiteur();
				addForDebiteur(debiteur, date(2012, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Echallens);
				ids.idDebiteur= debiteur.getNumero();
				PersonnePhysique sourcier = addHabitant(12365478L);
				ids.idSourcier= sourcier.getNumero();

				addRapportPrestationImposable(debiteur,sourcier,date(2012,5,1),null,false);

				return null;
			}
		});
		final MiseAJourRapportTravailRequest request = new MiseAJourRapportTravailRequest();
		final IdentifiantRapportTravail identifiant = new IdentifiantRapportTravail();
		identifiant.setNumeroDebiteur(ids.idDebiteur.intValue());
		identifiant.setNumeroContribuable(ids.idSourcier.intValue());
		final Date dateDebutPeriodeDeclaration = DataHelper.coreToXMLv1(date(2012, 1, 1));
		identifiant.setDateDebutPeriodeDeclaration(dateDebutPeriodeDeclaration);
		final Date dateFinPeriodeDeclaration = DataHelper.coreToXMLv1(date(2012, 12, 31));
		identifiant.setDateFinPeriodeDeclaration(dateFinPeriodeDeclaration);

		request.setIdentifiantRapportTravail(identifiant);
		request.setDateDebutVersementSalaire(dateDebutPeriodeDeclaration);
		request.setCreationProlongationRapportTravail(new CreationProlongationRapportTravail());

		// Envoie le message
		sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());

		final EsbMessage message = getEsbMessage(getOutputQueue());
		assertNotNull(message);

		final MiseAJourRapportTravailResponse response = parseResponse(message);
		assertNotNull(response);
		assertEquals(DataHelper.coreToXMLv1(RegDate.get()), response.getDatePriseEnCompte());

	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testMiseAJourRTRequestValidationException() throws Exception {

		class Ids {
			Long idDebiteur;
			Long idSourcier;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSessionWithoutValidation(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable debiteur = addDebiteur();
				addForDebiteur(debiteur, date(2012, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Echallens);
				PeriodeFiscale periode2011 = new PeriodeFiscale();
				periode2011.setAnnee(2011);
				addLR(debiteur,date(2011,10,1), PeriodiciteDecompte.TRIMESTRIEL,periode2011);
				ids.idDebiteur= debiteur.getNumero();
				PersonnePhysique sourcier = addHabitant(12365478L);
				ids.idSourcier= sourcier.getNumero();

				addRapportPrestationImposable(debiteur,sourcier,date(2012,5,1),null,false);

				return null;
			}
		});
		final MiseAJourRapportTravailRequest request = new MiseAJourRapportTravailRequest();
		final IdentifiantRapportTravail identifiant = new IdentifiantRapportTravail();
		identifiant.setNumeroDebiteur(ids.idDebiteur.intValue());
		identifiant.setNumeroContribuable(ids.idSourcier.intValue());
		final Date dateDebutPeriodeDeclaration = DataHelper.coreToXMLv1(date(2012, 1, 1));
		identifiant.setDateDebutPeriodeDeclaration(dateDebutPeriodeDeclaration);
		final Date dateFinPeriodeDeclaration = DataHelper.coreToXMLv1(date(2012, 12, 31));
		identifiant.setDateFinPeriodeDeclaration(dateFinPeriodeDeclaration);

		request.setIdentifiantRapportTravail(identifiant);
		request.setDateDebutVersementSalaire(dateDebutPeriodeDeclaration);
		request.setFermetureRapportTravail(new FermetureRapportTravail());


		// Envoie le message
		sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());

		final EsbMessage message = getEsbMessage(getOutputQueue());
		assertNotNull(message);

		final MiseAJourRapportTravailResponse response = parseResponse(message);
		assertNotNull(response);
		assertNotNull(response.getExceptionInfo());
		final String messageErreur = String.format("Exception de validation pour le message {businessId: %s}: Debiteur ou sourcier invalide dans Unireg.",message.getBusinessCorrelationId());
		assertEquals(messageErreur, response.getExceptionInfo().getMessage());

	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testMiseAJourRTRequestServiceException() throws Exception {

		class Ids {
			Long idDebiteur = 1500000L;
			Long idSourcier = 1000021L;
		}
		final Ids ids = new Ids();


		final MiseAJourRapportTravailRequest request = new MiseAJourRapportTravailRequest();
		final IdentifiantRapportTravail identifiant = new IdentifiantRapportTravail();
		identifiant.setNumeroDebiteur(ids.idDebiteur.intValue());
		identifiant.setNumeroContribuable(ids.idSourcier.intValue());
		final Date dateDebutPeriodeDeclaration = DataHelper.coreToXMLv1(date(2012, 1, 1));
		identifiant.setDateDebutPeriodeDeclaration(dateDebutPeriodeDeclaration);
		final Date dateFinPeriodeDeclaration = DataHelper.coreToXMLv1(date(2012, 12, 31));
		identifiant.setDateFinPeriodeDeclaration(dateFinPeriodeDeclaration);

		request.setIdentifiantRapportTravail(identifiant);
		request.setDateDebutVersementSalaire(dateDebutPeriodeDeclaration);
		request.setFermetureRapportTravail(new FermetureRapportTravail());


		// Envoie le message
		sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());

		final EsbMessage message = getEsbMessage(getOutputQueue());
		assertNotNull(message);

		final MiseAJourRapportTravailResponse response = parseResponse(message);
		assertNotNull(response);
		assertNotNull(response.getExceptionInfo());
		final String messageErreur = "Le débiteur 15.000.00 n'existe pas dans unireg";
		assertEquals(messageErreur, response.getExceptionInfo().getMessage());

	}


	//Aucun traitement attendu, on ne devrait pas retourner d'erreur de validation
	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testMiseAJourRTRequestValidationExceptionSansEffet() throws Exception {

		class Ids {
			Long idDebiteur;
			Long idSourcier;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSessionWithoutValidation(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable debiteur = addDebiteur();
				addForDebiteur(debiteur, date(2012, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Echallens);
				PeriodeFiscale periode2011 = new PeriodeFiscale();
				periode2011.setAnnee(2011);
				addLR(debiteur,date(2011,10,1),PeriodiciteDecompte.TRIMESTRIEL,periode2011);
				ids.idDebiteur= debiteur.getNumero();
				PersonnePhysique sourcier = addHabitant(12365478L);
				ids.idSourcier= sourcier.getNumero();

				addRapportPrestationImposable(debiteur,sourcier,date(2011,5,1),date(2011,12,31),false);

				return null;
			}
		});
		final MiseAJourRapportTravailRequest request = new MiseAJourRapportTravailRequest();
		final IdentifiantRapportTravail identifiant = new IdentifiantRapportTravail();
		identifiant.setNumeroDebiteur(ids.idDebiteur.intValue());
		identifiant.setNumeroContribuable(ids.idSourcier.intValue());
		final Date dateDebutPeriodeDeclaration = DataHelper.coreToXMLv1(date(2012, 1, 1));
		identifiant.setDateDebutPeriodeDeclaration(dateDebutPeriodeDeclaration);
		final Date dateFinPeriodeDeclaration = DataHelper.coreToXMLv1(date(2012, 12, 31));
		identifiant.setDateFinPeriodeDeclaration(dateFinPeriodeDeclaration);

		request.setIdentifiantRapportTravail(identifiant);
		request.setDateDebutVersementSalaire(dateDebutPeriodeDeclaration);
		request.setFermetureRapportTravail(new FermetureRapportTravail());


		// Envoie le message
		sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());

		final EsbMessage message = getEsbMessage(getOutputQueue());
		assertNotNull(message);

		final MiseAJourRapportTravailResponse response = parseResponse(message);
		assertNotNull(response);
		assertEquals(DataHelper.coreToXMLv1(RegDate.get()), response.getDatePriseEnCompte());

	}


}
