<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%@ include file="/WEB-INF/jsp/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title">Affichage d'un graphique personnalisé</tiles:put>

  	<tiles:put name="head">
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/sprintf-0.6.js"/>"></script>
  	</tiles:put>

  	<tiles:put name="body">

		<fieldset class="ui-widget-content">

			<label for="graphType">Type de graphique :</label>
			<select id="graphType">
				<option value="load">charge</option>
			</select>
			<br/>

			<label>Filtres :</label>
			<div class="breakout">
				<ol id="filters">(aucun)</ol>
				<select id="filterSelect" style="display:inline-block;">
				</select>
				<input type="button" id="addFilter" value="Ajouter"/><br/>
			</div>
			<br/>

			<label for="dateDebut">Du :</label>
			<input type="text" id="dateDebut">au</input>
			<input type="text" id="dateFin"></input>
			<br/>

			<label for="resolution">Résolution :</label>
			<select id="resolution">
				<option value="MINUTE">à la minute</option>
				<option value="FIVE_MINUTES">cinq minutes</option>
				<option value="FIFTEEN_MINUTES" selected="selected">au quart d'heure</option>
				<option value="HOUR">une heure</option>
				<option value="DAY">un jour</option>
			</select>
			<br/>

			<label for="width">Largeur / hauteur :</label>
			<input type="text" id="width" value="auto">/</input>
			<input type="text" id="height" value="auto">pixels</input>
			<br/>

			<label>Critères de séparation :</label>
			<div class="breakout">
				<ol id="criteria">(aucun)</ol>
				<select id="criterionSelect" style="display:inline-block;">
				</select>
				<input type="button" id="addCriterion" value="Ajouter"/><br/>
			</div>

		</fieldset><br/>

		<input id="show" type="button" value="Afficher"/> <span id="message" ></span>

        <div>
        	<img id="graph" src=""/>
        </div>

		<script>
			$(function() {

				// get the dimensions info (ajax call)
			    var dimensionsValues = [];
			    $.get('dimensions.do', function(dimensions) {
			    	for (var i in dimensions) {
			    		var dim = dimensions[i];
			    		// add dimensions to needed select boxes
			    		$('<option value="' + dim.type + '">par ' + dim.label + '</option>').appendTo('#filterSelect');
			    		$('<option value="' + dim.type + '">par ' + dim.label + '</option>').appendTo('#criterionSelect');
			    		dimensionsValues[dim.type] = dim.values;
			    	}
			    });

				// init the date pickers
			    $('#dateDebut').datepicker();
			    $('#dateFin').datepicker();

			    var now = new Date();
			    var today = new Date(now.getFullYear, now.getMonth(), now.getDate(), 0, 0, 0, 0);
			    $('#dateDebut').datepicker('setDate', today);

				$('#addFilter').click(function() {
					$("#filters:contains('aucun')").html('');
					var text = $('#filterSelect option:selected').text();
					var dim = $('#filterSelect').val();
					var filterId = 'filter' + dim;
					var selectHtml = '<select>'+ dimValuesToOptions(dimensionsValues[dim]) + '</select>';
					$('<li id="' + filterId + '">' + text + ' = ' + selectHtml + ' (<a href="#">supprimer</a>)</li>').appendTo('#filters');
					$('#' + filterId + ' a').click(function() {
						$('#' + filterId).remove();
						return false;
					});
				});

				$('#addCriterion').click(function() {
					$("#criteria:contains('aucun')").html('');
					var text = $('#criterionSelect option:selected').text();
					var critId = 'crit' + $('#criterionSelect').val();
					$('<li id="' + critId + '">' + text + ' (<a href="#">supprimer</a>)</li>').appendTo('#criteria');
					$('#' + critId + ' a').click(function() {
						$('#' + critId).remove();
						return false;
					});
				});

				$('#show').click(function() {
					$('#message').text('Veuillez patienter...');

					var from = formatDate($('#dateDebut').datepicker('getDate'));
					var to = formatDate($('#dateFin').datepicker('getDate'));
					var res = $('#resolution').val();
					var width = $('#width').val().replace(/[^0-9]*/g, '');
					var height = $('#height').val().replace(/[^0-9]*/g, '');

					var filters = [];
					$('#filters li').each(function(index, element) {
						var dim = $(element).attr('id').substring(6);
						var val = $("select", element).val();
						filters.push(dim + ':' + val);
					});
					var filters = filters.join(",");

					var criteria = [];
					$('#criteria li').each(function(index, element) {
						var dim = $(element).attr('id').substring(4);
						criteria.push(dim);
					});
					var criteria = criteria.join(",");

					$('#graph').attr('src', 'load.do?filters=' + filters +
						'&criteria=' + criteria +
						'&from=' + from  +
						'&to=' + to +
						'&resolution=' + res +
						'&width=' + width +
						'&height=' + height);
					return false;
				});

				$('#graph').load(function() {
					$('#message').text('');
				});
			});

			function formatDate(date) {
				if (!date) {
					return '';
				}
				return sprintf('%04d-%02d-%02dT%02d:%02d:%02d', date.getFullYear(), (date.getMonth() + 1), date.getDate(),
					date.getHours(), date.getMinutes(), date.getSeconds());
			}

			function dimValuesToOptions(values) {
				var options = '';
				for (var i in values) {
					var v = values[i];
					options += '<option value="' + v.id + '">' + v.name + '</option>';
				}
				return options;
			}
		</script>
  	</tiles:put>

</tiles:insert>