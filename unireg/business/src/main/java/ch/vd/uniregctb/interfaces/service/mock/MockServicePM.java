package ch.vd.uniregctb.interfaces.service.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.uniregctb.interfaces.model.AdresseEntreprise;
import ch.vd.uniregctb.interfaces.model.Etablissement;
import ch.vd.uniregctb.interfaces.model.EvenementPM;
import ch.vd.uniregctb.interfaces.model.PartPM;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;
import ch.vd.uniregctb.interfaces.model.mock.MockAdresseEntreprise;
import ch.vd.uniregctb.interfaces.model.mock.MockFormeJuridique;
import ch.vd.uniregctb.interfaces.model.mock.MockPersonneMorale;
import ch.vd.uniregctb.interfaces.service.ServicePersonneMoraleBase;
import ch.vd.uniregctb.type.TypeAdressePM;

public abstract class MockServicePM extends ServicePersonneMoraleBase {

	private final Map<Long, PersonneMorale> map = new HashMap<Long, PersonneMorale>();

	/**
	 * Constructeur par défaut qui appel init pour initialiser le mock.
	 */
	public MockServicePM() {
		init();
	}

	/**
	 * Cette méthode permet d'initialise le mock en fonction des données voulues.
	 */
	protected abstract void init();

	protected PersonneMorale addPM(PersonneMorale pm) {
		final long numero = pm.getNumeroEntreprise();
		Assert.isTrue(numero > 0);
		Assert.isFalse(map.containsKey(numero));
		map.put(numero, pm);
		return pm;
	}

	/**
	 * Ajoute ou remplace une PM existante par une autre
	 *
	 * @param pm une personne morale
	 */
	public void replacePM(MockPersonneMorale pm) {
		final long numero = pm.getNumeroEntreprise();
		Assert.isTrue(numero > 0);
		map.put(numero, pm);
	}

	protected MockPersonneMorale addPM(long numero, String raisonSociale, String codeFormeJuridique, RegDate debut, @Nullable RegDate fin) {
		Assert.isFalse(map.containsKey(numero));
		MockPersonneMorale pm = new MockPersonneMorale();
		pm.setNumeroEntreprise(numero);
		pm.setRaisonSociale(raisonSociale);
		pm.getFormesJuridiques().add(new MockFormeJuridique(null, null, codeFormeJuridique));
		pm.setDateConstitution(debut);
		pm.setDateFinActivite(fin);
		map.put(numero, pm);
		return pm;
	}

	/**
	 * Ajoute une adresse pour la PM spécifiée.
	 */
	@SuppressWarnings("unchecked")
	protected AdresseEntreprise addAdresse(MockPersonneMorale pm, TypeAdressePM type, @Nullable Rue rue, @Nullable String complement,
	                                       Localite localite, RegDate debutValidite, @Nullable RegDate finValidite) {
		return addAdresse(pm, type, rue, null, complement, localite, debutValidite, finValidite);
	}

	/**
	 * Ajoute une adresse pour la PM spécifiée.
	 */
	@SuppressWarnings("unchecked")
	protected AdresseEntreprise addAdresse(MockPersonneMorale pm, TypeAdressePM type, @Nullable Rue rue, @Nullable String numeroMaison, @Nullable String complement,
	                                       Localite localite, RegDate debutValidite, @Nullable RegDate finValidite) {

		MockAdresseEntreprise adresse = new MockAdresseEntreprise();
		adresse.setType(type);
		adresse.setComplement(complement);
		if (rue != null) {
			adresse.setRue(rue.getDesignationCourrier());
		}
		adresse.setNumeroMaison(numeroMaison);
		adresse.setLocalite(localite.getNomAbregeMinuscule());
		adresse.setDateDebutValidite(debutValidite);
		adresse.setDateFinValidite(finValidite);

		Collection<AdresseEntreprise> adresses = pm.getAdresses();
		if (adresses == null) {
			adresses = new ArrayList<AdresseEntreprise>();
			pm.setAdresses(adresses);
		}
		adresses.add(adresse);

		return adresse;
	}

	@Override
	public List<Long> getAllIds() {
		return new ArrayList<Long>(map.keySet());
	}

	@Override
	public PersonneMorale getPersonneMorale(Long id, PartPM... parts) {
		return map.get(id);
	}

	@Override
	public List<PersonneMorale> getPersonnesMorales(List<Long> ids, PartPM... parts) {
		List<PersonneMorale> list = new ArrayList<PersonneMorale>();
		for (Long id : ids) {
			PersonneMorale pm = map.get(id);
			list.add(pm);
		}
		return list;
	}

	@Override
	public Etablissement getEtablissement(long id) {
		return null;
	}

	@Override
	public List<Etablissement> getEtablissements(List<Long> ids) {
		return Collections.emptyList();
	}

	@Override
	public List<EvenementPM> findEvenements(long numeroEntreprise, String code, RegDate minDate, RegDate maxDate) {
		return Collections.emptyList();
	}

	public static boolean isActive(AdresseEntreprise adresse, RegDate date) {
		return RegDateHelper.isBetween(date, adresse.getDateDebutValidite(), adresse.getDateFinValidite(), NullDateBehavior.LATEST);
	}
}
