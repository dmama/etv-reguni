package ch.vd.unireg.interfaces.infra.mock;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.Commune;

public class MockCommune extends MockEntityOFS implements Commune {

	private static final String VAUD = "VD";
	private static final String JURA = "JU";
	private static final String BERN = "BE";
	private static final String ZURICH = "ZH";
	private static final String ZUG = "ZG";
	private static final String NEUCHATEL = "NE";
	private static final String FRIBOURG = "FR";
	private static final String GENEVE = "GE";
	private static final String BALE = "BS";
	private static final String VALAIS = "VS";
	private static final String GRISONS = "GR";

	// Quelques communes vaudoises
	//                                                        OFS   NOM    CANTON
	public static final MockCommune Aubonne = new MockCommune(5422, "Aubonne", VAUD, MockDistrict.RolleAubonne);
	public static final MockCommune Bex = new MockCommune(5402, "Bex", VAUD, MockDistrict.Aigle);
	public static final MockCommune Lausanne = new MockCommune(5586, "Lausanne", VAUD, MockDistrict.Lausanne);
	public static final MockCommune Cossonay = new MockCommune(5477, "Cossonay", VAUD, MockDistrict.Morges);
	public static final MockCommune Chavornay = new MockCommune(5749, "Chavornay", VAUD, MockDistrict.Orbe);
	public static final MockCommune RomainmotierEnvy = new MockCommune(5761, "Romainmôtier-Envy", VAUD, MockDistrict.Orbe);
	public static final MockCommune Croy = new MockCommune(5752, "Croy", VAUD, MockDistrict.Orbe);
	public static final MockCommune Vaulion = new MockCommune(5765, "Vaulion", VAUD, MockDistrict.Orbe);
	public static final MockCommune LaSarraz = new MockCommune(5498, "La Sarraz", VAUD, MockDistrict.Morges);
	public static final MockCommune LesClees = new MockCommune(5750, "Les Clées", VAUD, MockDistrict.Orbe);
	public static final MockCommune VillarsSousYens = new MockCommune(5652, "Villars-sous-Yens", VAUD, MockDistrict.Morges);
	public static final MockCommune Orbe = new MockCommune(5757, "Orbe", VAUD, MockDistrict.Orbe);
	public static final MockCommune Vevey = new MockCommune(5890, "Vevey", VAUD, MockDistrict.Vevey);
	public static final MockCommune Leysin = new MockCommune(5407, "Leysin", VAUD, MockDistrict.Aigle);
	public static final MockCommune Renens = new MockCommune(5591, "Renens VD", VAUD, MockDistrict.Lausanne);
	public static final MockCommune CheseauxSurLausanne = new MockCommune(5582, "Cheseaux-sur-Lausanne", VAUD, MockDistrict.Lausanne);
	public static final MockCommune VufflensLaVille = new MockCommune(5503, "Vufflens-la-Ville", VAUD, MockDistrict.Lausanne);
	public static final MockCommune Vallorbe = new MockCommune(5764, "Vallorbe", VAUD, MockDistrict.Orbe);
	public static final MockCommune LIsle = new MockCommune(5486, "L'Isle", VAUD, MockDistrict.Morges);
	public static final MockCommune Chamblon = new MockCommune(5904, "Chamblon", VAUD, MockDistrict.Yverdon);
	public static final MockCommune GrangesMarnand = new MockCommune(5818, "Granges-près-Marnand", VAUD, MockDistrict.Payerne);
	public static final MockCommune Bussigny = new MockCommune(5624, "Bussigny-près-Lausanne", VAUD, MockDistrict.Morges);
	public static final MockCommune Morges = new MockCommune(5642, "Morges", VAUD, MockDistrict.Morges);
	public static final MockCommune Echallens = new MockCommune(5518, "Echallens", VAUD, MockDistrict.Echallens);
	public static final MockCommune Lonay = new MockCommune(5638, "Lonay", VAUD, MockDistrict.Morges);
	public static final MockCommune RomanelSurLausanne = new MockCommune(5592, "Romanel-sur-Lausanne", VAUD, MockDistrict.Lausanne);
	public static final MockCommune Moudon = new MockCommune(5678, "Moudon", VAUD, MockDistrict.Moudon);
	public static final MockCommune Pully = new MockCommune(5590, "Pully", VAUD, MockDistrict.Lavaux);
	public static final MockCommune Prilly = new MockCommune(5589, "Prilly", VAUD, MockDistrict.Lausanne);
	public static final MockCommune JouxtensMezery = new MockCommune(5585, "Jouxtens-Mézery", VAUD, MockDistrict.Lausanne);
	public static final MockCommune Nyon = new MockCommune(5724, "Nyon", VAUD, MockDistrict.Nyon);
	public static final MockCommune Aigle = new MockCommune(5401, "Aigle", VAUD, MockDistrict.Aigle);
	public static final MockCommune Grandson = new MockCommune(5561, "Grandson", VAUD, MockDistrict.Grandson);
	public static final MockCommune ChateauDoex = new MockCommune(5841, "Chateau-d'Oex", VAUD, MockDistrict.PaysDenHaut);
	public static final MockCommune Mies = new MockCommune(5723, "Mies", VAUD, MockDistrict.Nyon);

