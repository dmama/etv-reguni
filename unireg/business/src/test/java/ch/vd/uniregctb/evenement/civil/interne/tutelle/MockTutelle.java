package ch.vd.uniregctb.evenement.civil.interne.tutelle;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.TuteurGeneral;
import ch.vd.uniregctb.type.TypeEvenementCivil;
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

	public MockTutelle(Individu individu, Long principalPPId, Individu conjoint, Long conjointPPId, RegDate date, Integer numeroOfsCommuneAnnonce, Individu tuteur, TuteurGeneral tuteurGeneral,
	                   TypeTutelle typeTutelle, CollectiviteAdministrative autoriteTutelaire) {
		super(individu, principalPPId, conjoint, conjointPPId, TypeEvenementCivil.MESURE_TUTELLE, date, numeroOfsCommuneAnnonce);
		this.tuteur = tuteur;
		this.tuteurGeneral = tuteurGeneral;
		this.typeTutelle = typeTutelle;
		this.autoriteTutelaire = autoriteTutelaire;
	}

	public Individu getTuteur() {
		return tuteur;
	}

	public TuteurGeneral getTuteurGeneral() {
		return tuteurGeneral;
	}

	public TypeTutelle getTypeTutelle() {
		return typeTutelle;
	}

	public CollectiviteAdministrative getAutoriteTutelaire() {
		return autoriteTutelaire;
	}

	public boolean isContribuablePresentBefore() {
		return true;
	}
}
