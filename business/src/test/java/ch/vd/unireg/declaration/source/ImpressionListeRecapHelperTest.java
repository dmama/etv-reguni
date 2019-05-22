package ch.vd.unireg.declaration.source;

import java.text.SimpleDateFormat;
import java.util.Date;

import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur;
import noNamespace.TypAdresse.Adresse;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.editique.LegacyEditiqueHelper;
import ch.vd.unireg.interfaces.infra.mock.DefaultMockInfrastructureConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.PeriodiciteDecompte;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ImpressionListeRecapHelperTest extends BusinessTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImpressionListeRecapHelperTest.class);

	private ImpressionListeRecapHelperImpl impressionLRHelper;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		final LegacyEditiqueHelper editiqueHelper = getBean(LegacyEditiqueHelper.class, "legacyEditiqueHelper");
		impressionLRHelper = new ImpressionListeRecapHelperImpl();
		impressionLRHelper.setLegacyEditiqueHelper(editiqueHelper);
		serviceInfra.setUp(new DefaultMockInfrastructureConnector());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRemplitExpediteur() throws Exception {
		LOGGER.debug("ImpressionListeRecapHelperTest - testRemplitExpediteur");
	     final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bussigny);

				final PeriodeFiscale pf = addPeriodeFiscale(2010);
				DeclarationImpotSource lr = addLR(dpi, date(2010, 1, 1), PeriodiciteDecompte.TRIMESTRIEL, pf);

		InfoEnteteDocument infoEnteteDocument = impressionLRHelper.remplitEnteteDocument(lr,null);
		Expediteur expediteur = infoEnteteDocument.getExpediteur();
		Adresse adresseExpediteur = expediteur.getAdresse();
		assertEquals("Administration cantonale des impôts", adresseExpediteur.getAdresseCourrierLigne1());
		assertEquals("Impôt à la source", adresseExpediteur.getAdresseCourrierLigne2());
		assertNull(adresseExpediteur.getAdresseCourrierLigne3());
		assertEquals("Route de Berne 46", adresseExpediteur.getAdresseCourrierLigne4());
		assertEquals("1014 Lausanne Adm cant", adresseExpediteur.getAdresseCourrierLigne5());
		assertNull( adresseExpediteur.getAdresseCourrierLigne6());
		final String numeroTelCAT = serviceInfra.getCAT().getNoTelephone();
		final String numeroExpediteur = expediteur.getNumTelephone();
		assertEquals(numeroTelCAT,numeroExpediteur);

		Date date = DateHelper.getCurrentDate();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		assertEquals(dateFormat.format(date), expediteur.getDateExpedition());
	}
}
