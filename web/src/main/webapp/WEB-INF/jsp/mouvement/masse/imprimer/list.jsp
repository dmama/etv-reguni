<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.mouvements.dossiers.masse.imprimer.bordereaux.full"/></tiles:put>
  	<tiles:put name="fichierAide">
	    <li>
		    <a href="#" onClick="ouvrirAide('<c:url value='/docs/imprim-bordereau-envoidossier.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	    </li>
	</tiles:put>
  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>

        <display:table name="command.bordereaux" id="bordereau" pagesize="25" requestURI="/mouvement/bordereau/a-imprimer.do" class="display_table" sort="list">

            <c:if test="${command.montreExpediteur}">
                <display:column sortable="true" titleKey="label.collectivite.administrative">
                    <c:out value="${bordereau.nomCollAdmInitiatrice}"/>
                </display:column>
            </c:if>
            <display:column sortable="true" titleKey="label.destination" >
                <c:if test="${bordereau.type == 'ReceptionDossier'}">
                    <fmt:message key="label.archives"/>
                </c:if>
                <c:if test="${bordereau.type == 'EnvoiDossier'}">
                    <c:out value="${bordereau.nomCollAdmDestinataire}"/>
                </c:if>
            </display:column>
            <display:column titleKey="label.nombre.mouvements">
                <c:out value="${bordereau.nombreMouvements}"/>
            </display:column>

            <display:column style="action">
                <a href="detail-avant-impression.do?src=${bordereau.idCollAdmInitiatrice}/${bordereau.noCollAdmInitiatrice}&dest=${bordereau.idCollAdmDestinataire}/${bordereau.noCollAdmDestinataire}&type=${bordereau.type}" class="detail" title="Détail d'un bordereau">&nbsp;</a>
            </display:column>

        </display:table>

   </tiles:put>

</tiles:insert>