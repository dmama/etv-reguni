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
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.xml.Context;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.party.person.v5.CommonHousehold;
import ch.vd.unireg.xml.party.v5.InternalPartyPart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CommonHouseholdStrategyTest extends BusinessTest {

	private CommonHouseholdStrategy strategy;
	private Context context;

	@Before
	public void setUp() throws Exception {
		strategy = new CommonHouseholdStrategy();

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

		final Long id = doInNewTransaction(status -> {
			final PersonnePhysique principal = addNonHabitant("Sébastien", "Goulot", null, Sexe.MASCULIN);
			final PersonnePhysique conjoint = addNonHabitant("Juliette", "Col", null, Sexe.FEMININ);
			final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(principal, conjoint, date(1990, 4, 12), null);
			return ensemble.getMenage().getNumero();
		});

		final CommonHousehold mc = newFrom(id);
		assertNotNull(mc);
		assertEquals("Monsieur et Madame", mc.getSalutation());
		assertEquals("Monsieur et Madame", mc.getFormalGreeting());
	}

	private CommonHousehold newFrom(long id, InternalPartyPart... parts) throws Exception {
		return doInNewTransaction(status -> {
			final MenageCommun mc = hibernateTemplate.get(MenageCommun.class, id);
			try {
				final Set<InternalPartyPart> p = (parts == null || parts.length == 0 ? null : new HashSet<>(Arrays.asList(parts)));
				return strategy.newFrom(mc, p, context);
			}
			catch (ServiceException e) {
				throw new RuntimeException(e);
			}
		});
	}
}