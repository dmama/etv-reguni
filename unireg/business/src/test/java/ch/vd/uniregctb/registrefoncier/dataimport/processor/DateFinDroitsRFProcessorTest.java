package ch.vd.uniregctb.registrefoncier.dataimport.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscal;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.evenement.fiscal.registrefoncier.EvenementFiscalDroit;
import ch.vd.uniregctb.evenement.fiscal.registrefoncier.EvenementFiscalDroitPropriete;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.IdentifiantDroitRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.ServitudeRF;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.dataimport.TraitementFinsDeDroitRFResults;
import ch.vd.uniregctb.registrefoncier.processor.MutationRFProcessorTestCase;
import ch.vd.uniregctb.rf.GenrePropriete;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings("Duplicates")
public class DateFinDroitsRFProcessorTest extends MutationRFProcessorTestCase {

	private DateFinDroitsRFProcessor processor;
	private ImmeubleRFDAO immeubleRFDAO;
	private EvenementFiscalDAO evenementFiscalDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		final EvenementFiscalService evenementFiscalService = getBean(EvenementFiscalService.class, "evenementFiscalService");
		this.immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		this.processor = new DateFinDroitsRFProcessor(immeubleRFDAO, transactionManager, evenementFiscalService);
		this.evenementFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
	}

	/**
	 * Vérifie que le traitement ne fait rien si tous les droits sont déjà fermés
	 */
	@Test
	public void testProcessImmeubleDroitsDejaFermes() throws Exception {

		// un immeuble avec deux droits dont les dates métier sont renseignées
		final Long id = doInNewTransaction(status -> {
			final CommuneRF commune = addCommuneRF(61, "La Sarraz", 5498);
			final BienFondRF bienFond = addBienFondRF("38383838", "CHE478391947", commune, 234);
			final PersonnePhysiqueRF jean = addPersonnePhysiqueRF("02893039", "Jean", "Routourne", RegDate.get(1962, 9, 12));
			final PersonnePhysiqueRF jacques = addPersonnePhysiqueRF("937823a0a02", "Jacques", "Roubloque", RegDate.get(1968, 1, 24));
			addDroitPropriete(jean, bienFond, null, GenrePropriete.INDIVIDUELLE, new Fraction(1, 1),
			                  null, RegDate.get(2003, 6, 4), RegDate.get(1990, 3, 1), RegDate.get(2003, 5, 12), "Achat", "Vente",
			                  new IdentifiantAffaireRF(8, 1990, 3, 0), "473839273923", "473839273922");
			addDroitPropriete(jacques, bienFond, null, GenrePropriete.INDIVIDUELLE, new Fraction(1, 1),
			                  RegDate.get(2003, 6, 5), null, RegDate.get(2003, 5, 13), null, "Achat", null,
			                  new IdentifiantAffaireRF(8, 2003, 21, 0), "45838923783", "45838923782");
			return bienFond.getId();
		});

		// on détecte les dates métiers manquantes sur les droits
		doInNewTransaction(status -> {
			processor.processImmeuble(id, new TraitementFinsDeDroitRFResults(1));
			return null;
		});

		// les dates des droits devraient être intouchées
		doInNewTransaction(status -> {
			final ImmeubleRF immeuble = immeubleRFDAO.get(id);
			assertNotNull(immeuble);

			final List<DroitProprieteRF> droits = new ArrayList<>(immeuble.getDroitsPropriete());
			assertEquals(2, droits.size());
			droits.sort(new DateRangeComparator<>());
			assertDatesDroit(null, RegDate.get(2003, 6, 4), RegDate.get(1990, 3, 1), RegDate.get(2003, 5, 12), "Achat", "Vente", droits.get(0));
			assertDatesDroit(RegDate.get(2003, 6, 5), null, RegDate.get(2003, 5, 13), null, "Achat", null, droits.get(1));
			return null;
		});

		// postcondition : aucun événement fiscal n'a été envoyé
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(0, events.size());
			return null;
		});
	}

	/**
	 * [SIFISC-24558] Vérifie que le traitement ne crashe pas lorsqu'il rencontre des servitudes et qu'il les ignore.
	 */
	@Test
	public void testProcessIgnoreLesServitudes() throws Exception {

		// un immeuble avec juste une servitude
		final Long id = doInNewTransaction(status -> {
			final CommuneRF commune = addCommuneRF(61, "La Sarraz", 5498);
			final BienFondRF bienFond = addBienFondRF("38383838", "CHE478391947", commune, 234);
			final PersonnePhysiqueRF jean = addPersonnePhysiqueRF("02893039", "Jean", "Routourne", RegDate.get(1962, 9, 12));
			final PersonnePhysiqueRF jacques = addPersonnePhysiqueRF("937823a0a02", "Jacques", "Roubloque", RegDate.get(1968, 1, 24));
			addUsufruitRF(null, RegDate.get(1990, 3, 1), RegDate.get(2017,1,13), null, null, null, "32727817", "1",
			              new IdentifiantAffaireRF(8, 1990, 3, 0), new IdentifiantDroitRF(8, 1990, 3),
			              Arrays.asList(jean, jacques), Collections.singletonList(bienFond));
			return bienFond.getId();
		});

		// on démarre le batch
		processor.process(1, null);

		// les dates des droits devraient être intouchées
		doInNewTransaction(status -> {
			final ImmeubleRF immeuble = immeubleRFDAO.get(id);
			assertNotNull(immeuble);

			// il n'y a pas de droit
			assertEmpty(immeuble.getDroitsPropriete());

			// la servitude est intouchée
			final Set<ServitudeRF> servitudes = immeuble.getServitudes();
			assertNotNull(servitudes);
			assertEquals(1, servitudes.size());
			final ServitudeRF servitude0 = servitudes.iterator().next();
			assertNull(servitude0.getDateDebut());
			assertEquals(RegDate.get(2017, 1, 13), servitude0.getDateFin());
			assertEquals(RegDate.get(1990, 3, 1), servitude0.getDateDebutMetier());
			assertNull(servitude0.getDateFinMetier());
			return null;
		});

		// postcondition : aucun événement fiscal n'a été envoyé
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(0, events.size());
			return null;
		});
	}

	/**
	 * Vérifie que le traitement calcule bien la date de fin métier dans le cas simple d'un achat/vente d'un immeuble en propriété individuelle
	 */
	@Test
	public void testProcessImmeubleVenteAchatProprieteIndividuelle() throws Exception {

		// un immeuble avec deux droits correspondant à la vente/achat de celui-li
		final Long id = doInNewTransaction(status -> {
			final CommuneRF commune = addCommuneRF(61, "La Sarraz", 5498);
			final BienFondRF bienFond = addBienFondRF("38383838", "CHE478391947", commune, 234);
			final PersonnePhysiqueRF jean = addPersonnePhysiqueRF("02893039", "Jean", "Routourne", RegDate.get(1962, 9, 12));
			final PersonnePhysiqueRF jacques = addPersonnePhysiqueRF("937823a0a02", "Jacques", "Roubloque", RegDate.get(1968, 1, 24));
			addDroitPropriete(jean, bienFond, null, GenrePropriete.INDIVIDUELLE, new Fraction(1, 1),
			                  null, RegDate.get(2003, 6, 4), RegDate.get(1990, 3, 1), null, "Achat", null,
			                  new IdentifiantAffaireRF(8, 1990, 3, 0), "473839273923", "473839273922");
			addDroitPropriete(jacques, bienFond, null, GenrePropriete.INDIVIDUELLE, new Fraction(1, 1),
			                  RegDate.get(2003, 6, 5), null, RegDate.get(2003, 5, 13), null, "Achat", null,
			                  new IdentifiantAffaireRF(8, 2003, 21, 0), "45838923783", "45838923782");
			return bienFond.getId();
		});

		// on détecte les dates métiers manquantes sur les droits
		doInNewTransaction(status -> {
			processor.processImmeuble(id, new TraitementFinsDeDroitRFResults(1));
			return null;
		});

		// la date de fin métier du premier droit devrait maintenant être renseignée
		doInNewTransaction(status -> {
			final ImmeubleRF immeuble = immeubleRFDAO.get(id);
			assertNotNull(immeuble);

			final List<DroitProprieteRF> droits = new ArrayList<>(immeuble.getDroitsPropriete());
			assertEquals(2, droits.size());
			droits.sort(new DateRangeComparator<>());
			assertDatesDroit(null, RegDate.get(2003, 6, 4), RegDate.get(1990, 3, 1), RegDate.get(2003, 5, 13), "Achat", "Vente", droits.get(0));
			assertDatesDroit(RegDate.get(2003, 6, 5), null, RegDate.get(2003, 5, 13), null, "Achat", null, droits.get(1));
			return null;
		});

		// postcondition : les événements fiscaux correspondants ont été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());

			final EvenementFiscalDroitPropriete event0 = (EvenementFiscalDroitPropriete) events.get(0);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.FERMETURE, event0.getType());
			assertEquals(RegDate.get(2003, 5, 13), event0.getDateValeur());
			assertEquals("38383838", event0.getImmeuble().getIdRF());
			assertEquals("02893039", event0.getAyantDroit().getIdRF());

			return null;
		});
	}

	/**
	 * Vérifie que le traitement calcule bien la date de fin métier séparant les affaires par ayant-droit lors du rattrapage de l'import initial.
	 */
	@Test
	public void testProcessImmeubleRattrapageImportInitial() throws Exception {

		class Ids {
			long immeuble;
			long jean;
			long jacques;
		}
		final Ids ids = new Ids();

		// un immeuble avec des droits correspondants à la vente/achat qui s'étendent sur plusieurs années
		doInNewTransaction(status -> {
			final CommuneRF commune = addCommuneRF(61, "La Sarraz", 5498);
			final BienFondRF bienFond = addBienFondRF("38383838", "CHE478391947", commune, 234);

			final PersonnePhysiqueRF jean = addPersonnePhysiqueRF("02893039", "Jean", "Routourne", RegDate.get(1962, 9, 12));
			final PersonnePhysiqueRF jacques = addPersonnePhysiqueRF("937823a0a02", "Jacques", "Roubloque", RegDate.get(1968, 1, 24));
			ids.immeuble = bienFond.getId();
			ids.jean = jean.getId();
			ids.jacques = jacques.getId();

			// import inital du 31.12.2016
			addDroitPropriete(jean, bienFond, null, GenrePropriete.COPROPRIETE, new Fraction(1, 2),
			                  null, RegDate.get(2017, 1, 6), RegDate.get(1997, 4, 21), null, "Succession", null,
			                  new IdentifiantAffaireRF(5, null, 151264, null), "473839273923", "473839273922");
			// second import du 07.02.2017
			addDroitPropriete(jean, bienFond, null, GenrePropriete.COPROPRIETE, new Fraction(1, 2),
			                  RegDate.get(2017, 1, 7), null, RegDate.get(2001, 9, 4), null, "Succession", null,
			                  new IdentifiantAffaireRF(5, 2001, 2174, 0), "2929181981818", "2929181981817");

			// import inital du 31.12.2016
			addDroitPropriete(jacques, bienFond, null, GenrePropriete.COPROPRIETE, new Fraction(1, 2),
			                  null, RegDate.get(2017, 1, 6), RegDate.get(2012, 3, 6), null, "Succession", null,
			                  new IdentifiantAffaireRF(5, 2012, 617, 0), "45838923783", "45838923782");
			// second import du 07.02.2017
			addDroitPropriete(jacques, bienFond, null, GenrePropriete.COPROPRIETE, new Fraction(1, 2),
			                  RegDate.get(2017, 1, 7), null, RegDate.get(2013, 5, 31), null, "Succession", null,
			                  new IdentifiantAffaireRF(5, 2012, 617, 0), "3838929217821", "3838929217820");

			return null;
		});

		// on calcule les dates métiers manquantes sur les droits
		doInNewTransaction(status -> {
			processor.processImmeuble(ids.immeuble, new TraitementFinsDeDroitRFResults(1));
			return null;
		});

		// les dates de fin métier doivent être calculées par ayant-droit
		doInNewTransaction(status -> {
			final ImmeubleRF immeuble = immeubleRFDAO.get(ids.immeuble);
			assertNotNull(immeuble);

			final List<DroitProprieteRF> droits = new ArrayList<>(immeuble.getDroitsPropriete());
			assertEquals(4, droits.size());
			droits.sort(Comparator.<DroitProprieteRF, Long>comparing(d -> d.getAyantDroit().getId())
					            .thenComparing(DateRangeComparator::compareRanges));

			// les droits de jean
			assertDatesDroit(null, RegDate.get(2017, 1, 6), RegDate.get(1997, 4, 21), RegDate.get(2001, 9, 4), "Succession", "Succession", droits.get(0));
			assertDatesDroit(RegDate.get(2017, 1, 7), null, RegDate.get(2001, 9, 4), null, "Succession", null, droits.get(1));

			// les droits de jacques
			assertDatesDroit(null, RegDate.get(2017, 1, 6), RegDate.get(2012, 3, 6), RegDate.get(2013, 5, 31), "Succession", "Succession", droits.get(2));
			assertDatesDroit(RegDate.get(2017, 1, 7), null, RegDate.get(2013, 5, 31), null, "Succession", null, droits.get(3));
			return null;
		});

		// postcondition : les événements fiscaux correspondants ont été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(2, events.size());
			events.sort(Comparator.comparing(EvenementFiscal::getDateValeur));

			final EvenementFiscalDroitPropriete event0 = (EvenementFiscalDroitPropriete) events.get(0);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.FERMETURE, event0.getType());
			assertEquals(RegDate.get(2001, 9, 4), event0.getDateValeur());
			assertEquals("38383838", event0.getImmeuble().getIdRF());
			assertEquals("02893039", event0.getAyantDroit().getIdRF());

			final EvenementFiscalDroitPropriete event1 = (EvenementFiscalDroitPropriete) events.get(1);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.FERMETURE, event1.getType());
			assertEquals(RegDate.get(2013, 5, 31), event1.getDateValeur());
			assertEquals("38383838", event1.getImmeuble().getIdRF());
			assertEquals("937823a0a02", event1.getAyantDroit().getIdRF());

			return null;
		});
	}

	/*
	 * Vérifie que le traitement calcule bien la date de fin métier regroupant les droits par ayant-droit lors du rattrapage de l'import initial.
	 */
	@Test
	public void testProcessImmeubleRattrapageImportInitial2() throws Exception {

		final String idPPRF1 = "_1f1091523810460801381046a6460f8d";
		final String idPPRF2 = "_1f109152381026b501381026fd906a80";
		final String idImmeubleRF1 = "_1f109152381026b501381028da172cca";

		// précondition : il y a déjà deux droits dans la base de données
		final Long immeubleId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				final PersonnePhysiqueRF pp0 = addPersonnePhysiqueRF(idPPRF1, "Isabelle", "Tissot", RegDate.get(1900, 1, 1));
				final PersonnePhysiqueRF pp1 = addPersonnePhysiqueRF(idPPRF2, "Simone", "Tissot", RegDate.get(1900, 1, 1));

				BienFondRF immeuble = new BienFondRF();
				immeuble.setIdRF(idImmeubleRF1);
				immeuble = (BienFondRF) immeubleRFDAO.save(immeuble);

				// import inital du 31.12.2016
				addDroitPropriete(pp0, immeuble, null, GenrePropriete.COPROPRIETE, new Fraction(1, 2),
				                  null, RegDate.get(2016, 12, 31), RegDate.get(1961, 2, 4), null, "Succession", null,
				                  new IdentifiantAffaireRF(11, null, 74634, null), "1f109152381026b5013810299b0b1905", "1f109152381026b5013810299b0b1904");
				// second import du 01.01.2017
				addDroitPropriete(pp0, immeuble, null, GenrePropriete.COPROPRIETE, new Fraction(1, 2),
				                  RegDate.get(2017, 1, 1), null, RegDate.get(1980, 12, 29), null, "Changement de régime", null,
				                  new IdentifiantAffaireRF(11, null, 95580, null), "21321321", "21321320");

				// import inital du 31.12.2016
				addDroitPropriete(pp1, immeuble, null, GenrePropriete.COPROPRIETE, new Fraction(1, 2),
				                  null, RegDate.get(2016, 12, 31), RegDate.get(1980, 12, 29), null, "Changement de régime", null,
				                  new IdentifiantAffaireRF(11, null, 95580, null), "1f109152381026b5013810299b0b1908", "1f109152381026b5013810299b0b1907");
				// second import du 01.01.2017
				addDroitPropriete(pp1, immeuble, null, GenrePropriete.COPROPRIETE, new Fraction(1, 2),
				                  RegDate.get(2017, 1, 1), null, RegDate.get(1998, 2, 11), null, "Succession", null,
				                  new IdentifiantAffaireRF(11, null, 115039, null), "90392039", "90392038");

				return immeuble.getId();
			}
		});

		// on calcule les dates métiers manquantes sur les droits
		doInNewTransaction(status -> {
			processor.processImmeuble(immeubleId, new TraitementFinsDeDroitRFResults(1));
			return null;
		});

		// les dates de fin métier doivent être calculées par ayant-droit
		doInNewTransaction(status -> {
			final ImmeubleRF immeuble = immeubleRFDAO.get(immeubleId);
			assertNotNull(immeuble);

			final List<DroitProprieteRF> droits = new ArrayList<>(immeuble.getDroitsPropriete());
			droits.sort(Comparator.<DroitProprieteRF, Long>comparing(d -> d.getAyantDroit().getId())
					            .thenComparing(DateRangeComparator::compareRanges));

			// les droits d'Isabelle
			assertDatesDroit(null, RegDate.get(2016, 12, 31), RegDate.get(1961, 2, 4), RegDate.get(1980, 12, 29), "Succession", "Changement de régime", droits.get(0));
			assertDatesDroit(RegDate.get(2017, 1, 1), null, RegDate.get(1980, 12, 29), null, "Changement de régime", null, droits.get(1));

			// les droits de Simone
			assertDatesDroit(null, RegDate.get(2016, 12, 31), RegDate.get(1980, 12, 29), RegDate.get(1998, 2, 11), "Changement de régime", "Succession", droits.get(2));
			assertDatesDroit(RegDate.get(2017, 1, 1), null, RegDate.get(1998, 2, 11), null, "Succession", null, droits.get(3));
			return null;
		});

		// postcondition : les événements fiscaux correspondants ont été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(2, events.size());
			events.sort(Comparator.comparing(EvenementFiscal::getDateValeur));

			final EvenementFiscalDroitPropriete event0 = (EvenementFiscalDroitPropriete) events.get(0);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.FERMETURE, event0.getType());
			assertEquals(RegDate.get(1980, 12, 29), event0.getDateValeur());
			assertEquals(idImmeubleRF1, event0.getImmeuble().getIdRF());
			assertEquals(idPPRF1, event0.getAyantDroit().getIdRF());

			final EvenementFiscalDroitPropriete event1 = (EvenementFiscalDroitPropriete) events.get(1);
			assertEquals(EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.FERMETURE, event1.getType());
			assertEquals(RegDate.get(1998, 2, 11), event1.getDateValeur());
			assertEquals(idImmeubleRF1, event1.getImmeuble().getIdRF());
			assertEquals(idPPRF2, event1.getAyantDroit().getIdRF());

			return null;
		});
	}

	private static void assertDatesDroit(RegDate dateDebut, RegDate dateFin, RegDate dateDebutMetier, RegDate dateFinMetier, String motifDebut, String motifFin, DroitRF droit) {
		assertNotNull(droit);
		assertEquals(dateDebut, droit.getDateDebut());
		assertEquals(dateFin, droit.getDateFin());
		assertEquals(dateDebutMetier, droit.getDateDebutMetier());
		assertEquals(dateFinMetier, droit.getDateFinMetier());
		assertEquals(motifDebut, droit.getMotifDebut());
		assertEquals(motifFin, droit.getMotifFin());
	}
}