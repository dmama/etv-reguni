<%-- [UNIREG-3300] Workaround du bug IE qui fait la couleur du fond des fieldsets dépasse sur le haut --%>
<!--[if IE]>
<style type="text/css">
	@media screen {
		fieldset {
			display: block;
			position: relative;
			margin-top: 18px ! important;
			padding-top: 15px ! important;
		}
		legend {
			position: absolute;
			top: -0.8em;
		}
	}
</style>
<![endif]-->