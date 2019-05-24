package ch.vd.unireg.interfaces.infra.cache;

import java.util.concurrent.atomic.AtomicInteger;

import net.sf.ehcache.CacheManager;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.cache.UniregCacheManagerImpl;
import ch.vd.unireg.interfaces.infra.InfrastructureConnector;
import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.data.TypeAffranchissement;

public class ServiceInfrastructurePaysCacheTest {

	private static final RegDate DATE_FIN_PAYS_ENCHANTE = RegDate.get(1999, 12, 31);
	private static final RegDate DATE_DEBUT_PAYS_MERVEILLEUX = DATE_FIN_PAYS_ENCHANTE.getOneDayAfter();

	private static final RegDate DATE_FIN_PAYS_TRISTE = RegDate.get(RegDate.get().year() + 1, 12, 31);  // demain, tout ira mieux...
	private static final RegDate DATE_DEBUT_PAYS_HEUREUX = DATE_FIN_PAYS_TRISTE.getOneDayAfter();

	private static final int noOfsPaysEnchante = 8888;
	private static final String isoPaysEnchante = "PE";

	private static final int noOfsPaysTriste = 8889;
	private static final String isoPaysTriste = "PT";

	private InfrastructureConnectorCache cache;
	private Test target;

	private static class MockPays implements Pays {

		public static final MockPays Suisse = new MockPays(InfrastructureConnector.noOfsSuisse, "CH", "Suisse");
		public static final MockPays PaysEnchante = new MockPays(noOfsPaysEnchante, isoPaysEnchante, "Pays enchanté", null, DATE_FIN_PAYS_ENCHANTE);
		public static final MockPays PaysMerveilleux = new MockPays(noOfsPaysEnchante, isoPaysEnchante, "Pays merveilleux", DATE_DEBUT_PAYS_MERVEILLEUX, null);
		public static final MockPays PaysTriste = new MockPays(noOfsPaysTriste, isoPaysTriste, "Pays triste", null, DATE_FIN_PAYS_TRISTE);
		public static final MockPays PaysHeureux = new MockPays(noOfsPaysTriste, isoPaysTriste, "Pays heureux", DATE_DEBUT_PAYS_HEUREUX, null);

		private final int noOfs;
		private final String codeIso;
		private final String nom;
		private final DateRange validityRange;

		private MockPays(int noOfs, String codeIso, String nom) {
			this(noOfs, codeIso, nom, null, null);
		}

		private MockPays(int noOfs, String codeIso, String nom, RegDate dateDebut, RegDate dateFin) {
			this.noOfs = noOfs;
			this.codeIso = codeIso;
			this.nom = nom;
			this.validityRange = new DateRangeHelper.Range(dateDebut, dateFin);
		}

		@Override
		public boolean isSuisse() {
			return noOfs == InfrastructureConnector.noOfsSuisse;
		}

		@Override
		public boolean isValide() {
			return true;
		}

		@Override
		public boolean isEtatSouverain() {
			return true;
		}

		@Override
		public int getNoOfsEtatSouverain() {
			return noOfs;
		}

		@Override
		public String getCodeIso2() {
			return codeIso;
		}

		@Override
		public String getCodeIso3() {
			throw new NotImplementedException("");
		}

		@Override
		public boolean isValidAt(RegDate date) {
			return validityRange.isValidAt(date);
		}

		@Override
		public RegDate getDateDebut() {
			return validityRange.getDateDebut();
		}

		@Override
		public RegDate getDateFin() {
			return validityRange.getDateFin();
		}

		@Override
		public String getNomCourt() {
			return nom;
		}

		@Override
		public String getNomOfficiel() {
			return nom;
		}

		@Override
		public int getNoOFS() {
			return noOfs;
		}

		@Override
		public String getSigleOFS() {
			throw new NotImplementedException("");
		}

