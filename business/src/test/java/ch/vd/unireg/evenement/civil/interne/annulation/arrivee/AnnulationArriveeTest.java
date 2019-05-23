package ch.vd.unireg.evenement.civil.interne.annulation.arrivee;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.unireg.evenement.civil.interne.MessageCollector;
import ch.vd.unireg.evenement.common.EvenementErreur;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockIndividuConnector;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAutoriteFiscale;

public class AnnulationArriveeTest extends AbstractEvenementCivilInterneTest {

	private AnnulationArrivee createValideAnnulationArrivee(Individu individu) {
		return new AnnulationArrivee(individu, null, RegDate.get(), MockCommune.Lausanne.getNoOFS(), context);
	}

	private static enum ErrorLocation {
		CHECK_COMPLETENESS,
		VALIDATE
	}

	private static class ErrorFoundException extends Exception {
		public final ErrorLocation location;
		public final List<? extends EvenementErreur> erreurs;
		public final List<? extends EvenementErreur> warnings;

		private ErrorFoundException(ErrorLocation location, List<? extends EvenementErreur> erreurs, List<? extends EvenementErreur> warnings) {
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
	private List<? extends EvenementErreur> sendEvent(AnnulationArrivee evt) throws ErrorFoundException, EvenementCivilException {

		final MessageCollector collector = buildMessageCollector();
		evt.validate(collector, collector);
		if (collector.hasErreurs()) {
			throw new ErrorFoundException(ErrorLocation.VALIDATE, collector.getErreurs(), collector.getWarnings());
		}

		evt.handle(collector);
		return collector.getWarnings();
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testMineurSansForCelibataire() throws Exception {

		final long noIndividu = 12345657879L;

		// mise en place civile
		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				addIndividu(noIndividu, RegDate.get().addYears(-17), "Poucet", "Petit", true);
			}
		});

		// mise en place fiscale (pour la PP)
		final long ppId = doInNewTransactionAndSession(status -> addHabitant(noIndividu).getNumero());

