package ch.vd.unireg.registrefoncier.dataimport.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.registrefoncier.AyantDroitRF;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.DroitProprieteCommunauteRF;
import ch.vd.unireg.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.DroitProprieteRF;
import ch.vd.unireg.registrefoncier.Fraction;
import ch.vd.unireg.registrefoncier.GenrePropriete;
import ch.vd.unireg.registrefoncier.IdentifiantAffaireRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.PersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.RaisonAcquisitionRF;
import ch.vd.unireg.registrefoncier.dao.MockDroitRFDAO;

import static ch.vd.unireg.common.WithoutSpringTest.assertEmpty;
import static ch.vd.unireg.common.WithoutSpringTest.date;
import static ch.vd.unireg.registrefoncier.GenrePropriete.COMMUNE;
import static ch.vd.unireg.registrefoncier.GenrePropriete.COPROPRIETE;
import static ch.vd.unireg.registrefoncier.GenrePropriete.INDIVIDUELLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class AffaireRFTest {

	@Test
	public void testRefreshDatesMetierAucuneRaisonAcquisition() throws Exception {
		final DroitProprieteRF d = new DroitProprietePersonnePhysiqueRF();

		final ImmeubleRF immeuble = new BienFondsRF();
		immeuble.addDroitPropriete(d);

		final AffaireRF affaire = new AffaireRF(null, immeuble);
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
		immeuble.addDroitPropriete(d);

		final AffaireRF affaire = new AffaireRF(null, immeuble);
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
		immeuble.addDroitPropriete(d);

		final AffaireRF affaire = new AffaireRF(null, immeuble);
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
		immeuble.addDroitPropriete(d);

		final AffaireRF affaire = new AffaireRF(null, immeuble);
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
		immeuble.addDroitPropriete(d);

		final AffaireRF affaire = new AffaireRF(null, immeuble);
		affaire.refreshDatesMetier(null);
		assertNull(d.getDateDebutMetier());
		assertEquals("Achat", d.getMotifDebut());
	}

	/**
	 * [SIFISC-24987] Ce test vérifie que la date de début métier d'un droit est bien déduite de la nouvelle raison d'acquisition pour un droit qui évolue (c'est-à-dire qu'il existe un droit précédent avec le même masterId).
	 */
	@Test
	public void testRefreshDatesMetierAvecDroitPrecedentMemeMasterId() throws Exception {

		final RegDate dateDebut = RegDate.get(2005, 6, 13);
		final PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
		pp.setId(1L);

		final DroitProprieteRF precedent = new DroitProprietePersonnePhysiqueRF();
		precedent.setMasterIdRF("28288228");
		precedent.setVersionIdRF("1");
		precedent.setDateFin(dateDebut.getOneDayBefore());
		precedent.setAyantDroit(pp);
		precedent.addRaisonAcquisition(newRaisonAcquisitionRF(null, RegDate.get(2000, 3, 23), "Achat", null));

		final DroitProprieteRF nouveau = new DroitProprietePersonnePhysiqueRF();
		nouveau.setMasterIdRF("28288228");
		nouveau.setVersionIdRF("2");
		nouveau.setDateDebut(dateDebut);
		nouveau.setAyantDroit(pp);
		nouveau.addRaisonAcquisition(newRaisonAcquisitionRF(null, RegDate.get(2000, 3, 23), "Achat", null));
		nouveau.addRaisonAcquisition(newRaisonAcquisitionRF(null, RegDate.get(2005, 8, 2), "Remaniement PPE", null));

		final ImmeubleRF immeuble = new BienFondsRF();
		immeuble.addDroitPropriete(precedent);
		immeuble.addDroitPropriete(nouveau);

		final AffaireRF affaire = new AffaireRF(dateDebut, immeuble);
		affaire.refreshDatesMetier(null);
		assertEquals(RegDate.get(2005, 8, 2), nouveau.getDateDebutMetier());
		assertEquals("Remaniement PPE", nouveau.getMotifDebut());
	}

	/**
	 * [SIFISC-25971] Ce test vérifie que la date de début métier d'un droit est bien déduite de la nouvelle raison d'acquisition pour un droit qui évolue (c'est-à-dire qu'il existe un droit précédent avec le même masterId).
	 */
	@Test
	public void testRefreshDatesMetierAvecDroitPrecedentMemeProprietaire() throws Exception {

		final RegDate dateDebut = RegDate.get(2005, 6, 13);
		final PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
		pp.setId(1L);

		final DroitProprieteRF precedent = new DroitProprietePersonnePhysiqueRF();
		precedent.setMasterIdRF("28288228");
		precedent.setVersionIdRF("1");
		precedent.setDateFin(dateDebut.getOneDayBefore());
		precedent.setAyantDroit(pp);
		precedent.addRaisonAcquisition(newRaisonAcquisitionRF(null, RegDate.get(2000, 3, 23), "Achat", null));

		final DroitProprieteRF nouveau = new DroitProprietePersonnePhysiqueRF();
		nouveau.setMasterIdRF("3838384444");
		nouveau.setVersionIdRF("1");
		nouveau.setDateDebut(dateDebut);
		nouveau.setAyantDroit(pp);
		nouveau.addRaisonAcquisition(newRaisonAcquisitionRF(null, RegDate.get(2000, 3, 23), "Achat", null));
		nouveau.addRaisonAcquisition(newRaisonAcquisitionRF(null, RegDate.get(2005, 8, 2), "Remaniement PPE", null));

		final ImmeubleRF immeuble = new BienFondsRF();
		immeuble.addDroitPropriete(precedent);
		immeuble.addDroitPropriete(nouveau);

		final AffaireRF affaire = new AffaireRF(dateDebut, immeuble);
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
		immeuble.addDroitPropriete(precedent1);
		immeuble.addDroitPropriete(precedent2);
		immeuble.addDroitPropriete(nouveau2);
		immeuble.addDroitPropriete(nouveau3);
		immeuble.addDroitPropriete(nouveau4);

		final AffaireRF affaire = new AffaireRF(dateImportSuccession, immeuble);
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
		finUpdates.sort(Comparator.comparing(r -> r.getDroit().getMasterIdRF()));
		assertFinUpdate(precedent1, null, null, finUpdates.get(0));
		assertFinUpdate(precedent2, null, null, finUpdates.get(1));
		final List<Listener.DebutUpdate> debutUpdates = listener.getDebutUpdates();
		assertEquals(3, debutUpdates.size());
		debutUpdates.sort(Comparator.comparing(r -> r.getDroit().getMasterIdRF()));
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
		immeuble.addDroitPropriete(droitMichelle);
		immeuble.addDroitPropriete(droitBlaise);
		immeuble.addDroitPropriete(droitJoelle);

		final AffaireRF affaire = new AffaireRF(dateSecondImport, immeuble);
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
		immeuble.addDroitPropriete(droitAlexander);
		immeuble.addDroitPropriete(droitPatriciaCession);
		immeuble.addDroitPropriete(droitPatriciaTransfer);
		immeuble.addDroitPropriete(droitEva);

		final AffaireRF affaire = new AffaireRF(dateSecondImport, immeuble);
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
		finUpdates.sort(Comparator.comparing(r -> r.getDroit().getMasterIdRF()));
		assertFinUpdate(patricia, dateCession, "Cession", null, null, finUpdates.get(0));
		assertFinUpdate(eva, dateCession, "Cession", null, null, finUpdates.get(1));
		// les droits de Patricia et Alexander sur la communauté 'Cession' doivent être ouverts
		final List<Listener.DebutUpdate> debutUpdates = listener.getDebutUpdates();
		assertEquals(2, debutUpdates.size());
		debutUpdates.sort(Comparator.comparing(r -> r.getDroit().getMasterIdRF()));
		assertDebutUpdate(alexander, dateTransfert, "Transfert", null, null, debutUpdates.get(0));
		assertDebutUpdate(patricia, dateCession, "Cession", null, null, debutUpdates.get(1));
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
		immeuble.addDroitPropriete(precedent1);
		immeuble.addDroitPropriete(precedent2);
		immeuble.addDroitPropriete(nouveau2);
		immeuble.addDroitPropriete(nouveau3);
		immeuble.addDroitPropriete(nouveau4);

		final AffaireRF affaire = new AffaireRF(dateImportSuccession, immeuble);
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

	/**
	 * [SIFISC-26540] Ce test vérifie que la date de début métier du nouveau droit est calculée correctement lorsque :
	 * <ul>
	 *     <li>le nouveau propriétaire possède deux droits (maintenant fermés) sur l'immeuble</li>
	 *     <li>que ces anciens droits n'avaient pas les mêmes raisons d'acquisition</li>
	 *     <li>que le nouveau droit a repris l'ensemble des raisons d'acquisition (= fusion des raison d'acquisition des anciens droits)</li>
	 * </ul>
	 * A ce moment-là, l'algorithme doit scanner tous les anciens droits pour trouver la raison d'acquisition la plus récente (et non prendre
	 * le droit le plus récent).
	 */
	@Test
	public void testRefreshDatesMetierPlusieursAnciensDroitPourNouveauProprietaire() throws Exception {

		final Listener listener = new Listener();

		final RegDate dateAchat = RegDate.get(2011, 7, 11);
		final RegDate dateSuccession = RegDate.get(2014, 4, 3);
		final RegDate dateSession = RegDate.get(2016, 11, 25);

		final RegDate dateImportInitial = RegDate.get(2016, 12, 31);
		final RegDate dateImportSession = RegDate.get(2017, 3, 18);

		final PersonnePhysiqueRF helene = new PersonnePhysiqueRF();
		helene.setId(1L);
		final PersonnePhysiqueRF brigitte = new PersonnePhysiqueRF();
		brigitte.setId(2L);

		// communauté Hélène - Brigitte
		final CommunauteRF communaute = new CommunauteRF();
		communaute.setId(3L);

		//
		// Situation au 31.12.2016
		//

		// droit de propriété en copropriété de Hélène
		final DroitProprietePersonnePhysiqueRF droitHelene = new DroitProprietePersonnePhysiqueRF();
		droitHelene.setMasterIdRF("28288228");
		droitHelene.setVersionIdRF("1");
		droitHelene.setPart(new Fraction(1, 2));
		droitHelene.setDateDebut(dateImportInitial);
		droitHelene.setDateFin(dateImportSession.getOneDayBefore());
		droitHelene.setDateDebutMetier(dateAchat);
		droitHelene.setDateFinMetier(null);
		droitHelene.setMotifDebut("Achat");
		droitHelene.setMotifFin(null);
		droitHelene.setAyantDroit(helene);
		droitHelene.setRegime(COPROPRIETE);
		droitHelene.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportInitial, dateAchat, "Achat", null));

		// droit de propriété en communauté de Hélène
		final DroitProprietePersonnePhysiqueRF droitHeleneComm = new DroitProprietePersonnePhysiqueRF();
		droitHeleneComm.setMasterIdRF("382818811");
		droitHeleneComm.setVersionIdRF("1");
		droitHeleneComm.setPart(new Fraction(1, 1));
		droitHeleneComm.setDateDebut(dateImportInitial);
		droitHeleneComm.setDateFin(dateImportSession.getOneDayBefore());
		droitHeleneComm.setDateDebutMetier(dateSuccession);
		droitHeleneComm.setDateFinMetier(null);
		droitHeleneComm.setMotifDebut("Succession");
		droitHeleneComm.setMotifFin(null);
		droitHeleneComm.setAyantDroit(helene);
		droitHeleneComm.setCommunaute(communaute);
		droitHeleneComm.setRegime(COMMUNE);
		droitHeleneComm.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportInitial, dateSuccession, "Succession", null));

		// droit de propriété en communauté de Brigitte
		final DroitProprietePersonnePhysiqueRF droitBrigitteComm = new DroitProprietePersonnePhysiqueRF();
		droitBrigitteComm.setMasterIdRF("0472617182");
		droitBrigitteComm.setVersionIdRF("1");
		droitBrigitteComm.setPart(new Fraction(1, 1));
		droitBrigitteComm.setDateDebut(dateImportInitial);
		droitBrigitteComm.setDateFin(dateImportSession.getOneDayBefore());
		droitBrigitteComm.setDateDebutMetier(dateSuccession);
		droitBrigitteComm.setDateFinMetier(null);
		droitBrigitteComm.setMotifDebut("Succession");
		droitBrigitteComm.setMotifFin(null);
		droitBrigitteComm.setAyantDroit(brigitte);
		droitBrigitteComm.setCommunaute(communaute);
		droitBrigitteComm.setRegime(COMMUNE);
		droitBrigitteComm.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportInitial, dateSuccession, "Succession", null));

		final DroitProprieteCommunauteRF droitCommunaute = new DroitProprieteCommunauteRF();
		droitCommunaute.setMasterIdRF("2828828221");
		droitCommunaute.setVersionIdRF("1");
		droitCommunaute.setPart(new Fraction(1, 2));
		droitCommunaute.setDateDebut(dateImportInitial);
		droitCommunaute.setDateFin(dateImportSession.getOneDayBefore());
		droitCommunaute.setDateDebutMetier(dateSuccession);
		droitCommunaute.setDateFinMetier(null);
		droitCommunaute.setMotifDebut("Succession");
		droitCommunaute.setMotifFin(null);
		droitCommunaute.setAyantDroit(communaute);
		droitCommunaute.setRegime(COPROPRIETE);
		droitCommunaute.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportInitial, dateSuccession, "Succession", null));

		//
		// Situation au 18.03.2017
		//

		// nouveau droit de propriété individuel de Hélène
		final DroitProprietePersonnePhysiqueRF nouveauDroitHelene = new DroitProprietePersonnePhysiqueRF();
		nouveauDroitHelene.setMasterIdRF("2625656116");
		nouveauDroitHelene.setVersionIdRF("1");
		nouveauDroitHelene.setPart(new Fraction(1, 1));
		nouveauDroitHelene.setDateDebut(dateImportSession);
		nouveauDroitHelene.setDateFin(null);
		nouveauDroitHelene.setDateDebutMetier(null);
		nouveauDroitHelene.setDateFinMetier(null);
		nouveauDroitHelene.setAyantDroit(helene);
		nouveauDroitHelene.setRegime(INDIVIDUELLE);
		nouveauDroitHelene.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportSession, dateAchat, "Achat", null));
		nouveauDroitHelene.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportInitial, dateSuccession, "Succession", null));
		nouveauDroitHelene.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportSession, dateSession, "Session", null));

		final ImmeubleRF immeuble = new BienFondsRF();
		immeuble.addDroitPropriete(droitHelene);
		immeuble.addDroitPropriete(droitHeleneComm);
		immeuble.addDroitPropriete(droitBrigitteComm);
		immeuble.addDroitPropriete(droitCommunaute);
		immeuble.addDroitPropriete(nouveauDroitHelene);

		final AffaireRF affaire = new AffaireRF(dateImportSession, immeuble);
		affaire.refreshDatesMetier(listener);

		// l'ancien droit d'hélène doit être fermé à la date de session
		assertEquals(dateAchat, droitHelene.getDateDebutMetier());
		assertEquals(dateSession, droitHelene.getDateFinMetier());
		assertEquals("Achat", droitHelene.getMotifDebut());
		assertEquals("Session", droitHelene.getMotifFin());

		// les anciens droits de la communauté doivent être fermés à la date de session
		assertEquals(dateSuccession, droitHeleneComm.getDateDebutMetier());
		assertEquals(dateSuccession, droitBrigitteComm.getDateDebutMetier());
		assertEquals(dateSuccession, droitCommunaute.getDateDebutMetier());
		assertEquals("Succession", droitHeleneComm.getMotifDebut());
		assertEquals("Succession", droitBrigitteComm.getMotifDebut());
		assertEquals("Succession", droitCommunaute.getMotifDebut());
		assertEquals(dateSession, droitHeleneComm.getDateFinMetier());
		assertEquals(dateSession, droitBrigitteComm.getDateFinMetier());
		assertEquals(dateSession, droitCommunaute.getDateFinMetier());
		assertEquals("Session", droitHeleneComm.getMotifFin());
		assertEquals("Session", droitBrigitteComm.getMotifFin());
		assertEquals("Session", droitCommunaute.getMotifFin());

		assertEquals(dateSession, nouveauDroitHelene.getDateDebutMetier());
		assertEquals("Session", nouveauDroitHelene.getMotifDebut());
		assertNull(nouveauDroitHelene.getDateFinMetier());
		assertNull(nouveauDroitHelene.getMotifFin());

		// les 5 droits sont mis-à-jour
		assertEquals(0, listener.getCreated().size());
		final List<Listener.FinUpdate> finUpdates = listener.getFinUpdates();
		assertEquals(4, finUpdates.size());
		finUpdates.sort(Comparator.comparing(r -> r.getDroit().getMasterIdRF()));
		assertFinUpdate(droitBrigitteComm, null, null, finUpdates.get(0));
		assertFinUpdate(droitHelene, null, null, finUpdates.get(1));
		assertFinUpdate(droitCommunaute, null, null, finUpdates.get(2));
		assertFinUpdate(droitHeleneComm, null, null, finUpdates.get(3));
		final List<Listener.DebutUpdate> debutUpdates = listener.getDebutUpdates();
		assertEquals(1, debutUpdates.size());
		assertDebutUpdate(nouveauDroitHelene, null, null, debutUpdates.get(0));
		assertEquals(0, listener.getClosed().size());
	}

	/**
	 * [SIFISC-26690] Ce test vérifie que la date de fin métier du droit <i>de communauté</i> est calculée correctement lorsque :
	 * <ul>
	 *     <li>une communauté avec deux membres est fermée</li>
	 *     <li>que le raisons d'acquisition des droits des membres de la communauté sont reprises dans les nouveaux droits</li>
	 *     <li>qu'une nouvelle raison d'acquisition est ajoutée sur les nouveaux droits</li>
	 * </ul>
	 * A ce moment-là, la date de fin métier du droit de communauté doit être égale à la date des nouvelles raisons sur les nouveaux droits
	 * (et non pas égale à la plus ancienne date des raisons des nouveaux droits, comme c'était le cas avant la correction).
	 */
	@Test
	public void testRefreshDatesMetierDateFinDroitCommunaute() throws Exception {

		// exemple immeuble CH344583717738
		final RegDate dateSuccession = RegDate.get(1962, 11, 7);
		final RegDate dateEchange = RegDate.get(1986, 12, 30);
		final RegDate dateSuccession2 = RegDate.get(1996, 12, 18);
		final RegDate dateDonation = RegDate.get(2004, 5, 6);
		final RegDate dateChangementRegime = RegDate.get(2016, 11, 11);
		final RegDate dateDonation2 = RegDate.get(2017, 1, 10);

		final RegDate dateImportInitial = RegDate.get(2016, 12, 31);    // FIXME (msi) utiliser la valeur null pour l'import principal
		final RegDate dateImportChangementRegime = RegDate.get(2017, 1, 28);
		final RegDate dateImportDonation2 = RegDate.get(2017, 2, 25);

		final PersonnePhysiqueRF charles = new PersonnePhysiqueRF();
		charles.setId(1L);
		final PersonnePhysiqueRF wilfred = new PersonnePhysiqueRF();
		wilfred.setId(2L);
		final PersonnePhysiqueRF christian = new PersonnePhysiqueRF();
		christian.setId(3L);

		// communauté Charles - Wilfred
		final CommunauteRF communaute = new CommunauteRF();
		communaute.setId(4L);

		//
		// Situation au 31.12.2016
		//

		// droit de propriété en communauté de Charles
		final DroitProprietePersonnePhysiqueRF droitCharles1 = new DroitProprietePersonnePhysiqueRF();
		droitCharles1.setMasterIdRF("droitCharles1");
		droitCharles1.setVersionIdRF("1");
		droitCharles1.setPart(new Fraction(1, 1));
		droitCharles1.setDateDebut(dateImportInitial);
		droitCharles1.setDateFin(dateImportChangementRegime.getOneDayBefore());
		droitCharles1.setAyantDroit(charles);
		droitCharles1.setCommunaute(communaute);
		droitCharles1.setRegime(COMMUNE);
		droitCharles1.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportInitial, dateEchange, "Echange", null));
		droitCharles1.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportInitial, dateSuccession2, "Succession", null));
		droitCharles1.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportInitial, dateDonation, "Donation", null));
		communaute.addMembre(droitCharles1);

		// droit de propriété en communauté de Wilfred
		final DroitProprietePersonnePhysiqueRF droitWilfred1 = new DroitProprietePersonnePhysiqueRF();
		droitWilfred1.setMasterIdRF("droitWilfred1");
		droitWilfred1.setVersionIdRF("1");
		droitWilfred1.setPart(new Fraction(1, 1));
		droitWilfred1.setDateDebut(dateImportInitial);
		droitWilfred1.setDateFin(dateImportChangementRegime.getOneDayBefore());
		droitWilfred1.setAyantDroit(wilfred);
		droitWilfred1.setCommunaute(communaute);
		droitWilfred1.setRegime(COMMUNE);
		droitWilfred1.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportInitial, dateEchange, "Echange", null));
		droitWilfred1.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportInitial, dateSuccession2, "Succession", null));
		droitWilfred1.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportInitial, dateDonation, "Donation", null));
		communaute.addMembre(droitWilfred1);

		final DroitProprieteCommunauteRF droitCommunaute = new DroitProprieteCommunauteRF();
		droitCommunaute.setMasterIdRF("droitCommunaute");
		droitCommunaute.setVersionIdRF("1");
		droitCommunaute.setPart(new Fraction(1, 1));
		droitCommunaute.setDateDebut(dateImportInitial);
		droitCommunaute.setDateFin(dateImportChangementRegime.getOneDayBefore());
		droitCommunaute.setAyantDroit(communaute);
		droitCommunaute.setRegime(COMMUNE);

		//
		// Situation au 28.01.2017 (changement de régime : commnauté transformée en copropriété)
		//

		// droit de propriété en copropriété de Charles
		final DroitProprietePersonnePhysiqueRF droitCharles2 = new DroitProprietePersonnePhysiqueRF();
		droitCharles2.setMasterIdRF("droitCharles2");
		droitCharles2.setVersionIdRF("1");
		droitCharles2.setPart(new Fraction(1, 2));
		droitCharles2.setDateDebut(dateImportChangementRegime);
		droitCharles2.setDateFin(dateImportDonation2.getOneDayBefore());
		droitCharles2.setAyantDroit(charles);
		droitCharles2.setRegime(COPROPRIETE);
		droitCharles2.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportChangementRegime, dateSuccession, "Succession", null));
		droitCharles2.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportChangementRegime, dateSuccession2, "Succession", null));
		droitCharles2.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportChangementRegime, dateDonation, "Donation", null));
		droitCharles2.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportChangementRegime, dateChangementRegime, "Changement de régime", null));

		// droit de propriété en copropriété de Wilfred
		final DroitProprietePersonnePhysiqueRF droitWilfred2 = new DroitProprietePersonnePhysiqueRF();
		droitWilfred2.setMasterIdRF("droitWilfred2");
		droitWilfred2.setVersionIdRF("1");
		droitWilfred2.setPart(new Fraction(1, 2));
		droitWilfred2.setDateDebut(dateImportChangementRegime);
		droitWilfred2.setDateFin(null);
		droitWilfred2.setAyantDroit(wilfred);
		droitWilfred2.setRegime(COPROPRIETE);
		droitWilfred2.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportChangementRegime, dateSuccession, "Succession", null));
		droitWilfred2.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportChangementRegime, dateSuccession2, "Succession", null));
		droitWilfred2.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportChangementRegime, dateDonation, "Donation", null));
		droitWilfred2.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportChangementRegime, dateChangementRegime, "Changement de régime", null));

		//
		// Situation au 25.02.2017 (donation de la part de Charles à Christian)
		//

		// droit de propriété en copropriété de Charles
		final DroitProprietePersonnePhysiqueRF droitChristian = new DroitProprietePersonnePhysiqueRF();
		droitChristian.setMasterIdRF("droitChristian");
		droitChristian.setVersionIdRF("1");
		droitChristian.setPart(new Fraction(1, 2));
		droitChristian.setDateDebut(dateImportDonation2);
		droitChristian.setDateFin(null);
		droitChristian.setAyantDroit(christian);
		droitChristian.setRegime(COPROPRIETE);
		droitChristian.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportDonation2, dateDonation2, "Donation", null));


		final ImmeubleRF immeuble = new BienFondsRF();
		immeuble.addDroitPropriete(droitCharles1);
		immeuble.addDroitPropriete(droitCharles2);
		immeuble.addDroitPropriete(droitChristian);
		immeuble.addDroitPropriete(droitCommunaute);
		immeuble.addDroitPropriete(droitWilfred1);
		immeuble.addDroitPropriete(droitWilfred2);

		//
		// Calcul des dates métier de l'import initial
		//

		final AffaireRF importInitial = new AffaireRF(dateImportInitial, immeuble);
		final Listener listenerImportInitial = new Listener();
		importInitial.refreshDatesMetier(listenerImportInitial);

		// tous les droits de la communauté doivent avoir une date de début calculée correctement
		assertEquals(dateEchange, droitCharles1.getDateDebutMetier());
		assertNull(droitCharles1.getDateFinMetier());
		assertEquals("Echange", droitCharles1.getMotifDebut());
		assertNull(droitCharles1.getMotifFin());
		assertEquals(dateEchange, droitWilfred1.getDateDebutMetier());
		assertNull(droitWilfred1.getDateFinMetier());
		assertEquals("Echange", droitWilfred1.getMotifDebut());
		assertNull(droitWilfred1.getMotifFin());
		assertEquals(dateEchange, droitCommunaute.getDateDebutMetier());
		assertNull(droitCommunaute.getDateFinMetier());
		assertEquals("Echange", droitCommunaute.getMotifDebut());
		assertNull(droitCommunaute.getMotifFin());

		// les dates de début des 3 droits de communautés sont mises-à-jour
		assertEmpty(listenerImportInitial.getCreated());
		assertEmpty(listenerImportInitial.getFinUpdates());
		final List<Listener.DebutUpdate> debutUpdatesII = listenerImportInitial.getDebutUpdates();
		assertEquals(3, debutUpdatesII.size());
		debutUpdatesII.sort(Comparator.comparing(r -> r.getDroit().getMasterIdRF()));
		assertDebutUpdate(droitCharles1, null, null, debutUpdatesII.get(0));
		assertDebutUpdate(droitCommunaute, null, null, debutUpdatesII.get(1));
		assertDebutUpdate(droitWilfred1, null, null, debutUpdatesII.get(2));
		assertEmpty(listenerImportInitial.getClosed());

		//
		// Calcul des dates métier du changement de régime
		//

		final AffaireRF changementRegime = new AffaireRF(dateImportChangementRegime, immeuble);
		final Listener listenerChangementRegime = new Listener();
		changementRegime.refreshDatesMetier(listenerChangementRegime);

		// tous les droits de la communauté doivent être fermés à la date du changement de régime
		assertEquals(dateEchange, droitCharles1.getDateDebutMetier());
		assertEquals(dateChangementRegime, droitCharles1.getDateFinMetier());
		assertEquals("Echange", droitCharles1.getMotifDebut());
		assertEquals("Changement de régime", droitCharles1.getMotifFin());
		assertEquals(dateEchange, droitWilfred1.getDateDebutMetier());
		assertEquals(dateChangementRegime, droitWilfred1.getDateFinMetier());
		assertEquals("Echange", droitWilfred1.getMotifDebut());
		assertEquals("Changement de régime", droitWilfred1.getMotifFin());
		assertEquals(dateEchange, droitCommunaute.getDateDebutMetier());
		assertEquals(dateChangementRegime, droitCommunaute.getDateFinMetier()); // <--- cette date était mal calculée avant
		assertEquals("Echange", droitCommunaute.getMotifDebut());
		assertEquals("Changement de régime", droitCommunaute.getMotifFin());

		// les droits de copro de Charles et Wilfred doivent être ouverts
		assertEquals(dateChangementRegime, droitCharles2.getDateDebutMetier());
		assertNull(droitCharles2.getDateFinMetier());
		assertEquals("Changement de régime", droitCharles2.getMotifDebut());
		assertNull(droitCharles2.getMotifFin());
		assertEquals(dateChangementRegime, droitWilfred2.getDateDebutMetier());
		assertNull(droitWilfred2.getDateFinMetier());
		assertEquals("Changement de régime", droitWilfred2.getMotifDebut());
		assertNull(droitWilfred2.getMotifFin());

		// les dates de fin des 3 droits de communautés sont mises-à-jour
		// les dates de début des 2 droits de copropriété sont mises-à-jour
		assertEmpty(listenerImportInitial.getCreated());
		final List<Listener.FinUpdate> finUpdatesCR = listenerChangementRegime.getFinUpdates();
		assertEquals(3, finUpdatesCR.size());
		finUpdatesCR.sort(Comparator.comparing(r -> r.getDroit().getMasterIdRF()));
		assertFinUpdate(droitCharles1, null, null, finUpdatesCR.get(0));
		assertFinUpdate(droitCommunaute, null, null, finUpdatesCR.get(1));
		assertFinUpdate(droitWilfred1, null, null, finUpdatesCR.get(2));
		final List<Listener.DebutUpdate> debutUpdatesCR = listenerChangementRegime.getDebutUpdates();
		assertEquals(2, debutUpdatesCR.size());
		debutUpdatesCR.sort(Comparator.comparing(r -> r.getDroit().getMasterIdRF()));
		assertDebutUpdate(droitCharles2, null, null, debutUpdatesCR.get(0));
		assertDebutUpdate(droitWilfred2, null, null, debutUpdatesCR.get(1));
		assertEmpty(listenerImportInitial.getClosed());

		//
		// Calcul des dates métier de la donation à Christian
		//

		final AffaireRF donationChristian = new AffaireRF(dateImportDonation2, immeuble);
		final Listener listenerDonationChristian = new Listener();
		donationChristian.refreshDatesMetier(listenerDonationChristian);

		// les droits de la communauté doivent rester inchangés
		assertEquals(dateEchange, droitCharles1.getDateDebutMetier());
		assertEquals(dateChangementRegime, droitCharles1.getDateFinMetier());
		assertEquals("Echange", droitCharles1.getMotifDebut());
		assertEquals("Changement de régime", droitCharles1.getMotifFin());
		assertEquals(dateEchange, droitWilfred1.getDateDebutMetier());
		assertEquals(dateChangementRegime, droitWilfred1.getDateFinMetier());
		assertEquals("Echange", droitWilfred1.getMotifDebut());
		assertEquals("Changement de régime", droitWilfred1.getMotifFin());
		assertEquals(dateEchange, droitCommunaute.getDateDebutMetier());
		assertEquals(dateChangementRegime, droitCommunaute.getDateFinMetier());
		assertEquals("Echange", droitCommunaute.getMotifDebut());
		assertEquals("Changement de régime", droitCommunaute.getMotifFin());

		// le droit de Charles devrait être fermé et celui de Christian ouvert
		assertEquals(dateChangementRegime, droitCharles2.getDateDebutMetier());
		assertEquals(dateDonation2, droitCharles2.getDateFinMetier());
		assertEquals("Changement de régime", droitCharles2.getMotifDebut());
		assertEquals("Donation", droitCharles2.getMotifFin());

		assertEquals(dateDonation2, droitChristian.getDateDebutMetier());
		assertNull(droitChristian.getDateFinMetier());
		assertEquals("Donation", droitChristian.getMotifDebut());
		assertNull(droitChristian.getMotifFin());

		// la date de fin du droit de copropriété de Charles est mise-à-jour
		// la date de début du droit de copropriété de Christian est mise-à-jour
		assertEmpty(listenerImportInitial.getCreated());
		final List<Listener.FinUpdate> finUpdatesDC = listenerDonationChristian.getFinUpdates();
		assertEquals(1, finUpdatesDC.size());
		assertFinUpdate(droitCharles2, null, null, finUpdatesDC.get(0));
		final List<Listener.DebutUpdate> debutUpdatesDC = listenerDonationChristian.getDebutUpdates();
		assertEquals(1, debutUpdatesDC.size());
		assertDebutUpdate(droitChristian, null, null, debutUpdatesDC.get(0));
		assertEmpty(listenerImportInitial.getClosed());
	}


	/**
	 * [SIFISC-28213] Ce test vérifie que la date de début métier d'un droit est calculée correctement dans le cas où :
	 * <ul>
	 *     <li>une communauté de deux membres est remplacée par une autre communauté de deux membres</li>
	 *     <li>un membre de la première communauté et remplacé par un autre membre dans la nouvelle communauté</li>
	 *     <li>un membre apparaît donc dans les deux communautés</li>
	 *     <li>le droit de ce membre dans la première communauté et recopié tel-quel dans la seconde communauté (= même raisons d'acquisition dans les deux cas)</li>
	 * </ul>
	 * <p>
	 * <b>Cas métier:</b> CH509045438381 (achat au 22.11.2016)
	 */
	@Test
	public void testRefreshDatesMetierNouvelleCommunauteAvecRepriseUnMembreSansNouvelleRaisonAcquisition() throws Exception {

		final Listener listener = new Listener();

		final RegDate dateSuccession = RegDate.get(2013, 9, 13);
		final RegDate dateAchat = RegDate.get(2016, 11, 22);
		final RegDate dateImportInitial = RegDate.get(2016, 12, 31);
		final RegDate dateImportAchat = RegDate.get(2017, 1, 28);

		// communauté Dub Addor Andres - Sprüngli Odette
		final CommunauteRF communaute1 = new CommunauteRF();
		communaute1.setId(1L);
		final PersonnePhysiqueRF andres = new PersonnePhysiqueRF();
		andres.setId(1L);
		final PersonnePhysiqueRF odette = new PersonnePhysiqueRF();
		odette.setId(2L);

		// droit initial de Dub Addor Andres
		final DroitProprietePersonnePhysiqueRF precedent1 = new DroitProprietePersonnePhysiqueRF();
		precedent1.setMasterIdRF("28288228");
		precedent1.setVersionIdRF("1");
		precedent1.setPart(new Fraction(1, 1));
		precedent1.setDateDebut(dateImportInitial);
		precedent1.setDateFin(dateImportAchat.getOneDayBefore());
		precedent1.setAyantDroit(andres);
		precedent1.setCommunaute(communaute1);
		precedent1.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportInitial, dateSuccession, "Succession", null));

		// droit initial de Sprüngli Odette
		final DroitProprietePersonnePhysiqueRF precedent2 = new DroitProprietePersonnePhysiqueRF();
		precedent2.setMasterIdRF("382818811");
		precedent2.setVersionIdRF("1");
		precedent2.setPart(new Fraction(1, 1));
		precedent2.setDateDebut(dateImportInitial);
		precedent2.setDateFin(dateImportAchat.getOneDayBefore());
		precedent2.setAyantDroit(odette);
		precedent2.setCommunaute(communaute1);
		precedent2.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportInitial, dateSuccession, "Succession", null));

		// le droit de la première communauté elle-même
		final DroitProprieteCommunauteRF precedent3 = new DroitProprieteCommunauteRF();
		precedent3.setMasterIdRF("27827827");
		precedent3.setVersionIdRF("1");
		precedent3.setPart(new Fraction(1, 1));
		precedent3.setDateDebut(dateImportInitial);
		precedent3.setDateFin(dateImportAchat.getOneDayBefore());
		precedent3.setAyantDroit(communaute1);

		// communauté Sprüngli Carlos - Sprüngli Odette
		final CommunauteRF communaute2 = new CommunauteRF();
		communaute2.setId(2L);
		final PersonnePhysiqueRF carlos = new PersonnePhysiqueRF();
		carlos.setId(3L);

		// droit de Sprüngli Carlos (nouveau)
		final DroitProprietePersonnePhysiqueRF nouveau1 = new DroitProprietePersonnePhysiqueRF();
		nouveau1.setMasterIdRF("372828");
		nouveau1.setVersionIdRF("1");
		nouveau1.setPart(new Fraction(1, 1));
		nouveau1.setDateDebut(dateImportAchat);
		nouveau1.setAyantDroit(carlos);
		nouveau1.setCommunaute(communaute2);
		nouveau1.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportAchat, dateAchat, "Achat", null));

		// droit de Sprüngli Odette (copie de l'ancien)
		final DroitProprietePersonnePhysiqueRF nouveau2 = new DroitProprietePersonnePhysiqueRF();
		nouveau2.setMasterIdRF("382818811");
		nouveau2.setVersionIdRF("1");
		nouveau2.setPart(new Fraction(1, 1));
		nouveau2.setDateDebut(dateImportAchat);
		nouveau2.setAyantDroit(odette);
		nouveau2.setCommunaute(communaute1);
		nouveau2.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportInitial, dateSuccession, "Succession", null));

		// le droit de la seconde communauté elle-même
		final DroitProprieteCommunauteRF nouveau3 = new DroitProprieteCommunauteRF();
		nouveau3.setMasterIdRF("8010102");
		nouveau3.setVersionIdRF("1");
		nouveau3.setPart(new Fraction(1, 1));
		nouveau3.setDateDebut(dateImportAchat);
		nouveau3.setDateFin(null);
		nouveau3.setAyantDroit(communaute2);

		// on recalcule les dates métiers des droits
		final ImmeubleRF immeuble = new BienFondsRF();
		immeuble.addDroitPropriete(precedent1);
		immeuble.addDroitPropriete(precedent2);
		immeuble.addDroitPropriete(precedent3);
		immeuble.addDroitPropriete(nouveau1);
		immeuble.addDroitPropriete(nouveau2);
		immeuble.addDroitPropriete(nouveau3);

		final AffaireRF affaire = new AffaireRF(dateImportAchat, immeuble);
		affaire.refreshDatesMetier(listener);

		// droits de la première communauté
		assertNull(precedent1.getDateDebutMetier());
		assertNull(precedent2.getDateDebutMetier());
		assertNull(precedent3.getDateDebutMetier());
		assertNull(precedent1.getMotifDebut());
		assertNull(precedent2.getMotifDebut());
		assertNull(precedent3.getMotifDebut());
		assertEquals(dateAchat, precedent1.getDateFinMetier()); // <-- les date de fin métier sont renseignées
		assertEquals(dateAchat, precedent2.getDateFinMetier());
		assertEquals(dateAchat, precedent3.getDateFinMetier());
		assertEquals("Vente", precedent1.getMotifFin());
		assertEquals("Vente", precedent2.getMotifFin());
		assertEquals("Vente", precedent3.getMotifFin());

		// droits de la seconde communauté
		assertEquals(dateAchat, nouveau1.getDateDebutMetier());
		assertEquals(dateAchat, nouveau2.getDateDebutMetier()); // <-- malgré l'absence de nouvelle raison d'acquisition, la raison d'acquisition "Achat" est utilisée (recopiée du droit nouveau1)
		assertEquals(dateAchat, nouveau3.getDateDebutMetier());
		assertEquals("Achat", nouveau1.getMotifDebut());
		assertEquals("Achat", nouveau2.getMotifDebut());
		assertEquals("Achat", nouveau3.getMotifDebut());
		assertNull(nouveau1.getDateFinMetier());
		assertNull(nouveau2.getDateFinMetier());
		assertNull(nouveau3.getDateFinMetier());
		assertNull(nouveau1.getMotifFin());
		assertNull(nouveau2.getMotifFin());
		assertNull(nouveau3.getMotifFin());

		// les 6 droits sont mis-à-jour
		assertEquals(0, listener.getCreated().size());
		final List<Listener.FinUpdate> finUpdates = listener.getFinUpdates();
		assertEquals(3, finUpdates.size());
		finUpdates.sort(Comparator.comparing(r -> r.getDroit().getMasterIdRF()));
		assertFinUpdate(precedent3, null, null, finUpdates.get(0));
		assertFinUpdate(precedent1, null, null, finUpdates.get(1));
		assertFinUpdate(precedent2, null, null, finUpdates.get(2));
		final List<Listener.DebutUpdate> debutUpdates = listener.getDebutUpdates();
		assertEquals(3, debutUpdates.size());
		debutUpdates.sort(Comparator.comparing(r -> r.getDroit().getMasterIdRF()));
		assertDebutUpdate(nouveau1, null, null, debutUpdates.get(0));
		assertDebutUpdate(nouveau2, null, null, debutUpdates.get(1));
		assertDebutUpdate(nouveau3, null, null, debutUpdates.get(2));
		assertEquals(0, listener.getClosed().size());
	}

	/**
	 * [SIFISC-29326] Ce test vérifie que la date de début métier d'un droit est bien déduite de la nouvelle raison d'acquisition pour un droit qui évolue (c'est-à-dire qu'il existe un droit précédent avec le même masterId).
	 */
	@Test
	public void testApplyDonationEntreDeuxCoproprietaires() throws Exception {

		// situation initiale

		final RegDate datePremiereDonation = date(2017, 5, 5);
		final RegDate dateImportPremiereDonation = date(2017, 7, 1);

		final PersonnePhysiqueRF jacques = new PersonnePhysiqueRF();
		jacques.setId(1L);
		final PersonnePhysiqueRF fabrice = new PersonnePhysiqueRF();
		fabrice.setId(2L);
		final PersonnePhysiqueRF vincent = new PersonnePhysiqueRF();
		vincent.setId(3L);

		final DroitProprieteRF droitFabrice = newDroitPP("fabrice", "1", null, null, date(2003, 11, 10), null, "Achat", null, new Fraction(50, 100), COPROPRIETE, fabrice);
		droitFabrice.addRaisonAcquisition(newRaisonAcquisitionRF(null, date(2003, 11, 10), "Achat", new IdentifiantAffaireRF(13, 2003, 1573, 0)));

		final DroitProprieteRF droitJacques1 = newDroitPP("jacques", "1", null, dateImportPremiereDonation.getOneDayBefore(), date(2003, 11, 10), date(2008, 7, 22), "Achat", "Donation", new Fraction(14, 100), COPROPRIETE, jacques);
		droitJacques1.addRaisonAcquisition(newRaisonAcquisitionRF(null, date(2003, 11, 10), "Achat", new IdentifiantAffaireRF(13, 2003, 1573, 0)));

		final DroitProprieteRF droitVincent1 = newDroitPP("vincent", "1", null, dateImportPremiereDonation.getOneDayBefore(), date(2008, 7, 22), datePremiereDonation, "Donation", "Donation", new Fraction(36, 100), COPROPRIETE, vincent);
		droitVincent1.addRaisonAcquisition(newRaisonAcquisitionRF(null, date(2008, 7, 22), "Donation", new IdentifiantAffaireRF(13, 2008, 1057, 0)));
		droitVincent1.addRaisonAcquisition(newRaisonAcquisitionRF(null, date(2016, 10, 9), "Donation", new IdentifiantAffaireRF(6, 2016, 10347, 0)));

		// première donation (déjà traitée) de 4/100 de Jacques à Vincent - 05.05.2017
		final DroitProprieteRF droitJacques2 = newDroitPP("jacques", "2", dateImportPremiereDonation, null, datePremiereDonation, null, "Donation", null, new Fraction(10, 100), COPROPRIETE, jacques);
		droitJacques2.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportPremiereDonation, date(2003, 11, 10), "Achat", new IdentifiantAffaireRF(13, 2003, 1573, 0)));

		final DroitProprieteRF droitVincent2 = newDroitPP("vincent", "2", dateImportPremiereDonation, null, datePremiereDonation, null, "Donation", null, new Fraction(40, 100), COPROPRIETE, vincent);
		droitVincent2.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportPremiereDonation, date(2008, 7, 22), "Donation", new IdentifiantAffaireRF(13, 2008, 1057, 0)));
		droitVincent2.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportPremiereDonation, date(2016, 10, 9), "Donation", new IdentifiantAffaireRF(6, 2016, 10347, 0)));
		droitVincent2.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportPremiereDonation, datePremiereDonation, "Donation", new IdentifiantAffaireRF(6, 2017, 4277, 0)));

		final ImmeubleRF immeuble = new BienFondsRF();
		immeuble.addDroitPropriete(droitFabrice);
		immeuble.addDroitPropriete(droitJacques1);
		immeuble.addDroitPropriete(droitJacques2);
		immeuble.addDroitPropriete(droitVincent1);
		immeuble.addDroitPropriete(droitVincent2);

		// seconde donation (à traiter) de 4/100 de Jacques à Vincent - 14.05.2018
		final RegDate dateSecondeDonation = date(2018, 5, 14);
		final RegDate dateImportSecondeDonation = date(2018, 6, 1);

		final DroitProprieteRF droitJacques3 = newDroitPP("jacques", "3", dateImportSecondeDonation, null, null, null, null, null, new Fraction(6, 100), COPROPRIETE, jacques);
		droitJacques3.addRaisonAcquisition(newRaisonAcquisitionRF(null, date(2003, 11, 10), "Achat", null));

		final DroitProprieteRF droitVincent3 = newDroitPP("vincent", "3", dateImportSecondeDonation, null, null, null, null, null, new Fraction(44, 100), COPROPRIETE, vincent);
		droitVincent3.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportSecondeDonation, date(2008, 7, 22), "Donation", new IdentifiantAffaireRF(13, 2008, 1057, 0)));
		droitVincent3.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportSecondeDonation, date(2016, 10, 9), "Donation", new IdentifiantAffaireRF(6, 2016, 10347, 0)));
		droitVincent3.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportSecondeDonation, datePremiereDonation, "Donation", new IdentifiantAffaireRF(6, 2017, 4277, 0)));
		droitVincent3.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportSecondeDonation, dateSecondeDonation, "Donation", new IdentifiantAffaireRF(6, 2018, 4539, 0)));   // <--- nouvelle raison d'acquisition

		// traitement de l'affaire
		final List<DroitProprieteRF> aOuvrir = Arrays.asList(droitJacques3, droitVincent3);
		final List<DroitProprieteRF> aFermer = Arrays.asList(droitJacques2, droitVincent2);

		final AffaireRF affaire = new AffaireRF(dateImportSecondeDonation, immeuble);
		affaire.apply(new MockDroitRFDAO(), aOuvrir, Collections.emptyList(), aFermer, null);

		// ce droit reste inchangé car il s'agit d'un ancien import
		assertEquals(date(2003, 11, 10), droitJacques1.getDateDebutMetier());
		assertEquals(date(2008, 7, 22), droitJacques1.getDateFinMetier());
		assertEquals("Achat", droitJacques1.getMotifDebut());
		assertEquals("Donation", droitJacques1.getMotifFin());
		assertFraction(14, 100, droitJacques1.getPart());

		// la date de fin métier de ce droit doit être renseignée
		assertEquals(datePremiereDonation, droitJacques2.getDateDebutMetier());
		assertEquals(dateSecondeDonation, droitJacques2.getDateFinMetier());
		assertEquals("Donation", droitJacques2.getMotifDebut());
		assertEquals("Donation", droitJacques2.getMotifFin());
		assertFraction(10, 100, droitJacques2.getPart());

		// la date de début métier de ce droit doit être renseignée
		assertEquals(dateSecondeDonation, droitJacques3.getDateDebutMetier());
		assertNull(droitJacques3.getDateFinMetier());
		assertEquals("Donation", droitJacques3.getMotifDebut());
		assertNull(droitJacques3.getMotifFin());
		assertFraction(6, 100, droitJacques3.getPart());

		// ce droit reste inchangé car il s'agit d'un ancien import
		assertEquals(date(2008, 7, 22), droitVincent1.getDateDebutMetier());
		assertEquals(datePremiereDonation, droitVincent1.getDateFinMetier());
		assertEquals("Donation", droitVincent1.getMotifDebut());
		assertEquals("Donation", droitVincent1.getMotifFin());
		assertFraction(36, 100, droitVincent1.getPart());

		// la date de fin métier de ce droit doit être renseignée
		assertEquals(datePremiereDonation, droitVincent2.getDateDebutMetier());
		assertEquals(dateSecondeDonation, droitVincent2.getDateFinMetier());
		assertEquals("Donation", droitVincent2.getMotifDebut());
		assertEquals("Donation", droitVincent2.getMotifFin());
		assertFraction(40, 100, droitVincent2.getPart());

		// la date de début métier de ce droit doit être renseignée
		assertEquals(dateSecondeDonation, droitVincent3.getDateDebutMetier());
		assertNull(droitVincent3.getDateFinMetier());
		assertEquals("Donation", droitVincent3.getMotifDebut());
		assertNull(droitVincent3.getMotifFin());
		assertFraction(44, 100, droitVincent3.getPart());
	}

	/**
	 * [SIFISC-29326] Ce test vérifie que la date de début métier d'un droit est bien déduite de la nouvelle raison d'acquisition pour un droit qui évolue (c'est-à-dire qu'il existe un droit précédent avec le même masterId).
	 */
	@Test
	public void testRefreshDatesMetierDonationEntreDeuxCoproprietaires() throws Exception {

		// première donation (déjà traitée) de 4/100 de Jacques à Vincent - 05.05.2017
		final RegDate datePremiereDonation = date(2017, 5, 5);
		final RegDate dateImportPremiereDonation = date(2017, 7, 1);

		// seconde donation (à traiter) de 4/100 de Jacques à Vincent - 14.05.2018
		final RegDate dateSecondeDonation = date(2018, 5, 14);
		final RegDate dateImportSecondeDonation = date(2018, 6, 1);

		final PersonnePhysiqueRF jacques = new PersonnePhysiqueRF();
		jacques.setId(1L);
		final PersonnePhysiqueRF fabrice = new PersonnePhysiqueRF();
		fabrice.setId(2L);
		final PersonnePhysiqueRF vincent = new PersonnePhysiqueRF();
		vincent.setId(3L);

		// situation initiale (juste)
		final DroitProprieteRF droitFabrice = newDroitPP("fabrice", "1", null, null, date(2003, 11, 10), null, "Achat", null, new Fraction(50, 100), COPROPRIETE, fabrice);
		droitFabrice.addRaisonAcquisition(newRaisonAcquisitionRF(null, date(2003, 11, 10), "Achat", new IdentifiantAffaireRF(13, 2003, 1573, 0)));

		final DroitProprieteRF droitJacques1 = newDroitPP("jacques", "1", null, dateImportPremiereDonation.getOneDayBefore(), date(2003, 11, 10), datePremiereDonation, "Achat", "Donation", new Fraction(14, 100), COPROPRIETE, jacques);
		droitJacques1.addRaisonAcquisition(newRaisonAcquisitionRF(null, date(2003, 11, 10), "Achat", new IdentifiantAffaireRF(13, 2003, 1573, 0)));

		final DroitProprieteRF droitVincent1 = newDroitPP("vincent", "1", null, dateImportPremiereDonation.getOneDayBefore(), date(2008, 7, 22), datePremiereDonation, "Donation", "Donation", new Fraction(36, 100), COPROPRIETE, vincent);
		droitVincent1.addRaisonAcquisition(newRaisonAcquisitionRF(null, date(2008, 7, 22), "Donation", new IdentifiantAffaireRF(13, 2008, 1057, 0)));
		droitVincent1.addRaisonAcquisition(newRaisonAcquisitionRF(null, date(2016, 10, 9), "Donation", new IdentifiantAffaireRF(6, 2016, 10347, 0)));

		// première donation (déjà traitée) de 4/100 de Jacques à Vincent - 05.05.2017
		final DroitProprieteRF droitJacques2 =
				newDroitPP("jacques", "2", dateImportPremiereDonation, dateImportSecondeDonation.getOneDayBefore(), datePremiereDonation, dateSecondeDonation, "Donation", "Donation", new Fraction(10, 100), COPROPRIETE, jacques);
		droitJacques2.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportPremiereDonation, date(2003, 11, 10), "Achat", new IdentifiantAffaireRF(13, 2003, 1573, 0)));

		final DroitProprieteRF droitVincent2 =
				newDroitPP("vincent", "2", dateImportPremiereDonation, dateImportSecondeDonation.getOneDayBefore(), datePremiereDonation, dateSecondeDonation, "Donation", "Donation", new Fraction(40, 100), COPROPRIETE, vincent);
		droitVincent2.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportPremiereDonation, date(2008, 7, 22), "Donation", new IdentifiantAffaireRF(13, 2008, 1057, 0)));
		droitVincent2.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportPremiereDonation, date(2016, 10, 9), "Donation", new IdentifiantAffaireRF(6, 2016, 10347, 0)));
		droitVincent2.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportPremiereDonation, datePremiereDonation, "Donation", new IdentifiantAffaireRF(6, 2017, 4277, 0)));

		// seconde donation (à traiter) de 4/100 de Jacques à Vincent - 14.05.2018
		final DroitProprieteRF droitJacques3 = newDroitPP("jacques", "3", dateImportSecondeDonation, null, dateSecondeDonation, null, "Donation", null, new Fraction(6, 100), COPROPRIETE, jacques);
		droitJacques3.addRaisonAcquisition(newRaisonAcquisitionRF(null, date(2003, 11, 10), "Achat", null));

		final DroitProprieteRF droitVincent3 = newDroitPP("vincent", "3", dateImportSecondeDonation, null, dateSecondeDonation, null, "Donation", null, new Fraction(44, 100), COPROPRIETE, vincent);
		droitVincent3.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportSecondeDonation, date(2008, 7, 22), "Donation", new IdentifiantAffaireRF(13, 2008, 1057, 0)));
		droitVincent3.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportSecondeDonation, date(2016, 10, 9), "Donation", new IdentifiantAffaireRF(6, 2016, 10347, 0)));
		droitVincent3.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportSecondeDonation, datePremiereDonation, "Donation", new IdentifiantAffaireRF(6, 2017, 4277, 0)));
		droitVincent3.addRaisonAcquisition(newRaisonAcquisitionRF(dateImportSecondeDonation, dateSecondeDonation, "Donation", new IdentifiantAffaireRF(6, 2018, 4539, 0)));

		final ImmeubleRF immeuble = new BienFondsRF();
		immeuble.addDroitPropriete(droitFabrice);
		immeuble.addDroitPropriete(droitJacques1);
		immeuble.addDroitPropriete(droitJacques2);
		immeuble.addDroitPropriete(droitJacques3);
		immeuble.addDroitPropriete(droitVincent1);
		immeuble.addDroitPropriete(droitVincent2);
		immeuble.addDroitPropriete(droitVincent3);

		// on rafraîchit les dates métier pour la première donation
		final Listener listener = new Listener();
		final AffaireRF premierDonation = new AffaireRF(dateImportPremiereDonation, immeuble);
		premierDonation.refreshDatesMetier(listener);

		// aucun changement
		assertEquals(date(2003, 11, 10), droitJacques1.getDateDebutMetier());
		assertEquals(datePremiereDonation, droitJacques1.getDateFinMetier());
		assertEquals("Achat", droitJacques1.getMotifDebut());
		assertEquals("Donation", droitJacques1.getMotifFin());
		assertFraction(14, 100, droitJacques1.getPart());

		assertEquals(datePremiereDonation, droitJacques2.getDateDebutMetier());
		assertEquals(dateSecondeDonation, droitJacques2.getDateFinMetier());
		assertEquals("Donation", droitJacques2.getMotifDebut());
		assertEquals("Donation", droitJacques2.getMotifFin());
		assertFraction(10, 100, droitJacques2.getPart());

		assertEquals(date(2008, 7, 22), droitVincent1.getDateDebutMetier());
		assertEquals(datePremiereDonation, droitVincent1.getDateFinMetier());
		assertEquals("Donation", droitVincent1.getMotifDebut());
		assertEquals("Donation", droitVincent1.getMotifFin());
		assertFraction(36, 100, droitVincent1.getPart());

		assertEquals(datePremiereDonation, droitVincent2.getDateDebutMetier());
		assertEquals(dateSecondeDonation, droitVincent2.getDateFinMetier());
		assertEquals("Donation", droitVincent2.getMotifDebut());
		assertEquals("Donation", droitVincent2.getMotifFin());
		assertFraction(40, 100, droitVincent2.getPart());

		// tout était déjà à jour
		assertEquals(0, listener.getCreated().size());
		assertEquals(0, listener.getDebutUpdates().size());
		assertEquals(0, listener.getFinUpdates().size());
		assertEquals(0, listener.getClosed().size());

		// on rafraîchit les dates métier pour la seconde donation
		final AffaireRF secondeDonation = new AffaireRF(dateImportSecondeDonation, immeuble);
		secondeDonation.refreshDatesMetier(listener);

		// aucun changement
		assertEquals(dateSecondeDonation, droitJacques3.getDateDebutMetier());
		assertNull(droitJacques3.getDateFinMetier());
		assertEquals("Donation", droitJacques3.getMotifDebut());
		assertNull(droitJacques3.getMotifFin());
		assertFraction(6, 100, droitJacques3.getPart());

		assertEquals(dateSecondeDonation, droitVincent3.getDateDebutMetier());
		assertNull(droitVincent3.getDateFinMetier());
		assertEquals("Donation", droitVincent3.getMotifDebut());
		assertNull(droitVincent3.getMotifFin());
		assertFraction(44, 100, droitVincent3.getPart());

		// tout était déjà à jour
		assertEquals(0, listener.getCreated().size());
		assertEquals(0, listener.getDebutUpdates().size());
		assertEquals(0, listener.getFinUpdates().size());
		assertEquals(0, listener.getClosed().size());
	}

	/**
	 * [SIFISC-29326] Ce test vérifie que les dates métier sont bien calculées lors du traitement des mutations sur des communautés complexes (voir descriptifs dans le test), notamment dans le cas où un membre renonce à son droit et où une nouvelle
	 * raisons d'acquisition est ajoutée sur un seul des membres restants .
	 */
	@Test
	public void testApplyCommunautePlusieursPartages() {

		final PersonnePhysiqueRF alain = new PersonnePhysiqueRF();
		alain.setId(1L);
		final PersonnePhysiqueRF amandine = new PersonnePhysiqueRF();
		amandine.setId(2L);
		final PersonnePhysiqueRF cornelia = new PersonnePhysiqueRF();
		cornelia.setId(3L);
		final PersonnePhysiqueRF chloe = new PersonnePhysiqueRF();
		chloe.setId(4L);

		final CommunauteRF communaute1 = new CommunauteRF();
		communaute1.setId(10L);

		final CommunauteRF communaute2 = new CommunauteRF();
		communaute2.setId(11L);

		final CommunauteRF communaute3 = new CommunauteRF();
		communaute3.setId(12L);

		// historique des changements :
		//
		//  | date import | date métier | commentaire
		//  +-------------+-------------+----------------------------------------------------------------------------------------------------------------------------------
		//  |     -       | 07.05.1971  | Import initial, deux communautés (Chloé toute seule) et (Alain + Amandine + Cornélia + Chloé) sont créées
		//  | 07.01.2017  | 19.12.2016  | Décès de Chloé, fermeture des deux communautés existantes et ouverture d'une troisième communauté (Alain + Amandine + Cornélia)
		//  | 11.02.2018  | 15.12.2017  | Partage, Cornélia renonce à sa part au profit d'Amandine
		//  | 24.06.2018  | 31.05.2018  | Partage, Amandine devient la seule propriétaire


		// import initial  --------------------------------

		final DroitProprieteRF droitCommunaute1 = newDroitComm("1f109152381059670138105fe79f3627", "1f1091523810596701381062e8971959",
		                                                       null, null,
		                                                       null, null,
		                                                       null, null, new Fraction(1, 1), COMMUNE, communaute1);

		final DroitProprieteRF droitChloe1 = newDroitPP("1f109152381059670138105f83310b45", "1f1091523810596701381062e8971959",
		                                                null, null,
		                                                null, null,
		                                                null, null, new Fraction(1, 1), COMMUNE, chloe, communaute1);
		droitChloe1.addRaisonAcquisition(newRaisonAcquisitionRF(null, date(1971, 5, 7), "Achat", new IdentifiantAffaireRF(12, "132394")));


		final DroitProprieteRF droitCommunaute2 = newDroitComm("8af806fa3d25b95e013e17b735d60ca2", "1f1091523810596701381062e8971959",
		                                                       null, null,
		                                                       null, null,
		                                                       null, null, new Fraction(1, 1), COMMUNE, communaute2);

		final DroitProprieteRF droitAlain1 = newDroitPP("8af806fa3d25b95e013e17b735d70ca4", "1f1091523810596701381062e8971959",
		                                             null, null,
		                                             null, null,
		                                             null, null, new Fraction(1, 1), COMMUNE, alain, communaute2);
		droitAlain1.addRaisonAcquisition(newRaisonAcquisitionRF(null, date(2013, 4, 11), "Succession", new IdentifiantAffaireRF(12, 2013, 1848, 0)));

		final DroitProprieteRF droitAmandine1 = newDroitPP("8af806fa3d25b95e013e17b735d70ca5", "1f1091523810596701381062e8971959",
		                                             null, null,
		                                             null, null,
		                                             null, null, new Fraction(1, 1), COMMUNE, amandine, communaute2);
		droitAmandine1.addRaisonAcquisition(newRaisonAcquisitionRF(null, date(2013, 4, 11), "Succession", new IdentifiantAffaireRF(12, 2013, 1848, 0)));

		final DroitProprieteRF droitCornelia1 = newDroitPP("8af806fa3d25b95e013e17b735d70ca6", "1f1091523810596701381062e8971959",
		                                             null, null,
		                                             null, null,
		                                             null, null, new Fraction(1, 1), COMMUNE, cornelia, communaute2);
		droitCornelia1.addRaisonAcquisition(newRaisonAcquisitionRF(null, date(2013, 4, 11), "Succession", new IdentifiantAffaireRF(12, 2013, 1848, 0)));

		final DroitProprieteRF droitChloe2 = newDroitPP("8af806fa3d25b95e013e17b735d70ca3", "1f1091523810596701381062e8971959",
		                                               null, null,
		                                               null, null,
		                                               null, null, new Fraction(1, 1), COMMUNE, chloe, communaute2);
		droitChloe2.addRaisonAcquisition(newRaisonAcquisitionRF(null, date(2013, 4, 11), "Succession", new IdentifiantAffaireRF(12, 2013, 1848, 0)));

		final ImmeubleRF immeuble = new BienFondsRF();
		immeuble.setDroitsPropriete(new HashSet<>());

		// on applique les changements
		final Listener listener = new Listener();
		final AffaireRF importInitial = new AffaireRF(null, immeuble);
		importInitial.apply(new MockDroitRFDAO(),
		                    Arrays.asList(droitCommunaute1, droitChloe1, droitCommunaute2, droitAlain1, droitAmandine1, droitCornelia1, droitChloe2),
		                    Collections.emptyList(),
		                    Collections.emptyList(), listener);

		// les dates de début sont bien calculées
		assertEquals(date(1971, 5, 7), droitCommunaute1.getDateDebutMetier());
		assertEquals(date(1971, 5, 7), droitChloe1.getDateDebutMetier());
		assertEquals(date(1971, 5, 7), droitCommunaute2.getDateDebutMetier());  // TODO (msi) cette date me semble fausse, elle devrait valoir date(2013, 4, 11) plutôt...
		assertEquals(date(2013, 4, 11), droitAlain1.getDateDebutMetier());
		assertEquals(date(2013, 4, 11), droitAmandine1.getDateDebutMetier());
		assertEquals(date(2013, 4, 11), droitCornelia1.getDateDebutMetier());
		assertEquals(date(2013, 4, 11), droitChloe2.getDateDebutMetier());

		assertEquals("Achat", droitCommunaute1.getMotifDebut());
		assertEquals("Achat", droitChloe1.getMotifDebut());
		assertEquals("Achat", droitCommunaute2.getMotifDebut());
		assertEquals("Succession", droitAlain1.getMotifDebut());
		assertEquals("Succession", droitAmandine1.getMotifDebut());
		assertEquals("Succession", droitCornelia1.getMotifDebut());
		assertEquals("Succession", droitChloe2.getMotifDebut());


		// décès de Chloé --------------------------------

		final DroitProprieteRF droitCommunaute3 = newDroitComm("8af8064e58e34c9601596981b010113d", "8af8064e58e34c9601596981b00f113a",
		                                                       date(2017, 1, 7), null,
		                                                       null, null,
		                                                       null, null, new Fraction(1, 1), COMMUNE, communaute3);

		final DroitProprieteRF droitAlain2 = newDroitPP("8af8064e58e34c9601596981d9121146", "8af8064e58e34c9601596981b00f113a",
		                                               date(2017, 1, 7), null,
		                                               null, null,
		                                               null, null, new Fraction(1, 1), COMMUNE, alain, communaute3);
		droitAlain2.addRaisonAcquisition(newRaisonAcquisitionRF(date(2017, 1, 7), date(2013, 4, 11), "Succession", new IdentifiantAffaireRF(12, 2013, 1848, 0)));
		droitAlain2.addRaisonAcquisition(newRaisonAcquisitionRF(date(2017, 1, 7), date(2016, 12, 19), "Succession", new IdentifiantAffaireRF(12, 2016, 7213, 0)));

		final DroitProprieteRF droitAmandine2 = newDroitPP("8af8064e58e34c9601596981d9111144", "8af8064e58e34c9601596981b00f113a",
		                                               date(2017, 1, 7), null,
		                                               null, null,
		                                               null, null, new Fraction(1, 1), COMMUNE, amandine, communaute3);
		droitAmandine2.addRaisonAcquisition(newRaisonAcquisitionRF(date(2017, 1, 7), date(2013, 4, 11), "Succession", new IdentifiantAffaireRF(12, 2013, 1848, 0)));
		droitAmandine2.addRaisonAcquisition(newRaisonAcquisitionRF(date(2017, 1, 7), date(2016, 12, 19), "Succession", new IdentifiantAffaireRF(12, 2016, 7213, 0)));


		final DroitProprieteRF droitCornelia2 = newDroitPP("8af8064e58e34c9601596981d9121148", "8af8064e58e34c9601596981b00f113a",
		                                               date(2017, 1, 7), date(2018, 2, 10),
		                                               null, null,
		                                               null, null, new Fraction(1, 1), COMMUNE, cornelia, communaute3);
		droitCornelia2.addRaisonAcquisition(newRaisonAcquisitionRF(date(2017, 1, 7), date(2013, 4, 11), "Succession", new IdentifiantAffaireRF(12, 2013, 1848, 0)));
		droitCornelia2.addRaisonAcquisition(newRaisonAcquisitionRF(date(2017, 1, 7), date(2016, 12, 19), "Succession", new IdentifiantAffaireRF(12, 2016, 7213, 0)));

		// on applique les changements
		final AffaireRF decesChloe = new AffaireRF(date(2017, 1, 7), immeuble);
		decesChloe.apply(new MockDroitRFDAO(), Arrays.asList(droitCommunaute3, droitAlain2, droitAmandine2, droitCornelia2),
		                    Collections.emptyList(),
		                    Arrays.asList(droitCommunaute1, droitChloe1, droitCommunaute2, droitAlain1, droitAmandine1, droitCornelia1, droitChloe2), listener);


		// les droits initiaux doivent être fermés
		assertEquals(date(2017, 1, 6), droitCommunaute1.getDateFin());
		assertEquals(date(2017, 1, 6), droitChloe1.getDateFin());
		assertEquals(date(2017, 1, 6), droitCommunaute2.getDateFin());
		assertEquals(date(2017, 1, 6), droitAlain1.getDateFin());
		assertEquals(date(2017, 1, 6), droitAmandine1.getDateFin());
		assertEquals(date(2017, 1, 6), droitCornelia1.getDateFin());
		assertEquals(date(2017, 1, 6), droitChloe2.getDateFin());

		assertEquals(date(2016, 12, 19), droitCommunaute1.getDateFinMetier());
		assertEquals(date(2016, 12, 19), droitChloe1.getDateFinMetier());
		assertEquals(date(2016, 12, 19), droitCommunaute2.getDateFinMetier());
		assertEquals(date(2016, 12, 19), droitAlain1.getDateFinMetier());
		assertEquals(date(2016, 12, 19), droitAmandine1.getDateFinMetier());
		assertEquals(date(2016, 12, 19), droitCornelia1.getDateFinMetier());
		assertEquals(date(2016, 12, 19), droitChloe2.getDateFinMetier());

		assertEquals("Succession", droitCommunaute1.getMotifFin());
		assertEquals("Succession", droitChloe1.getMotifFin());
		assertEquals("Succession", droitCommunaute2.getMotifFin());
		assertEquals("Succession", droitAlain1.getMotifFin());
		assertEquals("Succession", droitAmandine1.getMotifFin());
		assertEquals("Succession", droitCornelia1.getMotifFin());
		assertEquals("Succession", droitChloe2.getMotifFin());

		// les dates de début des nouveaux droits doivent être calculés

		assertEquals(date(2016, 12, 19), droitCommunaute3.getDateDebutMetier());
		assertEquals(date(2016, 12, 19), droitAlain2.getDateDebutMetier());
		assertEquals(date(2016, 12, 19), droitAmandine2.getDateDebutMetier());
		assertEquals(date(2016, 12, 19), droitCornelia2.getDateDebutMetier());

		assertEquals("Succession", droitCommunaute3.getMotifDebut());
		assertEquals("Succession", droitAlain2.getMotifDebut());
		assertEquals("Succession", droitAmandine2.getMotifDebut());
		assertEquals("Succession", droitCornelia2.getMotifDebut());


		// renoncement de Cornélia --------------------------------

		final DroitProprieteRF droitAmandine2Updated = newDroitPP("8af8064e58e34c9601596981d9111144", "8af8064e58e34c9601596981b00f113a",
		                                                          null, null,
		                                                          null, null,
		                                                          null, null, new Fraction(1, 1), COMMUNE, amandine, communaute3);
		droitAmandine2Updated.addRaisonAcquisition(newRaisonAcquisitionRF(null, date(2013, 4, 11), "Succession", new IdentifiantAffaireRF(12, 2013, 1848, 0)));
		droitAmandine2Updated.addRaisonAcquisition(newRaisonAcquisitionRF(null, date(2016, 12, 19), "Succession", new IdentifiantAffaireRF(12, 2016, 7213, 0)));
		droitAmandine2Updated.addRaisonAcquisition(newRaisonAcquisitionRF(null, date(2017, 12, 15), "Partage", new IdentifiantAffaireRF(12, 2017, 10336, 0)));

		// on applique les changements
		final AffaireRF renoncementCornelia = new AffaireRF(date(2018, 2, 11), immeuble);

		renoncementCornelia.apply(new MockDroitRFDAO(),
		                          Collections.emptyList(),
		                          Collections.singletonList(Pair.of(droitAmandine2Updated, droitAmandine2)),
		                          Collections.singletonList(droitCornelia1), listener);


		// le droit de Cornélia doit être fermé
		assertEquals(date(2018, 2, 10), droitCornelia1.getDateFin());
		assertEquals(date(2017, 12, 15), droitCornelia1.getDateFinMetier());
		assertEquals("Partage", droitCornelia1.getMotifFin());

		// la nouvelle raison d'acquisition doit apparaître sur le droit d'Amandine...

		final Set<RaisonAcquisitionRF> raisonsAmandines = droitAmandine2.getRaisonsAcquisition();
		assertEquals(3, raisonsAmandines.size());

		// ... mais la date de début doit rester intouchée

		assertEquals(date(2016, 12, 19), droitAmandine2.getDateDebutMetier());
		assertEquals("Succession", droitAmandine2.getMotifDebut());


		// partage pour Amandine ----------------------------------

		final DroitProprieteRF droitAmandine3 = newDroitPP("0ad275c5-374c-4660-96d6-f3fc424538fe", "d092423b-9221-4bbf-9286-4c725d179bfa",
		                                               date(2018, 6, 24), null,
		                                               null, null,
		                                               "Partage", null, new Fraction(1, 1), INDIVIDUELLE, amandine);
		droitAmandine3.addRaisonAcquisition(newRaisonAcquisitionRF(date(2018, 8, 24), date(2013, 4, 11), "Succession", new IdentifiantAffaireRF(12, 2013, 1848, 0)));
		droitAmandine3.addRaisonAcquisition(newRaisonAcquisitionRF(date(2018, 8, 24), date(2016, 12, 19), "Succession", new IdentifiantAffaireRF(12, 2016, 7213, 0)));
		droitAmandine3.addRaisonAcquisition(newRaisonAcquisitionRF(date(2018, 8, 24), date(2017, 12, 15), "Partage", new IdentifiantAffaireRF(12, 2017, 10336, 0)));
		droitAmandine3.addRaisonAcquisition(newRaisonAcquisitionRF(date(2018, 8, 24), date(2018, 5, 31), "Partage", new IdentifiantAffaireRF(12, 2018, 2858, 0)));

		// on applique les changements
		final AffaireRF partageAmandine = new AffaireRF(date(2018, 8, 24), immeuble);

		partageAmandine.apply(new MockDroitRFDAO(),
		                      Collections.singletonList(droitAmandine3),
		                      Collections.emptyList(),
		                      Arrays.asList(droitCommunaute3, droitAlain2, droitAmandine2, droitCornelia2), listener);

		// les droits de la communauté 3 doivent être fermés

		assertEquals(date(2018, 8, 23), droitCommunaute3.getDateFin());
		assertEquals(date(2018, 8, 23), droitAlain2.getDateFin());
		assertEquals(date(2018, 8, 23), droitAmandine2.getDateFin());
		assertEquals(date(2018, 8, 23), droitCornelia2.getDateFin());

		assertEquals(date(2018, 5, 31), droitCommunaute3.getDateFinMetier());
		assertEquals(date(2018, 5, 31), droitAlain2.getDateFinMetier());
		assertEquals(date(2018, 5, 31), droitAmandine2.getDateFinMetier());
		assertEquals(date(2018, 5, 31), droitCornelia2.getDateFinMetier());

		assertEquals("Partage", droitCommunaute3.getMotifFin());
		assertEquals("Partage", droitAlain2.getMotifFin());
		assertEquals("Partage", droitAmandine2.getMotifFin());
		assertEquals("Partage", droitCornelia2.getMotifFin());

		// la date de début du nouveau droit doit être calculée

		assertEquals(date(2018, 5, 31), droitAmandine3.getDateDebutMetier());
		assertEquals("Partage", droitAmandine3.getMotifDebut());
	}

	/**
	 * [SIFISC-29326] Ce test vérifie que les dates métier sont bien recalculées sur des communautés complexes (voir descriptifs dans le test), notamment dans le cas où un membre renonce à son droit et où une nouvelle
	 * raisons d'acquisition est ajoutée sur un seul des membres restants .
	 */
	@Test
	public void testRefreshDatesMetierCommunautePlusieursPartages() {

		final PersonnePhysiqueRF alain = new PersonnePhysiqueRF();
		alain.setId(1L);
		final PersonnePhysiqueRF amandine = new PersonnePhysiqueRF();
		amandine.setId(2L);
		final PersonnePhysiqueRF cornelia = new PersonnePhysiqueRF();
		cornelia.setId(3L);
		final PersonnePhysiqueRF chloe = new PersonnePhysiqueRF();
		chloe.setId(4L);

		final CommunauteRF communaute1 = new CommunauteRF();
		communaute1.setId(10L);

		final CommunauteRF communaute2 = new CommunauteRF();
		communaute2.setId(11L);

		final CommunauteRF communaute3 = new CommunauteRF();
		communaute3.setId(12L);

		// historique des changements :
		//
		//  | date import | date métier | commentaire
		//  +-------------+-------------+----------------------------------------------------------------------------------------------------------------------------------
		//  |     -       | 07.05.1971  | Import initial, deux communautés (Chloé toute seule) et (Alain + Amandine + Cornélia + Chloé) sont créées
		//  | 07.01.2017  | 19.12.2016  | Décès de Chloé, fermeture des deux communautés existantes et ouverture d'une troisième communauté (Alain + Amandine + Cornélia)
		//  | 11.02.2018  | 15.12.2017  | Partage, Cornélia renonce à sa part au profit d'Amandine
		//  | 24.06.2018  | 31.05.2018  | Partage, Amandine devient la seule propriétaire


		// communaute 1

		final DroitProprieteRF droitCommunaute1 = newDroitComm("1f109152381059670138105fe79f3627", "1f1091523810596701381062e8971959",
		                                                       null, date(2017, 1, 6),
		                                                       date(1971, 5, 7), date(2016, 12, 19),
		                                                       "Achat", "Succession", new Fraction(1, 1), COMMUNE, communaute1);

		final DroitProprieteRF droitChloe1 = newDroitPP("1f109152381059670138105f83310b45", "1f1091523810596701381062e8971959",
		                                                null, date(2017, 1, 6),
		                                                date(1971, 5, 7), date(2016, 12, 19),
		                                                "Achat", "Succession", new Fraction(1, 1), COMMUNE, chloe, communaute1);
		droitChloe1.addRaisonAcquisition(newRaisonAcquisitionRF(null, date(1971, 5, 7), "Achat", new IdentifiantAffaireRF(12, "132394")));


		// communaute 2

		final DroitProprieteRF droitCommunaute2 = newDroitComm("8af806fa3d25b95e013e17b735d60ca2", "1f1091523810596701381062e8971959",
		                                                       null, date(2017, 1, 6),
		                                                       date(1971, 5, 7), date(2016, 12, 19),
		                                                       "Achat", "Succession", new Fraction(1, 1), COMMUNE, communaute2);

		final DroitProprieteRF droitAlain1 = newDroitPP("8af806fa3d25b95e013e17b735d70ca4", "1f1091523810596701381062e8971959",
		                                             null, date(2017, 1, 6),
		                                             date(2013, 4, 11), date(2016, 12, 19),
		                                             "Succession", "Succession", new Fraction(1, 1), COMMUNE, alain, communaute2);
		droitAlain1.addRaisonAcquisition(newRaisonAcquisitionRF(null, date(2013, 4, 11), "Succession", new IdentifiantAffaireRF(12, 2013, 1848, 0)));

		final DroitProprieteRF droitAmandine1 = newDroitPP("8af806fa3d25b95e013e17b735d70ca5", "1f1091523810596701381062e8971959",
		                                             null, date(2017, 1, 6),
		                                             date(2013, 4, 11), date(2016, 12, 19),
		                                             "Succession", "Succession", new Fraction(1, 1), COMMUNE, amandine, communaute2);
		droitAmandine1.addRaisonAcquisition(newRaisonAcquisitionRF(null, date(2013, 4, 11), "Succession", new IdentifiantAffaireRF(12, 2013, 1848, 0)));

		final DroitProprieteRF droitCornelia1 = newDroitPP("8af806fa3d25b95e013e17b735d70ca6", "1f1091523810596701381062e8971959",
		                                             null, date(2017, 1, 6),
		                                             date(2013, 4, 11), date(2016, 12, 19),
		                                             "Succession", "Succession", new Fraction(1, 1), COMMUNE, cornelia, communaute2);
		droitCornelia1.addRaisonAcquisition(newRaisonAcquisitionRF(null, date(2013, 4, 11), "Succession", new IdentifiantAffaireRF(12, 2013, 1848, 0)));

		final DroitProprieteRF droitChloe2 = newDroitPP("8af806fa3d25b95e013e17b735d70ca3", "1f1091523810596701381062e8971959",
		                                               null, date(2017, 1, 6),
		                                               date(2013, 4, 11), date(2016, 12, 19),
		                                               "Succession", "Succession", new Fraction(1, 1), COMMUNE, chloe, communaute2);
		droitChloe2.addRaisonAcquisition(newRaisonAcquisitionRF(null, date(2013, 4, 11), "Succession", new IdentifiantAffaireRF(12, 2013, 1848, 0)));

		// communaute 3

		final DroitProprieteRF droitCommunaute3 = newDroitComm("8af8064e58e34c9601596981b010113d", "8af8064e58e34c9601596981b00f113a",
		                                                       date(2017, 1, 7), date(2018, 6, 23),
		                                                       date(2016, 12, 19), date(2018, 5, 31),
		                                                       "Succession", "Partage", new Fraction(1, 1), COMMUNE, communaute3);

		final DroitProprieteRF droitAlain2 = newDroitPP("8af8064e58e34c9601596981d9121146", "8af8064e58e34c9601596981b00f113a",
		                                               date(2017, 1, 7), date(2018, 6, 23),
		                                               date(2016, 12, 19), date(2018, 5, 31),
		                                               "Succession", "Partage", new Fraction(1, 1), COMMUNE, alain, communaute3);
		droitAlain2.addRaisonAcquisition(newRaisonAcquisitionRF(date(2017, 1, 7), date(2013, 4, 11), "Succession", new IdentifiantAffaireRF(12, 2013, 1848, 0)));
		droitAlain2.addRaisonAcquisition(newRaisonAcquisitionRF(date(2017, 1, 7), date(2016, 12, 19), "Succession", new IdentifiantAffaireRF(12, 2016, 7213, 0)));

		final DroitProprieteRF droitAmandine2 = newDroitPP("8af8064e58e34c9601596981d9111144", "8af8064e58e34c9601596981b00f113a",
		                                               date(2017, 1, 7), date(2018, 6, 23),
		                                               date(2016, 12, 19), date(2018, 5, 31),
		                                               "Succession", "Partage", new Fraction(1, 1), COMMUNE, amandine, communaute3);
		droitAmandine2.addRaisonAcquisition(newRaisonAcquisitionRF(date(2017, 1, 7), date(2013, 4, 11), "Succession", new IdentifiantAffaireRF(12, 2013, 1848, 0)));
		droitAmandine2.addRaisonAcquisition(newRaisonAcquisitionRF(date(2017, 1, 7), date(2016, 12, 19), "Succession", new IdentifiantAffaireRF(12, 2016, 7213, 0)));
		droitAmandine2.addRaisonAcquisition(newRaisonAcquisitionRF(date(2018, 2, 11), date(2017, 12, 15), "Partage", new IdentifiantAffaireRF(12, 2017, 10336, 0)));


		final DroitProprieteRF droitCornelia2 = newDroitPP("8af8064e58e34c9601596981d9121148", "8af8064e58e34c9601596981b00f113a",
		                                               date(2017, 1, 7), date(2018, 2, 10),
		                                               date(2016, 12, 19), date(2017, 12, 15),
		                                               "Succession", "Partage", new Fraction(1, 1), COMMUNE, cornelia, communaute3);
		droitCornelia2.addRaisonAcquisition(newRaisonAcquisitionRF(date(2017, 1, 7), date(2013, 4, 11), "Succession", new IdentifiantAffaireRF(12, 2013, 1848, 0)));
		droitCornelia2.addRaisonAcquisition(newRaisonAcquisitionRF(date(2017, 1, 7), date(2016, 12, 19), "Succession", new IdentifiantAffaireRF(12, 2016, 7213, 0)));

		// droit individuel d'Amandine

		final DroitProprieteRF droitAmandine3 = newDroitPP("0ad275c5-374c-4660-96d6-f3fc424538fe", "d092423b-9221-4bbf-9286-4c725d179bfa",
		                                               date(2018, 6, 24), null,
		                                               date(2018, 5, 31), null,
		                                               "Partage", null, new Fraction(1, 1), INDIVIDUELLE, amandine);
		droitAmandine3.addRaisonAcquisition(newRaisonAcquisitionRF(date(2018, 8, 24), date(2013, 4, 11), "Succession", new IdentifiantAffaireRF(12, 2013, 1848, 0)));
		droitAmandine3.addRaisonAcquisition(newRaisonAcquisitionRF(date(2018, 8, 24), date(2016, 12, 19), "Succession", new IdentifiantAffaireRF(12, 2016, 7213, 0)));
		droitAmandine3.addRaisonAcquisition(newRaisonAcquisitionRF(date(2018, 8, 24), date(2017, 12, 15), "Partage", new IdentifiantAffaireRF(12, 2017, 10336, 0)));
		droitAmandine3.addRaisonAcquisition(newRaisonAcquisitionRF(date(2018, 8, 24), date(2018, 5, 31), "Partage", new IdentifiantAffaireRF(12, 2018, 2858, 0)));

		final ImmeubleRF immeuble = new BienFondsRF();
		immeuble.addDroitPropriete(droitCommunaute1);
		immeuble.addDroitPropriete(droitChloe1);
		immeuble.addDroitPropriete(droitCommunaute2);
		immeuble.addDroitPropriete(droitAlain1);
		immeuble.addDroitPropriete(droitAmandine1);
		immeuble.addDroitPropriete(droitCornelia1);
		immeuble.addDroitPropriete(droitChloe2);
		immeuble.addDroitPropriete(droitCommunaute3);
		immeuble.addDroitPropriete(droitAlain2);
		immeuble.addDroitPropriete(droitAmandine2);
		immeuble.addDroitPropriete(droitCornelia2);
		immeuble.addDroitPropriete(droitAmandine3);

		// on rafraîchit les dates métier pour l'import initial
		final Listener listener = new Listener();
		final AffaireRF importInitial = new AffaireRF(null, immeuble);
		importInitial.refreshDatesMetier(listener);

		// tout était déjà à jour
		assertEquals(0, listener.getCreated().size());
		assertEquals(0, listener.getDebutUpdates().size());
		assertEquals(0, listener.getFinUpdates().size());
		assertEquals(0, listener.getClosed().size());

		// on rafraîchit les dates métier pour la succession
		final AffaireRF succession = new AffaireRF(date(2017, 1, 7), immeuble);
		succession.refreshDatesMetier(listener);

		// tout était déjà à jour
		assertEquals(0, listener.getCreated().size());
		assertEquals(0, listener.getDebutUpdates().size());
		assertEquals(0, listener.getFinUpdates().size());
		assertEquals(0, listener.getClosed().size());

		// on rafraîchit les dates métier pour le premier partage
		final AffaireRF premierPartage = new AffaireRF(date(2018, 2, 11), immeuble);
		premierPartage.refreshDatesMetier(listener);

		// tout était déjà à jour
		assertEquals(0, listener.getCreated().size());
		assertEquals(0, listener.getDebutUpdates().size());
		assertEquals(0, listener.getFinUpdates().size());
		assertEquals(0, listener.getClosed().size());

		// on rafraîchit les dates métier pour le second partage
		final AffaireRF secondPartage = new AffaireRF(date(2018, 8, 24), immeuble);
		secondPartage.refreshDatesMetier(listener);

		// tout était déjà à jour
		assertEquals(0, listener.getCreated().size());
		assertEquals(0, listener.getDebutUpdates().size());
		assertEquals(0, listener.getFinUpdates().size());
		assertEquals(0, listener.getClosed().size());

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

	private static void assertFraction(int numerateur, int denominateur, Fraction part) {
		assertNotNull(part);
		assertEquals(numerateur, part.getNumerateur());
		assertEquals(denominateur, part.getDenominateur());
	}

	private static DroitProprietePersonnePhysiqueRF newDroitPP(String masterIdRF, String versionIdRF, RegDate dateDebut, RegDate dateFin, RegDate dateDebutMetier, RegDate dateFinMetier, String motifDebut, String motifFin,
	                                                           Fraction part, GenrePropriete regime, PersonnePhysiqueRF ayantDroit) {
		final DroitProprietePersonnePhysiqueRF droit = new DroitProprietePersonnePhysiqueRF();
		droit.setMasterIdRF(masterIdRF);
		droit.setVersionIdRF(versionIdRF);
		droit.setDateDebut(dateDebut);
		droit.setDateFin(dateFin);
		droit.setDateDebutMetier(dateDebutMetier);
		droit.setDateFinMetier(dateFinMetier);
		droit.setMotifDebut(motifDebut);
		droit.setMotifFin(motifFin);
		droit.setPart(part);
		droit.setRegime(regime);
		droit.setAyantDroit(ayantDroit);
		return droit;
	}

	private DroitProprieteRF newDroitPP(String masterIdRF, String versionIdRF, RegDate dateDebut, RegDate dateFin, RegDate dateDebutMetier, RegDate dateFinMetier, String motifDebut, String motifFin,
	                                    Fraction part, GenrePropriete regime, PersonnePhysiqueRF ayantDroit, CommunauteRF communaute) {
		final DroitProprietePersonnePhysiqueRF droit = newDroitPP(masterIdRF, versionIdRF, dateDebut, dateFin, dateDebutMetier, dateFinMetier, motifDebut, motifFin, part, regime, ayantDroit);
		droit.setCommunaute(communaute);
		return droit;
	}

	private static DroitProprieteCommunauteRF newDroitComm(String masterIdRF, String versionIdRF, RegDate dateDebut, RegDate dateFin, RegDate dateDebutMetier, RegDate dateFinMetier, String motifDebut, String motifFin,
	                                                       Fraction part, GenrePropriete regime, CommunauteRF ayantDroit) {
		final DroitProprieteCommunauteRF droit = new DroitProprieteCommunauteRF();
		droit.setMasterIdRF(masterIdRF);
		droit.setVersionIdRF(versionIdRF);
		droit.setDateDebut(dateDebut);
		droit.setDateFin(dateFin);
		droit.setDateDebutMetier(dateDebutMetier);
		droit.setDateFinMetier(dateFinMetier);
		droit.setMotifDebut(motifDebut);
		droit.setMotifFin(motifFin);
		droit.setPart(part);
		droit.setRegime(regime);
		droit.setAyantDroit(ayantDroit);
		return droit;
	}
}