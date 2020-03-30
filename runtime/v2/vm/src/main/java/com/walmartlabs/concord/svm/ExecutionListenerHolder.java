package com.walmartlabs.concord.svm;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2020 Walmart Inc.
 * -----
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
 * =====
 */

import java.util.ArrayList;
import java.util.Collection;

import static com.walmartlabs.concord.svm.ExecutionListener.Result.BREAK;
import static com.walmartlabs.concord.svm.ExecutionListener.Result.CONTINUE;

public class ExecutionListenerHolder {

    private final VM vm;
    private final Collection<ExecutionListener> listeners;

    public ExecutionListenerHolder(VM vm, Collection<ExecutionListener> listeners) {
        this.vm = vm;
        this.listeners = new ArrayList<>(listeners);
    }

    public ExecutionListener.Result fireBeforeCommand(Runtime runtime, State state, ThreadId threadId, Command cmd) {
        ExecutionListener.Result result = CONTINUE;

        for (ExecutionListener l : listeners) {
            ExecutionListener.Result r = l.beforeCommand(runtime, vm, state, threadId, cmd);
            if (r == BREAK && result != BREAK) {
                result = BREAK;
            }
        }

        return result;
    }

    public ExecutionListener.Result fireAfterCommand(Runtime runtime, State state, ThreadId threadId, Command cmd) {
        ExecutionListener.Result result = CONTINUE;

        for (ExecutionListener l : listeners) {
            ExecutionListener.Result r = l.afterCommand(runtime, vm, state, threadId, cmd);
            if (r == BREAK && result != BREAK) {
                result = BREAK;
            }
        }

        return result;
    }

    public ExecutionListener.Result fireAfterEval(Runtime runtime, State state) {
        ExecutionListener.Result result = CONTINUE;

        for (ExecutionListener l : listeners) {
            ExecutionListener.Result r = l.afterEval(runtime, vm, state);
            if (r == BREAK && result != BREAK) {
                result = BREAK;
            }
        }

        return result;
    }

    public ExecutionListener.Result fireAfterWakeUp(Runtime runtime, State state) {
        ExecutionListener.Result result = CONTINUE;

        for (ExecutionListener l : listeners) {
            ExecutionListener.Result r = l.afterWakeUp(runtime, vm, state);
            if (r == BREAK && result != BREAK) {
                result = BREAK;
            }
        }

        return result;
    }
}