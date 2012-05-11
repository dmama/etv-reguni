package ch.vd.uniregctb.declaration.source;

import java.text.SimpleDateFormat;
import java.util.Date;

import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.DefaultMockServiceInfrastructureService;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.EtatDeclarationSommee;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.editique.EditiqueHelper;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

import static org.junit.Assert.assertEquals;

@SuppressWarnings({"JavaDoc"})
public class ImpressionSommationLRHelperTest extends BusinessTest {

	private static final Logger LOGGER = Logger.getLogger(ImpressionSommationLRHelperTest.class);


	private ImpressionSommationLRHelperImpl impressionSommationLRHelper;
	private EditiqueHelper editiqueHelper;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		editiqueHelper =  getBean(EditiqueHelper.class, "editiqueHelper");
		impressionSommationLRHelper = new ImpressionSommationLRHelperImpl();
		impressionSommationLRHelper.setEditiqueHelper(editiqueHelper);
		serviceInfra.setUp(new DefaultMockServiceInfrastructureService());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDateEnvoiCourrierForSommation() throws Exception {
		LOGGER.debug("ImpressionListeRecapHelperTest - testDateEnvoiCourrierForSommation");
	     final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), null, MockCommune.Bussigny);

				final PeriodeFiscale pf = addPeriodeFiscale(2010);
				DeclarationImpotSource lr = addLR(dpi, date(2010, 7, 1), date(2010, 9, 30), pf, TypeEtatDeclaration.EMISE);
		final RegDate dateObtention = date(2011, 1, 17);
		final RegDate dateEnvoiCourrier = date(2011, 1, 20);
		EtatDeclarationSommee sommee = new EtatDeclarationSommee(dateObtention, dateEnvoiCourrier);
		        lr.addEtat(sommee);

		InfoEnteteDocument infoEnteteDocument = impressionSommationLRHelper.remplitEnteteDocument(lr);
		Expediteur expediteur = infoEnteteDocument.getExpediteur();
		final String numeroTelCAT = serviceInfra.getCAT().getNoTelephone();
		final String numeroExpediteur = expediteur.getNumTelephone();
		assertEquals(numeroTelCAT,numeroExpediteur);

		final String numeroFaxACISource = serviceInfra.getACIImpotSource().getNoFax();
		final String numeroFaxExpediteur = expediteur.getNumFax();
		assertEquals(numeroFaxACISource,numeroFaxExpediteur);

		Date date = dateEnvoiCourrier.asJavaDate();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		assertEquals(dateFormat.format(date), expediteur.getDateExpedition());

	}




}
