package ch.vd.uniregctb.interfaces.model.mock;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.District;
import ch.vd.uniregctb.interfaces.model.OfficeImpot;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;

public class MockCommune extends MockEntityOFS implements Commune {

	private static final String VAUD = "VD";
	private static final String BERN = "BE";
	private static final String ZURICH = "ZH";
	private static final String NEUCHATEL = "NE";
	private static final String FRIBOURG = "FR";
	private static final String GENEVE = "GE";
	private static final String BALE = "BS";

	// Quelques communes vaudoises
	//                                                        OFS   NOM    CANTON
	public static final MockCommune Aubonne = new MockCommune(5422, "Aubonne", VAUD, MockOfficeImpot.OID_ROLLE_AUBONNE, MockDistrict.RolleAubonne);
	public static final MockCommune Bex = new MockCommune(5402, "Bex", VAUD, MockOfficeImpot.OID_AIGLE, MockDistrict.Aigle);
	public static final MockCommune Lausanne = new MockCommune(5586, "Lausanne", VAUD, MockOfficeImpot.OID_LAUSANNE_OUEST, MockDistrict.Lausanne);
	public static final MockCommune Cossonay = new MockCommune(5477, "Cossonay", VAUD, MockOfficeImpot.OID_COSSONAY);
	public static final MockCommune RomainmotierEnvy = new MockCommune(5761, "Romainmôtier-Envy", VAUD, MockOfficeImpot.OID_ORBE, MockDistrict.Orbe);
	public static final MockCommune Croy = new MockCommune(5752, "Croy", VAUD, MockOfficeImpot.OID_ORBE, MockDistrict.Orbe);
	public static final MockCommune Vaulion = new MockCommune(5765, "Vaulion", VAUD, MockOfficeImpot.OID_ORBE, MockDistrict.Orbe);
	public static final MockCommune LesClees = new MockCommune(5750, "Les Clées", VAUD, MockOfficeImpot.OID_ORBE, MockDistrict.Orbe);
	public static final MockCommune VillarsSousYens = new MockCommune(5652, "Villars-sous-Yens", VAUD, MockOfficeImpot.OID_MORGES, MockDistrict.Morges);
	public static final MockCommune Orbe = new MockCommune(5757, "Orbe", VAUD, MockOfficeImpot.OID_ORBE, MockDistrict.Orbe);
	public static final MockCommune Vevey = new MockCommune(5890, "Vevey", VAUD, MockOfficeImpot.OID_VEVEY, MockDistrict.Vevey);
	public static final MockCommune Leysin = new MockCommune(5407, "Leysin", VAUD, MockOfficeImpot.OID_AIGLE, MockDistrict.Aigle);
	public static final MockCommune Renens = new MockCommune(5591, "Renens VD", VAUD, MockOfficeImpot.OID_LAUSANNE_OUEST, MockDistrict.Lausanne);
	public static final MockCommune CheseauxSurLausanne = new MockCommune(5582, "Cheseaux-sur-Lausanne", VAUD, MockOfficeImpot.OID_LAUSANNE_OUEST, MockDistrict.Lausanne);
	public static final MockCommune VufflensLaVille = new MockCommune(5503, "Vufflens-la-Ville", VAUD, MockOfficeImpot.OID_LAUSANNE_OUEST, MockDistrict.Lausanne);
	public static final MockCommune Vallorbe = new MockCommune(5764, "Vallorbe", VAUD, MockOfficeImpot.OID_ORBE, MockDistrict.Orbe);
	public static final MockCommune LIsle = new MockCommune(5486, "L'Isle", VAUD, MockOfficeImpot.OID_MORGES, MockDistrict.Morges);
	public static final MockCommune Chamblon = new MockCommune(5904, "Chamblon", VAUD, MockOfficeImpot.OID_YVERDON, MockDistrict.Yverdon);
	public static final MockCommune GrangesMarnand = new MockCommune(5818, "Granges-près-Marnand", VAUD, MockOfficeImpot.OID_PAYERNE, MockDistrict.Yverdon);
	public static final MockCommune Bussigny = new MockCommune(5624, "Bussigny-près-Lausanne", VAUD, MockOfficeImpot.OID_MORGES, MockDistrict.Morges);
	public static final MockCommune Morges = new MockCommune(5642, "Morges", VAUD, MockOfficeImpot.OID_MORGES, MockDistrict.Morges);
	public static final MockCommune Echallens = new MockCommune(5518, "Echallens", VAUD, MockOfficeImpot.OID_ECHALLENS, MockDistrict.Echallens);
	public static final MockCommune Lonay = new MockCommune(5638, "Lonay", VAUD, MockOfficeImpot.OID_MORGES, MockDistrict.Morges);
	public static final MockCommune RomanelSurLausanne = new MockCommune(5592, "Romanel-sur-Lausanne", VAUD, MockOfficeImpot.OID_LAUSANNE_OUEST, MockDistrict.Lausanne);
	public static final MockCommune Moudon = new MockCommune(5678, "Moudon", VAUD, MockOfficeImpot.OID_MOUDON, MockDistrict.Moudon);
	public static final MockCommune Pully = new MockCommune(5590, "Pully", VAUD, MockOfficeImpot.OID_LAVAUX, MockDistrict.Lavaux);
	public static final MockCommune Prilly = new MockCommune(5589, "Prilly", VAUD, MockOfficeImpot.OID_LAUSANNE_OUEST, MockDistrict.Lausanne);
	public static final MockCommune Nyon = new MockCommune(5724, "Nyon", VAUD, MockOfficeImpot.OID_NYON, MockDistrict.Nyon);
	public static final MockCommune Aigle = new MockCommune(5401, "Aigle", VAUD, MockOfficeImpot.OID_AIGLE, MockDistrict.Aigle);
	public static final MockCommune Grandson = new MockCommune(5561, "Grandson", VAUD, MockOfficeImpot.OID_GRANDSON, MockDistrict.Grandson);
	public static final MockCommune ChateauDoex = new MockCommune(5841, "Chateau-d'Oex", VAUD, MockOfficeImpot.OID_PAYS_D_ENHAUT, MockDistrict.PaysDenHaut);

