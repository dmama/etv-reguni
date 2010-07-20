package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceInfrastructureService;


public class MockPays extends MockEntityOFS implements Pays {

	private static final long serialVersionUID = 8913326410895875642L;

	public static final MockPays Suisse = new MockPays(ServiceInfrastructureService.noOfsSuisse, "Suisse", "CH");
	public static final MockPays Liechtenstein = new MockPays(8222, "Liechtenstein", "LI");
	public static final MockPays France = new MockPays(8212, "France", "FR");
	public static final MockPays Danemark = new MockPays(8206, "Danemark", "DK");
	public static final MockPays Allemagne = new MockPays(8207, "Allemagne", "DE");
	public static final MockPays Espagne = new MockPays(8236, "Espagne", "ES");
	public static final MockPays Albanie = new MockPays(8201, "Albanie", "AL");
	public static final MockPays Turquie = new MockPays(8239, "Turquie", "TR");
	public static final MockPays CoreeSud = new MockPays(8539, "Corée (Sud)", "KR");
	public static final MockPays Colombie = new MockPays(8424, "Colombie", "CO");
	public static final MockPays EtatsUnis = new MockPays(8439, "Etats-Unis", "US");
	public static final MockPays PaysInconnu = new MockPays(8999, "PaysInconnu", "INC");

	private String nomMinusculeOFS;

	public MockPays() {
		DefaultMockServiceInfrastructureService.addPays(this);
	}

	public MockPays(int numeroOFS, String nomMiniscule) {
		super(numeroOFS, null, nomMiniscule);
		DefaultMockServiceInfrastructureService.addPays(this);
	}

	public MockPays(int numeroOFS, String nomMiniscule, String sigleOFS) {
		super(numeroOFS, sigleOFS, nomMiniscule);
		DefaultMockServiceInfrastructureService.addPays(this);
	}

	public String getNomMinusculeOFS() {
		return nomMinusculeOFS;
	}

	public void setNomMinusculeOFS(String nomMinusculeOFS) {
		this.nomMinusculeOFS = nomMinusculeOFS;
	}

	public boolean isSuisse() {
		return getNoOFS() == ServiceInfrastructureService.noOfsSuisse;
	}
}
