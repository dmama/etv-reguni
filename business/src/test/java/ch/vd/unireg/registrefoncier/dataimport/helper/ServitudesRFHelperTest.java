package ch.vd.unireg.registrefoncier.dataimport.helper;

import java.util.Arrays;
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
import ch.vd.capitastra.rechteregister.LastRechtGruppe;
import ch.vd.capitastra.rechteregister.NatuerlichePersonGb;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.registrefoncier.BeneficeServitudeRF;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.ChargeServitudeRF;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.DroitHabitationRF;
import ch.vd.unireg.registrefoncier.IdentifiantAffaireRF;
import ch.vd.unireg.registrefoncier.IdentifiantDroitRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.PersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.ServitudeRF;
import ch.vd.unireg.registrefoncier.UsufruitRF;
import ch.vd.unireg.registrefoncier.dataimport.elements.servitude.DienstbarkeitExtendedElement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ServitudesRFHelperTest {

	@Test
	public void testDataEqualsListNullity() throws Exception {

		assertTrue(ServitudesRFHelper.dataEquals((Set<ServitudeRF>) null, null));
		assertTrue(ServitudesRFHelper.dataEquals(Collections.emptySet(), null));
		assertTrue(ServitudesRFHelper.dataEquals(null, Collections.emptyList()));
		assertTrue(ServitudesRFHelper.dataEquals(Collections.emptySet(), Collections.emptyList()));

		assertFalse(ServitudesRFHelper.dataEquals(null, Collections.singletonList(new DienstbarkeitExtendedElement())));
		assertFalse(ServitudesRFHelper.dataEquals(Collections.singleton(new UsufruitRF()), null));
	}

	@Test
	public void testDataEqualsListDifferentSizes() throws Exception {
		assertFalse(ServitudesRFHelper.dataEquals(Collections.singleton(new UsufruitRF()),
		                                          Arrays.asList(new DienstbarkeitExtendedElement(), new DienstbarkeitExtendedElement())));
	}

	@Test
	public void testDataEqualsList() throws Exception {

		final ImmeubleRF immeuble1 = new BienFondsRF();
		immeuble1.setIdRF("_1f109152380ffd8901380ffe15bb729c");

		final ImmeubleRF immeuble2 = new BienFondsRF();
		immeuble2.setIdRF("_1f109152380ffd8901380ffe090827e1");

		final PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
		pp1.setIdRF("_1f109152380ffd8901380ffdabcc2441");

		final PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
		pp2.setIdRF("_1f109152380ffd8901380ffda8131c65");

		final UsufruitRF servitude1 = newUsufruit(immeuble1, pp1, new IdentifiantDroitRF(8, 2005, 699), RegDate.get(2002, 9, 2), null,
		                                          "1f109152380ffd8901380ffed6694392",  "1f109152380ffd8901380ffed66943a2", new IdentifiantAffaireRF(8, 2002, 392, null));

		final UsufruitRF servitude2 = newUsufruit(immeuble2, pp2, new IdentifiantDroitRF(8, 2006, 361), RegDate.get(2006, 6, 30), null,
		                                          "1f109152380ffd8901380ffefad54360", "1f109152380ffd8901380ffefad64374", new IdentifiantAffaireRF(8, 2006, 285, 0));


		final BelastetesGrundstueck grundstueck1 = new BelastetesGrundstueck("_1f109152380ffd8901380ffe15bb729c", null, null);
		final NatuerlichePersonGb natuerlichePerson1 = newNatuerlichePersonGb("Roger", "Gaillard", "_1f109152380ffd8901380ffdabcc2441");
		final Dienstbarkeit dienstbarkeit1 = newDienstbarkeit("1f109152380ffd8901380ffed6694392", "1f109152380ffd8901380ffed66943a2", "_1f109152380ffd8901380ffed6694392", 2005, 699, 8, "Usufruit", "2002/392", null, RegDate.get(2002, 9, 2), null);
		final DienstbarkeitExtendedElement discreteDienstbarkeit1 = newDienstbarkeitExtended(grundstueck1, natuerlichePerson1, dienstbarkeit1);

		final BelastetesGrundstueck grundstueck2 = new BelastetesGrundstueck("_1f109152380ffd8901380ffe090827e1", null, null);
		final NatuerlichePersonGb natuerlichePerson2 = newNatuerlichePersonGb("Anne-Lise", "Lassueur", "_1f109152380ffd8901380ffda8131c65");
		final Dienstbarkeit dienstbarkeit2 = newDienstbarkeit("1f109152380ffd8901380ffefad54360", "1f109152380ffd8901380ffefad64374", "_1f109152380ffd8901380ffefad54360", 2006, 361, 8, "Usufruit", null, new Beleg(8, 2006, 285, 0), RegDate.get(2006, 6, 30), null);
		final DienstbarkeitExtendedElement discreteDienstbarkeit2 = newDienstbarkeitExtended(grundstueck2, natuerlichePerson2, dienstbarkeit2);

		final Set<ServitudeRF> servitudes = new HashSet<>(Arrays.<ServitudeRF>asList(servitude1, servitude2));
		final List<DienstbarkeitExtendedElement> dienstbarkeits = Arrays.asList(discreteDienstbarkeit1, discreteDienstbarkeit2);

		assertTrue(ServitudesRFHelper.dataEquals(servitudes, dienstbarkeits));
	}

	@Test
	public void testEqualsUsufruit() throws Exception {

		final ImmeubleRF immeuble = new BienFondsRF();
		immeuble.setIdRF("_1f109152380ffd8901380ffe15bb729c");

		final PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
		pp.setIdRF("_1f109152380ffd8901380ffdabcc2441");

		final UsufruitRF usufruit = newUsufruit(immeuble, pp, new IdentifiantDroitRF(8, 2005, 699), RegDate.get(2002, 9, 2), null,
		                                        "1f109152380ffd8901380ffed6694392",  "1f109152380ffd8901380ffed66943a2", new IdentifiantAffaireRF(8, 2002, 392, null));

		final BelastetesGrundstueck grundstueck = new BelastetesGrundstueck("_1f109152380ffd8901380ffe15bb729c", null, null);
		final NatuerlichePersonGb natuerlichePerson = newNatuerlichePersonGb("Roger", "Gaillard", "_1f109152380ffd8901380ffdabcc2441");
		final Dienstbarkeit dienstbarkeit = newDienstbarkeit("1f109152380ffd8901380ffed6694392", "1f109152380ffd8901380ffed66943a2", "_1f109152380ffd8901380ffed6694392", 2005, 699, 8, "Usufruit", "2002/392", null, RegDate.get(2002, 9, 2), null);
		final DienstbarkeitExtendedElement discreteDienstbarkeit = newDienstbarkeitExtended(grundstueck, natuerlichePerson, dienstbarkeit);

		assertTrue(ServitudesRFHelper.dataEquals(usufruit, discreteDienstbarkeit));
	}

	@Test
	public void testEqualsUsufruitMultipleImmeublesOK() throws Exception {

		final ImmeubleRF immeuble1 = new BienFondsRF();
		immeuble1.setIdRF("_1f109152380ffd8901380ffe15bb729c");

		final ImmeubleRF immeuble2 = new BienFondsRF();
		immeuble2.setIdRF("_1f109152380ffd8901380ffe15bb729d");

		final ImmeubleRF immeuble3 = new BienFondsRF();
		immeuble3.setIdRF("_1f109152380ffd8901380ffe15bb729e");

		final PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
		pp.setIdRF("_1f109152380ffd8901380ffdabcc2441");

		final UsufruitRF usufruit = newUsufruit(immeuble1, pp, new IdentifiantDroitRF(8, 2005, 699), RegDate.get(2002, 9, 2), null,
		                                        "1f109152380ffd8901380ffed6694392",  "1f109152380ffd8901380ffed66943a2", new IdentifiantAffaireRF(8, 2002, 392, null));
		usufruit.addCharge(new ChargeServitudeRF(null, null, usufruit, immeuble2));
		usufruit.addCharge(new ChargeServitudeRF(null, null, usufruit, immeuble3));

		final BelastetesGrundstueck grundstueck1 = new BelastetesGrundstueck("_1f109152380ffd8901380ffe15bb729c", null, null);
		final BelastetesGrundstueck grundstueck2 = new BelastetesGrundstueck("_1f109152380ffd8901380ffe15bb729d", null, null);
		final BelastetesGrundstueck grundstueck3 = new BelastetesGrundstueck("_1f109152380ffd8901380ffe15bb729e", null, null);
		final NatuerlichePersonGb natuerlichePerson = newNatuerlichePersonGb("Roger", "Gaillard", "_1f109152380ffd8901380ffdabcc2441");
		final Dienstbarkeit dienstbarkeit = newDienstbarkeit("1f109152380ffd8901380ffed6694392", "1f109152380ffd8901380ffed66943a2", "_1f109152380ffd8901380ffed6694392", 2005, 699, 8, "Usufruit", "2002/392", null, RegDate.get(2002, 9, 2), null);
		final DienstbarkeitExtendedElement discreteDienstbarkeit = newDienstbarkeitExtended(Arrays.asList(grundstueck1, grundstueck2, grundstueck3), Collections.singletonList(natuerlichePerson), dienstbarkeit);

		assertTrue(ServitudesRFHelper.dataEquals(usufruit, discreteDienstbarkeit));
	}

	@Test
	public void testEqualsUsufruitMultipleImmeublesKO() throws Exception {

		final ImmeubleRF immeuble1 = new BienFondsRF();
		immeuble1.setIdRF("_1f109152380ffd8901380ffe15bb729c");

		final ImmeubleRF immeuble2 = new BienFondsRF();
		immeuble2.setIdRF("_1f109152380ffd8901380ffe15bb729d");

		final ImmeubleRF immeuble3 = new BienFondsRF();
		immeuble3.setIdRF("_1f109152380ffd8901380ffe15bb729e");

		final PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
		pp.setIdRF("_1f109152380ffd8901380ffdabcc2441");

		final UsufruitRF usufruit = newUsufruit(immeuble1, pp, new IdentifiantDroitRF(8, 2005, 699), RegDate.get(2002, 9, 2), null,
		                                        "1f109152380ffd8901380ffed6694392",  "1f109152380ffd8901380ffed66943a2", new IdentifiantAffaireRF(8, 2002, 392, null));
		usufruit.addCharge(new ChargeServitudeRF(null, null, usufruit, immeuble2));
		usufruit.addCharge(new ChargeServitudeRF(null, null, usufruit, immeuble3));

		final BelastetesGrundstueck grundstueck1 = new BelastetesGrundstueck("_1f109152380ffd8901380ffe15bb729c", null, null);
		final BelastetesGrundstueck grundstueck2 = new BelastetesGrundstueck("_1f109152380ffd8901380ffe15bb729d", null, null);
		final BelastetesGrundstueck grundstueck3 = new BelastetesGrundstueck("urkkrur", null, null);
		final NatuerlichePersonGb natuerlichePerson = newNatuerlichePersonGb("Roger", "Gaillard", "_1f109152380ffd8901380ffdabcc2441");
		final Dienstbarkeit dienstbarkeit = newDienstbarkeit("1f109152380ffd8901380ffed6694392", "1f109152380ffd8901380ffed66943a2", "_1f109152380ffd8901380ffed6694392", 2005, 699, 8, "Usufruit", "2002/392", null, RegDate.get(2002, 9, 2), null);
		final DienstbarkeitExtendedElement discreteDienstbarkeit = newDienstbarkeitExtended(Arrays.asList(grundstueck1, grundstueck2, grundstueck3), Collections.singletonList(natuerlichePerson), dienstbarkeit);

		// un des trois immeubles n'est pas le même
		assertFalse(ServitudesRFHelper.dataEquals(usufruit, discreteDienstbarkeit));
	}

	@Test
	public void testEqualsUsufruitMultiplePersonnesOK() throws Exception {

		final ImmeubleRF immeuble = new BienFondsRF();
		immeuble.setIdRF("_1f109152380ffd8901380ffe15bb729c");

		final PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
		pp1.setIdRF("_1f109152380ffd8901380ffdabcc2441");

		final PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
		pp2.setIdRF("_1f109152380ffd8901380ffdabcc2442");

		final PersonnePhysiqueRF pp3 = new PersonnePhysiqueRF();
		pp3.setIdRF("_1f109152380ffd8901380ffdabcc2443");

		final UsufruitRF usufruit = newUsufruit(immeuble, pp1, new IdentifiantDroitRF(8, 2005, 699), RegDate.get(2002, 9, 2), null,
		                                        "1f109152380ffd8901380ffed6694392",  "1f109152380ffd8901380ffed66943a2", new IdentifiantAffaireRF(8, 2002, 392, null));
		usufruit.addBenefice(new BeneficeServitudeRF(null, null, usufruit, pp2));
		usufruit.addBenefice(new BeneficeServitudeRF(null, null, usufruit, pp3));

		final BelastetesGrundstueck grundstueck = new BelastetesGrundstueck("_1f109152380ffd8901380ffe15bb729c", null, null);
		final NatuerlichePersonGb natuerlichePerson1 = newNatuerlichePersonGb("Roger", "Gaillard", "_1f109152380ffd8901380ffdabcc2441");
		final NatuerlichePersonGb natuerlichePerson2 = newNatuerlichePersonGb("Robert", "Gaillard", "_1f109152380ffd8901380ffdabcc2442");
		final NatuerlichePersonGb natuerlichePerson3 = newNatuerlichePersonGb("Jean", "Gaillard", "_1f109152380ffd8901380ffdabcc2443");
		final Dienstbarkeit dienstbarkeit = newDienstbarkeit("1f109152380ffd8901380ffed6694392", "1f109152380ffd8901380ffed66943a2", "_1f109152380ffd8901380ffed6694392", 2005, 699, 8, "Usufruit", "2002/392", null, RegDate.get(2002, 9, 2), null);
		final DienstbarkeitExtendedElement discreteDienstbarkeit = newDienstbarkeitExtended(Collections.singletonList(grundstueck), Arrays.asList(natuerlichePerson1, natuerlichePerson2, natuerlichePerson3), dienstbarkeit);

		assertTrue(ServitudesRFHelper.dataEquals(usufruit, discreteDienstbarkeit));
	}

	@Test
	public void testEqualsUsufruitMultiplePersonnesKO() throws Exception {

		final ImmeubleRF immeuble = new BienFondsRF();
		immeuble.setIdRF("_1f109152380ffd8901380ffe15bb729c");

		final PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
		pp1.setIdRF("_1f109152380ffd8901380ffdabcc2441");

		final PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
		pp2.setIdRF("_1f109152380ffd8901380ffdabcc2442");

		final PersonnePhysiqueRF pp3 = new PersonnePhysiqueRF();
		pp3.setIdRF("bla bla bla");

		final UsufruitRF usufruit = newUsufruit(immeuble, pp1, new IdentifiantDroitRF(8, 2005, 699), RegDate.get(2002, 9, 2), null,
		                                        "1f109152380ffd8901380ffed6694392",  "1f109152380ffd8901380ffed66943a2", new IdentifiantAffaireRF(8, 2002, 392, null));
		usufruit.addBenefice(new BeneficeServitudeRF(null, null, usufruit, pp2));
		usufruit.addBenefice(new BeneficeServitudeRF(null, null, usufruit, pp3));

		final BelastetesGrundstueck grundstueck = new BelastetesGrundstueck("_1f109152380ffd8901380ffe15bb729c", null, null);
		final NatuerlichePersonGb natuerlichePerson1 = newNatuerlichePersonGb("Roger", "Gaillard", "_1f109152380ffd8901380ffdabcc2441");
		final NatuerlichePersonGb natuerlichePerson2 = newNatuerlichePersonGb("Robert", "Gaillard", "_1f109152380ffd8901380ffdabcc2442");
		final NatuerlichePersonGb natuerlichePerson3 = newNatuerlichePersonGb("Jean", "Gaillard", "_1f109152380ffd8901380ffdabcc2443");
		final Dienstbarkeit dienstbarkeit = newDienstbarkeit("1f109152380ffd8901380ffed6694392", "1f109152380ffd8901380ffed66943a2", "_1f109152380ffd8901380ffed6694392", 2005, 699, 8, "Usufruit", "2002/392", null, RegDate.get(2002, 9, 2), null);
		final DienstbarkeitExtendedElement discreteDienstbarkeit = newDienstbarkeitExtended(Collections.singletonList(grundstueck), Arrays.asList(natuerlichePerson1, natuerlichePerson2, natuerlichePerson3), dienstbarkeit);

		// un des trois personnes n'est pas la même
		assertFalse(ServitudesRFHelper.dataEquals(usufruit, discreteDienstbarkeit));
	}

	@Test
	public void testEqualsDroitHabitation() throws Exception {

		final ImmeubleRF immeuble = new BienFondsRF();
		immeuble.setIdRF("_1f109152380ffd8901380ffe15bb729c");

		final PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
		pp.setIdRF("_1f109152380ffd8901380ffdabcc2441");

		final DroitHabitationRF droitHabitation = newDroitHabitation(immeuble, pp, new IdentifiantDroitRF(8, 2005, 699), RegDate.get(2002, 9, 2), null,
		                                                             "1f109152380ffd8901380ffed6694392",  "1f109152380ffd8901380ffed66943a2", new IdentifiantAffaireRF(8, 2002, 392, null));

		final BelastetesGrundstueck grundstueck = new BelastetesGrundstueck("_1f109152380ffd8901380ffe15bb729c", null, null);
		final NatuerlichePersonGb natuerlichePerson = newNatuerlichePersonGb("Roger", "Gaillard", "_1f109152380ffd8901380ffdabcc2441");
		final Dienstbarkeit dienstbarkeit = newDienstbarkeit("1f109152380ffd8901380ffed6694392", "1f109152380ffd8901380ffed66943a2", "_1f109152380ffd8901380ffed6694392", 2005, 699, 8, "Droit d'habitation", "2002/392", null, RegDate.get(2002, 9, 2), null);
		final DienstbarkeitExtendedElement discreteDienstbarkeit = newDienstbarkeitExtended(grundstueck, natuerlichePerson, dienstbarkeit);

		assertTrue(ServitudesRFHelper.dataEquals(droitHabitation, discreteDienstbarkeit));
	}

	@Test
	public void testNewServitudeRFUsufruit() throws Exception {

		final ImmeubleRF immeuble = new BienFondsRF();
		immeuble.setIdRF("_1f109152380ffd8901380ffe15bb729c");

		final PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
		pp1.setIdRF("_1f109152380ffd8901380ffdabcc2441");

		final BelastetesGrundstueck grundstueck = new BelastetesGrundstueck("_1f109152380ffd8901380ffe15bb729c", null, null);
		final NatuerlichePersonGb natuerlichePerson1 = newNatuerlichePersonGb("Roger", "Gaillard", "_1f109152380ffd8901380ffdabcc2441");
		final Dienstbarkeit dienstbarkeit = newDienstbarkeit("1f109152380ffd8901380ffed6694392", "1f109152380ffd8901380ffed66943a2", "_1f109152380ffd8901380ffed6694392", 2005, 699, 8, "Usufruit", "2002/392", null, RegDate.get(2002, 9, 2), null);
		final DienstbarkeitExtendedElement discreteDienstbarkeit = newDienstbarkeitExtended(grundstueck, natuerlichePerson1, dienstbarkeit);

		final ServitudeRF servitude = ServitudesRFHelper.newServitudeRF(discreteDienstbarkeit, (id) -> pp1, (id) -> immeuble);
		assertNotNull(servitude);
		assertTrue(servitude instanceof UsufruitRF);
		assertEquals("1f109152380ffd8901380ffed6694392", servitude.getMasterIdRF());
		assertEquals(RegDate.get(2002, 9, 2), servitude.getDateDebutMetier());
		assertNull(servitude.getDateFinMetier());
		assertNull(servitude.getMotifDebut());
		assertNull(servitude.getMotifFin());
		assertIdentifiantDroit(2005, 699, 8, servitude.getIdentifiantDroit());
		assertNumeroAffaire(8, "2002/392", servitude.getNumeroAffaire());

		final Set<BeneficeServitudeRF> benefices = servitude.getBenefices();
		assertEquals(1, benefices.size());
		final BeneficeServitudeRF benefice0 = benefices.iterator().next();
		assertEquals(RegDate.get(2002, 9, 2), benefice0.getDateDebut());
		assertNull(benefice0.getDateFin());
		assertSame(pp1, benefice0.getAyantDroit());

		final Set<ChargeServitudeRF> charges = servitude.getCharges();
		assertEquals(1, charges.size());
		final ChargeServitudeRF lienImmeuble0 = charges.iterator().next();
		assertEquals(RegDate.get(2002, 9, 2), lienImmeuble0.getDateDebut());
		assertNull(lienImmeuble0.getDateFin());
		assertSame(immeuble, lienImmeuble0.getImmeuble());
	}

	@Test
	public void testNewServitudeRFDroitHabitation() throws Exception {

		final ImmeubleRF immeuble = new BienFondsRF();
		immeuble.setIdRF("_1f109152380ffd8901380ffe15bb729c");

		final PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
		pp1.setIdRF("_1f109152380ffd8901380ffdabcc2441");

		final BelastetesGrundstueck grundstueck = new BelastetesGrundstueck("_1f109152380ffd8901380ffe15bb729c", null, null);
		final NatuerlichePersonGb natuerlichePerson1 = newNatuerlichePersonGb("Roger", "Gaillard", "_1f109152380ffd8901380ffdabcc2441");
		final Dienstbarkeit dienstbarkeit = newDienstbarkeit("1f109152380ffd8901380ffed6694392", "1f109152380ffd8901380ffed66943a2", "_1f109152380ffd8901380ffed6694392", 2005, 699, 8, "Droit d'habitation", "2002/392", null, RegDate.get(2002, 9, 2), null);
		final DienstbarkeitExtendedElement discreteDienstbarkeit = newDienstbarkeitExtended(grundstueck, natuerlichePerson1, dienstbarkeit);

		final ServitudeRF servitude = ServitudesRFHelper.newServitudeRF(discreteDienstbarkeit, (id) -> pp1, (id) -> immeuble);
		assertNotNull(servitude);
		assertTrue(servitude instanceof DroitHabitationRF);
		assertEquals("1f109152380ffd8901380ffed6694392", servitude.getMasterIdRF());
		assertEquals(RegDate.get(2002, 9, 2), servitude.getDateDebutMetier());
		assertNull(servitude.getDateFinMetier());
		assertNull(servitude.getMotifDebut());
		assertNull(servitude.getMotifFin());
		assertIdentifiantDroit(2005, 699, 8, servitude.getIdentifiantDroit());
		assertNumeroAffaire(8, "2002/392", servitude.getNumeroAffaire());

		final Set<BeneficeServitudeRF> benefices = servitude.getBenefices();
		assertEquals(1, benefices.size());
		final BeneficeServitudeRF benefice0 = benefices.iterator().next();
		assertEquals(RegDate.get(2002, 9, 2), benefice0.getDateDebut());
		assertNull(benefice0.getDateFin());
		assertSame(pp1, benefice0.getAyantDroit());

		final Set<ChargeServitudeRF> charges = servitude.getCharges();
		assertEquals(1, charges.size());
		final ChargeServitudeRF lienImmeuble0 = charges.iterator().next();
		assertEquals(RegDate.get(2002, 9, 2), lienImmeuble0.getDateDebut());
		assertNull(lienImmeuble0.getDateFin());
		assertSame(immeuble, lienImmeuble0.getImmeuble());
	}

	@Test
	public void testNewServitudeRFUsufruitAvecCommunaute() throws Exception {

		final ImmeubleRF immeuble = new BienFondsRF();
		immeuble.setIdRF("_1f109152380ffd8901380ffe15bb729c");

		final PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
		pp1.setIdRF("_1f109152380ffd8901380ffdabcc2441");

		final CommunauteRF communaute = new CommunauteRF();
		communaute.setIdRF("_1f109152380ffd8901380ffed6694392");

		final BelastetesGrundstueck grundstueck = new BelastetesGrundstueck("_1f109152380ffd8901380ffe15bb729c", null, null);
		final NatuerlichePersonGb natuerlichePerson1 = newNatuerlichePersonGb("Roger", "Gaillard", "_1f109152380ffd8901380ffdabcc2441");
		final NatuerlichePersonGb natuerlichePerson2 = newNatuerlichePersonGb("Anne-Lise", "Lassueur", "_1f109152380ffd8901380ffda8131c65");
		final Dienstbarkeit dienstbarkeit = newDienstbarkeit("1f109152380ffd8901380ffed6694392", "1f109152380ffd8901380ffed66943a2", "_1f109152380ffd8901380ffed6694392", 2005, 699, 8, "Usufruit", "2002/392", null, RegDate.get(2002, 9, 2), null);
		final DienstbarkeitExtendedElement discreteDienstbarkeit = newDienstbarkeitExtended(grundstueck, natuerlichePerson1, dienstbarkeit);

		final ServitudeRF servitude = ServitudesRFHelper.newServitudeRF(discreteDienstbarkeit, (id) -> pp1, (id) -> immeuble);
		assertNotNull(servitude);
		assertTrue(servitude instanceof UsufruitRF);
		assertEquals("1f109152380ffd8901380ffed6694392", servitude.getMasterIdRF());
		assertEquals(RegDate.get(2002, 9, 2), servitude.getDateDebutMetier());
		assertNull(servitude.getDateFinMetier());
		assertNull(servitude.getMotifDebut());
		assertNull(servitude.getMotifFin());
		assertIdentifiantDroit(2005, 699, 8, servitude.getIdentifiantDroit());
		assertNumeroAffaire(8, "2002/392", servitude.getNumeroAffaire());

		final Set<BeneficeServitudeRF> benefices = servitude.getBenefices();
		assertEquals(1, benefices.size());
		final BeneficeServitudeRF benefice0 = benefices.iterator().next();
		assertEquals(RegDate.get(2002, 9, 2), benefice0.getDateDebut());
		assertNull(benefice0.getDateFin());
		assertSame(pp1, benefice0.getAyantDroit());

		final Set<ChargeServitudeRF> charges = servitude.getCharges();
		assertEquals(1, charges.size());
		final ChargeServitudeRF lienImmeuble0 = charges.iterator().next();
		assertEquals(RegDate.get(2002, 9, 2), lienImmeuble0.getDateDebut());
		assertNull(lienImmeuble0.getDateFin());
		assertSame(immeuble, lienImmeuble0.getImmeuble());
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
	public static DienstbarkeitExtendedElement newDienstbarkeitExtended(BelastetesGrundstueck grundstueck, NatuerlichePersonGb natuerlichePersonGb, Dienstbarkeit dienstbarkeit) {
		final DienstbarkeitExtendedElement d = new DienstbarkeitExtendedElement();

		final LastRechtGruppe rechtGruppe = new LastRechtGruppe();
		rechtGruppe.getBelastetesGrundstueck().add(new BelastetesGrundstueck(grundstueck.getBelastetesGrundstueckIDREF(), null, null));
		rechtGruppe.getBerechtigtePerson().add(new BerechtigtePerson(natuerlichePersonGb, null, null, null));
		d.setLastRechtGruppe(rechtGruppe);

		d.setDienstbarkeit(dienstbarkeit);
		return d;
	}

	@NotNull
	public static DienstbarkeitExtendedElement newDienstbarkeitExtended(List<BelastetesGrundstueck> grundstuecks, List<NatuerlichePersonGb> persons, Dienstbarkeit dienstbarkeit) {
		final DienstbarkeitExtendedElement d = new DienstbarkeitExtendedElement();

		final LastRechtGruppe rechtGruppe = new LastRechtGruppe();
		grundstuecks.forEach(g -> rechtGruppe.getBelastetesGrundstueck().add(new BelastetesGrundstueck(g.getBelastetesGrundstueckIDREF(), null, null)));
		persons.forEach(p -> rechtGruppe.getBerechtigtePerson().add(new BerechtigtePerson(p, null, null, null)));
		d.setLastRechtGruppe(rechtGruppe);

		d.setDienstbarkeit(dienstbarkeit);
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

	public static Dienstbarkeit newDienstbarkeit(String masterID, String versionID, String standardRechtID, int anneeDroit, int numeroDroit, int numeroOffice, String type, String numeroAffaireTexte, Beleg numeroAffaire, RegDate dateDebut,
	                                             RegDate dateFin) {
		final Dienstbarkeit d = new Dienstbarkeit();
		d.setMasterID(masterID);
		d.setVersionID(versionID);
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

	private static UsufruitRF newUsufruit(ImmeubleRF immeuble1, PersonnePhysiqueRF pp1, IdentifiantDroitRF identifiantDroit, RegDate dateDebutOfficielle,
	                                      @Nullable RegDate dateFinOfficielle, String masterIdRF, String versionIdRF, IdentifiantAffaireRF numeroAffaire) {
		final UsufruitRF servitude1 = new UsufruitRF();
		servitude1.setIdentifiantDroit(identifiantDroit);
		servitude1.setDateDebutMetier(dateDebutOfficielle);
		servitude1.setDateFinMetier(dateFinOfficielle);
		servitude1.setMotifDebut(null);
		servitude1.setMotifFin(null);
		servitude1.addCharge(new ChargeServitudeRF(dateDebutOfficielle, dateFinOfficielle, servitude1, immeuble1));
		servitude1.setMasterIdRF(masterIdRF);
		servitude1.setVersionIdRF(versionIdRF);
		servitude1.setNumeroAffaire(numeroAffaire);
		servitude1.addBenefice(new BeneficeServitudeRF(dateDebutOfficielle, dateFinOfficielle, servitude1, pp1));
		return servitude1;
	}

	private static DroitHabitationRF newDroitHabitation(ImmeubleRF immeuble1, PersonnePhysiqueRF pp1, IdentifiantDroitRF identifiantDroit, RegDate dateDebutOfficielle,
	                                                    @Nullable RegDate dateFinOfficielle, String masterIdRF, String versionIdRF, IdentifiantAffaireRF numeroAffaire) {
		final DroitHabitationRF servitude1 = new DroitHabitationRF();
		servitude1.setIdentifiantDroit(identifiantDroit);
		servitude1.setDateDebutMetier(dateDebutOfficielle);
		servitude1.setDateFinMetier(dateFinOfficielle);
		servitude1.setMotifDebut(null);
		servitude1.setMotifFin(null);
		servitude1.addCharge(new ChargeServitudeRF(dateDebutOfficielle, dateFinOfficielle, servitude1, immeuble1));
		servitude1.setMasterIdRF(masterIdRF);
		servitude1.setVersionIdRF(versionIdRF);
		servitude1.setNumeroAffaire(numeroAffaire);
		servitude1.addBenefice(new BeneficeServitudeRF(dateDebutOfficielle, dateFinOfficielle, servitude1, pp1));
		return servitude1;
	}
}