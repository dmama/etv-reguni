package ch.vd.uniregctb.migration.pm.indexeur;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;

public class NonHabitantIndexTest {

	private Path indexDir;
	private IndexImpl index;
	private NonHabitantIndex nhIndex;

	@Before
	public void setup() throws Exception {
		this.indexDir = Files.createTempDirectory("nh-test");
		this.index = new IndexImpl(this.indexDir.toString());
		this.index.afterPropertiesSet();
		this.index.overwriteIndex();

		this.nhIndex = new NonHabitantIndex();
		this.nhIndex.setIndex(this.index);
	}

	@After
	public void tearDown() throws Exception {
		this.nhIndex = null;
		if (this.index != null) {
			this.index.destroy();
			this.index = null;
		}
		if (this.indexDir != null) {
			// nettoyage!
			Files.walkFileTree(this.indexDir, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.deleteIfExists(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
			});
			this.indexDir = null;
		}
	}

	@Test
	public void testNomsPrenoms() throws Exception {
		final PersonnePhysique pp = new PersonnePhysique(Boolean.FALSE);
		pp.setNumero(12345678L);
		pp.setNom("Raskolnikov");
		pp.setNomNaissance("Ivanovitch");
		pp.setPrenomUsuel("Micha");
		pp.setTousPrenoms("Michaïl Sergeï");

		this.nhIndex.index(pp);
		{
			final NonHabitantIndex.NonHabitantSearchParameters params = new NonHabitantIndex.NonHabitantSearchParameters("Raskolnikov", null, null, null);
			final List<Long> results = this.nhIndex.search(params, Integer.MAX_VALUE);
			Assert.assertNotNull(results);
			Assert.assertEquals(1, results.size());
			Assert.assertEquals((Long) 12345678L, results.get(0));
		}
		{
			final NonHabitantIndex.NonHabitantSearchParameters params = new NonHabitantIndex.NonHabitantSearchParameters("Ivanovitch", null, null, null);
			final List<Long> results = this.nhIndex.search(params, Integer.MAX_VALUE);
			Assert.assertNotNull(results);
			Assert.assertEquals(1, results.size());
			Assert.assertEquals((Long) 12345678L, results.get(0));
		}
		{
			final NonHabitantIndex.NonHabitantSearchParameters params = new NonHabitantIndex.NonHabitantSearchParameters("Micha", null, null, null);
			final List<Long> results = this.nhIndex.search(params, Integer.MAX_VALUE);
			Assert.assertNotNull(results);
			Assert.assertEquals(1, results.size());
			Assert.assertEquals((Long) 12345678L, results.get(0));
		}
		{
			final NonHabitantIndex.NonHabitantSearchParameters params = new NonHabitantIndex.NonHabitantSearchParameters("Michail", null, null, null);
			final List<Long> results = this.nhIndex.search(params, Integer.MAX_VALUE);
			Assert.assertNotNull(results);
			Assert.assertEquals(1, results.size());
			Assert.assertEquals((Long) 12345678L, results.get(0));
		}
		{
			final NonHabitantIndex.NonHabitantSearchParameters params = new NonHabitantIndex.NonHabitantSearchParameters("Michaïl", null, null, null);
			final List<Long> results = this.nhIndex.search(params, Integer.MAX_VALUE);
			Assert.assertNotNull(results);
			Assert.assertEquals(1, results.size());
			Assert.assertEquals((Long) 12345678L, results.get(0));
		}
		{
			final NonHabitantIndex.NonHabitantSearchParameters params = new NonHabitantIndex.NonHabitantSearchParameters("Sergeï", null, null, null);
			final List<Long> results = this.nhIndex.search(params, Integer.MAX_VALUE);
			Assert.assertNotNull(results);
			Assert.assertEquals(1, results.size());
			Assert.assertEquals((Long) 12345678L, results.get(0));
		}
		{
			final NonHabitantIndex.NonHabitantSearchParameters params = new NonHabitantIndex.NonHabitantSearchParameters("Youri", null, null, null);
			final List<Long> results = this.nhIndex.search(params, Integer.MAX_VALUE);
			Assert.assertNotNull(results);
			Assert.assertEquals(0, results.size());
		}
		{
			final NonHabitantIndex.NonHabitantSearchParameters params = new NonHabitantIndex.NonHabitantSearchParameters(null, "Raskolnikov", null, null);
			final List<Long> results = this.nhIndex.search(params, Integer.MAX_VALUE);
			Assert.assertNotNull(results);
			Assert.assertEquals(1, results.size());
			Assert.assertEquals((Long) 12345678L, results.get(0));
		}
		{
			final NonHabitantIndex.NonHabitantSearchParameters params = new NonHabitantIndex.NonHabitantSearchParameters(null, "Ivanovitch", null, null);
			final List<Long> results = this.nhIndex.search(params, Integer.MAX_VALUE);
			Assert.assertNotNull(results);
			Assert.assertEquals(1, results.size());
			Assert.assertEquals((Long) 12345678L, results.get(0));
		}
		{
			final NonHabitantIndex.NonHabitantSearchParameters params = new NonHabitantIndex.NonHabitantSearchParameters(null, "Micha", null, null);
			final List<Long> results = this.nhIndex.search(params, Integer.MAX_VALUE);
			Assert.assertNotNull(results);
			Assert.assertEquals(1, results.size());
			Assert.assertEquals((Long) 12345678L, results.get(0));
		}
		{
			final NonHabitantIndex.NonHabitantSearchParameters params = new NonHabitantIndex.NonHabitantSearchParameters(null, "Michail", null, null);
			final List<Long> results = this.nhIndex.search(params, Integer.MAX_VALUE);
			Assert.assertNotNull(results);
			Assert.assertEquals(1, results.size());
			Assert.assertEquals((Long) 12345678L, results.get(0));
		}
		{
			final NonHabitantIndex.NonHabitantSearchParameters params = new NonHabitantIndex.NonHabitantSearchParameters(null, "Michaïl", null, null);
			final List<Long> results = this.nhIndex.search(params, Integer.MAX_VALUE);
			Assert.assertNotNull(results);
			Assert.assertEquals(1, results.size());
			Assert.assertEquals((Long) 12345678L, results.get(0));
		}
		{
			final NonHabitantIndex.NonHabitantSearchParameters params = new NonHabitantIndex.NonHabitantSearchParameters(null, "Sergeï", null, null);
			final List<Long> results = this.nhIndex.search(params, Integer.MAX_VALUE);
			Assert.assertNotNull(results);
			Assert.assertEquals(1, results.size());
			Assert.assertEquals((Long) 12345678L, results.get(0));
		}
		{
			final NonHabitantIndex.NonHabitantSearchParameters params = new NonHabitantIndex.NonHabitantSearchParameters(null, "Youri", null, null);
			final List<Long> results = this.nhIndex.search(params, Integer.MAX_VALUE);
			Assert.assertNotNull(results);
			Assert.assertEquals(0, results.size());
		}
	}

