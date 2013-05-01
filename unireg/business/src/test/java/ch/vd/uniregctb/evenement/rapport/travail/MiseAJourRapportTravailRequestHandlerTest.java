package ch.vd.uniregctb.evenement.rapport.travail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.xml.common.v1.Date;
import ch.vd.unireg.xml.event.rt.common.v1.IdentifiantRapportTravail;
import ch.vd.unireg.xml.event.rt.request.v1.CreationProlongationRapportTravail;
import ch.vd.unireg.xml.event.rt.request.v1.FermetureRapportTravail;
import ch.vd.unireg.xml.event.rt.request.v1.FinRapportTravail;
import ch.vd.unireg.xml.event.rt.request.v1.FinRapportTravailType;
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
		handler.setHibernateTemplate(hibernateTemplate);
	}


	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleSurDebiteurInconnu() throws Exception {

		final MiseAJourRapportTravailRequest request = new MiseAJourRapportTravailRequest();
		IdentifiantRapportTravail identifiantRapportTravail = new IdentifiantRapportTravail();
		identifiantRapportTravail.setNumeroDebiteur(12325478);
		request.setIdentifiantRapportTravail(identifiantRapportTravail);
		try {
			handler.handle(MiseAjourRapportTravail.get(request, null));
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
			handler.handle(MiseAjourRapportTravail.get(request, null));
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
	public void testRTInexistant() throws Exception {

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
		final RegDate dateDebutVersementSalaire = date(2012, 1, 1);
		final DateRange periodeDeclaration = new DateRangeHelper.Range(dateDebutPeriode,dateFinPeriode);

		final MiseAJourRapportTravailRequest request = createMiseAJourRapportTravailRequest(idDebiteur, idSourcier, periodeDeclaration, dateDebutVersementSalaire, null);
		MiseAJourRapportTravailResponse response = doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(request, null));
			}
		});
		assertEquals(DataHelper.coreToXML(RegDate.get()),response.getDatePriseEnCompte());
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(idDebiteur);
		final PersonnePhysique sourcier = (PersonnePhysique) tiersService.getTiers(idSourcier);
		List<RapportPrestationImposable> rapports = tiersService.getAllRapportPrestationImposable(dpi, sourcier, true, true);
		assertEquals(1,rapports.size());
		RapportPrestationImposable rapport = rapports.get(0);
		assertEquals(dateDebutVersementSalaire,rapport.getDateDebut());

	}


	/*
	 Si le rapport de travail n’est pas présent,
	 un nouveau RT est ouvert avec une date de début égale à la date de début de versement de salaire (VS-DD).
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRTInexistant_SIFISC_7994() throws Exception {
		class Ids {
			Long idDebiteur;
			Long idSourcier;
		}
		final Ids ids = new Ids();
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable debiteur = addDebiteur();
				addForDebiteur(debiteur, date(2010, 1, 1), null, MockCommune.Echallens);
				ids.idDebiteur= debiteur.getNumero();
				PersonnePhysique sourcier = addHabitant(12365478L);
				ids.idSourcier= sourcier.getNumero();

				addRapportPrestationImposable(debiteur,sourcier,date(2010,1,1),null,true);

				return null;
			}
		});

		final RegDate dateDebutPeriode = date(2013, 1, 1);
		final RegDate dateFinPeriode = date(2013, 12, 31);
		final RegDate dateDebutVersementSalaire = date(2013, 2, 15);
		final DateRange periodeDeclaration = new DateRangeHelper.Range(dateDebutPeriode,dateFinPeriode);

		final MiseAJourRapportTravailRequest request = createMiseAJourRapportTravailRequest(ids.idDebiteur, ids.idSourcier, periodeDeclaration, dateDebutVersementSalaire, null);
		MiseAJourRapportTravailResponse response = doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(request, null));
			}
		});
		assertEquals(DataHelper.coreToXML(RegDate.get()),response.getDatePriseEnCompte());
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(ids.idDebiteur);
		final PersonnePhysique sourcier = (PersonnePhysique) tiersService.getTiers(ids.idSourcier);
		List<RapportPrestationImposable> rapports = tiersService.getAllRapportPrestationImposable(dpi, sourcier, true, true);
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
	public void testRTInexistantAvecFermeture() throws Exception {

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
		request.setFermetureRapportTravail(new FermetureRapportTravail());
		MiseAJourRapportTravailResponse response =  doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(request, null));
			}
		});
		assertEquals(DataHelper.coreToXML(RegDate.get()),response.getDatePriseEnCompte());
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(idDebiteur);
		final PersonnePhysique sourcier = (PersonnePhysique) tiersService.getTiers(idSourcier);
		List<RapportPrestationImposable> rapports = tiersService.getAllRapportPrestationImposable(dpi, sourcier, true, true);
		assertEquals(0,rapports.size());

	}


	/*CODE FERMETURE SUR MESSAGE
	Dans le cas d'un rapport de travail avec une date de début postèrieur ou égale à la date de début de la période de déclaration
	On doit annuler le rapport de travail
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRTExistantAvecFermeture() throws Exception {


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
		request.setFermetureRapportTravail(new FermetureRapportTravail());
		MiseAJourRapportTravailResponse response =  doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(request, null));
			}
		});
		assertEquals(DataHelper.coreToXML(RegDate.get()),response.getDatePriseEnCompte());
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(ids.idDebiteur);
		List<RapportEntreTiers> rapportPrestations = new ArrayList<>();
		rapportPrestations.addAll(dpi.getRapportsObjet());
		RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapportPrestations.get(0);
		assertTrue(rapportPrestationImposable.isAnnule());

	}

	//CODE FERMETURE SUR MESSAGE CAS 16
	//Dans le cas ou une date de fin de rapport de travail est antérieur ou égal à la date de fin de la période de déclaration
	//la date de fin du rapport de travail recoit la valeur date de début de période -1 jour
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRTExistantDateFinAnterieurDateFinPeriode() throws Exception {


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
		request.setFermetureRapportTravail(new FermetureRapportTravail());
		MiseAJourRapportTravailResponse response = doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(request, null));
			}
		});
		assertEquals(DataHelper.coreToXML(RegDate.get()),response.getDatePriseEnCompte());
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(ids.idDebiteur);
		List<RapportEntreTiers> rapportPrestations = new ArrayList<>();
		rapportPrestations.addAll(dpi.getRapportsObjet());
		RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapportPrestations.get(0);
		assertEquals(dateFinRapportAttendu, rapportPrestationImposable.getDateFin());

	}


	//CODE FERMETURE SUR MESSAGE CAS 15
	//Dans le cas ou une date de fin de rapport de travail est inexistante
	//le rapport de travail recoit comme date de fin la valeur date de début de période -1 jour
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRTExistantDateFinInexistante() throws Exception {


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
		request.setFermetureRapportTravail(new FermetureRapportTravail());
		MiseAJourRapportTravailResponse response = doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(request, null));
			}
		});
		assertEquals(DataHelper.coreToXML(RegDate.get()),response.getDatePriseEnCompte());
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(ids.idDebiteur);
		List<RapportEntreTiers> rapportPrestations = new ArrayList<>();
		rapportPrestations.addAll(dpi.getRapportsObjet());
		RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapportPrestations.get(0);
		assertEquals(dateFinRapportAttendu,rapportPrestationImposable.getDateFin());

	}


	//CODE FIN DE RAPPORT SORTIE OU DECES
	//Dans le cas ou une date de fin de rapport de travail est inexistante et que l'on reçoit une demande de fin de rapport
	//le rapport de travail recoit comme date de fin la date de l'évenement ou la date de fin de versement de salaire.
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRTExistantAvecEvenementFinRapport() throws Exception {


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
		final RegDate dateFinCore = date(2012, 6, 30);
		final Date dateFinVersementSalaire = DataHelper.coreToXML(dateFinCore);
		final Date dateEvenement = DataHelper.coreToXML(dateFinCore);
		final DateRange periodeDeclaration = new DateRangeHelper.Range(dateDebutPeriode,dateFinPeriode);


		final MiseAJourRapportTravailRequest request = createMiseAJourRapportTravailRequest(ids.idDebiteur, ids.idSourcier,periodeDeclaration, dateDebutVersementSalaire,null);
		FinRapportTravail finRapportTravail = new FinRapportTravail();
		finRapportTravail.setCode(FinRapportTravailType.SORTIE);
		finRapportTravail.setDateEvenement(dateEvenement);
		request.setFinRapportTravail(finRapportTravail);
		request.setDateFinVersementSalaire(dateFinVersementSalaire);

		MiseAJourRapportTravailResponse response =  doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(request, null));
			}
		});
		assertEquals(DataHelper.coreToXML(RegDate.get()),response.getDatePriseEnCompte());
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(ids.idDebiteur);
		List<RapportEntreTiers> rapportPrestations = new ArrayList<>();
		rapportPrestations.addAll(dpi.getRapportsObjet());
		RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapportPrestations.get(0);
		assertEquals(dateFinCore,rapportPrestationImposable.getDateFin());

	}


	//le rapport de travail existant à une date de fin dans la période de déclaration.
	//En cas d'absence d'evenement de fin ou de fermeture, on réouvre le rapport de travail
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRTExistantFermeDansPeriodeDeclaration() throws Exception {


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

				addRapportPrestationImposable(debiteur,sourcier,date(2011,1,1),date(2012,8,1),false);

				return null;
			}
		});

		final RegDate dateDebutPeriode = date(2012, 1, 1);
		final RegDate dateFinPeriode = date(2012, 12, 31);
		final RegDate dateDebutVersementSalaire = date(2012, 1, 1);
		final RegDate dateFinCore = date(2012, 6, 30);
		final Date dateFinVersementSalaire = DataHelper.coreToXML(dateFinCore);
		final Date dateEvenement = DataHelper.coreToXML(dateFinCore);
		final DateRange periodeDeclaration = new DateRangeHelper.Range(dateDebutPeriode,dateFinPeriode);


		final MiseAJourRapportTravailRequest request = createMiseAJourRapportTravailRequest(ids.idDebiteur, ids.idSourcier,periodeDeclaration, dateDebutVersementSalaire,dateFinCore);

		MiseAJourRapportTravailResponse response =  doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(request, null));
			}
		});
		assertEquals(DataHelper.coreToXML(RegDate.get()),response.getDatePriseEnCompte());
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(ids.idDebiteur);
		List<RapportEntreTiers> rapportPrestations = new ArrayList<>();
		rapportPrestations.addAll(dpi.getRapportsObjet());
		Collections.sort(rapportPrestations, new DateRangeComparator<RapportEntreTiers>());
		RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapportPrestations.get(1);
		assertEquals(null,rapportPrestationImposable.getDateFin());

	}

	//le rapport de travail existant à une date de début postérieur à la date de début de la période de déclaration.
	//la date de debut du rapport reçoit la date d edébut de versement de salaire
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRTExistantOuvertApresPeriodeDeclaration() throws Exception {


		class Ids {
			Long idDebiteur;
			Long idSourcier;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable debiteur = addDebiteur();
				addForDebiteur(debiteur, date(2011, 1, 1), null, MockCommune.Echallens);
				ids.idDebiteur= debiteur.getNumero();
				PersonnePhysique sourcier = addHabitant(12365478L);
				ids.idSourcier= sourcier.getNumero();

				addRapportPrestationImposable(debiteur,sourcier,date(2012,1,1),null,false);

				return null;
			}
		});

		final RegDate dateDebutPeriode = date(2011, 1, 1);
		final RegDate dateFinPeriode = date(2011, 12, 31);
		final RegDate dateDebutVersementSalaire = date(2011, 1, 1);
		final RegDate dateFinCore = date(2012, 6, 30);
		final Date dateFinVersementSalaire = DataHelper.coreToXML(dateFinCore);
		final DateRange periodeDeclaration = new DateRangeHelper.Range(dateDebutPeriode,dateFinPeriode);


		final MiseAJourRapportTravailRequest request = createMiseAJourRapportTravailRequest(ids.idDebiteur, ids.idSourcier,periodeDeclaration, dateDebutVersementSalaire,null);
		request.setDateFinVersementSalaire(dateFinVersementSalaire);

		MiseAJourRapportTravailResponse response =  doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(request, null));
			}
		});
		assertEquals(DataHelper.coreToXML(RegDate.get()),response.getDatePriseEnCompte());
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(ids.idDebiteur);
		List<RapportEntreTiers> rapportPrestations = new ArrayList<>();
		rapportPrestations.addAll(dpi.getRapportsObjet());
		Collections.sort(rapportPrestations, new DateRangeComparator<RapportEntreTiers>());
		RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapportPrestations.get(0);
		assertEquals(dateDebutVersementSalaire,rapportPrestationImposable.getDateDebut());

	}

	//le rapport de travail existant à une date de fin antérieur à la date de début de la période de déclaration.
	//le rapport est réouvert pour un écart inférieur ou égal a 1 jour
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRTFermeAvantPeriodeEcartUnJour() throws Exception {


		class Ids {
			Long idDebiteur;
			Long idSourcier;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable debiteur = addDebiteur();
				addForDebiteur(debiteur, date(2011, 1, 1), null, MockCommune.Echallens);
				ids.idDebiteur= debiteur.getNumero();
				PersonnePhysique sourcier = addHabitant(12365478L);
				ids.idSourcier= sourcier.getNumero();

				addRapportPrestationImposable(debiteur,sourcier,date(2011,1,1),date(2012, 3, 30),false);

				return null;
			}
		});

		final RegDate dateDebutPeriode = date(2012, 4, 1);
		final RegDate dateFinPeriode = date(2012, 12, 31);
		final RegDate dateDebutVersementSalaire = date(2012, 4, 1);
		final DateRange periodeDeclaration = new DateRangeHelper.Range(dateDebutPeriode,dateFinPeriode);


		final MiseAJourRapportTravailRequest request = createMiseAJourRapportTravailRequest(ids.idDebiteur, ids.idSourcier,periodeDeclaration, dateDebutVersementSalaire,null);

		MiseAJourRapportTravailResponse response =  doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(request, null));
			}
		});
		assertEquals(DataHelper.coreToXML(RegDate.get()),response.getDatePriseEnCompte());
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(ids.idDebiteur);
		List<RapportEntreTiers> rapportPrestations = new ArrayList<>();
		rapportPrestations.addAll(dpi.getRapportsObjet());
		Collections.sort(rapportPrestations, new DateRangeComparator<RapportEntreTiers>());
		RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapportPrestations.get(1);
		assertEquals(null,rapportPrestationImposable.getDateFin());

	}
	//le rapport de travail existant à une date de fin antérieur à la date de début de la période de déclaration.
	//le rapport est réouvert en annulant l'existant est en créant un nouveau rapport qui porte les modifications.
	//Doit permettre de conserver un historique.
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRTFermeAvantPeriodeEcartUnJour_SIFISC_7998() throws Exception {


		class Ids {
			Long idDebiteur;
			Long idSourcier;
		}
		final Ids ids = new Ids();
		final RegDate dateFinRapport = date(2013, 1, 14);

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable debiteur = addDebiteur();
				addForDebiteur(debiteur, date(2011, 1, 1), null, MockCommune.Echallens);
				ids.idDebiteur= debiteur.getNumero();
				PersonnePhysique sourcier = addHabitant(12365478L);
				ids.idSourcier= sourcier.getNumero();


				addRapportPrestationImposable(debiteur,sourcier,date(2010,3,23), dateFinRapport,false);
				addRapportPrestationImposable(debiteur,sourcier,date(2012,7,9),null,false);

				return null;
			}
		});

		final RegDate dateDebutPeriode = date(2013, 1, 1);
		final RegDate dateFinPeriode = date(2013, 12, 31);
		final RegDate dateDebutVersementSalaire = date(2013, 1, 15);
		final DateRange periodeDeclaration = new DateRangeHelper.Range(dateDebutPeriode,dateFinPeriode);


		final MiseAJourRapportTravailRequest request = createMiseAJourRapportTravailRequest(ids.idDebiteur, ids.idSourcier,periodeDeclaration, dateDebutVersementSalaire,null);

		MiseAJourRapportTravailResponse response =  doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(request, null));
			}
		});
		assertEquals(DataHelper.coreToXML(RegDate.get()),response.getDatePriseEnCompte());
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(ids.idDebiteur);
		List<RapportEntreTiers> rapportPrestations = new ArrayList<>();
		rapportPrestations.addAll(dpi.getRapportsObjet());
		assertEquals(3,rapportPrestations.size());
		Collections.sort(rapportPrestations, new DateRangeComparator<RapportEntreTiers>());

		RapportPrestationImposable rapportAnnule = (RapportPrestationImposable) rapportPrestations.get(0);
		assertTrue(rapportAnnule.isAnnule());
		assertEquals(dateFinRapport, rapportAnnule.getDateFin());

		RapportPrestationImposable rapportOuvert = (RapportPrestationImposable) rapportPrestations.get(1);
		assertEquals(null, rapportOuvert.getDateFin());

		RapportPrestationImposable dernierRappport = (RapportPrestationImposable) rapportPrestations.get(2);
		assertTrue(dernierRappport.isAnnule());
		assertEquals(null, dernierRappport.getDateFin());


	}
	//SIFISC-7541
	//Teste que l'on ne créé pas un RT en doublon suite à la reception du même message.
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRTDemandeOuvertureEnDoublon() throws Exception {


		class Ids {
			Long idDebiteur;
			Long idSourcier;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable debiteur = addDebiteur();
				addForDebiteur(debiteur, date(2011, 1, 1), null, MockCommune.Echallens);
				ids.idDebiteur= debiteur.getNumero();
				PersonnePhysique sourcier = addHabitant(12365478L);
				ids.idSourcier= sourcier.getNumero();

				addRapportPrestationImposable(debiteur,sourcier,date(2011,10,1),date(2012,12,31),false);
				addRapportPrestationImposable(debiteur,sourcier,date(2013,4,1),null,false);

				return null;
			}
		});

		final RegDate dateDebutPeriode = date(2013, 4, 1);
		final RegDate dateFinPeriode = date(2013, 6, 30);
		final RegDate dateDebutVersementSalaire = date(2013, 4, 1);
		final RegDate dateFinVersementSalaire = date(2013, 6, 30);
		final DateRange periodeDeclaration = new DateRangeHelper.Range(dateDebutPeriode,dateFinPeriode);


		final MiseAJourRapportTravailRequest request = createMiseAJourRapportTravailRequest(ids.idDebiteur, ids.idSourcier,periodeDeclaration, dateDebutVersementSalaire,dateFinVersementSalaire);
		request.setCreationProlongationRapportTravail(new CreationProlongationRapportTravail());
		MiseAJourRapportTravailResponse response =  doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(request, null));
			}
		});
		assertEquals(DataHelper.coreToXML(RegDate.get()),response.getDatePriseEnCompte());
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(ids.idDebiteur);
		final PersonnePhysique sourcier = (PersonnePhysique)tiersService.getTiers(ids.idSourcier);
		List<RapportPrestationImposable> rapportPrestations = tiersService.getAllRapportPrestationImposable(dpi,sourcier, true, true);
		List<RapportPrestationImposable> rapportPrestationsWithAnnule = tiersService.getAllRapportPrestationImposable(dpi,sourcier, false, true);

		assertEquals(2, rapportPrestations.size());
		assertEquals(2, rapportPrestationsWithAnnule.size());

		Collections.sort(rapportPrestations, new DateRangeComparator<RapportEntreTiers>());
		RapportPrestationImposable rapportPrestationImposableFerme = (RapportPrestationImposable) rapportPrestations.get(0);
		RapportPrestationImposable rapportPrestationImposableOuvert = (RapportPrestationImposable) rapportPrestations.get(1);

		assertEquals(date(2012, 12, 31), rapportPrestationImposableFerme.getDateFin());
		assertEquals(date(2013, 4, 1), rapportPrestationImposableOuvert.getDateDebut());
		assertEquals(null, rapportPrestationImposableOuvert.getDateFin());

	}

	//SIFISC-7549
	//Teste des cas 6 et 7
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCalculEcartPourCreationModificationRT() throws Exception {


		class Ids {
			Long idDebiteur;
			Long idSourcier;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable debiteur = addDebiteur();
				addForDebiteur(debiteur, date(2011, 1, 1), null, MockCommune.Echallens);
				ids.idDebiteur= debiteur.getNumero();
				PersonnePhysique sourcier = addHabitant(12365478L);
				ids.idSourcier= sourcier.getNumero();

				addRapportPrestationImposable(debiteur,sourcier,date(2011,10,1),date(2012,12,31),false);

				return null;
			}
		});

		final RegDate dateDebutPeriode = date(2013, 1, 1);
		final RegDate dateFinPeriode = date(2013, 3, 31);
		final RegDate dateDebutVersementSalaire = date(2013, 1, 3);
		final RegDate dateFinVersementSalaire = date(2013, 6, 30);
		final DateRange periodeDeclaration = new DateRangeHelper.Range(dateDebutPeriode,dateFinPeriode);


		final MiseAJourRapportTravailRequest request = createMiseAJourRapportTravailRequest(ids.idDebiteur, ids.idSourcier,periodeDeclaration, dateDebutVersementSalaire,dateFinVersementSalaire);
		request.setCreationProlongationRapportTravail(new CreationProlongationRapportTravail());
		MiseAJourRapportTravailResponse response =  doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(request, null));
			}
		});
		assertEquals(DataHelper.coreToXML(RegDate.get()),response.getDatePriseEnCompte());
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(ids.idDebiteur);
		final PersonnePhysique sourcier = (PersonnePhysique)tiersService.getTiers(ids.idSourcier);
		List<RapportPrestationImposable> rapportPrestations = tiersService.getAllRapportPrestationImposable(dpi,sourcier, true, true);

		assertEquals(2, rapportPrestations.size());

		Collections.sort(rapportPrestations, new DateRangeComparator<RapportEntreTiers>());
		RapportPrestationImposable rapportPrestationImposableFerme = (RapportPrestationImposable) rapportPrestations.get(0);
		RapportPrestationImposable rapportPrestationImposableOuvert = (RapportPrestationImposable) rapportPrestations.get(1);

		assertEquals(date(2012, 12, 31), rapportPrestationImposableFerme.getDateFin());
		assertEquals(dateDebutVersementSalaire, rapportPrestationImposableOuvert.getDateDebut());
		assertEquals(null, rapportPrestationImposableOuvert.getDateFin());

	}
	//le rapport de travail existant à une date de fin antérieur à la date de début de la période de déclaration.
	//le rapport est réouvert pour un écart inférieur ou égal a 1 jour et est fermé à la date de l'évènement en cas de décés
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRTFermeAvantPeriodeEcartUnJourEvenementFin() throws Exception {


		class Ids {
			Long idDebiteur;
			Long idSourcier;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable debiteur = addDebiteur();
				addForDebiteur(debiteur, date(2011, 1, 1), null, MockCommune.Echallens);
				ids.idDebiteur= debiteur.getNumero();
				PersonnePhysique sourcier = addHabitant(12365478L);
				ids.idSourcier= sourcier.getNumero();

				addRapportPrestationImposable(debiteur,sourcier,date(2011,1,1),date(2012,3,30),false);

				return null;
			}
		});

		final RegDate dateDebutPeriode = date(2012, 4, 1);
		final RegDate dateFinPeriode = date(2012, 12, 31);
		final RegDate dateDebutVersementSalaire = date(2012, 4, 1);
		final DateRange periodeDeclaration = new DateRangeHelper.Range(dateDebutPeriode,dateFinPeriode);


		final MiseAJourRapportTravailRequest request = createMiseAJourRapportTravailRequest(ids.idDebiteur, ids.idSourcier,periodeDeclaration, dateDebutVersementSalaire,null);


		FinRapportTravail finRapportTravail = new FinRapportTravail();
		finRapportTravail.setCode(FinRapportTravailType.DECES);
		final RegDate dateDeces = date(2012, 6, 30);
		final Date dateEvenement = DataHelper.coreToXML(dateDeces);
		finRapportTravail.setDateEvenement(dateEvenement);
		request.setFinRapportTravail(finRapportTravail);

		MiseAJourRapportTravailResponse response =  doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(request, null));
			}
		});
		assertEquals(DataHelper.coreToXML(RegDate.get()),response.getDatePriseEnCompte());
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(ids.idDebiteur);
		List<RapportEntreTiers> rapportPrestations = new ArrayList<>();
		rapportPrestations.addAll(dpi.getRapportsObjet());
		Collections.sort(rapportPrestations, new DateRangeComparator<RapportEntreTiers>());
		RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapportPrestations.get(1);
		assertEquals(dateDeces,rapportPrestationImposable.getDateFin());

	}

	//le rapport de travail existant à une date de fin antérieur à la date de début de la période de déclaration.
	//un nouveau  rapport est créé pour un écart supérieur à 1 jour
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRTFermeAvantPeriodeEcartPlusUnJour() throws Exception {


		class Ids {
			Long idDebiteur;
			Long idSourcier;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable debiteur = addDebiteur();
				addForDebiteur(debiteur, date(2011, 1, 1), null, MockCommune.Echallens);
				ids.idDebiteur= debiteur.getNumero();
				PersonnePhysique sourcier = addHabitant(12365478L);
				ids.idSourcier= sourcier.getNumero();

				addRapportPrestationImposable(debiteur,sourcier,date(2011,1,1),date(2012, 2, 18),false);

				return null;
			}
		});

		final RegDate dateDebutPeriode = date(2012, 4, 1);
		final RegDate dateFinPeriode = date(2012, 12, 31);
		final RegDate dateDebutVersementSalaire = date(2012, 4, 1);
		final DateRange periodeDeclaration = new DateRangeHelper.Range(dateDebutPeriode,dateFinPeriode);


		final MiseAJourRapportTravailRequest request = createMiseAJourRapportTravailRequest(ids.idDebiteur, ids.idSourcier,periodeDeclaration, dateDebutVersementSalaire,null);

		MiseAJourRapportTravailResponse response =  doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(request, null));
			}
		});
		assertEquals(DataHelper.coreToXML(RegDate.get()),response.getDatePriseEnCompte());
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(ids.idDebiteur);
		List<RapportEntreTiers> rapportPrestations = new ArrayList<>();
		rapportPrestations.addAll(dpi.getRapportsObjet());
		assertEquals(2, rapportPrestations.size());
		Collections.sort(rapportPrestations, new DateRangeComparator<RapportEntreTiers>());
		RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapportPrestations.get(1);
		assertEquals(dateDebutVersementSalaire,rapportPrestationImposable.getDateDebut());
		assertEquals(null,rapportPrestationImposable.getDateFin());

	}


	//le rapport de travail existant à une date de fin antérieur à la date de début de la période de déclaration.
	//un nouveau rapport est créé pour un écart supérieur à 1 jour en présence d'un evenement de sortie, la date de fin sera la date
	//de fin de versement de salaire
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRTFermeAvantPeriodeEcartPlusUnJourEvenementFin() throws Exception {


		class Ids {
			Long idDebiteur;
			Long idSourcier;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable debiteur = addDebiteur();
				addForDebiteur(debiteur, date(2011, 1, 1), null, MockCommune.Echallens);
				ids.idDebiteur= debiteur.getNumero();
				PersonnePhysique sourcier = addHabitant(12365478L);
				ids.idSourcier= sourcier.getNumero();

				addRapportPrestationImposable(debiteur,sourcier,date(2011,1,1),date(2012,2,18),false);

				return null;
			}
		});

		final RegDate dateDebutPeriode = date(2012, 4, 1);
		final RegDate dateFinPeriode = date(2012, 12, 31);
		final RegDate dateDebutVersementSalaire = date(2012, 4, 1);
		final RegDate dateFinVersementSalaire = date(2012, 8, 29);
		final DateRange periodeDeclaration = new DateRangeHelper.Range(dateDebutPeriode,dateFinPeriode);


		final MiseAJourRapportTravailRequest request = createMiseAJourRapportTravailRequest(ids.idDebiteur, ids.idSourcier,periodeDeclaration, dateDebutVersementSalaire,dateFinVersementSalaire);


		FinRapportTravail finRapportTravail = new FinRapportTravail();
		finRapportTravail.setCode(FinRapportTravailType.SORTIE);
		finRapportTravail.setDateEvenement(DataHelper.coreToXML(dateFinVersementSalaire));
		request.setFinRapportTravail(finRapportTravail);

		MiseAJourRapportTravailResponse response =  doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(request, null));
			}
		});
		assertEquals(DataHelper.coreToXML(RegDate.get()),response.getDatePriseEnCompte());
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(ids.idDebiteur);
		List<RapportEntreTiers> rapportPrestations = new ArrayList<>();
		rapportPrestations.addAll(dpi.getRapportsObjet());
		assertEquals(2, rapportPrestations.size());
		Collections.sort(rapportPrestations, new DateRangeComparator<RapportEntreTiers>());
		RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapportPrestations.get(1);
		assertEquals(dateDebutVersementSalaire,rapportPrestationImposable.getDateDebut());
		assertEquals(dateFinVersementSalaire,rapportPrestationImposable.getDateFin());

	}

	//Si deux rapports de travail existent et se chevauche  pour la période après application des règles de modification, il faut
	//les fusionner

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTraiterChevauchementRT() throws Exception {


		class Ids {
			Long idDebiteur;
			Long idSourcier;
		}
		final Ids ids = new Ids();
		final RegDate dateDebutPremierRapport = date(2011, 1, 1);

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable debiteur = addDebiteur();
				addForDebiteur(debiteur, date(2012, 1, 1), null, MockCommune.Echallens);
				ids.idDebiteur= debiteur.getNumero();
				PersonnePhysique sourcier = addHabitant(12365478L);
				ids.idSourcier= sourcier.getNumero();


				addRapportPrestationImposable(debiteur,sourcier, dateDebutPremierRapport,date(2012,5,24),false);
				addRapportPrestationImposable(debiteur,sourcier,date(2012,8,1),null,false);

				return null;
			}
		});

		final RegDate dateDebutPeriode = date(2012, 1, 1);
		final RegDate dateFinPeriode = date(2012, 6, 30);
		final RegDate dateDebutVersementSalaire = date(2012, 1, 1);
		final RegDate dateFinCore = date(2012, 6, 30);
		final DateRange periodeDeclaration = new DateRangeHelper.Range(dateDebutPeriode,dateFinPeriode);


		final MiseAJourRapportTravailRequest request = createMiseAJourRapportTravailRequest(ids.idDebiteur, ids.idSourcier,periodeDeclaration, dateDebutVersementSalaire,dateFinCore);

		MiseAJourRapportTravailResponse response =  doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(request, null));
			}
		});
		assertEquals(DataHelper.coreToXML(RegDate.get()),response.getDatePriseEnCompte());
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(ids.idDebiteur);
		PersonnePhysique sourcier = (PersonnePhysique) tiersService.getTiers(ids.idSourcier);
		List<RapportPrestationImposable> rapportPrestations = tiersService.getAllRapportPrestationImposable(dpi,sourcier, true, true);

		assertEquals(1,rapportPrestations.size());
		RapportPrestationImposable rapportPrestationImposable = rapportPrestations.get(0);

		assertEquals(dateDebutPremierRapport,rapportPrestationImposable.getDateDebut());
		assertEquals(null,rapportPrestationImposable.getDateFin());

	}


	//le rapport de travail existant à une date de fin dans la période de déclaration.
	//En cas d'absence d'evenement de fin ou de fermeture, on réouvre le rapport de travail
	//De plus si le for a une date de fin égal à la date de fin de la période de déclaration,
	//tous les RT encore ouverts doivent être fermés à cette date de fin
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRTExistantFormFerme() throws Exception {


		class Ids {
			Long idDebiteur;
			Long idSourcier;
		}
		final Ids ids = new Ids();
		final RegDate dateFermetureFor = date(2012, 11, 15);

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable debiteur = addDebiteur();

				addForDebiteur(debiteur, date(2012, 1, 1), dateFermetureFor, MockCommune.Echallens);
				ids.idDebiteur= debiteur.getNumero();
				PersonnePhysique sourcier = addHabitant(12365478L);
				ids.idSourcier= sourcier.getNumero();

				addRapportPrestationImposable(debiteur,sourcier,date(2011,1,1),date(2012,8,1),false);

				return null;
			}
		});

		final RegDate dateDebutPeriode = date(2012, 1, 1);
		final RegDate dateFinPeriode = dateFermetureFor;
		final RegDate dateDebutVersementSalaire = date(2012, 1, 1);
		final RegDate dateFinCore = date(2012, 6, 30);
		final Date dateFinVersementSalaire = DataHelper.coreToXML(dateFinCore);
		final Date dateEvenement = DataHelper.coreToXML(dateFinCore);
		final DateRange periodeDeclaration = new DateRangeHelper.Range(dateDebutPeriode,dateFinPeriode);


		final MiseAJourRapportTravailRequest request = createMiseAJourRapportTravailRequest(ids.idDebiteur, ids.idSourcier,periodeDeclaration, dateDebutVersementSalaire,dateFinCore);

		MiseAJourRapportTravailResponse response =  doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(request, null));
			}
		});
		assertEquals(DataHelper.coreToXML(RegDate.get()),response.getDatePriseEnCompte());
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(ids.idDebiteur);
		List<RapportEntreTiers> rapportPrestations = new ArrayList<>();
		rapportPrestations.addAll(dpi.getRapportsObjet());
		Collections.sort(rapportPrestations, new DateRangeComparator<RapportEntreTiers>());
		RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapportPrestations.get(1);
		assertEquals(dateFermetureFor,rapportPrestationImposable.getDateFin());

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
		request.setDateFinVersementSalaire(DataHelper.coreToXML(dateFinVersementSalaire));
		return request;
	}


}