<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%@ include file="/WEB-INF/jsp/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title">Affichage d'un graphique personnalisé</tiles:put>

  	<tiles:put name="head">
		<script type="text/javascript" language="Javascript" src="<c:url value="/js/sprintf-0.6.js"/>"></script>
  	</tiles:put>

  	<tiles:put name="body">

		<fieldset class="ui-widget-content">

			<label for="environment">Environnement :</label>
			<select id="environment">
				<c:forEach items="${environments}" var="env">
					<option value="<c:out value="${env.id}"/>"><c:out value="${env.name}"/></option>
				</c:forEach>
			</select>
			<br/>

			<label for="graphType">Type de graphique :</label>
			<select id="graphType">
				<option value="load">charge</option>
			</select>
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
				<select id="criterion" style="display:inline-block;">
					<option value="CALLER">par utilisateur</option>
					<option value="METHOD">par méthode</option>
					<option value="SERVICE">par service</option>
					<option value="ENVIRONMENT">par environnement</option>
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
			    $('#dateDebut').datepicker();
			    $('#dateFin').datepicker();

			    var now = new Date();
			    var today = new Date(now.getFullYear, now.getMonth(), now.getDate(), 0, 0, 0, 0);
			    $('#dateDebut').datepicker('setDate', today);

				$('#graph').load(function() {
					$('#message').text('');
				});

				$('#addCriterion').click(function() {
					$("ol:contains('aucun')").html('');
					var text = $('#criterion option:selected').text();
					var crit = $('#criterion').val();
					$('<li id="' + crit + '">' + text + ' (<a href="#">supprimer</a>)</li>').appendTo('#criteria');
					$('#' + crit + ' a').click(function() {
						$('#' + crit).remove();
						return false;
					});
				});
				
				$('#show').click(function() {
					$('#message').text('Veuillez patienter...');
					var env_id = $('#environment').val();
					var from = formatDate($('#dateDebut').datepicker('getDate'));
					var to = formatDate($('#dateFin').datepicker('getDate'));
					var res = $('#resolution').val();
					var width = $('#width').val().replace(/[^0-9]*/g, '');
					var height = $('#height').val().replace(/[^0-9]*/g, '');

					var criteria = [];
					$('#criteria li').each(function(index, element) {
						criteria.push($(element).attr('id'));
					});
					var criteria = criteria.join(",");

					$('#graph').attr('src', 'load.do?env=' + env_id +
						'&criteria=' + criteria +
						'&from=' + from  +
						'&to=' + to +
						'&resolution=' + res +
						'&width=' + width +
						'&height=' + height);
					return false;
				});
			});

			function formatDate(date) {
				if (!date) {
					return '';
				}
				return sprintf('%04d-%02d-%02dT%02d:%02d:%02d', date.getFullYear(), (date.getMonth() + 1), date.getDate(),
					date.getHours(), date.getMinutes(), date.getSeconds());
			}
		</script>
  	</tiles:put>

</tiles:insert>