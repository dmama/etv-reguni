package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.type.*;
import org.junit.Test;

import java.util.*;

import static ch.vd.registre.base.date.DateRangeHelper.Range;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

@SuppressWarnings({"JavaDoc"})
public class AssujettissementTest extends MetierTest {

	@Test
	public void testDetermineAucunFor() throws Exception {
		final Contribuable paul = createContribuableSansFor();
		assertEmpty(Assujettissement.determine(paul, 2008));
		assertEmpty(Assujettissement.determine(paul, RANGE_2002_2010, true));
	}

	@Test
	public void testDetermineUnForSimple() throws Exception {

		final Contribuable paul = createUnForSimple();
		List<Assujettissement> list = Assujettissement.determine(paul, 2008);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), null, null, list.get(0));

		list = Assujettissement.determine(paul, RANGE_2002_2010, true);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertOrdinaire(date(2002, 1, 1), date(2010, 12, 31), null, null, list.get(0));
	}

	@Test
	public void testDetermineMenageCommunMarieDansLAnnee() throws Exception {

		final EnsembleTiersCouple ensemble = createMenageCommunMarie(date(2008, 7, 1));

		// 2007
		{
			final List<Assujettissement> assujetPrincipal = Assujettissement.determine(ensemble.getPrincipal(), 2007);
			assertNotNull(assujetPrincipal);
			assertEquals(1, assujetPrincipal.size());
			assertOrdinaire(date(2007, 1, 1), date(2007, 12, 31), null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, assujetPrincipal.get(0));

			final List<Assujettissement> assujetConjoint = Assujettissement.determine(ensemble.getConjoint(), 2007);
			assertNotNull(assujetConjoint);
			assertEquals(1, assujetConjoint.size());
			assertOrdinaire(date(2007, 1, 1), date(2007, 12, 31), null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, assujetConjoint.get(0));

			assertEmpty(Assujettissement.determine(ensemble.getMenage(), 2007));
		}
		
		// 2008
		{
			assertEmpty(Assujettissement.determine(ensemble.getPrincipal(), 2008));
			assertEmpty(Assujettissement.determine(ensemble.getConjoint(), 2008));

			final List<Assujettissement> assujetMenage = Assujettissement.determine(ensemble.getMenage(), 2008);
			assertNotNull(assujetMenage);
			assertEquals(1, assujetMenage.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null, assujetMenage.get(0));
		}

		// 2002-2010
		{
			final List<Assujettissement> assujetPrincipal = Assujettissement.determine(ensemble.getPrincipal(), RANGE_2002_2010, true);
			assertNotNull(assujetPrincipal);
			assertEquals(1, assujetPrincipal.size());
			assertOrdinaire(date(2002, 1, 1), date(2007, 12, 31), null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, assujetPrincipal.get(0));

			final List<Assujettissement> assujetConjoint = Assujettissement.determine(ensemble.getConjoint(), RANGE_2002_2010, true);
			assertNotNull(assujetConjoint);
			assertEquals(1, assujetConjoint.size());
			assertOrdinaire(date(2002, 1, 1), date(2007, 12, 31), null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, assujetConjoint.get(0));

			final List<Assujettissement> assujetMenage = Assujettissement.determine(ensemble.getMenage(), RANGE_2002_2010, true);
			assertNotNull(assujetMenage);
			assertEquals(1, assujetMenage.size());
			assertOrdinaire(date(2008, 1, 1), date(2010, 12, 31), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null, assujetMenage.get(0));
		}
	}

	@Test
	public void testDetermineMenageCommunMarieAu1erJanvier() throws Exception {

		final EnsembleTiersCouple ensemble = createMenageCommunMarie(date(2009, 1, 1));

		// 2008
		{
			final List<Assujettissement> assujetPrincipal = Assujettissement.determine(ensemble.getPrincipal(), 2008);
			assertNotNull(assujetPrincipal);
			assertEquals(1, assujetPrincipal.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, assujetPrincipal.get(0));

			final List<Assujettissement> assujetConjoint = Assujettissement.determine(ensemble.getConjoint(), 2008);
			assertNotNull(assujetConjoint);
			assertEquals(1, assujetConjoint.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, assujetConjoint.get(0));

			assertEmpty(Assujettissement.determine(ensemble.getMenage(), 2008));
		}

		// 2009
		{
			assertEmpty(Assujettissement.determine(ensemble.getPrincipal(), 2009));
			assertEmpty(Assujettissement.determine(ensemble.getConjoint(), 2009));

			final List<Assujettissement> assujetMenage = Assujettissement.determine(ensemble.getMenage(), 2009);
			assertNotNull(assujetMenage);
			assertEquals(1, assujetMenage.size());
			assertOrdinaire(date(2009, 1, 1), date(2009, 12, 31), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null, assujetMenage.get(0));
		}

		// 2002-2010
		{
			final List<Assujettissement> assujetPrincipal = Assujettissement.determine(ensemble.getPrincipal(), RANGE_2002_2010, true);
			assertNotNull(assujetPrincipal);
			assertEquals(1, assujetPrincipal.size());
			assertOrdinaire(date(2002, 1, 1), date(2008, 12, 31), null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, assujetPrincipal.get(0));

			final List<Assujettissement> assujetConjoint = Assujettissement.determine(ensemble.getConjoint(), RANGE_2002_2010, true);
			assertNotNull(assujetConjoint);
			assertEquals(1, assujetConjoint.size());
			assertOrdinaire(date(2002, 1, 1), date(2008, 12, 31), null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, assujetConjoint.get(0));

			final List<Assujettissement> assujetMenage = Assujettissement.determine(ensemble.getMenage(), RANGE_2002_2010, true);
			assertNotNull(assujetMenage);
			assertEquals(1, assujetMenage.size());
			assertOrdinaire(date(2009, 1, 1), date(2010, 12, 31), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null, assujetMenage.get(0));
		}
	}

	@Test
	public void testDetermineMenageCommunDivorceDansLAnnee() throws Exception {

		final RegDate dateMariage = date(2000, 1, 1);
		final RegDate dateDivorce = date(2008, 7, 1);
		final EnsembleTiersCouple ensemble = createMenageCommunDivorce(dateMariage, dateDivorce);

		// 2007
		{
			assertEmpty(Assujettissement.determine(ensemble.getPrincipal(), 2007));
			assertEmpty(Assujettissement.determine(ensemble.getConjoint(), 2007));

			final List<Assujettissement> assujetMenage = Assujettissement.determine(ensemble.getMenage(), 2007);
			assertNotNull(assujetMenage);
			assertEquals(1, assujetMenage.size());
			assertOrdinaire(date(2007, 1, 1), date(2007, 12, 31), null, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, assujetMenage.get(0));
		}

		// 2008
		{
			final List<Assujettissement> assujetPrincipal = Assujettissement.determine(ensemble.getPrincipal(), 2008);
			assertNotNull(assujetPrincipal);
			assertEquals(1, assujetPrincipal.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null, assujetPrincipal.get(0));

			final List<Assujettissement> assujetConjoint = Assujettissement.determine(ensemble.getConjoint(), 2008);
			assertNotNull(assujetConjoint);
			assertEquals(1, assujetConjoint.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null, assujetConjoint.get(0));

			assertEmpty(Assujettissement.determine(ensemble.getMenage(), 2008));
		}
		
		// 2002-2010
		{
			final List<Assujettissement> assujetPrincipal = Assujettissement.determine(ensemble.getPrincipal(), RANGE_2002_2010, true);
			assertNotNull(assujetPrincipal);
			assertEquals(1, assujetPrincipal.size());
			assertOrdinaire(date(2008, 1, 1), date(2010, 12, 31), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null, assujetPrincipal.get(0));

			final List<Assujettissement> assujetConjoint = Assujettissement.determine(ensemble.getConjoint(), RANGE_2002_2010, true);
			assertNotNull(assujetConjoint);
			assertEquals(1, assujetConjoint.size());
			assertOrdinaire(date(2008, 1, 1), date(2010, 12, 31), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null, assujetConjoint.get(0));

			final List<Assujettissement> assujetMenage = Assujettissement.determine(ensemble.getMenage(), RANGE_2002_2010, true);
			assertNotNull(assujetMenage);
			assertEquals(1, assujetMenage.size());
			assertOrdinaire(date(2002, 1, 1), date(2007, 12, 31), null, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, assujetMenage.get(0));
		}
	}
	
	@Test
	public void testDetermineMenageCommunDivorceAu1erJanvier() throws Exception {

		final RegDate dateMariage = date(2000, 1, 1);
		final RegDate dateDivorce = date(2008, 7, 1);
		final EnsembleTiersCouple ensemble = createMenageCommunDivorce(dateMariage, dateDivorce);

		// 2007
		{
			assertEmpty(Assujettissement.determine(ensemble.getPrincipal(), 2007));
			assertEmpty(Assujettissement.determine(ensemble.getConjoint(), 2007));

			final List<Assujettissement> assujetMenage = Assujettissement.determine(ensemble.getMenage(), 2007);
			assertNotNull(assujetMenage);
			assertEquals(1, assujetMenage.size());
			assertOrdinaire(date(2007, 1, 1), date(2007, 12, 31), null, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, assujetMenage.get(0));
		}

		// 2008
		{
			final List<Assujettissement> assujetPrincipal = Assujettissement.determine(ensemble.getPrincipal(), 2008);
			assertNotNull(assujetPrincipal);
			assertEquals(1, assujetPrincipal.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null, assujetPrincipal.get(0));

			final List<Assujettissement> assujetConjoint = Assujettissement.determine(ensemble.getConjoint(), 2008);
			assertNotNull(assujetConjoint);
			assertEquals(1, assujetConjoint.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null, assujetConjoint.get(0));

			assertEmpty(Assujettissement.determine(ensemble.getMenage(), 2008));
		}
		
		// 2002-2010
		{
			final List<Assujettissement> assujetPrincipal = Assujettissement.determine(ensemble.getPrincipal(), RANGE_2002_2010, true);
			assertNotNull(assujetPrincipal);
			assertEquals(1, assujetPrincipal.size());
			assertOrdinaire(date(2008, 1, 1), date(2010, 12, 31), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null, assujetPrincipal.get(0));

			final List<Assujettissement> assujetConjoint = Assujettissement.determine(ensemble.getConjoint(), RANGE_2002_2010, true);
			assertNotNull(assujetConjoint);
			assertEquals(1, assujetConjoint.size());
			assertOrdinaire(date(2008, 1, 1), date(2010, 12, 31), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null, assujetConjoint.get(0));

			final List<Assujettissement> assujetMenage = Assujettissement.determine(ensemble.getMenage(), RANGE_2002_2010, true);
			assertNotNull(assujetMenage);
			assertEquals(1, assujetMenage.size());
			assertOrdinaire(date(2002, 1, 1), date(2007, 12, 31), null, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, assujetMenage.get(0));
		}
	}

	@Test
	public void testDetermineMenageCommunMarieEtDivorceDansLAnnee() throws Exception {

		final RegDate dateMariage = date(2008, 3, 1);
		final RegDate dateDivorce = date(2008, 11, 15);
		final EnsembleTiersCouple ensemble = createMenageCommunDivorce(dateMariage, dateDivorce);

		// mariage et divorce dans la même année -> aucun effet
		
		// 2007
		{
			final List<Assujettissement> assujetPrincipal = Assujettissement.determine(ensemble.getPrincipal(), 2007);
			assertNotNull(assujetPrincipal);
			assertEquals(1, assujetPrincipal.size());
			assertOrdinaire(date(2007, 1, 1), date(2007, 12, 31), null, null, assujetPrincipal.get(0));

			final List<Assujettissement> assujetConjoint = Assujettissement.determine(ensemble.getConjoint(), 2007);
			assertNotNull(assujetConjoint);
			assertEquals(1, assujetConjoint.size());
			assertOrdinaire(date(2007, 1, 1), date(2007, 12, 31), null, null, assujetConjoint.get(0));

			assertEmpty(Assujettissement.determine(ensemble.getMenage(), 2007));
		}

		// 2008
		{
			final List<Assujettissement> assujetPrincipal = Assujettissement.determine(ensemble.getPrincipal(), 2008);
			assertNotNull(assujetPrincipal);
			assertEquals(1, assujetPrincipal.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), null, null, assujetPrincipal.get(0));

			final List<Assujettissement> assujetConjoint = Assujettissement.determine(ensemble.getConjoint(), 2008);
			assertNotNull(assujetConjoint);
			assertEquals(1, assujetConjoint.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), null, null, assujetConjoint.get(0));

			assertEmpty(Assujettissement.determine(ensemble.getMenage(), 2008));
		}

		// 2002-2010
		{
			final List<Assujettissement> assujetPrincipal = Assujettissement.determine(ensemble.getPrincipal(), RANGE_2002_2010, true);
			assertNotNull(assujetPrincipal);
			assertEquals(1, assujetPrincipal.size());
			assertOrdinaire(date(2002, 1, 1), date(2010, 12, 31), null, null, assujetPrincipal.get(0));

			final List<Assujettissement> assujetConjoint = Assujettissement.determine(ensemble.getConjoint(), RANGE_2002_2010, true);
			assertNotNull(assujetConjoint);
			assertEquals(1, assujetConjoint.size());
			assertOrdinaire(date(2002, 1, 1), date(2010, 12, 31), null, null, assujetConjoint.get(0));

			assertEmpty(Assujettissement.determine(ensemble.getMenage(), RANGE_2002_2010, true));
		}
	}

	@Test
	public void testDetermineDepartHorsCantonDansLAnnee() throws Exception {

		final Contribuable paul = createDepartHorsCanton(date(2008, 6, 30));

		// 2007
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2007, 1, 1), date(2007, 12, 31), null, MotifFor.DEPART_HC, list.get(0));
		}

		// 2008
		{
			assertEmpty(Assujettissement.determine(paul, 2008));
		}

		// 2002-2010
		{
			List<Assujettissement> list = Assujettissement.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2002, 1, 1), date(2007, 12, 31), null, MotifFor.DEPART_HC, list.get(0));
		}
	}

	@Test
	public void testDetermineDepartHorsCantonAu31Decembre() throws Exception {

		final Contribuable paul = createDepartHorsCanton(date(2008, 12, 31));

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size()); // un départ-hc au 31 décembre correspond bien à une fin d'assujettissement (cas limite, il est vrai)
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), null, MotifFor.DEPART_HC, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = Assujettissement.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2002, 1, 1), date(2008, 12, 31), null, MotifFor.DEPART_HC, list.get(0));
		}
	}

	@Test
	public void testDetermineDepartHorsCantonDansLAnneeAvecImmeuble() throws Exception {

		final Contribuable paul = createDepartHorsCantonAvecImmeuble(date(2008, 6, 30));

		// 2007
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2007, 1, 1), date(2007, 12, 31), null, MotifFor.DEPART_HC, list.get(0));
		}

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2008, 1, 1), date(2008, 12, 31), MotifFor.DEPART_HC, null, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = Assujettissement.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertOrdinaire(date(2002, 1, 1), date(2007, 12, 31), null, MotifFor.DEPART_HC, list.get(0));
			assertHorsCanton(date(2008, 1, 1), date(2010, 12, 31), MotifFor.DEPART_HC, null, list.get(1));
		}
	}

	@Test
	public void testDetermineDepartHorsCantonAu31DecembreAvecImmeuble() throws Exception {

		final Contribuable paul = createDepartHorsCantonAvecImmeuble(date(2008, 12, 31));

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), null, MotifFor.DEPART_HC, list.get(0));
		}

		// 2009
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2009, 1, 1), date(2009, 12, 31), MotifFor.DEPART_HC, null, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = Assujettissement.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertOrdinaire(date(2002, 1, 1), date(2008, 12, 31), null, MotifFor.DEPART_HC, list.get(0));
			assertHorsCanton(date(2009, 1, 1), date(2010, 12, 31), MotifFor.DEPART_HC, null, list.get(1));
		}
	}

	@Test
	public void testDetermineDepartHorsCantonEtVenteImmeubleDansLAnnee() throws Exception {

		final Contribuable paul = createDepartHorsCantonEtVenteImmeuble(date(2008, 6, 30), date(2008, 9, 30));

		// 2007
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2007, 1, 1), date(2007, 12, 31), null, MotifFor.DEPART_HC, list.get(0));
		}

		// 2008 (départ puis vente)
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// La période d'assujettissement en raison d'un rattachement économique s'étend à toute l'année (art. 8 al. 6 LI).
			assertHorsCanton(date(2008, 1, 1), date(2008, 12, 31), MotifFor.DEPART_HC, MotifFor.VENTE_IMMOBILIER, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = Assujettissement.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertOrdinaire(date(2002, 1, 1), date(2007, 12, 31), null, MotifFor.DEPART_HC, list.get(0));
			assertHorsCanton(date(2008, 1, 1), date(2008, 12, 31), MotifFor.DEPART_HC, MotifFor.VENTE_IMMOBILIER, list.get(1));
		}
	}

	@Test
	public void testDetermineDepartHorsCantonSourcierPur() throws Exception {

		final Contribuable paul = createDepartHorsCantonSourcierPur(date(2008, 9, 25));

		// 2007
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2007, 1, 1), date(2007, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(2, list.size());
			// fractionnement de l'assujettissement
			assertSourcierPur(date(2008, 1, 1), date(2008, 9, 25), null, MotifFor.DEPART_HC, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
			assertSourcierPur(date(2008, 9, 26), date(2008, 12, 31), MotifFor.DEPART_HC, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(1));
		}

		// 2009
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2009, 1, 1), date(2009, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = Assujettissement.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertSourcierPur(date(2002, 1, 1), date(2008, 9, 25), null, MotifFor.DEPART_HC, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
			assertSourcierPur(date(2008, 9, 26), date(2010, 12, 31), MotifFor.DEPART_HC, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(1));
		}
	}

	@Test
	public void testDetermineDepartHorsCantonSourcierMixte137Al1() throws Exception {

		final Contribuable paul = createDepartHorsCantonSourcierMixte137Al1(date(2008, 9, 25));

		// 2007
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixte(date(2007, 1, 1), date(2007, 12, 31), null, MotifFor.DEPART_HC, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// [UNIREG-1742] pas de fractionnement dans ce cas-là car le contribuable reste assujetti toute l'année à raison de son for secondaire (immeuble ou activité indépendante).
			assertSourcierMixte(date(2008, 1, 1), date(2008, 12, 31), MotifFor.DEPART_HC, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
		}

		// 2009
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixte(date(2009, 1, 1), date(2009, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = Assujettissement.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertSourcierMixte(date(2002, 1, 1), date(2007, 12, 31), null, MotifFor.DEPART_HC, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
			assertSourcierMixte(date(2008, 1, 1), date(2010, 12, 31), MotifFor.DEPART_HC, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(1));
		}
	}

	@Test
	public void testDetermineDepartHorsCantonSourcierMixte137Al2() throws Exception {

		final Contribuable paul = createDepartHorsCantonSourcierMixte137Al2(date(2008, 9, 25));

		// 2007
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixte(date(2007, 1, 1), date(2007, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(2, list.size());
			// [UNIREG-1742] fractionnement de l'assujettissement (mais pas d'arrondi à la fin de mois)
			assertSourcierMixte(date(2008, 1, 1), date(2008, 9, 25), null, MotifFor.DEPART_HC, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
			assertSourcierPur(date(2008, 9, 26), date(2008, 12, 31), MotifFor.DEPART_HC, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(1));
		}

		// 2009
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2009, 1, 1), date(2009, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = Assujettissement.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertSourcierMixte(date(2002, 1, 1), date(2008, 9, 25), null, MotifFor.DEPART_HC, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
			assertSourcierPur(date(2008, 9, 26), date(2010, 12, 31), MotifFor.DEPART_HC, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(1));
		}
	}

	@Test
	public void testDetermineArriveeHorsCantonSourcierPur() throws Exception {

		final Contribuable paul = createArriveeHorsCantonSourcierPur(date(2008, 9, 25));

		// 2007
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2007, 1, 1), date(2007, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
		}

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(2, list.size());
			// fractionnement de l'assujettissement
			assertSourcierPur(date(2008, 1, 1), date(2008, 9, 24), null, MotifFor.ARRIVEE_HC, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
			assertSourcierPur(date(2008, 9, 25), date(2008, 12, 31), MotifFor.ARRIVEE_HC, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(1));
		}

		// 2009
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2009, 1, 1), date(2009, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = Assujettissement.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertSourcierPur(date(2002, 1, 1), date(2008, 9, 24), null, MotifFor.ARRIVEE_HC, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
			assertSourcierPur(date(2008, 9, 25), date(2010, 12, 31), MotifFor.ARRIVEE_HC, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(1));
		}
	}

	@Test
	public void testDetermineArriveeHorsCantonDansLAnnee() throws Exception {

		final Contribuable paul = createArriveeHorsCanton(date(2008, 9, 25));

		// 2007
		{
			assertEmpty(Assujettissement.determine(paul, 2007));
		}

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), MotifFor.ARRIVEE_HC, null, list.get(0));
		}

		// 2009
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2009, 1, 1), date(2009, 12, 31), null, null, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = Assujettissement.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2008, 1, 1), date(2010, 12, 31), MotifFor.ARRIVEE_HC, null, list.get(0));
		}
	}

	@Test
	public void testDetermineArriveeHorsCantonAvecImmeubleDansLAnnee() throws Exception {

		final Contribuable paul = createArriveeHorsCantonAvecImmeuble(date(2008, 9, 25));

		// 2007
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2007, 1, 1), date(2007, 12, 31), null, MotifFor.ARRIVEE_HC, list.get(0));
		}

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), MotifFor.ARRIVEE_HC, null, list.get(0));
		}

		// 2009
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2009, 1, 1), date(2009, 12, 31), null, null, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = Assujettissement.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertHorsCanton(date(2002, 1, 1), date(2007, 12, 31), null, MotifFor.ARRIVEE_HC, list.get(0));
			assertOrdinaire(date(2008, 1, 1), date(2010, 12, 31), MotifFor.ARRIVEE_HC, null, list.get(1));
		}
	}

	@Test
	public void testDetermineArriveeHorsCantonAu1erJanvier() throws Exception {

		final Contribuable paul = createArriveeHorsCanton(date(2008, 1, 1));

		// 2007
		{
			assertEmpty(Assujettissement.determine(paul, 2007));
		}

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), MotifFor.ARRIVEE_HC, null, list.get(0));
		}

		// 2009
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2009, 1, 1), date(2009, 12, 31), null, null, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = Assujettissement.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2008, 1, 1), date(2010, 12, 31), MotifFor.ARRIVEE_HC, null, list.get(0));
		}
	}

	@Test
	public void testDetermineArriveeHorsCantonSourcierMixte137Al1() throws Exception {

		final Contribuable paul = createArriveeHorsCantonSourcierMixte137Al1(date(2008, 9, 25));

		// 2007
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixte(date(2007, 1, 1), date(2007, 12, 31), null, MotifFor.ARRIVEE_HC, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
		}

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// [UNIREG-1742] pas de fractionnement à la date d'arrivée dans ce cas-là car le contribuable reste assujetti toute l'année à raison de son for secondaire (immeuble ou activité indépendante).
			assertSourcierMixte(date(2008, 1, 1), date(2008, 12, 31), MotifFor.ARRIVEE_HC, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2009
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixte(date(2009, 1, 1), date(2009, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = Assujettissement.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertSourcierMixte(date(2002, 1, 1), date(2007, 12, 31), MotifFor.ACHAT_IMMOBILIER, MotifFor.ARRIVEE_HC, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
			assertSourcierMixte(date(2008, 1, 1), date(2010, 12, 31), MotifFor.ARRIVEE_HC, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(1));
		}
	}

	@Test
	public void testDetermineArriveeHorsCantonSourcierMixte137Al2() throws Exception {

		final Contribuable paul = createArriveeHorsCantonSourcierMixte137Al2(date(2008, 9, 25));

		// 2007
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2007, 1, 1), date(2007, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
		}

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(2, list.size());
			// [UNIREG-1742] fractionnement de l'assujettissement (mais pas d'arrondi à la fin de mois)
			assertSourcierPur(date(2008, 1, 1), date(2008, 9, 24), null, MotifFor.ARRIVEE_HC, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
			assertSourcierMixte(date(2008, 9, 25), date(2008, 12, 31), MotifFor.ARRIVEE_HC, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(1));
		}

		// 2009
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixte(date(2009, 1, 1), date(2009, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = Assujettissement.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertSourcierPur(date(2002, 1, 1), date(2008, 9, 24), null, MotifFor.ARRIVEE_HC, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
			assertSourcierMixte(date(2008, 9, 25), date(2010, 12, 31), MotifFor.ARRIVEE_HC, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(1));
		}
	}

	@Test
	public void testDetermineDepartHorsSuisseDansLAnnee() throws Exception {

		final Contribuable paul = createDepartHorsSuisse(date(2008, 6, 30));

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// ordinaire pendant son séjour en suisse, et non-assujetti hors-Suisse
			assertOrdinaire(date(2008, 1, 1), date(2008, 6, 30), null, MotifFor.DEPART_HS, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = Assujettissement.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2002, 1, 1), date(2008, 6, 30), null, MotifFor.DEPART_HS, list.get(0));
		}
	}

	@Test
	public void testDetermineDepartHorsSuisseAu31Decembre() throws Exception {

		final Contribuable paul = createDepartHorsSuisse(date(2008, 12, 31));

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// ordinaire pendant son séjour en suisse, et non-assujetti hors-Suisse
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), null, MotifFor.DEPART_HS, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = Assujettissement.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2002, 1, 1), date(2008, 12, 31), null, MotifFor.DEPART_HS, list.get(0));
		}
	}

	@Test
	public void testDetermineDepartHorsSuisseDansLAnneeAvecImmeuble() throws Exception {

		final Contribuable paul = createDepartHorsSuisseAvecImmeuble(date(2008, 6, 30));

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(2, list.size());
			// ordinaire pendant son séjour en Suisse
			assertOrdinaire(date(2008, 1, 1), date(2008, 6, 30), null, MotifFor.DEPART_HS, list.get(0));
			// hors-Suisse le reste de l'année
			assertHorsSuisse(date(2008, 7, 1), date(2008, 12, 31), MotifFor.DEPART_HS, null, list.get(1));
		}

		// 2002-2010
		{
			List<Assujettissement> list = Assujettissement.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertOrdinaire(date(2002, 1, 1), date(2008, 6, 30), null, MotifFor.DEPART_HS, list.get(0));
			assertHorsSuisse(date(2008, 7, 1), date(2010, 12, 31), MotifFor.DEPART_HS, null, list.get(1));
		}
	}

	@Test
	public void testDetermineDepartHorsSuisseAu31DecembreAvecImmeuble() throws Exception {

		final Contribuable paul = createDepartHorsSuisseAvecImmeuble(date(2008, 12, 31));

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), null, MotifFor.DEPART_HS, list.get(0));
		}

		// 2009
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsSuisse(date(2009, 1, 1), date(2009, 12, 31), MotifFor.DEPART_HS, null, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = Assujettissement.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertOrdinaire(date(2002, 1, 1), date(2008, 12, 31), null, MotifFor.DEPART_HS, list.get(0));
			assertHorsSuisse(date(2009, 1, 1), date(2010, 12, 31), MotifFor.DEPART_HS, null, list.get(1));
		}
	}
	
	@Test
	public void testDetermineDepartHorsSuisseDepuisHorsCantonAvecImmeuble() throws Exception {

		final Contribuable ctb = createDepartHorsSuisseDepuisHorsCantonAvecImmeuble(date(2008, 6, 30));

		// 2007 (hors-canton)
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2007, 1, 1), date(2007, 12, 31), null, MotifFor.DEPART_HS, list.get(0));
		}

		// 2008 (hors-canton -> hors-Suisse)
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// [UNIREG-1742] le départ hors-Suisse depuis hors-canton ne doit pas fractionner l'assujettissement en cours de période (car le rattachement économique n'est pas interrompu)
			assertHorsSuisse(date(2008, 1, 1), date(2008, 12, 31), MotifFor.DEPART_HS, null, list.get(0));
		}

		// 2009 (hors-Suisse)
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsSuisse(date(2009, 1, 1), date(2009, 12, 31), null, null, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = Assujettissement.determine(ctb, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertHorsCanton(date(2002, 1, 1), date(2007, 12, 31), null, MotifFor.DEPART_HS, list.get(0));
			assertHorsSuisse(date(2008, 1, 1), date(2010, 12, 31), MotifFor.DEPART_HS, null, list.get(1));
		}
	}

	@Test
	public void testDetermineDepartHorsSuisseEtArriveeDeHorsCantonDansLAnnee() throws Exception {

		final RegDate dateDepart = RegDate.get(2007, 3, 15);
		final RegDate dateArrivee = RegDate.get(2007, 10, 1);
		final Contribuable ctb = createDepartHorsSuisseEtArriveeDeHorsCanton(dateDepart, dateArrivee);

		// 2006
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2006);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2006, 1, 1), date(2006, 12, 31), null, null, list.get(0));
		}

		// 2007 (départ HS et arrivée HC dans l'année -> pas moyen de connaître la date d'arrivée de HS dans l'autre canton, on prend toute la période restante par défaut)
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertOrdinaire(date(2007, 1, 1), dateDepart, null, MotifFor.DEPART_HS, list.get(0));
			assertOrdinaire(dateDepart.getOneDayAfter(), date(2007, 12, 31), MotifFor.ARRIVEE_HC, null, list.get(1));
		}

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), null, null, list.get(0));
		}
	}

	@Test
	public void testDetermineDepartHorsSuisseDepuisHorsCantonAvecActiviteIndependante() throws Exception {

		final Contribuable ctb = createDepartHorsSuisseDepuisHorsCantonAvecActiviteIndependante(date(2008, 6, 30));

		// 2007 (hors-canton)
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2007, 1, 1), date(2007, 12, 31), null, MotifFor.DEPART_HS, list.get(0));
		}

		// 2008 (hors-canton -> hors-Suisse)
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// [UNIREG-1742] le départ hors-Suisse depuis hors-canton ne doit pas fractionner l'assujettissement en cours de période (car le rattachement économique n'est pas interrompu)
			assertHorsSuisse(date(2008, 1, 1), date(2008, 12, 31), MotifFor.DEPART_HS, null, list.get(0));
		}

		// 2009 (hors-Suisse)
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsSuisse(date(2009, 1, 1), date(2009, 12, 31), null, null, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = Assujettissement.determine(ctb, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertHorsCanton(date(2002, 1, 1), date(2007, 12, 31), null, MotifFor.DEPART_HS, list.get(0));
			assertHorsSuisse(date(2008, 1, 1), date(2010, 12, 31), MotifFor.DEPART_HS, null, list.get(1));
		}
	}

	/**
	 * [UNIREG-1327] Vérifie que l'assujettissement d'un HS qui vend son immeuble ne s'étend pas au delà de la date de vente.
	 */
	@Test
	public void testDetermineVenteImmeubleContribuableHorsSuisse() throws Exception {

		final RegDate dateAchat = date(2000, 7, 1);
		final RegDate dateVente = date(2007, 5, 30);
		final Contribuable paul = createHorsSuisseAvecAchatEtVenteImmeuble(dateAchat, dateVente);

		// 2006
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2006);
			assertNotNull(list);
			assertEquals(1, list.size());
			// hors-Suisse toute l'année
			assertHorsSuisse(date(2006, 1, 1), date(2006, 12, 31), null, null, list.get(0));
		}

		// 2007
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			// hors-Suisse jusqu'à la date de la vente de l'immeuble
			assertHorsSuisse(date(2007, 1, 1), dateVente, null, MotifFor.VENTE_IMMOBILIER, list.get(0));
		}

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2008);
			assertEmpty(list);
		}

		// 2000-2008
		{
			List<Assujettissement> list = Assujettissement.determine(paul, RANGE_2000_2008, true);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsSuisse(dateAchat, dateVente, MotifFor.ACHAT_IMMOBILIER, MotifFor.VENTE_IMMOBILIER, list.get(0));
		}
	}

	@Test
	public void testDetermineArriveeHorsSuisseAvecImmeuble() throws Exception {

		final RegDate dateArrivee = date(2007, 3, 1);
		final Contribuable ctb = createArriveeHorsSuisseAvecImmeuble(dateArrivee);

		// 2006
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2006);
			assertNotNull(list);
			assertEquals(1, list.size());
			// hors-Suisse toute l'année
			assertHorsSuisse(date(2006, 1, 1), date(2006, 12, 31), null, null, list.get(0));
		}

		// 2007
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(2, list.size());
			// hors-Suisse jusqu'à la date d'arrivée
			assertHorsSuisse(date(2007, 1, 1), dateArrivee.getOneDayBefore(), null, MotifFor.ARRIVEE_HS, list.get(0));
			// ordinaire depuis l'arrivée
			assertOrdinaire(dateArrivee, date(2007, 12, 31), MotifFor.ARRIVEE_HS, null, list.get(1));
		}

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// ordinaire toute l'année
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), null, null, list.get(0));
		}
	}

	/**
	 * Version spéciale avec motif de fermeture du fors HS nul.
	 */
	@Test
	public void testDetermineArriveeHorsSuisseAvecImmeubleEtMotifFermetureNul() throws Exception {

		final RegDate dateArrivee = date(2007, 3, 1);
		final Contribuable ctb = createArriveeHorsSuisseAvecImmeuble(dateArrivee);
		
		final ForFiscalPrincipal ffp0 = (ForFiscalPrincipal) ctb.getForsFiscauxSorted().get(0);
		ffp0.setMotifFermeture(null);

		// 2006
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2006);
			assertNotNull(list);
			assertEquals(1, list.size());
			// hors-Suisse toute l'année
			assertHorsSuisse(date(2006, 1, 1), date(2006, 12, 31), null, null, list.get(0));
		}

		// 2007
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(2, list.size());
			// hors-Suisse jusqu'à la date d'arrivée
			// TODO (msi) le motif de fermeture devrait être MotifFor.ARRIVEE_HS
			assertHorsSuisse(date(2007, 1, 1), dateArrivee.getOneDayBefore(), null, null, list.get(0));
			// ordinaire depuis l'arrivée
			assertOrdinaire(dateArrivee, date(2007, 12, 31), MotifFor.ARRIVEE_HS, null, list.get(1));
		}

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// ordinaire toute l'année
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), null, null, list.get(0));
		}
	}

	/**
	 * [UNIREG-1327] Vérifie que l'assujettissement d'un contribuable HS qui possède un immeuble, arrive de HS et vend son immeuble dans la
	 * même année est bien fractionné à la date d'arrivée HS.
	 */
	@Test
	public void testDetermineArriveeHorsSuisseEtVenteImmeubleDansLAnnee() throws Exception {

		final RegDate dateArrivee = date(2007, 3, 1);
		final RegDate dateVente = date(2007, 5, 30);
		final Contribuable paul = createArriveeHorsSuisseEtVenteImmeuble(dateArrivee, dateVente);

		// 2006
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2006);
			assertNotNull(list);
			assertEquals(1, list.size());
			// hors-Suisse toute l'année
			assertHorsSuisse(date(2006, 1, 1), date(2006, 12, 31), null, null, list.get(0));
		}

		// 2007
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(2, list.size());
			// hors-Suisse jusqu'à la date d'arrivée
			assertHorsSuisse(date(2007, 1, 1), dateArrivee.getOneDayBefore(), null, MotifFor.ARRIVEE_HS, list.get(0));
			// ordinaire depuis l'arrivée
			assertOrdinaire(dateArrivee, date(2007, 12, 31), MotifFor.ARRIVEE_HS, null, list.get(1));
		}

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// ordinaire toute l'année
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), null, null, list.get(0));
		}

		// 2000-2008
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, RANGE_2000_2008, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertHorsSuisse(date(2000, 1, 1), dateArrivee.getOneDayBefore(), MotifFor.ACHAT_IMMOBILIER, MotifFor.ARRIVEE_HS, list.get(0));
			assertOrdinaire(dateArrivee, date(2008, 12, 31), MotifFor.ARRIVEE_HS, null, list.get(1));
		}
	}

	@Test
	public void testDetermineArriveHorsSuisseEtDemenagementVaudoisDansLAnnee() throws Exception {

		final RegDate dateArrivee = date(2007, 3, 1);
		final RegDate dateDemenagement = date(2007, 7, 1);

		Contribuable ctb = addNonHabitant("Werner", "Karey", date(1963, 1, 1), Sexe.MASCULIN);
		addForPrincipal(ctb, dateArrivee, MotifFor.ARRIVEE_HS, dateDemenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
		addForPrincipal(ctb, dateDemenagement, MotifFor.DEMENAGEMENT_VD, MockCommune.Bex);

		// 2006
		{
			assertEmpty(Assujettissement.determine(ctb, 2006));
		}

		// 2007
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(dateArrivee, date(2007, 12, 31), MotifFor.ARRIVEE_HS, null, list.get(0));
		}

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2008, 1, 1), date(2008, 12, 31), null, null, list.get(0));
		}
	}

	/**
	 * Cas très spécial du contribuable qui arrive de HS et qui repart HS la même année, et qui achète un immeuble entre-deux.
	 */
	@Test
	public void testDetermineArriveeHorsSuisseAchatImmeubleEtDepartHorsSuisseDansLAnnee() throws Exception {

		final RegDate dateArrivee = date(2007, 3, 1);
		final RegDate dateAchat = date(2007, 5, 30);
		final RegDate dateDepart = date(2007, 12, 8);
		final Contribuable paul = createArriveeHSAchatImmeubleEtDepartHS(dateArrivee, dateAchat, dateDepart);

		// 2006
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2006);
			assertEmpty(list);
		}

		// 2007
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(2, list.size());
			// hors-Suisse non-assujetti avant l'arrivée, ordinaire ensuite
			assertOrdinaire(dateArrivee, dateDepart, MotifFor.ARRIVEE_HS, MotifFor.DEPART_HS, list.get(0));
			// hors-Suisse mais assujetti après son départ
			assertHorsSuisse(dateDepart.getOneDayAfter(), date(2007, 12, 31), MotifFor.DEPART_HS, null, list.get(1));
		}

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// hors-Suisse toute l'année
			assertHorsSuisse(date(2008, 1, 1), date(2008, 12, 31), null, null, list.get(0));
		}

		// 2000-2008
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, RANGE_2000_2008, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertOrdinaire(dateArrivee, dateDepart, MotifFor.ARRIVEE_HS, MotifFor.DEPART_HS, list.get(0));
			assertHorsSuisse(dateDepart.getOneDayAfter(), date(2008, 12, 31), MotifFor.DEPART_HS, null, list.get(1));
		}
	}

	/**
	 * Cas très spécial du contribuable qui arrive de HS et qui repart HS la même année, et qui achète un immeuble après son départ. Il doit
	 * y avoir deux assujettissements distincts : un pour sa présence en Suisse, et un autre pour son immeuble acheté plus tard.
	 */
	@Test
	public void testDetermineArriveeHorsSuisseEtDepartHorsSuissePuisAchatImmeubleDansLAnnee() throws Exception {

		final RegDate dateArrivee = date(2007, 2, 1);
		final RegDate dateDepart = date(2007, 7, 30);
		final RegDate dateAchat = date(2007, 10, 8);
		final Contribuable paul = createArriveeHSDepartHSPuisAchatImmeuble(dateArrivee, dateDepart, dateAchat);

		// 2006
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2006);
			assertEmpty(list);
		}

		// 2007
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(2, list.size());
			// assujetti comme ordinaire pendant son passage en suisse
			assertOrdinaire(dateArrivee, dateDepart, MotifFor.ARRIVEE_HS, MotifFor.DEPART_HS, list.get(0));
			// assujetti comme hors-Suisse suite à l'achat de son immeuble
			assertHorsSuisse(dateAchat, date(2007, 12, 31), MotifFor.ACHAT_IMMOBILIER, null, list.get(1));
		}

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			// hors-Suisse toute l'année
			assertHorsSuisse(date(2008, 1, 1), date(2008, 12, 31), null, null, list.get(0));
		}

		// 2000-2008
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, RANGE_2000_2008, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertOrdinaire(dateArrivee, dateDepart, MotifFor.ARRIVEE_HS, MotifFor.DEPART_HS, list.get(0));
			assertHorsSuisse(dateAchat, date(2008, 12, 31), MotifFor.ACHAT_IMMOBILIER, null, list.get(1));
		}
	}

	@Test
	public void testDeterminePassageRoleSourceAOrdinaire() throws Exception {

		final Contribuable paul = createPassageRoleSourceAOrdinaire(date(2008, 2, 12));

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(2, list.size());
			// sourcier pure les deux premiers mois (=> arrondi au mois)
			assertSourcierPur(date(2008, 1, 1), date(2008, 2, 29), null, MotifFor.CHGT_MODE_IMPOSITION, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
			// ordinaire le reste de l'année
			assertOrdinaire(date(2008, 3, 1), date(2008, 12, 31), MotifFor.CHGT_MODE_IMPOSITION, null, list.get(1));
		}

		// 2002-2010
		{
			List<Assujettissement> list = Assujettissement.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertSourcierPur(date(2002, 1, 1), date(2008, 2, 29), null, MotifFor.CHGT_MODE_IMPOSITION, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
			assertOrdinaire(date(2008, 3, 1), date(2010, 12, 31), MotifFor.CHGT_MODE_IMPOSITION, null, list.get(1));
		}
	}

	@Test
	public void testDetermineSourcierPureHorsCanton() throws Exception {

		final Contribuable paul = createSourcierPureHorsCanton();

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2008, 1, 1), date(2008, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = Assujettissement.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2002, 1, 1), date(2010, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
		}
	}

	@Test
	public void testDetermineSourcierPureHorsSuisse() throws Exception {

		final Contribuable paul = createSourcierPureHorsSuisse();

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2008, 1, 1), date(2008, 12, 31), null, null, TypeAutoriteFiscale.PAYS_HS, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = Assujettissement.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2002, 1, 1), date(2010, 12, 31), null, null, TypeAutoriteFiscale.PAYS_HS, list.get(0));
		}
	}

	@Test
	public void testDetermineSourcierMixte137Al1HorsCanton() throws Exception {

		final RegDate achatImmeuble = date(2007, 7, 1);
		final Contribuable paul = createSourcierMixte137Al1HorsCanton(achatImmeuble);

		// 2006
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2006);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2006, 1, 1), date(2006, 12, 31), null, MotifFor.CHGT_MODE_IMPOSITION, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
		}

		// 2007
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixte(date(2007, 1, 1), date(2007, 12, 31), MotifFor.CHGT_MODE_IMPOSITION, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
		}

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixte(date(2008, 1, 1), date(2008, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = Assujettissement.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertSourcierPur(date(2002, 1, 1), date(2006, 12, 31), null, MotifFor.CHGT_MODE_IMPOSITION, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
			/*
			 * Note: le passage de sourcier pure à sourcier mixte ne provoque pas de fractionnement de l'assujettissement, la validité de
			 * l'assujettissement sourcier mixte débute simplement le 1er janvier
			 */
			assertSourcierMixte(date(2007, 1, 1), date(2010, 12, 31), MotifFor.CHGT_MODE_IMPOSITION, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(1));
		}
	}

	@Test
	public void testDetermineSourcierMixte137Al1HorsSuisse() throws Exception {

		final RegDate dateChangement = date(2007, 7, 1);
		final Contribuable paul = createSourcierMixte137Al1HorsSuisse(dateChangement);

		// 2006
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2006);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2006, 1, 1), date(2006, 12, 31), null, null, TypeAutoriteFiscale.PAYS_HS, list.get(0));
		}

		// 2007
		{
			// passage sourcier pure à sourcier mixte -> fractionnement de l'assujettissement
			final List<Assujettissement> list = Assujettissement.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(2, list.size());
			// TODO (msi) voir s'il est possible d'exposer le motif d'ouverture du for secondaire (qui est plus précis que celui du for principal)
			assertSourcierPur(date(2007, 1, 1), dateChangement.getOneDayBefore(), null, MotifFor.CHGT_MODE_IMPOSITION, TypeAutoriteFiscale.PAYS_HS, list.get(0));
			assertSourcierMixte(dateChangement, date(2007, 12, 31), MotifFor.CHGT_MODE_IMPOSITION, null, TypeAutoriteFiscale.PAYS_HS, list.get(1));
		}

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixte(date(2008, 1, 1), date(2008, 12, 31), null, null, TypeAutoriteFiscale.PAYS_HS, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = Assujettissement.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertSourcierPur(date(2002, 1, 1), dateChangement.getOneDayBefore(), null, MotifFor.CHGT_MODE_IMPOSITION, TypeAutoriteFiscale.PAYS_HS, list.get(0));
			assertSourcierMixte(dateChangement, date(2010, 12, 31), MotifFor.CHGT_MODE_IMPOSITION, null, TypeAutoriteFiscale.PAYS_HS, list.get(1));
		}
	}

	@Test
	public void testDetermineSourcierMixte137Al2() throws Exception {

		final Contribuable paul = createSourcierPassageMixte137Al2(date(2005, 1, 1));

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixte(date(2008, 1, 1), date(2008, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2002-2010
		{
			List<Assujettissement> list = Assujettissement.determine(paul, RANGE_2002_2010, true);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertSourcierPur(date(2002, 1, 1), date(2004, 12, 31), null, MotifFor.CHGT_MODE_IMPOSITION, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
			assertSourcierMixte(date(2005, 1, 1), date(2010, 12, 31), MotifFor.CHGT_MODE_IMPOSITION, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(1));
		}
	}

	/**
	 * Teste le cas limite où le passage du mode d'imposition ordinaire -> sourcier tombe au milieu du premier mois.
	 * <p>
	 * Selon les règles en vigueur, le passage source -> ordinaire doit tomber au fin de mois: les périodes d'assujettissement doivent donc
	 * être ajustées en conséquence. Et il s'agit donc d'un cas particulier parce qu'en avançant le début d'assujettissement source du 16
	 * janvier au 1 janvier, la première période d'assujettissement ordinaire (du 1er janvier au 15) est écrasée.
	 */
	@Test
	public void testDetermineOrdinairePuisSourcierCasLimite() throws Exception {

		final Contribuable paul = createContribuableSansFor();
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, date(2006, 1, 15), MotifFor.CHGT_MODE_IMPOSITION,
				MockCommune.Lausanne);
		ForFiscalPrincipal fp = addForPrincipal(paul, date(2006, 1, 16), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne);
		fp.setModeImposition(ModeImposition.SOURCE);

		// 2005 (ordinaire)
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2005);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2005, 1, 1), date(2005, 12, 31), null, MotifFor.CHGT_MODE_IMPOSITION, list.get(0));
		}

		// 2006 (passage à la source pure le 16 janvier)
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2006);
			assertNotNull(list);
			assertEquals(1, list.size()); // <--- une seule période (voir commentaire de la méthode)
			assertSourcierPur(date(2006, 1, 1), date(2006, 12, 31), MotifFor.CHGT_MODE_IMPOSITION, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2007
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2007, 1, 1), date(2007, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}
	}

	/**
	 * Teste le cas limite où le passage du mode d'imposition sourcier -> ordinaire tombe au milieu du dernier mois.
	 * <p>
	 * Selon les règles en vigueur, le passage source -> ordinaire doit tomber au fin de mois: les périodes d'assujettissement doivent donc
	 * être ajustées en conséquence. Et il s'agit donc d'un cas particulier parce qu'en poussant la fin d'assujettissement source du 16
	 * décembre au 31 décembre, la seconde période d'assujettissement ordinaire (du 17 décembre au 31) est écrasée.
	 */
	@Test
	public void testDetermineSourcierPuisOrdinaireCasLimite() throws Exception {

		final Contribuable paul = createContribuableSansFor();
		ForFiscalPrincipal fp = addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, date(2006, 12, 16),
				MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne);
		fp.setModeImposition(ModeImposition.SOURCE);
		addForPrincipal(paul, date(2006, 12, 17), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne);

		// 2005
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2005);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2005, 1, 1), date(2005, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2006 (passage au rôle ordinaire le 17 décembre)
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2006);
			assertNotNull(list);
			assertEquals(1, list.size()); // <--- une seule période (voir commentaire de la méthode)
			assertSourcierPur(date(2006, 1, 1), date(2006, 12, 31), null, MotifFor.CHGT_MODE_IMPOSITION, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2007
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2007, 1, 1), date(2007, 12, 31), MotifFor.CHGT_MODE_IMPOSITION, null, list.get(0));
		}
	}

	@Test
	public void testDetermineDiplomateAvecImmeuble() throws Exception {

		final Contribuable paul = createDiplomateAvecImmeuble(date(2000, 1, 1), date(2001, 6, 13));

		// 1999
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 1999);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(1999, 1, 1), date(1999, 12, 31), null, MotifFor.DEPART_HS, list.get(0));
		}

		// 2000
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2000);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertDiplomateSuisse(date(2000, 1, 1), date(2000, 12, 31), MotifFor.DEPART_HS, MotifFor.ACHAT_IMMOBILIER, list.get(0));
		}

		// 2001
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2001);
			assertNotNull(list);
			assertEquals(1, list.size());
			// le fait de posséder un immeuble en suisse fait basculer le diplomate dans la catégorie hors-Suisse
			assertHorsSuisse(date(2001, 1, 1), date(2001, 12, 31), MotifFor.ACHAT_IMMOBILIER, null, list.get(0));
		}

		// 1999-2010
		{
			List<Assujettissement> list = Assujettissement.determine(paul, RANGE_1999_2010, true);
			assertNotNull(list);
			assertEquals(3, list.size());
			assertOrdinaire(date(1999, 1, 1), date(1999, 12, 31), null, MotifFor.DEPART_HS, list.get(0));
			assertDiplomateSuisse(date(2000, 1, 1), date(2000, 12, 31), MotifFor.DEPART_HS, MotifFor.ACHAT_IMMOBILIER, list.get(1));
			assertHorsSuisse(date(2001, 1, 1), date(2010, 12, 31), MotifFor.ACHAT_IMMOBILIER, null, list.get(2));
		}
	}

	/**
	 * [UNIREG-1390] Vérifie qu'il est possible de déterminer l'assujettissement d'un hors-Suisse qui vend son immeuble et dont le for
	 * principal est fermé sans motif (cas du ctb n°807.110.03).
	 */
	@Test
	public void testDetermineHorsSuisseForPrincipalFermeSansMotif() throws Exception {

		final RegDate dateVente = date(2009, 3, 24);
		final Contribuable ctb = createHorsSuisseVenteImmeubleEtFermetureFFPSansMotif(dateVente);

		final List<Assujettissement> list = Assujettissement.determine(ctb, 2009);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertHorsSuisse(date(2009, 1, 1), dateVente, null, MotifFor.VENTE_IMMOBILIER, list.get(0));
	}

	@Test
	public void testDetermineHorsCantonAvecImmeuble() throws Exception {

		final RegDate dateAchat = date(2008, 4, 21);
		final Contribuable ctb = createHorsCantonAvecImmeuble(dateAchat);

		// 2007
		{
			assertEmpty(Assujettissement.determine(ctb, 2007));
		}

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2008, 1, 1), date(2008, 12, 31), MotifFor.ACHAT_IMMOBILIER, null, list.get(0));
		}

		// 2009
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2009, 1, 1), date(2009, 12, 31), null, null, list.get(0));
		}

		// 2010
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2010);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2010, 1, 1), date(2010, 12, 31), null, null, list.get(0));
		}

		// 2011
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2011);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2011, 1, 1), date(2011, 12, 31), null, null, list.get(0));
		}

		// 2007-2011
		{
			List<Assujettissement> list = Assujettissement.determine(ctb, new DateRangeHelper.Range(date(2007, 1, 1), date(2011, 12, 31)), true);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2008, 1, 1), date(2011, 12, 31), MotifFor.ACHAT_IMMOBILIER, null, list.get(0));
		}
	}

	/**
	 * [UNIREG-1742] Vérifie que l'assujettissement d'un contribuable hors-Suisse débute/arrête bien à l'achat/vente du premier/dernier immeuble. Dans le cas d'achats et de ventes de plusieurs immeubles
	 * (sans chevauchement) dans le même année, les périodes sont donc fractionnées.
	 */
	@Test
	public void testDetermineAchatsEtVentesMultipleHorsSuisse() throws Exception {

		final Range immeuble1 = new Range(date(2008, 1, 15), date(2008, 3, 30));
		final Range immeuble2 = new Range(date(2008, 6, 2), date(2008, 11, 26));
		final Contribuable ctb = createHorsSuisseAvecAchatsEtVentesImmeubles(immeuble1, immeuble2);

		// 2007
		{
			assertEmpty(Assujettissement.determine(ctb, 2007));
		}

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertHorsSuisse(immeuble1.getDateDebut(), immeuble1.getDateFin(), MotifFor.ACHAT_IMMOBILIER, MotifFor.VENTE_IMMOBILIER, list.get(0));
			assertHorsSuisse(immeuble2.getDateDebut(), immeuble2.getDateFin(), MotifFor.ACHAT_IMMOBILIER, MotifFor.VENTE_IMMOBILIER, list.get(1));
		}

		// 2009
		{
			assertEmpty(Assujettissement.determine(ctb, 2009));
		}
	}

	/**
	 * [UNIREG-1742] Vérifie que les périodes d'un contribuable hors-Suisse sourcier sont bien fractionnées en cas d'achat d'un immeuble (passage pur -> mixte, et vice-versa).
	 */
	@Test
	public void testDetermineAchatEtVenteImmeubleHorsSuisseSourcier() throws Exception {

		final RegDate dateAchat = date(2008, 1, 15);
		final RegDate dateVente = date(2008, 3, 30);
		final Contribuable ctb = createHorsSuisseSourcierAvecAchatEtVenteImmeuble(dateAchat, dateVente);

		// 2007
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2007, 1, 1), date(2007, 12, 31), null, null, TypeAutoriteFiscale.PAYS_HS, list.get(0));
		}

		// 2008
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(3, list.size());
			assertSourcierPur(date(2008, 1, 1), dateAchat.getOneDayBefore(), null, MotifFor.ACHAT_IMMOBILIER, TypeAutoriteFiscale.PAYS_HS, list.get(0));
			assertSourcierMixte(dateAchat, dateVente, MotifFor.ACHAT_IMMOBILIER, MotifFor.VENTE_IMMOBILIER, TypeAutoriteFiscale.PAYS_HS, list.get(1));
			assertSourcierPur(dateVente.getOneDayAfter(), date(2008, 12, 31), MotifFor.VENTE_IMMOBILIER, null, TypeAutoriteFiscale.PAYS_HS, list.get(2));
		}

		// 2009
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2009, 1, 1), date(2009, 12, 31), null, null, TypeAutoriteFiscale.PAYS_HS, list.get(0));
		}
	}

	@Test
	public void testDetermineVenteImmeubleHorsCanton() throws Exception {

		final RegDate dateVente = date(2008, 9, 30);
		final Contribuable ctb = createVenteImmeubleHorsCanton(dateVente);

		// 2007
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2007, 1, 1), date(2007, 12, 31), null, null, list.get(0));
		}

		// 2008 (vente de l'immeuble en cours d'année)
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2008, 1, 1), date(2008, 12, 31), null, MotifFor.VENTE_IMMOBILIER, list.get(0));
		}

		// 2009
		{
			// plus assujetti
			assertEmpty(Assujettissement.determine(ctb, 2009));
		}
	}

	@Test
	public void testDetermineFinActiviteHorsCanton() throws Exception {

		final RegDate dateFin = date(2008, 9, 30);
		final Contribuable ctb = createFinActiviteHorsCanton(dateFin);

		// 2007
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2007, 1, 1), date(2007, 12, 31), null, null, list.get(0));
		}

		// 2008 (fin d'activité indépendante en cours d'année)
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2008, 1, 1), date(2008, 12, 31), null, MotifFor.FIN_EXPLOITATION, list.get(0));
		}

		// 2009
		{
			// plus assujetti
			assertEmpty(Assujettissement.determine(ctb, 2009));
		}
	}

	@Test
	public void testDetermineDecesHorsCantonAvecImmeuble() throws Exception {

		final RegDate dateDeces = date(2008, 10, 26);
		final Contribuable ctb = createDecesHorsCantonAvecImmeuble(date(2006, 8, 5), dateDeces);

		// 2007
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2007, 1, 1), date(2007, 12, 31), null, null, list.get(0));
		}

		// 2008 (décès en cours d'année)
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2008, 1, 1), dateDeces, null, MotifFor.VEUVAGE_DECES, list.get(0));
		}

		// 2009
		{
			// plus assujetti
			assertEmpty(Assujettissement.determine(ctb, 2009));
		}
	}

	@Test
	public void testDetermineDecesHorsCantonActiviteIndependante() throws Exception {

		final RegDate dateDeces = date(2008, 2, 23);
		final Contribuable ctb = createDecesHorsCantonActiviteIndependante(date(1990, 4, 13), dateDeces);

		// 2007
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2007, 1, 1), date(2007, 12, 31), null, null, list.get(0));
		}

		// 2008 (décès en cours d'année)
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsCanton(date(2008, 1, 1), dateDeces, null, MotifFor.VEUVAGE_DECES, list.get(0));
		}

		// 2009
		{
			// plus assujetti
			assertEmpty(Assujettissement.determine(ctb, 2009));
		}
	}

	/**
	 * Cas du contribuable n°16109718
	 */
	@Test
	public void testDetermineArriveeHorsSuisseAvecImmeubleEtMotifDemanagement() throws Exception {

		final RegDate dateAchat = date(1998, 10, 17);
		final RegDate dateArrivee = date(2003, 1, 1);
		final Contribuable ctb = createArriveeHorsSuisseAvecImmeubleEtMotifDemanagement(dateAchat, dateArrivee);

		// 2002
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2002);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertHorsSuisse(date(2002, 1, 1), date(2002, 12, 31), null, MotifFor.DEMENAGEMENT_VD, list.get(0));
		}

		// 2003 (arrivée au 1er janvier)
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2003);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2003, 1, 1), date(2003, 12, 31), MotifFor.ARRIVEE_HS, null, list.get(0));
		}

		// 2004
		{
			final List<Assujettissement> list = Assujettissement.determine(ctb, 2004);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2004, 1, 1), date(2004, 12, 31), null, null, list.get(0));
		}
	}

	/**
	 * Cas du contribuable n°10000171
	 */
	@Test
	public void testDetermineCasCompletementTordu() throws Exception {

		final Contribuable paul = createContribuableSansFor();

		ForFiscalPrincipal fp = addForPrincipal(paul, date(2003, 4, 1), MotifFor.INDETERMINE, date(2005, 12, 18), MotifFor.DEPART_HS, MockCommune.Lausanne);
		fp.setModeImposition(ModeImposition.MIXTE_137_2);

		addForPrincipal(paul, date(2005, 12, 19), MotifFor.DEMENAGEMENT_VD, date(2008, 5, 28), MotifFor.DEMENAGEMENT_VD, MockPays.France);

		fp = addForPrincipal(paul, date(2008, 7, 7), MotifFor.ARRIVEE_HS, date(2008, 7, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
		fp.setModeImposition(ModeImposition.MIXTE_137_2);

		fp = addForPrincipal(paul, date(2008, 8, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Vevey);
		fp.setModeImposition(ModeImposition.MIXTE_137_2);

		addForSecondaire(paul, date(2008, 7, 7), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		// 2002
		{
			assertEmpty(Assujettissement.determine(paul, 2002));
		}

		// 2003 (arrivée indéterminée)
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2003);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixte(date(2003, 1, 1), date(2003, 12, 31), MotifFor.INDETERMINE, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2004
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2004);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixte(date(2004, 1, 1), date(2004, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2005 (départ hors-Suisse)
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2005);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixte(date(2005, 1, 1), date(2005, 12, 18), null, MotifFor.DEPART_HS, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2006
		{
			assertEmpty(Assujettissement.determine(paul, 2006));
		}

		// 2007
		{
			assertEmpty(Assujettissement.determine(paul, 2007));
		}

		// 2008 (arrivée de hors-Suisse + achat immobilier)
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2008);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixte(date(2008, 7, 7), date(2008, 12, 31), MotifFor.ARRIVEE_HS, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2009
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2009);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixte(date(2009, 1, 1), date(2009, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}
	}

	/**
	 * Cas du contribuable n°10002045
	 */
	@Test
	public void testDetermineFausseArriveeHorsSuisse() throws Exception {

		final Contribuable paul = createContribuableSansFor();

		ForFiscalPrincipal fp = addForPrincipal(paul, date(2001, 1, 1), MotifFor.ARRIVEE_HS, date(2003, 10, 9), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Aubonne);
		fp.setModeImposition(ModeImposition.SOURCE);

		// le motif d'ouverture du second for principal est incorrect que le for immédiatement précédent n'est pas hors-Suisse.
		// Dans ce cas-là, il ne doit pas y avoir de fractionnement de l'assujettissement.
		fp = addForPrincipal(paul, date(2003, 10, 10), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
		fp.setModeImposition(ModeImposition.MIXTE_137_2);

		// 2000
		{
			assertEmpty(Assujettissement.determine(paul, 2000));
		}

		// 2001 (arrivée HS)
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2001);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2001, 1, 1), date(2001, 12, 31), MotifFor.ARRIVEE_HS, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2002
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2002);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierPur(date(2002, 1, 1), date(2002, 12, 31), null, MotifFor.CHGT_MODE_IMPOSITION, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}

		// 2003 (fausse arrivée HS + changement mode d'imposition)
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2003);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixte(date(2003, 1, 1), date(2003, 12, 31), MotifFor.ARRIVEE_HS, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}
	}

	/**
	 * Cas du contribuable n°10003318
	 */
	@Test
	public void testDetermineFausseArriveeHorsSuisse2() throws Exception {

		final Contribuable paul = createContribuableSansFor();

		addForPrincipal(paul, date(2002, 5, 31), MotifFor.INDETERMINE, date(2002, 6, 4), MotifFor.DEMENAGEMENT_VD, MockCommune.Neuchatel);
		// le motif d'ouverture du second for principal est incorrect que le for immédiatement précédent n'est pas hors-Suisse.
		// Dans ce cas-là, il ne doit pas y avoir de fractionnement de l'assujettissement.
		addForPrincipal(paul, date(2002, 6, 5), MotifFor.ARRIVEE_HS, date(2007, 6, 30), MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(paul, date(2007, 7, 1), MotifFor.DEMENAGEMENT_VD, MockPays.Albanie);

		addForSecondaire(paul, date(2002, 6, 5), MotifFor.ACHAT_IMMOBILIER, date(2002, 12, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		// 2001
		{
			assertEmpty(Assujettissement.determine(paul, 2001));
		}

		// 2002 (achat immeuble puis arrivée HC)
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2002);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2002, 1, 1), date(2002, 12, 31), MotifFor.ARRIVEE_HS, null, list.get(0));
		}

		// 2003
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2003);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2003, 1, 1), date(2003, 12, 31), null, null, list.get(0));
		}

		// 2007 (départ HS)
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2007);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2007, 1, 1), date(2007, 6, 30), null, MotifFor.DEPART_HS, list.get(0));
		}

		// 2008
		{
			assertEmpty(Assujettissement.determine(paul, 2008));
		}
	}

	/**
	 * Cas du contribuable n°10003348
	 */
	@Test
	public void testDetermineSourcierPurePuisMixteSurDepartHSPuisArriveeHSDansLAnneeAvecImmeubleEtMotifsGrandguignolesques() throws Exception {

		final Contribuable paul = createContribuableSansFor();

		ForFiscalPrincipal fp = addForPrincipal(paul, date(2003, 1, 1), MotifFor.ARRIVEE_HS, date(2003, 5, 27), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne);
		fp.setModeImposition(ModeImposition.SOURCE);

		fp = addForPrincipal(paul, date(2003, 5, 28), MotifFor.INDETERMINE, date(2003, 8, 30), MotifFor.DEMENAGEMENT_VD, MockPays.France);
		fp.setModeImposition(ModeImposition.MIXTE_137_1);

		fp = addForPrincipal(paul, date(2003, 8, 31), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
		fp.setModeImposition(ModeImposition.MIXTE_137_2);

		addForSecondaire(paul, date(2003, 5, 28), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		// 2002
		{
			assertEmpty(Assujettissement.determine(paul, 2002));
		}

		// 2003 (arrivée HS sourcier pure + départ HS sourcier mixte 137 al. 1 + arrivée HS sourcier mixte 137 al. 2)
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2003);
			assertNotNull(list);
			assertEquals(3, list.size());
			assertSourcierPur(date(2003, 1, 1), date(2003, 5, 27), MotifFor.ARRIVEE_HS, MotifFor.CHGT_MODE_IMPOSITION, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
			assertSourcierMixte(date(2003, 5, 28), date(2003, 8, 30), MotifFor.INDETERMINE, MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.PAYS_HS, list.get(1));
			assertSourcierMixte(date(2003, 8, 31), date(2003, 12, 31), MotifFor.ARRIVEE_HS, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(2));
		}

		// 2004
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2004);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixte(date(2004, 1, 1), date(2004, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}
	}

	/**
	 * Cas du contribuable n°10002080
	 */
	@Test
	public void testDetermineFauxDemenagementVD() throws Exception {

		final Contribuable paul = createContribuableSansFor();

		// le motif de fermeture est incorrect, il devrait être ARRIVEE_HC
		ForFiscalPrincipal fp = addForPrincipal(paul, date(2003, 9, 9), MotifFor.INDETERMINE, date(2004, 7, 26), MotifFor.DEMENAGEMENT_VD, MockCommune.Neuchatel);
		fp.setModeImposition(ModeImposition.MIXTE_137_1);
		fp = addForPrincipal(paul, date(2004, 7, 27), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		fp.setModeImposition(ModeImposition.MIXTE_137_2);

		addForSecondaire(paul, date(2003, 9, 9), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		// 2002
		{
			assertEmpty(Assujettissement.determine(paul, 2002));
		}

		// 2003 (achat immeuble)
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2003);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixte(date(2003, 1, 1), date(2003, 12, 31), MotifFor.INDETERMINE, null, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
		}

		// 2004 (arrivée hors-canton)
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2004);
			assertNotNull(list);
			assertEquals(2, list.size());
			assertSourcierMixte(date(2004, 1, 1), date(2004, 7, 26), null, MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
			assertSourcierMixte(date(2004, 7, 27), date(2004, 12, 31), MotifFor.ARRIVEE_HC, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(1));
		}

		// 2005
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2005);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertSourcierMixte(date(2005, 1, 1), date(2005, 12, 31), null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(0));
		}
	}

	/**
	 * Cas du contribuable n°10004709
	 */
	@Test
	public void testDetermineFauxDepartHS() throws Exception {

		final Contribuable paul = createContribuableSansFor();

		// Dans le cas d'un contribuable avec deux fors principaux vaudois se touchant avec changement
		// au 31 décembre pour motif DEPART_HS, on vérifie que le motiff DEPART_HS est bien ignoré
		addForPrincipal(paul, date(2005,10, 1), MotifFor.ARRIVEE_HC, date(2005, 12, 31), MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(paul, date(2006,1, 1), MotifFor.INDETERMINE, date(2006, 12, 31), MotifFor.DEPART_HC, MockCommune.Cossonay);

		// 2004
		{
			assertEmpty(Assujettissement.determine(paul, 2004));
		}

		// 2005
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2005);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2005, 1, 1), date(2005, 12, 31), MotifFor.ARRIVEE_HC, null, list.get(0));
		}

		// 2006
		{
			final List<Assujettissement> list = Assujettissement.determine(paul, 2006);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2006, 1, 1), date(2006, 12, 31), null, MotifFor.DEPART_HC, list.get(0));
		}

		// 2007
		{
			assertEmpty(Assujettissement.determine(paul, 2004));
		}

		// 2004-2007
		{
			final List<Assujettissement> list = Assujettissement.determine(paul);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertOrdinaire(date(2005, 1, 1), date(2006, 12, 31), MotifFor.ARRIVEE_HC, MotifFor.DEPART_HC, list.get(0));
		}
	}

	/**
	 * Cas du contribuable n°10008508
	 */
	@Test
	public void testDetermineDepartHSAchatImmeubleEtArriveeHC() throws Exception {

		final Contribuable paul = createContribuableSansFor();

		addForPrincipal(paul, date(1995, 11, 2), MotifFor.ARRIVEE_HS, date(1997, 3, 1), MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(paul, date(1997, 3, 3), MotifFor.DEMENAGEMENT_VD, date(2004, 12, 31), MotifFor.DEPART_HC, MockPays.Albanie);
		addForPrincipal(paul, date(2005, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Neuchatel);
		addForSecondaire(paul, date(2004, 3, 2), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		final List<Assujettissement> list = Assujettissement.determine(paul);
		assertNotNull(list);
		assertEquals(3, list.size());
		assertOrdinaire(date(1995, 11, 2), date(1997, 3, 1), MotifFor.ARRIVEE_HS, MotifFor.DEPART_HS, list.get(0));
		assertHorsSuisse(date(2004, 3, 2), date(2004, 12, 31), MotifFor.ACHAT_IMMOBILIER, MotifFor.DEPART_HC, list.get(1));
		assertHorsCanton(date(2005, 1, 1), null, MotifFor.ARRIVEE_HS, null, list.get(2));
	}

	/**
	 * Cas du contribuable n°10015452
	 */
	@Test
	public void testDetermineSourcierMixteHorsSuisseAvecImmeuble() throws Exception {

		final Contribuable paul = createContribuableSansFor();

		ForFiscalPrincipal fp = addForPrincipal(paul, date(2004, 5, 6), MotifFor.INDETERMINE, date(2006, 7, 31), MotifFor.DEMENAGEMENT_VD, MockPays.Espagne);
		fp.setModeImposition(ModeImposition.MIXTE_137_1);
		fp = addForPrincipal(paul, date(2006, 8, 1), MotifFor.ARRIVEE_HS, MockCommune.Aubonne);
		fp.setModeImposition(ModeImposition.MIXTE_137_2);
		addForSecondaire(paul, date(2004, 5, 6), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		final List<Assujettissement> list = Assujettissement.determine(paul);
		assertNotNull(list);
		assertEquals(2, list.size());
		assertSourcierMixte(date(2004, 5, 6), date(2006, 7, 31), MotifFor.INDETERMINE, MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.PAYS_HS, list.get(0));
		assertSourcierMixte(date(2006, 8, 1), null, MotifFor.ARRIVEE_HS, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, list.get(1));
	}

	/**
	 * Cas du contribuable n°10019036
	 */
	@Test
	public void testDetermineSourcierHCDepartHSEtAchatImmeuble() throws Exception {

		final Contribuable paul = createContribuableSansFor();

		ForFiscalPrincipal fp = addForPrincipal(paul, date(2004, 1, 1), MotifFor.ARRIVEE_HS, date(2004, 6, 14), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Neuchatel);
		fp.setModeImposition(ModeImposition.SOURCE);
		addForPrincipal(paul, date(2004, 6, 15), MotifFor.DEPART_HC, date(2004, 7, 10), MotifFor.DEPART_HC, MockPays.Danemark);
		addForPrincipal(paul, date(2004, 7, 11), MotifFor.ARRIVEE_HS, date(2004, 7, 11), MotifFor.DEMENAGEMENT_VD, MockCommune.Neuchatel);
		addForPrincipal(paul, date(2004, 7, 12), MotifFor.ARRIVEE_HC, date(2006, 11, 15), MotifFor.DEPART_HS, MockCommune.Lausanne);
		addForPrincipal(paul, date(2006, 11, 16), MotifFor.DEMENAGEMENT_VD, MockPays.Albanie);

		addForSecondaire(paul, date(2004, 6, 15), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aubonne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		final List<Assujettissement> list = Assujettissement.determine(paul);
		assertNotNull(list);
		assertEquals(3, list.size());
		assertSourcierPur(date(2004, 1, 1), date(2004, 6, 30), MotifFor.ARRIVEE_HS, MotifFor.CHGT_MODE_IMPOSITION, TypeAutoriteFiscale.COMMUNE_HC, list.get(0));
		assertOrdinaire(date(2004, 7, 1), date(2006, 11, 15), MotifFor.DEPART_HC, MotifFor.DEPART_HS, list.get(1));
		assertHorsSuisse(date(2006, 11, 16), null, MotifFor.DEMENAGEMENT_VD, null, list.get(2));
	}

	@Test
	public void testCommuneActiveForPrincipal() throws Exception {
		final Contribuable ctb = createUnForSimple();
		final List<Assujettissement> assujettissements = Assujettissement.determine(ctb, RANGE_2000_2008, false);
		for (Assujettissement a : assujettissements) {
			assertCommunesActives(a, Arrays.asList(MockCommune.Lausanne.getNoOFS()));
		}
	}

	@Test
	public void testCommuneActivePourHCImmeuble() throws Exception {
		final Contribuable ctb = createHorsCantonAvecImmeuble(date(2006, 3, 12));
		final List<Assujettissement> assujettissements = Assujettissement.determine(ctb, RANGE_2006_2008, false);
		for (Assujettissement a : assujettissements) {
			assertCommunesActives(a, Arrays.asList(MockCommune.Aubonne.getNoOFS()));
		}
	}

	@Test
	public void testCommuneActivePourVaudoisImmeuble() throws Exception {
		final Contribuable ctb = createUnForSimple();
		addForSecondaire(ctb, date(2007, 4, 12), MotifFor.ACHAT_IMMOBILIER, date(2008, 6, 30), MotifFor.VENTE_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		for (int annee = 2006 ; annee < 2010 ; ++ annee) {
			final List<Assujettissement> assujettissements = Assujettissement.determine(ctb, annee);
			final List<Integer> communesActives = new ArrayList<Integer>(2);
			communesActives.add(MockCommune.Lausanne.getNoOFS());
			if (annee >= 2007 && annee <= 2008) {
				communesActives.add(MockCommune.Cossonay.getNoOFS());
			}
			for (Assujettissement a : assujettissements) {
				assertCommunesActives(a, communesActives);
			}
		}
	}

	@Test
	public void testCommuneActiveDemenagementVaudois() throws Exception {
		final Contribuable ctb = createContribuableSansFor();
		addForPrincipal(ctb, date(2005, 2, 4), MotifFor.ARRIVEE_HS, date(2006, 6, 30), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE);
		addForPrincipal(ctb, date(2006, 7, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Leysin);

		for (int annee = 2005 ; annee <= 2007 ; ++ annee) {
			final List<Assujettissement> assujettissements = Assujettissement.determine(ctb, annee);
			final List<Integer> communeActive = Arrays.asList(annee < 2006 ? MockCommune.Lausanne.getNoOFS() : MockCommune.Leysin.getNoOFS());
			for (Assujettissement a : assujettissements) {
				assertCommunesActives(a, communeActive);
			}
		}
	}

	@Test
	public void testCommuneActiveDecesDansAnnee() throws Exception {
		final Contribuable ctb = createDecesVaudoisOrdinaire(date(1990, 4, 21), date(2005, 5, 12));
		final List<Assujettissement> assujettissements = Assujettissement.determine(ctb, 2005);
		for (Assujettissement a : assujettissements) {
			assertCommunesActives(a, Arrays.asList(MockCommune.Lausanne.getNoOFS()));
		}
	}

	@Test
	public void testCommuneActiveDepartHS() throws Exception {
		final Contribuable ctb = createDepartHorsSuisse(date(2005, 5, 12));
		final List<Assujettissement> assujettissements = Assujettissement.determine(ctb, 2005);
		for (Assujettissement a : assujettissements) {
			assertCommunesActives(a, Arrays.asList(MockCommune.Lausanne.getNoOFS()));
		}
	}

	@Test
	public void testCommuneActiveDepartHSAvecImmeuble() throws Exception {
		final Contribuable ctb = createDepartHorsSuisseAvecImmeuble(date(2005, 5, 12));
		final List<Assujettissement> assujettissements = Assujettissement.determine(ctb, 2005);
		for (Assujettissement a : assujettissements) {
			assertCommunesActives(a, Arrays.asList(MockCommune.Lausanne.getNoOFS()));
		}
	}

	private static void assertCommunesActives(Assujettissement assujettissement, List<Integer> noOfsCommunesActives) {
		final Set<Integer> actives;
		if (noOfsCommunesActives != null && noOfsCommunesActives.size() > 0) {
			actives = new HashSet<Integer>(noOfsCommunesActives);
		}
		else {
			actives = new HashSet<Integer>(0);
		}

		int nbActives = 0;
		for (int i = 0 ; i < 10000 ; ++ i) {
			final boolean expected = actives.contains(i);
			final boolean found = assujettissement.isActifSurCommune(i);
			assertEquals("Commune " + i, expected, found);
			if (found) {
				++ nbActives;
			}
		}

		if (noOfsCommunesActives == null) {
			assertEquals(0, nbActives);
		}
		else {
			assertEquals(noOfsCommunesActives.size(), nbActives);
		}
	}
}
