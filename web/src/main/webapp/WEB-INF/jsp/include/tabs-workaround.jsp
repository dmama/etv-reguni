<%-- Workaround du bug sur les tabs jQuery avec IE 6 et 7 --%>
<!--[if IE]>
	<script>
		$(function() {
			// Toutes les tabs sauf la première sont décalées bizarrement de un pixel vers le haut, on compense à la main...
			$('div.ui-tabs div.ui-tabs-panel').css('margin-top', '1px');
			$('div.ui-tabs div.ui-tabs-panel:first').css('margin-top', '0px');
		});
	</script>
<![endif]-->
