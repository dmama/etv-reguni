<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/templateDialog.jsp">
  	<tiles:put name="title"></tiles:put>
  	<tiles:put name="body">

        <c:if test="${empty content}">
            <fmt:message key="label.inbox.empty.inbox"/>
        </c:if>
        <c:if test="${not empty content}">
        	<table class="inbox">
        		<tr>
        			<th><fmt:message key="label.inbox.date.reception"/></th>
        			<th><fmt:message key="label.inbox.nom"/></th>
        			<th><fmt:message key="label.inbox.description"/></th>
        			<th><fmt:message key="label.inbox.expiration"/></th>
        			<th><fmt:message key="label.inbox.doc"/></th>
        			<th><fmt:message key="label.action"/></th>
        		</tr>
        		<unireg:nextRowClass reset='1'/>
        		<c:forEach var="elt" items="${content}">
        		    <c:if test="${elt.read}">
        		    	<c:set var="trClass" value='read'/>
        		    </c:if>
        		    <c:if test="${!elt.read}">
        		    	<c:set var="trClass" value='unread'/>
        		    </c:if>

                    <tr class='<unireg:nextRowClass/> ${trClass}'>
						<td><fmt:formatDate value="${elt.incomingDate}" pattern="dd.MM.yyyy HH:mm:ss"/></td>
						<td><c:out value="${elt.name}"/></td>
						<td><c:out value="${elt.description}"/></td>

						<c:set var="sansExpiration"><fmt:message key="label.inbox.sans.expiration"/></c:set>
						<c:set var="expirationDepassee"><fmt:message key="label.inbox.expire"/></c:set>
						<td><unireg:duration msDuration='${elt.timeToExpiration}' rounded="true" zeroDisplay="${expirationDepassee}" nullDisplay="${sansExpiration}" isNullDuration="${elt.timeToExpiration == null}"/></td>

						<td>
							<c:set var="attachment" value="${elt.attachment}"/>
							<c:if test="${attachment != null}">
								<a href="download.do?id=${elt.uuid}">
									<unireg:documentIcon mimeType="${attachment.mimeType}"/>
								</a>
							</c:if>
							<c:if test="${attachment == null}">
								&nbsp;
							</c:if>
						</td>
						<td><unireg:raccourciAnnuler onClick="removeInboxContent('${elt.uuid}');"/></td>
                    </tr>
        		</c:forEach>
        	</table>

        	<script type="text/javascript">
				function removeInboxContent(uuid) {
					if (confirm('Êtes-vous sûr de vouloir effacer ce message ?')) {
						$.post(getContextPath() + "/admin/inbox/removeElement.do?id=" + uuid, function() {
                            refreshInboxContent();
						});
					}
				}
        	</script>

        </c:if>

	</tiles:put>
</tiles:insert>