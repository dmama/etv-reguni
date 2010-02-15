<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.mouvements.dossiers.masse.imprimer.bordereaux.full"/></tiles:put>
  	<tiles:put name="body">
		<c:set var="ligneTableau" value="${1}" scope="request" />
	    <c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />

        <display:table name="command.bordereaux" id="bordereau" pagesize="25" requestURI="/mouvement/imprimer-bordereaux.do" class="display_table" sort="list">

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
                <a href="detail-bordereau.do?height=600&width=900&src=${bordereau.idCollAdmInitiatrice}/${bordereau.noCollAdmInitiatrice}&dest=${bordereau.idCollAdmDestinataire}/${bordereau.noCollAdmDestinataire}&type=${bordereau.type}&TB_iframe=true&modal=true" class="detail thickbox" title="Détail d'un bordereau">&nbsp;</a>
            </display:column>

        </display:table>

		<script type="text/javascript">
			function AppSelect_OnChange(select) {
				var value = select.options[select.selectedIndex].value;
				if ( value && value !== '') {
					//window.open(value, '_blank') ;
					window.location.href = value;
				}
			}
		</script>

   </tiles:put>

</tiles:insert>