	//
	// Quelques communes fusionnées civilement au 1er juillet 2011, mais seulement au 31 décembre 2011 fiscalement
	// Note : date déplacée une année plus tôt pour pouvoir tester les événements civils qui ne supportent pas les dates dans le futur
	//
	public static final RegDate dateFusion2011 = RegDate.get(2011, 1, 1);
	public static final RegDate veilleFusion2011 = dateFusion2011.getOneDayBefore();

	// -- Villette, Grandvaux, Cully, Riey, Epesses => Bourg-en-Lavaux
	public static final MockCommune Villette = new MockCommune(5612, "Villette", VAUD, MockDistrict.Lavaux, null, veilleFusion2011);
	public static final MockCommune Grandvaux = new MockCommune(5605, "Grandvaux", VAUD, MockDistrict.Lavaux, null, veilleFusion2011);
	public static final MockCommune Cully = new MockCommune(5602, "Cully", VAUD, MockDistrict.Lavaux, null, veilleFusion2011);
	public static final MockCommune Riex = new MockCommune(5608, "Riex", VAUD, MockDistrict.Lavaux, null, veilleFusion2011);
	public static final MockCommune Epesses = new MockCommune(5603, "Epesses", VAUD, MockDistrict.Lavaux, null, veilleFusion2011);
	public static final MockCommune BourgEnLavaux = new MockCommune(5613, "Bourg-en-Lavaux", VAUD, MockDistrict.Lavaux, dateFusion2011, null);

	// -- Yverdon-les-Bains, Gressy => Yverdon-les-Bains
	public static final MockCommune Gressy = new MockCommune(5918, "Gressy", VAUD, MockDistrict.Yverdon, null, veilleFusion2011);
	public static final MockCommune YverdonLesBains = new MockCommune(5938, "Yverdon-les-Bains", VAUD, MockDistrict.Yverdon);


	// quelques communes non-éternelles
	public static final MockCommune Malapalud = new MockCommune(5526, "Malapalud", VAUD, MockDistrict.Echallens, null, RegDate.get(2008, 12, 31));
	public static final MockCommune ValDeTravers = new MockCommune(6512, "Val-de-Travers", NEUCHATEL, null, RegDate.get(2009, 1, 1), null);
	public static final MockCommune JoratMezieres = new MockCommune(5806, "Jorat-Mézières", VAUD, MockDistrict.Lavaux, RegDate.get(2017, 1, 1), null);

	// Une commune fictive dont la date de fin de validité existe mais est TOUJOURS dans le futur
	public static final MockCommune Mirage = new MockCommune(Integer.MAX_VALUE, "Mirage", VAUD, MockDistrict.Vevey, null, RegDate.get().getLastDayOfTheMonth());

