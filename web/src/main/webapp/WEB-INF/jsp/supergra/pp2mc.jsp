<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/templateSuperGra.jsp">
	<tiles:put name="menu" type="String"/>

	<tiles:put name="title" type="String">*** Mode Supergra ***</tiles:put>

	<%--@elvariable id="pp2mc" type="ch.vd.uniregctb.supergra.view.Pp2McView"--%>

	<tiles:put name="actions" type="String">
	</tiles:put>

	<tiles:put name="body" type="String">

		<table border="0" class="supergra"><tr valign="top">
		<td>
			<h3>Transformation de la personne physique n°${pp2mc.id} en ménage commun&nbsp;<a href="<c:url value="/tiers/visu.do?id=${pp2mc.id}"/>">(retour au mode normal)</a></h3>
			<br/>

			<%-- Affichage du formulaire de transformation de l'entité --%>
			<form:form commandName="pp2mc" method="post" action="pp2mc.do">
				<input type="hidden" name="id" value="${pp2mc.id}">

				<table class="transform">
					<tr>
						<td class="from"><unireg:bandeauTiers numero="${pp2mc.id}" showAvatar="true" showLinks="false" showEvenementsCivils="false" showValidation="false"/></td>
						<td class="middle"><div class="transformArrow"/></td>
						<td class="to"><unireg:bandeauTiers numero="${pp2mc.id}" forceAvatar="MC_SEXE_INCONNU" showAvatar="true" showLinks="false" showEvenementsCivils="false" showValidation="false"/></td>
					</tr>
					<tr>
						<td colspan="3">
							<table class="params">
								<tr>
									<td>Date d'ouverture du rapport d'appartenance ménage</td>
									<td>
										<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
											<jsp:param name="path" value="dateDebut" />
											<jsp:param name="id" value="dateDebut" />
											<jsp:param name="mandatory" value="true" />
										</jsp:include>
									</td>
								</tr>
								<tr>
									<td>Date de fermeture du rapport d'appartenance ménage</td>
									<td>
										<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
											<jsp:param name="path" value="dateFin" />
											<jsp:param name="id" value="dateFin" />
										</jsp:include>
									</td>
								</tr>
								<tr>
									<td>Numéro de contribuable principal</td>
									<td>
										<form:input path="idPrincipal" id="idPrincipal"/>
										<button id="searchPrincipal">...</button>
										<span class="mandatory">*</span>
										<form:errors path="idPrincipal" cssClass="error" delimiter=". "/>
									</td>
								</tr>
								<tr>
									<td>Numéro de contribuable secondaire</td>
									<td>
										<form:input path="idSecondaire" id="idSecondaire"/>
										<button id="searchSecondaire">...</button>
										<form:errors path="idSecondaire" cssClass="error" delimiter=". "/>
									</td>
								</tr>
								<tr>
									<td><input type="submit" id="transformer" value="Transformer"/></td>
									<td><unireg:buttonTo name="Annuler" action="/supergra/entity/show.do" method="GET" params="{id:${pp2mc.id},class:'Tiers'}"/></td>
								</tr>
							</table>
						</td>
					</tr>
					<tr>
						<td colspan="3" style="text-align: center">
							<div class="legend">
								En cliquant sur le bouton ci-dessus, les opérations suivantes seront effectuées :
								<ul style="padding-left: 2em">
									<li>suppression de toutes les situations de famille de la personne physique</li>
									<li>suppression de tous les rapports d'appartenance ménage de la personne physique</li>
									<li>suppression de toutes les identifications de la personne physique</li>
									<li>suppression de toutes les restrictions/autorisations d'accès sur la personne physique</li>
									<li>transformation de la personne physique en ménage-commun</li>
									<li>ajout d'un rapport d'appartenance ménage entre le nouveau ménage-commun et le contribuable principal</li>
									<li>ajout d'un rapport d'appartenance ménage entre le nouveau ménage-commun et le contribuable secondaire (si spécifié)</li>
								</ul>
							</div>
						</td>
					</tr>
				</table>

			</form:form>

		</td>
		</tr></table>

		<script type="text/javascript" language="Javascript">
			$(function() {
				$('#searchPrincipal').click(function() {
					Dialog.open_tiers_picker($(this), function(id) {
						$('#idPrincipal').val(id);
					});
					return false;
				});
				$('#searchSecondaire').click(function() {
					Dialog.open_tiers_picker($(this), function(id) {
						$('#idSecondaire').val(id);
					});
					return false;
				});
				$('#transformer').click(function() {
					return confirm("Etes-vous sûr de vouloir transformer ce tiers ?");
				});
			});
		</script>

	</tiles:put>
</tiles:insert>
