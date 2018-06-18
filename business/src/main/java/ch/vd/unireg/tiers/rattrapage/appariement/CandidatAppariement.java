package ch.vd.unireg.tiers.rattrapage.appariement;

import ch.vd.unireg.interfaces.organisation.data.EtablissementCivil;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.LocalizedDateRange;
import ch.vd.unireg.type.TypeAutoriteFiscale;

public class CandidatAppariement {

	/**
	 * Critère décisif qui a emporté la décision d'appariement
	 */
	public enum CritereDecisif {

		/**
		 * Identité des numéros IDE associés (+ sur même localisation)
		 */
		IDE,

		/**
		 * Même localisation (+ flag activité)
		 */
		LOCALISATION,
	}

	private final Etablissement etablissement;
	private final EtablissementCivil etablissementCivil;
	private final CritereDecisif critere;
	private final TypeAutoriteFiscale typeAutoriteFiscaleSiege;
	private final Integer ofsSiege;

	public CandidatAppariement(Etablissement etablissement, EtablissementCivil etablissementCivil, CritereDecisif critere, LocalizedDateRange siege) {
		if (etablissement == null || etablissementCivil == null || critere == null) {
			throw new NullPointerException("Ni l'établissement, ni l'établissement civil, ni le critère décisif ne peuvent être nuls ici!");
		}
		this.etablissement = etablissement;
		this.etablissementCivil = etablissementCivil;
		this.critere = critere;
		this.typeAutoriteFiscaleSiege = siege != null ? siege.getTypeAutoriteFiscale() : null;
		this.ofsSiege = siege != null ? siege.getNumeroOfsAutoriteFiscale() : null;
	}

	public Etablissement getEtablissement() {
		return etablissement;
	}

	public EtablissementCivil getEtablissementCivil() {
		return etablissementCivil;
	}

	public CritereDecisif getCritere() {
		return critere;
	}

	public TypeAutoriteFiscale getTypeAutoriteFiscaleSiege() {
		return typeAutoriteFiscaleSiege;
	}

	public Integer getOfsSiege() {
		return ofsSiege;
	}
}
