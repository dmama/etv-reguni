package ch.vd.uniregctb.migration.pm.adresse;

import java.io.Serializable;

import ch.vd.fidor.xml.post.v1.Street;

public class StreetData implements Serializable {

	private static final long serialVersionUID = 4812300181644183713L;

	private final String nomRue;
	private final String noPolice;
	private final Integer estrid;
	private final Integer noOrdreP;
	private final Integer npa;
	private final Integer npaComplementaire;
	private final String localitePostale;

	protected StreetData(Street street, String noPolice, int swissZipCodeId) {
		this.nomRue = null;
		this.noPolice = noPolice;
		this.estrid = street.getEstrid();
		this.noOrdreP = swissZipCodeId;
		this.npa = null;
		this.npaComplementaire = null;
		this.localitePostale = null;
	}

	protected StreetData(String nomRue, String noPolice, Integer noOrdreP, Integer npa, Integer npaComplementaire, String localitePostale) {
		this.nomRue = nomRue;
		this.noPolice = noPolice;
		this.estrid = null;
		this.noOrdreP = noOrdreP;
		this.npa = npa;
		this.npaComplementaire = npaComplementaire;
		this.localitePostale = localitePostale;
	}

	public String getNomRue() {
		return nomRue;
	}

	public String getNoPolice() {
		return noPolice;
	}

	public Integer getEstrid() {
		return estrid;
	}

	public Integer getNoOrdreP() {
		return noOrdreP;
	}

	public Integer getNpa() {
		return npa;
	}

	public Integer getNpaComplementaire() {
		return npaComplementaire;
	}

	public String getLocalitePostale() {
		return localitePostale;
	}

	@Override
	public String toString() {
		return String.format("%s{nomRue='%s', noPolice='%s', estrid=%d, noOrdreP=%d, npa=%d, npaComplementaire=%d, localitePostale='%s'}",
		                     getClass().getSimpleName(), nomRue, noPolice, estrid, noOrdreP, npa, npaComplementaire, localitePostale);
	}

	public static class AvecEstrid extends StreetData {
		public AvecEstrid(Street street, String noPolice, int swissZipCodeId) {
			super(street, noPolice, swissZipCodeId);
		}
	}

	public static class AucuneNomenclatureTrouvee extends StreetData {
		public AucuneNomenclatureTrouvee(String nomRue, String noPolice, String lieu) {
			super(nomRue, noPolice, null, null, null, lieu);
		}
	}

	public static class LocaliteAbsenteRefinf extends StreetData {
		public LocaliteAbsenteRefinf(String nomRue, String noPolice, int noOrdreP, Integer npa, Integer npaComplementaire, String localitePostale) {
			super(nomRue, noPolice, noOrdreP, npa, npaComplementaire, localitePostale);
		}
	}

	public static class RueInconnue extends StreetData {
		public RueInconnue(String nomRue, String noPolice, int noOrdreP) {
			super(nomRue, noPolice, noOrdreP, null, null, null);
		}
	}
}