	// commune avec fractions de commmunes
	public static final MockCommune LAbbaye = new CommuneFractionnee(5871, "L'Abbaye", VAUD, MockDistrict.LaVallee);
	public static final MockCommune LeChenit = new CommuneFractionnee(5872, "Le Chenit", VAUD, MockDistrict.LaVallee);
	public static final MockCommune LeLieu = new CommuneFractionnee(5873, "Le Lieu", VAUD, MockDistrict.LaVallee);

	public static class CommuneFractionnee extends MockCommune {

		private CommuneFractionnee(int noOfs, String nom, String sigleCanton, MockDistrict district) {
			super(noOfs, nom, sigleCanton, district);
		}

		@Override
		public boolean isPrincipale() {
			return true;
		}
	}

	public static class Fraction extends MockCommune {
		// commune de l'Abbaye
		public static final MockCommune LePont = new Fraction(8010, "Le Pont", VAUD, MockDistrict.LaVallee, MockCommune.LAbbaye.getNoOFS());
		public static final MockCommune LAbbaye = new Fraction(8011, "L'Abbaye", VAUD, MockDistrict.LaVallee, MockCommune.LAbbaye.getNoOFS());
		public static final MockCommune LesBioux = new Fraction(8012, "Les Bioux", VAUD, MockDistrict.LaVallee, MockCommune.LAbbaye.getNoOFS());

		// commune du Chenit
		public static final MockCommune LeSentier = new Fraction(8000, "Le Sentier", VAUD, MockDistrict.LaVallee, MockCommune.LeChenit.getNoOFS());
		public static final MockCommune LeBrassus = new Fraction(8001, "Le Brassus", VAUD, MockDistrict.LaVallee, MockCommune.LeChenit.getNoOFS());
		public static final MockCommune LOrient = new Fraction(8002, "L'Orient", VAUD, MockDistrict.LaVallee, MockCommune.LeChenit.getNoOFS());
		public static final MockCommune LeSolliat = new Fraction(8003, "Le Solliat", VAUD, MockDistrict.LaVallee, MockCommune.LeChenit.getNoOFS());

		// commune du Lieu
		public static final MockCommune LeLieu = new Fraction(8020, "Le Lieu", VAUD, MockDistrict.LaVallee, MockCommune.LeLieu.getNoOFS());
		public static final MockCommune LeSechey = new Fraction(8021, "Le Séchey", VAUD, MockDistrict.LaVallee, MockCommune.LeLieu.getNoOFS());
		public static final MockCommune LesCharbonnieres = new Fraction(8022, "Les Charbonnières", VAUD, MockDistrict.LaVallee, MockCommune.LeLieu.getNoOFS());

		private final int noTechniqueCommuneMere;

		private Fraction(int noOFS, String nom, String sigleCanton, MockDistrict district, int noTechniqueCommuneMere) {
			super(noOFS, nom, sigleCanton, district);
			this.noTechniqueCommuneMere = noTechniqueCommuneMere;
		}

		@Override
		public boolean isFraction() {
			return true;
		}

