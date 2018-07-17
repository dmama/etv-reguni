package ch.vd.unireg.evenement.retourdi.pm;

public class RetourDI {

	private final long noCtb;
	private final int pf;
	private final int noSequence;
	private final InformationsEntreprise entreprise;
	private final InformationsMandataire mandataire;
	private final EnumCanalAcquisition canalAquisitionDI;

	public RetourDI(long noCtb, int pf, int noSequence, InformationsEntreprise entreprise, InformationsMandataire mandataire, EnumCanalAcquisition enumCanalAcquisitionDI) {
		this.noCtb = noCtb;
		this.pf = pf;
		this.noSequence = noSequence;
		this.entreprise = entreprise;
		this.mandataire = mandataire;
		this.canalAquisitionDI = enumCanalAcquisitionDI;
	}

	public long getNoCtb() {
		return noCtb;
	}

	public int getPf() {
		return pf;
	}

	public int getNoSequence() {
		return noSequence;
	}

	public InformationsEntreprise getEntreprise() {
		return entreprise;
	}

	public InformationsMandataire getMandataire() {
		return mandataire;
	}

	public EnumCanalAcquisition getCanalAquisitionDI() {
		return canalAquisitionDI;
	}

	public enum EnumCanalAcquisition {
		ELECTRONIQUE,
		PAPIER
	}
}
