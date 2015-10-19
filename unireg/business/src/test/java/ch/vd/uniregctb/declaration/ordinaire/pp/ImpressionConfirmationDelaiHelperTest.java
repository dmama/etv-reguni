package ch.vd.uniregctb.declaration.ordinaire.pp;

import java.util.GregorianCalendar;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.PeriodeFiscale;

public class ImpressionConfirmationDelaiHelperTest extends BusinessTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImpressionDeclarationImpotOrdinaireHelperTest.class);

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

		DeclarationImpotOrdinairePP declaration = new DeclarationImpotOrdinairePP();
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
