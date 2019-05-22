package ch.vd.unireg.interfaces.infra.mock;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.InfrastructureConnector;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.data.TypeAffranchissement;


public class MockPays extends MockEntityOFS implements Pays {

	//
	// Les états souverains
	//

	public static final MockPays Suisse = new MockPays(InfrastructureConnector.noOfsSuisse, "Suisse", "CH", "CH", "CHE", TypeAffranchissement.SUISSE);
	public static final MockPays Albanie = new MockPays(8201, "Albanie", "AL", "AL", "ALB", TypeAffranchissement.EUROPE);
	public static final MockPays Danemark = new MockPays(8206, "Danemark", "DK", "DK", "DNK", TypeAffranchissement.EUROPE);
	public static final MockPays Allemagne = new MockPays(8207, "Allemagne", "DE", "DE", "DEU", TypeAffranchissement.EUROPE);
	public static final MockPays France = new MockPays(8212, "France", "FR", "FR", "FRA", TypeAffranchissement.EUROPE);
	public static final MockPays RoyaumeUni = new MockPays(8215, "Royaume-Uni", "GB", "GB", "GBR", TypeAffranchissement.EUROPE);
	public static final MockPays Liechtenstein = new MockPays(8222, "Liechtenstein", "LI", "LI", "LIE", TypeAffranchissement.SUISSE);
	public static final MockPays Espagne = new MockPays(8236, "Espagne", "ES", "ES", "ESP", TypeAffranchissement.EUROPE);
	public static final MockPays Turquie = new MockPays(8239, "Turquie", "TR", "TR", "TUR", TypeAffranchissement.EUROPE);
	public static final MockPays Russie = new MockPays(8264, "Russie", "RU", "RU", "RUS", TypeAffranchissement.EUROPE);
	public static final MockPays Colombie = new MockPays(8424, "Colombie", "CO", "CO", "COL", TypeAffranchissement.MONDE);
	public static final MockPays EtatsUnis = new MockPays(8439, "Etats-Unis", "US", "US", "USA", TypeAffranchissement.MONDE);
	public static final MockPays Japon = new MockPays(8515, "Japon", "JP", "JP", "JPN", TypeAffranchissement.MONDE);
	public static final MockPays CoreeSud = new MockPays(8539, "Corée (Sud)", "KR", "KR", "KOR", TypeAffranchissement.MONDE);
	public static final MockPays Kosovo = new MockPays(8256, "Kosovo", null, null, null, TypeAffranchissement.EUROPE);
	public static final MockPays Maroc = new MockPays(8331, "Maroc", "MA", "MA", "MAR", TypeAffranchissement.MONDE);

	//
	// les territoires
	//

	public static final MockPays Gibraltar = new MockPays(8213, "Gibraltar", "GI", true, "GI", "GIB", MockPays.RoyaumeUni.getNoOFS(), TypeAffranchissement.EUROPE);

	//
	// les cas bizarres (inactifs, pays inconnu...)
	// -> dans Ref-INF, leur code de zone tariffaire postale est toujours "MONDE"... donc on fait pareil ici...
	//

	public static final MockPays PaysInconnu = new MockPays(InfrastructureConnector.noPaysInconnu, "PaysInconnu", "INC", null, null, TypeAffranchissement.MONDE);
	public static final MockPays Apatridie = new MockPays(InfrastructureConnector.noPaysApatride, "Apatridie", "---", true, null, null, InfrastructureConnector.noPaysApatride, TypeAffranchissement.MONDE);     // <-- à n'utiliser que pour les nationalités!
	public static final MockPays RDA = new MockPays(8208, "République démocratique allemande", "", false, null, null, TypeAffranchissement.MONDE);

	private static final DateRange ETERNITY = new DateRangeHelper.Range(null, null);

	private final boolean valide;
	private final int ofsEtatSouverain;
	private final String codeIso2;
	private final String codeIso3;
	private final DateRange validityRange;
	private final TypeAffranchissement typeAffranchissement;

	public MockPays(int numeroOFS, String nom, String codeIso2, String codeIso3, TypeAffranchissement typeAffranchissement) {
		super(numeroOFS, null, nom, nom);
		this.codeIso2 = codeIso2;
		this.codeIso3 = codeIso3;
		this.typeAffranchissement = typeAffranchissement;
		this.valide = true;
		this.ofsEtatSouverain = numeroOFS;
		this.validityRange = ETERNITY;
		DefaultMockInfrastructureConnector.addPays(this);
	}

	public MockPays(int numeroOFS, String nom, String sigleOFS, String codeIso2, String codeIso3, TypeAffranchissement typeAffranchissement) {
		super(numeroOFS, sigleOFS, nom, nom);
		this.codeIso2 = codeIso2;
		this.codeIso3 = codeIso3;
		this.typeAffranchissement = typeAffranchissement;
		this.valide = true;
		this.ofsEtatSouverain = numeroOFS;
		this.validityRange = ETERNITY;
		DefaultMockInfrastructureConnector.addPays(this);
	}

	public MockPays(int numeroOFS, String nom, String sigleOFS, boolean valide, String codeIso2, String codeIso3, TypeAffranchissement typeAffranchissement) {
		super(numeroOFS, sigleOFS, nom, nom);
		this.valide = valide;
		this.codeIso2 = codeIso2;
		this.codeIso3 = codeIso3;
		this.ofsEtatSouverain = numeroOFS;
		this.typeAffranchissement = typeAffranchissement;
		this.validityRange = ETERNITY;
		DefaultMockInfrastructureConnector.addPays(this);
	}

	public MockPays(int numeroOFS, String nom, String sigleOFS, boolean valide, String codeIso2, String codeIso3, TypeAffranchissement typeAffranchissement, RegDate dateDebut, RegDate dateFin) {
		super(numeroOFS, sigleOFS, nom, nom);
		this.valide = valide;
		this.codeIso2 = codeIso2;
		this.codeIso3 = codeIso3;
		this.ofsEtatSouverain = numeroOFS;
		this.typeAffranchissement = typeAffranchissement;
		this.validityRange = new DateRangeHelper.Range(dateDebut, dateFin);
		DefaultMockInfrastructureConnector.addPays(this);
	}

	/**
	 * Constructeur pour les territoires
	 */
	public MockPays(int numeroOFS, String nom, String sigleOFS, boolean valide, String codeIso2, String codeIso3, int ofsEtatSouverainParent, TypeAffranchissement typeAffranchissement) {
		super(numeroOFS, sigleOFS, nom, nom);
		this.valide = valide;
		this.codeIso2 = codeIso2;
		this.codeIso3 = codeIso3;
		this.ofsEtatSouverain = ofsEtatSouverainParent;
		this.typeAffranchissement = typeAffranchissement;
		this.validityRange = ETERNITY;
		DefaultMockInfrastructureConnector.addPays(this);
	}

	@Override
	public boolean isSuisse() {
		return getNoOFS() == InfrastructureConnector.noOfsSuisse;
	}

	@Override
	public boolean isValide() {
		return valide;
	}

	@Override
	public String getCodeIso2() {
		return codeIso2;
	}

	@Override
	public String getCodeIso3() {
		return codeIso3;
	}

	@Override
	public boolean isEtatSouverain() {
		return getNoOFS() == getNoOfsEtatSouverain();
	}

	@Override
	public int getNoOfsEtatSouverain() {
		return ofsEtatSouverain;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return valide && validityRange.isValidAt(date);
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
	public TypeAffranchissement getTypeAffranchissement() {
		return typeAffranchissement;
	}
}
