package ch.vd.unireg.evenement.party;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.technical.esb.util.EsbDataHandler;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.evenement.RequestHandlerResult;
import ch.vd.unireg.security.MockSecurityProvider;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.tiers.AutreCommunaute;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.numbers.v1.NumbersRequest;
import ch.vd.unireg.xml.event.party.numbers.v1.NumbersResponse;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.party.v1.PartyType;

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

		handler.setSecurityProvider(new MockSecurityProvider());
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
			handler.setSecurityProvider(null);
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

		doInNewTransaction(status -> {
			PersonnePhysique pp0 = addNonHabitant("Marie", "Gooert", date(1965, 3, 22), Sexe.FEMININ);
			PersonnePhysique pp1 = addNonHabitant("Georgette", "van Zum", date(1934, 4, 19), Sexe.FEMININ);
			pp1.setAnnule(true);

			MenageCommun menage0 = addMenageCommun(null);
			MenageCommun menage1 = addMenageCommun(null);
			menage1.setAnnule(true);

			Etablissement etablissement0 = addEtablissement(null);
			Etablissement etablissement1 = addEtablissement(null);
			etablissement1.setAnnule(true);

			Entreprise entreprise0 = addEntrepriseInconnueAuCivil();
			Entreprise entreprise1 = addEntrepriseInconnueAuCivil();
			entreprise1.setAnnule(true);

			DebiteurPrestationImposable debiteur0 = addDebiteur();
			DebiteurPrestationImposable debiteur1 = addDebiteur();
			debiteur1.setAnnule(true);

			CollectiviteAdministrative colladm0 = addCollAdm(666);
			CollectiviteAdministrative colladm1 = addCollAdm(42);
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
		});

		handler.setSecurityProvider(provider);
		try {
			final NumbersRequest request = new NumbersRequest();
			final UserLogin login = new UserLogin("xxxxx", 22);
			request.setLogin(login);
			request.getTypes().add(PartyType.NATURAL_PERSON);
			request.getTypes().add(PartyType.HOUSEHOLD);

			// sans les tiers annulés
			request.setIncludeCancelled(false);
			{
				final RequestHandlerResult<NumbersResponse> r = handler.handle(request);
				final NumbersResponse response = r.getResponse();
				assertNotNull(response);
				assertFalse(response.isIncludeCancelled());

				final List<PartyType> types = response.getTypes();
				assertNotNull(types);
				assertEquals(2, types.size());
				assertTrue(types.containsAll(Arrays.asList(PartyType.NATURAL_PERSON, PartyType.HOUSEHOLD)));

				final List<Integer> foundIds = extractIds(r);
				assertNotNull(foundIds);
				assertEquals(2, foundIds.size());
				assertTrue(foundIds.containsAll(Arrays.asList((int) ids.pp0, (int) ids.menage0)));
			}

			// avec les tiers annulés
			request.setIncludeCancelled(true);
			{
				final RequestHandlerResult<NumbersResponse>  r = handler.handle(request);
				final NumbersResponse response = r.getResponse();
				assertNotNull(response);
				assertTrue(response.isIncludeCancelled());

				final List<PartyType> types = response.getTypes();
				assertNotNull(types);
				assertEquals(2, types.size());
				assertTrue(types.containsAll(Arrays.asList(PartyType.NATURAL_PERSON, PartyType.HOUSEHOLD)));

				final List<Integer> foundIds = extractIds(r);
				assertNotNull(foundIds);
				assertEquals(4, foundIds.size());
				assertTrue(foundIds.containsAll(Arrays.asList((int) ids.pp0, (int) ids.pp1, (int) ids.menage0, (int) ids.menage1)));
			}
		}
		finally {
			handler.setSecurityProvider(null);
		}
	}

	private static List<Integer> extractIds(RequestHandlerResult<NumbersResponse> r) throws IOException {
		final EsbDataHandler attachement = r.getAttachments().get("ids");
		assertNotNull(attachement);

		final InputStream inputStream = attachement.getDataHandler().getInputStream();
		assertNotNull(inputStream);

		final List<Integer> foundIds = new ArrayList<>();
		try (Scanner scanner = new Scanner(inputStream, "UTF-8")) {
			while (scanner.hasNext()) {
				foundIds.add(Integer.parseInt(scanner.next()));
			}
		}
		return foundIds;
	}
}
