package ch.vd.uniregctb.declaration.ordinaire;

import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.interfaces.model.mock.MockCollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class EnvoiSommationDIsProcessorTest extends BusinessTest {

	private EnvoiSommationsDIsProcessor processor;
	private DeclarationImpotOrdinaireDAO diDao;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		final DelaisService delaisService = getBean(DelaisService.class, "delaisService");
		final DeclarationImpotService diService = getBean(DeclarationImpotService.class, "diService");
		diDao = getBean(DeclarationImpotOrdinaireDAO.class, "diDAO");
		processor = new EnvoiSommationsDIsProcessor(hibernateTemplate, diDao, delaisService, diService, transactionManager);
	}

	@Test
	public void testDiRetournee() throws Exception {

		final long diId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {

				addCollAdm(MockCollectiviteAdministrative.CEDI);

				final PersonnePhysique pp = addNonHabitant("Jacques", "Cartier", RegDate.get(1980, 1, 5), Sexe.MASCULIN);
				addForPrincipal(pp, RegDate.get(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Aubonne);

				final RegDate dateEmission = RegDate.get(2009, 1, 15);
				final RegDate dateDelaiInitial = RegDate.get(2009, 3, 15);
				final PeriodeFiscale periode = addPeriodeFiscale(2008);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(pp, periode, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				declaration.addEtat(new EtatDeclaration(dateEmission, TypeEtatDeclaration.EMISE));
				declaration.addEtat(new EtatDeclaration(dateDelaiInitial.addDays(5), TypeEtatDeclaration.RETOURNEE));   // oui, le retour est après le délai initial, mais cela ne doit pas avoir d'influence

				final DelaiDeclaration delai = new DelaiDeclaration();
				delai.setDateDemande(dateEmission);
				delai.setDelaiAccordeAu(dateDelaiInitial);
				declaration.addDelai(delai);

				return declaration.getId();
			}
		});

		final EnvoiSommationsDIsResults results = processor.run(RegDate.get(), false, 0, null);
		Assert.assertEquals("La DI n'aurait même pas dû être vue", 0, results.getTotalDisTraitees());

		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalSommations(2008));
		Assert.assertEquals(0, results.getTotalIndigent());
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisOptionnelles());
	}

	@Test
	public void testDiPasEncoreSommable() throws Exception {

		final int anneePf = 2008;
		final RegDate dateEmission = RegDate.get(2009, 1, 15);
		final RegDate delaiInitial = RegDate.get(2009, 3, 15);

		final long diId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {

				addCollAdm(MockCollectiviteAdministrative.CEDI);

				final PersonnePhysique pp = addNonHabitant("Jacques", "Cartier", RegDate.get(1980, 1, 5), Sexe.MASCULIN);
				addForPrincipal(pp, RegDate.get(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Aubonne);

				final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(pp, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				declaration.addEtat(new EtatDeclaration(dateEmission, TypeEtatDeclaration.EMISE));

				final DelaiDeclaration delai = new DelaiDeclaration();
				delai.setDateDemande(dateEmission);
				delai.setDelaiAccordeAu(delaiInitial);
				declaration.addDelai(delai);

				return declaration.getId();
			}
		});

		final RegDate dateTraitement = delaiInitial.addDays(5);
		final EnvoiSommationsDIsResults results = processor.run(dateTraitement, false, 0, null);
		Assert.assertEquals(0, results.getTotalDisTraitees());
		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalIndigent());
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisOptionnelles());
	}

	@Test
	public void testDiSommableSimple() throws Exception {

		final int anneePf = 2008;
		final RegDate dateEmission = RegDate.get(2009, 1, 15);
		final RegDate delaiInitial = RegDate.get(2009, 3, 15);

		final long diId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {

				addCollAdm(MockCollectiviteAdministrative.CEDI);

				final PersonnePhysique pp = addNonHabitant("Jacques", "Cartier", RegDate.get(1980, 1, 5), Sexe.MASCULIN);
				addForPrincipal(pp, RegDate.get(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Aubonne);

				final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(pp, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				declaration.addEtat(new EtatDeclaration(dateEmission, TypeEtatDeclaration.EMISE));

				final DelaiDeclaration delai = new DelaiDeclaration();
				delai.setDateDemande(dateEmission);
				delai.setDelaiAccordeAu(delaiInitial);
				declaration.addDelai(delai);

				return declaration.getId();
			}
		});

		final RegDate dateTraitement = delaiInitial.addMonths(1);
		final EnvoiSommationsDIsResults results = processor.run(dateTraitement, false, 0, null);
		Assert.assertEquals(1, results.getTotalDisTraitees());
		Assert.assertEquals(1, results.getTotalDisSommees());
		Assert.assertEquals(1, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalIndigent());
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisOptionnelles());
	}

	@Test
	public void testDiSommableMaisIndigent() throws Exception {
		final int anneePf = 2008;
		final RegDate dateEmission = RegDate.get(2009, 1, 15);
		final RegDate delaiInitial = RegDate.get(2009, 3, 15);

		final long diId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {

				addCollAdm(MockCollectiviteAdministrative.CEDI);

				final PersonnePhysique pp = addNonHabitant("Jacques", "Cartier", RegDate.get(1980, 1, 5), Sexe.MASCULIN);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, RegDate.get(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Aubonne);
				ffp.setModeImposition(ModeImposition.INDIGENT);

				final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(pp, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				declaration.addEtat(new EtatDeclaration(dateEmission, TypeEtatDeclaration.EMISE));

				final DelaiDeclaration delai = new DelaiDeclaration();
				delai.setDateDemande(dateEmission);
				delai.setDelaiAccordeAu(delaiInitial);
				declaration.addDelai(delai);

				return declaration.getId();
			}
		});

		final RegDate dateTraitement = delaiInitial.addMonths(1);
		final EnvoiSommationsDIsResults results = processor.run(dateTraitement, false, 0, null);
		Assert.assertEquals(1, results.getTotalDisTraitees());
		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalSommations(anneePf));
		Assert.assertEquals(1, results.getTotalIndigent());
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisOptionnelles());
	}

	@Test
	public void testDiSommableMaisNonAssujetti() throws Exception {
		final int anneePf = 2008;
		final RegDate dateEmission = RegDate.get(2009, 1, 15);
		final RegDate delaiInitial = RegDate.get(2009, 3, 15);

		final long diId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {

				addCollAdm(MockCollectiviteAdministrative.CEDI);

				final PersonnePhysique pp = addNonHabitant("Jacques", "Cartier", RegDate.get(1980, 1, 5), Sexe.MASCULIN);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, RegDate.get(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Aubonne);
				ffp.setAnnule(true);

				final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(pp, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				declaration.addEtat(new EtatDeclaration(dateEmission, TypeEtatDeclaration.EMISE));

				final DelaiDeclaration delai = new DelaiDeclaration();
				delai.setDateDemande(dateEmission);
				delai.setDelaiAccordeAu(delaiInitial);
				declaration.addDelai(delai);

				return declaration.getId();
			}
		});

		final RegDate dateTraitement = delaiInitial.addMonths(1);
		final EnvoiSommationsDIsResults results = processor.run(dateTraitement, false, 0, null);
		Assert.assertEquals(1, results.getTotalDisTraitees());
		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalIndigent());
		Assert.assertEquals(0, results.getTotalSourcierPur());
		Assert.assertEquals(1, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisOptionnelles());
	}

	@Test
	public void testDiSommableMaisOptionnelle() throws Exception {
		final int anneePf = 2008;
		final RegDate dateEmission = RegDate.get(2009, 1, 15);
		final RegDate delaiInitial = RegDate.get(2009, 3, 15);

		final long diId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {

				addCollAdm(MockCollectiviteAdministrative.CEDI);

				final PersonnePhysique pp = addNonHabitant("Jacques", "Cartier", RegDate.get(1980, 1, 5), Sexe.MASCULIN);
				addForPrincipal(pp, RegDate.get(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockPays.France);
				addForSecondaire(pp, RegDate.get(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

				final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(pp, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				declaration.addEtat(new EtatDeclaration(dateEmission, TypeEtatDeclaration.EMISE));

				final DelaiDeclaration delai = new DelaiDeclaration();
				delai.setDateDemande(dateEmission);
				delai.setDelaiAccordeAu(delaiInitial);
				declaration.addDelai(delai);

				return declaration.getId();
			}
		});

		final RegDate dateTraitement = delaiInitial.addMonths(1);
		final EnvoiSommationsDIsResults results = processor.run(dateTraitement, false, 0, null);
		Assert.assertEquals(1, results.getTotalDisTraitees());
		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalIndigent());
		Assert.assertEquals(0, results.getTotalSourcierPur());
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(1, results.getTotalDisOptionnelles());
	}

	@Test
	public void testDiSommableEtPartiellementOptionnelle() throws Exception {
		final int anneePf = 2008;
		final RegDate dateEmission = RegDate.get(2009, 1, 15);
		final RegDate delaiInitial = RegDate.get(2009, 3, 15);

		final long diId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {

				addCollAdm(MockCollectiviteAdministrative.CEDI);
				
				final PersonnePhysique pp = addNonHabitant("Jacques", "Cartier", RegDate.get(1980, 1, 5), Sexe.MASCULIN);
				addForPrincipal(pp, RegDate.get(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, RegDate.get(anneePf, 5, 31), MotifFor.ARRIVEE_HS, MockPays.France);
				addForPrincipal(pp, RegDate.get(anneePf, 6, 1), MotifFor.ARRIVEE_HS, MockCommune.Aubonne);
				addForSecondaire(pp, RegDate.get(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

				final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(pp, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				declaration.addEtat(new EtatDeclaration(dateEmission, TypeEtatDeclaration.EMISE));

				final DelaiDeclaration delai = new DelaiDeclaration();
				delai.setDateDemande(dateEmission);
				delai.setDelaiAccordeAu(delaiInitial);
				declaration.addDelai(delai);

				return declaration.getId();
			}
		});

		final RegDate dateTraitement = delaiInitial.addMonths(1);
		final EnvoiSommationsDIsResults results = processor.run(dateTraitement, false, 0, null);
		Assert.assertEquals(1, results.getTotalDisTraitees());
		Assert.assertEquals(1, results.getTotalDisSommees());
		Assert.assertEquals(1, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalIndigent());
		Assert.assertEquals(0, results.getTotalSourcierPur());
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisOptionnelles());
	}



	//UNIREG-2466 test sur le log correcte des erreurs notamment les NullPointerException
	//Ignoré tant qu'on a pas trouvé un moyen de generer une exception innatendu dans le Processor
	// 1.- a gagner
	@Ignore
	public void testErreurSommation() throws Exception {

		final int anneePf = 2009;
		final RegDate dateEmission = RegDate.get(2009, 1, 15);
		final RegDate delaiInitial = RegDate.get(2010, 6, 30);

		final long diId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {

				addCollAdm(MockCollectiviteAdministrative.CEDI);

				final PersonnePhysique pp = addNonHabitant("arben Jakupi", "Cartier", RegDate.get(1982, 10, 15), Sexe.MASCULIN);

				addForPrincipalSource(pp, RegDate.get(2002, 1, 1), MotifFor.ARRIVEE_HS,RegDate.get(2009, 3, 31),MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Aubonne.getNoOFS());
				addForPrincipal(pp, RegDate.get(anneePf, 4, 1), MotifFor.INDETERMINE, MockCommune.Aubonne);

				final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode);
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(pp, periode, date(anneePf, 4, 1), date(anneePf, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				declaration.addEtat(new EtatDeclaration(dateEmission, TypeEtatDeclaration.EMISE));

				final DelaiDeclaration delai = new DelaiDeclaration();
				delai.setDateDemande(dateEmission);
				delai.setDelaiAccordeAu(delaiInitial);
				declaration.addDelai(delai);			

				return declaration.getId();
			}
		});

		final RegDate dateTraitement = delaiInitial.addMonths(1);
		final EnvoiSommationsDIsResults results = processor.run(dateTraitement, false, 0, null);
		final List<EnvoiSommationsDIsResults.ErrorInfo> infoListErreur = results.getListeSommationsEnErreur();
		Assert.assertEquals(1, infoListErreur.size());
		final DeclarationImpotOrdinaire declaration =  diDao.get(diId);

		EnvoiSommationsDIsResults.ErrorInfo error =  infoListErreur.get(0);
		Assert.assertEquals(declaration.getTiers().getNumero(),error.getNumeroTiers());		

		Assert.assertEquals(1, results.getTotalDisTraitees());
		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalIndigent());
		Assert.assertEquals(0, results.getTotalSourcierPur());
		Assert.assertEquals(0, results.getTotalNonAssujettissement());	
		Assert.assertEquals(0, results.getTotalDisOptionnelles());
	}

	@Test
	public void testDiContribuableSourcierPur() throws Exception {
		final int anneePf = 2008;
		final RegDate dateEmission = RegDate.get(2009, 1, 15);
		final RegDate delaiInitial = RegDate.get(2009, 3, 15);

		final long diId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {

				addCollAdm(MockCollectiviteAdministrative.CEDI);

				final PersonnePhysique pp = addNonHabitant("Jacques", "Cartier", RegDate.get(1980, 1, 5), Sexe.MASCULIN);

				addForPrincipalSource(pp, RegDate.get(anneePf, 6, 1), MotifFor.ARRIVEE_HS,null,null, MockCommune.Aubonne.getNoOFS());


				final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(pp, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				declaration.addEtat(new EtatDeclaration(dateEmission, TypeEtatDeclaration.EMISE));

				final DelaiDeclaration delai = new DelaiDeclaration();
				delai.setDateDemande(dateEmission);
				delai.setDelaiAccordeAu(delaiInitial);
				declaration.addDelai(delai);

				return declaration.getId();
			}
		});

		final RegDate dateTraitement = delaiInitial.addMonths(1);
		final EnvoiSommationsDIsResults results = processor.run(dateTraitement, false, 0, null);
		Assert.assertEquals(1, results.getTotalDisTraitees());
		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalIndigent());
		Assert.assertEquals(1, results.getTotalSourcierPur());
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalDisOptionnelles());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
	}

	/**
	 * C'est un cas qui se produit beaucoup avec les LR des débiteurs "web" : ils envoient la LR avant même qu'on leur demande...
	 * Par acquis de conscience, on fait aussi le test pour les DIs
	 */
	@Test
	public void testNonSommationDiRetourneeAvantEmission() throws Exception {
		final int anneePf = 2008;
		final RegDate dateEmission = RegDate.get(2009, 1, 15);
		final RegDate delaiInitial = RegDate.get(2009, 3, 15);

		final long diId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {

				addCollAdm(MockCollectiviteAdministrative.CEDI);

				final PersonnePhysique pp = addNonHabitant("Jacques", "Cartier", RegDate.get(1980, 1, 5), Sexe.MASCULIN);
				addForPrincipal(pp, RegDate.get(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, RegDate.get(anneePf, 5, 31), MotifFor.ARRIVEE_HS, MockPays.France);
				addForPrincipal(pp, RegDate.get(anneePf, 6, 1), MotifFor.ARRIVEE_HS, MockCommune.Aubonne);
				addForSecondaire(pp, RegDate.get(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

				final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(pp, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				declaration.addEtat(new EtatDeclaration(dateEmission, TypeEtatDeclaration.EMISE));
				declaration.addEtat(new EtatDeclaration(dateEmission.addDays(-5), TypeEtatDeclaration.RETOURNEE));

				final DelaiDeclaration delai = new DelaiDeclaration();
				delai.setDateDemande(dateEmission);
				delai.setDelaiAccordeAu(delaiInitial);
				declaration.addDelai(delai);

				return declaration.getId();
			}
		});

		final RegDate dateTraitement = delaiInitial.addYears(1);
		final EnvoiSommationsDIsResults results = processor.run(dateTraitement, false, 0, null);
		Assert.assertEquals(0, results.getTotalDisTraitees());
		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalIndigent());
		Assert.assertEquals(0, results.getTotalSourcierPur());
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisOptionnelles());
	}

	@Test
	public void testNonSommationLrDejaSommee() throws Exception {
		final int anneePf = 2008;
		final RegDate dateEmission = RegDate.get(2009, 1, 15);
		final RegDate delaiInitial = RegDate.get(2009, 3, 15);

		final long diId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {

				addCollAdm(MockCollectiviteAdministrative.CEDI);

				final PersonnePhysique pp = addNonHabitant("Jacques", "Cartier", RegDate.get(1980, 1, 5), Sexe.MASCULIN);
				addForPrincipal(pp, RegDate.get(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, RegDate.get(anneePf, 5, 31), MotifFor.ARRIVEE_HS, MockPays.France);
				addForPrincipal(pp, RegDate.get(anneePf, 6, 1), MotifFor.ARRIVEE_HS, MockCommune.Aubonne);
				addForSecondaire(pp, RegDate.get(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

				final PeriodeFiscale periode = addPeriodeFiscale(anneePf);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(pp, periode, date(anneePf, 1, 1), date(anneePf, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				declaration.addEtat(new EtatDeclaration(dateEmission, TypeEtatDeclaration.EMISE));
				declaration.addEtat(new EtatDeclaration(delaiInitial.addMonths(1), TypeEtatDeclaration.SOMMEE));

				final DelaiDeclaration delai = new DelaiDeclaration();
				delai.setDateDemande(dateEmission);
				delai.setDelaiAccordeAu(delaiInitial);
				declaration.addDelai(delai);

				return declaration.getId();
			}
		});

		final RegDate dateTraitement = delaiInitial.addYears(1);
		final EnvoiSommationsDIsResults results = processor.run(dateTraitement, false, 0, null);
		Assert.assertEquals(0, results.getTotalDisTraitees());
		Assert.assertEquals(0, results.getTotalDisSommees());
		Assert.assertEquals(0, results.getTotalSommations(anneePf));
		Assert.assertEquals(0, results.getTotalIndigent());
		Assert.assertEquals(0, results.getTotalSourcierPur());
		Assert.assertEquals(0, results.getTotalNonAssujettissement());
		Assert.assertEquals(0, results.getTotalSommationsEnErreur());
		Assert.assertEquals(0, results.getTotalDisOptionnelles());
	}

}
