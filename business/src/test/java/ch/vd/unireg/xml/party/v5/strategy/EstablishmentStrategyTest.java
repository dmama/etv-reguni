package ch.vd.unireg.xml.party.v5.strategy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.regimefiscal.RegimeFiscalService;
import ch.vd.unireg.registrefoncier.RegistreFoncierService;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.xml.Context;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.party.establishment.v2.Establishment;
import ch.vd.unireg.xml.party.v5.InternalPartyPart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EstablishmentStrategyTest extends BusinessTest {

	private EstablishmentStrategy strategy;
	private Context context;

	@Before
	public void setUp() throws Exception {
		strategy = new EstablishmentStrategy();

		context = new Context();
		context.tiersService = getBean(TiersService.class, "tiersService");
		context.regimeFiscalService = getBean(RegimeFiscalService.class, "regimeFiscalService");
		context.registreFoncierService = getBean(RegistreFoncierService.class, "serviceRF");
		context.adresseService = getBean(AdresseService.class, "adresseService");
	}

	/**
	 * [SIFISC-29739] Vérifie que les salutations sont bien retournées.
	 */
	@Test
	public void testGetSalutation() throws Exception {

		final Long id = doInNewTransaction(status -> addEtablissement().getId());

		final Establishment eta = newFrom(id);
		assertNotNull(eta);
		assertEquals("Madame, Monsieur", eta.getFormalGreeting());
	}


	private Establishment newFrom(long id, InternalPartyPart... parts) throws Exception {
		return doInNewTransaction(status -> {
			final Etablissement eta = hibernateTemplate.get(Etablissement.class, id);
			try {
				final Set<InternalPartyPart> p = (parts == null || parts.length == 0 ? null : new HashSet<>(Arrays.asList(parts)));
				return strategy.newFrom(eta, p, context);
			}
			catch (ServiceException e) {
				throw new RuntimeException(e);
			}
		});
	}
}