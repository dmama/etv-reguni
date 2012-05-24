package ch.vd.uniregctb.tiers.validator;

import java.util.List;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.security.MockSecurityProvider;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.view.AdresseView;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdresseTiers;

@SuppressWarnings({"JavaDoc"})
public class TiersAdresseValidatorTest extends WebTest {

	private TiersAdresseValidator validator;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		final AdresseService adresseService = getBean(AdresseService.class, "adresseService");
		final AssujettissementService assujettissementService = getBean(AssujettissementService.class, "assujettissementService");

		validator = new TiersAdresseValidator();
		validator.setAdresseService(adresseService);
		validator.setServiceInfra(serviceInfra);
		validator.setTiersService(tiersService);
		validator.setAssujettissementService(assujettissementService);
		if (validator instanceof InitializingBean) {
			((InitializingBean) validator).afterPropertiesSet();
		}
	}

	@Override
	public void onTearDown() throws Exception {
		if (validator instanceof DisposableBean) {
			((DisposableBean) validator).destroy();
		}
		validator = null;

		// reset le security provider pour les autres tests (ceux qui seront lancés alors que le contexte spring n'aura pas encore été ré-initialisé)
		popSecurityProvider();

		super.onTearDown();
	}

	/**
	 * Cas du jira UNIREG-3081
	 */
	@Test
	public void testValidationDroitsAccesRefuse() throws Exception {

		final long noIndOlivia = 25612436L;
		final long noIndFred = 25612437L;
		final RegDate dateMariage = date(2000, 1, 1);

		final Role[] roles = {Role.VISU_ALL,
				Role.CREATE_NONHAB,
				Role.MODIF_HC_HS,
				Role.MODIF_NONHAB_DEBPUR,
				Role.ADR_PP_D,
				Role.ADR_PP_C,
				Role.ADR_PP_B,
				Role.ADR_P,
				Role.ADR_PP_C_DCD};
		pushSecurityProvider(new MockSecurityProvider(roles));

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu olivia = addIndividu(noIndOlivia, date(1985, 8, 12), "Tartempion", "Olivia", false);
				final MockIndividu fred = addIndividu(noIndFred, date(1984, 1, 29), "Tartempion", "Olivier", true);
				addAdresse(olivia, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateMariage, null);
				addAdresse(fred, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateMariage, null);
				marieIndividus(fred, olivia, dateMariage);
			}
		});

		final class Ids {
			final long idOlivia;
			final long idFred;
			final long idMenage;

			Ids(long idOlivia, long idFred, long idMenage) {
				this.idOlivia = idOlivia;
				this.idFred = idFred;
				this.idMenage = idMenage;
			}
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique fred = addHabitant(noIndFred);
				final PersonnePhysique olivia = addHabitant(noIndOlivia);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(fred, olivia, dateMariage, null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Cossonay);
				return new Ids(olivia.getNumero(), fred.getNumero(), mc.getNumero());
			}
		});

		final AdresseView view = new AdresseView();
		view.setDateDebut(date(2010, 1, 1));
		view.setLocaliteSuisse(MockLocalite.Echallens.getNomCompletMinuscule());
		view.setNumeroOrdrePoste(Integer.toString(MockLocalite.Echallens.getNoOrdre()));
		view.setTypeLocalite("suisse");
		view.setNumCTB(ids.idFred);
		view.setUsage(TypeAdresseTiers.COURRIER);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final Errors errors = new BeanPropertyBindingResult(view, "view");
				validator.validate(view, errors);
				Assert.assertNotNull(errors);
				Assert.assertEquals(1, errors.getErrorCount());

				final List<?> errorList = errors.getAllErrors();
				Assert.assertEquals(1, errorList.size());

				final FieldError error = (FieldError) errorList.get(0);
				Assert.assertNotNull(error);
				Assert.assertEquals("usage", error.getField());
				Assert.assertEquals("error.usage.interdit", error.getCode());
				return null;
			}
		});
	}

	/**
	 * Cas du jira UNIREG-3081
	 */
	@Test
	public void testValidationDroitsAccesAccepte() throws Exception {

		final long noIndOlivia = 25612436L;
		final long noIndFred = 25612437L;
		final RegDate dateMariage = date(2000, 1, 1);

		final Role[] roles = {Role.VISU_ALL, Role.MODIF_VD_ORD, Role.ADR_PP_C};
		pushSecurityProvider(new MockSecurityProvider(roles));

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu olivia = addIndividu(noIndOlivia, date(1985, 8, 12), "Tartempion", "Olivia", false);
				final MockIndividu fred = addIndividu(noIndFred, date(1984, 1, 29), "Tartempion", "Olivier", true);
				addAdresse(olivia, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateMariage, null);
				addAdresse(fred, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateMariage, null);
				marieIndividus(fred, olivia, dateMariage);
			}
		});

		final class Ids {
			final long idOlivia;
			final long idFred;
			final long idMenage;

			Ids(long idOlivia, long idFred, long idMenage) {
				this.idOlivia = idOlivia;
				this.idFred = idFred;
				this.idMenage = idMenage;
			}
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique fred = addHabitant(noIndFred);
				final PersonnePhysique olivia = addHabitant(noIndOlivia);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(fred, olivia, dateMariage, null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Cossonay);
				return new Ids(olivia.getNumero(), fred.getNumero(), mc.getNumero());
			}
		});

		final AdresseView view = new AdresseView();
		view.setDateDebut(date(2010, 1, 1));
		view.setLocaliteSuisse(MockLocalite.Echallens.getNomCompletMinuscule());
		view.setNumeroOrdrePoste(Integer.toString(MockLocalite.Echallens.getNoOrdre()));
		view.setTypeLocalite("suisse");
		view.setNumCTB(ids.idFred);
		view.setUsage(TypeAdresseTiers.COURRIER);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final Errors errors = new BeanPropertyBindingResult(view, "view");
				validator.validate(view, errors);
				Assert.assertNotNull(errors);
				Assert.assertEquals(0, errors.getErrorCount());
				return null;
			}
		});
	}
}
