package ch.vd.uniregctb.registrefoncier.dataimport.detector;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.capitastra.grundstueck.CapiCode;
import ch.vd.capitastra.grundstueck.Gebaeude;
import ch.vd.capitastra.grundstueck.GebaeudeArt;
import ch.vd.capitastra.grundstueck.GrundstueckZuGebaeude;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.UniregJUnit4Runner;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImport;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.evenement.registrefoncier.MockEvenementRFImportDAO;
import ch.vd.uniregctb.evenement.registrefoncier.MockEvenementRFMutationDAO;
import ch.vd.uniregctb.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.uniregctb.evenement.registrefoncier.TypeMutationRF;
import ch.vd.uniregctb.registrefoncier.BatimentRF;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.DescriptionBatimentRF;
import ch.vd.uniregctb.registrefoncier.ImplantationRF;
import ch.vd.uniregctb.registrefoncier.dao.BatimentRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.MockBatimentRFDAO;
import ch.vd.uniregctb.registrefoncier.dataimport.MutationComparator;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRFImpl;
import ch.vd.uniregctb.transaction.MockTransactionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(UniregJUnit4Runner.class)
public class BatimentRFDetectorTest {

	private static final Long IMPORT_ID = 1L;
	private XmlHelperRF xmlHelperRF;
	private PlatformTransactionManager transactionManager;
	private BatimentRFDAO batimentRFDAO;

	@Before
	public void setUp() throws Exception {

		xmlHelperRF = new XmlHelperRFImpl();
		transactionManager = new MockTransactionManager();
		batimentRFDAO = new MockBatimentRFDAO();
		AuthenticationHelper.pushPrincipal("test-user");
	}

	@After
	public void tearDown() throws Exception {
		AuthenticationHelper.popPrincipal();
	}

	/**
	 * Ce test vérifie que des mutations sont bien créées lorsque les bâtiments n'existent pas dans la base de données.
	 */
	@Test
	public void testNouveauxBatiments() throws Exception {

		// un mock de DAO avec un import du registre foncier
		final EvenementRFImportDAO evenementRFImportDAO = new MockEvenementRFImportDAO() {
			@Override
			public EvenementRFImport get(Long id) {
				final EvenementRFImport imp = new EvenementRFImport();
				imp.setId(IMPORT_ID);
				return imp;
			}
		};

		// un mock qui mémorise toutes les mutations sauvées
		final EvenementRFMutationDAO evenementRFMutationDAO = new MockEvenementRFMutationDAO();

		final BatimentRFDetector detector = new BatimentRFDetector(xmlHelperRF, batimentRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie trois nouveaux bâtiments sur deux immeubles
		final Gebaeude gebaeude1 = newBatiment("4728a8e8c83e", 760, "Immeuble", new GrundstueckZuGebaeude("48e89c9a9", 760));
		final Gebaeude gebaeude2 = newBatiment("7837829e9a9a", 500, "Garage", new GrundstueckZuGebaeude("78238e8323", 150), new GrundstueckZuGebaeude("48e89c9a9", 350));
		final Gebaeude gebaeude3 = newBatiment("9028920a02ee", 230, "Villa", new GrundstueckZuGebaeude("78238e8323", null));

		List<Gebaeude> batiments = Arrays.asList(gebaeude1, gebaeude2, gebaeude3);
		detector.processBatiments(IMPORT_ID, 2, batiments.iterator(), null);

		// on devrait avoir trois événements de mutation de type CREATION sur chacun des bâtiments
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(3, mutations.size());
		Collections.sort(mutations, new MutationComparator());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(TypeEntiteRF.BATIMENT, mut0.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut0.getTypeMutation());
		assertEquals("4728a8e8c83e", mut0.getIdRF());  // le premier bâtiment
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<Gebaeude MasterID=\"4728a8e8c83e\" xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <GrundstueckZuGebaeude>\n" +
				             "        <GrundstueckIDREF>48e89c9a9</GrundstueckIDREF>\n" +
				             "        <AbschnittFlaeche>760</AbschnittFlaeche>\n" +
				             "    </GrundstueckZuGebaeude>\n" +
				             "    <Einzelobjekt>false</Einzelobjekt>\n" +
				             "    <Unterirdisch>false</Unterirdisch>\n" +
				             "    <Flaeche>760</Flaeche>\n" +
				             "    <GebaeudeArten>\n" +
				             "        <GebaeudeArtCode>\n" +
				             "            <TextFr>Immeuble</TextFr>\n" +
				             "        </GebaeudeArtCode>\n" +
				             "    </GebaeudeArten>\n" +
				             "</Gebaeude>\n", mut0.getXmlContent());

		final EvenementRFMutation mut1 = mutations.get(1);
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(TypeEntiteRF.BATIMENT, mut1.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut1.getTypeMutation());
		assertEquals("7837829e9a9a", mut1.getIdRF());  // le deuxième bâtiment
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<Gebaeude MasterID=\"7837829e9a9a\" xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <GrundstueckZuGebaeude>\n" +
				             "        <GrundstueckIDREF>78238e8323</GrundstueckIDREF>\n" +
				             "        <AbschnittFlaeche>150</AbschnittFlaeche>\n" +
				             "    </GrundstueckZuGebaeude>\n" +
				             "    <GrundstueckZuGebaeude>\n" +
				             "        <GrundstueckIDREF>48e89c9a9</GrundstueckIDREF>\n" +
				             "        <AbschnittFlaeche>350</AbschnittFlaeche>\n" +
				             "    </GrundstueckZuGebaeude>\n" +
				             "    <Einzelobjekt>false</Einzelobjekt>\n" +
				             "    <Unterirdisch>false</Unterirdisch>\n" +
				             "    <Flaeche>500</Flaeche>\n" +
				             "    <GebaeudeArten>\n" +
				             "        <GebaeudeArtCode>\n" +
				             "            <TextFr>Garage</TextFr>\n" +
				             "        </GebaeudeArtCode>\n" +
				             "    </GebaeudeArten>\n" +
				             "</Gebaeude>\n", mut1.getXmlContent());

