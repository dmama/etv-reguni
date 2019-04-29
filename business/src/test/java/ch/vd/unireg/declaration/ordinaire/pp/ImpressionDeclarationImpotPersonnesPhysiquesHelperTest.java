package ch.vd.unireg.declaration.ordinaire.pp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import noNamespace.DIBase;
import noNamespace.DIDocument.DI;
import noNamespace.DIHCDocument;
import noNamespace.DIRetour;
import noNamespace.FichierImpressionDocument;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Destinataire;
import noNamespace.InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur;
import noNamespace.TypAdresse.Adresse;
import noNamespace.TypFichierImpression;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.ModeleFeuilleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.editique.LegacyEditiqueHelper;
import ch.vd.unireg.editique.ModeleFeuilleDocumentEditique;
import ch.vd.unireg.editique.ZoneAffranchissementEditique;
import ch.vd.unireg.etiquette.Etiquette;
import ch.vd.unireg.etiquette.EtiquetteService;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.mock.DefaultMockServiceInfrastructureService;
import ch.vd.unireg.interfaces.infra.mock.MockBatiment;
import ch.vd.unireg.interfaces.infra.mock.MockCollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.situationfamille.SituationFamilleService;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.EtatCivil;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.ModeleFeuille;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings({"JavaDoc"})
public class ImpressionDeclarationImpotPersonnesPhysiquesHelperTest extends BusinessTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImpressionDeclarationImpotPersonnesPhysiquesHelperTest.class);

	private static final String DB_UNIT_DATA_FILE = "classpath:ch/vd/unireg/declaration/ordinaire/pp/ImpressionDeclarationImpotOrdinaireHelperTest.xml";

	private DeclarationImpotOrdinaireDAO diDAO;
	private ImpressionDeclarationImpotPersonnesPhysiquesHelperImpl impressionDIPPHelper;
	private AdresseService adresseService;
	private TiersService tiersService;
	private SituationFamilleService situationFamilleService;
	private LegacyEditiqueHelper editiqueHelper;
	private EtiquetteService etiquetteService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		diDAO = getBean(DeclarationImpotOrdinaireDAO.class, "diDAO");

		adresseService = getBean(AdresseService.class, "adresseService");
		tiersService = getBean(TiersService.class, "tiersService");
		situationFamilleService = getBean(SituationFamilleService.class, "situationFamilleService");
		editiqueHelper = getBean(LegacyEditiqueHelper.class, "legacyEditiqueHelper");
		serviceInfra.setUp(new DefaultMockServiceInfrastructureService());
		impressionDIPPHelper = new ImpressionDeclarationImpotPersonnesPhysiquesHelperImpl(serviceInfra, adresseService, tiersService, situationFamilleService, editiqueHelper);
		etiquetteService = getBean(EtiquetteService.class, "etiquetteService");
	}

	@Test
	public void testRemplitExpediteur() throws Exception {
		LOGGER.debug("EditiqueHelperTest - testRemplitExpediteur");
		loadDatabase(DB_UNIT_DATA_FILE);

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				DeclarationImpotOrdinairePP declaration = (DeclarationImpotOrdinairePP) diDAO.get(2L);
				InfoEnteteDocument infoEnteteDocument = impressionDIPPHelper.remplitEnteteDocument(new InformationsDocumentAdapter(declaration, null));
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
		});
	}
	//UNIREG-2541 Adresse de retour pour les DI hors canton 

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAdresseRetourDIHorsCanton() throws Exception {
		LOGGER.debug("EditiqueHelperTest - testRemplitExpediteur UNIREG-2541");

		final CollectiviteAdministrative aci = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noACI);

		// Crée une personne physique (ctb ordinaire vaudois) qui a déménagé mi 2010 de Morges à Paris
		final PersonnePhysique pp = addNonHabitant("Céline", "André", date(1980, 6, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(2006, 1, 1), MotifFor.ARRIVEE_HS, date(2010, 6, 30), MotifFor.DEPART_HS, MockCommune.Morges);

		final String numCtb = String.format("%09d", pp.getNumero());

		final PeriodeFiscale periode2010 = addPeriodeFiscale(2010);
		final ModeleDocument modele2010 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, periode2010);
		final DeclarationImpotOrdinairePP declaration2010 = addDeclarationImpot(pp, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
		declaration2010.setNumeroOfsForGestion(MockCommune.Morges.getNoOFS());
		declaration2010.setRetourCollectiviteAdministrativeId(aci.getId());
		{

			final DIHCDocument.DIHC di = impressionDIPPHelper.remplitSpecifiqueDIHC(new InformationsDocumentAdapter(declaration2010, null), null);
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

		final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noCEDI);
		final CollectiviteAdministrative nyon = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_NYON.getNoColAdm());
		final CollectiviteAdministrative echallens = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_ECHALLENS.getNoColAdm());
		final CollectiviteAdministrative yverdon = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_YVERDON.getNoColAdm());

		final String oidNyon = String.format("%02d", nyon.getNumeroCollectiviteAdministrative());
		final String oidEchallens = String.format("%02d", echallens.getNumeroCollectiviteAdministrative());
		final String oidYverdon = String.format("%02d", yverdon.getNumeroCollectiviteAdministrative());
		final int anneeCourante = RegDate.get().year();

		final PersonnePhysique pp = addNonHabitant("Céline", "André", date(1980, 6, 23), Sexe.MASCULIN);
		addForPrincipal(pp, date(2008, 1, 1), MotifFor.ARRIVEE_HS, date(2008, 12, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Aigle);
		addForPrincipal(pp, date(2009, 1, 1), MotifFor.DEMENAGEMENT_VD, date(anneeCourante - 1, 12, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Nyon);
		addForPrincipal(pp, date(anneeCourante, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Echallens);

		final String numCtb = String.format("%09d", pp.getNumero());
		final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
		final PeriodeFiscale periode2009 = addPeriodeFiscale(2009);

		final PeriodeFiscale periodeCourante = addPeriodeFiscale(anneeCourante);
		final ModeleDocument modele2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, periode2008);
		final ModeleDocument modele2009 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, periode2009);
		final ModeleDocument modeleCourant = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, periodeCourante);
		final DeclarationImpotOrdinairePP declaration2008 = addDeclarationImpot(pp, periode2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2008);
		final DeclarationImpotOrdinairePP declaration2009 = addDeclarationImpot(pp, periode2009, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2009);
		final DeclarationImpotOrdinairePP declarationCourante =
				addDeclarationImpot(pp, periodeCourante, date(anneeCourante, 1, 1), date(anneeCourante, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modeleCourant);

		declaration2008.setNumeroOfsForGestion(MockCommune.Aigle.getNoOFS());
		declaration2008.setRetourCollectiviteAdministrativeId(cedi.getId());
		declaration2009.setNumeroOfsForGestion(MockCommune.Nyon.getNoOFS());
		declaration2009.setRetourCollectiviteAdministrativeId(cedi.getId());
		declarationCourante.setNumeroOfsForGestion(MockCommune.Echallens.getNoOFS());
		declarationCourante.setRetourCollectiviteAdministrativeId(cedi.getId());
		{

			final DI di2008 = impressionDIPPHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration2008, null), new ArrayList<>());
			assertNotNull(di2008);
			final DI di2009 = impressionDIPPHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration2009, null), new ArrayList<>());
			assertNotNull(di2009);
			final DI diCourante = impressionDIPPHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declarationCourante, null), new ArrayList<>());
			assertNotNull(diCourante);


			//Adresse expedition 2008
			InfoEnteteDocument infoEnteteDocument2008 = impressionDIPPHelper.remplitEnteteDocument(new InformationsDocumentAdapter(declaration2008, null));
			Expediteur expediteur2008 = infoEnteteDocument2008.getExpediteur();
			Adresse adresseExpediteur2008 = expediteur2008.getAdresse();
			assertEquals("Office d'impôt du district", adresseExpediteur2008.getAdresseCourrierLigne1());
			assertEquals("d'Aigle", adresseExpediteur2008.getAdresseCourrierLigne2());
			assertEquals("rue de la Gare 27", adresseExpediteur2008.getAdresseCourrierLigne3());
			assertEquals("1860 Aigle", adresseExpediteur2008.getAdresseCourrierLigne4());
			assertNull(adresseExpediteur2008.getAdresseCourrierLigne6());

			//Adresse expedition 2009
			InfoEnteteDocument infoEnteteDocument2009 = impressionDIPPHelper.remplitEnteteDocument(new InformationsDocumentAdapter(declaration2009, null));
			Expediteur expediteur2009 = infoEnteteDocument2009.getExpediteur();
			Adresse adresseExpediteur2009 = expediteur2009.getAdresse();
			assertEquals("Office d'impôt du district", adresseExpediteur2009.getAdresseCourrierLigne1());
			assertEquals("de Nyon", adresseExpediteur2009.getAdresseCourrierLigne2());
			assertEquals("Avenue Reverdil 4-6", adresseExpediteur2009.getAdresseCourrierLigne3());
			assertEquals("1341 Nyon", adresseExpediteur2009.getAdresseCourrierLigne4());
			assertNull(adresseExpediteur2009.getAdresseCourrierLigne6());

			//Adresse expedition annee courante
			InfoEnteteDocument infoEnteteDocumentCourant = impressionDIPPHelper.remplitEnteteDocument(new InformationsDocumentAdapter(declarationCourante, null));
			Expediteur expediteurCourant = infoEnteteDocumentCourant.getExpediteur();
			Adresse adresseExpediteurCourant = expediteurCourant.getAdresse();
			assertEquals("Office d'impôt du district", adresseExpediteurCourant.getAdresseCourrierLigne1());
			assertEquals("du Gros-de-Vaud", adresseExpediteurCourant.getAdresseCourrierLigne2());
			assertEquals("Place Emile Gardaz 5", adresseExpediteurCourant.getAdresseCourrierLigne3());
			assertEquals("1040 Echallens", adresseExpediteurCourant.getAdresseCourrierLigne4());
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
			assertEquals("CEDI " + yverdon.getNumeroCollectiviteAdministrative(), retourCourante.getADRES3RETOUR());
			assertEquals("1014 Lausanne Adm cant", retourCourante.getADRES4RETOUR());

			final DI.InfoDI infoDICourante = diCourante.getInfoDI();

			// à partir de 2011, le défaut pour le suffixe du code routage est 0, et plus 1 comme avant
			// (et c'est la région qui apparait dans le gros xx-0, pas le district)
			assertEquals(oidYverdon + "-0", infoDICourante.getNOOID());
			assertEquals(numCtb + anneeCourante + "01" + oidEchallens, infoDICourante.getCODBARR());

		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testInfoCommune() throws Exception {
		LOGGER.debug("EditiqueHelperTest - testInfoCommune SIFISC-1389");

		final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noCEDI);
		final CollectiviteAdministrative nyon = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_NYON.getNoColAdm());
		final CollectiviteAdministrative morges = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_MORGES.getNoColAdm());

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
		final DeclarationImpotOrdinairePP declaration2008 = addDeclarationImpot(pp, periode2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2008);
		final DeclarationImpotOrdinairePP declaration2009 = addDeclarationImpot(pp, periode2009, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2009);
		final DeclarationImpotOrdinairePP declarationCourante =
				addDeclarationImpot(pp, periodeCourante, date(anneeCourante, 1, 1), date(anneeCourante, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modeleCourant);

		declaration2008.setNumeroOfsForGestion(MockCommune.Aigle.getNoOFS());
		declaration2008.setRetourCollectiviteAdministrativeId(cedi.getId());
		declaration2009.setNumeroOfsForGestion(MockCommune.Nyon.getNoOFS());
		declaration2009.setRetourCollectiviteAdministrativeId(cedi.getId());
		declarationCourante.setNumeroOfsForGestion(MockCommune.Morges.getNoOFS());
		declarationCourante.setRetourCollectiviteAdministrativeId(cedi.getId());
		{

			final DI di2008 = impressionDIPPHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration2008, null), new ArrayList<>());
			assertNotNull(di2008);
			final DI di2009 = impressionDIPPHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration2009, null), new ArrayList<>());
			assertNotNull(di2009);
			final DI diCourante = impressionDIPPHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declarationCourante, null), new ArrayList<>());
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

		final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noCEDI);
		final CollectiviteAdministrative nyon = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_NYON.getNoColAdm());

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
		final DeclarationImpotOrdinairePP declaration2008 = addDeclarationImpot(pp, periode2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2008);
		final DeclarationImpotOrdinairePP declaration2009 = addDeclarationImpot(pp, periode2009, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2009);

		declaration2008.setNumeroOfsForGestion(MockCommune.Aigle.getNoOFS());
		declaration2008.setRetourCollectiviteAdministrativeId(cedi.getId());
		declaration2009.setNumeroOfsForGestion(MockCommune.Nyon.getNoOFS());
		declaration2009.setRetourCollectiviteAdministrativeId(cedi.getId());
		{

			final DI di2008 = impressionDIPPHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration2008, null), new ArrayList<>());
			assertNotNull(di2008);
			final DI di2009 = impressionDIPPHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration2009, null), new ArrayList<>());
			assertNotNull(di2009);


			//Adresse expedition 2008
			InfoEnteteDocument infoEnteteDocument2008 = impressionDIPPHelper.remplitEnteteDocument(new InformationsDocumentAdapter(declaration2008, null));
			Expediteur expediteur2008 = infoEnteteDocument2008.getExpediteur();
			Adresse adresseExpediteur2008 = expediteur2008.getAdresse();
			assertEquals("Office d'impôt du district", adresseExpediteur2008.getAdresseCourrierLigne1());
			assertEquals("d'Aigle", adresseExpediteur2008.getAdresseCourrierLigne2());
			assertEquals("rue de la Gare 27", adresseExpediteur2008.getAdresseCourrierLigne3());
			assertEquals("1860 Aigle", adresseExpediteur2008.getAdresseCourrierLigne4());
			assertNull(adresseExpediteur2008.getAdresseCourrierLigne6());

			//Adresse expedition 2009
			InfoEnteteDocument infoEnteteDocument2009 = impressionDIPPHelper.remplitEnteteDocument(new InformationsDocumentAdapter(declaration2009, null));
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

		final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noCEDI);

		final int anneeCourante = RegDate.get().year();
		// Crée une personne physique (ctb ordinaire vaudois) qui a déménagé mi 2010 de Morges à Paris
		final PersonnePhysique pp = addNonHabitant("Maelle", "André", date(1980, 6, 23), Sexe.MASCULIN);
		addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2009, 6, 14), null, MockRue.Aubonne.CheminDesClos);
		addForPrincipal(pp, date(2008, 6, 15), MotifFor.ARRIVEE_HS, date(2009, 6, 13), MotifFor.DEMENAGEMENT_VD, MockCommune.Aigle);
		addForPrincipal(pp, date(2009, 6, 14), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Nyon);

		final PeriodeFiscale periode2009 = addPeriodeFiscale(2009);

		final ModeleDocument modele2009 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, periode2009);
		final DeclarationImpotOrdinairePP declaration2009 = addDeclarationImpot(pp, periode2009, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2009);

		declaration2009.setNumeroOfsForGestion(MockCommune.Nyon.getNoOFS());
		declaration2009.setRetourCollectiviteAdministrativeId(cedi.getId());
		{

			final DI di2009 = impressionDIPPHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration2009, null), new ArrayList<>());
			assertNotNull(di2009);

			final String formuleAppel = di2009.getFormuleAppel();
			assertEquals("Monsieur", formuleAppel);


		}


	}

	//SIFISC-8417
	//type de document imprimé est différent du type de document attendu.
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testImpressionLocaleTypeDI() throws Exception {
		LOGGER.debug("EditiqueHelperTest - testImpressionTypeDI SIFISC-8417");

		final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noCEDI);

		final int anneeCourante = RegDate.get().year();
		// Crée une personne physique (ctb ordinaire vaudois) qui a déménagé mi 2010 de Morges à Paris
		final PersonnePhysique pp = addNonHabitant("Maelle", "André", date(1980, 6, 23), Sexe.MASCULIN);
		addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2009, 6, 14), null, MockRue.Aubonne.CheminDesClos);
		addForPrincipal(pp, date(2008, 6, 15), MotifFor.ARRIVEE_HS, date(2009, 6, 13), MotifFor.DEMENAGEMENT_VD, MockCommune.Aigle);
		addForPrincipal(pp, date(2009, 6, 14), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Nyon);

		final PeriodeFiscale periode2009 = addPeriodeFiscale(2009);

		final ModeleDocument modele2009 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, periode2009);
		final DeclarationImpotOrdinairePP declaration2009 = addDeclarationImpot(pp, periode2009, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2009);

		declaration2009.setNumeroOfsForGestion(MockCommune.Nyon.getNoOFS());
		declaration2009.setRetourCollectiviteAdministrativeId(cedi.getId());
		{

			final FichierImpressionDocument mainDocument = noNamespace.FichierImpressionDocument.Factory.newInstance();
			final TypFichierImpression editiqueDI = mainDocument.addNewFichierImpression();
			//Impression local de la DI mais avec un type de document vautax
			//on doit obtenir une DI de type vautax et non une DI du type sauvegardé(COMPLETE)
			final TypFichierImpression.Document di2009 = impressionDIPPHelper.remplitEditiqueSpecifiqueDI(declaration2009, editiqueDI, TypeDocument.DECLARATION_IMPOT_VAUDTAX, null);
			assertNull(di2009.getDI());
			assertNotNull(di2009.getDIVDTAX());

		}


	}

	/**
	 * [UNIREG-1257] l'office d'impôt expéditeur doit être celui du for fiscal valide durant la période couverte par la déclaration.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRemplitAncienneCommune() throws Exception {

		final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noCEDI);
		final CollectiviteAdministrative orbe = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_ORBE.getNoColAdm());
		final CollectiviteAdministrative aigle = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_AIGLE.getNoColAdm());

		// Crée une personne physique (ctb ordinaire vaudois) qui a déménagé début 2008 de Vallorbe à Bex
		final PersonnePhysique pp = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
		addForPrincipal(pp, date(2003, 1, 1), MotifFor.MAJORITE, date(2007, 12, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Vallorbe);
		addForPrincipal(pp, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Bex);
		final String numCtb = String.format("%09d", pp.getNumero());

		final PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
		final ModeleDocument modele2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, periode2007);
		final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
		final ModeleDocument modele2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, periode2008);
		final DeclarationImpotOrdinairePP declaration2007 = addDeclarationImpot(pp, periode2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2007);
		declaration2007.setNumeroOfsForGestion(MockCommune.Vallorbe.getNoOFS());
		declaration2007.setRetourCollectiviteAdministrativeId(cedi.getId());
		final DeclarationImpotOrdinairePP declaration2008 = addDeclarationImpot(pp, periode2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2008);
		declaration2008.setNumeroOfsForGestion(MockCommune.Bex.getNoOFS());
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
			final InfoEnteteDocument entete = impressionDIPPHelper.remplitEnteteDocument(new InformationsDocumentAdapter(declaration2007, null));
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
			final DI di = impressionDIPPHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration2007, null), new ArrayList<>());
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
			final InfoEnteteDocument entete = impressionDIPPHelper.remplitEnteteDocument(new InformationsDocumentAdapter(declaration2008, null));
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
			final DI di = impressionDIPPHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration2008, null), new ArrayList<>());
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

		final CollectiviteAdministrative vevey = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_VEVEY.getNoColAdm());

		// Crée une personne physique (ctb ordinaire vaudois) à la dépense
		final PersonnePhysique pp = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
		final ForFiscalPrincipalPP ffp = addForPrincipal(pp, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Vevey);
		ffp.setModeImposition(ModeImposition.DEPENSE);

		final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
		final ModeleDocument modele2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE, periode2008);
		final DeclarationImpotOrdinairePP declaration2008 = addDeclarationImpot(pp, periode2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_DEPENSE, modele2008);
		declaration2008.setNumeroOfsForGestion(MockCommune.Vevey.getNoOFS());
		declaration2008.setRetourCollectiviteAdministrativeId(vevey.getId());

		final DI di = impressionDIPPHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration2008, null), new ArrayList<>());
		assertNotNull(di);

		final DIRetour.AdresseRetour retour = di.getAdresseRetour();
		assertNotNull(retour);
		assertEquals("Office d'impôt du district", retour.getADRES1RETOUR());
		assertEquals("de la Riviera - Pays-d'Enhaut", retour.getADRES2RETOUR());
		assertEquals("Rue du Simplon 22", retour.getADRES3RETOUR());
		assertEquals("Case Postale 1032", retour.getADRES4RETOUR());
		assertEquals("1800 Vevey", retour.getADRES5RETOUR());
	}

	/**
	 * [UNIREG-1741] vérifie que l'adresse de retour d'une DI pour un contribuable décédé est bien CEDI - 22
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAdresseRetourDIDecede() throws Exception {

		final CollectiviteAdministrative aci = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noACI);

		// Crée une personne physique décédé
		final PersonnePhysique pp = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
		addForPrincipal(pp, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, date(2008, 4, 23), MotifFor.VEUVAGE_DECES, MockCommune.Vevey);

		final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
		final ModeleDocument modele2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE, periode2008);
		final DeclarationImpotOrdinairePP declaration2008 = addDeclarationImpot(pp, periode2008, date(2008, 1, 1), date(2008, 4, 23), TypeContribuable.VAUDOIS_DEPENSE, modele2008);
		declaration2008.setNumeroOfsForGestion(MockCommune.Vevey.getNoOFS());
		declaration2008.setRetourCollectiviteAdministrativeId(aci.getId());

		final DI di = impressionDIPPHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration2008, null), new ArrayList<>());
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

		DeclarationImpotOrdinairePP declaration = (DeclarationImpotOrdinairePP) diDAO.get(Long.valueOf(2));
		InfoEnteteDocument infoEnteteDocument = impressionDIPPHelper.remplitEnteteDocument(new InformationsDocumentAdapter(declaration, null));
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
		final List<ModeleFeuilleDocumentEditique> annexes = new ArrayList<>();
		final Set<ModeleFeuilleDocument> listFeuille = di.getModeleDocument().getModelesFeuilleDocument();
		for (ModeleFeuilleDocument feuille : listFeuille) {
			final ModeleFeuilleDocumentEditique feuilleEditique = new ModeleFeuilleDocumentEditique(feuille, 1);
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
	public void testRempliQuelquesMachins() throws Exception {

		final int annee = 2005;

		// mise en place fiscale
		final long diId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique lui = addNonHabitant("Alain", "Dupont", date(1977, 2, 12), Sexe.MASCULIN);
			final PersonnePhysique elle = addNonHabitant("Maria", "Dupont", date(1953, 12, 18), Sexe.FEMININ);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, date(1990, 7, 3), null);
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, date(1990, 7, 3), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.VillarsSousYens);
			addSituation(mc, date(1990, 7, 3), null, 0, null, EtatCivil.MARIE);

			final PeriodeFiscale pf = addPeriodeFiscale(annee);
			final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, md);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, md);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, md);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, md);

			final DeclarationImpotOrdinairePP di = addDeclarationImpot(mc, pf, date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, md);
			di.setNumeroOfsForGestion(MockCommune.VillarsSousYens.getNoOFS());
			di.setDelaiRetourImprime(date(annee + 1, 7, 31));
			return di.getId();
		});

		// test de composition
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final DeclarationImpotOrdinairePP declaration = (DeclarationImpotOrdinairePP) diDAO.get(diId);

				final DI di = impressionDIPPHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration, null), buildDefaultAnnexes(declaration));

				final DIRetour.AdresseRetour cediImpression = di.getAdresseRetour();
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

				assertEquals(Integer.toString(annee), di.getInfoDI().getANNEEFISCALE());
				final Long noTiers = declaration.getTiers().getNumero();
				final int noColAdm = MockOfficeImpot.OID_MORGES.getNoColAdm();
				assertEquals(String.format("%09d%4d%02d%02d", noTiers, annee, 1, noColAdm), di.getInfoDI().getCODBARR());
				assertEquals("31.07.2006", di.getInfoDI().getDELAIRETOUR());
				assertEquals("Villars-sous-Yens", di.getInfoDI().getDESCOM());
				assertEquals(FormatNumeroHelper.numeroCTBToDisplay(noTiers), di.getInfoDI().getNOCANT());
				assertEquals(String.format("%02d-1", noColAdm), di.getInfoDI().getNOOID());

				assertEquals(1, di.getAnnexes().getAnnexe210());
				assertEquals(1, di.getAnnexes().getAnnexe220());
				assertEquals(1, di.getAnnexes().getAnnexe230());
				assertEquals(1, di.getAnnexes().getAnnexe240());
				assertFalse(di.getAnnexes().isSetAnnexe310());
			}
		});
	}


	@Test
	public void testRemplitAnnexe320_Annexe_330() throws Exception {

		final int annee = 2005;

		// mise en place fiscale
		final long diId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique lui = addNonHabitant("Alain", "Dupont", date(1977, 2, 12), Sexe.MASCULIN);
			final PersonnePhysique elle = addNonHabitant("Maria", "Dupont", date(1953, 12, 18), Sexe.FEMININ);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, date(1990, 7, 3), null);
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, date(1990, 7, 3), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.VillarsSousYens);
			addSituation(mc, date(1990, 7, 3), null, 0, null, EtatCivil.MARIE);

			final PeriodeFiscale pf = addPeriodeFiscale(annee);
			final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, md);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, md);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, md);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, md);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_320, md);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_330, md);

			final DeclarationImpotOrdinairePP di = addDeclarationImpot(mc, pf, date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, md);
			di.setNumeroOfsForGestion(MockCommune.VillarsSousYens.getNoOFS());
			di.setDelaiRetourImprime(date(annee + 1, 7, 31));
			return di.getId();
		});

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final DeclarationImpotOrdinairePP declaration = (DeclarationImpotOrdinairePP) diDAO.get(diId);
				{
					final DI di = impressionDIPPHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration, null), buildDefaultAnnexes(declaration));
					assertEquals(1, di.getAnnexes().getAnnexe320().getNombre());
					assertEquals("N", di.getAnnexes().getAnnexe320().getAvecCourrierExplicatif());
					assertEquals(1, di.getAnnexes().getAnnexe330());
				}
			}
		});
	}

	/**
	 * [SIFISC-2367] Contribuables sans enfants
	 */
	@Test
	public void testCtbSansEnfant() throws Exception {

		final long diId = doInNewTransactionAndSession(status -> {
			final CollectiviteAdministrative aci = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noACI);

			// Crée une personne physique décédé
			final PersonnePhysique pp = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(pp, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Vevey);

			final PeriodeFiscale periode2011 = addPeriodeFiscale(2011);
			final ModeleDocument modele2011 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE, periode2011);
			final DeclarationImpotOrdinairePP declaration2011 = addDeclarationImpot(pp, periode2011, date(2011, 1, 1), date(2011, 4, 23), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.Vevey.getNoOFS());
			declaration2011.setRetourCollectiviteAdministrativeId(aci.getId());
			return declaration2011.getId();
		});

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final DeclarationImpotOrdinairePP declaration2011 = hibernateTemplate.get(DeclarationImpotOrdinairePP.class, diId);
				final DI di = impressionDIPPHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration2011, null), new ArrayList<>());
				assertNotNull(di);
				//Aucune structure enfants ne devrait apparaitre pour les ctb sans enfants
				assertNull(di.getEnfants());
			}
		});
	}

	/**
	 * [SIFISC-2367] Contribuables avec enfants
	 */
	@Test
	public void testCtbAvecEnfant() throws Exception {
		final long indPere = 2;
		final long indFils = 3;
		final long indFille = 4;
		final RegDate dateNaissanceFils = date(2000, 2, 8);
		final RegDate dateNaissanceFille = date(2005, 2, 8);

		// On crée la situation de départ : une mère, un père, un fils mineur et une fille majeur
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu pere = addIndividu(indPere, date(1960, 1, 1), "Cognac", "Guy", true);
				final MockIndividu fils = addIndividu(indFils, dateNaissanceFils, "Cognac", "Yvan", true);
				final MockIndividu fille = addIndividu(indFille, dateNaissanceFille, "Cognac", "Eva", false);

				addAdresse(pere, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);
				addAdresse(fils, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);
				addAdresse(fille, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, 1, null, date(1998, 1, 1), null);

				addLiensFiliation(fils, pere, null, fils.getDateNaissance(), null);
				addLiensFiliation(fille, pere, null, fille.getDateNaissance(), null);
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

				addParente(fils, pere, dateNaissanceFils, null);
				addParente(fille, pere, dateNaissanceFille, null);

				final CollectiviteAdministrative aci = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noACI);

				// Crée un for
				addForPrincipal(pere, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Vevey);

				final PeriodeFiscale periode2011 = addPeriodeFiscale(2011);
				final ModeleDocument modele2011 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE, periode2011);
				final DeclarationImpotOrdinairePP declaration2011 = addDeclarationImpot(pere, periode2011, date(2011, 1, 1), date(2011, 4, 23), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
				declaration2011.setNumeroOfsForGestion(MockCommune.Vevey.getNoOFS());
				declaration2011.setRetourCollectiviteAdministrativeId(aci.getId());

				return declaration2011.getId();
			}
		});

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final DeclarationImpotOrdinairePP di2011 = (DeclarationImpotOrdinairePP) diDAO.get(idDi2011);
				final DI di = impressionDIPPHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(di2011, null), new ArrayList<>());
				assertNotNull(di);

				assertNotNull(di.getEnfants());
				assertEquals(2, di.getEnfants().getEnfantArray().length);
			}
		});
	}

	@Test
	public void testZoneAffranchissement() throws Exception {

		final long diId = doInNewTransactionAndSession(status -> {
			final CollectiviteAdministrative aci = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noACI);

			// Crée une personne physique
			final PersonnePhysique pp = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(pp, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Vevey);

			final PeriodeFiscale periode2010 = addPeriodeFiscale(2010);
			final ModeleDocument modele2010 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE, periode2010);
			final DeclarationImpotOrdinairePP declaration2010 = addDeclarationImpot(pp, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.Vevey.getNoOFS());
			declaration2010.setRetourCollectiviteAdministrativeId(aci.getId());
			return declaration2010.getId();
		});

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final DeclarationImpotOrdinairePP declaration2010 = hibernateTemplate.get(DeclarationImpotOrdinairePP.class, diId);
				final TypFichierImpression.Document document =
						impressionDIPPHelper.remplitEditiqueSpecifiqueDI(new InformationsDocumentAdapter(declaration2010, null), TypFichierImpression.Factory.newInstance(),
						                                                 null, false);
				assertNotNull(document);
				assertEquals(ZoneAffranchissementEditique.INCONNU.getCode(), document.getInfoDocument().getAffranchissement().getZone());
			}
		});
	}

	@Test
	public void testInfosSurDI2011() throws Exception {

		final long diId = doInNewTransactionAndSession(status -> {
			final CollectiviteAdministrative aci = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noACI);

			// Crée une personne physique décédé
			final PersonnePhysique pp = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(pp, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Vevey);

			final PeriodeFiscale periode2011 = addPeriodeFiscale(2011);
			final ModeleDocument modele2011 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE, periode2011);
			final DeclarationImpotOrdinairePP declaration2011 = addDeclarationImpot(pp, periode2011, date(2011, 1, 1), date(2011, 4, 23), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.Vevey.getNoOFS());
			declaration2011.setRetourCollectiviteAdministrativeId(aci.getId());
			return declaration2011.getId();
		});

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final DeclarationImpotOrdinairePP declaration2011 = hibernateTemplate.get(DeclarationImpotOrdinairePP.class, diId);
				final DI di = impressionDIPPHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration2011, null), new ArrayList<>());
				assertNotNull(di);
				//le NIP doit être présent
				assertNotNull(di.getInfoDI().getNIP());
				assertEquals("D", di.getInfoDI().getCODETRAME());
			}
		});
	}

	@Test
	public void testInfosSurDI2010() throws Exception {

		final long diId = doInNewTransactionAndSession(status -> {
			final CollectiviteAdministrative aci = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noACI);

			// Crée une personne physique
			final PersonnePhysique pp = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(pp, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Vevey);

			final PeriodeFiscale periode2010 = addPeriodeFiscale(2010);
			final ModeleDocument modele2010 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE, periode2010);
			final DeclarationImpotOrdinairePP declaration2010 = addDeclarationImpot(pp, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.Vevey.getNoOFS());
			declaration2010.setRetourCollectiviteAdministrativeId(aci.getId());
			return declaration2010.getId();
		});

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final DeclarationImpotOrdinairePP declaration2010 = hibernateTemplate.get(DeclarationImpotOrdinairePP.class, diId);
				final DI di = impressionDIPPHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration2010, null), new ArrayList<>());
				assertNotNull(di);
				//le NIP ne doit pas être présent
				assertNull(di.getInfoDI().getNIP());
				//La valeur de  code trame à X
				assertEquals("X", di.getInfoDI().getCODETRAME());
			}
		});
	}

	@Test
	public void testCtbAvecEnfantAvant2011() throws Exception {
		final long indPere = 2;
		final long indFils = 3;
		final long indFille = 4;
		final RegDate dateNaissanceFils = date(2000, 2, 8);
		final RegDate dateNaissanceFille = date(2005, 2, 8);

		// On crée la situation de départ : une mère, un père, un fils mineur et une fille majeur
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu pere = addIndividu(indPere, date(1960, 1, 1), "Cognac", "Guy", true);
				MockIndividu fils = addIndividu(indFils, dateNaissanceFils, "Cognac", "Yvan", true);
				MockIndividu fille = addIndividu(indFille, dateNaissanceFille, "Cognac", "Eva", false);

				addAdresse(pere, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, null, null, date(1998, 1, 1), null);
				addAdresse(fils, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, null, null, date(1998, 1, 1), null);
				addAdresse(fille, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, null, null, date(1998, 1, 1), null);

				addLiensFiliation(fils, pere, null, fils.getDateNaissance(), null);
				addLiensFiliation(fille, pere, null, fille.getDateNaissance(), null);
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

				addParente(fils, pere, dateNaissanceFils, null);
				addParente(fille, pere, dateNaissanceFille, null);

				final CollectiviteAdministrative aci = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noACI);

				// Crée une for
				addForPrincipal(pere, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Vevey);

				final PeriodeFiscale periode2010 = addPeriodeFiscale(2010);
				final ModeleDocument modele2010 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE, periode2010);
				final DeclarationImpotOrdinairePP declaration2010 = addDeclarationImpot(pere, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
				declaration2010.setNumeroOfsForGestion(MockCommune.Vevey.getNoOFS());
				declaration2010.setRetourCollectiviteAdministrativeId(aci.getId());

				return declaration2010.getId();
			}
		});

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final DeclarationImpotOrdinairePP di2011 = (DeclarationImpotOrdinairePP) diDAO.get(idDi2011);
				final DI di = impressionDIPPHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(di2011, null), new ArrayList<>());
				assertNotNull(di);
				assertNull(di.getEnfants());
			}
		});
	}

	@Test
	public void testRemplitDiAvecCodeRegion() throws Exception {

		final long diId = doInNewTransactionAndSession(status -> {
			CollectiviteAdministrative cedi = tiersDAO.getCollectiviteAdministrativesByNumeroTechnique(ServiceInfrastructureRaw.noCEDI);

			// Crée une personne physique (ctb ordinaire vaudois) qui a déménagé début 2008 de Vallorbe à Bex
			final PersonnePhysique pp = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(pp, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Aigle);

			final PeriodeFiscale periode2011 = addPeriodeFiscale(2011);
			final ModeleDocument modele2011 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, periode2011);
			final DeclarationImpotOrdinairePP declaration2011 = addDeclarationImpot(pp, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.Aigle.getNoOFS());
			declaration2011.setRetourCollectiviteAdministrativeId(cedi.getId());
			return declaration2011.getId();
		});

		// L'expéditeur de la déclaration 2007 doit être Aigle (= OID responsable de Bex)
		//Selon UNIREG-3059:
		//L'OID doit être l'OID de gestion valable au 31.12 de l'année N-1 (N étant la période lors de laquel l'édition du document a lieu)
		//-> SAUF une exception : si la DI concerne la période fiscale courante (il s'agit d'une DI libre),
		// alors l'OID doit être l'OID de gestion courant du moment de l'édition du docuement.
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final DeclarationImpotOrdinairePP declaration2011 = hibernateTemplate.get(DeclarationImpotOrdinairePP.class, diId);
				final Tiers tiers = declaration2011.getTiers();

				// ... sur l'entête
				final InfoEnteteDocument entete = impressionDIPPHelper.remplitEnteteDocument(new InformationsDocumentAdapter(declaration2011, null));
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
				final DI di = impressionDIPPHelper.remplitSpecifiqueDI(new InformationsDocumentAdapter(declaration2011, null), new ArrayList<>());
				assertNotNull(di);
				final DI.InfoDI info = di.getInfoDI();
				assertNotNull(info);

				final String numCtb = String.format("%09d", tiers.getNumero());
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
		});
	}

	private static void validate(XmlObject document) {

		// Endroit où on va récupérer les éventuelles erreurs
		final XmlOptions validateOptions = new XmlOptions();
		final List<XmlError> errorList = new ArrayList<>();
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

		final PeriodeFiscale periode2010 = addPeriodeFiscale(2010);
		final ModeleDocument modele2010 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2010);

		final PeriodeFiscale periode2011 = addPeriodeFiscale(2011);
		final ModeleDocument modele2011 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2011);


		final CollectiviteAdministrative cedi = tiersDAO.getCollectiviteAdministrativesByNumeroTechnique(ServiceInfrastructureRaw.noCEDI);
		final long idCedi = cedi.getId();
		//   SI l'utilisateur choisit "OID" dans l'adresse de retour, il s'agira alors d'imprimer l'OID de gestion
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Aigle);

			final DeclarationImpotOrdinairePP declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.Aigle.getNoOFS());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010, null));
			assertEquals(1, codeOid2010.intValue());


			final DeclarationImpotOrdinairePP declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.Aigle.getNoOFS());
			CollectiviteAdministrative oidAigle = tiersDAO.getCollectiviteAdministrativesByNumeroTechnique(MockOfficeImpot.OID_AIGLE.getNoColAdm());
			declaration2011.setRetourCollectiviteAdministrativeId(oidAigle.getId());
			final Integer codeOid2011 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011, null));
			assertEquals(1, codeOid2011.intValue());

		}


		//TEST sur la récupération des oid de region


		//AIgle
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Aigle);

			final DeclarationImpotOrdinairePP declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.Aigle.getNoOFS());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010, null));
			assertEquals(1, codeOid2010.intValue());


			final DeclarationImpotOrdinairePP declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.Aigle.getNoOFS());
			declaration2011.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2011 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011, null));
			assertEquals(18, codeOid2011.intValue());

		}


		//Echallens
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Echallens);

			final DeclarationImpotOrdinairePP declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.Echallens.getNoOFS());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010, null));
			assertEquals(5, codeOid2010.intValue());


			final DeclarationImpotOrdinairePP declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.Echallens.getNoOFS());
			declaration2011.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2011 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011, null));
			assertEquals(19, codeOid2011.intValue());

		}


		//Grandson
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Grandson);

			final DeclarationImpotOrdinairePP declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.Grandson.getNoOFS());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010, null));
			assertEquals(6, codeOid2010.intValue());

			final DeclarationImpotOrdinairePP declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.Grandson.getNoOFS());
			declaration2011.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2011 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011, null));
			assertEquals(19, codeOid2011.intValue());

		}


		//Lausanne
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Lausanne);

			final DeclarationImpotOrdinairePP declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.Lausanne.getNoOFS());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010, null));
			assertEquals(7, codeOid2010.intValue());

			final DeclarationImpotOrdinairePP declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.Lausanne.getNoOFS());
			declaration2011.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2011 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011, null));
			assertEquals(7, codeOid2011.intValue());

		}

		//La vallée (L'abbaye)
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Fraction.LAbbaye);

			final DeclarationImpotOrdinairePP declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.Fraction.LAbbaye.getNoOFS());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010, null));
			assertEquals(8, codeOid2010.intValue());

			final DeclarationImpotOrdinairePP declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.Fraction.LAbbaye.getNoOFS());
			declaration2011.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2011 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011, null));
			assertEquals(19, codeOid2011.intValue());

		}


		//Lavaux
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Pully);

			final DeclarationImpotOrdinairePP declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.Pully.getNoOFS());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010, null));
			assertEquals(9, codeOid2010.intValue());

			final DeclarationImpotOrdinairePP declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.Pully.getNoOFS());
			declaration2011.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2011 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011, null));
			assertEquals(18, codeOid2011.intValue());

		}


		//Morges
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Morges);

			final DeclarationImpotOrdinairePP declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.Morges.getNoOFS());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010, null));
			assertEquals(10, codeOid2010.intValue());


			final DeclarationImpotOrdinairePP declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.Morges.getNoOFS());
			declaration2011.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2011 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011, null));
			assertEquals(12, codeOid2011.intValue());

		}


		//Moudon
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Moudon);

			final DeclarationImpotOrdinairePP declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.Moudon.getNoOFS());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010, null));
			assertEquals(11, codeOid2010.intValue());

			final DeclarationImpotOrdinairePP declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.Moudon.getNoOFS());
			declaration2011.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2011 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011, null));
			assertEquals(19, codeOid2011.intValue());

		}


		//Nyon
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Nyon);

			final DeclarationImpotOrdinairePP declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.Nyon.getNoOFS());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010, null));
			assertEquals(12, codeOid2010.intValue());

			final DeclarationImpotOrdinairePP declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.Nyon.getNoOFS());
			declaration2011.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2011 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011, null));
			assertEquals(12, codeOid2011.intValue());

		}


		//Orbe
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Orbe);

			final DeclarationImpotOrdinairePP declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.Orbe.getNoOFS());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010, null));
			assertEquals(13, codeOid2010.intValue());

			final DeclarationImpotOrdinairePP declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.Orbe.getNoOFS());
			declaration2011.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2011 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011, null));
			assertEquals(19, codeOid2011.intValue());

		}


		//Payerne
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.GrangesMarnand);

			final DeclarationImpotOrdinairePP declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.GrangesMarnand.getNoOFS());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010, null));
			assertEquals(15, codeOid2010.intValue());

			final DeclarationImpotOrdinairePP declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.GrangesMarnand.getNoOFS());
			declaration2011.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2011 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011, null));
			assertEquals(19, codeOid2011.intValue());

		}


		//Pays d'Enhaut
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.ChateauDoex);

			final DeclarationImpotOrdinairePP declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.ChateauDoex.getNoOFS());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010, null));
			assertEquals(16, codeOid2010.intValue());

			final DeclarationImpotOrdinairePP declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.ChateauDoex.getNoOFS());
			declaration2011.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2011 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011, null));
			assertEquals(18, codeOid2011.intValue());

		}


		//Rolle - Aubonne
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Aubonne);

			final DeclarationImpotOrdinairePP declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.Aubonne.getNoOFS());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010, null));
			assertEquals(17, codeOid2010.intValue());

			final DeclarationImpotOrdinairePP declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.Aubonne.getNoOFS());
			declaration2011.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2011 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011, null));
			assertEquals(12, codeOid2011.intValue());

		}


		//Vevey
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Vevey);


			final DeclarationImpotOrdinairePP declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.Vevey.getNoOFS());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010, null));
			assertEquals(18, codeOid2010.intValue());


			final DeclarationImpotOrdinairePP declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.Vevey.getNoOFS());
			declaration2011.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2011 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011, null));
			assertEquals(18, codeOid2011.intValue());

		}


		//Yverdon
		{
			final PersonnePhysique personnePhysique = addNonHabitant("Julien", "Glayre", date(1975, 1, 1), Sexe.MASCULIN);
			addForPrincipal(personnePhysique, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.YverdonLesBains);

			final DeclarationImpotOrdinairePP declaration2010 = addDeclarationImpot(personnePhysique, periode2010, date(2010, 1, 1), date(2010, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2010);
			declaration2010.setNumeroOfsForGestion(MockCommune.YverdonLesBains.getNoOFS());
			declaration2010.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2010 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2010, null));
			assertEquals(19, codeOid2010.intValue());


			final DeclarationImpotOrdinairePP declaration2011 = addDeclarationImpot(personnePhysique, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2011);
			declaration2011.setNumeroOfsForGestion(MockCommune.YverdonLesBains.getNoOFS());
			declaration2011.setRetourCollectiviteAdministrativeId(idCedi);
			final Integer codeOid2011 = impressionDIPPHelper.getNumeroOfficeImpotRetour(new InformationsDocumentAdapter(declaration2011, null));
			assertEquals(19, codeOid2011.intValue());

		}


	}

	@Test
	public void testIDEnvoiHorsSuisseRueOuLocaliteInconnue() throws Exception {

		final long diId = doInNewTransactionAndSession(status -> {
			final CollectiviteAdministrative aci = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noACI);

			// Crée une personne physique (ctb ordinaire vaudois) qui a déménagé mi 2010 de Morges à Paris
			final PersonnePhysique pp = addNonHabitant("Céline", "André", date(1980, 6, 23), Sexe.MASCULIN);
			addForPrincipal(pp, date(2006, 1, 1), MotifFor.ARRIVEE_HS, date(2010, 6, 30), MotifFor.DEPART_HS, MockCommune.Morges);
			addAdresseEtrangere(pp, TypeAdresseTiers.COURRIER, date(2010, 7, 1), null, null, null, MockPays.Danemark);

			final String numCtb = String.format("%09d", pp.getNumero());

			final PeriodeFiscale periode2012 = addPeriodeFiscale(2012);
			final ModeleDocument modele2012 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, periode2012);
			final DeclarationImpotOrdinairePP declaration2012 = addDeclarationImpot(pp, periode2012, date(2012, 1, 1), date(2012, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2012);
			declaration2012.setNumeroOfsForGestion(MockCommune.Morges.getNoOFS());
			declaration2012.setRetourCollectiviteAdministrativeId(aci.getId());
			return declaration2012.getId();
		});

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final DeclarationImpotOrdinairePP declaration2012 = hibernateTemplate.get(DeclarationImpotOrdinairePP.class, diId);
				final TypFichierImpression.Document document= impressionDIPPHelper.remplitEditiqueSpecifiqueDI(new InformationsDocumentAdapter(declaration2012, null),TypFichierImpression.Factory.newInstance(),
				                                                                                             null, false);
				assertNotNull(document);
				assertEquals(ZoneAffranchissementEditique.INCONNU.getCode(), document.getInfoDocument().getAffranchissement().getZone());
				assertEquals("10",document.getInfoDocument().getIdEnvoi());
			}
		});
	}

	@Test
	public void testIDEnvoiHorsSuissePaysInconnu() throws Exception {
		LOGGER.debug("EditiqueHelperTest - testIDEnvoiHorsSuissePaysInconnue SIFISC-4146");

		final long diId = doInNewTransactionAndSession(status -> {
			final CollectiviteAdministrative aci = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noACI);

			// Crée une personne physique (ctb ordinaire vaudois) qui a déménagé mi 2010 de Morges à Paris
			final PersonnePhysique pp = addNonHabitant("Céline", "André", date(1980, 6, 23), Sexe.MASCULIN);
			addForPrincipal(pp, date(2006, 1, 1), MotifFor.ARRIVEE_HS, date(2010, 6, 30), MotifFor.DEPART_HS, MockCommune.Morges);
			addAdresseEtrangere(pp, TypeAdresseTiers.COURRIER, date(2010, 7, 1), null, null, null, MockPays.PaysInconnu);

			final PeriodeFiscale periode2012 = addPeriodeFiscale(2012);
			final ModeleDocument modele2012 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, periode2012);
			final DeclarationImpotOrdinairePP declaration2012 = addDeclarationImpot(pp, periode2012, date(2012, 1, 1), date(2012, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2012);
			declaration2012.setNumeroOfsForGestion(MockCommune.Morges.getNoOFS());
			declaration2012.setRetourCollectiviteAdministrativeId(aci.getId());
			return declaration2012.getId();
		});

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final DeclarationImpotOrdinairePP declaration2012 = hibernateTemplate.get(DeclarationImpotOrdinairePP.class, diId);
				final TypFichierImpression.Document document= impressionDIPPHelper.remplitEditiqueSpecifiqueDI(new InformationsDocumentAdapter(declaration2012, null),TypFichierImpression.Factory.newInstance(),
				                                                                                             null, false);
				assertNotNull(document);
				assertEquals(ZoneAffranchissementEditique.INCONNU.getCode(), document.getInfoDocument().getAffranchissement().getZone());
				assertEquals("10",document.getInfoDocument().getIdEnvoi());
			}
		});
	}

	@Test
	public void testAvecEtiquette() throws Exception {

		final long diId = doInNewTransactionAndSession(status -> {
			final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noCEDI);
			assertNotNull(cedi);

			// Crée une personne physique (ctb ordinaire vaudois) qui a déménagé mi 2010 de Morges à Paris
			final PersonnePhysique pp = addNonHabitant("Céline", "André", date(1980, 6, 23), Sexe.MASCULIN);
			addForPrincipal(pp, date(2006, 1, 1), MotifFor.ARRIVEE_HS, date(2010, 6, 30), MotifFor.DEPART_HS, MockCommune.Morges);
			addAdresseEtrangere(pp, TypeAdresseTiers.COURRIER, date(2010, 7, 1), null, null, null, MockPays.PaysInconnu);

			final PeriodeFiscale periode2012 = addPeriodeFiscale(2012);
			final ModeleDocument modele2012 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL, periode2012);
			final DeclarationImpotOrdinairePP declaration2012 = addDeclarationImpot(pp, periode2012, date(2012, 1, 1), date(2012, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2012);
			declaration2012.setNumeroOfsForGestion(MockCommune.Morges.getNoOFS());
			declaration2012.setRetourCollectiviteAdministrativeId(cedi.getId());

			// étiquette qui va bien
			final Etiquette collaborateur = etiquetteService.getEtiquette(CODE_ETIQUETTE_COLLABORATEUR);
			assertNotNull(collaborateur);
			addEtiquetteTiers(collaborateur, pp, date(RegDate.get().year(), 1, 1), null);
			return declaration2012.getId();
		});

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final DeclarationImpotOrdinairePP declaration2012 = hibernateTemplate.get(DeclarationImpotOrdinairePP.class, diId);
				final TypFichierImpression.Document document= impressionDIPPHelper.remplitEditiqueSpecifiqueDI(new InformationsDocumentAdapter(declaration2012, null),
				                                                                                               TypFichierImpression.Factory.newInstance(),
				                                                                                               null,
				                                                                                               false);
				assertNotNull(document);
				assertEquals(String.valueOf(MockOfficeImpot.OID_MORGES.getNoColAdm()), document.getInfoDocument().getIdEnvoi());
				assertEquals(String.format("CEDI %d", MockCollectiviteAdministrative.noNouvelleEntite), document.getDI().getAdresseRetour().getADRES3RETOUR());
				assertEquals(String.format("%d-0", MockCollectiviteAdministrative.noNouvelleEntite), document.getDI().getInfoDI().getNOOID());

				assertEquals(MockOfficeImpot.OID_MORGES.getNomComplet1(), document.getInfoEnteteDocument().getExpediteur().getAdresse().getAdresseCourrierLigne1());
				assertEquals(MockOfficeImpot.OID_MORGES.getNomComplet2(), document.getInfoEnteteDocument().getExpediteur().getAdresse().getAdresseCourrierLigne2());
			}
		});

	}
}
