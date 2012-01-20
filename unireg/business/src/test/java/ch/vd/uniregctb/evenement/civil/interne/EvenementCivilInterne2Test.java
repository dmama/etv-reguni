package ch.vd.uniregctb.evenement.civil.interne;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.interfaces.model.Individu;
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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

public class EvenementCivilInterne2Test extends BusinessTest {

	private TiersDAO tiersDAO;
	private EvenementCivilContext context;
	
	private static class DummyEvenementCivilInterne extends EvenementCivilInterne {

		protected DummyEvenementCivilInterne(Individu individu, Individu conjoint, RegDate dateEvenement, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
			super(individu, conjoint, dateEvenement, numeroOfsCommuneAnnonce, context);
		}

		@Override
		protected void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		}

		@Override
		public Pair<PersonnePhysique, PersonnePhysique> handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
			return null;
		}
	}

	private DummyEvenementCivilInterne dummyEvent;

	private static final long NUMERO_INDIVIDU = 54321L;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		final TiersService tiersService = getBean(TiersService.class, "tiersService");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		serviceCivil.setUp(new DefaultMockServiceCivil());
		context = new EvenementCivilContext(serviceCivil, serviceInfra, null, tiersService, null, null, tiersDAO, null, null);
		dummyEvent = new DummyEvenementCivilInterne(null, null, null, null, context);

		doInNewTransaction(new TxCallback<Object>(){
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				return addHabitant(NUMERO_INDIVIDU);
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testUpdateForFiscalPrincipal() {

		/*
		 * Travail préparatoire : for initial à Cossonay
		 */
		final RegDate dateInitiale = RegDate.get(1990, 7, 1);

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(NUMERO_INDIVIDU);
		habitant = (PersonnePhysique)tiersDAO.save(habitant);

		dummyEvent.openForFiscalPrincipalDomicileVaudoisOrdinaire(habitant, dateInitiale, MockCommune.Cossonay.getNoOFSEtendu(), MotifFor.ARRIVEE_HC, true);
		assertEquals(1, habitant.getForsFiscaux().size());
		final ForFiscalPrincipal forInitial = (ForFiscalPrincipal) habitant.getForsFiscauxSorted().get(0);

		final RegDate dateChangement = RegDate.get(1998, 3, 1);
		final RegDate veilleChangement = dateChangement.getOneDayBefore();

		/*
		 * Arrivée en doublon : aucun changement
		 */
		{
			assertEquals(1, habitant.getForsFiscaux().size());
			dummyEvent.updateForFiscalPrincipal(habitant, dateChangement, MockCommune.Cossonay.getNoOFSEtendu(), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null,
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
			dummyEvent.updateForFiscalPrincipal(habitant, dateChangement, MockCommune.LesClees.getNoOFSEtendu(), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null,
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
	@Transactional(rollbackFor = Throwable.class)
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
		dummyEvent.updateForFiscalPrincipal(habitant, RegDate.get(2004,7,1), MockCommune.Lausanne.getNoOFSEtendu(), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null, true);

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

	private static void assertContent(List<String> msgs, List<? extends EvenementCivilErreur> erreurs) {
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
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateCommon() throws EvenementCivilException {
		final MessageCollector collector = new MessageCollector();
		final Individu individu = serviceCivil.getIndividu(NUMERO_INDIVIDU, null);

		//test OK
		final EvenementCivilInterne even = new DummyEvenementCivilInterne(individu, null, RegDate.get(1990, 7, 1),356, context);
		even.validate(collector, collector);
		assertFalse(collector.hasErreurs());
		assertFalse(collector.hasWarnings());

		//test KO date null
		final EvenementCivilInterne evenDateNull = new DummyEvenementCivilInterne(individu, null, null, 356, context);
		evenDateNull.validate(collector, collector);
		assertTrue(collector.hasErreurs());
		assertFalse(collector.hasWarnings());
		assertContent(Arrays.asList("L'événement n'est pas daté"), collector.getErreurs());
		collector.clear();

		//test KO date future
		final EvenementCivilInterne evenDateFuture = new DummyEvenementCivilInterne(individu, null, RegDate.get().addYears(2), 356, context);
		evenDateFuture.validate(collector, collector);
		assertTrue(collector.hasErreurs());
		assertFalse(collector.hasWarnings());
		assertContent(Arrays.asList("La date de l'événement est dans le futur"), collector.getErreurs());
		collector.clear();

		//test KO numéro OFS null
		final EvenementCivilInterne evenOFSNull = new DummyEvenementCivilInterne(individu, null, RegDate.get(1990, 7, 1), null, context);
		evenOFSNull.validate(collector, collector);
		assertTrue(collector.hasErreurs());
		assertFalse(collector.hasWarnings());
		assertContent(Arrays.asList("La commune d'annonce n'est pas renseignée"), collector.getErreurs());
		collector.clear();

		//test OK numéro OFS commune du sentier
		final EvenementCivilInterne evenOFSSentier = new DummyEvenementCivilInterne(individu, null, RegDate.get(1990, 7, 1), 8000, context);
		evenOFSSentier.validate(collector, collector);
		assertFalse(collector.hasErreurs());
		assertFalse(collector.hasWarnings());
		collector.clear();
	}
}