		final EvenementRFMutation mut2 = mutations.get(2);
		assertEquals(IMPORT_ID, mut2.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut2.getEtat());
		assertEquals(TypeEntiteRF.BATIMENT, mut2.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut2.getTypeMutation());
		assertEquals("9028920a02ee", mut2.getIdRF());  // le troisième bâtiment
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<Gebaeude MasterID=\"9028920a02ee\" xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <GrundstueckZuGebaeude>\n" +
				             "        <GrundstueckIDREF>78238e8323</GrundstueckIDREF>\n" +
				             "    </GrundstueckZuGebaeude>\n" +
				             "    <Einzelobjekt>false</Einzelobjekt>\n" +
				             "    <Unterirdisch>false</Unterirdisch>\n" +
				             "    <Flaeche>230</Flaeche>\n" +
				             "    <GebaeudeArten>\n" +
				             "        <GebaeudeArtCode>\n" +
				             "            <TextFr>Villa</TextFr>\n" +
				             "        </GebaeudeArtCode>\n" +
				             "    </GebaeudeArten>\n" +
				             "</Gebaeude>\n", mut2.getXmlContent());
	}

	/**
	 * Ce test vérifie que des mutations sont bien créées si les bâtiments existent dans la base de données mais pas avec les mêmes valeurs.
	 */
	@Test
	public void testBatimentsModifies() throws Exception {

		final BienFondRF immeuble1 = new BienFondRF();
		immeuble1.setIdRF("48e89c9a9");

		final BienFondRF immeuble2 = new BienFondRF();
		immeuble2.setIdRF("78238e8323");

		final BatimentRF batiment1 = new BatimentRF();
		batiment1.setMasterIdRF("4728a8e8c83e");
		batiment1.addDescription(new DescriptionBatimentRF("Immeuble", 760));
		batiment1.addImplantation(new ImplantationRF(760, immeuble1));

		final BatimentRF batiment2 = new BatimentRF();
		batiment2.setMasterIdRF("7837829e9a9a");
		batiment2.addDescription(new DescriptionBatimentRF("Garage", 500));
		batiment2.addImplantation(new ImplantationRF(350, immeuble1));
		batiment2.addImplantation(new ImplantationRF(150, immeuble2));

		final BatimentRF batiment3 = new BatimentRF();
		batiment3.setMasterIdRF("9028920a02ee");
		batiment3.addDescription(new DescriptionBatimentRF("Villa", 230));
		batiment3.addImplantation(new ImplantationRF(null, immeuble2));

		// un mock avec les trois bâtiments.
		batimentRFDAO.save(batiment1);
		batimentRFDAO.save(batiment2);
		batimentRFDAO.save(batiment3);

		// un mock de DAO avec un import du registre foncier
		final EvenementRFImport imp = new EvenementRFImport();
		imp.setId(IMPORT_ID);
		final EvenementRFImportDAO evenementRFImportDAO = new MockEvenementRFImportDAO(imp);

		// un mock qui mémorise toutes les mutations sauvées
		final EvenementRFMutationDAO evenementRFMutationDAO = new MockEvenementRFMutationDAO();

		final BatimentRFDetector detector = new BatimentRFDetector(xmlHelperRF, batimentRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie trois bâtiments avec quelques différences
		//  - surface différente
		final Gebaeude gebaeude1 = newBatiment("4728a8e8c83e", 763, "Immeuble", new GrundstueckZuGebaeude("48e89c9a9", 763));
		//  - implantation différente
		final Gebaeude gebaeude2 = newBatiment("7837829e9a9a", 500, "Garage", new GrundstueckZuGebaeude("78238e8323", 120), new GrundstueckZuGebaeude("48e89c9a9", 380));
		//  - pas de différence
		final Gebaeude gebaeude3 = newBatiment("9028920a02ee", 230, "Villa", new GrundstueckZuGebaeude("78238e8323", null));

		List<Gebaeude> batiments = Arrays.asList(gebaeude1, gebaeude2, gebaeude3);
		detector.processBatiments(IMPORT_ID, 2, batiments.iterator(), null);

		// on devrait avoir deux événements de mutation de type MODIFICATION sur les deux premiers bâtiments
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(2, mutations.size());
		Collections.sort(mutations, new MutationComparator());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(TypeEntiteRF.BATIMENT, mut0.getTypeEntite());
		assertEquals(TypeMutationRF.MODIFICATION, mut0.getTypeMutation());
		assertEquals("4728a8e8c83e", mut0.getIdRF());  // le premier bâtiment
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<Gebaeude MasterID=\"4728a8e8c83e\" xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <GrundstueckZuGebaeude>\n" +
				             "        <GrundstueckIDREF>48e89c9a9</GrundstueckIDREF>\n" +
				             "        <AbschnittFlaeche>763</AbschnittFlaeche>\n" +
				             "    </GrundstueckZuGebaeude>\n" +
				             "    <Einzelobjekt>false</Einzelobjekt>\n" +
				             "    <Unterirdisch>false</Unterirdisch>\n" +
				             "    <Flaeche>763</Flaeche>\n" +
				             "    <GebaeudeArten>\n" +
				             "        <GebaeudeArtCode>\n" +
				             "            <TextFr>Immeuble</TextFr>\n" +
				             "        </GebaeudeArtCode>\n" +
				             "    </GebaeudeArten>\n" +
				             "</Gebaeude>\n", mut0.getXmlContent());

		final EvenementRFMutation mut1 = mutations.get(1);
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(TypeEntiteRF.BATIMENT, mut1.getTypeEntite());
		assertEquals(TypeMutationRF.MODIFICATION, mut1.getTypeMutation());
		assertEquals("7837829e9a9a", mut1.getIdRF());  // le deuxième bâtiment
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<Gebaeude MasterID=\"7837829e9a9a\" xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <GrundstueckZuGebaeude>\n" +
				             "        <GrundstueckIDREF>78238e8323</GrundstueckIDREF>\n" +
				             "        <AbschnittFlaeche>120</AbschnittFlaeche>\n" +
				             "    </GrundstueckZuGebaeude>\n" +
				             "    <GrundstueckZuGebaeude>\n" +
				             "        <GrundstueckIDREF>48e89c9a9</GrundstueckIDREF>\n" +
				             "        <AbschnittFlaeche>380</AbschnittFlaeche>\n" +
				             "    </GrundstueckZuGebaeude>\n" +
				             "    <Einzelobjekt>false</Einzelobjekt>\n" +
				             "    <Unterirdisch>false</Unterirdisch>\n" +
				             "    <Flaeche>500</Flaeche>\n" +
				             "    <GebaeudeArten>\n" +
				             "        <GebaeudeArtCode>\n" +
				             "            <TextFr>Garage</TextFr>\n" +
				             "        </GebaeudeArtCode>\n" +
				             "    </GebaeudeArten>\n" +
				             "</Gebaeude>\n", mut1.getXmlContent());
	}

	/**
	 * Ce test vérifie que des mutations sont bien créées si les bâtiments ont un type différents.
	 */
	@Test
	public void testBatimentsModifiesTypeDifferents() throws Exception {

		final BienFondRF immeuble1 = new BienFondRF();
		immeuble1.setIdRF("48e89c9a9");

		final BatimentRF batiment1 = new BatimentRF();
		batiment1.setMasterIdRF("4728a8e8c83e");
		batiment1.addDescription(new DescriptionBatimentRF("Immeuble", 760));
		batiment1.addImplantation(new ImplantationRF(760, immeuble1));

		// un mock avec le bâtiment.
		batimentRFDAO.save(batiment1);

		// un mock de DAO avec un import du registre foncier
		final EvenementRFImport imp = new EvenementRFImport();
		imp.setId(IMPORT_ID);
		final EvenementRFImportDAO evenementRFImportDAO = new MockEvenementRFImportDAO(imp);

		// un mock qui mémorise toutes les mutations sauvées
		final EvenementRFMutationDAO evenementRFMutationDAO = new MockEvenementRFMutationDAO();

		final BatimentRFDetector detector = new BatimentRFDetector(xmlHelperRF, batimentRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie le bâtiments avec le type différent
		final Gebaeude gebaeude1 = newBatiment("4728a8e8c83e", 760, "Habitation", new GrundstueckZuGebaeude("48e89c9a9", 763));

		List<Gebaeude> batiments = Collections.singletonList(gebaeude1);
		detector.processBatiments(IMPORT_ID, 2, batiments.iterator(), null);

		// on devrait avoir un événement de mutation de type MODIFICATION sur le bâtiment
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(1, mutations.size());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(TypeEntiteRF.BATIMENT, mut0.getTypeEntite());
		assertEquals(TypeMutationRF.MODIFICATION, mut0.getTypeMutation());
		assertEquals("4728a8e8c83e", mut0.getIdRF());  // le premier bâtiment
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<Gebaeude MasterID=\"4728a8e8c83e\" xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <GrundstueckZuGebaeude>\n" +
				             "        <GrundstueckIDREF>48e89c9a9</GrundstueckIDREF>\n" +
				             "        <AbschnittFlaeche>763</AbschnittFlaeche>\n" +
				             "    </GrundstueckZuGebaeude>\n" +
				             "    <Einzelobjekt>false</Einzelobjekt>\n" +
				             "    <Unterirdisch>false</Unterirdisch>\n" +
				             "    <Flaeche>760</Flaeche>\n" +
				             "    <GebaeudeArten>\n" +
				             "        <GebaeudeArtCode>\n" +
				             "            <TextFr>Habitation</TextFr>\n" +
				             "        </GebaeudeArtCode>\n" +
				             "    </GebaeudeArten>\n" +
				             "</Gebaeude>\n", mut0.getXmlContent());
	}

	/**
	 * Ce test vérifie qu'aucune mutation n'est créée si les données des immeubles dans l'import sont identiques avec l'état courant des immeubles stockés dans la DB.
	 */
	@Test
	public void testBatimentsIdentiques() throws Exception {

		final BienFondRF immeuble1 = new BienFondRF();
		immeuble1.setIdRF("48e89c9a9");

		final BienFondRF immeuble2 = new BienFondRF();
		immeuble2.setIdRF("78238e8323");

		final BatimentRF batiment1 = new BatimentRF();
		batiment1.setMasterIdRF("4728a8e8c83e");
		batiment1.addDescription(new DescriptionBatimentRF("Immeuble", 760));
		batiment1.addImplantation(new ImplantationRF(760, immeuble1));

		final BatimentRF batiment2 = new BatimentRF();
		batiment2.setMasterIdRF("7837829e9a9a");
		batiment2.addDescription(new DescriptionBatimentRF("Garage", 500));
		batiment2.addImplantation(new ImplantationRF(350, immeuble1));
		batiment2.addImplantation(new ImplantationRF(150, immeuble2));

		final BatimentRF batiment3 = new BatimentRF();
		batiment3.setMasterIdRF("9028920a02ee");
		batiment3.addDescription(new DescriptionBatimentRF("Villa", 230));
		batiment3.addImplantation(new ImplantationRF(null, immeuble2));

		// un mock avec les trois bâtiments.
		batimentRFDAO.save(batiment1);
		batimentRFDAO.save(batiment2);
		batimentRFDAO.save(batiment3);

		// un mock de DAO avec un import du registre foncier
		final EvenementRFImport imp = new EvenementRFImport();
		imp.setId(IMPORT_ID);
		final EvenementRFImportDAO evenementRFImportDAO = new MockEvenementRFImportDAO(imp);

		// un mock qui mémorise toutes les mutations sauvées
		final EvenementRFMutationDAO evenementRFMutationDAO = new MockEvenementRFMutationDAO();

		final BatimentRFDetector detector = new BatimentRFDetector(xmlHelperRF, batimentRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie les trois mêmes bâtiments
		final Gebaeude gebaeude1 = newBatiment("4728a8e8c83e", 760, "Immeuble", new GrundstueckZuGebaeude("48e89c9a9", 760));
		final Gebaeude gebaeude2 = newBatiment("7837829e9a9a", 500, "Garage", new GrundstueckZuGebaeude("78238e8323", 150), new GrundstueckZuGebaeude("48e89c9a9", 350));
		final Gebaeude gebaeude3 = newBatiment("9028920a02ee", 230, "Villa", new GrundstueckZuGebaeude("78238e8323", null));

		List<Gebaeude> batiments = Arrays.asList(gebaeude1, gebaeude2, gebaeude3);
		detector.processBatiments(IMPORT_ID, 2, batiments.iterator(), null);

		// on ne devrait pas avoir de mutation
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(0, mutations.size());
	}

	/**
	 * Ce test vérifie que des mutations de suppression sont créées si des bâtiments disparaissent.
	 */
	@Test
	public void testSuppressionDeBatiments() throws Exception {

		final BienFondRF immeuble1 = new BienFondRF();
		immeuble1.setIdRF("48e89c9a9");

		final BienFondRF immeuble2 = new BienFondRF();
		immeuble2.setIdRF("78238e8323");

		final BatimentRF batiment1 = new BatimentRF();
		batiment1.setMasterIdRF("4728a8e8c83e");
		batiment1.addDescription(new DescriptionBatimentRF("Immeuble", 760));
		batiment1.addImplantation(new ImplantationRF(760, immeuble1));

		final BatimentRF batiment2 = new BatimentRF();
		batiment2.setMasterIdRF("7837829e9a9a");
		batiment2.addDescription(new DescriptionBatimentRF("Garage", 500));
		batiment2.addImplantation(new ImplantationRF(350, immeuble1));
		batiment2.addImplantation(new ImplantationRF(150, immeuble2));

		final BatimentRF batiment3 = new BatimentRF();
		batiment3.setMasterIdRF("9028920a02ee");
		batiment3.addDescription(new DescriptionBatimentRF("Villa", 230));
		batiment3.addImplantation(new ImplantationRF(null, immeuble2));

		// un mock avec les trois bâtiments.
		batimentRFDAO.save(batiment1);
		batimentRFDAO.save(batiment2);
		batimentRFDAO.save(batiment3);

		// un mock de DAO avec un import du registre foncier
		final EvenementRFImport imp = new EvenementRFImport();
		imp.setId(IMPORT_ID);
		final EvenementRFImportDAO evenementRFImportDAO = new MockEvenementRFImportDAO(imp);

		// un mock qui mémorise toutes les mutations sauvées
		final EvenementRFMutationDAO evenementRFMutationDAO = new MockEvenementRFMutationDAO();

		final BatimentRFDetector detector = new BatimentRFDetector(xmlHelperRF, batimentRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie une liste de bâtiments vide
		detector.processBatiments(IMPORT_ID, 2, Collections.<Gebaeude>emptyList().iterator(), null);

		// on devrait avoir trois événements de mutation de type SUPPRESSION sur chacun des bâtiments
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(3, mutations.size());
		Collections.sort(mutations, new MutationComparator());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(TypeEntiteRF.BATIMENT, mut0.getTypeEntite());
		assertEquals(TypeMutationRF.SUPPRESSION, mut0.getTypeMutation());
		assertEquals("4728a8e8c83e", mut0.getIdRF());  // le premier bâtiment
		assertNull(mut0.getXmlContent());

		final EvenementRFMutation mut1 = mutations.get(1);
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(TypeEntiteRF.BATIMENT, mut1.getTypeEntite());
		assertEquals(TypeMutationRF.SUPPRESSION, mut1.getTypeMutation());
		assertEquals("7837829e9a9a", mut1.getIdRF());  // le deuxième propriétaire
		assertNull(mut1.getXmlContent());

		final EvenementRFMutation mut2 = mutations.get(2);
		assertEquals(IMPORT_ID, mut2.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut2.getEtat());
		assertEquals(TypeEntiteRF.BATIMENT, mut2.getTypeEntite());
		assertEquals(TypeMutationRF.SUPPRESSION, mut2.getTypeMutation());
		assertEquals("9028920a02ee", mut2.getIdRF());  // le troisième propriétaire
		assertNull(mut2.getXmlContent());
	}

	private static Gebaeude newBatiment(String masterId, Integer surface, String type, GrundstueckZuGebaeude... gzg) {
		final Gebaeude gebaeude = new Gebaeude();
		gebaeude.setMasterID(masterId);
		gebaeude.setFlaeche(surface);
		gebaeude.getGebaeudeArten().add(new GebaeudeArt(new CapiCode(null, type), null));
		if (gzg != null) {
			gebaeude.getGrundstueckZuGebaeude().addAll(Arrays.asList(gzg));
		}
		return gebaeude;
	}
}
