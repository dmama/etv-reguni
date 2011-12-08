package ch.vd.uniregctb.type;

public enum ModeleFeuille {

	ANNEXE_200("200") {
		@Override
		public String getDescription() {
			return "Déclaration Hors canton immeuble";
		}
	},
	ANNEXE_210("210") {
		@Override
		public String getDescription() {
			return "déclaration d'impôt ordinaire complète";
		}
	},
	ANNEXE_220("220") {
		@Override
		public String getDescription() {
			return "Annexe 1";
		}
	},
	ANNEXE_230("230") {
		@Override
		public String getDescription() {
			return "Annexe 2 et 3";
		}
	},
	ANNEXE_240("240") {
		@Override
		public String getDescription() {
			return "Annexe 4 et 5";
		}
	},
	ANNEXE_250("250") {
		@Override
		public String getDescription() {
			return "Déclaration Vaud Tax";
		}
	},

	ANNEXE_270("270") {
		@Override
		public String getDescription() {
			return "Déclaration Dépense";
		}
	},

	ANNEXE_310("310") {
		@Override
		public String getDescription() {
			return "Annexe 1-1";
		}
	},
	ANNEXE_320("320") {
		@Override
		public String getDescription() {
			return "Annexe 7";
		}
	},
	ANNEXE_330("330") {
		@Override
		public String getDescription() {
			return "Annexe 2 et 3";
		}
	};


	private String code;

	ModeleFeuille(String c) {
		code = c;
	}

	public String getCode() {
		return code;
	}

	public static ModeleFeuille fromCode(String c) {
		if (c != null) {
			for (ModeleFeuille modele : ModeleFeuille.values()) {
				if (c.equalsIgnoreCase(modele.code)) {
					return modele;
				}
			}

		}
		return null;
	}


	/**
	 * @return une description du modèle de feuille
	 */
	public abstract String getDescription();

}
