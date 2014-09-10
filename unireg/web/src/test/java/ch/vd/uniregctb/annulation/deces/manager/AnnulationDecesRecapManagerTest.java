package ch.vd.uniregctb.annulation.deces.manager;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.annulation.deces.view.AnnulationDecesRecapView;
import ch.vd.uniregctb.common.BusinessTestingConstants;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;

@ContextConfiguration(locations = BusinessTestingConstants.UNIREG_BUSINESS_UT_TACHES)       // je veux le véritable tache-service !
public class AnnulationDecesRecapManagerTest extends WebTest {

	private AnnulationDecesRecapManager manager;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		manager = getBean(AnnulationDecesRecapManager.class, "annulationDecesRecapManager");
	}

	/**
	 * [SIFISC-13407] Crash pour org.hibernate.StaleObjectStateException à l'annulation d'un décès quand le flag
	 * de "majorité traitée" était différent de FALSE
	 */
	@Test
	public void testAnnulationDecesMajoriteTraiteeFalse() throws Exception {

		final long noIndividu = 347878L;
		final RegDate dateDeces = date(2014, 7, 20);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1935, 11, 4), "Vaisselle", "Jehan", Sexe.MASCULIN);
				addEtatCivil(individu, date(1989, 10, 23), TypeEtatCivil.VEUF);
				individu.setDateDeces(dateDeces);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
				addForPrincipal(pp, date(1976, 1, 7), MotifFor.INDETERMINE, date(1992, 1, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				addForPrincipal(pp, date(1992, 2, 1), MotifFor.DEMENAGEMENT_VD, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Bussigny);
				pp.setMajoriteTraitee(false);
				return pp.getNumero();
			}
		});

		// annulation de décès comme fait dans le contrôleur ad'hoc
		final AnnulationDecesRecapView annulationDecesView = manager.get(ppId);
		manager.save(annulationDecesView);
	}

	/**
	 * [SIFISC-13407] Crash pour org.hibernate.StaleObjectStateException à l'annulation d'un décès quand le flag
	 * de "majorité traitée" était différent de FALSE
	 */
	@Test
	public void testAnnulationDecesMajoriteTraiteeTrue() throws Exception {

		final long noIndividu = 347878L;
		final RegDate dateDeces = date(2014, 7, 20);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1935, 11, 4), "Vaisselle", "Jehan", Sexe.MASCULIN);
				addEtatCivil(individu, date(1989, 10, 23), TypeEtatCivil.VEUF);
				individu.setDateDeces(dateDeces);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
				addForPrincipal(pp, date(1976, 1, 7), MotifFor.INDETERMINE, date(1992, 1, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				addForPrincipal(pp, date(1992, 2, 1), MotifFor.DEMENAGEMENT_VD, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Bussigny);
				pp.setMajoriteTraitee(true);
				return pp.getNumero();
			}
		});

		// annulation de décès comme fait dans le contrôleur ad'hoc
		final AnnulationDecesRecapView annulationDecesView = manager.get(ppId);
		manager.save(annulationDecesView);
	}

	/**
	 * [SIFISC-13407] Crash pour org.hibernate.StaleObjectStateException à l'annulation d'un décès quand le flag
	 * de "majorité traitée" était différent de FALSE
	 */
	@Test
	public void testAnnulationDecesMajoriteTraiteeNull() throws Exception {

		final long noIndividu = 347878L;
		final RegDate dateDeces = date(2014, 7, 20);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1935, 11, 4), "Vaisselle", "Jehan", Sexe.MASCULIN);
				addEtatCivil(individu, date(1989, 10, 23), TypeEtatCivil.VEUF);
				individu.setDateDeces(dateDeces);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
				addForPrincipal(pp, date(1976, 1, 7), MotifFor.INDETERMINE, date(1992, 1, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				addForPrincipal(pp, date(1992, 2, 1), MotifFor.DEMENAGEMENT_VD, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Bussigny);
				pp.setMajoriteTraitee(null);
				return pp.getNumero();
			}
		});

		// annulation de décès comme fait dans le contrôleur ad'hoc
		final AnnulationDecesRecapView annulationDecesView = manager.get(ppId);
		manager.save(annulationDecesView);
	}
}
