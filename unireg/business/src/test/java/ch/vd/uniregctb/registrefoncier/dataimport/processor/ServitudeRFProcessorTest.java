package ch.vd.uniregctb.registrefoncier.dataimport.processor;

import java.io.File;
import java.util.ArrayList;
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
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.IdentifiantDroitRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.UsufruitRF;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.DroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.dataimport.DroitRFComparator;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.processor.MutationRFProcessorTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

@SuppressWarnings("Duplicates")
public class ServitudeRFProcessorTest extends MutationRFProcessorTestCase {

	private AyantDroitRFDAO ayantDroitRFDAO;
	private ImmeubleRFDAO immeubleRFDAO;
	private EvenementRFMutationDAO evenementRFMutationDAO;
	private DroitRFDAO droitRFDAO;
	private ServitudeRFProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		this.ayantDroitRFDAO = getBean(AyantDroitRFDAO.class, "ayantDroitRFDAO");
		this.immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		this.evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");
		this.droitRFDAO = getBean(DroitRFDAO.class, "droitRFDAO");
		final XmlHelperRF xmlHelperRF = getBean(XmlHelperRF.class, "xmlHelperRF");

		this.processor = new ServitudeRFProcessor(ayantDroitRFDAO, immeubleRFDAO, droitRFDAO, xmlHelperRF);
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

