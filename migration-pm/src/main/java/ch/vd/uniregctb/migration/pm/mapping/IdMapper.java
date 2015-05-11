package ch.vd.uniregctb.migration.pm.mapping;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;
import ch.vd.uniregctb.migration.pm.regpm.WithLongId;
import ch.vd.uniregctb.migration.pm.utils.LockHelper;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.PersonnePhysique;

/**
 * Certaines entités (pas forcément les entreprises, mais en tous cas les établissements et les individus)
 * ont des identifiants dans RegPM différents de ceux qu'ils auront dans Unireg. Cette classe est responsable
 * de la maintenance de la correspondance entre les deux univers d'identifiants.
 */
public class IdMapper implements IdMapping {

	private static final String ENTREPRISE = "Entreprise";
	private static final String ETABLISSEMENT = "Etablissement";
	private static final String INDIVIDU = "Individu";

	private final LockHelper lock = new LockHelper(true);
	private final IdMapper reference;
	private final Map<Long, Long> entreprises;
	private final Map<Long, Long> etablissements;
	private final Map<Long, Long> individus;

	public IdMapper() {
		this(null);
	}

	private IdMapper(IdMapper reference) {
		this.reference = reference;
		this.entreprises = new HashMap<>();
		this.etablissements = new HashMap<>();
		this.individus = new HashMap<>();
	}

	private IdMapper(Map<Long, Long> entreprises, Map<Long, Long> etablissements, Map<Long, Long> individus) {
		this.reference = null;
		this.entreprises = entreprises;
		this.etablissements = etablissements;
		this.individus = individus;
	}

	/**
	 * Crée un nouveau mapper qui aura <code>this</code> pour référence
	 * @return un nouveau mapper dont la référence est <code>this</code> (on pourra ensuite remettre à jour <code>this</code> en appelant la méthode {@link #pushToReference()} sur ce mapper).
	 */
	public IdMapper withReference() {
		return new IdMapper(this);
	}

	/**
	 * @param sessionFactory session factory hibernate vers la base Unireg
	 * @return une instance de mapper sans référence et pré-rempli avec les données en base
	 */
	public static IdMapper fromDatabase(SessionFactory sessionFactory) {

		final Map<Long, Long> entreprises = new HashMap<>();
		final Map<Long, Long> etablissements = new HashMap<>();
		final Map<Long, Long> individus = new HashMap<>();

		//
		// Chargement de toutes les entités en base pour remplissage du mapper
		//

		//noinspection JpaQlInspection
		final Query query = sessionFactory.getCurrentSession().createQuery("select m from MigrationPmMapping m");

		//noinspection unchecked
		final Iterator<MigrationPmMapping> iterator = query.iterate();
		while (iterator.hasNext()) {
			final MigrationPmMapping mapping = iterator.next();
			switch (mapping.getTypeEntite()) {
			case ENTREPRISE:
				addEntityToMap(entreprises, mapping.getIdRegpm(), mapping.getIdUnireg(), ENTREPRISE,
				               oldValue -> buildPersistedMappingErrorText(ENTREPRISE, mapping.getIdRegpm(), mapping.getIdUnireg(), oldValue));
				break;
			case ETABLISSEMENT:
				addEntityToMap(etablissements, mapping.getIdRegpm(), mapping.getIdUnireg(), ETABLISSEMENT,
				               oldValue -> buildPersistedMappingErrorText(ETABLISSEMENT, mapping.getIdRegpm(), mapping.getIdUnireg(), oldValue));
				break;
			case INDIVIDU:
				addEntityToMap(individus, mapping.getIdRegpm(), mapping.getIdUnireg(), INDIVIDU,
				               oldValue -> buildPersistedMappingErrorText(INDIVIDU, mapping.getIdRegpm(), mapping.getIdUnireg(), oldValue));
				break;
			default:
				throw new IllegalArgumentException("Valeur non supportée : " + mapping.getTypeEntite());
			}
		}

		return new IdMapper(entreprises, etablissements, individus);
	}

