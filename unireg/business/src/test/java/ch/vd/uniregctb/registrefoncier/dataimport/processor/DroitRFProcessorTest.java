package ch.vd.uniregctb.registrefoncier.dataimport.processor;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscal;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.evenement.fiscal.registrefoncier.EvenementFiscalDroit;
import ch.vd.uniregctb.evenement.fiscal.registrefoncier.EvenementFiscalDroitPropriete;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.uniregctb.evenement.registrefoncier.TypeMutationRF;
import ch.vd.uniregctb.registrefoncier.BienFondsRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteImmeubleRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.DroitRFRangeMetierComparator;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleBeneficiaireRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.RaisonAcquisitionRF;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.DroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.processor.MutationRFProcessorTestCase;
import ch.vd.uniregctb.rf.GenrePropriete;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

@SuppressWarnings("Duplicates")
public class DroitRFProcessorTest extends MutationRFProcessorTestCase {

	private AyantDroitRFDAO ayantDroitRFDAO;
	private ImmeubleRFDAO immeubleRFDAO;
	private EvenementRFMutationDAO evenementRFMutationDAO;
	private EvenementFiscalDAO evenementFiscalDAO;
	private DroitRFDAO droitRFDAO;
	private DroitRFProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		this.ayantDroitRFDAO = getBean(AyantDroitRFDAO.class, "ayantDroitRFDAO");
		this.immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		this.evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");
		this.evenementFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
		this.droitRFDAO = getBean(DroitRFDAO.class, "droitRFDAO");
		final XmlHelperRF xmlHelperRF = getBean(XmlHelperRF.class, "xmlHelperRF");
		final EvenementFiscalService evenementFiscalService = getBean(EvenementFiscalService.class, "evenementFiscalService");

