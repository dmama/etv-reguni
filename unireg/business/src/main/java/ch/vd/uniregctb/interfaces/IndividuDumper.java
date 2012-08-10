package ch.vd.uniregctb.interfaces;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import gnu.trove.TIntObjectHashMap;

import ch.vd.registre.base.avs.AvsHelper;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.AdoptionReconnaissance;
import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.unireg.interfaces.civil.data.CasePostale;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.civil.data.Origine;
import ch.vd.unireg.interfaces.civil.data.Pays;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.interfaces.civil.data.RelationVersIndividu;
import ch.vd.unireg.interfaces.civil.data.Tutelle;
import ch.vd.unireg.interfaces.civil.data.TuteurGeneral;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypePermis;

public abstract class IndividuDumper {

	private static ServiceInfrastructureService infraService;

	private static final TIntObjectHashMap tabs = new TIntObjectHashMap();

	public static void setInfraService(ServiceInfrastructureService infraService) {
		IndividuDumper.infraService = infraService;
	}

	public static String dump(Collection<Individu> individus, boolean ignoreSpecific, boolean ignoreBugs) {
		final StringBuilder s = new StringBuilder();
		if (individus == null) {
			s.append("null");
		}
		else {
			s.append("[");
			boolean first = true;
			for (Individu individu : individus) {
				if (first) {
					first = false;
				}
				else {
					s.append(", ");
				}
				s.append(dump(individu, ignoreSpecific, ignoreBugs, false));
			}
		}
		return s.toString();
	}

	/**
	 * Dump l'individu spécifié sous forme de string.
	 *
	 * @param individu       un individu
	 * @param ignoreSpecific <b>vrai</b> si les différences connues et normales entre Reg-PP et RcPers doivent être ignorées; <b>faux</b> autrement.
	 * @param ignoreBugs     <b>vrai</b> si les différences entre Reg-PP et RcPers dues à des bugs RcPers doivent être ignorées; <b>faux</b> autrement.
	 * @param validateAVS    <b>vrai</b> s'il faut valider les numéros AVS et considérer comme nuls ceux qui ne valident pas.
	 * @return une string qui représente les données de l'individu.
	 */
	public static String dump(Individu individu, boolean ignoreSpecific, boolean ignoreBugs, boolean validateAVS) {
		return dump(individu, ignoreSpecific, ignoreBugs, validateAVS, 0);
	}

	private static String dump(Individu individu, boolean ignoreSpecific, boolean ignoreBugs, boolean validateAVS, int depth) {
		if (individu == null) {
			return "null";
		}

		StringBuilder s = new StringBuilder();
		s.append("Individu{\n");
		if (!ignoreSpecific) {
			s.append(tab(depth + 1)).append("adoptionsReconnaissances=").append(dumpAdoptions(individu.getAdoptionsReconnaissances(), depth + 1)).append(", \n");
		}
		s.append(tab(depth + 1)).append("adresses=").append(dumpAdresses(individu.getAdresses(), ignoreSpecific, depth + 1)).append(", \n");
		s.append(tab(depth + 1)).append("autresPrenoms=").append(dumpString(individu.getAutresPrenoms())).append(", \n");
		s.append(tab(depth + 1)).append("conjoints=").append(dumpRelationsVersIndividus(individu.getConjoints(), false, depth + 1)).append(", \n");
		s.append(tab(depth + 1)).append("dateDeces=").append(individu.getDateDeces()).append(", \n");
		s.append(tab(depth + 1)).append("dateNaissance=").append(individu.getDateNaissance()).append(", \n");
		if (!ignoreSpecific) {
			s.append(tab(depth + 1)).append("dateArriveeVD=").append(individu.getDateArriveeVD()).append(", \n");
		}
		s.append(tab(depth + 1)).append("enfants=").append(dumpRelationsVersIndividus(individu.getEnfants(), ignoreBugs, depth + 1)).append(", \n");
		s.append(tab(depth + 1)).append("etatsCivils=").append(dumpEtatsCivils(individu.getEtatsCivils(), depth + 1)).append(", \n");
		if (!ignoreSpecific) {
			s.append(tab(depth + 1)).append("nationalites=").append(dumpNationalites(individu.getNationalites(), ignoreSpecific, depth + 1)).append(", \n");
		}
		s.append(tab(depth + 1)).append("noAvs11=").append(dumpAVS11(individu.getNoAVS11(), validateAVS, individu.getDateNaissance(), individu.getSexe() == Sexe.MASCULIN)).append(", \n");
		s.append(tab(depth + 1)).append("noTechnique=").append(individu.getNoTechnique()).append(", \n");
		s.append(tab(depth + 1)).append("nom=").append(dumpString(individu.getNom())).append(", \n");
		s.append(tab(depth + 1)).append("nomNaissance=").append(dumpString(individu.getNomNaissance())).append(", \n");
		s.append(tab(depth + 1)).append("noAvs13=").append(dumpAVS13(individu.getNouveauNoAVS(), validateAVS)).append(", \n");
		s.append(tab(depth + 1)).append("numeroRCE=").append(dumpString(individu.getNumeroRCE())).append(", \n");
		s.append(tab(depth + 1)).append("origines=").append(dumpOrigines(individu.getOrigines(), depth + 1)).append(", \n");
		s.append(tab(depth + 1)).append("parents=").append(dumpRelationsVersIndividus(individu.getParents(), false, depth + 1)).append(", \n");
		s.append(tab(depth + 1)).append("permis=").append(dumpPermis(individu.getPermis(), ignoreSpecific, depth + 1)).append(", \n");
		s.append(tab(depth + 1)).append("prenom=").append(dumpString(individu.getPrenom())).append(", \n");
		s.append(tab(depth + 1)).append("sexe=").append(dumpSexe(individu.getSexe())).append(", \n");
		if (!ignoreSpecific) {
			s.append(tab(depth + 1)).append("tutelle=").append(dumpTutelle(individu.getTutelle(), depth + 1, ignoreSpecific, ignoreBugs, validateAVS)).append(", \n");
		}
		s.append(tab(depth)).append("}");

		return s.toString();
	}

