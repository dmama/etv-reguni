package ch.vd.uniregctb.foncier;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.uniregctb.common.AbstractJobResults;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.CollectivitePubliqueRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteCommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteImmeubleRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonneRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.ImmeubleBeneficiaireRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.PersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.ServitudeRF;
import ch.vd.uniregctb.registrefoncier.SituationRF;
import ch.vd.uniregctb.registrefoncier.TiersRF;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.tiers.Contribuable;

public class InitialisationIFoncResults extends AbstractJobResults<Long, InitialisationIFoncResults> {

	public final RegDate dateReference;
	public final int nbThreads;

	public static class NomPrenomRaisonSocialeDateNaissance {
		public final String prenom;
		public final String nom;
		public final String raisonSociale;
		public final RegDate dateNaissance;

		public static final NomPrenomRaisonSocialeDateNaissance EMPTY = new NomPrenomRaisonSocialeDateNaissance(null, null, null, null);

		public NomPrenomRaisonSocialeDateNaissance(String prenom, String nom, String raisonSociale, RegDate dateNaissance) {
			this.prenom = prenom;
			this.nom = nom;
			this.raisonSociale = raisonSociale;
			this.dateNaissance = dateNaissance;
		}

		public NomPrenomRaisonSocialeDateNaissance(PersonnePhysiqueRF pp) {
			this(pp.getPrenom(), pp.getNom(), null, pp.getDateNaissance());
		}

		public NomPrenomRaisonSocialeDateNaissance(CollectivitePubliqueRF cp) {
			this(null, null, cp.getRaisonSociale(), null);
		}

		public NomPrenomRaisonSocialeDateNaissance(PersonneMoraleRF pm) {
			this(null, null, pm.getRaisonSociale(), null);
		}
	}

	private static final Map<Class<? extends AyantDroitRF>, Function<? extends AyantDroitRF, NomPrenomRaisonSocialeDateNaissance>> RF_IDENT_AYANT_DROIT_EXTRACTOR = buildRfIdentificationAyantDroitExtractors();

	private static Map<Class<? extends AyantDroitRF>, Function<? extends AyantDroitRF, NomPrenomRaisonSocialeDateNaissance>> buildRfIdentificationAyantDroitExtractors() {
		final Map<Class<? extends AyantDroitRF>, Function<? extends AyantDroitRF, NomPrenomRaisonSocialeDateNaissance>> map = new HashMap<>();
		addToRfIdentificationAyantDroitExtractors(map, PersonnePhysiqueRF.class, NomPrenomRaisonSocialeDateNaissance::new);
		addToRfIdentificationAyantDroitExtractors(map, CollectivitePubliqueRF.class, NomPrenomRaisonSocialeDateNaissance::new);
		addToRfIdentificationAyantDroitExtractors(map, PersonneMoraleRF.class, NomPrenomRaisonSocialeDateNaissance::new);
		return map;
	}

