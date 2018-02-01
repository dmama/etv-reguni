package ch.vd.uniregctb.editique.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.xmlbeans.XmlObject;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.ordinaire.pm.ImpressionDeclarationImpotPersonnesMoralesHelper;
import ch.vd.uniregctb.declaration.ordinaire.pm.ImpressionLettreDecisionDelaiPMHelper;
import ch.vd.uniregctb.declaration.ordinaire.pm.ImpressionSommationDeclarationImpotPersonnesMoralesHelper;
import ch.vd.uniregctb.declaration.ordinaire.pp.ImpressionConfirmationDelaiPPHelper;
import ch.vd.uniregctb.declaration.ordinaire.pp.ImpressionDeclarationImpotPersonnesPhysiquesHelper;
import ch.vd.uniregctb.declaration.ordinaire.pp.ImpressionSommationDeclarationImpotPersonnesPhysiquesHelper;
import ch.vd.uniregctb.declaration.source.ImpressionListeRecapHelper;
import ch.vd.uniregctb.declaration.source.ImpressionSommationLRHelper;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.editique.mock.MockEditiqueService;
import ch.vd.uniregctb.efacture.ImpressionDocumentEfactureHelperImpl;
import ch.vd.uniregctb.evenement.docsortant.EvenementDocumentSortantService;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;
import ch.vd.uniregctb.mouvement.ImpressionBordereauMouvementDossierHelper;
import ch.vd.uniregctb.tache.ImpressionNouveauxDossiersHelper;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;

public class EditiqueCompositionServiceTest extends BusinessTest {

	private EditiqueCompositionServiceImpl service;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		service = new EditiqueCompositionServiceImpl();
		service.setEditiqueService(new MockEditiqueService());
		service.setImpressionDIPPHelper(getBean(ImpressionDeclarationImpotPersonnesPhysiquesHelper.class, "impressionDIPPHelper"));
		service.setImpressionDIPMHelper(getBean(ImpressionDeclarationImpotPersonnesMoralesHelper.class, "impressionDIPMHelper"));
		service.setImpressionLRHelper(getBean(ImpressionListeRecapHelper.class, "impressionLRHelper"));
		service.setImpressionSommationDIPPHelper(getBean(ImpressionSommationDeclarationImpotPersonnesPhysiquesHelper.class, "impressionSommationDIPPHelper"));
		service.setImpressionSommationDIPMHelper(getBean(ImpressionSommationDeclarationImpotPersonnesMoralesHelper.class, "impressionSommationDIPMHelper"));
		service.setImpressionSommationLRHelper(getBean(ImpressionSommationLRHelper.class, "impressionSommationLRHelper"));
		service.setImpressionNouveauxDossiersHelper(getBean(ImpressionNouveauxDossiersHelper.class, "impressionNouveauxDossiersHelper"));
		service.setImpressionConfirmationDelaiPPHelper(getBean(ImpressionConfirmationDelaiPPHelper.class, "impressionConfirmationDelaiPPHelper"));
		service.setImpressionLettreDecisionDelaiPMHelper(getBean(ImpressionLettreDecisionDelaiPMHelper.class, "impressionLettreDecisionDelaiPMHelper"));
		service.setImpressionBordereauMouvementDossierHelper(getBean(ImpressionBordereauMouvementDossierHelper.class, "impressionBordereauMouvementDossierHelper"));
		service.setServiceSecurite(getBean(ServiceSecuriteService.class, "serviceSecuriteService"));
		service.setImpressionEfactureHelper(getBean(ImpressionDocumentEfactureHelperImpl.class, "impressionEfactureHelper"));
		service.setEvenementDocumentSortantService(getBean(EvenementDocumentSortantService.class, "evenementDocumentSortantService"));
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
		final long ppId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateOuvertureFor, MotifFor.MAJORITE, MockCommune.Cossonay);
				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf);
				final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noCEDI);
				final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp, pf, date(annee, 1, 1), date(annee, 12, 31), cedi, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				di.setNumeroOfsForGestion(MockCommune.Cossonay.getNoOFS());
				addEtatDeclarationEmise(di, RegDate.get());
				service.imprimeDIForBatch(di);          // <-- ça pêtait ici sur un "type de document non supporté: null"
				return pp.getNumero();
			}
		});

		Assert.assertEquals(1, docsImprimes.size());

		final String doc = docsImprimes.get(0);
		Assert.assertTrue(doc, doc.startsWith(String.format("%s-%d 01 %09d", TypeDocumentEditique.DI_ORDINAIRE_COMPLETE, annee, ppId)));
	}
}
