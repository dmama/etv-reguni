package ch.vd.unireg.editique.impl;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import noNamespace.impl.FichierImpressionDocumentImpl;
import org.apache.cxf.common.xmlschema.XmlSchemaUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.xmlbeans.XmlObject;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.ordinaire.pm.ImpressionDeclarationImpotPersonnesMoralesHelper;
import ch.vd.unireg.declaration.ordinaire.pm.ImpressionLettreDecisionDelaiPMHelper;
import ch.vd.unireg.declaration.ordinaire.pm.ImpressionSommationDeclarationImpotPersonnesMoralesHelper;
import ch.vd.unireg.declaration.ordinaire.pp.ImpressionConfirmationDelaiPPHelper;
import ch.vd.unireg.declaration.ordinaire.pp.ImpressionDeclarationImpotPersonnesPhysiquesHelper;
import ch.vd.unireg.declaration.ordinaire.pp.ImpressionSommationDeclarationImpotPersonnesPhysiquesHelper;
import ch.vd.unireg.declaration.source.ImpressionListeRecapHelper;
import ch.vd.unireg.declaration.source.ImpressionSommationLRHelper;
import ch.vd.unireg.editique.EditiqueAbstractHelperImpl;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiqueResultat;
import ch.vd.unireg.editique.FormatDocumentEditique;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.editique.mock.MockEditiqueService;
import ch.vd.unireg.efacture.ImpressionDocumentEfactureHelperImpl;
import ch.vd.unireg.evenement.docsortant.EvenementDocumentSortantService;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.interfaces.service.ServiceSecuriteService;
import ch.vd.unireg.mouvement.ImpressionBordereauMouvementDossierHelper;
import ch.vd.unireg.tache.ImpressionNouveauxDossiersHelper;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;

