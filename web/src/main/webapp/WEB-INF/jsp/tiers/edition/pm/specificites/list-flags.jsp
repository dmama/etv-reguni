<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.edition.specificites.fiscales"/>
	</tiles:put>
	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
		<unireg:bandeauTiers numero="${pmId}" showAvatar="false" showValidation="false" showEvenementsCivils="false" showLinks="false" titre="Caractéristiques du contribuable"/>

		<fieldset>
			<legend><span><fmt:message key="label.specificites.${group}"/></span></legend>

			<table border="0">
				<tr><td>
					<unireg:linkTo name="Ajouter" action="/flag-entreprise/add.do" method="get" params="{pmId:${pmId},group:'${group}'}" title="Ajouter une spécificité" link_class="add noprint"/>
				</td></tr>
			</table>

			<c:if test="${not empty flags}">
				<display:table name="flags" id="flag" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator" requestURI="/flag-entreprise/edit-list.do" sort="list">
					<display:column sortable="true" titleKey="label.date.debut" sortProperty="dateDebut" style="width: 20%;">
						<unireg:regdate regdate="${flag.dateDebut}"/>
					</display:column>
					<display:column sortable="true" titleKey="label.date.fin" sortProperty="dateFin" style="width: 20%;">
						<unireg:regdate regdate="${flag.dateFin}"/>
					</display:column>
					<display:column titleKey="label.type">
						<fmt:message key="option.flag.entreprise.${flag.type}"/>
					</display:column>
					<display:column class="action" style="width: 10%;">
						<c:if test="${!flag.annule}">
							<c:if test="${flag.dateFin == null}">
								<unireg:linkTo name="" action="/flag-entreprise/edit.do" method="GET" params="{flagId:${flag.id}}" link_class="edit" title="Fermeture de spécificité" />
							</c:if>
							<unireg:linkTo name="" action="/flag-entreprise/cancel.do" method="POST" params="{flagId:${flag.id}}" link_class="delete"
							               title="Annulation de spécificité" confirm="Voulez-vous vraiment annuler cette spécificité ?"/>
						</c:if>
					</display:column>
				</display:table>
			</c:if>

		</fieldset>

		<!-- Scripts -->

		<!-- Debut Bouton -->
		<table>
			<tr>
				<td>
					<unireg:buttonTo name="Retour" action="/tiers/visu.do" method="get" params="{id:${pmId}}"/>
				</td>
			</tr>
		</table>
		<!-- Fin Bouton -->

	</tiles:put>

</tiles:insert>