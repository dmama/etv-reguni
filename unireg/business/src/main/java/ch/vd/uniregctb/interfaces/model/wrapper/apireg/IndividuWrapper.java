package ch.vd.uniregctb.interfaces.model.wrapper.apireg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import ch.vd.apireg.datamodel.AdrIndividu;
import ch.vd.apireg.datamodel.CaractIndividu;
import ch.vd.apireg.datamodel.HostIndividu;
import ch.vd.apireg.type.TypeSexe;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.interfaces.model.AdoptionReconnaissance;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.EntiteCivile;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.EtatCivilList;
import ch.vd.uniregctb.interfaces.model.HistoriqueIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Nationalite;
import ch.vd.uniregctb.interfaces.model.Origine;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.interfaces.model.Tutelle;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

public class IndividuWrapper implements EntiteCivile, Individu {

	private final RegDate date;
	private final long noIndividu;
	private final RegDate deces;
	private final RegDate naissance;
	private final Individu conjoint;
	private final EtatCivilList etatsCivils;
	private final String nouveauNoAVS;
	private final boolean isSexeMasculin;
	private final Collection<Adresse> adresses;
	private final HistoriqueIndividu dernierHistorique;
	private final String numeroRCE;

	public static IndividuWrapper get(HostIndividu target, RegDate date, ServiceInfrastructureService infraService) {
		if (target == null) {
			return null;
		}
		return new IndividuWrapper(target, date, infraService);
	}

	private IndividuWrapper(HostIndividu target, RegDate date, ServiceInfrastructureService infraService) {
		this.date = date;
		this.noIndividu = target.getNoIndividu();
		this.deces = RegDate.get(target.getDateDeces());
		this.naissance = RegDate.get(target.getDateNaissance());
		this.conjoint = extractConjoint(target, infraService);
		this.etatsCivils = extractEtatsCivils(target);
		this.nouveauNoAVS = extractNoAvs13(target);
		this.isSexeMasculin = (TypeSexe.MASCULIN == target.getSexe());
		this.adresses = extractAdresses(target, infraService);
		this.dernierHistorique = extractDernierHistorique(target);
		this.numeroRCE = target.getNoRegistreEtranger();
	}

	private IndividuWrapper(HostIndividu target, Individu conjoint, RegDate date, ServiceInfrastructureService infraService) {
		this.date = date;
		this.noIndividu = target.getNoIndividu();
		this.deces = RegDate.get(target.getDateDeces());
		this.naissance = RegDate.get(target.getDateNaissance());
		this.conjoint = conjoint;
		this.etatsCivils = extractEtatsCivils(target);
		this.nouveauNoAVS = extractNoAvs13(target);
		this.isSexeMasculin = (TypeSexe.MASCULIN == target.getSexe());
		this.adresses = extractAdresses(target, infraService);
		this.dernierHistorique = extractDernierHistorique(target);
		this.numeroRCE = target.getNoRegistreEtranger();
	}

	private IndividuWrapper extractConjoint(HostIndividu target, ServiceInfrastructureService infraService) {
		Set<ch.vd.apireg.datamodel.EtatCivil> etats = target.getEtatCivilList();
		if (etats == null) {
			return null;
		}

		// recherche de l'état civil valide à la date courante
		ch.vd.apireg.datamodel.EtatCivil dernier = null;
		for (ch.vd.apireg.datamodel.EtatCivil e : etats) {
			if (!DateHelper.isNullDate(e.getDateAnnulation())) {
				continue;
			}
			RegDate debut = RegDate.get(e.getDaValidite());
			if (date != null && debut.isAfter(date)) {
				continue;
			}
			if (dernier == null) {
				dernier = e;
			}
			else {
				if (e.getId().getNoSequence() > dernier.getId().getNoSequence()) {
					dernier = e;
				}
			}
		}
		IndividuWrapper c = null;
		if (dernier != null && dernier.getConjoint() != null) {
			c = new IndividuWrapper(dernier.getConjoint(), this, date, infraService);
		}
		return c;
	}

