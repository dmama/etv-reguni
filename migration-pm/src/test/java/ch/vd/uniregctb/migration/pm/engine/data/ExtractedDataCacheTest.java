package ch.vd.uniregctb.migration.pm.engine.data;

import java.util.Collections;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.migration.pm.Graphe;
import ch.vd.uniregctb.migration.pm.engine.MockGraphe;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;
import ch.vd.uniregctb.migration.pm.utils.EntityKey;

public class ExtractedDataCacheTest {

	@Test
	public void testUtilisationSimple() throws Exception {

		final class Nom implements Supplier<String> {
			private final String nom;

			public Nom(String nom) {
				this.nom = nom;
			}

			@Override
			public String get() {
				return nom;
			}
		}

		final class AutreNom implements Supplier<String> {
			private final String nom;

			public AutreNom(String nom) {
				this.nom = nom;
			}

			@Override
			public String get() {
				return nom;
			}
		}

		final String nomEntreprise = "Ma petite entreprise";
		final String nomEtablissemnt = "Mon établissement à moi";
		final String nomIndividu = "Moi";

		final RegpmEntreprise entreprise = new RegpmEntreprise();
		entreprise.setId(547684572L);
		entreprise.setEnseigne(nomEntreprise);

		final RegpmEtablissement etablissement = new RegpmEtablissement();
		etablissement.setId(487956754L);
		etablissement.setEnseigne(nomEtablissemnt);

		final RegpmIndividu individu = new RegpmIndividu();
		individu.setId(672456L);
		individu.setNom(nomIndividu);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     Collections.singletonList(etablissement),
		                                     Collections.singletonList(individu));
		final ExtractedDataCache cache = new ExtractedDataCache(graphe);
		cache.registerDataExtractor(Nom.class,
		                            e -> new Nom(e.getEnseigne()),
		                            e -> new Nom(e.getEnseigne()),
		                            i -> new Nom(i.getNom()));
		cache.registerDataExtractor(AutreNom.class,
		                            e -> new AutreNom(e.getEnseigne()),
		                            e -> new AutreNom(e.getEnseigne()),
		                            i -> new AutreNom(i.getNom()));

		// premiers appels pour remplir le cache
		Assert.assertEquals(nomEntreprise, cache.getExtractedData(Nom.class, EntityKey.of(entreprise)).get());
		Assert.assertEquals(nomEtablissemnt, cache.getExtractedData(Nom.class, EntityKey.of(etablissement)).get());
		Assert.assertEquals(nomIndividu, cache.getExtractedData(Nom.class, EntityKey.of(individu)).get());

		// changement des noms... dans les entités (-> rien ne doit changer dans le cache, car il est plein...)
		entreprise.setEnseigne(nomEntreprise + " - TEST");
		etablissement.setEnseigne(nomEtablissemnt + " - TEST");
		individu.setNom(nomIndividu + " - TEST");

		// le cache est toujours actif -> les anciens noms sortent encore
		Assert.assertEquals(nomEntreprise, cache.getExtractedData(Nom.class, EntityKey.of(entreprise)).get());
		Assert.assertEquals(nomEtablissemnt, cache.getExtractedData(Nom.class, EntityKey.of(etablissement)).get());
		Assert.assertEquals(nomIndividu, cache.getExtractedData(Nom.class, EntityKey.of(individu)).get());

