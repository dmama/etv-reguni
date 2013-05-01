package ch.vd.uniregctb.metier.assujettissement;

import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseRetour;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

@SuppressWarnings({"JavaDoc"})
public class PeriodeImpositionServiceTest extends MetierTest {

	private PeriodeImpositionServiceImpl service;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		AssujettissementService as = getBean(AssujettissementService.class, "assujettissementService");
		service = new PeriodeImpositionServiceImpl();
		service.setAssujettissementService(as);
	}

	/**
	 * [UNIREG-1327] Vérifie que la période d'imposition d'un HS qui vend son immeuble ne s'étend pas au delà de la date de vente.
	 * [UNIREG-1742] Vérifie que la dernière déclaration qui découle de la vente de l'immeuble n'est pas optionnelle.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineVenteImmeubleContribuableHorsSuisse() throws Exception {

		final RegDate dateAchat = date(2000, 7, 1);
		final RegDate dateVente = date(2007, 5, 30);
		final Contribuable paul = createHorsSuisseAvecAchatEtVenteImmeuble(dateAchat, dateVente);

		// 2006
		{
			final List<PeriodeImposition> list = service.determine(paul, 2006);
			assertNotNull(list);
			assertEquals(1, list.size());
			// hors-Suisse toute l'année
			assertPeriodeImposition(date(2006, 1, 1), date(2006, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(0));
		}

		// 2007
		{
			final List<PeriodeImposition> list = service.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			// hors-Suisse jusqu'à la date de la vente de l'immeuble
			assertPeriodeImposition(date(2007, 1, 1), dateVente, CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, true, list.get(0)); // [UNIREG-1742] vente de l'immeuble -> déclaration pas optionnelle
		}

		// 2008
		{
			final List<PeriodeImposition> list = service.determine(paul, 2008);
			assertEmpty(list);
		}

		// 2000-2008
		{
			List<PeriodeImposition> list = service.determine(paul, RANGE_2000_2008);
			assertNotNull(list);
			assertEquals(8, list.size());
			assertPeriodeImposition(dateAchat, date(2000, 12, 31), CategorieEnvoiDI.HS_VAUDTAX, TypeAdresseRetour.CEDI, true, false, false, false, list.get(0)); // [UNIREG-1742] la DI est optionnelle dès la première année
			assertPeriodeImposition(date(2001, 1, 1), date(2001, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(1));
			assertPeriodeImposition(date(2002, 1, 1), date(2002, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(2));
			assertPeriodeImposition(date(2003, 1, 1), date(2003, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(3));
			assertPeriodeImposition(date(2004, 1, 1), date(2004, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(4));
			assertPeriodeImposition(date(2005, 1, 1), date(2005, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(5));
			assertPeriodeImposition(date(2006, 1, 1), date(2006, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(6));
			assertPeriodeImposition(date(2007, 1, 1), dateVente, CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, true, list.get(7));
		}
	}

	/**
	 * [UNIREG-1742] En cas d'achats et de ventes d'immeuble multiples pour un hors-Suisse, il y a plusieurs
	 * périodes d'imposition et plusieurs déclarations (confirmé par Thierry Declercq le 18 décembre 2009)
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineAchatsEtVentesMultipleHorsSuisse() throws Exception {

		final DateRangeHelper.Range immeuble1 = new DateRangeHelper.Range(date(2008, 1, 15), date(2008, 3, 30));
		final DateRangeHelper.Range immeuble2 = new DateRangeHelper.Range(date(2008, 6, 2), date(2008, 11, 26));
		final Contribuable ctb = createHorsSuisseAvecAchatsEtVentesImmeubles(immeuble1, immeuble2);

		// 2007
		{
			assertEmpty(service.determine(ctb, 2007));
		}

		// 2008
		{
			final List<PeriodeImposition> list = service.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertPeriodeImposition(immeuble1.getDateDebut(), immeuble1.getDateFin(), CategorieEnvoiDI.HS_VAUDTAX, TypeAdresseRetour.CEDI, false, false, false, true, list.get(0)); // pas optionnelle car vente de l'immeuble
			assertPeriodeImposition(immeuble2.getDateDebut(), immeuble2.getDateFin(), CategorieEnvoiDI.HS_VAUDTAX, TypeAdresseRetour.CEDI, false, false, false, true, list.get(1)); // pas optionnelle car vente de l'immeuble
		}

		// 2009
		{
			assertEmpty(service.determine(ctb, 2009));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineArriveeHorsSuisseAvecImmeuble() throws Exception {

		final RegDate dateArrivee = date(2007, 3, 1);
		final Contribuable paul = createArriveeHorsSuisseAvecImmeuble(dateArrivee);

		// 2006
		{
			final List<PeriodeImposition> list = service.determine(paul, 2006);
			assertNotNull(list);
			assertEquals(1, list.size());
			// hors-Suisse toute l'année
			assertPeriodeImposition(date(2006, 1, 1), date(2006, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(0));
		}

		// 2007
		{
			final List<PeriodeImposition> list = service.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			// hors-Suisse avant l'arrivée et ordinaire ensuite, mais les deux assujettissements se confondent en un au niveau de la période
			// d'imposition puisque les types de DI sont les mêmes.
			assertPeriodeImposition(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDI.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
		}

		// 2008
		{
			final List<PeriodeImposition> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// ordinaire toute l'année
			assertPeriodeImposition(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDI.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
		}

		// 2000-2008
		{
			final List<PeriodeImposition> list = service.determine(paul, RANGE_2000_2008);
			assertNotNull(list);
			assertEquals(9, list.size());
			assertPeriodeImposition(date(2000, 1, 1), date(2000, 12, 31), CategorieEnvoiDI.HS_VAUDTAX, TypeAdresseRetour.CEDI, true, false, false, false, list.get(0)); // [UNIREG-1742] la DI est optionnelle dès la première année
			assertPeriodeImposition(date(2001, 1, 1), date(2001, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(1));
			assertPeriodeImposition(date(2002, 1, 1), date(2002, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(2));
			assertPeriodeImposition(date(2003, 1, 1), date(2003, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(3));
			assertPeriodeImposition(date(2004, 1, 1), date(2004, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(4));
			assertPeriodeImposition(date(2005, 1, 1), date(2005, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(5));
			assertPeriodeImposition(date(2006, 1, 1), date(2006, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(6));
			assertPeriodeImposition(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDI.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(7));
			assertPeriodeImposition(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDI.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(8));
		}
	}

	/**
	 * [UNIREG-1327] Vérifie que la période d'imposition d'un contribuable HS qui possède un immeuble, arrive de HS et vend son immeuble dans la
	 * même année est bien fractionné à la date d'arrivée HS.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineArriveeHorsSuisseEtVenteImmeubleDansLAnnee() throws Exception {

		final RegDate dateArrivee = date(2007, 3, 1);
		final RegDate dateVente = date(2007, 5, 30);
		final Contribuable paul = createArriveeHorsSuisseEtVenteImmeuble(dateArrivee, dateVente);

		// 2006
		{
			final List<PeriodeImposition> list = service.determine(paul, 2006);
			assertNotNull(list);
			assertEquals(1, list.size());
			// hors-Suisse toute l'année
			assertPeriodeImposition(date(2006, 1, 1), date(2006, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(0));
		}

		// 2007
		{
			final List<PeriodeImposition> list = service.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			// hors-Suisse avant l'arrivée et ordinaire ensuite, mais les deux assujettissements se confondent en un au niveau de la période
			// d'imposition puisque les types de DI sont les mêmes.
			assertPeriodeImposition(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDI.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
		}

		// 2008
		{
			final List<PeriodeImposition> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// ordinaire toute l'année
			assertPeriodeImposition(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDI.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
		}

		// 2000-2008
		{
			final List<PeriodeImposition> list = service.determine(paul, RANGE_2000_2008);
			assertNotNull(list);
			assertEquals(9, list.size());
			assertPeriodeImposition(date(2000, 1, 1), date(2000, 12, 31), CategorieEnvoiDI.HS_VAUDTAX, TypeAdresseRetour.CEDI, true, false, false, false, list.get(0)); // [UNIREG-1742] la DI est optionnelle dès la première année
			assertPeriodeImposition(date(2001, 1, 1), date(2001, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(1));
			assertPeriodeImposition(date(2002, 1, 1), date(2002, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(2));
			assertPeriodeImposition(date(2003, 1, 1), date(2003, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(3));
			assertPeriodeImposition(date(2004, 1, 1), date(2004, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(4));
			assertPeriodeImposition(date(2005, 1, 1), date(2005, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(5));
			assertPeriodeImposition(date(2006, 1, 1), date(2006, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(6));
			assertPeriodeImposition(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDI.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(7));
			assertPeriodeImposition(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDI.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(8));
		}
	}

	/**
	 * Cas très spécial du contribuable qui arrive de HS et qui repart HS la même année, et qui achète un immeuble entre-deux. La période
	 * d'imposition doit commencer à la date d'arrivée et - puisqu'il y a un immeuble - s'étendre jusqu'à la fin de l'année.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineArriveeHorsSuisseAchatImmeubleEtDepartHorsSuisseDansLAnnee() throws Exception {

		final RegDate dateArrivee = date(2007, 3, 1);
		final RegDate dateAchat = date(2007, 5, 30);
		final RegDate dateDepart = date(2007, 12, 8);
		final Contribuable paul = createArriveeHSAchatImmeubleEtDepartHS(dateArrivee, dateAchat, dateDepart);

		// 2006
		{
			final List<PeriodeImposition> list = service.determine(paul, 2006);
			assertEmpty(list);
		}

		// 2007
		{
			final List<PeriodeImposition> list = service.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			// hors-Suisse non-assujetti avant l'arrivée, ordinaire ensuite puis de nouveau hors-Suisse mais assujetti suite au départ ->
			// deux assujettissements (ordinaire + hors-Suisse) qui se confondent en un au niveau de la période d'imposition puisque les
			// types de DI sont les mêmes.
			assertPeriodeImposition(dateArrivee, date(2007, 12, 31), CategorieEnvoiDI.HS_VAUDTAX, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
		}

		// 2008
		{
			final List<PeriodeImposition> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// hors-Suisse toute l'année
			assertPeriodeImposition(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(0));
		}

		// 2000-2008
		{
			final List<PeriodeImposition> list = service.determine(paul, RANGE_2000_2008);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertPeriodeImposition(dateArrivee, date(2007, 12, 31), CategorieEnvoiDI.HS_VAUDTAX, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
			assertPeriodeImposition(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(1));
		}
	}

	/**
	 * Cas très spécial du contribuable qui arrive de HS et qui repart HS la même année, et qui achète un immeuble après son départ. Il doit
	 * y avoir deux périodes d'imposition distinctes : une pour sa présence en Suisse, et une autre pour son immeuble acheté plus tard.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineArriveeHorsSuisseEtDepartHorsSuissePuisAchatImmeubleDansLAnnee() throws Exception {

		final RegDate dateArrivee = date(2007, 2, 1);
		final RegDate dateDepart = date(2007, 7, 30);
		final RegDate dateAchat = date(2007, 10, 8);
		final Contribuable paul = createArriveeHSDepartHSPuisAchatImmeuble(dateArrivee, dateDepart, dateAchat);

		// 2006
		{
			final List<PeriodeImposition> list = service.determine(paul, 2006);
			assertEmpty(list);
		}

		// 2007
		{
			final List<PeriodeImposition> list = service.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(2, list.size());
			// assujetti comme ordinaire pendant son passage en suisse
			assertPeriodeImposition(dateArrivee, dateDepart, CategorieEnvoiDI.VAUDOIS_VAUDTAX, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
			// assujetti comme hors-Suisse suite à l'achat de son immeuble
			// [UNIREG-1742] la déclaration est optionnelle dans ce cas (HS avec immeuble)
			assertPeriodeImposition(dateAchat, date(2007, 12, 31), CategorieEnvoiDI.HS_VAUDTAX, TypeAdresseRetour.CEDI, true, false, false, false, list.get(1));
		}

		// 2008
		{
			final List<PeriodeImposition> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// hors-Suisse toute l'année
			assertPeriodeImposition(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(0));
		}

		// 2000-2008
		{
			final List<PeriodeImposition> list = service.determine(paul, RANGE_2000_2008);
			assertNotNull(list);
			assertEquals(3, list.size());
			assertPeriodeImposition(dateArrivee, dateDepart, CategorieEnvoiDI.VAUDOIS_VAUDTAX, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
			assertPeriodeImposition(dateAchat, date(2007, 12, 31), CategorieEnvoiDI.HS_VAUDTAX, TypeAdresseRetour.CEDI, true, false, false, false, list.get(1));
			assertPeriodeImposition(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(2));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDepartHorsSuisseDansLAnneeAvecImmeuble() throws Exception {

		final RegDate dateDepart = date(2007, 6, 30);
		final Contribuable paul = createDepartHorsSuisseAvecImmeuble(dateDepart);

		// 2006 (vaudois ordinaire)
		{
			final List<PeriodeImposition> list = service.determine(paul, 2006);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2006, 1, 1), date(2006, 12, 31), CategorieEnvoiDI.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
		}

		// 2007 (départ hors-Suisse dans l'année)
		{
			final List<PeriodeImposition> list = service.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			// vaudois ordinaire jusqu'au départ puis hors-Suisse, mais les deux périodes se fusionnent car les types de document sont les mêmes
			assertPeriodeImposition(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
		}

		// 2008 (hors-Suisse)
		{
			final List<PeriodeImposition> list = service.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// vaudois ordinaire jusqu'au départ puis hors-Suisse, mais les deux périodes se fusionnent car les types de document sont les mêmes
			assertPeriodeImposition(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(0));
		}

		// 2006-2008
		{
			final List<PeriodeImposition> list = service.determine(paul, RANGE_2006_2008);
			assertNotNull(list);
			assertEquals(3, list.size());
			assertPeriodeImposition(date(2006, 1, 1), date(2006, 12, 31), CategorieEnvoiDI.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
			assertPeriodeImposition(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(1));
			assertPeriodeImposition(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(2));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDepartHorsSuisseDepuisHorsCantonAvecImmeuble() throws Exception {

		final Contribuable ctb = createDepartHorsSuisseDepuisHorsCantonAvecImmeuble(date(2007, 6, 30));

		// 2006 (hors-canton)
		{
			final List<PeriodeImposition> list = service.determine(ctb, 2006);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2006, 1, 1), date(2006, 12, 31), CategorieEnvoiDI.HC_IMMEUBLE, TypeAdresseRetour.OID, false, false, false, false, list.get(0));
		}

		// 2007 (hors-canton -> hors-Suisse)
		{
			final List<PeriodeImposition> list = service.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			// [UNIREG-1742] le départ hors-Suisse depuis hors-canton ne doit pas fractionner la période d'assujettissement (car le rattachement économique n'est pas interrompu)
			assertPeriodeImposition(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(0));
		}

		// 2008 (hors-Suisse)
		{
			final List<PeriodeImposition> list = service.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// vaudois ordinaire jusqu'au départ puis hors-Suisse, mais les deux périodes se fusionnent car les types de document sont les mêmes
			assertPeriodeImposition(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(0));
		}

		// 2006-2008
		{
			final List<PeriodeImposition> list = service.determine(ctb, RANGE_2006_2008);
			assertNotNull(list);
			assertEquals(3, list.size());
			assertPeriodeImposition(date(2006, 1, 1), date(2006, 12, 31), CategorieEnvoiDI.HC_IMMEUBLE, TypeAdresseRetour.OID, false, false, false, false, list.get(0));
			assertPeriodeImposition(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(1));
			assertPeriodeImposition(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(2));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDepartHorsSuisseDepuisHorsCantonAvecActiviteIndependante() throws Exception {

		final Contribuable ctb = createDepartHorsSuisseDepuisHorsCantonAvecActiviteIndependante(date(2007, 6, 30));

		// 2006 (hors-canton)
		{
			final List<PeriodeImposition> list = service.determine(ctb, 2006);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2006, 1, 1), date(2006, 12, 31), CategorieEnvoiDI.HC_ACTIND_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
		}

		// 2007 (hors-canton -> hors-Suisse)
		{
			final List<PeriodeImposition> list = service.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			// [UNIREG-1742] le départ hors-Suisse depuis hors-canton ne doit pas fractionner la période d'assujettissement (car le rattachement économique n'est pas interrompu)
			assertPeriodeImposition(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
		}

		// 2008 (hors-Suisse)
		{
			final List<PeriodeImposition> list = service.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// vaudois ordinaire jusqu'au départ puis hors-Suisse, mais les deux périodes se fusionnent car les types de document sont les mêmes
			assertPeriodeImposition(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
		}

		// 2006-2008
		{
			final List<PeriodeImposition> list = service.determine(ctb, RANGE_2006_2008);
			assertNotNull(list);
			assertEquals(3, list.size());
			assertPeriodeImposition(date(2006, 1, 1), date(2006, 12, 31), CategorieEnvoiDI.HC_ACTIND_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
			assertPeriodeImposition(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(1));
			assertPeriodeImposition(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(2));
		}
	}

	/**
	 * [UNIREG-1390] Vérifie qu'il est possible de déterminer la période d'imposition d'un hors-Suisse qui vend son immeuble et dont le for
	 * principal est fermé sans motif (cas du ctb n°807.110.03).
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineHorsForPrincipalFermeSansMotif() throws Exception {

		final RegDate dateVente = date(2009, 3, 24);
		final Contribuable ctb = createHorsSuisseVenteImmeubleEtFermetureFFPSansMotif(dateVente);
		assertNotNull(ctb);

		final List<PeriodeImposition> list = service.determine(ctb, 2009);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertPeriodeImposition(date(2009, 1, 1), dateVente, CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, true, list.get(0));
	}
	
	/**
	 * [UNIREG-1756] Vérifie que le type de DI est vaudtax pour un sourcier qui passe à l'ordinaire.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineSourcierPassantOrdinaire() throws Exception
	{
		final RegDate dateChangement = date(2008, 9, 1);
		final Contribuable ctb = createSourcierPur();
		// Fermeture du dernier for à la source source
		ForFiscalPrincipal ffp = ctb.getForFiscalPrincipalAt(null);
		ffp.setDateFin(dateChangement.getOneDayBefore());
		ffp.setMotifFermeture(MotifFor.CHGT_MODE_IMPOSITION);
		// Passage à l'ordinaire
		addForPrincipal(ctb, dateChangement, MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne);
		
		final List<PeriodeImposition> list = service.determine(ctb, dateChangement.year());
		assertNotNull(list);
		assertEquals(1, list.size());
		assertPeriodeImposition(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDI.VAUDOIS_VAUDTAX, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
	}

	/**
	 * [UNIREG-1741]
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineVaudoisDepense() throws Exception {

		final RegDate dateArrivee = date(1990, 4, 13);
		final RegDate dateDeces = date(2009, 2, 23);
		final Contribuable ctb = createDecesVaudoisDepense(dateArrivee, dateDeces);
		assertNotNull(ctb);

		// 2007
		{
			final List<PeriodeImposition> list = service.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDI.VAUDOIS_DEPENSE, TypeAdresseRetour.OID, false, false, false, false, list.get(0));
		}

		// 2008
		{
			final List<PeriodeImposition> list = service.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDI.VAUDOIS_DEPENSE, TypeAdresseRetour.OID, false, false, false, false, list.get(0));
		}

		// 2009 (décès)
		{
			final List<PeriodeImposition> list = service.determine(ctb, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2009, 1, 1), dateDeces, CategorieEnvoiDI.VAUDOIS_DEPENSE, TypeAdresseRetour.ACI, false, false, true, false, list.get(0));
		}
	}

	/**
	 * [UNIREG-1360] Vérifie que la période d'imposition s'arrête bien à la date du décès dans le cas d'un contribuable hors-canton qui possèdait un immeuble dans le canton.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDecesHorsCantonImmeuble() throws Exception {

		final RegDate dateAchat = date(1990, 4, 13);
		final RegDate dateDeces = date(2009, 2, 23);
		final Contribuable ctb = createDecesHorsCantonAvecImmeuble(dateAchat, dateDeces);
		assertNotNull(ctb);

		// 2007
		{
			final List<PeriodeImposition> list = service.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDI.HC_IMMEUBLE, TypeAdresseRetour.OID, false, false, false, false, list.get(0));
		}

		// 2008
		{
			final List<PeriodeImposition> list = service.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDI.HC_IMMEUBLE, TypeAdresseRetour.OID, false, false, false, false, list.get(0));
		}

		// 2009 (décès)
		{
			// [SIFISC-7636] la déclaration d'impôt n'est ni optionnelle ni remplacée par une note pour les contribuables domiciliés dans un autre canton dont le rattachement économique
			// (activité indépendante ou immeuble) s’est terminé au cours de la période fiscale pour cause de décès
			final List<PeriodeImposition> list = service.determine(ctb, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2009, 1, 1), dateDeces, CategorieEnvoiDI.HC_IMMEUBLE, TypeAdresseRetour.ACI, false, false, true, false, list.get(0));
		}
	}

	/**
	 * [UNIREG-1360] Vérifie que la déclaration d'impôt reste bien remplacée par une note dans le cas où un contribuable hors-canton décède *même* s'il avait vendu son dernier immeuble dans l'année.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDecesHorsCantonAvecVenteImmeubleEnDebutDAnnee() throws Exception {

		final RegDate dateAchat = date(1990, 4, 13);
		final RegDate dateVente = date(2009, 2, 23);
		final RegDate dateDeces = date(2009, 6, 3);

		final Contribuable ctb = createContribuableSansFor(null);
		addForPrincipal(ctb, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Neuchatel);
		addForSecondaire(ctb, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		assertNotNull(ctb);

		// 2007
		{
			final List<PeriodeImposition> list = service.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDI.HC_IMMEUBLE, TypeAdresseRetour.OID, false, false, false, false, list.get(0));
		}

		// 2008
		{
			final List<PeriodeImposition> list = service.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDI.HC_IMMEUBLE, TypeAdresseRetour.OID, false, false, false, false, list.get(0));
		}

		// 2009 (décès)
		{
			// [SIFISC-7636] la déclaration d'impôt n'est ni optionnelle mais bien remplacée par une note dans ce cas-là (vente de l'immeuble avant décès)
			final List<PeriodeImposition> list = service.determine(ctb, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2009, 1, 1), dateDeces, CategorieEnvoiDI.HC_IMMEUBLE, TypeAdresseRetour.ACI, false, true, true, false, list.get(0));
		}
	}

	/**
	 * [UNIREG-1742] Vérifie que la dernière déclaration qui découle du décès du contribuable n'est pas optionnelle.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDecesHorsSuisseImmeuble() throws Exception {

		final RegDate dateAchat = date(2000, 7, 1);
		final RegDate dateDeces = date(2007, 5, 30);
		final Contribuable paul = createDecesHorsSuisseAvecImmeuble(dateAchat, dateDeces);

		// 2006
		{
			final List<PeriodeImposition> list = service.determine(paul, 2006);
			assertNotNull(list);
			assertEquals(1, list.size());
			// hors-Suisse toute l'année
			assertPeriodeImposition(date(2006, 1, 1), date(2006, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(0));
		}

		// 2007 (décès)
		{
			final List<PeriodeImposition> list = service.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			// hors-Suisse jusqu'à la date du décès
			assertPeriodeImposition(date(2007, 1, 1), dateDeces, CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.ACI, false, false, true, false, list.get(0)); // [UNIREG-1742] décès -> déclaration pas optionnelle
		}

		// 2008
		{
			final List<PeriodeImposition> list = service.determine(paul, 2008);
			assertEmpty(list);
		}

		// 2000-2008
		{
			List<PeriodeImposition> list = service.determine(paul, RANGE_2000_2008);
			assertNotNull(list);
			assertEquals(8, list.size());
			assertPeriodeImposition(dateAchat, date(2000, 12, 31), CategorieEnvoiDI.HS_VAUDTAX, TypeAdresseRetour.CEDI, true, false, false, false, list.get(0));
			assertPeriodeImposition(date(2001, 1, 1), date(2001, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(1));
			assertPeriodeImposition(date(2002, 1, 1), date(2002, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(2));
			assertPeriodeImposition(date(2003, 1, 1), date(2003, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(3));
			assertPeriodeImposition(date(2004, 1, 1), date(2004, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(4));
			assertPeriodeImposition(date(2005, 1, 1), date(2005, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(5));
			assertPeriodeImposition(date(2006, 1, 1), date(2006, 12, 31), CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(6));
			assertPeriodeImposition(date(2007, 1, 1), dateDeces, CategorieEnvoiDI.HS_COMPLETE, TypeAdresseRetour.ACI, false, false, true, false, list.get(7));
		}
	}

	/**
	 * [UNIREG-1741]
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDecesHorsCantonActiviteIndependante() throws Exception {

		final RegDate debutExploitation = date(1990, 4, 13);
		final RegDate dateDeces = date(2009, 2, 23);
		final Contribuable ctb = createDecesHorsCantonActiviteIndependante(debutExploitation, dateDeces);
		assertNotNull(ctb);

		// 2007
		{
			final List<PeriodeImposition> list = service.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDI.HC_ACTIND_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
		}

		// 2008
		{
			final List<PeriodeImposition> list = service.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDI.HC_ACTIND_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
		}

		// 2009 (décès)
		{
			// [SIFISC-7636] la déclaration d'impôt n'est ni optionnelle ni remplacée par une note pour les contribuables domiciliés dans un autre canton dont le rattachement économique
			// (activité indépendante ou immeuble) s’est terminé au cours de la période fiscale pour cause de décès
			final List<PeriodeImposition> list = service.determine(ctb, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2009, 1, 1), dateDeces, CategorieEnvoiDI.HC_ACTIND_COMPLETE, TypeAdresseRetour.ACI, false, false, true, false, list.get(0));
		}
	}

	/**
	 * [UNIREG-1741]
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDecesVaudoisOrdinaire() throws Exception {

		final RegDate debutExploitation = date(1990, 4, 13);
		final RegDate dateDeces = date(2009, 2, 23);
		final Contribuable ctb = createDecesVaudoisOrdinaire(debutExploitation, dateDeces);
		assertNotNull(ctb);

		// 2007
		{
			final List<PeriodeImposition> list = service.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDI.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
		}

		// 2008
		{
			final List<PeriodeImposition> list = service.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDI.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
		}

		// 2009 (décès)
		{
			final List<PeriodeImposition> list = service.determine(ctb, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2009, 1, 1), dateDeces, CategorieEnvoiDI.VAUDOIS_COMPLETE, TypeAdresseRetour.ACI, false, false, true, false, list.get(0));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDepartHorsCantonSourcierPur() throws Exception {

		final Contribuable ctb = createDepartHorsCantonSourcierPur(date(2008, 9, 25));

		// sourcier pur -> pas de déclaration d'impôt ordinaire
		assertEmpty(service.determine(ctb, 2007));
		assertEmpty(service.determine(ctb, 2008));
		assertEmpty(service.determine(ctb, 2009));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDepartHorsCantonSourcierMixte137Al1AvecImmeuble() throws Exception {

		final Contribuable ctb = createDepartHorsCantonSourcierMixte137Al1_Invalide(date(2008, 9, 25));

		// 2007
		{
			final List<PeriodeImposition> list = service.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDI.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
		}

		// 2008 (départ en cours d'année)
		{
			final List<PeriodeImposition> list = service.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDI.HC_IMMEUBLE, TypeAdresseRetour.OID, false, false, false, false, list.get(0));
		}

		// 2009
		{
			final List<PeriodeImposition> list = service.determine(ctb, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2009, 1, 1), date(2009, 12, 31), CategorieEnvoiDI.HC_IMMEUBLE, TypeAdresseRetour.OID, false, false, false, false, list.get(0));
		}
	}

	// [SIFISC-62] On s'assure que la DI d'un sourcier mixte 1 sans immeuble qui part hors-canton est bien optionnelle
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDepartHorsCantonSourcierMixte137Al1SansImmeuble() throws Exception {

		final RegDate dateDepart = date(2008, 9, 25);
		final Contribuable ctb = createDepartHorsCantonSourcierMixte137Al1(dateDepart);

		// 2007
		{
			// sourcier mixte
			final List<PeriodeImposition> list = service.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDI.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
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
			assertEmpty(service.determine(ctb, 2008));
		}

		// 2009
		{
			// sourcier pur
			assertEmpty(service.determine(ctb, 2009));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDepartHorsCantonSourcierMixte137Al2() throws Exception {

		final RegDate dateDepart = date(2008, 9, 25);
		final Contribuable ctb = createDepartHorsCantonSourcierMixte137Al2(dateDepart);

		// 2007
		{
			// sourcier mixte
			final List<PeriodeImposition> list = service.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDI.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
		}

		// 2008 (départ en cours d'année)
		{
			// sourcier mixte jusqu'au départ, sourcier pure après le départ

			// [UNIREG-1742] contribuables imposés selon le mode mixte, partis dans un autre canton durant l’année et n’ayant aucun
			// rattachement économique -> bien qu’ils soient assujettis de manière illimitée jusqu'au dernier jour du mois de leur départ,
			// leur déclaration d’impôt est remplacée (= elle est optionnelle, en fait, voir exemples à la fin de la spécification) par une
			// note à l’administration fiscale cantonale de leur domicile.
			final List<PeriodeImposition> list = service.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2008, 1, 1), dateDepart, CategorieEnvoiDI.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, true, true, false, false, list.get(0));
		}

		// 2009
		{
			// sourcier pur
			assertEmpty(service.determine(ctb, 2009));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineArriveeHorsCantonSourcierPur() throws Exception {

		final Contribuable ctb = createArriveeHorsCantonSourcierPur(date(2008, 9, 25));

		// sourcier pur -> pas de déclaration d'impôt ordinaire
		assertEmpty(service.determine(ctb, 2007));
		assertEmpty(service.determine(ctb, 2008));
		assertEmpty(service.determine(ctb, 2009));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineArriveeHorsCantonSourcierMixte137Al1() throws Exception {

		final Contribuable ctb = createArriveeHorsCantonSourcierMixte137Al1_Invalide(date(2008, 9, 25));

		// 2007
		{
			final List<PeriodeImposition> list = service.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDI.HC_IMMEUBLE, TypeAdresseRetour.OID, false, false, false, false, list.get(0));
		}

		// 2008 (arrivée en cours d'année)
		{
			final List<PeriodeImposition> list = service.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDI.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
		}

		// 2009
		{
			final List<PeriodeImposition> list = service.determine(ctb, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2009, 1, 1), date(2009, 12, 31), CategorieEnvoiDI.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineArriveeHorsCantonSourcierMixte137Al2() throws Exception {

		final RegDate dateArrivee = date(2008, 9, 25);
		final Contribuable ctb = createArriveeHorsCantonSourcierMixte137Al2(dateArrivee);

		// 2007
		{
			// sourcier pur
			assertEmpty(service.determine(ctb, 2007));
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
			final List<PeriodeImposition> list = service.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDI.VAUDOIS_VAUDTAX, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
		}

		// 2009
		{
			// sourcier mixte
			final List<PeriodeImposition> list = service.determine(ctb, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2009, 1, 1), date(2009, 12, 31), CategorieEnvoiDI.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineVenteImmeubleHorsCanton() throws Exception {

		final RegDate dateVente = date(2008, 9, 30);
		final Contribuable ctb = createVenteImmeubleHorsCanton(dateVente);

		// 2007
		{
			final List<PeriodeImposition> list = service.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDI.HC_IMMEUBLE, TypeAdresseRetour.OID, false, false, false, false, list.get(0));
		}

		// 2008 (vente de l'immeuble en cours d'année)
		{
			// [UNIREG-1742] pas de déclaration (remplacé par une note à l'administration fiscale de l'autre canton) pour les contribuables domiciliés
			// dans un autre canton dont le rattachement économique (activité indépendante ou immeuble) s’est terminé au cours de la période fiscale
			final List<PeriodeImposition> list = service.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDI.HC_IMMEUBLE, TypeAdresseRetour.OID, false, true, false, false, list.get(0));
		}

		// 2009
		{
			// plus assujetti
			assertEmpty(service.determine(ctb, 2009));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineFinActiviteHorsCanton() throws Exception {

		final RegDate dateFin = date(2008, 9, 30);
		final Contribuable ctb = createFinActiviteHorsCanton(dateFin);

		// 2007
		{
			final List<PeriodeImposition> list = service.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDI.HC_ACTIND_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
		}

		// 2008 (fin d'activité indépendante en cours d'année)
		{
			// [UNIREG-1742] pas de déclaration (remplacé par une note à l'administration fiscale de l'autre canton) pour les contribuables domiciliés
			// dans un autre canton dont le rattachement économique (activité indépendante ou immeuble) s’est terminé au cours de la période fiscale
			final List<PeriodeImposition> list = service.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2008, 1, 1), date(2008, 12, 31), CategorieEnvoiDI.HC_ACTIND_COMPLETE, TypeAdresseRetour.CEDI, false, true, false, false, list.get(0));
		}

		// 2009
		{
			// plus assujetti
			assertEmpty(service.determine(ctb, 2009));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDiplomateSuisse() throws Exception {

		final RegDate dateNomination = date(2008, 9, 30);
		final Contribuable ctb = createDiplomateSuisse(dateNomination);

		// 2007
		{
			final List<PeriodeImposition> list = service.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2007, 1, 1), date(2007, 12, 31), CategorieEnvoiDI.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
		}

		// 2008 (nomination comme diplomate en cours d'année)
		{
			final List<PeriodeImposition> list = service.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertPeriodeImposition(date(2008, 1, 1), dateNomination.getOneDayBefore(), CategorieEnvoiDI.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
			assertPeriodeImposition(dateNomination, date(2008, 12, 31), CategorieEnvoiDI.DIPLOMATE_SUISSE, null, false, false, false, false, list.get(1));
		}

		// 2009
		{
			final List<PeriodeImposition> list = service.determine(ctb, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2009, 1, 1), date(2009, 12, 31), CategorieEnvoiDI.DIPLOMATE_SUISSE, null, false, false, false, false, list.get(0));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineDiplomateSuisseAvecImmeuble() throws Exception {

		final Contribuable paul = createDiplomateAvecImmeuble(10000052L, date(2000, 1, 1), date(2001, 6, 13));

		// 1999
		{
			final List<PeriodeImposition> list = service.determine(paul, 1999);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(1999, 1, 1), date(1999, 12, 31), CategorieEnvoiDI.VAUDOIS_COMPLETE, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
		}

		// 2000 (nomination comme diplomate suisse basé à l'étanger)
		{
			final List<PeriodeImposition> list = service.determine(paul, 2000);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertPeriodeImposition(date(2000, 1, 1), date(2000, 12, 31), CategorieEnvoiDI.DIPLOMATE_SUISSE, null, false, false, false, false, list.get(0));
		}

		// 2001 (achat d'un immeuble au 13 juin)
		{
			final List<PeriodeImposition> list = service.determine(paul, 2001);
			assertNotNull(list);
			assertEquals(1, list.size());
			// [UNIREG-1976] le fait de posséder un immeuble en suisse ne fait plus basculer le diplomate dans la catégorie hors-Suisse: il reste diplomate suisse.
			assertPeriodeImposition(date(2001, 1, 1), date(2001, 12, 31), CategorieEnvoiDI.DIPLOMATE_SUISSE_IMMEUBLE_COMPLETE, TypeAdresseRetour.CEDI, true, false, false, false, list.get(0));
		}
	}

	/**
	 * [UNIREG-1980] Teste que le type de document pour un indigent qui a reçu des Vaudtax reste Vaudtax.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineIndigent() throws Exception {

		addCollAdm(MockCollectiviteAdministrative.CEDI);
		final Contribuable ctb = createIndigentAvecDIs(2008, TypeDocument.DECLARATION_IMPOT_VAUDTAX);

		final List<PeriodeImposition> list = service.determine(ctb, 2009);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertPeriodeImposition(date(2009, 1, 1), date(2009, 12, 31), CategorieEnvoiDI.VAUDOIS_VAUDTAX, TypeAdresseRetour.CEDI, false, false, false, false, list.get(0));
	}

	/**
	 * [UNIREG-820] [UNIREG-1824] Teste l'algorithme de détermination du format (VaudTax ou complète) pour une déclaration ordinaire
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetermineFormatDIOrdinaire() throws Exception {

		addCollAdm(MockCollectiviteAdministrative.CEDI);
		final PeriodeFiscale periode2005 = addPeriodeFiscale(2005);
		final ModeleDocument modeleComplete2005 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2005);
		final ModeleDocument modeleVaudTax2005 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2005);

		// un contribuable assujetti depuis le 1er janvier 2005 sur Lausanne
		final PersonnePhysique pp = addNonHabitant("Frédéric", "Pochin", date(1987, 1, 1), Sexe.MASCULIN);
		addForPrincipal(pp, date(2005, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);

		// aucune déclaration et nouvellement assujetti => 2005 = vaudtax
		assertEquals(FormatDIOrdinaire.VAUDTAX, service.determineFormatDIOrdinaire(pp, 2005));

		// aucune déclaration et assujetti depuis 1 année => 2006 = complète
		assertEquals(FormatDIOrdinaire.COMPLETE, service.determineFormatDIOrdinaire(pp, 2006));

		// une déclaration en 2005 complète, pas de déclaration en 2006 ni 2007 => 2006 = complète
		final DeclarationImpotOrdinaire diComplete = addDeclarationImpot(pp, periode2005, date(2005, 1, 1), date(2005, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modeleComplete2005);
		assertEquals(FormatDIOrdinaire.COMPLETE, service.determineFormatDIOrdinaire(pp, 2006));

		// une déclaration en 2005 complète, pas de déclaration en 2006 ni 2007 => 2007 = complète
		assertEquals(FormatDIOrdinaire.COMPLETE, service.determineFormatDIOrdinaire(pp, 2007));

		// une déclaration en 2005 complète, pas de déclaration en 2006 ni 2007 => 2008 = complète
		assertEquals(FormatDIOrdinaire.COMPLETE, service.determineFormatDIOrdinaire(pp, 2008));

		// une déclaration en 2005 VaudTax, pas de déclaration en 2006 ni 2007 => 2006 = vaudtax
		diComplete.setAnnule(true);
		addDeclarationImpot(pp, periode2005, date(2005, 1, 1), date(2005, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modeleVaudTax2005);
		assertEquals(FormatDIOrdinaire.VAUDTAX, service.determineFormatDIOrdinaire(pp, 2006));

		// une déclaration en 2005 VaudTax, pas de déclaration en 2006 ni 2007 => 2007 = vaudtax
		assertEquals(FormatDIOrdinaire.VAUDTAX, service.determineFormatDIOrdinaire(pp, 2007));

		// une déclaration en 2005 VaudTax, pas de déclaration en 2006 ni 2007 => 2008 = complète
		assertEquals(FormatDIOrdinaire.COMPLETE, service.determineFormatDIOrdinaire(pp, 2008));
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
				final List<PeriodeImposition> pis = service.determine(pp, 2009);
				assertNotNull(pis);
				assertEquals(1, pis.size());

				final PeriodeImposition pi = pis.get(0);
				assertNotNull(pi);
				assertEquals(date(2009, 1, 1), pi.getDateDebut());
				assertEquals(date(2009, 12, 31), pi.getDateFin());
				assertTrue(pi.isRemplaceeParNote());
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
				final List<PeriodeImposition> pis = service.determine(pp, 2009);
				assertNotNull(pis);
				assertEquals(1, pis.size());

				final PeriodeImposition pi = pis.get(0);
				assertNotNull(pi);
				assertEquals(date(2009, 1, 1), pi.getDateDebut());
				assertEquals(date(2009, 12, 31), pi.getDateFin());
				assertTrue(pi.isRemplaceeParNote());
				return null;
			}
		});

	}
}
