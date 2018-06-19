package ch.vd.unireg.tiers;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.avatar.TypeAvatar;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.indexer.tiers.MenageCommunIndexable;
import ch.vd.unireg.indexer.tiers.TiersIndexedData;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.type.TypeEtatEntreprise;

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

	public RegDate getRegDateNaissanceInscriptionRC() {
		if (data.getTiersType().equals(MenageCommunIndexable.SUB_TYPE)) {
			// [UNIREG-2633] on n'affiche pas de dates de naissance sur les ménages communs
			return null;
		}
		return data.getRegDateNaissanceInscriptionRC();
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

	public RegDate getDateOuvertureFor() {
		return data.getDateOuvertureFor();
	}

	public RegDate getDateFermetureFor() {
		return data.getDateFermetureFor();
	}

	public RegDate getDateOuvertureForVd() {
		return data.getDateOuvertureForVd();
	}

	public RegDate getDateFermetureForVd() {
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

	public String getNumeroAVS1() {
		return data.getNavs13_1();
	}

	public String getNumeroAVS2() {
		return data.getNavs13_2();
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