	private static String dumpTutelle(Tutelle tutelle, int depth, boolean ignoreSpecific, boolean ignoreBugs, boolean validateAVS) {
		if (tutelle == null) {
			return "null";
		}

		StringBuilder s = new StringBuilder();
		s.append("Tutelle{\n");
		s.append(tab(depth + 1)).append("dateDebut=").append(tutelle.getDateDebut()).append(", \n");
		s.append(tab(depth + 1)).append("dateFin=").append(tutelle.getDateFin()).append(", \n");
		s.append(tab(depth + 1)).append("libelleMotif=").append(dumpString(tutelle.getLibelleMotif())).append(", \n");
		s.append(tab(depth + 1)).append("noSequence=").append(tutelle.getNoSequence()).append(", \n");
		s.append(tab(depth + 1)).append("nomAutoriteTutelaire=").append(dumpString(tutelle.getNomAutoriteTutelaire())).append(", \n");
		s.append(tab(depth + 1)).append("numeroCollectiviteAutoriteTutelaire=").append(tutelle.getNumeroCollectiviteAutoriteTutelaire()).append(", \n");
		s.append(tab(depth + 1)).append("tuteur=").append(dump(tutelle.getTuteur(), ignoreSpecific, ignoreBugs, validateAVS, depth + 1)).append(", \n");
		s.append(tab(depth + 1)).append("tuteurGeneral=").append(dumpTuteurGeneral(tutelle.getTuteurGeneral(), depth + 1)).append(", \n");
		s.append(tab(depth + 1)).append("type=").append(tutelle.getTypeTutelle()).append(", \n");
		s.append(tab(depth)).append("}");

		return s.toString();
	}

	private static String dumpTuteurGeneral(TuteurGeneral tuteurGeneral, int depth) {
		if (tuteurGeneral == null) {
			return "null";
		}

		StringBuilder s = new StringBuilder();
		s.append("TuteurGeneral{\n");
		s.append(tab(depth + 1)).append("noTelephoneContact=").append(tuteurGeneral.getNoTelephoneContact()).append(", \n");
		s.append(tab(depth + 1)).append("nomContact=").append(tuteurGeneral.getNomContact()).append(", \n");
		s.append(tab(depth + 1)).append("nomOffice=").append(tuteurGeneral.getNomOffice()).append(", \n");
		s.append(tab(depth + 1)).append("prenomContact=").append(tuteurGeneral.getPrenomContact()).append(", \n");
		s.append(tab(depth)).append("}");

		return s.toString();
	}

