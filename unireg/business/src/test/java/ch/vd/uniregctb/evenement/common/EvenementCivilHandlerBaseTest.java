package ch.vd.uniregctb.evenement.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementCivil;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

public class EvenementCivilHandlerBaseTest extends BusinessTest {

	private TiersService tiersService;
	private TiersDAO tiersDAO;
	private final EvenementCivilHandlerBase handler = new EvenementCivilHandlerBase() {

		@Override
		public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		}

		@Override
		protected Set<TypeEvenementCivil> getHandledType() {
			return null;
		}

		@Override
		public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
			return null;
		}

		@Override
		protected void validateSpecific(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		}

		@Override
		public GenericEvenementAdapter createAdapter() {
			return null;
		}

	};

	private static final long NUMERO_INDIVIDU = 54321L;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		tiersService = getBean(TiersService.class, "tiersService");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		handler.setService(tiersService);
		serviceCivil.setUp(new DefaultMockServiceCivil());
	}

	@Test
	public void testUpdateForFiscalPrincipal() {

		/*
		 * Travail préparatoire : for initial à Cossonay
		 */
		final RegDate dateInitiale = RegDate.get(1990, 7, 1);

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(NUMERO_INDIVIDU);
		habitant = (PersonnePhysique)tiersDAO.save(habitant);

		handler.openForFiscalPrincipalDomicileVaudoisOrdinaire(habitant, dateInitiale, MockCommune.Cossonay.getNoOFSEtendu(), MotifFor.ARRIVEE_HC, true);
		assertEquals(1, habitant.getForsFiscaux().size());
		final ForFiscalPrincipal forInitial = (ForFiscalPrincipal) habitant.getForsFiscauxSorted().get(0);

		final RegDate dateChangement = RegDate.get(1998, 3, 1);
		final RegDate veilleChangement = dateChangement.getOneDayBefore();

		/*
		 * Arrivée en doublon : aucun changement
		 */
		{
			assertEquals(1, habitant.getForsFiscaux().size());
			handler.updateForFiscalPrincipal(habitant, dateChangement, MockCommune.Cossonay.getNoOFSEtendu(), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null,
					true
			);
			assertEquals(1, habitant.getForsFiscaux().size());
			assertNull(forInitial.getDateFin());
		}

		/*
		 * Arrivée normale
		 */
		{
			assertEquals(1, habitant.getForsFiscaux().size());
			handler.updateForFiscalPrincipal(habitant, dateChangement, MockCommune.LesClees.getNoOFSEtendu(), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null,
					true
			);
			assertEquals(2, habitant.getForsFiscaux().size());
			assertEquals(veilleChangement, forInitial.getDateFin());

			final ForFiscalPrincipal nouveauFor = (ForFiscalPrincipal) habitant.getForsFiscauxSorted().get(1);
			assertEquals(dateChangement, nouveauFor.getDateDebut());
			assertNull(nouveauFor.getDateFin());
		}
	}

	@Test
	public void testUpdateForFiscalPrincipalModeImpositionInvariant() {

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(NUMERO_INDIVIDU);
		ForFiscalPrincipal f = new ForFiscalPrincipal();
		f.setDateDebut(RegDate.get(2000,1,1));
		f.setMotifOuverture(MotifFor.ARRIVEE_HC);
		f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		f.setNumeroOfsAutoriteFiscale(MockCommune.Cossonay.getNoOFSEtendu());
		f.setMotifRattachement(MotifRattachement.DOMICILE);
		f.setModeImposition(ModeImposition.SOURCE);
		habitant.addForFiscal(f);
		habitant = (PersonnePhysique)tiersDAO.save(habitant);

		// déménagement sur Lausanne
		handler.updateForFiscalPrincipal(habitant, RegDate.get(2004,7,1), MockCommune.Lausanne.getNoOFSEtendu(), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null, true);

		// on vérifie que le type d'autorité fiscale, le motif de rattachement et le mode d'imposition restent inchangés
		final List<ForFiscal> fors = habitant.getForsFiscauxSorted();
		assertEquals(2, fors.size());

		final ForFiscalPrincipal forCossonay = (ForFiscalPrincipal) fors.get(0);
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forCossonay.getTypeAutoriteFiscale());
		assertEquals(MotifRattachement.DOMICILE, forCossonay.getMotifRattachement());
		assertEquals(ModeImposition.SOURCE, forCossonay.getModeImposition());

		final ForFiscalPrincipal forLausanne = (ForFiscalPrincipal) fors.get(1);
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forLausanne.getTypeAutoriteFiscale());
		assertEquals(MotifRattachement.DOMICILE, forLausanne.getMotifRattachement());
		assertEquals(ModeImposition.SOURCE, forLausanne.getModeImposition());
	}

	private static void assertContent(List<String> msgs, List<EvenementCivilErreur> erreurs) {
		assertEquals(msgs.size(), erreurs.size());
		for (int i = 0 ; i < msgs.size() ; ++ i) {
			final String expected = msgs.get(i);
			final EvenementCivilErreur erreur = erreurs.get(i);
			assertNotNull(expected);
			assertNotNull(erreur);
			assertEquals(expected, erreur.getMessage());
		}
	}

	@Test
	public void testValidateCommon() {
		final List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		final List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();

		//test OK
		final MockEvenementCivil even = new MockEvenementCivil();
		even.setIndividu(serviceCivil.getIndividu(NUMERO_INDIVIDU, 2400));
		even.setDate(RegDate.get(1990, 7, 1));
		even.setNumeroOfsCommuneAnnonce(356);
		handler.validate(even, erreurs, warnings);
		assertTrue(erreurs.isEmpty());
		assertTrue(warnings.isEmpty());

		//test KO date null
		final MockEvenementCivil evenDateNull = new MockEvenementCivil();
		evenDateNull.setIndividu(serviceCivil.getIndividu(NUMERO_INDIVIDU, 2400));
		evenDateNull.setDate(null);
		evenDateNull.setNumeroOfsCommuneAnnonce(356);
		handler.validate(evenDateNull, erreurs, warnings);
		assertFalse(erreurs.isEmpty());
		assertTrue(warnings.isEmpty());
		assertContent(Arrays.asList("L'événement n'est pas daté"), erreurs);
		erreurs.clear();
		warnings.clear();

		//test KO date future
		final MockEvenementCivil evenDateFuture = new MockEvenementCivil();
		evenDateFuture.setIndividu(serviceCivil.getIndividu(NUMERO_INDIVIDU, 2400));
		evenDateFuture.setDate(RegDate.get(2012, 7, 1));
		evenDateFuture.setNumeroOfsCommuneAnnonce(356);
		handler.validate(evenDateFuture, erreurs, warnings);
		assertFalse(erreurs.isEmpty());
		assertTrue(warnings.isEmpty());
		assertContent(Arrays.asList("La date de l'événement est dans le futur"), erreurs);
		erreurs.clear();
		warnings.clear();

		//test KO numéro OFS null
		final MockEvenementCivil evenOFSNull = new MockEvenementCivil();
		evenOFSNull.setIndividu(serviceCivil.getIndividu(NUMERO_INDIVIDU, 2400));
		evenOFSNull.setDate(RegDate.get(1990, 7, 1));
		evenOFSNull.setNumeroOfsCommuneAnnonce(null);
		handler.validate(evenOFSNull, erreurs, warnings);
		assertFalse(erreurs.isEmpty());
		assertTrue(warnings.isEmpty());
		assertContent(Arrays.asList("La commune d'annonce n'est pas renseignée"), erreurs);
		erreurs.clear();
		warnings.clear();

		//test OK numéro OFS commune du sentier
		final MockEvenementCivil evenOFSSentier = new MockEvenementCivil();
		evenOFSSentier.setIndividu(serviceCivil.getIndividu(NUMERO_INDIVIDU, 2400));
		evenOFSSentier.setDate(RegDate.get(1990, 7, 1));
		evenOFSSentier.setNumeroOfsCommuneAnnonce(8000);
		handler.validate(evenOFSSentier, erreurs, warnings);
		assertTrue(erreurs.isEmpty());
		assertTrue(warnings.isEmpty());
		erreurs.clear();
		warnings.clear();
	}
}
