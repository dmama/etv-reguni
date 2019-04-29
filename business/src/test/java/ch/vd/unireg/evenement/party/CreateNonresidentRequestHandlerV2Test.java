package ch.vd.unireg.evenement.party;

import java.util.EnumSet;

import org.junit.Test;

import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.security.MockSecurityProvider;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.xml.EnumHelper;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.common.v1.Date;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.nonresident.v2.CreateNonresidentRequest;
import ch.vd.unireg.xml.event.party.nonresident.v2.CreateNonresidentResponse;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.party.person.v2.NaturalPersonCategory;
import ch.vd.unireg.xml.party.person.v2.Sex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CreateNonresidentRequestHandlerV2Test extends BusinessTest {
	
	private CreateNonresidentRequestHandlerV2 handler;
	private static final UserLogin USER_LOGIN = new UserLogin("USER", 22);

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		handler = new CreateNonresidentRequestHandlerV2();
		handler.setHibernateTemplate(hibernateTemplate);
	}

	@Test
	public void testSansDroit() throws Exception {
		handler.setSecurityProvider(new MockSecurityProvider());
		final CreateNonresidentRequest request = new CreateNonresidentRequest(
				USER_LOGIN, "Nabit", "Pala", new Date(1980, 1, 2), Sex.MALE, NaturalPersonCategory.SWISS, 7561111111111L, null
		);
		try {
			handler.handle(request).getResponse();
			Assert.fail("Le demandeur n'a pas de droit sur l'appli et il ne devrait pas pouvoir demander la creation d'un non-habitant");
		}
		catch (ServiceException e) {
			assertTrue(e.getInfo() instanceof AccessDeniedExceptionInfo);
		}
		handler.setSecurityProvider(null);
	}

	@Test
	public void testCasNormaux() throws Exception {
		handler.setSecurityProvider(new MockSecurityProvider(Role.CREATE_NONHAB));
		for( NaturalPersonCategory category : EnumSet.allOf(NaturalPersonCategory.class)) {
			testCasNormal(category);
		}
		handler.setSecurityProvider(null);
	}

	private void testCasNormal(final NaturalPersonCategory category) throws Exception {

		final CreateNonresidentRequest request = new CreateNonresidentRequest(
				USER_LOGIN, "Nabit", "Pala", new Date(1980, 1, 2), Sex.MALE, category, 7561111111111L, null
		);
		final CreateNonresidentResponse res= doInNewTransaction(status -> {
			try {
				return handler.handle(request).getResponse();
			}
			catch (ServiceException e) {
				throw new RuntimeException(e);
			}
		});

		assertNotNull(res.getNumber());

		doInNewTransaction(status -> {
			PersonnePhysique pp = (PersonnePhysique) tiersDAO.get((long) res.getNumber());
			assertNotNull("Le non-habitant n'est pas en base", pp);
			assertFalse("On est sensé avoir créer un non-habitant !", pp.isHabitantVD());
			assertNull("le non-habitant ne devrait pas avoir de numéro d'indiv", pp.getNumeroIndividu());
			assertEquals("Le nom du non-habitant ne correspond pas à celui de la demande de création", "Nabit", pp.getNom());
			assertEquals("Le prénom du non-habitant ne correspond pas à celui de la demande de création", "Pala", pp.getPrenomUsuel());
			assertEquals("Le date de naissance du non-habitant ne correspond pas à celui de la demande de création", date(1980, 1, 2), pp.getDateNaissance());
			assertEquals("Le sexe du non-habitant ne correspond pas à celui de la demande de création", Sexe.MASCULIN, pp.getSexe());
			if (category == NaturalPersonCategory.SWISS) {
				assertNull("Le non-habitant crée est suisse, sa catégorie d'étranger doit être null", pp.getCategorieEtranger());
			}
			else {
				assertEquals("Le non-habitant crée n'est pas de la bonne catégorie d'étranger ", category, EnumHelper.coreToXMLv2(pp.getCategorieEtranger()));
			}
			assertEquals("Le numéro AVS du non-habitant ne correspond pas à celui de la demande de création", "7561111111111", pp.getNumeroAssureSocial());
			return null;
		});
	}


}
