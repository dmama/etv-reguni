package ch.vd.uniregctb.interfaces.model.wrapper;

import java.util.ArrayList;
import java.util.List;

import ch.vd.infrastructure.model.EnumCanton;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.Commune;

public class CommuneWrapper extends EntiteOFSWrapper implements Commune {

	private final ch.vd.infrastructure.model.Commune target;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private CollectiviteAdministrative administreePar = null;
	private List<CollectiviteAdministrative> collectivites = null;

	public static CommuneWrapper get(ch.vd.infrastructure.model.Commune target) {
		if (target == null) {
			return null;
		}
		return new CommuneWrapper(target);
	}

	private CommuneWrapper(ch.vd.infrastructure.model.Commune target) {
		super(target);
		this.target = target;
		this.dateDebut = RegDate.get(target.getDateDebutValidite());
		this.dateFin = RegDate.get(target.getDateFinValidite());
	}

	public CollectiviteAdministrative getAdminstreePar() {
		if (administreePar == null) {
			administreePar = CollectiviteAdministrativeWrapper.get(target.getAdminstreePar());
		}
		return administreePar;
	}

	public List<CollectiviteAdministrative> getCollectivites() {
		if (collectivites == null) {
			initCollectivites();
		}
		return collectivites;
	}

	private void initCollectivites() {
		synchronized (this) {
			if (collectivites == null) {
				collectivites = new ArrayList<CollectiviteAdministrative>();
				final List<?> targetCollectivites = target.getCollectivites();
				if (targetCollectivites != null) {
					for (Object o : targetCollectivites) {
						ch.vd.infrastructure.model.CollectiviteAdministrative c = (ch.vd.infrastructure.model.CollectiviteAdministrative) o;
						collectivites.add(CollectiviteAdministrativeWrapper.get(c));
					}
				}
			}
		}
	}

	public RegDate getDateDebutValidite() {
		return dateDebut;
	}

	public RegDate getDateFinValidite() {
		return dateFin;
	}

	public String getNoACI() {
		return target.getNoACI();
	}

	public String getNoCantonal() {
		return target.getNoCantonal();
	}

	public int getNoOFSEtendu() {
		if (isFraction()) {
			return target.getNoTechnique();
		}
		else {
			return target.getNoOFS();
		}
	}

	@Override
	public int getNoOFS() {
		return getNoOFSEtendu();
	}

	public String getNomAbrege() {
		return target.getNomAbrege();
	}

	public int getNumTechMere() {
		return target.getNumTechMere();
	}

	public String getSigleCanton() {
		return target.getSigleCanton();
	}

	public boolean isVaudoise() {
		return EnumCanton.SIGLE_VAUD.getName().equals(getSigleCanton());
	}

	public boolean isFraction() {
		return target.isFraction();
	}

	public boolean isPrincipale() {
		return target.isPrincipale();
	}

	public boolean isValide() {
		return target.isValide();
	}
}