import static ch.vd.unireg.editique.EditiqueAbstractHelperImpl.CAT_NOM_SERVICE_EXPEDITEUR;
import static ch.vd.unireg.editique.EditiqueAbstractHelperImpl.CAT_TRAITE_PAR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EditiqueCompositionServiceTest extends BusinessTest {

	private EditiqueCompositionServiceImpl service;

	private ImpressionConfirmationDelaiPPHelper impressionConfirmationDelaiPPHelper;
	private final String nomDocument = "12321123221L";

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		service = new EditiqueCompositionServiceImpl();
		service.setEditiqueService(new MockEditiqueService());
		service.setImpressionDIPPHelper(getBean(ImpressionDeclarationImpotPersonnesPhysiquesHelper.class, "impressionDIPPHelper"));
		service.setImpressionDIPMHelper(getBean(ImpressionDeclarationImpotPersonnesMoralesHelper.class, "impressionDIPMHelper"));
		service.setImpressionLRHelper(getBean(ImpressionListeRecapHelper.class, "impressionLRHelper"));
		service.setImpressionSommationDIPPHelper(getBean(ImpressionSommationDeclarationImpotPersonnesPhysiquesHelper.class, "impressionSommationDIPPHelper"));
		service.setImpressionSommationDIPMHelper(getBean(ImpressionSommationDeclarationImpotPersonnesMoralesHelper.class, "impressionSommationDIPMHelper"));
		service.setImpressionSommationLRHelper(getBean(ImpressionSommationLRHelper.class, "impressionSommationLRHelper"));
		service.setImpressionNouveauxDossiersHelper(getBean(ImpressionNouveauxDossiersHelper.class, "impressionNouveauxDossiersHelper"));
		service.setImpressionLettreDecisionDelaiPMHelper(getBean(ImpressionLettreDecisionDelaiPMHelper.class, "impressionLettreDecisionDelaiPMHelper"));
		service.setImpressionBordereauMouvementDossierHelper(getBean(ImpressionBordereauMouvementDossierHelper.class, "impressionBordereauMouvementDossierHelper"));
		service.setServiceSecurite(getBean(ServiceSecuriteService.class, "serviceSecuriteService"));
		service.setImpressionEfactureHelper(getBean(ImpressionDocumentEfactureHelperImpl.class, "impressionEfactureHelper"));
		service.setEvenementDocumentSortantService(getBean(EvenementDocumentSortantService.class, "evenementDocumentSortantService"));
		service.setInfraService(getBean(ServiceInfrastructureService.class, "serviceInfrastructureService"));
		//
		impressionConfirmationDelaiPPHelper = Mockito.spy(getBean(ImpressionConfirmationDelaiPPHelper.class, "impressionConfirmationDelaiPPHelper"));
		service.setImpressionConfirmationDelaiPPHelper(impressionConfirmationDelaiPPHelper);

	}

	@Test
	public void testFicherImpressionVersionPM() throws Exception {
		final ClassPathResource classPathResource = new ClassPathResource("editique/UniregPM_FichierImpression.xsd");
		final InputStream is = classPathResource.getInputStream();
		final XmlSchemaCollection schemaCol = new XmlSchemaCollection();
		final XmlSchema schema = schemaCol.read(new StreamSource(is));
		final List<String> sTypeVersionXSD = XmlSchemaUtils.enumeratorValues((XmlSchemaSimpleType) schema.getTypeByName("STypeVersionXSD"));
		Assert.assertEquals(1, sTypeVersionXSD.size());
		Assert.assertEquals("Attention la version Editique UNIREG diffère de celle de la xsd PM!", sTypeVersionXSD.get(0), EditiqueAbstractHelperImpl.VERSION_XSD_PM);
	}

	@Test
	public void testFicherImpressionVersionPP() throws Exception {
		final ClassPathResource classPathResource = new ClassPathResource("editique/UniregPP_FichierImpression.xsd");
		final InputStream is = classPathResource.getInputStream();
		final XmlSchemaCollection schemaCol = new XmlSchemaCollection();
		final XmlSchema schema = schemaCol.read(new StreamSource(is));
		final List<String> sTypeVersionXSD = XmlSchemaUtils.enumeratorValues((XmlSchemaSimpleType) schema.getTypeByName("STypeVersionXSD"));
		Assert.assertEquals(1, sTypeVersionXSD.size());
		Assert.assertEquals("Attention la version Editique UNIREG diffère de celle de la xsd PP!", sTypeVersionXSD.get(0), EditiqueAbstractHelperImpl.VERSION_XSD_PP);
	}

	/**
	 * SIFISC-9875 : il n'est plus possible d'envoyer une DI en mode batch...
	 */
	@Test
	public void testImprimeDIForBatch() throws Exception {

		final long noIndividu = 7423895678L;
		final RegDate dateNaissance = date(1984, 4, 12);
		final RegDate dateOuvertureFor = dateNaissance.addYears(18);
		final int annee = 2012;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Sorel", "Julien", Sexe.MASCULIN);
				addNationalite(individu, MockPays.Suisse, dateNaissance, null);
				addAdresse(individu, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateNaissance, null);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateNaissance, null);
			}
		});

		final List<String> docsImprimes = new ArrayList<>();
		service.setEditiqueService(new MockEditiqueService() {
			@Override
			public void creerDocumentParBatch(String nomDocument, TypeDocumentEditique typeDocument, XmlObject document, boolean archive) throws EditiqueException {
				docsImprimes.add(String.format("%s-%s", typeDocument, nomDocument));
			}
		});

		// mise en place fiscale et impression de la DI
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateOuvertureFor, MotifFor.MAJORITE, MockCommune.Cossonay);
			final PeriodeFiscale pf = addPeriodeFiscale(annee);
			final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf);
			final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noCEDI);
			final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp, pf, date(annee, 1, 1), date(annee, 12, 31), cedi, TypeContribuable.VAUDOIS_ORDINAIRE, md);
			di.setNumeroOfsForGestion(MockCommune.Cossonay.getNoOFS());
			addEtatDeclarationEmise(di, RegDate.get());
			service.imprimeDIForBatch(di);          // <-- ça pêtait ici sur un "type de document non supporté: null";
			return pp.getNumero();
		});

		Assert.assertEquals(1, docsImprimes.size());

		final String doc = docsImprimes.get(0);
		Assert.assertTrue(doc, doc.startsWith(String.format("%s-%d 01 %09d", TypeDocumentEditique.DI_ORDINAIRE_COMPLETE, annee, ppId)));
	}

	/**
	 * SIFISC-31119 :Lettre de refus/accord de délai, expéditeur CAT.
	 */
	@Test
	public void testLettreAccordDelaiExpediteurCAT() throws Exception {

		final long noIndividu = 7423895678L;
		final RegDate dateNaissance = date(1984, 4, 12);
		final RegDate dateOuvertureFor = dateNaissance.addYears(18);
		final int annee = 2012;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Sorel", "Julien", Sexe.MASCULIN);
				addNationalite(individu, MockPays.Suisse, dateNaissance, null);
				addAdresse(individu, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateNaissance, null);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateNaissance, null);

			}
		});

		final List<String> docsImprimes = new ArrayList<>();
		final Map<String, XmlObject> documentEditique = new HashMap<>();
		service.setEditiqueService(new MockEditiqueService() {
			@Override
			public void creerDocumentParBatch(String nomDocument, TypeDocumentEditique typeDocument, XmlObject document, boolean archive) throws EditiqueException {
				docsImprimes.add(String.format("%s-%s", typeDocument, nomDocument));
			}

			@Override
			public EditiqueResultat creerDocumentImmediatementSynchroneOuInbox(String nomDocument, TypeDocumentEditique typeDocument, FormatDocumentEditique typeFormat, XmlObject document, boolean archive, String description) throws
					EditiqueException {
				documentEditique.putIfAbsent(nomDocument, document);
				return null;
			}
		});


		// mise en place fiscale et impression de la DI
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateOuvertureFor, MotifFor.MAJORITE, MockCommune.Cossonay);
			final PeriodeFiscale pf = addPeriodeFiscale(annee);
			final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf);
			final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noCEDI);
			final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp, pf, date(annee, 1, 1), date(annee, 12, 31), cedi, TypeContribuable.VAUDOIS_ORDINAIRE, md);
			di.setNumeroOfsForGestion(MockCommune.Cossonay.getNoOFS());
			addEtatDeclarationEmise(di, RegDate.get());
			//addDelaiDeclaration(di, RegDate.get(annee, 2, 15), RegDate.get(annee, 3, 31), EtatDelaiDocumentFiscal.ACCORDE).setAnnule(true);
			addDelaiDeclaration(di, RegDate.get(annee, 2, 15), RegDate.get(annee, 2, 28), EtatDelaiDocumentFiscal.ACCORDE);
			return pp.getNumero();
		});

		doInNewTransactionAndSession(status -> {
			Mockito.doReturn(nomDocument).when(impressionConfirmationDelaiPPHelper).construitIdDocument(ArgumentMatchers.any(DelaiDeclaration.class));

			final Tiers pp = tiersDAO.get(ppId);
			assertNotNull(pp);
			final List<DeclarationImpotOrdinairePP> list = pp.getDeclarationsDansPeriode(DeclarationImpotOrdinairePP.class, annee, false);
			final DeclarationImpotOrdinairePP di = list.get(0);
			assertNotNull(di);
			service.imprimeLettreDecisionDelaiOnline(di, (DelaiDeclaration) di.getDelais().iterator().next());
			return null;
		});

		Assert.assertEquals(1, documentEditique.size());
		final XmlObject document = documentEditique.get(nomDocument);
		assertNotNull(document);
		final String traitePar = ((FichierImpressionDocumentImpl) document).getFichierImpression().getDocumentArray(0).getInfoEnteteDocument().getExpediteur().getTraitePar();
		final String AdrCourrier = ((FichierImpressionDocumentImpl) document).getFichierImpression().getDocumentArray(0).getInfoEnteteDocument().getExpediteur().getSrvExp();
		assertEquals(CAT_TRAITE_PAR, traitePar);
		assertEquals(CAT_NOM_SERVICE_EXPEDITEUR, AdrCourrier);

	}
}
