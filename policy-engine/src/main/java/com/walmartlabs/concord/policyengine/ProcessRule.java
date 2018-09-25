package com.walmartlabs.concord.policyengine;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2018 Walmart Inc.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.Set;

public class ProcessRule implements Serializable {

    private final String msg;
    private final Integer maxConcurrent;

    @JsonCreator
    public ProcessRule(@JsonProperty("msg") String msg,
                       @JsonProperty("maxConcurrent") Integer maxConcurrent) {
        this.msg = msg;
        this.maxConcurrent = maxConcurrent;
    }

    public String getMsg() {
        return msg;
    }

    public Integer getMaxConcurrent() {
        return maxConcurrent;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, Utils.NotNullToStringStyle.NOT_NULL_STYLE)
                .append("msg", msg)
                .append("maxConcurrent", maxConcurrent)
                .toString();
    }
}
