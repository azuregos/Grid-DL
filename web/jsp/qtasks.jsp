<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>GRID-DL</title>
    </head>
    <body>
        <%@include file="server_stats.jsp" %>
        <hr/><br/>
        
        <h2>Query Tasks:</h2>
        <table border="1" width="100%">
            <thead>
                <tr>
                    <th>Task ID</th>
                    <th>Submit Time</th>
                    <th>Query</th>
                    <th>Status</th>
                    <th>Results</th>
                    <th>Log</th>
                </tr>
            </thead>
            <c:forEach var="qtask" items="${qtasks}">
                <tr>
                    <td align="center" width="10%">${qtask.id}</td>
                    <td align="center" width="20%">${qtask.submitTime}</td>
                    <td align="center">${qtask.query}</td>
                    <td align="center" width="10%">${qtask.status}</td>
                    <td align="center" width="10%"><a href="qtasks?id=${qtask.id}&action=ret">View Results</a></td>
                    <td align="center" width="10%"><a href="qtasks?id=${qtask.id}&action=log">View Log</a></td>
                </tr>
            </c:forEach>
        </table>

        <br/>
        <c:if test="${not empty log}">
            <h3>Task Log:</h3>
            <textarea cols="135" rows="20"><c:out value="${log}"/></textarea>
        </c:if>
        <c:if test="${not empty results}">
            <h3>Results:</h3>
            <ul>
                <c:forEach var="result" items="${results}">
                    <li><c:out value="${result}" escapeXml="true"/></li>
                </c:forEach>
            </ul>
        </c:if>

    </body>
</html>
