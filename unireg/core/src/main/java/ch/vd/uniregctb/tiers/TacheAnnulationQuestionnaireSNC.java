package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.hibernate.annotations.ForeignKey;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.QuestionnaireSNC;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.type.TypeTache;

@Entity
@DiscriminatorValue("ANNUL_QSNC")
public class TacheAnnulationQuestionnaireSNC extends Tache {

	private QuestionnaireSNC questionnaireSNC;

	// Ce constructeur est requis par Hibernate
	protected TacheAnnulationQuestionnaireSNC() {
	}

	public TacheAnnulationQuestionnaireSNC(TypeEtatTache etat, RegDate dateEcheance, Contribuable contribuable, QuestionnaireSNC questionnaireSNC,
	                                       CollectiviteAdministrative collectiviteAdministrativeAssignee) {
		super(etat, dateEcheance, contribuable, collectiviteAdministrativeAssignee);
		this.questionnaireSNC = questionnaireSNC;
	}

	@ManyToOne
	@JoinColumn(name = "DECLARATION_ID")
	@ForeignKey(name = "FK_TACH_DECL_ID")
	public QuestionnaireSNC getQuestionnaireSNC() {
		return questionnaireSNC;
	}

	public void setQuestionnaireSNC(QuestionnaireSNC questionnaire) {
		this.questionnaireSNC = questionnaire;
	}

	@Transient
	@Override
	public TypeTache getTypeTache() {
		return TypeTache.TacheAnnulationQuestionnaireSNC;
	}
}
