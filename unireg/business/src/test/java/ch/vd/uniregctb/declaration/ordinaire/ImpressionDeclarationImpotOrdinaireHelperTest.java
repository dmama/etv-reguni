package ch.vd.uniregctb.declaration.ordinaire;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

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
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.editique.EditiqueHelper;
import ch.vd.uniregctb.interfaces.model.mock.MockCollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockOfficeImpot;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.utils.UniregModeHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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

		Date date = DateHelper.getCurrentDate();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		assertEquals(dateFormat.format(date), expediteur.getDateExpedition());

	}
	//UNIREG-2541 Adresse de retour pour les DI hors canton 

	@Test
	public void testAdresseRetourDIHorsCanton() throws Exception {
		LOGGER.debug("EditiqueHelperTest - testRemplitExpediteur UNIREG-2541");


		final CollectiviteAdministrative cedi = tiersService.getOrCreateCollectiviteAdministrative(ServiceInfrastructureService.noCEDI);
		final CollectiviteAdministrative morges = tiersService.getOrCreateCollectiviteAdministrative(MockOfficeImpot.OID_MORGES.getNoColAdm());
		final CollectiviteAdministrative aci = tiersService.getOrCreateCollectiviteAdministrative(ServiceInfrastructureService.noACI);


		// Crée une personne physique (ctb ordinaire vaudois) qui a déménagé mi 2010 de Morges à Paris
		final PersonnePhysique pp = addNonHabitant("Céline", "André", date(1980, 6, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(2006, 1, 1), MotifFor.ARRIVEE_HS, date(2010, 6, 30), MotifFor.DEPART_HS, MockCommune.Morges);

		final String numCtb = String.format("%09d", pp.getNumero());

		final PeriodeFiscale periode2010 = addPeriodeFiscale(2010);
		final ModeleDocument modele2010 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, periode2010);
		final DeclarationImpotOrdinaire declaration2010 = addDeclarationImpot(pp, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
		declaration2010.setNumeroOfsForGestion(MockCommune.Morges.getNoOFSEtendu());
		declaration2010.setRetourCollectiviteAdministrativeId(aci.getId());
		{

			final DIHCDocument.DIHC di = impressionDIHelper.remplitSpecifiqueDIHC(declaration2010, null);
			assertNotNull(di);

			// ... sur l'adresse du CEDI
			final DIHCDocument.DIHC.AdresseRetour retour = di.getAdresseRetour();
			assertNotNull(retour);
			assertEquals("Centre d'enregistrement", retour.getADRES1RETOUR());
			assertEquals("des déclarations d'impôt", retour.getADRES2RETOUR());
			assertEquals("CEDI " + aci.getNumeroCollectiviteAdministrative(), retour.getADRES3RETOUR());
			assertEquals("1014 Lausanne Adm cant", retour.getADRES4RETOUR());
		}


	}

	//UNIREG-3059 Adresse de retour pour les DI sur deux periode fiscales différentes avec chacune un for differents

	@Test
	public void testAdresseRetourDISur3Periodes() throws Exception {
		LOGGER.debug("EditiqueHelperTest - testRemplitExpediteur UNIREG-3059");


		final CollectiviteAdministrative cedi = tiersService.getOrCreateCollectiviteAdministrative(ServiceInfrastructureService.noCEDI);
		final CollectiviteAdministrative aigle = tiersService.getOrCreateCollectiviteAdministrative(MockOfficeImpot.OID_AIGLE.getNoColAdm());
		final CollectiviteAdministrative nyon = tiersService.getOrCreateCollectiviteAdministrative(MockOfficeImpot.OID_NYON.getNoColAdm());
		final CollectiviteAdministrative morges = tiersService.getOrCreateCollectiviteAdministrative(MockOfficeImpot.OID_MORGES.getNoColAdm());
		final CollectiviteAdministrative aci = tiersService.getOrCreateCollectiviteAdministrative(ServiceInfrastructureService.noACI);

		final String oidNyon = String.format("%02d", nyon.getNumeroCollectiviteAdministrative());
		final String oidMorges = String.format("%02d", morges.getNumeroCollectiviteAdministrative());
		final int anneeCourante = RegDate.get().year();
		// Crée une personne physique (ctb ordinaire vaudois) qui a déménagé mi 2010 de Morges à Paris
		final PersonnePhysique pp = addNonHabitant("Céline", "André", date(1980, 6, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(2008, 1, 1), MotifFor.ARRIVEE_HS, date(2008, 12, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Aigle);
		addForPrincipal(pp, date(2009, 1, 1), MotifFor.DEMENAGEMENT_VD, date(anneeCourante -1, 12, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Nyon);
		addForPrincipal(pp, date(anneeCourante, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Morges);

		final String numCtb = String.format("%09d", pp.getNumero());
		final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
		final PeriodeFiscale periode2009 = addPeriodeFiscale(2009);

		final PeriodeFiscale periodeCourante = addPeriodeFiscale(anneeCourante);
		final ModeleDocument modele2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, periode2008);
		final ModeleDocument modele2009 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, periode2009);
		final ModeleDocument modeleCourant = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, periodeCourante);
		final DeclarationImpotOrdinaire declaration2008 = addDeclarationImpot(pp, periode2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2008);
		final DeclarationImpotOrdinaire declaration2009 = addDeclarationImpot(pp, periode2009, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2009);
		final DeclarationImpotOrdinaire declarationCourante = addDeclarationImpot(pp, periodeCourante, date(anneeCourante, 1, 1), date(anneeCourante, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modeleCourant);

		declaration2008.setNumeroOfsForGestion(MockCommune.Aigle.getNoOFSEtendu());
		declaration2008.setRetourCollectiviteAdministrativeId(cedi.getId());
		declaration2009.setNumeroOfsForGestion(MockCommune.Nyon.getNoOFSEtendu());
		declaration2009.setRetourCollectiviteAdministrativeId(cedi.getId());
		declarationCourante.setNumeroOfsForGestion(MockCommune.Morges.getNoOFSEtendu());
		declarationCourante.setRetourCollectiviteAdministrativeId(cedi.getId());
		{

			final DI di2008 = impressionDIHelper.remplitSpecifiqueDI(declaration2008,null, false);
			assertNotNull(di2008);
			final DI di2009 = impressionDIHelper.remplitSpecifiqueDI(declaration2009,null, false);
			assertNotNull(di2009);
			final DI diCourante = impressionDIHelper.remplitSpecifiqueDI(declarationCourante,null, false);
			assertNotNull(diCourante);

			
			//Adresse expedition 2008
			InfoEnteteDocument infoEnteteDocument2008 = impressionDIHelper.remplitEnteteDocument(declaration2008);
			Expediteur expediteur2008 = infoEnteteDocument2008.getExpediteur();
			Adresse adresseExpediteur2008 = expediteur2008.getAdresse();
			assertEquals("Office d'impôt du district", adresseExpediteur2008.getAdresseCourrierLigne1());
			assertEquals("d'Aigle", adresseExpediteur2008.getAdresseCourrierLigne2());
			assertEquals("rue de la Gare 27", adresseExpediteur2008.getAdresseCourrierLigne3());
			assertEquals("1860 Aigle", adresseExpediteur2008.getAdresseCourrierLigne4());
			assertNull( adresseExpediteur2008.getAdresseCourrierLigne6());

			//Adresse expedition 2009
			InfoEnteteDocument infoEnteteDocument2009 = impressionDIHelper.remplitEnteteDocument(declaration2009);
			Expediteur expediteur2009 = infoEnteteDocument2009.getExpediteur();
			Adresse adresseExpediteur2009 = expediteur2009.getAdresse();
			assertEquals("Office d'impôt du district", adresseExpediteur2009.getAdresseCourrierLigne1());
			assertEquals("de Nyon", adresseExpediteur2009.getAdresseCourrierLigne2());
			assertEquals("Avenue Reverdil 4-6", adresseExpediteur2009.getAdresseCourrierLigne3());
			assertEquals("1341 Nyon", adresseExpediteur2009.getAdresseCourrierLigne4());
			assertNull( adresseExpediteur2009.getAdresseCourrierLigne6());
			
			//Adresse expedition annee courante
			InfoEnteteDocument infoEnteteDocumentCourant = impressionDIHelper.remplitEnteteDocument(declarationCourante);
			Expediteur expediteurCourant = infoEnteteDocumentCourant.getExpediteur();
			Adresse adresseExpediteurCourant = expediteurCourant.getAdresse();
			assertEquals("Office d'impôt du district", adresseExpediteurCourant.getAdresseCourrierLigne1());
			assertEquals("de Morges", adresseExpediteurCourant.getAdresseCourrierLigne2());
			assertEquals("rue de la Paix 1", adresseExpediteurCourant.getAdresseCourrierLigne3());
			assertEquals("1110 Morges", adresseExpediteurCourant.getAdresseCourrierLigne4());
			assertNull( adresseExpediteurCourant.getAdresseCourrierLigne6());
			



			// ... adresse retour pour 2008 2009
			final DI.AdresseRetour retour2008 = di2008.getAdresseRetour();
			assertNotNull(retour2008);
			assertEquals("Centre d'enregistrement", retour2008.getADRES1RETOUR());
			assertEquals("des déclarations d'impôt", retour2008.getADRES2RETOUR());
			assertEquals("CEDI " + nyon.getNumeroCollectiviteAdministrative(), retour2008.getADRES3RETOUR());
			assertEquals("1014 Lausanne Adm cant", retour2008.getADRES4RETOUR());

			final DI.InfoDI infoDI2008 = di2008.getInfoDI();
			assertEquals(oidNyon + "-1",infoDI2008.getNOOID());
			assertEquals(numCtb + "200801" + oidNyon,infoDI2008.getCODBARR());

			final DI.AdresseRetour retour2009 = di2009.getAdresseRetour();
			assertNotNull(retour2009);
			assertEquals("Centre d'enregistrement", retour2009.getADRES1RETOUR());
			assertEquals("des déclarations d'impôt", retour2009.getADRES2RETOUR());
			assertEquals("CEDI " + nyon.getNumeroCollectiviteAdministrative(), retour2009.getADRES3RETOUR());
			assertEquals("1014 Lausanne Adm cant", retour2009.getADRES4RETOUR());

			final DI.InfoDI infoDI2009 = di2009.getInfoDI();

			assertEquals(oidNyon + "-1",infoDI2009.getNOOID());
			assertEquals(numCtb + "200901" + oidNyon,infoDI2009.getCODBARR());

				// ... adresse retour pour periode courante DI libre
			final DI.AdresseRetour retourCourante = diCourante.getAdresseRetour();
			assertNotNull(retourCourante);
			assertEquals("Centre d'enregistrement", retourCourante.getADRES1RETOUR());
			assertEquals("des déclarations d'impôt", retourCourante.getADRES2RETOUR());
			assertEquals("CEDI " + morges.getNumeroCollectiviteAdministrative(), retourCourante.getADRES3RETOUR());
			assertEquals("1014 Lausanne Adm cant", retourCourante.getADRES4RETOUR());

			final DI.InfoDI infoDICourante = diCourante.getInfoDI();

			assertEquals(oidMorges + "-1",infoDICourante.getNOOID());
			assertEquals(numCtb +anneeCourante+"01" + oidMorges,infoDICourante.getCODBARR());

		}


	}
	
	 	//UNIREG-3059 Adresse de retour pour les DI sur deux periode fiscales différentes avec chacune un for differents

	@Test
	public void testAdresseRetourDI2008() throws Exception {
		LOGGER.debug("EditiqueHelperTest - testRemplitExpediteur UNIREG-3059");


		final CollectiviteAdministrative cedi = tiersService.getOrCreateCollectiviteAdministrative(ServiceInfrastructureService.noCEDI);
		final CollectiviteAdministrative aigle = tiersService.getOrCreateCollectiviteAdministrative(MockOfficeImpot.OID_AIGLE.getNoColAdm());
		final CollectiviteAdministrative nyon = tiersService.getOrCreateCollectiviteAdministrative(MockOfficeImpot.OID_NYON.getNoColAdm());
		final CollectiviteAdministrative morges = tiersService.getOrCreateCollectiviteAdministrative(MockOfficeImpot.OID_MORGES.getNoColAdm());
		final CollectiviteAdministrative aci = tiersService.getOrCreateCollectiviteAdministrative(ServiceInfrastructureService.noACI);

		final int anneeCourante = RegDate.get().year();
		// Crée une personne physique (ctb ordinaire vaudois) qui a déménagé mi 2010 de Morges à Paris
		final PersonnePhysique pp = addNonHabitant("Maelle", "André", date(1980, 6, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(2008, 6, 15), MotifFor.ARRIVEE_HS, date(2009, 6, 13), MotifFor.DEMENAGEMENT_VD, MockCommune.Aigle);
		addForPrincipal(pp, date(2009, 6, 14), MotifFor.DEMENAGEMENT_VD, null,null, MockCommune.Nyon);

		final String numCtb = String.format("%09d", pp.getNumero());
		final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
		final PeriodeFiscale periode2009 = addPeriodeFiscale(2009);

		final PeriodeFiscale periodeCourante = addPeriodeFiscale(anneeCourante);
		final ModeleDocument modele2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, periode2008);
		final ModeleDocument modele2009 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, periode2009);
		final ModeleDocument modeleCourant = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, periodeCourante);
		final DeclarationImpotOrdinaire declaration2008 = addDeclarationImpot(pp, periode2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2008);
		final DeclarationImpotOrdinaire declaration2009 = addDeclarationImpot(pp, periode2009, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2009);

		declaration2008.setNumeroOfsForGestion(MockCommune.Aigle.getNoOFSEtendu());
		declaration2008.setRetourCollectiviteAdministrativeId(cedi.getId());
		declaration2009.setNumeroOfsForGestion(MockCommune.Nyon.getNoOFSEtendu());
		declaration2009.setRetourCollectiviteAdministrativeId(cedi.getId());
		{

			final DI di2008 = impressionDIHelper.remplitSpecifiqueDI(declaration2008,null, false);
			assertNotNull(di2008);
			final DI di2009 = impressionDIHelper.remplitSpecifiqueDI(declaration2009,null, false);
			assertNotNull(di2009);


			//Adresse expedition 2008
			InfoEnteteDocument infoEnteteDocument2008 = impressionDIHelper.remplitEnteteDocument(declaration2008);
			Expediteur expediteur2008 = infoEnteteDocument2008.getExpediteur();
			Adresse adresseExpediteur2008 = expediteur2008.getAdresse();
			assertEquals("Office d'impôt du district", adresseExpediteur2008.getAdresseCourrierLigne1());
			assertEquals("d'Aigle", adresseExpediteur2008.getAdresseCourrierLigne2());
			assertEquals("rue de la Gare 27", adresseExpediteur2008.getAdresseCourrierLigne3());
			assertEquals("1860 Aigle", adresseExpediteur2008.getAdresseCourrierLigne4());
			assertNull( adresseExpediteur2008.getAdresseCourrierLigne6());

			//Adresse expedition 2009
			InfoEnteteDocument infoEnteteDocument2009 = impressionDIHelper.remplitEnteteDocument(declaration2009);
			Expediteur expediteur2009 = infoEnteteDocument2009.getExpediteur();
			Adresse adresseExpediteur2009 = expediteur2009.getAdresse();
			assertEquals("Office d'impôt du district", adresseExpediteur2009.getAdresseCourrierLigne1());
			assertEquals("de Nyon", adresseExpediteur2009.getAdresseCourrierLigne2());
			assertEquals("Avenue Reverdil 4-6", adresseExpediteur2009.getAdresseCourrierLigne3());
			assertEquals("1341 Nyon", adresseExpediteur2009.getAdresseCourrierLigne4());
			assertNull( adresseExpediteur2009.getAdresseCourrierLigne6());


			// ... adresse retour pour 2008 2009
			final DI.AdresseRetour retour2008 = di2008.getAdresseRetour();
			assertNotNull(retour2008);
			assertEquals("Centre d'enregistrement", retour2008.getADRES1RETOUR());
			assertEquals("des déclarations d'impôt", retour2008.getADRES2RETOUR());
			assertEquals("CEDI " + nyon.getNumeroCollectiviteAdministrative(), retour2008.getADRES3RETOUR());
			assertEquals("1014 Lausanne Adm cant", retour2008.getADRES4RETOUR());

			final DI.AdresseRetour retour2009 = di2009.getAdresseRetour();
			assertNotNull(retour2009);
			assertEquals("Centre d'enregistrement", retour2009.getADRES1RETOUR());
			assertEquals("des déclarations d'impôt", retour2009.getADRES2RETOUR());
			assertEquals("CEDI " + nyon.getNumeroCollectiviteAdministrative(), retour2009.getADRES3RETOUR());
			assertEquals("1014 Lausanne Adm cant", retour2009.getADRES4RETOUR());



		}


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
		declaration2007.setRetourCollectiviteAdministrativeId(cedi.getId());
		final DeclarationImpotOrdinaire declaration2008 = addDeclarationImpot(pp, periode2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2008);
		declaration2008.setNumeroOfsForGestion(MockCommune.Bex.getNoOFSEtendu());
		declaration2008.setRetourCollectiviteAdministrativeId(cedi.getId());

		// L'expéditeur de la déclaration 2007 doit être Aigle (= OID responsable de Bex)
		//Selon UNIREG-3059:
		//L'OID doit être l'OID de gestion valable au 31.12 de l'année N-1 (N étant la période lors de laquel l'édition du document a lieu)
		//-> SAUF une exception : si la DI concerne la période fiscale courante (il s'agit d'une DI libre),
		// alors l'OID doit être l'OID de gestion courant du moment de l'édition du docuement.
		{
			final String oidOrbe = String.format("%02d", orbe.getNumeroCollectiviteAdministrative());
			final String oidAigle = String.format("%02d", aigle.getNumeroCollectiviteAdministrative());

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
			final DI di = impressionDIHelper.remplitSpecifiqueDI(declaration2007, null, false);
			assertNotNull(di);
			final DI.InfoDI info = di.getInfoDI();
			assertNotNull(info);
			assertEquals(numCtb + "200701" + oidAigle, info.getCODBARR());
			assertEquals(oidAigle + "-1", info.getNOOID());

			// ... sur l'adresse du CEDI
			final DIRetour.AdresseRetour retour = di.getAdresseRetour();
			assertNotNull(retour);
			assertEquals("Centre d'enregistrement", retour.getADRES1RETOUR());
			assertEquals("des déclarations d'impôt", retour.getADRES2RETOUR());
			assertEquals("CEDI " + aigle.getNumeroCollectiviteAdministrative(), retour.getADRES3RETOUR());
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
			final DI di = impressionDIHelper.remplitSpecifiqueDI(declaration2008, null, false);
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

		addCollAdm(MockCollectiviteAdministrative.CEDI);
		final CollectiviteAdministrative vevey = addCollAdm(MockOfficeImpot.OID_VEVEY);

		// Crée une personne physique (ctb ordinaire vaudois) à la dépense
		final PersonnePhysique pp = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
		final ForFiscalPrincipal ffp = addForPrincipal(pp, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Vevey);
		ffp.setModeImposition(ModeImposition.DEPENSE);

		final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
		final ModeleDocument modele2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE, periode2008);
		final DeclarationImpotOrdinaire declaration2008 = addDeclarationImpot(pp, periode2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_DEPENSE, modele2008);
		declaration2008.setNumeroOfsForGestion(MockCommune.Vevey.getNoOFSEtendu());
		declaration2008.setRetourCollectiviteAdministrativeId(vevey.getId());

		final DI di = impressionDIHelper.remplitSpecifiqueDI(declaration2008, null, false);
		assertNotNull(di);

		final DIRetour.AdresseRetour retour = di.getAdresseRetour();
		assertNotNull(retour);
		assertEquals("Office d'impôt du district", retour.getADRES1RETOUR());
		assertEquals("de la Riviera - Pays-d'Enhaut", retour.getADRES2RETOUR());
		assertEquals("Rue du Simplon 22", retour.getADRES3RETOUR());
		assertEquals("Case Postale 1032", retour.getADRES4RETOUR());
		assertEquals("1800 Vevey 1", retour.getADRES5RETOUR());
	}

	/**
	 * [UNIREG-1741] vérifie que l'adresse de retour d'une DI pour un contribuable décédé est bien CEDI - 22
	 */
	@Test
	public void testAdresseRetourDIDecede() throws Exception {

		addCollAdm(MockCollectiviteAdministrative.CEDI);
		final CollectiviteAdministrative aci = addCollAdm(MockCollectiviteAdministrative.ACI);

		// Crée une personne physique décédé
		final PersonnePhysique pp = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
		addForPrincipal(pp, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, date(2008, 4, 23), MotifFor.VEUVAGE_DECES, MockCommune.Vevey);

		final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
		final ModeleDocument modele2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE, periode2008);
		final DeclarationImpotOrdinaire declaration2008 = addDeclarationImpot(pp, periode2008, date(2008, 1, 1), date(2008, 4, 23), TypeContribuable.VAUDOIS_DEPENSE, modele2008);
		declaration2008.setNumeroOfsForGestion(MockCommune.Vevey.getNoOFSEtendu());
		declaration2008.setRetourCollectiviteAdministrativeId(aci.getId());

		final DI di = impressionDIHelper.remplitSpecifiqueDI(declaration2008, null, false);
		assertNotNull(di);

		final DIBase.InfoDI infoDi = di.getInfoDI();
		assertNotNull(infoDi);
		assertEquals("22", infoDi.getNOOID()); // [UNIREG-1741] le numéro d'OID doit être renseignée en cas de retour au CEDI-22

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

	private static List<ModeleFeuilleDocumentEditique> buildDefaultAnnexes(DeclarationImpotOrdinaire di) {
		final List<ModeleFeuilleDocumentEditique> annexes = new ArrayList<ModeleFeuilleDocumentEditique>();
		final Set<ModeleFeuilleDocument> listFeuille = di.getModeleDocument().getModelesFeuilleDocument();
		for (ModeleFeuilleDocument feuille : listFeuille) {
			ModeleFeuilleDocumentEditique feuilleEditique = new ModeleFeuilleDocumentEditique();
			feuilleEditique.setIntituleFeuille(feuille.getIntituleFeuille());
			feuilleEditique.setNumeroFormulaire(feuille.getNumeroFormulaire());
			feuilleEditique.setNbreIntituleFeuille(1);
			annexes.add(feuilleEditique);
		}
		return annexes;
	}

	/**
	 * Remplit un objet pour l'impression de la partie spécifique DI
	 * @throws Exception
	 */
	@Test
	public void testRempliQuelquesMachins() throws Exception {
		loadDatabase("ImpressionDeclarationImpotOrdinaireHelperTest2.xml");
		DeclarationImpotOrdinaire declaration = diDAO.get(Long.valueOf(2));
		DI di = impressionDIHelper.remplitSpecifiqueDI(declaration, buildDefaultAnnexes(declaration), false);
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

		assertEquals(1, di.getAnnexes().getAnnexe210());
		assertEquals(1, di.getAnnexes().getAnnexe220());
		assertEquals(1, di.getAnnexes().getAnnexe230());
		assertEquals(1, di.getAnnexes().getAnnexe240());
		assertFalse(di.getAnnexes().isSetAnnexe310());
	}


	@Test
	public void testRemplitAnnexe320_Annexe_330() throws Exception {
		UniregModeHelper testMode = getBean(UniregModeHelper.class, "uniregModeHelper");
		testMode.setTestAnnexeDiMode("true");//sinon pas d'annexe 320 et 330
		loadDatabase("ImpressionDeclarationAnnexe_320_330.xml");
		DeclarationImpotOrdinaire declaration = diDAO.get(Long.valueOf(2));
		DI di = impressionDIHelper.remplitSpecifiqueDI(declaration, buildDefaultAnnexes(declaration), false);
		assertEquals(1, di.getAnnexes().getAnnexe320().getNombre());
		assertEquals("N", di.getAnnexes().getAnnexe320().getAvecCourrierExplicatif());
		assertEquals(1, di.getAnnexes().getAnnexe330());
		di = impressionDIHelper.remplitSpecifiqueDI(declaration, buildDefaultAnnexes(declaration), true);
	    assertEquals(1, di.getAnnexes().getAnnexe320().getNombre());
		assertEquals("O", di.getAnnexes().getAnnexe320().getAvecCourrierExplicatif());
	    assertEquals(1, di.getAnnexes().getAnnexe330());
		testMode.setTestAnnexeDiMode("false");
	}
}
