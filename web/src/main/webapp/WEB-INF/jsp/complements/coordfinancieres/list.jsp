<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<%--@elvariable id="tiersId" type="java.lang.Long"--%>
<unireg:setAuth var="autorisations" tiersId="${tiersId}"/>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.edition.complements.coordfinancieres" />
	</tiles:put>

	<tiles:put name="fichierAide">
		<li>
			<a href="#" onClick="ouvrirAide('<c:url value='/docs/maj-civil-complement.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
		</li>
	</tiles:put>

	<tiles:put name="body">
		<unireg:bandeauTiers numero="${tiersId}" showAvatar="true" showLinks="false"/>

		<fieldset>
			<legend><span><fmt:message key="label.complement.coordFinancieres" /></span></legend>
			<unireg:linkTo name="Ajouter" title="Ajouter des coordonnées financières" action="/complements/coordfinancieres/add.do" params="{tiersId:${tiersId}}" link_class="add margin_right_10"/>

			<unireg:nextRowClass reset="1"/>
			<display:table name="coordonneesFinancieres" id="coordoonnees" pagesize="10" class="display" decorator="ch.vd.unireg.decorator.TableEntityDecorator">
				<display:setProperty name="basic.empty.showtable" value="false"/>
				<display:setProperty name="basic.msg.empty_list" value=""/>
				<display:setProperty name="paging.banner.all_items_found" value=""/>
				<display:setProperty name="paging.banner.some_items_found" value=""/>
				<display:setProperty name="paging.banner.one_item_found" value=""/>
				<display:setProperty name="paging.banner.no_items_found" value=""/>
				<display:column sortable="true" titleKey="label.date.debut" sortProperty="dateDebut">
					<unireg:regdate regdate="${coordoonnees.dateDebut}"/>
				</display:column>
				<display:column sortable="true" titleKey="label.date.fin" sortProperty="dateFin">
					<unireg:regdate regdate="${coordoonnees.dateFin}"/>
				</display:column>
				<display:column sortable="false" titleKey="label.complement.numeroCompteBancaire">
					<c:out value="${coordoonnees.iban}"/>
				</display:column>
				<display:column sortable="false" titleKey="label.complement.titulaireCompte">
					<c:out value="${coordoonnees.titulaireCompteBancaire}"/>
				</display:column>
				<display:column sortable="false" titleKey="label.complement.bicSwift">
					<c:out value="${coordoonnees.adresseBicSwift}"/>
				</display:column>
				<display:column class="action">
					<c:if test="${!coordoonnees.annule}">
						<c:if test="${coordoonnees.dateFin == null}">
							<unireg:linkTo name="" action="/complements/coordfinancieres/edit.do" method="GET" params="{id:${coordoonnees.id}}" link_class="edit" title="Edition de ces coordonnées financières" />
						</c:if>
						<unireg:linkTo name="" action="/complements/coordfinancieres/cancel.do" method="POST" params="{id:${coordoonnees.id}}" link_class="delete"
						               title="Annulation de for" confirm="Voulez-vous vraiment annuler ces coordonnées financières ?"/>
					</c:if>
					<unireg:consulterLog entityNature="CoordonneesFinancieres" entityId="${coordoonnees.id}"/>
				</display:column>
			</display:table>
		</fieldset>

		<unireg:buttonTo method="get" action="/tiers/visu.do" params="{id:${tiersId}}" name="label.bouton.retour"/>

	</tiles:put>
</tiles:insert>
