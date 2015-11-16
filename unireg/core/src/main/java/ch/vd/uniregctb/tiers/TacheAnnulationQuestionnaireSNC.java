package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.QuestionnaireSNC;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.type.TypeTache;

@Entity
@DiscriminatorValue("ANNUL_QSNC")
public class TacheAnnulationQuestionnaireSNC extends TacheAnnulationDeclaration<QuestionnaireSNC> {

	// Ce constructeur est requis par Hibernate
	protected TacheAnnulationQuestionnaireSNC() {
	}

	public TacheAnnulationQuestionnaireSNC(TypeEtatTache etat, RegDate dateEcheance, Contribuable contribuable, QuestionnaireSNC questionnaireSNC,
	                                       CollectiviteAdministrative collectiviteAdministrativeAssignee) {
		super(etat, dateEcheance, contribuable, questionnaireSNC, collectiviteAdministrativeAssignee);
	}

	@Transient
	@Override
	public TypeTache getTypeTache() {
		return TypeTache.TacheAnnulationQuestionnaireSNC;
	}
}
