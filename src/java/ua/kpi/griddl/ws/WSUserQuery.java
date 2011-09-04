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
package ua.kpi.griddl.ws;

import java.util.List;

/**
 *
 * @author Pospishniy Olexandr <pospishniy@kpi.in.ua>
 */
public class WSUserQuery {

    private String query;
    private List<String> ontologies;

    public WSUserQuery() {
    }

    public WSUserQuery(String query, List<String> ontologies) {
        this.query = query;
        this.ontologies = ontologies;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<String> getOntologies() {
        return ontologies;
    }

    public void setOntologies(List<String> ontologies) {
        this.ontologies = ontologies;
    }
}