		// en revanche, dans la partie non-cachée, ce sont bien les nouveaux noms qui sortent
		Assert.assertEquals(nomEntreprise + " - TEST", cache.getExtractedData(AutreNom.class, EntityKey.of(entreprise)).get());
		Assert.assertEquals(nomEtablissemnt + " - TEST", cache.getExtractedData(AutreNom.class, EntityKey.of(etablissement)).get());
		Assert.assertEquals(nomIndividu + " - TEST", cache.getExtractedData(AutreNom.class, EntityKey.of(individu)).get());
	}

	@Test
	public void testRegistrationIncrementale() throws Exception {
		final class Nom implements Supplier<String> {
			private final String nom;

			public Nom(String nom) {
				this.nom = nom;
			}

			@Override
			public String get() {
				return nom;
			}
		}

		final String nomEntreprise = "Ma petite entreprise";
		final String nomEtablissemnt = "Mon établissement à moi";
		final String nomIndividu = "Moi";

		final RegpmEntreprise entreprise = new RegpmEntreprise();
		entreprise.setId(547684572L);
		entreprise.setEnseigne(nomEntreprise);

		final RegpmEtablissement etablissement = new RegpmEtablissement();
		etablissement.setId(487956754L);
		etablissement.setEnseigne(nomEtablissemnt);

		final RegpmIndividu individu = new RegpmIndividu();
		individu.setId(672456L);
		individu.setNom(nomIndividu);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     Collections.singletonList(etablissement),
		                                     Collections.singletonList(individu));
		final ExtractedDataCache cache = new ExtractedDataCache(graphe);
		cache.registerDataExtractor(Nom.class,
		                            e -> new Nom(e.getEnseigne()),
		                            null,
		                            null);
		cache.registerDataExtractor(Nom.class,
		                            null,
		                            e -> new Nom(e.getEnseigne()),
		                            null);
		cache.registerDataExtractor(Nom.class,
		                            null,
		                            null,
		                            i -> new Nom(i.getNom()));

		// les valeurs sont-elles toutes acceptées ?
		Assert.assertEquals(nomEntreprise, cache.getExtractedData(Nom.class, EntityKey.of(entreprise)).get());
		Assert.assertEquals(nomEtablissemnt, cache.getExtractedData(Nom.class, EntityKey.of(etablissement)).get());
		Assert.assertEquals(nomIndividu, cache.getExtractedData(Nom.class, EntityKey.of(individu)).get());

		// changement des noms... dans les entités (-> rien ne doit changer dans le cache, car il est plein...)
		entreprise.setEnseigne(nomEntreprise + " - TEST");
		etablissement.setEnseigne(nomEtablissemnt + " - TEST");
		individu.setNom(nomIndividu + " - TEST");

		// le cache est toujours actif -> les anciens noms sortent encore
		Assert.assertEquals(nomEntreprise, cache.getExtractedData(Nom.class, EntityKey.of(entreprise)).get());
		Assert.assertEquals(nomEtablissemnt, cache.getExtractedData(Nom.class, EntityKey.of(etablissement)).get());
		Assert.assertEquals(nomIndividu, cache.getExtractedData(Nom.class, EntityKey.of(individu)).get());
	}

	@Test
	public void testSansRegistrationClasse() throws Exception {

		final class Nom implements Supplier<String> {
			private final String nom;

			public Nom(String nom) {
				this.nom = nom;
			}

			@Override
			public String get() {
				return nom;
			}
		}

		final String nomEntreprise = "Ma petite entreprise";
		final String nomEtablissemnt = "Mon établissement à moi";
		final String nomIndividu = "Moi";

		final RegpmEntreprise entreprise = new RegpmEntreprise();
		entreprise.setId(547684572L);
		entreprise.setEnseigne(nomEntreprise);

		final RegpmEtablissement etablissement = new RegpmEtablissement();
		etablissement.setId(487956754L);
		etablissement.setEnseigne(nomEtablissemnt);

		final RegpmIndividu individu = new RegpmIndividu();
		individu.setId(672456L);
		individu.setNom(nomIndividu);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     Collections.singletonList(etablissement),
		                                     Collections.singletonList(individu));
		final ExtractedDataCache cache = new ExtractedDataCache(graphe);

		try {
			cache.getExtractedData(Nom.class, EntityKey.of(entreprise));
			Assert.fail("Aurait dû exploser en l'absence de régistration sur la classe demandée");
		}
		catch (IllegalStateException e) {
			Assert.assertEquals("Aucun extracteur n'a été enregistré pour la classe " + Nom.class.getName(), e.getMessage());
		}
		try {
			cache.getExtractedData(Nom.class, EntityKey.of(etablissement));
			Assert.fail("Aurait dû exploser en l'absence de régistration sur la classe demandée");
		}
		catch (IllegalStateException e) {
			Assert.assertEquals("Aucun extracteur n'a été enregistré pour la classe " + Nom.class.getName(), e.getMessage());
		}
		try {
			cache.getExtractedData(Nom.class, EntityKey.of(individu));
			Assert.fail("Aurait dû exploser en l'absence de régistration sur la classe demandée");
		}
		catch (IllegalStateException e) {
			Assert.assertEquals("Aucun extracteur n'a été enregistré pour la classe " + Nom.class.getName(), e.getMessage());
		}
	}

	@Test
	public void testSansRegistrationExtracteur() throws Exception {

		final class Nom implements Supplier<String> {
			private final String nom;

			public Nom(String nom) {
				this.nom = nom;
			}

			@Override
			public String get() {
				return nom;
			}
		}

		final String nomEntreprise = "Ma petite entreprise";
		final String nomEtablissemnt = "Mon établissement à moi";
		final String nomIndividu = "Moi";

		final RegpmEntreprise entreprise = new RegpmEntreprise();
		entreprise.setId(547684572L);
		entreprise.setEnseigne(nomEntreprise);

		final RegpmEtablissement etablissement = new RegpmEtablissement();
		etablissement.setId(487956754L);
		etablissement.setEnseigne(nomEtablissemnt);

		final RegpmIndividu individu = new RegpmIndividu();
		individu.setId(672456L);
		individu.setNom(nomIndividu);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     Collections.singletonList(etablissement),
		                                     Collections.singletonList(individu));
		final ExtractedDataCache cache = new ExtractedDataCache(graphe);
		cache.registerDataExtractor(Nom.class,
		                            e -> new Nom(e.getEnseigne()),
		                            null,
		                            null);

		Assert.assertEquals(nomEntreprise, cache.getExtractedData(Nom.class, EntityKey.of(entreprise)).get());
		try {
			cache.getExtractedData(Nom.class, EntityKey.of(etablissement));
			Assert.fail("Aurait dû exploser en l'absence d'extracteur sur la classe demandée");
		}
		catch (IllegalStateException e) {
			Assert.assertEquals("Pas d'extracteur enregistré pour la classe " + Nom.class.getName() + " et le type d'entité ETABLISSEMENT", e.getMessage());
		}
		try {
			cache.getExtractedData(Nom.class, EntityKey.of(individu));
			Assert.fail("Aurait dû exploser en l'absence d'extracteur sur la classe demandée");
		}
		catch (IllegalStateException e) {
			Assert.assertEquals("Pas d'extracteur enregistré pour la classe " + Nom.class.getName() + " et le type d'entité INDIVIDU", e.getMessage());
		}
	}

	@Test
	public void testConflitRegistration() throws Exception {
		final class Nom implements Supplier<String> {
			private final String nom;

			public Nom(String nom) {
				this.nom = nom;
			}

			@Override
			public String get() {
				return nom;
			}
		}

		final String nomIndividu = "Moi";
		final String prenomIndividu = "Prémoi";

		final RegpmIndividu individu = new RegpmIndividu();
		individu.setId(672456L);
		individu.setNom(nomIndividu);
		individu.setPrenom(prenomIndividu);

		final Graphe graphe = new MockGraphe(null,
		                                     null,
		                                     Collections.singletonList(individu));
		final ExtractedDataCache cache = new ExtractedDataCache(graphe);
		cache.registerDataExtractor(Nom.class,
		                            null,
		                            null,
		                            i -> new Nom(i.getNom()));

		try {
			cache.registerDataExtractor(Nom.class,
			                            null,
			                            null,
			                            i -> new Nom(i.getPrenom()));
			Assert.fail("Aurait dû exploser en raison du deuxième enregistrement pour l'individu");
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("Deux extracteurs 'individu' en conflit.", e.getMessage());
		}
	}

	@Test
	public void testNullCachedValue() throws Exception {
		final class Nom implements Supplier<String> {
			private final String nom;

			public Nom(String nom) {
				this.nom = nom;
			}

			@Override
			public String get() {
				return nom;
			}
		}

		final String nomIndividu = "Moi";
		final String prenomIndividu = "Prémoi";

		final RegpmIndividu individu = new RegpmIndividu();
		individu.setId(672456L);
		individu.setNom(nomIndividu);
		individu.setPrenom(null);

		final Graphe graphe = new MockGraphe(null,
		                                     null,
		                                     Collections.singletonList(individu));
		final ExtractedDataCache cache = new ExtractedDataCache(graphe);
		cache.registerDataExtractor(Nom.class,
		                            null,
		                            null,
		                            i -> i.getPrenom() == null ? null : new Nom(i.getPrenom()));

		// récupération de la valeur : pour l'instant, c'est null
		Assert.assertNull(cache.getExtractedData(Nom.class, EntityKey.of(individu)));

		// même si on mets une valeur maintenant, le null a été conservé en cache
		individu.setPrenom(prenomIndividu);
		Assert.assertNull(cache.getExtractedData(Nom.class, EntityKey.of(individu)));
	}
}