	private static EtatCivilList extractEtatsCivils(HostIndividu target) {
		List<EtatCivil> etatsCivils = new ArrayList<EtatCivil>();
		Set<ch.vd.apireg.datamodel.EtatCivil> targetEtatsCivils = target.getEtatCivilList();
		if (targetEtatsCivils != null) {
			for (ch.vd.apireg.datamodel.EtatCivil e : targetEtatsCivils) {
				if (!DateHelper.isNullDate(e.getDateAnnulation())) {
					continue;
				}
				etatsCivils.add(EtatCivilWrapper.get(e));
			}
		}
		return new EtatCivilList(target.getNoIndividu(), etatsCivils);
	}

	private String extractNoAvs13(HostIndividu target) {
		final Long noAvs13 = target.getNoAvs13();
		// [UNIREG-1223] interprète la valeur 0 comme une valeur nulle
		if (noAvs13 == null || noAvs13.longValue() == 0) {
			return null;
		}
		else {
			return noAvs13.toString();
		}
	}

	private Collection<Adresse> extractAdresses(HostIndividu target, ServiceInfrastructureService infraService) {
		List<Adresse> adresses = null;

		final Set<AdrIndividu> list = target.getAdresseList();
		if (list != null) {
			adresses = new ArrayList<Adresse>(list.size());
			for (AdrIndividu a : list) {
				if (!DateHelper.isNullDate(a.getDateAnnulation())) {
					// host-interface n'expose pas les adresses annulées, on fait de même.
					continue;
				}
				AdresseWrapper w = AdresseWrapper.get(a, infraService);
				if (w != null) {
					adresses.add(w);
				}
			}
		}

		return adresses;
	}

	private HistoriqueIndividu extractDernierHistorique(HostIndividu target) {

		CaractIndividu dernier = null;

		final Set<CaractIndividu> caracts = target.getCaractIndividuList();
		for (CaractIndividu c : caracts) {
			if (c.isAnnule()) {
				continue;
			}
			if (dernier == null) {
				dernier = c;
			}
			else if (c.getId().getNoSequence() > dernier.getId().getNoSequence()) {
				dernier = c;
			}
		}

		return HistoriqueIndividuWrapper.get(dernier);
	}

	public Collection<AdoptionReconnaissance> getAdoptionsReconnaissances() {
		throw new NotImplementedException();
	}

	public Individu getConjoint() {
		return conjoint;
	}

	public RegDate getDateDeces() {
		return deces;
	}

	public RegDate getDateNaissance() {
		return naissance;
	}

	public HistoriqueIndividu getDernierHistoriqueIndividu() {
		return dernierHistorique;
	}

	public Collection<Individu> getEnfants() {
		throw new NotImplementedException();
	}

	public EtatCivilList getEtatsCivils() {
		return etatsCivils;
	}

	public EtatCivil getEtatCivilCourant() {

		EtatCivil etatCivilCourant = null;

		int noSequence = -1;
		for (EtatCivil etatCivil : getEtatsCivils()) {
			if (etatCivil.getNoSequence() > noSequence) {
				etatCivilCourant = etatCivil;
				noSequence = etatCivil.getNoSequence();
			}
		}

		return etatCivilCourant;
	}

	public EtatCivil getEtatCivil(RegDate date) {
		return etatsCivils.getEtatCivilAt(date);
	}

	public Collection<HistoriqueIndividu> getHistoriqueIndividu() {
		throw new NotImplementedException();
	}

	public Individu getMere() {
		throw new NotImplementedException();
	}

	public Collection<Nationalite> getNationalites() {
		throw new NotImplementedException();
	}

	public long getNoTechnique() {
		return noIndividu;
	}

	public String getNouveauNoAVS() {
		return nouveauNoAVS;
	}

	public String getNumeroRCE() {
		return numeroRCE;
	}

	public Origine getOrigine() {
		throw new NotImplementedException();
	}

	public Individu getPere() {
		throw new NotImplementedException();
	}

	public Collection<Permis> getPermis() {
		throw new NotImplementedException();
	}

	public Tutelle getTutelle() {
		throw new NotImplementedException();
	}

	public boolean isSexeMasculin() {
		return isSexeMasculin;
	}

	public Collection<Adresse> getAdresses() {
		return adresses;
	}
}
