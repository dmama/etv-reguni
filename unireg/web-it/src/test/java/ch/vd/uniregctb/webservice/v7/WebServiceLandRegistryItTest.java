package ch.vd.uniregctb.webservice.v7;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.ws.landregistry.v7.BuildingEntry;
import ch.vd.unireg.ws.landregistry.v7.BuildingList;
import ch.vd.unireg.ws.landregistry.v7.CommunityOfOwnersEntry;
import ch.vd.unireg.ws.landregistry.v7.CommunityOfOwnersList;
import ch.vd.unireg.ws.landregistry.v7.ImmovablePropertyEntry;
import ch.vd.unireg.ws.landregistry.v7.ImmovablePropertyList;
import ch.vd.unireg.xml.common.v2.Date;
import ch.vd.unireg.xml.error.v1.ErrorType;
import ch.vd.unireg.xml.party.corporation.v5.Corporation;
import ch.vd.unireg.xml.party.landregistry.v1.AcquisitionReason;
import ch.vd.unireg.xml.party.landregistry.v1.Building;
import ch.vd.unireg.xml.party.landregistry.v1.BuildingDescription;
import ch.vd.unireg.xml.party.landregistry.v1.BuildingSetting;
import ch.vd.unireg.xml.party.landregistry.v1.CaseIdentifier;
import ch.vd.unireg.xml.party.landregistry.v1.CommunityOfOwners;
import ch.vd.unireg.xml.party.landregistry.v1.CommunityOfOwnersType;
import ch.vd.unireg.xml.party.landregistry.v1.CondominiumOwnership;
import ch.vd.unireg.xml.party.landregistry.v1.DatedShare;
import ch.vd.unireg.xml.party.landregistry.v1.GroundArea;
import ch.vd.unireg.xml.party.landregistry.v1.HousingRight;
import ch.vd.unireg.xml.party.landregistry.v1.ImmovableProperty;
import ch.vd.unireg.xml.party.landregistry.v1.LandOwnershipRight;
import ch.vd.unireg.xml.party.landregistry.v1.LandRight;
import ch.vd.unireg.xml.party.landregistry.v1.Location;
import ch.vd.unireg.xml.party.landregistry.v1.NaturalPersonIdentity;
import ch.vd.unireg.xml.party.landregistry.v1.OwnershipType;
import ch.vd.unireg.xml.party.landregistry.v1.RealEstate;
import ch.vd.unireg.xml.party.landregistry.v1.RightHolder;
import ch.vd.unireg.xml.party.landregistry.v1.Share;
import ch.vd.unireg.xml.party.landregistry.v1.TaxEstimate;
import ch.vd.unireg.xml.party.landregistry.v1.TotalArea;
import ch.vd.unireg.xml.party.landregistry.v1.UsufructRight;
import ch.vd.unireg.xml.party.landregistry.v1.VirtualInheritedLandRight;
import ch.vd.unireg.xml.party.landregistry.v1.VirtualLandOwnershipRight;
import ch.vd.unireg.xml.party.person.v5.NaturalPerson;
import ch.vd.unireg.xml.party.v5.Party;
import ch.vd.unireg.xml.party.v5.PartyPart;
import ch.vd.uniregctb.common.DataHelper;

import static ch.vd.uniregctb.webservice.v7.WebServiceGetPartyItTest.buildUriAndParams;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("Duplicates")
public class WebServiceLandRegistryItTest extends AbstractWebServiceItTest {

	private static final String DB_UNIT_DATA_FILE = "WebServiceLandRegistryItTest.xml";

