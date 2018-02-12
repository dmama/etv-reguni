package ch.vd.unireg.registrefoncier.dataimport.processor;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.evenement.fiscal.registrefoncier.EvenementFiscalDroit;
import ch.vd.unireg.evenement.fiscal.registrefoncier.EvenementFiscalDroitPropriete;
import ch.vd.unireg.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.unireg.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.unireg.evenement.registrefoncier.TypeMutationRF;
import ch.vd.unireg.registrefoncier.AyantDroitRF;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.DroitProprieteCommunauteRF;
import ch.vd.unireg.registrefoncier.DroitProprieteImmeubleRF;
import ch.vd.unireg.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.DroitProprieteRF;
import ch.vd.unireg.registrefoncier.DroitRF;
import ch.vd.unireg.registrefoncier.DroitRFRangeMetierComparator;
import ch.vd.unireg.registrefoncier.Fraction;
import ch.vd.unireg.registrefoncier.GenrePropriete;
import ch.vd.unireg.registrefoncier.IdentifiantAffaireRF;
import ch.vd.unireg.registrefoncier.ImmeubleBeneficiaireRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.ModeleCommunauteRF;
import ch.vd.unireg.registrefoncier.PersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.RaisonAcquisitionRF;
import ch.vd.unireg.registrefoncier.RegroupementCommunauteRF;
import ch.vd.unireg.registrefoncier.TypeCommunaute;
import ch.vd.unireg.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.unireg.registrefoncier.dao.DroitRFDAO;
import ch.vd.unireg.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.unireg.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.unireg.registrefoncier.processor.MutationRFProcessorTestCase;

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
		final CommunauteRFProcessor communauteRFProcessor = getBean(CommunauteRFProcessor.class, "communauteRFProcessor");

		this.processor = new DroitRFProcessor(ayantDroitRFDAO, immeubleRFDAO, droitRFDAO, communauteRFProcessor, xmlHelperRF, evenementFiscalService);
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

		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/processor/mutation_droit_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère quelques données satellites
		final Long ppId1 = insertPP("_1f109152381009be0138100a1d442eee", "Schulz", "Alodie", RegDate.get(1900, 1, 1));
		final Long ppId2 = insertPP("_1f1091523810039001381003da8b72ac", "Claude", "Daniel", RegDate.get(1900, 1, 1));
		final Long idImmeuble = insertImmeuble("_8af806fc4a35927c014ae2a6e76041b8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateImport, TypeEntiteRF.DROIT, TypeMutationRF.CREATION, "_8af806fc4a35927c014ae2a6e76041b8", null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, true, null);
			return null;
		});

		// postcondition : la mutation est traitée et les nouveaux droits sont créés
		doInNewTransaction(status -> {

			final ImmeubleRF immeuble = immeubleRFDAO.get(idImmeuble);
			assertNotNull(immeuble);

			final Set<DroitProprieteRF> droits = immeuble.getDroitsPropriete();
			assertNotNull(droits);
			assertEquals(2, droits.size());

			final List<DroitRF> droitList = new ArrayList<>(droits);
			droitList.sort(Comparator.comparing(DroitRF::getMasterIdRF));

			final DroitProprietePersonnePhysiqueRF droit0 = (DroitProprietePersonnePhysiqueRF) droitList.get(0);
			assertNotNull(droit0);
			assertEquals("1f1091523810039001381005be485efd", droit0.getMasterIdRF());
			assertEquals("1f10915238100390013810067ae35d4a", droit0.getVersionIdRF());
			assertNull(droit0.getDateDebut());
			assertNull(droit0.getDateFin());
			assertEquals("_1f1091523810039001381003da8b72ac", droit0.getAyantDroit().getIdRF());
			assertEquals(new Fraction(1, 2), droit0.getPart());
			assertEquals(GenrePropriete.COPROPRIETE, droit0.getRegime());

			final List<RaisonAcquisitionRF> raisons0 = new ArrayList<>(droit0.getRaisonsAcquisition());
			raisons0.sort(Comparator.naturalOrder());
			assertEquals(1, raisons0.size());
			assertRaisonAcquisition(RegDate.get(1997, 6, 19), "Achat", new IdentifiantAffaireRF(3,  "74'677"), raisons0.get(0));

			final DroitProprietePersonnePhysiqueRF droit1 = (DroitProprietePersonnePhysiqueRF) droitList.get(1);
			assertNotNull(droit1);
			assertEquals("8af806fa4a4dd302014b16fc17266a0b", droit1.getMasterIdRF());
			assertEquals("8af806fa4a4dd302014b16fc17256a06", droit1.getVersionIdRF());
			assertNull(droit1.getDateDebut());
			assertNull(droit1.getDateFin());
			assertEquals("Succession", droit1.getMotifDebut());
			assertEquals(RegDate.get(2003, 1, 1), droit1.getDateDebutMetier());
			assertEquals("_1f109152381009be0138100a1d442eee", droit1.getAyantDroit().getIdRF());
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
			assertEquals(RegDate.get(1997, 6, 19), event0.getDateValeur());
			assertEquals("_8af806fc4a35927c014ae2a6e76041b8", event0.getDroit().getImmeuble().getIdRF());
			assertEquals(ppId2, event0.getDroit().getAyantDroit().getId());

			final EvenementFiscalDroitPropriete event1 = (EvenementFiscalDroitPropriete) events.get(1);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.OUVERTURE, event1.getType());
			assertEquals(RegDate.get(2003, 1, 1), event1.getDateValeur());
			assertEquals("_8af806fc4a35927c014ae2a6e76041b8", event1.getDroit().getImmeuble().getIdRF());
			assertEquals(ppId1, event1.getDroit().getAyantDroit().getId());

			return null;
		});
	}

	/**
	 * [SIFISC-24595] Ce test vérifie que le processing d'une mutation de création sur une communauté crée bien la communauté et l'associe bien avec un modèle de communauté.
	 */
	@Test
	public void testProcessMutationCreationCommunaute() throws Exception {

		final RegDate dateImport = RegDate.get(2016, 10, 1);

		// précondition : la base est vide
		doInNewTransaction(status -> {
			assertEquals(0, droitRFDAO.getAll().size());
			assertEquals(0, evenementFiscalDAO.getAll().size());
			return null;
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/processor/mutation_droit_communaute_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère quelques données satellites
		final Long commId = insertCommunaute("72828ce8f830a");
		final Long ppId1 = insertPP("029191d4fec44", "Totor", "Jeanne", RegDate.get(1980, 1, 1));
		final Long ppId2 = insertPP("37838sc9d94de", "Totor", "Charlotte", RegDate.get(1980, 1, 1));
		final Long idImmeuble = insertImmeuble("_1f109152381009be0138100bc9f139e0");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateImport, TypeEntiteRF.DROIT, TypeMutationRF.CREATION, "_1f109152381009be0138100bc9f139e0", null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, true, null);
			return null;
		});

		// postcondition : la mutation est traitée et la communauté est bien regroupée avec un modèle
		doInNewTransaction(status -> {

			//
			// on vérifie la communaué
			//

			final CommunauteRF communaute = (CommunauteRF) ayantDroitRFDAO.get(commId);
			assertNotNull(communaute);

			// la communauté doit posséder un regroupement valide depuis l'ouverte des droits (date métier)
			final Set<RegroupementCommunauteRF> regroupements = communaute.getRegroupements();
			assertNotNull(regroupements);
			assertEquals(1, regroupements.size());

			final RegroupementCommunauteRF regroupement0 = regroupements.iterator().next();
			assertNotNull(regroupement0);
			assertEquals(RegDate.get(2010, 4, 23), regroupement0.getDateDebut());
			assertNull(regroupement0.getDateFin());

			// le modèle de communauté doit correspondre aux deux membres de la communauté
			final ModeleCommunauteRF modeleCommunaute = regroupement0.getModele();
			assertNotNull(modeleCommunaute);
			final List<AyantDroitRF> membres = new ArrayList<>(modeleCommunaute.getMembres());
			assertEquals(2, membres.size());
			membres.sort(Comparator.comparing(AyantDroitRF::getId));
			assertEquals(ppId1, membres.get(0).getId());
			assertEquals(ppId2, membres.get(1).getId());

			//
			// on vérifie aussi les droits par acquis de conscience
			//

			final ImmeubleRF immeuble = immeubleRFDAO.get(idImmeuble);
			assertNotNull(immeuble);

			final Set<DroitProprieteRF> droits = immeuble.getDroitsPropriete();
			assertNotNull(droits);
			assertEquals(3, droits.size());

			final List<DroitRF> droitList = new ArrayList<>(droits);
			droitList.sort(Comparator.comparing(DroitRF::getMasterIdRF));

			final DroitProprieteCommunauteRF droit0 = (DroitProprieteCommunauteRF) droitList.get(0);
			assertNotNull(droit0);
			assertEquals("38458fa0ac3", droit0.getMasterIdRF());
			assertEquals("38458fa0ac2", droit0.getVersionIdRF());
			assertNull(droit0.getDateDebut());
			assertNull(droit0.getDateFin());
			assertEquals(RegDate.get(2010, 4, 23), droit0.getDateDebutMetier());
			assertEquals("Succession", droit0.getMotifDebut());
			assertEquals("72828ce8f830a", droit0.getAyantDroit().getIdRF());
			assertEquals(new Fraction(1, 1), droit0.getPart());
			assertEquals(GenrePropriete.INDIVIDUELLE, droit0.getRegime());

			final List<RaisonAcquisitionRF> raisons0 = new ArrayList<>(droit0.getRaisonsAcquisition());
			assertEquals(1, raisons0.size());
			assertRaisonAcquisition(RegDate.get(2010, 4, 23), "Succession", new IdentifiantAffaireRF(6,  2013, 33, 1), raisons0.get(0));

			final DroitProprietePersonnePhysiqueRF droit1 = (DroitProprietePersonnePhysiqueRF) droitList.get(1);
			assertNotNull(droit1);
			assertEquals("45729cd9e20", droit1.getMasterIdRF());
			assertEquals("45729cd9e19", droit1.getVersionIdRF());
			assertNull(droit1.getDateDebut());
			assertNull(droit1.getDateFin());
			assertEquals(RegDate.get(2010, 4, 23), droit1.getDateDebutMetier());
			assertEquals("Succession", droit1.getMotifDebut());
			assertEquals("37838sc9d94de", droit1.getAyantDroit().getIdRF());
			assertEquals(new Fraction(1, 1), droit1.getPart());
			assertEquals(GenrePropriete.COMMUNE, droit1.getRegime());

			final List<RaisonAcquisitionRF> raisons1 = new ArrayList<>(droit1.getRaisonsAcquisition());
			assertEquals(1, raisons1.size());
			assertRaisonAcquisition(RegDate.get(2010, 4, 23), "Succession", new IdentifiantAffaireRF(6,  2013, 33, 1), raisons1.get(0));

			final DroitProprietePersonnePhysiqueRF droit2 = (DroitProprietePersonnePhysiqueRF) droitList.get(2);
			assertNotNull(droit2);
			assertEquals("9a9c9e94923", droit2.getMasterIdRF());
			assertEquals("9a9c9e94922", droit2.getVersionIdRF());
			assertNull(droit2.getDateDebut());
			assertNull(droit2.getDateFin());
			assertEquals(RegDate.get(2010, 4, 23), droit2.getDateDebutMetier());
			assertEquals("Succession", droit2.getMotifDebut());
			assertEquals("029191d4fec44", droit2.getAyantDroit().getIdRF());
			assertEquals(new Fraction(1, 1), droit2.getPart());
			assertEquals(GenrePropriete.COMMUNE, droit2.getRegime());

			final List<RaisonAcquisitionRF> raisons2 = new ArrayList<>(droit2.getRaisonsAcquisition());
			assertEquals(1, raisons2.size());
			assertRaisonAcquisition(RegDate.get(2010, 4, 23), "Succession", new IdentifiantAffaireRF(6,  2013, 33, 1), raisons2.get(0));
			return null;
		});

		// postcondition : les événements fiscaux correspondants ont été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(3, events.size());
			events.sort(Comparator.comparing(e -> ((EvenementFiscalDroitPropriete) e).getDroit().getAyantDroit().getId()));

			final EvenementFiscalDroitPropriete event0 = (EvenementFiscalDroitPropriete) events.get(0);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.OUVERTURE, event0.getType());
			assertEquals(RegDate.get(2010, 4, 23), event0.getDateValeur());
			assertEquals("_1f109152381009be0138100bc9f139e0", event0.getDroit().getImmeuble().getIdRF());
			assertEquals(commId, event0.getDroit().getAyantDroit().getId());

			final EvenementFiscalDroitPropriete event1 = (EvenementFiscalDroitPropriete) events.get(1);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.OUVERTURE, event1.getType());
			assertEquals(RegDate.get(2010, 4, 23), event1.getDateValeur());
			assertEquals("_1f109152381009be0138100bc9f139e0", event1.getDroit().getImmeuble().getIdRF());
			assertEquals(ppId1, event1.getDroit().getAyantDroit().getId());

			final EvenementFiscalDroitPropriete event2 = (EvenementFiscalDroitPropriete) events.get(2);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.OUVERTURE, event2.getType());
			assertEquals(RegDate.get(2010, 4, 23), event2.getDateValeur());
			assertEquals("_1f109152381009be0138100bc9f139e0", event2.getDroit().getImmeuble().getIdRF());
			assertEquals(ppId2, event2.getDroit().getAyantDroit().getId());

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

		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/processor/mutation_droit_immeuble_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère quelques données satellites
		insertImmeuble("_8af806fc4a35927c014ae2a6e76041b8");
		insertImmeuble("_1f109152381009be0138100ba7e31031");
		final Long immId = insertImmeubleBeneficiaire("_8af806fc4a35927c014ae2a6e76041b8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateImport, TypeEntiteRF.DROIT, TypeMutationRF.CREATION, "_1f109152381009be0138100ba7e31031", null);

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

		final String idRfPP1 = "_1f109152381009be0138100a1d442eee";
		final String idRfPP2 = "_1f1091523810039001381003da8b72ac";
		final String idImmeubleRF = "_8af806fc4a35927c014ae2a6e76041b8";
		final RegDate dateImportInitial = RegDate.get(2015, 3, 17);
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		// précondition : il y a déjà deux droits dans la base de données
		final Long immeubleId = doInNewTransaction(status -> {

			PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
			pp1.setIdRF(idRfPP1);
			pp1.setNom("Schulz");
			pp1.setPrenom("Alodie");
			pp1.setDateNaissance(RegDate.get(1900, 1, 1));
			pp1 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp1);

			PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
			pp2.setIdRF(idRfPP2);
			pp2.setNom("Schulz");
			pp2.setPrenom("Daniel");
			pp2.setDateNaissance(RegDate.get(1900, 1, 1));
			pp2 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp2);

			BienFondsRF immeuble = new BienFondsRF();
			immeuble.setIdRF(idImmeubleRF);
			immeuble = (BienFondsRF) immeubleRFDAO.save(immeuble);

			// on droit identique à celui qui arrive dans le fichier XML
			final DroitProprietePersonnePhysiqueRF droit0 = new DroitProprietePersonnePhysiqueRF();
			droit0.setMasterIdRF("8af806fa4a4dd302014b16fc17266a0b");
			droit0.setVersionIdRF("8af806fa4a4dd302014b16fc17256a06");
			droit0.setDateDebut(dateImportInitial);
			droit0.setDateDebutMetier(RegDate.get(2003, 1, 1));
			droit0.setMotifDebut("Succession");
			droit0.setAyantDroit(pp1);
			droit0.setImmeuble(immeuble);
			droit0.setPart(new Fraction(1, 2));
			droit0.setRegime(GenrePropriete.COPROPRIETE);
			droit0.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2003, 1, 1), "Succession", new IdentifiantAffaireRF(6, 2003, 9593, 0)));
			droit0.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2014, 12, 23), "Achat", new IdentifiantAffaireRF(6, 2014, 9593, 0)));
			droitRFDAO.save(droit0);


			// on droit différent de celui qui arrive dans le fichier XML
			final DroitProprietePersonnePhysiqueRF droit1 = new DroitProprietePersonnePhysiqueRF();
			droit1.setMasterIdRF("1f109152381009be0138100c87000000");
			droit1.setVersionIdRF("1f109152381009be0138100c87000001");
			droit1.setDateDebut(dateImportInitial);
			droit1.setDateDebutMetier(RegDate.get(1976, 2, 7));
			droit1.setMotifDebut("Appropriation illégitime");
			droit1.setAyantDroit(pp2);
			droit1.setImmeuble(immeuble);
			droit1.setPart(new Fraction(1, 1));
			droit1.setRegime(GenrePropriete.INDIVIDUELLE);
			// motif différent
			droit1.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(1976, 2, 7), "Appropriation illégitime", new IdentifiantAffaireRF(13, 1976, 173, 0)));
			droitRFDAO.save(droit1);

			return immeuble.getId();
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/processor/mutation_droit_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateSecondImport, TypeEntiteRF.DROIT, TypeMutationRF.MODIFICATION, idImmeubleRF, null);

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

			final ImmeubleRF immeuble = immeubleRFDAO.get(immeubleId);
			assertNotNull(immeuble);

			final Set<DroitProprieteRF> droits = immeuble.getDroitsPropriete();
			assertNotNull(droits);
			assertEquals(3, droits.size());

			final List<DroitRF> droitList = new ArrayList<>(droits);
			droitList.sort(new DroitRFRangeMetierComparator());

			// le droit0 doit être fermé
			final DroitProprietePersonnePhysiqueRF droit0 = (DroitProprietePersonnePhysiqueRF) droitList.get(0);
			assertNotNull(droit0);
			assertEquals("1f109152381009be0138100c87000000", droit0.getMasterIdRF());
			assertEquals("1f109152381009be0138100c87000001", droit0.getVersionIdRF());
			assertEquals(dateImportInitial, droit0.getDateDebut());
			assertEquals(dateSecondImport.getOneDayBefore(), droit0.getDateFin());
			assertEquals("Appropriation illégitime", droit0.getMotifDebut());
			assertEquals(RegDate.get(1976, 2, 7), droit0.getDateDebutMetier());
			assertEquals("_1f1091523810039001381003da8b72ac", droit0.getAyantDroit().getIdRF());
			assertEquals(new Fraction(1, 1), droit0.getPart());
			assertEquals(GenrePropriete.INDIVIDUELLE, droit0.getRegime());

			final Set<RaisonAcquisitionRF> raisons0 = droit0.getRaisonsAcquisition();
			assertEquals(1, raisons0.size());
			assertRaisonAcquisition(RegDate.get(1976, 2, 7), "Appropriation illégitime", new IdentifiantAffaireRF(13, 1976, 173, 0), raisons0.iterator().next());

			// un nouveau droit doit remplacer le droit0
			final DroitProprietePersonnePhysiqueRF droit1 = (DroitProprietePersonnePhysiqueRF) droitList.get(1);
			assertNotNull(droit1);
			assertEquals("1f1091523810039001381005be485efd", droit1.getMasterIdRF());
			assertEquals("1f10915238100390013810067ae35d4a", droit1.getVersionIdRF());
			assertEquals(dateSecondImport, droit1.getDateDebut());
			assertNull(droit1.getDateFin());
			assertEquals("Achat", droit1.getMotifDebut());
			assertEquals(RegDate.get(1997, 6, 19), droit1.getDateDebutMetier());
			assertEquals("_1f1091523810039001381003da8b72ac", droit1.getAyantDroit().getIdRF());
			assertEquals(new Fraction(1, 2), droit1.getPart());
			assertEquals(GenrePropriete.COPROPRIETE, droit1.getRegime());

			final List<RaisonAcquisitionRF> raisons1 = new ArrayList<>(droit1.getRaisonsAcquisition());
			raisons1.sort(Comparator.naturalOrder());
			assertEquals(1, raisons1.size());
			assertRaisonAcquisition(RegDate.get(1997, 6, 19), "Achat", new IdentifiantAffaireRF(3, "74'677"), raisons1.get(0));

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
			assertEquals(2, events.size());
			events.sort(Comparator.comparing(e -> ((EvenementFiscalDroitPropriete) e).getType()).reversed()
					            .thenComparing(e -> ((EvenementFiscalDroitPropriete) e).getDroit().getAyantDroit().getId()));

			// fermeture de l'ancien droit
			final EvenementFiscalDroitPropriete event0 = (EvenementFiscalDroitPropriete) events.get(0);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.FERMETURE, event0.getType());
			assertEquals(RegDate.get(1997, 6, 19), event0.getDateValeur());
			final DroitProprieteRF droit0 = event0.getDroit();
			assertEquals("1f109152381009be0138100c87000000", droit0.getMasterIdRF());
			assertEquals("1f109152381009be0138100c87000001", droit0.getVersionIdRF());
			assertEquals(idImmeubleRF, droit0.getImmeuble().getIdRF());
			assertEquals(idRfPP2, droit0.getAyantDroit().getIdRF());

			// ouverture du nouveau droit
			final EvenementFiscalDroitPropriete event1 = (EvenementFiscalDroitPropriete) events.get(1);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.OUVERTURE, event1.getType());
			assertEquals(RegDate.get(1997, 6, 19), event1.getDateValeur());
			final DroitProprieteRF droit1 = event1.getDroit();
			assertEquals("1f1091523810039001381005be485efd", droit1.getMasterIdRF());
			assertEquals("1f10915238100390013810067ae35d4a", droit1.getVersionIdRF());
			assertEquals(idImmeubleRF, droit1.getImmeuble().getIdRF());
			assertEquals(idRfPP2, droit1.getAyantDroit().getIdRF());

			return null;
		});
	}

	/**
	 * [SIFISC-23895] Ce test vérifie que le processing d'une mutation de modification <i>complète</i> de droits entre immeuble fonctionne bien.
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
			droit0.setDateDebutMetier(RegDate.get(2007, 1, 2));
			droit0.setMotifDebut("Appropriation illégitime");
			droit0.setAyantDroit(imm);
			droit0.setImmeuble(immeuble2);
			droit0.setPart(new Fraction(1, 2));
			droit0.setRegime(GenrePropriete.FONDS_DOMINANT);
			droit0.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2007, 1, 2), "Appropriation illégitime", new IdentifiantAffaireRF(13, 2007, 173, 0)));
			droitRFDAO.save(droit0);

			return imm.getId();
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/processor/mutation_droit_immeuble_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateSecondImport, TypeEntiteRF.DROIT, TypeMutationRF.MODIFICATION, idImmeubleRF2, null);

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
			assertEquals(2, events.size());
			events.sort(Comparator.comparing(e -> ((EvenementFiscalDroitPropriete) e).getType()).reversed()
					            .thenComparing(e -> ((EvenementFiscalDroitPropriete) e).getDroit().getAyantDroit().getId()));

			// fermeture de l'ancien droit
			final EvenementFiscalDroitPropriete event0 = (EvenementFiscalDroitPropriete) events.get(0);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.FERMETURE, event0.getType());
			assertEquals(RegDate.get(2010, 4, 11), event0.getDateValeur());
			final DroitProprieteRF droit0 = event0.getDroit();
			assertEquals("1f109152381009be0138100c87000000", droit0.getMasterIdRF());
			assertEquals("1f109152381009be0138100c87000001", droit0.getVersionIdRF());
			assertEquals("_1f109152381009be0138100ba7e31031", droit0.getImmeuble().getIdRF());
			assertEquals(immId, droit0.getAyantDroit().getId());

			// ouverture du nouveau droit
			final EvenementFiscalDroitPropriete event1 = (EvenementFiscalDroitPropriete) events.get(1);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.OUVERTURE, event1.getType());
			assertEquals(RegDate.get(2010, 4, 11), event1.getDateValeur());
			final DroitProprieteRF droit1 = event1.getDroit();
			assertEquals("3838292", droit1.getMasterIdRF());
			assertEquals("3838291", droit1.getVersionIdRF());
			assertEquals("_1f109152381009be0138100ba7e31031", droit1.getImmeuble().getIdRF());
			assertEquals(immId, droit1.getAyantDroit().getId());

			return null;
		});
	}

	/**
	 * [SIFISC-24423] Ce test vérifie que la processing d'une mutation de modification <i>partielle et substentielle</i> (par exemple, un changement de part de co-propriété) de droits entre immeuble ferme bien l'ancien droit et en crée un nouveau.
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
			droit0.setDateDebutMetier(RegDate.get(2007, 1, 2));
			droit0.setMotifDebut("Appropriation illégitime");
			droit0.setAyantDroit(imm);
			droit0.setImmeuble(immeuble2);
			// part différente
			droit0.setPart(new Fraction(1, 3));
			droit0.setRegime(GenrePropriete.FONDS_DOMINANT);
			droit0.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2007, 1, 2), "Appropriation illégitime", new IdentifiantAffaireRF(13, 2007, 173, 0)));
			droitRFDAO.save(droit0);

			return imm.getId();
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/processor/mutation_droit_immeuble_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateSecondImport, TypeEntiteRF.DROIT, TypeMutationRF.MODIFICATION, idImmeubleRF2, null);

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
			assertEquals(2, events.size());
			events.sort(Comparator.comparing(e -> ((EvenementFiscalDroitPropriete) e).getType()).reversed()
					            .thenComparing(e -> ((EvenementFiscalDroitPropriete) e).getDroit().getAyantDroit().getId()));

			// fermeture de l'ancien droit
			final EvenementFiscalDroitPropriete event0 = (EvenementFiscalDroitPropriete) events.get(0);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.FERMETURE, event0.getType());
			assertEquals(RegDate.get(2010, 4, 11), event0.getDateValeur());
			final DroitProprieteRF droit0 = event0.getDroit();
			assertEquals("3838292", droit0.getMasterIdRF());
			assertEquals("3838290", droit0.getVersionIdRF());
			assertEquals("_1f109152381009be0138100ba7e31031", droit0.getImmeuble().getIdRF());
			assertEquals(immId, droit0.getAyantDroit().getId());

			// ouverture du nouveau droit
			final EvenementFiscalDroitPropriete event1 = (EvenementFiscalDroitPropriete) events.get(1);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.OUVERTURE, event1.getType());
			assertEquals(RegDate.get(2010, 4, 11), event1.getDateValeur());
			final DroitProprieteRF droit1 = event1.getDroit();
			assertEquals("3838292", droit1.getMasterIdRF());
			assertEquals("3838291", droit1.getVersionIdRF());
			assertEquals("_1f109152381009be0138100ba7e31031", droit1.getImmeuble().getIdRF());
			assertEquals(immId, droit1.getAyantDroit().getId());

			return null;
		});
	}

	/**
	 * [SIFISC-24987] Ce test vérifie que la processing d'une mutation de modification <i>partielle et substentielle</i> (par exemple, un changement de part de co-propriété) calcule bien une date de début du nouveau droit à partir de la nouvelle
	 * raison d'acquisition.
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
			droit0.setDateDebutMetier(RegDate.get(2010, 4, 11));
			droit0.setMotifDebut("Constitution de PPE");
			droit0.setAyantDroit(imm);
			droit0.setImmeuble(immeuble2);
			// part différente
			droit0.setPart(new Fraction(1, 3));
			droit0.setRegime(GenrePropriete.FONDS_DOMINANT);
			// la même première raison d'acquisition
			droit0.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2010, 4, 11), "Constitution de PPE", new IdentifiantAffaireRF(6, 2013, 17, 0)));
			droitRFDAO.save(droit0);

			return imm.getId();
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/processor/mutation_droit_nouvelle_raison_acquisition_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateSecondImport, TypeEntiteRF.DROIT, TypeMutationRF.MODIFICATION, idImmeubleRF2, null);

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
			assertEquals(2, events.size());
			events.sort(Comparator.comparing(e -> ((EvenementFiscalDroitPropriete) e).getType()).reversed()
					            .thenComparing(e -> ((EvenementFiscalDroitPropriete) e).getDroit().getAyantDroit().getId()));

			// l'ancien droit est fermé
			final EvenementFiscalDroitPropriete event0 = (EvenementFiscalDroitPropriete) events.get(0);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.FERMETURE, event0.getType());
			assertEquals(RegDate.get(2015, 1, 1), event0.getDateValeur());
			final DroitProprieteRF droit0 = event0.getDroit();
			assertEquals("3838292", droit0.getMasterIdRF());
			assertEquals("3838290", droit0.getVersionIdRF());
			assertEquals("_1f109152381009be0138100ba7e31031", droit0.getImmeuble().getIdRF());
			assertEquals(immId, droit0.getAyantDroit().getId());

			// un nouveau droit est créé
			final EvenementFiscalDroitPropriete event1 = (EvenementFiscalDroitPropriete) events.get(1);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.OUVERTURE, event1.getType());
			assertEquals(RegDate.get(2015, 1, 1), event1.getDateValeur());
			final DroitProprieteRF droit1 = event1.getDroit();
			assertEquals("3838292", droit1.getMasterIdRF());
			assertEquals("3838291", droit1.getVersionIdRF());
			assertEquals("_1f109152381009be0138100ba7e31031", droit1.getImmeuble().getIdRF());
			assertEquals(immId, droit1.getAyantDroit().getId());

			return null;
		});
	}

	/**
	 * [SIFISC-25971] Ce test vérifie que la processing d'une mutation de création d'un nouveau droit (nouveau masterId) utilise bien la <b>plus ancienne des nouvelles raisons d'acquisition</b> lorsque :
	 * <ul>
	 *     <li>le nouveau droit possède plusieurs raisons d'acquisition</li>
	 *     <li>il existe un droit fermé dans la même affaire pour le même propriétaire</li>
	 * </ul>
	 */
	@Test
	public void testProcessMutationModificationDroitProprietaireExistantAvecNouvelleRaisonAcquisition() throws Exception {

		final String idImmeubleRF = "_8af806fc4a35927c014ae2a6e76041b8";
		final String pp1RF = "_1f109152381009be0138100a1d442eee";
		final RegDate dateImportInitial = RegDate.get(2015, 3, 17);
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		final RegDate dateSuccession = RegDate.get(2003, 1, 1);
		final RegDate dateAchat = RegDate.get(2014, 12, 23);
		final IdentifiantAffaireRF affaireSuccession = new IdentifiantAffaireRF(6, 2003, 9593, 0);

		class Ids {
			Long immeuble;
			Long communaute;
			Long pp1;
			Long pp2;
		}
		final Ids ids = new Ids();

		// précondition : un immeuble possédé par une communauté de deux personnes
		doInNewTransaction(status -> {

			BienFondsRF immeuble = new BienFondsRF();
			immeuble.setIdRF(idImmeubleRF);
			immeuble = (BienFondsRF) immeubleRFDAO.save(immeuble);

			CommunauteRF communaute = new CommunauteRF();
			communaute.setType(TypeCommunaute.COMMUNAUTE_HEREDITAIRE);
			communaute.setIdRF("a8283ee322");
			communaute = (CommunauteRF) ayantDroitRFDAO.save(communaute);

			PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
			pp1.setIdRF(pp1RF);
			pp1.setNom("Schulz");
			pp1.setPrenom("Alodie");
			pp1.setDateNaissance(RegDate.get(1900, 1, 1));
			pp1 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp1);

			PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
			pp2.setIdRF("_1f109152381009be0138100a1d442xxx");
			pp2.setNom("Schulz");
			pp2.setPrenom("Marcel");
			pp2.setDateNaissance(RegDate.get(1900, 1, 1));
			pp2 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp2);

			DroitProprieteCommunauteRF droitCommunaute = new DroitProprieteCommunauteRF();
			droitCommunaute.setMasterIdRF("29922929");
			droitCommunaute.setVersionIdRF("1");
			droitCommunaute.setPart(new Fraction(1, 1));
			droitCommunaute.setRegime(GenrePropriete.COMMUNE);
			droitCommunaute.setDateDebut(dateImportInitial);
			droitCommunaute.setDateDebutMetier(dateSuccession);
			droitCommunaute.setMotifDebut("Succession");
			droitCommunaute.addRaisonAcquisition(new RaisonAcquisitionRF(dateSuccession, "Succession", affaireSuccession));
			droitCommunaute.setAyantDroit(communaute);
			droitCommunaute.setImmeuble(immeuble);
			droitRFDAO.save(droitCommunaute);

			DroitProprietePersonnePhysiqueRF droitPP1 = new DroitProprietePersonnePhysiqueRF();
			droitPP1.setMasterIdRF("378378237");
			droitPP1.setVersionIdRF("1");
			droitPP1.setPart(new Fraction(1, 1));
			droitPP1.setRegime(GenrePropriete.COMMUNE);
			droitPP1.setDateDebut(dateImportInitial);
			droitPP1.setDateDebutMetier(dateSuccession);
			droitPP1.setMotifDebut("Succession");
			droitPP1.addRaisonAcquisition(new RaisonAcquisitionRF(dateSuccession, "Succession", affaireSuccession));
			droitPP1.setAyantDroit(pp1);
			droitPP1.setImmeuble(immeuble);
			droitPP1.setCommunaute(communaute);
			droitRFDAO.save(droitPP1);

			DroitProprietePersonnePhysiqueRF droitPP2 = new DroitProprietePersonnePhysiqueRF();
			droitPP2.setMasterIdRF("509389228");
			droitPP2.setVersionIdRF("1");
			droitPP2.setPart(new Fraction(1, 1));
			droitPP2.setRegime(GenrePropriete.COMMUNE);
			droitPP2.setDateDebut(dateImportInitial);
			droitPP2.setDateDebutMetier(dateSuccession);
			droitPP2.setMotifDebut("Succession");
			droitPP2.addRaisonAcquisition(new RaisonAcquisitionRF(dateSuccession, "Succession", affaireSuccession));
			droitPP2.setAyantDroit(pp2);
			droitPP2.setImmeuble(immeuble);
			droitPP2.setCommunaute(communaute);
			droitRFDAO.save(droitPP2);

			ids.immeuble = immeuble.getId();
			ids.communaute = communaute.getId();
			ids.pp1 = pp1.getId();
			ids.pp2 = pp2.getId();
			return null;
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/processor/mutation_droit_plusieurs_raisons_acquisition_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateSecondImport, TypeEntiteRF.DROIT, TypeMutationRF.MODIFICATION, idImmeubleRF, null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée et :
		//  - les anciens droits sont fermés
		//  - un nouveau droit est créé avec une date de début métier = 2014-12-23
		doInNewTransaction(status -> {

			final BienFondsRF imm = (BienFondsRF) immeubleRFDAO.get(ids.immeuble);
			assertNotNull(imm);

			final List<DroitProprieteRF> droits = imm.getDroitsPropriete().stream()
					.filter(d -> d.getAyantDroit().getIdRF().equals(pp1RF)) // on ne s'intéresse qu'aux droits du propriétaire pp1
					.collect(Collectors.toList());
			assertNotNull(droits);
			assertEquals(2, droits.size());

			final List<DroitRF> droitList = new ArrayList<>(droits);
			droitList.sort(new DateRangeComparator<>());

			// l'ancien droit doit être fermé
			final DroitProprieteRF droit0 = (DroitProprieteRF) droitList.get(0);
			assertNotNull(droit0);
			assertEquals(dateImportInitial, droit0.getDateDebut());
			assertEquals(dateSecondImport.getOneDayBefore(), droit0.getDateFin());
			assertEquals("Succession", droit0.getMotifDebut());
			assertEquals(dateSuccession, droit0.getDateDebutMetier());

			// le nouveau droit doit avoir une date de début métier = 2014-12-23
			final DroitProprieteRF droit1 = (DroitProprieteRF) droitList.get(1);
			assertNotNull(droit1);
			assertEquals(dateSecondImport, droit1.getDateDebut());
			assertNull(droit1.getDateFin());
			assertEquals("Achat", droit1.getMotifDebut());
			assertEquals(dateAchat, droit1.getDateDebutMetier());

			final List<RaisonAcquisitionRF> raisons1 = new ArrayList<>(droit1.getRaisonsAcquisition());
			assertEquals(2, raisons1.size());
			raisons1.sort(Comparator.comparing(RaisonAcquisitionRF::getDateAcquisition));
			assertRaisonAcquisition(dateSuccession, "Succession", new IdentifiantAffaireRF(6, 2003, 9593, 0), raisons1.get(0));
			assertRaisonAcquisition(dateAchat, "Achat", new IdentifiantAffaireRF(6, 2014, 9593, 0), raisons1.get(1));

			return null;
		});

		// postcondition : les événements fiscaux correspondants ont été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(4, events.size());
			events.sort(Comparator.comparing(e -> ((EvenementFiscalDroitPropriete) e).getType()).reversed()
					            .thenComparing(e -> ((EvenementFiscalDroitPropriete) e).getDroit().getAyantDroit().getId()));

			// fermeture de l'ancien droit de la comunauté
			final EvenementFiscalDroitPropriete event0 = (EvenementFiscalDroitPropriete) events.get(0);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.FERMETURE, event0.getType());
			assertEquals(dateAchat, event0.getDateValeur());
			assertEquals(idImmeubleRF, event0.getDroit().getImmeuble().getIdRF());
			assertEquals(ids.communaute, event0.getDroit().getAyantDroit().getId());

			// fermeture de l'ancien droit de la pp1
			final EvenementFiscalDroitPropriete event1 = (EvenementFiscalDroitPropriete) events.get(1);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.FERMETURE, event1.getType());
			assertEquals(dateAchat, event1.getDateValeur());
			assertEquals(idImmeubleRF, event1.getDroit().getImmeuble().getIdRF());
			assertEquals(ids.pp1, event1.getDroit().getAyantDroit().getId());

			// fermeture de l'ancien droit de la pp2
			final EvenementFiscalDroitPropriete event2 = (EvenementFiscalDroitPropriete) events.get(2);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.FERMETURE, event2.getType());
			assertEquals(dateAchat, event2.getDateValeur());
			assertEquals(idImmeubleRF, event2.getDroit().getImmeuble().getIdRF());
			assertEquals(ids.pp2, event2.getDroit().getAyantDroit().getId());

			// ouverture du nouveau droit de la pp1
			final EvenementFiscalDroitPropriete event3 = (EvenementFiscalDroitPropriete) events.get(3);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.OUVERTURE, event3.getType());
			assertEquals(dateAchat, event3.getDateValeur());
			assertEquals(idImmeubleRF, event3.getDroit().getImmeuble().getIdRF());
			assertEquals(ids.pp1, event3.getDroit().getAyantDroit().getId());

			return null;
		});
	}

	/*
	 * Ce test vérifie que le processing d'une mutation de modification de droits fonctionne bien dans le cas où
	 * la seule différence est une nouvelle raison d'acquisition (même masterID et même versionID) : la liste des
	 * raisons d'acquisition est juste mise-à-jour.
	 */
	@Test
	public void testProcessMutationModificationNouvelleRaisonAcquisition() throws Exception {

		final String idRfPP1 = "_1f109152381009be0138100a1d442eee";
		final String idRfPP2 = "_1f1091523810039001381003da8b72ac";
		final String idImmeubleRF = "_8af806fc4a35927c014ae2a6e76041b8";
		final RegDate dateImportInitial = RegDate.get(2015, 3, 17);
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		// précondition : il y a déjà deux droits dans la base de données
		final Long immeubleId = doInNewTransaction(status -> {

			PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
			pp1.setIdRF(idRfPP1);
			pp1.setNom("Schulz");
			pp1.setPrenom("Alodie");
			pp1.setDateNaissance(RegDate.get(1900, 1, 1));
			pp1 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp1);

			PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
			pp2.setIdRF(idRfPP2);
			pp2.setNom("Schulz");
			pp2.setPrenom("Daniel");
			pp2.setDateNaissance(RegDate.get(1900, 1, 1));
			pp2 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp2);

			BienFondsRF immeuble = new BienFondsRF();
			immeuble.setIdRF(idImmeubleRF);
			immeuble = (BienFondsRF) immeubleRFDAO.save(immeuble);

			// on droit différent de celui qui arrive dans le fichier XML
			final DroitProprietePersonnePhysiqueRF droit0 = new DroitProprietePersonnePhysiqueRF();
			droit0.setMasterIdRF("8af806fa4a4dd302014b16fc17266a0b");   // <--- même masterID
			droit0.setVersionIdRF("8af806fa4a4dd302014b16fc17256a06");  // <--- même versionID
			droit0.setDateDebut(dateImportInitial);
			droit0.setDateDebutMetier(RegDate.get(2003, 1, 1));
			droit0.setMotifDebut("Succession");
			droit0.setAyantDroit(pp1);
			droit0.setImmeuble(immeuble);
			droit0.setPart(new Fraction(1, 2));
			droit0.setRegime(GenrePropriete.COPROPRIETE);
			// une seule raison d'acquisition (il y en a deux dans le fichier)
			droit0.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2003, 1, 1), "Succession", new IdentifiantAffaireRF(6, 2003, 9593, 0)));
			droitRFDAO.save(droit0);

			// on droit identique à celui qui arrive dans le fichier XML
			final DroitProprietePersonnePhysiqueRF droit1 = new DroitProprietePersonnePhysiqueRF();
			droit1.setMasterIdRF("1f1091523810039001381005be485efd");
			droit1.setVersionIdRF("1f10915238100390013810067ae35d4a");
			droit1.setDateDebut(dateImportInitial);
			droit1.setDateDebutMetier(RegDate.get(1997, 6, 19));
			droit1.setMotifDebut("Achat");
			droit1.setAyantDroit(pp2);
			droit1.setImmeuble(immeuble);
			droit1.setPart(new Fraction(1, 2));
			droit1.setRegime(GenrePropriete.COPROPRIETE);
			droit1.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(1997, 6, 19), "Achat", new IdentifiantAffaireRF(3, "74'677")));
			droitRFDAO.save(droit1);

			return immeuble.getId();
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/processor/mutation_droit_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateSecondImport, TypeEntiteRF.DROIT, TypeMutationRF.MODIFICATION, idImmeubleRF, null);

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

			final ImmeubleRF immeuble = immeubleRFDAO.get(immeubleId);
			assertNotNull(immeuble);

			final Set<DroitProprieteRF> droits = immeuble.getDroitsPropriete();
			assertNotNull(droits);
			assertEquals(2, droits.size());

			final List<DroitRF> droitList = new ArrayList<>(droits);
			droitList.sort(new DroitRFRangeMetierComparator());

			// le dernier droit reste inchangé
			final DroitProprietePersonnePhysiqueRF droit0 = (DroitProprietePersonnePhysiqueRF) droitList.get(0);
			assertNotNull(droit0);
			assertEquals("1f1091523810039001381005be485efd", droit0.getMasterIdRF());
			assertEquals("1f10915238100390013810067ae35d4a", droit0.getVersionIdRF());
			assertEquals(dateImportInitial, droit0.getDateDebut());
			assertNull(droit0.getDateFin());
			assertEquals("Achat", droit0.getMotifDebut());
			assertEquals(RegDate.get(1997, 6, 19), droit0.getDateDebutMetier());
			assertEquals(idImmeubleRF, droit0.getImmeuble().getIdRF());
			assertEquals(idRfPP2, droit0.getAyantDroit().getIdRF());
			assertEquals(new Fraction(1, 2), droit0.getPart());
			assertEquals(GenrePropriete.COPROPRIETE, droit0.getRegime());

			final List<RaisonAcquisitionRF> raisons0 = new ArrayList<>(droit0.getRaisonsAcquisition());
			raisons0.sort(Comparator.naturalOrder());
			assertEquals(1, raisons0.size());
			assertRaisonAcquisition(RegDate.get(1997, 6, 19), "Achat", new IdentifiantAffaireRF(3, "74'677"), raisons0.get(0));

			// le droit0 doit être à jour
			final DroitProprietePersonnePhysiqueRF droit1 = (DroitProprietePersonnePhysiqueRF) droitList.get(1);
			assertNotNull(droit1);
			assertEquals("8af806fa4a4dd302014b16fc17266a0b", droit1.getMasterIdRF());
			assertEquals("8af806fa4a4dd302014b16fc17256a06", droit1.getVersionIdRF());
			assertEquals(dateImportInitial, droit1.getDateDebut());
			assertNull(droit1.getDateFin());
			assertEquals("Succession", droit1.getMotifDebut());
			assertEquals(RegDate.get(2003, 1, 1), droit1.getDateDebutMetier());
			assertEquals(idImmeubleRF, droit1.getImmeuble().getIdRF());
			assertEquals(idRfPP1, droit1.getAyantDroit().getIdRF());
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
			assertEquals(RegDate.get(2003, 1, 1), event0.getDateValeur());
			assertEquals(idImmeubleRF, event0.getDroit().getImmeuble().getIdRF());
			assertEquals(idRfPP1, event0.getDroit().getAyantDroit().getIdRF());

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
			droit0.setDateDebutMetier(RegDate.get(2005, 1, 1));
			droit0.setMotifDebut("Achat");
			droit0.setAyantDroit(pp);
			droit0.setImmeuble(immeuble1);
			droit0.setPart(new Fraction(1, 1));
			droit0.setRegime(GenrePropriete.INDIVIDUELLE);
			// une seule raison d'acquisition (il n'y en a pas dans le fichier)
			droit0.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2005, 1, 1), "Achat", new IdentifiantAffaireRF(13, 2005, 173, 0)));
			droitRFDAO.save(droit0);

			return pp.getId();
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/processor/mutation_droit_sans_raison_acquistion_rf.xml");
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
			assertNull(droit0.getMotifDebut());
			assertNull(droit0.getDateDebutMetier());
			assertEquals(idPPRF, droit0.getAyantDroit().getIdRF());
			assertEquals(idImmeubleRF1, droit0.getImmeuble().getIdRF());
			assertEquals(new Fraction(1, 1), droit0.getPart());
			assertEquals(GenrePropriete.INDIVIDUELLE, droit0.getRegime());

			// il n'y a plus de raisons d'acquisition valide
			final List<RaisonAcquisitionRF> raisons0 = new ArrayList<>(droit0.getRaisonsAcquisition());
			assertEquals(1, raisons0.size());
			assertNotNull(raisons0.get(0).getAnnulationDate());
			return null;
		});

		// postcondition : les événements fiscaux correspondants ont été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());

			// modification de la date de début métier
			final EvenementFiscalDroitPropriete event1 = (EvenementFiscalDroitPropriete) events.get(0);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.MODIFICATION, event1.getType());
			assertNull(event1.getDateValeur());
			assertEquals(idImmeubleRF1, event1.getDroit().getImmeuble().getIdRF());
			assertEquals(idPPRF, event1.getDroit().getAyantDroit().getIdRF());

			return null;
		});
	}

	/*
	 * Ce test vérifie que le processing d'une mutation de suppression fonctionne bien.
	 */
	@Test
	public void testProcessMutationSuppression() throws Exception {

		final String idRfPP1 = "_1f109152381009be0138100a1d442eee";
		final String idRfPP2 = "_1f1091523810039001381003da8b72ac";
		final String idImmeubleRF = "_8af806fc4a35927c014ae2a6e76041b8";
		final RegDate dateImportInitial = RegDate.get(2015, 3, 17);
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		// précondition : une personne physique avec  deux droits dans la base de données
		final Long immeubleId = doInNewTransaction(status -> {

			PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
			pp1.setIdRF(idRfPP1);
			pp1.setNom("Schulz");
			pp1.setPrenom("Alodie");
			pp1.setDateNaissance(RegDate.get(1900, 1, 1));
			pp1 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp1);

			PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
			pp2.setIdRF(idRfPP2);
			pp2.setNom("Schulz");
			pp2.setPrenom("Daniel");
			pp2.setDateNaissance(RegDate.get(1900, 1, 1));
			pp2 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp2);

			BienFondsRF immeuble = new BienFondsRF();
			immeuble.setIdRF(idImmeubleRF);
			immeuble = (BienFondsRF) immeubleRFDAO.save(immeuble);

			// on droit
			final DroitProprietePersonnePhysiqueRF droit0 = new DroitProprietePersonnePhysiqueRF();
			droit0.setMasterIdRF("8af806fa4a4dd302014b16fc17266a0b");
			droit0.setVersionIdRF("8af806fa4a4dd302014b16fc17256a06");
			droit0.setDateDebut(dateImportInitial);
			droit0.setDateDebutMetier(RegDate.get(2003, 1, 1));
			droit0.setMotifDebut("Succession");
			droit0.setAyantDroit(pp1);
			droit0.setImmeuble(immeuble);
			droit0.setPart(new Fraction(1, 2));
			droit0.setRegime(GenrePropriete.COPROPRIETE);
			droit0.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2003, 1, 1), "Succession", new IdentifiantAffaireRF(6, 2003, 9593, 0)));
			droit0.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2014, 12, 23), "Achat", new IdentifiantAffaireRF(6, 2014, 9593, 0)));
			droitRFDAO.save(droit0);

			// on autre droit
			final DroitProprietePersonnePhysiqueRF droit1 = new DroitProprietePersonnePhysiqueRF();
			droit1.setMasterIdRF("1f109152381009be0138100c87000000");
			droit1.setVersionIdRF("1f109152381009be0138100c87000001");
			droit1.setDateDebut(dateImportInitial);
			droit1.setDateDebutMetier(RegDate.get(1976, 2, 7));
			droit1.setMotifDebut("Appropriation illégitime");
			droit1.setAyantDroit(pp2);
			droit1.setImmeuble(immeuble);
			droit1.setPart(new Fraction(1, 2));
			droit1.setRegime(GenrePropriete.COPROPRIETE);
			droit1.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(1976, 2, 7), "Appropriation illégitime", new IdentifiantAffaireRF(13, 1976, 173, 0)));
			droitRFDAO.save(droit1);

			return immeuble.getId();
		});

		// on envoie un fichier d'import qui ne contient aucun droit
		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/processor/mutation_droit_vide_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateSecondImport, TypeEntiteRF.DROIT, TypeMutationRF.SUPPRESSION, idImmeubleRF, null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée et tous les droits existants sont fermés
		doInNewTransaction(status -> {

			final ImmeubleRF immeuble = immeubleRFDAO.get(immeubleId);
			assertNotNull(immeuble);

			final Set<DroitProprieteRF> droits = immeuble.getDroitsPropriete();
			assertNotNull(droits);
			assertEquals(2, droits.size());

			final List<DroitRF> droitList = new ArrayList<>(droits);
			droitList.sort(new DroitRFRangeMetierComparator());

			// le droit0 doit être fermé
			final DroitProprietePersonnePhysiqueRF droit0 = (DroitProprietePersonnePhysiqueRF) droitList.get(0);
			assertNotNull(droit0);
			assertEquals("1f109152381009be0138100c87000000", droit0.getMasterIdRF());
			assertEquals("1f109152381009be0138100c87000001", droit0.getVersionIdRF());
			assertEquals(dateImportInitial, droit0.getDateDebut());
			assertEquals(dateSecondImport.getOneDayBefore(), droit0.getDateFin());
			assertEquals("Appropriation illégitime", droit0.getMotifDebut());
			assertEquals(RegDate.get(1976, 2, 7), droit0.getDateDebutMetier());
			assertEquals(idImmeubleRF, droit0.getImmeuble().getIdRF());
			assertEquals(new Fraction(1, 2), droit0.getPart());
			assertEquals(GenrePropriete.COPROPRIETE, droit0.getRegime());

			final Set<RaisonAcquisitionRF> raisons0 = droit0.getRaisonsAcquisition();
			assertEquals(1, raisons0.size());
			assertRaisonAcquisition(RegDate.get(1976, 2, 7), "Appropriation illégitime", new IdentifiantAffaireRF(13, 1976, 173, 0), raisons0.iterator().next());

			// le droit1 doit être fermé
			final DroitProprietePersonnePhysiqueRF droit1 = (DroitProprietePersonnePhysiqueRF) droitList.get(1);
			assertNotNull(droit1);
			assertEquals("8af806fa4a4dd302014b16fc17266a0b", droit1.getMasterIdRF());
			assertEquals("8af806fa4a4dd302014b16fc17256a06", droit1.getVersionIdRF());
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
			assertEquals(2, events.size());
			events.sort(Comparator.comparing(e -> ((EvenementFiscalDroitPropriete) e).getType()).reversed()
					            .thenComparing(e -> ((EvenementFiscalDroitPropriete) e).getDroit().getMasterIdRF()));

			// fermeture du droit droit0
			final EvenementFiscalDroitPropriete event0 = (EvenementFiscalDroitPropriete) events.get(0);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.FERMETURE, event0.getType());
			assertNull(event0.getDateValeur());
			final DroitProprieteRF droit0 = event0.getDroit();
			assertEquals("1f109152381009be0138100c87000000", droit0.getMasterIdRF());
			assertEquals("1f109152381009be0138100c87000001", droit0.getVersionIdRF());
			assertEquals(idImmeubleRF, droit0.getImmeuble().getIdRF());

			// fermeture du droit droit1
			final EvenementFiscalDroitPropriete event1 = (EvenementFiscalDroitPropriete) events.get(1);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.FERMETURE, event1.getType());
			assertNull(event1.getDateValeur());
			final DroitProprieteRF droit1 = event1.getDroit();
			assertEquals("8af806fa4a4dd302014b16fc17266a0b", droit1.getMasterIdRF());
			assertEquals("8af806fa4a4dd302014b16fc17256a06", droit1.getVersionIdRF());
			assertEquals(idImmeubleRF, droit1.getImmeuble().getIdRF());

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
				droit0.setDateDebutMetier(RegDate.get(2007, 1, 2));
				droit0.setMotifDebut("Appropriation illégitime");
				droit0.setAyantDroit(imm);
				droit0.setImmeuble(immeuble2);
				droit0.setPart(new Fraction(1, 2));
				droit0.setRegime(GenrePropriete.FONDS_DOMINANT);
				droit0.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2007, 1, 2), "Appropriation illégitime", new IdentifiantAffaireRF(13, 2007, 173, 0)));
				droitRFDAO.save(droit0);

				return imm.getId();
			}
		});

		// on envoie un fichier d'import qui ne contient aucun droit
		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/processor/mutation_droit_vide_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateSecondImport, TypeEntiteRF.DROIT, TypeMutationRF.SUPPRESSION, idImmeubleRF2, null);

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
	public void testProcessMutationModificationNouvelleCommunauteAvecRepriseUnMembreSansNouvelleRaisonAcquisition() throws Exception {

		final String idRFAndres = "_8af806fc40347c3701412bead24754e2";
		final String idRFOdette = "_8af806fc40347c3701412c138f785658";
		final String idRFCarlos = "_8af80e62583eaf7b0158af42818778a6";
		final String idRFCommunaute1 = "_8af806fc40347c370141502e2d5d1516";
		final String idRFCommunaute2 = "_8af8064d58e34c5a0159cf79c9b71925";
		final String idImmeubleRF = "_1f109152381059670138105e159419b0";
		final RegDate dateImportInitial = null;
		final RegDate dateSecondImport = RegDate.get(2017, 1, 28);
		final RegDate dateSuccession = RegDate.get(2013, 9, 13);
		final RegDate dateAchat = RegDate.get(2016, 11, 22);

		// précondition : il y a une communauté composée de Andrès et Odette
		final Long immeubleId = doInNewTransaction(status -> {

			CommunauteRF communaute1 = new CommunauteRF();
			communaute1.setIdRF(idRFCommunaute1);
			communaute1 = (CommunauteRF) ayantDroitRFDAO.save(communaute1);

			CommunauteRF communaute2 = new CommunauteRF();
			communaute2.setIdRF(idRFCommunaute2);
			ayantDroitRFDAO.save(communaute2);

			PersonnePhysiqueRF andres = new PersonnePhysiqueRF();
			andres.setIdRF(idRFAndres);
			andres.setNom("Dub Addor");
			andres.setPrenom("Andrès");
			andres.setDateNaissance(RegDate.get(1900, 1, 1));
			andres = (PersonnePhysiqueRF) ayantDroitRFDAO.save(andres);

			PersonnePhysiqueRF odette = new PersonnePhysiqueRF();
			odette.setIdRF(idRFOdette);
			odette.setNom("Sprüngli");
			odette.setPrenom("Odette");
			odette.setDateNaissance(RegDate.get(1900, 1, 1));
			odette = (PersonnePhysiqueRF) ayantDroitRFDAO.save(odette);

			PersonnePhysiqueRF carlos = new PersonnePhysiqueRF();
			carlos.setIdRF(idRFCarlos);
			carlos.setNom("Sprüngli");
			carlos.setPrenom("Carlos");
			carlos.setDateNaissance(RegDate.get(1900, 1, 1));
			ayantDroitRFDAO.save(carlos);

			BienFondsRF immeuble = new BienFondsRF();
			immeuble.setIdRF(idImmeubleRF);
			immeuble = (BienFondsRF) immeubleRFDAO.save(immeuble);

			// le droit de la communauté
			DroitProprieteCommunauteRF droit0 = new DroitProprieteCommunauteRF();
			droit0.setMasterIdRF("8af806fc40347c370141502e2d5d1516");
			droit0.setVersionIdRF("8af806fc40347c370141502e2d5d1513");
			droit0.setDateDebut(dateImportInitial);
			droit0.setDateDebutMetier(dateSuccession);
			droit0.setMotifDebut("Succession");
			droit0.setAyantDroit(communaute1);
			droit0.setImmeuble(immeuble);
			droit0.setPart(new Fraction(1, 1));
			droit0.setRegime(GenrePropriete.COMMUNE);
			droitRFDAO.save(droit0);

			// le droit d'Andrès
			final DroitProprietePersonnePhysiqueRF droit1 = new DroitProprietePersonnePhysiqueRF();
			droit1.setMasterIdRF("8af806fc40347c370141502e2d5e1518");
			droit1.setVersionIdRF("8af806fc40347c370141502e2d5d1513");
			droit1.setDateDebut(dateImportInitial);
			droit1.setDateDebutMetier(dateSuccession);
			droit1.setMotifDebut("Succession");
			droit1.setAyantDroit(andres);
			droit1.setCommunaute(communaute1);
			droit1.setImmeuble(immeuble);
			droit1.setPart(new Fraction(1, 1));
			droit1.setRegime(GenrePropriete.COMMUNE);
			droit1.addRaisonAcquisition(new RaisonAcquisitionRF(dateSuccession, "Succession", new IdentifiantAffaireRF(12, 2013, 4914, 0)));
			droitRFDAO.save(droit1);

			// le droit d'Odette
			final DroitProprietePersonnePhysiqueRF droit2 = new DroitProprietePersonnePhysiqueRF();
			droit2.setMasterIdRF("8af806fc40347c370141502e6cef151d");
			droit2.setVersionIdRF("8af806fc40347c370141502e2d5d1513");
			droit2.setDateDebut(dateImportInitial);
			droit2.setDateDebutMetier(dateSuccession);
			droit2.setMotifDebut("Succession");
			droit2.setAyantDroit(odette);
			droit2.setCommunaute(communaute1);
			droit2.setImmeuble(immeuble);
			droit2.setPart(new Fraction(1, 1));
			droit2.setRegime(GenrePropriete.COMMUNE);
			droit2.addRaisonAcquisition(new RaisonAcquisitionRF(dateSuccession, "Succession", new IdentifiantAffaireRF(12, 2013, 4914, 0)));
			droitRFDAO.save(droit2);

			return immeuble.getId();
		});

		// fichier de mutation où :
		// - Andrès et la communauté 1 disparaissent
		// - la communauté 2 et Carlos apparaissent
		// - Odette change de la communauté 1 à la communauté 2 mais le nouveau droit garde la raison d'acquisition initiale
		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/processor/mutation_droit_arzier_le_muid_363_20170128.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");
		final Long mutationId = insertMutation(xml, dateSecondImport, TypeEntiteRF.DROIT, TypeMutationRF.MODIFICATION, idImmeubleRF, null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée et :
		//  - les 0, 1 et 2 sont fermés au 22.11.2016
		//  - de nouveaux droits 3, 4 et 5 sont ouverts au 22.11.2016 
		doInNewTransaction(status -> {

			final ImmeubleRF immeuble = immeubleRFDAO.get(immeubleId);
			assertNotNull(immeuble);

			final List<DroitProprieteRF> droits = new ArrayList<>(immeuble.getDroitsPropriete());
			assertEquals(6, droits.size());
			droits.sort(new DroitRFRangeMetierComparator());

			// le droit de la première communauté doit être fermé au 22.11.2016
			final DroitProprieteCommunauteRF droit0 = (DroitProprieteCommunauteRF) droits.get(0);
			assertNotNull(droit0);
			assertEquals("8af806fc40347c370141502e2d5d1516", droit0.getMasterIdRF());
			assertEquals("8af806fc40347c370141502e2d5d1513", droit0.getVersionIdRF());
			assertEquals(dateImportInitial, droit0.getDateDebut());
			assertEquals(dateSecondImport.getOneDayBefore(), droit0.getDateFin());
			assertEquals(dateSuccession, droit0.getDateDebutMetier());
			assertEquals(dateAchat, droit0.getDateFinMetier());
			assertEquals("Succession", droit0.getMotifDebut());
			assertEquals("Vente", droit0.getMotifFin());
			assertEquals(idRFCommunaute1, droit0.getAyantDroit().getIdRF());
			assertEquals(idImmeubleRF, droit0.getImmeuble().getIdRF());
			assertEquals(new Fraction(1, 1), droit0.getPart());
			assertEquals(GenrePropriete.COMMUNE, droit0.getRegime());

			// le droit d'Andrès doit être fermé au 22.11.2016
			final DroitProprietePersonnePhysiqueRF droit1 = (DroitProprietePersonnePhysiqueRF) droits.get(1);
			assertNotNull(droit1);
			assertEquals("8af806fc40347c370141502e2d5e1518", droit1.getMasterIdRF());
			assertEquals("8af806fc40347c370141502e2d5d1513", droit1.getVersionIdRF());
			assertEquals(dateImportInitial, droit1.getDateDebut());
			assertEquals(dateSecondImport.getOneDayBefore(), droit1.getDateFin());
			assertEquals(dateSuccession, droit1.getDateDebutMetier());
			assertEquals(dateAchat, droit1.getDateFinMetier());
			assertEquals("Succession", droit1.getMotifDebut());
			assertEquals("Vente", droit1.getMotifFin());
			assertEquals(idRFAndres, droit1.getAyantDroit().getIdRF());
			assertEquals(idImmeubleRF, droit1.getImmeuble().getIdRF());
			assertEquals(idRFCommunaute1, droit1.getCommunaute().getIdRF());
			assertEquals(new Fraction(1, 1), droit1.getPart());
			assertEquals(GenrePropriete.COMMUNE, droit1.getRegime());

			// le droit d'Odette doit être fermé au 22.11.2016
			final DroitProprietePersonnePhysiqueRF droit2 = (DroitProprietePersonnePhysiqueRF) droits.get(2);
			assertNotNull(droit2);
			assertEquals("8af806fc40347c370141502e6cef151d", droit2.getMasterIdRF());
			assertEquals("8af806fc40347c370141502e2d5d1513", droit2.getVersionIdRF());
			assertEquals(dateImportInitial, droit2.getDateDebut());
			assertEquals(dateSecondImport.getOneDayBefore(), droit2.getDateFin());
			assertEquals(dateSuccession, droit2.getDateDebutMetier());
			assertEquals(dateAchat, droit2.getDateFinMetier());
			assertEquals("Succession", droit2.getMotifDebut());
			assertEquals("Vente", droit2.getMotifFin());
			assertEquals(idRFOdette, droit2.getAyantDroit().getIdRF());
			assertEquals(idImmeubleRF, droit2.getImmeuble().getIdRF());
			assertEquals(idRFCommunaute1, droit2.getCommunaute().getIdRF());
			assertEquals(new Fraction(1, 1), droit2.getPart());
			assertEquals(GenrePropriete.COMMUNE, droit2.getRegime());

			// le droit de la seconde communauté doit être ouvert au 22.11.2016
			final DroitProprieteCommunauteRF droit3 = (DroitProprieteCommunauteRF) droits.get(3);
			assertNotNull(droit3);
			assertEquals("8af8064d58e34c5a0159cf79c9b71925", droit3.getMasterIdRF());
			assertEquals("8af8064d58e34c5a0159cf79c9b71922", droit3.getVersionIdRF());
			assertEquals(dateSecondImport, droit3.getDateDebut());
			assertNull(droit3.getDateFin());
			assertEquals(dateAchat, droit3.getDateDebutMetier());
			assertNull(droit3.getDateFinMetier());
			assertEquals("Achat", droit3.getMotifDebut());
			assertNull(droit3.getMotifFin());
			assertEquals(idRFCommunaute2, droit3.getAyantDroit().getIdRF());
			assertEquals(idImmeubleRF, droit3.getImmeuble().getIdRF());
			assertEquals(new Fraction(1, 1), droit3.getPart());
			assertEquals(GenrePropriete.COMMUNE, droit3.getRegime());

			// le nouveau droit d'Odette doit être ouvert au 22.11.2016
			final DroitProprietePersonnePhysiqueRF droit4 = (DroitProprietePersonnePhysiqueRF) droits.get(4);
			assertNotNull(droit4);
			assertEquals("8af8064d58e34c5a0159cf79eaf2192d", droit4.getMasterIdRF());
			assertEquals("8af8064d58e34c5a0159cf79c9b71922", droit4.getVersionIdRF());
			assertEquals(dateSecondImport, droit4.getDateDebut());
			assertNull(droit4.getDateFin());
			assertEquals(dateAchat, droit4.getDateDebutMetier());
			assertNull(droit4.getDateFinMetier());
			assertEquals("Achat", droit4.getMotifDebut());
			assertNull(droit4.getMotifFin());
			assertEquals(idRFOdette, droit4.getAyantDroit().getIdRF());
			assertEquals(idRFCommunaute2, droit4.getCommunaute().getIdRF());
			assertEquals(idImmeubleRF, droit4.getImmeuble().getIdRF());
			assertEquals(new Fraction(1, 1), droit4.getPart());
			assertEquals(GenrePropriete.COMMUNE, droit4.getRegime());

			// le droit de Carlos doit être ouvert au 22.11.2016
			final DroitProprietePersonnePhysiqueRF droit5 = (DroitProprietePersonnePhysiqueRF) droits.get(5);
			assertNotNull(droit5);
			assertEquals("8af8064d58e34c5a0159cf79c9b71927", droit5.getMasterIdRF());
			assertEquals("8af8064d58e34c5a0159cf79c9b71922", droit5.getVersionIdRF());
			assertEquals(dateSecondImport, droit5.getDateDebut());
			assertNull(droit5.getDateFin());
			assertEquals(dateAchat, droit5.getDateDebutMetier());
			assertNull(droit5.getDateFinMetier());
			assertEquals("Achat", droit5.getMotifDebut());
			assertNull(droit5.getMotifFin());
			assertEquals(idRFCarlos, droit5.getAyantDroit().getIdRF());
			assertEquals(idRFCommunaute2, droit5.getCommunaute().getIdRF());
			assertEquals(idImmeubleRF, droit5.getImmeuble().getIdRF());
			assertEquals(new Fraction(1, 1), droit5.getPart());
			assertEquals(GenrePropriete.COMMUNE, droit5.getRegime());

			return null;
		});

		// postcondition : les événements fiscaux correspondants ont été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscalDroitPropriete> events = evenementFiscalDAO.getAll().stream()
					.map(EvenementFiscalDroitPropriete.class::cast)
					.sorted(Comparator.comparing(EvenementFiscalDroitPropriete::getType).thenComparing(e -> e.getDroit().getAyantDroit().getId()))
					.collect(Collectors.toList());
			assertEquals(6, events.size());

			final EvenementFiscalDroitPropriete event3 = events.get(0);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.OUVERTURE, event3.getType());
			assertEquals(dateAchat, event3.getDateValeur());
			assertEquals(idRFCommunaute2, event3.getDroit().getAyantDroit().getIdRF());
			assertEquals(idImmeubleRF, event3.getDroit().getImmeuble().getIdRF());

			final EvenementFiscalDroitPropriete event5 = events.get(1);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.OUVERTURE, event5.getType());
			assertEquals(dateAchat, event5.getDateValeur());
			assertEquals(idRFOdette, event5.getDroit().getAyantDroit().getIdRF());
			assertEquals(idImmeubleRF, event5.getDroit().getImmeuble().getIdRF());

			final EvenementFiscalDroitPropriete event4 = events.get(2);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.OUVERTURE, event4.getType());
			assertEquals(dateAchat, event4.getDateValeur());
			assertEquals(idRFCarlos, event4.getDroit().getAyantDroit().getIdRF());
			assertEquals(idImmeubleRF, event4.getDroit().getImmeuble().getIdRF());

			final EvenementFiscalDroitPropriete event2 = events.get(3);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.FERMETURE, event2.getType());
			assertEquals(dateAchat, event2.getDateValeur());
			assertEquals(idRFCommunaute1, event2.getDroit().getAyantDroit().getIdRF());
			assertEquals(idImmeubleRF, event2.getDroit().getImmeuble().getIdRF());

			final EvenementFiscalDroitPropriete event1 = events.get(4);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.FERMETURE, event1.getType());
			assertEquals(dateAchat, event1.getDateValeur());
			assertEquals(idRFAndres, event1.getDroit().getAyantDroit().getIdRF());
			assertEquals(idImmeubleRF, event1.getDroit().getImmeuble().getIdRF());

			final EvenementFiscalDroitPropriete event0 = events.get(5);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.FERMETURE, event0.getType());
			assertEquals(dateAchat, event0.getDateValeur());
			assertEquals(idRFOdette, event0.getDroit().getAyantDroit().getIdRF());
			assertEquals(idImmeubleRF, event0.getDroit().getImmeuble().getIdRF());

			return null;
		});
	}

	/**
	 * [SIFISC-28213] Ce test vérifie que la date de début métier d'un droit est calculée correctement dans le cas où :
	 *  <ul>
	 *      <li>on a une communauté</li>
	 *      <li>un membre de la communauté possède un droit avec des raisons d'acquisition héritées d'une première communauté</li>
	 *      <li>tous les droits de la communauté possède un raison d'acquisition "Achat"</li>
	 *      <li>un nouvel import annule la raison d'acquisition "Achat" du membre avec les raisons d'acquisition héritées</li>
	 *  </ul>
	 *  =>  à ce moment-là, il n'y a plus de <i>nouvelle</i> raison d'acquisition sur le droit et l'algorithme doit tenir compte des autres droits de la communauté pour calculer la date de début métier.
	 * <p>
	 * <b>Cas métier:</b> CH509045438381 (achat au 22.11.2016)
	 */
	@Test
	public void testProcessMutationModificationAnnulationRaisonAcquisitionEtPlusDeNouvelleRaisonAcquisitionDeterminate() throws Exception {

		final String idRFAndres = "_8af806fc40347c3701412bead24754e2";
		final String idRFOdette = "_8af806fc40347c3701412c138f785658";
		final String idRFCarlos = "_8af80e62583eaf7b0158af42818778a6";
		final String idRFCommunaute1 = "_8af806fc40347c370141502e2d5d1516";
		final String idRFCommunaute2 = "_8af8064d58e34c5a0159cf79c9b71925";
		final String idImmeubleRF = "_1f109152381059670138105e159419b0";
		final RegDate dateImportInitial = null;
		final RegDate dateSecondImport = RegDate.get(2017, 1, 28);
		final RegDate dateSuccession = RegDate.get(2013, 9, 13);
		final RegDate dateAchat = RegDate.get(2016, 11, 22);

		// précondition : il y a une communauté composée de Andrès et Odette
		final Long immeubleId = doInNewTransaction(status -> {

			CommunauteRF communaute1 = new CommunauteRF();
			communaute1.setIdRF(idRFCommunaute1);
			communaute1 = (CommunauteRF) ayantDroitRFDAO.save(communaute1);

			CommunauteRF communaute2 = new CommunauteRF();
			communaute2.setIdRF(idRFCommunaute2);
			communaute2 = (CommunauteRF) ayantDroitRFDAO.save(communaute2);

			PersonnePhysiqueRF andres = new PersonnePhysiqueRF();
			andres.setIdRF(idRFAndres);
			andres.setNom("Dub Addor");
			andres.setPrenom("Andrès");
			andres.setDateNaissance(RegDate.get(1900, 1, 1));
			andres = (PersonnePhysiqueRF) ayantDroitRFDAO.save(andres);

			PersonnePhysiqueRF odette = new PersonnePhysiqueRF();
			odette.setIdRF(idRFOdette);
			odette.setNom("Sprüngli");
			odette.setPrenom("Odette");
			odette.setDateNaissance(RegDate.get(1900, 1, 1));
			odette = (PersonnePhysiqueRF) ayantDroitRFDAO.save(odette);

			PersonnePhysiqueRF carlos = new PersonnePhysiqueRF();
			carlos.setIdRF(idRFCarlos);
			carlos.setNom("Sprüngli");
			carlos.setPrenom("Carlos");
			carlos.setDateNaissance(RegDate.get(1900, 1, 1));
			carlos = (PersonnePhysiqueRF) ayantDroitRFDAO.save(carlos);

			BienFondsRF immeuble = new BienFondsRF();
			immeuble.setIdRF(idImmeubleRF);
			immeuble = (BienFondsRF) immeubleRFDAO.save(immeuble);

			// le droit de la première communauté
			DroitProprieteCommunauteRF droit0 = new DroitProprieteCommunauteRF();
			droit0.setMasterIdRF("8af806fc40347c370141502e2d5d1516");
			droit0.setVersionIdRF("8af806fc40347c370141502e2d5d1513");
			droit0.setDateDebut(dateImportInitial);
			droit0.setDateFin(dateSecondImport.getOneDayBefore());
			droit0.setDateDebutMetier(dateSuccession);
			droit0.setDateFinMetier(dateAchat);
			droit0.setMotifDebut("Succession");
			droit0.setMotifFin("Vente");
			droit0.setAyantDroit(communaute1);
			droit0.setImmeuble(immeuble);
			droit0.setPart(new Fraction(1, 1));
			droit0.setRegime(GenrePropriete.COMMUNE);
			droitRFDAO.save(droit0);

			// le droit d'Andrès sur la première communauté
			final DroitProprietePersonnePhysiqueRF droit1 = new DroitProprietePersonnePhysiqueRF();
			droit1.setMasterIdRF("8af806fc40347c370141502e2d5e1518");
			droit1.setVersionIdRF("8af806fc40347c370141502e2d5d1513");
			droit1.setDateDebut(dateImportInitial);
			droit1.setDateFin(dateSecondImport.getOneDayBefore());
			droit1.setDateDebutMetier(dateSuccession);
			droit1.setDateFinMetier(dateAchat);
			droit1.setMotifDebut("Succession");
			droit1.setMotifFin("Vente");
			droit1.setAyantDroit(andres);
			droit1.setCommunaute(communaute1);
			droit1.setImmeuble(immeuble);
			droit1.setPart(new Fraction(1, 1));
			droit1.setRegime(GenrePropriete.COMMUNE);
			droit1.addRaisonAcquisition(new RaisonAcquisitionRF(dateSuccession, "Succession", new IdentifiantAffaireRF(12, 2013, 4914, 0)));
			droitRFDAO.save(droit1);

			// le droit d'Odette sur la première communauté
			final DroitProprietePersonnePhysiqueRF droit2 = new DroitProprietePersonnePhysiqueRF();
			droit2.setMasterIdRF("8af806fc40347c370141502e6cef151d");
			droit2.setVersionIdRF("8af806fc40347c370141502e2d5d1513");
			droit2.setDateDebut(dateImportInitial);
			droit2.setDateFin(dateSecondImport.getOneDayBefore());
			droit2.setDateDebutMetier(dateSuccession);
			droit2.setDateFinMetier(dateAchat);
			droit2.setMotifDebut("Succession");
			droit2.setMotifFin("Vente");
			droit2.setAyantDroit(odette);
			droit2.setCommunaute(communaute1);
			droit2.setImmeuble(immeuble);
			droit2.setPart(new Fraction(1, 1));
			droit2.setRegime(GenrePropriete.COMMUNE);
			droit2.addRaisonAcquisition(new RaisonAcquisitionRF(dateSuccession, "Succession", new IdentifiantAffaireRF(12, 2013, 4914, 0)));
			droitRFDAO.save(droit2);

			// le droit de la seconde communauté
			DroitProprieteCommunauteRF droit3 = new DroitProprieteCommunauteRF();
			droit3.setMasterIdRF("8af8064d58e34c5a0159cf79c9b71925");
			droit3.setVersionIdRF("8af8064d58e34c5a0159cf79c9b71922");
			droit3.setDateDebut(dateSecondImport);
			droit3.setDateDebutMetier(dateAchat);
			droit3.setMotifDebut("Achat");
			droit3.setAyantDroit(communaute2);
			droit3.setImmeuble(immeuble);
			droit3.setPart(new Fraction(1, 1));
			droit3.setRegime(GenrePropriete.COMMUNE);
			droitRFDAO.save(droit3);

			// le droit d'Andrès sur la seconde communauté
			final DroitProprietePersonnePhysiqueRF droit4 = new DroitProprietePersonnePhysiqueRF();
			droit4.setMasterIdRF("8af8064d58e34c5a0159cf79c9b71927");
			droit4.setVersionIdRF("8af8064d58e34c5a0159cf79c9b71922");
			droit4.setDateDebut(dateSecondImport);
			droit4.setDateDebutMetier(dateAchat);
			droit4.setMotifDebut("Achat");
			droit4.setAyantDroit(carlos);
			droit4.setCommunaute(communaute2);
			droit4.setImmeuble(immeuble);
			droit4.setPart(new Fraction(1, 1));
			droit4.setRegime(GenrePropriete.COMMUNE);
			droit4.addRaisonAcquisition(new RaisonAcquisitionRF(dateAchat, "Achat", new IdentifiantAffaireRF(12, 2016, 6604, 0)));
			droitRFDAO.save(droit4);

			// le droit d'Odette sur la seconde communauté
			final DroitProprietePersonnePhysiqueRF droit5 = new DroitProprietePersonnePhysiqueRF();
			droit5.setMasterIdRF("8af8064d58e34c5a0159cf79eaf2192d");
			droit5.setVersionIdRF("8af8064d58e34c5a0159cf79c9b71922");
			droit5.setDateDebut(dateSecondImport);
			droit5.setDateDebutMetier(dateAchat);
			droit5.setMotifDebut("Achat");
			droit5.setAyantDroit(odette);
			droit5.setCommunaute(communaute2);
			droit5.setImmeuble(immeuble);
			droit5.setPart(new Fraction(1, 1));
			droit5.setRegime(GenrePropriete.COMMUNE);
			droit5.addRaisonAcquisition(new RaisonAcquisitionRF(dateSuccession, "Succession", new IdentifiantAffaireRF(12, 2013, 4914, 0)));
			droit5.addRaisonAcquisition(new RaisonAcquisitionRF(dateAchat, "Achat", new IdentifiantAffaireRF(12, 2016, 6604, 0)));
			droitRFDAO.save(droit5);

			return immeuble.getId();
		});

		// fichier de mutation où la raison d'acquisition "Achat" sur le droit d'Odette dans la deuxième communauté est annulé
		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/processor/mutation_droit_arzier_le_muid_363_20171205.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");
		final Long mutationId = insertMutation(xml, dateSecondImport, TypeEntiteRF.DROIT, TypeMutationRF.MODIFICATION, idImmeubleRF, null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée et :
		//  - la raison d'acquisition "Achat" sur le droit d'Odette dans la deuxième communauté est annulé
		//  - les dates de début métier du droit d'Odette doit rester changer au 22.11.2016 (parce qu'on utilise les autres droits de la communauté)
		doInNewTransaction(status -> {

			final ImmeubleRF immeuble = immeubleRFDAO.get(immeubleId);
			assertNotNull(immeuble);

			final List<DroitProprieteRF> droits = new ArrayList<>(immeuble.getDroitsPropriete());
			assertEquals(6, droits.size());
			droits.sort(new DroitRFRangeMetierComparator());

			// le droit de la première communauté est inchangé
			final DroitProprieteCommunauteRF droit0 = (DroitProprieteCommunauteRF) droits.get(0);
			assertNotNull(droit0);
			assertEquals("8af806fc40347c370141502e2d5d1516", droit0.getMasterIdRF());
			assertEquals("8af806fc40347c370141502e2d5d1513", droit0.getVersionIdRF());
			assertEquals(dateImportInitial, droit0.getDateDebut());
			assertEquals(dateSecondImport.getOneDayBefore(), droit0.getDateFin());
			assertEquals(dateSuccession, droit0.getDateDebutMetier());
			assertEquals(dateAchat, droit0.getDateFinMetier());
			assertEquals("Succession", droit0.getMotifDebut());
			assertEquals("Vente", droit0.getMotifFin());
			assertEquals(idRFCommunaute1, droit0.getAyantDroit().getIdRF());
			assertEquals(idImmeubleRF, droit0.getImmeuble().getIdRF());
			assertEquals(new Fraction(1, 1), droit0.getPart());
			assertEquals(GenrePropriete.COMMUNE, droit0.getRegime());

			// le droit d'Andrès est inchangé
			final DroitProprietePersonnePhysiqueRF droit1 = (DroitProprietePersonnePhysiqueRF) droits.get(1);
			assertNotNull(droit1);
			assertEquals("8af806fc40347c370141502e2d5e1518", droit1.getMasterIdRF());
			assertEquals("8af806fc40347c370141502e2d5d1513", droit1.getVersionIdRF());
			assertEquals(dateImportInitial, droit1.getDateDebut());
			assertEquals(dateSecondImport.getOneDayBefore(), droit1.getDateFin());
			assertEquals(dateSuccession, droit1.getDateDebutMetier());
			assertEquals(dateAchat, droit1.getDateFinMetier());
			assertEquals("Succession", droit1.getMotifDebut());
			assertEquals("Vente", droit1.getMotifFin());
			assertEquals(idRFAndres, droit1.getAyantDroit().getIdRF());
			assertEquals(idImmeubleRF, droit1.getImmeuble().getIdRF());
			assertEquals(idRFCommunaute1, droit1.getCommunaute().getIdRF());
			assertEquals(new Fraction(1, 1), droit1.getPart());
			assertEquals(GenrePropriete.COMMUNE, droit1.getRegime());

			// le droit d'Odette est inchangé
			final DroitProprietePersonnePhysiqueRF droit2 = (DroitProprietePersonnePhysiqueRF) droits.get(2);
			assertNotNull(droit2);
			assertEquals("8af806fc40347c370141502e6cef151d", droit2.getMasterIdRF());
			assertEquals("8af806fc40347c370141502e2d5d1513", droit2.getVersionIdRF());
			assertEquals(dateImportInitial, droit2.getDateDebut());
			assertEquals(dateSecondImport.getOneDayBefore(), droit2.getDateFin());
			assertEquals(dateSuccession, droit2.getDateDebutMetier());
			assertEquals(dateAchat, droit2.getDateFinMetier());
			assertEquals("Succession", droit2.getMotifDebut());
			assertEquals("Vente", droit2.getMotifFin());
			assertEquals(idRFOdette, droit2.getAyantDroit().getIdRF());
			assertEquals(idImmeubleRF, droit2.getImmeuble().getIdRF());
			assertEquals(idRFCommunaute1, droit2.getCommunaute().getIdRF());
			assertEquals(new Fraction(1, 1), droit2.getPart());
			assertEquals(GenrePropriete.COMMUNE, droit2.getRegime());

			// le droit de la seconde communauté est inchangé
			final DroitProprieteCommunauteRF droit3 = (DroitProprieteCommunauteRF) droits.get(3);
			assertNotNull(droit3);
			assertEquals("8af8064d58e34c5a0159cf79c9b71925", droit3.getMasterIdRF());
			assertEquals("8af8064d58e34c5a0159cf79c9b71922", droit3.getVersionIdRF());
			assertEquals(dateSecondImport, droit3.getDateDebut());
			assertNull(droit3.getDateFin());
			assertEquals(dateAchat, droit3.getDateDebutMetier());
			assertNull(droit3.getDateFinMetier());
			assertEquals("Achat", droit3.getMotifDebut());
			assertNull(droit3.getMotifFin());
			assertEquals(idRFCommunaute2, droit3.getAyantDroit().getIdRF());
			assertEquals(idImmeubleRF, droit3.getImmeuble().getIdRF());
			assertEquals(new Fraction(1, 1), droit3.getPart());
			assertEquals(GenrePropriete.COMMUNE, droit3.getRegime());

			// la raison d'acquisition "Achat" du droit d'Odette doit être annulée mais le reste du droit doit être inchangé
			final DroitProprietePersonnePhysiqueRF droit4 = (DroitProprietePersonnePhysiqueRF) droits.get(4);
			assertNotNull(droit4);
			assertEquals("8af8064d58e34c5a0159cf79eaf2192d", droit4.getMasterIdRF());
			assertEquals("8af8064d58e34c5a0159cf79c9b71922", droit4.getVersionIdRF());
			assertEquals(dateSecondImport, droit4.getDateDebut());
			assertNull(droit4.getDateFin());
			assertEquals(dateAchat, droit4.getDateDebutMetier());
			assertNull(droit4.getDateFinMetier());
			assertEquals("Achat", droit4.getMotifDebut());
			assertNull(droit4.getMotifFin());
			assertEquals(idRFOdette, droit4.getAyantDroit().getIdRF());
			assertEquals(idRFCommunaute2, droit4.getCommunaute().getIdRF());
			assertEquals(idImmeubleRF, droit4.getImmeuble().getIdRF());
			assertEquals(new Fraction(1, 1), droit4.getPart());
			assertEquals(GenrePropriete.COMMUNE, droit4.getRegime());

			final List<RaisonAcquisitionRF> raisonsAcquisitionOdette = new ArrayList<>(droit4.getRaisonsAcquisition());
			assertEquals(2, raisonsAcquisitionOdette.size());
			raisonsAcquisitionOdette.sort(Comparator.comparing(RaisonAcquisitionRF::getDateAcquisition));
			final RaisonAcquisitionRF raison0 = raisonsAcquisitionOdette.get(0);
			assertNotNull(raison0);
			assertNull(raison0.getAnnulationDate()); // <--- la raison d'acquisition "Succession" est toujours valide
			assertEquals(dateSuccession, raison0.getDateAcquisition());
			assertEquals("Succession", raison0.getMotifAcquisition());
			final RaisonAcquisitionRF raison1 = raisonsAcquisitionOdette.get(1);
			assertNotNull(raison1);
			assertNotNull(raison1.getAnnulationDate()); // <--- la raison d'acquisition "Achat" est annulée
			assertEquals(dateAchat, raison1.getDateAcquisition());
			assertEquals("Achat", raison1.getMotifAcquisition());

			// le droit de Carlos est inchangé
			final DroitProprietePersonnePhysiqueRF droit5 = (DroitProprietePersonnePhysiqueRF) droits.get(5);
			assertNotNull(droit5);
			assertEquals("8af8064d58e34c5a0159cf79c9b71927", droit5.getMasterIdRF());
			assertEquals("8af8064d58e34c5a0159cf79c9b71922", droit5.getVersionIdRF());
			assertEquals(dateSecondImport, droit5.getDateDebut());
			assertNull(droit5.getDateFin());
			assertEquals(dateAchat, droit5.getDateDebutMetier());
			assertNull(droit5.getDateFinMetier());
			assertEquals("Achat", droit5.getMotifDebut());
			assertNull(droit5.getMotifFin());
			assertEquals(idRFCarlos, droit5.getAyantDroit().getIdRF());
			assertEquals(idRFCommunaute2, droit5.getCommunaute().getIdRF());
			assertEquals(idImmeubleRF, droit5.getImmeuble().getIdRF());
			assertEquals(new Fraction(1, 1), droit5.getPart());
			assertEquals(GenrePropriete.COMMUNE, droit5.getRegime());

			return null;
		});

		// postcondition : les événements fiscaux correspondants ont été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());

			// les événements de fermeture des droits
			final EvenementFiscalDroitPropriete event0 = (EvenementFiscalDroitPropriete) events.get(0);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.MODIFICATION, event0.getType());
			assertEquals(dateAchat, event0.getDateValeur());    // <-- c'est bien toujours la date d'achat qui est considérée
			assertEquals(idRFOdette, event0.getDroit().getAyantDroit().getIdRF());
			assertEquals(idImmeubleRF, event0.getDroit().getImmeuble().getIdRF());

			return null;
		});
	}
}