	//
	// Quelques communes fusionnées civilement au 1er juillet 2011, mais seulement au 31 décembre 2011 fiscalement
	// Note : date déplacée une année plus tôt pour pouvoir tester les événements civils qui ne supportent pas les dates dans le futur
	//
	public static final RegDate dateFusion2011 = RegDate.get(2010, 12, 31);
	public static final RegDate veilleFusion2011 = dateFusion2011.getOneDayBefore();

	// -- Villette, Grandvaux, Cully, Riey, Epesses => Bourg-en-Lavaux
	public static final MockCommune Villette = new MockCommune(5612, "Villette", VAUD, MockOfficeImpot.OID_LAVAUX, null, veilleFusion2011);
	public static final MockCommune Grandvaux = new MockCommune(5605, "Grandvaux", VAUD, MockOfficeImpot.OID_LAVAUX, null, veilleFusion2011);
	public static final MockCommune Cully = new MockCommune(5602, "Cully", VAUD, MockOfficeImpot.OID_LAVAUX, null, veilleFusion2011);
	public static final MockCommune Riex = new MockCommune(5608, "Riex", VAUD, MockOfficeImpot.OID_LAVAUX, null, veilleFusion2011);
	public static final MockCommune Epesses = new MockCommune(5603, "Epesses", VAUD, MockOfficeImpot.OID_LAVAUX, null, veilleFusion2011);
	public static final MockCommune BourgEnLavaux = new MockCommune(5613, "Bourg-en-Lavaux", VAUD, MockOfficeImpot.OID_LAVAUX, dateFusion2011, null);

	// -- Yverdon-les-Bains, Gressy => Yverdon-les-Bains
	public static final MockCommune Gressy = new MockCommune(5918, "Gressy", VAUD, MockOfficeImpot.OID_YVERDON, null, veilleFusion2011);
	public static final MockCommune YverdonLesBains = new MockCommune(5938, "Yverdon-les-Bains", VAUD, MockOfficeImpot.OID_YVERDON, null, null);


