package ch.vd.uniregctb.tiers.view;

import java.io.Serializable;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEtatEntreprise;

/**
 * Critères de recherche pour les tiers.
 */
public class TiersCriteriaView implements Serializable {

	private static final long serialVersionUID = 4657272958620368484L;

	// Valeurs utilisées en sortie seulement
	private Long numeroSourcier;
	private Long numeroDebiteur;
	private Long numeroNonHabitant;
	private Long numeroPremierePersonne;
	private String modeImpositionAsString;

	// Les critères utilisés pour la recherche
	private String forAll;
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
		if (modeImpositionAsString == null || "".equals(modeImpositionAsString)) {
			setModeImposition(null);
		}
		else {
			setModeImposition(ModeImposition.valueOf(modeImpositionAsString));
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

	public RegDate getDateNaissanceInscriptionRC() {
		return criteria.getDateNaissanceInscriptionRC() != null ? criteria.getDateNaissanceInscriptionRC().value : null;
	}

	public void setDateNaissanceInscriptionRC(RegDate dateNaissance) {
		criteria.setDateNaissanceInscriptionRC(dateNaissance);
	}

	public String getNumeroAVS() {
		return criteria.getNumeroAVS();
	}

	public void setNumeroAVS(String numeroAVS) {
		if (StringUtils.isNotBlank(numeroAVS)) {
			criteria.setNumeroAVS(FormatNumeroHelper.removeSpaceAndDash(numeroAVS));
		}
		else {
			criteria.setNumeroAVS(numeroAVS);
		}
	}

	public String getNumeroEtranger() {
		return criteria.getNumeroEtranger();
	}

	public void setNumeroEtranger(String numeroEtranger) {
		criteria.setNumeroEtranger(numeroEtranger);
	}

	public FormeJuridiqueEntreprise getFormeJuridique() {
		return criteria.getFormeJuridique();
	}

	public void setFormeJuridique(FormeJuridiqueEntreprise formeJuridique) {
		criteria.setFormeJuridique(formeJuridique);
	}

	public CategorieEntreprise getCategorieEntreprise() {
		return criteria.getCategorieEntreprise();
	}

	public void setCategorieEntreprise(CategorieEntreprise categorieEntreprise) {
		criteria.setCategorieEntreprise(categorieEntreprise);
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

	public Set<MotifFor> getMotifsFermetureDernierForPrincipal() {
		return criteria.getMotifsFermetureDernierForPrincipal();
	}

	public void setMotifFermetureDernierForPrincipal(MotifFor motifFermetureDernierForPrincipal) {
		criteria.setMotifFermetureDernierForPrincipal(motifFermetureDernierForPrincipal);
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

	public void setTypeTiersImperatif(TiersCriteria.TypeTiers typeTiers) {
		criteria.setTypeTiersImperatif(typeTiers);
	}

	public void setTypesTiersImperatifs(Set<TiersCriteria.TypeTiers> typesTiers) {
		criteria.setTypesTiersImperatifs(typesTiers);
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
		return criteria.getNpaCourrier();
	}

	public void setNpa(String npa) {
		criteria.setNpaCourrier(npa);
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

	public String getNumeroIDE() {
		return criteria.getNumeroIDE();
	}

	public void setNumeroIDE(String numeroIDE) {
		criteria.setNumeroIDE(numeroIDE);
	}

	public Set<TypeEtatEntreprise> getEtatsEntrepriseInterdits() {
		return criteria.getEtatsEntrepriseInterdits();
	}

	public void setEtatsEntrepriseInterdits(Set<TypeEtatEntreprise> etatsEntrepriseInterdits) {
		criteria.setEtatsEntrepriseInterdits(etatsEntrepriseInterdits);
	}

	public Set<TypeEtatEntreprise> getEtatsEntrepriseCourantsInterdits() {
		return criteria.getEtatsEntrepriseCourantsInterdits();
	}

	public void setEtatsEntrepriseCourantsInterdits(Set<TypeEtatEntreprise> etatsEntrepriseCourantsInterdits) {
		criteria.setEtatsEntrepriseCourantsInterdits(etatsEntrepriseCourantsInterdits);
	}

	public TiersCriteria.TypeInscriptionRC getEtatInscriptionRC() {
		return criteria.getEtatInscriptionRC();
	}

	public void setEtatInscriptionRC(TiersCriteria.TypeInscriptionRC etatInscriptionRC) {
		criteria.setEtatInscriptionRC(etatInscriptionRC);
	}

	public TypeEtatEntreprise getEtatEntrepriseCourant() {
		return criteria.getEtatEntrepriseCourant();
	}

	public void setEtatEntrepriseCourant(TypeEtatEntreprise type) {
		criteria.setEtatEntrepriseCourant(type);
	}

	public Boolean getCorporationMergeResult() {
		return criteria.getCorporationMergeResult();
	}

	public void setCorporationMergeResult(Boolean corporationMergeResult) {
		criteria.setCorporationMergeResult(corporationMergeResult);
	}

	public Boolean getCorporationSplit() {
		return criteria.getCorporationSplit();
	}

	public void setCorporationSplit(Boolean corporationSplit) {
		criteria.setCorporationSplit(corporationSplit);
	}

	public Boolean hasCorporationTransferedPatrimony() {
		return criteria.hasCorporationTransferedPatrimony();
	}

	public void setCorporationTransferedPatrimony(Boolean hasCorporationTransferedPatrimony) {
		criteria.setCorporationTransferedPatrimony(hasCorporationTransferedPatrimony);
	}

	public Boolean getConnuAuCivil() {
		return criteria.getConnuAuCivil();
	}

	public void setConnuAuCivil(Boolean connuAuCivil) {
		criteria.setConnuAuCivil(connuAuCivil);
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
