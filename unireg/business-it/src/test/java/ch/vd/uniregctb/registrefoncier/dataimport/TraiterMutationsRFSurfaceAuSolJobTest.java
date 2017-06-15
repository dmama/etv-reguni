package ch.vd.uniregctb.registrefoncier.dataimport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImport;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.uniregctb.evenement.registrefoncier.TypeImportRF;
import ch.vd.uniregctb.evenement.registrefoncier.TypeMutationRF;
import ch.vd.uniregctb.registrefoncier.BienFondsRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.SurfaceAuSolRF;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.SurfaceAuSolRFDAO;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobDefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TraiterMutationsRFSurfaceAuSolJobTest extends ImportRFTestClass {

	private BatchScheduler batchScheduler;
	private ImmeubleRFDAO immeubleRFDAO;
	private EvenementRFImportDAO evenementRFImportDAO;
	private EvenementRFMutationDAO evenementRFMutationDAO;
	private SurfaceAuSolRFDAO surfaceAuSolRFDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		batchScheduler = getBean(BatchScheduler.class, "batchScheduler");
		immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		evenementRFImportDAO = getBean(EvenementRFImportDAO.class, "evenementRFImportDAO");
		evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");
		surfaceAuSolRFDAO = getBean(SurfaceAuSolRFDAO.class, "surfaceAuSolRFDAO");
	}

	/**
	 * Ce test vérifie que les mutations de type CREATION créent bien de nouvelles surfaces
	 */
	@Test
	public void testTraiterMutationsCreation() throws Exception {

		final RegDate dateImport = RegDate.get(2016, 10, 1);

		class Ids {
			long immeuble1;
			long immeuble2;
		}
		final Ids ids = new Ids();

		// on insère les données de l'import dans la base
		final Long importId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				EvenementRFImport importEvent = new EvenementRFImport();
				importEvent.setType(TypeImportRF.PRINCIPAL);
				importEvent.setDateEvenement(dateImport);
				importEvent.setEtat(EtatEvenementRF.TRAITE);
				importEvent.setFileUrl("http://turlututu");
				importEvent = evenementRFImportDAO.save(importEvent);

				BienFondsRF immeuble1 = new BienFondsRF();
				immeuble1.setIdRF("382929efa218");
				ids.immeuble1 = immeubleRFDAO.save(immeuble1).getId();

				BienFondsRF immeuble2 = new BienFondsRF();
				immeuble2.setIdRF("58390029228");
				ids.immeuble2 = immeubleRFDAO.save(immeuble2).getId();

				final EvenementRFMutation mut0 = new EvenementRFMutation();
				mut0.setParentImport(importEvent);
				mut0.setEtat(EtatEvenementRF.A_TRAITER);
				mut0.setTypeEntite(TypeEntiteRF.SURFACE_AU_SOL);
				mut0.setTypeMutation(TypeMutationRF.CREATION);
				mut0.setIdRF("382929efa218");
				mut0.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						                   "<BodenbedeckungList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						                   "    <Bodenbedeckung>\n" +
						                   "        <GrundstueckIDREF>382929efa218</GrundstueckIDREF>\n" +
						                   "        <Art>\n" +
						                   "            <TextDe></TextDe>\n" +
						                   "            <TextFr>Forêt</TextFr>\n" +
						                   "        </Art>\n" +
						                   "        <Flaeche>37823</Flaeche>\n" +
						                   "    </Bodenbedeckung>\n" +
						                   "    <Bodenbedeckung>\n" +
						                   "        <GrundstueckIDREF>382929efa218</GrundstueckIDREF>\n" +
						                   "        <Art>\n" +
						                   "            <TextDe></TextDe>\n" +
						                   "            <TextFr>Paturage</TextFr>\n" +
						                   "        </Art>\n" +
						                   "        <Flaeche>4728211</Flaeche>\n" +
						                   "    </Bodenbedeckung>\n" +
						                   "</BodenbedeckungList>\n");
				evenementRFMutationDAO.save(mut0);

				final EvenementRFMutation mut1 = new EvenementRFMutation();
				mut1.setParentImport(importEvent);
				mut1.setEtat(EtatEvenementRF.A_TRAITER);
				mut1.setTypeEntite(TypeEntiteRF.SURFACE_AU_SOL);
				mut1.setTypeMutation(TypeMutationRF.CREATION);
				mut1.setIdRF("58390029228");
				mut1.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						                   "<BodenbedeckungList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						                   "    <Bodenbedeckung>\n" +
						                   "        <GrundstueckIDREF>58390029228</GrundstueckIDREF>\n" +
						                   "        <Art>\n" +
						                   "            <TextDe></TextDe>\n" +
						                   "            <TextFr>Place de Tir</TextFr>\n" +
						                   "        </Art>\n" +
						                   "        <Flaeche>20289</Flaeche>\n" +
						                   "    </Bodenbedeckung>\n" +
						                   "    <Bodenbedeckung>\n" +
						                   "        <GrundstueckIDREF>58390029228</GrundstueckIDREF>\n" +
						                   "        <Art>\n" +
						                   "            <TextDe></TextDe>\n" +
						                   "            <TextFr>Héliport</TextFr>\n" +
						                   "        </Art>\n" +
						                   "        <Flaeche>10282</Flaeche>\n" +
						                   "    </Bodenbedeckung>\n" +
						                   "</BodenbedeckungList>\n");
				evenementRFMutationDAO.save(mut1);

				return importEvent.getId();
			}
		});
		assertNotNull(importId);

		// on déclenche le démarrage du job
		final Map<String, Object> params = new HashMap<>();
		params.put(TraiterMutationsRFJob.ID, importId);
		params.put(TraiterMutationsRFJob.NB_THREADS, 2);
		params.put(TraiterMutationsRFJob.CONTINUE_WITH_DATES_FIN_JOB, Boolean.FALSE);
		params.put(TraiterMutationsRFJob.CONTINUE_WITH_IDENTIFICATION_JOB, Boolean.FALSE);

		final JobDefinition job = batchScheduler.startJob(TraiterMutationsRFJob.NAME, params);
		assertNotNull(job);

		// le job doit se terminer correctement
		waitForJobCompletion(job);
		assertEquals(JobDefinition.JobStatut.JOB_OK, job.getStatut());

		// on vérifie que les mutations ont bien été traitées
		assertEtatMutations(2, EtatEvenementRF.TRAITE); // il y a 2 immeubles différents dans le fichier d'import

		// on vérifie que les surfaces ont bien été créées
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final ImmeubleRF immeuble1 = immeubleRFDAO.get(ids.immeuble1);
				assertNotNull(immeuble1);

				final List<SurfaceAuSolRF> surfaces1 = new ArrayList<>(immeuble1.getSurfacesAuSol());
				assertEquals(2, surfaces1.size());
				Collections.sort(surfaces1, (o1, o2) -> Integer.compare(o1.getSurface(), o2.getSurface()));

				final SurfaceAuSolRF surface10 = surfaces1.get(0);
				assertEquals(dateImport, surface10.getDateDebut());
				assertNull(surface10.getDateFin());
				assertEquals(37823, surface10.getSurface());
				assertEquals("Forêt", surface10.getType());

				final SurfaceAuSolRF surface11 = surfaces1.get(1);
				assertEquals(dateImport, surface11.getDateDebut());
				assertNull(surface11.getDateFin());
				assertEquals(4728211, surface11.getSurface());
				assertEquals("Paturage", surface11.getType());

				final ImmeubleRF immeuble2 = immeubleRFDAO.get(ids.immeuble2);
				assertNotNull(immeuble2);

				final List<SurfaceAuSolRF> surfaces2 = new ArrayList<>(immeuble2.getSurfacesAuSol());
				assertEquals(2, surfaces2.size());
				Collections.sort(surfaces2, (o1, o2) -> Integer.compare(o1.getSurface(), o2.getSurface()));

				final SurfaceAuSolRF surface20 = surfaces2.get(0);
				assertEquals(dateImport, surface20.getDateDebut());
				assertNull(surface20.getDateFin());
				assertEquals(10282, surface20.getSurface());
				assertEquals("Héliport", surface20.getType());

				final SurfaceAuSolRF surface21 = surfaces2.get(1);
				assertEquals(dateImport, surface21.getDateDebut());
				assertNull(surface21.getDateFin());
				assertEquals(20289, surface21.getSurface());
				assertEquals("Place de Tir", surface21.getType());
			}
		});
	}

	/**
	 * Ce test vérifie que les mutations de type MODIFICATION modifient bien des surfaces existantes
	 */
	@Test
	public void testTraiterMutationsModification() throws Exception {

		final RegDate dateImportInitial = RegDate.get(2010, 1, 1);
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		class Ids {
			long immeuble1;
			long immeuble2;
		}
		final Ids ids = new Ids();

		// on insère les données de l'import initial dans la base
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				BienFondsRF immeuble1 = new BienFondsRF();
				immeuble1.setIdRF("382929efa218");
				immeuble1 = (BienFondsRF) immeubleRFDAO.save(immeuble1);
				ids.immeuble1 = immeuble1.getId();

				BienFondsRF immeuble2 = new BienFondsRF();
				immeuble2.setIdRF("58390029228");
				immeuble2 = (BienFondsRF) immeubleRFDAO.save(immeuble2);
				ids.immeuble2 = immeuble2.getId();

				final SurfaceAuSolRF surface10 = new SurfaceAuSolRF();
				surface10.setDateDebut(dateImportInitial);
				surface10.setImmeuble(immeuble1);
				surface10.setSurface(22323);
				surface10.setType("Forêt");
				surfaceAuSolRFDAO.save(surface10);

				final SurfaceAuSolRF surface11 = new SurfaceAuSolRF();
				surface11.setDateDebut(dateImportInitial);
				surface11.setImmeuble(immeuble1);
				surface11.setSurface(4728211);
				surface11.setType("Paturage");
				surfaceAuSolRFDAO.save(surface11);

				final SurfaceAuSolRF surface20 = new SurfaceAuSolRF();
				surface20.setDateDebut(dateImportInitial);
				surface20.setImmeuble(immeuble2);
				surface20.setSurface(10282);
				surface20.setType("Héliport");
				surfaceAuSolRFDAO.save(surface20);

				final SurfaceAuSolRF surface21 = new SurfaceAuSolRF();
				surface21.setDateDebut(dateImportInitial);
				surface21.setImmeuble(immeuble2);
				surface21.setSurface(20289);
				surface21.setType("Marelle");
				surfaceAuSolRFDAO.save(surface21);
			}
		});

		// on insère les données du second import dans la base
		final Long importId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				EvenementRFImport importEvent = new EvenementRFImport();
				importEvent.setType(TypeImportRF.PRINCIPAL);
				importEvent.setDateEvenement(dateSecondImport);
				importEvent.setEtat(EtatEvenementRF.TRAITE);
				importEvent.setFileUrl("http://turlututu");
				importEvent = evenementRFImportDAO.save(importEvent);

				final EvenementRFMutation mut0 = new EvenementRFMutation();
				mut0.setParentImport(importEvent);
				mut0.setEtat(EtatEvenementRF.A_TRAITER);
				mut0.setTypeEntite(TypeEntiteRF.SURFACE_AU_SOL);
				mut0.setTypeMutation(TypeMutationRF.MODIFICATION);
				mut0.setIdRF("382929efa218");
				mut0.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						                   "<BodenbedeckungList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						                   "    <Bodenbedeckung>\n" +
						                   "        <GrundstueckIDREF>382929efa218</GrundstueckIDREF>\n" +
						                   "        <Art>\n" +
						                   "            <TextDe></TextDe>\n" +
						                   "            <TextFr>Forêt</TextFr>\n" +
						                   "        </Art>\n" +
						                   "        <Flaeche>37823</Flaeche>\n" +
						                   "    </Bodenbedeckung>\n" +
						                   "    <Bodenbedeckung>\n" +
						                   "        <GrundstueckIDREF>382929efa218</GrundstueckIDREF>\n" +
						                   "        <Art>\n" +
						                   "            <TextDe></TextDe>\n" +
						                   "            <TextFr>Paturage</TextFr>\n" +
						                   "        </Art>\n" +
						                   "        <Flaeche>4728211</Flaeche>\n" +
						                   "    </Bodenbedeckung>\n" +
						                   "</BodenbedeckungList>\n");
				evenementRFMutationDAO.save(mut0);

				final EvenementRFMutation mut1 = new EvenementRFMutation();
				mut1.setParentImport(importEvent);
				mut1.setEtat(EtatEvenementRF.A_TRAITER);
				mut1.setTypeEntite(TypeEntiteRF.SURFACE_AU_SOL);
				mut1.setTypeMutation(TypeMutationRF.MODIFICATION);
				mut1.setIdRF("58390029228");
				mut1.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						                   "<BodenbedeckungList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						                   "    <Bodenbedeckung>\n" +
						                   "        <GrundstueckIDREF>58390029228</GrundstueckIDREF>\n" +
						                   "        <Art>\n" +
						                   "            <TextDe></TextDe>\n" +
						                   "            <TextFr>Place de Tir</TextFr>\n" +
						                   "        </Art>\n" +
						                   "        <Flaeche>20289</Flaeche>\n" +
						                   "    </Bodenbedeckung>\n" +
						                   "    <Bodenbedeckung>\n" +
						                   "        <GrundstueckIDREF>58390029228</GrundstueckIDREF>\n" +
						                   "        <Art>\n" +
						                   "            <TextDe></TextDe>\n" +
						                   "            <TextFr>Héliport</TextFr>\n" +
						                   "        </Art>\n" +
						                   "        <Flaeche>10282</Flaeche>\n" +
						                   "    </Bodenbedeckung>\n" +
						                   "</BodenbedeckungList>\n");
				evenementRFMutationDAO.save(mut1);

				return importEvent.getId();
			}
		});
		assertNotNull(importId);

		// on déclenche le démarrage du job
		final Map<String, Object> params = new HashMap<>();
		params.put(TraiterMutationsRFJob.ID, importId);
		params.put(TraiterMutationsRFJob.NB_THREADS, 2);
		params.put(TraiterMutationsRFJob.CONTINUE_WITH_DATES_FIN_JOB, Boolean.FALSE);
		params.put(TraiterMutationsRFJob.CONTINUE_WITH_IDENTIFICATION_JOB, Boolean.FALSE);

		final JobDefinition job = batchScheduler.startJob(TraiterMutationsRFJob.NAME, params);
		assertNotNull(job);

		// le job doit se terminer correctement
		waitForJobCompletion(job);
		assertEquals(JobDefinition.JobStatut.JOB_OK, job.getStatut());

		// on vérifie que les mutations ont bien été traitées
		assertEtatMutations(2, EtatEvenementRF.TRAITE);

		// on vérifie que les surfaces ont bien été mises-à-jour
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final ImmeubleRF immeuble1 = immeubleRFDAO.get(ids.immeuble1);
				assertNotNull(immeuble1);

				final List<SurfaceAuSolRF> surfaces1 = new ArrayList<>(immeuble1.getSurfacesAuSol());
				assertEquals(3, surfaces1.size());
				Collections.sort(surfaces1, (o1, o2) -> Integer.compare(o1.getSurface(), o2.getSurface()));

				final SurfaceAuSolRF surface10 = surfaces1.get(0);
				assertEquals(dateImportInitial, surface10.getDateDebut());
				assertEquals(dateSecondImport.getOneDayBefore(), surface10.getDateFin());
				assertEquals(22323, surface10.getSurface());
				assertEquals("Forêt", surface10.getType());

				final SurfaceAuSolRF surface11 = surfaces1.get(1);
				assertEquals(dateSecondImport, surface11.getDateDebut());
				assertNull(surface11.getDateFin());
				assertEquals(37823, surface11.getSurface());
				assertEquals("Forêt", surface11.getType());

				final SurfaceAuSolRF surface12 = surfaces1.get(2);
				assertEquals(dateImportInitial, surface12.getDateDebut());
				assertNull(surface12.getDateFin());
				assertEquals(4728211, surface12.getSurface());
				assertEquals("Paturage", surface12.getType());

				final ImmeubleRF immeuble2 = immeubleRFDAO.get(ids.immeuble2);
				assertNotNull(immeuble2);

				final List<SurfaceAuSolRF> surfaces2 = new ArrayList<>(immeuble2.getSurfacesAuSol());
				assertEquals(3, surfaces2.size());
				Collections.sort(surfaces2, new SurfaceAuSolRFComparator());

				final SurfaceAuSolRF surface20 = surfaces2.get(0);
				assertEquals(dateImportInitial, surface20.getDateDebut());
				assertNull(surface20.getDateFin());
				assertEquals(10282, surface20.getSurface());
				assertEquals("Héliport", surface20.getType());

				final SurfaceAuSolRF surface21 = surfaces2.get(1);
				assertEquals(dateImportInitial, surface21.getDateDebut());
				assertEquals(dateSecondImport.getOneDayBefore(), surface21.getDateFin());
				assertEquals(20289, surface21.getSurface());
				assertEquals("Marelle", surface21.getType());

				final SurfaceAuSolRF surface22 = surfaces2.get(2);
				assertEquals(dateSecondImport, surface22.getDateDebut());
				assertNull(surface22.getDateFin());
				assertEquals(20289, surface22.getSurface());
				assertEquals("Place de Tir", surface22.getType());
			}
		});
	}

	/**
	 * Ce test vérifie que les mutations de type SUPPRESSION ferment bien toutes les surfaces existantes
	 */
	@Test
	public void testTraiterMutationsSuppression() throws Exception {

		final RegDate dateImportInitial = RegDate.get(2010, 1, 1);
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		class Ids {
			long immeuble1;
			long immeuble2;
		}
		final Ids ids = new Ids();

		// on insère les données de l'import initial dans la base
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				BienFondsRF immeuble1 = new BienFondsRF();
				immeuble1.setIdRF("382929efa218");
				immeuble1 = (BienFondsRF) immeubleRFDAO.save(immeuble1);
				ids.immeuble1 = immeuble1.getId();

				BienFondsRF immeuble2 = new BienFondsRF();
				immeuble2.setIdRF("58390029228");
				immeuble2 = (BienFondsRF) immeubleRFDAO.save(immeuble2);
				ids.immeuble2 = immeuble2.getId();

				final SurfaceAuSolRF surface10 = new SurfaceAuSolRF();
				surface10.setDateDebut(dateImportInitial);
				surface10.setDateFin(RegDate.get(2013, 3, 4));
				surface10.setImmeuble(immeuble1);
				surface10.setSurface(22323);
				surface10.setType("Forêt");
				surfaceAuSolRFDAO.save(surface10);

				final SurfaceAuSolRF surface11 = new SurfaceAuSolRF();
				surface11.setDateDebut(dateImportInitial);
				surface11.setImmeuble(immeuble1);
				surface11.setSurface(4728211);
				surface11.setType("Paturage");
				surfaceAuSolRFDAO.save(surface11);

				final SurfaceAuSolRF surface20 = new SurfaceAuSolRF();
				surface20.setDateDebut(dateImportInitial);
				surface20.setDateFin(RegDate.get(2013, 3, 4));
				surface20.setImmeuble(immeuble2);
				surface20.setSurface(10282);
				surface20.setType("Héliport");
				surfaceAuSolRFDAO.save(surface20);

				final SurfaceAuSolRF surface21 = new SurfaceAuSolRF();
				surface21.setDateDebut(dateImportInitial);
				surface21.setImmeuble(immeuble2);
				surface21.setSurface(20289);
				surface21.setType("Marelle");
				surfaceAuSolRFDAO.save(surface21);
			}
		});

		// on insère les données du second import dans la base
		final Long importId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				EvenementRFImport importEvent = new EvenementRFImport();
				importEvent.setType(TypeImportRF.PRINCIPAL);
				importEvent.setDateEvenement(dateSecondImport);
				importEvent.setEtat(EtatEvenementRF.TRAITE);
				importEvent.setFileUrl("http://turlututu");
				importEvent = evenementRFImportDAO.save(importEvent);

				final EvenementRFMutation mut0 = new EvenementRFMutation();
				mut0.setParentImport(importEvent);
				mut0.setEtat(EtatEvenementRF.A_TRAITER);
				mut0.setTypeEntite(TypeEntiteRF.SURFACE_AU_SOL);
				mut0.setTypeMutation(TypeMutationRF.SUPPRESSION);
				mut0.setIdRF("382929efa218");
				mut0.setXmlContent(null);
				evenementRFMutationDAO.save(mut0);

				final EvenementRFMutation mut1 = new EvenementRFMutation();
				mut1.setParentImport(importEvent);
				mut1.setEtat(EtatEvenementRF.A_TRAITER);
				mut1.setTypeEntite(TypeEntiteRF.SURFACE_AU_SOL);
				mut1.setTypeMutation(TypeMutationRF.SUPPRESSION);
				mut1.setIdRF("58390029228");
				mut1.setXmlContent(null);
				evenementRFMutationDAO.save(mut1);

				return importEvent.getId();
			}
		});
		assertNotNull(importId);

		// on déclenche le démarrage du job
		final Map<String, Object> params = new HashMap<>();
		params.put(TraiterMutationsRFJob.ID, importId);
		params.put(TraiterMutationsRFJob.NB_THREADS, 2);
		params.put(TraiterMutationsRFJob.CONTINUE_WITH_DATES_FIN_JOB, Boolean.FALSE);
		params.put(TraiterMutationsRFJob.CONTINUE_WITH_IDENTIFICATION_JOB, Boolean.FALSE);

		final JobDefinition job = batchScheduler.startJob(TraiterMutationsRFJob.NAME, params);
		assertNotNull(job);

		// le job doit se terminer correctement
		waitForJobCompletion(job);
		assertEquals(JobDefinition.JobStatut.JOB_OK, job.getStatut());

		// on vérifie que les mutations ont bien été traitées
		assertEtatMutations(2, EtatEvenementRF.TRAITE);

		// on vérifie que les surfaces ont bien été fermées
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final ImmeubleRF immeuble1 = immeubleRFDAO.get(ids.immeuble1);
				assertNotNull(immeuble1);

				final List<SurfaceAuSolRF> surfaces1 = new ArrayList<>(immeuble1.getSurfacesAuSol());
				assertEquals(2, surfaces1.size());
				Collections.sort(surfaces1, (o1, o2) -> Integer.compare(o1.getSurface(), o2.getSurface()));

				final SurfaceAuSolRF surface10 = surfaces1.get(0);
				assertEquals(dateImportInitial, surface10.getDateDebut());
				assertEquals(RegDate.get(2013, 3, 4), surface10.getDateFin());
				assertEquals(22323, surface10.getSurface());
				assertEquals("Forêt", surface10.getType());

				final SurfaceAuSolRF surface11 = surfaces1.get(1);
				assertEquals(dateImportInitial, surface11.getDateDebut());
				assertEquals(dateSecondImport.getOneDayBefore(), surface11.getDateFin());
				assertEquals(4728211, surface11.getSurface());
				assertEquals("Paturage", surface11.getType());

				final ImmeubleRF immeuble2 = immeubleRFDAO.get(ids.immeuble2);
				assertNotNull(immeuble2);

				final List<SurfaceAuSolRF> surfaces2 = new ArrayList<>(immeuble2.getSurfacesAuSol());
				assertEquals(2, surfaces2.size());
				Collections.sort(surfaces2, new SurfaceAuSolRFComparator());

				final SurfaceAuSolRF surface20 = surfaces2.get(0);
				assertEquals(dateImportInitial, surface20.getDateDebut());
				assertEquals(RegDate.get(2013, 3, 4), surface20.getDateFin());
				assertEquals(10282, surface20.getSurface());
				assertEquals("Héliport", surface20.getType());

				final SurfaceAuSolRF surface21 = surfaces2.get(1);
				assertEquals(dateImportInitial, surface21.getDateDebut());
				assertEquals(dateSecondImport.getOneDayBefore(), surface21.getDateFin());
				assertEquals(20289, surface21.getSurface());
				assertEquals("Marelle", surface21.getType());
			}
		});
	}

	/**
	 * Tri par surface puis type croissants.
	 */
	private static class SurfaceAuSolRFComparator implements Comparator<SurfaceAuSolRF> {
		@Override
		public int compare(SurfaceAuSolRF o1, SurfaceAuSolRF o2) {
			final int c1 = Integer.compare(o1.getSurface(), o2.getSurface());
			if (c1 != 0) {
				return c1;
			}
			return o1.getType().compareTo(o2.getType());
		}
	}
}