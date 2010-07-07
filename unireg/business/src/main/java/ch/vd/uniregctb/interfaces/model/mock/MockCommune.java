package ch.vd.uniregctb.interfaces.model.mock;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;
import ch.vd.uniregctb.interfaces.model.OfficeImpot;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;

public class MockCommune extends MockEntityOFS implements Commune, CommuneSimple {

	private static final String VAUD = "VD";
	private static final String BERN = "BE";
	private static final String ZURICH = "ZH";
	private static final String NEUCHATEL = "NE";
	private static final String FRIBOURG = "FR";
	private static final String GENEVE = "GE";

	// Quelques communes vaudoises
	//                                                        OFS   NOM    CANTON
	public static final MockCommune Aubonne = new MockCommune(5422, "Aubonne", VAUD, MockOfficeImpot.OID_ROLLE);
	public static final MockCommune Bex = new MockCommune(5402, "Bex", VAUD, MockOfficeImpot.OID_AIGLE);
	public static final MockCommune Lausanne = new MockCommune(5586, "Lausanne", VAUD, MockOfficeImpot.OID_LAUSANNE_OUEST);
	public static final MockCommune Cossonay = new MockCommune(5477, "Cossonay", VAUD, MockOfficeImpot.OID_COSSONAY);
	public static final MockCommune RomainmotierEnvy = new MockCommune(5761, "Romainmôtier-Envy", VAUD, MockOfficeImpot.OID_ORBE);
	public static final MockCommune Croy = new MockCommune(5752, "Croy", VAUD, MockOfficeImpot.OID_ORBE);
	public static final MockCommune Vaulion = new MockCommune(5765, "Vaulion", VAUD, MockOfficeImpot.OID_ORBE);
	public static final MockCommune LesClees = new MockCommune(5750, "Les Clées", VAUD, MockOfficeImpot.OID_ORBE);
	public static final MockCommune VillarsSousYens = new MockCommune(5652, "Villars-sous-Yens", VAUD, MockOfficeImpot.OID_MORGES);
	public static final MockCommune Orbe = new MockCommune(5757, "Orbe", VAUD, MockOfficeImpot.OID_ORBE);
	public static final MockCommune Vevey = new MockCommune(5890, "Vevey", VAUD, MockOfficeImpot.OID_VEVEY);
	public static final MockCommune Leysin = new MockCommune(5407, "Leysin", VAUD, MockOfficeImpot.OID_AIGLE);
	public static final MockCommune Renens = new MockCommune(5591, "Renens VD", VAUD, MockOfficeImpot.OID_LAUSANNE_OUEST);
	public static final MockCommune CheseauxSurLausanne = new MockCommune(5582, "Cheseaux-sur-Lausanne", VAUD, MockOfficeImpot.OID_LAUSANNE_OUEST);
	public static final MockCommune VufflensLaVille = new MockCommune(5503, "Vufflens-la-Ville", VAUD, MockOfficeImpot.OID_LAUSANNE_OUEST);
	public static final MockCommune Vallorbe = new MockCommune(5764, "Vallorbe", VAUD, MockOfficeImpot.OID_ORBE);
	public static final MockCommune LIsle = new MockCommune(5486, "L'Isle", VAUD, MockOfficeImpot.OID_MORGES);
	public static final MockCommune Chamblon = new MockCommune(5904, "Chamblon", VAUD, MockOfficeImpot.OID_YVERDON);
	public static final MockCommune GrangesMarnand = new MockCommune(5818, "Granges-près-Marnand", VAUD, MockOfficeImpot.OID_PAYERNE);
	public static final MockCommune Bussigny = new MockCommune(5624, "Bussigny-près-Lausanne", VAUD, MockOfficeImpot.OID_MORGES);
	public static final MockCommune Echallens = new MockCommune(5518, "Echallens", VAUD, MockOfficeImpot.OID_ECHALLENS);
	public static final MockCommune Lonay = new MockCommune(5638, "Lonay", VAUD, MockOfficeImpot.OID_MORGES);
	public static final MockCommune RomanelSurLausanne = new MockCommune(5592, "Romanel-sur-Lausanne", VAUD, MockOfficeImpot.OID_LAUSANNE_OUEST);

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
		public static final MockCommune LAbbaye = new Fraction(8011, "L'Abbaye", VAUD, MockOfficeImpot.OID_LA_VALLEE, MockCommune.LAbbaye.getNoOFSEtendu());
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

		private final int noOFSetendu;
		private final int noTechniqueCommuneMere;

		private Fraction(int noOFSetendu, String nomMinuscule, String sigleCanton, OfficeImpot oid, int noTechniqueCommuneMere) {
			super(0, nomMinuscule, sigleCanton, oid);
			this.noOFSetendu = noOFSetendu;
			this.noTechniqueCommuneMere = noTechniqueCommuneMere;
		}

		@Override
		public boolean isFraction() {
			return true;
		}

		@Override
		public int getNoOFSEtendu() {
			return noOFSetendu;
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
	public static final MockCommune Enney = new MockCommune(2132, "ENNEY", FRIBOURG, null);
	public static final MockCommune Geneve = new MockCommune(6621, "Genève", GENEVE, null);

	
	private String nomMinusculeOFS;
	private String cantonID;
	private RegDate dateDebutValidite;
	private RegDate dateDebValidite;
	private RegDate dateFinValidite;
	private String noACI;
	private String noCantonal;
	private String nomAbrege;
	private final String sigleCanton;
	private boolean valide;
	private final OfficeImpot officeImpot;

	private MockCommune(int noOFS, String nomMinuscule, String sigleCanton, OfficeImpot oid) {
		super(noOFS, null, nomMinuscule);
		this.sigleCanton = sigleCanton;
		this.officeImpot = oid;

		DefaultMockServiceInfrastructureService.addCommune(this);
	}

	public String getNomMinusculeOFS() {
		return nomMinusculeOFS;
	}

	public void setNomMinusculeOFS(String nomMinusculeOFS) {
		this.nomMinusculeOFS = nomMinusculeOFS;
	}

	public String getCantonID() {
		return cantonID;
	}

	public RegDate getDateDebutValidite() {
		return dateDebutValidite;
	}

	public RegDate getDateDebValidite() {
		return dateDebValidite;
	}

	public RegDate getDateFinValidite() {
		return dateFinValidite;
	}

	public String getNoACI() {
		return noACI;
	}

	public String getNoCantonal() {
		return noCantonal;
	}

	public String getNomAbrege() {
		return nomAbrege;
	}

	public int getNoOFSEtendu() {
		return getNoOFS();
	}

	public int getNumTechMere() {
		throw new RuntimeException("Not implemented!");
	}

	public String getSigleCanton() {
		return sigleCanton;
	}

	public boolean isVaudoise() {
		return VAUD.equals(sigleCanton);
	}

	public boolean isFraction() {
		return false;
	}

	public boolean isPrincipale() {
		return false;
	}

	public boolean isValide() {
		return valide;
	}

	public OfficeImpot getOfficeImpot() {
		return officeImpot;
	}

}
