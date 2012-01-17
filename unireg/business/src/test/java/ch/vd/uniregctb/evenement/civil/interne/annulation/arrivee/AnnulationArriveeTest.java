package ch.vd.uniregctb.evenement.civil.interne.annulation.arrivee;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;

public class AnnulationArriveeTest extends AbstractEvenementCivilInterneTest {

	private AnnulationArrivee createValideAnnulationArrivee(Individu individu) {
		final Long principalPPId = tiersDAO.getNumeroPPByNumeroIndividu(individu.getNoTechnique(), true);
		return new AnnulationArrivee(individu, principalPPId, null, null, RegDate.get(), MockCommune.Lausanne.getNoOFSEtendu(), context);
	}

	private static enum ErrorLocation {
		CHECK_COMPLETENESS,
		VALIDATE
	}

	private static class ErrorFoundException extends Exception {
		public final ErrorLocation location;
		public final List<EvenementCivilExterneErreur> erreurs;
		public final List<EvenementCivilExterneErreur> warnings;

		private ErrorFoundException(ErrorLocation location, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
			this.location = location;
			this.erreurs = erreurs;
			this.warnings = warnings;
		}

		@Override
		public String toString() {
			return "ErrorFoundException{" +
					"location=" + location +
					", erreurs=" + (erreurs != null ? Arrays.toString(erreurs.toArray()) : "null") +
					", warnings=" + (warnings != null ? Arrays.toString(warnings.toArray()) : "null") +
					'}';
		}
	}

