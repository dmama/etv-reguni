<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.mouvements.dossiers.masse.consulter.full"/></tiles:put>
  	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/consult_mouvementdossier.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
		<fieldset>
			<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
		    <form:form method="post" action="rechercher-pour-consultation.do" commandName="criteria">
				<form:errors cssClass="error"/>
				<jsp:include page="form.jsp"/>
			</form:form>
		</fieldset>

		<c:if test="${found != null}">
			<display:table name="found.results" id="mvt" pagesize="25" requestURI="/mouvement/masse/consulter.do" class="display_table" sort="external" size="found.resultSize" partialList="true" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
				<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.aucun.mouvement.trouve" /></span></display:setProperty>
				<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.mouvement.trouve" /></span></display:setProperty>
				<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.mouvements.trouves" /></span></display:setProperty>
				<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.mouvements.trouves" /></span></display:setProperty>

				<display:column sortable="true" titleKey="label.numero.tiers" sortName="contribuable.numero">
					<unireg:numCTB numero="${mvt.contribuable.numero}" />
				</display:column>
				<display:column sortable="true" titleKey="label.date.mouvement" sortName="dateMouvement">
                    <fmt:formatDate value="${mvt.dateMouvement}" pattern="dd.MM.yyyy"/>
				</display:column>
				<display:column titleKey="label.type.mouvement" >
				    <fmt:message key="option.type.mouvement.${mvt.typeMouvement}"/>
				</display:column>
				<display:column sortable="true" titleKey="label.etat.mouvement" sortName="etat">
				    <fmt:message key="option.etat.mouvement.${mvt.etatMouvement}"/>
				</display:column>
				<c:if test="${montrerInitiateur}">
                    <display:column titleKey="label.collectivite.administrative">
                    	<c:out value="${mvt.collectiviteAdministrative}"/>
                    </display:column>
                </c:if>
				<display:column titleKey="label.destination.utilisateur" >
					<c:out value="${mvt.destinationUtilisateur}" />
				</display:column>

                <display:column style="action">
                    <a href="#" class="detail" title="Détail d'un mouvement" onclick="return open_details_mouvement(${mvt.id});">&nbsp;</a>
                    <unireg:consulterLog entityNature="MouvementDossier" entityId="${mvt.id}"/>
                </display:column>

			</display:table>
		</c:if>

   </tiles:put>
   
</tiles:insert>
