package ch.vd.uniregctb.registrefoncier.dataimport.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.BienFondsRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.RaisonAcquisitionRF;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class AffaireRFTest {

	@Test
	public void testRefreshDatesMetierAucuneRaisonAcquisition() throws Exception {
		final DroitProprieteRF d = new DroitProprietePersonnePhysiqueRF();

		final ImmeubleRF immeuble = new BienFondsRF();
		final AffaireRF affaire = new AffaireRF(null, immeuble, Collections.singletonList(d), Collections.emptyList(), Collections.emptyList());
		affaire.refreshDatesMetier(null);
		assertNull(d.getDateDebutMetier());
		assertNull(d.getMotifDebut());
	}

	@Test
	public void testRefreshDatesMetierUneRaisonAcquisition() throws Exception {
		final DroitProprieteRF d = new DroitProprietePersonnePhysiqueRF();
		d.setMasterIdRF("28288228");
		d.setVersionIdRF("1");
		d.addRaisonAcquisition(newRaisonAcquisitionRF(null, RegDate.get(2000, 3, 23), "Achat", null));

		final ImmeubleRF immeuble = new BienFondsRF();
		final AffaireRF affaire = new AffaireRF(null, immeuble, Collections.singletonList(d), Collections.emptyList(), Collections.emptyList());
		affaire.refreshDatesMetier(null);
		assertEquals(RegDate.get(2000, 3, 23), d.getDateDebutMetier());
		assertEquals("Achat", d.getMotifDebut());
	}

	@Test
	public void testRefreshDatesMetierUneRaisonAcquisitionDateNulle() throws Exception {
		final DroitProprieteRF d = new DroitProprietePersonnePhysiqueRF();
		d.setMasterIdRF("28288228");
		d.setVersionIdRF("1");
		d.addRaisonAcquisition(newRaisonAcquisitionRF(null, null, "Achat", null));

		final ImmeubleRF immeuble = new BienFondsRF();
		final AffaireRF affaire = new AffaireRF(null, immeuble, Collections.singletonList(d), Collections.emptyList(), Collections.emptyList());
		affaire.refreshDatesMetier(null);
		assertNull(d.getDateDebutMetier());
		assertEquals("Achat", d.getMotifDebut());
	}

	@Test
	public void testRefreshDatesMetierPlusieursRaisonsAcquisition() throws Exception {
		final DroitProprieteRF d = new DroitProprietePersonnePhysiqueRF();
		d.setMasterIdRF("28288228");
		d.setVersionIdRF("1");
		d.addRaisonAcquisition(newRaisonAcquisitionRF(null, RegDate.get(2000, 3, 23), "Succession", null));
		d.addRaisonAcquisition(newRaisonAcquisitionRF(null, RegDate.get(1996, 10, 1), "Achat", null));

		final ImmeubleRF immeuble = new BienFondsRF();
		final AffaireRF affaire = new AffaireRF(null, immeuble, Collections.singletonList(d), Collections.emptyList(), Collections.emptyList());
		affaire.refreshDatesMetier(null);
		assertEquals(RegDate.get(1996, 10, 1), d.getDateDebutMetier());
		assertEquals("Achat", d.getMotifDebut());
	}

	@Test
	public void testRefreshDatesMetierPlusieursRaisonsAcquisitionDateNulle() throws Exception {
		final DroitProprieteRF d = new DroitProprietePersonnePhysiqueRF();
		d.setMasterIdRF("28288228");
		d.setVersionIdRF("1");
		d.addRaisonAcquisition(newRaisonAcquisitionRF(null, RegDate.get(2000, 3, 23), "Succession", null));
		d.addRaisonAcquisition(newRaisonAcquisitionRF(null, null, "Achat", null));

		final ImmeubleRF immeuble = new BienFondsRF();
		final AffaireRF affaire = new AffaireRF(null, immeuble, Collections.singletonList(d), Collections.emptyList(), Collections.emptyList());
		affaire.refreshDatesMetier(null);
		assertNull(d.getDateDebutMetier());
		assertEquals("Achat", d.getMotifDebut());
	}

	/**
	 * [SIFISC-24987] Ce test vérifie que la date de début métier d'un droit est bien déduite de la nouvelle raison d'acquisition pour un droit qui évolue (c'est-à-dire qu'il existe un droit précédent avec le même masterId).
	 */
	@Test
	public void testRefreshDatesMetierAvecDroitPrecedentMemeMasterId() throws Exception {

		final DroitProprieteRF precedent = new DroitProprietePersonnePhysiqueRF();
		precedent.setMasterIdRF("28288228");
		precedent.setVersionIdRF("1");
		precedent.addRaisonAcquisition(newRaisonAcquisitionRF(null, RegDate.get(2000, 3, 23), "Achat", null));

		final DroitProprieteRF nouveau = new DroitProprietePersonnePhysiqueRF();
		nouveau.setMasterIdRF("28288228");
		nouveau.setVersionIdRF("2");
		nouveau.addRaisonAcquisition(newRaisonAcquisitionRF(null, RegDate.get(2000, 3, 23), "Achat", null));
		nouveau.addRaisonAcquisition(newRaisonAcquisitionRF(null, RegDate.get(2005, 8, 2), "Remaniement PPE", null));

		final ImmeubleRF immeuble = new BienFondsRF();
		final AffaireRF affaire = new AffaireRF(null, immeuble, Collections.singletonList(nouveau), Collections.emptyList(), Collections.singletonList(precedent));
		affaire.refreshDatesMetier(null);
		assertEquals(RegDate.get(2005, 8, 2), nouveau.getDateDebutMetier());
		assertEquals("Remaniement PPE", nouveau.getMotifDebut());
	}

	/**
	 * [SIFISC-25971] Ce test vérifie que la date de début métier d'un droit est bien déduite de la nouvelle raison d'acquisition pour un droit qui évolue (c'est-à-dire qu'il existe un droit précédent avec le même masterId).
	 */
	@Test
	public void testRefreshDatesMetierAvecDroitPrecedentMemeProprietaire() throws Exception {

		final PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
		pp.setId(1L);

		final DroitProprieteRF precedent = new DroitProprietePersonnePhysiqueRF();
		precedent.setMasterIdRF("28288228");
		precedent.setVersionIdRF("1");
		precedent.setAyantDroit(pp);
		precedent.addRaisonAcquisition(newRaisonAcquisitionRF(null, RegDate.get(2000, 3, 23), "Achat", null));

		final DroitProprieteRF nouveau = new DroitProprietePersonnePhysiqueRF();
		nouveau.setMasterIdRF("3838384444");
		nouveau.setVersionIdRF("1");
		nouveau.setAyantDroit(pp);
		nouveau.addRaisonAcquisition(newRaisonAcquisitionRF(null, RegDate.get(2000, 3, 23), "Achat", null));
		nouveau.addRaisonAcquisition(newRaisonAcquisitionRF(null, RegDate.get(2005, 8, 2), "Remaniement PPE", null));

		final ImmeubleRF immeuble = new BienFondsRF();
		final AffaireRF affaire = new AffaireRF(null, immeuble, Collections.singletonList(nouveau), Collections.emptyList(), Collections.singletonList(precedent));
		affaire.refreshDatesMetier(null);
		assertEquals(RegDate.get(2005, 8, 2), nouveau.getDateDebutMetier());
		assertEquals("Remaniement PPE", nouveau.getMotifDebut());
	}

	/**
	 * [SIFISC-25583] Ce test vérifie que les dates de début métier sont bien calculées dans le cas où : <ul> <li>un droit existant est modifié (changement de part de propriété ou autre)</li> <li>aucun nouvelle raison d'acquisition n'existe sur le
	 * droit modifié</li> <li>un nouveau droit est ouvert avec une nouvelle raison d'acquisition</li> </ul> A ce moment-là, la date de début métier du droit modifié doit être égale à la date de début métier du nouveau droit.
	 * <p>
	 * <b>Cas métier:</b> CH707345958325 (succession au 31.07.2017)
	 */
	@Test
	public void testRefreshDatesMetierAvecRaisonAcquisitionManquanteSurUnDesDroits() throws Exception {

		final Listener listener = new Listener();

		final RegDate dateAchat = RegDate.get(1970, 8, 20);
		final RegDate dateSuccession = RegDate.get(2017, 7, 31);
		final RegDate dateImportInitial = RegDate.get(2016, 12, 31);
		final RegDate dateImportSuccession = RegDate.get(2017, 9, 10);

		// communauté André Piguet - Lucette Piguet
		final CommunauteRF communaute1 = new CommunauteRF();
		final PersonnePhysiqueRF andre = new PersonnePhysiqueRF();
		andre.setId(1L);
		final PersonnePhysiqueRF lucette = new PersonnePhysiqueRF();
		lucette.setId(2L);

		// droit initial de André Piguet
		final DroitProprietePersonnePhysiqueRF precedent1 = new DroitProprietePersonnePhysiqueRF();
		precedent1.setMasterIdRF("28288228");
		precedent1.setVersionIdRF("1");
		precedent1.setPart(new Fraction(1, 1));
		precedent1.setDateDebut(dateImportInitial);
		precedent1.setDateFin(dateImportSuccession.getOneDayBefore());
		precedent1.setAyantDroit(andre);
		precedent1.setCommunaute(communaute1);
		precedent1.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportInitial, dateAchat, "Achat", null));

		// droit initial de Lucette Piguet
		final DroitProprietePersonnePhysiqueRF precedent2 = new DroitProprietePersonnePhysiqueRF();
		precedent2.setMasterIdRF("382818811");
		precedent2.setVersionIdRF("1");
		precedent2.setPart(new Fraction(1, 1));
		precedent2.setDateDebut(dateImportInitial);
		precedent2.setDateFin(dateImportSuccession.getOneDayBefore());
		precedent2.setAyantDroit(lucette);
		precedent2.setCommunaute(communaute1);
		precedent2.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportInitial, dateAchat, "Achat", null));

		// communauté Lucette Piguet - Laurent Piguet - Evelyne Vultaggio
		final CommunauteRF communaute2 = new CommunauteRF();
		final PersonnePhysiqueRF laurent = new PersonnePhysiqueRF();
		laurent.setId(3L);
		final PersonnePhysiqueRF evelyne = new PersonnePhysiqueRF();
		evelyne.setId(4L);

		// droit modifié de Lucette Piguet <--- la raison d'acquisition ne change pas !
		final DroitProprietePersonnePhysiqueRF nouveau2 = new DroitProprietePersonnePhysiqueRF();
		nouveau2.setMasterIdRF("382818811");
		nouveau2.setVersionIdRF("2");
		nouveau2.setPart(new Fraction(1, 1));
		nouveau2.setDateDebut(dateImportSuccession);
		nouveau2.setAyantDroit(lucette);
		nouveau2.setCommunaute(communaute2);
		nouveau2.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportSuccession, dateAchat, "Achat", null));

		// nouveau droit de Laurent Piguet
		final DroitProprietePersonnePhysiqueRF nouveau3 = new DroitProprietePersonnePhysiqueRF();
		nouveau3.setMasterIdRF("777433");
		nouveau3.setVersionIdRF("1");
		nouveau3.setPart(new Fraction(1, 1));
		nouveau3.setDateDebut(dateImportSuccession);
		nouveau3.setAyantDroit(laurent);
		nouveau3.setCommunaute(communaute2);
		nouveau3.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportSuccession, dateSuccession, "Succession", null));

		// nouveau droit de Evelyne Vultaggio
		final DroitProprietePersonnePhysiqueRF nouveau4 = new DroitProprietePersonnePhysiqueRF();
		nouveau4.setMasterIdRF("91919191");
		nouveau4.setVersionIdRF("1");
		nouveau4.setPart(new Fraction(1, 1));
		nouveau4.setDateDebut(dateImportSuccession);
		nouveau4.setAyantDroit(evelyne);
		nouveau4.setCommunaute(communaute2);
		nouveau4.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportSuccession, dateSuccession, "Succession", null));

		final ImmeubleRF immeuble = new BienFondsRF();
		final AffaireRF affaire = new AffaireRF(dateImportSuccession, immeuble, Arrays.asList(nouveau2, nouveau3, nouveau4), Collections.emptyList(), Arrays.asList(precedent1, precedent2));
		affaire.refreshDatesMetier(listener);
		assertNull(precedent1.getDateDebutMetier());                    // <-- les droits précédents possèdent maintenant des dates de fin métier
		assertEquals(dateSuccession, precedent1.getDateFinMetier());
		assertNull(precedent2.getDateDebutMetier());
		assertEquals(dateSuccession, precedent2.getDateFinMetier());
		assertEquals(dateSuccession, nouveau2.getDateDebutMetier());    // <-- malgré l'absence de nouvelle raison d'acquisition, la raison d'acquisition "succession" est utilisée
		assertEquals(dateSuccession, nouveau3.getDateDebutMetier());
		assertEquals(dateSuccession, nouveau4.getDateDebutMetier());
		assertEquals("Succession", nouveau2.getMotifDebut());
		assertEquals("Succession", nouveau3.getMotifDebut());
		assertEquals("Succession", nouveau4.getMotifDebut());

		// les 5 droits sont mis-à-jour
		assertEquals(0, listener.getCreated().size());
		final List<Listener.FinUpdate> finUpdates = listener.getFinUpdates();
		assertEquals(2, finUpdates.size());
		assertFinUpdate(precedent1, null, null, finUpdates.get(0));
		assertFinUpdate(precedent2, null, null, finUpdates.get(1));
		final List<Listener.DebutUpdate> debutUpdates = listener.getDebutUpdates();
		assertEquals(3, debutUpdates.size());
		assertDebutUpdate(nouveau2, null, null, debutUpdates.get(0));
		assertDebutUpdate(nouveau3, null, null, debutUpdates.get(1));
		assertDebutUpdate(nouveau4, null, null, debutUpdates.get(2));
		assertEquals(0, listener.getClosed().size());
	}

	/**
	 * [SIFISC-25583] Ce test vérifie que les dates de début métier sont bien calculées dans le cas d'une communauté où :
	 * <ul>
	 *     <li>un droit existant est fermé (donation du droit ou partage</li>
	 *     <li>un ou plusieurs droits existants reçoivent une nouvelle raison d'acquisition (donation ou partage)</li>
	 * </ul>
	 * A ce moment-là, la date de fin métier du droit fermé doit être égale à la date de la nouvelle raison d'acquisition.
	 * <p>
	 * <b>Cas métier:</b> CH767965655789 (donation au 11.04.2017)
	 */
	@Test
	public void testRefreshDatesMetierAvecDonation() throws Exception {

		final Listener listener = new Listener();

		// dates métier
		final RegDate dateSuccession = RegDate.get(2016, 10, 3);
		final RegDate dateDonation = RegDate.get(2017, 4, 11);

		// dates techniques
		final RegDate dateImportInitial = RegDate.get(2016, 12, 31);
		final RegDate dateSecondImport = RegDate.get(2017, 9, 10);

		// communauté Michelle - Blaise - Joëlle
		final CommunauteRF communaute = new CommunauteRF();
		final PersonnePhysiqueRF michelle = new PersonnePhysiqueRF();
		michelle.setId(1L);
		final PersonnePhysiqueRF blaise = new PersonnePhysiqueRF();
		blaise.setId(2L);
		final PersonnePhysiqueRF joelle = new PersonnePhysiqueRF();
		joelle.setId(3L);

		// droit de Michelle
		final DroitProprietePersonnePhysiqueRF droitMichelle = new DroitProprietePersonnePhysiqueRF();
		droitMichelle.setMasterIdRF("28288228");
		droitMichelle.setVersionIdRF("1");
		droitMichelle.setPart(new Fraction(1, 1));
		droitMichelle.setDateDebut(dateImportInitial);
		droitMichelle.setDateDebutMetier(dateSuccession);
		droitMichelle.setMotifDebut("Succession");
		droitMichelle.setDateFin(dateSecondImport.getOneDayBefore());
		droitMichelle.setAyantDroit(michelle);
		droitMichelle.setCommunaute(communaute);
		droitMichelle.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportInitial, dateSuccession, "Succession", null));

		// droit de Blaise
		final DroitProprietePersonnePhysiqueRF droitBlaise = new DroitProprietePersonnePhysiqueRF();
		droitBlaise.setMasterIdRF("382818811");
		droitBlaise.setVersionIdRF("1");
		droitBlaise.setPart(new Fraction(1, 1));
		droitBlaise.setDateDebut(dateImportInitial);
		droitBlaise.setDateDebutMetier(dateSuccession);
		droitBlaise.setMotifDebut("Succession");
		droitBlaise.setAyantDroit(blaise);
		droitBlaise.setCommunaute(communaute);
		droitBlaise.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportInitial, dateSuccession, "Succession", null));
		droitBlaise.addRaisonAcquisition(newRaisonAcquisitionRF(dateSecondImport, dateDonation, "Donation", null));

		// droit de Joëlle
		final DroitProprietePersonnePhysiqueRF droitJoelle = new DroitProprietePersonnePhysiqueRF();
		droitJoelle.setMasterIdRF("382818811");
		droitJoelle.setVersionIdRF("1");
		droitJoelle.setPart(new Fraction(1, 1));
		droitJoelle.setDateDebut(dateImportInitial);
		droitJoelle.setDateDebutMetier(dateSuccession);
		droitJoelle.setMotifDebut("Succession");
		droitJoelle.setAyantDroit(joelle);
		droitJoelle.setCommunaute(communaute);
		droitJoelle.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportInitial, dateSuccession, "Succession", null));
		droitJoelle.addRaisonAcquisition(newRaisonAcquisitionRF(dateSecondImport, dateDonation, "Donation", null));

		final ImmeubleRF immeuble = new BienFondsRF();
		final AffaireRF affaire = new AffaireRF(dateSecondImport, immeuble, Collections.emptyList(), Arrays.asList(new Pair<>(droitBlaise, droitBlaise), new Pair<>(droitJoelle, droitJoelle)), Collections.singletonList(droitMichelle));
		affaire.refreshDatesMetier(listener);
		assertEquals(dateSuccession, droitMichelle.getDateDebutMetier());
		assertEquals(dateDonation, droitMichelle.getDateFinMetier());
		assertEquals(dateSuccession, droitBlaise.getDateDebutMetier());
		assertNull(droitBlaise.getDateFinMetier());
		assertEquals(dateSuccession, droitJoelle.getDateDebutMetier());
		assertNull(droitJoelle.getDateFinMetier());

		// le droit de Michelle doit être fermé
		assertEquals(0, listener.getCreated().size());
		final List<Listener.FinUpdate> finUpdates = listener.getFinUpdates();
		assertEquals(1, finUpdates.size());
		assertFinUpdate(michelle, dateDonation, "Donation", null, null, finUpdates.get(0));
		final List<Listener.DebutUpdate> debutUpdates = listener.getDebutUpdates();
		assertEquals(0, debutUpdates.size());
		assertEquals(0, listener.getClosed().size());
	}

	/**
	 * [SIFISC-25583] Ce test vérifie que les dates de début métier sont bien calculées dans le cas d'une communauté où :
	 * <ul>
	 *     <li>des droits existants sont fermés sur deux propriétaires</li>
	 *     <li>un nouveau droit est ouvert sur un propriétaire précédent en reprenant les raisons d'acquisition du droit fermé et
	 *     en y ajoutant une nouvelle (= toutes les raisons d'acquisition ont la même date de début technique, égale à celle de l'import)</li>
	 *     <li>un nouveau droit est ouvert sur un nouveau propriétaire en reprenant les mêmes raisons d'acquisition que ci-dessus, mais sans
	 *     que cela ait un sens métier puisqu'il n'y avait pas d'ancien droit pour ce propriétaire</li>
	 * </ul>
	 * A ce moment-là, la date de fin métier du droit fermé doit être égale à la date de la nouvelle raison d'acquisition du nouveau droit
	 * <p>
	 * <b>Cas métier:</b> CH574578678369 (cession au 03.07.2017)
	 */
	@Test
	public void testRefreshDatesMetierAvecNouveauDroitquiReprendToutesLesRaisonsAcquisition() throws Exception {

		final Listener listener = new Listener();

		// dates métier
		final RegDate dateTransfert = RegDate.get(1991, 4, 9);
		final RegDate dateSuccession = RegDate.get(2016, 11, 3);
		final RegDate dateCession = RegDate.get(2017, 7, 3);

		// dates techniques
		final RegDate dateImportInitial = RegDate.get(2016, 12, 31);
		final RegDate dateSecondImport = RegDate.get(2017, 9, 10);

		// communauté Patricia - Eva
		final CommunauteRF communauteTransfert = new CommunauteRF();
		final PersonnePhysiqueRF patricia = new PersonnePhysiqueRF();
		patricia.setId(1L);
		final PersonnePhysiqueRF eva = new PersonnePhysiqueRF();
		eva.setId(2L);

		// communauté Patricia - Alexander
		final CommunauteRF communauteCession = new CommunauteRF();
		final PersonnePhysiqueRF alexander = new PersonnePhysiqueRF();
		alexander.setId(3L);

		// droit de Patricia sur la communauté Transfer
		final DroitProprietePersonnePhysiqueRF droitPatriciaTransfer = new DroitProprietePersonnePhysiqueRF();
		droitPatriciaTransfer.setMasterIdRF("28288228");
		droitPatriciaTransfer.setVersionIdRF("1");
		droitPatriciaTransfer.setPart(new Fraction(1, 1));
		droitPatriciaTransfer.setDateDebut(dateImportInitial);
		droitPatriciaTransfer.setDateDebutMetier(dateTransfert);
		droitPatriciaTransfer.setMotifDebut("Transfert");
		droitPatriciaTransfer.setDateFin(dateSecondImport.getOneDayBefore());
		droitPatriciaTransfer.setAyantDroit(patricia);
		droitPatriciaTransfer.setCommunaute(communauteTransfert);
		droitPatriciaTransfer.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportInitial, dateTransfert, "Transfert", null));

		// droit d'Eva sur la communauté Transfer
		final DroitProprietePersonnePhysiqueRF droitEva = new DroitProprietePersonnePhysiqueRF();
		droitEva.setMasterIdRF("382818811");
		droitEva.setVersionIdRF("1");
		droitEva.setPart(new Fraction(1, 1));
		droitEva.setDateDebut(dateImportInitial);
		droitEva.setDateDebutMetier(dateSuccession);
		droitEva.setMotifDebut("Transfert");
		droitEva.setDateFin(dateSecondImport.getOneDayBefore());
		droitEva.setAyantDroit(eva);
		droitEva.setCommunaute(communauteTransfert);
		droitEva.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportInitial, dateSuccession, "Succession", null));

		// droit de Patricia sur la communauté Cession
		final DroitProprietePersonnePhysiqueRF droitPatriciaCession = new DroitProprietePersonnePhysiqueRF();
		droitPatriciaCession.setMasterIdRF("77772711");
		droitPatriciaCession.setVersionIdRF("1");
		droitPatriciaCession.setPart(new Fraction(1, 1));
		droitPatriciaCession.setDateDebut(dateSecondImport);
		droitPatriciaCession.setAyantDroit(patricia);
		droitPatriciaCession.setCommunaute(communauteCession);
		droitPatriciaCession.addRaisonAcquisition(newRaisonAcquisitionRF(dateSecondImport, dateTransfert, "Transfert", null));
		droitPatriciaCession.addRaisonAcquisition(newRaisonAcquisitionRF(dateSecondImport, dateCession, "Cession", null));

		// droit de Alexander sur la communauté Cession
		final DroitProprietePersonnePhysiqueRF droitAlexander = new DroitProprietePersonnePhysiqueRF();
		droitAlexander.setMasterIdRF("48833838");
		droitAlexander.setVersionIdRF("1");
		droitAlexander.setPart(new Fraction(1, 1));
		droitAlexander.setDateDebut(dateSecondImport);
		droitAlexander.setAyantDroit(alexander);
		droitAlexander.setCommunaute(communauteCession);
		droitAlexander.addRaisonAcquisition(newRaisonAcquisitionRF(dateSecondImport, dateTransfert, "Transfert", null));
		droitAlexander.addRaisonAcquisition(newRaisonAcquisitionRF(dateSecondImport, dateCession, "Cession", null));

		final ImmeubleRF immeuble = new BienFondsRF();
		final AffaireRF affaire = new AffaireRF(dateSecondImport, immeuble, Arrays.asList(droitPatriciaCession, droitAlexander), Collections.emptyList(), Arrays.asList(droitPatriciaTransfer, droitEva));
		affaire.refreshDatesMetier(listener);
		// droits de la communauté 'Transfer'
		assertEquals(dateTransfert, droitPatriciaTransfer.getDateDebutMetier());
		assertEquals(dateCession, droitPatriciaTransfer.getDateFinMetier());    // <-- correspond à la date de début métier du droit droitPatriciaCession (regroupement par ayant-droit)
		assertEquals(dateSuccession, droitEva.getDateDebutMetier());
		assertEquals(dateCession, droitEva.getDateFinMetier());                 // <-- correspond à la date de début métier du droit droitPatriciaCession (regroupement par date de transaction)
		// droits de la communauté 'Cession'
		assertEquals(dateCession, droitPatriciaCession.getDateDebutMetier());   // <-- la raison d'acquisition 'Transfert' est ignorée car elle n'est pas 'nouvelle' par rapport à l'ancien droit
		assertNull(droitPatriciaCession.getDateFinMetier());
		assertEquals(dateTransfert, droitAlexander.getDateDebutMetier());       // <-- la raison d'acquisition 'Transfert' est utilisée car il n'y a pas d'ancien droit pour ce propriétaire
		assertNull(droitAlexander.getDateFinMetier());

		// les droits de Patricia et Eva sur la communauté 'Transfer' doivent être fermés
		assertEquals(0, listener.getCreated().size());
		final List<Listener.FinUpdate> finUpdates = listener.getFinUpdates();
		assertEquals(2, finUpdates.size());
		assertFinUpdate(patricia, dateCession, "Cession", null, null, finUpdates.get(0));
		assertFinUpdate(eva, dateCession, "Cession", null, null, finUpdates.get(1));
		// les droits de Patricia et Alexander sur la communauté 'Cession' doivent être ouverts
		final List<Listener.DebutUpdate> debutUpdates = listener.getDebutUpdates();
		assertEquals(2, debutUpdates.size());
		assertDebutUpdate(patricia, dateCession, "Cession", null, null, debutUpdates.get(0));
		assertDebutUpdate(alexander, dateTransfert, "Transfert", null, null, debutUpdates.get(1));
		assertEquals(0, listener.getClosed().size());
	}

	/**
	 * [SIFISC-25583] Variante du test {@link #testRefreshDatesMetierAvecRaisonAcquisitionManquanteSurUnDesDroits()} avec des dates différentes sur les droits ouverts suite à la succession.
	 */
	@Test
	public void testRefreshDatesMetierAvecRaisonAcquisitionManquanteSurUnDesDroitsVariante1() throws Exception {

		final RegDate dateAchat = RegDate.get(1970, 8, 20);
		final RegDate dateSuccession1 = RegDate.get(2017, 7, 26);
		final RegDate dateSuccession2 = RegDate.get(2017, 7, 31);

		final RegDate dateImportInitial = RegDate.get(2016, 12, 31);
		final RegDate dateImportSuccession = RegDate.get(2017, 9, 10);

		// communauté André Piguet - Lucette Piguet
		final CommunauteRF communaute1 = new CommunauteRF();
		final PersonnePhysiqueRF andre = new PersonnePhysiqueRF();
		andre.setId(1L);
		final PersonnePhysiqueRF lucette = new PersonnePhysiqueRF();
		lucette.setId(2L);

		// droit initial de André Piguet
		final DroitProprietePersonnePhysiqueRF precedent1 = new DroitProprietePersonnePhysiqueRF();
		precedent1.setMasterIdRF("28288228");
		precedent1.setVersionIdRF("1");
		precedent1.setPart(new Fraction(1, 1));
		precedent1.setDateDebut(dateImportInitial);
		precedent1.setAyantDroit(andre);
		precedent1.setCommunaute(communaute1);
		precedent1.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportInitial, dateAchat, "Achat", null));

		// droit initial de Lucette Piguet
		final DroitProprietePersonnePhysiqueRF precedent2 = new DroitProprietePersonnePhysiqueRF();
		precedent2.setMasterIdRF("382818811");
		precedent2.setVersionIdRF("1");
		precedent2.setPart(new Fraction(1, 1));
		precedent2.setDateDebut(dateImportInitial);
		precedent2.setAyantDroit(lucette);
		precedent2.setCommunaute(communaute1);
		precedent2.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportInitial, dateAchat, "Achat", null));

		// communauté Lucette Piguet - Laurent Piguet - Evelyne Vultaggio
		final CommunauteRF communaute2 = new CommunauteRF();
		final PersonnePhysiqueRF laurent = new PersonnePhysiqueRF();
		laurent.setId(3L);
		final PersonnePhysiqueRF evelyne = new PersonnePhysiqueRF();
		evelyne.setId(4L);

		// droit modifié de Lucette Piguet <--- la raison d'acquisition ne change pas !
		final DroitProprietePersonnePhysiqueRF nouveau2 = new DroitProprietePersonnePhysiqueRF();
		nouveau2.setMasterIdRF("382818811");
		nouveau2.setVersionIdRF("2");
		nouveau2.setPart(new Fraction(1, 1));
		nouveau2.setDateDebut(dateImportSuccession);
		nouveau2.setAyantDroit(lucette);
		nouveau2.setCommunaute(communaute2);
		nouveau2.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportSuccession, dateAchat, "Achat", null));

		// nouveau droit de Laurent Piguet (avec une date de succession légèrement plus tardive)
		final DroitProprietePersonnePhysiqueRF nouveau3 = new DroitProprietePersonnePhysiqueRF();
		nouveau3.setMasterIdRF("777433");
		nouveau3.setVersionIdRF("1");
		nouveau3.setPart(new Fraction(1, 1));
		nouveau3.setDateDebut(dateImportSuccession);
		nouveau3.setAyantDroit(laurent);
		nouveau3.setCommunaute(communaute2);
		nouveau3.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportSuccession, dateSuccession2, "Succession", null));

		// nouveau droit de Evelyne Vultaggio
		final DroitProprietePersonnePhysiqueRF nouveau4 = new DroitProprietePersonnePhysiqueRF();
		nouveau4.setMasterIdRF("91919191");
		nouveau4.setVersionIdRF("1");
		nouveau4.setPart(new Fraction(1, 1));
		nouveau4.setDateDebut(dateImportSuccession);
		nouveau4.setAyantDroit(evelyne);
		nouveau4.setCommunaute(communaute2);
		nouveau4.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportSuccession, dateSuccession1, "Succession", null));

		final ImmeubleRF immeuble = new BienFondsRF();
		final AffaireRF affaire = new AffaireRF(dateSuccession1, immeuble, Arrays.asList(nouveau2, nouveau3, nouveau4), Collections.emptyList(), Arrays.asList(precedent1, precedent2));
		affaire.refreshDatesMetier(null);
		assertNull(precedent1.getDateDebutMetier());    // pas de changement sur les droits précédents
		assertNull(precedent2.getDateDebutMetier());
		assertEquals(dateSuccession1, nouveau2.getDateDebutMetier());   // <-- la raison d'acquisition "succession" la plus ancienne doit être utilisée
		assertEquals(dateSuccession2, nouveau3.getDateDebutMetier());
		assertEquals(dateSuccession1, nouveau4.getDateDebutMetier());
		assertEquals("Succession", nouveau2.getMotifDebut());
		assertEquals("Succession", nouveau3.getMotifDebut());
		assertEquals("Succession", nouveau4.getMotifDebut());
	}

	private static class Listener implements AffaireRFListener {

		private static class DebutUpdate {

			private final DroitProprieteRF droit;
			private final RegDate dateDebutMetierInitiale;
			private final String motifDebutInitial;

			public DebutUpdate(DroitProprieteRF droit, RegDate dateDebutMetierInitiale, String motifDebutInitial) {
				this.dateDebutMetierInitiale = dateDebutMetierInitiale;
				this.motifDebutInitial = motifDebutInitial;
				this.droit = droit;
			}

			public DroitProprieteRF getDroit() {
				return droit;
			}

			public RegDate getDateDebutMetierInitiale() {
				return dateDebutMetierInitiale;
			}

			public String getMotifDebutInitial() {
				return motifDebutInitial;
			}
		}

		private static class FinUpdate {

			private final DroitProprieteRF droit;
			private final RegDate dateFinMetierInitiale;
			private final String motifFinInitial;

			public FinUpdate(DroitProprieteRF droit, RegDate dateFinMetierInitiale, String motifFinInitial) {
				this.dateFinMetierInitiale = dateFinMetierInitiale;
				this.motifFinInitial = motifFinInitial;
				this.droit = droit;
			}

			public DroitProprieteRF getDroit() {
				return droit;
			}

			public RegDate getDateFinMetierInitiale() {
				return dateFinMetierInitiale;
			}

			public String getMotifFinInitial() {
				return motifFinInitial;
			}
		}

		private final List<DroitProprieteRF> created = new ArrayList<>();
		private final List<DebutUpdate> debutUpdates = new ArrayList<>();
		private final List<FinUpdate> finUpdates = new ArrayList<>();
		private final List<DroitProprieteRF> autresUpdates = new ArrayList<>();
		private final List<DroitProprieteRF> closed = new ArrayList<>();

		@Override
		public void onCreation(DroitProprieteRF droit) {
			created.add(droit);
		}

		@Override
		public void onUpdateDateDebut(@NotNull DroitProprieteRF droit, @Nullable RegDate dateDebutMetierInitiale, @Nullable String motifDebutInitial) {
			debutUpdates.add(new DebutUpdate(droit, dateDebutMetierInitiale, motifDebutInitial));
		}

		@Override
		public void onUpdateDateFin(@NotNull DroitProprieteRF droit, @Nullable RegDate dateFinMetierInitiale, @Nullable String motifFinInitial) {
			finUpdates.add(new FinUpdate(droit, dateFinMetierInitiale, motifFinInitial));
		}

		@Override
		public void onOtherUpdate(@NotNull DroitProprieteRF droit) {
			autresUpdates.add(droit);
		}

		@Override
		public void onClosing(@NotNull DroitProprieteRF droit) {
			closed.add(droit);
		}

		public List<DroitProprieteRF> getCreated() {
			return created;
		}

		public List<DebutUpdate> getDebutUpdates() {
			return debutUpdates;
		}

		public List<FinUpdate> getFinUpdates() {
			return finUpdates;
		}

		public List<DroitProprieteRF> getAutresUpdates() {
			return autresUpdates;
		}

		public List<DroitProprieteRF> getClosed() {
			return closed;
		}
	}

	private RaisonAcquisitionRF newRaisonAcquisitionRF(RegDate dateDebut, RegDate dateAcquisition, String motifAcquisition, IdentifiantAffaireRF numeroAffaire) {
		final RaisonAcquisitionRF r = new RaisonAcquisitionRF(dateAcquisition, motifAcquisition, numeroAffaire);
		r.setDateDebut(dateDebut);
		return r;
	}

	private static void assertDebutUpdate(DroitProprieteRF droit, RegDate dateDebutInitiale, String motifDebutInitial, Listener.DebutUpdate debutUpdate) {
		assertNotNull(debutUpdate);
		assertSame(droit, debutUpdate.getDroit());
		assertEquals(dateDebutInitiale, debutUpdate.getDateDebutMetierInitiale());
		assertEquals(motifDebutInitial, debutUpdate.getMotifDebutInitial());
	}

	private static void assertDebutUpdate(AyantDroitRF ayantDroit, RegDate dateDebutCorrigee, String motifDebutCorrige, RegDate dateDebutInitiale, String motifDebutInitial, Listener.DebutUpdate debutUpdate) {
		assertNotNull(debutUpdate);
		assertSame(ayantDroit, debutUpdate.getDroit().getAyantDroit());
		assertSame(dateDebutCorrigee, debutUpdate.getDroit().getDateDebutMetier());
		assertSame(motifDebutCorrige, debutUpdate.getDroit().getMotifDebut());
		assertEquals(dateDebutInitiale, debutUpdate.getDateDebutMetierInitiale());
		assertEquals(motifDebutInitial, debutUpdate.getMotifDebutInitial());
	}

	private static void assertFinUpdate(DroitProprieteRF droit, RegDate dateFinInitiale, String motifFinInitial, Listener.FinUpdate finUpdate) {
		assertNotNull(finUpdate);
		assertSame(droit, finUpdate.getDroit());
		assertEquals(dateFinInitiale, finUpdate.getDateFinMetierInitiale());
		assertEquals(motifFinInitial, finUpdate.getMotifFinInitial());
	}

	private static void assertFinUpdate(AyantDroitRF ayantDroit, RegDate dateFinCorrigee, String motifFinCorrige, RegDate dateFinInitiale, String motifFinInitial, Listener.FinUpdate finUpdate) {
		assertNotNull(finUpdate);
		assertSame(ayantDroit, finUpdate.getDroit().getAyantDroit());
		assertSame(dateFinCorrigee, finUpdate.getDroit().getDateFinMetier());
		assertSame(motifFinCorrige, finUpdate.getDroit().getMotifFin());
		assertEquals(dateFinInitiale, finUpdate.getDateFinMetierInitiale());
		assertEquals(motifFinInitial, finUpdate.getMotifFinInitial());
	}
}