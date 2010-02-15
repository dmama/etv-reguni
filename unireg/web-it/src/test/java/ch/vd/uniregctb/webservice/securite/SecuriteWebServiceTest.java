package ch.vd.uniregctb.webservice.securite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.ws.BindingProvider;

import net.java.dev.jaxb.array.LongArray;

import org.apache.log4j.Logger;
import org.junit.Test;

import ch.vd.uniregctb.common.WebitTest;
import ch.vd.uniregctb.webservices.security.GetAutorisationSurDossier;
import ch.vd.uniregctb.webservices.security.GetDossiersControles;
import ch.vd.uniregctb.webservices.security.NiveauAutorisation;
import ch.vd.uniregctb.webservices.security.SecuritePort;
import ch.vd.uniregctb.webservices.security.SecuriteService;
import ch.vd.uniregctb.webservices.security.UserLogin;
import ch.vd.uniregctb.webservices.security.WebServiceException_Exception;

/**
 * Test d'intégration qui vérifie le fonctionnement du web-service 'sécurité'.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class SecuriteWebServiceTest extends WebitTest {

	private static final Logger LOGGER = Logger.getLogger(SecuriteWebServiceTest.class);

	private static final String DB_UNIT_DATA_FILE = "SecuriteWebServiceTest.xml";

	protected static SecuritePort service;
	private static boolean alreadySetUp = false;

	private UserLogin zaiptf; // Francis Perroset
	private UserLogin zaipmd; // Philippe Maillard
	private UserLogin zciddo; // Daniel Di Lallo
	private UserLogin zciaoc; // Annie Ourliac

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		if (service == null) {
			LOGGER.info("Connecting to: " + securiteUrl + " with user = " + username);

			SecuriteService s = new SecuriteService();
			service = s.getSecuritePortPort();

			Map<String, Object> context = ((BindingProvider) service).getRequestContext();
			if (username != null) {
				context.put(BindingProvider.USERNAME_PROPERTY, username);
				context.put(BindingProvider.PASSWORD_PROPERTY, password);
			}
			context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, securiteUrl);
		}

		if (!alreadySetUp) {
			loadDatabase(DB_UNIT_DATA_FILE);
			alreadySetUp = true;
		}

		zaiptf = new UserLogin();
		zaiptf.setUserId("zaiptf");
		zaiptf.setOid(22); // ACI

		zaipmd = new UserLogin();
		zaipmd.setUserId("zaipmd");
		zaipmd.setOid(22); // ACI

		zciddo = new UserLogin();
		zciddo.setUserId("zciddo");
		zciddo.setOid(0);

		zciaoc = new UserLogin();
		zciaoc.setUserId("zciaoc");
		zciaoc.setOid(10);
	}

	@Test
	public void testTiersInconnu() {
		try {
			GetAutorisationSurDossier params = new GetAutorisationSurDossier();
			params.setLogin(zaiptf);
			params.setNumeroTiers(0L); // tiers inconnu
			assertEquals(NiveauAutorisation.ECRITURE, service.getAutorisationSurDossier(params));
			fail();
		}
		catch (WebServiceException_Exception expected) {
			assertContains("Le tiers id=[0] n'existe pas", expected.getMessage());
		}
	}

	/**
	 * Teste que Francis Perroset possède les droits d'écriture sur tous les contribuables (parce que les droits d'accès ont été définis comme ça)
	 */
	@Test
	public void testAutorisationsFrancisPerroset() throws Exception {

		GetAutorisationSurDossier params = new GetAutorisationSurDossier();
		params.setLogin(zaiptf);

		params.setNumeroTiers(12300001L); // Christine Schmid
		assertEquals(NiveauAutorisation.ECRITURE, service.getAutorisationSurDossier(params));

		params.setNumeroTiers(12300002L); // Laurent Schmid
		assertEquals(NiveauAutorisation.ECRITURE, service.getAutorisationSurDossier(params));

		params.setNumeroTiers(86006202L); // Christine et Laurent Schmid
		assertEquals(NiveauAutorisation.ECRITURE, service.getAutorisationSurDossier(params));

		params.setNumeroTiers(10210315L); // Jean-Eric Cuendet
		assertEquals(NiveauAutorisation.ECRITURE, service.getAutorisationSurDossier(params));

		params.setNumeroTiers(61615502L); // Jean-Philippe Maillefer
		assertEquals(NiveauAutorisation.ECRITURE, service.getAutorisationSurDossier(params));

		params.setNumeroTiers(10149508L); // Pascal Broulis
		assertEquals(NiveauAutorisation.ECRITURE, service.getAutorisationSurDossier(params));
	}

	/**
	 * Teste que Philippe Maillard possède les droits d'écriture sur tous les contribuables (parce qu'il fait partie de la direction de l'ACI)
	 */
	@Test
	public void testAutorisationsPhilippeMaillard() throws Exception {

		GetAutorisationSurDossier params = new GetAutorisationSurDossier();
		params.setLogin(zaipmd);

		params.setNumeroTiers(12300001L); // Christine Schmid
		assertEquals(NiveauAutorisation.ECRITURE, service.getAutorisationSurDossier(params));

		params.setNumeroTiers(12300002L); // Laurent Schmid
		assertEquals(NiveauAutorisation.ECRITURE, service.getAutorisationSurDossier(params));

		params.setNumeroTiers(86006202L); // Christine et Laurent Schmid
		assertEquals(NiveauAutorisation.ECRITURE, service.getAutorisationSurDossier(params));

		params.setNumeroTiers(10210315L); // Jean-Eric Cuendet
		assertEquals(NiveauAutorisation.ECRITURE, service.getAutorisationSurDossier(params));

		params.setNumeroTiers(61615502L); // Jean-Philippe Maillefer
		assertEquals(NiveauAutorisation.ECRITURE, service.getAutorisationSurDossier(params));

		params.setNumeroTiers(10149508L); // Pascal Broulis
		assertEquals(NiveauAutorisation.ECRITURE, service.getAutorisationSurDossier(params));
	}

	/**
	 * Teste que Daniel Di Lallo ne possède aucun droit sur Laurent Schmid et son couple (interdiction) ni sur Pascal Broulis
	 * (autorisation exclusive pour Francis Perroset).
	 */
	@Test
	public void testAutorisationsDanielDiLallo() throws Exception {

		GetAutorisationSurDossier params = new GetAutorisationSurDossier();
		params.setLogin(zciddo);

		params.setNumeroTiers(12300001L); // Christine Schmid
		assertEquals(NiveauAutorisation.ECRITURE, service.getAutorisationSurDossier(params)); // ok -> pas d'interdiction sur Christine
																								// Schmidt

		params.setNumeroTiers(12300002L); // Laurent Schmid
		assertNull(service.getAutorisationSurDossier(params));

		params.setNumeroTiers(86006202L); // Christine et Laurent Schmid
		assertNull(service.getAutorisationSurDossier(params)); // ok -> interdiction de Laurent étendu au couple

		params.setNumeroTiers(10210315L); // Jean-Eric Cuendet
		assertEquals(NiveauAutorisation.ECRITURE, service.getAutorisationSurDossier(params));

		params.setNumeroTiers(61615502L); // Jean-Philippe Maillefer
		assertEquals(NiveauAutorisation.ECRITURE, service.getAutorisationSurDossier(params));

		params.setNumeroTiers(10149508L); // Pascal Broulis
		assertNull(service.getAutorisationSurDossier(params)); // ok -> autorisation pour Francis Perroset uniquement
	}

	/**
	 * Teste que Annie Ourliac ne possède aucun droit sur Pascal Broulis (autorisation exclusive pour Francis Perroset).
	 */
	@Test
	public void testAutorisationsAnnieOurliac() throws Exception {

		GetAutorisationSurDossier params = new GetAutorisationSurDossier();
		params.setLogin(zciaoc);

		params.setNumeroTiers(12300001L); // Christine Schmid
		assertEquals(NiveauAutorisation.ECRITURE, service.getAutorisationSurDossier(params));

		params.setNumeroTiers(12300002L); // Laurent Schmid
		assertEquals(NiveauAutorisation.ECRITURE, service.getAutorisationSurDossier(params));

		params.setNumeroTiers(86006202L); // Christine et Laurent Schmid
		assertEquals(NiveauAutorisation.ECRITURE, service.getAutorisationSurDossier(params));

		params.setNumeroTiers(10210315L); // Daniel Di Lallo
		assertEquals(NiveauAutorisation.ECRITURE, service.getAutorisationSurDossier(params));

		params.setNumeroTiers(61615502L); // Jean-Philippe Maillefer
		assertEquals(NiveauAutorisation.ECRITURE, service.getAutorisationSurDossier(params));

		params.setNumeroTiers(10149508L); // Pascal Broulis
		assertNull(service.getAutorisationSurDossier(params)); // ok -> autorisation pour Francis Perroset uniquement
	}

	@Test
	public void testGetDossiersControles() throws Exception {

		// Requête de la part de host-interface
		GetDossiersControles params = new GetDossiersControles();
		params.setAuthenticationToken("I swear I am Host-Interface"); // What, a lie ?

		final LongArray dossiers = service.getDossiersControles(params);
		assertNotNull(dossiers);

		final Set<Long> ids = new HashSet<Long>(dossiers.getItem());
		assertEquals(3, ids.size());
		assertTrue(ids.contains(10149508L)); // Pascal Broulis
		assertTrue(ids.contains(12300002L)); // Laurent Schmid
		assertTrue(ids.contains(86006202L)); // le ménage Schmid

		// Requête de la part de quelqu'un d'autre
		try {
			params.setAuthenticationToken("I am an evil force trying to abuse Unireg");
			service.getDossiersControles(params);
			fail();
		}
		catch (WebServiceException_Exception ok) {
			// Unireg ne s'est pas laissé abusé :-)
		}
	}
}
