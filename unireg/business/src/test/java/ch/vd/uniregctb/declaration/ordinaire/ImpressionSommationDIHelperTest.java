package ch.vd.uniregctb.declaration.ordinaire;

import java.util.GregorianCalendar;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.DefaultMockServiceInfrastructureService;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueHelper;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;

public class ImpressionSommationDIHelperTest extends BusinessTest {

	private static final Logger LOGGER = Logger.getLogger(ImpressionDeclarationImpotOrdinaireHelperTest.class);

	private ImpressionSommationDIHelperImpl impressionSommationDIHelper;
	private AdresseService adresseService;
	private TiersService tiersService;
	private EditiqueHelper editiqueHelper;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		adresseService = getBean(AdresseService.class, "adresseService");
		tiersService = getBean(TiersService.class, "tiersService");
		editiqueHelper =  getBean(EditiqueHelper.class, "editiqueHelper");
		serviceInfra.setUp(new DefaultMockServiceInfrastructureService());
		impressionSommationDIHelper = new ImpressionSommationDIHelperImpl(serviceInfra, adresseService, tiersService,  editiqueHelper);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testConstruitIdArchivageDocument() throws Exception {
		LOGGER.debug("EditiqueHelperTest - testConstruitIdArchivageDocument");
		String idArchivageAttendu = "200802 Sommation DI        0101123020000";

		DeclarationImpotOrdinaire declaration = new DeclarationImpotOrdinaire();
		declaration.setNumero(Integer.valueOf(2));
		PeriodeFiscale periodeFiscale = new PeriodeFiscale();
		periodeFiscale.setAnnee(Integer.valueOf(2008));
		declaration.setPeriode(periodeFiscale);
		GregorianCalendar cal = new GregorianCalendar(2007, 0, 1, 12, 30, 20);
		declaration.setLogCreationDate(cal.getTime());

		Assert.assertEquals(idArchivageAttendu, impressionSommationDIHelper.construitIdArchivageDocument(declaration));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testConstruitAncienIdArchivageDocument() throws Exception {
		LOGGER.debug("EditiqueHelperTest - testConstruitAncienIdArchivageDocument");
		String idArchivageAttendu = "200802 Sommation DI         200701011230";

		DeclarationImpotOrdinaire declaration = new DeclarationImpotOrdinaire();
		declaration.setNumero(Integer.valueOf(2));
		PeriodeFiscale periodeFiscale = new PeriodeFiscale();
		periodeFiscale.setAnnee(Integer.valueOf(2008));
		declaration.setPeriode(periodeFiscale);
		GregorianCalendar cal = new GregorianCalendar(2007, 0, 1, 12, 30, 20);
		declaration.setLogCreationDate(cal.getTime());


		Assert.assertEquals(idArchivageAttendu, impressionSommationDIHelper.construitAncienIdArchivageDocument(declaration));

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testConstruitAncienIdArchivageDocumentPourOnLine() throws Exception {
		LOGGER.debug("EditiqueHelperTest - testConstruitAncienIdArchivageDocumentPourOnLine");
		String idArchivageAttendu = "200802 Sommation DI         20070101123020000";

		DeclarationImpotOrdinaire declaration = new DeclarationImpotOrdinaire();
		declaration.setNumero(Integer.valueOf(2));
		PeriodeFiscale periodeFiscale = new PeriodeFiscale();
		periodeFiscale.setAnnee(Integer.valueOf(2008));
		declaration.setPeriode(periodeFiscale);
		GregorianCalendar cal = new GregorianCalendar(2007, 0, 1, 12, 30, 20);
		declaration.setLogCreationDate(cal.getTime());

		Assert.assertEquals(idArchivageAttendu, impressionSommationDIHelper.construitAncienIdArchivageDocumentPourOnLine(declaration));
	}

	/**
	 * [SIFISC-5325] Le message d'erreur qui sort dans le rapport d'exécution du batch de sommation des DI doit
	 * indiquer clairement si l'individu n'a pas été trouvé dans le registre civil et que c'est ça qui pose problème
	 */
	@Test
	public void testExceptionRecueQuandIndividuNonPresentDansRegistreCivil() throws Exception {

		final long noIndividu = 213567254L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// persone !
			}
		});

		// préparation fiscale
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, date(2009, 1, 1), MotifFor.ARRIVEE_HS, date(2009, 12, 31), MotifFor.DEPART_HS, MockCommune.Lausanne);
				addCollAdm(MockOfficeImpot.OID_LAUSANNE_VILLE);
				addCedi();
				final PeriodeFiscale pf = addPeriodeFiscale(2009);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf);
				final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, pf, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, md);
				final ImpressionSommationDIHelperParams params = ImpressionSommationDIHelperParams.createBatchParams(di, false, RegDate.get());
				try {
					impressionSommationDIHelper.remplitSommationDI(params);
					Assert.fail("Devrait exploser car l'individu n'est pas dans le registre civil...");
				}
				catch (EditiqueException e) {
					final String expectedMessage = String.format("Exception lors de l'identification de la provenance de l'adresse (Impossible de trouver l'individu n°%d)", noIndividu);
					Assert.assertEquals(expectedMessage, e.getMessage());
				}
				return null;
			}
		});
	}

}
