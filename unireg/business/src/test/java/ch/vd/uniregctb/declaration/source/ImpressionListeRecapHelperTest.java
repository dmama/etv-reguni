package ch.vd.uniregctb.declaration.source;

import java.text.SimpleDateFormat;
import java.util.Date;

import noNamespace.DIBase;
import noNamespace.DIDocument.DI;
import noNamespace.DIHCDocument;
import noNamespace.DIRetour;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Destinataire;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur;
import noNamespace.TypAdresse.Adresse;
import org.apache.log4j.Logger;
import org.junit.Test;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.declaration.ordinaire.ImpressionDeclarationImpotOrdinaireHelperImpl;
import ch.vd.uniregctb.editique.EditiqueHelper;
import ch.vd.uniregctb.interfaces.model.mock.MockCollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockOfficeImpot;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceInfrastructureService;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings({"JavaDoc"})
public class ImpressionListeRecapHelperTest extends BusinessTest {

	private static final Logger LOGGER = Logger.getLogger(ImpressionListeRecapHelperTest.class);


	private ImpressionListeRecapHelperImpl impressionLRHelper;
	private EditiqueHelper editiqueHelper;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		editiqueHelper =  getBean(EditiqueHelper.class, "editiqueHelper");
		impressionLRHelper = new ImpressionListeRecapHelperImpl();
		impressionLRHelper.setEditiqueHelper(editiqueHelper);
		serviceInfra.setUp(new DefaultMockServiceInfrastructureService());
	}

	@Test
	public void testRemplitExpediteur() throws Exception {
		LOGGER.debug("ImpressionListeRecapHelperTest - testRemplitExpediteur");
	     final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), null, MockCommune.Bussigny);

				final PeriodeFiscale pf = addPeriodeFiscale(2010);
				DeclarationImpotSource lr = addLR(dpi, date(2010, 1, 1), date(2010, 3, 31), pf);

		InfoEnteteDocument infoEnteteDocument = impressionLRHelper.remplitEnteteDocument(lr,null);
		Expediteur expediteur = infoEnteteDocument.getExpediteur();
		Adresse adresseExpediteur = expediteur.getAdresse();
		assertEquals("Administration cantonale des impôts", adresseExpediteur.getAdresseCourrierLigne1());
		assertEquals("Impôt à la source", adresseExpediteur.getAdresseCourrierLigne2());
		assertEquals(null, adresseExpediteur.getAdresseCourrierLigne3());
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
