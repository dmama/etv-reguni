<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/templateDialog.jsp">
  	<tiles:put name="title"></tiles:put>
  	<tiles:put name="body">

        <c:if test="${empty content}">
            <fmt:message key="label.inbox.empty.attente"/>
        </c:if>
        <c:if test="${not empty content}">
        	<table>
        		<tr>
        			<th><fmt:message key="label.action"/></th>
        			<th><fmt:message key="label.inbox.date.demande"/></th>
        			<th><fmt:message key="label.inbox.description"/></th>
        			<th><fmt:message key="label.inbox.etat"/></th>
        			<th><fmt:message key="label.inbox.temps.execution"/></th>
        			<th><fmt:message key="label.inbox.progression"/></th>
        		</tr>
        		<unireg:nextRowClass reset='1'/>
        		<c:forEach var="job" items="${content}">
                    <tr class='<unireg:nextRowClass/>'>
                    	<td>
							<c:if test="${job.interrupted}">&nbsp;</c:if>
							<c:if test="${!job.interrupted}">
								<a href='#' onclick="javascript:stopJobEnAttente('${job.uuid}');" class="stop iepngfix">&nbsp;</a>
							</c:if>
						</td>
						<td><fmt:formatDate value="${job.creationDate}" pattern="dd.MM.yyyy HH:mm:ss"/></td>
						<td><c:out value="${job.description}"/></td>
						<td><c:out value="${job.runningMessage}"/></td>
						<c:if test="${job.running}">
							<td><unireg:duration msDuration="${job.duration}" shortVersion="true"/></td>
							<td>
								<c:if test="${job.interrupted}">
									<fmt:message key="label.inbox.interrupting"/>
								</c:if>
								<c:if test="${!job.interrupted}">
									<c:if test="${job.percentProgression != null}">
										<unireg:percentIndicator percent="${job.percentProgression}"/>
									</c:if>
									<c:if test="${job.percentProgression == null}">
										<fmt:message key="label.inbox.en.cours"/>
									</c:if>
								</c:if>
							</td>
						</c:if>
						<c:if test="${!job.running}">
							<td colspan="2">&nbsp;</td>
						</c:if>
                    </tr>
        		</c:forEach>
        	</table>

        	<script>
				function stopJobEnAttente(uuid) {
					if (confirm('Êtes-vous sûr de vouloir interrompre/annuler cette extraction ?')) {
						$.post(getContextPath() + "/admin/inbox/stopJob.do?id=" + uuid, function() {
                            refreshJobsEnAttente();
						});
					}
				}
        	</script>

        </c:if>

	</tiles:put>
</tiles:insert>