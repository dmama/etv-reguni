package ch.vd.unireg.registrefoncier.dataimport.processor;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.junit.Test;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.evenement.fiscal.registrefoncier.EvenementFiscalDroit;
import ch.vd.unireg.evenement.fiscal.registrefoncier.EvenementFiscalServitude;
import ch.vd.unireg.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.unireg.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.unireg.evenement.registrefoncier.TypeMutationRF;
import ch.vd.unireg.registrefoncier.AyantDroitRF;
import ch.vd.unireg.registrefoncier.BeneficeServitudeRF;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.ChargeServitudeRF;
import ch.vd.unireg.registrefoncier.IdentifiantAffaireRF;
import ch.vd.unireg.registrefoncier.IdentifiantDroitRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.PersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.ServitudeRF;
import ch.vd.unireg.registrefoncier.UsufruitRF;
import ch.vd.unireg.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.unireg.registrefoncier.dao.DroitRFDAO;
import ch.vd.unireg.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.unireg.registrefoncier.dao.ServitudeRFDAO;
import ch.vd.unireg.registrefoncier.dataimport.MutationsRFProcessorResults;
import ch.vd.unireg.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.unireg.registrefoncier.processor.MutationRFProcessorTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

@SuppressWarnings("Duplicates")
public class ServitudeRFProcessorTest extends MutationRFProcessorTestCase {

	private AyantDroitRFDAO ayantDroitRFDAO;
	private ImmeubleRFDAO immeubleRFDAO;
	private EvenementRFMutationDAO evenementRFMutationDAO;
	private EvenementFiscalDAO evenementFiscalDAO;
	private DroitRFDAO droitRFDAO;
	private ServitudeRFDAO servitudeRFDAO;
	private ServitudeRFProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		this.ayantDroitRFDAO = getBean(AyantDroitRFDAO.class, "ayantDroitRFDAO");
		this.immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		this.evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");
		this.evenementFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
		this.droitRFDAO = getBean(DroitRFDAO.class, "droitRFDAO");
		this.servitudeRFDAO = getBean(ServitudeRFDAO.class, "servitudeRFDAO");
		final XmlHelperRF xmlHelperRF = getBean(XmlHelperRF.class, "xmlHelperRF");
		final EvenementFiscalService evenementFiscalService = getBean(EvenementFiscalService.class, "evenementFiscalService");

