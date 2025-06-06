/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the "Elastic License
 * 2.0", the "GNU Affero General Public License v3.0 only", and the "Server Side
 * Public License v 1"; you may not use this file except in compliance with, at
 * your election, the "Elastic License 2.0", the "GNU Affero General Public
 * License v3.0 only", or the "Server Side Public License, v 1".
 */

package org.elasticsearch.transport;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.core.Nullable;
import org.elasticsearch.tasks.TaskId;

import java.io.IOException;
import java.net.InetSocketAddress;

public abstract class AbstractTransportRequest extends TransportMessage implements TransportRequest {
    @Nullable // set by the transport service on inbound messages; unset on outbound messages
    private InetSocketAddress remoteAddress;

    /**
     * Parent of this request. Defaults to {@link TaskId#EMPTY_TASK_ID}, meaning "no parent".
     */
    private TaskId parentTaskId = TaskId.EMPTY_TASK_ID;

    /**
     * Request ID. Defaults to -1, meaning "no request ID is set".
     */
    private volatile long requestId = -1;

    public AbstractTransportRequest() {}

    public AbstractTransportRequest(StreamInput in) throws IOException {
        parentTaskId = TaskId.readFromStream(in);
    }

    @Override
    public void remoteAddress(InetSocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    @Nullable // set by the transport service on inbound messages; unset on outbound messages
    @Override
    public InetSocketAddress remoteAddress() {
        return remoteAddress;
    }

    /**
     * Set a reference to task that created this request.
     */
    @Override
    public void setParentTask(TaskId taskId) {
        this.parentTaskId = taskId;
    }

    /**
     * Get a reference to the task that created this request. Defaults to {@link TaskId#EMPTY_TASK_ID}, meaning "there is no parent".
     */
    @Override
    public TaskId getParentTask() {
        return parentTaskId;
    }

    /**
     * Set the request ID of this request.
     */
    @Override
    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    @Override
    public long getRequestId() {
        return requestId;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        parentTaskId.writeTo(out);
    }

    @Override
    public String toString() {
        return getClass().getName() + "/" + getParentTask();
    }
}
