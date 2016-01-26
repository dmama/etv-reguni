<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<%--@elvariable id="command" type="ch.vd.uniregctb.entreprise.EntrepriseView"--%>

<unireg:setAuth var="autorisations" tiersId="${command.id}"/>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.edition.etats" />
	</tiles:put>
	<tiles:put name="fichierAide">
		<li>
			<a href="#" onClick="ouvrirAide('<c:url value="/docs/maj-civil-complement.pdf"/>');" title="AccessKey: a" accesskey="e">Aide</a>
		</li>
	</tiles:put>
	<tiles:put name="body">

		<unireg:bandeauTiers numero="${command.id}" showLinks="false" showComplements="false" showEvenementsCivils="false" showValidation="false" showAvatar="false"/>

		<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
		<fieldset>
			<legend><span><fmt:message key="label.etats.pm"/></span></legend>

			<c:if test="${autorisations.etatsPM}">
				<table border="0">
					<tr>
						<td>
							<unireg:linkTo name="Ajouter" title="Ajouter un etat d'entreprise" action="/entreprise/etats/add.do" params="{tiersId:${command.id}}" link_class="add"/>
						</td>
					</tr>
				</table>
			</c:if>

			<c:if test="${empty command.etats}">
				<fmt:message key="no.data" />
			</c:if>

			<c:if test="${not empty command.etats}">
				<display:table name="${command.etats}" id="etatPM" requestURI="visu.do" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
					<display:column titleKey="label.date.obtention">
						<unireg:regdate regdate="${etatPM.dateObtention}"/>
					</display:column>
					<display:column titleKey="label.type">
						<fmt:message key="option.etat.entreprise.${etatPM.type}"/>
					</display:column>
					<display:column titleKey="label.etats.type.generation">
						<fmt:message key="option.etat.entreprise.generation.${etatPM.generation}"/>
					</display:column>
					<display:column class="action">
						<c:if test="${(!etatPM.annule) && autorisations.etatsPM}">
							<c:if test="${etatPM.dernierElement}">
								<unireg:linkTo name="" action="/entreprise/etats/cancel.do" method="POST" params="{etatEntrepriseId:${etatPM.id}}" link_class="delete"
								               title="Annulation de l'état de l'entreprise" confirm="Voulez-vous vraiment annuler cet état de l'entreprise ?"/>
							</c:if>
						</c:if>
					</display:column>
				</display:table>

			</c:if>

		</fieldset>
		<c:set var="libelleBoutonRetour">
			<fmt:message key="label.bouton.retour"/>
		</c:set>
		<unireg:buttonTo method="get" action="/tiers/visu.do" params="{id:${command.id}}" name="${libelleBoutonRetour}"/>
	</tiles:put>
</tiles:insert>