	@Test
	public void testSexe() throws Exception {
		{
			final PersonnePhysique pp = new PersonnePhysique(Boolean.FALSE);
			pp.setNumero(12345678L);
			pp.setNom("Raskolnikov");
			pp.setNomNaissance("Ivanovitch");
			pp.setPrenomUsuel("Micha");
			pp.setTousPrenoms("Michaïl Sergeï Babouchka");
			pp.setSexe(Sexe.MASCULIN);
			this.nhIndex.index(pp);
		}
		{
			final PersonnePhysique pp = new PersonnePhysique(Boolean.FALSE);
			pp.setNumero(87654321L);
			pp.setNom("Raskolnikova");
			pp.setNomNaissance("Ivanovna");
			pp.setPrenomUsuel("Tanja");
			pp.setTousPrenoms("Tanja Katarina Babouchka");
			pp.setSexe(Sexe.FEMININ);
			this.nhIndex.index(pp);
		}
		{
			final PersonnePhysique pp = new PersonnePhysique(Boolean.FALSE);
			pp.setNumero(12348765L);
			pp.setNom("Dourakine");
			pp.setNomNaissance("Dourakine");
			pp.setPrenomUsuel("Anton");
			pp.setTousPrenoms("Anton Grigori Babouchka");
			pp.setSexe(null);
			this.nhIndex.index(pp);
		}

		{
			final NonHabitantIndex.NonHabitantSearchParameters params = new NonHabitantIndex.NonHabitantSearchParameters(null, null, Sexe.MASCULIN, null);
			final List<Long> results = this.nhIndex.search(params, Integer.MAX_VALUE);
			Assert.assertNotNull(results);
			Assert.assertEquals(1, results.size());
			Assert.assertEquals((Long) 12345678L, results.get(0));
		}
		{
			final NonHabitantIndex.NonHabitantSearchParameters params = new NonHabitantIndex.NonHabitantSearchParameters(null, null, Sexe.FEMININ, null);
			final List<Long> results = this.nhIndex.search(params, Integer.MAX_VALUE);
			Assert.assertNotNull(results);
			Assert.assertEquals(1, results.size());
			Assert.assertEquals((Long) 87654321L, results.get(0));
		}
		{
			final NonHabitantIndex.NonHabitantSearchParameters params = new NonHabitantIndex.NonHabitantSearchParameters(null, "Babouchka", null, null);
			final List<Long> results = this.nhIndex.search(params, Integer.MAX_VALUE);
			Assert.assertNotNull(results);
			Assert.assertEquals(3, results.size());

			Collections.sort(results);
			Assert.assertEquals((Long) 12345678L, results.get(0));
			Assert.assertEquals((Long) 12348765L, results.get(1));
			Assert.assertEquals((Long) 87654321L, results.get(2));
		}
	}

