package ch.vd.uniregctb.tiers.rattrapage.appariement;

import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.LocalizedDateRange;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

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

		/**
		 * Même raison sociale (+ flag activité + même localisation)
		 */
		RAISON_SOCIALE
	}

	private final Etablissement etablissement;
	private final SiteOrganisation site;
	private final CritereDecisif critere;
	private final TypeAutoriteFiscale typeAutoriteFiscaleSiege;
	private final Integer ofsSiege;

	public CandidatAppariement(Etablissement etablissement, SiteOrganisation site, CritereDecisif critere, LocalizedDateRange siege) {
		if (etablissement == null || site == null || critere == null) {
			throw new NullPointerException("Ni l'établissement, ni le site, ni le critère décisif ne peuvent être nuls ici!");
		}
		this.etablissement = etablissement;
		this.site = site;
		this.critere = critere;
		this.typeAutoriteFiscaleSiege = siege != null ? siege.getTypeAutoriteFiscale() : null;
		this.ofsSiege = siege != null ? siege.getNumeroOfsAutoriteFiscale() : null;
	}

	public Etablissement getEtablissement() {
		return etablissement;
	}

	public SiteOrganisation getSite() {
		return site;
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
