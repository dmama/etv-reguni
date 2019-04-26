package ch.vd.unireg.evenement.civil.interne;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.common.EvenementErreur;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

		@NotNull
		@Override
		public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
			return HandleStatus.TRAITE;
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
		context = new EvenementCivilContext(serviceCivil, serviceInfra, null, tiersService, null, null, tiersDAO, null, null, null, audit);
		dummyEvent = new DummyEvenementCivilInterne(null, null, null, null, context);

		doInNewTransaction(new TxCallback<Object>(){
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				return addHabitant(NUMERO_INDIVIDU);
			}
		});
	}

	@Test
	public void testUpdateForFiscalPrincipal() throws Exception {

		/*
		 * Travail préparatoire : for initial à Cossonay
		 */
		final RegDate dateInitiale = RegDate.get(1990, 7, 1);

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				PersonnePhysique habitant = new PersonnePhysique(true);
				habitant.setNumeroIndividu(NUMERO_INDIVIDU);
				habitant = (PersonnePhysique)tiersDAO.save(habitant);

				dummyEvent.openForFiscalPrincipalDomicileVaudoisOrdinaire(habitant, dateInitiale, MockCommune.Cossonay.getNoOFS(), MotifFor.ARRIVEE_HC);
				assertEquals(1, habitant.getForsFiscaux().size());
				final ForFiscalPrincipal forInitial = (ForFiscalPrincipal) habitant.getForsFiscauxSorted().get(0);

				final RegDate dateChangement = RegDate.get(1998, 3, 1);
				final RegDate veilleChangement = dateChangement.getOneDayBefore();

				/*
				 * Arrivée en doublon : aucun changement
				 */
				{
					assertEquals(1, habitant.getForsFiscaux().size());
					dummyEvent.updateForFiscalPrincipal(habitant, dateChangement, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Cossonay.getNoOFS(), null, MotifFor.DEMENAGEMENT_VD, null);
					assertEquals(1, habitant.getForsFiscaux().size());
					assertNull(forInitial.getDateFin());
				}

				/*
				 * Arrivée normale
				 */
				{
					assertEquals(1, habitant.getForsFiscaux().size());
					dummyEvent.updateForFiscalPrincipal(habitant, dateChangement, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.LesClees.getNoOFS(), null, MotifFor.DEMENAGEMENT_VD, null);
					assertEquals(2, habitant.getForsFiscaux().size());
					assertEquals(veilleChangement, forInitial.getDateFin());

					final ForFiscalPrincipal nouveauFor = (ForFiscalPrincipal) habitant.getForsFiscauxSorted().get(1);
					assertEquals(dateChangement, nouveauFor.getDateDebut());
					assertNull(nouveauFor.getDateFin());
				}
			}
		});
	}

	@Test
	public void testUpdateForFiscalPrincipalModeImpositionInvariant() throws Exception {

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				PersonnePhysique habitant = new PersonnePhysique(true);
				habitant.setNumeroIndividu(NUMERO_INDIVIDU);
				ForFiscalPrincipalPP f = new ForFiscalPrincipalPP();
				f.setDateDebut(RegDate.get(2000,1,1));
				f.setMotifOuverture(MotifFor.ARRIVEE_HC);
				f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
				f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
				f.setNumeroOfsAutoriteFiscale(MockCommune.Cossonay.getNoOFS());
				f.setMotifRattachement(MotifRattachement.DOMICILE);
				f.setModeImposition(ModeImposition.SOURCE);
				habitant.addForFiscal(f);
				habitant = (PersonnePhysique)tiersDAO.save(habitant);

				// déménagement sur Lausanne
				dummyEvent.updateForFiscalPrincipal(habitant, RegDate.get(2004,7,1), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), null, MotifFor.DEMENAGEMENT_VD, null);

				// on vérifie que le type d'autorité fiscale, le motif de rattachement et le mode d'imposition restent inchangés
				final List<ForFiscal> fors = habitant.getForsFiscauxSorted();
				assertEquals(2, fors.size());

				final ForFiscalPrincipalPP forCossonay = (ForFiscalPrincipalPP) fors.get(0);
				assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forCossonay.getTypeAutoriteFiscale());
				assertEquals(MotifRattachement.DOMICILE, forCossonay.getMotifRattachement());
				assertEquals(ModeImposition.SOURCE, forCossonay.getModeImposition());

				final ForFiscalPrincipalPP forLausanne = (ForFiscalPrincipalPP) fors.get(1);
				assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forLausanne.getTypeAutoriteFiscale());
				assertEquals(MotifRattachement.DOMICILE, forLausanne.getMotifRattachement());
				assertEquals(ModeImposition.SOURCE, forLausanne.getModeImposition());
			}
		});
	}

	private static void assertContent(List<String> msgs, List<? extends EvenementErreur> erreurs) {
		assertEquals(msgs.size(), erreurs.size());
		for (int i = 0 ; i < msgs.size() ; ++ i) {
			final String expected = msgs.get(i);
			final EvenementErreur erreur = erreurs.get(i);
			assertNotNull(expected);
			assertNotNull(erreur);
			assertEquals(expected, erreur.getMessage());
		}
	}

	@Test
	public void testValidateCommon() throws Exception {
		final MessageCollector collector = new MessageCollector();
		final Individu individu = serviceCivil.getIndividu(NUMERO_INDIVIDU, null);

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
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
				assertContent(Collections.singletonList("L'événement n'est pas daté"), collector.getErreurs());
				collector.clear();

				//test KO date future
				final EvenementCivilInterne evenDateFuture = new DummyEvenementCivilInterne(individu, null, RegDate.get().addYears(2), 356, context);
				evenDateFuture.validate(collector, collector);
				assertTrue(collector.hasErreurs());
				assertFalse(collector.hasWarnings());
				assertContent(Collections.singletonList("La date de l'événement est dans le futur"), collector.getErreurs());
				collector.clear();

				//test OK numéro OFS commune du sentier
				final EvenementCivilInterne evenOFSSentier = new DummyEvenementCivilInterne(individu, null, RegDate.get(1990, 7, 1), 8000, context);
				evenOFSSentier.validate(collector, collector);
				assertFalse(collector.hasErreurs());
				assertFalse(collector.hasWarnings());
				collector.clear();
			}
		});
	}
}
