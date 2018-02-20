package ch.vd.unireg.registrefoncier.rattrapage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.unireg.evenement.fiscal.registrefoncier.EvenementFiscalDroit;
import ch.vd.unireg.evenement.fiscal.registrefoncier.EvenementFiscalDroitPropriete;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.ChargeServitudeRF;
import ch.vd.unireg.registrefoncier.CommuneRF;
import ch.vd.unireg.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.DroitProprieteRF;
import ch.vd.unireg.registrefoncier.DroitRF;
import ch.vd.unireg.registrefoncier.Fraction;
import ch.vd.unireg.registrefoncier.GenrePropriete;
import ch.vd.unireg.registrefoncier.IdentifiantAffaireRF;
import ch.vd.unireg.registrefoncier.IdentifiantDroitRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.PersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.RaisonAcquisitionRF;
import ch.vd.unireg.registrefoncier.RegistreFoncierService;
import ch.vd.unireg.registrefoncier.ServitudeRF;
import ch.vd.unireg.registrefoncier.SituationRF;
import ch.vd.unireg.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.unireg.registrefoncier.dao.CommuneRFDAO;
import ch.vd.unireg.registrefoncier.dao.DroitRFDAO;
import ch.vd.unireg.registrefoncier.dao.ImmeubleRFDAO;

