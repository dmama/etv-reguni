package ch.vd.uniregctb.registrefoncier.dataimport.helper;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import ch.vd.capitastra.rechteregister.BelastetesGrundstueck;
import ch.vd.capitastra.rechteregister.Beleg;
import ch.vd.capitastra.rechteregister.BerechtigtePerson;
import ch.vd.capitastra.rechteregister.CapiCode;
import ch.vd.capitastra.rechteregister.Dienstbarkeit;
import ch.vd.capitastra.rechteregister.DienstbarkeitDiscrete;
import ch.vd.capitastra.rechteregister.NatuerlichePersonGb;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitHabitationRF;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.IdentifiantDroitRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.ServitudeRF;
import ch.vd.uniregctb.registrefoncier.UsufruitRF;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ServitudesRFHelperTest {

	@Test
	public void testDataEqualsListNullity() throws Exception {

		assertTrue(ServitudesRFHelper.dataEquals((Set<DroitRF>) null, null));
		assertTrue(ServitudesRFHelper.dataEquals(Collections.emptySet(), null));
		assertTrue(ServitudesRFHelper.dataEquals(null, Collections.emptyList()));
		assertTrue(ServitudesRFHelper.dataEquals(Collections.emptySet(), Collections.emptyList()));

		assertFalse(ServitudesRFHelper.dataEquals(null, Collections.singletonList(new DienstbarkeitDiscrete())));
		assertFalse(ServitudesRFHelper.dataEquals(Collections.singleton(new UsufruitRF()), null));
	}

	@Test
	public void testDataEqualsListDifferentSizes() throws Exception {
		assertFalse(ServitudesRFHelper.dataEquals(Collections.singleton(new UsufruitRF()),
		                                          Arrays.asList(new DienstbarkeitDiscrete(), new DienstbarkeitDiscrete())));
	}

	@Test
	public void testDataEqualsList() throws Exception {

		final ImmeubleRF immeuble1 = new BienFondRF();
		immeuble1.setIdRF("_1f109152380ffd8901380ffe15bb729c");

		final ImmeubleRF immeuble2 = new BienFondRF();
		immeuble2.setIdRF("_1f109152380ffd8901380ffe090827e1");

		final PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
		pp1.setIdRF("_1f109152380ffd8901380ffdabcc2441");

		final PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
		pp2.setIdRF("_1f109152380ffd8901380ffda8131c65");

		final UsufruitRF servitude1 = newUsufruit(immeuble1, pp1, new IdentifiantDroitRF(8, 2005, 699), RegDate.get(2002, 9, 2), null,
		                                          "1f109152380ffd8901380ffed6694392", new IdentifiantAffaireRF(8, 2002, 392, null));

		final UsufruitRF servitude2 = newUsufruit(immeuble2, pp2, new IdentifiantDroitRF(8, 2006, 361), RegDate.get(2006, 6, 30), null,
		                                          "1f109152380ffd8901380ffefad54360", new IdentifiantAffaireRF(8, 2006, 285, 0));


		final BelastetesGrundstueck grundstueck1 = new BelastetesGrundstueck("_1f109152380ffd8901380ffe15bb729c", null, null);
		final NatuerlichePersonGb natuerlichePerson1 = newNatuerlichePersonGb("Roger", "Gaillard", "_1f109152380ffd8901380ffdabcc2441");
		final Dienstbarkeit dienstbarkeit1 = newDienstbarkeit("1f109152380ffd8901380ffed6694392", "_1f109152380ffd8901380ffed6694392", 2005, 699, 8, "Usufruit", "2002/392", null, RegDate.get(2002, 9, 2), null);
		final DienstbarkeitDiscrete discreteDienstbarkeit1 = newDienstbarkeitDiscrete(grundstueck1, natuerlichePerson1, dienstbarkeit1, null);

		final BelastetesGrundstueck grundstueck2 = new BelastetesGrundstueck("_1f109152380ffd8901380ffe090827e1", null, null);
		final NatuerlichePersonGb natuerlichePerson2 = newNatuerlichePersonGb("Anne-Lise", "Lassueur", "_1f109152380ffd8901380ffda8131c65");
		final Dienstbarkeit dienstbarkeit2 = newDienstbarkeit("1f109152380ffd8901380ffefad54360", "_1f109152380ffd8901380ffefad54360", 2006, 361, 8, "Usufruit", null, new Beleg(8, 2006, 285, 0), RegDate.get(2006, 6, 30), null);
		final DienstbarkeitDiscrete discreteDienstbarkeit2 = newDienstbarkeitDiscrete(grundstueck2, natuerlichePerson2, dienstbarkeit2, null);

		final Set<DroitRF> servitudes = new HashSet<>(Arrays.<DroitRF>asList(servitude1, servitude2));
		final List<DienstbarkeitDiscrete> dienstbarkeits = Arrays.asList(discreteDienstbarkeit1, discreteDienstbarkeit2);

		assertTrue(ServitudesRFHelper.dataEquals(servitudes, dienstbarkeits));
	}

	@Test
	public void testEqualsUsufruit() throws Exception {

		final ImmeubleRF immeuble = new BienFondRF();
		immeuble.setIdRF("_1f109152380ffd8901380ffe15bb729c");

		final PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
		pp.setIdRF("_1f109152380ffd8901380ffdabcc2441");

		final UsufruitRF usufruit = newUsufruit(immeuble, pp, new IdentifiantDroitRF(8, 2005, 699), RegDate.get(2002, 9, 2), null,
		                                        "1f109152380ffd8901380ffed6694392", new IdentifiantAffaireRF(8, 2002, 392, null));

		final BelastetesGrundstueck grundstueck = new BelastetesGrundstueck("_1f109152380ffd8901380ffe15bb729c", null, null);
		final NatuerlichePersonGb natuerlichePerson = newNatuerlichePersonGb("Roger", "Gaillard", "_1f109152380ffd8901380ffdabcc2441");
		final Dienstbarkeit dienstbarkeit = newDienstbarkeit("1f109152380ffd8901380ffed6694392", "_1f109152380ffd8901380ffed6694392", 2005, 699, 8, "Usufruit", "2002/392", null, RegDate.get(2002, 9, 2), null);
		final DienstbarkeitDiscrete discreteDienstbarkeit = newDienstbarkeitDiscrete(grundstueck, natuerlichePerson, dienstbarkeit, null);

		assertTrue(ServitudesRFHelper.dataEquals(usufruit, discreteDienstbarkeit));
	}

	@Test
	public void testEqualsDroitHabitation() throws Exception {

		final ImmeubleRF immeuble = new BienFondRF();
		immeuble.setIdRF("_1f109152380ffd8901380ffe15bb729c");

		final PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
		pp.setIdRF("_1f109152380ffd8901380ffdabcc2441");

		final DroitHabitationRF droitHabitation = newDroitHabitation(immeuble, pp, new IdentifiantDroitRF(8, 2005, 699), RegDate.get(2002, 9, 2), null,
		                                                             "1f109152380ffd8901380ffed6694392", new IdentifiantAffaireRF(8, 2002, 392, null));

		final BelastetesGrundstueck grundstueck = new BelastetesGrundstueck("_1f109152380ffd8901380ffe15bb729c", null, null);
		final NatuerlichePersonGb natuerlichePerson = newNatuerlichePersonGb("Roger", "Gaillard", "_1f109152380ffd8901380ffdabcc2441");
		final Dienstbarkeit dienstbarkeit = newDienstbarkeit("1f109152380ffd8901380ffed6694392", "_1f109152380ffd8901380ffed6694392", 2005, 699, 8, "Droit d'habitation", "2002/392", null, RegDate.get(2002, 9, 2), null);
		final DienstbarkeitDiscrete discreteDienstbarkeit = newDienstbarkeitDiscrete(grundstueck, natuerlichePerson, dienstbarkeit, null);

		assertTrue(ServitudesRFHelper.dataEquals(droitHabitation, discreteDienstbarkeit));
	}

	@Test
	public void testNewServitudeRFUsufruit() throws Exception {

		final ImmeubleRF immeuble = new BienFondRF();
		immeuble.setIdRF("_1f109152380ffd8901380ffe15bb729c");

		final PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
		pp1.setIdRF("_1f109152380ffd8901380ffdabcc2441");

		final BelastetesGrundstueck grundstueck = new BelastetesGrundstueck("_1f109152380ffd8901380ffe15bb729c", null, null);
		final NatuerlichePersonGb natuerlichePerson1 = newNatuerlichePersonGb("Roger", "Gaillard", "_1f109152380ffd8901380ffdabcc2441");
		final Dienstbarkeit dienstbarkeit = newDienstbarkeit("1f109152380ffd8901380ffed6694392", "_1f109152380ffd8901380ffed6694392", 2005, 699, 8, "Usufruit", "2002/392", null, RegDate.get(2002, 9, 2), null);
		final DienstbarkeitDiscrete discreteDienstbarkeit = newDienstbarkeitDiscrete(grundstueck, natuerlichePerson1, dienstbarkeit, null);

		final ServitudeRF servitude = ServitudesRFHelper.newServitudeRF(discreteDienstbarkeit, (id) -> pp1, (id) -> null, (id) -> immeuble);
		assertNotNull(servitude);
		assertTrue(servitude instanceof UsufruitRF);
		assertEquals("1f109152380ffd8901380ffed6694392", servitude.getMasterIdRF());
		assertEquals(RegDate.get(2002, 9, 2), servitude.getDateDebutMetier());
		assertNull(servitude.getDateFinMetier());
		assertNull(servitude.getMotifDebut());
		assertNull(servitude.getMotifFin());
		assertIdentifiantDroit(2005, 699, 8, servitude.getIdentifiantDroit());
		assertNumeroAffaire(8, "2002/392", servitude.getNumeroAffaire());
		assertNull(servitude.getCommunaute());
		assertSame(pp1, servitude.getAyantDroit());
		assertSame(immeuble, servitude.getImmeuble());
	}

	@Test
	public void testNewServitudeRFDroitHabitation() throws Exception {

		final ImmeubleRF immeuble = new BienFondRF();
		immeuble.setIdRF("_1f109152380ffd8901380ffe15bb729c");

		final PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
		pp1.setIdRF("_1f109152380ffd8901380ffdabcc2441");

		final BelastetesGrundstueck grundstueck = new BelastetesGrundstueck("_1f109152380ffd8901380ffe15bb729c", null, null);
		final NatuerlichePersonGb natuerlichePerson1 = newNatuerlichePersonGb("Roger", "Gaillard", "_1f109152380ffd8901380ffdabcc2441");
		final Dienstbarkeit dienstbarkeit = newDienstbarkeit("1f109152380ffd8901380ffed6694392", "_1f109152380ffd8901380ffed6694392", 2005, 699, 8, "Droit d'habitation", "2002/392", null, RegDate.get(2002, 9, 2), null);
		final DienstbarkeitDiscrete discreteDienstbarkeit = newDienstbarkeitDiscrete(grundstueck, natuerlichePerson1, dienstbarkeit, null);

		final ServitudeRF servitude = ServitudesRFHelper.newServitudeRF(discreteDienstbarkeit, (id) -> pp1, (id) -> null, (id) -> immeuble);
		assertNotNull(servitude);
		assertTrue(servitude instanceof DroitHabitationRF);
		assertEquals("1f109152380ffd8901380ffed6694392", servitude.getMasterIdRF());
		assertEquals(RegDate.get(2002, 9, 2), servitude.getDateDebutMetier());
		assertNull(servitude.getDateFinMetier());
		assertNull(servitude.getMotifDebut());
		assertNull(servitude.getMotifFin());
		assertIdentifiantDroit(2005, 699, 8, servitude.getIdentifiantDroit());
		assertNumeroAffaire(8, "2002/392", servitude.getNumeroAffaire());
		assertNull(servitude.getCommunaute());
		assertSame(pp1, servitude.getAyantDroit());
		assertSame(immeuble, servitude.getImmeuble());
	}

	@Test
	public void testNewServitudeRFUsufruitAvecCommunaute() throws Exception {

		final ImmeubleRF immeuble = new BienFondRF();
		immeuble.setIdRF("_1f109152380ffd8901380ffe15bb729c");

		final PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
		pp1.setIdRF("_1f109152380ffd8901380ffdabcc2441");

		final CommunauteRF communaute = new CommunauteRF();
		communaute.setIdRF("_1f109152380ffd8901380ffed6694392");

		final BelastetesGrundstueck grundstueck = new BelastetesGrundstueck("_1f109152380ffd8901380ffe15bb729c", null, null);
		final NatuerlichePersonGb natuerlichePerson1 = newNatuerlichePersonGb("Roger", "Gaillard", "_1f109152380ffd8901380ffdabcc2441");
		final NatuerlichePersonGb natuerlichePerson2 = newNatuerlichePersonGb("Anne-Lise", "Lassueur", "_1f109152380ffd8901380ffda8131c65");
		final Dienstbarkeit dienstbarkeit = newDienstbarkeit("1f109152380ffd8901380ffed6694392", "_1f109152380ffd8901380ffed6694392", 2005, 699, 8, "Usufruit", "2002/392", null, RegDate.get(2002, 9, 2), null);
		final DienstbarkeitDiscrete discreteDienstbarkeit = newDienstbarkeitDiscrete(grundstueck, natuerlichePerson1, dienstbarkeit, Arrays.asList(natuerlichePerson1, natuerlichePerson2));

		final ServitudeRF servitude = ServitudesRFHelper.newServitudeRF(discreteDienstbarkeit, (id) -> pp1, (id) -> communaute, (id) -> immeuble);
		assertNotNull(servitude);
		assertTrue(servitude instanceof UsufruitRF);
		assertEquals("1f109152380ffd8901380ffed6694392", servitude.getMasterIdRF());
		assertEquals(RegDate.get(2002, 9, 2), servitude.getDateDebutMetier());
		assertNull(servitude.getDateFinMetier());
		assertNull(servitude.getMotifDebut());
		assertNull(servitude.getMotifFin());
		assertIdentifiantDroit(2005, 699, 8, servitude.getIdentifiantDroit());
		assertNumeroAffaire(8, "2002/392", servitude.getNumeroAffaire());
		assertSame(communaute, servitude.getCommunaute());
		assertSame(pp1, servitude.getAyantDroit());
		assertSame(immeuble, servitude.getImmeuble());
	}

	private static void assertNumeroAffaire(int numeroOffice, @Nullable String annee, IdentifiantAffaireRF numeroAffaire) {
		assertNotNull(numeroAffaire);
		assertEquals(numeroOffice, numeroAffaire.getNumeroOffice());
		assertEquals(annee, numeroAffaire.getNumeroAffaire());
	}

	private static void assertIdentifiantDroit(int anneeDroit, int numeroDroit, int numeroOffice, IdentifiantDroitRF identifiantDroit) {
		assertNotNull(identifiantDroit);
		assertEquals(anneeDroit, identifiantDroit.getAnnee());
		assertEquals(numeroDroit, identifiantDroit.getNumero());
		assertEquals(numeroOffice, identifiantDroit.getNumeroOffice());
	}

	@NotNull
	public static DienstbarkeitDiscrete newDienstbarkeitDiscrete(BelastetesGrundstueck grundstueck, NatuerlichePersonGb natuerlichePersonGb, Dienstbarkeit dienstbarkeit, Collection<NatuerlichePersonGb> gemeinchaft) {
		final DienstbarkeitDiscrete d = new DienstbarkeitDiscrete();
		d.setBelastetesGrundstueck(grundstueck);
		d.setBerechtigtePerson(new BerechtigtePerson(natuerlichePersonGb, null, null, null));
		d.setDienstbarkeit(dienstbarkeit);
		if (gemeinchaft != null) {
			gemeinchaft.stream()
					.map(p -> new BerechtigtePerson(p, null, null, null))
					.forEach(b -> d.getGemeinschaft().add(b));
		}
		return d;
	}

	@NotNull
	public static NatuerlichePersonGb newNatuerlichePersonGb(String prenom, String nom, String personstammIDREF) {
		final NatuerlichePersonGb natuerlichePersonGb = new NatuerlichePersonGb();
		natuerlichePersonGb.setVorname(prenom);
		natuerlichePersonGb.setName(nom);
		natuerlichePersonGb.setPersonstammIDREF(personstammIDREF);
		return natuerlichePersonGb;
	}

	public static Dienstbarkeit newDienstbarkeit(String masterID, String standardRechtID, int anneeDroit, int numeroDroit, int numeroOffice, String type, String numeroAffaireTexte, Beleg numeroAffaire, RegDate dateDebut, RegDate dateFin) {
		final Dienstbarkeit d = new Dienstbarkeit();
		d.setMasterID(masterID);
		d.setStandardRechtID(standardRechtID);
		d.setRechtEintragJahrID(anneeDroit);
		d.setRechtEintragNummerID(numeroDroit);
		d.setAmtNummer(numeroOffice);
		d.setStichwort(new CapiCode(null, type));
		d.setBelegAlt(numeroAffaireTexte);
		d.setBeleg(numeroAffaire);
		d.setBeginDatum(dateDebut);
		d.setAblaufDatum(dateFin);
		return d;
	}

	@NotNull
	private static UsufruitRF newUsufruit(ImmeubleRF immeuble1, PersonnePhysiqueRF pp1, IdentifiantDroitRF identifiantDroit, RegDate dateDebutOfficielle,
	                                      @Nullable RegDate dateFinOfficielle, String masterIdRF, IdentifiantAffaireRF numeroAffaire) {
		final UsufruitRF servitude1 = new UsufruitRF();
		servitude1.setIdentifiantDroit(identifiantDroit);
		servitude1.setDateDebutMetier(dateDebutOfficielle);
		servitude1.setDateFinMetier(dateFinOfficielle);
		servitude1.setMotifDebut(null);
		servitude1.setMotifFin(null);
		servitude1.setImmeuble(immeuble1);
		servitude1.setMasterIdRF(masterIdRF);
		servitude1.setNumeroAffaire(numeroAffaire);
		servitude1.setAyantDroit(pp1);
		return servitude1;
	}

	@NotNull
	private static DroitHabitationRF newDroitHabitation(ImmeubleRF immeuble1, PersonnePhysiqueRF pp1, IdentifiantDroitRF identifiantDroit, RegDate dateDebutOfficielle,
	                                                    @Nullable RegDate dateFinOfficielle, String masterIdRF, IdentifiantAffaireRF numeroAffaire) {
		final DroitHabitationRF servitude1 = new DroitHabitationRF();
		servitude1.setIdentifiantDroit(identifiantDroit);
		servitude1.setDateDebutMetier(dateDebutOfficielle);
		servitude1.setDateFinMetier(dateFinOfficielle);
		servitude1.setMotifDebut(null);
		servitude1.setMotifFin(null);
		servitude1.setImmeuble(immeuble1);
		servitude1.setMasterIdRF(masterIdRF);
		servitude1.setNumeroAffaire(numeroAffaire);
		servitude1.setAyantDroit(pp1);
		return servitude1;
	}
}