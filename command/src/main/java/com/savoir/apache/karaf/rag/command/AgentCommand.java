/*
 * Copyright (c) 2012-2024 Savoir Technologies, Inc.
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
package com.savoir.apache.karaf.rag.command;

import com.savoir.apache.karaf.rag.agent.service.api.AgentService;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Service
@Command(scope = "agent", name = "ask", description = "Ask our agent a question.")
public class AgentCommand implements Action {

    @Reference
    private AgentService agentService;

    @Argument(index = 0, name = "question", description = "User question", required = true, multiValued = false)
    String question;

    @Override
    public Object execute() throws Exception {
        System.out.println(agentService.getAnswer(question));
        return null;
    }

}