	private static String dumpPermis(Collection<Permis> coll, boolean ignoreSpecific, int depth) {
		if (coll == null || coll.isEmpty()) {
			return "null";
		}

		StringBuilder s = new StringBuilder();
		s.append("[");
		boolean first = true;
		for (Permis p : coll) {
			if (ignoreSpecific && p.getDateAnnulation() != null) {
				// RcPers n'expose pas les permis annulés
				continue;
			}
			if (first) {
				if (ignoreSpecific && p.getTypePermis() == TypePermis.PROVISOIRE && coll.size() > 1) {
					// les permis provisoires sont ignorés par RcPers, sauf si c'est le seul présent dans la liste
					continue;
				}
				first = false;
			}
			else {
				s.append(", ");
			}
			s.append(dumpPermis(p, depth + 1));
		}
		s.append("]");

		return s.toString();
	}

	private static String dumpPermis(Permis permis, int depth) {
		if (permis == null) {
			return "null";
		}

		StringBuilder s = new StringBuilder();
		s.append("Permis{\n");
		s.append(tab(depth + 1)).append("dateAnnulation=").append(permis.getDateAnnulation()).append(", \n");
		s.append(tab(depth + 1)).append("dateDebut=").append(permis.getDateDebut()).append(", \n");
		s.append(tab(depth + 1)).append("dateFin=").append(permis.getDateFin()).append(", \n");
		s.append(tab(depth + 1)).append("type=").append(permis.getTypePermis()).append(", \n");
		s.append(tab(depth)).append("}");

		return s.toString();
	}

	private static String dumpOrigines(Collection<Origine> coll, int depth) {
		if (coll == null) {
			return "null";
		}

		final List<Origine> list = new ArrayList<Origine>(coll);
		Collections.sort(list, new Comparator<Origine>() {
			@Override
			public int compare(Origine o1, Origine o2) {
				return o1.getNomLieu().compareTo(o2.getNomLieu());
			}
		});

		StringBuilder s = new StringBuilder();
		s.append("[");
		boolean first = true;
		for (Origine origine : list) {
			if (first) {
				first = false;
			}
			else {
				s.append(", ");
			}
			s.append(dumpOrigine(origine, depth + 1));
		}
		s.append("]");

		return s.toString();
	}

	private static String dumpOrigine(Origine origine, int depth) {
		if (origine == null) {
			return "null";
		}

		StringBuilder s = new StringBuilder();
		s.append("Origine{\n");
		s.append(tab(depth + 1)).append("nomLieu=").append(origine.getNomLieu()).append(", \n");
		s.append(tab(depth)).append("}");

		return s.toString();
	}

	private static String dumpNationalites(List<Nationalite> list, boolean ignoreSpecific, int depth) {
		if (list == null) {
			return "null";
		}

		StringBuilder s = new StringBuilder();
		s.append("[");
		boolean first = true;
		for (Nationalite nationalite : list) {
			if (first) {
				first = false;
			}
			else {
				s.append(", ");
			}
			s.append(dumpNationalite(nationalite, ignoreSpecific, depth + 1));
		}
		s.append("]");

		return s.toString();
	}

	private static String dumpNationalite(Nationalite nationalite, boolean ignoreSpecific, int depth) {
		if (nationalite == null) {
			return "null";
		}

		StringBuilder s = new StringBuilder();
		s.append("Nationalite{\n");
		if (!ignoreSpecific) {
			s.append(tab(depth + 1)).append("dateDebut=").append(nationalite.getDateDebut()).append(", \n");
			s.append(tab(depth + 1)).append("dateFin=").append(nationalite.getDateFin()).append(", \n");
		}
		s.append(tab(depth + 1)).append("pays=").append(dumpPays(nationalite.getPays(), ignoreSpecific, depth + 1)).append(", \n");
		s.append(tab(depth)).append("}");

		return s.toString();
	}

	private static String dumpPays(Pays pays, boolean ignoreSpecific, int depth) {
		if (pays == null) {
			return "null";
		}

		StringBuilder s = new StringBuilder();
		s.append("Pays{\n");
		if (!ignoreSpecific) {
			s.append(tab(depth + 1)).append("codeIso2=").append(dumpString(pays.getCodeIso2())).append(", \n");
			s.append(tab(depth + 1)).append("codeIso3=").append(dumpString(pays.getCodeIso3())).append(", \n");
		}
		s.append(tab(depth + 1)).append("noOfs=").append(pays.getNoOFS()).append(", \n");
		s.append(tab(depth + 1)).append("nomMajuscule=").append(dumpString(pays.getNomMajuscule())).append(", \n");
		s.append(tab(depth + 1)).append("nomMinuscule=").append(dumpString(pays.getNomMinuscule())).append(", \n");
		s.append(tab(depth + 1)).append("sigleOfs=").append(dumpString(pays.getSigleOFS())).append(", \n");
		s.append(tab(depth)).append("}");

		return s.toString();
	}

