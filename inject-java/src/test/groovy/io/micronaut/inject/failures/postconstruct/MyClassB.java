/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.inject.failures.postconstruct;

import io.micronaut.context.annotation.Requires;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Requires(property = "spec.name", value = "PostConstructExceptionSpec")
@Singleton
public class MyClassB {

    boolean setupComplete = false;
    boolean injectedFirst = false;

    @Inject
    protected MyClassA another;
    private MyClassA propA;

    @Inject
    public void setPropA(MyClassA propA) {
        this.propA = propA;
    }

    public MyClassA getPropA() {
        return propA;
    }

    @PostConstruct
    public void setup() {
        throw new RuntimeException("bad");
    }
}