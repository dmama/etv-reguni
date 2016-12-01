package ch.vd.uniregctb.registrefoncier.dataimport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImport;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.uniregctb.evenement.registrefoncier.TypeMutationRF;
import ch.vd.uniregctb.registrefoncier.BatimentRF;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.ImplantationRF;
import ch.vd.uniregctb.registrefoncier.SurfaceBatimentRF;
import ch.vd.uniregctb.registrefoncier.dao.BatimentRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.key.BatimentRFKey;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobDefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TraiterMutationsRFBatimentJobTest extends ImportRFTestClass {

	private BatchScheduler batchScheduler;
	private ImmeubleRFDAO immeubleRFDAO;
	private BatimentRFDAO batimentRFDAO;
	private EvenementRFImportDAO evenementRFImportDAO;
	private EvenementRFMutationDAO evenementRFMutationDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		batchScheduler = getBean(BatchScheduler.class, "batchScheduler");
		immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		batimentRFDAO = getBean(BatimentRFDAO.class, "batimentRFDAO");
		evenementRFImportDAO = getBean(EvenementRFImportDAO.class, "evenementRFImportDAO");
		evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");
	}

	/**
	 * Ce test vérifie que les mutations de type CREATION créent bien de nouveaux batiments
	 */
	@Test
	public void testTraiterMutationsCreation() throws Exception {

		final RegDate dateImport = RegDate.get(2016, 10, 1);

		// on insère les données de l'import dans la base
		final Long importId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				EvenementRFImport importEvent = new EvenementRFImport();
				importEvent.setDateEvenement(dateImport);
				importEvent.setEtat(EtatEvenementRF.TRAITE);
				importEvent.setFileUrl("http://turlututu");
				importEvent = evenementRFImportDAO.save(importEvent);

				BienFondRF immeuble1 = new BienFondRF();
				immeuble1.setIdRF("_1f109152381026b501381028a73d1852");
				immeubleRFDAO.save(immeuble1);

				BienFondRF immeuble2 = new BienFondRF();
				immeuble2.setIdRF("_1f10915238106bdc0138106ff6d3305d");
				immeubleRFDAO.save(immeuble2);

				final EvenementRFMutation mut0 = new EvenementRFMutation();
				mut0.setParentImport(importEvent);
				mut0.setEtat(EtatEvenementRF.A_TRAITER);
				mut0.setTypeEntite(TypeEntiteRF.BATIMENT);
				mut0.setTypeMutation(TypeMutationRF.CREATION);
				mut0.setIdRF("1f109152381026b50138102aa28557e0");
				mut0.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						                   "<Gebaeude VersionID=\"1f109152381026b50138102aa2875806\" MasterID=\"1f109152381026b50138102aa28557e0\" xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						                   "    <GrundstueckZuGebaeude>\n" +
						                   "        <GrundstueckIDREF>_1f109152381026b501381028a73d1852</GrundstueckIDREF>\n" +
						                   "        <AbschnittFlaeche>104</AbschnittFlaeche>\n" +
						                   "    </GrundstueckZuGebaeude>\n" +
						                   "    <Einzelobjekt>false</Einzelobjekt>\n" +
						                   "    <Unterirdisch>false</Unterirdisch>\n" +
						                   "    <MehrereGrundstuecke>false</MehrereGrundstuecke>\n" +
						                   "    <GebaeudeArten>\n" +
						                   "        <GebaeudeArtCode>\n" +
						                   "            <TextDe>*Habitation</TextDe>\n" +
						                   "            <TextFr>Habitation</TextFr>\n" +
						                   "        </GebaeudeArtCode>\n" +
						                   "    </GebaeudeArten>\n" +
						                   "    <Versicherungsnummer>3064</Versicherungsnummer>\n" +
						                   "</Gebaeude>\n");
				evenementRFMutationDAO.save(mut0);

				final EvenementRFMutation mut1 = new EvenementRFMutation();
				mut1.setParentImport(importEvent);
				mut1.setEtat(EtatEvenementRF.A_TRAITER);
				mut1.setTypeEntite(TypeEntiteRF.BATIMENT);
				mut1.setTypeMutation(TypeMutationRF.CREATION);
				mut1.setIdRF("1f10915238106bdc0138107364741e62");
				mut1.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						                   "<Gebaeude VersionID=\"1f10915238106bdc0138107364791e9d\" MasterID=\"1f10915238106bdc0138107364741e62\" xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						                   "    <GrundstueckZuGebaeude>\n" +
						                   "        <GrundstueckIDREF>_1f10915238106bdc0138106ff6d3305d</GrundstueckIDREF>\n" +
						                   "    </GrundstueckZuGebaeude>\n" +
						                   "    <Einzelobjekt>false</Einzelobjekt>\n" +
						                   "    <Unterirdisch>true</Unterirdisch>\n" +
						                   "    <MehrereGrundstuecke>false</MehrereGrundstuecke>\n" +
						                   "    <Flaeche>247</Flaeche>\n" +
						                   "    <GebaeudeArten>\n" +
						                   "        <GebaeudeArtCode>\n" +
						                   "            <TextDe>*Garage</TextDe>\n" +
						                   "            <TextFr>Garage</TextFr>\n" +
						                   "        </GebaeudeArtCode>\n" +
						                   "    </GebaeudeArten>\n" +
						                   "    <Versicherungsnummer>312b</Versicherungsnummer>\n" +
						                   "</Gebaeude>");
				evenementRFMutationDAO.save(mut1);

				return importEvent.getId();
			}
		});
		assertNotNull(importId);

		// on déclenche le démarrage du job
		final HashMap<String, Object> params = new HashMap<>();
		params.put(TraiterMutationsRFJob.ID, importId);
		params.put(TraiterMutationsRFJob.NB_THREADS, 2);
		params.put(TraiterMutationsRFJob.CONTINUE_WITH_IDENTIFICATION_JOB, false);

		final JobDefinition job = batchScheduler.startJob(TraiterMutationsRFJob.NAME, params);
		assertNotNull(job);

		// le job doit se terminer correctement
		waitForJobCompletion(job);
		assertEquals(JobDefinition.JobStatut.JOB_OK, job.getStatut());

		// on vérifie que les mutations ont bien été traitées
		assertEtatMutations(2, EtatEvenementRF.TRAITE); // il y a 2 bâtiments différents dans le fichier d'import

		// on vérifie que les bâtiments ont bien été créés
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final BatimentRF batiment0 = batimentRFDAO.find(new BatimentRFKey("1f109152381026b50138102aa28557e0"));
				assertNotNull(batiment0);
				assertEquals("1f109152381026b50138102aa28557e0", batiment0.getMasterIdRF());
				assertEquals("Habitation", batiment0.getType());
				assertEmpty(batiment0.getSurfaces());

				final Set<ImplantationRF> implantations0 = batiment0.getImplantations();
				assertEquals(1, implantations0.size());
				final ImplantationRF implantation00 = implantations0.iterator().next();
				assertEquals("_1f109152381026b501381028a73d1852", implantation00.getImmeuble().getIdRF());
				assertEquals(Integer.valueOf(104), implantation00.getSurface());
				assertEquals(dateImport, implantation00.getDateDebut());
				assertNull(implantation00.getDateFin());

				final BatimentRF batiment1 = batimentRFDAO.find(new BatimentRFKey("1f10915238106bdc0138107364741e62"));
				assertNotNull(batiment1);
				assertEquals("1f10915238106bdc0138107364741e62", batiment1.getMasterIdRF());
				assertEquals("Garage", batiment1.getType());

				final Set<SurfaceBatimentRF> surfaces1 = batiment1.getSurfaces();
				assertEquals(1, surfaces1.size());
				final SurfaceBatimentRF surface10 = surfaces1.iterator().next();
				assertEquals(247, surface10.getSurface());
				assertEquals(dateImport, surface10.getDateDebut());
				assertNull(surface10.getDateFin());

				final Set<ImplantationRF> implantations1 = batiment1.getImplantations();
				assertEquals(1, implantations1.size());
				final ImplantationRF implantation10 = implantations1.iterator().next();
				assertEquals("_1f10915238106bdc0138106ff6d3305d", implantation10.getImmeuble().getIdRF());
				assertNull(implantation10.getSurface());
				assertEquals(dateImport, implantation10.getDateDebut());
				assertNull(implantation10.getDateFin());
			}
		});
	}

	/**
	 * Ce test vérifie que les mutations de type CREATION créent bien de nouveaux batiments lorsque ces derniers ont des types en texte libre ou nuls.
	 */
	@Test
	public void testTraiterMutationsCreationTypeTexteLibreOuNull() throws Exception {

		final RegDate dateImport = RegDate.get(2016, 10, 1);

		// on insère les données de l'import dans la base
		final Long importId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				EvenementRFImport importEvent = new EvenementRFImport();
				importEvent.setDateEvenement(dateImport);
				importEvent.setEtat(EtatEvenementRF.TRAITE);
				importEvent.setFileUrl("http://turlututu");
				importEvent = evenementRFImportDAO.save(importEvent);

				BienFondRF immeuble1 = new BienFondRF();
				immeuble1.setIdRF("_8af80e6254709f68015476fecb1f0e0b");
				immeubleRFDAO.save(immeuble1);

				BienFondRF immeuble2 = new BienFondRF();
				immeuble2.setIdRF("_1f109152381026b501381028bb23779a");
				immeubleRFDAO.save(immeuble2);

				final EvenementRFMutation mut0 = new EvenementRFMutation();
				mut0.setParentImport(importEvent);
				mut0.setEtat(EtatEvenementRF.A_TRAITER);
				mut0.setTypeEntite(TypeEntiteRF.BATIMENT);
				mut0.setTypeMutation(TypeMutationRF.CREATION);
				mut0.setIdRF("8af80e6254709f6801547708f4c10ebd");
				mut0.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						                   "<Gebaeude VersionID=\"8af80e6254709f6801547708f4c10ebc\" MasterID=\"8af80e6254709f6801547708f4c10ebd\" xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						                   "    <GrundstueckZuGebaeude>\n" +
						                   "        <GrundstueckIDREF>_8af80e6254709f68015476fecb1f0e0b</GrundstueckIDREF>\n" +
						                   "        <AbschnittFlaeche>0</AbschnittFlaeche>\n" +
						                   "    </GrundstueckZuGebaeude>\n" +
						                   "    <KantGid>25141</KantGid>\n" +
						                   "    <Einzelobjekt>false</Einzelobjekt>\n" +
						                   "    <Unterirdisch>false</Unterirdisch>\n" +
						                   "    <MehrereGrundstuecke>false</MehrereGrundstuecke>\n" +
						                   "    <Koordinaten>\n" +
						                   "        <QualitaetKoordinaten>nicht_vorhanden</QualitaetKoordinaten>\n" +
						                   "    </Koordinaten>\n" +
						                   "    <GebaeudeArten>\n" +
						                   "        <GebaeudeArtZusatz>Centrale électrique sur le domaine public (art. 20LICom) contigüe à la parcelle 554</GebaeudeArtZusatz>\n" +
						                   "    </GebaeudeArten>\n" +
						                   "</Gebaeude>\n");
				evenementRFMutationDAO.save(mut0);

				final EvenementRFMutation mut1 = new EvenementRFMutation();
				mut1.setParentImport(importEvent);
				mut1.setEtat(EtatEvenementRF.A_TRAITER);
				mut1.setTypeEntite(TypeEntiteRF.BATIMENT);
				mut1.setTypeMutation(TypeMutationRF.CREATION);
				mut1.setIdRF("8af806fc3b8f410e013c437c69a112ed");
				mut1.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						                   "<Gebaeude VersionID=\"8af806fc3b8f410e013c437c69a112ee\" MasterID=\"8af806fc3b8f410e013c437c69a112ed\" xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						                   "    <GrundstueckZuGebaeude>\n" +
						                   "        <GrundstueckIDREF>_1f109152381026b501381028bb23779a</GrundstueckIDREF>\n" +
						                   "        <AbschnittFlaeche>136</AbschnittFlaeche>\n" +
						                   "    </GrundstueckZuGebaeude>\n" +
						                   "    <KantGid>4073</KantGid>\n" +
						                   "    <Einzelobjekt>false</Einzelobjekt>\n" +
						                   "    <Unterirdisch>false</Unterirdisch>\n" +
						                   "    <MehrereGrundstuecke>false</MehrereGrundstuecke>\n" +
						                   "    <Flaeche>136</Flaeche>\n" +
						                   "    <Koordinaten>\n" +
						                   "        <QualitaetKoordinaten>nicht_vorhanden</QualitaetKoordinaten>\n" +
						                   "    </Koordinaten>\n" +
						                   "    <Versicherungsnummer>502</Versicherungsnummer>\n" +
						                   "</Gebaeude>\n");
				evenementRFMutationDAO.save(mut1);

				return importEvent.getId();
			}
		});
		assertNotNull(importId);

		// on déclenche le démarrage du job
		final HashMap<String, Object> params = new HashMap<>();
		params.put(TraiterMutationsRFJob.ID, importId);
		params.put(TraiterMutationsRFJob.NB_THREADS, 2);
		params.put(TraiterMutationsRFJob.CONTINUE_WITH_IDENTIFICATION_JOB, false);

		final JobDefinition job = batchScheduler.startJob(TraiterMutationsRFJob.NAME, params);
		assertNotNull(job);

		// le job doit se terminer correctement
		waitForJobCompletion(job);
		assertEquals(JobDefinition.JobStatut.JOB_OK, job.getStatut());

		// on vérifie que les mutations ont bien été traitées
		assertEtatMutations(2, EtatEvenementRF.TRAITE); // il y a 2 bâtiments différents dans le fichier d'import

		// on vérifie que les bâtiments ont bien été créés
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final BatimentRF batiment0 = batimentRFDAO.find(new BatimentRFKey("8af80e6254709f6801547708f4c10ebd"));
				assertNotNull(batiment0);
				assertEquals("8af80e6254709f6801547708f4c10ebd", batiment0.getMasterIdRF());
				assertEquals("Centrale électrique sur le domaine public (art. 20LICom) contigüe à la parcelle 554", batiment0.getType());
				assertEmpty(batiment0.getSurfaces());

				final Set<ImplantationRF> implantations0 = batiment0.getImplantations();
				assertEquals(1, implantations0.size());
				final ImplantationRF implantation00 = implantations0.iterator().next();
				assertEquals("_8af80e6254709f68015476fecb1f0e0b", implantation00.getImmeuble().getIdRF());
				assertEquals(Integer.valueOf(0), implantation00.getSurface());
				assertEquals(dateImport, implantation00.getDateDebut());
				assertNull(implantation00.getDateFin());

				final BatimentRF batiment1 = batimentRFDAO.find(new BatimentRFKey("8af806fc3b8f410e013c437c69a112ed"));
				assertNotNull(batiment1);
				assertEquals("8af806fc3b8f410e013c437c69a112ed", batiment1.getMasterIdRF());
				assertNull(batiment1.getType());

				final Set<SurfaceBatimentRF> surfaces1 = batiment1.getSurfaces();
				assertEquals(1, surfaces1.size());
				final SurfaceBatimentRF surface10 = surfaces1.iterator().next();
				assertEquals(136, surface10.getSurface());
				assertEquals(dateImport, surface10.getDateDebut());
				assertNull(surface10.getDateFin());

				final Set<ImplantationRF> implantations1 = batiment1.getImplantations();
				assertEquals(1, implantations1.size());
				final ImplantationRF implantation10 = implantations1.iterator().next();
				assertEquals("_1f109152381026b501381028bb23779a", implantation10.getImmeuble().getIdRF());
				assertEquals(Integer.valueOf(136), implantation10.getSurface());
				assertEquals(dateImport, implantation10.getDateDebut());
				assertNull(implantation10.getDateFin());
			}
		});
	}

	/**
	 * Ce test vérifie que les mutations de type MODIFICATION modifient bien des bâtiments existants
	 */
	@Test
	public void testTraiterMutationsModification() throws Exception {

		final RegDate dateImportInitial = RegDate.get(2010, 1, 1);
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		// on insère les données de l'import initial dans la base
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				BienFondRF immeuble1 = new BienFondRF();
				immeuble1.setIdRF("_1f109152381026b501381028a73d1852");
				immeuble1 = (BienFondRF) immeubleRFDAO.save(immeuble1);

				BienFondRF immeuble2 = new BienFondRF();
				immeuble2.setIdRF("_1f10915238106bdc0138106ff6d3305d");
				immeuble2 = (BienFondRF) immeubleRFDAO.save(immeuble2);

				BatimentRF batiment1 = new BatimentRF();
				batiment1.setMasterIdRF("1f109152381026b50138102aa28557e0");
				batiment1.setType("Habitation");
				batiment1.addImplantation(new ImplantationRF(104, immeuble1, dateImportInitial, null));
				batiment1.addSurface(new SurfaceBatimentRF(12003, dateImportInitial, null));
				batimentRFDAO.save(batiment1);

				BatimentRF batiment2 = new BatimentRF();
				batiment2.setMasterIdRF("1f10915238106bdc0138107364741e62");
				batiment2.setType("Garage");
				batiment2.addImplantation(new ImplantationRF(247, immeuble2, dateImportInitial, null));
				batiment2.addSurface(new SurfaceBatimentRF(247, dateImportInitial, null));
				batimentRFDAO.save(batiment2);
			}
		});

		// on insère les données du second import dans la base
		final Long importId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				EvenementRFImport importEvent = new EvenementRFImport();
				importEvent.setDateEvenement(dateSecondImport);
				importEvent.setEtat(EtatEvenementRF.TRAITE);
				importEvent.setFileUrl("http://turlututu");
				importEvent = evenementRFImportDAO.save(importEvent);

				final EvenementRFMutation mut0 = new EvenementRFMutation();
				mut0.setParentImport(importEvent);
				mut0.setEtat(EtatEvenementRF.A_TRAITER);
				mut0.setTypeEntite(TypeEntiteRF.BATIMENT);
				mut0.setTypeMutation(TypeMutationRF.MODIFICATION);
				mut0.setIdRF("1f109152381026b50138102aa28557e0");
				mut0.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						                   "<Gebaeude VersionID=\"1f109152381026b50138102aa2875806\" MasterID=\"1f109152381026b50138102aa28557e0\" xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						                   "    <GrundstueckZuGebaeude>\n" +
						                   "        <GrundstueckIDREF>_1f109152381026b501381028a73d1852</GrundstueckIDREF>\n" +
						                   "        <AbschnittFlaeche>104</AbschnittFlaeche>\n" +
						                   "    </GrundstueckZuGebaeude>\n" +
						                   "    <Einzelobjekt>false</Einzelobjekt>\n" +
						                   "    <Unterirdisch>false</Unterirdisch>\n" +
						                   "    <MehrereGrundstuecke>false</MehrereGrundstuecke>\n" +
						                   "    <GebaeudeArten>\n" +
						                   "        <GebaeudeArtCode>\n" +
						                   "            <TextDe>*Habitation</TextDe>\n" +
						                   "            <TextFr>Habitation</TextFr>\n" +
						                   "        </GebaeudeArtCode>\n" +
						                   "    </GebaeudeArten>\n" +
						                   "    <Versicherungsnummer>3064</Versicherungsnummer>\n" +
						                   "</Gebaeude>\n");
				evenementRFMutationDAO.save(mut0);

				final EvenementRFMutation mut1 = new EvenementRFMutation();
				mut1.setParentImport(importEvent);
				mut1.setEtat(EtatEvenementRF.A_TRAITER);
				mut1.setTypeEntite(TypeEntiteRF.BATIMENT);
				mut1.setTypeMutation(TypeMutationRF.MODIFICATION);
				mut1.setIdRF("1f10915238106bdc0138107364741e62");
				mut1.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						                   "<Gebaeude VersionID=\"1f10915238106bdc0138107364791e9d\" MasterID=\"1f10915238106bdc0138107364741e62\" xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						                   "    <GrundstueckZuGebaeude>\n" +
						                   "        <GrundstueckIDREF>_1f10915238106bdc0138106ff6d3305d</GrundstueckIDREF>\n" +
						                   "    </GrundstueckZuGebaeude>\n" +
						                   "    <Einzelobjekt>false</Einzelobjekt>\n" +
						                   "    <Unterirdisch>true</Unterirdisch>\n" +
						                   "    <MehrereGrundstuecke>false</MehrereGrundstuecke>\n" +
						                   "    <Flaeche>247</Flaeche>\n" +
						                   "    <GebaeudeArten>\n" +
						                   "        <GebaeudeArtCode>\n" +
						                   "            <TextDe>*Garage</TextDe>\n" +
						                   "            <TextFr>Garage</TextFr>\n" +
						                   "        </GebaeudeArtCode>\n" +
						                   "    </GebaeudeArten>\n" +
						                   "    <Versicherungsnummer>312b</Versicherungsnummer>\n" +
						                   "</Gebaeude>");
				evenementRFMutationDAO.save(mut1);

				return importEvent.getId();
			}
		});
		assertNotNull(importId);

		// on déclenche le démarrage du job
		final HashMap<String, Object> params = new HashMap<>();
		params.put(TraiterMutationsRFJob.ID, importId);
		params.put(TraiterMutationsRFJob.NB_THREADS, 2);
		params.put(TraiterMutationsRFJob.CONTINUE_WITH_IDENTIFICATION_JOB, false);

		final JobDefinition job = batchScheduler.startJob(TraiterMutationsRFJob.NAME, params);
		assertNotNull(job);

		// le job doit se terminer correctement
		waitForJobCompletion(job);
		assertEquals(JobDefinition.JobStatut.JOB_OK, job.getStatut());

		// on vérifie que les mutations ont bien été traitées
		assertEtatMutations(2, EtatEvenementRF.TRAITE);

		// on vérifie que les bâtiments ont bien été mis-à-jour
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final BatimentRF batiment0 = batimentRFDAO.find(new BatimentRFKey("1f109152381026b50138102aa28557e0"));
				assertNotNull(batiment0);
				assertEquals("1f109152381026b50138102aa28557e0", batiment0.getMasterIdRF());
				assertEquals("Habitation", batiment0.getType());

				// la surface existante est fermée
				final Set<SurfaceBatimentRF> surfaces0 = batiment0.getSurfaces();
				assertEquals(1, surfaces0.size());
				final SurfaceBatimentRF surface00 = surfaces0.iterator().next();
				assertEquals(12003, surface00.getSurface());
				assertEquals(dateImportInitial, surface00.getDateDebut());
				assertEquals(dateSecondImport.getOneDayBefore(), surface00.getDateFin());

				// pas de changement sur l'implantation
				final Set<ImplantationRF> implantations0 = batiment0.getImplantations();
				assertEquals(1, implantations0.size());
				final ImplantationRF implantation00 = implantations0.iterator().next();
				assertEquals("_1f109152381026b501381028a73d1852", implantation00.getImmeuble().getIdRF());
				assertEquals(Integer.valueOf(104), implantation00.getSurface());
				assertEquals(dateImportInitial, implantation00.getDateDebut());
				assertNull(implantation00.getDateFin());

				final BatimentRF batiment1 = batimentRFDAO.find(new BatimentRFKey("1f10915238106bdc0138107364741e62"));
				assertNotNull(batiment1);
				assertEquals("1f10915238106bdc0138107364741e62", batiment1.getMasterIdRF());
				assertEquals("Garage", batiment1.getType());

				// pas de changement sur la surface
				final Set<SurfaceBatimentRF> surfaces1 = batiment1.getSurfaces();
				assertEquals(1, surfaces1.size());
				final SurfaceBatimentRF surface10 = surfaces1.iterator().next();
				assertEquals(247, surface10.getSurface());
				assertEquals(dateImportInitial, surface10.getDateDebut());
				assertNull(surface10.getDateFin());

				// l'implantation existante est fermée et une nouvelle implantation a été créée
				final List<ImplantationRF> implantations1 = new ArrayList<>(batiment1.getImplantations());
				assertEquals(2, implantations1.size());
				Collections.sort(implantations1, new DateRangeComparator<>());

				final ImplantationRF implantation10 = implantations1.get(0);
				assertEquals("_1f10915238106bdc0138106ff6d3305d", implantation10.getImmeuble().getIdRF());
				assertEquals(Integer.valueOf(247), implantation10.getSurface());
				assertEquals(dateImportInitial, implantation10.getDateDebut());
				assertEquals(dateSecondImport.getOneDayBefore(), implantation10.getDateFin());

				final ImplantationRF implantation11 = implantations1.get(1);
				assertEquals("_1f10915238106bdc0138106ff6d3305d", implantation11.getImmeuble().getIdRF());
				assertNull(implantation11.getSurface());
				assertEquals(dateSecondImport, implantation11.getDateDebut());
				assertNull(implantation11.getDateFin());
			}
		});
	}

	/**
	 * Ce test vérifie que les mutations de type SUPPRESSION ferment bien les bâtiments existants
	 */
	@Test
	public void testTraiterMutationsSuppression() throws Exception {

		final RegDate dateImportInitial = RegDate.get(2010, 1, 1);
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		// on insère les données de l'import initial dans la base
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				BienFondRF immeuble1 = new BienFondRF();
				immeuble1.setIdRF("_1f109152381026b501381028a73d1852");
				immeuble1 = (BienFondRF) immeubleRFDAO.save(immeuble1);

				BienFondRF immeuble2 = new BienFondRF();
				immeuble2.setIdRF("_1f10915238106bdc0138106ff6d3305d");
				immeuble2 = (BienFondRF) immeubleRFDAO.save(immeuble2);

				BatimentRF batiment1 = new BatimentRF();
				batiment1.setMasterIdRF("1f109152381026b50138102aa28557e0");
				batiment1.setType("Habitation");
				batiment1.addImplantation(new ImplantationRF(104, immeuble1, dateImportInitial, null));
				batiment1.addSurface(new SurfaceBatimentRF(12003, dateImportInitial, null));
				batimentRFDAO.save(batiment1);

				BatimentRF batiment2 = new BatimentRF();
				batiment2.setMasterIdRF("1f10915238106bdc0138107364741e62");
				batiment2.setType("Garage");
				batiment2.addImplantation(new ImplantationRF(247, immeuble2, dateImportInitial, null));
				batiment2.addSurface(new SurfaceBatimentRF(247, dateImportInitial, null));
				batimentRFDAO.save(batiment2);
			}
		});

		// on insère les données du second import dans la base
		final Long importId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				EvenementRFImport importEvent = new EvenementRFImport();
				importEvent.setDateEvenement(dateSecondImport);
				importEvent.setEtat(EtatEvenementRF.TRAITE);
				importEvent.setFileUrl("http://turlututu");
				importEvent = evenementRFImportDAO.save(importEvent);

				// des mutations de types suppression, c'est-à-dire que le bâtiment n'existe plus dans l'import
				final EvenementRFMutation mut0 = new EvenementRFMutation();
				mut0.setParentImport(importEvent);
				mut0.setEtat(EtatEvenementRF.A_TRAITER);
				mut0.setTypeEntite(TypeEntiteRF.BATIMENT);
				mut0.setTypeMutation(TypeMutationRF.SUPPRESSION);
				mut0.setIdRF("1f109152381026b50138102aa28557e0");
				mut0.setXmlContent(null);
				evenementRFMutationDAO.save(mut0);

				final EvenementRFMutation mut1 = new EvenementRFMutation();
				mut1.setParentImport(importEvent);
				mut1.setEtat(EtatEvenementRF.A_TRAITER);
				mut1.setTypeEntite(TypeEntiteRF.BATIMENT);
				mut1.setTypeMutation(TypeMutationRF.SUPPRESSION);
				mut1.setIdRF("1f10915238106bdc0138107364741e62");
				mut1.setXmlContent(null);
				evenementRFMutationDAO.save(mut1);
				return importEvent.getId();
			}
		});
		assertNotNull(importId);

		// on déclenche le démarrage du job
		final HashMap<String, Object> params = new HashMap<>();
		params.put(TraiterMutationsRFJob.ID, importId);
		params.put(TraiterMutationsRFJob.NB_THREADS, 2);
		params.put(TraiterMutationsRFJob.CONTINUE_WITH_IDENTIFICATION_JOB, false);

		final JobDefinition job = batchScheduler.startJob(TraiterMutationsRFJob.NAME, params);
		assertNotNull(job);

		// le job doit se terminer correctement
		waitForJobCompletion(job);
		assertEquals(JobDefinition.JobStatut.JOB_OK, job.getStatut());

		// on vérifie que les mutations ont bien été traitées
		assertEtatMutations(2, EtatEvenementRF.TRAITE);

		// on vérifie que les implantations et les surfaces des bâtiments ont bien été fermées
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final BatimentRF batiment0 = batimentRFDAO.find(new BatimentRFKey("1f109152381026b50138102aa28557e0"));
				assertNotNull(batiment0);
				assertEquals("1f109152381026b50138102aa28557e0", batiment0.getMasterIdRF());
				assertEquals("Habitation", batiment0.getType());

				// la surface existante est fermée
				final Set<SurfaceBatimentRF> surfaces0 = batiment0.getSurfaces();
				assertEquals(1, surfaces0.size());
				final SurfaceBatimentRF surface00 = surfaces0.iterator().next();
				assertEquals(12003, surface00.getSurface());
				assertEquals(dateImportInitial, surface00.getDateDebut());
				assertEquals(dateSecondImport.getOneDayBefore(), surface00.getDateFin());

				// l'implantation existante est fermée
				final Set<ImplantationRF> implantations0 = batiment0.getImplantations();
				assertEquals(1, implantations0.size());
				final ImplantationRF implantation00 = implantations0.iterator().next();
				assertEquals("_1f109152381026b501381028a73d1852", implantation00.getImmeuble().getIdRF());
				assertEquals(Integer.valueOf(104), implantation00.getSurface());
				assertEquals(dateImportInitial, implantation00.getDateDebut());
				assertEquals(dateSecondImport.getOneDayBefore(), implantation00.getDateFin());

				final BatimentRF batiment1 = batimentRFDAO.find(new BatimentRFKey("1f10915238106bdc0138107364741e62"));
				assertNotNull(batiment1);
				assertEquals("1f10915238106bdc0138107364741e62", batiment1.getMasterIdRF());
				assertEquals("Garage", batiment1.getType());

				// la surface existante est fermée
				final Set<SurfaceBatimentRF> surfaces1 = batiment1.getSurfaces();
				assertEquals(1, surfaces1.size());
				final SurfaceBatimentRF surface10 = surfaces1.iterator().next();
				assertEquals(247, surface10.getSurface());
				assertEquals(dateImportInitial, surface10.getDateDebut());
				assertEquals(dateSecondImport.getOneDayBefore(), surface10.getDateFin());

				// l'implantation existante est fermée
				final Set<ImplantationRF> implantations1 = batiment1.getImplantations();
				assertEquals(1, implantations1.size());

				final ImplantationRF implantation10 = implantations1.iterator().next();
				assertEquals("_1f10915238106bdc0138106ff6d3305d", implantation10.getImmeuble().getIdRF());
				assertEquals(Integer.valueOf(247), implantation10.getSurface());
				assertEquals(dateImportInitial, implantation10.getDateDebut());
				assertEquals(dateSecondImport.getOneDayBefore(), implantation10.getDateFin());
			}
		});
	}
}