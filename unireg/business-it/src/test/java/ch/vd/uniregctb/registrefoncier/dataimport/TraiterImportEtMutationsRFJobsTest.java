package ch.vd.uniregctb.registrefoncier.dataimport;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.technical.esb.store.raft.ZipRaftEsbStore;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImport;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.uniregctb.evenement.registrefoncier.TypeMutationRF;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.BatimentRF;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.DescriptionBatimentRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.EstimationRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.ImplantationRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.SituationRF;
import ch.vd.uniregctb.registrefoncier.SurfaceTotaleRF;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.BatimentRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.CommuneRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.DroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobDefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TraiterImportEtMutationsRFJobsTest extends ImportRFTestClass {

	private BatchScheduler batchScheduler;
	private EvenementRFImportDAO evenementRFImportDAO;
	private EvenementRFMutationDAO evenementRFMutationDAO;
	private AyantDroitRFDAO ayantDroitRFDAO;
	private DroitRFDAO droitRFDAO;
	private ImmeubleRFDAO immeubleRFDAO;
	private BatimentRFDAO batimentRFDAO;
	private CommuneRFDAO communeRFDAO;
	private ZipRaftEsbStore zipRaftEsbStore;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		batchScheduler = getBean(BatchScheduler.class, "batchScheduler");
		evenementRFImportDAO = getBean(EvenementRFImportDAO.class, "evenementRFImportDAO");
		evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");
		ayantDroitRFDAO = getBean(AyantDroitRFDAO.class, "ayantDroitRFDAO");
		droitRFDAO = getBean(DroitRFDAO.class, "droitRFDAO");
		immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		batimentRFDAO = getBean(BatimentRFDAO.class, "batimentRFDAO");
		communeRFDAO = getBean(CommuneRFDAO.class, "communeRFDAO");
		zipRaftEsbStore = getBean(ZipRaftEsbStore.class, "zipRaftEsbStore");
	}

	/**
	 * Ce test vérifie que l'import d'un immeuble et de toutes ses dépendances fonctionne bien.
	 */
	@Test
	public void testTraiterCreationImmeuble() throws Exception {

		final RegDate dateImportInitial = RegDate.get(2010, 1, 1);

		// on va chercher le fichier d'import
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/export_un_immeuble_complet_rf.xml");
		assertNotNull(importFile);

		// on l'upload dans Raft
		final String raftUrl;
		try (FileInputStream is = new FileInputStream(importFile)) {
			raftUrl = zipRaftEsbStore.store("Fiscalite", "UnitTest", "Unireg", is);
		}
		assertNotNull(raftUrl);

		// on insère les données de l'import dans la base
		final Long importId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final EvenementRFImport importEvent = new EvenementRFImport();
				importEvent.setDateEvenement(dateImportInitial);
				importEvent.setEtat(EtatEvenementRF.A_TRAITER);
				importEvent.setFileUrl(raftUrl);
				return evenementRFImportDAO.save(importEvent).getId();
			}
		});
		assertNotNull(importId);

		// on déclenche le démarrage du job
		{
			final HashMap<String, Object> params = new HashMap<>();
			params.put(TraiterImportRFJob.ID, importId);
			params.put(TraiterImportRFJob.NB_THREADS, 2);
			params.put(TraiterImportRFJob.CONTINUE_WITH_MUTATIONS_JOB, false);

			final JobDefinition job = batchScheduler.startJob(TraiterImportRFJob.NAME, params);
			assertNotNull(job);

			// le job doit se terminer correctement
			waitForJobCompletion(job);
			assertEquals(JobDefinition.JobStatut.JOB_OK, job.getStatut());
		}

		// on vérifie que l'import est bien passé au statut TRAITE
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFImport importEvent = evenementRFImportDAO.get(importId);
				assertNotNull(importEvent);
				assertEquals(EtatEvenementRF.TRAITE, importEvent.getEtat());
			}
		});

		// on vérifie que les mutations attendues sont bien dans la DB
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
				assertEquals(6, mutations.size());    // il y a 3 communes + 4 immeubles dans le fichier d'import et la DB était vide
				Collections.sort(mutations, new MutationComparator());

				final EvenementRFMutation mut0 = mutations.get(0);
				assertEquals(importId, mut0.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
				assertEquals(TypeEntiteRF.AYANT_DROIT, mut0.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut0.getTypeMutation());
				assertEquals("_1f1091523810039001381003da8b72ac", mut0.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<NatuerlichePersonstamm xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <PersonstammID>_1f1091523810039001381003da8b72ac</PersonstammID>\n" +
						             "    <Name>Claude</Name>\n" +
						             "    <Gueltig>true</Gueltig>\n" +
						             "    <ClientRegulier>false</ClientRegulier>\n" +
						             "    <NoSCC>0</NoSCC>\n" +
						             "    <Status>definitiv</Status>\n" +
						             "    <Sprache>\n" +
						             "        <TextDe>Französisch</TextDe>\n" +
						             "        <TextFr>Français</TextFr>\n" +
						             "    </Sprache>\n" +
						             "    <Anrede>\n" +
						             "        <TextDe>*Monsieur</TextDe>\n" +
						             "        <TextFr>Monsieur</TextFr>\n" +
						             "    </Anrede>\n" +
						             "    <NrACI>0</NrACI>\n" +
						             "    <NoRF>149888</NoRF>\n" +
						             "    <Vorname>Daniel</Vorname>\n" +
						             "    <Zivilstand>unbekannt</Zivilstand>\n" +
						             "    <Geburtsdatum>\n" +
						             "        <Tag>7</Tag>\n" +
						             "        <Monat>2</Monat>\n" +
						             "        <Jahr>1945</Jahr>\n" +
						             "    </Geburtsdatum>\n" +
						             "    <NameDerEltern>Joseph</NameDerEltern>\n" +
						             "    <Geschlecht>unbekannt</Geschlecht>\n" +
						             "    <WeitereVornamen>François</WeitereVornamen>\n" +
						             "    <NrIROLE>10131016</NrIROLE>\n" +
						             "</NatuerlichePersonstamm>\n", mut0.getXmlContent());

				final EvenementRFMutation mut1 = mutations.get(1);
				assertEquals(importId, mut1.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
				assertEquals(TypeEntiteRF.DROIT, mut1.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut1.getTypeMutation());
				assertEquals("_1f1091523810039001381003da8b72ac", mut1.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<PersonEigentumAnteilList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <PersonEigentumAnteil VersionID=\"1f10915238100390013810067ae35d4a\" MasterID=\"1f1091523810039001381005be485efd\">\n" +
						             "        <Quote>\n" +
						             "            <AnteilZaehler>1</AnteilZaehler>\n" +
						             "            <AnteilNenner>1</AnteilNenner>\n" +
						             "            <QuoteUnbekannt>false</QuoteUnbekannt>\n" +
						             "        </Quote>\n" +
						             "        <BelastetesGrundstueckIDREF>_1f10915238100390013810052537624b</BelastetesGrundstueckIDREF>\n" +
						             "        <NatuerlichePersonGb VersionID=\"1f1091523810039001381005be4b5f2f\" MasterID=\"1f10915238100390013810067ae35d4b\">\n" +
						             "            <Name>Claude</Name>\n" +
						             "            <Status>definitiv</Status>\n" +
						             "            <Rechtsgruende>\n" +
						             "                <AmtNummer>3</AmtNummer>\n" +
						             "                <RechtsgrundCode>\n" +
						             "                    <TextDe>*Achat</TextDe>\n" +
						             "                    <TextFr>Achat</TextFr>\n" +
						             "                </RechtsgrundCode>\n" +
						             "                <BelegDatum>1997-06-19</BelegDatum>\n" +
						             "                <BelegAlt>74'677</BelegAlt>\n" +
						             "            </Rechtsgruende>\n" +
						             "            <Vorname>Daniel</Vorname>\n" +
						             "            <Geburtsdatum>\n" +
						             "                <Tag>7</Tag>\n" +
						             "                <Monat>2</Monat>\n" +
						             "                <Jahr>1945</Jahr>\n" +
						             "            </Geburtsdatum>\n" +
						             "            <Zivilstand>unbekannt</Zivilstand>\n" +
						             "            <NameEltern>Joseph</NameEltern>\n" +
						             "            <WeitereVornamen>François</WeitereVornamen>\n" +
						             "            <PersonstammIDREF>_1f1091523810039001381003da8b72ac</PersonstammIDREF>\n" +
						             "        </NatuerlichePersonGb>\n" +
						             "        <PersonEigentumsForm>alleineigentum</PersonEigentumsForm>\n" +
						             "        <AnzahlPaquiers>0</AnzahlPaquiers>\n" +
						             "    </PersonEigentumAnteil>\n" +
						             "</PersonEigentumAnteilList>\n", mut1.getXmlContent());

				final EvenementRFMutation mut2 = mutations.get(2);
				assertEquals(importId, mut2.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut2.getEtat());
				assertEquals(TypeEntiteRF.IMMEUBLE, mut2.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut2.getTypeMutation());
				assertEquals("_1f10915238100390013810052537624b", mut2.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<Liegenschaft xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <GrundstueckID>_1f10915238100390013810052537624b</GrundstueckID>\n" +
						             "    <EGrid>CH507045198314</EGrid>\n" +
						             "    <GrundstueckNummer VersionID=\"1f1091523810039001381005253a62d1\">\n" +
						             "        <BfsNr>38</BfsNr>\n" +
						             "        <Gemeindenamen>Cudrefin</Gemeindenamen>\n" +
						             "        <StammNr>1365</StammNr>\n" +
						             "    </GrundstueckNummer>\n" +
						             "    <IstKopie>false</IstKopie>\n" +
						             "    <AmtlicheBewertung VersionID=\"1f109152381003900138100691a02df3\">\n" +
						             "        <AmtlicherWert>30000</AmtlicherWert>\n" +
						             "        <Ertragswert>0</Ertragswert>\n" +
						             "        <ProtokollNr>2002</ProtokollNr>\n" +
						             "        <ProtokollGueltig>true</ProtokollGueltig>\n" +
						             "        <MitEwKomponente>unbekannt</MitEwKomponente>\n" +
						             "    </AmtlicheBewertung>\n" +
						             "    <GrundbuchFuehrung>Eidgenoessisch</GrundbuchFuehrung>\n" +
						             "    <BeschreibungUebergeben>true</BeschreibungUebergeben>\n" +
						             "    <EigentumUebergeben>true</EigentumUebergeben>\n" +
						             "    <PfandrechtUebergeben>true</PfandrechtUebergeben>\n" +
						             "    <DienstbarkeitUebergeben>true</DienstbarkeitUebergeben>\n" +
						             "    <GrundlastUebergeben>true</GrundlastUebergeben>\n" +
						             "    <AnmerkungUebergeben>true</AnmerkungUebergeben>\n" +
						             "    <VormerkungUebergeben>true</VormerkungUebergeben>\n" +
						             "    <Bereinigungsmarkierung>false</Bereinigungsmarkierung>\n" +
						             "    <BereitZurVerifikation>false</BereitZurVerifikation>\n" +
						             "    <NutzungLandwirtschaft>nein</NutzungLandwirtschaft>\n" +
						             "    <NutzungWald>unbekannt</NutzungWald>\n" +
						             "    <NutzungEisenbahn>nein</NutzungEisenbahn>\n" +
						             "    <NutzungVerwaltungsvermoegen>nein</NutzungVerwaltungsvermoegen>\n" +
						             "    <GrundstueckFlaeche VersionID=\"1f109152381003900138100740923152\" MasterID=\"1f109152381003900138100740923151\">\n" +
						             "        <Flaeche>614</Flaeche>\n" +
						             "        <Qualitaet>\n" +
						             "            <TextDe>*numérique</TextDe>\n" +
						             "            <TextFr>numérique</TextFr>\n" +
						             "        </Qualitaet>\n" +
						             "        <ProjektMutation>false</ProjektMutation>\n" +
						             "        <GeometrischDarstellbar>false</GeometrischDarstellbar>\n" +
						             "        <UeberlagerndeRechte>false</UeberlagerndeRechte>\n" +
						             "    </GrundstueckFlaeche>\n" +
						             "</Liegenschaft>\n", mut2.getXmlContent());

				final EvenementRFMutation mut3 = mutations.get(3);
				assertEquals(importId, mut3.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut3.getEtat());
				assertEquals(TypeEntiteRF.SURFACE_AU_SOL, mut3.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut3.getTypeMutation());
				assertEquals("_1f10915238100390013810052537624b", mut3.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<BodenbedeckungList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <Bodenbedeckung VersionID=\"1f10915238100390013810064c8d2e82\">\n" +
						             "        <GrundstueckIDREF>_1f10915238100390013810052537624b</GrundstueckIDREF>\n" +
						             "        <Art>\n" +
						             "            <TextDe>*Place-jardin</TextDe>\n" +
						             "            <TextFr>Place-jardin</TextFr>\n" +
						             "        </Art>\n" +
						             "        <Flaeche>564</Flaeche>\n" +
						             "    </Bodenbedeckung>\n" +
						             "</BodenbedeckungList>\n", mut3.getXmlContent());

				final EvenementRFMutation mut4 = mutations.get(4);
				assertEquals(importId, mut4.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut4.getEtat());
				assertEquals(TypeEntiteRF.BATIMENT, mut4.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut4.getTypeMutation());
				assertEquals("1f1091523810039001381006e05770ac", mut4.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<Gebaeude VersionID=\"1f1091523810039001381006e05a70e0\" MasterID=\"1f1091523810039001381006e05770ac\" xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <GrundstueckZuGebaeude>\n" +
						             "        <GrundstueckIDREF>_1f10915238100390013810052537624b</GrundstueckIDREF>\n" +
						             "        <AbschnittFlaeche>50</AbschnittFlaeche>\n" +
						             "    </GrundstueckZuGebaeude>\n" +
						             "    <Einzelobjekt>false</Einzelobjekt>\n" +
						             "    <Unterirdisch>false</Unterirdisch>\n" +
						             "    <MehrereGrundstuecke>false</MehrereGrundstuecke>\n" +
						             "    <GebaeudeArten>\n" +
						             "        <GebaeudeArtCode>\n" +
						             "            <TextDe>*Bâtiment</TextDe>\n" +
						             "            <TextFr>Bâtiment</TextFr>\n" +
						             "        </GebaeudeArtCode>\n" +
						             "    </GebaeudeArten>\n" +
						             "    <Versicherungsnummer>486</Versicherungsnummer>\n" +
						             "</Gebaeude>\n", mut4.getXmlContent());

				final EvenementRFMutation mut5 = mutations.get(5);
				assertEquals(importId, mut5.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut5.getEtat());
				assertEquals(TypeEntiteRF.COMMUNE, mut5.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut5.getTypeMutation());
				assertEquals("38", mut5.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<GrundstueckNummer xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <BfsNr>38</BfsNr>\n" +
						             "    <Gemeindenamen>Cudrefin</Gemeindenamen>\n" +
						             "    <StammNr>0</StammNr>\n" +
						             "</GrundstueckNummer>\n", mut5.getXmlContent());
			}
		});


		// on déclenche le démarrage du job de traitement
		{
			final HashMap<String, Object> params = new HashMap<>();
			params.put(TraiterMutationsRFJob.ID, importId);
			params.put(TraiterMutationsRFJob.NB_THREADS, 2);
			params.put(TraiterMutationsRFJob.CONTINUE_WITH_IDENTIFICATION_JOB, Boolean.FALSE);

			final JobDefinition job = batchScheduler.startJob(TraiterMutationsRFJob.NAME, params);
			assertNotNull(job);

			// le job doit se terminer correctement
			waitForJobCompletion(job);
			assertEquals(JobDefinition.JobStatut.JOB_OK, job.getStatut());
		}

		// on vérifie que les mutations ont bien été traitées
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
				assertEquals(6, mutations.size());
				Collections.sort(mutations, new MutationComparator());
				mutations.forEach(m -> {
					assertEquals(EtatEvenementRF.TRAITE, m.getEtat());
				});
			}
		});

		// on vérifie que les données ont bien été importées
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final List<AyantDroitRF> ayantDroits = ayantDroitRFDAO.getAll();
				assertEquals(1, ayantDroits.size());

				final PersonnePhysiqueRF pp = (PersonnePhysiqueRF) ayantDroits.get(0);
				assertNotNull(pp);
				{
					assertEquals("Claude", pp.getNom());
					assertEquals("Daniel", pp.getPrenom());
					assertEquals(RegDate.get(1945, 2, 7), pp.getDateNaissance());
					assertEquals(149888, pp.getNoRF());
					assertEquals(Long.valueOf(10131016), pp.getNoContribuable());
				}

				final List<DroitRF> droits = droitRFDAO.getAll();
				assertEquals(1, droits.size());

				final DroitProprietePersonnePhysiqueRF droit0 = (DroitProprietePersonnePhysiqueRF) droits.get(0);
				{
					assertEquals(dateImportInitial, droit0.getDateDebut());
					assertNull(droit0.getDateFin());
					assertEquals(RegDate.get(1997, 6, 19), droit0.getDateDebutOfficielle());
					assertEquals("Achat", droit0.getMotifDebut());
					assertNull(droit0.getMotifFin());
					assertEquals("1f1091523810039001381005be485efd", droit0.getMasterIdRF());
					assertEquals("_1f10915238100390013810052537624b", droit0.getImmeuble().getIdRF());
					assertEquals(new IdentifiantAffaireRF(3, null, null, null), droit0.getNumeroAffaire());
					assertEquals(new Fraction(1, 1), droit0.getPart());
					assertEquals(GenrePropriete.INDIVIDUELLE, droit0.getRegime());
				}

				final List<ImmeubleRF> immeubles = immeubleRFDAO.getAll();
				assertEquals(1, immeubles.size());

				final BienFondRF bienFond = (BienFondRF) immeubles.get(0);
				assertNotNull(bienFond);
				{
					assertEquals("_1f10915238100390013810052537624b", bienFond.getIdRF());
					assertEquals("CH507045198314", bienFond.getEgrid());
					assertFalse(bienFond.isCfa());
					assertNull(bienFond.getDateRadiation());

					final Set<SituationRF> situations = bienFond.getSituations();
					assertEquals(1, situations.size());

					final SituationRF situation = situations.iterator().next();
					assertEquals(dateImportInitial, situation.getDateDebut());
					assertNull(situation.getDateFin());
					assertEquals(38, situation.getCommune().getNoRf());
					assertEquals(1365, situation.getNoParcelle());
					assertNull(situation.getIndex1());
					assertNull(situation.getIndex2());
					assertNull(situation.getIndex3());

					final Set<EstimationRF> estimations = bienFond.getEstimations();
					assertEquals(1, estimations.size());

					final EstimationRF estimation = estimations.iterator().next();
					assertEquals(dateImportInitial, estimation.getDateDebut());
					assertNull(estimation.getDateFin());
					assertEquals(Long.valueOf(30000), estimation.getMontant());
					assertEquals("2002", estimation.getReference());
					assertNull(estimation.getDateEstimation());
					assertFalse(estimation.isEnRevision());

					final Set<SurfaceTotaleRF> surfacesTotales = bienFond.getSurfacesTotales();
					assertEquals(1, surfacesTotales.size());

					final SurfaceTotaleRF surfaceTotale = surfacesTotales.iterator().next();
					assertEquals(dateImportInitial, surfaceTotale.getDateDebut());
					assertNull(surfaceTotale.getDateFin());
					assertEquals(614, surfaceTotale.getSurface());
				}

				final List<BatimentRF> batiments = batimentRFDAO.getAll();
				assertEquals(1, batiments.size());

				final BatimentRF batiment0 = batiments.get(0);
				{
					assertEquals("1f1091523810039001381006e05770ac", batiment0.getMasterIdRF());

					final Set<DescriptionBatimentRF> descriptions = batiment0.getDescriptions();
					assertEquals(1, descriptions.size());
					final DescriptionBatimentRF description0 = descriptions.iterator().next();
					assertNull(description0.getSurface());
					assertEquals("Bâtiment", description0.getType());

					final Set<ImplantationRF> implantations = batiment0.getImplantations();
					assertEquals(1, implantations.size());
					final ImplantationRF implantation0 = implantations.iterator().next();
					assertEquals(dateImportInitial, implantation0.getDateDebut());
					assertNull(implantation0.getDateFin());
					assertEquals(Integer.valueOf(50), implantation0.getSurface());
					assertEquals("_1f10915238100390013810052537624b", implantation0.getImmeuble().getIdRF());
				}

				final List<CommuneRF> communes = communeRFDAO.getAll();
				assertEquals(1, communes.size());

				final CommuneRF commune0 = communes.get(0);
				{
					assertEquals("Cudrefin", commune0.getNomRf());
					assertEquals(5456, commune0.getNoOfs());
					assertEquals(38, commune0.getNoRf());
				}
			}
		});
	}

	/**
	 * Ce test vérifie que l'import d'un immeuble (et de toutes ses dépendances) puis sa radiation fonctionne bien.
	 */
	@Test
	public void testTraiterCreationPuisRadiationImmeuble() throws Exception {

		final RegDate dateImportInitial = RegDate.get(2010, 1, 1);
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		//
		//   ----- import initial : création -----
		//

		// on va chercher le fichier d'import initial
		final File importInitialFile = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/export_un_immeuble_complet_rf.xml");
		assertNotNull(importInitialFile);

		// on l'upload dans Raft
		final String raftUrlInitial;
		try (FileInputStream is = new FileInputStream(importInitialFile)) {
			raftUrlInitial = zipRaftEsbStore.store("Fiscalite", "UnitTest", "Unireg", is);
		}
		assertNotNull(raftUrlInitial);

		// on insère les données de l'import dans la base
		final Long importIdInitial = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final EvenementRFImport importEvent = new EvenementRFImport();
				importEvent.setDateEvenement(dateImportInitial);
				importEvent.setEtat(EtatEvenementRF.A_TRAITER);
				importEvent.setFileUrl(raftUrlInitial);
				return evenementRFImportDAO.save(importEvent).getId();
			}
		});
		assertNotNull(importIdInitial);

		// on déclenche le démarrage du job
		{
			final HashMap<String, Object> params = new HashMap<>();
			params.put(TraiterImportRFJob.ID, importIdInitial);
			params.put(TraiterImportRFJob.NB_THREADS, 2);
			params.put(TraiterImportRFJob.CONTINUE_WITH_MUTATIONS_JOB, false);

			final JobDefinition job = batchScheduler.startJob(TraiterImportRFJob.NAME, params);
			assertNotNull(job);

			// le job doit se terminer correctement
			waitForJobCompletion(job);
			assertEquals(JobDefinition.JobStatut.JOB_OK, job.getStatut());
		}

		// on vérifie que l'import est bien passé au statut TRAITE
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFImport importEvent = evenementRFImportDAO.get(importIdInitial);
				assertNotNull(importEvent);
				assertEquals(EtatEvenementRF.TRAITE, importEvent.getEtat());
			}
		});

		// on déclenche le démarrage du job de traitement
		{
			final HashMap<String, Object> params = new HashMap<>();
			params.put(TraiterMutationsRFJob.ID, importIdInitial);
			params.put(TraiterMutationsRFJob.NB_THREADS, 2);
			params.put(TraiterMutationsRFJob.CONTINUE_WITH_IDENTIFICATION_JOB, Boolean.FALSE);

			final JobDefinition job = batchScheduler.startJob(TraiterMutationsRFJob.NAME, params);
			assertNotNull(job);

			// le job doit se terminer correctement
			waitForJobCompletion(job);
			assertEquals(JobDefinition.JobStatut.JOB_OK, job.getStatut());
		}

		// on vérifie que les mutations ont bien été traitées
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
				assertEquals(6, mutations.size());
				Collections.sort(mutations, new MutationComparator());
				mutations.forEach(m -> {
					assertEquals(EtatEvenementRF.TRAITE, m.getEtat());
				});
			}
		});

		//
		//   ----- second import : radiation -----
		//

		// on va chercher le second fichier d'import
		final File secondImportFile = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/export_immeubles_vide_rf_hebdo.xml");
		assertNotNull(secondImportFile);

		// on l'upload dans Raft
		final String raftUrlSecond;
		try (FileInputStream is = new FileInputStream(secondImportFile)) {
			raftUrlSecond = zipRaftEsbStore.store("Fiscalite", "UnitTest", "Unireg", is);
		}
		assertNotNull(raftUrlSecond);

		// on insère les données de l'import dans la base
		final Long secondImportId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final EvenementRFImport importEvent = new EvenementRFImport();
				importEvent.setDateEvenement(dateSecondImport);
				importEvent.setEtat(EtatEvenementRF.A_TRAITER);
				importEvent.setFileUrl(raftUrlSecond);
				return evenementRFImportDAO.save(importEvent).getId();
			}
		});
		assertNotNull(secondImportId);

		// on déclenche le démarrage du job
		{
			final HashMap<String, Object> params = new HashMap<>();
			params.put(TraiterImportRFJob.ID, secondImportId);
			params.put(TraiterImportRFJob.NB_THREADS, 2);
			params.put(TraiterImportRFJob.CONTINUE_WITH_MUTATIONS_JOB, false);

			final JobDefinition job = batchScheduler.startJob(TraiterImportRFJob.NAME, params);
			assertNotNull(job);

			// le job doit se terminer correctement
			waitForJobCompletion(job);
			assertEquals(JobDefinition.JobStatut.JOB_OK, job.getStatut());
		}

		// on vérifie que l'import est bien passé au statut TRAITE
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFImport importEvent = evenementRFImportDAO.get(secondImportId);
				assertNotNull(importEvent);
				assertEquals(EtatEvenementRF.TRAITE, importEvent.getEtat());
			}
		});

		// on déclenche le démarrage du job de traitement
		{
			final HashMap<String, Object> params = new HashMap<>();
			params.put(TraiterMutationsRFJob.ID, secondImportId);
			params.put(TraiterMutationsRFJob.NB_THREADS, 2);
			params.put(TraiterMutationsRFJob.CONTINUE_WITH_IDENTIFICATION_JOB, Boolean.FALSE);

			final JobDefinition job = batchScheduler.startJob(TraiterMutationsRFJob.NAME, params);
			assertNotNull(job);

			// le job doit se terminer correctement
			waitForJobCompletion(job);
			assertEquals(JobDefinition.JobStatut.JOB_OK, job.getStatut());
		}

		// on vérifie que les mutations ont bien été traitées
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll().stream()
						.filter(m -> m.getParentImport().getId().equals(secondImportId))
						.collect(Collectors.toList());
				assertEquals(4, mutations.size());
				Collections.sort(mutations, new MutationComparator());

				final EvenementRFMutation mut0 = mutations.get(0);
				assertEquals(secondImportId, mut0.getParentImport().getId());
				assertEquals(EtatEvenementRF.TRAITE, mut0.getEtat());
				assertEquals(TypeEntiteRF.DROIT, mut0.getTypeEntite());
				assertEquals(TypeMutationRF.SUPPRESSION, mut0.getTypeMutation());   // suppression car l'ayant-droit n'existe plus
				assertEquals("_1f1091523810039001381003da8b72ac", mut0.getIdRF());
				assertNull(mut0.getXmlContent());

				final EvenementRFMutation mut1 = mutations.get(1);
				assertEquals(secondImportId, mut1.getParentImport().getId());
				assertEquals(EtatEvenementRF.TRAITE, mut1.getEtat());
				assertEquals(TypeEntiteRF.IMMEUBLE, mut1.getTypeEntite());
				assertEquals(TypeMutationRF.SUPPRESSION, mut1.getTypeMutation());
				assertEquals("_1f10915238100390013810052537624b", mut1.getIdRF());
				assertNull(mut1.getXmlContent());

				final EvenementRFMutation mut2 = mutations.get(2);
				assertEquals(secondImportId, mut2.getParentImport().getId());
				assertEquals(EtatEvenementRF.TRAITE, mut2.getEtat());
				assertEquals(TypeEntiteRF.SURFACE_AU_SOL, mut2.getTypeEntite());
				assertEquals(TypeMutationRF.SUPPRESSION, mut2.getTypeMutation());   // suppression car l'immeuble n'existe plus
				assertEquals("_1f10915238100390013810052537624b", mut2.getIdRF());
				assertNull(mut2.getXmlContent());

				final EvenementRFMutation mut3 = mutations.get(3);
				assertEquals(secondImportId, mut3.getParentImport().getId());
				assertEquals(EtatEvenementRF.TRAITE, mut3.getEtat());
				assertEquals(TypeEntiteRF.BATIMENT, mut3.getTypeEntite());
				assertEquals(TypeMutationRF.SUPPRESSION, mut3.getTypeMutation());
				assertEquals("1f1091523810039001381006e05770ac", mut3.getIdRF());
				assertNull(mut3.getXmlContent());
			}
		});

		// on vérifie que les immeubles ont bien été radiés
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final List<AyantDroitRF> ayantDroits = ayantDroitRFDAO.getAll();
				assertEquals(1, ayantDroits.size());

				final PersonnePhysiqueRF pp = (PersonnePhysiqueRF) ayantDroits.get(0);
				assertNotNull(pp);
				{
					assertEquals("Claude", pp.getNom());
					assertEquals("Daniel", pp.getPrenom());
					assertEquals(RegDate.get(1945, 2, 7), pp.getDateNaissance());
					assertEquals(149888, pp.getNoRF());
					assertEquals(Long.valueOf(10131016), pp.getNoContribuable());
				}

				final List<DroitRF> droits = droitRFDAO.getAll();
				assertEquals(1, droits.size());

				final DroitProprietePersonnePhysiqueRF droit0 = (DroitProprietePersonnePhysiqueRF) droits.get(0);
				{
					assertEquals(dateImportInitial, droit0.getDateDebut());
					assertEquals(dateSecondImport.getOneDayBefore(), droit0.getDateFin());
					assertEquals(RegDate.get(1997, 6, 19), droit0.getDateDebutOfficielle());
					assertEquals("Achat", droit0.getMotifDebut());
					assertNull(droit0.getMotifFin());
					assertEquals("1f1091523810039001381005be485efd", droit0.getMasterIdRF());
					assertEquals("_1f10915238100390013810052537624b", droit0.getImmeuble().getIdRF());
					assertEquals(new IdentifiantAffaireRF(3, null, null, null), droit0.getNumeroAffaire());
					assertEquals(new Fraction(1, 1), droit0.getPart());
					assertEquals(GenrePropriete.INDIVIDUELLE, droit0.getRegime());
				}

				final List<ImmeubleRF> immeubles = immeubleRFDAO.getAll();
				assertEquals(1, immeubles.size());

				final BienFondRF bienFond = (BienFondRF) immeubles.get(0);
				assertNotNull(bienFond);
				{
					assertEquals("_1f10915238100390013810052537624b", bienFond.getIdRF());
					assertEquals("CH507045198314", bienFond.getEgrid());
					assertFalse(bienFond.isCfa());
					assertEquals(dateSecondImport.getOneDayBefore(), bienFond.getDateRadiation());

					final Set<SituationRF> situations = bienFond.getSituations();
					assertEquals(1, situations.size());

					final SituationRF situation = situations.iterator().next();
					assertEquals(dateImportInitial, situation.getDateDebut());
					assertEquals(dateSecondImport.getOneDayBefore(), situation.getDateFin());
					assertEquals(38, situation.getCommune().getNoRf());
					assertEquals(1365, situation.getNoParcelle());
					assertNull(situation.getIndex1());
					assertNull(situation.getIndex2());
					assertNull(situation.getIndex3());

					final Set<EstimationRF> estimations = bienFond.getEstimations();
					assertEquals(1, estimations.size());

					final EstimationRF estimation = estimations.iterator().next();
					assertEquals(dateImportInitial, estimation.getDateDebut());
					assertEquals(dateSecondImport.getOneDayBefore(), estimation.getDateFin());
					assertEquals(Long.valueOf(30000), estimation.getMontant());
					assertEquals("2002", estimation.getReference());
					assertNull(estimation.getDateEstimation());
					assertFalse(estimation.isEnRevision());

					final Set<SurfaceTotaleRF> surfacesTotales = bienFond.getSurfacesTotales();
					assertEquals(1, surfacesTotales.size());

					final SurfaceTotaleRF surfaceTotale = surfacesTotales.iterator().next();
					assertEquals(dateImportInitial, surfaceTotale.getDateDebut());
					assertEquals(dateSecondImport.getOneDayBefore(), surfaceTotale.getDateFin());
					assertEquals(614, surfaceTotale.getSurface());
				}

				final List<BatimentRF> batiments = batimentRFDAO.getAll();
				assertEquals(1, batiments.size());

				final BatimentRF batiment0 = batiments.get(0);
				{
					assertEquals("1f1091523810039001381006e05770ac", batiment0.getMasterIdRF());

					final Set<DescriptionBatimentRF> descriptions = batiment0.getDescriptions();
					assertEquals(1, descriptions.size());
					final DescriptionBatimentRF description0 = descriptions.iterator().next();
					assertNull(description0.getSurface());
					assertEquals("Bâtiment", description0.getType());

					final Set<ImplantationRF> implantations = batiment0.getImplantations();
					assertEquals(1, implantations.size());
					final ImplantationRF implantation0 = implantations.iterator().next();
					assertEquals(dateImportInitial, implantation0.getDateDebut());
					assertEquals(dateSecondImport.getOneDayBefore(), implantation0.getDateFin());
					assertEquals(Integer.valueOf(50), implantation0.getSurface());
					assertEquals("_1f10915238100390013810052537624b", implantation0.getImmeuble().getIdRF());
				}

				final List<CommuneRF> communes = communeRFDAO.getAll();
				assertEquals(1, communes.size());

				final CommuneRF commune0 = communes.get(0);
				{
					assertEquals("Cudrefin", commune0.getNomRf());
					assertEquals(5456, commune0.getNoOfs());
					assertEquals(38, commune0.getNoRf());
				}
			}
		});
	}

}
