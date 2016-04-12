package ch.vd.uniregctb.entreprise.complexe;

import org.apache.commons.lang3.StringUtils;

import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.tiers.TiersIndexedDataView;
import ch.vd.uniregctb.type.TypeEtatEntreprise;

public class FusionEntreprisesAbsorbeeView implements Annulable {

	private final TiersIndexedDataView indexedData;
	private final boolean selectionnable;
	private final String explicationNonSelectionnable;

	public FusionEntreprisesAbsorbeeView(TiersIndexedDataView data, String explicationNonSelectionnable) {
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

	public String getDateNaissanceInscriptionRC() {
		return indexedData.getDateNaissanceInscriptionRC();
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
