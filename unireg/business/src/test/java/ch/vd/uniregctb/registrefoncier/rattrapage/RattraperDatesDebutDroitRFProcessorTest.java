package ch.vd.uniregctb.registrefoncier.rattrapage;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.registrefoncier.BienFondsRF;
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.RaisonAcquisitionRF;
import ch.vd.uniregctb.registrefoncier.RegistreFoncierService;
import ch.vd.uniregctb.registrefoncier.SituationRF;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.CommuneRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.DroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.rf.GenrePropriete;

import static ch.vd.uniregctb.registrefoncier.rattrapage.RattraperDatesDebutDroitRFProcessorResults.Processed;
import static ch.vd.uniregctb.registrefoncier.rattrapage.RattraperDatesDebutDroitRFProcessorResults.Untouched;
import static ch.vd.uniregctb.registrefoncier.rattrapage.RattraperDatesDebutDroitRFProcessorResults.Updated;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class RattraperDatesDebutDroitRFProcessorTest extends BusinessTest {

	private CommuneRFDAO communeRFDAO;
	private ImmeubleRFDAO immeubleRFDAO;
	private AyantDroitRFDAO ayantDroitRFDAO;
	private DroitRFDAO droitRFDAO;
	private RattraperDatesDebutDroitRFProcessor processor;
	private RegistreFoncierService registreFoncierService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		communeRFDAO = getBean(CommuneRFDAO.class, "communeRFDAO");
		immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		ayantDroitRFDAO = getBean(AyantDroitRFDAO.class, "ayantDroitRFDAO");
		droitRFDAO = getBean(DroitRFDAO.class, "droitRFDAO");
		processor = getBean(RattraperDatesDebutDroitRFProcessor.class, "rattraperDatesDebutDroitRFProcessor");
		registreFoncierService = getBean(RegistreFoncierService.class, "serviceRF");
	}

	/**
	 * Ce test s'assure que le processeur ne modifie pas des dates qui sont déjà bien calculées.
	 */
	@Test
	public void testProcessDatesDejaBienCalculees() throws Exception {

		class Ids {
			Long immeuble;
			Long droit0;
			Long droit1;
			Long droit2;
		}
		final Ids ids = new Ids();

		final RegDate dateSuccession = RegDate.get(2000, 1, 1);
		final RegDate datePartage = RegDate.get(2005, 7, 1);

		// un immeuble :
		// - succession -> deux propriétaires à 1/2
		// - partage -> un seul propriétaire à 1/1 (droit qui évolue)
		doInNewTransaction(status -> {
			PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
			pp1.setIdRF("3333");
			pp1.setPrenom("Jean");
			pp1.setNom("Sanspeur");
			pp1 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp1);

			PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
			pp2.setIdRF("5555");
			pp2.setPrenom("Jacques");
			pp2.setNom("Sansreproche");
			pp2 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp2);

			final CommuneRF commune = communeRFDAO.save(new CommuneRF(61, "La Sarraz", 5498));

			final SituationRF situation = new SituationRF();
			situation.setNoParcelle(22);
			situation.setCommune(commune);

			BienFondsRF immeuble = new BienFondsRF();
			immeuble.setIdRF("232323");
			immeuble.setEgrid("CH3939393939");
			immeuble.addSituation(situation);
			immeuble = (BienFondsRF) immeubleRFDAO.save(immeuble);

			DroitProprietePersonnePhysiqueRF droit0 = new DroitProprietePersonnePhysiqueRF();
			droit0.setMasterIdRF("2892020289292");
			droit0.setVersionIdRF("1");
			droit0.setPart(new Fraction(1, 2));
			droit0.setRegime(GenrePropriete.COPROPRIETE);
			droit0.setDateDebut(RegDate.get(2016, 12, 31));
			droit0.setDateFin(RegDate.get(2017, 2, 28));
			droit0.setDateDebutMetier(dateSuccession);
			droit0.setMotifDebut("Succession");
			droit0.setDateFinMetier(datePartage);
			droit0.setMotifFin("Partage");
			droit0.addRaisonAcquisition(new RaisonAcquisitionRF(dateSuccession, "Succession", new IdentifiantAffaireRF(12, 2000, 1, null)));
			droit0.setImmeuble(immeuble);
			droit0.setAyantDroit(pp1);
			droit0 = (DroitProprietePersonnePhysiqueRF) droitRFDAO.save(droit0);

			DroitProprietePersonnePhysiqueRF droit1 = new DroitProprietePersonnePhysiqueRF();
			droit1.setMasterIdRF("837737272727");
			droit1.setVersionIdRF("1");
			droit1.setPart(new Fraction(1, 2));
			droit1.setRegime(GenrePropriete.COPROPRIETE);
			droit1.setDateDebut(RegDate.get(2016, 12, 31));
			droit1.setDateFin(RegDate.get(2017, 2, 28));
			droit1.setDateDebutMetier(dateSuccession);
			droit1.setMotifDebut("Succession");
			droit1.setDateFinMetier(datePartage);
			droit1.setMotifFin("Partage");
			droit1.addRaisonAcquisition(new RaisonAcquisitionRF(dateSuccession, "Succession", new IdentifiantAffaireRF(12, 2000, 1, null)));
			droit1.setImmeuble(immeuble);
			droit1.setAyantDroit(pp2);
			droit1 = (DroitProprietePersonnePhysiqueRF) droitRFDAO.save(droit1);

			DroitProprietePersonnePhysiqueRF droit2 = new DroitProprietePersonnePhysiqueRF();
			droit2.setMasterIdRF("2892020289292");
			droit2.setVersionIdRF("2");
			droit2.setPart(new Fraction(1, 1));
			droit2.setRegime(GenrePropriete.INDIVIDUELLE);
			droit2.setDateDebut(RegDate.get(2017, 3, 1));
			droit2.setDateFin(null);
			droit2.setDateDebutMetier(datePartage);
			droit2.setMotifDebut("Partage");
			droit2.setDateFinMetier(null);
			droit2.setMotifFin(null);
			droit2.addRaisonAcquisition(new RaisonAcquisitionRF(dateSuccession, "Succession", new IdentifiantAffaireRF(12, 2000, 1, null)));
			droit2.addRaisonAcquisition(new RaisonAcquisitionRF(datePartage, "Partage", new IdentifiantAffaireRF(12, 2005, 44, null)));
			droit2.setImmeuble(immeuble);
			droit2.setAyantDroit(pp1);
			droit2 = (DroitProprietePersonnePhysiqueRF) droitRFDAO.save(droit2);

			ids.immeuble = immeuble.getId();
			ids.droit0 = droit0.getId();
			ids.droit1 = droit1.getId();
			ids.droit2 = droit2.getId();

			return null;
		});

		doInNewTransaction(status -> {
			final RattraperDatesDebutDroitRFProcessorResults rapport = new RattraperDatesDebutDroitRFProcessorResults(RattrapageDataSelection.EXPLICIT_SELECTION, 1, immeubleRFDAO, registreFoncierService);
			processor.processImmeuble(ids.immeuble, rapport);

			// l'immeuble est processé
			final List<Processed> processed = rapport.getProcessed();
			assertNotNull(processed);
			assertEquals(1, processed.size());
			final Processed processed0 = processed.get(0);
			assertEquals(ids.immeuble.longValue(), processed0.getImmeubleId());
			assertEquals("232323", processed0.getIdRF());
			assertEquals("CH3939393939", processed0.getEgrid());
			assertEquals("La Sarraz", processed0.getCommune());
			assertEquals(Integer.valueOf(5498), processed0.getNoOfsCommune());
			assertEquals(22, processed0.getNoParcelle());
			assertNull(processed0.getIndex1());
			assertNull(processed0.getIndex2());
			assertNull(processed0.getIndex3());

			// les trois droits devraient être non-touchés
			assertEmpty(rapport.getUpdated());
			final List<Untouched> untouched = rapport.getUntouched();
			assertNotNull(untouched);
			assertEquals(3, untouched.size());
			untouched.sort(Comparator.comparing(Untouched::getDroitId));

			final Untouched untouched0 = untouched.get(0);
			assertEquals(ids.droit0.longValue(), untouched0.getDroitId());
			assertEquals(ids.immeuble.longValue(), untouched0.getImmeubleId());
			assertEquals("CH3939393939", untouched0.getEgrid());
			assertEquals(RegDate.get(2016, 12, 31), untouched0.getDateDebut());
			assertEquals(RegDate.get(2017, 2, 28), untouched0.getDateFin());
			assertEquals(dateSuccession, untouched0.getDateDebutMetier());
			assertEquals(datePartage, untouched0.getDateFinMetier());
			assertEquals("Succession", untouched0.getMotifDebut());
			assertEquals("Partage", untouched0.getMotifFin());

			final Untouched untouched1 = untouched.get(1);
			assertEquals(ids.droit1.longValue(), untouched1.getDroitId());
			assertEquals(ids.immeuble.longValue(), untouched1.getImmeubleId());
			assertEquals("CH3939393939", untouched1.getEgrid());
			assertEquals(RegDate.get(2016, 12, 31), untouched1.getDateDebut());
			assertEquals(RegDate.get(2017, 2, 28), untouched1.getDateFin());
			assertEquals(dateSuccession, untouched1.getDateDebutMetier());
			assertEquals(datePartage, untouched1.getDateFinMetier());
			assertEquals("Succession", untouched1.getMotifDebut());
			assertEquals("Partage", untouched1.getMotifFin());

			final Untouched untouched2 = untouched.get(2);
			assertEquals(ids.droit2.longValue(), untouched2.getDroitId());
			assertEquals(ids.immeuble.longValue(), untouched2.getImmeubleId());
			assertEquals("CH3939393939", untouched2.getEgrid());
			assertEquals(RegDate.get(2017, 3, 1), untouched2.getDateDebut());
			assertNull(untouched2.getDateFin());
			assertEquals(datePartage, untouched2.getDateDebutMetier());
			assertNull(untouched2.getDateFinMetier());
			assertEquals("Partage", untouched2.getMotifDebut());
			assertNull(untouched2.getMotifFin());

			// pas d'erreur
			assertEmpty(rapport.getErreurs());
			return null;
		});

		// les valeurs en DB doivent être inchangées
		doInNewTransaction(status -> {
			final ImmeubleRF immeuble = immeubleRFDAO.get(ids.immeuble);
			assertNotNull(immeuble);

			final List<DroitProprieteRF> droits = immeuble.getDroitsPropriete().stream()
					.sorted(Comparator.comparing(DroitProprieteRF::getId))
					.collect(Collectors.toList());
			assertEquals(3, droits.size());

			final DroitProprieteRF droit0 = droits.get(0);
			assertEquals(ids.droit0, droit0.getId());
			assertEquals(RegDate.get(2016, 12, 31), droit0.getDateDebut());
			assertEquals(RegDate.get(2017, 2, 28), droit0.getDateFin());
			assertEquals(dateSuccession, droit0.getDateDebutMetier());
			assertEquals(datePartage, droit0.getDateFinMetier());
			assertEquals("Succession", droit0.getMotifDebut());
			assertEquals("Partage", droit0.getMotifFin());

			final DroitProprieteRF droit1 = droits.get(1);
			assertEquals(ids.droit1, droit1.getId());
			assertEquals(RegDate.get(2016, 12, 31), droit1.getDateDebut());
			assertEquals(RegDate.get(2017, 2, 28), droit1.getDateFin());
			assertEquals(dateSuccession, droit1.getDateDebutMetier());
			assertEquals(datePartage, droit1.getDateFinMetier());
			assertEquals("Succession", droit1.getMotifDebut());
			assertEquals("Partage", droit1.getMotifFin());

			final DroitProprieteRF droit2 = droits.get(2);
			assertEquals(ids.droit2, droit2.getId());
			assertEquals(RegDate.get(2017, 3, 1), droit2.getDateDebut());
			assertNull(droit2.getDateFin());
			assertEquals(datePartage, droit2.getDateDebutMetier());
			assertNull(droit2.getDateFinMetier());
			assertEquals("Partage", droit2.getMotifDebut());
			assertNull(droit2.getMotifFin());

			return null;
		});
	}

	/**
	 * Ce test s'assure que le processeur recalcule bien les dates de début qui sont mal calculées
	 */
	@Test
	public void testProcessDatesMalCalculees() throws Exception {

		class Ids {
			Long immeuble;
			Long droit0;
			Long droit1;
			Long droit2;
		}
		final Ids ids = new Ids();

		final RegDate dateSuccession = RegDate.get(2000, 1, 1);
		final RegDate datePartage = RegDate.get(2005, 7, 1);

		// un immeuble :
		// - succession -> deux propriétaires à 1/2
		// - partage -> un seul propriétaire à 1/1 (droit qui évolue)
		doInNewTransaction(status -> {
			PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
			pp1.setIdRF("3333");
			pp1.setPrenom("Jean");
			pp1.setNom("Sanspeur");
			pp1 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp1);

			PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
			pp2.setIdRF("5555");
			pp2.setPrenom("Jacques");
			pp2.setNom("Sansreproche");
			pp2 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp2);

			final CommuneRF commune = communeRFDAO.save(new CommuneRF(61, "La Sarraz", 5498));

			final SituationRF situation = new SituationRF();
			situation.setNoParcelle(22);
			situation.setCommune(commune);

			BienFondsRF immeuble = new BienFondsRF();
			immeuble.setIdRF("232323");
			immeuble.setEgrid("CH3939393939");
			immeuble.addSituation(situation);
			immeuble = (BienFondsRF) immeubleRFDAO.save(immeuble);

			DroitProprietePersonnePhysiqueRF droit0 = new DroitProprietePersonnePhysiqueRF();
			droit0.setMasterIdRF("2892020289292");
			droit0.setVersionIdRF("1");
			droit0.setPart(new Fraction(1, 2));
			droit0.setRegime(GenrePropriete.COPROPRIETE);
			droit0.setDateDebut(RegDate.get(2016, 12, 31));
			droit0.setDateFin(RegDate.get(2017, 2, 28));
			droit0.setDateDebutMetier(dateSuccession);
			droit0.setMotifDebut("Succession");
			droit0.setDateFinMetier(datePartage);
			droit0.setMotifFin("Partage");
			droit0.addRaisonAcquisition(new RaisonAcquisitionRF(dateSuccession, "Succession", new IdentifiantAffaireRF(12, 2000, 1, null)));
			droit0.setImmeuble(immeuble);
			droit0.setAyantDroit(pp1);
			droit0 = (DroitProprietePersonnePhysiqueRF) droitRFDAO.save(droit0);

			DroitProprietePersonnePhysiqueRF droit1 = new DroitProprietePersonnePhysiqueRF();
			droit1.setMasterIdRF("837737272727");
			droit1.setVersionIdRF("1");
			droit1.setPart(new Fraction(1, 2));
			droit1.setRegime(GenrePropriete.COPROPRIETE);
			droit1.setDateDebut(RegDate.get(2016, 12, 31));
			droit1.setDateFin(RegDate.get(2017, 2, 28));
			droit1.setDateDebutMetier(dateSuccession);
			droit1.setMotifDebut("Succession");
			droit1.setDateFinMetier(datePartage);
			droit1.setMotifFin("Partage");
			droit1.addRaisonAcquisition(new RaisonAcquisitionRF(dateSuccession, "Succession", new IdentifiantAffaireRF(12, 2000, 1, null)));
			droit1.setImmeuble(immeuble);
			droit1.setAyantDroit(pp2);
			droit1 = (DroitProprietePersonnePhysiqueRF) droitRFDAO.save(droit1);

			DroitProprietePersonnePhysiqueRF droit2 = new DroitProprietePersonnePhysiqueRF();
			droit2.setMasterIdRF("2892020289292");
			droit2.setVersionIdRF("2");
			droit2.setPart(new Fraction(1, 1));
			droit2.setRegime(GenrePropriete.INDIVIDUELLE);
			droit2.setDateDebut(RegDate.get(2017, 3, 1));
			droit2.setDateFin(null);
			droit2.setDateDebutMetier(dateSuccession);  // <----- devrait être la date de partage
			droit2.setMotifDebut("Succession");
			droit2.setDateFinMetier(null);
			droit2.setMotifFin(null);
			droit2.addRaisonAcquisition(new RaisonAcquisitionRF(dateSuccession, "Succession", new IdentifiantAffaireRF(12, 2000, 1, null)));
			droit2.addRaisonAcquisition(new RaisonAcquisitionRF(datePartage, "Partage", new IdentifiantAffaireRF(12, 2005, 44, null)));
			droit2.setImmeuble(immeuble);
			droit2.setAyantDroit(pp1);
			droit2 = (DroitProprietePersonnePhysiqueRF) droitRFDAO.save(droit2);

			ids.immeuble = immeuble.getId();
			ids.droit0 = droit0.getId();
			ids.droit1 = droit1.getId();
			ids.droit2 = droit2.getId();

			return null;
		});

		doInNewTransaction(status -> {
			final RattraperDatesDebutDroitRFProcessorResults rapport = new RattraperDatesDebutDroitRFProcessorResults(RattrapageDataSelection.EXPLICIT_SELECTION, 1, immeubleRFDAO, registreFoncierService);
			processor.processImmeuble(ids.immeuble, rapport);

			// l'immeuble est processé
			final List<Processed> processed = rapport.getProcessed();
			assertNotNull(processed);
			assertEquals(1, processed.size());
			final Processed processed0 = processed.get(0);
			assertEquals(ids.immeuble.longValue(), processed0.getImmeubleId());
			assertEquals("232323", processed0.getIdRF());
			assertEquals("CH3939393939", processed0.getEgrid());
			assertEquals("La Sarraz", processed0.getCommune());
			assertEquals(Integer.valueOf(5498), processed0.getNoOfsCommune());
			assertEquals(22, processed0.getNoParcelle());
			assertNull(processed0.getIndex1());
			assertNull(processed0.getIndex2());
			assertNull(processed0.getIndex3());

			// la date de début du second droit de la pp1 doit être corrigé
			final List<Updated> updated = rapport.getUpdated();
			assertNotNull(updated);
			assertEquals(1, updated.size());

			final Updated updated0 = updated.get(0);
			assertEquals(ids.droit2.longValue(), updated0.getDroitId());
			assertEquals(ids.immeuble.longValue(), updated0.getImmeubleId());
			assertEquals("CH3939393939", updated0.getEgrid());
			assertEquals(RegDate.get(2017, 3, 1), updated0.getDateDebut());
			assertNull(updated0.getDateFin());
			assertEquals(dateSuccession, updated0.getDateDebutMetierOriginale());
			assertEquals("Succession", updated0.getMotifDebutOriginal());
			assertEquals(datePartage, updated0.getDateDebutMetierCorrigee());
			assertEquals("Partage", updated0.getMotifDebutCorrige());
			assertNull(updated0.getDateFinMetier());
			assertNull(updated0.getMotifFin());

			// les deux autres droits devraient être non-touchés
			final List<Untouched> untouched = rapport.getUntouched();
			assertNotNull(untouched);
			assertEquals(2, untouched.size());
			untouched.sort(Comparator.comparing(Untouched::getDroitId));

			final Untouched untouched0 = untouched.get(0);
			assertEquals(ids.droit0.longValue(), untouched0.getDroitId());
			assertEquals(ids.immeuble.longValue(), untouched0.getImmeubleId());
			assertEquals("CH3939393939", untouched0.getEgrid());
			assertEquals(RegDate.get(2016, 12, 31), untouched0.getDateDebut());
			assertEquals(RegDate.get(2017, 2, 28), untouched0.getDateFin());
			assertEquals(dateSuccession, untouched0.getDateDebutMetier());
			assertEquals(datePartage, untouched0.getDateFinMetier());
			assertEquals("Succession", untouched0.getMotifDebut());
			assertEquals("Partage", untouched0.getMotifFin());

			final Untouched untouched1 = untouched.get(1);
			assertEquals(ids.droit1.longValue(), untouched1.getDroitId());
			assertEquals(ids.immeuble.longValue(), untouched1.getImmeubleId());
			assertEquals("CH3939393939", untouched1.getEgrid());
			assertEquals(RegDate.get(2016, 12, 31), untouched1.getDateDebut());
			assertEquals(RegDate.get(2017, 2, 28), untouched1.getDateFin());
			assertEquals(dateSuccession, untouched1.getDateDebutMetier());
			assertEquals(datePartage, untouched1.getDateFinMetier());
			assertEquals("Succession", untouched1.getMotifDebut());
			assertEquals("Partage", untouched1.getMotifFin());

			// pas d'erreur
			assertEmpty(rapport.getErreurs());
			return null;
		});

		// la date et motif de début du deuxième droit doit être corrigée
		doInNewTransaction(status -> {
			final ImmeubleRF immeuble = immeubleRFDAO.get(ids.immeuble);
			assertNotNull(immeuble);

			final List<DroitProprieteRF> droits = immeuble.getDroitsPropriete().stream()
					.sorted(Comparator.comparing(DroitProprieteRF::getId))
					.collect(Collectors.toList());
			assertEquals(3, droits.size());

			final DroitProprieteRF droit0 = droits.get(0);
			assertEquals(ids.droit0, droit0.getId());
			assertEquals(RegDate.get(2016, 12, 31), droit0.getDateDebut());
			assertEquals(RegDate.get(2017, 2, 28), droit0.getDateFin());
			assertEquals(dateSuccession, droit0.getDateDebutMetier());
			assertEquals(datePartage, droit0.getDateFinMetier());
			assertEquals("Succession", droit0.getMotifDebut());
			assertEquals("Partage", droit0.getMotifFin());

			final DroitProprieteRF droit1 = droits.get(1);
			assertEquals(ids.droit1, droit1.getId());
			assertEquals(RegDate.get(2016, 12, 31), droit1.getDateDebut());
			assertEquals(RegDate.get(2017, 2, 28), droit1.getDateFin());
			assertEquals(dateSuccession, droit1.getDateDebutMetier());
			assertEquals(datePartage, droit1.getDateFinMetier());
			assertEquals("Succession", droit1.getMotifDebut());
			assertEquals("Partage", droit1.getMotifFin());

			final DroitProprieteRF droit2 = droits.get(2);
			assertEquals(ids.droit2, droit2.getId());
			assertEquals(RegDate.get(2017, 3, 1), droit2.getDateDebut());
			assertNull(droit2.getDateFin());
			assertEquals(datePartage, droit2.getDateDebutMetier());
			assertNull(droit2.getDateFinMetier());
			assertEquals("Partage", droit2.getMotifDebut());
			assertNull(droit2.getMotifFin());

			return null;
		});
	}
}