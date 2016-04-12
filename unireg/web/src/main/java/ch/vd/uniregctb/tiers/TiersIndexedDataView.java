package ch.vd.uniregctb.tiers;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.uniregctb.avatar.TypeAvatar;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.indexer.tiers.MenageCommunIndexable;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.type.TypeEtatEntreprise;

public class TiersIndexedDataView implements Annulable {

	private final TiersIndexedData data;

	public TiersIndexedDataView(TiersIndexedData data) {
		this.data = data;
	}

	public Long getNumero() {
		return data.getNumero();
	}

	public String getTiersType() {
		return data.getTiersType();
	}

	public Integer getNoOfsCommuneDomicile() {
		return data.getNoOfsCommuneDomicile();
	}

	@Override
	public boolean isAnnule() {
		return data.isAnnule();
	}

	public String getRoleLigne1() {
		return data.getRoleLigne1();
	}

	public String getRoleLigne2() {
		return data.getRoleLigne2();
	}

	public String getNom1() {
		return data.getNom1();
	}

	public String getNom2() {
		return data.getNom2();
	}

	public String getDateNaissanceInscriptionRC() {
		if (data.getTiersType().equals(MenageCommunIndexable.SUB_TYPE)) {
			// [UNIREG-2633] on n'affiche pas de dates de naissance sur les ménages communs
			return null;
		}
		return data.getDateNaissanceInscriptionRC();
	}

	public String getDateDeces() {
		if (data.getTiersType().equals(MenageCommunIndexable.SUB_TYPE)) {
			// [UNIREG-2633] on n'affiche pas de dates de naissance sur les ménages communs
			return null;
		}
		return data.getDateDeces();
	}

	public String getRue() {
		return data.getRue();
	}

	public String getNpa() {
		return data.getNpa();
	}

	public String getLocalite() {
		return data.getLocalite();
	}

	public String getLocaliteOuPays() {
		final String localiteOuPays;
		final String localite = data.getLocalite();
		final String pays = data.getPays();
		if (StringUtils.isBlank(localite)) {
			localiteOuPays = pays;
		}
		else {
			localiteOuPays = localite;
		}
		return localiteOuPays;
	}

	public String getPays() {
		return data.getPays();
	}

	public String getForPrincipal() {
		return data.getForPrincipal();
	}

	public Date getDateOuvertureFor() {
		return data.getDateOuvertureFor();
	}

	public Date getDateFermetureFor() {
		return data.getDateFermetureFor();
	}

	public Date getDateOuvertureForVd() {
		return data.getDateOuvertureForVd();
	}

	public Date getDateFermetureForVd() {
		return data.getDateFermetureForVd();
	}

	public boolean isDebiteurInactif() {
		return data.isDebiteurInactif();
	}

	public TypeAvatar getTypeAvatar() {
		return data.getTypeAvatar();
	}

	public String getNumeroIDE() {
		final List<String> numerosIDE = data.getNumerosIDE();
		return numerosIDE != null && !numerosIDE.isEmpty() ? numerosIDE.get(0) : null;
	}

	public String getDomicileEtablissementPrincipal() {
		return data.getDomicileEtablissementPrincipal();
	}

	public FormeLegale getFormeJuridique() {
		return data.getFormeJuridique();
	}

	public TypeEtatEntreprise getEtatEntreprise() {
		return data.getEtatEntreprise();
	}
}