	// quelques communes non-éternelles
	public static final MockCommune Malapalud = new MockCommune(5526, "Malapalud", VAUD, MockOfficeImpot.OID_ECHALLENS, null, RegDate.get(2008, 12, 31));
	public static final MockCommune ValDeTravers = new MockCommune(6512, "Val-de-Travers", NEUCHATEL, null, RegDate.get(2009, 1, 1), null);

	// Une commune fictive dont la date de fin de validité existe mais est TOUJOURS dans le futur
	public static final MockCommune Mirage = new MockCommune(Integer.MAX_VALUE, "Mirage", VAUD, MockOfficeImpot.OID_PM, null, RegDate.get().getLastDayOfTheMonth());

	// commune avec fractions de commmunes
	public static final MockCommune LAbbaye = new CommuneFractionnee(5871, "L'Abbaye", VAUD, MockOfficeImpot.OID_LA_VALLEE);
	public static final MockCommune LeChenit = new CommuneFractionnee(5872, "Le Chenit", VAUD, MockOfficeImpot.OID_LA_VALLEE);
	public static final MockCommune LeLieu = new CommuneFractionnee(5873, "Le Lieu", VAUD, MockOfficeImpot.OID_LA_VALLEE);

	public static class CommuneFractionnee extends MockCommune {

		private CommuneFractionnee(int noOfs, String nomMinuscule, String sigleCanton, OfficeImpot oid) {
			super(noOfs, nomMinuscule, sigleCanton, oid);
		}

		@Override
		public boolean isPrincipale() {
			return true;
		}
	}

	public static class Fraction extends MockCommune {
		// commune de l'Abbaye
		public static final MockCommune LePont = new Fraction(8010, "Le Pont", VAUD, MockOfficeImpot.OID_LA_VALLEE, MockCommune.LAbbaye.getNoOFSEtendu());
		public static final MockCommune LAbbaye = new Fraction(8011, "L'Abbaye", VAUD, MockOfficeImpot.OID_LA_VALLEE, MockCommune.LAbbaye.getNoOFSEtendu(), MockDistrict.LaVallee);
		public static final MockCommune LesBioux = new Fraction(8012, "Les Bioux", VAUD, MockOfficeImpot.OID_LA_VALLEE, MockCommune.LAbbaye.getNoOFSEtendu());

		// commune du Chenit
		public static final MockCommune LeSentier = new Fraction(8000, "Le Sentier", VAUD, MockOfficeImpot.OID_LA_VALLEE, MockCommune.LeChenit.getNoOFSEtendu());
		public static final MockCommune LeBrassus = new Fraction(8001, "Le Brassus", VAUD, MockOfficeImpot.OID_LA_VALLEE, MockCommune.LeChenit.getNoOFSEtendu());
		public static final MockCommune LOrient = new Fraction(8002, "L'Orient", VAUD, MockOfficeImpot.OID_LA_VALLEE, MockCommune.LeChenit.getNoOFSEtendu());
		public static final MockCommune LeSolliat = new Fraction(8003, "Le Solliat", VAUD, MockOfficeImpot.OID_LA_VALLEE, MockCommune.LeChenit.getNoOFSEtendu());

		// commune du Lieu
		public static final MockCommune LeLieu = new Fraction(8020, "Le Lieu", VAUD, MockOfficeImpot.OID_LA_VALLEE, MockCommune.LeLieu.getNoOFSEtendu());
		public static final MockCommune LeSechey = new Fraction(8021, "Le Séchey", VAUD, MockOfficeImpot.OID_LA_VALLEE, MockCommune.LeLieu.getNoOFSEtendu());
		public static final MockCommune LesCharbonnieres = new Fraction(8022, "Les Charbonnières", VAUD, MockOfficeImpot.OID_LA_VALLEE, MockCommune.LeLieu.getNoOFSEtendu());

		private final int noTechniqueCommuneMere;

		private Fraction(int noOFSetendu, String nomMinuscule, String sigleCanton, OfficeImpot oid, int noTechniqueCommuneMere) {
			super(noOFSetendu, nomMinuscule, sigleCanton, oid);
			this.noTechniqueCommuneMere = noTechniqueCommuneMere;
		}

