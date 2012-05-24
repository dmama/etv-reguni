package ch.vd.uniregctb.identification.contribuable.tooltip.adresse;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;

public class IdentificationAdresseTooltipManagerImpl implements IdentificationAdresseTooltipManager {

	private TiersDAO tiersDAO;
	private AdresseService adresseService;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	@Override
	@Transactional(readOnly = true)
	public void fillDerniereAdresseVaudoiseConnue(Long tiersId, IdentificationAdresseTooltipView view) {

		try {
			final Tiers tiers = tiersDAO.get(tiersId);
			final AdresseGenerique adresseGenerique = adresseService.getDerniereAdresseVaudoise(tiers, TypeAdresseFiscale.DOMICILE);
			if (adresseGenerique == null) {
				view.reset();
				return;
			}

			final AdresseEnvoiDetaillee adresseEnvoi = adresseService.getAdresseEnvoi(tiers, adresseGenerique.getDateDebut(), TypeAdresseFiscale.DOMICILE, false);
			Assert.notNull(adresseEnvoi);

			final String complements = adresseEnvoi.getComplement();
			final String rue = (adresseEnvoi.getRueEtNumero() == null ? null : adresseEnvoi.getRueEtNumero().getRueEtNumero());
			final String localite = (adresseEnvoi.getNpaEtLocalite() == null ? null : adresseEnvoi.getNpaEtLocalite().toString());
			final String pays = (adresseEnvoi.getPays() == null ? null : adresseEnvoi.getPays().getNomMinuscule());
			final AdresseGenerique.SourceType source = adresseGenerique.getSource().getType();

			view.init(rue, complements, localite, pays, source);
		}
		catch (Exception e) {
			view.init(e.getMessage());
		}
	}
}