		@Override
		public TypeAffranchissement getTypeAffranchissement() {
			throw new NotImplementedException("");
		}
	}

	/**
	 * On ne connait que la Suisse et le Pays enchanté, qui a changé de nom pour devenir plus tard le Pays merveilleux
	 */
	private static class Test extends NotImplementedInfrastructureConnector {

		private AtomicInteger callsOfs = new AtomicInteger(0);
		private AtomicInteger callsIso = new AtomicInteger(0);

		public int getCallsOfs() {
			return callsOfs.intValue();
		}

		public int getCallsIso() {
			return callsIso.intValue();
		}

		public void reset() {
			callsIso.set(0);
			callsOfs.set(0);
		}

		@Override
		public Pays getPays(int numeroOFS, @Nullable RegDate date) throws InfrastructureException {

			// "null" = date du jour
			if (date == null) {
				date = RegDate.get();
			}

			callsOfs.incrementAndGet();
			if (numeroOFS == MockPays.Suisse.getNoOFS()) {
				return MockPays.Suisse;
			}

			if (numeroOFS == noOfsPaysEnchante) {
				if (MockPays.PaysEnchante.isValidAt(date)) {
					return MockPays.PaysEnchante;
				}
				else {
					Assert.assertTrue(MockPays.PaysMerveilleux.isValidAt(date));
					Assert.assertEquals(numeroOFS, MockPays.PaysMerveilleux.getNoOFS());
					return MockPays.PaysMerveilleux;
				}
			}

			if (numeroOFS == noOfsPaysTriste) {
				if (MockPays.PaysTriste.isValidAt(date)) {
					return MockPays.PaysTriste;
				}
				else {
					Assert.assertTrue(MockPays.PaysHeureux.isValidAt(date));
					Assert.assertEquals(numeroOFS, MockPays.PaysHeureux.getNoOFS());
					return MockPays.PaysHeureux;
				}
			}
			return null;
		}

		@Override
		public Pays getPays(@NotNull String codePays, @Nullable RegDate date) throws InfrastructureException {

			// "null" = date du jour
			if (date == null) {
				date = RegDate.get();
			}

			callsIso.incrementAndGet();
			if (codePays.equals(MockPays.Suisse.getCodeIso2())) {
				return MockPays.Suisse;
			}

			if (codePays.equals(isoPaysEnchante)) {
				if (MockPays.PaysEnchante.isValidAt(date)) {
					return MockPays.PaysEnchante;
				}
				else {
					Assert.assertTrue(MockPays.PaysMerveilleux.isValidAt(date));
					Assert.assertEquals(codePays, MockPays.PaysMerveilleux.getCodeIso2());
					return MockPays.PaysMerveilleux;
				}
			}

			if (codePays.equals(isoPaysTriste)) {
				if (MockPays.PaysTriste.isValidAt(date)) {
					return MockPays.PaysTriste;
				}
				else {
					Assert.assertTrue(MockPays.PaysHeureux.isValidAt(date));
					Assert.assertEquals(codePays, MockPays.PaysHeureux.getCodeIso2());
					return MockPays.PaysHeureux;
				}
			}
			return null;
		}
	}

	@Before
	public void setup() throws Exception {
		final CacheManager manager = CacheManager.create(ResourceUtils.getFile("classpath:ut/ehcache.xml").getPath());

		cache = new InfrastructureConnectorCache();
		cache.setCache(manager.getCache("infraConnector"));
		cache.setShortLivedCache(manager.getCache("infraConnectorShortLived"));
		cache.setUniregCacheManager(new UniregCacheManagerImpl());
		target = new Test();
		cache.setTarget(target);
		cache.afterPropertiesSet();
	}

