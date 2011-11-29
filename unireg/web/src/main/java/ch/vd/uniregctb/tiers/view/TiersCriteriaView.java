package ch.vd.uniregctb.tiers.view;

import java.util.Set;

import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.ModeImposition;

/**
 * Critères de recherche pour les tiers.
 */
public class TiersCriteriaView {

	// Valeurs utilisées en sortie seulement
	private String urlTaoPP;
	private String urlTaoBA;
	private String urlSipf;
	private String urlCat;
	private String urlRegView;
	private Long numeroSourcier;
	private Long numeroDebiteur;
	private Long numeroNonHabitant;
	private Long numeroPremierePersonne;
	private String forAll;
	private String modeImpositionAsString;

	// Les critères utilisés pour la recherche
	private String numeroFormatte;
	private final TiersCriteria criteria = new TiersCriteria();

	public String getForAll() {
		return forAll;
	}

	public void setForAll(String forAll) {
		this.forAll = forAll;
	}

	/**
	 * @return the numero formatte
	 */
	public String getNumeroFormatte() {
		return this.numeroFormatte;
	}

	public void setNumeroFormatte(String numeroFormatte) {
		if (StringUtils.isNotEmpty(numeroFormatte)) {
			try {
				setNumero(Long.valueOf((FormatNumeroHelper.removeSpaceAndDash(numeroFormatte))));
			} catch(NumberFormatException nfe) {
				//Ne rien faire
			}
		} else {
			setNumero(null);
		}
		this.numeroFormatte = numeroFormatte;
	}

	public String getUrlTaoPP() {
		return urlTaoPP;
	}

	public void setUrlTaoPP(String urlTaoPP) {
		this.urlTaoPP = urlTaoPP;
	}

	public String getUrlTaoBA() {
		return urlTaoBA;
	}

	public void setUrlTaoBA(String urlTaoBA) {
		this.urlTaoBA = urlTaoBA;
	}

	public String getUrlSipf() {
		return urlSipf;
	}

	public void setUrlSipf(String urlSipf) {
		this.urlSipf = urlSipf;
	}

	public String getUrlCat() {
		return urlCat;
	}

	public void setUrlCat(String urlCAT) {
		this.urlCat = urlCAT;
	}

	public String getUrlRegView() {
		return urlRegView;
	}

	public void setUrlRegView(String urlRegView) {
		this.urlRegView = urlRegView;
	}

	public Long getNumeroSourcier() {
		return numeroSourcier;
	}

	public void setNumeroSourcier(Long numeroSourcier) {
		this.numeroSourcier = numeroSourcier;
	}

	public Long getNumeroDebiteur() {
		return numeroDebiteur;
	}

	public void setNumeroDebiteur(Long numeroDebiteur) {
		this.numeroDebiteur = numeroDebiteur;
	}

	public Long getNumeroNonHabitant() {
		return numeroNonHabitant;
	}

	public void setNumeroNonHabitant(Long numeroNonHabitant) {
		this.numeroNonHabitant = numeroNonHabitant;
	}

	public Long getNumeroPremierePersonne() {
		return numeroPremierePersonne;
	}

	public void setNumeroPremierePersonne(Long numeroPremierePersonne) {
		this.numeroPremierePersonne = numeroPremierePersonne;
	}

	public String getModeImpositionAsString() {
		return modeImpositionAsString;
	}

	public void setModeImpositionAsString(String modeImpositionAsString) {
		if (!"TOUS".equals(modeImpositionAsString)) {
			setModeImposition(ModeImposition.valueOf(modeImpositionAsString));
		}
		else {
			setModeImposition(null);
		}
		this.modeImpositionAsString = modeImpositionAsString;
	}

	public Long getNumero() {
		return criteria.getNumero();
	}

	public void setNumero(Long numero) {
		criteria.setNumero(numero);
	}

	public TiersCriteria.TypeRecherche getTypeRechercheDuNom() {
		return criteria.getTypeRechercheDuNom();
	}

	public void setTypeRechercheDuNom(TiersCriteria.TypeRecherche typeRechercheDuNom) {
		criteria.setTypeRechercheDuNom(typeRechercheDuNom);
	}

	public String getNomRaison() {
		return criteria.getNomRaison();
	}

	public void setNomRaison(String nomCourrier) {
		criteria.setNomRaison(nomCourrier);
	}

	public String getNatureJuridique() {
		return criteria.getNatureJuridique();
	}

	public void setNatureJuridique(String natureJuridique) {
		criteria.setNatureJuridique(natureJuridique);
	}

