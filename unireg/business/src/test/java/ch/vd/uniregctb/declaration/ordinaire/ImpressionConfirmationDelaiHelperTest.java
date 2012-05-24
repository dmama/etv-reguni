package ch.vd.uniregctb.declaration.ordinaire;

import java.util.GregorianCalendar;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.PeriodeFiscale;

public class ImpressionConfirmationDelaiHelperTest extends BusinessTest {

	private static final Logger LOGGER = Logger.getLogger(ImpressionDeclarationImpotOrdinaireHelperTest.class);

	private ImpressionConfirmationDelaiHelperImpl impressionConfirmationDelaiHelper;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		impressionConfirmationDelaiHelper = getBean(ImpressionConfirmationDelaiHelperImpl.class, "impressionConfirmationDelaiHelper");
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testConstruitIdArchivageDocument() throws Exception {
		LOGGER.debug("ImpressionConfirmationDelaiHelperTest - testConstruitIdArchivageDocument");
		String idArchivageAttendu = "325483 Confirmation Delai  0101123020000";

		DeclarationImpotOrdinaire declaration = new DeclarationImpotOrdinaire();
		DelaiDeclaration delai = new DelaiDeclaration();
		delai.setDeclaration(declaration);
		delai.setDelaiAccordeAu(date(2011, 9, 21));
		delai.setId(84512325483L);
		declaration.setNumero(Integer.valueOf(2));
		PeriodeFiscale periodeFiscale = new PeriodeFiscale();
		periodeFiscale.setAnnee(Integer.valueOf(2010));
		declaration.setPeriode(periodeFiscale);
		GregorianCalendar cal = new GregorianCalendar(2011, 0, 1, 12, 30, 20);
		delai.setLogCreationDate(cal.getTime());
		ImpressionConfirmationDelaiHelperParams params = new ImpressionConfirmationDelaiHelperParams(declaration, delai.getDelaiAccordeAu(),
				"userTest", "", "", delai.getId(), delai.getLogCreationDate());

		Assert.assertEquals(idArchivageAttendu, impressionConfirmationDelaiHelper.construitIdArchivageDocument(params));

	}


}
