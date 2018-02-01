package ch.vd.unireg.adresse;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.DonneeCivileEntreprise;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.RaisonSocialeFiscaleEntreprise;
import ch.vd.unireg.type.FormulePolitesse;

/*
 * Cette classe permet d'adapter une adresse supplémentaire (= spécialité UniregCTB) à l'interface d'adresse générique.
 */
public class AdresseMandataireAdapter extends AdresseFiscaleAdapter<AdresseMandataire> {

	private final Source source;

	/**
	 * Classe qui permet de transférer la civilité saisie sur l'adresse mandataire vers la première ligne de l'adresse
	 * (on a basé tout ça sur une Entreprise car le nom de l'entité tient alors dans un seul champ)
	 */
	private static final class TiersAvecCivilite extends Entreprise implements CiviliteSupplier {

		private final String nomDestinataire;
		private final String salutations;

		private TiersAvecCivilite(AdresseMandataire adresse) {
			this.nomDestinataire = adresse.getNomDestinataire();
			this.salutations = StringUtils.trimToNull(adresse.getCivilite());
		}

		@Override
		public Set<DonneeCivileEntreprise> getDonneesCiviles() {
			return Collections.singleton(new RaisonSocialeFiscaleEntreprise(null, null, nomDestinataire));
		}

		@Override
		public String getSalutations() {
			return salutations;
		}

		@Override
		public String getFormuleAppel() {
			// j'ai cru voir des salutations en "Maître", "Gérance"... dans les données issues de TAO qui doivent être reprises dans Unireg :
			// si, dans le cas de "Maître" (notaire...), on peut imaginer reprendre la même chose en formule d'appel, cela me paraît très étrange
			// dans le cas "Gérance"... Dans le doute, on prend "Madame, Monsieur"
			return FormulePolitesse.MADAME_MONSIEUR.formuleAppel();
		}
	}

	public AdresseMandataireAdapter(final AdresseMandataire adresse, ServiceInfrastructureService service) {
		super(adresse, service);
		this.source = new Source(SourceType.MANDATAIRE, new TiersAvecCivilite(adresse));
	}

	@Override
	public String getNumeroAppartement() {
		return null;
	}

	@Override
	public Source getSource() {
		return source;
	}

	@Override
	public boolean isDefault() {
		return false;
	}
}