		private Fraction(int noOFSetendu, String nomMinuscule, String sigleCanton, OfficeImpot oid, int noTechniqueCommuneMere, District district) {
			super(noOFSetendu, nomMinuscule, sigleCanton, oid, district);
			this.noTechniqueCommuneMere = noTechniqueCommuneMere;

		}

		@Override
		public boolean isFraction() {
			return true;
		}

		@Override
		public int getNumTechMere() {
			return noTechniqueCommuneMere;
		}
	}

	// Quelques communes hors-canton
	public static final MockCommune Zurich = new MockCommune(261, "Zurich", ZURICH, null);
	public static final MockCommune Bern = new MockCommune(351, "Bern", BERN, null);
	public static final MockCommune Neuchatel = new MockCommune(6458, "Neuchâtel", NEUCHATEL, null);
	public static final MockCommune Peseux = new MockCommune(6412, "Peseux", NEUCHATEL, null);
	public static final MockCommune Enney = new MockCommune(2132, "Enney", FRIBOURG, null);
	public static final MockCommune Geneve = new MockCommune(6621, "Genève", GENEVE, null);
	public static final MockCommune Bale = new MockCommune(2701, "Basel", BALE, null);

	/**
	 * ce bloque statique est positionné en dernier pour s'assurer que les mocks des communes soient tous initialisés avant d'initialiser
	 * les mocks des bâtiments (où le cycle suivant existant : MockCommune->MockBatiment->MockRue->MockLocalite->MockCommune)
	 */
	static {
		//
		// Liens bâtiment-commune sur quelques communes fusionnées au 1er juillet 2011
		//

		// -- Villette, Grandvaux, Cully, Riey, Epesses => Bourg-en-Lavaux
		Villette.addBatiment(MockBatiment.Villette.BatimentRouteDeLausanne, null, veilleFusion2011);
		Villette.addBatiment(MockBatiment.Villette.BatimentCheminDeCreuxBechet, null, veilleFusion2011);
		Villette.addBatiment(MockBatiment.Villette.BatimentCheminDesGranges, null, veilleFusion2011);
		Grandvaux.addBatiment(MockBatiment.Grandvaux.BatimentSentierDesVinches, null, veilleFusion2011);
		Grandvaux.addBatiment(MockBatiment.Grandvaux.BatimentRouteDeLausanne, null, veilleFusion2011);
		Grandvaux.addBatiment(MockBatiment.Grandvaux.BatimentRueSaintGeorges, null, veilleFusion2011);
		Cully.addBatiment(MockBatiment.Cully.BatimentPlaceDuTemple, null, veilleFusion2011);
		Cully.addBatiment(MockBatiment.Cully.BatimentChCFRamuz, null, veilleFusion2011);
		Cully.addBatiment(MockBatiment.Cully.BatimentChDesColombaires, null, veilleFusion2011);
		Riex.addBatiment(MockBatiment.Riex.BatimentRueDuCollege, null, veilleFusion2011);
		Riex.addBatiment(MockBatiment.Riex.BatimentRouteDeRossetDessus, null, veilleFusion2011);
		Riex.addBatiment(MockBatiment.Riex.BatimentRouteDeLaCorniche, null, veilleFusion2011);
		Epesses.addBatiment(MockBatiment.Epesses.BatimentChDuMont, null, veilleFusion2011);
		Epesses.addBatiment(MockBatiment.Epesses.BatimentLaPlace, null, veilleFusion2011);
		Epesses.addBatiment(MockBatiment.Epesses.BatimentRueDeLaMottaz, null, veilleFusion2011);
		BourgEnLavaux.addAllBatiments(Villette.getBatiments(veilleFusion2011), dateFusion2011, null);
		BourgEnLavaux.addAllBatiments(Grandvaux.getBatiments(veilleFusion2011), dateFusion2011, null);
		BourgEnLavaux.addAllBatiments(Cully.getBatiments(veilleFusion2011), dateFusion2011, null);
		BourgEnLavaux.addAllBatiments(Riex.getBatiments(veilleFusion2011), dateFusion2011, null);
		BourgEnLavaux.addAllBatiments(Epesses.getBatiments(veilleFusion2011), dateFusion2011, null);

		// -- Yverdon-les-Bains, Gressy => Yverdon-les-Bains
		Gressy.addBatiment(MockBatiment.Gressy.BatimentRueCentrale, null, veilleFusion2011);
		Gressy.addBatiment(MockBatiment.Gressy.BatimentLesPechauds, null, veilleFusion2011);
		Gressy.addBatiment(MockBatiment.Gressy.BatimentCheminDuMichamp, null, veilleFusion2011);
		YverdonLesBains.addBatiment(MockBatiment.YverdonLesBains.BatimentCheminDesMuguets, null, null);
		YverdonLesBains.addBatiment(MockBatiment.YverdonLesBains.BatimentQuaiDeLaThiele, null, null);
		YverdonLesBains.addBatiment(MockBatiment.YverdonLesBains.BatimentRueDeLaFaiencerie, null, null);
		YverdonLesBains.addAllBatiments(Gressy.getBatiments(veilleFusion2011), dateFusion2011, null);
	}

