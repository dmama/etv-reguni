<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="head">
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/original/d3.js"/>"></script>
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/original/viz.js"/>"></script>
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/original/d3-graphviz-0.1.2.js"/>"></script>
		<style>
			#graph {
				width: 100%;
			}
			div.tooltip {
				position: absolute;
				/*text-align: center;*/
				width: 350px;
				height: 50px;
				padding: 2px;
				font: 12px sans-serif;
				/*background: lightsteelblue;*/
				border: 0;
				pointer-events: none;
			}
		</style>
	</tiles:put>

	<tiles:put name="title">${title}</tiles:put>

	<tiles:put name="body">

		<p style="text-align: center; color:red;">Attention: cette page est une aide pour les développeurs de Unireg. Il ne s'agit en aucune manière d'une page officielle, et aucun support n'est prévu.</p>

		<div id="info"></div>
		<div id="dot" style="display: none">${dot}</div>
		<div id="graph" style="text-align: center;"></div>

		<script>
			// voir https://github.com/magjac/d3-graphviz

			var dotSrc = $('#dot').text();

			// on génère le graph
			try {
				var graphviz = d3.select("#graph").graphviz();
				graphviz.renderDot(dotSrc);
				nodes = d3.selectAll('.node');
				edges = d3.selectAll('.edge');

				// gestion du resizing du graph
				let svg = d3.select("#graph").select('svg');
				let chartDiv = document.getElementById("graph");
				window.addEventListener("resize", resizeSvg);
				resizeSvg();

				// désactivation des tooltips par défaut
				d3.selectAll('.node, .edge, .graph').select('title').text("");

				// on sélectionne le noeud qui correspond au contribuable ou à l'immeuble spécifié en paramètre de la page
				selectNode('${selected}');

				// création des tooltips custom
				var tooltipDiv = d3.select("body").append("div")
					.attr("id", "tooltip-div")
					.attr("class", "tooltip")
					.style("opacity", 0);


				nodes
					.on("click", function (node) {
						selectNode(node.key);
					})
					.on("mouseover", function (d) {
						showTooltip(tooltipDiv, d.key);
					})
					.on("mouseout", function () {
						hideTooltip(tooltipDiv);
					});

				edges
					.on("click", function (edge) {
						selectEdge(edge);
					})
					.on("mouseover", function (d) {
						showTooltip(tooltipDiv, d.attributes.id);
					})
					.on("mouseout", function () {
						hideTooltip(tooltipDiv);
					});

				function resizeSvg() {
					var width = chartDiv.clientWidth;
					// var height = chartDiv.clientHeight;

					// Use the extracted size to set the size of an SVG element.
					svg
						.attr("width", width);
					// .attr("height", height);
				}

				/**
				 * une fonction pour mettre en gras les noeuds et liens cliqués
				 *
				 * voir https://bl.ocks.org/magjac/28a70231e2c9dddb84b3b20f450a215f
				 * voir https://stackoverflow.com/questions/41098296/how-to-highlight-the-selected-node-in-d3js
				 */
				function selectNode(nodeKey) {
					$('#info').load('<c:url value="/registrefoncier/immeuble/details.do"/>?elementKey=' + nodeKey);
					nodes.style("font-weight", function(p) {
						return p.key === nodeKey ? "bold" : "normal";
					});
					edges.style("font-weight", function(p) {
						// la clé du lien contient les ids des deux noeuds liés
						return p.key.indexOf(nodeKey) > -1 ? "bold" : "normal";
					});
					edges.attr("stroke-width", function(p) {
						return p.key.indexOf(nodeKey) > -1 ? 2 : 1;
					});
				}

				/**
				 * une fonction pour mettre en gras le lien cliqué
				 */
				function selectEdge(edge) {
					var edgeKey = edge.key;
					$('#info').load('<c:url value="/registrefoncier/immeuble/details.do"/>?elementKey=' + edge.attributes.id);
					nodes.style("font-weight", function() {
						// on met tous les noeuds à la normale
						return "normal";
					});
					edges.style("font-weight", function(p) {
						// la clé du lien contient les ids des deux noeuds liés
						return p.key.indexOf(edgeKey) > -1 ? "bold" : "normal";
					});
					edges.attr("stroke-width", function(p) {
						return p.key.indexOf(edgeKey) > -1 ? 2 : 1;
					});
				}

				function showTooltip(div, elementKey) {
					div.transition()
						.duration(200)
						.style("opacity", 1);
					$('#tooltip-div').load('<c:url value="/registrefoncier/immeuble/details.do"/>?elementKey=' + elementKey);
					div.style("left", (d3.event.pageX + 20) + "px")
						.style("top", (d3.event.pageY + 20) + "px");
				}

				function hideTooltip(div) {
					div.transition()
						.duration(500)
						.style("opacity", 0);
				}
			}
			catch (error) {
				$('#info').html("<br><span style='font-weight: bold'>Nous sommes désolés, votre navigateur ne semble pas supporter cette page.</span> Si vous utilisez Internet Explorer, essayez avec Firefox." +
					"<br><br><span class='error'>Erreur = " + error + "</span>");
			}

		</script>
	</tiles:put>
</tiles:insert>
