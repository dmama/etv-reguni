/**
 * Active les tooltips ajax sur tous les liens de la page ayant la classe 'jTip'.
 *
 * Exemple de tooltip ajax :
 *
 *     <a href="#" title="<c:url value="/htm/forPrincipalActif.htm?width=375"/>" class="jTip">?</a>
 */
function activate_ajax_tooltips() {
	$(".jTip").tooltip({
		items: "[title]",
		content: function(response) {
			var url = $(this).attr("title");
			$.get(url, response);
			return "Chargement...";
		}
	});
}

/**
  * Active les tooltips statiques sur tous les liens de la page ayant la classe 'staticTip'.
 *
 * Exemple de tooltip statique :
 *
 *     <a href="#tooltip" class="staticTip" id="link123">some link</a>
 *     <div id="link123-tooltip" style="display:none;">
 *         tooltip content goes here
 *     </div>
 */
function activate_static_tooltips() {
	$(".staticTip").tooltip({
		items: "[id]",
		content: function(response) {
			// on détermine l'id de la div qui contient le tooltip à afficher
			var id = $(this).attr("id") + "-tooltip";
			id = id.replace(/\./g, '\\.'); // on escape les points

			// on récupère la div et on affiche son contenu
			var div = $("#" + id);
			return div.attr("innerHTML");
		}
	});
}
