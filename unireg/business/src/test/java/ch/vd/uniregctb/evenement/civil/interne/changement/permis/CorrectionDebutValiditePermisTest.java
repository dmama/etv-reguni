package ch.vd.uniregctb.evenement.civil.interne.changement.permis;

import java.util.Arrays;

import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypePermis;

public class CorrectionDebutValiditePermisTest extends AbstractEvenementCivilInterneTest {

	private CorrectionDebutValiditePermis createEvt(long noIndividu, int ofsCommune, RegDate date, Long principalId) {
		final Individu individu = serviceCivil.getIndividu(noIndividu, 2400, AttributeIndividu.PERMIS);
		return new CorrectionDebutValiditePermis(individu, principalId, null, null, date, ofsCommune, context);
	}

	@Test
	public void testIndividuInconnuAuFiscal() throws Exception {

		final long noIndividu = 12334122L;
		final RegDate dateNaissance = RegDate.get().addYears(-17);
		final RegDate datePermis = RegDate.get().addYears(-1);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Granger", "Hermione", false);
				addNationalite(ind, MockPays.RoyaumeUni, dateNaissance, null, 1);
				addPermis(ind, TypePermis.ETABLISSEMENT, datePermis, null, 1, false);
			}
		});

		// envoi de l'événement civil de correction de début de validité de permis
		final CorrectionDebutValiditePermis evt = createEvt(noIndividu, MockCommune.Lausanne.getNoOFSEtendu(), datePermis, null);
		assertErreurs(evt, Arrays.asList(String.format("Aucun tiers contribuable ne correspond au numero d'individu %d", noIndividu)));
	}

	@Test
	@NotTransactional
	public void testIndividuMineurSansForNiPermis() throws Exception {

		final long noIndividu = 12334122L;
		final RegDate dateNaissance = RegDate.get().addYears(-17);
		final RegDate datePermis = RegDate.get().addYears(-1);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Granger", "Hermione", false);
				addNationalite(ind, MockPays.RoyaumeUni, dateNaissance, null, 1);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				return pp.getNumero();
			}
		});

		// envoi de l'événement civil de correction de début de validité de permis
		final CorrectionDebutValiditePermis evt = createEvt(noIndividu, MockCommune.Lausanne.getNoOFSEtendu(), datePermis, ppId);
		assertSansErreurNiWarning(evt);
	}

	@Test
	@NotTransactional
	public void testIndividuMineurPermisBSansFor() throws Exception {

		final long noIndividu = 12334122L;
		final RegDate dateNaissance = RegDate.get().addYears(-17);
		final RegDate datePermis = RegDate.get().addYears(-1);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Granger", "Hermione", false);
				addNationalite(ind, MockPays.RoyaumeUni, dateNaissance, null, 1);
				addPermis(ind, TypePermis.ANNUEL, datePermis, null, 1, false);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				return pp.getNumero();
			}
		});

		// envoi de l'événement civil de correction de début de validité de permis
		final CorrectionDebutValiditePermis evt = createEvt(noIndividu, MockCommune.Lausanne.getNoOFSEtendu(), datePermis, ppId);
		assertSansErreurNiWarning(evt);
	}

	@Test
	@NotTransactional
	public void testIndividuMineurPermisCSansFor() throws Exception {

		final long noIndividu = 12334122L;
		final RegDate dateNaissance = RegDate.get().addYears(-17);
		final RegDate datePermis = RegDate.get().addYears(-1);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Granger", "Hermione", false);
				addNationalite(ind, MockPays.RoyaumeUni, dateNaissance, null, 1);
				addPermis(ind, TypePermis.ETABLISSEMENT, datePermis, null, 1, false);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				return pp.getNumero();
			}
		});

		// envoi de l'événement civil de correction de début de validité de permis
		final CorrectionDebutValiditePermis evt = createEvt(noIndividu, MockCommune.Lausanne.getNoOFSEtendu(), datePermis, ppId);
		assertSansErreurNiWarning(evt);
	}

	@Test
	@NotTransactional
	public void testIndividuMineurSansPermisAvecFor() throws Exception {

		final long noIndividu = 12334122L;
		final RegDate dateNaissance = RegDate.get().addYears(-17);
		final RegDate datePermis = RegDate.get().addYears(-1);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Granger", "Hermione", false);
				addNationalite(ind, MockPays.RoyaumeUni, dateNaissance, null, 1);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, datePermis.addMonths(2), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				return pp.getNumero();
			}
		});

		// envoi de l'événement civil de correction de début de validité de permis
		final CorrectionDebutValiditePermis evt = createEvt(noIndividu, MockCommune.Lausanne.getNoOFSEtendu(), datePermis, ppId);
		assertSansErreurNiWarning(evt);
	}

	@Test
	@NotTransactional
	public void testIndividuMineurAvecForEtPermisB() throws Exception {

		final long noIndividu = 12334122L;
		final RegDate dateNaissance = RegDate.get().addYears(-17);
		final RegDate datePermis = RegDate.get().addYears(-1);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Granger", "Hermione", false);
				addNationalite(ind, MockPays.RoyaumeUni, dateNaissance, null, 1);
				addPermis(ind, TypePermis.ANNUEL, datePermis, null, 1, false);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, datePermis.addMonths(2), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				return pp.getNumero();
			}
		});

		// envoi de l'événement civil de correction de début de validité de permis
		final CorrectionDebutValiditePermis evt = createEvt(noIndividu, MockCommune.Lausanne.getNoOFSEtendu(), datePermis, ppId);
		assertSansErreurNiWarning(evt);
	}

	@Test
	@NotTransactional
	public void testIndividuMineurAvecForEtPermisC() throws Exception {

		final long noIndividu = 12334122L;
		final RegDate dateNaissance = RegDate.get().addYears(-17);
		final RegDate datePermis = RegDate.get().addYears(-1);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Granger", "Hermione", false);
				addNationalite(ind, MockPays.RoyaumeUni, dateNaissance, null, 1);
				addPermis(ind, TypePermis.ETABLISSEMENT, datePermis, null, 1, false);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, datePermis.addMonths(2), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				return pp.getNumero();
			}
		});

		// envoi de l'événement civil de correction de début de validité de permis
		final CorrectionDebutValiditePermis evt = createEvt(noIndividu, MockCommune.Lausanne.getNoOFSEtendu(), datePermis, ppId);
		assertErreurs(evt, Arrays.asList("Permis C sur individu majeur ou ayant un for fiscal actif : veuillez traiter le cas manuellement."));
	}

	@Test
	@NotTransactional
	public void testIndividuMineurMarieSeulAvecForEtPermisC() throws Exception {

		final long noIndividu = 12334122L;
		final RegDate dateNaissance = RegDate.get().addYears(-17);
		final RegDate dateMariage = RegDate.get().addMonths(-6);
		final RegDate datePermis = RegDate.get().addYears(-1);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Granger", "Hermione", false);
				addNationalite(ind, MockPays.RoyaumeUni, dateNaissance, null, 1);
				addPermis(ind, TypePermis.ETABLISSEMENT, datePermis, null, 1, false);
				marieIndividu(ind, dateMariage);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, dateMariage, null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, dateMariage, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				return pp.getNumero();
			}
		});

		// envoi de l'événement civil de correction de début de validité de permis
		final CorrectionDebutValiditePermis evt = createEvt(noIndividu, MockCommune.Lausanne.getNoOFSEtendu(), datePermis, ppId);
		assertErreurs(evt, Arrays.asList("Permis C sur individu majeur ou ayant un for fiscal actif : veuillez traiter le cas manuellement."));
	}

	@Test
	@NotTransactional
	public void testIndividuMineurMarieAvecAutreMineurAvecForEtPermisC() throws Exception {

		final long noIndividuMme = 12334122L;
		final long noIndividuMr = 12334124L;
		final RegDate dateNaissance = RegDate.get().addYears(-17);
		final RegDate dateMariage = RegDate.get().addMonths(-6);
		final RegDate datePermis = RegDate.get().addYears(-1);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu mme = addIndividu(noIndividuMme, dateNaissance, "Granger", "Hermione", false);
				final MockIndividu mr = addIndividu(noIndividuMr, dateNaissance, "Weasley", "Ronald", true);
				addNationalite(mme, MockPays.RoyaumeUni, dateNaissance, null, 1);
				addNationalite(mr, MockPays.RoyaumeUni, dateNaissance, null, 1);
				addPermis(mme, TypePermis.ETABLISSEMENT, datePermis, null, 1, false);
				marieIndividus(mme, mr, dateMariage);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique mme = addHabitant(noIndividuMme);
				final PersonnePhysique mr = addHabitant(noIndividuMr);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(mme, mr, dateMariage, null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, dateMariage, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				return mme.getNumero();
			}
		});

		// envoi de l'événement civil de correction de début de validité de permis
		final CorrectionDebutValiditePermis evt = createEvt(noIndividuMme, MockCommune.Lausanne.getNoOFSEtendu(), datePermis, ppId);
		assertErreurs(evt, Arrays.asList("Permis C sur individu majeur ou ayant un for fiscal actif : veuillez traiter le cas manuellement."));
	}

	@Test
	@NotTransactional
	public void testIndividuMineurMarieAvecAutreMineurPermisCSansFor() throws Exception {

		final long noIndividuMme = 12334122L;
		final long noIndividuMr = 12334124L;
		final RegDate dateNaissance = RegDate.get().addYears(-17);
		final RegDate dateMariage = RegDate.get().addMonths(-6);
		final RegDate datePermis = RegDate.get().addYears(-1);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu mme = addIndividu(noIndividuMme, dateNaissance, "Granger", "Hermione", false);
				final MockIndividu mr = addIndividu(noIndividuMr, dateNaissance, "Weasley", "Ronald", true);
				addNationalite(mme, MockPays.RoyaumeUni, dateNaissance, null, 1);
				addNationalite(mr, MockPays.RoyaumeUni, dateNaissance, null, 1);
				addPermis(mme, TypePermis.ETABLISSEMENT, datePermis, null, 1, false);
				marieIndividus(mme, mr, dateMariage);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique mme = addHabitant(noIndividuMme);
				final PersonnePhysique mr = addHabitant(noIndividuMr);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(mme, mr, dateMariage, null);
				final MenageCommun mc = couple.getMenage();
				return mme.getNumero();
			}
		});

		// envoi de l'événement civil de correction de début de validité de permis
		final CorrectionDebutValiditePermis evt = createEvt(noIndividuMme, MockCommune.Lausanne.getNoOFSEtendu(), datePermis, ppId);
		assertSansErreurNiWarning(evt);
	}

	@Test
	@NotTransactional
	public void testIndividuMineurMarieMajeurPermisCSansFor() throws Exception {

		final long noIndividuMme = 12334122L;
		final long noIndividuMr = 12334124L;
		final RegDate dateNaissance = RegDate.get().addYears(-17);
		final RegDate dateMariage = RegDate.get().addMonths(-6);
		final RegDate datePermis = RegDate.get().addYears(-1);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final RegDate dateNaissanceMajeur = dateNaissance.addYears(-3);
				final MockIndividu mme = addIndividu(noIndividuMme, dateNaissance, "Granger", "Hermione", false);
				final MockIndividu mr = addIndividu(noIndividuMr, dateNaissanceMajeur, "Weasley", "Ronald", true);
				addNationalite(mme, MockPays.RoyaumeUni, dateNaissance, null, 1);
				addNationalite(mr, MockPays.RoyaumeUni, dateNaissanceMajeur, null, 1);
				addPermis(mme, TypePermis.ETABLISSEMENT, datePermis, null, 1, false);
				marieIndividus(mme, mr, dateMariage);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique mme = addHabitant(noIndividuMme);
				final PersonnePhysique mr = addHabitant(noIndividuMr);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(mme, mr, dateMariage, null);
				final MenageCommun mc = couple.getMenage();
				return mme.getNumero();
			}
		});

		// envoi de l'événement civil de correction de début de validité de permis
		final CorrectionDebutValiditePermis evt = createEvt(noIndividuMme, MockCommune.Lausanne.getNoOFSEtendu(), datePermis, ppId);
		assertErreurs(evt, Arrays.asList("Permis C sur individu majeur ou ayant un for fiscal actif : veuillez traiter le cas manuellement."));
	}

	@Test
	@NotTransactional
	public void testIndividuMajeurSansForNiPermis() throws Exception {

		final long noIndividu = 12334122L;
		final RegDate dateNaissance = RegDate.get().addYears(-20);
		final RegDate datePermis = RegDate.get().addYears(-1);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Granger", "Hermione", false);
				addNationalite(ind, MockPays.RoyaumeUni, dateNaissance, null, 1);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				return pp.getNumero();
			}
		});

		// envoi de l'événement civil de correction de début de validité de permis
		final CorrectionDebutValiditePermis evt = createEvt(noIndividu, MockCommune.Lausanne.getNoOFSEtendu(), datePermis, ppId);
		assertSansErreurNiWarning(evt);
	}

	@Test
	@NotTransactional
	public void testIndividuMajeurPermisBSansFor() throws Exception {

		final long noIndividu = 12334122L;
		final RegDate dateNaissance = RegDate.get().addYears(-20);
		final RegDate datePermis = RegDate.get().addYears(-1);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Granger", "Hermione", false);
				addNationalite(ind, MockPays.RoyaumeUni, dateNaissance, null, 1);
				addPermis(ind, TypePermis.ANNUEL, datePermis, null, 1, false);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				return pp.getNumero();
			}
		});

		// envoi de l'événement civil de correction de début de validité de permis
		final CorrectionDebutValiditePermis evt = createEvt(noIndividu, MockCommune.Lausanne.getNoOFSEtendu(), datePermis, ppId);
		assertSansErreurNiWarning(evt);
	}

	@Test
	@NotTransactional
	public void testIndividuMajeurPermisCSansFor() throws Exception {

		final long noIndividu = 12334122L;
		final RegDate dateNaissance = RegDate.get().addYears(-20);
		final RegDate datePermis = RegDate.get().addYears(-1);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Granger", "Hermione", false);
				addNationalite(ind, MockPays.RoyaumeUni, dateNaissance, null, 1);
				addPermis(ind, TypePermis.ETABLISSEMENT, datePermis, null, 1, false);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				return pp.getNumero();
			}
		});

		// envoi de l'événement civil de correction de début de validité de permis
		final CorrectionDebutValiditePermis evt = createEvt(noIndividu, MockCommune.Lausanne.getNoOFSEtendu(), datePermis, ppId);
		assertErreurs(evt, Arrays.asList("Permis C sur individu majeur ou ayant un for fiscal actif : veuillez traiter le cas manuellement."));
	}
}
