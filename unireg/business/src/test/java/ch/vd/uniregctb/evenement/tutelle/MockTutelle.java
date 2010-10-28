package ch.vd.uniregctb.evenement.tutelle;

import ch.vd.uniregctb.evenement.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.TuteurGeneral;
import ch.vd.uniregctb.type.TypeTutelle;

/**
 * Bouchon pour un événement de type Mise sous tutelle
 *
 * @author <a href="mailto:ludovic.bertin@oosphere.com">Ludovic BERTIN</a>
 */
public class MockTutelle extends MockEvenementCivil implements Tutelle {

	private Individu tuteur;
	private TuteurGeneral tuteurGeneral;
	private TypeTutelle typeTutelle;
	private CollectiviteAdministrative autoriteTutelaire;

	public Individu getTuteur() {
		return tuteur;
	}

	public void setTuteur(Individu tuteur) {
		this.tuteur = tuteur;
	}

	public TuteurGeneral getTuteurGeneral() {
		return tuteurGeneral;
	}

	public void setTuteurGeneral(TuteurGeneral tuteurGeneral) {
		this.tuteurGeneral = tuteurGeneral;
	}

	public TypeTutelle getTypeTutelle() {
		return typeTutelle;
	}

	public void setTypeTutelle(TypeTutelle typeTutelle) {
		this.typeTutelle = typeTutelle;
	}

	public CollectiviteAdministrative getAutoriteTutelaire() {
		return autoriteTutelaire;
	}

	public void setAutoriteTutelaire(CollectiviteAdministrative autoriteTutelaire) {
		this.autoriteTutelaire = autoriteTutelaire;
	}

	public boolean isContribuablePresentBefore() {
		return true;
	}
}
