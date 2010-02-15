package ch.vd.uniregctb.declaration.ordinaire;

import java.util.GregorianCalendar;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.editique.EditiqueHelper;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;
import ch.vd.uniregctb.tiers.TiersService;

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

}