	/**
	 * Gérère et persiste les entités {@link MigrationPmMapping} correspondant aux mappings locaux (dans la transaction courante)
	 * @param sessionFactory session factory hibernate vers la base Unireg
	 */
	public void pushLocalPartToDatabase(SessionFactory sessionFactory) {
		final Session session = sessionFactory.getCurrentSession();
		entreprises.forEach((idRegpm, idUnireg) -> persistNewMapping(session, MigrationPmMapping.TypeEntite.ENTREPRISE, idRegpm, idUnireg));
		etablissements.forEach((idRegpm, idUnireg) -> persistNewMapping(session, MigrationPmMapping.TypeEntite.ETABLISSEMENT, idRegpm, idUnireg));
		individus.forEach((idRegpm, idUnireg) -> persistNewMapping(session, MigrationPmMapping.TypeEntite.INDIVIDU, idRegpm, idUnireg));
	}

	private static MigrationPmMapping persistNewMapping(Session session, MigrationPmMapping.TypeEntite type, long idRegpm, long idUnireg) {
		final MigrationPmMapping mapping = new MigrationPmMapping(type, idRegpm, idUnireg);
		return (MigrationPmMapping) session.merge(mapping);

		// TODO est-on certain qu'il n'est pas possible de créer deux mappings identiques ici (si c'est possible, il faut blinder le cas...) ?
	}

	private void addEntity(Map<Long, Long> map, WithLongId regpm, Contribuable unireg, String categorieEntite, Function<Long, String> chgtMappingErrorText) {
		addEntity(map, regpm.getId(), unireg.getNumero(), categorieEntite, chgtMappingErrorText);
	}

	private void addEntity(Map<Long, Long> map, Long regpmId, Long uniregId, String categorieEntite, Function<Long, String> chgtMappingErrorText) {
		// tout ajout doit se faire avec un verrou exclusif
		lock.doInWriteLock(() -> addEntityToMap(map, regpmId, uniregId, categorieEntite, chgtMappingErrorText));
	}

	private static void addEntityToMap(Map<Long, Long> map, Long regpmId, Long uniregId, String categorieEntite, Function<Long, String> chgtMappingErrorText) {
		if (regpmId == null || uniregId == null) {
			throw new NullPointerException(String.format("%s sans identifiant", categorieEntite));
		}

		final Long oldValue = map.put(regpmId, uniregId);
		if (oldValue != null && !oldValue.equals(uniregId)) {
			// changement de valeur ?
			throw new IllegalArgumentException(chgtMappingErrorText.apply(oldValue));
		}
	}

	@FunctionalInterface
	private interface AppelReference {
		long getIdFromRegpmId(long idRegpm) throws NonExistentMappingException;
	}

	private long getIdUnireg(Map<Long, Long> mapLocale, long idRegpm, String categorie, @Nullable AppelReference appelReference) throws NonExistentMappingException {
		// toute consultation doit se faire à l'abri des modifications concurrentes
		return lock.doInReadLock(() -> {

			final Long idUnireg = mapLocale.get(idRegpm);
			if (idUnireg == null && appelReference != null) {
				return appelReference.getIdFromRegpmId(idRegpm);
			}
			if (idUnireg == null) {
				throw new NonExistentMappingException("Pas de mapping connu pour une entité '" + categorie + "' dont l'ID dans RegPM est " + idRegpm);
			}
			return idUnireg;
		});
	}

	private boolean hasMapping(Map<Long, Long> mapLocale, long idRegpm, @Nullable Predicate<Long> hasMappingInReference) {
		return lock.doInReadLock(() -> mapLocale.containsKey(idRegpm) || (hasMappingInReference != null && hasMappingInReference.test(idRegpm)));
	}

