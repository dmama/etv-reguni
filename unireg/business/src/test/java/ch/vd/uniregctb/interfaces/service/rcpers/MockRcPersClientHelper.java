package ch.vd.uniregctb.interfaces.service.rcpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.AdoptionReconnaissance;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.EtatCivilListImpl;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Nationalite;
import ch.vd.uniregctb.interfaces.model.RelationVersIndividu;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;
import ch.vd.uniregctb.interfaces.model.mock.MockEtatCivil;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;

public abstract class MockRcPersClientHelper implements RcPersClientHelper {
	
	private final Map<Long, MockIndividu> map = new HashMap<Long, MockIndividu>();

	protected MockRcPersClientHelper() {
		init();
	}

	public abstract void init();

	@Override
	public Individu getIndividuFromEvent(long eventId) {
		return map.get(eventId);
	}
	
	protected void addIndividuFromEvent(MockIndividu individu, long eventId) {
		map.put(eventId, individu);
	}
	
	protected MockIndividu createIndividu(long numero, @Nullable RegDate dateNaissance, String nom, String prenom, boolean isMasculin) {
		final MockIndividu individu = new MockIndividu();
		individu.setNoTechnique(numero);
		individu.setDateNaissance(dateNaissance);
		individu.setSexeMasculin(isMasculin);
		individu.setPrenom(prenom);
		individu.setNom(nom);

		// Etats civils
		final EtatCivilListImpl etatsCivils = new EtatCivilListImpl();
		etatsCivils.add(new MockEtatCivil(dateNaissance, null, TypeEtatCivil.CELIBATAIRE));
		individu.setEtatsCivils(etatsCivils);

		// Adresses
		final List<Adresse> sdresses = new ArrayList<Adresse>();
		individu.setAdresses(sdresses);

		// Enfants
		final List<RelationVersIndividu> enfants = new ArrayList<RelationVersIndividu>();
		individu.setEnfants(enfants);

		// Adoptions et reconnaissances
		final List<AdoptionReconnaissance> adoptions = new ArrayList<AdoptionReconnaissance>();
		individu.setAdoptionsReconnaissances(adoptions);
		
		// Nationalités
		final List<Nationalite> nationalites = new ArrayList<Nationalite>();
		individu.setNationalites(nationalites);

		individu.setConjoints(new ArrayList<RelationVersIndividu>());

		return individu;
	}
}
