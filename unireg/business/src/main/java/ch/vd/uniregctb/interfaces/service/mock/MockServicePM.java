package ch.vd.uniregctb.interfaces.service.mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.pm.model.EnumFormeJuridique;
import ch.vd.registre.pm.model.EnumTypeAdresseEntreprise;
import ch.vd.uniregctb.adresse.AdressesPM;
import ch.vd.uniregctb.adresse.AdressesPMHisto;
import ch.vd.uniregctb.interfaces.model.AdresseEntreprise;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;
import ch.vd.uniregctb.interfaces.model.Rue;
import ch.vd.uniregctb.interfaces.model.mock.MockAdresseEntreprise;
import ch.vd.uniregctb.interfaces.model.mock.MockPersonneMorale;
import ch.vd.uniregctb.interfaces.service.ServicePersonneMoraleService;

public abstract class MockServicePM implements ServicePersonneMoraleService {

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
		Assert.isFalse(map.containsKey(Long.valueOf(numero)));
		map.put(Long.valueOf(numero), pm);
		return pm;
	}

	protected MockPersonneMorale addPM(long numero, String raisonSociale, EnumFormeJuridique forme, RegDate debut, RegDate fin) {
		Assert.isFalse(map.containsKey(Long.valueOf(numero)));
		MockPersonneMorale pm = new MockPersonneMorale();
		pm.setNumeroEntreprise(numero);
		pm.setRaisonSociale(raisonSociale);
		pm.setFormeJurique(forme);
		pm.setDateConstitution(debut);
		pm.setDateFinActivite(fin);
		map.put(Long.valueOf(numero), pm);
		return pm;
	}

	/**
	 * Ajoute une adresse pour l'individu spécifié.
	 *
	 * @param rue
	 * @param casePostale
	 */
	@SuppressWarnings("unchecked")
	protected AdresseEntreprise addAdresse(MockPersonneMorale pm, EnumTypeAdresseEntreprise type, Rue rue, String complement,
			Localite localite, RegDate debutValidite, RegDate finValidite) {

		MockAdresseEntreprise adresse = new MockAdresseEntreprise();
		adresse.setType(type);
		adresse.setComplement(complement);
		adresse.setRue(rue.getDesignationCourrier());
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

	public AdressesPM getAdresses(long noEntreprise, RegDate date) {

		AdressesPM adresses = new AdressesPM();

		final PersonneMorale entreprise = getPersonneMorale(noEntreprise);
		final Collection<AdresseEntreprise> adressesPM = entreprise.getAdresses();

		for (AdresseEntreprise a : adressesPM) {
			if (isActive(a, date)) {
				adresses.set(a);
			}
		}

		return adresses;
	}

	public AdressesPMHisto getAdressesHisto(long noEntreprise) {

		AdressesPMHisto adresses = new AdressesPMHisto();

		final PersonneMorale entreprise = getPersonneMorale(noEntreprise);
		final Collection<AdresseEntreprise> adressesPM = entreprise.getAdresses();

		for (AdresseEntreprise a : adressesPM) {
			adresses.add(a);
		}

		adresses.sort();
		return adresses;
	}

	public PersonneMorale getPersonneMorale(Long id) {
		return map.get(id);
	}

	public static boolean isActive(AdresseEntreprise adresse, RegDate date) {
		return RegDateHelper.isBetween(date, adresse.getDateDebutValidite(), adresse.getDateFinValidite(), NullDateBehavior.LATEST);
	}
}