	@Override
	public void addEntreprise(RegpmEntreprise regpm, Entreprise unireg) {
		addEntity(entreprises, regpm, unireg, ENTREPRISE, oldValue -> buildMappingChangeErrorText(ENTREPRISE, regpm.getId(), oldValue, unireg.getNumero()));
	}

	@Override
	public void addEtablissement(RegpmEtablissement regpm, Etablissement unireg) {
		addEntity(etablissements, regpm, unireg, ETABLISSEMENT, oldValue -> buildMappingChangeErrorText(ETABLISSEMENT, regpm.getId(), oldValue, unireg.getNumero()));
	}

	@Override
	public void addIndividu(RegpmIndividu regpm, PersonnePhysique unireg) {
		addEntity(individus, regpm, unireg, INDIVIDU, oldValue -> buildMappingChangeErrorText(INDIVIDU, regpm.getId(), oldValue, unireg.getNumero()));
	}

	@Override
	public long getIdUniregEntreprise(long idRegpm) throws NonExistentMappingException {
		return getIdUnireg(entreprises, idRegpm, ENTREPRISE, reference != null ? reference::getIdUniregEntreprise : null);
	}

	@Override
	public long getIdUniregEtablissement(long idRegpm) throws NonExistentMappingException {
		return getIdUnireg(etablissements, idRegpm, ETABLISSEMENT, reference != null ? reference::getIdUniregEtablissement : null);
	}

	@Override
	public long getIdUniregIndividu(long idRegpm) throws NonExistentMappingException {
		return getIdUnireg(individus, idRegpm, INDIVIDU, reference != null ? reference::getIdUniregIndividu : null);
	}

	@Override
	public boolean hasMappingForEntreprise(long idRegpm) {
		return hasMapping(entreprises, idRegpm, reference != null ? reference::hasMappingForEntreprise : null);
	}

	@Override
	public boolean hasMappingForEtablissement(long idRegpm) {
		return hasMapping(etablissements, idRegpm, reference != null ? reference::hasMappingForEtablissement : null);
	}

	@Override
	public boolean hasMappingForIndividu(long idRegpm) {
		return hasMapping(individus, idRegpm, reference != null ? reference::hasMappingForIndividu : null);
	}

	private static String buildPersistedMappingErrorText(String categorieEntite, long regpmId, long oldUniregId, long newUniregId) {
		return String.format("Incohérence de chargement (%s) : regpm=%d, unireg=%d, autre unireg=%d", categorieEntite, regpmId, oldUniregId, newUniregId);
	}

	private static String buildMappingChangeErrorText(String categorieEntite, long regpmId, long oldUniregId, long newUniregId) {
		return String.format("Entité '%s' RegPM %d précédemment enregistrée avec l'ID Unireg %d aurait maintenant l'ID Unireg %d ??",
		                     categorieEntite, regpmId, oldUniregId, newUniregId);
	}

	/**
	 * A la fin du remplissage d'un mapper local qui avait une référence, on publie toutes les nouvelles entrées dans la référence.
	 * Ne fait rien si le mapper n'a pas de référence.
	 */
	public void pushToReference() {
		if (reference != null) {
			lock.doInReadLock(() -> {

				entreprises.forEach((regpmId, uniregId) -> reference.addEntity(reference.entreprises, regpmId, uniregId, ENTREPRISE,
				                                                               oldValue -> buildMappingChangeErrorText(ENTREPRISE, regpmId, oldValue, uniregId)));

				etablissements.forEach((regpmId, uniregId) -> reference.addEntity(reference.etablissements, regpmId, uniregId, ETABLISSEMENT,
				                                                                  oldValue -> buildMappingChangeErrorText(ETABLISSEMENT, regpmId, oldValue, uniregId)));

				individus.forEach((regpmId, uniregId) -> reference.addEntity(reference.individus, regpmId, uniregId, INDIVIDU,
				                                                             oldValue -> buildMappingChangeErrorText(INDIVIDU, regpmId, oldValue, uniregId)));
			});
		}
	}
}
