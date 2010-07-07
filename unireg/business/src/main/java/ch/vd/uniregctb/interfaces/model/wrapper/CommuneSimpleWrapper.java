package ch.vd.uniregctb.interfaces.model.wrapper;

import ch.vd.infrastructure.model.EnumCanton;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;

public class CommuneSimpleWrapper extends EntiteOFSWrapper implements CommuneSimple {

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private String noACI;
	private String noCantonal;
	private int noOFSEtendu;
	private String nomAbrege;
	private int numTechMere;
	private boolean vaudoise;
	private boolean fraction;
	private boolean principale;
	private boolean valide;
	private String sigleCanton;

	public static CommuneSimpleWrapper get(ch.vd.infrastructure.model.CommuneSimple target) {
		if (target == null) {
			return null;
		}
		return new CommuneSimpleWrapper(target);
	}

	protected CommuneSimpleWrapper(ch.vd.infrastructure.model.CommuneSimple target) {
		super(target);
		this.dateDebut = RegDate.get(target.getDateDebutValidite());
		this.dateFin = RegDate.get(target.getDateFinValidite());
		this.noACI = target.getNoACI();
		this.noCantonal = target.getNoCantonal();
		this.noOFSEtendu = (target.isFraction() ? target.getNoTechnique() : target.getNoOFS());
		this.nomAbrege = target.getNomAbrege();
		this.sigleCanton = target.getSigleCanton();
		this.numTechMere = target.getNumTechMere();
		this.vaudoise = EnumCanton.SIGLE_VAUD.getName().equals(getSigleCanton());
		this.fraction = target.isFraction();
		this.principale = target.isPrincipale();
		this.valide = target.isValide();
	}

	protected CommuneSimpleWrapper(ch.vd.infrastructure.model.Commune target) {
		super(target);
		this.dateDebut = RegDate.get(target.getDateDebutValidite());
		this.dateFin = RegDate.get(target.getDateFinValidite());
		this.noACI = target.getNoACI();
		this.noCantonal = target.getNoCantonal();
		this.noOFSEtendu = (target.isFraction() ? target.getNoTechnique() : target.getNoOFS());
		this.nomAbrege = target.getNomAbrege();
		this.sigleCanton = target.getSigleCanton();
		this.numTechMere = target.getNumTechMere();
		this.vaudoise = EnumCanton.SIGLE_VAUD.getName().equals(getSigleCanton());
		this.fraction = target.isFraction();
		this.principale = target.isPrincipale();
		this.valide = target.isValide();
	}

	public RegDate getDateDebutValidite() {
		return dateDebut;
	}

	public RegDate getDateFinValidite() {
		return dateFin;
	}

	public String getNoACI() {
		return noACI;
	}

	public String getNoCantonal() {
		return noCantonal;
	}

	public int getNoOFSEtendu() {
		return noOFSEtendu;
	}

	@Override
	public int getNoOFS() {
		return getNoOFSEtendu();
	}

	public String getNomAbrege() {
		return nomAbrege;
	}

	public int getNumTechMere() {
		return numTechMere;
	}

	public String getSigleCanton() {
		return sigleCanton;
	}

	public boolean isVaudoise() {
		return vaudoise;
	}

	public boolean isFraction() {
		return fraction;
	}

	public boolean isPrincipale() {
		return principale;
	}

	public boolean isValide() {
		return valide;
	}
}
