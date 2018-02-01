package ch.vd.unireg.avatar;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.common.MimeTypeHelper;
import ch.vd.unireg.tiers.AutreCommunaute;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.Sexe;

public class AvatarServiceImpl implements AvatarService {

	private TiersService tiersService;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	private static final Pattern EXTENSION_EXTRACTOR = Pattern.compile("\\.([a-z]+)$");
	private static final Map<String, String> MIME_TYPES = buildMimeTypes();

	private static Map<String, String> buildMimeTypes() {
		final Map<String, String> map = new HashMap<>();
		map.put("png", MimeTypeHelper.MIME_PNG);
		map.put("jpg", MimeTypeHelper.MIME_JPEG);
		return map;
	}

	private final Map<Class<? extends Tiers>, Function<? extends Tiers, TypeAvatar>> AVATAR_TYPE_COMPUTERS = buildAvatarTypeComputers();

	private static <T extends Tiers> void registerAvatarTypeComputer(Map<Class<? extends Tiers>, Function<? extends Tiers, TypeAvatar>> map,
	                                                                 Class<T> clazz,
	                                                                 Function<T, TypeAvatar> computer) {
		map.put(clazz, computer);
	}

	@NotNull
	private Map<Class<? extends Tiers>, Function<? extends Tiers, TypeAvatar>> buildAvatarTypeComputers() {
		final Map<Class<? extends Tiers>, Function<? extends Tiers, TypeAvatar>> map = new HashMap<>();
		registerAvatarTypeComputer(map, Entreprise.class, e -> TypeAvatar.ENTREPRISE);
		registerAvatarTypeComputer(map, Etablissement.class, e -> TypeAvatar.ETABLISSEMENT);
		registerAvatarTypeComputer(map, AutreCommunaute.class, e -> TypeAvatar.AUTRE_COMM);
		registerAvatarTypeComputer(map, CollectiviteAdministrative.class, e -> TypeAvatar.COLLECT_ADMIN);
		registerAvatarTypeComputer(map, DebiteurPrestationImposable.class, e -> TypeAvatar.DEBITEUR);
		registerAvatarTypeComputer(map, PersonnePhysique.class, this::getTypeAvatar);
		registerAvatarTypeComputer(map, MenageCommun.class, this::getTypeAvatar);
		return Collections.unmodifiableMap(map);
	}

	private static final Map<Pair<Sexe, Sexe>, TypeAvatar> TYPES_MENAGES = buildTypesMenages();

	@NotNull
	private static Map<Pair<Sexe, Sexe>, TypeAvatar> buildTypesMenages() {
		final Map<Pair<Sexe, Sexe>, TypeAvatar> map = new HashMap<>();
		map.put(Pair.of(Sexe.MASCULIN, Sexe.FEMININ), TypeAvatar.MC_MIXTE);
		map.put(Pair.of(Sexe.MASCULIN, Sexe.MASCULIN), TypeAvatar.MC_HOMME_HOMME);
		map.put(Pair.of(Sexe.MASCULIN, null), TypeAvatar.MC_HOMME_SEUL);
		map.put(Pair.of(Sexe.FEMININ, Sexe.MASCULIN), TypeAvatar.MC_MIXTE);
		map.put(Pair.of(Sexe.FEMININ, Sexe.FEMININ), TypeAvatar.MC_FEMME_FEMME);
		map.put(Pair.of(Sexe.FEMININ, null), TypeAvatar.MC_FEMME_SEULE);
		map.put(Pair.of(null, null), TypeAvatar.MC_SEXE_INCONNU);
		return Collections.unmodifiableMap(map);
	}

	private static final Map<TypeAvatar, String> IMAGE_PATHS = buildImagePaths();

