package ch.vd.uniregctb.interfaces.model.wrapper;

import java.util.ArrayList;
import java.util.List;

import ch.vd.infrastructure.model.EnumSigleUsageEmail;
import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.infrastructure.model.EnumTypeSupportEchangeInformation;
import ch.vd.infrastructure.model.TypeCollectivite;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Region;

public class CollectiviteAdministrativeWrapper implements CollectiviteAdministrative {

	private final ch.vd.infrastructure.model.CollectiviteAdministrative target;
	private Adresse adresse = null;
	private final RegDate dateFin;
	private List<Commune> communes = null;
	private Region region = null;

	public static CollectiviteAdministrativeWrapper get(ch.vd.infrastructure.model.CollectiviteAdministrative target) {
		if (target == null) {
			return null;
		}
		if (EnumTypeCollectivite.SIGLE_CIR.equals(target.getType().getEnumTypeCollectivite())) {
			return new OfficeImpotWrapper(target);
		}
		else {
			return new CollectiviteAdministrativeWrapper(target);
		}
	}

	protected CollectiviteAdministrativeWrapper(ch.vd.infrastructure.model.CollectiviteAdministrative target) {
		this.target = target;
		this.dateFin = RegDate.get(target.getDateFinValidite());
	}

	public Adresse getAdresse() {
		if (adresse == null) {
			adresse = AdresseWrapper.get(target.getAdresse());
		}
		return adresse;
	}

	public String getAdresseEmail() {
		return target.getAdresseEmail();
	}

	public String getAdresseEmail(EnumSigleUsageEmail sigleUsage) {
		return target.getAdresseEmail();
	}

	public List<Commune> getCommunes() {
		if (communes == null) {
			initCommunes();
		}
		return communes;
	}

	private void initCommunes() {
		synchronized (this) {
			if (communes == null) {
				communes = new ArrayList<Commune>();
				final List<?> targetCommunes = target.getCommunes();
				if (targetCommunes != null) {
					for (Object o : targetCommunes) {
						ch.vd.infrastructure.model.Commune c = (ch.vd.infrastructure.model.Commune) o;
						communes.add(CommuneWrapper.get(c));
					}
				}
			}
		}
	}

	public RegDate getDateFinValidite() {
		return dateFin;
	}

	public String getNoCCP() {
		return target.getNoCCP();
	}

	public int getNoColAdm() {
		return target.getNoColAdm();
	}

	public String getNoFax() {
		return target.getNoFax();
	}

	public String getNoTelephone() {
		return target.getNoTelephone();
	}

	public String getNomComplet1() {
		return target.getNomComplet1();
	}

	public String getNomComplet2() {
		return target.getNomComplet2();
	}

	public String getNomComplet3() {
		return target.getNomComplet3();
	}

	public String getNomCourt() {
		return target.getNomCourt();
	}

	public Region getRegionRattachement() {
		if (region == null) {
			region = RegionWrapper.get(target.getRegionRattachement());
		}
		return region;
	}

	public String getSigle() {
		return target.getSigle();
	}

	public String getSigleCanton() {
		return target.getSigleCanton();
	}

	public EnumTypeSupportEchangeInformation getSupportEchangeTAO() {
		return target.getSupportEchangeTAO();
	}

	public TypeCollectivite getType() {
		return target.getType();
	}

	public boolean isACI() {
		return target.isACI();
	}

	public boolean isOID() {
		return target.isOID();
	}

	public boolean isTiersTAO() {
		return target.isTiersTAO();
	}

	public boolean isValide() {
		return target.isValide();
	}

}
