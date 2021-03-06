package com.walmartlabs.concord.server.boot.validation;

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

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.matcher.Matchers;
import org.aopalliance.intercept.MethodInterceptor;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.spi.properties.ConstrainableExecutable;
import org.hibernate.validator.spi.properties.GetterPropertySelectionStrategy;
import org.sonatype.siesta.Validate;

import javax.inject.Singleton;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.executable.ExecutableValidator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ValidationModule extends AbstractModule {

    @Override
    protected void configure() {
        final MethodInterceptor interceptor = new ValidationInterceptor();
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(Validate.class), interceptor);
        requestInjection(interceptor);
    }

    @Provides
    @Singleton
    ValidatorFactory validatorFactory() {
        return Validation.byProvider(HibernateValidator.class)
                .configure()
                .getterPropertySelectionStrategy(new GetterPropertySelectionStrategy() {

                    private final GetterPropertySelectionStrategy delegate = new DefaultGetterPropertySelectionStrategy();

                    @Override
                    public Optional<String> getProperty(ConstrainableExecutable executable) {
                        Optional<String> result = delegate.getProperty(executable);
                        if (result.isPresent()) {
                            return result;
                        }

                        if (executable.getParameterTypes().length != 0 || executable.getReturnType() == void.class) {
                            return Optional.empty();
                        }

                        return Optional.of(executable.getName());
                    }

                    @Override
                    public Set<String> getGetterMethodNameCandidates(String propertyName) {
                        Set<String> getters = delegate.getGetterMethodNameCandidates(propertyName);
                        Set<String> result = new HashSet<>(getters);
                        result.add(propertyName);
                        return result;
                    }
                })
                .buildValidatorFactory();
    }

    @Provides
    @Singleton
    Validator validator(final ValidatorFactory validatorFactory) {
        return validatorFactory.getValidator();
    }

    @Provides
    @Singleton
    ExecutableValidator executableValidator(final Validator validator) {
        return validator.forExecutables();
    }
}
