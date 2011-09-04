/*
 * Copyright 2011 NTUU "KPI".
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ua.kpi.griddl.web;

import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import ua.kpi.griddl.core.infrastructure.reasoner.ReasonerManager;

/**
 *
 * @author Pospishniy Olexandr <pospishniy@kpi.in.ua>
 */
public class QueryTaskServlet extends HttpServlet {

    private static final String QTASKS_JSP = "/jsp/qtasks.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher disp = getServletContext().getRequestDispatcher(QTASKS_JSP);
        request.setAttribute("qtasks", ReasonerManager.getInstance().listQueryTasks());

        //process actions
        String action = request.getParameter("action");
        if (action != null) {
            Long taskID = Long.parseLong(request.getParameter("id"));
            if (action.equals("log")) {
                request.setAttribute("log", ReasonerManager.getInstance().getQueryTask(taskID).getTaskLog());
            } else if (action.equals("ret")) {
                request.setAttribute("results", ReasonerManager.getInstance().getQueryTask(taskID).getResultSet());
            }
        }

        disp.forward(request, response);
    }
}
