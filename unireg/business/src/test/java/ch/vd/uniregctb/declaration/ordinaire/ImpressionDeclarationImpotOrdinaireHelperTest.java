package ch.vd.uniregctb.declaration.ordinaire;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.editique.EditiqueHelper;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockOfficeImpot;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.*;
import noNamespace.DIDocument.DI;
import noNamespace.DIRetour;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Destinataire;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur;
import noNamespace.TypAdresse.Adresse;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.*;


@SuppressWarnings({"JavaDoc"})
public class ImpressionDeclarationImpotOrdinaireHelperTest extends BusinessTest {

	private static final Logger LOGGER = Logger.getLogger(ImpressionDeclarationImpotOrdinaireHelperTest.class);

	private final static String DB_UNIT_DATA_FILE = "classpath:ch/vd/uniregctb/declaration/ordinaire/ImpressionDeclarationImpotOrdinaireHelperTest.xml";

	private DeclarationImpotOrdinaireDAO diDAO;
	private ImpressionDeclarationImpotOrdinaireHelperImpl impressionDIHelper;
	private AdresseService adresseService;
	private TiersService tiersService;
	private SituationFamilleService situationFamilleService;
	private EditiqueHelper editiqueHelper;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		diDAO = getBean(DeclarationImpotOrdinaireDAO.class, "diDAO");

