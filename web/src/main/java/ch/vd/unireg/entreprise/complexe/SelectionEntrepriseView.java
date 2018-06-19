package ch.vd.unireg.entreprise.complexe;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.tiers.TiersIndexedDataView;
import ch.vd.unireg.type.TypeEtatEntreprise;

/**
 * Classe des données utilisées lors de la présentation d'une liste d'entreprises
 * issues d'une recherche, dont certaines seulement sont sélectionnables
 */
public class SelectionEntrepriseView implements Annulable {

	private final TiersIndexedDataView indexedData;
	private final boolean selectionnable;
	private final String explicationNonSelectionnable;

	public SelectionEntrepriseView(TiersIndexedDataView data, String explicationNonSelectionnable) {
		this.indexedData = data;
		this.selectionnable = StringUtils.isBlank(explicationNonSelectionnable);
		this.explicationNonSelectionnable = explicationNonSelectionnable;
	}

	@Override
	public boolean isAnnule() {
		return indexedData.isAnnule();
	}

	public boolean isSelectionnable() {
		return selectionnable;
	}

	public String getExplicationNonSelectionnable() {
		return explicationNonSelectionnable;
	}

	public Long getNumero() {
		return indexedData.getNumero();
	}

	public String getForPrincipal() {
		return indexedData.getForPrincipal();
	}

	public RegDate getDateNaissanceInscriptionRC() {
		return indexedData.getRegDateNaissanceInscriptionRC();
	}

	public String getNumeroIDE() {
		return indexedData.getNumeroIDE();
	}

	public FormeLegale getFormeJuridique() {
		return indexedData.getFormeJuridique();
	}

	public String getDomicileEtablissementPrincipal() {
		return indexedData.getDomicileEtablissementPrincipal();
	}

	public String getNom1() {
		return indexedData.getNom1();
	}

	public TypeEtatEntreprise getEtatEntreprise() {
		return indexedData.getEtatEntreprise();
	}
}