	private static String dumpEtatsCivils(List<EtatCivil> list, int depth) {
		if (list == null) {
			return "null";
		}

		StringBuilder s = new StringBuilder();
		s.append("[");
		boolean first = true;
		for (EtatCivil etatCivil : list) {
			if (first) {
				first = false;
			}
			else {
				s.append(", ");
			}
			s.append(dumpEtatCivil(etatCivil, depth + 1));
		}
		s.append("]");

		return s.toString();
	}

	private static String dumpEtatCivil(EtatCivil etatCivil, int depth) {
		if (etatCivil == null) {
			return "null";
		}

		StringBuilder s = new StringBuilder();
		s.append("EtatCivil{\n");
		s.append(tab(depth + 1)).append("dateDebut=").append(etatCivil.getDateDebut()).append(", \n");
		s.append(tab(depth + 1)).append("dateFin=").append(etatCivil.getDateFin()).append(", \n");
		s.append(tab(depth + 1)).append("type=").append(etatCivil.getTypeEtatCivil()).append(", \n");
		s.append(tab(depth)).append("}");

		return s.toString();
	}

	private static String dumpRelationsVersIndividus(Collection<RelationVersIndividu> conjoints, boolean ignoreBugs, int depth) {
		if (conjoints == null) {
			return "null";
		}

		StringBuilder s = new StringBuilder();
		s.append("[");
		boolean first = true;
		for (RelationVersIndividu conjoint : conjoints) {
			if (first) {
				first = false;
			}
			else {
				s.append(", ");
			}
			s.append(dumpRelationVersIndividu(conjoint, ignoreBugs, depth + 1));
		}
		s.append("]");

		return s.toString();
	}

	private static String dumpRelationVersIndividu(RelationVersIndividu rel, boolean ignoreBugs, int depth) {
		if (rel == null) {
			return "null";
		}

		StringBuilder s = new StringBuilder();
		s.append("RelationVersIndividu{\n");
		s.append(tab(depth + 1)).append("dateDebut=").append(rel.getDateDebut()).append(", \n");
		s.append(tab(depth + 1)).append("dateFin=").append(rel.getDateFin()).append(", \n");
		s.append(tab(depth + 1)).append("numeroAutreIndividu=").append(rel.getNumeroAutreIndividu()).append(", \n");
		s.append(tab(depth)).append("}");

		return s.toString();
	}

	private static String dumpAdresses(Collection<Adresse> list, boolean ignoreSpecific, int depth) {

		if (list == null) {
			return "null";
		}

		// on trie les adresses pour pouvoir plus facilement les comparer
		final ArrayList<Adresse> adresses = new ArrayList<Adresse>(list);
		Collections.sort(adresses, new Comparator<Adresse>() {
			@Override
			public int compare(Adresse o1, Adresse o2) {
				if (o1.getTypeAdresse() == o2.getTypeAdresse()) {
					return DateRangeComparator.compareRanges(o1, o2);
				}
				else {
					return o1.getTypeAdresse().compareTo(o2.getTypeAdresse());
				}
			}
		});

		final Integer noPaysInconnu = ServiceInfrastructureService.noPaysInconnu;

		StringBuilder s = new StringBuilder();
		s.append("[");
		boolean first = true;
		for (Adresse adresse : adresses) {
			if (ignoreSpecific) {
				if (noPaysInconnu.equals(adresse.getNoOfsPays())) {
					// le pays inconnu n'existe pas dans RcPers et les adresses avec ce pays ne sont pas migrées
					continue;
				}
				if (adresse.getTypeAdresse() == TypeAdresseCivil.PRINCIPALE && isHorsCantonOuHorsSuisse(adresse)) {
					// les adresses principales hors-canton et hors-Suisse ne sont pas stockées dans RcPers
					continue;
				}
			}
			if (first) {
				first = false;
			}
			else {
				s.append(", ");
			}
			s.append(dumpAdresse(adresse, ignoreSpecific, depth + 1));
		}
		s.append("]");

		return s.toString();
	}

