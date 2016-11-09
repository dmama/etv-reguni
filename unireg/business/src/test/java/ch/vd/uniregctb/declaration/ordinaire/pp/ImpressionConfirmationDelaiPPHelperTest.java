package ch.vd.uniregctb.declaration.ordinaire.pp;

import java.util.GregorianCalendar;

import noNamespace.FichierImpressionDocument;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.interfaces.infra.mock.MockCollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.etiquette.Etiquette;
import ch.vd.uniregctb.etiquette.EtiquetteService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatDelaiDeclaration;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;

public class ImpressionConfirmationDelaiPPHelperTest extends BusinessTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImpressionDeclarationImpotPersonnesPhysiquesHelperTest.class);

	private ImpressionConfirmationDelaiPPHelperImpl impressionConfirmationDelaiHelper;
	private EtiquetteService etiquetteService;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		impressionConfirmationDelaiHelper = getBean(ImpressionConfirmationDelaiPPHelperImpl.class, "impressionConfirmationDelaiPPHelper");
		etiquetteService = getBean(EtiquetteService.class, "etiquetteService");
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testConstruitIdArchivageDocument() throws Exception {
		LOGGER.debug("ImpressionConfirmationDelaiHelperTest - testConstruitIdArchivageDocument");
		String idArchivageAttendu = "325483 Confirmation Delai  0101123020000";

		DeclarationImpotOrdinairePP declaration = new DeclarationImpotOrdinairePP();
		DelaiDeclaration delai = new DelaiDeclaration();
		delai.setEtat(EtatDelaiDeclaration.ACCORDE);
		delai.setDeclaration(declaration);
		delai.setDelaiAccordeAu(date(2011, 9, 21));
		delai.setId(84512325483L);
		declaration.setNumero(Integer.valueOf(2));
		PeriodeFiscale periodeFiscale = new PeriodeFiscale();
		periodeFiscale.setAnnee(Integer.valueOf(2010));
		declaration.setPeriode(periodeFiscale);
		GregorianCalendar cal = new GregorianCalendar(2011, 0, 1, 12, 30, 20);
		delai.setLogCreationDate(cal.getTime());
		ImpressionConfirmationDelaiHelperParams params = new ImpressionConfirmationDelaiHelperParams(declaration, delai.getDelaiAccordeAu(),
				"userTest", "", "", delai.getId(), delai.getLogCreationDate());

		Assert.assertEquals(idArchivageAttendu, impressionConfirmationDelaiHelper.construitIdArchivageDocument(params));

	}

	@Test
	public void testExpediteurSansEtiquette() throws Exception {

		final int annee = 2015;

		// mise en place
		final long diId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final PersonnePhysique pp = addNonHabitant("Francis", "Orange", null, Sexe.MASCULIN);
				addForPrincipal(pp, date(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
				final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp, pf, date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, RegDate.get().addMonths(-1));
				addDelaiDeclaration(di, RegDate.get(), date(annee + 1, 6, 30), EtatDelaiDeclaration.ACCORDE);

				return di.getId();
			}
		});

		// demande d'impression de confirmation de délai
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final DeclarationImpotOrdinairePP di = hibernateTemplate.get(DeclarationImpotOrdinairePP.class, diId);
				final DelaiDeclaration delai = addDelaiDeclaration(di, RegDate.get(), date(annee + 1, 9, 30), EtatDelaiDeclaration.ACCORDE);

				final ImpressionConfirmationDelaiHelperParams params = new ImpressionConfirmationDelaiHelperParams(di, RegDate.get(), "MOI", "0213160000", null, delai.getId(), delai.getLogCreationDate());
				final FichierImpressionDocument doc = impressionConfirmationDelaiHelper.remplitConfirmationDelai(params, "TOTO");
				Assert.assertNotNull(doc);

				Assert.assertEquals(MockCollectiviteAdministrative.CAT.getNomComplet1(), doc.getFichierImpression().getDocumentArray(0).getInfoEnteteDocument().getExpediteur().getAdresse().getAdresseCourrierLigne1());
				Assert.assertEquals(MockCollectiviteAdministrative.CAT.getNomComplet2(), doc.getFichierImpression().getDocumentArray(0).getInfoEnteteDocument().getExpediteur().getAdresse().getAdresseCourrierLigne2());
			}
		});
	}

	@Test
	public void testExpediteurAvecEtiquette() throws Exception {

		final int annee = 2015;

		// mise en place
		final long diId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final PersonnePhysique pp = addNonHabitant("Francis", "Orange", null, Sexe.MASCULIN);
				addForPrincipal(pp, date(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Echallens);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
				final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp, pf, date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, RegDate.get().addMonths(-1));
				addDelaiDeclaration(di, RegDate.get(), date(annee + 1, 6, 30), EtatDelaiDeclaration.ACCORDE);

				final Etiquette collaborateur = etiquetteService.getEtiquette(CODE_ETIQUETTE_COLLABORATEUR);
				Assert.assertNotNull(collaborateur);
				addEtiquetteTiers(collaborateur, pp, date(annee + 1, 6, 1), null);

				return di.getId();
			}
		});

		// demande d'impression de confirmation de délai
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final DeclarationImpotOrdinairePP di = hibernateTemplate.get(DeclarationImpotOrdinairePP.class, diId);
				final DelaiDeclaration delai = addDelaiDeclaration(di, RegDate.get(), date(annee + 1, 9, 30), EtatDelaiDeclaration.ACCORDE);

				final ImpressionConfirmationDelaiHelperParams params = new ImpressionConfirmationDelaiHelperParams(di, RegDate.get(), "MOI", "0213160000", null, delai.getId(), delai.getLogCreationDate());
				final FichierImpressionDocument doc = impressionConfirmationDelaiHelper.remplitConfirmationDelai(params, "TOTO");
				Assert.assertNotNull(doc);

				Assert.assertEquals(MockCollectiviteAdministrative.ACI_NOUVELLE_ENTITE.getNomComplet1(), doc.getFichierImpression().getDocumentArray(0).getInfoEnteteDocument().getExpediteur().getAdresse().getAdresseCourrierLigne1());
				Assert.assertEquals(MockCollectiviteAdministrative.ACI_NOUVELLE_ENTITE.getNomComplet2(), doc.getFichierImpression().getDocumentArray(0).getInfoEnteteDocument().getExpediteur().getAdresse().getAdresseCourrierLigne2());
			}
		});
	}

}