		adresseService = getBean(AdresseService.class, "adresseService");
		tiersService = getBean(TiersService.class, "tiersService");
		situationFamilleService =  getBean(SituationFamilleService.class, "situationFamilleService");
		editiqueHelper =  getBean(EditiqueHelper.class, "editiqueHelper");
		serviceInfra.setUp(new DefaultMockServiceInfrastructureService());
		impressionDIHelper = new ImpressionDeclarationImpotOrdinaireHelperImpl(serviceInfra, adresseService, tiersService, situationFamilleService, editiqueHelper);
	}

	@Test
	public void testRemplitExpediteur() throws Exception {
		LOGGER.debug("EditiqueHelperTest - testRemplitExpediteur");
		loadDatabase(DB_UNIT_DATA_FILE);

		DeclarationImpotOrdinaire declaration = diDAO.get(Long.valueOf(2));
		InfoEnteteDocument infoEnteteDocument = impressionDIHelper.remplitEnteteDocument(declaration);
		Expediteur expediteur = infoEnteteDocument.getExpediteur();
		Adresse adresseExpediteur = expediteur.getAdresse();
		assertEquals("Office d'impôt du district", adresseExpediteur.getAdresseCourrierLigne1());
		assertEquals("de Morges", adresseExpediteur.getAdresseCourrierLigne2());
		assertEquals("rue de la Paix 1", adresseExpediteur.getAdresseCourrierLigne3());
		assertEquals("1110 Morges", adresseExpediteur.getAdresseCourrierLigne4());
		assertNull( adresseExpediteur.getAdresseCourrierLigne6());

		Date date = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		assertEquals(dateFormat.format(date), expediteur.getDateExpedition());

	}

	/**
	 * [UNIREG-1257] l'office d'impôt expéditeur doit être celui du for fiscal valide durant la période couverte par la déclaration.
	 */
	@Test
	public void testRemplitAncienneCommune() throws Exception {

		final CollectiviteAdministrative cedi = tiersService.getOrCreateCollectiviteAdministrative(ServiceInfrastructureService.noCEDI);
		final CollectiviteAdministrative orbe = tiersService.getOrCreateCollectiviteAdministrative(MockOfficeImpot.OID_ORBE.getNoColAdm());
		final CollectiviteAdministrative aigle = tiersService.getOrCreateCollectiviteAdministrative(MockOfficeImpot.OID_AIGLE.getNoColAdm());

		// Crée une personne physique (ctb ordinaire vaudois) qui a déménagé début 2008 de Vallorbe à Bex
		final PersonnePhysique pp = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
		addForPrincipal(pp, date(2003, 1, 1), MotifFor.MAJORITE, date(2007, 12, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Vallorbe);
		addForPrincipal(pp, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Bex);
		final String numCtb = String.format("%09d", pp.getNumero());

		final PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
		final ModeleDocument modele2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, periode2007);
		final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
		final ModeleDocument modele2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, periode2008);
		final DeclarationImpotOrdinaire declaration2007 = addDeclarationImpot(pp, periode2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2007);
		declaration2007.setNumeroOfsForGestion(MockCommune.Vallorbe.getNoOFSEtendu());
		declaration2007.setRetourCollectiviteAdministrative(cedi);
		final DeclarationImpotOrdinaire declaration2008 = addDeclarationImpot(pp, periode2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2008);
		declaration2008.setNumeroOfsForGestion(MockCommune.Bex.getNoOFSEtendu());
		declaration2008.setRetourCollectiviteAdministrative(cedi);

		// L'expéditeur de la déclaration 2007 doit être Orbe (= OID responsable de Vallorbe)
		{
			final String oidOrbe = String.format("%02d", orbe.getNumeroCollectiviteAdministrative());

			// ... sur l'entête
			final InfoEnteteDocument entete = impressionDIHelper.remplitEnteteDocument(declaration2007);
			assertNotNull(entete);
			final Expediteur expediteur = entete.getExpediteur();
			assertNotNull(expediteur);
			final Adresse adresse = expediteur.getAdresse();
			assertEquals("Office d'impôt du district", adresse.getAdresseCourrierLigne1());
			assertEquals("du Jura - Nord vaudois", adresse.getAdresseCourrierLigne2());
			assertEquals("Bureau d'Orbe", adresse.getAdresseCourrierLigne3());
			assertEquals("rue de la Poste 2", adresse.getAdresseCourrierLigne4());
			assertEquals("1350 Orbe", adresse.getAdresseCourrierLigne5());
			assertNull(adresse.getAdresseCourrierLigne6());

			// .. sur le code bar
			final DI di = impressionDIHelper.remplitSpecifiqueDI(declaration2007, null);
			assertNotNull(di);
			final DI.InfoDI info = di.getInfoDI();
			assertNotNull(info);
			assertEquals(numCtb + "200701" + oidOrbe, info.getCODBARR());
			assertEquals(oidOrbe + "-1", info.getNOOID());

			// ... sur l'adresse du CEDI
			final DIRetour.AdresseRetour retour = di.getAdresseRetour();
			assertNotNull(retour);
			assertEquals("Centre d'enregistrement", retour.getADRES1RETOUR());
			assertEquals("des déclarations d'impôt", retour.getADRES2RETOUR());
			assertEquals("CEDI " + orbe.getNumeroCollectiviteAdministrative(), retour.getADRES3RETOUR());
			assertEquals("1014 Lausanne Adm cant", retour.getADRES4RETOUR());
		}

		// L'expéditeur de la déclaration 2008 doit être Aigle (= OID responsable de Bex)
		{
			final String oidAigle = String.format("%02d", aigle.getNumeroCollectiviteAdministrative());

			// ... sur l'entête
			final InfoEnteteDocument entete = impressionDIHelper.remplitEnteteDocument(declaration2008);
			assertNotNull(entete);
			final Expediteur expediteur = entete.getExpediteur();
			assertNotNull(expediteur);
			final Adresse adresse = expediteur.getAdresse();
			assertEquals("Office d'impôt du district", adresse.getAdresseCourrierLigne1());
			assertEquals("d'Aigle", adresse.getAdresseCourrierLigne2());
			assertEquals("rue de la Gare 27", adresse.getAdresseCourrierLigne3());
			assertEquals("1860 Aigle", adresse.getAdresseCourrierLigne4());
			assertNull(adresse.getAdresseCourrierLigne5());
			assertNull(adresse.getAdresseCourrierLigne6());

			// .. sur le code bar
			final DI di = impressionDIHelper.remplitSpecifiqueDI(declaration2008, null);
			assertNotNull(di);
			final DI.InfoDI info = di.getInfoDI();
			assertNotNull(info);
			assertEquals(numCtb + "200801" + oidAigle, info.getCODBARR());
			assertEquals(oidAigle + "-1", info.getNOOID());

			// ... sur l'adresse du CEDI
			final DIRetour.AdresseRetour retour = di.getAdresseRetour();
			assertNotNull(retour);
			assertEquals("Centre d'enregistrement", retour.getADRES1RETOUR());
			assertEquals("des déclarations d'impôt", retour.getADRES2RETOUR());
			assertEquals("CEDI " + aigle.getNumeroCollectiviteAdministrative(), retour.getADRES3RETOUR());
			assertEquals("1014 Lausanne Adm cant", retour.getADRES4RETOUR());
		}
	}

	/**
	 * [UNIREG-1957] vérifie que la case postale ext bien renseignée dans le cas d'une déclaraiton dépense.
	 */
	@Test
	public void testAdresseRetourDIDepense() throws Exception {

		final CollectiviteAdministrative vevey = tiersService.getOrCreateCollectiviteAdministrative(MockOfficeImpot.OID_VEVEY.getNoColAdm());

		// Crée une personne physique (ctb ordinaire vaudois) à la dépense
		final PersonnePhysique pp = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
		final ForFiscalPrincipal ffp = addForPrincipal(pp, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Vevey);
		ffp.setModeImposition(ModeImposition.DEPENSE);

		final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
		final ModeleDocument modele2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE, periode2008);
		final DeclarationImpotOrdinaire declaration2008 = addDeclarationImpot(pp, periode2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_DEPENSE, modele2008);
		declaration2008.setNumeroOfsForGestion(MockCommune.Vevey.getNoOFSEtendu());
		declaration2008.setRetourCollectiviteAdministrative(vevey);

		final DI di = impressionDIHelper.remplitSpecifiqueDI(declaration2008, null);
		assertNotNull(di);

		final DIRetour.AdresseRetour retour = di.getAdresseRetour();
		assertNotNull(retour);
		assertEquals("Office d'impôt du district", retour.getADRES1RETOUR());
		assertEquals("de la Riviera - Pays-d'Enhaut", retour.getADRES2RETOUR());
		assertEquals("Rue du Simplon 22", retour.getADRES3RETOUR());
		assertEquals("Case postale 1032", retour.getADRES4RETOUR());
		assertEquals("1800 Vevey 1", retour.getADRES5RETOUR());
	}

	/**
	 * [UNIREG-1741] vérifie que l'adresse de retour d'une DI pour un contribuable décédé est bien CEDI - 22
	 */
	@Test
	public void testAdresseRetourDIDecede() throws Exception {

		final CollectiviteAdministrative aci = tiersService.getOrCreateCollectiviteAdministrative(MockOfficeImpot.ACI.getNoColAdm());

		// Crée une personne physique décédé
		final PersonnePhysique pp = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
		addForPrincipal(pp, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, date(2008, 4, 23), MotifFor.VEUVAGE_DECES, MockCommune.Vevey);

		final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
		final ModeleDocument modele2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE, periode2008);
		final DeclarationImpotOrdinaire declaration2008 = addDeclarationImpot(pp, periode2008, date(2008, 1, 1), date(2008, 4, 23), TypeContribuable.VAUDOIS_DEPENSE, modele2008);
		declaration2008.setNumeroOfsForGestion(MockCommune.Vevey.getNoOFSEtendu());
		declaration2008.setRetourCollectiviteAdministrative(aci);

		final DI di = impressionDIHelper.remplitSpecifiqueDI(declaration2008, null);
		assertNotNull(di);

		final DIRetour.AdresseRetour retour = di.getAdresseRetour();
		assertNotNull(retour);
		assertEquals("Centre d'enregistrement", retour.getADRES1RETOUR());
		assertEquals("des déclarations d'impôt", retour.getADRES2RETOUR());
		assertEquals("CEDI 22", retour.getADRES3RETOUR());
		assertEquals("1014 Lausanne Adm cant", retour.getADRES4RETOUR());
		assertNull(retour.getADRES5RETOUR());
		assertNull(retour.getADRES6RETOUR());
	}

	@Test
	public void testRemplitDestinataire() throws Exception {
		LOGGER.debug("EditiqueHelperTest - testRemplitDestinataire");
		loadDatabase(DB_UNIT_DATA_FILE);

		DeclarationImpotOrdinaire declaration = diDAO.get(Long.valueOf(2));
		InfoEnteteDocument infoEnteteDocument = impressionDIHelper.remplitEnteteDocument(declaration);
		Destinataire destinataire = infoEnteteDocument.getDestinataire();
		Adresse adresseDestinataire = destinataire.getAdresse();
		assertEquals("Monsieur et Madame", adresseDestinataire.getAdresseCourrierLigne1());
		assertEquals("Alain Dupont", adresseDestinataire.getAdresseCourrierLigne2());
		assertEquals("Maria Dupont", adresseDestinataire.getAdresseCourrierLigne3());
		assertEquals("Rue des terreaux 12", adresseDestinataire.getAdresseCourrierLigne4());
		assertEquals("1350 Orbe", adresseDestinataire.getAdresseCourrierLigne5());
		assertNull( adresseDestinataire.getAdresseCourrierLigne6());

	}

	/**
	 * Remplit un objet pour l'impression de la partie spécifique DI
	 * @throws Exception
	 */
	@Test
	public void testRempliQuelquesMachins() throws Exception {
		loadDatabase("ImpressionDeclarationImpotOrdinaireHelperTest2.xml");
		DeclarationImpotOrdinaire declaration = diDAO.get(Long.valueOf(2));
		DI di = impressionDIHelper.remplitSpecifiqueDI(declaration, null);
		DIRetour.AdresseRetour cediImpression = di.getAdresseRetour();
		assertEquals("Centre d'enregistrement", cediImpression.getADRES1RETOUR());
		assertEquals("des déclarations d'impôt", cediImpression.getADRES2RETOUR());
		assertEquals("CEDI 10", cediImpression.getADRES3RETOUR());
		assertEquals("1014 Lausanne Adm cant", cediImpression.getADRES4RETOUR());

		assertEquals("12.02.1977", di.getContrib1().getINDDATENAISS1());
		assertEquals("Marié(e)", di.getContrib1().getINDETATCIVIL1());
		assertEquals("Monsieur Alain Dupont", di.getContrib1().getINDNOMPRENOM1());
		//assertEquals("154.89.652.357", di.getContrib1().getNAVS13());

		assertEquals("18.12.1953", di.getContrib2().getINDDATENAISS2());
		assertEquals("Marié(e)", di.getContrib2().getINDETATCIVIL2());
		assertEquals("Madame Maria Dupont", di.getContrib2().getINDNOMPRENOM2());
		//assertEquals("514.89.652.375", di.getContrib2().getNAVS13());

		assertEquals("2005", di.getInfoDI().getANNEEFISCALE());
		assertEquals("01260000420050110", di.getInfoDI().getCODBARR());
		assertEquals("31.07.2006", di.getInfoDI().getDELAIRETOUR());
		assertEquals("Villars-sous-Yens", di.getInfoDI().getDESCOM());
		assertEquals("126.000.04", di.getInfoDI().getNOCANT());
		assertEquals("10-1", di.getInfoDI().getNOOID());

		assertEquals("1", di.getAnnexes().getAnnexe210());
		assertEquals("1", di.getAnnexes().getAnnexe220());
		assertEquals("1", di.getAnnexes().getAnnexe230());
		assertEquals("1", di.getAnnexes().getAnnexe240());
		assertNull(di.getAnnexes().getAnnexe310());
	}
}
