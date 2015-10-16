/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.di.spi;

import org.apache.cayenne.di.DIRuntimeException;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.di.UnorderedListBuilder;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.di.Scope;

import java.util.Collection;
import java.util.List;

/**
 * @since 3.1
 */
class DefaultListBuilder<T> implements ListBuilder<T> {

    protected DefaultInjector injector;
    protected Key<List<?>> bindingKey;

    DefaultListBuilder(Key<List<?>> bindingKey, DefaultInjector injector) {
        this.injector = injector;
        this.bindingKey = bindingKey;

        // trigger initialization of the ListProvider right away, as we need to bind an
        // empty list even if the user never calls 'put'
        getListProvider();
    }

    @Override
    public ListBuilder<T> add(Class<? extends T> interfaceType)
            throws DIRuntimeException {

        Key key = Key.get(interfaceType);
        getListProvider().add(key, injector.getProvider(key));
        return this;
    }

    @Override
    public ListBuilder<T> add(T object) throws DIRuntimeException {

        Provider<T> provider0 = new InstanceProvider<T>(object);
        Provider<T> provider1 = new FieldInjectingProvider<T>(provider0, injector);

        getListProvider().add(Key.get(object.getClass(), String.valueOf(object.hashCode())), provider1);
        return this;
    }

    @Override
    public ListBuilder<T> add(Key<T> key, T object) throws DIRuntimeException {

        Provider<T> provider0 = new InstanceProvider<T>(object);
        Provider<T> provider1 = new FieldInjectingProvider<T>(provider0, injector);

        getListProvider().add(key, provider1);
        return this;
    }

    @Override
    public UnorderedListBuilder<T> after(Class<?> type) {
        return after(Key.get(type));
    }

    @Override
    public UnorderedListBuilder<T> after(Key<?> key) {
        getListProvider().after(key);
        return this;
    }

    @Override
    public UnorderedListBuilder<T> before(Class<?> type) {
        return before(Key.get(type));
    }

    @Override
    public UnorderedListBuilder<T> before(Key<?> key) {
        getListProvider().before(key);
        return this;
    }

    @Override
    public ListBuilder<T> addAll(Collection<T> objects) throws DIRuntimeException {

        ListProvider listProvider = getListProvider();

        for (T object : objects) {
            Provider<T> provider0 = new InstanceProvider<T>(object);
            Provider<T> provider1 = new FieldInjectingProvider<T>(provider0, injector);

            listProvider.add(Key.get(object.getClass(), String.valueOf(object.hashCode())), provider1);
        }

        return this;
    }

    private ListProvider getListProvider() {

        ListProvider provider = null;

        Binding<List<?>> binding = injector.getBinding(bindingKey);
        if (binding == null) {
            provider = new ListProvider();
            injector.putBinding(bindingKey, provider);
        }
        else {
            provider = (ListProvider) binding.getOriginal();
        }

        return provider;
    }

    @Override
    public void in(Scope scope) {
        injector.changeBindingScope(bindingKey, scope);
    }
}
