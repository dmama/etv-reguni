package ch.vd.uniregctb.evenement.party;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.numbers.v1.NumbersRequest;
import ch.vd.unireg.xml.event.party.numbers.v1.NumbersResponse;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.party.v1.PartyType;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.security.MockSecurityProvider;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.xml.ServiceException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NumbersRequestHandlerTest extends BusinessTest {

	private NumbersRequestHandler handler;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		handler = new NumbersRequestHandler();
		handler.setTiersDAO(tiersDAO);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleUtilisateurSansDroit() throws Exception {

		pushSecurityProvider(new MockSecurityProvider());
		try {

			final NumbersRequest request = new NumbersRequest();
			final UserLogin login = new UserLogin("xxxxx", 22);
			request.setLogin(login);

			try {
				handler.handle(request);
				fail();
			}
			catch (ServiceException e) {
				assertTrue(e.getInfo() instanceof AccessDeniedExceptionInfo);
				assertEquals("L'utilisateur spécifié (xxxxx/22) n'a pas les droits d'accès en lecture complète sur l'application.", e.getMessage());
			}

		}
		finally {
			popSecurityProvider();
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleRequeteOK() throws Exception {

		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);

		class Ids {
			long pp0;
			long pp1;
			long menage0;
			long menage1;
			long etablissement0;
			long etablissement1;
			long entreprise0;
			long entreprise1;
			long debiteur0;
			long debiteur1;
			long colladm0;
			long colladm1;
			long autre0;
			long autre1;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique pp0 = addNonHabitant("Marie", "Gooert", date(1965, 3, 22), Sexe.FEMININ);
				PersonnePhysique pp1 = addNonHabitant("Georgette", "van Zum", date(1934, 4, 19), Sexe.FEMININ);
				pp1.setAnnule(true);

				MenageCommun menage0 = addMenageCommun(null);
				MenageCommun menage1 = addMenageCommun(null);
				menage1.setAnnule(true);

				Etablissement etablissement0 = addEtablissement(null);
				Etablissement etablissement1 = addEtablissement(null);
				etablissement1.setAnnule(true);

				Entreprise entreprise0 = addEntreprise(423L);
				Entreprise entreprise1 = addEntreprise(424L);
				entreprise1.setAnnule(true);

				DebiteurPrestationImposable debiteur0 = addDebiteur();
				DebiteurPrestationImposable debiteur1 = addDebiteur();
				debiteur1.setAnnule(true);

				CollectiviteAdministrative colladm0 = addCollAdm(0);
				CollectiviteAdministrative colladm1 = addCollAdm(1);
				colladm1.setAnnule(true);

				AutreCommunaute autre0 = addAutreCommunaute("dollar est ton dieu");
				AutreCommunaute autre1 = addAutreCommunaute("en voilà un qui voit");
				autre1.setAnnule(true);

				ids.pp0 = pp0.getNumero();
				ids.pp1 = pp1.getNumero();
				ids.menage0 = menage0.getNumero();
				ids.menage1 = menage1.getNumero();
				ids.etablissement0 = etablissement0.getNumero();
				ids.etablissement1 = etablissement0.getNumero();
				ids.entreprise0 = entreprise0.getNumero();
				ids.entreprise1 = entreprise1.getNumero();
				ids.debiteur0 = debiteur0.getNumero();
				ids.debiteur1 = debiteur1.getNumero();
				ids.colladm0 = colladm0.getNumero();
				ids.colladm1 = colladm1.getNumero();
				ids.autre0 = autre0.getNumero();
				ids.autre1 = autre1.getNumero();
				return null;
			}
		});

		pushSecurityProvider(provider);
		try {
			final NumbersRequest request = new NumbersRequest();
			final UserLogin login = new UserLogin("xxxxx", 22);
			request.setLogin(login);
			request.getTypes().add(PartyType.NATURAL_PERSON);
			request.getTypes().add(PartyType.HOUSEHOLD);

			// sans les tiers annulés
			request.setIncludeCancelled(false);
			{
				final NumbersResponse response = (NumbersResponse) handler.handle(request);
				assertNotNull(response);
				assertFalse(response.isIncludeCancelled());

				final List<PartyType> types = response.getTypes();
				assertNotNull(types);
				assertEquals(2, types.size());
				assertTrue(types.containsAll(Arrays.asList(PartyType.NATURAL_PERSON, PartyType.HOUSEHOLD)));

				final List<Integer> foundIds = response.getIds();
				assertNotNull(foundIds);
				assertEquals(2, foundIds.size());
				assertTrue(foundIds.containsAll(Arrays.asList((int) ids.pp0, (int) ids.menage0)));
			}

			// avec les tiers annulés
			request.setIncludeCancelled(true);
			{
				final NumbersResponse response = (NumbersResponse) handler.handle(request);
				assertNotNull(response);
				assertTrue(response.isIncludeCancelled());

				final List<PartyType> types = response.getTypes();
				assertNotNull(types);
				assertEquals(2, types.size());
				assertTrue(types.containsAll(Arrays.asList(PartyType.NATURAL_PERSON, PartyType.HOUSEHOLD)));

				final List<Integer> foundIds = response.getIds();
				assertNotNull(foundIds);
				assertEquals(4, foundIds.size());
				assertTrue(foundIds.containsAll(Arrays.asList((int) ids.pp0, (int) ids.pp1, (int) ids.menage0, (int) ids.menage1)));
			}
		}
		finally {
			popSecurityProvider();
		}
	}
}
