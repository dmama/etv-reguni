package ch.vd.uniregctb.evenement.rt;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.xml.common.v1.Date;
import ch.vd.unireg.xml.event.rt.common.v1.IdentifiantRapportTravail;
import ch.vd.unireg.xml.event.rt.request.v1.CreationProlongationRapportTravail;
import ch.vd.unireg.xml.event.rt.request.v1.MiseAJourRapportTravailRequest;
import ch.vd.unireg.xml.event.rt.response.v1.MiseAJourRapportTravailResponse;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.evenement.rapport.travail.MiseAJourRapportTravailRequestHandler;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.xml.DataHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Classe de test du listener de requêtes de modification de rapport de travail. Cette classe nécessite une connexion à l'ESB de développement pour fonctionner.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>, Baba NGOM <baba-issa.ngom@vd.ch>
 */
public class MiseAJourRapportTravailRequestListenerItTest extends RapportTravailRequestListenerItTest {

	private MiseAJourRapportTravailRequestHandler handler;

	@Override
	public void onSetUp() throws Exception {
		handler = getBean(MiseAJourRapportTravailRequestHandler.class, "rapportTravailRequestHandler");
		super.onSetUp();
	}

	@Override
	public void onTearDown() throws Exception {
		super.onTearDown();
	}

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
				addForDebiteur(debiteur, date(2012, 1, 1), null, MockCommune.Echallens);
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
		final Date dateDebutPeriodeDeclaration = DataHelper.coreToXML(date(2012, 1, 1));
		identifiant.setDateDebutPeriodeDeclaration(dateDebutPeriodeDeclaration);
		final Date dateFinPeriodeDeclaration = DataHelper.coreToXML(date(2012, 12, 31));
		identifiant.setDateFinPeriodeDeclaration(dateFinPeriodeDeclaration);

		request.setIdentifiantRapportTravail(identifiant);
		request.setDateDebutVersementSalaire(dateDebutPeriodeDeclaration);
		request.setCreationProlongationRapportTravail(new CreationProlongationRapportTravail());


		// Envoie le message
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());
				return null;
			}
		});

		final EsbMessage message = getEsbMessage(getOutputQueue());
		assertNotNull(message);

		final MiseAJourRapportTravailResponse response = (MiseAJourRapportTravailResponse) parseResponse(message);
		assertNotNull(response);
		assertEquals(DataHelper.coreToXML(RegDate.get()), response.getDatePriseEnCompte());

	}




}