	private static boolean isHorsCantonOuHorsSuisse(Adresse adresse) {
		if (adresse.getNoOfsPays() != ServiceInfrastructureService.noOfsSuisse) {
			return true;
		}
		if (infraService != null) {
			final Commune commune = infraService.getCommuneByNumeroOfsEtendu(adresse.getNoOfsCommuneAdresse(), adresse.getDateDebut());
			if (commune != null && !"VD".equals(commune.getSigleCanton())) {
				return true;
			}
			final int ordrePoste = adresse.getNumeroOrdrePostal();
			if (ordrePoste > 0) {
				if (isLocaliteHorsCanton(ordrePoste)) {
					return true;
				}
			}
			final Integer noRue = adresse.getNumeroRue();
			if (noRue != null) {
				final Rue rue = infraService.getRueByNumero(noRue);
				if (rue != null) {
					final Integer noLocalite = rue.getNoLocalite();
					if (noLocalite != null && isLocaliteHorsCanton(noLocalite)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static boolean isLocaliteHorsCanton(int ordrePoste) {
		boolean horscanton = false;
		final Localite localite = infraService.getLocaliteByONRP(ordrePoste);
		if (localite != null) {
			final Commune c = localite.getCommuneLocalite();
			if (c != null && !"VD".equals(c.getSigleCanton())) {
				horscanton = true;
			}
		}
		return horscanton;
	}

	private static String dumpAdresse(Adresse a, boolean ignoreSpecific, int depth) {
		if (a == null) {
			return "null";
		}

		StringBuilder s = new StringBuilder();
		s.append("Adresse{\n");
		if (!ignoreSpecific) {
			s.append(tab(depth + 1)).append("casePostale=").append(dumpCasePostale(a.getCasePostale(), depth + 1)).append(", \n");
			s.append(tab(depth + 1)).append("noOfsCommuneAdresse=").append(a.getNoOfsCommuneAdresse()).append(", \n");
		}
		s.append(tab(depth + 1)).append("dateDebut=").append(a.getDateDebut()).append(", \n");
		s.append(tab(depth + 1)).append("dateFin=").append(a.getDateFin()).append(", \n");
		if (!ignoreSpecific) {
			// Host-interfaces contient très peu d'egid/ewid, inutile de comparer avec RcPers
			s.append(tab(depth + 1)).append("egid=").append(a.getEgid()).append(", \n");
			s.append(tab(depth + 1)).append("ewid=").append(a.getEwid()).append(", \n");
			// Les localisation précédentes/suivantes n'existent pas dans Host-interfaces
			// Les localisation précédentes/suivantes n'existent pas dans Host-interfaces
			s.append(tab(depth + 1)).append("localisationPrecedente=").append(dumpLocalisation(a.getLocalisationPrecedente(), depth + 1)).append(", \n");
			s.append(tab(depth + 1)).append("localisationSuivante=").append(dumpLocalisation(a.getLocalisationSuivante(), depth + 1)).append(", \n");
		}
		s.append(tab(depth + 1)).append("localite=").append(dumpString(a.getLocalite())).append(", \n");
		s.append(tab(depth + 1)).append("noOfsPays=").append(a.getNoOfsPays()).append(", \n");
		if (!ignoreSpecific) {
			// RcPers a épuré la plupart des rues/numéros, à tel point que comparer avec Host-interfaces n'est plus possible
			s.append(tab(depth + 1)).append("numero=").append(dumpString(a.getNumero())).append(", \n");
		}
		s.append(tab(depth + 1)).append("numeroAppartement=").append(dumpString(a.getNumeroAppartement())).append(", \n");
		s.append(tab(depth + 1)).append("numeroOrdrePostal=").append(a.getNumeroOrdrePostal()).append(", \n");
		s.append(tab(depth + 1)).append("numeroPostal=").append(dumpString(a.getNumeroPostal())).append(", \n");
		s.append(tab(depth + 1)).append("numeroPostalComplementaire=").append(dumpString(a.getNumeroPostalComplementaire())).append(", \n");
		s.append(tab(depth + 1)).append("numeroRue=").append(a.getNumeroRue()).append(", \n");
		if (!ignoreSpecific) {
			// RcPers a épuré la plupart des rues/numéros, à tel point que comparer avec Host-interfaces n'est plus possible
			s.append(tab(depth + 1)).append("rue=").append(dumpString(a.getRue())).append(", \n");
		}
		s.append(tab(depth + 1)).append("titre=").append(dumpString(a.getTitre())).append(", \n");
		s.append(tab(depth + 1)).append("type=").append(a.getTypeAdresse()).append(", \n");
		s.append(tab(depth)).append("}");

		return s.toString();
	}

	private static String dumpLocalisation(Localisation loc, int depth) {
		if (loc == null) {
			return "null";
		}

		StringBuilder s = new StringBuilder();
		s.append("Localisation{\n");
		s.append(tab(depth + 1)).append("noOfs=").append(loc.getNoOfs()).append(", \n");
		s.append(tab(depth + 1)).append("type=").append(loc.getType()).append(", \n");
		s.append(tab(depth + 1)).append("adresseCourrier=").append(dumpAdresse(loc.getAdresseCourrier(), false, depth + 1)).append(", \n");
		s.append(tab(depth)).append("}");

		return s.toString();
	}

	private static String dumpCasePostale(CasePostale cp, int depth) {
		if (cp == null) {
			return "null";
		}

		StringBuilder s = new StringBuilder();
		s.append("CasePostale{\n");
		s.append(tab(depth + 1)).append("numero=").append(cp.getNumero()).append(", \n");
		s.append(tab(depth + 1)).append("type=").append(cp.getType()).append(", \n");
		s.append(tab(depth)).append("}");

		return s.toString();
	}

	private static String dumpAdoptions(Collection<AdoptionReconnaissance> collection, int depth) {

		if (collection == null) {
			return "null";
		}

		StringBuilder s = new StringBuilder();
		s.append("[");
		boolean first = true;
		for (AdoptionReconnaissance a : collection) {
			if (first) {
				first = false;
			}
			else {
				s.append(", ");
			}
			s.append(dumpAdoption(a, depth + 1));
		}
		s.append("]");

		return s.toString();
	}

	private static String dumpAdoption(AdoptionReconnaissance a, int depth) {
		if (a == null) {
			return "null";
		}

		StringBuilder s = new StringBuilder();
		s.append("AdoptionReconnaissance{\n");
		if (a.getAdopteReconnu() != null) {
			s.append(tab(depth + 1)).append("adopteReconnu=").append(a.getAdopteReconnu().getNoTechnique()).append(", \n");
		}
		s.append(tab(depth + 1)).append("dateAccueilAdoption=").append(a.getDateAccueilAdoption()).append(", \n");
		s.append(tab(depth + 1)).append("dateAdoption=").append(a.getDateAdoption()).append(", \n");
		s.append(tab(depth + 1)).append("dateDesaveu=").append(a.getDateDesaveu()).append(", \n");
		s.append(tab(depth + 1)).append("dateReconnaissance=").append(a.getDateReconnaissance()).append(", \n");
		s.append(tab(depth)).append("}");

		return s.toString();
	}

	private static String tab(int n) {
		String t = (String) tabs.get(n);
		if (t == null) {
			final char[] buf = new char[4 * n];
			for (int i = 0; i < buf.length; i++) {
				buf[i] = ' ';
			}
			t = new String(buf);
			tabs.put(n, t);
		}
		return t;
	}

	private static String dumpAVS11(String s, boolean validateAVS, RegDate dateNaissance, boolean isMasculin) {
		if (s == null) {
			return "null";
		}
		if (validateAVS && !AvsHelper.isValidAncienNumAVS(s, dateNaissance, isMasculin)) {
			return "null";
		}
		if (s.endsWith("000") || s.endsWith("999")) { // RcPers considère comme invalides les numéros qui finissent avec ces modulos
			return "null";
		}
		return "\"" + s + "\"";
	}

	private static String dumpAVS13(String s, boolean validateAVS) {
		if (s == null) {
			return "null";
		}
		if (validateAVS && !AvsHelper.isValidNouveauNumAVS(s)) {
			return "null";
		}
		return "\"" + s + "\"";
	}

	private static String dumpSexe(Sexe sexe) {
		return sexe != null ? sexe.name() : "null";
	}

	private static String dumpString(String s) {
		if (s == null) {
			return "null";
		}
		return "\"" + s + "\"";
	}
}
