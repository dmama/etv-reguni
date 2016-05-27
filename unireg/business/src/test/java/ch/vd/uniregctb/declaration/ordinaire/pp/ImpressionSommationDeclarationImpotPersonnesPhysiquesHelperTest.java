package ch.vd.uniregctb.declaration.ordinaire.pp;

import java.util.Collections;
import java.util.GregorianCalendar;

import noNamespace.FichierImpressionDocument;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.DefaultMockServiceInfrastructureService;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.LegacyEditiqueHelper;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.EtatDelaiDeclaration;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ImpressionSommationDeclarationImpotPersonnesPhysiquesHelperTest extends BusinessTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImpressionDeclarationImpotPersonnesPhysiquesHelperTest.class);

	private ImpressionSommationDeclarationImpotPersonnesPhysiquesHelperImpl impressionSommationDIHelper;
	private AdresseService adresseService;
	private TiersService tiersService;
	private LegacyEditiqueHelper editiqueHelper;
	private DelaisService delaisService;
	private PeriodeFiscale periodeFiscale;
	private ModeleDocument modeleDocument;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		adresseService = getBean(AdresseService.class, "adresseService");
		tiersService = getBean(TiersService.class, "tiersService");
		editiqueHelper =  getBean(LegacyEditiqueHelper.class, "legacyEditiqueHelper");
		delaisService =  getBean(DelaisService.class, "delaisService");
		serviceInfra.setUp(new DefaultMockServiceInfrastructureService());
		impressionSommationDIHelper = new ImpressionSommationDeclarationImpotPersonnesPhysiquesHelperImpl(serviceInfra, adresseService, tiersService,  editiqueHelper, delaisService);

		final Object[] res = doInTransaction(new TransactionCallback<Object[]>() {
			@Override
			public Object[] doInTransaction(TransactionStatus status) {
				final PeriodeFiscale pf = addPeriodeFiscale(2009);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf);
				return new Object[]{pf, md};
			}
		});
		periodeFiscale = (PeriodeFiscale) res[0];
		modeleDocument = (ModeleDocument) res[1];
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testConstruitIdArchivageDocument() throws Exception {
		LOGGER.debug("EditiqueHelperTest - testConstruitIdArchivageDocument");
		String idArchivageAttendu = "200902 Sommation DI        0101123020000";

		DeclarationImpotOrdinairePP declaration = new DeclarationImpotOrdinairePP();
		declaration.setNumero(2);
		declaration.setPeriode(periodeFiscale);
		GregorianCalendar cal = new GregorianCalendar(2007, 0, 1, 12, 30, 20);
		declaration.setLogCreationDate(cal.getTime());

		Assert.assertEquals(idArchivageAttendu, impressionSommationDIHelper.construitIdArchivageDocument(declaration));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testConstruitAncienIdArchivageDocument() throws Exception {
		LOGGER.debug("EditiqueHelperTest - testConstruitAncienIdArchivageDocument");
		String idArchivageAttendu = "200902 Sommation DI         200701011230";

		DeclarationImpotOrdinairePP declaration = new DeclarationImpotOrdinairePP();
		declaration.setNumero(2);
		declaration.setPeriode(periodeFiscale);
		GregorianCalendar cal = new GregorianCalendar(2007, 0, 1, 12, 30, 20);
		declaration.setLogCreationDate(cal.getTime());


		Assert.assertEquals(idArchivageAttendu, impressionSommationDIHelper.construitAncienIdArchivageDocument(declaration));

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testConstruitAncienIdArchivageDocumentPourOnLine() throws Exception {
		LOGGER.debug("EditiqueHelperTest - testConstruitAncienIdArchivageDocumentPourOnLine");
		String idArchivageAttendu = "200902 Sommation DI         20070101123020000";

		DeclarationImpotOrdinairePP declaration = new DeclarationImpotOrdinairePP();
		declaration.setNumero(2);
		declaration.setPeriode(periodeFiscale);
		GregorianCalendar cal = new GregorianCalendar(2007, 0, 1, 12, 30, 20);
		declaration.setLogCreationDate(cal.getTime());

		Assert.assertEquals(idArchivageAttendu, impressionSommationDIHelper.construitAncienIdArchivageDocumentPourOnLine(declaration));
	}

	/**
	 * [SIFISC-5325] Le message d'erreur qui sort dans le rapport d'exécution du batch de sommation des DI doit indiquer clairement si l'individu n'a pas été trouvé dans le registre civil et que c'est ça
	 * qui pose problème
	 */
	@Test
	public void testExceptionRecueQuandIndividuNonPresentDansRegistreCivil() throws Exception {

		final long noIndividu = 213567254L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne !
			}
		});

		// préparation fiscale
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, date(2009, 1, 1), MotifFor.ARRIVEE_HS, date(2009, 12, 31), MotifFor.DEPART_HS, MockCommune.Lausanne);
				final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp, periodeFiscale, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modeleDocument);
				di.setEtats(Collections.<EtatDeclaration>singleton(new EtatDeclarationEmise(date(2009, 1, 1))));
				final ImpressionSommationDIHelperParams params = ImpressionSommationDIHelperParams.createBatchParams(di, false, RegDate.get());
				try {
					impressionSommationDIHelper.remplitSommationDI(params);
					Assert.fail("Devrait exploser car l'individu n'est pas dans le registre civil...");
				}
				catch (EditiqueException e) {
					final String expectedMessage =
							String.format("Exception lors du calcul de l'affranchissement de l'adresse du tiers %d (Impossible de trouver l'individu n°%d)", pp.getNumero(), noIndividu);
					Assert.assertEquals(expectedMessage, e.getMessage());
				}
				return null;
			}
		});
	}

	@Test
	public void testRemplitSommationDIPourLesSepares() throws Exception {

		final long noIndividu = 213567254L;
		final RegDate dateDivorce = date(2010, 6, 1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu jean = addIndividu(noIndividu, null, "Dupont", "Jean", Sexe.MASCULIN);
				addAdresse(jean, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, dateDivorce, null);
				final MockIndividu marie = addIndividu(noIndividu + 1, null, "Dupont", "Marie", Sexe.FEMININ);
				addAdresse(marie, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeLaGare, null, dateDivorce, null);
				divorceIndividus(jean, marie, dateDivorce);
			}
		});

		// préparation fiscale
		final DeclarationImpotOrdinairePP di = doInTransaction(new TransactionCallback<DeclarationImpotOrdinairePP>() {
			@Override
			public DeclarationImpotOrdinairePP doInTransaction(TransactionStatus status) {
				final PersonnePhysique jean = addHabitant(noIndividu);
				final PersonnePhysique marie = addHabitant(noIndividu + 1);

				final EnsembleTiersCouple etc = addEnsembleTiersCouple(jean, marie, dateDivorce.addYears(-3), dateDivorce.getOneDayBefore());

				addForPrincipal(etc.getMenage(),
						dateDivorce.addYears(-3), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
						dateDivorce.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
						MockCommune.Lausanne);
				addForPrincipal(jean,
						dateDivorce, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				addForPrincipal(marie,
						dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);

				final DeclarationImpotOrdinairePP di = addDeclarationImpot(etc.getMenage(), periodeFiscale, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modeleDocument);
				EtatDeclaration etatEmise = new EtatDeclarationEmise();
				etatEmise.setDateObtention(date(2010, 1, 1));
				etatEmise.setDeclaration(di);
				di.setEtats(Collections.singleton(etatEmise));
				return di;
			}
		});

		// Test Sommation
		doInTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final ImpressionSommationDIHelperParams params = ImpressionSommationDIHelperParams.createBatchParams(di, false, RegDate.get());
				FichierImpressionDocument fichier;
				try {
					fichier = impressionSommationDIHelper.remplitSommationDI(params);
				}
				catch (EditiqueException e) {
					throw new RuntimeException(e);
				}
				assertEquals("On devrait avoir 2 documents pour les séparés", 2, fichier.getFichierImpression().getDocumentArray().length);
				assertNotNull(fichier.getFichierImpression().getDocumentArray(0).getInfoDocument().getSepares());
				assertNotNull(fichier.getFichierImpression().getDocumentArray(1).getInfoDocument().getSepares());
				return null;
			}
		});

	}

	@Test
	public void testGetLocaliteExpeditionWithLaValleeException() throws Exception {

// préparation fiscale
		final DeclarationImpotOrdinairePP di = doInTransaction(new TransactionCallback<DeclarationImpotOrdinairePP>() {
			@Override
			public DeclarationImpotOrdinairePP doInTransaction(TransactionStatus status) {


				final PersonnePhysique pp = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);

				addForPrincipal(pp, date(2009, 1, 1), MotifFor.DEPART_HS, null, null, MockCommune.Fraction.LeSentier);

				final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp, periodeFiscale, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modeleDocument);
				di.setNumeroOfsForGestion(MockCommune.LeChenit.getNoOFS());
				EtatDeclaration etatEmise = new EtatDeclarationEmise();
				etatEmise.setDateObtention(date(2010, 1, 1));
				etatEmise.setDeclaration(di);
				di.setEtats(Collections.singleton(etatEmise));
				return di;
			}
		});

		doInTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
			final ImpressionSommationDIHelperParams params = ImpressionSommationDIHelperParams.createBatchParams(di, false, RegDate.get());
			FichierImpressionDocument fichier;
			try {
				fichier = impressionSommationDIHelper.remplitSommationDI(params);
			}
			catch (EditiqueException e) {
				throw new RuntimeException(e);
			}
			assertEquals("Le Sentier", fichier.getFichierImpression().getDocumentArray(0).getInfoEnteteDocument().getExpediteur().getLocaliteExpedition());
				return null;
			}
		});


	}

	@Test
	public void testGetLocaliteExpeditionCasStandard() throws Exception {

// préparation fiscale
		final DeclarationImpotOrdinairePP di = doInTransaction(new TransactionCallback<DeclarationImpotOrdinairePP>() {
			@Override
			public DeclarationImpotOrdinairePP doInTransaction(TransactionStatus status) {


				final PersonnePhysique pp = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);

				addForPrincipal(pp, date(2009, 1, 1), MotifFor.DEPART_HS, null, null, MockCommune.Vevey);

				final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp, periodeFiscale, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modeleDocument);
				di.setNumeroOfsForGestion(MockCommune.Vevey.getNoOFS());
				EtatDeclaration etatEmise = new EtatDeclarationEmise();
				etatEmise.setDateObtention(date(2010, 1, 1));
				etatEmise.setDeclaration(di);
				di.setEtats(Collections.singleton(etatEmise));
				return di;
			}
		});

		doInTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final ImpressionSommationDIHelperParams params = ImpressionSommationDIHelperParams.createBatchParams(di, false, RegDate.get());
				FichierImpressionDocument fichier;
				try {
					fichier = impressionSommationDIHelper.remplitSommationDI(params);
				}
				catch (EditiqueException e) {
					throw new RuntimeException(e);
				}
				assertEquals("Vevey", fichier.getFichierImpression().getDocumentArray(0).getInfoEnteteDocument().getExpediteur().getLocaliteExpedition());
				return null;
			}
		});
	}

	@Test
	public void testImpressionCodeControle() throws Exception {

		final class Ids {
			long diAvec;
			long diSans;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PeriodeFiscale pfSans = addPeriodeFiscale(2011);
				pfSans.setShowCodeControleSommationDeclarationPP(false);
				final ModeleDocument mdSans = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pfSans);

				final PeriodeFiscale pfAvec = addPeriodeFiscale(2012);
				pfAvec.setShowCodeControleSommationDeclarationPP(true);
				final ModeleDocument mdAvec = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pfAvec);

				final PersonnePhysique pp = addNonHabitant("Gudule", "Tsichorée", date(1979, 8, 25), Sexe.FEMININ);
				addForPrincipal(pp, date(2011, 1, 1), MotifFor.ARRIVEE_HS, date(2012, 12, 31), MotifFor.DEPART_HS, MockCommune.Vevey);
				final DeclarationImpotOrdinairePP diSans = addDeclarationImpot(pp, pfSans, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, mdSans);
				diSans.setCodeControle("F31823");
				addEtatDeclarationEmise(diSans, date(2012, 1, 20));
				final DeclarationImpotOrdinairePP diAvec = addDeclarationImpot(pp, pfAvec, date(2012, 1, 1), date(2012, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, mdAvec);
				diAvec.setCodeControle("J75397");
				addEtatDeclarationEmise(diAvec, date(2013, 1, 18));

				final Ids ids = new Ids();
				ids.diAvec = diAvec.getId();
				ids.diSans = diSans.getId();
				return ids;
			}
		});

		// test de construction des données des sommations à envoyer à l'éditique
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// di 2011 -> sans code de contrôle sur la sommation
				{
					final DeclarationImpotOrdinairePP di = hibernateTemplate.get(DeclarationImpotOrdinairePP.class, ids.diSans);
					final ImpressionSommationDIHelperParams params = ImpressionSommationDIHelperParams.createBatchParams(di, false, RegDate.get());
					final FichierImpressionDocument doc = impressionSommationDIHelper.remplitSommationDI(params);
					assertNull(doc.getFichierImpression().getDocumentArray(0).getSommationDI().getLettreSom().getCodeValidation());
				}
				// di 2012 -> avec code de contrôle sur la sommation
				{
					final DeclarationImpotOrdinairePP di = hibernateTemplate.get(DeclarationImpotOrdinairePP.class, ids.diAvec);
					final ImpressionSommationDIHelperParams params = ImpressionSommationDIHelperParams.createBatchParams(di, false, RegDate.get());
					final FichierImpressionDocument doc = impressionSommationDIHelper.remplitSommationDI(params);
					assertEquals("J75397", doc.getFichierImpression().getDocumentArray(0).getSommationDI().getLettreSom().getCodeValidation());
				}
				return null;
			}
		});
	}

	@Test
	public void testSommationDiSurMenageAvecUnMembresDecede() throws Exception {
		final Long noIndLui = 1234567L;
		final Long noIndElle = 1234568L;
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu indElle = addIndividu(noIndElle, date(1942,1,1), "Maksimovic", "Radislavka", Sexe.MASCULIN);
				addAdresse(indElle, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, date(1982,3,1), null);

			}
		});


		final RegDate dateMariage = RegDate.get(2008, 1, 1);
		final RegDate dateVeuvage = RegDate.get(2014, 3,28 );
		class Ids {
			final long mcId;
			final long luiId;
			final long elleId;
			final long diId;
			Ids(long mcId, long diId,long lui, long elle) {
				this.mcId = mcId;
				this.diId = diId;
				luiId = lui;
				elleId = elle;
			}
		}

		// pas de validation : nécessaire pour créer le for sur un ménage commun sans appartenance ménage existante
		final Ids ids = doInNewTransactionAndSessionWithoutValidation(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				PersonnePhysique pLui =addNonHabitant("Slavko", " Maksimovic", date(1942, 2, 1), Sexe.MASCULIN);
				//pLui.setNumeroIndividu(noIndLui);
				PersonnePhysique pElle = addHabitant(noIndElle);
				final EnsembleTiersCouple etc = addEnsembleTiersCouple(pLui, pElle, dateMariage, dateVeuvage);
				final MenageCommun menage = etc.getMenage();
				addForPrincipal(menage, dateMariage, MotifFor.ARRIVEE_HS, dateVeuvage, MotifFor.VEUVAGE_DECES, MockCommune.Aubonne);

				final RegDate dateEmission = RegDate.get(2015, 1, 19);
				final RegDate dateDelaiInitial = RegDate.get(2015, 3, 20);
				final PeriodeFiscale periode = addPeriodeFiscale(2014);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode);
				final DeclarationImpotOrdinairePP declaration = addDeclarationImpot(menage, periode, date(2014, 1, 1), dateVeuvage, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				declaration.addEtat(new EtatDeclarationEmise(dateEmission));

				final DelaiDeclaration delai = new DelaiDeclaration();
				delai.setEtat(EtatDelaiDeclaration.ACCORDE);
				delai.setDateDemande(dateEmission);
				delai.setDelaiAccordeAu(dateDelaiInitial);
				declaration.addDelai(delai);

				return new Ids(menage.getId(), declaration.getId(),pLui.getNumero(),pElle.getNumero());
			}
		});

		// test de construction des données des sommations à envoyer à l'éditique
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				{
					final DeclarationImpotOrdinairePP di = hibernateTemplate.get(DeclarationImpotOrdinairePP.class, ids.diId);
					final ImpressionSommationDIHelperParams params = ImpressionSommationDIHelperParams.createBatchParams(di, false, RegDate.get());
					FichierImpressionDocument fichier;
					try {
						fichier = impressionSommationDIHelper.remplitSommationDI(params);
					}
					catch (EditiqueException e) {
						throw new RuntimeException(e);

					}
					//On ne doit avoir qu'une seule sommation et pas 2 sommations séparés			}
					assertEquals("On devrait avoir 1 document pour le survivant", 1, fichier.getFichierImpression().getDocumentArray().length);
					assertNull(fichier.getFichierImpression().getDocumentArray(0).getInfoDocument().getSepares());


				}

				return null;
			}
		});

	}
}
