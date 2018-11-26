package ch.vd.unireg.param.view;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.parametrage.ParametrePeriodeFiscaleSNC;

public class ParametrePeriodeFiscaleSNCEditView {

	private Long idPeriodeFiscale;
	private Integer anneePeriodeFiscale;
	private boolean codeControleSurRappelQSNC;

	private RegDate rappelReglementaire;
	private RegDate rappelEffectif;

	public ParametrePeriodeFiscaleSNCEditView() {
	}

	public ParametrePeriodeFiscaleSNCEditView(@NotNull PeriodeFiscale pf) {
		this.idPeriodeFiscale = pf.getId();
		this.anneePeriodeFiscale = pf.getAnnee();
		this.codeControleSurRappelQSNC = pf.isShowCodeControleRappelQuestionnaireSNC();

		final ParametrePeriodeFiscaleSNC data = pf.getParametrePeriodeFiscaleSNC();
		if (data != null) {
			this.rappelReglementaire = data.getTermeGeneralRappelImprime();
			this.rappelEffectif = data.getTermeGeneralRappelEffectif();
		}
	}

	public Long getIdPeriodeFiscale() {
		return idPeriodeFiscale;
	}

	public void setIdPeriodeFiscale(Long idPeriodeFiscale) {
		this.idPeriodeFiscale = idPeriodeFiscale;
	}

	public Integer getAnneePeriodeFiscale() {
		return anneePeriodeFiscale;
	}

	public void setAnneePeriodeFiscale(Integer anneePeriodeFiscale) {
		this.anneePeriodeFiscale = anneePeriodeFiscale;
	}

	public RegDate getRappelReglementaire() {
		return rappelReglementaire;
	}

	public void setRappelReglementaire(RegDate rappelReglementaire) {
		this.rappelReglementaire = rappelReglementaire;
	}

	public RegDate getRappelEffectif() {
		return rappelEffectif;
	}

	public void setRappelEffectif(RegDate rappelEffectif) {
		this.rappelEffectif = rappelEffectif;
	}

	public boolean isCodeControleSurRappelQSNC() {
		return codeControleSurRappelQSNC;
	}

	public void setCodeControleSurRappelQSNC(boolean codeControleSurRappelQSNC) {
		this.codeControleSurRappelQSNC = codeControleSurRappelQSNC;
	}

	public void saveTo(@NotNull PeriodeFiscale pf) {

		final ParametrePeriodeFiscaleSNC data = pf.getParametrePeriodeFiscaleSNC();
		if (data == null) {
			throw new ObjectNotFoundException("Impossible de retrouver les paramètres SNC pour la période fiscale " + pf.getAnnee());
		}
		data.setTermeGeneralRappelEffectif(rappelEffectif);
		data.setTermeGeneralRappelImprime(rappelReglementaire);

		pf.setShowCodeControleRappelQuestionnaireSNC(codeControleSurRappelQSNC);
	}
}
