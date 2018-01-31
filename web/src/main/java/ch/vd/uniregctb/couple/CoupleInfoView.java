package ch.vd.uniregctb.couple;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.type.EtatCivil;

@SuppressWarnings({"UnusedDeclaration"})
public class CoupleInfoView {
	private String titre;
	private String confirmMessage;
	private TypeUnion type;
	private EtatCivil etatCivil;
	private String forceDateDebut;
	private Long forceMcId;

	public CoupleInfoView(CoupleManager.CoupleInfo info) {

		this.etatCivil = info.getEtatCivil();
		this.type = info.getType();
		this.forceDateDebut = info.getForceDateDebut() == null ? null : RegDateHelper.dateToDisplayString(info.getForceDateDebut());
		this.forceMcId = info.getForceMcId();

		if (type != null) {
			switch (type) {
			case SEUL:
				this.titre = "Création d'un marié seul";
				this.confirmMessage = "Voulez-vous vraiment créer un ménage commun avec cette personne seule ?";
				break;
			case COUPLE:
				this.titre = "Création d'un nouveau couple";
				this.confirmMessage = "Voulez-vous vraiment créer un ménage commun avec ces deux personnes ?";
				break;
			case FUSION_MENAGES:
				this.titre = "Fusion de deux ménages communs";
				this.confirmMessage = "Voulez-vous vraiment créer un ménage commun en fusionant ceux de ces deux personnes ?";
				break;
			case RECONCILIATION:
				this.titre = "Réconciliation de deux personnes";
				this.confirmMessage = "Voulez-vous vraiment réconcilier ces deux personnes ?";
				break;
			case RECONSTITUTION_MENAGE:
				this.titre = "Reconstitution d'un ménage commun";
				this.confirmMessage = "Voulez-vous vraiment créer un ménage commun avec ces deux personnes ?";
				break;
			}
		}
	}

	public String getTitre() {
		return titre;
	}

	public EtatCivil getEtatCivil() {
		return etatCivil;
	}

	public String getConfirmMessage() {
		return confirmMessage;
	}

	public TypeUnion getType() {
		return type;
	}

	public String getForceDateDebut() {
		return forceDateDebut;
	}

	public Long getForceMcId() {
		return forceMcId;
	}
}
