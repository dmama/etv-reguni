<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%--@elvariable id="uniregEnvironnement" type="java.lang.String"--%>
<c:if test="${uniregEnvironnement == 'Developpement'}">
	<script type="text/javascript" language="Javascript" src="<c:url value="/js/original/jquery.js"/>"></script>
	<script type="text/javascript" language="Javascript" src="<c:url value="/js/original/jquery.bgiframe.js"/>"></script>
	<script type="text/javascript" language="Javascript" src="<c:url value="/js/original/jquery-ui.js"/>"></script>
	<script type="text/javascript" language="Javascript" src="<c:url value="/js/original/jquery.ui.datepicker-fr-CH.js"/>"></script>
	<script type="text/javascript" language="Javascript" src="<c:url value="/js/original/jquery.ui.tooltip.js"/>"></script>
	<script type="text/javascript" language="Javascript" src="<c:url value="/js/original/jquery.cookie.js"/>"></script>
	<script type="text/javascript" language="Javascript" src="<c:url value="/js/original/jquery.timers.js"/>"></script>
	<script type="text/javascript" language="Javascript" src="<c:url value="/js/original/jquery.form.js"/>"></script>
	<script type="text/javascript" language="Javascript" src="<c:url value="/js/original/jquery.jgrowl.js"/>"></script>
</c:if>
<c:if test="${uniregEnvironnement != 'Developpement'}">
	<script type="text/javascript" language="Javascript" src="<c:url value="/js/jquery-all.js"/>"></script>
</c:if>

<script type="text/javascript" language="Javascript" src="<c:url value="/js/unireg.js"/>"></script>