	/**
	 * @param evt événement à envoyer dans le handler
	 * @return la liste des warnings reçus
	 * @throws ErrorFoundException si des erreurs ont été levées dans la méthode validate du handler
	 */
	private List<EvenementCivilExterneErreur> sendEvent(AnnulationArrivee evt) throws ErrorFoundException, EvenementCivilException {

		final List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();

		evt.validate(erreurs, warnings);
		if (!erreurs.isEmpty()) {
			throw new ErrorFoundException(ErrorLocation.VALIDATE, erreurs, warnings);
		}

		evt.handle(warnings);
		return warnings;
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testMineurSansForCelibataire() throws Exception {

		final long noIndividu = 12345657879L;

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, RegDate.get().addYears(-17), "Poucet", "Petit", true);
			}
		});

		// mise en place fiscale (pour la PP)
		final long ppId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) {
				return addHabitant(noIndividu).getNumero();
			}
		});

		// envoi de l'événement dans le handler
		final Individu individu = serviceCivil.getIndividu(noIndividu, 2400);
		final AnnulationArrivee evt = createValideAnnulationArrivee(individu);
		final List<EvenementCivilExterneErreur> warnings = sendEvent(evt);
		Assert.assertEquals(0, warnings.size());

		// vérification que la personne physique n'a toujours pas de for
		final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
		final Set<ForFiscal> fors = pp.getForsFiscaux();
		Assert.assertEquals(0, fors.size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testMineurAvecForAnnuleCelibataire() throws Exception {

		final long noIndividu = 12345657879L;

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, RegDate.get().addYears(-17), "Poucet", "Petit", true);
			}
		});

		// mise en place fiscale (pour la PP)
		final long ppId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, RegDate.get().addYears(-1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				ffp.setAnnule(true);
				return pp.getNumero();
			}
		});

		// envoi de l'événement dans le handler
		final Individu individu = serviceCivil.getIndividu(noIndividu, 2400);
		final AnnulationArrivee evt = createValideAnnulationArrivee(individu);
		final List<EvenementCivilExterneErreur> warnings = sendEvent(evt);
		Assert.assertEquals(0, warnings.size());

		// vérification que la personne physique n'a toujours pas de for
		final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
		final List<ForFiscal> fors = pp.getForsFiscauxNonAnnules(false);
		Assert.assertEquals(0, fors.size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testMineurAvecForNonAnnuleCelibataire() throws Exception {

		final long noIndividu = 12345657879L;

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, RegDate.get().addYears(-17), "Poucet", "Petit", true);
			}
		});

		// mise en place fiscale (pour la PP)
		final long ppId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, RegDate.get().addYears(-1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				return pp.getNumero();
			}
		});

		// envoi de l'événement dans le handler
		final Individu individu = serviceCivil.getIndividu(noIndividu, 2400);
		final AnnulationArrivee evt = createValideAnnulationArrivee(individu);
		try {
			sendEvent(evt);
			Assert.fail("L'événement n'aurait pas dû passer : l'individu est certes mineur, mais il a un for non-annulé!");
		}
		catch (EvenementCivilException e) {
			Assert.assertEquals("Veuillez effectuer cette opération manuellement", e.getMessage());
		}

		// vérification que la personne physique n'a toujours que le for initial
		final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
		final List<ForFiscal> fors = pp.getForsFiscauxNonAnnules(false);
		Assert.assertEquals(1, fors.size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testMineurSansForMarieSeul() throws Exception {

		final long noIndividu = 12345657879L;
		final RegDate dateMariage = RegDate.get().addMonths(-1);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, RegDate.get().addYears(-17), "Poucet", "Petit", true);
				marieIndividu(ind, dateMariage);
			}
		});

		// mise en place fiscale (pour la PP)
		final long mcId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, dateMariage, null);
				return couple.getMenage().getNumero();
			}
		});

		// envoi de l'événement dans le handler
		final Individu individu = serviceCivil.getIndividu(noIndividu, 2400);
		final AnnulationArrivee evt = createValideAnnulationArrivee(individu);
		final List<EvenementCivilExterneErreur> warnings = sendEvent(evt);
		Assert.assertEquals(0, warnings.size());

		// vérification que le ménage n'a toujours pas de for
		final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
		final Set<ForFiscal> forsMc = mc.getForsFiscaux();
		Assert.assertEquals(0, forsMc.size());

		// vérification sur la personne physique : pas de for non-plus
		final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(mc, RegDate.get());
		Assert.assertNotNull(couple);
		Assert.assertNull(couple.getConjoint());
		final PersonnePhysique pp = couple.getPrincipal();
		final Set<ForFiscal> forsPp = pp.getForsFiscaux();
		Assert.assertEquals(0, forsPp.size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testMineurAvecForAnnuleMarieSeul() throws Exception {

		final long noIndividu = 12345657879L;
		final RegDate dateMariage = RegDate.get().addMonths(-1);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, RegDate.get().addYears(-17), "Poucet", "Petit", true);
				marieIndividu(ind, dateMariage);
			}
		});

		// mise en place fiscale (pour la PP)
		final long mcId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, dateMariage, null);
				final MenageCommun mc = couple.getMenage();
				final ForFiscalPrincipal ffp = addForPrincipal(mc, dateMariage, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				ffp.setAnnule(true);
				return mc.getNumero();
			}
		});

		// envoi de l'événement dans le handler
		final Individu individu = serviceCivil.getIndividu(noIndividu, 2400);
		final AnnulationArrivee evt = createValideAnnulationArrivee(individu);
		final List<EvenementCivilExterneErreur> warnings = sendEvent(evt);
		Assert.assertEquals(0, warnings.size());

		// vérification que le ménage n'a toujours pas de for
		final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
		final List<ForFiscal> forsMc = mc.getForsFiscauxNonAnnules(false);
		Assert.assertEquals(0, forsMc.size());

		// vérification sur la personne physique : pas de for non-plus
		final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(mc, RegDate.get());
		Assert.assertNotNull(couple);
		Assert.assertNull(couple.getConjoint());
		final PersonnePhysique pp = couple.getPrincipal();
		final Set<ForFiscal> forsPp = pp.getForsFiscaux();
		Assert.assertEquals(0, forsPp.size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testMineurAvecForNonAnnuleMarieSeul() throws Exception {

		final long noIndividu = 12345657879L;
		final RegDate dateMariage = RegDate.get().addMonths(-1);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, RegDate.get().addYears(-17), "Poucet", "Petit", true);
				marieIndividu(ind, dateMariage);
			}
		});

		// mise en place fiscale (pour la PP)
		final long mcId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, dateMariage, null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, dateMariage, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				return mc.getNumero();
			}
		});

		// envoi de l'événement dans le handler
		final Individu individu = serviceCivil.getIndividu(noIndividu, 2400);
		final AnnulationArrivee evt = createValideAnnulationArrivee(individu);
		try {
			sendEvent(evt);
			Assert.fail("L'événement n'aurait pas dû passer : l'individu est certes mineur, mais il a un for non-annulé!");
		}
		catch (EvenementCivilException e) {
			Assert.assertEquals("Veuillez effectuer cette opération manuellement", e.getMessage());
		}

		// vérification que le ménage n'a toujours pas de for
		final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
		final List<ForFiscal> forsMc = mc.getForsFiscauxNonAnnules(false);
		Assert.assertEquals(1, forsMc.size());

		// vérification sur la personne physique : pas de for non-plus
		final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(mc, RegDate.get());
		Assert.assertNotNull(couple);
		Assert.assertNull(couple.getConjoint());
		final PersonnePhysique pp = couple.getPrincipal();
		final Set<ForFiscal> forsPp = pp.getForsFiscaux();
		Assert.assertEquals(0, forsPp.size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testMineurSansForMarieAvecMineur() throws Exception {

		final long noIndividu = 12345657879L;
		final long noIndividuAutre = 12345657878L;
		final RegDate dateMariage = RegDate.get().addMonths(-1);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu m = addIndividu(noIndividu, RegDate.get().addYears(-17), "Poucet", "Petit", true);
				final MockIndividu mme = addIndividu(noIndividuAutre, RegDate.get().addYears(-17), "Poucette", "Petite", false);
				marieIndividus(m, mme, dateMariage);
			}
		});

		// mise en place fiscale (pour la PP)
		final long mcId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) {
				final PersonnePhysique m = addHabitant(noIndividu);
				final PersonnePhysique mme = addHabitant(noIndividuAutre);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(m, mme, dateMariage, null);
				return couple.getMenage().getNumero();
			}
		});

		// envoi de l'événement dans le handler
		final Individu individu = serviceCivil.getIndividu(noIndividu, 2400);
		final AnnulationArrivee evt = createValideAnnulationArrivee(individu);
		final List<EvenementCivilExterneErreur> warnings = sendEvent(evt);
		Assert.assertEquals(0, warnings.size());

		// vérification que le ménage n'a toujours pas de for
		final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
		final Set<ForFiscal> forsMc = mc.getForsFiscaux();
		Assert.assertEquals(0, forsMc.size());

		// vérification sur la personne physique : pas de for non-plus
		final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(mc, RegDate.get());
		Assert.assertNotNull(couple);
		Assert.assertNotNull(couple.getPrincipal());
		Assert.assertNotNull(couple.getConjoint());
		final PersonnePhysique m = couple.getPrincipal();
		final Set<ForFiscal> forsM = m.getForsFiscaux();
		Assert.assertEquals(0, forsM.size());
		final PersonnePhysique mme = couple.getPrincipal();
		final Set<ForFiscal> forsMme = mme.getForsFiscaux();
		Assert.assertEquals(0, forsMme.size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testMineurAvecForAnnuleMarieAvecMineur() throws Exception {

		final long noIndividu = 12345657879L;
		final long noIndividuAutre = 12345657878L;
		final RegDate dateMariage = RegDate.get().addMonths(-1);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu m = addIndividu(noIndividu, RegDate.get().addYears(-17), "Poucet", "Petit", true);
				final MockIndividu mme = addIndividu(noIndividuAutre, RegDate.get().addYears(-17), "Poucette", "Petite", false);
				marieIndividus(m, mme, dateMariage);
			}
		});

		// mise en place fiscale (pour la PP)
		final long mcId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) {
				final PersonnePhysique m = addHabitant(noIndividu);
				final PersonnePhysique mme = addHabitant(noIndividuAutre);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(m, mme, dateMariage, null);
				final MenageCommun mc = couple.getMenage();
				final ForFiscalPrincipal ffp = addForPrincipal(mc, dateMariage, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				ffp.setAnnule(true);
				return mc.getNumero();
			}
		});

		// envoi de l'événement dans le handler
		final Individu individu = serviceCivil.getIndividu(noIndividu, 2400);
		final AnnulationArrivee evt = createValideAnnulationArrivee(individu);
		final List<EvenementCivilExterneErreur> warnings = sendEvent(evt);
		Assert.assertEquals(0, warnings.size());

		// vérification que le ménage n'a toujours pas de for
		final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
		final List<ForFiscal> forsMc = mc.getForsFiscauxNonAnnules(false);
		Assert.assertEquals(0, forsMc.size());

		// vérification sur la personne physique : pas de for non-plus
		final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(mc, RegDate.get());
		Assert.assertNotNull(couple);
		Assert.assertNotNull(couple.getPrincipal());
		Assert.assertNotNull(couple.getConjoint());
		final PersonnePhysique m = couple.getPrincipal();
		final Set<ForFiscal> forsM = m.getForsFiscaux();
		Assert.assertEquals(0, forsM.size());
		final PersonnePhysique mme = couple.getPrincipal();
		final Set<ForFiscal> forsMme = mme.getForsFiscaux();
		Assert.assertEquals(0, forsMme.size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testMineurAvecForNonAnnuleMarieAvecMineur() throws Exception {

		final long noIndividu = 12345657879L;
		final long noIndividuAutre = 12345657878L;
		final RegDate dateMariage = RegDate.get().addMonths(-1);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu m = addIndividu(noIndividu, RegDate.get().addYears(-17), "Poucet", "Petit", true);
				final MockIndividu mme = addIndividu(noIndividuAutre, RegDate.get().addYears(-17), "Poucette", "Petite", false);
				marieIndividus(m, mme, dateMariage);
			}
		});

		// mise en place fiscale (pour la PP)
		final long mcId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) {
				final PersonnePhysique m = addHabitant(noIndividu);
				final PersonnePhysique mme = addHabitant(noIndividuAutre);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(m, mme, dateMariage, null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, dateMariage, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				return mc.getNumero();
			}
		});

		// envoi de l'événement dans le handler
		final Individu individu = serviceCivil.getIndividu(noIndividu, 2400);
		final AnnulationArrivee evt = createValideAnnulationArrivee(individu);
		try {
			sendEvent(evt);
			Assert.fail("L'événement n'aurait pas dû passer : l'individu est certes mineur (et son conjoint aussi), mais il a un for non-annulé sur le ménage!");
		}
		catch (EvenementCivilException e) {
			Assert.assertEquals("Veuillez effectuer cette opération manuellement", e.getMessage());
		}

		// vérification que le ménage a toujours le même for
		final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
		final List<ForFiscal> forsMc = mc.getForsFiscauxNonAnnules(false);
		Assert.assertEquals(1, forsMc.size());

		// vérification sur la personne physique : pas de for
		final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(mc, RegDate.get());
		Assert.assertNotNull(couple);
		Assert.assertNotNull(couple.getPrincipal());
		Assert.assertNotNull(couple.getConjoint());
		final PersonnePhysique m = couple.getPrincipal();
		final Set<ForFiscal> forsM = m.getForsFiscaux();
		Assert.assertEquals(0, forsM.size());
		final PersonnePhysique mme = couple.getPrincipal();
		final Set<ForFiscal> forsMme = mme.getForsFiscaux();
		Assert.assertEquals(0, forsMme.size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testMineurSansForMarieAvecMajeur() throws Exception {

		final long noIndividu = 12345657879L;
		final long noIndividuAutre = 12345657878L;
		final RegDate dateMariage = RegDate.get().addMonths(-1);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu m = addIndividu(noIndividu, RegDate.get().addYears(-17), "Poucet", "Petit", true);
				final MockIndividu mme = addIndividu(noIndividuAutre, RegDate.get().addYears(-19), "Poucette", "Grande", false);
				marieIndividus(m, mme, dateMariage);
			}
		});

		// mise en place fiscale (pour la PP)
		final long mcId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) {
				final PersonnePhysique m = addHabitant(noIndividu);
				final PersonnePhysique mme = addHabitant(noIndividuAutre);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(m, mme, dateMariage, null);
				return couple.getMenage().getNumero();
			}
		});

		// envoi de l'événement dans le handler
		final Individu individu = serviceCivil.getIndividu(noIndividu, 2400);
		final AnnulationArrivee evt = createValideAnnulationArrivee(individu);
		try {
			sendEvent(evt);
			Assert.fail("L'événement n'aurait pas dû passer : l'individu est certes mineur, mais son conjoint ne l'est pas!");
		}
		catch (EvenementCivilException e) {
			Assert.assertEquals("Veuillez effectuer cette opération manuellement", e.getMessage());
		}

		// vérification que le ménage n'a toujours pas de for
		final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
		final Set<ForFiscal> forsMc = mc.getForsFiscaux();
		Assert.assertEquals(0, forsMc.size());

		// vérification sur la personne physique : pas de for non-plus
		final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(mc, RegDate.get());
		Assert.assertNotNull(couple);
		Assert.assertNotNull(couple.getPrincipal());
		Assert.assertNotNull(couple.getConjoint());
		final PersonnePhysique m = couple.getPrincipal();
		final Set<ForFiscal> forsM = m.getForsFiscaux();
		Assert.assertEquals(0, forsM.size());
		final PersonnePhysique mme = couple.getPrincipal();
		final Set<ForFiscal> forsMme = mme.getForsFiscaux();
		Assert.assertEquals(0, forsMme.size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testMineurAvecForAnnuleMarieAvecMajeur() throws Exception {

		final long noIndividu = 12345657879L;
		final long noIndividuAutre = 12345657878L;
		final RegDate dateMariage = RegDate.get().addMonths(-1);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu m = addIndividu(noIndividu, RegDate.get().addYears(-17), "Poucet", "Petit", true);
				final MockIndividu mme = addIndividu(noIndividuAutre, RegDate.get().addYears(-19), "Poucette", "Grande", false);
				marieIndividus(m, mme, dateMariage);
			}
		});

		// mise en place fiscale (pour la PP)
		final long mcId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) {
				final PersonnePhysique m = addHabitant(noIndividu);
				final PersonnePhysique mme = addHabitant(noIndividuAutre);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(m, mme, dateMariage, null);
				final MenageCommun mc = couple.getMenage();
				final ForFiscalPrincipal ffp = addForPrincipal(mc, dateMariage, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				ffp.setAnnule(true);
				return mc.getNumero();
			}
		});

		// envoi de l'événement dans le handler
		final Individu individu = serviceCivil.getIndividu(noIndividu, 2400);
		final AnnulationArrivee evt = createValideAnnulationArrivee(individu);
		try {
			sendEvent(evt);
			Assert.fail("L'événement n'aurait pas dû passer : l'individu est certes mineur, mais son conjoint ne l'est pas!");
		}
		catch (EvenementCivilException e) {
			Assert.assertEquals("Veuillez effectuer cette opération manuellement", e.getMessage());
		}

		// vérification que le ménage n'a toujours pas de for
		final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
		final List<ForFiscal> forsMc = mc.getForsFiscauxNonAnnules(false);
		Assert.assertEquals(0, forsMc.size());

		// vérification sur la personne physique : pas de for non-plus
		final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(mc, RegDate.get());
		Assert.assertNotNull(couple);
		Assert.assertNotNull(couple.getPrincipal());
		Assert.assertNotNull(couple.getConjoint());
		final PersonnePhysique m = couple.getPrincipal();
		final Set<ForFiscal> forsM = m.getForsFiscaux();
		Assert.assertEquals(0, forsM.size());
		final PersonnePhysique mme = couple.getPrincipal();
		final Set<ForFiscal> forsMme = mme.getForsFiscaux();
		Assert.assertEquals(0, forsMme.size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testMineurAvecForNonAnnuleMarieAvecMajeur() throws Exception {

		final long noIndividu = 12345657879L;
		final long noIndividuAutre = 12345657878L;
		final RegDate dateMariage = RegDate.get().addMonths(-1);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu m = addIndividu(noIndividu, RegDate.get().addYears(-17), "Poucet", "Petit", true);
				final MockIndividu mme = addIndividu(noIndividuAutre, RegDate.get().addYears(-19), "Poucette", "Grande", false);
				marieIndividus(m, mme, dateMariage);
			}
		});

		// mise en place fiscale (pour la PP)
		final long mcId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) {
				final PersonnePhysique m = addHabitant(noIndividu);
				final PersonnePhysique mme = addHabitant(noIndividuAutre);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(m, mme, dateMariage, null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, dateMariage, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				return mc.getNumero();
			}
		});

		// envoi de l'événement dans le handler
		final Individu individu = serviceCivil.getIndividu(noIndividu, 2400);
		final AnnulationArrivee evt = createValideAnnulationArrivee(individu);
		try {
			sendEvent(evt);
			Assert.fail("L'événement n'aurait pas dû passer : l'individu est certes mineur, mais son conjoint ne l'est pas!");
		}
		catch (EvenementCivilException e) {
			Assert.assertEquals("Veuillez effectuer cette opération manuellement", e.getMessage());
		}

		// vérification que le ménage a toujours le même for
		final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
		final List<ForFiscal> forsMc = mc.getForsFiscauxNonAnnules(false);
		Assert.assertEquals(1, forsMc.size());

		// vérification sur la personne physique : pas de for
		final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(mc, RegDate.get());
		Assert.assertNotNull(couple);
		Assert.assertNotNull(couple.getPrincipal());
		Assert.assertNotNull(couple.getConjoint());
		final PersonnePhysique m = couple.getPrincipal();
		final Set<ForFiscal> forsM = m.getForsFiscaux();
		Assert.assertEquals(0, forsM.size());
		final PersonnePhysique mme = couple.getPrincipal();
		final Set<ForFiscal> forsMme = mme.getForsFiscaux();
		Assert.assertEquals(0, forsMme.size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testMajeurSansForCelibataire() throws Exception {

		final long noIndividu = 12345657879L;

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, RegDate.get().addYears(-19), "Poucet", "Grand", true);
			}
		});

		// mise en place fiscale (pour la PP)
		final long ppId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) {
				return addHabitant(noIndividu).getNumero();
			}
		});

		final Individu individu = serviceCivil.getIndividu(noIndividu, 2400);
		final AnnulationArrivee evt = createValideAnnulationArrivee(individu);
		try {
			sendEvent(evt);
			Assert.fail("L'événement n'aurait pas dû passer : l'individu est majeur!");
		}
		catch (EvenementCivilException e) {
			Assert.assertEquals("Veuillez effectuer cette opération manuellement", e.getMessage());
		}

		// vérification que la personne physique n'a toujours pas de for
		final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
		final Set<ForFiscal> fors = pp.getForsFiscaux();
		Assert.assertEquals(0, fors.size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testMajeurAvecForAnnuleCelibataire() throws Exception {

		final long noIndividu = 12345657879L;

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, RegDate.get().addYears(-19), "Poucet", "Grand", true);
			}
		});

		// mise en place fiscale (pour la PP)
		final long ppId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, RegDate.get().addYears(-1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				ffp.setAnnule(true);
				return pp.getNumero();
			}
		});

		final Individu individu = serviceCivil.getIndividu(noIndividu, 2400);
		final AnnulationArrivee evt = createValideAnnulationArrivee(individu);
		try {
			sendEvent(evt);
			Assert.fail("L'événement n'aurait pas dû passer : l'individu est majeur!");
		}
		catch (EvenementCivilException e) {
			Assert.assertEquals("Veuillez effectuer cette opération manuellement", e.getMessage());
		}

		// vérification que la personne physique n'a toujours pas de for
		final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
		final List<ForFiscal> fors = pp.getForsFiscauxNonAnnules(false);
		Assert.assertEquals(0, fors.size());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testMajeurAvecForNonAnnuleCelibataire() throws Exception {

		final long noIndividu = 12345657879L;

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, RegDate.get().addYears(-19), "Poucet", "Grand", true);
			}
		});

		// mise en place fiscale (pour la PP)
		final long ppId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, RegDate.get().addYears(-1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				return pp.getNumero();
			}
		});

		final Individu individu = serviceCivil.getIndividu(noIndividu, 2400);
		final AnnulationArrivee evt = createValideAnnulationArrivee(individu);
		try {
			sendEvent(evt);
			Assert.fail("L'événement n'aurait pas dû passer : l'individu est majeur avec en plus un for non-annulé!");
		}
		catch (EvenementCivilException e) {
			Assert.assertEquals("Veuillez effectuer cette opération manuellement", e.getMessage());
		}

		// vérification que la personne physique n'a toujours pas de for
		final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
		final List<ForFiscal> fors = pp.getForsFiscauxNonAnnules(false);
		Assert.assertEquals(1, fors.size());
	}
}