	@org.junit.Test
	public void testGetPaysParNumeroOfs() throws Exception {
		target.reset();

		// test sur la Suisse
		{
			{
				// premier appel -> véritable appel au service
				final Pays suisse = cache.getPays(MockPays.Suisse.getNoOFS(), RegDate.get(1975, 1, 1));
				Assert.assertNotNull(suisse);
				Assert.assertEquals(1, target.getCallsOfs());
				Assert.assertEquals(0, target.getCallsIso());
				Assert.assertEquals("CH", suisse.getCodeIso2());
				Assert.assertEquals("Suisse", suisse.getNomCourt());
				Assert.assertNull(suisse.getDateDebut());
				Assert.assertNull(suisse.getDateFin());
			}
			{
				// appel avec la même date -> cache
				final Pays suisse = cache.getPays(MockPays.Suisse.getNoOFS(), RegDate.get(1975, 1, 1));
				Assert.assertNotNull(suisse);
				Assert.assertEquals(1, target.getCallsOfs());
				Assert.assertEquals(0, target.getCallsIso());
				Assert.assertEquals("CH", suisse.getCodeIso2());
				Assert.assertEquals("Suisse", suisse.getNomCourt());
				Assert.assertNull(suisse.getDateDebut());
				Assert.assertNull(suisse.getDateFin());
			}
			{
				// appel avec autre date dans la période de validité du pays -> cache
				final Pays suisse = cache.getPays(MockPays.Suisse.getNoOFS(), RegDate.get(2000, 1, 1));
				Assert.assertNotNull(suisse);
				Assert.assertEquals(1, target.getCallsOfs());
				Assert.assertEquals(0, target.getCallsIso());
				Assert.assertEquals("CH", suisse.getCodeIso2());
				Assert.assertEquals("Suisse", suisse.getNomCourt());
				Assert.assertNull(suisse.getDateDebut());
				Assert.assertNull(suisse.getDateFin());
			}
			{
				final Pays suisse = cache.getPays(MockPays.Suisse.getNoOFS(), null);
				Assert.assertNotNull(suisse);
				Assert.assertEquals(1, target.getCallsOfs());
				Assert.assertEquals(0, target.getCallsIso());
				Assert.assertEquals("CH", suisse.getCodeIso2());
				Assert.assertEquals("Suisse", suisse.getNomCourt());
				Assert.assertNull(suisse.getDateDebut());
				Assert.assertNull(suisse.getDateFin());
			}
		}

		// test sur le pays qui change de nom au cours de son existence (dans le passé)
		{
			{
				// premier appel
				final Pays pays = cache.getPays(noOfsPaysEnchante, RegDate.get(1975, 1, 1));
				Assert.assertNotNull(pays);
				Assert.assertEquals(2, target.getCallsOfs());
				Assert.assertEquals(0, target.getCallsIso());
				Assert.assertEquals(isoPaysEnchante, pays.getCodeIso2());
				Assert.assertEquals("Pays enchanté", pays.getNomCourt());
				Assert.assertNull(pays.getDateDebut());
				Assert.assertEquals(DATE_FIN_PAYS_ENCHANTE, pays.getDateFin());
			}
			{
				// deuxième appel identique
				final Pays pays = cache.getPays(noOfsPaysEnchante, RegDate.get(1975, 1, 1));
				Assert.assertNotNull(pays);
				Assert.assertEquals(2, target.getCallsOfs());
				Assert.assertEquals(0, target.getCallsIso());
				Assert.assertEquals(isoPaysEnchante, pays.getCodeIso2());
				Assert.assertEquals("Pays enchanté", pays.getNomCourt());
				Assert.assertNull(pays.getDateDebut());
				Assert.assertEquals(DATE_FIN_PAYS_ENCHANTE, pays.getDateFin());
			}
			{
				// appel avec date différente, mais toujours dans la période de validité de la valeur déjà demandée
				final Pays pays = cache.getPays(noOfsPaysEnchante, RegDate.get(1990, 1, 1));
				Assert.assertNotNull(pays);
				Assert.assertEquals(2, target.getCallsOfs());
				Assert.assertEquals(0, target.getCallsIso());
				Assert.assertEquals(isoPaysEnchante, pays.getCodeIso2());
				Assert.assertEquals("Pays enchanté", pays.getNomCourt());
				Assert.assertNull(pays.getDateDebut());
				Assert.assertEquals(DATE_FIN_PAYS_ENCHANTE, pays.getDateFin());
			}
			{
				// appel avec date hors de la période de validité de la valeur déjà demandée -> nouvel appel au cache
				final Pays pays = cache.getPays(noOfsPaysEnchante, RegDate.get(2001, 1, 1));
				Assert.assertNotNull(pays);
				Assert.assertEquals(3, target.getCallsOfs());
				Assert.assertEquals(0, target.getCallsIso());
				Assert.assertEquals(isoPaysEnchante, pays.getCodeIso2());
				Assert.assertEquals("Pays merveilleux", pays.getNomCourt());
				Assert.assertEquals(DATE_DEBUT_PAYS_MERVEILLEUX, pays.getDateDebut());
				Assert.assertNull(pays.getDateFin());
			}
			{
				// appel avec date hors de la période de validité de la valeur déjà demandée -> nouvel appel au cache
				final Pays pays = cache.getPays(noOfsPaysEnchante, RegDate.get(2010, 1, 1));
				Assert.assertNotNull(pays);
				Assert.assertEquals(3, target.getCallsOfs());
				Assert.assertEquals(0, target.getCallsIso());
				Assert.assertEquals(isoPaysEnchante, pays.getCodeIso2());
				Assert.assertEquals("Pays merveilleux", pays.getNomCourt());
				Assert.assertEquals(DATE_DEBUT_PAYS_MERVEILLEUX, pays.getDateDebut());
				Assert.assertNull(pays.getDateFin());
			}
			{
				// appel avec date hors de la période de validité de la valeur déjà demandée (= date du jour) -> nouvel appel au cache
				final Pays pays = cache.getPays(noOfsPaysEnchante, null);
				Assert.assertNotNull(pays);
				Assert.assertEquals(3, target.getCallsOfs());
				Assert.assertEquals(0, target.getCallsIso());
				Assert.assertEquals(isoPaysEnchante, pays.getCodeIso2());
				Assert.assertEquals("Pays merveilleux", pays.getNomCourt());
				Assert.assertEquals(DATE_DEBUT_PAYS_MERVEILLEUX, pays.getDateDebut());
				Assert.assertNull(pays.getDateFin());
			}
		}

		// test sur le pays qui change de nom au cours de son existence (dans le futur)
		{
			{
				// premier appel
				final Pays pays = cache.getPays(noOfsPaysTriste, RegDate.get());
				Assert.assertNotNull(pays);
				Assert.assertEquals(4, target.getCallsOfs());
				Assert.assertEquals(0, target.getCallsIso());
				Assert.assertEquals(isoPaysTriste, pays.getCodeIso2());
				Assert.assertEquals("Pays triste", pays.getNomCourt());
				Assert.assertNull(pays.getDateDebut());
				Assert.assertEquals(DATE_FIN_PAYS_TRISTE, pays.getDateFin());
			}
			{
				// appel avec date nulle = date du jour, qui devrait être dans le cache
				final Pays pays = cache.getPays(noOfsPaysTriste, null);
				Assert.assertNotNull(pays);
				Assert.assertEquals(4, target.getCallsOfs());
				Assert.assertEquals(0, target.getCallsIso());
				Assert.assertEquals(isoPaysTriste, pays.getCodeIso2());
				Assert.assertEquals("Pays triste", pays.getNomCourt());
				Assert.assertNull(pays.getDateDebut());
				Assert.assertEquals(DATE_FIN_PAYS_TRISTE, pays.getDateFin());
			}
		}
	}

