<%-- Workaround du bug IE6 qui ne tient pas compte de la transparence dans les images PNG --%>
<!--[if IE 6]>
<style type="text/css">
	.iepngfix {
		behavior: url(<c:url value="/css/x/iepngfix.htc"/>);
	}
</style>
<![endif]-->

<%-- Workaround du bug sur les tabs jQuery avec IE 6 et 7 --%>
<!--[if IE]>
<style type="text/css">
	/**
	 * Toutes les tabs sauf la première sont décalées bizarrement de un pixel vers le haut, on compense à la main...
	 */
	#tabs-1, #tabs-2, #tabs-3, #tabs-4, #tabs-5, #tabs-6, #tabs-7, #tabs-8, #tabs-9, #tabs-10 {
		margin-top: 1px;
	}
</style>
<![endif]-->
