/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */
package com.microsoft.azure.eventhubs.amqp;


public interface IOperationResult<T, E extends Throwable> {

    void onComplete(T result);

    void onError(E error);
}
