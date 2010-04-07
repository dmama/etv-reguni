package ch.vd.uniregctb.tiers.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.view.ForFiscalView;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatTache;

public class ForFiscalManagerTest extends WebTest {

	private ForFiscalManager manager;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		manager = getBean(ForFiscalManager.class, "forFiscalManager");
	}

	/**
	 * [UNIREG-1036] Test que le bug qui provoquait la disparition des fors fiscaux précédents après l'ajout d'un fors fiscal HS sur un
	 * contribuable ne réapparaît pas.
	 */
	@NotTransactional
	@Test
	public void testAddForHorsSuisseSurCouple() throws Exception {

		final long noIndLaurent = 333908;
		final long noIndChristine = 333905;

		// Crée un ménage commun composé de deux habitants

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				Individu laurent = addIndividu(noIndLaurent, RegDate.get(1961, 2, 9), "Laurent", "Schmidt", true);
				Individu christine = addIndividu(noIndChristine, RegDate.get(1960, 10, 20), "Christine", "Schmidt", false);
				addAdresse(laurent, EnumTypeAdresse.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, date(1978, 10, 20), date(
						1985, 2, 14));
				addAdresse(laurent, EnumTypeAdresse.PRINCIPALE, MockRue.VillarsSousYens.RouteDeStPrex, null,
						date(1985, 2, 14), null);
				addAdresse(christine, EnumTypeAdresse.PRINCIPALE, MockRue.VillarsSousYens.RouteDeStPrex, null,
						date(1979, 2, 9), null);
			}
		});

		truncateDatabase();

		final Long numeroMenage = (Long) doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PeriodeFiscale periode2005 = addPeriodeFiscale(2005);
				final PeriodeFiscale periode2006 = addPeriodeFiscale(2006);
				final PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
				final ModeleDocument modele2005 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2005);
				final ModeleDocument modele2006 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2006);
				final ModeleDocument modele2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);

				final PersonnePhysique laurent = addHabitant(noIndLaurent);
				addForPrincipal(laurent, date(1978, 10, 20), MotifFor.DEMENAGEMENT_VD, date(1985, 2, 14),
						MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bex);

				final PersonnePhysique christine = addHabitant(noIndChristine);
				addForPrincipal(christine, date(1979, 2, 9), MotifFor.DEMENAGEMENT_VD, date(1985, 2, 14),
						MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.VillarsSousYens);

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(laurent, christine, date(1985, 2, 15), null);
				final MenageCommun menage = ensemble.getMenage();

				addForPrincipal(menage, date(1985, 2, 15), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
						MockCommune.VillarsSousYens);

				final DeclarationImpotOrdinaire declaration2005 = addDeclarationImpot(menage, periode2005, date(2005, 1, 1), date(2005, 12,
						31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2005);
				addDeclarationImpot(menage, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						modele2006);
				addDeclarationImpot(menage, periode2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						modele2007);

				addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2007, 10, 25), date(2007, 1, 1), date(2007, 12, 31),
						TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, menage, null);
				addTacheAnnulDI(TypeEtatTache.EN_INSTANCE, date(2007, 10, 25), declaration2005, menage);
				addTacheControleDossier(TypeEtatTache.TRAITE, date(2007, 10, 25), menage);
				addTacheControleDossier(TypeEtatTache.EN_INSTANCE, date(2007, 10, 25), menage);
				return menage.getNumero();
			}
		});
		assertNotNull(numeroMenage);

		// Ajoute un nouveau for fiscal principal hors-Suisse

		ForFiscalView view = new ForFiscalView();
		view.setDateOuverture(date(2009, 6, 8));
		view.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		view.setModeImposition(ModeImposition.ORDINAIRE);
		view.setMotifOuverture(MotifFor.DEPART_HS);
		view.setMotifRattachement(MotifRattachement.DOMICILE);
		view.setNumeroCtb(numeroMenage);
		view.setNumeroForFiscalPays(MockPays.France.getNoOFS());
		view.setTypeAutoriteFiscale(TypeAutoriteFiscale.PAYS_HS);

		manager.save(view);

		// Vérifie que le ménage commun possède bien deux fors fiscaux

		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final MenageCommun menage = (MenageCommun) hibernateTemplate.get(MenageCommun.class, numeroMenage);
				assertNotNull(menage);

				final List<ForFiscal> fors = menage.getForsFiscauxSorted();
				assertNotNull(fors);
				assertEquals(2, fors.size());

				final ForFiscalPrincipal forSuisse = (ForFiscalPrincipal) fors.get(0);
				assertNotNull(forSuisse);
				assertEquals(date(1985, 2, 15), forSuisse.getDateDebut());
				assertEquals(date(2009, 6, 7), forSuisse.getDateFin());
				assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forSuisse.getTypeAutoriteFiscale());
				assertEquals(MockCommune.VillarsSousYens.getNoOFS(), forSuisse.getNumeroOfsAutoriteFiscale().intValue());

				final ForFiscalPrincipal forFrancais = (ForFiscalPrincipal) fors.get(1);
				assertNotNull(forFrancais);
				assertEquals(date(2009, 6, 8), forFrancais.getDateDebut());
				assertNull(forFrancais.getDateFin());
				assertEquals(TypeAutoriteFiscale.PAYS_HS, forFrancais.getTypeAutoriteFiscale());
				assertEquals(MockPays.France.getNoOFS(), forFrancais.getNumeroOfsAutoriteFiscale().intValue());
				return null;
			}
		});
	}



	/**
	 *  	 UNIREG-1576 Test permettant de verifier que l'on peut ajouter un for ferme sur une personne qui est deja en couple
	 *  si  le for est valide en dehors de la validité du couple
	 * @throws Exception
	 */

	//@Transactional
	@Test
	public void testAjoutForFerme() throws Exception {

		final long noIndLaurent = 333908;
		final long noIndChristine = 333905;

		// Crée un ménage commun composé de deux habitants

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				Individu laurent = addIndividu(noIndLaurent, RegDate.get(1961, 2, 9), "Laurent", "Schmidt", true);
				Individu christine = addIndividu(noIndChristine, RegDate.get(1960, 10, 20), "Christine", "Schmidt", false);
				addAdresse(laurent, EnumTypeAdresse.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, date(1978, 10, 20), date(
						1985, 2, 14));
				addAdresse(laurent, EnumTypeAdresse.PRINCIPALE, MockRue.VillarsSousYens.RouteDeStPrex, null,
						date(1985, 2, 14), null);
				addAdresse(christine, EnumTypeAdresse.PRINCIPALE, MockRue.VillarsSousYens.RouteDeStPrex, null,
						date(1979, 2, 9), null);
			}
		});

		truncateDatabase();

		final Long numeroChristine = (Long) doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PeriodeFiscale periode2005 = addPeriodeFiscale(2005);
				final PeriodeFiscale periode2006 = addPeriodeFiscale(2006);
				final PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
				final ModeleDocument modele2005 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2005);
				final ModeleDocument modele2006 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2006);
				final ModeleDocument modele2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);

				final PersonnePhysique laurent = addHabitant(noIndLaurent);
				addForPrincipal(laurent, date(1978, 10, 20), MotifFor.DEMENAGEMENT_VD, date(1985, 2, 14),
						MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bex);

				final PersonnePhysique christine = addHabitant(noIndChristine);


				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(laurent, christine, date(1985, 2, 15), null);
				final MenageCommun menage = ensemble.getMenage();

				addForPrincipal(menage, date(1985, 2, 15), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
						MockCommune.VillarsSousYens);

				final DeclarationImpotOrdinaire declaration2005 = addDeclarationImpot(menage, periode2005, date(2005, 1, 1), date(2005, 12,
						31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2005);
				addDeclarationImpot(menage, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						modele2006);
				addDeclarationImpot(menage, periode2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						modele2007);

				addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2007, 10, 25), date(2007, 1, 1), date(2007, 12, 31),
						TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, menage, null);
				addTacheAnnulDI(TypeEtatTache.EN_INSTANCE, date(2007, 10, 25), declaration2005, menage);
				addTacheControleDossier(TypeEtatTache.TRAITE, date(2007, 10, 25), menage);
				addTacheControleDossier(TypeEtatTache.EN_INSTANCE, date(2007, 10, 25), menage);
				return christine.getNumero();
			}
		});
		assertNotNull(numeroChristine);

		// Ajoute un nouveau for fiscal principal ferme avant le mariage

		ForFiscalView view = new ForFiscalView();
		view.setDateOuverture(date(1979, 6, 8));
		view.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		view.setModeImposition(ModeImposition.ORDINAIRE);
		view.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
		view.setMotifRattachement(MotifRattachement.DOMICILE);
		view.setDateFermeture(date(1985, 2, 14));
		view.setMotifFermeture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		view.setNumeroCtb(numeroChristine);
		view.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		view.setNumeroForFiscalCommune(MockCommune.VillarsSousYens.getNoOFS());

		manager.save(view);

		// Vérifie que le ménage commun possède bien deux fors fiscaux

		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique christine = (PersonnePhysique) hibernateTemplate.get(PersonnePhysique.class, numeroChristine);
				assertNotNull(christine);

				final List<ForFiscal> fors = christine.getForsFiscauxSorted();
				assertNotNull(fors);
				assertEquals(1, fors.size());

				final ForFiscalPrincipal forSuisse = (ForFiscalPrincipal) fors.get(0);
				assertNotNull(forSuisse);
				assertEquals(date(1979, 6, 8), forSuisse.getDateDebut());
				assertEquals(date(1985, 2, 14), forSuisse.getDateFin());
				assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forSuisse.getTypeAutoriteFiscale());
				assertEquals(MockCommune.VillarsSousYens.getNoOFS(), forSuisse.getNumeroOfsAutoriteFiscale().intValue());
				return null;
			}
		});
	}
}