	private String nomMinusculeOFS;
	private RegDate dateDebutValidite;
	private RegDate dateFinValidite;
	private final String sigleCanton;
	private final OfficeImpot officeImpot;
	private District district;


	private final List<MockLienCommuneBatiment> liensBatiments = new ArrayList<MockLienCommuneBatiment>();

	private MockCommune(int noOFS, String nomMinuscule, String sigleCanton, OfficeImpot oid, RegDate dateDebutValidite, RegDate dateFinValidite) {
		this(noOFS, nomMinuscule, sigleCanton, oid);
		this.dateDebutValidite = dateDebutValidite;
		this.dateFinValidite = dateFinValidite;
	}

	private MockCommune(int noOFS, String nomMinuscule, String sigleCanton, OfficeImpot oid) {
		super(noOFS, null, nomMinuscule);
		this.sigleCanton = sigleCanton;
		this.officeImpot = oid;

		DefaultMockServiceInfrastructureService.addCommune(this);
	}

	private MockCommune(int noOFS, String nomMinuscule, String sigleCanton, OfficeImpot oid, District district) {
		this(noOFS, nomMinuscule, sigleCanton, oid);
		this.district = district;
	}

	public String getNomMinusculeOFS() {
		return nomMinusculeOFS;
	}

	public void setNomMinusculeOFS(String nomMinusculeOFS) {
		this.nomMinusculeOFS = nomMinusculeOFS;
	}

	@Override
	public RegDate getDateDebutValidite() {
		return dateDebutValidite;
	}

	@Override
	public RegDate getDateFinValidite() {
		return dateFinValidite;
	}

	@Override
	public int getNoOFSEtendu() {
		return getNoOFS();
	}

	@Override
	public int getNumTechMere() {
		throw new RuntimeException("Not implemented!");
	}

	@Override
	public int getNumeroTechnique() {
		return getNoOFSEtendu();
	}

	@Override
	public String getSigleCanton() {
		return sigleCanton;
	}

	@Override
	public boolean isVaudoise() {
		return VAUD.equals(sigleCanton);
	}

	@Override
	public boolean isFraction() {
		return false;
	}

	@Override
	public boolean isPrincipale() {
		return false;
	}

	@Override
	public District getDistrict() {
		return district;
	}

	public OfficeImpot getOfficeImpot() {
		return officeImpot;
	}

	public List<MockLienCommuneBatiment> getLiensBatiments() {
		return liensBatiments;
	}

	public void addBatiment(MockBatiment b, RegDate debutValidite, RegDate finValidite) {
		liensBatiments.add(new MockLienCommuneBatiment(this, b, debutValidite, finValidite));
	}

	public List<MockBatiment> getBatiments(RegDate date) {
		final List<MockBatiment> list = new ArrayList<MockBatiment>();
		for (MockLienCommuneBatiment lien : liensBatiments) {
			if (lien.isValidAt(date)) {
				list.add(lien.getBatiment());
			}
		}
		return list;
	}

	private void addAllBatiments(List<MockBatiment> batiments, RegDate debutValidite, RegDate finValidite) {
		for (MockBatiment batiment : batiments) {
			this.liensBatiments.add(new MockLienCommuneBatiment(this, batiment, debutValidite, finValidite));
		}
	}
}
