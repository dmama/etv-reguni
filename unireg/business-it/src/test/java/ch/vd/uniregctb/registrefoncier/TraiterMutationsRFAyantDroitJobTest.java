package ch.vd.uniregctb.registrefoncier;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImport;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.key.AyantDroitRFKey;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobDefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TraiterMutationsRFAyantDroitJobTest extends ImportRFTestClass {

	private BatchScheduler batchScheduler;
	private EvenementRFImportDAO evenementRFImportDAO;
	private EvenementRFMutationDAO evenementRFMutationDAO;
	private AyantDroitRFDAO ayantDroitRFDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		batchScheduler = getBean(BatchScheduler.class, "batchScheduler");
		evenementRFImportDAO = getBean(EvenementRFImportDAO.class, "evenementRFImportDAO");
		evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");
		ayantDroitRFDAO = getBean(AyantDroitRFDAO.class, "ayantDroitRFDAO");
	}

	/**
	 * Ce test vérifie que les mutations de type CREATION sont bien créées lorsqu'on importe un fichier RF sur une base vide
	 */
	@Test
	public void testTraiterMutationsCreation() throws Exception {

		// on insère les données de l'import dans la base
		final Long importId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				EvenementRFImport importEvent = new EvenementRFImport();
				importEvent.setDateEvenement(RegDate.get(2016, 10, 1));
				importEvent.setEtat(EtatEvenementRF.TRAITE);
				importEvent.setFileUrl("http://turlututu");
				importEvent = evenementRFImportDAO.save(importEvent);

				final EvenementRFMutation mut0 = new EvenementRFMutation();
				mut0.setParentImport(importEvent);
				mut0.setEtat(EtatEvenementRF.A_TRAITER);
				mut0.setTypeEntite(EvenementRFMutation.TypeEntite.AYANT_DROIT);
				mut0.setTypeMutation(EvenementRFMutation.TypeMutation.CREATION);
				mut0.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						                   " <NatuerlichePersonstamm xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						                   "     <PersonstammID>3893728273382823</PersonstammID>\n" +
						                   "     <Name>Nom</Name>\n" +
						                   "     <Gueltig>false</Gueltig>\n" +
						                   "     <NoRF>3727</NoRF>\n" +
						                   "     <Vorname>Prénom</Vorname>\n" +
						                   "     <Geburtsdatum>\n" +
						                   "         <Tag>23</Tag>\n" +
						                   "         <Monat>1</Monat>\n" +
						                   "         <Jahr>1956</Jahr>\n" +
						                   "     </Geburtsdatum>\n" +
						                   "     <NrIROLE>827288022</NrIROLE>\n" +
						                   " </NatuerlichePersonstamm>\n\n");
				evenementRFMutationDAO.save(mut0);

				final EvenementRFMutation mut1 = new EvenementRFMutation();
				mut1.setParentImport(importEvent);
				mut1.setEtat(EtatEvenementRF.A_TRAITER);
				mut1.setTypeEntite(EvenementRFMutation.TypeEntite.AYANT_DROIT);
				mut1.setTypeMutation(EvenementRFMutation.TypeMutation.CREATION);
				mut1.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						                   "<JuristischePersonstamm xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						                   "    <PersonstammID>48349384890202</PersonstammID>\n" +
						                   "    <Name>Raison sociale</Name>\n" +
						                   "    <Gueltig>false</Gueltig>\n" +
						                   "    <NrACI>827288022</NrACI>\n" +
						                   "    <NoRF>3727</NoRF>\n" +
						                   "    <Unterart>SchweizerischeJuristischePerson</Unterart>\n" +
						                   "</JuristischePersonstamm>\n");
				evenementRFMutationDAO.save(mut1);

				final EvenementRFMutation mut2 = new EvenementRFMutation();
				mut2.setParentImport(importEvent);
				mut2.setEtat(EtatEvenementRF.A_TRAITER);
				mut2.setTypeEntite(EvenementRFMutation.TypeEntite.AYANT_DROIT);
				mut2.setTypeMutation(EvenementRFMutation.TypeMutation.CREATION);
				mut2.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						                   "<JuristischePersonstamm xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						                   "    <PersonstammID>574739202303482</PersonstammID>\n" +
						                   "    <Name>Raison sociale</Name>\n" +
						                   "    <Gueltig>false</Gueltig>\n" +
						                   "    <NrACI>827288022</NrACI>\n" +
						                   "    <NoRF>3727</NoRF>\n" +
						                   "    <Unterart>OeffentlicheKoerperschaft</Unterart>\n" +
						                   "</JuristischePersonstamm>\n");
				evenementRFMutationDAO.save(mut2);

				return importEvent.getId();
			}
		});
		assertNotNull(importId);

		// on déclenche le démarrage du job
		final HashMap<String, Object> params = new HashMap<>();
		params.put(TraiterMutationsRFJob.ID, importId);
		params.put(TraiterMutationsRFJob.NB_THREADS, 2);

		final JobDefinition job = batchScheduler.startJob(TraiterMutationsRFJob.NAME, params);
		assertNotNull(job);

		// le job doit se terminer correctement
		waitForJobCompletion(job);
		assertEquals(JobDefinition.JobStatut.JOB_OK, job.getStatut());

		// on vérifie que les mutations ont bien été traitées
		assertEtatMutations(3, EtatEvenementRF.TRAITE); // il y a 3 ayants-droits dans le fichier d'import

		// on vérifie que les ayants-droits ont bien été créées
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final PersonnePhysiqueRF pp = (PersonnePhysiqueRF) ayantDroitRFDAO.find(new AyantDroitRFKey("3893728273382823"));
				assertNotNull(pp);
				assertEquals("3893728273382823", pp.getIdRF());
				assertEquals(3727L, pp.getNoRF());
				assertEquals(Long.valueOf(827288022L), pp.getNoContribuable());
				assertEquals("Nom", pp.getNom());
				assertEquals("Prénom", pp.getPrenom());
				assertEquals(RegDate.get(1956, 1, 23), pp.getDateNaissance());

				final PersonneMoraleRF pm = (PersonneMoraleRF) ayantDroitRFDAO.find(new AyantDroitRFKey("48349384890202"));
				assertNotNull(pm);
				assertEquals("48349384890202", pm.getIdRF());
				assertEquals(3727L, pm.getNoRF());
				assertEquals(Long.valueOf(827288022L), pm.getNoContribuable());
				assertEquals("Raison sociale", pm.getRaisonSociale());

				final CollectivitePubliqueRF coll = (CollectivitePubliqueRF) ayantDroitRFDAO.find(new AyantDroitRFKey("574739202303482"));
				assertNotNull(coll);
				assertEquals("574739202303482", coll.getIdRF());
				assertEquals(3727L, coll.getNoRF());
				assertEquals(Long.valueOf(827288022L), coll.getNoContribuable());
				assertEquals("Raison sociale", coll.getRaisonSociale());
			}
		});
	}

	/**
	 * Ce test vérifie que des mutations sont bien créées lorsqu'on importe un fichier RF et que les immeubles dans la base ne correspondent pas.
	 */
	@Test
	public void testTraiterMutationsModification() throws Exception {

		final RegDate dateImport = RegDate.get(2016, 10, 1);

		// on insère les données de l'import initial dans la base
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				// données partiellement différentes de celles du fichier export_ayantsdroits_rf_hebdo.xm.xml
				//  - no RF différent
				final PersonnePhysiqueRF pp = newPersonnePhysique("3893728273382823", 48322L, 827288022L, "Nom", "Prénom", RegDate.get(1956, 1, 23));
				// - raison sociale différente
				final PersonneMoraleRF pm = newPersonneMorale("48349384890202", 3727L, 827288022L, "Raison sociale différente");
				// - no CTB différent
				final CollectivitePubliqueRF coll = newCollectivitePublique("574739202303482", 3727L, 584323450L, "Raison sociale");
				ayantDroitRFDAO.save(pp);
				ayantDroitRFDAO.save(pm);
				ayantDroitRFDAO.save(coll);
			}
		});

		// on insère les données du second import dans la base
		final Long importId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				EvenementRFImport importEvent = new EvenementRFImport();
				importEvent.setDateEvenement(dateImport);
				importEvent.setEtat(EtatEvenementRF.TRAITE);
				importEvent.setFileUrl("http://turlututu");
				importEvent = evenementRFImportDAO.save(importEvent);

				final EvenementRFMutation mut0 = new EvenementRFMutation();
				mut0.setParentImport(importEvent);
				mut0.setEtat(EtatEvenementRF.A_TRAITER);
				mut0.setTypeEntite(EvenementRFMutation.TypeEntite.AYANT_DROIT);
				mut0.setTypeMutation(EvenementRFMutation.TypeMutation.MODIFICATION);
				mut0.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						                   " <NatuerlichePersonstamm xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						                   "     <PersonstammID>3893728273382823</PersonstammID>\n" +
						                   "     <Name>Nom</Name>\n" +
						                   "     <Gueltig>false</Gueltig>\n" +
						                   "     <NoRF>3727</NoRF>\n" +
						                   "     <Vorname>Prénom</Vorname>\n" +
						                   "     <Geburtsdatum>\n" +
						                   "         <Tag>23</Tag>\n" +
						                   "         <Monat>1</Monat>\n" +
						                   "         <Jahr>1956</Jahr>\n" +
						                   "     </Geburtsdatum>\n" +
						                   "     <NrIROLE>827288022</NrIROLE>\n" +
						                   " </NatuerlichePersonstamm>\n\n");
				evenementRFMutationDAO.save(mut0);

				final EvenementRFMutation mut1 = new EvenementRFMutation();
				mut1.setParentImport(importEvent);
				mut1.setEtat(EtatEvenementRF.A_TRAITER);
				mut1.setTypeEntite(EvenementRFMutation.TypeEntite.AYANT_DROIT);
				mut1.setTypeMutation(EvenementRFMutation.TypeMutation.MODIFICATION);
				mut1.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						                   "<JuristischePersonstamm xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						                   "    <PersonstammID>48349384890202</PersonstammID>\n" +
						                   "    <Name>Raison sociale</Name>\n" +
						                   "    <Gueltig>false</Gueltig>\n" +
						                   "    <NrACI>827288022</NrACI>\n" +
						                   "    <NoRF>3727</NoRF>\n" +
						                   "    <Unterart>SchweizerischeJuristischePerson</Unterart>\n" +
						                   "</JuristischePersonstamm>\n");
				evenementRFMutationDAO.save(mut1);

				final EvenementRFMutation mut2 = new EvenementRFMutation();
				mut2.setParentImport(importEvent);
				mut2.setEtat(EtatEvenementRF.A_TRAITER);
				mut2.setTypeEntite(EvenementRFMutation.TypeEntite.AYANT_DROIT);
				mut2.setTypeMutation(EvenementRFMutation.TypeMutation.MODIFICATION);
				mut2.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						                   "<JuristischePersonstamm xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						                   "    <PersonstammID>574739202303482</PersonstammID>\n" +
						                   "    <Name>Raison sociale</Name>\n" +
						                   "    <Gueltig>false</Gueltig>\n" +
						                   "    <NrACI>827288022</NrACI>\n" +
						                   "    <NoRF>3727</NoRF>\n" +
						                   "    <Unterart>OeffentlicheKoerperschaft</Unterart>\n" +
						                   "</JuristischePersonstamm>\n");
				evenementRFMutationDAO.save(mut2);

				return importEvent.getId();
			}
		});
		assertNotNull(importId);

		// on déclenche le démarrage du job
		final HashMap<String, Object> params = new HashMap<>();
		params.put(TraiterMutationsRFJob.ID, importId);
		params.put(TraiterMutationsRFJob.NB_THREADS, 2);

		final JobDefinition job = batchScheduler.startJob(TraiterMutationsRFJob.NAME, params);
		assertNotNull(job);

		// le job doit se terminer correctement
		waitForJobCompletion(job);
		assertEquals(JobDefinition.JobStatut.JOB_OK, job.getStatut());

		// on vérifie que les mutations ont bien été traitées
		assertEtatMutations(3, EtatEvenementRF.TRAITE);

		// on vérifie que les immeubles ont bien été mis-à-jour
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				// le no RF a changé
				final PersonnePhysiqueRF pp = (PersonnePhysiqueRF) ayantDroitRFDAO.find(new AyantDroitRFKey("3893728273382823"));
				assertNotNull(pp);
				assertEquals("3893728273382823", pp.getIdRF());
				assertEquals(3727L, pp.getNoRF());
				assertEquals(Long.valueOf(827288022L), pp.getNoContribuable());
				assertEquals("Nom", pp.getNom());
				assertEquals("Prénom", pp.getPrenom());
				assertEquals(RegDate.get(1956, 1, 23), pp.getDateNaissance());

				// la raison sociale a changé
				final PersonneMoraleRF pm = (PersonneMoraleRF) ayantDroitRFDAO.find(new AyantDroitRFKey("48349384890202"));
				assertNotNull(pm);
				assertEquals("48349384890202", pm.getIdRF());
				assertEquals(3727L, pm.getNoRF());
				assertEquals(Long.valueOf(827288022L), pm.getNoContribuable());
				assertEquals("Raison sociale", pm.getRaisonSociale());

				// le numéro de contribuable a changé
				final CollectivitePubliqueRF coll = (CollectivitePubliqueRF) ayantDroitRFDAO.find(new AyantDroitRFKey("574739202303482"));
				assertNotNull(coll);
				assertEquals("574739202303482", coll.getIdRF());
				assertEquals(3727L, coll.getNoRF());
				assertEquals(Long.valueOf(827288022L), coll.getNoContribuable());
				assertEquals("Raison sociale", coll.getRaisonSociale());
			}
		});
	}

	private void assertEtatMutations(final int count, final EtatEvenementRF etat) throws Exception {
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
				assertEquals(count, mutations.size());
				Collections.sort(mutations, (o1, o2) -> o1.getId().compareTo(o2.getId()));
				for (EvenementRFMutation mutation : mutations) {
					assertEquals(etat, mutation.getEtat());
				}
			}
		});
	}
}