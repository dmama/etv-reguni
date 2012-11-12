package ch.vd.uniregctb.evenement.rapport.travail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.xml.event.rt.common.v1.IdentifiantRapportTravail;
import ch.vd.unireg.xml.event.rt.request.v1.FermetureRapportTravail;
import ch.vd.unireg.xml.event.rt.request.v1.MiseAJourRapportTravailRequest;
import ch.vd.unireg.xml.event.rt.response.v1.MiseAJourRapportTravailResponse;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RapportPrestationImposable;
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
			handler.handle(MiseAjourRapportTravail.get(request));
			fail();
		}
		catch (ServiceException e) {
			assertTrue(e.getInfo() instanceof BusinessExceptionInfo);
			final BusinessExceptionInfo info = (BusinessExceptionInfo) e.getInfo();
			assertEquals(BusinessExceptionCode.UNKNOWN_PARTY.name(), info.getCode());
			assertEquals("le débiteur 123.254.78 n'existe pas dans unireg", e.getMessage());
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

		final Long idSourcier = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				PersonnePhysique sourcier = addHabitant(12365478L);

				return sourcier.getNumero();
			}
		});

		final MiseAJourRapportTravailRequest request = new MiseAJourRapportTravailRequest();
		IdentifiantRapportTravail identifiantRapportTravail = new IdentifiantRapportTravail();
		identifiantRapportTravail.setNumeroDebiteur(idDebiteur.intValue());
		identifiantRapportTravail.setNumeroContribuable(idSourcier.intValue());

		final RegDate dateDebutPeriode = date(2012, 1, 1);

		final RegDate dateFinPeriode = date(2012, 12, 31);

		identifiantRapportTravail.setDateDebutPeriodeDeclaration(DataHelper.coreToXML(dateDebutPeriode));

		identifiantRapportTravail.setDateFinPeriodeDeclaration(DataHelper.coreToXML(dateFinPeriode));
		request.setIdentifiantRapportTravail(identifiantRapportTravail);

		try {
			handler.handle(MiseAjourRapportTravail.get(request));
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

	/*
	 Si le rapport de travail n’est pas présent,
	 un nouveau RT est ouvert avec une date de début égale à la date de début de versement de salaire (VS-DD).
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRapportTravailInexistant() throws Exception {

		final Long idDebiteur = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable debiteur = addDebiteur();
				addForDebiteur(debiteur, date(2012, 1, 1), null, MockCommune.Echallens);
				return debiteur.getNumero();
			}
		});


		final Long idSourcier = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				PersonnePhysique sourcier = addHabitant(12365478L);

				return sourcier.getNumero();
			}
		});

		final RegDate dateDebutPeriode = date(2012, 1, 1);
		final RegDate dateFinPeriode = date(2012, 12, 31);
		final RegDate dateDebutVersementSalaire = date(2012, 4, 1);
		final DateRange periodeDeclaration = new DateRangeHelper.Range(dateDebutPeriode,dateFinPeriode);

		final MiseAJourRapportTravailRequest request = createMiseAJourRapportTravailRequest(idDebiteur, idSourcier,periodeDeclaration, dateDebutVersementSalaire,null);
		MiseAJourRapportTravailResponse response =  handler.handle(MiseAjourRapportTravail.get(request));
		assertEquals(DataHelper.coreToXML(RegDate.get()),response.getDatePriseEnCompte());
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(idDebiteur);
		final PersonnePhysique sourcier = (PersonnePhysique) tiersService.getTiers(idSourcier);
		List<RapportPrestationImposable> rapports = tiersService.getRapportPrestationImposableForPeriode(dpi, sourcier,periodeDeclaration);
		assertEquals(1,rapports.size());
		RapportPrestationImposable rapport = rapports.get(0);
		assertEquals(dateDebutVersementSalaire,rapport.getDateDebut());

	}


	/*
		 Si le rapport de travail n’est pas présent,et en cas d'évènement de fermeture, le message est ignorée
		 un nouveau RT est ouvert avec une date de début égale à la date de début de versement de salaire (VS-DD).
		 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRapportTravailInexistantAvecFermeture() throws Exception {

		final Long idDebiteur = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable debiteur = addDebiteur();
				addForDebiteur(debiteur, date(2012, 1, 1), null, MockCommune.Echallens);
				return debiteur.getNumero();
			}
		});


		final Long idSourcier = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				PersonnePhysique sourcier = addHabitant(12365478L);

				return sourcier.getNumero();
			}
		});



		final RegDate dateDebutPeriode = date(2012, 1, 1);
		final RegDate dateFinPeriode = date(2012, 12, 31);
		final RegDate dateDebutVersementSalaire = date(2012, 4, 1);
		final DateRange periodeDeclaration = new DateRangeHelper.Range(dateDebutPeriode,dateFinPeriode);


		final MiseAJourRapportTravailRequest request = createMiseAJourRapportTravailRequest(idDebiteur, idSourcier,periodeDeclaration, dateDebutVersementSalaire,null);
		request.setFermetureRapportTravail(FermetureRapportTravail.Z);
		MiseAJourRapportTravailResponse response =  handler.handle(MiseAjourRapportTravail.get(request));
		assertEquals(DataHelper.coreToXML(RegDate.get()),response.getDatePriseEnCompte());
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(idDebiteur);
		final PersonnePhysique sourcier = (PersonnePhysique) tiersService.getTiers(idSourcier);
		List<RapportPrestationImposable> rapports = tiersService.getRapportPrestationImposableForPeriode(dpi, sourcier,periodeDeclaration);
		assertEquals(0,rapports.size());

	}


	/*CODE FERMETURE SUR MESSAGE
	Dans le cas d'un rapport de travail avec une date de début postèrieur ou égale à la date de début de la période de déclaration
	On doit annuler le rapport de travail
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRapportTravailExistantAvecFermeture() throws Exception {


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

		final RegDate dateDebutPeriode = date(2012, 1, 1);
		final RegDate dateFinPeriode = date(2012, 12, 31);
		final RegDate dateDebutVersementSalaire = date(2012, 4, 1);
		final DateRange periodeDeclaration = new DateRangeHelper.Range(dateDebutPeriode,dateFinPeriode);


		final MiseAJourRapportTravailRequest request = createMiseAJourRapportTravailRequest(ids.idDebiteur, ids.idSourcier,periodeDeclaration, dateDebutVersementSalaire,null);
		request.setFermetureRapportTravail(FermetureRapportTravail.Z);
		MiseAJourRapportTravailResponse response =  handler.handle(MiseAjourRapportTravail.get(request));
		assertEquals(DataHelper.coreToXML(RegDate.get()),response.getDatePriseEnCompte());
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(ids.idDebiteur);
		List<RapportEntreTiers> rapportPrestations = new ArrayList<RapportEntreTiers>();
		rapportPrestations.addAll(dpi.getRapportsObjet());
		RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapportPrestations.get(0);
		assertTrue(rapportPrestationImposable.isAnnule());

	}

	//CODE FERMETURE SUR MESSAGE CAS 16
	//Dans le cas ou une date de fin de rapport de travail est antérieur ou égal à la date de fin de la période de déclaration
	//la date de fin du rapport de travail recoit la valeur date de début de période -1 jour
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRapportTravailExistantDateFinAnterieurDateFinPeriode() throws Exception {


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

				addRapportPrestationImposable(debiteur,sourcier,date(2011,1,1),date(2012,6,1),false);

				return null;
			}
		});

		final RegDate dateDebutPeriode = date(2012, 1, 1);
		final RegDate dateFinPeriode = date(2012, 12, 31);
		final RegDate dateDebutVersementSalaire = date(2012, 1, 1);
		final RegDate dateFinRapportAttendu = dateDebutPeriode.getOneDayBefore();
		final DateRange periodeDeclaration = new DateRangeHelper.Range(dateDebutPeriode,dateFinPeriode);


		final MiseAJourRapportTravailRequest request = createMiseAJourRapportTravailRequest(ids.idDebiteur, ids.idSourcier,periodeDeclaration, dateDebutVersementSalaire,null);
		request.setFermetureRapportTravail(FermetureRapportTravail.Z);
		MiseAJourRapportTravailResponse response =  handler.handle(MiseAjourRapportTravail.get(request));
		assertEquals(DataHelper.coreToXML(RegDate.get()),response.getDatePriseEnCompte());
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(ids.idDebiteur);
		List<RapportEntreTiers> rapportPrestations = new ArrayList<RapportEntreTiers>();
		rapportPrestations.addAll(dpi.getRapportsObjet());
		RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapportPrestations.get(0);
		assertEquals(dateFinRapportAttendu,rapportPrestationImposable.getDateFin());

	}


	//CODE FERMETURE SUR MESSAGE CAS 15
	//Dans le cas ou une date de fin de rapport de travail est inexistante
	//le rapport de travail recoit comme date de fin la valeur date de début de période -1 jour
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRapportTravailExistantDateFinInexistante() throws Exception {


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

				addRapportPrestationImposable(debiteur,sourcier,date(2011,1,1),null,false);

				return null;
			}
		});

		final RegDate dateDebutPeriode = date(2012, 1, 1);
		final RegDate dateFinPeriode = date(2012, 12, 31);
		final RegDate dateDebutVersementSalaire = date(2012, 1, 1);
		final RegDate dateFinRapportAttendu = dateDebutPeriode.getOneDayBefore();
		final DateRange periodeDeclaration = new DateRangeHelper.Range(dateDebutPeriode,dateFinPeriode);


		final MiseAJourRapportTravailRequest request = createMiseAJourRapportTravailRequest(ids.idDebiteur, ids.idSourcier,periodeDeclaration, dateDebutVersementSalaire,null);
		request.setFermetureRapportTravail(FermetureRapportTravail.Z);
		MiseAJourRapportTravailResponse response =  handler.handle(MiseAjourRapportTravail.get(request));
		assertEquals(DataHelper.coreToXML(RegDate.get()),response.getDatePriseEnCompte());
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(ids.idDebiteur);
		List<RapportEntreTiers> rapportPrestations = new ArrayList<RapportEntreTiers>();
		rapportPrestations.addAll(dpi.getRapportsObjet());
		RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapportPrestations.get(0);
		assertEquals(dateFinRapportAttendu,rapportPrestationImposable.getDateFin());

	}


	private MiseAJourRapportTravailRequest createMiseAJourRapportTravailRequest(Long idDebiteur, Long idSourcier, DateRange periodeDeclaration,
	                                                                            RegDate dateDebutVersementSalaire, RegDate dateFinVersementSalaire) {
		final MiseAJourRapportTravailRequest request = new MiseAJourRapportTravailRequest();
		IdentifiantRapportTravail identifiantRapportTravail = new IdentifiantRapportTravail();
		identifiantRapportTravail.setNumeroDebiteur(idDebiteur.intValue());


		identifiantRapportTravail.setDateDebutPeriodeDeclaration(DataHelper.coreToXML(periodeDeclaration.getDateDebut()));

		identifiantRapportTravail.setDateFinPeriodeDeclaration(DataHelper.coreToXML(periodeDeclaration.getDateFin()));

		identifiantRapportTravail.setNumeroContribuable(idSourcier.intValue());
		request.setIdentifiantRapportTravail(identifiantRapportTravail);
		request.setDateDebutVersementSalaire(DataHelper.coreToXML(dateDebutVersementSalaire));
		return request;
	}


}