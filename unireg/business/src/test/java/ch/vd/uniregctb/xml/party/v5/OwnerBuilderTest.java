package ch.vd.uniregctb.xml.party.v5;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.party.landregistry.v1.AdministrativeAuthorityIdentity;
import ch.vd.unireg.xml.party.landregistry.v1.CorporationIdentity;
import ch.vd.unireg.xml.party.landregistry.v1.NaturalPersonIdentity;
import ch.vd.unireg.xml.party.landregistry.v1.Owner;
import ch.vd.uniregctb.registrefoncier.CollectivitePubliqueRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.PersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.xml.DataHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class OwnerBuilderTest {

	@Test
	public void testGetOwnerOnCommunity() throws Exception {
		try {
			OwnerBuilder.getOwner(new CommunauteRF(), t -> null);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("On ne devrait pas recevoir de communauté", e.getMessage());
		}
	}

	/**
	 * Ce test vérifie que le numéro de contribuable est bien la seule information retournée sur un propriétaire rapproché avec un contribuable Unireg.
	 */
	@Test
	public void testGetOwnerOnKnownParty() throws Exception {

		final long ctbId = 3838383L;

		final Owner owner = OwnerBuilder.getOwner(new PersonnePhysiqueRF(), t -> ctbId);
		assertNotNull(owner);
		assertEquals(Integer.valueOf((int) ctbId), owner.getTaypPayerNumber());
		assertNull(owner.getIdentity());
	}

	@Test
	public void testGetOwnerKnownPartyForPP() throws Exception {

		final PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
		pp.setPrenom("Camille");
		pp.setNom("Bjoook");
		pp.setDateNaissance(RegDate.get(1933, 7, 21));

		final Owner owner = OwnerBuilder.getOwner(pp, t -> null);
		assertNotNull(owner);
		assertNull(owner.getTaypPayerNumber());
		final NaturalPersonIdentity identity = (NaturalPersonIdentity) owner.getIdentity();
		assertEquals("Camille", identity.getFirstName());
		assertEquals("Bjoook", identity.getLastName());
		assertEquals(RegDate.get(1933, 7, 21), DataHelper.xmlToCore(identity.getDateOfBirth()));
	}

	@Test
	public void testGetOwnerKnownPartyForPM() throws Exception {

		final PersonneMoraleRF pm = new PersonneMoraleRF();
		pm.setNumeroRC("CH3384838");
		pm.setRaisonSociale("Papiers fins");

		final Owner owner = OwnerBuilder.getOwner(pm, t -> null);
		assertNotNull(owner);
		assertNull(owner.getTaypPayerNumber());
		final CorporationIdentity identity = (CorporationIdentity) owner.getIdentity();
		assertEquals("Papiers fins", identity.getName());
		assertEquals("CH3384838", identity.getCommercialRegisterNumber());
	}

	@Test
	public void testGetOwnerKnownPartyForColl() throws Exception {

		final CollectivitePubliqueRF coll = new CollectivitePubliqueRF();
		coll.setRaisonSociale("Club de dés");

		final Owner owner = OwnerBuilder.getOwner(coll, t -> null);
		assertNotNull(owner);
		assertNull(owner.getTaypPayerNumber());
		final AdministrativeAuthorityIdentity identity = (AdministrativeAuthorityIdentity) owner.getIdentity();
		assertEquals("Club de dés", identity.getName());
	}
}