package ch.vd.uniregctb.migration.pm.utils;

import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.PersonnePhysique;

/**
 * Certaines entités (pas forcément les entreprises, mais en tous cas les établissements et les individus)
 * ont des identifiants dans RegPM différents de ceux qu'ils auront dans Unireg. Cette classe est responsable
 * de la maintenance de la correspondance entre les deux univers d'identifiants.
 */
public class IdMapper {

	public static class NonExistentMappingException extends RuntimeException {
		public NonExistentMappingException(String message) {
			super(message);
		}
	}

	private final Map<Long, Long> entreprises = new HashMap<>();
	private final Map<Long, Long> etablissements = new HashMap<>();
	private final Map<Long, Long> individus = new HashMap<>();

	public void addEntreprise(RegpmEntreprise regpm, Entreprise unireg) {
		if (regpm.getId() == null || unireg.getId() == null) {
			throw new NullPointerException("Entreprise sans identifiant");
		}

		final Long oldValue = entreprises.put(regpm.getId(), unireg.getId());
		if (oldValue != null && !oldValue.equals(unireg.getId())) {
			// changement de valeur ?
			throw new IllegalArgumentException("Entreprise RegPM " + regpm.getId() + " précédemment enregistrée avec l'ID Unireg " + oldValue + " aurait maintenant l'ID Unireg " + unireg.getId() + " aussi ??");
		}
	}

	public void addEtablissement(RegpmEtablissement regpm, Etablissement unireg) {
		if (regpm.getId() == null || unireg.getId() == null) {
			throw new NullPointerException("Etablissement sans identifiant");
		}

		final Long oldValue = etablissements.put(regpm.getId(), unireg.getId());
		if (oldValue != null && !oldValue.equals(unireg.getId())) {
			// changement de valeur ?
			throw new IllegalArgumentException("Etablissement RegPM " + regpm.getId() + " précédemment enregistré avec l'ID Unireg " + oldValue + " aurait maintenant l'ID Unireg " + unireg.getId() + " aussi ??");
		}
	}

	public void addIndividu(RegpmIndividu regpm, PersonnePhysique unireg) {
		if (regpm.getId() == null || unireg.getId() == null) {
			throw new NullPointerException("Individu sans identifiant");
		}

		final Long oldValue = individus.put(regpm.getId(), unireg.getId());
		if (oldValue != null && !oldValue.equals(unireg.getId())) {
			// changement de valeur ?
			throw new IllegalArgumentException("Individu RegPM " + regpm.getId() + " précédemment enregistré avec l'ID Unireg " + oldValue + " aurait maintenant l'ID Unireg " + unireg.getId() + " aussi ??");
		}
	}

	public long getIdUniregEntreprise(long idRegpm) throws NonExistentMappingException {
		final Long idUnireg = entreprises.get(idRegpm);
		if (idUnireg == null) {
			throw new NonExistentMappingException("Pas de mapping connu pour une entreprise dont l'ID dans RegPM est " + idRegpm);
		}
		return idUnireg;
	}

	public long getIdUniregEtablissement(long idRegpm) throws NonExistentMappingException {
		final Long idUnireg = etablissements.get(idRegpm);
		if (idUnireg == null) {
			throw new NonExistentMappingException("Pas de mapping connu pour un établissement dont l'ID dans RegPM est " + idRegpm);
		}
		return idUnireg;
	}

	public long getIdUniregIndividu(long idRegpm) throws NonExistentMappingException {
		final Long idUnireg = individus.get(idRegpm);
		if (idUnireg == null) {
			throw new NonExistentMappingException("Pas de mapping connu pour un individu dont l'ID dans RegPM est " + idRegpm);
		}
		return idUnireg;
	}
}
