/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the "Elastic License
 * 2.0", the "GNU Affero General Public License v3.0 only", and the "Server Side
 * Public License v 1"; you may not use this file except in compliance with, at
 * your election, the "Elastic License 2.0", the "GNU Affero General Public
 * License v3.0 only", or the "Server Side Public License, v 1".
 */

package org.elasticsearch.test.cluster.util.resource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.function.Supplier;

class StringResource implements Resource {
    private final Supplier<String> text;

    StringResource(String text) {
        this.text = () -> text;
    }

    StringResource(Supplier<String> supplier) {
        this.text = supplier;
    }

    @Override
    public InputStream asStream() {
        return new ByteArrayInputStream(text.get().getBytes());
    }

}