		this.processor = new ServitudeRFProcessor(ayantDroitRFDAO, immeubleRFDAO, servitudeRFDAO, xmlHelperRF, evenementFiscalService);
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
	 * Ce test vérifie que le processing d'une mutation de création crée bien des nouvelles servitudes.
	 */
	@Test
	public void testProcessMutationCreation() throws Exception {

		final RegDate dateImport = RegDate.get(2016, 10, 1);
		final RegDate dateDebutMetier = RegDate.get(2002, 9, 2);
		final RegDate dateFinMetier = RegDate.get(2111, 2, 23);

		// précondition : la base est vide
		doInNewTransaction(status -> {
			assertEquals(0, droitRFDAO.getAll().size());
			assertEquals(0, evenementFiscalDAO.getAll().size());
			return null;
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/processor/mutation_servitude_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère quelques données satellites
		final Long ppId0 = insertPP("_1f109152380ffd8901380ffdabcc2441", "Gaillard", "Roger", RegDate.get(1900, 1, 1));
		final Long ppId1 = insertPP("_1f109152380ffd8901380ffda8131c65", "Lassueur", "Anne-Lise", RegDate.get(1900, 1, 1));
		insertImmeuble("_1f109152380ffd8901380ffe15bb729c");
		insertImmeuble("_1f109152381037590138103b6f6e3cfc");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateImport, TypeEntiteRF.SERVITUDE, TypeMutationRF.CREATION, "1f109152380ffd8901380ffed6694392", "1f109152380ffd8901380ffed66943a2");

		final MutationsRFProcessorResults results = new MutationsRFProcessorResults(0, true, dateImport, 1, evenementRFMutationDAO);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, true, results);
			return null;
		});

		// postcondition : la mutation est traitée et les nouvelles servitudes sont créés
		doInNewTransaction(status -> {

			// on vérifie que la servitude est bien exposée sur la première personne
			final PersonnePhysiqueRF pp0 = (PersonnePhysiqueRF) ayantDroitRFDAO.get(ppId0);
			assertNotNull(pp0);
			{
				final Set<BeneficeServitudeRF> benefices = pp0.getBeneficesServitudes();
				assertNotNull(benefices);
				assertEquals(1, benefices.size());

				final BeneficeServitudeRF benefice0 = benefices.iterator().next();
				assertBeneficeServitude(dateDebutMetier, dateFinMetier, pp0.getIdRF(), benefice0);

				final UsufruitRF usufruit0 = (UsufruitRF) benefice0.getServitude();
				assertNotNull(usufruit0);
				assertEquals("1f109152380ffd8901380ffed6694392", usufruit0.getMasterIdRF());
				assertNull(usufruit0.getDateDebut());
				assertNull(usufruit0.getDateFin());
				assertNull(usufruit0.getMotifDebut());
				assertEquals(dateDebutMetier, usufruit0.getDateDebutMetier());
				assertEquals(dateFinMetier, usufruit0.getDateFinMetier());
				assertEquals(new IdentifiantAffaireRF(8, 2002, 392, null), usufruit0.getNumeroAffaire());
				assertEquals(new IdentifiantDroitRF(8, 2005, 699), usufruit0.getIdentifiantDroit());

				final List<ChargeServitudeRF> charges = new ArrayList<>(usufruit0.getCharges());
				assertEquals(2, charges.size());
				charges.sort(Comparator.comparing(lien -> lien.getImmeuble().getIdRF()));
				assertChargeServitude(dateDebutMetier, dateFinMetier, "_1f109152380ffd8901380ffe15bb729c", charges.get(0));
				assertChargeServitude(dateDebutMetier, dateFinMetier, "_1f109152381037590138103b6f6e3cfc", charges.get(1));
			}

			// on vérifie que la servitude est bien exposée sur la seconde personne
			final PersonnePhysiqueRF pp1 = (PersonnePhysiqueRF) ayantDroitRFDAO.get(ppId1);
			assertNotNull(pp1);
			{
				final Set<BeneficeServitudeRF> benefices = pp1.getBeneficesServitudes();
				assertNotNull(benefices);
				assertEquals(1, benefices.size());

				final BeneficeServitudeRF benefice0 = benefices.iterator().next();
				assertBeneficeServitude(dateDebutMetier, dateFinMetier, pp1.getIdRF(), benefice0);

				final UsufruitRF usufruit1 = (UsufruitRF) benefice0.getServitude();
				assertNotNull(usufruit1);
				assertEquals("1f109152380ffd8901380ffed6694392", usufruit1.getMasterIdRF());
			}
			return null;
		});

		// postcondition : les événements fiscaux correspondants ont été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());
			events.sort(Comparator.comparing(EvenementFiscal::getDateValeur));

			final EvenementFiscalServitude event0 = (EvenementFiscalServitude) events.get(0);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.OUVERTURE, event0.getType());
			assertEquals(dateDebutMetier, event0.getDateValeur());

			final ServitudeRF servitude = event0.getServitude();
			final Set<String> immeubles = servitude.getCharges().stream()
					.map(ChargeServitudeRF::getImmeuble)
					.map(ImmeubleRF::getIdRF)
					.collect(Collectors.toSet());
			assertEquals(new HashSet<>(Arrays.asList("_1f109152380ffd8901380ffe15bb729c", "_1f109152381037590138103b6f6e3cfc")), immeubles);

			final Set<String> ayantDroits = servitude.getBenefices().stream()
					.map(BeneficeServitudeRF::getAyantDroit)
					.map(AyantDroitRF::getIdRF)
					.collect(Collectors.toSet());
			assertEquals(new HashSet<>(Arrays.asList("_1f109152380ffd8901380ffdabcc2441", "_1f109152380ffd8901380ffda8131c65")), ayantDroits);

			return null;
		});

		// [SIFISC-24511] on vérifie que le rapport contient bien des mutations de type SERVITUDE
		assertEquals(0, results.getNbErreurs());
		final Map<MutationsRFProcessorResults.ProcessedKey, MutableLong> processed = results.getProcessed();
		assertEquals(1, processed.size());
		final MutationsRFProcessorResults.ProcessedKey key0 = processed.keySet().iterator().next();
		assertEquals(TypeEntiteRF.SERVITUDE, key0.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, key0.getTypeMutation());
	}

	/*
	 * Ce test vérifie que le processing d'une mutation de modification de droits fonctionne bien quand il y a un ayant-droit et un immeuble en plus.
	 */
	@Test
	public void testProcessMutationModificationElementsEnPlus() throws Exception {

		final String idPPRF1 = "_1f109152380ffd8901380ffdabcc2441";
		final String idPPRF2 = "_1f109152380ffd8901380ffda8131c65";
		final String idImmeubleRF1 = "_1f109152380ffd8901380ffe15bb729c";
		final String idImmeubleRF2 = "_1f109152381037590138103b6f6e3cfc";
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);
		final RegDate dateDebutMetier = RegDate.get(2002, 9, 2);
		final RegDate dateFinMetier = RegDate.get(2111, 2, 23);

		// précondition : il y a déjà un usufruit avec un ayant-droit et un immeuble dans la base de données
		final Long usuId = doInNewTransaction(status -> {

			PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
			pp1.setIdRF(idPPRF1);
			pp1.setNom("Gaillard");
			pp1.setPrenom("Roger");
			pp1.setDateNaissance(RegDate.get(1900, 1, 1));
			pp1 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp1);

			PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
			pp2.setIdRF(idPPRF2);
			pp2.setNom("Lassueur");
			pp2.setPrenom("Anne-Lise");
			pp2.setDateNaissance(RegDate.get(1900, 1, 1));
			ayantDroitRFDAO.save(pp2);

			BienFondsRF immeuble1 = new BienFondsRF();
			immeuble1.setIdRF(idImmeubleRF1);
			immeuble1 = (BienFondsRF) immeubleRFDAO.save(immeuble1);

			BienFondsRF immeuble2 = new BienFondsRF();
			immeuble2.setIdRF(idImmeubleRF2);
			immeubleRFDAO.save(immeuble2);

			// un usufruit avec seulement un ayant-droit et un immeuble
			UsufruitRF usu = new UsufruitRF();
			usu.setMasterIdRF("1f109152380ffd8901380ffed6694392");
			usu.setVersionIdRF("1f109152380ffd8901380ffed66943a2");
			usu.setDateDebut(null);
			usu.setMotifDebut(null);
			usu.setDateDebutMetier(dateDebutMetier);
			usu.setDateFinMetier(dateFinMetier);
			usu.setNumeroAffaire(new IdentifiantAffaireRF(8, "2002/392"));
			usu.setIdentifiantDroit(new IdentifiantDroitRF(8, 2005, 699));
			usu.addBenefice(new BeneficeServitudeRF(dateDebutMetier, dateFinMetier, null, pp1));
			usu.addCharge(new ChargeServitudeRF(dateDebutMetier, dateFinMetier, null, immeuble1));
			usu = (UsufruitRF) droitRFDAO.save(usu);

			return usu.getId();
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/processor/mutation_servitude_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateSecondImport, TypeEntiteRF.SERVITUDE, TypeMutationRF.MODIFICATION, "1f109152380ffd8901380ffed6694392", "1f109152380ffd8901380ffed66943a2");

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée et :
		//  - un nouvel ayant-droit est ajouté
		//  - un nouvel immeuble est ajouté
		doInNewTransaction(status -> {

			final UsufruitRF usu = (UsufruitRF) droitRFDAO.get(usuId);
			assertNotNull(usu);

			// les propriétés de base sont inchangées
			assertEquals("1f109152380ffd8901380ffed6694392", usu.getMasterIdRF());
			assertEquals("1f109152380ffd8901380ffed66943a2", usu.getVersionIdRF());
			assertNull(usu.getDateDebut());
			assertNull(usu.getDateFin());
			assertNull(usu.getMotifDebut());
			assertEquals(dateDebutMetier, usu.getDateDebutMetier());
			assertEquals(dateFinMetier, usu.getDateFinMetier());
			assertEquals(new IdentifiantAffaireRF(8, "2002/392"), usu.getNumeroAffaire());
			assertEquals(new IdentifiantDroitRF(8, 2005, 699), usu.getIdentifiantDroit());

			// il y a un ayant-droit de plus
			final List<BeneficeServitudeRF> benefices = new ArrayList<>(usu.getBenefices());
			assertEquals(2, benefices.size());
			benefices.sort(new DateRangeComparator<>());
			assertBeneficeServitude(dateDebutMetier, dateFinMetier, idPPRF1, benefices.get(0));
			assertBeneficeServitude(dateSecondImport, dateFinMetier, idPPRF2, benefices.get(1));

			// il y a un immeuble de plus
			final List<ChargeServitudeRF> charges = new ArrayList<>(usu.getCharges());
			assertEquals(2, charges.size());
			charges.sort(new DateRangeComparator<>());
			assertChargeServitude(dateDebutMetier, dateFinMetier, idImmeubleRF1, charges.get(0));
			assertChargeServitude(dateSecondImport, dateFinMetier, idImmeubleRF2, charges.get(1));

			return null;
		});
	}
	
	/*
	 * Ce test vérifie que le processing d'une mutation de modification de droits fonctionne bien quand il y a un ayant-droit et un immeuble en moins.
	 */
	@Test
	public void testProcessMutationModificationElementsEnMoins() throws Exception {

		final String idPPRF1 = "_1f109152380ffd8901380ffda8131c65";
		final String idPPRF2 = "_1f109152380ffd8901380ffdabcc2441";
		final String idPPRF3 = "_34893489438934";
		final String idImmeubleRF1 = "_1f109152380ffd8901380ffe15bb729c";
		final String idImmeubleRF2 = "_1f109152381037590138103b6f6e3cfc";
		final String idImmeubleRF3 = "_3e2392390390";
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		// précondition : il y a déjà un usufruit avec trois ayants-droits et trois immeubles dans la base de données
		final Long usuId = doInNewTransaction(status -> {

			PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
			pp1.setIdRF(idPPRF1);
			pp1.setNom("Lassueur");
			pp1.setPrenom("Anne-Lise");
			pp1.setDateNaissance(RegDate.get(1900, 1, 1));
			pp1 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp1);

			PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
			pp2.setIdRF(idPPRF2);
			pp2.setNom("Gaillard");
			pp2.setPrenom("Roger");
			pp2.setDateNaissance(RegDate.get(1900, 1, 1));
			pp2 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp2);

			PersonnePhysiqueRF pp3 = new PersonnePhysiqueRF();
			pp3.setIdRF(idPPRF3);
			pp3.setNom("Muche");
			pp3.setPrenom("Truc");
			pp3.setDateNaissance(RegDate.get(1900, 1, 1));
			pp3 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp3);

			BienFondsRF immeuble1 = new BienFondsRF();
			immeuble1.setIdRF(idImmeubleRF1);
			immeuble1 = (BienFondsRF) immeubleRFDAO.save(immeuble1);

			BienFondsRF immeuble2 = new BienFondsRF();
			immeuble2.setIdRF(idImmeubleRF2);
			immeuble2 = (BienFondsRF) immeubleRFDAO.save(immeuble2);

			BienFondsRF immeuble3 = new BienFondsRF();
			immeuble3.setIdRF(idImmeubleRF3);
			immeuble3 = (BienFondsRF) immeubleRFDAO.save(immeuble3);

			// un usufruit avec trois ayants-droit et trois immeubles (au lieu de 2 * 2 dans le XML)
			UsufruitRF usu = new UsufruitRF();
			usu.setMasterIdRF("1f109152380ffd8901380ffed6694392");
			usu.setVersionIdRF("1f109152380ffd8901380ffed66943a2");
			usu.setDateDebut(null);
			usu.setMotifDebut(null);
			usu.setDateDebutMetier(RegDate.get(2002, 9, 2));
			usu.setDateFinMetier(RegDate.get(2111, 2, 23));
			usu.setNumeroAffaire(new IdentifiantAffaireRF(8, "2002/392"));
			usu.setIdentifiantDroit(new IdentifiantDroitRF(8, 2005, 699));
			usu.addBenefice(new BeneficeServitudeRF(null, null, null, pp1));
			usu.addBenefice(new BeneficeServitudeRF(null, null, null, pp2));
			usu.addBenefice(new BeneficeServitudeRF(null, null, null, pp3));
			usu.addCharge(new ChargeServitudeRF(null, null, null, immeuble1));
			usu.addCharge(new ChargeServitudeRF(null, null, null, immeuble2));
			usu.addCharge(new ChargeServitudeRF(null, null, null, immeuble3));
			usu = (UsufruitRF) droitRFDAO.save(usu);

			return usu.getId();
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/processor/mutation_servitude_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateSecondImport, TypeEntiteRF.SERVITUDE, TypeMutationRF.MODIFICATION, "1f109152380ffd8901380ffed6694392", "1f109152380ffd8901380ffed66943a2");

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée et :
		//  - l'ayant-droit en trop a été supprimé
		//  - l'immeuble en trop a été supprimé
		doInNewTransaction(status -> {

			final UsufruitRF usu = (UsufruitRF) droitRFDAO.get(usuId);
			assertNotNull(usu);

			// les propriétés de base sont inchangées
			assertEquals("1f109152380ffd8901380ffed6694392", usu.getMasterIdRF());
			assertEquals("1f109152380ffd8901380ffed66943a2", usu.getVersionIdRF());
			assertNull(usu.getDateDebut());
			assertNull(usu.getDateFin());
			assertNull(usu.getMotifDebut());
			assertEquals(RegDate.get(2002, 9, 2), usu.getDateDebutMetier());
			assertEquals(RegDate.get(2111, 2, 23), usu.getDateFinMetier());
			assertEquals(new IdentifiantAffaireRF(8, "2002/392"), usu.getNumeroAffaire());
			assertEquals(new IdentifiantDroitRF(8, 2005, 699), usu.getIdentifiantDroit());

			// le lien vers l'ayant-droit qui n'existe plus a été fermé
			final List<BeneficeServitudeRF> benefices = new ArrayList<>(usu.getBenefices());
			assertEquals(3, benefices.size());
			benefices.sort(Comparator.comparing(b -> b.getAyantDroit().getIdRF()));
			assertBeneficeServitude(null, null, idPPRF1, benefices.get(0));
			assertBeneficeServitude(null, null, idPPRF2, benefices.get(1));
			assertBeneficeServitude(null, dateSecondImport.getOneDayBefore(), idPPRF3, benefices.get(2));

			// le lien vers l'immeuble qui n'existe plus a été fermé
			final List<ChargeServitudeRF> charges = new ArrayList<>(usu.getCharges());
			assertEquals(3, charges.size());
			charges.sort(Comparator.comparing(c -> c.getImmeuble().getIdRF()));
			assertChargeServitude(null, null, idImmeubleRF1, charges.get(0));
			assertChargeServitude(null, null, idImmeubleRF2, charges.get(1));
			assertChargeServitude(null, dateSecondImport.getOneDayBefore(), idImmeubleRF3, charges.get(2));

			return null;
		});
	}

	/*
	 * Ce test vérifie que le processing d'une mutation de suppression fonctionne bien.
	 */
	@Test
	public void testProcessMutationSuppression() throws Exception {

		final String idPPRF1 = "_1f1091523810375901381037f42e3142";
		final String idPPRF2 = "_1f109152381037590138103835995c0d";
		final String idImmeubleRF1 = "_1f109152381037590138103b6f6e3cfa";
		final String idImmeubleRF2 = "_1f109152381037590138103b6f6e3cfc";
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		// précondition : il y a déjà deux usufruits dans la base de données
		final Long ppId = doInNewTransaction(status -> {

			PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
			pp1.setIdRF(idPPRF1);
			pp1.setNom("Porchet");
			pp1.setPrenom("Jean-Jacques");
			pp1.setDateNaissance(RegDate.get(1900, 1, 1));
			pp1 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp1);

			PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
			pp2.setIdRF(idPPRF2);
			pp2.setNom("Porchet");
			pp2.setPrenom("Jeanne");
			pp2.setDateNaissance(RegDate.get(1900, 1, 1));
			pp2 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp2);

			BienFondsRF immeuble1 = new BienFondsRF();
			immeuble1.setIdRF(idImmeubleRF1);
			immeuble1 = (BienFondsRF) immeubleRFDAO.save(immeuble1);

			BienFondsRF immeuble2 = new BienFondsRF();
			immeuble2.setIdRF(idImmeubleRF2);
			immeuble2 = (BienFondsRF) immeubleRFDAO.save(immeuble2);

			final UsufruitRF usu0 = new UsufruitRF();
			usu0.setMasterIdRF("1f1091523810375901381044fa823515");
			usu0.setVersionIdRF("1f1091523810375901381044fa823514");
			usu0.setDateDebut(null);
			usu0.setMotifDebut(null);
			usu0.setDateDebutMetier(RegDate.get(2010, 3, 8));
			usu0.setNumeroAffaire(new IdentifiantAffaireRF(5, 2010, 731, 0));
			usu0.setIdentifiantDroit(new IdentifiantDroitRF(5, 2010, 432));
			usu0.addBenefice(new BeneficeServitudeRF(null, null, null, pp1));
			usu0.addBenefice(new BeneficeServitudeRF(null, null, null, pp2));
			usu0.addCharge(new ChargeServitudeRF(null, null, null, immeuble1));
			usu0.addCharge(new ChargeServitudeRF(null, null, null, immeuble2));
			droitRFDAO.save(usu0);

			return pp1.getId();
		});

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(null, dateSecondImport, TypeEntiteRF.SERVITUDE, TypeMutationRF.SUPPRESSION, "1f1091523810375901381044fa823515", "1f1091523810375901381044fa823514");

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée et tous les usufruits existants sont fermés
		doInNewTransaction(status -> {

			final PersonnePhysiqueRF pp = (PersonnePhysiqueRF) ayantDroitRFDAO.get(ppId);
			assertNotNull(pp);

			// le lien entre l'ayant-droit et la servitude doit être fermé
			final Set<BeneficeServitudeRF> benefices = pp.getBeneficesServitudes();
			assertEquals(1, benefices.size());
			final BeneficeServitudeRF benefice0 = benefices.iterator().next();
			assertBeneficeServitude(null, dateSecondImport.getOneDayBefore(), idPPRF1, benefice0);

			// le usu0 doit être fermé
			final UsufruitRF usufruit0 = (UsufruitRF) benefice0.getServitude();
			assertNotNull(usufruit0);
			assertEquals("1f1091523810375901381044fa823515", usufruit0.getMasterIdRF());
			assertEquals("1f1091523810375901381044fa823514", usufruit0.getVersionIdRF());
			assertNull(usufruit0.getDateDebut());
			assertEquals(dateSecondImport.getOneDayBefore(), usufruit0.getDateFin());
			assertNull(usufruit0.getMotifDebut());
			assertEquals(RegDate.get(2010, 3, 8), usufruit0.getDateDebutMetier());
			// la date de fin métier doit être renseignée car la servitude est maintenant fermée
			assertEquals(dateSecondImport.getOneDayBefore(), usufruit0.getDateFinMetier());
			assertEquals(new IdentifiantAffaireRF(5, 2010, 731, 0), usufruit0.getNumeroAffaire());
			assertEquals(new IdentifiantDroitRF(5, 2010, 432), usufruit0.getIdentifiantDroit());

			// les lien entre la servitude et les immeubles doivent être fermés
			final List<ChargeServitudeRF> charges = new ArrayList<>(usufruit0.getCharges());
			assertEquals(2, charges.size());
			charges.sort(Comparator.comparing(c -> c.getImmeuble().getIdRF()));
			assertChargeServitude(null, dateSecondImport.getOneDayBefore(), idImmeubleRF1, charges.get(0));
			assertChargeServitude(null, dateSecondImport.getOneDayBefore(), idImmeubleRF2, charges.get(1));
			return null;
		});

		// postcondition : les événements fiscaux correspondants ont été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());
			events.sort(Comparator.comparing(EvenementFiscal::getDateValeur));

			final EvenementFiscalServitude event0 = (EvenementFiscalServitude) events.get(0);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.FERMETURE, event0.getType());
			assertEquals(dateSecondImport.getOneDayBefore(), event0.getDateValeur());

			final ServitudeRF servitude = event0.getServitude();
			final Set<String> immeubles = servitude.getCharges().stream()
					.map(ChargeServitudeRF::getImmeuble)
					.map(ImmeubleRF::getIdRF)
					.collect(Collectors.toSet());
			assertEquals(new HashSet<>(Arrays.asList("_1f109152381037590138103b6f6e3cfa", "_1f109152381037590138103b6f6e3cfc")), immeubles);

			final Set<String> ayantDroits = servitude.getBenefices().stream()
					.map(BeneficeServitudeRF::getAyantDroit)
					.map(AyantDroitRF::getIdRF)
					.collect(Collectors.toSet());
			assertEquals(new HashSet<>(Arrays.asList("_1f1091523810375901381037f42e3142", "_1f109152381037590138103835995c0d")), ayantDroits);

			return null;
		});
	}

	private static void assertBeneficeServitude(RegDate dateDebut, RegDate dateFin, String idRFAyantDroit, BeneficeServitudeRF benefice) {
		assertNotNull(benefice);
		assertEquals(dateDebut, benefice.getDateDebut());
		assertEquals(dateFin, benefice.getDateFin());
		assertEquals(idRFAyantDroit, benefice.getAyantDroit().getIdRF());
	}

	private static void assertChargeServitude(RegDate dateDebut, RegDate dateFin, String idRFImmeuble, ChargeServitudeRF lien) {
		assertNotNull(lien);
		assertEquals(dateDebut, lien.getDateDebut());
		assertEquals(dateFin, lien.getDateFin());
		assertEquals(idRFImmeuble, lien.getImmeuble().getIdRF());
	}
}