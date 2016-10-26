package ch.vd.uniregctb.registrefoncier;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import ch.vd.capitastra.grundstueck.Bodenbedeckung;
import ch.vd.capitastra.grundstueck.Gebaeude;
import ch.vd.capitastra.grundstueck.Grundstueck;
import ch.vd.capitastra.grundstueck.Liegenschaft;
import ch.vd.capitastra.grundstueck.NatuerlichePersonstamm;
import ch.vd.capitastra.grundstueck.PersonEigentumAnteil;
import ch.vd.capitastra.grundstueck.Personstamm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class DataRFBatcherTest {

	/**
	 * Ce test vérifie qu'aucune donnée n'est regroupée si aucune donnée n'est reçue en entrée.
	 */
	@Test
	public void testNoData() throws Exception {

		final TestCallback callback = new TestCallback();
		final DataRFBatcher batcher = new DataRFBatcher(10, callback);
		// pas de données, c'est déjà fini
		batcher.done();

		assertEquals(0, callback.getImmeubles().size());
		assertEquals(0, callback.getDroits().size());
		assertEquals(0, callback.getProprietaires().size());
		assertEquals(0, callback.getBatiments().size());
		assertEquals(0, callback.getSurfaces().size());
		assertEquals(1, callback.getDoneCount());
	}

	/**
	 * Ce test vérifie que les données sont bien transmises avec un groupe plus petit que la taille demandée s'il n'y a qu'un seul élément de chaque donnée en entrée.
	 */
	@Test
	public void testOneElementEach() throws Exception {

		final Grundstueck immeuble = new Liegenschaft();
		final PersonEigentumAnteil droit = new PersonEigentumAnteil();
		final Personstamm proprietaire = new NatuerlichePersonstamm();
		final Gebaeude batiment = new Gebaeude();
		final Bodenbedeckung surface = new Bodenbedeckung();

		final TestCallback callback = new TestCallback();
		final DataRFBatcher batcher = new DataRFBatcher(10, callback);

		// on envoie un seul élément de chaque type
		batcher.onImmeuble(immeuble);
		batcher.onDroit(droit);
		batcher.onProprietaire(proprietaire);
		batcher.onBatiment(batiment);
		batcher.onSurface(surface);
		batcher.done();

		// on doit recevoir un lot ne contenant qu'un seul élément de chaque type
		final List<List<Grundstueck>> immeubles = callback.getImmeubles();
		assertEquals(1, immeubles.size());
		assertEquals(1, immeubles.get(0).size());
		assertSame(immeuble, immeubles.get(0).get(0));

		final List<List<PersonEigentumAnteil>> droits = callback.getDroits();
		assertEquals(1, droits.size());
		assertEquals(1, droits.get(0).size());
		assertSame(droit, droits.get(0).get(0));

		final List<List<Personstamm>> proprietaires = callback.getProprietaires();
		assertEquals(1, proprietaires.size());
		assertEquals(1, proprietaires.get(0).size());
		assertSame(proprietaire, proprietaires.get(0).get(0));

		final List<List<Gebaeude>> batiments = callback.getBatiments();
		assertEquals(1, batiments.size());
		assertEquals(1, batiments.get(0).size());
		assertSame(batiment, batiments.get(0).get(0));

		final List<List<Bodenbedeckung>> surfaces = callback.getSurfaces();
		assertEquals(1, surfaces.size());
		assertEquals(1, surfaces.get(0).size());
		assertSame(surface, surfaces.get(0).get(0));

		assertEquals(1, callback.getDoneCount());
	}

	/**
	 * Ce test vérifie que les données sont bien transmises s'il n'y a qu'un seul immeuble.
	 */
	@Test
	public void testOneImmeuble() throws Exception {

		final Grundstueck immeuble = new Liegenschaft();

		final TestCallback callback = new TestCallback();
		final DataRFBatcher batcher = new DataRFBatcher(10, callback);

		// on envoie un seul immeuble
		batcher.onImmeuble(immeuble);
		batcher.done();

		// on doit recevoir un lot ne contenant qu'un seul immeuble
		final List<List<Grundstueck>> immeubles = callback.getImmeubles();
		assertEquals(1, immeubles.size());
		assertEquals(1, immeubles.get(0).size());
		assertSame(immeuble, immeubles.get(0).get(0));

		final List<List<PersonEigentumAnteil>> droits = callback.getDroits();
		assertEquals(0, droits.size());

		final List<List<Personstamm>> proprietaires = callback.getProprietaires();
		assertEquals(0, proprietaires.size());

		final List<List<Gebaeude>> batiments = callback.getBatiments();
		assertEquals(0, batiments.size());

		final List<List<Bodenbedeckung>> surfaces = callback.getSurfaces();
		assertEquals(0, surfaces.size());

		assertEquals(1, callback.getDoneCount());
	}

	/**
	 * Ce test vérifie que les données sont bien transmises s'il n'y a qu'un seul droit.
	 */
	@Test
	public void testOneDroit() throws Exception {

		final PersonEigentumAnteil droit = new PersonEigentumAnteil();

		final TestCallback callback = new TestCallback();
		final DataRFBatcher batcher = new DataRFBatcher(10, callback);

		// on envoie un seul droit
		batcher.onDroit(droit);
		batcher.done();

		// on doit recevoir un lot ne contenant qu'un seul droit
		final List<List<Grundstueck>> immeubles = callback.getImmeubles();
		assertEquals(0, immeubles.size());

		final List<List<PersonEigentumAnteil>> droits = callback.getDroits();
		assertEquals(1, droits.size());
		assertEquals(1, droits.get(0).size());
		assertSame(droit, droits.get(0).get(0));

		final List<List<Personstamm>> proprietaires = callback.getProprietaires();
		assertEquals(0, proprietaires.size());

		final List<List<Gebaeude>> batiments = callback.getBatiments();
		assertEquals(0, batiments.size());

		final List<List<Bodenbedeckung>> surfaces = callback.getSurfaces();
		assertEquals(0, surfaces.size());

		assertEquals(1, callback.getDoneCount());
	}

	/**
	 * Ce test vérifie que les données sont bien transmises s'il n'y a qu'un seul droit.
	 */
	@Test
	public void testOneProprietaire() throws Exception {

		final Personstamm proprietaire = new NatuerlichePersonstamm();

		final TestCallback callback = new TestCallback();
		final DataRFBatcher batcher = new DataRFBatcher(10, callback);

		// on envoie un seul propriétaire
		batcher.onProprietaire(proprietaire);
		batcher.done();

		// on doit recevoir un lot ne contenant qu'un seul propriétaire
		final List<List<Grundstueck>> immeubles = callback.getImmeubles();
		assertEquals(0, immeubles.size());

		final List<List<PersonEigentumAnteil>> droits = callback.getDroits();
		assertEquals(0, droits.size());

		final List<List<Personstamm>> proprietaires = callback.getProprietaires();
		assertEquals(1, proprietaires.size());
		assertEquals(1, proprietaires.get(0).size());
		assertSame(proprietaire, proprietaires.get(0).get(0));

		final List<List<Gebaeude>> batiments = callback.getBatiments();
		assertEquals(0, batiments.size());

		final List<List<Bodenbedeckung>> surfaces = callback.getSurfaces();
		assertEquals(0, surfaces.size());

		assertEquals(1, callback.getDoneCount());
	}

	/**
	 * Ce test vérifie que les données sont bien transmises s'il n'y a qu'un seul bâtiment.
	 */
	@Test
	public void testOneBatiment() throws Exception {

		final Gebaeude batiment = new Gebaeude();

		final TestCallback callback = new TestCallback();
		final DataRFBatcher batcher = new DataRFBatcher(10, callback);

		// on envoie un seul bâtiment
		batcher.onBatiment(batiment);
		batcher.done();

		// on doit recevoir un lot ne contenant qu'un seul bâtiment
		final List<List<Grundstueck>> immeubles = callback.getImmeubles();
		assertEquals(0, immeubles.size());

		final List<List<PersonEigentumAnteil>> droits = callback.getDroits();
		assertEquals(0, droits.size());

		final List<List<Personstamm>> proprietaires = callback.getProprietaires();
		assertEquals(0, proprietaires.size());

		final List<List<Gebaeude>> batiments = callback.getBatiments();
		assertEquals(1, batiments.size());
		assertEquals(1, batiments.get(0).size());
		assertSame(batiment, batiments.get(0).get(0));

		final List<List<Bodenbedeckung>> surfaces = callback.getSurfaces();
		assertEquals(0, surfaces.size());

		assertEquals(1, callback.getDoneCount());
	}

	/**
	 * Ce test vérifie que les données sont bien transmises s'il n'y a qu'une seule surface
	 */
	@Test
	public void testOneSurface() throws Exception {

		final Bodenbedeckung surface = new Bodenbedeckung();

		final TestCallback callback = new TestCallback();
		final DataRFBatcher batcher = new DataRFBatcher(10, callback);

		// on envoie une seule surface
		batcher.onSurface(surface);
		batcher.done();

		// on doit recevoir un lot ne contenant qu'une seule surface
		final List<List<Grundstueck>> immeubles = callback.getImmeubles();
		assertEquals(0, immeubles.size());

		final List<List<PersonEigentumAnteil>> droits = callback.getDroits();
		assertEquals(0, droits.size());

		final List<List<Personstamm>> proprietaires = callback.getProprietaires();
		assertEquals(0, proprietaires.size());

		final List<List<Gebaeude>> batiments = callback.getBatiments();
		assertEquals(0, batiments.size());

		final List<List<Bodenbedeckung>> surfaces = callback.getSurfaces();
		assertEquals(1, surfaces.size());
		assertEquals(1, surfaces.get(0).size());
		assertSame(surface, surfaces.get(0).get(0));

		assertEquals(1, callback.getDoneCount());
	}

	/**
	 * Ce test vérifie que l'envoie de 5 éléments pour une taille de lot égale à 2 génère bien 3 trois lots de 2, 2 et 1 éléments.
	 */
	@Test
	public void testFiveElementBatchSizeTwo() throws Exception {

		final Grundstueck immeuble0 = new Liegenschaft();
		final Grundstueck immeuble1 = new Liegenschaft();
		final Grundstueck immeuble2 = new Liegenschaft();
		final Grundstueck immeuble3 = new Liegenschaft();
		final Grundstueck immeuble4 = new Liegenschaft();
		final PersonEigentumAnteil droit0 = new PersonEigentumAnteil();
		final PersonEigentumAnteil droit1 = new PersonEigentumAnteil();
		final PersonEigentumAnteil droit2 = new PersonEigentumAnteil();
		final PersonEigentumAnteil droit3 = new PersonEigentumAnteil();
		final PersonEigentumAnteil droit4 = new PersonEigentumAnteil();
		final Personstamm proprietaire0 = new NatuerlichePersonstamm();
		final Personstamm proprietaire1 = new NatuerlichePersonstamm();
		final Personstamm proprietaire2 = new NatuerlichePersonstamm();
		final Personstamm proprietaire3 = new NatuerlichePersonstamm();
		final Personstamm proprietaire4 = new NatuerlichePersonstamm();
		final Gebaeude batiment0 = new Gebaeude();
		final Gebaeude batiment1 = new Gebaeude();
		final Gebaeude batiment2 = new Gebaeude();
		final Gebaeude batiment3 = new Gebaeude();
		final Gebaeude batiment4 = new Gebaeude();
		final Bodenbedeckung surface0 = new Bodenbedeckung();
		final Bodenbedeckung surface1 = new Bodenbedeckung();
		final Bodenbedeckung surface2 = new Bodenbedeckung();
		final Bodenbedeckung surface3 = new Bodenbedeckung();
		final Bodenbedeckung surface4 = new Bodenbedeckung();

		final TestCallback callback = new TestCallback();
		final DataRFBatcher batcher = new DataRFBatcher(2, callback);

		// on envoie un seul élément de chaque type
		batcher.onImmeuble(immeuble0);
		batcher.onImmeuble(immeuble1);
		batcher.onImmeuble(immeuble2);
		batcher.onImmeuble(immeuble3);
		batcher.onImmeuble(immeuble4);
		batcher.onDroit(droit0);
		batcher.onDroit(droit1);
		batcher.onDroit(droit2);
		batcher.onDroit(droit3);
		batcher.onDroit(droit4);
		batcher.onProprietaire(proprietaire0);
		batcher.onProprietaire(proprietaire1);
		batcher.onProprietaire(proprietaire2);
		batcher.onProprietaire(proprietaire3);
		batcher.onProprietaire(proprietaire4);
		batcher.onBatiment(batiment0);
		batcher.onBatiment(batiment1);
		batcher.onBatiment(batiment2);
		batcher.onBatiment(batiment3);
		batcher.onBatiment(batiment4);
		batcher.onSurface(surface0);
		batcher.onSurface(surface1);
		batcher.onSurface(surface2);
		batcher.onSurface(surface3);
		batcher.onSurface(surface4);
		batcher.done();

		// on doit recevoir trois lot contenant 2, 2, et 1 élément de chaque type
		final List<List<Grundstueck>> immeubles = callback.getImmeubles();
		assertEquals(3, immeubles.size());
		assertEquals(2, immeubles.get(0).size());
		assertSame(immeuble0, immeubles.get(0).get(0));
		assertSame(immeuble1, immeubles.get(0).get(1));
		assertEquals(2, immeubles.get(1).size());
		assertSame(immeuble2, immeubles.get(1).get(0));
		assertSame(immeuble3, immeubles.get(1).get(1));
		assertEquals(1, immeubles.get(2).size());
		assertSame(immeuble4, immeubles.get(2).get(0));

		final List<List<PersonEigentumAnteil>> droits = callback.getDroits();
		assertEquals(3, droits.size());
		assertEquals(2, droits.get(0).size());
		assertSame(droit0, droits.get(0).get(0));
		assertSame(droit1, droits.get(0).get(1));
		assertEquals(2, droits.get(1).size());
		assertSame(droit2, droits.get(1).get(0));
		assertSame(droit3, droits.get(1).get(1));
		assertEquals(1, droits.get(2).size());
		assertSame(droit4, droits.get(2).get(0));

		final List<List<Personstamm>> proprietaires = callback.getProprietaires();
		assertEquals(3, proprietaires.size());
		assertEquals(2, proprietaires.get(0).size());
		assertSame(proprietaire0, proprietaires.get(0).get(0));
		assertSame(proprietaire1, proprietaires.get(0).get(1));
		assertEquals(2, proprietaires.get(1).size());
		assertSame(proprietaire2, proprietaires.get(1).get(0));
		assertSame(proprietaire3, proprietaires.get(1).get(1));
		assertEquals(1, proprietaires.get(2).size());
		assertSame(proprietaire4, proprietaires.get(2).get(0));

		final List<List<Gebaeude>> batiments = callback.getBatiments();
		assertEquals(3, batiments.size());
		assertEquals(2, batiments.get(0).size());
		assertSame(batiment0, batiments.get(0).get(0));
		assertSame(batiment1, batiments.get(0).get(1));
		assertEquals(2, batiments.get(1).size());
		assertSame(batiment2, batiments.get(1).get(0));
		assertSame(batiment3, batiments.get(1).get(1));
		assertEquals(1, batiments.get(2).size());
		assertSame(batiment4, batiments.get(2).get(0));

		final List<List<Bodenbedeckung>> surfaces = callback.getSurfaces();
		assertEquals(3, surfaces.size());
		assertEquals(2, surfaces.get(0).size());
		assertSame(surface0, surfaces.get(0).get(0));
		assertSame(surface1, surfaces.get(0).get(1));
		assertEquals(2, surfaces.get(1).size());
		assertSame(surface2, surfaces.get(1).get(0));
		assertSame(surface3, surfaces.get(1).get(1));
		assertEquals(1, surfaces.get(2).size());
		assertSame(surface4, surfaces.get(2).get(0));

		assertEquals(1, callback.getDoneCount());
	}

	private static class TestCallback implements DataRFBatcher.Callback {

		private final List<List<Grundstueck>> immeubles = new ArrayList<>();
		private final List<List<PersonEigentumAnteil>> droits = new ArrayList<>();
		private final List<List<Personstamm>> proprietaires = new ArrayList<>();
		private final List<List<Gebaeude>> batiments = new ArrayList<>();
		private final List<List<Bodenbedeckung>> surfaces = new ArrayList<>();
		private int doneCount = 0;


		@Override
		public void onImmeubles(@NotNull List<Grundstueck> immeubles) {
			this.immeubles.add(immeubles);
		}

		@Override
		public void onDroits(@NotNull List<PersonEigentumAnteil> droits) {
			this.droits.add(droits);
		}

		@Override
		public void onProprietaires(@NotNull List<Personstamm> personnes) {
			this.proprietaires.add(personnes);
		}

		@Override
		public void onBatiments(@NotNull List<Gebaeude> batiments) {
			this.batiments.add(batiments);
		}

		@Override
		public void onSurfaces(@NotNull List<Bodenbedeckung> surfaces) {
			this.surfaces.add(surfaces);
		}

		@Override
		public void done() {
			doneCount++;
		}

		public List<List<Grundstueck>> getImmeubles() {
			return immeubles;
		}

		public List<List<PersonEigentumAnteil>> getDroits() {
			return droits;
		}

		public List<List<Personstamm>> getProprietaires() {
			return proprietaires;
		}

		public List<List<Gebaeude>> getBatiments() {
			return batiments;
		}

		public List<List<Bodenbedeckung>> getSurfaces() {
			return surfaces;
		}

		public int getDoneCount() {
			return doneCount;
		}
	}
}