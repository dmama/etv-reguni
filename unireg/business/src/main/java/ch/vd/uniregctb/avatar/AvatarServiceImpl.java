package ch.vd.uniregctb.avatar;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.vd.uniregctb.common.MimeTypeHelper;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.Sexe;

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

	@Override
	public TypeAvatar getTypeAvatar(Tiers tiers) {

		final TypeAvatar type;

		if (tiers instanceof PersonnePhysique) {
			final Sexe sexe = tiersService.getSexe((PersonnePhysique) tiers);
			if (sexe == null) {
				type = TypeAvatar.SEXE_INCONNU;
			}
			else if (sexe == Sexe.MASCULIN) {
				type = TypeAvatar.HOMME;
			}
			else {
				type = TypeAvatar.FEMME;
			}
		}
		else if (tiers instanceof Entreprise) {
			type = TypeAvatar.ENTREPRISE;
		}
		else if (tiers instanceof Etablissement) {
			type = TypeAvatar.ETABLISSEMENT;
		}
		else if (tiers instanceof AutreCommunaute) {
			type = TypeAvatar.AUTRE_COMM;
		}
		else if (tiers instanceof MenageCommun) {
			final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple((MenageCommun) tiers, null);
			final PersonnePhysique principal = ensemble.getPrincipal();
			final PersonnePhysique conjoint = ensemble.getConjoint();

			Sexe sexePrincipal = tiersService.getSexe(principal);
			Sexe sexeConjoint = tiersService.getSexe(conjoint);
			if (sexePrincipal == null && sexeConjoint != null) {
				// Le conjoint passe principal si son sexe est connu mais que celui du principal ne l'est pas
				sexePrincipal = sexeConjoint;
				sexeConjoint = null;
			}

			if (sexePrincipal == null) {
				type = TypeAvatar.MC_SEXE_INCONNU;
			}
			else if (sexeConjoint == null) {
				if (sexePrincipal == Sexe.MASCULIN) {
					type = TypeAvatar.MC_HOMME_SEUL;
				}
				else {
					type = TypeAvatar.MC_FEMME_SEULE;
				}
			}
			else {
				if (sexePrincipal == sexeConjoint) {
					if (sexePrincipal == Sexe.MASCULIN) {
						type = TypeAvatar.MC_HOMME_HOMME;
					}
					else {
						type = TypeAvatar.MC_FEMME_FEMME;
					}
				}
				else {
					type = TypeAvatar.MC_MIXTE;
				}
			}
		}
		else if (tiers instanceof CollectiviteAdministrative) {
			type = TypeAvatar.COLLECT_ADMIN;
		}
		else if (tiers instanceof DebiteurPrestationImposable) {
			type = TypeAvatar.DEBITEUR;
		}
		else {
			type = null;
		}

		return type;
	}

	static String getImagePath(TypeAvatar type, boolean forLink) {
		final String image;
		switch (type) {
		case HOMME:
			image = "homme.png";
			break;
		case FEMME:
			image = "femme.png";
			break;
		case SEXE_INCONNU:
			image = "inconnu.png";
			break;
		case MC_MIXTE:
			image = "menagecommun.png";
			break;
		case MC_HOMME_SEUL:
			image = "homme_seul.png";
			break;
		case MC_FEMME_SEULE:
			image = "femme_seule.png";
			break;
		case MC_HOMME_HOMME:
			image = "homme_homme.png";
			break;
		case MC_FEMME_FEMME:
			image = "femme_femme.png";
			break;
		case MC_SEXE_INCONNU:
			image = "mc_inconnu.png";
			break;
		case ENTREPRISE:
			image = "entreprise.png";
			break;
		case ETABLISSEMENT:
			image = "etablissement.png";
			break;
		case AUTRE_COMM:
			image = "autrecommunaute.png";
			break;
		case COLLECT_ADMIN:
			image = "collectiviteadministrative.png";
			break;
		case DEBITEUR:
			image = "debiteur.png";
			break;
		default:
			throw new IllegalArgumentException("Type de tiers inconnu = [" + type + ']');
		}

		return String.format("%s/%s", forLink ? "link" : "nolink", image);
	}
}
