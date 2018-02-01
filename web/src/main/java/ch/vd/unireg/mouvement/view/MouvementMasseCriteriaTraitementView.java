package ch.vd.uniregctb.mouvement.view;

import ch.vd.uniregctb.type.Localisation;
import ch.vd.uniregctb.type.TypeMouvement;

/**
 * Vue pour la recherche des mouvements de dossiers de masse implémentée dans
 */
public class MouvementMasseCriteriaTraitementView extends MouvementMasseCriteriaMutiplesEtatsView {

	private boolean mouvementsPourArchives;

	private Long retireMvtId;

	private Long reinitMvtId;

	private long[] tabIdsMvts;

	public boolean isMouvementsPourArchives() {
		return mouvementsPourArchives;
	}

	public void setMouvementsPourArchives(boolean mouvementsPourArchives) {
		this.mouvementsPourArchives = mouvementsPourArchives;
	}

	public Long getRetireMvtId() {
		return retireMvtId;
	}

	public void setRetireMvtId(Long retireMvtId) {
		this.retireMvtId = retireMvtId;
	}

	public Long getReinitMvtId() {
		return reinitMvtId;
	}

	public void setReinitMvtId(Long reinitMvtId) {
		this.reinitMvtId = reinitMvtId;
	}

	public long[] getTabIdsMvts() {
		return tabIdsMvts;
	}

	public void setTabIdsMvts(long[] tabIdsMvts) {
		this.tabIdsMvts = tabIdsMvts;
	}

	@Override
	public void init() {
		super.init();
		mouvementsPourArchives = false;
		retireMvtId = null;
		reinitMvtId = null;
		tabIdsMvts = null;
	}

	@Override
	protected boolean getDefaultWantTous() {
		return false;
	}

	@Override
	protected boolean getDefaultWantATraiter() {
		return true;
	}

	@Override
	public TypeMouvement getTypeMouvement() {
		final TypeMouvement type;
		if (mouvementsPourArchives) {
			type = TypeMouvement.ReceptionDossier;
		}
		else if (getNoCollAdmDestinataire() != null) {
			type = TypeMouvement.EnvoiDossier;
		}
		else {
			type = null;
		}
		return type;
	}

	@Override
	public Localisation getLocalisationReception() {
		final Localisation localisation;
		if (mouvementsPourArchives) {
			localisation = Localisation.ARCHIVES;
		}
		else {
			localisation = null;
		}
		return localisation;
	}
}