	private static <T extends AyantDroitRF> void addToRfIdentificationAyantDroitExtractors(Map<Class<? extends AyantDroitRF>, Function<? extends AyantDroitRF, NomPrenomRaisonSocialeDateNaissance>> map,
	                                                                                       Class<T> clazz,
	                                                                                       Function<T, NomPrenomRaisonSocialeDateNaissance> extractor) {
		map.put(clazz, extractor);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	private static <T extends AyantDroitRF> InitialisationIFoncResults.NomPrenomRaisonSocialeDateNaissance buildNomPrenomRaisonSociale(T ayantDroit) {
		final Function<T, NomPrenomRaisonSocialeDateNaissance> extractor = (Function<T, NomPrenomRaisonSocialeDateNaissance>) RF_IDENT_AYANT_DROIT_EXTRACTOR.get(ayantDroit.getClass());
		return extractor != null ? extractor.apply(ayantDroit) : null;
	}

	private static final Map<Class<? extends DroitRF>, Function<?, CommunauteRF>> COMMUNAUTE_EXTRACTORS = buildIdCommunauteExtractors();

	private static Map<Class<? extends DroitRF>, Function<?, CommunauteRF>> buildIdCommunauteExtractors() {
		final Map<Class<? extends DroitRF>, Function<?, CommunauteRF>> map = new HashMap<>();
		addToIdCommunauteExtractors(map, DroitProprietePersonneMoraleRF.class, DroitProprietePersonneRF::getCommunaute);
		addToIdCommunauteExtractors(map, DroitProprietePersonnePhysiqueRF.class, DroitProprietePersonneRF::getCommunaute);
		addToIdCommunauteExtractors(map, DroitProprieteCommunauteRF.class, droit -> (CommunauteRF) droit.getAyantDroit());
		return map;
	}

	private static <T extends DroitRF> void addToIdCommunauteExtractors(Map<Class<? extends DroitRF>, Function<?, CommunauteRF>> map,
	                                                                    Class<T> clazz,
	                                                                    Function<? super T, CommunauteRF> extractor) {
		map.put(clazz, extractor);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	private static <T extends DroitRF> CommunauteRF extractCommunaute(T droit) {
		final Function<? super T, CommunauteRF> extractor = (Function<? super T, CommunauteRF>) COMMUNAUTE_EXTRACTORS.get(droit.getClass());
		return extractor != null ? extractor.apply(droit) : null;
	}

	/**
	 * Pour les droits de propriété entre immeubles, renvoie l'identifiant de l'immeuble bénéficiaire, et <code>null</code> pour les autres
	 * @param droit le droit dont on veut extraire l'information
	 * @return l'identifiant de l'immeuble bénéficiaire, s'il existe
	 */
	@Nullable
	private static Long getIdImmeubleBeneficiaire(DroitRF droit) {
		return Optional.of(droit)
				.filter(DroitProprieteImmeubleRF.class::isInstance)
				.map(DroitProprieteImmeubleRF.class::cast)
				.map(DroitProprieteImmeubleRF::getAyantDroit)
				.map(ImmeubleBeneficiaireRF.class::cast)
				.map(ImmeubleBeneficiaireRF::getImmeuble)
				.map(ImmeubleRF::getId)
				.orElse(null);
	}

	@Nullable
	private static Long getNoRFAyantDroit(AyantDroitRF ayantDroit) {
		return Optional.of(ayantDroit)
				.filter(TiersRF.class::isInstance)
				.map(TiersRF.class::cast)
				.map(TiersRF::getNoRF)
				.orElse(null);
	}

	public static class InfoImmeuble {
		public final Long idImmeuble;
		public final String egrid;
		public final RegDate dateRadiationImmeuble;
		public final Integer noParcelle;
		public final Integer index1;
		public final Integer index2;
		public final Integer index3;
		public final String nomCommune;
		public final Integer noOfsCommune;

		public InfoImmeuble(ImmeubleRF immeuble, @Nullable SituationRF situation) {
			idImmeuble = immeuble.getId();
			egrid = immeuble.getEgrid();
			dateRadiationImmeuble = immeuble.getDateRadiation();
			if (situation != null) {
				noParcelle = situation.getNoParcelle();
				index1 = situation.getIndex1();
				index2 = situation.getIndex2();
				index3 = situation.getIndex3();

				final CommuneRF commune = situation.getCommune();
				nomCommune = commune.getNomRf();
				noOfsCommune = commune.getNoOfs();
			}
			else {
				noParcelle = null;
				index1 = null;
				index2 = null;
				index3 = null;
				nomCommune = null;
				noOfsCommune = null;
			}
		}
	}

	public static class InfoExtraction {

		public final Long idContribuable;
		public final Long idCommunaute;
		public final NomPrenomRaisonSocialeDateNaissance identificationRF;
		public final Class<? extends AyantDroitRF> classAyantDroit;
		public final String idRFAyantDroit;
		public final Long noRFAyantDroit;
		public final Class<? extends DroitRF> classDroit;
		public final GenrePropriete regime;
		public final String motifDebut;
		public final RegDate dateDebut;
		public final String motifFin;
		public final RegDate dateFin;
		public final Fraction part;
		public final InfoImmeuble infoImmeuble;
		public final Long idImmeubleBeneficiaire;

		public InfoExtraction(@Nullable Contribuable contribuable, @NotNull DroitProprieteRF droit, @Nullable SituationRF situation) {
			// information du contribuable
			if (contribuable != null) {
				idContribuable = contribuable.getNumero();
			}
			else {
				idContribuable = null;
			}

			final AyantDroitRF ayantDroit = droit.getAyantDroit();

			// information d'identification en provenance du RF
			identificationRF = buildNomPrenomRaisonSociale(ayantDroit);
			noRFAyantDroit = getNoRFAyantDroit(ayantDroit);

			// l'éventuelle communauté
			idCommunaute = Optional.ofNullable(extractCommunaute(droit)).map(CommunauteRF::getId).orElse(null);

			// les données du droit lui-même
			classAyantDroit = ayantDroit.getClass();
			idRFAyantDroit = ayantDroit.getIdRF();
			classDroit = droit.getClass();
			motifDebut = droit.getMotifDebut();
			dateDebut = droit.getDateDebutMetier();
			motifFin = droit.getMotifFin();
			dateFin = droit.getDateFinMetier();
			regime = droit.getRegime();
			part = droit.getPart();
			idImmeubleBeneficiaire = getIdImmeubleBeneficiaire(droit);

			// les données de l'immeuble et de sa situation
			infoImmeuble = new InfoImmeuble(droit.getImmeuble(), situation);
		}

		public InfoExtraction(@Nullable Contribuable contribuable, @NotNull ServitudeRF servitude, @Nullable SituationRF situation) {

			// préconditions
			final Set<AyantDroitRF> ayantDroits = servitude.getAyantDroits();
			if (ayantDroits.size() != 1) {
				throw new IllegalArgumentException("La servitude ne doit contenir qu'un seul bénéficiaire");
			}
			final Set<ImmeubleRF> immeubles = servitude.getImmeubles();
			if (immeubles.size() != 1) {
				throw new IllegalArgumentException("La servitude ne doit contenir qu'un seul immeuble");
			}

			// information du contribuable
			if (contribuable != null) {
				idContribuable = contribuable.getNumero();
			}
			else {
				idContribuable = null;
			}

			final AyantDroitRF ayantDroit = ayantDroits.iterator().next();

			// information d'identification en provenance du RF
			identificationRF = buildNomPrenomRaisonSociale(ayantDroit);
			noRFAyantDroit = getNoRFAyantDroit(ayantDroit);

			// l'éventuelle communauté
			idCommunaute = Optional.ofNullable(extractCommunaute(servitude)).map(CommunauteRF::getId).orElse(null);

			// les données du droit lui-même
			classAyantDroit = ayantDroit.getClass();
			idRFAyantDroit = ayantDroit.getIdRF();
			classDroit = servitude.getClass();
			motifDebut = servitude.getMotifDebut();
			dateDebut = servitude.getDateDebutMetier();
			motifFin = servitude.getMotifFin();
			dateFin = servitude.getDateFinMetier();
			regime = null;
			part = null;
			idImmeubleBeneficiaire = getIdImmeubleBeneficiaire(servitude);

			// les données de l'immeuble et de sa situation
			infoImmeuble = new InfoImmeuble(immeubles.iterator().next(), situation);
		}

		public InfoExtraction(ImmeubleRF immeuble, @Nullable SituationRF situation) {
			idContribuable = null;
			idCommunaute = null;
			identificationRF = null;
			classAyantDroit = null;
			idRFAyantDroit = null;
			noRFAyantDroit = null;
			classDroit = null;
			regime = null;
			dateDebut = null;
			motifDebut = null;
			dateFin = null;
			motifFin = null;
			part = null;
			idImmeubleBeneficiaire = null;
			infoImmeuble = new InfoImmeuble(immeuble, situation);
		}
	}

	public static class ErreurImmeuble {
		public final long idImmeuble;
		public final String message;
		public final String stackTrace;

		public ErreurImmeuble(long idImmeuble, Exception e) {
			this.idImmeuble = idImmeuble;
			this.message = e.getMessage();
			this.stackTrace = ExceptionUtils.extractCallStack(e);
		}
	}

	public static class ImmeubleIgnore {
		public final InfoImmeuble infoImmeuble;
		public final String raison;

		public ImmeubleIgnore(ImmeubleRF immeuble, @Nullable SituationRF situation, String raison) {
			this.infoImmeuble = new InfoImmeuble(immeuble, situation);
			this.raison = raison;
		}
	}

	private long nbImmeublesInspectes = 0;
	private final List<InfoExtraction> lignesExtraites = new LinkedList<>();
	private final List<ImmeubleIgnore> immeublesIgnores = new LinkedList<>();
	private final List<ErreurImmeuble> erreurs = new LinkedList<>();

	public InitialisationIFoncResults(RegDate dateReference, int nbThreads) {
		this.dateReference = dateReference;
		this.nbThreads = nbThreads;
	}

	@Override
	public void addErrorException(Long idImmeuble, Exception e) {
		this.erreurs.add(new ErreurImmeuble(idImmeuble, e));
	}

	public void addDroitPropriete(@Nullable Contribuable contribuable, @NotNull DroitProprieteRF droit, @Nullable SituationRF situation) {
		this.lignesExtraites.add(new InfoExtraction(contribuable, droit, situation));
	}

	public void addServitude(@Nullable Contribuable contribuable, @NotNull ServitudeRF servitude, @Nullable SituationRF situation) {
		this.lignesExtraites.add(new InfoExtraction(contribuable, servitude, situation));
	}

	public void addImmeubleSansDroit(ImmeubleRF immeuble, @Nullable SituationRF situation) {
		this.lignesExtraites.add(new InfoExtraction(immeuble, situation));
	}

	public void addImmeubleSansDroitADateReference(ImmeubleRF immeuble, @Nullable SituationRF situation) {
		this.immeublesIgnores.add(new ImmeubleIgnore(immeuble, situation, "Aucun droit valide à la date de référence."));
	}

	public void onNewImmeuble() {
		++ nbImmeublesInspectes;
	}

	@Override
	public void addAll(InitialisationIFoncResults right) {
		lignesExtraites.addAll(right.lignesExtraites);
		erreurs.addAll(right.erreurs);
		immeublesIgnores.addAll(right.immeublesIgnores);
		nbImmeublesInspectes += right.nbImmeublesInspectes;
	}

	@Override
	public void end() {
		lignesExtraites.sort(Comparator.comparing(info -> info.idContribuable, Comparator.nullsLast(Comparator.naturalOrder())));
		erreurs.sort(Comparator.comparing(err -> err.idImmeuble));
		immeublesIgnores.sort(Comparator.comparing(info -> info.infoImmeuble.idImmeuble));
		super.end();
	}

	public long getNbImmeublesInspectes() {
		return nbImmeublesInspectes;
	}

	public List<InfoExtraction> getLignesExtraites() {
		return lignesExtraites;
	}

	public List<ImmeubleIgnore> getImmeublesIgnores() {
		return immeublesIgnores;
	}

	public List<ErreurImmeuble> getErreurs() {
		return erreurs;
	}
}