	public RegDate getDateNaissance() {
		return criteria.getDateNaissance();
	}

	public void setDateNaissance(RegDate dateNaissance) {
		criteria.setDateNaissance(dateNaissance);
	}

	public String getNumeroAVS() {
		return criteria.getNumeroAVS();
	}

	public void setNumeroAVS(String numeroAVS) {
		criteria.setNumeroAVS(numeroAVS);
	}

	public String getNumeroEtranger() {
		return criteria.getNumeroEtranger();
	}

	public void setNumeroEtranger(String numeroEtranger) {
		criteria.setNumeroEtranger(numeroEtranger);
	}

	public String getFormeJuridique() {
		return criteria.getFormeJuridique();
	}

	public void setFormeJuridique(String formeJuridique) {
		criteria.setFormeJuridique(formeJuridique);
	}

	public String getLocaliteOuPays() {
		return criteria.getLocaliteOuPays();
	}

	public void setLocaliteOuPays(String localiteOuPays) {
		criteria.setLocaliteOuPays(localiteOuPays);
	}

	public boolean isForPrincipalActif() {
		return criteria.isForPrincipalActif();
	}

	public void setForPrincipalActif(boolean forPrincipalActif) {
		criteria.setForPrincipalActif(forPrincipalActif);
	}

	public TiersCriteria.TypeRechercheLocalitePays getTypeRechercheDuPaysLocalite() {
		return criteria.getTypeRechercheDuPaysLocalite();
	}

	public void setTypeRechercheDuPaysLocalite(TiersCriteria.TypeRechercheLocalitePays typeRechercheDuPaysLocalite) {
		criteria.setTypeRechercheDuPaysLocalite(typeRechercheDuPaysLocalite);
	}

	public TiersCriteria.TypeVisualisation getTypeVisualisation() {
		return criteria.getTypeVisualisation();
	}

	public void setTypeVisualisation(TiersCriteria.TypeVisualisation typeVisualisation) {
		criteria.setTypeVisualisation(typeVisualisation);
	}

	public String getNoOfsFor() {
		return criteria.getNoOfsFor();
	}

	public void setNoOfsFor(String noOfsFor) {
		criteria.setNoOfsFor(noOfsFor);
	}

	public TiersCriteria.TypeTiers getTypeTiers() {
		final Set<TiersCriteria.TypeTiers> set = criteria.getTypesTiers();
		if (set == null || set.isEmpty()) {
			return null;
		}
		return set.iterator().next();
	}

	public void setTypeTiers(TiersCriteria.TypeTiers typeTiers) {
		criteria.setTypeTiers(typeTiers);
	}

	public boolean isInclureI107() {
		return criteria.isInclureI107();
	}

	public void setInclureI107(boolean inclureI107) {
		criteria.setInclureI107(inclureI107);
	}

	public boolean isInclureTiersAnnules() {
		return criteria.isInclureTiersAnnules();
	}

	public void setInclureTiersAnnules(boolean inclureTiersAnnules) {
		criteria.setInclureTiersAnnules(inclureTiersAnnules);
	}

	public boolean isTiersAnnulesSeulement() {
		return criteria.isTiersAnnulesSeulement();
	}

	public void setTiersAnnulesSeulement(boolean tiersAnnulesSeulement) {
		criteria.setTiersAnnulesSeulement(tiersAnnulesSeulement);
	}

	public String getNpa() {
		return criteria.getNpa();
	}

	public void setNpa(String npa) {
		criteria.setNpa(npa);
	}

	public ModeImposition getModeImposition() {
		return criteria.getModeImposition();
	}

	public void setModeImposition(ModeImposition modeImposition) {
		criteria.setModeImposition(modeImposition);
	}

	public String getNoSymic() {
		return criteria.getNoSymic();
	}

	public void setNoSymic(String noSymic) {
		criteria.setNoSymic(noSymic);
	}

	public CategorieImpotSource getCategorieDebiteurIs() {
		return criteria.getCategorieDebiteurIs();
	}

	public void setCategorieDebiteurIs(CategorieImpotSource categorieDebiteurIs) {
		criteria.setCategorieDebiteurIs(categorieDebiteurIs);
	}

	public Boolean isTiersActif() {
		return criteria.isTiersActif();
	}

	public void setTiersActif(Boolean tiersActif) {
		criteria.setTiersActif(tiersActif);
	}

	public boolean isEmpty() {
		return criteria.isEmpty() && StringUtils.isBlank(this.numeroFormatte);
	}

	public TiersCriteria asCore() {
		return criteria;
	}

	@Override
	public String toString() {
		return criteria.toString();
	}
}