import static ch.vd.unireg.registrefoncier.rattrapage.RattraperDatesMetierDroitRFProcessorResults.DebutUpdated;
import static ch.vd.unireg.registrefoncier.rattrapage.RattraperDatesMetierDroitRFProcessorResults.Processed;
import static ch.vd.unireg.registrefoncier.rattrapage.RattraperDatesMetierDroitRFProcessorResults.Untouched;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class RattraperDatesMetierDroitRFProcessorTest extends BusinessTest {

	private CommuneRFDAO communeRFDAO;
	private ImmeubleRFDAO immeubleRFDAO;
	private AyantDroitRFDAO ayantDroitRFDAO;
	private DroitRFDAO droitRFDAO;
	private RattraperDatesMetierDroitRFProcessor processor;
	private RegistreFoncierService registreFoncierService;
	private EvenementFiscalDAO evenementFiscalDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		communeRFDAO = getBean(CommuneRFDAO.class, "communeRFDAO");
		immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		ayantDroitRFDAO = getBean(AyantDroitRFDAO.class, "ayantDroitRFDAO");
		droitRFDAO = getBean(DroitRFDAO.class, "droitRFDAO");
		processor = getBean(RattraperDatesMetierDroitRFProcessor.class, "rattraperDatesMetierDroitRFProcessor");
		registreFoncierService = getBean(RegistreFoncierService.class, "serviceRF");
		evenementFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
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
			final RattraperDatesMetierDroitRFProcessorResults rapport = new RattraperDatesMetierDroitRFProcessorResults(RattrapageDataSelection.EXPLICIT_SELECTION, 1, immeubleRFDAO, registreFoncierService);
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
			assertEmpty(rapport.getDebutUpdated());
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
			final RattraperDatesMetierDroitRFProcessorResults rapport = new RattraperDatesMetierDroitRFProcessorResults(RattrapageDataSelection.EXPLICIT_SELECTION, 1, immeubleRFDAO, registreFoncierService);
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
			final List<DebutUpdated> debutUpdated = rapport.getDebutUpdated();
			assertNotNull(debutUpdated);
			assertEquals(1, debutUpdated.size());

			final DebutUpdated debutUpdated0 = debutUpdated.get(0);
			assertEquals(ids.droit2.longValue(), debutUpdated0.getDroitId());
			assertEquals(ids.immeuble.longValue(), debutUpdated0.getImmeubleId());
			assertEquals("CH3939393939", debutUpdated0.getEgrid());
			assertEquals(RegDate.get(2017, 3, 1), debutUpdated0.getDateDebut());
			assertNull(debutUpdated0.getDateFin());
			assertEquals(dateSuccession, debutUpdated0.getDateDebutMetierInitiale());
			assertEquals("Succession", debutUpdated0.getMotifDebutInitial());
			assertEquals(datePartage, debutUpdated0.getDateDebutMetierCorrigee());
			assertEquals("Partage", debutUpdated0.getMotifDebutCorrige());
			assertNull(debutUpdated0.getDateFinMetier());
			assertNull(debutUpdated0.getMotifFin());

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

	/**
	 * Vérifie que le traitement ne fait rien si tous les droits sont déjà fermés
	 */
	@Test
	public void testProcessDatesMetierDroitsDejaFermes() throws Exception {

		// un immeuble avec deux droits dont les dates métier sont renseignées
		final Long id = doInNewTransaction(status -> {
			final CommuneRF commune = addCommuneRF(61, "La Sarraz", 5498);
			final BienFondsRF bienFonds = addBienFondsRF("38383838", "CHE478391947", commune, 234);
			final PersonnePhysiqueRF jean = addPersonnePhysiqueRF("02893039", "Jean", "Routourne", RegDate.get(1962, 9, 12));
			final PersonnePhysiqueRF jacques = addPersonnePhysiqueRF("937823a0a02", "Jacques", "Roubloque", RegDate.get(1968, 1, 24));
			addDroitPropriete(jean, bienFonds, null, GenrePropriete.INDIVIDUELLE, new Fraction(1, 1),
			                  null, RegDate.get(2003, 6, 4), RegDate.get(1990, 3, 1), RegDate.get(2003, 5, 13), "Achat", "Vente",
			                  new IdentifiantAffaireRF(8, 1990, 3, 0), "473839273923", "473839273922");
			addDroitPropriete(jacques, bienFonds, null, GenrePropriete.INDIVIDUELLE, new Fraction(1, 1),
			                  RegDate.get(2003, 6, 5), null, RegDate.get(2003, 5, 13), null, "Achat", null,
			                  new IdentifiantAffaireRF(8, 2003, 21, 0), "45838923783", "45838923782");
			return bienFonds.getId();
		});

		// on détecte les dates métiers manquantes sur les droits
		doInNewTransaction(status -> {
			final RattraperDatesMetierDroitRFProcessorResults rapport = new RattraperDatesMetierDroitRFProcessorResults(RattrapageDataSelection.EXPLICIT_SELECTION, 1, immeubleRFDAO, registreFoncierService);
			processor.processImmeuble(id, rapport);
			return null;
		});

		// les dates des droits devraient être intouchées
		doInNewTransaction(status -> {
			final ImmeubleRF immeuble = immeubleRFDAO.get(id);
			assertNotNull(immeuble);

			final List<DroitProprieteRF> droits = new ArrayList<>(immeuble.getDroitsPropriete());
			assertEquals(2, droits.size());
			droits.sort(new DateRangeComparator<>());
			assertDatesDroit(null, RegDate.get(2003, 6, 4), RegDate.get(1990, 3, 1), RegDate.get(2003, 5, 13), "Achat", "Vente", droits.get(0));
			assertDatesDroit(RegDate.get(2003, 6, 5), null, RegDate.get(2003, 5, 13), null, "Achat", null, droits.get(1));
			return null;
		});

		// postcondition : aucun événement fiscal n'a été envoyé
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(0, events.size());
			return null;
		});
	}

	/**
	 * [SIFISC-24558] Vérifie que le traitement ne crashe pas lorsqu'il rencontre des servitudes et qu'il les ignore.
	 */
	@Test
	public void testProcessIgnoreLesServitudes() throws Exception {

		// un immeuble avec juste une servitude
		final Long id = doInNewTransaction(status -> {
			final CommuneRF commune = addCommuneRF(61, "La Sarraz", 5498);
			final BienFondsRF bienFonds = addBienFondsRF("38383838", "CHE478391947", commune, 234);
			final PersonnePhysiqueRF jean = addPersonnePhysiqueRF("02893039", "Jean", "Routourne", RegDate.get(1962, 9, 12));
			final PersonnePhysiqueRF jacques = addPersonnePhysiqueRF("937823a0a02", "Jacques", "Roubloque", RegDate.get(1968, 1, 24));
			addUsufruitRF(null, RegDate.get(1990, 3, 1), RegDate.get(2017,1,13), null, null, null, "32727817", "1",
			              new IdentifiantAffaireRF(8, 1990, 3, 0), new IdentifiantDroitRF(8, 1990, 3),
			              Arrays.asList(jean, jacques), Collections.singletonList(bienFonds));
			return bienFonds.getId();
		});

		// on démarre le batch
		doInNewTransaction(status -> {
			final RattraperDatesMetierDroitRFProcessorResults rapport = new RattraperDatesMetierDroitRFProcessorResults(RattrapageDataSelection.EXPLICIT_SELECTION, 1, immeubleRFDAO, registreFoncierService);
			processor.processImmeuble(id, rapport);
			return null;
		});

		// les dates des droits devraient être intouchées
		doInNewTransaction(status -> {
			final ImmeubleRF immeuble = immeubleRFDAO.get(id);
			assertNotNull(immeuble);

			// il n'y a pas de droit
			assertEmpty(immeuble.getDroitsPropriete());

			// la servitude est intouchée
			final Set<ChargeServitudeRF> lienImmeubles = immeuble.getChargesServitudes();
			assertNotNull(lienImmeubles);
			assertEquals(1, lienImmeubles.size());
			final ChargeServitudeRF lien0 = lienImmeubles.iterator().next();
			assertEquals(RegDate.get(1990, 3, 1), lien0.getDateDebut());
			assertNull(lien0.getDateFin());
			final ServitudeRF servitude0 = lien0.getServitude();
			assertNull(servitude0.getDateDebut());
			assertEquals(RegDate.get(2017, 1, 13), servitude0.getDateFin());
			assertEquals(RegDate.get(1990, 3, 1), servitude0.getDateDebutMetier());
			assertNull(servitude0.getDateFinMetier());
			return null;
		});

		// postcondition : aucun événement fiscal n'a été envoyé
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(0, events.size());
			return null;
		});
	}

	/**
	 * Vérifie que le traitement calcule bien la date de fin métier dans le cas simple d'un achat/vente d'un immeuble en propriété individuelle
	 */
	@Test
	public void testProcessDatesMetierVenteAchatProprieteIndividuelle() throws Exception {

		// un immeuble avec deux droits correspondant à la vente/achat de celui-li
		final Long id = doInNewTransaction(status -> {
			final CommuneRF commune = addCommuneRF(61, "La Sarraz", 5498);
			final BienFondsRF bienFonds = addBienFondsRF("38383838", "CHE478391947", commune, 234);
			final PersonnePhysiqueRF jean = addPersonnePhysiqueRF("02893039", "Jean", "Routourne", RegDate.get(1962, 9, 12));
			final PersonnePhysiqueRF jacques = addPersonnePhysiqueRF("937823a0a02", "Jacques", "Roubloque", RegDate.get(1968, 1, 24));
			addDroitPropriete(jean, bienFonds, null, GenrePropriete.INDIVIDUELLE, new Fraction(1, 1),
			                  null, RegDate.get(2003, 6, 4), RegDate.get(1990, 3, 1), null, "Achat", null,
			                  new IdentifiantAffaireRF(8, 1990, 3, 0), "473839273923", "473839273922");
			addDroitPropriete(jacques, bienFonds, null, GenrePropriete.INDIVIDUELLE, new Fraction(1, 1),
			                  RegDate.get(2003, 6, 5), null, RegDate.get(2003, 5, 13), null, "Achat", null,
			                  new IdentifiantAffaireRF(8, 2003, 21, 0), "45838923783", "45838923782");
			return bienFonds.getId();
		});

		// on détecte les dates métiers manquantes sur les droits
		doInNewTransaction(status -> {
			final RattraperDatesMetierDroitRFProcessorResults rapport = new RattraperDatesMetierDroitRFProcessorResults(RattrapageDataSelection.EXPLICIT_SELECTION, 1, immeubleRFDAO, registreFoncierService);
			processor.processImmeuble(id, rapport);
			return null;
		});

		// la date de fin métier du premier droit devrait maintenant être renseignée
		doInNewTransaction(status -> {
			final ImmeubleRF immeuble = immeubleRFDAO.get(id);
			assertNotNull(immeuble);

			final List<DroitProprieteRF> droits = new ArrayList<>(immeuble.getDroitsPropriete());
			assertEquals(2, droits.size());
			droits.sort(new DateRangeComparator<>());
			assertDatesDroit(null, RegDate.get(2003, 6, 4), RegDate.get(1990, 3, 1), RegDate.get(2003, 5, 13), "Achat", "Vente", droits.get(0));
			assertDatesDroit(RegDate.get(2003, 6, 5), null, RegDate.get(2003, 5, 13), null, "Achat", null, droits.get(1));
			return null;
		});

		// postcondition : les événements fiscaux correspondants ont été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());

			final EvenementFiscalDroitPropriete event0 = (EvenementFiscalDroitPropriete) events.get(0);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.MODIFICATION, event0.getType());
			assertEquals(RegDate.get(2003, 5, 13), event0.getDateValeur());
			assertEquals("38383838", event0.getDroit().getImmeuble().getIdRF());
			assertEquals("02893039", event0.getDroit().getAyantDroit().getIdRF());

			return null;
		});
	}

	/**
	 * Vérifie que le traitement calcule bien la date de fin métier séparant les affaires par ayant-droit lors du rattrapage de l'import initial.
	 */
	@Test
	public void testProcessDatesMetierRattrapageImportInitial() throws Exception {

		class Ids {
			long immeuble;
			long jean;
			long jacques;
		}
		final Ids ids = new Ids();

		// un immeuble avec des droits correspondants à la vente/achat qui s'étendent sur plusieurs années
		doInNewTransaction(status -> {
			final CommuneRF commune = addCommuneRF(61, "La Sarraz", 5498);
			final BienFondsRF bienFonds = addBienFondsRF("38383838", "CHE478391947", commune, 234);

			final PersonnePhysiqueRF jean = addPersonnePhysiqueRF("02893039", "Jean", "Routourne", RegDate.get(1962, 9, 12));
			final PersonnePhysiqueRF jacques = addPersonnePhysiqueRF("937823a0a02", "Jacques", "Roubloque", RegDate.get(1968, 1, 24));
			ids.immeuble = bienFonds.getId();
			ids.jean = jean.getId();
			ids.jacques = jacques.getId();

			// import inital du 31.12.2016
			addDroitPropriete(jean, bienFonds, null, GenrePropriete.COPROPRIETE, new Fraction(1, 2),
			                  null, RegDate.get(2017, 1, 6), RegDate.get(1997, 4, 21), null, "Succession", null,
			                  new IdentifiantAffaireRF(5, null, 151264, null), "473839273923", "473839273922");
			// second import du 07.02.2017
			addDroitPropriete(jean, bienFonds, null, GenrePropriete.COPROPRIETE, new Fraction(1, 2),
			                  RegDate.get(2017, 1, 7), null, RegDate.get(2001, 9, 4), null, "Succession", null,
			                  new IdentifiantAffaireRF(5, 2001, 2174, 0), "2929181981818", "2929181981817");

			// import inital du 31.12.2016
			addDroitPropriete(jacques, bienFonds, null, GenrePropriete.COPROPRIETE, new Fraction(1, 2),
			                  null, RegDate.get(2017, 1, 6), RegDate.get(2012, 3, 6), null, "Succession", null,
			                  new IdentifiantAffaireRF(5, 2012, 617, 0), "45838923783", "45838923782");
			// second import du 07.02.2017
			addDroitPropriete(jacques, bienFonds, null, GenrePropriete.COPROPRIETE, new Fraction(1, 2),
			                  RegDate.get(2017, 1, 7), null, RegDate.get(2013, 5, 31), null, "Succession", null,
			                  new IdentifiantAffaireRF(5, 2012, 617, 0), "3838929217821", "3838929217820");

			return null;
		});

		// on calcule les dates métiers manquantes sur les droits
		doInNewTransaction(status -> {
			final RattraperDatesMetierDroitRFProcessorResults rapport = new RattraperDatesMetierDroitRFProcessorResults(RattrapageDataSelection.EXPLICIT_SELECTION, 1, immeubleRFDAO, registreFoncierService);
			processor.processImmeuble(ids.immeuble, rapport);
			return null;
		});

		// les dates de fin métier doivent être calculées par ayant-droit
		doInNewTransaction(status -> {
			final ImmeubleRF immeuble = immeubleRFDAO.get(ids.immeuble);
			assertNotNull(immeuble);

			final List<DroitProprieteRF> droits = new ArrayList<>(immeuble.getDroitsPropriete());
			assertEquals(4, droits.size());
			droits.sort(Comparator.<DroitProprieteRF, Long>comparing(d -> d.getAyantDroit().getId())
					            .thenComparing(DateRangeComparator::compareRanges));

			// les droits de jean
			assertDatesDroit(null, RegDate.get(2017, 1, 6), RegDate.get(1997, 4, 21), RegDate.get(2001, 9, 4), "Succession", "Succession", droits.get(0));
			assertDatesDroit(RegDate.get(2017, 1, 7), null, RegDate.get(2001, 9, 4), null, "Succession", null, droits.get(1));

			// les droits de jacques
			assertDatesDroit(null, RegDate.get(2017, 1, 6), RegDate.get(2012, 3, 6), RegDate.get(2013, 5, 31), "Succession", "Succession", droits.get(2));
			assertDatesDroit(RegDate.get(2017, 1, 7), null, RegDate.get(2013, 5, 31), null, "Succession", null, droits.get(3));
			return null;
		});

		// postcondition : les événements fiscaux correspondants ont été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(2, events.size());
			events.sort(Comparator.comparing(EvenementFiscal::getDateValeur));

			final EvenementFiscalDroitPropriete event0 = (EvenementFiscalDroitPropriete) events.get(0);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.MODIFICATION, event0.getType());
			assertEquals(RegDate.get(2001, 9, 4), event0.getDateValeur());
			assertEquals("38383838", event0.getDroit().getImmeuble().getIdRF());
			assertEquals("02893039", event0.getDroit().getAyantDroit().getIdRF());

			final EvenementFiscalDroitPropriete event1 = (EvenementFiscalDroitPropriete) events.get(1);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.MODIFICATION, event1.getType());
			assertEquals(RegDate.get(2013, 5, 31), event1.getDateValeur());
			assertEquals("38383838", event1.getDroit().getImmeuble().getIdRF());
			assertEquals("937823a0a02", event1.getDroit().getAyantDroit().getIdRF());

			return null;
		});
	}

	/*
	 * Vérifie que le traitement calcule bien la date de fin métier regroupant les droits par ayant-droit lors du rattrapage de l'import initial.
	 */
	@Test
	public void testProcessDatesMetierRattrapageImportInitial2() throws Exception {

		final String idPPRF1 = "_1f1091523810460801381046a6460f8d";
		final String idPPRF2 = "_1f109152381026b501381026fd906a80";
		final String idImmeubleRF1 = "_1f109152381026b501381028da172cca";

		// précondition : il y a déjà deux droits dans la base de données
		final Long immeubleId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				final PersonnePhysiqueRF pp0 = addPersonnePhysiqueRF(idPPRF1, "Isabelle", "Tissot", RegDate.get(1900, 1, 1));
				final PersonnePhysiqueRF pp1 = addPersonnePhysiqueRF(idPPRF2, "Simone", "Tissot", RegDate.get(1900, 1, 1));

				final CommuneRF commune = addCommuneRF(61, "La Sarraz", 5498);
				final BienFondsRF immeuble = addBienFondsRF(idImmeubleRF1, "CHE393939", commune, 12);

				// import inital du 31.12.2016
				addDroitPropriete(pp0, immeuble, null, GenrePropriete.COPROPRIETE, new Fraction(1, 2),
				                  null, RegDate.get(2016, 12, 31), RegDate.get(1961, 2, 4), null, "Succession", null,
				                  new IdentifiantAffaireRF(11, null, 74634, null), "1f109152381026b5013810299b0b1905", "1f109152381026b5013810299b0b1904");
				// second import du 01.01.2017
				addDroitPropriete(pp0, immeuble, null, GenrePropriete.COPROPRIETE, new Fraction(1, 2),
				                  RegDate.get(2017, 1, 1), null, RegDate.get(1980, 12, 29), null, "Changement de régime", null,
				                  new IdentifiantAffaireRF(11, null, 95580, null), "21321321", "21321320");

				// import inital du 31.12.2016
				addDroitPropriete(pp1, immeuble, null, GenrePropriete.COPROPRIETE, new Fraction(1, 2),
				                  null, RegDate.get(2016, 12, 31), RegDate.get(1980, 12, 29), null, "Changement de régime", null,
				                  new IdentifiantAffaireRF(11, null, 95580, null), "1f109152381026b5013810299b0b1908", "1f109152381026b5013810299b0b1907");
				// second import du 01.01.2017
				addDroitPropriete(pp1, immeuble, null, GenrePropriete.COPROPRIETE, new Fraction(1, 2),
				                  RegDate.get(2017, 1, 1), null, RegDate.get(1998, 2, 11), null, "Succession", null,
				                  new IdentifiantAffaireRF(11, null, 115039, null), "90392039", "90392038");

				return immeuble.getId();
			}
		});

		// on calcule les dates métiers manquantes sur les droits
		doInNewTransaction(status -> {
			final RattraperDatesMetierDroitRFProcessorResults rapport = new RattraperDatesMetierDroitRFProcessorResults(RattrapageDataSelection.EXPLICIT_SELECTION, 1, immeubleRFDAO, registreFoncierService);
			processor.processImmeuble(immeubleId, rapport);
			return null;
		});

		// les dates de fin métier doivent être calculées par ayant-droit
		doInNewTransaction(status -> {
			final ImmeubleRF immeuble = immeubleRFDAO.get(immeubleId);
			assertNotNull(immeuble);

			final List<DroitProprieteRF> droits = new ArrayList<>(immeuble.getDroitsPropriete());
			droits.sort(Comparator.<DroitProprieteRF, Long>comparing(d -> d.getAyantDroit().getId())
					            .thenComparing(DateRangeComparator::compareRanges));

			// les droits d'Isabelle
			assertDatesDroit(null, RegDate.get(2016, 12, 31), RegDate.get(1961, 2, 4), RegDate.get(1980, 12, 29), "Succession", "Changement de régime", droits.get(0));
			assertDatesDroit(RegDate.get(2017, 1, 1), null, RegDate.get(1980, 12, 29), null, "Changement de régime", null, droits.get(1));

			// les droits de Simone
			assertDatesDroit(null, RegDate.get(2016, 12, 31), RegDate.get(1980, 12, 29), RegDate.get(1998, 2, 11), "Changement de régime", "Succession", droits.get(2));
			assertDatesDroit(RegDate.get(2017, 1, 1), null, RegDate.get(1998, 2, 11), null, "Succession", null, droits.get(3));
			return null;
		});

		// postcondition : les événements fiscaux correspondants ont été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(2, events.size());
			events.sort(Comparator.comparing(EvenementFiscal::getDateValeur));

			final EvenementFiscalDroitPropriete event0 = (EvenementFiscalDroitPropriete) events.get(0);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.MODIFICATION, event0.getType());
			assertEquals(RegDate.get(1980, 12, 29), event0.getDateValeur());
			assertEquals(idImmeubleRF1, event0.getDroit().getImmeuble().getIdRF());
			assertEquals(idPPRF1, event0.getDroit().getAyantDroit().getIdRF());

			final EvenementFiscalDroitPropriete event1 = (EvenementFiscalDroitPropriete) events.get(1);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.MODIFICATION, event1.getType());
			assertEquals(RegDate.get(1998, 2, 11), event1.getDateValeur());
			assertEquals(idImmeubleRF1, event1.getDroit().getImmeuble().getIdRF());
			assertEquals(idPPRF2, event1.getDroit().getAyantDroit().getIdRF());

			return null;
		});
	}

	private static void assertDatesDroit(RegDate dateDebut, RegDate dateFin, RegDate dateDebutMetier, RegDate dateFinMetier, String motifDebut, String motifFin, DroitRF droit) {
		assertNotNull(droit);
		assertEquals(dateDebut, droit.getDateDebut());
		assertEquals(dateFin, droit.getDateFin());
		assertEquals(dateDebutMetier, droit.getDateDebutMetier());
		assertEquals(dateFinMetier, droit.getDateFinMetier());
		assertEquals(motifDebut, droit.getMotifDebut());
		assertEquals(motifFin, droit.getMotifFin());
	}
}