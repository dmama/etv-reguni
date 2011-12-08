package ch.vd.uniregctb.couple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.security.DroitAccesDAO;
import ch.vd.uniregctb.tiers.DroitAcces;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeDroitAcces;
import ch.vd.uniregctb.validation.EntityValidator;
import ch.vd.uniregctb.validation.ValidationService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

@ContextConfiguration(locations = {
		"classpath:ch/vd/uniregctb/couple/manager/config.xml"
})
public class CoupleManagerImplTest extends BusinessTest {

	private CoupleManager mngr;
	private DroitAccesDAO droitAccesDAO;
	private ValidationService validationService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		tiersService = getBean(TiersService.class, "tiersService");
		droitAccesDAO = getBean(DroitAccesDAO.class, "droitAccesDAO");
		mngr = getBean(CoupleManager.class, "coupleManager");
		validationService = getBean(ValidationService.class, "validationService");
	}

	// [UNIREG-1521]
	@Test
	public void testTranformationNHEnMenageCommun() throws Exception {

		// Création de trois personnes physiques dont une représente en fait un ménage commun
		final int noIndArnold = 829837;
		final int noIndJanine = 829838;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu arnold = addIndividu(noIndArnold, date(1970, 1, 1), "Arnold", "Simon", true);
				addNationalite(arnold, MockPays.Suisse, date(1970, 1, 1), null);
				MockIndividu janine = addIndividu(noIndJanine, date(1970, 1, 1), "Janine", "Simon", false);
				addNationalite(janine, MockPays.Suisse, date(1970, 1, 1), null);
			}
		});

		class Ids {
			long arnold;
			long janine;
			long menage;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique arnold = addHabitant(noIndArnold);
				ids.arnold = arnold.getNumero();
				PersonnePhysique janine = addHabitant(noIndJanine);
				ids.janine = janine.getNumero();
				PersonnePhysique menage = addNonHabitant("Arnold", "Simon", date(1970, 1, 1), Sexe.MASCULIN);
				addForPrincipal(menage, RegDate.get(), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				ids.menage = menage.getNumero();
				return null;
			}
		});

		// Regroupement des trois personnes physiques en un ménage, avec transformation en ménage commun d'une des personnes physiques
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				mngr.sauverCouple(ids.arnold, ids.janine, ids.menage, RegDate.get(), TypeUnion.COUPLE, EtatCivil.MARIE, null);
				return null;
			}
		});

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// On s'assure que le ménage commun est bien de la bonne classe et qu'il est bien composé
				final Tiers mc = hibernateTemplate.get(Tiers.class, ids.menage);
				assertNotNull(mc);
				assertEquals(MenageCommun.class, mc.getClass());
				assertEquals(2, mc.getRapportsObjet().size()); // possède bien deux parties

				final Tiers arnold = hibernateTemplate.get(Tiers.class, ids.arnold);
				assertNotNull(arnold);
				assertEquals(PersonnePhysique.class, arnold.getClass());
				assertEquals(1, arnold.getRapportsSujet().size()); // fait partie du ménage commun

				final Tiers janine = hibernateTemplate.get(Tiers.class, ids.janine);
				assertNotNull(janine);
				assertEquals(PersonnePhysique.class, janine.getClass());
				assertEquals(1, janine.getRapportsSujet().size()); // fait partie du ménage commun
				return null;
			}
		});
	}

	// [UNIREG-3011]
	@Test
	public void testTranformationNHEnMenageCommunAvecForsFiscauxPreexistants() throws Exception {

		// Création de trois personnes physiques dont une représente en fait un ménage commun
		final int noIndOleg = 815993;
		final int noIndAgnes = 405927;
		final RegDate dateArrivee = date(1995, 10, 19);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu arnold = addIndividu(noIndOleg, date(1970, 1, 1), "Kulinich", "Oleg", true);
				addNationalite(arnold, MockPays.Suisse, date(1970, 1, 1), null);
				MockIndividu janine = addIndividu(noIndAgnes, date(1970, 1, 1), "Baubault", "Agnès", false);
				addNationalite(janine, MockPays.Suisse, date(1970, 1, 1), null);
			}
		});

		class Ids {
			long arnold;
			long janine;
			long menage;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique arnold = addHabitant(noIndOleg);
				ids.arnold = arnold.getNumero();
				PersonnePhysique janine = addHabitant(noIndAgnes);
				ids.janine = janine.getNumero();
				// [UNIREG-3011] Crée un non-habitant avec un for fiscal principal ouvert
				PersonnePhysique menage = addNonHabitant("Kulinich", "Oleg", date(1970, 1, 1), Sexe.MASCULIN);
				addForPrincipal(menage, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				ids.menage = menage.getNumero();
				return null;
			}
		});

		// Regroupement des trois personnes physiques en un ménage, avec transformation en ménage commun d'une des personnes physiques
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				mngr.sauverCouple(ids.arnold, ids.janine, ids.menage, dateArrivee, TypeUnion.COUPLE, EtatCivil.MARIE, null);
				return null;
			}
		});

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// On s'assure que le ménage commun est bien de la bonne classe et qu'il est bien composé
				final Tiers mc = hibernateTemplate.get(Tiers.class, ids.menage);
				assertNotNull(mc);
				assertEquals(MenageCommun.class, mc.getClass());
				assertEquals(2, mc.getRapportsObjet().size()); // possède bien deux parties

				final Tiers arnold = hibernateTemplate.get(Tiers.class, ids.arnold);
				assertNotNull(arnold);
				assertEquals(PersonnePhysique.class, arnold.getClass());
				assertEquals(1, arnold.getRapportsSujet().size()); // fait partie du ménage commun

				final Tiers janine = hibernateTemplate.get(Tiers.class, ids.janine);
				assertNotNull(janine);
				assertEquals(PersonnePhysique.class, janine.getClass());
				assertEquals(1, janine.getRapportsSujet().size()); // fait partie du ménage commun
				return null;
			}
		});
	}

	// [UNIREG-2893] Droits d'accès sur le non-habitant à transformer en couple
	@Test
	public void testTranformationNHAvecDroitsAccesEnMenageCommun() throws Exception {

		// Création de trois personnes physiques dont une représente en fait un ménage commun
		final int noIndArnold = 829837;
		final int noIndJanine = 829838;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu arnold = addIndividu(noIndArnold, date(1970, 1, 1), "Arnold", "Simon", true);
				addNationalite(arnold, MockPays.Suisse, date(1970, 1, 1), null);
				final MockIndividu janine = addIndividu(noIndJanine, date(1970, 1, 1), "Janine", "Simon", false);
				addNationalite(janine, MockPays.Suisse, date(1970, 1, 1), null);
			}
		});

		class Ids {
			long arnold;
			long janine;
			long menage;
		}

		final long operateurAvecDroitFerme = 1;
		final long operateurAvecDroitOuvert = 2;

		final Ids ids = doInNewTransactionAndSession(new TxCallback<Ids>() {
			@Override
			public Ids execute(TransactionStatus status) throws Exception {
				final PersonnePhysique arnold = addHabitant(noIndArnold);
				final PersonnePhysique janine = addHabitant(noIndJanine);
				final PersonnePhysique menage = addNonHabitant("Arnold", "Simon", date(1970, 1, 1), Sexe.MASCULIN);
				addForPrincipal(menage, RegDate.get(), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				addDroitAcces(operateurAvecDroitFerme, menage, TypeDroitAcces.AUTORISATION, Niveau.LECTURE, date(2005, 12, 1), date(2010, 6, 12));
				addDroitAcces(operateurAvecDroitOuvert, menage, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE, date(2005, 12, 1), null);

				final Ids ids = new Ids();
				ids.arnold = arnold.getNumero();
				ids.janine = janine.getNumero();
				ids.menage = menage.getNumero();
				return ids;
			}
		});

		// Regroupement des trois personnes physiques en un ménage, avec transformation en ménage commun d'une des personnes physiques
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				mngr.sauverCouple(ids.arnold, ids.janine, ids.menage, RegDate.get(), TypeUnion.COUPLE, EtatCivil.MARIE, null);
				return null;
			}
		});

		// on s'assure que les droits d'accès sont au bon endroit

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// on doit avoir effacé les droits pour cet opérateurs (ils ne sont plus valables sur le ménage et ne doivent pas être repris sur la PP car ils sont fermés)
				final List<DroitAcces> droitsPourOperateurFerme = droitAccesDAO.getDroitsAcces(operateurAvecDroitFerme);
				assertNotNull(droitsPourOperateurFerme);
				assertEquals(0, droitsPourOperateurFerme.size());

				// le droit sur l'ancien non-habitant doit avoir été reproduit sur les deux membres du ménage
				final List<DroitAcces> droitsPourOperateurOuvert = droitAccesDAO.getDroitsAcces(operateurAvecDroitOuvert);
				assertNotNull(droitsPourOperateurOuvert);
				assertEquals(2, droitsPourOperateurOuvert.size());

				// on trie la liste par numéro de tiers : Arnold a été créé d'abord, il a donc un numéro de tiers plus petit
				final List<DroitAcces> droits = new ArrayList<DroitAcces>(droitsPourOperateurOuvert);
				Collections.sort(droits, new Comparator<DroitAcces>() {
					@Override
					public int compare(DroitAcces o1, DroitAcces o2) {
						final long n1 = o1.getTiers().getNumero();
						final long n2 = o2.getTiers().getNumero();
						return n1 > n2 ? 1 : (n1 < n2 ? -1 : 0);
					}
				});
				final RegDate aujourdhui = RegDate.get();
				{
					final DroitAcces droit = droits.get(0);
					assertEquals(ids.arnold, (long) droit.getTiers().getNumero());
					assertEquals(Niveau.ECRITURE, droit.getNiveau());
					assertEquals(aujourdhui, droit.getDateDebut());
					assertNull(droit.getDateFin());
					assertFalse(droit.isAnnule());
				}
				{
					final DroitAcces droit = droits.get(1);
					assertEquals(ids.janine, (long) droit.getTiers().getNumero());
					assertEquals(Niveau.ECRITURE, droit.getNiveau());
					assertEquals(aujourdhui, droit.getDateDebut());
					assertNull(droit.getDateFin());
					assertFalse(droit.isAnnule());
				}
				return null;
			}
		});
	}

	// [UNIREG-1521] Teste que toutes les opérations sont bien rollées-back en cas d'erreur de validation
	@Test
	public void testTranformationNHEnMenageCommunEtRollback() throws Exception {

		// Création de trois personnes physiques dont une représente en fait un ménage commun
		final int noIndArnold = 829837;
		final int noIndJanine = 829838;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu arnold = addIndividu(noIndArnold, date(1970, 1, 1), "Arnold", "Simon", true);
				addNationalite(arnold, MockPays.Suisse, date(1970, 1, 1), null);
				MockIndividu janine = addIndividu(noIndJanine, date(1970, 1, 1), "Janine", "Simon", false);
				addNationalite(janine, MockPays.Suisse, date(1970, 1, 1), null);
			}
		});

		class Ids {
			long arnold;
			long janine;
			long menage;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique arnold = addHabitant(noIndArnold);
				ids.arnold = arnold.getNumero();
				PersonnePhysique janine = addHabitant(noIndJanine);
				ids.janine = janine.getNumero();
				PersonnePhysique menage = addNonHabitant("Arnold", "Simon", date(1970, 1, 1), Sexe.MASCULIN);
				addForPrincipal(menage, date(1988, 1, 1), MotifFor.MAJORITE, MockCommune.Fraction.LAbbaye);
				ids.menage = menage.getNumero();
				return null;
			}
		});

		// on s'assure que le ménage-commun ne validera pas
		final MenageCommunTrapValidator trap = new MenageCommunTrapValidator(ids.menage);
		validationService.registerValidator(MenageCommun.class, trap);

		// Essai de regroupement des trois personnes physiques en un ménage, avec transformation
		// en ménage commun d'une des personnes physiques + erreur de validation sur le ménage-commun résultant
		try {
			doInNewTransactionAndSession(new TxCallback<Object>() {
				@Override
				public Object execute(TransactionStatus status) throws Exception {
					mngr.sauverCouple(ids.arnold, ids.janine, ids.menage, date(1988, 1, 1), TypeUnion.COUPLE, EtatCivil.MARIE, null);
					return null;
				}
			});
			fail("Le ménage-commun ne devrait pas valider à cause du validateur piégé enregistré dans le validation service.");
		}
		catch (Exception e) {
			assertContains("MenageCommun #" + ids.menage + " - 1 erreur(s) - 0 warning(s):\n" +
					" [E] Erreur de validation bidon\n", e.getMessage());
		}
		finally {
			validationService.unregisterValidator(MenageCommun.class, trap);
		}

		// On s'assure que la transaction a bien été rollée-back, c'est-à-dire que le trois tiers sont toujours des personnes physiques.
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Tiers arnold = hibernateTemplate.get(Tiers.class, ids.arnold);
				assertNotNull(arnold);
				assertEquals(PersonnePhysique.class, arnold.getClass());
				assertEmpty(arnold.getRapportsSujet()); // pas associé à un ménage
				assertEmpty(arnold.getRapportsObjet());

				final Tiers janine = hibernateTemplate.get(Tiers.class, ids.janine);
				assertNotNull(janine);
				assertEquals(PersonnePhysique.class, janine.getClass());
				assertEmpty(janine.getRapportsSujet()); // pas associé à un ménage
				assertEmpty(janine.getRapportsObjet());

				final Tiers mc = hibernateTemplate.get(Tiers.class, ids.menage);
				assertNotNull(mc);
				assertEquals(PersonnePhysique.class, mc.getClass());
				assertEmpty(mc.getRapportsSujet()); // pas associé à un ménage
				assertEmpty(mc.getRapportsObjet());
				return null;
			}
		});
	}

	@Test
	public void testReconciliationDeSeparesAuCivilMariageInconnuAuFiscal() throws Exception {

		final long noMr = 1234567L;
		final long noMme = 1234568L;
		final RegDate dateMariage = date(1971, 4, 17);
		final RegDate dateSeparation = date(2005, 10, 13);

		// civil
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu mr = addIndividu(noMr, date(1948, 1, 26), "Tartempion", "Robert", true);
				final MockIndividu mme = addIndividu(noMme, date(1948, 9, 4), "Tartempion", "Martine", false);
				marieIndividus(mr, mme, dateMariage);
				separeIndividus(mr, mme, dateSeparation);
			}
		});

		final class Ids {
			long noHabMr;
			long noHabMme;
		}
		final Ids ids = new Ids();

		// fiscal de départ
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique mr = addHabitant(noMr);
				final PersonnePhysique mme = addHabitant(noMme);
				addForPrincipal(mr, dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);
				addForPrincipal(mme, dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Bussigny);
				ids.noHabMr = mr.getNumero();
				ids.noHabMme = mme.getNumero();
				return null;
			}
		});

		// re-création du couple
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				mngr.sauverCouple(ids.noHabMr, ids.noHabMme, null, RegDate.get(), TypeUnion.COUPLE, EtatCivil.MARIE, null);
				return null;
			}
		});
	}

	private static class MenageCommunTrapValidator implements EntityValidator<MenageCommun> {
		private final Set<Long> ids;

		public MenageCommunTrapValidator(Long... ids) {
			this.ids = new HashSet<Long>(Arrays.asList(ids));
		}

		@Override
		public ValidationResults validate(MenageCommun entity) {
			final ValidationResults results = new ValidationResults();
			if (ids.contains(entity.getNumero())) {
				results.addError("Erreur de validation bidon");
			}
			return results;
		}
	}
}