	@Test
	public void testDateNaissance() throws Exception {
		{
			final PersonnePhysique pp = new PersonnePhysique(Boolean.FALSE);
			pp.setNumero(12345678L);
			pp.setNom("Raskolnikov");
			pp.setNomNaissance("Ivanovitch");
			pp.setPrenomUsuel("Micha");
			pp.setTousPrenoms("Michaïl Sergeï Babouchka");
			pp.setDateNaissance(RegDate.get(1954, 1, 8));
			this.nhIndex.index(pp);
		}
		{
			final PersonnePhysique pp = new PersonnePhysique(Boolean.FALSE);
			pp.setNumero(87654321L);
			pp.setNom("Raskolnikova");
			pp.setNomNaissance("Ivanovna");
			pp.setPrenomUsuel("Tanja");
			pp.setTousPrenoms("Tanja Katarina Babouchka");
			pp.setSexe(Sexe.FEMININ);
			pp.setDateNaissance(RegDate.get(1954, 2, 25));
			this.nhIndex.index(pp);
		}
		{
			final PersonnePhysique pp = new PersonnePhysique(Boolean.FALSE);
			pp.setNumero(12348765L);
			pp.setNom("Dourakine");
			pp.setNomNaissance("Dourakine");
			pp.setPrenomUsuel("Anton");
			pp.setTousPrenoms("Anton Grigori Babouchka");
			pp.setSexe(null);
			pp.setDateNaissance(null);
			this.nhIndex.index(pp);
		}

		{
			final NonHabitantIndex.NonHabitantSearchParameters params = new NonHabitantIndex.NonHabitantSearchParameters(null, null, null, RegDate.get(1954, 1, 8));
			final List<Long> results = this.nhIndex.search(params, Integer.MAX_VALUE);
			Assert.assertNotNull(results);
			Assert.assertEquals(1, results.size());
			Assert.assertEquals((Long) 12345678L, results.get(0));
		}
		{
			final NonHabitantIndex.NonHabitantSearchParameters params = new NonHabitantIndex.NonHabitantSearchParameters(null, null, null, RegDate.get(1954, 2, 25));
			final List<Long> results = this.nhIndex.search(params, Integer.MAX_VALUE);
			Assert.assertNotNull(results);
			Assert.assertEquals(1, results.size());
			Assert.assertEquals((Long) 87654321L, results.get(0));
		}
		{
			final NonHabitantIndex.NonHabitantSearchParameters params = new NonHabitantIndex.NonHabitantSearchParameters(null, null, null, RegDate.get(1954));
			final List<Long> results = this.nhIndex.search(params, Integer.MAX_VALUE);
			Assert.assertNotNull(results);
			Assert.assertEquals(2, results.size());

			Collections.sort(results);
			Assert.assertEquals((Long) 12345678L, results.get(0));
			Assert.assertEquals((Long) 87654321L, results.get(1));
		}
		{
			final NonHabitantIndex.NonHabitantSearchParameters params = new NonHabitantIndex.NonHabitantSearchParameters(null, "Babouchka", null, null);
			final List<Long> results = this.nhIndex.search(params, Integer.MAX_VALUE);
			Assert.assertNotNull(results);
			Assert.assertEquals(3, results.size());

			Collections.sort(results);
			Assert.assertEquals((Long) 12345678L, results.get(0));
			Assert.assertEquals((Long) 12348765L, results.get(1));
			Assert.assertEquals((Long) 87654321L, results.get(2));
		}
	}
}
