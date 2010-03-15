package ch.vd.uniregctb.interfaces.service.mock;

import java.util.*;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.pm.model.EnumTypeAdresseEntreprise;
import ch.vd.uniregctb.adresse.AdressesPM;
import ch.vd.uniregctb.adresse.AdressesPMHisto;
import ch.vd.uniregctb.interfaces.model.*;
import ch.vd.uniregctb.interfaces.model.mock.MockAdresseEntreprise;
import ch.vd.uniregctb.interfaces.model.mock.MockFormeJuridique;
import ch.vd.uniregctb.interfaces.model.mock.MockPersonneMorale;
import ch.vd.uniregctb.interfaces.service.PartPM;
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
		Assert.isFalse(map.containsKey(numero));
		map.put(numero, pm);
		return pm;
	}

	protected MockPersonneMorale addPM(long numero, String raisonSociale, String codeFormeJuridique, RegDate debut, RegDate fin) {
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

		final PersonneMorale entreprise = getPersonneMorale(noEntreprise, PartPM.ADRESSES);
		if (entreprise == null) {
			return null;
		}

		AdressesPM adresses = new AdressesPM();

		final Collection<AdresseEntreprise> adressesPM = entreprise.getAdresses();
		if (adressesPM != null) {
			for (AdresseEntreprise a : adressesPM) {
				if (isActive(a, date)) {
					adresses.set(a);
				}
			}
		}

		return adresses;
	}

	public AdressesPMHisto getAdressesHisto(long noEntreprise) {

		AdressesPMHisto adresses = new AdressesPMHisto();

		final PersonneMorale entreprise = getPersonneMorale(noEntreprise, PartPM.ADRESSES);
		final Collection<AdresseEntreprise> adressesPM = entreprise.getAdresses();

		for (AdresseEntreprise a : adressesPM) {
			adresses.add(a);
		}

		adresses.sort();
		return adresses;
	}

	public List<Long> getAllIds() {
		return new ArrayList<Long>(map.keySet());
	}

	public PersonneMorale getPersonneMorale(Long id, PartPM... parts) {
		return map.get(id);
	}

	public List<PersonneMorale> getPersonnesMorales(List<Long> ids, PartPM... parts) {
		List<PersonneMorale> list = new ArrayList<PersonneMorale>();
		for (Long id : ids) {
			PersonneMorale pm = map.get(id);
			list.add(pm);
		}
		return list;
	}

	public List<EvenementPM> findEvenements(Long numeroEntreprise, String code, RegDate minDate, RegDate maxDate) {
		return Collections.emptyList();
	}

	public static boolean isActive(AdresseEntreprise adresse, RegDate date) {
		return RegDateHelper.isBetween(date, adresse.getDateDebutValidite(), adresse.getDateFinValidite(), NullDateBehavior.LATEST);
	}
}
