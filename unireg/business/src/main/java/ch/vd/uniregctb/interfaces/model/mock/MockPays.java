package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;


public class MockPays extends MockEntityOFS implements Pays {

	public static final MockPays Suisse = new MockPays(ServiceInfrastructureService.noOfsSuisse, "Suisse", "CH");
	public static final MockPays Albanie = new MockPays(8201, "Albanie", "AL");
	public static final MockPays Danemark = new MockPays(8206, "Danemark", "DK");
	public static final MockPays Allemagne = new MockPays(8207, "Allemagne", "DE");
	public static final MockPays France = new MockPays(8212, "France", "FR");
	public static final MockPays Gibraltar = new MockPays(8213, "Gibraltar", "GI", true, false);
	public static final MockPays RoyaumeUni = new MockPays(8215, "Royaume-Uni", "GB");
	public static final MockPays Liechtenstein = new MockPays(8222, "Liechtenstein", "LI");
	public static final MockPays Espagne = new MockPays(8236, "Espagne", "ES");
	public static final MockPays Turquie = new MockPays(8239, "Turquie", "TR");
	public static final MockPays Russie = new MockPays(8264, "Russie", "RU");
	public static final MockPays Colombie = new MockPays(8424, "Colombie", "CO");
	public static final MockPays EtatsUnis = new MockPays(8439, "Etats-Unis", "US");
	public static final MockPays CoreeSud = new MockPays(8539, "Corée (Sud)", "KR");

	public static final MockPays PaysInconnu = new MockPays(ServiceInfrastructureService.noPaysInconnu, "PaysInconnu", "INC");
	public static final MockPays RDA = new MockPays(8208, "République démocratique allemande", "", false);

	private final boolean valide;
	private final boolean etatSouverain;

	public MockPays(int numeroOFS, String nomMiniscule) {
		super(numeroOFS, null, nomMiniscule);
		this.valide = true;
		this.etatSouverain = true;
		DefaultMockServiceInfrastructureService.addPays(this);
	}

	public MockPays(int numeroOFS, String nomMiniscule, String sigleOFS) {
		super(numeroOFS, sigleOFS, nomMiniscule);
		this.valide = true;
		this.etatSouverain = true;
		DefaultMockServiceInfrastructureService.addPays(this);
	}

	public MockPays(int numeroOFS, String nomMiniscule, String sigleOFS, boolean valide) {
		super(numeroOFS, sigleOFS, nomMiniscule);
		this.valide = valide;
		this.etatSouverain = true;
		DefaultMockServiceInfrastructureService.addPays(this);
	}

	public MockPays(int numeroOFS, String nomMiniscule, String sigleOFS, boolean valide, boolean etatSouverain) {
		super(numeroOFS, sigleOFS, nomMiniscule);
		this.valide = valide;
		this.etatSouverain = etatSouverain;
		DefaultMockServiceInfrastructureService.addPays(this);
	}

	@Override
	public boolean isSuisse() {
		return getNoOFS() == ServiceInfrastructureService.noOfsSuisse;
	}

	@Override
	public boolean isValide() {
		return valide;
	}

	@Override
	public boolean isEtatSouverain() {
		return etatSouverain;
	}
}