		// envoi de l'événement dans le handler
		final Individu individu = serviceCivil.getIndividu(noIndividu, null);
		final AnnulationArrivee evt = createValideAnnulationArrivee(individu);
		final List<? extends EvenementErreur> warnings = sendEvent(evt);
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
		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				addIndividu(noIndividu, RegDate.get().addYears(-17), "Poucet", "Petit", true);
			}
		});

		// mise en place fiscale (pour la PP)
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			final ForFiscalPrincipal ffp = addForPrincipal(pp, RegDate.get().addYears(-1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			ffp.setAnnule(true);
			return pp.getNumero();
		});

		// envoi de l'événement dans le handler
		final Individu individu = serviceCivil.getIndividu(noIndividu, null);
		final AnnulationArrivee evt = createValideAnnulationArrivee(individu);
		final List<? extends EvenementErreur> warnings = sendEvent(evt);
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
		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				addIndividu(noIndividu, RegDate.get().addYears(-17), "Poucet", "Petit", true);
			}
		});

		// mise en place fiscale (pour la PP)
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, RegDate.get().addYears(-1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			return pp.getNumero();
		});

		// envoi de l'événement dans le handler
		final Individu individu = serviceCivil.getIndividu(noIndividu, null);
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
		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, RegDate.get().addYears(-17), "Poucet", "Petit", true);
				marieIndividu(ind, dateMariage);
			}
		});

		// mise en place fiscale (pour la PP)
		final long mcId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, dateMariage, null);
			return couple.getMenage().getNumero();
		});

		// envoi de l'événement dans le handler
		final Individu individu = serviceCivil.getIndividu(noIndividu, null);
		final AnnulationArrivee evt = createValideAnnulationArrivee(individu);
		final List<? extends EvenementErreur> warnings = sendEvent(evt);
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
		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, RegDate.get().addYears(-17), "Poucet", "Petit", true);
				marieIndividu(ind, dateMariage);
			}
		});

		// mise en place fiscale (pour la PP)
		final long mcId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, dateMariage, null);
			final MenageCommun mc = couple.getMenage();
			final ForFiscalPrincipal ffp = addForPrincipal(mc, dateMariage, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			ffp.setAnnule(true);
			return mc.getNumero();
		});

		// envoi de l'événement dans le handler
		final Individu individu = serviceCivil.getIndividu(noIndividu, null);
		final AnnulationArrivee evt = createValideAnnulationArrivee(individu);
		final List<? extends EvenementErreur> warnings = sendEvent(evt);
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
		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, RegDate.get().addYears(-17), "Poucet", "Petit", true);
				marieIndividu(ind, dateMariage);
			}
		});

		// mise en place fiscale (pour la PP)
		final long mcId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, dateMariage, null);
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, dateMariage, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			return mc.getNumero();
		});

		// envoi de l'événement dans le handler
		final Individu individu = serviceCivil.getIndividu(noIndividu, null);
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
		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu m = addIndividu(noIndividu, RegDate.get().addYears(-17), "Poucet", "Petit", true);
				final MockIndividu mme = addIndividu(noIndividuAutre, RegDate.get().addYears(-17), "Poucette", "Petite", false);
				marieIndividus(m, mme, dateMariage);
			}
		});

		// mise en place fiscale (pour la PP)
		final long mcId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique m = addHabitant(noIndividu);
			final PersonnePhysique mme = addHabitant(noIndividuAutre);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(m, mme, dateMariage, null);
			return couple.getMenage().getNumero();
		});

		// envoi de l'événement dans le handler
		final Individu individu = serviceCivil.getIndividu(noIndividu, null);
		final AnnulationArrivee evt = createValideAnnulationArrivee(individu);
		final List<? extends EvenementErreur> warnings = sendEvent(evt);
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
		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu m = addIndividu(noIndividu, RegDate.get().addYears(-17), "Poucet", "Petit", true);
				final MockIndividu mme = addIndividu(noIndividuAutre, RegDate.get().addYears(-17), "Poucette", "Petite", false);
				marieIndividus(m, mme, dateMariage);
			}
		});

		// mise en place fiscale (pour la PP)
		final long mcId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique m = addHabitant(noIndividu);
			final PersonnePhysique mme = addHabitant(noIndividuAutre);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(m, mme, dateMariage, null);
			final MenageCommun mc = couple.getMenage();
			final ForFiscalPrincipal ffp = addForPrincipal(mc, dateMariage, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			ffp.setAnnule(true);
			return mc.getNumero();
		});

		// envoi de l'événement dans le handler
		final Individu individu = serviceCivil.getIndividu(noIndividu, null);
		final AnnulationArrivee evt = createValideAnnulationArrivee(individu);
		final List<? extends EvenementErreur> warnings = sendEvent(evt);
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
		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu m = addIndividu(noIndividu, RegDate.get().addYears(-17), "Poucet", "Petit", true);
				final MockIndividu mme = addIndividu(noIndividuAutre, RegDate.get().addYears(-17), "Poucette", "Petite", false);
				marieIndividus(m, mme, dateMariage);
			}
		});

		// mise en place fiscale (pour la PP)
		final long mcId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique m = addHabitant(noIndividu);
			final PersonnePhysique mme = addHabitant(noIndividuAutre);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(m, mme, dateMariage, null);
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, dateMariage, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			return mc.getNumero();
		});

		// envoi de l'événement dans le handler
		final Individu individu = serviceCivil.getIndividu(noIndividu, null);
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
		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu m = addIndividu(noIndividu, RegDate.get().addYears(-17), "Poucet", "Petit", true);
				final MockIndividu mme = addIndividu(noIndividuAutre, RegDate.get().addYears(-19), "Poucette", "Grande", false);
				marieIndividus(m, mme, dateMariage);
			}
		});

		// mise en place fiscale (pour la PP)
		final long mcId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique m = addHabitant(noIndividu);
			final PersonnePhysique mme = addHabitant(noIndividuAutre);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(m, mme, dateMariage, null);
			return couple.getMenage().getNumero();
		});

		// envoi de l'événement dans le handler
		final Individu individu = serviceCivil.getIndividu(noIndividu, null);
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
		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu m = addIndividu(noIndividu, RegDate.get().addYears(-17), "Poucet", "Petit", true);
				final MockIndividu mme = addIndividu(noIndividuAutre, RegDate.get().addYears(-19), "Poucette", "Grande", false);
				marieIndividus(m, mme, dateMariage);
			}
		});

		// mise en place fiscale (pour la PP)
		final long mcId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique m = addHabitant(noIndividu);
			final PersonnePhysique mme = addHabitant(noIndividuAutre);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(m, mme, dateMariage, null);
			final MenageCommun mc = couple.getMenage();
			final ForFiscalPrincipal ffp = addForPrincipal(mc, dateMariage, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			ffp.setAnnule(true);
			return mc.getNumero();
		});

		// envoi de l'événement dans le handler
		final Individu individu = serviceCivil.getIndividu(noIndividu, null);
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
		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu m = addIndividu(noIndividu, RegDate.get().addYears(-17), "Poucet", "Petit", true);
				final MockIndividu mme = addIndividu(noIndividuAutre, RegDate.get().addYears(-19), "Poucette", "Grande", false);
				marieIndividus(m, mme, dateMariage);
			}
		});

		// mise en place fiscale (pour la PP)
		final long mcId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique m = addHabitant(noIndividu);
			final PersonnePhysique mme = addHabitant(noIndividuAutre);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(m, mme, dateMariage, null);
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, dateMariage, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			return mc.getNumero();
		});

		// envoi de l'événement dans le handler
		final Individu individu = serviceCivil.getIndividu(noIndividu, null);
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
		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				addIndividu(noIndividu, RegDate.get().addYears(-19), "Poucet", "Grand", true);
			}
		});

		// mise en place fiscale (pour la PP)
		final long ppId = doInNewTransactionAndSession(status -> addHabitant(noIndividu).getNumero());

		final Individu individu = serviceCivil.getIndividu(noIndividu, null);
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
		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				addIndividu(noIndividu, RegDate.get().addYears(-19), "Poucet", "Grand", true);
			}
		});

		// mise en place fiscale (pour la PP)
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			final ForFiscalPrincipal ffp = addForPrincipal(pp, RegDate.get().addYears(-1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			ffp.setAnnule(true);
			return pp.getNumero();
		});

		final Individu individu = serviceCivil.getIndividu(noIndividu, null);
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
	public void testMajeurAvecDecisionAci() throws Exception {

		final long noIndividu = 12345657879L;

		// mise en place civile
		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				addIndividu(noIndividu, RegDate.get().addYears(-19), "Poucet", "Grand", true);
			}
		});

		// mise en place fiscale (pour la PP)
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			final ForFiscalPrincipal ffp = addForPrincipal(pp, RegDate.get().addYears(-1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			addDecisionAci(pp, date(RegDate.get().year() - 1, 1, 1), null, MockCommune.Aigle.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null);
			return pp.getNumero();
		});

		final Individu individu = serviceCivil.getIndividu(noIndividu, null);
		final AnnulationArrivee evt = createValideAnnulationArrivee(individu);
		try {
			sendEvent(evt);
			Assert.fail("L'événement n'aurait pas dû passer : l'individu possède une décision ACI");
		}
		catch (EvenementCivilException e) {
			final String message = String.format("Le contribuable trouvé (%s) est sous l'influence d'une décision ACI", FormatNumeroHelper.numeroCTBToDisplay(ppId));
			Assert.assertEquals(message, e.getMessage());
		}


	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testMajeurAvecForNonAnnuleCelibataire() throws Exception {

		final long noIndividu = 12345657879L;

		// mise en place civile
		serviceCivil.setUp(new DefaultMockIndividuConnector() {
			@Override
			protected void init() {
				addIndividu(noIndividu, RegDate.get().addYears(-19), "Poucet", "Grand", true);
			}
		});

		// mise en place fiscale (pour la PP)
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, RegDate.get().addYears(-1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			return pp.getNumero();
		});

		final Individu individu = serviceCivil.getIndividu(noIndividu, null);
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
