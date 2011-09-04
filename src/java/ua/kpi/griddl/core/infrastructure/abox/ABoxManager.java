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
package ua.kpi.griddl.core.infrastructure.abox;

import org.semanticweb.owlapi.model.OWLOntology;

/**
 *
 * @author Pospishniy Olexandr <pospishniy@kpi.in.ua>
 */
public class ABoxManager {

    private static ABoxManager instance;

    private ABoxManager() {
    }

    private static class SingletonHolder {
        public static final ABoxManager INSTANCE = new ABoxManager();
    }

    public static ABoxManager getInstance() {
        return SingletonHolder.INSTANCE;
    }
    private OWLOntology abox;

    public synchronized OWLOntology getActiveABox() {
        return abox;
    }

    public synchronized void setActiveABox(OWLOntology abox) {
        this.abox = abox;
    }
}