		this.processor = new DroitRFProcessor(ayantDroitRFDAO, immeubleRFDAO, droitRFDAO, xmlHelperRF, evenementFiscalService);
	}

	/**
	 * Ce test vérifie qu'une mutation déjà traitée ne peut pas être traitée une seconde fois.
	 */
	@Test
	public void testProcessMutationDejaTraitee() throws Exception {

		final EvenementRFMutation mutation = new EvenementRFMutation();
		mutation.setId(1L);
		mutation.setEtat(EtatEvenementRF.TRAITE);
		try {
			processor.process(mutation, false, null);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("La mutation n°1 est déjà traitée (état=[TRAITE]).", e.getMessage());
		}
	}

	/**
	 * Ce test vérifie qu'une mutation forcée ne peut pas être traitée.
	 */
	@Test
	public void testProcessMutationForcee() throws Exception {

		final EvenementRFMutation mutation = new EvenementRFMutation();
		mutation.setId(1L);
		mutation.setEtat(EtatEvenementRF.FORCE);
		try {
			processor.process(mutation, false, null);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("La mutation n°1 est déjà traitée (état=[FORCE]).", e.getMessage());
		}
	}

	/**
	 * [SIFISC-22400] Ce test vérifie que le processing d'une mutation de création crée bien des nouveaux droits avec la date de début correspondant à la plus ancienne raison d'acquisition.
	 */
	@Test
	public void testProcessMutationCreation() throws Exception {

		final RegDate dateImport = RegDate.get(2016, 10, 1);

		// précondition : la base est vide
		doInNewTransaction(status -> {
			assertEquals(0, droitRFDAO.getAll().size());
			assertEquals(0, evenementFiscalDAO.getAll().size());
			return null;
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_droit_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère quelques données satellites
		final Long ppId = insertPP("_1f109152381009be0138100a1d442eee", "Schulz", "Alodie", RegDate.get(1900, 1, 1));
		insertImmeuble("_8af806fc4a35927c014ae2a6e76041b8");
		insertImmeuble("_1f109152381009be0138100ba7e31031");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateImport, TypeEntiteRF.DROIT, TypeMutationRF.CREATION, "_1f109152381009be0138100a1d442eee", null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, true, null);
			return null;
		});

		// postcondition : la mutation est traitée et les nouveaux droits sont créés
		doInNewTransaction(status -> {

			final PersonnePhysiqueRF pp = (PersonnePhysiqueRF) ayantDroitRFDAO.get(ppId);
			assertNotNull(pp);

			final Set<DroitProprieteRF> droits = pp.getDroitsPropriete();
			assertNotNull(droits);
			assertEquals(2, droits.size());

			final List<DroitRF> droitList = new ArrayList<>(droits);
			droitList.sort(Comparator.comparing(DroitRF::getMasterIdRF));

			final DroitProprietePersonnePhysiqueRF droit0 = (DroitProprietePersonnePhysiqueRF) droitList.get(0);
			assertNotNull(droit0);
			assertEquals("1f109152381009be0138100c87276e68", droit0.getMasterIdRF());
			assertEquals("1f109152381009be0138100e4c7c00e5", droit0.getVersionIdRF());
			assertNull(droit0.getDateDebut());
			assertNull(droit0.getDateFin());
			assertEquals("_1f109152381009be0138100ba7e31031", droit0.getImmeuble().getIdRF());
			assertEquals(new Fraction(1, 1), droit0.getPart());
			assertEquals(GenrePropriete.INDIVIDUELLE, droit0.getRegime());

			final List<RaisonAcquisitionRF> raisons0 = new ArrayList<>(droit0.getRaisonsAcquisition());
			raisons0.sort(Comparator.naturalOrder());
			assertEquals(2, raisons0.size());
			assertRaisonAcquisition(RegDate.get(2005, 1, 1), "Achat", new IdentifiantAffaireRF(13, 2005, 173, 0), raisons0.get(0));
			assertRaisonAcquisition(RegDate.get(2007, 2, 7), "Donation", new IdentifiantAffaireRF(13, 2007, 173, 0), raisons0.get(1));

			final DroitProprietePersonnePhysiqueRF droit1 = (DroitProprietePersonnePhysiqueRF) droitList.get(1);
			assertNotNull(droit1);
			assertEquals("8af806fa4a4dd302014b16fc17266a0b", droit1.getMasterIdRF());
			assertEquals("8af806fa4a4dd302014b16fc17256a06", droit1.getVersionIdRF());
			assertNull(droit1.getDateDebut());
			assertNull(droit1.getDateFin());
			assertEquals("Succession", droit1.getMotifDebut());
			assertEquals(RegDate.get(2003, 1, 1), droit1.getDateDebutMetier());
			assertEquals("_8af806fc4a35927c014ae2a6e76041b8", droit1.getImmeuble().getIdRF());
			assertEquals(new Fraction(1, 2), droit1.getPart());
			assertEquals(GenrePropriete.COPROPRIETE, droit1.getRegime());

			final List<RaisonAcquisitionRF> raisons1 = new ArrayList<>(droit1.getRaisonsAcquisition());
			raisons1.sort(Comparator.naturalOrder());
			assertEquals(2, raisons1.size());
			assertRaisonAcquisition(RegDate.get(2003, 1, 1), "Succession", new IdentifiantAffaireRF(6, 2003, 9593, 0), raisons1.get(0));
			assertRaisonAcquisition(RegDate.get(2014, 12, 23), "Achat", new IdentifiantAffaireRF(6, 2014, 9593, 0), raisons1.get(1));
			return null;
		});

		// postcondition : les événements fiscaux correspondants ont été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(2, events.size());
			events.sort(Comparator.comparing(EvenementFiscal::getDateValeur));

			final EvenementFiscalDroitPropriete event0 = (EvenementFiscalDroitPropriete) events.get(0);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.OUVERTURE, event0.getType());
			assertEquals(RegDate.get(2003, 1, 1), event0.getDateValeur());
			assertEquals("_8af806fc4a35927c014ae2a6e76041b8", event0.getDroit().getImmeuble().getIdRF());
			assertEquals(ppId, event0.getDroit().getAyantDroit().getId());

			final EvenementFiscalDroitPropriete event1 = (EvenementFiscalDroitPropriete) events.get(1);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.OUVERTURE, event1.getType());
			assertEquals(RegDate.get(2005, 1, 1), event1.getDateValeur());
			assertEquals("_1f109152381009be0138100ba7e31031", event1.getDroit().getImmeuble().getIdRF());
			assertEquals(ppId, event1.getDroit().getAyantDroit().getId());

			return null;
		});
	}

	/**
	 * [SIFISC-23985] Ce test vérifie que le processing d'une mutation de création pour un droit entre immeubles fonctionne bien.
	 */
	@Test
	public void testProcessMutationCreationDroitEntreImmeuble() throws Exception {

		final RegDate dateImport = RegDate.get(2016, 10, 1);

		// précondition : la base est vide
		doInNewTransaction(status -> {
			assertEquals(0, droitRFDAO.getAll().size());
			return null;
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_droit_immeuble_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère quelques données satellites
		insertImmeuble("_8af806fc4a35927c014ae2a6e76041b8");
		insertImmeuble("_1f109152381009be0138100ba7e31031");
		final Long immId = insertImmeubleBeneficiaire("_8af806fc4a35927c014ae2a6e76041b8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateImport, TypeEntiteRF.DROIT, TypeMutationRF.CREATION, "_8af806fc4a35927c014ae2a6e76041b8", null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, true, null);
			return null;
		});

		// postcondition : la mutation est traitée et le nouveau droit est créé
		doInNewTransaction(status -> {

			final ImmeubleBeneficiaireRF imm = (ImmeubleBeneficiaireRF) ayantDroitRFDAO.get(immId);
			assertNotNull(imm);

			final Set<DroitProprieteRF> droits = imm.getDroitsPropriete();
			assertNotNull(droits);
			assertEquals(1, droits.size());

			final DroitProprieteImmeubleRF droit0 = (DroitProprieteImmeubleRF) droits.iterator().next();
			assertNotNull(droit0);
			assertEquals("3838292", droit0.getMasterIdRF());
			assertEquals("3838291", droit0.getVersionIdRF());
			assertNull(droit0.getDateDebut());
			assertNull(droit0.getDateFin());
			assertEquals("_1f109152381009be0138100ba7e31031", droit0.getImmeuble().getIdRF());
			assertEquals(new Fraction(1, 1), droit0.getPart());
			assertEquals(GenrePropriete.FONDS_DOMINANT, droit0.getRegime());

			final Set<RaisonAcquisitionRF> raisons0 = droit0.getRaisonsAcquisition();
			assertEquals(1, raisons0.size());
			assertRaisonAcquisition(RegDate.get(2010, 4, 11), "Constitution de PPE", new IdentifiantAffaireRF(6, 2013, 17, 0), raisons0.iterator().next());
			return null;
		});

		// postcondition : les événements fiscaux correspondants ont été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());

			final EvenementFiscalDroitPropriete event0 = (EvenementFiscalDroitPropriete) events.get(0);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.OUVERTURE, event0.getType());
			assertEquals(RegDate.get(2010, 4, 11), event0.getDateValeur());
			assertEquals("_1f109152381009be0138100ba7e31031", event0.getDroit().getImmeuble().getIdRF());
			assertEquals(immId, event0.getDroit().getAyantDroit().getId());

			return null;
		});
	}

	/*
	 * Ce test vérifie que le processing d'une mutation de modification de droits fonctionne bien.
	 */
	@Test
	public void testProcessMutationModification() throws Exception {

		final String idPPRF = "_1f109152381009be0138100a1d442eee";
		final String idImmeubleRF1 = "_1f109152381009be0138100ba7e31031";
		final String idImmeubleRF2 = "_8af806fc4a35927c014ae2a6e76041b8";
		final RegDate dateImportInitial = RegDate.get(2015, 3, 17);
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		// précondition : il y a déjà deux droits dans la base de données
		final Long ppId = doInNewTransaction(status -> {

			PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
			pp.setIdRF(idPPRF);
			pp.setNom("Schulz");
			pp.setPrenom("Alodie");
			pp.setDateNaissance(RegDate.get(1900, 1, 1));
			pp = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp);

			BienFondsRF immeuble1 = new BienFondsRF();
			immeuble1.setIdRF(idImmeubleRF1);
			immeuble1 = (BienFondsRF) immeubleRFDAO.save(immeuble1);

			BienFondsRF immeuble2 = new BienFondsRF();
			immeuble2.setIdRF(idImmeubleRF2);
			immeuble2 = (BienFondsRF) immeubleRFDAO.save(immeuble2);

			// on droit différent de celui qui arrive dans le fichier XML
			final DroitProprietePersonnePhysiqueRF droit0 = new DroitProprietePersonnePhysiqueRF();
			droit0.setMasterIdRF("1f109152381009be0138100c87000000");
			droit0.setVersionIdRF("1f109152381009be0138100c87000001");
			droit0.setDateDebut(dateImportInitial);
			droit0.setAyantDroit(pp);
			droit0.setImmeuble(immeuble1);
			droit0.setPart(new Fraction(1, 1));
			droit0.setRegime(GenrePropriete.INDIVIDUELLE);
			// motif différent
			droit0.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2007, 2, 7), "Appropriation illégitime", new IdentifiantAffaireRF(13, 2007, 173, 0)));
			droit0.calculateDateEtMotifDebut(p -> null);
			droitRFDAO.save(droit0);

			// on droit identique à celui qui arrive dans le fichier XML
			final DroitProprietePersonnePhysiqueRF droit1 = new DroitProprietePersonnePhysiqueRF();
			droit1.setMasterIdRF("8af806fa4a4dd302014b16fc17266a0b");
			droit1.setVersionIdRF("8af806fa4a4dd302014b16fc17256a06");
			droit1.setDateDebut(dateImportInitial);
			droit1.setAyantDroit(pp);
			droit1.setImmeuble(immeuble2);
			droit1.setPart(new Fraction(1, 2));
			droit1.setRegime(GenrePropriete.COPROPRIETE);
			droit1.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2003, 1, 1), "Succession", new IdentifiantAffaireRF(6, 2003, 9593, 0)));
			droit1.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2014, 12, 23), "Achat", new IdentifiantAffaireRF(6, 2014, 9593, 0)));
			droit1.calculateDateEtMotifDebut(p -> null);

			droitRFDAO.save(droit1);

			return pp.getId();
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_droit_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateSecondImport, TypeEntiteRF.DROIT, TypeMutationRF.MODIFICATION, idPPRF, null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée et :
		//  - le droit0 est fermé
		//  - le droit1 est inchangé
		//  - un nouveau droit est créé
		doInNewTransaction(status -> {

			final PersonnePhysiqueRF pp = (PersonnePhysiqueRF) ayantDroitRFDAO.get(ppId);
			assertNotNull(pp);

			final Set<DroitProprieteRF> droits = pp.getDroitsPropriete();
			assertNotNull(droits);
			assertEquals(3, droits.size());

			final List<DroitRF> droitList = new ArrayList<>(droits);
			droitList.sort(Comparator.comparing(DroitRF::getMasterIdRF));

			// le droit0 doit être fermé
			final DroitProprietePersonnePhysiqueRF droit0 = (DroitProprietePersonnePhysiqueRF) droitList.get(0);
			assertNotNull(droit0);
			assertEquals("1f109152381009be0138100c87000000", droit0.getMasterIdRF());
			assertEquals("1f109152381009be0138100c87000001", droit0.getVersionIdRF());
			assertEquals(dateImportInitial, droit0.getDateDebut());
			assertEquals(dateSecondImport.getOneDayBefore(), droit0.getDateFin());
			assertEquals("Appropriation illégitime", droit0.getMotifDebut());
			assertEquals(RegDate.get(2007, 2, 7), droit0.getDateDebutMetier());
			assertEquals("_1f109152381009be0138100ba7e31031", droit0.getImmeuble().getIdRF());
			assertEquals(new Fraction(1, 1), droit0.getPart());
			assertEquals(GenrePropriete.INDIVIDUELLE, droit0.getRegime());

			final Set<RaisonAcquisitionRF> raisons0 = droit0.getRaisonsAcquisition();
			assertEquals(1, raisons0.size());
			assertRaisonAcquisition(RegDate.get(2007, 2, 7), "Appropriation illégitime", new IdentifiantAffaireRF(13, 2007, 173, 0), raisons0.iterator().next());

			// un nouveau droit doit remplacer le droit1
			final DroitProprietePersonnePhysiqueRF droit1 = (DroitProprietePersonnePhysiqueRF) droitList.get(1);
			assertNotNull(droit1);
			assertEquals("1f109152381009be0138100c87276e68", droit1.getMasterIdRF());
			assertEquals("1f109152381009be0138100e4c7c00e5", droit1.getVersionIdRF());
			assertEquals(dateSecondImport, droit1.getDateDebut());
			assertNull(droit1.getDateFin());
			assertEquals("Achat", droit1.getMotifDebut());
			assertEquals(RegDate.get(2005, 1, 1), droit1.getDateDebutMetier());
			assertEquals("_1f109152381009be0138100ba7e31031", droit1.getImmeuble().getIdRF());
			assertEquals(new Fraction(1, 1), droit1.getPart());
			assertEquals(GenrePropriete.INDIVIDUELLE, droit1.getRegime());

			final List<RaisonAcquisitionRF> raisons1 = new ArrayList<>(droit1.getRaisonsAcquisition());
			raisons1.sort(Comparator.naturalOrder());
			assertEquals(2, raisons1.size());
			assertRaisonAcquisition(RegDate.get(2005, 1, 1), "Achat", new IdentifiantAffaireRF(13, 2005, 173, 0), raisons1.get(0));
			assertRaisonAcquisition(RegDate.get(2007, 2, 7), "Donation", new IdentifiantAffaireRF(13, 2007, 173, 0), raisons1.get(1));

			// le dernier droit reste inchangé
			final DroitProprietePersonnePhysiqueRF droit2 = (DroitProprietePersonnePhysiqueRF) droitList.get(2);
			assertNotNull(droit2);
			assertEquals("8af806fa4a4dd302014b16fc17266a0b", droit2.getMasterIdRF());
			assertEquals("8af806fa4a4dd302014b16fc17256a06", droit2.getVersionIdRF());
			assertEquals(dateImportInitial, droit2.getDateDebut());
			assertNull(droit2.getDateFin());
			assertEquals("Succession", droit2.getMotifDebut());
			assertEquals(RegDate.get(2003, 1, 1), droit2.getDateDebutMetier());
			assertEquals("_8af806fc4a35927c014ae2a6e76041b8", droit2.getImmeuble().getIdRF());
			assertEquals(new Fraction(1, 2), droit2.getPart());
			assertEquals(GenrePropriete.COPROPRIETE, droit2.getRegime());

			final List<RaisonAcquisitionRF> raisons2 = new ArrayList<>(droit2.getRaisonsAcquisition());
			raisons2.sort(Comparator.naturalOrder());
			assertEquals(2, raisons2.size());
			assertRaisonAcquisition(RegDate.get(2003, 1, 1), "Succession", new IdentifiantAffaireRF(6, 2003, 9593, 0), raisons2.get(0));
			assertRaisonAcquisition(RegDate.get(2014, 12, 23), "Achat", new IdentifiantAffaireRF(6, 2014, 9593, 0), raisons2.get(1));
			return null;
		});

		// postcondition : les événements fiscaux correspondants ont été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());

			final EvenementFiscalDroitPropriete event0 = (EvenementFiscalDroitPropriete) events.get(0);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.OUVERTURE, event0.getType());
			assertEquals(RegDate.get(2005, 1, 1), event0.getDateValeur());
			assertEquals("_1f109152381009be0138100ba7e31031", event0.getDroit().getImmeuble().getIdRF());
			assertEquals(ppId, event0.getDroit().getAyantDroit().getId());

			// note : l'événement fiscal de fermeture du droit est envoyé par le DateFinDroitsRFProcessor

			return null;
		});
	}

	/**
	 * [SIFISC-23985] Ce test vérifie que le processing d'une mutation de modification <i>complète</i> de droits entre immeuble fonctionne bien.
	 */
	@Test
	public void testProcessMutationModificationCompleteDroitEntreImmeubles() throws Exception {

		final String idImmeubleRF1 = "_8af806fc4a35927c014ae2a6e76041b8";
		final String idImmeubleRF2 = "_1f109152381009be0138100ba7e31031";

		final RegDate dateImportInitial = RegDate.get(2015, 3, 17);
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		// précondition : il y a déjà un droit entre immeubles dans la base de données
		final Long immId = doInNewTransaction(status -> {

			BienFondsRF immeuble1 = new BienFondsRF();
			immeuble1.setIdRF(idImmeubleRF1);
			immeuble1 = (BienFondsRF) immeubleRFDAO.save(immeuble1);

			BienFondsRF immeuble2 = new BienFondsRF();
			immeuble2.setIdRF(idImmeubleRF2);
			immeuble2 = (BienFondsRF) immeubleRFDAO.save(immeuble2);

			ImmeubleBeneficiaireRF imm = new ImmeubleBeneficiaireRF();
			imm.setIdRF(idImmeubleRF1);
			imm.setImmeuble(immeuble1);
			imm = (ImmeubleBeneficiaireRF) ayantDroitRFDAO.save(imm);

			// on droit différent de celui qui arrive dans le fichier XML
			final DroitProprieteImmeubleRF droit0 = new DroitProprieteImmeubleRF();
			// master id RF différent
			droit0.setMasterIdRF("1f109152381009be0138100c87000000");
			droit0.setVersionIdRF("1f109152381009be0138100c87000001");
			droit0.setDateDebut(dateImportInitial);
			droit0.setAyantDroit(imm);
			droit0.setImmeuble(immeuble2);
			droit0.setPart(new Fraction(1, 2));
			droit0.setRegime(GenrePropriete.FONDS_DOMINANT);
			droit0.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2007, 1, 2), "Appropriation illégitime", new IdentifiantAffaireRF(13, 2007, 173, 0)));
			droit0.calculateDateEtMotifDebut(p -> null);
			droitRFDAO.save(droit0);

			return imm.getId();
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_droit_immeuble_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateSecondImport, TypeEntiteRF.DROIT, TypeMutationRF.MODIFICATION, idImmeubleRF1, null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée et :
		//  - le droit0 est fermé
		//  - un nouveau droit est créé
		doInNewTransaction(status -> {

			final ImmeubleBeneficiaireRF imm = (ImmeubleBeneficiaireRF) ayantDroitRFDAO.get(immId);
			assertNotNull(imm);

			final Set<DroitProprieteRF> droits = imm.getDroitsPropriete();
			assertNotNull(droits);
			assertEquals(2, droits.size());

			final List<DroitRF> droitList = new ArrayList<>(droits);
			droitList.sort(Comparator.comparing(DroitRF::getMasterIdRF));

			// le droit0 doit être fermé
			final DroitProprieteImmeubleRF droit0 = (DroitProprieteImmeubleRF) droitList.get(0);
			assertNotNull(droit0);
			assertEquals("1f109152381009be0138100c87000000", droit0.getMasterIdRF());
			assertEquals("1f109152381009be0138100c87000001", droit0.getVersionIdRF());
			assertEquals(dateImportInitial, droit0.getDateDebut());
			assertEquals(dateSecondImport.getOneDayBefore(), droit0.getDateFin());
			assertEquals("Appropriation illégitime", droit0.getMotifDebut());
			assertEquals(RegDate.get(2007, 1, 2), droit0.getDateDebutMetier());
			assertEquals("_1f109152381009be0138100ba7e31031", droit0.getImmeuble().getIdRF());
			assertEquals(new Fraction(1, 2), droit0.getPart());
			assertEquals(GenrePropriete.FONDS_DOMINANT, droit0.getRegime());

			final Set<RaisonAcquisitionRF> raisons0 = droit0.getRaisonsAcquisition();
			assertEquals(1, raisons0.size());
			assertRaisonAcquisition(RegDate.get(2007, 1, 2), "Appropriation illégitime", new IdentifiantAffaireRF(13, 2007, 173, 0), raisons0.iterator().next());

			// un nouveau droit doit remplacer le droit1
			final DroitProprieteImmeubleRF droit1 = (DroitProprieteImmeubleRF) droitList.get(1);
			assertNotNull(droit1);
			assertEquals("3838292", droit1.getMasterIdRF());
			assertEquals("3838291", droit1.getVersionIdRF());
			assertEquals(dateSecondImport, droit1.getDateDebut());
			assertNull(droit1.getDateFin());
			assertEquals("Constitution de PPE", droit1.getMotifDebut());
			assertEquals(RegDate.get(2010, 4, 11), droit1.getDateDebutMetier());
			assertEquals("_1f109152381009be0138100ba7e31031", droit1.getImmeuble().getIdRF());
			assertEquals(new Fraction(1, 1), droit1.getPart());
			assertEquals(GenrePropriete.FONDS_DOMINANT, droit1.getRegime());

			final List<RaisonAcquisitionRF> raisons1 = new ArrayList<>(droit1.getRaisonsAcquisition());
			raisons1.sort(Comparator.naturalOrder());
			assertEquals(1, raisons1.size());
			assertRaisonAcquisition(RegDate.get(2010, 4, 11), "Constitution de PPE", new IdentifiantAffaireRF(6, 2013, 17, 0), raisons1.get(0));
			return null;
		});

		// postcondition : les événements fiscaux correspondants ont été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());
			events.sort(Comparator.comparing(EvenementFiscal::getDateValeur));

			final EvenementFiscalDroitPropriete event0 = (EvenementFiscalDroitPropriete) events.get(0);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.OUVERTURE, event0.getType());
			assertEquals(RegDate.get(2010, 4, 11), event0.getDateValeur());
			assertEquals("_1f109152381009be0138100ba7e31031", event0.getDroit().getImmeuble().getIdRF());
			assertEquals(immId, event0.getDroit().getAyantDroit().getId());

			// note : l'événement fiscal de fermeture du droit est envoyé par le DateFinDroitsRFProcessor

			return null;
		});
	}

	/**
	 * [SIFISC-24423] Ce test vérifie que la processing d'une mutation de modification <i>partielle et substentielle</i>
	 * (par exemple, un changement de part de co-propriété) de droits entre immeuble ferme bien l'ancien droit et en crée un nouveau.
	 */
	@Test
	public void testProcessMutationModificationPartielleDroitEntreImmeubles() throws Exception {

		final String idImmeubleRF1 = "_8af806fc4a35927c014ae2a6e76041b8";
		final String idImmeubleRF2 = "_1f109152381009be0138100ba7e31031";

		final RegDate dateImportInitial = RegDate.get(2015, 3, 17);
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		// précondition : il y a déjà un droit entre immeubles dans la base de données
		final Long immId = doInNewTransaction(status -> {

			BienFondsRF immeuble1 = new BienFondsRF();
			immeuble1.setIdRF(idImmeubleRF1);
			immeuble1 = (BienFondsRF) immeubleRFDAO.save(immeuble1);

			BienFondsRF immeuble2 = new BienFondsRF();
			immeuble2.setIdRF(idImmeubleRF2);
			immeuble2 = (BienFondsRF) immeubleRFDAO.save(immeuble2);

			ImmeubleBeneficiaireRF imm = new ImmeubleBeneficiaireRF();
			imm.setIdRF(idImmeubleRF1);
			imm.setImmeuble(immeuble1);
			imm = (ImmeubleBeneficiaireRF) ayantDroitRFDAO.save(imm);

			// on droit différent de celui qui arrive dans le fichier XML
			final DroitProprieteImmeubleRF droit0 = new DroitProprieteImmeubleRF();
			// master id RF identique
			droit0.setMasterIdRF("3838292");
			// version id RF différent
			droit0.setVersionIdRF("3838290");
			droit0.setDateDebut(dateImportInitial);
			droit0.setAyantDroit(imm);
			droit0.setImmeuble(immeuble2);
			// part différente
			droit0.setPart(new Fraction(1, 3));
			droit0.setRegime(GenrePropriete.FONDS_DOMINANT);
			droit0.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2007, 1, 2), "Appropriation illégitime", new IdentifiantAffaireRF(13, 2007, 173, 0)));
			droit0.calculateDateEtMotifDebut(p -> null);
			droitRFDAO.save(droit0);

			return imm.getId();
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_droit_immeuble_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateSecondImport, TypeEntiteRF.DROIT, TypeMutationRF.MODIFICATION, idImmeubleRF1, null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée et :
		//  - le droit0 est fermé
		//  - un nouveau droit est créé
		doInNewTransaction(status -> {

			final ImmeubleBeneficiaireRF imm = (ImmeubleBeneficiaireRF) ayantDroitRFDAO.get(immId);
			assertNotNull(imm);

			final Set<DroitProprieteRF> droits = imm.getDroitsPropriete();
			assertNotNull(droits);
			assertEquals(2, droits.size());

			final List<DroitRF> droitList = new ArrayList<>(droits);
			droitList.sort(new DroitRFRangeMetierComparator());

			// le droit0 doit être fermé
			final DroitProprieteImmeubleRF droit0 = (DroitProprieteImmeubleRF) droitList.get(0);
			assertNotNull(droit0);
			assertEquals("3838292", droit0.getMasterIdRF());
			assertEquals("3838290", droit0.getVersionIdRF());
			assertEquals(dateImportInitial, droit0.getDateDebut());
			assertEquals(dateSecondImport.getOneDayBefore(), droit0.getDateFin());
			assertEquals("Appropriation illégitime", droit0.getMotifDebut());
			assertEquals(RegDate.get(2007, 1, 2), droit0.getDateDebutMetier());
			assertEquals("_1f109152381009be0138100ba7e31031", droit0.getImmeuble().getIdRF());
			assertEquals(new Fraction(1, 3), droit0.getPart());
			assertEquals(GenrePropriete.FONDS_DOMINANT, droit0.getRegime());

			final Set<RaisonAcquisitionRF> raisons0 = droit0.getRaisonsAcquisition();
			assertEquals(1, raisons0.size());
			assertRaisonAcquisition(RegDate.get(2007, 1, 2), "Appropriation illégitime", new IdentifiantAffaireRF(13, 2007, 173, 0), raisons0.iterator().next());

			// un nouveau droit doit remplacer le droit1
			final DroitProprieteImmeubleRF droit1 = (DroitProprieteImmeubleRF) droitList.get(1);
			assertNotNull(droit1);
			assertEquals("3838292", droit1.getMasterIdRF());
			assertEquals("3838291", droit1.getVersionIdRF());
			assertEquals(dateSecondImport, droit1.getDateDebut());
			assertNull(droit1.getDateFin());
			assertEquals("Constitution de PPE", droit1.getMotifDebut());
			assertEquals(RegDate.get(2010, 4, 11), droit1.getDateDebutMetier());
			assertEquals("_1f109152381009be0138100ba7e31031", droit1.getImmeuble().getIdRF());
			assertEquals(new Fraction(1, 1), droit1.getPart());
			assertEquals(GenrePropriete.FONDS_DOMINANT, droit1.getRegime());

			final List<RaisonAcquisitionRF> raisons1 = new ArrayList<>(droit1.getRaisonsAcquisition());
			raisons1.sort(Comparator.naturalOrder());
			assertEquals(1, raisons1.size());
			assertRaisonAcquisition(RegDate.get(2010, 4, 11), "Constitution de PPE", new IdentifiantAffaireRF(6, 2013, 17, 0), raisons1.get(0));
			return null;
		});

		// postcondition : les événements fiscaux correspondants ont été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());

			final EvenementFiscalDroitPropriete event0 = (EvenementFiscalDroitPropriete) events.get(0);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.OUVERTURE, event0.getType());
			assertEquals(RegDate.get(2010, 4, 11), event0.getDateValeur());
			assertEquals("_1f109152381009be0138100ba7e31031", event0.getDroit().getImmeuble().getIdRF());
			assertEquals(immId, event0.getDroit().getAyantDroit().getId());

			// note : l'événement fiscal de fermeture du droit est envoyé par le DateFinDroitsRFProcessor

			return null;
		});
	}

	/**
	 * [SIFISC-24987] Ce test vérifie que la processing d'une mutation de modification <i>partielle et substentielle</i>
	 * (par exemple, un changement de part de co-propriété) calcule bien une date de début du nouveau droit à partir de
	 * la nouvelle raison d'acquisition.
	 */
	@Test
	public void testProcessMutationModificationPartielleDroitAvecNouvelleRaisonAcquisition() throws Exception {

		final String idImmeubleRF1 = "_8af806fc4a35927c014ae2a6e76041b8";
		final String idImmeubleRF2 = "_1f109152381009be0138100ba7e31031";

		final RegDate dateImportInitial = RegDate.get(2015, 3, 17);
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		// précondition : il y a déjà un droit entre immeubles dans la base de données
		final Long immId = doInNewTransaction(status -> {

			BienFondsRF immeuble1 = new BienFondsRF();
			immeuble1.setIdRF(idImmeubleRF1);
			immeuble1 = (BienFondsRF) immeubleRFDAO.save(immeuble1);

			BienFondsRF immeuble2 = new BienFondsRF();
			immeuble2.setIdRF(idImmeubleRF2);
			immeuble2 = (BienFondsRF) immeubleRFDAO.save(immeuble2);

			ImmeubleBeneficiaireRF imm = new ImmeubleBeneficiaireRF();
			imm.setIdRF(idImmeubleRF1);
			imm.setImmeuble(immeuble1);
			imm = (ImmeubleBeneficiaireRF) ayantDroitRFDAO.save(imm);

			// on droit différent de celui qui arrive dans le fichier XML
			final DroitProprieteImmeubleRF droit0 = new DroitProprieteImmeubleRF();
			// master id RF identique
			droit0.setMasterIdRF("3838292");
			// version id RF différent
			droit0.setVersionIdRF("3838290");
			droit0.setDateDebut(dateImportInitial);
			droit0.setAyantDroit(imm);
			droit0.setImmeuble(immeuble2);
			// part différente
			droit0.setPart(new Fraction(1, 3));
			droit0.setRegime(GenrePropriete.FONDS_DOMINANT);
			// la même première raison d'acquisition
			droit0.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2010, 4, 11), "Constitution de PPE", new IdentifiantAffaireRF(6, 2013, 17, 0)));
			droit0.calculateDateEtMotifDebut(p -> null);
			droitRFDAO.save(droit0);

			return imm.getId();
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_droit_nouvelle_raison_acquisition_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateSecondImport, TypeEntiteRF.DROIT, TypeMutationRF.MODIFICATION, idImmeubleRF1, null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée et :
		//  - le droit0 est fermé
		//  - un nouveau droit est créé avec une date de début métier = 20150101
		doInNewTransaction(status -> {

			final ImmeubleBeneficiaireRF imm = (ImmeubleBeneficiaireRF) ayantDroitRFDAO.get(immId);
			assertNotNull(imm);

			final Set<DroitProprieteRF> droits = imm.getDroitsPropriete();
			assertNotNull(droits);
			assertEquals(2, droits.size());

			final List<DroitRF> droitList = new ArrayList<>(droits);
			droitList.sort(new DroitRFRangeMetierComparator());

			// le droit0 doit être fermé
			final DroitProprieteImmeubleRF droit0 = (DroitProprieteImmeubleRF) droitList.get(0);
			assertNotNull(droit0);
			assertEquals("3838292", droit0.getMasterIdRF());
			assertEquals("3838290", droit0.getVersionIdRF());
			assertEquals(dateImportInitial, droit0.getDateDebut());
			assertEquals(dateSecondImport.getOneDayBefore(), droit0.getDateFin());
			assertEquals("Constitution de PPE", droit0.getMotifDebut());
			assertEquals(RegDate.get(2010, 4, 11), droit0.getDateDebutMetier());
			assertEquals("_1f109152381009be0138100ba7e31031", droit0.getImmeuble().getIdRF());
			assertEquals(new Fraction(1, 3), droit0.getPart());
			assertEquals(GenrePropriete.FONDS_DOMINANT, droit0.getRegime());

			final Set<RaisonAcquisitionRF> raisons0 = droit0.getRaisonsAcquisition();
			assertEquals(1, raisons0.size());
			assertRaisonAcquisition(RegDate.get(2010, 4, 11), "Constitution de PPE", new IdentifiantAffaireRF(6, 2013, 17, 0), raisons0.iterator().next());

			// un nouveau droit doit remplacer le droit1 et la date de début métier doit correspondre avec la nouvelle raison d'acquisition
			final DroitProprieteImmeubleRF droit1 = (DroitProprieteImmeubleRF) droitList.get(1);
			assertNotNull(droit1);
			assertEquals("3838292", droit1.getMasterIdRF());
			assertEquals("3838291", droit1.getVersionIdRF());
			assertEquals(dateSecondImport, droit1.getDateDebut());
			assertNull(droit1.getDateFin());
			assertEquals("Remaniement de PPE", droit1.getMotifDebut());
			assertEquals(RegDate.get(2015, 1, 1), droit1.getDateDebutMetier());  // <--- correspond à la nouvelle raison d'acquisition
			assertEquals("_1f109152381009be0138100ba7e31031", droit1.getImmeuble().getIdRF());
			assertEquals(new Fraction(1, 2), droit1.getPart());
			assertEquals(GenrePropriete.FONDS_DOMINANT, droit1.getRegime());

			final List<RaisonAcquisitionRF> raisons1 = new ArrayList<>(droit1.getRaisonsAcquisition());
			raisons1.sort(Comparator.naturalOrder());
			assertEquals(2, raisons1.size());
			assertRaisonAcquisition(RegDate.get(2010, 4, 11), "Constitution de PPE", new IdentifiantAffaireRF(6, 2013, 17, 0), raisons1.get(0));
			assertRaisonAcquisition(RegDate.get(2015, 1, 1), "Remaniement de PPE", new IdentifiantAffaireRF(6, 2015, 1, 0), raisons1.get(1));
			return null;
		});

		// postcondition : les événements fiscaux correspondants ont été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());

			final EvenementFiscalDroitPropriete event0 = (EvenementFiscalDroitPropriete) events.get(0);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.OUVERTURE, event0.getType());
			assertEquals(RegDate.get(2015, 1, 1), event0.getDateValeur());
			assertEquals("_1f109152381009be0138100ba7e31031", event0.getDroit().getImmeuble().getIdRF());
			assertEquals(immId, event0.getDroit().getAyantDroit().getId());

			// note : l'événement fiscal de fermeture du droit est envoyé par le DateFinDroitsRFProcessor

			return null;
		});
	}

	/*
	 * Ce test vérifie que le processing d'une mutation de modification de droits fonctionne bien dans le cas où la seule différence est une nouvelle raison d'acquisition.
	 */
	@Test
	public void testProcessMutationModificationNouvelleRaisonAcquisition() throws Exception {

		final String idPPRF = "_1f109152381009be0138100a1d442eee";
		final String idImmeubleRF1 = "_1f109152381009be0138100ba7e31031";
		final String idImmeubleRF2 = "_8af806fc4a35927c014ae2a6e76041b8";
		final RegDate dateImportInitial = RegDate.get(2015, 3, 17);
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		// précondition : il y a déjà deux droits dans la base de données
		final Long ppId = doInNewTransaction(status -> {

			PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
			pp.setIdRF(idPPRF);
			pp.setNom("Schulz");
			pp.setPrenom("Alodie");
			pp.setDateNaissance(RegDate.get(1900, 1, 1));
			pp = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp);

			BienFondsRF immeuble1 = new BienFondsRF();
			immeuble1.setIdRF(idImmeubleRF1);
			immeuble1 = (BienFondsRF) immeubleRFDAO.save(immeuble1);

			BienFondsRF immeuble2 = new BienFondsRF();
			immeuble2.setIdRF(idImmeubleRF2);
			immeuble2 = (BienFondsRF) immeubleRFDAO.save(immeuble2);

			// on droit différent de celui qui arrive dans le fichier XML
			final DroitProprietePersonnePhysiqueRF droit0 = new DroitProprietePersonnePhysiqueRF();
			droit0.setMasterIdRF("1f109152381009be0138100c87276e68");
			droit0.setVersionIdRF("1f109152381009be0138100e4c7c00e5");
			droit0.setDateDebut(dateImportInitial);
			droit0.setAyantDroit(pp);
			droit0.setImmeuble(immeuble1);
			droit0.setPart(new Fraction(1, 1));
			droit0.setRegime(GenrePropriete.INDIVIDUELLE);
			// une seule raison d'acquisition (il y en a deux dans le fichier)
			droit0.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2005, 1, 1), "Achat", new IdentifiantAffaireRF(13, 2005, 173, 0)));
			droit0.calculateDateEtMotifDebut(p -> null);
			droitRFDAO.save(droit0);

			// on droit identique à celui qui arrive dans le fichier XML
			final DroitProprietePersonnePhysiqueRF droit1 = new DroitProprietePersonnePhysiqueRF();
			droit1.setMasterIdRF("8af806fa4a4dd302014b16fc17266a0b");
			droit1.setVersionIdRF("8af806fa4a4dd302014b16fc17256a06");
			droit1.setDateDebut(dateImportInitial);
			droit1.setAyantDroit(pp);
			droit1.setImmeuble(immeuble2);
			droit1.setPart(new Fraction(1, 2));
			droit1.setRegime(GenrePropriete.COPROPRIETE);
			droit1.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2003, 1, 1), "Succession", new IdentifiantAffaireRF(6, 2003, 9593, 0)));
			droit1.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2014, 12, 23), "Achat", new IdentifiantAffaireRF(6, 2014, 9593, 0)));
			droit1.calculateDateEtMotifDebut(p -> null);

			droitRFDAO.save(droit1);

			return pp.getId();
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_droit_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateSecondImport, TypeEntiteRF.DROIT, TypeMutationRF.MODIFICATION, idPPRF, null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée et :
		//  - le droit0 est modifié
		//  - le droit1 est inchangé
		doInNewTransaction(status -> {

			final PersonnePhysiqueRF pp = (PersonnePhysiqueRF) ayantDroitRFDAO.get(ppId);
			assertNotNull(pp);

			final Set<DroitProprieteRF> droits = pp.getDroitsPropriete();
			assertNotNull(droits);
			assertEquals(2, droits.size());

			final List<DroitRF> droitList = new ArrayList<>(droits);
			droitList.sort(Comparator.comparing(DroitRF::getMasterIdRF));

			// le droit0 doit être à jour
			final DroitProprietePersonnePhysiqueRF droit0 = (DroitProprietePersonnePhysiqueRF) droitList.get(0);
			assertNotNull(droit0);
			assertEquals("1f109152381009be0138100c87276e68", droit0.getMasterIdRF());
			assertEquals("1f109152381009be0138100e4c7c00e5", droit0.getVersionIdRF());
			assertEquals(dateImportInitial, droit0.getDateDebut());
			assertNull(droit0.getDateFin());
			assertEquals("Achat", droit0.getMotifDebut());
			assertEquals(RegDate.get(2005, 1, 1), droit0.getDateDebutMetier());
			assertEquals("_1f109152381009be0138100ba7e31031", droit0.getImmeuble().getIdRF());
			assertEquals(new Fraction(1, 1), droit0.getPart());
			assertEquals(GenrePropriete.INDIVIDUELLE, droit0.getRegime());

			final List<RaisonAcquisitionRF> raisons0 = new ArrayList<>(droit0.getRaisonsAcquisition());
			raisons0.sort(Comparator.naturalOrder());
			assertEquals(2, raisons0.size());
			assertRaisonAcquisition(RegDate.get(2005, 1, 1), "Achat", new IdentifiantAffaireRF(13, 2005, 173, 0), raisons0.get(0));
			assertRaisonAcquisition(RegDate.get(2007, 2, 7), "Donation", new IdentifiantAffaireRF(13, 2007, 173, 0), raisons0.get(1));

			// le dernier droit reste inchangé
			final DroitProprietePersonnePhysiqueRF droit1 = (DroitProprietePersonnePhysiqueRF) droitList.get(1);
			assertNotNull(droit1);
			assertEquals("8af806fa4a4dd302014b16fc17266a0b", droit1.getMasterIdRF());
			assertEquals("8af806fa4a4dd302014b16fc17256a06", droit1.getVersionIdRF());
			assertEquals(dateImportInitial, droit1.getDateDebut());
			assertNull(droit1.getDateFin());
			assertEquals("Succession", droit1.getMotifDebut());
			assertEquals(RegDate.get(2003, 1, 1), droit1.getDateDebutMetier());
			assertEquals("_8af806fc4a35927c014ae2a6e76041b8", droit1.getImmeuble().getIdRF());
			assertEquals(new Fraction(1, 2), droit1.getPart());
			assertEquals(GenrePropriete.COPROPRIETE, droit1.getRegime());

			final List<RaisonAcquisitionRF> raisons1 = new ArrayList<>(droit1.getRaisonsAcquisition());
			raisons1.sort(Comparator.naturalOrder());
			assertEquals(2, raisons1.size());
			assertRaisonAcquisition(RegDate.get(2003, 1, 1), "Succession", new IdentifiantAffaireRF(6, 2003, 9593, 0), raisons1.get(0));
			assertRaisonAcquisition(RegDate.get(2014, 12, 23), "Achat", new IdentifiantAffaireRF(6, 2014, 9593, 0), raisons1.get(1));
			return null;
		});

		// postcondition : les événements fiscaux correspondants ont été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());

			final EvenementFiscalDroitPropriete event0 = (EvenementFiscalDroitPropriete) events.get(0);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.MODIFICATION, event0.getType());
			assertEquals(RegDate.get(2005, 1, 1), event0.getDateValeur());
			assertEquals("_1f109152381009be0138100ba7e31031", event0.getDroit().getImmeuble().getIdRF());
			assertEquals(ppId, event0.getDroit().getAyantDroit().getId());

			return null;
		});
	}

	/*
	 * [SIFISC-24887] Ce test vérifie que le processing d'une mutation fonctionne bien dans le cas où le nouvel immeuble ne possède pas de raisons d'acquisition.
	 */
	@Test
	public void testProcessMutationModificationSansRaisonAcquisition() throws Exception {

		final String idPPRF = "_1f109152381009be0138100a1d442eee";
		final String idImmeubleRF1 = "_1f109152381009be0138100ba7e31031";
		final RegDate dateImportInitial = RegDate.get(2015, 3, 17);
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		// précondition : il y a déjà un droit dans la base de données
		final Long ppId = doInNewTransaction(status -> {

			PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
			pp.setIdRF(idPPRF);
			pp.setNom("Schulz");
			pp.setPrenom("Alodie");
			pp.setDateNaissance(RegDate.get(1900, 1, 1));
			pp = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp);

			BienFondsRF immeuble1 = new BienFondsRF();
			immeuble1.setIdRF(idImmeubleRF1);
			immeuble1 = (BienFondsRF) immeubleRFDAO.save(immeuble1);

			// on droit différent de celui qui arrive dans le fichier XML
			final DroitProprietePersonnePhysiqueRF droit0 = new DroitProprietePersonnePhysiqueRF();
			droit0.setMasterIdRF("1f109152381009be0138100c87276e68");
			droit0.setVersionIdRF("1f109152381009be0138100e4c7c00e5");
			droit0.setDateDebut(dateImportInitial);
			droit0.setAyantDroit(pp);
			droit0.setImmeuble(immeuble1);
			droit0.setPart(new Fraction(1, 1));
			droit0.setRegime(GenrePropriete.INDIVIDUELLE);
			// une seule raison d'acquisition (il n'y en a pas dans le fichier)
			droit0.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2005, 1, 1), "Achat", new IdentifiantAffaireRF(13, 2005, 173, 0)));
			droit0.calculateDateEtMotifDebut(p -> null);
			droitRFDAO.save(droit0);

			return pp.getId();
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_droit_sans_raison_acquistion_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateSecondImport, TypeEntiteRF.DROIT, TypeMutationRF.MODIFICATION, idPPRF, null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée et :
		//  - le droit0 est modifié
		doInNewTransaction(status -> {

			final PersonnePhysiqueRF pp = (PersonnePhysiqueRF) ayantDroitRFDAO.get(ppId);
			assertNotNull(pp);

			final Set<DroitProprieteRF> droits = pp.getDroitsPropriete();
			assertNotNull(droits);
			assertEquals(1, droits.size());

			// le droit0 doit être à jour
			final DroitProprietePersonnePhysiqueRF droit0 = (DroitProprietePersonnePhysiqueRF) droits.iterator().next();
			assertNotNull(droit0);
			assertEquals("1f109152381009be0138100c87276e68", droit0.getMasterIdRF());
			assertEquals("1f109152381009be0138100e4c7c00e5", droit0.getVersionIdRF());
			assertEquals(dateImportInitial, droit0.getDateDebut());
			assertNull(droit0.getDateFin());
			assertEquals("Achat", droit0.getMotifDebut());
			assertEquals(RegDate.get(2005, 1, 1), droit0.getDateDebutMetier());
			assertEquals("_1f109152381009be0138100ba7e31031", droit0.getImmeuble().getIdRF());
			assertEquals(new Fraction(1, 1), droit0.getPart());
			assertEquals(GenrePropriete.INDIVIDUELLE, droit0.getRegime());

			// il n'y a plus de raisons d'acquisition
			final List<RaisonAcquisitionRF> raisons0 = new ArrayList<>(droit0.getRaisonsAcquisition());
			assertEquals(1, raisons0.size());
			assertNotNull(raisons0.get(0).getAnnulationDate());
			return null;
		});

		// postcondition : les événements fiscaux correspondants ont été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());

			final EvenementFiscalDroitPropriete event0 = (EvenementFiscalDroitPropriete) events.get(0);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.MODIFICATION, event0.getType());
			assertEquals(RegDate.get(2005, 1, 1), event0.getDateValeur());
			assertEquals("_1f109152381009be0138100ba7e31031", event0.getDroit().getImmeuble().getIdRF());
			assertEquals(ppId, event0.getDroit().getAyantDroit().getId());

			return null;
		});
	}

	/*
	 * Ce test vérifie que le processing d'une mutation de suppression fonctionne bien.
	 */
	@Test
	public void testProcessMutationSuppression() throws Exception {

		final String idPPRF = "_1f109152381009be0138100a1d442eee";
		final String idImmeubleRF1 = "_1f109152381009be0138100ba7e31031";
		final String idImmeubleRF2 = "_8af806fc4a35927c014ae2a6e76041b8";
		final RegDate dateImportInitial = RegDate.get(2015, 3, 17);
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		// précondition : une personne physique avec  deux droits dans la base de données
		final Long ppId = doInNewTransaction(status -> {

			PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
			pp.setIdRF(idPPRF);
			pp.setNom("Schulz");
			pp.setPrenom("Alodie");
			pp.setDateNaissance(RegDate.get(1900, 1, 1));
			pp = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp);

			BienFondsRF immeuble1 = new BienFondsRF();
			immeuble1.setIdRF(idImmeubleRF1);
			immeuble1 = (BienFondsRF) immeubleRFDAO.save(immeuble1);

			BienFondsRF immeuble2 = new BienFondsRF();
			immeuble2.setIdRF(idImmeubleRF2);
			immeuble2 = (BienFondsRF) immeubleRFDAO.save(immeuble2);

			final DroitProprietePersonnePhysiqueRF droit0 = new DroitProprietePersonnePhysiqueRF();
			droit0.setMasterIdRF("1f109152381009be0138100c87276e68");
			droit0.setVersionIdRF("1f109152381009be0138100c87276e67");
			droit0.setDateDebut(dateImportInitial);
			droit0.setAyantDroit(pp);
			droit0.setImmeuble(immeuble1);
			droit0.setPart(new Fraction(1, 1));
			droit0.setRegime(GenrePropriete.INDIVIDUELLE);
			droit0.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2007, 2, 7), "Donation", new IdentifiantAffaireRF(13, 2007, 173, 0)));
			droit0.calculateDateEtMotifDebut(p -> null);

			droitRFDAO.save(droit0);

			final DroitProprietePersonnePhysiqueRF droit1 = new DroitProprietePersonnePhysiqueRF();
			droit1.setMasterIdRF("8af806fa4a4dd302014b16fc17266a0b");
			droit1.setVersionIdRF("8af806fa4a4dd302014b16fc17266a0a");
			droit1.setDateDebut(dateImportInitial);
			droit1.setAyantDroit(pp);
			droit1.setImmeuble(immeuble2);
			droit1.setPart(new Fraction(1, 2));
			droit1.setRegime(GenrePropriete.COPROPRIETE);
			droit1.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2003, 1, 1), "Succession", new IdentifiantAffaireRF(6, 2003, 9593, 0)));
			droit1.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2014, 12, 23), "Achat", new IdentifiantAffaireRF(6, 2014, 9593, 0)));
			droit1.calculateDateEtMotifDebut(p -> null);
			droitRFDAO.save(droit1);

			return pp.getId();
		});

		// on envoie un fichier d'import qui ne contient aucun droit
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_droit_vide_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateSecondImport, TypeEntiteRF.DROIT, TypeMutationRF.SUPPRESSION, idPPRF, null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée et tous les droits existants sont fermés
		doInNewTransaction(status -> {

			final PersonnePhysiqueRF pp = (PersonnePhysiqueRF) ayantDroitRFDAO.get(ppId);
			assertNotNull(pp);

			final Set<DroitProprieteRF> droits = pp.getDroitsPropriete();
			assertNotNull(droits);
			assertEquals(2, droits.size());

			final List<DroitRF> droitList = new ArrayList<>(droits);
			droitList.sort(Comparator.comparing(DroitRF::getMasterIdRF));

			// le droit0 doit être fermé
			final DroitProprietePersonnePhysiqueRF droit0 = (DroitProprietePersonnePhysiqueRF) droitList.get(0);
			assertNotNull(droit0);
			assertEquals("1f109152381009be0138100c87276e68", droit0.getMasterIdRF());
			assertEquals("1f109152381009be0138100c87276e67", droit0.getVersionIdRF());
			assertEquals(dateImportInitial, droit0.getDateDebut());
			assertEquals(dateSecondImport.getOneDayBefore(), droit0.getDateFin());
			assertEquals("Donation", droit0.getMotifDebut());
			assertEquals(RegDate.get(2007, 2, 7), droit0.getDateDebutMetier());
			assertEquals("_1f109152381009be0138100ba7e31031", droit0.getImmeuble().getIdRF());
			assertEquals(new Fraction(1, 1), droit0.getPart());
			assertEquals(GenrePropriete.INDIVIDUELLE, droit0.getRegime());

			final Set<RaisonAcquisitionRF> raisons0 = droit0.getRaisonsAcquisition();
			assertEquals(1, raisons0.size());
			assertRaisonAcquisition(RegDate.get(2007, 2, 7), "Donation", new IdentifiantAffaireRF(13, 2007, 173, 0), raisons0.iterator().next());

			// le droit1 doit être fermé
			final DroitProprietePersonnePhysiqueRF droit1 = (DroitProprietePersonnePhysiqueRF) droitList.get(1);
			assertNotNull(droit1);
			assertEquals("8af806fa4a4dd302014b16fc17266a0b", droit1.getMasterIdRF());
			assertEquals("8af806fa4a4dd302014b16fc17266a0a", droit1.getVersionIdRF());
			assertEquals(dateImportInitial, droit1.getDateDebut());
			assertEquals(dateSecondImport.getOneDayBefore(), droit1.getDateFin());
			assertEquals("Succession", droit1.getMotifDebut());
			assertEquals(RegDate.get(2003, 1, 1), droit1.getDateDebutMetier());
			assertEquals("_8af806fc4a35927c014ae2a6e76041b8", droit1.getImmeuble().getIdRF());
			assertEquals(new Fraction(1, 2), droit1.getPart());
			assertEquals(GenrePropriete.COPROPRIETE, droit1.getRegime());

			final List<RaisonAcquisitionRF> raisons1 = new ArrayList<>(droit1.getRaisonsAcquisition());
			raisons1.sort(Comparator.naturalOrder());
			assertEquals(2, raisons1.size());
			assertRaisonAcquisition(RegDate.get(2003, 1, 1), "Succession", new IdentifiantAffaireRF(6, 2003, 9593, 0), raisons1.get(0));
			assertRaisonAcquisition(RegDate.get(2014, 12, 23), "Achat", new IdentifiantAffaireRF(6, 2014, 9593, 0), raisons1.get(1));
			return null;
		});

		// postcondition : les événements fiscaux correspondants ont été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(0, events.size());
			// note : l'événement fiscal de fermeture du droit est envoyé par le DateFinDroitsRFProcessor
			return null;
		});

	}

	/*
	 * Ce test vérifie que le processing d'une mutation de suppression de droit entre deux immeubles fonctionne bien.
	 */
	@Test
	public void testProcessMutationSuppressionDroitEntreImmeuble() throws Exception {

		final String idImmeubleRF1 = "_8af806fc4a35927c014ae2a6e76041b8";
		final String idImmeubleRF2 = "_1f109152381009be0138100ba7e31031";

		final RegDate dateImportInitial = RegDate.get(2015, 3, 17);
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		// précondition : il y a déjà un droit entre immeubles dans la base de données
		final Long immId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				BienFondsRF immeuble1 = new BienFondsRF();
				immeuble1.setIdRF(idImmeubleRF1);
				immeuble1 = (BienFondsRF) immeubleRFDAO.save(immeuble1);

				BienFondsRF immeuble2 = new BienFondsRF();
				immeuble2.setIdRF(idImmeubleRF2);
				immeuble2 = (BienFondsRF) immeubleRFDAO.save(immeuble2);

				ImmeubleBeneficiaireRF imm = new ImmeubleBeneficiaireRF();
				imm.setIdRF(idImmeubleRF1);
				imm.setImmeuble(immeuble1);
				imm = (ImmeubleBeneficiaireRF) ayantDroitRFDAO.save(imm);

				// on droit différent de celui qui arrive dans le fichier XML
				final DroitProprieteImmeubleRF droit0 = new DroitProprieteImmeubleRF();
				// master id RF différent
				droit0.setMasterIdRF("1f109152381009be0138100c87000000");
				droit0.setVersionIdRF("8af806fc40347c370141c079ff970020");
				droit0.setDateDebut(dateImportInitial);
				droit0.setAyantDroit(imm);
				droit0.setImmeuble(immeuble2);
				droit0.setPart(new Fraction(1, 2));
				droit0.setRegime(GenrePropriete.FONDS_DOMINANT);
				droit0.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2007, 1, 2), "Appropriation illégitime", new IdentifiantAffaireRF(13, 2007, 173, 0)));
				droit0.calculateDateEtMotifDebut(p -> null);
				droitRFDAO.save(droit0);

				return imm.getId();
			}
		});

		// on envoie un fichier d'import qui ne contient aucun droit
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_droit_vide_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateSecondImport, TypeEntiteRF.DROIT, TypeMutationRF.SUPPRESSION, idImmeubleRF1, null);

		// on process la mutation
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
				processor.process(mutation, false, null);
			}
		});

		// postcondition : la mutation est traitée et tous les droits existants sont fermés
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final ImmeubleBeneficiaireRF imm = (ImmeubleBeneficiaireRF) ayantDroitRFDAO.get(immId);
				assertNotNull(imm);

				final Set<DroitProprieteRF> droits = imm.getDroitsPropriete();
				assertNotNull(droits);
				assertEquals(1, droits.size());

				// le droit0 doit être fermé
				final DroitProprieteImmeubleRF droit0 = (DroitProprieteImmeubleRF) droits.iterator().next();
				assertNotNull(droit0);
				assertEquals("1f109152381009be0138100c87000000", droit0.getMasterIdRF());
				assertEquals("8af806fc40347c370141c079ff970020", droit0.getVersionIdRF());
				assertEquals(dateImportInitial, droit0.getDateDebut());
				assertEquals(dateSecondImport.getOneDayBefore(), droit0.getDateFin());
				assertEquals("Appropriation illégitime", droit0.getMotifDebut());
				assertEquals(RegDate.get(2007, 1, 2), droit0.getDateDebutMetier());
				assertEquals("_1f109152381009be0138100ba7e31031", droit0.getImmeuble().getIdRF());
				assertEquals(new Fraction(1, 2), droit0.getPart());
				assertEquals(GenrePropriete.FONDS_DOMINANT, droit0.getRegime());

				final Set<RaisonAcquisitionRF> raisons0 = droit0.getRaisonsAcquisition();
				assertEquals(1, raisons0.size());
				assertRaisonAcquisition(RegDate.get(2007, 1, 2), "Appropriation illégitime", new IdentifiantAffaireRF(13, 2007, 173, 0), raisons0.iterator().next());
			}
		});
	}
}