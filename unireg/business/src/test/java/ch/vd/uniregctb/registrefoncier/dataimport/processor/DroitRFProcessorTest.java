package ch.vd.uniregctb.registrefoncier.dataimport.processor;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.uniregctb.evenement.registrefoncier.TypeMutationRF;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
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
	private DroitRFDAO droitRFDAO;
	private DroitRFProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		this.ayantDroitRFDAO = getBean(AyantDroitRFDAO.class, "ayantDroitRFDAO");
		this.immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		this.evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");
		this.droitRFDAO = getBean(DroitRFDAO.class, "droitRFDAO");
		final XmlHelperRF xmlHelperRF = getBean(XmlHelperRF.class, "xmlHelperRF");

		this.processor = new DroitRFProcessor(ayantDroitRFDAO, immeubleRFDAO, droitRFDAO, xmlHelperRF);
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
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				assertEquals(0, droitRFDAO.getAll().size());
			}
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_droit_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère quelques données satellites
		final Long ppId = insertPP("_1f109152381009be0138100a1d442eee", "Schulz", "Alodie", RegDate.get(1900, 1, 1));
		insertImmeuble("_8af806fc4a35927c014ae2a6e76041b8");
		insertImmeuble("_1f109152381009be0138100ba7e31031");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateImport, TypeEntiteRF.DROIT, TypeMutationRF.CREATION, "_1f109152381009be0138100a1d442eee");

		// on process la mutation
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
				processor.process(mutation, true, null);
			}
		});

		// postcondition : la mutation est traitée et les nouveaux droits sont créés
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

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
			}
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
		final Long ppId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
				pp.setIdRF(idPPRF);
				pp.setNom("Schulz");
				pp.setPrenom("Alodie");
				pp.setDateNaissance(RegDate.get(1900, 1, 1));
				pp = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp);

				BienFondRF immeuble1 = new BienFondRF();
				immeuble1.setIdRF(idImmeubleRF1);
				immeuble1 = (BienFondRF) immeubleRFDAO.save(immeuble1);

				BienFondRF immeuble2 = new BienFondRF();
				immeuble2.setIdRF(idImmeubleRF2);
				immeuble2 = (BienFondRF) immeubleRFDAO.save(immeuble2);

				// on droit différent de celui qui arrive dans le fichier XML
				final DroitProprietePersonnePhysiqueRF droit0 = new DroitProprietePersonnePhysiqueRF();
				droit0.setMasterIdRF("1f109152381009be0138100c87000000");
				droit0.setDateDebut(dateImportInitial);
				droit0.setAyantDroit(pp);
				droit0.setImmeuble(immeuble1);
				droit0.setPart(new Fraction(1, 1));
				droit0.setRegime(GenrePropriete.INDIVIDUELLE);
				// motif différent
				droit0.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2007, 2, 7), "Appropriation illégitime", new IdentifiantAffaireRF(13, 2007, 173, 0)));
				droit0.calculateDateEtMotifDebut();
				droitRFDAO.save(droit0);

				// on droit identique à celui qui arrive dans le fichier XML
				final DroitProprietePersonnePhysiqueRF droit1 = new DroitProprietePersonnePhysiqueRF();
				droit1.setMasterIdRF("8af806fa4a4dd302014b16fc17266a0b");
				droit1.setDateDebut(dateImportInitial);
				droit1.setAyantDroit(pp);
				droit1.setImmeuble(immeuble2);
				droit1.setPart(new Fraction(1, 2));
				droit1.setRegime(GenrePropriete.COPROPRIETE);
				droit1.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2003, 1, 1), "Succession", new IdentifiantAffaireRF(6, 2003, 9593, 0)));
				droit1.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2014, 12, 23), "Achat", new IdentifiantAffaireRF(6, 2014, 9593, 0)));
				droit1.calculateDateEtMotifDebut();

				droitRFDAO.save(droit1);

				return pp.getId();
			}
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_droit_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateSecondImport, TypeEntiteRF.DROIT, TypeMutationRF.MODIFICATION, idPPRF);

		// on process la mutation
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
				processor.process(mutation, false, null);
			}
		});

		// postcondition : la mutation est traitée et :
		//  - le droit0 est fermé
		//  - le droit1 est inchangé
		//  - un nouveau droit est créé
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

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
			}
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
		final Long ppId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
				pp.setIdRF(idPPRF);
				pp.setNom("Schulz");
				pp.setPrenom("Alodie");
				pp.setDateNaissance(RegDate.get(1900, 1, 1));
				pp = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp);

				BienFondRF immeuble1 = new BienFondRF();
				immeuble1.setIdRF(idImmeubleRF1);
				immeuble1 = (BienFondRF) immeubleRFDAO.save(immeuble1);

				BienFondRF immeuble2 = new BienFondRF();
				immeuble2.setIdRF(idImmeubleRF2);
				immeuble2 = (BienFondRF) immeubleRFDAO.save(immeuble2);

				// on droit différent de celui qui arrive dans le fichier XML
				final DroitProprietePersonnePhysiqueRF droit0 = new DroitProprietePersonnePhysiqueRF();
				droit0.setMasterIdRF("1f109152381009be0138100c87276e68");
				droit0.setDateDebut(dateImportInitial);
				droit0.setAyantDroit(pp);
				droit0.setImmeuble(immeuble1);
				droit0.setPart(new Fraction(1, 1));
				droit0.setRegime(GenrePropriete.INDIVIDUELLE);
				// une seule raison d'acquisition (il y en a deux dans le fichier)
				droit0.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2005, 1, 1), "Achat", new IdentifiantAffaireRF(13, 2005, 173, 0)));
				droit0.calculateDateEtMotifDebut();
				droitRFDAO.save(droit0);

				// on droit identique à celui qui arrive dans le fichier XML
				final DroitProprietePersonnePhysiqueRF droit1 = new DroitProprietePersonnePhysiqueRF();
				droit1.setMasterIdRF("8af806fa4a4dd302014b16fc17266a0b");
				droit1.setDateDebut(dateImportInitial);
				droit1.setAyantDroit(pp);
				droit1.setImmeuble(immeuble2);
				droit1.setPart(new Fraction(1, 2));
				droit1.setRegime(GenrePropriete.COPROPRIETE);
				droit1.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2003, 1, 1), "Succession", new IdentifiantAffaireRF(6, 2003, 9593, 0)));
				droit1.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2014, 12, 23), "Achat", new IdentifiantAffaireRF(6, 2014, 9593, 0)));
				droit1.calculateDateEtMotifDebut();

				droitRFDAO.save(droit1);

				return pp.getId();
			}
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_droit_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateSecondImport, TypeEntiteRF.DROIT, TypeMutationRF.MODIFICATION, idPPRF);

		// on process la mutation
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
				processor.process(mutation, false, null);
			}
		});

		// postcondition : la mutation est traitée et :
		//  - le droit0 est modifié
		//  - le droit1 est inchangé
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

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
			}
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
		final Long ppId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
				pp.setIdRF(idPPRF);
				pp.setNom("Schulz");
				pp.setPrenom("Alodie");
				pp.setDateNaissance(RegDate.get(1900, 1, 1));
				pp = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp);

				BienFondRF immeuble1 = new BienFondRF();
				immeuble1.setIdRF(idImmeubleRF1);
				immeuble1 = (BienFondRF) immeubleRFDAO.save(immeuble1);

				BienFondRF immeuble2 = new BienFondRF();
				immeuble2.setIdRF(idImmeubleRF2);
				immeuble2 = (BienFondRF) immeubleRFDAO.save(immeuble2);

				final DroitProprietePersonnePhysiqueRF droit0 = new DroitProprietePersonnePhysiqueRF();
				droit0.setMasterIdRF("1f109152381009be0138100c87276e68");
				droit0.setDateDebut(dateImportInitial);
				droit0.setAyantDroit(pp);
				droit0.setImmeuble(immeuble1);
				droit0.setPart(new Fraction(1, 1));
				droit0.setRegime(GenrePropriete.INDIVIDUELLE);
				droit0.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2007, 2, 7), "Donation", new IdentifiantAffaireRF(13, 2007, 173, 0)));
				droit0.calculateDateEtMotifDebut();

				droitRFDAO.save(droit0);

				final DroitProprietePersonnePhysiqueRF droit1 = new DroitProprietePersonnePhysiqueRF();
				droit1.setMasterIdRF("8af806fa4a4dd302014b16fc17266a0b");
				droit1.setDateDebut(dateImportInitial);
				droit1.setAyantDroit(pp);
				droit1.setImmeuble(immeuble2);
				droit1.setPart(new Fraction(1, 2));
				droit1.setRegime(GenrePropriete.COPROPRIETE);
				droit1.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2003, 1, 1), "Succession", new IdentifiantAffaireRF(6, 2003, 9593, 0)));
				droit1.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2014, 12, 23), "Achat", new IdentifiantAffaireRF(6, 2014, 9593, 0)));
				droit1.calculateDateEtMotifDebut();
				droitRFDAO.save(droit1);

				return pp.getId();
			}
		});

		// on envoie un fichier d'import qui ne contient aucun droit
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_droit_vide_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateSecondImport, TypeEntiteRF.DROIT, TypeMutationRF.SUPPRESSION, idPPRF);

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

				final PersonnePhysiqueRF pp = (PersonnePhysiqueRF) ayantDroitRFDAO.get(ppId);
				assertNotNull(pp);

				final Set<DroitProprieteRF> droits = pp.getDroitsPropriete();
				assertNotNull(droits);
				assertEquals(2, droits.size());

				final List<DroitRF> droitList = new ArrayList<>(droits);
				Collections.sort(droitList, (o1, o2) -> o1.getMasterIdRF().compareTo(o2.getMasterIdRF()));

				// le droit0 doit être fermé
				final DroitProprietePersonnePhysiqueRF droit0 = (DroitProprietePersonnePhysiqueRF) droitList.get(0);
				assertNotNull(droit0);
				assertEquals("1f109152381009be0138100c87276e68", droit0.getMasterIdRF());
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
			}
		});
	}
}