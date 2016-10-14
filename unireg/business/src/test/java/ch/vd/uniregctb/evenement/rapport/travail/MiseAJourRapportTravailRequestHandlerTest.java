package ch.vd.uniregctb.evenement.rapport.travail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
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
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypePermis;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.ServiceException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
			assertEquals("Le débiteur 123.254.78 n'existe pas dans unireg", e.getMessage());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDebiteurNonActif() throws Exception {

		final Long idDebiteur = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable debiteur = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2012, 1, 1));
				addForDebiteur(debiteur, date(2012, 1, 1), MotifFor.INDETERMINE, date(2012, 6, 30), MotifFor.INDETERMINE, MockCommune.Echallens);
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

		identifiantRapportTravail.setDateDebutPeriodeDeclaration(DataHelper.coreToXMLv1(dateDebutPeriode));

		identifiantRapportTravail.setDateFinPeriodeDeclaration(DataHelper.coreToXMLv1(dateFinPeriode));
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
				addForDebiteur(debiteur, date(2012, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Echallens);
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
		assertEquals(DataHelper.coreToXMLv1(RegDate.get()),response.getDatePriseEnCompte());
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
				addForDebiteur(debiteur, date(2010, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Echallens);
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
		assertEquals(DataHelper.coreToXMLv1(RegDate.get()),response.getDatePriseEnCompte());
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
				addForDebiteur(debiteur, date(2012, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Echallens);
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
		assertEquals(DataHelper.coreToXMLv1(RegDate.get()),response.getDatePriseEnCompte());
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
				addForDebiteur(debiteur, date(2012, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Echallens);
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
		assertEquals(DataHelper.coreToXMLv1(RegDate.get()),response.getDatePriseEnCompte());
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(ids.idDebiteur);
		List<RapportEntreTiers> rapportPrestations = new ArrayList<>();
		rapportPrestations.addAll(dpi.getRapportsObjet());
		RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapportPrestations.get(0);
		assertTrue(rapportPrestationImposable.isAnnule());

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
				addForDebiteur(debiteur, date(2012, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Echallens);
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
		assertEquals(DataHelper.coreToXMLv1(RegDate.get()),response.getDatePriseEnCompte());
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
				addForDebiteur(debiteur, date(2012, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Echallens);
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
		final Date dateFinVersementSalaire = DataHelper.coreToXMLv1(dateFinCore);
		final Date dateEvenement = DataHelper.coreToXMLv1(dateFinCore);
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
		assertEquals(DataHelper.coreToXMLv1(RegDate.get()),response.getDatePriseEnCompte());
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
				addForDebiteur(debiteur, date(2012, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Echallens);
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
		final Date dateFinVersementSalaire = DataHelper.coreToXMLv1(dateFinCore);
		final Date dateEvenement = DataHelper.coreToXMLv1(dateFinCore);
		final DateRange periodeDeclaration = new DateRangeHelper.Range(dateDebutPeriode,dateFinPeriode);


		final MiseAJourRapportTravailRequest request = createMiseAJourRapportTravailRequest(ids.idDebiteur, ids.idSourcier,periodeDeclaration, dateDebutVersementSalaire,dateFinCore);

		MiseAJourRapportTravailResponse response =  doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(request, null));
			}
		});
		assertEquals(DataHelper.coreToXMLv1(RegDate.get()),response.getDatePriseEnCompte());
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(ids.idDebiteur);
		List<RapportEntreTiers> rapportPrestations = new ArrayList<>();
		rapportPrestations.addAll(dpi.getRapportsObjet());
		Collections.sort(rapportPrestations, new DateRangeComparator<>());
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
				addForDebiteur(debiteur, date(2011, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Echallens);
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
		final Date dateFinVersementSalaire = DataHelper.coreToXMLv1(dateFinCore);
		final DateRange periodeDeclaration = new DateRangeHelper.Range(dateDebutPeriode,dateFinPeriode);


		final MiseAJourRapportTravailRequest request = createMiseAJourRapportTravailRequest(ids.idDebiteur, ids.idSourcier,periodeDeclaration, dateDebutVersementSalaire,null);
		request.setDateFinVersementSalaire(dateFinVersementSalaire);

		MiseAJourRapportTravailResponse response =  doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(request, null));
			}
		});
		assertEquals(DataHelper.coreToXMLv1(RegDate.get()),response.getDatePriseEnCompte());
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(ids.idDebiteur);
		List<RapportEntreTiers> rapportPrestations = new ArrayList<>();
		rapportPrestations.addAll(dpi.getRapportsObjet());
		Collections.sort(rapportPrestations, new DateRangeComparator<>());
		RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapportPrestations.get(0);
		assertEquals(dateDebutVersementSalaire,rapportPrestationImposable.getDateDebut());

	}

	//le rapport de travail existant à une date de fin antérieur à la date de début de la période de déclaration.
	//le rapport est réouvert pour un écart inférieur ou égal a 1 jour
	@Test
	public void testRTFermeAvantPeriodeEcartDeuxJour() throws Exception {


		class Ids {
			Long idDebiteur;
			Long idSourcier;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable debiteur = addDebiteur();
				addForDebiteur(debiteur, date(2011, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Echallens);
				ids.idDebiteur= debiteur.getNumero();
				PersonnePhysique sourcier = addHabitant(12365478L);
				ids.idSourcier= sourcier.getNumero();

				addRapportPrestationImposable(debiteur, sourcier, date(2011, 1, 1), date(2012, 3, 30), false);
				return null;
			}
		});

		final RegDate dateDebutPeriode = date(2012, 4, 1);
		final RegDate dateFinPeriode = date(2012, 12, 31);
		final RegDate dateDebutVersementSalaire = date(2012, 4, 1);
		final DateRange periodeDeclaration = new DateRangeHelper.Range(dateDebutPeriode,dateFinPeriode);

		final MiseAJourRapportTravailRequest request = createMiseAJourRapportTravailRequest(ids.idDebiteur, ids.idSourcier,periodeDeclaration, dateDebutVersementSalaire,null);

		final MiseAJourRapportTravailResponse response =  doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(request, null));
			}
		});
		assertEquals(DataHelper.coreToXMLv1(RegDate.get()),response.getDatePriseEnCompte());

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(ids.idDebiteur);

				final List<RapportEntreTiers> rapportPrestations = new ArrayList<>(dpi.getRapportsObjet());
				assertEquals(2, rapportPrestations.size());

				Collections.sort(rapportPrestations, new DateRangeComparator<>());

				// rapport existant conservé ...
				{
					final RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapportPrestations.get(0);
					assertEquals(date(2011, 1, 1), rapportPrestationImposable.getDateDebut());
					assertEquals(date(2012, 3, 30), rapportPrestationImposable.getDateFin());
					assertFalse(rapportPrestationImposable.isAnnule());
				}

				// ... et nouveau rapport créé
				{
					final RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapportPrestations.get(1);
					assertEquals(date(2012, 4, 1), rapportPrestationImposable.getDateDebut());
					assertEquals(null, rapportPrestationImposable.getDateFin());
					assertFalse(rapportPrestationImposable.isAnnule());
				}
				return null;
			}
		});
	}

	//le rapport de travail existant à une date de fin antérieur à la date de début de la période de déclaration.
	//le rapport est réouvert pour un écart inférieur ou égal a 1 jour
	@Test
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
				addForDebiteur(debiteur, date(2011, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Echallens);
				ids.idDebiteur= debiteur.getNumero();
				PersonnePhysique sourcier = addHabitant(12365478L);
				ids.idSourcier= sourcier.getNumero();

				addRapportPrestationImposable(debiteur, sourcier, date(2011, 1, 1), date(2012, 3, 31), false);
				return null;
			}
		});

		final RegDate dateDebutPeriode = date(2012, 4, 1);
		final RegDate dateFinPeriode = date(2012, 12, 31);
		final RegDate dateDebutVersementSalaire = date(2012, 4, 1);
		final DateRange periodeDeclaration = new DateRangeHelper.Range(dateDebutPeriode,dateFinPeriode);

		final MiseAJourRapportTravailRequest request = createMiseAJourRapportTravailRequest(ids.idDebiteur, ids.idSourcier,periodeDeclaration, dateDebutVersementSalaire,null);

		final MiseAJourRapportTravailResponse response =  doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(request, null));
			}
		});
		assertEquals(DataHelper.coreToXMLv1(RegDate.get()),response.getDatePriseEnCompte());

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(ids.idDebiteur);

				final List<RapportEntreTiers> rapportPrestations = new ArrayList<>(dpi.getRapportsObjet());
				assertEquals(2, rapportPrestations.size());

				Collections.sort(rapportPrestations, new DateRangeComparator<>());

				// rapport existant annulé ...
				{
					final RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapportPrestations.get(0);
					assertEquals(date(2011, 1, 1), rapportPrestationImposable.getDateDebut());
					assertEquals(date(2012, 3, 31), rapportPrestationImposable.getDateFin());
					assertTrue(rapportPrestationImposable.isAnnule());
				}

				// ... et remplacé
				{
					final RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapportPrestations.get(1);
					assertEquals(date(2011, 1, 1), rapportPrestationImposable.getDateDebut());
					assertEquals(null, rapportPrestationImposable.getDateFin());
					assertFalse(rapportPrestationImposable.isAnnule());
				}
				return null;
			}
		});
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
				addForDebiteur(debiteur, date(2011, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Echallens);
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
		assertEquals(DataHelper.coreToXMLv1(RegDate.get()),response.getDatePriseEnCompte());
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(ids.idDebiteur);
		List<RapportEntreTiers> rapportPrestations = new ArrayList<>();
		rapportPrestations.addAll(dpi.getRapportsObjet());
		assertEquals(3,rapportPrestations.size());
		Collections.sort(rapportPrestations, new DateRangeComparator<>());

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
				addForDebiteur(debiteur, date(2011, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Echallens);
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
		assertEquals(DataHelper.coreToXMLv1(RegDate.get()),response.getDatePriseEnCompte());
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
				addForDebiteur(debiteur, date(2011, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Echallens);
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
		assertEquals(DataHelper.coreToXMLv1(RegDate.get()),response.getDatePriseEnCompte());
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
				addForDebiteur(debiteur, date(2011, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Echallens);
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
		final Date dateEvenement = DataHelper.coreToXMLv1(dateDeces);
		finRapportTravail.setDateEvenement(dateEvenement);
		request.setFinRapportTravail(finRapportTravail);

		MiseAJourRapportTravailResponse response =  doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(request, null));
			}
		});
		assertEquals(DataHelper.coreToXMLv1(RegDate.get()),response.getDatePriseEnCompte());
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(ids.idDebiteur);
		List<RapportEntreTiers> rapportPrestations = new ArrayList<>();
		rapportPrestations.addAll(dpi.getRapportsObjet());
		Collections.sort(rapportPrestations, new DateRangeComparator<>());
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
				addForDebiteur(debiteur, date(2011, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Echallens);
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
		assertEquals(DataHelper.coreToXMLv1(RegDate.get()),response.getDatePriseEnCompte());
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(ids.idDebiteur);
		List<RapportEntreTiers> rapportPrestations = new ArrayList<>();
		rapportPrestations.addAll(dpi.getRapportsObjet());
		assertEquals(2, rapportPrestations.size());
		Collections.sort(rapportPrestations, new DateRangeComparator<>());
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
				addForDebiteur(debiteur, date(2011, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Echallens);
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
		finRapportTravail.setDateEvenement(DataHelper.coreToXMLv1(dateFinVersementSalaire));
		request.setFinRapportTravail(finRapportTravail);

		MiseAJourRapportTravailResponse response =  doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(request, null));
			}
		});
		assertEquals(DataHelper.coreToXMLv1(RegDate.get()),response.getDatePriseEnCompte());
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(ids.idDebiteur);
		List<RapportEntreTiers> rapportPrestations = new ArrayList<>();
		rapportPrestations.addAll(dpi.getRapportsObjet());
		assertEquals(2, rapportPrestations.size());
		Collections.sort(rapportPrestations, new DateRangeComparator<>());
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
				addForDebiteur(debiteur, date(2012, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Echallens);
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
		assertEquals(DataHelper.coreToXMLv1(RegDate.get()),response.getDatePriseEnCompte());
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
		final RegDate dateFermetureFor = date(2012, 11, 30);

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable debiteur = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2012, 1, 1));

				addForDebiteur(debiteur, date(2012, 1, 1), MotifFor.INDETERMINE, dateFermetureFor, MotifFor.INDETERMINE, MockCommune.Echallens);
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
		final Date dateFinVersementSalaire = DataHelper.coreToXMLv1(dateFinCore);
		final Date dateEvenement = DataHelper.coreToXMLv1(dateFinCore);
		final DateRange periodeDeclaration = new DateRangeHelper.Range(dateDebutPeriode,dateFinPeriode);


		final MiseAJourRapportTravailRequest request = createMiseAJourRapportTravailRequest(ids.idDebiteur, ids.idSourcier,periodeDeclaration, dateDebutVersementSalaire,dateFinCore);

		MiseAJourRapportTravailResponse response =  doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(request, null));
			}
		});
		assertEquals(DataHelper.coreToXMLv1(RegDate.get()),response.getDatePriseEnCompte());
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(ids.idDebiteur);
		List<RapportEntreTiers> rapportPrestations = new ArrayList<>();
		rapportPrestations.addAll(dpi.getRapportsObjet());
		Collections.sort(rapportPrestations, new DateRangeComparator<>());
		RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapportPrestations.get(1);
		assertEquals(dateFermetureFor,rapportPrestationImposable.getDateFin());

	}

	private MiseAJourRapportTravailRequest createMiseAJourRapportTravailRequest(Long idDebiteur, Long idSourcier, DateRange periodeDeclaration,
	                                                                            RegDate dateDebutVersementSalaire, RegDate dateFinVersementSalaire) {
		final MiseAJourRapportTravailRequest request = new MiseAJourRapportTravailRequest();
		IdentifiantRapportTravail identifiantRapportTravail = new IdentifiantRapportTravail();
		identifiantRapportTravail.setNumeroDebiteur(idDebiteur.intValue());


		identifiantRapportTravail.setDateDebutPeriodeDeclaration(DataHelper.coreToXMLv1(periodeDeclaration.getDateDebut()));

		identifiantRapportTravail.setDateFinPeriodeDeclaration(DataHelper.coreToXMLv1(periodeDeclaration.getDateFin()));

		identifiantRapportTravail.setNumeroContribuable(idSourcier.intValue());
		request.setIdentifiantRapportTravail(identifiantRapportTravail);
		request.setDateDebutVersementSalaire(DataHelper.coreToXMLv1(dateDebutVersementSalaire));
		request.setDateFinVersementSalaire(DataHelper.coreToXMLv1(dateFinVersementSalaire));
		return request;
	}

	/**
	 * SIFISC-8663, création de chevauchement de rapports de travail lors d'une demande de fermeture
	 */
	@Test
	public void testFermetureRapportTravailSurCasAvecDeuxRapportsDisjoints() throws Exception {

		final long noIndividu = 323263L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Plantagenet", "Pierre-Henri", Sexe.MASCULIN);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Aubonne.CheminDesClos, null, date(2000, 1, 1), null);
				addNationalite(ind, MockPays.RoyaumeUni, date(2000, 1, 1), null);
				addPermis(ind, TypePermis.SEJOUR, date(2000, 1, 1), null, false);
			}
		});

		final class Ids {
			long idSourcier;
			long idDebiteur;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique sourcier = addHabitant(noIndividu);
				addForPrincipal(sourcier, date(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Aubonne, ModeImposition.SOURCE);

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2005, 1, 1));
				addForDebiteur(dpi, date(2005, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bussigny);

				addRapportPrestationImposable(dpi, sourcier, date(2005, 1, 1), date(2006, 4, 12), false);
				addRapportPrestationImposable(dpi, sourcier, date(2008, 1, 1), null, false);

				final Ids ids = new Ids();
				ids.idDebiteur = dpi.getNumero();
				ids.idSourcier = sourcier.getNumero();
				return ids;
			}
		});

		final MiseAJourRapportTravailRequest req = createMiseAJourRapportTravailRequest(ids.idDebiteur, ids.idSourcier, new DateRangeHelper.Range(date(2013, 1, 1), date(2013, 1, 31)), null, null);
		req.setFermetureRapportTravail(new FermetureRapportTravail());
		MiseAJourRapportTravailResponse response =  doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(req, null));
			}
		});
		assertEquals(DataHelper.coreToXMLv1(RegDate.get()), response.getDatePriseEnCompte());
		assertNull(response.getExceptionInfo());

		// vérification du résultat...
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(ids.idDebiteur);
				final PersonnePhysique sourcier = (PersonnePhysique) tiersDAO.get(ids.idSourcier);

				final List<RapportPrestationImposable> rapports = tiersService.getAllRapportPrestationImposable(dpi, sourcier, true, true);
				assertEquals(2,rapports.size());

				Collections.sort(rapports, new DateRangeComparator<>());
				{
					final RapportPrestationImposable rapport = rapports.get(0);
					assertNotNull(rapport);
					assertEquals(date(2005, 1, 1), rapport.getDateDebut());
					assertEquals(date(2006, 4, 12), rapport.getDateFin());      // <-- cette date ne doit pas être modifiée !
					assertFalse(rapport.isAnnule());
				}
				{
					final RapportPrestationImposable rapport = rapports.get(1);
					assertNotNull(rapport);
					assertEquals(date(2008, 1, 1), rapport.getDateDebut());
					assertEquals(date(2012, 12, 31), rapport.getDateFin());
					assertFalse(rapport.isAnnule());
				}
				return null;
			}
		});
	}

	/**
	 * SIFISC-8881
	 */
	@Test
	public void testEvenementSortieSurCasAvecDeuxRapportsDisjoints() throws Exception {

		final long noIndividu = 323263L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Plantagenet", "Pierre-Henri", Sexe.MASCULIN);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Aubonne.CheminDesClos, null, date(2000, 1, 1), null);
				addNationalite(ind, MockPays.RoyaumeUni, date(2000, 1, 1), null);
				addPermis(ind, TypePermis.SEJOUR, date(2000, 1, 1), null, false);
			}
		});

		final class Ids {
			long idSourcier;
			long idDebiteur;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique sourcier = addHabitant(noIndividu);
				addForPrincipal(sourcier, date(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Aubonne, ModeImposition.SOURCE);

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2005, 1, 1));
				addForDebiteur(dpi, date(2005, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bussigny);

				addRapportPrestationImposable(dpi, sourcier, date(2005, 1, 1), date(2006, 4, 12), false);
				addRapportPrestationImposable(dpi, sourcier, date(2008, 1, 1), null, false);

				final Ids ids = new Ids();
				ids.idDebiteur = dpi.getNumero();
				ids.idSourcier = sourcier.getNumero();
				return ids;
			}
		});

		final MiseAJourRapportTravailRequest req = createMiseAJourRapportTravailRequest(ids.idDebiteur, ids.idSourcier, new DateRangeHelper.Range(date(2013, 1, 1), date(2013, 1, 31)), null, null);
		req.setFinRapportTravail(new FinRapportTravail(FinRapportTravailType.SORTIE, DataHelper.coreToXMLv1(date(2013, 1, 12))));
		req.setDateDebutVersementSalaire(DataHelper.coreToXMLv1(date(2013, 1, 1)));
		req.setDateFinVersementSalaire(DataHelper.coreToXMLv1(date(2013, 1, 12)));
		final MiseAJourRapportTravailResponse response =  doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(req, null));
			}
		});
		assertEquals(DataHelper.coreToXMLv1(RegDate.get()), response.getDatePriseEnCompte());
		assertNull(response.getExceptionInfo());

		// vérification du résultat...
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(ids.idDebiteur);
				final PersonnePhysique sourcier = (PersonnePhysique) tiersDAO.get(ids.idSourcier);

				final List<RapportPrestationImposable> rapports = tiersService.getAllRapportPrestationImposable(dpi, sourcier, false, true);
				assertEquals(2,rapports.size());

				Collections.sort(rapports, new DateRangeComparator<>());
				{
					final RapportPrestationImposable rapport = rapports.get(0);
					assertNotNull(rapport);
					assertEquals(date(2005, 1, 1), rapport.getDateDebut());
					assertEquals(date(2006, 4, 12), rapport.getDateFin());
					assertFalse(rapport.isAnnule());
				}
				{
					final RapportPrestationImposable rapport = rapports.get(1);
					assertNotNull(rapport);
					assertEquals(date(2008, 1, 1), rapport.getDateDebut());
					assertEquals(date(2013, 1, 12), rapport.getDateFin());
					assertFalse(rapport.isAnnule());
				}
				return null;
			}
		});
	}

	/**
	 * SIFISC-8883
	 */
	@Test
	public void testDeplacementDateFinRapport() throws Exception {

		final long noIndividu = 323263L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Plantagenet", "Pierre-Henri", Sexe.MASCULIN);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Aubonne.CheminDesClos, null, date(2000, 1, 1), null);
				addNationalite(ind, MockPays.RoyaumeUni, date(2000, 1, 1), null);
				addPermis(ind, TypePermis.SEJOUR, date(2000, 1, 1), null, false);
			}
		});

		final class Ids {
			long idSourcier;
			long idDebiteur;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique sourcier = addHabitant(noIndividu);
				addForPrincipal(sourcier, date(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Aubonne, ModeImposition.SOURCE);

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2005, 1, 1));
				addForDebiteur(dpi, date(2005, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bussigny);

				addRapportPrestationImposable(dpi, sourcier, date(2005, 1, 1), date(2006, 4, 12), false);
				addRapportPrestationImposable(dpi, sourcier, date(2008, 1, 1), date(2012, 12, 31), false);

				final Ids ids = new Ids();
				ids.idDebiteur = dpi.getNumero();
				ids.idSourcier = sourcier.getNumero();
				return ids;
			}
		});

		final MiseAJourRapportTravailRequest req = createMiseAJourRapportTravailRequest(ids.idDebiteur, ids.idSourcier, new DateRangeHelper.Range(date(2013, 1, 1), date(2013, 1, 31)), null, null);
		req.setFinRapportTravail(new FinRapportTravail(FinRapportTravailType.SORTIE, DataHelper.coreToXMLv1(date(2013, 1, 10))));
		req.setDateDebutVersementSalaire(DataHelper.coreToXMLv1(date(2013, 1, 1)));
		req.setDateFinVersementSalaire(DataHelper.coreToXMLv1(date(2013, 1, 10)));
		final MiseAJourRapportTravailResponse response =  doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(req, null));
			}
		});
		assertEquals(DataHelper.coreToXMLv1(RegDate.get()), response.getDatePriseEnCompte());
		assertNull(response.getExceptionInfo());

		// vérification du résultat...
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(ids.idDebiteur);
				final PersonnePhysique sourcier = (PersonnePhysique) tiersDAO.get(ids.idSourcier);

				final List<RapportPrestationImposable> rapports = tiersService.getAllRapportPrestationImposable(dpi, sourcier, false, true);
				assertEquals(3,rapports.size());

				Collections.sort(rapports, new DateRangeComparator<>());
				{
					final RapportPrestationImposable rapport = rapports.get(0);
					assertNotNull(rapport);
					assertEquals(date(2005, 1, 1), rapport.getDateDebut());
					assertEquals(date(2006, 4, 12), rapport.getDateFin());
					assertFalse(rapport.isAnnule());
				}
				{
					final RapportPrestationImposable rapport = rapports.get(1);
					assertNotNull(rapport);
					assertEquals(date(2008, 1, 1), rapport.getDateDebut());
					assertEquals(date(2012, 12, 31), rapport.getDateFin());
					assertTrue(rapport.isAnnule());
				}
				{
					final RapportPrestationImposable rapport = rapports.get(2);
					assertNotNull(rapport);
					assertEquals(date(2008, 1, 1), rapport.getDateDebut());
					assertEquals(date(2013, 1, 10), rapport.getDateFin());
					assertFalse(rapport.isAnnule());
				}
				return null;
			}
		});
	}






	//SIFISC 8135
	@Test
	public void testFermetureSurDoublon() throws Exception {

		final long noIndividu = 323263L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Plantagenet", "Pierre-Henri", Sexe.MASCULIN);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Aubonne.CheminDesClos, null, date(2000, 1, 1), null);
				addNationalite(ind, MockPays.RoyaumeUni, date(2000, 1, 1), null);
				addPermis(ind, TypePermis.SEJOUR, date(2000, 1, 1), null, false);
			}
		});

		final class Ids {
			long idSourcier;
			long idDebiteur;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique sourcier = addHabitant(noIndividu);
				addForPrincipal(sourcier, date(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Aubonne, ModeImposition.SOURCE);

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2005, 1, 1));
				addForDebiteur(dpi, date(2005, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bussigny);

				addRapportPrestationImposable(dpi, sourcier, date(2011, 1, 1), null, false);
				addRapportPrestationImposable(dpi, sourcier, date(2011, 1, 1), null, false);

				final Ids ids = new Ids();
				ids.idDebiteur = dpi.getNumero();
				ids.idSourcier = sourcier.getNumero();
				return ids;
			}
		});

		final MiseAJourRapportTravailRequest req = createMiseAJourRapportTravailRequest(ids.idDebiteur, ids.idSourcier, new DateRangeHelper.Range(date(2013, 1, 1), date(2013, 1, 31)), null, null);
		req.setFinRapportTravail(new FinRapportTravail(FinRapportTravailType.SORTIE, DataHelper.coreToXMLv1(date(2013, 1, 20))));
		req.setCreationProlongationRapportTravail(new CreationProlongationRapportTravail());
		req.setDateDebutVersementSalaire(DataHelper.coreToXMLv1(date(2013, 1, 10)));
		req.setDateFinVersementSalaire(DataHelper.coreToXMLv1(date(2013, 1, 20)));
		final MiseAJourRapportTravailResponse response =  doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(req, null));
			}
		});
		assertEquals(DataHelper.coreToXMLv1(RegDate.get()), response.getDatePriseEnCompte());
		assertNull(response.getExceptionInfo());

		// vérification du résultat...
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(ids.idDebiteur);
				final PersonnePhysique sourcier = (PersonnePhysique) tiersDAO.get(ids.idSourcier);

				final List<RapportPrestationImposable> rapports = tiersService.getAllRapportPrestationImposable(dpi, sourcier, false, true);
				assertEquals(2,rapports.size());

				Collections.sort(rapports, new DateRangeComparator<>());
				{
					final RapportPrestationImposable rapport = rapports.get(0);
					assertNotNull(rapport);
					assertEquals(date(2011, 1, 1), rapport.getDateDebut());
					assertEquals(date(2013, 1, 20), rapport.getDateFin());
					assertFalse(rapport.isAnnule());
				}
				{
					final RapportPrestationImposable rapport = rapports.get(1);
					assertNotNull(rapport);
					assertEquals(date(2011, 1, 1), rapport.getDateDebut());
					assertEquals(null, rapport.getDateFin());
					assertTrue(rapport.isAnnule());
				}
				return null;
			}
		});
	}

	//SIFISC 8135
	@Test
	public void testFermetureSurMultiple() throws Exception {

		final long noIndividu = 323263L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Plantagenet", "Pierre-Henri", Sexe.MASCULIN);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Aubonne.CheminDesClos, null, date(2000, 1, 1), null);
				addNationalite(ind, MockPays.RoyaumeUni, date(2000, 1, 1), null);
				addPermis(ind, TypePermis.SEJOUR, date(2000, 1, 1), null, false);
			}
		});

		final class Ids {
			long idSourcier;
			long idDebiteur;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique sourcier = addHabitant(noIndividu);
				addForPrincipal(sourcier, date(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Aubonne, ModeImposition.SOURCE);

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2005, 1, 1));
				addForDebiteur(dpi, date(2005, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bussigny);

				addRapportPrestationImposable(dpi, sourcier, date(2011, 1, 1), null, false);
				addRapportPrestationImposable(dpi, sourcier, date(2010, 1, 1), null, false);
				addRapportPrestationImposable(dpi, sourcier, date(2012, 3, 1), null, false);

				final Ids ids = new Ids();
				ids.idDebiteur = dpi.getNumero();
				ids.idSourcier = sourcier.getNumero();
				return ids;
			}
		});

		final MiseAJourRapportTravailRequest req = createMiseAJourRapportTravailRequest(ids.idDebiteur, ids.idSourcier, new DateRangeHelper.Range(date(2013, 1, 1), date(2013, 1, 31)), null, null);
		req.setFinRapportTravail(new FinRapportTravail(FinRapportTravailType.SORTIE, DataHelper.coreToXMLv1(date(2013, 1, 20))));
		req.setCreationProlongationRapportTravail(new CreationProlongationRapportTravail());
		req.setDateDebutVersementSalaire(DataHelper.coreToXMLv1(date(2013, 1, 10)));
		req.setDateFinVersementSalaire(DataHelper.coreToXMLv1(date(2013, 1, 20)));
		final MiseAJourRapportTravailResponse response =  doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(req, null));
			}
		});
		assertEquals(DataHelper.coreToXMLv1(RegDate.get()), response.getDatePriseEnCompte());
		assertNull(response.getExceptionInfo());

		// vérification du résultat...
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(ids.idDebiteur);
				final PersonnePhysique sourcier = (PersonnePhysique) tiersDAO.get(ids.idSourcier);

				final List<RapportPrestationImposable> rapports = tiersService.getAllRapportPrestationImposable(dpi, sourcier, false, true);
				assertEquals(3,rapports.size());

				Collections.sort(rapports, new DateRangeComparator<>());
				{
					final RapportPrestationImposable rapport = rapports.get(0);
					assertNotNull(rapport);
					assertEquals(date(2010, 1, 1), rapport.getDateDebut());
					assertEquals(date(2013, 1, 20), rapport.getDateFin());
					assertFalse(rapport.isAnnule());
				}

				{
					final RapportPrestationImposable rapport = rapports.get(1);
					assertNotNull(rapport);
					assertEquals(date(2011, 1, 1), rapport.getDateDebut());
					assertEquals(null, rapport.getDateFin());
					assertTrue(rapport.isAnnule());
				}
				{
					final RapportPrestationImposable rapport = rapports.get(2);
					assertNotNull(rapport);
					assertEquals(date(2012, 3, 1), rapport.getDateDebut());
					assertEquals(null, rapport.getDateFin());
					assertTrue(rapport.isAnnule());
				}

				return null;
			}
		});
	}

	/**
	 * CODE FERMETURE SUR MESSAGE CAS 16
	 * Dans le cas ou une date de fin de rapport de travail est antérieur ou égal à la date de fin de la période de déclaration
	 * la date de fin du rapport de travail recoit la valeur date de début de période -1 jour
	 * [SIFISC-8964] l'ancien rapport doit être annulé pour conserver l'historique
	 */
	@Test
	public void testDemandeFermetureDateAnterieureAFermetureExistante() throws Exception {

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne !
			}
		});

		final class Ids {
			long idSourcier;
			long idDebiteur;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique sourcier = addNonHabitant("Harry", "Miettinnen", date(1980, 6, 30), Sexe.MASCULIN);
				addForPrincipal(sourcier, date(2010, 4, 1), MotifFor.ARRIVEE_HS, MockCommune.Nyon, ModeImposition.SOURCE);

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2013, 1, 1));
				addForDebiteur(dpi, date(2013, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Aubonne);
				addRapportPrestationImposable(dpi, sourcier, date(2013, 1, 1), date(2013, 4, 1), false);

				final Ids ids = new Ids();
				ids.idSourcier = sourcier.getNumero();
				ids.idDebiteur = dpi.getNumero();
				return ids;
			}
		});

		final MiseAJourRapportTravailRequest req = createMiseAJourRapportTravailRequest(ids.idDebiteur, ids.idSourcier, new DateRangeHelper.Range(date(2013, 4, 1), date(2013, 4, 30)), null, null);
		req.setFermetureRapportTravail(new FermetureRapportTravail());

		final MiseAJourRapportTravailResponse response =  doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(req, null));
			}
		});
		assertEquals(DataHelper.coreToXMLv1(RegDate.get()), response.getDatePriseEnCompte());
		assertNull(response.getExceptionInfo());

		// allons voir en base
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(ids.idDebiteur);
				final PersonnePhysique sourcier = (PersonnePhysique) tiersDAO.get(ids.idSourcier);

				final List<RapportPrestationImposable> rapports = tiersService.getAllRapportPrestationImposable(dpi, sourcier, false, true);
				assertNotNull(rapports);
				assertEquals(2, rapports.size());

				Collections.sort(rapports, new DateRangeComparator<>());
				{
					final RapportPrestationImposable rapport = rapports.get(0);
					assertNotNull(rapport);
					assertEquals(date(2013, 1, 1), rapport.getDateDebut());
					assertEquals(date(2013, 3, 31), rapport.getDateFin());
					assertFalse(rapport.isAnnule());
				}
				{
					final RapportPrestationImposable rapport = rapports.get(1);
					assertNotNull(rapport);
					assertEquals(date(2013, 1, 1), rapport.getDateDebut());
					assertEquals(date(2013, 4, 1), rapport.getDateFin());
					assertTrue(rapport.isAnnule());
				}

				return null;
			}
		});
	}

	/**
	 * C'est le cas du SIFISC-10334 : on avait deux rapports qui se chevauchaient et la réception d'une "sortie" -> modification des rapports existants et chevauchement conservé...
	 */
	@Test
	public void testFermetureSurChevauchementSansEnglobant() throws Exception {

		final long noIndividuSourcier = 478267L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividuSourcier, date(1965, 9, 17), "O'Hara", "Starlett", Sexe.FEMININ);
			}
		});

		final class Ids {
			long pp;
			long dpi;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique sourcier = addHabitant(noIndividuSourcier);
				addForPrincipal(sourcier, date(2009, 6, 1), MotifFor.ARRIVEE_HS, MockCommune.Aigle, ModeImposition.SOURCE);

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Lausanne);
				addRapportPrestationImposable(dpi, sourcier, date(2009, 6, 1), date(2012, 12, 31), false);
				addRapportPrestationImposable(dpi, sourcier, date(2012, 1, 1), date(2013, 5, 10), false);

				final Ids ids = new Ids();
				ids.pp = sourcier.getNumero();
				ids.dpi = dpi.getNumero();
				return ids;
			}
		});

		final MiseAJourRapportTravailRequest req = createMiseAJourRapportTravailRequest(ids.dpi, ids.pp, new DateRangeHelper.Range(date(2013, 5, 1), date(2013, 5, 31)), date(2013, 5, 1), date(2013, 5, 31));
		req.setFinRapportTravail(new FinRapportTravail(FinRapportTravailType.SORTIE, DataHelper.coreToXMLv1(date(2013, 5, 15))));

		final MiseAJourRapportTravailResponse response =  doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(req, null));
			}
		});
		assertEquals(DataHelper.coreToXMLv1(RegDate.get()), response.getDatePriseEnCompte());
		assertNull(response.getExceptionInfo());

		// vérification des rapports au final -> les deux existants doivent avoir été annulés et remplacé par leur fusion, sans autre modification
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(ids.dpi);
				final PersonnePhysique sourcier = (PersonnePhysique) tiersDAO.get(ids.pp);

				final List<RapportPrestationImposable> rapports = tiersService.getAllRapportPrestationImposable(dpi, sourcier, false, true);
				assertNotNull(rapports);
				assertEquals(3, rapports.size());

				Collections.sort(rapports, new DateRangeComparator<>());
				{
					final RapportPrestationImposable rapport = rapports.get(0);
					assertNotNull(rapport);
					assertEquals(date(2009, 6, 1), rapport.getDateDebut());
					assertEquals(date(2012, 12, 31), rapport.getDateFin());
					assertTrue(rapport.isAnnule());
				}
				{
					final RapportPrestationImposable rapport = rapports.get(1);
					assertNotNull(rapport);
					assertEquals(date(2009, 6, 1), rapport.getDateDebut());
					assertEquals(date(2013, 5, 10), rapport.getDateFin());
					assertFalse(rapport.isAnnule());
				}
				{
					final RapportPrestationImposable rapport = rapports.get(2);
					assertNotNull(rapport);
					assertEquals(date(2012, 1, 1), rapport.getDateDebut());
					assertEquals(date(2013, 5, 10), rapport.getDateFin());
					assertTrue(rapport.isAnnule());
				}
			}
		});
	}

	/**
	 * C'est un cas dérivé du SIFISC-10334 : on avait deux rapports qui se chevauchaient et la réception d'une "sortie" -> modification des rapports existants et chevauchement conservé...
	 */
	@Test
	public void testFermetureSurChevauchementAvecEnglobant() throws Exception {

		final long noIndividuSourcier = 478267L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividuSourcier, date(1965, 9, 17), "O'Hara", "Starlett", Sexe.FEMININ);
			}
		});

		final class Ids {
			long pp;
			long dpi;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique sourcier = addHabitant(noIndividuSourcier);
				addForPrincipal(sourcier, date(2009, 6, 1), MotifFor.ARRIVEE_HS, MockCommune.Aigle, ModeImposition.SOURCE);

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Lausanne);
				addRapportPrestationImposable(dpi, sourcier, date(2009, 6, 1), date(2013, 5, 10), false);
				addRapportPrestationImposable(dpi, sourcier, date(2012, 1, 1), date(2013, 5, 10), false);

				final Ids ids = new Ids();
				ids.pp = sourcier.getNumero();
				ids.dpi = dpi.getNumero();
				return ids;
			}
		});

		final MiseAJourRapportTravailRequest req = createMiseAJourRapportTravailRequest(ids.dpi, ids.pp, new DateRangeHelper.Range(date(2013, 5, 1), date(2013, 5, 31)), date(2013, 5, 1), date(2013, 5, 31));
		req.setFinRapportTravail(new FinRapportTravail(FinRapportTravailType.SORTIE, DataHelper.coreToXMLv1(date(2013, 5, 15))));

		final MiseAJourRapportTravailResponse response =  doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(req, null));
			}
		});
		assertEquals(DataHelper.coreToXMLv1(RegDate.get()), response.getDatePriseEnCompte());
		assertNull(response.getExceptionInfo());

		// vérification des rapports au final -> les deux existants doivent avoir été annulés et remplacé par leur fusion, sans autre modification
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(ids.dpi);
				final PersonnePhysique sourcier = (PersonnePhysique) tiersDAO.get(ids.pp);

				final List<RapportPrestationImposable> rapports = tiersService.getAllRapportPrestationImposable(dpi, sourcier, false, true);
				assertNotNull(rapports);
				assertEquals(2, rapports.size());

				Collections.sort(rapports, new DateRangeComparator<>());
				{
					final RapportPrestationImposable rapport = rapports.get(0);
					assertNotNull(rapport);
					assertEquals(date(2009, 6, 1), rapport.getDateDebut());
					assertEquals(date(2013, 5, 10), rapport.getDateFin());
					assertFalse(rapport.isAnnule());
				}
				{
					final RapportPrestationImposable rapport = rapports.get(1);
					assertNotNull(rapport);
					assertEquals(date(2012, 1, 1), rapport.getDateDebut());
					assertEquals(date(2013, 5, 10), rapport.getDateFin());
					assertTrue(rapport.isAnnule());
				}
			}
		});
	}

	@Test
	public void testFermetureAvecChevauchementPrealable() throws Exception {

		final long noIndividuSourcier = 478267L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividuSourcier, date(1965, 9, 17), "O'Hara", "Starlett", Sexe.FEMININ);
			}
		});

		final class Ids {
			long pp;
			long dpi;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique sourcier = addHabitant(noIndividuSourcier);
				addForPrincipal(sourcier, date(2009, 6, 1), MotifFor.ARRIVEE_HS, MockCommune.Aigle, ModeImposition.SOURCE);

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Lausanne);
				addRapportPrestationImposable(dpi, sourcier, date(2009, 6, 1), date(2012, 12, 31), false);
				addRapportPrestationImposable(dpi, sourcier, date(2012, 1, 1), date(2013, 5, 10), false);

				final Ids ids = new Ids();
				ids.pp = sourcier.getNumero();
				ids.dpi = dpi.getNumero();
				return ids;
			}
		});

		final MiseAJourRapportTravailRequest req = createMiseAJourRapportTravailRequest(ids.dpi, ids.pp, new DateRangeHelper.Range(date(2013, 5, 1), date(2013, 5, 31)), null, null);
		req.setFermetureRapportTravail(new FermetureRapportTravail());

		final MiseAJourRapportTravailResponse response = doInNewTransaction(new TxCallback<MiseAJourRapportTravailResponse>() {
			@Override
			public MiseAJourRapportTravailResponse execute(TransactionStatus status) throws Exception {
				return handler.handle(MiseAjourRapportTravail.get(req, null));
			}
		});
		assertEquals(DataHelper.coreToXMLv1(RegDate.get()), response.getDatePriseEnCompte());
		assertNull(response.getExceptionInfo());

		// vérification des rapports au final -> les deux existants doivent avoir été annulés et remplacé par leur fusion, sans autre modification
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(ids.dpi);
				final PersonnePhysique sourcier = (PersonnePhysique) tiersDAO.get(ids.pp);

				final List<RapportPrestationImposable> rapports = tiersService.getAllRapportPrestationImposable(dpi, sourcier, false, true);
				assertNotNull(rapports);
				assertEquals(3, rapports.size());

				Collections.sort(rapports, new DateRangeComparator<>());
				{
					final RapportPrestationImposable rapport = rapports.get(0);
					assertNotNull(rapport);
					assertEquals(date(2009, 6, 1), rapport.getDateDebut());
					assertEquals(date(2012, 12, 31), rapport.getDateFin());
					assertTrue(rapport.isAnnule());
				}
				{
					final RapportPrestationImposable rapport = rapports.get(1);
					assertNotNull(rapport);
					assertEquals(date(2009, 6, 1), rapport.getDateDebut());
					assertEquals(date(2013, 4, 30), rapport.getDateFin());
					assertFalse(rapport.isAnnule());
				}
				{
					final RapportPrestationImposable rapport = rapports.get(2);
					assertNotNull(rapport);
					assertEquals(date(2012, 1, 1), rapport.getDateDebut());
					assertEquals(date(2013, 5, 10), rapport.getDateFin());
					assertTrue(rapport.isAnnule());
				}
			}
		});
	}
}