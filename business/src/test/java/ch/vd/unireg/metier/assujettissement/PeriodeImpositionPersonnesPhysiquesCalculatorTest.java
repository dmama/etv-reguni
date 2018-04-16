package ch.vd.unireg.metier.assujettissement;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseRetour;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.validation.ValidationService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test des calculs de periodes d'imposition dans le contexte des personnes physiques
 */
public class PeriodeImpositionPersonnesPhysiquesCalculatorTest extends MetierTest {

	private AssujettissementService assujettissementService;
	private PeriodeImpositionPersonnesPhysiquesCalculator calculator;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		assujettissementService = getBean(AssujettissementService.class, "assujettissementService");

		final ParametreAppService parametreAppService = getBean(ParametreAppService.class, "parametreAppService");
		calculator = new PeriodeImpositionPersonnesPhysiquesCalculator(parametreAppService);
	}

	@NotNull
	private List<PeriodeImposition> determine(ContribuableImpositionPersonnesPhysiques ctb, PeriodeImpositionCalculator<ContribuableImpositionPersonnesPhysiques> calculator) throws AssujettissementException {
		final List<Assujettissement> a = assujettissementService.determine(ctb);
		return calculator.determine(ctb, a);
	}

	@NotNull
	private List<PeriodeImposition> determine(ContribuableImpositionPersonnesPhysiques ctb, int annee) throws AssujettissementException {
		final PeriodeImpositionCalculator<ContribuableImpositionPersonnesPhysiques> yearLimiting = PeriodeImpositionHelper.periodeFiscaleLimiting(calculator, annee);
		return determine(ctb, yearLimiting);
	}

	@NotNull
	private List<PeriodeImposition> determine(ContribuableImpositionPersonnesPhysiques ctb, DateRange range) throws AssujettissementException {
		final PeriodeImpositionCalculator<ContribuableImpositionPersonnesPhysiques> rangeIntersecting = PeriodeImpositionHelper.rangeIntersecting(calculator, range);
		return determine(ctb, rangeIntersecting);
	}

	/**
	 * [UNIREG-1327] Vérifie que la période d'imposition d'un HS qui vend son immeuble ne s'étend pas au delà de la date de vente.
	 * [UNIREG-1742] Vérifie que la dernière déclaration qui découle de la vente de l'immeuble n'est pas optionnelle.
	 */
	@Test
	public void testDetermineVenteImmeubleContribuableHorsSuisse() throws Exception {

		final RegDate dateAchat = date(2003, 7, 1);
		final RegDate dateVente = date(2007, 5, 30);

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique paul = createHorsSuisseAvecAchatEtVenteImmeuble(dateAchat, dateVente);

				// 2006
				{
					final List<PeriodeImposition> list = determine(paul, 2006);
					assertNotNull(list);
					assertEquals(1, list.size());
					// hors-Suisse toute l'année
					assertPeriodeImpositionPersonnesPhysiques(date(2006, 1, 1), date(2006, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(0));
				}

				// 2007
				{
					final List<PeriodeImposition> list = determine(paul, 2007);
					assertNotNull(list);
					assertEquals(1, list.size());
					// hors-Suisse jusqu'à la date de la vente de l'immeuble
					assertPeriodeImpositionPersonnesPhysiques(date(2007, 1, 1), dateVente, CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, true, list.get(0)); // [UNIREG-1742] vente de l'immeuble -> déclaration pas optionnelle
				}

				// 2008
				{
					final List<PeriodeImposition> list = determine(paul, 2008);
					assertEmpty(list);
				}

				// 2000-2008 (-> transformé en 2003-2008 car on ne calcule pas de période d'imposition avant 2003 pour les personnes physiques)
				{
					List<PeriodeImposition> list = determine(paul, RANGE_2000_2008);
					assertNotNull(list);
					assertEquals(5, list.size());
					assertPeriodeImpositionPersonnesPhysiques(dateAchat, date(2003, 12, 31), CategorieEnvoiDIPP.HS_VAUDTAX, TypeAdresseRetour.CEDI, true, false, false, false, list.get(0));  // [UNIREG-1742] la DI est optionnelle dès la première année
					assertPeriodeImpositionPersonnesPhysiques(date(2004, 1, 1), date(2004, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(1));
					assertPeriodeImpositionPersonnesPhysiques(date(2005, 1, 1), date(2005, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(2));
					assertPeriodeImpositionPersonnesPhysiques(date(2006, 1, 1), date(2006, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(3));
					assertPeriodeImpositionPersonnesPhysiques(date(2007, 1, 1), dateVente, CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, true, list.get(4));
				}
			}
		});
	}

	/**
	 * [UNIREG-1742] En cas d'achats et de ventes d'immeuble multiples pour un hors-Suisse, il y a plusieurs
	 * périodes d'imposition et plusieurs déclarations (confirmé par Thierry Declercq le 18 décembre 2009)
	 */
	@Test
	public void testDetermineAchatsEtVentesMultipleHorsSuisse() throws Exception {

		final DateRangeHelper.Range immeuble1 = new DateRangeHelper.Range(date(2008, 1, 15), date(2008, 3, 30));
		final DateRangeHelper.Range immeuble2 = new DateRangeHelper.Range(date(2008, 6, 2), date(2008, 11, 26));

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ctb = createHorsSuisseAvecAchatsEtVentesImmeubles(immeuble1, immeuble2);

				// 2007
				{
					assertEmpty(determine(ctb, 2007));
				}

				// 2008
				{
					final List<PeriodeImposition> list = determine(ctb, 2008);
					assertNotNull(list);
					assertEquals(2, list.size());
					assertPeriodeImpositionPersonnesPhysiques(immeuble1.getDateDebut(), immeuble1.getDateFin(), CategorieEnvoiDIPP.HS_VAUDTAX, TypeAdresseRetour.CEDI, false, false, false, true,
					                                          list.get(0)); // pas optionnelle car vente de l'immeuble
					assertPeriodeImpositionPersonnesPhysiques(immeuble2.getDateDebut(), immeuble2.getDateFin(), CategorieEnvoiDIPP.HS_VAUDTAX, TypeAdresseRetour.CEDI, false, false, false, true,
					                                          list.get(1)); // pas optionnelle car vente de l'immeuble
				}

				// 2009
				{
					assertEmpty(determine(ctb, 2009));
				}
			}
		});
	}

	@Test
	public void testDetermineArriveeHorsSuisseAvecImmeuble() throws Exception {

		final RegDate dateArrivee = date(2007, 3, 1);
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique paul = createArriveeHorsSuisseAvecImmeuble(dateArrivee);

				// 2006
				{
					final List<PeriodeImposition> list = determine(paul, 2006);
					assertNotNull(list);
					assertEquals(1, list.size());
					// hors-Suisse toute l'année
					assertPeriodeImpositionPersonnesPhysiques(date(2006, 1, 1), date(2006, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(0));
				}

				// 2007
				{
					final List<PeriodeImposition> list = determine(paul, 2007);
					assertNotNull(list);
					assertEquals(1, list.size());
					// hors-Suisse avant l'arrivée et ordinaire ensuite, mais les deux assujettissements se confondent en un au niveau de la période
					// d'imposition puisque les types de DI sont les mêmes.
					assertPeriodeImpositionPersonnesPhysiques(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDIPP.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
				}

				// 2008
				{
					final List<PeriodeImposition> list = determine(paul, 2008);
					assertNotNull(list);
					assertEquals(1, list.size());
					// ordinaire toute l'année
					assertPeriodeImpositionPersonnesPhysiques(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDIPP.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
				}

				// 2000-2008 (-> transformé en 2003-2008 car on ne calcule pas de période d'imposition avant 2003 pour les personnes physiques)
				{
					final List<PeriodeImposition> list = determine(paul, RANGE_2000_2008);
					assertNotNull(list);
					assertEquals(6, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2003, 1, 1), date(2003, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(0));
					assertPeriodeImpositionPersonnesPhysiques(date(2004, 1, 1), date(2004, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(1));
					assertPeriodeImpositionPersonnesPhysiques(date(2005, 1, 1), date(2005, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(2));
					assertPeriodeImpositionPersonnesPhysiques(date(2006, 1, 1), date(2006, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(3));
					assertPeriodeImpositionPersonnesPhysiques(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDIPP.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(4));
					assertPeriodeImpositionPersonnesPhysiques(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDIPP.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(5));
				}
			}
		});
	}

	/**
	 * [UNIREG-1327] Vérifie que la période d'imposition d'un contribuable HS qui possède un immeuble, arrive de HS et vend son immeuble dans la
	 * même année est bien fractionné à la date d'arrivée HS.
	 */
	@Test
	public void testDetermineArriveeHorsSuisseEtVenteImmeubleDansLAnnee() throws Exception {

		final RegDate dateArrivee = date(2007, 3, 1);
		final RegDate dateVente = date(2007, 5, 30);

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique paul = createArriveeHorsSuisseEtVenteImmeuble(dateArrivee, dateVente);

				// 2006
				{
					final List<PeriodeImposition> list = determine(paul, 2006);
					assertNotNull(list);
					assertEquals(1, list.size());
					// hors-Suisse toute l'année
					assertPeriodeImpositionPersonnesPhysiques(date(2006, 1, 1), date(2006, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(0));
				}

				// 2007
				{
					final List<PeriodeImposition> list = determine(paul, 2007);
					assertNotNull(list);
					assertEquals(1, list.size());
					// hors-Suisse avant l'arrivée et ordinaire ensuite, mais les deux assujettissements se confondent en un au niveau de la période
					// d'imposition puisque les types de DI sont les mêmes.
					assertPeriodeImpositionPersonnesPhysiques(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDIPP.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
				}

				// 2008
				{
					final List<PeriodeImposition> list = determine(paul, 2008);
					assertNotNull(list);
					assertEquals(1, list.size());
					// ordinaire toute l'année
					assertPeriodeImpositionPersonnesPhysiques(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDIPP.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
				}

				// 2000-2008 (-> transformé en 2003-2008 car on ne calcule pas de période d'imposition avant 2003 pour les personnes physiques)
				{
					final List<PeriodeImposition> list = determine(paul, RANGE_2000_2008);
					assertNotNull(list);
					assertEquals(6, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2003, 1, 1), date(2003, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(0));
					assertPeriodeImpositionPersonnesPhysiques(date(2004, 1, 1), date(2004, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(1));
					assertPeriodeImpositionPersonnesPhysiques(date(2005, 1, 1), date(2005, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(2));
					assertPeriodeImpositionPersonnesPhysiques(date(2006, 1, 1), date(2006, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(3));
					assertPeriodeImpositionPersonnesPhysiques(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDIPP.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(4));
					assertPeriodeImpositionPersonnesPhysiques(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDIPP.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(5));
				}
			}
		});
	}

	/**
	 * Cas très spécial du contribuable qui arrive de HS et qui repart HS la même année, et qui achète un immeuble entre-deux. La période
	 * d'imposition doit commencer à la date d'arrivée et - puisqu'il y a un immeuble - s'étendre jusqu'à la fin de l'année.
	 */
	@Test
	public void testDetermineArriveeHorsSuisseAchatImmeubleEtDepartHorsSuisseDansLAnnee() throws Exception {

		final RegDate dateArrivee = date(2007, 3, 1);
		final RegDate dateAchat = date(2007, 5, 30);
		final RegDate dateDepart = date(2007, 12, 8);

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique paul = createArriveeHSAchatImmeubleEtDepartHS(dateArrivee, dateAchat, dateDepart);

				// 2006
				{
					final List<PeriodeImposition> list = determine(paul, 2006);
					assertEmpty(list);
				}

				// 2007
				{
					final List<PeriodeImposition> list = determine(paul, 2007);
					assertNotNull(list);
					assertEquals(1, list.size());
					// hors-Suisse non-assujetti avant l'arrivée, ordinaire ensuite puis de nouveau hors-Suisse mais assujetti suite au départ ->
					// deux assujettissements (ordinaire + hors-Suisse) qui se confondent en un au niveau de la période d'imposition puisque les
					// types de DI sont les mêmes.
					assertPeriodeImpositionPersonnesPhysiques(dateArrivee, date(2007, 12, 31), CategorieEnvoiDIPP.HS_VAUDTAX, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
				}

				// 2008
				{
					final List<PeriodeImposition> list = determine(paul, 2008);
					assertNotNull(list);
					assertEquals(1, list.size());
					// hors-Suisse toute l'année
					assertPeriodeImpositionPersonnesPhysiques(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(0));
				}

				// 2000-2008
				{
					final List<PeriodeImposition> list = determine(paul, RANGE_2000_2008);
					assertNotNull(list);
					assertEquals(2, list.size());
					assertPeriodeImpositionPersonnesPhysiques(dateArrivee, date(2007, 12, 31), CategorieEnvoiDIPP.HS_VAUDTAX, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
					assertPeriodeImpositionPersonnesPhysiques(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(1));
				}
			}
		});
	}

	/**
	 * Cas très spécial du contribuable qui arrive de HS et qui repart HS la même année, et qui achète un immeuble après son départ. Il doit
	 * y avoir deux périodes d'imposition distinctes : une pour sa présence en Suisse, et une autre pour son immeuble acheté plus tard.
	 */
	@Test
	public void testDetermineArriveeHorsSuisseEtDepartHorsSuissePuisAchatImmeubleDansLAnnee() throws Exception {

		final RegDate dateArrivee = date(2007, 2, 1);
		final RegDate dateDepart = date(2007, 7, 30);
		final RegDate dateAchat = date(2007, 10, 8);

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique paul = createArriveeHSDepartHSPuisAchatImmeuble(dateArrivee, dateDepart, dateAchat);

				// 2006
				{
					final List<PeriodeImposition> list = determine(paul, 2006);
					assertEmpty(list);
				}

				// 2007
				{
					final List<PeriodeImposition> list = determine(paul, 2007);
					assertNotNull(list);
					assertEquals(2, list.size());
					// assujetti comme ordinaire pendant son passage en suisse
					assertPeriodeImpositionPersonnesPhysiques(dateArrivee, dateDepart, CategorieEnvoiDIPP.VAUDOIS_VAUDTAX, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
					// assujetti comme hors-Suisse suite à l'achat de son immeuble
					// [UNIREG-1742] la déclaration est optionnelle dans ce cas (HS avec immeuble)
					assertPeriodeImpositionPersonnesPhysiques(dateAchat, date(2007, 12, 31), CategorieEnvoiDIPP.HS_VAUDTAX, TypeAdresseRetour.CEDI, true, false, false, false, list.get(1));
				}

				// 2008
				{
					final List<PeriodeImposition> list = determine(paul, 2008);
					assertNotNull(list);
					assertEquals(1, list.size());
					// hors-Suisse toute l'année
					assertPeriodeImpositionPersonnesPhysiques(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(0));
				}

				// 2000-2008
				{
					final List<PeriodeImposition> list = determine(paul, RANGE_2000_2008);
					assertNotNull(list);
					assertEquals(3, list.size());
					assertPeriodeImpositionPersonnesPhysiques(dateArrivee, dateDepart, CategorieEnvoiDIPP.VAUDOIS_VAUDTAX, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
					assertPeriodeImpositionPersonnesPhysiques(dateAchat, date(2007, 12, 31), CategorieEnvoiDIPP.HS_VAUDTAX, TypeAdresseRetour.CEDI, true, false, false, false, list.get(1));
					assertPeriodeImpositionPersonnesPhysiques(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(2));
				}
			}
		});
	}

	@Test
	public void testDetermineDepartHorsSuisseDansLAnneeAvecImmeuble() throws Exception {

		final RegDate dateDepart = date(2007, 6, 30);

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique paul = createDepartHorsSuisseAvecImmeuble(dateDepart);

				// 2006 (vaudois ordinaire)
				{
					final List<PeriodeImposition> list = determine(paul, 2006);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2006, 1, 1), date(2006, 12, 31), CategorieEnvoiDIPP.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
				}

				// 2007 (départ hors-Suisse dans l'année)
				{
					final List<PeriodeImposition> list = determine(paul, 2007);
					assertNotNull(list);
					assertEquals(1, list.size());
					// vaudois ordinaire jusqu'au départ puis hors-Suisse, mais les deux périodes se fusionnent car les types de document sont les mêmes
					assertPeriodeImpositionPersonnesPhysiques(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
				}

				// 2008 (hors-Suisse)
				{
					final List<PeriodeImposition> list = determine(paul, 2008);
					assertNotNull(list);
					assertEquals(1, list.size());
					// vaudois ordinaire jusqu'au départ puis hors-Suisse, mais les deux périodes se fusionnent car les types de document sont les mêmes
					assertPeriodeImpositionPersonnesPhysiques(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(0));
				}

				// 2006-2008
				{
					final List<PeriodeImposition> list = determine(paul, RANGE_2006_2008);
					assertNotNull(list);
					assertEquals(3, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2006, 1, 1), date(2006, 12, 31), CategorieEnvoiDIPP.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
					assertPeriodeImpositionPersonnesPhysiques(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(1));
					assertPeriodeImpositionPersonnesPhysiques(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(2));
				}
			}
		});
	}

	@Test
	public void testDetermineDepartHorsSuisseDepuisHorsCantonAvecImmeuble() throws Exception {

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ctb = createDepartHorsSuisseDepuisHorsCantonAvecImmeuble(date(2007, 6, 30));

				// 2006 (hors-canton)
				{
					final List<PeriodeImposition> list = determine(ctb, 2006);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2006, 1, 1), date(2006, 12, 31), CategorieEnvoiDIPP.HC_IMMEUBLE, TypeAdresseRetour.OID, false, false, false, false, list.get(0));
				}

				// 2007 (hors-canton -> hors-Suisse)
				{
					final List<PeriodeImposition> list = determine(ctb, 2007);
					assertNotNull(list);
					assertEquals(1, list.size());
					// [UNIREG-1742] le départ hors-Suisse depuis hors-canton ne doit pas fractionner la période d'assujettissement (car le rattachement économique n'est pas interrompu)
					assertPeriodeImpositionPersonnesPhysiques(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(0));
				}

				// 2008 (hors-Suisse)
				{
					final List<PeriodeImposition> list = determine(ctb, 2008);
					assertNotNull(list);
					assertEquals(1, list.size());
					// vaudois ordinaire jusqu'au départ puis hors-Suisse, mais les deux périodes se fusionnent car les types de document sont les mêmes
					assertPeriodeImpositionPersonnesPhysiques(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(0));
				}

				// 2006-2008
				{
					final List<PeriodeImposition> list = determine(ctb, RANGE_2006_2008);
					assertNotNull(list);
					assertEquals(3, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2006, 1, 1), date(2006, 12, 31), CategorieEnvoiDIPP.HC_IMMEUBLE, TypeAdresseRetour.OID, false, false, false, false, list.get(0));
					assertPeriodeImpositionPersonnesPhysiques(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(1));
					assertPeriodeImpositionPersonnesPhysiques(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(2));
				}
			}
		});
	}

	@Test
	public void testDetermineDepartHorsSuisseDepuisHorsCantonAvecActiviteIndependante() throws Exception {

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ctb = createDepartHorsSuisseDepuisHorsCantonAvecActiviteIndependante(date(2007, 6, 30));

				// 2006 (hors-canton)
				{
					final List<PeriodeImposition> list = determine(ctb, 2006);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2006, 1, 1), date(2006, 12, 31), CategorieEnvoiDIPP.HC_ACTIND_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false,
					                                          list.get(0));
				}

				// 2007 (hors-canton -> hors-Suisse)
				{
					final List<PeriodeImposition> list = determine(ctb, 2007);
					assertNotNull(list);
					assertEquals(1, list.size());
					// [UNIREG-1742] le départ hors-Suisse depuis hors-canton ne doit pas fractionner la période d'assujettissement (car le rattachement économique n'est pas interrompu)
					assertPeriodeImpositionPersonnesPhysiques(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
				}

				// 2008 (hors-Suisse)
				{
					final List<PeriodeImposition> list = determine(ctb, 2008);
					assertNotNull(list);
					assertEquals(1, list.size());
					// vaudois ordinaire jusqu'au départ puis hors-Suisse, mais les deux périodes se fusionnent car les types de document sont les mêmes
					assertPeriodeImpositionPersonnesPhysiques(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
				}

				// 2006-2008
				{
					final List<PeriodeImposition> list = determine(ctb, RANGE_2006_2008);
					assertNotNull(list);
					assertEquals(3, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2006, 1, 1), date(2006, 12, 31), CategorieEnvoiDIPP.HC_ACTIND_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
					assertPeriodeImpositionPersonnesPhysiques(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(1));
					assertPeriodeImpositionPersonnesPhysiques(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(2));
				}
			}
		});
	}

	/**
	 * [UNIREG-1390] Vérifie qu'il est possible de déterminer la période d'imposition d'un hors-Suisse qui vend son immeuble et dont le for
	 * principal est fermé sans motif (cas du ctb n°807.110.03).
	 */
	@Test
	public void testDetermineHorsForPrincipalFermeSansMotif() throws Exception {

		final RegDate dateVente = date(2009, 3, 24);

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ctb = createHorsSuisseVenteImmeubleEtFermetureFFPSansMotif(dateVente);
				assertNotNull(ctb);

				final List<PeriodeImposition> list = determine(ctb, 2009);
				assertNotNull(list);
				assertEquals(1, list.size());
				assertPeriodeImpositionPersonnesPhysiques(date(2009, 1, 1), dateVente, CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, true, list.get(0));
			}
		});
	}

	/**
	 * [UNIREG-1756] Vérifie que le type de DI est vaudtax pour un sourcier qui passe à l'ordinaire.
	 */
	@Test
	public void testDetermineSourcierPassantOrdinaire() throws Exception
	{
		final RegDate dateChangement = date(2008, 9, 1);

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ctb = createSourcierPur();
				// Fermeture du dernier for à la source source
				ForFiscalPrincipal ffp = ctb.getForFiscalPrincipalAt(null);
				ffp.setDateFin(dateChangement.getOneDayBefore());
				ffp.setMotifFermeture(MotifFor.CHGT_MODE_IMPOSITION);
				// Passage à l'ordinaire
				addForPrincipal(ctb, dateChangement, MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne);

				final List<PeriodeImposition> list = determine(ctb, dateChangement.year());
				assertNotNull(list);
				assertEquals(1, list.size());
				assertPeriodeImpositionPersonnesPhysiques(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDIPP.VAUDOIS_VAUDTAX, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
			}
		});
	}

	/**
	 * [UNIREG-1741]
	 */
	@Test
	public void testDetermineVaudoisDepense() throws Exception {

		final RegDate dateArrivee = date(1990, 4, 13);
		final RegDate dateDeces = date(2009, 2, 23);

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ctb = createDecesVaudoisDepense(dateArrivee, dateDeces);
				assertNotNull(ctb);

				// 2007
				{
					final List<PeriodeImposition> list = determine(ctb, 2007);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDIPP.VAUDOIS_DEPENSE, TypeAdresseRetour.OID, false, false, false, false, list.get(0));
				}

				// 2008
				{
					final List<PeriodeImposition> list = determine(ctb, 2008);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDIPP.VAUDOIS_DEPENSE, TypeAdresseRetour.OID, false, false, false, false, list.get(0));
				}

				// 2009 (décès)
				{
					final List<PeriodeImposition> list = determine(ctb, 2009);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2009, 1, 1), dateDeces, CategorieEnvoiDIPP.VAUDOIS_DEPENSE, TypeAdresseRetour.CEDI, false, false, true, false, list.get(0));
				}
			}
		});
	}

	/**
	 * [UNIREG-1360] Vérifie que la période d'imposition s'arrête bien à la date du décès dans le cas d'un contribuable hors-canton qui possèdait un immeuble dans le canton.
	 */
	@Test
	public void testDetermineDecesHorsCantonImmeuble() throws Exception {

		final RegDate dateAchat = date(1990, 4, 13);
		final RegDate dateDeces = date(2009, 2, 23);

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ctb = createDecesHorsCantonAvecImmeuble(dateAchat, dateDeces);
				assertNotNull(ctb);

				// 2007
				{
					final List<PeriodeImposition> list = determine(ctb, 2007);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDIPP.HC_IMMEUBLE, TypeAdresseRetour.OID, false, false, false, false, list.get(0));
				}

				// 2008
				{
					final List<PeriodeImposition> list = determine(ctb, 2008);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDIPP.HC_IMMEUBLE, TypeAdresseRetour.OID, false, false, false, false, list.get(0));
				}

				// 2009 (décès)
				{
					// [SIFISC-7636] la déclaration d'impôt n'est ni optionnelle ni remplacée par une note pour les contribuables domiciliés dans un autre canton dont le rattachement économique
					// (activité indépendante ou immeuble) s’est terminé au cours de la période fiscale pour cause de décès
					final List<PeriodeImposition> list = determine(ctb, 2009);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2009, 1, 1), dateDeces, CategorieEnvoiDIPP.HC_IMMEUBLE, TypeAdresseRetour.CEDI, false, false, true, false, list.get(0));
				}
			}
		});
	}

	/**
	 * [UNIREG-1360] Vérifie que la déclaration d'impôt reste bien remplacée par une note dans le cas où un contribuable hors-canton décède *même* s'il avait vendu son dernier immeuble dans l'année.
	 */
	@Test
	public void testDetermineDecesHorsCantonAvecVenteImmeubleEnDebutDAnnee() throws Exception {

		final RegDate dateAchat = date(1990, 4, 13);
		final RegDate dateVente = date(2009, 2, 23);
		final RegDate dateDeces = date(2009, 6, 3);

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ctb = createContribuableSansFor(null);
				addForPrincipal(ctb, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Neuchatel);
				addForSecondaire(ctb, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				assertNotNull(ctb);

				// 2007
				{
					final List<PeriodeImposition> list = determine(ctb, 2007);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDIPP.HC_IMMEUBLE, TypeAdresseRetour.OID, false, false, false, false, list.get(0));
				}

				// 2008
				{
					final List<PeriodeImposition> list = determine(ctb, 2008);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDIPP.HC_IMMEUBLE, TypeAdresseRetour.OID, false, false, false, false, list.get(0));
				}

				// 2009 (décès)
				{
					// [SIFISC-7636] la déclaration d'impôt n'est ni optionnelle mais bien remplacée par une note dans ce cas-là (vente de l'immeuble avant décès)
					final List<PeriodeImposition> list = determine(ctb, 2009);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2009, 1, 1), dateDeces, CategorieEnvoiDIPP.HC_IMMEUBLE, TypeAdresseRetour.CEDI, false, true, true, false, list.get(0));
				}
			}
		});
	}

	/**
	 * [UNIREG-1742] Vérifie que la dernière déclaration qui découle du décès du contribuable n'est pas optionnelle.
	 */
	@Test
	public void testDetermineDecesHorsSuisseImmeuble() throws Exception {

		final RegDate dateAchat = date(2003, 7, 1);
		final RegDate dateDeces = date(2007, 5, 30);

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique paul = createDecesHorsSuisseAvecImmeuble(dateAchat, dateDeces);

				// 2006
				{
					final List<PeriodeImposition> list = determine(paul, 2006);
					assertNotNull(list);
					assertEquals(1, list.size());
					// hors-Suisse toute l'année
					assertPeriodeImpositionPersonnesPhysiques(date(2006, 1, 1), date(2006, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(0));
				}

				// 2007 (décès)
				{
					final List<PeriodeImposition> list = determine(paul, 2007);
					assertNotNull(list);
					assertEquals(1, list.size());
					// hors-Suisse jusqu'à la date du décès
					assertPeriodeImpositionPersonnesPhysiques(date(2007, 1, 1), dateDeces, CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, false, false, true, false, list.get(0)); // [UNIREG-1742] décès -> déclaration pas optionnelle
				}

				// 2008
				{
					final List<PeriodeImposition> list = determine(paul, 2008);
					assertEmpty(list);
				}

				// 2000-2008 (-> transformé en 2003-2008 car on ne calcule pas de période d'imposition avant 2003 pour les personnes physiques)
				{
					List<PeriodeImposition> list = determine(paul, RANGE_2000_2008);
					assertNotNull(list);
					assertEquals(5, list.size());
					assertPeriodeImpositionPersonnesPhysiques(dateAchat, date(2003, 12, 31), CategorieEnvoiDIPP.HS_VAUDTAX, TypeAdresseRetour.CEDI, true, false, false, false, list.get(0));
					assertPeriodeImpositionPersonnesPhysiques(date(2004, 1, 1), date(2004, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(1));
					assertPeriodeImpositionPersonnesPhysiques(date(2005, 1, 1), date(2005, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(2));
					assertPeriodeImpositionPersonnesPhysiques(date(2006, 1, 1), date(2006, 12, 31), CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(3));
					assertPeriodeImpositionPersonnesPhysiques(date(2007, 1, 1), dateDeces, CategorieEnvoiDIPP.HS_COMPLETE, TypeAdresseRetour.CEDI, false, false, true, false, list.get(4));
				}
			}
		});
	}

	/**
	 * [UNIREG-1741]
	 */
	@Test
	public void testDetermineDecesHorsCantonActiviteIndependante() throws Exception {

		final RegDate debutExploitation = date(1990, 4, 13);
		final RegDate dateDeces = date(2009, 2, 23);

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ctb = createDecesHorsCantonActiviteIndependante(debutExploitation, dateDeces);
				assertNotNull(ctb);

				// 2007
				{
					final List<PeriodeImposition> list = determine(ctb, 2007);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDIPP.HC_ACTIND_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
				}

				// 2008
				{
					final List<PeriodeImposition> list = determine(ctb, 2008);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDIPP.HC_ACTIND_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
				}

				// 2009 (décès)
				{
					// [SIFISC-7636] la déclaration d'impôt n'est ni optionnelle ni remplacée par une note pour les contribuables domiciliés dans un autre canton dont le rattachement économique
					// (activité indépendante ou immeuble) s’est terminé au cours de la période fiscale pour cause de décès
					final List<PeriodeImposition> list = determine(ctb, 2009);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2009, 1, 1), dateDeces, CategorieEnvoiDIPP.HC_ACTIND_COMPLETE, TypeAdresseRetour.CEDI, false, false, true, false, list.get(0));
				}
			}
		});
	}

	/**
	 * [UNIREG-1741]
	 */
	@Test
	public void testDetermineDecesVaudoisOrdinaire() throws Exception {

		final RegDate debutExploitation = date(1990, 4, 13);
		final RegDate dateDeces = date(2009, 2, 23);

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ctb = createDecesVaudoisOrdinaire(debutExploitation, dateDeces);
				assertNotNull(ctb);

				// 2007
				{
					final List<PeriodeImposition> list = determine(ctb, 2007);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDIPP.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
				}

				// 2008
				{
					final List<PeriodeImposition> list = determine(ctb, 2008);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDIPP.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
				}

				// 2009 (décès)
				{
					final List<PeriodeImposition> list = determine(ctb, 2009);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2009, 1, 1), dateDeces, CategorieEnvoiDIPP.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, true, false, list.get(0));
				}
			}
		});
	}

	@Test
	public void testDetermineDepartHorsCantonSourcierPur() throws Exception {

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ctb = createDepartHorsCantonSourcierPur(date(2008, 9, 25));

				// sourcier pur -> pas de déclaration d'impôt ordinaire
				assertEmpty(determine(ctb, 2007));
				assertEmpty(determine(ctb, 2008));
				assertEmpty(determine(ctb, 2009));
			}
		});
	}

	@Test
	public void testDetermineDepartHorsCantonSourcierMixte137Al1AvecImmeuble() throws Exception {

		final long ctbId = doInNewTransactionAndSessionWithoutValidation(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final Contribuable ctb = createDepartHorsCantonSourcierMixte137Al1_Invalide(date(2008, 9, 25));
				return ctb.getNumero();
			}
		});

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ctb = (PersonnePhysique) tiersDAO.get(ctbId);

				// 2007
				{
					final List<PeriodeImposition> list = determine(ctb, 2007);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDIPP.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
				}

				// 2008 (départ en cours d'année)
				{
					final List<PeriodeImposition> list = determine(ctb, 2008);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDIPP.HC_IMMEUBLE, TypeAdresseRetour.OID, false, false, false, false, list.get(0));
				}

				// 2009
				{
					final List<PeriodeImposition> list = determine(ctb, 2009);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2009, 1, 1), date(2009, 12, 31), CategorieEnvoiDIPP.HC_IMMEUBLE, TypeAdresseRetour.OID, false, false, false, false, list.get(0));
				}
			}
		});
	}

	// [SIFISC-62] On s'assure que la DI d'un sourcier mixte 1 sans immeuble qui part hors-canton est bien optionnelle
	@Test
	public void testDetermineDepartHorsCantonSourcierMixte137Al1SansImmeuble() throws Exception {

		final RegDate dateDepart = date(2008, 9, 25);

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ctb = createDepartHorsCantonSourcierMixte137Al1(dateDepart);

				// 2007
				{
					// sourcier mixte
					final List<PeriodeImposition> list = determine(ctb, 2007);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDIPP.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
				}

				// 2008 (départ en cours d'année)
				{
					// sourcier mixte jusqu'au départ, sourcier pur après le départ

					// [UNIREG-1742] contribuables imposés selon le mode mixte, partis dans un autre canton durant l’année et n’ayant aucun
					// rattachement économique -> bien qu’ils soient assujettis de manière illimitée jusqu'au dernier jour du mois de leur départ,
					// leur déclaration d’impôt est remplacée (= elle est optionnelle, en fait, voir exemples à la fin de la spécification) par une
					// note à l’administration fiscale cantonale de leur domicile.

					// [SIFISC-7281] les sourcier mixte 1 sans immeuble qui partent hors-canton ne sont plus du tout assujetti au rôle dès l'année de départ
					// sourcier pur
					assertEmpty(determine(ctb, 2008));
				}

				// 2009
				{
					// sourcier pur
					assertEmpty(determine(ctb, 2009));
				}
			}
		});
	}

	@Test
	public void testDetermineDepartHorsCantonSourcierMixte137Al2() throws Exception {

		final RegDate dateDepart = date(2008, 9, 25);

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ctb = createDepartHorsCantonSourcierMixte137Al2(dateDepart);

				// 2007
				{
					// sourcier mixte
					final List<PeriodeImposition> list = determine(ctb, 2007);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDIPP.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
				}

				// 2008 (départ en cours d'année)
				{
					// sourcier mixte jusqu'au départ, sourcier pure après le départ

					// [UNIREG-1742] contribuables imposés selon le mode mixte, partis dans un autre canton durant l’année et n’ayant aucun
					// rattachement économique -> bien qu’ils soient assujettis de manière illimitée jusqu'au dernier jour du mois de leur départ,
					// leur déclaration d’impôt est remplacée (= elle est optionnelle, en fait, voir exemples à la fin de la spécification) par une
					// note à l’administration fiscale cantonale de leur domicile.
					final List<PeriodeImposition> list = determine(ctb, 2008);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2008, 1, 1), dateDepart, CategorieEnvoiDIPP.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, true, true, false, false, list.get(0));
				}

				// 2009
				{
					// sourcier pur
					assertEmpty(determine(ctb, 2009));
				}
			}
		});
	}

	@Test
	public void testDetermineArriveeHorsCantonSourcierPur() throws Exception {

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ctb = createArriveeHorsCantonSourcierPur(date(2008, 9, 25));

				// sourcier pur -> pas de déclaration d'impôt ordinaire
				assertEmpty(determine(ctb, 2007));
				assertEmpty(determine(ctb, 2008));
				assertEmpty(determine(ctb, 2009));
			}
		});
	}

	@Test
	public void testDetermineArriveeHorsCantonSourcierMixte137Al1() throws Exception {

		final long ctbId = doInNewTransactionAndSessionWithoutValidation(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final Contribuable ctb = createArriveeHorsCantonSourcierMixte137Al1_Invalide(date(2008, 9, 25));
				return ctb.getNumero();
			}
		});

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ctb = (PersonnePhysique) tiersDAO.get(ctbId);

				// 2007
				{
					final List<PeriodeImposition> list = determine(ctb, 2007);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDIPP.HC_IMMEUBLE, TypeAdresseRetour.OID, false, false, false, false, list.get(0));
				}

				// 2008 (arrivée en cours d'année)
				{
					final List<PeriodeImposition> list = determine(ctb, 2008);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDIPP.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
				}

				// 2009
				{
					final List<PeriodeImposition> list = determine(ctb, 2009);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2009, 1, 1), date(2009, 12, 31), CategorieEnvoiDIPP.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
				}
			}
		});
	}

	@Test
	public void testDetermineArriveeHorsCantonSourcierMixte137Al2() throws Exception {

		final RegDate dateArrivee = date(2008, 9, 25);

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ctb = createArriveeHorsCantonSourcierMixte137Al2(dateArrivee);

				// 2007
				{
					// sourcier pur
					assertEmpty(determine(ctb, 2007));
				}

				// 2008 (départ en cours d'année)
				{
					// sourcier pur jusqu'à l'arrivée, mixte après l'arrivée

					// [UNIREG-1742] contribuables imposés selon le mode mixte, partis dans un autre canton durant l’année et n’ayant aucun
					// rattachement économique -> bien qu’ils soient assujettis de manière illimitée jusqu'au dernier jour du mois de leur départ,
					// leur déclaration d’impôt est remplacée (= elle est optionnelle, en fait, voir exemples à la fin de la spécification) par une
					// note à l’administration fiscale cantonale de leur domicile.
					// --> par analogie, déclaration optionnelle à l'arrivée
					// [UNIREG-2328] l'analogie est fausse, en cas d'arrivée de hors-canton, l'administration fiscale responsable est justement l'administration
					// vaudoise : la déclaration n'est donc ni optionnelle ni remplacée par une note.

					// [SIFISC-7281] pas de fractionnement de l'assujettissement lors de l'arrivée hors-canton de sourcier mixte 137 al 2 => assujetti sur toute l'année
					final List<PeriodeImposition> list = determine(ctb, 2008);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDIPP.VAUDOIS_VAUDTAX, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
				}

				// 2009
				{
					// sourcier mixte
					final List<PeriodeImposition> list = determine(ctb, 2009);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2009, 1, 1), date(2009, 12, 31), CategorieEnvoiDIPP.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
				}
			}
		});
	}

	@Test
	public void testDetermineVenteImmeubleHorsCanton() throws Exception {

		final RegDate dateVente = date(2008, 9, 30);

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ctb = createVenteImmeubleHorsCanton(dateVente);

				// 2007
				{
					final List<PeriodeImposition> list = determine(ctb, 2007);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDIPP.HC_IMMEUBLE, TypeAdresseRetour.OID, false, false, false, false, list.get(0));
				}

				// 2008 (vente de l'immeuble en cours d'année)
				{
					// [UNIREG-1742] pas de déclaration (remplacé par une note à l'administration fiscale de l'autre canton) pour les contribuables domiciliés
					// dans un autre canton dont le rattachement économique (activité indépendante ou immeuble) s’est terminé au cours de la période fiscale
					final List<PeriodeImposition> list = determine(ctb, 2008);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDIPP.HC_IMMEUBLE, TypeAdresseRetour.OID, false, true, false, false, list.get(0));
				}

				// 2009
				{
					// plus assujetti
					assertEmpty(determine(ctb, 2009));
				}
			}
		});
	}

	@Test
	public void testDetermineFinActiviteHorsCanton() throws Exception {

		final RegDate dateFin = date(2008, 9, 30);

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ctb = createFinActiviteHorsCanton(dateFin);

				// 2007
				{
					final List<PeriodeImposition> list = determine(ctb, 2007);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDIPP.HC_ACTIND_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
				}

				// 2008 (fin d'activité indépendante en cours d'année)
				{
					// [UNIREG-1742] pas de déclaration (remplacé par une note à l'administration fiscale de l'autre canton) pour les contribuables domiciliés
					// dans un autre canton dont le rattachement économique (activité indépendante ou immeuble) s’est terminé au cours de la période fiscale
					final List<PeriodeImposition> list = determine(ctb, 2008);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDIPP.HC_ACTIND_COMPLETE, TypeAdresseRetour.CEDI, false, true, false, false, list.get(0));
				}

				// 2009
				{
					// plus assujetti
					assertEmpty(determine(ctb, 2009));
				}
			}
		});
	}

	@Test
	public void testDetermineDiplomateSuisse() throws Exception {

		final RegDate dateNomination = date(2008, 9, 30);

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ctb = createDiplomateSuisse(dateNomination);

				// 2007
				{
					final List<PeriodeImposition> list = determine(ctb, 2007);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDIPP.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
				}

				// 2008 (nomination comme diplomate en cours d'année)
				{
					final List<PeriodeImposition> list = determine(ctb, 2008);
					assertNotNull(list);
					assertEquals(2, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2008, 1, 1), dateNomination.getOneDayBefore(), CategorieEnvoiDIPP.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false,
					                                          list.get(0));
					assertPeriodeImpositionPersonnesPhysiques(dateNomination, date(2008, 12, 31), CategorieEnvoiDIPP.DIPLOMATE_SUISSE, null, false, false, false, false, list.get(1));
				}

				// 2009
				{
					final List<PeriodeImposition> list = determine(ctb, 2009);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2009, 1, 1), date(2009, 12, 31), CategorieEnvoiDIPP.DIPLOMATE_SUISSE, null, false, false, false, false, list.get(0));
				}
			}
		});
	}

	@Test
	public void testDetermineDiplomateSuisseAvecImmeuble() throws Exception {

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique paul = createDiplomateAvecImmeuble(10000052L, date(2004, 1, 1), date(2005, 6, 13));

				// 2003
				{
					final List<PeriodeImposition> list = determine(paul, 2003);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2003, 1, 1), date(2003, 12, 31), CategorieEnvoiDIPP.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
				}

				// 2000 (nomination comme diplomate suisse basé à l'étanger)
				{
					final List<PeriodeImposition> list = determine(paul, 2004);
					assertNotNull(list);
					assertEquals(1, list.size());
					assertPeriodeImpositionPersonnesPhysiques(date(2004, 1, 1), date(2004, 12, 31), CategorieEnvoiDIPP.DIPLOMATE_SUISSE, null, false, false, false, false, list.get(0));
				}

				// 2001 (achat d'un immeuble au 13 juin)
				{
					final List<PeriodeImposition> list = determine(paul, 2005);
					assertNotNull(list);
					assertEquals(1, list.size());
					// [UNIREG-1976] le fait de posséder un immeuble en suisse ne fait plus basculer le diplomate dans la catégorie hors-Suisse: il reste diplomate suisse.
					assertPeriodeImpositionPersonnesPhysiques(date(2005, 1, 1), date(2005, 12, 31), CategorieEnvoiDIPP.DIPLOMATE_SUISSE_IMMEUBLE_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(0));
				}
			}
		});
	}

	/**
	 * [UNIREG-1980] Teste que le type de document pour un indigent qui a reçu des Vaudtax reste Vaudtax.
	 */
	@Test
	public void testDetermineIndigent() throws Exception {

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique ctb = createIndigentAvecDIs(2008, TypeDocument.DECLARATION_IMPOT_VAUDTAX);

				final List<PeriodeImposition> list = determine(ctb, 2009);
				assertNotNull(list);
				assertEquals(1, list.size());
				assertPeriodeImpositionPersonnesPhysiques(date(2009, 1, 1), date(2009, 12, 31), CategorieEnvoiDIPP.VAUDOIS_VAUDTAX, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
			}
		});
	}

	/**
	 * Cas SIFISC-3541
	 */
	@Test
	public void testHcVenteDernierImmeubleEtAchatAnneeSuivante() throws Exception {

		// mise en place des fors
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Gudule", "Tartempion", date(1967, 3, 25), Sexe.FEMININ);
				addForPrincipal(pp, date(2008, 12, 18), MotifFor.ACHAT_IMMOBILIER, MockCommune.Geneve);
				addForSecondaire(pp, date(2008, 12, 18), MotifFor.ACHAT_IMMOBILIER, date(2009, 4, 15), MotifFor.VENTE_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				addForSecondaire(pp, date(2010, 7, 7), MotifFor.ACHAT_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		// calcul des périodes d'imposition
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<PeriodeImposition> pis = determine(pp, 2009);
				assertNotNull(pis);
				assertEquals(1, pis.size());

				final PeriodeImposition pi = pis.get(0);
				assertNotNull(pi);
				assertEquals(date(2009, 1, 1), pi.getDateDebut());
				assertEquals(date(2009, 12, 31), pi.getDateFin());
				assertTrue(pi.isDeclarationRemplaceeParNote());
				return null;
			}
		});
	}

	/**
	 * Cas SIFISC-8490: cas de vente du dernier immeuble avec changement de for principal HC en fin d'année
	 */
	@Test
	public void testHcVenteDernierImmeubleAvecDemenagementPrincipalEnFinAnnee() throws Exception {

		// mise en place des fors
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Mélina", "Dostoïevskaïa", null, Sexe.FEMININ);
				addForPrincipal(pp, date(2008, 12, 18), MotifFor.ACHAT_IMMOBILIER, date(2009, 12, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Sierre);
				addForPrincipal(pp, date(2010, 1, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Bern);
				addForSecondaire(pp, date(2008, 12, 18), MotifFor.ACHAT_IMMOBILIER, date(2009, 4, 15), MotifFor.VENTE_IMMOBILIER, MockCommune.Aubonne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		// calcul de la période d'imposition 2009 qui devrait être "remplacée par note"
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<PeriodeImposition> pis = determine(pp, 2009);
				assertNotNull(pis);
				assertEquals(1, pis.size());

				final PeriodeImposition pi = pis.get(0);
				assertNotNull(pi);
				assertEquals(date(2009, 1, 1), pi.getDateDebut());
				assertEquals(date(2009, 12, 31), pi.getDateFin());
				assertTrue(pi.isDeclarationRemplaceeParNote());
				return null;
			}
		});
	}

	@Test
	public void testNombreCalculsAssujettissementPourCalculDesPeriodesImposition() throws Exception {

		final ValidationService vs = getBean(ValidationService.class, "validationService");
		final AssujettissementServiceImpl assImpl = new AssujettissementServiceImpl();
		assImpl.setValidationService(vs);
		assImpl.setTiersService(tiersService);
		assImpl.afterPropertiesSet();

		// construction d'un service d'assujettissement qui compte le nombre d'appels effectués aux méthodes "determineXXX"
		final MutableInt compteurAppels = new MutableInt(0);
		final AssujettissementService assProxy = (AssujettissementService) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{AssujettissementService.class}, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if (method.getName().startsWith("determine")) {
					compteurAppels.increment();
				}
				return method.invoke(assImpl, args);
			}
		});

		final PeriodeImpositionServiceImpl pis = new PeriodeImpositionServiceImpl();
		pis.setAssujettissementService(assProxy);
		pis.setTiersService(tiersService);
		pis.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		pis.afterPropertiesSet();

		// 2 pf actives pour avoir au moins 2 périodes d'impositions à calculer (et vérifier que le service d'assujettissement n'est bien appelé qu'une fois)
		final int firstYear = 2010;
		Assert.assertTrue("Il faudrait au moins deux pf actives", RegDate.get().year() - firstYear > 1);

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Piotr", "Pietrovitch", null, Sexe.MASCULIN);

				// j'ai pris le mode d'imposition "DEPENSE" afin de ne pas avoir d'effet de bord sur le calcul du type de DI (VAUDTAX vs. COMPLETE)
				// (qui doit parfois recalculer l'assujettissement de l'année précédente)
				addForPrincipal(pp, date(firstYear, 3, 1), MotifFor.ARRIVEE_HS, MockCommune.Aigle, ModeImposition.DEPENSE);

				return pp.getNumero();
			}
		});

		// calcul des périodes d'imposition
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<PeriodeImposition> piList = pis.determine(pp, new DateRangeHelper.Range(date(firstYear + 1, 1, 1), RegDate.get()));
				assertNotNull(piList);
				assertEquals(RegDate.get().year() - firstYear, piList.size());      // qui est plus grand que 1, je le rappelle (voir plus haut)
				assertEquals(1, compteurAppels.intValue());                         // un seul appel pour plusieurs PF -> optim réussie !
				return null;
			}
		});
	}


	/**
	 * [SIFISC-21684] Cas d'un mixte 2 qui se sépare et part HC dès le lendemain (en 2014 ou plus tard)
	 * --> il devrait avoir un assujettissement depuis le début de l'année jusqu'à la fin du mois du départ HC
	 */
	@Transactional(rollbackFor = Throwable.class)
	@Test
	public void testMixte2QuiSeSepareEtPartHCLeLendemain() throws Exception {

		final int annee = 2014;
		final RegDate mariage = date(annee - 4, 1, 1);
		final RegDate separation = date(annee, 4, 17);
		final RegDate departHC = separation.getOneDayAfter();

		final PersonnePhysique pp = createContribuableSansFor();
		final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, mariage, separation);
		final MenageCommun mc = couple.getMenage();

		addForPrincipal(mc, mariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, separation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Bussigny, ModeImposition.MIXTE_137_2);
		addForPrincipal(pp, separation.getOneDayAfter(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, departHC, MotifFor.DEPART_HC, MockCommune.Lausanne, ModeImposition.MIXTE_137_2);
		addForPrincipal(pp, departHC.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Bern, ModeImposition.SOURCE);

		final List<PeriodeImposition> pis = determine(pp, annee);
		assertNotNull(pis);
		assertEquals(1, pis.size());

		final PeriodeImposition pi = pis.get(0);
		assertNotNull(pi);
		assertEquals(date(annee, 1, 1), pi.getDateDebut());
		assertEquals(departHC.getLastDayOfTheMonth(), pi.getDateFin());
	}

	/**
	 * [SIFISC-21684] Cas d'un mixte 2 qui se sépare et part HC dès le surlendemain (en 2014 ou plus tard)
	 * --> il devrait avoir un assujettissement depuis le début de l'année jusqu'à la fin du mois du départ HC
	 */
	@Transactional(rollbackFor = Throwable.class)
	@Test
	public void testMixte2QuiSeSepareEtPartHCLeSurlendemain() throws Exception {

		final int annee = 2014;
		final RegDate mariage = date(annee - 4, 1, 1);
		final RegDate separation = date(annee, 4, 17);
		final RegDate departHC = separation.getOneDayAfter().getOneDayAfter();

		final PersonnePhysique pp = createContribuableSansFor();
		final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, mariage, separation);
		final MenageCommun mc = couple.getMenage();

		addForPrincipal(mc, mariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, separation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Bussigny, ModeImposition.MIXTE_137_2);
		addForPrincipal(pp, separation.getOneDayAfter(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, departHC, MotifFor.DEPART_HC, MockCommune.Lausanne, ModeImposition.MIXTE_137_2);
		addForPrincipal(pp, departHC.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Bern, ModeImposition.SOURCE);

		final List<PeriodeImposition> pis = determine(pp, annee);
		assertNotNull(pis);
		assertEquals(1, pis.size());

		final PeriodeImposition pi = pis.get(0);
		assertNotNull(pi);
		assertEquals(date(annee, 1, 1), pi.getDateDebut());
		assertEquals(departHC.getLastDayOfTheMonth(), pi.getDateFin());
	}
}
