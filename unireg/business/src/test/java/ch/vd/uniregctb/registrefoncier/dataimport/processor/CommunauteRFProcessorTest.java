package ch.vd.uniregctb.registrefoncier.dataimport.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.evenement.fiscal.MockEvenementFiscalService;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRFMembreInfo;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.ModeleCommunauteRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.PrincipalCommunauteRF;
import ch.vd.uniregctb.registrefoncier.RapprochementRF;
import ch.vd.uniregctb.registrefoncier.RegroupementCommunauteRF;
import ch.vd.uniregctb.registrefoncier.TiersRF;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;

import static ch.vd.uniregctb.common.WithoutSpringTest.assertEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class CommunauteRFProcessorTest {

	private CommunauteRFProcessor processor;

	private Map<Set<Long>, ModeleCommunauteRF> modeles = new HashMap<>();
	private List<Pair<RegDate, CommunauteRF>> evenementsModificationPrincipalCommunaute;

	@Before
	public void setUp() throws Exception {
		evenementsModificationPrincipalCommunaute = new ArrayList<>();
		final EvenementFiscalService evenementFiscalService = new MockEvenementFiscalService() {
			@Override
			public void publierModificationPrincipalCommunaute(RegDate dateDebut, CommunauteRF communaute) {
				evenementsModificationPrincipalCommunaute.add(new Pair<>(dateDebut, communaute));
			}
		};
		processor = new CommunauteRFProcessor(this::getModeleCommunauteRF, this::getPrincipalCommunauteId, evenementFiscalService);
	}

	/**
	 * Vérifie qu'aucun regroupement n'est créé si la communauté ne possède pas de membre.
	 */
	@Test
	public void testProcessCommunauteSansMembreNiRegroupement() {

		// une communauté sans membre, ni regroupement
		CommunauteRF communaute = new CommunauteRF();
		communaute.setRegroupements(Collections.emptySet());
		communaute.setMembres(Collections.emptySet());

		processor.process(communaute);

		// il ne devrait toujours pas y avoir de regroupement
		assertEmpty(communaute.getRegroupements());

		// aucun événement ne devrait être envoyé car le principal ne change pas
		assertEmpty(evenementsModificationPrincipalCommunaute);
	}

	/**
	 * Vérifie que les éventuels regroupements sont annulés si la communauté ne possède pas de membre.
	 */
	@Test
	public void testProcessCommunauteSansMembreAvecRegroupement() {

		final PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
		pp1.setId(1L);
		pp1.setPrenom("Ronald");
		pp1.setNom("Lasalt");

		final PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
		pp2.setId(2L);
		pp2.setPrenom("Georgette");
		pp2.setNom("Lasalt");

		final ModeleCommunauteRF modele = getModeleCommunauteRF(new HashSet<>(Arrays.asList(pp1, pp2)));

		// une communauté sans membre, mais avec un regroupement
		final RegroupementCommunauteRF regroupement = new RegroupementCommunauteRF();
		regroupement.setDateDebut(RegDate.get(2000,1,1));
		regroupement.setDateFin(null);
		regroupement.setModele(modele);

		CommunauteRF communaute = new CommunauteRF();
		communaute.addRegroupement(regroupement);
		communaute.setMembres(Collections.emptySet());

		processor.process(communaute);

		// le regroupement devrait être annulé
		final Set<RegroupementCommunauteRF> regroupements = communaute.getRegroupements();
		assertEquals(1, regroupements.size());
		assertTrue(regroupements.iterator().next().isAnnule());

		// aucun événement ne devrait être envoyé car le principal ne change pas
		assertEmpty(evenementsModificationPrincipalCommunaute);
	}

	/**
	 * Vérifie qu'un regroupement est créé si la communauté possède des membres.
	 */
	@Test
	public void testProcessCommunauteAvecMembresSansRegroupement() {

		final PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
		pp1.setId(1L);
		pp1.setPrenom("Ronald");
		pp1.setNom("Lasalt");

		final PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
		pp2.setId(2L);
		pp2.setPrenom("Georgette");
		pp2.setNom("Lasalt");

		final DroitProprietePersonnePhysiqueRF droit1 = new DroitProprietePersonnePhysiqueRF();
		droit1.setId(11L);
		droit1.setDateDebutMetier(RegDate.get(2000,1,1));
		droit1.setDateFinMetier(null);
		droit1.setAyantDroit(pp1);

		final DroitProprietePersonnePhysiqueRF droit2 = new DroitProprietePersonnePhysiqueRF();
		droit2.setId(12L);
		droit2.setDateDebutMetier(RegDate.get(2000,1,1));
		droit2.setDateFinMetier(null);
		droit2.setAyantDroit(pp2);

		// une communauté avec deux membres, mais sans regroupement
		CommunauteRF communaute = new CommunauteRF();
		communaute.setRegroupements(new HashSet<>());
		communaute.addMembre(droit1);
		communaute.addMembre(droit2);

		processor.process(communaute);

		// un regroupement devrait être créé
		final Set<RegroupementCommunauteRF> regroupements = communaute.getRegroupements();
		assertEquals(1, regroupements.size());
		final RegroupementCommunauteRF regroupement0 = regroupements.iterator().next();
		assertFalse(regroupement0.isAnnule());
		assertRegroupement(RegDate.get(2000, 1, 1), null, communaute, regroupement0, pp1, pp2);

		// aucun événement ne devrait être envoyé car le principal ne change pas
		assertEmpty(evenementsModificationPrincipalCommunaute);
	}

	/**
	 * Vérifie que deux regroupements sont créés dans le cas où un des membres d'une communauté renonce à sa part.
	 */
	@Test
	public void testProcessCommunauteAvecUnMembreQuiRenonceASaPart() {

		final RegDate dateSuccession = RegDate.get(2000, 1, 1);
		final RegDate dateRenoncement = RegDate.get(2000, 7, 12);

		final PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
		pp1.setId(1L);
		pp1.setPrenom("Ronald");
		pp1.setNom("Lasalt");

		final PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
		pp2.setId(2L);
		pp2.setPrenom("Georgette");
		pp2.setNom("Lasalt");

		final PersonnePhysiqueRF pp3 = new PersonnePhysiqueRF();
		pp3.setId(3L);
		pp3.setPrenom("Jean-Rodolphe");
		pp3.setNom("Zwarisk");

		final DroitProprietePersonnePhysiqueRF droit1 = new DroitProprietePersonnePhysiqueRF();
		droit1.setId(11L);
		droit1.setDateDebutMetier(dateSuccession);
		droit1.setDateFinMetier(null);
		droit1.setAyantDroit(pp1);

		final DroitProprietePersonnePhysiqueRF droit2 = new DroitProprietePersonnePhysiqueRF();
		droit2.setId(12L);
		droit2.setDateDebutMetier(dateSuccession);
		droit2.setDateFinMetier(dateRenoncement);   // <--- Georgette renonce à sa part
		droit2.setAyantDroit(pp2);

		final DroitProprietePersonnePhysiqueRF droit3 = new DroitProprietePersonnePhysiqueRF();
		droit3.setId(13L);
		droit3.setDateDebutMetier(dateSuccession);
		droit3.setDateFinMetier(null);
		droit3.setAyantDroit(pp3);

		// une communauté avec trois membres
		CommunauteRF communaute = new CommunauteRF();
		communaute.setRegroupements(new HashSet<>());
		communaute.addMembre(droit1);
		communaute.addMembre(droit2);
		communaute.addMembre(droit3);

		droit1.setCommunaute(communaute);
		droit2.setCommunaute(communaute);
		droit3.setCommunaute(communaute);

		processor.process(communaute);

		// deux regroupements devraient être créés :
		//  1. un premier regroupement pendant la période à trois membres (entre la succession et le renoncement)
		//  2. un second regroupement pendant la période à deux membres (depuis le renoncement)
		final List<RegroupementCommunauteRF> regroupements = new ArrayList<>(communaute.getRegroupements());
		regroupements.sort(new DateRangeComparator<>());
		assertEquals(2, regroupements.size());

		final RegroupementCommunauteRF regroupement0 = regroupements.get(0);
		assertFalse(regroupement0.isAnnule());
		assertRegroupement(dateSuccession, dateRenoncement, communaute, regroupement0, pp1, pp2, pp3);

		final RegroupementCommunauteRF regroupement1 = regroupements.get(1);
		assertFalse(regroupement1.isAnnule());
		assertRegroupement(dateRenoncement.getOneDayAfter(), null, communaute, regroupement1, pp1, pp3);

		// aucun événement ne devrait être envoyé car le principal ne change pas
		assertEmpty(evenementsModificationPrincipalCommunaute);
	}

	/**
	 * [SIFISC-28009] Vérifie que les regroupements sont bien créés dans le cas où un nouveau membre s'ajoute à une.
	 */
	@Test
	public void testProcessCommunauteAvecNouveauMembreQuiSAjoute() {

		final RegDate dateSuccession = RegDate.get(2000, 1, 1);
		final RegDate dateAdmission = RegDate.get(2000, 7, 12);

		final PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
		pp1.setId(1L);
		pp1.setPrenom("Ronald");
		pp1.setNom("Lasalt");

		final PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
		pp2.setId(2L);
		pp2.setPrenom("Georgette");
		pp2.setNom("Lasalt");

		final PersonnePhysiqueRF pp3 = new PersonnePhysiqueRF();
		pp3.setId(3L);
		pp3.setPrenom("Jean-Rodolphe");
		pp3.setNom("Zwarisk");

		final DroitProprietePersonnePhysiqueRF droit1 = new DroitProprietePersonnePhysiqueRF();
		droit1.setId(11L);
		droit1.setDateDebutMetier(dateSuccession);
		droit1.setDateFinMetier(null);
		droit1.setAyantDroit(pp1);

		final DroitProprietePersonnePhysiqueRF droit2 = new DroitProprietePersonnePhysiqueRF();
		droit2.setId(12L);
		droit2.setDateDebutMetier(dateSuccession);
		droit2.setDateFinMetier(null);
		droit2.setAyantDroit(pp2);

		final DroitProprietePersonnePhysiqueRF droit3 = new DroitProprietePersonnePhysiqueRF();
		droit3.setId(13L);
		droit3.setDateDebutMetier(dateAdmission);   // <--- arrivée du nouveau membre
		droit3.setDateFinMetier(null);
		droit3.setAyantDroit(pp3);

		// une communauté avec trois membres
		CommunauteRF communaute = new CommunauteRF();
		communaute.setRegroupements(new HashSet<>());
		communaute.addMembre(droit1);
		communaute.addMembre(droit2);
		communaute.addMembre(droit3);

		droit1.setCommunaute(communaute);
		droit2.setCommunaute(communaute);
		droit3.setCommunaute(communaute);

		processor.process(communaute);

		// deux regroupements devraient être créés :
		//  1. un premier regroupement pendant la période à deux membres (entre la succession et l'admission)
		//  2. un second regroupement pendant la période à trois membres (depuis l'admission)
		final List<RegroupementCommunauteRF> regroupements = new ArrayList<>(communaute.getRegroupements());
		regroupements.sort(new DateRangeComparator<>());
		assertEquals(2, regroupements.size());

		final RegroupementCommunauteRF regroupement0 = regroupements.get(0);
		assertFalse(regroupement0.isAnnule());
		assertRegroupement(dateSuccession, dateAdmission.getOneDayBefore(), communaute, regroupement0, pp1, pp2);

		final RegroupementCommunauteRF regroupement1 = regroupements.get(1);
		assertFalse(regroupement1.isAnnule());
		assertRegroupement(dateAdmission, null, communaute, regroupement1, pp1, pp2, pp3);

		// aucun événement ne devrait être envoyé car le principal ne change pas
		assertEmpty(evenementsModificationPrincipalCommunaute);
	}

	/**
	 * Vérifie que les regroupements sont bien créés dans le cas suivant (communauté n°189976085) :
	 * <pre>
	 *     Droit 1  : 19990712 |---------------------------------------------------
	 *     Droit 2  : 19990712 |---------------------------------------------------
	 *     Droit 3  : 19990712 |-----------------| 20081219
	 *     Droit 4  :                   20081219 |---------------------------------
	 *     Droit 5  :                                        20170627 |------------
	 * </pre>
	 */
	@Test
	public void testProcessCommunauteAvecMembresRemplacementPlusAdditionTardive() {

		final PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
		pp1.setId(1L);
		pp1.setPrenom("Ronald");
		pp1.setNom("Lasalt");

		final PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
		pp2.setId(2L);
		pp2.setPrenom("Georgette");
		pp2.setNom("Lasalt");

		final PersonnePhysiqueRF pp3 = new PersonnePhysiqueRF();
		pp3.setId(3L);
		pp3.setPrenom("Lucette");
		pp3.setNom("Lasalt");

		final PersonnePhysiqueRF pp4 = new PersonnePhysiqueRF();
		pp4.setId(4L);
		pp4.setPrenom("Elodie");
		pp4.setNom("Lasalt");

		final PersonnePhysiqueRF pp5 = new PersonnePhysiqueRF();
		pp5.setId(5L);
		pp5.setPrenom("Edouard");
		pp5.setNom("Lasalt");

		final DroitProprietePersonnePhysiqueRF droit1 = new DroitProprietePersonnePhysiqueRF();
		droit1.setId(11L);
		droit1.setDateDebutMetier(RegDate.get(1999, 7, 12));
		droit1.setDateFinMetier(null);
		droit1.setAyantDroit(pp1);

		final DroitProprietePersonnePhysiqueRF droit2 = new DroitProprietePersonnePhysiqueRF();
		droit2.setId(12L);
		droit2.setDateDebutMetier(RegDate.get(1999, 7, 12));
		droit2.setDateFinMetier(null);
		droit2.setAyantDroit(pp2);

		final DroitProprietePersonnePhysiqueRF droit3 = new DroitProprietePersonnePhysiqueRF();
		droit3.setId(13L);
		droit3.setDateDebutMetier(RegDate.get(1999, 7, 12));
		droit3.setDateFinMetier(RegDate.get(2008, 12, 19));
		droit3.setAyantDroit(pp3);

		final DroitProprietePersonnePhysiqueRF droit4 = new DroitProprietePersonnePhysiqueRF();
		droit4.setId(14L);
		droit4.setDateDebutMetier(RegDate.get(2008, 12, 19));
		droit4.setDateFinMetier(null);
		droit4.setAyantDroit(pp4);

		final DroitProprietePersonnePhysiqueRF droit5 = new DroitProprietePersonnePhysiqueRF();
		droit5.setId(15L);
		droit5.setDateDebutMetier(RegDate.get(2017, 6, 27));
		droit5.setDateFinMetier(null);
		droit5.setAyantDroit(pp5);

		// une communauté avec deux membres, mais sans regroupement
		CommunauteRF communaute = new CommunauteRF();
		communaute.setRegroupements(new HashSet<>());
		communaute.addMembre(droit1);
		communaute.addMembre(droit2);
		communaute.addMembre(droit3);
		communaute.addMembre(droit4);
		communaute.addMembre(droit5);

		processor.process(communaute);

		// les regroupememts suivants devraient être créés :
		//
		// Regroupement 1  : 19990712 |-----------------| 20081218
		// Regroupement 2  :                   20081219 |-| 20081219
		// Regroupement 3  :                     20081220 |-----------------| 20170626
		// Regroupement 4  :                                       20170627 |-----------
		//
		final List<RegroupementCommunauteRF> regroupements = new ArrayList<>(communaute.getRegroupements());
		regroupements.sort(new DateRangeComparator<>());
		assertEquals(4, regroupements.size());
		assertRegroupement(RegDate.get(1999, 7, 12), RegDate.get(2008, 12, 18), communaute, regroupements.get(0), pp1, pp2, pp3);
		assertRegroupement(RegDate.get(2008, 12, 19), RegDate.get(2008, 12, 19), communaute, regroupements.get(1), pp1, pp2, pp3, pp4);
		assertRegroupement(RegDate.get(2008, 12, 20), RegDate.get(2017, 6, 26), communaute, regroupements.get(2), pp1, pp2, pp4);
		assertRegroupement(RegDate.get(2017, 6, 27), null, communaute, regroupements.get(3), pp1, pp2, pp4, pp5);

		// aucun événement ne devrait être envoyé car le principal ne change pas
		assertEmpty(evenementsModificationPrincipalCommunaute);
	}

	/**
	 * Vérifie que rien n'est changé si la communauté possède des membres et le regroupement correspondant.
	 */
	@Test
	public void testProcessCommunauteAvecMembresEtRegroupementCorrespondant() {

		final PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
		pp1.setId(1L);
		pp1.setPrenom("Ronald");
		pp1.setNom("Lasalt");

		final PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
		pp2.setId(2L);
		pp2.setPrenom("Georgette");
		pp2.setNom("Lasalt");

		final DroitProprietePersonnePhysiqueRF droit1 = new DroitProprietePersonnePhysiqueRF();
		droit1.setId(11L);
		droit1.setDateDebutMetier(RegDate.get(2000,1,1));
		droit1.setDateFinMetier(null);
		droit1.setAyantDroit(pp1);

		final DroitProprietePersonnePhysiqueRF droit2 = new DroitProprietePersonnePhysiqueRF();
		droit2.setId(12L);
		droit2.setDateDebutMetier(RegDate.get(2000,1,1));
		droit2.setDateFinMetier(null);
		droit2.setAyantDroit(pp2);

		final ModeleCommunauteRF modele = getModeleCommunauteRF(new HashSet<>(Arrays.asList(pp1, pp2)));

		// une communauté avec deux membres et le regroupement qui correspond
		final RegroupementCommunauteRF regroupement = new RegroupementCommunauteRF();
		regroupement.setDateDebut(RegDate.get(2000,1,1));
		regroupement.setDateFin(null);
		regroupement.setModele(modele);

		CommunauteRF communaute = new CommunauteRF();
		communaute.addRegroupement(regroupement);
		communaute.addMembre(droit1);
		communaute.addMembre(droit2);

		processor.process(communaute);

		// le regroupement devrait être inchangé
		final Set<RegroupementCommunauteRF> regroupements = communaute.getRegroupements();
		assertEquals(1, regroupements.size());
		final RegroupementCommunauteRF regroupement0 = regroupements.iterator().next();
		assertFalse(regroupement0.isAnnule());
		assertRegroupement(RegDate.get(2000, 1, 1), null, communaute, regroupement0, pp1, pp2);

		// aucun événement ne devrait être envoyé car le principal ne change pas
		assertEmpty(evenementsModificationPrincipalCommunaute);
	}

	/**
	 * Vérifie que le regroupement existant est annulé et qu'un regroupement corrigé et créé si la communauté possède des membres
	 * et un regroupement mais qu'il ne correspond pas.
	 */
	@Test
	public void testProcessCommunauteAvecMembresEtRegroupementDifferent() {

		final PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
		pp1.setId(1L);
		pp1.setPrenom("Ronald");
		pp1.setNom("Lasalt");

		final PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
		pp2.setId(2L);
		pp2.setPrenom("Georgette");
		pp2.setNom("Lasalt");

		final PersonnePhysiqueRF pp3 = new PersonnePhysiqueRF();
		pp3.setId(3L);
		pp3.setPrenom("Jean-Rodolphe");
		pp3.setNom("Zwarisk");

		final DroitProprietePersonnePhysiqueRF droit1 = new DroitProprietePersonnePhysiqueRF();
		droit1.setId(11L);
		droit1.setDateDebutMetier(RegDate.get(2000,1,1));
		droit1.setDateFinMetier(null);
		droit1.setAyantDroit(pp1);

		final DroitProprietePersonnePhysiqueRF droit2 = new DroitProprietePersonnePhysiqueRF();
		droit2.setId(12L);
		droit2.setDateDebutMetier(RegDate.get(2000,1,1));
		droit2.setDateFinMetier(null);
		droit2.setAyantDroit(pp2);

		final ModeleCommunauteRF modele = getModeleCommunauteRF(new HashSet<>(Arrays.asList(pp1, pp3)));

		// une communauté avec deux membres et un regroupement qui ne correspond pas (pp1 + pp3 à la place de pp1 + pp2)
		final RegroupementCommunauteRF regroupement = new RegroupementCommunauteRF();
		regroupement.setId(100L);
		regroupement.setDateDebut(RegDate.get(2000,1,1));
		regroupement.setDateFin(null);
		regroupement.setModele(modele);

		CommunauteRF communaute = new CommunauteRF();
		communaute.addRegroupement(regroupement);
		communaute.addMembre(droit1);
		communaute.addMembre(droit2);

		processor.process(communaute);

		// le regroupement existant devrait être annulé et un nouveau regroupement devrait être créé
		final List<RegroupementCommunauteRF> regroupements = communaute.getRegroupements().stream()
				.sorted(Comparator.comparing(r -> r.getModele().getId()))
				.collect(Collectors.toList());
		assertEquals(2, regroupements.size());
		assertTrue(regroupements.get(0).isAnnule());
		assertRegroupement(RegDate.get(2000, 1, 1), null, communaute, regroupements.get(0), pp1, pp3);
		assertFalse(regroupements.get(1).isAnnule());
		assertRegroupement(RegDate.get(2000, 1, 1), null, communaute, regroupements.get(1), pp1, pp2);

		// aucun événement ne devrait être envoyé car le principal ne change pas
		assertEmpty(evenementsModificationPrincipalCommunaute);
	}

	/**
	 * Vérifie qu'un événement de changement de communauté est bien envoyé dans le cas où le principal change.
	 * Cas métier : renoncement du principal sélectionné à ses droits dans la communauté -> création d'un nouveau modèle de communauté et sélection d'un nouveau principal
	 */
	@Test
	public void testProcessCommunauteAvecChangementPrincipal() {

		final RegDate dateSuccession = RegDate.get(2000, 1, 1);
		final RegDate dateRenoncement = RegDate.get(2000, 7, 12);

		final PersonnePhysique ctb1 = new PersonnePhysique();
		ctb1.setNumero(111L);

		final PersonnePhysique ctb2 = new PersonnePhysique();
		ctb2.setNumero(222L);

		final PersonnePhysique ctb3 = new PersonnePhysique();
		ctb3.setNumero(333L);

		final RapprochementRF rp1 = new RapprochementRF();
		rp1.setContribuable(ctb1);

		final PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
		pp1.setId(1L);
		pp1.setPrenom("Ronald");
		pp1.setNom("Lasalt");
		pp1.addRapprochementRF(rp1);

		final RapprochementRF rp2 = new RapprochementRF();
		rp2.setContribuable(ctb2);

		final PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
		pp2.setId(2L);
		pp2.setPrenom("Georgette");
		pp2.setNom("Lasalt");
		pp2.addRapprochementRF(rp2);

		final RapprochementRF rp3 = new RapprochementRF();
		rp3.setContribuable(ctb3);

		final PersonnePhysiqueRF pp3 = new PersonnePhysiqueRF();
		pp3.setId(3L);
		pp3.setPrenom("Jean-Rodolphe");
		pp3.setNom("Zwarisk");
		pp3.addRapprochementRF(rp3);

		final DroitProprietePersonnePhysiqueRF droit1 = new DroitProprietePersonnePhysiqueRF();
		droit1.setId(11L);
		droit1.setDateDebutMetier(dateSuccession);
		droit1.setDateFinMetier(null);
		droit1.setAyantDroit(pp1);

		final DroitProprietePersonnePhysiqueRF droit2 = new DroitProprietePersonnePhysiqueRF();
		droit2.setId(12L);
		droit2.setDateDebutMetier(dateSuccession);
		droit2.setDateFinMetier(dateRenoncement);   // <--- Georgette renonce à sa part
		droit2.setAyantDroit(pp2);

		final DroitProprietePersonnePhysiqueRF droit3 = new DroitProprietePersonnePhysiqueRF();
		droit3.setId(13L);
		droit3.setDateDebutMetier(dateSuccession);
		droit3.setDateFinMetier(null);
		droit3.setAyantDroit(pp3);

		// un modèle de communauté avec Georgette comme principal sélectionné
		final ModeleCommunauteRF modele = getModeleCommunauteRF(new HashSet<>(Arrays.asList(pp1, pp2, pp3)));
		final PrincipalCommunauteRF principal = new PrincipalCommunauteRF();
		principal.setPrincipal(pp2);
		modele.addPrincipal(principal);

		final RegroupementCommunauteRF regroupement = new RegroupementCommunauteRF();
		regroupement.setId(100L);
		regroupement.setDateDebut(dateSuccession);
		regroupement.setDateFin(null);
		regroupement.setModele(modele);

		// une communauté avec trois membres
		CommunauteRF communaute = new CommunauteRF();
		communaute.addRegroupement(regroupement);
		communaute.addMembre(droit1);
		communaute.addMembre(droit2);
		communaute.addMembre(droit3);

		droit1.setCommunaute(communaute);
		droit2.setCommunaute(communaute);
		droit3.setCommunaute(communaute);

		processor.process(communaute);

		// un nouveau regroupement devrait être créé pour la période à deux membres (depuis le renoncement)
		final List<RegroupementCommunauteRF> regroupements = new ArrayList<>(communaute.getRegroupements());
		regroupements.sort(new DateRangeComparator<>());
		assertEquals(2, regroupements.size());

		final RegroupementCommunauteRF regroupement0 = regroupements.get(0);
		assertFalse(regroupement0.isAnnule());
		assertRegroupement(dateSuccession, dateRenoncement, communaute, regroupement0, pp1, pp2, pp3);

		final RegroupementCommunauteRF regroupement1 = regroupements.get(1);
		assertFalse(regroupement1.isAnnule());
		assertRegroupement(dateRenoncement.getOneDayAfter(), null, communaute, regroupement1, pp1, pp3);

		// un événement de modification de principal devrait être envoyé
		assertEquals(1, evenementsModificationPrincipalCommunaute.size());
		final Pair<RegDate, CommunauteRF> event0 = evenementsModificationPrincipalCommunaute.get(0);
		assertEquals(null, event0.getFirst());
		assertEquals(communaute, event0.getSecond());
	}

	private static void assertRegroupement(RegDate dateDebut, RegDate dateFin, CommunauteRF communaute, RegroupementCommunauteRF regroupement, AyantDroitRF... ayantDroits) {
		assertEquals(dateDebut, regroupement.getDateDebut());
		assertEquals(dateFin, regroupement.getDateFin());
		assertSame(communaute, regroupement.getCommunaute());

		final List<AyantDroitRF> membres0 = regroupement.getModele().getMembres().stream()
				.sorted(Comparator.comparing(AyantDroitRF::getId))
				.collect(Collectors.toList());
		assertEquals(ayantDroits.length, membres0.size());

		for (int i = 0; i < ayantDroits.length; i++) {
			final AyantDroitRF a = ayantDroits[i];
			final AyantDroitRF m = membres0.get(i);
			assertSame(a, m);
		}
	}

	private ModeleCommunauteRF getModeleCommunauteRF(Set<? extends AyantDroitRF> membres) {
		final Set<Long> ids = membres.stream()
				.map(AyantDroitRF::getId)
				.collect(Collectors.toSet());

		return modeles.computeIfAbsent(ids, key -> {
			final ModeleCommunauteRF modele = new ModeleCommunauteRF();
			modele.setId(1000L + modeles.size());
			//noinspection unchecked
			modele.setMembres((Set<AyantDroitRF>) membres);
			modele.setMembresHashCode(ModeleCommunauteRF.hashCode(membres));
			return modele;
		});
	}

	private Long getPrincipalCommunauteId(CommunauteRF communaute) {

		final CommunauteRFMembreInfo info = communaute.buildMembreInfoNonTries();
		final Long principalCtbId = Optional.ofNullable(communaute.getPrincipalCommunauteDesigne())
				.filter(TiersRF.class::isInstance)
				.map(TiersRF.class::cast)
				.map(TiersRF::getCtbRapproche)
				.map(Tiers::getId)
				.orElse(null);

		// on trie par ordre croissant des numéros de CTB, à l'exception du l'id du principal qui est toujours en premier
		info.sortMembers((o1, o2) -> {
			if (Objects.equals(o1, o2)) {
				return 0;
			}
			if (Objects.equals(o1, principalCtbId)) {
				return -1;
			}
			else if (Objects.equals(o2, principalCtbId)) {
				return 1;
			}
			return o1.compareTo(o2);
		});

		// on retourne l'id du principal
		final List<Long> ctbIds = info.getCtbIds();
		return ctbIds.isEmpty() ? null : ctbIds.get(0);
	}

}