	@org.junit.Test
	public void testGetPaysParCodeIso() throws Exception {
		target.reset();

		// test sur la Suisse
		{
			{
				// premier appel -> véritable appel au service
				final Pays suisse = cache.getPays("CH", RegDate.get(1975, 1, 1));
				Assert.assertNotNull(suisse);
				Assert.assertEquals(0, target.getCallsOfs());
				Assert.assertEquals(1, target.getCallsIso());
				Assert.assertEquals(MockPays.Suisse.getNoOFS(), suisse.getNoOFS());
				Assert.assertEquals("Suisse", suisse.getNomCourt());
				Assert.assertNull(suisse.getDateDebut());
				Assert.assertNull(suisse.getDateFin());
			}
			{
				// appel avec la même date -> cache
				final Pays suisse = cache.getPays("CH", RegDate.get(1975, 1, 1));
				Assert.assertNotNull(suisse);
				Assert.assertEquals(0, target.getCallsOfs());
				Assert.assertEquals(1, target.getCallsIso());
				Assert.assertEquals(MockPays.Suisse.getNoOFS(), suisse.getNoOFS());
				Assert.assertEquals("Suisse", suisse.getNomCourt());
				Assert.assertNull(suisse.getDateDebut());
				Assert.assertNull(suisse.getDateFin());
			}
			{
				// appel avec autre date dans la période de validité du pays -> cache
				final Pays suisse = cache.getPays("CH", RegDate.get(2000, 1, 1));
				Assert.assertNotNull(suisse);
				Assert.assertEquals(0, target.getCallsOfs());
				Assert.assertEquals(1, target.getCallsIso());
				Assert.assertEquals(MockPays.Suisse.getNoOFS(), suisse.getNoOFS());
				Assert.assertEquals("Suisse", suisse.getNomCourt());
				Assert.assertNull(suisse.getDateDebut());
				Assert.assertNull(suisse.getDateFin());
			}
			{
				final Pays suisse = cache.getPays("CH", null);
				Assert.assertNotNull(suisse);
				Assert.assertEquals(0, target.getCallsOfs());
				Assert.assertEquals(1, target.getCallsIso());
				Assert.assertEquals(MockPays.Suisse.getNoOFS(), suisse.getNoOFS());
				Assert.assertEquals("Suisse", suisse.getNomCourt());
				Assert.assertNull(suisse.getDateDebut());
				Assert.assertNull(suisse.getDateFin());
			}
		}

		// test sur le pays qui change de nom au cours de son existence (dans le passé)
		{
			{
				// premier appel
				final Pays pays = cache.getPays(isoPaysEnchante, RegDate.get(1975, 1, 1));
				Assert.assertNotNull(pays);
				Assert.assertEquals(0, target.getCallsOfs());
				Assert.assertEquals(2, target.getCallsIso());
				Assert.assertEquals(noOfsPaysEnchante, pays.getNoOFS());
				Assert.assertEquals("Pays enchanté", pays.getNomCourt());
				Assert.assertNull(pays.getDateDebut());
				Assert.assertEquals(DATE_FIN_PAYS_ENCHANTE, pays.getDateFin());
			}
			{
				// deuxième appel identique
				final Pays pays = cache.getPays(isoPaysEnchante, RegDate.get(1975, 1, 1));
				Assert.assertNotNull(pays);
				Assert.assertEquals(0, target.getCallsOfs());
				Assert.assertEquals(2, target.getCallsIso());
				Assert.assertEquals(noOfsPaysEnchante, pays.getNoOFS());
				Assert.assertEquals("Pays enchanté", pays.getNomCourt());
				Assert.assertNull(pays.getDateDebut());
				Assert.assertEquals(DATE_FIN_PAYS_ENCHANTE, pays.getDateFin());
			}
			{
				// appel avec date différente, mais toujours dans la période de validité de la valeur déjà demandée
				final Pays pays = cache.getPays(isoPaysEnchante, RegDate.get(1990, 1, 1));
				Assert.assertNotNull(pays);
				Assert.assertEquals(0, target.getCallsOfs());
				Assert.assertEquals(2, target.getCallsIso());
				Assert.assertEquals(noOfsPaysEnchante, pays.getNoOFS());
				Assert.assertEquals("Pays enchanté", pays.getNomCourt());
				Assert.assertNull(pays.getDateDebut());
				Assert.assertEquals(DATE_FIN_PAYS_ENCHANTE, pays.getDateFin());
			}
			{
				// appel avec date hors de la période de validité de la valeur déjà demandée -> nouvel appel au cache
				final Pays pays = cache.getPays(isoPaysEnchante, RegDate.get(2001, 1, 1));
				Assert.assertNotNull(pays);
				Assert.assertEquals(0, target.getCallsOfs());
				Assert.assertEquals(3, target.getCallsIso());
				Assert.assertEquals(noOfsPaysEnchante, pays.getNoOFS());
				Assert.assertEquals("Pays merveilleux", pays.getNomCourt());
				Assert.assertEquals(DATE_DEBUT_PAYS_MERVEILLEUX, pays.getDateDebut());
				Assert.assertNull(pays.getDateFin());
			}
			{
				// appel avec date hors de la période de validité de la valeur déjà demandée -> nouvel appel au cache
				final Pays pays = cache.getPays(isoPaysEnchante, RegDate.get(2010, 1, 1));
				Assert.assertNotNull(pays);
				Assert.assertEquals(0, target.getCallsOfs());
				Assert.assertEquals(3, target.getCallsIso());
				Assert.assertEquals(noOfsPaysEnchante, pays.getNoOFS());
				Assert.assertEquals("Pays merveilleux", pays.getNomCourt());
				Assert.assertEquals(DATE_DEBUT_PAYS_MERVEILLEUX, pays.getDateDebut());
				Assert.assertNull(pays.getDateFin());
			}
			{
				// appel avec date hors de la période de validité de la valeur déjà demandée (= date du jour) -> nouvel appel au cache
				final Pays pays = cache.getPays(isoPaysEnchante, null);
				Assert.assertNotNull(pays);
				Assert.assertEquals(0, target.getCallsOfs());
				Assert.assertEquals(3, target.getCallsIso());
				Assert.assertEquals(noOfsPaysEnchante, pays.getNoOFS());
				Assert.assertEquals("Pays merveilleux", pays.getNomCourt());
				Assert.assertEquals(DATE_DEBUT_PAYS_MERVEILLEUX, pays.getDateDebut());
				Assert.assertNull(pays.getDateFin());
			}
		}

		// test sur le pays qui change de nom au cours de son existence (dans le futur)
		{
			{
				// premier appel
				final Pays pays = cache.getPays(isoPaysTriste, RegDate.get());
				Assert.assertNotNull(pays);
				Assert.assertEquals(0, target.getCallsOfs());
				Assert.assertEquals(4, target.getCallsIso());
				Assert.assertEquals(isoPaysTriste, pays.getCodeIso2());
				Assert.assertEquals("Pays triste", pays.getNomCourt());
				Assert.assertNull(pays.getDateDebut());
				Assert.assertEquals(DATE_FIN_PAYS_TRISTE, pays.getDateFin());
			}
			{
				// appel avec date nulle = date du jour, qui devrait être dans le cache
				final Pays pays = cache.getPays(isoPaysTriste, null);
				Assert.assertNotNull(pays);
				Assert.assertEquals(0, target.getCallsOfs());
				Assert.assertEquals(4, target.getCallsIso());
				Assert.assertEquals(isoPaysTriste, pays.getCodeIso2());
				Assert.assertEquals("Pays triste", pays.getNomCourt());
				Assert.assertNull(pays.getDateDebut());
				Assert.assertEquals(DATE_FIN_PAYS_TRISTE, pays.getDateFin());
			}
		}
	}
}