		// précondition : la base est vide
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				assertEquals(0, droitRFDAO.getAll().size());
			}
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_servitude_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère quelques données satellites
		final Long ppId0 = insertPP("_1f1091523810375901381037f42e3142", "Porchet", "Jean-Jacques", RegDate.get(1900, 1, 1));
		insertPP("_1f109152381037590138103835995c0d", "Porchet", "Jeanne", RegDate.get(1900, 1, 1));
		insertCommunaute("_1f1091523810375901381044fa823515");
		insertImmeuble("_1f109152381037590138103b6f6e3cfc");
		insertImmeuble("_1f109152381037590138103b6f6e3cfa");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateImport, TypeEntiteRF.SERVITUDE, TypeMutationRF.CREATION, "_1f1091523810375901381037f42e3142");

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

				final PersonnePhysiqueRF pp0 = (PersonnePhysiqueRF) ayantDroitRFDAO.get(ppId0);
				assertNotNull(pp0);

				final Set<DroitRF> droits0 = pp0.getDroits();
				assertNotNull(droits0);
				assertEquals(2, droits0.size());

				final List<DroitRF> droitsList0 = new ArrayList<>(droits0);
				droitsList0.sort(new DroitRFComparator());

				final UsufruitRF usufruit0 = (UsufruitRF) droitsList0.get(0);
				assertNotNull(usufruit0);
				assertEquals("1f1091523810375901381044fa823515", usufruit0.getMasterIdRF());
				assertNull(usufruit0.getDateDebut());
				assertNull(usufruit0.getDateFin());
				assertNull(usufruit0.getMotifDebut());
				assertEquals(RegDate.get(2010, 3, 8), usufruit0.getDateDebutMetier());
				assertNull(usufruit0.getDateFinMetier());
				assertEquals(new IdentifiantAffaireRF(5, 2010, 731, 0), usufruit0.getNumeroAffaire());
				assertEquals(new IdentifiantDroitRF(5, 2010, 432), usufruit0.getIdentifiantDroit());
				assertEquals("_1f109152381037590138103b6f6e3cfa", usufruit0.getImmeuble().getIdRF());
				final CommunauteRF communaute0 = usufruit0.getCommunaute();
				assertNotNull(communaute0);
				assertEquals("_1f1091523810375901381044fa823515", communaute0.getIdRF());

				final UsufruitRF usufruit1 = (UsufruitRF) droitsList0.get(1);
				assertNotNull(usufruit1);
				assertEquals("1f1091523810375901381044fa823515", usufruit1.getMasterIdRF());
				assertNull(usufruit1.getDateDebut());
				assertNull(usufruit1.getDateFin());
				assertNull(usufruit1.getMotifDebut());
				assertEquals(RegDate.get(2010, 3, 8), usufruit1.getDateDebutMetier());
				assertNull(usufruit1.getDateFinMetier());
				assertEquals(new IdentifiantAffaireRF(5, 2010, 731, 0), usufruit1.getNumeroAffaire());
				assertEquals(new IdentifiantDroitRF(5, 2010, 432), usufruit1.getIdentifiantDroit());
				assertEquals("_1f109152381037590138103b6f6e3cfc", usufruit1.getImmeuble().getIdRF());
				final CommunauteRF communaute1 = usufruit1.getCommunaute();
				assertNotNull(communaute1);
				assertEquals("_1f1091523810375901381044fa823515", communaute1.getIdRF());
			}
		});
	}

	/*
	 * Ce test vérifie que le processing d'une mutation de modification de droits fonctionne bien.
	 */
	@Test
	public void testProcessMutationModification() throws Exception {

		final String idPPRF1 = "_1f1091523810375901381037f42e3142";
		final String idPPRF2 = "_1f109152381037590138103835995c0d";
		final String idCommRF = "_1f1091523810375901381044fa823515";
		final String idImmeubleRF1 = "_1f109152381037590138103b6f6e3cfa";
		final String idImmeubleRF2 = "_1f109152381037590138103b6f6e3cfc";
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		// précondition : il y a déjà deux usufruits dans la base de données
		final Long ppId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

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
				ayantDroitRFDAO.save(pp2);

				CommunauteRF comm = new CommunauteRF();
				comm.setIdRF(idCommRF);
				comm = (CommunauteRF) ayantDroitRFDAO.save(comm);

				BienFondRF immeuble1 = new BienFondRF();
				immeuble1.setIdRF(idImmeubleRF1);
				immeuble1 = (BienFondRF) immeubleRFDAO.save(immeuble1);

				BienFondRF immeuble2 = new BienFondRF();
				immeuble2.setIdRF(idImmeubleRF2);
				immeuble2 = (BienFondRF) immeubleRFDAO.save(immeuble2);

				// un usufruit différent de celui qui arrive dans le fichier XML
				final UsufruitRF usu0 = new UsufruitRF();
				usu0.setMasterIdRF("1f1091523810375901381044fa823515");
				usu0.setDateDebut(null);
				usu0.setMotifDebut(null);
				usu0.setDateDebutMetier(RegDate.get(2010, 3, 8));
				usu0.setNumeroAffaire(new IdentifiantAffaireRF(5, 2010, 731, 666));   // <- index différent
				usu0.setIdentifiantDroit(new IdentifiantDroitRF(5, 2010, 432));
				usu0.setAyantDroit(pp1);
				usu0.setImmeuble(immeuble1);
				usu0.setCommunaute(comm);
				droitRFDAO.save(usu0);

				// un usufruit identique à celui qui arrive dans le fichier XML
				final UsufruitRF usu1 = new UsufruitRF();
				usu1.setMasterIdRF("1f1091523810375901381044fa823515");
				usu1.setDateDebut(null);
				usu1.setMotifDebut(null);
				usu1.setDateDebutMetier(RegDate.get(2010, 3, 8));
				usu1.setNumeroAffaire(new IdentifiantAffaireRF(5, 2010, 731, 0));
				usu1.setIdentifiantDroit(new IdentifiantDroitRF(5, 2010, 432));
				usu1.setAyantDroit(pp1);
				usu1.setImmeuble(immeuble2);
				usu1.setCommunaute(comm);
				droitRFDAO.save(usu1);

				return pp1.getId();
			}
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_servitude_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateSecondImport, TypeEntiteRF.SERVITUDE, TypeMutationRF.MODIFICATION, idPPRF1);

		// on process la mutation
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
				processor.process(mutation, false, null);
			}
		});

		// postcondition : la mutation est traitée et :
		//  - le usu0 est fermé
		//  - le usu1 est inchangé
		//  - un nouveau usufruit est créé
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final PersonnePhysiqueRF pp = (PersonnePhysiqueRF) ayantDroitRFDAO.get(ppId);
				assertNotNull(pp);

				final Set<DroitRF> droits = pp.getDroits();
				assertNotNull(droits);
				assertEquals(3, droits.size());

				final List<DroitRF> droitsList = new ArrayList<>(droits);
				droitsList.sort(new DroitRFComparator());

				// le usu0 doit être fermé
				final UsufruitRF usufruit0 = (UsufruitRF) droitsList.get(0);
				assertNotNull(usufruit0);
				assertEquals("1f1091523810375901381044fa823515", usufruit0.getMasterIdRF());
				assertNull(usufruit0.getDateDebut());
				assertEquals(dateSecondImport.getOneDayBefore(), usufruit0.getDateFin());
				assertNull(usufruit0.getMotifDebut());
				assertEquals(RegDate.get(2010, 3, 8), usufruit0.getDateDebutMetier());
				assertNull(usufruit0.getDateFinMetier());
				assertEquals(new IdentifiantAffaireRF(5, 2010, 731, 666), usufruit0.getNumeroAffaire());
				assertEquals(new IdentifiantDroitRF(5, 2010, 432), usufruit0.getIdentifiantDroit());
				assertEquals("_1f109152381037590138103b6f6e3cfa", usufruit0.getImmeuble().getIdRF());
				final CommunauteRF communaute0 = usufruit0.getCommunaute();
				assertNotNull(communaute0);
				assertEquals("_1f1091523810375901381044fa823515", communaute0.getIdRF());

				// un nouveau usufruit doit remplacer le usu1
				final UsufruitRF usufruit1 = (UsufruitRF) droitsList.get(1);
				assertNotNull(usufruit1);
				assertEquals("1f1091523810375901381044fa823515", usufruit1.getMasterIdRF());
				assertEquals(dateSecondImport, usufruit1.getDateDebut());
				assertNull(usufruit1.getDateFin());
				assertNull(usufruit1.getMotifDebut());
				assertEquals(RegDate.get(2010, 3, 8), usufruit1.getDateDebutMetier());
				assertNull(usufruit1.getDateFinMetier());
				assertEquals(new IdentifiantAffaireRF(5, 2010, 731, 0), usufruit1.getNumeroAffaire());
				assertEquals(new IdentifiantDroitRF(5, 2010, 432), usufruit1.getIdentifiantDroit());
				assertEquals("_1f109152381037590138103b6f6e3cfa", usufruit1.getImmeuble().getIdRF());
				final CommunauteRF communaute1 = usufruit1.getCommunaute();
				assertNotNull(communaute1);
				assertEquals("_1f1091523810375901381044fa823515", communaute1.getIdRF());

				// le dernier usufruit reste inchangé
				final UsufruitRF usufruit2 = (UsufruitRF) droitsList.get(2);
				assertNotNull(usufruit2);
				assertEquals("1f1091523810375901381044fa823515", usufruit2.getMasterIdRF());
				assertNull(usufruit2.getDateDebut());
				assertNull(usufruit2.getDateFin());
				assertNull(usufruit2.getMotifDebut());
				assertEquals(RegDate.get(2010, 3, 8), usufruit2.getDateDebutMetier());
				assertNull(usufruit2.getDateFinMetier());
				assertEquals(new IdentifiantAffaireRF(5, 2010, 731, 0), usufruit2.getNumeroAffaire());
				assertEquals(new IdentifiantDroitRF(5, 2010, 432), usufruit2.getIdentifiantDroit());
				assertEquals("_1f109152381037590138103b6f6e3cfc", usufruit2.getImmeuble().getIdRF());
				final CommunauteRF communaute2 = usufruit2.getCommunaute();
				assertNotNull(communaute2);
				assertEquals("_1f1091523810375901381044fa823515", communaute2.getIdRF());

			}
		});
	}

	/*
	 * Ce test vérifie que le processing d'une mutation de suppression fonctionne bien.
	 */
	@Test
	public void testProcessMutationSuppression() throws Exception {

		final String idPPRF1 = "_1f1091523810375901381037f42e3142";
		final String idPPRF2 = "_1f109152381037590138103835995c0d";
		final String idCommRF = "_1f1091523810375901381044fa823515";
		final String idImmeubleRF1 = "_1f109152381037590138103b6f6e3cfa";
		final String idImmeubleRF2 = "_1f109152381037590138103b6f6e3cfc";
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		// précondition : il y a déjà deux usufruits dans la base de données
		final Long ppId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

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
				ayantDroitRFDAO.save(pp2);

				CommunauteRF comm = new CommunauteRF();
				comm.setIdRF(idCommRF);
				comm = (CommunauteRF) ayantDroitRFDAO.save(comm);

				BienFondRF immeuble1 = new BienFondRF();
				immeuble1.setIdRF(idImmeubleRF1);
				immeuble1 = (BienFondRF) immeubleRFDAO.save(immeuble1);

				BienFondRF immeuble2 = new BienFondRF();
				immeuble2.setIdRF(idImmeubleRF2);
				immeuble2 = (BienFondRF) immeubleRFDAO.save(immeuble2);

				final UsufruitRF usu0 = new UsufruitRF();
				usu0.setMasterIdRF("1f1091523810375901381044fa823515");
				usu0.setDateDebut(null);
				usu0.setMotifDebut(null);
				usu0.setDateDebutMetier(RegDate.get(2010, 3, 8));
				usu0.setNumeroAffaire(new IdentifiantAffaireRF(5, 2010, 731, 0));
				usu0.setIdentifiantDroit(new IdentifiantDroitRF(5, 2010, 432));
				usu0.setAyantDroit(pp1);
				usu0.setImmeuble(immeuble1);
				usu0.setCommunaute(comm);
				droitRFDAO.save(usu0);

				final UsufruitRF usu1 = new UsufruitRF();
				usu1.setMasterIdRF("1f1091523810375901381044fa823515");
				usu1.setDateDebut(null);
				usu1.setMotifDebut(null);
				usu1.setDateDebutMetier(RegDate.get(2010, 3, 8));
				usu1.setNumeroAffaire(new IdentifiantAffaireRF(5, 2010, 731, 0));
				usu1.setIdentifiantDroit(new IdentifiantDroitRF(5, 2010, 432));
				usu1.setAyantDroit(pp1);
				usu1.setImmeuble(immeuble2);
				usu1.setCommunaute(comm);
				droitRFDAO.save(usu1);

				return pp1.getId();
			}
		});

		// on envoie un fichier d'import qui ne contient aucun droit
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_servitude_vide_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateSecondImport, TypeEntiteRF.SERVITUDE, TypeMutationRF.SUPPRESSION, idPPRF1);

		// on process la mutation
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
				processor.process(mutation, false, null);
			}
		});

		// postcondition : la mutation est traitée et tous les usufruits existants sont fermés
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final PersonnePhysiqueRF pp = (PersonnePhysiqueRF) ayantDroitRFDAO.get(ppId);
				assertNotNull(pp);

				final Set<DroitRF> droits = pp.getDroits();
				assertNotNull(droits);
				assertEquals(2, droits.size());

				final List<DroitRF> droitList = new ArrayList<>(droits);
				droitList.sort(new DroitRFComparator());

				// le usu0 doit être fermé
				final UsufruitRF usufruit0 = (UsufruitRF) droitList.get(0);
				assertNotNull(usufruit0);
				assertEquals("1f1091523810375901381044fa823515", usufruit0.getMasterIdRF());
				assertNull(usufruit0.getDateDebut());
				assertEquals(dateSecondImport.getOneDayBefore(), usufruit0.getDateFin());
				assertNull(usufruit0.getMotifDebut());
				assertEquals(RegDate.get(2010, 3, 8), usufruit0.getDateDebutMetier());
				assertNull(usufruit0.getDateFinMetier());
				assertEquals(new IdentifiantAffaireRF(5, 2010, 731, 0), usufruit0.getNumeroAffaire());
				assertEquals(new IdentifiantDroitRF(5, 2010, 432), usufruit0.getIdentifiantDroit());
				assertEquals("_1f109152381037590138103b6f6e3cfa", usufruit0.getImmeuble().getIdRF());
				final CommunauteRF communaute0 = usufruit0.getCommunaute();
				assertNotNull(communaute0);
				assertEquals("_1f1091523810375901381044fa823515", communaute0.getIdRF());

				// le usu1 doit être fermé
				final UsufruitRF usufruit1 = (UsufruitRF) droitList.get(1);
				assertNotNull(usufruit1);
				assertEquals("1f1091523810375901381044fa823515", usufruit1.getMasterIdRF());
				assertNull(usufruit1.getDateDebut());
				assertEquals(dateSecondImport.getOneDayBefore(), usufruit1.getDateFin());
				assertNull(usufruit1.getMotifDebut());
				assertEquals(RegDate.get(2010, 3, 8), usufruit1.getDateDebutMetier());
				assertNull(usufruit1.getDateFinMetier());
				assertEquals(new IdentifiantAffaireRF(5, 2010, 731, 0), usufruit1.getNumeroAffaire());
				assertEquals(new IdentifiantDroitRF(5, 2010, 432), usufruit1.getIdentifiantDroit());
				assertEquals("_1f109152381037590138103b6f6e3cfc", usufruit1.getImmeuble().getIdRF());
				final CommunauteRF communaute1 = usufruit1.getCommunaute();
				assertNotNull(communaute1);
				assertEquals("_1f1091523810375901381044fa823515", communaute1.getIdRF());
			}
		});
	}
}