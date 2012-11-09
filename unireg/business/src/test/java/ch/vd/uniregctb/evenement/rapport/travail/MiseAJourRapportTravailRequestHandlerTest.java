package ch.vd.uniregctb.evenement.rapport.travail;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.xml.event.rt.common.v1.IdentifiantRapportTravail;
import ch.vd.unireg.xml.event.rt.request.v1.MiseAJourRapportTravailRequest;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.ServiceException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MiseAJourRapportTravailRequestHandlerTest extends BusinessTest {
	private MiseAJourRapportTravailRequestHandler handler;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		handler = new MiseAJourRapportTravailRequestHandler();
		handler.setTiersService(tiersService);
	}


	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleSurDebiteurInconnu() throws Exception {

		final MiseAJourRapportTravailRequest request = new MiseAJourRapportTravailRequest();
		IdentifiantRapportTravail identifiantRapportTravail = new IdentifiantRapportTravail();
		identifiantRapportTravail.setNumeroDebiteur(12325478);
		request.setIdentifiantRapportTravail(identifiantRapportTravail);
		try {
			handler.handle(request);
			fail();
		}
		catch (ServiceException e) {
			assertTrue(e.getInfo() instanceof BusinessExceptionInfo);
			final BusinessExceptionInfo info = (BusinessExceptionInfo) e.getInfo();
			assertEquals(BusinessExceptionCode.UNKNOWN_PARTY.name(), info.getCode());
			assertEquals("le débiteur 123.254.78 n'existe pas", e.getMessage());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDebiteurNonActif() throws Exception {

		final Long idDebiteur = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable debiteur = addDebiteur();
				addForDebiteur(debiteur, date(2012, 1, 1), date(2012, 6, 20), MockCommune.Echallens);
				return debiteur.getNumero();
			}
		});

		final MiseAJourRapportTravailRequest request = new MiseAJourRapportTravailRequest();
		IdentifiantRapportTravail identifiantRapportTravail = new IdentifiantRapportTravail();
		identifiantRapportTravail.setNumeroDebiteur(idDebiteur.intValue());

		final RegDate dateDebutPeriode = date(2012, 1, 1);

		final RegDate dateFinPeriode = date(2012, 12, 31);

		identifiantRapportTravail.setDateDebutPeriodeDeclaration(DataHelper.coreToXML(dateDebutPeriode));

		identifiantRapportTravail.setDateFinPeriodeDeclaration(DataHelper.coreToXML(dateFinPeriode));
		request.setIdentifiantRapportTravail(identifiantRapportTravail);

		try {
			handler.handle(request);
			fail();
		}
		catch (ServiceException e) {
			assertTrue(e.getInfo() instanceof BusinessExceptionInfo);
			final BusinessExceptionInfo info = (BusinessExceptionInfo) e.getInfo();
			final String messageAttendu = String.format("le débiteur (%s) ne possède pas de fors couvrant la totalité de la période de déclaration qui va du %s au %s.",
					FormatNumeroHelper.numeroCTBToDisplay(idDebiteur), RegDateHelper.dateToDisplayString(dateDebutPeriode), RegDateHelper.dateToDisplayString(dateFinPeriode));
			assertEquals(BusinessExceptionCode.VALIDATION.name(), info.getCode());
			assertEquals(messageAttendu, e.getMessage());
		}
	}
}