	private static boolean alreadySetUp = false;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		if (!alreadySetUp) {
			loadDatabase(DB_UNIT_DATA_FILE);
			alreadySetUp = true;
		}
	}

	@Test
	public void testGetPPLandRights() throws Exception {
		final int noTiers = 10035633;

		final Pair<String, Map<String, ?>> params = buildUriAndParams(noTiers, EnumSet.of(PartyPart.LAND_RIGHTS));
		final ResponseEntity<Party> resp = get(Party.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
		assertNotNull(resp);
		assertEquals(HttpStatus.OK, resp.getStatusCode());

		final Party party = resp.getBody();
		assertNotNull(party);
		assertEquals(NaturalPerson.class, party.getClass());

		final NaturalPerson naturalPerson = (NaturalPerson) party;
		assertEquals("Tummers-De Wit Wouter", naturalPerson.getOfficialName());
		assertEquals("Elisabeth", naturalPerson.getFirstName());

		final List<LandRight> landRights = naturalPerson.getLandRights();
		assertEquals(1, landRights.size());

		final LandOwnershipRight landRight0 = (LandOwnershipRight) landRights.get(0);
		assertLandOwnershipRight(RegDate.get(1981, 3, 6), null, "Succession", null, OwnershipType.SIMPLE_CO_OWNERSHIP, 1, 4, noTiers, 264822986L, 264310664, landRight0);
	}

	/**
	 * <pre>
	 *     +----------+
	 *     |    PP    |       copropriété (1/4)    +-------------------------+
	 *     | 10035633 |--------------------------->| Immeuble 0 (bien-fonds) |
	 *     |          |                            +-------------------------+
	 *     +----------+                                      ^
	 *          ^                                            :
	 *          | hérite de                                  :
	 *          |                                            :
	 *     +----------+                                      :
	 *     |    PP    |      droit virtuel hérité            :
	 *     | 10092818 |......................................+
	 *     |          |
	 *     +----------+
	 * </pre>
	 */
	@Test
	public void testGetPPVirtualInheritedLandRights() throws Exception {

		final int noHeritier = 10092818;    // Gertrude De Wit Tummers
		final int noDecede = 10035633;      // Elisabeth Tummers-De Wit Wouter
		final RegDate dateHeritage = RegDate.get(2017, 11, 1);

		// le droit réel possède bien une date 'dateInheritedTo' renseignée à la date d'héritage sur le décédé
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(noDecede, EnumSet.of(PartyPart.VIRTUAL_INHERITANCE_LAND_RIGHTS));
			final ResponseEntity<Party> resp = get(Party.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
			assertNotNull(resp);
			assertEquals(HttpStatus.OK, resp.getStatusCode());

			final Party party = resp.getBody();
			assertNotNull(party);
			assertEquals(NaturalPerson.class, party.getClass());

			final NaturalPerson naturalPerson = (NaturalPerson) party;
			assertEquals("Tummers-De Wit Wouter", naturalPerson.getOfficialName());
			assertEquals("Elisabeth", naturalPerson.getFirstName());

			final List<LandRight> landRights = naturalPerson.getLandRights();
			assertEquals(1, landRights.size());

			final LandOwnershipRight landRight0 = (LandOwnershipRight) landRights.get(0);
			assertDate(dateHeritage, landRight0.getDateInheritedTo());
			assertLandOwnershipRight(RegDate.get(1981, 3, 6), null, "Succession", null, OwnershipType.SIMPLE_CO_OWNERSHIP, 1, 4, noDecede, 264822986L, 264310664, landRight0);
		}

		// le droit virtuel est bien exposé sur l'héritier
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(noHeritier, EnumSet.of(PartyPart.VIRTUAL_INHERITANCE_LAND_RIGHTS));
			final ResponseEntity<Party> resp = get(Party.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
			assertNotNull(resp);
			assertEquals(HttpStatus.OK, resp.getStatusCode());

			final Party party = resp.getBody();
			assertNotNull(party);
			assertEquals(NaturalPerson.class, party.getClass());

			final NaturalPerson naturalPerson = (NaturalPerson) party;
			assertEquals("De Wit Wouter", naturalPerson.getOfficialName());
			assertEquals("Gertrude", naturalPerson.getFirstName());

			final List<LandRight> landRights = naturalPerson.getLandRights();
			assertEquals(1, landRights.size());

			final VirtualInheritedLandRight landRight0 = (VirtualInheritedLandRight) landRights.get(0);
			assertVirtualInheritedRight(noHeritier, noDecede, dateHeritage, null, "Succession", null, 264310664, false, landRight0);
			assertLandOwnershipRight(RegDate.get(1981, 3, 6), null, "Succession", null, OwnershipType.SIMPLE_CO_OWNERSHIP, 1, 4, noDecede, 264822986L, 264310664, (LandOwnershipRight) landRight0.getReference());
		}
	}

	/**
	 * <pre>
	 *                       copropriété (1/4)      +-------------------------+
	 *                    +------------------------>| Immeuble 0 (bien-fonds) |
	 *     +---------+    |                         +-------------------------+
	 *     |    PM   |----+                              ^                 ^
	 *     |  21550  |                                   | ppe (20/100)    :
	 *     |         |----+                              |                 :
	 *     +---------+    |  individuelle (1/1)      +------------------+  :
	 *          ^         +------------------------->| Immeuble 1 (ppe) |  :
	 *          |                                    +------------------+  :
	 *          |                                            ^             :
	 *          | absorbe par fusion                         :             :
	 *          |                                            :             :
	 *     +----------+      droit virtuel hérité            :             :
	 *     |    PM    |......................................+             :
	 *     |   666    |      droit virtuel hérité                          :
	 *     |          |....................................................+
	 *     +----------+
	 * </pre>
	 */
	@Test
	public void testGetPMVirtualInheritedLandRights() throws Exception {

		final int noAbsorbante = 666;
		final int noAbsorbee = 21550; // BIGS Architecture et Entreprise Générale S.A.
		final RegDate dateFusion = RegDate.get(2017, 11, 1);

		// les droits réels de l'entreprise absorbée doivent posséder des dates 'dateInheritedTo' renseignées à la date de la fusion
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(noAbsorbee, EnumSet.of(PartyPart.VIRTUAL_INHERITANCE_LAND_RIGHTS));
			final ResponseEntity<Party> resp = get(Party.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
			assertNotNull(resp);
			assertEquals(HttpStatus.OK, resp.getStatusCode());

			final Party party = resp.getBody();
			assertNotNull(party);
			assertEquals(Corporation.class, party.getClass());

			final Corporation corporation = (Corporation) party;
			assertEquals("BIGS Architecture et Entreprise Générale S.A.", corporation.getName());

			final List<LandRight> landRights = corporation.getLandRights();
			assertEquals(2, landRights.size());

			final LandOwnershipRight landRight0 = (LandOwnershipRight) landRights.get(0);
			assertDate(dateFusion, landRight0.getDateInheritedTo());
			assertLandOwnershipRight(null, null, "Achat", null, OwnershipType.SIMPLE_CO_OWNERSHIP, 1, 4, noAbsorbee, 264822986L, 264310664, landRight0);

			final LandOwnershipRight landRight1 = (LandOwnershipRight) landRights.get(1);
			assertDate(dateFusion, landRight1.getDateInheritedTo());
			assertLandOwnershipRight(RegDate.get(1981, 3, 6), null, "Transfert", null, OwnershipType.SOLE_OWNERSHIP, 1, 1, noAbsorbee, null, 357426402, landRight1);
		}

		// les droits réels de l'entreprise absorbée doivent être exposés comme droits virtuels sur l'entreprise absorbante
		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(noAbsorbante, EnumSet.of(PartyPart.VIRTUAL_INHERITANCE_LAND_RIGHTS));
			final ResponseEntity<Party> resp = get(Party.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
			assertNotNull(resp);
			assertEquals(HttpStatus.OK, resp.getStatusCode());

			final Party party = resp.getBody();
			assertNotNull(party);
			assertEquals(Corporation.class, party.getClass());

			final Corporation corporation = (Corporation) party;
			assertEquals("Frigos de Bressonnaz S.A. en liquidation", corporation.getName());

			final List<LandRight> landRights = corporation.getLandRights();
			assertEquals(2, landRights.size());

			final VirtualInheritedLandRight landRight0 = (VirtualInheritedLandRight) landRights.get(0);
			assertVirtualInheritedRight(noAbsorbante, noAbsorbee, dateFusion, null, "Fusion", null, 264310664, false, landRight0);
			assertLandOwnershipRight(null, null, "Achat", null, OwnershipType.SIMPLE_CO_OWNERSHIP, 1, 4, noAbsorbee, 264822986L, 264310664, (LandOwnershipRight) landRight0.getReference());

			final VirtualInheritedLandRight landRight1 = (VirtualInheritedLandRight) landRights.get(1);
			assertVirtualInheritedRight(noAbsorbante, noAbsorbee, dateFusion, null, "Fusion", null, 357426402, false, landRight1);
			assertLandOwnershipRight(RegDate.get(1981, 3, 6), null, "Transfert", null, OwnershipType.SOLE_OWNERSHIP, 1, 1, noAbsorbee, null, 357426402, (LandOwnershipRight) landRight1.getReference());
		}
	}

	/**
	 * <pre>
	 *                       copropriété (1/4)    +-------------------------+
	 *                    +---------------------->| Immeuble 0 (bien-fonds) |
	 *     +---------+    |                       +-------------------------+
	 *     |         |----+                              ^
	 *     |   PM    |                                   | ppe (20/100)
	 *     |         |----+                              |
	 *     +---------+    |  individuelle (1/1)      +------------------+
	 *                    +------------------------->| Immeuble 1 (ppe) |
	 *                                               +------------------+
	 * </pre>
	 */
	@Test
	public void testGetPMLandRights() throws Exception {
		final int noTiers = 21550;
		final long immeuble0 = 264310664L;
		final int immeuble1 = 357426402;

		final Pair<String, Map<String, ?>> params = buildUriAndParams(noTiers, EnumSet.of(PartyPart.LAND_RIGHTS));
		final ResponseEntity<Party> resp = get(Party.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
		assertNotNull(resp);
		assertEquals(HttpStatus.OK, resp.getStatusCode());

		final Party party = resp.getBody();
		assertNotNull(party);
		assertEquals(Corporation.class, party.getClass());

		final Corporation corporation = (Corporation) party;
		assertEquals("BIGS Architecture et Entreprise Générale S.A.", corporation.getName());

		final List<LandRight> landRights = corporation.getLandRights();
		assertEquals(2, landRights.size());

		// le droit de la PM sur le bien-fonds
		final LandOwnershipRight landRight0 = (LandOwnershipRight) landRights.get(0);
		assertLandOwnershipRight(null, null, "Achat", null, OwnershipType.SIMPLE_CO_OWNERSHIP, 1, 4, noTiers, 264822986L, immeuble0, landRight0);

		// le droit de la PM sur la PPE
		final LandOwnershipRight landRight1 = (LandOwnershipRight) landRights.get(1);
		assertLandOwnershipRight(RegDate.get(1981, 3, 6), null, "Transfert", null, OwnershipType.SOLE_OWNERSHIP, 1, 1, noTiers, null, immeuble1, landRight1);
	}

	/**
	 * <pre>
	 *               droit virtuel (1/1 * 20/100)
	 *          +........................................+
	 *          :                                        :
	 *          :                                        v
	 *          :            copropriété (1/4)    +-------------------------+
	 *          :         +---------------------->| Immeuble 0 (bien-fonds) |
	 *     +---------+    |                       +-------------------------+
	 *     |         |----+                              ^
	 *     |   PM    |                                   | ppe (20/100)
	 *     |         |----+                              |
	 *     +---------+    |  individuelle (1/1)      +------------------+
	 *                    +------------------------->| Immeuble 1 (ppe) |
	 *                                               +------------------+
	 * </pre>
	 */
	@Test
	public void testGetPMVirtualLandRights() throws Exception {
		final int noTiers = 21550;
		final long immeuble0 = 264310664L;
		final int immeuble1 = 357426402;

		final Pair<String, Map<String, ?>> params = buildUriAndParams(noTiers, EnumSet.of(PartyPart.VIRTUAL_LAND_RIGHTS));
		final ResponseEntity<Party> resp = get(Party.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
		assertNotNull(resp);
		assertEquals(HttpStatus.OK, resp.getStatusCode());

		final Party party = resp.getBody();
		assertNotNull(party);
		assertEquals(Corporation.class, party.getClass());

		final Corporation corporation = (Corporation) party;
		assertEquals("BIGS Architecture et Entreprise Générale S.A.", corporation.getName());

		final List<LandRight> landRights = corporation.getLandRights();
		assertEquals(3, landRights.size());

		// le droit de la PM sur le bien-fonds
		final LandOwnershipRight landRight0 = (LandOwnershipRight) landRights.get(0);
		assertLandOwnershipRight(null, null, "Achat", null, OwnershipType.SIMPLE_CO_OWNERSHIP, 1, 4, noTiers, 264822986L, immeuble0, landRight0);

		// le droit de la PM sur la PPE
		final LandOwnershipRight landRight1 = (LandOwnershipRight) landRights.get(1);
		assertLandOwnershipRight(RegDate.get(1981, 3, 6), null, "Transfert", null, OwnershipType.SOLE_OWNERSHIP, 1, 1, noTiers, null, immeuble1, landRight1);

		// le droit virtual de la PM sur le bien-fonds propriété de la PPE
		final VirtualLandOwnershipRight landRight2 = (VirtualLandOwnershipRight) landRights.get(2);
		assertVirutalLandOwnershipRight(landRight2, null, RegDate.get(1996, 4, 16), null, "Constitution de PPE", null, noTiers, immeuble0);

		final List<LandOwnershipRight> path = landRight2.getPath();
		assertEquals(2, path.size());
		assertLandOwnershipRight(RegDate.get(1981, 3, 6), null, "Transfert", null, OwnershipType.SOLE_OWNERSHIP, 1, 1, noTiers, null, immeuble1, path.get(0));
		assertLandOwnershipRightImmovableProp(RegDate.get(1996, 4, 16), null, "Constitution de PPE", null, OwnershipType.CONDOMINIUM_OWNERSHIP, 10, 1000, immeuble1, immeuble0, path.get(1));
	}

	@Test
	public void testGetImmovableProperty() throws Exception {

		final int noImmo = 264310664;

		final ResponseEntity<ImmovableProperty> resp = get(ImmovableProperty.class, MediaType.APPLICATION_XML, "/landRegistry/immovableProperty/{id}?user=zaizzt/22", Collections.singletonMap("id", noImmo));
		assertNotNull(resp);
		assertEquals(HttpStatus.OK, resp.getStatusCode());

		final ImmovableProperty immo = resp.getBody();
		assertNotNull(immo);
		assertEquals(RealEstate.class, immo.getClass());

		final RealEstate realEstate = (RealEstate) immo;
		assertEquals(noImmo, realEstate.getId());
		assertEquals("CH785283458046", realEstate.getEgrid());
		assertEquals("https://secure.vd.ch/territoire/intercapi/faces?bfs=227&kr=0&n1=59&n2=&n3=&n4=&type=grundstueck_grundbuch_auszug&sec=WUcNIuAaAn07zT5ky-Pi-g1sLdOQx2ccPgnp0PmINA0SComhxznkhXe6oY5P5pW2-q5y5NRgFZ7s4crPzqU-Yg%3D%3D",
		             realEstate.getUrlIntercapi());
		assertFalse(realEstate.isCfa());

		final List<Location> locations = realEstate.getLocations();
		assertEquals(1, locations.size());
		assertLocation(locations.get(0), RegDate.get(2016, 9, 13), null, 59, null, null, null, 5706);

		final List<TotalArea> totalAreas = realEstate.getTotalAreas();
		assertEquals(1, totalAreas.size());
		assertTotalArea(RegDate.get(2016, 9, 13), null, 1200, totalAreas.get(0));

		final List<GroundArea> groundAreas = realEstate.getGroundAreas();
		assertEquals(1, groundAreas.size());
		assertGroundArea(RegDate.get(2016, 9, 13), null, 1050, "Place-jardin", groundAreas.get(0));

		final List<TaxEstimate> taxEstimates = realEstate.getTaxEstimates();
		assertEquals(1, taxEstimates.size());
		assertTaxEstimation(RegDate.get(1994, 1, 1), null, 570_000L, "RG94", false, taxEstimates.get(0));

		final List<BuildingSetting> settings = realEstate.getBuildingSettings();
		assertEquals(1, settings.size());

		final BuildingSetting setting = settings.get(0);
		assertSetting(RegDate.get(2016, 9, 13), null, 150, noImmo, 266023444, setting);

		final List<LandRight> landRights = realEstate.getLandRights();
		assertEquals(8, landRights.size());
		assertLandOwnershipRight(null, null, "Achat", null, OwnershipType.SIMPLE_CO_OWNERSHIP, 1, 4, 21550, 264822986L, noImmo, (LandOwnershipRight) landRights.get(0));
		final LandOwnershipRight landRight1 = (LandOwnershipRight) landRights.get(1);
		assertLandOwnershipRight(RegDate.get(1981, 3, 6), RegDate.get(2017, 10, 17), "Succession", null, OwnershipType.SIMPLE_CO_OWNERSHIP, 1, 4, "Raymonde", "Grandjean", null, 264822986L, landRight1);
		assertLandOwnershipRight(RegDate.get(1981, 3, 6), null, null, null, OwnershipType.SIMPLE_CO_OWNERSHIP, 1, 4, 264822986L, (LandOwnershipRight) landRights.get(2));
		assertLandOwnershipRight(RegDate.get(1981, 3, 6), null, "Succession", null, OwnershipType.SIMPLE_CO_OWNERSHIP, 1, 4, 10035633, 264822986L, noImmo, (LandOwnershipRight) landRights.get(3));
		assertLandOwnershipRight(RegDate.get(1981, 3, 6), null, "Succession", null, OwnershipType.SIMPLE_CO_OWNERSHIP, 1, 4, null, "Berard Renée", null, 264822986L, (LandOwnershipRight) landRights.get(4));

		assertUsufructRight(RegDate.get(1985, 10, 10), RegDate.get(2017, 10, 17), "Convention", null, landRights.get(5));
		final List<RightHolder> usufructHolders = ((UsufructRight) landRights.get(5)).getRightHolders();
		assertEquals(2, usufructHolders.size());
		assertRightHolderNaturalPerson("Charles", "de Noblebois", null, usufructHolders.get(0));
		assertRightHolderNaturalPerson("Roland", "Proutch", null, usufructHolders.get(1));

		assertLandOwnershipRightImmovableProp(RegDate.get(1996, 4, 16), null, "Constitution de PPE", null, OwnershipType.CONDOMINIUM_OWNERSHIP, 10, 1000, 357426402, noImmo, (LandOwnershipRight) landRights.get(6));

		assertHousingRight(RegDate.get(1999, 8, 8), null, "Convention", null, landRights.get(7));
		final List<RightHolder> housingRightHolders = ((HousingRight) landRights.get(7)).getRightHolders();
		assertEquals(1, housingRightHolders.size());
		assertRightHolderNaturalPerson("Charles", "de Noblebois", null, housingRightHolders.get(0));

		// [SIFISC-23894] ce droit possède plusieurs raisons d'acquisition
		final List<AcquisitionReason> reasons = landRight1.getAcquisitionReasons();
		assertEquals(2, reasons.size());
		assertAcquisitionReason(RegDate.get(1981, 3, 6), "Succession", 12, null, reasons.get(0));
		assertAcquisitionReason(RegDate.get(2014, 9, 13), "Voyage spatio-temporel", 24, "2014/12/1", reasons.get(1));
	}

	@Test
	public void testGetImmovableProperties() throws Exception {

		final int noBienFonds = 264310664;
		final int noPPE1 = 189968987;
		final int noPPE2 = 357426402;

		final Map<String, Integer> params = new HashMap<>();
		params.put("noBienFonds", noBienFonds);
		params.put("noPPE1", noPPE1);
		params.put("noPPE2", noPPE2);

		// on demande les trois immeubles qui existent dans la DB
		final ResponseEntity<ImmovablePropertyList> resp = get(ImmovablePropertyList.class, MediaType.APPLICATION_XML,
		                                                   "/landRegistry/immovableProperties?immoId={noBienFonds}&immoId={noPPE1}&immoId={noPPE2}&user=zaizzt/22",
		                                                   params);
		assertNotNull(resp);
		assertEquals(HttpStatus.OK, resp.getStatusCode());

		// on vérifie qu'on a reçu les immeubles
		final ImmovablePropertyList list = resp.getBody();
		assertNotNull(list);

		final List<ImmovablePropertyEntry> entries = list.getEntries();
		assertNotNull(entries);
		assertEquals(3, entries.size());
		assertEntry(noPPE1, "CH416556658161", entries.get(0));
		assertEntry(noBienFonds, "CH785283458046", entries.get(1));
		assertEntry(noPPE2, "CH796577806563", entries.get(2));
	}

	/**
	 * [SIFISC-24715] Vérifie que l'historique des quotes-parts est bien exposé.
	 */
	@Test
	public void testGetImmovablePropertyCondo() throws Exception {

		final int noImmo = 189968987;

		final ResponseEntity<ImmovableProperty> resp = get(ImmovableProperty.class, MediaType.APPLICATION_XML, "/landRegistry/immovableProperty/{id}?user=zaizzt/22", Collections.singletonMap("id", noImmo));
		assertNotNull(resp);
		assertEquals(HttpStatus.OK, resp.getStatusCode());

		final ImmovableProperty immo = resp.getBody();
		assertNotNull(immo);
		assertEquals(CondominiumOwnership.class, immo.getClass());

		final CondominiumOwnership condo = (CondominiumOwnership) immo;
		assertEquals(noImmo, condo.getId());
		assertEquals("CH416556658161", condo.getEgrid());

		final List<DatedShare> shares = condo.getShares();
		assertNotNull(shares);
		assertEquals(2, shares.size());
		assertShare(null, RegDate.get(2017, 1, 13), 172, 1000, shares.get(0));
		assertShare(RegDate.get(2017, 1, 14), null, 170, 1000, shares.get(1));
	}

	@Test
	public void testGetImmovablePropertyOwningAnotherImmovableProperty() throws Exception {

		final int noImmo = 357426402;

		final ResponseEntity<ImmovableProperty> resp = get(ImmovableProperty.class, MediaType.APPLICATION_XML, "/landRegistry/immovableProperty/{id}?user=zaizzt/22", Collections.singletonMap("id", noImmo));
		assertNotNull(resp);
		assertEquals(HttpStatus.OK, resp.getStatusCode());

		final ImmovableProperty immo = resp.getBody();
		assertNotNull(immo);
		assertEquals(CondominiumOwnership.class, immo.getClass());

		final CondominiumOwnership ppe = (CondominiumOwnership) immo;
		assertEquals("CH796577806563", ppe.getEgrid());
		assertShare(10, 1000, ppe.getShare());

		final List<LandRight> landRightsTo = immo.getLandRights();
		assertNotNull(landRightsTo);
		assertEquals(1, landRightsTo.size());

		// le droit de propriété de la PM vers cette PPE
		final LandOwnershipRight landRightTo0 = (LandOwnershipRight) landRightsTo.get(0);
		assertNotNull(landRightTo0);
		assertLandOwnershipRight(RegDate.get(1981, 3, 6), null, "Transfert", null, OwnershipType.SOLE_OWNERSHIP, 1, 1, 21550, null, noImmo, landRightTo0);

		final List<LandRight> landRightsFrom = immo.getLandRightsFrom();
		assertNotNull(landRightsFrom);
		assertEquals(1, landRightsFrom.size());

		// le droit de propriété de la PPE sur son bien-fonds
		final LandOwnershipRight landRightFrom0 = (LandOwnershipRight) landRightsFrom.get(0);
		assertLandOwnershipRightImmovableProp(RegDate.get(1996, 4, 16), null, "Constitution de PPE", null, OwnershipType.CONDOMINIUM_OWNERSHIP, 10, 1000, noImmo, 264310664, landRightFrom0);
	}

	@Test
	public void testGetBuilding() throws Exception {

		final int buildingId = 266023444;

		final ResponseEntity<Building> resp = get(Building.class, MediaType.APPLICATION_XML, "/landRegistry/building/{id}?user=zaizzt/22", Collections.singletonMap("id", buildingId));
		assertNotNull(resp);
		assertEquals(HttpStatus.OK, resp.getStatusCode());

		final Building building = resp.getBody();
		assertNotNull(building);
		assertEquals(buildingId, building.getId());

		final List<BuildingDescription> descriptions = building.getDescriptions();
		assertEquals(1, descriptions.size());
		assertDescription(RegDate.get(2016, 9, 13), null, "Habitation", null, descriptions.get(0));

		final List<BuildingSetting> settings = building.getSettings();
		assertEquals(1, settings.size());
		assertSetting(RegDate.get(2016, 9, 13), null, 150, 264310664, buildingId, settings.get(0));
	}

	@Test
	public void testGetBuildings() throws Exception {

		final int buildingId1 = 266023444;
		final int buildingId2 = 348934893;

		final Map<String, Integer> params = new HashMap<>();
		params.put("buildingId1", buildingId1);
		params.put("buildingId2", buildingId2);

		// on demande les deux bâtiments qui existent dans la DB
		final ResponseEntity<BuildingList> resp = get(BuildingList.class,
		                                              MediaType.APPLICATION_XML,
		                                              "/landRegistry/buildings?buildingId={buildingId1}&buildingId={buildingId2}&user=zaizzt/22",
		                                              params);
		assertNotNull(resp);
		assertEquals(HttpStatus.OK, resp.getStatusCode());

		// on vérifie qu'on a reçu les bâtiments
		final BuildingList list = resp.getBody();
		assertNotNull(list);

		final List<BuildingEntry> entries = list.getEntries();
		assertNotNull(entries);
		assertEquals(2, entries.size());
		assertEntry(buildingId1, entries.get(0));
		assertEntry(buildingId2, entries.get(1));
	}

	@Test
	public void testGetCommunityOfOwner() throws Exception {

		final int communityId = 264822986;

		final ResponseEntity<CommunityOfOwners> resp = get(CommunityOfOwners.class, MediaType.APPLICATION_XML, "/landRegistry/communityOfOwners/{id}?user=zaizzt/22", Collections.singletonMap("id", communityId));
		assertNotNull(resp);
		assertEquals(HttpStatus.OK, resp.getStatusCode());

		final CommunityOfOwners community = resp.getBody();
		assertNotNull(community);
		assertEquals(communityId, community.getId());
		assertEquals(CommunityOfOwnersType.COMMUNITY_OF_HEIRS, community.getType());

		final List<RightHolder> members = community.getMembers();
		assertNotNull(members);
		assertEquals(4, members.size());
		// [SIFISC-23747] les membres de la communauté doivent être triés
		assertRightHolderParty(21550, members.get(0));      // Entreprise "BIGS Architecture et Entreprise Générale S.A."
		assertRightHolderParty(10035633, members.get(1));   // Personne physique "Elisabeth Astrid Mary"
		assertRightHolderNaturalPerson("Raymonde", "Grandjean", null, members.get(2));
		assertRightHolderNaturalPerson(null, "Berard Renée", null, members.get(3));

		// SIFISC-24457
		final LandOwnershipRight landRight = community.getLandRight();
		assertNotNull(landRight);
		assertLandOwnershipRight(RegDate.get(1981, 3, 6), null, null, null, OwnershipType.SIMPLE_CO_OWNERSHIP, 1, 4, 264822986L, landRight);
	}

	@Test
	public void testGetCommunitiesOfOwner() throws Exception {

		final int communityId = 264822986;
		final int communauteInconnueId = -1;

		final Map<String, Integer> params = new HashMap<>();
		params.put("communityId", communityId);
		params.put("communauteInconnueId", communauteInconnueId);

		final ResponseEntity<CommunityOfOwnersList> resp = get(CommunityOfOwnersList.class,
		                                                       MediaType.APPLICATION_XML,
		                                                       "/landRegistry/communitiesOfOwners?communityId={communityId}&communityId={communauteInconnueId}&user=zaizzt/22",
		                                                       params);
		assertNotNull(resp);
		assertEquals(HttpStatus.OK, resp.getStatusCode());

		final CommunityOfOwnersList list = resp.getBody();
		assertNotNull(list);

		final List<CommunityOfOwnersEntry> entries = list.getEntries();
		assertNotNull(entries);
		assertEquals(2, entries.size());
		assertEntryNotFound(communauteInconnueId, entries.get(0));
		assertEntry(communityId, entries.get(1));
	}

	private static void assertRightHolderNaturalPerson(String firstName, String lastName, RegDate dateOfBirth, RightHolder owner) {
		final NaturalPersonIdentity identity = (NaturalPersonIdentity) owner.getIdentity();
		assertNotNull(identity);
		assertEquals(firstName, identity.getFirstName());
		assertEquals(lastName, identity.getLastName());
		assertEquals(dateOfBirth, DataHelper.xmlToCore(identity.getDateOfBirth()));
	}

	private static void assertRightHolderParty(long id, RightHolder owner0) {
		assertEquals(Integer.valueOf((int) id), owner0.getTaxPayerNumber());
	}

	private static void assertDescription(RegDate dateFrom, RegDate dateTo, String type, Object area, BuildingDescription description) {
		assertNotNull(description);
		assertDate(dateFrom, description.getDateFrom());
		assertDate(dateTo, description.getDateTo());
		assertEquals(type, description.getType());
		assertEquals(area, description.getArea());
	}

	private static void assertSetting(RegDate dateFrom, RegDate dateTo, Integer area, int immoId, int buildingId, BuildingSetting setting) {
		assertNotNull(setting);
		assertDate(dateFrom, setting.getDateFrom());
		assertDate(dateTo, setting.getDateTo());
		assertEquals(area, setting.getArea());
		assertEquals(immoId, setting.getImmovablePropertyId());
		assertEquals(buildingId, setting.getBuildingId());
	}

	private static void assertTaxEstimation(RegDate dateFrom, RegDate dateTo, Long amount, String reference, boolean isInReview, TaxEstimate taxEstimate) {
		assertNotNull(taxEstimate);
		assertDate(dateFrom, taxEstimate.getDateFrom());
		assertDate(dateTo, taxEstimate.getDateTo());
		assertEquals(amount, taxEstimate.getAmount());
		assertEquals(reference, taxEstimate.getReference());
		assertEquals(isInReview, taxEstimate.isInReview());
	}

	private static void assertTotalArea(RegDate dateFrom, RegDate dateTo, int area, TotalArea totalArea) {
		assertNotNull(totalArea);
		assertDate(dateFrom, totalArea.getDateFrom());
		assertDate(dateTo, totalArea.getDateTo());
		assertEquals(area, totalArea.getArea());
	}

	private static void assertGroundArea(RegDate dateFrom, RegDate dateTo, int area, String type, GroundArea groundArea) {
		assertNotNull(groundArea);
		assertDate(dateFrom, groundArea.getDateFrom());
		assertDate(dateTo, groundArea.getDateTo());
		assertEquals(area, groundArea.getArea());
		assertEquals(type, groundArea.getType());
	}

	private static void assertLocation(Location location, RegDate dateFrom, RegDate dateTo, int parcelNumber, Integer index1, Integer index2, Integer index3, int municipalityFsoId) {
		assertNotNull(location);
		assertDate(dateFrom, location.getDateFrom());
		assertDate(dateTo, location.getDateTo());
		assertEquals(parcelNumber, location.getParcelNumber());
		assertEquals(index1, location.getIndex1());
		assertEquals(index2, location.getIndex2());
		assertEquals(index3, location.getIndex3());
		assertEquals(municipalityFsoId, location.getMunicipalityFsoId());
	}

	public static void assertDate(@Nullable RegDate expected, @Nullable Date actual) {
		if (expected == null) {
			assertNull(actual);
		}
		else {
			assertNotNull(actual);
			assertEquals(expected.year(), actual.getYear());
			assertEquals(expected.month(), actual.getMonth());
			assertEquals(expected.day(), actual.getDay());
		}
	}

	private static void assertShare(int numerator, int denominator, Share share) {
		assertNotNull(share);
		assertEquals(numerator, share.getNumerator());
		assertEquals(denominator, share.getDenominator());
	}

	private static void assertLandOwnershipRight(RegDate dateFrom, RegDate dateTo, String startReason, Object endReason, OwnershipType type, int numerator, int denominator, String firstName, String lastName, RegDate dateOfBirth,
	                                             Long communityId, LandOwnershipRight landRight) {
		assertNotNull(landRight);
		assertEquals(communityId, landRight.getCommunityId());
		assertEquals(type, landRight.getType());
		assertShare(numerator, denominator, landRight.getShare());
		assertDate(dateFrom, landRight.getDateFrom());
		assertDate(dateTo, landRight.getDateTo());
		assertEquals(startReason, landRight.getStartReason());
		assertEquals(endReason, landRight.getEndReason());
		assertRightHolderNaturalPerson(firstName, lastName, dateOfBirth, landRight.getRightHolder());
	}

	private static void assertLandOwnershipRight(RegDate dateFrom, RegDate dateTo, String startReason, Object endReason, OwnershipType type, int numerator, int denominator, long communityId, LandOwnershipRight landRight) {
		assertNotNull(landRight);
		assertNull(landRight.getCommunityId());
		assertEquals(type, landRight.getType());
		assertShare(numerator, denominator, landRight.getShare());
		assertDate(dateFrom, landRight.getDateFrom());
		assertDate(dateTo, landRight.getDateTo());
		assertEquals(startReason, landRight.getStartReason());
		assertEquals(endReason, landRight.getEndReason());
		assertEquals(Long.valueOf(communityId), landRight.getRightHolder().getCommunityId());
	}

	private static void assertLandOwnershipRightImmovableProp(RegDate dateFrom, RegDate dateTo, String startReason, Object endReason, OwnershipType type, int numerator, int denominator, long dominantImmoProp, long servantImmoProp,
	                                                          LandOwnershipRight landRight) {
		assertNotNull(landRight);
		assertNull(landRight.getCommunityId());
		assertEquals(type, landRight.getType());
		assertShare(numerator, denominator, landRight.getShare());
		assertDate(dateFrom, landRight.getDateFrom());
		assertDate(dateTo, landRight.getDateTo());
		assertEquals(startReason, landRight.getStartReason());
		assertEquals(endReason, landRight.getEndReason());
		assertEquals(Long.valueOf(dominantImmoProp), landRight.getRightHolder().getImmovablePropertyId());
		assertEquals(servantImmoProp, landRight.getImmovablePropertyId());
	}

	private static void assertLandOwnershipRight(RegDate dateFrom, RegDate dateTo, String startReason, Object endReason, OwnershipType type, int numerator, int denominator, long taxPayerNumber,
	                                             Long communityId, long immoPropId, LandOwnershipRight landRight) {
		assertNotNull(landRight);
		assertEquals(communityId, landRight.getCommunityId());
		assertEquals(type, landRight.getType());
		assertShare(numerator, denominator, landRight.getShare());
		assertDate(dateFrom, landRight.getDateFrom());
		assertDate(dateTo, landRight.getDateTo());
		assertEquals(startReason, landRight.getStartReason());
		assertEquals(endReason, landRight.getEndReason());
		assertRightHolderParty(taxPayerNumber, landRight.getRightHolder());
		assertEquals(immoPropId, landRight.getImmovablePropertyId());
	}

	private static void assertVirutalLandOwnershipRight(VirtualLandOwnershipRight landRight, Long communityId, RegDate dateFrom, RegDate dateTo, String startReason, String endReason, int taxPayerNumber, long immovablePropId) {
		assertNotNull(landRight);
		assertEquals(communityId, landRight.getCommunityId());
		assertDate(dateFrom, landRight.getDateFrom());
		assertDate(dateTo, landRight.getDateTo());
		assertEquals(startReason, landRight.getStartReason());
		assertEquals(endReason, landRight.getEndReason());
		assertEquals(Integer.valueOf(taxPayerNumber), landRight.getRightHolder().getTaxPayerNumber());
		assertEquals(immovablePropId, landRight.getImmovablePropertyId());
	}

	private static void assertVirtualInheritedRight(int heritierId, int decedeId, RegDate dateFrom, RegDate dateTo, String startReason, Object endReason, int immovablePropId, boolean implicitCommunity, VirtualInheritedLandRight right) {
		assertDate(dateFrom, right.getDateFrom());
		assertDate(dateTo, right.getDateTo());
		assertEquals(startReason, right.getStartReason());
		assertEquals(endReason, right.getEndReason());
		assertEquals(immovablePropId, right.getImmovablePropertyId());
		assertEquals(decedeId, right.getInheritedFromId());
		assertRightHolderParty(heritierId, right.getRightHolder());
		assertEquals(implicitCommunity, right.isImplicitCommunity());
	}

	private static void assertUsufructRight(RegDate dateFrom, RegDate dateTo, String startReason, String endReason, LandRight right) {
		assertNotNull(right);
		assertTrue(right instanceof UsufructRight);
		assertDate(dateFrom, right.getDateFrom());
		assertDate(dateTo, right.getDateTo());
		assertEquals(startReason, right.getStartReason());
		assertEquals(endReason, right.getEndReason());
	}

	private static void assertHousingRight(RegDate dateFrom, RegDate dateTo, String startReason, String endReason, LandRight right) {
		assertNotNull(right);
		assertTrue(right instanceof HousingRight);
		assertDate(dateFrom, right.getDateFrom());
		assertDate(dateTo, right.getDateTo());
		assertEquals(startReason, right.getStartReason());
		assertEquals(endReason, right.getEndReason());
	}

	private static void assertAcquisitionReason(RegDate date, String r, int officeNumber, String caseNumber, AcquisitionReason reason) {
		assertNotNull(reason);
		assertDate(date, reason.getDate());
		assertEquals(r, reason.getReason());
		assertCaseIdentifier(officeNumber, caseNumber, reason.getCaseIdentifier());
	}

	private static void assertCaseIdentifier(int officeNumber, String caseNumber, CaseIdentifier caseIdentifier) {
		assertNotNull(caseIdentifier);
		assertEquals(officeNumber, caseIdentifier.getOfficeNumber());
		assertEquals(caseNumber, caseIdentifier.getCaseNumberText());
	}

	private static void assertShare(RegDate dateFrom, RegDate dateTo, int numerator, int denominator, DatedShare share) {
		assertNotNull(share);
		assertDate(dateFrom, share.getDateFrom());
		assertDate(dateTo, share.getDateTo());
		assertEquals(numerator, share.getNumerator());
		assertEquals(denominator, share.getDenominator());
	}

	private static void assertEntry(int immoId, String egrid, ImmovablePropertyEntry entry) {
		assertNotNull(entry);
		assertEquals(immoId, entry.getImmovablePropertyId());
		final ImmovableProperty immo = entry.getImmovableProperty();
		assertNotNull(immo);
		assertEquals(immoId, immo.getId());
		assertEquals(egrid, immo.getEgrid());
	}

	private static void assertEntry(int buildingId, BuildingEntry entry) {
		assertNotNull(entry);
		assertEquals(buildingId, entry.getBuildingId());
		final Building immo = entry.getBuilding();
		assertNotNull(immo);
		assertEquals(buildingId, immo.getId());
	}

	private static void assertEntry(int communityId, CommunityOfOwnersEntry entry) {
		assertNotNull(entry);
		assertEquals(communityId, entry.getCommunityOfOwnersId());
		final CommunityOfOwners community = entry.getCommunityOfOwners();
		assertNotNull(community);
		assertEquals(communityId, community.getId());
	}

	private static void assertEntryNotFound(int communityId, CommunityOfOwnersEntry entry) {
		assertNotNull(entry);
		assertEquals(communityId, entry.getCommunityOfOwnersId());
		assertNull(entry.getCommunityOfOwners());
		assertEquals(ErrorType.BUSINESS, entry.getError().getType());
		assertEquals("La communauté n°[" + communityId + "] n'existe pas.", entry.getError().getErrorMessage());
	}
}
