package ch.vd.uniregctb.declaration.source;

import java.text.SimpleDateFormat;
import java.util.Date;

import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.DefaultMockServiceInfrastructureService;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.EtatDeclarationSommee;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.editique.LegacyEditiqueHelper;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

import static org.junit.Assert.assertEquals;

@SuppressWarnings({"JavaDoc"})
public class ImpressionSommationLRHelperTest extends BusinessTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImpressionSommationLRHelperTest.class);


	private ImpressionSommationLRHelperImpl impressionSommationLRHelper;
	private LegacyEditiqueHelper editiqueHelper;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		editiqueHelper =  getBean(LegacyEditiqueHelper.class, "legacyEditiqueHelper");
		impressionSommationLRHelper = new ImpressionSommationLRHelperImpl();
		impressionSommationLRHelper.setLegacyEditiqueHelper(editiqueHelper);
		serviceInfra.setUp(new DefaultMockServiceInfrastructureService());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDateEnvoiCourrierForSommation() throws Exception {
		LOGGER.debug("ImpressionListeRecapHelperTest - testDateEnvoiCourrierForSommation");
	    final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
		addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bussigny);

		final PeriodeFiscale pf = addPeriodeFiscale(2010);
		final DeclarationImpotSource lr = addLR(dpi, date(2010, 7, 1), PeriodiciteDecompte.TRIMESTRIEL, pf, TypeEtatDeclaration.EMISE);
		final RegDate dateObtention = date(2011, 1, 17);
		final RegDate dateEnvoiCourrier = date(2011, 1, 20);
		final EtatDeclarationSommee sommee = new EtatDeclarationSommee(dateObtention, dateEnvoiCourrier);
		lr.addEtat(sommee);

		final InfoEnteteDocument infoEnteteDocument = impressionSommationLRHelper.remplitEnteteDocument(lr);
		final Expediteur expediteur = infoEnteteDocument.getExpediteur();
		final String numeroTelCAT = serviceInfra.getCAT().getNoTelephone();
		final String numeroExpediteur = expediteur.getNumTelephone();
		assertEquals(numeroTelCAT,numeroExpediteur);

		final String numeroFaxACISource = serviceInfra.getACIImpotSource().getNoFax();
		final String numeroFaxExpediteur = expediteur.getNumFax();
		assertEquals(numeroFaxACISource,numeroFaxExpediteur);

		final Date date = dateEnvoiCourrier.asJavaDate();
		final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		assertEquals(dateFormat.format(date), expediteur.getDateExpedition());
	}
}
