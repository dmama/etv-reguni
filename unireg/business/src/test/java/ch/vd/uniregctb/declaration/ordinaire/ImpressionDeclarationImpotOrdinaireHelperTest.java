package ch.vd.uniregctb.declaration.ordinaire;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import noNamespace.TypFichierImpression;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.DefaultMockServiceInfrastructureService;
import ch.vd.unireg.interfaces.infra.mock.MockBatiment;
import ch.vd.unireg.interfaces.infra.mock.MockCollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.InformationsDocumentAdapter;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.editique.EditiqueHelper;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;

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
		situationFamilleService = getBean(SituationFamilleService.class, "situationFamilleService");
		editiqueHelper = getBean(EditiqueHelper.class, "editiqueHelper");
		serviceInfra.setUp(new DefaultMockServiceInfrastructureService());
		impressionDIHelper = new ImpressionDeclarationImpotOrdinaireHelperImpl(serviceInfra, adresseService, tiersService, situationFamilleService, editiqueHelper);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRemplitExpediteur() throws Exception {
		LOGGER.debug("EditiqueHelperTest - testRemplitExpediteur");
		loadDatabase(DB_UNIT_DATA_FILE);

		DeclarationImpotOrdinaire declaration = diDAO.get(Long.valueOf(2));
		InfoEnteteDocument infoEnteteDocument = impressionDIHelper.remplitEnteteDocument(new InformationsDocumentAdapter(declaration));
		Expediteur expediteur = infoEnteteDocument.getExpediteur();
		Adresse adresseExpediteur = expediteur.getAdresse();
		assertEquals("Office d'impôt du district", adresseExpediteur.getAdresseCourrierLigne1());
		assertEquals("de Morges", adresseExpediteur.getAdresseCourrierLigne2());
		assertEquals("rue de la Paix 1", adresseExpediteur.getAdresseCourrierLigne3());
		assertEquals("1110 Morges", adresseExpediteur.getAdresseCourrierLigne4());
		assertNull(adresseExpediteur.getAdresseCourrierLigne6());

		Date date = DateHelper.getCurrentDate();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		assertEquals(dateFormat.format(date), expediteur.getDateExpedition());

	}
	//UNIREG-2541 Adresse de retour pour les DI hors canton 

	@Test
	@Transactional(rollbackFor = Throwable.class)
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

			final DIHCDocument.DIHC di = impressionDIHelper.remplitSpecifiqueDIHC(new InformationsDocumentAdapter(declaration2010), null);
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
	@Transactional(rollbackFor = Throwable.class)
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
		addForPrincipal(pp, date(2009, 1, 1), MotifFor.DEMENAGEMENT_VD, date(anneeCourante - 1, 12, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Nyon);
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
		final DeclarationImpotOrdinaire declarationCourante =
				addDeclarationImpot(pp, periodeCourante, date(anneeCourante, 1, 1), date(anneeCourante, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modeleCourant);

		declaration2008.setNumeroOfsForGestion(MockCommune.Aigle.getNoOFSEtendu());
		declaration2008.setRetourCollectiviteAdministrativeId(cedi.getId());
		declaration2009.setNumeroOfsForGestion(MockCommune.Nyon.getNoOFSEtendu());
		declaration2009.setRetourCollectiviteAdministrativeId(cedi.getId());
		declarationCourante.setNumeroOfsForGestion(MockCommune.Morges.getNoOFSEtendu());
		declarationCourante.setRetourCollectiviteAdministrativeId(cedi.getId());
		{

			final DI di2008 = impressionDIHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration2008), null, false);
			assertNotNull(di2008);
			final DI di2009 = impressionDIHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration2009), null, false);
			assertNotNull(di2009);
			final DI diCourante = impressionDIHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declarationCourante), null, false);
			assertNotNull(diCourante);


			//Adresse expedition 2008
			InfoEnteteDocument infoEnteteDocument2008 = impressionDIHelper.remplitEnteteDocument(new InformationsDocumentAdapter(declaration2008));
			Expediteur expediteur2008 = infoEnteteDocument2008.getExpediteur();
			Adresse adresseExpediteur2008 = expediteur2008.getAdresse();
			assertEquals("Office d'impôt du district", adresseExpediteur2008.getAdresseCourrierLigne1());
			assertEquals("d'Aigle", adresseExpediteur2008.getAdresseCourrierLigne2());
			assertEquals("rue de la Gare 27", adresseExpediteur2008.getAdresseCourrierLigne3());
			assertEquals("1860 Aigle", adresseExpediteur2008.getAdresseCourrierLigne4());
			assertNull(adresseExpediteur2008.getAdresseCourrierLigne6());

			//Adresse expedition 2009
			InfoEnteteDocument infoEnteteDocument2009 = impressionDIHelper.remplitEnteteDocument(new InformationsDocumentAdapter(declaration2009));
			Expediteur expediteur2009 = infoEnteteDocument2009.getExpediteur();
			Adresse adresseExpediteur2009 = expediteur2009.getAdresse();
			assertEquals("Office d'impôt du district", adresseExpediteur2009.getAdresseCourrierLigne1());
			assertEquals("de Nyon", adresseExpediteur2009.getAdresseCourrierLigne2());
			assertEquals("Avenue Reverdil 4-6", adresseExpediteur2009.getAdresseCourrierLigne3());
			assertEquals("1341 Nyon", adresseExpediteur2009.getAdresseCourrierLigne4());
			assertNull(adresseExpediteur2009.getAdresseCourrierLigne6());

			//Adresse expedition annee courante
			InfoEnteteDocument infoEnteteDocumentCourant = impressionDIHelper.remplitEnteteDocument(new InformationsDocumentAdapter(declarationCourante));
			Expediteur expediteurCourant = infoEnteteDocumentCourant.getExpediteur();
			Adresse adresseExpediteurCourant = expediteurCourant.getAdresse();
			assertEquals("Office d'impôt du district", adresseExpediteurCourant.getAdresseCourrierLigne1());
			assertEquals("de Morges", adresseExpediteurCourant.getAdresseCourrierLigne2());
			assertEquals("rue de la Paix 1", adresseExpediteurCourant.getAdresseCourrierLigne3());
			assertEquals("1110 Morges", adresseExpediteurCourant.getAdresseCourrierLigne4());
			assertNull(adresseExpediteurCourant.getAdresseCourrierLigne6());


			// ... adresse retour pour 2008 2009
			final DI.AdresseRetour retour2008 = di2008.getAdresseRetour();
			assertNotNull(retour2008);
			assertEquals("Centre d'enregistrement", retour2008.getADRES1RETOUR());
			assertEquals("des déclarations d'impôt", retour2008.getADRES2RETOUR());
			assertEquals("CEDI " + nyon.getNumeroCollectiviteAdministrative(), retour2008.getADRES3RETOUR());
			assertEquals("1014 Lausanne Adm cant", retour2008.getADRES4RETOUR());

			final DI.InfoDI infoDI2008 = di2008.getInfoDI();
			assertEquals(oidNyon + "-1", infoDI2008.getNOOID());
			assertEquals(numCtb + "200801" + oidNyon, infoDI2008.getCODBARR());

			final DI.AdresseRetour retour2009 = di2009.getAdresseRetour();
			assertNotNull(retour2009);
			assertEquals("Centre d'enregistrement", retour2009.getADRES1RETOUR());
			assertEquals("des déclarations d'impôt", retour2009.getADRES2RETOUR());
			assertEquals("CEDI " + nyon.getNumeroCollectiviteAdministrative(), retour2009.getADRES3RETOUR());
			assertEquals("1014 Lausanne Adm cant", retour2009.getADRES4RETOUR());

			final DI.InfoDI infoDI2009 = di2009.getInfoDI();

			assertEquals(oidNyon + "-1", infoDI2009.getNOOID());
			assertEquals(numCtb + "200901" + oidNyon, infoDI2009.getCODBARR());

			// ... adresse retour pour periode courante DI libre
			final DI.AdresseRetour retourCourante = diCourante.getAdresseRetour();
			assertNotNull(retourCourante);
			assertEquals("Centre d'enregistrement", retourCourante.getADRES1RETOUR());
			assertEquals("des déclarations d'impôt", retourCourante.getADRES2RETOUR());
			assertEquals("CEDI " + morges.getNumeroCollectiviteAdministrative(), retourCourante.getADRES3RETOUR());
			assertEquals("1014 Lausanne Adm cant", retourCourante.getADRES4RETOUR());

			final DI.InfoDI infoDICourante = diCourante.getInfoDI();

			// à partir de 2011, le défaut pour le suffixe du code routage est 0, et plus 1 comme avant
			assertEquals(oidMorges + "-0", infoDICourante.getNOOID());
			assertEquals(numCtb + anneeCourante + "01" + oidMorges, infoDICourante.getCODBARR());

		}


	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testInfoCommune() throws Exception {
		LOGGER.debug("EditiqueHelperTest - testInfoCommune SIFISC-1389");


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
		addForPrincipal(pp, date(2009, 1, 1), MotifFor.DEMENAGEMENT_VD, date(anneeCourante - 1, 12, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Nyon);
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
		final DeclarationImpotOrdinaire declarationCourante =
				addDeclarationImpot(pp, periodeCourante, date(anneeCourante, 1, 1), date(anneeCourante, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modeleCourant);

		declaration2008.setNumeroOfsForGestion(MockCommune.Aigle.getNoOFSEtendu());
		declaration2008.setRetourCollectiviteAdministrativeId(cedi.getId());
		declaration2009.setNumeroOfsForGestion(MockCommune.Nyon.getNoOFSEtendu());
		declaration2009.setRetourCollectiviteAdministrativeId(cedi.getId());
		declarationCourante.setNumeroOfsForGestion(MockCommune.Morges.getNoOFSEtendu());
		declarationCourante.setRetourCollectiviteAdministrativeId(cedi.getId());
		{

			final DI di2008 = impressionDIHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration2008), null, false);
			assertNotNull(di2008);
			final DI di2009 = impressionDIHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration2009), null, false);
			assertNotNull(di2009);
			final DI diCourante = impressionDIHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declarationCourante), null, false);
			assertNotNull(diCourante);

			DIBase.InfoDI info2008 = di2008.getInfoDI();
			assertNotNull(info2008.getDESCOM());
			assertEquals("Aigle", info2008.getDESCOM());
			DIBase.InfoDI info2009 = di2009.getInfoDI();
			assertNotNull(info2009.getDESCOM());
			assertEquals("Nyon", info2009.getDESCOM());
			DIBase.InfoDI infoCourante = diCourante.getInfoDI();
			assertNull(infoCourante.getDESCOM());

		}


	}


	//UNIREG-3059 Adresse de retour pour les DI sur deux periode fiscales différentes avec chacune un for differents

	@Test
	@Transactional(rollbackFor = Throwable.class)
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
		addForPrincipal(pp, date(2009, 6, 14), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Nyon);

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

			final DI di2008 = impressionDIHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration2008), null, false);
			assertNotNull(di2008);
			final DI di2009 = impressionDIHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration2009), null, false);
			assertNotNull(di2009);


			//Adresse expedition 2008
			InfoEnteteDocument infoEnteteDocument2008 = impressionDIHelper.remplitEnteteDocument(new InformationsDocumentAdapter(declaration2008));
			Expediteur expediteur2008 = infoEnteteDocument2008.getExpediteur();
			Adresse adresseExpediteur2008 = expediteur2008.getAdresse();
			assertEquals("Office d'impôt du district", adresseExpediteur2008.getAdresseCourrierLigne1());
			assertEquals("d'Aigle", adresseExpediteur2008.getAdresseCourrierLigne2());
			assertEquals("rue de la Gare 27", adresseExpediteur2008.getAdresseCourrierLigne3());
			assertEquals("1860 Aigle", adresseExpediteur2008.getAdresseCourrierLigne4());
			assertNull(adresseExpediteur2008.getAdresseCourrierLigne6());

			//Adresse expedition 2009
			InfoEnteteDocument infoEnteteDocument2009 = impressionDIHelper.remplitEnteteDocument(new InformationsDocumentAdapter(declaration2009));
			Expediteur expediteur2009 = infoEnteteDocument2009.getExpediteur();
			Adresse adresseExpediteur2009 = expediteur2009.getAdresse();
			assertEquals("Office d'impôt du district", adresseExpediteur2009.getAdresseCourrierLigne1());
			assertEquals("de Nyon", adresseExpediteur2009.getAdresseCourrierLigne2());
			assertEquals("Avenue Reverdil 4-6", adresseExpediteur2009.getAdresseCourrierLigne3());
			assertEquals("1341 Nyon", adresseExpediteur2009.getAdresseCourrierLigne4());
			assertNull(adresseExpediteur2009.getAdresseCourrierLigne6());


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

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFormuleAppel() throws Exception {
		LOGGER.debug("EditiqueHelperTest - testFormuleAppel SIFISC-1989");


		final CollectiviteAdministrative cedi = tiersService.getOrCreateCollectiviteAdministrative(ServiceInfrastructureService.noCEDI);
		final CollectiviteAdministrative nyon = tiersService.getOrCreateCollectiviteAdministrative(MockOfficeImpot.OID_NYON.getNoColAdm());

		final int anneeCourante = RegDate.get().year();
		// Crée une personne physique (ctb ordinaire vaudois) qui a déménagé mi 2010 de Morges à Paris
		final PersonnePhysique pp = addNonHabitant("Maelle", "André", date(1980, 6, 23), Sexe.MASCULIN);
		addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2009, 6, 14), null, MockRue.Aubonne.CheminCurzilles);
		addForPrincipal(pp, date(2008, 6, 15), MotifFor.ARRIVEE_HS, date(2009, 6, 13), MotifFor.DEMENAGEMENT_VD, MockCommune.Aigle);
		addForPrincipal(pp, date(2009, 6, 14), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Nyon);

		final PeriodeFiscale periode2009 = addPeriodeFiscale(2009);

		final ModeleDocument modele2009 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, periode2009);
		final DeclarationImpotOrdinaire declaration2009 = addDeclarationImpot(pp, periode2009, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2009);

		declaration2009.setNumeroOfsForGestion(MockCommune.Nyon.getNoOFSEtendu());
		declaration2009.setRetourCollectiviteAdministrativeId(cedi.getId());
		{

			final DI di2009 = impressionDIHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration2009), null, false);
			assertNotNull(di2009);

			final String formuleAppel = di2009.getFormuleAppel();
			assertEquals("Monsieur", formuleAppel);


		}


	}

	/**
	 * [UNIREG-1257] l'office d'impôt expéditeur doit être celui du for fiscal valide durant la période couverte par la déclaration.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
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
			final InfoEnteteDocument entete = impressionDIHelper.remplitEnteteDocument(new InformationsDocumentAdapter(declaration2007));
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
			final DI di = impressionDIHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration2007), null, false);
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
			final InfoEnteteDocument entete = impressionDIHelper.remplitEnteteDocument(new InformationsDocumentAdapter(declaration2008));
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
			final DI di = impressionDIHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration2008), null, false);
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
	@Transactional(rollbackFor = Throwable.class)
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

		final DI di = impressionDIHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration2008), null, false);
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
	@Transactional(rollbackFor = Throwable.class)
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

		final DI di = impressionDIHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration2008), null, false);
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
	@Transactional(rollbackFor = Throwable.class)
	public void testRemplitDestinataire() throws Exception {
		LOGGER.debug("EditiqueHelperTest - testRemplitDestinataire");
		loadDatabase(DB_UNIT_DATA_FILE);

		DeclarationImpotOrdinaire declaration = diDAO.get(Long.valueOf(2));
		InfoEnteteDocument infoEnteteDocument = impressionDIHelper.remplitEnteteDocument(new InformationsDocumentAdapter(declaration));
		Destinataire destinataire = infoEnteteDocument.getDestinataire();
		Adresse adresseDestinataire = destinataire.getAdresse();
		assertEquals("Monsieur et Madame", adresseDestinataire.getAdresseCourrierLigne1());
		assertEquals("Alain Dupont", adresseDestinataire.getAdresseCourrierLigne2());
		assertEquals("Maria Dupont", adresseDestinataire.getAdresseCourrierLigne3());
		assertEquals("Rue des terreaux 12", adresseDestinataire.getAdresseCourrierLigne4());
		assertEquals("1350 Orbe", adresseDestinataire.getAdresseCourrierLigne5());
		assertNull(adresseDestinataire.getAdresseCourrierLigne6());

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
	 *
	 * @throws Exception
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRempliQuelquesMachins() throws Exception {
		loadDatabase("ImpressionDeclarationImpotOrdinaireHelperTest2.xml");
		DeclarationImpotOrdinaire declaration = diDAO.get(Long.valueOf(2));
		DI di = impressionDIHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration), buildDefaultAnnexes(declaration), false);
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
	@Transactional(rollbackFor = Throwable.class)
	public void testRemplitAnnexe320_Annexe_330() throws Exception {
		loadDatabase("ImpressionDeclarationAnnexe_320_330.xml");
		DeclarationImpotOrdinaire declaration = diDAO.get(Long.valueOf(2));
		DI di = impressionDIHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration), buildDefaultAnnexes(declaration), false);
		assertEquals(1, di.getAnnexes().getAnnexe320().getNombre());
		assertEquals("N", di.getAnnexes().getAnnexe320().getAvecCourrierExplicatif());
		assertEquals(1, di.getAnnexes().getAnnexe330());
		final InformationsDocumentAdapter informationsDocument = new InformationsDocumentAdapter(declaration);
		di = impressionDIHelper.remplitSpecifiqueDI(informationsDocument, buildDefaultAnnexes(declaration), true);
		assertEquals(1, di.getAnnexes().getAnnexe320().getNombre());
		assertEquals("O", di.getAnnexes().getAnnexe320().getAvecCourrierExplicatif());
		assertEquals(1, di.getAnnexes().getAnnexe330());
	}

	/**[SIFISC-2367] Contribuables sans enfants
	 *
	 * @throws Exception
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCtbSansEnfant() throws Exception {

		addCollAdm(MockCollectiviteAdministrative.CEDI);
		final CollectiviteAdministrative aci = addCollAdm(MockCollectiviteAdministrative.ACI);

		// Crée une personne physique décédé
		final PersonnePhysique pp = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
		addForPrincipal(pp, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Vevey);

		final PeriodeFiscale periode2011 = addPeriodeFiscale(2011);
		final ModeleDocument modele2011 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE, periode2011);
		final DeclarationImpotOrdinaire declaration2011 = addDeclarationImpot(pp, periode2011, date(2011, 1, 1), date(2011, 4, 23), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
		declaration2011.setNumeroOfsForGestion(MockCommune.Vevey.getNoOFSEtendu());
		declaration2011.setRetourCollectiviteAdministrativeId(aci.getId());

		final DI di = impressionDIHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration2011), null, false);
		assertNotNull(di);
		//Aucune structure enfants ne devrait apparaitre pour les ctb sans enfants
		assertNull(di.getEnfants());

	}

	/**[SIFISC-2367] Contribuables sans enfants
	 *
	 * @throws Exception
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCtbAvecEnfant() throws Exception {
		final long indPere = 2;
		final long indFils = 3;
		final long indFille = 4;

		// On crée la situation de départ : une mère, un père, un fils mineur et une fille majeur
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pere = addIndividu(indPere, date(1960, 1, 1), "Cognac", "Guy", true);
				MockIndividu fils = addIndividu(indFils, date(2000, 2, 8), "Cognac", "Yvan", true);
				MockIndividu fille = addIndividu(indFille, date(2005, 2, 8), "Cognac", "Eva", false);

				addAdresse(pere, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, null, date(1998, 1, 1), null);
				addAdresse(fils, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, null, date(1998, 1, 1), null);
				addAdresse(fille, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, null, date(1998, 1, 1), null);

				fils.setParentsFromIndividus(Arrays.<Individu>asList(pere));
				fille.setParentsFromIndividus(Arrays.<Individu>asList(pere));
				pere.setEnfantsFromIndividus(Arrays.<Individu>asList(fils, fille));
			}
		});

		class Ids {
			Long pere;
			Long fils;
			Long fille;
		}
		final Ids ids = new Ids();

		final long idDi2011 = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pere = addHabitant(indPere);
				ids.pere = pere.getId();
				final PersonnePhysique fils = addHabitant(indFils);
				ids.fils = fils.getId();
				final PersonnePhysique fille = addHabitant(indFille);
				ids.fille = fille.getId();
				addCollAdm(MockCollectiviteAdministrative.CEDI);
				final CollectiviteAdministrative aci = addCollAdm(MockCollectiviteAdministrative.ACI);

				// Crée une for
				addForPrincipal(pere, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Vevey);

				final PeriodeFiscale periode2011 = addPeriodeFiscale(2011);
				final ModeleDocument modele2011 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE, periode2011);
				final DeclarationImpotOrdinaire declaration2011 = addDeclarationImpot(pere, periode2011, date(2011, 1, 1), date(2011, 4, 23), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
				declaration2011.setNumeroOfsForGestion(MockCommune.Vevey.getNoOFSEtendu());
				declaration2011.setRetourCollectiviteAdministrativeId(aci.getId());

				return declaration2011.getId();
			}
		});

		final DeclarationImpotOrdinaire di2011 = diDAO.get(idDi2011);
		final DI di = impressionDIHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(di2011), null, false);
		assertNotNull(di);

		assertNotNull(di.getEnfants());
		assertEquals(2, di.getEnfants().getEnfantArray().length);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testImpressionAnnexe230() throws Exception {
		loadDatabase("ImpressionDeclarationAnnexe_230.xml");
		DeclarationImpotOrdinaire declaration = diDAO.get(Long.valueOf(2));
		DI di = impressionDIHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration), buildDefaultAnnexes(declaration), false);
		validate(di);
		assertEquals(1, di.getAnnexes().getAnnexe230());


	}


	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testZoneAffranchissement() throws Exception {

		addCollAdm(MockCollectiviteAdministrative.CEDI);
		addCollAdm(MockOfficeImpot.OID_VEVEY);
		final CollectiviteAdministrative aci = addCollAdm(MockCollectiviteAdministrative.ACI);

		// Crée une personne physique
		final PersonnePhysique pp = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
		addForPrincipal(pp, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Vevey);

		final PeriodeFiscale periode2010 = addPeriodeFiscale(2010);
		final ModeleDocument modele2010 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE, periode2010);
		final DeclarationImpotOrdinaire declaration2010 = addDeclarationImpot(pp, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
		declaration2010.setNumeroOfsForGestion(MockCommune.Vevey.getNoOFSEtendu());
		declaration2010.setRetourCollectiviteAdministrativeId(aci.getId());

		final TypFichierImpression.Document document = impressionDIHelper.remplitEditiqueSpecifiqueDI(new InformationsDocumentAdapter(declaration2010), TypFichierImpression.Factory.newInstance(),
				null, false);
		assertNotNull(document);
		assertEquals(EditiqueHelper.ZONE_AFFRANCHISSEMENT_SUISSE,document.getInfoDocument().getAffranchissement().getZone());
	}


	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testInfosSurDI2011() throws Exception {

		addCollAdm(MockCollectiviteAdministrative.CEDI);
		final CollectiviteAdministrative aci = addCollAdm(MockCollectiviteAdministrative.ACI);

		// Crée une personne physique décédé
		final PersonnePhysique pp = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
		addForPrincipal(pp, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Vevey);

		final PeriodeFiscale periode2011 = addPeriodeFiscale(2011);
		final ModeleDocument modele2011 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE, periode2011);
		final DeclarationImpotOrdinaire declaration2011 = addDeclarationImpot(pp, periode2011, date(2011, 1, 1), date(2011, 4, 23), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
		declaration2011.setNumeroOfsForGestion(MockCommune.Vevey.getNoOFSEtendu());
		declaration2011.setRetourCollectiviteAdministrativeId(aci.getId());

		final DI di = impressionDIHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration2011), null, false);
		assertNotNull(di);
		//le NIP doit être présent
		assertNotNull(di.getInfoDI().getNIP());
		assertEquals("D", di.getInfoDI().getCODETRAME());

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testInfosSurDI2010() throws Exception {

		addCollAdm(MockCollectiviteAdministrative.CEDI);
		final CollectiviteAdministrative aci = addCollAdm(MockCollectiviteAdministrative.ACI);

		// Crée une personne physique
		final PersonnePhysique pp = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
		addForPrincipal(pp, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Vevey);

		final PeriodeFiscale periode2010 = addPeriodeFiscale(2010);
		final ModeleDocument modele2010 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE, periode2010);
		final DeclarationImpotOrdinaire declaration2010 = addDeclarationImpot(pp, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
		declaration2010.setNumeroOfsForGestion(MockCommune.Vevey.getNoOFSEtendu());
		declaration2010.setRetourCollectiviteAdministrativeId(aci.getId());

		final DI di = impressionDIHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration2010), null, false);
		assertNotNull(di);
		//le NIP ne doit pas être présent
		assertNull(di.getInfoDI().getNIP());
		//La valeur de  code trame à X
		assertEquals("X", di.getInfoDI().getCODETRAME());

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCtbAvecEnfantAvant2011() throws Exception {
		final long indPere = 2;
		final long indFils = 3;
		final long indFille = 4;

		// On crée la situation de départ : une mère, un père, un fils mineur et une fille majeur
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pere = addIndividu(indPere, date(1960, 1, 1), "Cognac", "Guy", true);
				MockIndividu fils = addIndividu(indFils, date(2000, 2, 8), "Cognac", "Yvan", true);
				MockIndividu fille = addIndividu(indFille, date(2005, 2, 8), "Cognac", "Eva", false);

				addAdresse(pere, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, null, date(1998, 1, 1), null);
				addAdresse(fils, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, null, date(1998, 1, 1), null);
				addAdresse(fille, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, null, date(1998, 1, 1), null);

				fils.setParentsFromIndividus(Arrays.<Individu>asList(pere));
				fille.setParentsFromIndividus(Arrays.<Individu>asList(pere));
				pere.setEnfantsFromIndividus(Arrays.<Individu>asList(fils, fille));
			}
		});

		class Ids {
			Long pere;
			Long fils;
			Long fille;
		}
		final Ids ids = new Ids();

		final long idDi2011 = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pere = addHabitant(indPere);
				ids.pere = pere.getId();
				final PersonnePhysique fils = addHabitant(indFils);
				ids.fils = fils.getId();
				final PersonnePhysique fille = addHabitant(indFille);
				ids.fille = fille.getId();
				addCollAdm(MockCollectiviteAdministrative.CEDI);
				final CollectiviteAdministrative aci = addCollAdm(MockCollectiviteAdministrative.ACI);

				// Crée une for
				addForPrincipal(pere, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Vevey);

				final PeriodeFiscale periode2010 = addPeriodeFiscale(2010);
				final ModeleDocument modele2010 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE, periode2010);
				final DeclarationImpotOrdinaire declaration2010 = addDeclarationImpot(pere, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
				declaration2010.setNumeroOfsForGestion(MockCommune.Vevey.getNoOFSEtendu());
				declaration2010.setRetourCollectiviteAdministrativeId(aci.getId());

				return declaration2010.getId();
			}
		});

		final DeclarationImpotOrdinaire di2011 = diDAO.get(idDi2011);
		final DI di = impressionDIHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(di2011), null, false);
		assertNotNull(di);

		assertNull(di.getEnfants());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRemplitDiAvecCodeRegion() throws Exception {

		doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final CollectiviteAdministrative cedi = tiersService.getOrCreateCollectiviteAdministrative(ServiceInfrastructureService.noCEDI);
				final Integer idDistrictOrbe = MockCollectiviteAdministrative.districts.get(MockOfficeImpot.OID_ORBE.getNoColAdm());
				final Integer idDistrictAigle = MockCollectiviteAdministrative.districts.get(MockOfficeImpot.OID_AIGLE.getNoColAdm());
				final Integer idRegionOrbe = MockCollectiviteAdministrative.regions.get(MockOfficeImpot.OID_ORBE.getNoColAdm());
				final Integer idRegionAigle = MockCollectiviteAdministrative.regions.get(MockOfficeImpot.OID_AIGLE.getNoColAdm());

				final Integer idDistrictVevey = MockCollectiviteAdministrative.regions.get(MockOfficeImpot.OID_VEVEY.getNoColAdm());
				final Integer idRegionVevey = MockCollectiviteAdministrative.regions.get(MockOfficeImpot.OID_VEVEY.getNoColAdm());

				final CollectiviteAdministrative orbe = addCollAdm(MockOfficeImpot.OID_ORBE, idDistrictOrbe, idRegionOrbe);
				final CollectiviteAdministrative aigle = addCollAdm(MockOfficeImpot.OID_AIGLE, idDistrictAigle, idRegionAigle);
				final CollectiviteAdministrative vevey = addCollAdm(MockOfficeImpot.OID_VEVEY, idDistrictVevey, idRegionVevey);
				return null;
			}
		});

		CollectiviteAdministrative cedi = tiersDAO.getCollectiviteAdministrativesByNumeroTechnique(MockCollectiviteAdministrative.CEDI.getNoColAdm());
		CollectiviteAdministrative orbe = tiersDAO.getCollectiviteAdministrativesByNumeroTechnique(MockOfficeImpot.OID_ORBE.getNoColAdm());
		CollectiviteAdministrative aigle = tiersDAO.getCollectiviteAdministrativesByNumeroTechnique(MockOfficeImpot.OID_AIGLE.getNoColAdm());
		// Crée une personne physique (ctb ordinaire vaudois) qui a déménagé début 2008 de Vallorbe à Bex
		final PersonnePhysique pp = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
		addForPrincipal(pp, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Aigle);
		final String numCtb = String.format("%09d", pp.getNumero());

		final PeriodeFiscale periode2011 = addPeriodeFiscale(2011);
		final ModeleDocument modele2011 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, periode2011);
		final DeclarationImpotOrdinaire declaration2011 = addDeclarationImpot(pp, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
		declaration2011.setNumeroOfsForGestion(MockCommune.Aigle.getNoOFSEtendu());
		declaration2011.setRetourCollectiviteAdministrativeId(cedi.getId());

		// L'expéditeur de la déclaration 2007 doit être Aigle (= OID responsable de Bex)
		//Selon UNIREG-3059:
		//L'OID doit être l'OID de gestion valable au 31.12 de l'année N-1 (N étant la période lors de laquel l'édition du document a lieu)
		//-> SAUF une exception : si la DI concerne la période fiscale courante (il s'agit d'une DI libre),
		// alors l'OID doit être l'OID de gestion courant du moment de l'édition du docuement.
		{
			final String oidOrbe = String.format("%02d", orbe.getNumeroCollectiviteAdministrative());
			final String oidAigle = String.format("%02d", aigle.getNumeroCollectiviteAdministrative());

			// ... sur l'entête
			final InfoEnteteDocument entete = impressionDIHelper.remplitEnteteDocument(new InformationsDocumentAdapter(declaration2011));
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
			final DI di = impressionDIHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration2011), null, false);
			assertNotNull(di);
			final DI.InfoDI info = di.getInfoDI();
			assertNotNull(info);
			assertEquals(numCtb + "20110101", info.getCODBARR());
			assertEquals("18-0", info.getNOOID());

			// ... sur l'adresse du CEDI
			final DIRetour.AdresseRetour retour = di.getAdresseRetour();
			assertNotNull(retour);
			assertEquals("Centre d'enregistrement", retour.getADRES1RETOUR());
			assertEquals("des déclarations d'impôt", retour.getADRES2RETOUR());
			assertEquals("CEDI 18", retour.getADRES3RETOUR());
			assertEquals("1014 Lausanne Adm cant", retour.getADRES4RETOUR());
		}


	}

	private static void validate(XmlObject document) {

		// Endroit où on va récupérer les éventuelles erreurs
		final XmlOptions validateOptions = new XmlOptions();
		final List<XmlError> errorList = new ArrayList<XmlError>();
		validateOptions.setErrorListener(errorList);

		// C'est parti pour la validation !
		final boolean isValid = document.validate(validateOptions);

		// si le document n'est pas valide, on va logguer pour avoir de quoi identifier et corriger le bug ensuite
		if (!isValid) {
			final StringBuilder b = new StringBuilder();
			b.append("--------------------------------------------------\n");
			b.append("--------------------------------------------------\n");
			b.append("Erreur de validation du message éditique en sortie\n");
			b.append("--------------------------------------------------\n");
			b.append("Message :\n").append(document).append('\n');
			b.append("--------------------------------------------------\n");
			for (XmlError error : errorList) {
				b.append("Erreur : ").append(error.getMessage()).append('\n');
				b.append("Localisation de l'erreur : ").append(error.getCursorLocation().xmlText()).append('\n');
				b.append("--------------------------------------------------\n");
			}
			b.append("--------------------------------------------------\n");
			Assert.fail(b.toString());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testcodeOIDSurDI() throws Exception {
		doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final CollectiviteAdministrative cedi = addCollAdm(MockCollectiviteAdministrative.CEDI);
				final CollectiviteAdministrative oidAigle = addCollAdm(MockOfficeImpot.OID_AIGLE);

				final Integer idDistrictVevey = MockCollectiviteAdministrative.regions.get(MockOfficeImpot.OID_VEVEY.getNoColAdm());
				final Integer idRegionVevey = MockCollectiviteAdministrative.regions.get(MockOfficeImpot.OID_VEVEY.getNoColAdm());

				final Integer idDistrictNyon = MockCollectiviteAdministrative.regions.get(MockOfficeImpot.OID_NYON.getNoColAdm());
				final Integer idRegionNyon = MockCollectiviteAdministrative.regions.get(MockOfficeImpot.OID_NYON.getNoColAdm());

				final Integer idDistrictLausanne = MockCollectiviteAdministrative.regions.get(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());
				final Integer idRegionLausanne = MockCollectiviteAdministrative.regions.get(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());

				final Integer idDistrictYverdon = MockCollectiviteAdministrative.regions.get(MockOfficeImpot.OID_YVERDON.getNoColAdm());
				final Integer idRegionYverdon = MockCollectiviteAdministrative.regions.get(MockOfficeImpot.OID_YVERDON.getNoColAdm());

				final CollectiviteAdministrative oidVevey = addCollAdm(MockOfficeImpot.OID_VEVEY, idDistrictVevey, idRegionVevey);
				final CollectiviteAdministrative oidLausanne = addCollAdm(MockOfficeImpot.OID_LAUSANNE_OUEST, idDistrictLausanne, idRegionLausanne);
				final CollectiviteAdministrative oidNyon = addCollAdm(MockOfficeImpot.OID_NYON, idDistrictNyon, idRegionNyon);
				final CollectiviteAdministrative oidYverdon = addCollAdm(MockOfficeImpot.OID_YVERDON, idDistrictYverdon, idRegionYverdon);
				return null;
			}

		});

		final PeriodeFiscale periode2010 = addPeriodeFiscale(2010);
		final ModeleDocument modele2010 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2010);

		final PeriodeFiscale periode2011 = addPeriodeFiscale(2011);
		final ModeleDocument modele2011 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2011);


		final CollectiviteAdministrative cedi = tiersDAO.getCollectiviteAdministrativesByNumeroTechnique(MockCollectiviteAdministrative.CEDI.getNoColAdm());
		final long idCedi = cedi.getId();
		//   SI l'utilisateur choisit "OID" dans l'adresse de retour, il s'agira alors d'imprimer l'OID de gestion
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Aigle);

			final DeclarationImpotOrdinaire declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.Aigle.getNoOFSEtendu());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010));
			assertEquals(1, codeOid2010.intValue());


			final DeclarationImpotOrdinaire declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.Aigle.getNoOFSEtendu());
			CollectiviteAdministrative oidAigle = tiersDAO.getCollectiviteAdministrativesByNumeroTechnique(MockOfficeImpot.OID_AIGLE.getNoColAdm());
			declaration2011.setRetourCollectiviteAdministrativeId(oidAigle.getId());
			final Integer codeOid2011 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011));
			assertEquals(1, codeOid2011.intValue());

		}


		//TEST sur la récupération des oid de region


		//AIgle
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Aigle);

			final DeclarationImpotOrdinaire declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.Aigle.getNoOFSEtendu());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010));
			assertEquals(1, codeOid2010.intValue());


			final DeclarationImpotOrdinaire declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.Aigle.getNoOFSEtendu());
			declaration2011.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2011 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011));
			assertEquals(18, codeOid2011.intValue());

		}


		//Echallens
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Echallens);

			final DeclarationImpotOrdinaire declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.Echallens.getNoOFSEtendu());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010));
			assertEquals(5, codeOid2010.intValue());


			final DeclarationImpotOrdinaire declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.Echallens.getNoOFSEtendu());
			declaration2011.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2011 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011));
			assertEquals(19, codeOid2011.intValue());

		}


		//Grandson
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Grandson);

			final DeclarationImpotOrdinaire declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.Grandson.getNoOFSEtendu());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010));
			assertEquals(6, codeOid2010.intValue());

			final DeclarationImpotOrdinaire declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.Grandson.getNoOFSEtendu());
			declaration2011.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2011 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011));
			assertEquals(19, codeOid2011.intValue());

		}


		//Lausanne
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Lausanne);

			final DeclarationImpotOrdinaire declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.Lausanne.getNoOFSEtendu());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010));
			assertEquals(7, codeOid2010.intValue());

			final DeclarationImpotOrdinaire declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.Lausanne.getNoOFSEtendu());
			declaration2011.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2011 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011));
			assertEquals(7, codeOid2011.intValue());

		}

		//La vallée (L'abbaye)
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Fraction.LAbbaye);

			final DeclarationImpotOrdinaire declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.Fraction.LAbbaye.getNoOFSEtendu());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010));
			assertEquals(8, codeOid2010.intValue());

			final DeclarationImpotOrdinaire declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.Fraction.LAbbaye.getNoOFSEtendu());
			declaration2011.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2011 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011));
			assertEquals(19, codeOid2011.intValue());

		}


		//Lavaux
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Pully);

			final DeclarationImpotOrdinaire declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.Pully.getNoOFSEtendu());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010));
			assertEquals(9, codeOid2010.intValue());

			final DeclarationImpotOrdinaire declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.Pully.getNoOFSEtendu());
			declaration2011.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2011 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011));
			assertEquals(18, codeOid2011.intValue());

		}


		//Morges
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Morges);

			final DeclarationImpotOrdinaire declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.Morges.getNoOFSEtendu());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010));
			assertEquals(10, codeOid2010.intValue());


			final DeclarationImpotOrdinaire declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.Morges.getNoOFSEtendu());
			declaration2011.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2011 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011));
			assertEquals(12, codeOid2011.intValue());

		}


		//Moudon
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Moudon);

			final DeclarationImpotOrdinaire declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.Moudon.getNoOFSEtendu());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010));
			assertEquals(11, codeOid2010.intValue());

			final DeclarationImpotOrdinaire declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.Moudon.getNoOFSEtendu());
			declaration2011.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2011 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011));
			assertEquals(19, codeOid2011.intValue());

		}


		//Nyon
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Nyon);

			final DeclarationImpotOrdinaire declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.Nyon.getNoOFSEtendu());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010));
			assertEquals(12, codeOid2010.intValue());

			final DeclarationImpotOrdinaire declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.Nyon.getNoOFSEtendu());
			declaration2011.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2011 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011));
			assertEquals(12, codeOid2011.intValue());

		}


		//Orbe
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Orbe);

			final DeclarationImpotOrdinaire declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.Orbe.getNoOFSEtendu());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010));
			assertEquals(13, codeOid2010.intValue());

			final DeclarationImpotOrdinaire declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.Orbe.getNoOFSEtendu());
			declaration2011.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2011 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011));
			assertEquals(19, codeOid2011.intValue());

		}


		//Payerne
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.GrangesMarnand);


			final DeclarationImpotOrdinaire declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.GrangesMarnand.getNoOFSEtendu());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010));
			assertEquals(15, codeOid2010.intValue());

			final DeclarationImpotOrdinaire declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.GrangesMarnand.getNoOFSEtendu());
			declaration2011.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2011 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011));
			assertEquals(19, codeOid2011.intValue());

		}


		//Pays d'Enhaut
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.ChateauDoex);

			final DeclarationImpotOrdinaire declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.ChateauDoex.getNoOFSEtendu());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010));
			assertEquals(16, codeOid2010.intValue());

			final DeclarationImpotOrdinaire declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.ChateauDoex.getNoOFSEtendu());
			declaration2011.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2011 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011));
			assertEquals(18, codeOid2011.intValue());

		}


		//Rolle - Aubonne
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Aubonne);

			final DeclarationImpotOrdinaire declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.Aubonne.getNoOFSEtendu());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010));
			assertEquals(17, codeOid2010.intValue());

			final DeclarationImpotOrdinaire declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.Aubonne.getNoOFSEtendu());
			declaration2011.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2011 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011));
			assertEquals(12, codeOid2011.intValue());

		}


		//Vevey
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Vevey);


			final DeclarationImpotOrdinaire declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.Vevey.getNoOFSEtendu());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010));
			assertEquals(18, codeOid2010.intValue());


			final DeclarationImpotOrdinaire declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.Vevey.getNoOFSEtendu());
			declaration2011.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2011 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011));
			assertEquals(18, codeOid2011.intValue());

		}


		//Yverdon
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.YverdonLesBains);

			final DeclarationImpotOrdinaire declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.YverdonLesBains.getNoOFSEtendu());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010));
			assertEquals(19, codeOid2010.intValue());


			final DeclarationImpotOrdinaire declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.YverdonLesBains.getNoOFSEtendu());
			declaration2011.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2011 = impressionDIHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011));
			assertEquals(19, codeOid2011.intValue());

		}


	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIDEnvoiHorsSuisse() throws Exception {
		LOGGER.debug("EditiqueHelperTest - testRemplitExpediteur UNIREG-2541");


		final CollectiviteAdministrative cedi = tiersService.getOrCreateCollectiviteAdministrative(ServiceInfrastructureService.noCEDI);
		final CollectiviteAdministrative morges = tiersService.getOrCreateCollectiviteAdministrative(MockOfficeImpot.OID_MORGES.getNoColAdm());
		final CollectiviteAdministrative aci = tiersService.getOrCreateCollectiviteAdministrative(ServiceInfrastructureService.noACI);


		// Crée une personne physique (ctb ordinaire vaudois) qui a déménagé mi 2010 de Morges à Paris
		final PersonnePhysique pp = addNonHabitant("Céline", "André", date(1980, 6, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(2006, 1, 1), MotifFor.ARRIVEE_HS, date(2010, 6, 30), MotifFor.DEPART_HS, MockCommune.Morges);
		addAdresseEtrangere(pp,TypeAdresseTiers.COURRIER,date(2010,7,1),null,null,null, MockPays.Danemark);

		final String numCtb = String.format("%09d", pp.getNumero());

		final PeriodeFiscale periode2012 = addPeriodeFiscale(2012);
		final ModeleDocument modele2012 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, periode2012);
		final DeclarationImpotOrdinaire declaration2012 = addDeclarationImpot(pp, periode2012, date(2012, 1, 1), date(2012, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2012);
		declaration2012.setNumeroOfsForGestion(MockCommune.Morges.getNoOFSEtendu());
		declaration2012.setRetourCollectiviteAdministrativeId(aci.getId());
		{

			final TypFichierImpression.Document document= impressionDIHelper.remplitEditiqueSpecifiqueDI(new InformationsDocumentAdapter(declaration2012),TypFichierImpression.Factory.newInstance(),
					null, false);
			assertNotNull(document);
			assertEquals(EditiqueHelper.ZONE_AFFRANCHISSEMENT_EUROPE,document.getInfoDocument().getAffranchissement().getZone());
			assertEquals("",document.getInfoDocument().getIdEnvoi());


		}


	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIDEnvoiHorsSuisseAdresseIncomplete() throws Exception {
		LOGGER.debug("EditiqueHelperTest - testRemplitExpediteur UNIREG-2541");


		final CollectiviteAdministrative cedi = tiersService.getOrCreateCollectiviteAdministrative(ServiceInfrastructureService.noCEDI);
		final CollectiviteAdministrative morges = tiersService.getOrCreateCollectiviteAdministrative(MockOfficeImpot.OID_MORGES.getNoColAdm());
		final CollectiviteAdministrative aci = tiersService.getOrCreateCollectiviteAdministrative(ServiceInfrastructureService.noACI);


		// Crée une personne physique (ctb ordinaire vaudois) qui a déménagé mi 2010 de Morges à Paris
		final PersonnePhysique pp = addNonHabitant("Céline", "André", date(1980, 6, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(2006, 1, 1), MotifFor.ARRIVEE_HS, date(2010, 6, 30), MotifFor.DEPART_HS, MockCommune.Morges);
		addAdresseEtrangere(pp,TypeAdresseTiers.COURRIER,date(2010,7,1),null,null,null,MockPays.PaysInconnu);

		final String numCtb = String.format("%09d", pp.getNumero());

		final PeriodeFiscale periode2012 = addPeriodeFiscale(2012);
		final ModeleDocument modele2012 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, periode2012);
		final DeclarationImpotOrdinaire declaration2012 = addDeclarationImpot(pp, periode2012, date(2012, 1, 1), date(2012, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2012);
		declaration2012.setNumeroOfsForGestion(MockCommune.Morges.getNoOFSEtendu());
		declaration2012.setRetourCollectiviteAdministrativeId(aci.getId());
		{

			final TypFichierImpression.Document document= impressionDIHelper.remplitEditiqueSpecifiqueDI(new InformationsDocumentAdapter(declaration2012),TypFichierImpression.Factory.newInstance(),
					null, false);
			assertNotNull(document);
			assertEquals(EditiqueHelper.ZONE_AFFRANCHISSEMENT_RESTE_MONDE,document.getInfoDocument().getAffranchissement().getZone());
			assertEquals("10",document.getInfoDocument().getIdEnvoi());


		}


	}



}
