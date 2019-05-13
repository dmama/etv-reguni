<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/templateSuperGra.jsp">
	<tiles:put name="menu" type="String"/>

	<tiles:put name="title" type="String">*** Mode Supergra ***</tiles:put>

	<%--@elvariable id="mc2pp" type="ch.vd.unireg.supergra.view.mc2ppView"--%>

	<tiles:put name="actions" type="String">
	</tiles:put>

	<tiles:put name="body" type="String">

		<table border="0" class="supergra"><tr valign="top">
		<td>
			<h3>Transformation du ménage commun n°${mc2pp.id} en personne physique&nbsp;<a href="<c:url value="/tiers/visu.do?id=${mc2pp.id}"/>">(retour au mode normal)</a></h3>
			<br/>

			<%-- Affichage du formulaire de transformation de l'entité --%>
			<form:form modelAttribute="mc2pp" method="post" action="mc2pp.do">
				<input type="hidden" name="id" value="${mc2pp.id}">

				<table class="transform">
					<tr>
						<td class="from"><unireg:bandeauTiers numero="${mc2pp.id}" showAvatar="true" showLinks="false" showEvenementsCivils="false" showValidation="false"/></td>
						<td class="middle"><div class="transformArrow"/></td>
						<td class="to"><unireg:bandeauTiers numero="${mc2pp.id}" forceAvatar="SEXE_INCONNU" showAvatar="true" showLinks="false" showEvenementsCivils="false" showValidation="false"/></td>
					</tr>
					<tr>
						<td colspan="3">
							<table class="params">
								<tr>
									<td>Numéro d'individu de la personne physique</td>
									<td>
										<form:input path="indNo" id="indNo"/>
										<span class="mandatory">*</span>
										<form:errors path="indNo" cssClass="error"/>
									</td>
								</tr>
								<tr>
									<td><input type="submit" id="transformer" value="Transformer"/></td>
									<td><unireg:buttonTo name="Annuler" action="/supergra/entity/show.do" method="GET" params="{id:${mc2pp.id},class:'Tiers'}"/></td>
								</tr>
							</table>
						</td>
					</tr>
					<tr>
						<td colspan="3" style="text-align: center">
							<div class="legend">
								En cliquant sur le bouton ci-dessus, les opérations suivantes seront effectuées :
								<ul style="padding-left: 2em">
									<li>suppression de toutes les situations de famille du ménage-commun</li>
									<li>suppression de tous les rapports d'appartenance ménage du ménage-commun</li>
									<li>transformation du ménage-commun en personne physique</li>
									<li>association de la personne physique avec l'individu</li>
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
				$('#transformer').click(function() {
					return confirm("Etes-vous sûr de vouloir transformer ce tiers ?");
				});
			});
		</script>

	</tiles:put>
</tiles:insert>