		@Override
		public int getOfsCommuneMere() {
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
	public static final MockCommune Sierre = new MockCommune(6248, "Sierre", VALAIS, null);
	public static final MockCommune Sion = new MockCommune(6266, "Sion", VALAIS, null);
	public static final MockCommune Conthey = new MockCommune(6023, "Conthey", VALAIS, null);
	public static final MockCommune Chur = new MockCommune(3901, "Chur", GRISONS, null);

	// quelques dates de changement de canton (passée et future)
	public static final RegDate dateIntegrationMoutierJU = RegDate.get().getLastDayOfTheMonth().getOneDayAfter();               // toujours dans le futur...
	public static final RegDate dateChangementCantonPasse = RegDate.get(RegDate.get().year() - 1, 1, 1);            // en début d'année dernière

	// quelques communes qui changent de canton !!
	public static final MockCommune MoutierBE = new MockCommune(700, "Moutier", BERN, null, null, dateIntegrationMoutierJU.getOneDayBefore());
	public static final MockCommune MoutierJU = new MockCommune(700, "Moutier", JURA, null, dateIntegrationMoutierJU, null);
	public static final MockCommune TransfugeZH = new MockCommune(Integer.MAX_VALUE - 1, "Transfuge (ZH)", ZURICH, null, null, dateChangementCantonPasse.getOneDayBefore());
	public static final MockCommune TransfugeZG = new MockCommune(Integer.MAX_VALUE - 1, "Transfuge (ZG)", ZUG, null, dateChangementCantonPasse, null);

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

		// bâtiments dans la Vallée (pas de fusion, mais des fractions...)
		Fraction.LesBioux.addBatiment(MockBatiment.LAbbaye.LesBioux.BatimentLaGrandePartie, null, null);
		Fraction.LesBioux.addBatiment(MockBatiment.LAbbaye.LesBioux.BatimentLeClos, null, null);
		Fraction.LePont.addBatiment(MockBatiment.LAbbaye.LePont.BatimentSurLesQuais, null, null);

		// finalement un bâtiment sur Echallens
		Echallens.addBatiment(MockBatiment.Echallens.BatimentRouteDeMoudon, null, null);
	}

	private RegDate dateDebutValidite;
	private RegDate dateFinValidite;
	private final String sigleCanton;
	private MockDistrict district;


	private final List<MockLienCommuneBatiment> liensBatiments = new ArrayList<>();

	private MockCommune(int noOFS, String nom, String sigleCanton, MockDistrict district, RegDate dateDebutValidite, RegDate dateFinValidite) {
		this(noOFS, nom, sigleCanton, district);
		this.dateDebutValidite = dateDebutValidite;
		this.dateFinValidite = dateFinValidite;
	}

	private MockCommune(int noOFS, String nom, String sigleCanton, MockDistrict district) {
		super(noOFS, null, nom, nom);
		this.sigleCanton = sigleCanton;
		this.district = district;

		DefaultMockServiceInfrastructureService.addCommune(this);
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
	public int getOfsCommuneMere() {
		return -1;
	}

	@Override
	public String getSigleCanton() {
		return sigleCanton;
	}

	@Override
	public String getNomOfficielAvecCanton() {
		final String canton = String.format("(%s)", sigleCanton);
		final String nomOfficiel = getNomOfficiel();
		if (nomOfficiel.endsWith(sigleCanton) || nomOfficiel.endsWith(canton)) {
			return nomOfficiel;
		}
		else {
			return String.format("%s %s", nomOfficiel, canton);
		}
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
	public Integer getCodeDistrict() {
		return district == null ? null : district.getCode();
	}

	@Override
	public Integer getCodeRegion() {
		return district == null ? null : district.getCodeRegion();
	}

	public List<MockLienCommuneBatiment> getLiensBatiments() {
		return liensBatiments;
	}

	public void addBatiment(MockBatiment b, @Nullable RegDate debutValidite, @Nullable RegDate finValidite) {
		liensBatiments.add(new MockLienCommuneBatiment(this, b, debutValidite, finValidite));
	}

	public List<MockBatiment> getBatiments(RegDate date) {
		final List<MockBatiment> list = new ArrayList<>();
		for (MockLienCommuneBatiment lien : liensBatiments) {
			if (lien.isValidAt(date)) {
				list.add(lien.getBatiment());
			}
		}
		return list;
	}

	private void addAllBatiments(List<MockBatiment> batiments, RegDate debutValidite, @Nullable RegDate finValidite) {
		for (MockBatiment batiment : batiments) {
			this.liensBatiments.add(new MockLienCommuneBatiment(this, batiment, debutValidite, finValidite));
		}
	}

	@Override
	public RegDate getDateDebut() {
		return getDateDebutValidite();
	}

	@Override
	public RegDate getDateFin() {
		return getDateFinValidite();
	}
}
