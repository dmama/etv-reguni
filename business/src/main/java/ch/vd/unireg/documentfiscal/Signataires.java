package ch.vd.unireg.documentfiscal;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.interfaces.service.ServiceSecuriteService;
import ch.vd.unireg.interfaces.service.host.Operateur;

/**
 * Classe contenant les informations des signataires d'un document émis
 */
public final class Signataires {

	/**
	 * Caractéristique d'un signataire
	 */
	public static final class VisaFonction {
		private final String visa;
		private final String libelleFonction;
		private NomPrenom nomPrenomOperateur;       // lazy-init

		public VisaFonction(String visa, String libelleFonction) {
			this.visa = StringUtils.trimToNull(visa);
			this.libelleFonction = StringUtils.trimToNull(libelleFonction);
		}

		public String getVisa() {
			return visa;
		}

		@NotNull
		public String getLibelleFonction() {
			return libelleFonction == null ? StringUtils.EMPTY : libelleFonction;
		}

		@NotNull
		public NomPrenom getNomPrenomOperateur(ServiceSecuriteService serviceSecurite) {
			if (nomPrenomOperateur == null) {
				fetchNomPrenomOperateur(serviceSecurite);
			}
			return nomPrenomOperateur;
		}

		public boolean isEmpty() {
			return visa == null && libelleFonction == null;
		}

		private synchronized void fetchNomPrenomOperateur(ServiceSecuriteService serviceSecurite) {
			if (nomPrenomOperateur == null) {
				if (visa != null) {
					final Operateur operateur = serviceSecurite.getOperateur(visa);
					if (operateur == null) {
						throw new ObjectNotFoundException("Opérateur '" + visa + "'");
					}
					nomPrenomOperateur = new NomPrenom(operateur.getNom(), operateur.getPrenom());
				}
				else {
					nomPrenomOperateur = NomPrenom.VIDE;
				}
			}
		}
	}

	private final List<VisaFonction> signataires;

	public Signataires(List<VisaFonction> signataires) {
		this.signataires = signataires == null ? Collections.emptyList() : Collections.unmodifiableList(signataires);
	}

	public List<VisaFonction> getSignataires() {
		return signataires;
	}
}