	@NotNull
	private static Map<TypeAvatar, String> buildImagePaths() {
		final Map<TypeAvatar, String> map = new EnumMap<>(TypeAvatar.class);
		map.put(TypeAvatar.HOMME, "homme.png");
		map.put(TypeAvatar.FEMME, "femme.png");
		map.put(TypeAvatar.SEXE_INCONNU, "inconnu.png");
		map.put(TypeAvatar.MC_MIXTE, "menagecommun.png");
		map.put(TypeAvatar.MC_HOMME_SEUL, "homme_seul.png");
		map.put(TypeAvatar.MC_FEMME_SEULE, "femme_seule.png");
		map.put(TypeAvatar.MC_HOMME_HOMME, "homme_homme.png");
		map.put(TypeAvatar.MC_FEMME_FEMME, "femme_femme.png");
		map.put(TypeAvatar.MC_SEXE_INCONNU, "mc_inconnu.png");
		map.put(TypeAvatar.ENTREPRISE, "entreprise.png");
		map.put(TypeAvatar.ETABLISSEMENT, "etablissement.png");
		map.put(TypeAvatar.AUTRE_COMM, "autrecommunaute.png");
		map.put(TypeAvatar.COLLECT_ADMIN, "collectiviteadministrative.png");
		map.put(TypeAvatar.DEBITEUR, "debiteur.png");
		return Collections.unmodifiableMap(map);
	}

	@Override
	public ImageData getAvatar(Tiers tiers, boolean withLink) {
		final TypeAvatar type = getTypeAvatar(tiers);
		return getAvatar(type, withLink);
	}

	@Override
	public ImageData getAvatar(TypeAvatar type, boolean withLink) {
		final String imagePath = getImagePath(type, withLink);
		final String mime = MIME_TYPES.get(extractExtension(imagePath));
		if (mime == null) {
			throw new IllegalArgumentException("Le format de l'image n'est pas reconnu: " + imagePath);
		}
		return new ImageData(mime, AvatarServiceImpl.class.getResourceAsStream(imagePath));
	}

	private static String extractExtension(String imageName) {
		final Matcher matcher = EXTENSION_EXTRACTOR.matcher(imageName);
		if (matcher.find()) {
			return matcher.group(1);
		}
		else {
			return null;
		}
	}

	private TypeAvatar getTypeAvatar(PersonnePhysique pp) {
		final Sexe sexe = tiersService.getSexe(pp);
		if (sexe == null) {
			return TypeAvatar.SEXE_INCONNU;
		}
		else if (sexe == Sexe.MASCULIN) {
			return TypeAvatar.HOMME;
		}
		else {
			return TypeAvatar.FEMME;
		}
	}

	private TypeAvatar getTypeAvatar(MenageCommun mc) {
		final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(mc, null);
		final PersonnePhysique principal = ensemble.getPrincipal();
		final PersonnePhysique conjoint = ensemble.getConjoint();

		Sexe sexePrincipal = tiersService.getSexe(principal);
		Sexe sexeConjoint = tiersService.getSexe(conjoint);
		if (sexePrincipal == null && sexeConjoint != null) {
			// Le conjoint passe principal si son sexe est connu mais que celui du principal ne l'est pas
			sexePrincipal = sexeConjoint;
			sexeConjoint = null;
		}
		return TYPES_MENAGES.get(Pair.of(sexePrincipal, sexeConjoint));
	}

	private <T extends Tiers> TypeAvatar _getTypeAvatar(@NotNull T tiers) {
		//noinspection unchecked
		final Function<T, TypeAvatar> computer = (Function<T, TypeAvatar>) AVATAR_TYPE_COMPUTERS.get(tiers.getClass());
		return computer != null ? computer.apply(tiers) : null;
	}

	@Override
	public TypeAvatar getTypeAvatar(Tiers tiers) {
		if (tiers == null) {
			return null;
		}
		return _getTypeAvatar(tiers);
	}

	static String getImagePath(TypeAvatar type, boolean forLink) {
		final String image = IMAGE_PATHS.get(type);
		return String.format("%s/%s", forLink ? "link" : "nolink", image);
	}
}
