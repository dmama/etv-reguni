package ch.vd.uniregctb.xml.party.v5;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.party.landregistry.v1.AdministrativeAuthorityIdentity;
import ch.vd.unireg.xml.party.landregistry.v1.CorporationIdentity;
import ch.vd.unireg.xml.party.landregistry.v1.NaturalPersonIdentity;
import ch.vd.unireg.xml.party.landregistry.v1.RightHolder;
import ch.vd.uniregctb.registrefoncier.CollectivitePubliqueRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleBeneficiaireRF;
import ch.vd.uniregctb.registrefoncier.PersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.ProprieteParEtageRF;
import ch.vd.uniregctb.xml.DataHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class RightHolderBuilderTest {

	@Test
	public void testGetRightHolderOnCommunity() throws Exception {
		try {
			RightHolderBuilder.getRightHolder(new CommunauteRF(), t -> null);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("Type d'ayant-droit illégal=[CommunauteRF]", e.getMessage());
		}
	}

	/**
	 * Ce test vérifie que le numéro de contribuable est bien la seule information retournée sur un propriétaire rapproché avec un contribuable Unireg.
	 */
	@Test
	public void testGetRightHolderOnKnownParty() throws Exception {

		final long ctbId = 3838383L;

		final RightHolder owner = RightHolderBuilder.getRightHolder(new PersonnePhysiqueRF(), t -> ctbId);
		assertNotNull(owner);
		assertEquals(Integer.valueOf((int) ctbId), owner.getTaxPayerNumber());
		assertNull(owner.getImmovablePropertyId());
		assertNull(owner.getIdentity());
	}

	@Test
	public void testGetRightHolderKnownPartyForPP() throws Exception {

		final PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
		pp.setPrenom("Camille");
		pp.setNom("Bjoook");
		pp.setDateNaissance(RegDate.get(1933, 7, 21));

		final RightHolder owner = RightHolderBuilder.getRightHolder(pp, t -> null);
		assertNotNull(owner);
		assertNull(owner.getTaxPayerNumber());
		assertNull(owner.getImmovablePropertyId());
		final NaturalPersonIdentity identity = (NaturalPersonIdentity) owner.getIdentity();
		assertEquals("Camille", identity.getFirstName());
		assertEquals("Bjoook", identity.getLastName());
		assertEquals(RegDate.get(1933, 7, 21), DataHelper.xmlToCore(identity.getDateOfBirth()));
	}

	@Test
	public void testGetRightHolderKnownPartyForPM() throws Exception {

		final PersonneMoraleRF pm = new PersonneMoraleRF();
		pm.setNumeroRC("CH3384838");
		pm.setRaisonSociale("Papiers fins");

		final RightHolder owner = RightHolderBuilder.getRightHolder(pm, t -> null);
		assertNotNull(owner);
		assertNull(owner.getTaxPayerNumber());
		assertNull(owner.getImmovablePropertyId());
		final CorporationIdentity identity = (CorporationIdentity) owner.getIdentity();
		assertEquals("Papiers fins", identity.getName());
		assertEquals("CH3384838", identity.getCommercialRegisterNumber());
	}

	@Test
	public void testGetRightHolderKnownPartyForColl() throws Exception {

		final CollectivitePubliqueRF coll = new CollectivitePubliqueRF();
		coll.setRaisonSociale("Club de dés");

		final RightHolder owner = RightHolderBuilder.getRightHolder(coll, t -> null);
		assertNotNull(owner);
		assertNull(owner.getTaxPayerNumber());
		assertNull(owner.getImmovablePropertyId());
		final AdministrativeAuthorityIdentity identity = (AdministrativeAuthorityIdentity) owner.getIdentity();
		assertEquals("Club de dés", identity.getName());
	}

	@Test
	public void testGetRightHolderImmovableProperty() throws Exception {

		final Long dominantId = 2928282L;

		final ProprieteParEtageRF dominant = new ProprieteParEtageRF();
		dominant.setIdRF("a8388e8e83");
		dominant.setId(dominantId);

		final ImmeubleBeneficiaireRF beneficiaire = new ImmeubleBeneficiaireRF();
		beneficiaire.setIdRF(dominant.getIdRF());
		beneficiaire.setImmeuble(dominant);

		final RightHolder owner = RightHolderBuilder.getRightHolder(beneficiaire, t -> null);
		assertNotNull(owner);
		assertNull(owner.getTaxPayerNumber());
		assertNull(owner.getIdentity());
		assertEquals(dominantId ,owner.getImmovablePropertyId());
	}
}