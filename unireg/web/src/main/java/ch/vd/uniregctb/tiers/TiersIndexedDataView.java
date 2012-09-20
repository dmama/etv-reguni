package ch.vd.uniregctb.tiers;

import java.util.Date;

import org.apache.commons.lang.StringUtils;

import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.indexer.tiers.MenageCommunIndexable;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;

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

	public String getDateNaissance() {
		if (data.getTiersType().equals(MenageCommunIndexable.SUB_TYPE)) {
			// [UNIREG-2633] on n'affiche pas de dates de naissance sur les ménages communs
			return null;
		}
		return data.getDateNaissance();
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

	public boolean isDebiteurInactif() {
		return data.isDebiteurInactif();
	}

}
